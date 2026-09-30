package com.example.util

import android.content.Context
import android.graphics.Bitmap
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Choices from the "Order Form PDF" sheet. The supplier's GSTIN and market area are optional
 * extras (off unless switched on); the order nature is printed as given (editable by the user).
 */
data class SupplierOrderFormOptions(
    val showGstin: Boolean = false,
    val showMarketArea: Boolean = false,
    val orderNature: String = ORDER_NATURE_SELF
) {
    companion object {
        const val ORDER_NATURE_SELF = "Self Order"
        const val ORDER_NATURE_WHATSAPP = "WhatsApp Order"
    }
}

object PdfGenerator {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // Our own details printed in every light-theme header
    private const val COMPANY_ADDRESS = "First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Ahmedabad, Gujarat 380022"
    private const val COMPANY_CONTACT = "GSTIN: 24EASPS6621D1ZG  |  Phone: +91 98739 38095"

    /**
     * Splits [text] into at most [maxLines] lines that each fit [maxWidth] at [paint]'s current size.
     * The last line ends with ".." when there was more to say. Used for notes that have to live
     * inside a narrow table column.
     */
    private fun wrapToWidth(paint: Paint, text: String, maxWidth: Float, maxLines: Int = 2): List<String> {
        if (text.isBlank() || maxWidth <= 0f) return emptyList()
        val words = text.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                if (lines.size == maxLines) break
                // A single word longer than the column: cut it so it still fits
                current = if (paint.measureText(word) > maxWidth) {
                    var cut = word
                    while (cut.length > 2 && paint.measureText("$cut..") > maxWidth) cut = cut.dropLast(1)
                    StringBuilder("$cut..")
                } else StringBuilder(word)
            }
        }
        if (lines.size < maxLines && current.isNotEmpty()) lines.add(current.toString())
        if (lines.size == maxLines) {
            val consumed = lines.joinToString(" ")
            if (consumed.length < text.trim().length) {
                var last = lines[maxLines - 1]
                while (last.length > 2 && paint.measureText("$last..") > maxWidth) last = last.dropLast(1)
                lines[maxLines - 1] = "$last.."
            }
        }
        return lines
    }

    /** Draws [text], shrinking the size (not below [minSize]) until it fits [maxWidth]; restores the size. */
    private fun drawTextFitted(canvas: Canvas, paint: Paint, text: String, x: Float, baseline: Float, maxWidth: Float, minSize: Float = 5.5f) {
        val size = paint.textSize
        while (paint.textSize > minSize && paint.measureText(text) > maxWidth) paint.textSize -= 0.1f
        canvas.drawText(text, x, baseline, paint)
        paint.textSize = size
    }

    // -------------------------------------------------------------------------
    // Small teal glyphs used as section markers on the light-theme sheets.
    // -------------------------------------------------------------------------

    private fun drawStoreGlyph(c: Canvas, x: Float, y: Float, fill: Paint) {
        val p = android.graphics.Path().apply {
            moveTo(x, y + 3f)
            lineTo(x + 5.5f, y)
            lineTo(x + 11f, y + 3f)
            lineTo(x + 11f, y + 5f)
            lineTo(x, y + 5f)
            close()
        }
        c.drawPath(p, fill)
        c.drawRect(x + 1f, y + 5f, x + 10f, y + 11f, fill)
        c.drawRect(x + 4f, y + 7f, x + 7f, y + 11f, Paint().apply { color = Color.WHITE; isAntiAlias = true })
    }

    private fun drawTruckGlyph(c: Canvas, x: Float, y: Float, fill: Paint) {
        c.drawRoundRect(RectF(x, y + 1f, x + 7.5f, y + 8.5f), 1f, 1f, fill)
        c.drawRoundRect(RectF(x + 7.5f, y + 3.5f, x + 11f, y + 8.5f), 1f, 1f, fill)
        c.drawCircle(x + 2.5f, y + 9.5f, 1.3f, fill)
        c.drawCircle(x + 8.8f, y + 9.5f, 1.3f, fill)
    }

    private fun drawPersonGlyph(c: Canvas, x: Float, y: Float, fill: Paint) {
        c.drawCircle(x + 5.5f, y + 3.5f, 2.8f, fill)
        c.drawRoundRect(RectF(x + 1f, y + 7.5f, x + 10f, y + 12f), 2f, 2f, fill)
    }

    private fun drawBankGlyph(c: Canvas, x: Float, y: Float, fill: Paint) {
        val p = android.graphics.Path().apply {
            moveTo(x, y + 3.5f)
            lineTo(x + 5.5f, y)
            lineTo(x + 11f, y + 3.5f)
            close()
        }
        c.drawPath(p, fill)
        c.drawRect(x + 1.5f, y + 4f, x + 3.2f, y + 9f, fill)
        c.drawRect(x + 4.7f, y + 4f, x + 6.4f, y + 9f, fill)
        c.drawRect(x + 7.9f, y + 4f, x + 9.6f, y + 9f, fill)
        c.drawRect(x, y + 9f, x + 11f, y + 11f, fill)
    }

    private fun drawShieldGlyph(c: Canvas, x: Float, y: Float, fill: Paint) {
        val p = android.graphics.Path().apply {
            moveTo(x + 5.5f, y)
            lineTo(x + 11f, y + 2.5f)
            lineTo(x + 11f, y + 7f)
            quadTo(x + 11f, y + 11f, x + 5.5f, y + 12f)
            quadTo(x, y + 11f, x, y + 7f)
            lineTo(x, y + 2.5f)
            close()
        }
        c.drawPath(p, fill)
    }

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

    fun generateQrCodeBitmap(content: String, size: Int): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    // =========================================================================
    // 1. CUSTOMER PURCHASE REPORT (Exact replica of Reference UI)
    // =========================================================================
    fun generateCustomerDayReport(
        context: Context,
        visit: VisitEntity,
        customer: CustomerEntity?,
        salesman: EmployeeEntity?,
        entries: List<PurchaseEntryEntity>,
        options: com.example.ui.components.CustomerReportOptions = com.example.ui.components.CustomerReportOptions()
    ): File {
        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val primaryDark = Color.rgb(15, 23, 42) // Slate 900 #0f172a
        val tealAccent = Color.rgb(15, 118, 110) // Teal 700 #0f766e
        val yellowAccent = Color.rgb(217, 119, 6) // Golden Yellow #d97706
        val textDark = Color.rgb(15, 23, 42)
        val textMuted = Color.rgb(100, 116, 139) // Slate 500 #64748b
        val textHeader = Color.rgb(71, 85, 105) // Slate 600 #475569
        val borderLight = Color.rgb(226, 232, 240) // Slate 200 #e2e8f0
        val tableHeaderBg = Color.rgb(248, 250, 252) // Slate 50 #f8fafc
        val tableDivider = Color.rgb(226, 232, 240)
        val altRowBg = Color.rgb(250, 250, 250)

        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = borderLight
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val tealIconPaint = Paint().apply {
            isAntiAlias = true
            color = tealAccent
            style = Paint.Style.FILL
        }

        fun drawStoreIcon(c: Canvas, x: Float, y: Float) {
            val p = android.graphics.Path().apply {
                moveTo(x, y + 3f)
                lineTo(x + 5.5f, y)
                lineTo(x + 11f, y + 3f)
                lineTo(x + 11f, y + 5f)
                lineTo(x, y + 5f)
                close()
            }
            c.drawPath(p, tealIconPaint)
            c.drawRect(x + 1f, y + 5f, x + 10f, y + 11f, tealIconPaint)
            val whiteP = Paint().apply { color = Color.WHITE; isAntiAlias = true }
            c.drawRect(x + 4f, y + 7f, x + 7f, y + 11f, whiteP)
        }

        fun drawTruckIcon(c: Canvas, x: Float, y: Float) {
            c.drawRoundRect(RectF(x, y + 1f, x + 7.5f, y + 8.5f), 1f, 1f, tealIconPaint)
            c.drawRoundRect(RectF(x + 7.5f, y + 3.5f, x + 11f, y + 8.5f), 1f, 1f, tealIconPaint)
            c.drawCircle(x + 2.5f, y + 9.5f, 1.3f, tealIconPaint)
            c.drawCircle(x + 8.8f, y + 9.5f, 1.3f, tealIconPaint)
        }

        fun drawPersonIcon(c: Canvas, x: Float, y: Float) {
            c.drawCircle(x + 5.5f, y + 3.5f, 2.8f, tealIconPaint)
            c.drawRoundRect(RectF(x + 1f, y + 7.5f, x + 10f, y + 12f), 2f, 2f, tealIconPaint)
        }

        fun drawBankIcon(c: Canvas, x: Float, y: Float) {
            val p = android.graphics.Path().apply {
                moveTo(x, y + 3.5f)
                lineTo(x + 5.5f, y)
                lineTo(x + 11f, y + 3.5f)
                close()
            }
            c.drawPath(p, tealIconPaint)
            c.drawRect(x + 1.5f, y + 4f, x + 3.2f, y + 9f, tealIconPaint)
            c.drawRect(x + 4.7f, y + 4f, x + 6.4f, y + 9f, tealIconPaint)
            c.drawRect(x + 7.9f, y + 4f, x + 9.6f, y + 9f, tealIconPaint)
            c.drawRect(x, y + 9f, x + 11f, y + 11f, tealIconPaint)
        }

        fun drawLinkIcon(c: Canvas, x: Float, y: Float) {
            val strokeP = Paint().apply {
                color = tealAccent
                style = Paint.Style.STROKE
                strokeWidth = 1.3f
                isAntiAlias = true
            }
            c.drawRoundRect(RectF(x, y + 1f, x + 6f, y + 7f), 2f, 2f, strokeP)
            c.drawRoundRect(RectF(x + 4f, y + 4f, x + 10f, y + 10f), 2f, 2f, strokeP)
        }

        fun drawShieldIcon(c: Canvas, x: Float, y: Float) {
            val p = android.graphics.Path().apply {
                moveTo(x + 5.5f, y)
                lineTo(x + 11f, y + 2.5f)
                lineTo(x + 11f, y + 7f)
                quadTo(x + 11f, y + 11f, x + 5.5f, y + 12f)
                quadTo(x, y + 11f, x, y + 7f)
                lineTo(x, y + 2.5f)
                close()
            }
            c.drawPath(p, tealIconPaint)
        }

        fun drawHeader() {
            // Logo on Left
            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.himat_logo)
            } catch (_: Exception) {
                null
            }

            val logoHeight = 44f
            val logoWidth = if (logoBitmap != null) logoHeight * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat()) else 0f
            val logoLeft = 28f
            val logoTop = 18f
            if (logoBitmap != null) {
                canvas.drawBitmap(logoBitmap, null, RectF(logoLeft, logoTop, logoLeft + logoWidth, logoTop + logoHeight), paint)
            }
            val textStartX = if (logoBitmap != null) (logoLeft + logoWidth + 10f) else 28f

            // HIMAT TEXTILE
            paint.color = primaryDark
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("HIMAT TEXTILE", textStartX, 32f, paint)

            // YOUR GARMENT GUIDE ACROSS INDIA (Yellow)
            paint.textSize = 7.5f
            paint.color = yellowAccent
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("YOUR GARMENT GUIDE ACROSS INDIA", textStartX, 44f, paint)

            // Address & Contact info: bold and darker so our details read clearly
            paint.textSize = 7f
            paint.color = Color.rgb(51, 65, 85)
            paint.typeface = Typeface.DEFAULT_BOLD
            val detailsWidth = 372f - 8f - textStartX
            drawTextFitted(canvas, paint, COMPANY_ADDRESS, textStartX, 54f, detailsWidth)
            drawTextFitted(canvas, paint, COMPANY_CONTACT, textStartX, 64f, detailsWidth)

            // Vertical divider between company info and report title
            canvas.drawLine(372f, 18f, 372f, 68f, borderPaint)

            // Right header: CUSTOMER (Teal) + PURCHASE REPORT (Dark)
            paint.color = tealAccent
            paint.textSize = 13.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CUSTOMER", 385f, 30f, paint)

            paint.color = primaryDark
            canvas.drawText("PURCHASE REPORT", 385f, 44f, paint)

            // Meta Line 1: Visit ID
            paint.color = textMuted
            paint.textSize = 7.8f
            paint.typeface = Typeface.DEFAULT
            val docX = 385f
            // Little document icon
            canvas.drawRect(docX, 50f, docX + 6.5f, 58f, Paint().apply { color = textMuted; style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true })
            canvas.drawText("Visit ID: ${visit.visitCode}", docX + 11f, 56.5f, paint)

            // Meta Line 2: Date
            canvas.drawRect(docX, 61f, docX + 6.5f, 69f, Paint().apply { color = textMuted; style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true })
            val orderDate = visit.date.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
            canvas.drawText("Date: $orderDate", docX + 11f, 67.5f, paint)
        }

        drawHeader()

        var y = 74f

        // Top Customer & Transport Details Card (Height: 74f)
        val cardHeight = 74f
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(28f, y, 567f, y + cardHeight), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(28f, y, 567f, y + cardHeight), 6f, 6f, borderPaint)

        // Customer Details (Left side: 28f..300f)
        drawStoreIcon(canvas, 38f, y + 10f)
        paint.color = textHeader
        paint.textSize = 7.2f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMER / SHOP DETAILS", 53f, y + 18f, paint)

        val customerBrand = (customer?.firmName?.takeIf { it.isNotBlank() } ?: customer?.name ?: visit.customerName).uppercase()
        paint.color = primaryDark
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val dispBrand = if (customerBrand.length > 34) customerBrand.take(32) + ".." else customerBrand
        canvas.drawText(dispBrand, 38f, y + 32f, paint)

        val customerOwner = customer?.name.orEmpty().ifBlank { "Mr. Jitendra Bhai" }
        val customerPhone = if (options.showPhone) customer?.phone.orEmpty().ifBlank { "+91 98765 43210" } else "—"
        val customerGstin = if (options.showGstin) (customer?.gstin?.takeIf { it.isNotBlank() } ?: "Unregistered") else "—"
        val customerAddress = if (options.showAddress) {
            customer?.shopAddress?.takeIf { it.isNotBlank() }
                ?: customer?.city?.let { "$it${if (!customer.state.isNullOrBlank()) ", ${customer.state}" else ""}" }
                ?: "Ahiliyanagar, Maharashtra"
        } else "—"

        val kvStartY = y + 43f
        val kvSpacing = 9.8f
        val colonXCust = 86f
        val valXCust = 94f

        val custKeys = arrayOf("Proprietor", "Phone", "GSTIN", "Address")
        val custVals = arrayOf(customerOwner, customerPhone, customerGstin, customerAddress)

        for (i in custKeys.indices) {
            val lineY = kvStartY + (i * kvSpacing)
            paint.color = textMuted
            paint.textSize = 7.5f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(custKeys[i], 38f, lineY, paint)
            canvas.drawText(":", colonXCust, lineY, paint)
            paint.color = textDark
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val dispVal = if (custVals[i].length > 35) custVals[i].take(33) + ".." else custVals[i]
            canvas.drawText(dispVal, valXCust, lineY, paint)
        }

        // Transport Details (Right side: 315f..567f)
        if (options.showTransportSection) {
            drawTruckIcon(canvas, 315f, y + 10f)
            paint.color = textHeader
            paint.textSize = 7.2f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TRANSPORT & BOOKING DETAILS", 330f, y + 18f, paint)

            val primaryTransporter = entries.find { it.transporter.isNotBlank() }?.transporter
                ?: customer?.preferredTransporterName?.takeIf { it.isNotBlank() }
                ?: "Shree Maruti Transport"
            val bookingStation = customer?.transportPreference?.takeIf { it.isNotBlank() }
                ?: customer?.city?.takeIf { it.isNotBlank() }?.let { "$it (${it.take(3).uppercase()})" }
                ?: "Ahmedabad (ADI)"
            val lrNo = "—"
            val dispatchDate = visit.date.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
            val deliveryTo = customer?.city?.let { "$it${if (!customer.state.isNullOrBlank()) ", ${customer.state}" else ""}" } ?: "Ahiliyanagar, Maharashtra"

            // Build filtered list of transport rows based on options
            val transRows = mutableListOf<Pair<String, String>>()
            if (options.showTransporter)    transRows += "Transporter" to primaryTransporter
            if (options.showBookingStation) transRows += "Booking Station" to bookingStation
            if (options.showLrNo)           transRows += "LR / Booking No." to lrNo
            if (options.showDispatchDate)   transRows += "Dispatch Date" to dispatchDate
            if (options.showDeliveryTo)     transRows += "Delivery To" to deliveryTo

            val colonXTrans = 385f
            val valXTrans = 393f

            transRows.forEachIndexed { i, (key, value) ->
                val lineY = (y + 32f) + (i * kvSpacing)
                paint.color = textMuted
                paint.textSize = 7.5f
                paint.typeface = Typeface.DEFAULT
                canvas.drawText(key, 315f, lineY, paint)
                canvas.drawText(":", colonXTrans, lineY, paint)
                paint.color = textDark
                paint.typeface = if (i == 0 || i == 1) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                val dispVal = if (value.length > 30) value.take(28) + ".." else value
                canvas.drawText(dispVal, valXTrans, lineY, paint)
            }
        }

        y += cardHeight + 8f

        // 11 Table Columns across 539f (from 28f to 567f):
        // Col boundaries: 28f, 48f, 96f, 154f, 218f, 244f, 286f, 338f, 386f, 434f, 494f, 567f
        val colX = floatArrayOf(28f, 48f, 96f, 154f, 218f, 244f, 286f, 338f, 386f, 434f, 494f, 567f)

        fun drawTableHeader() {
            paint.color = tableHeaderBg
            canvas.drawRect(28f, y, 567f, y + 18f, paint)
            canvas.drawLine(28f, y, 567f, y, borderPaint)
            canvas.drawLine(28f, y + 18f, 567f, y + 18f, borderPaint)

            paint.color = primaryDark
            paint.textSize = 6.6f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("SR. NO.", 38f - (paint.measureText("SR. NO.") / 2f), y + 11.5f, paint)
            canvas.drawText("ORDER NO.", 51f, y + 11.5f, paint)
            canvas.drawText("BRAND", 100f, y + 11.5f, paint)
            canvas.drawText("ITEM / STYLE", 158f, y + 11.5f, paint)
            canvas.drawText("PCS", 231f - (paint.measureText("PCS") / 2f), y + 11.5f, paint)
            canvas.drawText("RATE (₹)", 282f - paint.measureText("RATE (₹)"), y + 11.5f, paint)
            canvas.drawText("AMOUNT (₹)", 334f - paint.measureText("AMOUNT (₹)"), y + 11.5f, paint)
            canvas.drawText("CASE & PACKING", 362f - (paint.measureText("CASE & PACKING") / 2f), y + 11.5f, paint)
            canvas.drawText("PURCHASE DATE", 410f - (paint.measureText("PURCHASE DATE") / 2f), y + 11.5f, paint)
            canvas.drawText("SALESMAN", 438f, y + 11.5f, paint)
            canvas.drawText("STATUS", 530.5f - (paint.measureText("STATUS") / 2f), y + 11.5f, paint)

            for (x in colX) {
                canvas.drawLine(x, y, x, y + 18f, borderPaint)
            }
            y += 18f
        }

        drawTableHeader()

        var totalPieces = 0
        var totalAmount = 0.0
        var totalGst = 0.0
        var totalCases = 0
        var totalLoose = 0

        entries.forEachIndexed { index, item ->
            // Check page height limit
            if (y > 680f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
                y = 80f
                drawTableHeader()
            }

            totalPieces += item.pieces
            totalAmount += item.totalAmount
            totalGst += item.gstAmount
            totalCases += item.caseCount
            totalLoose += item.loosePieces

            val rowTop = y
            // A packing note is printed inside the CASE & PACKING column, so the row grows a little
            // instead of pushing a full-width note row underneath the order.
            val rowHeight = if (item.mixedPackNote.isNullOrBlank()) 15.5f else 25f

            // Alternate row background
            if (index % 2 == 1) {
                paint.color = altRowBg
                canvas.drawRect(28f, rowTop, 567f, rowTop + rowHeight, paint)
            }

            paint.color = textDark
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 6.8f

            // 1. SR. NO.
            val srStr = "${index + 1}"
            canvas.drawText(srStr, 38f - (paint.measureText(srStr) / 2f), y + 10.5f, paint)

            // 2. ORDER NO.
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val orderNo = if (item.orderNo.isNotBlank()) item.orderNo else "HT-${2620 + index}"
            val dispO = if (orderNo.length > 10) orderNo.take(8) + ".." else orderNo
            canvas.drawText(dispO, 51f, y + 10.5f, paint)

            // 3. BRAND
            val brand = item.supplierName.ifBlank { "—" }
            val dispB = if (brand.length > 12) brand.take(10) + ".." else brand
            canvas.drawText(dispB, 100f, y + 10.5f, paint)

            // 4. ITEM / STYLE
            val itemCode = item.itemCode.ifBlank { "—" }
            val dispI = if (itemCode.length > 13) itemCode.take(11) + ".." else itemCode
            canvas.drawText(dispI, 158f, y + 10.5f, paint)

            // 5. PCS
            val pcStr = "${item.pieces}"
            canvas.drawText(pcStr, 231f - (paint.measureText(pcStr) / 2f), y + 10.5f, paint)

            // 6. RATE (₹)
            val rate = if (item.rate > 0) item.rate else 0.0
            val rateStr = String.format(Locale.US, "%,.2f", rate)
            canvas.drawText(rateStr, 282f - paint.measureText(rateStr), y + 10.5f, paint)

            // 7. AMOUNT (₹)
            val amt = if (item.totalAmount > 0) item.totalAmount else (item.pieces * rate)
            val amtStr = String.format(Locale.US, "%,.2f", amt)
            canvas.drawText(amtStr, 334f - paint.measureText(amtStr), y + 10.5f, paint)

            // 8. CASE & PACKING — the mixed-pack note belongs to packing, so it is printed inside
            // this column (on a second line) instead of on a separate row under the whole order.
            val packDesc = if (item.loosePieces > 0) "${item.caseCount}c+${item.loosePieces}L" else "${item.caseCount} cs"
            val packNote = item.mixedPackNote?.trim().orEmpty()
            val packColLeft = 338f
            val packColRight = 386f
            val packCentre = (packColLeft + packColRight) / 2f
            canvas.drawText(packDesc, packCentre - (paint.measureText(packDesc) / 2f), y + 10.5f, paint)
            if (packNote.isNotEmpty()) {
                val notePaint = Paint(paint).apply {
                    textSize = 5.4f
                    color = textMuted
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                }
                // Two short lines keep it inside the 48pt column instead of spilling into RATE
                val noteLines = wrapToWidth(notePaint, packNote, packColRight - packColLeft - 4f, maxLines = 2)
                noteLines.forEachIndexed { line, text ->
                    canvas.drawText(
                        text,
                        packCentre - (notePaint.measureText(text) / 2f),
                        y + 16.5f + (line * 5.8f),
                        notePaint
                    )
                }
            }

            // 9. PURCHASE DATE
            val pDate = (item.orderDate.takeIf { it.isNotBlank() } ?: visit.date).take(10)
            canvas.drawText(pDate, 410f - (paint.measureText(pDate) / 2f), y + 10.5f, paint)

            // 10. SALESMAN
            val smName = item.salesmanName.takeIf { it.isNotBlank() } ?: (salesman?.name ?: visit.employeeName).ifBlank { "Jalam Bhai" }
            val dispSm = if (smName.length > 10) smName.take(8) + ".." else smName
            canvas.drawText(dispSm, 438f, y + 10.5f, paint)

            // 11. STATUS (Badge)
            val status = item.deliveryStatus.ifBlank { "Pending" }
            val (statusBg, statusFg) = when (status.lowercase()) {
                "delivered" -> Pair(Color.rgb(220, 252, 231), Color.rgb(22, 101, 52))
                "dispatched" -> Pair(Color.rgb(219, 234, 254), Color.rgb(30, 64, 175))
                else -> Pair(Color.rgb(254, 243, 199), Color.rgb(180, 83, 9))
            }
            val statusWidth = 58f
            val statusHeight = 11f
            val statusLeft = 494f + ((567f - 494f - statusWidth) / 2f)
            // Always centred on the first line, so a taller note row does not drop the badge
            val statusTop = y + ((15.5f - statusHeight) / 2f)
            paint.color = statusBg
            canvas.drawRoundRect(RectF(statusLeft, statusTop, statusLeft + statusWidth, statusTop + statusHeight), 3f, 3f, paint)
            paint.color = statusFg
            paint.textSize = 6.2f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(status, statusLeft + ((statusWidth - paint.measureText(status)) / 2f), statusTop + 8f, paint)

            y += rowHeight

            val rowBottom = y
            for (x in colX) {
                canvas.drawLine(x, rowTop, x, rowBottom, borderPaint)
            }
            canvas.drawLine(28f, rowBottom, 567f, rowBottom, borderPaint)
        }

        // Check if there is enough space for Totals + 3-Column Card + Alert Banner + Footer (~170f)
        if (y > 630f) {
            pdfDocument.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            drawHeader()
            y = 80f
        }

        y += 8f

        // Totals Box (Right aligned: 347f..567f, width 220f)
        val totLeft = 347f
        val totRight = 567f
        val totWidth = totRight - totLeft

        paint.color = textDark
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Subtotal (${totalPieces} Pcs)", totLeft + 8f, y + 10f, paint)
        val subStr = "₹" + String.format(Locale.US, "%,.2f", totalAmount)
        canvas.drawText(subStr, totRight - 8f - paint.measureText(subStr), y + 10f, paint)

        canvas.drawText("Garment GST (5%)", totLeft + 8f, y + 22f, paint)
        val gstStr = "₹" + String.format(Locale.US, "%,.2f", totalGst)
        canvas.drawText(gstStr, totRight - 8f - paint.measureText(gstStr), y + 22f, paint)

        // Grand Total highlighted pill row
        val grandTotal = totalAmount + totalGst
        val pillTop = y + 28f
        val pillHeight = 20f
        paint.color = Color.rgb(238, 242, 246) // Soft slate-teal/blue background #eef2f6
        canvas.drawRoundRect(RectF(totLeft, pillTop, totRight, pillTop + pillHeight), 4f, 4f, paint)

        paint.color = primaryDark
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Grand Total", totLeft + 8f, pillTop + 14f, paint)

        val grandStr = "₹" + String.format(Locale.US, "%,.2f", grandTotal)
        paint.textSize = 11f
        canvas.drawText(grandStr, totRight - 8f - paint.measureText(grandStr), pillTop + 14.5f, paint)

        y += 56f

        // Bottom 3-Column Info Card (Height: 74f)
        val bCardHeight = 74f
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(28f, y, 567f, y + bCardHeight), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(28f, y, 567f, y + bCardHeight), 6f, 6f, borderPaint)

        val divX1 = 200f
        val divX2 = 425f
        canvas.drawLine(divX1, y, divX1, y + bCardHeight, borderPaint)
        canvas.drawLine(divX2, y, divX2, y + bCardHeight, borderPaint)

        // Col 1: Salesman Details (28f..200f)
        drawPersonIcon(canvas, 38f, y + 10f)
        paint.color = primaryDark
        paint.textSize = 7.2f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SALESMAN DETAILS", 52f, y + 18f, paint)

        // Every salesman who worked these orders (they may have been entered from several phones)
        val tripSalesmen = salesmenForReport(visit, entries)
        val activeSalesmanName = tripSalesmen.joinToString(", ")
            .ifBlank { (salesman?.name ?: visit.employeeName) }
            .ifBlank { "Jalam Bhai" }
        val activeSalesmanPhone = salesman?.phone.orEmpty().ifBlank { "+91 98739 38095" }
        val activeSalesmanEmail = salesman?.email.orEmpty().ifBlank { "jalam@himattextile.com" }

        // When a Sub Agent brought this customer, their name goes right under the salesman's,
        // so the customer can see both people who look after them.
        val dayReportSubAgent = customer?.subAgentName?.trim().orEmpty()
        val smKeys: Array<String>
        val smVals: Array<String>
        if (dayReportSubAgent.isNotBlank()) {
            smKeys = arrayOf(
                if (tripSalesmen.size > 1) "Salesmen" else "Salesman Name",
                "Sub Agent",
                "Contact Number",
                "WhatsApp",
                "Email"
            )
            smVals = arrayOf(activeSalesmanName, dayReportSubAgent, activeSalesmanPhone, activeSalesmanPhone, activeSalesmanEmail)
        } else {
            smKeys = arrayOf(if (tripSalesmen.size > 1) "Salesmen" else "Salesman Name", "Contact Number", "WhatsApp", "Email")
            smVals = arrayOf(activeSalesmanName, activeSalesmanPhone, activeSalesmanPhone, activeSalesmanEmail)
        }
        val colonXSm = 88f
        val valXSm = 94f

        // With the extra Sub Agent line the five rows are packed a little tighter to stay in the card
        val smLineGap = if (smKeys.size > 4) 8.6f else 10f
        for (i in smKeys.indices) {
            val lineY = y + 29f + (i * smLineGap)
            paint.color = textMuted
            paint.textSize = 7f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(smKeys[i], 38f, lineY, paint)
            canvas.drawText(":", colonXSm, lineY, paint)
            paint.color = textDark
            paint.typeface = if (i <= 1) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            // The names line can hold several salesmen, so it gets more room than the other lines
            val maxLen = if (i == 0) 27 else 19
            val disp = if (smVals[i].length > maxLen) smVals[i].take(maxLen - 2) + ".." else smVals[i]
            canvas.drawText(disp, valXSm, lineY, paint)
        }

        // Col 2: Bank Account Details (200f..425f)
        drawBankIcon(canvas, 210f, y + 10f)
        paint.color = primaryDark
        paint.textSize = 7.2f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BANK ACCOUNT DETAILS (HIMAT TEXTILE)", 224f, y + 18f, paint)

        val bankKeys = arrayOf("Bank Name", "Account Name", "Account Number", "IFSC Code", "Type & Branch")
        val bankVals = arrayOf("ICICI Bank", "Himat Textile", "136805501447", "ICIC0000189", "Current • Ashram Rd")
        val colonXBank = 274f
        val valXBank = 280f

        for (i in bankKeys.indices) {
            val lineY = y + 28f + (i * 9.2f)
            paint.color = textMuted
            paint.textSize = 7f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(bankKeys[i], 210f, lineY, paint)
            canvas.drawText(":", colonXBank, lineY, paint)
            paint.color = textDark
            paint.typeface = if (i == 0 || i == 1 || i == 2 || i == 3) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            canvas.drawText(bankVals[i], valXBank, lineY, paint)
        }

        // Col 3: Scan to Pay via UPI QR (425f..567f)
        val col3CenterX = (divX2 + 567f) / 2f
        drawShieldIcon(canvas, col3CenterX - 48f, y + 10f)
        paint.color = primaryDark
        paint.textSize = 6.8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val scanTitle = "SCAN TO PAY (UPI QR)"
        canvas.drawText(scanTitle, col3CenterX - (paint.measureText(scanTitle) / 2f) + 6f, y + 18f, paint)

        val paymentQrBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.payment_qr)
        } catch (_: Exception) {
            null
        } ?: generateQrCodeBitmap("upi://pay?pa=eazypay.0000053310@icici&pn=ICICI Bank InstaBIZ Merchant&tr=EZYS0000053310&cu=INR&mc=5999", 160)

        val qrSize = 38f
        val qrLeft = col3CenterX - (qrSize / 2f)
        val qrTop = y + 21f
        if (paymentQrBitmap != null) {
            canvas.drawBitmap(paymentQrBitmap, null, RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), paint)
        }

        paint.color = textDark
        paint.textSize = 6.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val upiIdText = "eazypay.0000053310@icici"
        canvas.drawText(upiIdText, col3CenterX - (paint.measureText(upiIdText) / 2f), y + 68f, paint)

        y += bCardHeight + 8f

        // Note Alert Ribbon (Pink/red banner with red exclamation circle).
        //
        // Two lines now. The dispatch policy used to sit up in the terms grid where it was easy to
        // miss; the one thing a customer must not miss is that this is a draft, and the one thing the
        // transporter must not miss is one bill per LR. So both live here, and the policy gets its own
        // colour so the two lines do not read as one sentence.
        val alertHeight = 29f
        paint.color = Color.rgb(254, 242, 242) // #fef2f2
        canvas.drawRoundRect(RectF(28f, y, 567f, y + alertHeight), 4f, 4f, paint)
        val alertBorderPaint = Paint().apply {
            color = Color.rgb(254, 202, 202) // #fecaca
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(28f, y, 567f, y + alertHeight), 4f, 4f, alertBorderPaint)

        // Red Circle with White '!'
        val circleX = 39f
        val circleY = y + 9f
        canvas.drawCircle(circleX, circleY, 5.5f, Paint().apply { color = Color.rgb(220, 38, 38); isAntiAlias = true })
        val whiteExclP = Paint().apply {
            color = Color.WHITE
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("!", circleX - 1.8f, circleY + 2.5f, whiteExclP)

        // Line 1 — this is a draft, in red
        paint.color = Color.rgb(185, 28, 28)
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DRAFT BILL:", 49f, y + 11f, paint)

        paint.color = Color.rgb(153, 27, 27)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(
            "This is not an actual invoice. All amounts are approximate and may vary from the final supplier invoice / dispatch quantity.",
            97f, y + 11f, paint
        )

        // Line 2 — the dispatch policy, on its own highlight so it reads as a separate instruction
        val policyText = "DISPATCH POLICY: One Bill One LR is strictly mandatory."
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val policyWidth = paint.measureText(policyText) + 12f
        canvas.drawRoundRect(
            RectF(49f, y + 15f, 49f + policyWidth, y + 26f),
            2.5f,
            2.5f,
            Paint().apply { color = Color.rgb(254, 243, 199); isAntiAlias = true } // amber #fef3c7
        )
        paint.color = Color.rgb(146, 64, 14) // amber-800, clearly not the red above
        canvas.drawText(policyText, 55f, y + 23f, paint)

        // Footer at bottom of page
        val footerY = 822f
        canvas.drawLine(28f, footerY - 10f, 567f, footerY - 10f, borderPaint)

        paint.color = yellowAccent
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("YOUR GARMENT GUIDE ACROSS INDIA", 28f, footerY, paint)

        val footerRight = AgencyProfile.load(context).pdfFooterLine()
        paint.color = textDark
        canvas.drawText(footerRight, 567f - paint.measureText(footerRight), footerY, paint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        // Named after the customer's brand (shop / firm) so the chat shows whose report it is
        val reportBrand = customer?.brandName()?.takeIf { it.isNotBlank() } ?: visit.customerName
        // "Quotation", not "Report": what the customer receives is a priced offer, not a statement,
        // and the file name is the first thing they read in the chat.
        val file = File(outputDir, PdfFileNames.build(reportBrand, "Customer Quotation", visit.visitCode))
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // 2. SUPPLIER ORDER FORM (DRAFT BILL - Matching Reference UI Layout)
    // =========================================================================
    fun generateSupplierCopy(
        context: Context,
        visit: VisitEntity,
        supplier: SupplierEntity,
        customer: CustomerEntity?,
        salesman: EmployeeEntity?,
        entries: List<PurchaseEntryEntity>,
        options: SupplierOrderFormOptions = SupplierOrderFormOptions()
    ): File {
        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val primaryDark = Color.rgb(15, 23, 42) // Slate 900
        val tealAccent = Color.rgb(15, 118, 110) // Teal 700
        val yellowAccent = Color.rgb(217, 119, 6) // Golden Yellow #d97706
        val textDark = Color.rgb(15, 23, 42)
        val textMuted = Color.rgb(71, 85, 105) // Slate 600: labels darker than before, easier to read
        val textHeader = Color.rgb(51, 65, 85) // Slate 700
        val borderLight = Color.rgb(226, 232, 240)
        val tableHeaderBg = Color.rgb(248, 250, 252)
        val altRowBg = Color.rgb(250, 250, 250)

        // Heavier type on the order form: labels medium, values bold, key figures extra bold (black)
        val tfMedium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val tfBold: Typeface = Typeface.DEFAULT_BOLD
        val tfBlack: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)

        /** [text] cut with ".." so it fits [maxWidth] at the current paint settings. */
        fun fit(text: String, maxWidth: Float): String {
            if (text.isEmpty() || paint.measureText(text) <= maxWidth) return text
            var end = text.length
            val dots = paint.measureText("..")
            while (end > 1 && paint.measureText(text, 0, end) + dots > maxWidth) end--
            return text.substring(0, end).trimEnd() + ".."
        }

        /** Shrinks the text size (down to [minSize]) before cutting, so long values stay readable. */
        fun drawFitted(text: String, x: Float, baseline: Float, maxWidth: Float, minSize: Float) {
            val size = paint.textSize
            while (paint.textSize > minSize && paint.measureText(text) > maxWidth) paint.textSize -= 0.2f
            canvas.drawText(fit(text, maxWidth), x, baseline, paint)
            paint.textSize = size
        }

        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = borderLight
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val tealIconPaint = Paint().apply {
            isAntiAlias = true
            color = tealAccent
            style = Paint.Style.FILL
        }

        fun drawStoreIcon(c: Canvas, x: Float, y: Float) {
            val p = android.graphics.Path().apply {
                moveTo(x, y + 3f)
                lineTo(x + 5.5f, y)
                lineTo(x + 11f, y + 3f)
                lineTo(x + 11f, y + 5f)
                lineTo(x, y + 5f)
                close()
            }
            c.drawPath(p, tealIconPaint)
            c.drawRect(x + 1f, y + 5f, x + 10f, y + 11f, tealIconPaint)
            val whiteP = Paint().apply { color = Color.WHITE; isAntiAlias = true }
            c.drawRect(x + 4f, y + 7f, x + 7f, y + 11f, whiteP)
        }

        fun drawShieldIcon(c: Canvas, x: Float, y: Float) {
            val p = android.graphics.Path().apply {
                moveTo(x + 5.5f, y)
                lineTo(x + 11f, y + 2.5f)
                lineTo(x + 11f, y + 7f)
                quadTo(x + 11f, y + 11f, x + 5.5f, y + 12f)
                quadTo(x, y + 11f, x, y + 7f)
                lineTo(x, y + 2.5f)
                close()
            }
            c.drawPath(p, tealIconPaint)
        }

        fun drawPersonIcon(c: Canvas, x: Float, y: Float) {
            c.drawCircle(x + 5.5f, y + 3.5f, 2.8f, tealIconPaint)
            c.drawRoundRect(RectF(x + 1f, y + 7.5f, x + 10f, y + 12f), 2f, 2f, tealIconPaint)
        }

        fun drawTermsIcon(c: Canvas, x: Float, y: Float) {
            c.drawRoundRect(RectF(x + 1f, y, x + 10f, y + 11f), 1.5f, 1.5f, tealIconPaint)
            val whiteP = Paint().apply { color = Color.WHITE; strokeWidth = 1f; isAntiAlias = true }
            c.drawLine(x + 3f, y + 4f, x + 8f, y + 4f, whiteP)
            c.drawLine(x + 3f, y + 6.5f, x + 8f, y + 6.5f, whiteP)
            c.drawLine(x + 3f, y + 9f, x + 6.5f, y + 9f, whiteP)
        }

        fun drawLinkIcon(c: Canvas, x: Float, y: Float) {
            val strokeP = Paint().apply {
                color = tealAccent
                style = Paint.Style.STROKE
                strokeWidth = 1.3f
                isAntiAlias = true
            }
            c.drawRoundRect(RectF(x, y + 1f, x + 6f, y + 7f), 2f, 2f, strokeP)
            c.drawRoundRect(RectF(x + 4f, y + 4f, x + 10f, y + 10f), 2f, 2f, strokeP)
        }

        fun drawHeader() {
            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.himat_logo)
            } catch (_: Exception) {
                null
            }

            val logoHeight = 44f
            val logoWidth = if (logoBitmap != null) logoHeight * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat()) else 0f
            val logoLeft = 28f
            val logoTop = 18f
            if (logoBitmap != null) {
                canvas.drawBitmap(logoBitmap, null, RectF(logoLeft, logoTop, logoLeft + logoWidth, logoTop + logoHeight), paint)
            }
            val textStartX = if (logoBitmap != null) (logoLeft + logoWidth + 10f) else 28f

            paint.color = primaryDark
            paint.textSize = 18f
            paint.typeface = tfBlack
            canvas.drawText("HIMAT TEXTILE", textStartX, 32f, paint)

            paint.textSize = 7.5f
            paint.color = yellowAccent
            paint.typeface = tfBlack
            canvas.drawText("YOUR GARMENT GUIDE ACROSS INDIA", textStartX, 44f, paint)

            // Our address and contact: bold and darker so they read clearly
            paint.textSize = 7f
            paint.color = textHeader
            paint.typeface = tfBold
            val detailsWidth = 372f - 8f - textStartX
            drawFitted(COMPANY_ADDRESS, textStartX, 54f, detailsWidth, 5.6f)
            drawFitted(COMPANY_CONTACT, textStartX, 64f, detailsWidth, 5.6f)

            canvas.drawLine(372f, 18f, 372f, 68f, borderPaint)

            // Right header: ORDER FORM (Dark)
            paint.color = primaryDark
            paint.textSize = 15f
            paint.typeface = tfBlack
            canvas.drawText("ORDER FORM", 385f, 38f, paint)

            paint.color = textMuted
            paint.textSize = 7.8f
            paint.typeface = tfBold
            val docX = 385f
            canvas.drawRect(docX, 50f, docX + 6.5f, 58f, Paint().apply { color = textMuted; style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true })
            canvas.drawText("Trip Code: ${visit.visitCode}", docX + 11f, 56.5f, paint)

            canvas.drawRect(docX, 61f, docX + 6.5f, 69f, Paint().apply { color = textMuted; style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true })
            val orderDate = visit.date.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
            canvas.drawText("Date: $orderDate", docX + 11f, 67.5f, paint)
        }

        drawHeader()

        var y = 74f

        // Top Supplier & Buyer Trade Account Details Card
        val cardHeight = 82f
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(28f, y, 567f, y + cardHeight), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(28f, y, 567f, y + cardHeight), 6f, 6f, borderPaint)

        // Supplier Details (Left side: 28f..300f)
        drawStoreIcon(canvas, 38f, y + 10f)
        paint.color = textHeader
        paint.textSize = 7.4f
        paint.typeface = tfBlack
        canvas.drawText("SUPPLIER DETAILS", 53f, y + 18f, paint)

        val suppBrand = supplier.brandName().uppercase()
        paint.color = primaryDark
        paint.textSize = 12f
        paint.typeface = tfBlack
        drawFitted(suppBrand, 38f, y + 32f, 256f, 9f)

        // Firm name and the supplier's own primary phone always; GSTIN / market area only when chosen
        val suppFirm = supplier.name.trim().ifBlank { supplier.firmName.trim() }.ifBlank { supplier.brandName() }
        val suppRows = buildList {
            add("Firm Name" to suppFirm)
            add("Phone" to supplier.primaryPhone().ifBlank { "—" })
            if (options.showGstin) add("GSTIN" to supplier.gstin.trim().ifBlank { "Unregistered" })
            if (options.showMarketArea) {
                add(
                    "Market Area" to supplier.marketArea.trim()
                        .ifBlank { supplier.marketName.trim() }
                        .ifBlank { supplier.city.trim() }
                        .ifBlank { "—" }
                )
            }
        }

        val kvStartY = y + 45f
        val kvSpacing = 10.5f
        val colonXSupp = 88f
        val valXSupp = 96f

        suppRows.forEachIndexed { i, (key, value) ->
            val lineY = kvStartY + (i * kvSpacing)
            paint.color = textMuted
            paint.textSize = 7.6f
            paint.typeface = tfMedium
            canvas.drawText(key, 38f, lineY, paint)
            canvas.drawText(":", colonXSupp, lineY, paint)
            paint.color = textDark
            paint.typeface = tfBold
            canvas.drawText(fit(value, 300f - valXSupp), valXSupp, lineY, paint)
        }

        // Customer / Buyer Details (Right side: 315f..567f - Protected Wholesale Identity)
        drawShieldIcon(canvas, 315f, y + 10f)
        paint.color = textHeader
        paint.textSize = 7.4f
        paint.typeface = tfBlack
        canvas.drawText("CUSTOMER", 330f, y + 18f, paint)

        val custId = if (!customer?.customerId.isNullOrBlank()) customer?.customerId!! else if (customer != null && customer.id > 0) "CUST-${customer.id}" else if (visit.customerId > 0) "CUST-${visit.customerId}" else "CUST-TRADE"
        paint.color = primaryDark
        paint.textSize = 12f
        paint.typeface = tfBlack
        drawFitted(custId, 315f, y + 32f, 245f, 9f)

        val buyerCity = customer?.transportPreference?.takeIf { it.isNotBlank() }
            ?: customer?.city?.trim()?.takeIf { it.isNotBlank() }?.let { city ->
                val state = customer.state?.trim().orEmpty()
                if (state.isNotBlank() && !city.contains(state, ignoreCase = true)) "$city, $state" else city
            } ?: "—"
        val colonXBuyer = 400f
        val valXBuyer = 408f
        val buyerValueWidth = 560f - valXBuyer

        // Destination station is the line the supplier dispatches to, so it is printed larger
        val destY = y + 47f
        paint.color = textMuted
        paint.textSize = 7.6f
        paint.typeface = tfMedium
        canvas.drawText("Destination Station", 315f, destY, paint)
        canvas.drawText(":", colonXBuyer, destY, paint)
        paint.color = primaryDark
        paint.textSize = 10.5f
        paint.typeface = tfBlack
        drawFitted(buyerCity, valXBuyer, destY, buyerValueWidth, 8f)

        val buyerRows = listOf(
            "Dispatch Policy" to "One Bill One LR",
            "Order Nature" to options.orderNature.trim().ifBlank { SupplierOrderFormOptions.ORDER_NATURE_SELF }
        )
        buyerRows.forEachIndexed { i, (key, value) ->
            val lineY = destY + 13f + (i * 11f)
            paint.color = textMuted
            paint.textSize = 7.6f
            paint.typeface = tfMedium
            canvas.drawText(key, 315f, lineY, paint)
            canvas.drawText(":", colonXBuyer, lineY, paint)
            paint.color = textDark
            paint.typeface = tfBold
            canvas.drawText(fit(value, buyerValueWidth), valXBuyer, lineY, paint)
        }

        y += cardHeight + 8f

        // 7 Table Columns across 539f (from 28f to 567f):
        // Col boundaries: 28f, 60f, 130f, 270f, 310f, 385f, 470f, 567f
        val colX = floatArrayOf(28f, 60f, 130f, 270f, 310f, 385f, 470f, 567f)

        fun drawTableHeader() {
            paint.color = tableHeaderBg
            canvas.drawRect(28f, y, 567f, y + 18f, paint)
            canvas.drawLine(28f, y, 567f, y, borderPaint)
            canvas.drawLine(28f, y + 18f, 567f, y + 18f, borderPaint)

            paint.color = primaryDark
            paint.textSize = 6.8f
            paint.typeface = tfBlack

            canvas.drawText("SR. NO.", 44f - (paint.measureText("SR. NO.") / 2f), y + 11.5f, paint)
            canvas.drawText("ORDER NO.", 64f, y + 11.5f, paint)
            canvas.drawText("ITEM / STYLE", 134f, y + 11.5f, paint)
            canvas.drawText("PCS", 290f - (paint.measureText("PCS") / 2f), y + 11.5f, paint)
            canvas.drawText("RATE (₹)", 381f - paint.measureText("RATE (₹)"), y + 11.5f, paint)
            canvas.drawText("AMOUNT (₹)", 466f - paint.measureText("AMOUNT (₹)"), y + 11.5f, paint)
            canvas.drawText("CASE & PACKING", 518.5f - (paint.measureText("CASE & PACKING") / 2f), y + 11.5f, paint)

            for (x in colX) {
                canvas.drawLine(x, y, x, y + 18f, borderPaint)
            }
            y += 18f
        }

        drawTableHeader()

        var totalPieces = 0
        var totalAmount = 0.0
        var totalGst = 0.0
        var totalCases = 0
        var totalLoose = 0

        entries.forEachIndexed { index, item ->
            if (y > 680f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
                y = 80f
                drawTableHeader()
            }

            totalPieces += item.pieces
            totalAmount += item.totalAmount
            totalGst += item.gstAmount
            totalCases += item.caseCount
            totalLoose += item.loosePieces

            val rowTop = y
            val rowHeight = 15.5f

            if (index % 2 == 1) {
                paint.color = altRowBg
                canvas.drawRect(28f, rowTop, 567f, rowTop + rowHeight, paint)
            }

            paint.color = textDark
            paint.typeface = tfBold
            paint.textSize = 7.2f

            // SR. NO.
            val srStr = "${index + 1}"
            canvas.drawText(srStr, 44f - (paint.measureText(srStr) / 2f), y + 10.5f, paint)

            // ORDER NO.
            val orderNo = if (item.orderNo.isNotBlank()) {
                if (item.orderNo.startsWith("#")) item.orderNo else "#${item.orderNo}"
            } else "#HT-${2620 + index}"
            canvas.drawText(fit(orderNo, 130f - 64f - 3f), 64f, y + 10.5f, paint)

            // ITEM / STYLE
            val itemCode = item.itemCode.ifBlank { "—" }
            canvas.drawText(fit(itemCode, 270f - 134f - 3f), 134f, y + 10.5f, paint)

            // PCS
            val pcStr = "${item.pieces}"
            paint.typeface = tfBlack
            canvas.drawText(pcStr, 290f - (paint.measureText(pcStr) / 2f), y + 10.5f, paint)
            paint.typeface = tfBold

            val rate = if (item.rate > 0) item.rate else 0.0
            val rateStr = String.format(Locale.US, "%,.2f", rate)
            canvas.drawText(rateStr, 381f - paint.measureText(rateStr), y + 10.5f, paint)

            // APPROX AMOUNT
            val amt = if (item.totalAmount > 0) item.totalAmount else (item.pieces * rate)
            val amtStr = String.format(Locale.US, "%,.2f", amt)
            paint.typeface = tfBlack
            canvas.drawText(amtStr, 466f - paint.measureText(amtStr), y + 10.5f, paint)
            paint.typeface = tfBold

            // CASE & PACKING
            val packDesc = if (item.loosePieces > 0) "${item.caseCount}c+${item.loosePieces}L" else "${item.caseCount} cs"
            canvas.drawText(packDesc, 518.5f - (paint.measureText(packDesc) / 2f), y + 10.5f, paint)

            y += rowHeight
            val itemRowBottom = y

            if (!item.mixedPackNote.isNullOrBlank()) {
                paint.color = Color.rgb(250, 250, 250)
                canvas.drawRect(28f, y, 567f, y + 12f, paint)
                paint.color = textMuted
                paint.textSize = 6.6f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                canvas.drawText(fit("  ↳ PACKING INSTRUCTION: ${item.mixedPackNote}", 567f - 64f - 4f), 64f, y + 8.5f, paint)
                y += 12f
            }

            val rowBottom = y
            for (x in colX) {
                canvas.drawLine(x, rowTop, x, itemRowBottom, borderPaint)
            }
            // The packing note spans the full width: only the outer borders run through it
            if (rowBottom > itemRowBottom) {
                canvas.drawLine(colX.first(), itemRowBottom, colX.first(), rowBottom, borderPaint)
                canvas.drawLine(colX.last(), itemRowBottom, colX.last(), rowBottom, borderPaint)
            }
            canvas.drawLine(28f, rowBottom, 567f, rowBottom, borderPaint)
        }

        if (y > 630f) {
            pdfDocument.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            drawHeader()
            y = 80f
        }

        y += 8f

        // Totals Box (Right aligned: 347f..567f)
        val totLeft = 347f
        val totRight = 567f

        paint.color = textDark
        paint.textSize = 8f
        paint.typeface = tfBold
        canvas.drawText("Subtotal (${totalPieces} Pcs)", totLeft + 8f, y + 10f, paint)
        val subStr = "₹" + String.format(Locale.US, "%,.2f", totalAmount)
        canvas.drawText(subStr, totRight - 8f - paint.measureText(subStr), y + 10f, paint)

        canvas.drawText("Approx Garment GST (5%)", totLeft + 8f, y + 22f, paint)
        val gstStr = "₹" + String.format(Locale.US, "%,.2f", totalGst)
        canvas.drawText(gstStr, totRight - 8f - paint.measureText(gstStr), y + 22f, paint)

        val grandTotal = totalAmount + totalGst
        val pillTop = y + 28f
        val pillHeight = 20f
        paint.color = Color.rgb(238, 242, 246)
        canvas.drawRoundRect(RectF(totLeft, pillTop, totRight, pillTop + pillHeight), 4f, 4f, paint)

        paint.color = primaryDark
        paint.textSize = 10f
        paint.typeface = tfBlack
        canvas.drawText("Approx Net Total", totLeft + 8f, pillTop + 14f, paint)

        val grandStr = "₹" + String.format(Locale.US, "%,.2f", grandTotal)
        paint.textSize = 11f
        canvas.drawText(grandStr, totRight - 8f - paint.measureText(grandStr), pillTop + 14.5f, paint)

        y += 56f

        // Bottom 3-Column Info Card (Height: 74f)
        val bCardHeight = 74f
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(28f, y, 567f, y + bCardHeight), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(28f, y, 567f, y + bCardHeight), 6f, 6f, borderPaint)

        val divX1 = 200f
        val divX2 = 425f
        canvas.drawLine(divX1, y, divX1, y + bCardHeight, borderPaint)
        canvas.drawLine(divX2, y, divX2, y + bCardHeight, borderPaint)

        // Col 1: Salesman Details (28f..200f)
        drawPersonIcon(canvas, 38f, y + 10f)
        paint.color = primaryDark
        paint.textSize = 7.2f
        paint.typeface = tfBlack
        canvas.drawText("SALESMAN DETAILS", 52f, y + 18f, paint)

        // Every salesman who worked these orders (they may have been entered from several phones)
        val tripSalesmen = salesmenForReport(visit, entries)
        val activeSalesmanName = tripSalesmen.joinToString(", ")
            .ifBlank { (salesman?.name ?: visit.employeeName) }
            .ifBlank { "Jalam Bhai" }
        val activeSalesmanPhone = salesman?.phone.orEmpty().ifBlank { "+91 98739 38095" }

        // No email on the supplier's copy: name, contact number and WhatsApp only
        val smKeys = arrayOf(if (tripSalesmen.size > 1) "Salesmen" else "Salesman Name", "Contact Number", "WhatsApp")
        val smVals = arrayOf(activeSalesmanName, activeSalesmanPhone, activeSalesmanPhone)
        // Room for the wider medium-weight labels ("Contact Number") before the colon
        val colonXSm = 97f
        val valXSm = 103f

        for (i in smKeys.indices) {
            val lineY = y + 29f + (i * 10f)
            paint.color = textMuted
            paint.textSize = 7f
            paint.typeface = tfMedium
            canvas.drawText(smKeys[i], 38f, lineY, paint)
            canvas.drawText(":", colonXSm, lineY, paint)
            paint.color = textDark
            paint.typeface = if (i == 0) tfBlack else tfBold
            canvas.drawText(fit(smVals[i], divX1 - 4f - valXSm), valXSm, lineY, paint)
        }

        // Col 2: Order & Dispatch Terms (200f..425f)
        drawTermsIcon(canvas, 210f, y + 10f)
        paint.color = primaryDark
        paint.textSize = 7.2f
        paint.typeface = tfBlack
        canvas.drawText("ORDER & DISPATCH TERMS", 224f, y + 18f, paint)

        val termKeys = arrayOf("One Bill One LR", "Draft Estimate", "GST Invoice", "Packing Note")
        val termVals = arrayOf("1 Bill per 1 LR strictly required", "Values subject to final dispatch qty", "Original tax invoice with consignment", "Pack strictly per case instruction")
        val colonXTerm = 276f
        val valXTerm = 282f

        for (i in termKeys.indices) {
            val lineY = y + 29f + (i * 10f)
            paint.color = textMuted
            paint.textSize = 7f
            paint.typeface = tfMedium
            canvas.drawText(termKeys[i], 210f, lineY, paint)
            canvas.drawText(":", colonXTerm, lineY, paint)
            paint.color = textDark
            paint.typeface = if (i == 0) tfBlack else tfBold
            canvas.drawText(fit(termVals[i], divX2 - 4f - valXTerm), valXTerm, lineY, paint)
        }

        // Col 3: Scan to Visit Our Website (425f..567f); icon + title centred together
        val col3CenterX = (divX2 + 567f) / 2f
        paint.color = primaryDark
        paint.textSize = 6.8f
        paint.typeface = tfBlack
        val scanTitle = "SCAN TO VISIT OUR WEBSITE"
        val scanGroupStart = col3CenterX - (14f + paint.measureText(scanTitle)) / 2f
        drawLinkIcon(canvas, scanGroupStart, y + 10f)
        canvas.drawText(scanTitle, scanGroupStart + 14f, y + 18f, paint)

        val websiteQrBitmap = generateQrCodeBitmap("https://himattextile.com", 160)
        val qrSize = 38f
        val qrLeft = col3CenterX - (qrSize / 2f)
        val qrTop = y + 21f
        if (websiteQrBitmap != null) {
            canvas.drawBitmap(websiteQrBitmap, null, RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), paint)
        }

        paint.color = textDark
        paint.textSize = 7.5f
        paint.typeface = tfBlack
        val siteUrl = "himattextile.com"
        canvas.drawText(siteUrl, col3CenterX - (paint.measureText(siteUrl) / 2f), y + 68f, paint)

        y += bCardHeight + 8f

        // Note Alert Ribbon (Pink/red banner with red exclamation circle), split over two lines
        val alertHeight = 28f
        paint.color = Color.rgb(254, 242, 242)
        canvas.drawRoundRect(RectF(28f, y, 567f, y + alertHeight), 4f, 4f, paint)
        val alertBorderPaint = Paint().apply {
            color = Color.rgb(254, 202, 202)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(28f, y, 567f, y + alertHeight), 4f, 4f, alertBorderPaint)

        val circleX = 39f
        val circleY = y + alertHeight / 2f
        canvas.drawCircle(circleX, circleY, 5.5f, Paint().apply { color = Color.rgb(220, 38, 38); isAntiAlias = true })
        val whiteExclP = Paint().apply {
            color = Color.WHITE
            textSize = 7.5f
            typeface = tfBlack
            isAntiAlias = true
        }
        canvas.drawText("!", circleX - 1.8f, circleY + 2.6f, whiteExclP)

        paint.color = Color.rgb(185, 28, 28)
        paint.textSize = 7.4f
        paint.typeface = tfBlack
        canvas.drawText("Note:", 49f, y + 11.5f, paint)
        val noteX = 49f + paint.measureText("Note:") + 5f
        val noteWidth = 567f - 8f - noteX

        paint.color = Color.rgb(153, 27, 27)
        paint.typeface = tfBold
        drawFitted(
            "All amounts shown in this draft order form are approximate and subject to final supplier dispatch and invoice.",
            noteX, y + 11.5f, noteWidth, 6f
        )
        paint.typeface = tfBlack
        drawFitted("Dispatch Policy: One Bill One LR is strictly mandatory.", noteX, y + 22f, noteWidth, 6f)

        // Footer at bottom of page
        val footerY = 822f
        canvas.drawLine(28f, footerY - 10f, 567f, footerY - 10f, borderPaint)

        paint.color = yellowAccent
        paint.textSize = 7.5f
        paint.typeface = tfBlack
        canvas.drawText("YOUR GARMENT GUIDE ACROSS INDIA", 28f, footerY, paint)

        val footerRight = "himattextile.com   |   Instagram • Facebook • LinkedIn • YouTube"
        paint.color = textDark
        paint.typeface = tfBold
        canvas.drawText(footerRight, 567f - paint.measureText(footerRight), footerY, paint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        // Named after the supplier's brand so the chat shows whose order form it is
        val file = File(outputDir, PdfFileNames.build(supplier.brandName(), "Order Form", visit.visitCode))
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

            // Our details in bold (brighter on the dark band)
            paint.textSize = 7.5f
            paint.color = Color.rgb(226, 232, 240)
            paint.typeface = Typeface.DEFAULT_BOLD
            val detailsWidth = 435f - 10f - textStartX
            drawTextFitted(canvas, paint, "First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Sherkotda, Ahmedabad - 380022", textStartX, 63f, detailsWidth)
            drawTextFitted(canvas, paint, "GSTIN: 24EASPS6621D1ZG • Phone: +91 98739 38095 • State: Gujarat (24)", textStartX, 76f, detailsWidth)

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
        canvas.drawText("Bank: ICICI Bank • Ashram Road Ahmedabad", 40f, y + 32f, paint)
        canvas.drawText("A/C Name: HIMAT TEXTILE (Current)", 40f, y + 46f, paint)
        canvas.drawText("A/C No: 136805501447 • IFSC: ICIC0000189", 40f, y + 60f, paint)
        canvas.drawText("UPI / GPay: eazypay.0000053310@icici", 40f, y + 74f, paint)

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
        val invoiceBrand = customer?.brandName()?.takeIf { it.isNotBlank() } ?: visit.customerName
        val file = File(outputDir, PdfFileNames.build(invoiceBrand, "GST Invoice", visit.visitCode))
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

        // Our details in bold (brighter on the dark band)
        paint.textSize = 7.5f
        paint.color = Color.rgb(226, 232, 240)
        paint.typeface = Typeface.DEFAULT_BOLD
        val companyDetailsWidth = 410f - 10f - textStartX
        drawTextFitted(canvas, paint, "First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Sherkotda, Ahmedabad - 380022", textStartX, 63f, companyDetailsWidth)
        drawTextFitted(canvas, paint, "GSTIN: 24EASPS6621D1ZG • Phone: +91 98739 38095 • State: Gujarat (24)", textStartX, 76f, companyDetailsWidth)

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
        canvas.drawText("PACKING", 338f, y + 13f, paint)
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
        val file = File(outputDir, PdfFileNames.build(supplier.brandName(), "GST PO", visit.visitCode))
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // 5. CUSTOMER STATEMENT FOR A DATE RANGE (MULTI-PAGE)
    //
    // Same sheet design as the trip Customer Purchase Report, so a customer gets one familiar
    // looking document whether it came from a trip or from a date range they picked themselves:
    // their brand (shop) name, their own name, their booking station, the salesman on every single
    // order, and the Sub Agent who looks after them when one is linked.
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
        val primaryDark = Color.rgb(15, 23, 42)
        val tealAccent = Color.rgb(15, 118, 110)
        val yellowAccent = Color.rgb(217, 119, 6)
        val textDark = Color.rgb(15, 23, 42)
        val textMuted = Color.rgb(100, 116, 139)
        val textHeader = Color.rgb(71, 85, 105)
        val borderLight = Color.rgb(226, 232, 240)
        val tableHeaderBg = Color.rgb(248, 250, 252)
        val altRowBg = Color.rgb(250, 250, 250)

        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = borderLight
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val iconPaint = Paint().apply {
            isAntiAlias = true
            color = tealAccent
            style = Paint.Style.FILL
        }

        val generatedOn = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val periodText = "$startDate to $endDate"

        fun drawHeader() {
            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.himat_logo)
            } catch (_: Exception) {
                null
            }

            val logoHeight = 44f
            val logoWidth = if (logoBitmap != null) logoHeight * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat()) else 0f
            val logoLeft = 28f
            val logoTop = 18f
            if (logoBitmap != null) {
                canvas.drawBitmap(logoBitmap, null, RectF(logoLeft, logoTop, logoLeft + logoWidth, logoTop + logoHeight), paint)
            }
            val textStartX = if (logoBitmap != null) (logoLeft + logoWidth + 10f) else 28f

            paint.color = primaryDark
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("HIMAT TEXTILE", textStartX, 32f, paint)

            paint.textSize = 7.5f
            paint.color = yellowAccent
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("YOUR GARMENT GUIDE ACROSS INDIA", textStartX, 44f, paint)

            paint.textSize = 7f
            paint.color = Color.rgb(51, 65, 85)
            paint.typeface = Typeface.DEFAULT_BOLD
            val detailsWidth = 372f - 8f - textStartX
            drawTextFitted(canvas, paint, COMPANY_ADDRESS, textStartX, 54f, detailsWidth)
            drawTextFitted(canvas, paint, COMPANY_CONTACT, textStartX, 64f, detailsWidth)

            canvas.drawLine(372f, 18f, 372f, 68f, borderPaint)

            paint.color = tealAccent
            paint.textSize = 13.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CUSTOMER", 385f, 30f, paint)

            paint.color = primaryDark
            drawTextFitted(canvas, paint, "PURCHASE STATEMENT", 385f, 44f, 182f, minSize = 10f)

            paint.color = textMuted
            paint.textSize = 7.8f
            paint.typeface = Typeface.DEFAULT
            val docX = 385f
            val metaBox = Paint().apply {
                color = textMuted
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
                isAntiAlias = true
            }
            canvas.drawRect(docX, 50f, docX + 6.5f, 58f, metaBox)
            drawTextFitted(canvas, paint, "Period: $periodText", docX + 11f, 56.5f, 171f, minSize = 6f)
            canvas.drawRect(docX, 61f, docX + 6.5f, 69f, metaBox)
            val metaLine2 = if (statusFilter.equals("All", ignoreCase = true)) {
                "Generated: $generatedOn"
            } else {
                "Generated: $generatedOn  •  $statusFilter"
            }
            drawTextFitted(canvas, paint, metaLine2, docX + 11f, 67.5f, 171f, minSize = 6f)
        }

        drawHeader()

        var y = 74f

        // ---------------------------------------------------------------------
        // Customer + station card (same 74f card as the trip report)
        // ---------------------------------------------------------------------
        val cardHeight = 74f
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(28f, y, 567f, y + cardHeight), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(28f, y, 567f, y + cardHeight), 6f, 6f, borderPaint)

        drawStoreGlyph(canvas, 38f, y + 10f, iconPaint)
        paint.color = textHeader
        paint.textSize = 7.2f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMER / SHOP DETAILS", 53f, y + 18f, paint)

        // The brand (shop / firm) name is what the customer recognises as "their" name
        val customerBrand = customer.firmName.ifBlank { customer.name }.uppercase()
        paint.color = primaryDark
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawTextFitted(canvas, paint, customerBrand, 38f, y + 32f, 255f, minSize = 8f)

        val customerOwner = customer.name.ifBlank { "—" }
        val customerPhone = customer.phone.ifBlank { "—" }
        val customerGstin = customer.gstin.takeIf { it.isNotBlank() } ?: "Unregistered"
        val customerIdLine = listOf(
            customer.customerId.takeIf { it.isNotBlank() },
            customer.marketArea.takeIf { it.isNotBlank() }
        ).filterNotNull().joinToString(" • ").ifBlank { "—" }

        val kvSpacing = 9.8f
        val custKeys = arrayOf("Proprietor", "Phone", "GSTIN", "Customer ID")
        val custVals = arrayOf(customerOwner, customerPhone, customerGstin, customerIdLine)
        for (i in custKeys.indices) {
            val lineY = (y + 43f) + (i * kvSpacing)
            paint.color = textMuted
            paint.textSize = 7.5f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(custKeys[i], 38f, lineY, paint)
            canvas.drawText(":", 86f, lineY, paint)
            paint.color = textDark
            paint.typeface = if (i == 0) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            drawTextFitted(canvas, paint, custVals[i], 94f, lineY, 199f, minSize = 6f)
        }

        // Right half: the station the goods book to, and the Sub Agent behind this customer
        drawTruckGlyph(canvas, 315f, y + 10f, iconPaint)
        paint.color = textHeader
        paint.textSize = 7.2f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("STATION & LINKED AGENT", 330f, y + 18f, paint)

        val stationText = customer.transportPreference.takeIf { it.isNotBlank() }
            ?: customer.city.trim().let { city ->
                if (city.isBlank()) "—" else "$city (${city.take(3).uppercase()})"
            }
        val stationCity = customer.city.trim()
        val deliveryTo = if (stationCity.isBlank()) "—" else {
            "$stationCity${if (customer.state.isNotBlank()) ", ${customer.state}" else ""}"
        }
        val subAgentName = customer.subAgentName.trim()
        val statementTransporter = entries.firstOrNull { it.transporter.isNotBlank() }?.transporter
            ?: customer.preferredTransporterName.takeIf { it.isNotBlank() }
            ?: "To be advised"

        val transKeys = arrayOf("Booking Station", "Delivery To", "Sub Agent", "Transporter", "Statement Period")
        val transVals = arrayOf(
            stationText,
            deliveryTo,
            subAgentName.ifBlank { "Not linked" },
            statementTransporter,
            periodText
        )
        for (i in transKeys.indices) {
            val lineY = (y + 32f) + (i * kvSpacing)
            paint.color = textMuted
            paint.textSize = 7.5f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(transKeys[i], 315f, lineY, paint)
            canvas.drawText(":", 385f, lineY, paint)
            paint.color = if (i == 2 && subAgentName.isBlank()) textMuted else textDark
            paint.typeface = if (i <= 2) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            drawTextFitted(canvas, paint, transVals[i], 393f, lineY, 170f, minSize = 6f)
        }

        y += cardHeight + 8f

        // ---------------------------------------------------------------------
        // Order table: the same 11 columns as the trip report
        // ---------------------------------------------------------------------
        val colX = floatArrayOf(28f, 48f, 96f, 154f, 218f, 244f, 286f, 338f, 386f, 434f, 494f, 567f)

        fun drawTableHeader() {
            paint.color = tableHeaderBg
            paint.style = Paint.Style.FILL
            canvas.drawRect(28f, y, 567f, y + 18f, paint)
            canvas.drawLine(28f, y, 567f, y, borderPaint)
            canvas.drawLine(28f, y + 18f, 567f, y + 18f, borderPaint)

            paint.color = primaryDark
            paint.textSize = 6.6f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("SR. NO.", 38f - (paint.measureText("SR. NO.") / 2f), y + 11.5f, paint)
            canvas.drawText("ORDER NO.", 51f, y + 11.5f, paint)
            canvas.drawText("BRAND", 100f, y + 11.5f, paint)
            canvas.drawText("ITEM / STYLE", 158f, y + 11.5f, paint)
            canvas.drawText("PCS", 231f - (paint.measureText("PCS") / 2f), y + 11.5f, paint)
            canvas.drawText("RATE (₹)", 282f - paint.measureText("RATE (₹)"), y + 11.5f, paint)
            canvas.drawText("AMOUNT (₹)", 334f - paint.measureText("AMOUNT (₹)"), y + 11.5f, paint)
            canvas.drawText("CASE & PACKING", 362f - (paint.measureText("CASE & PACKING") / 2f), y + 11.5f, paint)
            canvas.drawText("PURCHASE DATE", 410f - (paint.measureText("PURCHASE DATE") / 2f), y + 11.5f, paint)
            canvas.drawText("SALESMAN", 438f, y + 11.5f, paint)
            canvas.drawText("STATUS", 530.5f - (paint.measureText("STATUS") / 2f), y + 11.5f, paint)

            for (x in colX) {
                canvas.drawLine(x, y, x, y + 18f, borderPaint)
            }
            y += 18f
        }

        drawTableHeader()

        var totalPieces = 0
        var totalAmount = 0.0
        var totalGst = 0.0
        var totalCases = 0
        var totalLoose = 0
        var dispatchedPieces = 0
        var pendingPieces = 0
        val salesmenSeen = linkedSetOf<String>()

        if (entries.isEmpty()) {
            paint.color = textMuted
            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("No orders recorded for this customer in the selected period.", 40f, y + 16f, paint)
            canvas.drawLine(28f, y + 26f, 567f, y + 26f, borderPaint)
            canvas.drawLine(28f, y, 28f, y + 26f, borderPaint)
            canvas.drawLine(567f, y, 567f, y + 26f, borderPaint)
            y += 26f
        } else {
            entries.forEachIndexed { index, item ->
                if (y > 680f) {
                    pdfDocument.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader()
                    y = 80f
                    drawTableHeader()
                }

                totalPieces += item.pieces
                totalAmount += item.totalAmount
                totalGst += item.gstAmount
                totalCases += item.caseCount
                totalLoose += item.loosePieces

                val delivered = item.deliveryStatus.equals("Delivered", ignoreCase = true)
                val dispatched = delivered || item.deliveryStatus.equals("Dispatched", ignoreCase = true)
                if (dispatched) dispatchedPieces += item.pieces else pendingPieces += item.pieces

                val rowTop = y
                val rowHeight = if (item.mixedPackNote.isNullOrBlank()) 15.5f else 25f

                if (index % 2 == 1) {
                    paint.color = altRowBg
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(28f, rowTop, 567f, rowTop + rowHeight, paint)
                }

                paint.color = textDark
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 6.8f

                val srStr = "${index + 1}"
                canvas.drawText(srStr, 38f - (paint.measureText(srStr) / 2f), y + 10.5f, paint)

                val orderNo = item.orderNo.ifBlank { "—" }
                canvas.drawText(if (orderNo.length > 10) orderNo.take(8) + ".." else orderNo, 51f, y + 10.5f, paint)

                val brand = item.supplierName.ifBlank { "—" }
                canvas.drawText(if (brand.length > 12) brand.take(10) + ".." else brand, 100f, y + 10.5f, paint)

                val itemCode = item.itemCode.ifBlank { "—" }
                canvas.drawText(if (itemCode.length > 13) itemCode.take(11) + ".." else itemCode, 158f, y + 10.5f, paint)

                val pcStr = "${item.pieces}"
                canvas.drawText(pcStr, 231f - (paint.measureText(pcStr) / 2f), y + 10.5f, paint)

                val rateStr = String.format(Locale.US, "%,.2f", item.rate)
                canvas.drawText(rateStr, 282f - paint.measureText(rateStr), y + 10.5f, paint)

                val amt = if (item.totalAmount > 0) item.totalAmount else (item.pieces * item.rate)
                val amtStr = String.format(Locale.US, "%,.2f", amt)
                canvas.drawText(amtStr, 334f - paint.measureText(amtStr), y + 10.5f, paint)

                // Packing note lives inside the CASE & PACKING column, same as the trip report
                val packDesc = if (item.loosePieces > 0) "${item.caseCount}c+${item.loosePieces}L" else "${item.caseCount} cs"
                canvas.drawText(packDesc, 362f - (paint.measureText(packDesc) / 2f), y + 10.5f, paint)
                val packNote = item.mixedPackNote?.trim().orEmpty()
                if (packNote.isNotEmpty()) {
                    val notePaint = Paint(paint).apply {
                        textSize = 5.4f
                        color = textMuted
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    }
                    wrapToWidth(notePaint, packNote, 44f, maxLines = 2).forEachIndexed { line, text ->
                        canvas.drawText(
                            text,
                            362f - (notePaint.measureText(text) / 2f),
                            y + 16.5f + (line * 5.8f),
                            notePaint
                        )
                    }
                }

                val pDate = item.orderDate.take(10).ifBlank { "—" }
                canvas.drawText(pDate, 410f - (paint.measureText(pDate) / 2f), y + 10.5f, paint)

                // The salesman credited with this one order, not a single name for the whole sheet
                val smName = item.salesmanName.trim().ifBlank { item.createdByName.trim() }.ifBlank { "—" }
                if (smName != "—") salesmenSeen.add(smName)
                canvas.drawText(if (smName.length > 10) smName.take(8) + ".." else smName, 438f, y + 10.5f, paint)

                val status = item.deliveryStatus.ifBlank { "Pending" }
                val (statusBg, statusFg) = when {
                    delivered -> Pair(Color.rgb(220, 252, 231), Color.rgb(22, 101, 52))
                    dispatched -> Pair(Color.rgb(219, 234, 254), Color.rgb(30, 64, 175))
                    else -> Pair(Color.rgb(254, 243, 199), Color.rgb(180, 83, 9))
                }
                val statusWidth = 58f
                val statusHeight = 11f
                val statusLeft = 494f + ((567f - 494f - statusWidth) / 2f)
                val statusTop = y + ((15.5f - statusHeight) / 2f)
                paint.color = statusBg
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(RectF(statusLeft, statusTop, statusLeft + statusWidth, statusTop + statusHeight), 3f, 3f, paint)
                paint.color = statusFg
                paint.textSize = 6.2f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(status, statusLeft + ((statusWidth - paint.measureText(status)) / 2f), statusTop + 8f, paint)

                y += rowHeight

                // Sub-line: the Sub Agent behind this order plus the transporter. The packing note
                // is not repeated here — it sits in the CASE & PACKING column where it belongs.
                val subLineParts = buildList {
                    if (subAgentName.isNotBlank()) add("Sub Agent: $subAgentName")
                    add("Transporter: ${item.transporter.ifBlank { "To be advised" }}")
                }
                if (subLineParts.isNotEmpty()) {
                    paint.color = Color.rgb(250, 250, 250)
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(28f, y, 567f, y + 12f, paint)
                    paint.color = textMuted
                    paint.textSize = 6.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    drawTextFitted(canvas, paint, "  ↳ " + subLineParts.joinToString(" • "), 51f, y + 8.5f, 505f, minSize = 5f)
                    y += 12f
                }

                val rowBottom = y
                for (x in colX) {
                    canvas.drawLine(x, rowTop, x, rowBottom, borderPaint)
                }
                canvas.drawLine(28f, rowBottom, 567f, rowBottom, borderPaint)
            }
        }

        // Totals + bottom card + banner + footer need roughly 170f of room
        if (y > 630f) {
            pdfDocument.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            drawHeader()
            y = 80f
        }

        y += 8f

        // ---------------------------------------------------------------------
        // Totals, right aligned like the trip report
        // ---------------------------------------------------------------------
        val totLeft = 347f
        val totRight = 567f

        // Left of the totals: how much has actually moved out
        paint.color = textMuted
        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Orders in period", 28f, y + 10f, paint)
        canvas.drawText("Dispatched / delivered", 28f, y + 22f, paint)
        canvas.drawText("Still pending", 28f, y + 34f, paint)
        paint.color = textDark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${entries.size} orders • $totalPieces pcs", 140f, y + 10f, paint)
        canvas.drawText("$dispatchedPieces pcs", 140f, y + 22f, paint)
        paint.color = if (pendingPieces > 0) Color.rgb(180, 83, 9) else Color.rgb(22, 101, 52)
        canvas.drawText(if (pendingPieces > 0) "$pendingPieces pcs" else "Nothing pending", 140f, y + 34f, paint)

        paint.color = textDark
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Subtotal ($totalPieces Pcs)", totLeft + 8f, y + 10f, paint)
        val subStr = "₹" + String.format(Locale.US, "%,.2f", totalAmount)
        canvas.drawText(subStr, totRight - 8f - paint.measureText(subStr), y + 10f, paint)

        canvas.drawText("Garment GST", totLeft + 8f, y + 22f, paint)
        val gstStr = "₹" + String.format(Locale.US, "%,.2f", totalGst)
        canvas.drawText(gstStr, totRight - 8f - paint.measureText(gstStr), y + 22f, paint)

        val grandTotal = totalAmount + totalGst
        val pillTop = y + 28f
        val pillHeight = 20f
        paint.color = Color.rgb(238, 242, 246)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(totLeft, pillTop, totRight, pillTop + pillHeight), 4f, 4f, paint)

        paint.color = primaryDark
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Grand Total", totLeft + 8f, pillTop + 14f, paint)
        val grandStr = "₹" + String.format(Locale.US, "%,.2f", grandTotal)
        paint.textSize = 11f
        canvas.drawText(grandStr, totRight - 8f - paint.measureText(grandStr), pillTop + 14.5f, paint)

        y += 56f

        // ---------------------------------------------------------------------
        // Bottom 3-column card: who served them, where to pay, and the QR
        // ---------------------------------------------------------------------
        val bCardHeight = 74f
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(28f, y, 567f, y + bCardHeight), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(28f, y, 567f, y + bCardHeight), 6f, 6f, borderPaint)

        val divX1 = 200f
        val divX2 = 425f
        canvas.drawLine(divX1, y, divX1, y + bCardHeight, borderPaint)
        canvas.drawLine(divX2, y, divX2, y + bCardHeight, borderPaint)

        drawPersonGlyph(canvas, 38f, y + 10f, iconPaint)
        paint.color = primaryDark
        paint.textSize = 7.2f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SALESMAN & AGENT", 52f, y + 18f, paint)

        val salesmenLine = salesmenSeen.joinToString(", ").ifBlank { "—" }
        val smKeys = arrayOf(
            if (salesmenSeen.size > 1) "Salesmen" else "Salesman",
            "Sub Agent",
            "Office Number",
            "Email"
        )
        val smVals = arrayOf(
            salesmenLine,
            subAgentName.ifBlank { "Not linked" },
            "+91 98739 38095",
            "info@himattextile.com"
        )
        for (i in smKeys.indices) {
            val lineY = y + 30f + (i * 10f)
            paint.color = textMuted
            paint.textSize = 7f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(smKeys[i], 38f, lineY, paint)
            canvas.drawText(":", 88f, lineY, paint)
            paint.color = if (i == 1 && subAgentName.isBlank()) textMuted else textDark
            paint.typeface = if (i <= 1) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            drawTextFitted(canvas, paint, smVals[i], 94f, lineY, 100f, minSize = 5.5f)
        }

        drawBankGlyph(canvas, 210f, y + 10f, iconPaint)
        paint.color = primaryDark
        paint.textSize = 7.2f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BANK ACCOUNT DETAILS (HIMAT TEXTILE)", 224f, y + 18f, paint)

        val bankKeys = arrayOf("Bank Name", "Account Name", "Account Number", "IFSC Code", "Type & Branch")
        val bankVals = arrayOf("ICICI Bank", "Himat Textile", "136805501447", "ICIC0000189", "Current • Ashram Rd")
        for (i in bankKeys.indices) {
            val lineY = y + 28f + (i * 9.2f)
            paint.color = textMuted
            paint.textSize = 7f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(bankKeys[i], 210f, lineY, paint)
            canvas.drawText(":", 274f, lineY, paint)
            paint.color = textDark
            paint.typeface = if (i < 4) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            canvas.drawText(bankVals[i], 280f, lineY, paint)
        }

        val col3CenterX = (divX2 + 567f) / 2f
        drawShieldGlyph(canvas, col3CenterX - 48f, y + 10f, iconPaint)
        paint.color = primaryDark
        paint.textSize = 6.8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val scanTitle = "SCAN TO PAY (UPI QR)"
        canvas.drawText(scanTitle, col3CenterX - (paint.measureText(scanTitle) / 2f) + 6f, y + 18f, paint)

        val paymentQrBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.payment_qr)
        } catch (_: Exception) {
            null
        } ?: generateQrCodeBitmap("upi://pay?pa=eazypay.0000053310@icici&pn=ICICI Bank InstaBIZ Merchant&tr=EZYS0000053310&cu=INR&mc=5999", 160)

        val qrSize = 38f
        val qrLeft = col3CenterX - (qrSize / 2f)
        val qrTop = y + 21f
        if (paymentQrBitmap != null) {
            canvas.drawBitmap(paymentQrBitmap, null, RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), paint)
        }

        paint.color = textDark
        paint.textSize = 6.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val upiIdText = "eazypay.0000053310@icici"
        canvas.drawText(upiIdText, col3CenterX - (paint.measureText(upiIdText) / 2f), y + 68f, paint)

        y += bCardHeight + 8f

        // Same note ribbon as the trip report
        val alertHeight = 18f
        paint.color = Color.rgb(254, 242, 242)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(28f, y, 567f, y + alertHeight), 4f, 4f, paint)
        val alertBorderPaint = Paint().apply {
            color = Color.rgb(254, 202, 202)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(28f, y, 567f, y + alertHeight), 4f, 4f, alertBorderPaint)

        val circleX = 39f
        val circleY = y + 9f
        canvas.drawCircle(circleX, circleY, 5.5f, Paint().apply { color = Color.rgb(220, 38, 38); isAntiAlias = true })
        val whiteExclP = Paint().apply {
            color = Color.WHITE
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("!", circleX - 1.8f, circleY + 2.5f, whiteExclP)

        paint.color = Color.rgb(185, 28, 28)
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Note:", 49f, y + 12f, paint)

        paint.color = Color.rgb(153, 27, 27)
        paint.typeface = Typeface.DEFAULT
        drawTextFitted(
            canvas,
            paint,
            "All amounts shown in this statement are approximate and may vary from the final supplier invoice / dispatch quantity.",
            73f,
            y + 12f,
            488f,
            minSize = 5.5f
        )

        val footerY = 822f
        canvas.drawLine(28f, footerY - 10f, 567f, footerY - 10f, borderPaint)

        paint.color = yellowAccent
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("YOUR GARMENT GUIDE ACROSS INDIA", 28f, footerY, paint)

        val footerRight = "himattextile.com   |   Instagram • Facebook • LinkedIn • YouTube"
        paint.color = textDark
        canvas.drawText(footerRight, 567f - paint.measureText(footerRight), footerY, paint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, PdfFileNames.build(customer.brandName(), "Statement", periodText))
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    // =========================================================================
    // 6. ALL PURCHASE ORDERS & SUPPLIER INVOICES BULK PDF (MULTI-PAGE)
    // =========================================================================
    fun generatePurchaseOrdersBulkPdf(
        context: Context,
        entries: List<PurchaseEntryEntity>,
        dateFilterLabel: String = "All Time",
        supplierFilterLabel: String = "All Suppliers",
        statusFilterLabel: String = "All Status"
    ): File {
        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val primaryColor = Color.rgb(19, 35, 56)
        val ochreColor = Color.rgb(194, 65, 12)
        val textDark = Color.rgb(20, 25, 35)
        val textGray = Color.rgb(100, 110, 125)
        val lightBg = Color.rgb(245, 247, 250)
        val tableBorder = Color.rgb(215, 222, 230)

        fun drawPurchaseOrdersHeader() {
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

            paint.textSize = 9.5f
            paint.color = Color.rgb(254, 215, 170) // Warm ochre light
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("PURCHASE ORDERS & SUPPLIERS INVOICES STATEMENT", textStartX, 50f, paint)

            // Same company details as every other PDF (bold, no email)
            paint.textSize = 7.5f
            paint.color = Color.rgb(226, 232, 240)
            paint.typeface = Typeface.DEFAULT_BOLD
            val detailsWidth = 565f - 8f - textStartX
            drawTextFitted(canvas, paint, COMPANY_ADDRESS, textStartX, 63f, detailsWidth)
            drawTextFitted(canvas, paint, COMPANY_CONTACT, textStartX, 74f, detailsWidth)

            paint.color = Color.rgb(254, 243, 199)
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val pageStr = "Page $pageNum"
            canvas.drawText(pageStr, 565f - paint.measureText(pageStr), 25f, paint)

            val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            paint.color = Color.rgb(203, 213, 225)
            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Generated: $nowStr", 565f - paint.measureText("Generated: $nowStr"), 38f, paint)
        }

        drawPurchaseOrdersHeader()

        var y = 110f

        // Filter / Scope Info Card
        paint.color = lightBg
        canvas.drawRoundRect(30f, y, 565f, y + 36f, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        canvas.drawRoundRect(30f, y, 565f, y + 36f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Period / Date:", 42f, y + 15f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = textGray
        canvas.drawText(dateFilterLabel, 106f, y + 15f, paint)

        paint.color = textDark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Supplier Filter:", 235f, y + 15f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = textGray
        canvas.drawText(supplierFilterLabel, 305f, y + 15f, paint)

        paint.color = textDark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Status Filter:", 430f, y + 15f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = textGray
        canvas.drawText(statusFilterLabel, 490f, y + 15f, paint)

        paint.color = textDark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Purchase Orders:", 42f, y + 28f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = ochreColor
        canvas.drawText("${entries.size} Records Listed", 145f, y + 28f, paint)

        y += 48f

        // Table Header function
        fun drawTableHeader() {
            paint.color = primaryColor
            canvas.drawRect(30f, y, 565f, y + 20f, paint)

            paint.color = Color.WHITE
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("#", 35f, y + 13f, paint)
            canvas.drawText("Order No", 50f, y + 13f, paint)
            canvas.drawText("Supplier Mill", 115f, y + 13f, paint)
            canvas.drawText("Item / Fabric", 225f, y + 13f, paint)
            canvas.drawText("Cases", 310f, y + 13f, paint)
            canvas.drawText("Pieces", 350f, y + 13f, paint)
            canvas.drawText("Rate", 390f, y + 13f, paint)
            canvas.drawText("Taxable", 430f, y + 13f, paint)
            canvas.drawText("Total+GST", 480f, y + 13f, paint)
            canvas.drawText("Status", 535f, y + 13f, paint)

            y += 20f
        }

        drawTableHeader()

        val totalPieces = entries.sumOf { it.pieces }
        val totalCases = entries.sumOf { it.caseCount }
        val totalTaxable = entries.sumOf { it.totalAmount }
        val totalGst = entries.sumOf { it.gstAmount }
        val grandTotal = entries.sumOf { it.grandTotalWithGst }

        entries.forEachIndexed { index, entry ->
            if (y > 770f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawPurchaseOrdersHeader()
                y = 110f
                drawTableHeader()
            }

            if (index % 2 == 1) {
                paint.color = Color.rgb(250, 250, 252)
                canvas.drawRect(30f, y, 565f, y + 21f, paint)
            }

            paint.color = tableBorder
            canvas.drawLine(30f, y + 21f, 565f, y + 21f, paint)

            paint.color = textDark
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            canvas.drawText("${index + 1}", 35f, y + 14f, paint)

            val orderCode = entry.orderNo.ifBlank { "PO-${entry.id}" }
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(orderCode, 50f, y + 14f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val supDisplay = entry.supplierName.take(18)
            canvas.drawText(supDisplay, 115f, y + 14f, paint)

            val itemDisplay = entry.itemCode.ifBlank { "Garments" }.take(14)
            canvas.drawText(itemDisplay, 225f, y + 14f, paint)

            canvas.drawText("${entry.caseCount}", 315f, y + 14f, paint)
            canvas.drawText("${entry.pieces}", 352f, y + 14f, paint)
            canvas.drawText("₹${entry.rate.toInt()}", 390f, y + 14f, paint)
            canvas.drawText("₹${entry.totalAmount.toInt()}", 430f, y + 14f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("₹${entry.grandTotalWithGst.toInt()}", 480f, y + 14f, paint)

            // Status indicator
            val isDelivered = entry.deliveryStatus.equals("Delivered", ignoreCase = true)
            paint.color = if (isDelivered) Color.rgb(22, 163, 74) else Color.rgb(217, 119, 6)
            paint.textSize = 6.5f
            canvas.drawText(entry.deliveryStatus.take(8), 535f, y + 14f, paint)

            y += 21f
        }

        y += 14f

        if (y > 720f) {
            pdfDocument.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            drawPurchaseOrdersHeader()
            y = 110f
        }

        // Summary Card
        paint.color = lightBg
        canvas.drawRoundRect(280f, y, 565f, y + 68f, 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = tableBorder
        canvas.drawRoundRect(280f, y, 565f, y + 68f, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Items / Pieces:", 295f, y + 16f, paint)
        canvas.drawText("${entries.size} Orders / $totalPieces Pcs", 415f, y + 16f, paint)

        canvas.drawText("Taxable + GST Amount:", 295f, y + 32f, paint)
        canvas.drawText("${formatInr(totalTaxable)} + ${formatInr(totalGst)}", 415f, y + 32f, paint)

        paint.color = primaryColor
        canvas.drawRect(280f, y + 42f, 565f, y + 68f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GRAND TOTAL VALUE:", 295f, y + 58f, paint)
        canvas.drawText(formatInr(grandTotal), 465f, y + 58f, paint)

        y += 88f

        // Footer
        paint.color = textGray
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("• This report is a consolidated statement of purchase orders and supplier invoices.", 30f, y, paint)
        canvas.drawText("• Generated securely by Himat Textile Application system.", 30f, y + 11f, paint)

        y += 35f
        paint.color = textDark
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawLine(50f, y, 190f, y, paint)
        canvas.drawText("Prepared By", 85f, y + 14f, paint)

        canvas.drawLine(405f, y, 545f, y, paint)
        canvas.drawText("For HIMAT TEXTILE", 430f, y + 14f, paint)

        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "Purchase_Orders_Statement_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }
}
