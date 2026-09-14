package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
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

private enum class PaymentViewTab(val label: String) {
    ALL_BILLS("All Bills"),
    CUSTOMERS("By Customer"),
    SUPPLIERS("By Supplier")
}

@Composable
fun PaymentsScreen(
    viewModel: HimatViewModel,
    onBack: () -> Unit,
    onOpenCustomer: (CustomerEntity) -> Unit = {},
    onOpenSupplier: (SupplierEntity) -> Unit = {},
    onOpenVisit: (VisitEntity) -> Unit = {}
) {
    val allEntries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    val allVisits by viewModel.visibleVisits.collectAsStateWithLifecycle()
    val allCustomers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val allSuppliers by viewModel.visibleSuppliers.collectAsStateWithLifecycle()

    val visitMap = remember(allVisits) { allVisits.associateBy { it.id } }
    val customerMap = remember(allCustomers) { allCustomers.associateBy { it.id } }
    val supplierMap = remember(allSuppliers) { allSuppliers.associateBy { it.id } }

    var selectedTab by remember { mutableStateOf(PaymentViewTab.ALL_BILLS) }
    var selectedStatusFilter by remember { mutableStateOf("All") } // "All", "Pending", "Partial", "Paid"
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    var entryToUpdate by remember { mutableStateOf<PurchaseEntryEntity?>(null) }
    var expandedCustomerIds by remember { mutableStateOf(setOf<Long>()) }
    var expandedSupplierIds by remember { mutableStateOf(setOf<Long>()) }

    // Helper to calculate total bill with GST for an entry
    val entryBillAmount: (PurchaseEntryEntity) -> Double = { entry ->
        if (entry.grandTotalWithGst > 0) entry.grandTotalWithGst else entry.totalAmount + entry.gstAmount
    }

    // Top Level Financial KPI Metrics
    val totalInvoiced = remember(allEntries) { allEntries.sumOf { entryBillAmount(it) } }
    val totalReceived = remember(allEntries) { allEntries.sumOf { it.paidAmount } }
    val totalOutstandingDue = remember(totalInvoiced, totalReceived) { maxOf(0.0, totalInvoiced - totalReceived) }
    val collectionPercent = remember(totalInvoiced, totalReceived) {
        if (totalInvoiced > 0) ((totalReceived / totalInvoiced) * 100).toInt().coerceIn(0, 100) else 100
    }

    // Counts for status chips
    val pendingCount = remember(allEntries) {
        allEntries.count { it.paymentStatus.equals("Pending", ignoreCase = true) }
    }
    val partialCount = remember(allEntries) {
        allEntries.count { it.paymentStatus.equals("Partial", ignoreCase = true) }
    }
    val paidCount = remember(allEntries) {
        allEntries.count { it.paymentStatus.equals("Paid", ignoreCase = true) || it.paymentStatus.equals("Received", ignoreCase = true) }
    }

    val cleanQuery = searchQuery.trim().lowercase()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // TOP HEADER BAR
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
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
                            text = "Payments & Bills Ledger",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Customer collections & supplier payment status",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Search Toggle Icon in Header Right Corner
                    Surface(
                        shape = CircleShape,
                        color = if (isSearchVisible || searchQuery.isNotBlank()) NavyPrimary else Color.White,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, if (isSearchVisible || searchQuery.isNotBlank()) NavyPrimary else Color(0xFFE2E8F0)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible) searchQuery = ""
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isSearchVisible || searchQuery.isNotBlank()) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchVisible || searchQuery.isNotBlank()) Color.White else NavyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Expandable Search Bar (Visible only when search icon is clicked or query active)
                AnimatedVisibility(
                    visible = isSearchVisible || searchQuery.isNotBlank(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Search by customer, supplier, order, city...",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // TOP FINANCIAL KPI SUMMARY CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OVERALL SETTLEMENT OVERVIEW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                color = if (collectionPercent >= 80) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "$collectionPercent% Settled",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (collectionPercent >= 80) Color(0xFF15803D) else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3 Financial Metric Pillars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Invoiced
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Total Billed", fontSize = 9.5.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₹${PdfGenerator.formatInr(totalInvoiced)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = NavyPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Total Collected / Paid
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Received", fontSize = 9.5.sp, color = Color(0xFF15803D))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₹${PdfGenerator.formatInr(totalReceived)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF15803D),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Total Due
                            Surface(
                                color = if (totalOutstandingDue > 0) Color(0xFFFFF1F2) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (totalOutstandingDue > 0) Color(0xFFFECDD3) else Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Balance Due", fontSize = 9.5.sp, color = if (totalOutstandingDue > 0) Color(0xFFBE123C) else TextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (totalOutstandingDue > 0) "₹${PdfGenerator.formatInr(totalOutstandingDue)}" else "Cleared ✓",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (totalOutstandingDue > 0) Color(0xFFBE123C) else Color(0xFF15803D),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { collectionPercent / 100f },
                            color = Color(0xFF059669),
                            trackColor = Color(0xFFE2E8F0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            // PRIMARY VIEW SELECTOR (TABS)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentViewTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Surface(
                            color = if (isSelected) NavyPrimary else Color.White,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable { selectedTab = tab }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // SECONDARY STATUS FILTER CHIPS
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf(
                        "All" to allEntries.size,
                        "Pending" to pendingCount,
                        "Partial" to partialCount,
                        "Paid" to paidCount
                    )
                    items(filters) { (status, count) ->
                        val isSelected = selectedStatusFilter == status
                        Surface(
                            color = if (isSelected) Color(0xFF0F172A) else Color.White,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedStatusFilter = status }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = status,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Surface(
                                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0xFFF1F5F9),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // CONTENT BASED ON SELECTED TAB
            when (selectedTab) {
                PaymentViewTab.ALL_BILLS -> {
                    // Filter All Bills
                    val filteredEntries = allEntries.filter { entry ->
                        val v = visitMap[entry.visitId]
                        val customerName = v?.customerName ?: ""
                        val date = v?.date ?: ""

                        val matchesStatus = when (selectedStatusFilter) {
                            "Pending" -> entry.paymentStatus.equals("Pending", ignoreCase = true)
                            "Partial" -> entry.paymentStatus.equals("Partial", ignoreCase = true)
                            "Paid" -> entry.paymentStatus.equals("Paid", ignoreCase = true) || entry.paymentStatus.equals("Received", ignoreCase = true)
                            else -> true
                        }

                        val matchesSearch = cleanQuery.isBlank() ||
                                entry.orderNo.lowercase().contains(cleanQuery) ||
                                entry.itemCode.lowercase().contains(cleanQuery) ||
                                entry.supplierName.lowercase().contains(cleanQuery) ||
                                customerName.lowercase().contains(cleanQuery) ||
                                date.lowercase().contains(cleanQuery)

                        matchesStatus && matchesSearch
                    }.sortedByDescending { it.id }

                    if (filteredEntries.isEmpty()) {
                        item {
                            EmptyPaymentState(message = "No bills match the selected filter.")
                        }
                    } else {
                        items(filteredEntries, key = { it.id }) { entry ->
                            val v = visitMap[entry.visitId]
                            val billAmount = entryBillAmount(entry)
                            val dueAmount = maxOf(0.0, billAmount - entry.paidAmount)

                            BillPaymentCard(
                                entry = entry,
                                visit = v,
                                billAmount = billAmount,
                                dueAmount = dueAmount,
                                onUpdatePayment = { entryToUpdate = entry }
                            )
                        }
                    }
                }

                PaymentViewTab.CUSTOMERS -> {
                    // Group entries by Customer
                    val entriesByCustomer = allEntries.groupBy { entry ->
                        val v = visitMap[entry.visitId]
                        v?.customerId ?: 0L
                    }

                    val customerLedgers = allCustomers.mapNotNull { customer ->
                        val entries = entriesByCustomer[customer.id] ?: emptyList()
                        if (entries.isEmpty()) null
                        else {
                            val billed = entries.sumOf { entryBillAmount(it) }
                            val paid = entries.sumOf { it.paidAmount }
                            val due = maxOf(0.0, billed - paid)
                            CustomerLedgerItem(
                                customer = customer,
                                totalBilled = billed,
                                totalPaid = paid,
                                totalDue = due,
                                entries = entries
                            )
                        }
                    }.filter { ledger ->
                        val matchesStatus = when (selectedStatusFilter) {
                            "Pending" -> ledger.totalDue > 0 && ledger.totalPaid == 0.0
                            "Partial" -> ledger.totalDue > 0 && ledger.totalPaid > 0.0
                            "Paid" -> ledger.totalDue <= 0.0 && ledger.totalBilled > 0.0
                            else -> true
                        }
                        val matchesSearch = cleanQuery.isBlank() ||
                                ledger.customer.name.lowercase().contains(cleanQuery) ||
                                ledger.customer.city.lowercase().contains(cleanQuery) ||
                                ledger.customer.phone.contains(cleanQuery)

                        matchesStatus && matchesSearch
                    }.sortedByDescending { it.totalDue }

                    if (customerLedgers.isEmpty()) {
                        item {
                            EmptyPaymentState(message = "No customer accounts match the selected filter.")
                        }
                    } else {
                        items(customerLedgers, key = { it.customer.id }) { ledger ->
                            val isExpanded = expandedCustomerIds.contains(ledger.customer.id)
                            CustomerLedgerCard(
                                ledger = ledger,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedCustomerIds = if (isExpanded) {
                                        expandedCustomerIds - ledger.customer.id
                                    } else {
                                        expandedCustomerIds + ledger.customer.id
                                    }
                                },
                                visitMap = visitMap,
                                entryBillAmount = entryBillAmount,
                                onUpdatePayment = { entryToUpdate = it }
                            )
                        }
                    }
                }

                PaymentViewTab.SUPPLIERS -> {
                    // Group entries by Supplier
                    val entriesBySupplier = allEntries.groupBy { it.supplierId }

                    val supplierLedgers = allSuppliers.mapNotNull { supplier ->
                        val entries = entriesBySupplier[supplier.id] ?: allEntries.filter {
                            it.supplierName.isNotBlank() && it.supplierName.trim().equals(supplier.name.trim(), ignoreCase = true)
                        }
                        if (entries.isEmpty()) null
                        else {
                            val billed = entries.sumOf { entryBillAmount(it) }
                            val paid = entries.sumOf { it.paidAmount }
                            val due = maxOf(0.0, billed - paid)
                            SupplierLedgerItem(
                                supplier = supplier,
                                totalSourced = billed,
                                totalPaid = paid,
                                totalDue = due,
                                entries = entries
                            )
                        }
                    }.filter { ledger ->
                        val matchesStatus = when (selectedStatusFilter) {
                            "Pending" -> ledger.totalDue > 0 && ledger.totalPaid == 0.0
                            "Partial" -> ledger.totalDue > 0 && ledger.totalPaid > 0.0
                            "Paid" -> ledger.totalDue <= 0.0 && ledger.totalSourced > 0.0
                            else -> true
                        }
                        val matchesSearch = cleanQuery.isBlank() ||
                                ledger.supplier.name.lowercase().contains(cleanQuery) ||
                                ledger.supplier.marketArea.lowercase().contains(cleanQuery) ||
                                ledger.supplier.type.lowercase().contains(cleanQuery)

                        matchesStatus && matchesSearch
                    }.sortedByDescending { it.totalDue }

                    if (supplierLedgers.isEmpty()) {
                        item {
                            EmptyPaymentState(message = "No supplier accounts match the selected filter.")
                        }
                    } else {
                        items(supplierLedgers, key = { it.supplier.id }) { ledger ->
                            val isExpanded = expandedSupplierIds.contains(ledger.supplier.id)
                            SupplierLedgerCard(
                                ledger = ledger,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedSupplierIds = if (isExpanded) {
                                        expandedSupplierIds - ledger.supplier.id
                                    } else {
                                        expandedSupplierIds + ledger.supplier.id
                                    }
                                },
                                visitMap = visitMap,
                                entryBillAmount = entryBillAmount,
                                onUpdatePayment = { entryToUpdate = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // RECORD / UPDATE PAYMENT DIALOG
    entryToUpdate?.let { entry ->
        val v = visitMap[entry.visitId]
        val billAmount = entryBillAmount(entry)

        RecordPaymentDialog(
            entry = entry,
            customerName = v?.customerName ?: "Customer",
            billAmount = billAmount,
            onDismiss = { entryToUpdate = null },
            onSave = { newStatus, newMode, newPaid, newRemarks ->
                viewModel.updatePaymentInfo(
                    entry = entry,
                    paymentStatus = newStatus,
                    paymentMode = newMode,
                    paidAmount = newPaid,
                    paymentRemarks = newRemarks
                ) {
                    entryToUpdate = null
                }
            }
        )
    }
}

// DATA CLASSES FOR LEDGERS
private data class CustomerLedgerItem(
    val customer: CustomerEntity,
    val totalBilled: Double,
    val totalPaid: Double,
    val totalDue: Double,
    val entries: List<PurchaseEntryEntity>
)

private data class SupplierLedgerItem(
    val supplier: SupplierEntity,
    val totalSourced: Double,
    val totalPaid: Double,
    val totalDue: Double,
    val entries: List<PurchaseEntryEntity>
)

// COMPONENT: BILL PAYMENT CARD (ALL BILLS VIEW)
@Composable
private fun BillPaymentCard(
    entry: PurchaseEntryEntity,
    visit: VisitEntity?,
    billAmount: Double,
    dueAmount: Double,
    onUpdatePayment: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Order No + Date + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Order ${entry.orderNo}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary
                    )
                    if (visit != null && visit.date.isNotBlank()) {
                        Text(
                            text = " • ${visit.date}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                PaymentStatusBadge(status = entry.paymentStatus)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF1F5F9))

            // Parties Row: Customer & Supplier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "👤 ${visit?.customerName ?: "Customer"}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🏭 ${entry.supplierName}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.supplierType.isNotBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            SupplierTypeBadge(type = entry.supplierType)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${entry.itemCode} • ${entry.pieces} pcs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "@ ₹${entry.rate.toInt()} / pc",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Financial Breakdown & Update Action
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Billed (GST)", fontSize = 9.sp, color = TextSecondary)
                            Text(
                                "₹${PdfGenerator.formatInr(billAmount)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Column {
                            Text("Paid (${entry.paymentMode})", fontSize = 9.sp, color = Color(0xFF15803D))
                            Text(
                                "₹${PdfGenerator.formatInr(entry.paidAmount)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                        Column {
                            Text("Balance Due", fontSize = 9.sp, color = if (dueAmount > 0) Color(0xFFBE123C) else TextSecondary)
                            Text(
                                text = if (dueAmount > 0) "₹${PdfGenerator.formatInr(dueAmount)}" else "Cleared ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dueAmount > 0) Color(0xFFBE123C) else Color(0xFF15803D)
                            )
                        }
                    }

                    // Update button
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, NavyPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onUpdatePayment)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = NavyPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Update", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }
                }
            }

            if (entry.paymentRemarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ref/Note: ${entry.paymentRemarks}",
                    fontSize = 9.5.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// COMPONENT: CUSTOMER LEDGER CARD
@Composable
private fun CustomerLedgerCard(
    ledger: CustomerLedgerItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    visitMap: Map<Long, VisitEntity>,
    entryBillAmount: (PurchaseEntryEntity) -> Double,
    onUpdatePayment: (PurchaseEntryEntity) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Customer Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ledger.customer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = NavyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${ledger.customer.city}${if (ledger.customer.phone.isNotBlank()) " • ${ledger.customer.phone}" else ""}",
                        fontSize = 10.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                PaymentAccountStatusBadge(due = ledger.totalDue, paid = ledger.totalPaid)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3 Financial Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Billed", fontSize = 9.5.sp, color = TextSecondary)
                    Text("₹${PdfGenerator.formatInr(ledger.totalBilled)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
                }
                Column {
                    Text("Total Paid", fontSize = 9.5.sp, color = Color(0xFF15803D))
                    Text("₹${PdfGenerator.formatInr(ledger.totalPaid)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF15803D))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Balance Due", fontSize = 9.5.sp, color = if (ledger.totalDue > 0) Color(0xFFBE123C) else TextSecondary)
                    Text(
                        text = if (ledger.totalDue > 0) "₹${PdfGenerator.formatInr(ledger.totalDue)}" else "Cleared ✓",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (ledger.totalDue > 0) Color(0xFFBE123C) else Color(0xFF15803D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Accordion Toggle
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleExpand)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${ledger.entries.size} Orders / Bills Breakdown",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = NavyPrimary
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = NavyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expanded List of Bills
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ledger.entries.forEach { entry ->
                        val billAmt = entryBillAmount(entry)
                        val dueAmt = maxOf(0.0, billAmt - entry.paidAmount)

                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(entry.orderNo, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NavyPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(entry.itemCode, fontSize = 10.5.sp, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("(${entry.supplierName})", fontSize = 10.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(
                                        text = "Billed: ₹${PdfGenerator.formatInr(billAmt)} • Paid: ₹${PdfGenerator.formatInr(entry.paidAmount)} • Due: ${if (dueAmt > 0) "₹${PdfGenerator.formatInr(dueAmt)}" else "0"}",
                                        fontSize = 9.5.sp,
                                        color = TextSecondary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PaymentStatusBadge(status = entry.paymentStatus)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onUpdatePayment(entry) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Update",
                                            tint = NavyPrimary,
                                            modifier = Modifier.size(13.dp)
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
}

// COMPONENT: SUPPLIER LEDGER CARD
@Composable
private fun SupplierLedgerCard(
    ledger: SupplierLedgerItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    visitMap: Map<Long, VisitEntity>,
    entryBillAmount: (PurchaseEntryEntity) -> Double,
    onUpdatePayment: (PurchaseEntryEntity) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Supplier Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ledger.supplier.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = NavyPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        SupplierTypeBadge(type = ledger.supplier.type)
                    }
                    if (ledger.supplier.marketArea.isNotBlank()) {
                        Text(
                            text = ledger.supplier.marketArea,
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                PaymentAccountStatusBadge(due = ledger.totalDue, paid = ledger.totalPaid)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3 Financial Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Sourced", fontSize = 9.5.sp, color = TextSecondary)
                    Text("₹${PdfGenerator.formatInr(ledger.totalSourced)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
                }
                Column {
                    Text("Total Paid", fontSize = 9.5.sp, color = Color(0xFF15803D))
                    Text("₹${PdfGenerator.formatInr(ledger.totalPaid)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF15803D))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Payable Due", fontSize = 9.5.sp, color = if (ledger.totalDue > 0) Color(0xFFBE123C) else TextSecondary)
                    Text(
                        text = if (ledger.totalDue > 0) "₹${PdfGenerator.formatInr(ledger.totalDue)}" else "Settled ✓",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (ledger.totalDue > 0) Color(0xFFBE123C) else Color(0xFF15803D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Accordion Toggle
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleExpand)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${ledger.entries.size} Wholesale Vouchers",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = NavyPrimary
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = NavyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expanded List of Vouchers
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ledger.entries.forEach { entry ->
                        val billAmt = entryBillAmount(entry)
                        val dueAmt = maxOf(0.0, billAmt - entry.paidAmount)
                        val v = visitMap[entry.visitId]

                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(entry.orderNo, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NavyPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(entry.itemCode, fontSize = 10.5.sp, color = TextPrimary)
                                        if (v != null) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("(${v.customerName})", fontSize = 10.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Text(
                                        text = "Value: ₹${PdfGenerator.formatInr(billAmt)} • Paid: ₹${PdfGenerator.formatInr(entry.paidAmount)} • Due: ${if (dueAmt > 0) "₹${PdfGenerator.formatInr(dueAmt)}" else "0"}",
                                        fontSize = 9.5.sp,
                                        color = TextSecondary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PaymentStatusBadge(status = entry.paymentStatus)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onUpdatePayment(entry) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Update",
                                            tint = NavyPrimary,
                                            modifier = Modifier.size(13.dp)
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
}

// COMPONENT: PAYMENT STATUS BADGE
@Composable
fun PaymentStatusBadge(status: String) {
    val (bgColor, textColor, icon: ImageVector) = when (status.lowercase()) {
        "paid", "received" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.CheckCircle)
        "partial" -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.Schedule)
        else -> Triple(Color(0xFFFFE4E6), Color(0xFFBE123C), Icons.Default.Warning)
    }

    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(10.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = when (status.lowercase()) {
                    "received" -> "Paid"
                    else -> status.replaceFirstChar { it.uppercase() }
                },
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// COMPONENT: ACCOUNT STATUS BADGE (OVERALL FOR CUSTOMER / SUPPLIER)
@Composable
private fun PaymentAccountStatusBadge(due: Double, paid: Double) {
    val (text, bgColor, textColor) = when {
        due <= 0.0 -> Triple("Settled ✓", Color(0xFFDCFCE7), Color(0xFF15803D))
        paid > 0.0 -> Triple("Partial Due", Color(0xFFFEF3C7), Color(0xFFB45309))
        else -> Triple("Pending Due", Color(0xFFFFE4E6), Color(0xFFBE123C))
    }

    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// COMPONENT: RECORD / UPDATE PAYMENT DIALOG
@Composable
fun RecordPaymentDialog(
    entry: PurchaseEntryEntity,
    customerName: String,
    billAmount: Double,
    onDismiss: () -> Unit,
    onSave: (paymentStatus: String, paymentMode: String, paidAmount: Double, paymentRemarks: String) -> Unit
) {
    var status by remember { mutableStateOf(entry.paymentStatus) }
    var mode by remember { mutableStateOf(if (entry.paymentMode.isNotBlank()) entry.paymentMode else "Cash") }
    var paidAmountText by remember { mutableStateOf(if (entry.paidAmount > 0) entry.paidAmount.toInt().toString() else "") }
    var remarks by remember { mutableStateOf(entry.paymentRemarks) }

    val currentPaid = paidAmountText.toDoubleOrNull() ?: 0.0
    val remainingBalance = maxOf(0.0, billAmount - currentPaid)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Dialog Header
                Text(
                    text = "Update Payment: Order ${entry.orderNo}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NavyPrimary
                )
                Text(
                    text = "👤 $customerName • 🏭 ${entry.supplierName}",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bill Amount Reference Box
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Invoice (incl. GST)", fontSize = 10.sp, color = TextSecondary)
                            Text("₹${PdfGenerator.formatInr(billAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Remaining Due", fontSize = 10.sp, color = if (remainingBalance > 0) Color(0xFFBE123C) else Color(0xFF15803D))
                            Text(
                                text = if (remainingBalance > 0) "₹${PdfGenerator.formatInr(remainingBalance)}" else "Zero Due ✓",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (remainingBalance > 0) Color(0xFFBE123C) else Color(0xFF15803D)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Selection
                Text("Payment Status", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Pending", "Partial", "Received").forEach { s ->
                        val isSelected = status.equals(s, ignoreCase = true)
                        Surface(
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable {
                                    status = s
                                    if (s == "Received") {
                                        paidAmountText = billAmount.toInt().toString()
                                    } else if (s == "Pending") {
                                        paidAmountText = "0"
                                    }
                                }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (s) {
                                        "Received" -> "Paid"
                                        else -> s
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Amount Paid Input
                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = { input ->
                        paidAmountText = input
                        val p = input.toDoubleOrNull() ?: 0.0
                        if (p >= billAmount && billAmount > 0) {
                            status = "Received"
                        } else if (p > 0) {
                            status = "Partial"
                        } else {
                            status = "Pending"
                        }
                    },
                    label = { Text("Amount Paid (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Mode Selection
                Text("Payment Mode", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Cash", "UPI", "Online", "Cheque").forEach { m ->
                        val isSelected = mode.equals(m, ignoreCase = true)
                        Surface(
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable { mode = m }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = m,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Remarks / Reference
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("UTR / Cheque No / Remarks") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val resolvedPaid = paidAmountText.toDoubleOrNull() ?: 0.0
                            val finalStatus = when {
                                resolvedPaid >= billAmount && billAmount > 0 -> "Received"
                                resolvedPaid > 0 -> "Partial"
                                else -> "Pending"
                            }
                            onSave(finalStatus, mode, resolvedPaid, remarks)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = CircleShape
                    ) {
                        Text("Save Payment")
                    }
                }
            }
        }
    }
}

// EMPTY STATE COMPONENT
@Composable
private fun EmptyPaymentState(message: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No Payment Records",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = NavyPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 11.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
