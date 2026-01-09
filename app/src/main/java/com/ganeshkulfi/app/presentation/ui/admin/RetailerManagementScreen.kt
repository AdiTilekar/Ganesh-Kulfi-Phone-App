package com.ganeshkulfi.app.presentation.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.data.model.PricingTier
import com.ganeshkulfi.app.data.model.Retailer
import com.ganeshkulfi.app.presentation.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerManagementScreen(
    onNavigateBack: () -> Unit,
    onRetailerClick: (String) -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Get auth token from SharedPreferences - NO remember() so it reads fresh
    // IMPORTANT: Use the SAME prefs name as AuthRepository ("kulfi_shared_prefs")
    val prefs = context.getSharedPreferences("kulfi_shared_prefs", android.content.Context.MODE_PRIVATE)
    val authToken = prefs.getString("auth_token", null) ?: "dummy-token-for-testing"
    
    
    val retailers by viewModel.retailers.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showCredentialsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedRetailer by remember { mutableStateOf<Retailer?>(null) }
    var generatedEmail by remember { mutableStateOf("") }
    var generatedPassword by remember { mutableStateOf("") }
    var createdShopName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTierFilter by remember { mutableStateOf("All") }
    
    val tierFilters = listOf("All", "GOLD", "SILVER", "BRONZE")
    
    // Filter retailers
    val filteredRetailers = remember(retailers, searchQuery, selectedTierFilter) {
        var filtered = retailers
        
        // Apply search
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { retailer ->
                retailer.name.contains(searchQuery, ignoreCase = true) ||
                retailer.shopName.contains(searchQuery, ignoreCase = true) ||
                retailer.id.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // Apply tier filter
        if (selectedTierFilter != "All") {
            filtered = filtered.filter { it.pricingTier.name == selectedTierFilter }
        }
        
        filtered
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Retailer Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.PersonAdd, "Add Retailer")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "${retailers.size}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Total Retailers", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "₹${String.format("%.0f", retailers.sumOf { it.totalOutstanding })}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Outstanding", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search retailers...", style = MaterialTheme.typography.bodySmall) },
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
            }
            
            item {
                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tierFilters.forEach { tier ->
                        FilterChip(
                            selected = selectedTierFilter == tier,
                            onClick = { selectedTierFilter = tier },
                            label = {
                                Text(
                                    tier,
                                    fontWeight = if (selectedTierFilter == tier) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (selectedTierFilter == tier) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    "Active Retailers (${filteredRetailers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(filteredRetailers) { retailer ->
                RetailerCard(
                    retailer = retailer,
                    onClick = { onRetailerClick(retailer.id) },
                    onEdit = {
                        selectedRetailer = retailer
                        showEditDialog = true
                    },
                    onDelete = {
                        selectedRetailer = retailer
                        showDeleteDialog = true
                    }
                )
            }
        }
    }

    // Add Retailer Dialog
    if (showAddDialog) {
        AddRetailerDialog(
            onDismiss = { showAddDialog = false },
            onAddRetailer = { name, shopName, phone, address, email, password, pricingTier ->
                val retailer = Retailer(
                    id = "ret_${System.currentTimeMillis()}",
                    name = name,
                    shopName = shopName,
                    phone = phone,
                    address = address,
                    isActive = true,
                    totalOutstanding = 0.0,
                    creditLimit = 50000.0,
                    pricingTier = pricingTier
                )
                
                // Store credentials to show in success dialog
                generatedEmail = email
                generatedPassword = password
                createdShopName = shopName
                
                // Create retailer account with credentials
                viewModel.addRetailerWithCredentials(retailer, email, password)
                
                showAddDialog = false
                showCredentialsDialog = true
            }
        )
    }

    // Show Credentials Dialog
    if (showCredentialsDialog) {
        RetailerCredentialsDialog(
            shopName = createdShopName,
            email = generatedEmail,
            password = generatedPassword,
            onDismiss = { showCredentialsDialog = false }
        )
    }
    
    // Edit Retailer Dialog
    if (showEditDialog && selectedRetailer != null) {
        EditRetailerDialog(
            retailer = selectedRetailer!!,
            onDismiss = { 
                showEditDialog = false
                selectedRetailer = null
            },
            onSave = { updatedRetailer ->
                viewModel.updateRetailerViaApi(updatedRetailer, authToken)
                showEditDialog = false
                selectedRetailer = null
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && selectedRetailer != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedRetailer = null
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Delete Retailer?") },
            text = {
                Text("Are you sure you want to delete ${selectedRetailer!!.shopName}? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRetailerViaApi(selectedRetailer!!, authToken)
                        showDeleteDialog = false
                        selectedRetailer = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    selectedRetailer = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RetailerCredentialsDialog(
    shopName: String,
    email: String,
    password: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { 
            Text(
                "Retailer Account Created!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Account for $shopName has been successfully created.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Login Credentials",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        // Email
                        Column {
                            Text(
                                "Email",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                email,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Password
                        Column {
                            Text(
                                "Password",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                password,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Please save these credentials. The retailer will need them to log in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRetailerDialog(
    retailer: Retailer,
    onDismiss: () -> Unit,
    onSave: (Retailer) -> Unit
) {
    var name by remember { mutableStateOf(retailer.name) }
    var phone by remember { mutableStateOf(retailer.phone) }
    var shopName by remember { mutableStateOf(retailer.shopName) }
    var address by remember { mutableStateOf(retailer.address) }
    var isActive by remember { mutableStateOf(retailer.isActive) }
    var totalOutstanding by remember { mutableStateOf(retailer.totalOutstanding.toString()) }
    var selectedTier by remember { mutableStateOf(retailer.pricingTier) }
    var expandedTier by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Retailer") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Phone, null) }
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                OutlinedTextField(
                    value = totalOutstanding,
                    onValueChange = { totalOutstanding = it },
                    label = { Text("Outstanding Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("₹", modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                
                // Pricing Tier Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedTier,
                    onExpandedChange = { expandedTier = !expandedTier }
                ) {
                    OutlinedTextField(
                        value = selectedTier.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pricing Tier") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTier) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedTier,
                        onDismissRequest = { expandedTier = false }
                    ) {
                        PricingTier.values().forEach { tier ->
                            DropdownMenuItem(
                                text = { Text(tier.name) },
                                onClick = {
                                    selectedTier = tier
                                    expandedTier = false
                                }
                            )
                        }
                    }
                }

                // Active Status Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Status", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank() && shopName.isNotBlank()) {
                        val updatedRetailer = retailer.copy(
                            name = name,
                            phone = phone,
                            shopName = shopName,
                            address = address,
                            isActive = isActive,
                            totalOutstanding = totalOutstanding.toDoubleOrNull() ?: retailer.totalOutstanding,
                            pricingTier = selectedTier
                        )
                        onSave(updatedRetailer)
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank() && shopName.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerCard(
    retailer: Retailer,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Retailer Icon
            Icon(
                Icons.Default.Store,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Retailer Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = retailer.shopName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = retailer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = retailer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Outstanding Amount
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (retailer.totalOutstanding > 0)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Outstanding: ₹${String.format("%.0f", retailer.totalOutstanding)}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    if (retailer.isActive) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRetailerDialog(
    onDismiss: () -> Unit,
    onAddRetailer: (name: String, shopName: String, phone: String, address: String, email: String, password: String, pricingTier: PricingTier) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf(PricingTier.SILVER) }
    var expandedTier by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Retailer") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Owner Name") },
                    leadingIcon = { Icon(Icons.Default.Person, "Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop Name") },
                    leadingIcon = { Icon(Icons.Default.Store, "Shop") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, "Address") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Pricing Tier Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedTier,
                    onExpandedChange = { expandedTier = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedTier.displayName} (${selectedTier.discountPercentage}% discount)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pricing Tier") },
                        leadingIcon = { Icon(Icons.Default.Star, "Tier") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTier) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedTier,
                        onDismissRequest = { expandedTier = false }
                    ) {
                        listOf(PricingTier.BASIC, PricingTier.SILVER, PricingTier.GOLD).forEach { tier ->
                            DropdownMenuItem(
                                text = { Text("${tier.displayName} (${tier.discountPercentage}% discount)") },
                                onClick = {
                                    selectedTier = tier
                                    expandedTier = false
                                }
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Login credentials will be generated automatically",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "You'll receive the email and password after creation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && shopName.isNotBlank() && phone.isNotBlank()) {
                        // Generate email from shop name
                        val email = shopName.lowercase()
                            .replace(" ", "")
                            .replace(Regex("[^a-z0-9]"), "") + "@ganeshkulfi.com"
                        
                        // Generate secure password that meets backend requirements:
                        // - At least 8 characters
                        // - Contains uppercase, lowercase, and number
                        val randomNum = (100000..999999).random()
                        val password = "Gk${randomNum}@"  // Uppercase G, lowercase k, numbers, special char
                        
                        onAddRetailer(name, shopName, phone, address, email, password, selectedTier)
                    }
                },
                enabled = name.isNotBlank() && shopName.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Create Retailer Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
