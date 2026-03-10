package org.hartford.greensure.repository;


import org.hartford.greensure.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // Used by NOTIFICATION INBOX — get all notifications
    // for a specific recipient (user or agent)
    // ordered by latest first
    List<Notification> findByRecipientTypeAndRecipientIdOrderBySentAtDesc(
            Notification.RecipientType recipientType, Long recipientId);

    // Used by NOTIFICATION INBOX — get notifications
    // for a recipient filtered by notification type
    // e.g. show only SCORE_READY notifications
    List<Notification> findByRecipientTypeAndRecipientIdAndNotificationType(
            Notification.RecipientType recipientType,
            Long recipientId,
            Notification.NotificationType notificationType);

    // Used by UNREAD COUNT BADGE — count unread notifications
    // for a recipient to show on dashboard header
    // A notification is "unread" when status is SENT not DELIVERED
    long countByRecipientTypeAndRecipientIdAndStatus(
            Notification.RecipientType recipientType,
            Long recipientId,
            Notification.NotificationStatus status);

    // Used by NOTIFICATION SERVICE — check if a specific
    // notification type was already sent to a recipient
    // Prevents sending duplicate notifications
    // e.g. renewal reminder should only be sent once
    boolean existsByRecipientTypeAndRecipientIdAndNotificationType(
            Notification.RecipientType recipientType,
            Long recipientId,
            Notification.NotificationType notificationType);

    // Used by DEADLINE MONITOR — check if a reminder
    // was already sent for a specific assignment
    // so the monitor does not send duplicate reminders
    @Query("SELECT COUNT(n) > 0 FROM Notification n " +
            "WHERE n.recipientType = 'AGENT' " +
            "AND n.recipientId = :agentId " +
            "AND n.notificationType = 'DEADLINE_REMINDER' " +
            "AND n.sentAt >= :cutoffTime")
    boolean existsRecentDeadlineReminder(
            @Param("agentId") Long agentId,
            @Param("cutoffTime") LocalDateTime cutoffTime);

    // Used by ADMIN — find all notifications that failed
    // to deliver so admin can investigate and retry
    List<Notification> findByStatusOrderBySentAtDesc(
            Notification.NotificationStatus status);

    // Used by ADMIN REPORTS — count notifications by
    // channel to see EMAIL vs SMS delivery rates
    long countByChannel(Notification.NotificationChannel channel);

    // Used by ADMIN REPORTS — count notifications by
    // status to see delivery success rate
    long countByStatus(Notification.NotificationStatus status);

    // Used by ADMIN REPORTS — count notifications by
    // type to see which notification fires most often
    long countByNotificationType(Notification.NotificationType notificationType);

    // Used by ADMIN REPORTS — get delivery failure rate
    // grouped by channel
    // Shows if EMAIL or SMS has more failures
    @Query("SELECT n.channel, " +
            "COUNT(CASE WHEN n.status = 'FAILED' THEN 1 END) " +
            "* 100.0 / COUNT(n) " +
            "FROM Notification n " +
            "GROUP BY n.channel")
    List<Object[]> calculateFailureRateByChannel();

    // Used by ADMIN — get all notifications sent to
    // a specific user with full history
    @Query("SELECT n FROM Notification n " +
            "WHERE n.recipientType = 'USER' " +
            "AND n.recipientId = :userId " +
            "ORDER BY n.sentAt DESC")
    List<Notification> findAllNotificationsForUser(
            @Param("userId") Long userId);

    // Used by ADMIN — get all notifications sent to
    // a specific agent with full history
    @Query("SELECT n FROM Notification n " +
            "WHERE n.recipientType = 'AGENT' " +
            "AND n.recipientId = :agentId " +
            "ORDER BY n.sentAt DESC")
    List<Notification> findAllNotificationsForAgent(
            @Param("agentId") Long agentId);

    // Used by RENEWAL ENGINE — find users who already
    // received a renewal reminder this year
    // so the engine does not send duplicate reminders
    @Query("SELECT n.recipientId FROM Notification n " +
            "WHERE n.recipientType = 'USER' " +
            "AND n.notificationType = 'RENEWAL_REMINDER' " +
            "AND n.sentAt >= :yearStart")
    List<Long> findUserIdsAlreadyRemindedThisYear(
            @Param("yearStart") LocalDateTime yearStart);

    // Used by ADMIN REPORTS — get notification volume
    // by day for the last 30 days
    // Used to plot notification activity graph
    @Query("SELECT DATE(n.sentAt), COUNT(n) " +
            "FROM Notification n " +
            "WHERE n.sentAt >= :fromDate " +
            "GROUP BY DATE(n.sentAt) " +
            "ORDER BY DATE(n.sentAt) ASC")
    List<Object[]> findDailyNotificationVolume(
            @Param("fromDate") LocalDateTime fromDate);
}
