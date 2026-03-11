package org.hartford.greensure.controller;

import org.hartford.greensure.dto.request.CreateAgentRequest;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.security.JwtUtil;
import org.hartford.greensure.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

        @Autowired
        private UserRepository userRepository;
        @Autowired
        private AgentRepository agentRepository;
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
        private PasswordEncoder passwordEncoder;
        @Autowired
        private JwtUtil jwtUtil;
        @Autowired
        private EmailService emailService;

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
                                        d.setStatus(CarbonDeclaration.DeclarationStatus.DRAFT);
                                        declarationRepo.save(d);
                                });

                return ResponseEntity.ok(
                                ApiResponse.success("Account unlocked. User can resubmit."));
        }

        @PostMapping("/agents/create")
        public ResponseEntity<ApiResponse<Void>> createAgent(
                        @Valid @RequestBody CreateAgentRequest request) {

                if (agentRepository.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("Email already registered");
                }
                if (agentRepository.existsByMobile(request.getMobile())) {
                        throw new RuntimeException("Mobile already registered");
                }
                if (agentRepository.existsByEmployeeId(request.getEmployeeId())) {
                        throw new RuntimeException("Employee ID already exists");
                }

                Agent agent = Agent.builder()
                                .agentType(request.getAgentType())
                                .fullName(request.getFullName())
                                .email(request.getEmail())
                                .mobile(request.getMobile())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .employeeId(request.getEmployeeId())
                                .assignedZones(request.getAssignedZones())
                                .strikeCount(0)
                                .status(Agent.AgentStatus.ACTIVE)
                                .build();

                agentRepository.save(agent);

                // Send welcome email with credentials (best-effort)
                emailService.sendAgentWelcomeEmail(
                                request.getEmail(),
                                request.getFullName(),
                                request.getPassword());

                return ResponseEntity.ok(
                                ApiResponse.success("Agent created successfully. Welcome email sent."));
        }

        @GetMapping("/agents")
        public ResponseEntity<ApiResponse<Page<java.util.Map<String, Object>>>> getAllAgents(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size);
                Page<Agent> agents = agentRepository.findByAgentType(Agent.AgentType.FIELD_AGENT, pageable);

                Page<java.util.Map<String, Object>> responses = agents.map(agent -> {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("agentId", agent.getAgentId());
                        map.put("fullName", agent.getFullName());
                        map.put("employeeId", agent.getEmployeeId());
                        map.put("assignedZones", agent.getAssignedZones());
                        map.put("strikeCount", agent.getStrikeCount());
                        map.put("status", agent.getStatus());
                        map.put("createdAt", agent.getCreatedAt());
                        return map;
                });

                return ResponseEntity.ok(ApiResponse.success("Agents fetched", responses));
        }

        @GetMapping("/agents/{id}")
        public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getAgentById(@PathVariable Long id) {
                Agent agent = agentRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Agent not found"));

                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("agentId", agent.getAgentId());
                map.put("fullName", agent.getFullName());
                map.put("email", agent.getEmail());
                map.put("mobile", agent.getMobile());
                map.put("employeeId", agent.getEmployeeId());
                map.put("assignedZones", agent.getAssignedZones());
                map.put("strikeCount", agent.getStrikeCount());
                map.put("status", agent.getStatus());
                map.put("agentType", agent.getAgentType());
                map.put("createdAt", agent.getCreatedAt());

                return ResponseEntity.ok(ApiResponse.success("Agent fetched", map));
        }

        @PutMapping("/agents/{id}/status")
        public ResponseEntity<ApiResponse<Void>> updateAgentStatus(
                        @PathVariable Long id,
                        @RequestParam Agent.AgentStatus status) {

                Agent agent = agentRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Agent not found"));
                agent.setStatus(status);
                agentRepository.save(agent);
                return ResponseEntity.ok(
                                ApiResponse.success("Agent status updated to " + status));
        }

        @PutMapping("/agents/{id}/clear-strikes")
        public ResponseEntity<ApiResponse<Void>> clearStrikes(@PathVariable Long id) {

                Agent agent = agentRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Agent not found"));
                agent.setStrikeCount(0);
                agentRepository.save(agent);
                return ResponseEntity.ok(
                                ApiResponse.success("Strike count cleared"));
        }

        @GetMapping("/declarations")
        public ResponseEntity<ApiResponse<Page<DeclarationResponse>>> getAllDeclarations(
                        @RequestParam(required = false) CarbonDeclaration.DeclarationStatus status,
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
                                        resp.setAssignedAgentId(assignment.getAgent().getAgentId());
                                        resp.setAssignedAgentName(assignment.getAgent().getFullName());
                                });

                        return resp;
                });

                return ResponseEntity.ok(ApiResponse.success("Declarations fetched", responses));
        }

        @GetMapping("/declarations/{id}")
        public ResponseEntity<ApiResponse<DeclarationResponse>> getDeclarationById(@PathVariable Long id) {

                CarbonDeclaration declaration = declarationRepo.findById(id)
                                .orElseThrow(() -> new RuntimeException("Declaration not found"));

                DeclarationResponse response = declarationService.mapToResponse(declaration);

                return ResponseEntity.ok(
                                ApiResponse.success("Declaration fetched successfully", response));
        }

        @PutMapping("/declarations/{id}/unlock")
        public ResponseEntity<ApiResponse<Void>> unlockDeclaration(@PathVariable Long id) {

                CarbonDeclaration declaration = declarationRepo.findById(id)
                                .orElseThrow(() -> new RuntimeException("Declaration not found"));

                if (declaration.getStatus() != CarbonDeclaration.DeclarationStatus.SUBMITTED) {
                        throw new RuntimeException("Only SUBMITTED declarations can be unlocked");
                }

                if (declaration.getSubmittedAt().isBefore(
                                java.time.LocalDateTime.now().minusHours(24))) {
                        throw new RuntimeException(
                                        "Unlock window expired — can only unlock within 24 hours of submission");
                }

                boolean agentAssigned = assignmentRepo
                                .existsByDeclarationDeclarationIdAndStatusIn(
                                                id,
                                                java.util.List.of(
                                                                AgentAssignment.AssignmentStatus.ASSIGNED,
                                                                AgentAssignment.AssignmentStatus.IN_PROGRESS));

                if (agentAssigned) {
                        throw new RuntimeException("Cannot unlock — agent has already been assigned");
                }

                declaration.setStatus(CarbonDeclaration.DeclarationStatus.DRAFT);
                declarationRepo.save(declaration);

                return ResponseEntity.ok(
                                ApiResponse.success("Declaration unlocked for editing"));
        }

        @GetMapping("/assignments")
        public ResponseEntity<ApiResponse<Page<AgentTaskResponse>>> getAllAssignments(
                        @RequestParam(required = false) AgentAssignment.AssignmentStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size);
                Page<AgentAssignment> assignments;

                if (status != null) {
                        assignments = assignmentRepo.findByStatusOrderByAssignedAtDesc(status, pageable);
                } else {
                        assignments = assignmentRepo.findAll(pageable);
                }

                Page<AgentTaskResponse> response = assignments.map(a -> AgentTaskResponse.builder()
                                .assignmentId(a.getAssignmentId())
                                .status(a.getStatus())
                                .assignedAt(a.getAssignedAt())
                                .deadline(a.getDeadline())
                                .completedAt(a.getCompletedAt())
                                .isOverdue(
                                                java.time.LocalDateTime.now().isAfter(a.getDeadline())
                                                                && a.getStatus() != AgentAssignment.AssignmentStatus.COMPLETED)
                                .userId(a.getDeclaration().getUser().getUserId())
                                .userName(a.getDeclaration().getUser().getFullName())
                                .declarationId(a.getDeclaration().getDeclarationId())
                                .declarationYear(a.getDeclaration().getDeclarationYear())
                                .agentId(a.getAgent().getAgentId())
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

                Agent newAgent = agentRepository.findById(newAgentId)
                                .orElseThrow(() -> new RuntimeException("Agent not found"));

                assignment.setStatus(AgentAssignment.AssignmentStatus.REASSIGNED);
                assignmentRepo.save(assignment);

                AgentAssignment newAssignment = AgentAssignment.builder()
                                .declaration(assignment.getDeclaration())
                                .agent(newAgent)
                                .status(AgentAssignment.AssignmentStatus.ASSIGNED)
                                .build();

                assignmentRepo.save(newAssignment);

                return ResponseEntity.ok(
                                ApiResponse.success("Task reassigned to agent: " + newAgent.getFullName()));
        }

        @GetMapping("/reports/overview")
        public ResponseEntity<ApiResponse<Object>> getOverview() {

                long totalUsers = userRepository.count();
                long totalAgents = agentRepository.count();
                long totalDeclarations = declarationRepo.count();
                long pendingVerifications = declarationRepo.countByStatus(
                                CarbonDeclaration.DeclarationStatus.UNDER_VERIFICATION);
                long totalScores = scoreRepo.count();
                long flaggedAgents = agentRepository.findByStrikeCountGreaterThanEqual(3).size();

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
                Pageable firstPage = PageRequest.of(0, 100);
                Page<Agent> agents = agentRepository.findByAgentType(Agent.AgentType.FIELD_AGENT, firstPage);

                long activeCount = agents.getTotalElements();

                long totalAssignments = assignmentRepo.count();
                long completedAssignments = assignmentRepo.countByStatus(AgentAssignment.AssignmentStatus.COMPLETED);
                long reassignedAssignments = assignmentRepo.countByStatus(AgentAssignment.AssignmentStatus.REASSIGNED);

                long totalVerifications = verificationRepo.count();
                long confirmedCount = verificationRepo.countByOverallAction(Verification.VerificationAction.CONFIRMED);
                long modifiedCount = verificationRepo.countByOverallAction(Verification.VerificationAction.MODIFIED);
                long rejectedCount = verificationRepo.countByOverallAction(Verification.VerificationAction.REJECTED);

                double completionRate = totalAssignments > 0 ? ((double) completedAssignments / totalAssignments) * 100 : 0.0;
                double confirmationRate = totalVerifications > 0 ? ((double) confirmedCount / totalVerifications) * 100 : 0.0;
                double modificationRate = totalVerifications > 0 ? ((double) modifiedCount / totalVerifications) * 100 : 0.0;

                int totalStrikes = agents.getContent().stream().mapToInt(Agent::getStrikeCount).sum();

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

                List<Object[]> cityData = scoreRepo.calculateAveragePerCapitaCo2ByCity(targetYear);
                List<Object[]> pinCodeData = scoreRepo.calculateAveragePerCapitaCo2ByPinCode(targetYear);

                java.util.Map<String, Object> heatmap = new java.util.LinkedHashMap<>();
                heatmap.put("year", targetYear);
                heatmap.put("byCity", cityData);
                heatmap.put("byPinCode", pinCodeData);

                return ResponseEntity.ok(
                                ApiResponse.success("Heatmap data fetched", heatmap));
        }
}
