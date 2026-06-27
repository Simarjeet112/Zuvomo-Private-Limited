package com.tradingsignal.tracker.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BinanceTickerResponse {
    private String symbol;
    private BigDecimal price;
}