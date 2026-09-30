package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator
import com.example.ui.components.CustomerReportOptions
import com.example.ui.components.ReportOptionsBottomSheet
import com.example.util.ReportFields
import com.example.util.ShareUtil

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CustomerReportScreen(
    viewModel: HimatViewModel,
    visit: VisitEntity,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entries by viewModel.visitEntries.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val customer = allCustomers.find { it.id == visit.customerId }
    val allEmployees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val salesman = allEmployees.find { it.id == visit.employeeId }
    val packGroups by viewModel.visitPackGroups.collectAsStateWithLifecycle()

    val totalPieces = entries.sumOf { it.pieces }
    val totalAmount = entries.sumOf { it.totalAmount }
    val totalGst = entries.sumOf { it.gstAmount }
    val grandTotal = totalAmount + totalGst
    val totalCases = entries.sumOf { it.caseCount }
    val totalLoose = entries.sumOf { it.loosePieces }

    // Report customization options + sheet visibility
    var reportOptions by remember { mutableStateOf(CustomerReportOptions()) }
    var showDayReportSheet by remember { mutableStateOf(false) }

    // Bottom sheets (shown before PDF generation)
    if (showDayReportSheet) {
        ReportOptionsBottomSheet(
            title = "Quotation PDF Customize Karein",
            options = reportOptions,
            onOptionsChange = { reportOptions = it },
            onGeneratePdf = { viewModel.shareCustomerDayReportPdf(visit, reportOptions) },
            onDismiss = { showDayReportSheet = false },
            // Resolved with the same function the PDF writer uses, so each box shows as its
            // placeholder exactly what will be printed if the sender leaves it alone
            defaults = ReportFields.resolve(visit, customer, entries)
        )
    }

    Scaffold(
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // WhatsApp Share
                        Button(
                            onClick = { viewModel.shareCustomerReportWhatsApp(visit) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Share WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }

                        // Copy Text
                        OutlinedButton(
                            onClick = {
                                val text = ShareUtil.buildCustomerReportText(visit, customer, entries)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Customer Report", text))
                                Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp),
                            modifier = Modifier.weight(0.6f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NavyPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Standard Day Report PDF — opens customization sheet first
                        Button(
                            onClick = { showDayReportSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Day Report PDF", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color.White)
                        }

                        // GST Tax Invoice PDF
                        Button(
                            onClick = { viewModel.shareCustomerGstInvoicePdf(visit) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GST Invoice PDF", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 0.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NavyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Customer Purchase Report",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = "Aggregated purchases for ${visit.customerName}",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // High Fidelity Paper Document Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // 1. Letterhead
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.himat_logo),
                                    contentDescription = "Himat Textile Logo",
                                    modifier = Modifier
                                        .size(46.dp)
                                        .padding(end = 10.dp)
                                )
                                Column {
                                    Text(
                                        text = "HIMAT TEXTILE",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NavyPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "YOUR GARMENT GUIDE ACROSS INDIA",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD97706), // Golden Yellow
                                        letterSpacing = 0.4.sp
                                    )
                                    Text(
                                        text = "First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Ahmedabad, Gujarat",
                                        fontSize = 7.5.sp,
                                        color = TextSecondary,
                                        lineHeight = 11.sp
                                    )
                                    Text(
                                        text = "GSTIN: 24EASPS6621D1ZG  |  Ph: +91 98739 38095",
                                        fontSize = 7.5.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(0.7f)
                            ) {
                                Text(
                                    text = "CUSTOMER",
                                    color = Color(0xFF0F766E), // Teal 700
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "PURCHASE REPORT",
                                    color = NavyPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Trip: ${visit.visitCode}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Date: ${visit.date}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFFE2E8F0),
                            thickness = 1.dp
                        )

                        // 2. Customer & Transport Details Card
                        val custBrand = (if (!customer?.firmName.isNullOrBlank()) customer?.firmName else customer?.name ?: visit.customerName).orEmpty().uppercase()
                        val customerOwner = customer?.name.orEmpty().ifBlank { "Mr. Jitendra Bhai" }
                        val customerPhone = customer?.phone.orEmpty().ifBlank { "+91 98765 43210" }
                        val customerGstin = customer?.gstin?.takeIf { it.isNotBlank() } ?: "Unregistered"
                        val customerAddress = customer?.shopAddress?.takeIf { it.isNotBlank() }
                            ?: customer?.city?.let { "$it${if (!customer.state.isNullOrBlank()) ", ${customer.state}" else ""}" }
                            ?: "Ahiliyanagar, Maharashtra"

                        val primaryTransporter = entries.find { it.transporter.isNotBlank() }?.transporter
                            ?: customer?.preferredTransporterName?.takeIf { it.isNotBlank() }
                            ?: "Shree Maruti Transport"
                        val bookingStation = customer?.transportPreference?.takeIf { it.isNotBlank() }
                            ?: customer?.city?.let { "$it (${it.take(3).uppercase()})" }
                            ?: "Ahmedabad (ADI)"
                        val deliveryTo = customer?.city?.let { "$it${if (!customer.state.isNullOrBlank()) ", ${customer.state}" else ""}" } ?: "Ahiliyanagar, Maharashtra"

                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // Left: Customer / Shop Details
                                Text(
                                    text = "CUSTOMER / SHOP DETAILS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF475569),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = custBrand,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = NavyPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Proprietor: $customerOwner", fontSize = 9.sp, color = TextPrimary)
                                        Text("Phone: $customerPhone", fontSize = 9.sp, color = TextPrimary)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("GSTIN: $customerGstin", fontSize = 9.sp, color = TextPrimary)
                                        Text("Address: $customerAddress", fontSize = 9.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = Color(0xFFF1F5F9),
                                    thickness = 1.dp
                                )

                                // Right: Transport & Booking Details
                                Text(
                                    text = "TRANSPORT & BOOKING DETAILS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF475569),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row {
                                            Text("Transporter: ", fontSize = 9.sp, color = TextSecondary)
                                            Text(primaryTransporter, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                        }
                                        Row {
                                            Text("Station: ", fontSize = 9.sp, color = TextSecondary)
                                            Text(bookingStation, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                        }
                                        Text("LR No: —", fontSize = 9.sp, color = TextSecondary)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Dispatch Date: ${visit.date}", fontSize = 9.sp, color = TextSecondary)
                                        Text("Delivery To: $deliveryTo", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Orders Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PURCHASE ORDERS (${entries.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NavyPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$totalPieces Pcs • ${totalCases} cs",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 4. Consolidated Orders Cards
                        entries.forEachIndexed { index, item ->
                            val orderNo = if (item.orderNo.isNotBlank()) item.orderNo else "HT-${2620 + index}"
                            val status = item.deliveryStatus.ifBlank { "Pending" }
                            val (statusBg, statusFg) = when (status.lowercase()) {
                                "delivered" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
                                "dispatched" -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
                                else -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
                            }
                            val pack = if (item.loosePieces > 0) "${item.caseCount}c + ${item.loosePieces}L" else "${item.caseCount} cs"
                            val orderSalesman = if (item.salesmanName.isNotBlank()) item.salesmanName else visit.employeeName

                            Surface(
                                color = if (index % 2 == 1) Color(0xFFF8FAFC) else Color.White,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(9.dp)) {
                                    // Row 1: Order No, Sr, Status Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF475569),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = orderNo,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NavyPrimary
                                            )
                                        }

                                        // Status badge
                                        Surface(
                                            color = statusBg,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = status,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = statusFg,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Row 2: Item Code & Supplier Brand
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Text(
                                                text = item.itemCode,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = NavyPrimary
                                            )
                                            Text(
                                                text = "Brand: ${item.supplierName}",
                                                fontSize = 9.5.sp,
                                                color = Color(0xFF475569),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // Amount on the right
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "₹${PdfGenerator.formatInr(item.totalAmount)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                color = NavyPrimary
                                            )
                                            Text(
                                                text = "${item.pieces} pcs @ ₹${item.rate.toInt()}",
                                                fontSize = 9.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Row 3: Meta details (Packing, Date, Salesman)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Pack: $pack",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (item.loosePieces > 0) Color(0xFFD97706) else Color(0xFF15803D)
                                        )
                                        Text(
                                            text = "Salesman: $orderSalesman",
                                            fontSize = 8.5.sp,
                                            color = Color(0xFF64748b)
                                        )
                                        Text(
                                            text = (item.orderDate.takeIf { it.isNotBlank() } ?: visit.date).take(10),
                                            fontSize = 8.5.sp,
                                            color = Color(0xFF64748b)
                                        )
                                    }

                                    // Row 4: Packing note if any
                                    if (!item.mixedPackNote.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Surface(
                                            color = Color(0xFFFFFBEB),
                                            shape = RoundedCornerShape(3.dp),
                                            border = BorderStroke(0.5.dp, Color(0xFFFDE68A)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "↳ Note: ${item.mixedPackNote}",
                                                fontSize = 8.5.sp,
                                                color = Color(0xFF92400E),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 5. Totals Box (Right aligned style)
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal ($totalPieces Pcs):", fontSize = 10.5.sp, color = TextSecondary)
                                    Text("₹${PdfGenerator.formatInr(totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = NavyPrimary)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Garment GST (5%):", fontSize = 10.5.sp, color = TextSecondary)
                                    Text("₹${PdfGenerator.formatInr(totalGst)}", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = NavyPrimary)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    color = Color(0xFFEEF2F6),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Grand Total",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = "₹${PdfGenerator.formatInr(grandTotal)}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = NavyPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 6. Bottom Info Cards (Salesman & Bank Details)
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                val activeSalesmanName = (salesman?.name ?: visit.employeeName).ifBlank { "Jalam Bhai" }
                                val activeSalesmanPhone = salesman?.phone.orEmpty().ifBlank { "+91 98739 38095" }
                                val activeSalesmanEmail = salesman?.email.orEmpty().ifBlank { "jalam@himattextile.com" }

                                Text(
                                    text = "SALESMAN DETAILS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NavyPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Name: $activeSalesmanName  |  Ph: $activeSalesmanPhone", fontSize = 9.sp, color = TextPrimary)
                                Text("WhatsApp: $activeSalesmanPhone  |  Email: $activeSalesmanEmail", fontSize = 9.sp, color = TextSecondary)

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = Color(0xFFF1F5F9),
                                    thickness = 1.dp
                                )

                                Text(
                                    text = "BANK ACCOUNT DETAILS (HIMAT TEXTILE)",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NavyPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text("Bank: ICICI Bank  •  Current A/C", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("A/C Name: HIMAT TEXTILE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = NavyPrimary)
                                        Text("A/C No: 136805501447", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("IFSC: ICIC0000189", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Branch: Ashram Road, Ahmedabad", fontSize = 8.5.sp, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("UPI ID: eazypay.0000053310@icici", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                                    }
                                    Column(
                                        modifier = Modifier.weight(0.7f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.payment_qr),
                                            contentDescription = "UPI Payment QR",
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                        )
                                        Text("Scan to Pay", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 7. Note Alert Banner
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFFDC2626),
                                    shape = CircleShape,
                                    modifier = Modifier.size(14.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("!", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Note: All amounts shown in this report are approximate and may vary from final supplier invoice / dispatch quantity.",
                                    fontSize = 8.sp,
                                    color = Color(0xFF991B1B),
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 8. Footer
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "YOUR GARMENT GUIDE ACROSS INDIA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFD97706), // Golden Yellow
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "himattextile.com   |   Instagram • Facebook • LinkedIn • YouTube",
                                fontSize = 8.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
