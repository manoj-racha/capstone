package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_policies")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PolicyPlan plan;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "final_price", nullable = false)
    private Double finalPrice;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;
}
