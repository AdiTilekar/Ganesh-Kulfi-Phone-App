# Security Model & Role Separation

## Business Model Overview

**Shree Ganesh Kulfi** operates with three distinct entities:

```
Factory Owner (Admin)
        ↕️ (orders only)
    Retailers
        ↕️ (sales)
    Customers
```

### Key Principle:
**Factory Owner and Retailers are SEPARATE businesses** with NO direct communication except through the order system.

---

## 🔒 Role-Based Access Control

### 1. Factory Owner (Admin Role)
**Full Access** - Complete control of the business

#### Can See:
✅ All inventory stock levels (real-time)  
✅ Total stock, available stock, stock given to retailers  
✅ Cost prices and profit margins  
✅ All retailer information  
✅ All customer orders  
✅ All transactions and payments  
✅ Dashboard analytics  
✅ Sales metrics  

#### Can Do:
✅ Add/remove inventory  
✅ Update stock levels  
✅ Add/edit/delete retailers  
✅ Set pricing tiers for retailers  
✅ Give stock to retailers  
✅ Record payments  
✅ View all reports  
✅ Manage the entire system  

### 2. Retailers
**Limited Access** - Can only order products and manage their own orders

#### Can See:
✅ Product names and flavors  
✅ **Their own pricing** (based on their tier discount)  
✅ **Their own orders** (past and pending)  
✅ **Their own profile** information  

#### CANNOT See:
❌ Factory stock levels  
❌ How much stock is available  
❌ Cost prices  
❌ Other retailers' information  
❌ Other retailers' orders  
❌ Customer orders  
❌ Factory's inventory management  
❌ Dashboard or analytics  

#### Can Do:
✅ Place orders (unlimited quantity - factory will fulfill)  
✅ View order history  
✅ Edit their profile  

#### CANNOT Do:
❌ Access admin panel  
❌ See inventory management  
❌ View stock levels  
❌ Communicate with factory except through orders  

### 3. Customers
**Most Restricted** - Can only buy products based on availability

#### Can See:
✅ Product catalog  
✅ Prices (standard retail prices)  
✅ Availability status (In Stock / Out of Stock)  
✅ Their own orders  
✅ Their profile  

#### CANNOT See:
❌ Factory stock quantities  
❌ Retailer information  
❌ Pricing tiers  
❌ Cost prices  
❌ Admin panel  
❌ Other customers' orders  

#### Can Do:
✅ Browse products  
✅ Add to cart (only if stock > 0)  
✅ Place orders (limited by available stock)  
✅ View order history  

#### CANNOT Do:
❌ Order when out of stock  
❌ Order more than available stock  
❌ Access admin features  
❌ See retailer information  

---

## 🚫 Business Logic Differences

### Ordering Rules

| Aspect | Factory Owner | Retailer | Customer |
|--------|--------------|----------|----------|
| **Can Order** | N/A (manages stock) | ✅ Yes | ✅ Yes |
| **Quantity Limit** | N/A | ❌ Unlimited | ✅ Limited by stock |
| **See Stock Levels** | ✅ Yes | ❌ No | ⚠️ Yes/No only |
| **Order Fulfillment** | N/A | Factory produces | Delivered from stock |
| **Stock Check** | N/A | ❌ Not enforced | ✅ Enforced |

### Stock Visibility

**Factory Owner:**
```
Mango Kulfi
├── Total Stock: 500 units
├── Available Stock: 300 units
├── Stock Given to Retailers: 200 units
├── Sold Today: 50 units
├── Cost Price: ₹30
└── Selling Price: ₹50
```

**Retailer:**
```
Mango Kulfi
├── Your Price: ₹37.50 (25% VIP discount)
└── [Can order any quantity]
```

**Customer:**
```
Mango Kulfi
├── Price: ₹50
└── Status: In Stock ✅
```

---

## 🔐 Implementation Details

### UI Level Protection

**1. Retailer Screens (RetailerOrderKulfiScreen, RetailerPlaceOrderScreen)**
- ❌ Removed stock badges ("Out of Stock", "Low Stock")
- ❌ Removed stock display text ("X units available")
- ❌ Removed stock-based button disabling
- ✅ Clean UI with only product info and pricing

**2. Customer Screens (HomeScreen, CartScreen)**
- ✅ Shows availability status (In Stock / Out of Stock)
- ✅ Enforces stock limits on "Add to Cart"
- ✅ Cannot order if stock = 0

**3. Admin Screens (Dashboard, Inventory, Retailer Management)**
- ✅ Full access to all data
- ✅ Complete stock visibility
- ✅ All management features

### Data Model Protection

**Created: `ProductCatalogItem`**
```kotlin
data class ProductCatalogItem(
    val flavorId: String,
    val flavorName: String,
    val sellingPrice: Double,
    val isAvailable: Boolean  // Only for customers
)
// Does NOT include stock levels, cost prices, or factory data
```

**Conversion Function:**
```kotlin
fun InventoryItem.toProductCatalogItem(forRetailer: Boolean = true): ProductCatalogItem {
    // Strips away factory-sensitive information
    isAvailable = if (forRetailer) true else this.availableStock > 0
}
```

### ViewModel Level

**RetailerViewModel:**
- Exposes product catalog (currently still using `InventoryItem` but UI hides stock)
- Future improvement: Use `ProductCatalogItem` instead
- No access to admin operations

**AdminViewModel:**
- Full access to `InventoryRepository`, `RetailerRepository`, etc.
- All management operations

**FlavorViewModel (Customer):**
- Limited to product browsing
- Shows availability status only

---

## 🔄 Order Flow Differences

### Retailer Order Flow
```
1. Retailer logs in
2. Browses products (no stock info shown)
3. Enters quantity (any number - 100, 500, 1000)
4. Places order
5. Order sent to Factory Owner
6. Factory Owner sees order
7. Factory produces required quantity (if needed)
8. Factory fulfills order
```

**Why No Stock Limits?**
- Retailer and Factory are separate businesses
- Factory will produce more to fulfill demand
- No need to restrict retailer based on current stock
- Factory owner decides production schedule

### Customer Order Flow
```
1. Customer browses products
2. Sees "In Stock" or "Out of Stock"
3. Can only add if stock > 0
4. Quantity limited by available stock
5. Places order
6. Order fulfilled from available stock
7. Stock decreases
```

**Why Stock Limits?**
- Customers buy from available inventory
- Cannot promise more than what's in stock
- Immediate fulfillment expected
- Stock-based restrictions necessary

---

## 🛡️ Security Considerations

### Current Implementation ✅

1. **UI Hiding** (Implemented)
   - Retailer screens don't display stock information
   - Removed all stock badges and indicators
   - Clean separation in UI

2. **Role-Based Navigation** (Implemented)
   - Retailers cannot access admin routes
   - Proper navigation guards
   - Role checked on login

3. **Order Logic** (Implemented)
   - Retailers: No stock validation
   - Customers: Stock validation enforced
   - Different business rules per role

### Future Improvements 🔄

1. **API Level Protection** (When backend added)
   - Separate endpoints for retailers vs admin
   - Role-based API authorization
   - JWT tokens with role claims

2. **Data Model Separation** (Partially done)
   - Use `ProductCatalogItem` in RetailerViewModel
   - Never send `InventoryItem` to retailers
   - Encrypted sensitive data

3. **Audit Logging**
   - Track who accessed what data
   - Log all admin operations
   - Monitor suspicious access patterns

---

## 📁 Files Modified for Security

### Created:
1. ✅ `ProductCatalog.kt` - Secure data model without factory data

### Modified:
1. ✅ `RetailerOrderKulfiScreen.kt` - Removed stock displays
2. ✅ `RetailerPlaceOrderScreen.kt` - Removed stock restrictions
3. ✅ `RetailerViewModel.kt` - Added security comments
4. ✅ `HomeScreen.kt` - (Already had stock checks for customers)

### Untouched (Admin Only):
1. ✅ `AdminViewModel.kt` - Full access maintained
2. ✅ `InventoryRepository.kt` - Complete inventory management
3. ✅ `RetailerRepository.kt` - Admin-only operations

---

## ✅ Verification Checklist

### Retailer Screens:
- [x] No "Out of Stock" badges visible
- [x] No "Low Stock" warnings visible
- [x] No "X units available" text visible
- [x] Can enter quantities > 1000
- [x] Order button always enabled
- [x] + button not capped by stock

### Customer Screens:
- [x] Shows "In Stock" / "Out of Stock" status
- [x] Cannot add to cart if stock = 0
- [x] Quantity limited by available stock
- [x] Proper stock validation

### Admin Screens:
- [x] Full stock visibility maintained
- [x] All management features working
- [x] Dashboard shows correct metrics
- [x] Can manage inventory fully

---

## 🎯 Key Takeaways

### 1. **Separation of Concerns**
Factory Owner and Retailers are separate businesses - treat them as such.

### 2. **Different Rules for Different Roles**
- **Retailers**: Unlimited ordering (B2B wholesale)
- **Customers**: Stock-limited ordering (B2C retail)
- **Admin**: Complete visibility and control

### 3. **Security Through Hiding**
Current implementation uses UI-level hiding. Future should add:
- API-level filtering
- Data model separation
- Backend authorization

### 4. **Business Logic Alignment**
Technical implementation now matches business reality:
- Retailers order what they need
- Factory produces to fulfill
- Customers buy from available stock

---

**Implementation Date**: November 9, 2025  
**Status**: ✅ Core Security Implemented  
**Next Step**: Backend API with role-based authorization
