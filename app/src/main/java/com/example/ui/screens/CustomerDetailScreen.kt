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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
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
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.viewmodel.HimatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: HimatViewModel,
    customer: CustomerEntity,
    onBack: () -> Unit,
    onCreateVisit: () -> Unit,
    onOpenVisit: (VisitEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("All") } // "All", "Pending", "Delivered"
    var filterDateRange by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "LAST_7", "THIS_MONTH"
    var filterSupplier by remember { mutableStateOf("All") }
    var filterTransporter by remember { mutableStateOf("All") }
    var activeFilterCategory by remember { mutableStateOf<String?>(null) } // "Date", "Status", "Supplier", "Transporter", or null

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

    // All visits for this specific customer
    val customerVisits = remember(allVisits, customer.id) {
        allVisits.filter { it.customerId == customer.id }
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
    val filteredVisits = remember(customerVisits, customerEntries, searchQuery, filterStatus, filterDateRange, filterSupplier, filterTransporter) {
        customerVisits.filter { visit ->
            // 1. Date Range Filter
            val matchesDate = when (filterDateRange) {
                "TODAY" -> visit.date == todayStr
                "LAST_7" -> visit.date >= last7DaysCutoff
                "THIS_MONTH" -> visit.date.startsWith(thisMonthPrefix)
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
                        if (accumulatedDelta < -25f) {
                            isProfileExpanded = false
                        }
                    } else if (delta > 15f) {
                        if (accumulatedDelta < 0) accumulatedDelta = 0f
                        accumulatedDelta += delta
                        if (accumulatedDelta > 20f) {
                            isProfileExpanded = true
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, searchQuery) {
        if (searchQuery.isNotBlank()) {
            // Keep search bar visible
        } else if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 10) {
            isProfileExpanded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Customer Entries & Day Reports (${customer.customerId})",
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
                    // Cardless Hero Centered Profile Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Hero Centered Profile Picture in Circle
                    val avatarPhoto = customer.purchaserPhotoUri.ifBlank { customer.shopPhotoUri }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(2.dp, Color(0xFFBFDBFE)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                    ) {
                        if (avatarPhoto.isNotBlank()) {
                            AsyncImage(
                                model = avatarPhoto,
                                contentDescription = customer.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                val initials = (customer.firmName.ifBlank { customer.name }).take(2).uppercase()
                                Text(
                                    text = if (initials.isNotBlank()) initials else "CU",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    // Firm Name / Shop Name
                    Text(
                        text = customer.firmName.ifBlank { customer.name },
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Owner Name & Customer ID
                    val ownerSubtitle = buildString {
                        if (customer.name.isNotBlank() && customer.name != customer.firmName) {
                            append("Prop: ${customer.name}")
                        }
                        if (customer.customerId.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append("ID: ${customer.customerId}")
                        }
                        if (customer.city.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(customer.city)
                        }
                    }
                    if (ownerSubtitle.isNotBlank()) {
                        Text(
                            text = ownerSubtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons Row: Phone, Direction (Map), Mail
                    val mapTarget = customer.shopMapLink.ifBlank { customer.shopLocation.ifBlank { customer.address } }
                    val primaryPhone = customer.phone.ifBlank { customer.phone2 }
                    val primaryEmail = customer.email.ifBlank { customer.email2 }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Call Button
                        if (primaryPhone.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$primaryPhone"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = primaryPhone,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                            }
                        }

                        // Direction Button for Map (if available)
                        if (mapTarget.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.clickable {
                                    val uri = if (mapTarget.startsWith("http")) {
                                        Uri.parse(mapTarget)
                                    } else {
                                        Uri.parse("geo:0,0?q=" + Uri.encode(mapTarget))
                                    }
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Location: $mapTarget", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = "Directions", tint = Color(0xFF2563EB), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Directions",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8)
                                    )
                                }
                            }
                        }

                        // Mail Button (if available)
                        if (primaryEmail.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$primaryEmail"))
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0xFF475569), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Mail",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    // GSTIN below
                    val gstin = customer.gstin
                    if (gstin.isNotBlank()) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.8.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = "GSTIN: $gstin",
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }

                    // Linked references (Clean, normal inline text)
                    if (customer.referredBy.isNotBlank() || customer.addedByAgentName.isNotBlank() || customer.preferredTransporterName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (customer.referredBy.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Referred by: ", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(customer.referredBy, fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                }
                            }
                            if (customer.addedByAgentName.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Agent: ", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(customer.addedByAgentName, fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                }
                            }
                            val trans = customer.preferredTransporterName.ifBlank { customer.transportPreference }
                            if (trans.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Transporter: ", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(trans, fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(9.dp))

                    // Cardless Clean Stat Numbers (Inline directly on background, NO cards/surfaces)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$totalVisits",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Total Visits",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(26.dp)
                                .background(Color(0xFFCBD5E1))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$totalEntriesCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F766E)
                            )
                            Text(
                                text = "Total Orders",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "${String.format("%,d", totalPieces)} pcs",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF059669)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(26.dp)
                                .background(Color(0xFFCBD5E1))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$pendingEntriesCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (pendingEntriesCount > 0) Color(0xFFD97706) else Color(0xFF059669)
                            )
                            Text(
                                text = if (pendingEntriesCount > 0) "Pending" else "All Delivered",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(9.dp))

                    // Start New Visit Button (compact, 20% smaller height 38dp)
                    Button(
                        onClick = onCreateVisit,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 36.dp)
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFDE047))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ Start New Visit for ${customer.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                    }

                    // Customer KYC Documents & Cloud Photos
                    val customerDocs = listOfNotNull(
                        customer.aadharPhotoUri.takeIf { it.isNotBlank() }?.let { "Aadhaar Card" to it },
                        customer.gstCertPhotoUri.takeIf { it.isNotBlank() }?.let { "GST Certificate" to it },
                        customer.panPhotoUri.takeIf { it.isNotBlank() }?.let { "PAN Card" to it },
                        customer.shopPhotoUri.takeIf { it.isNotBlank() }?.let { "Shop Front" to it },
                        customer.purchaserPhotoUri.takeIf { it.isNotBlank() }?.let { "Purchaser / Owner" to it },
                        customer.cancelChequePhotoUri.takeIf { it.isNotBlank() }?.let { "Cancelled Cheque" to it }
                    )

                    if (customerDocs.isNotEmpty()) {
                        ElevatedCard(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
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
                                            Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "KYC Documents & Cloud Photos",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFFECFDF5),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                    ) {
                                        Text(
                                            text = "${customerDocs.size} Attached",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF065F46),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

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
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (url.startsWith("http")) "Firebase Cloud" else "Local File",
                                                    fontSize = 9.5.sp,
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
                    }
                }
            }
        }

            // 2. Fixed/Pinned Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
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
                                    text = "Search date, order #, item, supplier...",
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

            // 3. Scrollable List Content
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                                    fontSize = 11.sp,
                                    fontWeight = if (isDateActive || activeFilterCategory == "Date") FontWeight.Bold else FontWeight.Medium
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
                                    fontSize = 11.sp,
                                    fontWeight = if (isStatusActive || activeFilterCategory == "Status") FontWeight.Bold else FontWeight.Medium
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
                                        fontSize = 11.sp,
                                        fontWeight = if (isSupplierActive || activeFilterCategory == "Supplier") FontWeight.Bold else FontWeight.Medium
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
                                        fontSize = 11.sp,
                                        fontWeight = if (isTransporterActive || activeFilterCategory == "Transporter") FontWeight.Bold else FontWeight.Medium
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
                                        fontSize = 10.5.sp,
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
                                                "THIS_MONTH" to "This Month"
                                            ).forEach { (rangeKey, label) ->
                                                val isSelected = filterDateRange == rangeKey
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { filterDateRange = rangeKey },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.defaultMinSize(minHeight = 28.dp),
                                                    label = {
                                                        Text(
                                                            text = label,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                                                            fontSize = 10.5.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                                                        fontSize = 10.5.sp,
                                                        fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal
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
                                                            fontSize = 10.5.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                                                        fontSize = 10.5.sp,
                                                        fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal
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
                                                            fontSize = 10.5.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                        text = "Day-by-Day Entries & Reports (${filteredVisits.size} Days)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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
                        onAdvanceStatus = { entry: PurchaseEntryEntity -> viewModel.advanceEntryDeliveryStatus(entry) }
                    )
                }
            }
        }
    }
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
    onAdvanceStatus: (PurchaseEntryEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
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

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${entry.pieces} pcs",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // Packaging & Case Details
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val packText = if (entry.caseCount > 0 && entry.loosePieces > 0) {
                "${entry.caseCount} Cases (${entry.caseCount * entry.caseSize} pcs) + ${entry.loosePieces} Loose"
            } else if (entry.caseCount > 0) {
                "${entry.caseCount} Cases (${entry.pieces} pcs)"
            } else {
                "${entry.loosePieces} Loose pcs"
            }
            Text(
                text = "$packText (Size: ${entry.caseSize})",
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

        if (!entry.mixedPackNote.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.mixedPackNote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Medium
            )
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
