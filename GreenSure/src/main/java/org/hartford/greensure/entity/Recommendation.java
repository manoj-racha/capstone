package org.hartford.greensure.entity;



import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long recommendationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private RecommendationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private RecommendationPriority priority;

    @Column(name = "recommendation_text", nullable = false, columnDefinition = "TEXT")
    private String recommendationText;

    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;

    // ── MAPPINGS ───────────────────────────────────────────────

    // Many recommendations belong to one carbon score
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "score_id", nullable = false)
    private CarbonScore score;

    // Many recommendations belong to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── ENUMS ──────────────────────────────────────────────────

    public enum RecommendationCategory {
        ENERGY, TRANSPORT, LIFESTYLE, OPERATIONS
    }

    public enum RecommendationPriority {
        HIGH, MEDIUM, LOW
    }

    // ── LIFECYCLE ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.generatedAt = LocalDateTime.now();
    }
}
