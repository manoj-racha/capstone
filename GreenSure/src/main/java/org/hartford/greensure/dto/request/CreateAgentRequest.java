package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Request DTO for admin to create a new field agent account. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateAgentRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String phone;

    @NotBlank(message = "Pin code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid pincode format")
    private String pinCode;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
