package org.hartford.greensure.repository;



import org.hartford.greensure.entity.DeclarationVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeclarationVehicleRepository
        extends JpaRepository<DeclarationVehicle, Long> {

    // Used by CARBON CALCULATION ENGINE — get all vehicles
    // for a declaration to compute total transport CO2
    // This is called every time a score is generated
    List<DeclarationVehicle> findByDeclarationDeclarationId(
            Long declarationId);

    // Used by DECLARATION FORM — count how many vehicles
    // a user has already added to their current declaration
    long countByDeclarationDeclarationId(Long declarationId);

    // Used by AGENT VERIFICATION — get all vehicles of a
    // specific type for a declaration
    // e.g. agent wants to see only FOUR_WHEELER entries
    List<DeclarationVehicle> findByDeclarationDeclarationIdAndVehicleType(
            Long declarationId, DeclarationVehicle.VehicleType vehicleType);

    // Used by AGENT VERIFICATION — get all vehicles of a
    // specific fuel type for a declaration
    // e.g. agent wants to see all ELECTRIC vehicles declared
    List<DeclarationVehicle> findByDeclarationDeclarationIdAndFuelType(
            Long declarationId, DeclarationVehicle.FuelType fuelType);

    // Used by ADMIN REPORTS — find all declarations that
    // contain at least one electric vehicle
    // Useful for green behavior analytics
    @Query("SELECT DISTINCT v.declaration.declarationId " +
            "FROM DeclarationVehicle v " +
            "WHERE v.fuelType = 'ELECTRIC'")
    List<Long> findDeclarationIdsWithElectricVehicles();

    // Used by ADMIN REPORTS — count total vehicles by fuel type
    // across entire platform
    // e.g. how many petrol vs electric vehicles are declared
    long countByFuelType(DeclarationVehicle.FuelType fuelType);

    // Used by ADMIN REPORTS — count total vehicles by vehicle type
    // across entire platform
    long countByVehicleType(DeclarationVehicle.VehicleType vehicleType);

    // Used by CALCULATION ENGINE — get total km driven per month
    // across all vehicles in a declaration
    // Summed and used for aggregate transport calculation
    @Query("SELECT SUM(v.kmPerMonth * v.quantity) " +
            "FROM DeclarationVehicle v " +
            "WHERE v.declaration.declarationId = :declarationId " +
            "AND v.fuelType = :fuelType")
    Double sumKmByDeclarationAndFuelType(
            @Param("declarationId") Long declarationId,
            @Param("fuelType") DeclarationVehicle.FuelType fuelType);
}
