package org.example.binancetestservice.dto;

import java.math.BigDecimal;

public record AveragePriceResponse(
        BigDecimal averagePrice
) {
}