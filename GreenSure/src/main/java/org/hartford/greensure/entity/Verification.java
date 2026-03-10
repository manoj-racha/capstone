package org.hartford.greensure.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "verifications")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long verificationId;

    // ── CORRECTED ENERGY VALUES ────────────────────────────────
    // null = agent confirmed the user declared value
    // non-null = agent corrected it to this value

    @Column(name = "corrected_electricity_units")
    private Double correctedElectricityUnits;

    @Column(name = "corrected_solar_units")
    private Double correctedSolarUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "corrected_cooking_fuel_type")
    private CarbonDeclaration.CookingFuelType correctedCookingFuelType;

    @Column(name = "corrected_lpg_cylinders")
    private Double correctedLpgCylinders;

    @Column(name = "corrected_png_units")
    private Double correctedPngUnits;

    @Column(name = "corrected_biomass_kg")
    private Double correctedBiomassKg;

    @Column(name = "corrected_generator_hours")
    private Double correctedGeneratorHours;

    // ── CORRECTED TRANSPORT VALUES ─────────────────────────────

    @Column(name = "corrected_public_transport_km")
    private Double correctedPublicTransportKm;

    // ── CORRECTED LIFESTYLE VALUES — Household Only ────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "corrected_dietary_pattern")
    private CarbonDeclaration.DietaryPattern correctedDietaryPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "corrected_shopping_orders")
    private CarbonDeclaration.ShoppingOrders correctedShoppingOrders;

    // ── CORRECTED OPERATIONS VALUES — MSME Only ────────────────

    @Column(name = "corrected_commercial_vehicle_km")
    private Double correctedCommercialVehicleKm;

    @Column(name = "corrected_third_party_shipments")
    private Integer correctedThirdPartyShipments;

    @Column(name = "corrected_generator_liters")
    private Double correctedGeneratorLiters;

    @Column(name = "corrected_boiler_coal_kg")
    private Double correctedBoilerCoalKg;

    @Column(name = "corrected_boiler_gas_scm")
    private Double correctedBoilerGasScm;

    @Column(name = "corrected_paper_reams")
    private Integer correctedPaperReams;

    @Column(name = "corrected_raw_material_kg")
    private Double correctedRawMaterialKg;

    // ── AGENT DECISION ─────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_action", nullable = false)
    private VerificationAction overallAction;

    // Required when overallAction is MODIFIED or REJECTED
    @Column(name = "agent_remarks", columnDefinition = "TEXT")
    private String agentRemarks;

    // GPS location of agent at time of submission
    @Column(name = "agent_gps_lat")
    private Double agentGpsLat;

    @Column(name = "agent_gps_lng")
    private Double agentGpsLng;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // ── MAPPINGS ───────────────────────────────────────────────

    // One verification belongs to one declaration
    // declaration is the owner of this relationship
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false, unique = true)
    private CarbonDeclaration declaration;

    // Many verifications can be submitted by one agent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    // One verification has many corrected vehicle entries
    @OneToMany(mappedBy = "verification", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VerifiedVehicle> verifiedVehicles = new ArrayList<>();

    // ── ENUM ───────────────────────────────────────────────────

    public enum VerificationAction {
        CONFIRMED, MODIFIED, REJECTED
    }

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }
}
