package com.ganeshkulfi.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.data.model.*
import com.ganeshkulfi.app.presentation.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingManagementScreen(
    onBackClick: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val retailers by viewModel.retailers.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    
    var selectedRetailer by remember { mutableStateOf<Retailer?>(null) }
    var showPricingDialog by remember { mutableStateOf(false) }
    var selectedFlavor by remember { mutableStateOf<InventoryItem?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Pricing Management") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                
                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Retailer Pricing") },
                        icon = { Icon(Icons.Default.Store, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Base Prices") },
                        icon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> RetailerPricingTab(
                retailers = retailers,
                padding = padding,
                onRetailerClick = { retailer ->
                    selectedRetailer = retailer
                    showPricingDialog = true
                },
                onChangeTier = { retailerId, tier ->
                    viewModel.updateRetailerPricingTier(retailerId, tier)
                }
            )
            1 -> BasePricesTab(
                inventory = inventory,
                padding = padding,
                onUpdatePrice = { flavorId, costPrice, sellingPrice ->
                    viewModel.updateItemPrice(flavorId, costPrice, sellingPrice)
                }
            )
        }
    }

    // Pricing Dialog
    if (showPricingDialog && selectedRetailer != null) {
        RetailerPricingDialog(
            retailer = selectedRetailer!!,
            inventory = inventory,
            onDismiss = { showPricingDialog = false },
            onSetCustomPrice = { flavorId, price, discount, minQty ->
                viewModel.setCustomPrice(selectedRetailer!!.id, flavorId, price, discount, minQty)
            },
            onRemoveCustomPrice = { flavorId ->
                viewModel.removeCustomPrice(selectedRetailer!!.id, flavorId)
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun RetailerPricingTab(
    retailers: List<Retailer>,
    padding: PaddingValues,
    onRetailerClick: (Retailer) -> Unit,
    onChangeTier: (String, PricingTier) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Manage Retailer Pricing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Set pricing tiers or custom prices for each retailer",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Pricing Tiers Legend
        item {
            Text(
                "Available Pricing Tiers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            PricingTiersLegend()
        }

        // Retailers List
        item {
            Text(
                "Retailers (${retailers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(retailers) { retailer ->
            RetailerPricingCard(
                retailer = retailer,
                onClick = { onRetailerClick(retailer) },
                onChangeTier = { tier -> onChangeTier(retailer.id, tier) }
            )
        }
    }
}

@Composable
fun BasePricesTab(
    inventory: List<InventoryItem>,
    padding: PaddingValues,
    onUpdatePrice: (String, Double, Double) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Update Base Prices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Click on any item to update cost and selling prices",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Items Count
        item {
            Text(
                "All Items (${inventory.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(inventory) { item ->
            BasePriceCard(
                item = item,
                onUpdatePrice = { costPrice, sellingPrice ->
                    onUpdatePrice(item.flavorId, costPrice, sellingPrice)
                }
            )
        }
    }
}

@Composable
fun PricingTiersLegend() {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            PricingTier.values().forEach { tier ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = when (tier) {
                                PricingTier.GOLD -> MaterialTheme.colorScheme.primary
                                PricingTier.SILVER -> MaterialTheme.colorScheme.secondary
                                PricingTier.BASIC -> MaterialTheme.colorScheme.tertiary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            tier.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        "${tier.discountPercentage.toInt()}% OFF",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    tier.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerPricingCard(
    retailer: Retailer,
    onClick: () -> Unit,
    onChangeTier: (PricingTier) -> Unit
) {
    var showTierMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        retailer.shopName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        retailer.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit pricing",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Tier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = when (retailer.pricingTier) {
                            PricingTier.GOLD -> MaterialTheme.colorScheme.primary
                            PricingTier.SILVER -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            retailer.pricingTier.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${retailer.pricingTier.discountPercentage.toInt()}% discount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Change Tier Button
                Box {
                    FilledTonalButton(onClick = { showTierMenu = true }) {
                        Text("Change Tier")
                        Icon(Icons.Default.ArrowDropDown, "")
                    }

                    DropdownMenu(
                        expanded = showTierMenu,
                        onDismissRequest = { showTierMenu = false }
                    ) {
                        PricingTier.values().forEach { tier ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(tier.displayName)
                                        Text(
                                            "${tier.discountPercentage.toInt()}% OFF",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    onChangeTier(tier)
                                    showTierMenu = false
                                },
                                leadingIcon = {
                                    if (tier == retailer.pricingTier) {
                                        Icon(Icons.Default.Check, "Current tier")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RetailerPricingDialog(
    retailer: Retailer,
    inventory: List<InventoryItem>,
    onDismiss: () -> Unit,
    onSetCustomPrice: (String, Double, Double, Int) -> Unit,
    onRemoveCustomPrice: (String) -> Unit,
    viewModel: AdminViewModel
) {
    var selectedFlavor by remember { mutableStateOf<InventoryItem?>(null) }
    var customPrice by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("0") }
    var minQuantity by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Custom Pricing for")
                Text(
                    retailer.shopName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Current Tier: ${retailer.pricingTier.displayName} (${retailer.pricingTier.discountPercentage.toInt()}% OFF)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Flavor Prices",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(inventory) { flavor ->
                    val priceBreakdown = viewModel.getPriceBreakdown(
                        retailer,
                        flavor.flavorId,
                        flavor.flavorName,
                        flavor.sellingPrice,
                        1
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (priceBreakdown.isCustomPrice)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    flavor.flavorName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (priceBreakdown.isCustomPrice) {
                                    AssistChip(
                                        onClick = { onRemoveCustomPrice(flavor.flavorId) },
                                        label = { Text("Remove") },
                                        leadingIcon = { Icon(Icons.Default.Close, "") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Base: ₹${priceBreakdown.basePrice}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Retailer: ₹${String.format("%.2f", priceBreakdown.retailerPrice)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    "${String.format("%.1f", priceBreakdown.discountPercentage)}% OFF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (priceBreakdown.isCustomPrice) {
                                Text(
                                    "Custom Pricing Active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasePriceCard(
    item: InventoryItem,
    onUpdatePrice: (costPrice: Double, sellingPrice: Double) -> Unit
) {
    var showPriceDialog by remember { mutableStateOf(false) }

    Card(
        onClick = { showPriceDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.flavorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            "Cost Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "₹${String.format("%.0f", item.costPrice)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column {
                        Text(
                            "Selling Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "₹${String.format("%.0f", item.sellingPrice)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            "Profit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "₹${String.format("%.0f", item.sellingPrice - item.costPrice)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit price",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showPriceDialog) {
        UpdatePriceDialog(
            item = item,
            onDismiss = { showPriceDialog = false },
            onUpdatePrice = { costPrice, sellingPrice ->
                onUpdatePrice(costPrice, sellingPrice)
                showPriceDialog = false
            }
        )
    }
}

@Composable
fun UpdatePriceDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onUpdatePrice: (costPrice: Double, sellingPrice: Double) -> Unit
) {
    var costPriceText by remember { mutableStateOf(item.costPrice.toString()) }
    var sellingPriceText by remember { mutableStateOf(item.sellingPrice.toString()) }

    val costPrice = costPriceText.toDoubleOrNull() ?: 0.0
    val sellingPrice = sellingPriceText.toDoubleOrNull() ?: 0.0
    val profit = sellingPrice - costPrice
    val profitMargin = if (costPrice > 0) (profit / costPrice) * 100 else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Update Price")
                Text(
                    item.flavorName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = { costPriceText = it },
                    label = { Text("Cost Price (₹)") },
                    leadingIcon = { Icon(Icons.Default.ShoppingCart, "Cost") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                OutlinedTextField(
                    value = sellingPriceText,
                    onValueChange = { sellingPriceText = it },
                    label = { Text("Selling Price (₹)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, "Selling") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                // Calculation Summary
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Price Summary",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Profit per unit:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "₹${String.format("%.2f", profit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (profit > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Profit Margin:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${String.format("%.1f", profitMargin)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (profitMargin > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (costPrice > 0 && sellingPrice > 0) {
                        onUpdatePrice(costPrice, sellingPrice)
                    }
                },
                enabled = costPrice > 0 && sellingPrice > 0
            ) {
                Text("Update Price")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
