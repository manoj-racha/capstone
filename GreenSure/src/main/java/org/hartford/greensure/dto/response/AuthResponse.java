package org.hartford.greensure.dto.response;

import org.hartford.greensure.entity.User.UserType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String role;
    private UserType userType;
    private Long id;
    private String fullName;
    private String email;
    private boolean isFirstLogin;
}
