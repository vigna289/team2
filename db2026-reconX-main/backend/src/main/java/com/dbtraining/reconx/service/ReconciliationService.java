package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.ReconResultRepository;

import java.util.List;

/** Runs reconciliation and passes every resulting row to the result repository. */
public class ReconciliationService {

    private final ReconciliationEngine engine;
    private final ReconResultRepository repository;

    public ReconciliationService(ReconciliationEngine engine, ReconResultRepository repository) {
        this.engine = engine;
        this.repository = repository;
    }

    public void runRecon(List<TradeType> internal, List<TradeType> external, ReconciliationRule rule) {
        engine.reconcile(internal, external, rule).forEach(repository::save);
    }
}
