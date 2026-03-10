package org.hartford.greensure.dto.response;

import org.hartford.greensure.entity.CarbonDeclaration.DeclarationStatus;
import org.hartford.greensure.entity.CarbonScore.CarbonZone;
import org.hartford.greensure.entity.User.UserType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private Long userId;
    private String fullName;
    private UserType userType;
    private boolean hasDeclaration;

    private Long currentDeclarationId;
    private DeclarationStatus declarationStatus;
    private Integer declarationYear;

    private CarbonScoreResponse latestScore;

    private CarbonZone zone;

    private boolean renewalDue;

    private long unreadNotifications;
}
