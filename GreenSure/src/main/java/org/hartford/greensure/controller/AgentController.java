package org.hartford.greensure.controller;

import org.hartford.greensure.dto.request.VerificationRequest;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.AgentAssignment;
import org.hartford.greensure.security.SecurityUser;
import org.hartford.greensure.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agent")
@PreAuthorize("hasRole('AGENT')")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<List<AgentTaskResponse>>> getDashboard(
            @AuthenticationPrincipal SecurityUser user) {

        List<AgentTaskResponse> tasks = agentService.getDashboard(user.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard fetched", tasks));
    }

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<AgentTaskResponse>>> getAssignments(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(required = false) AgentAssignment.AssignmentStatus status) {

        List<AgentTaskResponse> tasks = agentService.getAssignments(user.getId(), status);
        return ResponseEntity.ok(
                ApiResponse.success("Assignments fetched", tasks));
    }

    @GetMapping("/assignment/{id}")
    public ResponseEntity<ApiResponse<AgentTaskResponse>> getAssignment(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser user) {

        List<AgentTaskResponse> all = agentService.getDashboard(user.getId());
        AgentTaskResponse task = all.stream()
                .filter(t -> t.getAssignmentId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        return ResponseEntity.ok(
                ApiResponse.success("Assignment fetched", task));
    }

    @PutMapping("/assignment/{id}/start")
    public ResponseEntity<ApiResponse<AgentTaskResponse>> startAssignment(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser user) {

        AgentTaskResponse task = agentService.startAssignment(id, user.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Assignment started", task));
    }

    @PostMapping("/verification/{assignmentId}/submit")
    public ResponseEntity<ApiResponse<Void>> submitVerification(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody VerificationRequest body) {

        agentService.submitVerification(assignmentId, user.getId(), body);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Verification submitted successfully. Carbon score will be calculated shortly."));
    }

    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<AgentPerformanceResponse>> getPerformance(
            @AuthenticationPrincipal SecurityUser user) {

        AgentPerformanceResponse performance = agentService.getPerformance(user.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Performance fetched", performance));
    }

    @GetMapping("/assignment/{id}/declaration")
    public ResponseEntity<ApiResponse<DeclarationResponse>> getDeclarationForAssignment(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser user) {

        DeclarationResponse declaration = agentService.getDeclarationForAssignment(id, user.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Declaration fetched successfully", declaration));
    }
}
