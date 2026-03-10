package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.security.JwtUtil;
import org.hartford.greensure.engine.CarbonScoreService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/score")
public class CarbonScoreController {

    @Autowired private CarbonScoreService carbonScoreService;
    @Autowired private JwtUtil jwtUtil;

    @GetMapping("/my-score")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CarbonScoreResponse>>
            getMyScore(HttpServletRequest request) {

        Long userId = extractUserId(request);
        CarbonScoreResponse score = carbonScoreService.getMyScore(userId);
        return ResponseEntity.ok(
            ApiResponse.success("Score fetched", score));
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<CarbonScoreResponse>>>
            getMyHistory(HttpServletRequest request) {

        Long userId = extractUserId(request);
        List<CarbonScoreResponse> history = carbonScoreService.getScoreHistory(userId);
        return ResponseEntity.ok(
            ApiResponse.success("Score history fetched", history));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CarbonScoreResponse>>
            getScoreByUserId(@PathVariable Long userId) {

        CarbonScoreResponse score = carbonScoreService.getMyScore(userId);
        return ResponseEntity.ok(
            ApiResponse.success("Score fetched", score));
    }

    @PostMapping("/generate/{declarationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> generateScore(
            @PathVariable Long declarationId) {

        carbonScoreService.generateScore(declarationId);
        return ResponseEntity.ok(
            ApiResponse.success("Score generation triggered"));
    }

    private Long extractUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.extractId(token);
    }
}
