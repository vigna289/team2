package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.model.TradeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TradeAnalyticsServiceTest {

    private final TradeAnalyticsService service = new TradeAnalyticsService();

    @Test
    void notionalByCounterparty_groupsTradesAndSumsNotionalPerCounterparty() {
        List<TradeType> trades = List.of(
                trade("AAA-20250101-0001", 101L, "100"),
                trade("AAA-20250101-0002", 101L, "50"),
                trade("AAA-20250101-0003", 202L, "25")
        );

        var result = service.notionalByCounterparty(trades);

        assertThat(result).containsOnlyKeys(101L, 202L);
        assertThat(result.get(101L).count()).isEqualTo(2L);
        assertThat(result.get(101L).total()).isEqualByComparingTo("150");
        assertThat(result.get(202L).count()).isEqualTo(1L);
        assertThat(result.get(202L).total()).isEqualByComparingTo("25");
    }

    @Test
    void pnlByInstrument_sumsSignedPnlPerInstrument() {
        List<EquityTrade> trades = List.of(
                trade("AAA-20250101-0001", 101L, "100", Side.BUY),
                trade("AAA-20250101-0002", 101L, "50", Side.BUY),
                trade("AAA-20250101-0003", 202L, "80", Side.SELL)
        );

        var result = service.pnlByInstrument(trades);

        assertThat(result).containsEntry("AAPL", new BigDecimal("-70"));
    }

    private EquityTrade trade(String ref, long counterpartyId, String price) {
        return trade(ref, counterpartyId, price, Side.BUY);
    }

    private EquityTrade trade(String ref, long counterpartyId, String price, Side side) {
        return EquityTrade.builder()
                .tradeRef(new TradeRef(ref))
                .instrumentSymbol("AAPL")
                .quantity(new BigDecimal("1"))
                .price(new BigDecimal(price))
                .currency("USD")
                .side(side)
                .tradeDate(LocalDate.of(2025, 1, 1))
                .counterpartyId(counterpartyId)
                .build();
    }
}
