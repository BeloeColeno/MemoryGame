# ИНСТРУКЦИЯ ПО НАСТРОЙКЕ FIREBASE ДЛЯ ОНЛАЙН-РЕЖИМА

## Шаг 1: Создание проекта Firebase

1. Перейдите на https://console.firebase.google.com/
2. Нажмите "Добавить проект" или "Create a project"
3. Введите название проекта (например, "MemoryGame")
4. Отключите Google Analytics (не обязательно для этого проекта)
5. Нажмите "Создать проект"

## Шаг 2: Добавление Android-приложения

1. В консоли Firebase выберите свой проект
2. Нажмите на иконку Android (или "Добавить приложение" → Android)
3. Заполните форму:
   - **Android package name**: `com.petrov.memory`
   - **App nickname** (опционально): MemoryGame
   - **Debug signing certificate SHA-1** (опционально, можно пропустить)
4. Нажмите "Зарегистрировать приложение"

## Шаг 3: Загрузка google-services.json

1. После регистрации приложения Firebase предложит скачать файл `google-services.json`
2. Нажмите "Download google-services.json"
3. **ВАЖНО**: Скопируйте этот файл в папку:
   ```
   MemoryGame/app/google-services.json
   ```
   (НЕ в папку `src`, а напрямую в папку `app`)

## Шаг 4: Настройка Firebase Realtime Database

1. В левом меню консоли Firebase выберите "Build" → "Realtime Database"
2. Нажмите "Создать базу данных" ("Create Database")
3. Выберите регион (например, europe-west1)
4. Выберите режим безопасности: "Start in test mode" (для разработки)
5. Нажмите "Включить" ("Enable")

## Шаг 5: Настройка правил безопасности (временные, для тестирования)

В разделе "Realtime Database" → вкладка "Rules", замените правила на:

```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",
    "rooms": {
      "$roomId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

Нажмите "Опубликовать" ("Publish")

**ВАЖНО**: Эти правила разрешают доступ всем аутентифицированным пользователям.
Для production-версии нужно настроить более строгие правила!

## Шаг 6: Включение Anonymous Authentication

1. В левом меню выберите "Build" → "Authentication"
2. Нажмите "Get started" (если первый раз)
3. Перейдите на вкладку "Sign-in method"
4. Найдите "Anonymous" в списке провайдеров
5. Включите переключатель ("Enable")
6. Нажмите "Save"

## Шаг 7: Проверка настройки

После выполнения всех шагов:

1. Убедитесь, что файл `google-services.json` находится в папке `app/`
2. Синхронизируйте проект с Gradle: File → Sync Project with Gradle Files
3. Соберите проект: Build → Make Project

## Структура файлов должна быть такой:

```
MemoryGame/
├── app/
│   ├── google-services.json  ← ФАЙЛ FIREBASE ДОЛЖЕН БЫТЬ ЗДЕСЬ
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           └── java/...
├── build.gradle.kts
└── settings.gradle.kts
```

## Возможные проблемы и решения

### Ошибка: "google-services.json not found"
**Решение**: Убедитесь, что файл находится в папке `app/`, а не в `app/src/`

### Ошибка сборки с плагином google-services
**Решение**: Проверьте, что в `app/build.gradle.kts` есть строка:
```kotlin
id("com.google.gms.google-services")
```

### База данных не работает
**Решение**: 
1. Проверьте правила безопасности в Firebase Console
2. Убедитесь, что Anonymous Authentication включена
3. Проверьте интернет-соединение на устройстве

## Тестирование онлайн-режима

1. Запустите приложение на двух устройствах (или эмуляторах)
2. На первом устройстве: Главное меню → Онлайн игра → Создать комнату
3. Скопируйте ID комнаты (отображается на экране ожидания)
4. На втором устройстве: Главное меню → Онлайн игра → Список комнат → Присоединиться
5. На первом устройстве нажмите "Начать игру"

## Дополнительная информация

- Firebase документация: https://firebase.google.com/docs/android/setup
- Realtime Database: https://firebase.google.com/docs/database/android/start

---

## БЕЗОПАСНОСТЬ ДЛЯ PRODUCTION

Перед публикацией в Google Play обязательно настройте правила безопасности:

```json
{
  "rules": {
    ".read": false,
    ".write": false,
    "rooms": {
      "$roomId": {
        ".read": "auth != null && (
          data.child('hostPlayerId').val() == auth.uid || 
          data.child('guestPlayerId').val() == auth.uid
        )",
        ".write": "auth != null && (
          !data.exists() || 
          data.child('hostPlayerId').val() == auth.uid || 
          data.child('guestPlayerId').val() == auth.uid
        )"
      }
    }
  }
}
```

Это ограничит доступ к комнатам только участникам игры.
