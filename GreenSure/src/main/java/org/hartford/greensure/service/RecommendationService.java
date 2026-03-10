package org.hartford.greensure.service;


import org.hartford.greensure.dto.response.RecommendationResponse;
import org.hartford.greensure.entity.CarbonScore;
import org.hartford.greensure.entity.Recommendation;
import org.hartford.greensure.entity.User;
import org.hartford.greensure.repository.RecommendationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private RecommendationRepository recommendationRepo;

    // ── GENERATE 3 RECOMMENDATIONS ─────────────────────────────

    @Transactional
    public void generateRecommendations(CarbonScore score) {

        // Prevent duplicate generation
        if (recommendationRepo.existsByScoreScoreId(
                score.getScoreId())) {
            return;
        }

        boolean isHousehold = score.getUser().getUserType()
                == User.UserType.HOUSEHOLD;

        // Build category → CO2 map
        Map<Recommendation.RecommendationCategory, Double>
                categoryScores = new LinkedHashMap<>();

        categoryScores.put(
                Recommendation.RecommendationCategory.ENERGY,
                score.getEnergyCo2());

        categoryScores.put(
                Recommendation.RecommendationCategory.TRANSPORT,
                score.getTransportCo2());

        if (isHousehold && score.getLifestyleCo2() != null) {
            categoryScores.put(
                    Recommendation.RecommendationCategory.LIFESTYLE,
                    score.getLifestyleCo2());
        }

        if (!isHousehold && score.getOperationsCo2() != null) {
            categoryScores.put(
                    Recommendation.RecommendationCategory.OPERATIONS,
                    score.getOperationsCo2());
        }

        // Sort categories by CO2 descending
        // Top 3 highest contributing categories get recommendations
        List<Map.Entry<Recommendation.RecommendationCategory, Double>>
                sorted = categoryScores.entrySet().stream()
                .sorted(Map.Entry.<Recommendation.RecommendationCategory, Double>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        // Assign priorities — highest CO2 = HIGH
        Recommendation.RecommendationPriority[] priorities = {
                Recommendation.RecommendationPriority.HIGH,
                Recommendation.RecommendationPriority.MEDIUM,
                Recommendation.RecommendationPriority.LOW
        };

        for (int i = 0; i < sorted.size(); i++) {
            Recommendation.RecommendationCategory category =
                    sorted.get(i).getKey();

            String text = getRecommendationText(
                    category, isHousehold, score);

            Recommendation recommendation = Recommendation.builder()
                    .score(score)
                    .user(score.getUser())
                    .category(category)
                    .priority(priorities[i])
                    .recommendationText(text)
                    .build();

            recommendationRepo.save(recommendation);
        }
    }

    // ── GET MY RECOMMENDATIONS ─────────────────────────────────

    public List<RecommendationResponse> getMyRecommendations(
            Long userId) {
        return recommendationRepo
                .findByUserUserIdOrderByPriorityAsc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── RECOMMENDATION TEXT BANK ───────────────────────────────

    private String getRecommendationText(
            Recommendation.RecommendationCategory category,
            boolean isHousehold, CarbonScore score) {

        switch (category) {

            case ENERGY:
                if (isHousehold) {
                    return "Your energy consumption is your " +
                            "highest carbon contributor. Consider " +
                            "switching to LED bulbs, using 5-star " +
                            "rated appliances, and setting your AC " +
                            "to 24°C. Installing a rooftop solar " +
                            "panel can reduce your electricity bill " +
                            "and carbon footprint by up to 60%.";
                } else {
                    return "Energy consumption is your largest " +
                            "emission source. Conduct an energy " +
                            "audit of your facility, switch to LED " +
                            "lighting, and consider solar panels or " +
                            "wind energy procurement. Upgrading to " +
                            "energy-efficient machinery can reduce " +
                            "consumption by 20-40%.";
                }

            case TRANSPORT:
                if (isHousehold) {
                    return "Transport is a major part of your " +
                            "carbon footprint. Consider carpooling, " +
                            "using public transport for daily " +
                            "commutes, or switching your next " +
                            "vehicle to CNG or electric. Even " +
                            "combining trips can reduce your " +
                            "transport emissions by 30%.";
                } else {
                    return "Your fleet emissions are significant. " +
                            "Transition commercial vehicles to CNG " +
                            "or electric, optimize delivery routes, " +
                            "and incentivize employees to use public " +
                            "transport or carpool. A fleet " +
                            "electrification plan could reduce " +
                            "transport CO2 by up to 50%.";
                }

            case LIFESTYLE:
                return "Your dietary choices contribute " +
                        "significantly to your carbon score. " +
                        "Reducing meat consumption by even 2 days " +
                        "per week can lower your food-related " +
                        "emissions by 25%. Also consider reducing " +
                        "online shopping frequency and choosing " +
                        "sellers with eco-friendly packaging.";

            case OPERATIONS:
                return "Your operations category shows high " +
                        "emissions from industrial activities. " +
                        "Switching from coal boilers to natural gas " +
                        "can reduce boiler emissions by 40%. " +
                        "Transitioning to recycled raw materials " +
                        "and reducing paper consumption with " +
                        "digital workflows are immediate " +
                        "high-impact actions.";

            default:
                return "Review your consumption patterns and " +
                        "look for areas where efficiency " +
                        "improvements can reduce your carbon " +
                        "footprint.";
        }
    }

    // ── MAP TO RESPONSE ────────────────────────────────────────

    private RecommendationResponse mapToResponse(
            Recommendation r) {
        return RecommendationResponse.builder()
                .recommendationId(r.getRecommendationId())
                .category(r.getCategory())
                .priority(r.getPriority())
                .recommendationText(r.getRecommendationText())
                .generatedAt(r.getGeneratedAt())
                .build();
    }
}
