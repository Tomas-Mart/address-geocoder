package com.example.geocoder.infrastructure.client;

import com.example.geocoder.domain.model.Coordinate;

/**
 * Интерфейс для всех клиентов геокодирования
 */
public interface GeocoderClient {

    /**
     * Получает координаты по адресу
     *
     * @param address текстовый адрес
     * @return координаты
     */
    Coordinate geocode(String address);
}