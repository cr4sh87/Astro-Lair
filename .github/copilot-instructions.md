# Astro-Lair: AI Coding Agent Guide

## Project Overview
**Astro-Lair** is an Android astronomy observation companion app that provides moon phase calculations and weather-based observing conditions (seeing index). Built with Kotlin/Android, it integrates real-time weather data from Open-Meteo API.

### Architecture
- **Monolithic Android app** with single `MainActivity` orchestrating all UI and logic
- **No background services or multi-module structure** - all code in `com.cr4sh.astrolair` package
- **Astronomy logic** separated into `MoonPhaseCalculator` singleton object for moon phase calculations
- **Weather integration** handled inline in MainActivity via HTTP calls to Open-Meteo API

## Key Technical Stack
- **Language**: Kotlin 2.0.20
- **Framework**: Android 34 (minSdk 24, targetSdk 34)
- **Build System**: Gradle 8.7.0 with Kotlin DSL (`.kts` files)
- **Java Version**: JDK 17 with desugaring enabled for `java.time` on minSdk < 26
- **UI**: AppCompat framework, XML layouts in `res/layout/`
- **External API**: Open-Meteo free weather API (no auth required)

## Critical Architecture Patterns

### Moon Phase Calculation (`MoonPhaseCalculator` object)
- **Reference point**: UTC 2000-01-06 18:14:00 (known new moon)
- **Synodic month**: 29.53058867 days
- Returns `phase` as 0.0–1.0 where: 0.0 = new moon, 0.25 = first quarter, 0.5 = full moon, 0.75 = last quarter
- Uses `Duration.between()` to calculate elapsed days, then modulo operation for phase
- **Tolerance for phase descriptions**: ±0.03 (with wraparound for 0.0/1.0)

### Seeing Index Model (`computeSeeingIndex`)
Proprietary formula combining three weighted factors into 0–100 score:
- Cloud cover penalty: 60% weight (0–100%)
- Humidity penalty: 25% weight (0–60%)
- Wind penalty: 15% weight (0–~40%)
- Output scaled: 100 - weighted penalties, clamped to [0, 100]

### Weather Data Flow
1. **Fetch**: HTTP GET from Open-Meteo hourly forecast endpoint
2. **Parse**: Use org.json library to extract cloud_cover, wind_speed_10m, relative_humidity_2m arrays
3. **Extract**: Always use first hourly value (index 0) from response
4. **Thread model**: Runs on background thread, updates UI via `runOnUiThread {}`
5. **Error handling**: Catches exceptions and displays error message to user

## Important Implementation Details

### Coordinates
Hardcoded observation location (configurable for future):
- Latitude: 37.6, Longitude: 15.1 (Sicily area)
- Pass directly to Open-Meteo API; no geocoding layer

### Time Handling
- Use `ZonedDateTime` for all time calculations (respects timezones)
- Format pattern: `"yyyy-MM-dd HH:mm"`
- System timezone used for DatePickerDialog/TimePickerDialog
- Conversions: LocalDate ↔ LocalDateTime → ZonedDateTime (always for calculations)

### Dependencies
```gradle
androidx.core:core-ktx:1.13.1
androidx.appcompat:appcompat:1.7.0
org.json (bundled with Android)
com.android.tools:desugar_jdk_libs:2.1.2 (for java.time on older APIs)
```

### Resources Structure
- Layouts: `app/src/main/res/layout/activity_main.xml` contains:
  - `R.id.moon_selected_datetime` (TextView)
  - `R.id.moon_phase_text` (TextView)
  - `R.id.seeing_text` (TextView)
  - `R.id.button_now`, `R.id.button_pick_datetime`, `R.id.button_refresh_weather` (Buttons)

## Build & Run Commands

### Docker Build (Recommended)
```bash
docker build -t astro-lair .
docker run --rm -v $(pwd):/home/gradle/project astro-lair gradlew assembleDebug
```
Produces APK at `app/build/outputs/apk/debug/app-debug.apk`

### Local Build (Requires Android SDK)
```bash
./gradlew clean build
./gradlew assembleDebug  # Creates debug APK
./gradlew installDebug   # Installs on connected device
```

### Gradle Properties
Configured in `gradle.properties`:
- AndroidX enabled
- Jetifier enabled (for legacy library compatibility)
- Daemon mode + 2GB JVM heap

## Common Development Workflows

### Adding New UI Elements
1. Add to `res/layout/activity_main.xml`
2. Reference in `MainActivity.onCreate()` with `findViewById(R.id....)`
3. Bind listeners in `onCreate()`
4. Update `TextView` displays in corresponding handler methods

### Modifying Moon Phase Logic
Edit `MoonPhaseCalculator.phaseFraction()` for calculation changes or `describePhase()` for phase descriptions. Reference new moon is immutable for consistency.

### Extending Weather Integration
- Modify Open-Meteo URL parameters in `fetchWeatherAndSeeing()` method
- Keep thread-based async pattern for non-blocking UI
- Always call `runOnUiThread {}` before updating UI TextViews
- Test error paths with network simulation tools

### Adjusting Seeing Index Weights
Edit `computeSeeingIndex()` penalty weights and coefficients. Current weights: cloud 0.6, humidity 0.25, wind 0.15. Remember final output must remain [0, 100].

## Code Style & Conventions
- **Language**: Idiomatic Kotlin (val by default, data classes for models)
- **Naming**: camelCase for properties/methods, PascalCase for classes/objects
- **Comments**: Italian and English mixed (observe existing pattern)
- **Permissions**: INTERNET required in AndroidManifest.xml (already present)
- **No architecture frameworks**: No Jetpack Compose, Navigation, ViewModel currently in use

## Known Constraints
- Single-threaded UI updates (all weather/API calls must call `runOnUiThread{}`)
- No persistence layer (data lost on app close)
- Fixed observation coordinates (hardcoded)
- Open-Meteo forecast limited to 1 day + current hour only
- Simple seeing model (no advanced atmospheric optics)
