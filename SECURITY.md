# Безопасность Firebase конфигурации

## ⚠️ ВАЖНО: Файл google-services.json НЕ должен попадать в Git!

Файл `app/google-services.json` содержит приватные ключи Firebase и **НЕ ДОЛЖЕН** храниться в публичном репозитории.

## Настройка для разработки

1. **Скопируйте шаблон:**
   ```bash
   cp google-services.json.template app/google-services.json
   ```

2. **Получите свой файл из Firebase Console:**
   - Перейдите в [Firebase Console](https://console.firebase.google.com/)
   - Выберите проект MemoryGame (или создайте новый)
   - Project Settings → General → Your apps → Android app
   - Скачайте `google-services.json`
   - Замените содержимое `app/google-services.json` скачанным файлом

3. **Проверьте, что файл в .gitignore:**
   ```bash
   git check-ignore app/google-services.json
   # Должно вывести: app/google-services.json
   ```

## Что делать если ключ уже был опубликован?

Если ключ уже был запушен в GitHub:

1. **Удалите ключ из репозитория** (уже сделано в этом коммите)
2. **Удалите из истории Git:**
   ```bash
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch app/google-services.json" \
     --prune-empty --tag-name-filter cat -- --all
   ```
3. **Принудительно запушьте изменения:**
   ```bash
   git push origin --force --all
   ```
4. **Обновите ключи в Firebase Console:**
   - Перейдите в Project Settings
   - Удалите старое приложение и создайте новое с новыми ключами
   - ИЛИ сгенерируйте новый API ключ

## Файлы которые защищены .gitignore

- `app/google-services.json` - Основной файл конфигурации Firebase
- `google-services.json` - На случай если файл окажется в корне

## Подробная инструкция по настройке Firebase

См. файл [FIREBASE_SETUP_INSTRUCTIONS.md](./FIREBASE_SETUP_INSTRUCTIONS.md)
