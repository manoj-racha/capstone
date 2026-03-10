package org.hartford.greensure.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "verified_vehicles")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifiedVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verified_vehicle_id")
    private Long verifiedVehicleId;

    // null = agent confirmed declared fuel type
    // non-null = agent corrected it to this fuel type
    @Enumerated(EnumType.STRING)
    @Column(name = "corrected_fuel_type")
    private DeclarationVehicle.FuelType correctedFuelType;

    // null = agent confirmed declared km
    // non-null = agent corrected it to this value
    @Column(name = "corrected_km")
    private Double correctedKm;

    // ── MAPPINGS ───────────────────────────────────────────────

    // Many verified vehicles belong to one verification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id", nullable = false)
    private Verification verification;

    // Each verified vehicle references the original declared vehicle
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private DeclarationVehicle declarationVehicle;
}