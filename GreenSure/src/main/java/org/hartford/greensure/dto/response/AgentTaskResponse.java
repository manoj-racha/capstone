package org.hartford.greensure.dto.response;

import org.hartford.greensure.entity.AgentAssignment.AssignmentStatus;
import org.hartford.greensure.entity.User.UserType;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTaskResponse {

    private Long assignmentId;
    private AssignmentStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime deadline;
    private LocalDateTime completedAt;
    private boolean isOverdue;

    private Long userId;
    private String userName;
    private String userAddress;
    private String userPinCode;
    private String userCity;
    private String userMobile;
    private UserType userType;

    private Long declarationId;
    private Integer declarationYear;

    private Long agentId;
    private String agentName;
}
