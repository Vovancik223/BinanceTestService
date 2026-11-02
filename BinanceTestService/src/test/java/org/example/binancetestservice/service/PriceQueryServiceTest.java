package org.example.binancetestservice.service;

import org.example.binancetestservice.config.BinanceProperties;
import org.example.binancetestservice.dto.AveragePriceResponse;
import org.example.binancetestservice.dto.PricePageResponse;
import org.example.binancetestservice.dto.PriceResponse;
import org.example.binancetestservice.dto.PriceTrendResponse;
import org.example.binancetestservice.entity.PriceRecord;
import org.example.binancetestservice.exception.PriceNotFoundException;
import org.example.binancetestservice.repository.PriceRecordRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceQueryServiceTest {

    @Mock
    private PriceRecordRepository repository;

    private PriceQueryService service;

    private final BinanceProperties properties =
            new BinanceProperties(
                    "https://api.binance.com",
                    "BNBUSDT",
                    Duration.ofSeconds(30)
            );

    private final Instant from =
            Instant.parse("2026-10-01T00:00:00Z");

    private final Instant to =
            Instant.parse("2026-11-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        service = new PriceQueryService(repository, properties);
    }

    @Test
    void getAveragePriceShouldReturnAverage() {

        BigDecimal expectedAverage = new BigDecimal("850.50");

        when(repository.calculateAverage(
                properties.symbol(),
                from,
                to
        )).thenReturn(expectedAverage);

        AveragePriceResponse response =
                service.getAveragePrice(from, to);

        assertEquals(
                0,
                expectedAverage.compareTo(response.averagePrice())
        );

        verify(repository).calculateAverage(
                properties.symbol(),
                from,
                to
        );
    }

    @Test
    void getAveragePriceShouldThrowWhenNoRecordsFound() {

        when(repository.calculateAverage(
                properties.symbol(),
                from,
                to
        )).thenReturn(null);

        assertThrows(
                PriceNotFoundException.class,
                () -> service.getAveragePrice(from, to)
        );
    }

    @Test
    void getTrendShouldReturnUpward() {

        PriceRecord first = createRecord(
                "800.00",
                "2026-10-01T10:00:00Z"
        );

        PriceRecord last = createRecord(
                "900.00",
                "2026-10-02T10:00:00Z"
        );

        mockFirstAndLast(first, last);

        PriceTrendResponse response =
                service.getTrend(from, to);

        assertEquals(
                PriceTrendResponse.Trend.UPWARD,
                response.trend()
        );
    }

    @Test
    void getTrendShouldReturnDownward() {

        PriceRecord first = createRecord(
                "900.00",
                "2026-10-01T10:00:00Z"
        );

        PriceRecord last = createRecord(
                "800.00",
                "2026-10-02T10:00:00Z"
        );

        mockFirstAndLast(first, last);

        PriceTrendResponse response =
                service.getTrend(from, to);

        assertEquals(
                PriceTrendResponse.Trend.DOWNWARD,
                response.trend()
        );
    }

    @Test
    void getTrendShouldReturnFlatWhenPricesAreEqual() {

        PriceRecord first = createRecord(
                "850.0",
                "2026-10-01T10:00:00Z"
        );

        PriceRecord last = createRecord(
                "850.00",
                "2026-10-02T10:00:00Z"
        );

        mockFirstAndLast(first, last);

        PriceTrendResponse response =
                service.getTrend(from, to);

        assertEquals(
                PriceTrendResponse.Trend.FLAT,
                response.trend()
        );
    }

    @Test
    void getTrendShouldThrowWhenNoRecordsFound() {

        when(repository
                .findFirstBySymbolAndFetchedAtGreaterThanEqualAndFetchedAtLessThanOrderByFetchedAtAscIdAsc(
                        properties.symbol(),
                        from,
                        to
                )).thenReturn(Optional.empty());

        assertThrows(
                PriceNotFoundException.class,
                () -> service.getTrend(from, to)
        );
    }

    @Test
    void getLatestPriceShouldReturnLatestRecord() {

        Instant fetchedAt = Instant.parse("2026-10-15T12:00:00Z");

        PriceRecord record = new PriceRecord(
                properties.symbol(),
                new BigDecimal("850.50"),
                fetchedAt
        );

        when(repository.findFirstBySymbolOrderByFetchedAtDescIdDesc(
                properties.symbol()
        )).thenReturn(Optional.of(record));

        PriceResponse response = service.getLatestPrice();

        assertEquals(properties.symbol(), response.symbol());
        assertEquals(0, new BigDecimal("850.50").compareTo(response.price()));
        assertEquals(fetchedAt, response.fetchedAt());

        verify(repository).findFirstBySymbolOrderByFetchedAtDescIdDesc(
                properties.symbol()
        );
    }

    @Test
    void getLatestPriceShouldThrowWhenNoRecordsFound() {

        when(repository.findFirstBySymbolOrderByFetchedAtDescIdDesc(
                properties.symbol()
        )).thenReturn(Optional.empty());

        assertThrows(
                PriceNotFoundException.class,
                () -> service.getLatestPrice()
        );
    }

    @Test
    void getPricesShouldReturnPaginatedResults() {

        int page = 1;
        int size = 2;

        PriceRecord first = createRecord(
                "850.00",
                "2026-10-15T12:00:00Z"
        );

        PriceRecord second = createRecord(
                "851.00",
                "2026-10-15T11:00:00Z"
        );

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("fetchedAt"),
                        Sort.Order.desc("id")
                )
        );

        PageImpl<PriceRecord> repositoryResult = new PageImpl<>(
                List.of(first, second),
                pageable,
                5
        );

        when(repository
                .findBySymbolAndFetchedAtGreaterThanEqualAndFetchedAtLessThan(
                        properties.symbol(),
                        from,
                        to,
                        pageable
                )).thenReturn(repositoryResult);

        PricePageResponse response = service.getPrices(
                from,
                to,
                page,
                size
        );

        assertEquals(2, response.items().size());
        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(5, response.totalElements());
        assertTrue(response.hasNext());

        assertEquals(
                0,
                new BigDecimal("850.00").compareTo(
                        response.items().get(0).price()
                )
        );

        verify(repository)
                .findBySymbolAndFetchedAtGreaterThanEqualAndFetchedAtLessThan(
                        properties.symbol(),
                        from,
                        to,
                        pageable
                );
    }



    private void mockFirstAndLast(
            PriceRecord first,
            PriceRecord last
    ) {
        when(repository
                .findFirstBySymbolAndFetchedAtGreaterThanEqualAndFetchedAtLessThanOrderByFetchedAtAscIdAsc(
                        properties.symbol(),
                        from,
                        to
                )).thenReturn(Optional.of(first));

        when(repository
                .findFirstBySymbolAndFetchedAtGreaterThanEqualAndFetchedAtLessThanOrderByFetchedAtDescIdDesc(
                        properties.symbol(),
                        from,
                        to
                )).thenReturn(Optional.of(last));
    }

    private PriceRecord createRecord(
            String price,
            String fetchedAt
    ) {
        return new PriceRecord(
                properties.symbol(),
                new BigDecimal(price),
                Instant.parse(fetchedAt)
        );
    }
}