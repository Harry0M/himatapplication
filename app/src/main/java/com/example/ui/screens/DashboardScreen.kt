package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.IncompleteCaseBanner
import com.example.ui.components.StatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.dialogs.CustomDateRangePickerDialog
import com.example.ui.theme.ManufacturerBadge
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WholesalerBadge
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DashboardBarItem(
    val id: String,
    val label: String,
    val fullDateLabel: String,
    val pieces: Int,
    val amount: Double,
    val orderCount: Int
)

data class PieChartSlice(
    val name: String,
    val value: Float,
    val formattedValue: String,
    val color: Color,
    val percentage: Float
)

@Composable
fun DashboardScreen(
    viewModel: HimatViewModel,
    onNavigate: (AppScreen) -> Unit,
    onOpenNewVisit: () -> Unit,
    onOpenVisit: (VisitEntity) -> Unit,
    onBack: () -> Unit = { onNavigate(AppScreen.DASHBOARD) }
) {
    val role by viewModel.currentRole.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val rawVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val rawEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.visibleSuppliers.collectAsStateWithLifecycle()
    val allPackGroups by viewModel.allPackGroups.collectAsStateWithLifecycle()

    // 1. Role-Based Visibility (Admin sees all; Employee sees strictly only their own)
    val isAdmin = remember(role) { role.equals("Admin", ignoreCase = true) }

    val baseVisits = remember(rawVisits, isAdmin, currentEmployee) {
        rawVisits.filter { !it.isDeleted }.filter { visit ->
            if (isAdmin || currentEmployee == null) true
            else visit.employeeId == currentEmployee?.id || visit.employeeName.equals(currentEmployee?.name, ignoreCase = true)
        }
    }

    val allowedVisitIds = remember(baseVisits) { baseVisits.map { it.id }.toSet() }

    val baseEntries = remember(rawEntries, allowedVisitIds, isAdmin) {
        rawEntries.filter { !it.isDeleted }.filter { entry ->
            if (isAdmin) true
            else entry.visitId in allowedVisitIds
        }
    }

    // 2. Time Filter State
    var selectedPeriod by remember { mutableStateOf("This Month") }
    var customStartDateMillis by remember { mutableStateOf<Long?>(null) }
    var customEndDateMillis by remember { mutableStateOf<Long?>(null) }
    var customDateLabel by remember { mutableStateOf<String?>(null) }
    var showCustomDatePickerDialog by remember { mutableStateOf(false) }

    val (todayStart, yesterdayStart, weekStart, monthStart) = remember {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tStart = todayCal.timeInMillis
        val yStart = tStart - 86400000L
        val wStart = tStart - (6 * 86400000L)
        val mStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        listOf(tStart, yStart, wStart, mStart)
    }

    fun isInPeriod(time: Long): Boolean {
        return when (selectedPeriod) {
            "Today" -> time >= todayStart
            "Yesterday" -> time in yesterdayStart until todayStart
            "This Week" -> time >= weekStart
            "This Month" -> time >= monthStart
            "Custom" -> {
                if (customStartDateMillis != null && customEndDateMillis != null) {
                    time in customStartDateMillis!!..customEndDateMillis!!
                } else true
            }
            else -> true // "All Time"
        }
    }

    // Order counts for filter chips
    val todayCount = remember(baseEntries, todayStart) { baseEntries.count { it.createdAt >= todayStart } }
    val yesterdayCount = remember(baseEntries, yesterdayStart, todayStart) { baseEntries.count { it.createdAt in yesterdayStart until todayStart } }
    val weekCount = remember(baseEntries, weekStart) { baseEntries.count { it.createdAt >= weekStart } }
    val monthCount = remember(baseEntries, monthStart) { baseEntries.count { it.createdAt >= monthStart } }
    val allCount = remember(baseEntries) { baseEntries.size }

    // Dynamic Filtered Data in current period
    val filteredEntries = remember(baseEntries, selectedPeriod, customStartDateMillis, customEndDateMillis) {
        baseEntries.filter { isInPeriod(it.createdAt) }
    }
    val filteredVisits = remember(baseVisits, selectedPeriod, customStartDateMillis, customEndDateMillis) {
        baseVisits.filter { isInPeriod(it.createdAt) }
    }

    // Dynamic metrics
    val totalPieces = remember(filteredEntries) { filteredEntries.sumOf { it.pieces } }
    val totalCases = remember(filteredEntries) { filteredEntries.sumOf { it.caseCount } }
    // Accurate Loose Pack Status: check if entries are already fit into pack groups
    val packedEntryIds = remember(allPackGroups) {
        allPackGroups.flatMap { group ->
            group.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() }
        }.toSet()
    }

    val isEntryPacked: (PurchaseEntryEntity) -> Boolean = remember(packedEntryIds) {
        { entry ->
            (entry.packGroupId != null && entry.packGroupId != 0L) ||
            (entry.id in packedEntryIds) ||
            !entry.mixedPackNote.isNullOrBlank()
        }
    }

    val unfixedLooseEntries = remember(filteredEntries, isEntryPacked) {
        filteredEntries.filter { it.loosePieces > 0 && !isEntryPacked(it) }
    }

    val filteredVisitIds = remember(filteredVisits) { filteredVisits.map { it.id }.toSet() }
    val relevantPackGroups = remember(allPackGroups, filteredVisitIds) {
        allPackGroups.filter { it.visitId in filteredVisitIds }
    }

    val totalUnfixedLoose = remember(unfixedLooseEntries, relevantPackGroups) {
        unfixedLooseEntries.sumOf { it.loosePieces } + relevantPackGroups.sumOf { it.remainingLoose }
    }
    val grandTotalAmount = remember(filteredEntries) { filteredEntries.sumOf { it.grandTotalWithGst } }
    val activeVisits = remember(filteredVisits) { filteredVisits.filter { it.status.equals("Active", ignoreCase = true) } }

    // Dispatch & Pipeline stats
    val pendingCount = remember(filteredEntries) { filteredEntries.count { it.deliveryStatus.equals("Pending", ignoreCase = true) } }
    val packedCount = remember(filteredEntries) { filteredEntries.count { it.deliveryStatus.equals("Packed", ignoreCase = true) } }
    val dispatchedCount = remember(filteredEntries) { filteredEntries.count { it.deliveryStatus.equals("Dispatched", ignoreCase = true) } }
    val deliveredCount = remember(filteredEntries) { filteredEntries.count { it.deliveryStatus.equals("Delivered", ignoreCase = true) } }
    val inTransitCount = pendingCount + packedCount + dispatchedCount

    // Sourcing Split: Manufacturer vs Wholesaler
    val manufacturerEntries = remember(filteredEntries) { filteredEntries.filter { it.supplierType.equals("Manufacturer", ignoreCase = true) } }
    val wholesalerEntries = remember(filteredEntries) { filteredEntries.filter { it.supplierType.equals("Wholesaler", ignoreCase = true) } }
    val mfrPcs = remember(manufacturerEntries) { manufacturerEntries.sumOf { it.pieces } }
    val wholesalePcs = remember(wholesalerEntries) { wholesalerEntries.sumOf { it.pieces } }
    val mfrAmount = remember(manufacturerEntries) { manufacturerEntries.sumOf { it.grandTotalWithGst } }
    val wholesaleAmount = remember(wholesalerEntries) { wholesalerEntries.sumOf { it.grandTotalWithGst } }

    // Top Suppliers ranking in period
    val topSuppliers = remember(filteredEntries) {
        filteredEntries
            .groupBy { it.supplierName.ifBlank { "Direct Purchase" } }
            .map { (name, list) ->
                val supType = list.firstOrNull()?.supplierType ?: "Wholesaler"
                Tuple4(name, list.sumOf { it.pieces }, list.sumOf { it.grandTotalWithGst }, list.size, supType)
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    // Retailer aggregates in period
    val retailerAggregates = remember(filteredVisits, filteredEntries) {
        filteredVisits.groupBy { it.customerName }.map { (custName, vList) ->
            val vIds = vList.map { it.id }.toSet()
            val vEntries = filteredEntries.filter { it.visitId in vIds }
            val pcs = vEntries.sumOf { it.pieces }
            val amt = vEntries.sumOf { it.grandTotalWithGst }
            Triple(custName, pcs, amt)
        }.sortedByDescending { it.second }.take(6)
    }

    // Cylindrical Chart data
    val cylindricalChartData = remember(filteredEntries, selectedPeriod, customStartDateMillis, customEndDateMillis) {
        buildTrendChartData(selectedPeriod, filteredEntries, customStartDateMillis, customEndDateMillis)
    }

    if (showCustomDatePickerDialog) {
        CustomDateRangePickerDialog(
            initialStartMillis = customStartDateMillis,
            initialEndMillis = customEndDateMillis,
            onDismissRequest = { showCustomDatePickerDialog = false },
            onDateRangeSelected = { start, end, label ->
                customStartDateMillis = start
                customEndDateMillis = end
                customDateLabel = label
                selectedPeriod = "Custom"
                showCustomDatePickerDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
    ) {
        // NO HEADER TITLE OR BUTTONS. Start directly with Role Scope & Time Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isAdmin) Color(0xFFEFF6FF) else Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, if (isAdmin) Color(0xFFBFDBFE) else Color(0xFFBBF7D0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.Security else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isAdmin) Color(0xFF1D4ED8) else Color(0xFF15803D),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isAdmin) "Admin: All Reports" else "My Reports (${currentEmployee?.name ?: "Sales"})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdmin) Color(0xFF1E40AF) else Color(0xFF166534)
                    )
                }
            }

            Text(
                text = when (selectedPeriod) {
                    "Today" -> "Today"
                    "Yesterday" -> "Yesterday"
                    "This Week" -> "Last 7 Days"
                    "This Month" -> "This Month"
                    "Custom" -> customDateLabel ?: "Custom"
                    else -> "All-Time"
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }

        // Time Period Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimePeriodFilterChip(
                label = "Today",
                count = todayCount,
                isSelected = selectedPeriod == "Today",
                onClick = { selectedPeriod = "Today" }
            )
            TimePeriodFilterChip(
                label = "Yesterday",
                count = yesterdayCount,
                isSelected = selectedPeriod == "Yesterday",
                onClick = { selectedPeriod = "Yesterday" }
            )
            TimePeriodFilterChip(
                label = "This Week",
                count = weekCount,
                isSelected = selectedPeriod == "This Week",
                onClick = { selectedPeriod = "This Week" }
            )
            TimePeriodFilterChip(
                label = "This Month",
                count = monthCount,
                isSelected = selectedPeriod == "This Month",
                onClick = { selectedPeriod = "This Month" }
            )
            TimePeriodFilterChip(
                label = "All Time",
                count = allCount,
                isSelected = selectedPeriod == "All Time",
                onClick = { selectedPeriod = "All Time" }
            )

            // Custom Range Picker Chip
            val isCustomActive = selectedPeriod == "Custom"
            Surface(
                shape = CircleShape,
                color = if (isCustomActive) NavyPrimary else Color.White,
                border = BorderStroke(1.dp, if (isCustomActive) NavyPrimary else Color(0xFFE2E8F0)),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showCustomDatePickerDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (isCustomActive) Color.White else NavyPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCustomActive && !customDateLabel.isNullOrBlank()) customDateLabel!! else "Custom 📅",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (isCustomActive) Color.White else NavyPrimary
                    )
                    if (isCustomActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reset Date",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    selectedPeriod = "This Month"
                                    customStartDateMillis = null
                                    customEndDateMillis = null
                                    customDateLabel = null
                                }
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. CHARTS RIGHT AT THE START: Rounded Cylindrical Bar Chart
            item {
                DashboardCylindricalChart(
                    periodLabel = selectedPeriod,
                    chartData = cylindricalChartData
                )
            }

            // 2. CHARTS RIGHT AT THE START: Native Compose Pie / Donut Chart
            item {
                DashboardPieChart(
                    totalPieces = totalPieces,
                    totalOrders = filteredEntries.size,
                    mfrPcs = mfrPcs,
                    wholesalePcs = wholesalePcs,
                    mfrAmount = mfrAmount,
                    wholesaleAmount = wholesaleAmount,
                    pendingCount = pendingCount,
                    packedCount = packedCount,
                    dispatchedCount = dispatchedCount,
                    deliveredCount = deliveredCount
                )
            }

            // 3. REPORTS SECTION: Primary KPI Metric Summary Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardMetricCard(
                            title = "Procured Volume",
                            value = "${String.format("%,d", totalPieces)} Pcs",
                            subtitle = if (totalUnfixedLoose > 0) "$totalCases Cases • $totalUnfixedLoose Loose" else "$totalCases Cases Packed",
                            accentColor = Color(0xFF059669),
                            icon = Icons.Default.Inventory,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppScreen.PURCHASE_ORDERS) }
                        )

                        DashboardMetricCard(
                            title = "Procurement Spend",
                            value = "₹${PdfGenerator.formatInr(grandTotalAmount)}",
                            subtitle = "${filteredEntries.size} Orders Logged",
                            accentColor = Color(0xFF1E3A8A),
                            icon = Icons.Default.CurrencyRupee,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppScreen.PURCHASE_ORDERS) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardMetricCard(
                            title = "Market Trips",
                            value = "${activeVisits.size} Active",
                            subtitle = "${filteredVisits.size} Total in Period",
                            accentColor = Color(0xFF4F46E5),
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppScreen.VISITS) }
                        )

                        val deliveryRate = if (filteredEntries.isNotEmpty()) (deliveredCount * 100 / filteredEntries.size) else 0
                        DashboardMetricCard(
                            title = "Dispatch Pipeline",
                            value = "$inTransitCount In-Transit",
                            subtitle = "$deliveredCount Delivered ($deliveryRate%)",
                            accentColor = Color(0xFFD97706),
                            icon = Icons.Default.LocalShipping,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppScreen.DELIVERIES) }
                        )
                    }
                }
            }

            // 4. Loose Pieces Packing Alert Banner (ONLY if there are genuine UNFIXED loose pieces)
            if (totalUnfixedLoose > 0 && unfixedLooseEntries.isNotEmpty()) {
                item {
                    IncompleteCaseBanner(
                        looseCount = totalUnfixedLoose,
                        ordersCount = unfixedLooseEntries.size,
                        onMixedPackClick = {
                            val activeVisit = baseVisits.firstOrNull { it.status == "Active" } ?: baseVisits.firstOrNull()
                            if (activeVisit != null) {
                                onOpenVisit(activeVisit)
                            } else {
                                onNavigate(AppScreen.VISITS)
                            }
                        }
                    )
                }
            }

            // 5. REPORTS SECTION: Source Distribution (Manufacturer vs Wholesaler Split Report)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Source Distribution Report",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "${filteredEntries.size} orders",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val mfrRatio = if (grandTotalAmount > 0) (mfrAmount / grandTotalAmount).toFloat() else 0.5f
                        LinearProgressIndicator(
                            progress = { mfrRatio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = ManufacturerBadge,
                            trackColor = WholesalerBadge
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(ManufacturerBadge, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Manufacturers (Direct)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NavyPrimary)
                                }
                                Text("₹${PdfGenerator.formatInr(mfrAmount)}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ManufacturerBadge)
                                Text("$mfrPcs Pcs • ${manufacturerEntries.size} orders", fontSize = 11.sp, color = TextSecondary)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(WholesalerBadge, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Wholesalers / Hubs", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NavyPrimary)
                                }
                                Text("₹${PdfGenerator.formatInr(wholesaleAmount)}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = WholesalerBadge)
                                Text("$wholesalePcs Pcs • ${wholesalerEntries.size} orders", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // 6. REPORTS SECTION: Top Sourcing Partners (Suppliers Ranking Report)
            if (topSuppliers.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Top Sourcing Partners (Suppliers)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = NavyPrimary
                                    )
                                }
                                Text(
                                    text = "Volume",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val maxPcs = topSuppliers.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                            topSuppliers.forEach { (name, pcs, amt, orderCnt, supType) ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.5.sp,
                                                color = NavyPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            SupplierTypeBadge(type = supType)
                                        }
                                        Text(
                                            text = "$pcs Pcs • ₹${PdfGenerator.formatInr(amt)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (pcs.toFloat() / maxPcs).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape),
                                        color = NavyPrimary,
                                        trackColor = Color(0xFFF1F5F9)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. REPORTS SECTION: Retailer Procurement Aggregates Report
            if (retailerAggregates.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Retailer Procurement Aggregates",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = NavyPrimary
                                    )
                                }
                                Text(
                                    text = "${retailerAggregates.size} Retailers",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            retailerAggregates.forEach { (custName, pcs, amt) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = custName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = "$pcs Pieces sourced",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Text(
                                        text = "₹${PdfGenerator.formatInr(amt)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF059669)
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }

            // 8. Recent Market Visits Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Market Trips ($selectedPeriod)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NavyPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigate(AppScreen.VISITS) }
                    ) {
                        Text(
                            text = "View All",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
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

            if (filteredVisits.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No market visits recorded for this period.",
                            fontSize = 12.5.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(filteredVisits.take(5)) { visit ->
                    val visitEntries = filteredEntries.filter { it.visitId == visit.id }
                    val tripPcs = visitEntries.sumOf { it.pieces }
                    val cust = customers.find { it.id == visit.customerId || it.firmName.equals(visit.customerName, true) || it.name.equals(visit.customerName, true) }
                    val photoUrl = cust?.let { it.purchaserPhotoUri.ifBlank { it.shopPhotoUri } }?.takeIf { it.isNotBlank() }
                        ?: visitEntries.firstOrNull { !it.orderFormPhotoUri.isNullOrBlank() }?.orderFormPhotoUri

                    VisitCardItem(
                        visit = visit,
                        entriesCount = visitEntries.size,
                        totalPcs = tripPcs,
                        photoUrl = photoUrl,
                        onClick = { onOpenVisit(visit) }
                    )
                }
            }
        }
    }
}

/**
 * 3D-styled Rounded Cylindrical Bar Chart
 */
@Composable
fun DashboardCylindricalChart(
    periodLabel: String,
    chartData: List<DashboardBarItem>,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf("pieces") } // "pieces" or "value"
    var selectedIndex by remember(chartData) {
        val lastIdx = chartData.indexOfLast { if (selectedMetric == "pieces") it.pieces > 0 else it.amount > 0 }
        mutableStateOf(if (lastIdx >= 0) lastIdx else chartData.lastIndex.coerceAtLeast(0))
    }

    val maxVal = remember(chartData, selectedMetric) {
        if (selectedMetric == "pieces") {
            chartData.maxOfOrNull { it.pieces }?.coerceAtLeast(1) ?: 1
        } else {
            chartData.maxOfOrNull { it.amount.toInt() }?.coerceAtLeast(1) ?: 1
        }
    }

    val peakItem = remember(chartData, selectedMetric) {
        if (selectedMetric == "pieces") chartData.maxByOrNull { it.pieces }
        else chartData.maxByOrNull { it.amount }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with metric toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Procurement Trend",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = NavyPrimary
                    )
                }

                // Switcher Pill: Volume vs Value
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selectedMetric == "pieces") NavyPrimary else Color.Transparent)
                                .clickable { selectedMetric = "pieces" }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Pcs",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMetric == "pieces") Color.White else TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selectedMetric == "value") NavyPrimary else Color.Transparent)
                                .clickable { selectedMetric = "value" }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "₹ Value",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMetric == "value") Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selection details box
            val selectedItem = chartData.getOrNull(selectedIndex)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedItem != null) {
                        Column {
                            Text(
                                text = selectedItem.fullDateLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (selectedMetric == "pieces") "${String.format("%,d", selectedItem.pieces)} Pcs" else "₹${PdfGenerator.formatInr(selectedItem.amount)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NavyPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${selectedItem.orderCount} orders)",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Tap cylinder to inspect",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    if (peakItem != null && (peakItem.pieces > 0 || peakItem.amount > 0)) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Text(
                                text = "Peak: ${peakItem.label}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3D Rounded Cylindrical Bars Representation
            if (chartData.all { it.pieces == 0 && it.amount == 0.0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No procurement entries for $periodLabel",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartData.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        val curVal = if (selectedMetric == "pieces") item.pieces.toFloat() else item.amount.toFloat()
                        val heightFraction = if (maxVal > 0) (curVal / maxVal).coerceIn(0.06f, 1f) else 0.06f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedIndex = index },
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (isSelected && curVal > 0) {
                                Text(
                                    text = if (selectedMetric == "pieces") "${item.pieces}" else "₹${if (item.amount >= 1000) "${(item.amount / 1000).toInt()}k" else "${item.amount.toInt()}"}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Cylindrical 3D Bar with rounded ends & metallic reflection gradient
                            val cylinderWidth = if (chartData.size > 7) 14.dp else 22.dp
                            Box(
                                modifier = Modifier
                                    .width(cylinderWidth)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = if (isSelected) {
                                                if (selectedMetric == "pieces") listOf(
                                                    Color(0xFF047857), // deep emerald
                                                    Color(0xFF34D399), // highlight center
                                                    Color(0xFF064E3B)  // dark shadow edge
                                                ) else listOf(
                                                    Color(0xFF1E3A8A), // deep navy
                                                    Color(0xFF60A5FA), // light blue highlight
                                                    Color(0xFF0F172A)  // dark shadow edge
                                                )
                                            } else {
                                                if (selectedMetric == "pieces") listOf(
                                                    Color(0xFF059669).copy(alpha = 0.6f),
                                                    Color(0xFF6EE7B7).copy(alpha = 0.85f),
                                                    Color(0xFF047857).copy(alpha = 0.6f)
                                                ) else listOf(
                                                    Color(0xFF3B82F6).copy(alpha = 0.6f),
                                                    Color(0xFF93C5FD).copy(alpha = 0.85f),
                                                    Color(0xFF1D4ED8).copy(alpha = 0.6f)
                                                )
                                            }
                                        )
                                    ),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                // 3D Top Cap Ellipse
                                Box(
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(width = cylinderWidth * 0.75f, height = 3.5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = if (isSelected) 0.65f else 0.4f))
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = item.label,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NavyPrimary else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Native Jetpack Compose Canvas Pie / Donut Chart
 */
@Composable
fun DashboardPieChart(
    totalPieces: Int,
    totalOrders: Int,
    mfrPcs: Int,
    wholesalePcs: Int,
    mfrAmount: Double,
    wholesaleAmount: Double,
    pendingCount: Int,
    packedCount: Int,
    dispatchedCount: Int,
    deliveredCount: Int,
    modifier: Modifier = Modifier
) {
    var pieMode by remember { mutableStateOf("source") } // "source" (Mfr vs Whls) or "pipeline" (Delivery Status)

    val slices = remember(pieMode, mfrPcs, wholesalePcs, pendingCount, packedCount, dispatchedCount, deliveredCount) {
        if (pieMode == "source") {
            val total = (mfrPcs + wholesalePcs).coerceAtLeast(1).toFloat()
            listOf(
                PieChartSlice(
                    name = "Manufacturers",
                    value = mfrPcs.toFloat(),
                    formattedValue = "$mfrPcs Pcs (₹${PdfGenerator.formatInr(mfrAmount)})",
                    color = ManufacturerBadge,
                    percentage = if (totalPieces > 0) (mfrPcs * 100f / totalPieces) else 50f
                ),
                PieChartSlice(
                    name = "Wholesalers",
                    value = wholesalePcs.toFloat(),
                    formattedValue = "$wholesalePcs Pcs (₹${PdfGenerator.formatInr(wholesaleAmount)})",
                    color = WholesalerBadge,
                    percentage = if (totalPieces > 0) (wholesalePcs * 100f / totalPieces) else 50f
                )
            )
        } else {
            val total = (pendingCount + packedCount + dispatchedCount + deliveredCount).coerceAtLeast(1).toFloat()
            listOf(
                PieChartSlice("Delivered", deliveredCount.toFloat(), "$deliveredCount orders", Color(0xFF059669), deliveredCount * 100f / total),
                PieChartSlice("Dispatched", dispatchedCount.toFloat(), "$dispatchedCount orders", Color(0xFF0D9488), dispatchedCount * 100f / total),
                PieChartSlice("Packed", packedCount.toFloat(), "$packedCount orders", Color(0xFF2563EB), packedCount * 100f / total),
                PieChartSlice("Pending", pendingCount.toFloat(), "$pendingCount orders", Color(0xFFD97706), pendingCount * 100f / total)
            )
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with Pie Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pieMode == "source") "Source Split (Mfr vs Whls)" else "Dispatch Status Pipeline",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = NavyPrimary
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (pieMode == "source") NavyPrimary else Color.Transparent)
                                .clickable { pieMode = "source" }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Source",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pieMode == "source") Color.White else TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (pieMode == "pipeline") NavyPrimary else Color.Transparent)
                                .clickable { pieMode = "pipeline" }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Dispatch",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pieMode == "pipeline") Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pie Chart Canvas & Legend Row
            val hasData = if (pieMode == "source") (mfrPcs + wholesalePcs) > 0 else totalOrders > 0
            if (!hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data recorded for pie chart distribution",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Donut Canvas with center total
                    Box(
                        modifier = Modifier.size(125.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val canvasSize = size.minDimension
                            val arcSize = Size(canvasSize, canvasSize)
                            val topLeft = Offset((size.width - canvasSize) / 2f, (size.height - canvasSize) / 2f)

                            slices.forEach { slice ->
                                val sweep = (slice.percentage / 100f) * 360f
                                if (sweep > 0.5f) {
                                    drawArc(
                                        color = slice.color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = true,
                                        size = arcSize,
                                        topLeft = topLeft
                                    )
                                    startAngle += sweep
                                }
                            }

                            // Donut Center Cutout
                            drawCircle(
                                color = Color.White,
                                radius = canvasSize * 0.32f,
                                center = center
                            )
                        }

                        // Center Metric inside Donut Hole
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (pieMode == "source") "$totalPieces" else "$totalOrders",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = if (pieMode == "source") "Pcs" else "Orders",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Legend Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        slices.forEach { slice ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .background(slice.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = slice.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NavyPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${slice.percentage.toInt()}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = slice.color
                                        )
                                    }
                                    Text(
                                        text = slice.formattedValue,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

private fun buildTrendChartData(
    period: String,
    entries: List<PurchaseEntryEntity>,
    customStart: Long?,
    customEnd: Long?
): List<DashboardBarItem> {
    val dayFmt = SimpleDateFormat("dd MMM", Locale.getDefault())
    val shortDayFmt = SimpleDateFormat("EEE", Locale.getDefault())
    val fullFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    return when (period) {
        "Today" -> {
            val baseCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val slots = listOf(
                Pair("Morning", "Morning (Until 12 PM)") to (baseCal.timeInMillis until (baseCal.timeInMillis + 12 * 3600000L)),
                Pair("Afternoon", "Afternoon (12 PM - 3 PM)") to ((baseCal.timeInMillis + 12 * 3600000L) until (baseCal.timeInMillis + 15 * 3600000L)),
                Pair("Evening", "Evening (3 PM - 6 PM)") to ((baseCal.timeInMillis + 15 * 3600000L) until (baseCal.timeInMillis + 18 * 3600000L)),
                Pair("Night", "Night (After 6 PM)") to ((baseCal.timeInMillis + 18 * 3600000L) until (baseCal.timeInMillis + 24 * 3600000L))
            )
            slots.map { (labelPair, range) ->
                val bucketEntries = entries.filter { it.createdAt in range }
                DashboardBarItem(
                    id = labelPair.first,
                    label = labelPair.first,
                    fullDateLabel = labelPair.second,
                    pieces = bucketEntries.sumOf { it.pieces },
                    amount = bucketEntries.sumOf { it.grandTotalWithGst },
                    orderCount = bucketEntries.size
                )
            }
        }
        "Yesterday" -> {
            val baseCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val slots = listOf(
                Pair("Morning", "Yesterday Morning") to (baseCal.timeInMillis until (baseCal.timeInMillis + 12 * 3600000L)),
                Pair("Afternoon", "Yesterday Afternoon") to ((baseCal.timeInMillis + 12 * 3600000L) until (baseCal.timeInMillis + 15 * 3600000L)),
                Pair("Evening", "Yesterday Evening") to ((baseCal.timeInMillis + 15 * 3600000L) until (baseCal.timeInMillis + 18 * 3600000L)),
                Pair("Night", "Yesterday Night") to ((baseCal.timeInMillis + 18 * 3600000L) until (baseCal.timeInMillis + 24 * 3600000L))
            )
            slots.map { (labelPair, range) ->
                val bucketEntries = entries.filter { it.createdAt in range }
                DashboardBarItem(
                    id = labelPair.first,
                    label = labelPair.first,
                    fullDateLabel = labelPair.second,
                    pieces = bucketEntries.sumOf { it.pieces },
                    amount = bucketEntries.sumOf { it.grandTotalWithGst },
                    orderCount = bucketEntries.size
                )
            }
        }
        "This Week" -> {
            val items = mutableListOf<DashboardBarItem>()
            val c = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            c.add(Calendar.DAY_OF_YEAR, -6)
            for (i in 0..6) {
                val start = c.timeInMillis
                val end = start + 86400000L - 1
                val dayEntries = entries.filter { it.createdAt in start..end }
                items.add(
                    DashboardBarItem(
                        id = "week_day_$i",
                        label = shortDayFmt.format(c.time),
                        fullDateLabel = fullFmt.format(c.time),
                        pieces = dayEntries.sumOf { it.pieces },
                        amount = dayEntries.sumOf { it.grandTotalWithGst },
                        orderCount = dayEntries.size
                    )
                )
                c.add(Calendar.DAY_OF_YEAR, 1)
            }
            items
        }
        "This Month" -> {
            val cMonth = Calendar.getInstance()
            val monthMaxDays = cMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(cMonth.time)

            val weeks = listOf(
                Triple("1-7 $monthName", "Week 1 (1-7 $monthName)", 1 to 7),
                Triple("8-14 $monthName", "Week 2 (8-14 $monthName)", 8 to 14),
                Triple("15-21 $monthName", "Week 3 (15-21 $monthName)", 15 to 21),
                Triple("22-$monthMaxDays $monthName", "Week 4 (22-$monthMaxDays $monthName)", 22 to monthMaxDays)
            )

            weeks.map { (shortLbl, fullLbl, dayRange) ->
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, dayRange.first)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, dayRange.second)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val bucketEntries = entries.filter { it.createdAt in startCal.timeInMillis..endCal.timeInMillis }
                DashboardBarItem(
                    id = shortLbl,
                    label = shortLbl,
                    fullDateLabel = fullLbl,
                    pieces = bucketEntries.sumOf { it.pieces },
                    amount = bucketEntries.sumOf { it.grandTotalWithGst },
                    orderCount = bucketEntries.size
                )
            }
        }
        "Custom" -> {
            if (customStart != null && customEnd != null) {
                val diffDays = ((customEnd - customStart) / 86400000L).toInt() + 1
                if (diffDays <= 7) {
                    val items = mutableListOf<DashboardBarItem>()
                    val c = Calendar.getInstance().apply { timeInMillis = customStart }
                    for (i in 0 until diffDays) {
                        val s = c.timeInMillis
                        val e = s + 86400000L - 1
                        val dayEntries = entries.filter { it.createdAt in s..e }
                        items.add(
                            DashboardBarItem(
                                id = "custom_day_$i",
                                label = dayFmt.format(c.time),
                                fullDateLabel = fullFmt.format(c.time),
                                pieces = dayEntries.sumOf { it.pieces },
                                amount = dayEntries.sumOf { it.grandTotalWithGst },
                                orderCount = dayEntries.size
                            )
                        )
                        c.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    items
                } else {
                    val bucketsCount = minOf(diffDays, 5)
                    val stepMillis = (customEnd - customStart) / bucketsCount
                    val items = mutableListOf<DashboardBarItem>()
                    for (i in 0 until bucketsCount) {
                        val s = customStart + (i * stepMillis)
                        val e = if (i == bucketsCount - 1) customEnd else s + stepMillis - 1
                        val bucketEntries = entries.filter { it.createdAt in s..e }
                        val startCal = Calendar.getInstance().apply { timeInMillis = s }
                        val endCal = Calendar.getInstance().apply { timeInMillis = e }
                        val lbl = dayFmt.format(startCal.time)
                        val fullLbl = "${dayFmt.format(startCal.time)} - ${dayFmt.format(endCal.time)}"
                        items.add(
                            DashboardBarItem(
                                id = "custom_bucket_$i",
                                label = lbl,
                                fullDateLabel = fullLbl,
                                pieces = bucketEntries.sumOf { it.pieces },
                                amount = bucketEntries.sumOf { it.grandTotalWithGst },
                                orderCount = bucketEntries.size
                            )
                        )
                    }
                    items
                }
            } else {
                buildTrendChartData("This Week", entries, null, null)
            }
        }
        else -> {
            val items = mutableListOf<DashboardBarItem>()
            val c = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -5)
            }

            val mFmt = SimpleDateFormat("MMM", Locale.getDefault())
            val mYearFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            for (i in 0..5) {
                val start = c.timeInMillis
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = start
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val mEntries = entries.filter { it.createdAt in start..endCal.timeInMillis }
                items.add(
                    DashboardBarItem(
                        id = "month_$i",
                        label = mFmt.format(c.time),
                        fullDateLabel = mYearFmt.format(c.time),
                        pieces = mEntries.sumOf { it.pieces },
                        amount = mEntries.sumOf { it.grandTotalWithGst },
                        orderCount = mEntries.size
                    )
                )
                c.add(Calendar.MONTH, 1)
            }
            items
        }
    }
}

@Composable
private fun TimePeriodFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) NavyPrimary else Color.White,
        border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFE2E8F0)),
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else NavyPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0xFFF1F5F9),
                modifier = Modifier.padding(0.dp)
            ) {
                Text(
                    text = "$count",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = if (isSelected) Color.White else TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun VisitCardItem(
    visit: VisitEntity,
    entriesCount: Int,
    totalPcs: Int,
    photoUrl: String? = null,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = visit.customerName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        val initials = visit.customerName.take(2).uppercase()
                        Text(
                            text = if (initials.isNotBlank()) initials else "MV",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = visit.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    StatusBadge(status = visit.status)
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${visit.visitCode} • ${visit.date} • Agent: ${visit.employeeName}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "$entriesCount stops • $totalPcs Pcs",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF059669)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavyPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class Tuple4<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
