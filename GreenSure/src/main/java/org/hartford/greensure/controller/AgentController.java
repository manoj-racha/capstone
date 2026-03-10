package org.hartford.greensure.controller;

import org.hartford.greensure.dto.request.VerificationRequest;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.AgentAssignment;
import org.hartford.greensure.security.JwtUtil;
import org.hartford.greensure.service.AgentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agent")
@PreAuthorize("hasRole('AGENT')")
public class AgentController {

    @Autowired
    private AgentService agentService;
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<List<AgentTaskResponse>>> getDashboard(HttpServletRequest request) {

        Long agentId = extractAgentId(request);
        List<AgentTaskResponse> tasks = agentService.getDashboard(agentId);
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard fetched", tasks));
    }

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<AgentTaskResponse>>> getAssignments(
            HttpServletRequest request,
            @RequestParam(required = false) AgentAssignment.AssignmentStatus status) {

        Long agentId = extractAgentId(request);
        List<AgentTaskResponse> tasks = agentService.getAssignments(agentId, status);
        return ResponseEntity.ok(
                ApiResponse.success("Assignments fetched", tasks));
    }

    @GetMapping("/assignment/{id}")
    public ResponseEntity<ApiResponse<AgentTaskResponse>> getAssignment(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long agentId = extractAgentId(request);
        List<AgentTaskResponse> all = agentService.getDashboard(agentId);
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
            HttpServletRequest request) {

        Long agentId = extractAgentId(request);
        AgentTaskResponse task = agentService.startAssignment(id, agentId);
        return ResponseEntity.ok(
                ApiResponse.success("Assignment started", task));
    }

    @PostMapping("/verification/{assignmentId}/submit")
    public ResponseEntity<ApiResponse<Void>> submitVerification(
            @PathVariable Long assignmentId,
            HttpServletRequest request,
            @Valid @RequestBody VerificationRequest body) {

        Long agentId = extractAgentId(request);
        agentService.submitVerification(assignmentId, agentId, body);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Verification submitted successfully. Carbon score will be calculated shortly."));
    }

    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<AgentPerformanceResponse>> getPerformance(HttpServletRequest request) {

        Long agentId = extractAgentId(request);
        AgentPerformanceResponse performance = agentService.getPerformance(agentId);
        return ResponseEntity.ok(
                ApiResponse.success("Performance fetched", performance));
    }

    @GetMapping("/assignment/{id}/declaration")
    public ResponseEntity<ApiResponse<DeclarationResponse>> getDeclarationForAssignment(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long agentId = extractAgentId(request);
        DeclarationResponse declaration = agentService.getDeclarationForAssignment(id, agentId);
        return ResponseEntity.ok(
                ApiResponse.success("Declaration fetched successfully", declaration));
    }

    private Long extractAgentId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.extractId(token);
    }
}
