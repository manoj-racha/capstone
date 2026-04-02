package org.hartford.greensure.repository;

import org.hartford.greensure.entity.AgentAssignment;
import org.hartford.greensure.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentAssignmentRepository extends JpaRepository<AgentAssignment, Long> {

        List<AgentAssignment> findByAgentUserIdOrderByDeadlineAsc(Long userId);

        List<AgentAssignment> findByAgentUserIdAndAssignmentStatus(Long userId, AssignmentStatus assignmentStatus);

        List<AgentAssignment> findByAgentUserId(Long userId);

        List<AgentAssignment> findTop10ByAgentUserIdOrderByAssignedAtDesc(Long userId);

        @Query("SELECT aa FROM AgentAssignment aa " +
                        "WHERE aa.declaration.declarationId = :declarationId " +
                        "AND aa.assignmentStatus = 'ACTIVE'")
        Optional<AgentAssignment> findActiveAssignmentByDeclarationId(@Param("declarationId") Long declarationId);

        Optional<AgentAssignment> findTopByDeclarationDeclarationIdOrderByAssignedAtDesc(Long declarationId);

        @Query("SELECT CASE WHEN COUNT(aa) > 0 THEN true ELSE false END FROM AgentAssignment aa " +
                        "WHERE aa.declaration.declarationId = :declarationId " +
                        "AND aa.assignmentStatus = 'ACTIVE'")
        boolean existsActiveByDeclarationId(@Param("declarationId") Long declarationId);

        @Query("SELECT aa FROM AgentAssignment aa WHERE aa.assignmentStatus = 'ACTIVE' ORDER BY aa.assignedAt DESC")
        List<AgentAssignment> findActiveAssignments();

        @Query("SELECT aa FROM AgentAssignment aa WHERE aa.assignmentStatus = 'ACTIVE' ORDER BY aa.assignedAt DESC")
        Page<AgentAssignment> findActiveAssignments(Pageable pageable);

        List<AgentAssignment> findByDeclarationDeclarationId(Long declarationId);

        @Query("SELECT aa FROM AgentAssignment aa WHERE aa.assignmentStatus = 'ACTIVE' AND aa.deadline < :now")
        List<AgentAssignment> findOverdueAssignments(@Param("now") LocalDateTime now);

        @Query("SELECT aa FROM AgentAssignment aa WHERE aa.assignmentStatus = 'ACTIVE' AND aa.deadline BETWEEN :start AND :end")
        List<AgentAssignment> findAssignmentsNeedingReminder(@Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        Page<AgentAssignment> findByAssignmentStatusOrderByAssignedAtDesc(AssignmentStatus assignmentStatus,
                        Pageable pageable);

        long countByAgentUserIdAndAssignmentStatus(Long userId, AssignmentStatus assignmentStatus);

        @Query("SELECT COUNT(aa) FROM AgentAssignment aa WHERE aa.agent.userId = :userId AND aa.assignmentStatus = 'ACTIVE'")
        long countActiveAssignmentsByUserId(@Param("userId") Long userId);

        long countByAssignmentStatus(AssignmentStatus assignmentStatus);

        @Query("SELECT aa FROM AgentAssignment aa WHERE aa.agent.userId = :userId AND aa.assignmentStatus = 'COMPLETED' AND aa.completedAt BETWEEN :startDate AND :endDate")
        List<AgentAssignment> findCompletedAssignmentsByAgentInRange(
                        @Param("userId") Long userId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        boolean existsByDeclarationDeclarationIdAndAssignmentStatusIn(Long declarationId,
                        List<AssignmentStatus> statuses);
}
