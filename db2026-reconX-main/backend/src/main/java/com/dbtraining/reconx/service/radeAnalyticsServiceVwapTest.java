package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TradeAnalyticsServiceVwapTest {

    @Test
    void vwap_serial_and_parallel_and_empty() {
        TradeAnalyticsService svc = new TradeAnalyticsService();

        EquityTrade t1 = EquityTrade.builder()
                .tradeRef(TradeRef.of("T1"))
                .instrumentSymbol("ABC")
                .price(new BigDecimal("10.00"))
                .quantity(new BigDecimal("2"))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.now())
                .counterpartyId(1L)
                .build();

        EquityTrade t2 = EquityTrade.builder()
                .tradeRef(TradeRef.of("T2"))
                .instrumentSymbol("ABC")
                .price(new BigDecimal("12.00"))
                .quantity(new BigDecimal("3"))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.now())
                .counterpartyId(1L)
                .build();

        List<EquityTrade> trades = List.of(t1, t2);

        // expected = (10*2 + 12*3) / (2+3) = 56 / 5 = 11.2 -> scaled per collector
        Map<String, BigDecimal> serial = svc.vwapByInstrument(trades);
        Map<String, BigDecimal> parallel = trades.parallelStream()
                .collect(java.util.stream.Collectors.groupingBy(EquityTrade::instrumentSymbol, new TradeAnalyticsService.VwapCollector()));

        assertThat(serial).containsKey("ABC");
        assertThat(parallel).containsKey("ABC");
        assertThat(serial.get("ABC")).isEqualTo(parallel.get("ABC"));

        // empty input -> BigDecimal.ZERO
        assertThat(svc.vwapByInstrument(List.of())).isEmpty();
    }
}