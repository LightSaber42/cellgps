# Play Store Publication Checklist

## ✅ Already Complete

1. **App Configuration**
   - ✅ Version code: 1
   - ✅ Version name: "1.0"
   - ✅ Target SDK: 34 (Android 14)
   - ✅ Min SDK: 26 (Android 8.0)
   - ✅ App name: "Cell Signal Logger"
   - ✅ Application ID: com.signaldrivelogger
   - ✅ App icons: Launcher icons configured

2. **Technical Requirements**
   - ✅ ProGuard rules configured
   - ✅ Permissions properly declared
   - ✅ Foreground service properly configured
   - ✅ FileProvider configured for file sharing
   - ✅ Intent filters for CSV file sharing

## ❌ Required Before Publishing

### 1. **App Signing (CRITICAL)**
   - [ ] Create release keystore
   - [ ] Configure signing in `build.gradle.kts`
   - [ ] Store keystore securely (NEVER commit to git)
   - [ ] Consider enabling "App signing by Google Play" (recommended)

**Action Required:**
```kotlin
// Add to app/build.gradle.kts:
android {
    signingConfigs {
        create("release") {
            storeFile = file("path/to/your-keystore.jks")
            storePassword = "your-store-password"
            keyAlias = "your-key-alias"
            keyPassword = "your-key-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true  // Enable for release
            isShrinkResources = true  // Enable resource shrinking
        }
    }
}
```

### 2. **Privacy Policy (REQUIRED)**
   - [ ] Create privacy policy document
   - [ ] Host it online (GitHub Pages, your website, etc.)
   - [ ] Add privacy policy URL to Play Console
   - [ ] Link in app (optional but recommended)

**Required Content:**
- What data is collected (location, signal strength, cell ID)
- How data is used (stored locally, not transmitted)
- Data storage (local device only)
- User rights (can delete data)

### 3. **Data Safety Form (REQUIRED)**
   - [ ] Complete Data Safety section in Play Console
   - [ ] Declare location data collection
   - [ ] Declare device/phone identifiers (cell ID)
   - [ ] State that data is stored locally only
   - [ ] State that data is not shared with third parties

### 4. **Store Listing Assets**
   - [ ] App icon (512x512 PNG) - ✅ Already have
   - [ ] Feature graphic (1024x500 PNG)
   - [ ] Screenshots (at least 2, up to 8):
     - Phone: 16:9 or 9:16, min 320px, max 3840px
     - Tablet (optional): 16:9 or 9:16
   - [ ] App description (4000 chars max)
   - [ ] Short description (80 chars max)
   - [ ] Promotional text (optional, 80 chars)

### 5. **Content Rating**
   - [ ] Complete content rating questionnaire
   - [ ] Answer questions about app content
   - [ ] Get rating certificate

### 6. **Release Build**
   - [ ] Test release build thoroughly
   - [ ] Enable ProGuard/R8 minification
   - [ ] Test on multiple devices
   - [ ] Verify all features work in release mode

### 7. **Testing**
   - [ ] Test on Android 8.0+ devices
   - [ ] Test location permissions flow
   - [ ] Test background service
   - [ ] Test CSV import/export
   - [ ] Test map display
   - [ ] Test multi-SIM functionality

## 📋 Recommended Improvements

### 1. **User Experience**
   - [ ] Add onboarding/tutorial for first-time users
   - [ ] Add help/about screen
   - [ ] Improve error messages
   - [ ] Add data export confirmation

### 2. **Performance**
   - [ ] Enable R8 code shrinking
   - [ ] Enable resource shrinking
   - [ ] Optimize map rendering for large datasets
   - [ ] Add loading indicators

### 3. **Accessibility**
   - [ ] Add content descriptions to all interactive elements
   - [ ] Test with TalkBack
   - [ ] Ensure sufficient color contrast

### 4. **Localization** (Optional)
   - [ ] Consider translating to other languages
   - [ ] Add string resources for all text

## 🚀 Publishing Steps

1. **Prepare Release Build**
   ```powershell
   .\gradlew.bat assembleRelease
   ```

2. **Create App in Play Console**
   - Go to Google Play Console
   - Create new app
   - Fill in app details

3. **Upload APK/AAB**
   - Upload release AAB (recommended) or APK
   - Complete store listing
   - Upload screenshots and graphics

4. **Complete Required Forms**
   - Data Safety form
   - Content rating
   - Privacy policy URL

5. **Test Track (Recommended)**
   - Create internal test track
   - Test with internal testers
   - Create closed/open beta (optional)

6. **Submit for Review**
   - Review all information
   - Submit for production release
   - Wait for review (typically 1-7 days)

## ⚠️ Important Notes

1. **Location Data**: This app collects precise location data. You MUST:
   - Declare this in Data Safety form
   - Provide privacy policy explaining usage
   - Ensure users understand what data is collected

2. **Background Location**: Foreground service with location requires:
   - Clear explanation to users
   - Persistent notification (already implemented)
   - Proper permission handling (already implemented)

3. **Phone State Permission**: READ_PHONE_STATE requires:
   - Explanation of why it's needed (signal strength monitoring)
   - Declaration in Data Safety form

4. **Keystore Security**:
   - NEVER commit keystore to git
   - Store keystore password securely
   - Consider using environment variables or secure storage
   - Enable "App signing by Google Play" to let Google manage signing

## 📝 Sample Privacy Policy Template

You'll need to create a privacy policy. Here's what to include:

```
# Privacy Policy for Cell Signal Logger

## Data Collection
This app collects the following data:
- GPS location (latitude, longitude, altitude)
- Mobile signal strength
- Cell tower ID
- Network type and connection information
- Device information (SIM card details)

## Data Usage
All data is stored locally on your device only. No data is transmitted to external servers or third parties.

## Data Storage
Data is stored in CSV/GPX files on your device at:
Android/data/com.signaldrivelogger/files/signal_logs/

## Data Deletion
You can delete all logged data at any time using the "Clear Records" button in the app.

## Permissions
- Location: Required to log GPS coordinates with signal measurements
- Phone State: Required to read signal strength and cell information
- Notifications: Required for background service notification

## Contact
[Your contact information]
```

## 🎯 Quick Start Checklist

Before publishing, ensure you have:
1. ✅ Release keystore created and configured
2. ✅ Privacy policy created and hosted online
3. ✅ Release build tested and working
4. ✅ Screenshots prepared (at least 2)
5. ✅ Feature graphic created
6. ✅ App description written
7. ✅ Data Safety form completed
8. ✅ Content rating obtained
