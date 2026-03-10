package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "declaration_vehicles")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclarationVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Column(name = "km_per_month", nullable = false)
    private Double kmPerMonth;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    // ── MAPPINGS ───────────────────────────────────────────────

    // Many vehicles belong to one declaration
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private CarbonDeclaration declaration;

    // One declared vehicle can have one verified vehicle correction
    @OneToMany(mappedBy = "declarationVehicle", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<VerifiedVehicle> verifiedVehicles = new ArrayList<>();

    // ── ENUMS ──────────────────────────────────────────────────

    public enum VehicleType {
        TWO_WHEELER, FOUR_WHEELER, COMMERCIAL
    }

    public enum FuelType {
        PETROL, DIESEL, CNG, ELECTRIC, NONE
    }
}
