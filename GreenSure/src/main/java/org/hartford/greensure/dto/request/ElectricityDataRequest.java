package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Request DTO for Module 4 — Electricity Data. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ElectricityDataRequest {

    @NotBlank(message = "Electricity provider is required")
    private String provider;

    @NotBlank(message = "Consumer number is required")
    private String consumerNumber;

    @NotNull(message = "Monthly kWh is required")
    @Min(value = 10,   message = "Monthly kWh must be at least 10")
    @Max(value = 3000, message = "Monthly kWh cannot exceed 3000")
    private Double userDeclaredMonthlyKwh;
}
