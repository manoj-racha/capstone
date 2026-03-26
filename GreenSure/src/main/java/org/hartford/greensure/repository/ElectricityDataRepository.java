package org.hartford.greensure.repository;

import org.hartford.greensure.entity.ElectricityData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ElectricityDataRepository extends JpaRepository<ElectricityData, Long> {

    Optional<ElectricityData> findByDeclarationDeclarationId(Long declarationId);
    boolean existsByDeclarationDeclarationId(Long declarationId);
}
