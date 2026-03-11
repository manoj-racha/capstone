package org.hartford.greensure.repository;

import org.hartford.greensure.entity.AgentAssignment;
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
public interface AgentAssignmentRepository
                extends JpaRepository<AgentAssignment, Long> {

        // Used by AGENT DASHBOARD — get all assignments for
        // a specific agent ordered by deadline (urgent first)
        List<AgentAssignment> findByAgentAgentIdOrderByDeadlineAsc(
                        Long agentId);

        // Used by AGENT DASHBOARD — get assignments for an agent
        // filtered by status
        // e.g. show only ASSIGNED tasks not completed ones
        List<AgentAssignment> findByAgentAgentIdAndStatus(
                        Long agentId, AgentAssignment.AssignmentStatus status);

        // Used by DECLARATION FLOW — get the current active
        // assignment for a specific declaration
        // Active means ASSIGNED or IN_PROGRESS
        @Query("SELECT aa FROM AgentAssignment aa " +
                        "WHERE aa.declaration.declarationId = :declarationId " +
                        "AND aa.status IN ('ASSIGNED', 'IN_PROGRESS')")
        Optional<AgentAssignment> findActiveAssignmentByDeclarationId(
                        @Param("declarationId") Long declarationId);

        // Used by ADMIN — get all assignments for a declaration
        // including past REASSIGNED ones for full audit trail
        List<AgentAssignment> findByDeclarationDeclarationId(
                        Long declarationId);

        // Used by DEADLINE MONITOR — find all assignments
        // that have breached the 72 hour deadline
        // and are still not completed
        @Query("SELECT aa FROM AgentAssignment aa " +
                        "WHERE aa.status IN ('ASSIGNED', 'IN_PROGRESS') " +
                        "AND aa.deadline < :now")
        List<AgentAssignment> findOverdueAssignments(
                        @Param("now") LocalDateTime now);

        // Used by DEADLINE MONITOR — find assignments approaching
        // the 48 hour reminder window
        // These agents need a reminder notification
        @Query("SELECT aa FROM AgentAssignment aa " +
                        "WHERE aa.status IN ('ASSIGNED', 'IN_PROGRESS') " +
                        "AND aa.deadline BETWEEN :reminderStart AND :reminderEnd")
        List<AgentAssignment> findAssignmentsNeedingReminder(
                        @Param("reminderStart") LocalDateTime reminderStart,
                        @Param("reminderEnd") LocalDateTime reminderEnd);

        // Used by ADMIN — get all assignments filtered by status
        Page<AgentAssignment> findByStatusOrderByAssignedAtDesc(
                        AgentAssignment.AssignmentStatus status, Pageable pageable);

        // Used by AGENT PERFORMANCE — count completed assignments
        // for a specific agent
        long countByAgentAgentIdAndStatus(
                        Long agentId, AgentAssignment.AssignmentStatus status);

        // Used by AGENT PERFORMANCE — count reassigned assignments
        // for an agent — high reassigned count = poor performance
        // long countByAgentAgentIdAndStatus(
        // Long agentId, AgentAssignment.AssignmentStatus status);

        // Used by AUTO ASSIGNMENT ENGINE — count active assignments
        // for an agent to check if they are under max workload
        @Query("SELECT COUNT(aa) FROM AgentAssignment aa " +
                        "WHERE aa.agent.agentId = :agentId " +
                        "AND aa.status IN ('ASSIGNED', 'IN_PROGRESS')")
        long countActiveAssignmentsByAgentId(
                        @Param("agentId") Long agentId);

        // Used by ADMIN REPORTS — count all assignments by status
        long countByStatus(AgentAssignment.AssignmentStatus status);

        // Used by AGENT PERFORMANCE — get all completed assignments
        // for an agent within a date range
        // Used to calculate monthly performance metrics
        @Query("SELECT aa FROM AgentAssignment aa " +
                        "WHERE aa.agent.agentId = :agentId " +
                        "AND aa.status = 'COMPLETED' " +
                        "AND aa.completedAt BETWEEN :startDate AND :endDate")
        List<AgentAssignment> findCompletedAssignmentsByAgentInRange(
                        @Param("agentId") Long agentId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Used by ADMIN — check if a declaration already has
        // a pending assignment before creating a new one
        // Prevents duplicate assignments for same declaration
        boolean existsByDeclarationDeclarationIdAndStatusIn(
                        Long declarationId, List<AgentAssignment.AssignmentStatus> statuses);
}
