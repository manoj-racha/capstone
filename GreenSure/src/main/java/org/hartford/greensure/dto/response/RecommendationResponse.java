package org.hartford.greensure.dto.response;

import org.hartford.greensure.entity.Recommendation.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {

    private Long recommendationId;
    private RecommendationCategory category;
    private RecommendationPriority priority;
    private String recommendationText;
    private LocalDateTime generatedAt;
}
