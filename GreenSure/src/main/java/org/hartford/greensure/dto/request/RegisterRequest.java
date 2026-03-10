package org.hartford.greensure.dto.request;


import jakarta.validation.constraints.*;
import lombok.*;
import org.hartford.greensure.entity.HouseholdProfile;
import org.hartford.greensure.entity.MsmeProfile;
import org.hartford.greensure.entity.User;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    // ── COMMON FIELDS — Both Household and MSME ────────────────

    @NotNull(message = "User type is required")
    private User.UserType userType;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Mobile is required")
    @Pattern(regexp = "^[6-9]\\d{9}$",
            message = "Invalid Indian mobile number")
    private String mobile;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Pin code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$",
            message = "Invalid Indian pin code")
    private String pinCode;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    // ── HOUSEHOLD ONLY FIELDS ──────────────────────────────────

    // Required when userType = HOUSEHOLD
    @Min(value = 1, message = "Household must have at least 1 member")
    @Max(value = 20, message = "Member count seems too high")
    private Integer numberOfMembers;

    private HouseholdProfile.DwellingType dwellingType;

    // ── MSME ONLY FIELDS ───────────────────────────────────────

    // Required when userType = MSME
    @Size(max = 150, message = "Business name too long")
    private String businessName;

    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GST number format")
    private String gstNumber;

    private MsmeProfile.BusinessType businessType;

    @Min(value = 1, message = "Must have at least 1 employee")
    private Integer numEmployees;
}
