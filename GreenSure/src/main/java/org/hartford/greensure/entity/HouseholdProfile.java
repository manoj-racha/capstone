package org.hartford.greensure.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "household_profiles")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseholdProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "number_of_members", nullable = false)
    private Integer numberOfMembers;

    @Enumerated(EnumType.STRING)
    @Column(name = "dwelling_type", nullable = false)
    private DwellingType dwellingType;

    // ── MAPPINGS ───────────────────────────────────────────────

    // One-to-one with User — owner side
    // household_profiles table holds the user_id foreign key
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ── ENUM ───────────────────────────────────────────────────

    public enum DwellingType {
        APARTMENT, INDEPENDENT_HOUSE
    }
}
