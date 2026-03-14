package org.hartford.greensure.repository;

import org.hartford.greensure.entity.UserPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPolicyRepository extends JpaRepository<UserPolicy, Long> {
    List<UserPolicy> findByUserUserIdOrderByPurchasedAtDesc(Long userId);
}
