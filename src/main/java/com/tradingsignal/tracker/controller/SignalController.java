package com.tradingsignal.tracker.controller;

import com.tradingsignal.tracker.dto.CreateSignalRequest;
import com.tradingsignal.tracker.dto.SignalResponse;
import com.tradingsignal.tracker.service.SignalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signals")
@Tag(name = "Trading Signals", description = "Create and track trading signals against live Binance prices")
public class SignalController {

    private final SignalService signalService;

    public SignalController(SignalService signalService) {
        this.signalService = signalService;
    }

    @PostMapping
    @Operation(summary = "Create a new trading signal")
    public ResponseEntity<SignalResponse> createSignal(@Valid @RequestBody CreateSignalRequest request) {
        SignalResponse response = signalService.createSignal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all trading signals, with live status evaluation")
    public ResponseEntity<List<SignalResponse>> getAllSignals() {
        return ResponseEntity.ok(signalService.getAllSignals());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single trading signal by id, with live status evaluation")
    public ResponseEntity<SignalResponse> getSignalById(@PathVariable Long id) {
        return ResponseEntity.ok(signalService.getSignalById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a trading signal")
    public ResponseEntity<Void> deleteSignal(@PathVariable Long id) {
        signalService.deleteSignal(id);
        return ResponseEntity.noContent().build();
    }
}