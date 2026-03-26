package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.ApiResponse;
import org.hartford.greensure.dto.response.CarbonScoreResponse;
import org.hartford.greensure.engine.CarbonScoreService;
import org.hartford.greensure.repository.CarbonScoreRepository;
import org.hartford.greensure.security.SecurityUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return scoreRepository.findTopByUserUserIdOrderByScoreYearDesc(user.getId())
                .map(score -> ResponseEntity.ok(ApiResponse.success("Latest score fetched", carbonScoreService.toResponse(score))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("No score yet", null)));
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<CarbonScoreResponse>>> getMyHistory(@AuthenticationPrincipal SecurityUser user) {
        List<CarbonScoreResponse> history = scoreRepository.findByUserUserIdOrderByScoreYearDesc(user.getId())
                .stream()
                .map(carbonScoreService::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Score history fetched", history));
    }
}
