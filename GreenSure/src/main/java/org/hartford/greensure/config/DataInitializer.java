package org.hartford.greensure.config;

import org.hartford.greensure.entity.Agent;
import org.hartford.greensure.repository.AgentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AgentRepository agentRepository;

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
    }
}
