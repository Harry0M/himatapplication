package com.example.util

/**
 * A staff member asked for something to be deleted and is waiting on an admin.
 *
 * Staff deletes never remove data. The record is flagged `isDeleted` (so it drops out of the lists)
 * and one of these is filed under `deletion_requests/<collection>_<itemId>`. An admin then either
 * approves it, which really removes the record, or rejects it, which puts the record back untouched.
 */
data class DeletionRequest(
    val key: String = "",
    /** RTDB node the record lives in, e.g. "purchase_entries". */
    val collection: String = "",
    val itemId: Long = 0,
    /** Human summary written when the request was filed, e.g. "Order #HT-2711 - ABC (60 pcs ...)". */
    val itemSummary: String = "",
    val deletedBy: String = "",
    val deletedByEmail: String = "",
    val deletedByRole: String = "",
    val deletedAt: Long = 0,
    val deletionReason: String = ""
) {
    /** What to call this kind of record on screen. */
    val entityLabel: String
        get() = when (collection) {
            "visits" -> "Trip"
            "purchase_entries" -> "Order"
            "customers" -> "Customer"
            "suppliers" -> "Supplier"
            "products" -> "Product"
            "employees" -> "Staff"
            "brands" -> "Brand"
            "transporters" -> "Transporter"
            "markets" -> "Market"
            "cheques_pdc" -> "Cheque / PDC"
            "pack_groups" -> "Mixed pack"
            "leads" -> "Lead"
            else -> collection.replace('_', ' ').trim().ifBlank { "Record" }
        }

    /** Approving a trip also removes its orders, which is worth warning about. */
    val cascades: Boolean get() = collection == "visits"
}
