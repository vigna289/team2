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

@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final TradeEventProducer events;
    private final TradeMetrics metrics;

    public TradeService(
            TradeRepository tradeRepository,
            CounterpartyRepository cpRepo,
            InstrumentRepository instrumentRepository,
            TradeEventProducer events,
            TradeMetrics metrics) {

        this.tradeRepository = tradeRepository;
        this.instrumentRepository = instrumentRepository;
        this.counterpartyRepository = cpRepo;
        this.events = events;
        this.metrics = metrics;
    }


    @Transactional
    public Trade create(TradeRequest req, String actor) {

        if (tradeRepository.findByTradeRef(req.tradeRef()).isPresent()) {
            throw new DuplicateTradeRefException(req.tradeRef());
        }

        Instrument instrument = instrumentRepository.findById(req.instrumentId())
                .orElseThrow(() -> new TradeNotFoundException(
                        "Instrument id: " + req.instrumentId()));

        Counterparty counterparty = counterpartyRepository.findById(req.counterpartyId())
                .orElseThrow(() -> new TradeNotFoundException(
                        "Counterparty id: " + req.counterpartyId()));

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

        metrics.incrementTradeCreated();
        metrics.recordTradeValue(
                saved.getQuantity().multiply(saved.getPrice()).doubleValue()
        );

        return saved;
    }


    @Transactional
    public Trade update(Long id, TradeRequest req, String actor) {

        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException(
                        "Trade id: " + id));

        trade.setTradeRef(req.tradeRef());
        trade.setQuantity(req.quantity());
        trade.setPrice(req.price());
        trade.setTradeDate(req.tradeDate());
        trade.setSide(req.side());
        trade.setAssetClass(req.assetClass());

        trade.setCounterparty(
                counterpartyRepository.findById(req.counterpartyId())
                        .orElseThrow(() -> new TradeNotFoundException(
                                "Counterparty id: " + req.counterpartyId()))
        );

        trade.setInstrument(
                instrumentRepository.findById(req.instrumentId())
                        .orElseThrow(() -> new TradeNotFoundException(
                                "Instrument id: " + req.instrumentId()))
        );

        return tradeRepository.save(trade);
    }


    @Transactional
    public Trade updateStatus(Long id, String status, String actor) {

        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException(
                        "Trade id: " + id));

        trade.setStatus(status);
                Trade saved = tradeRepository.save(trade);
                // initialize lazily-loaded associations before returning (avoid LazyInitializationException in mappers)
                if (saved.getInstrument() != null) {
                        saved.getInstrument().getSymbol();
                }
                if (saved.getCounterparty() != null) {
                        saved.getCounterparty().getName();
                }
                return saved;
    }


    @Transactional
    public void softDelete(Long id, String actor) {

        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException(
                        "Trade id: " + id));

        trade.softDelete();

        tradeRepository.save(trade);
    }


    @Transactional(readOnly = true)
    public Page<Trade> list(
            LocalDate from,
            LocalDate to,
            String status,
            Long counterpartyId,
            Pageable pageable) {

        Specification<Trade> spec = Specification.where(null);

        if (from != null && to != null) {
            spec = spec.and(
                    TradeSpecifications.tradeDateBetween(from, to)
            );
        }

        if (status != null) {
            spec = spec.and(
                    TradeSpecifications.hasStatus(status)
            );
        }

        if (counterpartyId != null) {
            spec = spec.and(
                    TradeSpecifications.hasCounterparty(counterpartyId)
            );
        }

                Page<Trade> page = tradeRepository.findAll(spec, pageable);
                // initialize lazy associations while still in transaction to avoid LazyInitializationException
                page.getContent().forEach(t -> {
                        if (t.getInstrument() != null) t.getInstrument().getSymbol();
                        if (t.getCounterparty() != null) t.getCounterparty().getName();
                });
                return page;
    }
}