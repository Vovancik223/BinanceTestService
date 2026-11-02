package org.example.binancetestservice.scheduler;

import org.example.binancetestservice.config.BinanceProperties;
import org.example.binancetestservice.exception.BinanceApiException;
import org.example.binancetestservice.service.PricePollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class PricePollingScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(PricePollingScheduler.class);

    private static final Duration DEFAULT_429_DELAY =
            Duration.ofSeconds(60);

    private static final Duration DEFAULT_418_DELAY =
            Duration.ofMinutes(5);

    private final PricePollingService pollingService;
    private final BinanceProperties properties;

    private Instant nextAllowedPollTime = Instant.MIN;

    public PricePollingScheduler(
            PricePollingService pollingService,
            BinanceProperties properties
    ) {
        this.pollingService = pollingService;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${binance.poll-interval:PT30S}",
            initialDelayString = "${binance.poll-interval:PT30S}"
    )
    public void poll() {

        Instant now = Instant.now();

        if (now.isBefore(nextAllowedPollTime)) {
            log.warn(
                    "Polling skipped for symbol {}. Next attempt is allowed at {}",
                    properties.symbol(),
                    nextAllowedPollTime
            );
            return;
        }

        try {
            pollingService.fetchAndSave();

        } catch (BinanceApiException exception) {
            handleBinanceError(exception);

        } catch (Exception exception) {
            log.error(
                    "Failed to fetch price for symbol {}. "
                            + "The next scheduled poll will continue.",
                    properties.symbol(),
                    exception
            );
        }
    }

    private void handleBinanceError(BinanceApiException exception) {

        int statusCode = exception.getStatusCode();

        if (statusCode == 429 || statusCode == 418) {

            Duration delay = exception.getRetryAfter();

            if (delay == null) {
                delay = statusCode == 429
                        ? DEFAULT_429_DELAY
                        : DEFAULT_418_DELAY;
            }

            nextAllowedPollTime = Instant.now().plus(delay);

            log.warn(
                    "Binance returned HTTP {} for symbol {}. "
                            + "Polling suspended until {} ({} seconds).",
                    statusCode,
                    properties.symbol(),
                    nextAllowedPollTime,
                    delay.toSeconds()
            );

        } else {
            log.error(
                    "Binance API error for symbol {}. HTTP status: {}",
                    properties.symbol(),
                    statusCode,
                    exception
            );
        }
    }
}