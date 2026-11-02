package org.example.binancetestservice;

import org.example.binancetestservice.config.BinanceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BinanceProperties.class)
public class BinanceTestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BinanceTestServiceApplication.class, args);
    }

}
