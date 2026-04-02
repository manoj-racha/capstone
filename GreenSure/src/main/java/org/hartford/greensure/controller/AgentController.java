package org.hartford.greensure.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.hartford.greensure.dto.request.*;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.enums.AssignmentStatus;
import org.hartford.greensure.exception.*;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.service.AgentVerificationService;
import org.hartford.greensure.service.ai.AiDocumentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * All field agent endpoints.
 *
 * GET /agent/dashboard — list active assignments
 * GET /agent/workspace/{id} — open verification workspace for an assignment
 * POST /agent/verify/{id}/confirm
 * POST /agent/verify/{id}/modify
 * POST /agent/verify/{id}/reject
 * GET /agent/history — completed assignments
 * GET /agent/profile — own profile (strikes, stats)
 */
@RestController
@RequestMapping("/agent")
@Slf4j
public class AgentController {

    @Autowired
    private AgentVerificationService verificationService;
    @Autowired
    private AiDocumentAnalysisService aiDocumentAnalysisService;
    @Autowired
    private AgentAssignmentRepository assignmentRepo;
    @Autowired
    private CarbonDeclarationRepository declarationRepo;
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
    private HouseholdProfileRepository householdProfileRepo;
    @Autowired
    private UserRepository userRepository;

    private Long agentId(Authentication auth) {
        org.hartford.greensure.security.SecurityUser user = (org.hartford.greensure.security.SecurityUser) auth
                .getPrincipal();
        return user.getId();
    }

    // ── Dashboard — active assignments ─────────────────────────

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<AgentTaskSummary>>> dashboard(Authentication auth) {
        List<AgentTaskSummary> tasks = assignmentRepo
                .findByAgentUserIdAndAssignmentStatus(agentId(auth), AssignmentStatus.ACTIVE)
                .stream().map(this::toTaskSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Active assignments", tasks));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<List<AgentTaskSummary>>> dashboardAlias(Authentication auth) {
        return dashboard(auth);
    }

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<AgentTaskSummary>>> assignments(
            Authentication auth, @RequestParam(required = false) String status) {
        List<AgentTaskSummary> tasks;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            String q = status.toUpperCase();
            try {
                AssignmentStatus queryStatus;
                if (q.equals("ASSIGNED") || q.equals("IN_PROGRESS") || q.equals("ACTIVE")) {
                    queryStatus = AssignmentStatus.ACTIVE;
                } else {
                    queryStatus = AssignmentStatus.valueOf(q);
                }
                tasks = assignmentRepo.findByAgentUserIdAndAssignmentStatus(agentId(auth), queryStatus)
                        .stream().map(this::toTaskSummary).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                tasks = List.of(); // Return empty if invalid status requested
            }
        } else {
            tasks = assignmentRepo.findByAgentUserIdOrderByDeadlineAsc(agentId(auth))
                    .stream().map(this::toTaskSummary).collect(Collectors.toList());
        }
        return ResponseEntity.ok(ApiResponse.success("Assignments fetched", tasks));
    }

    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<AgentPerformanceDTO>> performance(Authentication auth) {
        Long id = agentId(auth);
        User agent = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Agent not found"));

        long completed = assignmentRepo.countByAgentUserIdAndAssignmentStatus(id, AssignmentStatus.COMPLETED);
        long active = assignmentRepo.countByAgentUserIdAndAssignmentStatus(id, AssignmentStatus.ACTIVE);
        long total = completed + active;

        List<AssignmentHistorySummaryDTO> history = assignmentRepo
                .findTop10ByAgentUserIdOrderByAssignedAtDesc(id)
                .stream()
                .map(a -> AssignmentHistorySummaryDTO.builder()
                        .assignmentId(a.getAssignmentId())
                        .declarationId(a.getDeclaration().getDeclarationId())
                        .userName(a.getDeclaration().getUser().getFullName())
                        .assignmentStatus(a.getAssignmentStatus().name())
                        .deadline(a.getDeadline())
                        .createdAt(a.getAssignedAt())
                        .build())
                .collect(Collectors.toList());

        AgentPerformanceDTO dto = AgentPerformanceDTO.builder()
                .agentId(agent.getUserId())
                .agentName(agent.getFullName())
                .email(agent.getEmail())
                .pinCode(agent.getPinCode())
                .strikes(0) // MOCKED
                .isActive(agent.getStatus() == User.UserStatus.ACTIVE)
                .suspendedAt(null) // MOCKED
                .activeAssignments(active)
                .completedAssignments(completed)
                .totalAssignments(total)
                .recentHistory(history)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Agent performance", dto));
    }

    // ── Workspace ──────────────────────────────────────────────

    @GetMapping("/declarations/{assignmentId}/workspace")
    public ResponseEntity<ApiResponse<AgentWorkspaceResponse>> workspace(
            Authentication auth,
            @PathVariable Long assignmentId) {
        AgentWorkspaceResponse workspace = verificationService.getWorkspace(assignmentId, agentId(auth));
        return ResponseEntity.ok(ApiResponse.success("Workspace loaded", workspace));
    }

    @PostMapping("/declarations/{assignmentId}/ai-analysis")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AiDocumentAnalysisResult>> runAiAnalysis(
            @PathVariable Long assignmentId) {

        User currentAgent = currentAgentOrThrow();

        AgentAssignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found: " + assignmentId));

        if (assignment.getAgent() == null
                || assignment.getAgent().getUserId() == null
                || !assignment.getAgent().getUserId().equals(currentAgent.getUserId())) {
            throw new UnauthorizedException("This assignment does not belong to you.");
        }

        CarbonDeclaration assignmentDeclaration = assignment.getDeclaration();
        if (assignmentDeclaration == null || assignmentDeclaration.getDeclarationId() == null) {
            throw new ResourceNotFoundException("No declaration found for assignment: " + assignmentId);
        }

        CarbonDeclaration declaration = declarationRepo.findById(assignmentDeclaration.getDeclarationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Declaration not found: " + assignmentDeclaration.getDeclarationId()));

        Long declarationId = declaration.getDeclarationId();
        List<DeclarationVehicleData> vehicles = vehicleDataRepo.findByDeclarationDeclarationId(declarationId);
        ElectricityData electricityData = electricityDataRepo.findByDeclarationDeclarationId(declarationId)
                .orElse(null);
        List<ElectricityBill> electricityBills = electricityBillRepo
                .findByDeclarationDeclarationIdOrderByBillingMonthDesc(declarationId);
        CookingData cookingData = cookingDataRepo.findByDeclarationDeclarationId(declarationId).orElse(null);
        SolarData solarData = solarDataRepo.findByDeclarationDeclarationId(declarationId).orElse(null);

        HouseholdProfile householdProfile = null;
        if (declaration.getUser() != null && declaration.getUser().getUserId() != null) {
            householdProfile = householdProfileRepo.findByUserUserId(declaration.getUser().getUserId()).orElse(null);
        }

        AiDocumentAnalysisResult result;
        try {
            result = aiDocumentAnalysisService.analyseDeclarationDocuments(
                    declarationId,
                    declaration,
                    vehicles,
                    electricityData,
                    electricityBills,
                    cookingData,
                    solarData,
                    householdProfile);
        } catch (Exception e) {
            log.error("AI analysis crashed unexpectedly for assignment {} (declaration {}): {}",
                    assignmentId,
                    declarationId,
                    e.getMessage(),
                    e);
            result = AiDocumentAnalysisResult.builder()
                    .analysisSuccess(false)
                    .errorMessage("AI analysis failed unexpectedly. Proceed with manual verification.")
                    .overallFindings(List.of())
                    .analysedAt(LocalDateTime.now())
                    .build();
        }

        if (result == null) {
            result = AiDocumentAnalysisResult.builder()
                    .analysisSuccess(false)
                    .errorMessage("AI analysis returned no result. Proceed with manual verification.")
                    .overallFindings(List.of())
                    .analysedAt(LocalDateTime.now())
                    .build();
        }

        return ResponseEntity.ok(ApiResponse.success("AI analysis complete", result));
    }

    @GetMapping("/assignment/{id}")
    public ResponseEntity<ApiResponse<AgentTaskSummary>> getAssignment(
            Authentication auth,
            @PathVariable Long id) {
        AgentAssignment a = assignmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        if (!a.getAgent().getUserId().equals(agentId(auth))) {
            throw new UnauthorizedException("Not authorized to view this assignment");
        }

        return ResponseEntity.ok(ApiResponse.success("Assignment loaded", toTaskSummary(a)));
    }

    // ── Confirm ────────────────────────────────────────────────

    @PostMapping("/verify/{assignmentId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirm(
            Authentication auth,
            @PathVariable Long assignmentId,
            @RequestParam(required = false) Double gpsLat,
            @RequestParam(required = false) Double gpsLng,
            @RequestParam(required = false) String notes,
            @RequestBody(required = false) List<String> documentUrls) {
        verificationService.confirmVerification(assignmentId, agentId(auth), gpsLat, gpsLng, notes, documentUrls);
        return ResponseEntity.ok(ApiResponse.success(
                "Declaration confirmed. Carbon score has been calculated."));
    }

    // ── Modify ─────────────────────────────────────────────────

    @PostMapping("/verify/{assignmentId}/modify")
    public ResponseEntity<ApiResponse<Void>> modify(
            Authentication auth,
            @PathVariable Long assignmentId,
            @RequestParam(required = false) Double gpsLat,
            @RequestParam(required = false) Double gpsLng,
            @Valid @RequestBody AgentModifyRequest req) {
        verificationService.modifyAndVerify(assignmentId, agentId(auth), req, gpsLat, gpsLng);
        return ResponseEntity.ok(ApiResponse.success(
                "Values corrected. Carbon score calculated using corrected data."));
    }

    // ── Reject ─────────────────────────────────────────────────

    @PostMapping("/verify/{assignmentId}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            Authentication auth,
            @PathVariable Long assignmentId,
            @Valid @RequestBody AgentRejectRequest req) {
        verificationService.rejectDeclaration(assignmentId, agentId(auth), req);
        return ResponseEntity.ok(ApiResponse.success(
                "Declaration rejected. User has been notified."));
    }

    // ── History ────────────────────────────────────────────────

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AgentTaskSummary>>> history(Authentication auth) {
        List<AgentTaskSummary> tasks = assignmentRepo
                .findByAgentUserIdAndAssignmentStatus(agentId(auth), AssignmentStatus.COMPLETED)
                .stream().map(this::toTaskSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Completed assignments", tasks));
    }

    // ── Profile ────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AgentProfileResponse>> profile(Authentication auth) {
        Long id = agentId(auth);
        User agent = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Agent not found"));

        long completed = assignmentRepo.countByAgentUserIdAndAssignmentStatus(id, AssignmentStatus.COMPLETED);
        long active = assignmentRepo.countByAgentUserIdAndAssignmentStatus(id, AssignmentStatus.ACTIVE);

        AgentProfileResponse profile = AgentProfileResponse.builder()
                .agentId(agent.getUserId())
                .fullName(agent.getFullName())
                .email(agent.getEmail())
                .phone(agent.getMobile())
                .pinCode(agent.getPinCode())
                .active(agent.getStatus() == User.UserStatus.ACTIVE)
                .suspendedAt(null) // MOCKED
                .strikes(0) // MOCKED
                .totalCompleted(completed)
                .activeAssignments(active)
                .createdAt(agent.getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Agent profile", profile));
    }

    // ── Private mapper ─────────────────────────────────────────

    private AgentTaskSummary toTaskSummary(AgentAssignment a) {
        CarbonDeclaration d = a.getDeclaration();
        User u = d.getUser();
        return AgentTaskSummary.builder()
                .assignmentId(a.getAssignmentId())
                .declarationId(d.getDeclarationId())
                .userName(u.getFullName())
                .userAddress(u.getAddress())
                .declarationYear(d.getDeclarationYear())
                .deadline(a.getDeadline())
                .status(a.getAssignmentStatus().name())
                .fraudRiskLevel(d.getFraudRiskLevel())
                .build();
    }

    private User currentAgentOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof org.hartford.greensure.security.SecurityUser principal)) {
            throw new UnauthorizedException("User is not authenticated.");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException("Agent not found"));
    }

    // ── Inline summary DTO ─────────────────────────────────────

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AgentTaskSummary {
        private Long assignmentId;
        private Long declarationId;
        private String userName;
        private String userAddress;
        private Integer declarationYear;
        private java.time.LocalDateTime deadline;
        private String status;
        private String fraudRiskLevel;
    }
}
