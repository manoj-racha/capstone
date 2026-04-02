package org.hartford.greensure.controller;

import org.hartford.greensure.dto.request.ChangeAssignmentAgentRequest;
import org.hartford.greensure.dto.request.CreateAgentRequest;
import org.hartford.greensure.dto.request.ManualAssignmentRequest;
import org.hartford.greensure.dto.request.ReassignDeclarationRequest;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.enums.AssignmentStatus;
import org.hartford.greensure.enums.DeclarationStatus;
import org.hartford.greensure.enums.VerificationOutcome;
import org.hartford.greensure.exception.BadRequestException;
import org.hartford.greensure.exception.DuplicateAgentFieldException;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

        @Autowired
        private UserRepository userRepository;
        @Autowired
        private CarbonDeclarationRepository declarationRepo;
        @Autowired
        private AgentAssignmentRepository assignmentRepo;
        @Autowired
        private VerificationRepository verificationRepo;
        @Autowired
        private CarbonScoreRepository scoreRepo;
        @Autowired
        private UserService userService;
        @Autowired
        private AgentService agentService;
        @Autowired
        private DeclarationService declarationService;
        @Autowired
        private DeclarationModuleService declarationModuleService;
        @Autowired
        private PasswordEncoder passwordEncoder;

        @GetMapping("/users")
        public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> getAllUsers(
                        @RequestParam(required = false) User.UserType userType,
                        @RequestParam(required = false) User.UserStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size);
                Page<User> users;

                if (userType != null && status != null) {
                        users = userRepository.findByUserTypeAndStatus(userType, status, pageable);
                } else if (userType != null) {
                        users = userRepository.findByUserType(userType, pageable);
                } else if (status != null) {
                        users = userRepository.findByStatus(status, pageable);
                } else {
                        users = userRepository.findAll(pageable);
                }

                Page<UserProfileResponse> responses = users.map(user -> UserProfileResponse.builder()
                                .userId(user.getUserId())
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .mobile(user.getMobile())
                                .userType(user.getUserType())
                                .status(user.getStatus())
                                .city(user.getCity())
                                .createdAt(user.getCreatedAt())
                                .build());

                return ResponseEntity.ok(ApiResponse.success("Users fetched", responses));
        }

        @GetMapping("/users/{id}")
        public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(@PathVariable Long id) {

                UserProfileResponse profile = userService.getProfile(id);
                return ResponseEntity.ok(
                                ApiResponse.success("User fetched", profile));
        }

        @PutMapping("/users/{id}/status")
        public ResponseEntity<ApiResponse<Void>> updateUserStatus(
                        @PathVariable Long id,
                        @RequestParam User.UserStatus status) {
                User user = userRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                user.setStatus(status);
                userRepository.save(user);
                return ResponseEntity.ok(
                                ApiResponse.success("User status updated to " + status));
        }

        @PutMapping("/users/{id}/unlock-resubmission")
        public ResponseEntity<ApiResponse<Void>> unlockResubmission(@PathVariable Long id) {

                int currentYear = java.time.LocalDate.now().getYear();

                declarationRepo.findByUserUserIdAndDeclarationYear(id, currentYear)
                                .ifPresent(d -> {
                                        d.setResubmissionCount(0);
                                        d.setStatus(DeclarationStatus.DRAFT);
                                        declarationRepo.save(d);
                                });

                return ResponseEntity.ok(
                                ApiResponse.success("Account unlocked. User can resubmit."));
        }

        @PostMapping("/agents/create")
        public ResponseEntity<ApiResponse<Void>> createAgent(
                        @Valid @RequestBody CreateAgentRequest request) {

                String mobile = request.resolvedMobile();
                if (mobile == null || mobile.isBlank()) {
                        throw new BadRequestException("Mobile number is required");
                }

                String pinCode = request.resolvedPinCode();
                if (pinCode == null || pinCode.isBlank()) {
                        throw new BadRequestException("Pin code is required");
                }

                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new DuplicateAgentFieldException("email", "Email already registered");
                }
                if (userRepository.existsByMobile(mobile)) {
                        throw new DuplicateAgentFieldException("mobile", "Mobile already registered");
                }
                // Employee ID check is removed as it's not part of User entity directly
                // if (agentRepository.existsByEmployeeId(request.getEmployeeId())) {
                // throw new DuplicateAgentFieldException("employeeId", "Employee ID already
                // exists");
                // }

                User agent = User.builder()
                                .userType(request.resolvedAgentType())
                                .fullName(request.getFullName())
                                .email(request.getEmail())
                                .mobile(mobile)
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .status(User.UserStatus.ACTIVE)
                                .address("System generated")
                                .pinCode(pinCode)
                                .city("System")
                                .state("System")
                                .build();

                try {
                        userRepository.save(agent);
                } catch (DataIntegrityViolationException ex) {
                        throw new DuplicateAgentFieldException(
                                        "agent",
                                        "Agent already exists with same email, mobile, or employee ID");
                }

                return ResponseEntity.ok(
                                ApiResponse.success("Agent created successfully."));
        }

        @GetMapping("/declarations/unassigned")
        public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> getUnassignedDeclarations() {
                List<java.util.Map<String, Object>> data = agentService.getUnassignedDeclarations()
                                .stream()
                                .map(d -> {
                                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                                        map.put("declarationId", d.getDeclarationId());
                                        map.put("userId", d.getUser().getUserId());
                                        map.put("userName", d.getUser().getFullName());
                                        map.put("userType", d.getUser().getUserType());
                                        map.put("submittedAt", d.getSubmittedAt());
                                        map.put("pinCode", d.getUser().getPinCode());
                                        return map;
                                })
                                .toList();

                return ResponseEntity.ok(ApiResponse.success("Unassigned declarations fetched", data));
        }

        @GetMapping("/declarations/assigned")
        public ResponseEntity<ApiResponse<List<AgentTaskResponse>>> getAssignedDeclarations() {
                return ResponseEntity.ok(ApiResponse.success("Assigned declarations fetched",
                                agentService.getActiveAssignments()));
        }

        @GetMapping("/agents/available")
        public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableAgents(
                        @RequestParam(required = false) String pinCode) {

                List<Map<String, Object>> agents = userRepository
                                .findByUserType(User.UserType.AGENT, Pageable.unpaged())
                                .stream()
                                .filter(a -> pinCode == null || pinCode.isBlank() || a.getPinCode().equals(pinCode))
                                .filter(a -> a.getStatus() == User.UserStatus.ACTIVE)
                                .map(a -> {
                                        Map<String, Object> map = new java.util.HashMap<>();
                                        map.put("agentId", a.getUserId());
                                        map.put("fullName", a.getFullName());
                                        map.put("employeeId", "EMP-" + a.getUserId());
                                        map.put("activeAssignments", 0); // Mock active assignments
                                        return map;
                                })
                                .toList();

                return ResponseEntity.ok(ApiResponse.success(
                                "Available agents fetched", agents));
        }

        @PostMapping("/assignment/assign")
        public ResponseEntity<ApiResponse<AgentTaskResponse>> assignAgent(
                        @Valid @RequestBody ManualAssignmentRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Agent assigned successfully",
                                agentService.assignDeclaration(request)));
        }

        @PutMapping("/assignment/reassign")
        public ResponseEntity<ApiResponse<AgentTaskResponse>> reassignAgent(
                        @Valid @RequestBody ReassignDeclarationRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Assignment reassigned successfully",
                                agentService.reassignDeclaration(request)));
        }

        @PutMapping("/assignment/change-agent")
        public ResponseEntity<ApiResponse<AgentTaskResponse>> changeAssignmentAgent(
                        @Valid @RequestBody ChangeAssignmentAgentRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                "Assignment reassigned successfully",
                                agentService.changeAgent(request)));
        }

        @DeleteMapping("/assignment/cancel/{declarationId}")
        public ResponseEntity<ApiResponse<Void>> cancelAssignment(@PathVariable Long declarationId) {
                agentService.cancelAssignment(declarationId);
                return ResponseEntity.ok(ApiResponse.success("Assignment cancelled successfully"));
        }

        @GetMapping("/agents")
        public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getAllAgents(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
                Page<User> agents = userRepository.findByUserType(User.UserType.AGENT, pageable);

                Page<Map<String, Object>> response = agents.map(agent -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("agentId", agent.getUserId());
                        map.put("fullName", agent.getFullName());
                        map.put("employeeId", "EMP-" + agent.getUserId());
                        map.put("assignedZones", agent.getPinCode());
                        map.put("strikeCount", 0);
                        map.put("status", agent.getStatus());
                        return map;
                });

                return ResponseEntity.ok(ApiResponse.success(
                                "Agents fetched successfully", response));
        }

        @GetMapping("/agents/{agentId}")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getAgentById(
                        @PathVariable Long agentId) {

                User agent = userRepository.findById(agentId)
                                .orElseThrow(() -> new RuntimeException("Agent not found")); // Changed from
                                                                                             // ResourceNotFoundException

                Map<String, Object> map = new java.util.HashMap<>();
                map.put("agentId", agent.getUserId());
                map.put("fullName", agent.getFullName());
                map.put("email", agent.getEmail());
                map.put("mobile", agent.getMobile());
                map.put("employeeId", "EMP-" + agent.getUserId());
                map.put("assignedZones", agent.getPinCode());
                map.put("strikeCount", 0);
                map.put("status", agent.getStatus());
                map.put("createdAt", agent.getCreatedAt());

                return ResponseEntity.ok(ApiResponse.success(
                                "Agent details fetched", map));
        }

        @PutMapping("/agents/{agentId}/status")
        public ResponseEntity<ApiResponse<Void>> updateAgentStatus(
                        @PathVariable Long agentId,
                        @RequestParam User.UserStatus status) {

                User agent = userRepository.findById(agentId)
                                .orElseThrow(() -> new RuntimeException("Agent not found")); // Changed from
                                                                                             // ResourceNotFoundException

                agent.setStatus(status);
                userRepository.save(agent);

                return ResponseEntity.ok(ApiResponse.success(
                                "Agent status updated to " + status));
        }

        @PutMapping("/agents/{id}/clear-strikes")
        public ResponseEntity<ApiResponse<Void>> clearStrikes(@PathVariable Long id) {

                User agent = userRepository.findById(id) // Changed from agentRepository
                                .orElseThrow(() -> new RuntimeException("Agent not found"));
                // agent.setStrikeCount(0); // StrikeCount is not directly on User entity
                userRepository.save(agent); // Save the user (agent)
                return ResponseEntity.ok(
                                ApiResponse.success("Strike count cleared"));
        }

        @GetMapping("/declarations")
        public ResponseEntity<ApiResponse<Page<DeclarationResponse>>> getAllDeclarations(
                        @RequestParam(required = false) DeclarationStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size);
                Page<CarbonDeclaration> declarations;

                if (status != null) {
                        declarations = declarationRepo.findByStatusOrderBySubmittedAtDesc(status, pageable);
                } else {
                        declarations = declarationRepo.findAllByOrderBySubmittedAtDesc(pageable); // Changed from
                                                                                                  // findAll(pageable)
                                                                                                  // to maintain order
                }

                Page<DeclarationResponse> responses = declarations.map(d -> {
                        DeclarationResponse resp = DeclarationResponse.builder()
                                        .declarationId(d.getDeclarationId())
                                        .userId(d.getUser().getUserId())
                                        .declarationYear(d.getDeclarationYear())
                                        .status(d.getStatus())
                                        .submittedAt(d.getSubmittedAt())
                                        .resubmissionCount(d.getResubmissionCount())
                                        .build();

                        assignmentRepo.findActiveAssignmentByDeclarationId(d.getDeclarationId())
                                        .ifPresent(assignment -> {
                                                resp.setAssignedAgentId(assignment.getAgent().getUserId());
                                                resp.setAssignedAgentName(assignment.getAgent().getFullName());
                                        });

                        if (resp.getAssignedAgentName() == null) {
                                assignmentRepo.findTopByDeclarationDeclarationIdOrderByAssignedAtDesc(
                                                d.getDeclarationId())
                                                .ifPresent(assignment -> {
                                                        resp.setAssignedAgentId(assignment.getAgent().getUserId());
                                                        resp.setAssignedAgentName(assignment.getAgent().getFullName());
                                                });
                        }

                        return resp;
                });

                return ResponseEntity.ok(ApiResponse.success("Declarations fetched", responses));
        }

        @GetMapping("/declarations/{id}")
        public ResponseEntity<ApiResponse<DeclarationDetailResponse>> getDeclarationById(@PathVariable Long id) {
                return ResponseEntity.ok(
                                ApiResponse.success("Declaration fetched successfully",
                                                declarationModuleService.getDeclarationDetail(id)));
        }

        @PutMapping("/declarations/{id}/unlock")
        public ResponseEntity<ApiResponse<Void>> unlockDeclaration(@PathVariable Long id) {

                CarbonDeclaration declaration = declarationRepo.findById(id)
                                .orElseThrow(() -> new RuntimeException("Declaration not found"));

                if (declaration.getStatus() != DeclarationStatus.SUBMITTED) {
                        throw new RuntimeException("Only SUBMITTED declarations can be unlocked");
                }

                if (declaration.getSubmittedAt().isBefore(
                                java.time.LocalDateTime.now().minusHours(24))) {
                        throw new RuntimeException(
                                        "Unlock window expired — can only unlock within 24 hours of submission");
                }

                boolean agentAssigned = assignmentRepo.existsActiveByDeclarationId(id);

                if (agentAssigned) {
                        throw new RuntimeException("Cannot unlock — agent has already been assigned");
                }

                declaration.setStatus(DeclarationStatus.DRAFT);
                declarationRepo.save(declaration);

                return ResponseEntity.ok(
                                ApiResponse.success("Declaration unlocked for editing"));
        }

        @GetMapping("/assignments")
        public ResponseEntity<ApiResponse<Page<AgentTaskResponse>>> getAllAssignments(
                        @RequestParam(required = false) AssignmentStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size);
                Page<AgentAssignment> assignments;

                if (status != null) {
                        assignments = assignmentRepo.findByAssignmentStatusOrderByAssignedAtDesc(status, pageable);
                } else {
                        assignments = assignmentRepo.findAll(pageable);
                }

                Page<AgentTaskResponse> response = assignments.map(a -> AgentTaskResponse.builder()
                                .assignmentId(a.getAssignmentId())
                                .status(a.getAssignmentStatus())
                                .assignedAt(a.getAssignedAt())
                                .deadline(a.getDeadline())
                                .completedAt(a.getCompletedAt())
                                .overdue(
                                                a.getDeadline() != null &&
                                                                java.time.LocalDateTime.now().isAfter(a.getDeadline())
                                                                && a.getAssignmentStatus() != AssignmentStatus.COMPLETED)
                                .userId(a.getDeclaration().getUser().getUserId())
                                .userName(a.getDeclaration().getUser().getFullName())
                                .declarationId(a.getDeclaration().getDeclarationId())
                                .declarationYear(a.getDeclaration().getDeclarationYear())
                                .agentId(a.getAgent().getUserId())
                                .agentName(a.getAgent().getFullName())
                                .build());

                return ResponseEntity.ok(
                                ApiResponse.success("Assignments fetched", response));
        }

        @PostMapping("/assignments/reassign/{id}")
        public ResponseEntity<ApiResponse<Void>> reassignTask(
                        @PathVariable Long id,
                        @RequestParam Long newAgentId) {

                AgentAssignment assignment = assignmentRepo.findById(id)
                                .orElseThrow(() -> new RuntimeException("Assignment not found"));

                User newAgent = userRepository.findById(newAgentId) // Changed from agentRepository
                                .orElseThrow(() -> new RuntimeException("Agent not found"));

                assignment.setAssignmentStatus(AssignmentStatus.REASSIGNED);
                assignmentRepo.save(assignment);

                AgentAssignment newAssignment = AgentAssignment.builder()
                                .declaration(assignment.getDeclaration())
                                .agent(newAgent)
                                .assignmentStatus(AssignmentStatus.ACTIVE)
                                .build();

                assignmentRepo.save(newAssignment);

                return ResponseEntity.ok(
                                ApiResponse.success("Task reassigned to agent: " + newAgent.getFullName()));
        }

        @GetMapping("/reports/overview")
        public ResponseEntity<ApiResponse<Object>> getOverview() {

                long totalUsers = userRepository.count();
                long totalAgents = userRepository.countByUserType(User.UserType.AGENT);
                long totalDeclarations = declarationRepo.count();
                long pendingVerifications = declarationRepo.countByStatus(
                                DeclarationStatus.UNDER_VERIFICATION);
                long totalScores = scoreRepo.count();
                long flaggedAgents = userRepository.findByUserType(User.UserType.AGENT, Pageable.unpaged())
                                .stream()
                                .filter(u -> u.getStatus() == User.UserStatus.SUSPENDED)
                                .count();

                java.util.Map<String, Object> overview = new java.util.LinkedHashMap<>();
                overview.put("totalUsers", totalUsers);
                overview.put("totalAgents", totalAgents);
                overview.put("totalDeclarations", totalDeclarations);
                overview.put("pendingVerifications", pendingVerifications);
                overview.put("totalScoresGenerated", totalScores);
                overview.put("flaggedAgents", flaggedAgents);

                return ResponseEntity.ok(
                                ApiResponse.success("Overview fetched", overview));
        }

        @GetMapping("/reports/performance")
        public ResponseEntity<ApiResponse<AgentPerformanceResponse>> getAgentPerformanceReport() {

                // For now, return aggregate of all active agents
                // TODO: Could be expanded to take agentId or return list of
                // AgentPerformanceResponse
                long activeCount = userRepository.findByUserType(User.UserType.AGENT, Pageable.unpaged())
                                .stream()
                                .filter(a -> a.getStatus() == User.UserStatus.ACTIVE)
                                .count();

                long totalAssignments = assignmentRepo.count();
                long completedAssignments = assignmentRepo.countByAssignmentStatus(AssignmentStatus.COMPLETED);
                long reassignedAssignments = assignmentRepo.countByAssignmentStatus(AssignmentStatus.REASSIGNED);

                long totalVerifications = verificationRepo.count();
                long confirmedCount = verificationRepo.countByOutcome(VerificationOutcome.CONFIRMED);
                long modifiedCount = verificationRepo.countByOutcome(VerificationOutcome.MODIFIED);
                long rejectedCount = verificationRepo.countByOutcome(VerificationOutcome.REJECTED);

                double completionRate = totalAssignments > 0 ? ((double) completedAssignments / totalAssignments) * 100
                                : 0.0;
                double confirmationRate = totalVerifications > 0 ? ((double) confirmedCount / totalVerifications) * 100
                                : 0.0;
                double modificationRate = totalVerifications > 0 ? ((double) modifiedCount / totalVerifications) * 100
                                : 0.0;

                int totalStrikes = 0;

                AgentPerformanceResponse response = AgentPerformanceResponse.builder()
                                .agentId(0L)
                                .fullName("Active Field Staff (" + activeCount + ")")
                                .employeeId("AGG-000")
                                .strikeCount(totalStrikes)
                                .totalAssignments(totalAssignments)
                                .completedAssignments(completedAssignments)
                                .reassignedAssignments(reassignedAssignments)
                                .totalVerifications(totalVerifications)
                                .confirmedCount(confirmedCount)
                                .modifiedCount(modifiedCount)
                                .rejectedCount(rejectedCount)
                                .completionRate(completionRate)
                                .confirmationRate(confirmationRate)
                                .modificationRate(modificationRate)
                                .build();

                return ResponseEntity.ok(ApiResponse.success("Performance report fetched", response));
        }

        @GetMapping("/reports/carbon-heatmap")
        public ResponseEntity<ApiResponse<Object>> getCarbonHeatmap(@RequestParam(defaultValue = "0") int year) {

                int targetYear = year == 0
                                ? java.time.LocalDate.now().getYear()
                                : year;

                Double cityData = scoreRepo.calculateAveragePerCapitaCo2ByCity(targetYear);
                Double pinCodeData = scoreRepo.calculateAveragePerCapitaCo2ByPinCode(targetYear);

                java.util.Map<String, Object> heatmap = new java.util.LinkedHashMap<>();
                heatmap.put("year", targetYear);
                heatmap.put("byCity", cityData);
                heatmap.put("byPinCode", pinCodeData);

                return ResponseEntity.ok(
                                ApiResponse.success("Heatmap data fetched", heatmap));
        }
}
