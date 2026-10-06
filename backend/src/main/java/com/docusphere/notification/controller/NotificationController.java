package com.docusphere.notification.controller;

import com.docusphere.auth.domain.User;
import com.docusphere.notification.dto.NotificationResponse;
import com.docusphere.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUserNotifications(user, unreadOnly, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getUnreadCount(user));
    }

    @PostMapping("/{publicId}/read")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal User user,
                                           @PathVariable String publicId) {
        notificationService.markAsRead(publicId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId) {
        notificationService.deleteNotification(publicId, user);
        return ResponseEntity.noContent().build();
    }
}