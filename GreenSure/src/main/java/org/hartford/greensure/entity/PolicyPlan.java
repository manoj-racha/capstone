package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "policy_plans")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PolicyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long planId;

    private String planName;
    private Double coverageAmount;
    private Double basePremiumYearly;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "policy_plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "feature")
    private List<String> features;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_type", nullable = false)
    private Policy policy;
}
