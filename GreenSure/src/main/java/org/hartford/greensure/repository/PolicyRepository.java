package org.hartford.greensure.repository;

import org.hartford.greensure.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {
    List<Policy> findByEligibilityOrEligibilityIsNull(String eligibility);
}
