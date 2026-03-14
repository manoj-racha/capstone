package org.hartford.greensure.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserPolicyResponse {
    private Long id;
    private Long userId;
    private Long planId;
    private String policyType;
    private String policyName;
    private String planName;
    private Double coverageAmount;
    private Integer durationMonths;
    private Double finalPrice;
    private LocalDateTime purchasedAt;
}
