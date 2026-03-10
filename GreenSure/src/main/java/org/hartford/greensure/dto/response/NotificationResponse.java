package org.hartford.greensure.dto.response;

import org.hartford.greensure.entity.Notification.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long notificationId;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private String message;
    private NotificationStatus status;
    private LocalDateTime sentAt;
}
