package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.security.JwtUtil;
import org.hartford.greensure.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    @Autowired private RecommendationService recommendationService;
    @Autowired private JwtUtil jwtUtil;

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>>
            getMyRecommendations(HttpServletRequest request) {

        Long userId = extractUserId(request);
        List<RecommendationResponse> recommendations =
                recommendationService.getMyRecommendations(userId);
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

    private Long extractUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.extractId(token);
    }
}
