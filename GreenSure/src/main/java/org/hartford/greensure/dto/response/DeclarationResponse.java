package org.hartford.greensure.dto.response;

import org.hartford.greensure.entity.CarbonDeclaration.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclarationResponse {

    private Long declarationId;
    private Long userId;
    private Integer declarationYear;
    private DeclarationStatus status;
    private Integer resubmissionCount;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private String rejectionReason;

    private Double electricityUnits;
    private Boolean hasSolar;
    private Double solarUnits;
    private CookingFuelType cookingFuelType;
    private Double lpgCylinders;
    private Double pngUnits;
    private Double biomassKgPerDay;
    private Integer numAcUnits;
    private Double acHoursPerDay;
    private Boolean hasGenerator;
    private Double generatorHoursPerMonth;

    private Boolean usesPublicTransport;
    private Double publicTransportKm;
    private List<VehicleResponse> vehicles;

    private DietaryPattern dietaryPattern;
    private ShoppingOrders shoppingOrdersPerMonth;

    private Boolean hasCommercialVehicles;
    private Double commercialVehicleKm;
    private Integer thirdPartyShipments;
    private Integer employeesPrivateVehicle;
    private Integer employeesPublicTransport;
    private Double generatorLitersPerMonth;
    private Boolean hasBoiler;
    private BoilerFuelType boilerFuelType;
    private Double boilerCoalKg;
    private Double boilerGasScm;
    private Integer paperReamsPerMonth;
    private Boolean usesRecycledPaper;
    private RawMaterialType rawMaterialType;
    private Double rawMaterialKg;

    // Assignment Details
    private Long assignedAgentId;
    private String assignedAgentName;
}
