package com.tradingsignal.tracker.service;

import com.tradingsignal.tracker.entity.Direction;
import com.tradingsignal.tracker.entity.Signal;
import com.tradingsignal.tracker.entity.SignalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SignalEvaluationServiceTest {

    private SignalEvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        evaluationService = new SignalEvaluationService();
    }

    private Signal buyOpenSignal() {
        Signal signal = new Signal();
        signal.setDirection(Direction.BUY);
        signal.setEntryPrice(new BigDecimal("100"));
        signal.setStopLoss(new BigDecimal("90"));
        signal.setTargetPrice(new BigDecimal("120"));
        signal.setEntryTime(Instant.now().minus(1, ChronoUnit.HOURS));
        signal.setExpiryTime(Instant.now().plus(1, ChronoUnit.HOURS));
        signal.setStatus(SignalStatus.OPEN);
        return signal;
    }

    private Signal sellOpenSignal() {
        Signal signal = new Signal();
        signal.setDirection(Direction.SELL);
        signal.setEntryPrice(new BigDecimal("100"));
        signal.setStopLoss(new BigDecimal("110"));
        signal.setTargetPrice(new BigDecimal("80"));
        signal.setEntryTime(Instant.now().minus(1, ChronoUnit.HOURS));
        signal.setExpiryTime(Instant.now().plus(1, ChronoUnit.HOURS));
        signal.setStatus(SignalStatus.OPEN);
        return signal;
    }

    @Nested
    @DisplayName("BUY signal status logic")
    class BuySignalTests {

        @Test
        @DisplayName("price >= target -> TARGET_HIT")
        void buyHitsTarget() {
            Signal signal = buyOpenSignal();
            SignalStatus status = evaluationService.evaluateStatus(signal, new BigDecimal("120"));
            assertThat(status).isEqualTo(SignalStatus.TARGET_HIT);
        }

        @Test
        @DisplayName("price above target also -> TARGET_HIT")
        void buyHitsTargetAbove() {
            Signal signal = buyOpenSignal();
            SignalStatus status = evaluationService.evaluateStatus(signal, new BigDecimal("125"));
            assertThat(status).isEqualTo(SignalStatus.TARGET_HIT);
        }

        @Test
        @DisplayName("price <= stopLoss -> STOPLOSS_HIT")
        void buyHitsStopLoss() {
            Signal signal = buyOpenSignal();
            SignalStatus status = evaluationService.evaluateStatus(signal, new BigDecimal("90"));
            assertThat(status).isEqualTo(SignalStatus.STOPLOSS_HIT);
        }

        @Test
        @DisplayName("price between stopLoss and target -> stays OPEN")
        void buyStaysOpen() {
            Signal signal = buyOpenSignal();
            SignalStatus status = evaluationService.evaluateStatus(signal, new BigDecimal("105"));
            assertThat(status).isEqualTo(SignalStatus.OPEN);
        }
    }

    @Nested
    @DisplayName("SELL signal status logic")
    class SellSignalTests {

        @Test
        @DisplayName("price <= target -> TARGET_HIT")
        void sellHitsTarget() {
            Signal signal = sellOpenSignal();
            SignalStatus status = evaluationService.evaluateStatus(signal, new BigDecimal("80"));
            assertThat(status).isEqualTo(SignalStatus.TARGET_HIT);
        }

        @Test
        @DisplayName("price >= stopLoss -> STOPLOSS_HIT")
        void sellHitsStopLoss() {
            Signal signal = sellOpenSignal();
            SignalStatus status = evaluationService.evaluateStatus(signal, new BigDecimal("110"));
            assertThat(status).isEqualTo(SignalStatus.STOPLOSS_HIT);
        }

        @Test
        @DisplayName("price between target and stopLoss -> stays OPEN")
        void sellStaysOpen() {
            Signal signal = sellOpenSignal();
            SignalStatus status = evaluationService.evaluateStatus(signal, new BigDecimal("95"));
            assertThat(status).isEqualTo(SignalStatus.OPEN);
        }
    }

    @Nested
    @DisplayName("Expiry logic")
    class ExpiryTests {

        @Test
        @DisplayName("expiry passed, no price hit -> EXPIRED")
        void expiresWhenPastExpiryAndNoHit() {
            Signal signal = buyOpenSignal();
            signal.setExpiryTime(Instant.now().minus(1, ChronoUnit.MINUTES)); // already expired
            SignalStatus status = evaluationService.evaluateStatus(signal, new BigDecimal("105")); // no hit
            assertThat(status).isEqualTo(SignalStatus.EXPIRED);
        }

        @Test
        @DisplayName("expiry passed BUT target hit -> TARGET_HIT wins, not EXPIRED")
        void targetHitTakesPriorityOverExpiry() {
            Signal signal = buyOpenSignal();
            signal.setExpiryTime(Instant.now().minus(1, ChronoUnit.MINUTES)); // already expired
            SignalStatus status = evaluationService.evaluateStatus(signal, new BigDecimal("120")); // target hit
            assertThat(status).isEqualTo(SignalStatus.TARGET_HIT);
        }
    }

    @Nested
    @DisplayName("Final state immutability")
    class FinalStateTests {

        @Test
        @DisplayName("TARGET_HIT never transitions, even if price now hits stop loss")
        void targetHitIsFinal() {
            Signal signal = buyOpenSignal();
            signal.setStatus(SignalStatus.TARGET_HIT);
            SignalStatus status = evaluationService.evaluateStatus(signal, new BigDecimal("50")); // would be stop loss
            assertThat(status).isEqualTo(SignalStatus.TARGET_HIT);
        }

        @Test
        @DisplayName("EXPIRED never transitions, even if price now hits target")
        void expiredIsFinal() {
            Signal signal = buyOpenSignal();
            signal.setStatus(SignalStatus.EXPIRED);
            SignalStatus status = evaluationService.evaluateStatus(signal, new BigDecimal("120")); // would be target hit
            assertThat(status).isEqualTo(SignalStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("ROI calculation")
    class RoiTests {

        @Test
        @DisplayName("BUY ROI: (current - entry) / entry * 100")
        void buyRoiPositive() {
            Signal signal = buyOpenSignal(); // entry = 100
            BigDecimal roi = evaluationService.calculateRoi(signal, new BigDecimal("120"));
            assertThat(roi).isEqualTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("BUY ROI: negative when price drops")
        void buyRoiNegative() {
            Signal signal = buyOpenSignal(); // entry = 100
            BigDecimal roi = evaluationService.calculateRoi(signal, new BigDecimal("90"));
            assertThat(roi).isEqualTo(new BigDecimal("-10.00"));
        }

        @Test
        @DisplayName("SELL ROI: (entry - current) / entry * 100")
        void sellRoiPositive() {
            Signal signal = sellOpenSignal(); // entry = 100
            BigDecimal roi = evaluationService.calculateRoi(signal, new BigDecimal("80"));
            assertThat(roi).isEqualTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("SELL ROI: negative when price rises")
        void sellRoiNegative() {
            Signal signal = sellOpenSignal(); // entry = 100
            BigDecimal roi = evaluationService.calculateRoi(signal, new BigDecimal("110"));
            assertThat(roi).isEqualTo(new BigDecimal("-10.00"));
        }
    }
}