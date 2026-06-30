package com.example.geocoder.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.geocoder.infrastructure.persistence.entity.AddressEntity;

/**
 * JPA репозиторий для работы с AddressEntity
 */
@Repository
public interface AddressJpaRepository extends JpaRepository<AddressEntity, Long> {

    /**
     * Находит адреса по исходному адресу
     *
     * @param originalAddress исходный адрес
     * @return список сущностей адресов
     */
    List<AddressEntity> findByOriginalAddress(String originalAddress);

    /**
     * Находит адреса по диапазону дат
     *
     * @param from начальная дата
     * @param to   конечная дата
     * @return список сущностей адресов
     */
    List<AddressEntity> findByProcessedAtBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Ищет адреса по частичному совпадению с исходным адресом
     * Использует текстовый блок для многострочного SQL-запроса
     *
     * @param address часть адреса для поиска
     * @return список сущностей адресов
     */
    @Query("""
             SELECT a
             FROM AddressEntity a
             WHERE a.originalAddress LIKE %:address%
            """)
    List<AddressEntity> searchByAddress(@Param("address") String address);
}