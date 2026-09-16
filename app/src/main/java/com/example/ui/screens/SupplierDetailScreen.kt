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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.viewmodel.HimatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailScreen(
    viewModel: HimatViewModel,
    supplier: SupplierEntity,
    onBack: () -> Unit,
    onOpenVisit: (VisitEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var activeParentFilter by remember { mutableStateOf<String?>("STATUS") }
    var filterStatus by remember { mutableStateOf("All") } // "All", "Pending", "Delivered"
    var filterDateRange by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "LAST_7", "THIS_MONTH"
    var filterCustomer by remember { mutableStateOf("All") }
    var filterTransporter by remember { mutableStateOf("All") }

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

    val visitMap = remember(allVisits) {
        allVisits.associateBy { it.id }
    }

    // All purchase entries belonging to this supplier
    val supplierEntries = remember(allEntries, supplier.id, supplier.name) {
        allEntries.filter {
            it.supplierId == supplier.id || it.supplierName.equals(supplier.name, ignoreCase = true)
        }.sortedByDescending { it.id }
    }

    val distinctCustomers = remember(supplierEntries, visitMap) {
        supplierEntries.mapNotNull { visitMap[it.visitId]?.customerName?.trim() }
            .filter { it.isNotBlank() }.distinct().sorted()
    }
    val distinctTransporters = remember(supplierEntries) {
        supplierEntries.map { it.transporter.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val totalOrders = supplierEntries.size
    val totalPieces = supplierEntries.sumOf { it.pieces }
    val totalCases = supplierEntries.sumOf { it.caseCount }
    val totalLoose = supplierEntries.sumOf { it.loosePieces }
    val pendingOrders = supplierEntries.count { it.deliveryStatus != "Delivered" }
    val pendingPieces = supplierEntries.filter { it.deliveryStatus != "Delivered" }.sumOf { it.pieces }
    val deliveredOrders = supplierEntries.count { it.deliveryStatus == "Delivered" }
    val deliveredPieces = supplierEntries.filter { it.deliveryStatus == "Delivered" }.sumOf { it.pieces }

    // Filtered entries based on date range, status, customer, transporter, and search query
    val filteredEntries = remember(supplierEntries, searchQuery, filterStatus, filterDateRange, filterCustomer, filterTransporter, visitMap) {
        supplierEntries.filter { entry ->
            val visit = visitMap[entry.visitId]
            val customerName = visit?.customerName ?: ""
            val visitDate = visit?.date ?: ""

            // 1. Date Range Filter
            val matchesDate = when (filterDateRange) {
                "TODAY" -> visitDate == todayStr
                "LAST_7" -> visitDate >= last7DaysCutoff
                "THIS_MONTH" -> visitDate.startsWith(thisMonthPrefix)
                else -> true
            }
            if (!matchesDate) return@filter false

            // 2. Status Filter
            val matchesStatus = when (filterStatus) {
                "Pending" -> entry.deliveryStatus != "Delivered"
                "Delivered" -> entry.deliveryStatus == "Delivered"
                else -> true
            }
            if (!matchesStatus) return@filter false

            // 3. Customer Filter
            val matchesCustomer = if (filterCustomer == "All") true else customerName.equals(filterCustomer, ignoreCase = true)
            if (!matchesCustomer) return@filter false

            // 4. Transporter Filter
            val matchesTransporter = if (filterTransporter == "All") true else entry.transporter.equals(filterTransporter, ignoreCase = true)
            if (!matchesTransporter) return@filter false

            // 5. Search Filter
            val matchesSearch = searchQuery.isBlank() ||
                    entry.orderNo.contains(searchQuery, ignoreCase = true) ||
                    entry.itemCode.contains(searchQuery, ignoreCase = true) ||
                    entry.transporter.contains(searchQuery, ignoreCase = true) ||
                    customerName.contains(searchQuery, ignoreCase = true) ||
                    visitDate.contains(searchQuery, ignoreCase = true)

            matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = supplier.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Supplier Reports, Bills & All Entries",
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
                    if (supplier.phone.isNotBlank()) {
                        FilledTonalIconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call Supplier",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (supplier.email.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        FilledTonalIconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${supplier.email}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email Supplier",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
            // Cardless Hero Profile Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 66dp Hero Centered Circle Avatar
                    val suppAvatarPhoto = supplier.shopPhotoUri.ifBlank { supplier.visitingCardPhotoUri }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(2.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.size(66.dp)
                    ) {
                        if (suppAvatarPhoto.isNotBlank()) {
                            AsyncImage(
                                model = suppAvatarPhoto,
                                contentDescription = supplier.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Firm / Supplier Name
                    val displayName = supplier.firmName.ifBlank { supplier.name }
                    Text(
                        text = displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Contact Person
                    if (supplier.firmName.isNotBlank() && supplier.name.isNotBlank() && supplier.firmName != supplier.name) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Contact: ${supplier.name}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Monospace GSTIN & Badges
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SupplierTypeBadge(type = supplier.type)
                        if (supplier.gstin.isNotBlank()) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "GST: ${supplier.gstin}",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (supplier.defaultCaseSize > 0) {
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Pack: ${supplier.defaultCaseSize} pcs",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (supplier.shopCount > 1) {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "🏪 ${supplier.shopCount} Outlets",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                        if (supplier.phone.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier.phone}"))
                                        context.startActivity(intent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF059669), modifier = Modifier.size(13.dp))
                                    Text(text = supplier.phone, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF065F46))
                                }
                            }
                        }

                        val mapAddress = listOfNotNull(
                            supplier.officeLocation.takeIf { it.isNotBlank() },
                            supplier.officeAddress.takeIf { it.isNotBlank() },
                            supplier.address.takeIf { it.isNotBlank() },
                            supplier.city.takeIf { it.isNotBlank() }
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
                                    Text(text = "Direction", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D4ED8))
                                }
                            }
                        }

                        if (supplier.email.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${supplier.email}"))
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
            }

            // Cardless Stats Row (NO cards, clean inline stats with 1dp vertical dividers)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalOrders",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Total Orders",
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

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%,d", totalPieces),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Total Pieces",
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

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$deliveredOrders",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "Delivered",
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

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$pendingOrders",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingOrders > 0) Color(0xFFDC2626) else Color(0xFF059669)
                        )
                        Text(
                            text = "Pending",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Extended Details (Office, Markets, Garments)
            val officeAddr = supplier.officeAddress.ifBlank { supplier.address }
            val hasExtendedDetails = officeAddr.isNotBlank() ||
                    supplier.homeAddress.isNotBlank() ||
                    supplier.officeLocation.isNotBlank() ||
                    supplier.personalLocation.isNotBlank() ||
                    supplier.shopLocations.isNotBlank() ||
                    supplier.markets.isNotBlank() ||
                    supplier.categories.isNotBlank() ||
                    supplier.garmentTypes.isNotBlank() ||
                    supplier.referredBy.isNotBlank()

            if (hasExtendedDetails) {
                item {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (officeAddr.isNotBlank() || supplier.officeLocation.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Office/Mill: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(
                                        text = listOfNotNull(officeAddr.takeIf { it.isNotBlank() }, supplier.officeLocation.takeIf { it.isNotBlank() }).joinToString(" • "),
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                            if (supplier.homeAddress.isNotBlank() || supplier.personalLocation.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Home/Fact: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(
                                        text = listOfNotNull(supplier.homeAddress.takeIf { it.isNotBlank() }, supplier.personalLocation.takeIf { it.isNotBlank() }).joinToString(" • "),
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                            if (supplier.shopLocations.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Branches: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(supplier.shopLocations, fontSize = 11.sp, color = Color(0xFF1E293B))
                                }
                            }
                            if (supplier.markets.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Markets: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(supplier.markets, fontSize = 11.sp, color = Color(0xFF0F766E), fontWeight = FontWeight.Medium)
                                }
                            }
                            val cats = supplier.categories.ifBlank { supplier.garmentTypes }
                            if (cats.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Products: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(cats, fontSize = 11.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Medium)
                                }
                            }
                            if (supplier.referredBy.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Referred by: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Text(supplier.referredBy, fontSize = 11.sp, color = Color(0xFF1E293B))
                                }
                            }
                        }
                    }
                }
            }

            // Supplier Verification Photos & Cloud Documents
            val supplierDocs = listOfNotNull(
                supplier.visitingCardPhotoUri.takeIf { it.isNotBlank() }?.let { "Visiting Card" to it },
                supplier.shopPhotoUri.takeIf { it.isNotBlank() }?.let { "Shop / Mill Front" to it }
            )

            if (supplierDocs.isNotEmpty()) {
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
                                        text = "Verification Photos & Cloud Media",
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
                                        text = "${supplierDocs.size} Attached",
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
                                supplierDocs.forEach { (label, url) ->
                                    var showPreview by remember { mutableStateOf(false) }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clickable { showPreview = true }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(124.dp, 85.dp)
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

            // Search and Cascading Filter Chips
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Compact 36dp Pill Search Bar
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
                                        text = "Search by Order #, Item, Customer...",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cascading Filter Chips: Level 1 Single Line Parent Chips
                    val isAnyFilterActive = filterDateRange != "ALL" || filterStatus != "All" || filterCustomer != "All" || filterTransporter != "All"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date Parent Chip
                        val isDateSelected = activeParentFilter == "DATE"
                        val hasDateFilter = filterDateRange != "ALL"
                        Surface(
                            shape = CircleShape,
                            color = if (isDateSelected) Color(0xFF1E3A8A) else if (hasDateFilter) Color(0xFFEFF6FF) else Color.White,
                            border = BorderStroke(1.dp, if (isDateSelected) Color(0xFF1E3A8A) else if (hasDateFilter) Color(0xFF3B82F6) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { activeParentFilter = if (isDateSelected) null else "DATE" }
                        ) {
                            Text(
                                text = if (hasDateFilter) "Date: $filterDateRange" else "Date",
                                fontSize = 11.5.sp,
                                fontWeight = if (isDateSelected || hasDateFilter) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDateSelected) Color.White else if (hasDateFilter) Color(0xFF1D4ED8) else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }

                        // Status Parent Chip
                        val isStatusSelected = activeParentFilter == "STATUS"
                        val hasStatusFilter = filterStatus != "All"
                        Surface(
                            shape = CircleShape,
                            color = if (isStatusSelected) Color(0xFF1E3A8A) else if (hasStatusFilter) Color(0xFFEFF6FF) else Color.White,
                            border = BorderStroke(1.dp, if (isStatusSelected) Color(0xFF1E3A8A) else if (hasStatusFilter) Color(0xFF3B82F6) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { activeParentFilter = if (isStatusSelected) null else "STATUS" }
                        ) {
                            Text(
                                text = if (hasStatusFilter) "Status: $filterStatus" else "Status",
                                fontSize = 11.5.sp,
                                fontWeight = if (isStatusSelected || hasStatusFilter) FontWeight.Bold else FontWeight.Medium,
                                color = if (isStatusSelected) Color.White else if (hasStatusFilter) Color(0xFF1D4ED8) else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }

                        // Customer Parent Chip
                        val isCustomerSelected = activeParentFilter == "CUSTOMER"
                        val hasCustomerFilter = filterCustomer != "All"
                        Surface(
                            shape = CircleShape,
                            color = if (isCustomerSelected) Color(0xFF1E3A8A) else if (hasCustomerFilter) Color(0xFFEFF6FF) else Color.White,
                            border = BorderStroke(1.dp, if (isCustomerSelected) Color(0xFF1E3A8A) else if (hasCustomerFilter) Color(0xFF3B82F6) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { activeParentFilter = if (isCustomerSelected) null else "CUSTOMER" }
                        ) {
                            Text(
                                text = if (hasCustomerFilter) "Cust: $filterCustomer" else "Customer",
                                fontSize = 11.5.sp,
                                fontWeight = if (isCustomerSelected || hasCustomerFilter) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCustomerSelected) Color.White else if (hasCustomerFilter) Color(0xFF1D4ED8) else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }

                        // Transporter Parent Chip
                        val isTransporterSelected = activeParentFilter == "TRANSPORTER"
                        val hasTransporterFilter = filterTransporter != "All"
                        Surface(
                            shape = CircleShape,
                            color = if (isTransporterSelected) Color(0xFF1E3A8A) else if (hasTransporterFilter) Color(0xFFEFF6FF) else Color.White,
                            border = BorderStroke(1.dp, if (isTransporterSelected) Color(0xFF1E3A8A) else if (hasTransporterFilter) Color(0xFF3B82F6) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { activeParentFilter = if (isTransporterSelected) null else "TRANSPORTER" }
                        ) {
                            Text(
                                text = if (hasTransporterFilter) "Trans: $filterTransporter" else "Transporter",
                                fontSize = 11.5.sp,
                                fontWeight = if (isTransporterSelected || hasTransporterFilter) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTransporterSelected) Color.White else if (hasTransporterFilter) Color(0xFF1D4ED8) else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }

                        // Reset Chip
                        if (isAnyFilterActive) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        filterDateRange = "ALL"
                                        filterStatus = "All"
                                        filterCustomer = "All"
                                        filterTransporter = "All"
                                        searchQuery = ""
                                    }
                            ) {
                                Text(
                                    text = "Reset All",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Level 2 Child Chips (Unboxed, NO card container)
                    if (activeParentFilter != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (activeParentFilter) {
                                "DATE" -> {
                                    listOf(
                                        "ALL" to "All Dates",
                                        "TODAY" to "Today",
                                        "LAST_7" to "Last 7 Days",
                                        "THIS_MONTH" to "This Month"
                                    ).forEach { (rangeKey, label) ->
                                        val isSelected = filterDateRange == rangeKey
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { filterDateRange = rangeKey }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFF334155),
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
                                            )
                                        }
                                    }
                                }
                                "STATUS" -> {
                                    listOf(
                                        "All" to "All Bills ($totalOrders)",
                                        "Pending" to "Pending ($pendingOrders)",
                                        "Delivered" to "Delivered ($deliveredOrders)"
                                    ).forEach { (statusKey, label) ->
                                        val isSelected = filterStatus == statusKey
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { filterStatus = statusKey }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFF334155),
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
                                            )
                                        }
                                    }
                                }
                                "CUSTOMER" -> {
                                    val isAllSelected = filterCustomer == "All"
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isAllSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { filterCustomer = "All" }
                                    ) {
                                        Text(
                                            text = "All Customers (${distinctCustomers.size})",
                                            fontSize = 11.sp,
                                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isAllSelected) Color.White else Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
                                        )
                                    }
                                    distinctCustomers.forEach { cust ->
                                        val isSelected = filterCustomer == cust
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { filterCustomer = cust }
                                        ) {
                                            Text(
                                                text = cust,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFF334155),
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
                                            )
                                        }
                                    }
                                }
                                "TRANSPORTER" -> {
                                    val isAllSelected = filterTransporter == "All"
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isAllSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { filterTransporter = "All" }
                                    ) {
                                        Text(
                                            text = "All Transporters",
                                            fontSize = 11.sp,
                                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isAllSelected) Color.White else Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
                                        )
                                    }
                                    distinctTransporters.forEach { tr ->
                                        val isSelected = filterTransporter == tr
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { filterTransporter = tr }
                                        ) {
                                            Text(
                                                text = tr,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFF334155),
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
                                            )
                                        }
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
                        text = "Supplier Bills & Entries (${filteredEntries.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (filteredEntries.isEmpty()) {
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
                                text = if (supplierEntries.isEmpty()) "No orders recorded with this supplier" else "No bills matching filter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Purchase entries recorded in Customer visits will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredEntries, key = { it.id }) { entry ->
                    val visit = visitMap[entry.visitId]
                    SupplierBillCard(
                        entry = entry,
                        visit = visit,
                        onOpenSupplierCopy = {
                            if (visit != null) {
                                viewModel.openSupplierCopy(visit, supplier)
                            }
                        },
                        onAdvanceStatus = { viewModel.advanceEntryDeliveryStatus(entry) },
                        onOpenVisit = { visit?.let { onOpenVisit(it) } }
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierBillCard(
    entry: PurchaseEntryEntity,
    visit: VisitEntity?,
    onOpenSupplierCopy: () -> Unit,
    onAdvanceStatus: () -> Unit,
    onOpenVisit: (() -> Unit)? = null
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Order No & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entry.orderNo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (visit != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(6.dp),
                            modifier = if (onOpenVisit != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable { onOpenVisit() } else Modifier
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = visit.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (onOpenVisit != null) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = "Open Trip",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                DeliveryStatusBadge(status = entry.deliveryStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Info & Item Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Customer: ${visit?.customerName ?: "Direct Order"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Item Code: ${entry.itemCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${entry.pieces} pcs",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    val packSummary = if (entry.caseCount > 0) "${entry.caseCount} Cases" else "${entry.loosePieces} Loose"
                    Text(
                        text = packSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Case Packaging Breakdown & Transporter
            Spacer(modifier = Modifier.height(8.dp))
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
                    val packDesc = if (entry.caseCount > 0 && entry.loosePieces > 0) {
                        "${entry.caseCount} Cases (${entry.caseCount * entry.caseSize} pcs) + ${entry.loosePieces} Loose"
                    } else if (entry.caseCount > 0) {
                        "${entry.caseCount} Cases (${entry.pieces} pcs)"
                    } else {
                        "${entry.loosePieces} Loose pcs"
                    }
                    Text(
                        text = packDesc,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (entry.transporter.isNotBlank()) {
                        Text(
                            text = entry.transporter,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!entry.mixedPackNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pack Group Note: ${entry.mixedPackNote}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Buttons Row: View Supplier Voucher Copy, Open Trip & Quick Status
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (visit != null) {
                        Button(
                            onClick = onOpenSupplierCopy,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 34.dp)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Voucher",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (onOpenVisit != null) {
                            OutlinedButton(
                                onClick = onOpenVisit,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 34.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Open Trip",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                if (entry.deliveryStatus != "Delivered") {
                    OutlinedButton(
                        onClick = onAdvanceStatus,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
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
