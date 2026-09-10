package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SupplierEntity
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.dialogs.AddEditCustomerDialog
import com.example.ui.dialogs.AddEditEmployeeDialog
import com.example.ui.dialogs.AddEditProductDialog
import com.example.ui.dialogs.AddEditSupplierDialog
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel

enum class MasterTab {
    CUSTOMERS,
    SUPPLIERS,
    PRODUCTS,
    EMPLOYEES
}

@Composable
fun MastersScreen(
    viewModel: HimatViewModel,
    initialTab: MasterTab = MasterTab.CUSTOMERS
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var supplierTypeFilter by remember { mutableStateOf("All") } // "All", "Manufacturer", "Wholesaler"

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }

    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<SupplierEntity?>(null) }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }

    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<EmployeeEntity?>(null) }

    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        MasterTab.CUSTOMERS -> {
                            customerToEdit = null
                            showAddCustomerDialog = true
                        }
                        MasterTab.SUPPLIERS -> {
                            supplierToEdit = null
                            showAddSupplierDialog = true
                        }
                        MasterTab.PRODUCTS -> {
                            productToEdit = null
                            showAddProductDialog = true
                        }
                        MasterTab.EMPLOYEES -> {
                            employeeToEdit = null
                            showAddEmployeeDialog = true
                        }
                    }
                },
                containerColor = NavyPrimary,
                contentColor = GoldAccent
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = NavyPrimary,
                contentColor = GoldAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = GoldAccent,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == MasterTab.CUSTOMERS,
                    onClick = { selectedTab = MasterTab.CUSTOMERS },
                    text = { Text("Customers (${customers.size})", fontWeight = FontWeight.Bold, color = if (selectedTab == MasterTab.CUSTOMERS) GoldAccent else Color.White) },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selectedTab == MasterTab.CUSTOMERS) GoldAccent else Color.White) }
                )
                Tab(
                    selected = selectedTab == MasterTab.SUPPLIERS,
                    onClick = { selectedTab = MasterTab.SUPPLIERS },
                    text = { Text("Suppliers (${suppliers.size})", fontWeight = FontWeight.Bold, color = if (selectedTab == MasterTab.SUPPLIERS) GoldAccent else Color.White) },
                    icon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selectedTab == MasterTab.SUPPLIERS) GoldAccent else Color.White) }
                )
                Tab(
                    selected = selectedTab == MasterTab.PRODUCTS,
                    onClick = { selectedTab = MasterTab.PRODUCTS },
                    text = { Text("Products (${products.size})", fontWeight = FontWeight.Bold, color = if (selectedTab == MasterTab.PRODUCTS) GoldAccent else Color.White) },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selectedTab == MasterTab.PRODUCTS) GoldAccent else Color.White) }
                )
                Tab(
                    selected = selectedTab == MasterTab.EMPLOYEES,
                    onClick = { selectedTab = MasterTab.EMPLOYEES },
                    text = { Text("Salesmen (${employees.size})", fontWeight = FontWeight.Bold, color = if (selectedTab == MasterTab.EMPLOYEES) GoldAccent else Color.White) },
                    icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selectedTab == MasterTab.EMPLOYEES) GoldAccent else Color.White) }
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search ${selectedTab.name.lowercase().replaceFirstChar { it.uppercase() }}") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

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
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
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

                when (selectedTab) {
                    MasterTab.CUSTOMERS -> {
                        val filtered = customers.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.city.contains(searchQuery, ignoreCase = true) ||
                                    it.phone.contains(searchQuery, ignoreCase = true)
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered) { customer ->
                                CustomerCard(
                                    customer = customer,
                                    onClick = { viewModel.openCustomerDetail(customer) },
                                    onEdit = {
                                        customerToEdit = customer
                                        showAddCustomerDialog = true
                                    },
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
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered) { supplier ->
                                SupplierCard(
                                    supplier = supplier,
                                    onClick = { viewModel.openSupplierDetail(supplier) },
                                    onEdit = {
                                        supplierToEdit = supplier
                                        showAddSupplierDialog = true
                                    },
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
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered) { product ->
                                ProductCard(
                                    product = product,
                                    onEdit = {
                                        productToEdit = product
                                        showAddProductDialog = true
                                    },
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
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered) { employee ->
                                EmployeeCard(
                                    employee = employee,
                                    onClick = { viewModel.openEmployeeDetail(employee) },
                                    onEdit = {
                                        employeeToEdit = employee
                                        showAddEmployeeDialog = true
                                    },
                                    onDelete = { viewModel.deleteEmployee(employee) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddCustomerDialog) {
        AddEditCustomerDialog(
            customer = customerToEdit,
            onDismiss = { showAddCustomerDialog = false },
            onSave = {
                viewModel.saveCustomer(it)
                showAddCustomerDialog = false
            }
        )
    }

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

    if (showAddProductDialog) {
        AddEditProductDialog(
            product = productToEdit,
            suppliers = suppliers,
            onDismiss = { showAddProductDialog = false },
            onSave = {
                viewModel.saveProduct(it)
                showAddProductDialog = false
            }
        )
    }

    if (showAddEmployeeDialog) {
        AddEditEmployeeDialog(
            employee = employeeToEdit,
            onDismiss = { showAddEmployeeDialog = false },
            onSave = {
                viewModel.saveEmployee(it)
                showAddEmployeeDialog = false
            }
        )
    }
}

@Composable
fun CustomerCard(
    customer: CustomerEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("(${customer.customerId})", fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(customer.city, fontSize = 12.sp, color = TextSecondary)
                    Text(" • ", color = TextSecondary)
                    Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(customer.phone, fontSize = 12.sp, color = TextSecondary)
                }
                if (customer.gstin.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("GSTIN: ${customer.gstin} • Credit: ${customer.creditDays} days", fontSize = 11.sp, color = NavyPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "👉 Click to view entries & day reports",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyPrimary
                )
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
                    Text(supplier.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    SupplierTypeBadge(type = supplier.type)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Market: ${supplier.marketArea} • Contact: ${supplier.contactPerson} (${supplier.phone})",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (supplier.brand.isNotBlank()) {
                        Text("Brand: ${supplier.brand}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NavyPrimary)
                        Text(" • ", color = TextSecondary)
                    }
                    Text("Case size: ${supplier.defaultCaseSize} pcs", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Medium)
                }
                if (supplier.categories.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Categories: ${supplier.categories}",
                        fontSize = 11.sp,
                        color = NavyPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "👉 Click to view reports, bills & all entries",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF059669)
                )
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
    onDelete: () -> Unit
) {
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
                Text(
                    text = "ID: ${employee.employeeId} • Phone: ${employee.phone}",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "👉 Click to view Sahyog entries & pending bills",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD97706)
                )
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
