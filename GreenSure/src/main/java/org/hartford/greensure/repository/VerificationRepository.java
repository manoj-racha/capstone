package org.hartford.greensure.repository;

import org.hartford.greensure.entity.Verification;
import org.hartford.greensure.enums.VerificationOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationRepository extends JpaRepository<Verification, Long> {
    Optional<Verification> findByAssignmentAssignmentId(Long assignmentId);

    Optional<Verification> findTopByDeclarationDeclarationIdOrderByVerifiedAtDescVerificationIdDesc(Long declarationId);

    default Optional<Verification> findByDeclarationDeclarationId(Long declarationId) {
        return findTopByDeclarationDeclarationIdOrderByVerifiedAtDescVerificationIdDesc(declarationId);
    }

    boolean existsByDeclarationDeclarationId(Long declarationId);

    /** Used by AdminController performance report. */
    long countByOutcome(VerificationOutcome outcome);

    long countByAgentUserIdAndOutcome(Long agentId, VerificationOutcome outcome);
}
