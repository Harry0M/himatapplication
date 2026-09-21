package com.example.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun formatInr(amount: Double): String {
        return "₹" + String.format(Locale.US, "%,.2f", amount)
    }

    /**
     * Converts a numeric amount to Indian currency words
     */
    fun convertNumberToWords(amount: Long): String {
        if (amount == 0L) return "Rupees Zero Only"
        val units = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf(
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        )

        fun numToWords(n: Long): String {
            return when {
                n < 20 -> units[n.toInt()]
                n < 100 -> tens[(n / 10).toInt()] + if (n % 10 != 0L) " " + units[(n % 10).toInt()] else ""
                n < 1000 -> units[(n / 100).toInt()] + " Hundred" + if (n % 100 != 0L) " " + numToWords(n % 100) else ""
                n < 100000 -> numToWords(n / 1000) + " Thousand" + if (n % 1000 != 0L) " " + numToWords(n % 1000) else ""
                n < 10000000 -> numToWords(n / 100000) + " Lakh" + if (n % 100000 != 0L) " " + numToWords(n % 100000) else ""
                else -> numToWords(n / 10000000) + " Crore" + if (n % 10000000 != 0L) " " + numToWords(n % 10000000) else ""
            }
        }

        return "Rupees " + numToWords(amount).trim() + " Only"
    }

    // =========================================================================
    // 1. CUSTOMER DAY REPORT (Preserved & Enhanced with DISPATCH STATUS)
    // =========================================================================
    fun generateCustomerDayReport(
        context: Context,
        visit: VisitEntity,
        customer: CustomerEntity?,
        salesman: EmployeeEntity?,
        entries: List<PurchaseEntryEntity>
    ): File {
        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val primaryColor = Color.rgb(19, 35, 56) // Navy Primary
        val goldColor = Color.rgb(200, 157, 60) // Gold Accent
        val textDark = Color.rgb(20, 25, 35)
        val textGray = Color.rgb(100, 110, 125)
        val lightBg = Color.rgb(245, 247, 250)
        val tableBorder = Color.rgb(220, 225, 232)

        fun drawHeader() {
            paint.color = primaryColor
            canvas.drawRect(0f, 0f, 595f, 95f, paint)

            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.himat_logo)
            } catch (_: Exception) {
                null
            }

            val logoHeight = 62f
            val logoWidth = if (logoBitmap != null) logoHeight * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat()) else 0f
            val logoLeft = 30f
            val logoTop = 16f
            if (logoBitmap != null) {
                canvas.drawBitmap(logoBitmap, null, RectF(logoLeft, logoTop, logoLeft + logoWidth, logoTop + logoHeight), paint)
            }
            val textStartX = if (logoBitmap != null) (logoLeft + logoWidth + 12f) else 30f

            paint.color = Color.WHITE
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("HIMAT TEXTILE", textStartX, 35f, paint)

            paint.textSize = 9f
            paint.color = Color.rgb(203, 213, 225)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("YOUR BUSINESS GUIDE ACROSS INDIA", textStartX, 49f, paint)

            paint.textSize = 7.5f
            paint.color = Color.rgb(203, 213, 225)
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Ahmedabad, Gujarat 380022", textStartX, 63f, paint)
            canvas.drawText("GSTIN: 24EASPS6621D1ZG • Phone: +91 98739 38095", textStartX, 76f, paint)

            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CUSTOMER DAY REPORT", 410f, 45f, paint)

            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.rgb(220, 230, 245)
            canvas.drawText("Visit: ${visit.visitCode}", 410f, 62f, paint)
            canvas.drawText("Date: ${visit.date}", 410f, 76f, paint)
        }

        drawHeader()

        var y = 115f

        // Customer & Salesman Info Box
        paint.color = lightBg
        canvas.drawRoundRect(30f, y, 565f, y + 60f, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        paint.strokeWidth = 1f
        canvas.drawRoundRect(30f, y, 565f, y + 60f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMER DETAILS:", 42f, y + 18f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(customer?.name ?: visit.customerName, 42f, y + 33f, paint)
        paint.color = textGray
        paint.textSize = 8.5f
        val custGstin = if (!customer?.gstin.isNullOrBlank()) "GSTIN: ${customer?.gstin}" else "GSTIN: Unregistered"
        canvas.drawText("${customer?.city ?: ""} • ${customer?.phone ?: ""} • $custGstin", 42f, y + 47f, paint)

        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FIELD AGENT / SALESMAN:", 350f, y + 18f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(salesman?.name ?: visit.employeeName, 350f, y + 33f, paint)
        paint.color = textGray
        paint.textSize = 8.5f
        canvas.drawText("Phone: ${salesman?.phone ?: "—"}", 350f, y + 47f, paint)

        y += 75f

        val colX = floatArrayOf(30f, 75f, 195f, 265f, 300f, 345f, 415f, 485f, 565f)
        val rowBorderPaint = Paint().apply {
            isAntiAlias = true
            color = tableBorder
            strokeWidth = 1f
        }
        val headerBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(71, 85, 105)
            strokeWidth = 1f
        }

        fun drawTableHeader() {
            paint.color = primaryColor
            canvas.drawRect(30f, y, 565f, y + 20f, paint)
            paint.color = Color.WHITE
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("ORDER #", 33f, y + 13f, paint)
            canvas.drawText("SUPPLIER / MILL", 78f, y + 13f, paint)
            canvas.drawText("ITEM / STYLE", 198f, y + 13f, paint)
            canvas.drawText("PCS", 268f, y + 13f, paint)
            canvas.drawText("RATE", 303f, y + 13f, paint)
            canvas.drawText("PACKING", 348f, y + 13f, paint)
            canvas.drawText("STATUS", 418f, y + 13f, paint)
            canvas.drawText("AMOUNT", 488f, y + 13f, paint)

            for (x in colX) {
                canvas.drawLine(x, y, x, y + 20f, headerBorderPaint)
            }
            canvas.drawLine(30f, y, 565f, y, headerBorderPaint)
            canvas.drawLine(30f, y + 20f, 565f, y + 20f, headerBorderPaint)
            y += 20f
        }

        drawTableHeader()

        var totalPieces = 0
        var totalAmount = 0.0
        var totalGst = 0.0
        var totalCases = 0
        var totalLoose = 0

        val grouped = entries.groupBy { it.supplierName }

        for ((supplierName, supplierEntries) in grouped) {
            // Check page height
            if (y > 720f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 35f
                drawHeader()
                y = 115f
                drawTableHeader()
            }

            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(30f, y, 565f, y + 16f, paint)
            paint.color = primaryColor
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val supType = supplierEntries.firstOrNull()?.supplierType ?: ""
            canvas.drawText("▶ $supplierName ($supType)", 35f, y + 11f, paint)
            canvas.drawLine(30f, y, 565f, y, rowBorderPaint)
            canvas.drawLine(30f, y + 16f, 565f, y + 16f, rowBorderPaint)
            canvas.drawLine(30f, y, 30f, y + 16f, rowBorderPaint)
            canvas.drawLine(565f, y, 565f, y + 16f, rowBorderPaint)
            y += 16f

            for (item in supplierEntries) {
                if (y > 720f) {
                    pdfDocument.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 35f
                    drawHeader()
                    y = 115f
                    drawTableHeader()
                }

                totalPieces += item.pieces
                totalAmount += item.totalAmount
                totalGst += item.gstAmount
                totalCases += item.caseCount
                totalLoose += item.loosePieces

                val rowTop = y
                paint.color = textDark
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 7.5f
                canvas.drawText(item.orderNo, 33f, y + 12f, paint)

                val displaySupp = if (item.supplierName.length > 22) item.supplierName.take(20) + ".." else item.supplierName
                canvas.drawText(displaySupp, 78f, y + 12f, paint)

                val displayItem = if (item.itemCode.length > 13) item.itemCode.take(11) + ".." else item.itemCode
                canvas.drawText(displayItem, 198f, y + 12f, paint)

                canvas.drawText("${item.pieces}", 268f, y + 12f, paint)
                canvas.drawText(formatInr(item.rate), 303f, y + 12f, paint)

                val packDesc = if (item.loosePieces > 0) "${item.caseCount}c+${item.loosePieces}L" else "${item.caseCount} cs"
                canvas.drawText(packDesc, 348f, y + 12f, paint)

                // Delivery Status column
                val status = item.deliveryStatus.ifBlank { "Pending" }
                val isDelivered = status.equals("Delivered", ignoreCase = true)
                val isDispatched = status.equals("Dispatched", ignoreCase = true)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = if (isDelivered) Color.rgb(22, 163, 74) else if (isDispatched) Color.rgb(37, 99, 235) else Color.rgb(180, 83, 9)
                canvas.drawText(status, 418f, y + 12f, paint)

                paint.color = textDark
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(formatInr(item.totalAmount), 488f, y + 12f, paint)

                y += 16f

                if (!item.mixedPackNote.isNullOrBlank()) {
                    paint.color = textGray
                    paint.textSize = 7f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    canvas.drawText("↳ Note: ${item.mixedPackNote}", 78f, y + 9f, paint)
                    y += 13f
                }

                val rowBottom = y
                for (x in colX) {
                    canvas.drawLine(x, rowTop, x, rowBottom, rowBorderPaint)
                }
                canvas.drawLine(30f, rowBottom, 565f, rowBottom, rowBorderPaint)
            }
        }

        if (y > 670f) {
            pdfDocument.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 35f
            drawHeader()
            y = 115f
        }

        y += 10f

        // Totals Box
        paint.color = lightBg
        canvas.drawRoundRect(310f, y, 565f, y + 80f, 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        canvas.drawRoundRect(310f, y, 565f, y + 80f, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("Subtotal (${totalPieces} Pcs):", 325f, y + 18f, paint)
        canvas.drawText(formatInr(totalAmount), 475f, y + 18f, paint)

        canvas.drawText("Garment GST (5%):", 325f, y + 34f, paint)
        canvas.drawText(formatInr(totalGst), 475f, y + 34f, paint)

        canvas.drawText("Packing Summary:", 325f, y + 50f, paint)
        canvas.drawText("$totalCases Full Cases, $totalLoose Loose", 435f, y + 50f, paint)

        paint.color = primaryColor
        canvas.drawRect(310f, y + 58f, 565f, y + 80f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9.5f
        canvas.drawText("GRAND TOTAL:", 325f, y + 73f, paint)
        canvas.drawText(formatInr(totalAmount + totalGst), 465f, y + 73f, paint)

        y += 105f

        // Footer / Terms
        paint.color = textGray
        paint.textSize = 7.5f
        canvas.drawText("• This consolidated report is generated by Himat Textile — Your Business Guide Across India.", 30f, y, paint)
        canvas.drawText("• Delivery & goods receipt subject to individual supplier dispatch terms & transporter consignment note.", 30f, y + 12f, paint)
        canvas.drawText("• For any packing discrepancies or invoice queries, contact Himat Textile office.", 30f, y + 24f, paint)

        // Signatures
        y += 45f
        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawLine(50f, y, 190f, y, paint)
        canvas.drawText("Customer Acceptance", 65f, y + 14f, paint)

        canvas.drawLine(405f, y, 545f, y, paint)
        canvas.drawText("For HIMAT TEXTILE", 425f, y + 14f, paint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "Customer_Report_${visit.visitCode}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // 2. SUPPLIER VOUCHER (Preserved)
    // =========================================================================
    fun generateSupplierCopy(
        context: Context,
        visit: VisitEntity,
        supplier: SupplierEntity,
        customer: CustomerEntity?,
        salesman: EmployeeEntity?,
        entries: List<PurchaseEntryEntity>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val primaryColor = Color.rgb(19, 35, 56)
        val goldColor = Color.rgb(200, 157, 60)
        val textDark = Color.rgb(20, 25, 35)
        val textGray = Color.rgb(100, 110, 125)
        val lightBg = Color.rgb(245, 247, 250)
        val tableBorder = Color.rgb(220, 225, 232)

        var y = 35f

        // Top Banner
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, 595f, 95f, paint)

        val logoBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.himat_logo)
        } catch (_: Exception) {
            null
        }

        val logoHeight = 62f
        val logoWidth = if (logoBitmap != null) logoHeight * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat()) else 0f
        val logoLeft = 30f
        val logoTop = 16f
        if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, null, RectF(logoLeft, logoTop, logoLeft + logoWidth, logoTop + logoHeight), paint)
        }
        val textStartX = if (logoBitmap != null) (logoLeft + logoWidth + 12f) else 30f

        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("HIMAT TEXTILE", textStartX, 35f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(203, 213, 225)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("YOUR BUSINESS GUIDE ACROSS INDIA", textStartX, 49f, paint)

        paint.textSize = 7.5f
        paint.color = Color.rgb(203, 213, 225)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Ahmedabad, Gujarat 380022", textStartX, 63f, paint)
        canvas.drawText("GSTIN: 24EASPS6621D1ZG • Phone: +91 98739 38095", textStartX, 76f, paint)

        paint.color = Color.WHITE
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SUPPLIER VOUCHER", 415f, 45f, paint)

        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(220, 230, 245)
        canvas.drawText("Date: ${visit.date}", 415f, 62f, paint)
        canvas.drawText("Type: ${supplier.type}", 415f, 76f, paint)

        y = 115f

        // Supplier & Buyer Info
        paint.color = lightBg
        canvas.drawRoundRect(30f, y, 565f, y + 65f, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        paint.strokeWidth = 1f
        canvas.drawRoundRect(30f, y, 565f, y + 65f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Supplier Info
        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SUPPLIER (${supplier.type.uppercase()}):", 42f, y + 18f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(supplier.name, 42f, y + 33f, paint)
        paint.color = textGray
        paint.textSize = 8.5f
        val suppGstin = if (supplier.gstin.isNotBlank()) "GSTIN: ${supplier.gstin}" else ""
        canvas.drawText("${supplier.marketArea} • Contact: ${supplier.contactPerson} (${supplier.phone})", 42f, y + 47f, paint)
        if (suppGstin.isNotBlank()) {
            canvas.drawText(suppGstin, 42f, y + 59f, paint)
        }

        // Buyer Info
        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BUYER / RETAILER:", 340f, y + 18f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(customer?.name ?: visit.customerName, 340f, y + 33f, paint)
        paint.color = textGray
        paint.textSize = 8.5f
        canvas.drawText("Market: ${customer?.city ?: "—"}", 340f, y + 47f, paint)
        canvas.drawText("Agent: ${salesman?.name ?: visit.employeeName}", 340f, y + 59f, paint)

        y += 80f

        val colX = floatArrayOf(30f, 75f, 185f, 220f, 265f, 325f, 410f, 480f, 565f)
        val rowBorderPaint = Paint().apply {
            isAntiAlias = true
            color = tableBorder
            strokeWidth = 1f
        }
        val headerBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(71, 85, 105)
            strokeWidth = 1f
        }

        // Table Header
        paint.color = primaryColor
        canvas.drawRect(30f, y, 565f, y + 20f, paint)
        paint.color = Color.WHITE
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("ORDER #", 33f, y + 13f, paint)
        canvas.drawText("ITEM / STYLE CODE", 78f, y + 13f, paint)
        canvas.drawText("PCS", 188f, y + 13f, paint)
        canvas.drawText("RATE", 224f, y + 13f, paint)
        canvas.drawText("CASE SIZE", 268f, y + 13f, paint)
        canvas.drawText("PACKING", 328f, y + 13f, paint)
        canvas.drawText("STATUS", 414f, y + 13f, paint)
        canvas.drawText("AMOUNT", 484f, y + 13f, paint)

        for (x in colX) {
            canvas.drawLine(x, y, x, y + 20f, headerBorderPaint)
        }
        canvas.drawLine(30f, y, 565f, y, headerBorderPaint)
        canvas.drawLine(30f, y + 20f, 565f, y + 20f, headerBorderPaint)

        y += 20f

        var totalPcs = 0
        var totalAmount = 0.0
        var totalGst = 0.0
        var totalCases = 0
        var totalLoose = 0

        for (item in entries) {
            totalPcs += item.pieces
            totalAmount += item.totalAmount
            totalGst += item.gstAmount
            totalCases += item.caseCount
            totalLoose += item.loosePieces

            val rowTop = y
            paint.color = textDark
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 7.5f
            canvas.drawText(item.orderNo, 33f, y + 12f, paint)

            val displayItem = if (item.itemCode.length > 20) item.itemCode.take(18) + ".." else item.itemCode
            canvas.drawText(displayItem, 78f, y + 12f, paint)
            canvas.drawText("${item.pieces}", 188f, y + 12f, paint)
            canvas.drawText(formatInr(item.rate), 224f, y + 12f, paint)
            canvas.drawText("${item.caseSize} p/c", 268f, y + 12f, paint)

            val packSplit = if (item.loosePieces > 0) "${item.caseCount}c+${item.loosePieces}L" else "${item.caseCount} cs"
            canvas.drawText(packSplit, 328f, y + 12f, paint)

            // Delivery Status column
            val status = item.deliveryStatus.ifBlank { "Pending" }
            val isDelivered = status.equals("Delivered", ignoreCase = true)
            val isDispatched = status.equals("Dispatched", ignoreCase = true)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (isDelivered) Color.rgb(22, 163, 74) else if (isDispatched) Color.rgb(37, 99, 235) else Color.rgb(180, 83, 9)
            canvas.drawText(status, 414f, y + 12f, paint)

            paint.color = textDark
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(formatInr(item.totalAmount), 484f, y + 12f, paint)

            y += 16f

            if (!item.mixedPackNote.isNullOrBlank()) {
                paint.color = textGray
                paint.textSize = 7f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText("↳ Note: ${item.mixedPackNote}", 78f, y + 9f, paint)
                y += 13f
            }

            val rowBottom = y
            for (x in colX) {
                canvas.drawLine(x, rowTop, x, rowBottom, rowBorderPaint)
            }
            canvas.drawLine(30f, rowBottom, 565f, rowBottom, rowBorderPaint)
        }

        y += 12f

        // Totals and Dispatch Info
        paint.color = lightBg
        canvas.drawRoundRect(30f, y, 290f, y + 75f, 4f, 4f, paint)
        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DELIVERY & DISPATCH INSTRUCTIONS:", 40f, y + 16f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8f
        val expDate = entries.firstOrNull()?.expectedDeliveryDate?.ifBlank { "Immediate / Standard" } ?: "Standard"
        val transporter = entries.firstOrNull()?.transporter?.ifBlank { "To be advised" } ?: "To be advised"
        canvas.drawText("Expected Dispatch: $expDate", 40f, y + 32f, paint)
        canvas.drawText("Transporter: $transporter", 40f, y + 46f, paint)
        canvas.drawText("Billing: Supplier's GST Invoice to follow", 40f, y + 60f, paint)

        // Totals Box
        paint.color = lightBg
        canvas.drawRoundRect(310f, y, 565f, y + 75f, 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        canvas.drawRoundRect(310f, y, 565f, y + 75f, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("Total Quantity:", 325f, y + 16f, paint)
        canvas.drawText("$totalPcs Pieces ($totalCases Cases, $totalLoose Loose)", 415f, y + 16f, paint)

        canvas.drawText("Taxable Subtotal:", 325f, y + 32f, paint)
        canvas.drawText(formatInr(totalAmount), 480f, y + 32f, paint)

        canvas.drawText("Garment GST (5%):", 325f, y + 46f, paint)
        canvas.drawText(formatInr(totalGst), 480f, y + 46f, paint)

        paint.color = primaryColor
        canvas.drawRect(310f, y + 54f, 565f, y + 75f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9f
        canvas.drawText("ORDER NET TOTAL:", 325f, y + 68f, paint)
        canvas.drawText(formatInr(totalAmount + totalGst), 475f, y + 68f, paint)

        y += 105f

        // Signatures
        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawLine(50f, y, 190f, y, paint)
        canvas.drawText("Supplier's Confirmation", 55f, y + 14f, paint)

        canvas.drawLine(405f, y, 545f, y, paint)
        canvas.drawText("Himat Textile Representative", 410f, y + 14f, paint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val safeSupp = supplier.name.replace("\\s+".toRegex(), "_")
        val file = File(outputDir, "Supplier_Copy_${safeSupp}_${visit.visitCode}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // 3. CUSTOMER GST TAX INVOICE COPY (NEW)
    // =========================================================================
    fun generateCustomerGstInvoice(
        context: Context,
        visit: VisitEntity,
        customer: CustomerEntity?,
        salesman: EmployeeEntity?,
        entries: List<PurchaseEntryEntity>
    ): File {
        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val primaryColor = Color.rgb(19, 35, 56)
        val goldColor = Color.rgb(200, 157, 60)
        val textDark = Color.rgb(20, 25, 35)
        val textGray = Color.rgb(100, 110, 125)
        val lightBg = Color.rgb(245, 247, 250)
        val tableBorder = Color.rgb(215, 222, 230)

        fun drawGstHeader() {
            // Top Header
            paint.color = primaryColor
            canvas.drawRect(0f, 0f, 595f, 95f, paint)

            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.himat_logo)
            } catch (_: Exception) {
                null
            }

            val logoHeight = 60f
            val logoWidth = if (logoBitmap != null) logoHeight * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat()) else 0f
            val logoLeft = 30f
            val logoTop = 18f
            if (logoBitmap != null) {
                canvas.drawBitmap(logoBitmap, null, RectF(logoLeft, logoTop, logoLeft + logoWidth, logoTop + logoHeight), paint)
            }
            val textStartX = if (logoBitmap != null) (logoLeft + logoWidth + 12f) else 30f

            paint.color = Color.WHITE
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("HIMAT TEXTILE", textStartX, 35f, paint)

            paint.textSize = 9f
            paint.color = Color.rgb(203, 213, 225)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("GOVERNMENT REGISTERED TAX INVOICE", textStartX, 49f, paint)

            paint.textSize = 7.5f
            paint.color = Color.rgb(203, 213, 225)
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Sherkotda, Ahmedabad - 380022", textStartX, 63f, paint)
            canvas.drawText("GSTIN: 24EASPS6621D1ZG • Phone: +91 98739 38095 • State: Gujarat (24)", textStartX, 76f, paint)

            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TAX INVOICE", 435f, 42f, paint)

            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.rgb(220, 230, 245)
            canvas.drawText("Invoice: HT-INV-${visit.visitCode}", 435f, 58f, paint)
            canvas.drawText("Date: ${visit.date}", 435f, 72f, paint)
            canvas.drawText("Place of Supply: Gujarat (24)", 435f, 85f, paint)
        }

        drawGstHeader()

        var y = 110f

        // Billed To & Agent Details Box
        paint.color = lightBg
        canvas.drawRoundRect(30f, y, 565f, y + 65f, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        paint.strokeWidth = 1f
        canvas.drawRoundRect(30f, y, 565f, y + 65f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Customer Details
        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILLED TO / CONSIGNEE:", 42f, y + 17f, paint)
        paint.typeface = Typeface.DEFAULT
        val custFirm = customer?.firmName?.ifBlank { customer.name } ?: visit.customerName
        canvas.drawText(custFirm, 42f, y + 32f, paint)
        paint.color = textGray
        paint.textSize = 8.5f
        val custGstin = if (!customer?.gstin.isNullOrBlank()) "GSTIN: ${customer?.gstin}" else "GSTIN: Unregistered"
        canvas.drawText("${customer?.address?.ifBlank { customer.city } ?: "City: —"} • Phone: ${customer?.phone ?: "—"}", 42f, y + 46f, paint)
        canvas.drawText(custGstin, 42f, y + 58f, paint)

        // Transport & Booking Info
        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DISPATCH & BOOKING DETAILS:", 340f, y + 17f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.color = textGray
        paint.textSize = 8.5f
        val mainTransporter = entries.firstOrNull { it.transporter.isNotBlank() }?.transporter ?: customer?.preferredTransporterName ?: "To be Advised"
        canvas.drawText("Transporter: $mainTransporter", 340f, y + 32f, paint)
        canvas.drawText("Representative: ${salesman?.name ?: visit.employeeName}", 340f, y + 46f, paint)
        canvas.drawText("Reverse Charge: No • Trip Ref: ${visit.visitCode}", 340f, y + 58f, paint)

        y += 80f

        val colX = floatArrayOf(30f, 52f, 150f, 190f, 290f, 330f, 375f, 435f, 495f, 565f)
        val rowBorderPaint = Paint().apply {
            isAntiAlias = true
            color = tableBorder
            strokeWidth = 1f
        }
        val headerBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(71, 85, 105)
            strokeWidth = 1f
        }

        fun drawGstTableHeader() {
            paint.color = primaryColor
            canvas.drawRect(30f, y, 565f, y + 20f, paint)
            paint.color = Color.WHITE
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("S.N.", 33f, y + 13f, paint)
            canvas.drawText("ITEM / STYLE", 56f, y + 13f, paint)
            canvas.drawText("HSN", 154f, y + 13f, paint)
            canvas.drawText("SUPPLIER / MFR", 194f, y + 13f, paint)
            canvas.drawText("QTY", 294f, y + 13f, paint)
            canvas.drawText("RATE", 334f, y + 13f, paint)
            canvas.drawText("TAXABLE", 379f, y + 13f, paint)
            canvas.drawText("GST", 439f, y + 13f, paint)
            canvas.drawText("TOTAL", 499f, y + 13f, paint)

            for (x in colX) {
                canvas.drawLine(x, y, x, y + 20f, headerBorderPaint)
            }
            canvas.drawLine(30f, y, 565f, y, headerBorderPaint)
            canvas.drawLine(30f, y + 20f, 565f, y + 20f, headerBorderPaint)
            y += 20f
        }

        drawGstTableHeader()

        var totalPieces = 0
        var totalTaxable = 0.0
        var totalGst = 0.0
        var totalCases = 0
        var totalLoose = 0

        var sn = 1
        for (item in entries) {
            if (y > 710f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 35f
                drawGstHeader()
                y = 110f
                drawGstTableHeader()
            }

            totalPieces += item.pieces
            totalTaxable += item.totalAmount
            totalGst += item.gstAmount
            totalCases += item.caseCount
            totalLoose += item.loosePieces

            val rowTop = y
            paint.color = textDark
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 7.5f

            canvas.drawText("$sn", 33f, y + 12f, paint)
            val dispItem = if (item.itemCode.length > 15) item.itemCode.take(13) + ".." else item.itemCode
            canvas.drawText(dispItem, 56f, y + 12f, paint)
            canvas.drawText("6203", 154f, y + 12f, paint) // Standard Garments HSN

            val dispSupp = if (item.supplierName.length > 16) item.supplierName.take(14) + ".." else item.supplierName
            canvas.drawText(dispSupp, 194f, y + 12f, paint)

            canvas.drawText("${item.pieces}p", 294f, y + 12f, paint)
            canvas.drawText(formatInr(item.rate), 334f, y + 12f, paint)
            canvas.drawText(formatInr(item.totalAmount), 379f, y + 12f, paint)
            canvas.drawText(formatInr(item.gstAmount), 439f, y + 12f, paint)
            canvas.drawText(formatInr(item.grandTotalWithGst), 499f, y + 12f, paint)

            y += 15f

            // Dispatch Status line for this order
            val isDispatched = item.deliveryStatus.equals("Dispatched", ignoreCase = true) || item.deliveryStatus.equals("Delivered", ignoreCase = true)
            val dispatchLabel = if (isDispatched) {
                val trInfo = if (item.transporter.isNotBlank()) " via ${item.transporter}" else ""
                "[✓] DISPATCHED$trInfo"
            } else {
                "[!] NOT DISPATCHED (${item.deliveryStatus.ifBlank { "Pending" }})"
            }

            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (isDispatched) Color.rgb(16, 128, 60) else Color.rgb(210, 80, 20)
            canvas.drawText("Order: ${item.orderNo} • $dispatchLabel", 56f, y + 8f, paint)

            if (!item.mixedPackNote.isNullOrBlank()) {
                paint.color = textGray
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText("• Note: ${item.mixedPackNote}", 260f, y + 8f, paint)
            }

            y += 13f

            val rowBottom = y
            for (x in colX) {
                canvas.drawLine(x, rowTop, x, rowBottom, rowBorderPaint)
            }
            canvas.drawLine(30f, rowBottom, 565f, rowBottom, rowBorderPaint)
            sn++
        }

        if (y > 640f) {
            pdfDocument.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 35f
            drawGstHeader()
            y = 110f
        }

        y += 10f

        // Tax Breakdown & Totals Box
        val grandTotalWithTax = totalTaxable + totalGst
        val halfGst = totalGst / 2.0

        // Bank Details & Summary
        paint.color = lightBg
        canvas.drawRoundRect(30f, y, 290f, y + 88f, 4f, 4f, paint)
        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BANK PAYMENT DETAILS:", 40f, y + 16f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8f
        paint.color = textGray
        canvas.drawText("Bank: State Bank of India • Ring Road Branch", 40f, y + 32f, paint)
        canvas.drawText("A/C Name: HIMAT TEXTILE", 40f, y + 46f, paint)
        canvas.drawText("A/C No: 39820192831 • IFSC: SBIN0004123", 40f, y + 60f, paint)
        canvas.drawText("UPI / GPay: 9825123456@sbi", 40f, y + 74f, paint)

        // Totals Box
        paint.color = lightBg
        canvas.drawRoundRect(305f, y, 565f, y + 88f, 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        canvas.drawRoundRect(305f, y, 565f, y + 88f, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("Taxable Subtotal (${totalPieces} Pcs):", 318f, y + 16f, paint)
        canvas.drawText(formatInr(totalTaxable), 475f, y + 16f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8f
        canvas.drawText("CGST (2.5%):", 318f, y + 32f, paint)
        canvas.drawText(formatInr(halfGst), 475f, y + 32f, paint)

        canvas.drawText("SGST (2.5%):", 318f, y + 46f, paint)
        canvas.drawText(formatInr(halfGst), 475f, y + 46f, paint)

        paint.color = primaryColor
        canvas.drawRect(305f, y + 58f, 565f, y + 88f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("INVOICE GRAND TOTAL:", 318f, y + 76f, paint)
        canvas.drawText(formatInr(grandTotalWithTax), 465f, y + 76f, paint)

        y += 100f

        // Total in words
        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Amount Chargeable (in words):", 30f, y, paint)
        paint.typeface = Typeface.DEFAULT
        paint.color = primaryColor
        canvas.drawText(convertNumberToWords(grandTotalWithTax.toLong()), 175f, y, paint)

        y += 25f

        // Signatures
        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawLine(50f, y, 190f, y, paint)
        canvas.drawText("Customer's Acceptance", 62f, y + 14f, paint)

        canvas.drawLine(405f, y, 545f, y, paint)
        canvas.drawText("For HIMAT TEXTILE", 430f, y + 14f, paint)
        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Authorized Signatory", 436f, y + 26f, paint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "Customer_GST_Invoice_${visit.visitCode}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // 4. SUPPLIER GST PURCHASE ORDER / TAX INVOICE COPY (NEW)
    // =========================================================================
    fun generateSupplierGstInvoice(
        context: Context,
        visit: VisitEntity,
        supplier: SupplierEntity,
        customer: CustomerEntity?,
        salesman: EmployeeEntity?,
        entries: List<PurchaseEntryEntity>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val primaryColor = Color.rgb(19, 35, 56)
        val goldColor = Color.rgb(200, 157, 60)
        val textDark = Color.rgb(20, 25, 35)
        val textGray = Color.rgb(100, 110, 125)
        val lightBg = Color.rgb(245, 247, 250)
        val tableBorder = Color.rgb(215, 222, 230)

        var y = 35f

        // Top Banner
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, 595f, 95f, paint)

        val logoBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.himat_logo)
        } catch (_: Exception) {
            null
        }

        val logoHeight = 60f
        val logoWidth = if (logoBitmap != null) logoHeight * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat()) else 0f
        val logoLeft = 30f
        val logoTop = 18f
        if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, null, RectF(logoLeft, logoTop, logoLeft + logoWidth, logoTop + logoHeight), paint)
        }
        val textStartX = if (logoBitmap != null) (logoLeft + logoWidth + 12f) else 30f

        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("HIMAT TEXTILE", textStartX, 35f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(203, 213, 225)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GST PURCHASE ORDER & PROCUREMENT INVOICE", textStartX, 49f, paint)

        paint.textSize = 7.5f
        paint.color = Color.rgb(203, 213, 225)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Sherkotda, Ahmedabad - 380022", textStartX, 63f, paint)
        canvas.drawText("GSTIN: 24EASPS6621D1ZG • Phone: +91 98739 38095 • State: Gujarat (24)", textStartX, 76f, paint)

        paint.color = Color.WHITE
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GST PURCHASE ORDER", 410f, 42f, paint)

        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(220, 230, 245)
        val safeSuffix = if (supplier.id.toString().length >= 4) supplier.id.toString().takeLast(4) else "01"
        canvas.drawText("PO Ref: PO-${visit.visitCode}-$safeSuffix", 410f, 58f, paint)
        canvas.drawText("Date: ${visit.date}", 410f, 72f, paint)
        canvas.drawText("Supplier Type: ${supplier.type.uppercase()}", 410f, 85f, paint)

        y = 110f

        // Supplier & Buyer Info
        paint.color = lightBg
        canvas.drawRoundRect(30f, y, 565f, y + 68f, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        paint.strokeWidth = 1f
        canvas.drawRoundRect(30f, y, 565f, y + 68f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Supplier Info
        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SUPPLIER / VENDOR DETAILS:", 42f, y + 17f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(supplier.name, 42f, y + 32f, paint)
        paint.color = textGray
        paint.textSize = 8.5f
        val suppGstin = if (supplier.gstin.isNotBlank()) "GSTIN: ${supplier.gstin}" else "GSTIN: Unregistered"
        canvas.drawText("${supplier.marketArea} • Contact: ${supplier.contactPerson} (${supplier.phone})", 42f, y + 46f, paint)
        if (suppGstin.isNotBlank()) {
            canvas.drawText(suppGstin, 42f, y + 59f, paint)
        }

        // Buyer / Customer Info
        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BUYER / CONCESSIONAIRE:", 340f, y + 17f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(customer?.name ?: visit.customerName, 340f, y + 32f, paint)
        paint.color = textGray
        paint.textSize = 8.5f
        val custGstin = if (!customer?.gstin.isNullOrBlank()) "GSTIN: ${customer?.gstin}" else "GSTIN: Unregistered"
        canvas.drawText("${customer?.city ?: "—"} • Agent: ${salesman?.name ?: visit.employeeName}", 340f, y + 46f, paint)
        canvas.drawText(custGstin, 340f, y + 59f, paint)

        y += 82f

        val colX = floatArrayOf(30f, 52f, 115f, 205f, 245f, 285f, 335f, 415f, 490f, 565f)
        val rowBorderPaint = Paint().apply {
            isAntiAlias = true
            color = tableBorder
            strokeWidth = 1f
        }
        val headerBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(71, 85, 105)
            strokeWidth = 1f
        }

        // Table Header
        paint.color = primaryColor
        canvas.drawRect(30f, y, 565f, y + 20f, paint)
        paint.color = Color.WHITE
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("S.N.", 33f, y + 13f, paint)
        canvas.drawText("ORDER #", 55f, y + 13f, paint)
        canvas.drawText("ITEM / STYLE", 118f, y + 13f, paint)
        canvas.drawText("HSN", 208f, y + 13f, paint)
        canvas.drawText("QTY", 248f, y + 13f, paint)
        canvas.drawText("RATE", 288f, y + 13f, paint)
        canvas.drawText("CASE PKG", 338f, y + 13f, paint)
        canvas.drawText("TAXABLE", 418f, y + 13f, paint)
        canvas.drawText("TOTAL (5%)", 493f, y + 13f, paint)

        for (x in colX) {
            canvas.drawLine(x, y, x, y + 20f, headerBorderPaint)
        }
        canvas.drawLine(30f, y, 565f, y, headerBorderPaint)
        canvas.drawLine(30f, y + 20f, 565f, y + 20f, headerBorderPaint)

        y += 20f

        var totalPcs = 0
        var totalAmount = 0.0
        var totalGst = 0.0
        var totalCases = 0
        var totalLoose = 0

        var sn = 1
        for (item in entries) {
            totalPcs += item.pieces
            totalAmount += item.totalAmount
            totalGst += item.gstAmount
            totalCases += item.caseCount
            totalLoose += item.loosePieces

            val rowTop = y
            paint.color = textDark
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 7.5f

            canvas.drawText("$sn", 33f, y + 12f, paint)
            canvas.drawText(item.orderNo, 55f, y + 12f, paint)
            val dispItem = if (item.itemCode.length > 14) item.itemCode.take(12) + ".." else item.itemCode
            canvas.drawText(dispItem, 118f, y + 12f, paint)
            canvas.drawText("6203", 208f, y + 12f, paint)
            canvas.drawText("${item.pieces}p", 248f, y + 12f, paint)
            canvas.drawText(formatInr(item.rate), 288f, y + 12f, paint)

            val packSplit = if (item.loosePieces > 0) "${item.caseCount}c+${item.loosePieces}L" else "${item.caseCount} cs"
            canvas.drawText(packSplit, 338f, y + 12f, paint)
            canvas.drawText(formatInr(item.totalAmount), 418f, y + 12f, paint)
            canvas.drawText(formatInr(item.grandTotalWithGst), 493f, y + 12f, paint)

            y += 15f

            if (!item.mixedPackNote.isNullOrBlank()) {
                paint.color = textGray
                paint.textSize = 7f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText("↳ Note: ${item.mixedPackNote}", 55f, y + 8f, paint)
                y += 13f
            }

            val rowBottom = y
            for (x in colX) {
                canvas.drawLine(x, rowTop, x, rowBottom, rowBorderPaint)
            }
            canvas.drawLine(30f, rowBottom, 565f, rowBottom, rowBorderPaint)
            sn++
        }

        y += 12f

        // Dispatch Instructions Box
        paint.color = lightBg
        canvas.drawRoundRect(30f, y, 290f, y + 85f, 4f, 4f, paint)
        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DISPATCH & DELIVERY INSTRUCTIONS:", 40f, y + 16f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8f
        val expDate = entries.firstOrNull()?.expectedDeliveryDate?.ifBlank { "Standard / Immediate" } ?: "Standard"
        val transporter = entries.firstOrNull()?.transporter?.ifBlank { "To be advised" } ?: "To be advised"
        val deliveryStatus = entries.firstOrNull()?.deliveryStatus ?: "Pending"
        canvas.drawText("Expected Dispatch Date: $expDate", 40f, y + 32f, paint)
        canvas.drawText("Transporter: $transporter", 40f, y + 46f, paint)
        canvas.drawText("Current Status: $deliveryStatus", 40f, y + 60f, paint)
        canvas.drawText("Invoice Terms: Supplier GST Invoice to accompany goods", 40f, y + 74f, paint)

        // Totals Box
        paint.color = lightBg
        canvas.drawRoundRect(305f, y, 565f, y + 85f, 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        canvas.drawRoundRect(305f, y, 565f, y + 85f, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        val grandTotal = totalAmount + totalGst
        val halfGst = totalGst / 2.0

        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Quantity: $totalPcs Pcs ($totalCases Cases, $totalLoose Loose)", 318f, y + 16f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8f
        canvas.drawText("Taxable Subtotal:", 318f, y + 32f, paint)
        canvas.drawText(formatInr(totalAmount), 475f, y + 32f, paint)

        canvas.drawText("CGST (2.5%) + SGST (2.5%):", 318f, y + 46f, paint)
        canvas.drawText(formatInr(totalGst), 475f, y + 46f, paint)

        paint.color = primaryColor
        canvas.drawRect(305f, y + 56f, 565f, y + 85f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ORDER NET TOTAL:", 318f, y + 74f, paint)
        canvas.drawText(formatInr(grandTotal), 465f, y + 74f, paint)

        y += 100f

        // Total in words
        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total PO Value (in words):", 30f, y, paint)
        paint.typeface = Typeface.DEFAULT
        paint.color = primaryColor
        canvas.drawText(convertNumberToWords(grandTotal.toLong()), 160f, y, paint)

        y += 30f

        // Signatures
        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawLine(50f, y, 190f, y, paint)
        canvas.drawText("Supplier's Confirmation", 60f, y + 14f, paint)

        canvas.drawLine(405f, y, 545f, y, paint)
        canvas.drawText("For HIMAT TEXTILE", 430f, y + 14f, paint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val safeSupp = supplier.name.replace("\\s+".toRegex(), "_")
        val file = File(outputDir, "Supplier_GST_PO_${safeSupp}_${visit.visitCode}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // 5. CUSTOMER CUSTOM DATE RANGE REPORT PDF (NEW - MULTI-PAGE)
    // =========================================================================
    fun generateCustomerDateRangeReport(
        context: Context,
        customer: CustomerEntity,
        entries: List<PurchaseEntryEntity>,
        startDate: String,
        endDate: String,
        statusFilter: String = "All"
    ): File {
        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val primaryColor = Color.rgb(19, 35, 56)
        val goldColor = Color.rgb(200, 157, 60)
        val textDark = Color.rgb(20, 25, 35)
        val textGray = Color.rgb(100, 110, 125)
        val lightBg = Color.rgb(245, 247, 250)
        val tableBorder = Color.rgb(215, 222, 230)

        fun drawDateRangeHeader() {
            paint.color = primaryColor
            canvas.drawRect(0f, 0f, 595f, 95f, paint)

            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.himat_logo)
            } catch (_: Exception) {
                null
            }

            val logoHeight = 60f
            val logoWidth = if (logoBitmap != null) logoHeight * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat()) else 0f
            val logoLeft = 30f
            val logoTop = 18f
            if (logoBitmap != null) {
                canvas.drawBitmap(logoBitmap, null, RectF(logoLeft, logoTop, logoLeft + logoWidth, logoTop + logoHeight), paint)
            }
            val textStartX = if (logoBitmap != null) (logoLeft + logoWidth + 12f) else 30f

            paint.color = Color.WHITE
            paint.textSize = 17f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("HIMAT TEXTILE", textStartX, 35f, paint)

            paint.textSize = 9f
            paint.color = Color.rgb(203, 213, 225)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CUSTOMER STATEMENT & ORDER REPORT", textStartX, 49f, paint)

            paint.textSize = 7.5f
            paint.color = Color.rgb(203, 213, 225)
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Sherkotda, Ahmedabad - 380022", textStartX, 63f, paint)
            canvas.drawText("GSTIN: 24EASPS6621D1ZG • Phone: +91 98739 38095", textStartX, 76f, paint)

            paint.color = Color.WHITE
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ACCOUNT STATEMENT", 415f, 42f, paint)

            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.rgb(220, 230, 245)
            canvas.drawText("Period: $startDate to $endDate", 415f, 58f, paint)
            canvas.drawText("Status Filter: $statusFilter", 415f, 72f, paint)
            val todayStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            canvas.drawText("Generated: $todayStr", 415f, 85f, paint)
        }

        drawDateRangeHeader()

        var y = 110f

        // Customer Profile Card
        paint.color = lightBg
        canvas.drawRoundRect(30f, y, 565f, y + 55f, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        paint.strokeWidth = 1f
        canvas.drawRoundRect(30f, y, 565f, y + 55f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = textDark
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val custFirm = customer.firmName.ifBlank { customer.name }
        canvas.drawText("CUSTOMER: $custFirm", 42f, y + 17f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.color = textGray
        paint.textSize = 8.5f
        val custGstin = if (customer.gstin.isNotBlank()) "GSTIN: ${customer.gstin}" else "GSTIN: Unregistered"
        canvas.drawText("Prop: ${customer.name} • ID: ${customer.customerId} • City: ${customer.city}", 42f, y + 32f, paint)
        canvas.drawText("Phone: ${customer.phone} • $custGstin", 42f, y + 46f, paint)

        // Summary metric counts
        val totalPieces = entries.sumOf { it.pieces }
        val totalAmount = entries.sumOf { it.totalAmount }
        val totalGst = entries.sumOf { it.gstAmount }
        val grandTotal = totalAmount + totalGst
        val totalCases = entries.sumOf { it.caseCount }
        val totalLoose = entries.sumOf { it.loosePieces }

        val dispatchedEntries = entries.filter { it.deliveryStatus.equals("Dispatched", ignoreCase = true) || it.deliveryStatus.equals("Delivered", ignoreCase = true) }
        val pendingEntries = entries.filter { !it.deliveryStatus.equals("Dispatched", ignoreCase = true) && !it.deliveryStatus.equals("Delivered", ignoreCase = true) }
        val dispatchedPieces = dispatchedEntries.sumOf { it.pieces }
        val pendingPieces = pendingEntries.sumOf { it.pieces }

        y += 65f

        // Summary Metric Ribbon (4 stats)
        val statBoxWidth = (565f - 30f - 18f) / 4f
        val statTitles = listOf("TOTAL ORDERS", "TOTAL PIECES", "DISPATCHED", "PENDING DISPATCH")
        val statValues = listOf(
            "${entries.size} Orders",
            "$totalPieces Pcs",
            "$dispatchedPieces Pcs",
            "$pendingPieces Pcs"
        )
        val statColors = listOf(
            primaryColor,
            Color.rgb(15, 118, 110),
            Color.rgb(16, 128, 60),
            if (pendingPieces > 0) Color.rgb(215, 80, 20) else Color.rgb(16, 128, 60)
        )

        for (i in 0..3) {
            val sx = 30f + i * (statBoxWidth + 6f)
            paint.color = lightBg
            canvas.drawRoundRect(sx, y, sx + statBoxWidth, y + 36f, 4f, 4f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = tableBorder
            canvas.drawRoundRect(sx, y, sx + statBoxWidth, y + 36f, 4f, 4f, paint)
            paint.style = Paint.Style.FILL

            paint.color = textGray
            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(statTitles[i], sx + 8f, y + 13f, paint)

            paint.color = statColors[i]
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(statValues[i], sx + 8f, y + 27f, paint)
        }

        y += 46f

        val colX = floatArrayOf(30f, 52f, 135f, 255f, 335f, 400f, 455f, 510f, 565f)
        val rowBorderPaint = Paint().apply {
            isAntiAlias = true
            color = tableBorder
            strokeWidth = 1f
        }
        val headerBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(71, 85, 105)
            strokeWidth = 1f
        }

        fun drawDateRangeTableHeader() {
            paint.color = primaryColor
            canvas.drawRect(30f, y, 565f, y + 20f, paint)
            paint.color = Color.WHITE
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("S.N.", 33f, y + 13f, paint)
            canvas.drawText("ORDER #", 55f, y + 13f, paint)
            canvas.drawText("SUPPLIER / MFR", 138f, y + 13f, paint)
            canvas.drawText("ITEM / STYLE", 258f, y + 13f, paint)
            canvas.drawText("QTY / PKG", 338f, y + 13f, paint)
            canvas.drawText("RATE", 403f, y + 13f, paint)
            canvas.drawText("TOTAL", 458f, y + 13f, paint)
            canvas.drawText("STATUS", 513f, y + 13f, paint)

            for (x in colX) {
                canvas.drawLine(x, y, x, y + 20f, headerBorderPaint)
            }
            canvas.drawLine(30f, y, 565f, y, headerBorderPaint)
            canvas.drawLine(30f, y + 20f, 565f, y + 20f, headerBorderPaint)
            y += 20f
        }

        drawDateRangeTableHeader()

        if (entries.isEmpty()) {
            paint.color = textDark
            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("No purchase entries found for this customer in the selected date range.", 100f, y + 30f, paint)
            y += 50f
        } else {
            var sn = 1
            for (item in entries) {
                if (y > 720f) {
                    pdfDocument.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 35f
                    drawDateRangeHeader()
                    y = 110f
                    drawDateRangeTableHeader()
                }

                val rowTop = y
                paint.color = textDark
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 7.5f

                canvas.drawText("$sn", 33f, y + 12f, paint)
                canvas.drawText(item.orderNo, 55f, y + 12f, paint)

                val suppName = if (item.supplierName.length > 20) item.supplierName.take(18) + ".." else item.supplierName
                canvas.drawText(suppName, 138f, y + 12f, paint)

                val itemCode = if (item.itemCode.length > 13) item.itemCode.take(11) + ".." else item.itemCode
                canvas.drawText(itemCode, 258f, y + 12f, paint)

                val packDesc = "${item.pieces}p (${item.caseCount}c)"
                canvas.drawText(packDesc, 338f, y + 12f, paint)

                canvas.drawText(formatInr(item.rate), 403f, y + 12f, paint)
                canvas.drawText(formatInr(item.grandTotalWithGst), 458f, y + 12f, paint)

                // Dispatch Status in the table row
                val isDispatched = item.deliveryStatus.equals("Dispatched", ignoreCase = true) || item.deliveryStatus.equals("Delivered", ignoreCase = true)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = if (item.deliveryStatus.equals("Delivered", ignoreCase = true)) Color.rgb(22, 163, 74) else if (isDispatched) Color.rgb(37, 99, 235) else Color.rgb(215, 80, 20)
                val statusShort = if (item.deliveryStatus.equals("Delivered", ignoreCase = true)) "Delivered" else if (isDispatched) "Dispatched" else "Pending"
                canvas.drawText(statusShort, 513f, y + 12f, paint)

                y += 14f

                // Detailed Transporter & Note sub-line
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 7f
                paint.color = textGray
                val transInfo = if (item.transporter.isNotBlank()) "Transporter: ${item.transporter}" else "Transporter: To be Advised"
                val noteInfo = if (!item.mixedPackNote.isNullOrBlank()) " • Note: ${item.mixedPackNote}" else ""
                canvas.drawText("↳ $transInfo$noteInfo", 55f, y + 8f, paint)

                y += 12f
                val rowBottom = y
                for (x in colX) {
                    canvas.drawLine(x, rowTop, x, rowBottom, rowBorderPaint)
                }
                canvas.drawLine(30f, rowBottom, 565f, rowBottom, rowBorderPaint)
                sn++
            }
        }

        if (y > 670f) {
            pdfDocument.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 35f
            drawDateRangeHeader()
            y = 110f
        }

        y += 10f

        // Grand Totals Box
        paint.color = lightBg
        canvas.drawRoundRect(280f, y, 565f, y + 70f, 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        canvas.drawRoundRect(280f, y, 565f, y + 70f, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Orders / Pieces:", 295f, y + 16f, paint)
        canvas.drawText("${entries.size} Orders / $totalPieces Pcs ($totalCases Cases)", 415f, y + 16f, paint)

        canvas.drawText("Dispatched Goods:", 295f, y + 32f, paint)
        canvas.drawText("$dispatchedPieces Pcs (${dispatchedEntries.size} Orders)", 415f, y + 32f, paint)

        paint.color = primaryColor
        canvas.drawRect(280f, y + 44f, 565f, y + 70f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL VALUE (WITH GST):", 295f, y + 60f, paint)
        canvas.drawText(formatInr(grandTotal), 465f, y + 60f, paint)

        y += 90f

        // Footer & Signature
        paint.color = textGray
        paint.textSize = 7.5f
        canvas.drawText("• This statement reflects all recorded purchase transactions and live dispatch statuses.", 30f, y, paint)
        canvas.drawText("• For any discrepancies or transporter receipt inquiries, contact Himat Textile office.", 30f, y + 11f, paint)

        y += 35f
        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawLine(50f, y, 190f, y, paint)
        canvas.drawText("Customer Verification", 65f, y + 14f, paint)

        canvas.drawLine(405f, y, 545f, y, paint)
        canvas.drawText("For HIMAT TEXTILE", 430f, y + 14f, paint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val safeCust = customer.name.replace("\\s+".toRegex(), "_")
        val file = File(outputDir, "Customer_Statement_${safeCust}_${startDate}_to_${endDate}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }
}
