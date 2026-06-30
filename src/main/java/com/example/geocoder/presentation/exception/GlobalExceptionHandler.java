package com.example.geocoder.presentation.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.example.geocoder.infrastructure.exception.ApiClientException;
import com.example.geocoder.infrastructure.exception.BusinessException;
import com.example.geocoder.infrastructure.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * Глобальный обработчик исключений с использованием Problem Details (RFC 7807)
 *
 * <p>Обрабатывает все исключения в приложении и возвращает структурированный ответ
 * в формате Problem Details (RFC 7807).
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка ошибок валидации (400)
     * Возникает при нарушении аннотаций @Valid, @NotBlank, @Size и т.д.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        log.warn("Ошибка валидации: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Ошибка валидации данных");
        problemDetail.setDetail("Проверьте правильность введенных данных");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("path", request.getDescription(false));

        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * Обработка ошибок бизнес-логики (422)
     * Возникает при нарушении бизнес-правил (пустой адрес, невалидные координаты и т.д.)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException ex, WebRequest request) {

        log.error("Ошибка бизнес-логики: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problemDetail.setTitle("Ошибка обработки данных");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getDescription(false));

        return ResponseEntity.unprocessableEntity().body(problemDetail);
    }

    /**
     * Обработка ошибок внешних API (503)
     * Возникает при ошибках вызова Yandex или Dadata API
     */
    @ExceptionHandler(ApiClientException.class)
    public ResponseEntity<ProblemDetail> handleApiClientException(
            ApiClientException ex, WebRequest request) {

        log.error("Ошибка внешнего API: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problemDetail.setTitle("Ошибка внешнего сервиса");
        problemDetail.setDetail("Сервис временно недоступен: " + ex.getMessage());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail);
    }

    /**
     * Обработка ошибки "Не найдено" (404)
     * Объединенный обработчик для всех случаев 404
     */
    @ExceptionHandler({
            NoSuchElementException.class,
            NotFoundException.class,
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<ProblemDetail> handleNotFoundExceptions(
            Exception ex, WebRequest request) {

        log.warn("Сущность или ресурс не найдены: {}", ex.getMessage());

        // Определяем заголовок в зависимости от типа исключения
        String title;
        if (ex instanceof NoHandlerFoundException || ex instanceof NoResourceFoundException) {
            title = "Ресурс не найден";
        } else {
            title = "Сущность не найдена";
        }

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle(title);
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Обработка ошибки "Не поддерживаемый тип" (415)
     * Возникает когда Content-Type не application/json
     */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotSupportedException(
            org.springframework.web.HttpMediaTypeNotSupportedException ex, WebRequest request) {

        log.warn("Неподдерживаемый тип медиа: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        problemDetail.setTitle("Неподдерживаемый тип данных");
        problemDetail.setDetail("Пожалуйста, используйте Content-Type: application/json");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problemDetail);
    }

    /**
     * Обработка ошибки "Неверный запрос" (400)
     * Общие ошибки запроса (отсутствие параметров, неверный тип и т.д.)
     */
    @ExceptionHandler({
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.ServletRequestBindingException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequestException(
            Exception ex, WebRequest request) {

        log.warn("Неверный запрос: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Неверный запрос");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getDescription(false));

        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * Обработка остальных исключений (500)
     * Непредвиденные ошибки, которые не были обработаны выше
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Необработанная ошибка: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Внутренняя ошибка сервера");
        problemDetail.setDetail("Произошла непредвиденная ошибка. Пожалуйста, попробуйте позже.");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getDescription(false));

        return ResponseEntity.internalServerError().body(problemDetail);
    }
}