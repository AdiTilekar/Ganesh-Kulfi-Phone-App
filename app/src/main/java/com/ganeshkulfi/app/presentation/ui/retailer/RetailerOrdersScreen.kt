package com.ganeshkulfi.app.presentation.ui.retailer

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.data.model.Order
import com.ganeshkulfi.app.data.model.OrderStatus
import com.ganeshkulfi.app.presentation.viewmodel.RetailerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerOrdersScreen(
    onNavigateBack: () -> Unit,
    retailerViewModel: RetailerViewModel = hiltViewModel()
) {
    val orders by retailerViewModel.myOrders.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Auto-refresh orders when screen loads
    LaunchedEffect(Unit) {
        retailerViewModel.refreshOrders()
    }
    
    // Show order count as Toast when orders change
    LaunchedEffect(orders.size) {
        if (orders.isNotEmpty()) {
            Toast.makeText(
                context,
                "📦 Total: ${orders.size} orders loaded",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // Separate orders into active and history
    val activeOrders = remember(orders) {
        orders.filter { 
            it.status != OrderStatus.DELIVERED && 
            it.status != OrderStatus.COMPLETED &&
            it.status != OrderStatus.CANCELLED &&
            it.status != OrderStatus.CANCELLED_ADMIN &&
            it.status != OrderStatus.REJECTED
        }.sortedByDescending { it.createdAt }
    }
    val historyOrders = remember(orders) {
        orders.filter { 
            it.status == OrderStatus.DELIVERED ||
            it.status == OrderStatus.COMPLETED ||
            it.status == OrderStatus.CANCELLED ||
            it.status == OrderStatus.CANCELLED_ADMIN ||
            it.status == OrderStatus.REJECTED
        }.sortedByDescending { it.createdAt }
    }
    
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "My Orders (${orders.size})",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                Toast.makeText(context, "🔄 Refreshing...", Toast.LENGTH_SHORT).show()
                                retailerViewModel.refreshOrders()
                                isRefreshing = false
                                Toast.makeText(
                                    context,
                                    "✅ Refreshed! ${orders.size} orders",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh Orders")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text(
                            "Active (${activeOrders.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text(
                            "History (${historyOrders.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
            }
            
            // Content
            val displayOrders = if (selectedTab == 0) activeOrders else historyOrders
            
            if (displayOrders.isEmpty()) {
                EmptyOrdersView(
                    isActive = selectedTab == 0,
                    onNavigateBack = onNavigateBack,
                    retailerViewModel = retailerViewModel
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayOrders, key = { it.id }) { order ->
                        OrderCard(order = order)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Order number and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.id.takeLast(8).uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDate(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                OrderStatusChip(status = order.status)
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // Items
            Text(
                text = "Items (${order.items.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.flavorName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Qty: ${item.quantity} × ₹${String.format("%.2f", item.discountedPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "₹${String.format("%.2f", item.subtotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Amount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${String.format("%.2f", order.totalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Discount info if applicable
            if (order.discountAmount > 0) {
                Text(
                    text = "You saved ₹${String.format("%.2f", order.discountAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        OrderStatus.PENDING -> Triple(
            Color(0xFFFFF4E5),
            Color(0xFFFF9800),
            "Pending"
        )
        OrderStatus.CONFIRMED -> Triple(
            Color(0xFFE3F2FD),
            Color(0xFF2196F3),
            "Confirmed"
        )
        OrderStatus.PACKED, OrderStatus.PREPARING -> Triple(
            Color(0xFFFFF9C4),
            Color(0xFFFBC02D),
            "Packed"
        )
        OrderStatus.READY -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF66BB6A),
            "Ready"
        )
        OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DISPATCHED -> Triple(
            Color(0xFFE1F5FE),
            Color(0xFF03A9F4),
            "Out for Delivery"
        )
        OrderStatus.DELIVERED, OrderStatus.COMPLETED -> Triple(
            Color(0xFFC8E6C9),
            Color(0xFF4CAF50),
            "Delivered"
        )
        OrderStatus.CANCELLED, OrderStatus.CANCELLED_ADMIN -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFE53935),
            "Cancelled"
        )
        OrderStatus.REJECTED -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFE53935),
            "Rejected"
        )
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun EmptyOrdersView(
    isActive: Boolean,
    onNavigateBack: () -> Unit,
    retailerViewModel: RetailerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDebugRefreshing by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.List else Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isActive) "No active orders" else "No order history",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isActive) 
                    "Your active orders will appear here once you place an order."
                else 
                    "Your completed and cancelled orders will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Debug refresh button
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isDebugRefreshing = true
                        Toast.makeText(context, "🔍 Debug: Force refreshing orders...", Toast.LENGTH_SHORT).show()
                        retailerViewModel.refreshOrders()
                        kotlinx.coroutines.delay(1000) // Wait for refresh
                        val orders = retailerViewModel.myOrders.value
                        Toast.makeText(
                            context,
                            "🔍 Debug Result: ${orders.size} orders in ViewModel",
                            Toast.LENGTH_LONG
                        ).show()
                        isDebugRefreshing = false
                    }
                },
                enabled = !isDebugRefreshing
            ) {
                if (isDebugRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Debugging...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Debug: Force Refresh")
                }
            }
            
            if (isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(onClick = onNavigateBack) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Shopping")
                }
            }
        }
    }
}
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
