package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hartford.greensure.enums.DataSource;
import org.hartford.greensure.enums.FuelType;
import org.hartford.greensure.enums.MileageBand;

import java.time.LocalDateTime;

/**
 * Module 3 — Vehicle data for a carbon declaration.
 *
 * Stores both user-declared values and agent-corrected values as separate
 * fields. The getEffective*() methods always return the agent-corrected
 * value when present, falling back to the user-declared value.
 * This design provides a full audit trail: both the user's original
 * claim and the agent's on-site correction are preserved.
 *
 * One declaration has exactly one vehicle entry (simplified for this
 * capstone — a future version could support multi-vehicle households).
 */
@Entity
@Table(name = "declaration_vehicle_data")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclarationVehicleData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_data_id")
    private Long vehicleDataId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private CarbonDeclaration declaration;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_category")
    private org.hartford.greensure.enums.VehicleCategory vehicleCategory;

    @Column(name = "vehicle_nickname")
    private String vehicleNickname;

    // ── USER DECLARED VALUES ───────────────────────────────────

    /** Vehicle Identification Number — 17 characters. */
    @Column(name = "vin", length = 17)
    private String vin;

    @Column(name = "registration_number", length = 20)
    private String registrationNumber;

    @Column(name = "make", length = 60)
    private String make;

    @Column(name = "model", length = 60)
    private String model;

    @Column(name = "manufacturing_year")
    private Integer manufacturingYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type")
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "mileage_band")
    private MileageBand mileageBand;

    /**
     * Indicates how the vehicle data was sourced.
     * VAHAN/DIGILOCKER = system-verified; MANUAL = agent must cross-check.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source")
    @Builder.Default
    private DataSource dataSource = DataSource.MANUAL;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private java.util.List<VehicleDocument> documents = new java.util.ArrayList<>();

    // ── AGENT CORRECTED VALUES ─────────────────────────────────

    /** Agent's corrected fuel type — null if agent left it unchanged. */
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_corrected_fuel_type")
    private FuelType agentCorrectedFuelType;

    /** Agent's corrected mileage band — null if agent left it unchanged. */
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_corrected_mileage_band")
    private MileageBand agentCorrectedMileageBand;

    /** Free-text note explaining any agent corrections. */
    @Column(name = "agent_correction_note", columnDefinition = "TEXT")
    private String agentCorrectionNote;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── EFFECTIVE VALUE METHODS ────────────────────────────────

    /**
     * Returns the fuel type used in carbon calculation.
     * Agent-corrected value wins over user-declared value.
     */
    public FuelType getEffectiveFuelType() {
        return agentCorrectedFuelType != null ? agentCorrectedFuelType : fuelType;
    }

    /**
     * Returns the mileage band used in carbon calculation.
     * Agent-corrected value wins over user-declared value.
     */
    public MileageBand getEffectiveMileageBand() {
        return agentCorrectedMileageBand != null ? agentCorrectedMileageBand : mileageBand;
    }
}
