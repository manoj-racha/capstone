package org.hartford.greensure.repository;

import org.hartford.greensure.entity.SolarData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolarDataRepository extends JpaRepository<SolarData, Long> {
    Optional<SolarData> findByDeclarationDeclarationId(Long declarationId);
    boolean existsByDeclarationDeclarationId(Long declarationId);
}
