package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "policies")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Policy {
    
    @Id
    @Column(name = "policy_type")
    private String policyType;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "icon")
    private String icon;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "eligibility")
    private String eligibility;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    private List<PolicyPlan> plans = new ArrayList<>();
}
