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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import com.example.ui.dialogs.CustomerRequestsDialog
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
import com.example.ui.components.ListRow
import com.example.ui.components.StatusPill
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
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val pendingRequestsCount by viewModel.pendingRegistrationRequestsCount.collectAsStateWithLifecycle()
    var showRequestsDialog by remember { mutableStateOf(false) }

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

    val totalPendingCount = allPendingPayments.size + allPendingDeliveries.size + allActiveTrips.size

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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP HEADER BAR
        Surface(
            color = MaterialTheme.colorScheme.surface,
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
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (totalPendingCount > 0) "$totalPendingCount pending items requiring action" else "All tasks are cleared ✓",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Search Toggle Icon Button in top-right header corner
                    Surface(
                        shape = CircleShape,
                        color = if (isSearchVisible || searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
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
            // Admin Pending Customer Registration Requests Banner
            if (isSuperAdmin && pendingRequestsCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRequestsDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = Color(0xFFD97706),
                                    shape = CircleShape,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "$pendingRequestsCount Customer Requests",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "New retail registrations waiting for admin approval",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                            Button(
                                onClick = { showRequestsDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Review", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

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
                                fontWeight = FontWeight.SemiBold,
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
                        val isPartial = entry.paymentStatus.equals("Partial", true)

                        ListRow(
                            horizontalPadding = 0.dp,
                            title = "${entry.orderNo.ifBlank { "Order" }} • ${entry.itemCode}",
                            value = "₹${PdfGenerator.formatInr(dueBalance)} due",
                            valueColor = Color(0xFFDC2626),
                            detail = "$customerName • ${entry.supplierName}",
                            note = "Bill ₹${PdfGenerator.formatInr(billTotal)} • Paid ₹${PdfGenerator.formatInr(entry.paidAmount)}",
                            status = {
                                StatusPill(
                                    text = if (isPartial) "Partial" else "Pending",
                                    background = if (isPartial) Color(0xFFFEF3C7) else Color(0xFFFEE2E2),
                                    foreground = if (isPartial) Color(0xFFB45309) else Color(0xFFDC2626)
                                )
                            },
                            onClick = { paymentEntryToUpdate = entry },
                            trailing = {
                                IconButton(
                                    onClick = { paymentEntryToUpdate = entry },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = "Record payment for ${entry.orderNo}",
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
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
                        val packing = "${entry.pieces} pcs • ${entry.caseCount} cs" +
                            if (entry.loosePieces > 0) " • ${entry.loosePieces} loose" else ""

                        ListRow(
                            horizontalPadding = 0.dp,
                            title = "${entry.orderNo.ifBlank { "Order" }} • ${entry.itemCode}",
                            detail = "${entry.supplierName} • $customerName",
                            note = "$packing • ${entry.transporter.ifBlank { "No transporter yet" }}",
                            status = { DeliveryStatusBadge(status = entry.deliveryStatus) },
                            onClick = { deliveryEntryToUpdate = entry },
                            trailing = {
                                IconButton(
                                    onClick = { deliveryEntryToUpdate = entry },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "Update delivery status for ${entry.orderNo}",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
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

                        ListRow(
                            horizontalPadding = 0.dp,
                            title = trip.customerName.ifBlank { "Customer" },
                            detail = "${trip.visitCode} • ${trip.date} • ${trip.employeeName}",
                            note = "${tripEntries.size} supplier stops • $tripPieces pcs ($tripCases cases)",
                            status = { StatusBadge(status = trip.status) },
                            onClick = { onOpenVisit(trip) },
                            trailing = {
                                IconButton(onClick = { onOpenVisit(trip) }, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Open trip ${trip.visitCode}",
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
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

                        ListRow(
                            horizontalPadding = 0.dp,
                            title = "${entry.orderNo.ifBlank { "Order" }} • ${entry.itemCode}",
                            detail = "${entry.supplierName} • $customerName",
                            note = "${entry.caseCount} full cases • ${entry.loosePieces} loose pcs",
                            status = {
                                StatusPill(
                                    text = "${entry.loosePieces} loose",
                                    background = Color(0xFFFFEDD5),
                                    foreground = Color(0xFFC2410C)
                                )
                            },
                            onClick = onOpenMixedPack,
                            trailing = {
                                IconButton(onClick = onOpenMixedPack, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                        contentDescription = "Pack loose pieces of ${entry.orderNo}",
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
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

    if (showRequestsDialog) {
        CustomerRequestsDialog(
            viewModel = viewModel,
            onDismiss = { showRequestsDialog = false }
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
