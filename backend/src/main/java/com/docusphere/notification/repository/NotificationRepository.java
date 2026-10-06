package com.docusphere.notification.repository;

import com.docusphere.notification.domain.Notification;
import com.docusphere.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    // Nouvelles méthodes paginées
    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    Page<Notification> findByRecipientAndReadFalseOrderByCreatedAtDesc(User recipient, Pageable pageable);


    long countByRecipientAndReadFalse(User recipient);

    Optional<Notification> findByPublicIdAndRecipient(String publicId, User recipient);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient = :recipient AND n.read = false")
    int markAllAsReadByRecipient(User recipient);
}