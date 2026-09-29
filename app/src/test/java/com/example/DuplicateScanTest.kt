package com.example

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.util.DuplicateScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The duplicate check hides records, so a wrong guess hides real work. These tests pin down the cases
 * that used to be collapsed wrongly: two people with no phone number yet, and one item code sold by
 * two different mills.
 */
class DuplicateScanTest {

    private fun customer(id: Long, name: String, phone: String = "") =
        CustomerEntity(id = id, name = name, phone = phone)

    private fun supplier(id: Long, name: String, brand: String = "", phone: String = "") =
        SupplierEntity(id = id, name = name, brand = brand, phone = phone)

    private fun product(id: Long, code: String, supplierId: Long, name: String = "Item") =
        ProductEntity(id = id, productCode = code, name = name, supplierId = supplierId)

    private fun order(
        id: Long,
        visitId: Long = 1,
        orderNo: String = "ORD-1",
        pieces: Int = 10,
        packGroupId: Long? = null,
        pending: Boolean = false
    ) = PurchaseEntryEntity(
        id = id,
        visitId = visitId,
        orderNo = orderNo,
        supplierId = 20,
        itemCode = "DENIM-701",
        pieces = pieces,
        rate = 450.0,
        packGroupId = packGroupId
    ).also { it.pendingPush = pending }

    // -------------------------------------------------------------------------
    // Customers
    // -------------------------------------------------------------------------

    @Test
    fun sameNameAndPhone_isADuplicate_andTheOldestIsKept() {
        val groups = DuplicateScan.customers(
            listOf(
                customer(id = 20, name = "Mahesh", phone = "9812345678"),
                customer(id = 10, name = "mahesh", phone = "9812345678")
            )
        )

        assertEquals(1, groups.size)
        assertEquals(10L, groups.single().canonical.id)
        assertEquals(listOf(20L), groups.single().duplicates.map { it.id })
    }

    @Test
    fun sameNameButNoPhone_isNotADuplicate() {
        val groups = DuplicateScan.customers(
            listOf(
                customer(id = 10, name = "Mahesh"),
                customer(id = 11, name = "Mahesh")
            )
        )

        assertTrue("Two customers with no number yet are not evidence of anything", groups.isEmpty())
    }

    @Test
    fun sameNameDifferentPhone_isNotADuplicate() {
        val groups = DuplicateScan.customers(
            listOf(
                customer(id = 10, name = "Mahesh", phone = "9812345678"),
                customer(id = 11, name = "Mahesh", phone = "9800000000")
            )
        )

        assertTrue(groups.isEmpty())
    }

    @Test
    fun alreadyHiddenCustomer_isIgnored() {
        val groups = DuplicateScan.customers(
            listOf(
                customer(id = 10, name = "Mahesh", phone = "9812345678"),
                customer(id = 11, name = "Mahesh", phone = "9812345678").copy(isDeleted = true)
            )
        )

        assertTrue(groups.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Suppliers
    // -------------------------------------------------------------------------

    @Test
    fun sameSupplierNameBrandAndPhone_isADuplicate() {
        val groups = DuplicateScan.suppliers(
            listOf(
                supplier(id = 10, name = "Vardhman", brand = "V-Denim", phone = "9811111111"),
                supplier(id = 11, name = "vardhman", brand = "v-denim", phone = "9811111111")
            )
        )

        assertEquals(listOf(11L), groups.single().duplicates.map { it.id })
    }

    @Test
    fun sameSupplierNameWithoutPhone_isNotADuplicate() {
        val groups = DuplicateScan.suppliers(
            listOf(
                supplier(id = 10, name = "Vardhman", brand = "V-Denim"),
                supplier(id = 11, name = "Vardhman", brand = "V-Denim")
            )
        )

        assertTrue(groups.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Products
    // -------------------------------------------------------------------------

    @Test
    fun sameItemCodeUnderOneSupplier_isADuplicate() {
        val groups = DuplicateScan.products(
            listOf(
                product(id = 10, code = "DENIM-701", supplierId = 20),
                product(id = 11, code = "denim-701", supplierId = 20)
            )
        )

        assertEquals(listOf(11L), groups.single().duplicates.map { it.id })
    }

    @Test
    fun sameItemCodeFromTwoSuppliers_isNotADuplicate() {
        val groups = DuplicateScan.products(
            listOf(
                product(id = 10, code = "DENIM-701", supplierId = 20),
                product(id = 11, code = "DENIM-701", supplierId = 21)
            )
        )

        assertTrue("Two mills genuinely sell the same code at different rates", groups.isEmpty())
    }

    @Test
    fun productWithNoSupplier_isNeverGrouped() {
        val groups = DuplicateScan.products(
            listOf(
                product(id = 10, code = "DENIM-701", supplierId = 0),
                product(id = 11, code = "DENIM-701", supplierId = 0)
            )
        )

        assertTrue(groups.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Orders
    // -------------------------------------------------------------------------

    @Test
    fun identicalOrderSavedTwice_isADuplicate_andThePackedCopyIsKept() {
        val groups = DuplicateScan.orders(
            listOf(
                order(id = 10),
                order(id = 11, packGroupId = 55L)
            )
        )

        assertEquals(11L, groups.single().canonical.id)
        assertEquals(listOf(10L), groups.single().duplicates.map { it.id })
    }

    @Test
    fun ordersDifferingInPieces_areNotDuplicates() {
        val groups = DuplicateScan.orders(listOf(order(id = 10, pieces = 10), order(id = 11, pieces = 12)))

        assertTrue(groups.isEmpty())
    }

    @Test
    fun orderStillWaitingToUpload_isNeverTreatedAsADuplicate() {
        val groups = DuplicateScan.orders(
            listOf(
                order(id = 10),
                order(id = 11, pending = true)
            )
        )

        assertTrue("An unsent order may be the very copy that did reach the office", groups.isEmpty())
    }

    @Test
    fun orderWithoutOrderNumber_isNeverGrouped() {
        val groups = DuplicateScan.orders(listOf(order(id = 10, orderNo = ""), order(id = 11, orderNo = "")))

        assertTrue(groups.isEmpty())
    }
}
