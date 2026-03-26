package org.hartford.greensure.repository;

import org.hartford.greensure.entity.CookingData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CookingDataRepository extends JpaRepository<CookingData, Long> {
    Optional<CookingData> findByDeclarationDeclarationId(Long declarationId);
    boolean existsByDeclarationDeclarationId(Long declarationId);
}
