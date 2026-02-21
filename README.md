# RunTracker - Android Exercise App

A comprehensive running and gym tracking app for Android phones and Wear OS (Pixel Watch).

## Features

### Running Features
- **GPS Tracking**: Real-time location tracking with pace, distance, elevation
- **Heart Rate Monitoring**: Integration with Wear OS for heart rate data
- **Split Times**: Automatic kilometer splits with pace analysis
- **Run Analytics**: Pace progression, weekly distance charts, personal records
- **Training Plans**: AI-generated training plans for various goals (5K, 10K, Half Marathon, Marathon)
- **Strava Import**: Connect and import activities from Strava

### Gym Features
- **Exercise Library**: 60+ exercises organized by muscle group and equipment
- **Workout Tracking**: Log sets, reps, and weight for each exercise
- **Rest Timer**: Automatic rest timer between sets with skip option
- **Workout Templates**: Pre-built templates (Push/Pull/Legs, Upper/Lower, Full Body)
- **Progress Tracking**: Track volume, personal records, and 1RM estimates
- **Smart Suggestions**: AI-powered weight/rep progression recommendations based on:
  - Recent performance history
  - Rep range targets
  - Deload detection when performance declines
- **Exercise History**: View progress over time for each exercise

### Wear OS Companion App
- **Standalone Tracking**: Track runs directly from your Pixel Watch
- **Health Services Integration**: Uses Android Health Services for accurate metrics
- **Phone Sync**: Automatic sync of runs to phone app
- **Heart Rate Streaming**: Real-time heart rate data sent to phone during runs

## Project Structure

```
├── app/                    # Phone app module
│   ├── src/main/java/com/runtracker/app/
│   │   ├── di/            # Dependency injection (Hilt)
│   │   ├── service/       # Background services
│   │   └── ui/            # Compose UI screens
│   └── src/main/res/      # Resources
├── wear/                   # Wear OS app module
│   └── src/main/java/com/runtracker/wear/
│       ├── presentation/  # Wear Compose UI
│       └── service/       # Watch services
└── shared/                 # Shared module
    └── src/main/java/com/runtracker/shared/
        ├── data/          # Data models, Room DB, repositories
        ├── location/      # Location tracking utilities
        ├── sync/          # Phone-watch sync
        └── training/      # Training plan generator
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Phone), Wear Compose (Watch)
- **Architecture**: MVVM with Repository pattern
- **DI**: Hilt
- **Database**: Room
- **Location**: Google Play Services Location
- **Health**: Android Health Services (Wear OS)
- **Sync**: Wearable Data Layer API

## Setup

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34
- A physical Android device or emulator (API 26+)
- Wear OS device or emulator (API 30+) for watch features

### API Keys Required

1. **Google Maps API Key** (for map features)
   - Get from [Google Cloud Console](https://console.cloud.google.com/)
   - Add to `app/src/main/AndroidManifest.xml`

2. **Strava API Credentials** (for Strava import)
   - Register at [Strava Developers](https://developers.strava.com/)
   - Update in `app/build.gradle.kts`:
     ```kotlin
     buildConfigField("String", "STRAVA_CLIENT_ID", "\"YOUR_CLIENT_ID\"")
     buildConfigField("String", "STRAVA_CLIENT_SECRET", "\"YOUR_SECRET\"")
     ```

### Building

```bash
# Build phone app
./gradlew :app:assembleDebug

# Build wear app
./gradlew :wear:assembleDebug

# Build both
./gradlew assembleDebug
```

### Installing

```bash
# Install phone app
adb install app/build/outputs/apk/debug/app-debug.apk

# Install wear app (connect watch via ADB)
adb -s <watch-device-id> install wear/build/outputs/apk/debug/wear-debug.apk
```

## Permissions

### Phone App
- `ACCESS_FINE_LOCATION` - GPS tracking
- `ACCESS_BACKGROUND_LOCATION` - Background tracking
- `FOREGROUND_SERVICE` - Run tracking service
- `POST_NOTIFICATIONS` - Tracking notifications
- `INTERNET` - Strava sync

### Wear App
- `ACCESS_FINE_LOCATION` - GPS tracking
- `BODY_SENSORS` - Heart rate monitoring
- `ACTIVITY_RECOGNITION` - Exercise detection
- `FOREGROUND_SERVICE` - Run tracking service

## Training Plans

The app generates personalized training plans based on:
- **Goal Type**: First 5K, Improve 5K, 10K, Half Marathon, Marathon
- **Current Fitness Level**: Automatically assessed from recent runs
- **Available Days**: 3-6 days per week

Workout types include:
- Easy runs
- Long runs
- Tempo runs
- Interval training
- Hill repeats
- Recovery runs

## Gym Progression System

The smart progression engine analyzes your workout history and suggests:
- **Weight increases** when you consistently hit the top of your rep range
- **Rep increases** when you're in the middle of your target range
- **Deloads** when performance declines over 3+ sessions
- **Starting weights** based on estimated 1RM

Uses the Brzycki formula for 1RM estimation:
```
1RM = weight × (36 / (37 - reps))
```

## Future Enhancements

- [ ] Social features and challenges
- [ ] Cloud sync across devices
- [ ] Apple Watch companion app
- [ ] Nutrition tracking integration
- [ ] AI form analysis via camera

## License

MIT License
