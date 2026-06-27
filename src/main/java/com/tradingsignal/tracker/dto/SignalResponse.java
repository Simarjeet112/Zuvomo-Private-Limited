package com.tradingsignal.tracker.dto;

import com.tradingsignal.tracker.entity.Direction;
import com.tradingsignal.tracker.entity.Signal;
import com.tradingsignal.tracker.entity.SignalStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class SignalResponse {

    private Long id;
    private String symbol;
    private Direction direction;
    private BigDecimal entryPrice;
    private BigDecimal stopLoss;
    private BigDecimal targetPrice;
    private Instant entryTime;
    private Instant expiryTime;
    private Instant createdAt;
    private SignalStatus status;
    private BigDecimal realizedRoi;

    // Maps entity -> response DTO. Keeping this mapping logic here (not in the
    // service or controller) means there's exactly one place that knows how
    // a Signal becomes a SignalResponse.
    public static SignalResponse fromEntity(Signal signal) {
        SignalResponse response = new SignalResponse();
        response.setId(signal.getId());
        response.setSymbol(signal.getSymbol());
        response.setDirection(signal.getDirection());
        response.setEntryPrice(signal.getEntryPrice());
        response.setStopLoss(signal.getStopLoss());
        response.setTargetPrice(signal.getTargetPrice());
        response.setEntryTime(signal.getEntryTime());
        response.setExpiryTime(signal.getExpiryTime());
        response.setCreatedAt(signal.getCreatedAt());
        response.setStatus(signal.getStatus());
        response.setRealizedRoi(signal.getRealizedRoi());
        return response;
    }
}