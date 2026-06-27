package com.tradingsignal.tracker.exception;

public class SignalNotFoundException extends RuntimeException {
    public SignalNotFoundException(Long id) {
        super("Signal not found with id: " + id);
    }
}