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
 * Клиент для взаимодействия с Dadata Clean Address API.
 *
 * <p>Использует эндпоинт {@code /clean/address} для стандартизации адресов
 * и получения географических координат.
 *
 * <p>Ответ приходит в формате JSON массива, где каждый элемент содержит:
 * <ul>
 *   <li>{@code source} - исходный адрес</li>
 *   <li>{@code result} - стандартизированный адрес</li>
 *   <li>{@code geo_lat} - широта</li>
 *   <li>{@code geo_lon} - долгота</li>
 * </ul>
 *
 * <p><b>Пример запроса:</b>
 * <pre>
 * POST <a href="https://cleaner.dadata.ru/api/v1/clean/address">...</a>
 * Authorization: Token YOUR_API_KEY
 * X-Secret: YOUR_SECRET_KEY
 * Content-Type: application/json
 *
 * ["Москва, Кремль"]
 * </pre>
 *
 * <p><b>Пример ответа:</b>
 * <pre>
 * [{
 *   "source": "Москва Кремль",
 *   "result": "г Москва, тер Кремль",
 *   "geo_lat": "55.752004",
 *   "geo_lon": "37.617734"
 * }]
 * </pre>
 *
 * @see <a href="https://dadata.ru/api/clean/address/">Документация Dadata API</a>
 * @see GeocoderClient
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DadataGeocoderClient implements GeocoderClient {

    private final WebClient dadataWebClient;
    private final ObjectMapper objectMapper;

    @Value("${api.dadata.api-key}")
    private String apiKey;

    @Value("${api.dadata.secret-key}")
    private String secretKey;

    @Value("${api.dadata.timeout:5000}")
    private int timeout;

    @Override
    @Retry(name = "dadataRetry", fallbackMethod = "fallbackGeocode")
    @CircuitBreaker(name = "dadataCircuitBreaker", fallbackMethod = "fallbackGeocode")
    public Coordinate geocode(String address) {
        log.debug("Вызов Dadata Clean Address API для адреса: {}", address);

        try {
            // Формат для /clean/address: массив строк
            String requestBody = String.format("[\"%s\"]", address);
            log.debug("Тело запроса к Dadata: {}", requestBody);

            String response = dadataWebClient.post()
                    .header("Authorization", "Token " + apiKey)
                    .header("X-Secret", secretKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Ошибка Dadata API. Статус: {}, Тело: {}",
                                                clientResponse.statusCode(), errorBody);
                                        return Mono.error(new ApiClientException(
                                                "Dadata API вернул ошибку: " + errorBody));
                                    })
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("Получен ответ от Dadata API для адреса: {}", address);
            return parseCleanAddressResponse(response);

        } catch (Exception e) {
            log.error("Ошибка при запросе к Dadata API для адреса '{}': {}", address, e.getMessage(), e);
            throw new ApiClientException("Ошибка вызова Dadata API: " + e.getMessage(), e);
        }
    }

    /**
     * Парсит ответ от Dadata /clean/address
     *
     * <p>Ответ приходит в формате массива объектов:
     * <pre>
     * [{
     *   "source": "Москва Кремль",
     *   "result": "г Москва, тер Кремль",
     *   "geo_lat": "55.752004",
     *   "geo_lon": "37.617734"
     * }]
     * </pre>
     *
     * @param response JSON строка ответа
     * @return координаты или null если не найдены
     * @throws ApiClientException если не удалось распарсить ответ
     */
    private Coordinate parseCleanAddressResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // Ответ должен быть массивом
            if (!root.isArray() || root.isEmpty()) {
                log.warn("Dadata вернул пустой ответ для запроса");
                throw new ApiClientException("Dadata не нашел адрес");
            }

            JsonNode firstResult = root.path(0);

            // Проверяем наличие координат
            JsonNode geoLat = firstResult.path("geo_lat");
            JsonNode geoLon = firstResult.path("geo_lon");

            if (geoLat.isMissingNode() || geoLon.isMissingNode()) {
                String result = firstResult.path("result").asText("неизвестный адрес");
                log.warn("Dadata не вернул координаты для адреса: {}", result);
                throw new ApiClientException("Не удалось найти координаты для адреса: " + result);
            }

            double lat = geoLat.asDouble();
            double lon = geoLon.asDouble();

            // Проверка: координаты не должны быть 0,0 (Dadata может вернуть нулевые координаты)
            if (lat == 0.0 && lon == 0.0) {
                log.warn("Dadata вернул нулевые координаты для адреса");
                throw new ApiClientException("Dadata вернул невалидные координаты (0,0)");
            }

            // Проверка валидности координат
            Coordinate coordinate = new Coordinate(lat, lon);
            if (!coordinate.isValid()) {
                log.warn("Dadata вернул невалидные координаты: lat={}, lon={}", lat, lon);
                throw new ApiClientException("Dadata вернул невалидные координаты");
            }

            log.debug("Успешно получены координаты от Dadata: lat={}, lon={}", lat, lon);
            return coordinate;

        } catch (ApiClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка парсинга ответа Dadata: {}", response, e);
            throw new ApiClientException("Ошибка парсинга ответа Dadata: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback метод при ошибках вызова Dadata API
     * Срабатывает при превышении таймаута, ошибках сети или недоступности API
     *
     * @param address   адрес, который пытались геокодировать
     * @param throwable причина ошибки
     * @return никогда не возвращает значение, всегда выбрасывает исключение
     * @throws ApiClientException всегда
     */
    public Coordinate fallbackGeocode(String address, Throwable throwable) {
        log.warn("Использован fallback для Dadata API. Адрес: {}, Ошибка: {}",
                address, throwable.getMessage());
        throw new ApiClientException("Dadata API временно недоступен. Попробуйте позже.");
    }
}