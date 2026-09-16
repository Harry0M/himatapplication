package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
    onCreateVisit: () -> Unit
) {
    val context = LocalContext.current
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("All") } // "All", "Pending", "Delivered"
    var filterDateRange by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "LAST_7", "THIS_MONTH"
    var filterSupplier by remember { mutableStateOf("All") }
    var filterTransporter by remember { mutableStateOf("All") }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Header Profile Card
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
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${customer.city}${if (customer.address.isNotBlank()) " • ${customer.address}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        // Badges: GSTIN, Credit Days, Credit Limit, Shop Outlets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (customer.gstin.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "GSTIN: ${customer.gstin}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Credit: ${customer.creditDays} Days",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (customer.shopCount > 1) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                ) {
                                    Text(
                                        text = "🏪 ${customer.shopCount} Outlets",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Contact Phones (up to 5 phones with 1-click dial chips)
                        val customerPhones = listOfNotNull(
                            customer.phone.takeIf { it.isNotBlank() },
                            customer.phone2.takeIf { it.isNotBlank() },
                            customer.phone3.takeIf { it.isNotBlank() },
                            customer.phone4.takeIf { it.isNotBlank() },
                            customer.phone5.takeIf { it.isNotBlank() }
                        )
                        if (customerPhones.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                customerPhones.forEachIndexed { idx, p ->
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
                        val customerEmails = listOfNotNull(
                            customer.email.takeIf { it.isNotBlank() },
                            customer.email2.takeIf { it.isNotBlank() }
                        )
                        if (customerEmails.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                customerEmails.forEach { em ->
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

                        // Extended details card: addresses, locations, markets, categories, referredBy
                        val hasExtendedDetails = customer.homeAddress.isNotBlank() ||
                                customer.shopLocation.isNotBlank() ||
                                customer.personalLocation.isNotBlank() ||
                                customer.shopLocations.isNotBlank() ||
                                customer.markets.isNotBlank() ||
                                customer.preferredCategories.isNotBlank() ||
                                customer.referredBy.isNotBlank()

                        if (hasExtendedDetails) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (customer.shopLocation.isNotBlank() || customer.address.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("Shop: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            Text(
                                                text = customer.shopLocation.ifBlank { customer.address },
                                                fontSize = 11.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                        }
                                    }
                                    if (customer.homeAddress.isNotBlank() || customer.personalLocation.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("Home: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            Text(
                                                text = listOfNotNull(customer.homeAddress.takeIf { it.isNotBlank() }, customer.personalLocation.takeIf { it.isNotBlank() }).joinToString(" • "),
                                                fontSize = 11.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                        }
                                    }
                                    if (customer.shopLocations.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("Outlets: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            Text(customer.shopLocations, fontSize = 11.sp, color = Color(0xFF1E293B))
                                        }
                                    }
                                    if (customer.markets.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("Markets: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            Text(customer.markets, fontSize = 11.sp, color = Color(0xFF0F766E), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    if (customer.preferredCategories.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("Products: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            Text(customer.preferredCategories, fontSize = 11.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    if (customer.referredBy.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("Referred by: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            Text(customer.referredBy, fontSize = 11.sp, color = Color(0xFF1E293B))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Operational Metric Statistics Cards (2x2 Grid - NO Payment Figures)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CustomerSummaryChip(
                                title = "Total Visits",
                                value = "$totalVisits Days",
                                subtitle = "$totalEntriesCount Total Orders",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            CustomerSummaryChip(
                                title = "Total Volume",
                                value = "${String.format("%,d", totalPieces)} pcs",
                                subtitle = "$deliveredEntriesCount Fulfilled Orders",
                                color = Color(0xFF059669),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CustomerSummaryChip(
                                title = "Ongoing Deliveries",
                                value = "$pendingEntriesCount Pending",
                                subtitle = "$deliveredEntriesCount Fulfilled",
                                color = if (pendingEntriesCount > 0) MaterialTheme.colorScheme.secondary else Color(0xFF059669),
                                modifier = Modifier.weight(1f)
                            )
                            CustomerSummaryChip(
                                title = "Packaging Volume",
                                value = "$totalCases Cases",
                                subtitle = "$totalLoose Loose Pieces",
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onCreateVisit,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp)
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+ Start New Visit for ${customer.name}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
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
                item {
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
                                        Dialog(onDismissRequest = { showPreview = false }) {
                                            Card(
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        IconButton(onClick = { showPreview = false }, modifier = Modifier.size(28.dp)) {
                                                            Icon(Icons.Default.Close, contentDescription = "Close")
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(300.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(Color(0xFFF1F5F9)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        AsyncImage(
                                                            model = url,
                                                            contentDescription = label,
                                                            contentScale = ContentScale.Fit,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                        if (url.startsWith("http")) {
                                                            OutlinedButton(
                                                                onClick = {
                                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                                    context.startActivity(intent)
                                                                },
                                                                shape = RoundedCornerShape(8.dp),
                                                                modifier = Modifier.padding(end = 8.dp)
                                                            ) {
                                                                Text("Open Full", fontSize = 11.sp)
                                                            }
                                                        }
                                                        Button(
                                                            onClick = { showPreview = false },
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text("Close", fontSize = 11.sp)
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
                }
            }

            // Search and Date / Entity Filters
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search date, order #, item code, supplier, transporter...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date Filters Row
                    Text(
                        text = "Filter by Date:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Filters Row
                    Text(
                        text = "Filter by Delivery Status:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "All" to "All Status ($totalVisits Days)",
                            "Pending" to "Pending ($pendingEntriesCount)",
                            "Delivered" to "Delivered ($deliveredEntriesCount)"
                        ).forEach { (statusKey, label) ->
                            val isSelected = filterStatus == statusKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { filterStatus = statusKey },
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

                    // Entity Filters: Supplier filter
                    if (distinctSuppliers.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Filter by Supplier / Mill:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val allSelected = filterSupplier == "All"
                            FilterChip(
                                selected = allSelected,
                                onClick = { filterSupplier = "All" },
                                shape = CircleShape,
                                modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                label = { Text("All Suppliers (${distinctSuppliers.size})", fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal) }
                            )
                            distinctSuppliers.forEach { sup ->
                                val isSelected = filterSupplier == sup
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { filterSupplier = sup },
                                    shape = CircleShape,
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                    label = { Text(sup, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }

                    // Entity Filters: Transporter filter
                    if (distinctTransporters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Filter by Transporter:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val allSelected = filterTransporter == "All"
                            FilterChip(
                                selected = allSelected,
                                onClick = { filterTransporter = "All" },
                                shape = CircleShape,
                                modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                label = { Text("All Transporters", fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal) }
                            )
                            distinctTransporters.forEach { tr ->
                                val isSelected = filterTransporter == tr
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { filterTransporter = tr },
                                    shape = CircleShape,
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                    label = { Text(tr, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                )
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
                        onOpenDayReport = { viewModel.openCustomerReport(visit) },
                        onShareWhatsApp = { viewModel.shareCustomerReportWhatsApp(visit) },
                        onSharePdf = { viewModel.shareCustomerDayReportPdf(visit) },
                        onAdvanceStatus = { viewModel.advanceEntryDeliveryStatus(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun DayVisitCard(
    visit: VisitEntity,
    entries: List<PurchaseEntryEntity>,
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
            // Day Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            // Summary Totals & Day Report Action Buttons
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
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
                    // Day Report full screen preview
                    Button(
                        onClick = onOpenDayReport,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Day Report", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
