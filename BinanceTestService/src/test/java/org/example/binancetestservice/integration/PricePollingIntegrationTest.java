package org.example.binancetestservice.integration;



import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.binancetestservice.entity.PriceRecord;
import org.example.binancetestservice.exception.BinanceApiException;
import org.example.binancetestservice.repository.PriceRecordRepository;
import org.example.binancetestservice.service.PricePollingService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Instant;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PricePollingIntegrationTest {

    private static final WireMockServer wireMockServer =
            new WireMockServer(wireMockConfig().dynamicPort());

    static {
        wireMockServer.start();
    }

    @Autowired
    private PricePollingService pricePollingService;

    @Autowired
    private PriceRecordRepository repository;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("binance.base-url", wireMockServer::baseUrl);
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        wireMockServer.resetAll();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void shouldFetchPriceSaveItAndReturnThroughApi() {

        // Arrange: имитируем ответ Binance
        wireMockServer.stubFor(
                get(urlPathEqualTo("/api/v3/avgPrice"))
                        .withQueryParam("symbol", equalTo("BNBUSDT"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                                {
                                                  "mins": 5,
                                                  "price": "850.12345678",
                                                  "closeTime": 1790000000000
                                                }
                                                """)
                        )
        );

        // Act: запускаем получение и сохранение цены
        pricePollingService.fetchAndSave();

        // Assert: проверяем, что запись появилась в PostgreSQL
        var savedRecord = repository
                .findFirstBySymbolOrderByFetchedAtDescIdDesc("BNBUSDT");

        assertThat(savedRecord).isPresent();
        assertThat(savedRecord.get().getPrice())
                .isEqualByComparingTo("850.12345678");

        // Assert: проверяем REST API
        var response = restTemplate.getForEntity(
                "/api/v1/prices/latest",
                JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("symbol").asText())
                .isEqualTo("BNBUSDT");
        assertThat(response.getBody().get("price").decimalValue())
                .isEqualByComparingTo("850.12345678");
    }

    @Test
    void shouldNotSavePriceWhenBinanceReturnsZeroPrice() {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/api/v3/avgPrice"))
                        .withQueryParam("symbol", equalTo("BNBUSDT"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                            {
                                              "mins": 5,
                                              "price": "0",
                                              "closeTime": 1790000000000
                                            }
                                            """)
                        )
        );

        assertThatThrownBy(() -> pricePollingService.fetchAndSave())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Binance returned an invalid average price");

        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldNotSavePriceWhenBinanceReturnsServerError() {
        wireMockServer.stubFor(
                get(urlPathEqualTo("/api/v3/avgPrice"))
                        .withQueryParam("symbol", equalTo("BNBUSDT"))
                        .willReturn(
                                aResponse()
                                        .withStatus(500)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                            {
                                              "code": -1000,
                                              "msg": "Internal server error"
                                            }
                                            """)
                        )
        );

        assertThatThrownBy(() -> pricePollingService.fetchAndSave())
                .isInstanceOf(BinanceApiException.class);

        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldReturn404WhenNoPriceRecordsExist() {
        var response = restTemplate.getForEntity(
                "/api/v1/prices/latest",
                JsonNode.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldReturnAveragePriceForPeriod() {
        repository.save(new PriceRecord(
                "BNBUSDT",
                new BigDecimal("100.00"),
                Instant.parse("2026-10-20T10:00:00Z")
        ));

        repository.save(new PriceRecord(
                "BNBUSDT",
                new BigDecimal("200.00"),
                Instant.parse("2026-10-20T11:00:00Z")
        ));

        var response = restTemplate.getForEntity(
                "/api/v1/prices/average?from=2026-10-20T00:00:00Z&to=2026-10-21T00:00:00Z",
                JsonNode.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("averagePrice").decimalValue())
                .isEqualByComparingTo("150.00");
    }


}