# Android Studio Environment Setup Script for PowerShell
# Run this script to set up Android development environment in your current PowerShell session

Write-Host "Setting up Android Studio environment..." -ForegroundColor Green

# Set Android SDK path (default location)
$androidSdkPath = "$env:LOCALAPPDATA\Android\Sdk"

if (-not (Test-Path $androidSdkPath)) {
    Write-Host "Warning: Android SDK not found at: $androidSdkPath" -ForegroundColor Yellow
    Write-Host "Please install Android SDK or update the path in this script." -ForegroundColor Yellow
    exit 1
}

# Set environment variables for current session
$env:ANDROID_HOME = $androidSdkPath
$env:ANDROID_SDK_ROOT = $androidSdkPath

# Add Android SDK tools to PATH
$platformTools = "$androidSdkPath\platform-tools"
$tools = "$androidSdkPath\tools"
$toolsBin = "$androidSdkPath\tools\bin"

if (Test-Path $platformTools) {
    $env:PATH = "$platformTools;$env:PATH"
    Write-Host "Added platform-tools to PATH" -ForegroundColor Green
}

if (Test-Path $tools) {
    $env:PATH = "$tools;$env:PATH"
    Write-Host "Added tools to PATH" -ForegroundColor Green
}

if (Test-Path $toolsBin) {
    $env:PATH = "$toolsBin;$env:PATH"
    Write-Host "Added tools\bin to PATH" -ForegroundColor Green
}

# Create local.properties if it doesn't exist
$localPropertiesPath = "local.properties"
if (-not (Test-Path $localPropertiesPath)) {
    $sdkDir = $androidSdkPath -replace '\\', '\\'
    $content = @"
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
# For customization when using a Version Control System, please read the
# header note.
sdk.dir=$sdkDir
"@
    $content | Out-File -FilePath $localPropertiesPath -Encoding UTF8
    Write-Host "Created local.properties file" -ForegroundColor Green
} else {
    Write-Host "local.properties already exists" -ForegroundColor Yellow
}

# Verify setup
Write-Host "`nEnvironment Setup Complete!" -ForegroundColor Green
Write-Host "ANDROID_HOME: $env:ANDROID_HOME" -ForegroundColor Cyan
Write-Host "ANDROID_SDK_ROOT: $env:ANDROID_SDK_ROOT" -ForegroundColor Cyan
Write-Host "`nYou can now run:" -ForegroundColor Yellow
Write-Host "  .\gradlew.bat build" -ForegroundColor White
Write-Host "  .\gradlew.bat installDebug" -ForegroundColor White

