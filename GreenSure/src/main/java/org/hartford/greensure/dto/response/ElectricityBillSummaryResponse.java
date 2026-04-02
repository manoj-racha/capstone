package org.hartford.greensure.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectricityBillSummaryResponse {

    private String billingMonth;
    private Double unitsKwh;
    private Double amount;
    private String billUrl;
    private Double ocrConfidenceScore;
    private Boolean aiAnomalyFlag;
}
