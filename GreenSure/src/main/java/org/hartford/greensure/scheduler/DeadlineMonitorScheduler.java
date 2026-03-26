package org.hartford.greensure.scheduler;

import org.hartford.greensure.entity.*;
import org.hartford.greensure.enums.AssignmentStatus;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Monitors agent deadlines for assignment completions.
 *
 * MISSED_DEADLINE check (runs hourly):
 *   - Finds assignments past their 72-hour deadline.
 *   - Applies 1 strike to the agent.
 *   - At 3+ strikes → suspends the agent (sets suspendedAt, isActive = false).
 *   - Marks the assignment as REASSIGNED (does NOT auto-reassign — admin must do so).
 *   - Sends email notification.
 *
 * 24-HOUR REMINDER check (runs hourly):
 *   - Warns agents whose deadline is within the next 24 hours.
 */
@Component
public class DeadlineMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeadlineMonitorScheduler.class);
    private static final int    SUSPENSION_STRIKE_THRESHOLD = 3;

    @Autowired private AgentAssignmentRepository assignmentRepo;
    @Autowired private UserRepository            userRepository;
    @Autowired private CarbonDeclarationRepository declarationRepo;
    @Autowired private EmailService              emailService;

    // ── Missed deadline check — every hour ─────────────────────

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void checkMissedDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        List<AgentAssignment> missed = assignmentRepo.findMissedDeadlines(now);

        if (missed.isEmpty()) return;

        log.info("[DeadlineMonitor] Processing {} missed deadlines", missed.size());

        for (AgentAssignment assignment : missed) {
            try {
                handleMissedDeadline(assignment, now);
            } catch (Exception ex) {
                log.error("[DeadlineMonitor] Error processing assignment {}: {}",
                        assignment.getAssignmentId(), ex.getMessage());
            }
        }
    }

    private void handleMissedDeadline(AgentAssignment assignment, LocalDateTime now) {
        // Guard: only process each assignment once
        if (assignment.getStrikeApplied() != null && assignment.getStrikeApplied() > 0) return;

        User agent = assignment.getAgent();
        agent.setStrikes(agent.getStrikes() + 1);
        assignment.setStrikeApplied(1);

        boolean suspended = false;
        if (agent.getStrikes() >= SUSPENSION_STRIKE_THRESHOLD) {
            agent.setSuspendedAt(now);
            agent.setActive(false);
            suspended = true;
            log.warn("[DeadlineMonitor] Agent {} suspended after {} strikes",
                    agent.getUserId(), agent.getStrikes());
        }

        userRepository.save(agent);
        assignment.setStatus(AssignmentStatus.REASSIGNED);
        assignmentRepo.save(assignment);

        // Revert declaration to SUBMITTED so AssignmentEngine picks it up again
        CarbonDeclaration declaration = assignment.getDeclaration();
        declaration.setStatus(org.hartford.greensure.enums.DeclarationStatus.SUBMITTED);
        declarationRepo.save(declaration);

        // Email agent
        String subject = suspended
                ? "GreenSure Account Suspended"
                : "Missed Assignment Deadline — Strike " + agent.getStrikes();
        String body = suspended
                ? "<p>Your GreenSure agent account has been suspended due to "
                  + agent.getStrikes() + " missed assignment deadlines. "
                  + "Please contact your administrator.</p>"
                : "<p>You missed the deadline for assignment "
                  + assignment.getAssignmentId() + ". "
                  + "Strike " + agent.getStrikes() + " of 3 has been applied to your account.</p>"
                  + "<p>Please complete your remaining assignments on time.</p>";

        try {
            emailService.sendEmail(agent.getEmail(), subject, body);
        } catch (Exception ex) {
            log.warn("[DeadlineMonitor] Email failed for agent {}: {}", agent.getUserId(), ex.getMessage());
        }
    }

    // ── 24-hour reminder — every hour ──────────────────────────

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    @Transactional
    public void sendDeadlineReminders() {
        LocalDateTime now   = LocalDateTime.now();
        LocalDateTime start = now;
        LocalDateTime end   = now.plusHours(24);

        List<AgentAssignment> approaching = assignmentRepo.findDeadlineApproaching(start, end);

        for (AgentAssignment assignment : approaching) {
            try {
                User agent = assignment.getAgent();
                emailService.sendEmail(
                        agent.getEmail(),
                        "Deadline Reminder — Assignment Due in 24 Hours",
                        "<p>Hi " + agent.getFullName() + ",</p>"
                                + "<p>Your assignment for Declaration ID "
                                + assignment.getDeclaration().getDeclarationId()
                                + " is due by <strong>"
                                + assignment.getDeadline()
                                + "</strong>.</p><p>Please complete it on time.</p>");
            } catch (Exception ex) {
                log.warn("[DeadlineMonitor] Reminder email failed: {}", ex.getMessage());
            }
        }

        if (!approaching.isEmpty()) {
            log.info("[DeadlineMonitor] Sent {} deadline reminders", approaching.size());
        }
    }
}
