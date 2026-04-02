package org.hartford.greensure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Household profile for a USER-role account.
 * Contains census-level information about the household.
 * This entity is created at the time the user saves Module 2
 * (Household Data) in their first declaration.
 * It is shared across declarations (the same household profile
 * is reused for subsequent years — only numberOfMembers may change).
 */
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Number of people living in the household (1–20). */
    @Column(name = "number_of_members", nullable = false)
    private Integer numberOfMembers;

    @Enumerated(EnumType.STRING)
    @Column(name = "dwelling_type")
    private DwellingType dwellingType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum DwellingType {
        APARTMENT, INDEPENDENT_HOUSE, VILLA, OTHER
    }
}
