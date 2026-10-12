# Выпуск Pulse Player в RuStore

## 1. Создать ключи локально на Windows

Ключи и пароли нельзя отправлять в чат или добавлять в GitHub. Храните резервную копию в двух защищённых местах.

Для первого AAB RuStore нужны два RSA-ключа размером не менее 2048 бит: ключ подписи приложения и отдельный ключ загрузки. В Android Studio откройте **Build → Generate Signed Bundle / APK → Android App Bundle → Create new** и создайте хранилище вне папки проекта, например `C:\Users\Максим\Keys\pulse-release.keystore`.

Создайте ключ подписи с alias `pulse-sign`, сроком действия 100 лет. Затем в терминале Android Studio создайте в том же хранилище ключ загрузки:

```powershell
keytool -keystore "C:\Users\Максим\Keys\pulse-release.keystore" -genkeypair -alias pulse-upload -keyalg RSA -keysize 2048 -validity 36500
```

Экспортируйте публичный сертификат ключа загрузки:

```powershell
keytool -exportcert -alias pulse-upload -keystore "C:\Users\Максим\Keys\pulse-release.keystore" -rfc -file "C:\Users\Максим\Keys\pulse-upload.pem"
```

Скопируйте `keystore.properties.example` как `keystore.properties`. Укажите путь к хранилищу и alias `pulse-upload`. Этот файл и хранилище уже исключены из Git.

В RuStore Консоли скачайте `pepk.jar` и используйте сформированную консолью команду с alias `pulse-sign`. Загрузите полученный ZIP с подписью приложения и `pulse-upload.pem`.

## 2. Настроить рекламу

Создайте приложение и адаптивный баннер в Рекламной сети Яндекса. Реальный идентификатор вида `R-M-...` добавьте в пользовательский файл `%USERPROFILE%\.gradle\gradle.properties`:

```properties
PULSE_YANDEX_BANNER_ID=R-M-XXXXXXXX-X
```

Debug-сборка использует официальный тестовый идентификатор. В release без свойства баннер скрыт, поэтому перед публикацией обязательно проверьте значение.

## 3. Собрать и проверить

1. Синхронизируйте Gradle и выполните **Build → Clean Project**.
2. Установите release-сборку на реальный телефон с Android 8 или новее.
3. Проверьте доступ к музыке, фон, уведомление, экран блокировки, виджет, эквалайзер, плейлисты, очередь, таймер сна и баннер.
4. Выполните **Build → Generate Signed Bundle / APK → Android App Bundle** и подпишите alias `pulse-upload`.
5. Итоговый файл: `app\release\app-release.aab`.

## 4. Карточка RuStore

- Название: `Pulse: музыкальный плеер`.
- Категория: `Музыка и аудио`.
- Цена: бесплатно, содержит рекламу.
- Политика: `https://github.com/maxx52/PulsePlayer/blob/main/docs/PRIVACY_POLICY.md`.
- Загрузите иконку 512×512 с непрозрачным фоном.
- Добавьте минимум три актуальных вертикальных скриншота: медиатека, экран плеера, эквалайзер/виджет.
- Укажите рабочий контакт разработчика.
- В декларации разрешений объясните доступ к аудио воспроизведением локальных файлов, уведомления — фоновым управлением, интернет и состояние сети — загрузкой рекламы.

После загрузки AAB сначала используйте закрытое тестирование, затем отправляйте версию на модерацию.
