package org.example.binancetestservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceResponse(
        String symbol,
        BigDecimal price,
        Instant fetchedAt
) {
}