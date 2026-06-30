package com.example.geocoder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import lombok.extern.slf4j.Slf4j;

/**
 * Главный класс приложения для геокодирования адресов
 *
 * @author Ксения
 * @version 1.0.0
 */
@Slf4j
@EnableAsync
@SpringBootApplication(scanBasePackages = "com.example.geocoder")
public class AddressGeocoderApplication implements CommandLineRunner {

    @Value("${api.yandex.api-key:NOT_SET}")
    private String yandexApiKey;

    @Value("${api.dadata.api-key:NOT_SET}")
    private String dadataApiKey;

    @Value("${api.dadata.secret-key:NOT_SET}")
    private String dadataSecretKey;

    public static void main(String[] args) {
        SpringApplication.run(AddressGeocoderApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("YANDEX_API_KEY: {}", yandexApiKey);
        log.info("DADATA_API_KEY: {}", dadataApiKey);
        log.info("DADATA_SECRET_KEY: {}", dadataSecretKey);
        log.info("========================================");
    }
}