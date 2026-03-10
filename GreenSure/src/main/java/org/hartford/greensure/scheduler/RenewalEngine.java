package org.hartford.greensure.scheduler;



import org.hartford.greensure.entity.*;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class RenewalEngine {

    @Autowired
    private CarbonScoreRepository scoreRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private NotificationService notificationService;

    // Runs at 9:00 AM on the 1st of every month
    @Scheduled(cron = "0 0 9 1 * *")
    @Transactional
    public void sendRenewalReminders() {

        System.out.println(
                "[RenewalEngine] Running renewal reminder check...");

        int currentYear = LocalDate.now().getYear();
        int lastYear = currentYear - 1;

        // Find users who need renewal reminder:
        // Had a score last year AND no score this year yet
        List<Object> usersNeedingRenewal = scoreRepo
                .findUsersNeedingRenewal(lastYear, currentYear);

        // Find users already reminded this year
        // so we don't send duplicate reminders
        LocalDateTime yearStart = LocalDate.now()
                .withDayOfYear(1)
                .atStartOfDay();

        List<Long> alreadyReminded = notificationRepo
                .findUserIdsAlreadyRemindedThisYear(yearStart);

        int sent = 0;

        for (Object userObj : usersNeedingRenewal) {
            User user = (User) userObj;
            Long userId = user.getUserId();

            // Skip if already reminded this year
            if (alreadyReminded.contains(userId)) {
                continue;
            }

            notificationService.sendToUser(
                    userId,
                    Notification.NotificationType.RENEWAL_REMINDER,
                    "It's time to renew your GreenTrace " +
                            "Carbon Declaration for " + currentYear + ". " +
                            "Your last score was from " + lastYear + ". " +
                            "Login now to start your renewal declaration " +
                            "and track your improvement this year."
            );

            sent++;
            System.out.println(
                    "[RenewalEngine] Sent renewal reminder to: " +
                            user.getFullName() +
                            " (" + user.getEmail() + ")");
        }

        System.out.println(
                "[RenewalEngine] Done. Sent " + sent +
                        " renewal reminders.");
    }
}
