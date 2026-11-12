# Wellness Tracker

Android app (Kotlin, Views + Material 3, MVVM) that tracks steps, water and sleep and calculates
everything from the user's profile.

## What is calculated automatically
- Steps from the phone's step counter (manual entry also works)
- Distance and calories burned (stride = 0.415 × height; 0.5 kcal/kg/km)
- BMI and category, BMR (Mifflin–St Jeor), daily energy need
- Water goal (35 ml/kg), sleep goal (8 h), daily wellness score
- Weekly averages, totals, best day and a steps chart (Activity screen)

Weather uses the free Open-Meteo API (no key). The Map tab shows a real OpenStreetMap map (osmdroid) with markers for gyms, parks and running tracks found with the open Overpass API; tap a marker to see its description.

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
Today, Activity (weekly chart and summary), Diary (notes in a local SQLite database), Map (OpenStreetMap with markers), Profile, plus detail screens: note editor and weather (open Open-Meteo API).

User guide (Russian): [docs/USER_GUIDE_RU.md](docs/USER_GUIDE_RU.md) / [PDF](docs/USER_GUIDE_RU.pdf).

## Local database (CRUD)
Diary notes are stored in SQLite (`NotesDb`): `insert` (create), `all` / `get` (read), `update`, `delete`.
The Save / Delete buttons of the note editor call these methods through `MainViewModel`.
On-device test: `.\gradlew.bat connectedDebugAndroidTest`. Video demo scenario: [VIDEO_RU.md](VIDEO_RU.md).

## Publication, monetization and security
- Monetization and publication plan (Russian): [docs/MONETIZATION_PLAN_RU.md](docs/MONETIZATION_PLAN_RU.md)
- Security measures (Russian): [docs/SECURITY_RU.md](docs/SECURITY_RU.md)
- Privacy policy draft (Russian): [docs/PRIVACY_POLICY_RU.md](docs/PRIVACY_POLICY_RU.md)

Built-in protection: data stays on the phone, backups are disabled, HTTPS-only networking, parameterized SQL,
minimal permissions and a "Delete all my data" button in the profile.

## Map reliability (v1.6.3)
The Map tab uses OpenTopoMap raster tiles (HTTPS, with map-style attribution), identifies WellnessTracker in tile/API requests, keeps the osmdroid
local tile cache, and offers an in-app retry for nearby places. Nearby place requests use
HTTP POST to Overpass; if the primary server is temporarily unavailable (timeout/5xx), a
second public instance may be tried. Access-denied and rate-limit responses (4xx, including
403/429) are **not** bypassed. If the tile provider is unavailable, use the existing
"Search nearby in maps app" action. OSM tiles are best-effort and not guaranteed.

