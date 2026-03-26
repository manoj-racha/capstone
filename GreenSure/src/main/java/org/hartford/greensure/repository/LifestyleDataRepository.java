package org.hartford.greensure.repository;

import org.hartford.greensure.entity.LifestyleData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LifestyleDataRepository extends JpaRepository<LifestyleData, Long> {
    Optional<LifestyleData> findByDeclarationDeclarationId(Long declarationId);
    boolean existsByDeclarationDeclarationId(Long declarationId);
}
