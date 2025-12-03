# Setup Guide for Cell Signal Logger

## Prerequisites

1. **Android Studio** (Hedgehog or later recommended)
2. **JDK 17** or later
3. **Android SDK** with API 26+ and build tools
4. **No API key required!** This app uses osmdroid with OpenStreetMap

## Step 1: No Setup Required!

This app uses **osmdroid** with **OpenStreetMap** tiles, so no API key configuration is needed. The maps work out of the box!

## Step 2: Build and Run

### Using Android Studio:
1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Connect an Android device or start an emulator
4. Click **Run** (green play button)

### Using Command Line:

**On Windows (PowerShell):**
```powershell
# Build the project
.\gradlew.bat build

# Install on connected device
.\gradlew.bat installDebug

# Run tests
.\gradlew.bat test
```

**On Linux/Mac:**
```bash
# Build the project
./gradlew build

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## Step 3: Grant Permissions

On first launch, the app will request:
- **Location** (Fine and Coarse)
- **Phone State** (for signal strength)
- **Notifications** (Android 13+)

Grant all permissions to use the app.

## Troubleshooting

### Maps Not Showing
- Ensure device has internet connection (needed to download map tiles)
- Check that INTERNET permission is granted
- osmdroid uses OpenStreetMap tiles which are free and don't require an API key

### No Signal Data
- Check that READ_PHONE_STATE permission is granted
- Verify device has cellular connection (not just WiFi)
- Some emulators may not provide signal data

### Location Not Working
- Ensure location permission is granted
- Check device location services are enabled
- Try testing outdoors for better GPS accuracy

### Build Errors
- Ensure you're using JDK 17+
- Sync Gradle files: **File** → **Sync Project with Gradle Files**
- Clean and rebuild: **Build** → **Clean Project**, then **Build** → **Rebuild Project**

## Testing on Emulator

For testing signal data on an emulator:
1. Use Android Studio's extended controls
2. Go to **Cellular** tab
3. Set signal strength and network type manually
4. Note: Real signal data requires a physical device

## Next Steps

- Review the code structure in `README.md`
- Customize update intervals in `LocationProvider.kt` and `TelephonyMonitor.kt`
- Adjust signal strength color mapping in `MapScreen.kt`
- Add additional export formats if needed
