package com.tradingsignal.tracker.service;

import com.tradingsignal.tracker.dto.BinanceTickerResponse;
import com.tradingsignal.tracker.exception.BinancePriceException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Service
public class BinancePriceService {

    private final RestClient binanceRestClient;

    public BinancePriceService(RestClient binanceRestClient) {
        this.binanceRestClient = binanceRestClient;
    }

    /**
     * Fetches the current price for a trading pair from Binance's public
     * ticker endpoint. No API key required — this is a public market data
     * endpoint, per the assignment spec.
     */
    public BigDecimal getCurrentPrice(String symbol) {
        try {
            BinanceTickerResponse response = binanceRestClient.get()
                    .uri("/api/v3/ticker/price?symbol={symbol}", symbol)
                    .retrieve()
                    .body(BinanceTickerResponse.class);

            if (response == null || response.getPrice() == null) {
                throw new BinancePriceException(
                        "Binance returned an empty price for symbol: " + symbol, null);
            }
            return response.getPrice();

        } catch (RestClientException ex) {
            throw new BinancePriceException(
                    "Failed to fetch price from Binance for symbol: " + symbol, ex);
        }
    }
}