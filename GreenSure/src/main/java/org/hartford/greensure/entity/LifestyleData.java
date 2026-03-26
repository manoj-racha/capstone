package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hartford.greensure.enums.PublicTransportUsage;

import java.time.LocalDateTime;

/**
 * Module 7 — Optional lifestyle data providing bonus discounts.
 *
 * This module is SELF-DECLARED and NOT physically verified by the agent.
 * Its values contribute bonus reductions to the total CO₂ score:
 *   - publicTransportUsage: reduces vehicle CO₂ by a percentage
 *   - wastesRecycling: flat 50 kg CO₂ reduction
 *
 * Because this module is optional, the declaration can be submitted
 * without a LifestyleData record. When absent, lifestyle bonuses = 0.
 */
@Entity
@Table(name = "lifestyle_data")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LifestyleData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lifestyle_data_id")
    private Long lifestyleDataId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false, unique = true)
    private CarbonDeclaration declaration;

    /**
     * How often the household uses public transport instead of personal vehicles.
     * Maps to a percentage reduction applied to vehicleCo2 in CarbonScoreService.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "public_transport_usage")
    @Builder.Default
    private PublicTransportUsage publicTransportUsage = PublicTransportUsage.NEVER;

    /**
     * Whether the household participates in waste recycling.
     * Grants a flat 50 kg CO₂/year reduction.
     */
    @Column(name = "wastes_recycling")
    @Builder.Default
    private boolean wastesRecycling = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
