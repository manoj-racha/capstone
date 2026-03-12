package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.security.SecurityUser;
import org.hartford.greensure.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@PreAuthorize("hasRole('USER')")
public class UserController {

    @Autowired private UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>>
            getProfile(@AuthenticationPrincipal SecurityUser user) {

        UserProfileResponse profile = userService.getProfile(user.getId());
        return ResponseEntity.ok(
            ApiResponse.success("Profile fetched", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>>
            updateProfile(
                @AuthenticationPrincipal SecurityUser user,
                @RequestBody UserProfileResponse updateRequest) {

        UserProfileResponse updated = userService.updateProfile(user.getId(), updateRequest);
        return ResponseEntity.ok(
            ApiResponse.success("Profile updated", updated));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>>
            getDashboard(@AuthenticationPrincipal SecurityUser user) {

        DashboardResponse dashboard = userService.getDashboard(user.getId());
        return ResponseEntity.ok(
            ApiResponse.success("Dashboard fetched", dashboard));
    }
}
