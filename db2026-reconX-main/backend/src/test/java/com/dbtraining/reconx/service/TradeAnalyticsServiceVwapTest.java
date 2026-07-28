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
    void vwap_serial_and_empty() {
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

        // expected = (10*2 + 12*3) / (2+3) = 56 / 5 = 11.2 -> scaled according to collector
        Map<String, BigDecimal> result = svc.vwapByInstrument(trades);
        assertThat(result).containsKey("ABC");

        // check value equals hand computed expected (scale isn't checked exactly here, but arithmetic should match)
        BigDecimal expected = new BigDecimal("56.00").divide(new BigDecimal("5"), 6, java.math.RoundingMode.HALF_UP);
        assertThat(result.get("ABC")).isEqualTo(expected);

        // empty input -> service returns empty map
        assertThat(svc.vwapByInstrument(List.of())).isEmpty();
    }
}