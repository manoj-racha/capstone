package org.hartford.greensure.repository;


import org.hartford.greensure.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRepository
        extends JpaRepository<Verification, Long> {

    // Used by CARBON CALCULATION ENGINE — get the verified
    // data for a declaration to calculate the carbon score
    // This is the most critical call in the entire platform
    Optional<Verification> findByDeclarationDeclarationId(
            Long declarationId);

    // Used by AGENT DASHBOARD — check if agent has already
    // submitted a verification for this declaration
    // Prevents duplicate verification submissions
    boolean existsByDeclarationDeclarationId(
            Long declarationId);

    // Used by AGENT PERFORMANCE — get all verifications
    // submitted by a specific agent
    List<Verification> findByAgentAgentId(Long agentId);

    // Used by AGENT PERFORMANCE — get all verifications
    // submitted by an agent filtered by action type
    // e.g. how many did this agent CONFIRM vs MODIFY vs REJECT
    List<Verification> findByAgentAgentIdAndOverallAction(
            Long agentId, Verification.VerificationAction overallAction);

    // Used by AGENT PERFORMANCE — count verifications by
    // action type for a specific agent
    // Used to calculate confirmation rate and modification rate
    long countByAgentAgentIdAndOverallAction(
            Long agentId, Verification.VerificationAction overallAction);

    // Used by ADMIN REPORTS — count all verifications
    // by action type across the entire platform
    long countByOverallAction(Verification.VerificationAction overallAction);

    // Used by ADMIN — get all verifications submitted
    // within a date range for audit purposes
    @Query("SELECT v FROM Verification v " +
            "WHERE v.submittedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY v.submittedAt DESC")
    List<Verification> findVerificationsInDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Used by ADMIN AUDIT — get all verifications where
    // agent made corrections — overallAction = MODIFIED
    // Used to identify which agents correct data most often
    @Query("SELECT v FROM Verification v " +
            "WHERE v.overallAction = 'MODIFIED' " +
            "ORDER BY v.submittedAt DESC")
    List<Verification> findAllModifiedVerifications();

    // Used by AGENT PERFORMANCE — get all verifications
    // by an agent within a date range
    // Used to calculate monthly performance report
    @Query("SELECT v FROM Verification v " +
            "WHERE v.agent.agentId = :agentId " +
            "AND v.submittedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY v.submittedAt DESC")
    List<Verification> findByAgentInDateRange(
            @Param("agentId") Long agentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Used by ADMIN AUDIT — find verifications where
    // agent submitted from suspicious GPS location
    // GPS coordinates far from user registered address
    // Admin can investigate these manually
    @Query("SELECT v FROM Verification v " +
            "WHERE v.agentGpsLat IS NULL " +
            "OR v.agentGpsLng IS NULL")
    List<Verification> findVerificationsWithMissingGps();

    // Used by ADMIN REPORTS — calculate modification rate
    // for a specific agent
    // modification rate = MODIFIED count / total count
    @Query("SELECT " +
            "COUNT(CASE WHEN v.overallAction = 'MODIFIED' " +
            "THEN 1 END) * 100.0 / COUNT(v) " +
            "FROM Verification v " +
            "WHERE v.agent.agentId = :agentId")
    Double calculateModificationRateByAgent(
            @Param("agentId") Long agentId);
}
