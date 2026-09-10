package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
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
import com.example.data.local.entity.SupplierEntity
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
fun SupplierReportScreen(
    viewModel: HimatViewModel,
    visit: VisitEntity,
    initialSupplier: SupplierEntity,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allEntries by viewModel.visitEntries.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val customer = allCustomers.find { it.id == visit.customerId }

    // Suppliers visited in this trip
    val visitedSuppliers = suppliers.filter { sup ->
        allEntries.any { it.supplierId == sup.id }
    }

    var selectedSupplier by remember { mutableStateOf(initialSupplier) }

    val supplierEntries = allEntries.filter { it.supplierId == selectedSupplier.id }

    val totalPieces = supplierEntries.sumOf { it.pieces }
    val totalAmount = supplierEntries.sumOf { it.totalAmount }
    val totalGst = supplierEntries.sumOf { it.gstAmount }
    val grandTotal = totalAmount + totalGst
    val totalCases = supplierEntries.sumOf { it.caseCount }
    val totalLoose = supplierEntries.sumOf { it.loosePieces }

    Scaffold(
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp Share
                    Button(
                        onClick = { viewModel.shareSupplierCopyWhatsApp(visit, selectedSupplier) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("🟢 WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // PDF Export & Share
                    Button(
                        onClick = { viewModel.shareSupplierCopyPdf(visit, selectedSupplier) },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF Bill", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Copy Text
                    OutlinedButton(
                        onClick = {
                            val text = ShareUtil.buildSupplierCopyText(visit, selectedSupplier, customer, supplierEntries)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Supplier Copy", text))
                            Toast.makeText(context, "Supplier bill copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NavyPrimary, modifier = Modifier.size(16.dp))
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
                            text = "Supplier Purchase Copy",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Wholesaler-specific purchase voucher & packing sheet",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Supplier Selector Tabs if multiple suppliers visited
            if (visitedSuppliers.size > 1) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visitedSuppliers) { sup ->
                            val isSelected = sup.id == selectedSupplier.id
                            Surface(
                                color = if (isSelected) NavyPrimary else Color.White,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NavyPrimary else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { selectedSupplier = sup }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sup.name,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SupplierTypeBadge(type = sup.type)
                                }
                            }
                        }
                    }
                }
            }

            // Document View
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
                            Column {
                                Text(
                                    text = "HIMAT TEXTILE",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "GARMENT SOURCING AGENCY • SUPPLIER COPY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                                Text(
                                    text = "Spot Procurement Voucher for Wholesaler / Manufacturer",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = Color(0xFF7C3AED),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SUPPLIER VOUCHER",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Date: ${visit.date}", fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Supplier & Buyer Info
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("SUPPLIER: ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        SupplierTypeBadge(type = selectedSupplier.type)
                                    }
                                    Text(selectedSupplier.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text("${selectedSupplier.marketArea} • Contact: ${selectedSupplier.contactPerson}", fontSize = 11.sp, color = TextSecondary)
                                    if (selectedSupplier.gstin.isNotBlank()) {
                                        Text("GSTIN: ${selectedSupplier.gstin}", fontSize = 10.5.sp, color = NavyPrimary)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("BUYER / RETAILER:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Text(customer?.name ?: visit.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                                    Text("${customer?.city ?: ""} • Escort: ${visit.employeeName}", fontSize = 11.sp, color = TextSecondary)
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
                                Text("ORDER", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(60.dp))
                                Text("ITEM", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(1f))
                                Text("PCS", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(40.dp))
                                Text("RATE", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(50.dp))
                                Text("PACKING", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(65.dp))
                                Text("AMOUNT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(65.dp))
                            }
                        }

                        // Table Rows
                        supplierEntries.forEach { item ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.orderNo, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.width(60.dp))
                                    Text(item.itemCode, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                                    Text("${item.pieces}", fontSize = 11.sp, modifier = Modifier.width(40.dp))
                                    Text("₹${item.rate.toInt()}", fontSize = 11.sp, modifier = Modifier.width(50.dp))
                                    val pack = if (item.loosePieces > 0) "${item.caseCount}c+${item.loosePieces}L" else "${item.caseCount}c"
                                    Text(pack, fontSize = 10.sp, color = if (item.loosePieces > 0) Color(0xFFD97706) else Color(0xFF15803D), modifier = Modifier.width(65.dp))
                                    Text(PdfGenerator.formatInr(item.totalAmount), fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.width(65.dp))
                                }

                                // Crucial: Packing instruction note showing what was mixed with!
                                if (!item.mixedPackNote.isNullOrBlank()) {
                                    Surface(
                                        color = Color(0xFFFFFBEB),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ PACKING INSTRUCTION: ${item.mixedPackNote}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF92400E),
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }

                                Divider(color = Color(0xFFF1F5F9))
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
                                    Text("Total Quantity:", fontSize = 12.sp, color = TextSecondary)
                                    Text("$totalPieces Pcs ($totalCases Cases, $totalLoose Loose)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Taxable Subtotal:", fontSize = 12.sp, color = TextSecondary)
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

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("SUPPLIER ORDER NET TOTAL:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NavyPrimary)
                                    Text(PdfGenerator.formatInr(grandTotal), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "• Supplier to issue original GST Tax Invoice against this purchase order.",
                            fontSize = 9.5.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
