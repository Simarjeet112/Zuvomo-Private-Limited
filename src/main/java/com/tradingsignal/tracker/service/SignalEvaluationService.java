package com.tradingsignal.tracker.service;

import com.tradingsignal.tracker.entity.Direction;
import com.tradingsignal.tracker.entity.Signal;
import com.tradingsignal.tracker.entity.SignalStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class SignalEvaluationService {

    private static final int ROI_SCALE = 2;

    /**
     * Evaluates a signal against the current market price and returns the
     * resulting status. Does NOT mutate the signal — caller decides what to
     * persist. This keeps the method pure and easy to test in isolation.
     */
    public SignalStatus evaluateStatus(Signal signal, BigDecimal currentPrice) {
        // Final states never transition again — this is the core invariant
        // from the PDF: TARGET_HIT, STOPLOSS_HIT, EXPIRED are all terminal.
        if (signal.getStatus().isFinal()) {
            return signal.getStatus();
        }

        // Expiry is checked first: even if price conditions also happen to
        // be true the instant we check, the PDF's expiry rule only fires when
        // "no target or stop loss has been hit" — so we must check price hits
        // BEFORE falling back to expiry, not after. Order here matters.
        SignalStatus priceBasedStatus = evaluatePriceConditions(signal, currentPrice);
        if (priceBasedStatus != SignalStatus.OPEN) {
            return priceBasedStatus;
        }

        if (isExpired(signal)) {
            return SignalStatus.EXPIRED;
        }

        return SignalStatus.OPEN;
    }

    private SignalStatus evaluatePriceConditions(Signal signal, BigDecimal currentPrice) {
        Direction direction = signal.getDirection();
        BigDecimal target = signal.getTargetPrice();
        BigDecimal stopLoss = signal.getStopLoss();

        if (direction == Direction.BUY) {
            if (currentPrice.compareTo(target) >= 0) {
                return SignalStatus.TARGET_HIT;
            }
            if (currentPrice.compareTo(stopLoss) <= 0) {
                return SignalStatus.STOPLOSS_HIT;
            }
        } else { // SELL
            if (currentPrice.compareTo(target) <= 0) {
                return SignalStatus.TARGET_HIT;
            }
            if (currentPrice.compareTo(stopLoss) >= 0) {
                return SignalStatus.STOPLOSS_HIT;
            }
        }
        return SignalStatus.OPEN;
    }

    private boolean isExpired(Signal signal) {
        return Instant.now().isAfter(signal.getExpiryTime());
    }

    /**
     * ROI calculation per the PDF's formulas. Returns null if entryPrice is
     * zero (defensive — shouldn't happen given @Positive validation, but
     * division by zero would throw ArithmeticException otherwise).
     */
    public BigDecimal calculateRoi(Signal signal, BigDecimal currentPrice) {
        BigDecimal entry = signal.getEntryPrice();
        if (entry.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal diff = signal.getDirection() == Direction.BUY
                ? currentPrice.subtract(entry)
                : entry.subtract(currentPrice);

        return diff
                .divide(entry, 10, RoundingMode.HALF_UP) // extra precision before final rounding
                .multiply(BigDecimal.valueOf(100))
                .setScale(ROI_SCALE, RoundingMode.HALF_UP);
    }
}