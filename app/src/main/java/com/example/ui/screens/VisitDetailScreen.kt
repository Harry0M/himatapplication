package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.IncompleteCaseBanner
import com.example.ui.components.StatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator

@Composable
fun VisitDetailScreen(
    viewModel: HimatViewModel,
    visit: VisitEntity,
    onBack: () -> Unit,
    onOpenAddEntry: () -> Unit,
    onOpenMixedPack: () -> Unit,
    onOpenCustomerReport: (VisitEntity) -> Unit,
    onOpenSupplierCopy: (VisitEntity, SupplierEntity) -> Unit
) {
    val entries by viewModel.visitEntries.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()

    var showSupplierPickerMenu by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<PurchaseEntryEntity?>(null) }

    val totalPieces = entries.sumOf { it.pieces }
    val totalAmount = entries.sumOf { it.totalAmount }
    val totalCases = entries.sumOf { it.caseCount }
    val looseEntries = entries.filter { it.loosePieces > 0 }
    val totalLoose = looseEntries.sumOf { it.loosePieces }

    // Distinct suppliers visited in this trip
    val visitedSuppliers = suppliers.filter { sup ->
        entries.any { it.supplierId == sup.id }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddEntry,
                containerColor = NavyPrimary,
                contentColor = GoldAccent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Stop", fontWeight = FontWeight.Bold, color = GoldAccent)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Navigation & Visit Header (Well-proportioned & responsive)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = visit.customerName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${visit.visitCode} • ${visit.date}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            // Compact Status Toggle Chip
                            val isActive = visit.status.equals("Active", ignoreCase = true)
                            Surface(
                                color = if (isActive) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isActive) Color(0xFF93C5FD) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val next = if (isActive) "Completed" else "Active"
                                        viewModel.updateVisitStatus(visit, next)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isActive) Icons.Default.Schedule else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isActive) Color(0xFF1D4ED8) else Color(0xFF15803D),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isActive) "Active" else "Done",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color(0xFF1D4ED8) else Color(0xFF15803D)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Salesman: ${visit.employeeName}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Tap status chip to switch",
                                fontSize = 10.5.sp,
                                color = NavyPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Trip Sourcing Summary (Clean 4-metric strip)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Pcs", fontSize = 10.5.sp, color = TextSecondary)
                            Text("$totalPieces", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE2E8F0)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Full Cases", fontSize = 10.5.sp, color = TextSecondary)
                            Text("$totalCases", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF059669))
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE2E8F0)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Loose Pcs", fontSize = 10.5.sp, color = TextSecondary)
                            Text(
                                "$totalLoose",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (totalLoose > 0) Color(0xFFD97706) else Color(0xFF15803D)
                            )
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE2E8F0)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Trip Value", fontSize = 10.5.sp, color = TextSecondary)
                            Text(PdfGenerator.formatInr(totalAmount), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        }
                    }
                }
            }

            // Documents & Vouchers Toolbar (Clean, compact, no long explanations)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sourcing Documents",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "${visitedSuppliers.size} Supplier Stop(s)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Button 1: Customer Day Report
                            Button(
                                onClick = { onOpenCustomerReport(visit) },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Customer Report",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }

                            // Button 2: Supplier Copy Picker
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showSupplierPickerMenu = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Supplier Bills (${visitedSuppliers.size})",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary,
                                        maxLines = 1
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSupplierPickerMenu,
                                    onDismissRequest = { showSupplierPickerMenu = false }
                                ) {
                                    if (visitedSuppliers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No supplier stops logged yet") },
                                            onClick = { showSupplierPickerMenu = false }
                                        )
                                    } else {
                                        visitedSuppliers.forEach { sup ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text("${sup.type} • ${sup.marketArea}", fontSize = 11.sp, color = TextSecondary)
                                                    }
                                                },
                                                onClick = {
                                                    showSupplierPickerMenu = false
                                                    onOpenSupplierCopy(visit, sup)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Single Compact Combine Banner (Only if loose pieces exist)
            if (totalLoose > 0) {
                item {
                    IncompleteCaseBanner(
                        looseCount = totalLoose,
                        ordersCount = looseEntries.size,
                        onMixedPackClick = onOpenMixedPack
                    )
                }
            }

            // Stops Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Supplier Stops (${entries.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Grouped by Wholesale Stop",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Entries List
            if (entries.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No items logged for this visit yet", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap '+ Add Stop' below when customer purchases from a shop.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(entries) { entry ->
                    PurchaseEntryItemCard(
                        entry = entry,
                        onDelete = { entryToDelete = entry },
                        onViewSupplierCopy = {
                            val sup = suppliers.find { it.id == entry.supplierId }
                            if (sup != null) {
                                onOpenSupplierCopy(visit, sup)
                            }
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Purchase Entry?") },
            text = { Text("Remove order ${entry.orderNo} (${entry.itemCode} from ${entry.supplierName})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePurchaseEntry(entry)
                        entryToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PurchaseEntryItemCard(
    entry: PurchaseEntryEntity,
    onDelete: () -> Unit,
    onViewSupplierCopy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Order No, Supplier Name, Supplier Type Badge, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        color = NavyPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = entry.orderNo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = NavyPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.supplierName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    SupplierTypeBadge(type = entry.supplierType)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color.Gray,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Item Code, Pieces, Rate, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Item: ${entry.itemCode}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${entry.pieces} Pcs @ ${PdfGenerator.formatInr(entry.rate)}/pc",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = PdfGenerator.formatInr(entry.totalAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = NavyPrimary
                    )
                    Text(
                        text = "+ GST 5%: ${PdfGenerator.formatInr(entry.gstAmount)}",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Row 3: Packing Breakdown Badge & Status Badge (Neatly sized and constrained)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasLoose = entry.loosePieces > 0
                Surface(
                    color = if (hasLoose) Color(0xFFFEF3C7) else Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = if (hasLoose) "📦 ${entry.caseCount} Cs + ${entry.loosePieces} Loose (${entry.caseSize}/cs)"
                        else "📦 ${entry.caseCount} Cases (${entry.caseSize} pcs/cs)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasLoose) Color(0xFF92400E) else Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                StatusBadge(status = entry.deliveryStatus)
            }

            // Mixed Packing Note if linked
            if (!entry.mixedPackNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔗 ${entry.mixedPackNote}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Row 4: View Supplier Voucher Quick Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "View Supplier Voucher →",
                    color = NavyPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onViewSupplierCopy() }
                        .padding(top = 4.dp, bottom = 2.dp)
                )
            }
        }
    }
}

