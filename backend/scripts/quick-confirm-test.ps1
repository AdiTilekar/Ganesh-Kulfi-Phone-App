# Quick Confirmation Test
$baseUrl = "http://localhost:8080"

# Register admin
$adminBody = @{
    email = "confirmadmin$(Get-Random)@test.com"
    password = "Admin@123"
    name = "Confirm Admin"
    phone = "9999999999"
    role = "ADMIN"
} | ConvertTo-Json

$adminResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $adminBody -ContentType "application/json"
$adminToken = $adminResponse.data.token
Write-Host "✅ Admin registered" -ForegroundColor Green

# Register retailer and create order
$retailerBody = @{
    email = "confirmretailer$(Get-Random)@test.com"
    password = "Retailer@123"
    name = "Confirm Retailer"
    phone = "8888888888"
    role = "RETAILER"
    shopName = "Confirm Shop"
} | ConvertTo-Json

$retailerResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $retailerBody -ContentType "application/json"
$retailerToken = $retailerResponse.data.token
Write-Host "✅ Retailer registered" -ForegroundColor Green

# Get products
$productsResponse = Invoke-RestMethod -Uri "$baseUrl/api/products" -Method Get
$products = $productsResponse.data.products
$product = $products[0]

# Create order
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
    retailerNotes = "Confirmation test order"
} | ConvertTo-Json -Depth 10

$headers = @{ Authorization = "Bearer $retailerToken" }
$orderResponse = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Post -Body $orderBody -ContentType "application/json" -Headers $headers
$orderId = $orderResponse.data.id
Write-Host "✅ Order created: $orderId" -ForegroundColor Green

# Confirm order
Write-Host "`nTesting confirmation..." -ForegroundColor Cyan
try {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $confirmResponse = Invoke-RestMethod -Uri "$baseUrl/api/orders/$orderId/confirm" -Method Post -Headers $headers -ErrorAction Stop
    Write-Host "✅ Order confirmed successfully!" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Cyan
    $confirmResponse | ConvertTo-Json -Depth 3
} catch {
    Write-Host "❌ Confirmation failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "Error details:" -ForegroundColor Red
        $_.ErrorDetails.Message
    }
}
