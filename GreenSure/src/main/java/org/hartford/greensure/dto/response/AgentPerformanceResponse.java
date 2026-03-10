package org.hartford.greensure.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentPerformanceResponse {

    private Long agentId;
    private String fullName;
    private String employeeId;
    private Integer strikeCount;

    private long totalAssignments;
    private long completedAssignments;
    private long reassignedAssignments;
    private long totalVerifications;
    private long confirmedCount;
    private long modifiedCount;
    private long rejectedCount;

    private Double completionRate;
    private Double modificationRate;
    private Double confirmationRate;
}
