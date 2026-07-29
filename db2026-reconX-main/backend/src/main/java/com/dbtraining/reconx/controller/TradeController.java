package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.BondTrade;
import com.dbtraining.reconx.model.DerivativeTrade;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.FXTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ReconciliationEngine — index externals for O(1) lookups, stream internals,
 * and produce ReconResult per internal trade. Also provides a batched
 * reconcileByCounterparty that runs each counterparty batch concurrently.
 */
@Service
public class ReconciliationEngine {

    @Timed(
            value = "reconciliation.duration",
            description = "Wall time of reconcile()",
            percentiles = {0.5, 0.95, 0.99},
            histogram = true
    )
    public List<ReconResult> reconcile(List<TradeType> internal,
                                       List<TradeType> external,
                                       ReconciliationRule rule) {

        // Guard: null or empty inputs → empty result list
        if (internal == null || internal.isEmpty()) {
            return List.of();
        }

        // Build external index (tradeRef -> TradeType). Treat null external as empty list.
        Map<String, TradeType> externalByRef = (external == null ? List.<TradeType>of() : external)
                .stream()
                .collect(Collectors.toMap(
                        t -> t.tradeRef().value(),
                        Function.identity(),
                        (first, second) -> first // keep first on duplicates
                ));

        // Stream internals in parallel and match each one via constant-time lookup.
        return internal.parallelStream()
                .map(in -> matchOne(in, externalByRef.get(in.tradeRef().value()), rule))
                .toList();
    }

    /**
     * Reconcile by counterparty: inputs are lists containing many counterparties.
     * We group each side by counterparty id and reconcile each counterparty in its own
     * async task, then combine the results.
     */
    public List<ReconResult> reconcileByCounterparty(List<TradeType> internal,
                                                     List<TradeType> external,
                                                     ReconciliationRule rule) {

        if (internal == null || internal.isEmpty()) {
            return List.of();
        }

        // Group both sides by counterparty id (null external handled as empty)
        Map<String, List<TradeType>> internalByCp = internal.stream()
                .collect(Collectors.groupingBy(this::counterpartyIdOf));

        Map<String, List<TradeType>> externalByCp = (external == null ? List.<TradeType>of() : external)
                .stream()
                .collect(Collectors.groupingBy(this::counterpartyIdOf));

        // For each counterparty, reconcile in its own CompletableFuture
        List<CompletableFuture<List<ReconResult>>> futures = internalByCp.entrySet()
                .stream()
                .map(entry -> CompletableFuture.supplyAsync(() ->
                        reconcile(
                                entry.getValue(),
                                externalByCp.getOrDefault(entry.getKey(), List.of()),
                                rule)))
                .toList();

        // Wait for all and combine
        CompletableFuture<Void> all =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        return all.thenApply(v ->
                        futures.stream()
                                .flatMap(f -> f.join().stream())
                                .toList())
                .join();
    }

    private ReconResult matchOne(TradeType internal,
                                 TradeType external,
                                 ReconciliationRule rule) {

        String ref = internal.tradeRef().value();

        if (external == null) {
            return ReconResult.breakResult(
                    ref,
                    "MISSING_EXTERNAL",
                    "No matching external trade found"
            );
        }

        BigDecimal[] internalValues = priceQty(internal);
        BigDecimal[] externalValues = priceQty(external);

        boolean matches = rule.matches(
                internalValues[0],
                internalValues[1],
                externalValues[0],
                externalValues[1]
        );

        if (matches) {
            return ReconResult.matched(ref);
        }

        return ReconResult.breakResult(
                ref,
                "VALUE_MISMATCH",
                "internal=%s/%s external=%s/%s".formatted(
                        internalValues[0],
                        internalValues[1],
                        externalValues[0],
                        externalValues[1]
                )
        );
    }

    /**
     * Exhaustive switch over the sealed TradeType hierarchy.
     */
    private BigDecimal[] priceQty(TradeType t) {
        return switch (t) {
            case EquityTrade e -> new BigDecimal[]{e.price(), e.quantity()};
            case FXTrade f -> new BigDecimal[]{f.fxRate(), f.notionalCcy1()};
            case BondTrade b -> new BigDecimal[]{b.faceValue(), BigDecimal.ONE};
            case DerivativeTrade d -> new BigDecimal[]{d.strike(), d.quantity()};
        };
    }

    /**
     * Helper: get counterparty id for grouping.
     */
    private String counterpartyIdOf(TradeType t) {
        return switch (t) {
            case EquityTrade e -> Long.toString(e.counterpartyId());
            case FXTrade f -> Long.toString(f.counterpartyId());
            case BondTrade b -> Long.toString(b.counterpartyId());
            case DerivativeTrade d -> Long.toString(d.counterpartyId());
        };
    }
}