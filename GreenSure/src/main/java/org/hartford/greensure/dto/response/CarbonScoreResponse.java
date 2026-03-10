package org.hartford.greensure.dto.response;

import org.hartford.greensure.entity.CarbonScore.CarbonZone;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarbonScoreResponse {

    private Long scoreId;
    private Long userId;
    private Integer scoreYear;

    private Double energyCo2;
    private Double transportCo2;
    private Double lifestyleCo2;
    private Double operationsCo2;

    private Double totalCo2;
    private Double perCapitaCo2;

    private CarbonZone zone;
    private LocalDateTime generatedAt;

    private Double energyPercentage;
    private Double transportPercentage;
    private Double lifestylePercentage;
    private Double operationsPercentage;

    private Double cityAverage;
    private Double nationalAverage;

    private List<RecommendationResponse> recommendations;

    private Double previousYearCo2;
    private Double improvementPercentage;
}
