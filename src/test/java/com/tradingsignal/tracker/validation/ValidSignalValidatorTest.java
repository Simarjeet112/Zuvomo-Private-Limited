package com.tradingsignal.tracker.validation;

import com.tradingsignal.tracker.dto.CreateSignalRequest;
import com.tradingsignal.tracker.entity.Direction;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidSignalValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private CreateSignalRequest validBuyRequest() {
        CreateSignalRequest request = new CreateSignalRequest();
        request.setSymbol("BTCUSDT");
        request.setDirection(Direction.BUY);
        request.setEntryPrice(new BigDecimal("100"));
        request.setStopLoss(new BigDecimal("90"));
        request.setTargetPrice(new BigDecimal("120"));
        request.setEntryTime(Instant.now().minus(1, ChronoUnit.HOURS));
        request.setExpiryTime(Instant.now().plus(1, ChronoUnit.HOURS));
        return request;
    }

    private CreateSignalRequest validSellRequest() {
        CreateSignalRequest request = new CreateSignalRequest();
        request.setSymbol("ETHUSDT");
        request.setDirection(Direction.SELL);
        request.setEntryPrice(new BigDecimal("100"));
        request.setStopLoss(new BigDecimal("110"));
        request.setTargetPrice(new BigDecimal("80"));
        request.setEntryTime(Instant.now().minus(1, ChronoUnit.HOURS));
        request.setExpiryTime(Instant.now().plus(1, ChronoUnit.HOURS));
        return request;
    }

    @Test
    void validBuySignal_hasNoViolations() {
        Set<ConstraintViolation<CreateSignalRequest>> violations = validator.validate(validBuyRequest());
        assertThat(violations).isEmpty();
    }

    @Test
    void validSellSignal_hasNoViolations() {
        Set<ConstraintViolation<CreateSignalRequest>> violations = validator.validate(validSellRequest());
        assertThat(violations).isEmpty();
    }

    @Test
    void buySignal_withStopLossAboveEntry_isInvalid() {
        CreateSignalRequest request = validBuyRequest();
        request.setStopLoss(new BigDecimal("105")); // violates: stopLoss must be < entryPrice for BUY

        Set<ConstraintViolation<CreateSignalRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void buySignal_withTargetBelowEntry_isInvalid() {
        CreateSignalRequest request = validBuyRequest();
        request.setTargetPrice(new BigDecimal("95")); // violates: targetPrice must be > entryPrice for BUY

        Set<ConstraintViolation<CreateSignalRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void sellSignal_withStopLossBelowEntry_isInvalid() {
        CreateSignalRequest request = validSellRequest();
        request.setStopLoss(new BigDecimal("95")); // violates: stopLoss must be > entryPrice for SELL

        Set<ConstraintViolation<CreateSignalRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void sellSignal_withTargetAboveEntry_isInvalid() {
        CreateSignalRequest request = validSellRequest();
        request.setTargetPrice(new BigDecimal("105")); // violates: targetPrice must be < entryPrice for SELL

        Set<ConstraintViolation<CreateSignalRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void expiryTime_beforeEntryTime_isInvalid() {
        CreateSignalRequest request = validBuyRequest();
        request.setExpiryTime(request.getEntryTime().minus(1, ChronoUnit.HOURS)); // expiry before entry

        Set<ConstraintViolation<CreateSignalRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void entryTime_moreThan24HoursInPast_isInvalid() {
        CreateSignalRequest request = validBuyRequest();
        request.setEntryTime(Instant.now().minus(25, ChronoUnit.HOURS)); // too far back

        Set<ConstraintViolation<CreateSignalRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void entryTime_exactly24HoursInPast_isValid() {
        CreateSignalRequest request = validBuyRequest();
        // just inside the 24h window — proves the boundary is inclusive of "up to 24 hours"
        request.setEntryTime(Instant.now().minus(23, ChronoUnit.HOURS).minus(59, ChronoUnit.MINUTES));

        Set<ConstraintViolation<CreateSignalRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void entryTime_inTheFuture_isInvalid() {
        CreateSignalRequest request = validBuyRequest();
        request.setEntryTime(Instant.now().plus(1, ChronoUnit.HOURS)); // future entry not allowed

        Set<ConstraintViolation<CreateSignalRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }
}