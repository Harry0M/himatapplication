package com.example.util

import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.ChequePdcEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.MarketEntity
import com.example.data.local.entity.PackGroupEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.TransporterEntity
import com.example.data.local.entity.VisitEntity

/**
 * What kind of record a delete is about. The string values match the RTDB node names so the same
 * value can be used for a deletion request key.
 */
enum class RecordKind(val node: String, val label: String) {
    VISIT("visits", "Trip"),
    ORDER("purchase_entries", "Order"),
    CUSTOMER("customers", "Customer"),
    SUPPLIER("suppliers", "Supplier"),
    PRODUCT("products", "Product"),
    BRAND("brands", "Brand"),
    TRANSPORTER("transporters", "Transporter"),
    MARKET("markets", "Market"),
    STAFF("employees", "Staff"),
    CHEQUE("cheques_pdc", "Cheque")
}

/**
 * One group of records that are attached to the thing being deleted.
 *
 * [removable] means the app can delete these along with the parent. When it is false the records
 * only *mention* the parent (by name, not by id) and would merely lose a label, so they are reported
 * as a heads-up and never touched.
 */
data class OrphanGroup(
    val label: String,
    val kind: RecordKind,
    val ids: List<Long>,
    val removable: Boolean,
    /** Shown under the group, e.g. "these orders would no longer belong to any trip". */
    val consequence: String
) {
    val count: Int get() = ids.size
}

/**
 * Everything that hangs off one record, worked out before anything is deleted.
 *
 * The point is that nobody deletes a trip and silently loses its orders. [removableGroups] can be
 * cleaned up together with the parent; [advisoryGroups] are only reported.
 */
data class DeleteImpact(
    val kind: RecordKind,
    val id: Long,
    val title: String,
    val groups: List<OrphanGroup> = emptyList()
) {
    val removableGroups: List<OrphanGroup> get() = groups.filter { it.removable && it.count > 0 }
    val advisoryGroups: List<OrphanGroup> get() = groups.filter { !it.removable && it.count > 0 }

    /** True when something is attached, so the user has to be asked before going ahead. */
    val hasImpact: Boolean get() = groups.any { it.count > 0 }

    /** How many records would be removed if the user says "delete these too". */
    val removableCount: Int get() = removableGroups.sumOf { it.count }

    /** Every id that would be removed alongside the parent, grouped by kind. */
    fun removableIdsByKind(): Map<RecordKind, List<Long>> =
        removableGroups.groupBy { it.kind }.mapValues { (_, groups) -> groups.flatMap { it.ids }.distinct() }
}

/**
 * Works out what a delete would leave behind.
 *
 * Everything here is pure: it takes the current lists and returns a report, so it can be unit tested
 * and reused by any screen. Only live (not already deleted) records are counted — there is no point
 * warning about something that is already hidden.
 */
object OrphanScan {

    /** All the data the scan needs. Pass the full lists; the scan filters deleted rows itself. */
    data class Data(
        val visits: List<VisitEntity> = emptyList(),
        val entries: List<PurchaseEntryEntity> = emptyList(),
        val packGroups: List<PackGroupEntity> = emptyList(),
        val customers: List<CustomerEntity> = emptyList(),
        val suppliers: List<SupplierEntity> = emptyList(),
        val products: List<ProductEntity> = emptyList(),
        val brands: List<BrandEntity> = emptyList(),
        val transporters: List<TransporterEntity> = emptyList(),
        val markets: List<MarketEntity> = emptyList(),
        val employees: List<EmployeeEntity> = emptyList(),
        val cheques: List<ChequePdcEntity> = emptyList()
    )

    private fun String.matches(other: String): Boolean =
        isNotBlank() && other.isNotBlank() && trim().equals(other.trim(), ignoreCase = true)

    // -------------------------------------------------------------------------
    // Trip
    // -------------------------------------------------------------------------

    fun forVisit(visit: VisitEntity, data: Data): DeleteImpact {
        val orders = data.entries.filter { !it.isDeleted && it.visitId == visit.id }
        val packs = data.packGroups.filter { it.visitId == visit.id }
        return DeleteImpact(
            kind = RecordKind.VISIT,
            id = visit.id,
            title = "Trip ${visit.visitCode.ifBlank { visit.id.toString() }} • ${visit.customerName.ifBlank { "Customer" }}",
            groups = listOf(
                OrphanGroup(
                    label = "Orders on this trip",
                    kind = RecordKind.ORDER,
                    ids = orders.map { it.id },
                    removable = true,
                    consequence = "They would not belong to any trip, so they disappear from the customer's report"
                ),
                OrphanGroup(
                    label = "Mixed packs on this trip",
                    kind = RecordKind.VISIT,
                    ids = packs.map { it.id },
                    removable = false,
                    consequence = "Packing notes on the affected orders would no longer make sense"
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // Order
    // -------------------------------------------------------------------------

    fun forOrder(entry: PurchaseEntryEntity, data: Data): DeleteImpact {
        // A mixed pack that lists this order among its linked entries
        val packs = data.packGroups.filter { group ->
            group.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() }.contains(entry.id)
        }
        return DeleteImpact(
            kind = RecordKind.ORDER,
            id = entry.id,
            title = "Order ${entry.orderNo.ifBlank { entry.id.toString() }} • ${entry.supplierName.ifBlank { "Supplier" }}",
            groups = listOf(
                OrphanGroup(
                    label = "Mixed packs that include it",
                    kind = RecordKind.ORDER,
                    ids = packs.map { it.id },
                    removable = false,
                    consequence = "The pack would be short by ${entry.loosePieces.coerceAtLeast(0)} loose pieces"
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // Customer
    // -------------------------------------------------------------------------

    fun forCustomer(customer: CustomerEntity, data: Data): DeleteImpact {
        val visits = data.visits.filter { !it.isDeleted && it.customerId == customer.id }
        val visitIds = visits.map { it.id }.toSet()
        val orders = data.entries.filter { !it.isDeleted && it.visitId in visitIds }
        val cheques = data.cheques.filter {
            !it.isDeleted && it.partyType.equals("Customer", true) && it.partyId == customer.id
        }
        return DeleteImpact(
            kind = RecordKind.CUSTOMER,
            id = customer.id,
            title = customer.brandName().ifBlank { "Customer ${customer.id}" },
            groups = listOf(
                OrphanGroup(
                    label = "Trips for this customer",
                    kind = RecordKind.VISIT,
                    ids = visits.map { it.id },
                    removable = true,
                    consequence = "The trips would have no customer"
                ),
                OrphanGroup(
                    label = "Orders on those trips",
                    kind = RecordKind.ORDER,
                    ids = orders.map { it.id },
                    removable = true,
                    consequence = "Their money and delivery history is lost with them"
                ),
                OrphanGroup(
                    label = "Cheques / PDC for this customer",
                    kind = RecordKind.CUSTOMER,
                    ids = cheques.map { it.id },
                    removable = false,
                    consequence = "They would point at a customer that no longer exists"
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // Supplier
    // -------------------------------------------------------------------------

    fun forSupplier(supplier: SupplierEntity, data: Data): DeleteImpact {
        val orders = data.entries.filter { entry ->
            !entry.isDeleted && (entry.supplierId == supplier.id || entry.supplierName.matches(supplier.name))
        }
        val products = data.products.filter { !it.isDeleted && it.supplierId == supplier.id }
        val cheques = data.cheques.filter {
            !it.isDeleted && it.partyType.equals("Supplier", true) && it.partyId == supplier.id
        }
        return DeleteImpact(
            kind = RecordKind.SUPPLIER,
            id = supplier.id,
            title = supplier.brandName().ifBlank { "Supplier ${supplier.id}" },
            groups = listOf(
                OrphanGroup(
                    label = "Orders from this supplier",
                    kind = RecordKind.ORDER,
                    ids = orders.map { it.id },
                    removable = true,
                    consequence = "Customers' reports would lose these purchases"
                ),
                OrphanGroup(
                    label = "Products under this supplier",
                    kind = RecordKind.PRODUCT,
                    ids = products.map { it.id },
                    removable = true,
                    consequence = "The products would have no supplier"
                ),
                OrphanGroup(
                    label = "Cheques / PDC for this supplier",
                    kind = RecordKind.SUPPLIER,
                    ids = cheques.map { it.id },
                    removable = false,
                    consequence = "They would point at a supplier that no longer exists"
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // Product
    // -------------------------------------------------------------------------

    fun forProduct(product: ProductEntity, data: Data): DeleteImpact {
        val orders = data.entries.filter { !it.isDeleted && it.itemCode.matches(product.productCode) }
        return DeleteImpact(
            kind = RecordKind.PRODUCT,
            id = product.id,
            title = product.name.ifBlank { "Product ${product.id}" },
            groups = listOf(
                OrphanGroup(
                    label = "Orders using this item code",
                    kind = RecordKind.ORDER,
                    ids = orders.map { it.id },
                    removable = false,
                    consequence = "The orders stay, they just no longer match a product in the master"
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // Brand
    // -------------------------------------------------------------------------

    fun forBrand(brand: BrandEntity, data: Data): DeleteImpact {
        val suppliers = data.suppliers.filter { supplier ->
            !supplier.isDeleted && (
                supplier.brandId == brand.id ||
                    supplier.brand.matches(brand.brandName) ||
                    (brand.manufacturerId != null && brand.manufacturerId == supplier.id)
                )
        }
        return DeleteImpact(
            kind = RecordKind.BRAND,
            id = brand.id,
            title = brand.brandName.ifBlank { "Brand ${brand.id}" },
            groups = listOf(
                OrphanGroup(
                    label = "Suppliers linked to this brand",
                    kind = RecordKind.SUPPLIER,
                    ids = suppliers.map { it.id },
                    removable = false,
                    consequence = "The suppliers stay, but their brand link is lost"
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // Transporter
    // -------------------------------------------------------------------------

    fun forTransporter(transporter: TransporterEntity, data: Data): DeleteImpact {
        val orders = data.entries.filter { !it.isDeleted && it.transporter.matches(transporter.transporterName) }
        return DeleteImpact(
            kind = RecordKind.TRANSPORTER,
            id = transporter.id,
            title = transporter.transporterName.ifBlank { "Transporter ${transporter.id}" },
            groups = listOf(
                OrphanGroup(
                    label = "Orders booked with this transporter",
                    kind = RecordKind.ORDER,
                    ids = orders.map { it.id },
                    removable = false,
                    consequence = "The orders keep the name, but it is no longer in the master"
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // Market
    // -------------------------------------------------------------------------

    fun forMarket(market: MarketEntity, data: Data): DeleteImpact {
        val name = market.marketName
        val customers = data.customers.filter { customer ->
            !customer.isDeleted && (
                customer.marketArea.matches(name) ||
                    customer.markets.split(",").any { it.matches(name) }
                )
        }
        val suppliers = data.suppliers.filter { supplier ->
            !supplier.isDeleted && (
                supplier.marketId == market.id ||
                    supplier.marketArea.matches(name) ||
                    supplier.marketName.matches(name) ||
                    supplier.markets.split(",").any { it.matches(name) }
                )
        }
        return DeleteImpact(
            kind = RecordKind.MARKET,
            id = market.id,
            title = market.marketName.ifBlank { "Market ${market.id}" },
            groups = listOf(
                OrphanGroup(
                    label = "Customers in this market",
                    kind = RecordKind.CUSTOMER,
                    ids = customers.map { it.id },
                    removable = false,
                    consequence = "They keep the market name, but it disappears from the pickers"
                ),
                OrphanGroup(
                    label = "Suppliers in this market",
                    kind = RecordKind.SUPPLIER,
                    ids = suppliers.map { it.id },
                    removable = false,
                    consequence = "The supplier registration form would no longer offer this market"
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // Cheque / PDC
    // -------------------------------------------------------------------------

    /**
     * A cheque has nothing hanging off it, so there is never an orphan to warn about. It goes through
     * the same pipeline anyway: a cheque is a money record, so a staff delete has to become a request
     * an admin answers rather than the record simply vanishing.
     */
    fun forCheque(cheque: ChequePdcEntity): DeleteImpact = DeleteImpact(
        kind = RecordKind.CHEQUE,
        id = cheque.id,
        title = listOf(
            cheque.chequeNo.trim().takeIf { it.isNotBlank() }?.let { "Cheque $it" },
            cheque.partyName.trim().takeIf { it.isNotBlank() },
            cheque.amount.takeIf { it > 0.0 }?.let { "₹${it.toLong()}" }
        ).filterNotNull().joinToString(" • ").ifBlank { "Cheque ${cheque.id}" }
    )

    // -------------------------------------------------------------------------
    // Staff / Sub Agent
    // -------------------------------------------------------------------------

    fun forStaff(employee: EmployeeEntity, data: Data): DeleteImpact {
        val visits = data.visits.filter { !it.isDeleted && it.hasMember(employee) }
        val orders = data.entries.filter {
            !it.isDeleted && (it.salesmanId == employee.id || it.createdById == employee.id)
        }
        val linkedCustomers = data.customers.filter {
            !it.isDeleted && (it.subAgentId == employee.id || it.subAgentName.matches(employee.name))
        }
        return DeleteImpact(
            kind = RecordKind.STAFF,
            id = employee.id,
            title = employee.name.ifBlank { "Staff ${employee.id}" },
            groups = listOf(
                OrphanGroup(
                    label = "Trips they are on",
                    kind = RecordKind.VISIT,
                    ids = visits.map { it.id },
                    removable = false,
                    consequence = "The trips stay, but a salesman on them no longer exists"
                ),
                OrphanGroup(
                    label = "Orders credited to them",
                    kind = RecordKind.ORDER,
                    ids = orders.map { it.id },
                    removable = false,
                    consequence = "Their name stays printed on past customer reports"
                ),
                OrphanGroup(
                    label = "Customers linked to them as Sub Agent",
                    kind = RecordKind.CUSTOMER,
                    ids = linkedCustomers.map { it.id },
                    removable = false,
                    consequence = "The customers would have no Sub Agent"
                )
            )
        )
    }
}
