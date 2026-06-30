package com.example.geocoder.infrastructure.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Конфигурация WebClient для внешних HTTP вызовов.
 *
 * <p>Настраивает HTTP клиенты для взаимодействия с внешними API:
 * <ul>
 *   <li>Yandex Geocoder API - для получения координат по адресу</li>
 *   <li>Dadata Clean Address API - для стандартизации адресов и получения координат</li>
 * </ul>
 *
 * <p><b>Настройки подключения:</b>
 * <ul>
 *   <li>Таймаут подключения: 5 секунд</li>
 *   <li>Таймаут чтения: 10 секунд</li>
 *   <li>Таймаут записи: 10 секунд</li>
 * </ul>
 *
 * <p><b>Заголовки по умолчанию:</b>
 * <ul>
 *   <li>User-Agent: AddressGeocoder/1.0</li>
 *   <li>Accept: application/json (для Dadata)</li>
 * </ul>
 *
 * @see <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/reactive/function/client/WebClient.html">Spring WebClient Documentation</a>
 * @see <a href="https://projectreactor.io/docs/netty/release/api/reactor/netty/http/client/HttpClient.html">Reactor Netty HttpClient Documentation</a>
 */
@Slf4j
@Configuration
public class WebClientConfig {

    /**
     * Базовый URL для Yandex Geocoder API.
     * Значение из application.yml: api.yandex.url
     * Пример: <a href="https://geocode-maps.yandex.ru/v1">...</a>
     */
    @org.springframework.beans.factory.annotation.Value("${api.yandex.url}")
    private String yandexUrl;

    /**
     * Базовый URL для Dadata Clean Address API.
     * Значение из application.yml: api.dadata.url
     * Пример: <a href="https://cleaner.dadata.ru/api/v1/clean/address">...</a>
     */
    @org.springframework.beans.factory.annotation.Value("${api.dadata.url}")
    private String dadataUrl;

    // ========================================================================
    // HTTP CLIENT НАСТРОЙКИ
    // ========================================================================

    /**
     * Создает и настраивает HTTP клиент на основе Reactor Netty.
     *
     * <p>Настройки:
     * <ul>
     *   <li><b>Connect Timeout:</b> 5 секунд - время ожидания установки соединения</li>
     *   <li><b>Response Timeout:</b> 10 секунд - общее время ожидания ответа</li>
     *   <li><b>Read Timeout:</b> 10 секунд - время ожидания чтения данных</li>
     *   <li><b>Write Timeout:</b> 10 секунд - время ожидания записи данных</li>
     * </ul>
     *
     * <p>Используется для всех WebClient бинов.
     *
     * @return настроенный HttpClient
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
                );
    }

    // ========================================================================
    // WEBCLIENT ДЛЯ YANDEX API
    // ========================================================================

    /**
     * Создает WebClient для взаимодействия с Yandex Geocoder API.
     *
     * <p><b>Конфигурация:</b>
     * <ul>
     *   <li>Base URL: из application.yml (api.yandex.url)</li>
     *   <li>User-Agent: AddressGeocoder/1.0</li>
     *   <li>Таймауты наследуются от {@link #httpClient()}</li>
     * </ul>
     *
     * <p><b>Пример использования:</b>
     * <pre>
     * String response = yandexWebClient.get()
     *     .uri(uriBuilder -> uriBuilder
     *         .queryParam("apikey", apiKey)
     *         .queryParam("format", "json")
     *         .queryParam("geocode", "Москва+Кремль")
     *         .build())
     *     .retrieve()
     *     .bodyToMono(String.class)
     *     .block();
     * </pre>
     *
     * <p><b>Документация API:</b>
     * <a href="https://yandex.ru/dev/geocode/doc/ru/">Yandex Geocoder API</a>
     *
     * @param httpClient настроенный HttpClient
     * @return WebClient для Yandex API
     */
    @Bean
    public WebClient yandexWebClient(HttpClient httpClient) {
        return WebClient.builder()
                .baseUrl(yandexUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "AddressGeocoder/1.0")
                .build();
    }

    // ========================================================================
    // WEBCLIENT ДЛЯ DADATA API
    // ========================================================================

    /**
     * Создает WebClient для взаимодействия с Dadata Clean Address API.
     *
     * <p><b>Конфигурация:</b>
     * <ul>
     *   <li>Base URL: из application.yml (api.dadata.url)</li>
     *   <li>Accept: application/json</li>
     *   <li>User-Agent: AddressGeocoder/1.0</li>
     *   <li>Логирование всех запросов (DEBUG уровень)</li>
     *   <li>Таймауты наследуются от {@link #httpClient()}</li>
     * </ul>
     *
     * <p><b>Пример использования:</b>
     * <pre>
     * String response = dadataWebClient.post()
     *     .header("Authorization", "Token " + apiKey)
     *     .header("X-Secret", secretKey)
     *     .bodyValue("[\"Москва, Кремль\"]")
     *     .retrieve()
     *     .bodyToMono(String.class)
     *     .block();
     * </pre>
     *
     * <p><b>Документация API:</b>
     * <a href="https://dadata.ru/api/clean/address/">Dadata Clean Address API</a>
     *
     * @param httpClient настроенный HttpClient
     * @return WebClient для Dadata API
     */
    @Bean
    public WebClient dadataWebClient(HttpClient httpClient) {
        return WebClient.builder()
                .baseUrl(dadataUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "AddressGeocoder/1.0")
                .filter(logRequest())
                .build();
    }

    // ========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ========================================================================

    /**
     * Создает фильтр для логирования всех исходящих HTTP запросов.
     *
     * <p>Логирует на уровне DEBUG:
     * <ul>
     *   <li>HTTP метод (GET, POST, etc.)</li>
     *   <li>URL запроса</li>
     * </ul>
     *
     * <p>Пример лога:
     * <pre>
     * Request: POST <a href="https://cleaner.dadata.ru/api/v1/clean/address">...</a>
     * </pre>
     *
     * @return ExchangeFilterFunction для логирования запросов
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }
}