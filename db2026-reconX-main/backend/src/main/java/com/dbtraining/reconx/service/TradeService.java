package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.TradeSpecifications;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * NOTE: list(...) is the ADV056/057 deliverable — it composes the three
 * TradeSpecifications with Specification.where(...).and(...).and(...) and
 * calls tradeRepository.findAll(spec, pageable).
 *
 * create/update/updateStatus/softDelete are placeholders for the Day 5
 * REST tickets (ADV064-ADV067) — status is a plain String on the real
 * Trade entity (no TradeStatus enum exists in this codebase), and
 * counterparty/instrument are resolved from the ids on TradeRequest via
 * their repositories, since the real Trade entity has no multi-arg
 * constructor — only a no-arg constructor plus setters.
 */
@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final CounterpartyRepository counterpartyRepository;

    public TradeService(TradeRepository tradeRepository,
                         InstrumentRepository instrumentRepository,
                         CounterpartyRepository counterpartyRepository) {
        this.tradeRepository = tradeRepository;
        this.instrumentRepository = instrumentRepository;
        this.counterpartyRepository = counterpartyRepository;
    }

    @Transactional(readOnly = true)
    public Page<Trade> list(LocalDate from, LocalDate to, String status,
                             Long counterpartyId, Pageable pageable) {
        Specification<Trade> spec = Specification
            .where(TradeSpecifications.tradeDateBetween(from, to))
            .and(TradeSpecifications.hasStatus(status))
            .and(TradeSpecifications.hasCounterparty(counterpartyId));
        return tradeRepository.findAll(spec, pageable);
    }

    @Transactional
    public Trade create(TradeRequest request) {
        // TODO(TICKET-ADV064): confirm exact duplicate-check + event-publish
        // behaviour once the ADV064 ticket text is available.
        tradeRepository.findByTradeRef(request.tradeRef()).ifPresent(existing -> {
            throw new DuplicateTradeRefException(request.tradeRef());
        });

        Instrument instrument = instrumentRepository.findById(request.instrumentId())
            .orElseThrow(() -> new IllegalArgumentException(
                "No instrument with id " + request.instrumentId()));
        Counterparty counterparty = counterpartyRepository.findById(request.counterpartyId())
            .orElseThrow(() -> new IllegalArgumentException(
                "No counterparty with id " + request.counterpartyId()));

        Trade trade = new Trade();
        trade.setTradeRef(request.tradeRef());
        trade.setInstrument(instrument);
        trade.setCounterparty(counterparty);
        trade.setAssetClass(request.assetClass());
        trade.setSide(request.side());
        trade.setQuantity(request.quantity());
        trade.setPrice(request.price());
        trade.setTradeDate(request.tradeDate());
        return tradeRepository.save(trade);
    }

    @Transactional
    public Trade update(Long id, TradeRequest request) {
        // TODO(TICKET-ADV065): confirm exact update semantics once available.
        Trade trade = tradeRepository.findById(id)
            .orElseThrow(() -> new TradeNotFoundException(String.valueOf(id)));
        trade.setQuantity(request.quantity());
        trade.setPrice(request.price());
        trade.setTradeDate(request.tradeDate());
        return trade;
    }

    @Transactional
    public Trade updateStatus(Long id, String status) {
        // TODO(TICKET-ADV066): confirm exact status-transition rules once available.
        Trade trade = tradeRepository.findById(id)
            .orElseThrow(() -> new TradeNotFoundException(String.valueOf(id)));
        trade.setStatus(status);
        return trade;
    }

    @Transactional
    public void softDelete(Long id) {
        // TODO(TICKET-ADV067): confirm exact soft-delete semantics once available.
        Trade trade = tradeRepository.findById(id)
            .orElseThrow(() -> new TradeNotFoundException(String.valueOf(id)));
        trade.softDelete();
    }
}
