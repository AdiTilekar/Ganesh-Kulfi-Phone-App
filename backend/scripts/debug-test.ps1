# Debug Test Script
$baseUrl = "http://localhost:8080"

Write-Host "=== Registering Admin ===" -ForegroundColor Cyan
$adminBody = @{
    email = "debugadmin$(Get-Random)@test.com"
    password = "Admin@123"
    name = "Debug Admin"
    phone = "9999999999"
    role = "ADMIN"
} | ConvertTo-Json
Write-Host "Body: $adminBody"

try {
    $adminResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $adminBody -ContentType "application/json"
    Write-Host "✅ Admin registered" -ForegroundColor Green
    $adminToken = $adminResponse.data.token
    Write-Host "Token: $($adminToken.Substring(0,30))..."
} catch {
    Write-Host "❌ Failed: $($_.Exception.Message)" -ForegroundColor Red
    $_.Exception | Format-List -Force
    exit
}

Write-Host "`n=== Testing Analytics ===" -ForegroundColor Cyan
try {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $analytics = Invoke-RestMethod -Uri "$baseUrl/api/admin/analytics/dashboard" -Method Get -Headers $headers -ErrorAction Stop
    Write-Host "✅ Analytics success" -ForegroundColor Green
    $analytics | ConvertTo-Json -Depth 5
} catch {
    Write-Host "❌ Analytics failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Error details:" -ForegroundColor Red
    $_.Exception | Format-List -Force
    if ($_.ErrorDetails) {
        Write-Host "Response body:" -ForegroundColor Red
        $_.ErrorDetails.Message
    }
}

Write-Host "`n=== Creating Order ===" -ForegroundColor Cyan
$retailerBody = @{
    email = "debugretailer$(Get-Random)@test.com"
    password = "Retailer@123"
    name = "Debug Retailer"
    phone = "8888888888"
    role = "RETAILER"
    shopName = "Debug Shop"
} | ConvertTo-Json

try {
    $retailerResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $retailerBody -ContentType "application/json"
    $retailerToken = $retailerResponse.data.token
    Write-Host "✅ Retailer registered" -ForegroundColor Green
    
    $productsResponse = Invoke-RestMethod -Uri "$baseUrl/api/products" -Method Get
    $products = $productsResponse.data.products
    Write-Host "Products found: $($products.Count)"
    Write-Host "First product:" -ForegroundColor Cyan
    $products[0] | ConvertTo-Json -Depth 2
    Write-Host "Product ID: $($products[0].id)" -ForegroundColor Yellow
    
    if ($products.Count -gt 0) {
        $product = $products[0]
        $orderBody = @{
            items = @(
                @{
                    productId = $product.id
                    productName = $product.name
                    quantity = 5
                    unitPrice = $product.basePrice
                    discountPercent = 0.0
                }
            )
            retailerNotes = "Debug order"
        } | ConvertTo-Json -Depth 10
        
        Write-Host "Order body: $orderBody"
        $headers = @{ Authorization = "Bearer $retailerToken" }
        $orderResponse = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Post -Body $orderBody -ContentType "application/json" -Headers $headers -ErrorAction Stop
        Write-Host "✅ Order created" -ForegroundColor Green
        $orderResponse | ConvertTo-Json -Depth 3
    }
} catch {
    Write-Host "❌ Order failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "Response body:" -ForegroundColor Red
        $_.ErrorDetails.Message
    }
}
