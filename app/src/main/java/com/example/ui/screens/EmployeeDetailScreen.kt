package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.viewmodel.HimatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    viewModel: HimatViewModel,
    employee: EmployeeEntity,
    onBack: () -> Unit,
    onOpenVisit: (VisitEntity) -> Unit
) {
    val context = LocalContext.current
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf("ALL") } // "ALL", "PENDING", "CLEARED", "VISITS"

    val listState = rememberLazyListState()
    var isProfileExpanded by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            private var accumulatedDelta = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    if (delta < -15f) {
                        if (accumulatedDelta > 0) accumulatedDelta = 0f
                        accumulatedDelta += delta
                        if (accumulatedDelta < -25f) isProfileExpanded = false
                    } else if (delta > 15f) {
                        if (accumulatedDelta < 0) accumulatedDelta = 0f
                        accumulatedDelta += delta
                        if (accumulatedDelta > 20f) isProfileExpanded = true
                    }
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, searchQuery) {
        if (searchQuery.isNotBlank()) {
            // Keep pinned
        } else if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 10) {
            isProfileExpanded = true
        }
    }

    // All visits handled or assisted by this employee/agent
    val employeeVisits = remember(allVisits, employee.id, employee.name) {
        allVisits.filter {
            it.employeeId == employee.id || it.employeeName.equals(employee.name, ignoreCase = true)
        }.sortedByDescending { it.date }
    }

    val employeeVisitMap = remember(employeeVisits) {
        employeeVisits.associateBy { it.id }
    }
    val employeeVisitIds = remember(employeeVisits) {
        employeeVisits.map { it.id }.toSet()
    }

    // All purchase entries where this employee assisted or contributed
    val employeeEntries = remember(allEntries, employeeVisitIds) {
        allEntries.filter { it.visitId in employeeVisitIds }
            .sortedByDescending { it.id }
    }

    // Key metrics requested by the user:
    // "unhone kin kin entries me apna assisted diya hai , unke banaye gaye kinte bill pending hai kitne clear hai"
    val totalEntriesCount = employeeEntries.size
    val totalVisitsCount = employeeVisits.size
    val totalPieces = employeeEntries.sumOf { it.pieces }
    val totalAmount = employeeEntries.sumOf { it.grandTotalWithGst }

    // Pending vs Cleared Bills breakdown
    val pendingEntries = remember(employeeEntries) {
        employeeEntries.filter { it.deliveryStatus != "Delivered" }
    }
    val pendingCount = pendingEntries.size
    val pendingAmount = pendingEntries.sumOf { it.grandTotalWithGst }

    val clearedEntries = remember(employeeEntries) {
        employeeEntries.filter { it.deliveryStatus == "Delivered" }
    }
    val clearedCount = clearedEntries.size
    val clearedAmount = clearedEntries.sumOf { it.grandTotalWithGst }

    // Filter entries based on search and tab
    val displayedEntries = remember(employeeEntries, pendingEntries, clearedEntries, searchQuery, selectedFilterTab, employeeVisitMap) {
        val baseList = when (selectedFilterTab) {
            "PENDING" -> pendingEntries
            "CLEARED" -> clearedEntries
            else -> employeeEntries
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { entry ->
                val visit = employeeVisitMap[entry.visitId]
                val custName = visit?.customerName ?: ""
                entry.orderNo.contains(searchQuery, ignoreCase = true) ||
                        entry.itemCode.contains(searchQuery, ignoreCase = true) ||
                        entry.supplierName.contains(searchQuery, ignoreCase = true) ||
                        custName.contains(searchQuery, ignoreCase = true) ||
                        entry.transporter.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = employee.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Agent Performance, Assisted Orders & Bill Status",
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
                    if (employee.phone.isNotBlank()) {
                        FilledTonalIconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${employee.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call Agent",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF6F8FB)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
                .nestedScroll(nestedScrollConnection)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Collapsible Profile Details (above Search Bar)
            AnimatedVisibility(
                visible = isProfileExpanded && searchQuery.isBlank(),
                enter = expandVertically(tween(240, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cardless Hero Profile Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 66dp Hero Centered Circle Avatar
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(2.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.size(66.dp)
                        ) {
                            if (employee.photoUri.isNotBlank()) {
                                AsyncImage(
                                    model = employee.photoUri,
                                    contentDescription = employee.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = employee.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = if (employee.role.equals("Admin", ignoreCase = true)) Color(0xFFFEF3C7) else Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = employee.role,
                                    color = if (employee.role.equals("Admin", ignoreCase = true)) Color(0xFF92400E) else Color(0xFF1D4ED8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            if (employee.employeeId.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = "ID: ${employee.employeeId}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Action Pills: Phone & Email
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (employee.phone.isNotBlank()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${employee.phone}"))
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF059669), modifier = Modifier.size(13.dp))
                                        Text(text = employee.phone, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF065F46))
                                    }
                                }
                            }

                            if (employee.email.isNotBlank()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${employee.email}"))
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0xFF475569), modifier = Modifier.size(13.dp))
                                        Text(text = "Email", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                    }
                                }
                            }
                        }
                    }

                    // Cardless Stats Row (NO cards around the numbers, clean typography with 1dp vertical dividers)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedFilterTab = "ALL" }
                        ) {
                            Text(
                                text = "$totalEntriesCount",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Orders",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedFilterTab = "PENDING" }
                        ) {
                            Text(
                                text = "$pendingCount",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingCount > 0) Color(0xFFDC2626) else Color(0xFF059669)
                            )
                            Text(
                                text = "Pending",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedFilterTab = "CLEARED" }
                        ) {
                            Text(
                                text = "$clearedCount",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                            Text(
                                text = "Cleared",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedFilterTab = "VISITS" }
                        ) {
                            Text(
                                text = "$totalVisitsCount",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                            Text(
                                text = "Visits",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Extended details card: territory, addresses, emergency
                    val hasExtendedDetails = employee.assignedMarkets.isNotBlank() ||
                            employee.currentAddress.isNotBlank() ||
                            employee.emergencyContactPhone.isNotBlank() ||
                            employee.referredBy.isNotBlank()

                    if (hasExtendedDetails) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (employee.assignedMarkets.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("Territory: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                        Text(employee.assignedMarkets, fontSize = 11.sp, color = Color(0xFF0F766E), fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (employee.currentAddress.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("Address: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                        Text(employee.currentAddress, fontSize = 11.sp, color = Color(0xFF1E293B))
                                    }
                                }
                                if (employee.emergencyContactPhone.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${employee.emergencyContactPhone}"))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Text("Emergency: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                        Text("${employee.emergencyContactName.ifBlank { "Contact" }} (${employee.emergencyContactPhone})", fontSize = 11.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Compact 36dp Pill Search Bar (Fixed / Pinned directly below Hero)
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search Customer, Order #, Item, Supplier...",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2563EB)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(11.dp),
                                tint = Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            // 3. LazyColumn for List items & Filters
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Unboxed Segment Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    listOf(
                        "ALL" to "All ($totalEntriesCount)",
                        "PENDING" to "Pending ($pendingCount)",
                        "CLEARED" to "Cleared ($clearedCount)",
                        "VISITS" to "Visits ($totalVisitsCount)"
                    ).forEach { (tabKey, label) ->
                        val isSelected = selectedFilterTab == tabKey
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF1E3A8A) else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedFilterTab = tabKey }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // View Content depending on Tab
            if (selectedFilterTab == "VISITS") {
                // List of visits handled by this employee
                if (employeeVisits.isEmpty()) {
                    item {
                        EmptyStateCard("No visits recorded for this employee.")
                    }
                } else {
                    items(employeeVisits, key = { it.id }) { visit ->
                        val visitEntries = employeeEntries.filter { it.visitId == visit.id }
                        val visitPieces = visitEntries.sumOf { it.pieces }
                        val visitAmount = visitEntries.sumOf { it.grandTotalWithGst }

                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                            text = visit.customerName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${visit.date} • Code: ${visit.visitCode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = { onOpenVisit(visit) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = 34.dp)
                                    ) {
                                        Text("Open Visit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${visitEntries.size} Items • ${String.format("%,d", visitPieces)} pcs",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Total: ₹${String.format("%,.0f", visitAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (visit.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Notes: ${visit.notes}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // List of Purchase Entries where agent assisted
                if (displayedEntries.isEmpty()) {
                    item {
                        EmptyStateCard("No entries matching the current filter.")
                    }
                } else {
                    items(displayedEntries, key = { it.id }) { entry ->
                        val visit = employeeVisitMap[entry.visitId]
                        EmployeeEntryCard(
                            entry = entry,
                            visit = visit,
                            onOpenVisit = {
                                if (visit != null) onOpenVisit(visit)
                            },
                            onAdvanceStatus = { viewModel.advanceEntryDeliveryStatus(entry) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun EmployeeEntryCard(
    entry: PurchaseEntryEntity,
    visit: VisitEntity?,
    onOpenVisit: () -> Unit,
    onAdvanceStatus: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Customer Name & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = visit?.customerName ?: "Direct Customer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Order: ${entry.orderNo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (visit != null) {
                            Text(
                                text = " • ${visit.date}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                DeliveryStatusBadge(status = entry.deliveryStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Item Details & Supplier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Item: ${entry.itemCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Supplier: ${entry.supplierName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Case Size & Transporter
            Spacer(modifier = Modifier.height(6.dp))
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
                    val packText = if (entry.caseCount > 0 && entry.loosePieces > 0) {
                        "${entry.caseCount} Cases + ${entry.loosePieces} Loose"
                    } else if (entry.caseCount > 0) {
                        "${entry.caseCount} Cases"
                    } else {
                        "${entry.loosePieces} Loose"
                    }
                    Text(
                        text = "$packText (${entry.pieces} pcs)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (entry.transporter.isNotBlank()) {
                        Text(
                            text = entry.transporter,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Row: Open Visit & Quick Status Advance
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (visit != null) {
                    OutlinedButton(
                        onClick = onOpenVisit,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Visit", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (entry.deliveryStatus != "Delivered") {
                    Button(
                        onClick = onAdvanceStatus,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                    ) {
                        val nextLabel = when (entry.deliveryStatus.lowercase()) {
                            "pending" -> "Mark Packed"
                            "packed" -> "Mark Dispatched"
                            "dispatched" -> "Mark Delivered"
                            else -> "Advance"
                        }
                        Text(nextLabel, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
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
                Icons.AutoMirrored.Filled.Assignment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
