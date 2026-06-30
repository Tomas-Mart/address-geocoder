#!/bin/bash

# =========================================
# Address Geocoder - Полная проверка API
# =========================================

echo "========================================="
echo "     ПРОВЕРКА ВСЕХ СЦЕНАРИЕВ API"
echo "========================================="
echo ""

# Проверка, что Docker запущен
if ! docker ps &> /dev/null; then
    echo "❌ Docker не запущен!"
    echo "Пожалуйста, запустите Docker и выполните:"
    echo "  cd docker && docker-compose up -d"
    exit 1
fi

# Проверка, что контейнеры запущены
if ! docker ps | grep -q "geocoder-app"; then
    echo "❌ Контейнер geocoder-app не запущен!"
    echo "Пожалуйста, выполните:"
    echo "  cd docker && docker-compose up -d"
    exit 1
fi

# Проверка, что приложение отвечает
echo "⏳ Проверка доступности приложения..."
if ! curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q "200"; then
    echo "❌ Приложение не отвечает на порту 8080!"
    echo "Пожалуйста, проверьте логи: docker logs geocoder-app"
    exit 1
fi
echo "✅ Приложение доступно"
echo ""

echo "========================================="
echo "НАЧАЛО ПРОВЕРКИ"
echo "========================================="
echo ""

# ==========================================
# 1. POST - Москва, Кремль
# ==========================================
echo "1. POST /api/address/geocode - Москва, Кремль"
echo "-----------------------------------------"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Москва, Кремль"}' | jq '.'
echo ""
echo ""

# ==========================================
# 2. POST - Москва, Красная площадь
# ==========================================
echo "2. POST /api/address/geocode - Москва, Красная площадь, 1"
echo "-----------------------------------------"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Москва, Красная площадь, 1"}' | jq '.'
echo ""
echo ""

# ==========================================
# 3. POST - Санкт-Петербург
# ==========================================
echo "3. POST /api/address/geocode - Санкт-Петербург, Дворцовая площадь, 2"
echo "-----------------------------------------"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Санкт-Петербург, Дворцовая площадь, 2"}' | jq '.'
echo ""
echo ""

# ==========================================
# 4. POST - адрес без запятой
# ==========================================
echo "4. POST /api/address/geocode - Москва Кремль (без запятой)"
echo "-----------------------------------------"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Москва Кремль"}' | jq '.'
echo ""
echo ""

# ==========================================
# 5. POST - пустой адрес (должен вернуть 400)
# ==========================================
echo "5. POST /api/address/geocode - пустой адрес (должен вернуть 400)"
echo "-----------------------------------------"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": ""}' | jq '.'
echo ""
echo ""

# ==========================================
# 6. POST - очень длинный адрес (должен вернуть 400)
# ==========================================
echo "6. POST /api/address/geocode - очень длинный адрес (должен вернуть 400)"
echo "-----------------------------------------"
LONG_ADDRESS=$(printf 'a%.0s' {1..501})
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d "{\"address\": \"$LONG_ADDRESS\"}" | jq '.'
echo ""
echo ""

# ==========================================
# 7. GET - список всех адресов
# ==========================================
echo "7. GET /api/address - список всех адресов"
echo "-----------------------------------------"
curl -s -X GET http://localhost:8080/api/address | jq '.'
echo ""
echo ""

# ==========================================
# 8. GET - адрес по ID
# ==========================================
echo "8. GET /api/address/1 - адрес по ID"
echo "-----------------------------------------"
curl -s -X GET http://localhost:8080/api/address/1 | jq '.'
echo ""
echo ""

# ==========================================
# 9. GET - несуществующий ID (должен вернуть 404)
# ==========================================
echo "9. GET /api/address/999 - несуществующий ID (должен вернуть 404)"
echo "-----------------------------------------"
curl -s -X GET http://localhost:8080/api/address/999 | jq '.'
echo ""
echo ""

# ==========================================
# 10. Health Check
# ==========================================
echo "10. GET /actuator/health - Health Check"
echo "-----------------------------------------"
curl -s http://localhost:8080/actuator/health | jq '.'
echo ""
echo ""

echo "========================================="
echo "     ПРОВЕРКА ЗАВЕРШЕНА"
echo "========================================="
echo ""
echo "📊 Статистика:"
echo "  • Всего запросов: 10"
echo "  • POST запросов: 6"
echo "  • GET запросов: 4"
echo ""
echo "✅ Ожидаемые статусы:"
echo "  • Запросы 1-4: 201 CREATED"
echo "  • Запрос 5 (пустой): 400 BAD REQUEST"
echo "  • Запрос 6 (длинный): 400 BAD REQUEST"
echo "  • Запрос 7 (список): 200 OK"
echo "  • Запрос 8 (ID=1): 200 OK"
echo "  • Запрос 9 (ID=999): 404 NOT FOUND"
echo "  • Запрос 10 (health): 200 OK"
echo ""
echo "========================================="