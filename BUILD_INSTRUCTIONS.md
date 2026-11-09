# Wellness Tracker — build instructions (Practical Work 2)

This repository contains the source code of the Android user interface built with Kotlin, XML layouts, Material components and Android Gradle Plugin. The UI follows the prototype from Practical Work 1: Today, Activity, Diary, Map and Profile. Weekly statistics can be opened from Activity.

## Requirements

- Android Studio with Android SDK Platform 36 and SDK Build Tools installed
- JDK 17 or newer (Android Studio's bundled JDK is suitable)
- Windows PowerShell, or a macOS/Linux terminal
- Internet access for the first build to download Gradle dependencies

## Command-line APK build

1. Clone the public repository and enter it:

   ```powershell
   git clone https://github.com/Hamed745/WellnessTracker.git
   cd WellnessTracker
   ```

2. Make sure the Android SDK is installed. Android Studio normally generates `local.properties` with `sdk.dir` when you open the project. Alternatively set `ANDROID_HOME` to your Android SDK directory.

3. Build the debug APK in PowerShell:

   ```powershell
   .\gradlew.bat assembleDebug
   ```

   On macOS/Linux use `./gradlew assembleDebug` instead.

4. Find the generated APK at:

   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

5. Optionally run unit tests:

   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```

6. Optionally install on a connected Android device with USB debugging enabled:

   ```powershell
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Interface and test data

The interface is implemented with Android XML layout resources (not a web page). On first launch, enter a sample profile, for example: name `Alex`, age `22`, height `175 cm`, weight `70 kg`, city `Krasnoyarsk`. Activity values initially start at zero and can be populated with the **Add steps**, **+250 ml**, and **Log sleep** controls. The diary supports adding, editing and deleting local notes. The map opens an installed map application; weather uses Open-Meteo. Network access is not needed for locally stored activity and diary data.

The app uses real editable local data rather than pretending that sample records were obtained from a server. A step-counter sensor may not be available in an emulator; manual entry is supported.
