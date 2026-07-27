package com.dbtraining.reconx.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Strongly-typed wrapper around the trade reference string. Format:
 * AAA-YYYYMMDD-NNNN (3 uppercase letters, 8-digit date, 4 digits).
 * Validated in the compact constructor so an invalid TradeRef can never
 * exist as an object.
 */
public record TradeRef(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{3}-\\d{8}-\\d{4}$");

    public TradeRef {
        Objects.requireNonNull(value, "tradeRef value");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid tradeRef format '%s' — expected AAA-YYYYMMDD-NNNN".formatted(value));
        }
    }

    public static TradeRef of(String value) {
        return new TradeRef(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
