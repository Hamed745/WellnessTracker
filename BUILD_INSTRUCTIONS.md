# Wellness Tracker — building the APK from the command line

Russian version: [BUILD_RU.md](BUILD_RU.md).

## Requirements
- **JDK 17–21** (tested with JDK 21; JDK 25 is not supported by this Gradle version)
- **Android SDK** with platform 36 and Build-Tools (installed together with Android Studio)
- Internet access for the first build (Gradle downloads dependencies)

## Steps (Windows PowerShell)
```powershell
git clone https://github.com/Hamed745/WellnessTracker.git
cd WellnessTracker

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"   # your JDK folder
$sdk = ("$env:LOCALAPPDATA\Android\Sdk") -replace '\\','/'
Set-Content -Path local.properties -Value "sdk.dir=$sdk" -Encoding ASCII

.\gradlew.bat --no-daemon --console=plain assembleDebug
```
macOS / Linux: `echo "sdk.dir=$HOME/Android/Sdk" > local.properties && chmod +x gradlew && ./gradlew assembleDebug`.

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

Unit tests: `.\gradlew.bat testDebugUnitTest`.

Install on a phone (USB debugging enabled):
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n com.example.welnesstracker/.MainActivity
```

## What to look at in the app
Today · Activity (weekly chart + summary) · Diary (notes in a local SQLite database, add / edit / delete) ·
Map (static map mockup with example places, tap a pin) · Profile (automatic calculations). Tap the weather card on
the Today screen to open the weather screen (Open-Meteo API). Use the **EN | RU** switch in the top bar to change
the language. Activity data starts empty: use the buttons on the Today screen (+500 steps, +250 ml, log sleep) or
the phone's step counter.
