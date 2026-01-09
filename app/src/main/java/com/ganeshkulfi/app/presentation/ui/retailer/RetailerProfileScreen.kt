package com.ganeshkulfi.app.presentation.ui.retailer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.data.model.OrderStatus
import com.ganeshkulfi.app.presentation.viewmodel.AuthViewModel
import com.ganeshkulfi.app.presentation.viewmodel.RetailerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    retailerViewModel: RetailerViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val orders by retailerViewModel.myOrders.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Calculate stats
    val totalOrders = orders.size
    val currentMonth = SimpleDateFormat("MM", Locale.US).format(Date()).toInt()
    val currentYear = SimpleDateFormat("yyyy", Locale.US).format(Date()).toInt()
    val thisMonthOrders = orders.count { order ->
        val orderDate = Date(order.createdAt)
        val orderMonth = SimpleDateFormat("MM", Locale.US).format(orderDate).toInt()
        val orderYear = SimpleDateFormat("yyyy", Locale.US).format(orderDate).toInt()
        orderMonth == currentMonth && orderYear == currentYear
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "My Profile",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Name
                    Text(
                        text = currentUser?.name ?: "Retailer",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    
                    // Shop Name
                    if (!currentUser?.shopName.isNullOrBlank()) {
                        Text(
                            text = currentUser?.shopName ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Tier Badge
                    currentUser?.pricingTier?.let { tier ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = getTierColor(tier.name),
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                text = "${tier.name} TIER",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            // Account Information Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Account Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Info Cards
                InfoCard(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = currentUser?.email ?: "Not available"
                )
                
                if (!currentUser?.phone.isNullOrBlank()) {
                    InfoCard(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = currentUser?.phone ?: ""
                    )
                }
                
                if (!currentUser?.shopName.isNullOrBlank()) {
                    InfoCard(
                        icon = Icons.Default.Store,
                        label = "Shop Name",
                        value = currentUser?.shopName ?: ""
                    )
                }
                
                currentUser?.pricingTier?.let { tier ->
                    InfoCard(
                        icon = Icons.Default.Discount,
                        label = "Discount",
                        value = "${tier.discountPercentage}% OFF"
                    )
                }
                
                // Business Stats (if available)
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "Business Stats",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShoppingCart,
                        label = "Total Orders",
                        value = totalOrders.toString()
                    )
                    
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TrendingUp,
                        label = "This Month",
                        value = thisMonthOrders.toString()
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Help & Support Section
                Text(
                    text = "Help & Support",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                MenuCard(
                    icon = Icons.Default.Help,
                    title = "Help Center",
                    subtitle = "Get help with your orders",
                    onClick = { 
                        scope.launch { 
                            snackbarHostState.showSnackbar("Help Center coming soon!") 
                        }
                    }
                )
                
                MenuCard(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Learn more about Ganesh Kulfi",
                    onClick = { 
                        scope.launch { 
                            snackbarHostState.showSnackbar("About section coming soon!") 
                        }
                    }
                )
                
                Spacer(Modifier.height(32.dp))
                
                // Logout Button
                OutlinedButton(
                    onClick = {
                        authViewModel.signOut()
                        onNavigateToLogin()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 2.dp
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Logout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun InfoCard(
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier
            .heightIn(min = 120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun getTierColor(tierName: String): Color {
    return when (tierName.uppercase()) {
        "GOLD" -> Color(0xFFFFD700)
        "SILVER" -> Color(0xFFC0C0C0)
        "PLATINUM" -> Color(0xFFE5E4E2)
        else -> MaterialTheme.colorScheme.tertiary
    }
}
