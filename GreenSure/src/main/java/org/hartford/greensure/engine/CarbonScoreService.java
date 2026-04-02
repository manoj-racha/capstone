package org.hartford.greensure.engine;

import lombok.extern.slf4j.Slf4j;
import org.hartford.greensure.dto.response.CarbonScoreResponse;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.enums.Zone;
import org.hartford.greensure.exception.ResourceNotFoundException;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.service.RecommendationService;
import org.hartford.greensure.service.ai.GeminiApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CarbonScoreService {

    @Autowired
    private CarbonDeclarationRepository declarationRepo;
    @Autowired
    private HouseholdProfileRepository householdRepo;
    @Autowired
    private CarbonScoreRepository scoreRepo;
    @Autowired
    private RecommendationService recommendationService;
    @Autowired
    private GeminiApiService geminiApiService;
    @Autowired
    private DeclarationVehicleDataRepository vehicleRepository;
    @Autowired
    private CookingDataRepository cookingDataRepository;
    @Autowired
    private SolarDataRepository solarDataRepository;

    // ── GENERATE SCORE — called after verification ─────────────

    @Transactional
    public void generateScore(Long declarationId) {

        // Prevent duplicate score generation
        if (scoreRepo.existsByDeclarationDeclarationId(declarationId)) {
            return;
        }

        CarbonDeclaration declaration = declarationRepo
                .findById(declarationId)
                .orElseThrow(() -> new RuntimeException("Declaration not found: " + declarationId));

        User user = declaration.getUser();

        // 1. Electricity CO2
        double electricityCo2 = 0.0;
        if (declaration.getElectricityData() != null) {
            double kwh = declaration.getElectricityData().getEffectiveMonthlyKwh();
            electricityCo2 = kwh * EmissionFactors.ELECTRICITY_KG_PER_KWH * EmissionFactors.MONTHS_IN_YEAR;
        }

        // 2. Solar Offset
        double solarOffset = 0.0;
        if (declaration.getSolarData() != null && Boolean.TRUE.equals(declaration.getSolarData().isHasSolar())) {
            Double kw = declaration.getSolarData().getEffectiveCapacityKw();
            if (kw != null) {
                // Approximate solar offset: kw * 4 units/day * 365 days * emission factor
                solarOffset = kw * 4 * 365 * EmissionFactors.ELECTRICITY_KG_PER_KWH;
            }
        }

        // 3. Cooking CO2
        double cookingCo2 = 0.0;
        if (declaration.getCookingData() != null && declaration.getCookingData().getEffectiveFuelType() != null) {
            switch (declaration.getCookingData().getEffectiveFuelType()) {
                case LPG:
                    Integer cylinders = declaration.getCookingData().getEffectiveCylinders();
                    if (cylinders != null) {
                        cookingCo2 = cylinders * EmissionFactors.LPG_KG_CO2_PER_CYLINDER;
                    }
                    break;
                case PNG:
                    cookingCo2 = 15 * EmissionFactors.PNG_KG_CO2_PER_SCM * EmissionFactors.MONTHS_IN_YEAR; // Approximation
                    break;
                case BIOGAS:
                case ELECTRIC:
                    cookingCo2 = 0.0;
                    break;
            }
        }

        // 4. Transport/Vehicle CO2
        double vehicleCo2 = 0.0;
        if (declaration.getVehicles() != null) {
            for (var v : declaration.getVehicles()) {
                double kmPerYear = 10000;
                if (v.getEffectiveMileageBand() != null) {
                    switch (v.getEffectiveMileageBand().name()) {
                        case "LESS_THAN_5000":
                            kmPerYear = 3500;
                            break;
                        case "FIVE_TO_TEN_THOUSAND":
                            kmPerYear = 7500;
                            break;
                        case "TEN_TO_FIFTEEN_THOUSAND":
                            kmPerYear = 12500;
                            break;
                        case "MORE_THAN_FIFTEEN_THOUSAND":
                            kmPerYear = 18000;
                            break;
                        default:
                            kmPerYear = 10000;
                    }
                }

                double factor = 0.0;
                if (v.getVehicleCategory() == org.hartford.greensure.enums.VehicleCategory.TWO_WHEELER) {
                    if (v.getEffectiveFuelType() == org.hartford.greensure.enums.FuelType.PETROL)
                        factor = EmissionFactors.TWO_WHEELER_PETROL_KG_PER_KM;
                    else if (v.getEffectiveFuelType() == org.hartford.greensure.enums.FuelType.EV)
                        factor = 0;
                } else if (v.getVehicleCategory() == org.hartford.greensure.enums.VehicleCategory.FOUR_WHEELER) {
                    if (v.getEffectiveFuelType() == org.hartford.greensure.enums.FuelType.PETROL)
                        factor = EmissionFactors.FOUR_WHEELER_PETROL_KG_PER_KM;
                    else if (v.getEffectiveFuelType() == org.hartford.greensure.enums.FuelType.DIESEL)
                        factor = EmissionFactors.FOUR_WHEELER_DIESEL_KG_PER_KM;
                    else if (v.getEffectiveFuelType() == org.hartford.greensure.enums.FuelType.CNG)
                        factor = 0.15; // fallback approximation
                    else if (v.getEffectiveFuelType() == org.hartford.greensure.enums.FuelType.EV)
                        factor = 0;
                }
                vehicleCo2 += kmPerYear * factor;
            }
        }

        // 5. Lifestyle Bonus
        double lifestyleBonus = 0.0;
        if (declaration.getLifestyleData() != null) {
            if (declaration.getLifestyleData().isWastesRecycling()) {
                lifestyleBonus += 50.0; // Flat 50 kg reduction
            }
            if (declaration.getLifestyleData().getPublicTransportUsage() != null) {
                switch (declaration.getLifestyleData().getPublicTransportUsage().name()) {
                    case "OFTEN":
                        lifestyleBonus += vehicleCo2 * 0.20;
                        break; // 20% vehicle emission reduction
                    case "SOMETIMES":
                        lifestyleBonus += vehicleCo2 * 0.05;
                        break;
                    case "NEVER":
                        break;
                }
            }
        }

        // Note: MSME Operations logic was removed as part of the Unified User
        // refactoring.

        // Subtract offsets/bonuses from total components
        double adjustedElectricity = Math.max(0, electricityCo2 - solarOffset);
        double adjustedVehicle = Math.max(0, vehicleCo2 - lifestyleBonus);

        // Total computation
        double totalCo2 = adjustedElectricity + cookingCo2 + adjustedVehicle;

        // Per Capita
        int members = householdRepo.findByUserUserId(user.getUserId())
                .map(HouseholdProfile::getNumberOfMembers)
                .orElse(1);
        double perCapitaCo2 = totalCo2 / members;

        // Classify Zone
        Zone zone = classifyZone(perCapitaCo2);

        // Save Carbon Score
        CarbonScore score = CarbonScore.builder()
                .user(user)
                .declaration(declaration)
                .scoreYear(declaration.getDeclarationYear())
                .vehicleCo2(round(vehicleCo2))
                .electricityCo2(round(electricityCo2))
                .cookingCo2(round(cookingCo2))
                .solarOffset(round(solarOffset))
                .lifestyleBonus(round(lifestyleBonus))
                .totalCo2(round(totalCo2))
                .perCapitaCo2(round(perCapitaCo2))
                .zone(zone)
                .discountPercent(getDiscountForZone(zone)) // Simple mapping
                .build();

        CarbonScore savedScore = scoreRepo.save(score);

        // Generate Recommendations
        recommendationService.generateRecommendations(savedScore);

        // Generate AI explanation (best-effort; never fails score persistence)
        persistAiExplanationForScore(savedScore, declaration, declarationId);
    }

    /**
     * Fills {@code aiExplanation} for the user's latest score when it was never set
     * (e.g. score created before this feature, or generation failed earlier).
     * Safe to call on every dashboard load; no-ops when text is already present.
     */
    @Transactional
    public void backfillAiExplanationForLatestScoreIfMissing(Long userId) {
        CarbonScore score = scoreRepo.findTopByUserUserIdOrderByScoreYearDesc(userId).orElse(null);
        if (score == null) {
            return;
        }
        if (score.getAiExplanation() != null && !score.getAiExplanation().isBlank()) {
            return;
        }
        CarbonDeclaration linked = score.getDeclaration();
        if (linked == null) {
            return;
        }
        CarbonDeclaration declaration = declarationRepo.findById(linked.getDeclarationId()).orElse(null);
        if (declaration == null) {
            return;
        }
        persistAiExplanationForScore(score, declaration, declaration.getDeclarationId());
    }

    private void persistAiExplanationForScore(
            CarbonScore savedScore, CarbonDeclaration declaration, Long declarationId) {
        try {
            HouseholdProfile profile = householdRepo.findByUserUserId(declaration.getUser().getUserId()).orElse(null);

            List<DeclarationVehicleData> vehicles = vehicleRepository.findByDeclarationDeclarationId(declarationId);

            CookingData cooking = cookingDataRepository.findByDeclarationDeclarationId(declarationId).orElse(null);

            SolarData solar = solarDataRepository.findByDeclarationDeclarationId(declarationId).orElse(null);

            Double previousYearCo2 = scoreRepo
                    .findTopByUserUserIdAndScoreYearLessThanOrderByScoreYearDesc(
                            declaration.getUser().getUserId(),
                            declaration.getDeclarationYear())
                    .map(CarbonScore::getTotalCo2)
                    .orElse(null);

            String explanation = geminiApiService.generateScoreExplanation(
                    savedScore, profile, vehicles, cooking, solar, previousYearCo2);

            savedScore.setAiExplanation(explanation);
            scoreRepo.save(savedScore);

            log.info("AI explanation generated for score {}", savedScore.getScoreId());
        } catch (Exception e) {
            log.warn(
                    "AI explanation generation failed: {}. Score saved without explanation.",
                    e.getMessage());
        }
    }

    public CarbonScoreResponse getMyScore(Long userId) {
        backfillAiExplanationForLatestScoreIfMissing(userId);
        CarbonScore score = scoreRepo.findTopByUserUserIdOrderByScoreYearDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No score found yet"));
        return toResponse(score);
    }

    public List<CarbonScoreResponse> getScoreHistory(Long userId) {
        return scoreRepo.findByUserUserIdOrderByScoreYearDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Zone classifyZone(double perCapitaCo2) {
        if (perCapitaCo2 <= EmissionFactors.HH_GREEN_CHAMPION_THRESHOLD) {
            return Zone.GREEN_CHAMPION;
        } else if (perCapitaCo2 <= EmissionFactors.HH_GREEN_IMPROVER_THRESHOLD) {
            return Zone.IMPROVER;
        } else {
            return Zone.DEFAULTER;
        }
    }

    private double getDiscountForZone(Zone zone) {
        switch (zone) {
            case GREEN_CHAMPION:
                return 15.0;
            case IMPROVER:
                return 5.0;
            case DEFAULTER:
                return 0.0;
            default:
                return 0.0;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public CarbonScoreResponse toResponse(CarbonScore score) {
        return CarbonScoreResponse.builder()
                .scoreId(score.getScoreId())
                .scoreYear(score.getScoreYear())
                .vehicleCo2(score.getVehicleCo2())
                .electricityCo2(score.getElectricityCo2())
                .cookingCo2(score.getCookingCo2())
                .solarOffset(score.getSolarOffset())
                .lifestyleBonus(score.getLifestyleBonus())
                .totalCo2(score.getTotalCo2())
                .perCapitaCo2(score.getPerCapitaCo2())
                .zone(score.getZone() != null ? score.getZone().name() : null)
                .discountPercent(score.getDiscountPercent())
                .discountBreakdown(score.getDiscountBreakdown())
                .generatedAt(score.getGeneratedAt())
                .aiExplanation(score.getAiExplanation())
                .build();
    }
}
