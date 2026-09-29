package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ChequePdcEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.SupplierEntity
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChequePdcScreen(
    viewModel: HimatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Live data
    val cheques by viewModel.allChequesPdc.collectAsStateWithLifecycle()
    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.visibleSuppliers.collectAsStateWithLifecycle()

    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedPartyTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "CUSTOMER", "SUPPLIER"
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "DUE_TODAY", "UPCOMING_PDC", "PENDING", "DEPOSITED", "CLEARED", "BOUNCED"
    var selectedPartyId by remember { mutableStateOf<Long?>(null) } // Specific Customer or Supplier ID filter
    var selectedPartyName by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    var isHeaderVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            private var accumulatedDelta = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    if (delta < 0) {
                        // Scrolling DOWN (swiping up) -> collapse header to let list fill entire screen
                        if (accumulatedDelta > 0) accumulatedDelta = 0f
                        accumulatedDelta += delta
                        if (accumulatedDelta < -20f) {
                            isHeaderVisible = false
                        }
                    } else if (delta > 0) {
                        // Scrolling UP (pulling down) -> expand header back
                        if (accumulatedDelta < 0) accumulatedDelta = 0f
                        accumulatedDelta += delta
                        if (accumulatedDelta > 15f) {
                            isHeaderVisible = true
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Automatically expand header when at top of list or actively searching
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, searchQuery) {
        if (searchQuery.isNotBlank() || (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 10)) {
            isHeaderVisible = true
        }
    }

    // Dialog state
    var showAddEditDialog by remember { mutableStateOf(false) }
    var chequeToEdit by remember { mutableStateOf<ChequePdcEntity?>(null) }
    var chequeToDelete by remember { mutableStateOf<ChequePdcEntity?>(null) }

    // Calculations
    val activeCheques = remember(cheques) {
        cheques.filter { !it.isDeleted }
    }

    val dueTodayCheques = remember(activeCheques, today) {
        activeCheques.filter {
            it.chequeDate == today && (it.status.equals("Pending", ignoreCase = true) || it.status.equals("Due Today", ignoreCase = true))
        }
    }

    val upcomingPdcCheques = remember(activeCheques, today) {
        activeCheques.filter {
            it.chequeDate > today && it.status.equals("Pending", ignoreCase = true)
        }
    }

    val depositedCheques = remember(activeCheques) {
        activeCheques.filter { it.status.equals("Deposited", ignoreCase = true) }
    }

    val clearedCheques = remember(activeCheques) {
        activeCheques.filter { it.status.equals("Cleared", ignoreCase = true) }
    }

    val bouncedCheques = remember(activeCheques) {
        activeCheques.filter { it.status.equals("Bounced", ignoreCase = true) }
    }

    // Filtered list
    val filteredCheques = remember(activeCheques, searchQuery, selectedPartyTypeFilter, selectedStatusFilter, selectedPartyId, today) {
        activeCheques.filter { cheque ->
            // Search query filter
            val query = searchQuery.trim().lowercase()
            val matchesQuery = query.isBlank() ||
                    cheque.chequeNo.lowercase().contains(query) ||
                    cheque.bankName.lowercase().contains(query) ||
                    cheque.partyName.lowercase().contains(query) ||
                    cheque.notes.lowercase().contains(query) ||
                    cheque.amount.toString().contains(query)

            // Party Type filter
            val matchesPartyType = when (selectedPartyTypeFilter) {
                "CUSTOMER" -> cheque.partyType.equals("Customer", ignoreCase = true)
                "SUPPLIER" -> cheque.partyType.equals("Supplier", ignoreCase = true)
                else -> true
            }

            // Specific Party ID filter (customer-wise / supplier-wise)
            val matchesPartyId = selectedPartyId == null || cheque.partyId == selectedPartyId

            // Status filter
            val matchesStatus = when (selectedStatusFilter) {
                "DUE_TODAY" -> cheque.chequeDate == today && (cheque.status.equals("Pending", ignoreCase = true) || cheque.status.equals("Due Today", ignoreCase = true))
                "UPCOMING_PDC" -> cheque.chequeDate > today && cheque.status.equals("Pending", ignoreCase = true)
                "PENDING" -> cheque.status.equals("Pending", ignoreCase = true)
                "DEPOSITED" -> cheque.status.equals("Deposited", ignoreCase = true)
                "CLEARED" -> cheque.status.equals("Cleared", ignoreCase = true)
                "BOUNCED" -> cheque.status.equals("Bounced", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesPartyType && matchesPartyId && matchesStatus
        }.sortedWith(compareBy<ChequePdcEntity> {
            // Sort due today first, then pending upcoming ascending by date, then others
            when {
                it.chequeDate == today && (it.status.equals("Pending", ignoreCase = true) || it.status.equals("Due Today", ignoreCase = true)) -> 0
                it.status.equals("Pending", ignoreCase = true) -> 1
                it.status.equals("Deposited", ignoreCase = true) -> 2
                else -> 3
            }
        }.thenBy { it.chequeDate })
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(
                                text = "Cheques & PDC",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isHeaderVisible) {
                                Text(
                                    text = "Post-Dated & Regular Cheques Register",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "${filteredCheques.size} Cheques • ₹${PdfGenerator.formatInr(filteredCheques.sumOf { it.amount })}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = { isHeaderVisible = !isHeaderVisible },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isHeaderVisible) Icons.Default.KeyboardArrowUp else Icons.Default.Tune,
                                contentDescription = if (isHeaderVisible) "Collapse Summary" else "Expand Summary",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = {
                                chequeToEdit = null
                                showAddEditDialog = true
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 28.dp, minWidth = 1.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("New", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    chequeToEdit = null
                    showAddEditDialog = true
                },
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Cheque", modifier = Modifier.size(18.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .padding(innerPadding)
        ) {
            // Collapsible Header Section (KPI Summary, Alert, Search, Filters)
            AnimatedVisibility(
                visible = isHeaderVisible,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 200)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 180)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // KPI Summary Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                KpiCard(
                    title = "Due Today",
                    count = dueTodayCheques.size,
                    amount = dueTodayCheques.sumOf { it.amount },
                    color = if (dueTodayCheques.isNotEmpty()) Color(0xFFDC2626) else Color(0xFF6B7280),
                    isSelected = selectedStatusFilter == "DUE_TODAY",
                    icon = Icons.Default.NotificationsActive,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == "DUE_TODAY") "ALL" else "DUE_TODAY"
                    }
                )
                KpiCard(
                    title = "Upcoming PDC",
                    count = upcomingPdcCheques.size,
                    amount = upcomingPdcCheques.sumOf { it.amount },
                    color = Color(0xFF2563EB),
                    isSelected = selectedStatusFilter == "UPCOMING_PDC",
                    icon = Icons.Default.CalendarToday,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == "UPCOMING_PDC") "ALL" else "UPCOMING_PDC"
                    }
                )
                KpiCard(
                    title = "Deposited",
                    count = depositedCheques.size,
                    amount = depositedCheques.sumOf { it.amount },
                    color = Color(0xFFD97706),
                    isSelected = selectedStatusFilter == "DEPOSITED",
                    icon = Icons.Default.HourglassTop,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == "DEPOSITED") "ALL" else "DEPOSITED"
                    }
                )
                KpiCard(
                    title = "Cleared",
                    count = clearedCheques.size,
                    amount = clearedCheques.sumOf { it.amount },
                    color = Color(0xFF16A34A),
                    isSelected = selectedStatusFilter == "CLEARED",
                    icon = Icons.Default.CheckCircle,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == "CLEARED") "ALL" else "CLEARED"
                    }
                )
                if (bouncedCheques.isNotEmpty()) {
                    KpiCard(
                        title = "Bounced",
                        count = bouncedCheques.size,
                        amount = bouncedCheques.sumOf { it.amount },
                        color = Color(0xFF991B1B),
                        isSelected = selectedStatusFilter == "BOUNCED",
                        icon = Icons.Default.ErrorOutline,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == "BOUNCED") "ALL" else "BOUNCED"
                        }
                    )
                }
            }

            // DUE TODAY ALERT BANNER
            if (dueTodayCheques.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "DEPOSIT TODAY: ${dueTodayCheques.size} Cheque${if (dueTodayCheques.size > 1) "s" else ""}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B),
                                    fontSize = 10.5.sp
                                )
                                Text(
                                    text = "Total Value: ₹${PdfGenerator.formatInr(dueTodayCheques.sumOf { it.amount })} due today for deposit!",
                                    color = Color(0xFF7F1D1D),
                                    fontSize = 9.sp
                                )
                            }
                        }
                        Button(
                            onClick = { selectedStatusFilter = "DUE_TODAY" },
                            modifier = Modifier.defaultMinSize(minHeight = 24.dp, minWidth = 1.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text("View", fontSize = 9.5.sp, color = Color.White)
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                textStyle = TextStyle(fontSize = 11.5.sp),
                placeholder = { Text("Search by cheque no, bank, party name...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Party Type & Party Picker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Party Type Chips
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedPartyTypeFilter == "ALL",
                        onClick = {
                            selectedPartyTypeFilter = "ALL"
                            selectedPartyId = null
                            selectedPartyName = ""
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 26.dp),
                        label = { Text("All Parties", fontSize = 9.5.sp) },
                        shape = RoundedCornerShape(6.dp)
                    )
                    FilterChip(
                        selected = selectedPartyTypeFilter == "CUSTOMER",
                        onClick = {
                            selectedPartyTypeFilter = "CUSTOMER"
                            selectedPartyId = null
                            selectedPartyName = ""
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 26.dp),
                        label = { Text("Customers", fontSize = 9.5.sp) },
                        shape = RoundedCornerShape(6.dp)
                    )
                    FilterChip(
                        selected = selectedPartyTypeFilter == "SUPPLIER",
                        onClick = {
                            selectedPartyTypeFilter = "SUPPLIER"
                            selectedPartyId = null
                            selectedPartyName = ""
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 26.dp),
                        label = { Text("Suppliers", fontSize = 9.5.sp) },
                        shape = RoundedCornerShape(6.dp)
                    )
                }

                // Party-Wise Dropdown Selector Button
                PartySelectorDropdown(
                    partyType = selectedPartyTypeFilter,
                    customers = customers,
                    suppliers = suppliers,
                    selectedPartyId = selectedPartyId,
                    selectedPartyName = selectedPartyName,
                    onSelect = { id, name ->
                        selectedPartyId = id
                        selectedPartyName = name
                    },
                    onClear = {
                        selectedPartyId = null
                        selectedPartyName = ""
                    }
                )
            }

            // Active Customer/Supplier Filter Indicator
            if (selectedPartyId != null && selectedPartyName.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtered by: $selectedPartyName",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear filter",
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable {
                                        selectedPartyId = null
                                        selectedPartyName = ""
                                    },
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "ALL" to "All Status",
                    "DUE_TODAY" to "Due Today",
                    "UPCOMING_PDC" to "Upcoming PDC",
                    "PENDING" to "Pending",
                    "DEPOSITED" to "Deposited",
                    "CLEARED" to "Cleared",
                    "BOUNCED" to "Bounced"
                ).forEach { (statusKey, label) ->
                    FilterChip(
                        selected = selectedStatusFilter == statusKey,
                        onClick = { selectedStatusFilter = statusKey },
                        modifier = Modifier.defaultMinSize(minHeight = 26.dp),
                        label = { Text(label, fontSize = 9.5.sp) },
                        shape = RoundedCornerShape(6.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (statusKey == "DUE_TODAY") Color(0xFFDC2626) else MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = if (statusKey == "DUE_TODAY") Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }

            // Results count & Quick Summary / Expand Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .clickable { isHeaderVisible = !isHeaderVisible }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${filteredCheques.size} Cheque${if (filteredCheques.size != 1) "s" else ""}",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = " • Total: ₹${PdfGenerator.formatInr(filteredCheques.sumOf { it.amount })}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!isHeaderVisible && (selectedStatusFilter != "ALL" || selectedPartyTypeFilter != "ALL" || selectedPartyId != null || searchQuery.isNotBlank())) {
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Filtered",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isHeaderVisible) "Hide Summary" else "Show Filters & Stats",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = if (isHeaderVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Cheque List
            if (filteredCheques.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No Cheques Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedPartyId != null || selectedStatusFilter != "ALL")
                                "Try changing filters or search terms"
                            else
                                "Tap '+ New Cheque' to record your first cheque",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 72.dp, top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCheques, key = { it.id }) { cheque ->
                        ChequeCard(
                            cheque = cheque,
                            today = today,
                            onEdit = {
                                chequeToEdit = cheque
                                showAddEditDialog = true
                            },
                            onDelete = {
                                chequeToDelete = cheque
                            },
                            onUpdateStatus = { newStatus ->
                                viewModel.updateChequePdcStatus(cheque.id, newStatus) {
                                    Toast.makeText(context, "Cheque #${cheque.chequeNo} marked as $newStatus", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditChequeDialog(
            cheque = chequeToEdit,
            customers = customers,
            suppliers = suppliers,
            onDismiss = {
                showAddEditDialog = false
                chequeToEdit = null
            },
            onSave = { id, chNo, bank, amount, date, partyType, partyId, partyName, status, notes ->
                viewModel.saveChequePdc(
                    id = id,
                    chequeNo = chNo,
                    bankName = bank,
                    amount = amount,
                    chequeDate = date,
                    partyType = partyType,
                    partyId = partyId,
                    partyName = partyName,
                    status = status,
                    notes = notes
                ) {
                    Toast.makeText(context, if (id == 0L) "Cheque added successfully" else "Cheque updated", Toast.LENGTH_SHORT).show()
                    showAddEditDialog = false
                    chequeToEdit = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (chequeToDelete != null) {
        AlertDialog(
            onDismissRequest = { chequeToDelete = null },
            title = { Text("Delete Cheque", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete Cheque #${chequeToDelete?.chequeNo} from ${chequeToDelete?.partyName} of ₹${chequeToDelete?.amount?.let { PdfGenerator.formatInr(it) }}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = chequeToDelete
                        if (toDel != null) {
                            viewModel.deleteChequePdc(toDel.id) {
                                Toast.makeText(context, "Cheque deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                        chequeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { chequeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// KPI Metric Card
// -------------------------------------------------------------
@Composable
private fun KpiCard(
    title: String,
    count: Int,
    amount: Double,
    color: Color,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) color else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(96.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
                }
                Text(
                    text = "$count",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = color
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = title,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${PdfGenerator.formatInr(amount)}",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -------------------------------------------------------------
// Cheque Card Component
// -------------------------------------------------------------
@Composable
private fun ChequeCard(
    cheque: ChequePdcEntity,
    today: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    val isDueToday = cheque.chequeDate == today && (cheque.status.equals("Pending", ignoreCase = true) || cheque.status.equals("Due Today", ignoreCase = true))
    val isOverdue = cheque.chequeDate < today && cheque.status.equals("Pending", ignoreCase = true)
    val isPdc = cheque.chequeDate > today && cheque.status.equals("Pending", ignoreCase = true)

    val cardBorderColor = when {
        isDueToday -> Color(0xFFDC2626)
        isOverdue -> Color(0xFFEA580C)
        cheque.status.equals("Cleared", ignoreCase = true) -> Color(0xFF16A34A)
        cheque.status.equals("Deposited", ignoreCase = true) -> Color(0xFF2563EB)
        cheque.status.equals("Bounced", ignoreCase = true) -> Color(0xFF991B1B)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDueToday) Color(0xFFFFF1F2) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(if (isDueToday) 1.5.dp else 1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(if (isDueToday) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
            // Header: Cheque No + Badges + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CH N: ${cheque.chequeNo}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(6.dp))
                        // Party Type Chip
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = if (cheque.partyType.equals("Customer", ignoreCase = true)) Color(0xFFE0E7FF) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = cheque.partyType.uppercase(),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cheque.partyType.equals("Customer", ignoreCase = true)) Color(0xFF3730A3) else Color(0xFF92400E)
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = cheque.partyName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Amount
                Text(
                    text = "₹${PdfGenerator.formatInr(cheque.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = if (isDueToday) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(5.dp))

            // Bank Name & Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = cheque.bankName,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = if (isDueToday) Color(0xFFDC2626) else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = cheque.chequeDate,
                        fontSize = 10.sp,
                        fontWeight = if (isDueToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isDueToday) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status Banner / Alert
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Deposit Date Status Tag
                when {
                    isDueToday -> {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color(0xFFDC2626)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "DUE TODAY FOR DEPOSIT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    isPdc -> {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = "PDC (${cheque.chequeDate})",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }
                    isOverdue -> {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color(0xFFFFF7ED)
                        ) {
                            Text(
                                text = "OVERDUE (Date Passed)",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C)
                            )
                        }
                    }
                    cheque.status.equals("Deposited", ignoreCase = true) -> {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "DEPOSITED (Pending Clearance)",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                    cheque.status.equals("Cleared", ignoreCase = true) -> {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "CLEARED ✓ ${if (cheque.clearedDate.isNotBlank()) "(${cheque.clearedDate})" else ""}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                    cheque.status.equals("Bounced", ignoreCase = true) -> {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "BOUNCED ✕",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }

                // Edit / Delete icons
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(13.dp), tint = Color(0xFFDC2626))
                    }
                }
            }

            // Notes if any
            if (cheque.notes.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Note: ${cheque.notes}",
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Status Transition Actions
            HorizontalDivider(modifier = Modifier.padding(vertical = 5.dp), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cheque.status.equals("Pending", ignoreCase = true) || cheque.status.equals("Due Today", ignoreCase = true)) {
                    Button(
                        onClick = { onUpdateStatus("Deposited") },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 26.dp, minWidth = 1.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Mark Deposited", fontSize = 9.5.sp)
                    }
                    Button(
                        onClick = { onUpdateStatus("Cleared") },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 26.dp, minWidth = 1.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Mark Cleared", fontSize = 9.5.sp)
                    }
                } else if (cheque.status.equals("Deposited", ignoreCase = true)) {
                    Button(
                        onClick = { onUpdateStatus("Cleared") },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 26.dp, minWidth = 1.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Mark Cleared", fontSize = 9.5.sp)
                    }
                    OutlinedButton(
                        onClick = { onUpdateStatus("Bounced") },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 26.dp, minWidth = 1.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = BorderStroke(1.dp, Color(0xFFDC2626)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Bounced", fontSize = 9.5.sp)
                    }
                } else if (cheque.status.equals("Bounced", ignoreCase = true) || cheque.status.equals("Cleared", ignoreCase = true)) {
                    OutlinedButton(
                        onClick = { onUpdateStatus("Pending") },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 26.dp, minWidth = 1.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Revert to Pending", fontSize = 9.5.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Customer / Supplier Dropdown Selector
// -------------------------------------------------------------
@Composable
private fun PartySelectorDropdown(
    partyType: String,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    selectedPartyId: Long?,
    selectedPartyName: String,
    onSelect: (Long, String) -> Unit,
    onClear: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.defaultMinSize(minHeight = 26.dp, minWidth = 1.dp),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
            border = BorderStroke(1.dp, if (selectedPartyId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                text = if (selectedPartyName.isNotBlank())
                    selectedPartyName.take(12) + (if (selectedPartyName.length > 12) ".." else "")
                else
                    "Select Party",
                fontSize = 9.5.sp
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(260.dp)
        ) {
            DropdownMenuItem(
                text = { Text("All Parties (Clear)", fontWeight = FontWeight.Bold) },
                onClick = {
                    onClear()
                    expanded = false
                }
            )
            HorizontalDivider()

            if (partyType == "ALL" || partyType == "CUSTOMER") {
                Text(
                    text = "CUSTOMERS",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                customers.forEach { cust ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(cust.firmName.ifBlank { cust.name }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                if (cust.city.isNotBlank()) {
                                    Text(cust.city, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        },
                        onClick = {
                            onSelect(cust.id, cust.firmName.ifBlank { cust.name })
                            expanded = false
                        }
                    )
                }
            }

            if (partyType == "ALL" || partyType == "SUPPLIER") {
                HorizontalDivider()
                Text(
                    text = "SUPPLIERS",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706)
                )
                suppliers.forEach { sup ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(sup.brand.ifBlank { sup.name }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                if (sup.city.isNotBlank()) {
                                    Text(sup.city, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        },
                        onClick = {
                            onSelect(sup.id, sup.brand.ifBlank { sup.name })
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Add / Edit Cheque Dialog
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditChequeDialog(
    cheque: ChequePdcEntity?,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        chequeNo: String,
        bankName: String,
        amount: Double,
        chequeDate: String,
        partyType: String,
        partyId: Long,
        partyName: String,
        status: String,
        notes: String
    ) -> Unit
) {
    val isEdit = cheque != null
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var chequeNo by remember { mutableStateOf(cheque?.chequeNo ?: "") }
    var bankName by remember { mutableStateOf(cheque?.bankName ?: "") }
    var amountText by remember { mutableStateOf(cheque?.amount?.let { if (it > 0) it.toString() else "" } ?: "") }
    var chequeDate by remember { mutableStateOf(cheque?.chequeDate ?: today) }
    var partyType by remember { mutableStateOf(cheque?.partyType ?: "Customer") }
    var partyId by remember { mutableStateOf(cheque?.partyId ?: 0L) }
    var partyName by remember { mutableStateOf(cheque?.partyName ?: "") }
    var status by remember { mutableStateOf(cheque?.status ?: "Pending") }
    var notes by remember { mutableStateOf(cheque?.notes ?: "") }

    var partyDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var partySearchQuery by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Cheque" else "Record New Cheque (PDC)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Party Type Toggle: Customer vs Supplier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = partyType.equals("Customer", ignoreCase = true),
                        onClick = {
                            if (!partyType.equals("Customer", ignoreCase = true)) {
                                partyType = "Customer"
                                partyId = 0L
                                partyName = ""
                            }
                        },
                        label = { Text("Customer") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = partyType.equals("Supplier", ignoreCase = true),
                        onClick = {
                            if (!partyType.equals("Supplier", ignoreCase = true)) {
                                partyType = "Supplier"
                                partyId = 0L
                                partyName = ""
                            }
                        },
                        label = { Text("Supplier") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Party Selector (Customer or Supplier)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = partyName,
                        onValueChange = {
                            partyName = it
                            partyDropdownExpanded = true
                        },
                        label = { Text("Select $partyType *") },
                        placeholder = { Text("Choose $partyType name") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { partyDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    DropdownMenu(
                        expanded = partyDropdownExpanded,
                        onDismissRequest = { partyDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        if (partyType.equals("Customer", ignoreCase = true)) {
                            val filteredCusts = if (partyName.isBlank()) customers else customers.filter {
                                it.firmName.contains(partyName, ignoreCase = true) || it.name.contains(partyName, ignoreCase = true)
                            }
                            filteredCusts.take(15).forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(cust.firmName.ifBlank { cust.name }, fontWeight = FontWeight.Medium)
                                            if (cust.city.isNotBlank()) Text(cust.city, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    },
                                    onClick = {
                                        partyId = cust.id
                                        partyName = cust.firmName.ifBlank { cust.name }
                                        partyDropdownExpanded = false
                                    }
                                )
                            }
                        } else {
                            val filteredSupps = if (partyName.isBlank()) suppliers else suppliers.filter {
                                it.brand.contains(partyName, ignoreCase = true) || it.name.contains(partyName, ignoreCase = true)
                            }
                            filteredSupps.take(15).forEach { sup ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(sup.brand.ifBlank { sup.name }, fontWeight = FontWeight.Medium)
                                            if (sup.city.isNotBlank()) Text(sup.city, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    },
                                    onClick = {
                                        partyId = sup.id
                                        partyName = sup.brand.ifBlank { sup.name }
                                        partyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Cheque Number (CH N)
                OutlinedTextField(
                    value = chequeNo,
                    onValueChange = { chequeNo = it },
                    label = { Text("Cheque No (CH N) *") },
                    placeholder = { Text("e.g. 000123") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Bank Name
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name *") },
                    placeholder = { Text("e.g. HDFC Bank, SBI, ICICI") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Cheque Amount (₹) *") },
                    placeholder = { Text("e.g. 50000") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Cheque / Deposit Date
                OutlinedTextField(
                    value = chequeDate,
                    onValueChange = { chequeDate = it },
                    label = { Text("Deposit Date (YYYY-MM-DD) *") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { chequeDate = today }) {
                            Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )

                // Status Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { statusDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    )

                    DropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        listOf("Pending", "Deposited", "Cleared", "Bounced").forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    status = s
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Remarks (Optional)") },
                    placeholder = { Text("e.g. Given against Bill #105") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (chequeNo.isBlank()) return@Button
                    if (bankName.isBlank()) return@Button
                    if (amt <= 0.0) return@Button
                    if (chequeDate.isBlank()) return@Button
                    if (partyName.isBlank()) return@Button

                    onSave(
                        cheque?.id ?: 0L,
                        chequeNo.trim(),
                        bankName.trim(),
                        amt,
                        chequeDate.trim(),
                        partyType,
                        partyId,
                        partyName.trim(),
                        status,
                        notes.trim()
                    )
                },
                shape = RoundedCornerShape(8.dp),
                enabled = chequeNo.isNotBlank() && bankName.isNotBlank() && amountText.toDoubleOrNull() != null && partyName.isNotBlank()
            ) {
                Text(if (isEdit) "Update" else "Save Cheque")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
