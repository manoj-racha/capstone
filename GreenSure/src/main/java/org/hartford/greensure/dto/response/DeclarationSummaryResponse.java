package org.hartford.greensure.dto.response;

import lombok.*;
import org.hartford.greensure.enums.DeclarationStatus;

import java.time.LocalDateTime;

/** Summary card shown in declaration history list. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeclarationSummaryResponse {

    private Long              declarationId;
    private Long              userId;
    private String            fullName;
    private Integer           declarationYear;
    private DeclarationStatus status;
    private String            fraudRiskLevel;
    private LocalDateTime     submittedAt;
    private LocalDateTime     createdAt;

    // Score fields — populated only when status = VERIFIED
    private Double  totalCo2;
    private Double  perCapitaCo2;
    private String  zone;
    private Double  discountPercent;
}
