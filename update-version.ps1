# PowerShell script to update app version for Play Store uploads
# Increments version code and optionally updates version name

param(
    [string]$VersionName = "",
    [switch]$BuildBundle = $false,
    [switch]$AutoIncrement = $true
)

$buildGradlePath = "app\build.gradle.kts"

if (-not (Test-Path $buildGradlePath)) {
    Write-Host "Error: build.gradle.kts not found at $buildGradlePath" -ForegroundColor Red
    exit 1
}

Write-Host "Updating app version for Play Store..." -ForegroundColor Cyan
Write-Host ""

# Read current build.gradle.kts
$content = Get-Content $buildGradlePath -Raw

# Extract current version code
if ($content -match 'versionCode\s*=\s*(\d+)') {
    $currentVersionCode = [int]$matches[1]
    Write-Host "Current version code: $currentVersionCode" -ForegroundColor Gray
} else {
    Write-Host "Error: Could not find versionCode in build.gradle.kts" -ForegroundColor Red
    exit 1
}

# Extract current version name
if ($content -match 'versionName\s*=\s*"([^"]+)"') {
    $currentVersionName = $matches[1]
    Write-Host "Current version name: $currentVersionName" -ForegroundColor Gray
} else {
    Write-Host "Error: Could not find versionName in build.gradle.kts" -ForegroundColor Red
    exit 1
}

# Calculate new version code
$newVersionCode = if ($AutoIncrement) {
    $currentVersionCode + 1
} else {
    $currentVersionCode
}

# Determine new version name
$newVersionName = if ($VersionName -ne "") {
    $VersionName
} else {
    # Auto-increment version name (e.g., "1.0" -> "1.1", "1.1" -> "1.2")
    if ($currentVersionName -match '^(\d+)\.(\d+)(?:\.(\d+))?$') {
        $major = [int]$matches[1]
        $minor = [int]$matches[2]
        $patch = if ($matches[3]) { [int]$matches[3] } else { 0 }

        # Increment patch version, or minor if patch is 9+
        if ($patch -lt 9) {
            $patch++
        } else {
            $minor++
            $patch = 0
        }

        "$major.$minor.$patch"
    } else {
        # If version format doesn't match, just increment patch
        "$currentVersionName.1"
    }
}

Write-Host ""
Write-Host "New version:" -ForegroundColor Cyan
Write-Host "  Version Code: $newVersionCode (was $currentVersionCode)" -ForegroundColor Green
Write-Host "  Version Name: $newVersionName (was $currentVersionName)" -ForegroundColor Green
Write-Host ""

# Confirm with user
$confirm = Read-Host "Update version? (Y/N)"
if ($confirm -ne "Y" -and $confirm -ne "y") {
    Write-Host "Cancelled." -ForegroundColor Yellow
    exit 0
}

# Update version code
$content = $content -replace "versionCode\s*=\s*\d+", "versionCode = $newVersionCode"

# Update version name
$content = $content -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$newVersionName`""

# Write back to file
$content | Set-Content $buildGradlePath -NoNewline

Write-Host "✓ Version updated in build.gradle.kts" -ForegroundColor Green
Write-Host ""

# Build bundle if requested
if ($BuildBundle) {
    Write-Host "Building release bundle..." -ForegroundColor Cyan
    .\gradlew.bat bundleRelease --no-daemon 2>&1 | Out-Null

    if ($LASTEXITCODE -eq 0) {
        $bundlePath = "app\build\outputs\bundle\release\app-release.aab"
        if (Test-Path $bundlePath) {
            $fileInfo = Get-Item $bundlePath
            Write-Host ""
            Write-Host "✓ Bundle built successfully!" -ForegroundColor Green
            Write-Host "  Location: $(Resolve-Path $bundlePath)" -ForegroundColor Gray
            Write-Host "  Size: $([math]::Round($fileInfo.Length / 1MB, 2)) MB" -ForegroundColor Gray
            Write-Host ""
            Write-Host "Ready to upload to Play Store!" -ForegroundColor Green
        } else {
            Write-Host "Warning: Bundle file not found" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Error: Build failed" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "To build the bundle, run:" -ForegroundColor Yellow
    Write-Host "  .\gradlew.bat bundleRelease" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  Version Code: $newVersionCode" -ForegroundColor White
Write-Host "  Version Name: $newVersionName" -ForegroundColor White
Write-Host ""
