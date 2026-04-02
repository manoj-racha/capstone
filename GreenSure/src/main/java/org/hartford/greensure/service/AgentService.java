package org.hartford.greensure.service;

import org.hartford.greensure.dto.request.ChangeAssignmentAgentRequest;
import org.hartford.greensure.dto.request.ManualAssignmentRequest;
import org.hartford.greensure.dto.request.ReassignDeclarationRequest;
import org.hartford.greensure.dto.request.VerificationRequest;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.enums.AssignmentStatus;
import org.hartford.greensure.enums.DeclarationStatus;
import org.hartford.greensure.exception.AgentNotAvailableException;
import org.hartford.greensure.exception.AgentNotFoundException;
import org.hartford.greensure.exception.AssignmentNotFoundException;
import org.hartford.greensure.exception.BadRequestException;
import org.hartford.greensure.exception.DeclarationAlreadyAssignedException;
import org.hartford.greensure.exception.ResourceNotFoundException;
import org.hartford.greensure.exception.UnauthorizedException;
import org.hartford.greensure.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentService {

        private static final int MAX_ACTIVE_ASSIGNMENTS = 5;

        @Autowired
        private UserRepository userRepository;
        @Autowired
        private AgentAssignmentRepository assignmentRepo;
        @Autowired
        private VerificationRepository verificationRepo;
        @Autowired
        private CarbonDeclarationRepository declarationRepo;
        @Autowired
        private NotificationService notificationService;
        @Autowired
        private org.hartford.greensure.engine.CarbonScoreService carbonScoreService;

        public List<AgentTaskResponse> getDashboard(Long agentId) {
                return assignmentRepo.findByAgentUserIdOrderByDeadlineAsc(agentId)
                                .stream()
                                .map(this::mapToTaskResponse)
                                .collect(Collectors.toList());
        }

        public List<AgentTaskResponse> getAssignments(Long agentId, AssignmentStatus status) {
                List<AgentAssignment> assignments = status != null
                                ? assignmentRepo.findByAgentUserIdAndAssignmentStatus(agentId, status)
                                : assignmentRepo.findByAgentUserIdOrderByDeadlineAsc(agentId);

                return assignments.stream()
                                .map(this::mapToTaskResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public AgentTaskResponse startAssignment(Long assignmentId, Long agentId) {
                AgentAssignment assignment = getAndValidateAssignment(assignmentId, agentId);

                if (assignment.getAssignmentStatus() != AssignmentStatus.ACTIVE) {
                        throw new BadRequestException("Assignment is not in ACTIVE status");
                }

                // In the new unified enum, ACTIVE covers both ASSIGNED and IN_PROGRESS.
                // We keep it as ACTIVE, but we could add more specific flags if needed.
                assignment.setAssignmentStatus(AssignmentStatus.ACTIVE);
                assignment = assignmentRepo.save(assignment);
                return mapToTaskResponse(assignment);
        }

        public List<CarbonDeclaration> getUnassignedDeclarations() {
                return declarationRepo.findByStatus(DeclarationStatus.SUBMITTED)
                                .stream()
                                .filter(d -> !assignmentRepo.existsActiveByDeclarationId(d.getDeclarationId()))
                                .toList();
        }

        public List<AgentTaskResponse> getActiveAssignments() {
                return assignmentRepo.findActiveAssignments()
                                .stream()
                                .map(this::mapToTaskResponse)
                                .toList();
        }

        public List<User> getAvailableAgents() {
                return userRepository.findByUserType(User.UserType.AGENT, org.springframework.data.domain.Pageable.unpaged())
                                .stream()
                                .filter(a -> a.getStatus() == User.UserStatus.ACTIVE)
                                .filter(this::isAgentAvailable)
                                .toList();
        }

        @Transactional
        public AgentTaskResponse assignDeclaration(ManualAssignmentRequest request) {
                CarbonDeclaration declaration = declarationRepo.findById(request.getDeclarationId())
                                .orElseThrow(() -> new ResourceNotFoundException("Declaration not found"));

                if (assignmentRepo.existsActiveByDeclarationId(declaration.getDeclarationId())) {
                        throw new DeclarationAlreadyAssignedException("Declaration already has an active assignment.");
                }

                User agent = userRepository.findById(request.getAgentId())
                                .orElseThrow(() -> new AgentNotFoundException("Agent not found"));

                ensureAgentAvailable(agent);

                AgentAssignment assignment = AgentAssignment.builder()
                                .declaration(declaration)
                                .agent(agent)
                                .assignmentStatus(AssignmentStatus.ACTIVE)
                                .assignedBy("ADMIN")
                                .build();
                assignment = assignmentRepo.save(assignment);

                declaration.setStatus(DeclarationStatus.UNDER_VERIFICATION);
                declarationRepo.save(declaration);

                return mapToTaskResponse(assignment);
        }

        @Transactional
        public AgentTaskResponse reassignDeclaration(ReassignDeclarationRequest request) {
                AgentAssignment activeAssignment = assignmentRepo.findActiveAssignmentByDeclarationId(request.getDeclarationId())
                                .orElseThrow(() -> new AssignmentNotFoundException("Active assignment not found for declaration."));

                return performReassignment(activeAssignment, request.getNewAgentId(), request.getReason());
        }

        @Transactional
        public AgentTaskResponse changeAgent(ChangeAssignmentAgentRequest request) {
                AgentAssignment assignment = assignmentRepo.findById(request.getAssignmentId())
                                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found."));

                if (!assignment.isActive()) {
                        throw new AssignmentNotFoundException("Assignment is not active.");
                }

                return performReassignment(assignment, request.getNewAgentId(), request.getReason());
        }

        @Transactional
        public void cancelAssignment(Long declarationId) {
                AgentAssignment assignment = assignmentRepo.findActiveAssignmentByDeclarationId(declarationId)
                                .orElseThrow(() -> new AssignmentNotFoundException("Active assignment not found for declaration."));

                assignment.setAssignmentStatus(AssignmentStatus.CANCELLED);
                assignmentRepo.save(assignment);

                CarbonDeclaration declaration = assignment.getDeclaration();
                declaration.setStatus(DeclarationStatus.SUBMITTED);
                declarationRepo.save(declaration);
        }

        @Transactional
        public void submitVerification(Long assignmentId, Long agentId, VerificationRequest request) {
                // Deprecated — superseded by AgentVerificationService
        }

        public AgentPerformanceResponse getPerformance(Long agentId) {
                User agent = userRepository.findById(agentId)
                                .orElseThrow(() -> new AgentNotFoundException("Agent not found"));

                long completed = assignmentRepo.countByAgentUserIdAndAssignmentStatus(agentId, AssignmentStatus.COMPLETED);
                long reassigned = assignmentRepo.countByAgentUserIdAndAssignmentStatus(agentId, AssignmentStatus.REASSIGNED);
                long total = completed + reassigned;

                long confirmed = verificationRepo.countByAgentUserIdAndOutcome(agentId, org.hartford.greensure.enums.VerificationOutcome.CONFIRMED);
                long modified = verificationRepo.countByAgentUserIdAndOutcome(agentId, org.hartford.greensure.enums.VerificationOutcome.MODIFIED);
                long rejected = verificationRepo.countByAgentUserIdAndOutcome(agentId, org.hartford.greensure.enums.VerificationOutcome.REJECTED);
                long totalVerifications = confirmed + modified + rejected;

                double completionRate = total > 0 ? (completed * 100.0 / total) : 0;
                double modificationRate = totalVerifications > 0 ? (modified * 100.0 / totalVerifications) : 0;
                double confirmationRate = totalVerifications > 0 ? (confirmed * 100.0 / totalVerifications) : 0;

                return AgentPerformanceResponse.builder()
                                .agentId(agentId)
                                .fullName(agent.getFullName())
                                .employeeId("EMP-" + agentId)
                                .strikeCount(0)
                                .totalAssignments(total)
                                .completedAssignments(completed)
                                .reassignedAssignments(reassigned)
                                .totalVerifications(totalVerifications)
                                .confirmedCount(confirmed)
                                .modifiedCount(modified)
                                .rejectedCount(rejected)
                                .completionRate(completionRate)
                                .modificationRate(modificationRate)
                                .confirmationRate(confirmationRate)
                                .build();
        }

        private AgentAssignment getAndValidateAssignment(Long assignmentId, Long agentId) {
                AgentAssignment assignment = assignmentRepo.findById(assignmentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

                if (!assignment.getAgent().getUserId().equals(agentId)) {
                        throw new UnauthorizedException("Access denied — this assignment does not belong to you");
                }

                return assignment;
        }

        private AgentTaskResponse mapToTaskResponse(AgentAssignment a) {
                User user = a.getDeclaration().getUser();
                return AgentTaskResponse.builder()
                                .assignmentId(a.getAssignmentId())
                                .status(a.getAssignmentStatus())
                                .assignedAt(a.getAssignedAt())
                                .deadline(a.getDeadline())
                                .completedAt(a.getCompletedAt())
                                .assignedBy(a.getAssignedBy())
                                .reassignReason(a.getReassignReason())
                                .overdue(a.getDeadline() != null &&
                                                LocalDateTime.now().isAfter(a.getDeadline()) &&
                                                a.getAssignmentStatus() != AssignmentStatus.COMPLETED)
                                .userId(user.getUserId())
                                .userName(user.getFullName())
                                .userAddress(user.getAddress())
                                .userPinCode(user.getPinCode())
                                .userCity(user.getCity())
                                .userMobile(user.getMobile())
                                .userType(user.getUserType())
                                .declarationId(a.getDeclaration().getDeclarationId())
                                .declarationYear(a.getDeclaration().getDeclarationYear())
                                .agentId(a.getAgent().getUserId())
                                .agentName(a.getAgent().getFullName())
                                .build();
        }

        private AgentTaskResponse performReassignment(AgentAssignment current, Long newAgentId, String reason) {
                User newAgent = userRepository.findById(newAgentId)
                                .orElseThrow(() -> new AgentNotFoundException("Agent not found"));

                if (current.getAgent().getUserId().equals(newAgent.getUserId())) {
                        throw new BadRequestException("New agent must be different from current agent.");
                }

                ensureAgentAvailable(newAgent);

                current.setAssignmentStatus(AssignmentStatus.REASSIGNED);
                current.setReassignReason(reason);
                assignmentRepo.save(current);

                AgentAssignment newAssignment = AgentAssignment.builder()
                                .declaration(current.getDeclaration())
                                .agent(newAgent)
                                .assignmentStatus(AssignmentStatus.ACTIVE)
                                .assignedBy("ADMIN")
                                .reassignReason(reason)
                                .build();
                newAssignment = assignmentRepo.save(newAssignment);

                return mapToTaskResponse(newAssignment);
        }

        private void ensureAgentAvailable(User agent) {
                if (agent.getUserType() != User.UserType.AGENT || agent.getStatus() != User.UserStatus.ACTIVE) {
                        throw new AgentNotAvailableException("Selected agent is not available for assignment.");
                }
                if (!isAgentAvailable(agent)) {
                        throw new AgentNotAvailableException("Selected agent is at full capacity.");
                }
        }

        private boolean isAgentAvailable(User agent) {
                return assignmentRepo.countActiveAssignmentsByUserId(agent.getUserId()) < MAX_ACTIVE_ASSIGNMENTS;
        }

        public DeclarationResponse getDeclarationForAssignment(Long assignmentId, Long agentId) {
                return null; // Deprecated
        }
}
