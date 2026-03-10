package org.hartford.greensure.repository;

import org.hartford.greensure.entity.MsmeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MsmeProfileRepository
        extends JpaRepository<MsmeProfile, Long> {

    // Used during DECLARATION — get MSME profile by user
    // to fetch number of employees for per employee score calculation
    Optional<MsmeProfile> findByUserUserId(Long userId);

    // Used during REGISTRATION CHECK — does this user already
    // have an MSME profile?
    boolean existsByUserUserId(Long userId);

    // Used during REGISTRATION — check if GST number already exists
    // GST number must be unique across all businesses
    boolean existsByGstNumber(String gstNumber);

    // Used by ADMIN — find a business by their GST number
    Optional<MsmeProfile> findByGstNumber(String gstNumber);

    // Used by ADMIN REPORTS — filter businesses by type
    // e.g. show all MANUFACTURING businesses
    List<MsmeProfile> findByBusinessType(MsmeProfile.BusinessType businessType);

    // Used by ADMIN REPORTS — find large businesses
    // with more than given number of employees
    List<MsmeProfile> findByNumEmployeesGreaterThan(Integer count);

    // Used by ADMIN REPORTS — find small businesses
    // with fewer than or equal to given number of employees
    List<MsmeProfile> findByNumEmployeesLessThanEqual(Integer count);

    // Used by ADMIN REPORTS — count businesses by type
    // e.g. how many MANUFACTURING vs RETAIL on platform
    long countByBusinessType(MsmeProfile.BusinessType businessType);

    // Used by ADMIN — search businesses by name
    @Query("SELECT m FROM MsmeProfile m WHERE " +
            "LOWER(m.businessName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<MsmeProfile> searchByBusinessName(
            @Param("keyword") String keyword);
}
