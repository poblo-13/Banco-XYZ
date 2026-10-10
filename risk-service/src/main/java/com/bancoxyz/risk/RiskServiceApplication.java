package com.bancoxyz.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class RiskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                RiskServiceApplication.class, args);
    }
}
