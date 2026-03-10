package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.Notification;
import org.hartford.greensure.security.JwtUtil;
import org.hartford.greensure.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
public class NotificationController {

    @Autowired private NotificationService notificationService;
    @Autowired private JwtUtil jwtUtil;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
                HttpServletRequest request) {

        String token = request.getHeader("Authorization").substring(7);
        Long id = jwtUtil.extractId(token);
        String role = jwtUtil.extractRole(token);

        Notification.RecipientType type =
                role.equals("AGENT") || role.equals("ADMIN")
                ? Notification.RecipientType.AGENT
                : Notification.RecipientType.USER;

        List<NotificationResponse> notifications =
                notificationService.getMyNotifications(type, id);
        return ResponseEntity.ok(
            ApiResponse.success("Notifications fetched", notifications));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {

        notificationService.markAsRead(id);
        return ResponseEntity.ok(
            ApiResponse.success("Notification marked as read"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            HttpServletRequest request) {

        String token = request.getHeader("Authorization").substring(7);
        Long id = jwtUtil.extractId(token);
        String role = jwtUtil.extractRole(token);

        Notification.RecipientType type =
                role.equals("AGENT") || role.equals("ADMIN")
                ? Notification.RecipientType.AGENT
                : Notification.RecipientType.USER;

        long count = notificationService.getUnreadCount(type, id);
        return ResponseEntity.ok(
            ApiResponse.success("Unread count fetched", count));
    }
}
