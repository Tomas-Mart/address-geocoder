package com.example.geocoder.infrastructure.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Конфигурация OpenAPI для Swagger документации
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Address Geocoder API")
                        .version("1.0.0")
                        .description("Микросервис для геокодирования адресов с использованием Yandex Maps API и Dadata API")
                        .contact(new Contact()
                                .name("Ксения")
                                .email("tomas_mart@bk.ru")
                                .url("https://github.com/your-username/address-geocoder"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT"))
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Локальный сервер разработки")
                ));
    }
}