package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconResult;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.model.TradeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * TICKET-ADV047 — edge cases: empty internal, single-trade-no-external,
 * all-mismatched. These are the boundary contract every later-day consumer
 * of ReconciliationEngine (REST layer, Kafka retry handler, chaos tests)
 * assumes holds — the engine must never throw NPE/IllegalStateException on
 * these inputs.
 * ============================================================================
 *
 * NOTE: add these three methods into your existing ReconciliationEngineTest
 * class (or keep this as a separate ReconciliationEngineEdgeCasesTest class —
 * either works, just make sure `engine` is constructed the same way your
 * other engine tests do it).
 */
class ReconciliationEngineEdgeCasesTest {

    private final ReconciliationEngine engine = new ReconciliationEngine();

    private EquityTrade equity(String ref, String price, String qty) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal(qty))
                .price(new BigDecimal(price))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.now())
                .counterpartyId(1L)
                .build();
    }

    @Test
    void testReconcile_emptyInternal_returnsEmpty() {
        assertThat(engine.reconcile(List.of(), List.of(), ReconciliationRule.EXACT)).isEmpty();
    }

    @Test
    void testReconcile_singleInternalNoExternal_returnsBreak() {
        TradeType internal = equity("EQU-20260603-0001", "100.00", "1000");

        List<ReconResult> out = engine.reconcile(List.of(internal), List.of(), ReconciliationRule.EXACT);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.BREAK);
        assertThat(out.get(0).discrepancyType()).isEqualTo("MISSING_EXTERNAL");
    }

    @Test
    void testReconcile_allMismatched_summaryShowsZeroMatched() {
        List<TradeType> internals = List.of(
                equity("EQU-20260603-0002", "100.00", "1000"),
                equity("EQU-20260603-0003", "100.00", "1000"),
                equity("EQU-20260603-0004", "100.00", "1000"));
        List<TradeType> externals = List.of(
                equity("EQU-20260603-0002", "200.00", "1000"),
                equity("EQU-20260603-0003", "200.00", "1000"),
                equity("EQU-20260603-0004", "200.00", "1000"));

        List<ReconResult> out = engine.reconcile(internals, externals, ReconciliationRule.EXACT);
        ReconSummary summary = out.stream().collect(new ReconSummaryCollector());

        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.matched()).isEqualTo(0);
        assertThat(summary.broken()).isEqualTo(3);
    }
}
