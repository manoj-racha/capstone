package org.hartford.greensure.repository;



import org.hartford.greensure.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository
        extends JpaRepository<Agent, Long> {

    // Used during AGENT LOGIN — find agent by email
    Optional<Agent> findByEmail(String email);

    // Used during AGENT ONBOARDING — check if email exists
    boolean existsByEmail(String email);

    // Used during AGENT ONBOARDING — check if mobile exists
    boolean existsByMobile(String mobile);

    // Used during AGENT ONBOARDING — check if employee ID exists
    boolean existsByEmployeeId(String employeeId);

    // Used by AUTO ASSIGNMENT ENGINE — find all active
    // field agents only (not admins)
    List<Agent> findByAgentTypeAndStatus(
            Agent.AgentType agentType, Agent.AgentStatus status);

    // Used by AUTO ASSIGNMENT ENGINE — find active field agents
    // whose assigned zones contain the user's pin code
    // assignedZones is stored as comma-separated pin codes
    // e.g. "560001,560002,560003"
    @Query("SELECT a FROM Agent a " +
            "WHERE a.agentType = 'FIELD_AGENT' " +
            "AND a.status = 'ACTIVE' " +
            "AND a.assignedZones LIKE CONCAT('%', :pinCode, '%')")
    List<Agent> findActiveAgentsByPinCode(
            @Param("pinCode") String pinCode);

    // Used by ADMIN — find all agents by type
    List<Agent> findByAgentType(Agent.AgentType agentType);

    // Used by ADMIN — find all agents by status
    List<Agent> findByStatus(Agent.AgentStatus status);

    // Used by STRIKE SYSTEM — find all agents whose
    // strike count has reached or exceeded 3
    // These need admin attention and review
    List<Agent> findByStrikeCountGreaterThanEqual(Integer strikeCount);

    // Used by ADMIN REPORTS — count active field agents
    long countByAgentTypeAndStatus(
            Agent.AgentType agentType, Agent.AgentStatus status);

    // Used by ADMIN — search agents by name or employee ID
    @Query("SELECT a FROM Agent a WHERE " +
            "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.employeeId) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Agent> searchByNameOrEmployeeId(
            @Param("keyword") String keyword);

    // Used by AUTO ASSIGNMENT ENGINE — find agents in a pin code
    // with the lowest current active assignment count
    // so work is distributed evenly across agents
    @Query("SELECT a FROM Agent a " +
            "WHERE a.agentType = 'FIELD_AGENT' " +
            "AND a.status = 'ACTIVE' " +
            "AND a.assignedZones LIKE CONCAT('%', :pinCode, '%') " +
            "ORDER BY " +
            "(SELECT COUNT(aa) FROM AgentAssignment aa " +
            " WHERE aa.agent = a " +
            " AND aa.status IN ('ASSIGNED', 'IN_PROGRESS')) ASC")
    List<Agent> findLeastBusyAgentsByPinCode(
            @Param("pinCode") String pinCode);
}
