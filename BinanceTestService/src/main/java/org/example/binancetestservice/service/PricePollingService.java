package org.example.binancetestservice.service;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.example.binancetestservice.client.BinanceAveragePriceResponse;
import org.example.binancetestservice.client.BinanceClient;
import org.example.binancetestservice.config.BinanceProperties;
import org.example.binancetestservice.entity.PriceRecord;
import org.example.binancetestservice.repository.PriceRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class PricePollingService {

    private static final Logger log =
            LoggerFactory.getLogger(PricePollingService.class);

    private final BinanceClient binanceClient;
    private final PriceRecordRepository repository;
    private final BinanceProperties properties;
    private final Clock clock;

    public PricePollingService(
            BinanceClient binanceClient,
            PriceRecordRepository repository,
            BinanceProperties properties,
            Clock clock
    ) {
        this.binanceClient = binanceClient;
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public void fetchAndSave() {

        BinanceAveragePriceResponse response =
                binanceClient.fetchAveragePrice();

        if (response == null
                || response.price() == null
                || response.price().signum() <= 0) {

            throw new IllegalStateException(
                    "Binance returned an invalid average price"
            );
        }

        Instant fetchedAt = Instant.now(clock);

        PriceRecord record = new PriceRecord(
                properties.symbol(),
                response.price(),
                fetchedAt
        );

        repository.save(record);

        log.info(
                "Saved price: symbol={}, price={}, fetchedAt={}",
                properties.symbol(),
                response.price(),
                fetchedAt
        );
    }
}