package com.example.util

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity

/**
 * One customer who has bought past the limit they were given.
 *
 * [outstanding] is what is still unpaid, not what they have ever bought — a customer who buys twice
 * their limit every month and settles every bill is not the problem this is looking for.
 */
data class CreditBreach(
    val customerId: Long,
    val customerName: String,
    val phone: String,
    val creditLimit: Double,
    val outstanding: Double
) {
    val overBy: Double get() = (outstanding - creditLimit).coerceAtLeast(0.0)

    /** How far past the limit, as a percentage, for the warning text. */
    val overPercent: Int
        get() = if (creditLimit <= 0.0) 0 else ((overBy / creditLimit) * 100).toInt()
}

/**
 * Watches credit customers against the limit on their record.
 *
 * Only "Credit" customers are checked. A cash customer has no limit to cross, and a customer whose
 * limit is left at zero is treated as "no limit set" rather than "limit of nothing" — otherwise every
 * such customer would be permanently in breach, the warning would always be on, and nobody would
 * read it.
 *
 * Pure so it can be unit tested and so the same numbers appear on the home screen, in the
 * notification and on the customer's own screen.
 */
object CreditWatch {

    /** Bills that still count against the limit: live orders that are not fully paid. */
    private fun unpaidAmount(entry: PurchaseEntryEntity): Double {
        if (entry.isDeleted) return 0.0
        if (entry.paymentStatus.equals("Received", ignoreCase = true)) return 0.0
        val billed = if (entry.grandTotalWithGst > 0.0) entry.grandTotalWithGst else entry.totalAmount
        return (billed - entry.paidAmount).coerceAtLeast(0.0)
    }

    /** What this customer still owes across all their trips. */
    fun outstandingFor(
        customerId: Long,
        visits: List<VisitEntity>,
        entries: List<PurchaseEntryEntity>
    ): Double {
        val visitIds = visits.filter { !it.isDeleted && it.customerId == customerId }.map { it.id }.toSet()
        if (visitIds.isEmpty()) return 0.0
        return entries.filter { it.visitId in visitIds }.sumOf { unpaidAmount(it) }
    }

    fun isCreditCustomer(customer: CustomerEntity): Boolean =
        customer.customerType.equals("Credit", ignoreCase = true)

    /**
     * Every credit customer currently over their limit, worst first.
     *
     * [clearedIds] are the customers an admin has already acknowledged; they stay out until they cross
     * again by a further [reWarnStep] rupees, so clearing a warning does not simply mute it forever
     * while the balance keeps climbing.
     */
    fun breaches(
        customers: List<CustomerEntity>,
        visits: List<VisitEntity>,
        entries: List<PurchaseEntryEntity>,
        clearedAt: Map<Long, Double> = emptyMap(),
        reWarnStep: Double = 1.0
    ): List<CreditBreach> = customers
        .asSequence()
        .filter { !it.isDeleted && isCreditCustomer(it) && it.creditLimit > 0.0 }
        .mapNotNull { customer ->
            val outstanding = outstandingFor(customer.id, visits, entries)
            if (outstanding <= customer.creditLimit) return@mapNotNull null
            // Acknowledged at a balance this high or higher: stay quiet until it climbs further
            val ackedAt = clearedAt[customer.id]
            if (ackedAt != null && outstanding < ackedAt + reWarnStep) return@mapNotNull null
            CreditBreach(
                customerId = customer.id,
                customerName = customer.brandName().ifBlank { customer.name },
                phone = customer.phone,
                creditLimit = customer.creditLimit,
                outstanding = outstanding
            )
        }
        .sortedByDescending { it.overBy }
        .toList()
}
