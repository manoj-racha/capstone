package org.hartford.greensure.service;

import org.hartford.greensure.dto.response.RecommendationResponse;
import org.hartford.greensure.entity.CarbonScore;
import org.hartford.greensure.entity.Recommendation;
import org.hartford.greensure.repository.RecommendationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private RecommendationRepository recommendationRepo;

    @Transactional
    public void generateRecommendations(CarbonScore score) {
        // Recommendations are now generated directly inside CarbonScoreService
    }

    public List<RecommendationResponse> getMyRecommendations(Long userId) {
        return recommendationRepo
                .findByUserUserIdOrderByPriorityAsc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private RecommendationResponse mapToResponse(Recommendation r) {
        return RecommendationResponse.builder()
                .recommendationId(r.getRecommendationId())
                .category(r.getCategory())
                .priority(r.getPriority())
                .recommendationText(r.getRecommendationText())
                .generatedAt(r.getGeneratedAt())
                .build();
    }
}
