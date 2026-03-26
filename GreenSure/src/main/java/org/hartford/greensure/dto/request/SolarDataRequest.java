package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Request DTO for Module 5 — Solar Installation Data (optional module). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SolarDataRequest {

    @NotNull(message = "hasSolar flag is required")
    private Boolean hasSolar;

    /** Required when hasSolar = true. */
    @DecimalMin(value = "0.1", message = "Solar capacity must be at least 0.1 kW")
    @DecimalMax(value = "100", message = "Solar capacity cannot exceed 100 kW")
    private Double capacityKw;

    /** URL of uploaded MNRE/installer certificate. */
    private String certificateUrl;
}
