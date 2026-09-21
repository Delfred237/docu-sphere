package com.docusphere.notification.listener;

import com.docusphere.common.email.EmailService;
import com.docusphere.notification.event.NotificationEvent;
import com.docusphere.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            // 1. Créer la notification in-app
            notificationService.createNotification(
                    event.getRecipientId(),
                    event.getType(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getActionUrl()
            );

            // 2. Envoyer par email si demandé
            if (event.isSendEmail()) {
                emailService.sendNotificationEmail(
                        event.getRecipientEmail(),
                        event.getTitle(),
                        event.getMessage(),
                        event.getActionUrl()
                );
            }

            log.debug("Notification processed: {} for user {}", event.getType(), event.getRecipientId());
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", event.getType(), e);
        }
    }
}