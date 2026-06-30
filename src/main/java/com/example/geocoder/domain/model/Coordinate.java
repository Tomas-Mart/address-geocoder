package com.example.geocoder.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Value Object для представления географических координат
 * Используется в доменной модели
 */
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Coordinate {

    private Double latitude;   // Широта
    private Double longitude;  // Долгота

    /**
     * Проверяет, являются ли координаты валидными
     *
     * @return true если координаты в допустимых пределах
     */
    public boolean isValid() {
        if (latitude == null || longitude == null) {
            return false;
        }
        // Проверка, что координаты не равны 0 (Dadata может вернуть 0,0)
        if (latitude == 0.0 && longitude == 0.0) {
            return false;
        }
        return latitude >= -90 && latitude <= 90
               && longitude >= -180 && longitude <= 180;
    }

    /**
     * Проверяет, являются ли координаты невалидными
     * Используется для упрощения проверок в клиентском коде
     *
     * @return true если координаты НЕ валидны
     */
    public boolean isInvalid() {
        return !isValid();
    }
}