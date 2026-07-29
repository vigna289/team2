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

    public TradeService(TradeRepository tradeRepo,
                        CounterpartyRepository cpRepo,
                        InstrumentRepository instRepo,
                        TradeEventProducer events,
                        TradeMetrics metrics) {
        this.tradeRepo = tradeRepo;
        this.cpRepo = cpRepo;
        this.instRepo = instRepo;
        this.events = events;
        this.metrics = metrics;
    }

    public Trade create(TradeRequest req, String actor) {

    if (tradeRepo.findByTradeRef(req.tradeRef()).isPresent()) {
        throw new DuplicateTradeRefException(req.tradeRef());
    }

   var instrument = instRepo.findById(req.instrumentId())
        .orElseThrow(() -> new TradeNotFoundException(
                "Instrument id: " + req.instrumentId()
        ));
    var counterparty = cpRepo.findById(req.counterpartyId())
        .orElseThrow(() -> new TradeNotFoundException(
                "Counterparty id: " + req.counterpartyId()
        ));

    Trade trade = new Trade();

    trade.setTradeRef(req.tradeRef());
    trade.setInstrument(instrument);
    trade.setCounterparty(counterparty);
    trade.setAssetClass(req.assetClass());
    trade.setSide(req.side());
    trade.setQuantity(req.quantity());
    trade.setPrice(req.price());
    trade.setTradeDate(req.tradeDate());
    trade.setStatus("PENDING");

    return tradeRepo.save(trade);
}

    public Trade update(Long id, TradeRequest req, String actor) {

    Trade trade = tradeRepo.findById(id)
        .orElseThrow(() -> new TradeNotFoundException(
                "Trade id: " + id
        ));

    trade.setTradeRef(req.tradeRef());
    trade.setQuantity(req.quantity());
    trade.setPrice(req.price());
    trade.setTradeDate(req.tradeDate());
    trade.setSide(req.side());
    trade.setAssetClass(req.assetClass());

    trade.setCounterparty(
            cpRepo.findById(req.counterpartyId())
        .orElseThrow(() -> new TradeNotFoundException(
                "Counterparty id: " + req.counterpartyId()
        ))
    );

    trade.setInstrument(
            instRepo.findById(req.instrumentId())
        .orElseThrow(() -> new TradeNotFoundException(
                "Instrument id: " + req.instrumentId()
        ))
    );

    return tradeRepo.save(trade);
}

    public Trade updateStatus(Long id, String status, String actor) {
        // TODO(TICKET-ADV066): load, setStatus(status), save, publish TRADE_UPDATED
        //   with the new status in the "after" slot of the event.
        throw new UnsupportedOperationException("TICKET-ADV066");
    }

    public void softDelete(Long id, String actor) {
        // TODO(TICKET-ADV067): load, call t.softDelete() (sets deleted_at), save,
        //   publish a TRADE_CANCELLED event.
        throw new UnsupportedOperationException("TICKET-ADV067");
    }

    @Transactional(readOnly = true)
public Page<Trade> list(LocalDate from,
                        LocalDate to,
                        String status,
                        Long counterpartyId,
                        Pageable pageable) {

    Specification<Trade> spec = Specification.where(null);

    if (from != null && to != null) {
        spec = spec.and(tradeDateBetween(from, to));
    }

    if (status != null) {
        spec = spec.and(hasStatus(status));
    }

    if (counterpartyId != null) {
        spec = spec.and(hasCounterparty(counterpartyId));
    }

    return tradeRepo.findAll(spec, pageable);
}
}
