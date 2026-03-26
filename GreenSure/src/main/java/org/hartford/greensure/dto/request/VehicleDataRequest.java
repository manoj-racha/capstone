package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.hartford.greensure.enums.DataSource;
import org.hartford.greensure.enums.FuelType;
import org.hartford.greensure.enums.MileageBand;

/**
 * Request DTO for Module 3 — Vehicle Data.
 * At least one of vin or registrationNumber must be supplied.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VehicleDataRequest {

    /** Vehicle Identification Number — 17 chars. Optional if regNumber provided. */
    private String vin;

    /** Registration plate number. Optional if vin provided. */
    private String registrationNumber;

    @NotBlank(message = "Vehicle make is required")
    private String make;

    @NotBlank(message = "Vehicle model is required")
    private String model;

    @NotNull(message = "Manufacturing year is required")
    @Min(value = 1990, message = "Year must be 1990 or later")
    @Max(value = 2026, message = "Year cannot be in the future")
    private Integer year;

    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;

    @NotNull(message = "Mileage band is required")
    private MileageBand mileageBand;

    /** How the vehicle data was sourced. Defaults to MANUAL if not provided. */
    private DataSource dataSource;

    /** URL of uploaded RC book — required when dataSource = MANUAL. */
    private String rcUploadUrl;
}
