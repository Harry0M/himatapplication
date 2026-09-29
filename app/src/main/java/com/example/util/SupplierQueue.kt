package com.example.util

/** Which two buttons the order form shows while a supplier queue is running. */
enum class QueueButtons { NEXT_AND_SKIP, FINISH_AND_SKIP }

/**
 * A phone order: the customer named several suppliers, so the order form opens once per supplier
 * instead of the salesman walking the market. Nothing else about an order changes.
 *
 * Immutable: every step returns the next state.
 */
data class SupplierQueue(
    val tripId: Long,
    val supplierIds: List<Long>,
    val salesmanId: Long,
    val salesmanName: String,
    val index: Int = 0,
    val savedCount: Int = 0,
    val finished: Boolean = false
) {
    val total: Int get() = supplierIds.size

    val currentSupplierId: Long? get() = if (finished) null else supplierIds.getOrNull(index)

    val isLast: Boolean get() = index >= supplierIds.lastIndex

    /** "2 of 4" */
    val progressLabel: String get() = "${(index + 1).coerceAtMost(total)} of $total"

    val buttons: QueueButtons get() = if (isLast) QueueButtons.FINISH_AND_SKIP else QueueButtons.NEXT_AND_SKIP

    fun saveAndNext(): SupplierQueue = advance(saved = true)

    fun skip(): SupplierQueue = advance(saved = false)

    fun saveAndFinish(): SupplierQueue =
        if (finished) this else copy(savedCount = savedCount + 1, finished = true)

    private fun advance(saved: Boolean): SupplierQueue {
        if (finished) return this
        val count = if (saved) savedCount + 1 else savedCount
        return if (isLast) copy(savedCount = count, finished = true) else copy(index = index + 1, savedCount = count)
    }

    companion object {
        /**
         * Keeps the order the user picked the suppliers in; drops repeats and unsaved ids.
         * Returns null when there is no supplier to work through.
         */
        fun start(tripId: Long, supplierIds: List<Long>, salesmanId: Long, salesmanName: String): SupplierQueue? {
            val ids = supplierIds.filter { it > 0L }.distinct()
            return if (ids.isEmpty()) null
            else SupplierQueue(tripId = tripId, supplierIds = ids, salesmanId = salesmanId, salesmanName = salesmanName)
        }
    }
}
