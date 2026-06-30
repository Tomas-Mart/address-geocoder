package com.example.geocoder.infrastructure.client;

import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.geocoder.domain.model.Coordinate;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты YandexGeocoderClient")
class YandexGeocoderClientTest {

    private MockWebServer mockWebServer;
    private YandexGeocoderClient client;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        client = new YandexGeocoderClient(webClient, new ObjectMapper());

        // Устанавливаем поля через рефлексию
        ReflectionTestUtils.setField(client, "apiKey", "test-api-key");

        // Устанавливаем таймаут 10000ms
        Field timeoutField = YandexGeocoderClient.class.getDeclaredField("timeout");
        timeoutField.setAccessible(true);
        timeoutField.setInt(client, 10000);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Должен успешно парсить ответ Yandex с координатами")
    void shouldParseResponseSuccessfully() throws Exception {
        String jsonResponse = """
                {
                  "response": {
                    "GeoObjectCollection": {
                      "featureMember": [
                        {
                          "GeoObject": {
                            "Point": {
                              "pos": "37.617734 55.752004"
                            }
                          }
                        }
                      ]
                    }
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        Coordinate result = client.geocode("Москва");

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(55.752004);
        assertThat(result.getLongitude()).isEqualTo(37.617734);
    }

    @Test
    @DisplayName("Должен вернуть null при ошибке API (403)")
    void shouldReturnNullWhenApiError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(403)
                .setBody("{\"error\": \"Invalid api key\"}")
                .addHeader("Content-Type", "application/json"));

        Coordinate result = client.geocode("Москва");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Должен вернуть null при ошибке API (500)")
    void shouldReturnNullWhenInternalServerError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        Coordinate result = client.geocode("Москва");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Должен вернуть null при пустом ответе (featureMember пуст)")
    void shouldReturnNullWhenEmptyResponse() throws Exception {
        String jsonResponse = """
                {
                  "response": {
                    "GeoObjectCollection": {
                      "featureMember": []
                    }
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        Coordinate result = client.geocode("Москва");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Должен вернуть null при отсутствии поля pos")
    void shouldReturnNullWhenPosMissing() throws Exception {
        String jsonResponse = """
                {
                  "response": {
                    "GeoObjectCollection": {
                      "featureMember": [
                        {
                          "GeoObject": {
                            "Point": {}
                          }
                        }
                      ]
                    }
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        Coordinate result = client.geocode("Москва");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Должен вернуть null при отсутствии GeoObject")
    void shouldReturnNullWhenGeoObjectMissing() throws Exception {
        String jsonResponse = """
                {
                  "response": {
                    "GeoObjectCollection": {
                      "featureMember": [
                        {}
                      ]
                    }
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        Coordinate result = client.geocode("Москва");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Должен вернуть null при некорректном формате координат")
    void shouldReturnNullWhenInvalidCoordinatesFormat() throws Exception {
        String jsonResponse = """
                {
                  "response": {
                    "GeoObjectCollection": {
                      "featureMember": [
                        {
                          "GeoObject": {
                            "Point": {
                              "pos": "invalid"
                            }
                          }
                        }
                      ]
                    }
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        Coordinate result = client.geocode("Москва");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Должен вернуть null при ошибке парсинга JSON")
    void shouldReturnNullWhenJsonParseError() throws Exception {
        String jsonResponse = "invalid json {";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        Coordinate result = client.geocode("Москва");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Должен корректно обработать fallback метод")
    void shouldHandleFallbackMethod() {
        Throwable throwable = new RuntimeException("Connection timeout");
        Coordinate result = client.fallbackGeocode("Москва", throwable);
        assertThat(result).isNull();
    }
}