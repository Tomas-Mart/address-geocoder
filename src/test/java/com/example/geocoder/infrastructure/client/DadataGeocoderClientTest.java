package com.example.geocoder.infrastructure.client;

import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.geocoder.domain.model.Coordinate;
import com.example.geocoder.infrastructure.exception.ApiClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Тесты DadataGeocoderClient (clean/address)")
class DadataGeocoderClientTest {

    private MockWebServer mockWebServer;
    private DadataGeocoderClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        client = new DadataGeocoderClient(webClient, objectMapper);

        // Устанавливаем поля через рефлексию
        ReflectionTestUtils.setField(client, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(client, "secretKey", "test-secret-key");

        // ВАЖНО: устанавливаем таймаут 5000ms (иначе будет 0ms)
        Field timeoutField = DadataGeocoderClient.class.getDeclaredField("timeout");
        timeoutField.setAccessible(true);
        timeoutField.setInt(client, 5000);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Должен успешно парсить ответ Dadata с координатами")
    void shouldParseResponseWithGeoLatLon() throws Exception {
        String jsonResponse = """
                [{
                  "source": "Москва Кремль",
                  "result": "г Москва, тер Кремль",
                  "geo_lat": "55.752004",
                  "geo_lon": "37.617734"
                }]
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        Coordinate result = client.geocode("Москва Кремль");

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(55.752004);
        assertThat(result.getLongitude()).isEqualTo(37.617734);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при пустом ответе")
    void shouldThrowExceptionWhenEmptyResponse() throws Exception {
        String jsonResponse = "[]";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> client.geocode("Москва"))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("Dadata не нашел адрес");
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при отсутствии координат")
    void shouldThrowExceptionWhenNoCoordinates() throws Exception {
        String jsonResponse = """
                [{
                  "source": "Москва Кремль",
                  "result": "г Москва, тер Кремль"
                }]
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> client.geocode("Москва"))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("Не удалось найти координаты");
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при ошибке API")
    void shouldThrowExceptionWhenApiError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        assertThatThrownBy(() -> client.geocode("Москва"))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("Ошибка вызова Dadata API");
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при нулевых координатах")
    void shouldThrowExceptionWhenZeroCoordinates() throws Exception {
        String jsonResponse = """
                [{
                  "source": "Москва Кремль",
                  "result": "г Москва, тер Кремль",
                  "geo_lat": "0.0",
                  "geo_lon": "0.0"
                }]
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> client.geocode("Москва"))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("Dadata вернул невалидные координаты (0,0)");
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при невалидных координатах")
    void shouldThrowExceptionWhenInvalidCoordinates() throws Exception {
        String jsonResponse = """
                [{
                  "source": "Москва Кремль",
                  "result": "г Москва, тер Кремль",
                  "geo_lat": "200.0",
                  "geo_lon": "400.0"
                }]
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> client.geocode("Москва"))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("Dadata вернул невалидные координаты");
    }
}