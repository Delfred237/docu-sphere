package com.docusphere.notification.service;

import com.docusphere.auth.domain.User;
import com.docusphere.auth.repository.UserRepository;
import com.docusphere.common.exception.ResourceNotFoundException;
import com.docusphere.notification.domain.Notification;
import com.docusphere.notification.dto.NotificationResponse;
import com.docusphere.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createNotification(Long recipientId, com.docusphere.notification.domain.NotificationType type,
                                           String title, String message, String actionUrl) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", recipientId));

        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setActionUrl(actionUrl);
        notification.setRecipient(recipient);

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
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
}