package org.hartford.greensure.dto.response;

import lombok.*;

/**
 * @deprecated Superseded by DeclarationDetailResponse + AgentWorkspaceResponse.
 * Kept as a deprecated stub to satisfy any remaining import references.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Deprecated
public class VehicleResponse {
    private Long vehicleId;
    private String fuelType;
    private String mileageBand;
}
