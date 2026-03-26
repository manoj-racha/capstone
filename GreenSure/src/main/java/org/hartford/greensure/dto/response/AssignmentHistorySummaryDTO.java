package org.hartford.greensure.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Summary of a single assignment for the agent performance history list.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignmentHistorySummaryDTO {
    private Long assignmentId;
    private Long declarationId;
    private String userName;
    private String assignmentStatus;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
}
