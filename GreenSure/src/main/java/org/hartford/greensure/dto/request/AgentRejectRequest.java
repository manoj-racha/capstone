package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/** Request DTO for the agent to reject a declaration. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentRejectRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(min = 10, max = 500, message = "Rejection reason must be 10–500 characters")
    private String rejectionReason;

    private List<String> documentUrls;
}
