package com.dbtraining.reconx.observability;

import org.apache.kafka.clients.admin.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Only registers when spring.kafka.bootstrap-servers is actually set —
 * @ConditionalOnProperty means this component is entirely absent under dev
 * (no Kafka) rather than sitting there as a permanent yellow UNKNOWN status.
 * Builds a short-timeout AdminClient so a dead broker fails fast instead of
 * hanging the health check.
 */
@Component("reconxKafka")
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaHealthIndicator extends AbstractHealthIndicator {

    private final String bootstrapServers;

    public KafkaHealthIndicator(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        Map<String, Object> cfg = Map.of(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,      bootstrapServers,
            AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,     2_000,
            AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 3_000
        );
        try (AdminClient admin = AdminClient.create(cfg)) {
            DescribeClusterResult cluster = admin.describeCluster();
            String clusterId = cluster.clusterId().get(2, TimeUnit.SECONDS);
            int nodeCount    = cluster.nodes().get(2, TimeUnit.SECONDS).size();
            builder.up()
                   .withDetail("clusterId", clusterId)
                   .withDetail("nodeCount", nodeCount);
        } catch (Exception e) {
            builder.down(e);
        }
    }
}
