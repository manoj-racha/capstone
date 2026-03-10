package org.hartford.greensure.repository;

import org.hartford.greensure.entity.CarbonScore;
import org.hartford.greensure.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarbonScoreRepository
        extends JpaRepository<CarbonScore, Long> {

    // Used by USER DASHBOARD — get the most recent score
    // for a user to display on their dashboard
    Optional<CarbonScore> findTopByUserUserIdOrderByScoreYearDesc(
            Long userId);

    // Used by USER DASHBOARD — get score for a specific year
    // Used when user views a past year's score
    Optional<CarbonScore> findByUserUserIdAndScoreYear(
            Long userId, Integer year);

    // Used by SCORE HISTORY — get all scores for a user
    // ordered by year descending for year-on-year comparison
    List<CarbonScore> findByUserUserIdOrderByScoreYearDesc(
            Long userId);

    // Used by CALCULATION ENGINE — check if a score already
    // exists for this declaration before generating a new one
    boolean existsByDeclarationDeclarationId(Long declarationId);

    // Used by ADMIN — get score by declaration ID
    Optional<CarbonScore> findByDeclarationDeclarationId(
            Long declarationId);

    // Used by ADMIN REPORTS — get all scores for a specific
    // zone classification across the platform
    List<CarbonScore> findByZone(CarbonScore.CarbonZone zone);

    // Used by ADMIN REPORTS — count users in each zone
    long countByZone(CarbonScore.CarbonZone zone);

    // Used by ADMIN REPORTS — count scores for a specific year
    long countByScoreYear(Integer year);

    // Used by ADMIN REPORTS — calculate platform average
    // per capita CO2 for a given year
    // Used as the benchmark for zone classification
    @Query("SELECT AVG(s.perCapitaCo2) FROM CarbonScore s " +
            "WHERE s.scoreYear = :year")
    Double calculateAveragePerCapitaCo2ByYear(
            @Param("year") Integer year);

    // Used by ADMIN REPORTS — calculate average per capita CO2
    // by user type for a given year
    // Household average vs MSME average
    @Query("SELECT AVG(s.perCapitaCo2) FROM CarbonScore s " +
            "JOIN s.user u " +
            "WHERE s.scoreYear = :year " +
            "AND u.userType = :userType")
    Double calculateAveragePerCapitaCo2ByYearAndUserType(
            @Param("year") Integer year,
            @Param("userType") User.UserType userType);

    // Used by ADMIN REPORTS — calculate average per capita CO2
    // by city for a given year
    // Used to build the geographic carbon heatmap
    @Query("SELECT u.city, AVG(s.perCapitaCo2) " +
            "FROM CarbonScore s " +
            "JOIN s.user u " +
            "WHERE s.scoreYear = :year " +
            "GROUP BY u.city " +
            "ORDER BY AVG(s.perCapitaCo2) DESC")
    List<Object[]> calculateAveragePerCapitaCo2ByCity(
            @Param("year") Integer year);

    // Used by ADMIN REPORTS — calculate average per capita CO2
    // by pin code for a given year
    // Used to build the geographic heatmap at pin code level
    @Query("SELECT u.pinCode, AVG(s.perCapitaCo2) " +
            "FROM CarbonScore s " +
            "JOIN s.user u " +
            "WHERE s.scoreYear = :year " +
            "GROUP BY u.pinCode " +
            "ORDER BY AVG(s.perCapitaCo2) DESC")
    List<Object[]> calculateAveragePerCapitaCo2ByPinCode(
            @Param("year") Integer year);

    // Used by ADMIN REPORTS — calculate platform average
    // category scores for a given year
    // Shows which category contributes most across all users
    @Query("SELECT " +
            "AVG(s.energyCo2), " +
            "AVG(s.transportCo2), " +
            "AVG(s.lifestyleCo2), " +
            "AVG(s.operationsCo2) " +
            "FROM CarbonScore s " +
            "WHERE s.scoreYear = :year")
    Object[] calculateAverageCategoryScoresByYear(
            @Param("year") Integer year);

    // Used by YEAR ON YEAR COMPARISON — get scores for a user
    // for the last 3 years to show improvement trend
    @Query("SELECT s FROM CarbonScore s " +
            "WHERE s.user.userId = :userId " +
            "AND s.scoreYear >= :fromYear " +
            "ORDER BY s.scoreYear ASC")
    List<CarbonScore> findScoresByUserFromYear(
            @Param("userId") Long userId,
            @Param("fromYear") Integer fromYear);

    // Used by RENEWAL ENGINE — find all users who have a
    // verified score for the previous year but no score
    // for the current year — these need renewal reminders
    @Query("SELECT s.user FROM CarbonScore s " +
            "WHERE s.scoreYear = :lastYear " +
            "AND s.user.userId NOT IN (" +
            "  SELECT s2.user.userId FROM CarbonScore s2 " +
            "  WHERE s2.scoreYear = :currentYear" +
            ")")
    List<Object> findUsersNeedingRenewal(
            @Param("lastYear") Integer lastYear,
            @Param("currentYear") Integer currentYear);
}
