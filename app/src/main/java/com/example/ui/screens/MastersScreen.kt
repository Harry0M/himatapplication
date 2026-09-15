package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun MastersScreen(
    viewModel: HimatViewModel,
    initialTab: MasterTab = MasterTab.CUSTOMERS
) {
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()

    val availableTabs = remember {
        listOf(
            MasterTab.CUSTOMERS,
            MasterTab.SUPPLIERS,
            MasterTab.BRANDS,
            MasterTab.TRANSPORTERS,
            MasterTab.EMPLOYEES,
            MasterTab.MARKETS,
            MasterTab.PRODUCTS
        )
    }

    var selectedTab by remember { mutableStateOf(initialTab) }
    if (!availableTabs.contains(selectedTab)) {
        selectedTab = MasterTab.CUSTOMERS
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var supplierTypeFilter by remember { mutableStateOf("All") } // "All", "Manufacturer", "Wholesaler"

    val customerListState = rememberLazyListState()
    val supplierListState = rememberLazyListState()
    val brandListState = rememberLazyListState()
    val transporterListState = rememberLazyListState()
    val employeeListState = rememberLazyListState()
    val marketListState = rememberLazyListState()
    val productListState = rememberLazyListState()

    val currentListState = when (selectedTab) {
        MasterTab.CUSTOMERS -> customerListState
        MasterTab.SUPPLIERS -> supplierListState
        MasterTab.BRANDS -> brandListState
        MasterTab.TRANSPORTERS -> transporterListState
        MasterTab.EMPLOYEES -> employeeListState
        MasterTab.MARKETS -> marketListState
        MasterTab.PRODUCTS -> productListState
    }

    var isHeaderVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            private var accumulatedDelta = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    if (delta < 0) {
                        // Scrolling DOWN (swiping up) -> collapse header
                        if (accumulatedDelta > 0) accumulatedDelta = 0f
                        accumulatedDelta += delta
                        if (accumulatedDelta < -25f) {
                            isHeaderVisible = false
                        }
                    } else if (delta > 0) {
                        // Scrolling UP (pulling down / pushing back) -> expand header
                        if (accumulatedDelta < 0) accumulatedDelta = 0f
                        accumulatedDelta += delta
                        if (accumulatedDelta > 18f) {
                            isHeaderVisible = true
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Keep header visible at the top of the list, when actively searching, or when switching tabs
    LaunchedEffect(currentListState.firstVisibleItemIndex, currentListState.firstVisibleItemScrollOffset, searchQuery, isSearchVisible) {
        if (isSearchVisible || searchQuery.isNotBlank() || (currentListState.firstVisibleItemIndex == 0 && currentListState.firstVisibleItemScrollOffset <= 15)) {
            isHeaderVisible = true
        }
    }

    LaunchedEffect(selectedTab) {
        isHeaderVisible = true
    }

    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.visibleSuppliers.collectAsStateWithLifecycle()
    val brands by viewModel.visibleBrands.collectAsStateWithLifecycle()
    val transporters by viewModel.visibleTransporters.collectAsStateWithLifecycle()
    val markets by viewModel.visibleMarkets.collectAsStateWithLifecycle()
    val products by viewModel.visibleProducts.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()


    Scaffold(
        containerColor = Color(0xFFF6F8FB),
        floatingActionButton = {
            if (selectedTab != MasterTab.EMPLOYEES || isSuperAdmin) {
                FloatingActionButton(
                    onClick = {
                        viewModel.openAddMaster(selectedTab)
                    },
                    containerColor = NavyPrimary,
                    contentColor = GoldAccent,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
                .nestedScroll(nestedScrollConnection)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            AnimatedVisibility(
                visible = isHeaderVisible,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 200)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 180)
                )
            ) {
                Column {
                    // 1. Top Header (Flat, borderless, matching VisitDetailScreen style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 0.dp,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Business Masters",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = when (selectedTab) {
                            MasterTab.CUSTOMERS -> "${customers.size} registered customers"
                            MasterTab.SUPPLIERS -> "${suppliers.size} manufacturers & wholesalers"
                            MasterTab.BRANDS -> "${brands.size} garment brands"
                            MasterTab.TRANSPORTERS -> "${transporters.size} transport partners"
                            MasterTab.EMPLOYEES -> "${employees.size} registered salesmen"
                            MasterTab.MARKETS -> "${markets.size} textile markets"
                            MasterTab.PRODUCTS -> "${products.size} active catalog items"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Tally XML Export Button
                val context = LocalContext.current
                if (selectedTab == MasterTab.CUSTOMERS || selectedTab == MasterTab.SUPPLIERS) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F766E), // Dark Teal
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                if (selectedTab == MasterTab.CUSTOMERS) {
                                    viewModel.exportCustomersToTallyXml(context)
                                } else {
                                    viewModel.exportSuppliersToTallyXml(context)
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "Tally XML",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tally XML",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Search Toggle Button in top-right header corner
                Surface(
                    shape = CircleShape,
                    color = if (isSearchVisible || searchQuery.isNotBlank()) NavyPrimary else Color.White,
                    border = BorderStroke(1.dp, if (isSearchVisible || searchQuery.isNotBlank()) NavyPrimary else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .size(36.dp)
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Quick Action Add Pill
                if (selectedTab != MasterTab.EMPLOYEES || isSuperAdmin) {
                    Surface(
                        shape = CircleShape,
                        color = NavyPrimary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                viewModel.openAddMaster(selectedTab)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                    }
                }
            }

            // 2. Selection Chips in Material Colors (replacing TabRow) - fully rounded pill shape
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableTabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val (label, icon, count) = when (tab) {
                        MasterTab.CUSTOMERS -> Triple("Customers", Icons.Default.People, customers.size)
                        MasterTab.SUPPLIERS -> Triple("Suppliers", Icons.Default.Store, suppliers.size)
                        MasterTab.BRANDS -> Triple("Brands", Icons.Default.Sell, brands.size)
                        MasterTab.TRANSPORTERS -> Triple("Transporters", Icons.Default.LocalShipping, transporters.size)
                        MasterTab.EMPLOYEES -> Triple("Salesmen", Icons.Default.Person, employees.size)
                        MasterTab.MARKETS -> Triple("Markets", Icons.Default.LocationCity, markets.size)
                        MasterTab.PRODUCTS -> Triple("Products", Icons.Default.Inventory, products.size)
                    }

                    Surface(
                        color = if (isSelected) NavyPrimary else Color.White,
                        shape = CircleShape,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NavyPrimary else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { selectedTab = tab }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) GoldAccent else NavyPrimary
                            )
                            Text(
                                text = "$label ($count)",
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }

            // 3. Clean Rounded Corner Pill Search Bar (Animated visibility)
            AnimatedVisibility(
                visible = isSearchVisible || searchQuery.isNotBlank(),
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(180))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search",
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
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
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

            // Supplier-specific Type filter (Manufacturer vs Wholesaler)
            if (selectedTab == MasterTab.SUPPLIERS) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
                }
            }

            when (selectedTab) {
                MasterTab.CUSTOMERS -> {
                    val filtered = customers.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                                it.city.contains(searchQuery, ignoreCase = true) ||
                                it.phone.contains(searchQuery, ignoreCase = true)
                    }
                    LazyColumn(
                        state = customerListState,
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered) { customer ->
                            CustomerCard(
                                customer = customer,
                                onClick = { viewModel.openCustomerDetail(customer) },
                                onEdit = { viewModel.openEditCustomer(customer) },
                                onDelete = { viewModel.deleteCustomer(customer) }
                            )
                        }
                    }
                }

                MasterTab.SUPPLIERS -> {
                    val filtered = suppliers.filter {
                        val matchesSearch = it.name.contains(searchQuery, ignoreCase = true) ||
                                it.marketArea.contains(searchQuery, ignoreCase = true) ||
                                it.brand.contains(searchQuery, ignoreCase = true)
                        val matchesType = when (supplierTypeFilter) {
                            "Manufacturer" -> it.type.equals("Manufacturer", ignoreCase = true)
                            "Wholesaler" -> it.type.equals("Wholesaler", ignoreCase = true)
                            else -> true
                        }
                        matchesSearch && matchesType
                    }
                    LazyColumn(
                        state = supplierListState,
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered) { supplier ->
                            SupplierCard(
                                supplier = supplier,
                                onClick = { viewModel.openSupplierDetail(supplier) },
                                onEdit = { viewModel.openEditSupplier(supplier) },
                                onDelete = { viewModel.deleteSupplier(supplier) }
                            )
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
                    LazyColumn(
                        state = productListState,
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered) { product ->
                            ProductCard(
                                product = product,
                                onEdit = { viewModel.openEditProduct(product) },
                                onDelete = { viewModel.deleteProduct(product) }
                            )
                        }
                    }
                }

                MasterTab.EMPLOYEES -> {
                    val filtered = employees.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                                it.employeeId.contains(searchQuery, ignoreCase = true) ||
                                it.phone.contains(searchQuery, ignoreCase = true)
                    }
                    LazyColumn(
                        state = employeeListState,
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered) { employee ->
                            EmployeeCard(
                                employee = employee,
                                onClick = { viewModel.openEmployeeDetail(employee) },
                                onEdit = { viewModel.openEditEmployee(employee) },
                                onDelete = { viewModel.deleteEmployee(employee) },
                                isAdmin = isSuperAdmin
                            )
                        }
                    }
                }

                MasterTab.BRANDS -> {
                    val filtered = brands.filter {
                        it.brandName.contains(searchQuery, ignoreCase = true) ||
                                it.category.contains(searchQuery, ignoreCase = true) ||
                                it.manufacturerName.contains(searchQuery, ignoreCase = true)
                    }
                    LazyColumn(
                        state = brandListState,
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered) { brand ->
                            BrandCard(
                                brand = brand,
                                onEdit = { viewModel.openEditBrand(brand) },
                                onDelete = { viewModel.deleteBrand(brand) }
                            )
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
                    LazyColumn(
                        state = transporterListState,
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered) { transporter ->
                            TransporterCard(
                                transporter = transporter,
                                onEdit = { viewModel.openEditTransporter(transporter) },
                                onDelete = { viewModel.deleteTransporter(transporter) }
                            )
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
                    LazyColumn(
                        state = marketListState,
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered) { market ->
                            MarketCard(
                                market = market,
                                onEdit = { viewModel.openEditMarket(market) },
                                onDelete = { viewModel.deleteMarket(market) }
                            )
                        }
                    }
                }
            }

        }
    }
}

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
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    if (customer.customerId.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("(${customer.customerId})", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                if (customer.firmName.isNotBlank() && customer.name.isNotBlank() && customer.firmName != customer.name) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Owner: ${customer.name}", fontSize = 11.5.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(marketDisplay, fontSize = 12.sp, color = TextSecondary)
                    Text(" • ", color = TextSecondary)
                    Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(customer.phone, fontSize = 12.sp, color = TextSecondary)
                    if (extraPhonesCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "+$extraPhonesCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (customer.shopCount > 1) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "🏪 ${customer.shopCount} Outlets",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (customer.gstin.isNotBlank()) {
                        Text("GSTIN: ${customer.gstin}", fontSize = 11.sp, color = NavyPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Details",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

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
        supplier.phone4.takeIf { it.isNotBlank() },
        supplier.phone5.takeIf { it.isNotBlank() }
    ).size
    val marketDisplay = supplier.marketArea.ifBlank {
        supplier.markets.split(",").firstOrNull()?.trim() ?: supplier.city.ifBlank { "Ahmedabad" }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    SupplierTypeBadge(type = supplier.type)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(marketDisplay, fontSize = 11.5.sp, color = TextSecondary)
                    if (supplier.phone.isNotBlank()) {
                        Text(" • ", color = TextSecondary)
                        Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(supplier.phone, fontSize = 11.5.sp, color = TextSecondary)
                        if (extraPhonesCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(color = Color(0xFFF1F5F9), shape = CircleShape) {
                                Text(
                                    text = "+$extraPhonesCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (supplier.brand.isNotBlank()) {
                        Text("Brand: ${supplier.brand}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NavyPrimary)
                        Text(" • ", color = TextSecondary)
                    }
                    Text("Case size: ${supplier.defaultCaseSize} pcs", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Medium)
                    if (supplier.shopCount > 1) {
                        Text(" • ", color = TextSecondary)
                        Text("${supplier.shopCount} Shops", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                    }
                }
                val cats = supplier.categories.ifBlank { supplier.garmentTypes }
                if (cats.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Categories: $cats",
                        fontSize = 11.sp,
                        color = NavyPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Details",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

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
        employee.phone3.takeIf { it.isNotBlank() },
        employee.phone4.takeIf { it.isNotBlank() },
        employee.phone5.takeIf { it.isNotBlank() }
    ).size

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Surface(
                        color = if (employee.role == "Admin") NavyPrimary else GoldAccent,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = employee.role,
                            color = if (employee.role == "Admin") Color.White else NavyPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ID: ${employee.employeeId} • ${employee.phone}",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                    if (extraPhonesCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(color = Color(0xFFF1F5F9), shape = CircleShape) {
                            Text(
                                text = "+$extraPhonesCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                if (employee.assignedMarkets.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Territory: ${employee.assignedMarkets}",
                        fontSize = 11.sp,
                        color = NavyPrimary,
                        maxLines = 1
                    )
                }
                if (employee.emergencyContactPhone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Emergency: ${employee.emergencyContactName} (${employee.emergencyContactPhone})",
                        fontSize = 10.5.sp,
                        color = Color(0xFFD97706)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAdmin) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Details",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = product.productCode,
                            color = NavyPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Supplier: ${product.supplierName}",
                    fontSize = 12.5.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Category: ${product.category}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Default Rate: ₹${product.defaultRate.toInt()}/pc",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )
                Text(
                    text = "Case: ${product.defaultCaseSize} pcs • HSN: ${product.hsnCode}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (product.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.description,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun BrandCard(
    brand: BrandEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = NavyPrimary.copy(alpha = 0.08f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Sell, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(brand.brandName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        if (brand.category.isNotBlank()) {
                            Text(brand.category, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (brand.manufacturerName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Manufacturer / Mill: ${brand.manufacturerName}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            if (brand.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = brand.description,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun TransporterCard(
    transporter: TransporterEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F766E).copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(transporter.transporterName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Text(
                            text = if (transporter.contactPerson.isNotBlank()) "${transporter.contactPerson} • ${transporter.city}" else transporter.city,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (transporter.phone1.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    val phones = listOfNotNull(
                        transporter.phone1.takeIf { it.isNotBlank() },
                        transporter.phone2.takeIf { it.isNotBlank() },
                        transporter.phone3.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    Text(phones, fontSize = 12.sp, color = TextSecondary)
                }
            }

            if (transporter.destinationsCovered.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Routes: ${transporter.destinationsCovered}",
                        fontSize = 11.5.sp,
                        color = NavyPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (transporter.officeAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Office: ${transporter.officeAddress}",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun MarketCard(
    market: MarketEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = NavyPrimary.copy(alpha = 0.08f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocationCity, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(market.marketName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Text(
                            text = "${market.city}${if (market.area.isNotBlank()) ", ${market.area}" else ""}${if (market.pincode.isNotBlank()) " - ${market.pincode}" else ""}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = market.marketType,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF92400E),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            if (market.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = market.description,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

