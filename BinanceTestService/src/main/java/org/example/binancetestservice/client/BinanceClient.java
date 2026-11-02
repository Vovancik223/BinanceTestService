package org.example.binancetestservice.client;

import org.example.binancetestservice.config.BinanceProperties;
import org.example.binancetestservice.exception.BinanceApiException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class BinanceClient {

    private final RestClient restClient;
    private final BinanceProperties properties;

    public BinanceClient(
            RestClient binanceRestClient,
            BinanceProperties properties
    ) {
        this.restClient = binanceRestClient;
        this.properties = properties;
    }

    public BinanceAveragePriceResponse fetchAveragePrice() {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/avgPrice")
                            .queryParam("symbol", properties.symbol())
                            .build())
                    .retrieve()
                    .body(BinanceAveragePriceResponse.class);

        } catch (RestClientResponseException exception) {
            Duration retryAfter = parseRetryAfter(
                    exception.getResponseHeaders()
            );

            throw new BinanceApiException(
                    exception.getStatusCode().value(),
                    retryAfter,
                    "Binance API returned HTTP "
                            + exception.getStatusCode().value()
            );
        }
    }

    private Duration parseRetryAfter(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }

        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);

        if (value == null || value.isBlank()) {
            return null;
        }

        // Retry-After может содержать количество секунд.
        try {
            long seconds = Long.parseLong(value.trim());
            return Duration.ofSeconds(Math.max(0, seconds));
        } catch (NumberFormatException ignored) {
            // Если это не число, пробуем разобрать HTTP-дату.
        }

        // Retry-After также может содержать дату в формате HTTP-date.
        try {
            Instant retryTime = ZonedDateTime.parse(
                    value,
                    DateTimeFormatter.RFC_1123_DATE_TIME
            ).toInstant();

            Duration duration = Duration.between(
                    Instant.now(),
                    retryTime
            );

            return duration.isNegative() ? Duration.ZERO : duration;

        } catch (RuntimeException ignored) {
            return null;
        }
    }
}