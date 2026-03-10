package org.hartford.greensure.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agents")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agent_id")
    private Long agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false)
    private AgentType agentType;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "mobile", nullable = false, unique = true, length = 15)
    private String mobile;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    private String employeeId;

    // Comma-separated pin codes this agent is assigned to
    @Column(name = "assigned_zones", columnDefinition = "TEXT")
    private String assignedZones;

    @Column(name = "strike_count")
    @Builder.Default
    private Integer strikeCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AgentStatus status = AgentStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── MAPPINGS ───────────────────────────────────────────────

    // One agent  many ashassignments over time
    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<AgentAssignment> assignments = new ArrayList<>();

    // One agent has submitted many verifications
    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<Verification> verifications = new ArrayList<>();

    // ── ENUMS ──────────────────────────────────────────────────

    public enum AgentType {
        FIELD_AGENT, ADMIN
    }

    public enum AgentStatus {
        ACTIVE, INACTIVE
    }

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
