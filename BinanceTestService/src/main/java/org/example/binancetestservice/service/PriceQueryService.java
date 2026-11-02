package org.example.binancetestservice.service;

import org.example.binancetestservice.config.BinanceProperties;
import org.example.binancetestservice.dto.AveragePriceResponse;
import org.example.binancetestservice.dto.PricePageResponse;
import org.example.binancetestservice.dto.PriceResponse;
import org.example.binancetestservice.dto.PriceTrendResponse;
import org.example.binancetestservice.entity.PriceRecord;
import org.example.binancetestservice.exception.PriceNotFoundException;
import org.example.binancetestservice.repository.PriceRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@Transactional(readOnly = true)
public class PriceQueryService {

    private final PriceRecordRepository repository;
    private final BinanceProperties properties;

    public PriceQueryService(
            PriceRecordRepository repository,
            BinanceProperties properties
    ) {
        this.repository = repository;
        this.properties = properties;
    }

    public PriceResponse getLatestPrice() {
        PriceRecord record = repository
                .findFirstBySymbolOrderByFetchedAtDescIdDesc(
                        properties.symbol()
                )
                .orElseThrow(() -> new PriceNotFoundException(
                        "No price records found for "
                                + properties.symbol()
                ));

        return toResponse(record);
    }

    public AveragePriceResponse getAveragePrice(
            Instant from,
            Instant to
    ) {
        BigDecimal average = repository.calculateAverage(
                properties.symbol(),
                from,
                to
        );

        if (average == null) {
            throw new PriceNotFoundException(
                    "No price records found for the specified period"
            );
        }

        return new AveragePriceResponse(average);
    }

    public PricePageResponse getPrices(
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("fetchedAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<PriceRecord> result = repository
                .findBySymbolAndFetchedAtGreaterThanEqualAndFetchedAtLessThan(
                        properties.symbol(),
                        from,
                        to,
                        pageable
                );

        return new PricePageResponse(
                result.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.hasNext()
        );
    }

    public PriceTrendResponse getTrend(
            Instant from,
            Instant to
    ) {
        PriceRecord first = repository
                .findFirstBySymbolAndFetchedAtGreaterThanEqualAndFetchedAtLessThanOrderByFetchedAtAscIdAsc(
                        properties.symbol(),
                        from,
                        to
                )
                .orElseThrow(() -> new PriceNotFoundException(
                        "No price records found for the specified period"
                ));

        PriceRecord last = repository
                .findFirstBySymbolAndFetchedAtGreaterThanEqualAndFetchedAtLessThanOrderByFetchedAtDescIdDesc(
                        properties.symbol(),
                        from,
                        to
                )
                .orElseThrow(() -> new PriceNotFoundException(
                        "No price records found for the specified period"
                ));

        int comparison = last.getPrice().compareTo(first.getPrice());

        PriceTrendResponse.Trend trend;

        if (comparison > 0) {
            trend = PriceTrendResponse.Trend.UPWARD;
        } else if (comparison < 0) {
            trend = PriceTrendResponse.Trend.DOWNWARD;
        } else {
            trend = PriceTrendResponse.Trend.FLAT;
        }

        return new PriceTrendResponse(
                properties.symbol(),
                from.atZone(ZoneOffset.UTC).toLocalDate(),
                to.atZone(ZoneOffset.UTC).toLocalDate(),
                trend
        );
    }

    private PriceResponse toResponse(PriceRecord record) {
        return new PriceResponse(
                record.getSymbol(),
                record.getPrice(),
                record.getFetchedAt()
        );
    }
}