package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Employee Profile & Role Card
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
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (employee.role == "Admin") MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.tertiaryContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = if (employee.role == "Admin") MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onTertiaryContainer,
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
                                            text = employee.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            color = if (employee.role == "Admin") MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondary,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = employee.role,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Employee ID: ${employee.employeeId} • Phone: ${employee.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Contact Phones (up to 5 phones with 1-click dial chips)
                        val employeePhones = listOfNotNull(
                            employee.phone.takeIf { it.isNotBlank() },
                            employee.phone2.takeIf { it.isNotBlank() },
                            employee.phone3.takeIf { it.isNotBlank() },
                            employee.phone4.takeIf { it.isNotBlank() },
                            employee.phone5.takeIf { it.isNotBlank() }
                        )
                        if (employeePhones.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                employeePhones.forEachIndexed { idx, p ->
                                    Surface(
                                        color = Color(0xFFF0FDF4),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        modifier = Modifier.clickable {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$p"))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                            Text(
                                                text = if (idx == 0) p else "Alt ${idx + 1}: $p",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF065F46)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Emails
                        val employeeEmails: List<String> = listOfNotNull(
                            employee.email.takeIf { it.isNotBlank() },
                            employee.alternateEmail.takeIf { it.isNotBlank() }
                        )
                        if (employeeEmails.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                employeeEmails.forEach { em ->
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.clickable {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$em"))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(12.dp))
                                            Text(
                                                text = em,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Emergency Contact Badge
                        if (employee.emergencyContactPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${employee.emergencyContactPhone}"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Emergency: ${employee.emergencyContactName.ifBlank { "Contact" }} (${employee.emergencyContactPhone})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }

                        // Extended details card: assigned markets, addresses, personal location, referredBy
                        val hasExtendedDetails = employee.assignedMarkets.isNotBlank() ||
                                employee.currentAddress.isNotBlank() ||
                                employee.permanentAddress.isNotBlank() ||
                                employee.personalLocation.isNotBlank() ||
                                employee.referredBy.isNotBlank()

                        if (hasExtendedDetails) {
                            Spacer(modifier = Modifier.height(10.dp))
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
                                            Text("Current: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            Text(employee.currentAddress, fontSize = 11.sp, color = Color(0xFF1E293B))
                                        }
                                    }
                                    if (employee.permanentAddress.isNotBlank() || employee.personalLocation.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("Native/Perm: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            Text(
                                                text = listOfNotNull(employee.permanentAddress.takeIf { it.isNotBlank() }, employee.personalLocation.takeIf { it.isNotBlank() }).joinToString(" • "),
                                                fontSize = 11.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                        }
                                    }
                                    if (employee.referredBy.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("Referred by: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            Text(employee.referredBy, fontSize = 11.sp, color = Color(0xFF1E293B))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // High-Priority Highlights: Pending vs Cleared Bills!
                        Text(
                            text = "Bill Status Summary (Unke Banaye Gaye Bills)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Pending Bills Highlight Card
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFilterTab = "PENDING" }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Pending Bills",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Icon(
                                            Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$pendingCount Bills",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₹${String.format("%,.0f", pendingAmount)} in transit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Cleared Bills Highlight Card
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFilterTab = "CLEARED" }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Cleared Bills",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$clearedCount Bills",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF059669)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₹${String.format("%,.0f", clearedAmount)} fulfilled",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary Row: Total Handled Orders & Visits
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CustomerSummaryChip(
                                title = "Total Handled Orders",
                                value = "$totalEntriesCount Orders",
                                subtitle = "${String.format("%,d", totalPieces)} pcs volume",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            CustomerSummaryChip(
                                title = "Visits & Days",
                                value = "$totalVisitsCount Visits",
                                subtitle = "₹${String.format("%,.0f", totalAmount)} total",
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Customer, Order #, Item, Supplier...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    singleLine = true
                )
            }

            // Segment Tabs (All Entries / Pending / Cleared / Visits)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All Entries ($totalEntriesCount)",
                        "PENDING" to "Pending Bills ($pendingCount)",
                        "CLEARED" to "Cleared Bills ($clearedCount)",
                        "VISITS" to "Handled Visits ($totalVisitsCount)"
                    ).forEach { (tabKey, label) ->
                        val isSelected = selectedFilterTab == tabKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilterTab = tabKey },
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
