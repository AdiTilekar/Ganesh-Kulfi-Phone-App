package com.ganeshkulfi.salelog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.salelog.SalesViewModel
import com.ganeshkulfi.salelog.data.Product
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    onCartClick: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val products    by viewModel.products.collectAsState()
    val cart        by viewModel.cart.collectAsState()
    val cartTotal   by viewModel.cartTotal.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val error       by viewModel.errorMessage.collectAsState()
    val pingMessage by viewModel.pingMessage.collectAsState()

    val cartCount = cart.sumOf { it.quantity }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show ping result in a Snackbar whenever it changes
    LaunchedEffect(pingMessage) {
        val msg = pingMessage ?: return@LaunchedEffect
        if (msg == "Testing…") return@LaunchedEffect
        scope.launch {
            snackbarHostState.showSnackbar(
                message  = msg,
                duration = SnackbarDuration.Long
            )
            viewModel.clearPingMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ganesh Kulfi — Sales") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    val isTesting = pingMessage == "Testing…"
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    IconButton(onClick = { viewModel.pingBackend() }, enabled = !isTesting) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Test backend connection"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (cartCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onCartClick,
                    icon    = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    text    = {
                        Text("Cart: $cartCount items · ₹${"%.0f".format(cartTotal)}")
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { padding ->
        when {
            isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.loadProducts() }) { Text("Retry") }
                        OutlinedButton(onClick = { viewModel.pingBackend() }) { Text("Test Backend") }
                    }
                }
            }

            products.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("No products found") }

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(products) { product ->
                    ProductCard(
                        product  = product,
                        qty      = viewModel.getCartQty(product.id),
                        onAdd    = { viewModel.updateQuantity(product, +1) },
                        onRemove = { viewModel.updateQuantity(product, -1) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) } // FAB clearance
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    qty: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text       = product.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = "₹${"%.2f".format(product.basePrice)} / ${product.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stepper
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick  = onRemove,
                    enabled  = qty > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                }
                Text(
                    text      = qty.toString(),
                    modifier  = Modifier.widthIn(min = 32.dp).wrapContentWidth(),
                    style     = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FilledIconButton(
                    onClick  = onAdd,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
