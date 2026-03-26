package org.hartford.greensure.repository;

import org.hartford.greensure.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    List<VehicleDocument> findByVehicleVehicleDataId(Long vehicleId);
    
    List<VehicleDocument> findByVehicleDeclarationDeclarationId(Long declarationId);

    void deleteByVehicleVehicleDataId(Long vehicleId);
}
