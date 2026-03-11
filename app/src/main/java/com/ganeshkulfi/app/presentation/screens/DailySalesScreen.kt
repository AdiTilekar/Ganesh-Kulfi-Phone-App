package com.ganeshkulfi.app.presentation.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.R
import com.ganeshkulfi.app.data.remote.DailySalesSummaryResponse
import com.ganeshkulfi.app.data.remote.SaleResponse
import com.ganeshkulfi.app.presentation.viewmodel.ExportStatus
import com.ganeshkulfi.app.presentation.viewmodel.SalesViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySalesScreen(
    onNavigateBack: () -> Unit,
    onAddSaleClick: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val context      = LocalContext.current
    val summary      by viewModel.salesSummary.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()

    // Show export result as a Toast
    LaunchedEffect(exportStatus) {
        when (exportStatus) {
            is ExportStatus.Success -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.sale_export_saved, (exportStatus as ExportStatus.Success).filePath),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetExportStatus()
            }
            is ExportStatus.Error -> {
                Toast.makeText(
                    context,
                    (exportStatus as ExportStatus.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetExportStatus()
            }
            else -> {}
        }
    }

    val displayDate = runCatching {
        LocalDate.parse(selectedDate).format(displayFormatter)
    }.getOrDefault(selectedDate)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_sales_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // Date picker button
                    IconButton(onClick = { showDatePicker(context, selectedDate) { viewModel.selectDate(it) } }) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.sale_pick_date))
                    }
                    // Download Excel button
                    IconButton(
                        onClick  = { viewModel.exportToExcel(context, selectedDate) },
                        enabled  = exportStatus !is ExportStatus.Loading
                    ) {
                        if (exportStatus is ExportStatus.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = stringResource(R.string.sale_download_excel))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSaleClick,
                icon    = { Icon(Icons.Default.Add, contentDescription = null) },
                text    = { Text(stringResource(R.string.sale_add_button)) }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment    = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadSalesForDate(selectedDate) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date header
                    item {
                        Text(
                            text       = displayDate,
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Summary cards
                    if (summary != null) {
                        item {
                            SummaryCards(summary = summary!!)
                        }
                    }

                    // Sales list header
                    item {
                        Text(
                            text  = stringResource(R.string.daily_sales_transactions_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val sales = summary?.sales ?: emptyList()
                    if (sales.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors   = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier         = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text  = stringResource(R.string.daily_sales_empty),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(sales) { sale ->
                            SaleCard(sale = sale)
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }
    }
}

@Composable
private fun SummaryCards(summary: DailySalesSummaryResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                label  = stringResource(R.string.daily_sales_total_revenue),
                value  = "₹%.2f".format(summary.totalRevenue),
                color  = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label  = stringResource(R.string.daily_sales_total_cost),
                value  = "₹%.2f".format(summary.totalCost),
                color  = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                label  = stringResource(R.string.daily_sales_total_profit),
                value  = "₹%.2f".format(summary.totalProfit),
                color  = if (summary.totalProfit >= 0)
                             MaterialTheme.colorScheme.secondaryContainer
                         else
                             MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label  = stringResource(R.string.daily_sales_units_sold),
                value  = summary.totalUnits.toString(),
                color  = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                label  = stringResource(R.string.daily_sales_total_products),
                value  = summary.sales.distinctBy { it.productName }.size.toString(),
                color  = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SaleCard(sale: SaleResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = sale.productName,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = {},
                    label   = { Text(sale.paymentMethod) }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SaleMetric(label = "Qty",    value = "${sale.quantity}")
                SaleMetric(label = "Cost",   value = "₹${sale.costPrice}")
                SaleMetric(label = "Price",  value = "₹${sale.sellPrice}")
                SaleMetric(
                    label = "Profit",
                    value = "₹%.2f".format(sale.profit),
                    valueColor = if (sale.profit >= 0)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            if (!sale.note.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = sale.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))
            val timeStr = runCatching {
                sale.soldAt.substringAfterLast("T").take(8)
            }.getOrDefault(sale.soldAt)
            Text(
                text  = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SaleMetric(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

// ─── Date Picker Helper ───────────────────────────────────────────────────────

private fun showDatePicker(
    context: Context,
    currentDate: String,
    onDateSelected: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    runCatching {
        val ld = LocalDate.parse(currentDate)
        cal.set(ld.year, ld.monthValue - 1, ld.dayOfMonth)
    }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val picked = LocalDate.of(year, month + 1, day)
            onDateSelected(picked.toString())
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.maxDate = System.currentTimeMillis()
        show()
    }
}
