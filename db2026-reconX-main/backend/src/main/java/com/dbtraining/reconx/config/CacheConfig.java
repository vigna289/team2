package com.dbtraining.reconx.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * TICKET-ADV081 — @EnableCaching switches on Spring's caching proxy.
 * TICKET-ADV082 — two separately-named Caffeine caches with different TTLs:
 *   instruments:    5 min TTL, max 500 entries (symbol lookup is stable data)
 *   counterparties: 1 min TTL, max 200 entries (ages faster / smaller set)
 * recordStats() on both is what lets Micrometer publish cache_gets_total
 * (hit/miss counts) to /actuator/prometheus.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache instruments = new CaffeineCache("instruments",
            Caffeine.newBuilder()
                    .maximumSize(500)
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .recordStats()
                    .build());

        CaffeineCache counterparties = new CaffeineCache("counterparties",
            Caffeine.newBuilder()
                    .maximumSize(200)
                    .expireAfterWrite(1, TimeUnit.MINUTES)
                    .recordStats()
                    .build());

        SimpleCacheManager mgr = new SimpleCacheManager();
        mgr.setCaches(List.of(instruments, counterparties));
        return mgr;
    }
}
