package com.dbtraining.reconx.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TradeRequest DTO (POST body). JSR-380 validation lives here, not on the
 * JPA entity — the DTO is the wire contract; the domain Builder (ADV019-022)
 * is the second line of defence.
 */
public record TradeRequest(
        @NotNull
        @Pattern(regexp = "^[A-Z]{3}-\\d{8}-\\d{4}$",
                 message = "tradeRef must match AAA-YYYYMMDD-NNNN")
        String tradeRef,

        @NotNull
        Long instrumentId,

        @NotNull
        Long counterpartyId,

        @NotBlank
        String assetClass,

        @NotBlank
        @Pattern(regexp = "^(BUY|SELL)$")
        String side,

        @NotNull @Positive
        BigDecimal quantity,

        @NotNull @PositiveOrZero
        BigDecimal price,

        @NotNull
        LocalDate tradeDate
) {}
