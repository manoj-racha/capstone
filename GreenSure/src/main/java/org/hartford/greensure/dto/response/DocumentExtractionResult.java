package org.hartford.greensure.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentExtractionResult {

    private boolean success;
    private String billingMonth;
    private Double unitsKwh;
    private Double amount;
    private String consumerNumber;
    private String providerName;
    private String distributorName;
    private String registrationNumber;
    private String fuelType;
    private Double scmConsumed;
    private Double installedCapacityKw;
    private String installationDate;
    private Integer cylinderCount;
    private Double confidence;
    @Builder.Default
    private List<String> anomalies = new ArrayList<>();
    private String errorMessage;

    public static DocumentExtractionResult failed(String msg) {
        return DocumentExtractionResult.builder()
                .success(false)
                .errorMessage(msg)
                .confidence(0.0)
                .anomalies(Collections.emptyList())
                .build();
    }
}
