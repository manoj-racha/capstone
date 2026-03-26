package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hartford.greensure.enums.VerificationOutcome;

import java.time.LocalDateTime;

/**
 * Records the outcome of a field agent's physical visit to verify a declaration.
 *
 * Linked to an AgentAssignment (one assignment → one verification).
 * Agent-corrected field values are stored on the respective module entities
 * (DeclarationVehicleData, ElectricityData, CookingData, SolarData) so that
 * getEffective*() methods automatically use corrected values in the carbon
 * score calculation.
 *
 * This entity stores: the overall outcome, GPS location at action time,
 * agent notes, and uploaded proof document URLs.
 */
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

    // ── AGENT DECISION ─────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private VerificationOutcome outcome;

    /**
     * Mandatory written reason when outcome = REJECTED.
     * Optional general notes for CONFIRMED / MODIFIED.
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** General agent notes written during the physical visit. */
    @Column(name = "agent_notes", columnDefinition = "TEXT")
    private String agentNotes;

    // ── GPS ────────────────────────────────────────────────────

    /** Agent GPS latitude at the time they submitted the verification action. */
    @Column(name = "gps_lat")
    private Double gpsLat;

    /** Agent GPS longitude at the time they submitted the verification action. */
    @Column(name = "gps_lng")
    private Double gpsLng;

    /**
     * JSON array of uploaded proof photo / document URLs.
     * Stored as TEXT to avoid a join table in this capstone project.
     * Example: ["https://cdn.example.com/proof1.jpg", "..."]
     */
    @Column(name = "document_urls", columnDefinition = "TEXT")
    private String documentUrls;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // ── RELATIONS ──────────────────────────────────────────────

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false, unique = true)
    private AgentAssignment assignment;

    /** The field agent who performed the verification. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    /** The declaration that was verified. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private CarbonDeclaration declaration;

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.verifiedAt = LocalDateTime.now();
    }
}
