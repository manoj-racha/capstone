package org.hartford.greensure.scheduler;


import org.hartford.greensure.entity.Agent;
import org.hartford.greensure.entity.AgentAssignment;
import org.hartford.greensure.entity.CarbonDeclaration;
import org.hartford.greensure.entity.Notification;
import org.hartford.greensure.repository.AgentAssignmentRepository;
import org.hartford.greensure.repository.AgentRepository;
import org.hartford.greensure.repository.CarbonDeclarationRepository;
import org.hartford.greensure.repository.NotificationRepository;
import org.hartford.greensure.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DeadlineMonitor {

    @Autowired
    private AgentAssignmentRepository assignmentRepo;
    @Autowired private AgentRepository agentRepository;
    @Autowired private CarbonDeclarationRepository declarationRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private NotificationService notificationService;

    // Runs every hour
    @Scheduled(fixedRate = 3600000) // 1 hour in ms
    @Transactional
    public void monitorDeadlines() {

        System.out.println(
                "[DeadlineMonitor] Running deadline check...");

        checkAndSendReminders();
        checkAndReassignOverdue();

        System.out.println(
                "[DeadlineMonitor] Deadline check complete.");
    }

    // ── STEP 1 — Send 48-hour reminders ───────────────────────

    private void checkAndSendReminders() {

        // Find assignments whose deadline is between
        // 23 hours and 25 hours from now
        // (the 48-hour mark ± 1 hour window)
        LocalDateTime reminderStart =
                LocalDateTime.now().plusHours(23);
        LocalDateTime reminderEnd =
                LocalDateTime.now().plusHours(25);

        List<AgentAssignment> needingReminder = assignmentRepo
                .findAssignmentsNeedingReminder(
                        reminderStart, reminderEnd);

        for (AgentAssignment assignment : needingReminder) {

            Long agentId = assignment.getAgent().getAgentId();

            // Check if reminder already sent in last 2 hours
            // Prevents duplicate reminders if monitor runs twice
            boolean alreadySent = notificationRepo
                    .existsRecentDeadlineReminder(
                            agentId,
                            LocalDateTime.now().minusHours(2));

            if (!alreadySent) {
                notificationService.sendToAgent(
                        agentId,
                        Notification.NotificationType.DEADLINE_REMINDER,
                        "REMINDER: You have less than 24 hours to " +
                                "complete verification for " +
                                assignment.getDeclaration()
                                        .getUser().getFullName() +
                                " at " +
                                assignment.getDeclaration()
                                        .getUser().getAddress() +
                                ". Deadline: " + assignment.getDeadline() +
                                ". Failure to submit will result in a strike."
                );

                System.out.println(
                        "[DeadlineMonitor] Sent 48h reminder to " +
                                "agent: " +
                                assignment.getAgent().getFullName());
            }
        }
    }

    // ── STEP 2 — Reassign overdue tasks ───────────────────────

    private void checkAndReassignOverdue() {

        // Find all assignments past their 72-hour deadline
        List<AgentAssignment> overdue = assignmentRepo
                .findOverdueAssignments(LocalDateTime.now());

        for (AgentAssignment overdueAssignment : overdue) {

            Agent originalAgent = overdueAssignment.getAgent();
            CarbonDeclaration declaration =
                    overdueAssignment.getDeclaration();

            System.out.println(
                    "[DeadlineMonitor] Overdue assignment found. " +
                            "Agent: " + originalAgent.getFullName() +
                            ", Declaration: " +
                            declaration.getDeclarationId());

            // Step A — Mark original assignment REASSIGNED
            overdueAssignment.setStatus(
                    AgentAssignment.AssignmentStatus.REASSIGNED);
            assignmentRepo.save(overdueAssignment);

            // Step B — Add strike to original agent
            originalAgent.setStrikeCount(
                    originalAgent.getStrikeCount() + 1);
            agentRepository.save(originalAgent);

            System.out.println(
                    "[DeadlineMonitor] Strike added to agent: " +
                            originalAgent.getFullName() +
                            ". Total strikes: " +
                            originalAgent.getStrikeCount());

            // Step C — Check if agent has reached 3 strikes
            if (originalAgent.getStrikeCount() >= 3) {
                System.out.println(
                        "[DeadlineMonitor] ALERT: Agent " +
                                originalAgent.getFullName() +
                                " has reached 3 strikes. " +
                                "Admin review required.");

                // Notify admin about flagged agent
                // Find all admins and notify them
                agentRepository.findByAgentType(
                                Agent.AgentType.ADMIN)
                        .forEach(admin ->
                                notificationService.sendToAgent(
                                        admin.getAgentId(),
                                        Notification.NotificationType
                                                .ASSIGNMENT_ALERT,
                                        "ALERT: Agent " +
                                                originalAgent.getFullName() +
                                                " (ID: " +
                                                originalAgent.getEmployeeId() +
                                                ") has reached 3 strikes. " +
                                                "Please review and take action."
                                ));
            }

            // Step D — Find new agent in same pin code
            // Exclude the original agent to ensure different agent
            String pinCode = declaration.getUser().getPinCode();

            List<Agent> availableAgents = agentRepository
                    .findLeastBusyAgentsByPinCode(pinCode)
                    .stream()
                    .filter(a -> !a.getAgentId().equals(
                            originalAgent.getAgentId()))
                    .toList();

            if (availableAgents.isEmpty()) {
                System.out.println(
                        "[DeadlineMonitor] No replacement agent " +
                                "found for pin code: " + pinCode +
                                ". Admin must manually assign.");

                // Notify admin — manual intervention needed
                agentRepository.findByAgentType(
                                Agent.AgentType.ADMIN)
                        .forEach(admin ->
                                notificationService.sendToAgent(
                                        admin.getAgentId(),
                                        Notification.NotificationType
                                                .ASSIGNMENT_ALERT,
                                        "URGENT: No available agent found " +
                                                "for pin code " + pinCode +
                                                " for declaration " +
                                                declaration.getDeclarationId() +
                                                ". Manual assignment required."
                                ));
                continue;
            }

            // Step E — Assign to new agent
            // New agent gets fresh 72-hour window
            // New agent does NOT see previous agent's notes
            Agent newAgent = availableAgents.get(0);

            AgentAssignment newAssignment = AgentAssignment.builder()
                    .declaration(declaration)
                    .agent(newAgent)
                    .status(AgentAssignment.AssignmentStatus.ASSIGNED)
                    .build();

            assignmentRepo.save(newAssignment);

            // Step F — Notify original agent of strike
            notificationService.sendToAgent(
                    originalAgent.getAgentId(),
                    Notification.NotificationType.REASSIGNMENT_ALERT,
                    "Your assignment for " +
                            declaration.getUser().getFullName() +
                            " has been reassigned due to missed deadline. " +
                            "A strike has been added to your account. " +
                            "Current strikes: " +
                            originalAgent.getStrikeCount()
            );

            // Step G — Notify user of reassignment
            notificationService.sendToUser(
                    declaration.getUser().getUserId(),
                    Notification.NotificationType.REASSIGNMENT_ALERT,
                    "We have reassigned your verification to a " +
                            "new field agent. They will visit you within " +
                            "72 hours. We apologize for the delay."
            );

            // Step H — Notify new agent
            notificationService.sendToAgent(
                    newAgent.getAgentId(),
                    Notification.NotificationType.ASSIGNMENT_ALERT,
                    "New verification task assigned. " +
                            "User: " +
                            declaration.getUser().getFullName() +
                            ", Address: " +
                            declaration.getUser().getAddress() +
                            ". This is a reassigned task — " +
                            "complete within 72 hours."
            );

            System.out.println(
                    "[DeadlineMonitor] Reassigned declaration " +
                            declaration.getDeclarationId() +
                            " from " + originalAgent.getFullName() +
                            " to " + newAgent.getFullName());
        }
    }
}
