package org.hartford.greensure.repository;

import org.hartford.greensure.entity.CarbonDeclaration;
import org.hartford.greensure.enums.DeclarationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CarbonDeclarationRepository extends JpaRepository<CarbonDeclaration, Long> {

    List<CarbonDeclaration> findByStatus(DeclarationStatus status);
    Page<CarbonDeclaration> findByStatusOrderBySubmittedAtDesc(DeclarationStatus status, Pageable pageable);
    Page<CarbonDeclaration> findAllByOrderBySubmittedAtDesc(Pageable pageable);
    long countByStatus(DeclarationStatus status);

    List<CarbonDeclaration> findByUserUserId(Long userId);

    List<CarbonDeclaration> findByUserUserIdOrderByDeclarationYearDesc(Long userId);

    Optional<CarbonDeclaration> findByUserUserIdAndDeclarationYear(Long userId, Integer year);

    boolean existsByUserUserId(Long userId);

    boolean existsByUserUserIdAndDeclarationYear(Long userId, Integer year);

    List<CarbonDeclaration> findByStatusAndFraudRiskLevel(
            DeclarationStatus status, String fraudRiskLevel);

    /** Find SUBMITTED declarations that have no active AgentAssignment. */
    @Query("""
        SELECT d FROM CarbonDeclaration d
        WHERE d.status = 'SUBMITTED'
          AND NOT EXISTS (
            SELECT a FROM AgentAssignment a
            WHERE a.declaration = d
              AND a.assignmentStatus = 'ACTIVE'
          )
    """)
    List<CarbonDeclaration> findUnassignedSubmitted();

    List<CarbonDeclaration> findByStatusIn(List<DeclarationStatus> statuses);

    /** Latest declaration year for renewal reminder check. */
    @Query("""
        SELECT d FROM CarbonDeclaration d
        WHERE d.user.userId = :userId
        ORDER BY d.declarationYear DESC
    """)
    List<CarbonDeclaration> findByUserIdOrderedByYear(@Param("userId") Long userId);
}
