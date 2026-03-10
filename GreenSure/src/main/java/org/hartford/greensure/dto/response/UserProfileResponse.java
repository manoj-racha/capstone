package org.hartford.greensure.dto.response;

import org.hartford.greensure.entity.HouseholdProfile.DwellingType;
import org.hartford.greensure.entity.MsmeProfile.BusinessType;
import org.hartford.greensure.entity.User.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long userId;
    private UserType userType;
    private String fullName;
    private String email;
    private String mobile;
    private String address;
    private String pinCode;
    private String city;
    private String state;
    private UserStatus status;
    private LocalDateTime createdAt;

    private Integer numberOfMembers;
    private DwellingType dwellingType;

    private String businessName;
    private String gstNumber;
    private BusinessType businessType;
    private Integer numEmployees;
}
