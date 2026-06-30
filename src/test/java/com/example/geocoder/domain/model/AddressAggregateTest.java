package com.example.geocoder.domain.model;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.geocoder.domain.service.DistanceCalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Тесты AddressAggregate")
class AddressAggregateTest {

    private DistanceCalculator distanceCalculator;
    private Coordinate yandexCoords;
    private Coordinate dadataCoords;

    @BeforeEach
    void setUp() {
        distanceCalculator = mock(DistanceCalculator.class);
        yandexCoords = new Coordinate(55.752004, 37.617734);
        dadataCoords = new Coordinate(55.753220, 37.620400);
        when(distanceCalculator.calculate(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(42.5);
    }

    @Test
    @DisplayName("Должен успешно рассчитывать расстояние между координатами")
    void shouldCalculateDistance() {
        AddressAggregate aggregate = AddressAggregate.builder()
                .yandexCoordinates(yandexCoords)
                .dadataCoordinates(dadataCoords)
                .build();

        double distance = aggregate.calculateDistance(distanceCalculator);

        assertThat(distance).isEqualTo(42.5);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при отсутствии координат Yandex")
    void shouldThrowExceptionWhenNoYandexCoords() {
        AddressAggregate aggregate = AddressAggregate.builder()
                .dadataCoordinates(dadataCoords)
                .build();

        assertThatThrownBy(() -> aggregate.calculateDistance(distanceCalculator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Координаты не установлены");
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при отсутствии координат Dadata")
    void shouldThrowExceptionWhenNoDadataCoords() {
        AddressAggregate aggregate = AddressAggregate.builder()
                .yandexCoordinates(yandexCoords)
                .build();

        assertThatThrownBy(() -> aggregate.calculateDistance(distanceCalculator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Координаты не установлены");
    }

    @Test
    @DisplayName("Должен возвращать true когда расстояние меньше порога")
    void shouldReturnTrueWhenDistanceLessThanThreshold() {
        AddressAggregate aggregate = AddressAggregate.builder()
                .distanceInMeters(42.5)
                .build();

        boolean result = aggregate.isCoordinatesAgreed(50.0);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Должен возвращать false когда расстояние больше порога")
    void shouldReturnFalseWhenDistanceGreaterThanThreshold() {
        AddressAggregate aggregate = AddressAggregate.builder()
                .distanceInMeters(100.0)
                .build();

        boolean result = aggregate.isCoordinatesAgreed(50.0);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Должен возвращать false когда расстояние не установлено")
    void shouldReturnFalseWhenDistanceIsNull() {
        AddressAggregate aggregate = AddressAggregate.builder()
                .distanceInMeters(null)
                .build();

        boolean result = aggregate.isCoordinatesAgreed(50.0);

        assertThat(result).isFalse();
    }

    @Test
    void shouldCreateAggregateWithSuccessStatus() {
        AddressAggregate aggregate = AddressAggregate.builder()
                .originalAddress("Москва, Кремль")
                .yandexCoordinates(new Coordinate(55.752004, 37.617734))
                .dadataCoordinates(new Coordinate(55.753220, 37.620400))
                .distanceInMeters(42.5)
                .processedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.SUCCESS)  // ← ИСПОЛЬЗОВАТЬ ENUM
                .build();

        assertThat(aggregate.getProcessingStatus()).isEqualTo(ProcessingStatus.SUCCESS);
    }
}