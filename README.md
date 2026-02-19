# App Tracker - Digital Wellbeing Monitor

An Android application that monitors app usage patterns, detects addictive behavior, and helps users manage their screen time through smart alerts and detailed analytics.

## Features

### Real-Time Usage Tracking
- Continuous foreground app monitoring via a foreground service
- Tracks daily usage time, open counts, and session durations
- Persists across device reboots with auto-start

### Smart Alerts
- Configurable per-app time limits (1-60 minutes)
- Fullscreen overlay warnings when limits are exceeded
- "Add Time" option with escalating increments (5 min, 10 min, 15 min...)
- Auto-dismiss after 60 seconds as a safety net

### Addiction Detection
Analyzes 7-day usage patterns and flags apps as "addictive" based on four criteria:
- **High daily usage**: >1 hour per day
- **Frequent opens**: >20 times per day
- **Compulsive checking**: >10 short sessions (<2 min) per day
- **Late-night usage**: >30 minutes between 11 PM - 6 AM

An app is marked addictive if it meets 2 or more criteria (score 0-4).
Note- User selected app as "Work" won't be marked as Addictive

### Dashboard
- Total screen time for the day
- Per-app usage breakdown with icons
- Real-time tracking status (ON/OFF)
- Quick permission management

### App Settings
- Searchable list of all installed apps
- Per-app time limit configuration with sliders
- App exclusion toggles (with 3-step confirmation for addictive apps)
- Visual indicators for addictive apps (red highlighting, warning icons)

### Analytics
- Weekly usage summary with daily averages
- Visual bar chart showing 7-day usage trends
- Addictive apps section with addiction scores
- Top 10 most-used apps ranking

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM (Model-View-ViewModel) |
| Dependency Injection | Hilt (Dagger) |
| Database | Room (SQLite) |
| Async | Kotlin Coroutines + StateFlow |
| Image Loading | Coil |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

## Project Structure

```
com.alertsystem.apptracker/
├── data/
│   ├── local/          # Room database, entities, DAOs
│   ├── model/          # Data transfer objects
│   └── repository/     # Repository implementation
├── di/                 # Hilt dependency injection modules
├── domain/
│   └── repository/     # Repository interface
├── receiver/           # Boot receiver, notification actions
├── service/            # Tracking service, overlay manager, notifications
├── ui/
│   ├── alert/          # Usage alert activity
│   ├── navigation/     # Bottom navigation
│   ├── screens/
│   │   ├── analytics/  # 7-day analytics screen
│   │   ├── dashboard/  # Home screen
│   │   └── settings/   # App settings screen
│   └── theme/          # Material 3 theming
└── util/               # Addiction detector, constants
```

## Permissions

| Permission | Purpose |
|------------|---------|
| `PACKAGE_USAGE_STATS` | Read app usage data |
| `POST_NOTIFICATIONS` | Show usage alerts |
| `SYSTEM_ALERT_WINDOW` | Display overlay warnings |
| `FOREGROUND_SERVICE` | Run background tracking |
| `RECEIVE_BOOT_COMPLETED` | Auto-start on device boot |
| `QUERY_ALL_PACKAGES` | List installed apps |

## Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on a device or emulator (API 26+)
5. Grant usage access permission when prompted

## Data Models

- **AppSettingsEntity** - Per-app time limits, exclusions, notification settings
- **DailyUsageEntity** - Aggregated daily stats (usage time, open count, short sessions, late-night usage)
- **UsageSessionEntity** - Individual session records with start/end timestamps

Data is retained for 30 days and automatically cleaned up.

## How It Works

1. A **foreground service** polls `UsageStatsManager` every 5 seconds to detect the active app
2. When the app changes, the previous session is saved to the **Room database**
3. Daily usage metrics are aggregated in real-time
4. When a time limit is exceeded, a **fullscreen overlay alert** is shown with options to ignore or add time
5. The **addiction detector** runs on a 7-day rolling window to score apps
6. Addictive apps trigger special warnings with multi-step confirmation for exclusion


## How To Run

1. Clone the repository in your computer
2. Open in Android Studio
3. Enable Developer mode on your phone
4. Connect your android to the computer (USB/Wireless debugging)
5. Click the "Run" button in Android Studio
