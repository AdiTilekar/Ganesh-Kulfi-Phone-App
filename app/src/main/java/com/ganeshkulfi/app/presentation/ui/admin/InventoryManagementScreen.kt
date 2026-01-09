package com.ganeshkulfi.app.presentation.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ganeshkulfi.app.data.model.InventoryItem
import com.ganeshkulfi.app.presentation.viewmodel.AdminViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val inventoryItems by viewModel.inventory.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }
    
    val filterOptions = listOf("All", "In Stock", "Low Stock", "Out of Stock")

    // Filter items based on search query and filter
    val filteredItems by remember {
        derivedStateOf {
            var filtered = inventoryItems
            
            // Apply search
            if (searchQuery.isNotBlank()) {
                filtered = inventoryItems.filter { item ->
                    item.flavorName.contains(searchQuery, ignoreCase = true) ||
                    item.flavorId.contains(searchQuery, ignoreCase = true)
                }
            }
            
            // Apply stock filter
            when (selectedFilter) {
                "Low Stock" -> filtered.filter { it.availableStock > 0 && it.availableStock <= 10 }
                "Out of Stock" -> filtered.filter { it.availableStock == 0 }
                "In Stock" -> filtered.filter { it.availableStock > 10 }
                else -> filtered
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Stock")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onClearSearch = { searchQuery = "" }
            )
            
            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions.size) { index ->
                    val filter = filterOptions[index]
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                filter,
                                fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (selectedFilter == filter) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Summary Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Value",
                            value = "₹${String.format("%.0f", inventoryItems.sumOf { it.totalStock * it.costPrice })}",
                            icon = Icons.Default.Payments
                        )
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Items",
                            value = "${filteredItems.size}/${inventoryItems.size}",
                            icon = Icons.Default.Inventory
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "All Items" else "Search Results",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = "${filteredItems.size} found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (filteredItems.isEmpty()) {
                    item {
                        EmptySearchResult(searchQuery)
                    }
                } else {
                    items(filteredItems) { item ->
                        InventoryItemCard(
                            item = item,
                            onUpdateStock = { selectedItem = item }
                        )
                    }
                }
            }
        }
    }

    // Update Stock Dialog
    if (selectedItem != null) {
        UpdateStockDialog(
            item = selectedItem!!,
            onDismiss = { selectedItem = null },
            onUpdate = { quantity ->
                viewModel.updateStock(selectedItem!!.flavorId, quantity)
                selectedItem = null
            }
        )
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search by flavor name...") },
            leadingIcon = {
                Icon(Icons.Default.Search, "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        onClearSearch()
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Clear, "Clear")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun EmptySearchResult(searchQuery: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = "No results",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No results found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "No items match \"$searchQuery\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(icon, title, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onUpdateStock: () -> Unit
) {
    val context = LocalContext.current
    val imageResId = remember(item.flavorId) {
        val imageName = "${item.flavorId}_kulfi"
        context.resources.getIdentifier(
            imageName,
            "drawable",
            context.packageName
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onUpdateStock
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Kulfi Image
            Card(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (imageResId != 0) {
                    AsyncImage(
                        model = imageResId,
                        contentDescription = item.flavorName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Icecream,
                            contentDescription = item.flavorName,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Item Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.flavorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${item.sellingPrice.toInt()} per unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stock Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactStockInfo("Total", item.totalStock.toString())
                    CompactStockInfo("Available", item.availableStock.toString())
                    CompactStockInfo("Given", item.stockGivenToRetailers.toString())
                }

                if (item.needsRestock) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            "Low Stock",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Low Stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Edit Icon
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Top),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CompactStockInfo(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StockInfo(label: String, value: String) {
    Column {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateStockDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onUpdate: (Int) -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    var isAddMode by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val imageResId = remember(item.flavorId) {
        val imageName = "${item.flavorId}_kulfi"
        context.resources.getIdentifier(
            imageName,
            "drawable",
            context.packageName
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            if (imageResId != 0) {
                Card(
                    modifier = Modifier.size(60.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    AsyncImage(
                        model = imageResId,
                        contentDescription = item.flavorName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Icon(
                    Icons.Default.Inventory,
                    contentDescription = "Update Stock",
                    modifier = Modifier.size(48.dp)
                )
            }
        },
        title = { 
            Text(
                "Update Stock",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Flavor Name
                Text(
                    text = item.flavorName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Divider()
                
                // Current Stock Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Current Stock",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${item.totalStock} units",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            "Available",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${item.availableStock} units",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.needsRestock) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Add/Subtract Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isAddMode,
                        onClick = { isAddMode = true },
                        label = { Text("Add Stock") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isAddMode,
                        onClick = { isAddMode = false },
                        label = { Text("Subtract Stock") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            quantity = it
                        }
                    },
                    label = { Text(if (isAddMode) "Add Quantity" else "Subtract Quantity") },
                    placeholder = { Text(if (isAddMode) "Enter quantity to add" else "Enter quantity to remove") },
                    leadingIcon = {
                        Icon(
                            if (isAddMode) Icons.Default.Add else Icons.Default.Remove,
                            if (isAddMode) "Add" else "Subtract"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isAddMode) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                )
                
                // Preview of new stock
                if (quantity.isNotEmpty()) {
                    val changeAmount = quantity.toIntOrNull() ?: 0
                    val newTotal = if (isAddMode) {
                        item.totalStock + changeAmount
                    } else {
                        item.totalStock - changeAmount
                    }
                    val newAvailable = if (isAddMode) {
                        item.availableStock + changeAmount
                    } else {
                        item.availableStock - changeAmount
                    }
                    
                    val isValid = newTotal >= 0 && newAvailable >= 0
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isValid) {
                                if (isAddMode) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "New Total Stock:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "$newTotal units",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isValid) {
                                        if (isAddMode) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "New Available:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "$newAvailable units",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isValid) {
                                        if (isAddMode) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                            
                            if (!isValid) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "⚠️ Invalid: Stock cannot be negative",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    quantity.toIntOrNull()?.let { amt ->
                        if (amt > 0) {
                            val changeAmount = if (isAddMode) amt else -amt
                            val newTotal = item.totalStock + changeAmount
                            val newAvailable = item.availableStock + changeAmount
                            if (newTotal >= 0 && newAvailable >= 0) {
                                onUpdate(changeAmount)
                            }
                        }
                    }
                },
                enabled = quantity.toIntOrNull()?.let { amt -> 
                    if (amt > 0) {
                        val changeAmount = if (isAddMode) amt else -amt
                        val newTotal = item.totalStock + changeAmount
                        val newAvailable = item.availableStock + changeAmount
                        newTotal >= 0 && newAvailable >= 0
                    } else false
                } ?: false,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAddMode) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    if (isAddMode) Icons.Default.Add else Icons.Default.Remove,
                    "Confirm",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isAddMode) "Add Stock" else "Subtract Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
