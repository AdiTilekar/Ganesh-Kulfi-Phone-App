package com.ganeshkulfi.app.presentation.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.data.remote.ApiService
import com.ganeshkulfi.app.data.repository.AuthRepository
import com.ganeshkulfi.app.presentation.viewmodel.AdminViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    onBackClick: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Get auth token directly from SharedPreferences - NO remember() so it reads fresh every time
    // IMPORTANT: Use the SAME prefs name as AuthRepository ("kulfi_shared_prefs")
    val prefs = context.getSharedPreferences("kulfi_shared_prefs", android.content.Context.MODE_PRIVATE)
    val authToken = prefs.getString("auth_token", null)
    
    // Debug logging
    SideEffect {
        if (authToken != null) {
        } else {
            val allKeys = prefs.all.keys
        }
    }
    
    if (authToken == null || authToken.isBlank()) {
        // Show error if no token
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Orders Management") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Authentication Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your session has expired or you're not logged in. Please log in again to view orders.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "💡 Tip:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Go back and log in with:\nadmin@ganeshkulfi.com\nPassword: Admin@123",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Button(onClick = onBackClick) {
                        Text("Go Back")
                    }
                }
            }
        }
        return
    }
    
    
    // Collect orders from ViewModel - with explicit initial value
    val backendOrders by viewModel.orders.collectAsState(initial = emptyList())
    val isLoading by viewModel.ordersLoading.collectAsState(initial = false)
    val errorMessage by viewModel.ordersError.collectAsState(initial = null)
    
    // Debug logging - use SideEffect to ensure it runs on every recomposition
    SideEffect {
        backendOrders.forEachIndexed { index, order ->
        }
    }
    
    // Convert backend Order DTOs to UI OrderInfo models - directly depend on backendOrders
    val orders = remember(backendOrders) {
        val converted = backendOrders.map { order ->
            OrderInfo(
                orderId = order.id,              // Use UUID for API calls
                orderNumber = order.orderNumber,  // Keep for display
                customerName = order.retailerName ?: "Retailer",
                shopName = order.shopName,
                items = order.items ?: emptyList(),  // Pass full item list
                total = order.totalAmount,
                status = order.status,
                timestamp = parseTimestamp(order.createdAt)
            )
        }
        converted
    }
    
    // Separate active orders from completed/delivered orders
    val activeOrders = remember(orders) {
        orders.filter { 
            val status = it.status.lowercase()
            status != "completed" && status != "delivered" && status != "cancelled"
        }
    }
    
    val completedOrders = remember(orders) {
        orders.filter { 
            val status = it.status.lowercase()
            status == "completed" || status == "delivered" || status == "cancelled"
        }
    }
    
    // Tab selection state
    var selectedTab by remember { mutableStateOf(0) }
    
    // Search and filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    val statusFilters = listOf("All", "PENDING", "CONFIRMED", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED", "REJECTED", "CANCELLED")
    
    // Fetch orders on first load
    LaunchedEffect(Unit) {
        viewModel.fetchOrders(authToken)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders Management") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs for Active Orders and Order History
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Orders")
                            if (activeOrders.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        "${activeOrders.size}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Order History")
                            if (completedOrders.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        "${completedOrders.size}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                )
            }
            
            // Search and Filter Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search TextField
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search orders...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, "Search", modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    // Filter Button
                    Box {
                        FilterChip(
                            selected = selectedStatusFilter != "All",
                            onClick = { showFilterMenu = true },
                            label = { 
                                Text(
                                    if (selectedStatusFilter == "All") "Filter" else selectedStatusFilter,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            statusFilters.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status) },
                                    onClick = {
                                        selectedStatusFilter = status
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        if (selectedStatusFilter == status) {
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Content based on selected tab
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val tabOrders = if (selectedTab == 0) activeOrders else completedOrders
                
                // Apply search and filter
                val displayOrders = remember(tabOrders, searchQuery, selectedStatusFilter) {
                    var filtered = tabOrders
                    
                    // Apply search
                    if (searchQuery.isNotBlank()) {
                        filtered = filtered.filter { order ->
                            order.orderNumber.contains(searchQuery, ignoreCase = true) ||
                            order.customerName.contains(searchQuery, ignoreCase = true) ||
                            order.shopName?.contains(searchQuery, ignoreCase = true) == true
                        }
                    }
                    
                    // Apply status filter
                    if (selectedStatusFilter != "All") {
                        filtered = filtered.filter { order ->
                            order.status.equals(selectedStatusFilter, ignoreCase = true)
                        }
                    }
                    
                    filtered
                }
                
                if (displayOrders.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                if (selectedTab == 0) Icons.Default.ShoppingBag else Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (selectedTab == 0) "No Active Orders" else "No Order History",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (selectedTab == 0) 
                                    "New orders will appear here when customers place them"
                                else 
                                    "Completed and delivered orders will appear here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            val currentError = errorMessage
                            if (currentError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Error:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = currentError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Manual Refresh Button
                    Button(
                        onClick = {
                            viewModel.fetchOrders(authToken)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh Orders")
                    }
                    
                    // Debug Info
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                "Debug Info:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Backend: http://10.242.116.68:8080",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Token: ${if (authToken.length > 20) "Valid (${authToken.length} chars)" else "Missing or Invalid"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Backend Orders: ${backendOrders.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "UI Orders: ${orders.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Loading: $isLoading",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        } else {
            // Show orders based on selected tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (selectedTab == 0) "Active Orders Summary" else "History Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                if (selectedTab == 0) {
                                    SummaryItem(
                                        label = "Active",
                                        value = activeOrders.size.toString(),
                                        icon = Icons.Default.ShoppingBag
                                    )
                                    SummaryItem(
                                        label = "Pending",
                                        value = activeOrders.count { it.status.lowercase() == "pending" }.toString(),
                                        icon = Icons.Default.PendingActions
                                    )
                                    SummaryItem(
                                        label = "In Progress",
                                        value = activeOrders.count { 
                                            val s = it.status.lowercase()
                                            s == "confirmed" || s == "packed" || s == "out_for_delivery"
                                        }.toString(),
                                        icon = Icons.Default.LocalShipping
                                    )
                                } else {
                                    SummaryItem(
                                        label = "Total",
                                        value = completedOrders.size.toString(),
                                        icon = Icons.Default.History
                                    )
                                    SummaryItem(
                                        label = "Delivered",
                                        value = completedOrders.count { 
                                            val s = it.status.lowercase()
                                            s == "completed" || s == "delivered"
                                        }.toString(),
                                        icon = Icons.Default.CheckCircle
                                    )
                                    SummaryItem(
                                        label = "Cancelled",
                                        value = completedOrders.count { it.status.lowercase() == "cancelled" }.toString(),
                                        icon = Icons.Default.Cancel
                                    )
                                }
                            }
                        }
                    }
                }

                // Orders List Header
                item {
                    Text(
                        if (selectedTab == 0) "All Active Orders" else "Order History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Orders
                items(displayOrders) { order ->
                    OrderCard(
                        order = order,
                        onConfirm = { 
                            viewModel.updateOrderStatus(order.orderId, "confirmed", authToken)
                        },
                        onCancel = { 
                            viewModel.cancelOrder(order.orderId, "Cancelled by admin", authToken)
                        },
                        onPack = { 
                            viewModel.updateOrderStatus(order.orderId, "packed", authToken)
                        },
                        onOutForDelivery = { 
                            viewModel.updateOrderStatus(order.orderId, "out_for_delivery", authToken)
                        },
                        onDeliver = { 
                            viewModel.updateOrderStatus(order.orderId, "delivered", authToken)
                        },
                        isLoading = isLoading
                    )
                }
            }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderCard(
    order: OrderInfo,
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {},
    onPack: () -> Unit = {},
    onOutForDelivery: () -> Unit = {},
    onDeliver: () -> Unit = {},
    isLoading: Boolean = false
) {
    var showDetailsDialog by remember { mutableStateOf(false) }

    Card(
        onClick = { showDetailsDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Retailer name first - highlighted
                    Text(
                        order.customerName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!order.shopName.isNullOrBlank()) {
                        Text(
                            order.shopName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Order number below - less prominent
                    Text(
                        order.orderNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                StatusChip(status = order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Show order items with quantities
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${item.productName} × ${item.quantity}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            "₹${String.format("%.2f", item.lineTotal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total Amount",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "₹${String.format("%.2f", order.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    formatTime(order.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action Buttons
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (order.status.lowercase()) {
                    "pending" -> {
                        Button(
                            onClick = {
                                onConfirm()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("Confirm")
                        }
                        OutlinedButton(
                            onClick = {
                                onCancel()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("Cancel")
                        }
                    }
                    "confirmed" -> {
                        Button(
                            onClick = onPack,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Inventory, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("Mark as Packed")
                        }
                    }
                    "packed" -> {
                        Button(
                            onClick = onOutForDelivery,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("Out for Delivery")
                        }
                    }
                    "out_for_delivery" -> {
                        Button(
                            onClick = onDeliver,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("Mark as Delivered")
                        }
                    }
                    "completed", "delivered" -> {
                        // No actions for completed orders
                    }
                    else -> {
                        // Unknown status, show no actions
                    }
                }
            }
        }
    }

    if (showDetailsDialog) {
        OrderDetailsDialog(
            order = order,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

@Composable
private fun StatusChip(status: String) {
    val (containerColor, contentColor) = when (status) {
        "Pending" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "Processing" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "Completed" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OrderDetailsDialog(
    order: OrderInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Receipt, contentDescription = null)
        },
        title = {
            Text("Order Details")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow("Order Number", order.orderNumber)
                DetailRow("Customer", order.customerName)
                if (!order.shopName.isNullOrBlank()) {
                    DetailRow("Shop Name", order.shopName)
                }
                
                Divider()
                Text(
                    "Order Items",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                
                order.items.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                item.productName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            DetailRow("Quantity", "${item.quantity} pcs")
                            DetailRow("Unit Price", "₹${String.format("%.2f", item.unitPrice)}")
                            if (item.discountPercent > 0) {
                                DetailRow("Discount", "${String.format("%.1f", item.discountPercent)}%")
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            DetailRow(
                                "Line Total",
                                "₹${String.format("%.2f", item.lineTotal)}",
                                isBold = true
                            )
                        }
                    }
                }
                
                Divider()
                DetailRow(
                    "Total Amount",
                    "₹${String.format("%.2f", order.total)}",
                    isBold = true
                )
                DetailRow("Status", order.status)
                DetailRow("Order Time", formatFullTime(order.timestamp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hours ago"
        else -> "$days days ago"
    }
}

private fun parseTimestamp(dateString: String): Long {
    return try {
        // Try to parse ISO 8601 format (e.g., "2025-12-30T12:34:56.789Z")
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.parse(dateString)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        // Fallback to current time if parsing fails
        System.currentTimeMillis()
    }
}

private fun formatFullTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Data class for order information
private data class OrderInfo(
    val orderId: String,           // UUID for API calls
    val orderNumber: String,       // Display number like "ORD-001"
    val customerName: String,
    val shopName: String?,
    val items: List<com.ganeshkulfi.app.data.remote.OrderItem>,  // Full item details
    val total: Double,
    val status: String,
    val timestamp: Long
)
