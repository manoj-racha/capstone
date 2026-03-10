package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "msme_profiles")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MsmeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(name = "gst_number", nullable = false, unique = true, length = 20)
    private String gstNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false)
    private BusinessType businessType;

    @Column(name = "num_employees", nullable = false)
    private Integer numEmployees;

    // ── MAPPINGS ───────────────────────────────────────────────

    // One-to-one with User — owner side
    // msme_profiles table holds the user_id foreign key
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ── ENUM ───────────────────────────────────────────────────

    public enum BusinessType {
        MANUFACTURING, RETAIL, SERVICE, FOOD
    }
}
