package com.example.util

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity

/**
 * One set of records that are the same thing saved twice. [canonical] is the copy that is kept.
 */
data class DuplicateGroup<T>(
    val canonical: T,
    val duplicates: List<T>
)

/**
 * Finds records that are genuinely the same thing saved twice — usually one save from a phone and
 * one from the web admin, or a double tap.
 *
 * The whole point of this file is that the *rules* are conservative and testable. The old inline
 * version collapsed on keys that two different real records can share, and then hard-deleted the
 * loser from the cloud. Two examples it got wrong:
 *
 *  - two customers with the same owner name and no phone number yet became "duplicates";
 *  - one item code sold by two different suppliers became "duplicates".
 *
 * So every rule here needs a field that is actually distinguishing (a phone number, a supplier) and
 * refuses to group when that field is missing. Nothing is deleted by this file: it only reports.
 */
object DuplicateScan {

    private fun norm(value: String): String = value.trim().lowercase()

    private fun <T> collapse(
        rows: List<T>,
        key: (T) -> String?,
        pickCanonical: (List<T>) -> T
    ): List<DuplicateGroup<T>> = rows
        .mapNotNull { row -> key(row)?.let { it to row } }
        .groupBy({ it.first }, { it.second })
        .values
        .filter { it.size > 1 }
        .map { group ->
            val canonical = pickCanonical(group)
            DuplicateGroup(canonical, group.filter { it !== canonical })
        }

    /**
     * Same owner name AND same phone number. A blank phone never groups: "Ramesh" with no number is
     * not evidence of anything, and the agency has several of those.
     */
    fun customers(customers: List<CustomerEntity>): List<DuplicateGroup<CustomerEntity>> = collapse(
        rows = customers.filter { !it.isDeleted },
        key = { customer ->
            val phone = customer.phone.trim()
            val name = norm(customer.name)
            if (phone.isBlank() || name.isBlank()) null else "$name|$phone"
        },
        pickCanonical = { group -> group.minByOrNull { it.id } ?: group.first() }
    )

    /** Same name, same brand and same phone. Again, a blank phone never groups. */
    fun suppliers(suppliers: List<SupplierEntity>): List<DuplicateGroup<SupplierEntity>> = collapse(
        rows = suppliers.filter { !it.isDeleted },
        key = { supplier ->
            val phone = supplier.phone.trim()
            val name = norm(supplier.name)
            if (phone.isBlank() || name.isBlank()) null else "$name|${norm(supplier.brand)}|$phone"
        },
        pickCanonical = { group -> group.minByOrNull { it.id } ?: group.first() }
    )

    /**
     * The same item code under the same supplier. Scoping by supplier matters: two mills genuinely
     * sell "DENIM-701", and collapsing those loses one supplier's rate and case size.
     */
    fun products(products: List<ProductEntity>): List<DuplicateGroup<ProductEntity>> = collapse(
        rows = products.filter { !it.isDeleted },
        key = { product ->
            val code = norm(product.productCode)
            val name = norm(product.name)
            when {
                product.supplierId <= 0L -> null
                code.isNotBlank() -> "${product.supplierId}|code|$code"
                name.isNotBlank() -> "${product.supplierId}|name|$name"
                else -> null
            }
        },
        pickCanonical = { group -> group.minByOrNull { it.id } ?: group.first() }
    )

    /**
     * A true double-save: every business field identical, on the same trip.
     *
     * Trip + order number alone is not a safe key — several salesmen add orders to one trip from
     * different phones — so the whole tuple has to match. An order this phone has not uploaded yet
     * is never considered a duplicate, because the copy it would be compared against may be the
     * very same order that did reach the office.
     */
    fun orders(entries: List<PurchaseEntryEntity>): List<DuplicateGroup<PurchaseEntryEntity>> = collapse(
        rows = entries.filter { !it.isDeleted && !it.pendingPush },
        key = { entry ->
            if (entry.orderNo.isBlank() || entry.visitId <= 0L) null
            else listOf(
                entry.visitId,
                entry.orderNo.trim(),
                entry.supplierId,
                norm(entry.itemCode),
                entry.pieces,
                entry.rate,
                entry.caseCount,
                entry.loosePieces,
                entry.salesmanId,
                entry.createdById
            ).joinToString("|")
        },
        // A copy that is already part of a mixed pack is the one the packing notes point at
        pickCanonical = { group ->
            group.firstOrNull { it.packGroupId != null } ?: group.minByOrNull { it.id } ?: group.first()
        }
    )
}
