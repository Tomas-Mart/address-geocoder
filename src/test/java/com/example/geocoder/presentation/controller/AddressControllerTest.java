package com.example.geocoder.presentation.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.geocoder.application.mapper.AddressMapper;
import com.example.geocoder.application.service.AddressGeocodingService;
import com.example.geocoder.domain.model.AddressAggregate;
import com.example.geocoder.domain.model.Coordinate;
import com.example.geocoder.domain.model.ProcessingStatus;
import com.example.geocoder.infrastructure.exception.BusinessException;
import com.example.geocoder.infrastructure.exception.NotFoundException;
import com.example.geocoder.presentation.dto.AddressRequest;
import com.example.geocoder.presentation.dto.AddressResponse;
import com.example.geocoder.presentation.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты контроллера AddressController")
class AddressControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AddressGeocodingService geocodingService;

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressController controller;

    private ObjectMapper objectMapper;
    private AddressAggregate aggregate;
    private AddressResponse response;

    @BeforeEach
    void setUp() {
        // ВАЖНО: добавляем GlobalExceptionHandler в тестовый контекст
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        // Подготовка тестовых данных
        Coordinate yandexCoords = new Coordinate(55.752004, 37.617734);
        Coordinate dadataCoords = new Coordinate(55.753220, 37.620400);

        aggregate = AddressAggregate.builder()
                .id(1L)
                .originalAddress("Москва, Кремль")
                .yandexCoordinates(yandexCoords)
                .dadataCoordinates(dadataCoords)
                .distanceInMeters(42.5)
                .processedAt(LocalDateTime.now())
                .processingStatus(ProcessingStatus.SUCCESS)
                .build();

        AddressResponse.CoordinateDto yandexDto = new AddressResponse.CoordinateDto(
                55.752004, 37.617734
        );
        AddressResponse.CoordinateDto dadataDto = new AddressResponse.CoordinateDto(
                55.753220, 37.620400
        );

        response = AddressResponse.builder()
                .id(1L)
                .originalAddress("Москва, Кремль")
                .yandexCoordinates(yandexDto)
                .dadataCoordinates(dadataDto)
                .distanceInMeters(42.5)
                .processedAt(LocalDateTime.now())
                .processingStatus("SUCCESS")
                .build();
    }

    // ============ ТЕСТЫ POST /api/address/geocode ============

    @Test
    @DisplayName("Должен вернуть 201 при успешном геокодировании")
    void shouldReturn201WhenGeocodeSuccess() throws Exception {
        AddressRequest request = new AddressRequest("Москва, Кремль");

        when(geocodingService.processAddress(anyString())).thenReturn(aggregate);
        when(addressMapper.toResponse(any(AddressAggregate.class))).thenReturn(response);

        mockMvc.perform(post("/api/address/geocode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.originalAddress", is("Москва, Кремль")))
                .andExpect(jsonPath("$.distanceInMeters", is(42.5)))
                .andExpect(jsonPath("$.processingStatus", is("SUCCESS")))
                .andExpect(jsonPath("$.yandexCoordinates.latitude", is(55.752004)))
                .andExpect(jsonPath("$.yandexCoordinates.longitude", is(37.617734)))
                .andExpect(jsonPath("$.dadataCoordinates.latitude", is(55.753220)))
                .andExpect(jsonPath("$.dadataCoordinates.longitude", is(37.620400)));
    }

    @Test
    @DisplayName("Должен вернуть 400 при пустом адресе")
    void shouldReturn400WhenAddressIsEmpty() throws Exception {
        AddressRequest request = new AddressRequest("");

        mockMvc.perform(post("/api/address/geocode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(result -> {
                    System.out.println("=== RESPONSE BODY ===");
                    System.out.println(result.getResponse().getContentAsString());
                    System.out.println("====================");
                })
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ошибка валидации данных"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.address").value("Адрес не может быть пустым"));
    }

    @Test
    @DisplayName("Должен вернуть 400 при адресе с пробелами")
    void shouldReturn400WhenAddressIsBlank() throws Exception {
        AddressRequest request = new AddressRequest("   ");

        mockMvc.perform(post("/api/address/geocode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(result -> {
                    System.out.println("=== RESPONSE BODY ===");
                    System.out.println(result.getResponse().getContentAsString());
                    System.out.println("====================");
                })
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ошибка валидации данных"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.address").value("Адрес не может быть пустым"));
    }

    @Test
    @DisplayName("Должен вернуть 400 при адресе длиннее 500 символов")
    void shouldReturn400WhenAddressTooLong() throws Exception {
        String longAddress = "a".repeat(501);
        AddressRequest request = new AddressRequest(longAddress);

        mockMvc.perform(post("/api/address/geocode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(result -> {
                    System.out.println("=== RESPONSE BODY ===");
                    System.out.println(result.getResponse().getContentAsString());
                    System.out.println("====================");
                })
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ошибка валидации данных"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.address").value("Адрес не должен превышать 500 символов"));
    }

    @Test
    @DisplayName("Должен вернуть 422 при ошибке бизнес-логики")
    void shouldReturn422WhenBusinessException() throws Exception {
        AddressRequest request = new AddressRequest("Москва, Кремль");

        when(geocodingService.processAddress(anyString()))
                .thenThrow(new BusinessException("Yandex API временно недоступен. Попробуйте позже."));

        mockMvc.perform(post("/api/address/geocode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Ошибка обработки данных"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value(containsString("Yandex API временно недоступен")));
    }

    // ============ ТЕСТЫ GET /api/address ============

    @Test
    @DisplayName("Должен вернуть список всех адресов")
    void shouldReturnAllAddresses() throws Exception {
        List<AddressAggregate> aggregates = Collections.singletonList(aggregate);
        List<AddressResponse> responses = Collections.singletonList(response);

        when(geocodingService.getAllAddresses()).thenReturn(aggregates);
        when(addressMapper.toResponse(any(AddressAggregate.class))).thenReturn(response);

        mockMvc.perform(get("/api/address")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].originalAddress", is("Москва, Кремль")))
                .andExpect(jsonPath("$[0].distanceInMeters", is(42.5)));
    }

    @Test
    @DisplayName("Должен вернуть пустой список когда адресов нет")
    void shouldReturnEmptyListWhenNoAddresses() throws Exception {
        when(geocodingService.getAllAddresses()).thenReturn(List.of());

        mockMvc.perform(get("/api/address")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ============ ТЕСТЫ GET /api/address/{id} ============

    @Test
    @DisplayName("Должен вернуть адрес по ID")
    void shouldReturnAddressById() throws Exception {
        when(geocodingService.getAddressById(anyLong())).thenReturn(aggregate);
        when(addressMapper.toResponse(any(AddressAggregate.class))).thenReturn(response);

        mockMvc.perform(get("/api/address/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.originalAddress", is("Москва, Кремль")))
                .andExpect(jsonPath("$.distanceInMeters", is(42.5)))
                .andExpect(jsonPath("$.processingStatus", is("SUCCESS")));
    }

    @Test
    @DisplayName("Должен вернуть 404 когда адрес не найден")
    void shouldReturn404WhenAddressNotFound() throws Exception {
        when(geocodingService.getAddressById(anyLong()))
                .thenThrow(new NotFoundException("Адрес с id 999 не найден"));

        mockMvc.perform(get("/api/address/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Сущность не найдена"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(containsString("Адрес с id 999 не найден")));
    }

    @Test
    @DisplayName("Должен вернуть 500 при непредвиденной ошибке")
    void shouldReturn500WhenUnexpectedError() throws Exception {
        when(geocodingService.getAddressById(anyLong()))
                .thenThrow(new RuntimeException("Неожиданная ошибка"));

        mockMvc.perform(get("/api/address/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Внутренняя ошибка сервера"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Произошла непредвиденная ошибка. Пожалуйста, попробуйте позже."));
    }

    @Test
    @DisplayName("Должен обработать адрес с запятой")
    void shouldHandleAddressWithComma() throws Exception {
        AddressRequest request = new AddressRequest("Москва, Красная площадь, 1");

        when(geocodingService.processAddress(anyString())).thenReturn(aggregate);
        when(addressMapper.toResponse(any(AddressAggregate.class))).thenReturn(response);

        mockMvc.perform(post("/api/address/geocode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalAddress", is("Москва, Кремль")));
    }
}