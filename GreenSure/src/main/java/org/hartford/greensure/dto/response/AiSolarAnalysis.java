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
public class AiSolarAnalysis {

    @JsonAlias("capacity_on_certificate")
    private Double capacityOnCertificate;

    @JsonAlias("declared_capacity")
    private Double declaredCapacity;

    @JsonAlias("capacity_match")
    private boolean capacityMatch;

    @JsonAlias("address_on_certificate")
    private String addressOnCertificate;

    @JsonAlias("address_match")
    private boolean addressMatch;

    @Builder.Default
    private List<String> findings = new ArrayList<>();
}
