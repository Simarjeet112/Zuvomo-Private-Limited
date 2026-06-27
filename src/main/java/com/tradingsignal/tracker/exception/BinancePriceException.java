package com.tradingsignal.tracker.exception;

public class BinancePriceException extends RuntimeException {
    public BinancePriceException(String message, Throwable cause) {
        super(message, cause);
    }
}