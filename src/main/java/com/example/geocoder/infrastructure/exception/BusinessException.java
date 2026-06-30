package com.example.geocoder.infrastructure.exception;

/**
 * Исключение для ошибок бизнес-логики.
 * Используется, когда:
 * <ul>
 *   <li>Нарушены бизнес-правила</li>
 *   <li>Входные данные некорректны</li>
 *   <li>Операция не может быть выполнена</li>
 * </ul>
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}