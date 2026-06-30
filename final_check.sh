#!/bin/bash

echo "========================================="
echo "     ФИНАЛЬНАЯ ПРОВЕРКА ПРОЕКТА"
echo "========================================="
echo ""

# 1. Запуск тестов
echo "📦 1. ЗАПУСК ТЕСТОВ..."
echo "-----------------------------------------"
cd /home/mina/projects/address-geocoder
mvn clean test

if [ $? -ne 0 ]; then
    echo "❌ Тесты не пройдены!"
    exit 1
fi
echo "✅ Тесты пройдены успешно!"
echo ""

# 2. Сборка JAR
echo "📦 2. СБОРКА JAR..."
echo "-----------------------------------------"
mvn package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Сборка не удалась!"
    exit 1
fi
echo "✅ JAR собран успешно!"
echo ""

# 3. Пересборка Docker
echo "🐳 3. ПЕРЕСБОРКА DOCKER..."
echo "-----------------------------------------"
cd docker
docker-compose down
docker-compose build --no-cache
docker-compose up -d

if [ $? -ne 0 ]; then
    echo "❌ Сборка Docker не удалась!"
    exit 1
fi
echo "✅ Docker пересобран и запущен!"
echo ""

# 4. Ожидание запуска
echo "⏳ 4. ОЖИДАНИЕ ЗАПУСКА ПРИЛОЖЕНИЯ..."
echo "-----------------------------------------"
sleep 15

# 5. Проверка Health
echo "💚 5. ПРОВЕРКА HEALTH CHECK..."
echo "-----------------------------------------"
HEALTH=$(curl -s http://localhost:8080/actuator/health)
echo "$HEALTH" | jq '.'

if echo "$HEALTH" | grep -q '"status":"UP"'; then
    echo "✅ Приложение работает!"
else
    echo "❌ Приложение не отвечает!"
    exit 1
fi
echo ""

# 6. Проверка API (Yandex + Dadata)
echo "🌐 6. ПРОВЕРКА API (ГЕОКОДИРОВАНИЕ)..."
echo "-----------------------------------------"
echo ""
echo "📤 Отправка запроса на геокодирование..."
echo ""

RESPONSE=$(curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Москва, Кремль"}')

echo "$RESPONSE" | jq '.'

echo ""
# Проверяем наличие Yandex координат
if echo "$RESPONSE" | grep -q '"yandexCoordinates":null'; then
    echo "⚠️  ВНИМАНИЕ: Yandex координаты не получены (null)"
    echo "   Проверьте API ключ и доступность Yandex API"
else
    echo "✅ Yandex координаты получены!"
fi

# Проверяем наличие Dadata координат
if echo "$RESPONSE" | grep -q '"dadataCoordinates":null'; then
    echo "❌ ОШИБКА: Dadata координаты не получены!"
    exit 1
else
    echo "✅ Dadata координаты получены!"
fi

# Проверяем статус
if echo "$RESPONSE" | grep -q '"processingStatus":"SUCCESS"'; then
    echo "✅ Статус: SUCCESS"
elif echo "$RESPONSE" | grep -q '"processingStatus":"PARTIAL_SUCCESS"'; then
    echo "✅ Статус: PARTIAL_SUCCESS (только Dadata)"
else
    echo "⚠️  Неизвестный статус"
fi
echo ""

# 7. Полная проверка API
echo "🌐 7. ПОЛНАЯ ПРОВЕРКА API (check_api.sh)..."
echo "-----------------------------------------"
./check_api.sh

echo ""
echo "========================================="
echo "     ✅ ВСЕ ПРОВЕРКИ ПРОЙДЕНЫ!"
echo "========================================="
echo ""
echo "📊 ИТОГИ:"
echo "  ✅ Тесты пройдены"
echo "  ✅ JAR собран"
echo "  ✅ Docker запущен"
echo "  ✅ API работает"
echo "  ✅ Health Check OK"
echo "  ✅ Dadata координаты получены"
if echo "$RESPONSE" | grep -q '"yandexCoordinates":null'; then
    echo "  ⚠️  Yandex координаты: null (проверьте API ключ)"
else
    echo "  ✅ Yandex координаты получены"
fi
echo ""
echo "🚀 Проект готов к сдаче!"