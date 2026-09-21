package com.docusphere.notification.dto;

import com.docusphere.notification.domain.Notification;
import com.docusphere.notification.domain.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        String publicId,
        NotificationType type,
        String title,
        String message,
        String actionUrl,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
                notification.getPublicId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActionUrl(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}