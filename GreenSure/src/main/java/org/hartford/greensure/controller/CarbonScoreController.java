package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.security.SecurityUser;
import org.hartford.greensure.engine.CarbonScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/score")
public class CarbonScoreController {

    @Autowired private CarbonScoreService carbonScoreService;

    @GetMapping("/my-score")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CarbonScoreResponse>>
            getMyScore(@AuthenticationPrincipal SecurityUser user) {

        CarbonScoreResponse score = carbonScoreService.getMyScore(user.getId());
        return ResponseEntity.ok(
            ApiResponse.success("Score fetched", score));
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<CarbonScoreResponse>>>
            getMyHistory(@AuthenticationPrincipal SecurityUser user) {

        List<CarbonScoreResponse> history = carbonScoreService.getScoreHistory(user.getId());
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
}
