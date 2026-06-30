-- Создание таблицы для хранения результатов геокодирования
CREATE TABLE IF NOT EXISTS addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_address VARCHAR(500) NOT NULL COMMENT 'Исходный адрес',

    -- Только Dadata координаты (Yandex не сохраняем по лицензионным причинам)
    dadata_latitude DOUBLE NOT NULL COMMENT 'Широта от Dadata',
    dadata_longitude DOUBLE NOT NULL COMMENT 'Долгота от Dadata',

    -- Результаты
    distance_in_meters DOUBLE COMMENT 'Расстояние между координатами в метрах',
    processing_status VARCHAR(50) DEFAULT 'SUCCESS' COMMENT 'Статус обработки',

    -- Метаданные
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Время обработки',

    -- Индексы для оптимизации запросов
    INDEX idx_original_address (original_address(255)),
    INDEX idx_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Комментарий к таблице (однострочный, без переноса)
ALTER TABLE addresses COMMENT = 'Хранятся только координаты от Dadata. Координаты Yandex не сохраняются согласно лицензионным требованиям.';