package org.hartford.greensure.service;

import org.hartford.greensure.dto.request.AgentModifyRequest;
import org.hartford.greensure.dto.request.AgentRejectRequest;
import org.hartford.greensure.dto.response.AgentWorkspaceResponse;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.engine.CarbonScoreService;
import org.hartford.greensure.enums.*;
import org.hartford.greensure.exception.*;
import org.hartford.greensure.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles everything the field agent does during their workspace session:
 * 1. getWorkspace() — builds the full AgentWorkspaceResponse
 * 2. confirmVerification — agent found everything correct
 * 3. modifyAndVerify — agent corrected one or more values
 * 4. rejectDeclaration — agent rejected the declaration with a reason
 *
 * After CONFIRM or MODIFY, CarbonScoreService.calculateAndSave() is called
 * to generate the score using effective (agent-corrected) values.
 */
@Service
public class AgentVerificationService {

    private static final Logger log = LoggerFactory.getLogger(AgentVerificationService.class);

    @Autowired
    private AgentAssignmentRepository assignmentRepo;
    @Autowired
    private CarbonDeclarationRepository declarationRepo;
    @Autowired
    private VerificationRepository verificationRepo;
    @Autowired
    private DeclarationVehicleDataRepository vehicleDataRepo;
    @Autowired
    private ElectricityDataRepository electricityDataRepo;
    @Autowired
    private CookingDataRepository cookingDataRepo;
    @Autowired
    private SolarDataRepository solarDataRepo;
    @Autowired
    private HouseholdProfileRepository householdRepo;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CarbonScoreService carbonScoreService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ElectricityBillRepository electricityBillRepo;
    @Autowired
    private FraudAdvisoryService fraudAdvisoryService;

    // ── WORKSPACE ──────────────────────────────────────────────

    /**
     * Builds the agent verification workspace.
     * Validates that the assignment belongs to the logged-in agent.
     * Records startedAt if this is the first time the agent opens it.
     */
    @Transactional
    public AgentWorkspaceResponse getWorkspace(Long assignmentId, Long agentId) {
        AgentAssignment assignment = getActiveAssignmentForAgent(assignmentId, agentId);
        CarbonDeclaration declaration = assignment.getDeclaration();
        User user = declaration.getUser();

        List<DeclarationVehicleData> vehicles = vehicleDataRepo
                .findByDeclarationDeclarationId(declaration.getDeclarationId());
        ElectricityData electricity = electricityDataRepo
                .findByDeclarationDeclarationId(declaration.getDeclarationId())
                .orElse(null);
        CookingData cooking = cookingDataRepo
                .findByDeclarationDeclarationId(declaration.getDeclarationId())
                .orElse(null);
        SolarData solar = solarDataRepo
                .findByDeclarationDeclarationId(declaration.getDeclarationId())
                .orElse(null);

        // Build comparison table for non-vehicle modules
        List<AgentWorkspaceResponse.ComparisonField> compTable = buildComparisonTable(
                electricity, cooking, solar);

        // Parse fraud flags (trim; never pass null entries into List.of / Jackson)
        List<String> fraudFlagList = new ArrayList<>();
        if (declaration.getFraudAdvisoryFlags() != null
                && !declaration.getFraudAdvisoryFlags().isBlank()) {
            for (String raw : declaration.getFraudAdvisoryFlags().split(",")) {
                if (raw == null) {
                    continue;
                }
                String t = raw.trim();
                if (!t.isEmpty()) {
                    fraudFlagList.add(t);
                }
            }
        }

        Integer memberCount = householdRepo.findByUserUserId(user.getUserId())
                .map(HouseholdProfile::getNumberOfMembers)
                .orElse(null);

        AgentWorkspaceResponse.AgentWorkspaceResponseBuilder builder = AgentWorkspaceResponse.builder()
                .declarationId(declaration.getDeclarationId())
                .assignmentId(assignment.getAssignmentId())
                .userName(user.getFullName())
                .userAddress(user.getAddress())
                .userPhone(user.getMobile())
                .pinCode(user.getPinCode())
                .householdMemberCount(memberCount)
                .declarationYear(declaration.getDeclarationYear())
                .fraudRiskLevel(declaration.getFraudRiskLevel())
                .fraudScore(declaration.getFraudAdvisoryScore())
                .fraudFlags(fraudFlagList)
                .fraudFlagDescriptions(fraudAdvisoryService.describeFlags(fraudFlagList))
                .comparisonTable(compTable);

        List<AgentWorkspaceResponse.VehicleComparisonBlock> vehicleBlocks = new ArrayList<>();
        for (int i = 0; i < vehicles.size(); i++) {
            DeclarationVehicleData v = vehicles.get(i);
            AgentWorkspaceResponse.VehicleComparisonBlock block = new AgentWorkspaceResponse.VehicleComparisonBlock();
            block.setVehicleLabel("Vehicle " + (i + 1) + " — " + v.getMake() + " " + v.getModel());
            block.setVehicleCategory(v.getVehicleCategory() != null ? v.getVehicleCategory().name() : null);

            List<AgentWorkspaceResponse.ComparisonField> vComps = new ArrayList<>();
            vComps.add(field("Fuel Type", strVal(v.getFuelType()), strVal(v.getDataSource()),
                    v.getDataSource() != null && v.getDataSource().isVerified() ? MatchStatus.MATCH
                            : MatchStatus.UNVERIFIED));
            vComps.add(field("Mileage Band", strVal(v.getMileageBand()), "Self-declared", MatchStatus.UNVERIFIED));
            vComps.add(field("Registration", strVal(v.getRegistrationNumber()), strVal(v.getRegistrationNumber()),
                    MatchStatus.MATCH));
            block.setComparisons(vComps);

            List<org.hartford.greensure.dto.response.VehicleDocumentResponseDTO> docs = new ArrayList<>();
            if (v.getDocuments() != null) {
                for (VehicleDocument d : v.getDocuments()) {
                    org.hartford.greensure.dto.response.VehicleDocumentResponseDTO dto = new org.hartford.greensure.dto.response.VehicleDocumentResponseDTO();
                    dto.setDocumentId(d.getId());
                    dto.setDocumentType(d.getDocumentType() != null ? d.getDocumentType().name() : null);
                    dto.setDocumentUrl(d.getDocumentUrl());
                    dto.setOriginalFileName(d.getOriginalFileName());
                    dto.setMimeType(d.getMimeType());
                    dto.setFileSizeBytes(d.getFileSizeBytes());
                    dto.setVerified(d.isVerified());
                    dto.setAgentNote(d.getAgentNote());
                    dto.setUploadedAt(d.getUploadedAt() != null ? d.getUploadedAt().toString() : null);
                    docs.add(dto);
                }
            }
            block.setDocuments(docs);
            vehicleBlocks.add(block);
        }
        builder.vehicles(vehicleBlocks);
        if (electricity != null) {
            builder.userDeclaredMonthlyKwh(electricity.getUserDeclaredMonthlyKwh())
                    .ocrComputedMonthlyKwh(electricity.getOcrComputedMonthlyKwh())
                    .billsUploaded(electricity.getBillsUploaded());

            List<AgentWorkspaceResponse.ComparisonField> eComps = new ArrayList<>();
            eComps.add(field("Provider", strVal(electricity.getProvider()), strVal(electricity.getProvider()),
                    MatchStatus.MATCH));
            eComps.add(field("Consumer No", strVal(electricity.getConsumerNumber()),
                    strVal(electricity.getConsumerNumber()), MatchStatus.MATCH));
            eComps.add(field("Monthly kWh", strVal(electricity.getUserDeclaredMonthlyKwh()),
                    strVal(electricity.getOcrComputedMonthlyKwh()),
                    electricity.getOcrComputedMonthlyKwh() != null ? MatchStatus.MATCH : MatchStatus.UNVERIFIED));
            builder.electricityComparison(eComps);
            List<String> electricBillUrls = new ArrayList<>();
            try {
                electricBillUrls = electricityBillRepo
                        .findByDeclarationDeclarationIdOrderByBillingMonthDesc(
                                declaration.getDeclarationId())
                        .stream()
                        .map(ElectricityBill::getBillUrl)
                        .filter(u -> u != null && !u.isBlank())
                        .distinct()
                        .toList();
            } catch (Exception e) {
                log.warn(
                        "Could not load electricity bill URLs for declaration {}: {}",
                        declaration.getDeclarationId(),
                        e.getMessage());
            }
            builder.electricityDocumentUrls(electricBillUrls);
        }
        if (cooking != null) {
            builder.cookingFuelType(cooking.getFuelType() != null ? cooking.getFuelType().name() : null)
                    .userDeclaredCylinders(cooking.getUserDeclaredCylinders())
                    .ocrComputedCylinders(cooking.getOcrComputedCylinders());

            List<AgentWorkspaceResponse.ComparisonField> cComps = new ArrayList<>();
            cComps.add(field("Fuel Type", cooking.getFuelType() != null ? cooking.getFuelType().name() : "N/A", "N/A",
                    MatchStatus.UNVERIFIED));
            if (cooking.getFuelType() != null && cooking.getFuelType().name().equals("PNG")) {
                cComps.add(field("Consumer No", strVal(cooking.getPngConsumerNumber()),
                        strVal(cooking.getPngConsumerNumber()), MatchStatus.MATCH));
            } else {
                cComps.add(field("Annual Cylinders", strVal(cooking.getUserDeclaredCylinders()),
                        strVal(cooking.getOcrComputedCylinders()),
                        cooking.getOcrComputedCylinders() != null ? MatchStatus.MATCH : MatchStatus.UNVERIFIED));
            }
            builder.cookingComparison(cComps);

            List<String> cDocs = new ArrayList<>();
            if (cooking.getBillUrls() != null && !cooking.getBillUrls().isEmpty()) {
                // Remove brackets and quotes from JSON string ["url"]
                String urls = cooking.getBillUrls().replaceAll("[\\[\\]\"]", "");
                for (String u : urls.split(",")) {
                    if (!u.trim().isEmpty())
                        cDocs.add(u.trim());
                }
            }
            builder.cookingDocumentUrls(cDocs);
        }
        if (solar != null) {
            builder.hasSolar(solar.isHasSolar())
                    .solarCapacityKw(solar.getCapacityKw())
                    .mnreVerified(solar.isMnreVerified())
                    .solarCertificateUrl(solar.getCertificateUrl());

            List<AgentWorkspaceResponse.ComparisonField> sComps = new ArrayList<>();
            sComps.add(field("Has Solar?", String.valueOf(solar.isHasSolar()), "N/A", MatchStatus.UNVERIFIED));
            if (solar.isHasSolar()) {
                sComps.add(field("Capacity (kW)", strVal(solar.getCapacityKw()),
                        solar.isMnreVerified() ? strVal(solar.getCapacityKw()) : "N/A",
                        solar.isMnreVerified() ? MatchStatus.MATCH : MatchStatus.UNVERIFIED));
            }
            builder.solarComparison(sComps);

            List<String> sDocs = new ArrayList<>();
            if (solar.getCertificateUrl() != null && !solar.getCertificateUrl().isEmpty()) {
                sDocs.add(solar.getCertificateUrl());
            }
            builder.solarDocumentUrls(sDocs);
        }

        return builder.build();
    }

    // ── CONFIRM ────────────────────────────────────────────────

    @Transactional
    public void confirmVerification(Long assignmentId, Long agentId,
            Double gpsLat, Double gpsLng,
            String agentNotes,
            List<String> documentUrls) {
        AgentAssignment assignment = getActiveAssignmentForAgent(assignmentId, agentId);
        CarbonDeclaration declaration = assignment.getDeclaration();
        User agent = assignment.getAgent();

        // Save Verification record
        Verification verification = Verification.builder()
                .assignment(assignment)
                .agent(agent)
                .declaration(declaration)
                .outcome(VerificationOutcome.CONFIRMED)
                .gpsLat(gpsLat)
                .gpsLng(gpsLng)
                .agentNotes(agentNotes)
                .documentUrls(documentUrls != null ? String.join(",", documentUrls) : null)
                .build();
        verificationRepo.save(verification);

        // Finalise assignment
        assignment.setAssignmentStatus(AssignmentStatus.COMPLETED);
        assignment.setGpsLatAtStart(gpsLat);
        assignment.setGpsLngAtStart(gpsLng);
        assignmentRepo.save(assignment);
        declaration.setStatus(DeclarationStatus.VERIFIED);
        declarationRepo.save(declaration);

        // Calculate and save carbon score
        carbonScoreService.generateScore(declaration.getDeclarationId());

        log.info("Verification CONFIRMED for declaration {} by agent {}",
                declaration.getDeclarationId(), agentId);
    }

    // ── MODIFY ─────────────────────────────────────────────────

    @Transactional
    public void modifyAndVerify(Long assignmentId, Long agentId,
            AgentModifyRequest req,
            Double gpsLat, Double gpsLng) {
        AgentAssignment assignment = getActiveAssignmentForAgent(assignmentId, agentId);
        CarbonDeclaration declaration = assignment.getDeclaration();
        User agent = assignment.getAgent();
        Long declarationId = declaration.getDeclarationId();

        // Apply corrections to the primary vehicle (multi-vehicle corrections would
        // require DTO changes)
        vehicleDataRepo.findByDeclarationDeclarationId(declarationId).stream().findFirst().ifPresent(v -> {
            if (req.getCorrectedFuelType() != null)
                v.setAgentCorrectedFuelType(req.getCorrectedFuelType());
            if (req.getCorrectedMileageBand() != null)
                v.setAgentCorrectedMileageBand(req.getCorrectedMileageBand());
            v.setAgentCorrectionNote(req.getCorrectionNotes());
            vehicleDataRepo.save(v);
        });

        electricityDataRepo.findByDeclarationDeclarationId(declarationId).ifPresent(e -> {
            if (req.getCorrectedMonthlyKwh() != null)
                e.setAgentCorrectedMonthlyKwh(req.getCorrectedMonthlyKwh());
            e.setAgentCorrectionNote(req.getCorrectionNotes());
            electricityDataRepo.save(e);
        });

        cookingDataRepo.findByDeclarationDeclarationId(declarationId).ifPresent(c -> {
            if (req.getCorrectedCookingFuelType() != null)
                c.setAgentCorrectedFuelType(req.getCorrectedCookingFuelType());
            if (req.getCorrectedAnnualCylinders() != null)
                c.setAgentCorrectedCylinders(req.getCorrectedAnnualCylinders());
            c.setAgentCorrectionNote(req.getCorrectionNotes());
            cookingDataRepo.save(c);
        });

        solarDataRepo.findByDeclarationDeclarationId(declarationId).ifPresent(s -> {
            if (req.getCorrectedSolarCapacityKw() != null)
                s.setAgentCorrectedCapacityKw(req.getCorrectedSolarCapacityKw());
            if (req.getAgentVerifiedSolar() != null)
                s.setAgentVerifiedSolar(req.getAgentVerifiedSolar());
            s.setAgentCorrectionNote(req.getCorrectionNotes());
            solarDataRepo.save(s);
        });

        // Save Verification record
        Verification verification = Verification.builder()
                .assignment(assignment)
                .agent(agent)
                .declaration(declaration)
                .outcome(VerificationOutcome.MODIFIED)
                .gpsLat(gpsLat)
                .gpsLng(gpsLng)
                .agentNotes(req.getCorrectionNotes())
                .documentUrls(req.getDocumentUrls() != null
                        ? String.join(",", req.getDocumentUrls())
                        : null)
                .build();
        verificationRepo.save(verification);

        // Finalise assignment
        assignment.setAssignmentStatus(AssignmentStatus.COMPLETED);
        assignment.setGpsLatAtStart(gpsLat);
        assignment.setGpsLngAtStart(gpsLng);
        assignmentRepo.save(assignment);
        declaration.setStatus(DeclarationStatus.VERIFIED);
        declarationRepo.save(declaration);

        // Calculate score using corrected values
        carbonScoreService.generateScore(declarationId);

        log.info("Verification MODIFIED for declaration {} by agent {}",
                declarationId, agentId);
    }

    // ── REJECT ─────────────────────────────────────────────────

    @Transactional
    public void rejectDeclaration(Long assignmentId, Long agentId, AgentRejectRequest req) {
        AgentAssignment assignment = getActiveAssignmentForAgent(assignmentId, agentId);
        CarbonDeclaration declaration = assignment.getDeclaration();
        User agent = assignment.getAgent();

        Verification verification = Verification.builder()
                .assignment(assignment)
                .agent(agent)
                .declaration(declaration)
                .outcome(VerificationOutcome.REJECTED)
                .rejectionReason(req.getRejectionReason())
                .documentUrls(req.getDocumentUrls() != null
                        ? String.join(",", req.getDocumentUrls())
                        : null)
                .build();
        verificationRepo.save(verification);

        assignment.setAssignmentStatus(AssignmentStatus.COMPLETED);
        assignmentRepo.save(assignment);

        declaration.setStatus(DeclarationStatus.REJECTED);
        declarationRepo.save(declaration);

        // Notify user
        notificationService.sendToUser(
                declaration.getUser().getUserId(),
                Notification.NotificationType.VERIFICATION_COMPLETE,
                "Your declaration has been rejected. Reason: " + req.getRejectionReason()
                        + " You may resubmit up to 3 times.");

        log.info("Verification REJECTED for declaration {} by agent {}",
                declaration.getDeclarationId(), agentId);
    }

    // ── Private helpers ─────────────────────────────────────────

    private AgentAssignment getActiveAssignmentForAgent(Long assignmentId, Long agentId) {
        AgentAssignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(
                        "Assignment not found: " + assignmentId));

        if (!assignment.getAgent().getUserId().equals(agentId)) {
            throw new UnauthorizedException("This assignment does not belong to you.");
        }

        if (assignment.getAssignmentStatus() != AssignmentStatus.ACTIVE) {
            throw new BadRequestException("Assignment is no longer active: " + assignment.getAssignmentStatus());
        }

        return assignment;
    }

    private List<AgentWorkspaceResponse.ComparisonField> buildComparisonTable(
            ElectricityData electricity,
            CookingData cooking,
            SolarData solar) {

        List<AgentWorkspaceResponse.ComparisonField> rows = new ArrayList<>();

        if (electricity != null) {
            boolean billsMatch = electricity.getOcrComputedMonthlyKwh() != null
                    && electricity.getUserDeclaredMonthlyKwh() != null;

            MatchStatus kwStatus;
            if (!billsMatch) {
                kwStatus = MatchStatus.UNVERIFIED;
            } else {
                double diff = Math.abs(electricity.getOcrComputedMonthlyKwh()
                        - electricity.getUserDeclaredMonthlyKwh());
                kwStatus = diff <= electricity.getUserDeclaredMonthlyKwh() * 0.15
                        ? MatchStatus.MATCH
                        : MatchStatus.MISMATCH;
            }

            rows.add(field("Monthly kWh",
                    num(electricity.getUserDeclaredMonthlyKwh()) + " (declared)",
                    num(electricity.getOcrComputedMonthlyKwh()) + " (OCR avg)",
                    kwStatus));
        }

        if (cooking != null) {
            rows.add(field("Cooking Fuel",
                    strVal(cooking.getFuelType()),
                    "Self-declared", MatchStatus.UNVERIFIED));

            if (cooking.getFuelType() == CookingFuel.LPG) {
                rows.add(field("Annual LPG Cylinders",
                        num(cooking.getUserDeclaredCylinders()),
                        num(cooking.getOcrComputedCylinders()),
                        cooking.getOcrComputedCylinders() != null
                                && cooking.getUserDeclaredCylinders() != null
                                && Math.abs(cooking.getOcrComputedCylinders()
                                        - cooking.getUserDeclaredCylinders()) <= 2
                                                ? MatchStatus.MATCH
                                                : MatchStatus.UNVERIFIED));
            }
        }

        if (solar != null && solar.isHasSolar()) {
            rows.add(field("Solar Installation",
                    solar.getCapacityKw() + " kW",
                    solar.isMnreVerified() ? "MNRE Verified" : "Not verified",
                    solar.isMnreVerified() ? MatchStatus.MATCH : MatchStatus.UNVERIFIED));
        }

        return rows;
    }

    private AgentWorkspaceResponse.ComparisonField field(
            String name, String userClaim, String sysValue, MatchStatus status) {
        return AgentWorkspaceResponse.ComparisonField.builder()
                .fieldName(name).userClaim(userClaim)
                .systemValue(sysValue).matchStatus(status).build();
    }

    private String strVal(Object val) {
        return val != null ? val.toString() : "—";
    }

    private String num(Number n) {
        return n != null ? String.valueOf(n) : "—";
    }
}

// Corrected missing setGpsLatAtStart on Verification — Verification only has
// gpsLat/gpsLng.
// The assignment stores gpsLatAtStart. Suppressed by using a non-existent
// method above;
// correcting: remove .gpsLatAtStart(gpsLat) from Verification builder (not a
// field).
