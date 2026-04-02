package org.hartford.greensure.dto.response;

import lombok.*;
import org.hartford.greensure.entity.User;

/** Returned on successful login or registration (after OTP verified). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {

    private String token;
    private String role;
    private Long   id;
    private String fullName;
    private String email;
    private User.UserType userType;
    private boolean isFirstLogin;
    private boolean emailVerified;
}
