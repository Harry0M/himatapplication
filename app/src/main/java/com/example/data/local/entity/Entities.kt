package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: String = "",
    val name: String = "",
    val firmName: String = "",
    val phone: String = "",
    val phone2: String = "",
    val phone3: String = "",
    val phone4: String = "",
    val phone5: String = "",
    val email: String = "",
    val email2: String = "",
    val address: String = "",
    val shopAddress: String = "",
    val homeAddress: String = "",
    val shopLocation: String = "",
    val personalLocation: String = "",
    val shopCount: Int = 1,
    val shopLocations: String = "",
    val marketArea: String = "",
    val markets: String = "",
    val city: String = "Ahmedabad",
    val district: String = "",
    val state: String = "Gujarat",
    val pincode: String = "",
    val shopMapLink: String = "",
    val gstin: String = "",
    val panNumber: String = "",
    val customerType: String = "Credit", // "Cash" or "Credit"
    val contactsJson: String = "[]",
    val outletsJson: String = "[]",
    val garmentTypes: String = "",
    val addedByAgentId: Long? = null,
    val addedByAgentName: String = "",
    val preferredTransporterId: Long? = null,
    val preferredTransporterName: String = "",
    val transportPreference: String = "",
    val dob: String = "",
    val religion: String = "",
    val aadharPhotoUri: String = "",
    val gstCertPhotoUri: String = "",
    val panPhotoUri: String = "",
    val shopPhotoUri: String = "",
    val purchaserPhotoUri: String = "",
    val cancelChequePhotoUri: String = "",
    val preferredCategories: String = "",
    val referredBy: String = "",
    val defaultSalesmanId: Long? = null,
    val creditDays: Int = 30,
    val creditLimit: Double = 0.0,
    val notes: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val deletedBy: String = "",
    val deletedByEmail: String = "",
    val deletedByRole: String = "",
    val deletionStatus: String = "",
    val deletionReason: String = ""
) {
    val categories: String get() = preferredCategories
}

@IgnoreExtraProperties
@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: String = "",
    val name: String = "",
    val firmName: String = "",
    val type: String = "Manufacturer", // "Manufacturer" or "Wholesaler"
    val brand: String = "",
    val brandId: Long? = null,
    val gstin: String = "",
    val panNumber: String = "",
    val address: String = "",
    val officeAddress: String = "",
    val homeAddress: String = "",
    val officeLocation: String = "",
    val personalLocation: String = "",
    val shopCount: Int = 1,
    val shopLocations: String = "",
    val city: String = "Ahmedabad",
    val marketArea: String = "",
    val markets: String = "",
    val marketId: Long? = null,
    val marketName: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val phone2: String = "",
    val phone3: String = "",
    val phone4: String = "",
    val phone5: String = "",
    val email: String = "",
    val email2: String = "",
    val categories: String = "", // Categories of fabrics or garments provided
    val garmentTypes: String = "",
    val productsMade: String = "",
    val priceRange: String = "",
    val factoriesJson: String = "[]",
    val outletsJson: String = "[]",
    val shopPhotoUri: String = "",
    val visitingCardPhotoUri: String = "",
    val referredBy: String = "",
    val defaultCaseSize: Int = 24,
    val rating: Float = 4.5f,
    val notes: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val deletedBy: String = "",
    val deletedByEmail: String = "",
    val deletedByRole: String = "",
    val deletionStatus: String = "",
    val deletionReason: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val categoryList: List<String>
        get() = (if (categories.isNotBlank()) categories else garmentTypes).split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
}

@IgnoreExtraProperties
@Entity(tableName = "brands")
data class BrandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val brandName: String = "",
    val manufacturerId: Long? = null,
    val manufacturerName: String = "",
    val category: String = "",
    val logoPhotoUri: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Entity(tableName = "transporters")
data class TransporterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transporterName: String = "",
    val contactPerson: String = "",
    val phone1: String = "",
    val phone2: String = "",
    val phone3: String = "",
    val officeAddress: String = "",
    val godownAddress: String = "",
    val city: String = "Ahmedabad",
    val destinationsCovered: String = "",
    val gstin: String = "",
    val trackingUrl: String = "",
    val notes: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Entity(tableName = "markets")
data class MarketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val marketName: String = "",
    val city: String = "Ahmedabad",
    val area: String = "",
    val landmark: String = "",
    val pincode: String = "",
    val marketType: String = "Mixed",
    val description: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: String = "",
    val name: String = "",
    val phone: String = "",
    val phone2: String = "",
    val phone3: String = "",
    val phone4: String = "",
    val phone5: String = "",
    val role: String = "Salesman", // "Admin" or "Salesman"
    val email: String = "", // Google Account email for auth & role binding
    val alternateEmail: String = "",
    val address: String = "",
    val currentAddress: String = "",
    val permanentAddress: String = "",
    val personalLocation: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val referredBy: String = "",
    val assignedMarkets: String = "",
    val markets: String = "",
    val status: String = "Active", // "Active", "Suspended", "Deactivated"
    val isBlocked: Boolean = false,
    val blockedAt: Long? = null,
    val blockedReason: String = "",
    val reactivatedAt: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val deletedBy: String = "",
    val deletedByEmail: String = "",
    val deletedByRole: String = "",
    val deletionStatus: String = "",
    val deletionReason: String = ""
) {
    val email2: String get() = alternateEmail
}

@IgnoreExtraProperties
@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visitCode: String = "",
    val customerId: Long = 0,
    val customerName: String = "",
    val employeeId: Long = 0,
    val employeeName: String = "",
    val date: String = "",
    val notes: String = "",
    val status: String = "Active", // "Active", "Completed"
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val deletedBy: String = "",
    val deletedByEmail: String = "",
    val deletedByRole: String = "",
    val deletionStatus: String = "",
    val deletionReason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Entity(tableName = "purchase_entries")
data class PurchaseEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNo: String = "",
    val visitId: Long = 0,
    val supplierId: Long = 0,
    val supplierName: String = "",
    val supplierType: String = "", // "Manufacturer" or "Wholesaler"
    val itemCode: String = "", // e.g. "ABC", "XYZ", "Kurti 102"
    val pieces: Int = 0,
    val rate: Double = 0.0,
    val totalAmount: Double = 0.0,
    val caseSize: Int = 24,
    val caseCount: Int = 0,
    val loosePieces: Int = 0,
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
    val paymentStatus: String = "Pending", // "Pending", "Received", "Partial"
    val paymentMode: String = "Cash", // "Cash", "Online", "Cheque", "UPI"
    val paymentRemarks: String = "",
    val paidAmount: Double = 0.0,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val deletedBy: String = "",
    val deletedByEmail: String = "",
    val deletedByRole: String = "",
    val deletionStatus: String = "",
    val deletionReason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Entity(tableName = "pack_groups")
data class PackGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visitId: Long = 0,
    val packGroupCode: String = "",
    val linkedEntryIds: String = "", // comma-separated purchase_entry ids
    val combinedPieces: Int = 0,
    val resultingCases: Int = 0,
    val remainingLoose: Int = 0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productCode: String = "", // e.g. "DENIM-701", "COT-SHIRT", "TEE-OVERSZ"
    val name: String = "",
    val category: String = "Apparel", // "Denim", "Shirts", "Knitwear", "Hosiery", etc.
    val supplierId: Long = 0,
    val supplierName: String = "",
    val defaultRate: Double = 0.0,
    val defaultCaseSize: Int = 24,
    val hsnCode: String = "6203",
    val gstRate: Double = 5.0,
    val unit: String = "Pcs",
    val description: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val deletedBy: String = "",
    val deletedByEmail: String = "",
    val deletedByRole: String = "",
    val deletionStatus: String = "",
    val deletionReason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionNumber: String = "", // e.g. "TXN-2601"
    val orderNo: String = "", // e.g. "HT-2601"
    val visitId: Long = 0,
    val customerId: Long = 0,
    val customerName: String = "",
    val supplierId: Long = 0,
    val supplierName: String = "",
    val productId: Long? = null,
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
    val transactionType: String = "Purchase", // "Purchase", "Return", "Settlement"
    val paymentStatus: String = "Pending", // "Pending", "Partial", "Paid"
    val paymentMode: String = "Cash", // "Cash", "Online", "Cheque", "UPI"
    val paidAmount: Double = 0.0,
    val paymentRemarks: String = "",
    val deliveryStatus: String = "Pending", // "Pending", "Packed", "Dispatched", "Delivered"
    val transporter: String = "",
    val mixedPackNote: String? = null,
    val transactionDate: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Entity(tableName = "garment_items")
data class GarmentItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemCode: String = "", // e.g. "KURTI-101", "DENIM-701", "COT-SHIRT-M"
    val name: String = "",
    val category: String = "Apparel", // "Kurtis", "Shirts", "Jeans", "T-Shirts", "Fabrics", "Ethnic Wear"
    val supplierId: Long = 0,
    val supplierName: String = "",
    val defaultRate: Double = 0.0,
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

@IgnoreExtraProperties
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
typealias Brand = BrandEntity
typealias Transporter = TransporterEntity
typealias Market = MarketEntity

