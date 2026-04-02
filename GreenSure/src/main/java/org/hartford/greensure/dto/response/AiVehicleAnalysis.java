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
public class AiVehicleAnalysis {

    @JsonAlias("vehicle_index")
    private int vehicleIndex;

    @JsonAlias("registration_number_on_doc")
    private String registrationNumberOnDoc;

    @JsonAlias("registration_number_match")
    private boolean registrationNumberMatch;

    @JsonAlias("fuel_type_on_doc")
    private String fuelTypeOnDoc;

    @JsonAlias("fuel_type_match")
    private boolean fuelTypeMatch;

    @JsonAlias("documents_read")
    private int documentsRead;

    @Builder.Default
    private List<String> findings = new ArrayList<>();
}
