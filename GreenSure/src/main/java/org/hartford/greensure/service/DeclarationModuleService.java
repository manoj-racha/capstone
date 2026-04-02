package org.hartford.greensure.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.hartford.greensure.dto.response.DocumentExtractionResult;
import org.hartford.greensure.dto.response.ElectricityBillSummaryResponse;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.enums.CookingFuel;
import org.hartford.greensure.enums.DeclarationStatus;
import org.hartford.greensure.exception.*;
import org.hartford.greensure.dto.request.*;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.service.ai.VertexAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles all 7-module declaration data collection.
 * Each module is saved independently, allowing the user to fill
 * the form step-by-step and return later to complete it.
 *
 * Modules:
 * M2 — saveHouseholdData() — updates HouseholdProfile
 * M3 — saveVehicleData() — upserts DeclarationVehicleData
 * M4 — saveElectricityData() — upserts ElectricityData
 * M4b—addElectricityBill() — adds ElectricityBill, recalculates average
 * M5 — saveSolarData() — upserts SolarData (optional)
 * M6 — saveCookingData() — upserts CookingData
 * M7 — saveLifestyleData() — upserts LifestyleData (optional)
 * submitDeclaration() — validates mandatory modules present, triggers fraud
 * advisory
 * resubmitDeclaration() — after rejection, increments counter, resets status
 */
@Service
@Slf4j
public class DeclarationModuleService {

    @Autowired
    private CarbonDeclarationRepository declarationRepo;
    @Autowired
    private HouseholdProfileRepository householdProfileRepo;
    @Autowired
    private DeclarationVehicleDataRepository vehicleDataRepo;
    @Autowired
    private ElectricityDataRepository electricityDataRepo;
    @Autowired
    private ElectricityBillRepository electricityBillRepo;
    @Autowired
    private CookingDataRepository cookingDataRepo;
    @Autowired
    private SolarDataRepository solarDataRepo;
    @Autowired
    private LifestyleDataRepository lifestyleDataRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private FraudAdvisoryService fraudAdvisoryService;
    @Autowired
    private VertexAiService vertexAiService;
    @Autowired
    private ObjectMapper objectMapper;

    // ── Start Declaration ──────────────────────────────────────

    @Transactional
    public CarbonDeclaration startDeclaration(Long userId) {
        int currentYear = Year.now().getValue();

        if (declarationRepo.existsByUserUserIdAndDeclarationYear(userId, currentYear)) {
            // Return the existing DRAFT / REJECTED declaration instead of creating
            // duplicate
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

    @Autowired
    private VehicleDocumentRepository vehicleDocumentRepo;

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
    public org.hartford.greensure.dto.response.VehicleResponseDTO updateVehicle(
            Long declarationId,
            Long vehicleId,
            AddVehicleRequestDTO req) {
        getEditableDeclaration(declarationId);

        DeclarationVehicleData vehicle = vehicleDataRepo
                .findByVehicleDataIdAndDeclarationDeclarationId(vehicleId, declarationId)
                .orElseThrow(() -> new BadRequestException("Vehicle not found for declaration"));

        vehicle.setVehicleCategory(req.getVehicleCategory());
        vehicle.setVehicleNickname(req.getVehicleNickname());
        vehicle.setVin(req.getVin());
        vehicle.setRegistrationNumber(req.getRegistrationNumber());
        vehicle.setMake(req.getMake());
        vehicle.setModel(req.getModel());
        vehicle.setManufacturingYear(req.getYear());
        vehicle.setFuelType(req.getFuelType());
        vehicle.setMileageBand(req.getMileageBand());
        vehicle.setDataSource(req.getDataSource() != null
                ? req.getDataSource()
                : org.hartford.greensure.enums.DataSource.MANUAL);

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
    public void removeVehicleDocument(Long declarationId, Long vehicleId, Long documentId) {
        getEditableDeclaration(declarationId);

        VehicleDocument doc = vehicleDocumentRepo.findById(documentId)
                .orElseThrow(() -> new BadRequestException("Document not found"));

        if (doc.getVehicle() == null
                || doc.getVehicle().getVehicleDataId() == null
                || !doc.getVehicle().getVehicleDataId().equals(vehicleId)
                || doc.getVehicle().getDeclaration() == null
                || doc.getVehicle().getDeclaration().getDeclarationId() == null
                || !doc.getVehicle().getDeclaration().getDeclarationId().equals(declarationId)) {
            throw new BadRequestException("Document does not belong to the specified vehicle/declaration");
        }

        vehicleDocumentRepo.delete(doc);
    }

    @Transactional
    public void removeVehicle(Long vehicleId, Long declarationId) {
        getEditableDeclaration(declarationId); // Verify status is DRAFT
        vehicleDataRepo.findByVehicleDataIdAndDeclarationDeclarationId(vehicleId, declarationId)
                .orElseThrow(() -> new BadRequestException("Vehicle not found"));
        vehicleDataRepo.deleteByVehicleDataIdAndDeclarationDeclarationId(vehicleId, declarationId);
    }

    public java.util.List<org.hartford.greensure.dto.response.VehicleResponseDTO> getVehiclesForDeclaration(
            Long declarationId) {
        return vehicleDataRepo.findByDeclarationDeclarationId(declarationId)
                .stream()
                .map(this::mapToVehicleResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<String> getElectricityBillUrls(Long declarationId) {
        return electricityBillRepo.findByDeclarationDeclarationIdOrderByBillingMonthDesc(declarationId)
                .stream()
                .map(ElectricityBill::getBillUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
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

        java.util.List<org.hartford.greensure.dto.response.VehicleDocumentResponseDTO> docs = v.getDocuments() != null
                ? v.getDocuments().stream().map(this::mapToDocumentResponseDTO)
                        .collect(java.util.stream.Collectors.toList())
                : new java.util.ArrayList<>();
        dto.setDocuments(docs);
        return dto;
    }

    private org.hartford.greensure.dto.response.VehicleDocumentResponseDTO mapToDocumentResponseDTO(
            VehicleDocument doc) {
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

        DocumentExtractionResult aiResult = null;
        try {
            if (billUrl != null && !billUrl.isBlank()) {
                aiResult = vertexAiService.extractFromDocument(billUrl, "ELECTRICITY_BILL");
            }
        } catch (Exception e) {
            log.warn("AI document extraction failed for {}: {}", billUrl, e.getMessage());
        }

        Double kwh = unitsKwh;
        String month = billingMonth;
        Double amt = amount;
        double confidence = ocrConfidenceScore != null ? ocrConfidenceScore : 0.5;
        boolean billAnomaly = false;

        if (aiResult != null
                && aiResult.isSuccess()
                && aiResult.getConfidence() != null
                && aiResult.getConfidence() > 0.7) {
            // Preserve user-entered bill data; AI output is advisory-only here.
            confidence = aiResult.getConfidence();
        }
        if (aiResult != null
                && aiResult.getAnomalies() != null
                && !aiResult.getAnomalies().isEmpty()) {
            billAnomaly = true;
            mergeFraudFlags(declarationId, List.of("ELECTRICITY_BILL_ANOMALY"));
        }

        ElectricityBill bill = ElectricityBill.builder()
                .declaration(declaration)
                .billingMonth(month)
                .unitsKwh(kwh)
                .amount(amt)
                .billUrl(billUrl)
                .ocrConfidenceScore(confidence)
                .aiAnomalyFlag(billAnomaly)
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

        solarData = solarDataRepo.save(solarData);

        try {
            if (solarData.isHasSolar()
                    && solarData.getCertificateUrl() != null
                    && !solarData.getCertificateUrl().isBlank()) {
                DocumentExtractionResult r = vertexAiService.extractFromDocument(
                        solarData.getCertificateUrl(), "SOLAR_CERTIFICATE");
                if (r != null
                        && r.isSuccess()
                        && r.getConfidence() != null
                        && r.getConfidence() > 0.7
                        && r.getInstalledCapacityKw() != null
                        && solarData.getCapacityKw() != null
                        && Math.abs(r.getInstalledCapacityKw() - solarData.getCapacityKw()) > 0.5) {
                    mergeFraudFlags(declarationId, List.of("SOLAR_CERT_CAPACITY_MISMATCH"));
                }
                if (r != null && r.getAnomalies() != null && !r.getAnomalies().isEmpty()) {
                    mergeFraudFlags(declarationId, List.of("SOLAR_CERT_ANOMALY"));
                }
            }
        } catch (Exception e) {
            log.warn("Solar certificate AI failed: {}", e.getMessage());
        }

        return solarData;
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

        cookingData = cookingDataRepo.save(cookingData);

        try {
            processCookingBillAi(declarationId, cookingData);
        } catch (Exception e) {
            log.warn("Cooking receipt AI failed: {}", e.getMessage());
        }

        return cookingDataRepo.findByDeclarationDeclarationId(declarationId).orElse(cookingData);
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

    @Autowired
    private CarbonScoreRepository scoreRepository;
    @Autowired
    private VerificationRepository verificationRepo;

    public org.hartford.greensure.dto.response.DeclarationDetailResponse getDeclarationDetail(Long id) {
        CarbonDeclaration d = declarationRepo.findById(id)
                .orElseThrow(() -> new DeclarationNotFoundException("Declaration not found"));

        org.hartford.greensure.dto.response.DeclarationDetailResponse.DeclarationDetailResponseBuilder b = org.hartford.greensure.dto.response.DeclarationDetailResponse
                .builder()
                .declarationId(d.getDeclarationId())
                .userId(d.getUser().getUserId())
                .fullName(d.getUser().getFullName())
                .declarationYear(d.getDeclarationYear())
                .status(d.getStatus())
                .resubmissionCount(d.getResubmissionCount())
                .submittedAt(d.getSubmittedAt())
                .createdAt(d.getCreatedAt());

        householdProfileRepo.findByUserUserId(d.getUser().getUserId())
                .ifPresent(h -> b.householdMembers(h.getNumberOfMembers()));

        b.vehicles(getVehiclesForDeclaration(id));

        electricityDataRepo.findByDeclarationDeclarationId(id).ifPresent(e -> b.provider(e.getProvider())
                .consumerNumber(e.getConsumerNumber())
                .userDeclaredMonthlyKwh(e.getUserDeclaredMonthlyKwh())
                .ocrComputedMonthlyKwh(e.getOcrComputedMonthlyKwh())
                .billsUploaded(e.getBillsUploaded()));

        try {
            b.electricityBillUrls(
                    electricityBillRepo.findByDeclarationDeclarationIdOrderByBillingMonthDesc(id)
                            .stream()
                            .map(ElectricityBill::getBillUrl)
                            .filter(url -> url != null && !url.isBlank())
                            .toList());
        } catch (Exception e) {
            log.warn("Could not load electricity bill URLs for declaration {}: {}", id, e.getMessage());
            b.electricityBillUrls(List.of());
        }

        try {
            b.electricityBills(
                    electricityBillRepo.findByDeclarationDeclarationIdOrderByBillingMonthDesc(id).stream()
                            .map(
                                    bill -> ElectricityBillSummaryResponse.builder()
                                            .billingMonth(bill.getBillingMonth())
                                            .unitsKwh(bill.getUnitsKwh())
                                            .amount(bill.getAmount())
                                            .billUrl(bill.getBillUrl())
                                            .ocrConfidenceScore(bill.getOcrConfidenceScore())
                                            .aiAnomalyFlag(Boolean.TRUE.equals(bill.getAiAnomalyFlag()))
                                            .build())
                            .toList());
        } catch (Exception e) {
            log.warn("Could not load electricity bill rows for declaration {}: {}", id, e.getMessage());
            b.electricityBills(List.of());
        }

        solarDataRepo.findByDeclarationDeclarationId(id).ifPresent(s -> b.hasSolar(s.isHasSolar())
                .solarCapacityKw(s.getEffectiveCapacityKw())
                .certificateUrl(s.getCertificateUrl())
                .mnreVerified(s.isMnreVerified()));

        cookingDataRepo.findByDeclarationDeclarationId(id).ifPresent(c -> b.cookingFuelType(c.getEffectiveFuelType())
                .pngConsumerNumber(c.getPngConsumerNumber())
                .userDeclaredCylinders(c.getUserDeclaredCylinders())
                .billUrls(c.getBillUrls())
                .cylinders(c.getEffectiveCylinders()));

        lifestyleDataRepo.findByDeclarationDeclarationId(id)
                .ifPresent(l -> b.publicTransportUsage(l.getPublicTransportUsage())
                        .wastesRecycling(l.isWastesRecycling()));

        scoreRepository.findByDeclarationDeclarationId(id)
                .ifPresent(s -> b.carbonScore(org.hartford.greensure.dto.response.CarbonScoreResponse.builder()
                        .scoreId(s.getScoreId())
                        .scoreYear(s.getScoreYear())
                        .vehicleCo2(s.getVehicleCo2())
                        .electricityCo2(s.getElectricityCo2())
                        .cookingCo2(s.getCookingCo2())
                        .solarOffset(s.getSolarOffset())
                        .lifestyleBonus(s.getLifestyleBonus())
                        .totalCo2(s.getTotalCo2())
                        .perCapitaCo2(s.getPerCapitaCo2())
                        .zone(s.getZone() != null ? s.getZone().name() : null)
                        .discountPercent(s.getDiscountPercent())
                        .discountBreakdown(s.getDiscountBreakdown())
                        .aiExplanation(s.getAiExplanation())
                        .build()));

        verificationRepo.findByDeclarationDeclarationId(id).ifPresent(v -> {
            b.verificationOutcome(v.getOutcome() != null ? v.getOutcome().name() : null)
                    .rejectionReason(v.getRejectionReason())
                    .agentNotes(v.getAgentNotes());
        });

        return b.build();
    }

    private void mergeFraudFlags(Long declarationId, List<String> newTokens) {
        try {
            CarbonDeclaration d = declarationRepo.findById(declarationId).orElse(null);
            if (d == null) {
                return;
            }
            Set<String> set = new LinkedHashSet<>();
            if (d.getFraudAdvisoryFlags() != null && !d.getFraudAdvisoryFlags().isBlank()) {
                for (String s : d.getFraudAdvisoryFlags().split(",")) {
                    if (!s.isBlank()) {
                        set.add(s.trim());
                    }
                }
            }
            for (String t : newTokens) {
                if (t != null && !t.isBlank()) {
                    set.add(t.trim());
                }
            }
            d.setFraudAdvisoryFlags(String.join(",", set));
            int score = set.size();
            d.setFraudAdvisoryScore(score);
            d.setFraudRiskLevel(score >= 4 ? "HIGH" : score >= 2 ? "MEDIUM" : "LOW");
            declarationRepo.save(d);
        } catch (Exception e) {
            log.warn("mergeFraudFlags failed: {}", e.getMessage());
        }
    }

    private void processCookingBillAi(Long declarationId, CookingData cooking) {
        List<String> urls = parseCookingBillUrls(cooking.getBillUrls());
        if (urls.isEmpty()) {
            return;
        }
        CookingFuel fuel = cooking.getEffectiveFuelType();
        if (fuel == null) {
            return;
        }

        int cylinderSum = 0;
        boolean anyLpgReceipt = false;

        for (String url : urls) {
            DocumentExtractionResult r = null;
            try {
                if (fuel == CookingFuel.LPG) {
                    r = vertexAiService.extractFromDocument(url, "LPG_RECEIPT");
                } else if (fuel == CookingFuel.PNG) {
                    r = vertexAiService.extractFromDocument(url, "PNG_BILL");
                }
            } catch (Exception e) {
                log.warn("Cooking AI extraction failed for {}: {}", url, e.getMessage());
            }
            if (r == null || !r.isSuccess()) {
                continue;
            }
            if (r.getAnomalies() != null && !r.getAnomalies().isEmpty()) {
                if (fuel == CookingFuel.LPG) {
                    mergeFraudFlags(declarationId, List.of("LPG_RECEIPT_ANOMALY"));
                } else if (fuel == CookingFuel.PNG) {
                    mergeFraudFlags(declarationId, List.of("PNG_BILL_ANOMALY"));
                }
            }
            if (fuel == CookingFuel.LPG && r.getCylinderCount() != null) {
                cylinderSum += r.getCylinderCount();
                anyLpgReceipt = true;
            }
        }

        if (anyLpgReceipt && cylinderSum > 0) {
            cooking.setOcrComputedCylinders(cylinderSum);
            if (cooking.getUserDeclaredCylinders() != null
                    && Math.abs(cylinderSum - cooking.getUserDeclaredCylinders()) > 0) {
                mergeFraudFlags(declarationId, List.of("LPG_CYLINDER_COUNT_MISMATCH"));
            }
            cookingDataRepo.save(cooking);
        }
    }

    private List<String> parseCookingBillUrls(String billUrlsJson) {
        if (billUrlsJson == null || billUrlsJson.isBlank()) {
            return List.of();
        }
        try {
            if (billUrlsJson.trim().startsWith("[")) {
                return objectMapper.readValue(billUrlsJson, new TypeReference<>() {
                });
            }
        } catch (Exception ignored) {
        }
        String urls = billUrlsJson.replaceAll("[\\[\\]\"]", "");
        List<String> out = new ArrayList<>();
        for (String u : urls.split(",")) {
            if (u != null && !u.isBlank()) {
                out.add(u.trim());
            }
        }
        return out;
    }

    private CarbonDeclaration getEditableDeclaration(Long declarationId) {
        CarbonDeclaration declaration = declarationRepo.findById(declarationId)
                .orElseThrow(() -> new DeclarationNotFoundException("Declaration not found"));
        if (!declaration.getStatus().isEditable()) {
            throw new DeclarationNotEditableException(
                    "Declaration cannot be edited in status: " + declaration.getStatus());
        }
        return declaration;
    }
}
