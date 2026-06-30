package com.example.geocoder.presentation.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.geocoder.application.mapper.AddressMapper;
import com.example.geocoder.application.service.AddressGeocodingService;
import com.example.geocoder.domain.model.AddressAggregate;
import com.example.geocoder.presentation.dto.AddressRequest;
import com.example.geocoder.presentation.dto.AddressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST контроллер для обработки запросов геокодирования
 * Слой презентации, только вызовы сервисов и маппинг DTO
 */
@Slf4j
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
@Tag(name = "Address Geocoding", description = "API для геокодирования адресов")
public class AddressController {

    private final AddressGeocodingService geocodingService;
    private final AddressMapper addressMapper;

    /**
     * Эндпоинт для геокодирования адреса
     * POST /api/address/geocode
     */
    @Operation(
            summary = "Геокодирование адреса",
            description = "Принимает текстовый адрес, получает координаты от Yandex и Dadata API, рассчитывает расстояние между ними"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Адрес успешно обработан",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AddressResponse.class),
                            examples = @ExampleObject(
                                    name = "Успешный ответ",
                                    value = """
                                            {
                                              "id": 1,
                                              "originalAddress": "Москва, Кремль",
                                              "yandexCoordinates": {
                                                "latitude": 55.752004,
                                                "longitude": 37.617734
                                              },
                                              "dadataCoordinates": {
                                                "latitude": 55.753220,
                                                "longitude": 37.620400
                                              },
                                              "distanceInMeters": 42.5,
                                              "processedAt": "2026-06-27 22:47:13",
                                              "processingStatus": "SUCCESS"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный запрос (например, пустой адрес)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            examples = @ExampleObject(
                                    name = "Ошибка валидации",
                                    value = """
                                            {
                                              "type": "about:blank",
                                              "title": "Ошибка валидации данных",
                                              "status": 400,
                                              "detail": "Проверьте правильность введенных данных",
                                              "properties": {
                                                "timestamp": "2026-06-27T22:47:00.897Z",
                                                "errors": {
                                                  "address": "Адрес не может быть пустым"
                                                }
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Ошибка обработки данных (API недоступен, неверный ключ и т.д.)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            examples = @ExampleObject(
                                    name = "Ошибка API",
                                    value = """
                                            {
                                              "type": "about:blank",
                                              "title": "Ошибка обработки данных",
                                              "status": 422,
                                              "detail": "Не удалось обработать адрес: Yandex API временно недоступен. Попробуйте позже.",
                                              "instance": "/api/address/geocode",
                                              "properties": {
                                                "timestamp": "2026-06-27T22:47:00.897Z",
                                                "path": "uri=/api/address/geocode"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = "application/problem+json",
                            examples = @ExampleObject(
                                    name = "Внутренняя ошибка",
                                    value = """
                                            {
                                              "type": "about:blank",
                                              "title": "Внутренняя ошибка сервера",
                                              "status": 500,
                                              "detail": "Произошла непредвиденная ошибка"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/geocode")
    public ResponseEntity<AddressResponse> geocode(
            @Parameter(
                    description = "Запрос с адресом для геокодирования",
                    required = true,
                    schema = @Schema(implementation = AddressRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "Пример с Кремлем",
                                    value = "{\"address\": \"Москва, Кремль\"}"
                            ),
                            @ExampleObject(
                                    name = "Пример с Красной площадью",
                                    value = "{\"address\": \"Москва, Красная площадь, 1\"}"
                            ),
                            @ExampleObject(
                                    name = "Пример с Санкт-Петербургом",
                                    value = "{\"address\": \"Санкт-Петербург, Дворцовая площадь, 2\"}"
                            )
                    }
            )
            @Valid @RequestBody AddressRequest request
    ) {
        log.info("Получен запрос на геокодирование адреса: {}", request.getAddress());

        AddressAggregate aggregate = geocodingService.processAddress(request.getAddress());
        AddressResponse response = addressMapper.toResponse(aggregate);

        log.info("Успешно обработан адрес: {}, расстояние: {} м",
                request.getAddress(), response.getDistanceInMeters());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получить все обработанные адреса
     * GET /api/address
     */
    @Operation(
            summary = "Получить все адреса",
            description = "Возвращает список всех обработанных адресов"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список адресов успешно получен",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AddressResponse.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAllAddresses() {
        log.info("Запрос всех адресов");

        List<AddressAggregate> aggregates = geocodingService.getAllAddresses();
        List<AddressResponse> responses = aggregates.stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Найдено {} адресов", responses.size());
        return ResponseEntity.ok(responses);
    }

    /**
     * Получить адрес по ID
     * GET /api/address/{id}
     */
    @Operation(
            summary = "Получить адрес по ID",
            description = "Возвращает адрес по его идентификатору"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Адрес найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AddressResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Адрес не найден",
                    content = @Content(
                            mediaType = "application/problem+json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "type": "about:blank",
                                              "title": "Адрес не найден",
                                              "status": 404,
                                              "detail": "Адрес с id 1 не найден"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddressById(
            @Parameter(description = "Идентификатор адреса", example = "1")
            @PathVariable Long id
    ) {
        log.info("Запрос адреса по id: {}", id);

        AddressAggregate aggregate = geocodingService.getAddressById(id);
        AddressResponse response = addressMapper.toResponse(aggregate);

        log.info("Найден адрес с id: {}, адрес: {}", id, response.getOriginalAddress());
        return ResponseEntity.ok(response);
    }
}