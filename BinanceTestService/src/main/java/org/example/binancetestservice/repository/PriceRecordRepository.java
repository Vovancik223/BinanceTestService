package org.example.binancetestservice.repository;

import org.example.binancetestservice.entity.PriceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface PriceRecordRepository
        extends JpaRepository<PriceRecord, Long> {

    Optional<PriceRecord>
    findFirstBySymbolOrderByFetchedAtDescIdDesc(String symbol);

    Page<PriceRecord> findBySymbolAndFetchedAtGreaterThanEqualAndFetchedAtLessThan(
            String symbol,
            Instant from,
            Instant to,
            Pageable pageable
    );

    @Query("""
        select avg(p.price)
        from PriceRecord p
        where p.symbol = :symbol
          and p.fetchedAt >= :from
          and p.fetchedAt < :to
        """)
    BigDecimal calculateAverage(
            @Param("symbol") String symbol,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    Optional<PriceRecord>
    findFirstBySymbolAndFetchedAtGreaterThanEqualAndFetchedAtLessThanOrderByFetchedAtAscIdAsc(
            String symbol,
            Instant from,
            Instant to
    );

    Optional<PriceRecord>
    findFirstBySymbolAndFetchedAtGreaterThanEqualAndFetchedAtLessThanOrderByFetchedAtDescIdDesc(
            String symbol,
            Instant from,
            Instant to
    );
}