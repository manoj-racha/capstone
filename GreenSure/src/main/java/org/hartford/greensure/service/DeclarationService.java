package org.hartford.greensure.service;

import org.hartford.greensure.dto.request.*;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeclarationService {

    @Autowired private CarbonDeclarationRepository declarationRepo;
    @Autowired private DeclarationVehicleRepository vehicleRepo;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;

    @Transactional
    public DeclarationResponse startDeclaration(Long userId) {
        int currentYear = java.time.LocalDate.now().getYear();

        if (declarationRepo.existsByUserUserIdAndDeclarationYear(userId, currentYear)) {
            CarbonDeclaration existing = declarationRepo
                    .findByUserUserIdAndDeclarationYear(userId, currentYear)
                    .orElseThrow();

            if (existing.getStatus() != CarbonDeclaration.DeclarationStatus.REJECTED) {
                throw new RuntimeException("You already have an active declaration for " + currentYear);
            }

            if (existing.getResubmissionCount() >= 3) {
                throw new RuntimeException("Maximum resubmissions reached. Contact admin to unlock your account.");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CarbonDeclaration declaration = CarbonDeclaration.builder()
                .user(user)
                .declarationYear(currentYear)
                .status(CarbonDeclaration.DeclarationStatus.DRAFT)
                .resubmissionCount(0)
                .build();

        declaration = declarationRepo.save(declaration);
        return mapToResponse(declaration);
    }

    @Transactional
    public DeclarationResponse saveDraft(Long declarationId, Long userId, DeclarationRequest request) {
        CarbonDeclaration declaration = getAndValidateDeclaration(declarationId, userId);

        if (declaration.getStatus() != CarbonDeclaration.DeclarationStatus.DRAFT) {
            throw new RuntimeException("Declaration cannot be edited in its current status: " + declaration.getStatus());
        }

        populateDeclarationFields(declaration, request);
        declaration = declarationRepo.save(declaration);
        return mapToResponse(declaration);
    }

    @Transactional
    public DeclarationResponse submitDeclaration(Long declarationId, Long userId) {
        CarbonDeclaration declaration = getAndValidateDeclaration(declarationId, userId);

        if (declaration.getStatus() != CarbonDeclaration.DeclarationStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT declarations can be submitted");
        }

        if (declaration.getElectricityUnits() == null) {
            throw new RuntimeException("Electricity units are required before submission");
        }

        declaration.setStatus(CarbonDeclaration.DeclarationStatus.SUBMITTED);
        declaration.setSubmittedAt(LocalDateTime.now());

        declarationRepo.save(declaration);

        notificationService.sendToUser(
                declaration.getUser().getUserId(),
                Notification.NotificationType.ASSIGNMENT_ALERT,
                "Your declaration has been submitted. A field agent will visit you within 72 hours.");

        return mapToResponse(declaration);
    }

    @Transactional
    public VehicleResponse addVehicle(Long declarationId, Long userId, VehicleRequest request) {
        CarbonDeclaration declaration = getAndValidateDeclaration(declarationId, userId);

        if (declaration.getStatus() != CarbonDeclaration.DeclarationStatus.DRAFT) {
            throw new RuntimeException("Vehicles can only be added to DRAFT declarations");
        }

        DeclarationVehicle vehicle = DeclarationVehicle.builder()
                .declaration(declaration)
                .vehicleType(request.getVehicleType())
                .fuelType(request.getFuelType())
                .kmPerMonth(request.getKmPerMonth())
                .quantity(request.getQuantity())
                .build();

        vehicle = vehicleRepo.save(vehicle);
        return mapVehicleToResponse(vehicle);
    }

    @Transactional
    public void removeVehicle(Long declarationId, Long userId, Long vehicleId) {
        CarbonDeclaration declaration = getAndValidateDeclaration(declarationId, userId);

        if (declaration.getStatus() != CarbonDeclaration.DeclarationStatus.DRAFT) {
            throw new RuntimeException("Vehicles can only be removed from DRAFT declarations");
        }

        DeclarationVehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!vehicle.getDeclaration().getDeclarationId().equals(declarationId)) {
            throw new RuntimeException("Vehicle does not belong to this declaration");
        }

        vehicleRepo.delete(vehicle);
    }

    public DeclarationResponse getDeclaration(Long declarationId, Long userId) {
        CarbonDeclaration declaration = getAndValidateDeclaration(declarationId, userId);
        return mapToResponse(declaration);
    }

    public List<DeclarationResponse> getHistory(Long userId) {
        return declarationRepo.findByUserUserIdOrderByDeclarationYearDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CarbonDeclaration getAndValidateDeclaration(Long declarationId, Long userId) {
        CarbonDeclaration declaration = declarationRepo.findById(declarationId)
                .orElseThrow(() -> new RuntimeException("Declaration not found"));

        if (!declaration.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Access denied — this declaration does not belong to you");
        }

        return declaration;
    }

    private void populateDeclarationFields(CarbonDeclaration d, DeclarationRequest r) {
        d.setElectricityUnits(r.getElectricityUnits());
        d.setHasSolar(r.getHasSolar());
        d.setSolarUnits(r.getSolarUnits());
        d.setCookingFuelType(r.getCookingFuelType());
        d.setLpgCylinders(r.getLpgCylinders());
        d.setPngUnits(r.getPngUnits());
        d.setBiomassKgPerDay(r.getBiomassKgPerDay());
        d.setNumAcUnits(r.getNumAcUnits());
        d.setAcHoursPerDay(r.getAcHoursPerDay());
        d.setHasGenerator(r.getHasGenerator());
        d.setGeneratorHoursPerMonth(r.getGeneratorHoursPerMonth());
        d.setUsesPublicTransport(r.getUsesPublicTransport());
        d.setPublicTransportKm(r.getPublicTransportKm());
        d.setDietaryPattern(r.getDietaryPattern());
        d.setShoppingOrdersPerMonth(r.getShoppingOrdersPerMonth());
        d.setHasCommercialVehicles(r.getHasCommercialVehicles());
        d.setCommercialVehicleKm(r.getCommercialVehicleKm());
        d.setThirdPartyShipments(r.getThirdPartyShipments());
        d.setEmployeesPrivateVehicle(r.getEmployeesPrivateVehicle());
        d.setEmployeesPublicTransport(r.getEmployeesPublicTransport());
        d.setGeneratorLitersPerMonth(r.getGeneratorLitersPerMonth());
        d.setHasBoiler(r.getHasBoiler());
        d.setBoilerFuelType(r.getBoilerFuelType());
        d.setBoilerCoalKg(r.getBoilerCoalKg());
        d.setBoilerGasScm(r.getBoilerGasScm());
        d.setPaperReamsPerMonth(r.getPaperReamsPerMonth());
        d.setUsesRecycledPaper(r.getUsesRecycledPaper());
        d.setRawMaterialType(r.getRawMaterialType());
        d.setRawMaterialKg(r.getRawMaterialKg());
    }

    public DeclarationResponse mapToResponse(CarbonDeclaration d) {
        List<VehicleResponse> vehicles = vehicleRepo.findByDeclarationDeclarationId(d.getDeclarationId())
                .stream()
                .map(this::mapVehicleToResponse)
                .collect(Collectors.toList());

        return DeclarationResponse.builder()
                .declarationId(d.getDeclarationId())
                .userId(d.getUser().getUserId())
                .declarationYear(d.getDeclarationYear())
                .status(d.getStatus())
                .resubmissionCount(d.getResubmissionCount())
                .submittedAt(d.getSubmittedAt())
                .createdAt(d.getCreatedAt())
                .electricityUnits(d.getElectricityUnits())
                .hasSolar(d.getHasSolar())
                .solarUnits(d.getSolarUnits())
                .cookingFuelType(d.getCookingFuelType())
                .lpgCylinders(d.getLpgCylinders())
                .pngUnits(d.getPngUnits())
                .biomassKgPerDay(d.getBiomassKgPerDay())
                .numAcUnits(d.getNumAcUnits())
                .acHoursPerDay(d.getAcHoursPerDay())
                .hasGenerator(d.getHasGenerator())
                .generatorHoursPerMonth(d.getGeneratorHoursPerMonth())
                .usesPublicTransport(d.getUsesPublicTransport())
                .publicTransportKm(d.getPublicTransportKm())
                .vehicles(vehicles)
                .dietaryPattern(d.getDietaryPattern())
                .shoppingOrdersPerMonth(d.getShoppingOrdersPerMonth())
                .hasCommercialVehicles(d.getHasCommercialVehicles())
                .commercialVehicleKm(d.getCommercialVehicleKm())
                .thirdPartyShipments(d.getThirdPartyShipments())
                .employeesPrivateVehicle(d.getEmployeesPrivateVehicle())
                .employeesPublicTransport(d.getEmployeesPublicTransport())
                .generatorLitersPerMonth(d.getGeneratorLitersPerMonth())
                .hasBoiler(d.getHasBoiler())
                .boilerFuelType(d.getBoilerFuelType())
                .boilerCoalKg(d.getBoilerCoalKg())
                .boilerGasScm(d.getBoilerGasScm())
                .paperReamsPerMonth(d.getPaperReamsPerMonth())
                .usesRecycledPaper(d.getUsesRecycledPaper())
                .rawMaterialType(d.getRawMaterialType())
                .rawMaterialKg(d.getRawMaterialKg())
                .build();
    }

    private VehicleResponse mapVehicleToResponse(DeclarationVehicle v) {
        return VehicleResponse.builder()
                .vehicleId(v.getVehicleId())
                .vehicleType(v.getVehicleType())
                .fuelType(v.getFuelType())
                .kmPerMonth(v.getKmPerMonth())
                .quantity(v.getQuantity())
                .build();
    }
}
