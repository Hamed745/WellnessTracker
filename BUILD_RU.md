# Инструкция по сборке APK из командной строки

Приложение: **Wellness Tracker** (Kotlin, Android, Gradle). Ниже — как собрать `apk`-файл без Android Studio,
только средствами командной строки.

## 1. Что нужно установить

| Что | Версия | Как проверить |
|---|---|---|
| JDK | 17 или новее (проверено на JDK 21; JDK 25 **не** подходит) | `java -version` |
| Android SDK | платформа **Android 36** и Build-Tools | папка `...\Android\Sdk` |
| Интернет | при первой сборке (Gradle скачает зависимости, в том числе библиотеку карты osmdroid); при работе приложения — для карты, погоды и поиска мест | — |

Проще всего поставить **Android Studio** — вместе с ней устанавливается SDK
(Windows: `C:\Users\<имя>\AppData\Local\Android\Sdk`). Открывать Android Studio для сборки не нужно.

## 2. Получить исходный код

```
git clone https://github.com/Hamed745/WellnessTracker.git
cd WellnessTracker
```

Или распаковать архив с проектом и перейти в папку, где лежит файл `gradlew.bat`.

## 3. Указать пути к JDK и SDK

**Windows (PowerShell):**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"   # путь к вашему JDK
$sdk = ("$env:LOCALAPPDATA\Android\Sdk") -replace '\\','/'
Set-Content -Path local.properties -Value "sdk.dir=$sdk" -Encoding ASCII
```

**Linux / macOS (bash):**

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
chmod +x gradlew
```

Файл `local.properties` содержит путь к SDK на вашем компьютере, поэтому он не хранится в репозитории.

## 4. Собрать debug-APK

```powershell
.\gradlew.bat --no-daemon --console=plain assembleDebug      # Windows
./gradlew --no-daemon --console=plain assembleDebug          # Linux / macOS
```

В конце должно появиться `BUILD SUCCESSFUL`. Первая сборка занимает 2–5 минут.

Готовый файл: `app/build/outputs/apk/debug/app-debug.apk`

## 5. Запустить тесты (по желанию)

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest
```

Тесты проверяют формулы (ИМТ, калории, норма воды и др.).

## 5.1. Тесты базы данных на устройстве (по желанию)

Нужен подключённый телефон или запущенный эмулятор:

```powershell
.\gradlew.bat --no-daemon connectedDebugAndroidTest
```

Тест `NotesDbTest` проверяет операции CRUD (создание, чтение, изменение, удаление заметок).

## 6. Установить на телефон

1. На телефоне: *Настройки → О телефоне →* 7 раз нажать на «Номер сборки», затем
   *Для разработчиков → Отладка по USB* — включить.
2. Подключить телефон к компьютеру кабелем и разрешить отладку на экране телефона.
3. Выполнить (путь к `adb` — в папке `Sdk\platform-tools`):

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices                                                  # в списке должно быть "device"
& $adb install -r "app\build\outputs\apk\debug\app-debug.apk"
& $adb shell am start -n com.example.welnesstracker/.MainActivity
```

## 7. Release-сборка для публикации (необязательно)

Для магазинов нужна подписанная release-версия. Ключ подписи создаётся один раз и хранится **вне репозитория**
(файлы `*.jks` и `*.keystore` добавлены в `.gitignore`). Потеряв ключ, обновлять приложение будет невозможно,
поэтому сделайте его резервную копию.

```powershell
# 1. Создать ключ подписи (один раз)
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -keystore wellness-release.jks -alias wellness -keyalg RSA -keysize 2048 -validity 10000

# 2. Собрать release-APK (без подписи) и пакет AAB для Google Play
.\gradlew.bat --no-daemon assembleRelease bundleRelease

# 3. Выровнять и подписать APK (версию build-tools подставьте свою)
$bt = (Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" | Sort-Object Name | Select-Object -Last 1).FullName
& "$bt\zipalign.exe" -p 4 app\build\outputs\apk\release\app-release-unsigned.apk app-release-aligned.apk
& "$bt\apksigner.bat" sign --ks wellness-release.jks --out app-release.apk app-release-aligned.apk
& "$bt\apksigner.bat" verify app-release.apk
```

Пакет AAB: `app/build/outputs/bundle/release/app-release.aab`. В Google Play его подписывает сам магазин
(Play App Signing), а загружать нужно AAB, подписанный ключом загрузки. Перед публикацией включите сжатие и обфускацию R8
(`isMinifyEnabled = true` в release-сборке) и обязательно проверьте release-версию на телефоне.

## 8. Частые ошибки

| Сообщение | Что делать |
|---|---|
| `SDK location not found` | Создать `local.properties` с `sdk.dir=...` (пункт 3) |
| `JAVA_HOME is set to an invalid directory` | Указать правильную папку JDK (без `\bin`) |
| `Unsupported class file major version` | Версия JDK слишком новая — использовать JDK 17–21 |
| `License ... not accepted` | `sdkmanager --licenses` и согласиться со всеми лицензиями |
| `Could not resolve ...` / ошибки загрузки | Проверить интернет и повторить сборку |
