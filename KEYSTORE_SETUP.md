# Keystore Setup Guide

This guide will help you create and configure a keystore for signing your Android release builds.

## Quick Start

1. **Run the keystore creation script:**
   ```powershell
   .\create-keystore.ps1
   ```

2. **Follow the prompts:**
   - Enter a strong keystore password (save it securely!)
   - Enter key password (or use same as keystore)
   - Enter certificate information (name, organization, etc.)

3. **Create keystore.properties file:**
   ```powershell
   Copy-Item keystore.properties.example keystore.properties
   ```
   Then edit `keystore.properties` and fill in your actual passwords.

4. **Build release APK:**
   ```powershell
   .\gradlew.bat assembleRelease
   ```

## Manual Keystore Creation

If you prefer to create the keystore manually:

```powershell
keytool -genkey -v -keystore cell-signal-logger-release.jks -alias release -keyalg RSA -keysize 2048 -validity 9125 -storetype JKS
```

**Parameters:**
- `-keystore`: Keystore filename
- `-alias`: Key alias name
- `-keyalg`: RSA algorithm
- `-keysize`: 2048 bits (recommended)
- `-validity`: 9125 days (25 years - required for Play Store)
- `-storetype`: JKS format

## Keystore Properties File

The `keystore.properties` file should contain:

```properties
storeFile=cell-signal-logger-release.jks
storePassword=Terapieea24!
keyAlias=release
keyPassword=Terapieea24!
```

**IMPORTANT:**
- This file is in `.gitignore` - DO NOT commit it!
- Store passwords in a secure password manager
- Keep a backup of both the keystore file and passwords

## Verifying Your Keystore

To verify the keystore was created correctly:

```powershell
keytool -list -v -keystore cell-signal-logger-release.jks
```

You'll be prompted for the keystore password, then you'll see certificate details.

## Security Best Practices

1. **Backup your keystore:**
   - Store in multiple secure locations
   - Use encrypted storage
   - Consider cloud backup (encrypted)

2. **Store passwords securely:**
   - Use a password manager
   - Never store in plain text files
   - Share securely with team members if needed

3. **Never commit to git:**
   - Keystore files are in `.gitignore`
   - `keystore.properties` is in `.gitignore`
   - Double-check before committing

4. **If you lose the keystore:**
   - You CANNOT update your app on Play Store
   - You'll need to create a new app listing
   - This is why backups are critical!

## Building Release APK/AAB

### APK (for direct distribution):
```powershell
.\gradlew.bat assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### AAB (for Play Store - recommended):
```powershell
.\gradlew.bat bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

## Troubleshooting

### "keytool not found"
- Ensure Java JDK is installed
- Add Java bin directory to PATH
- Or use full path: `C:\Program Files\Java\jdk-*\bin\keytool.exe`

### "Keystore was tampered with, or password was incorrect"
- Double-check your password
- Ensure you're using the correct keystore file

### "Signing config not found"
- Ensure `keystore.properties` exists
- Check that all properties are filled in correctly
- Verify keystore file path is correct

## Play Store App Signing

Google Play offers "App signing by Google Play" which:
- Lets Google manage your app signing key
- Reduces risk of losing your key
- Still requires an upload key (which you create)

You can enable this in Play Console after uploading your first app.

## Next Steps

After creating your keystore:
1. ✅ Test release build: `.\gradlew.bat assembleRelease`
2. ✅ Install and test on device
3. ✅ Create privacy policy
4. ✅ Prepare Play Store listing
5. ✅ Upload to Play Console
