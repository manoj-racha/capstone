package org.hartford.greensure.repository;

import org.hartford.greensure.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationRepository extends JpaRepository<Verification, Long> {
    Optional<Verification> findByAssignmentAssignmentId(Long assignmentId);
    Optional<Verification> findByDeclarationDeclarationId(Long declarationId);
    boolean existsByDeclarationDeclarationId(Long declarationId);

    long countByAgentUserIdAndOutcome(Long agentId, org.hartford.greensure.enums.VerificationOutcome outcome);
}
