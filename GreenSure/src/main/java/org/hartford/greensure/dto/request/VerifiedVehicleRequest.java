package org.hartford.greensure.dto.request;

import org.hartford.greensure.entity.DeclarationVehicle.FuelType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifiedVehicleRequest {

    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    private FuelType correctedFuelType;

    private Double correctedKm;
}
