package org.hartford.greensure.dto.response;

import lombok.*;

/** Carbon score breakdown used inside DeclarationDetailResponse and standalone endpoints. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CarbonScoreResponse {

    private Long   scoreId;
    private Integer scoreYear;

    private Double vehicleCo2;
    private Double electricityCo2;
    private Double cookingCo2;
    private Double solarOffset;
    private Double lifestyleBonus;
    private Double totalCo2;
    private Double perCapitaCo2;

    private String zone;
    private Double discountPercent;
    private String discountBreakdown;
}
