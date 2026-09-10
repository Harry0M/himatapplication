package com.example

import com.example.data.local.entity.GarmentItemEntity
import com.example.data.local.entity.SupplierEntity
import com.example.util.RecordValidator
import com.example.util.ValidationResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordValidatorTest {

    @Test
    fun testValidSupplier_passesValidation() {
        val validSupplier = SupplierEntity(
            id = 1,
            supplierId = "SUP-101",
            name = "Vardhman Denim Mills",
            type = "Manufacturer",
            brand = "V-Denim",
            gstin = "24AABCV9876B1Z2",
            address = "Plot 88, GIDC",
            city = "Surat",
            marketArea = "Surat Textile Market",
            contactPerson = "Sanjay Bhai",
            phone = "+91 98251 23456",
            email = "contact@vardhmandenim.com",
            categories = "Denim",
            defaultCaseSize = 24,
            rating = 4.8f
        )
        val result = RecordValidator.validateSupplier(validSupplier)
        assertTrue("Expected valid supplier to pass, but got: ${(result as? ValidationResult.Invalid)?.errors}", result.isValid)
    }

    @Test
    fun testSupplierWithBlankName_failsValidation() {
        val invalidSupplier = SupplierEntity(
            supplierId = "SUP-001",
            name = "",
            type = "Manufacturer",
            city = "Surat",
            marketArea = "Ring Road",
            defaultCaseSize = 24
        )
        val result = RecordValidator.validateSupplier(invalidSupplier)
        assertFalse(result.isValid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("Supplier name is required", ignoreCase = true) })
    }

    @Test
    fun testSupplierWithInvalidType_failsValidation() {
        val invalidSupplier = SupplierEntity(
            supplierId = "SUP-002",
            name = "Test Textiles",
            type = "Retailer", // Must be Manufacturer or Wholesaler
            marketArea = "Gandhi Nagar",
            defaultCaseSize = 24
        )
        val result = RecordValidator.validateSupplier(invalidSupplier)
        assertFalse(result.isValid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("Manufacturer", ignoreCase = true) && it.contains("Wholesaler", ignoreCase = true) })
    }

    @Test
    fun testSupplierWithoutLocation_failsValidation() {
        val invalidSupplier = SupplierEntity(
            supplierId = "SUP-003",
            name = "Test Textiles",
            type = "Manufacturer",
            address = "",
            city = "",
            marketArea = "",
            defaultCaseSize = 24
        )
        val result = RecordValidator.validateSupplier(invalidSupplier)
        assertFalse(result.isValid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("location", ignoreCase = true) || it.contains("Market area", ignoreCase = true) })
    }

    @Test
    fun testSupplierWithZeroOrNegativeCaseSize_failsValidation() {
        val invalidSupplier = SupplierEntity(
            supplierId = "SUP-004",
            name = "Test Textiles",
            type = "Manufacturer",
            marketArea = "Ring Road",
            defaultCaseSize = 0
        )
        val result = RecordValidator.validateSupplier(invalidSupplier)
        assertFalse(result.isValid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("case size", ignoreCase = true) })
    }

    @Test
    fun testSupplierWithMalformedGstin_failsValidation() {
        val invalidSupplier = SupplierEntity(
            supplierId = "SUP-005",
            name = "Test Textiles",
            type = "Manufacturer",
            marketArea = "Ring Road",
            defaultCaseSize = 24,
            gstin = "INVALID_GST"
        )
        val result = RecordValidator.validateSupplier(invalidSupplier)
        assertFalse(result.isValid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("GSTIN", ignoreCase = true) })
    }

    @Test
    fun testValidGarmentItem_passesValidation() {
        val validItem = GarmentItemEntity(
            id = 1,
            itemCode = "DENIM-701",
            name = "Slim Fit Stretch Jeans 701",
            category = "Denim",
            supplierId = 1,
            supplierName = "Vardhman Denim Mills",
            defaultRate = 450.0,
            defaultCaseSize = 24,
            fabricType = "Cotton Spandex",
            sizeRange = "28-36",
            hsnCode = "6203",
            gstRate = 5.0,
            inStockPieces = 120
        )
        val result = RecordValidator.validateGarmentItem(validItem)
        assertTrue("Expected valid garment item to pass, but got: ${(result as? ValidationResult.Invalid)?.errors}", result.isValid)
    }

    @Test
    fun testGarmentItemWithBlankCodeOrName_failsValidation() {
        val blankCodeItem = GarmentItemEntity(
            itemCode = "",
            name = "Test Jeans",
            category = "Denim",
            supplierId = 1,
            supplierName = "Supplier",
            defaultRate = 350.0,
            defaultCaseSize = 20
        )
        val blankCodeResult = RecordValidator.validateGarmentItem(blankCodeItem)
        assertFalse(blankCodeResult.isValid)
        assertTrue((blankCodeResult as ValidationResult.Invalid).errors.any { it.contains("Item code is required", ignoreCase = true) })

        val blankNameItem = GarmentItemEntity(
            itemCode = "JEANS-01",
            name = "",
            category = "Denim",
            supplierId = 1,
            supplierName = "Supplier",
            defaultRate = 350.0,
            defaultCaseSize = 20
        )
        val blankNameResult = RecordValidator.validateGarmentItem(blankNameItem)
        assertFalse(blankNameResult.isValid)
        assertTrue((blankNameResult as ValidationResult.Invalid).errors.any { it.contains("name is required", ignoreCase = true) })
    }

    @Test
    fun testGarmentItemWithZeroRateOrCaseSize_failsValidation() {
        val zeroRateItem = GarmentItemEntity(
            itemCode = "JEANS-01",
            name = "Test Jeans",
            category = "Denim",
            supplierId = 1,
            supplierName = "Supplier",
            defaultRate = 0.0,
            defaultCaseSize = 20
        )
        val zeroRateResult = RecordValidator.validateGarmentItem(zeroRateItem)
        assertFalse(zeroRateResult.isValid)
        assertTrue((zeroRateResult as ValidationResult.Invalid).errors.any { it.contains("rate must be greater", ignoreCase = true) })

        val zeroCaseItem = GarmentItemEntity(
            itemCode = "JEANS-01",
            name = "Test Jeans",
            category = "Denim",
            supplierId = 1,
            supplierName = "Supplier",
            defaultRate = 400.0,
            defaultCaseSize = 0
        )
        val zeroCaseResult = RecordValidator.validateGarmentItem(zeroCaseItem)
        assertFalse(zeroCaseResult.isValid)
        assertTrue((zeroCaseResult as ValidationResult.Invalid).errors.any { it.contains("case size", ignoreCase = true) })
    }

    @Test
    fun testGarmentItemWithoutSupplier_failsValidation() {
        val unassignedItem = GarmentItemEntity(
            itemCode = "JEANS-01",
            name = "Test Jeans",
            category = "Denim",
            supplierId = 0L,
            supplierName = "",
            defaultRate = 400.0,
            defaultCaseSize = 24
        )
        val result = RecordValidator.validateGarmentItem(unassignedItem)
        assertFalse(result.isValid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("supplier", ignoreCase = true) })
    }
}
