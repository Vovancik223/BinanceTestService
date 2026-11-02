package org.example.binancetestservice.exception;

import java.time.Duration;

public class BinanceApiException extends RuntimeException {

    private final int statusCode;
    private final Duration retryAfter;

    public BinanceApiException(
            int statusCode,
            Duration retryAfter,
            String message
    ) {
        super(message);
        this.statusCode = statusCode;
        this.retryAfter = retryAfter;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}