package com.ganeshkulfi.app.presentation.ui.retailer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ganeshkulfi.app.data.remote.Product
import com.ganeshkulfi.app.presentation.viewmodel.RetailerViewModel
import com.ganeshkulfi.app.presentation.viewmodel.CartViewModel
import com.ganeshkulfi.app.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ProductCatalogScreen(
    onNavigateToCart: () -> Unit,
    onOpenDrawer: () -> Unit,
    cartViewModel: CartViewModel,
    onNavigateBack: (() -> Unit)? = null,
    isAdminPreview: Boolean = false,
    authViewModel: AuthViewModel = hiltViewModel(),
    retailerViewModel: RetailerViewModel = hiltViewModel()
) {
    val products by retailerViewModel.availableProducts.collectAsState()
    val cartSummary by cartViewModel.cartSummary.collectAsState()
    val cartItems by cartViewModel.cartItems.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter { 
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Ganesh Kulfi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Fresh • Delicious • Premium",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (isAdminPreview && onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                "Back to Admin",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    }
                },
                actions = {
                    // Animated Cart Badge
                    AnimatedCartButton(
                        itemCount = cartSummary.totalItems,
                        onClick = onNavigateToCart
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Sticky cart button when items in cart
            AnimatedVisibility(
                visible = cartSummary.itemCount > 0,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Button(
                        onClick = onNavigateToCart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingCart, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${cartSummary.totalItems} items",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "₹${String.format("%.0f", cartSummary.subtotal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, null)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (filteredProducts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search Bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                EmptyProductState(searchQuery.isNotBlank())
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Welcome Banner
                item {
                    RetailerWelcomeBanner()
                }
                
                // Search Bar
                item {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Product List
                items(filteredProducts, key = { it.id }) { product ->
                    val cartQuantity = cartItems[product.id]?.quantity ?: 0
                    ModernProductCard(
                        product = product,
                        cartQuantity = cartQuantity,
                        onAddToCart = { cartViewModel.addToCart(product, 1) },
                        onIncrement = { cartViewModel.addToCart(product, 1) },
                        onDecrement = { 
                            cartViewModel.updateQuantity(product.id, cartQuantity - 1)
                        },
                        onSetQuantity = { quantity ->
                            cartViewModel.updateQuantity(product.id, quantity)
                        }
                    )
                }
                
                // Brand Story Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    RetailerBrandStoryCard()
                }
                
                // Contact Section
                item {
                    RetailerContactCard()
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedCartButton(
    itemCount: Int,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (itemCount > 0) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cart_scale"
    )
    
    Box(
        modifier = Modifier.padding(end = 8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.scale(scale)
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = "Cart",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        if (itemCount > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = 4.dp),
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Text(
                    text = if (itemCount > 99) "99+" else itemCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search for kulfi flavors...") },
        leadingIcon = {
            Icon(Icons.Default.Search, "Search")
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernProductCard(
    product: Product,
    cartQuantity: Int,
    onAddToCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetQuantity: (Int) -> Unit
) {
    val context = LocalContext.current
    val drawableName = getDrawableResourceId(product.id)
    val drawableId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
    
    var showQuantityDialog by remember { mutableStateOf(false) }
    var customQuantity by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
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
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Product Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(4.dp))
                
                // Product Description
                if (product.description.isNotBlank()) {
                    Text(
                        product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "₹${String.format("%.0f", product.basePrice)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        " /pc",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Add to Cart Controls
                AnimatedQuantityControls(
                    quantity = cartQuantity,
                    onAddToCart = onAddToCart,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                    onQuantityClick = {
                        customQuantity = if (cartQuantity > 0) cartQuantity.toString() else ""
                        showQuantityDialog = true
                    }
                )
            }
        }
    }
    
    // Custom Quantity Dialog
    if (showQuantityDialog) {
        CustomQuantityDialog(
            currentQuantity = cartQuantity,
            onDismiss = { showQuantityDialog = false },
            onConfirm = { quantity ->
                // Set quantity directly for instant update
                onSetQuantity(quantity)
                showQuantityDialog = false
            }
        )
    }
}

@Composable
fun AnimatedQuantityControls(
    quantity: Int,
    onAddToCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onQuantityClick: () -> Unit = {}
) {
    AnimatedContent(
        targetState = quantity > 0,
        transitionSpec = {
            fadeIn() + slideInHorizontally() togetherWith
                    fadeOut() + slideOutHorizontally()
        },
        label = "quantity_controls"
    ) { hasQuantity ->
        if (hasQuantity) {
            // Quantity Controls
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        "Decrease",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Surface(
                    modifier = Modifier
                        .width(50.dp)
                        .height(32.dp)
                        .clickable { onQuantityClick() },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        "Increase",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // Add Button
            Button(
                onClick = onAddToCart,
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("ADD", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyProductState(isSearching: Boolean) {
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
                imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.Inventory,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = if (isSearching) "No products found" else "No products available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isSearching) {
                Text(
                    "Try searching with different keywords",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getDrawableResourceId(flavorId: String): String {
    return when (flavorId.lowercase()) {
        "mango" -> "mango_kulfi"
        "rabdi", "rabadi" -> "rabdi_kulfi"
        "strawberry" -> "strawberry_kulfi"
        "chocolate" -> "chocolate_kulfi"
        "paan" -> "paan_kulfi"
        "pineapple" -> "pineapple_kulfi"
        "chikoo" -> "chikoo_kulfi"
        "guava" -> "guava_kulfi"
        "jamun" -> "jamun_kulfi"
        "fig" -> "fig_kulfi"
        "shitafal", "sitafal", "custard_apple" -> "sitafal_kulfi"
        "dry_fruit", "dryfruit" -> "dry_fruit_kulfi"
        "gulkand" -> "gulkand_kulfi"
        else -> "logo"
    }
}

@Composable
fun CustomQuantityDialog(
    currentQuantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantityText by remember { mutableStateOf(if (currentQuantity > 0) currentQuantity.toString() else "") }
    var errorMessage by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                "Enter Quantity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "How many pieces would you like?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { 
                        quantityText = it.filter { char -> char.isDigit() }
                        errorMessage = ""
                    },
                    label = { Text("Quantity") },
                    placeholder = { Text("Enter number of pieces") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    isError = errorMessage.isNotEmpty(),
                    supportingText = if (errorMessage.isNotEmpty()) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (currentQuantity > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Current: $currentQuantity pieces",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quantity = quantityText.toIntOrNull()
                    when {
                        quantity == null || quantity <= 0 -> {
                            errorMessage = "Please enter a valid quantity"
                        }
                        quantity > 10000 -> {
                            errorMessage = "Maximum 10,000 pieces per order"
                        }
                        else -> {
                            onConfirm(quantity)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RetailerWelcomeBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Welcome to",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Shree Ganesh Kulfi",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Experience the rich, creamy delight handcrafted with love in Kopargaon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun RetailerBrandStoryCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Our Heritage",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A Legacy of Sweetness",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Founded by Ganesh Raut, Shri Ganesh Kulfi is more than just a sweet shop; it's a cherished tradition passed down through generations. Our journey began in the heart of Kopargaon with a simple mission: to share the authentic, unforgettable taste of homemade kulfi with our community.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
            )
        }
    }
}

@Composable
fun RetailerContactCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Visit Us",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Location
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Card(
                    modifier = Modifier.size(40.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Kopargaon, Maharashtra",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Phone
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Card(
                    modifier = Modifier.size(40.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Contact",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Contact us for orders",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
