package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.viewmodel.HimatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: HimatViewModel,
    customer: CustomerEntity,
    onBack: () -> Unit,
    onCreateVisit: () -> Unit
) {
    val context = LocalContext.current
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("All") } // "All", "Pending", "Delivered"

    // All visits for this specific customer
    val customerVisits = remember(allVisits, customer.id) {
        allVisits.filter { it.customerId == customer.id }
            .sortedByDescending { it.date }
    }
    val customerVisitIds = remember(customerVisits) {
        customerVisits.map { it.id }.toSet()
    }

    // All entries for this customer across all visits
    val customerEntries = remember(allEntries, customerVisitIds) {
        allEntries.filter { it.visitId in customerVisitIds }
    }

    val totalVisits = customerVisits.size
    val totalEntriesCount = customerEntries.size
    val totalPieces = customerEntries.sumOf { it.pieces }
    val totalAmount = customerEntries.sumOf { it.totalAmount }
    val totalGst = customerEntries.sumOf { it.gstAmount }
    val grandTotal = customerEntries.sumOf { it.grandTotalWithGst }
    val pendingEntriesCount = customerEntries.count { it.deliveryStatus != "Delivered" }
    val deliveredEntriesCount = customerEntries.count { it.deliveryStatus == "Delivered" }

    // Filtered visits based on search and status
    val filteredVisits = remember(customerVisits, customerEntries, searchQuery, filterStatus) {
        customerVisits.filter { visit ->
            val visitEntries = customerEntries.filter { it.visitId == visit.id }
            val matchesSearch = searchQuery.isBlank() ||
                    visit.date.contains(searchQuery, ignoreCase = true) ||
                    visit.visitCode.contains(searchQuery, ignoreCase = true) ||
                    visit.employeeName.contains(searchQuery, ignoreCase = true) ||
                    visitEntries.any {
                        it.itemCode.contains(searchQuery, ignoreCase = true) ||
                                it.supplierName.contains(searchQuery, ignoreCase = true) ||
                                it.transporter.contains(searchQuery, ignoreCase = true)
                    }

            val matchesStatus = when (filterStatus) {
                "Pending" -> visitEntries.any { it.deliveryStatus != "Delivered" }
                "Delivered" -> visitEntries.isNotEmpty() && visitEntries.all { it.deliveryStatus == "Delivered" }
                else -> true
            }

            matchesSearch && matchesStatus
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Customer Entries & Day Reports (${customer.customerId})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (customer.phone.isNotBlank()) {
                        FilledTonalIconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call Customer",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Header Profile Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${customer.city}${if (customer.address.isNotBlank()) " • ${customer.address}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (customer.gstin.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "GSTIN: ${customer.gstin}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Credit: ${customer.creditDays} Days",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Metric Statistics Cards (2x2 Grid)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CustomerSummaryChip(
                                title = "Total Visits",
                                value = "$totalVisits Days",
                                subtitle = "$totalEntriesCount Total Orders",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            CustomerSummaryChip(
                                title = "Total Volume",
                                value = "${String.format("%,d", totalPieces)} pcs",
                                subtitle = "₹${String.format("%,.0f", grandTotal)} billed",
                                color = Color(0xFF059669),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CustomerSummaryChip(
                                title = "Ongoing Deliveries",
                                value = "$pendingEntriesCount Pending",
                                subtitle = "$deliveredEntriesCount Delivered",
                                color = if (pendingEntriesCount > 0) MaterialTheme.colorScheme.secondary else Color(0xFF059669),
                                modifier = Modifier.weight(1f)
                            )
                            CustomerSummaryChip(
                                title = "GST Total",
                                value = "₹${String.format("%,.0f", totalGst)}",
                                subtitle = "₹${String.format("%,.0f", totalAmount)} base",
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onCreateVisit,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp)
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+ Start New Visit for ${customer.name}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Search and Status Filters
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by date, item code, supplier, transporter...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "All" to "All Days ($totalVisits)",
                            "Pending" to "⏳ Pending Deliveries ($pendingEntriesCount)",
                            "Delivered" to "✅ Fully Delivered ($deliveredEntriesCount)"
                        ).forEach { (statusKey, label) ->
                            val isSelected = filterStatus == statusKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { filterStatus = statusKey },
                                shape = CircleShape,
                                modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Day-by-Day Entries & Reports (${filteredVisits.size} Days)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (filteredVisits.isEmpty()) {
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (customerVisits.isEmpty()) "No visits or orders recorded yet" else "No entries matching filter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (customerVisits.isEmpty()) "Create a visit to start recording purchase entries for this customer." else "Try adjusting your search query or filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredVisits, key = { it.id }) { visit ->
                    val visitEntries = customerEntries.filter { it.visitId == visit.id }
                    DayVisitCard(
                        visit = visit,
                        entries = visitEntries,
                        onOpenDayReport = { viewModel.openCustomerReport(visit) },
                        onShareWhatsApp = { viewModel.shareCustomerReportWhatsApp(visit) },
                        onSharePdf = { viewModel.shareCustomerDayReportPdf(visit) },
                        onAdvanceStatus = { viewModel.advanceEntryDeliveryStatus(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun DayVisitCard(
    visit: VisitEntity,
    entries: List<PurchaseEntryEntity>,
    onOpenDayReport: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onSharePdf: () -> Unit,
    onAdvanceStatus: (PurchaseEntryEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val dayPieces = entries.sumOf { it.pieces }
    val dayAmount = entries.sumOf { it.grandTotalWithGst }
    val pendingCount = entries.count { it.deliveryStatus != "Delivered" }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Day Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📅 ${visit.date}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = visit.visitCode,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Assisted by Agent: ${visit.employeeName} • ${entries.size} Items • ${String.format("%,d", dayPieces)} pcs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            // Summary Totals & Day Report Action Buttons
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Day Total: ₹${String.format("%,.0f", dayAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (pendingCount > 0) "⏳ $pendingCount items ongoing" else "✅ All items fulfilled",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pendingCount > 0) MaterialTheme.colorScheme.secondary else Color(0xFF059669),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day Report full screen preview
                    Button(
                        onClick = onOpenDayReport,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Day Report", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    // Share WhatsApp
                    FilledTonalIconButton(
                        onClick = onShareWhatsApp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Day Report via WhatsApp",
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (visit.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: ${visit.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Expandable List of Entries for this Day
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    if (entries.isEmpty()) {
                        Text(
                            text = "No purchase entries recorded for this day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        entries.forEachIndexed { index, entry ->
                            CustomerEntryRow(
                                entry = entry,
                                onAdvanceStatus = { onAdvanceStatus(entry) }
                            )
                            if (index < entries.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerEntryRow(
    entry: PurchaseEntryEntity,
    onAdvanceStatus: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.itemCode,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${entry.orderNo})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DeliveryStatusBadge(status = entry.deliveryStatus)
        }

        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.supplierName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                SupplierTypeBadge(type = entry.supplierType)
            }

            Text(
                text = "${entry.pieces} pcs @ ₹${entry.rate} = ₹${String.format("%,.0f", entry.totalAmount)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Packaging & Case Details
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val packText = if (entry.caseCount > 0 && entry.loosePieces > 0) {
                "${entry.caseCount} Cases (${entry.caseCount * entry.caseSize} pcs) + ${entry.loosePieces} Loose"
            } else if (entry.caseCount > 0) {
                "${entry.caseCount} Cases (${entry.pieces} pcs)"
            } else {
                "${entry.loosePieces} Loose pcs"
            }
            Text(
                text = "📦 $packText (Size: ${entry.caseSize})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (entry.transporter.isNotBlank()) {
                Text(
                    text = "🚛 ${entry.transporter}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!entry.mixedPackNote.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "🏷️ ${entry.mixedPackNote}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Medium
            )
        }

        // Quick status advance button
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (entry.deliveryStatus != "Delivered") {
                OutlinedButton(
                    onClick = onAdvanceStatus,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 28.dp)
                ) {
                    val nextLabel = when (entry.deliveryStatus.lowercase()) {
                        "pending" -> "Mark Packed 📦"
                        "packed" -> "Mark Dispatched 🚚"
                        "dispatched" -> "Mark Delivered ✅"
                        else -> "Advance"
                    }
                    Text(nextLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun CustomerSummaryChip(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
