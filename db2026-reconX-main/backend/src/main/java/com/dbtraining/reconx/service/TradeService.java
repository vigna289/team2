package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
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
 * create/update are the Day 5 REST tickets (ADV064/065) — status is a plain
 * String on the real Trade entity (no TradeStatus enum), and
 * counterparty/instrument are resolved from the ids on TradeRequest via
 * their repositories, since the real Trade entity has no multi-arg
 * constructor — only a no-arg constructor plus setters.
 *
 * updateStatus/softDelete (ADV066/067) are still stubs — leave them
 * throwing until those tickets are picked up.
 *
 * events (TradeEventProducer) is wired in but NOT called yet — its
 * publish() method still throws UnsupportedOperationException (ADV129
 * is a separate, later ticket). Calling it now would break every trade
 * creation. Wire in a call once ADV129 is actually implemented.
 *
 * metrics (TradeMetrics) IS fully implemented (ADV083/086) and is safe
 * to call — incrementTradeCreated() + recordTradeValue() fire after a
 * successful save.
 */
@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final TradeEventProducer events;
    private final TradeMetrics metrics;

    public TradeService(TradeRepository tradeRepository,
                         CounterpartyRepository counterpartyRepository,
                         InstrumentRepository instrumentRepository,
                         TradeEventProducer events,
                         TradeMetrics metrics) {
        this.tradeRepository = tradeRepository;
        this.counterpartyRepository = counterpartyRepository;
        this.instrumentRepository = instrumentRepository;
        this.events = events;
        this.metrics = metrics;
    }

    @Transactional
    public Trade create(TradeRequest req, String actor) {

        if (tradeRepository.findByTradeRef(req.tradeRef()).isPresent()) {
            throw new DuplicateTradeRefException(req.tradeRef());
        }

        Instrument instrument = instrumentRepository.findById(req.instrumentId())
                .orElseThrow(() -> new TradeNotFoundException("Instrument id: " + req.instrumentId()));
        Counterparty counterparty = counterpartyRepository.findById(req.counterpartyId())
                .orElseThrow(() -> new TradeNotFoundException("Counterparty id: " + req.counterpartyId()));

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

        Trade saved = tradeRepository.save(trade);

        // TICKET-ADV083/086 — safe to call, fully implemented.
        metrics.incrementTradeCreated();
        metrics.recordTradeValue(saved.getQuantity().multiply(saved.getPrice()).doubleValue());

        // TODO(TICKET-ADV129): once TradeEventProducer.publish(...) is
        // implemented, publish a TRADE_CREATED event here — AFTER commit,
        // per the GOTCHA in TradeEventProducer's Javadoc (never let a Kafka
        // publish failure roll back the DB transaction).

        return saved;
    }

    @Transactional
    public Trade update(Long id, TradeRequest req, String actor) {

        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Trade id: " + id));

        trade.setTradeRef(req.tradeRef());
        trade.setQuantity(req.quantity());
        trade.setPrice(req.price());
        trade.setTradeDate(req.tradeDate());
        trade.setSide(req.side());
        trade.setAssetClass(req.assetClass());

        trade.setCounterparty(
                counterpartyRepository.findById(req.counterpartyId())
                        .orElseThrow(() -> new TradeNotFoundException("Counterparty id: " + req.counterpartyId())));

        trade.setInstrument(
                instrumentRepository.findById(req.instrumentId())
                        .orElseThrow(() -> new TradeNotFoundException("Instrument id: " + req.instrumentId())));

        return tradeRepository.save(trade);
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
            spec = spec.and(TradeSpecifications.tradeDateBetween(from, to));
        }

        if (status != null) {
            spec = spec.and(TradeSpecifications.hasStatus(status));
        }

        if (counterpartyId != null) {
            spec = spec.and(TradeSpecifications.hasCounterparty(counterpartyId));
        }

        return tradeRepository.findAll(spec, pageable);
    }
}
