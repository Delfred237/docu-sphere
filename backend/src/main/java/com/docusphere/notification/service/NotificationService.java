package com.docusphere.notification.service;

import com.docusphere.auth.domain.User;
import com.docusphere.auth.repository.UserRepository;
import com.docusphere.common.exception.ResourceNotFoundException;
import com.docusphere.notification.domain.Notification;
import com.docusphere.notification.domain.NotificationType;
import com.docusphere.notification.dto.NotificationResponse;
import com.docusphere.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(Long recipientId, NotificationType type,
                                   String title, String message, String actionUrl) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", recipientId));

        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setActionUrl(actionUrl);
        notification.setRecipient(recipient);

        Notification saved = notificationRepository.save(notification);
        log.info("✅ Notification saved with id: {}", saved.getId());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(User user, boolean unreadOnly, Pageable pageable) {
        Page<Notification> notifications;

        if (unreadOnly) {
            notifications = notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(user, pageable);
        } else {
            notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable);
        }

        return notifications.map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    @Transactional
    public void markAsRead(String notificationPublicId, User user) {
        Notification notification = notificationRepository.findByPublicIdAndRecipient(notificationPublicId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "publicId", notificationPublicId));
        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadByRecipient(user);
    }

    public void deleteNotification(String publicId, User user) {
        Notification notification = notificationRepository.findByPublicIdAndRecipient(publicId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "publicId", publicId));
        notificationRepository.delete(notification);
    }
}