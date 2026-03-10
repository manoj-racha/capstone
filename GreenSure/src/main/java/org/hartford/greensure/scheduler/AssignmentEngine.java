package org.hartford.greensure.scheduler;

import org.hartford.greensure.entity.Agent;
import org.hartford.greensure.entity.AgentAssignment;
import org.hartford.greensure.entity.CarbonDeclaration;
import org.hartford.greensure.entity.Notification;
import org.hartford.greensure.repository.AgentAssignmentRepository;
import org.hartford.greensure.repository.AgentRepository;
import org.hartford.greensure.repository.CarbonDeclarationRepository;
import org.hartford.greensure.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class AssignmentEngine {

        @Autowired
        private CarbonDeclarationRepository declarationRepo;
        @Autowired
        private AgentRepository agentRepository;
        @Autowired
        private AgentAssignmentRepository assignmentRepo;
        @Autowired
        private NotificationService notificationService;

        // Runs every 2 minutes
        @Scheduled(fixedRate = 120000) // 2 minutes in ms
        @Transactional
        public void assignPendingDeclarations() {

                System.out.println(
                                "[AssignmentEngine] Running assignment check...");

                // Find all SUBMITTED declarations
                List<CarbonDeclaration> submitted = declarationRepo
                                .findByStatus(
                                                CarbonDeclaration.DeclarationStatus.SUBMITTED);

                int assigned = 0;
                int skipped = 0;

                for (CarbonDeclaration declaration : submitted) {

                        // Check if already has an active assignment
                        boolean alreadyAssigned = assignmentRepo
                                        .existsByDeclarationDeclarationIdAndStatusIn(
                                                        declaration.getDeclarationId(),
                                                        List.of(
                                                                        AgentAssignment.AssignmentStatus.ASSIGNED,
                                                                        AgentAssignment.AssignmentStatus.IN_PROGRESS));

                        if (alreadyAssigned) {
                                skipped++;
                                continue;
                        }

                        // Find least busy agent in user's pin code
                        String pinCode = declaration.getUser().getPinCode();

                        List<Agent> availableAgents = agentRepository
                                        .findLeastBusyAgentsByPinCode(pinCode);

                        if (availableAgents.isEmpty()) {
                                System.out.println(
                                                "[AssignmentEngine] No agents available " +
                                                                "for pin code: " + pinCode +
                                                                " — Declaration ID: " +
                                                                declaration.getDeclarationId());
                                continue;
                        }

                        // Pick the least busy agent — first in sorted list
                        Agent selectedAgent = availableAgents.get(0);

                        // Create assignment — deadline auto-set to 72h
                        // in AgentAssignment @PrePersist
                        AgentAssignment assignment = AgentAssignment.builder()
                                        .declaration(declaration)
                                        .agent(selectedAgent)
                                        .status(AgentAssignment.AssignmentStatus.ASSIGNED)
                                        .build();

                        assignmentRepo.save(assignment);

                        // Update declaration status
                        declaration.setStatus(
                                        CarbonDeclaration.DeclarationStatus.UNDER_VERIFICATION);
                        declarationRepo.save(declaration);

                        // Notify user
                        notificationService.sendToUser(
                                        declaration.getUser().getUserId(),
                                        Notification.NotificationType.ASSIGNMENT_ALERT,
                                        "A field agent has been assigned to verify " +
                                                        "your declaration. They will visit your " +
                                                        "address within 72 hours.");

                        // Notify agent
                        notificationService.sendToAgent(
                                        selectedAgent.getAgentId(),
                                        Notification.NotificationType.ASSIGNMENT_ALERT,
                                        "New verification task assigned. " +
                                                        "User: " + declaration.getUser().getFullName() +
                                                        ", Address: " + declaration.getUser().getAddress() +
                                                        ", City: " + declaration.getUser().getCity() +
                                                        ". Please complete within 72 hours.");

                        assigned++;
                        System.out.println(
                                        "[AssignmentEngine] Assigned declaration " +
                                                        declaration.getDeclarationId() +
                                                        " to agent " + selectedAgent.getFullName());
                }

                System.out.println(
                                "[AssignmentEngine] Done. Assigned: " + assigned +
                                                ", Skipped (already assigned): " + skipped);
        }
}
