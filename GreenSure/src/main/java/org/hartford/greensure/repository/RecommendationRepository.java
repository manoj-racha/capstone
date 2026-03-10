package org.hartford.greensure.repository;


import org.hartford.greensure.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository
        extends JpaRepository<Recommendation, Long> {

    // Used by USER DASHBOARD — get all recommendations
    // for a user's latest score
    // Ordered by priority — HIGH first, then MEDIUM, then LOW
    List<Recommendation> findByUserUserIdOrderByPriorityAsc(
            Long userId);

    // Used by USER DASHBOARD — get recommendations
    // for a specific score only
    // Used when user views a past year's recommendations
    List<Recommendation> findByScoreScoreId(Long scoreId);

    // Used by RECOMMENDATION ENGINE — check if recommendations
    // already exist for a score before generating new ones
    // Prevents duplicate recommendations for same score
    boolean existsByScoreScoreId(Long scoreId);

    // Used by RECOMMENDATION ENGINE — delete old recommendations
    // for a score before regenerating updated ones
    void deleteByScoreScoreId(Long scoreId);

    // Used by USER DASHBOARD — get recommendations for a user
    // filtered by category
    // e.g. show only ENERGY recommendations
    List<Recommendation> findByUserUserIdAndCategory(
            Long userId, Recommendation.RecommendationCategory category);

    // Used by USER DASHBOARD — get recommendations for a user
    // filtered by priority
    // e.g. show only HIGH priority recommendations
    List<Recommendation> findByUserUserIdAndPriority(
            Long userId, Recommendation.RecommendationCategory priority);

    // Used by ADMIN REPORTS — count how many recommendations
    // of each category have been generated across platform
    // Shows which category is the biggest problem overall
    long countByCategory(Recommendation.RecommendationCategory category);

    // Used by ADMIN REPORTS — count recommendations by
    // priority across platform
    long countByPriority(Recommendation.RecommendationCategory priority);

    // Used by ADMIN REPORTS — find the most common category
    // appearing as HIGH priority across all users
    // Tells admin which area needs most platform-wide attention
    @Query("SELECT r.category, COUNT(r) " +
            "FROM Recommendation r " +
            "WHERE r.priority = 'HIGH' " +
            "GROUP BY r.category " +
            "ORDER BY COUNT(r) DESC")
    List<Object[]> findMostCommonHighPriorityCategories();

    // Used by ADMIN REPORTS — category breakdown
    // for a specific score year
    // Shows platform-wide emission problem areas by year
    @Query("SELECT r.category, COUNT(r) " +
            "FROM Recommendation r " +
            "JOIN r.score s " +
            "WHERE s.scoreYear = :year " +
            "GROUP BY r.category " +
            "ORDER BY COUNT(r) DESC")
    List<Object[]> findCategoryBreakdownByYear(
            @Param("year") Integer year);

    // Used by ADMIN REPORTS — get all HIGH priority
    // recommendations for GREEN_DEFAULTER users
    // These users need the most urgent attention
    @Query("SELECT r FROM Recommendation r " +
            "JOIN r.score s " +
            "WHERE s.zone = 'GREEN_DEFAULTER' " +
            "AND r.priority = 'HIGH' " +
            "ORDER BY r.generatedAt DESC")
    List<Recommendation> findHighPriorityRecommendationsForDefaulters();
}
