package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.viewmodel.HimatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailScreen(
    viewModel: HimatViewModel,
    supplier: SupplierEntity,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("All") } // "All", "Pending", "Delivered"

    val visitMap = remember(allVisits) {
        allVisits.associateBy { it.id }
    }

    // All purchase entries belonging to this supplier
    val supplierEntries = remember(allEntries, supplier.id, supplier.name) {
        allEntries.filter {
            it.supplierId == supplier.id || it.supplierName.equals(supplier.name, ignoreCase = true)
        }.sortedByDescending { it.id }
    }

    val totalOrders = supplierEntries.size
    val totalPieces = supplierEntries.sumOf { it.pieces }
    val totalBaseAmount = supplierEntries.sumOf { it.totalAmount }
    val totalGst = supplierEntries.sumOf { it.gstAmount }
    val grandTotal = supplierEntries.sumOf { it.grandTotalWithGst }
    val pendingOrders = supplierEntries.count { it.deliveryStatus != "Delivered" }
    val pendingAmount = supplierEntries.filter { it.deliveryStatus != "Delivered" }.sumOf { it.grandTotalWithGst }
    val deliveredOrders = supplierEntries.count { it.deliveryStatus == "Delivered" }
    val deliveredAmount = supplierEntries.filter { it.deliveryStatus == "Delivered" }.sumOf { it.grandTotalWithGst }

    // Filtered entries
    val filteredEntries = remember(supplierEntries, searchQuery, filterStatus, visitMap) {
        supplierEntries.filter { entry ->
            val visit = visitMap[entry.visitId]
            val customerName = visit?.customerName ?: ""
            val matchesSearch = searchQuery.isBlank() ||
                    entry.orderNo.contains(searchQuery, ignoreCase = true) ||
                    entry.itemCode.contains(searchQuery, ignoreCase = true) ||
                    entry.transporter.contains(searchQuery, ignoreCase = true) ||
                    customerName.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (filterStatus) {
                "Pending" -> entry.deliveryStatus != "Delivered"
                "Delivered" -> entry.deliveryStatus == "Delivered"
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
                            text = supplier.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Supplier Reports, Bills & All Entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (supplier.phone.isNotBlank()) {
                        FilledTonalIconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call Supplier",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (supplier.email.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        FilledTonalIconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${supplier.email}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email Supplier",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
            // Supplier Profile Card
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
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = supplier.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        SupplierTypeBadge(type = supplier.type)
                                    }
                                    if (supplier.brand.isNotBlank()) {
                                        Text(
                                            text = "Brand: ${supplier.brand}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Contact & Address Details
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${supplier.marketArea}${if (supplier.city.isNotBlank()) ", ${supplier.city}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (supplier.contactPerson.isNotBlank()) {
                                Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${supplier.contactPerson} (${supplier.phone})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Categories Tag Chips
                        if (supplier.categories.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                supplier.categoryList.forEach { cat ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "🏷️ $cat",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Metric Statistics Cards (2x2 Grid)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CustomerSummaryChip(
                                title = "Total Orders",
                                value = "$totalOrders Bills",
                                subtitle = "${String.format("%,d", totalPieces)} pcs volume",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            CustomerSummaryChip(
                                title = "Total Bill Amount",
                                value = "₹${String.format("%,.0f", grandTotal)}",
                                subtitle = "₹${String.format("%,.0f", totalGst)} GST",
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
                                title = "⏳ Pending Deliveries",
                                value = "$pendingOrders Bills",
                                subtitle = "₹${String.format("%,.0f", pendingAmount)} pending",
                                color = if (pendingOrders > 0) MaterialTheme.colorScheme.secondary else Color(0xFF059669),
                                modifier = Modifier.weight(1f)
                            )
                            CustomerSummaryChip(
                                title = "✅ Cleared / Delivered",
                                value = "$deliveredOrders Bills",
                                subtitle = "₹${String.format("%,.0f", deliveredAmount)} fulfilled",
                                color = Color(0xFF059669),
                                modifier = Modifier.weight(1f)
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
                        placeholder = { Text("Search by Order #, Item Code, Customer...") },
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
                            "All" to "All Bills ($totalOrders)",
                            "Pending" to "⏳ Ongoing ($pendingOrders)",
                            "Delivered" to "✅ Delivered ($deliveredOrders)"
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
                        text = "Supplier Bills & Entries (${filteredEntries.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (filteredEntries.isEmpty()) {
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
                                text = if (supplierEntries.isEmpty()) "No orders recorded with this supplier" else "No bills matching filter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Purchase entries recorded in Customer visits will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredEntries, key = { it.id }) { entry ->
                    val visit = visitMap[entry.visitId]
                    SupplierBillCard(
                        entry = entry,
                        visit = visit,
                        onOpenSupplierCopy = {
                            if (visit != null) {
                                viewModel.openSupplierCopy(visit, supplier)
                            }
                        },
                        onAdvanceStatus = { viewModel.advanceEntryDeliveryStatus(entry) }
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierBillCard(
    entry: PurchaseEntryEntity,
    visit: VisitEntity?,
    onOpenSupplierCopy: () -> Unit,
    onAdvanceStatus: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Order No & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entry.orderNo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (visit != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "📅 ${visit.date}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                DeliveryStatusBadge(status = entry.deliveryStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Info & Item Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Customer: ${visit?.customerName ?: "Direct Order"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Item Code: ${entry.itemCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%,.0f", entry.grandTotalWithGst)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${entry.pieces} pcs @ ₹${entry.rate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Case Packaging Breakdown & Transporter
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val packDesc = if (entry.caseCount > 0 && entry.loosePieces > 0) {
                        "${entry.caseCount} Cases (${entry.caseCount * entry.caseSize} pcs) + ${entry.loosePieces} Loose"
                    } else if (entry.caseCount > 0) {
                        "${entry.caseCount} Cases (${entry.pieces} pcs)"
                    } else {
                        "${entry.loosePieces} Loose pcs"
                    }
                    Text(
                        text = "📦 $packDesc",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (entry.transporter.isNotBlank()) {
                        Text(
                            text = "🚛 ${entry.transporter}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!entry.mixedPackNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🏷️ Pack Group Note: ${entry.mixedPackNote}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Buttons Row: View Supplier Voucher Copy & Quick Status
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (visit != null) {
                    Button(
                        onClick = onOpenSupplierCopy,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 34.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Supplier Voucher",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (entry.deliveryStatus != "Delivered") {
                    OutlinedButton(
                        onClick = onAdvanceStatus,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
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
}
