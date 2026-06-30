package com.example.geocoder.infrastructure.exception;

/**
 * Исключение для ошибок при вызове внешних API.
 * Используется, когда:
 * <ul>
 *   <li>Внешний API вернул ошибку</li>
 *   <li>Произошла ошибка сети или таймаут</li>
 *   <li>Ответ API не может быть обработан</li>
 * </ul>
 */
public class ApiClientException extends RuntimeException {

    public ApiClientException(String message) {
        super(message);
    }

    public ApiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}