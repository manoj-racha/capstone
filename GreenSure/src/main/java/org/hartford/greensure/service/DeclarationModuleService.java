package org.hartford.greensure.service;

import org.hartford.greensure.entity.*;
import org.hartford.greensure.enums.DeclarationStatus;
import org.hartford.greensure.exception.*;
import org.hartford.greensure.dto.request.*;
import org.hartford.greensure.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;

/**
 * Handles all 7-module declaration data collection.
 * Each module is saved independently, allowing the user to fill
 * the form step-by-step and return later to complete it.
 *
 * Modules:
 *   M2 — saveHouseholdData()     — updates HouseholdProfile
 *   M3 — saveVehicleData()       — upserts DeclarationVehicleData
 *   M4 — saveElectricityData()   — upserts ElectricityData
 *   M4b—addElectricityBill()    — adds ElectricityBill, recalculates average
 *   M5 — saveSolarData()         — upserts SolarData (optional)
 *   M6 — saveCookingData()       — upserts CookingData
 *   M7 — saveLifestyleData()     — upserts LifestyleData (optional)
 *   submitDeclaration()          — validates mandatory modules present, triggers fraud advisory
 *   resubmitDeclaration()        — after rejection, increments counter, resets status
 */
@Service
public class DeclarationModuleService {

    @Autowired private CarbonDeclarationRepository declarationRepo;
    @Autowired private HouseholdProfileRepository householdProfileRepo;
    @Autowired private DeclarationVehicleDataRepository vehicleDataRepo;
    @Autowired private ElectricityDataRepository electricityDataRepo;
    @Autowired private ElectricityBillRepository electricityBillRepo;
    @Autowired private CookingDataRepository cookingDataRepo;
    @Autowired private SolarDataRepository solarDataRepo;
    @Autowired private LifestyleDataRepository lifestyleDataRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private FraudAdvisoryService fraudAdvisoryService;

    // ── Start Declaration ──────────────────────────────────────

    @Transactional
    public CarbonDeclaration startDeclaration(Long userId) {
        int currentYear = Year.now().getValue();

        if (declarationRepo.existsByUserUserIdAndDeclarationYear(userId, currentYear)) {
            // Return the existing DRAFT / REJECTED declaration instead of creating duplicate
            return declarationRepo.findByUserUserIdAndDeclarationYear(userId, currentYear)
                    .filter(d -> d.getStatus().isEditable())
                    .orElseThrow(() -> new DuplicateDeclarationException(
                            "A declaration for " + currentYear + " already exists."));
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        CarbonDeclaration declaration = CarbonDeclaration.builder()
                .user(user)
                .declarationYear(currentYear)
                .status(DeclarationStatus.DRAFT)
                .build();

        return declarationRepo.save(declaration);
    }

    // ── Module 2 — Household Data ──────────────────────────────

    @Transactional
    public void saveHouseholdData(Long userId, HouseholdDataRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        HouseholdProfile profile = householdProfileRepo.findByUserUserId(userId)
                .orElse(HouseholdProfile.builder().user(user).build());

        profile.setNumberOfMembers(req.getNumberOfMembers());
        householdProfileRepo.save(profile);
    }

    // ── Module 3 — Vehicle Data ────────────────────────────────

    @Autowired private VehicleDocumentRepository vehicleDocumentRepo;

    @Transactional
    public org.hartford.greensure.dto.response.VehicleResponseDTO addVehicle(
            Long declarationId, AddVehicleRequestDTO req) {
        CarbonDeclaration declaration = getEditableDeclaration(declarationId);

        DeclarationVehicleData vehicle = DeclarationVehicleData.builder()
                .declaration(declaration)
                .vehicleCategory(req.getVehicleCategory())
                .vehicleNickname(req.getVehicleNickname())
                .vin(req.getVin())
                .registrationNumber(req.getRegistrationNumber())
                .make(req.getMake())
                .model(req.getModel())
                .manufacturingYear(req.getYear())
                .fuelType(req.getFuelType())
                .mileageBand(req.getMileageBand())
                .dataSource(req.getDataSource() != null
                        ? req.getDataSource()
                        : org.hartford.greensure.enums.DataSource.MANUAL)
                .build();

        vehicle = vehicleDataRepo.save(vehicle);
        return mapToVehicleResponseDTO(vehicle);
    }

    @Transactional
    public org.hartford.greensure.dto.response.VehicleDocumentResponseDTO addVehicleDocument(
            Long vehicleId, Long declarationId, UploadVehicleDocumentRequestDTO req) {

        DeclarationVehicleData vehicle = vehicleDataRepo
                .findByVehicleDataIdAndDeclarationDeclarationId(vehicleId, declarationId)
                .orElseThrow(() -> new BadRequestException("Vehicle not found for declaration"));
                
        getEditableDeclaration(declarationId); // Ensure draft status

        VehicleDocument doc = VehicleDocument.builder()
                .vehicle(vehicle)
                .documentType(req.getDocumentType())
                .documentUrl(req.getDocumentUrl())
                .originalFileName(req.getOriginalFileName())
                .mimeType(req.getMimeType())
                .fileSizeBytes(req.getFileSizeBytes())
                .build();

        doc = vehicleDocumentRepo.save(doc);
        return mapToDocumentResponseDTO(doc);
    }

    @Transactional
    public void removeVehicle(Long vehicleId, Long declarationId) {
        getEditableDeclaration(declarationId); // Verify status is DRAFT
        vehicleDataRepo.findByVehicleDataIdAndDeclarationDeclarationId(vehicleId, declarationId)
                .orElseThrow(() -> new BadRequestException("Vehicle not found"));
        vehicleDataRepo.deleteByVehicleDataIdAndDeclarationDeclarationId(vehicleId, declarationId);
    }

    public java.util.List<org.hartford.greensure.dto.response.VehicleResponseDTO> getVehiclesForDeclaration(Long declarationId) {
        return vehicleDataRepo.findByDeclarationDeclarationId(declarationId)
                .stream()
                .map(this::mapToVehicleResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    private org.hartford.greensure.dto.response.VehicleResponseDTO mapToVehicleResponseDTO(DeclarationVehicleData v) {
        org.hartford.greensure.dto.response.VehicleResponseDTO dto = new org.hartford.greensure.dto.response.VehicleResponseDTO();
        dto.setVehicleId(v.getVehicleDataId());
        dto.setVehicleCategory(v.getVehicleCategory());
        dto.setVehicleNickname(v.getVehicleNickname());
        dto.setRegistrationNumber(v.getRegistrationNumber());
        dto.setMake(v.getMake());
        dto.setModel(v.getModel());
        dto.setYear(v.getManufacturingYear());
        dto.setFuelType(v.getFuelType() != null ? v.getFuelType().name() : null);
        dto.setMileageBand(v.getMileageBand() != null ? v.getMileageBand().name() : null);
        dto.setDataSource(v.getDataSource() != null ? v.getDataSource().name() : null);

        java.util.List<org.hartford.greensure.dto.response.VehicleDocumentResponseDTO> docs = 
                v.getDocuments() != null ? v.getDocuments().stream().map(this::mapToDocumentResponseDTO).collect(java.util.stream.Collectors.toList()) : new java.util.ArrayList<>();
        dto.setDocuments(docs);
        return dto;
    }

    private org.hartford.greensure.dto.response.VehicleDocumentResponseDTO mapToDocumentResponseDTO(VehicleDocument doc) {
        org.hartford.greensure.dto.response.VehicleDocumentResponseDTO dto = new org.hartford.greensure.dto.response.VehicleDocumentResponseDTO();
        dto.setDocumentId(doc.getId());
        dto.setDocumentType(doc.getDocumentType() != null ? doc.getDocumentType().name() : null);
        dto.setDocumentUrl(doc.getDocumentUrl());
        dto.setOriginalFileName(doc.getOriginalFileName());
        dto.setMimeType(doc.getMimeType());
        dto.setFileSizeBytes(doc.getFileSizeBytes());
        dto.setVerified(doc.isVerified());
        dto.setAgentNote(doc.getAgentNote());
        dto.setUploadedAt(doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : null);
        return dto;
    }

    // ── Module 4a — Electricity Data ───────────────────────────

    @Transactional
    public ElectricityData saveElectricityData(Long declarationId, ElectricityDataRequest req) {
        CarbonDeclaration declaration = getEditableDeclaration(declarationId);

        ElectricityData data = electricityDataRepo
                .findByDeclarationDeclarationId(declarationId)
                .orElse(ElectricityData.builder().declaration(declaration).build());

        data.setProvider(req.getProvider());
        data.setConsumerNumber(req.getConsumerNumber());
        data.setUserDeclaredMonthlyKwh(req.getUserDeclaredMonthlyKwh());

        return electricityDataRepo.save(data);
    }

    // ── Module 4b — Add Electricity Bill ──────────────────────

    @Transactional
    public ElectricityBill addElectricityBill(Long declarationId,
                                               String billingMonth,
                                               Double unitsKwh,
                                               Double amount,
                                               String billUrl,
                                               Double ocrConfidenceScore) {
        CarbonDeclaration declaration = getEditableDeclaration(declarationId);

        ElectricityBill bill = ElectricityBill.builder()
                .declaration(declaration)
                .billingMonth(billingMonth)
                .unitsKwh(unitsKwh)
                .amount(amount)
                .billUrl(billUrl)
                .ocrConfidenceScore(ocrConfidenceScore)
                .build();

        bill = electricityBillRepo.save(bill);

        // Recalculate average and update ElectricityData
        double avg = electricityBillRepo.calculateAverageKwh(declarationId).orElse(unitsKwh);
        long count = electricityBillRepo.countByDeclarationDeclarationId(declarationId);

        ElectricityData electricityData = electricityDataRepo
                .findByDeclarationDeclarationId(declarationId)
                .orElse(ElectricityData.builder().declaration(declaration).build());

        electricityData.setOcrComputedMonthlyKwh(avg);
        electricityData.setBillsUploaded((int) count);
        electricityDataRepo.save(electricityData);

        return bill;
    }

    // ── Module 5 — Solar Data ──────────────────────────────────

    @Transactional
    public SolarData saveSolarData(Long declarationId, SolarDataRequest req) {
        CarbonDeclaration declaration = getEditableDeclaration(declarationId);

        SolarData solarData = solarDataRepo
                .findByDeclarationDeclarationId(declarationId)
                .orElse(SolarData.builder().declaration(declaration).build());

        solarData.setHasSolar(Boolean.TRUE.equals(req.getHasSolar()));
        if (Boolean.TRUE.equals(req.getHasSolar())) {
            solarData.setCapacityKw(req.getCapacityKw());
            solarData.setCertificateUrl(req.getCertificateUrl());
        }

        return solarDataRepo.save(solarData);
    }

    // ── Module 6 — Cooking Data ────────────────────────────────

    @Transactional
    public CookingData saveCookingData(Long declarationId, CookingDataRequest req) {
        CarbonDeclaration declaration = getEditableDeclaration(declarationId);

        CookingData cookingData = cookingDataRepo
                .findByDeclarationDeclarationId(declarationId)
                .orElse(CookingData.builder().declaration(declaration).build());

        cookingData.setFuelType(req.getFuelType());
        cookingData.setPngConsumerNumber(req.getPngConsumerNumber());
        cookingData.setUserDeclaredCylinders(req.getUserDeclaredCylinders());
        cookingData.setBillUrls(req.getBillUrls());

        return cookingDataRepo.save(cookingData);
    }

    // ── Module 7 — Lifestyle Data (optional) ──────────────────

    @Transactional
    public LifestyleData saveLifestyleData(Long declarationId, LifestyleDataRequest req) {
        CarbonDeclaration declaration = getEditableDeclaration(declarationId);

        LifestyleData lifestyleData = lifestyleDataRepo
                .findByDeclarationDeclarationId(declarationId)
                .orElse(LifestyleData.builder().declaration(declaration).build());

        lifestyleData.setPublicTransportUsage(
                req.getPublicTransportUsage() != null
                        ? req.getPublicTransportUsage()
                        : org.hartford.greensure.enums.PublicTransportUsage.NEVER);
        lifestyleData.setWastesRecycling(req.isWastesRecycling());

        return lifestyleDataRepo.save(lifestyleData);
    }

    // ── Submit Declaration ─────────────────────────────────────

    /**
     * Validates that all mandatory modules are present, runs fraud advisory,
     * and transitions status to SUBMITTED.
     */
    @Transactional
    public CarbonDeclaration submitDeclaration(Long declarationId, Long userId) {
        CarbonDeclaration declaration = getEditableDeclaration(declarationId);

        // Verify it belongs to this user
        if (!declaration.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }

        // Validate mandatory modules present
        java.util.List<DeclarationVehicleData> vehicles = vehicleDataRepo.findByDeclarationDeclarationId(declarationId);
        if (vehicles.isEmpty()) {
            throw new VehicleDataNotFoundException("At least one vehicle is required before submitting.");
        }
        if (!electricityDataRepo.existsByDeclarationDeclarationId(declarationId)) {
            throw new BadRequestException("Electricity data (Module 4) is required before submitting.");
        }
        if (!cookingDataRepo.existsByDeclarationDeclarationId(declarationId)) {
            throw new BadRequestException("Cooking data (Module 6) is required before submitting.");
        }
        if (!householdProfileRepo.existsByUserUserId(userId)) {
            throw new BadRequestException("Household data (Module 2) is required before submitting.");
        }

        // Run fraud advisory (advisory-only — never blocks submission)
        var result = fraudAdvisoryService.analyze(declarationId);
        declaration.setFraudAdvisoryScore(result.score());
        declaration.setFraudAdvisoryFlags(String.join(",", result.flags()));
        declaration.setFraudRiskLevel(result.riskLevel());

        declaration.setStatus(DeclarationStatus.SUBMITTED);
        declaration.setSubmittedAt(LocalDateTime.now());

        return declarationRepo.save(declaration);
    }

    // ── Resubmit after Rejection ───────────────────────────────

    @Transactional
    public CarbonDeclaration resubmitDeclaration(Long declarationId, Long userId) {
        CarbonDeclaration declaration = declarationRepo.findById(declarationId)
                .orElseThrow(() -> new DeclarationNotFoundException("Declaration not found"));

        if (!declaration.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }

        if (declaration.getStatus() != DeclarationStatus.REJECTED) {
            throw new DeclarationNotEditableException(
                    "Only REJECTED declarations can be resubmitted.");
        }

        if (declaration.getResubmissionCount() >= 3) {
            throw new MaxResubmissionLimitExceededException(
                    "Maximum resubmission limit (3) reached.");
        }

        declaration.setResubmissionCount(declaration.getResubmissionCount() + 1);
        declaration.setStatus(DeclarationStatus.SUBMITTED);
        declaration.setSubmittedAt(LocalDateTime.now());

        // Re-run fraud advisory with corrected data
        var result = fraudAdvisoryService.analyze(declarationId);
        declaration.setFraudAdvisoryScore(result.score());
        declaration.setFraudAdvisoryFlags(String.join(",", result.flags()));
        declaration.setFraudRiskLevel(result.riskLevel());

        return declarationRepo.save(declaration);
    }

    // ── Private helpers ─────────────────────────────────────────

    private CarbonDeclaration getEditableDeclaration(Long declarationId) {
        CarbonDeclaration declaration = declarationRepo.findById(declarationId)
                .orElseThrow(() -> new DeclarationNotFoundException("Declaration not found"));
        if (!declaration.getStatus().isEditable()) {
            throw new DeclarationNotEditableException(
                    "Declaration is not editable in status: " + declaration.getStatus());
        }
        return declaration;
    }
}
