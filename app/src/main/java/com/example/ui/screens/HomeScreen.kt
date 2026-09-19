package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import com.example.ui.dialogs.CustomerRequestsDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator

data class HomeTileItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val statusBadge: String? = null,
    val icon: ImageVector,
    val accentColor: Color,
    val isPrimaryAction: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HimatViewModel,
    onNavigate: (AppScreen) -> Unit,
    onOpenNewVisit: () -> Unit,
    onOpenVisit: (VisitEntity) -> Unit,
    onOpenSupplier: (SupplierEntity) -> Unit = { viewModel.openSupplierDetail(it) },
    onOpenMixedPack: () -> Unit,
    onSwitchRole: () -> Unit = {}
) {
    val role by viewModel.currentRole.collectAsStateWithLifecycle()
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val visits by viewModel.allVisits.collectAsStateWithLifecycle()
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val packGroups by viewModel.allPackGroups.collectAsStateWithLifecycle()
    val allLeads by viewModel.allLeads.collectAsStateWithLifecycle()
    val pendingRequestsCount by viewModel.pendingRegistrationRequestsCount.collectAsStateWithLifecycle()

    var showCustomerRequestsDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchCategory by remember { mutableStateOf("All") } // "All", "Trips", "Suppliers"

    val cleanQuery = searchQuery.trim()

    // Search query matching for trips
    val matchingVisits = remember(visits, cleanQuery) {
        if (cleanQuery.isBlank()) emptyList()
        else {
            visits.filter {
                it.customerName.contains(cleanQuery, ignoreCase = true) ||
                        it.visitCode.contains(cleanQuery, ignoreCase = true) ||
                        it.employeeName.contains(cleanQuery, ignoreCase = true) ||
                        it.date.contains(cleanQuery, ignoreCase = true) ||
                        it.status.contains(cleanQuery, ignoreCase = true) ||
                        it.notes.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    // Search query matching for suppliers
    val matchingSuppliers = remember(suppliers, cleanQuery) {
        if (cleanQuery.isBlank()) emptyList()
        else {
            suppliers.filter {
                it.name.contains(cleanQuery, ignoreCase = true) ||
                        it.marketArea.contains(cleanQuery, ignoreCase = true) ||
                        it.type.contains(cleanQuery, ignoreCase = true) ||
                        it.brand.contains(cleanQuery, ignoreCase = true) ||
                        it.city.contains(cleanQuery, ignoreCase = true) ||
                        it.contactPerson.contains(cleanQuery, ignoreCase = true) ||
                        it.phone.contains(cleanQuery, ignoreCase = true) ||
                        it.categories.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    // Entry stats for trips
    val visitStats = remember(entries) {
        entries.groupBy { it.visitId }.mapValues { (_, visitEntries) ->
            Pair(visitEntries.size, visitEntries.sumOf { it.pieces })
        }
    }

    // Live Metrics Calculations
    val activeVisits = remember(visits) { visits.filter { it.status == "Active" } }
    val totalPieces = remember(entries) { entries.sumOf { it.pieces } }
    val totalCases = remember(entries) { entries.sumOf { it.caseCount } }
    val packedEntryIds = remember(packGroups) {
        packGroups.flatMap { group ->
            group.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() }
        }.toSet()
    }
    val looseEntries = remember(entries, packedEntryIds) {
        entries.filter { entry ->
            entry.loosePieces > 0 &&
                    (entry.packGroupId == null || entry.packGroupId == 0L) &&
                    entry.mixedPackNote.isNullOrBlank() &&
                    entry.id !in packedEntryIds
        }
    }
    val totalLoosePcs = remember(looseEntries) { looseEntries.sumOf { it.loosePieces } }
    val pendingDeliveries = remember(entries) { entries.count { it.deliveryStatus != "Delivered" } }
    val grandTotalAmount = remember(entries) { entries.sumOf { it.grandTotalWithGst } }
    val totalPendingDues = remember(entries) {
        entries.filter { !it.paymentStatus.equals("Paid", ignoreCase = true) && !it.paymentStatus.equals("Received", ignoreCase = true) }
            .sumOf { maxOf(0.0, (it.totalAmount + it.gstAmount) - it.paidAmount) }
    }
    val pendingPaymentsCount = remember(entries) {
        entries.count { !it.paymentStatus.equals("Paid", ignoreCase = true) && !it.paymentStatus.equals("Received", ignoreCase = true) }
    }
    val totalPendingTasks = remember(pendingPaymentsCount, pendingDeliveries, activeVisits.size, looseEntries.size) {
        pendingPaymentsCount + pendingDeliveries + activeVisits.size + looseEntries.size
    }

    // Option Tiles Definition - Every feature has its own distinct tile
    val allTiles = remember(
        visits.size, activeVisits.size, customers.size, suppliers.size,
        employees.size, pendingDeliveries, totalLoosePcs, totalPendingDues, totalPendingTasks, isSuperAdmin
    ) {
        buildList {
            add(
                HomeTileItem(
                    id = "new_visit",
                    title = "New Visit",
                    subtitle = "Start market trip",
                    icon = Icons.Default.AddCircle,
                    accentColor = Color(0xFF059669), // Emerald Green
                    isPrimaryAction = true,
                    onClick = onOpenNewVisit
                )
            )
            add(
                HomeTileItem(
                    id = "pendings",
                    title = "Pending Hub",
                    subtitle = if (totalPendingTasks > 0) "$totalPendingTasks tasks need action" else "All operations cleared",
                    statusBadge = if (totalPendingTasks > 0) "$totalPendingTasks Pending" else "All Clear ✓",
                    icon = Icons.Default.PendingActions,
                    accentColor = Color(0xFFDC2626), // Crimson Red
                    onClick = { onNavigate(AppScreen.PENDINGS) }
                )
            )
            add(
                HomeTileItem(
                    id = "dashboard",
                    title = "Dashboard",
                    subtitle = "Operations & analytics",
                    icon = Icons.Default.Dashboard,
                    accentColor = Color(0xFF4F46E5), // Royal Indigo
                    onClick = { onNavigate(AppScreen.ANALYTICS_DASHBOARD) }
                )
            )
            add(
                HomeTileItem(
                    id = "visits",
                    title = "Market Trips",
                    subtitle = if (activeVisits.isNotEmpty()) "${activeVisits.size} active ongoing" else "${visits.size} total trips",
                    statusBadge = if (activeVisits.isNotEmpty()) "${activeVisits.size} Active" else null,
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    accentColor = Color(0xFF1E40AF), // Deep Sapphire
                    onClick = { onNavigate(AppScreen.VISITS) }
                )
            )
            add(
                HomeTileItem(
                    id = "customers",
                    title = "Customers",
                    subtitle = "${customers.size} registered retailers",
                    icon = Icons.Default.People,
                    accentColor = Color(0xFF0284C7), // Sky Blue
                    onClick = { onNavigate(AppScreen.CUSTOMER_MASTER) }
                )
            )
            add(
                HomeTileItem(
                    id = "suppliers",
                    title = "Suppliers",
                    subtitle = "${suppliers.size} wholesale suppliers",
                    icon = Icons.Default.Store,
                    accentColor = Color(0xFF7C3AED), // Rich Purple
                    onClick = { onNavigate(AppScreen.SUPPLIER_MASTER) }
                )
            )
            add(
                HomeTileItem(
                    id = "leads",
                    title = "Leads & Prospects",
                    subtitle = "${allLeads.size} market contacts",
                    statusBadge = if (allLeads.isNotEmpty()) "${allLeads.size} Leads" else null,
                    icon = Icons.Default.People,
                    accentColor = Color(0xFF0D9488), // Teal
                    onClick = { onNavigate(AppScreen.LEADS) }
                )
            )
            if (isSuperAdmin) {
                add(
                    HomeTileItem(
                        id = "customer_requests",
                        title = "User Requests",
                        subtitle = if (pendingRequestsCount > 0) "$pendingRequestsCount pending verification" else "All verified",
                        statusBadge = if (pendingRequestsCount > 0) "$pendingRequestsCount Pending" else null,
                        icon = Icons.Default.PersonAdd,
                        accentColor = Color(0xFFD97706), // Amber
                        onClick = { showCustomerRequestsDialog = true }
                    )
                )
            }
            if (isSuperAdmin) {
                add(
                    HomeTileItem(
                        id = "employees",
                        title = "Staff & Agents",
                        subtitle = "${employees.size} active agents",
                        icon = Icons.Default.Badge,
                        accentColor = Color(0xFFD97706), // Warm Amber
                        onClick = { onNavigate(AppScreen.EMPLOYEE_MASTER) }
                    )
                )
            }
            add(
                HomeTileItem(
                    id = "deliveries",
                    title = "Deliveries",
                    subtitle = if (pendingDeliveries > 0) "$pendingDeliveries in transit" else "All orders cleared",
                    statusBadge = if (pendingDeliveries > 0) "$pendingDeliveries Transit" else null,
                    icon = Icons.Default.LocalShipping,
                    accentColor = Color(0xFF0D9488), // Teal
                    onClick = { onNavigate(AppScreen.DELIVERIES) }
                )
            )
            add(
                HomeTileItem(
                    id = "payments",
                    title = "Payments & Bills",
                    subtitle = if (totalPendingDues > 0) "₹${PdfGenerator.formatInr(totalPendingDues)} balance due" else "All bills cleared",
                    statusBadge = if (totalPendingDues > 0) "₹${PdfGenerator.formatInr(totalPendingDues)}" else "Cleared ✓",
                    icon = Icons.Default.Payments,
                    accentColor = Color(0xFF059669), // Emerald Green
                    onClick = { onNavigate(AppScreen.PAYMENTS) }
                )
            )
            add(
                HomeTileItem(
                    id = "mixed_pack",
                    title = "Loose Packing",
                    subtitle = if (totalLoosePcs > 0) "$totalLoosePcs loose pieces" else "All cases packed",
                    statusBadge = if (totalLoosePcs > 0) "$totalLoosePcs Pcs" else null,
                    icon = Icons.AutoMirrored.Filled.FactCheck,
                    accentColor = Color(0xFFEA580C), // Orange / Coral
                    onClick = onOpenMixedPack
                )
            )
            add(
                HomeTileItem(
                    id = "reports",
                    title = "Reports",
                    subtitle = "Day summary & billing",
                    icon = Icons.Default.Assessment,
                    accentColor = Color(0xFFE11D48), // Rose Crimson
                    onClick = { onNavigate(AppScreen.REPORTS) }
                )
            )
            add(
                HomeTileItem(
                    id = "trading_hub",
                    title = "Wholesale Hub",
                    subtitle = "Direct supplier deals",
                    icon = Icons.Default.Storefront,
                    accentColor = Color(0xFF334155), // Dark Slate
                    onClick = { onNavigate(AppScreen.SUPPLIER_HUB) }
                )
            )
        }
    }

    // Filter tiles based on search query
    val displayedTiles = remember(allTiles, searchQuery) {
        if (searchQuery.isBlank()) {
            allTiles
        } else {
            allTiles.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.subtitle.contains(searchQuery, ignoreCase = true) ||
                        (it.statusBadge != null && it.statusBadge.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    val gridState = rememberLazyGridState()
    var isSearchBarVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -10f) {
                    isSearchBarVisible = false
                } else if (delta > 10f) {
                    isSearchBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        if (gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0) {
            isSearchBarVisible = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScrollConnection)
    ) {
        // TOP SEARCH BAR - Prominently located at the top of the Home Screen (animates hide on scroll)
        AnimatedVisibility(
            visible = isSearchBarVisible || searchQuery.isNotBlank(),
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = tween(180))
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_top_search_container")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search trips or suppliers by name...",
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = NavyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 46.dp)
                            .testTag("home_top_search_bar")
                    )

                    if (searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val totalFound = matchingVisits.size + matchingSuppliers.size
                            FilterChip(
                                selected = searchCategory == "All",
                                onClick = { searchCategory = "All" },
                                label = { Text("All ($totalFound)", fontSize = 11.5.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.heightIn(min = 28.dp, max = 32.dp)
                            )
                            FilterChip(
                                selected = searchCategory == "Trips",
                                onClick = { searchCategory = "Trips" },
                                label = { Text("Trips (${matchingVisits.size})", fontSize = 11.5.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.heightIn(min = 28.dp, max = 32.dp)
                            )
                            FilterChip(
                                selected = searchCategory == "Suppliers",
                                onClick = { searchCategory = "Suppliers" },
                                label = { Text("Suppliers (${matchingSuppliers.size})", fontSize = 11.5.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.heightIn(min = 28.dp, max = 32.dp)
                            )
                        }
                    }
                }
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (searchQuery.isNotBlank()) {
                // ==================== SEARCH MODE ====================
                // 1. Trips matching search query
                if ((searchCategory == "All" || searchCategory == "Trips") && matchingVisits.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "Market Trips (${matchingVisits.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    items(matchingVisits, key = { "trip_${it.id}" }, span = { GridItemSpan(2) }) { visit ->
                        val tripEntries = remember(entries, visit.id) { entries.filter { it.visitId == visit.id } }
                        val tripPieces = remember(tripEntries) { tripEntries.sumOf { it.pieces } }
                        val tripCases = remember(tripEntries) { tripEntries.sumOf { it.caseCount } }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenVisit(visit) }
                                .testTag("search_trip_${visit.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(NavyPrimary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Assignment,
                                                contentDescription = null,
                                                tint = NavyPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = visit.customerName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${visit.visitCode} • ${visit.date}",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    StatusBadge(status = visit.status)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${tripEntries.size} stops • $tripPieces pcs ($tripCases cases) • Agent: ${visit.employeeName}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "Open Trip",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = NavyPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Suppliers matching search query
                if ((searchCategory == "All" || searchCategory == "Suppliers") && matchingSuppliers.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "Suppliers & Mills (${matchingSuppliers.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    items(matchingSuppliers, key = { "supplier_${it.id}" }, span = { GridItemSpan(2) }) { supplier ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenSupplier(supplier) }
                                .testTag("search_supplier_${supplier.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF7C3AED).copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Store,
                                                contentDescription = null,
                                                tint = Color(0xFF7C3AED),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = supplier.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val locationContact = if (supplier.phone.isNotBlank()) {
                                                "${supplier.marketArea} • ${supplier.phone}"
                                            } else {
                                                supplier.marketArea
                                            }
                                            Text(
                                                text = locationContact,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    SupplierTypeBadge(type = supplier.type)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (supplier.categories.isNotBlank()) "Fabrics: ${supplier.categories}" else "Wholesale Textile Supplier",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "View Supplier",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7C3AED)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Matching Navigation Options
                if (searchCategory == "All" && displayedTiles.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "Matching Navigation Options (${displayedTiles.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                    }
                    items(displayedTiles, key = { "search_tile_${it.id}" }) { tile ->
                        HomeOptionTile(
                            tile = tile,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tile_${tile.id}")
                        )
                    }
                }

                // 4. Empty state if nothing matched
                if (matchingVisits.isEmpty() && matchingSuppliers.isEmpty() && (searchCategory != "All" || displayedTiles.isEmpty())) {
                    item(span = { GridItemSpan(2) }) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = "No trips or suppliers found",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "No results found for \"$searchQuery\". Try searching by customer name, trip code, or supplier name.",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { searchQuery = "" },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Clear Search")
                                }
                            }
                        }
                    }
                }
            } else {
                // ==================== DEFAULT HOME VIEW ====================
                // 1. Active Market Visit Strip (Quick Action if ongoing)
                if (activeVisits.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        val currentActive = activeVisits.first()
                        val currentVisitEntries = entries.filter { it.visitId == currentActive.id }
                        val currentPcs = currentVisitEntries.sumOf { it.pieces }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenVisit(currentActive) }
                                .testTag("home_active_visit_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF059669)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Assignment,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Active Trip: ${currentActive.customerName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF065F46),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${currentVisitEntries.size} stops • $currentPcs pcs • Agent: ${currentActive.employeeName}",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF047857),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onOpenVisit(currentActive) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                ) {
                                    Text(
                                        text = "Resume",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Market Modules Header
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Operations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${allTiles.size} Actions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 3. THE OPTION TILES GRID
                items(allTiles, key = { it.id }) { tile ->
                    HomeOptionTile(
                        tile = tile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tile_${tile.id}")
                    )
                }
            }
        }
    }

    if (showCustomerRequestsDialog) {
        CustomerRequestsDialog(
            viewModel = viewModel,
            onDismiss = { showCustomerRequestsDialog = false }
        )
    }
}

@Composable
fun HomeOptionTile(
    tile: HomeTileItem,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (tile.isPrimaryAction) 3.dp else 1.5.dp
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { tile.onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon Container & Status pill / Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (tile.isPrimaryAction) Color(0xFF059669)
                            else tile.accentColor.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tile.icon,
                        contentDescription = tile.title,
                        tint = if (tile.isPrimaryAction) Color.White else tile.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (tile.statusBadge != null) {
                    Surface(
                        color = tile.accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tile.statusBadge,
                            color = tile.accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tile.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
