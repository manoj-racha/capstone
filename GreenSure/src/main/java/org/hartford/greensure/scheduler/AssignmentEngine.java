package org.hartford.greensure.scheduler;

import org.hartford.greensure.entity.AgentAssignment;
import org.hartford.greensure.entity.CarbonDeclaration;
import org.hartford.greensure.entity.Notification;
import org.hartford.greensure.entity.User;
import org.hartford.greensure.enums.AssignmentStatus;
import org.hartford.greensure.enums.DeclarationStatus;
import org.hartford.greensure.repository.AgentAssignmentRepository;
import org.hartford.greensure.repository.CarbonDeclarationRepository;
import org.hartford.greensure.repository.UserRepository;
import org.hartford.greensure.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Component
public class AssignmentEngine {
    private static final Logger log = LoggerFactory.getLogger(AssignmentEngine.class);

    @Autowired
    private CarbonDeclarationRepository declarationRepo;
    @Autowired
    private AgentAssignmentRepository assignmentRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private NotificationService notificationService;

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void assignSubmittedDeclarations() {
        List<CarbonDeclaration> pending = declarationRepo.findUnassignedSubmitted();
        int assigned = 0;
        int skipped = 0;

        for (CarbonDeclaration declaration : pending) {
            User selectedAgent = findBestAgent(declaration.getUser().getPinCode());
            if (selectedAgent == null) {
                skipped++;
                log.warn("No available agent for declaration {}", declaration.getDeclarationId());
                continue;
            }

            AgentAssignment assignment = AgentAssignment.builder()
                    .declaration(declaration)
                    .agent(selectedAgent)
                    .assignmentStatus(AssignmentStatus.ACTIVE)
                    .assignedBy("SYSTEM")
                    .build();
            assignmentRepo.save(assignment);

            declaration.setStatus(DeclarationStatus.UNDER_VERIFICATION);
            declarationRepo.save(declaration);

            notificationService.sendToUser(
                    declaration.getUser().getUserId(),
                    Notification.NotificationType.ASSIGNMENT_ALERT,
                    "A field agent has been assigned to verify your declaration.");
            notificationService.sendToAgent(
                    selectedAgent.getUserId(),
                    Notification.NotificationType.ASSIGNMENT_ALERT,
                    "New verification task assigned. Please complete within 72 hours.");

            assigned++;
        }

        log.info("AssignmentEngine completed. Assigned={}, Skipped={}", assigned, skipped);
    }

    private User findBestAgent(String userPinCode) {
        String prefix = safePrefix(userPinCode);
        return userRepo.findByUserType(User.UserType.AGENT, Pageable.unpaged())
                .stream()
                .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                .filter(u -> safePrefix(u.getPinCode()).equals(prefix))
                .min(Comparator.comparingLong(u -> assignmentRepo.countActiveAssignmentsByUserId(u.getUserId())))
                .orElse(null);
    }

    private String safePrefix(String pinCode) {
        if (pinCode == null) {
            return "";
        }
        String trimmed = pinCode.trim();
        return trimmed.length() >= 3 ? trimmed.substring(0, 3) : trimmed;
    }
}
