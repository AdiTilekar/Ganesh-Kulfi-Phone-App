package com.ganeshkulfi.app.presentation.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.presentation.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToInventory: () -> Unit,
    onNavigateToRetailers: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRetailerView: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                onNavigateToInventory = {
                    scope.launch { drawerState.close() }
                    onNavigateToInventory()
                },
                onNavigateToPricing = {
                    scope.launch { drawerState.close() }
                    onNavigateToPricing()
                },
                onSignOut = {
                    scope.launch { drawerState.close() }
                    onSignOut()
                }
            )
        }
    ) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Admin Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Shree Ganesh Kulfi",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            Icons.Default.Menu,
                            "Menu",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    // Customer View Preview
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.Home,
                            "Customer View",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    // Retailer View Preview
                    IconButton(onClick = onNavigateToRetailerView) {
                        Icon(
                            Icons.Default.Store,
                            "Retailer View",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Modern Welcome Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Good ${getGreeting()}, Owner! 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = getCurrentDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Quick Stats in Welcome Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WelcomeStatItem(
                                value = "${dashboardStats.pendingOrders}",
                                label = "Pending",
                                icon = Icons.Default.PendingActions
                            )
                            WelcomeStatItem(
                                value = "₹${String.format("%.0f", dashboardStats.todaySales)}",
                                label = "Today",
                                icon = Icons.Default.TrendingUp
                            )
                            WelcomeStatItem(
                                value = "${dashboardStats.activeRetailers}",
                                label = "Retailers",
                                icon = Icons.Default.Store
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Business Metrics Section
                Text(
                    text = "Business Metrics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Stats Grid - 2x2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernStatsCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Revenue",
                        value = "₹${String.format("%.0f", dashboardStats.totalRevenue)}",
                        icon = Icons.Default.AccountBalance,
                        iconColor = MaterialTheme.colorScheme.primary
                    )
                    
                    ModernStatsCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Stock",
                        value = "${dashboardStats.totalStock}",
                        subtitle = "units",
                        icon = Icons.Default.Inventory,
                        iconColor = MaterialTheme.colorScheme.tertiary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernStatsCard(
                        modifier = Modifier.weight(1f),
                        title = "Low Stock Alert",
                        value = "${dashboardStats.lowStockItems}",
                        subtitle = "items",
                        icon = Icons.Default.Warning,
                        iconColor = if (dashboardStats.lowStockItems > 0) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.outline
                    )
                    
                    ModernStatsCard(
                        modifier = Modifier.weight(1f),
                        title = "Pending Payments",
                        value = "${dashboardStats.pendingPayments}",
                        icon = Icons.Default.PendingActions,
                        iconColor = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Actions
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                QuickActionCard(
                    title = "Manage Retailers",
                    subtitle = "View retailers, track payments, give stock",
                    icon = Icons.Default.Groups,
                    onClick = onNavigateToRetailers
                )

                QuickActionCard(
                    title = "View Orders",
                    subtitle = "Process orders, update status",
                    icon = Icons.Default.ShoppingBag,
                    onClick = onNavigateToOrders
                )

                QuickActionCard(
                    title = "Reports & Analytics",
                    subtitle = "Sales reports, inventory analysis",
                    icon = Icons.Default.Analytics,
                    onClick = onNavigateToReports
                )
            }
        }
    }
    }
}

// Helper functions for modern UI
private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "morning"
        in 12..16 -> "afternoon"
        else -> "evening"
    }
}

private fun getCurrentDate(): String {
    val dateFormat = java.text.SimpleDateFormat("EEEE, MMM d, yyyy", java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date())
}

@Composable
fun WelcomeStatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ModernStatsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
                
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(28.dp),
                    tint = iconColor
                )
            }
        }
    }
}

@Composable
fun StatsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp)
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdminDrawerContent(
    onNavigateToInventory: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onSignOut: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .padding(16.dp)
        ) {
            // Drawer Header
            Column(
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Icon(
                    Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Admin Panel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Shree Ganesh Kulfi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Quick Actions Section
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Manage Inventory
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                label = { 
                    Column {
                        Text("Manage Inventory", fontWeight = FontWeight.Medium)
                        Text(
                            "Update stock & flavors",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                selected = false,
                onClick = onNavigateToInventory,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            // Pricing Management
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                label = { 
                    Column {
                        Text("Pricing Management", fontWeight = FontWeight.Medium)
                        Text(
                            "Set pricing tiers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                selected = false,
                onClick = onNavigateToPricing,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Logout Button
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                label = { 
                    Text(
                        "Sign Out",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                selected = false,
                onClick = onSignOut,
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
