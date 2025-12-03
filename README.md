# SignalDriveLogger

A production-ready Android app that logs mobile signal strength while driving, displays the route on a map colored by signal strength, and runs in the background.

## Features

- **Background Signal Logging**: Continuously logs signal strength, cell ID, network type, and data rate
- **GPS Location Tracking**: Captures location every 1-5 seconds
- **File Logging**: Exports data to CSV or GPX format
- **Interactive Map**: Displays route with color-coded signal strength (green = strong, red = weak)
- **Foreground Service**: Runs in background with persistent notification
- **Real-time Display**: Shows current signal strength and network type

## Setup

### 1. No API Key Required!

This app uses **osmdroid** with **OpenStreetMap** tiles, so no API key is needed. Just build and run!

### 2. Build the Project

**On Windows (PowerShell):**
```powershell
.\gradlew.bat build
```

**On Linux/Mac:**
```bash
./gradlew build
```

### 3. Install on Device

**On Windows (PowerShell):**
```powershell
.\gradlew.bat installDebug
```

**On Linux/Mac:**
```bash
./gradlew installDebug
```

## Permissions

The app requires the following permissions:
- `ACCESS_FINE_LOCATION` - For GPS tracking
- `ACCESS_COARSE_LOCATION` - For approximate location
- `READ_PHONE_STATE` - For signal strength monitoring
- `FOREGROUND_SERVICE` - For background logging
- `FOREGROUND_SERVICE_LOCATION` - For location-based foreground service
- `POST_NOTIFICATIONS` - For service notification (Android 13+)

## Usage

1. **Start Logging**: Tap "Start Logging" to begin recording signal data
2. **View Map**: Tap "View Map" to see your route colored by signal strength
3. **Save File**: Choose CSV or GPX format and save your log
4. **Export/Share**: Share the log file with other apps

## File Formats

### CSV Format
```
timestamp,latitude,longitude,signal_strength_dbm,cell_id,data_rate_kbps,network_type,asu
1699123456789,37.7749,-122.4194,-85,12345,20000,LTE,0
```

### GPX Format
Standard GPX with custom extensions for signal data:
```xml
<trkpt lat="37.7749" lon="-122.4194">
    <time>2023-11-04T12:34:56Z</time>
    <extensions>
        <signal:strength>-85</signal:strength>
        <signal:cellId>12345</signal:cellId>
        <signal:dataRate>20000</signal:dataRate>
        <signal:networkType>LTE</signal:networkType>
    </extensions>
</trkpt>
```

## Architecture

```
/app
  /data
    - SignalRepository.kt      # Combines location and signal data
    - FileLogger.kt            # Handles CSV/GPX file operations
    - LocationProvider.kt       # GPS location provider
    - TelephonyMonitor.kt      # Signal strength monitoring
  /domain
    /models
      - SignalRecord.kt        # Data model
  /ui
    - MainScreen.kt            # Main UI with controls
    - MapScreen.kt             # Map display with colored route
    - LoggingViewModel.kt      # ViewModel for state management
    - MainActivity.kt          # Main activity
  /service
    - LoggingService.kt        # Foreground service
```

## Technical Details

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Maps**: osmdroid (OpenStreetMap)
- **Location**: FusedLocationProviderClient
- **Signal**: TelephonyManager with SignalStrengthCallback

## Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## Notes

- Signal strength is measured in dBm (RSRP for LTE/5G)
- Typical range: -140 dBm (weak) to -50 dBm (strong)
- Data rate is estimated based on network type and signal strength
- Files are saved to `Android/data/com.signaldrivelogger/files/signal_logs/`

## License

This project is provided as-is for educational and development purposes.
