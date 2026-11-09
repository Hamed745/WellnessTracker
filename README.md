# Wellness Tracker

Android app (Kotlin, Views + Material 3, MVVM) that tracks steps, water and sleep and calculates
everything from the user's profile.

## What is calculated automatically
- Steps from the phone's step counter (manual entry also works)
- Distance and calories burned (stride = 0.415 × height; 0.5 kcal/kg/km)
- BMI and category, BMR (Mifflin–St Jeor), daily energy need
- Water goal (35 ml/kg), sleep goal (8 h), daily wellness score
- Weekly averages, totals, best day and a steps chart (Activity screen)

Weather uses the free Open-Meteo API (no key). The Map tab shows a static map mockup with example places; its buttons open your maps app.

## Build
Step-by-step command-line build instructions: [BUILD_RU.md](BUILD_RU.md) (Russian), [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) (English).

Open the folder in Android Studio (Narwhal or newer, JDK 17+) and press Run, or from a terminal:

    ./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
    ./gradlew testDebugUnitTest    # unit tests for the formulas

Android Studio creates `local.properties` with your SDK path automatically.

## Structure
- `data/` models, local storage (SharedPreferences + JSON for daily data, SQLite for diary notes), date helpers
- `domain/HealthCalculator.kt` all formulas (unit-tested)
- `network/WeatherService.kt` weather API
- `ui/` ViewModel, fragments, bar chart view

## Background step counting
`StepCounterService` is a foreground service (shows a small "counting your steps" notification) that listens to the
hardware step counter even when the app is closed, so steps are always saved on the correct day. No internet or
account is needed. For reliable results, allow notifications and set the app's battery mode to "Unrestricted".

## Language
The app is fully available in English and Russian. Use the **EN | RU** switch in the top bar to change the language
inside the app (it is remembered); by default the language of the phone is used. All screens, dialogs, messages,
weather descriptions, dates and the step-counter notification follow the selected language.

## Screens
Today, Activity (weekly chart and summary), Diary (notes in a local SQLite database), Map (static map mockup with
example places), Profile, plus detail screens: note editor and weather (open Open-Meteo API).
