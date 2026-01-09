# Retailer-Specific Pricing System

## 📋 Overview

The **Retailer-Specific Pricing System** allows you to set different prices for different retailers based on:
1. **Pricing Tiers** - Automatic discounts based on retailer category
2. **Custom Pricing** - Manually set prices for specific retailer-flavor combinations
3. **Bulk Discounts** - Quantity-based automatic discounts
4. **Priority System** - Custom prices override tier discounts

---

## 🎯 Pricing Tiers

### Available Tiers

| Tier | Discount | Description | Use Case |
|------|----------|-------------|----------|
| **VIP** | 25% OFF | Premium retailers with highest volume | Long-term high-volume customers |
| **PREMIUM** | 15% OFF | High volume retailers | Regular bulk buyers |
| **REGULAR** | 10% OFF | Standard retailers | Standard partnership |
| **WHOLESALE** | 5% OFF | Bulk buyers | Wholesale distributors |
| **RETAIL** | 0% OFF | Small retailers | New or small-scale retailers |
| **CUSTOM** | Variable | Custom negotiated prices | Special agreements |

### How Pricing Tiers Work

When you assign a tier to a retailer, they automatically get the discount on **ALL flavors**:

**Example:**
- Base Price: Mango Kulfi ₹20
- Retailer Tier: **PREMIUM** (15% OFF)
- **Retailer Price: ₹17** (₹3 savings per unit)

For 100 units:
- Base Total: ₹2,000
- **Retailer Total: ₹1,700**
- **Total Savings: ₹300**

---

## 💰 Custom Pricing

### When to Use Custom Pricing

Use custom pricing when:
- ✅ You have a special agreement with a retailer
- ✅ A retailer buys specific flavors in very high volume
- ✅ You want to set minimum order quantities
- ✅ You need pricing different from tier discount

### Custom Price Priority

Custom prices **OVERRIDE** tier discounts:

```
Priority Order:
1. Custom Price (if set) ← Highest Priority
2. Bulk Discount (quantity-based)
3. Tier Discount
4. Base Price ← Lowest Priority
```

### Custom Pricing Example

**Scenario:** Kumar Sweet Shop (VIP tier, 25% OFF) wants special pricing on Chocolate Kulfi

**Normal Pricing (VIP Tier):**
- Base Price: ₹35
- VIP Discount: 25%
- Price: ₹26.25

**Custom Pricing:**
- Custom Price: ₹25 (fixed)
- **Final Price: ₹25** (custom price used instead of tier)

---

## 📦 Bulk Discounts

### Default Bulk Pricing Rules

Automatic discounts based on quantity ordered:

| Minimum Quantity | Discount |
|------------------|----------|
| 100 units | 5% OFF |
| 200 units | 10% OFF |
| 500 units | 15% OFF |
| 1,000 units | 20% OFF |

### Bulk Discount Examples

**Example 1: Regular Tier Retailer ordering 150 units**
- Base Price: ₹20
- Regular Tier: 10% OFF = ₹18
- Bulk Discount (100+): 5% OFF = ₹19
- **Final Price: ₹18** (tier is better)

**Example 2: Regular Tier Retailer ordering 600 units**
- Base Price: ₹20
- Regular Tier: 10% OFF = ₹18
- Bulk Discount (500+): 15% OFF = ₹17
- **Final Price: ₹17** (bulk is better)

System **automatically uses the BEST discount** for the retailer!

---

## 🔧 How to Use the System

### 1. Assigning Pricing Tiers

**Steps:**
1. Go to **Admin Dashboard**
2. Click **Pricing Management**
3. Find the retailer
4. Click **Change Tier** dropdown
5. Select new tier (VIP, Premium, Regular, etc.)
6. Tier applies immediately to all future transactions

**When to Change Tiers:**
- Retailer increases order volume → Upgrade to higher tier
- Retailer payment history improves → Upgrade
- New partnership established → Start with RETAIL, promote later
- Retailer reduces orders → Downgrade tier

### 2. Setting Custom Prices

**Steps:**
1. Go to **Pricing Management**
2. Click on retailer card
3. Dialog shows all flavors with current pricing
4. Find the flavor
5. Click to set custom price
6. Enter:
   - Custom price per unit
   - Additional discount % (optional)
   - Minimum quantity (optional)
7. Save

**Custom Price Fields:**
- **Custom Price**: Fixed price per unit (overrides tier)
- **Additional Discount**: Extra % off the custom price
- **Minimum Quantity**: Required order size for this price

### 3. Viewing Price Breakdown

**In Pricing Management:**
- Each flavor shows:
  - Base Price (your standard price)
  - Retailer Price (what they actually pay)
  - Discount % (how much they save)
  - Whether custom pricing is active

**Example Display:**
```
Mango Kulfi
Base: ₹20
Retailer: ₹15.00
15% OFF
Custom Pricing Active
```

---

## 💼 Real-World Scenarios

### Scenario 1: New Retailer Partnership

**Situation:** Sharma Ice Cream Parlor just joined

**Steps:**
1. Add retailer with **RETAIL** tier (0% discount)
2. Monitor their orders for 1 month
3. After consistent ₹20,000/month orders, upgrade to **REGULAR** (10% OFF)
4. After 3 months of ₹50,000/month, upgrade to **PREMIUM** (15% OFF)

**Result:** Retailer earns better pricing through performance

---

### Scenario 2: Special Festival Pricing

**Situation:** Kumar Sweet Shop wants 200 units of Mango Kulfi for Diwali

**Current Setup:**
- Retailer Tier: VIP (25% OFF)
- Mango Kulfi Base: ₹20
- Normal VIP Price: ₹15

**Special Deal:**
1. Set custom price for Kumar + Mango: ₹13
2. Set minimum quantity: 200
3. Valid for this order

**Calculation:**
- 200 units × ₹13 = ₹2,600
- vs 200 units × ₹15 = ₹3,000
- **Extra ₹400 savings for customer**

---

### Scenario 3: Flavor-Specific Discounting

**Situation:** Chocolate Kulfi is overstocked, need to move inventory

**Strategy:**
1. Keep all retailers at their current tiers
2. Set custom pricing ONLY for Chocolate Kulfi:
   - VIP retailers: ₹25 → ₹22
   - Premium retailers: ₹30 → ₹27
   - Regular retailers: ₹31 → ₹29

**Result:** Targeted discount without affecting other flavors

---

### Scenario 4: Volume-Based Custom Pricing

**Situation:** Priya Sharma wants better pricing on 500+ unit orders

**Setup:**
1. Keep her at PREMIUM tier (15% OFF for normal orders)
2. Set custom pricing on popular flavors:
   - Mango Kulfi: ₹16 (min 500 units)
   - Strawberry: ₹20 (min 500 units)
3. Small orders still use PREMIUM tier pricing

**Benefit:** Encourages bulk ordering while maintaining tier for small orders

---

## 📊 Price Calculation Logic

### Automatic Price Selection

The system automatically calculates the **BEST** price:

```kotlin
fun calculatePrice(
    retailer: Retailer,
    flavor: Flavor,
    quantity: Int
): Double {
    
    // 1. Check for custom price
    if (hasCustomPrice(retailer, flavor)) {
        return customPrice
    }
    
    // 2. Calculate bulk discount
    bulkDiscount = getBulkDiscount(quantity)
    
    // 3. Get tier discount
    tierDiscount = retailer.pricingTier.discount
    
    // 4. Use BETTER discount
    effectiveDiscount = max(bulkDiscount, tierDiscount)
    
    return basePrice * (1 - effectiveDiscount/100)
}
```

### Example Calculation

**Inputs:**
- Retailer: Kumar Sweet Shop (VIP tier - 25% OFF)
- Flavor: Mango Kulfi (₹20 base)
- Quantity: 600 units
- Custom Price: None

**Step-by-Step:**
1. Check custom price → None found
2. Bulk discount (600 units) → 15% OFF
3. Tier discount (VIP) → 25% OFF
4. **Use 25% (tier is better)**
5. Final price: ₹20 × 0.75 = **₹15/unit**
6. Total: 600 × ₹15 = **₹9,000**

---

## 🎨 User Interface Guide

### Pricing Management Screen

**Layout:**
```
┌─────────────────────────────────────┐
│ Pricing Management          [Back]  │
├─────────────────────────────────────┤
│ ℹ️ Manage Retailer Pricing          │
│ Set pricing tiers or custom prices  │
├─────────────────────────────────────┤
│ Pricing Tiers                       │
│ ┌─────────────────────────────────┐ │
│ │ ⭐ VIP Tier          25% OFF    │ │
│ │ ⭐ PREMIUM Tier      15% OFF    │ │
│ │ ⭐ REGULAR Tier      10% OFF    │ │
│ │ ⭐ WHOLESALE         5% OFF     │ │
│ │ ⭐ RETAIL            0% OFF     │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Retailers (2)                       │
│ ┌─────────────────────────────────┐ │
│ │ Kumar Sweet Shop         [Edit] │ │
│ │ Rajesh Kumar                    │ │
│ │ ⭐ VIP Tier - 25% discount      │ │
│ │           [Change Tier ▼]       │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

### Retailer Pricing Dialog

Click on any retailer to see detailed pricing:

```
┌──────────────────────────────────────┐
│ Custom Pricing for                   │
│ Kumar Sweet Shop                     │
├──────────────────────────────────────┤
│ Current Tier: VIP (25% OFF)          │
├──────────────────────────────────────┤
│ Flavor Prices                        │
│ ┌──────────────────────────────────┐ │
│ │ Mango Kulfi                      │ │
│ │ Base: ₹20  Retailer: ₹15.00      │ │
│ │ 25% OFF                          │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ Chocolate Kulfi        [Remove]  │ │
│ │ Base: ₹35  Retailer: ₹25.00      │ │
│ │ 28.6% OFF                        │ │
│ │ Custom Pricing Active            │ │
│ └──────────────────────────────────┘ │
│                         [Close]      │
└──────────────────────────────────────┘
```

---

## 🔄 Integration with Stock Transactions

### Automatic Price Application

When giving stock to retailer, pricing is **automatic**:

```kotlin
// Old way (manual pricing)
giveStockToRetailer(
    retailerId = "ret_001",
    flavorId = "mango",
    quantity = 100,
    pricePerUnit = 15.0  // Had to calculate manually
)

// New way (automatic pricing)
giveStockToRetailer(
    retailerId = "ret_001",
    flavorId = "mango",
    quantity = 100
    // Price calculated automatically based on tier/custom pricing!
)
```

### Transaction Record

Each transaction stores:
- Retailer ID
- Flavor ID
- Quantity
- **Price Per Unit** (retailer-specific price used)
- Total Amount
- Transaction Type
- Payment Status

**Example Transaction:**
```
Retailer: Kumar Sweet Shop (VIP)
Flavor: Mango Kulfi
Quantity: 100 units
Price/Unit: ₹15 (VIP pricing)
Total: ₹1,500
Savings: ₹500 (vs ₹20 base price)
```

---

## 📈 Business Benefits

### 1. **Retain High-Volume Customers**
- VIP/Premium tiers reward loyalty
- Automatic discounts encourage bulk ordering
- Transparent pricing builds trust

### 2. **Flexible Pricing Strategy**
- Tier system for standard customers
- Custom pricing for special deals
- Bulk discounts for large orders
- Easy to adjust as business grows

### 3. **Inventory Management**
- Custom pricing helps clear overstocked flavors
- Bulk discounts incentivize large orders
- Can offer competitive pricing to specific retailers

### 4. **Simplified Administration**
- Set tier once, applies to all flavors
- Custom prices for exceptions
- Automatic calculation at transaction time
- Clear audit trail in transaction history

### 5. **Competitive Advantage**
- Reward loyal retailers with better pricing
- Attract new retailers with competitive tiers
- Flexibility to match competitor pricing
- Volume-based incentives increase order size

---

## 🛠️ Technical Implementation

### Data Models

**RetailerPricing:**
```kotlin
data class RetailerPricing(
    val id: String,
    val retailerId: String,
    val flavorId: String,
    val customPrice: Double?,
    val discount: Double,
    val minimumQuantity: Int
)
```

**PricingTier (Enum):**
```kotlin
enum class PricingTier(
    val displayName: String,
    val discountPercentage: Double
) {
    VIP("VIP Tier", 25.0),
    PREMIUM("Premium Tier", 15.0),
    REGULAR("Regular Tier", 10.0),
    WHOLESALE("Wholesale", 5.0),
    RETAIL("Retail", 0.0),
    CUSTOM("Custom Pricing", 0.0)
}
```

### Repositories

**PricingRepository:**
- `getRetailerPrice()` - Calculate price for retailer
- `setCustomPrice()` - Set custom pricing
- `removeCustomPrice()` - Remove custom pricing
- `getPriceBreakdown()` - Get detailed breakdown
- `calculateTransactionAmount()` - Calculate total

**InventoryRepository:**
- Stores base prices for flavors
- Provides flavor details

**RetailerRepository:**
- Stores retailer tier information
- CRUD operations for retailers

### ViewModel Integration

**AdminViewModel:**
```kotlin
// Update pricing tier
fun updateRetailerPricingTier(retailerId, tier)

// Set custom price
fun setCustomPrice(retailerId, flavorId, price)

// Get price breakdown for UI
fun getPriceBreakdown(retailer, flavor, quantity)

// Automatic pricing in stock transactions
fun giveStockToRetailer(retailerId, flavorId, quantity)
// ↑ Automatically uses best price
```

---

## 📋 Quick Reference

### Setting Up New Retailer

1. Add retailer in Retailer Management
2. Assign initial tier (usually RETAIL or REGULAR)
3. Monitor order volume
4. Upgrade tier as relationship grows

### Creating Custom Pricing

1. Go to Pricing Management
2. Click retailer card
3. Find flavor in dialog
4. Set custom price + optional discount
5. Set minimum quantity if needed
6. Save

### Viewing Current Pricing

1. Go to Pricing Management
2. All retailers listed with their tiers
3. Click any retailer to see detailed flavor pricing
4. Green highlight = custom pricing active

### Changing Tier

1. Find retailer in Pricing Management
2. Click "Change Tier" button
3. Select new tier from dropdown
4. Changes apply immediately

---

## ⚠️ Important Notes

1. **Custom prices override tiers** - If you set custom price, tier discount is ignored
2. **System uses best discount** - Automatically compares bulk vs tier discount
3. **Tier applies to ALL flavors** - One tier per retailer affects all products
4. **Transactions are immutable** - Once created, transaction prices don't change if tiers change later
5. **Minimum quantities are enforced** - Custom prices with min quantity only apply when met

---

## 🎯 Best Practices

### Tier Assignment
✅ **DO:**
- Start new retailers at RETAIL or REGULAR
- Review and upgrade based on performance
- Document reason for tier changes
- Keep tier changes in admin notes

❌ **DON'T:**
- Give VIP immediately to new retailers
- Downgrade without communication
- Change tiers too frequently
- Use same tier for all retailers

### Custom Pricing
✅ **DO:**
- Use for special agreements
- Set minimum quantities for bulk deals
- Document custom pricing reasons
- Review custom prices periodically

❌ **DON'T:**
- Overuse custom pricing (defeats tier purpose)
- Forget to remove expired deals
- Set confusing minimum quantities
- Make custom prices too complex

### Bulk Discounts
✅ **DO:**
- Keep quantities realistic
- Ensure discounts are profitable
- Communicate bulk discounts to retailers
- Review effectiveness monthly

❌ **DON'T:**
- Set minimums too high
- Offer unsustainable discounts
- Change rules too often
- Ignore market conditions

---

## 📞 Example Customer Communication

### Email Template: Tier Upgrade

```
Subject: Price Tier Upgrade - Better Pricing for Kumar Sweet Shop!

Dear Rajesh Kumar,

Thank you for being a valued partner of Shree Ganesh Kulfi!

We're pleased to inform you that due to your consistent order volume, 
we're upgrading your pricing tier:

Previous Tier: REGULAR (10% discount)
New Tier: PREMIUM (15% discount)

This means you'll now save 15% on all our kulfi flavors!

Example:
- Mango Kulfi: Was ₹18, Now ₹17
- Chocolate Kulfi: Was ₹31.50, Now ₹29.75

Your new pricing is effective immediately.

Thank you for your continued partnership!

Best regards,
Shree Ganesh Kulfi Team
```

---

## 🔮 Future Enhancements

Potential additions to pricing system:

1. **Time-Based Pricing** - Different prices for different times of year
2. **Geographic Pricing** - Prices based on retailer location
3. **Payment Terms Discounts** - Better pricing for advance payment
4. **Volume Commitments** - Lock in pricing with guaranteed monthly volume
5. **Competitor Price Matching** - Track and match competitor pricing
6. **Dynamic Pricing** - AI-based pricing recommendations
7. **Price History** - Track price changes over time
8. **Promotional Pricing** - Temporary promotional rates
9. **Bundle Pricing** - Discounts for flavor combinations
10. **Loyalty Points** - Points system for long-term customers

---

## ✅ Summary

The Retailer-Specific Pricing System provides:

✅ **5 Pricing Tiers** (VIP to Retail) with automatic discounts
✅ **Custom Pricing** for special agreements
✅ **Bulk Discounts** based on order quantity
✅ **Automatic Price Selection** using best available discount
✅ **Easy Management** via dedicated UI
✅ **Transaction Integration** with automatic pricing
✅ **Transparent Pricing** visible to admin
✅ **Flexible System** supporting various business models

**Result:** Professional, scalable pricing management for your kulfi factory business!

---

**System Version:** 1.0  
**Date:** November 7, 2025  
**Status:** ✅ Production Ready
