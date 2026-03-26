package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hartford.greensure.enums.DataSource;
import org.hartford.greensure.enums.FuelType;
import org.hartford.greensure.enums.MileageBand;
import org.hartford.greensure.enums.VehicleCategory;

@Data
public class AddVehicleRequestDTO {
    @NotNull private VehicleCategory vehicleCategory;
    private String vehicleNickname;
    private String vin;
    @NotBlank private String registrationNumber;
    @NotBlank private String make;
    @NotBlank private String model;
    @NotNull @Min(1990) @Max(2026) private Integer year;
    @NotNull private FuelType fuelType;
    @NotNull private MileageBand mileageBand;
    private DataSource dataSource = DataSource.MANUAL;
}
