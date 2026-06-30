package com.example.geocoder.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import com.example.geocoder.domain.model.AddressAggregate;
import com.example.geocoder.domain.model.Coordinate;
import com.example.geocoder.infrastructure.persistence.entity.AddressEntity;
import lombok.extern.slf4j.Slf4j;

/**
 * Маппер между доменной моделью и JPA сущностью
 * Сохраняет только Dadata координаты (Yandex не сохраняется)
 */
@Slf4j
@Component
public class AddressEntityMapper {

    public AddressEntity toEntity(AddressAggregate aggregate) {
        if (aggregate == null) {
            log.warn("Попытка преобразовать null агрегат в Entity");
            return null;
        }

        // Проверка наличия Dadata координат
        if (aggregate.getDadataCoordinates() == null) {
            log.error("Невозможно сохранить агрегат: Dadata координаты отсутствуют");
            throw new IllegalStateException("Dadata координаты не могут быть null для сохранения");
        }

        return AddressEntity.builder()
                .id(aggregate.getId())
                .originalAddress(aggregate.getOriginalAddress())
                // Сохраняем только Dadata координаты
                .dadataLatitude(aggregate.getDadataCoordinates().getLatitude())
                .dadataLongitude(aggregate.getDadataCoordinates().getLongitude())
                .distanceInMeters(aggregate.getDistanceInMeters())
                .processingStatus(aggregate.getProcessingStatus())
                .processedAt(aggregate.getProcessedAt())
                .build();
    }

    public AddressAggregate toDomain(AddressEntity entity) {
        if (entity == null) {
            log.warn("Попытка преобразовать null Entity в доменную модель");
            return null;
        }

        // Проверка наличия координат в БД
        if (entity.getDadataLatitude() == null || entity.getDadataLongitude() == null) {
            log.error("Некорректные данные в БД: отсутствуют координаты для записи id={}", entity.getId());
            throw new IllegalStateException("Некорректные данные в БД: отсутствуют координаты");
        }

        return AddressAggregate.builder()
                .id(entity.getId())
                .originalAddress(entity.getOriginalAddress())
                // Dadata координаты из БД
                .dadataCoordinates(new Coordinate(
                        entity.getDadataLatitude(),
                        entity.getDadataLongitude()
                ))
                // Yandex координаты не восстанавливаем из БД (лицензионные ограничения)
                .yandexCoordinates(null)
                .distanceInMeters(entity.getDistanceInMeters())
                .processingStatus(entity.getProcessingStatus())
                .processedAt(entity.getProcessedAt())
                .build();
    }
}