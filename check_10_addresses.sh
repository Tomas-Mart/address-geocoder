#!/bin/bash

echo "========================================="
echo "     ПРОВЕРКА 10 РАЗНЫХ АДРЕСОВ"
echo "========================================="
echo ""

# 1. Москва, Кремль
echo "1. Москва, Кремль"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Москва, Кремль"}' | jq '{originalAddress, distanceInMeters, processingStatus}'
echo ""

# 2. Москва, Красная площадь, 1
echo "2. Москва, Красная площадь, 1"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Москва, Красная площадь, 1"}' | jq '{originalAddress, distanceInMeters, processingStatus}'
echo ""

# 3. Санкт-Петербург, Дворцовая площадь, 2
echo "3. Санкт-Петербург, Дворцовая площадь, 2"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Санкт-Петербург, Дворцовая площадь, 2"}' | jq '{originalAddress, distanceInMeters, processingStatus}'
echo ""

# 4. Казань, Кремль
echo "4. Казань, Кремль"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Казань, Кремль"}' | jq '{originalAddress, distanceInMeters, processingStatus}'
echo ""

# 5. Нижний Новгород, Кремль
echo "5. Нижний Новгород, Кремль"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Нижний Новгород, Кремль"}' | jq '{originalAddress, distanceInMeters, processingStatus}'
echo ""

# 6. Екатеринбург, Площадь 1905 года
echo "6. Екатеринбург, Площадь 1905 года"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Екатеринбург, Площадь 1905 года"}' | jq '{originalAddress, distanceInMeters, processingStatus}'
echo ""

# 7. Новосибирск, Красный проспект, 1
echo "7. Новосибирск, Красный проспект, 1"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Новосибирск, Красный проспект, 1"}' | jq '{originalAddress, distanceInMeters, processingStatus}'
echo ""

# 8. Владивосток, Океанский проспект, 1
echo "8. Владивосток, Океанский проспект, 1"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Владивосток, Океанский проспект, 1"}' | jq '{originalAddress, distanceInMeters, processingStatus}'
echo ""

# 9. Сочи, Курортный проспект, 1
echo "9. Сочи, Курортный проспект, 1"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Сочи, Курортный проспект, 1"}' | jq '{originalAddress, distanceInMeters, processingStatus}'
echo ""

# 10. Якутск, площадь Ленина
echo "10. Якутск, площадь Ленина"
curl -s -X POST http://localhost:8080/api/address/geocode \
  -H "Content-Type: application/json" \
  -d '{"address": "Якутск, площадь Ленина"}' | jq '{originalAddress, distanceInMeters, processingStatus}'
echo ""

echo "========================================="
echo "     ПРОВЕРКА ЗАВЕРШЕНА"
echo "========================================="