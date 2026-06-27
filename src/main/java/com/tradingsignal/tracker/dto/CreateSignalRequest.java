package com.tradingsignal.tracker.dto;

import com.tradingsignal.tracker.entity.Direction;
import com.tradingsignal.tracker.validation.ValidSignal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@ValidSignal // our custom class-level validator — checks BUY/SELL + time rules together
public class CreateSignalRequest {

    @NotBlank(message = "symbol is required")
    private String symbol;

    @NotNull(message = "direction is required")
    private Direction direction;

    @NotNull(message = "entryPrice is required")
    @Positive(message = "entryPrice must be positive")
    private BigDecimal entryPrice;

    @NotNull(message = "stopLoss is required")
    @Positive(message = "stopLoss must be positive")
    private BigDecimal stopLoss;

    @NotNull(message = "targetPrice is required")
    @Positive(message = "targetPrice must be positive")
    private BigDecimal targetPrice;

    @NotNull(message = "entryTime is required")
    private Instant entryTime;

    @NotNull(message = "expiryTime is required")
    private Instant expiryTime;
}