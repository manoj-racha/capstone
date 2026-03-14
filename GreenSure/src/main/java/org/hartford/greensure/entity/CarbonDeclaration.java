package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "carbon_declarations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_year",
                columnNames = {"user_id", "declaration_year"}
        )
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarbonDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "declaration_id")
    private Long declarationId;

    @Column(name = "declaration_year", nullable = false)
    private Integer declarationYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DeclarationStatus status = DeclarationStatus.DRAFT;

    @Column(name = "resubmission_count")
    @Builder.Default
    private Integer resubmissionCount = 0;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    // ── ENERGY FIELDS — Both Household and MSME ────────────────

    @Column(name = "electricity_units")
    private Double electricityUnits;

    @Column(name = "has_solar")
    @Builder.Default
    private Boolean hasSolar = false;

    @Column(name = "solar_units")
    private Double solarUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "cooking_fuel_type")
    private CookingFuelType cookingFuelType;

    @Column(name = "lpg_cylinders")
    private Double lpgCylinders;

    @Column(name = "png_units")
    private Double pngUnits;

    @Column(name = "biomass_kg_per_day")
    private Double biomassKgPerDay;

    @Column(name = "num_ac_units")
    private Integer numAcUnits;

    @Column(name = "ac_hours_per_day")
    private Double acHoursPerDay;

    @Column(name = "has_generator")
    @Builder.Default
    private Boolean hasGenerator = false;

    @Column(name = "generator_hours_per_month")
    private Double generatorHoursPerMonth;

    // ── TRANSPORT FIELDS — Both Household and MSME ─────────────

    @Column(name = "uses_public_transport")
    @Builder.Default
    private Boolean usesPublicTransport = false;

    @Column(name = "public_transport_km")
    private Double publicTransportKm;

    // ── LIFESTYLE FIELDS — Household Only ──────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_pattern")
    private DietaryPattern dietaryPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "shopping_orders_per_month")
    private ShoppingOrders shoppingOrdersPerMonth;

    // ── OPERATIONS FIELDS — MSME Only ──────────────────────────

    @Column(name = "has_commercial_vehicles")
    @Builder.Default
    private Boolean hasCommercialVehicles = false;

    @Column(name = "commercial_vehicle_km")
    private Double commercialVehicleKm;

    @Column(name = "third_party_shipments")
    private Integer thirdPartyShipments;

    @Column(name = "employees_private_vehicle")
    private Integer employeesPrivateVehicle;

    @Column(name = "employees_public_transport")
    private Integer employeesPublicTransport;

    @Column(name = "generator_liters_per_month")
    private Double generatorLitersPerMonth;

    @Column(name = "has_boiler")
    @Builder.Default
    private Boolean hasBoiler = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "boiler_fuel_type")
    private BoilerFuelType boilerFuelType;

    @Column(name = "boiler_coal_kg")
    private Double boilerCoalKg;

    @Column(name = "boiler_gas_scm")
    private Double boilerGasScm;

    @Column(name = "paper_reams_per_month")
    private Integer paperReamsPerMonth;

    @Column(name = "uses_recycled_paper")
    @Builder.Default
    private Boolean usesRecycledPaper = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "raw_material_type")
    private RawMaterialType rawMaterialType;

    @Column(name = "raw_material_kg")
    private Double rawMaterialKg;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── MAPPINGS ───────────────────────────────────────────────

    // Many declarations belong to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // One declaration has many vehicles
    @OneToMany(mappedBy = "declaration", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DeclarationVehicle> vehicles = new ArrayList<>();

    // One declaration has many agent assignments
    // (multiple if reassigned)
    @OneToMany(mappedBy = "declaration", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<AgentAssignment> agentAssignments = new ArrayList<>();

    // One declaration has one verification
    @OneToOne(mappedBy = "declaration", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private Verification verification;

    // One declaration produces one carbon score
    @OneToOne(mappedBy = "declaration", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private CarbonScore carbonScore;

    // ── ENUMS ──────────────────────────────────────────────────

    public enum DeclarationStatus {
        DRAFT, SUBMITTED, UNDER_VERIFICATION, VERIFIED, REJECTED
    }

    public enum CookingFuelType {
        LPG, PNG, ELECTRIC, BIOMASS, KEROSENE, NONE
    }

    public enum DietaryPattern {
        VEGAN, VEGETARIAN, EGGETARIAN, NON_VEGETARIAN, HEAVY_NON_VEGETARIAN
    }

    public enum ShoppingOrders {
        ZERO_TO_FIVE, SIX_TO_FIFTEEN, ABOVE_FIFTEEN
    }

    public enum BoilerFuelType {
        COAL, NATURAL_GAS, NONE
    }

    public enum RawMaterialType {
        VIRGIN, RECYCLED, MIXED
    }

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
