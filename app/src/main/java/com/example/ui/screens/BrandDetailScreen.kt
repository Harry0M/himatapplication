package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.ActivitySummary
import com.example.ui.components.AppSearchField
import com.example.ui.components.ChoiceChips
import com.example.ui.components.CustomersList
import com.example.ui.components.DateRangeFilterBar
import com.example.ui.components.HeaderIconButton
import com.example.ui.components.InfoCard
import com.example.ui.components.OrdersList
import com.example.ui.components.ProductsList
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.SuppliersList
import com.example.ui.components.TripsList
import com.example.ui.components.UiDimens
import com.example.ui.dialogs.FullScreenImageViewerDialog
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.BrandLinks
import com.example.util.DateRangeFilter
import com.example.util.RelatedLogic
import com.example.util.brandName

/**
 * One brand and everything linked to it: its suppliers, their products, the orders placed with
 * them, the trips those orders came from and the customers who bought.
 */
@Composable
fun BrandDetailScreen(
    viewModel: HimatViewModel,
    brand: BrandEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onOpenProduct: (ProductEntity) -> Unit = { viewModel.openProductDetail(it) },
    onOpenOrder: (PurchaseEntryEntity) -> Unit = { viewModel.openOrderDetail(it, returnScreen = AppScreen.BRAND_DETAIL) },
    onOpenSupplier: (SupplierEntity) -> Unit = { viewModel.openSupplierDetail(it) },
    onOpenVisit: (VisitEntity) -> Unit = { viewModel.openVisitDetail(it) },
    onOpenCustomer: (CustomerEntity) -> Unit = { viewModel.openCustomerDetail(it) }
) {
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allSuppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val allEmployees by viewModel.allEmployees.collectAsStateWithLifecycle()

    var showLogoViewer by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("SUPPLIERS") }
    var dateFilter by remember { mutableStateOf(DateRangeFilter()) }

    val visitsById = remember(allVisits) { allVisits.associateBy { it.id } }

    // One rule for brand links, shared with the web admin
    val links = remember(brand, allSuppliers, allProducts, allEntries, allVisits, allCustomers) {
        BrandLinks.compute(brand, allSuppliers, allProducts, allEntries, allVisits, allCustomers)
    }

    // The date filter applies to orders and trips; suppliers, products and customers are not dated
    val orders = remember(links, dateFilter, visitsById) {
        RelatedLogic.filterOrdersByDate(links.orders, visitsById, dateFilter)
    }
    val trips = remember(links, dateFilter) { RelatedLogic.filterTripsByDate(links.trips, dateFilter) }

    val q = searchQuery.trim()
    fun matches(vararg fields: String) = q.isBlank() || fields.any { it.contains(q, ignoreCase = true) }

    val shownSuppliers = links.suppliers.filter { matches(it.brandName(), it.firmName, it.name, it.marketName, it.marketArea, it.phone) }
    val shownProducts = links.products.filter { matches(it.name, it.productCode, it.category, it.supplierName) }
    val shownOrders = orders.filter { matches(it.itemCode, it.orderNo, it.supplierName, it.deliveryStatus, visitsById[it.visitId]?.customerName.orEmpty()) }
    val shownTrips = trips.filter { matches(it.customerName, it.visitCode, it.date) }
    val shownCustomers = links.customers.filter { matches(it.brandName(), it.name, it.city, it.phone) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = brand.brandName.ifBlank { "Brand" },
                    subtitle = listOf(
                        brand.category.takeIf { it.isNotBlank() },
                        brand.manufacturerName.takeIf { it.isNotBlank() }?.let { "Made by $it" }
                    ).filterNotNull().joinToString(" • ").ifBlank { "Brand" },
                    onBack = onBack,
                    actions = {
                        HeaderIconButton(icon = Icons.Default.Edit, contentDescription = "Edit brand", onClick = onEdit)
                    }
                )
            }

            item(key = "profile") {
                Column(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(if (brand.logoPhotoUri.isNotBlank()) Modifier.clickable { showLogoViewer = true } else Modifier)
                            ) {
                                if (brand.logoPhotoUri.isNotBlank()) {
                                    AsyncImage(
                                        model = brand.logoPhotoUri,
                                        contentDescription = "${brand.brandName} logo, tap to view",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Sell,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = brand.brandName.ifBlank { "Brand" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (brand.manufacturerName.isNotBlank()) "Made by ${brand.manufacturerName}" else "No manufacturer set",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                if (brand.description.isNotBlank()) {
                                    Text(
                                        text = brand.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (brand.category.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusPill(
                                    brand.category,
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            item(key = "filters") {
                Column(
                    modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search suppliers, items, orders or customers"
                    )
                    DateRangeFilterBar(filter = dateFilter, onChange = { dateFilter = it })
                    ActivitySummary(trips = trips.size, orders = orders)
                    ChoiceChips(
                        options = listOf(
                            "SUPPLIERS" to "Suppliers (${links.suppliers.size})",
                            "PRODUCTS" to "Products (${links.products.size})",
                            "ORDERS" to "Orders (${orders.size})",
                            "TRIPS" to "Trips (${trips.size})",
                            "CUSTOMERS" to "Customers (${links.customers.size})"
                        ),
                        selected = tab,
                        onSelect = { tab = it }
                    )
                }
            }

            item(key = "section_title") {
                Column(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                    val (title, count) = when (tab) {
                        "PRODUCTS" -> "Products" to shownProducts.size
                        "ORDERS" -> "Orders" to shownOrders.size
                        "TRIPS" -> "Trips" to shownTrips.size
                        "CUSTOMERS" -> "Customers" to shownCustomers.size
                        else -> "Suppliers" to shownSuppliers.size
                    }
                    SectionHeader(title = title, count = count)
                }
            }

            item(key = "list") {
                when (tab) {
                    "PRODUCTS" -> ProductsList(
                        products = shownProducts,
                        onOpenProduct = onOpenProduct,
                        emptyText = "No products of this brand yet. Products follow the brand's suppliers."
                    )
                    "ORDERS" -> OrdersList(
                        orders = shownOrders,
                        visitsById = visitsById,
                        onOpenOrder = onOpenOrder,
                        emptyText = "No orders of this brand in this period"
                    )
                    "TRIPS" -> TripsList(
                        trips = shownTrips,
                        entries = shownOrders,
                        employees = allEmployees,
                        onOpenVisit = onOpenVisit,
                        emptyText = "No trips bought this brand in this period"
                    )
                    "CUSTOMERS" -> CustomersList(
                        customers = shownCustomers,
                        onOpenCustomer = onOpenCustomer,
                        emptyText = "No customer has bought this brand yet"
                    )
                    else -> SuppliersList(
                        suppliers = shownSuppliers,
                        onOpenSupplier = onOpenSupplier,
                        emptyText = "No suppliers linked to this brand. Open a supplier and set its brand, " +
                            "or set this brand's manufacturer."
                    )
                }
            }

            if (brand.logoPhotoUri.isNotBlank()) {
                item(key = "logo_hint") { Spacer(modifier = Modifier.height(4.dp)) }
            }
        }
    }

    if (showLogoViewer && brand.logoPhotoUri.isNotBlank()) {
        FullScreenImageViewerDialog(
            imageUrl = brand.logoPhotoUri,
            title = brand.brandName,
            onDismiss = { showLogoViewer = false }
        )
    }
}
