package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.hartford.greensure.entity.User;

/** Request DTO for admin to create a new field agent account. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAgentRequest {

    @Builder.Default
    private String agentType = "AGENT";

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String phone;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String mobile;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid pincode format")
    private String pinCode;

    private String employeeId;
    private String assignedZones;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    public User.UserType resolvedAgentType() {
        if (agentType == null) {
            return User.UserType.AGENT;
        }
        String normalized = agentType.trim().toUpperCase();
        if ("ADMIN".equals(normalized)) {
            return User.UserType.ADMIN;
        }
        // Legacy/front-end alias support.
        if ("FIELD_AGENT".equals(normalized) || "AGENT".equals(normalized)) {
            return User.UserType.AGENT;
        }
        return User.UserType.AGENT;
    }

    public String resolvedMobile() {
        if (mobile != null && !mobile.isBlank()) {
            return mobile.trim();
        }
        if (phone != null && !phone.isBlank()) {
            return phone.trim();
        }
        return null;
    }

    public String resolvedPinCode() {
        if (pinCode != null && !pinCode.isBlank()) {
            return pinCode.trim();
        }
        if (assignedZones != null && !assignedZones.isBlank()) {
            String[] zones = assignedZones.split(",");
            if (zones.length > 0 && !zones[0].isBlank()) {
                return zones[0].trim();
            }
        }
        return null;
    }
}
