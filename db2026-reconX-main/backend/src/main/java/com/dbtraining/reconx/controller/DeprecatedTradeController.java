package com.dbtraining.reconx.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TICKET-ADV080 — deprecated v0 trade endpoint example.
 */
@RestController
@RequestMapping("/v0/trades")
@Tag(name = "deprecated-trades", description = "Deprecated API surface")
public class DeprecatedTradeController {

    @Deprecated(since = "v1.4.0", forRemoval = true)
    @GetMapping
    @Operation(summary = "Deprecated v0 trade endpoint")
    public ResponseEntity<Void> deprecatedTrades(HttpServletResponse response) {
        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", "Sat, 01 Jul 2026 00:00:00 GMT");
        response.setHeader("Link", "</api/v1/trades>; rel=\"successor-version\"");
        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}
