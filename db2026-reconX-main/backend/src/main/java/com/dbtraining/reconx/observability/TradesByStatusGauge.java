package com.dbtraining.reconx.observability;

import com.dbtraining.reconx.repository.TradeRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TradesByStatusGauge {

    private static final List<String> STATUSES = List.of(
            "PENDING",
            "MATCHED",
            "UNMATCHED",
            "DISPUTED",
            "CANCELLED"
    );

    public TradesByStatusGauge(
            MeterRegistry registry,
            TradeRepository repo
    ) {
        for (String status : STATUSES) {
            Gauge.builder(
                    "trades_by_status",
                    repo,
                    r -> r.countByStatus(status)
            )
            .tag("status", status)
            .register(registry);
        }
    }
}