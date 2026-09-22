package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.components.CompactSearchBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator
import com.example.util.ShareUtil
import com.example.util.rememberDialogBottomPadding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
fun CustomerOrderReportScreen(
    viewModel: HimatViewModel,
    initialCustomer: CustomerEntity? = null,
    onBack: () -> Unit,
    onOpenOrder: (PurchaseEntryEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val allSuppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()

    var selectedCustomer by remember { mutableStateOf(initialCustomer) }
    var customerSearchQuery by remember { mutableStateOf("") }

    var orderSearchQuery by remember { mutableStateOf("") }
    var selectedDateRange by remember { mutableStateOf("All Time") } // "All Time", "Today", "Yesterday", "This Week", "This Month"
    var selectedSupplierName by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf("All") } // "All", "Pending", "Delivered"
    var isSupplierDropdownExpanded by remember { mutableStateOf(false) }

    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 15

    var isGeneratingPdf by remember { mutableStateOf(false) }

    val safeBottomPadding = rememberDialogBottomPadding(extraPadding = 12.dp, fallbackNavHeight = 48.dp)

    // =========================================================================
    // VIEW A: CUSTOMER PICKER (If no customer is currently selected)
    // =========================================================================
    if (selectedCustomer == null) {
        val filteredCustomers = remember(allCustomers, customerSearchQuery) {
            if (customerSearchQuery.isBlank()) allCustomers
            else {
                val q = customerSearchQuery.trim().lowercase()
                allCustomers.filter {
                    it.firmName.lowercase().contains(q) ||
                    it.name.lowercase().contains(q) ||
                    it.phone.contains(q) ||
                    it.city.lowercase().contains(q)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
        ) {
            // Flat, borderless, clean header matching VisitsScreen & DeliveriesScreen
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
                        text = "Customer Reports",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "Select a retailer to view all orders of all time",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }
            }
                // Compact Search Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    CompactSearchBar(
                        query = customerSearchQuery,
                        onQueryChange = { customerSearchQuery = it },
                        placeholder = "Search customer by firm name, phone or city..."
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                if (filteredCustomers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No customers found", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredCustomers, key = { it.id }) { customer ->
                            val customerVisits = remember(allVisits, customer.id) {
                                allVisits.filter { it.customerId == customer.id }
                            }
                            val customerVisitIds = remember(customerVisits) { customerVisits.map { it.id }.toSet() }
                            val customerOrdersCount = remember(allEntries, customerVisitIds) {
                                allEntries.count { it.visitId in customerVisitIds && !it.isDeleted }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCustomer = customer
                                        currentPage = 1
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF9F1239).copy(alpha = 0.1f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF9F1239), modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = customer.firmName.ifBlank { customer.name },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${customer.city} • ${customer.phone}",
                                                fontSize = 10.5.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Surface(
                                        color = NavyPrimary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(
                                            text = "$customerOrdersCount Orders",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            color = NavyPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
        }
        return
    }

    // =========================================================================
    // VIEW B: ALL-TIME ORDERS STATEMENT FOR SELECTED CUSTOMER
    // =========================================================================
    val customer = selectedCustomer!!

    // Find all visits for this customer
    val customerVisits = remember(allVisits, customer.id) {
        allVisits.filter { it.customerId == customer.id }
    }
    val customerVisitIds = remember(customerVisits) { customerVisits.map { it.id }.toSet() }

    // Find all purchase entries for this customer across all visits
    val allCustomerEntries = remember(allEntries, customerVisitIds) {
        allEntries.filter { it.visitId in customerVisitIds && !it.isDeleted }
    }

    // Filtered by date range, supplier, status, search query
    val filteredCustomerEntries = remember(
        allCustomerEntries, selectedDateRange, selectedSupplierName, selectedStatus, orderSearchQuery
    ) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayStart = todayStart - 86400000L
        val weekStart = todayStart - (6 * 86400000L)
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        allCustomerEntries.filter { entry ->
            val entryTime = entry.createdAt
            val dateMatches = when (selectedDateRange) {
                "Today" -> entryTime >= todayStart
                "Yesterday" -> entryTime in yesterdayStart until todayStart
                "This Week" -> entryTime >= weekStart
                "This Month" -> entryTime >= monthStart
                else -> true
            }
            if (!dateMatches) return@filter false

            if (selectedSupplierName != null && entry.supplierName != selectedSupplierName) {
                return@filter false
            }

            if (selectedStatus != "All" && !entry.deliveryStatus.equals(selectedStatus, ignoreCase = true)) {
                return@filter false
            }

            if (orderSearchQuery.isNotBlank()) {
                val q = orderSearchQuery.trim().lowercase()
                val orderMatches = entry.orderNo.lowercase().contains(q) || "po-${entry.id}".contains(q)
                val supMatches = entry.supplierName.lowercase().contains(q)
                val itemMatches = entry.itemCode.lowercase().contains(q)
                if (!orderMatches && !supMatches && !itemMatches) {
                    return@filter false
                }
            }

            true
        }.sortedByDescending { it.createdAt }
    }

    // Pagination
    val totalOrders = filteredCustomerEntries.size
    val totalPages = maxOf(1, ceil(totalOrders / pageSize.toDouble()).toInt())
    val safePage = currentPage.coerceIn(1, totalPages)
    val startIndex = (safePage - 1) * pageSize
    val pagedEntries = remember(filteredCustomerEntries, safePage) {
        filteredCustomerEntries.drop(startIndex).take(pageSize)
    }

    // Summary calculations
    val totalPieces = remember(filteredCustomerEntries) { filteredCustomerEntries.sumOf { it.pieces } }
    val totalCases = remember(filteredCustomerEntries) { filteredCustomerEntries.sumOf { it.caseCount } }
    val totalAmount = remember(filteredCustomerEntries) { filteredCustomerEntries.sumOf { it.grandTotalWithGst } }
    val totalGst = remember(filteredCustomerEntries) { filteredCustomerEntries.sumOf { it.gstAmount } }

    Scaffold(
        containerColor = Color(0xFFF6F8FB),
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = safeBottomPadding)
                ) {
                    // Pagination Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = safePage > 1,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", modifier = Modifier.size(16.dp))
                            Text("Prev", fontSize = 11.5.sp)
                        }

                        Text("Page $safePage of $totalPages ($totalOrders orders)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                        OutlinedButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = safePage < totalPages,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Next", fontSize = 11.5.sp)
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // PDF Statement Generation Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (filteredCustomerEntries.isEmpty()) {
                                    Toast.makeText(context, "No orders to export for this customer", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isGeneratingPdf = true
                                coroutineScope.launch {
                                    try {
                                        val pdfFile = PdfGenerator.generateCustomerDateRangeReport(
                                            context = context,
                                            customer = customer,
                                            entries = filteredCustomerEntries,
                                            startDate = selectedDateRange,
                                            endDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                                            statusFilter = selectedStatus
                                        )
                                        ShareUtil.sharePdfFile(context, pdfFile, "${customer.firmName} - Order Report")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGeneratingPdf = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9F1239)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            enabled = !isGeneratingPdf,
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Generate PDF Statement", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F8FB))
        ) {
            // Flat, borderless, clean header matching VisitsScreen & DeliveriesScreen
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
                    IconButton(onClick = { selectedCustomer = null }, modifier = Modifier.size(38.dp)) {
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
                        text = customer.firmName.ifBlank { customer.name },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "${customer.city} • $totalOrders orders of all time",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Switch Customer button
                OutlinedButton(
                    onClick = { selectedCustomer = null },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Switch", color = NavyPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Compact KPI Summary Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Orders", fontSize = 9.5.sp, color = TextSecondary)
                    Text("$totalOrders", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF9F1239))
                }
                Column {
                    Text("Total Pieces", fontSize = 9.5.sp, color = TextSecondary)
                    Text("$totalPieces pcs", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NavyPrimary)
                }
                Column {
                    Text("Total Cases", fontSize = 9.5.sp, color = TextSecondary)
                    Text("$totalCases cases", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF047857))
                }
                Column {
                    Text("Total Billed", fontSize = 9.5.sp, color = TextSecondary)
                    Text(PdfGenerator.formatInr(totalAmount), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFFC2410C))
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Compact Filter Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                CompactSearchBar(
                    query = orderSearchQuery,
                    onQueryChange = {
                        orderSearchQuery = it
                        currentPage = 1
                    },
                    placeholder = "Filter customer orders by Order #, Mill, or Item..."
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All Time", "Today", "Yesterday", "This Week", "This Month").forEach { range ->
                        FilterChip(
                            selected = selectedDateRange == range,
                            onClick = {
                                selectedDateRange = range
                                currentPage = 1
                            },
                            label = { Text(range, fontSize = 10.5.sp) },
                            modifier = Modifier.height(28.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    // Supplier Filter Dropdown
                    Box {
                        FilterChip(
                            selected = selectedSupplierName != null,
                            onClick = { isSupplierDropdownExpanded = true },
                            label = { Text(selectedSupplierName ?: "All Suppliers", fontSize = 10.5.sp) },
                            modifier = Modifier.height(28.dp),
                            trailingIcon = {
                                if (selectedSupplierName != null) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(13.dp).clickable { selectedSupplierName = null; currentPage = 1 }
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0D9488),
                                selectedLabelColor = Color.White
                            )
                        )

                        DropdownMenu(
                            expanded = isSupplierDropdownExpanded,
                            onDismissRequest = { isSupplierDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Suppliers (Show All)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                onClick = {
                                    selectedSupplierName = null
                                    currentPage = 1
                                    isSupplierDropdownExpanded = false
                                }
                            )
                            allCustomerEntries.map { it.supplierName }.distinct().sorted().forEach { supName ->
                                DropdownMenuItem(
                                    text = { Text(supName, fontSize = 12.sp) },
                                    onClick = {
                                        selectedSupplierName = supName
                                        currentPage = 1
                                        isSupplierDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    listOf("All", "Pending", "Delivered").forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = {
                                selectedStatus = st
                                currentPage = 1
                            },
                            label = { Text(if (st == "All") "All Status" else st, fontSize = 10.5.sp) },
                            modifier = Modifier.height(28.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF9F1239),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Orders List
            if (pagedEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No orders found for this customer", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pagedEntries, key = { it.id }) { entry ->
                        val formattedDate = remember(entry.createdAt) {
                            if (entry.createdAt > 0L) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entry.createdAt))
                            else entry.expectedDeliveryDate.ifBlank { "Date not recorded" }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenOrder(entry) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = Color(0xFF9F1239).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = entry.orderNo.ifBlank { "PO-${entry.id}" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF9F1239),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(text = entry.supplierName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        if (entry.supplierType.isNotBlank()) {
                                            SupplierTypeBadge(entry.supplierType)
                                        }
                                    }

                                    StatusBadge(status = entry.deliveryStatus)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Item: ${entry.itemCode.ifBlank { "Apparel" }}", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = TextPrimary)
                                        Text(text = "${entry.pieces} pcs (${entry.caseCount} cases, ${entry.loosePieces} loose)", fontSize = 10.5.sp, color = TextSecondary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = PdfGenerator.formatInr(entry.grandTotalWithGst), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF047857))
                                        Text(text = "@ ₹${entry.rate}/pc", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Date: $formattedDate", fontSize = 9.5.sp, color = TextSecondary.copy(alpha = 0.8f))
                                    if (entry.transporter.isNotBlank()) {
                                        Text(text = "Transport: ${entry.transporter}", fontSize = 9.5.sp, color = TextSecondary.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
