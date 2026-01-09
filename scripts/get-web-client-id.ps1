# Web Client ID Extractor
# This script helps you find the Web Client ID from google-services.json

$googleServicesPath = "e:\Ganesh Kulfi web\Ganesh Kulfi\android\KulfiDelightAndroid\app\google-services.json"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Web Client ID Extractor" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if file exists
if (-not (Test-Path $googleServicesPath)) {
    Write-Host "❌ ERROR: google-services.json not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Expected location:" -ForegroundColor Yellow
    Write-Host $googleServicesPath -ForegroundColor White
    Write-Host ""
    Write-Host "Please download google-services.json from Firebase Console" -ForegroundColor Yellow
    Write-Host "and place it in the app folder." -ForegroundColor Yellow
    exit
}

Write-Host "✅ Found google-services.json" -ForegroundColor Green
Write-Host ""

# Read and parse JSON
try {
    $json = Get-Content $googleServicesPath -Raw | ConvertFrom-Json
    
    # Extract Web Client ID
    $webClientId = $null
    
    foreach ($client in $json.client[0].oauth_client) {
        if ($client.client_type -eq 3) {
            $webClientId = $client.client_id
            break
        }
    }
    
    if ($webClientId) {
        Write-Host "🎉 Web Client ID Found!" -ForegroundColor Green
        Write-Host ""
        Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host $webClientId -ForegroundColor Yellow
        Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "📋 Copied to clipboard!" -ForegroundColor Green
        
        # Copy to clipboard
        $webClientId | Set-Clipboard
        
        Write-Host ""
        Write-Host "Next Steps:" -ForegroundColor Cyan
        Write-Host "1. Open: GoogleSignInHelper.kt" -ForegroundColor White
        Write-Host "2. Find line: private val webClientId = ..." -ForegroundColor White
        Write-Host "3. Replace with: private val webClientId = `"$webClientId`"" -ForegroundColor White
        Write-Host ""
        
    } else {
        Write-Host "⚠️  Could not find Web Client ID in google-services.json" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "This might mean:" -ForegroundColor Yellow
        Write-Host "1. You need to add a Web app in Firebase Console" -ForegroundColor White
        Write-Host "2. You need to re-download google-services.json" -ForegroundColor White
        Write-Host ""
        Write-Host "How to fix:" -ForegroundColor Cyan
        Write-Host "1. Go to Firebase Console → Project Settings" -ForegroundColor White
        Write-Host "2. Scroll to 'Your apps'" -ForegroundColor White
        Write-Host "3. Click 'Add app' → Select Web (</>) icon" -ForegroundColor White
        Write-Host "4. Register the web app" -ForegroundColor White
        Write-Host "5. Download new google-services.json" -ForegroundColor White
        Write-Host "6. Run this script again" -ForegroundColor White
    }
    
} catch {
    Write-Host "❌ Error reading google-services.json" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
