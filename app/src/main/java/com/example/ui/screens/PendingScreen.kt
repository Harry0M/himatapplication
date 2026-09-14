package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator

enum class PendingFilterTab {
    ALL,
    PAYMENTS,
    DELIVERIES,
    TRIPS,
    LOOSE_PACKS
}

@Composable
fun PendingScreen(
    viewModel: HimatViewModel,
    onBack: () -> Unit,
    onOpenVisit: (VisitEntity) -> Unit,
    onOpenCustomer: (CustomerEntity) -> Unit,
    onOpenSupplier: (SupplierEntity) -> Unit,
    onOpenMixedPack: () -> Unit
) {
    val visits by viewModel.visibleVisits.collectAsStateWithLifecycle()
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.visibleSuppliers.collectAsStateWithLifecycle()
    val packGroups by viewModel.allPackGroups.collectAsStateWithLifecycle()

    val visitMap = remember(visits) { visits.associateBy { it.id } }
    val customerMap = remember(customers) { customers.associateBy { it.id } }
    val supplierMap = remember(suppliers) { suppliers.associateBy { it.id } }

    var selectedTab by remember { mutableStateOf(PendingFilterTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Dialog state for inline updates
    var paymentEntryToUpdate by remember { mutableStateOf<PurchaseEntryEntity?>(null) }
    var deliveryEntryToUpdate by remember { mutableStateOf<PurchaseEntryEntity?>(null) }

    // 1. Pending Payments (Unpaid or Partial)
    val allPendingPayments = remember(entries) {
        entries.filter {
            !it.paymentStatus.equals("Paid", ignoreCase = true) &&
                    !it.paymentStatus.equals("Received", ignoreCase = true)
        }
    }
    val totalPendingDues = remember(allPendingPayments) {
        allPendingPayments.sumOf { maxOf(0.0, (it.totalAmount + it.gstAmount) - it.paidAmount) }
    }

    // 2. Pending Deliveries (Not delivered yet)
    val allPendingDeliveries = remember(entries) {
        entries.filter { !it.deliveryStatus.equals("Delivered", ignoreCase = true) }
    }

    // 3. Active Trips
    val allActiveTrips = remember(visits) {
        visits.filter { it.status.equals("Active", ignoreCase = true) }
    }

    // 4. Loose Pack Entries (only entries genuinely unpacked and not in mixed cases)
    val packedEntryIds = remember(packGroups) {
        packGroups.flatMap { group ->
            group.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() }
        }.toSet()
    }
    val allLooseEntries = remember(entries, packedEntryIds) {
        entries.filter { entry ->
            entry.loosePieces > 0 &&
                    (entry.packGroupId == null || entry.packGroupId == 0L) &&
                    entry.mixedPackNote.isNullOrBlank() &&
                    entry.id !in packedEntryIds
        }
    }
    val totalLoosePcs = remember(allLooseEntries) {
        allLooseEntries.sumOf { it.loosePieces }
    }

    val totalPendingCount = allPendingPayments.size + allPendingDeliveries.size + allActiveTrips.size + allLooseEntries.size

    // Search Filtering
    val cleanQuery = searchQuery.trim().lowercase()

    val filteredPayments = remember(allPendingPayments, cleanQuery, visitMap) {
        if (cleanQuery.isBlank()) allPendingPayments
        else {
            allPendingPayments.filter { entry ->
                val custName = visitMap[entry.visitId]?.customerName ?: ""
                entry.orderNo.contains(cleanQuery, ignoreCase = true) ||
                        entry.itemCode.contains(cleanQuery, ignoreCase = true) ||
                        entry.supplierName.contains(cleanQuery, ignoreCase = true) ||
                        custName.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    val filteredDeliveries = remember(allPendingDeliveries, cleanQuery) {
        if (cleanQuery.isBlank()) allPendingDeliveries
        else {
            allPendingDeliveries.filter { entry ->
                entry.orderNo.contains(cleanQuery, ignoreCase = true) ||
                        entry.itemCode.contains(cleanQuery, ignoreCase = true) ||
                        entry.supplierName.contains(cleanQuery, ignoreCase = true) ||
                        entry.transporter.contains(cleanQuery, ignoreCase = true) ||
                        entry.deliveryStatus.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    val filteredTrips = remember(allActiveTrips, cleanQuery) {
        if (cleanQuery.isBlank()) allActiveTrips
        else {
            allActiveTrips.filter { trip ->
                trip.customerName.contains(cleanQuery, ignoreCase = true) ||
                        trip.visitCode.contains(cleanQuery, ignoreCase = true) ||
                        trip.employeeName.contains(cleanQuery, ignoreCase = true) ||
                        trip.date.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    val filteredLooseEntries = remember(allLooseEntries, cleanQuery, visitMap) {
        if (cleanQuery.isBlank()) allLooseEntries
        else {
            allLooseEntries.filter { entry ->
                val custName = visitMap[entry.visitId]?.customerName ?: ""
                entry.orderNo.contains(cleanQuery, ignoreCase = true) ||
                        entry.itemCode.contains(cleanQuery, ignoreCase = true) ||
                        entry.supplierName.contains(cleanQuery, ignoreCase = true) ||
                        custName.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
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
                            text = "Pending Operations",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = if (totalPendingCount > 0) "$totalPendingCount pending items requiring action" else "All tasks are cleared ✓",
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Search Toggle Icon Button in top-right header corner
                    Surface(
                        shape = CircleShape,
                        color = if (isSearchVisible || searchQuery.isNotBlank()) NavyPrimary else Color.White,
                        shadowElevation = 2.dp,
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
                                contentDescription = "Toggle Search",
                                tint = if (isSearchVisible || searchQuery.isNotBlank()) Color.White else NavyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Smooth Expandable Pill Search Bar
                AnimatedVisibility(
                    visible = isSearchVisible || searchQuery.isNotBlank(),
                    enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(200)),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(180))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Search pending orders, customers, trips...",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // TOP METRIC OVERVIEW CARDS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pending Payments Stat Card
                    PendingMetricCard(
                        title = "Pending Due",
                        value = "₹${PdfGenerator.formatInr(totalPendingDues)}",
                        countLabel = "${allPendingPayments.size} Unpaid Bills",
                        icon = Icons.Default.Payments,
                        color = Color(0xFFD97706), // Amber
                        isSelected = selectedTab == PendingFilterTab.PAYMENTS,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedTab = if (selectedTab == PendingFilterTab.PAYMENTS) PendingFilterTab.ALL else PendingFilterTab.PAYMENTS
                        }
                    )

                    // Pending Deliveries Stat Card
                    PendingMetricCard(
                        title = "Deliveries",
                        value = "${allPendingDeliveries.size}",
                        countLabel = "In Transit / Packed",
                        icon = Icons.Default.LocalShipping,
                        color = Color(0xFF0284C7), // Sky Blue
                        isSelected = selectedTab == PendingFilterTab.DELIVERIES,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedTab = if (selectedTab == PendingFilterTab.DELIVERIES) PendingFilterTab.ALL else PendingFilterTab.DELIVERIES
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Active Trips Stat Card
                    PendingMetricCard(
                        title = "Active Trips",
                        value = "${allActiveTrips.size}",
                        countLabel = "Market Trips Ongoing",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        color = Color(0xFF1E40AF), // Sapphire
                        isSelected = selectedTab == PendingFilterTab.TRIPS,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedTab = if (selectedTab == PendingFilterTab.TRIPS) PendingFilterTab.ALL else PendingFilterTab.TRIPS
                        }
                    )

                    // Loose Packing Stat Card
                    PendingMetricCard(
                        title = "Loose Packs",
                        value = "$totalLoosePcs pcs",
                        countLabel = "${allLooseEntries.size} Orders Need Case",
                        icon = Icons.AutoMirrored.Filled.FactCheck,
                        color = Color(0xFFEA580C), // Orange
                        isSelected = selectedTab == PendingFilterTab.LOOSE_PACKS,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedTab = if (selectedTab == PendingFilterTab.LOOSE_PACKS) PendingFilterTab.ALL else PendingFilterTab.LOOSE_PACKS
                        }
                    )
                }
            }

            // PILL-SHAPED CATEGORY FILTER CHIPS (CircleShape)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        PendingFilterTab.ALL to "All ($totalPendingCount)",
                        PendingFilterTab.PAYMENTS to "Payments (${allPendingPayments.size})",
                        PendingFilterTab.DELIVERIES to "Deliveries (${allPendingDeliveries.size})",
                        PendingFilterTab.TRIPS to "Trips (${allActiveTrips.size})",
                        PendingFilterTab.LOOSE_PACKS to "Loose Packs (${allLooseEntries.size})"
                    )

                    tabs.forEach { (tab, label) ->
                        val isSelected = selectedTab == tab
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) NavyPrimary else Color.White,
                            shadowElevation = if (isSelected) 2.dp else 1.dp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedTab = tab }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) GoldAccent else TextPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // ==================== LIST ITEMS ====================

            // 1. PENDING PAYMENTS SECTION
            if (selectedTab == PendingFilterTab.ALL || selectedTab == PendingFilterTab.PAYMENTS) {
                if (filteredPayments.isNotEmpty()) {
                    item {
                        PendingSectionHeader(
                            title = "Pending Payments",
                            count = filteredPayments.size,
                            icon = Icons.Default.Payments,
                            color = Color(0xFFD97706)
                        )
                    }

                    items(filteredPayments, key = { "payment_${it.id}" }) { entry ->
                        val customerName = visitMap[entry.visitId]?.customerName ?: "Customer"
                        val billTotal = entry.totalAmount + entry.gstAmount
                        val dueBalance = maxOf(0.0, billTotal - entry.paidAmount)

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Order ${entry.orderNo} • ${entry.itemCode}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = "👤 $customerName • 🏭 ${entry.supplierName}",
                                            fontSize = 11.5.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = if (entry.paymentStatus.equals("Partial", true)) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = if (entry.paymentStatus.equals("Partial", true)) "Partial" else "Pending Due",
                                            color = if (entry.paymentStatus.equals("Partial", true)) Color(0xFFB45309) else Color(0xFFDC2626),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Due Balance",
                                            fontSize = 10.5.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "₹${PdfGenerator.formatInr(dueBalance)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFDC2626)
                                        )
                                        Text(
                                            text = "Total: ₹${PdfGenerator.formatInr(billTotal)} • Paid: ₹${PdfGenerator.formatInr(entry.paidAmount)}",
                                            fontSize = 10.5.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Button(
                                        onClick = { paymentEntryToUpdate = entry },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                    ) {
                                        Text(
                                            text = "Record Payment",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. PENDING DELIVERIES SECTION
            if (selectedTab == PendingFilterTab.ALL || selectedTab == PendingFilterTab.DELIVERIES) {
                if (filteredDeliveries.isNotEmpty()) {
                    item {
                        PendingSectionHeader(
                            title = "Pending Deliveries",
                            count = filteredDeliveries.size,
                            icon = Icons.Default.LocalShipping,
                            color = Color(0xFF0284C7)
                        )
                    }

                    items(filteredDeliveries, key = { "delivery_${it.id}" }) { entry ->
                        val customerName = visitMap[entry.visitId]?.customerName ?: "Customer"

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Order ${entry.orderNo} • ${entry.itemCode}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = "🏭 ${entry.supplierName} • 👤 $customerName",
                                            fontSize = 11.5.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    DeliveryStatusBadge(status = entry.deliveryStatus)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${entry.pieces} pcs (${entry.caseCount} cases, ${entry.loosePieces} loose)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (entry.transporter.isNotBlank()) "🚚 ${entry.transporter}" else "⚠️ No transporter LR assigned",
                                            fontSize = 11.sp,
                                            color = if (entry.transporter.isNotBlank()) TextSecondary else Color(0xFFD97706)
                                        )
                                    }

                                    Button(
                                        onClick = { deliveryEntryToUpdate = entry },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                    ) {
                                        Text(
                                            text = "Update Status",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. ACTIVE MARKET TRIPS SECTION
            if (selectedTab == PendingFilterTab.ALL || selectedTab == PendingFilterTab.TRIPS) {
                if (filteredTrips.isNotEmpty()) {
                    item {
                        PendingSectionHeader(
                            title = "Active Market Trips",
                            count = filteredTrips.size,
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            color = Color(0xFF1E40AF)
                        )
                    }

                    items(filteredTrips, key = { "trip_${it.id}" }) { trip ->
                        val tripEntries = entries.filter { it.visitId == trip.id }
                        val tripPieces = tripEntries.sumOf { it.pieces }
                        val tripCases = tripEntries.sumOf { it.caseCount }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = trip.customerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = "${trip.visitCode} • ${trip.date} • Agent: ${trip.employeeName}",
                                            fontSize = 11.5.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    StatusBadge(status = trip.status)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${tripEntries.size} supplier stops • $tripPieces pcs ($tripCases cases)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = { onOpenVisit(trip) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Open Trip",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldAccent
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = GoldAccent,
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

            // 4. LOOSE PACKS SECTION
            if (selectedTab == PendingFilterTab.ALL || selectedTab == PendingFilterTab.LOOSE_PACKS) {
                if (filteredLooseEntries.isNotEmpty()) {
                    item {
                        PendingSectionHeader(
                            title = "Loose Packs (Need Mixed Case)",
                            count = filteredLooseEntries.size,
                            icon = Icons.AutoMirrored.Filled.FactCheck,
                            color = Color(0xFFEA580C)
                        )
                    }

                    items(filteredLooseEntries, key = { "loose_${it.id}" }) { entry ->
                        val customerName = visitMap[entry.visitId]?.customerName ?: "Customer"

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Order ${entry.orderNo} • ${entry.itemCode}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = "🏭 ${entry.supplierName} • 👤 $customerName",
                                            fontSize = 11.5.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFFEDD5)
                                    ) {
                                        Text(
                                            text = "${entry.loosePieces} Loose Pcs",
                                            color = Color(0xFFC2410C),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${entry.caseCount} full cases • Case size: ${entry.caseSize} pcs",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = onOpenMixedPack,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                    ) {
                                        Text(
                                            text = "Pack Loose Pcs",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // EMPTY STATE IF NO PENDING ITEMS
            val isCurrentTabEmpty = when (selectedTab) {
                PendingFilterTab.ALL -> filteredPayments.isEmpty() && filteredDeliveries.isEmpty() && filteredTrips.isEmpty() && filteredLooseEntries.isEmpty()
                PendingFilterTab.PAYMENTS -> filteredPayments.isEmpty()
                PendingFilterTab.DELIVERIES -> filteredDeliveries.isEmpty()
                PendingFilterTab.TRIPS -> filteredTrips.isEmpty()
                PendingFilterTab.LOOSE_PACKS -> filteredLooseEntries.isEmpty()
            }

            if (isCurrentTabEmpty) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matching pending items" else "All Operations Cleared!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "No pending payments, deliveries, or trips matched \"$searchQuery\"."
                                else
                                    "There are no pending items in this category. Great job keeping everything up to date!",
                                fontSize = 12.5.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // INLINE RECORD PAYMENT DIALOG
    paymentEntryToUpdate?.let { entry ->
        val v = visitMap[entry.visitId]
        val billAmount = entry.totalAmount + entry.gstAmount

        RecordPaymentDialog(
            entry = entry,
            customerName = v?.customerName ?: "Customer",
            billAmount = billAmount,
            onDismiss = { paymentEntryToUpdate = null },
            onSave = { newStatus, newMode, newPaid, newRemarks ->
                viewModel.updatePaymentInfo(
                    entry = entry,
                    paymentStatus = newStatus,
                    paymentMode = newMode,
                    paidAmount = newPaid,
                    paymentRemarks = newRemarks
                ) {
                    paymentEntryToUpdate = null
                }
            }
        )
    }

    // INLINE UPDATE DELIVERY STATUS DIALOG
    deliveryEntryToUpdate?.let { entry ->
        UpdateDeliveryStatusDialog(
            entry = entry,
            onDismiss = { deliveryEntryToUpdate = null },
            onSave = { newStatus, transporter ->
                viewModel.updateDeliveryStatus(entry, newStatus, transporter)
                deliveryEntryToUpdate = null
            }
        )
    }
}

// Top Metric Card
@Composable
private fun PendingMetricCard(
    title: String,
    value: String,
    countLabel: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.08f) else Color.White
        ),
        border = if (isSelected) BorderStroke(1.5.dp, color) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.5.dp else 1.5.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = countLabel,
                fontSize = 10.5.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Section Header Component
@Composable
private fun PendingSectionHeader(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        }

        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f)
        ) {
            Text(
                text = "$count Pending",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}
