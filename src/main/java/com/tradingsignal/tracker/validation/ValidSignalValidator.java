package com.tradingsignal.tracker.validation;

import com.tradingsignal.tracker.dto.CreateSignalRequest;
import com.tradingsignal.tracker.entity.Direction;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public class ValidSignalValidator implements ConstraintValidator<ValidSignal, CreateSignalRequest> {

    private static final Duration MAX_HISTORICAL_WINDOW = Duration.ofHours(24);

    @Override
    public boolean isValid(CreateSignalRequest request, ConstraintValidatorContext context) {
        // Don't double-report errors already caught by @NotNull on individual fields.
        // If any required field is null at this point, skip cross-field checks —
        // there's nothing meaningful to compare yet, and the field-level message
        // already tells the user what's missing.
        if (request.getDirection() == null
                || request.getEntryPrice() == null
                || request.getStopLoss() == null
                || request.getTargetPrice() == null
                || request.getEntryTime() == null
                || request.getExpiryTime() == null) {
            return true;
        }

        // Disable Jakarta's default message; we'll build our own per-violation messages.
        context.disableDefaultConstraintViolation();

        boolean valid = true;

        valid &= validatePriceRules(request, context);
        valid &= validateTimeRules(request, context);

        return valid;
    }

    private boolean validatePriceRules(CreateSignalRequest request, ConstraintValidatorContext context) {
        Direction direction = request.getDirection();
        BigDecimal entry = request.getEntryPrice();
        BigDecimal stopLoss = request.getStopLoss();
        BigDecimal target = request.getTargetPrice();

        if (direction == Direction.BUY) {
            if (stopLoss.compareTo(entry) >= 0) {
                addViolation(context, "stopLoss", "For BUY signals, stopLoss must be less than entryPrice");
                return false;
            }
            if (target.compareTo(entry) <= 0) {
                addViolation(context, "targetPrice", "For BUY signals, targetPrice must be greater than entryPrice");
                return false;
            }
        } else if (direction == Direction.SELL) {
            if (stopLoss.compareTo(entry) <= 0) {
                addViolation(context, "stopLoss", "For SELL signals, stopLoss must be greater than entryPrice");
                return false;
            }
            if (target.compareTo(entry) >= 0) {
                addViolation(context, "targetPrice", "For SELL signals, targetPrice must be less than entryPrice");
                return false;
            }
        }
        return true;
    }

    private boolean validateTimeRules(CreateSignalRequest request, ConstraintValidatorContext context) {
        Instant now = Instant.now();
        Instant entryTime = request.getEntryTime();
        Instant expiryTime = request.getExpiryTime();
        boolean valid = true;

        // expiry must be strictly after entry
        if (!expiryTime.isAfter(entryTime)) {
            addViolation(context, "expiryTime", "expiryTime must be after entryTime");
            valid = false;
        }

        // entryTime can't be in the future
        if (entryTime.isAfter(now)) {
            addViolation(context, "entryTime", "entryTime cannot be in the future");
            valid = false;
        }

        // entryTime can't be more than 24 hours in the past
        Instant earliestAllowed = now.minus(MAX_HISTORICAL_WINDOW);
        if (entryTime.isBefore(earliestAllowed)) {
            addViolation(context, "entryTime", "entryTime cannot be more than 24 hours in the past");
            valid = false;
        }

        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}