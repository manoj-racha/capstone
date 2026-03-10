package org.hartford.greensure.dto.request;

import org.hartford.greensure.entity.Agent.AgentType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAgentRequest {

    @NotNull(message = "Agent type is required")
    private AgentType agentType;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Mobile is required")
    @Pattern(regexp = "^[6-9]\\d{9}$",
             message = "Invalid Indian mobile number")
    private String mobile;

    @NotBlank(message = "Password is required")
    @Size(min = 8)
    private String password;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotBlank(message = "Assigned zones are required")
    private String assignedZones;
}
