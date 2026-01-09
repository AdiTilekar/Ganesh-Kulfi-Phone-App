package com.ganeshkulfi.app.presentation.ui.retailer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ganeshkulfi.app.presentation.viewmodel.CartViewModel
import com.ganeshkulfi.app.presentation.viewmodel.CartItemNew
import com.ganeshkulfi.app.presentation.viewmodel.RetailerViewModel
import com.ganeshkulfi.app.presentation.viewmodel.OrderPlacementStatus
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateBack: () -> Unit,
    onOrderSuccess: (String) -> Unit,
    cartViewModel: CartViewModel,
    retailerViewModel: RetailerViewModel = hiltViewModel()
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val cartSummary by cartViewModel.cartSummary.collectAsState()
    val orderStatus by retailerViewModel.orderPlacementStatus.collectAsState()
    val context = LocalContext.current
    
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isPlacingOrder by remember { mutableStateOf(false) }
    
    // Handle order status
    LaunchedEffect(orderStatus) {
        when (orderStatus) {
            is OrderPlacementStatus.Success -> {
                val orderNumber = (orderStatus as OrderPlacementStatus.Success).orderId // Changed from orderNumber to orderId
                cartViewModel.clearCart()
                isPlacingOrder = false
                Toast.makeText(context, "✅ Order placed successfully!", Toast.LENGTH_LONG).show()
                retailerViewModel.resetOrderStatus()
                onOrderSuccess(orderNumber)
            }
            is OrderPlacementStatus.Error -> {
                val errorMsg = (orderStatus as OrderPlacementStatus.Error).message
                Toast.makeText(context, "❌ $errorMsg", Toast.LENGTH_LONG).show()
                isPlacingOrder = false
                retailerViewModel.resetOrderStatus()
            }
            is OrderPlacementStatus.Loading -> {
                isPlacingOrder = true
            }
            else -> {
                isPlacingOrder = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Cart",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (cartItems.isNotEmpty()) {
                        TextButton(
                            onClick = { cartViewModel.clearCart() }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                CartBottomBar(
                    subtotal = cartSummary.subtotal,
                    itemCount = cartSummary.itemCount,
                    totalItems = cartSummary.totalItems,
                    onPlaceOrder = { showConfirmDialog = true },
                    isLoading = isPlacingOrder
                )
            }
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            EmptyCartState(onNavigateBack)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems.values.toList(), key = { it.productId }) { item ->
                    CartItemCard(
                        item = item,
                        onIncrement = {
                            cartViewModel.updateQuantity(item.productId, item.quantity + 1)
                        },
                        onDecrement = {
                            cartViewModel.updateQuantity(item.productId, item.quantity - 1)
                        },
                        onRemove = {
                            cartViewModel.removeFromCart(item.productId)
                        }
                    )
                }
                
                // Summary Section
                item {
                    Spacer(Modifier.height(8.dp))
                    CartSummaryCard(
                        itemCount = cartSummary.itemCount,
                        totalItems = cartSummary.totalItems,
                        subtotal = cartSummary.subtotal
                    )
                }
                
                // Bottom spacer
                item {
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
    
    // Confirmation Dialog
    if (showConfirmDialog) {
        ConfirmOrderDialog(
            itemCount = cartSummary.totalItems,
            total = cartSummary.subtotal,
            onConfirm = {
                showConfirmDialog = false
                retailerViewModel.placeOrderFromCart(cartItems.values.toList())
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
fun CartItemCard(
    item: CartItemNew,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val drawableName = getDrawableResourceIdFromName(item.productName)
    val drawableId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(drawableId)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.productName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            // Product Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.productName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "₹${String.format("%.0f", item.unitPrice)} per piece",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Remove",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quantity Controls
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDecrement,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                "Decrease",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Text(
                            text = item.quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        
                        IconButton(
                            onClick = onIncrement,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                "Increase",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // Line Total
                    Text(
                        "₹${String.format("%.0f", item.lineTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun CartSummaryCard(
    itemCount: Int,
    totalItems: Int,
    subtotal: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Order Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(12.dp))
            
            SummaryRow("Items", "$itemCount products")
            Spacer(Modifier.height(4.dp))
            SummaryRow("Total Quantity", "$totalItems pieces")
            
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total Amount",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "₹${String.format("%.0f", subtotal)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CartBottomBar(
    subtotal: Double,
    itemCount: Int,
    totalItems: Int,
    onPlaceOrder: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 16.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "₹${String.format("%.0f", subtotal)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "$totalItems items • $itemCount products",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Button(
                    onClick = onPlaceOrder,
                    enabled = !isLoading,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Place Order",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCartState(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Text(
                "Your cart is empty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                "Add some delicious kulfis to get started!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ArrowBack, null)
                Spacer(Modifier.width(8.dp))
                Text("Browse Products")
            }
        }
    }
}

@Composable
fun ConfirmOrderDialog(
    itemCount: Int,
    total: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Confirm Your Order",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "You are about to place an order for:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Items:", fontWeight = FontWeight.SemiBold)
                            Text("$itemCount")
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Amount:", fontWeight = FontWeight.Bold)
                            Text(
                                "₹${String.format("%.0f", total)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Confirm Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getDrawableResourceIdFromName(productName: String): String {
    return when {
        productName.contains("Mango", ignoreCase = true) -> "mango_kulfi"
        productName.contains("Rabdi", ignoreCase = true) || productName.contains("Rabadi", ignoreCase = true) -> "rabdi_kulfi"
        productName.contains("Strawberry", ignoreCase = true) -> "strawberry_kulfi"
        productName.contains("Chocolate", ignoreCase = true) -> "chocolate_kulfi"
        productName.contains("Paan", ignoreCase = true) -> "paan_kulfi"
        productName.contains("Pineapple", ignoreCase = true) -> "pineapple_kulfi"
        productName.contains("Chikoo", ignoreCase = true) -> "chikoo_kulfi"
        productName.contains("Guava", ignoreCase = true) -> "guava_kulfi"
        productName.contains("Jamun", ignoreCase = true) -> "jamun_kulfi"
        productName.contains("Fig", ignoreCase = true) -> "fig_kulfi"
        productName.contains("Shitafal", ignoreCase = true) || productName.contains("Sitafal", ignoreCase = true) -> "sitafal_kulfi"
        productName.contains("Dry Fruit", ignoreCase = true) || productName.contains("Dryfruit", ignoreCase = true) -> "dry_fruit_kulfi"
        productName.contains("Gulkand", ignoreCase = true) -> "gulkand_kulfi"
        else -> "logo"
    }
}
