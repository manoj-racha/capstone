package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.ApiResponse;
import org.hartford.greensure.dto.response.CarbonScoreResponse;
import org.hartford.greensure.engine.CarbonScoreService;
import org.hartford.greensure.repository.CarbonScoreRepository;
import org.hartford.greensure.security.SecurityUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/score")
@PreAuthorize("hasRole('USER')")
public class CarbonScoreController {

    @Autowired
    private CarbonScoreRepository scoreRepository;
    
    @Autowired
    private CarbonScoreService carbonScoreService;

    @GetMapping("/my-score")
    public ResponseEntity<ApiResponse<CarbonScoreResponse>> getMyScore(@AuthenticationPrincipal SecurityUser user) {
        try {
            CarbonScoreResponse score = carbonScoreService.getMyScore(user.getId());
            return ResponseEntity.ok(ApiResponse.success("Latest score fetched", score));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success("No score yet", null));
        }
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<CarbonScoreResponse>>> getMyHistory(@AuthenticationPrincipal SecurityUser user) {
        List<CarbonScoreResponse> history = carbonScoreService.getScoreHistory(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Score history fetched", history));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    public ResponseEntity<ApiResponse<CarbonScoreResponse>> getScoreByUserId(@PathVariable Long userId) {
        try {
            CarbonScoreResponse score = carbonScoreService.getMyScore(userId);
            return ResponseEntity.ok(ApiResponse.success("Score fetched for user", score));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("No score found for this user"));
        }
    }

    @PostMapping("/generate/{declarationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    public ResponseEntity<ApiResponse<Void>> generateScore(@PathVariable Long declarationId) {
        carbonScoreService.generateScore(declarationId);
        return ResponseEntity.ok(ApiResponse.success("Score generation triggered", null));
    }
}
