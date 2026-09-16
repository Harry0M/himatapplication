package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.MarketEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.TransporterEntity
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.ui.viewmodel.MasterTab

// -------------------------------------------------------------
// Helper Data Models for Hub & Modals
// -------------------------------------------------------------

data class MasterCategoryItem(
    val tab: MasterTab,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val count: Int,
    val iconBgColor: Color,
    val iconTintColor: Color,
    val tag: String
)

data class MasterDeleteRequest(
    val typeName: String,
    val itemName: String,
    val onConfirm: () -> Unit
)

data class MasterDetailView(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val details: List<Pair<String, String>>,
    val onEdit: (() -> Unit)? = null
)

// -------------------------------------------------------------
// Main MastersScreen
// -------------------------------------------------------------

@Composable
fun MastersScreen(
    viewModel: HimatViewModel,
    initialTab: MasterTab? = null
) {
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()

    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.visibleSuppliers.collectAsStateWithLifecycle()
    val brands by viewModel.visibleBrands.collectAsStateWithLifecycle()
    val transporters by viewModel.visibleTransporters.collectAsStateWithLifecycle()
    val markets by viewModel.visibleMarkets.collectAsStateWithLifecycle()
    val products by viewModel.visibleProducts.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()

    // Active Category Selection: null means on the Masters Hub Directory
    var selectedCategory by remember { mutableStateOf<MasterTab?>(initialTab) }

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            selectedCategory = initialTab
        }
    }

    // Hardware / System Back Button: Returns to Masters Hub if currently in a specific master screen
    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    // Search and Filter States
    var hubSearchQuery by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var supplierTypeFilter by remember { mutableStateOf("All") } // "All", "Manufacturer", "Wholesaler"

    // Dialog States for Delete & View
    var deleteConfirmRequest by remember { mutableStateOf<MasterDeleteRequest?>(null) }
    var detailViewItem by remember { mutableStateOf<MasterDetailView?>(null) }

    // List States
    val customerListState = rememberLazyListState()
    val supplierListState = rememberLazyListState()
    val brandListState = rememberLazyListState()
    val transporterListState = rememberLazyListState()
    val employeeListState = rememberLazyListState()
    val marketListState = rememberLazyListState()
    val productListState = rememberLazyListState()

    var isHeaderVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            private var accumulatedDelta = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    if (delta < 0) {
                        if (accumulatedDelta > 0) accumulatedDelta = 0f
                        accumulatedDelta += delta
                        if (accumulatedDelta < -25f) isHeaderVisible = false
                    } else if (delta > 0) {
                        if (accumulatedDelta < 0) accumulatedDelta = 0f
                        accumulatedDelta += delta
                        if (accumulatedDelta > 18f) isHeaderVisible = true
                    }
                }
                return Offset.Zero
            }
        }
    }

    val context = LocalContext.current

    // Master Categories metadata
    val categories = remember(customers.size, suppliers.size, products.size, brands.size, transporters.size, markets.size, employees.size) {
        listOf(
            MasterCategoryItem(
                tab = MasterTab.CUSTOMERS,
                title = "Customer Master",
                subtitle = "Retail stores, wholesale buyers, GSTIN & party contacts",
                icon = Icons.Default.People,
                count = customers.size,
                iconBgColor = Color(0xFFEFF6FF),
                iconTintColor = Color(0xFF2563EB),
                tag = "Buyers"
            ),
            MasterCategoryItem(
                tab = MasterTab.SUPPLIERS,
                title = "Supplier Master",
                subtitle = "Ahmedabad mills, garment manufacturers & wholesalers",
                icon = Icons.Default.Store,
                count = suppliers.size,
                iconBgColor = Color(0xFFECFDF5),
                iconTintColor = Color(0xFF059669),
                tag = "Mills / Mfrs"
            ),
            MasterCategoryItem(
                tab = MasterTab.PRODUCTS,
                title = "Product Master",
                subtitle = "Garment styles, fabric blends, default rates & pack sizes",
                icon = Icons.Default.Inventory,
                count = products.size,
                iconBgColor = Color(0xFFEEF2FF),
                iconTintColor = Color(0xFF4F46E5),
                tag = "Catalog"
            ),
            MasterCategoryItem(
                tab = MasterTab.BRANDS,
                title = "Brand Master",
                subtitle = "Garment brand labels, mill brands & classifications",
                icon = Icons.Default.Sell,
                count = brands.size,
                iconBgColor = Color(0xFFFFFBEB),
                iconTintColor = Color(0xFFD97706),
                tag = "Labels"
            ),
            MasterCategoryItem(
                tab = MasterTab.TRANSPORTERS,
                title = "Transporter Master",
                subtitle = "Logistics, parcel transport, delivery routes & contact desks",
                icon = Icons.Default.LocalShipping,
                count = transporters.size,
                iconBgColor = Color(0xFFECFEFF),
                iconTintColor = Color(0xFF0891B2),
                tag = "Logistics"
            ),
            MasterCategoryItem(
                tab = MasterTab.MARKETS,
                title = "Market Master",
                subtitle = "Textile markets, commercial complexes & trade hubs",
                icon = Icons.Default.LocationCity,
                count = markets.size,
                iconBgColor = Color(0xFFFAF5FF),
                iconTintColor = Color(0xFF9333EA),
                tag = "Trade Hubs"
            ),
            MasterCategoryItem(
                tab = MasterTab.EMPLOYEES,
                title = "Salesmen / Employee Master",
                subtitle = "Salesmen, territory assignments & mobile logins",
                icon = Icons.Default.Person,
                count = employees.size,
                iconBgColor = Color(0xFFF1F5F9),
                iconTintColor = NavyPrimary,
                tag = "Staff"
            )
        )
    }

    Scaffold(
        containerColor = Color(0xFFF6F8FB),
        floatingActionButton = {
            // Show FAB on Specific Master screen for instant record creation
            val currentTab = selectedCategory
            if (currentTab != null && (currentTab != MasterTab.EMPLOYEES || isSuperAdmin)) {
                FloatingActionButton(
                    onClick = {
                        viewModel.openAddMaster(currentTab)
                    },
                    containerColor = NavyPrimary,
                    contentColor = GoldAccent,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add New Record")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
                .padding(paddingValues)
        ) {
            val activeTab = selectedCategory

            if (activeTab == null) {
                // =========================================================================
                // LEVEL 1: MASTERS DIRECTORY / HUB SCREEN
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Business Masters",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "Select a master directory to manage records",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Total Entity Count Pill
                        val totalRecords = customers.size + suppliers.size + products.size + brands.size + transporters.size + markets.size + employees.size
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NavyPrimary.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = "$totalRecords Total",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Search bar on Masters Directory
                    OutlinedTextField(
                        value = hubSearchQuery,
                        onValueChange = { hubSearchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search master categories...",
                                fontSize = 13.5.sp,
                                color = TextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        trailingIcon = {
                            if (hubSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { hubSearchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = NavyPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    // Master Categories List
                    val filteredCategories = categories.filter {
                        it.title.contains(hubSearchQuery, ignoreCase = true) ||
                                it.subtitle.contains(hubSearchQuery, ignoreCase = true) ||
                                it.tag.contains(hubSearchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredCategories) { item ->
                            MasterHubCard(
                                item = item,
                                onClick = {
                                    selectedCategory = item.tab
                                    searchQuery = ""
                                },
                                onQuickAdd = {
                                    viewModel.openAddMaster(item.tab)
                                },
                                canAdd = item.tab != MasterTab.EMPLOYEES || isSuperAdmin
                            )
                        }
                    }
                }
            } else {
                // =========================================================================
                // LEVEL 2: SPECIFIC MASTER SCREEN (Customers, Suppliers, etc.)
                // =========================================================================
                val currentCategoryMeta = categories.firstOrNull { it.tab == activeTab } ?: categories[0]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    AnimatedVisibility(
                        visible = isHeaderVisible,
                        enter = expandVertically(tween(240, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                        exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
                    ) {
                        Column {
                            // Specific Screen Top Bar with Back Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Back Button to Masters Hub
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { selectedCategory = null }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back to Masters Hub",
                                            tint = NavyPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Category Icon & Title
                                Surface(
                                    shape = CircleShape,
                                    color = currentCategoryMeta.iconBgColor,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = currentCategoryMeta.icon,
                                            contentDescription = null,
                                            tint = currentCategoryMeta.iconTintColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentCategoryMeta.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary,
                                        letterSpacing = (-0.2).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val countText = when (activeTab) {
                                        MasterTab.CUSTOMERS -> "${customers.size} customers"
                                        MasterTab.SUPPLIERS -> "${suppliers.size} suppliers & mills"
                                        MasterTab.BRANDS -> "${brands.size} garment brands"
                                        MasterTab.TRANSPORTERS -> "${transporters.size} transport partners"
                                        MasterTab.EMPLOYEES -> "${employees.size} salesmen"
                                        MasterTab.MARKETS -> "${markets.size} textile markets"
                                        MasterTab.PRODUCTS -> "${products.size} active catalog items"
                                    }
                                    Text(
                                        text = countText,
                                        fontSize = 11.5.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Tally XML Export Button
                                if (activeTab == MasterTab.CUSTOMERS || activeTab == MasterTab.SUPPLIERS) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF0F766E),
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                if (activeTab == MasterTab.CUSTOMERS) {
                                                    viewModel.exportCustomersToTallyXml(context)
                                                } else {
                                                    viewModel.exportSuppliersToTallyXml(context)
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.FileDownload,
                                                contentDescription = "Export Tally XML",
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // Search Toggle Button
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSearchVisible || searchQuery.isNotBlank()) NavyPrimary else Color.White,
                                    border = BorderStroke(1.dp, if (isSearchVisible || searchQuery.isNotBlank()) NavyPrimary else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            isSearchVisible = !isSearchVisible
                                            if (!isSearchVisible) searchQuery = ""
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isSearchVisible || searchQuery.isNotBlank()) Icons.Default.Clear else Icons.Default.Search,
                                            contentDescription = "Toggle Search",
                                            tint = if (isSearchVisible || searchQuery.isNotBlank()) Color.White else NavyPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Quick Top Add Button
                                if (activeTab != MasterTab.EMPLOYEES || isSuperAdmin) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = NavyPrimary,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                viewModel.openAddMaster(activeTab)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add New Record",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Search Bar for Current Master
                            AnimatedVisibility(
                                visible = isSearchVisible || searchQuery.isNotBlank(),
                                enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                                exit = shrinkVertically(tween(180)) + fadeOut(tween(160))
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = {
                                            Text(
                                                text = "Search in ${currentCategoryMeta.title}...",
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = NavyPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Clear",
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = CircleShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedBorderColor = NavyPrimary,
                                            unfocusedBorderColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                }
                            }

                            // Supplier Type Filter (Manufacturer vs Wholesaler)
                            if (activeTab == MasterTab.SUPPLIERS) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("All", "Manufacturer", "Wholesaler").forEach { type ->
                                        val isSelected = supplierTypeFilter == type
                                        Surface(
                                            color = if (isSelected) NavyPrimary else Color.White,
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { supplierTypeFilter = type }
                                        ) {
                                            Text(
                                                text = type,
                                                color = if (isSelected) Color.White else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.5.sp,
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // -------------------------------------------------------------
                    // Entity Record Lists (with See, Edit, Delete)
                    // -------------------------------------------------------------
                    when (activeTab) {
                        MasterTab.CUSTOMERS -> {
                            val filtered = customers.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.firmName.contains(searchQuery, ignoreCase = true) ||
                                        it.city.contains(searchQuery, ignoreCase = true) ||
                                        it.phone.contains(searchQuery, ignoreCase = true) ||
                                        it.gstin.contains(searchQuery, ignoreCase = true) ||
                                        it.customerId.contains(searchQuery, ignoreCase = true)
                            }
                            if (filtered.isEmpty()) {
                                MasterEmptyState(
                                    entityName = "Customers",
                                    onAdd = { viewModel.openAddMaster(MasterTab.CUSTOMERS) }
                                )
                            } else {
                                LazyColumn(
                                    state = customerListState,
                                    contentPadding = PaddingValues(bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filtered, key = { it.id }) { customer ->
                                        CustomerCard(
                                            customer = customer,
                                            onClick = { viewModel.openCustomerDetail(customer) },
                                            onEdit = { viewModel.openEditCustomer(customer) },
                                            onDelete = {
                                                deleteConfirmRequest = MasterDeleteRequest(
                                                    typeName = "Customer",
                                                    itemName = customer.firmName.ifBlank { customer.name },
                                                    onConfirm = { viewModel.deleteCustomer(customer) }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        MasterTab.SUPPLIERS -> {
                            val filtered = suppliers.filter {
                                val matchesSearch = it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.firmName.contains(searchQuery, ignoreCase = true) ||
                                        it.marketArea.contains(searchQuery, ignoreCase = true) ||
                                        it.brand.contains(searchQuery, ignoreCase = true) ||
                                        it.city.contains(searchQuery, ignoreCase = true) ||
                                        it.gstin.contains(searchQuery, ignoreCase = true)
                                val matchesType = when (supplierTypeFilter) {
                                    "Manufacturer" -> it.type.equals("Manufacturer", ignoreCase = true)
                                    "Wholesaler" -> it.type.equals("Wholesaler", ignoreCase = true)
                                    else -> true
                                }
                                matchesSearch && matchesType
                            }
                            if (filtered.isEmpty()) {
                                MasterEmptyState(
                                    entityName = "Suppliers & Mills",
                                    onAdd = { viewModel.openAddMaster(MasterTab.SUPPLIERS) }
                                )
                            } else {
                                LazyColumn(
                                    state = supplierListState,
                                    contentPadding = PaddingValues(bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filtered, key = { it.id }) { supplier ->
                                        SupplierCard(
                                            supplier = supplier,
                                            onClick = { viewModel.openSupplierDetail(supplier) },
                                            onEdit = { viewModel.openEditSupplier(supplier) },
                                            onDelete = {
                                                deleteConfirmRequest = MasterDeleteRequest(
                                                    typeName = "Supplier",
                                                    itemName = supplier.firmName.ifBlank { supplier.name },
                                                    onConfirm = { viewModel.deleteSupplier(supplier) }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        MasterTab.PRODUCTS -> {
                            val filtered = products.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.productCode.contains(searchQuery, ignoreCase = true) ||
                                        it.category.contains(searchQuery, ignoreCase = true) ||
                                        it.supplierName.contains(searchQuery, ignoreCase = true)
                            }
                            if (filtered.isEmpty()) {
                                MasterEmptyState(
                                    entityName = "Products",
                                    onAdd = { viewModel.openAddMaster(MasterTab.PRODUCTS) }
                                )
                            } else {
                                LazyColumn(
                                    state = productListState,
                                    contentPadding = PaddingValues(bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filtered, key = { it.id }) { product ->
                                        ProductCard(
                                            product = product,
                                            onClick = {
                                                detailViewItem = MasterDetailView(
                                                    title = product.name,
                                                    subtitle = "Code: ${product.productCode} • ${product.category}",
                                                    icon = Icons.Default.Inventory,
                                                    iconColor = Color(0xFF4F46E5),
                                                    details = listOf(
                                                        "Product Name" to product.name,
                                                        "Design Code" to product.productCode,
                                                        "Category" to product.category,
                                                        "Supplier" to product.supplierName,
                                                        "Default Rate" to "₹${product.defaultRate.toInt()} / pc",
                                                        "Case Pack" to "${product.defaultCaseSize} pcs",
                                                        "HSN Code" to product.hsnCode,
                                                        "Description" to product.description
                                                    ),
                                                    onEdit = { viewModel.openEditProduct(product) }
                                                )
                                            },
                                            onEdit = { viewModel.openEditProduct(product) },
                                            onDelete = {
                                                deleteConfirmRequest = MasterDeleteRequest(
                                                    typeName = "Product",
                                                    itemName = "${product.name} (${product.productCode})",
                                                    onConfirm = { viewModel.deleteProduct(product) }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        MasterTab.BRANDS -> {
                            val filtered = brands.filter {
                                it.brandName.contains(searchQuery, ignoreCase = true) ||
                                        it.category.contains(searchQuery, ignoreCase = true) ||
                                        it.manufacturerName.contains(searchQuery, ignoreCase = true)
                            }
                            if (filtered.isEmpty()) {
                                MasterEmptyState(
                                    entityName = "Garment Brands",
                                    onAdd = { viewModel.openAddMaster(MasterTab.BRANDS) }
                                )
                            } else {
                                LazyColumn(
                                    state = brandListState,
                                    contentPadding = PaddingValues(bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filtered, key = { it.id }) { brand ->
                                        BrandCard(
                                            brand = brand,
                                            onClick = {
                                                detailViewItem = MasterDetailView(
                                                    title = brand.brandName,
                                                    subtitle = "Brand • ${brand.category}",
                                                    icon = Icons.Default.Sell,
                                                    iconColor = Color(0xFFD97706),
                                                    details = listOf(
                                                        "Brand Name" to brand.brandName,
                                                        "Garment Category" to brand.category,
                                                        "Manufacturer / Mill" to brand.manufacturerName,
                                                        "Description" to brand.description
                                                    ),
                                                    onEdit = { viewModel.openEditBrand(brand) }
                                                )
                                            },
                                            onEdit = { viewModel.openEditBrand(brand) },
                                            onDelete = {
                                                deleteConfirmRequest = MasterDeleteRequest(
                                                    typeName = "Brand",
                                                    itemName = brand.brandName,
                                                    onConfirm = { viewModel.deleteBrand(brand) }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        MasterTab.TRANSPORTERS -> {
                            val filtered = transporters.filter {
                                it.transporterName.contains(searchQuery, ignoreCase = true) ||
                                        it.city.contains(searchQuery, ignoreCase = true) ||
                                        it.contactPerson.contains(searchQuery, ignoreCase = true) ||
                                        it.destinationsCovered.contains(searchQuery, ignoreCase = true)
                            }
                            if (filtered.isEmpty()) {
                                MasterEmptyState(
                                    entityName = "Transporters",
                                    onAdd = { viewModel.openAddMaster(MasterTab.TRANSPORTERS) }
                                )
                            } else {
                                LazyColumn(
                                    state = transporterListState,
                                    contentPadding = PaddingValues(bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filtered, key = { it.id }) { transporter ->
                                        TransporterCard(
                                            transporter = transporter,
                                            onClick = {
                                                val phones = listOfNotNull(
                                                    transporter.phone1.takeIf { it.isNotBlank() },
                                                    transporter.phone2.takeIf { it.isNotBlank() },
                                                    transporter.phone3.takeIf { it.isNotBlank() }
                                                ).joinToString(", ")
                                                detailViewItem = MasterDetailView(
                                                    title = transporter.transporterName,
                                                    subtitle = "Transporter • ${transporter.city}",
                                                    icon = Icons.Default.LocalShipping,
                                                    iconColor = Color(0xFF0891B2),
                                                    details = listOf(
                                                        "Transporter Name" to transporter.transporterName,
                                                        "Contact Person" to transporter.contactPerson,
                                                        "Hub / City" to transporter.city,
                                                        "Phone Numbers" to phones,
                                                        "Destinations / Routes" to transporter.destinationsCovered,
                                                        "Office Address" to transporter.officeAddress,
                                                        "GSTIN" to transporter.gstin
                                                    ),
                                                    onEdit = { viewModel.openEditTransporter(transporter) }
                                                )
                                            },
                                            onEdit = { viewModel.openEditTransporter(transporter) },
                                            onDelete = {
                                                deleteConfirmRequest = MasterDeleteRequest(
                                                    typeName = "Transporter",
                                                    itemName = transporter.transporterName,
                                                    onConfirm = { viewModel.deleteTransporter(transporter) }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        MasterTab.MARKETS -> {
                            val filtered = markets.filter {
                                it.marketName.contains(searchQuery, ignoreCase = true) ||
                                        it.city.contains(searchQuery, ignoreCase = true) ||
                                        it.area.contains(searchQuery, ignoreCase = true) ||
                                        it.marketType.contains(searchQuery, ignoreCase = true)
                            }
                            if (filtered.isEmpty()) {
                                MasterEmptyState(
                                    entityName = "Textile Markets",
                                    onAdd = { viewModel.openAddMaster(MasterTab.MARKETS) }
                                )
                            } else {
                                LazyColumn(
                                    state = marketListState,
                                    contentPadding = PaddingValues(bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filtered, key = { it.id }) { market ->
                                        MarketCard(
                                            market = market,
                                            onClick = {
                                                detailViewItem = MasterDetailView(
                                                    title = market.marketName,
                                                    subtitle = "${market.marketType} • ${market.city}",
                                                    icon = Icons.Default.LocationCity,
                                                    iconColor = Color(0xFF9333EA),
                                                    details = listOf(
                                                        "Market Name" to market.marketName,
                                                        "Market Type" to market.marketType,
                                                        "City" to market.city,
                                                        "Area / Sector" to market.area,
                                                        "Pincode" to market.pincode,
                                                        "Description" to market.description
                                                    ),
                                                    onEdit = { viewModel.openEditMarket(market) }
                                                )
                                            },
                                            onEdit = { viewModel.openEditMarket(market) },
                                            onDelete = {
                                                deleteConfirmRequest = MasterDeleteRequest(
                                                    typeName = "Market",
                                                    itemName = market.marketName,
                                                    onConfirm = { viewModel.deleteMarket(market) }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        MasterTab.EMPLOYEES -> {
                            val filtered = employees.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.employeeId.contains(searchQuery, ignoreCase = true) ||
                                        it.phone.contains(searchQuery, ignoreCase = true) ||
                                        it.assignedMarkets.contains(searchQuery, ignoreCase = true)
                            }
                            if (filtered.isEmpty()) {
                                MasterEmptyState(
                                    entityName = "Salesmen / Staff",
                                    onAdd = {
                                        if (isSuperAdmin) viewModel.openAddMaster(MasterTab.EMPLOYEES)
                                    }
                                )
                            } else {
                                LazyColumn(
                                    state = employeeListState,
                                    contentPadding = PaddingValues(bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filtered, key = { it.id }) { employee ->
                                        EmployeeCard(
                                            employee = employee,
                                            onClick = { viewModel.openEmployeeDetail(employee) },
                                            onEdit = { viewModel.openEditEmployee(employee) },
                                            onDelete = {
                                                deleteConfirmRequest = MasterDeleteRequest(
                                                    typeName = "Employee",
                                                    itemName = "${employee.name} (${employee.employeeId})",
                                                    onConfirm = { viewModel.deleteEmployee(employee) }
                                                )
                                            },
                                            isAdmin = isSuperAdmin
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // DELETE CONFIRMATION MODAL DIALOG
            // =========================================================================
            deleteConfirmRequest?.let { req ->
                AlertDialog(
                    onDismissRequest = { deleteConfirmRequest = null },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Delete ${req.typeName}?",
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to delete \"${req.itemName}\"? This record will be permanently removed.",
                            fontSize = 13.5.sp,
                            color = TextSecondary
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val act = req.onConfirm
                                deleteConfirmRequest = null
                                act()
                            }
                        ) {
                            Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteConfirmRequest = null }) {
                            Text("Cancel", color = NavyPrimary)
                        }
                    }
                )
            }

            // =========================================================================
            // IN-PLACE DETAIL VIEW MODAL DIALOG (For Product, Brand, Transporter, Market)
            // =========================================================================
            detailViewItem?.let { item ->
                MasterDetailDialog(
                    item = item,
                    onDismiss = { detailViewItem = null }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Master Hub Category Card
// -------------------------------------------------------------

@Composable
fun MasterHubCard(
    item: MasterCategoryItem,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    canAdd: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = item.iconBgColor,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.iconTintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = item.iconBgColor
                    ) {
                        Text(
                            text = "${item.count}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = item.iconTintColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canAdd) {
                    Surface(
                        shape = CircleShape,
                        color = NavyPrimary.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onQuickAdd() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Quick Add",
                            tint = NavyPrimary,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Directory",
                    tint = TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Master Empty State
// -------------------------------------------------------------

@Composable
fun MasterEmptyState(
    entityName: String,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = NavyPrimary.copy(alpha = 0.08f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = NavyPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "No $entityName Found",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = NavyPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Try adjusting your search or add a new record.",
            fontSize = 12.5.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = CircleShape,
            color = NavyPrimary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onAdd() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add New Record",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Master Detail Dialog (Modal View)
// -------------------------------------------------------------

@Composable
fun MasterDetailDialog(
    item: MasterDetailView,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = item.iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp,
                    color = NavyPrimary
                )
                if (item.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.details.forEach { (label, value) ->
                    if (value.isNotBlank()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                text = value,
                                fontSize = 13.5.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 6.dp),
                                color = Color(0xFFF1F5F9),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (item.onEdit != null) {
                TextButton(
                    onClick = {
                        onDismiss()
                        item.onEdit.invoke()
                    }
                ) {
                    Text("Edit Record", color = NavyPrimary, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

// -------------------------------------------------------------
// -------------------------------------------------------------
// Component: Master Card Action Buttons (Edit, Delete, Details)
// -------------------------------------------------------------

@Composable
fun MasterCardActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    canEdit: Boolean = true,
    canDelete: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (canEdit) {
            Surface(
                shape = CircleShape,
                color = NavyPrimary.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable { onEdit() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = NavyPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        if (canDelete) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFEE2E2),
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable { onDelete() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Surface(
            shape = CircleShape,
            color = Color(0xFFF1F5F9),
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Details",
                    tint = TextSecondary,
                    modifier = Modifier.size(12.5.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: CustomerCard (with View, Edit, Delete)
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerCard(
    customer: CustomerEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val displayName = customer.firmName.ifBlank { customer.name }
    val extraPhonesCount = listOfNotNull(
        customer.phone2.takeIf { it.isNotBlank() },
        customer.phone3.takeIf { it.isNotBlank() },
        customer.phone4.takeIf { it.isNotBlank() },
        customer.phone5.takeIf { it.isNotBlank() }
    ).size
    val marketDisplay = customer.marketArea.ifBlank {
        customer.markets.split(",").firstOrNull()?.trim() ?: customer.city.ifBlank { "Ahmedabad" }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header: Avatar + Title & Subtitle + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val custAvatarPhoto = customer.shopPhotoUri.ifBlank { customer.purchaserPhotoUri }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(32.dp)
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
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = if (customer.firmName.isNotBlank() && customer.name.isNotBlank() && customer.firmName != customer.name) {
                        "Owner: ${customer.name}"
                    } else if (customer.customerId.isNotBlank()) {
                        "ID: ${customer.customerId}"
                    } else {
                        "Customer"
                    }
                    Text(
                        text = sub,
                        fontSize = 10.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                MasterCardActions(
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onClick = onClick
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = Color(0xFFF1F5F9),
                thickness = 0.8.dp
            )

            // Info Row: Market / City on Left, Phone on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = marketDisplay,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = customer.phone,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                    if (extraPhonesCount > 0) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "+$extraPhonesCount",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                            )
                        }
                    }
                }
            }

            // Badges Row using FlowRow: Outlets, GSTIN, CustomerId
            val hasOutlets = customer.shopCount > 1
            val hasGstin = customer.gstin.isNotBlank()
            val hasCustomerId = customer.customerId.isNotBlank() && (customer.firmName.isNotBlank() && customer.name.isNotBlank() && customer.firmName != customer.name)

            if (hasOutlets || hasGstin || hasCustomerId) {
                Spacer(modifier = Modifier.height(5.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (hasOutlets) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "🏪 ${customer.shopCount} Outlets",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                    if (hasGstin) {
                        Surface(
                            color = NavyPrimary.copy(alpha = 0.07f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "GST: ${customer.gstin}",
                                fontSize = 9.5.sp,
                                color = NavyPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (hasCustomerId) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ID: ${customer.customerId}",
                                fontSize = 9.5.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: SupplierCard (with View, Edit, Delete)
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupplierCard(
    supplier: SupplierEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val displayName = supplier.firmName.ifBlank { supplier.name }
    val extraPhonesCount = listOfNotNull(
        supplier.phone2.takeIf { it.isNotBlank() },
        supplier.phone3.takeIf { it.isNotBlank() },
        supplier.phone4.takeIf { it.isNotBlank() }
    ).size
    val marketDisplay = supplier.marketArea.ifBlank {
        supplier.markets.split(",").firstOrNull()?.trim() ?: supplier.city.ifBlank { "Ahmedabad" }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            // Header: Avatar + Title & Subtitle + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val suppAvatarPhoto = supplier.shopPhotoUri.ifBlank { supplier.visitingCardPhotoUri }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFECFDF5),
                    modifier = Modifier.size(36.dp)
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
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = if (supplier.firmName.isNotBlank() && supplier.name.isNotBlank() && supplier.firmName != supplier.name) {
                        "Contact: ${supplier.name}"
                    } else if (supplier.brand.isNotBlank()) {
                        "Brand: ${supplier.brand}"
                    } else {
                        "Supplier / Mill"
                    }
                    Text(
                        text = sub,
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                MasterCardActions(
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onClick = onClick
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF1F5F9),
                thickness = 0.8.dp
            )

            // Info Row: Market / City on Left, Phone on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = marketDisplay,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = supplier.phone,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                    if (extraPhonesCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(color = Color(0xFFF1F5F9), shape = CircleShape) {
                            Text(
                                text = "+$extraPhonesCount",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Badges FlowRow: SupplierType, Case Pack, Shops, Categories
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SupplierTypeBadge(type = supplier.type)

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Pack: ${supplier.defaultCaseSize} pcs",
                        fontSize = 10.5.sp,
                        color = Color(0xFF0F766E),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (supplier.shopCount > 1) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${supplier.shopCount} Shops",
                            fontSize = 10.5.sp,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val cats = supplier.categories.ifBlank { supplier.garmentTypes }
                if (cats.isNotBlank()) {
                    Surface(
                        color = NavyPrimary.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = cats,
                            fontSize = 10.5.sp,
                            color = NavyPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: EmployeeCard (with View, Edit, Delete)
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmployeeCard(
    employee: EmployeeEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isAdmin: Boolean = false
) {
    val extraPhonesCount = listOfNotNull(
        employee.phone2.takeIf { it.isNotBlank() },
        employee.phone3.takeIf { it.isNotBlank() }
    ).size

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            // Header: Avatar + Title & Subtitle + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID: ${employee.employeeId} • ${employee.role}",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                MasterCardActions(
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onClick = onClick,
                    canEdit = isAdmin,
                    canDelete = isAdmin
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF1F5F9),
                thickness = 0.8.dp
            )

            // Info Row: Phone on Left, Role Badge on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = employee.phone,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                    if (extraPhonesCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(color = Color(0xFFF1F5F9), shape = CircleShape) {
                            Text(
                                text = "+$extraPhonesCount",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = if (employee.role.equals("Admin", ignoreCase = true)) Color(0xFFFEF3C7) else Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = employee.role,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (employee.role.equals("Admin", ignoreCase = true)) Color(0xFF92400E) else Color(0xFF1D4ED8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Badges Row: Assigned Territory / Emergency contact
            val hasTerritory = employee.assignedMarkets.isNotBlank()
            val hasEmergency = employee.emergencyContactPhone.isNotBlank()
            if (hasTerritory || hasEmergency) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hasTerritory) {
                        Surface(
                            color = NavyPrimary.copy(alpha = 0.07f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Territory: ${employee.assignedMarkets}",
                                fontSize = 10.5.sp,
                                color = NavyPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (hasEmergency) {
                        Text(
                            text = "Emergency: ${employee.emergencyContactPhone}",
                            fontSize = 10.5.sp,
                            color = Color(0xFFD97706),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: ProductCard (with View, Edit, Delete)
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductCard(
    product: ProductEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            // Header: Product Code Badge + Product Name & Category + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldAccent.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Code: ${product.productCode} • ${product.category}",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                MasterCardActions(
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onClick = onClick
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF1F5F9),
                thickness = 0.8.dp
            )

            // Info Row: Rate on Left, Case & HSN on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₹${product.defaultRate.toInt()} / pc",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = NavyPrimary
                )

                Text(
                    text = "Case: ${product.defaultCaseSize} pcs${if (product.hsnCode.isNotBlank()) " • HSN: ${product.hsnCode}" else ""}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Badges Row
            if (product.supplierName.isNotBlank() || product.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (product.supplierName.isNotBlank()) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Supplier: ${product.supplierName}",
                                fontSize = 10.5.sp,
                                color = NavyPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (product.description.isNotBlank()) {
                        Text(
                            text = product.description,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: BrandCard (with View, Edit, Delete)
// -------------------------------------------------------------

@Composable
fun BrandCard(
    brand: BrandEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            // Header: Avatar + Title & Subtitle + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    modifier = Modifier.size(36.dp)
                ) {
                    if (brand.logoPhotoUri.isNotBlank()) {
                        AsyncImage(
                            model = brand.logoPhotoUri,
                            contentDescription = brand.brandName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Sell,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = brand.brandName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = brand.category.ifBlank { "Garment Brand" },
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                MasterCardActions(
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onClick = onClick
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF1F5F9),
                thickness = 0.8.dp
            )

            // Info Row: Manufacturer / Mill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mill / Mfr: ${brand.manufacturerName.ifBlank { "Direct Label" }}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (brand.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = brand.description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: TransporterCard (with View, Edit, Delete)
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransporterCard(
    transporter: TransporterEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val extraPhonesCount = listOfNotNull(
        transporter.phone2.takeIf { it.isNotBlank() },
        transporter.phone3.takeIf { it.isNotBlank() }
    ).size

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            // Header: Avatar + Title & Subtitle + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFECFEFF),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = Color(0xFF0891B2),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transporter.transporterName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = if (transporter.contactPerson.isNotBlank()) {
                        "${transporter.contactPerson} • ${transporter.city}"
                    } else {
                        transporter.city.ifBlank { "Transport Desk" }
                    }
                    Text(
                        text = sub,
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                MasterCardActions(
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onClick = onClick
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF1F5F9),
                thickness = 0.8.dp
            )

            // Info Row: City on Left, Phone on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = transporter.city.ifBlank { "Ahmedabad" },
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                if (transporter.phone1.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = transporter.phone1,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                        if (extraPhonesCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(color = Color(0xFFF1F5F9), shape = CircleShape) {
                                Text(
                                    text = "+$extraPhonesCount",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Badges / Routes / Office
            val hasRoutes = transporter.destinationsCovered.isNotBlank()
            val hasOffice = transporter.officeAddress.isNotBlank()
            if (hasRoutes || hasOffice) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hasRoutes) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Routes: ${transporter.destinationsCovered}",
                                fontSize = 10.5.sp,
                                color = NavyPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (hasOffice) {
                        Text(
                            text = "Office: ${transporter.officeAddress}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: MarketCard (with View, Edit, Delete)
// -------------------------------------------------------------

@Composable
fun MarketCard(
    market: MarketEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            // Header: Avatar + Title & Subtitle + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFAF5FF),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LocationCity,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = market.marketName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val loc = "${market.city}${if (market.area.isNotBlank()) ", ${market.area}" else ""}${if (market.pincode.isNotBlank()) " - ${market.pincode}" else ""}"
                    Text(
                        text = loc,
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                MasterCardActions(
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onClick = onClick
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF1F5F9),
                thickness = 0.8.dp
            )

            // Info Row: Market Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = market.marketType.ifBlank { "Textile Market" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }

                if (market.pincode.isNotBlank()) {
                    Text(
                        text = "PIN: ${market.pincode}",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }
            }

            if (market.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = market.description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
