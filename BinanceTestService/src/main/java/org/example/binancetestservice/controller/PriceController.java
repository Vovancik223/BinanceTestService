package org.example.binancetestservice.controller;

import org.example.binancetestservice.dto.*;
import org.example.binancetestservice.service.PriceQueryService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PriceQueryService priceQueryService;

    public PriceController(PriceQueryService priceQueryService) {
        this.priceQueryService = priceQueryService;
    }

    @GetMapping("/latest")
    public PriceResponse getLatestPrice() {
        return priceQueryService.getLatestPrice();
    }

    @GetMapping("/average")
    public AveragePriceResponse getAveragePrice(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        validatePeriod(from, to);

        return priceQueryService.getAveragePrice(from, to);
    }

    @GetMapping
    public PricePageResponse getPrices(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size
    ) {
        validatePeriod(from, to);

        return priceQueryService.getPrices(
                from,
                to,
                page,
                size
        );
    }

    @GetMapping("/trend")
    public PriceTrendResponse getTrend(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        validatePeriod(from, to);

        return priceQueryService.getTrend(from, to);
    }

    private void validatePeriod(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException(
                    "'from' must be earlier than 'to'"
            );
        }
    }
}