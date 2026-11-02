package org.example.binancetestservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BinanceAveragePriceResponse(
        Integer mins,
        BigDecimal price,
        Long closeTime,
        String symbol
) {
}