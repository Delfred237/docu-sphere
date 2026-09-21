package com.docusphere.notification.event;

import com.docusphere.notification.domain.NotificationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEvent extends ApplicationEvent {

    private final NotificationType type;
    private final Long recipientId;
    private final String recipientEmail;
    private final String title;
    private final String message;
    private final String actionUrl;
    private final boolean sendEmail;

    public NotificationEvent(Object source, NotificationType type, Long recipientId,
                             String recipientEmail, String title, String message,
                             String actionUrl, boolean sendEmail) {
        super(source);
        this.type = type;
        this.recipientId = recipientId;
        this.recipientEmail = recipientEmail;
        this.title = title;
        this.message = message;
        this.actionUrl = actionUrl;
        this.sendEmail = sendEmail;
    }
}