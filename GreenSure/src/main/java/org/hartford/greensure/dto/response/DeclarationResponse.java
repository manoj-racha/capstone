package org.hartford.greensure.dto.response;

import org.hartford.greensure.enums.DeclarationStatus;
import lombok.*;
import java.time.LocalDateTime;

/**
 * @deprecated Legacy flat declaration response.
 * Replaced by DeclarationDetailResponse (7-module structure).
 * Kept as stub to avoid import errors.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Deprecated
public class DeclarationResponse {
    private Long declarationId;
    private Long userId;
    private Integer declarationYear;
    private DeclarationStatus status;
    private Integer resubmissionCount;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private String rejectionReason;
    private Long assignedAgentId;
    private String assignedAgentName;
}
