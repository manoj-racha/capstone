package org.hartford.greensure.repository;

import org.hartford.greensure.entity.CarbonScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarbonScoreRepository extends JpaRepository<CarbonScore, Long> {
    Optional<CarbonScore> findByDeclarationDeclarationId(Long declarationId);
    boolean existsByDeclarationDeclarationId(Long declarationId);
    Optional<CarbonScore> findTopByUserUserIdOrderByScoreYearDesc(Long userId);
    List<CarbonScore> findByUserUserIdOrderByScoreYearDesc(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT s.user FROM CarbonScore s WHERE s.scoreYear = ?1 AND s.user NOT IN (SELECT s2.user FROM CarbonScore s2 WHERE s2.scoreYear = ?2)")
    List<Object> findUsersNeedingRenewal(int lastYear, int currentYear);
}
