# ADB Screenshot Script
# Takes a screenshot from connected Android device and saves to a dedicated folder

# Create screenshots folder if it doesn't exist
$screenshotFolder = "screenshots"
if (-not (Test-Path $screenshotFolder)) {
    New-Item -ItemType Directory -Path $screenshotFolder | Out-Null
    Write-Host "Created screenshots folder: $screenshotFolder" -ForegroundColor Green
}

# Generate filename with timestamp
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$filename = "$screenshotFolder\screenshot-$timestamp.png"
$fullPath = Join-Path (Get-Location) $filename

# Check if ADB is available
$adbCheck = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adbCheck) {
    Write-Host "Error: ADB not found in PATH. Please ensure Android SDK platform-tools is in your PATH." -ForegroundColor Red
    exit 1
}

# Check if device is connected and get first device ID
$deviceList = adb devices
$deviceLines = $deviceList | Select-String -Pattern "\tdevice$"
if (-not $deviceLines) {
    Write-Host "Error: No Android device connected. Please connect a device and enable USB debugging." -ForegroundColor Red
    exit 1
}

# Get the first device ID (split by tab and take first part)
$firstDevice = ($deviceLines[0].Line -split "`t")[0]
Write-Host "Using device: $firstDevice" -ForegroundColor Cyan

Write-Host "Taking screenshot..." -ForegroundColor Cyan

# Take screenshot and save to file
try {
    # Capture binary output using .NET methods
    $processInfo = New-Object System.Diagnostics.ProcessStartInfo
    $processInfo.FileName = "adb"
    $processInfo.Arguments = "-s $firstDevice exec-out screencap -p"
    $processInfo.UseShellExecute = $false
    $processInfo.RedirectStandardOutput = $true
    $processInfo.CreateNoWindow = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $processInfo
    $process.Start() | Out-Null

    # Read binary data
    $binaryData = $process.StandardOutput.BaseStream
    $memoryStream = New-Object System.IO.MemoryStream
    $binaryData.CopyTo($memoryStream)
    $bytes = $memoryStream.ToArray()

    $process.WaitForExit()

    if ($process.ExitCode -ne 0) {
        throw "ADB command failed with exit code $($process.ExitCode)"
    }

    # Write binary data to file
    [System.IO.File]::WriteAllBytes($fullPath, $bytes)
    Write-Host "Screenshot saved: $fullPath" -ForegroundColor Green

    # Get file size
    $fileSize = (Get-Item $fullPath).Length / 1KB
    Write-Host "File size: $([math]::Round($fileSize, 2)) KB" -ForegroundColor Gray

    # Optionally open the screenshot
    $open = Read-Host "Open screenshot? (Y/N)"
    if ($open -eq "Y" -or $open -eq "y") {
        Start-Process $fullPath
    }
} catch {
    Write-Host "Error taking screenshot: $_" -ForegroundColor Red
    exit 1
}
