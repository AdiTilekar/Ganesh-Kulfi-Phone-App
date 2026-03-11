package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.*
import com.ganeshkulfi.backend.data.models.*
import com.ganeshkulfi.backend.data.repository.ProductRepository
import com.ganeshkulfi.backend.data.repository.SaleRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Sale Routes
 *
 * POST   /api/sales                       — Record a new sale (ADMIN, RETAILER)
 * GET    /api/sales?date=YYYY-MM-DD       — List sales for date (own or all for admin)
 * GET    /api/sales/export?date=YYYY-MM-DD — Download .xlsx profit report
 */
fun Route.saleRoutes(
    saleRepository: SaleRepository,
    productRepository: ProductRepository
) {
    route("/api/sales") {

            /**
             * POST /api/sales
             * Record a point-of-sale transaction. No authentication required.
             */
            post {
                try {
                    val req = call.receive<CreateSaleRequest>()

                    // Basic input validation
                    if (req.quantity <= 0) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Quantity must be greater than zero"))
                        return@post
                    }
                    if (req.sellPrice < 0) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Sell price cannot be negative"))
                        return@post
                    }
                    val allowedMethods = setOf("CASH", "UPI", "CREDIT")
                    if (req.paymentMethod.uppercase() !in allowedMethods) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("paymentMethod must be CASH, UPI, or CREDIT"))
                        return@post
                    }

                    // Idempotency check
                    if (req.idempotencyKey != null) {
                        val existing = saleRepository.findByIdempotencyKey(req.idempotencyKey)
                        if (existing != null) {
                            call.respond(HttpStatusCode.OK, ApiResponse(success = true, message = "Duplicate request — returning existing sale", data = existing.toResponse()))
                            return@post
                        }
                    }

                    // Look up product to get name and base cost
                    val product = productRepository.findById(req.productId)
                        ?: run {
                            call.respond(HttpStatusCode.NotFound, ErrorResponse("Product not found: ${req.productId}"))
                            return@post
                        }

                    val costPrice = product.basePrice

                    val sale = DailySale(
                        id             = UUID.randomUUID().toString(),
                        userId         = "salelog-app",
                        productId      = product.id,
                        productName    = product.name,
                        quantity       = req.quantity,
                        costPrice      = costPrice,
                        sellPrice      = req.sellPrice,
                        profit         = (req.sellPrice - costPrice) * req.quantity,
                        paymentMethod  = PaymentMethod.valueOf(req.paymentMethod.uppercase()),
                        note           = req.note,
                        idempotencyKey = req.idempotencyKey
                    )

                    val created = saleRepository.create(sale)
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, message = "Sale recorded", data = created.toResponse()))

                } catch (e: Exception) {
                    call.application.log.error("POST /api/sales error", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Server error: ${e.message}"))
                }
            }

            /**
             * GET /api/sales?date=YYYY-MM-DD
             * List all sales for the date. No authentication required.
             */
            get {
                try {
                    val dateParam = call.request.queryParameters["date"]
                        ?: LocalDate.now(ZoneOffset.UTC).toString()

                    val date = runCatching { LocalDate.parse(dateParam) }.getOrElse {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid date format. Use YYYY-MM-DD"))
                        return@get
                    }

                    val sales = saleRepository.findByDate(date, null) // null = all sales

                    val totalRevenue = sales.sumOf { it.sellPrice * it.quantity }
                    val totalCost    = sales.sumOf { it.costPrice * it.quantity }
                    val totalProfit  = sales.sumOf { it.profit }
                    val totalUnits   = sales.sumOf { it.quantity }

                    val summary = DailySalesSummaryResponse(
                        date         = date.toString(),
                        sales        = sales.map { it.toResponse() },
                        totalRevenue = totalRevenue,
                        totalCost    = totalCost,
                        totalProfit  = totalProfit,
                        totalUnits   = totalUnits
                    )

                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, message = "", data = summary))

                } catch (e: Exception) {
                    call.application.log.error("GET /api/sales error", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Server error: ${e.message}"))
                }
            }

            /**
             * GET /api/sales/export?date=YYYY-MM-DD
             * Download .xlsx profit report. No authentication required.
             */
            get("/export") {
                try {
                    val dateParam = call.request.queryParameters["date"]
                        ?: LocalDate.now(ZoneOffset.UTC).toString()

                    val date = runCatching { LocalDate.parse(dateParam) }.getOrElse {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid date format. Use YYYY-MM-DD"))
                        return@get
                    }

                    val sales = saleRepository.findByDate(date, null)

                    val xlsxBytes = buildExcel(date, sales)
                    val filename  = "sales_${date}.xlsx"

                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        "attachment; filename=\"$filename\""
                    )
                    call.respondBytes(
                        bytes       = xlsxBytes,
                        contentType = ContentType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                        status      = HttpStatusCode.OK
                    )

                } catch (e: Exception) {
                    call.application.log.error("GET /api/sales/export error", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Server error: ${e.message}"))
                }
            }
    }
}

// ─── Excel Builder ────────────────────────────────────────────────────────────

private fun buildExcel(date: LocalDate, sales: List<DailySale>): ByteArray {
    val workbook = XSSFWorkbook()
    val sheet    = workbook.createSheet("Sales ${date}")

    // ── Styles ────────────────────────────────────────────────────────────────
    val headerFont = workbook.createFont().apply {
        bold      = true
        fontName  = "Arial"
        fontHeightInPoints = 11
    }
    val headerStyle = workbook.createCellStyle().apply {
        setFont(headerFont)
        fillForegroundColor = IndexedColors.CORNFLOWER_BLUE.index
        fillPattern         = FillPatternType.SOLID_FOREGROUND
        alignment           = HorizontalAlignment.CENTER
        setBorderBottom(BorderStyle.THIN)
        setBorderTop(BorderStyle.THIN)
        setBorderLeft(BorderStyle.THIN)
        setBorderRight(BorderStyle.THIN)
    }
    val totalsStyle = workbook.createCellStyle().apply {
        setFont(workbook.createFont().apply { bold = true })
        fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
        fillPattern         = FillPatternType.SOLID_FOREGROUND
        setBorderBottom(BorderStyle.THIN)
        setBorderTop(BorderStyle.THIN)
        setBorderLeft(BorderStyle.THIN)
        setBorderRight(BorderStyle.THIN)
    }
    val currencyFormat = workbook.createDataFormat().getFormat("₹#,##0.00")
    val currencyStyle  = workbook.createCellStyle().apply {
        dataFormat = currencyFormat
        setBorderBottom(BorderStyle.THIN)
        setBorderTop(BorderStyle.THIN)
        setBorderLeft(BorderStyle.THIN)
        setBorderRight(BorderStyle.THIN)
    }
    val bodyStyle = workbook.createCellStyle().apply {
        setBorderBottom(BorderStyle.THIN)
        setBorderTop(BorderStyle.THIN)
        setBorderLeft(BorderStyle.THIN)
        setBorderRight(BorderStyle.THIN)
    }
    val totalsCurrencyStyle = workbook.createCellStyle().apply {
        cloneStyleFrom(totalsStyle)
        dataFormat = currencyFormat
        setFont(workbook.createFont().apply { bold = true })
    }

    // ── Title Row ─────────────────────────────────────────────────────────────
    val titleRow = sheet.createRow(0)
    titleRow.createCell(0).also { cell ->
        cell.setCellValue("Daily Sales Report — ${date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}")
        val titleStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                bold              = true
                fontHeightInPoints = 14
            })
        }
        cell.cellStyle = titleStyle
    }
    sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 11))

    // ── Header Row ────────────────────────────────────────────────────────────
    val headers = listOf("#", "Product Name", "Category", "Qty", "Cost Price",
        "Sell Price", "Profit/Unit", "Total Profit", "Total Revenue", "Payment", "Note", "Time")
    val headerRow = sheet.createRow(1)
    headers.forEachIndexed { col, title ->
        headerRow.createCell(col).also {
            it.setCellValue(title)
            it.cellStyle = headerStyle
        }
    }

    // ── Data Rows ─────────────────────────────────────────────────────────────
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    sales.forEachIndexed { idx, sale ->
        val row = sheet.createRow(idx + 2)
        val profitPerUnit = sale.sellPrice - sale.costPrice

        row.createCell(0).also  { it.setCellValue((idx + 1).toDouble()); it.cellStyle = bodyStyle }
        row.createCell(1).also  { it.setCellValue(sale.productName);      it.cellStyle = bodyStyle }
        row.createCell(2).also  { it.setCellValue("");                     it.cellStyle = bodyStyle } // category not stored, left blank
        row.createCell(3).also  { it.setCellValue(sale.quantity.toDouble()); it.cellStyle = bodyStyle }
        row.createCell(4).also  { it.setCellValue(sale.costPrice);         it.cellStyle = currencyStyle }
        row.createCell(5).also  { it.setCellValue(sale.sellPrice);         it.cellStyle = currencyStyle }
        row.createCell(6).also  { it.setCellValue(profitPerUnit);           it.cellStyle = currencyStyle }
        row.createCell(7).also  { it.setCellValue(sale.profit);            it.cellStyle = currencyStyle }
        row.createCell(8).also  { it.setCellValue(sale.sellPrice * sale.quantity); it.cellStyle = currencyStyle }
        row.createCell(9).also  { it.setCellValue(sale.paymentMethod.name); it.cellStyle = bodyStyle }
        row.createCell(10).also { it.setCellValue(sale.note ?: "");        it.cellStyle = bodyStyle }
        row.createCell(11).also {
            it.setCellValue(sale.soldAt.atZone(ZoneOffset.UTC).format(timeFormatter))
            it.cellStyle = bodyStyle
        }
    }

    // ── Totals Row ────────────────────────────────────────────────────────────
    val totalsRowIdx = sales.size + 2
    val totalsRow    = sheet.createRow(totalsRowIdx)
    totalsRow.createCell(0).also  { it.setCellValue("TOTALS");                        it.cellStyle = totalsStyle }
    totalsRow.createCell(1).also  { it.setCellValue("");                               it.cellStyle = totalsStyle }
    totalsRow.createCell(2).also  { it.setCellValue("");                               it.cellStyle = totalsStyle }
    totalsRow.createCell(3).also  { it.setCellValue(sales.sumOf { it.quantity }.toDouble()); it.cellStyle = totalsStyle }
    totalsRow.createCell(4).also  { it.setCellValue(sales.sumOf { it.costPrice * it.quantity }); it.cellStyle = totalsCurrencyStyle }
    totalsRow.createCell(5).also  { it.setCellValue(sales.sumOf { it.sellPrice * it.quantity }); it.cellStyle = totalsCurrencyStyle }
    totalsRow.createCell(6).also  { it.setCellValue("");                               it.cellStyle = totalsStyle }
    totalsRow.createCell(7).also  { it.setCellValue(sales.sumOf { it.profit });        it.cellStyle = totalsCurrencyStyle }
    totalsRow.createCell(8).also  { it.setCellValue(sales.sumOf { it.sellPrice * it.quantity }); it.cellStyle = totalsCurrencyStyle }
    (9..11).forEach { col -> totalsRow.createCell(col).also { it.cellStyle = totalsStyle } }

    // Auto-size columns
    headers.indices.forEach { sheet.autoSizeColumn(it) }

    return ByteArrayOutputStream().also { workbook.write(it); workbook.close() }.toByteArray()
}

// ─── Extension: DailySale → SaleResponse ────────────────────────────────────

private fun DailySale.toResponse() = SaleResponse(
    id            = id,
    userId        = userId,
    productId     = productId,
    productName   = productName,
    quantity      = quantity,
    costPrice     = costPrice,
    sellPrice     = sellPrice,
    profit        = profit,
    paymentMethod = paymentMethod.name,
    note          = note,
    soldAt        = soldAt.toString()
)
