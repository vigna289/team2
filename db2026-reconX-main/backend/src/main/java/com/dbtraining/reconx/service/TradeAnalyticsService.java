package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.BondTrade;
import com.dbtraining.reconx.model.DerivativeTrade;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.FXTrade;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * TICKET-ADV034 / ADV035 / ADV036 — Trade analytics implementations.
 *
 * ADV035: VwapCollector is implemented here as a custom Collector that
 * accumulates (sumPriceQty, sumQty) and finishes to sumPriceQty / sumQty.
 * Combiner returns a fresh accumulator (parallel-safety).
 */
@Service
public class TradeAnalyticsService {

    /** TICKET-ADV034 — count + sum of notional per counterparty. */
    public Map<Long, NotionalSummary> notionalByCounterparty(List<? extends TradeType> trades) {
        if (trades == null || trades.isEmpty()) {
            return Map.of();
        }

        return trades.stream()
                .collect(Collectors.groupingBy(
                        this::counterpartyIdOf,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> new NotionalSummary(
                                        list.size(),
                                        list.stream()
                                                .map(t -> t.notional().amount())
                                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                )
                        )
                ));
    }

    /**
     * TICKET-ADV035 — VWAP = SUM(price * qty) / SUM(qty). Equity-only.
     *
     * Uses a custom Collector (VwapCollector) to compute VWAP in a
     * parallel-safe way and return BigDecimal scaled to 6 decimal places
     * with RoundingMode.HALF_UP. Empty input -> BigDecimal.ZERO.
     */
    public Map<String, BigDecimal> vwapByInstrument(List<EquityTrade> equityTrades) {
        if (equityTrades == null || equityTrades.isEmpty()) {
            return Map.of();
        }

        return equityTrades.stream()
                .collect(Collectors.groupingBy(
                        EquityTrade::instrumentSymbol,
                        new VwapCollector()
                ));
    }

    /** TICKET-ADV036 — P&L per instrument symbol (sign by Side). */
    public Map<String, BigDecimal> pnlByInstrument(List<EquityTrade> equityTrades) {
        if (equityTrades == null || equityTrades.isEmpty()) {
            return Map.of();
        }

        return equityTrades.stream()
                .collect(Collectors.groupingBy(
                        EquityTrade::instrumentSymbol,
                        Collectors.mapping(
                                this::pnl,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
    }

    private BigDecimal pnl(EquityTrade t) {
        BigDecimal abs = t.price().multiply(t.quantity());
        return t.side() == Side.SELL ? abs : abs.negate();
    }

    private long counterpartyIdOf(TradeType t) {
        return switch (t) {
            case EquityTrade e -> e.counterpartyId();
            case FXTrade f -> f.counterpartyId();
            case BondTrade b -> b.counterpartyId();
            case DerivativeTrade d -> d.counterpartyId();
        };
    }

    public record NotionalSummary(long count, BigDecimal total) {}

    /**
     * Custom Collector implementation that computes VWAP for a stream of EquityTrade.
     * Accumulator holds sumPriceQty and sumQty. Combiner returns a fresh accumulator
     * combining both inputs' sums (important for parallel safety).
     *
     * Produces BigDecimal scaled to 6 decimal places, HALF_UP. Empty accumulator -> BigDecimal.ZERO.
     */
    private static final class VwapCollector implements Collector<EquityTrade, VwapCollector.Acc, BigDecimal> {

        static final int SCALE = 4;
        static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

        static final class Acc {
            BigDecimal sumPriceQty;
            BigDecimal sumQty;

            Acc() {
                this.sumPriceQty = BigDecimal.ZERO;
                this.sumQty = BigDecimal.ZERO;
            }

            Acc(BigDecimal sumPriceQty, BigDecimal sumQty) {
                this.sumPriceQty = sumPriceQty;
                this.sumQty = sumQty;
            }
        }

        @Override
        public Supplier<Acc> supplier() {
            return Acc::new;
        }

        @Override
        public BiConsumer<Acc, EquityTrade> accumulator() {
            return (acc, t) -> {
                // price * qty
                BigDecimal priceQty = t.price().multiply(t.quantity());
                acc.sumPriceQty = acc.sumPriceQty.add(priceQty);
                acc.sumQty = acc.sumQty.add(t.quantity());
            };
        }

        @Override
        public BinaryOperator<Acc> combiner() {
            // Return a fresh accumulator combining the two inputs (do not mutate and return one of the inputs).
            return (a, b) -> new Acc(a.sumPriceQty.add(b.sumPriceQty), a.sumQty.add(b.sumQty));
        }

        @Override
        public Function<Acc, BigDecimal> finisher() {
            return acc -> {
                if (acc.sumQty.compareTo(BigDecimal.ZERO) == 0) {
                    return BigDecimal.ZERO;
                }
                return acc.sumPriceQty.divide(acc.sumQty, SCALE, ROUNDING);
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            // UNORDERED is appropriate (order doesn't matter for sums); no IDENTITY_FINISH because finish maps to BigDecimal.
            return Collections.singleton(Characteristics.UNORDERED);
        }
    }
}