package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_assignments")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    // Auto-set to 72 hours from assignedAt
    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ── MAPPINGS ───────────────────────────────────────────────

    // Many assignments can belong to one declaration
    // (one declaration can be reassigned multiple times)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private CarbonDeclaration declaration;

    // Many assignments can belong to one agent
    // (one agent handles many assignments over time)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    // ── ENUM ───────────────────────────────────────────────────

    public enum AssignmentStatus {
        ASSIGNED, IN_PROGRESS, COMPLETED, REASSIGNED
    }

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.assignedAt = LocalDateTime.now();
        // Deadline is always 72 hours from assignment time
        this.deadline = this.assignedAt.plusHours(72);
    }
}
