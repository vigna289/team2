package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.dto.ReconResult;

/** Persists reconciliation results produced by {@code ReconciliationService}. */
public interface ReconResultRepository {

    ReconResult save(ReconResult result);
}
