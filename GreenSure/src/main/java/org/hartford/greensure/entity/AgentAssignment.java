package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hartford.greensure.enums.AssignmentStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_assignments")
@Getter
@Setter
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

    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AssignmentStatus assignmentStatus = AssignmentStatus.ACTIVE;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "gps_lat_at_start")
    private Double gpsLatAtStart;

    @Column(name = "gps_lng_at_start")
    private Double gpsLngAtStart;

    @Column(name = "assigned_by", length = 20)
    @Builder.Default
    private String assignedBy = "SYSTEM";

    @Column(name = "reassign_reason", columnDefinition = "TEXT")
    private String reassignReason;

    // ── MAPPINGS ───────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private CarbonDeclaration declaration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    // ── Convenience helpers used by service/repository queries ─

    public boolean isActive() {
        return assignmentStatus == AssignmentStatus.ACTIVE;
    }

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.assignedAt = LocalDateTime.now();
        if (this.deadline == null) {
            this.deadline = this.assignedAt.plusHours(72);
        }
        if (this.assignmentStatus == null) {
            this.assignmentStatus = AssignmentStatus.ACTIVE;
        }
        if (this.assignedBy == null || this.assignedBy.isBlank()) {
            this.assignedBy = "SYSTEM";
        }
    }
}
