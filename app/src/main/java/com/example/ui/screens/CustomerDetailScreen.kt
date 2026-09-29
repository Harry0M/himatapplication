package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import com.example.ui.dialogs.FullScreenImageViewerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import android.widget.Toast
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.PersonRow
import com.example.ui.components.ReferredList
import com.example.ui.components.SectionHeader
import com.example.ui.components.SupplierTypeBadge
import com.example.util.ReferrerTypes
import com.example.util.RelatedLogic
import com.example.util.brandName
import com.example.util.Roles
import com.example.ui.viewmodel.HimatViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: HimatViewModel,
    customer: CustomerEntity,
    onBack: () -> Unit,
    onCreateVisit: () -> Unit,
    onOpenVisit: (VisitEntity) -> Unit = {},
    onEditCustomer: ((CustomerEntity) -> Unit)? = null,
    onDeleteCustomer: ((CustomerEntity) -> Unit)? = null,
    onOpenCustomer: (CustomerEntity) -> Unit = { viewModel.openCustomerDetail(it) },
    onOpenSupplier: (SupplierEntity) -> Unit = { viewModel.openSupplierDetail(it) },
    onOpenEmployee: (EmployeeEntity) -> Unit = { viewModel.openEmployeeDetail(it) }
) {
    val context = LocalContext.current
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val allCheques by viewModel.allChequesPdc.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val allSuppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val allPeople by viewModel.allPeople.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("All") } // "All", "Pending", "Delivered"
    var filterDateRange by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "LAST_7", "THIS_MONTH", "CUSTOM"
    var customDateRange by remember { mutableStateOf(com.example.util.DateRangeFilter()) }
    var showCustomRangePicker by remember { mutableStateOf(false) }
    var filterSupplier by remember { mutableStateOf("All") }
    var filterTransporter by remember { mutableStateOf("All") }
    var activeFilterCategory by remember { mutableStateOf<String?>(null) } // "Date", "Status", "Supplier", "Transporter", or null
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Date calculations for date filtering
    val todayStr = remember {
        val cal = java.util.Calendar.getInstance()
        String.format("%04d-%02d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }
    val last7DaysCutoff = remember {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
        String.format("%04d-%02d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }
    val thisMonthPrefix = remember {
        val cal = java.util.Calendar.getInstance()
        String.format("%04d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
    }

    // All visits for this specific customer (also trips saved without / with a stale customer id)
    val knownCustomerIds = remember(allCustomers) { allCustomers.map { it.id }.toSet() }
    val customerVisits = remember(allVisits, customer, knownCustomerIds) {
        RelatedLogic.tripsOfCustomer(allVisits, customer, knownCustomerIds)
            .sortedByDescending { it.date }
    }
    val customerVisitIds = remember(customerVisits) {
        customerVisits.map { it.id }.toSet()
    }

    // All entries for this customer across all visits
    val customerEntries = remember(allEntries, customerVisitIds) {
        allEntries.filter { it.visitId in customerVisitIds }
    }

    val distinctSuppliers = remember(customerEntries) {
        customerEntries.map { it.supplierName.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val distinctTransporters = remember(customerEntries) {
        customerEntries.map { it.transporter.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val totalVisits = customerVisits.size
    val totalEntriesCount = customerEntries.size
    val totalPieces = customerEntries.sumOf { it.pieces }
    val totalCases = customerEntries.sumOf { it.caseCount }
    val totalLoose = customerEntries.sumOf { it.loosePieces }
    val pendingEntriesCount = customerEntries.count { it.deliveryStatus != "Delivered" }
    val deliveredEntriesCount = customerEntries.count { it.deliveryStatus == "Delivered" }

    // Filtered visits based on search, status, date-wise and entity-wise filters
    val filteredVisits = remember(customerVisits, customerEntries, searchQuery, filterStatus, filterDateRange, customDateRange, filterSupplier, filterTransporter) {
        customerVisits.filter { visit ->
            // 1. Date Range Filter
            val matchesDate = when (filterDateRange) {
                "TODAY" -> visit.date == todayStr
                "LAST_7" -> visit.date >= last7DaysCutoff
                "THIS_MONTH" -> visit.date.startsWith(thisMonthPrefix)
                "CUSTOM" -> customDateRange.matches(visit.date)
                else -> true
            }
            if (!matchesDate) return@filter false

            val visitEntries = customerEntries.filter { it.visitId == visit.id }

            // 2. Delivery Status Filter
            val matchesStatus = when (filterStatus) {
                "Pending" -> visitEntries.any { it.deliveryStatus != "Delivered" }
                "Delivered" -> visitEntries.isNotEmpty() && visitEntries.all { it.deliveryStatus == "Delivered" }
                else -> true
            }
            if (!matchesStatus) return@filter false

            // 3. Entity Filter: Supplier
            val matchesSupplier = if (filterSupplier == "All") true else visitEntries.any { it.supplierName.equals(filterSupplier, ignoreCase = true) }
            if (!matchesSupplier) return@filter false

            // 4. Entity Filter: Transporter
            val matchesTransporter = if (filterTransporter == "All") true else visitEntries.any { it.transporter.equals(filterTransporter, ignoreCase = true) }
            if (!matchesTransporter) return@filter false

            // 5. Search Query Filter
            val matchesSearch = searchQuery.isBlank() ||
                    visit.date.contains(searchQuery, ignoreCase = true) ||
                    visit.visitCode.contains(searchQuery, ignoreCase = true) ||
                    visit.employeeName.contains(searchQuery, ignoreCase = true) ||
                    visitEntries.any {
                        it.itemCode.contains(searchQuery, ignoreCase = true) ||
                                it.supplierName.contains(searchQuery, ignoreCase = true) ||
                                it.transporter.contains(searchQuery, ignoreCase = true) ||
                                it.orderNo.contains(searchQuery, ignoreCase = true)
                    }

            matchesSearch
        }
    }

    val listState = rememberLazyListState()
    var showDateRangeDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = customer.brandName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = listOf(
                                customer.name.trim().takeIf { it.isNotBlank() && !it.equals(customer.brandName(), ignoreCase = true) },
                                customer.customerId.takeIf { it.isNotBlank() }
                            ).filterNotNull().joinToString(" • ").ifBlank { "Customer trips & orders" },
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
                    if (onEditCustomer != null) {
                        FilledTonalIconButton(
                            onClick = { onEditCustomer(customer) },
                            modifier = Modifier
                                .size(38.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Customer",
                                tint = Color(0xFF4338CA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (onDeleteCustomer != null) {
                        FilledTonalIconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete Customer",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    FilledTonalIconButton(
                        onClick = { showDateRangeDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Customer Statement PDF",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))

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
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Profile Details (above Search Bar)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cardless Hero Centered Profile Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 68dp Circular Avatar with Initial fallback
                    val custAvatarPhoto = customer.purchaserPhotoUri.ifBlank { customer.shopPhotoUri }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(2.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.size(68.dp)
                    ) {
                        if (custAvatarPhoto.isNotBlank()) {
                            AsyncImage(
                                model = custAvatarPhoto,
                                contentDescription = customer.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (customer.firmName.ifBlank { customer.name }).take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Display Name: Firm Name
                    val primaryName = customer.firmName.ifBlank { customer.name }
                    Text(
                        text = primaryName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Secondary Subtitle: Contact Person (if firmName differs)
                    if (customer.firmName.isNotBlank() && customer.name.isNotBlank() && customer.firmName != customer.name) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Prop: ${customer.name}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Monospace GSTIN & Badges Row
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (customer.gstin.isNotBlank()) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "GST: ${customer.gstin}",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (customer.religion.isNotBlank()) {
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = customer.religion,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (customer.panNumber.isNotBlank()) {
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "PAN: ${customer.panNumber}",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Action Pills: Phone, Direction/Map, Email
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (customer.phone.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                        context.startActivity(intent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF059669), modifier = Modifier.size(13.dp))
                                    Text(text = customer.phone, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF065F46))
                                }
                            }
                        }

                        val mapAddress = listOfNotNull(
                            customer.shopAddress.takeIf { it.isNotBlank() },
                            customer.address.takeIf { it.isNotBlank() },
                            customer.marketArea.takeIf { it.isNotBlank() },
                            customer.city.takeIf { it.isNotBlank() }
                        ).firstOrNull()

                        if (mapAddress != null) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(mapAddress))
                                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                        context.startActivity(mapIntent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = "Map Directions", tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                                    Text(text = "Direction", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D4ED8))
                                }
                            }
                        }

                        if (customer.email.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${customer.email}"))
                                        context.startActivity(intent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0xFF475569), modifier = Modifier.size(13.dp))
                                    Text(text = "Email", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                }
                            }
                        }
                    }
                }

                // Cardless Inline Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalVisits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Visits / Days",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%,d", totalPieces),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Total Pieces",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$deliveredEntriesCount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "Delivered",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$pendingEntriesCount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingEntriesCount > 0) Color(0xFFDC2626) else Color(0xFF059669)
                        )
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Extended Details (Addresses, Markets, Working Markets, DOB, Religion, Garments, Bank, Transport)
                val fullAddress = customer.shopAddress.ifBlank { customer.address }
                val hasBankDetails = customer.bankName.isNotBlank() || customer.accountNumber.isNotBlank() || customer.ifscCode.isNotBlank()
                val hasExtendedDetails = fullAddress.isNotBlank() ||
                        customer.marketArea.isNotBlank() ||
                        customer.workingMarkets.isNotBlank() ||
                        customer.dob.isNotBlank() ||
                        customer.religion.isNotBlank() ||
                        customer.city.isNotBlank() ||
                        customer.garmentTypes.isNotBlank() ||
                        customer.preferredTransporterName.isNotBlank() ||
                        customer.referredBy.isNotBlank() ||
                        hasBankDetails

                if (hasExtendedDetails) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (fullAddress.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Address: ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(
                                        text = listOfNotNull(
                                            fullAddress.takeIf { it.isNotBlank() },
                                            customer.city.takeIf { it.isNotBlank() },
                                            customer.district.takeIf { it.isNotBlank() },
                                            customer.state.takeIf { it.isNotBlank() },
                                            customer.pincode.takeIf { it.isNotBlank() }
                                        ).joinToString(", "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                            if (customer.marketArea.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Market: ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(customer.marketArea, style = MaterialTheme.typography.labelSmall, color = Color(0xFF0F766E), fontWeight = FontWeight.Medium)
                                }
                            }
                            if (customer.workingMarkets.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Working Mkts: ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(customer.workingMarkets, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (customer.garmentTypes.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Deals In: ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(customer.garmentTypes, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6366F1), fontWeight = FontWeight.Medium)
                                }
                            }
                            if (customer.dob.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("DOB: ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(customer.dob, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E293B))
                                }
                            }
                            if (customer.religion.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Community: ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(customer.religion, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E293B))
                                }
                            }
                            if (hasBankDetails) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Bank A/C: ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    val bankParts = listOfNotNull(
                                        customer.bankName.takeIf { it.isNotBlank() },
                                        customer.accountNumber.takeIf { it.isNotBlank() }?.let { "A/C: $it" },
                                        customer.ifscCode.takeIf { it.isNotBlank() }?.let { "IFSC: $it" }
                                    )
                                    Text(bankParts.joinToString(" • "), style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (customer.preferredTransporterName.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Transporter: ", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    Text(
                                        customer.preferredTransporterName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1E293B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (customer.transportPreference.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Station: ", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    Text(
                                        customer.transportPreference,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF047857),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (customer.referredBy.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Referred by: ", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    Text(customer.referredBy, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // Compact "See KYC Documents" Expandable Option
                val customerDocs = listOfNotNull(
                    customer.aadharPhotoUri.takeIf { it.isNotBlank() }?.let { "Aadhaar (Front)" to it },
                    customer.aadharBackPhotoUri.takeIf { it.isNotBlank() }?.let { "Aadhaar (Back)" to it },
                    customer.gstCertPhotoUri.takeIf { it.isNotBlank() }?.let { "GST Certificate" to it },
                    customer.panPhotoUri.takeIf { it.isNotBlank() }?.let { "PAN Card" to it },
                    customer.shopPhotoUri.takeIf { it.isNotBlank() }?.let { "Shop Front" to it },
                    customer.purchaserPhotoUri.takeIf { it.isNotBlank() }?.let { "Purchaser / Owner" to it },
                    customer.cancelChequePhotoUri.takeIf { it.isNotBlank() }?.let { "Cancelled Cheque" to it }
                )

                if (customerDocs.isNotEmpty()) {
                    var showKycDocs by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showKycDocs = !showKycDocs }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "See KYC Documents",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF065F46)
                                )
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${customerDocs.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showKycDocs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (showKycDocs) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            customerDocs.forEach { (label, url) ->
                                var showPreview by remember { mutableStateOf(false) }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable { showPreview = true }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(114.dp, 80.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFE2E8F0)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = label,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (url.startsWith("http")) "Firebase Cloud" else "Local File",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (url.startsWith("http")) Color(0xFF059669) else Color(0xFF64748B)
                                        )
                                    }
                                }

                                if (showPreview) {
                                    FullScreenImageViewerDialog(
                                        imageUrl = url,
                                        title = "$label • ${customer.firmName.ifBlank { customer.name }}",
                                        onDismiss = { showPreview = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // Compact "See Security Cheques" Expandable Option (Text Data)
                val customerSecurityCheques = remember(allCheques, customer.id, customer.firmName, customer.name) {
                    allCheques.filter { cheque ->
                        !cheque.isDeleted &&
                        (cheque.partyId == customer.id || 
                         (cheque.partyType.equals("Customer", ignoreCase = true) && 
                          (cheque.partyName.equals(customer.firmName, ignoreCase = true) || cheque.partyName.equals(customer.name, ignoreCase = true)))) &&
                        (cheque.notes.contains("Security", ignoreCase = true) || 
                         cheque.notes.contains("Sec CHQ", ignoreCase = true) || 
                         cheque.notes.contains("Guarantee", ignoreCase = true))
                    }
                }

                if (customerSecurityCheques.isNotEmpty()) {
                    var showSecurityCheques by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showSecurityCheques = !showSecurityCheques }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "See Security Cheques",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1E40AF)
                                )
                                Surface(
                                    color = Color(0xFFDBEAFE),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${customerSecurityCheques.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showSecurityCheques) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (showSecurityCheques) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customerSecurityCheques.forEach { cheque ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "CHQ #${cheque.chequeNo}",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF0F172A)
                                                )
                                                val statusColor = when (cheque.status.lowercase()) {
                                                    "cleared" -> Color(0xFF166534) to Color(0xFFDCFCE7)
                                                    "bounced" -> Color(0xFF991B1B) to Color(0xFFFEE2E2)
                                                    "deposited" -> Color(0xFF1E40AF) to Color(0xFFDBEAFE)
                                                    else -> Color(0xFF92400E) to Color(0xFFFEF3C7)
                                                }
                                                Surface(
                                                    color = statusColor.second,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = cheque.status.uppercase(),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = statusColor.first
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "₹${PdfGenerator.formatInr(cheque.amount)}",
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF2563EB)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Bank: ${cheque.bankName.ifBlank { "Not Specified" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF475569),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Due: ${cheque.chequeDate.ifBlank { "Undated" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (cheque.chequeDate == todayStr) Color(0xFFDC2626) else Color(0xFF64748B),
                                                fontWeight = if (cheque.chequeDate == todayStr) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }

                                        if (cheque.notes.isNotBlank()) {
                                            Text(
                                                text = cheque.notes.removePrefix("[Security Cheque]").trim().ifBlank { "Security / Guarantee Cheque" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B),
                                                maxLines = 2,
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

            // Links: which Sub Agent brought this customer, and who this customer referred
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val linkedAgent = remember(allPeople, customer) {
                        allPeople.firstOrNull { p ->
                            Roles.isAgent(p.role) && (p.id == customer.subAgentId ||
                                (customer.subAgentId == null && customer.subAgentName.isNotBlank() && p.name.equals(customer.subAgentName, true)))
                        }
                    }
                    if (linkedAgent != null || customer.subAgentName.isNotBlank()) {
                        PersonRow(
                            title = linkedAgent?.name ?: customer.subAgentName,
                            subtitle = "Sub Agent who brought this customer" +
                                (linkedAgent?.phone?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""),
                            badge = "Sub Agent",
                            badgeColor = Color(0xFFB45309),
                            onClick = linkedAgent?.let { a -> { onOpenEmployee(a) } }
                        )
                    }
                    val referrerNames = listOf(customer.firmName, customer.name)
                    val referredCustomers = remember(allCustomers, customer) {
                        RelatedLogic.referredCustomers(allCustomers, ReferrerTypes.CUSTOMER, customer.id, referrerNames)
                    }
                    val referredSuppliers = remember(allSuppliers, customer) {
                        RelatedLogic.referredSuppliers(allSuppliers, ReferrerTypes.CUSTOMER, customer.id, referrerNames)
                    }
                    val referredPeople = remember(allPeople, customer) {
                        RelatedLogic.referredPeople(allPeople, ReferrerTypes.CUSTOMER, customer.id, referrerNames)
                    }
                    SectionHeader(
                        title = "Referred by this customer",
                        count = referredCustomers.size + referredSuppliers.size + referredPeople.size
                    )
                    ReferredList(
                        customers = referredCustomers,
                        suppliers = referredSuppliers,
                        people = referredPeople,
                        onOpenCustomer = onOpenCustomer,
                        onOpenSupplier = onOpenSupplier,
                        onOpenEmployee = onOpenEmployee,
                        emptyText = "Nobody has been referred by this customer yet."
                    )
                }
            }

            // 2. Fixed/Pinned Search Bar
            stickyHeader {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                        text = "Search date, order #, item, supplier...",
                                        style = MaterialTheme.typography.bodySmall,
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
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Search",
                                        modifier = Modifier.size(11.dp),
                                        tint = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Cascading Filters (Single-line parent chips -> child chips on tap)
            item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Single Row of Parent Filter Chips
                    val isAnyFilterActive = filterDateRange != "ALL" || filterStatus != "All" || filterSupplier != "All" || filterTransporter != "All"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date Parent Chip
                        val isDateActive = filterDateRange != "ALL"
                        val dateLabel = when (filterDateRange) {
                            "TODAY" -> "Today"
                            "LAST_7" -> "7 Days"
                            "THIS_MONTH" -> "This Month"
                            "CUSTOM" -> customDateRange.customLabel.ifBlank { "Custom" }
                            else -> "Date"
                        }
                        FilterChip(
                            selected = activeFilterCategory == "Date" || isDateActive,
                            onClick = {
                                activeFilterCategory = if (activeFilterCategory == "Date") null else "Date"
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isDateActive) "Date: $dateLabel" else "Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )

                        // Status Parent Chip
                        val isStatusActive = filterStatus != "All"
                        FilterChip(
                            selected = activeFilterCategory == "Status" || isStatusActive,
                            onClick = {
                                activeFilterCategory = if (activeFilterCategory == "Status") null else "Status"
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isStatusActive) "Status: $filterStatus" else "Status",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )

                        // Supplier Parent Chip
                        if (distinctSuppliers.size > 1) {
                            val isSupplierActive = filterSupplier != "All"
                            FilterChip(
                                selected = activeFilterCategory == "Supplier" || isSupplierActive,
                                onClick = {
                                    activeFilterCategory = if (activeFilterCategory == "Supplier") null else "Supplier"
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Store,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                },
                                label = {
                                    val supName = if (filterSupplier.length > 10) filterSupplier.take(10) + "…" else filterSupplier
                                    Text(
                                        text = if (isSupplierActive) "Sup: $supName" else "Supplier",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            )
                        }

                        // Transporter Parent Chip
                        if (distinctTransporters.isNotEmpty()) {
                            val isTransporterActive = filterTransporter != "All"
                            FilterChip(
                                selected = activeFilterCategory == "Transporter" || isTransporterActive,
                                onClick = {
                                    activeFilterCategory = if (activeFilterCategory == "Transporter") null else "Transporter"
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 30.dp),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                },
                                label = {
                                    val trName = if (filterTransporter.length > 10) filterTransporter.take(10) + "…" else filterTransporter
                                    Text(
                                        text = if (isTransporterActive) "Trans: $trName" else "Transporter",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            )
                        }

                        // Reset / Clear All Filter Chip
                        if (isAnyFilterActive) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 28.dp)
                                    .clickable {
                                        filterDateRange = "ALL"
                                        filterStatus = "All"
                                        filterSupplier = "All"
                                        filterTransporter = "All"
                                        activeFilterCategory = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Reset Filters",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Reset",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }
                    }

                    // Cascading Child Chips (directly rendered without any enclosing card/box)
                    AnimatedVisibility(visible = activeFilterCategory != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                                    when (activeFilterCategory) {
                                        "Date" -> {
                                            listOf(
                                                "ALL" to "All Dates",
                                                "TODAY" to "Today",
                                                "LAST_7" to "Last 7 Days",
                                                "THIS_MONTH" to "This Month",
                                                "CUSTOM" to (if (filterDateRange == "CUSTOM" && customDateRange.customLabel.isNotBlank()) customDateRange.customLabel else "Custom range")
                                            ).forEach { (rangeKey, label) ->
                                                val isSelected = filterDateRange == rangeKey
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        if (rangeKey == "CUSTOM") showCustomRangePicker = true else filterDateRange = rangeKey
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.defaultMinSize(minHeight = 28.dp),
                                                    label = {
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                        "Status" -> {
                                            listOf(
                                                "All" to "All ($totalVisits Days)",
                                                "Pending" to "Pending ($pendingEntriesCount)",
                                                "Delivered" to "Delivered ($deliveredEntriesCount)"
                                            ).forEach { (statusKey, label) ->
                                                val isSelected = filterStatus == statusKey
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { filterStatus = statusKey },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.defaultMinSize(minHeight = 28.dp),
                                                    label = {
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                        "Supplier" -> {
                                            val allSelected = filterSupplier == "All"
                                            FilterChip(
                                                selected = allSelected,
                                                onClick = { filterSupplier = "All" },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.defaultMinSize(minHeight = 28.dp),
                                                label = {
                                                    Text(
                                                        "All Suppliers",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            )
                                            distinctSuppliers.forEach { sup ->
                                                val isSelected = filterSupplier == sup
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { filterSupplier = sup },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.defaultMinSize(minHeight = 28.dp),
                                                    label = {
                                                        Text(
                                                            sup,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                        "Transporter" -> {
                                            val allSelected = filterTransporter == "All"
                                            FilterChip(
                                                selected = allSelected,
                                                onClick = { filterTransporter = "All" },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.defaultMinSize(minHeight = 28.dp),
                                                label = {
                                                    Text(
                                                        "All Transporters",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            )
                                            distinctTransporters.forEach { tr ->
                                                val isSelected = filterTransporter == tr
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { filterTransporter = tr },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.defaultMinSize(minHeight = 28.dp),
                                                    label = {
                                                        Text(
                                                            tr,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                )
                                            }
                                    }
                                }
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
                        text = "Day Reports (${filteredVisits.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showDateRangeDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFF1D4ED8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF Report", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                    }
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
                        onOpenVisit = { onOpenVisit(visit) },
                        onOpenDayReport = { viewModel.openCustomerReport(visit) },
                        onShareWhatsApp = { viewModel.shareCustomerReportWhatsApp(visit) },
                        onSharePdf = { viewModel.shareCustomerDayReportPdf(visit) },
                        onAdvanceStatus = { entry: PurchaseEntryEntity -> viewModel.advanceEntryDeliveryStatus(entry) },
                        onDeleteVisit = { viewModel.deleteVisit(visit) }
                    )
                }
            }
        }
    }

    if (showCustomRangePicker) {
        com.example.ui.dialogs.CustomDateRangePickerDialog(
            initialStartMillis = customDateRange.customStartMillis,
            initialEndMillis = customDateRange.customEndMillis,
            onDismissRequest = { showCustomRangePicker = false },
            onDateRangeSelected = { start, end, label ->
                customDateRange = com.example.util.DateRangeFilter(
                    preset = com.example.util.DatePreset.CUSTOM,
                    customStartMillis = start,
                    customEndMillis = end,
                    customLabel = label
                )
                filterDateRange = "CUSTOM"
                showCustomRangePicker = false
            }
        )
    }

    if (showDateRangeDialog) {
        CustomerDateRangeDialog(
            customer = customer,
            entries = customerEntries,
            allVisits = allVisits,
            onDismiss = { showDateRangeDialog = false },
            onGeneratePdf = { sDate: String, eDate: String, sFilter: String ->
                showDateRangeDialog = false
                viewModel.shareCustomerDateRangeReportPdf(customer, sDate, eDate, sFilter)
            }
        )
    }

    if (showDeleteConfirmDialog && onDeleteCustomer != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Customer?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = { Text("Are you sure you want to permanently delete \"${customer.firmName.ifBlank { customer.name }}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteCustomer(customer)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DayVisitCard(
    visit: VisitEntity,
    entries: List<PurchaseEntryEntity>,
    onOpenVisit: () -> Unit = {},
    onOpenDayReport: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onSharePdf: () -> Unit,
    onAdvanceStatus: (PurchaseEntryEntity) -> Unit,
    onDeleteVisit: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
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
            // Day Header Row - Clickable to open dedicated visit screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenVisit() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = visit.date,
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenVisit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Dedicated Trip Screen",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Visit",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
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
            }

            // Summary Totals & Action Buttons
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Day Volume: ${String.format("%,d", dayPieces)} pcs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (pendingCount > 0) "$pendingCount items pending • ${entries.size - pendingCount} delivered" else "All ${entries.size} items delivered",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pendingCount > 0) MaterialTheme.colorScheme.secondary else Color(0xFF059669),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Open Dedicated Trip Screen
                    Button(
                        onClick = onOpenVisit,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Open Trip",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Day Report full screen preview
                    OutlinedButton(
                        onClick = onOpenDayReport,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Delete Day Visit?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Are you sure you want to delete visit ${visit.visitCode} on ${visit.date}?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Text(
                            text = "⚠ WARNING: Deleting this trip will also permanently delete all ${entries.size} deliveries/orders recorded on this day.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB91C1C),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteVisit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Trip & Deliveries", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
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

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = PdfGenerator.formatInr(entry.grandTotalWithGst),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${entry.pieces} pcs",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.5.dp)
                    )
                }
            }
        }

        if (entry.transporter.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = entry.transporter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

@Composable
fun CustomerDateRangeDialog(
    customer: CustomerEntity,
    entries: List<PurchaseEntryEntity>,
    allVisits: List<VisitEntity>,
    onDismiss: () -> Unit,
    onGeneratePdf: (startDate: String, endDate: String, statusFilter: String) -> Unit
) {
    val cal = remember { Calendar.getInstance() }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time) }

    val thisMonthStart = remember {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }
    val last30DaysStart = remember {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -30)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }
    val last90DaysStart = remember {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -90)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }

    var selectedPreset by remember { mutableStateOf("LAST_30") }
    var startDate by remember { mutableStateOf(last30DaysStart) }
    var endDate by remember { mutableStateOf(today) }
    var statusFilter by remember { mutableStateOf("All") }

    // [entries] already belong to this customer; look up their trips by id (covers trips with a stale customer id)
    val entryVisitIds = remember(entries) { entries.map { it.visitId }.toSet() }
    val visitMap = remember(allVisits, entryVisitIds) { allVisits.filter { it.id in entryVisitIds }.associateBy { it.id } }

    val filteredEntries = remember(entries, startDate, endDate, statusFilter) {
        entries.filter { entry ->
            val visit = visitMap[entry.visitId]
            val entryDate = if (visit != null && visit.date.isNotBlank()) visit.date else entry.expectedDeliveryDate
            val inDateRange = when {
                startDate.isNotBlank() && endDate.isNotBlank() -> entryDate in startDate..endDate
                startDate.isNotBlank() -> entryDate >= startDate
                endDate.isNotBlank() -> entryDate <= endDate
                else -> true
            }
            if (!inDateRange) return@filter false

            when (statusFilter) {
                "Dispatched" -> entry.deliveryStatus.equals("Dispatched", ignoreCase = true) || entry.deliveryStatus.equals("Delivered", ignoreCase = true)
                "Pending" -> !entry.deliveryStatus.equals("Dispatched", ignoreCase = true) && !entry.deliveryStatus.equals("Delivered", ignoreCase = true)
                else -> true
            }
        }
    }

    val matchPieces = filteredEntries.sumOf { it.pieces }
    val matchAmount = filteredEntries.sumOf { it.grandTotalWithGst }
    val dispatchedCount = filteredEntries.count { it.deliveryStatus.equals("Dispatched", ignoreCase = true) || it.deliveryStatus.equals("Delivered", ignoreCase = true) }
    val pendingCount = filteredEntries.size - dispatchedCount

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Customer Statement PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = customer.firmName.ifBlank { customer.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Quick Date Presets
                Text("Select Period:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "LAST_30" to "Last 30 Days",
                        "THIS_MONTH" to "This Month",
                        "LAST_90" to "Last 90 Days",
                        "ALL" to "All Time"
                    ).forEach { (presetKey, label) ->
                        val isSel = selectedPreset == presetKey
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedPreset = presetKey
                                when (presetKey) {
                                    "LAST_30" -> {
                                        startDate = last30DaysStart
                                        endDate = today
                                    }
                                    "THIS_MONTH" -> {
                                        startDate = thisMonthStart
                                        endDate = today
                                    }
                                    "LAST_90" -> {
                                        startDate = last90DaysStart
                                        endDate = today
                                    }
                                    "ALL" -> {
                                        startDate = ""
                                        endDate = ""
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Custom Date Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {
                            startDate = it
                            selectedPreset = "CUSTOM"
                        },
                        label = { Text("From Date", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("YYYY-MM-DD", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = {
                            endDate = it
                            selectedPreset = "CUSTOM"
                        },
                        label = { Text("To Date", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("YYYY-MM-DD", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Status Filter
                Text("Dispatch Filter:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All" to "All Orders", "Dispatched" to "Dispatched", "Pending" to "Pending").forEach { (filterKey, label) ->
                        val isSel = statusFilter == filterKey
                        FilterChip(
                            selected = isSel,
                            onClick = { statusFilter = filterKey },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Matching Summary Box
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Matching Orders:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Text("${filteredEntries.size} Orders (${matchPieces} Pcs)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dispatch Status:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Text("$dispatchedCount Dispatched • $pendingCount Pending", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = if (pendingCount > 0) Color(0xFFD97706) else Color(0xFF059669))
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Value:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Text(PdfGenerator.formatInr(matchAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = { onGeneratePdf(startDate, endDate, statusFilter) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate PDF", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
