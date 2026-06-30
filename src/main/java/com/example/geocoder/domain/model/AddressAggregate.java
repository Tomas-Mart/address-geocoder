package com.example.geocoder.domain.model;

import java.time.LocalDateTime;
import com.example.geocoder.domain.service.DistanceCalculator;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Агрегат для работы с адресом
 * Содержит бизнес-логику и правила работы с адресом
 * <p>
 * Важно: Yandex координаты НЕ СОХРАНЯЮТСЯ в БД (только для ответа API)
 * Dadata координаты сохраняются в БД
 */
@Getter
@Builder
@ToString
public class AddressAggregate {

    private Long id;
    private String originalAddress;
    // Только для ответа, НЕ сохраняется в БД!
    private Coordinate yandexCoordinates;
    // Сохраняется в БД
    private Coordinate dadataCoordinates;
    private Double distanceInMeters;
    private LocalDateTime processedAt;
    private ProcessingStatus processingStatus;  // ← ИЗМЕНЕНО: String → ProcessingStatus

    /**
     * Рассчитывает расстояние между координатами от Yandex и Dadata
     *
     * @param calculator сервис для расчета расстояния
     * @return расстояние в метрах
     */
    public double calculateDistance(DistanceCalculator calculator) {
        if (yandexCoordinates == null || dadataCoordinates == null) {
            throw new IllegalStateException("Координаты не установлены для расчета расстояния");
        }
        return calculator.calculate(
                yandexCoordinates.getLatitude(),
                yandexCoordinates.getLongitude(),
                dadataCoordinates.getLatitude(),
                dadataCoordinates.getLongitude()
        );
    }

    /**
     * Проверяет, что разница между координатами не превышает допустимый порог
     *
     * @param maxDifferenceInMeters максимальная допустимая разница в метрах
     * @return true если координаты согласованы
     */
    public boolean isCoordinatesAgreed(double maxDifferenceInMeters) {
        if (distanceInMeters == null) {
            return false;
        }
        return distanceInMeters <= maxDifferenceInMeters;
    }
}