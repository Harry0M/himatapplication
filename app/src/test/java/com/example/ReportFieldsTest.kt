package com.example

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import com.example.util.ReportFields
import com.example.util.ReportOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * What actually gets printed on a quotation, and what the sender is allowed to correct.
 *
 * The rule is one line: a blank override means "use the record". It matters because the sheet shows
 * these same values as the placeholder in each box, so if the resolver and the PDF ever disagreed the
 * sender would be shown one thing and send another.
 */
class ReportFieldsTest {

    private val visit = VisitEntity(
        id = 1,
        visitCode = "HT-TRIP-12",
        customerId = 2,
        customerName = "Shree Balaji Fashion",
        employeeId = 3,
        employeeName = "Jalam Singh",
        date = "2026-09-28"
    )

    private val customer = CustomerEntity(
        id = 2,
        name = "Jitendra Bhai",
        firmName = "Shree Balaji Fashion",
        phone = "+91 98765 43210",
        shopAddress = "Shop 14, Cloth Market",
        city = "Ahilyanagar",
        state = "Maharashtra",
        gstin = "27AABCS1429B1ZX",
        preferredTransporterName = "Shree Maruti Transport",
        transportPreference = "Ahmedabad (ADI)"
    )

    private fun order(transporter: String = "", lr: String = "") = PurchaseEntryEntity(
        id = 1,
        orderNo = "HT-2601",
        visitId = 1,
        itemCode = "KURTI-101",
        pieces = 24,
        rate = 350.0,
        totalAmount = 8400.0,
        transporter = transporter,
        lrNo = lr
    )

    private val today: String get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // ── Records come through untouched ───────────────────────────────────────

    @Test
    fun `with nothing typed over, the record is what prints`() {
        val f = ReportFields.resolve(visit, customer, listOf(order()))
        assertEquals("Shree Balaji Fashion", f.firmName)
        assertEquals("Jitendra Bhai", f.proprietor)
        assertEquals("+91 98765 43210", f.phone)
        assertEquals("27AABCS1429B1ZX", f.gstin)
        assertEquals("Shop 14, Cloth Market", f.address)
        assertEquals("Ahilyanagar, Maharashtra", f.cityState)
        assertEquals("Ahmedabad (ADI)", f.bookingStation)
        assertEquals("2026-09-28", f.dispatchDate)
    }

    /** The order's transporter beats the customer's usual one: it is the decision for this trip. */
    @Test
    fun `the transporter on the order wins over the customers usual one`() {
        val f = ReportFields.resolve(visit, customer, listOf(order(transporter = "Jaipur Golden")))
        assertEquals("Jaipur Golden", f.transporter)
    }

    @Test
    fun `with no transporter on the order the customers usual one is used`() {
        val f = ReportFields.resolve(visit, customer, listOf(order()))
        assertEquals("Shree Maruti Transport", f.transporter)
    }

    // ── Overrides win ────────────────────────────────────────────────────────

    @Test
    fun `a typed transporter replaces both the order and the customer record`() {
        val f = ReportFields.resolve(
            visit,
            customer,
            listOf(order(transporter = "Jaipur Golden")),
            ReportOverrides(transporter = "V.R.L. Logistics")
        )
        assertEquals("V.R.L. Logistics", f.transporter)
    }

    @Test
    fun `every field can be typed over`() {
        val overrides = ReportOverrides(
            firmName = "Balaji Readymade",
            proprietor = "Jitu Bhai",
            phone = "+91 90000 11111",
            gstin = "27ZZZZZ9999Z1ZZ",
            address = "New Shop 22, Ring Road",
            transporter = "V.R.L. Logistics",
            bookingStation = "Surat (ST)",
            lrNo = "VRL-99881",
            dispatchDate = "2026-10-02",
            deliveryTo = "Pune, Maharashtra"
        )
        val f = ReportFields.resolve(visit, customer, listOf(order(lr = "OLD-111")), overrides)

        assertEquals("Balaji Readymade", f.firmName)
        assertEquals("Jitu Bhai", f.proprietor)
        assertEquals("+91 90000 11111", f.phone)
        assertEquals("27ZZZZZ9999Z1ZZ", f.gstin)
        assertEquals("New Shop 22, Ring Road", f.address)
        assertEquals("V.R.L. Logistics", f.transporter)
        assertEquals("Surat (ST)", f.bookingStation)
        assertEquals("VRL-99881", f.lrNo)
        assertEquals("2026-10-02", f.dispatchDate)
        assertEquals("Pune, Maharashtra", f.deliveryTo)
    }

    /** Whitespace is not an edit; it must fall back to the record rather than print a blank line. */
    @Test
    fun `spaces typed into a box are not treated as an edit`() {
        val f = ReportFields.resolve(
            visit,
            customer,
            listOf(order()),
            ReportOverrides(transporter = "   ", phone = "\t")
        )
        assertEquals("Shree Maruti Transport", f.transporter)
        assertEquals("+91 98765 43210", f.phone)
    }

    @Test
    fun `a typed value is trimmed before it is printed`() {
        val f = ReportFields.resolve(visit, customer, listOf(order()), ReportOverrides(lrNo = "  VRL-4477  "))
        assertEquals("VRL-4477", f.lrNo)
    }

    // ── Nothing is invented ──────────────────────────────────────────────────

    /**
     * The card layout used to print "+91 98765 43210" and "Mr. Jitendra Bhai" when those fields were
     * empty — sample values from a design mock, on a document that goes to a real customer. An empty
     * field has to read as empty.
     */
    @Test
    fun `an empty record prints a dash, never invented data`() {
        val bare = CustomerEntity(id = 9, name = "", firmName = "", phone = "", city = "", state = "")
        val f = ReportFields.resolve(visit.copy(customerName = ""), bare, emptyList())

        assertEquals(ReportFields.BLANK, f.proprietor)
        assertEquals(ReportFields.BLANK, f.phone)
        assertEquals(ReportFields.BLANK, f.address)
        assertEquals(ReportFields.BLANK, f.deliveryTo)
        assertEquals(ReportFields.BLANK, f.bookingStation)
        assertFalse("a phone number must never be invented", f.phone.contains("98765"))
        assertFalse("a proprietor name must never be invented", f.proprietor.contains("Jitendra"))
        assertFalse("a transporter must never be invented", f.transporter.contains("Maruti"))
    }

    @Test
    fun `an unregistered customer is said to be unregistered`() {
        val f = ReportFields.resolve(visit, customer.copy(gstin = ""), emptyList())
        assertEquals("Unregistered", f.gstin)
    }

    @Test
    fun `an unknown transporter is to be advised`() {
        val f = ReportFields.resolve(visit, customer.copy(preferredTransporterName = ""), listOf(order()))
        assertEquals("To be advised", f.transporter)
    }

    @Test
    fun `with no customer record the trip name still carries the firm`() {
        val f = ReportFields.resolve(visit, null, emptyList())
        assertEquals("Shree Balaji Fashion", f.firmName)
    }

    // ── LR numbers ───────────────────────────────────────────────────────────

    @Test
    fun `nothing booked says so rather than showing a dash`() {
        val f = ReportFields.resolve(visit, customer, listOf(order(), order()))
        assertEquals("Not booked yet", f.lrNo)
    }

    @Test
    fun `one booked LR is printed as itself`() {
        val f = ReportFields.resolve(visit, customer, listOf(order(lr = "MRT-88213"), order()))
        assertEquals("MRT-88213", f.lrNo)
    }

    /** Several LRs on one trip is exactly what "One Bill One LR" is about, so the count is shown. */
    @Test
    fun `several booked LRs show the first and how many more`() {
        val orders = listOf(order(lr = "MRT-1"), order(lr = "MRT-2"), order(lr = "MRT-3"))
        val f = ReportFields.resolve(visit, customer, orders)
        assertEquals("MRT-1 +2 more", f.lrNo)
    }

    @Test
    fun `the same LR on two orders counts once`() {
        val orders = listOf(order(lr = "MRT-7"), order(lr = "MRT-7"))
        assertEquals("MRT-7", ReportFields.resolve(visit, customer, orders).lrNo)
    }

    // ── Dates ────────────────────────────────────────────────────────────────

    @Test
    fun `a trip with no date falls back to today`() {
        val f = ReportFields.resolve(visit.copy(date = ""), customer, emptyList())
        assertEquals(today, f.dispatchDate)
    }

    // ── Telling the sender what they changed ─────────────────────────────────

    @Test
    fun `no edits reads as empty`() {
        assertTrue(ReportOverrides().isEmpty)
        assertEquals(0, ReportOverrides().editedCount)
    }

    @Test
    fun `edits are counted so the sheet can say how many`() {
        val overrides = ReportOverrides(transporter = "VRL", lrNo = "VRL-1", phone = "   ")
        assertFalse(overrides.isEmpty)
        assertEquals("a blank box is not an edit", 2, overrides.editedCount)
    }

    @Test
    fun `clearing a box puts the record value back`() {
        val typed = ReportOverrides(transporter = "VRL Logistics")
        assertEquals("VRL Logistics", ReportFields.resolve(visit, customer, listOf(order()), typed).transporter)

        val cleared = typed.copy(transporter = "")
        assertTrue(cleared.isEmpty)
        assertEquals(
            "Shree Maruti Transport",
            ReportFields.resolve(visit, customer, listOf(order()), cleared).transporter
        )
    }
}
