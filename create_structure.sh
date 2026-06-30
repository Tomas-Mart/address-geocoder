#!/bin/bash

echo "Создание структуры проекта..."
echo

# Создаем структуру папок
mkdir -p src/main/java/com/example/geocoder/presentation/controller
mkdir -p src/main/java/com/example/geocoder/presentation/dto
mkdir -p src/main/java/com/example/geocoder/presentation/exception
mkdir -p src/main/java/com/example/geocoder/application/service
mkdir -p src/main/java/com/example/geocoder/application/mapper
mkdir -p src/main/java/com/example/geocoder/domain/model
mkdir -p src/main/java/com/example/geocoder/domain/repository
mkdir -p src/main/java/com/example/geocoder/domain/service
mkdir -p src/main/java/com/example/geocoder/infrastructure/config
mkdir -p src/main/java/com/example/geocoder/infrastructure/client
mkdir -p src/main/java/com/example/geocoder/infrastructure/persistence/entity
mkdir -p src/main/java/com/example/geocoder/infrastructure/persistence/repository
mkdir -p src/main/java/com/example/geocoder/infrastructure/persistence/mapper
mkdir -p src/main/java/com/example/geocoder/infrastructure/exception
mkdir -p src/main/resources/db/migration
mkdir -p src/test/java/com/example/geocoder/integration
mkdir -p src/test/java/com/example/geocoder/unit
mkdir -p docker

# Создаем пустые файлы
touch src/main/java/com/example/geocoder/AddressGeocoderApplication.java
touch src/main/java/com/example/geocoder/presentation/controller/AddressController.java
touch src/main/java/com/example/geocoder/presentation/dto/AddressRequest.java
touch src/main/java/com/example/geocoder/presentation/dto/AddressResponse.java
touch src/main/java/com/example/geocoder/presentation/exception/GlobalExceptionHandler.java
touch src/main/java/com/example/geocoder/application/service/AddressGeocodingService.java
touch src/main/java/com/example/geocoder/application/mapper/AddressMapper.java
touch src/main/java/com/example/geocoder/domain/model/AddressAggregate.java
touch src/main/java/com/example/geocoder/domain/model/Coordinate.java
touch src/main/java/com/example/geocoder/domain/repository/AddressRepository.java
touch src/main/java/com/example/geocoder/domain/service/DistanceCalculator.java
touch src/main/java/com/example/geocoder/infrastructure/config/WebClientConfig.java
touch src/main/java/com/example/geocoder/infrastructure/config/AppConfig.java
touch src/main/java/com/example/geocoder/infrastructure/client/YandexGeocoderClient.java
touch src/main/java/com/example/geocoder/infrastructure/client/DadataGeocoderClient.java
touch src/main/java/com/example/geocoder/infrastructure/persistence/entity/AddressEntity.java
touch src/main/java/com/example/geocoder/infrastructure/persistence/repository/AddressJpaRepository.java
touch src/main/java/com/example/geocoder/infrastructure/persistence/mapper/AddressEntityMapper.java
touch src/main/java/com/example/geocoder/infrastructure/exception/ApiClientException.java
touch src/main/java/com/example/geocoder/infrastructure/exception/BusinessException.java
touch src/main/resources/application.yml
touch src/main/resources/db/migration/V1__create_address_table.sql
touch src/main/resources/logback-spring.xml
touch src/test/java/com/example/geocoder/integration/AddressGeocodingIntegrationTest.java
touch src/test/java/com/example/geocoder/unit/AddressGeocodingServiceTest.java
touch docker/Dockerfile
touch docker/docker-compose.yml
touch README.md
touch pom.xml

echo
echo "✅ Структура проекта успешно создана!"
echo "📁 Папка проекта: $(pwd)"
