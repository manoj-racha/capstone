package org.hartford.greensure.repository;


import org.hartford.greensure.entity.CarbonDeclaration;
import org.hartford.greensure.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarbonDeclarationRepository
        extends JpaRepository<CarbonDeclaration, Long> {

    // Used during DECLARATION SUBMISSION — check if user already
    // has a declaration for the current year
    // Enforces the one declaration per user per year rule
    boolean existsByUserUserIdAndDeclarationYear(
            Long userId, Integer year);

    // Used during FIRST LOGIN CHECK — does this user have
    // any declaration at all?
    // If false → show welcome screen, force declaration
    boolean existsByUserUserId(Long userId);

    // Used during DECLARATION FLOW — get user's current year
    // declaration to show status timeline on dashboard
    Optional<CarbonDeclaration> findByUserUserIdAndDeclarationYear(
            Long userId, Integer year);

    // Used during DECLARATION HISTORY — show all past declarations
    // for a user sorted by latest year first
    List<CarbonDeclaration> findByUserUserIdOrderByDeclarationYearDesc(
            Long userId);

    // Used by AGENT ASSIGNMENT ENGINE — find all declarations
    // that are submitted but not yet assigned to an agent
    List<CarbonDeclaration> findByStatus(CarbonDeclaration.DeclarationStatus status);

    // Used by ADMIN — filter all declarations by status
    List<CarbonDeclaration> findByStatusOrderBySubmittedAtDesc(
            CarbonDeclaration.DeclarationStatus status);

    // Used by ADMIN — filter declarations by user type
    // e.g. show only MSME declarations
    @Query("SELECT d FROM CarbonDeclaration d " +
            "JOIN d.user u " +
            "WHERE u.userType = :userType " +
            "ORDER BY d.submittedAt DESC")
    List<CarbonDeclaration> findByUserType(
            @Param("userType") User.UserType userType);

    // Used by ADMIN — filter declarations by status AND user type
    @Query("SELECT d FROM CarbonDeclaration d " +
            "JOIN d.user u " +
            "WHERE d.status = :status " +
            "AND u.userType = :userType " +
            "ORDER BY d.submittedAt DESC")
    List<CarbonDeclaration> findByStatusAndUserType(
            @Param("status") CarbonDeclaration.DeclarationStatus status,
            @Param("userType") User.UserType userType);

    // Used by ADMIN DECLARATION UNLOCK — find SUBMITTED declarations
    // that were submitted within the last 24 hours
    // Only these can be unlocked for editing
    @Query("SELECT d FROM CarbonDeclaration d " +
            "WHERE d.status = 'SUBMITTED' " +
            "AND d.submittedAt >= :cutoffTime")
    List<CarbonDeclaration> findUnlockableDeclarations(
            @Param("cutoffTime") LocalDateTime cutoffTime);

    // Used by RENEWAL ENGINE — find all VERIFIED declarations
    // from 11 months ago that need renewal reminders
    @Query("SELECT d FROM CarbonDeclaration d " +
            "WHERE d.status = 'VERIFIED' " +
            "AND d.declarationYear = :year")
    List<CarbonDeclaration> findVerifiedDeclarationsByYear(
            @Param("year") Integer year);

    // Used by ADMIN REPORTS — count declarations by status
    long countByStatus(CarbonDeclaration.DeclarationStatus status);

    // Used by ADMIN REPORTS — count total declarations for a year
    long countByDeclarationYear(Integer year);

    // Used by REJECTION FLOW — find declarations with high
    // resubmission count that need admin attention
    @Query("SELECT d FROM CarbonDeclaration d " +
            "WHERE d.resubmissionCount >= :count " +
            "AND d.status = 'REJECTED'")
    List<CarbonDeclaration> findByResubmissionCountGreaterThanEqual(
            @Param("count") Integer count);
}
