package org.hartford.greensure.repository;

import org.hartford.greensure.entity.HouseholdProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HouseholdProfileRepository
        extends JpaRepository<HouseholdProfile, Long> {

    // Used during DECLARATION — get household profile by user
    // to fetch number of members for per capita score calculation
    Optional<HouseholdProfile> findByUserUserId(Long userId);

    // Used during REGISTRATION CHECK — does this user already
    // have a household profile?
    boolean existsByUserUserId(Long userId);

    // Used by ADMIN REPORTS — find all households by dwelling type
    List<HouseholdProfile> findByDwellingType(HouseholdProfile.DwellingType dwellingType);

    // Used by ADMIN REPORTS — find households by member count
    // e.g. find all large families with more than 5 members
    List<HouseholdProfile> findByNumberOfMembersGreaterThan(
            Integer count);
}
