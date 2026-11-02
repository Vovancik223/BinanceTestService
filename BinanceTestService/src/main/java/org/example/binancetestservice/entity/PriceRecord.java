package org.example.binancetestservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "price_records",
        indexes = {
                @Index(
                        name = "idx_price_records_symbol_fetched_at",
                        columnList = "symbol, fetched_at"
                )
        }
)
public class PriceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 30, scale = 15)
    private BigDecimal price;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected PriceRecord() {
        // Конструктор для JPA
    }

    public PriceRecord(
            String symbol,
            BigDecimal price,
            Instant fetchedAt
    ) {
        this.symbol = symbol;
        this.price = price;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}