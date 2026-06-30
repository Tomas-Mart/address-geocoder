package com.example.geocoder.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.example.geocoder.domain.model.AddressAggregate;
import com.example.geocoder.domain.model.Coordinate;
import com.example.geocoder.domain.model.ProcessingStatus;
import com.example.geocoder.domain.repository.AddressRepository;
import com.example.geocoder.infrastructure.client.DadataGeocoderClient;
import com.example.geocoder.infrastructure.client.YandexGeocoderClient;
import com.example.geocoder.presentation.dto.AddressRequest;
import com.example.geocoder.presentation.dto.AddressResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Testcontainers
@DisplayName("Интеграционные тесты геокодирования")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AddressGeocodingIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
            .withDatabaseName("geocoder_db")
            .withUsername("geocoder_user")
            .withPassword("geocoder_pass");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AddressRepository addressRepository;

    @MockBean
    private YandexGeocoderClient yandexClient;

    @MockBean
    private DadataGeocoderClient dadataClient;

    private final Coordinate YANDEX_COORDS = new Coordinate(55.752004, 37.617734);
    private final Coordinate DADATA_COORDS = new Coordinate(55.753220, 37.620400);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("api.yandex.enabled", () -> "false");
        registry.add("api.dadata.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() {
        when(yandexClient.geocode(anyString())).thenReturn(YANDEX_COORDS);
        when(dadataClient.geocode(anyString())).thenReturn(DADATA_COORDS);
    }

    // ==================== УСПЕШНЫЕ СЦЕНАРИИ ====================

    @Test
    @DisplayName("POST - должен вернуть 201 при успешном геокодировании (оба API работают)")
    void shouldReturn201WhenBothApisWork() {
        AddressRequest request = new AddressRequest("Москва, Кремль");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AddressResponse> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/geocode",
                HttpMethod.POST,
                entity,
                AddressResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOriginalAddress()).isEqualTo("Москва, Кремль");
        assertThat(response.getBody().getDadataCoordinates()).isNotNull();
        assertThat(response.getBody().getDistanceInMeters()).isNotNull();
        assertThat(response.getBody().getProcessingStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("POST - должен вернуть 201 с PARTIAL_SUCCESS когда только Dadata нашел (Yandex null)")
    void shouldReturn201WithPartialSuccessWhenOnlyDadataFound() {
        when(yandexClient.geocode(anyString())).thenReturn(null);
        when(dadataClient.geocode(anyString())).thenReturn(DADATA_COORDS);

        AddressRequest request = new AddressRequest("Москва, Кремль");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AddressResponse> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/geocode",
                HttpMethod.POST,
                entity,
                AddressResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getYandexCoordinates()).isNull();
        assertThat(response.getBody().getDadataCoordinates()).isNotNull();
        assertThat(response.getBody().getDistanceInMeters()).isNull();
        assertThat(response.getBody().getProcessingStatus()).isEqualTo("PARTIAL_SUCCESS");
    }

    @Test
    @DisplayName("POST - должен обрабатывать адрес без запятой")
    void shouldHandleAddressWithoutComma() {
        AddressRequest request = new AddressRequest("Москва Кремль");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AddressResponse> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/geocode",
                HttpMethod.POST,
                entity,
                AddressResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOriginalAddress()).isEqualTo("Москва Кремль");
    }

    // ==================== ОШИБКИ ВАЛИДАЦИИ ====================

    @Test
    @DisplayName("POST - должен вернуть 400 при пустом адресе")
    void shouldReturn400WhenAddressIsEmpty() {
        AddressRequest request = new AddressRequest("");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/geocode",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Адрес не может быть пустым");
    }

    @Test
    @DisplayName("POST - должен вернуть 400 при слишком длинном адресе")
    void shouldReturn400WhenAddressTooLong() {
        String longAddress = "a".repeat(501);
        AddressRequest request = new AddressRequest(longAddress);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/geocode",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Адрес не должен превышать 500 символов");
    }

    // ==================== ОШИБКИ API ====================

    @Test
    @DisplayName("POST - должен вернуть 422 когда оба API не нашли адрес (Dadata = null)")
    void shouldReturn422WhenBothApisFail() {
        when(yandexClient.geocode(anyString())).thenReturn(null);
        when(dadataClient.geocode(anyString())).thenReturn(null);

        AddressRequest request = new AddressRequest("Несуществующий адрес 12345");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/geocode",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("Не удалось получить координаты от Dadata");
    }

    @Test
    @DisplayName("POST - должен вернуть 422 когда только Yandex нашел (Dadata = null)")
    void shouldReturn422WhenOnlyYandexFound() {
        when(yandexClient.geocode(anyString())).thenReturn(YANDEX_COORDS);
        when(dadataClient.geocode(anyString())).thenReturn(null);

        AddressRequest request = new AddressRequest("Москва, Кремль");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/geocode",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("Не удалось получить координаты от Dadata");
    }

    @Test
    @DisplayName("POST - должен вернуть 422 при невалидных координатах от Dadata")
    void shouldReturn422WhenInvalidCoordinatesFromDadata() {
        when(yandexClient.geocode(anyString())).thenReturn(YANDEX_COORDS);
        when(dadataClient.geocode(anyString()))
                .thenReturn(new Coordinate(100.0, 200.0));

        AddressRequest request = new AddressRequest("Москва, Кремль");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/geocode",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("Dadata вернул невалидные координаты");
    }

    // ==================== СОХРАНЕНИЕ В БД ====================

    @Test
    @DisplayName("Должен сохранять результат в БД (только Dadata координаты)")
    void shouldSaveResultToDatabase() {
        AddressRequest request = new AddressRequest("Москва, Красная площадь, 1");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AddressResponse> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/geocode",
                HttpMethod.POST,
                entity,
                AddressResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        Long id = response.getBody().getId();
        assertThat(id).isNotNull();

        AddressAggregate saved = addressRepository.findById(id).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getOriginalAddress()).isEqualTo("Москва, Красная площадь, 1");
        assertThat(saved.getDadataCoordinates()).isNotNull();
        assertThat(saved.getYandexCoordinates()).isNull();
        assertThat(saved.getDistanceInMeters()).isNotNull();
        assertThat(saved.getProcessingStatus()).isEqualTo(ProcessingStatus.SUCCESS);
    }

    // ==================== GET ЗАПРОСЫ ====================

    @Test
    @DisplayName("GET /api/address - должен вернуть список адресов")
    void shouldReturnAllAddresses() {
        AddressRequest request = new AddressRequest("Москва, Кремль");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.exchange(
                "http://localhost:" + port + "/api/address/geocode",
                HttpMethod.POST,
                entity,
                AddressResponse.class
        );

        ResponseEntity<AddressResponse[]> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address",
                HttpMethod.GET,
                null,
                AddressResponse[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);
    }

    @Test
    @DisplayName("GET /api/address/{id} - должен вернуть адрес по ID")
    void shouldReturnAddressById() {
        AddressRequest request = new AddressRequest("Москва, Кремль");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AddressRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AddressResponse> created = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/geocode",
                HttpMethod.POST,
                entity,
                AddressResponse.class
        );

        Long id = created.getBody().getId();
        assertThat(id).isNotNull();

        ResponseEntity<AddressResponse> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/" + id,
                HttpMethod.GET,
                null,
                AddressResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("GET /api/address/999 - должен вернуть 404")
    void shouldReturn404WhenAddressNotFound() {
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/address/999",
                HttpMethod.GET,
                null,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Адрес с id 999 не найден");
    }

    @Test
    @DisplayName("GET /actuator/health - должен вернуть UP")
    void shouldReturnUpHealthStatus() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}