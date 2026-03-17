package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualAssignmentRequest {

    @NotNull(message = "Declaration ID is required")
    private Long declarationId;

    @NotNull(message = "Agent ID is required")
    private Long agentId;
}
