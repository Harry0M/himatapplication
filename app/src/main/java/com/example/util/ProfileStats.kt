package com.example.util

import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * What one person did this month, for the Profile screen.
 *
 * "Mine" means: trips this person started or joined, and orders credited to them. An admin sees
 * the whole agency's numbers, because every trip and order is theirs.
 */
data class ProfileStats(
    val trips: Int = 0,
    val orders: Int = 0,
    val pieces: Int = 0,
    val amount: Double = 0.0,
    val monthLabel: String = ""
) {
    companion object {
        private val ymd = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        /** First day of the current month as yyyy-MM-dd, e.g. "2026-09-01". */
        fun monthStart(now: Date = Date()): String {
            val cal = Calendar.getInstance()
            cal.time = now
            cal.set(Calendar.DAY_OF_MONTH, 1)
            return ymd.format(cal.time)
        }

        fun monthLabel(now: Date = Date()): String = monthFmt.format(now)

        /**
         * [employee] null with [isAdmin] true = the whole agency. [employee] set = only their work.
         * Dates are compared as plain yyyy-MM-dd strings, the same format the rest of the app saves.
         */
        fun compute(
            visits: List<VisitEntity>,
            entries: List<PurchaseEntryEntity>,
            employee: EmployeeEntity?,
            isAdmin: Boolean,
            now: Date = Date()
        ): ProfileStats {
            val from = monthStart(now)
            val visitById = visits.associateBy { it.id }

            val myVisits = visits.filter { visit ->
                val date = visit.date.take(10)
                date >= from && (isAdmin || employee == null || visit.hasMember(employee))
            }
            val myVisitIds = myVisits.map { it.id }.toSet()

            val myEntries = entries.filter { entry ->
                val date = entry.orderDate.ifBlank { visitById[entry.visitId]?.date.orEmpty() }.take(10)
                if (date < from) return@filter false
                when {
                    isAdmin || employee == null -> true
                    // Credited to me, or booked on a trip I am on
                    entry.salesmanId == employee.id -> true
                    entry.createdById == employee.id -> true
                    else -> entry.visitId in myVisitIds
                }
            }

            return ProfileStats(
                trips = myVisits.size,
                orders = myEntries.size,
                pieces = myEntries.sumOf { it.pieces },
                amount = myEntries.sumOf { if (it.grandTotalWithGst > 0.0) it.grandTotalWithGst else it.totalAmount },
                monthLabel = monthLabel(now)
            )
        }
    }
}

/** "RK" for "Ramesh Kumar", "R" for one word, "?" for nothing. Used for the avatar. */
fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase(Locale.getDefault())
        else -> (parts[0].take(1) + parts[parts.size - 1].take(1)).uppercase(Locale.getDefault())
    }
}
