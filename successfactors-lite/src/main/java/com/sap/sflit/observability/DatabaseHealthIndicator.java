package com.sap.sflit.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Custom Health Indicator for Actuator
 * 
 * INTERVIEW TALK TRACK:
 * "While Spring Boot provides default health checks, I implemented a custom `HealthIndicator` 
 * to specifically validate that our PostgreSQL instance is actively accepting queries. 
 * In a real SAP production environment, Kubernetes or Cloud Foundry uses this `/actuator/health` 
 * endpoint to determine if it should route traffic to this pod or restart it."
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Execute a lightweight query just to verify SQL connection is alive
            stmt.execute("SELECT 1");
            long latency = System.currentTimeMillis() - start;

            // If the query takes too long, we can optionally mark it as "degraded"
            if (latency > 500) {
                return Health.status("DEGRADED")
                        .withDetail("Database", "PostgreSQL is slow")
                        .withDetail("Latency_ms", latency)
                        .build();
            }

            return Health.up()
                    .withDetail("Database", "PostgreSQL is rock solid")
                    .withDetail("Latency_ms", latency)
                    .build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("Database", "Connection FAILED")
                    .withException(e)
                    .build();
        }
    }
}
