package com.dbtraining.reconx.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(
        name = "reconx.audit.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AuditAutoConfiguration {

    @Bean
    public AuditEventPublisher auditEventPublisher() {
        return new AuditEventPublisher();
    }
}