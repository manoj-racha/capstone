package org.hartford.greensure.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false)
    private RecipientType recipientType;

    // Stores user_id or agent_id depending on recipient_type
    // Not a FK because recipient can be either User or Agent
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.SENT;

    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    // ── NOTE ON MAPPINGS ───────────────────────────────────────
    // Notification does NOT have a JPA mapping to User or Agent
    // because recipient_id can reference either users or agents table.
    // Recipient resolution is handled in NotificationService
    // by checking recipient_type first.

    // ── ENUMS ──────────────────────────────────────────────────

    public enum RecipientType {
        USER, AGENT
    }

    public enum NotificationType {
        ASSIGNMENT_ALERT,
        VERIFICATION_COMPLETE,
        SCORE_READY,
        RENEWAL_REMINDER,
        DEADLINE_REMINDER,
        REJECTION_ALERT,
        REASSIGNMENT_ALERT
    }

    public enum NotificationChannel {
        EMAIL, SMS
    }

    public enum NotificationStatus {
        SENT, DELIVERED, FAILED
    }

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }
}
