package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.ExpressiveSearchBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.dialogs.AddEditSupplierDialog
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainScreenTab {
    SUPPLIERS,
    TRANSACTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: HimatViewModel,
    onNavigate: (AppScreen) -> Unit,
    onOpenNewVisit: () -> Unit
) {
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(MainScreenTab.SUPPLIERS) }
    var selectedSupplierFilterForTransactions by remember { mutableStateOf<String?>(null) }

    // Supplier Dialogs
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<SupplierEntity?>(null) }
    var supplierToDelete by remember { mutableStateOf<SupplierEntity?>(null) }

    // Transaction Dialogs
    var transactionToUpdateStatus by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }

    // Searches & Filters
    var supplierSearchQuery by remember { mutableStateOf("") }
    var supplierTypeFilter by remember { mutableStateOf("All") }

    var transactionSearchQuery by remember { mutableStateOf("") }
    var transactionStatusFilter by remember { mutableStateOf("All") }

    // Key Stats Calculations
    val pendingCount = transactions.count { it.deliveryStatus.equals("Pending", ignoreCase = true) }
    val packedCount = transactions.count { it.deliveryStatus.equals("Packed", ignoreCase = true) }
    val dispatchedCount = transactions.count { it.deliveryStatus.equals("Dispatched", ignoreCase = true) }
    val deliveredCount = transactions.count { it.deliveryStatus.equals("Delivered", ignoreCase = true) }
    val totalVolumePcs = transactions.sumOf { it.pieces }
    val totalVolumeCases = transactions.sumOf { it.caseCount }

    val manufacturerCount = suppliers.count { it.type.equals("Manufacturer", ignoreCase = true) }
    val wholesalerCount = suppliers.count { it.type.equals("Wholesaler", ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // Expressive Material 3 Header Banner with wallpaper dynamic matching
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Himat Textile Agency",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "MARKET OPERATIONS",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Suppliers & Transaction Hub",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectedTab == MainScreenTab.SUPPLIERS) {
                            Button(
                                onClick = {
                                    supplierToEdit = null
                                    showAddSupplierDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Supplier", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { showAddTransactionDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Order", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High-level KPI Mini Grid with Material 3 Expressive Tonal Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MainMetricCard(
                        title = "Suppliers",
                        value = "${suppliers.size}",
                        subtitle = "$manufacturerCount Mfr • $wholesalerCount Whl",
                        icon = Icons.Default.Storefront,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = MainScreenTab.SUPPLIERS }
                    )
                    MainMetricCard(
                        title = "Transactions",
                        value = "${transactions.size}",
                        subtitle = "$pendingCount Pending • $dispatchedCount Transit",
                        icon = Icons.Default.ReceiptLong,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedSupplierFilterForTransactions = null
                            selectedTab = MainScreenTab.TRANSACTIONS
                        }
                    )
                    MainMetricCard(
                        title = "Volume",
                        value = String.format("%,d", totalVolumePcs),
                        subtitle = "$totalVolumeCases Cases Packed",
                        icon = Icons.Default.Inventory,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.REPORTS) }
                    )
                }
            }
        }

        // Material 3 Primary Tab Row
        TabRow(
            selectedTabIndex = if (selectedTab == MainScreenTab.SUPPLIERS) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                val index = if (selectedTab == MainScreenTab.SUPPLIERS) 0 else 1
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == MainScreenTab.SUPPLIERS,
                onClick = { selectedTab = MainScreenTab.SUPPLIERS },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedTab == MainScreenTab.SUPPLIERS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Suppliers (${suppliers.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == MainScreenTab.SUPPLIERS) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == MainScreenTab.SUPPLIERS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == MainScreenTab.TRANSACTIONS,
                onClick = { selectedTab = MainScreenTab.TRANSACTIONS },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedTab == MainScreenTab.TRANSACTIONS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Order Tracker (${transactions.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == MainScreenTab.TRANSACTIONS) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == MainScreenTab.TRANSACTIONS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                MainScreenTab.SUPPLIERS -> {
                    SuppliersListView(
                        suppliers = suppliers,
                        transactions = transactions,
                        searchQuery = supplierSearchQuery,
                        onSearchChange = { supplierSearchQuery = it },
                        typeFilter = supplierTypeFilter,
                        onTypeFilterChange = { supplierTypeFilter = it },
                        onEditSupplier = {
                            supplierToEdit = it
                            showAddSupplierDialog = true
                        },
                        onDeleteSupplier = { supplierToDelete = it },
                        onTrackSupplierTransactions = { supplier ->
                            viewModel.openSupplierDetail(supplier)
                        },
                        onOpenSupplierDetail = { supplier ->
                            viewModel.openSupplierDetail(supplier)
                        },
                        onAddSupplierClick = {
                            supplierToEdit = null
                            showAddSupplierDialog = true
                        }
                    )
                }

                MainScreenTab.TRANSACTIONS -> {
                    TransactionStatusTrackerView(
                        transactions = transactions,
                        searchQuery = transactionSearchQuery,
                        onSearchChange = { transactionSearchQuery = it },
                        statusFilter = transactionStatusFilter,
                        onStatusFilterChange = { transactionStatusFilter = it },
                        selectedSupplierFilter = selectedSupplierFilterForTransactions,
                        onClearSupplierFilter = { selectedSupplierFilterForTransactions = null },
                        onQuickAdvanceStatus = { txn ->
                            val nextStatus = when (txn.deliveryStatus.lowercase()) {
                                "pending" -> "Packed"
                                "packed" -> "Dispatched"
                                "dispatched" -> "Delivered"
                                else -> "Delivered"
                            }
                            viewModel.updateTransactionDeliveryStatus(
                                transactionId = txn.id,
                                status = nextStatus,
                                transporter = if (txn.transporter.isNotBlank()) txn.transporter else "Standard Logistics"
                            )
                        },
                        onOpenStatusDialog = { transactionToUpdateStatus = it },
                        onTogglePayment = { txn ->
                            val newPaymentStatus = if (txn.paymentStatus == "Paid") "Pending" else "Paid"
                            viewModel.updateTransactionPaymentStatus(txn.id, newPaymentStatus)
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Supplier Dialog
    if (showAddSupplierDialog) {
        AddEditSupplierDialog(
            supplier = supplierToEdit,
            onDismiss = { showAddSupplierDialog = false },
            onSave = {
                viewModel.saveSupplier(it)
                showAddSupplierDialog = false
            }
        )
    }

    // Delete Supplier Confirmation Dialog
    supplierToDelete?.let { supplier ->
        Dialog(onDismissRequest = { supplierToDelete = null }) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Delete Supplier",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to remove '${supplier.name}' from your suppliers directory?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { supplierToDelete = null },
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteSupplier(supplier)
                                supplierToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Transaction Status & Transporter Edit Dialog
    transactionToUpdateStatus?.let { txn ->
        UpdateTransactionStatusDialog(
            transaction = txn,
            onDismiss = { transactionToUpdateStatus = null },
            onSave = { newStatus, newTransporter, newPaymentStatus ->
                viewModel.updateTransactionDeliveryStatus(txn.id, newStatus, newTransporter)
                viewModel.updateTransactionPaymentStatus(txn.id, newPaymentStatus)
                transactionToUpdateStatus = null
            }
        )
    }

    // Quick Add Transaction Dialog
    if (showAddTransactionDialog) {
        QuickAddTransactionDialog(
            suppliers = suppliers,
            customers = customers,
            onDismiss = { showAddTransactionDialog = false },
            onSave = { newTxn ->
                viewModel.saveTransaction(newTxn)
                showAddTransactionDialog = false
            }
        )
    }
}

@Composable
fun MainMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersListView(
    suppliers: List<SupplierEntity>,
    transactions: List<TransactionEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    typeFilter: String,
    onTypeFilterChange: (String) -> Unit,
    onEditSupplier: (SupplierEntity) -> Unit,
    onDeleteSupplier: (SupplierEntity) -> Unit,
    onTrackSupplierTransactions: (SupplierEntity) -> Unit,
    onOpenSupplierDetail: (SupplierEntity) -> Unit = onTrackSupplierTransactions,
    onAddSupplierClick: () -> Unit
) {
    val context = LocalContext.current

    val filteredSuppliers = suppliers.filter { s ->
        val matchesSearch = s.name.contains(searchQuery, ignoreCase = true) ||
                s.brand.contains(searchQuery, ignoreCase = true) ||
                s.marketArea.contains(searchQuery, ignoreCase = true) ||
                s.contactPerson.contains(searchQuery, ignoreCase = true) ||
                s.gstin.contains(searchQuery, ignoreCase = true) ||
                s.phone.contains(searchQuery, ignoreCase = true) ||
                s.email.contains(searchQuery, ignoreCase = true) ||
                s.city.contains(searchQuery, ignoreCase = true) ||
                s.categories.contains(searchQuery, ignoreCase = true)

        val matchesType = when (typeFilter) {
            "Manufacturer" -> s.type.equals("Manufacturer", ignoreCase = true)
            "Wholesaler" -> s.type.equals("Wholesaler", ignoreCase = true)
            else -> true
        }
        matchesSearch && matchesType
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Material 3 Expressive Search and Filter Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                ExpressiveSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchChange,
                    placeholderText = "Search supplier, market, phone, GSTIN..."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Type filter chips with ergonomic touch targets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Manufacturer", "Wholesaler").forEach { type ->
                        val isSelected = typeFilter == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTypeFilterChange(type) },
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                            shape = CircleShape,
                            label = {
                                Text(
                                    when (type) {
                                        "Manufacturer" -> "Manufacturers"
                                        "Wholesaler" -> "Wholesalers"
                                        else -> "All (${suppliers.size})"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Supplier Count Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing ${filteredSuppliers.size} of ${suppliers.size} suppliers",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap 'Track' to see orders",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (filteredSuppliers.isEmpty()) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No suppliers matched your search",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try searching with a different keyword or add a new supplier.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddSupplierClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("+ Add New Supplier", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredSuppliers, key = { it.id }) { supplier ->
                val supplierTxns = transactions.filter { it.supplierId == supplier.id || it.supplierName.equals(supplier.name, ignoreCase = true) }
                val supplierPieces = supplierTxns.sumOf { it.pieces }
                val pendingOrders = supplierTxns.count { it.deliveryStatus != "Delivered" }

                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSupplierDetail(supplier) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Title & Type Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = supplier.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (supplier.brand.isNotBlank()) {
                                        Text(
                                            text = "Brand: ${supplier.brand}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            SupplierTypeBadge(type = supplier.type)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Location & Contact info
                        if (supplier.marketArea.isNotBlank() || supplier.address.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (supplier.marketArea.isNotBlank()) supplier.marketArea else supplier.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (supplier.contactPerson.isNotBlank() || supplier.phone.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${supplier.contactPerson}${if (supplier.phone.isNotBlank()) " • ${supplier.phone}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (supplier.gstin.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "GST: ${supplier.gstin}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        if (supplier.email.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = supplier.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (supplier.categories.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                supplier.categoryList.forEach { cat ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Transaction status summary & Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${supplierTxns.size} Orders (${String.format("%,d", supplierPieces)} pcs)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (pendingOrders > 0) "$pendingOrders ongoing / transit" else "All fulfilled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (pendingOrders > 0) MaterialTheme.colorScheme.secondary else Color(0xFF059669),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Touch targets >= 44dp for accessibility and high comfort
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (supplier.phone.isNotBlank()) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier.phone}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Call,
                                            contentDescription = "Call",
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                FilledTonalIconButton(
                                    onClick = { onEditSupplier(supplier) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = { onDeleteSupplier(supplier) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Button(
                                    onClick = { onTrackSupplierTransactions(supplier) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                                ) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Track (${supplierTxns.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionStatusTrackerView(
    transactions: List<TransactionEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    statusFilter: String,
    onStatusFilterChange: (String) -> Unit,
    selectedSupplierFilter: String?,
    onClearSupplierFilter: () -> Unit,
    onQuickAdvanceStatus: (TransactionEntity) -> Unit,
    onOpenStatusDialog: (TransactionEntity) -> Unit,
    onTogglePayment: (TransactionEntity) -> Unit
) {
    val filteredTransactions = transactions.filter { txn ->
        val matchesSupplier = selectedSupplierFilter == null ||
                txn.supplierName.equals(selectedSupplierFilter, ignoreCase = true)

        val matchesStatus = when (statusFilter) {
            "Pending" -> txn.deliveryStatus.equals("Pending", ignoreCase = true)
            "Packed" -> txn.deliveryStatus.equals("Packed", ignoreCase = true)
            "Dispatched" -> txn.deliveryStatus.equals("Dispatched", ignoreCase = true)
            "Delivered" -> txn.deliveryStatus.equals("Delivered", ignoreCase = true)
            else -> true
        }

        val matchesSearch = txn.orderNo.contains(searchQuery, ignoreCase = true) ||
                txn.transactionNumber.contains(searchQuery, ignoreCase = true) ||
                txn.supplierName.contains(searchQuery, ignoreCase = true) ||
                txn.customerName.contains(searchQuery, ignoreCase = true) ||
                txn.itemCode.contains(searchQuery, ignoreCase = true) ||
                txn.transporter.contains(searchQuery, ignoreCase = true)

        matchesSupplier && matchesStatus && matchesSearch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Supplier Filter Banner (if filtered)
        if (selectedSupplierFilter != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Filtered by: $selectedSupplierFilter",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        TextButton(
                            onClick = onClearSupplierFilter,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                        ) {
                            Text("Clear Filter ✕", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Material 3 Expressive Search & Filter Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                ExpressiveSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchChange,
                    placeholderText = "Search Order #, Item, Supplier, Customer..."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Status Filter Chips with horizontal scroll and standard M3 heights
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statuses = listOf("All", "Pending", "Packed", "Dispatched", "Delivered")
                    statuses.forEach { st ->
                        val isSelected = statusFilter == st
                        val count = when (st) {
                            "Pending" -> transactions.count { it.deliveryStatus.equals("Pending", ignoreCase = true) }
                            "Packed" -> transactions.count { it.deliveryStatus.equals("Packed", ignoreCase = true) }
                            "Dispatched" -> transactions.count { it.deliveryStatus.equals("Dispatched", ignoreCase = true) }
                            "Delivered" -> transactions.count { it.deliveryStatus.equals("Delivered", ignoreCase = true) }
                            else -> transactions.size
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { onStatusFilterChange(st) },
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                            shape = CircleShape,
                            label = {
                                Text(
                                    text = "$st ($count)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Transactions List
        if (filteredTransactions.isEmpty()) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing search filters or changing the status filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { txn ->
                TransactionCardItem(
                    transaction = txn,
                    onQuickAdvance = { onQuickAdvanceStatus(txn) },
                    onEditStatus = { onOpenStatusDialog(txn) },
                    onTogglePayment = { onTogglePayment(txn) }
                )
            }
        }
    }
}

@Composable
fun TransactionCardItem(
    transaction: TransactionEntity,
    onQuickAdvance: () -> Unit,
    onEditStatus: () -> Unit,
    onTogglePayment: () -> Unit
) {
    val status = transaction.deliveryStatus

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Order #, Date, Status Badge & Payment Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = transaction.orderNo.ifBlank { transaction.transactionNumber },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = transaction.transactionDate.ifBlank { "Recent" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Payment Status Badge
                    Surface(
                        color = if (transaction.paymentStatus == "Paid") Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                        shape = CircleShape,
                        modifier = Modifier.clickable { onTogglePayment() }
                    ) {
                        Text(
                            text = if (transaction.paymentStatus == "Paid") "Paid" else "Pending",
                            color = if (transaction.paymentStatus == "Paid") Color(0xFF166534) else Color(0xFF92400E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    StatusBadge(status = status)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Parties: Supplier & Customer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Supplier", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = transaction.supplierName,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Customer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = transaction.customerName.ifBlank { "Retail Customer" },
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Item Details & Breakdown
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = transaction.itemCode,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${transaction.pieces} Pcs @ ₹${transaction.rate.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${transaction.caseCount} Cases (${transaction.caseCount * transaction.caseSize} pcs)" +
                                    if (transaction.loosePieces > 0) " + ${transaction.loosePieces} Loose pcs" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val amountToDisplay = if (transaction.grandTotalWithGst > 0.0) transaction.grandTotalWithGst else transaction.totalAmount
                        Text(
                            text = "₹${String.format("%,.0f", amountToDisplay)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "incl. ${transaction.gstRate}% GST",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Material 3 Interactive 4-Step Stepper Progress
            TransactionStepProgress(currentStatus = status)

            Spacer(modifier = Modifier.height(10.dp))

            // Transporter info
            if (transaction.transporter.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Transporter: ${transaction.transporter}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Standardized Action Buttons with ergonomic touch heights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEditStatus,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 42.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Edit Status / LR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }

                // 1-Tap Advance Status Button
                when (status.lowercase()) {
                    "pending" -> {
                        Button(
                            onClick = onQuickAdvance,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 42.dp)
                        ) {
                            Text("Mark Packed", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    "packed" -> {
                        Button(
                            onClick = onQuickAdvance,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 42.dp)
                        ) {
                            Text("Mark Dispatched", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    "dispatched" -> {
                        Button(
                            onClick = onQuickAdvance,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 42.dp)
                        ) {
                            Text("Mark Delivered", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    else -> {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Delivered & Verified ✓",
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionStepProgress(currentStatus: String) {
    val steps = listOf("Pending", "Packed", "Dispatched", "Delivered")
    val currentIndex = when (currentStatus.lowercase()) {
        "packed" -> 1
        "dispatched" -> 2
        "delivered" -> 3
        else -> 0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, stepName ->
            val isCompleted = index < currentIndex
            val isCurrent = index == currentIndex

            val stepColor = when {
                isCompleted -> Color(0xFF059669)
                isCurrent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted || isCurrent) stepColor else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = if (isCompleted || isCurrent) stepColor else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    } else {
                        Text(
                            text = "${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stepName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) stepColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Connecting line between steps
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .height(2.dp)
                        .background(if (index < currentIndex) Color(0xFF059669) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateTransactionStatusDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onSave: (newStatus: String, newTransporter: String, paymentStatus: String) -> Unit
) {
    var status by remember { mutableStateOf(transaction.deliveryStatus) }
    var transporter by remember { mutableStateOf(transaction.transporter) }
    var paymentStatus by remember { mutableStateOf(transaction.paymentStatus) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Update Transaction Status",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${transaction.orderNo.ifBlank { transaction.transactionNumber }} • ${transaction.supplierName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Delivery Status Dropdown with standard height
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Delivery Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        listOf("Pending", "Packed", "Dispatched", "Delivered").forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    status = st
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = transporter,
                    onValueChange = { transporter = it },
                    label = { Text("Transporter / LR No") },
                    placeholder = { Text("e.g. VRL Logistics, LR #4920") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Status selector
                Text("Payment Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Pending", "Paid").forEach { pStatus ->
                        val isSel = paymentStatus == pStatus
                        FilterChip(
                            selected = isSel,
                            onClick = { paymentStatus = pStatus },
                            shape = CircleShape,
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                            label = { Text(pStatus, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (pStatus == "Paid") Color(0xFFDCFCE7) else MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = if (pStatus == "Paid") Color(0xFF15803D) else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(status, transporter, paymentStatus)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Save Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTransactionDialog(
    suppliers: List<SupplierEntity>,
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    var orderNo by remember { mutableStateOf("HT-${(2600..2999).random()}") }
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var itemCode by remember { mutableStateOf("") }
    var piecesText by remember { mutableStateOf("50") }
    var rateText by remember { mutableStateOf("450") }
    var caseSizeText by remember { mutableStateOf("24") }
    var transporter by remember { mutableStateOf("") }
    var supplierExpanded by remember { mutableStateOf(false) }
    var customerExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "New Transaction / Order",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Supplier Dropdown
                ExposedDropdownMenuBox(
                    expanded = supplierExpanded,
                    onExpandedChange = { supplierExpanded = !supplierExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.name ?: "Select Supplier",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supplier *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = supplierExpanded,
                        onDismissRequest = { supplierExpanded = false }
                    ) {
                        suppliers.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.name} (${s.type})") },
                                onClick = {
                                    selectedSupplier = s
                                    caseSizeText = s.defaultCaseSize.toString()
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Customer Dropdown
                ExposedDropdownMenuBox(
                    expanded = customerExpanded,
                    onExpandedChange = { customerExpanded = !customerExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "Select Customer",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Customer *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = customerExpanded,
                        onDismissRequest = { customerExpanded = false }
                    ) {
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name} (${c.city})") },
                                onClick = {
                                    selectedCustomer = c
                                    customerExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = itemCode,
                    onValueChange = { itemCode = it.uppercase() },
                    label = { Text("Item Code *") },
                    placeholder = { Text("e.g. DENIM-701, KURTI-102") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = piecesText,
                        onValueChange = { piecesText = it },
                        label = { Text("Pieces *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 54.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Rate (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 54.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = caseSizeText,
                        onValueChange = { caseSizeText = it },
                        label = { Text("Case Size") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 54.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = transporter,
                        onValueChange = { transporter = it },
                        label = { Text("Transporter") },
                        placeholder = { Text("e.g. VRL") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 54.dp),
                        singleLine = true
                    )
                }

                if (errorMsg.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val sup = selectedSupplier
                            val cust = selectedCustomer
                            if (sup == null || cust == null || itemCode.isBlank()) {
                                errorMsg = "Please fill in all required fields."
                                return@Button
                            }
                            val pcs = piecesText.toIntOrNull() ?: 0
                            val rate = rateText.toDoubleOrNull() ?: 0.0
                            val caseSize = caseSizeText.toIntOrNull() ?: 24
                            if (pcs <= 0 || rate <= 0) {
                                errorMsg = "Pieces and rate must be greater than 0."
                                return@Button
                            }

                            val total = pcs * rate
                            val gst = (total * 5.0) / 100.0
                            val grandTotal = total + gst
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                            val newTxn = TransactionEntity(
                                transactionNumber = "TXN-${orderNo.replace("HT-", "")}",
                                orderNo = orderNo,
                                visitId = 0,
                                customerId = cust.id,
                                customerName = cust.name,
                                supplierId = sup.id,
                                supplierName = sup.name,
                                itemCode = itemCode.trim(),
                                pieces = pcs,
                                rate = rate,
                                totalAmount = total,
                                gstRate = 5.0,
                                gstAmount = gst,
                                grandTotalWithGst = grandTotal,
                                caseSize = caseSize,
                                caseCount = pcs / caseSize,
                                loosePieces = pcs % caseSize,
                                deliveryStatus = "Pending",
                                transporter = transporter.trim(),
                                transactionDate = today
                            )
                            onSave(newTxn)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Save Transaction", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
