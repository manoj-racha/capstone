package org.hartford.greensure.dto.request;

import org.hartford.greensure.entity.CarbonDeclaration.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclarationRequest {

    @NotNull(message = "Electricity units are required")
    @Min(value = 0, message = "Electricity units cannot be negative")
    private Double electricityUnits;

    private Boolean hasSolar;

    @Min(value = 0, message = "Solar units cannot be negative")
    private Double solarUnits;

    private CookingFuelType cookingFuelType;

    @Min(value = 0)
    private Double lpgCylinders;

    @Min(value = 0)
    private Double pngUnits;

    @Min(value = 0)
    private Double biomassKgPerDay;

    @Min(value = 0)
    private Integer numAcUnits;

    @Min(value = 0)
    private Double acHoursPerDay;

    private Boolean hasGenerator;

    @Min(value = 0)
    private Double generatorHoursPerMonth;

    private Boolean usesPublicTransport;

    @Min(value = 0)
    private Double publicTransportKm;

    private List<VehicleRequest> vehicles;

    private DietaryPattern dietaryPattern;

    private ShoppingOrders shoppingOrdersPerMonth;

    private Boolean hasCommercialVehicles;

    @Min(value = 0)
    private Double commercialVehicleKm;

    @Min(value = 0)
    private Integer thirdPartyShipments;

    @Min(value = 0)
    private Integer employeesPrivateVehicle;

    @Min(value = 0)
    private Integer employeesPublicTransport;

    @Min(value = 0)
    private Double generatorLitersPerMonth;

    private Boolean hasBoiler;

    private BoilerFuelType boilerFuelType;

    @Min(value = 0)
    private Double boilerCoalKg;

    @Min(value = 0)
    private Double boilerGasScm;

    @Min(value = 0)
    private Integer paperReamsPerMonth;

    private Boolean usesRecycledPaper;

    private RawMaterialType rawMaterialType;

    @Min(value = 0)
    private Double rawMaterialKg;
}
