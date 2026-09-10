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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

data class HomeTileItem(
    val id: String,
    val title: String,
    val hindiTitle: String,
    val subtitle: String,
    val badgeText: String,
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
    onSwitchRole: () -> Unit
) {
    val role by viewModel.currentRole.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val visits by viewModel.allVisits.collectAsStateWithLifecycle()
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()

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
    val looseEntries = remember(entries) { entries.filter { it.loosePieces > 0 } }
    val totalLoosePcs = remember(looseEntries) { looseEntries.sumOf { it.loosePieces } }
    val pendingDeliveries = remember(entries) { entries.count { it.deliveryStatus != "Delivered" } }
    val grandTotalAmount = remember(entries) { entries.sumOf { it.grandTotalWithGst } }

    // Option Tiles Definition - Every feature has its own distinct tile
    val allTiles = remember(
        visits.size, activeVisits.size, customers.size, suppliers.size,
        employees.size, pendingDeliveries, totalLoosePcs, role
    ) {
        listOf(
            HomeTileItem(
                id = "new_visit",
                title = "New Market Visit",
                hindiTitle = "नई विज़िट शुरू करें",
                subtitle = "Start retailer market escort & log spot purchases",
                badgeText = "Quick Action ⚡",
                icon = Icons.Default.AddCircle,
                accentColor = Color(0xFF059669), // Emerald Green
                isPrimaryAction = true,
                onClick = onOpenNewVisit
            ),
            HomeTileItem(
                id = "visits",
                title = "Market Visits",
                hindiTitle = "मार्केट विज़िट्स",
                subtitle = "Live market trips, order entries & day reports",
                badgeText = if (activeVisits.isNotEmpty()) "${activeVisits.size} Active (${visits.size} Total)" else "${visits.size} Total Trips",
                icon = Icons.Default.Assignment,
                accentColor = Color(0xFF1E3A8A), // Deep Navy
                onClick = { onNavigate(AppScreen.VISITS) }
            ),
            HomeTileItem(
                id = "customers",
                title = "Customer Master",
                hindiTitle = "ग्राहक डायरेक्टरी",
                subtitle = "Day reports, credit terms, ledger & visit history",
                badgeText = "${customers.size} Retailers",
                icon = Icons.Default.People,
                accentColor = Color(0xFF0284C7), // Sky Blue
                onClick = { onNavigate(AppScreen.CUSTOMER_MASTER) }
            ),
            HomeTileItem(
                id = "suppliers",
                title = "Supplier Master",
                hindiTitle = "थोक सप्लायर्स",
                subtitle = "Mills, wholesalers, fabric types & wholesale bills",
                badgeText = "${suppliers.size} Suppliers",
                icon = Icons.Default.Store,
                accentColor = Color(0xFF7C3AED), // Rich Purple
                onClick = { onNavigate(AppScreen.SUPPLIER_MASTER) }
            ),
            HomeTileItem(
                id = "employees",
                title = "Staff & Agents",
                hindiTitle = "सेल्समैन व एजेंट",
                subtitle = "Sahyog entries, pending bills & cleared volume",
                badgeText = "${employees.size} Agents",
                icon = Icons.Default.Badge,
                accentColor = Color(0xFFD97706), // Warm Amber
                onClick = { onNavigate(AppScreen.EMPLOYEE_MASTER) }
            ),
            HomeTileItem(
                id = "deliveries",
                title = "Deliveries & Bilty",
                hindiTitle = "डिलीवरी व बिल्टी",
                subtitle = "Track packing, dispatches, LR & transport status",
                badgeText = if (pendingDeliveries > 0) "$pendingDeliveries In Transit" else "All Cleared ✅",
                icon = Icons.Default.LocalShipping,
                accentColor = Color(0xFF0D9488), // Teal
                onClick = { onNavigate(AppScreen.DELIVERIES) }
            ),
            HomeTileItem(
                id = "mixed_pack",
                title = "Mixed Pack (Loose)",
                hindiTitle = "मिक्स पैकिंग (खुला माल)",
                subtitle = "Consolidate loose pieces into master transport cases",
                badgeText = if (totalLoosePcs > 0) "$totalLoosePcs Pcs Waiting" else "0 Loose Pcs",
                icon = Icons.Default.FactCheck,
                accentColor = Color(0xFFEA580C), // Orange / Coral
                onClick = onOpenMixedPack
            ),
            HomeTileItem(
                id = "reports",
                title = "Business Reports",
                hindiTitle = "बिजनेस रिपोर्ट्स",
                subtitle = "Consolidated Day PDF, Supplier Copies & ledger summary",
                badgeText = "PDF & Ledger 📄",
                icon = Icons.Default.Assessment,
                accentColor = Color(0xFFE11D48), // Rose Crimson
                onClick = { onNavigate(AppScreen.REPORTS) }
            ),
            HomeTileItem(
                id = "trading_hub",
                title = "Wholesale Hub",
                hindiTitle = "सप्लायर हब व सौदे",
                subtitle = "Spot order ledger, market rates & direct wholesale tracking",
                badgeText = "Market Deals 🏢",
                icon = Icons.Default.Storefront,
                accentColor = Color(0xFF4338CA), // Indigo
                onClick = { onNavigate(AppScreen.SUPPLIER_HUB) }
            ),
            HomeTileItem(
                id = "role_switch",
                title = "Switch Role",
                hindiTitle = "रोल बदलें",
                subtitle = "Toggle between Owner (Himat Bhai) & Field Salesman",
                badgeText = "Active: $role",
                icon = Icons.Default.SwapHoriz,
                accentColor = Color(0xFF475569), // Slate
                onClick = onSwitchRole
            )
        )
    }

    // Filter tiles based on search query
    val displayedTiles = remember(allTiles, searchQuery) {
        if (searchQuery.isBlank()) {
            allTiles
        } else {
            allTiles.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.hindiTitle.contains(searchQuery, ignoreCase = true) ||
                        it.subtitle.contains(searchQuery, ignoreCase = true) ||
                        it.badgeText.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP SEARCH BAR - Prominently located at the top of the Home Screen
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
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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

        LazyVerticalGrid(
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
                                imageVector = Icons.Default.Assignment,
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
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                                Icons.Default.Assignment,
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
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                            Text(
                                                text = "📍 ${supplier.marketArea} • 📞 ${supplier.phone}",
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
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
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
                // 1. Hero Branding & Active User Card
                item(span = { GridItemSpan(2) }) {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = NavyPrimary),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_hero_card")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            NavyPrimary,
                                            Color(0xFF0F172A)
                                        )
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = GoldAccent,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "HIMAT CLOTH AGENCY",
                                                    color = NavyPrimary,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp,
                                                    letterSpacing = 0.5.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (role == "Admin") "Himat Bhai (Agency Owner)" else (currentEmployee?.name ?: "Field Salesman"),
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Ahmedabad Wholesale Textile Market",
                                            color = Color.White.copy(alpha = 0.75f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    // Role Switcher Button
                                    Surface(
                                        color = Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSwitchRole() }
                                            .testTag("home_role_switch_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.SwapHoriz,
                                                contentDescription = "Switch Role",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = role,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Market Pulse Stats Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MarketPulseItem(
                                        label = "Active Trips",
                                        value = "${activeVisits.size}",
                                        highlightColor = GoldAccent
                                    )
                                    MarketPulseItem(
                                        label = "Procured Pcs",
                                        value = String.format("%,d", totalPieces),
                                        highlightColor = Color(0xFF34D399)
                                    )
                                    MarketPulseItem(
                                        label = "In Transit",
                                        value = "$pendingDeliveries",
                                        highlightColor = Color(0xFF60A5FA)
                                    )
                                    MarketPulseItem(
                                        label = "Loose Pcs",
                                        value = "$totalLoosePcs",
                                        highlightColor = Color(0xFFFBBF24)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Active Market Visit Strip (Quick Action if ongoing)
                if (activeVisits.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        val currentActive = activeVisits.first()
                        val currentVisitEntries = entries.filter { it.visitId == currentActive.id }
                        val currentPcs = currentVisitEntries.sumOf { it.pieces }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenVisit(currentActive) }
                                .testTag("home_active_visit_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF059669)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Assignment,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "🟢 Active Market Visit: ${currentActive.customerName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF065F46),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${currentVisitEntries.size} stops • $currentPcs pcs • Agent: ${currentActive.employeeName}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onOpenVisit(currentActive) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                ) {
                                    Text(
                                        text = "Resume",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Quick Navigation Section Header
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Navigation • All Options",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${allTiles.size} Tiles",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. THE OPTION TILES GRID
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
}

@Composable
fun HomeOptionTile(
    tile: HomeTileItem,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (tile.isPrimaryAction) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (tile.isPrimaryAction) Color(0xFF059669).copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (tile.isPrimaryAction) 3.dp else 1.5.dp
        ),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { tile.onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon container & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Colored Icon Container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tile.accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tile.icon,
                        contentDescription = tile.title,
                        tint = tile.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Micro Arrow indicator
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Hindi Title
            Text(
                text = tile.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = tile.hindiTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tile.accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle Description
            Text(
                text = tile.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Badge
            Surface(
                color = tile.accentColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, tile.accentColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tile.badgeText,
                    color = tile.accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MarketPulseItem(
    label: String,
    value: String,
    highlightColor: Color
) {
    Column {
        Text(
            text = value,
            color = highlightColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.5.sp
        )
    }
}
