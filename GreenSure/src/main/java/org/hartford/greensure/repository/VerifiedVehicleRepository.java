package org.hartford.greensure.repository;




import org.hartford.greensure.entity.VerifiedVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VerifiedVehicleRepository
        extends JpaRepository<VerifiedVehicle, Long> {

    // Used by CALCULATION ENGINE — find if agent corrected
    // a specific declared vehicle
    Optional<VerifiedVehicle> findByDeclarationVehicleVehicleId(
            Long vehicleId);

    // Used by VERIFICATION FLOW — get all corrected vehicles
    // for a specific verification
    java.util.List<VerifiedVehicle> findByVerificationVerificationId(
            Long verificationId);
}
