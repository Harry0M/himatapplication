package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.theme.TextSecondary
import com.example.util.PdfGenerator
import com.example.util.Roles
import com.example.util.brandName
import com.example.util.membersDisplay
import com.example.util.primaryPhone

private const val PAGE = 20

@Composable
private fun ShowMore(shown: Int, total: Int, onMore: () -> Unit) {
    if (total > shown) {
        TextButton(onClick = onMore, modifier = Modifier.fillMaxWidth()) {
            Text("Show more (${total - shown} left)")
        }
    }
}

@Composable
private fun NoneText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
}

/** Numbers for the current filter: trips, orders, pieces, value and orders not yet delivered. */
@Composable
fun ActivitySummary(
    trips: Int,
    orders: List<PurchaseEntryEntity>,
    modifier: Modifier = Modifier
) {
    val pieces = orders.sumOf { it.pieces }
    val value = orders.sumOf { it.totalAmount }
    val pending = orders.count { !it.deliveryStatus.equals("Delivered", ignoreCase = true) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Trips", "$trips", Modifier.weight(1f))
            StatTile("Orders", "${orders.size}", Modifier.weight(1f))
            StatTile("Pieces", "$pieces", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Value", "₹${PdfGenerator.formatInr(value)}", Modifier.weight(1f))
            StatTile(
                "Not delivered",
                "$pending",
                Modifier.weight(1f),
                accent = if (pending > 0) Color(0xFFB45309) else Color(0xFF059669)
            )
        }
    }
}

@Composable
fun TripsList(
    trips: List<VisitEntity>,
    entries: List<PurchaseEntryEntity>,
    employees: List<EmployeeEntity>,
    onOpenVisit: (VisitEntity) -> Unit,
    emptyText: String = "No trips in this period"
) {
    var shown by remember(trips) { mutableIntStateOf(PAGE) }
    val byTrip = remember(entries) { entries.groupBy { it.visitId } }
    Column() {
        if (trips.isEmpty()) NoneText(emptyText)
        trips.take(shown).forEach { visit ->
            val tripOrders = byTrip[visit.id].orEmpty()
            TripRow(
                visit = visit,
                salesmen = visit.membersDisplay(employees),
                ordersCount = tripOrders.size,
                pieces = tripOrders.sumOf { it.pieces },
                onClick = { onOpenVisit(visit) }
            )
        }
        ShowMore(shown, trips.size) { shown += PAGE }
    }
}

@Composable
fun OrdersList(
    orders: List<PurchaseEntryEntity>,
    visitsById: Map<Long, VisitEntity>,
    onOpenOrder: (PurchaseEntryEntity) -> Unit,
    emptyText: String = "No orders in this period"
) {
    var shown by remember(orders) { mutableIntStateOf(PAGE) }
    Column() {
        if (orders.isEmpty()) NoneText(emptyText)
        orders.take(shown).forEach { entry ->
            OrderRow(
                entry = entry,
                customerName = visitsById[entry.visitId]?.customerName.orEmpty(),
                onClick = { onOpenOrder(entry) }
            )
        }
        ShowMore(shown, orders.size) { shown += PAGE }
    }
}

@Composable
fun CustomersList(
    customers: List<CustomerEntity>,
    onOpenCustomer: (CustomerEntity) -> Unit,
    emptyText: String = "No customers yet"
) {
    var shown by remember(customers) { mutableIntStateOf(PAGE) }
    Column() {
        if (customers.isEmpty()) NoneText(emptyText)
        customers.take(shown).forEach { c ->
            PersonRow(
                title = c.firmName.ifBlank { c.name },
                subtitle = listOf(c.name.takeIf { it != c.firmName }.orEmpty(), c.city, c.phone)
                    .filter { it.isNotBlank() }.joinToString(" • "),
                badge = "Customer",
                badgeColor = Color(0xFF059669),
                onClick = { onOpenCustomer(c) }
            )
        }
        ShowMore(shown, customers.size) { shown += PAGE }
    }
}

/** Everybody whose "Referred By" points at this master. */
@Composable
fun ReferredList(
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    people: List<EmployeeEntity>,
    onOpenCustomer: (CustomerEntity) -> Unit,
    onOpenSupplier: (SupplierEntity) -> Unit,
    onOpenEmployee: (EmployeeEntity) -> Unit,
    emptyText: String = "Nobody has been referred yet"
) {
    Column() {
        if (customers.isEmpty() && suppliers.isEmpty() && people.isEmpty()) NoneText(emptyText)
        customers.forEach { c ->
            PersonRow(
                title = c.firmName.ifBlank { c.name },
                subtitle = listOf(c.city, c.phone).filter { it.isNotBlank() }.joinToString(" • "),
                badge = "Customer",
                badgeColor = Color(0xFF059669),
                onClick = { onOpenCustomer(c) }
            )
        }
        suppliers.forEach { s ->
            PersonRow(
                title = s.firmName.ifBlank { s.name },
                subtitle = listOf(s.marketName.ifBlank { s.marketArea }, s.phone).filter { it.isNotBlank() }.joinToString(" • "),
                badge = "Supplier",
                badgeColor = Color(0xFF7C3AED),
                onClick = { onOpenSupplier(s) }
            )
        }
        people.forEach { p ->
            PersonRow(
                title = p.name,
                subtitle = listOf(p.firmName, p.phone).filter { it.isNotBlank() }.joinToString(" • "),
                badge = Roles.label(p.role),
                badgeColor = if (Roles.isAgent(p.role)) Color(0xFFB45309) else Color(0xFF2563EB),
                onClick = { onOpenEmployee(p) }
            )
        }
    }
}

@Composable
fun SuppliersList(
    suppliers: List<SupplierEntity>,
    onOpenSupplier: (SupplierEntity) -> Unit,
    emptyText: String = "No suppliers linked yet"
) {
    var shown by remember(suppliers) { mutableIntStateOf(PAGE) }
    Column {
        if (suppliers.isEmpty()) NoneText(emptyText)
        suppliers.take(shown).forEach { s ->
            ListRow(
                title = s.brandName(),
                detail = listOf(s.firmName.takeIf { it != s.brandName() }.orEmpty(), s.marketName.ifBlank { s.marketArea }, s.primaryPhone())
                    .filter { it.isNotBlank() }.joinToString(" • "),
                status = { StatusPill(s.type.ifBlank { "Supplier" }, Color(0xFF7C3AED).copy(alpha = 0.10f), Color(0xFF7C3AED)) },
                onClick = { onOpenSupplier(s) }
            )
        }
        ShowMore(shown, suppliers.size) { shown += PAGE }
    }
}

@Composable
fun ProductsList(
    products: List<ProductEntity>,
    onOpenProduct: (ProductEntity) -> Unit,
    emptyText: String = "No products linked yet"
) {
    var shown by remember(products) { mutableIntStateOf(PAGE) }
    Column {
        if (products.isEmpty()) NoneText(emptyText)
        products.take(shown).forEach { p ->
            ListRow(
                title = p.name.ifBlank { p.productCode },
                value = if (p.defaultRate > 0) "₹${PdfGenerator.formatInr(p.defaultRate)}" else null,
                detail = listOf(p.productCode, p.category, p.supplierName).filter { it.isNotBlank() }.joinToString(" • "),
                onClick = { onOpenProduct(p) }
            )
        }
        ShowMore(shown, products.size) { shown += PAGE }
    }
}
