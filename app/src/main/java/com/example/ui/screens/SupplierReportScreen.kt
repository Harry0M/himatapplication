package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PurchaseEntryEntity
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

    // Helper to check if entry belongs to supplier by ID or canonical name
    val isSameSupplier: (PurchaseEntryEntity, SupplierEntity) -> Boolean = { entry, sup ->
        entry.supplierId == sup.id || (entry.supplierName.isNotBlank() && entry.supplierName.trim().equals(sup.name.trim(), ignoreCase = true))
    }

    // Suppliers visited in this trip
    val visitedSuppliers = suppliers.filter { sup ->
        allEntries.any { isSameSupplier(it, sup) }
    }

    var selectedSupplier by remember(initialSupplier.id) {
        mutableStateOf(
            visitedSuppliers.find { it.id == initialSupplier.id || (it.name.isNotBlank() && it.name.trim().equals(initialSupplier.name.trim(), ignoreCase = true)) } ?: initialSupplier
        )
    }

    // All entries for the selected supplier in this visit (aggregates multiple stops if any)
    val supplierEntries = allEntries.filter { isSameSupplier(it, selectedSupplier) }

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
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // WhatsApp Share
                    Button(
                        onClick = { viewModel.shareSupplierCopyWhatsApp(visit, selectedSupplier) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }

                    // PDF Export & Share
                    Button(
                        onClick = { viewModel.shareSupplierCopyPdf(visit, selectedSupplier) },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF Bill", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(0.6f)
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NavyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Supplier Purchase Copy",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Wholesaler voucher & packing sheet",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Supplier Selector Tabs if multiple suppliers visited
            if (visitedSuppliers.size > 1) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(visitedSuppliers) { sup ->
                            val isSelected = sup.id == selectedSupplier.id || (sup.name.isNotBlank() && sup.name.trim().equals(selectedSupplier.name.trim(), ignoreCase = true))
                            Surface(
                                color = if (isSelected) NavyPrimary else Color.White,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NavyPrimary else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedSupplier = sup }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sup.name,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    SupplierTypeBadge(type = sup.type)
                                }
                            }
                        }
                    }
                }
            }

            // Document View Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Letterhead
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.himat_logo),
                                    contentDescription = "Himat Textile Logo",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .padding(end = 10.dp)
                                )
                                Column {
                                    Text(
                                        text = "HIMAT TEXTILE",
                                        fontSize = 16.sp,
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
                                        text = "Spot Procurement Voucher for Wholesaler / Manufacturer",
                                        fontSize = 8.5.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = Color(0xFF7C3AED),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SUPPLIER VOUCHER",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Date: ${visit.date}", fontSize = 9.5.sp, color = TextSecondary)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                        // Supplier & Buyer Compact Info Box
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Supplier Line
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SUPPLIER: ", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    SupplierTypeBadge(type = selectedSupplier.type)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = selectedSupplier.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (selectedSupplier.marketArea.isNotBlank()) {
                                        Text(
                                            text = selectedSupplier.marketArea,
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)

                                // Buyer Line
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("BUYER: ", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Text(
                                        text = customer?.name ?: visit.customerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NavyPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val escortCity = buildString {
                                        if (!customer?.city.isNullOrBlank()) append(customer?.city)
                                        if (visit.employeeName.isNotBlank()) {
                                            if (isNotEmpty()) append(" • ")
                                            append("Escort: ${visit.employeeName}")
                                        }
                                    }
                                    if (escortCity.isNotBlank()) {
                                        Text(
                                            text = escortCity,
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Table Header (Weighted columns to guarantee fit on any screen)
                        Surface(
                            color = NavyPrimary,
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ITEM / ORDER", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color.White, modifier = Modifier.weight(1.8f))
                                Text("PCS", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
                                Text("RATE", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                                Text("PACK", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1.0f))
                                Text("AMOUNT", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color.White, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                            }
                        }

                        // Table Rows
                        supplierEntries.forEach { item ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Item Code & Order No stacked
                                    Column(modifier = Modifier.weight(1.8f)) {
                                        Text(
                                            text = item.itemCode,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.orderNo,
                                            fontSize = 9.sp,
                                            color = TextSecondary,
                                            maxLines = 1
                                        )
                                    }

                                    // Pieces
                                    Text(
                                        text = "${item.pieces}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.7f)
                                    )

                                    // Rate
                                    Text(
                                        text = "₹${item.rate.toInt()}",
                                        fontSize = 10.5.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.8f)
                                    )

                                    // Packing
                                    val pack = if (item.loosePieces > 0) "${item.caseCount}c+${item.loosePieces}L" else "${item.caseCount}c"
                                    Text(
                                        text = pack,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        color = if (item.loosePieces > 0) Color(0xFFD97706) else Color(0xFF15803D),
                                        modifier = Modifier.weight(1.0f)
                                    )

                                    // Amount
                                    Text(
                                        text = PdfGenerator.formatInr(item.totalAmount),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp,
                                        textAlign = TextAlign.End,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }

                                // Packing instruction note if present
                                if (!item.mixedPackNote.isNullOrBlank()) {
                                    Surface(
                                        color = Color(0xFFFFFBEB),
                                        shape = RoundedCornerShape(3.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "PACKING: ${item.mixedPackNote}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF92400E),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Totals Summary Box
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Quantity:", fontSize = 11.5.sp, color = TextSecondary)
                                    Text("$totalPieces Pcs ($totalCases Cases, $totalLoose Loose)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Taxable Subtotal:", fontSize = 11.5.sp, color = TextSecondary)
                                    Text(PdfGenerator.formatInr(totalAmount), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Garment GST (5%):", fontSize = 11.5.sp, color = TextSecondary)
                                    Text(PdfGenerator.formatInr(totalGst), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE2E8F0))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SUPPLIER ORDER NET TOTAL:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = NavyPrimary)
                                    Text(PdfGenerator.formatInr(grandTotal), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "• Supplier to issue original GST Tax Invoice against this purchase order.",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
