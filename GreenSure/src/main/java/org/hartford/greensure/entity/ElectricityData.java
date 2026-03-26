package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Module 4a — Electricity consumption data for a carbon declaration.
 *
 * Three sources of the monthly kWh figure exist:
 * 1. userDeclaredMonthlyKwh — what the user typed in Module 4
 * 2. ocrComputedMonthlyKwh  — average extracted from uploaded bills via OCR
 * 3. agentCorrectedMonthlyKwh — value the agent entered after physical visit
 *
 * getEffectiveMonthlyKwh() picks in priority order:
 *   agent-corrected → OCR-computed → user-declared
 * This ensures the most trustworthy value flows into CarbonScoreService.
 */
@Entity
@Table(name = "electricity_data")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectricityData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "electricity_data_id")
    private Long electricityDataId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false, unique = true)
    private CarbonDeclaration declaration;

    // ── USER DECLARED VALUES ───────────────────────────────────

    @Column(name = "provider", length = 100)
    private String provider;

    @Column(name = "consumer_number", length = 30)
    private String consumerNumber;

    /** Monthly kWh as typed by the user. */
    @Column(name = "user_declared_monthly_kwh")
    private Double userDeclaredMonthlyKwh;

    // ── OCR EXTRACTED VALUES ───────────────────────────────────

    /**
     * Average monthly kWh computed from OCR extraction of uploaded bills.
     * Updated by addElectricityBill() in DeclarationModuleService each
     * time a new bill is processed.
     */
    @Column(name = "ocr_computed_monthly_kwh")
    private Double ocrComputedMonthlyKwh;

    /** Number of bill months for which OCR extraction succeeded. */
    @Column(name = "bills_uploaded")
    @Builder.Default
    private Integer billsUploaded = 0;

    // ── AGENT CORRECTED VALUES ─────────────────────────────────

    /** Agent's override of monthly kWh — null if agent accepted OCR/user value. */
    @Column(name = "agent_corrected_monthly_kwh")
    private Double agentCorrectedMonthlyKwh;

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
     * Priority: agentCorrected → ocrComputed → userDeclared.
     */
    public Double getEffectiveMonthlyKwh() {
        if (agentCorrectedMonthlyKwh != null) return agentCorrectedMonthlyKwh;
        if (ocrComputedMonthlyKwh    != null) return ocrComputedMonthlyKwh;
        return userDeclaredMonthlyKwh != null ? userDeclaredMonthlyKwh : 0.0;
    }
}
