# Project Structure

## Complete File List

```
SignalDriveLogger/
├── app/
│   ├── build.gradle.kts              # App-level Gradle configuration
│   ├── proguard-rules.pro            # ProGuard rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # App manifest with permissions
│       │   ├── java/com/signaldrivelogger/
│       │   │   ├── SignalDriveLoggerApplication.kt
│       │   │   ├── data/
│       │   │   │   ├── FileLogger.kt           # CSV/GPX file operations
│       │   │   │   ├── LocationProvider.kt     # GPS location provider
│       │   │   │   ├── SignalRepository.kt     # Combines location + signal
│       │   │   │   └── TelephonyMonitor.kt     # Signal strength monitoring
│       │   │   ├── domain/models/
│       │   │   │   └── SignalRecord.kt         # Data model
│       │   │   ├── service/
│       │   │   │   └── LoggingService.kt       # Foreground service
│       │   │   └── ui/
│       │   │       ├── LoggingViewModel.kt     # ViewModel
│       │   │       ├── MainActivity.kt         # Main activity
│       │   │       ├── MainScreen.kt           # Main UI screen
│       │   │       ├── MapScreen.kt            # Map display screen
│       │   │       └── theme/
│       │   │           ├── Color.kt
│       │   │           ├── Theme.kt
│       │   │           └── Type.kt
│       │   └── res/
│       │       ├── drawable/
│       │       │   └── ic_notification.xml
│       │       ├── values/
│       │       │   ├── colors.xml
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml/
│       │           └── file_paths.xml          # FileProvider paths
│       └── test/java/com/signaldrivelogger/data/
│           └── FileLoggerTest.kt               # Unit tests
├── build.gradle.kts                  # Project-level Gradle
├── settings.gradle.kts               # Gradle settings
├── gradle.properties                # Gradle properties
├── .gitignore
├── README.md                         # Main documentation
├── SETUP.md                          # Setup instructions
└── PROJECT_STRUCTURE.md              # This file
```

## Key Components

### Data Layer
- **LocationProvider**: Handles GPS location updates using FusedLocationProviderClient
- **TelephonyMonitor**: Monitors signal strength, cell ID, and network type
- **SignalRepository**: Combines location and signal data into SignalRecord objects
- **FileLogger**: Handles CSV and GPX file operations

### Domain Layer
- **SignalRecord**: Core data model with location, signal strength, and metadata

### Service Layer
- **LoggingService**: Foreground service that runs in background, collects data, and logs to files

### UI Layer
- **MainScreen**: Controls for start/stop logging, save, export
- **MapScreen**: osmdroid (OpenStreetMap) display with color-coded route
- **LoggingViewModel**: Manages app state and coordinates data collection

## Data Flow

1. User taps "Start Logging"
2. MainActivity → ViewModel → LoggingService.startLogging()
3. LoggingService starts:
   - LocationProvider.getLocationUpdates() (Flow)
   - TelephonyMonitor.getSignalStrengthUpdates() (Flow)
4. When both location and signal data are available:
   - Create SignalRecord
   - Add to repository
   - Log to file (CSV + GPX)
5. UI observes repository.records Flow and updates map

## File Locations

- **Log files**: `Android/data/com.signaldrivelogger/files/signal_logs/`
- **CSV format**: `filename.csv`
- **GPX format**: `filename.gpx`

## Dependencies

Key dependencies (see `app/build.gradle.kts`):
- Jetpack Compose
- osmdroid (OpenStreetMap)
- Google Play Services Location
- Coroutines
- Navigation Compose
- Accompanist Permissions
