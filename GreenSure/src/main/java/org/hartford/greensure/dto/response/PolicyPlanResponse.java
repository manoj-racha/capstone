package org.hartford.greensure.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PolicyPlanResponse {
    private Long planId;
    private String planName;
    private Double coverageAmount;
    private Double basePremiumYearly;
    private List<String> features;
}
