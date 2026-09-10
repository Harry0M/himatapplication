package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object PdfGenerator {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun formatInr(amount: Double): String {
        return "₹" + String.format(Locale.US, "%,.2f", amount)
    }

    /**
     * Generates a Customer Day Report PDF
     */
    fun generateCustomerDayReport(
        context: Context,
        visit: VisitEntity,
        customer: CustomerEntity?,
        salesman: EmployeeEntity?,
        entries: List<PurchaseEntryEntity>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val primaryColor = Color.rgb(19, 35, 56) // Navy Primary
        val goldColor = Color.rgb(200, 157, 60) // Gold Accent
        val textDark = Color.rgb(20, 25, 35)
        val textGray = Color.rgb(100, 110, 125)
        val lightBg = Color.rgb(245, 247, 250)
        val tableBorder = Color.rgb(220, 225, 232)

        var y = 35f

        // Top Header Banner
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, 595f, 95f, paint)

        // Company Title
        paint.color = Color.WHITE
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("HIMAT TEXTILE", 30f, 40f, paint)

        paint.textSize = 10f
        paint.color = goldColor
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("GARMENT SOURCING AGENCY • WHOLESALE TO RETAIL FACILITATOR", 30f, 56f, paint)

        paint.textSize = 8.5f
        paint.color = Color.rgb(200, 210, 225)
        canvas.drawText("Market Escort & Spot Procurement Logs • Multi-Supplier Consolidated Billing", 30f, 72f, paint)

        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMER DAY REPORT", 410f, 45f, paint)

        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(220, 230, 245)
        canvas.drawText("Visit: ${visit.visitCode}", 410f, 62f, paint)
        canvas.drawText("Date: ${visit.date}", 410f, 76f, paint)

        y = 115f

        // Customer & Salesman Info Box
        paint.color = lightBg
        canvas.drawRoundRect(30f, y, 565f, y + 60f, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        paint.strokeWidth = 1f
        canvas.drawRoundRect(30f, y, 565f, y + 60f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Info Details
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

        // Table Header
        paint.color = primaryColor
        canvas.drawRect(30f, y, 565f, y + 20f, paint)
        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("ORDER #", 36f, y + 13f, paint)
        canvas.drawText("SUPPLIER / MANUFACTURER", 95f, y + 13f, paint)
        canvas.drawText("ITEM / STYLE", 260f, y + 13f, paint)
        canvas.drawText("PCS", 340f, y + 13f, paint)
        canvas.drawText("RATE", 375f, y + 13f, paint)
        canvas.drawText("CASES", 425f, y + 13f, paint)
        canvas.drawText("AMOUNT", 495f, y + 13f, paint)

        y += 20f

        var totalPieces = 0
        var totalAmount = 0.0
        var totalGst = 0.0
        var totalCases = 0
        var totalLoose = 0

        val grouped = entries.groupBy { it.supplierName }

        for ((supplierName, supplierEntries) in grouped) {
            paint.color = Color.rgb(238, 242, 248)
            canvas.drawRect(30f, y, 565f, y + 16f, paint)
            paint.color = primaryColor
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val supType = supplierEntries.firstOrNull()?.supplierType ?: ""
            canvas.drawText("▶ $supplierName ($supType)", 38f, y + 11f, paint)
            y += 16f

            for (item in supplierEntries) {
                totalPieces += item.pieces
                totalAmount += item.totalAmount
                totalGst += item.gstAmount
                totalCases += item.caseCount
                totalLoose += item.loosePieces

                paint.color = textDark
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 8f
                canvas.drawText(item.orderNo, 36f, y + 12f, paint)

                val displaySupp = if (item.supplierName.length > 30) item.supplierName.take(28) + ".." else item.supplierName
                canvas.drawText(displaySupp, 95f, y + 12f, paint)
                canvas.drawText(item.itemCode, 260f, y + 12f, paint)
                canvas.drawText("${item.pieces}", 340f, y + 12f, paint)
                canvas.drawText(formatInr(item.rate), 375f, y + 12f, paint)

                val packDesc = if (item.loosePieces > 0) "${item.caseCount}c + ${item.loosePieces}L" else "${item.caseCount} cases"
                canvas.drawText(packDesc, 425f, y + 12f, paint)
                canvas.drawText(formatInr(item.totalAmount), 495f, y + 12f, paint)

                y += 15f

                // Mixed pack note if applicable
                if (!item.mixedPackNote.isNullOrBlank()) {
                    paint.color = Color.rgb(180, 110, 20)
                    paint.textSize = 7.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    canvas.drawText("↳ NOTE: ${item.mixedPackNote}", 95f, y + 8f, paint)
                    y += 12f
                }

                // Row separator
                paint.color = tableBorder
                canvas.drawLine(30f, y, 565f, y, paint)
            }
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
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("• This consolidated report is generated by Himat Textile for internal coordination and retailer verification.", 30f, y, paint)
        canvas.drawText("• Delivery & goods receipt subject to individual supplier dispatch terms & transporter bilty.", 30f, y + 12f, paint)
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

    /**
     * Generates a Supplier Bill / Purchase Copy PDF
     */
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

        paint.color = Color.WHITE
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("HIMAT TEXTILE", 30f, 40f, paint)

        paint.textSize = 10f
        paint.color = goldColor
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("GARMENT SOURCING AGENCY • SUPPLIER PURCHASE COPY", 30f, 56f, paint)

        paint.textSize = 8.5f
        paint.color = Color.rgb(200, 210, 225)
        canvas.drawText("Official Purchase Order & Spot Booking Voucher", 30f, 72f, paint)

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

        // Table Header
        paint.color = primaryColor
        canvas.drawRect(30f, y, 565f, y + 20f, paint)
        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("ORDER #", 36f, y + 13f, paint)
        canvas.drawText("ITEM / STYLE CODE", 110f, y + 13f, paint)
        canvas.drawText("PCS", 220f, y + 13f, paint)
        canvas.drawText("RATE", 265f, y + 13f, paint)
        canvas.drawText("CASE SIZE", 320f, y + 13f, paint)
        canvas.drawText("PACKING (CASE/LOOSE)", 390f, y + 13f, paint)
        canvas.drawText("AMOUNT", 500f, y + 13f, paint)

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

            paint.color = textDark
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8f
            canvas.drawText(item.orderNo, 36f, y + 12f, paint)
            canvas.drawText(item.itemCode, 110f, y + 12f, paint)
            canvas.drawText("${item.pieces}", 220f, y + 12f, paint)
            canvas.drawText(formatInr(item.rate), 265f, y + 12f, paint)
            canvas.drawText("${item.caseSize} pcs/cs", 320f, y + 12f, paint)

            val packSplit = if (item.loosePieces > 0) "${item.caseCount} Case + ${item.loosePieces} Loose" else "${item.caseCount} Full Cases"
            canvas.drawText(packSplit, 390f, y + 12f, paint)
            canvas.drawText(formatInr(item.totalAmount), 500f, y + 12f, paint)

            y += 15f

            // Crucial: Packing note showing what goods were mixed with!
            if (!item.mixedPackNote.isNullOrBlank()) {
                paint.color = Color.rgb(180, 110, 20)
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                canvas.drawText("PACKING INSTRUCTION: ${item.mixedPackNote}", 110f, y + 8f, paint)
                y += 13f
            }

            paint.color = tableBorder
            canvas.drawLine(30f, y, 565f, y, paint)
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
}
