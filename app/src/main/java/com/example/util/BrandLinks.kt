package com.example.util

import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity

/** Everything linked to one brand. Empty lists when nothing is linked. */
data class BrandLinkResult(
    val suppliers: List<SupplierEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val orders: List<PurchaseEntryEntity> = emptyList(),
    val trips: List<VisitEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList()
)

/**
 * "Show me everything for this brand". The suppliers come first: a supplier belongs to a brand
 * through its brandId, its brand name, or because the brand names it as its manufacturer. Products,
 * orders, trips and customers then follow those suppliers, so the brand screen shows the same
 * records as the supplier screens.
 *
 * Pure functions (no Android types), and the web uses the same rules in lib/brandLinks.ts.
 */
object BrandLinks {

    fun compute(
        brand: BrandEntity,
        suppliers: List<SupplierEntity>,
        products: List<ProductEntity>,
        entries: List<PurchaseEntryEntity>,
        visits: List<VisitEntity>,
        customers: List<CustomerEntity>
    ): BrandLinkResult {
        val name = brand.brandName.trim()
        // A brand with no name can only be matched by id, never by text
        val useText = name.isNotEmpty()
        val manufacturerName = brand.manufacturerName.trim()

        val linkedSuppliers = suppliers
            .filter { s ->
                !s.isDeleted && (
                    (brand.id > 0L && s.brandId == brand.id) ||
                        (useText && s.brand.trim().equals(name, ignoreCase = true)) ||
                        (brand.manufacturerId != null && brand.manufacturerId != 0L && s.id == brand.manufacturerId) ||
                        (useText && manufacturerName.isNotEmpty() && (
                            s.firmName.trim().equals(manufacturerName, ignoreCase = true) ||
                                s.name.trim().equals(manufacturerName, ignoreCase = true)
                            ))
                    )
            }
            .distinctBy { it.id }
            .sortedBy { it.brandName().lowercase() }

        val supplierIds = linkedSuppliers.map { it.id }.toSet()

        val linkedProducts = products
            .filter { p ->
                !p.isDeleted && (
                    (p.supplierId > 0L && p.supplierId in supplierIds) ||
                        (useText && (
                            p.name.contains(name, ignoreCase = true) ||
                                p.productCode.contains(name, ignoreCase = true) ||
                                p.description.contains(name, ignoreCase = true)
                            ))
                    )
            }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

        val productCodes = linkedProducts
            .map { it.productCode.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

        val linkedOrders = entries
            .filter { e ->
                !e.isDeleted && (
                    (e.supplierId > 0L && e.supplierId in supplierIds) ||
                        (useText && (
                            e.itemCode.trim().lowercase() in productCodes ||
                                e.itemCode.contains(name, ignoreCase = true)
                            ))
                    )
            }
            .sortedWith(compareByDescending<PurchaseEntryEntity> { it.createdAt }.thenByDescending { it.id })

        val tripIds = linkedOrders.map { it.visitId }.toSet()
        val linkedTrips = visits
            .filter { !it.isDeleted && it.id in tripIds }
            .sortedByDescending { it.date }

        val linkedCustomers = linkedTrips
            .mapNotNull { RelatedLogic.customerOfTrip(it, customers) }
            .filter { !it.isDeleted }
            .distinctBy { it.id }
            .sortedBy { it.brandName().lowercase() }

        return BrandLinkResult(
            suppliers = linkedSuppliers,
            products = linkedProducts,
            orders = linkedOrders,
            trips = linkedTrips,
            customers = linkedCustomers
        )
    }
}
