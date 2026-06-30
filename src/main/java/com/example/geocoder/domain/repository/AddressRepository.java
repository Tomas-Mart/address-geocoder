package com.example.geocoder.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.example.geocoder.domain.model.AddressAggregate;

/**
 * Интерфейс репозитория для работы с агрегатами адресов
 * Определяет контракт для инфраструктурного слоя
 */
public interface AddressRepository {

    /**
     * Сохраняет агрегат адреса
     *
     * @param address агрегат для сохранения
     * @return сохраненный агрегат
     */
    AddressAggregate save(AddressAggregate address);

    /**
     * Находит агрегат по идентификатору
     *
     * @param id идентификатор
     * @return Optional с агрегатом или пустой Optional
     */
    Optional<AddressAggregate> findById(Long id);

    /**
     * Находит все агрегаты
     *
     * @return список агрегатов
     */
    List<AddressAggregate> findAll();

    /**
     * Находит агрегаты по оригинальному адресу
     *
     * @param address адрес для поиска
     * @return список агрегатов
     */
    List<AddressAggregate> findByOriginalAddress(String address);

    /**
     * Находит агрегаты за определенный период
     *
     * @param from дата начала
     * @param to   дата окончания
     * @return список агрегатов
     */
    List<AddressAggregate> findByProcessedAtBetween(LocalDateTime from, LocalDateTime to);
}