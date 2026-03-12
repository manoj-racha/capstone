package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.security.SecurityUser;
import org.hartford.greensure.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    @Autowired private RecommendationService recommendationService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>>
            getMyRecommendations(@AuthenticationPrincipal SecurityUser user) {

        List<RecommendationResponse> recommendations =
                recommendationService.getMyRecommendations(user.getId());
        return ResponseEntity.ok(
            ApiResponse.success("Recommendations fetched", recommendations));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>>
            getRecommendationsByUserId(@PathVariable Long userId) {

        List<RecommendationResponse> recommendations =
                recommendationService.getMyRecommendations(userId);
        return ResponseEntity.ok(
            ApiResponse.success("Recommendations fetched", recommendations));
    }
}
