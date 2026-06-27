package com.tradingsignal.tracker.entity;

public enum SignalStatus {
    OPEN,
    TARGET_HIT,
    STOPLOSS_HIT,
    EXPIRED;

    public boolean isFinal() {
        return this == TARGET_HIT || this == STOPLOSS_HIT || this == EXPIRED;
    }
}