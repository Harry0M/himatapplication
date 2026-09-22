package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.IncompleteCaseBanner
import com.example.ui.components.StatusBadge
import com.example.ui.dialogs.CustomDateRangePickerDialog
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
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

@Composable
fun DashboardScreen(
    viewModel: HimatViewModel,
    onNavigate: (AppScreen) -> Unit,
    onOpenNewVisit: () -> Unit,
    onOpenVisit: (VisitEntity) -> Unit,
    onBack: () -> Unit = { onNavigate(AppScreen.DASHBOARD) }
) {
    val visits by viewModel.visibleVisits.collectAsStateWithLifecycle()
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.visibleSuppliers.collectAsStateWithLifecycle()

    // 1. Time Filter State
    var selectedPeriod by remember { mutableStateOf("This Month") }
    var customStartDateMillis by remember { mutableStateOf<Long?>(null) }
    var customEndDateMillis by remember { mutableStateOf<Long?>(null) }
    var customDateLabel by remember { mutableStateOf<String?>(null) }
    var showCustomDatePickerDialog by remember { mutableStateOf(false) }

    // Precalculate boundary timestamps
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

    // Helper predicate to check if time falls into selected period
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

    // Fast order counts for the filter chips
    val todayCount = remember(entries, todayStart) { entries.count { !it.isDeleted && it.createdAt >= todayStart } }
    val yesterdayCount = remember(entries, yesterdayStart, todayStart) { entries.count { !it.isDeleted && it.createdAt in yesterdayStart until todayStart } }
    val weekCount = remember(entries, weekStart) { entries.count { !it.isDeleted && it.createdAt >= weekStart } }
    val monthCount = remember(entries, monthStart) { entries.count { !it.isDeleted && it.createdAt >= monthStart } }
    val allCount = remember(entries) { entries.count { !it.isDeleted } }

    // Filtered data memoized for large datasets
    val filteredEntries = remember(entries, selectedPeriod, customStartDateMillis, customEndDateMillis) {
        entries.filter { !it.isDeleted && isInPeriod(it.createdAt) }
    }
    val filteredVisits = remember(visits, selectedPeriod, customStartDateMillis, customEndDateMillis) {
        visits.filter { !it.isDeleted && isInPeriod(it.createdAt) }
    }

    // Dynamic metrics based on the selected period
    val totalPieces = remember(filteredEntries) { filteredEntries.sumOf { it.pieces } }
    val totalCases = remember(filteredEntries) { filteredEntries.sumOf { it.caseCount } }
    val looseEntries = remember(filteredEntries) { filteredEntries.filter { it.loosePieces > 0 } }
    val totalLoosePcs = remember(looseEntries) { looseEntries.sumOf { it.loosePieces } }
    val grandTotalAmount = remember(filteredEntries) { filteredEntries.sumOf { it.grandTotalWithGst } }
    val activeVisits = remember(filteredVisits) { filteredVisits.filter { it.status.equals("Active", ignoreCase = true) } }

    // Dispatch & Pipeline stats in period
    val pendingCount = remember(filteredEntries) { filteredEntries.count { it.deliveryStatus.equals("Pending", ignoreCase = true) } }
    val packedCount = remember(filteredEntries) { filteredEntries.count { it.deliveryStatus.equals("Packed", ignoreCase = true) } }
    val dispatchedCount = remember(filteredEntries) { filteredEntries.count { it.deliveryStatus.equals("Dispatched", ignoreCase = true) } }
    val deliveredCount = remember(filteredEntries) { filteredEntries.count { it.deliveryStatus.equals("Delivered", ignoreCase = true) } }
    val inTransitCount = pendingCount + packedCount + dispatchedCount

    // Top suppliers in period
    val topSuppliers = remember(filteredEntries) {
        filteredEntries
            .groupBy { it.supplierName.ifBlank { "Direct Purchase" } }
            .map { (name, list) ->
                Triple(name, list.sumOf { it.pieces }, list.sumOf { it.grandTotalWithGst })
            }
            .sortedByDescending { it.second }
            .take(4)
    }

    // Chart data computation
    val chartData = remember(filteredEntries, selectedPeriod, customStartDateMillis, customEndDateMillis) {
        buildTrendChartData(selectedPeriod, filteredEntries, customStartDateMillis, customEndDateMillis)
    }

    // Custom Date Range Picker Dialog
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
        // Modern Flat Top Bar matching Visits & Deliveries
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.size(38.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NavyPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Operations Dashboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = when (selectedPeriod) {
                        "Today" -> "Today's Market Activity"
                        "Yesterday" -> "Yesterday's Performance"
                        "This Week" -> "Last 7 Days Overview"
                        "This Month" -> "Current Month Analytics"
                        "Custom" -> customDateLabel ?: "Custom Date Range"
                        else -> "All-Time Aggregate"
                    },
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onOpenNewVisit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "New Trip",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Time Period Filter Pill Chips (Matching VisitsScreen & DeliveriesScreen)
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Primary KPI Metric Cards (2x2 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardMetricCard(
                            title = "Procured Volume",
                            value = "${String.format("%,d", totalPieces)} Pcs",
                            subtitle = "$totalCases Cases • $totalLoosePcs Loose",
                            accentColor = Color(0xFF059669),
                            icon = Icons.Default.Inventory,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppScreen.REPORTS) }
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

            // 2. Loose Pieces Packing Alert Banner (if applicable)
            if (looseEntries.isNotEmpty()) {
                item {
                    IncompleteCaseBanner(
                        looseCount = totalLoosePcs,
                        ordersCount = looseEntries.size,
                        onMixedPackClick = {
                            val activeVisit = visits.firstOrNull { it.status == "Active" } ?: visits.firstOrNull()
                            if (activeVisit != null) {
                                onOpenVisit(activeVisit)
                            } else {
                                onNavigate(AppScreen.VISITS)
                            }
                        }
                    )
                }
            }

            // 3. Interactive Trend Chart (Volume & Spend)
            item {
                DashboardTrendChart(
                    periodLabel = selectedPeriod,
                    chartData = chartData
                )
            }

            // 4. Dispatch & Transport Status Pipeline
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
                            Column {
                                Text(
                                    text = "Dispatch & Transport Status",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = "Consignment tracking in $selectedPeriod",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "${filteredEntries.size} Orders",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NavyPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Segmented multi-colored pipeline bar
                        val totalOrders = filteredEntries.size
                        if (totalOrders > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                if (pendingCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(pendingCount.toFloat())
                                            .fillMaxHeight()
                                            .background(Color(0xFFD97706))
                                    )
                                }
                                if (packedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(packedCount.toFloat())
                                            .fillMaxHeight()
                                            .background(Color(0xFF2563EB))
                                    )
                                }
                                if (dispatchedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(dispatchedCount.toFloat())
                                            .fillMaxHeight()
                                            .background(Color(0xFF0D9488))
                                    )
                                }
                                if (deliveredCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(deliveredCount.toFloat())
                                            .fillMaxHeight()
                                            .background(Color(0xFF059669))
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2E8F0))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Count Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PipelineStatusBadge(label = "Pending", count = pendingCount, color = Color(0xFFD97706))
                            PipelineStatusBadge(label = "Packed", count = packedCount, color = Color(0xFF2563EB))
                            PipelineStatusBadge(label = "Dispatched", count = dispatchedCount, color = Color(0xFF0D9488))
                            PipelineStatusBadge(label = "Delivered", count = deliveredCount, color = Color(0xFF059669))
                        }
                    }
                }
            }

            // 5. Top Suppliers in Selected Period
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
                                        text = "Top Suppliers in Period",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = NavyPrimary
                                    )
                                }
                                Text(
                                    text = "Share of Volume",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val maxPcs = topSuppliers.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                            topSuppliers.forEach { (name, pcs, amt) ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                            color = NavyPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
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

            // 6. Quick Operations Directory
            item {
                Text(
                    text = "Directory & Operations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NavyPrimary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardQuickLink(
                        title = "Retailers",
                        count = "${customers.size}",
                        icon = Icons.Default.People,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.CUSTOMER_MASTER) }
                    )
                    DashboardQuickLink(
                        title = "Suppliers",
                        count = "${suppliers.size}",
                        icon = Icons.Default.Store,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.SUPPLIER_MASTER) }
                    )
                    DashboardQuickLink(
                        title = "Deliveries",
                        count = "$inTransitCount",
                        icon = Icons.Default.LocalShipping,
                        color = Color(0xFF0D9488),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.DELIVERIES) }
                    )
                    DashboardQuickLink(
                        title = "Orders",
                        count = "${filteredEntries.size}",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        color = Color(0xFFE11D48),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.PURCHASE_ORDERS) }
                    )
                }
            }

            // 7. Recent Market Visits Section
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
                            text = "No market visits recorded in this period. Tap '+ New Trip' to begin or switch time filter.",
                            fontSize = 12.5.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(filteredVisits.take(5)) { visit ->
                    val visitEntries = entries.filter { it.visitId == visit.id }
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

@Composable
fun DashboardTrendChart(
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
                Column {
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
                    Text(
                        text = if (selectedMetric == "pieces") "Periodic volume in pieces" else "Periodic spend in ₹",
                        fontSize = 11.sp,
                        color = TextSecondary
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
                            text = "Tap any bar to inspect",
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

            // Bars representation
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
                        val heightFraction = if (maxVal > 0) (curVal / maxVal).coerceIn(0.04f, 1f) else 0.04f

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

                            Box(
                                modifier = Modifier
                                    .width(if (chartData.size > 7) 14.dp else 22.dp)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(
                                        if (isSelected) {
                                            if (selectedMetric == "pieces") Color(0xFF059669) else NavyPrimary
                                        } else {
                                            if (selectedMetric == "pieces") Color(0xFF34D399).copy(alpha = 0.65f)
                                            else Color(0xFF93C5FD).copy(alpha = 0.65f)
                                        }
                                    )
                            )

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

@Composable
private fun PipelineStatusBadge(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.padding(bottom = 3.dp)
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = color,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@Composable
private fun DashboardQuickLink(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
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
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
            Text(
                text = count,
                fontSize = 10.5.sp,
                color = TextSecondary
            )
        }
    }
}
