package com.example.geocoder.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import com.example.geocoder.domain.model.ProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA сущность для таблицы addresses
 * Хранит только координаты от Dadata (Yandex не сохраняется)
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "addresses", indexes = {
        @Index(name = "idx_original_address", columnList = "original_address"),
        @Index(name = "idx_processed_at", columnList = "processed_at")})
public class AddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_address", nullable = false, length = 500)
    private String originalAddress;

    // Только Dadata координаты
    @Column(name = "dadata_latitude", nullable = false)
    private Double dadataLatitude;

    @Column(name = "dadata_longitude", nullable = false)
    private Double dadataLongitude;

    @Column(name = "distance_in_meters")
    private Double distanceInMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", length = 50)
    private ProcessingStatus processingStatus;

    @CreationTimestamp
    @Column(name = "processed_at", updatable = false)
    private LocalDateTime processedAt;
}