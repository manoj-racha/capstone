package org.hartford.greensure.engine;



import org.hartford.greensure.dto.response.CarbonScoreResponse;
import org.hartford.greensure.engine.EmissionFactors;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CarbonScoreService {

    @Autowired
    private CarbonDeclarationRepository declarationRepo;
    @Autowired private VerificationRepository verificationRepo;
    @Autowired private DeclarationVehicleRepository vehicleRepo;
    @Autowired private VerifiedVehicleRepository verifiedVehicleRepo;
    @Autowired private HouseholdProfileRepository householdRepo;
    @Autowired private MsmeProfileRepository msmeRepo;
    @Autowired private CarbonScoreRepository scoreRepo;
    @Autowired private RecommendationService recommendationService;

    // ── GENERATE SCORE — called after verification ─────────────

    @Transactional
    public void generateScore(Long declarationId) {

        // Prevent duplicate score generation
        if (scoreRepo.existsByDeclarationDeclarationId(
                declarationId)) {
            return;
        }

        CarbonDeclaration declaration = declarationRepo
                .findById(declarationId)
                .orElseThrow(() -> new RuntimeException(
                        "Declaration not found: " + declarationId));

        Verification verification = verificationRepo
                .findByDeclarationDeclarationId(declarationId)
                .orElseThrow(() -> new RuntimeException(
                        "Verification not found for declaration: "
                                + declarationId));

        User user = declaration.getUser();
        boolean isHousehold =
                user.getUserType() == User.UserType.HOUSEHOLD;

        // ── Step 1 — Calculate Energy CO2 ─────────────────────
        double energyCo2 = calculateEnergyCo2(
                declaration, verification);

        // ── Step 2 — Calculate Transport CO2 ──────────────────
        double transportCo2 = calculateTransportCo2(
                declaration, verification);

        // ── Step 3 — Calculate Lifestyle or Operations CO2 ────
        double lifestyleCo2 = 0.0;
        double operationsCo2 = 0.0;

        if (isHousehold) {
            lifestyleCo2 = calculateLifestyleCo2(
                    declaration, verification, user);
        } else {
            operationsCo2 = calculateOperationsCo2(
                    declaration, verification);
        }

        // ── Step 4 — Sum all categories ───────────────────────
        double totalCo2 = energyCo2 + transportCo2
                + lifestyleCo2 + operationsCo2;

        // ── Step 5 — Calculate Per Capita CO2 ─────────────────
        double perCapitaCo2;
        if (isHousehold) {
            int members = householdRepo
                    .findByUserUserId(user.getUserId())
                    .map(p -> p.getNumberOfMembers())
                    .orElse(1);
            perCapitaCo2 = totalCo2 / members;
        } else {
            int employees = msmeRepo
                    .findByUserUserId(user.getUserId())
                    .map(p -> p.getNumEmployees())
                    .orElse(1);
            perCapitaCo2 = totalCo2 / employees;
        }

        // ── Step 6 — Classify Zone ────────────────────────────
        CarbonScore.CarbonZone zone = classifyZone(
                perCapitaCo2, isHousehold);

        // ── Step 7 — Save Carbon Score ────────────────────────
        CarbonScore score = CarbonScore.builder()
                .user(user)
                .declaration(declaration)
                .scoreYear(declaration.getDeclarationYear())
                .energyCo2(round(energyCo2))
                .transportCo2(round(transportCo2))
                .lifestyleCo2(isHousehold
                        ? round(lifestyleCo2) : null)
                .operationsCo2(!isHousehold
                        ? round(operationsCo2) : null)
                .totalCo2(round(totalCo2))
                .perCapitaCo2(round(perCapitaCo2))
                .zone(zone)
                .build();

        score = scoreRepo.save(score);

        // ── Step 8 — Generate Recommendations ─────────────────
        recommendationService.generateRecommendations(score);
    }

    // ── GET MY SCORE ───────────────────────────────────────────

    public CarbonScoreResponse getMyScore(Long userId) {
        CarbonScore score = scoreRepo
                .findTopByUserUserIdOrderByScoreYearDesc(userId)
                .orElseThrow(() -> new RuntimeException(
                        "No score found yet"));
        return mapToResponse(score);
    }

    // ── GET SCORE HISTORY ──────────────────────────────────────

    public List<CarbonScoreResponse> getScoreHistory(Long userId) {
        List<CarbonScore> scores = scoreRepo
                .findByUserUserIdOrderByScoreYearDesc(userId);

        return scores.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // CALCULATION METHODS
    // ═══════════════════════════════════════════════════════════

    // ── ENERGY CO2 ─────────────────────────────────────────────

    private double calculateEnergyCo2(
            CarbonDeclaration d, Verification v) {

        double total = 0.0;

        // 1. Electricity
        double electricityUnits = resolve(
                v.getCorrectedElectricityUnits(),
                d.getElectricityUnits());

        double electricityCo2 = electricityUnits
                * EmissionFactors.ELECTRICITY_KG_PER_KWH
                * EmissionFactors.MONTHS_IN_YEAR;

        // Subtract solar generation if applicable
        if (Boolean.TRUE.equals(d.getHasSolar())) {
            double solarUnits = resolve(
                    v.getCorrectedSolarUnits(),
                    d.getSolarUnits());
            electricityCo2 -= solarUnits
                    * EmissionFactors.ELECTRICITY_KG_PER_KWH
                    * EmissionFactors.MONTHS_IN_YEAR;
        }

        // Ensure electricity CO2 never goes negative
        total += Math.max(0, electricityCo2);

        // 2. Cooking fuel
        if (d.getCookingFuelType() != null) {
            CarbonDeclaration.CookingFuelType fuelType = resolve(
                    v.getCorrectedCookingFuelType(),
                    d.getCookingFuelType());

            total += calculateCookingCo2(fuelType, d, v);
        }

        // 3. AC units
        if (d.getNumAcUnits() != null
                && d.getAcHoursPerDay() != null) {
            int acUnits = d.getNumAcUnits();
            double acHours = d.getAcHoursPerDay();

            total += acUnits
                    * acHours
                    * EmissionFactors.AC_KG_CO2_PER_UNIT_PER_HOUR
                    * EmissionFactors.DAYS_IN_YEAR;
        }

        // 4. Generator
        if (Boolean.TRUE.equals(d.getHasGenerator())) {
            double genHours = resolve(
                    v.getCorrectedGeneratorHours(),
                    d.getGeneratorHoursPerMonth());

            total += genHours
                    * EmissionFactors.GENERATOR_KG_CO2_PER_HOUR
                    * EmissionFactors.MONTHS_IN_YEAR;
        }

        return total;
    }

    private double calculateCookingCo2(
            CarbonDeclaration.CookingFuelType fuelType,
            CarbonDeclaration d, Verification v) {

        switch (fuelType) {
            case LPG:
                double cylinders = resolve(
                        v.getCorrectedLpgCylinders(),
                        d.getLpgCylinders());
                return cylinders
                        * EmissionFactors.LPG_KG_CO2_PER_CYLINDER
                        * EmissionFactors.MONTHS_IN_YEAR;

            case PNG:
                double pngUnits = resolve(
                        v.getCorrectedPngUnits(), d.getPngUnits());
                return pngUnits
                        * EmissionFactors.PNG_KG_CO2_PER_SCM
                        * EmissionFactors.MONTHS_IN_YEAR;

            case BIOMASS:
                double biomassKg = resolve(
                        v.getCorrectedBiomassKg(),
                        d.getBiomassKgPerDay());
                return biomassKg
                        * EmissionFactors.BIOMASS_KG_CO2_PER_KG
                        * EmissionFactors.DAYS_IN_YEAR;

            case ELECTRIC:
                // Already counted in electricity units
                return 0.0;

            case NONE:
            default:
                return 0.0;
        }
    }

    // ── TRANSPORT CO2 ──────────────────────────────────────────

    private double calculateTransportCo2(
            CarbonDeclaration d, Verification v) {

        double total = 0.0;

        // 1. All declared vehicles
        List<DeclarationVehicle> vehicles = vehicleRepo
                .findByDeclarationDeclarationId(d.getDeclarationId());

        for (DeclarationVehicle vehicle : vehicles) {

            // Check if agent corrected this vehicle
            double km = vehicle.getKmPerMonth();
            DeclarationVehicle.FuelType fuelType =
                    vehicle.getFuelType();

            // Look for a verified vehicle correction
            Optional<VerifiedVehicle> corrected =
                    verifiedVehicleRepo
                            .findByDeclarationVehicleVehicleId(
                                    vehicle.getVehicleId());

            if (corrected.isPresent()) {
                if (corrected.get().getCorrectedKm() != null) {
                    km = corrected.get().getCorrectedKm();
                }
                if (corrected.get().getCorrectedFuelType() != null) {
                    fuelType = corrected.get().getCorrectedFuelType();
                }
            }

            double emissionFactor = getVehicleEmissionFactor(
                    vehicle.getVehicleType(), fuelType);

            total += km
                    * vehicle.getQuantity()
                    * emissionFactor
                    * EmissionFactors.MONTHS_IN_YEAR;
        }

        // 2. Public transport
        if (Boolean.TRUE.equals(d.getUsesPublicTransport())) {
            double publicKm = resolve(
                    v.getCorrectedPublicTransportKm(),
                    d.getPublicTransportKm());

            total += publicKm
                    * EmissionFactors.PUBLIC_BUS_KG_PER_KM
                    * EmissionFactors.MONTHS_IN_YEAR;
        }

        return total;
    }

    private double getVehicleEmissionFactor(
            DeclarationVehicle.VehicleType vehicleType,
            DeclarationVehicle.FuelType fuelType) {

        switch (vehicleType) {
            case TWO_WHEELER:
                switch (fuelType) {
                    case PETROL:
                        return EmissionFactors
                                .TWO_WHEELER_PETROL_KG_PER_KM;
                    case DIESEL:
                        return EmissionFactors
                                .TWO_WHEELER_DIESEL_KG_PER_KM;
                    case ELECTRIC:
                        return EmissionFactors
                                .TWO_WHEELER_ELECTRIC_KG_PER_KM;
                    default: return 0.0;
                }

            case FOUR_WHEELER:
                switch (fuelType) {
                    case PETROL:
                        return EmissionFactors
                                .FOUR_WHEELER_PETROL_KG_PER_KM;
                    case DIESEL:
                        return EmissionFactors
                                .FOUR_WHEELER_DIESEL_KG_PER_KM;
                    case CNG:
                        return EmissionFactors
                                .FOUR_WHEELER_CNG_KG_PER_KM;
                    case ELECTRIC:
                        return EmissionFactors
                                .FOUR_WHEELER_ELECTRIC_KG_PER_KM;
                    default: return 0.0;
                }

            case COMMERCIAL:
                switch (fuelType) {
                    case DIESEL:
                        return EmissionFactors
                                .COMMERCIAL_DIESEL_KG_PER_KM;
                    case CNG:
                        return EmissionFactors
                                .COMMERCIAL_CNG_KG_PER_KM;
                    default: return 0.0;
                }

            default: return 0.0;
        }
    }

    // ── LIFESTYLE CO2 — Household Only ─────────────────────────

    private double calculateLifestyleCo2(
            CarbonDeclaration d, Verification v, User user) {

        double total = 0.0;

        // Get number of members for diet calculation
        int members = householdRepo
                .findByUserUserId(user.getUserId())
                .map(p -> p.getNumberOfMembers())
                .orElse(1);

        // 1. Diet
        CarbonDeclaration.DietaryPattern diet = resolve(
                v.getCorrectedDietaryPattern(),
                d.getDietaryPattern());

        if (diet != null) {
            total += getDietCo2PerPerson(diet) * members;
        }

        // 2. Online shopping
        CarbonDeclaration.ShoppingOrders shopping = resolve(
                v.getCorrectedShoppingOrders(),
                d.getShoppingOrdersPerMonth());

        if (shopping != null) {
            total += getShoppingCo2(shopping);
        }

        return total;
    }

    private double getDietCo2PerPerson(
            CarbonDeclaration.DietaryPattern diet) {
        switch (diet) {
            case VEGAN:
                return EmissionFactors.DIET_VEGAN;
            case VEGETARIAN:
                return EmissionFactors.DIET_VEGETARIAN;
            case EGGETARIAN:
                return EmissionFactors.DIET_EGGETARIAN;
            case NON_VEGETARIAN:
                return EmissionFactors.DIET_NON_VEGETARIAN;
            case HEAVY_NON_VEGETARIAN:
                return EmissionFactors.DIET_HEAVY_NON_VEGETARIAN;
            default: return 0.0;
        }
    }

    private double getShoppingCo2(
            CarbonDeclaration.ShoppingOrders shopping) {
        switch (shopping) {
            case ZERO_TO_FIVE:
                return EmissionFactors.SHOPPING_ZERO_TO_FIVE;
            case SIX_TO_FIFTEEN:
                return EmissionFactors.SHOPPING_SIX_TO_FIFTEEN;
            case ABOVE_FIFTEEN:
                return EmissionFactors.SHOPPING_ABOVE_FIFTEEN;
            default: return 0.0;
        }
    }

    // ── OPERATIONS CO2 — MSME Only ─────────────────────────────

    private double calculateOperationsCo2(
            CarbonDeclaration d, Verification v) {

        double total = 0.0;

        // 1. MSME generator — diesel litres per month
        if (d.getGeneratorLitersPerMonth() != null) {
            double liters = resolve(
                    v.getCorrectedGeneratorLiters(),
                    d.getGeneratorLitersPerMonth());

            total += liters
                    * EmissionFactors.DIESEL_KG_CO2_PER_LITRE
                    * EmissionFactors.MONTHS_IN_YEAR;
        }

        // 2. Commercial vehicles
        if (Boolean.TRUE.equals(d.getHasCommercialVehicles())) {
            double commKm = resolve(
                    v.getCorrectedCommercialVehicleKm(),
                    d.getCommercialVehicleKm());

            total += commKm
                    * EmissionFactors.COMMERCIAL_DIESEL_KG_PER_KM
                    * EmissionFactors.MONTHS_IN_YEAR;
        }

        // 3. Third party shipments
        if (d.getThirdPartyShipments() != null) {
            int shipments = resolve(
                    v.getCorrectedThirdPartyShipments(),
                    d.getThirdPartyShipments());

            total += shipments
                    * EmissionFactors.LOGISTICS_KG_CO2_PER_SHIPMENT
                    * EmissionFactors.MONTHS_IN_YEAR;
        }

        // 4. Boiler
        if (Boolean.TRUE.equals(d.getHasBoiler())
                && d.getBoilerFuelType() != null) {

            switch (d.getBoilerFuelType()) {
                case COAL:
                    double coalKg = resolve(
                            v.getCorrectedBoilerCoalKg(),
                            d.getBoilerCoalKg());
                    total += coalKg
                            * EmissionFactors.BOILER_COAL_KG_CO2_PER_KG
                            * EmissionFactors.MONTHS_IN_YEAR;
                    break;

                case NATURAL_GAS:
                    double gasScm = resolve(
                            v.getCorrectedBoilerGasScm(),
                            d.getBoilerGasScm());
                    total += gasScm
                            * EmissionFactors.BOILER_GAS_KG_CO2_PER_SCM
                            * EmissionFactors.MONTHS_IN_YEAR;
                    break;

                default: break;
            }
        }

        // 5. Paper consumption
        if (d.getPaperReamsPerMonth() != null) {
            int reams = resolve(
                    v.getCorrectedPaperReams(),
                    d.getPaperReamsPerMonth());

            double paperCo2 = reams
                    * EmissionFactors.PAPER_KG_CO2_PER_REAM
                    * EmissionFactors.MONTHS_IN_YEAR;

            // Recycled paper has 50% lower footprint
            if (Boolean.TRUE.equals(d.getUsesRecycledPaper())) {
                paperCo2 *= EmissionFactors.PAPER_RECYCLED_FACTOR;
            }

            total += paperCo2;
        }

        // 6. Raw material
        if (d.getRawMaterialKg() != null
                && d.getRawMaterialType() != null) {
            double rawKg = resolve(
                    v.getCorrectedRawMaterialKg(),
                    d.getRawMaterialKg());

            double rawFactor = getRawMaterialFactor(
                    d.getRawMaterialType());

            total += rawKg * rawFactor
                    * EmissionFactors.MONTHS_IN_YEAR;
        }

        return total;
    }

    private double getRawMaterialFactor(
            CarbonDeclaration.RawMaterialType type) {
        switch (type) {
            case VIRGIN:
                return EmissionFactors
                        .RAW_MATERIAL_VIRGIN_KG_CO2_PER_KG;
            case RECYCLED:
                return EmissionFactors
                        .RAW_MATERIAL_RECYCLED_KG_CO2_PER_KG;
            case MIXED:
                return EmissionFactors
                        .RAW_MATERIAL_MIXED_KG_CO2_PER_KG;
            default: return 0.0;
        }
    }

    // ── ZONE CLASSIFICATION ────────────────────────────────────

    private CarbonScore.CarbonZone classifyZone(
            double perCapitaCo2, boolean isHousehold) {

        if (isHousehold) {
            if (perCapitaCo2 <= EmissionFactors.HH_GREEN_CHAMPION_THRESHOLD) {
                return CarbonScore.CarbonZone.GREEN_CHAMPION;
            } else if (perCapitaCo2 <= EmissionFactors.HH_GREEN_IMPROVER_THRESHOLD) {
                return CarbonScore.CarbonZone.GREEN_IMPROVER;
            } else {
                return CarbonScore.CarbonZone.GREEN_DEFAULTER;
            }
        } else {
            if (perCapitaCo2 <= EmissionFactors.MSME_GREEN_CHAMPION_THRESHOLD) {
                return CarbonScore.CarbonZone.GREEN_CHAMPION;
            } else if (perCapitaCo2 <= EmissionFactors.MSME_GREEN_IMPROVER_THRESHOLD) {
                return CarbonScore.CarbonZone.GREEN_IMPROVER;
            } else {
                return CarbonScore.CarbonZone.GREEN_DEFAULTER;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════

    // Core resolve rule:
    // If agent corrected the value → use corrected
    // If agent left it null → use declared value
    private <T> T resolve(T corrected, T declared) {
        return corrected != null ? corrected : declared;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ── MAP TO RESPONSE ────────────────────────────────────────

    public CarbonScoreResponse mapToResponse(CarbonScore score) {
        double total = score.getTotalCo2();

        double energyPct = total > 0
                ? round(score.getEnergyCo2() * 100.0 / total)
                : 0;
        double transportPct = total > 0
                ? round(score.getTransportCo2() * 100.0 / total)
                : 0;
        double lifestylePct = total > 0
                && score.getLifestyleCo2() != null
                ? round(score.getLifestyleCo2() * 100.0 / total)
                : 0;
        double operationsPct = total > 0
                && score.getOperationsCo2() != null
                ? round(score.getOperationsCo2() * 100.0 / total)
                : 0;

        return CarbonScoreResponse.builder()
                .scoreId(score.getScoreId())
                .userId(score.getUser().getUserId())
                .scoreYear(score.getScoreYear())
                .energyCo2(score.getEnergyCo2())
                .transportCo2(score.getTransportCo2())
                .lifestyleCo2(score.getLifestyleCo2())
                .operationsCo2(score.getOperationsCo2())
                .totalCo2(score.getTotalCo2())
                .perCapitaCo2(score.getPerCapitaCo2())
                .zone(score.getZone())
                .generatedAt(score.getGeneratedAt())
                .energyPercentage(energyPct)
                .transportPercentage(transportPct)
                .lifestylePercentage(lifestylePct)
                .operationsPercentage(operationsPct)
                .build();
    }
}
