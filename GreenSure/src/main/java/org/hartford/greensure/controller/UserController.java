package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.security.JwtUtil;
import org.hartford.greensure.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@PreAuthorize("hasRole('USER')")
public class UserController {

    @Autowired private UserService userService;
    @Autowired private JwtUtil jwtUtil;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>>
            getProfile(HttpServletRequest request) {

        Long userId = extractUserId(request);
        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(
            ApiResponse.success("Profile fetched", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>>
            updateProfile(
                HttpServletRequest request,
                @RequestBody UserProfileResponse updateRequest) {

        Long userId = extractUserId(request);
        UserProfileResponse updated = userService.updateProfile(userId, updateRequest);
        return ResponseEntity.ok(
            ApiResponse.success("Profile updated", updated));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>>
            getDashboard(HttpServletRequest request) {

        Long userId = extractUserId(request);
        DashboardResponse dashboard = userService.getDashboard(userId);
        return ResponseEntity.ok(
            ApiResponse.success("Dashboard fetched", dashboard));
    }

    private Long extractUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.extractId(token);
    }
}
