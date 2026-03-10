package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carbon_scores")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarbonScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @Column(name = "score_year", nullable = false)
    private Integer scoreYear;

    // ── CATEGORY SCORES ────────────────────────────────────────

    @Column(name = "energy_co2", nullable = false)
    private Double energyCo2;

    @Column(name = "transport_co2", nullable = false)
    private Double transportCo2;

    // Populated for Household users — null for MSME
    @Column(name = "lifestyle_co2")
    private Double lifestyleCo2;

    // Populated for MSME users — null for Household
    @Column(name = "operations_co2")
    private Double operationsCo2;

    // ── TOTAL SCORES ───────────────────────────────────────────

    @Column(name = "total_co2", nullable = false)
    private Double totalCo2;

    // Per person for Household, per employee for MSME
    @Column(name = "per_capita_co2", nullable = false)
    private Double perCapitaCo2;

    // ── ZONE CLASSIFICATION ────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "zone", nullable = false)
    private CarbonZone zone;

    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;

    // ── MAPPINGS ───────────────────────────────────────────────

    // Many scores belong to one user (one per year)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // One score is generated from one declaration
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false, unique = true)
    private CarbonDeclaration declaration;

    // One score produces many recommendations
    @OneToMany(mappedBy = "score", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<Recommendation> recommendations = new ArrayList<>();

    // ── ENUM ───────────────────────────────────────────────────

    public enum CarbonZone {
        GREEN_CHAMPION, GREEN_IMPROVER, GREEN_DEFAULTER
    }

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.generatedAt = LocalDateTime.now();
    }
}
