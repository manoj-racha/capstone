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
        // Clear existing recommendations for this user if any (simplified: just clear for this score's direct relation)
        // recommendationRepo.deleteByScoreScoreId(score.getScoreId());

        if (score.getElectricityCo2() > 200) {
            saveRec(score, Recommendation.RecommendationCategory.ENERGY, Recommendation.RecommendationPriority.HIGH, "Consider switching to LED bulbs and energy-efficient appliances to reduce your electricity footprint.");
        }
        if (score.getSolarOffset() == 0) {
            saveRec(score, Recommendation.RecommendationCategory.ENERGY, Recommendation.RecommendationPriority.MEDIUM, "Installing a solar water heater or rooftop panels could significantly offset your carbon emissions.");
        }
        if (score.getVehicleCo2() > 300) {
            saveRec(score, Recommendation.RecommendationCategory.TRANSPORT, Recommendation.RecommendationPriority.HIGH, "Your vehicle emissions are high. Using public transport once a week could reduce this by up to 20%.");
        }
        if (score.getCookingCo2() > 100) {
            saveRec(score, Recommendation.RecommendationCategory.ENERGY, Recommendation.RecommendationPriority.MEDIUM, "Ensure your gas burners are clean for better efficiency or consider an induction cooktop.");
        }
    }

    private void saveRec(CarbonScore score,
                         Recommendation.RecommendationCategory category,
                         Recommendation.RecommendationPriority priority,
                         String text) {
        Recommendation r = Recommendation.builder()
                .user(score.getUser())
                .score(score)
                .category(category)
                .priority(priority)
                .recommendationText(text)
                .build();
        recommendationRepo.save(r);
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
