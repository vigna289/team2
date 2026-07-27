package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Enum-with-state: each constant carries its own price tolerance (%) and
 * quantity tolerance (absolute units). matches() returns true only if BOTH
 * the price diff (as %) and the qty diff (as abs) are within tolerance.
 * BigDecimal.compareTo only — no double, no ==.
 */
public enum ReconciliationRule {

    EXACT(BigDecimal.ZERO, BigDecimal.ZERO),
    PRICE_TOLERANCE_1PCT(new BigDecimal("0.01"), BigDecimal.ZERO),
    PRICE_TOLERANCE_50BPS(new BigDecimal("0.005"), BigDecimal.ZERO),
    QTY_TOLERANCE_5UNITS(BigDecimal.ZERO, new BigDecimal("5")),
    LOOSE(new BigDecimal("0.05"), new BigDecimal("10"));

    private final BigDecimal priceTolerancePct;
    private final BigDecimal qtyToleranceAbs;

    ReconciliationRule(BigDecimal priceTolerancePct, BigDecimal qtyToleranceAbs) {
        this.priceTolerancePct = priceTolerancePct;
        this.qtyToleranceAbs   = qtyToleranceAbs;
    }

    public BigDecimal priceTolerancePct() { return priceTolerancePct; }
    public BigDecimal qtyToleranceAbs()   { return qtyToleranceAbs; }

    /**
     * @return true if both the price diff (as %) and the qty diff (as abs)
     *         are within this rule's tolerance.
     */
    public boolean matches(BigDecimal internalPrice, BigDecimal internalQty,
                           BigDecimal externalPrice, BigDecimal externalQty) {
        BigDecimal priceDiff = internalPrice.subtract(externalPrice).abs();
        BigDecimal priceDiffPct = internalPrice.signum() == 0
                ? BigDecimal.ZERO
                : priceDiff.divide(internalPrice, 6, RoundingMode.HALF_UP);
        BigDecimal qtyDiff = internalQty.subtract(externalQty).abs();

        boolean priceOk = priceDiffPct.compareTo(priceTolerancePct) <= 0;
        boolean qtyOk   = qtyDiff.compareTo(qtyToleranceAbs) <= 0;
        return priceOk && qtyOk;
    }
}
