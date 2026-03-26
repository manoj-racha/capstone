package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.hartford.greensure.enums.CookingFuel;

/**
 * Request DTO for Module 6 — Cooking Fuel Data.
 * pngConsumerNumber required when fuelType = PNG.
 * userDeclaredCylinders required when fuelType = LPG.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CookingDataRequest {

    @NotNull(message = "Cooking fuel type is required")
    private CookingFuel fuelType;

    /** Required when fuelType = PNG. */
    private String pngConsumerNumber;

    /** Required when fuelType = LPG. Range 0–50. */
    @Min(value = 0,  message = "Cylinders cannot be negative")
    @Max(value = 50, message = "Cylinders cannot exceed 50 per year")
    private Integer userDeclaredCylinders;

    /** JSON array string of uploaded bill URLs */
    private String billUrls;
}
