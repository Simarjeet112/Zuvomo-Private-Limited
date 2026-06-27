package com.tradingsignal.tracker.service;

import com.tradingsignal.tracker.dto.CreateSignalRequest;
import com.tradingsignal.tracker.dto.SignalResponse;
import com.tradingsignal.tracker.entity.Signal;
import com.tradingsignal.tracker.entity.SignalStatus;
import com.tradingsignal.tracker.exception.SignalNotFoundException;
import com.tradingsignal.tracker.repository.SignalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SignalService {

    private final SignalRepository signalRepository;
    private final SignalEvaluationService evaluationService;
    private final BinancePriceService binancePriceService;

    public SignalService(SignalRepository signalRepository,
                          SignalEvaluationService evaluationService,
                          BinancePriceService binancePriceService) {
        this.signalRepository = signalRepository;
        this.evaluationService = evaluationService;
        this.binancePriceService = binancePriceService;
    }

    @Transactional
    public SignalResponse createSignal(CreateSignalRequest request) {
        Signal signal = new Signal();
        signal.setSymbol(request.getSymbol());
        signal.setDirection(request.getDirection());
        signal.setEntryPrice(request.getEntryPrice());
        signal.setStopLoss(request.getStopLoss());
        signal.setTargetPrice(request.getTargetPrice());
        signal.setEntryTime(request.getEntryTime());
        signal.setExpiryTime(request.getExpiryTime());
        signal.setStatus(SignalStatus.OPEN);

        Signal saved = signalRepository.save(signal);
        return SignalResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SignalResponse> getAllSignals() {
        return signalRepository.findAll()
                .stream()
                .map(this::evaluateAndMaybePersist)
                .map(SignalResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public SignalResponse getSignalById(Long id) {
        Signal signal = findSignalOrThrow(id);
        Signal evaluated = evaluateAndMaybePersist(signal);
        return SignalResponse.fromEntity(evaluated);
    }

    @Transactional
    public void deleteSignal(Long id) {
        Signal signal = findSignalOrThrow(id);
        signalRepository.delete(signal);
    }

    /**
     * Re-evaluates a signal's status against the live Binance price every
     * time it's read. If the status changes (OPEN -> TARGET_HIT, etc.), the
     * new status and ROI are persisted immediately so the DB never serves
     * stale state on the next read.
     *
     * If a final state is already reached, we skip the Binance call entirely
     * — no point spending an API call on a signal that can never change again.
     */
    private Signal evaluateAndMaybePersist(Signal signal) {
        if (signal.getStatus().isFinal()) {
            return signal;
        }

        BigDecimal currentPrice = binancePriceService.getCurrentPrice(signal.getSymbol());
        SignalStatus newStatus = evaluationService.evaluateStatus(signal, currentPrice);

        if (newStatus != signal.getStatus()) {
            signal.setStatus(newStatus);
            if (newStatus.isFinal()) {
                BigDecimal roi = evaluationService.calculateRoi(signal, currentPrice);
                signal.setRealizedRoi(roi);
            }
            signal = signalRepository.save(signal);
        }

        return signal;
    }

    private Signal findSignalOrThrow(Long id) {
        return signalRepository.findById(id)
                .orElseThrow(() -> new SignalNotFoundException(id));
    }
}