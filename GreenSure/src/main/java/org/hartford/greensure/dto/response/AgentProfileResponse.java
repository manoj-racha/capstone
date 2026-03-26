package org.hartford.greensure.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/** Agent profile DTO returned by /agent/profile and /admin/agents/{id}. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentProfileResponse {

    private Long          agentId;
    private String        fullName;
    private String        email;
    private String        phone;
    private String        pinCode;
    private boolean       active;
    private LocalDateTime suspendedAt;
    private int           strikes;
    private long          totalCompleted;
    private long          activeAssignments;
    private LocalDateTime createdAt;
}
