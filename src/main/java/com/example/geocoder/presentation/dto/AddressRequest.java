package com.example.geocoder.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO для входящего запроса на геокодирование
 * Используется в POST /api/address/geocode
 *
 * <p>Пример использования:
 * <pre>
 * {
 *   "address": "Москва, Красная площадь, 1"
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "AddressRequest",
        description = "Запрос на геокодирование адреса",
        example = """
                {
                  "address": "Москва, Кремль"
                }
                """
)
public class AddressRequest {

    @NotBlank(message = "Адрес не может быть пустым")
    @Size(max = 500, message = "Адрес не должен превышать 500 символов")
    @Schema(
            description = "Текстовый адрес для геокодирования",
            example = "Москва, Кремль",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 500
    )
    private String address;
}