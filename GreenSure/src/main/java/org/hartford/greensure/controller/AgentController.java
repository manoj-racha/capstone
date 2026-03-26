package org.hartford.greensure.controller;

import jakarta.validation.Valid;
import org.hartford.greensure.dto.request.*;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.enums.AssignmentStatus;
import org.hartford.greensure.exception.*;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.service.AgentVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import org.hartford.greensure.dto.response.AgentPerformanceDTO;
import org.hartford.greensure.dto.response.AssignmentHistorySummaryDTO;

/**
 * All field agent endpoints.
 *
 * GET  /agent/dashboard          — list active assignments
 * GET  /agent/workspace/{id}     — open verification workspace for an assignment
 * POST /agent/verify/{id}/confirm
 * POST /agent/verify/{id}/modify
 * POST /agent/verify/{id}/reject
 * GET  /agent/history            — completed assignments
 * GET  /agent/profile            — own profile (strikes, stats)
 */
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Autowired private AgentVerificationService verificationService;
    @Autowired private AgentAssignmentRepository assignmentRepo;
    @Autowired private UserRepository userRepository;

    private Long agentId(Authentication auth) {
        org.hartford.greensure.security.SecurityUser user = (org.hartford.greensure.security.SecurityUser) auth.getPrincipal();
        return user.getId();
    }

    // ── Dashboard — active assignments ─────────────────────────

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<AgentTaskSummary>>> dashboard(Authentication auth) {
        List<AgentTaskSummary> tasks = assignmentRepo
                .findActiveByAgentId(agentId(auth))
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
                if (q.equals("ASSIGNED") || q.equals("IN_PROGRESS")) {
                    queryStatus = AssignmentStatus.ACTIVE;
                } else {
                    queryStatus = AssignmentStatus.valueOf(q);
                }
                tasks = assignmentRepo.findByAgentUserIdAndStatus(agentId(auth), queryStatus)
                        .stream().map(this::toTaskSummary).collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                tasks = List.of(); // Return empty if invalid status requested
            }
        } else {
            tasks = assignmentRepo.findByAgentUserId(agentId(auth))
                    .stream().map(this::toTaskSummary).collect(Collectors.toList());
        }
        return ResponseEntity.ok(ApiResponse.success("Assignments fetched", tasks));
    }

    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<AgentPerformanceDTO>> performance(Authentication auth) {
        Long id = agentId(auth);
        User agent = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Agent not found"));

        long completed = assignmentRepo.countByAgentUserIdAndStatus(id, AssignmentStatus.COMPLETED);
        long active    = assignmentRepo.countByAgentUserIdAndStatus(id, AssignmentStatus.ACTIVE);
        long total     = completed + active;

        List<AssignmentHistorySummaryDTO> history = assignmentRepo
                .findTop10ByAgentUserIdOrderByAssignedAtDesc(id)
                .stream()
                .map(a -> AssignmentHistorySummaryDTO.builder()
                        .assignmentId(a.getAssignmentId())
                        .declarationId(a.getDeclaration().getDeclarationId())
                        .userName(a.getDeclaration().getUser().getFullName())
                        .assignmentStatus(a.getStatus().name())
                        .deadline(a.getDeadline())
                        .createdAt(a.getAssignedAt())
                        .build())
                .collect(Collectors.toList());

        AgentPerformanceDTO dto = AgentPerformanceDTO.builder()
                .agentId(agent.getUserId())
                .agentName(agent.getFullName())
                .email(agent.getEmail())
                .pinCode(agent.getPinCode())
                .strikes(agent.getStrikes())
                .isActive(agent.isActive())
                .suspendedAt(agent.getSuspendedAt())
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
                .findByAgentUserIdAndStatus(agentId(auth), AssignmentStatus.COMPLETED)
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

        long completed = assignmentRepo.countByAgentUserIdAndStatus(id, AssignmentStatus.COMPLETED);
        long active    = assignmentRepo.countByAgentUserIdAndStatus(id, AssignmentStatus.ACTIVE);

        AgentProfileResponse profile = AgentProfileResponse.builder()
                .agentId(agent.getUserId())
                .fullName(agent.getFullName())
                .email(agent.getEmail())
                .phone(agent.getPhone())
                .pinCode(agent.getPinCode())
                .active(agent.isActive())
                .suspendedAt(agent.getSuspendedAt())
                .strikes(agent.getStrikes())
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
                .status(a.getStatus().name())
                .fraudRiskLevel(d.getFraudRiskLevel())
                .build();
    }

    // ── Inline summary DTO ─────────────────────────────────────

    @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor @lombok.Builder
    public static class AgentTaskSummary {
        private Long   assignmentId;
        private Long   declarationId;
        private String userName;
        private String userAddress;
        private Integer declarationYear;
        private java.time.LocalDateTime deadline;
        private String status;
        private String fraudRiskLevel;
    }
}
