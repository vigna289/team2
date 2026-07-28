package com.dbtraining.reconx.model;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * Sealed root of the trade hierarchy. Only the four named permitted classes
 * can implement it. Comparable natural ordering: most-recent trade first,
 * ties broken by tradeRef ascending. Kept consistent with equals/hashCode
 * (TICKET-ADV028), which is also keyed on tradeRef.
 */
public sealed interface TradeType
        extends Comparable<TradeType>
        permits EquityTrade, FXTrade, BondTrade, DerivativeTrade {

    /** Stable natural key. Drives equals/hashCode. */
    TradeRef tradeRef();

    /** Notional value of the trade for reconciliation summaries. */
    Money notional();

    /** Business date the trade was struck on. */
    LocalDate tradeDate();

    /** Discriminator for switch expressions and persistence mapping. */
    AssetClass assetClass();

    Comparator<TradeType> NATURAL = Comparator
            .comparing(TradeType::tradeDate).reversed()
            .thenComparing(t -> t.tradeRef().value());

    @Override
    default int compareTo(TradeType other) {
        return NATURAL.compare(this, other);
    }

    enum AssetClass { EQUITY, FX, BOND, DERIVATIVE }
}
