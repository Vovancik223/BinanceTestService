package org.example.binancetestservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record PriceTrendResponse(
        String symbol,

        @JsonProperty("start_date")
        LocalDate startDate,

        @JsonProperty("end_date")
        LocalDate endDate,

        Trend trend
) {
    public enum Trend {
        UPWARD,
        DOWNWARD,
        FLAT
    }
}