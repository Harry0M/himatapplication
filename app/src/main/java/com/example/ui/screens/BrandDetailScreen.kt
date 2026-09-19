package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.dialogs.FullScreenImageViewerDialog
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandDetailScreen(
    viewModel: HimatViewModel,
    brand: BrandEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onOpenProduct: (ProductEntity) -> Unit = { viewModel.openProductDetail(it) },
    onOpenOrder: (PurchaseEntryEntity) -> Unit = { viewModel.openOrderDetail(it, returnScreen = AppScreen.BRAND_DETAIL) },
    onOpenSupplier: (SupplierEntity) -> Unit = { viewModel.openSupplierDetail(it) }
) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allSuppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()

    val visitMap = remember(allVisits) { allVisits.associateBy { it.id } }
    var showLogoViewer by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("PRODUCTS") } // "PRODUCTS", "ORDERS", "MANUFACTURER"

    // Linked Manufacturer / Supplier Mill
    val linkedSupplier = remember(allSuppliers, brand.manufacturerId, brand.manufacturerName) {
        allSuppliers.find { s ->
            (brand.manufacturerId != null && brand.manufacturerId != 0L && s.id == brand.manufacturerId) ||
            (brand.manufacturerName.isNotBlank() && (s.firmName.equals(brand.manufacturerName, ignoreCase = true) || s.name.equals(brand.manufacturerName, ignoreCase = true)))
        }
    }

    val brandProducts = remember(allProducts, brand.id, brand.brandName, brand.manufacturerId) {
        allProducts.filter { product ->
            (brand.manufacturerId != null && brand.manufacturerId != 0L && product.supplierId == brand.manufacturerId) ||
            product.name.contains(brand.brandName, ignoreCase = true) ||
            product.productCode.contains(brand.brandName, ignoreCase = true) ||
            product.description.contains(brand.brandName, ignoreCase = true)
        }.sortedBy { it.name }
    }

    val brandOrders = remember(allEntries, brandProducts, brand.brandName, brand.manufacturerId) {
        val productCodes = brandProducts.map { it.productCode.lowercase() }.toSet()
        allEntries.filter { entry ->
            productCodes.contains(entry.itemCode.lowercase()) ||
            entry.itemCode.contains(brand.brandName, ignoreCase = true) ||
            (brand.manufacturerId != null && brand.manufacturerId != 0L && entry.supplierId == brand.manufacturerId)
        }.sortedByDescending { it.id }
    }

    val totalPiecesVolume = brandOrders.sumOf { it.pieces }

    // Filtered by Search Query
    val filteredProducts = remember(brandProducts, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) brandProducts
        else brandProducts.filter {
            it.name.contains(q, ignoreCase = true) ||
            it.productCode.contains(q, ignoreCase = true) ||
            it.category.contains(q, ignoreCase = true) ||
            it.description.contains(q, ignoreCase = true)
        }
    }

    val filteredOrders = remember(brandOrders, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) brandOrders
        else brandOrders.filter {
            it.itemCode.contains(q, ignoreCase = true) ||
            it.orderNo.contains(q, ignoreCase = true) ||
            it.supplierName.contains(q, ignoreCase = true) ||
            it.deliveryStatus.contains(q, ignoreCase = true)
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
                            text = brand.brandName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Brand Master • ${brand.category.ifBlank { "Garments" }}",
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
                        Icon(Icons.Default.Edit, contentDescription = "Edit Brand", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF6F8FB))
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
                    // Cardless Hero Profile Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(2.dp, Color(0xFFFDE68A)),
                            modifier = Modifier
                                .size(60.dp)
                                .then(
                                    if (brand.logoPhotoUri.isNotBlank()) Modifier.clickable { showLogoViewer = true }
                                    else Modifier
                                )
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
                                        imageVector = Icons.Default.Sell,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        if (brand.logoPhotoUri.isNotBlank()) {
                            Text(
                                text = "Tap logo for full view",
                                fontSize = 10.sp,
                                color = Color(0xFFD97706),
                                modifier = Modifier.padding(top = 2.dp).clickable { showLogoViewer = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = brand.brandName,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val mfgSubtitle = if (brand.manufacturerName.isNotBlank()) "Mill: ${brand.manufacturerName}" else "Direct / Independent Brand"
                        Text(
                            text = mfgSubtitle,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (brand.category.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, Color(0xFFFDE68A))
                                ) {
                                    Text(
                                        text = brand.category,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Surface(
                                color = if (brand.isActive) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, if (brand.isActive) Color(0xFFA7F3D0) else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = if (brand.isActive) "Active" else "Inactive",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (brand.isActive) Color(0xFF047857) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Action Pills
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onEdit() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                                    Text("Edit Brand", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E))
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val text = "Brand: ${brand.brandName}\nManufacturer: ${brand.manufacturerName}\nCategory: ${brand.category}\nActive Products: ${brandProducts.size}"
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Brand Specs", text))
                                        Toast.makeText(context, "Brand details copied", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(12.dp))
                                    Text("Copy Specs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                }
                            }
                        }
                    }

                    // Cardless Stats Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${brandProducts.size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text("Products", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        }

                        Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${brandOrders.size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text("Orders", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        }

                        Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%,d", totalPiecesVolume),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                            Text("Pieces Sold", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }

            // 2. Pinned Search Bar (Sticky right below TopAppBar when collapsed)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    placeholder = {
                        Text(
                            if (selectedTab == "PRODUCTS") "Search products by name or code..."
                            else if (selectedTab == "ORDERS") "Search orders by item or customer..."
                            else "Search details...",
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
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }

            // 3. Content below Search Bar (LazyColumn)
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Tab Selector Pills (Horizontally Scrollable)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isProd = selectedTab == "PRODUCTS"
                        Surface(
                            shape = CircleShape,
                            color = if (isProd) Color(0xFFD97706) else Color.White,
                            border = BorderStroke(1.dp, if (isProd) Color(0xFFD97706) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedTab = "PRODUCTS" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = if (isProd) Color.White else Color(0xFFD97706),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Products (${filteredProducts.size})",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isProd) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isProd) Color.White else Color(0xFF334155)
                                )
                            }
                        }

                        val isOrder = selectedTab == "ORDERS"
                        Surface(
                            shape = CircleShape,
                            color = if (isOrder) Color(0xFF2563EB) else Color.White,
                            border = BorderStroke(1.dp, if (isOrder) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedTab = "ORDERS" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = if (isOrder) Color.White else Color(0xFF2563EB),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Orders (${filteredOrders.size})",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isOrder) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isOrder) Color.White else Color(0xFF334155)
                                )
                            }
                        }

                        val isMfg = selectedTab == "MANUFACTURER"
                        Surface(
                            shape = CircleShape,
                            color = if (isMfg) Color(0xFF475569) else Color.White,
                            border = BorderStroke(1.dp, if (isMfg) Color(0xFF475569) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedTab = "MANUFACTURER" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.Business,
                                    contentDescription = null,
                                    tint = if (isMfg) Color.White else Color(0xFF475569),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Linked Mill",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isMfg) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isMfg) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                }
            }

            // TAB 1: PRODUCTS
            if (selectedTab == "PRODUCTS") {
                if (filteredProducts.isEmpty()) {
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
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (searchQuery.isNotBlank()) "No products match your search"
                                    else "No catalog products linked to this brand yet",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    items(filteredProducts, key = { it.id }) { prod ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenProduct(prod) }
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
                                        color = Color(0xFFEEF2FF),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Inventory, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Text("Code: ${prod.productCode} • ${prod.category}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("₹${prod.defaultRate.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF059669))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: ORDERS
            if (selectedTab == "ORDERS") {
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
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (searchQuery.isNotBlank()) "No orders match your search"
                                    else "No orders generated for this brand yet",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    items(filteredOrders, key = { it.id }) { entry ->
                        val visit = visitMap[entry.visitId]
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
                                        Text(entry.orderNo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2563EB))
                                        DeliveryStatusBadge(status = entry.deliveryStatus)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Item: ${entry.itemCode} • Buyer: ${visit?.customerName ?: "Direct"}", fontSize = 11.5.sp, color = Color(0xFF475569))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${entry.pieces} pcs", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Text(PdfGenerator.formatInr(entry.grandTotalWithGst), fontSize = 11.sp, color = Color(0xFF059669))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // TAB 3: LINKED MANUFACTURER MILL
            if (selectedTab == "MANUFACTURER") {
                item {
                    if (linkedSupplier != null) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenSupplier(linkedSupplier) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFFFFBEB),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Column {
                                            Text(linkedSupplier.firmName.ifBlank { linkedSupplier.name }, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                            Text(linkedSupplier.type.ifBlank { "Textile Manufacturer" }, fontSize = 11.5.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open Supplier", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)

                                if (linkedSupplier.contactPerson.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Contact Person:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(linkedSupplier.contactPerson, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                    }
                                }

                                if (linkedSupplier.phone.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Primary Phone:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(linkedSupplier.phone, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
                                    }
                                }

                                if (linkedSupplier.city.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("City Hub:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(linkedSupplier.city, fontSize = 12.sp, color = Color(0xFF0F172A))
                                    }
                                }

                                if (linkedSupplier.address.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Mill Address:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(linkedSupplier.address, fontSize = 12.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                if (linkedSupplier.gstin.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("GSTIN:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(linkedSupplier.gstin, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    if (brand.manufacturerName.isNotBlank()) "Manufacturer \"${brand.manufacturerName}\" is not registered in Suppliers Master"
                                    else "Independent Label: No linked manufacturer mill specified",
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    if (showLogoViewer && brand.logoPhotoUri.isNotBlank()) {
        FullScreenImageViewerDialog(
            imageUrl = brand.logoPhotoUri,
            title = "${brand.brandName} Logo",
            onDismiss = { showLogoViewer = false }
        )
    }
}
