package org.hartford.greensure.dto.request;

import org.hartford.greensure.entity.DeclarationVehicle.FuelType;
import org.hartford.greensure.entity.DeclarationVehicle.VehicleType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequest {

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;

    @NotNull(message = "KM per month is required")
    @Min(value = 0, message = "KM cannot be negative")
    private Double kmPerMonth;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
