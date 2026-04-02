package org.hartford.greensure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Ensures {@code electricity_bills.ai_anomaly_flag} exists for H2/dev DBs where
 * {@code ddl-auto=update} did not run (e.g. app not restarted after pulling changes).
 */
@Configuration
@DependsOn("entityManagerFactory")
@Slf4j
public class ElectricityBillSchemaCompatibility implements InitializingBean {

    private final DataSource dataSource;
    private final Environment environment;

    public ElectricityBillSchemaCompatibility(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String url = environment.getProperty("spring.datasource.url", "");
        if (!url.contains(":h2:") && !url.toLowerCase().contains("h2")) {
            return;
        }
        String[] variants =
                new String[] {
                    "ALTER TABLE electricity_bills ADD COLUMN IF NOT EXISTS ai_anomaly_flag BOOLEAN DEFAULT FALSE",
                    "ALTER TABLE ELECTRICITY_BILLS ADD COLUMN IF NOT EXISTS AI_ANOMALY_FLAG BOOLEAN DEFAULT FALSE"
                };
        try (Connection conn = dataSource.getConnection()) {
            boolean applied = false;
            for (String sql : variants) {
                try (Statement st = conn.createStatement()) {
                    st.execute(sql);
                    applied = true;
                    break;
                } catch (Exception ignored) {
                    // try next variant (identifier quoting differs by DDL history)
                }
            }
            if (applied) {
                log.info("Schema check: electricity_bills.ai_anomaly_flag ensured");
                String[] backfill =
                        new String[] {
                            "UPDATE electricity_bills SET ai_anomaly_flag = FALSE WHERE ai_anomaly_flag IS NULL",
                            "UPDATE ELECTRICITY_BILLS SET AI_ANOMALY_FLAG = FALSE WHERE AI_ANOMALY_FLAG IS NULL"
                        };
                for (String sql : backfill) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate(sql);
                        break;
                    } catch (Exception ignored) {
                        // try next identifier variant
                    }
                }
            }
        } catch (Exception e) {
            log.warn(
                    "Could not ensure ai_anomaly_flag column (use a fresh H2 file or ddl-auto=update): {}",
                    e.getMessage());
        }
    }
}
