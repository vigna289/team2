package com.dbtraining.reconx.service;

import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.entity.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * TICKET-ADV081 — @Cacheable on findBySymbol (cache name "instruments").
 * TTL/size configured in CacheConfig (TICKET-ADV082).
 *
 * Symbol lookup is hot — most requests should hit the cache, not the DB.
 * The log line proves it: it should print on the first call for a given
 * symbol, then NOT print again for that same symbol until the entry expires.
 */
@Service
public class InstrumentService {

    private static final Logger log = LoggerFactory.getLogger(InstrumentService.class);

    private final InstrumentRepository repo;

    public InstrumentService(InstrumentRepository repo) { this.repo = repo; }

    @Cacheable(value = "instruments", key = "#symbol")
    public Instrument findBySymbol(String symbol) {
        log.info("DB hit for {}", symbol);
        return repo.findBySymbol(symbol)
                .orElseThrow(() -> new InvalidTradeException("Unknown instrument symbol: " + symbol));
    }
}
