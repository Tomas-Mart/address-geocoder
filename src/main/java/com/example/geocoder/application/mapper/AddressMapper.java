package com.example.geocoder.application.mapper;

import org.springframework.stereotype.Component;
import com.example.geocoder.domain.model.AddressAggregate;
import com.example.geocoder.domain.model.ProcessingStatus;
import com.example.geocoder.presentation.dto.AddressResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Маппер для преобразования доменной модели в DTO ответа
 */
@Slf4j
@Component
public class AddressMapper {

    /**
     * Преобразует агрегат Address в DTO ответа
     *
     * @param aggregate агрегат адреса
     * @return DTO для ответа клиенту
     */
    public AddressResponse toResponse(AddressAggregate aggregate) {
        if (aggregate == null) {
            log.warn("Попытка преобразовать null агрегат в Response DTO");
            return null;
        }

        AddressResponse response = new AddressResponse();
        response.setId(aggregate.getId());
        response.setOriginalAddress(aggregate.getOriginalAddress());
        response.setDistanceInMeters(aggregate.getDistanceInMeters());
        response.setProcessedAt(aggregate.getProcessedAt());

        // Конвертируем enum в строку
        ProcessingStatus status = aggregate.getProcessingStatus();
        response.setProcessingStatus(status != null ? status.name() : null);

        // Маппинг координат Yandex
        if (aggregate.getYandexCoordinates() != null) {
            AddressResponse.CoordinateDto yandexDto = new AddressResponse.CoordinateDto(
                    aggregate.getYandexCoordinates().getLatitude(),
                    aggregate.getYandexCoordinates().getLongitude()
            );
            response.setYandexCoordinates(yandexDto);
        }

        // Маппинг координат Dadata
        if (aggregate.getDadataCoordinates() != null) {
            AddressResponse.CoordinateDto dadataDto = new AddressResponse.CoordinateDto(
                    aggregate.getDadataCoordinates().getLatitude(),
                    aggregate.getDadataCoordinates().getLongitude()
            );
            response.setDadataCoordinates(dadataDto);
        }

        return response;
    }
}