package org.hartford.greensure.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hartford.greensure.entity.User;
import org.hartford.greensure.enums.AssignmentStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTaskResponse {
    private Long assignmentId;
    private AssignmentStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime deadline;
    private LocalDateTime completedAt;
    private String assignedBy;
    private String reassignReason;
    private boolean overdue;

    // User details
    private Long userId;
    private String userName;
    private String userAddress;
    private String userPinCode;
    private String userCity;
    private String userMobile;
    private User.UserType userType;

    // Declaration details
    private Long declarationId;
    private Integer declarationYear;

    // Agent details
    private Long agentId;
    private String agentName;
}
