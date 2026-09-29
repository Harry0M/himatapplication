package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import com.example.ui.dialogs.CustomerRequestsDialog
import com.example.ui.dialogs.SupplierRegistrationDialog
import com.example.ui.dialogs.SupplierRequestsDialog
import com.example.ui.dialogs.WhatsAppInviteDialog
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.ListRow
import com.example.ui.components.StatusPill
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.ui.viewmodel.MasterTab
import com.example.util.ShareUtil

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
    initialTab: MasterTab? = null,
    onOpenCustomer: (CustomerEntity) -> Unit = { viewModel.openCustomerDetail(it) },
    onOpenSupplier: (SupplierEntity) -> Unit = { viewModel.openSupplierDetail(it) },
    onOpenEmployee: (EmployeeEntity) -> Unit = { viewModel.openEmployeeDetail(it) },
    onOpenProduct: (ProductEntity) -> Unit = { viewModel.openProductDetail(it) },
    onOpenBrand: (BrandEntity) -> Unit = { viewModel.openBrandDetail(it) },
    onOpenTransporter: (TransporterEntity) -> Unit = { viewModel.openTransporterDetail(it) },
    onOpenMarket: (MarketEntity) -> Unit = { viewModel.openMarketDetail(it) }
) {
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()

    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.visibleSuppliers.collectAsStateWithLifecycle()
    val brands by viewModel.visibleBrands.collectAsStateWithLifecycle()
    val transporters by viewModel.visibleTransporters.collectAsStateWithLifecycle()
    val markets by viewModel.visibleMarkets.collectAsStateWithLifecycle()
    val products by viewModel.visibleProducts.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val subAgents by viewModel.subAgents.collectAsStateWithLifecycle()
    val subAgentCount = subAgents.count { !it.status.equals("Deactivated", true) }
    val pendingRequestsCount by viewModel.pendingRegistrationRequestsCount.collectAsStateWithLifecycle()
    val pendingSupplierRequestsCount by viewModel.pendingSupplierRegistrationRequestsCount.collectAsStateWithLifecycle()
    val pendingCountByCustomer by viewModel.pendingCountByCustomer.collectAsStateWithLifecycle()
    var showRequestsDialog by remember { mutableStateOf(false) }
    var showSupplierRequestsDialog by remember { mutableStateOf(false) }
    var showSupplierRegistrationDialog by remember { mutableStateOf(false) }
    var inviteShareType by remember { mutableStateOf<MasterTab?>(null) }

    // Active master list (null = Masters hub). Lives in the ViewModel so it survives opening a
    // record and coming back; hub -> list -> back is a normal back-stack step.
    val vmCategory by viewModel.mastersCategory.collectAsStateWithLifecycle()
    val selectedCategory: MasterTab? = vmCategory ?: initialTab
    val isAgentUser by viewModel.isAgentUser.collectAsStateWithLifecycle()
    val currentEmployeeForLink by viewModel.currentEmployee.collectAsStateWithLifecycle()
    // Sub Agents share their personal link so new customers are linked to them
    val agentLinkId = if (isAgentUser) currentEmployeeForLink?.id else null

    // Search and Filter States
    var hubSearchQuery by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var supplierTypeFilter by remember { mutableStateOf("All") } // "All", "Manufacturer", "Trading", "Distributor", "Fabric"
    var supplierCategoryFilter by remember { mutableStateOf("All") }
    var customerGarmentFilter by remember { mutableStateOf("All") }
    var customerCreditFilter by remember { mutableStateOf("All") }

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

    // The title, search box and filters stay put while the list scrolls: the header used to slide
    // away and come back, which made the screen jump and hid the search box mid-typing.
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
                title = "Staff Master",
                subtitle = "Salesmen & admins: logins, trips, orders and customers",
                icon = Icons.Default.Person,
                count = employees.size,
                iconBgColor = Color(0xFFF1F5F9),
                iconTintColor = NavyPrimary,
                tag = "Staff"
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // Show FAB on Specific Master screen for instant record creation
            val currentTab = selectedCategory
            if (currentTab != null && !isAgentUser && (currentTab != MasterTab.EMPLOYEES || isSuperAdmin)) {
                FloatingActionButton(
                    onClick = {
                        viewModel.openAddMaster(currentTab)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
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
                .background(MaterialTheme.colorScheme.background)
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
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Business Masters",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "Select a master directory to manage records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Total Entity Count Pill
                        val totalRecords = customers.size + suppliers.size + products.size + brands.size + transporters.size + markets.size + employees.size
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$totalRecords Total",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Compact Pill Search Bar on Masters Directory
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                if (hubSearchQuery.isEmpty()) {
                                    Text(
                                        text = "Search master categories...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = hubSearchQuery,
                                    onValueChange = { hubSearchQuery = it },
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
                            if (hubSearchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0))
                                        .clickable { hubSearchQuery = "" },
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

                    // Master Categories List (Sub Agents sit right after Suppliers)
                    val matchesHubSearch: (MasterCategoryItem) -> Boolean = {
                        it.title.contains(hubSearchQuery, ignoreCase = true) ||
                                it.subtitle.contains(hubSearchQuery, ignoreCase = true) ||
                                it.tag.contains(hubSearchQuery, ignoreCase = true)
                    }
                    val filteredCategories = categories
                        .filter { !isAgentUser || it.tab == MasterTab.CUSTOMERS }
                        .filter(matchesHubSearch)
                    val subAgentCard = MasterCategoryItem(
                        tab = MasterTab.EMPLOYEES,
                        title = "Sub Agent Master",
                        subtitle = "Outside agents who bring customers, with their customers & orders",
                        icon = Icons.Default.Handshake,
                        count = subAgentCount,
                        iconBgColor = Color(0xFFFFF7ED),
                        iconTintColor = Color(0xFFB45309),
                        tag = "Agents"
                    )
                    val showSubAgentCard = !isAgentUser && matchesHubSearch(subAgentCard)

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        filteredCategories.forEach { item ->
                            item(key = "hub_${item.tab.name}") {
                                MasterHubCard(
                                    item = item,
                                    onClick = {
                                        searchQuery = ""
                                        viewModel.openMasterList(item.tab)
                                    },
                                    onQuickAdd = {
                                        viewModel.openAddMaster(item.tab)
                                    },
                                    canAdd = !isAgentUser && (item.tab != MasterTab.EMPLOYEES || isSuperAdmin)
                                )
                            }
                            if (item.tab == MasterTab.SUPPLIERS && showSubAgentCard) {
                                item(key = "hub_sub_agents") {
                                    MasterHubCard(
                                        item = subAgentCard,
                                        onClick = { viewModel.openSubAgents() },
                                        onQuickAdd = { viewModel.openAddSubAgent() },
                                        canAdd = true
                                    )
                                }
                            }
                        }
                        if (showSubAgentCard && filteredCategories.none { it.tab == MasterTab.SUPPLIERS }) {
                            item(key = "hub_sub_agents_end") {
                                MasterHubCard(
                                    item = subAgentCard,
                                    onClick = { viewModel.openSubAgents() },
                                    onQuickAdd = { viewModel.openAddSubAgent() },
                                    canAdd = true
                                )
                            }
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
                        .padding(horizontal = 16.dp, vertical = 10.dp)
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
                                    .clickable { viewModel.navigateBack() }
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
                                    style = MaterialTheme.typography.titleMedium,
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
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Circular Customer Registration Requests Button with Red-dot indicator (Admin only)
                            if (activeTab == MasterTab.CUSTOMERS && isSuperAdmin) {
                                Box(modifier = Modifier.padding(end = 6.dp)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (pendingRequestsCount > 0) Color(0xFFFEF3C7) else Color.White,
                                        border = BorderStroke(1.dp, if (pendingRequestsCount > 0) Color(0xFFF59E0B) else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .clickable { showRequestsDialog = true }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PersonAdd,
                                                contentDescription = "Registration Requests",
                                                tint = if (pendingRequestsCount > 0) Color(0xFFB45309) else NavyPrimary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }

                                    // Red-dot notification indicator
                                    if (pendingRequestsCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .align(Alignment.TopEnd)
                                                .clip(CircleShape)
                                                .background(Color(0xFFDC2626))
                                        )
                                    }
                                }
                            }

                            // Circular Supplier Registration Requests Button (Supplier Master)
                            if (activeTab == MasterTab.SUPPLIERS && isSuperAdmin) {
                                Box(contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (pendingSupplierRequestsCount > 0) Color(0xFFFEF3C7) else Color(0xFFECFDF5),
                                        border = BorderStroke(1.dp, if (pendingSupplierRequestsCount > 0) Color(0xFFF59E0B) else Color(0xFFA7F3D0)),
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .clickable { showSupplierRequestsDialog = true }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PersonAdd,
                                                contentDescription = "Supplier Registration Requests",
                                                tint = if (pendingSupplierRequestsCount > 0) Color(0xFFB45309) else Color(0xFF059669),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }

                                    // Red-dot notification indicator
                                    if (pendingSupplierRequestsCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .align(Alignment.TopEnd)
                                                .clip(CircleShape)
                                                .background(Color(0xFFDC2626))
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

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
                        }
                        // Compact Pill Search Bar for Current Master
                        AnimatedVisibility(
                            visible = isSearchVisible || searchQuery.isNotBlank(),
                            enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                            exit = shrinkVertically(tween(180)) + fadeOut(tween(160))
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
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
                                                    text = "Search in ${currentCategoryMeta.title}...",
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
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Clear",
                                                    modifier = Modifier.size(11.dp),
                                                    tint = Color(0xFF475569)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Customer Filters (Garments & Terms)
                        if (activeTab == MasterTab.CUSTOMERS) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Garment filter
                                listOf("All", "Gents", "Ladies", "Kids", "Handloom", "Family Shop").forEach { garment ->
                                    val isSelected = customerGarmentFilter == garment
                                    Surface(
                                        color = if (isSelected) NavyPrimary else Color.White,
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { customerGarmentFilter = garment }
                                    ) {
                                        Text(
                                            text = garment,
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Divider dot
                                Text("•", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)

                                // Credit type
                                listOf("All", "Cash", "Credit").forEach { term ->
                                    val isSelected = customerCreditFilter == term
                                    Surface(
                                        color = if (isSelected) Color(0xFF4338CA) else Color.White,
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF4338CA) else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { customerCreditFilter = term }
                                    ) {
                                        Text(
                                            text = if (term == "All") "All Terms" else term,
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Supplier Type & Category Filter
                        if (activeTab == MasterTab.SUPPLIERS) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("All", "Manufacturer", "Trading", "Distributor", "Fabric").forEach { type ->
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
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Divider dot
                                Text("•", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)

                                listOf("All", "Ladies", "Gents", "Kids", "Handloom").forEach { cat ->
                                    val isSelected = supplierCategoryFilter == cat
                                    Surface(
                                        color = if (isSelected) Color(0xFF0F766E) else Color.White,
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F766E) else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { supplierCategoryFilter = cat }
                                    ) {
                                        Text(
                                            text = if (cat == "All") "All Cats" else cat,
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // -------------------------------------------------------------
                    // Entity Record Lists (with See, Edit, Delete)
                    // -------------------------------------------------------------
                    when (activeTab) {
                        MasterTab.CUSTOMERS -> {
                            val filtered = customers.filter {
                                val matchesSearch = it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.firmName.contains(searchQuery, ignoreCase = true) ||
                                        it.city.contains(searchQuery, ignoreCase = true) ||
                                        it.phone.contains(searchQuery, ignoreCase = true) ||
                                        it.gstin.contains(searchQuery, ignoreCase = true) ||
                                        it.workingMarkets.contains(searchQuery, ignoreCase = true) ||
                                        it.marketArea.contains(searchQuery, ignoreCase = true) ||
                                        it.customerId.contains(searchQuery, ignoreCase = true)

                                val matchesGarment = if (customerGarmentFilter == "All") true else {
                                    val cats = "${it.preferredCategories} ${it.garmentTypes}"
                                    cats.contains(customerGarmentFilter, ignoreCase = true)
                                }

                                val matchesCredit = if (customerCreditFilter == "All") true else {
                                    it.customerType.equals(customerCreditFilter, ignoreCase = true)
                                }

                                matchesSearch && matchesGarment && matchesCredit
                            }
                            if (filtered.isEmpty()) {
                                MasterEmptyState(
                                    entityName = "Customers",
                                    onAdd = { viewModel.openAddMaster(MasterTab.CUSTOMERS) }
                                )
                            } else {
                                LazyColumn(
                                    state = customerListState,
                                    contentPadding = PaddingValues(bottom = 88.dp)
                                ) {
                                    item(key = "whatsapp_reg_banner") {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    inviteShareType = MasterTab.CUSTOMERS
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF25D366),
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Share,
                                                            contentDescription = "Share Registration Link",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Share Registration Link via WhatsApp",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF166534)
                                                    )
                                                    Text(
                                                        text = "Send web portal link with SMS OTP to new retail buyers",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF15803D)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Share ➜",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF166534)
                                                )
                                            }
                                        }
                                    }

                                    items(filtered, key = { it.id }) { customer ->
                                        CustomerCard(
                                            customer = customer,
                                            pendingCount = pendingCountByCustomer[customer.id.toLong()] ?: 0,
                                            onClick = { onOpenCustomer(customer) },
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
                                        it.gstin.contains(searchQuery, ignoreCase = true) ||
                                        it.productsMade.contains(searchQuery, ignoreCase = true)

                                val matchesType = when (supplierTypeFilter) {
                                    "Manufacturer" -> it.type.contains("Manufacturer", ignoreCase = true) || it.type.contains("Mill", ignoreCase = true)
                                    "Trading" -> it.type.contains("Trading", ignoreCase = true) || it.type.contains("Wholesaler", ignoreCase = true) || it.type.contains("Trader", ignoreCase = true)
                                    "Distributor" -> it.type.contains("Distributor", ignoreCase = true) || it.type.contains("Dealer", ignoreCase = true)
                                    "Fabric" -> it.type.contains("Fabric", ignoreCase = true) || it.type.contains("Processor", ignoreCase = true) || it.type.contains("Jobworker", ignoreCase = true)
                                    else -> true
                                }

                                val matchesCategory = if (supplierCategoryFilter == "All") true else {
                                    val cats = "${it.categories} ${it.subCategories} ${it.productsMade} ${it.garmentTypes}"
                                    cats.contains(supplierCategoryFilter, ignoreCase = true)
                                }

                                matchesSearch && matchesType && matchesCategory
                            }
                            if (filtered.isEmpty()) {
                                MasterEmptyState(
                                    entityName = "Suppliers & Mills",
                                    onAdd = { viewModel.openAddMaster(MasterTab.SUPPLIERS) }
                                )
                            } else {
                                LazyColumn(
                                    state = supplierListState,
                                    contentPadding = PaddingValues(bottom = 88.dp)
                                ) {
                                    item(key = "whatsapp_supplier_reg_banner") {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    inviteShareType = MasterTab.SUPPLIERS
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF25D366),
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Share,
                                                            contentDescription = "Share Supplier Registration Link",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Share Registration Link via WhatsApp",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF166534)
                                                    )
                                                    Text(
                                                        text = "Send web portal link to new textile mills & fabric suppliers",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF15803D)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Share ➜",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF166534)
                                                )
                                            }
                                        }
                                    }

                                    items(filtered, key = { it.id }) { supplier ->
                                        SupplierCard(
                                            supplier = supplier,
                                            onClick = { onOpenSupplier(supplier) },
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
                                    contentPadding = PaddingValues(bottom = 88.dp)
                                ) {
                                    items(filtered, key = { it.id }) { product ->
                                        ProductCard(
                                            product = product,
                                            onClick = { onOpenProduct(product) },
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
                                    contentPadding = PaddingValues(bottom = 88.dp)
                                ) {
                                    items(filtered, key = { it.id }) { brand ->
                                        BrandCard(
                                            brand = brand,
                                            onClick = { onOpenBrand(brand) },
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
                                    contentPadding = PaddingValues(bottom = 88.dp)
                                ) {
                                    items(filtered, key = { it.id }) { transporter ->
                                        TransporterCard(
                                            transporter = transporter,
                                            onClick = { onOpenTransporter(transporter) },
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
                                    contentPadding = PaddingValues(bottom = 88.dp)
                                ) {
                                    items(filtered, key = { it.id }) { market ->
                                        MarketCard(
                                            market = market,
                                            onClick = { onOpenMarket(market) },
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
                                    contentPadding = PaddingValues(bottom = 88.dp)
                                ) {
                                    items(filtered, key = { it.id }) { employee ->
                                        EmployeeCard(
                                            employee = employee,
                                            onClick = { onOpenEmployee(employee) },
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
                val isAdminUser = isSuperAdmin || currentRole.equals("Admin", ignoreCase = true)
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
                            text = if (isAdminUser) "Delete ${req.typeName}?" else "Request Deletion?",
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    },
                    text = {
                        Text(
                            text = if (isAdminUser) {
                                "Are you sure you want to permanently delete \"${req.itemName}\"? This record will be removed immediately from cloud and app."
                            } else {
                                "Are you sure you want to request deletion of \"${req.itemName}\"? This will be sent to Admin for approval."
                            },
                            style = MaterialTheme.typography.bodyMedium,
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
                            Text(
                                text = if (isAdminUser) "Delete" else "Submit Request",
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
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

            if (showRequestsDialog) {
                CustomerRequestsDialog(
                    viewModel = viewModel,
                    onDismiss = { showRequestsDialog = false }
                )
            }

            if (showSupplierRequestsDialog) {
                SupplierRequestsDialog(
                    viewModel = viewModel,
                    onDismiss = { showSupplierRequestsDialog = false },
                    onOpenDirectForm = {
                        showSupplierRequestsDialog = false
                        showSupplierRegistrationDialog = true
                    }
                )
            }

            if (showSupplierRegistrationDialog) {
                SupplierRegistrationDialog(
                    viewModel = viewModel,
                    onDismiss = { showSupplierRegistrationDialog = false }
                )
            }

            inviteShareType?.let { type ->
                WhatsAppInviteDialog(
                    title = if (type == MasterTab.CUSTOMERS) "Invite Customer via WhatsApp" else "Invite Supplier / Mill via WhatsApp",
                    subtitle = if (type == MasterTab.CUSTOMERS) 
                        "Send registration link to new retail buyers with SMS OTP verification"
                    else 
                        "Send registration link to prospective textile mills & fabric suppliers",
                    onSendToPhone = { phone ->
                        if (type == MasterTab.CUSTOMERS) {
                            ShareUtil.shareCustomerRegistrationLink(context, phone, agentLinkId)
                        } else {
                            ShareUtil.shareSupplierRegistrationLink(context, phone)
                        }
                        inviteShareType = null
                    },
                    onSendGeneral = {
                        if (type == MasterTab.CUSTOMERS) {
                            ShareUtil.shareCustomerRegistrationLink(context, null, agentLinkId)
                        } else {
                            ShareUtil.shareSupplierRegistrationLink(context, null)
                        }
                        inviteShareType = null
                    },
                    onDismiss = { inviteShareType = null }
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
                        style = MaterialTheme.typography.titleSmall,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = item.iconBgColor
                    ) {
                        Text(
                            text = "${item.count}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = item.iconTintColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
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
            style = MaterialTheme.typography.titleMedium,
            color = NavyPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Try adjusting your search or add a new record.",
            style = MaterialTheme.typography.bodySmall,
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
                    style = MaterialTheme.typography.bodySmall,
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
                    style = MaterialTheme.typography.titleMedium,
                    color = NavyPrimary
                )
                if (item.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
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
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
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

/**
 * Edit / Delete on a master row. Every button is a 40dp circle inside a 44dp tap area so it can be
 * hit with a thumb, and each one says out loud what it does. "Open" is the row itself, so there is
 * no third arrow button competing for space.
 */
@Composable
fun MasterCardActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    canEdit: Boolean = true,
    canDelete: Boolean = true,
    /** Used in the spoken label, e.g. "Edit Ramesh Textiles". */
    itemLabel: String = ""
) {
    val suffix = if (itemLabel.isBlank()) "" else " $itemLabel"
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (canEdit) {
            IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                Surface(shape = CircleShape, color = NavyPrimary.copy(alpha = 0.08f), modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit$suffix",
                            tint = NavyPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
        if (canDelete) {
            IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                Surface(shape = CircleShape, color = Color(0xFFFEE2E2), modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete$suffix",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
        if (!canEdit && !canDelete) {
            // Read-only row still needs an affordance that it opens
            IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open$suffix",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Master list rows
//
// Every master list (customers, suppliers, staff, products, brands, transporters, markets) uses
// the same row: small avatar, name, one line of detail, then Edit / Delete. Text uses the theme's
// typography so it follows the phone's font size instead of a hard-coded sp value, and the row
// keeps one height whether or not a badge, photo or phone number is present.
// -------------------------------------------------------------

/** Shared 36dp avatar: the photo when there is one, otherwise a tinted icon. */
@Composable
private fun MasterAvatar(
    photoUri: String,
    icon: ImageVector,
    background: Color,
    tint: Color,
    label: String
) {
    Surface(shape = CircleShape, color = background, modifier = Modifier.size(36.dp)) {
        if (photoUri.isNotBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun CustomerCard(
    customer: CustomerEntity,
    pendingCount: Int = 0,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val displayName = customer.firmName.ifBlank { customer.name }
    val marketDisplay = customer.marketArea.ifBlank {
        customer.markets.split(",").firstOrNull()?.trim() ?: customer.city.ifBlank { "" }
    }
    val subtitle = buildString {
        if (customer.firmName.isNotBlank() && customer.name.isNotBlank() && customer.firmName != customer.name) {
            append(customer.name)
        }
        if (marketDisplay.isNotBlank()) {
            if (isNotEmpty()) append(" • ")
            append(marketDisplay)
        } else if (customer.phone.isNotBlank()) {
            if (isNotEmpty()) append(" • ")
            append(customer.phone)
        }
    }.ifBlank { "Customer" }

    ListRow(
        horizontalPadding = 0.dp,
        title = displayName,
        detail = subtitle,
        leading = {
            MasterAvatar(
                photoUri = customer.shopPhotoUri.ifBlank { customer.purchaserPhotoUri },
                icon = Icons.Default.People,
                background = Color(0xFFEFF6FF),
                tint = Color(0xFF2563EB),
                label = displayName
            )
        },
        status = if (pendingCount > 0) {
            { StatusPill("$pendingCount pending", Color(0xFFFFF3CD), Color(0xFF92400E)) }
        } else null,
        trailing = { MasterCardActions(onEdit = onEdit, onDelete = onDelete, onClick = onClick, itemLabel = displayName) },
        onClick = onClick
    )
}

@Composable
fun SupplierCard(
    supplier: SupplierEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val displayName = supplier.firmName.ifBlank { supplier.name }
    val marketDisplay = supplier.marketArea.ifBlank {
        supplier.markets.split(",").firstOrNull()?.trim() ?: supplier.city.ifBlank { "" }
    }
    val subtitle = buildString {
        if (supplier.type.isNotBlank()) {
            append(supplier.type)
        }
        if (supplier.firmName.isNotBlank() && supplier.name.isNotBlank() && supplier.firmName != supplier.name) {
            if (isNotEmpty()) append(" • ")
            append(supplier.name)
        } else if (marketDisplay.isNotBlank()) {
            if (isNotEmpty()) append(" • ")
            append(marketDisplay)
        }
    }.ifBlank { "Supplier / Mill" }

    ListRow(
        horizontalPadding = 0.dp,
        title = displayName,
        detail = subtitle,
        leading = {
            MasterAvatar(
                photoUri = supplier.shopPhotoUri.ifBlank { supplier.visitingCardPhotoUri },
                icon = Icons.Default.Store,
                background = Color(0xFFECFDF5),
                tint = Color(0xFF059669),
                label = displayName
            )
        },
        trailing = { MasterCardActions(onEdit = onEdit, onDelete = onDelete, onClick = onClick, itemLabel = displayName) },
        onClick = onClick
    )
}

@Composable
fun EmployeeCard(
    employee: EmployeeEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isAdmin: Boolean = false
) {
    ListRow(
        horizontalPadding = 0.dp,
        title = employee.name,
        detail = "${employee.role}${if (employee.employeeId.isNotBlank()) " • ${employee.employeeId}" else ""}",
        leading = {
            MasterAvatar(
                photoUri = "",
                icon = Icons.Default.Person,
                background = Color(0xFFF1F5F9),
                tint = NavyPrimary,
                label = employee.name
            )
        },
        trailing = {
            MasterCardActions(
                onEdit = onEdit,
                onDelete = onDelete,
                onClick = onClick,
                canEdit = isAdmin,
                canDelete = isAdmin,
                itemLabel = employee.name
            )
        },
        onClick = onClick
    )
}

@Composable
fun ProductCard(
    product: ProductEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val rateText = if (product.defaultRate > 0) "₹${product.defaultRate.toInt()}" else ""
    val catText = product.category.ifBlank { product.productCode }
    val subtitle = if (rateText.isNotBlank() && catText.isNotBlank()) "$rateText • $catText" else rateText.ifBlank { catText }

    ListRow(
        horizontalPadding = 0.dp,
        title = product.name,
        detail = subtitle,
        leading = {
            MasterAvatar(
                photoUri = "",
                icon = Icons.Default.Inventory,
                background = GoldAccent.copy(alpha = 0.15f),
                tint = NavyPrimary,
                label = product.name
            )
        },
        trailing = { MasterCardActions(onEdit = onEdit, onDelete = onDelete, onClick = onClick, itemLabel = product.name) },
        onClick = onClick
    )
}

@Composable
fun BrandCard(
    brand: BrandEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cat = brand.category.ifBlank { "Garment Brand" }
    val subtitle = if (brand.manufacturerName.isNotBlank()) "$cat • ${brand.manufacturerName}" else cat

    ListRow(
        horizontalPadding = 0.dp,
        title = brand.brandName,
        detail = subtitle,
        leading = {
            MasterAvatar(
                photoUri = brand.logoPhotoUri,
                icon = Icons.Default.Sell,
                background = Color(0xFFFFFBEB),
                tint = Color(0xFFD97706),
                label = brand.brandName
            )
        },
        trailing = { MasterCardActions(onEdit = onEdit, onDelete = onDelete, onClick = onClick, itemLabel = brand.brandName) },
        onClick = onClick
    )
}

@Composable
fun TransporterCard(
    transporter: TransporterEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cityText = transporter.city.ifBlank { "Transport Desk" }
    val subtitle = if (transporter.phone1.isNotBlank()) "$cityText • ${transporter.phone1}" else cityText

    ListRow(
        horizontalPadding = 0.dp,
        title = transporter.transporterName,
        detail = subtitle,
        leading = {
            MasterAvatar(
                photoUri = "",
                icon = Icons.Default.LocalShipping,
                background = Color(0xFFECFEFF),
                tint = Color(0xFF0891B2),
                label = transporter.transporterName
            )
        },
        trailing = { MasterCardActions(onEdit = onEdit, onDelete = onDelete, onClick = onClick, itemLabel = transporter.transporterName) },
        onClick = onClick
    )
}

@Composable
fun MarketCard(
    market: MarketEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeText = market.marketType.ifBlank { "Textile Market" }
    val subtitle = if (market.city.isNotBlank()) "$typeText • ${market.city}" else typeText

    ListRow(
        horizontalPadding = 0.dp,
        title = market.marketName,
        detail = subtitle,
        leading = {
            MasterAvatar(
                photoUri = "",
                icon = Icons.Default.LocationCity,
                background = Color(0xFFFAF5FF),
                tint = Color(0xFF9333EA),
                label = market.marketName
            )
        },
        trailing = { MasterCardActions(onEdit = onEdit, onDelete = onDelete, onClick = onClick, itemLabel = market.marketName) },
        onClick = onClick
    )
}
