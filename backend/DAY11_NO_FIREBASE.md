# Day 11: Order Timeline & Polling (No Firebase)

✅ **Complete Implementation - No Firebase Dependencies**

## Overview
This implementation provides order tracking and updates WITHOUT requiring Firebase/FCM. Instead, the Android app polls for updates periodically.

## Changes Made

### 1. Removed Firebase/FCM Dependencies
- ❌ Removed `FCMNotificationService` (replaced with `NotificationService`)
- ❌ Removed `FCMRoutes.kt` (no FCM token management needed)
- ❌ Removed FCM token columns from database
- ❌ Removed Ktor HTTP client dependency
- ✅ Simple logging-based notification system

### 2. Added Order Timeline System
- ✅ `order_timeline` table for tracking order status history
- ✅ Automatic triggers for timeline creation on status change
- ✅ Timeline REST API endpoints

### 3. Added Polling Endpoints
- ✅ GET `/api/retailer/orders/updates?since={timestamp}` - Get order updates
- ✅ GET `/api/retailer/orders/has-updates?since={timestamp}` - Check for new updates

## Database Migration

### V10__order_timeline.sql
```sql
-- Create order_timeline table
CREATE TABLE IF NOT EXISTS order_timeline (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    message TEXT,
    created_by UUID REFERENCES app_user(id),
    created_by_role VARCHAR(20),
    notification_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for fast queries
CREATE INDEX IF NOT EXISTS idx_order_timeline_order_id ON order_timeline(order_id);
CREATE INDEX IF NOT EXISTS idx_order_timeline_created_at ON order_timeline(created_at DESC);

-- Automatic trigger for timeline creation
CREATE OR REPLACE FUNCTION create_order_timeline_entry()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO order_timeline (...) VALUES (...);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_create_order_timeline
    AFTER UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION create_order_timeline_entry();
```

## API Endpoints

### Admin Order Status Updates
```http
POST /api/orders/{orderId}/confirm
POST /api/orders/{orderId}/pack
POST /api/orders/{orderId}/out-for-delivery
POST /api/orders/{orderId}/deliver

Authorization: Bearer {admin_jwt_token}
```

**Response:**
```json
{
  "success": true,
  "message": "Order status updated to CONFIRMED successfully",
  "data": null
}
```

### Retailer Order Polling
```http
GET /api/retailer/orders/updates?since=1699999999000

Authorization: Bearer {retailer_jwt_token}
```

**Response:**
```json
{
  "success": true,
  "message": "Order updates retrieved successfully",
  "updates": [
    {
      "orderId": "550e8400-e29b-41d4-a716-446655440000",
      "orderNumber": "55440000",
      "status": "CONFIRMED",
      "paymentStatus": "UNPAID",
      "totalAmount": 15000.0,
      "recentTimeline": [
        {
          "id": "timeline-uuid",
          "status": "CONFIRMED",
          "message": "Order has been confirmed by factory",
          "createdByRole": "ADMIN",
          "notificationSent": true,
          "createdAt": 1700000001000
        }
      ],
      "lastUpdated": 1700000001000
    }
  ],
  "timestamp": 1700000100000
}
```

### Check for Updates
```http
GET /api/retailer/orders/has-updates?since=1699999999000

Authorization: Bearer {retailer_jwt_token}
```

**Response:**
```json
{
  "success": true,
  "message": "New updates available",
  "data": {
    "hasUpdates": true,
    "timestamp": 1700000100000
  }
}
```

### Order Timeline
```http
GET /api/orders/{orderId}/timeline

Authorization: Bearer {jwt_token}
```

**Response:**
```json
{
  "success": true,
  "message": "Timeline retrieved successfully",
  "timeline": [
    {
      "id": "timeline-uuid",
      "status": "CONFIRMED",
      "message": "Order has been confirmed by factory",
      "createdByRole": "ADMIN",
      "notificationSent": true,
      "createdAt": 1700000001000
    },
    {
      "id": "timeline-uuid-2",
      "status": "PENDING",
      "message": "Order created",
      "createdByRole": "SYSTEM",
      "notificationSent": false,
      "createdAt": 1700000000000
    }
  ]
}
```

## Android App Integration

### 1. Add Polling Worker (WorkManager)
```kotlin
// app/build.gradle.kts
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}

// OrderPollingWorker.kt
class OrderPollingWorker(
    context: Context,
    params: WorkerParameters,
    private val orderRepository: OrderRepository
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        try {
            // Get last sync timestamp from SharedPreferences
            val lastSync = getLastSyncTimestamp()
            
            // Poll for updates
            val hasUpdates = orderRepository.checkForUpdates(lastSync)
            
            if (hasUpdates) {
                // Show notification
                showUpdateNotification()
                
                // Refresh orders
                orderRepository.refreshOrders()
            }
            
            // Update last sync timestamp
            saveLastSyncTimestamp(System.currentTimeMillis())
            
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
    
    private fun showUpdateNotification() {
        val notification = NotificationCompat.Builder(applicationContext, "orders")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Order Update")
            .setContentText("Your order status has been updated")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        NotificationManagerCompat.from(applicationContext)
            .notify(1, notification)
    }
}
```

### 2. Schedule Periodic Polling
```kotlin
// Application.kt or MainActivity.kt
class KulfiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Schedule periodic polling every 15 minutes
        val pollingRequest = PeriodicWorkRequestBuilder<OrderPollingWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "order_polling",
                ExistingPeriodicWorkPolicy.KEEP,
                pollingRequest
            )
    }
}
```

### 3. Update OrderRepository
```kotlin
@Singleton
class OrderRepository @Inject constructor(
    private val apiService: ApiService,
    private val sharedPreferences: SharedPreferences
) {
    
    suspend fun checkForUpdates(since: Long): Boolean {
        return try {
            val response = apiService.hasOrderUpdates(since)
            response.data?.get("hasUpdates") as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun fetchOrderUpdates(since: Long): List<OrderUpdateDTO> {
        return try {
            val response = apiService.getOrderUpdates(since)
            response.updates
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun refreshOrders() {
        val lastSync = sharedPreferences.getLong("last_order_sync", 0L)
        val updates = fetchOrderUpdates(lastSync)
        
        // Update local database or StateFlow
        _ordersFlow.value = updates.map { it.toOrder() }
    }
}

// ApiService.kt
interface ApiService {
    @GET("/api/retailer/orders/has-updates")
    suspend fun hasOrderUpdates(
        @Query("since") since: Long
    ): ApiResponse<Map<String, Any>>
    
    @GET("/api/retailer/orders/updates")
    suspend fun getOrderUpdates(
        @Query("since") since: Long
    ): OrderUpdatesResponse
    
    @GET("/api/orders/{orderId}/timeline")
    suspend fun getOrderTimeline(
        @Path("orderId") orderId: String
    ): OrderTimelineResponse
}
```

### 4. Add Notification Channel
```kotlin
// MainActivity.kt onCreate()
createNotificationChannel()

private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "orders",
            "Order Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for order status updates"
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
```

### 5. Request Notification Permission (Android 13+)
```kotlin
// Add to AndroidManifest.xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

// Request at runtime (Android 13+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) 
        != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1001
        )
    }
}
```

## Testing

### 1. Start Backend
```bash
cd backend
.\gradlew.bat shadowJar
java -jar build/libs/ganeshkulfi-backend-all.jar
```

### 2. Test Admin Updates Order
```bash
# Login as admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ganeshkulfi.com","password":"Admin1234"}'

# Update order status
curl -X POST http://localhost:8080/api/orders/{orderId}/confirm \
  -H "Authorization: Bearer {admin_token}"
```

### 3. Test Retailer Polling
```bash
# Login as retailer
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"retailer@test.com","password":"Retailer1234"}'

# Check for updates
curl -X GET "http://localhost:8080/api/retailer/orders/has-updates?since=0" \
  -H "Authorization: Bearer {retailer_token}"

# Get full updates
curl -X GET "http://localhost:8080/api/retailer/orders/updates?since=0" \
  -H "Authorization: Bearer {retailer_token}"
```

## Order Status Flow

```
PENDING 
   ↓ (Admin confirms)
CONFIRMED
   ↓ (Admin packs)
PACKED
   ↓ (Admin dispatches)
OUT_FOR_DELIVERY
   ↓ (Admin marks delivered)
DELIVERED
```

Each status change creates a timeline entry automatically via database trigger.

## Advantages Over Firebase

### ✅ Pros:
- **No external dependencies** - No Firebase setup required
- **No API keys** - No FCM server key management
- **Works offline-first** - App can work without polling
- **Simple architecture** - Just REST API calls
- **No quota limits** - No Firebase free tier limits

### ⚠️ Cons:
- **Battery usage** - Periodic polling uses more battery than push
- **Delayed updates** - Updates only arrive when app polls (every 15 mins)
- **Network usage** - More API calls compared to push notifications

## Performance Optimization

### 1. Smart Polling Intervals
```kotlin
// Use exponential backoff when no updates
var pollInterval = 15L // minutes

if (!hasUpdates) {
    pollInterval = min(pollInterval * 2, 60L) // Max 1 hour
} else {
    pollInterval = 15L // Reset to 15 mins when updates found
}
```

### 2. Conditional Polling
```kotlin
// Only poll when app is in foreground or specific conditions
if (isAppInForeground() || hasPendingOrders()) {
    pollForUpdates()
}
```

### 3. WebSocket Alternative (Future Enhancement)
For real-time updates without Firebase, consider WebSocket:
```kotlin
// Future: Add WebSocket support for real-time updates
wss://localhost:8080/api/ws/orders
```

## Summary

✅ **Completed:**
- Order timeline tracking system
- Admin order status update APIs
- Retailer polling endpoints
- Automatic timeline creation via DB triggers
- Removed all Firebase dependencies

📱 **Android App TODO:**
- Add WorkManager for periodic polling
- Create OrderPollingWorker
- Add notification channel and permissions
- Update OrderRepository with polling methods
- Display order timeline in Order Details screen

🚀 **Backend Version:** 0.0.11-SNAPSHOT (No Firebase)
