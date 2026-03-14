package org.hartford.greensure.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PolicyResponse {
    private String policyType;
    private String name;
    private String icon;
    private String description;
    private String eligibility;
    private List<PolicyPlanResponse> plans;
}
