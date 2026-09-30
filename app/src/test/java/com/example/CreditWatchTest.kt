package com.example

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import com.example.util.CreditWatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The credit warning is only useful if it is quiet when it should be. A warning that is always on
 * gets ignored, and then the one that matters gets ignored too — so the cases that must NOT warn are
 * as important here as the ones that must.
 */
class CreditWatchTest {

    private fun customer(
        id: Long,
        limit: Double,
        type: String = "Credit",
        name: String = "Mahesh Garments"
    ) = CustomerEntity(id = id, name = name, firmName = name, customerType = type, creditLimit = limit)

    private fun trip(id: Long, customerId: Long) = VisitEntity(id = id, customerId = customerId)

    private fun order(
        id: Long,
        visitId: Long,
        billed: Double,
        paid: Double = 0.0,
        status: String = "Pending",
        deleted: Boolean = false
    ) = PurchaseEntryEntity(
        id = id,
        visitId = visitId,
        grandTotalWithGst = billed,
        paidAmount = paid,
        paymentStatus = status,
        isDeleted = deleted
    )

    // -------------------------------------------------------------------------
    // Outstanding
    // -------------------------------------------------------------------------

    @Test
    fun outstandingCountsOnlyWhatIsStillUnpaid() {
        val visits = listOf(trip(1, 10))
        val entries = listOf(
            order(101, 1, billed = 50_000.0),
            order(102, 1, billed = 30_000.0, paid = 10_000.0),
            order(103, 1, billed = 20_000.0, status = "Received")
        )

        // 50,000 + (30,000 - 10,000) + 0 (settled)
        assertEquals(70_000.0, CreditWatch.outstandingFor(10, visits, entries), 0.01)
    }

    @Test
    fun aDeletedOrderIsNotOwed() {
        val visits = listOf(trip(1, 10))
        val entries = listOf(order(101, 1, billed = 50_000.0, deleted = true))

        assertEquals(0.0, CreditWatch.outstandingFor(10, visits, entries), 0.01)
    }

    @Test
    fun anotherCustomersTripsDoNotCount() {
        val visits = listOf(trip(1, 10), trip(2, 11))
        val entries = listOf(order(101, 1, billed = 40_000.0), order(102, 2, billed = 90_000.0))

        assertEquals(40_000.0, CreditWatch.outstandingFor(10, visits, entries), 0.01)
    }

    // -------------------------------------------------------------------------
    // When to warn
    // -------------------------------------------------------------------------

    @Test
    fun warnsWhenACreditCustomerGoesOverTheirLimit() {
        val breaches = CreditWatch.breaches(
            customers = listOf(customer(10, limit = 50_000.0)),
            visits = listOf(trip(1, 10)),
            entries = listOf(order(101, 1, billed = 62_000.0))
        )

        val breach = breaches.single()
        assertEquals(10L, breach.customerId)
        assertEquals(12_000.0, breach.overBy, 0.01)
        assertEquals(24, breach.overPercent)
    }

    @Test
    fun exactlyAtTheLimitIsNotOver() {
        val breaches = CreditWatch.breaches(
            customers = listOf(customer(10, limit = 50_000.0)),
            visits = listOf(trip(1, 10)),
            entries = listOf(order(101, 1, billed = 50_000.0))
        )

        assertTrue(breaches.isEmpty())
    }

    @Test
    fun aCashCustomerHasNoLimitToCross() {
        val breaches = CreditWatch.breaches(
            customers = listOf(customer(10, limit = 50_000.0, type = "Cash")),
            visits = listOf(trip(1, 10)),
            entries = listOf(order(101, 1, billed = 90_000.0))
        )

        assertTrue(breaches.isEmpty())
    }

    @Test
    fun aZeroLimitMeansNoLimitSet_notALimitOfNothing() {
        val breaches = CreditWatch.breaches(
            customers = listOf(customer(10, limit = 0.0)),
            visits = listOf(trip(1, 10)),
            entries = listOf(order(101, 1, billed = 90_000.0))
        )

        assertTrue("Otherwise every such customer warns forever and nobody reads it", breaches.isEmpty())
    }

    @Test
    fun aDeletedCustomerIsNotWarnedAbout() {
        val breaches = CreditWatch.breaches(
            customers = listOf(customer(10, limit = 10_000.0).copy(isDeleted = true)),
            visits = listOf(trip(1, 10)),
            entries = listOf(order(101, 1, billed = 90_000.0))
        )

        assertTrue(breaches.isEmpty())
    }

    @Test
    fun worstOffenderComesFirst() {
        val breaches = CreditWatch.breaches(
            customers = listOf(
                customer(10, limit = 50_000.0, name = "Small over"),
                customer(11, limit = 50_000.0, name = "Big over")
            ),
            visits = listOf(trip(1, 10), trip(2, 11)),
            entries = listOf(order(101, 1, billed = 55_000.0), order(102, 2, billed = 90_000.0))
        )

        assertEquals(listOf(11L, 10L), breaches.map { it.customerId })
    }

    // -------------------------------------------------------------------------
    // Clearing a warning
    // -------------------------------------------------------------------------

    @Test
    fun anAcknowledgedBalanceStaysQuiet() {
        val breaches = CreditWatch.breaches(
            customers = listOf(customer(10, limit = 50_000.0)),
            visits = listOf(trip(1, 10)),
            entries = listOf(order(101, 1, billed = 62_000.0)),
            clearedAt = mapOf(10L to 62_000.0)
        )

        assertTrue(breaches.isEmpty())
    }

    @Test
    fun clearingIsNotAPermanentMute_itComesBackIfTheyKeepBuying() {
        val breaches = CreditWatch.breaches(
            customers = listOf(customer(10, limit = 50_000.0)),
            visits = listOf(trip(1, 10)),
            entries = listOf(order(101, 1, billed = 62_000.0), order(102, 1, billed = 5_000.0)),
            clearedAt = mapOf(10L to 62_000.0)
        )

        assertEquals(67_000.0, breaches.single().outstanding, 0.01)
    }
}
