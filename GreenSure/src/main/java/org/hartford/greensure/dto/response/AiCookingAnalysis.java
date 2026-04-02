package org.hartford.greensure.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiCookingAnalysis {

    @JsonAlias("fuel_type")
    private String fuelType;

    @JsonAlias("receipts_found")
    private int receiptsFound;

    @JsonAlias("declared_cylinders")
    private Integer declaredCylinders;

    @JsonAlias("cylinder_count_match")
    private boolean cylinderCountMatch;

    @JsonAlias("distributor_name")
    private String distributorName;

    @JsonAlias("consumer_number_consistent")
    private boolean consumerNumberConsistent;

    @Builder.Default
    private List<String> findings = new ArrayList<>();
}
