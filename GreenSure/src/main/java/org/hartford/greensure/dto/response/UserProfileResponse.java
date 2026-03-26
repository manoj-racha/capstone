package org.hartford.greensure.dto.response;

import lombok.*;
import org.hartford.greensure.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long userId;
    private Role role;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private String pinCode;
    private String city;
    private String state;
    private boolean emailVerified;
    private boolean phoneVerified;
    private int strikes;
    private LocalDateTime suspendedAt;
    private LocalDateTime createdAt;
    
    // Household specific
    private Integer numberOfMembers;
}
