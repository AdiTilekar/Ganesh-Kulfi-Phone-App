package com.ganeshkulfi.app.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.presentation.viewmodel.AdminViewModel
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAnalyticsScreen(
    onBackClick: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val retailers by viewModel.retailers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with Quick Stats
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📈 Business Overview",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time performance metrics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Revenue Chart Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Revenue Performance",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Today vs Total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Revenue comparison bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RevenueBarCard(
                            modifier = Modifier.weight(1f),
                            title = "Today's Sales",
                            value = dashboardStats.todaySales,
                            total = dashboardStats.totalRevenue,
                            color = MaterialTheme.colorScheme.primary
                        )
                        RevenueBarCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Revenue",
                            value = dashboardStats.totalRevenue,
                            total = dashboardStats.totalRevenue,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // Inventory Health with Circular Progress
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Inventory Health",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Stock distribution analysis",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val totalStock = inventory.sumOf { it.totalStock }
                    val availableStock = inventory.sumOf { it.availableStock }
                    val soldStock = inventory.sumOf { it.soldQuantity }
                    val stockValue = inventory.sumOf { it.availableStock * it.sellingPrice }
                    
                    // Safe totals with minimum value of 1 to prevent division by zero in UI
                    val safeTotalStock = if (totalStock > 0) totalStock else 1
                    val safeInventorySize = if (inventory.isNotEmpty()) inventory.size else 1
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CircularMetric(
                            value = availableStock,
                            total = safeTotalStock,
                            label = "Available",
                            color = MaterialTheme.colorScheme.primary
                        )
                        CircularMetric(
                            value = soldStock,
                            total = safeTotalStock,
                            label = "Sold",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        CircularMetric(
                            value = dashboardStats.lowStockItems,
                            total = safeInventorySize,
                            label = "Low Stock",
                            color = MaterialTheme.colorScheme.error,
                            showPercentage = false
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Divider()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Stock metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricColumn("Total Items", "${inventory.size}")
                        MetricColumn("Stock Value", "₹${String.format("%.0f", stockValue)}")
                        MetricColumn("Total Units", "$totalStock")
                    }
                }
            }

            // Retailer Performance
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Retailer Analytics",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Network performance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val totalOutstanding = retailers.sumOf { it.totalOutstanding }
                    val activeRetailers = retailers.count { it.isActive }
                    val activePercentage = if (retailers.isNotEmpty()) (activeRetailers.toFloat() / retailers.size) else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsStatCard(
                            modifier = Modifier.weight(1f),
                            value = "${retailers.size}",
                            label = "Total Retailers",
                            icon = Icons.Default.Groups,
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        AnalyticsStatCard(
                            modifier = Modifier.weight(1f),
                            value = "$activeRetailers",
                            label = "Active",
                            subtitle = "${String.format("%.0f", activePercentage * 100)}%",
                            icon = Icons.Default.CheckCircle,
                            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Total Outstanding",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                "₹${String.format("%.0f", totalOutstanding)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Top Selling Items with Visual Ranking
            Card {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Top Performers",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Best selling products",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val topItems = inventory
                        .sortedByDescending { it.soldQuantity }
                        .take(5)
                    val maxSold = topItems.maxOfOrNull { it.soldQuantity } ?: 1
                    
                    if (topItems.isEmpty()) {
                        // Show empty state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No sales data yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Start selling to see top performers",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        topItems.forEachIndexed { index, item ->
                            TopSellingItem(
                                rank = index + 1,
                                name = item.flavorName,
                                price = item.sellingPrice,
                                soldQuantity = item.soldQuantity,
                                maxSold = maxSold,
                                color = when (index) {
                                    0 -> MaterialTheme.colorScheme.primary
                                    1 -> MaterialTheme.colorScheme.secondary
                                    2 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                            if (index < topItems.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            // Profit Analysis with Visual Breakdown
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Profit Analysis",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Financial performance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val totalProfit = inventory.sumOf { it.totalProfit }
                    val totalCost = inventory.sumOf { it.soldQuantity * it.costPrice }
                    val profitMargin = if (totalCost > 0) (totalProfit / totalCost) * 100 else 0.0
                    val avgProfitPerItem = if (inventory.isNotEmpty() && totalProfit > 0) totalProfit / inventory.size else 0.0

                    if (inventory.isEmpty() || totalProfit == 0.0) {
                        // Show empty state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No profit data yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Sales will generate profit analysis",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProfitMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "Total Profit",
                                value = "₹${String.format("%.0f", totalProfit)}",
                                icon = Icons.Default.TrendingUp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            ProfitMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "Margin",
                                value = "${String.format("%.1f", profitMargin)}%",
                                icon = Icons.Default.Percent,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Average profit per item
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Average Profit per Item",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "₹${String.format("%.0f", avgProfitPerItem)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// New composables for modern visualizations

@Composable
fun RevenueBarCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Double,
    total: Double,
    color: Color
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
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "₹${String.format("%.0f", value)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = if (total > 0) min((value / total).toFloat(), 1f) else 0f,
                modifier = Modifier.fillMaxWidth(),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun CircularMetric(
    value: Int,
    total: Int,
    label: String,
    color: Color,
    showPercentage: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            val progress = if (total > 0) (value.toFloat() / total) else 0f
            val displayText = if (showPercentage) {
                if (total > 0) "${(progress * 100).toInt()}%" else "0%"
            } else {
                "$value"
            }
            
            Canvas(modifier = Modifier.size(80.dp)) {
                // Background circle
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
                // Progress arc
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
            }
            
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AnalyticsStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TopSellingItem(
    rank: Int,
    name: String,
    price: Double,
    soldQuantity: Int,
    maxSold: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank badge
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(32.dp)) {
                        drawCircle(color = color.copy(alpha = 0.2f))
                    }
                    Text(
                        text = "$rank",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹${String.format("%.0f", price)} per unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = "$soldQuantity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Progress bar
        LinearProgressIndicator(
            progress = if (maxSold > 0) (soldQuantity.toFloat() / maxSold) else 0f,
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun ProfitMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
