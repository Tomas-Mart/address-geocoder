package com.example.geocoder.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

/**
 * Сервис для расчета расстояния между координатами
 * Использует формулу гаверсинусов для расчета расстояния по сфере
 */
@Slf4j
@Service
public class DistanceCalculator {

    private final double earthRadius;

    public DistanceCalculator(@Value("${app.distance.earth-radius:6371000}") double earthRadius) {
        this.earthRadius = earthRadius;
        log.info("Инициализирован DistanceCalculator с радиусом Земли: {} метров", earthRadius);
    }

    /**
     * Рассчитывает расстояние между двумя точками на сфере
     * @param lat1 широта первой точки
     * @param lon1 долгота первой точки
     * @param lat2 широта второй точки
     * @param lon2 долгота второй точки
     * @return расстояние в метрах
     */
    public double calculate(double lat1, double lon1, double lat2, double lon2) {
        // Переводим градусы в радианы
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Разница координат
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        // Формула гаверсинусов
        double a = Math.pow(Math.sin(dLat / 2), 2)
                   + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                     * Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = earthRadius * c;

        log.debug("Рассчитано расстояние между координатами: {} метров", distance);
        return distance;
    }
}