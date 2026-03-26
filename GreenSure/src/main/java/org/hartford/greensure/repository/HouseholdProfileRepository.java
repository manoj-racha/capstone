package org.hartford.greensure.repository;

import org.hartford.greensure.entity.HouseholdProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HouseholdProfileRepository extends JpaRepository<HouseholdProfile, Long> {
    Optional<HouseholdProfile> findByUserUserId(Long userId);
    boolean existsByUserUserId(Long userId);
}
