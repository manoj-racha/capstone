package org.hartford.greensure.config;

import org.hartford.greensure.entity.Agent;
import org.hartford.greensure.entity.Policy;
import org.hartford.greensure.entity.PolicyPlan;
import org.hartford.greensure.repository.AgentRepository;
import org.hartford.greensure.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Only seed if no admin exists
        if (agentRepository.findByEmail("admin@greensure.com").isEmpty()) {

            Agent admin = Agent.builder()
                    .agentType(Agent.AgentType.ADMIN)
                    .fullName("System Admin")
                    .email("admin@greensure.com")
                    .mobile("9999999999")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .employeeId("EMP-ADMIN-001")
                    .assignedZones("ALL")
                    .build();

            agentRepository.save(admin);

            System.out.println("══════════════════════════════════════");
            System.out.println("  DEFAULT ADMIN CREATED");
            System.out.println("  Email:    admin@greensure.com");
            System.out.println("  Password: admin123");
            System.out.println("══════════════════════════════════════");
        }

        if (policyRepository.count() == 0) {
            seedPolicies();
        }
    }

    private void seedPolicies() {
        // HOME_SHIELD
        Policy homeShield = Policy.builder()
                .policyType("HOME_SHIELD")
                .name("Home Shield")
                .icon("🏠")
                .description("Complete protection for your home or business premises")
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

        // VEHICLE_GUARD
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

        // BUSINESS_PROTECT
        Policy businessProtect = Policy.builder()
                .policyType("BUSINESS_PROTECT")
                .name("Business Protect")
                .icon("🏭")
                .description("Protect your MSME operations against unexpected losses")
                .eligibility("MSME")
                .build();
        
        businessProtect.setPlans(Arrays.asList(
                PolicyPlan.builder().planName("Basic").coverageAmount(1000000.0).basePremiumYearly(9999.0)
                        .features(Arrays.asList("Equipment breakdown cover", "Fire and theft protection", "Basic liability coverage"))
                        .policy(businessProtect).build(),
                PolicyPlan.builder().planName("Standard").coverageAmount(5000000.0).basePremiumYearly(19999.0)
                        .features(Arrays.asList("Everything in Basic", "Business interruption cover", "Employee liability", "Inventory protection"))
                        .policy(businessProtect).build(),
                PolicyPlan.builder().planName("Premium").coverageAmount(10000000.0).basePremiumYearly(34999.0)
                        .features(Arrays.asList("Everything in Standard", "Directors liability", "Cyber risk cover", "Export credit insurance"))
                        .policy(businessProtect).build()
        ));

        policyRepository.saveAll(Arrays.asList(homeShield, vehicleGuard, businessProtect));
        System.out.println("Policies seeded successfully!");
    }
}
