package com.example.util

import com.example.data.local.entity.GarmentItemEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SupplierEntity

/**
 * Result representation for input validation.
 */
sealed class ValidationResult {
    abstract val isValid: Boolean

    object Valid : ValidationResult() {
        override val isValid: Boolean get() = true
    }

    data class Invalid(val errors: List<String>) : ValidationResult() {
        override val isValid: Boolean get() = false
        val errorMessage: String get() = errors.joinToString("\n")
        val singleLineMessage: String get() = errors.joinToString("; ")
        val firstError: String get() = errors.firstOrNull() ?: "Validation failed"
    }
}

/**
 * Custom exception thrown when a record fails repository/viewmodel validation.
 */
class RecordValidationException(val errors: List<String>) :
    IllegalArgumentException("Validation failed: ${errors.joinToString("; ")}")

/**
 * Centralized business validation engine to guarantee completeness and data integrity
 * for Supplier details and Garment Item records before database persistence.
 */
object RecordValidator {

    // Standard 15-character GSTIN regex: 2 digits state + 10 chars PAN + 1 entity + 1 'Z' + 1 check digit
    private val GSTIN_REGEX = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Validates supplier details completeness before insertion/update.
     */
    fun validateSupplier(supplier: SupplierEntity): ValidationResult {
        val errors = mutableListOf<String>()

        // 1. Supplier Name
        val cleanName = supplier.name.trim()
        if (cleanName.isBlank()) {
            errors.add("Supplier name is required and cannot be blank.")
        } else if (cleanName.length < 2) {
            errors.add("Supplier name must be at least 2 characters long.")
        }

        // 2. Supplier Type (Manufacturer or Wholesaler)
        val cleanType = supplier.type.trim()
        val validTypes = listOf("Manufacturer", "Wholesaler")
        if (cleanType.isBlank()) {
            errors.add("Supplier type (Manufacturer or Wholesaler) is required.")
        } else if (!validTypes.any { it.equals(cleanType, ignoreCase = true) }) {
            errors.add("Supplier type must be either 'Manufacturer' or 'Wholesaler'.")
        }

        // 3. Location / Market Area completeness (Wholesale textile gaddi/market location)
        val hasLocation = supplier.marketArea.trim().isNotBlank() ||
                supplier.city.trim().isNotBlank() ||
                supplier.address.trim().isNotBlank()
        if (!hasLocation) {
            errors.add("Market area, city, or physical address is required to identify supplier location.")
        }

        // 4. Default Case Size
        if (supplier.defaultCaseSize <= 0) {
            errors.add("Default case size must be greater than 0 pieces (carton/pack size).")
        } else if (supplier.defaultCaseSize > 1000) {
            errors.add("Default case size cannot exceed 1000 pieces.")
        }

        // 5. Phone validation (if provided)
        if (supplier.phone.isNotBlank()) {
            val digitsOnly = supplier.phone.filter { it.isDigit() }
            if (digitsOnly.length < 7) {
                errors.add("Contact phone must contain at least 7 digits.")
            }
        }

        // 6. Email validation (if provided)
        if (supplier.email.isNotBlank()) {
            if (!EMAIL_REGEX.matches(supplier.email.trim())) {
                errors.add("Email address format is invalid (e.g. name@domain.com).")
            }
        }

        // 7. GSTIN validation (if provided)
        if (supplier.gstin.isNotBlank()) {
            val cleanGstin = supplier.gstin.trim().uppercase()
            if (cleanGstin.length != 15) {
                errors.add("GSTIN must be exactly 15 alphanumeric characters (e.g. 24AABCV9876B1Z2).")
            } else if (!GSTIN_REGEX.matches(cleanGstin)) {
                errors.add("GSTIN format is invalid (expected 2-digit state code + 10-char PAN + entity code + Z + checksum).")
            }
        }

        // 8. Rating bounds
        if (supplier.rating < 0f || supplier.rating > 5f) {
            errors.add("Supplier rating must be between 0.0 and 5.0.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates garment item records completeness before insertion/update.
     */
    fun validateGarmentItem(item: GarmentItemEntity): ValidationResult {
        val errors = mutableListOf<String>()

        // 1. Item Code
        val cleanCode = item.itemCode.trim()
        if (cleanCode.isBlank()) {
            errors.add("Item code is required (e.g. 'DENIM-701', 'KURTI-101').")
        } else if (cleanCode.length < 2) {
            errors.add("Item code must be at least 2 characters.")
        }

        // 2. Garment Item Name
        val cleanName = item.name.trim()
        if (cleanName.isBlank()) {
            errors.add("Garment item name is required and cannot be blank.")
        } else if (cleanName.length < 2) {
            errors.add("Garment item name must be at least 2 characters.")
        }

        // 3. Category
        if (item.category.trim().isBlank()) {
            errors.add("Garment category is required (e.g. 'Kurtis', 'Shirts', 'Denim', 'Ethnic Wear').")
        }

        // 4. Supplier Association
        if (item.supplierId <= 0L) {
            errors.add("A valid supplier must be assigned to the garment item.")
        }
        if (item.supplierName.trim().isBlank()) {
            errors.add("Supplier name is required for garment item record.")
        }

        // 5. Default Rate per piece
        if (item.defaultRate <= 0.0) {
            errors.add("Default rate must be greater than ₹0.00 per piece.")
        } else if (item.defaultRate > 100000.0) {
            errors.add("Default rate exceeds maximum allowed value.")
        }

        // 6. Master Carton / Case Size
        if (item.defaultCaseSize <= 0) {
            errors.add("Case size must be greater than 0 pieces.")
        } else if (item.defaultCaseSize > 1000) {
            errors.add("Case size cannot exceed 1000 pieces.")
        }

        // 7. GST Rate
        if (item.gstRate < 0.0) {
            errors.add("GST rate cannot be negative.")
        } else if (item.gstRate > 40.0) {
            errors.add("GST rate cannot exceed 40%.")
        }

        // 8. Stock Count
        if (item.inStockPieces < 0) {
            errors.add("In-stock pieces quantity cannot be negative.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates product entity completeness.
     */
    fun validateProduct(product: ProductEntity): ValidationResult {
        val errors = mutableListOf<String>()

        if (product.productCode.trim().isBlank()) {
            errors.add("Product code is required.")
        }
        if (product.name.trim().isBlank()) {
            errors.add("Product name is required.")
        }
        if (product.supplierId <= 0L) {
            errors.add("A valid supplier must be selected.")
        }
        if (product.supplierName.trim().isBlank()) {
            errors.add("Supplier name is required.")
        }
        if (product.defaultRate <= 0.0) {
            errors.add("Default rate must be greater than ₹0.00.")
        }
        if (product.defaultCaseSize <= 0) {
            errors.add("Case size must be greater than 0 pieces.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}
