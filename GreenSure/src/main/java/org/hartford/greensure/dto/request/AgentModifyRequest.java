package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.hartford.greensure.enums.CookingFuel;
import org.hartford.greensure.enums.FuelType;
import org.hartford.greensure.enums.MileageBand;

import java.util.List;

/**
 * Request DTO for the agent to submit corrected values during
 * the MODIFY verification action.
 * All fields are optional — only fields the agent changes are populated.
 * correctionNotes is mandatory when any correction is made.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentModifyRequest {

    private FuelType    correctedFuelType;
    private MileageBand correctedMileageBand;
    private Double      correctedMonthlyKwh;
    private CookingFuel correctedCookingFuelType;
    private Integer     correctedAnnualCylinders;
    private Double      correctedSolarCapacityKw;
    private Boolean     agentVerifiedSolar;

    @NotBlank(message = "Correction notes are required when modifying values")
    private String correctionNotes;

    /** URLs of uploaded proof photos/documents. */
    private List<String> documentUrls;
}
