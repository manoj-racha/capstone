package org.hartford.greensure.repository;

import org.hartford.greensure.entity.ElectricityBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ElectricityBillRepository extends JpaRepository<ElectricityBill, Long> {

    List<ElectricityBill> findByDeclarationDeclarationIdOrderByBillingMonthDesc(Long declarationId);

    long countByDeclarationDeclarationId(Long declarationId);

    /**
     * Computes the average monthly kWh across all uploaded bills for a declaration.
     * Called after each new bill is added to update ElectricityData.ocrComputedMonthlyKwh.
     */
    @Query("""
        SELECT AVG(b.unitsKwh) FROM ElectricityBill b
        WHERE b.declaration.declarationId = :declarationId
    """)
    Optional<Double> calculateAverageKwh(@Param("declarationId") Long declarationId);
}
