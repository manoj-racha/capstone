package org.hartford.greensure.service;

import org.hartford.greensure.dto.response.NotificationResponse;
import org.hartford.greensure.entity.Notification;
import org.hartford.greensure.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired private NotificationRepository notificationRepo;

    public void sendToUser(Long userId, Notification.NotificationType type, String message) {
        save(Notification.RecipientType.USER, userId, type, message);
    }

    public void sendToAgent(Long agentId, Notification.NotificationType type, String message) {
        save(Notification.RecipientType.AGENT, agentId, type, message);
    }

    public List<NotificationResponse> getMyNotifications(Notification.RecipientType type, Long recipientId) {
        return notificationRepo.findByRecipientTypeAndRecipientIdOrderBySentAtDesc(type, recipientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void markAsRead(Long notificationId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            n.setStatus(Notification.NotificationStatus.DELIVERED);
            notificationRepo.save(n);
        });
    }

    public void markAllAsRead(Notification.RecipientType type, Long recipientId) {
        List<Notification> unread = notificationRepo.findByRecipientTypeAndRecipientIdAndStatus(
                type, recipientId, Notification.NotificationStatus.SENT);
        unread.forEach(n -> n.setStatus(Notification.NotificationStatus.DELIVERED));
        notificationRepo.saveAll(unread);
    }

    public long getUnreadCount(Notification.RecipientType type, Long recipientId) {
        return notificationRepo.countByRecipientTypeAndRecipientIdAndStatus(
                type, recipientId, Notification.NotificationStatus.SENT);
    }

    private void save(Notification.RecipientType recipientType, Long recipientId,
                      Notification.NotificationType type, String message) {
        Notification notification = Notification.builder()
                .recipientType(recipientType)
                .recipientId(recipientId)
                .notificationType(type)
                .channel(Notification.NotificationChannel.EMAIL)
                .message(message)
                .status(Notification.NotificationStatus.SENT)
                .build();

        notificationRepo.save(notification);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .notificationType(n.getNotificationType())
                .channel(n.getChannel())
                .message(n.getMessage())
                .status(n.getStatus())
                .sentAt(n.getSentAt())
                .build();
    }
}
