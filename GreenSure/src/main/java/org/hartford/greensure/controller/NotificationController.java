package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.Notification;
import org.hartford.greensure.security.SecurityUser;
import org.hartford.greensure.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
public class NotificationController {

    @Autowired private NotificationService notificationService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
                @AuthenticationPrincipal SecurityUser user) {

        boolean isAgentOrAdmin = user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_AGENT")) ||
                                 user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        Notification.RecipientType type = isAgentOrAdmin
                ? Notification.RecipientType.AGENT
                : Notification.RecipientType.USER;

        List<NotificationResponse> notifications =
                notificationService.getMyNotifications(type, user.getId());
        return ResponseEntity.ok(
            ApiResponse.success("Notifications fetched", notifications));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {

        notificationService.markAsRead(id);
        return ResponseEntity.ok(
            ApiResponse.success("Notification marked as read"));
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal SecurityUser user) {
        boolean isAgentOrAdmin = user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_AGENT")) ||
                                 user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        Notification.RecipientType type = isAgentOrAdmin
                ? Notification.RecipientType.AGENT
                : Notification.RecipientType.USER;

        notificationService.markAllAsRead(type, user.getId());
        return ResponseEntity.ok(
            ApiResponse.success("All notifications marked as read"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal SecurityUser user) {

        boolean isAgentOrAdmin = user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_AGENT")) ||
                                 user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        Notification.RecipientType type = isAgentOrAdmin
                ? Notification.RecipientType.AGENT
                : Notification.RecipientType.USER;

        long count = notificationService.getUnreadCount(type, user.getId());
        return ResponseEntity.ok(
            ApiResponse.success("Unread count fetched", count));
    }
}
