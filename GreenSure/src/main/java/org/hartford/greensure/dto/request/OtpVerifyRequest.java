package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Request DTO for OTP verification at the end of registration. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpVerifyRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    private String otp;
}
