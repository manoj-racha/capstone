package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BuyPolicyRequest {
    @NotNull
    private Long planId;
    
    @NotNull
    private Integer durationMonths;
    
    @NotNull
    private Double finalPrice;
}
