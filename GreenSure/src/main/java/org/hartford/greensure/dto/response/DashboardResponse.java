package org.hartford.greensure.dto.response;

import org.hartford.greensure.enums.DeclarationStatus;
import org.hartford.greensure.enums.Zone;
import org.hartford.greensure.entity.User;
import lombok.*;

/**
 * Dashboard response for USER role.
 * MSME UserType references removed.
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private Long userId;
    private String fullName;
    private User.UserType userType;
    private boolean hasDeclaration;

    private Long currentDeclarationId;
    private DeclarationStatus declarationStatus;
    private Integer declarationYear;

    private CarbonScoreResponse latestScore;
    private Zone zone;
    private boolean renewalDue;

    private long unreadNotifications;
}
