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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NavyPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Customer Consolidated Day Report",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "All wholesaler purchases aggregated for ${visit.customerName}",
                            fontSize = 11.sp,
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

                        // Table Header
                        Surface(
                            color = NavyPrimary,
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ORDER", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(55.dp))
                                Text("ITEM", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(1f))
                                Text("PCS", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(35.dp))
                                Text("RATE", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(45.dp))
                                Text("PACK", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(55.dp))
                                Text("AMOUNT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(65.dp))
                            }
                        }

                        // Grouped by Supplier
                        val grouped = entries.groupBy { it.supplierName }
                        grouped.forEach { (supplierName, supplierItems) ->
                            val supType = supplierItems.firstOrNull()?.supplierType ?: ""
                            Surface(
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "▶ $supplierName",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = NavyPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SupplierTypeBadge(type = supType)
                                }
                            }

                            supplierItems.forEach { item ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.orderNo, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.width(55.dp))
                                        Text(item.itemCode, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                        Text("${item.pieces}", fontSize = 11.sp, modifier = Modifier.width(35.dp))
                                        Text("₹${item.rate.toInt()}", fontSize = 11.sp, modifier = Modifier.width(45.dp))
                                        val pack = if (item.loosePieces > 0) "${item.caseCount}c+${item.loosePieces}L" else "${item.caseCount}c"
                                        Text(pack, fontSize = 10.sp, color = if (item.loosePieces > 0) Color(0xFFD97706) else Color(0xFF15803D), modifier = Modifier.width(55.dp))
                                        Text(PdfGenerator.formatInr(item.totalAmount), fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.width(65.dp))
                                    }

                                    if (!item.mixedPackNote.isNullOrBlank()) {
                                        Text(
                                            text = "↳ ${item.mixedPackNote}",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(start = 55.dp, bottom = 4.dp)
                                        )
                                    }

                                    Divider(color = Color(0xFFF1F5F9))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Totals Summary Box
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Packing Summary:", fontSize = 12.sp, color = TextSecondary)
                                    Text("$totalCases Cases + $totalLoose Loose Pcs", fontSize = 11.5.sp, color = NavyPrimary, fontWeight = FontWeight.SemiBold)
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("CONSOLIDATED GRAND TOTAL:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                                    Text(PdfGenerator.formatInr(grandTotal), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                                }
                            }
                        }

                        // Mixed Pack Groups Breakdown
                        if (packGroups.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Mixed Packing Summary:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                            Spacer(modifier = Modifier.height(4.dp))
                            packGroups.forEach { pg ->
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${pg.packGroupCode}: ${pg.note}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
