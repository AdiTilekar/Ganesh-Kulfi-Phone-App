package com.ganeshkulfi.salelog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.salelog.SalesViewModel
import com.ganeshkulfi.salelog.data.CartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateBack: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val cart      by viewModel.cart.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error     by viewModel.errorMessage.collectAsState()
    val confirmed by viewModel.saleConfirmed.collectAsState()

    var selectedPayment by remember { mutableStateOf("CASH") }
    var note            by remember { mutableStateOf("") }

    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(confirmed) {
        if (confirmed) {
            snackbarHost.showSnackbar("Sale confirmed! Cart cleared.")
            viewModel.resetSaleConfirmed()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cart") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        if (cart.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Your cart is empty", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cart items
            items(cart) { item ->
                CartItemRow(
                    item          = item,
                    onPriceChange = { viewModel.updateSellPrice(item.product.id, it) }
                )
            }

            // Total
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text       = "TOTAL: ₹${"%.2f".format(cartTotal)}",
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Payment method
            item {
                Text(
                    text  = "Payment Method",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("CASH", "UPI", "CREDIT").forEach { method ->
                        FilterChip(
                            selected = selectedPayment == method,
                            onClick  = { selectedPayment = method },
                            label    = { Text(method) }
                        )
                    }
                }
            }

            // Note
            item {
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    label         = { Text("Note (optional)") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = false,
                    maxLines      = 3
                )
            }

            // Error
            if (error != null) {
                item {
                    Text(
                        text  = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Confirm button
            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick  = { viewModel.confirmSale(selectedPayment, note) },
                    enabled  = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(
                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    else Text("CONFIRM SALE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onPriceChange: (Double) -> Unit
) {
    var priceText by remember(item.product.id) {
        mutableStateOf("%.2f".format(item.sellPrice))
    }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.product.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "Qty: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value         = priceText,
                onValueChange = { txt ->
                    priceText = txt
                    txt.toDoubleOrNull()?.let { onPriceChange(it) }
                },
                label         = { Text("₹ Price") },
                modifier      = Modifier.width(110.dp),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}
