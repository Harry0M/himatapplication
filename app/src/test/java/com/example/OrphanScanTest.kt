package com.example

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
import com.example.util.DeleteImpact
import com.example.util.OrphanScan
import com.example.util.RecordKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The orphan scan is what stands between a delete and a silently lost order, so every link it is
 * supposed to find is pinned down here: what gets cascaded, what is only reported, and the fact that
 * an already deleted row is never counted twice.
 */
class OrphanScanTest {

    private val customer = CustomerEntity(id = 10, name = "Mahesh", firmName = "Mahesh Garments")
    private val supplier = SupplierEntity(id = 20, name = "Vardhman", brand = "V-Denim")

    private fun trip(id: Long, customerId: Long = 10, deleted: Boolean = false) = VisitEntity(
        id = id,
        visitCode = "HT-$id",
        customerId = customerId,
        customerName = "Mahesh Garments",
        employeeId = 5,
        employeeName = "Ramesh",
        isDeleted = deleted
    )

    private fun order(
        id: Long,
        visitId: Long = 0,
        supplierId: Long = 0,
        supplierName: String = "",
        itemCode: String = "",
        transporter: String = "",
        salesmanId: Long = 0,
        deleted: Boolean = false
    ) = PurchaseEntryEntity(
        id = id,
        orderNo = "ORD-$id",
        visitId = visitId,
        supplierId = supplierId,
        supplierName = supplierName,
        itemCode = itemCode,
        transporter = transporter,
        salesmanId = salesmanId,
        isDeleted = deleted
    )

    // -------------------------------------------------------------------------
    // Trip
    // -------------------------------------------------------------------------

    @Test
    fun deletingTrip_reportsItsOrdersAsRemovable() {
        val data = OrphanScan.Data(
            entries = listOf(
                order(id = 101, visitId = 1),
                order(id = 102, visitId = 1),
                order(id = 103, visitId = 2) // different trip, must not be touched
            )
        )

        val impact = OrphanScan.forVisit(trip(1), data)

        assertTrue("A trip with orders on it must warn", impact.hasImpact)
        assertEquals(2, impact.removableCount)
        assertEquals(listOf(101L, 102L), impact.removableIdsByKind()[RecordKind.ORDER])
    }

    @Test
    fun deletingTrip_ignoresOrdersThatAreAlreadyDeleted() {
        val data = OrphanScan.Data(
            entries = listOf(
                order(id = 101, visitId = 1),
                order(id = 102, visitId = 1, deleted = true)
            )
        )

        val impact = OrphanScan.forVisit(trip(1), data)

        assertEquals(1, impact.removableCount)
        assertEquals(listOf(101L), impact.removableIdsByKind()[RecordKind.ORDER])
    }

    @Test
    fun deletingEmptyTrip_reportsNoImpact() {
        val data = OrphanScan.Data(entries = listOf(order(id = 101, visitId = 9)))

        val impact = OrphanScan.forVisit(trip(1), data)

        assertFalse("An empty trip must delete straight away", impact.hasImpact)
        assertEquals(0, impact.removableCount)
        assertTrue(impact.removableIdsByKind().isEmpty())
    }

    @Test
    fun deletingTrip_reportsMixedPacksAsAdvisoryOnly() {
        val data = OrphanScan.Data(
            packGroups = listOf(PackGroupEntity(id = 55, visitId = 1, packGroupCode = "PK-1"))
        )

        val impact = OrphanScan.forVisit(trip(1), data)

        assertTrue(impact.hasImpact)
        assertEquals(0, impact.removableCount)
        assertEquals(1, impact.advisoryGroups.sumOf { it.count })
    }

    // -------------------------------------------------------------------------
    // Customer
    // -------------------------------------------------------------------------

    @Test
    fun deletingCustomer_cascadesTripsAndTheirOrders() {
        val data = OrphanScan.Data(
            visits = listOf(
                trip(id = 1, customerId = 10),
                trip(id = 2, customerId = 10),
                trip(id = 3, customerId = 99) // another customer
            ),
            entries = listOf(
                order(id = 101, visitId = 1),
                order(id = 102, visitId = 2),
                order(id = 103, visitId = 3)
            )
        )

        val impact = OrphanScan.forCustomer(customer, data)

        assertTrue(impact.hasImpact)
        val removable = impact.removableIdsByKind()
        assertEquals(listOf(1L, 2L), removable[RecordKind.VISIT])
        assertEquals(listOf(101L, 102L), removable[RecordKind.ORDER])
        assertEquals(4, impact.removableCount)
    }

    @Test
    fun deletingCustomer_reportsChequesButNeverRemovesThem() {
        val data = OrphanScan.Data(
            cheques = listOf(
                ChequePdcEntity(id = 70, partyType = "Customer", partyId = 10, chequeNo = "CH-1"),
                ChequePdcEntity(id = 71, partyType = "Supplier", partyId = 10, chequeNo = "CH-2")
            )
        )

        val impact = OrphanScan.forCustomer(customer, data)

        assertTrue(impact.hasImpact)
        assertEquals(0, impact.removableCount)
        assertEquals(listOf(70L), impact.advisoryGroups.single().ids)
    }

    // -------------------------------------------------------------------------
    // Supplier
    // -------------------------------------------------------------------------

    @Test
    fun deletingSupplier_cascadesOrdersAndProducts() {
        val data = OrphanScan.Data(
            entries = listOf(
                order(id = 101, supplierId = 20),
                // legacy row saved with the name only
                order(id = 102, supplierName = " vardhman "),
                order(id = 103, supplierId = 21, supplierName = "Other Mills")
            ),
            products = listOf(
                ProductEntity(id = 201, productCode = "DENIM-701", supplierId = 20),
                ProductEntity(id = 202, productCode = "TEE-01", supplierId = 21)
            )
        )

        val impact = OrphanScan.forSupplier(supplier, data)

        val removable = impact.removableIdsByKind()
        assertEquals(listOf(101L, 102L), removable[RecordKind.ORDER])
        assertEquals(listOf(201L), removable[RecordKind.PRODUCT])
        assertEquals(3, impact.removableCount)
    }

    @Test
    fun deletingSupplier_withNothingAttached_reportsNoImpact() {
        val impact = OrphanScan.forSupplier(supplier, OrphanScan.Data())

        assertFalse(impact.hasImpact)
        assertEquals(0, impact.removableCount)
    }

    // -------------------------------------------------------------------------
    // Master records: reported, never cascaded
    // -------------------------------------------------------------------------

    @Test
    fun deletingProduct_onlyWarnsAboutOrdersUsingTheItemCode() {
        val product = ProductEntity(id = 201, productCode = "DENIM-701", name = "Slim Jeans")
        val data = OrphanScan.Data(
            entries = listOf(
                order(id = 101, itemCode = "DENIM-701"),
                order(id = 102, itemCode = "denim-701"),
                order(id = 103, itemCode = "TEE-01")
            )
        )

        val impact = OrphanScan.forProduct(product, data)

        assertTrue(impact.hasImpact)
        assertNoCascade(impact)
        assertEquals(listOf(101L, 102L), impact.advisoryGroups.single().ids)
    }

    @Test
    fun deletingBrand_onlyWarnsAboutItsSuppliers() {
        val brand = BrandEntity(id = 30, brandName = "V-Denim")
        val data = OrphanScan.Data(
            suppliers = listOf(
                supplier.copy(id = 20, brandId = 30),
                supplier.copy(id = 21, brandId = null, brand = "V-Denim"),
                supplier.copy(id = 22, brandId = null, brand = "Something Else")
            )
        )

        val impact = OrphanScan.forBrand(brand, data)

        assertTrue(impact.hasImpact)
        assertNoCascade(impact)
        assertEquals(listOf(20L, 21L), impact.advisoryGroups.single().ids)
    }

    @Test
    fun deletingTransporter_onlyWarnsAboutBookedOrders() {
        val transporter = TransporterEntity(id = 40, transporterName = "VRL Logistics")
        val data = OrphanScan.Data(
            entries = listOf(
                order(id = 101, transporter = "VRL Logistics"),
                order(id = 102, transporter = "")
            )
        )

        val impact = OrphanScan.forTransporter(transporter, data)

        assertTrue(impact.hasImpact)
        assertNoCascade(impact)
        assertEquals(listOf(101L), impact.advisoryGroups.single().ids)
    }

    @Test
    fun deletingMarket_onlyWarnsAboutCustomersAndSuppliersInIt() {
        val market = MarketEntity(id = 50, marketName = "Ring Road")
        val data = OrphanScan.Data(
            customers = listOf(
                customer.copy(id = 10, marketArea = "Ring Road"),
                customer.copy(id = 11, marketArea = "", markets = "New Cloth Market, Ring Road"),
                customer.copy(id = 12, marketArea = "Dhalgarwad")
            ),
            suppliers = listOf(
                supplier.copy(id = 20, marketId = 50),
                supplier.copy(id = 21, marketId = null, marketArea = "Ring Road"),
                supplier.copy(id = 22, marketId = null, marketArea = "Sindhi Market")
            )
        )

        val impact = OrphanScan.forMarket(market, data)

        assertTrue(impact.hasImpact)
        assertNoCascade(impact)
        assertEquals(listOf(10L, 11L), impact.advisoryGroups.first { it.kind == RecordKind.CUSTOMER }.ids)
        assertEquals(listOf(20L, 21L), impact.advisoryGroups.first { it.kind == RecordKind.SUPPLIER }.ids)
    }

    @Test
    fun deletingStaff_onlyWarnsAboutTheirTripsOrdersAndCustomers() {
        val staff = EmployeeEntity(id = 5, name = "Ramesh", role = "Salesman")
        val data = OrphanScan.Data(
            visits = listOf(
                trip(id = 1).copy(employeeId = 5),
                trip(id = 2).copy(employeeId = 6, employeeName = "Suresh", memberIds = "6,5", memberNames = "Suresh, Ramesh"),
                trip(id = 3).copy(employeeId = 7, employeeName = "Dinesh")
            ),
            entries = listOf(order(id = 101, salesmanId = 5), order(id = 102, salesmanId = 7)),
            customers = listOf(
                customer.copy(id = 10, subAgentId = 5),
                customer.copy(id = 11, subAgentId = null)
            )
        )

        val impact = OrphanScan.forStaff(staff, data)

        assertTrue(impact.hasImpact)
        assertNoCascade(impact)
        assertEquals(listOf(1L, 2L), impact.advisoryGroups.first { it.kind == RecordKind.VISIT }.ids)
        assertEquals(listOf(101L), impact.advisoryGroups.first { it.kind == RecordKind.ORDER }.ids)
        assertEquals(listOf(10L), impact.advisoryGroups.first { it.kind == RecordKind.CUSTOMER }.ids)
    }

    // -------------------------------------------------------------------------
    // Order
    // -------------------------------------------------------------------------

    @Test
    fun deletingOrder_warnsWhenItIsInsideAMixedPack() {
        val entry = order(id = 101, visitId = 1)
        val data = OrphanScan.Data(
            packGroups = listOf(
                PackGroupEntity(id = 55, visitId = 1, linkedEntryIds = "101,102"),
                PackGroupEntity(id = 56, visitId = 1, linkedEntryIds = "102,103")
            )
        )

        val impact = OrphanScan.forOrder(entry, data)

        assertTrue(impact.hasImpact)
        assertNoCascade(impact)
        assertEquals(listOf(55L), impact.advisoryGroups.single().ids)
    }

    @Test
    fun deletingLooseOrder_reportsNoImpact() {
        val impact = OrphanScan.forOrder(order(id = 101, visitId = 1), OrphanScan.Data())

        assertFalse(impact.hasImpact)
    }

    /** An advisory link only loses a label, so it must never end up in the cascade delete list. */
    private fun assertNoCascade(impact: DeleteImpact) {
        assertEquals("Advisory links must never be cascaded", 0, impact.removableCount)
        assertTrue(impact.removableGroups.isEmpty())
        assertTrue(impact.removableIdsByKind().isEmpty())
    }
}
