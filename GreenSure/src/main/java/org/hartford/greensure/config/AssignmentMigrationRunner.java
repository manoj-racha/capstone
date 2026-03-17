package org.hartford.greensure.config;

import org.hartford.greensure.repository.AgentAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AssignmentMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AssignmentMigrationRunner.class);

    private final AgentAssignmentRepository assignmentRepository;

    public AssignmentMigrationRunner(AgentAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public void run(String... args) {
        int assignedByUpdated = assignmentRepository.backfillAssignedBy();
        int lifecycleUpdated = assignmentRepository.backfillAssignmentStatus();

        if (assignedByUpdated > 0 || lifecycleUpdated > 0) {
            log.info("Backfilled assignment fields: assignedBy={}, assignmentStatus={}",
                    assignedByUpdated, lifecycleUpdated);
        }
    }
}
