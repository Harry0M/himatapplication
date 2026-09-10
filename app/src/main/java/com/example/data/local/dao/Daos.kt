package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.GarmentItemEntity
import com.example.data.local.entity.PackGroupEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionLogEntity
import com.example.data.local.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE type = :type ORDER BY name ASC")
    fun getSuppliersByType(type: String): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE categories LIKE '%' || :category || '%' ORDER BY name ASC")
    fun getSuppliersByCategory(category: String): Flow<List<SupplierEntity>>

    @Query("""
        SELECT * FROM suppliers 
        WHERE name LIKE '%' || :query || '%' 
           OR brand LIKE '%' || :query || '%' 
           OR contactPerson LIKE '%' || :query || '%' 
           OR phone LIKE '%' || :query || '%' 
           OR email LIKE '%' || :query || '%' 
           OR categories LIKE '%' || :query || '%' 
           OR marketArea LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchSuppliers(query: String): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    fun getSupplierFlowById(id: Long): Flow<SupplierEntity?>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Query("SELECT COUNT(*) FROM suppliers")
    suspend fun getSuppliersCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliers: List<SupplierEntity>)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteSupplierById(id: Long)
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<EmployeeEntity>)

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Delete
    suspend fun deleteEmployee(employee: EmployeeEntity)
}

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY id DESC")
    fun getAllVisits(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE employeeId = :employeeId ORDER BY id DESC")
    fun getVisitsByEmployee(employeeId: Long): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    suspend fun getVisitById(id: Long): VisitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(visits: List<VisitEntity>)

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    @Delete
    suspend fun deleteVisit(visit: VisitEntity)
}

@Dao
interface PurchaseEntryDao {
    @Query("SELECT * FROM purchase_entries ORDER BY id DESC")
    fun getAllEntries(): Flow<List<PurchaseEntryEntity>>

    @Query("SELECT * FROM purchase_entries WHERE visitId = :visitId ORDER BY id ASC")
    fun getEntriesByVisit(visitId: Long): Flow<List<PurchaseEntryEntity>>

    @Query("SELECT * FROM purchase_entries WHERE visitId = :visitId AND supplierId = :supplierId ORDER BY id ASC")
    fun getEntriesByVisitAndSupplier(visitId: Long, supplierId: Long): Flow<List<PurchaseEntryEntity>>

    @Query("SELECT * FROM purchase_entries WHERE visitId = :visitId AND loosePieces > 0 ORDER BY id ASC")
    fun getIncompleteEntriesByVisit(visitId: Long): Flow<List<PurchaseEntryEntity>>

    @Query("SELECT * FROM purchase_entries WHERE deliveryStatus = :status ORDER BY id DESC")
    fun getEntriesByDeliveryStatus(status: String): Flow<List<PurchaseEntryEntity>>

    @Query("SELECT DISTINCT itemCode FROM purchase_entries ORDER BY itemCode ASC")
    fun getDistinctItemCodes(): Flow<List<String>>

    @Query("SELECT * FROM purchase_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): PurchaseEntryEntity?

    @Query("SELECT COUNT(*) FROM purchase_entries")
    suspend fun getEntriesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PurchaseEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<PurchaseEntryEntity>)

    @Update
    suspend fun updateEntry(entry: PurchaseEntryEntity)

    @Query("UPDATE purchase_entries SET deliveryStatus = :status, transporter = :transporter WHERE id = :id")
    suspend fun updateDeliveryStatus(id: Long, status: String, transporter: String)

    @Query("UPDATE purchase_entries SET deliveryStatus = :status, transporter = :transporter WHERE orderNo = :orderNo")
    suspend fun updateDeliveryStatusByOrderNo(orderNo: String, status: String, transporter: String)

    @Query("UPDATE purchase_entries SET packGroupId = :packGroupId, mixedPackNote = :note WHERE id = :id")
    suspend fun updateMixedPackInfo(id: Long, packGroupId: Long?, note: String?)

    @Delete
    suspend fun deleteEntry(entry: PurchaseEntryEntity)
}

@Dao
interface PackGroupDao {
    @Query("SELECT * FROM pack_groups WHERE visitId = :visitId ORDER BY id DESC")
    fun getPackGroupsByVisit(visitId: Long): Flow<List<PackGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackGroup(group: PackGroupEntity): Long

    @Delete
    suspend fun deletePackGroup(group: PackGroupEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE supplierId = :supplierId ORDER BY name ASC")
    fun getProductsBySupplier(supplierId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE productCode = :code LIMIT 1")
    suspend fun getProductByCode(code: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE visitId = :visitId ORDER BY id ASC")
    fun getTransactionsByVisit(visitId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY id DESC")
    fun getTransactionsByCustomer(customerId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE supplierId = :supplierId ORDER BY id DESC")
    fun getTransactionsBySupplier(supplierId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deliveryStatus = :status ORDER BY id DESC")
    fun getTransactionsByDeliveryStatus(status: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET deliveryStatus = :status, transporter = :transporter WHERE id = :id")
    suspend fun updateDeliveryStatus(id: Long, status: String, transporter: String)

    @Query("UPDATE transactions SET deliveryStatus = :status, transporter = :transporter WHERE orderNo = :orderNo")
    suspend fun updateDeliveryStatusByOrderNo(orderNo: String, status: String, transporter: String)

    @Query("UPDATE transactions SET paymentStatus = :status WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, status: String)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
}

@Dao
interface GarmentItemDao {
    @Query("SELECT * FROM garment_items ORDER BY name ASC")
    fun getAllGarmentItems(): Flow<List<GarmentItemEntity>>

    @Query("SELECT * FROM garment_items WHERE supplierId = :supplierId ORDER BY name ASC")
    fun getGarmentItemsBySupplier(supplierId: Long): Flow<List<GarmentItemEntity>>

    @Query("SELECT * FROM garment_items WHERE category = :category ORDER BY name ASC")
    fun getGarmentItemsByCategory(category: String): Flow<List<GarmentItemEntity>>

    @Query("""
        SELECT * FROM garment_items 
        WHERE name LIKE '%' || :query || '%' 
           OR itemCode LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%' 
           OR fabricType LIKE '%' || :query || '%'
           OR supplierName LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchGarmentItems(query: String): Flow<List<GarmentItemEntity>>

    @Query("SELECT * FROM garment_items WHERE id = :id LIMIT 1")
    fun getGarmentItemFlowById(id: Long): Flow<GarmentItemEntity?>

    @Query("SELECT * FROM garment_items WHERE id = :id LIMIT 1")
    suspend fun getGarmentItemById(id: Long): GarmentItemEntity?

    @Query("SELECT * FROM garment_items WHERE itemCode = :code LIMIT 1")
    suspend fun getGarmentItemByCode(code: String): GarmentItemEntity?

    @Query("SELECT COUNT(*) FROM garment_items")
    suspend fun getGarmentItemsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGarmentItem(item: GarmentItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GarmentItemEntity>)

    @Update
    suspend fun updateGarmentItem(item: GarmentItemEntity)

    @Delete
    suspend fun deleteGarmentItem(item: GarmentItemEntity)

    @Query("DELETE FROM garment_items WHERE id = :id")
    suspend fun deleteGarmentItemById(id: Long)
}

@Dao
interface TransactionLogDao {
    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC, id DESC")
    fun getAllLogs(): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE supplierId = :supplierId ORDER BY timestamp DESC")
    fun getLogsBySupplier(supplierId: Long): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getLogsByCustomer(customerId: Long): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE visitId = :visitId ORDER BY timestamp ASC")
    fun getLogsByVisit(visitId: Long): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE orderNo = :orderNo ORDER BY timestamp DESC")
    fun getLogsByOrderNo(orderNo: String): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE transactionType = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: String): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE deliveryStatus = :status ORDER BY timestamp DESC")
    fun getLogsByDeliveryStatus(status: String): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE paymentStatus = :status ORDER BY timestamp DESC")
    fun getLogsByPaymentStatus(status: String): Flow<List<TransactionLogEntity>>

    @Query("""
        SELECT * FROM transaction_logs 
        WHERE logNumber LIKE '%' || :query || '%' 
           OR orderNo LIKE '%' || :query || '%' 
           OR customerName LIKE '%' || :query || '%' 
           OR supplierName LIKE '%' || :query || '%'
           OR itemCode LIKE '%' || :query || '%'
           OR transporter LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchLogs(query: String): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): TransactionLogEntity?

    @Query("SELECT * FROM transaction_logs WHERE logNumber = :logNumber LIMIT 1")
    suspend fun getLogByNumber(logNumber: String): TransactionLogEntity?

    @Query("SELECT COUNT(*) FROM transaction_logs")
    suspend fun getLogsCount(): Int

    @Query("SELECT SUM(pieces) FROM transaction_logs")
    fun getTotalPieces(): Flow<Int?>

    @Query("SELECT SUM(grandTotalWithGst) FROM transaction_logs")
    fun getTotalAmount(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TransactionLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<TransactionLogEntity>)

    @Update
    suspend fun updateLog(log: TransactionLogEntity)

    @Query("UPDATE transaction_logs SET deliveryStatus = :status, transporter = :transporter WHERE id = :id")
    suspend fun updateDeliveryStatus(id: Long, status: String, transporter: String)

    @Query("UPDATE transaction_logs SET paymentStatus = :status WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, status: String)

    @Delete
    suspend fun deleteLog(log: TransactionLogEntity)

    @Query("DELETE FROM transaction_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)
}

