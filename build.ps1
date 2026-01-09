# Kulfi Delight Android - Quick Build Script (PowerShell)

Write-Host @"

========================================
  KULFI DELIGHT - ANDROID BUILD
========================================

"@ -ForegroundColor Cyan

# Check if running from correct directory
if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "ERROR: Please run this script from the KulfiDelightAndroid directory" -ForegroundColor Red
    Write-Host "Current directory: $PWD" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Check Java
Write-Host "[1/5] Checking Java installation..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✓ Java is installed: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Java is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install JDK 17+ from: https://adoptium.net/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Check Firebase
Write-Host "`n[2/5] Checking Firebase configuration..." -ForegroundColor Yellow
if (Test-Path "app\google-services.json") {
    Write-Host "✓ Firebase configuration found" -ForegroundColor Green
} else {
    Write-Host "⚠ WARNING: google-services.json not found" -ForegroundColor Yellow
    Write-Host "Please configure Firebase before building" -ForegroundColor Yellow
    Write-Host "See BUILD_GUIDE.md for instructions" -ForegroundColor Cyan
}

# Build menu
Write-Host "`n[3/5] Select build type:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  1. Debug Build (For Testing)" -ForegroundColor White
Write-Host "  2. Release Build (For Distribution)" -ForegroundColor White
Write-Host "  3. Build and Install on Device" -ForegroundColor White
Write-Host "  4. Clean Project" -ForegroundColor White
Write-Host "  5. Check Dependencies" -ForegroundColor White
Write-Host "  6. Exit" -ForegroundColor White
Write-Host ""

$choice = Read-Host "Enter your choice (1-6)"

switch ($choice) {
    "1" {
        Write-Host "`n[4/5] Building Debug APK..." -ForegroundColor Cyan
        Write-Host "This may take 3-5 minutes on first build..." -ForegroundColor Yellow
        Write-Host ""
        
        & .\gradlew.bat assembleDebug
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n[5/5] ✓ BUILD SUCCESSFUL!" -ForegroundColor Green
            Write-Host "`nAPK Location: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
            Write-Host "`nYou can now:" -ForegroundColor Yellow
            Write-Host "  - Install: adb install app\build\outputs\apk\debug\app-debug.apk"
            Write-Host "  - Share the APK file for testing"
        } else {
            Write-Host "`n✗ BUILD FAILED!" -ForegroundColor Red
            Write-Host "Check error messages above" -ForegroundColor Yellow
        }
    }
    
    "2" {
        Write-Host "`n[4/5] Building Release APK..." -ForegroundColor Cyan
        Write-Host "NOTE: Release builds require a signing keystore" -ForegroundColor Yellow
        Write-Host ""
        
        & .\gradlew.bat assembleRelease
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n[5/5] ✓ BUILD SUCCESSFUL!" -ForegroundColor Green
            Write-Host "`nAPK Location: app\build\outputs\apk\release\app-release.apk" -ForegroundColor Cyan
        } else {
            Write-Host "`n✗ BUILD FAILED!" -ForegroundColor Red
            Write-Host "`nCommon issues:" -ForegroundColor Yellow
            Write-Host "  - Missing signing keystore"
            Write-Host "  - Check BUILD_GUIDE.md for keystore creation"
        }
    }
    
    "3" {
        Write-Host "`n[4/5] Building and Installing..." -ForegroundColor Cyan
        Write-Host "Make sure your device is connected via USB" -ForegroundColor Yellow
        Write-Host "USB debugging must be enabled" -ForegroundColor Yellow
        Write-Host ""
        
        & .\gradlew.bat installDebug
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n[5/5] ✓ INSTALL SUCCESSFUL!" -ForegroundColor Green
            Write-Host "`nThe app is now installed on your device" -ForegroundColor Cyan
            Write-Host "Look for 'Kulfi Delight' icon" -ForegroundColor Yellow
        } else {
            Write-Host "`n✗ INSTALL FAILED!" -ForegroundColor Red
            Write-Host "`nTry: adb devices" -ForegroundColor Yellow
        }
    }
    
    "4" {
        Write-Host "`n[4/5] Cleaning project..." -ForegroundColor Cyan
        & .\gradlew.bat clean
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n[5/5] ✓ CLEAN SUCCESSFUL!" -ForegroundColor Green
        }
    }
    
    "5" {
        Write-Host "`n[4/5] Checking dependencies..." -ForegroundColor Cyan
        & .\gradlew.bat dependencies --configuration debugRuntimeClasspath
    }
    
    "6" {
        Write-Host "`nExiting..." -ForegroundColor Yellow
        exit 0
    }
    
    default {
        Write-Host "`n✗ Invalid choice" -ForegroundColor Red
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "For more options, see BUILD_GUIDE.md" -ForegroundColor Yellow
Write-Host "========================================`n" -ForegroundColor Cyan

Read-Host "Press Enter to exit"
