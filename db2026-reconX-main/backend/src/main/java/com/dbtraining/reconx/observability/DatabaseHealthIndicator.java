package com.dbtraining.reconx.observability;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;

/**
 * Replaces Boot's default DataSource health indicator, which only opens a
 * connection and never runs SQL or measures latency — silent during a
 * slow-driver outage. This one runs SELECT 1 with a 2-second timeout and
 * reports elapsed time so ops can actually see degradation, not just
 * up/down.
 */
@Component("database")
public class DatabaseHealthIndicator extends AbstractHealthIndicator {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        super("ReconX database health check failed");
        this.dataSource = dataSource;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        long start = System.nanoTime();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout((int) TIMEOUT.toSeconds());
            try (ResultSet rs = stmt.executeQuery("SELECT 1")) {
                rs.next();
            }
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            builder.up().withDetail("latencyMs", latencyMs);
        } catch (SQLException e) {
            builder.down(e);
        }
    }
}
