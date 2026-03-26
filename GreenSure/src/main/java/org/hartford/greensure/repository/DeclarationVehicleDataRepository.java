package org.hartford.greensure.repository;

import org.hartford.greensure.entity.DeclarationVehicleData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
public interface DeclarationVehicleDataRepository
        extends JpaRepository<DeclarationVehicleData, Long> {

    List<DeclarationVehicleData> findByDeclarationDeclarationId(Long declarationId);

    Optional<DeclarationVehicleData> findByVehicleDataIdAndDeclarationDeclarationId(Long vehicleId, Long declarationId);

    void deleteByVehicleDataIdAndDeclarationDeclarationId(Long vehicleId, Long declarationId);
    
    boolean existsByDeclarationDeclarationId(Long declarationId);
}
