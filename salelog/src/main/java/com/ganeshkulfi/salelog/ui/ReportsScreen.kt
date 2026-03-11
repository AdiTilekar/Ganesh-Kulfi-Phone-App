package com.ganeshkulfi.salelog.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.salelog.ExportStatus
import com.ganeshkulfi.salelog.SalesViewModel
import com.ganeshkulfi.salelog.data.DailySummaryResponse
import com.ganeshkulfi.salelog.data.SaleResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

private val displayFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: SalesViewModel = hiltViewModel()
) {
    val context      = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val summary      by viewModel.summary.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val error        by viewModel.errorMessage.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()

    val displayDate = runCatching {
        LocalDate.parse(selectedDate).format(displayFmt)
    }.getOrDefault(selectedDate)

    LaunchedEffect(Unit) { viewModel.loadReports() }

    LaunchedEffect(exportStatus) {
        when (exportStatus) {
            is ExportStatus.Success -> {
                Toast.makeText(
                    context,
                    "Saved to ${(exportStatus as ExportStatus.Success).filePath}",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetExportStatus()
            }
            is ExportStatus.Error -> {
                Toast.makeText(context, (exportStatus as ExportStatus.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetExportStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports — $displayDate") },
                actions = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        val parts = selectedDate.split("-").map { it.toInt() }
                        if (parts.size == 3) cal.set(parts[0], parts[1] - 1, parts[2])
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                viewModel.selectDate("%04d-%02d-%02d".format(y, m + 1, d))
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
                    }
                    IconButton(
                        onClick  = { viewModel.exportExcel(context, selectedDate) },
                        enabled  = exportStatus !is ExportStatus.Loading
                    ) {
                        if (exportStatus is ExportStatus.Loading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Download Excel")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadReports() }) { Text("Retry") }
                }
            }
            else -> ReportsContent(summary = summary, padding = padding)
        }
    }
}

@Composable
private fun ReportsContent(
    summary: DailySummaryResponse?,
    padding: PaddingValues
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (summary != null) {
            item { SummaryGrid(summary) }

            item {
                Text(
                    text       = "Transactions (${summary.sales.size})",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (summary.sales.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("No sales for this date") }
                    }
                }
            } else {
                items(summary.sales) { SaleRow(it) }
            }
        } else {
            item {
                Box(Modifier.fillParentMaxSize(), Alignment.Center) {
                    Text("No data. Load reports using the date picker.")
                }
            }
        }
    }
}

@Composable
private fun SummaryGrid(summary: DailySummaryResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Revenue", "₹${"%.0f".format(summary.totalRevenue)}", Modifier.weight(1f))
            SummaryCard("Cost", "₹${"%.0f".format(summary.totalCost)}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Profit", "₹${"%.0f".format(summary.totalProfit)}", Modifier.weight(1f))
            SummaryCard("Units", "${summary.totalUnits}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SaleRow(sale: SaleResponse) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(sale.productName, fontWeight = FontWeight.SemiBold)
                Text(
                    "${sale.quantity} units · ${sale.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!sale.note.isNullOrBlank()) {
                    Text(
                        sale.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${"%.0f".format(sale.sellPrice)}", fontWeight = FontWeight.Bold)
                Text(
                    "Profit ₹${"%.0f".format(sale.profit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sale.profit >= 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
