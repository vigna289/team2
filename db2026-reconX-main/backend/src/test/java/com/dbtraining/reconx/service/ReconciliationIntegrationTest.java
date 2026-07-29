package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.ReconResult;
import com.dbtraining.reconx.model.Trade;
import com.dbtraining.reconx.repository.ExternalTradeRepository;
import com.dbtraining.reconx.repository.InternalTradeRepository;
import com.dbtraining.reconx.repository.ReconResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * TICKET-ADV044 + TICKET-ADV045
 *
 * WHAT:    Boots a real postgres:16-alpine container via Testcontainers,
 *          wires Spring's datasource against it, and end-to-end tests the
 *          insert -> reconcile -> persist round trip through real SQL.
 * WHY:     Mocks prove the code path; this proves the SQL actually works
 *          (CHAR/VARCHAR mismatches, @Lob issues, Liquibase migrations all
 *          surface here that an H2 stand-in would hide).
 * ============================================================================
 */
@SpringBootTest
@Testcontainers
class ReconciliationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reconx")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private InternalTradeRepository internalTradeRepo;

    @Autowired
    private ExternalTradeRepository externalTradeRepo;

    @Autowired
    private ReconResultRepository reconResultRepo;

    @Autowired
    private ReconciliationService reconciliationService;

    /** TICKET-ADV044 sanity check — if this passes, the container + Spring wiring is correct. */
    @Test
    void containerIsRunning() {
        // sanity: the real assertions live in insertedTradesAreReconciledAndPersisted below.
    }

    /** TICKET-ADV045 — full round trip through real SQL. */
    @Test
    void insertedTradesAreReconciledAndPersisted() {
        // given — two matching trades, one in each repo
        Trade internal = new Trade("TRD-INT-1", "CP-1", "SAP.DE",
                new BigDecimal("100"), new BigDecimal("245.50"), LocalDate.now());
        Trade external = new Trade("TRD-INT-1", "CP-1", "SAP.DE",
                new BigDecimal("100"), new BigDecimal("245.50"), LocalDate.now());

        internalTradeRepo.save(internal);
        externalTradeRepo.save(external);

        // when
        reconciliationService.runRecon(
                internalTradeRepo.findAll(),
                externalTradeRepo.findAll());

        // then — exactly one MATCHED row landed in recon_results
        List<ReconResult> persisted = reconResultRepo.findAll();
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
        assertThat(persisted.get(0).tradeRef()).isEqualTo("TRD-INT-1");
    }
}
