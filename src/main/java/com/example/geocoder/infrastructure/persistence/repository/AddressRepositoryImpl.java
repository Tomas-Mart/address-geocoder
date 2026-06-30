package com.example.geocoder.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import com.example.geocoder.domain.model.AddressAggregate;
import com.example.geocoder.domain.repository.AddressRepository;
import com.example.geocoder.infrastructure.persistence.entity.AddressEntity;
import com.example.geocoder.infrastructure.persistence.mapper.AddressEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация доменного репозитория через JPA
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AddressRepositoryImpl implements AddressRepository {

    private final AddressJpaRepository jpaRepository;
    private final AddressEntityMapper mapper;

    @Override
    public AddressAggregate save(AddressAggregate address) {
        log.debug("Сохранение адреса: {}", address.getOriginalAddress());

        AddressEntity entity = mapper.toEntity(address);
        AddressEntity saved = jpaRepository.save(entity);

        log.info("Адрес сохранен с id: {}", saved.getId());
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AddressAggregate> findById(Long id) {
        log.debug("Поиск адреса по id: {}", id);
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<AddressAggregate> findAll() {
        log.debug("Поиск всех адресов");
        List<AddressEntity> entities = jpaRepository.findAll();
        return entities.isEmpty()
                ? Collections.emptyList()
                : entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AddressAggregate> findByOriginalAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            log.warn("Попытка поиска по пустому адресу");
            return Collections.emptyList();
        }

        log.debug("Поиск адресов по: {}", address);
        List<AddressEntity> entities = jpaRepository.findByOriginalAddress(address);
        return entities.isEmpty()
                ? Collections.emptyList()
                : entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AddressAggregate> findByProcessedAtBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            log.warn("Попытка поиска с null датами");
            return Collections.emptyList();
        }

        if (from.isAfter(to)) {
            log.warn("Начальная дата {} позже конечной {}", from, to);
            return Collections.emptyList();
        }

        log.debug("Поиск адресов за период: {} - {}", from, to);
        List<AddressEntity> entities = jpaRepository.findByProcessedAtBetween(from, to);
        return entities.isEmpty()
                ? Collections.emptyList()
                : entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}