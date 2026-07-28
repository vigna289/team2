package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.TradeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * TICKET-ADV034 — Trade analytics with Collectors (groupingBy + summarizing)
 * TICKET-ADV035 — VWAP calculator using Streams + custom collector
 * TICKET-ADV036 — P&L per instrument: stream reduction
 * ============================================================================
 */
@Service
public class TradeAnalyticsService {

    /** TICKET-ADV034 — count + sum of notional per counterparty. */
    public Map<Long, NotionalSummary> notionalByCounterparty(List<? extends TradeType> trades) {
        return trades.stream().collect(Collectors.groupingBy(
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
     * TICKET-ADV035 — VWAP = SUM(price * qty) / SUM(qty). Equity-only — only
     * EquityTrade has a meaningful price-volume pair.
     */
    public Map<String, BigDecimal> vwapByInstrument(List<EquityTrade> equityTrades) {
        // TODO(TICKET-ADV035): group by EquityTrade::instrumentSymbol, then for
        //   each bucket compute SUM(price * qty) / SUM(qty) using BigDecimal
        //   with RoundingMode.HALF_UP. Return BigDecimal.ZERO when totalQty is 0
        //   (avoid ArithmeticException on division by zero).
        throw new UnsupportedOperationException("TICKET-ADV035");
    }

    /** TICKET-ADV036 — P&L per instrument symbol (sign by Side). */
    public Map<String, BigDecimal> pnlByInstrument(List<EquityTrade> equityTrades) {
        return equityTrades.stream().collect(Collectors.groupingBy(
                EquityTrade::instrumentSymbol,
                Collectors.mapping(this::pnl,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
        ));
    }

    private BigDecimal pnl(EquityTrade t) {
        BigDecimal abs = t.price().multiply(t.quantity());
        return t.side() == com.dbtraining.reconx.model.Side.SELL ? abs : abs.negate();
    }

    private long counterpartyIdOf(TradeType t) {
        return switch (t) {
            case com.dbtraining.reconx.model.EquityTrade equityTrade -> equityTrade.counterpartyId();
            case com.dbtraining.reconx.model.FXTrade fxTrade -> fxTrade.counterpartyId();
            case com.dbtraining.reconx.model.BondTrade bondTrade -> bondTrade.counterpartyId();
            case com.dbtraining.reconx.model.DerivativeTrade derivativeTrade -> derivativeTrade.counterpartyId();
        };
    }

    public record NotionalSummary(long count, BigDecimal total) {}
}
