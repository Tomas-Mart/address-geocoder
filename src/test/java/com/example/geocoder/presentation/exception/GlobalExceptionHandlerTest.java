package com.example.geocoder.presentation.exception;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import com.example.geocoder.infrastructure.exception.ApiClientException;
import com.example.geocoder.infrastructure.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Тесты GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Должен обрабатывать BusinessException")
    void shouldHandleBusinessException() {
        BusinessException ex = new BusinessException("Ошибка бизнес-логики");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/test");

        ResponseEntity<ProblemDetail> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Ошибка обработки данных");
        assertThat(response.getBody().getDetail()).isEqualTo("Ошибка бизнес-логики");
    }

    @Test
    @DisplayName("Должен обрабатывать ApiClientException")
    void shouldHandleApiClientException() {
        ApiClientException ex = new ApiClientException("API недоступен");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/test");

        ResponseEntity<ProblemDetail> response = handler.handleApiClientException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Ошибка внешнего сервиса");
        assertThat(response.getBody().getDetail()).contains("API недоступен");
    }

    @Test
    @DisplayName("Должен обрабатывать ValidationException")
    void shouldHandleValidationException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("addressRequest", "address", "Адрес не может быть пустым");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/test");

        ResponseEntity<ProblemDetail> response = handler.handleValidationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Ошибка валидации данных");
    }

    @Test
    @DisplayName("Должен обрабатывать общие исключения")
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("Неизвестная ошибка");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/test");

        ResponseEntity<ProblemDetail> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Внутренняя ошибка сервера");
    }
}