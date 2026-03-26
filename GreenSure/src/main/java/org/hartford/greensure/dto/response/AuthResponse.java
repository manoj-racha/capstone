package org.hartford.greensure.dto.response;

import lombok.*;
import org.hartford.greensure.enums.Role;

/** Returned on successful login or registration (after OTP verified). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {

    private String token;
    private Role   role;
    private Long   id;
    private String fullName;
    private String email;
    private boolean isFirstLogin;
    private boolean emailVerified;
}
