package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hartford.greensure.enums.CookingFuel;

import java.time.LocalDateTime;

/**
 * Module 6 — Cooking fuel data for a carbon declaration.
 *
 * Supports four fuel types: LPG, PNG, ELECTRIC, BIOGAS.
 * LPG users declare annual cylinder count; PNG users supply a consumer number.
 * Agent-corrected values override user values in carbon calculation.
 *
 * getEffectiveFuelType() and getEffectiveCylinders() always return
 * the agent-corrected value when set, falling back to user-declared.
 */
@Entity
@Table(name = "cooking_data")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CookingData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cooking_data_id")
    private Long cookingDataId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false, unique = true)
    private CarbonDeclaration declaration;

    // ── USER DECLARED VALUES ───────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type")
    private CookingFuel fuelType;

    /** PNG consumer number — required only if fuelType = PNG. */
    @Column(name = "png_consumer_number", length = 30)
    private String pngConsumerNumber;

    /** Annual LPG cylinders declared by user — required only if fuelType = LPG. */
    @Column(name = "user_declared_cylinders")
    private Integer userDeclaredCylinders;

    /** Annual LPG cylinders extracted by OCR from uploaded receipts. */
    @Column(name = "ocr_computed_cylinders")
    private Integer ocrComputedCylinders;

    /**
     * JSON array of uploaded bill/receipt URLs.
     * Stored as TEXT to avoid a separate join table for a capstone project.
     */
    @Column(name = "bill_urls", columnDefinition = "TEXT")
    private String billUrls;

    // ── AGENT CORRECTED VALUES ─────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_corrected_fuel_type")
    private CookingFuel agentCorrectedFuelType;

    @Column(name = "agent_corrected_cylinders")
    private Integer agentCorrectedCylinders;

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

    public CookingFuel getEffectiveFuelType() {
        return agentCorrectedFuelType != null ? agentCorrectedFuelType : fuelType;
    }

    /**
     * Priority: agent-corrected → OCR computed → user-declared.
     */
    public Integer getEffectiveCylinders() {
        if (agentCorrectedCylinders != null) return agentCorrectedCylinders;
        if (ocrComputedCylinders    != null) return ocrComputedCylinders;
        return userDeclaredCylinders;
    }
}
