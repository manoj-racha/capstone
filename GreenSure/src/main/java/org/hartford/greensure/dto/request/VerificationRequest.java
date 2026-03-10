package org.hartford.greensure.dto.request;

import org.hartford.greensure.entity.CarbonDeclaration.*;
import org.hartford.greensure.entity.Verification.VerificationAction;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRequest {

    private Double correctedElectricityUnits;
    private Double correctedSolarUnits;
    private CookingFuelType correctedCookingFuelType;
    private Double correctedLpgCylinders;
    private Double correctedPngUnits;
    private Double correctedBiomassKg;
    private Double correctedGeneratorHours;

    private Double correctedPublicTransportKm;

    private List<VerifiedVehicleRequest> correctedVehicles;

    private DietaryPattern correctedDietaryPattern;
    private ShoppingOrders correctedShoppingOrders;

    private Double correctedCommercialVehicleKm;
    private Integer correctedThirdPartyShipments;
    private Double correctedGeneratorLiters;
    private Double correctedBoilerCoalKg;
    private Double correctedBoilerGasScm;
    private Integer correctedPaperReams;
    private Double correctedRawMaterialKg;

    @NotNull(message = "Overall action is required")
    private VerificationAction overallAction;

    private String agentRemarks;

    @NotNull(message = "GPS latitude is required")
    private Double agentGpsLat;

    @NotNull(message = "GPS longitude is required")
    private Double agentGpsLng;
}
