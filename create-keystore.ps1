# PowerShell script to create Android release keystore
# This will create a keystore file for signing your release APK/AAB

Write-Host "Android Release Keystore Generator" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host ""

# Check if keytool is available
$keytoolPath = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytoolPath) {
    Write-Host "Error: keytool not found in PATH." -ForegroundColor Red
    Write-Host "Please ensure Java JDK is installed and in your PATH." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "You can find keytool at:" -ForegroundColor Yellow
    Write-Host "  C:\Program Files\Java\jdk-*\bin\keytool.exe" -ForegroundColor Gray
    Write-Host "  or" -ForegroundColor Gray
    Write-Host "  %JAVA_HOME%\bin\keytool.exe" -ForegroundColor Gray
    exit 1
}

# Default values
$keystoreName = "cell-signal-logger-release.jks"
$aliasName = "release"
$validityYears = 25  # Android requires at least 25 years for Play Store

Write-Host "This script will create a keystore for signing your release builds." -ForegroundColor Yellow
Write-Host ""
Write-Host "IMPORTANT:" -ForegroundColor Red
Write-Host "  - Keep your keystore file and passwords SECURE" -ForegroundColor Red
Write-Host "  - NEVER commit the keystore to git" -ForegroundColor Red
Write-Host "  - If you lose the keystore, you cannot update your app on Play Store" -ForegroundColor Red
Write-Host ""

# Check if keystore already exists
if (Test-Path $keystoreName) {
    Write-Host "Warning: $keystoreName already exists!" -ForegroundColor Yellow
    $overwrite = Read-Host "Do you want to overwrite it? (yes/no)"
    if ($overwrite -ne "yes") {
        Write-Host "Aborted." -ForegroundColor Yellow
        exit 0
    }
}

Write-Host "Keystore Configuration:" -ForegroundColor Cyan
Write-Host "  Keystore file: $keystoreName" -ForegroundColor Gray
Write-Host "  Key alias: $aliasName" -ForegroundColor Gray
Write-Host "  Validity: $validityYears years" -ForegroundColor Gray
Write-Host ""

# Get passwords
$keystorePassword = Read-Host "Enter keystore password" -AsSecureString
$keystorePasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($keystorePassword)
)

$keyPassword = Read-Host "Enter key password (or press Enter to use same as keystore)" -AsSecureString
$keyPasswordPlain = if ($keyPassword.Length -eq 0) {
    $keystorePasswordPlain
} else {
    [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPassword)
    )
}

Write-Host ""
Write-Host "Enter certificate information:" -ForegroundColor Cyan
$firstName = Read-Host "First and Last Name (e.g., Your Name or Company Name)"
$organizationalUnit = Read-Host "Organizational Unit (e.g., Development)"
$organization = Read-Host "Organization (e.g., Your Company)"
$city = Read-Host "City or Locality"
$state = Read-Host "State or Province"
$countryCode = Read-Host "Two-letter Country Code (e.g., US, GB)"

Write-Host ""
Write-Host "Creating keystore..." -ForegroundColor Cyan

# Create the keystore
$dname = "CN=$firstName, OU=$organizationalUnit, O=$organization, L=$city, ST=$state, C=$countryCode"

try {
    $keytoolArgs = @(
        "-genkey",
        "-v",
        "-keystore", $keystoreName,
        "-alias", $aliasName,
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", ($validityYears * 365),
        "-storetype", "JKS",
        "-dname", $dname,
        "-storepass", $keystorePasswordPlain,
        "-keypass", $keyPasswordPlain
    )

    & keytool $keytoolArgs

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✓ Keystore created successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Keystore Details:" -ForegroundColor Cyan
        Write-Host "  File: $keystoreName" -ForegroundColor Gray
        Write-Host "  Alias: $aliasName" -ForegroundColor Gray
        Write-Host "  Location: $(Resolve-Path $keystoreName)" -ForegroundColor Gray
        Write-Host ""
        Write-Host "NEXT STEPS:" -ForegroundColor Yellow
        Write-Host "1. Store your passwords securely (password manager recommended)" -ForegroundColor Yellow
        Write-Host "2. Backup the keystore file to a secure location" -ForegroundColor Yellow
        Write-Host "3. Update app/build.gradle.kts with keystore configuration" -ForegroundColor Yellow
        Write-Host "4. The keystore is already in .gitignore - DO NOT commit it!" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "To verify the keystore, run:" -ForegroundColor Cyan
        Write-Host "  keytool -list -v -keystore $keystoreName" -ForegroundColor Gray
    } else {
        Write-Host "Error: Failed to create keystore" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
} finally {
    # Clear passwords from memory
    $keystorePasswordPlain = $null
    $keyPasswordPlain = $null
}
