package com.example.geocoder.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.geocoder.domain.model.AddressAggregate;
import com.example.geocoder.domain.model.Coordinate;
import com.example.geocoder.domain.model.ProcessingStatus;
import com.example.geocoder.domain.repository.AddressRepository;
import com.example.geocoder.domain.service.DistanceCalculator;
import com.example.geocoder.infrastructure.client.DadataGeocoderClient;
import com.example.geocoder.infrastructure.client.YandexGeocoderClient;
import com.example.geocoder.infrastructure.exception.BusinessException;
import com.example.geocoder.infrastructure.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для координации геокодирования адресов
 * Реализует бизнес-логику и оркестрацию внешних API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressGeocodingService {

    private final YandexGeocoderClient yandexClient;
    private final DadataGeocoderClient dadataClient;
    private final DistanceCalculator distanceCalculator;
    private final AddressRepository addressRepository;

    @Value("${app.geocoding.timeout:15}")
    private int geocodingTimeout;

    /**
     * Нормализует адрес для отправки в API геокодеров
     *
     * @param address   исходный адрес от пользователя
     * @param forYandex true если для Yandex, false если для Dadata
     * @return нормализованный адрес
     */
    public String normalizeAddress(String address, boolean forYandex) {
        if (address == null) {
            return "";
        }

        if (forYandex) {
            return address.replaceAll("\\s+", " ").trim();
        } else {
            return address.trim();
        }
    }

    /**
     * Обрабатывает адрес: получает координаты от двух сервисов, рассчитывает расстояние
     *
     * @param address текстовый адрес
     * @return агрегат с результатами обработки
     */
    @Transactional
    public AddressAggregate processAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            log.error("Адрес не может быть пустым или null");
            throw new BusinessException("Адрес не может быть пустым");
        }

        log.info("Начата обработка адреса: {}", address);

        String yandexAddress = normalizeAddress(address, true);
        String dadataAddress = normalizeAddress(address, false);

        log.debug("Адрес для Yandex: {}", yandexAddress);
        log.debug("Адрес для Dadata: {}", dadataAddress);

        try {
            // Параллельно вызываем оба API
            CompletableFuture<Coordinate> yandexFuture = CompletableFuture.supplyAsync(() -> {
                        log.debug("Запрос к Yandex API для адреса: {}", yandexAddress);
                        return yandexClient.geocode(yandexAddress);
                    }).orTimeout(geocodingTimeout, TimeUnit.SECONDS)
                    .exceptionally(throwable -> {
                        log.error("Ошибка при вызове Yandex API: {}", throwable.getMessage());
                        return null;
                    });

            CompletableFuture<Coordinate> dadataFuture = CompletableFuture.supplyAsync(() -> {
                        log.debug("Запрос к Dadata API для адреса: {}", dadataAddress);
                        return dadataClient.geocode(dadataAddress);
                    }).orTimeout(geocodingTimeout, TimeUnit.SECONDS)
                    .exceptionally(throwable -> {
                        log.error("Ошибка при вызове Dadata API: {}", throwable.getMessage());
                        return null;
                    });

            CompletableFuture.allOf(yandexFuture, dadataFuture).join();

            Coordinate yandexCoords = yandexFuture.get();
            Coordinate dadataCoords = dadataFuture.get();

            // ============================================================
            // ✅ Dadata ОБЯЗАТЕЛЕН для сохранения в БД
            // ============================================================
            if (dadataCoords == null) {
                log.error("Dadata не вернул координаты для адреса: {}", address);
                throw new BusinessException("Не удалось получить координаты от Dadata для адреса: " + address);
            }

            // ============================================================
            // ✅ Yandex может быть null - это нормально
            // ============================================================
            if (yandexCoords != null && !yandexCoords.isValid()) {
                log.warn("Yandex вернул невалидные координаты, игнорируем: {}", yandexCoords);
                yandexCoords = null;
            }

            // Валидация Dadata координат
            if (!dadataCoords.isValid()) {
                log.error("Dadata вернул невалидные координаты: {}", dadataCoords);
                throw new BusinessException("Dadata вернул невалидные координаты");
            }

            log.info("Получены координаты от Yandex: {}, от Dadata: {}", yandexCoords, dadataCoords);

            // ============================================================
            // ✅ Расчет расстояния (только если оба API сработали)
            // ============================================================
            Double distance = null;
            ProcessingStatus status;

            if (yandexCoords != null) {
                distance = distanceCalculator.calculate(
                        yandexCoords.getLatitude(), yandexCoords.getLongitude(),
                        dadataCoords.getLatitude(), dadataCoords.getLongitude()
                );
                status = ProcessingStatus.SUCCESS;
                log.info("Рассчитано расстояние между координатами: {} метров", distance);
            } else {
                status = ProcessingStatus.PARTIAL_SUCCESS;
                log.info("Yandex не вернул координаты, сохраняем только Dadata");
            }

            // Строим агрегат
            AddressAggregate aggregate = AddressAggregate.builder()
                    .originalAddress(address)
                    .yandexCoordinates(yandexCoords)
                    .dadataCoordinates(dadataCoords)
                    .distanceInMeters(distance)
                    .processedAt(LocalDateTime.now())
                    .processingStatus(status)
                    .build();

// Сохраняем в БД (Yandex координаты не сохраняются)
            AddressAggregate saved = addressRepository.save(aggregate);
            log.info("Результат сохранен в БД с id: {}, статус: {}", saved.getId(), status);

// ✅ ВОЗВРАЩАЕМ агрегат с id из БД и Yandex координатами
            return AddressAggregate.builder()
                    .id(saved.getId())
                    .originalAddress(saved.getOriginalAddress())
                    .yandexCoordinates(aggregate.getYandexCoordinates())
                    .dadataCoordinates(saved.getDadataCoordinates())
                    .distanceInMeters(saved.getDistanceInMeters())
                    .processedAt(saved.getProcessedAt())
                    .processingStatus(saved.getProcessingStatus())
                    .build();

        } catch (BusinessException e) {
            // Пробрасываем бизнес-исключения без обертки
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обработке адреса: {}", address, e);
            throw new BusinessException("Не удалось обработать адрес: " + e.getMessage(), e);
        }
    }

    /**
     * Возвращает все обработанные адреса
     *
     * @return список всех агрегатов адресов
     */
    @Transactional(readOnly = true)
    public List<AddressAggregate> getAllAddresses() {
        log.debug("Запрос всех адресов из БД");
        return addressRepository.findAll();
    }

    /**
     * Возвращает адрес по идентификатору
     *
     * @param id идентификатор адреса
     * @return агрегат адреса
     * @throws NotFoundException если адрес не найден
     */
    @Transactional(readOnly = true)
    public AddressAggregate getAddressById(Long id) {
        log.debug("Запрос адреса по id: {}", id);
        return addressRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Адрес с id {} не найден", id);
                    return new NotFoundException("Адрес с id " + id + " не найден");
                });
    }
}