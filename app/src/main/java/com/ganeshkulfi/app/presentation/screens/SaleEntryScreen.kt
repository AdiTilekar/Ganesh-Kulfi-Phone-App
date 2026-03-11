package com.ganeshkulfi.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.R
import com.ganeshkulfi.app.data.remote.Product
import com.ganeshkulfi.app.presentation.viewmodel.AddSaleState
import com.ganeshkulfi.app.presentation.viewmodel.SalesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleEntryScreen(
    onNavigateBack: () -> Unit,
    onSaleRecorded: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val products    by viewModel.products.collectAsState()
    val addSaleState by viewModel.addSaleState.collectAsState()

    // Form fields
    var selectedProduct   by remember { mutableStateOf<Product?>(null) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var quantityText      by remember { mutableStateOf("1") }
    var sellPriceText     by remember { mutableStateOf("") }
    var selectedPayment   by remember { mutableStateOf("CASH") }
    var noteText          by remember { mutableStateOf("") }

    // Pre-fill sell price when product is selected
    LaunchedEffect(selectedProduct) {
        if (selectedProduct != null && sellPriceText.isBlank()) {
            sellPriceText = selectedProduct!!.basePrice.toString()
        }
    }

    // Handle state changes from ViewModel
    LaunchedEffect(addSaleState) {
        when (addSaleState) {
            is AddSaleState.Success -> {
                viewModel.resetAddSaleState()
                onSaleRecorded()
            }
            else -> {}
        }
    }

    val showError = addSaleState is AddSaleState.Error
    val errorMsg  = (addSaleState as? AddSaleState.Error)?.message

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sale_entry_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Section header ────────────────────────────────────────────
            Text(
                text       = stringResource(R.string.sale_entry_section_product),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )

            // ── Product dropdown ──────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded        = productDropdownExpanded,
                onExpandedChange = { productDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value         = selectedProduct?.name ?: stringResource(R.string.sale_select_product),
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text(stringResource(R.string.sale_product_label)) },
                    trailingIcon  = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded        = productDropdownExpanded,
                    onDismissRequest = { productDropdownExpanded = false }
                ) {
                    products.filter { it.isActive }.forEach { product ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(product.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "₹${product.basePrice}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            },
                            onClick = {
                                selectedProduct          = product
                                sellPriceText            = product.basePrice.toString()
                                productDropdownExpanded  = false
                            }
                        )
                    }
                }
            }

            // ── Cost price (read-only, from product base price) ───────────
            if (selectedProduct != null) {
                OutlinedTextField(
                    value         = "₹${selectedProduct!!.basePrice}",
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text(stringResource(R.string.sale_cost_price_label)) },
                    supportingText = { Text(stringResource(R.string.sale_cost_price_hint)) },
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        disabledTextColor       = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor     = MaterialTheme.colorScheme.outline,
                        disabledLabelColor      = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled       = false
                )
            }

            // ── Quantity ──────────────────────────────────────────────────
            OutlinedTextField(
                value         = quantityText,
                onValueChange = { if (it.all(Char::isDigit)) quantityText = it },
                label         = { Text(stringResource(R.string.sale_quantity_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Sell price (editable) ─────────────────────────────────────
            OutlinedTextField(
                value         = sellPriceText,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        sellPriceText = it
                    }
                },
                label         = { Text(stringResource(R.string.sale_sell_price_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix        = { Text("₹") },
                modifier      = Modifier.fillMaxWidth()
            )

            // Profit preview
            val qty   = quantityText.toIntOrNull() ?: 0
            val sell  = sellPriceText.toDoubleOrNull() ?: 0.0
            val cost  = selectedProduct?.basePrice ?: 0.0
            val profit = (sell - cost) * qty

            if (selectedProduct != null && qty > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (profit >= 0)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.sale_profit_preview),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "₹%.2f".format(profit),
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color      = if (profit >= 0)
                                           MaterialTheme.colorScheme.secondary
                                         else
                                           MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Divider()

            // ── Payment method ────────────────────────────────────────────
            Text(
                text       = stringResource(R.string.sale_payment_method_label),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("CASH", "UPI", "CREDIT").forEach { method ->
                    FilterChip(
                        selected  = selectedPayment == method,
                        onClick   = { selectedPayment = method },
                        label     = { Text(method) },
                        modifier  = Modifier.weight(1f)
                    )
                }
            }

            Divider()

            // ── Optional note ─────────────────────────────────────────────
            OutlinedTextField(
                value         = noteText,
                onValueChange = { noteText = it },
                label         = { Text(stringResource(R.string.sale_note_label)) },
                placeholder   = { Text(stringResource(R.string.sale_note_placeholder)) },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 2,
                maxLines      = 4
            )

            // ── Error message ─────────────────────────────────────────────
            if (showError) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text     = errorMsg ?: "",
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ── Submit button ─────────────────────────────────────────────
            val isSubmitting = addSaleState is AddSaleState.Loading
            Button(
                onClick  = {
                    viewModel.addSale(
                        productId     = selectedProduct?.id ?: "",
                        quantity      = quantityText.toIntOrNull() ?: 0,
                        sellPrice     = sellPriceText.toDoubleOrNull() ?: 0.0,
                        paymentMethod = selectedPayment,
                        note          = noteText.takeUnless { it.isBlank() }
                    )
                },
                enabled  = !isSubmitting && selectedProduct != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color    = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.sale_submit_button))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
