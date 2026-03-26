package org.hartford.greensure.config;

import org.hartford.greensure.entity.*;
import org.hartford.greensure.enums.Role;
import org.hartford.greensure.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Seeds default data on application startup.
 * Agent entity removed — Admin is now a User with role=ADMIN.
 * Agents are Users with role=AGENT, created via POST /admin/agents/create.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private PolicyRepository policyRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Seed default ADMIN user if none exists
        if (userRepository.findByEmail("admin@greensure.com").isEmpty()) {
            User admin = User.builder()
                    .role(Role.ADMIN)
                    .fullName("System Admin")
                    .email("admin@greensure.com")
                    .phone("9999999999")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .emailVerified(true)
                    .isActive(true)
                    .build();

            userRepository.save(admin);

            System.out.println("══════════════════════════════════════");
            System.out.println("  DEFAULT ADMIN CREATED");
            System.out.println("  Email:    admin@greensure.com");
            System.out.println("  Password: admin123");
            System.out.println("══════════════════════════════════════");
        }

        // Seed default field agent for development
        if (userRepository.findByEmail("agent@greensure.com").isEmpty()) {
            User agent = User.builder()
                    .role(Role.AGENT)
                    .fullName("Test Field Agent")
                    .email("agent@greensure.com")
                    .phone("8888888888")
                    .pinCode("400001")
                    .passwordHash(passwordEncoder.encode("agent123"))
                    .emailVerified(true)
                    .isActive(true)
                    .build();

            userRepository.save(agent);
            System.out.println("  DEFAULT AGENT CREATED: agent@greensure.com / agent123");
        }

        if (policyRepository.count() == 0) {
            seedPolicies();
        }
    }

    private void seedPolicies() {
        Policy homeShield = Policy.builder()
                .policyType("HOME_SHIELD")
                .name("Home Shield")
                .icon("🏠")
                .description("Complete protection for your home premises")
                .build();

        homeShield.setPlans(Arrays.asList(
                PolicyPlan.builder().planName("Basic").coverageAmount(500000.0).basePremiumYearly(4999.0)
                        .features(Arrays.asList("Fire and natural disaster coverage", "Burglary protection", "24x7 claim support"))
                        .policy(homeShield).build(),
                PolicyPlan.builder().planName("Standard").coverageAmount(1500000.0).basePremiumYearly(9999.0)
                        .features(Arrays.asList("Everything in Basic", "Flood and earthquake coverage", "Temporary accommodation benefit", "Personal belongings cover"))
                        .policy(homeShield).build(),
                PolicyPlan.builder().planName("Premium").coverageAmount(5000000.0).basePremiumYearly(19999.0)
                        .features(Arrays.asList("Everything in Standard", "Full reconstruction coverage", "Loss of rent benefit", "Dedicated claim manager"))
                        .policy(homeShield).build()
        ));

        Policy vehicleGuard = Policy.builder()
                .policyType("VEHICLE_GUARD")
                .name("Vehicle Guard")
                .icon("🚗")
                .description("Comprehensive coverage for all your declared vehicles")
                .build();

        vehicleGuard.setPlans(Arrays.asList(
                PolicyPlan.builder().planName("Basic").coverageAmount(500000.0).basePremiumYearly(2999.0)
                        .features(Arrays.asList("Third party liability", "Theft protection", "Basic own damage"))
                        .policy(vehicleGuard).build(),
                PolicyPlan.builder().planName("Standard").coverageAmount(1500000.0).basePremiumYearly(5999.0)
                        .features(Arrays.asList("Everything in Basic", "Full own damage coverage", "Zero depreciation", "Roadside assistance"))
                        .policy(vehicleGuard).build(),
                PolicyPlan.builder().planName("Premium").coverageAmount(2500000.0).basePremiumYearly(9999.0)
                        .features(Arrays.asList("Everything in Standard", "Engine protection", "Key replacement", "Return to invoice cover"))
                        .policy(vehicleGuard).build()
        ));

        policyRepository.saveAll(Arrays.asList(homeShield, vehicleGuard));
        System.out.println("Policies seeded successfully!");
    }
}
