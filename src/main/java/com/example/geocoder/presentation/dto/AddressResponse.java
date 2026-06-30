package com.example.geocoder.presentation.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO для ответа с результатами геокодирования
 *
 * <p>Содержит координаты от обоих API и расстояние между ними.
 * Yandex координаты возвращаются только в ответе, НЕ СОХРАНЯЮТСЯ в БД.
 *
 * @see com.example.geocoder.domain.model.ProcessingStatus
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "AddressResponse",
        description = "Ответ с результатами геокодирования",
        example = """
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
public class AddressResponse {

    @Schema(
            description = "Идентификатор записи в БД",
            example = "1"
    )
    private Long id;

    @Schema(
            description = "Исходный адрес, переданный пользователем",
            example = "Москва, Кремль"
    )
    private String originalAddress;

    @Schema(
            description = "Координаты от Yandex API. " +
                          "⚠️ НЕ СОХРАНЯЮТСЯ в БД (только для ответа). " +
                          "Согласно лицензионным требованиям Yandex API Геокодера",
            example = "{\"latitude\": 55.752004, \"longitude\": 37.617734}"
    )
    private CoordinateDto yandexCoordinates;

    @Schema(
            description = "Координаты от Dadata API. " +
                          "✅ СОХРАНЯЮТСЯ в БД",
            example = "{\"latitude\": 55.753220, \"longitude\": 37.620400}"
    )
    private CoordinateDto dadataCoordinates;

    @Schema(
            description = "Расстояние между координатами в метрах (формула гаверсинусов)",
            example = "42.5"
    )
    private Double distanceInMeters;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(
            description = "Время обработки запроса",
            example = "2026-06-27 22:47:13"
    )
    private LocalDateTime processedAt;

    @Schema(
            description = "Статус обработки",
            example = "SUCCESS",
            allowableValues = {"SUCCESS", "PARTIAL_SUCCESS", "FAILED"}
    )
    private String processingStatus;

    /**
     * Вложенный DTO для географических координат
     * Используется для yandexCoordinates и dadataCoordinates
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
            name = "CoordinateDto",
            description = "Географические координаты (широта и долгота)"
    )
    public static class CoordinateDto {

        @Schema(
                description = "Широта в градусах",
                example = "55.752004",
                minimum = "-90",
                maximum = "90"
        )
        private Double latitude;

        @Schema(
                description = "Долгота в градусах",
                example = "37.617734",
                minimum = "-180",
                maximum = "180"
        )
        private Double longitude;
    }
}