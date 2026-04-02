package org.hartford.greensure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Legacy scheduler retained for compatibility.
 * Active reassignment/reminder flow is handled by DeadlineMonitor.
 */
@Component
public class DeadlineMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeadlineMonitorScheduler.class);

    @Scheduled(fixedDelay = 3_600_000)
    public void checkMissedDeadlines() {
        log.debug("Legacy DeadlineMonitorScheduler.checkMissedDeadlines skipped");
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    public void sendDeadlineReminders() {
        log.debug("Legacy DeadlineMonitorScheduler.sendDeadlineReminders skipped");
    }
}
