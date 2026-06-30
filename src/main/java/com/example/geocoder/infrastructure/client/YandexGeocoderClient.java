package com.example.geocoder.infrastructure.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.geocoder.domain.model.Coordinate;
import com.example.geocoder.infrastructure.exception.ApiClientException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Клиент для взаимодействия с Yandex Geocoder API
 * Использует WebClient для реактивных запросов
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YandexGeocoderClient implements GeocoderClient {

    private final WebClient yandexWebClient;
    private final ObjectMapper objectMapper;

    @Value("${api.yandex.api-key}")
    private String apiKey;

    @Value("${api.yandex.timeout:10000}")
    private int timeout;

    @Override
    @Retry(name = "yandexRetry", fallbackMethod = "fallbackGeocode")
    @CircuitBreaker(name = "yandexCircuitBreaker", fallbackMethod = "fallbackGeocode")
    public Coordinate geocode(String address) {
        log.debug("Вызов Yandex Geocoder API для адреса: {}", address);

        try {
            // Используем относительный путь (baseUrl уже настроен в WebClientConfig)
            String response = yandexWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("apikey", apiKey)
                            .queryParam("format", "json")
                            .queryParam("geocode", address.replace(" ", "+"))
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Ошибка Yandex API. Статус: {}, Тело: {}",
                                                clientResponse.statusCode(), errorBody);
                                        return Mono.error(new ApiClientException("Yandex API вернул ошибку: " + errorBody));
                                    })
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("=== ОТВЕТ YANDEX API: {}", response);

            Coordinate result = parseResponse(response);

            if (result == null || !result.isValid()) {
                log.warn("Yandex не вернул валидные координаты для адреса: {}, используем только Dadata", address);
                return null;
            }

            return result;

        } catch (Exception e) {
            log.error("Ошибка при запросе к Yandex API: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Парсит ответ от Yandex API
     *
     * @param response JSON ответ
     * @return координаты или null, если координаты не найдены
     */
    private Coordinate parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // Проверяем, что есть результаты
            JsonNode featureMember = root
                    .path("response")
                    .path("GeoObjectCollection")
                    .path("featureMember");

            if (featureMember.isMissingNode() || featureMember.isEmpty()) {
                log.warn("Yandex не нашел адрес (featureMember пуст или отсутствует)");
                return null;
            }

            JsonNode geoObject = featureMember.path(0).path("GeoObject");
            if (geoObject.isMissingNode()) {
                log.warn("Yandex не нашел адрес (GeoObject отсутствует)");
                return null;
            }

            String pos = geoObject
                    .path("Point")
                    .path("pos")
                    .asText();

            if (pos.isEmpty()) {
                log.warn("Yandex не нашел адрес (pos пуст)");
                return null;
            }

            String[] coordinates = pos.split(" ");
            if (coordinates.length < 2) {
                log.warn("Yandex вернул некорректный формат координат: {}", pos);
                return null;
            }

            double lon = Double.parseDouble(coordinates[0]);
            double lat = Double.parseDouble(coordinates[1]);

            log.debug("Yandex вернул координаты: lat={}, lon={}", lat, lon);
            return new Coordinate(lat, lon);

        } catch (Exception e) {
            log.error("Ошибка парсинга ответа Yandex: {}", response, e);
            return null;
        }
    }

    /**
     * Fallback метод при ошибках
     */
    public Coordinate fallbackGeocode(String address, Throwable throwable) {
        log.warn("Использован fallback для Yandex API. Адрес: {}, Ошибка: {}", address, throwable.getMessage());
        return null;
    }
}