package org.hartford.greensure.dto.response;

import org.hartford.greensure.entity.DeclarationVehicle.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    private Long vehicleId;
    private VehicleType vehicleType;
    private FuelType fuelType;
    private Double kmPerMonth;
    private Integer quantity;
}
