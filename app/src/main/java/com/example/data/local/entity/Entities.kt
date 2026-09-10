package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: String,
    val name: String,
    val phone: String,
    val address: String,
    val city: String,
    val gstin: String = "",
    val defaultSalesmanId: Long? = null,
    val creditDays: Int = 30
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: String,
    val name: String,
    val type: String, // "Manufacturer" or "Wholesaler"
    val brand: String = "",
    val gstin: String = "",
    val address: String = "",
    val city: String = "",
    val marketArea: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val categories: String = "", // Categories of fabrics or garments provided (e.g. "Cotton, Denim, Rayon, Formal Shirts, Kurtis")
    val defaultCaseSize: Int = 24,
    val rating: Float = 4.5f,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val categoryList: List<String>
        get() = categories.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
}

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: String,
    val name: String,
    val phone: String,
    val role: String // "Admin" or "Salesman"
)

@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visitCode: String,
    val customerId: Long,
    val customerName: String,
    val employeeId: Long,
    val employeeName: String,
    val date: String,
    val notes: String = "",
    val status: String = "Active", // "Active", "Completed"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_entries")
data class PurchaseEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNo: String,
    val visitId: Long,
    val supplierId: Long,
    val supplierName: String,
    val supplierType: String, // "Manufacturer" or "Wholesaler"
    val itemCode: String, // e.g. "ABC", "XYZ", "Kurti 102"
    val pieces: Int,
    val rate: Double,
    val totalAmount: Double,
    val caseSize: Int,
    val caseCount: Int,
    val loosePieces: Int,
    val gstRate: Double = 5.0, // 5% standard garment GST
    val gstAmount: Double = 0.0,
    val grandTotalWithGst: Double = 0.0,
    val expectedDeliveryDate: String = "",
    val deliveryStatus: String = "Pending", // "Pending", "Packed", "Dispatched", "Delivered"
    val transporter: String = "",
    val orderFormPhotoUri: String? = null,
    val supplierInvoiceUri: String? = null,
    val packGroupId: Long? = null,
    val mixedPackNote: String? = null, // Shown on both customer report and supplier bill
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pack_groups")
data class PackGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visitId: Long,
    val packGroupCode: String,
    val linkedEntryIds: String, // comma-separated purchase_entry ids
    val combinedPieces: Int,
    val resultingCases: Int,
    val remainingLoose: Int,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productCode: String, // e.g. "DENIM-701", "COT-SHIRT", "TEE-OVERSZ"
    val name: String,
    val category: String = "Apparel", // "Denim", "Shirts", "Knitwear", "Hosiery", etc.
    val supplierId: Long,
    val supplierName: String,
    val defaultRate: Double,
    val defaultCaseSize: Int = 24,
    val hsnCode: String = "6203",
    val gstRate: Double = 5.0,
    val unit: String = "Pcs",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionNumber: String, // e.g. "TXN-2601"
    val orderNo: String = "", // e.g. "HT-2601"
    val visitId: Long = 0,
    val customerId: Long,
    val customerName: String,
    val supplierId: Long,
    val supplierName: String,
    val productId: Long? = null,
    val itemCode: String,
    val pieces: Int,
    val rate: Double,
    val totalAmount: Double,
    val gstRate: Double = 5.0,
    val gstAmount: Double = 0.0,
    val grandTotalWithGst: Double = 0.0,
    val caseSize: Int = 24,
    val caseCount: Int = 0,
    val loosePieces: Int = 0,
    val transactionType: String = "Purchase", // "Purchase", "Return", "Settlement"
    val paymentStatus: String = "Pending", // "Pending", "Partial", "Paid"
    val deliveryStatus: String = "Pending", // "Pending", "Packed", "Dispatched", "Delivered"
    val transporter: String = "",
    val mixedPackNote: String? = null,
    val transactionDate: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "garment_items")
data class GarmentItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemCode: String, // e.g. "KURTI-101", "DENIM-701", "COT-SHIRT-M"
    val name: String,
    val category: String = "Apparel", // "Kurtis", "Shirts", "Jeans", "T-Shirts", "Fabrics", "Ethnic Wear"
    val supplierId: Long,
    val supplierName: String,
    val defaultRate: Double,
    val defaultCaseSize: Int = 24,
    val hsnCode: String = "6203",
    val gstRate: Double = 5.0,
    val unit: String = "Pcs",
    val fabricType: String = "Cotton",
    val sizeRange: String = "M - XXL",
    val colorOptions: String = "Assorted",
    val description: String = "",
    val inStockPieces: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transaction_logs")
data class TransactionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logNumber: String, // e.g. "LOG-2026-001", "TXN-2601"
    val transactionType: String = "Purchase", // "Purchase", "Dispatch", "Delivery", "Return", "Settlement"
    val orderNo: String = "",
    val visitId: Long = 0,
    val customerId: Long = 0,
    val customerName: String = "",
    val supplierId: Long = 0,
    val supplierName: String = "",
    val garmentItemId: Long? = null,
    val itemCode: String = "",
    val pieces: Int = 0,
    val rate: Double = 0.0,
    val totalAmount: Double = 0.0,
    val gstRate: Double = 5.0,
    val gstAmount: Double = 0.0,
    val grandTotalWithGst: Double = 0.0,
    val caseSize: Int = 24,
    val caseCount: Int = 0,
    val loosePieces: Int = 0,
    val paymentStatus: String = "Pending", // "Pending", "Partial", "Paid"
    val deliveryStatus: String = "Pending", // "Pending", "Packed", "Dispatched", "Delivered"
    val transporter: String = "",
    val actionTakenBy: String = "Salesman",
    val remarks: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = ""
)

typealias Supplier = SupplierEntity
typealias Product = ProductEntity
typealias GarmentItem = GarmentItemEntity
typealias Transaction = TransactionEntity
typealias TransactionLog = TransactionLogEntity
