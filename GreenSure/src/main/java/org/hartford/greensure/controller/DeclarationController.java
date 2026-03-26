package org.hartford.greensure.controller;

import jakarta.validation.Valid;
import org.hartford.greensure.dto.request.*;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.service.DeclarationModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * All declaration lifecycle endpoints for the USER role.
 *
 * POST   /declaration/start                  — start a new declaration
 * PUT    /declaration/{id}/household         — save Module 2
 * PUT    /declaration/{id}/vehicle           — save Module 3
 * PUT    /declaration/{id}/electricity       — save Module 4
 * POST   /declaration/{id}/electricity/bills — add a bill OCR record
 * PUT    /declaration/{id}/solar             — save Module 5 (optional)
 * PUT    /declaration/{id}/cooking           — save Module 6
 * PUT    /declaration/{id}/lifestyle         — save Module 7 (optional)
 * POST   /declaration/{id}/submit            — submit for verification
 * POST   /declaration/{id}/resubmit          — resubmit after rejection
 * GET    /declaration/history                — list own declarations
 * GET    /declaration/{id}                   — get full detail
 */
@RestController
@RequestMapping("/declaration")
public class DeclarationController {

    @Autowired private DeclarationModuleService moduleService;
    @Autowired private CarbonDeclarationRepository declarationRepo;
    @Autowired private CarbonScoreRepository scoreRepository;
    @Autowired private VerificationRepository verificationRepo;
    @Autowired private HouseholdProfileRepository householdRepo;
    @Autowired private DeclarationVehicleDataRepository vehicleDataRepo;
    @Autowired private ElectricityDataRepository electricityDataRepo;
    @Autowired private CookingDataRepository cookingDataRepo;
    @Autowired private SolarDataRepository solarDataRepo;
    @Autowired private LifestyleDataRepository lifestyleDataRepo;

    private Long userId(Authentication auth) {
        return ((org.hartford.greensure.security.SecurityUser) auth.getPrincipal()).getId();
    }

    // ── Start ──────────────────────────────────────────────────

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Long>> start(Authentication auth) {
        CarbonDeclaration d = moduleService.startDeclaration(userId(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Declaration started", d.getDeclarationId()));
    }

    // ── Module 2 — Household ───────────────────────────────────

    @PutMapping("/{id}/household")
    public ResponseEntity<ApiResponse<Void>> saveHousehold(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody HouseholdDataRequest req) {
        moduleService.saveHouseholdData(userId(auth), req);
        return ResponseEntity.ok(ApiResponse.success("Household data saved"));
    }

    @PostMapping("/{id}/vehicles")
    public ResponseEntity<ApiResponse<org.hartford.greensure.dto.response.VehicleResponseDTO>> addVehicle(
            @PathVariable Long id,
            @Valid @RequestBody AddVehicleRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle added", moduleService.addVehicle(id, req)));
    }

    @PostMapping("/{id}/vehicles/{vehicleId}/documents")
    public ResponseEntity<ApiResponse<org.hartford.greensure.dto.response.VehicleDocumentResponseDTO>> addVehicleDocument(
            @PathVariable Long id,
            @PathVariable Long vehicleId,
            @Valid @RequestBody UploadVehicleDocumentRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded", moduleService.addVehicleDocument(vehicleId, id, req)));
    }

    @DeleteMapping("/{id}/vehicles/{vehicleId}")
    public ResponseEntity<Void> removeVehicle(
            @PathVariable Long id,
            @PathVariable Long vehicleId) {
        moduleService.removeVehicle(vehicleId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/vehicles")
    public ResponseEntity<ApiResponse<List<org.hartford.greensure.dto.response.VehicleResponseDTO>>> getVehiclesForDeclaration(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Vehicles retrieved", moduleService.getVehiclesForDeclaration(id)));
    }

    // ── Module 4 — Electricity ─────────────────────────────────

    @PutMapping("/{id}/electricity")
    public ResponseEntity<ApiResponse<Void>> saveElectricity(
            @PathVariable Long id,
            @Valid @RequestBody ElectricityDataRequest req) {
        moduleService.saveElectricityData(id, req);
        return ResponseEntity.ok(ApiResponse.success("Electricity data saved"));
    }

    @PostMapping("/{id}/electricity/bills")
    public ResponseEntity<ApiResponse<Void>> addBill(
            @PathVariable Long id,
            @RequestParam String billingMonth,
            @RequestParam Double unitsKwh,
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String billUrl,
            @RequestParam(defaultValue = "1.0") Double confidence) {
        moduleService.addElectricityBill(id, billingMonth, unitsKwh, amount, billUrl, confidence);
        return ResponseEntity.ok(ApiResponse.success("Bill added and average kWh updated."));
    }

    // ── Module 5 — Solar ───────────────────────────────────────

    @PutMapping("/{id}/solar")
    public ResponseEntity<ApiResponse<Void>> saveSolar(
            @PathVariable Long id,
            @Valid @RequestBody SolarDataRequest req) {
        moduleService.saveSolarData(id, req);
        return ResponseEntity.ok(ApiResponse.success("Solar data saved"));
    }

    // ── Module 6 — Cooking ─────────────────────────────────────

    @PutMapping("/{id}/cooking")
    public ResponseEntity<ApiResponse<Void>> saveCooking(
            @PathVariable Long id,
            @Valid @RequestBody CookingDataRequest req) {
        moduleService.saveCookingData(id, req);
        return ResponseEntity.ok(ApiResponse.success("Cooking data saved"));
    }

    // ── Module 7 — Lifestyle ───────────────────────────────────

    @PutMapping("/{id}/lifestyle")
    public ResponseEntity<ApiResponse<Void>> saveLifestyle(
            @PathVariable Long id,
            @RequestBody LifestyleDataRequest req) {
        moduleService.saveLifestyleData(id, req);
        return ResponseEntity.ok(ApiResponse.success("Lifestyle data saved"));
    }

    // ── Submit ─────────────────────────────────────────────────

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<Void>> submit(
            Authentication auth,
            @PathVariable Long id) {
        moduleService.submitDeclaration(id, userId(auth));
        return ResponseEntity.ok(ApiResponse.success(
                "Declaration submitted. A field agent will be assigned within 48 hours."));
    }

    // ── Resubmit ───────────────────────────────────────────────

    @PostMapping("/{id}/resubmit")
    public ResponseEntity<ApiResponse<Void>> resubmit(
            Authentication auth,
            @PathVariable Long id) {
        moduleService.resubmitDeclaration(id, userId(auth));
        return ResponseEntity.ok(ApiResponse.success("Declaration resubmitted."));
    }

    // ── History ────────────────────────────────────────────────

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<DeclarationSummaryResponse>>> history(
            Authentication auth) {
        List<DeclarationSummaryResponse> list = declarationRepo
                .findByUserUserIdOrderByDeclarationYearDesc(userId(auth))
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Declarations retrieved", list));
    }

    // ── Detail ─────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeclarationDetailResponse>> detail(
            Authentication auth,
            @PathVariable Long id) {
        
        org.hartford.greensure.security.SecurityUser userAuth = 
            (org.hartford.greensure.security.SecurityUser) auth.getPrincipal();

        boolean isAdmin = userAuth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        CarbonDeclaration d = declarationRepo.findById(id)
                .filter(decl -> isAdmin || decl.getUser().getUserId().equals(userAuth.getId()))
                .orElseThrow(() -> new org.hartford.greensure.exception.DeclarationNotFoundException(
                        "Declaration not found"));
        return ResponseEntity.ok(ApiResponse.success("Declaration details", toDetail(d)));
    }

    // ── Private mappers ────────────────────────────────────────

    private DeclarationSummaryResponse toSummary(CarbonDeclaration d) {
        DeclarationSummaryResponse.DeclarationSummaryResponseBuilder b =
                DeclarationSummaryResponse.builder()
                        .declarationId(d.getDeclarationId())
                        .userId(d.getUser().getUserId())
                        .fullName(d.getUser().getFullName())
                        .declarationYear(d.getDeclarationYear())
                        .status(d.getStatus())
                        .submittedAt(d.getSubmittedAt())
                        .createdAt(d.getCreatedAt());

        scoreRepository.findByDeclarationDeclarationId(d.getDeclarationId())
                .ifPresent(s -> b.totalCo2(s.getTotalCo2())
                        .perCapitaCo2(s.getPerCapitaCo2())
                        .zone(s.getZone() != null ? s.getZone().name() : null)
                        .discountPercent(s.getDiscountPercent()));

        return b.build();
    }

    private DeclarationDetailResponse toDetail(CarbonDeclaration d) {
        Long did = d.getDeclarationId();
        DeclarationDetailResponse.DeclarationDetailResponseBuilder b =
                DeclarationDetailResponse.builder()
                        .declarationId(did)
                        .userId(d.getUser().getUserId())
                        .fullName(d.getUser().getFullName())
                        .declarationYear(d.getDeclarationYear())
                        .status(d.getStatus())
                        .resubmissionCount(d.getResubmissionCount())
                        .submittedAt(d.getSubmittedAt())
                        .createdAt(d.getCreatedAt());

        householdRepo.findByUserUserId(d.getUser().getUserId())
                .ifPresent(h -> b.householdMembers(h.getNumberOfMembers()));

        b.vehicles(moduleService.getVehiclesForDeclaration(did));

        electricityDataRepo.findByDeclarationDeclarationId(did).ifPresent(e ->
                b.provider(e.getProvider())
                 .userDeclaredMonthlyKwh(e.getUserDeclaredMonthlyKwh())
                 .ocrComputedMonthlyKwh(e.getOcrComputedMonthlyKwh())
                 .billsUploaded(e.getBillsUploaded()));

        solarDataRepo.findByDeclarationDeclarationId(did).ifPresent(s ->
                b.hasSolar(s.isHasSolar())
                 .solarCapacityKw(s.getEffectiveCapacityKw())
                 .mnreVerified(s.isMnreVerified()));

        cookingDataRepo.findByDeclarationDeclarationId(did).ifPresent(c ->
                b.cookingFuelType(c.getEffectiveFuelType())
                 .cylinders(c.getEffectiveCylinders()));

        lifestyleDataRepo.findByDeclarationDeclarationId(did).ifPresent(l ->
                b.publicTransportUsage(l.getPublicTransportUsage())
                 .wastesRecycling(l.isWastesRecycling()));

        scoreRepository.findByDeclarationDeclarationId(did).ifPresent(s ->
                b.carbonScore(CarbonScoreResponse.builder()
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
                        .build()));

        verificationRepo.findByDeclarationDeclarationId(did).ifPresent(v -> {
            b.verificationOutcome(v.getOutcome() != null ? v.getOutcome().name() : null)
             .rejectionReason(v.getRejectionReason())
             .agentNotes(v.getAgentNotes());
        });

        return b.build();
    }
}
