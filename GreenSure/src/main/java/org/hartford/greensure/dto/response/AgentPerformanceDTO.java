package org.hartford.greensure.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full agent performance DTO returned by GET /agent/performance.
 * Includes identity, assignment counts, and recent history.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentPerformanceDTO {
    private Long agentId;
    private String agentName;
    private String email;
    private String pinCode;
    private int strikes;
    private boolean isActive;
    private LocalDateTime suspendedAt;

    private long activeAssignments;
    private long completedAssignments;
    private long totalAssignments;

    @Builder.Default
    private List<AssignmentHistorySummaryDTO> recentHistory = new java.util.ArrayList<>();
}
