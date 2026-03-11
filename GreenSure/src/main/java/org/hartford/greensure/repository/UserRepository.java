package org.hartford.greensure.repository;

import org.hartford.greensure.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used during LOGIN — find user by email
    Optional<User> findByEmail(String email);

    // Used during REGISTRATION — check if email already exists
    boolean existsByEmail(String email);

    // Used during REGISTRATION — check if mobile already exists
    boolean existsByMobile(String mobile);

    // Used during AGENT ASSIGNMENT — find all users in a pin code
    List<User> findByPinCode(String pinCode);

    // Used by ADMIN — filter users by type (HOUSEHOLD or MSME)
    Page<User> findByUserType(User.UserType userType, Pageable pageable);

    // Used by ADMIN — filter users by status (ACTIVE, SUSPENDED etc)
    Page<User> findByStatus(User.UserStatus status, Pageable pageable);

    // Used by ADMIN — filter users by type AND status
    Page<User> findByUserTypeAndStatus(User.UserType userType, User.UserStatus status, Pageable pageable);

    // Used by ADMIN — filter users by city
    List<User> findByCity(String city);

    // Used by RENEWAL ENGINE — find all active users
    // List<User> findByStatus(User.UserStatus status);

    // Used by ADMIN REPORTS — count users by type
    long countByUserType(User.UserType userType);

    // Used by ADMIN — search users by name or email
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchByNameOrEmail(@Param("keyword") String keyword);

    // Used by PASSWORD RESET — find user by reset token
    Optional<User> findByResetToken(String resetToken);
}
