package org.hartford.greensure.dto.response;

import lombok.Data;
import org.hartford.greensure.enums.VehicleCategory;

import java.util.List;

@Data
public class VehicleResponseDTO {
    private Long vehicleId;
    private VehicleCategory vehicleCategory;
    private String vehicleNickname;
    private String registrationNumber;
    private String make;
    private String model;
    private Integer year;
    private String fuelType;
    private String mileageBand;
    private String dataSource;
    private List<VehicleDocumentResponseDTO> documents;
}
