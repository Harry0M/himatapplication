package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.MarketEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDetailScreen(
    viewModel: HimatViewModel,
    market: MarketEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onOpenCustomer: (CustomerEntity) -> Unit = { viewModel.openCustomerDetail(it) },
    onOpenSupplier: (SupplierEntity) -> Unit = { viewModel.openSupplierDetail(it) },
    onOpenOrder: (PurchaseEntryEntity) -> Unit = { viewModel.openOrderDetail(it, returnScreen = AppScreen.MARKET_DETAIL) },
    onOpenEmployee: (EmployeeEntity) -> Unit = { viewModel.openEmployeeDetail(it) }
) {
    val context = LocalContext.current
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val allSuppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allEmployees by viewModel.allEmployees.collectAsStateWithLifecycle()

    val visitMap = remember(allVisits) { allVisits.associateBy { it.id } }
    val customerNameMap = remember(allCustomers) { allCustomers.associate { it.id to (it.firmName.ifBlank { it.name }) } }
    val supplierNameMap = remember(allSuppliers) { allSuppliers.associate { it.id to (it.firmName.ifBlank { it.name }) } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("CUSTOMERS") } // "CUSTOMERS", "SUPPLIERS", "ORDERS", "AGENTS"

    val marketCustomers = remember(allCustomers, market.id, market.marketName, market.area) {
        val mName = market.marketName.trim().lowercase()
        val mArea = market.area.trim().lowercase()
        allCustomers.filter { c ->
            val cArea = c.marketArea.lowercase()
            val cMkts = c.markets.lowercase()
            val cAddress = c.address.lowercase()
            (mName.isNotBlank() && (cArea.contains(mName) || cMkts.contains(mName))) ||
            (mArea.isNotBlank() && cAddress.contains(mArea))
        }.sortedBy { it.firmName.ifBlank { it.name } }
    }

    val marketSuppliers = remember(allSuppliers, market.id, market.marketName, market.area) {
        val mName = market.marketName.trim().lowercase()
        val mArea = market.area.trim().lowercase()
        allSuppliers.filter { s ->
            val sArea = s.marketArea.lowercase()
            val sMkts = s.markets.lowercase()
            val sAddress = s.address.lowercase()
            s.marketId == market.id ||
            (mName.isNotBlank() && (sArea.contains(mName) || sMkts.contains(mName))) ||
            (mArea.isNotBlank() && sAddress.contains(mArea))
        }.sortedBy { it.firmName.ifBlank { it.name } }
    }

    val assignedAgents = remember(allEmployees, market.marketName) {
        val mName = market.marketName.trim().lowercase()
        allEmployees.filter { emp ->
            val assigned = emp.assignedMarkets.lowercase()
            val mkts = emp.markets.lowercase()
            (mName.isNotBlank() && (assigned.contains(mName) || mkts.contains(mName)))
        }.sortedBy { it.name }
    }

    val custIdSet = remember(marketCustomers) { marketCustomers.map { it.id }.toSet() }
    val suppIdSet = remember(marketSuppliers) { marketSuppliers.map { it.id }.toSet() }
    val marketOrders = remember(allEntries, custIdSet, suppIdSet, visitMap) {
        allEntries.filter { entry ->
            val visit = visitMap[entry.visitId]
            val custMatches = visit != null && custIdSet.contains(visit.customerId)
            val suppMatches = entry.supplierId != 0L && suppIdSet.contains(entry.supplierId)
            custMatches || suppMatches
        }.sortedByDescending { it.id }
    }

    val totalPiecesVolume = remember(marketOrders) { marketOrders.sumOf { it.pieces } }

    // Filtered by Search Query
    val filteredCustomers = remember(marketCustomers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) marketCustomers
        else marketCustomers.filter {
            it.name.contains(q, ignoreCase = true) ||
            it.firmName.contains(q, ignoreCase = true) ||
            it.phone.contains(q, ignoreCase = true) ||
            it.city.contains(q, ignoreCase = true) ||
            it.address.contains(q, ignoreCase = true)
        }
    }

    val filteredSuppliers = remember(marketSuppliers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) marketSuppliers
        else marketSuppliers.filter {
            it.name.contains(q, ignoreCase = true) ||
            it.firmName.contains(q, ignoreCase = true) ||
            it.phone.contains(q, ignoreCase = true) ||
            it.city.contains(q, ignoreCase = true) ||
            it.type.contains(q, ignoreCase = true) ||
            it.productsMade.contains(q, ignoreCase = true)
        }
    }

    val filteredOrders = remember(marketOrders, searchQuery, visitMap, customerNameMap, supplierNameMap) {
        val q = searchQuery.trim()
        if (q.isBlank()) marketOrders
        else marketOrders.filter { entry ->
            val visit = visitMap[entry.visitId]
            val custName = visit?.customerName ?: (visit?.let { customerNameMap[it.customerId] } ?: "")
            val suppName = entry.supplierName.ifBlank { supplierNameMap[entry.supplierId] ?: "" }
            entry.orderNo.contains(q, ignoreCase = true) ||
            entry.itemCode.contains(q, ignoreCase = true) ||
            entry.deliveryStatus.contains(q, ignoreCase = true) ||
            custName.contains(q, ignoreCase = true) ||
            suppName.contains(q, ignoreCase = true)
        }
    }

    val filteredAgents = remember(assignedAgents, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) assignedAgents
        else assignedAgents.filter {
            it.name.contains(q, ignoreCase = true) ||
            it.phone.contains(q, ignoreCase = true) ||
            it.email.contains(q, ignoreCase = true) ||
            it.role.contains(q, ignoreCase = true) ||
            it.assignedMarkets.contains(q, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = market.marketName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Market Hub • ${market.city}",
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
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Market", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF6F8FB))
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
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
                        .padding(top = 2.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFAF5FF),
                        border = BorderStroke(2.dp, Color(0xFFE9D5FF)),
                        modifier = Modifier.size(66.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = null,
                                tint = Color(0xFF9333EA),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = market.marketName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    val areaText = listOfNotNull(market.area.takeIf { it.isNotBlank() }, market.city.takeIf { it.isNotBlank() }).joinToString(", ")
                    Text(
                        text = areaText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFFAF5FF),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color(0xFFE9D5FF))
                        ) {
                            Text(
                                text = "🏷️ ${market.marketType}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7E22CE),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (market.pincode.isNotBlank()) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "PIN: ${market.pincode}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Action Pills: Map Direction, Edit, Copy
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val fullAddress = listOfNotNull(market.marketName, market.landmark.takeIf { it.isNotBlank() }, market.area.takeIf { it.isNotBlank() }, market.city).joinToString(", ")
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(fullAddress))
                                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                    context.startActivity(mapIntent)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                                Text("Market Map", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D4ED8))
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFAF5FF),
                            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onEdit() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(13.dp))
                                Text("Edit Hub", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7E22CE))
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val text = "Market: ${market.marketName}\nType: ${market.marketType}\nArea: ${market.area}\nCity: ${market.city}\nPIN: ${market.pincode}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Market Details", text))
                                    Toast.makeText(context, "Market info copied", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(13.dp))
                                Text("Copy Info", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                            }
                        }
                    }
                }
            }

            // Stats Row: Buyers, Suppliers, Orders, Pieces volume
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${marketCustomers.size}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                        Text("Buyers", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${marketSuppliers.size}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Text("Suppliers", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${marketOrders.size}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9333EA)
                        )
                        Text("Orders", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalPiecesVolume",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text("Pieces", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }

            // Market Details Card
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MARKET LOCATION & DETAILS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            letterSpacing = 0.5.sp
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)

                        if (market.area.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Area / Locality:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(market.area, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            }
                        }

                        if (market.landmark.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Landmark:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(market.landmark, fontSize = 12.sp, color = Color(0xFF0F172A))
                            }
                        }

                        if (market.city.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("City Hub:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(market.city, fontSize = 12.sp, color = Color(0xFF0F172A))
                            }
                        }

                        if (market.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("About Market:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Text(market.description, fontSize = 11.5.sp, color = Color(0xFF334155))
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    placeholder = {
                        Text(
                            text = when (selectedTab) {
                                "CUSTOMERS" -> "Search buyers by name, firm or phone..."
                                "SUPPLIERS" -> "Search suppliers by name, firm or type..."
                                "ORDERS" -> "Search orders by order no, item, status..."
                                "AGENTS" -> "Search salesmen by name or phone..."
                                else -> "Search in this market hub..."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF9333EA),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }

            // 4 Tabs: CUSTOMERS, SUPPLIERS, ORDERS, AGENTS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isCust = selectedTab == "CUSTOMERS"
                    Surface(
                        shape = CircleShape,
                        color = if (isCust) Color(0xFF2563EB) else Color.White,
                        border = BorderStroke(1.dp, if (isCust) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { selectedTab = "CUSTOMERS" }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = if (isCust) Color.White else Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Buyers (${filteredCustomers.size})",
                                fontSize = 10.5.sp,
                                fontWeight = if (isCust) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCust) Color.White else Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val isSupp = selectedTab == "SUPPLIERS"
                    Surface(
                        shape = CircleShape,
                        color = if (isSupp) Color(0xFF059669) else Color.White,
                        border = BorderStroke(1.dp, if (isSupp) Color(0xFF059669) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { selectedTab = "SUPPLIERS" }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Store,
                                contentDescription = null,
                                tint = if (isSupp) Color.White else Color(0xFF059669),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Mills (${filteredSuppliers.size})",
                                fontSize = 10.5.sp,
                                fontWeight = if (isSupp) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSupp) Color.White else Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val isOrder = selectedTab == "ORDERS"
                    Surface(
                        shape = CircleShape,
                        color = if (isOrder) Color(0xFF9333EA) else Color.White,
                        border = BorderStroke(1.dp, if (isOrder) Color(0xFF9333EA) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { selectedTab = "ORDERS" }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                tint = if (isOrder) Color.White else Color(0xFF9333EA),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Orders (${filteredOrders.size})",
                                fontSize = 10.5.sp,
                                fontWeight = if (isOrder) FontWeight.Bold else FontWeight.Medium,
                                color = if (isOrder) Color.White else Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val isAgent = selectedTab == "AGENTS"
                    Surface(
                        shape = CircleShape,
                        color = if (isAgent) Color(0xFF0F766E) else Color.White,
                        border = BorderStroke(1.dp, if (isAgent) Color(0xFF0F766E) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { selectedTab = "AGENTS" }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Badge,
                                contentDescription = null,
                                tint = if (isAgent) Color.White else Color(0xFF0F766E),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Agents (${filteredAgents.size})",
                                fontSize = 10.5.sp,
                                fontWeight = if (isAgent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAgent) Color.White else Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // TAB CONTENT
            when (selectedTab) {
                "CUSTOMERS" -> {
                    if (filteredCustomers.isEmpty()) {
                        item {
                            ElevatedCard(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No buyers match '$searchQuery'" else "No customers mapped to this market yet",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredCustomers, key = { it.id }) { customer ->
                            val name = customer.firmName.ifBlank { customer.name }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenCustomer(customer) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFEFF6FF),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = name.take(1).uppercase(),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2563EB)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val sub = listOfNotNull(customer.phone.takeIf { it.isNotBlank() }, customer.city.takeIf { it.isNotBlank() }).joinToString(" • ")
                                            Text(sub.ifBlank { "Buyer" }, fontSize = 11.5.sp, color = Color(0xFF64748B))
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (customer.phone.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                "SUPPLIERS" -> {
                    if (filteredSuppliers.isEmpty()) {
                        item {
                            ElevatedCard(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No suppliers match '$searchQuery'" else "No suppliers/mills mapped to this market yet",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredSuppliers, key = { it.id }) { supplier ->
                            val name = supplier.firmName.ifBlank { supplier.name }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenSupplier(supplier) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFECFDF5),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                SupplierTypeBadge(type = supplier.type)
                                            }
                                            val sub = listOfNotNull(supplier.phone.takeIf { it.isNotBlank() }, supplier.productsMade.takeIf { it.isNotBlank() } ?: supplier.city.takeIf { it.isNotBlank() }).joinToString(" • ")
                                            Text(sub.ifBlank { "Supplier / Mill" }, fontSize = 11.5.sp, color = Color(0xFF64748B))
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (supplier.phone.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier.phone}"))
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                "ORDERS" -> {
                    if (filteredOrders.isEmpty()) {
                        item {
                            ElevatedCard(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No orders match '$searchQuery'" else "No orders recorded in this market hub yet",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredOrders, key = { it.id }) { entry ->
                            val visit = visitMap[entry.visitId]
                            val buyerName = visit?.customerName ?: (visit?.let { customerNameMap[it.customerId] } ?: "Customer")
                            val supplierName = entry.supplierName.ifBlank { supplierNameMap[entry.supplierId] ?: "Supplier" }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenOrder(entry) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(entry.orderNo, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF9333EA))
                                            DeliveryStatusBadge(status = entry.deliveryStatus)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Buyer: $buyerName • ${entry.itemCode}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val packText = if (entry.caseCount > 0) "${entry.caseCount}c + ${entry.loosePieces}L (${entry.pieces} pcs)" else "${entry.pieces} loose pcs"
                                        Text(
                                            text = "Mill: $supplierName • Pack: $packText",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                "AGENTS" -> {
                    if (filteredAgents.isEmpty()) {
                        item {
                            ElevatedCard(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No agents match '$searchQuery'" else "No salesmen or field agents assigned to this market",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredAgents, key = { it.id }) { agent ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenEmployee(agent) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFF0FDFA),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(agent.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Surface(
                                                    color = Color(0xFFF0FDFA),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = BorderStroke(0.5.dp, Color(0xFF99F6E4))
                                                ) {
                                                    Text(
                                                        text = agent.role.ifBlank { "Salesman" },
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF0F766E),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            val phoneOrEmail = listOfNotNull(agent.phone.takeIf { it.isNotBlank() }, agent.email.takeIf { it.isNotBlank() }).joinToString(" • ")
                                            Text(phoneOrEmail.ifBlank { "Sales Representative" }, fontSize = 11.5.sp, color = Color(0xFF64748B))
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (agent.phone.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${agent.phone}"))
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
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
