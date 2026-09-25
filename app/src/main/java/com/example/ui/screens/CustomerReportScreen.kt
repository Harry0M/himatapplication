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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.util.ShareUtil

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
    val packGroups by viewModel.visitPackGroups.collectAsStateWithLifecycle()

    val totalPieces = entries.sumOf { it.pieces }
    val totalAmount = entries.sumOf { it.totalAmount }
    val totalGst = entries.sumOf { it.gstAmount }
    val grandTotal = totalAmount + totalGst
    val totalCases = entries.sumOf { it.caseCount }
    val totalLoose = entries.sumOf { it.loosePieces }

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
                        // Standard Day Report PDF
                        Button(
                            onClick = { viewModel.shareCustomerDayReportPdf(visit) },
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
                .background(Color(0xFFF1F5F9))
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
                            text = "Customer Day Report",
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Letterhead
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.himat_logo),
                                    contentDescription = "Himat Textile Logo",
                                    modifier = Modifier
                                        .size(42.dp)
                                        .padding(end = 10.dp)
                                )
                                Column {
                                    Text(
                                        text = "HIMAT TEXTILE",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "YOUR BUSINESS GUIDE ACROSS INDIA",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                    Text(
                                        text = "Multi-Supplier Procurement Facilitator",
                                        fontSize = 8.5.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = NavyPrimary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "DAY REPORT",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Visit: ${visit.visitCode}", fontSize = 10.sp, color = TextSecondary)
                                Text("Date: ${visit.date}", fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Customer & Salesman Meta Box
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("CUSTOMER / BUYER:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Text(customer?.name ?: visit.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text("${customer?.city ?: ""} • ${customer?.phone ?: ""}", fontSize = 11.sp, color = TextSecondary)
                                    if (!customer?.gstin.isNullOrBlank()) {
                                        Text("GSTIN: ${customer?.gstin}", fontSize = 10.5.sp, color = NavyPrimary)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("ACCOMPANYING AGENT:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Text(visit.employeeName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                                    Text("Himat Sourcing Team", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Partitioned Supplier Sections
                        val grouped = entries.groupBy { it.supplierName }
                        grouped.forEach { (supplierName, supplierItems) ->
                            val supType = supplierItems.firstOrNull()?.supplierType ?: ""
                            val supplierPcs = supplierItems.sumOf { it.pieces }
                            val supplierTotal = supplierItems.sumOf { it.totalAmount }

                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                            ) {
                                Column {
                                    // Section Header
                                    Surface(
                                        color = NavyPrimary,
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🏭 $supplierName",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (supType.isNotBlank()) {
                                                SupplierTypeBadge(type = supType)
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = "$supplierPcs pcs",
                                                fontSize = 11.sp,
                                                color = Color(0xFF93C5FD),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Partition Column Headers
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("ITEM / ORDER", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = NavyPrimary, modifier = Modifier.weight(1.8f))
                                            Text("QTY", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = NavyPrimary, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                                            Text("RATE", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = NavyPrimary, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                                            Text("PACKING", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = NavyPrimary, textAlign = TextAlign.Center, modifier = Modifier.weight(1.0f))
                                            Text("AMOUNT", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = NavyPrimary, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.dp)

                                    // Items List
                                    supplierItems.forEachIndexed { idx, item ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (idx % 2 == 1) Color(0xFFF8FAFC) else Color.White)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 1. ITEM & ORDER (Stacked so Item has plenty of horizontal room)
                                                Column(modifier = Modifier.weight(1.8f)) {
                                                    Text(
                                                        text = item.itemCode,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.5.sp,
                                                        color = TextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "#${item.orderNo}",
                                                        fontSize = 9.5.sp,
                                                        color = TextSecondary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }

                                                // 2. QTY (Dedicated visual pill badge so Quantity never mixes with Item)
                                                Box(
                                                    modifier = Modifier.weight(0.9f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Surface(
                                                        color = Color(0xFFEFF6FF),
                                                        shape = RoundedCornerShape(4.dp),
                                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFBFDBFE))
                                                    ) {
                                                        Text(
                                                            text = "${item.pieces} p",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF1D4ED8),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                // 3. RATE
                                                Text(
                                                    text = "₹${item.rate.toInt()}",
                                                    fontSize = 10.5.sp,
                                                    color = TextPrimary,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.weight(0.8f)
                                                )

                                                // 4. PACKING
                                                val pack = if (item.loosePieces > 0) "${item.caseCount}c + ${item.loosePieces}L" else "${item.caseCount} cs"
                                                Text(
                                                    text = pack,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (item.loosePieces > 0) Color(0xFFD97706) else Color(0xFF15803D),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.weight(1.0f)
                                                )

                                                // 5. AMOUNT
                                                Text(
                                                    text = PdfGenerator.formatInr(item.totalAmount),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = NavyPrimary,
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier.weight(1.2f)
                                                )
                                            }

                                            // Packing Remarks / Note if present
                                            if (!item.mixedPackNote.isNullOrBlank()) {
                                                Surface(
                                                    color = Color(0xFFFFFBEB),
                                                    shape = RoundedCornerShape(3.dp),
                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFDE68A)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "↳ Note: ${item.mixedPackNote}",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF92400E),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            if (idx < supplierItems.size - 1) {
                                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Totals Summary Box (No packing summary at bottom)
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal ($totalPieces Pcs):", fontSize = 12.sp, color = TextSecondary)
                                    Text(PdfGenerator.formatInr(totalAmount), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Garment GST (5%):", fontSize = 12.sp, color = TextSecondary)
                                    Text(PdfGenerator.formatInr(totalGst), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("CONSOLIDATED GRAND TOTAL:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                                    Text(PdfGenerator.formatInr(grandTotal), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
