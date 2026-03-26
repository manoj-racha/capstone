package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hartford.greensure.enums.DeclarationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one annual carbon footprint declaration submitted by a household user.
 * The declaration is a shell / header record. All actual data lives in
 * six child module entities linked via OneToOne / OneToMany:
 * <ul>
 *   <li>Module 2 — HouseholdProfile (shared with the user account)</li>
 *   <li>Module 3 — DeclarationVehicleData</li>
 *   <li>Module 4 — ElectricityData + ElectricityBill (list)</li>
 *   <li>Module 5 — SolarData</li>
 *   <li>Module 6 — CookingData</li>
 *   <li>Module 7 — LifestyleData</li>
 * </ul>
 *
 * Fraud advisory fields are populated at submission time and
 * are visible only to field agents — never exposed to the user.
 */
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

    /** How many times the user has resubmitted after rejection. Max 3. */
    @Column(name = "resubmission_count")
    @Builder.Default
    private Integer resubmissionCount = 0;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── FRAUD ADVISORY — agent-visible only ───────────────────

    /**
     * Integer score computed by FraudAdvisoryService at submission time.
     * 0–1 = LOW, 2–3 = MEDIUM, 4+ = HIGH.
     * NEVER exposed to the user via API.
     */
    @Column(name = "fraud_advisory_score")
    private Integer fraudAdvisoryScore;

    /**
     * Comma-separated list of fraud rule names that fired.
     * Example: "MANUAL_VEHICLE_NO_DOCUMENT,SOLAR_CLAIMED_NO_CERTIFICATE"
     */
    @Column(name = "fraud_advisory_flags", columnDefinition = "TEXT")
    private String fraudAdvisoryFlags;

    /**
     * One of "LOW", "MEDIUM", "HIGH".
     * Shown to the agent in the verification workspace.
     */
    @Column(name = "fraud_risk_level", length = 10)
    private String fraudRiskLevel;

    // ── USER RELATION ──────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── MODULE RELATIONS ───────────────────────────────────────

    /** Module 3 — vehicle data (multiple vehicles per declaration). */
    @OneToMany(mappedBy = "declaration", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<DeclarationVehicleData> vehicles = new ArrayList<>();

    /** Module 4a — electricity summary. */
    @OneToOne(mappedBy = "declaration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ElectricityData electricityData;

    /** Module 4b — individual monthly bill OCR records. */
    @OneToMany(mappedBy = "declaration", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ElectricityBill> electricityBills = new ArrayList<>();

    /** Module 5 — solar installation (optional). */
    @OneToOne(mappedBy = "declaration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SolarData solarData;

    /** Module 6 — cooking fuel. */
    @OneToOne(mappedBy = "declaration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CookingData cookingData;

    /** Module 7 — lifestyle self-declaration (optional). */
    @OneToOne(mappedBy = "declaration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private LifestyleData lifestyleData;

    // ── OTHER RELATIONS ────────────────────────────────────────

    @OneToMany(mappedBy = "declaration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AgentAssignment> agentAssignments = new ArrayList<>();

    @OneToOne(mappedBy = "declaration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Verification verification;

    @OneToOne(mappedBy = "declaration", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CarbonScore carbonScore;

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
