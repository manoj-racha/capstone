package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Module 5 — Solar installation data (optional module).
 *
 * If hasSolar = false, this record may still be created with hasSolar = false
 * so the form step is recorded as "seen and skipped".
 * If hasSolar = true, the declared capacityKw generates a CO₂ offset in the
 * carbon calculation — but only if mnreVerified = true OR a certificate was uploaded.
 *
 * Agent can physically confirm solar installation and correct the kW figure.
 */
@Entity
@Table(name = "solar_data")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolarData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "solar_data_id")
    private Long solarDataId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false, unique = true)
    private CarbonDeclaration declaration;

    // ── USER DECLARED VALUES ───────────────────────────────────

    @Column(name = "has_solar", nullable = false)
    @Builder.Default
    private boolean hasSolar = false;

    /** Installed capacity in kilowatts. */
    @Column(name = "capacity_kw")
    private Double capacityKw;

    /** URL of uploaded MNRE/installer certificate. */
    @Column(name = "certificate_url", columnDefinition = "TEXT")
    private String certificateUrl;

    /**
     * True if the system successfully verified the installation against
     * the MNRE database (or equivalent). Set false by default; updated
     * by the backend verification integration.
     */
    @Column(name = "mnre_verified")
    @Builder.Default
    private boolean mnreVerified = false;

    // ── AGENT CORRECTED VALUES ─────────────────────────────────

    /** Agent's corrected capacity — null if agent accepted the declared value. */
    @Column(name = "agent_corrected_capacity_kw")
    private Double agentCorrectedCapacityKw;

    /** True if the agent physically saw and confirmed the solar installation. */
    @Column(name = "agent_verified_solar")
    @Builder.Default
    private boolean agentVerifiedSolar = false;

    @Column(name = "agent_correction_note", columnDefinition = "TEXT")
    private String agentCorrectionNote;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── EFFECTIVE VALUE METHOD ─────────────────────────────────

    /**
     * Returns the kW value used in the solar offset calculation.
     * Agent-corrected value wins over user-declared value.
     */
    public Double getEffectiveCapacityKw() {
        return agentCorrectedCapacityKw != null ? agentCorrectedCapacityKw : capacityKw;
    }
}
