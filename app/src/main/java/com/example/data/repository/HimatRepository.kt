package com.example.data.repository

import com.example.data.local.AppDatabase
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
import com.example.util.RecordValidationException
import com.example.util.RecordValidator
import com.example.util.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class HimatRepository(private val database: AppDatabase) {
    private val customerDao = database.customerDao()
    private val supplierDao = database.supplierDao()
    private val productDao = database.productDao()
    private val garmentItemDao = database.garmentItemDao()
    private val transactionDao = database.transactionDao()
    private val transactionLogDao = database.transactionLogDao()
    private val employeeDao = database.employeeDao()
    private val visitDao = database.visitDao()
    private val purchaseEntryDao = database.purchaseEntryDao()
    private val packGroupDao = database.packGroupDao()

    // Customers
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    suspend fun getCustomerById(id: Long) = customerDao.getCustomerById(id)
    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            customerDao.insertCustomer(customer)
        } else {
            customerDao.updateCustomer(customer)
            customer.id
        }
    }
    suspend fun deleteCustomer(customer: CustomerEntity) = customerDao.deleteCustomer(customer)

    // Suppliers
    val allSuppliers: Flow<List<SupplierEntity>> = supplierDao.getAllSuppliers()
    fun getSuppliersByType(type: String): Flow<List<SupplierEntity>> = supplierDao.getSuppliersByType(type)
    fun getSuppliersByCategory(category: String): Flow<List<SupplierEntity>> = supplierDao.getSuppliersByCategory(category)
    fun searchSuppliers(query: String): Flow<List<SupplierEntity>> = supplierDao.searchSuppliers(query)
    suspend fun getSupplierById(id: Long) = supplierDao.getSupplierById(id)
    fun getSupplierFlowById(id: Long): Flow<SupplierEntity?> = supplierDao.getSupplierFlowById(id)
    fun validateSupplier(supplier: SupplierEntity): ValidationResult = RecordValidator.validateSupplier(supplier)

    suspend fun saveSupplier(supplier: SupplierEntity): Long {
        val validation = validateSupplier(supplier)
        if (validation is ValidationResult.Invalid) {
            throw RecordValidationException(validation.errors)
        }
        return if (supplier.id == 0L) {
            supplierDao.insertSupplier(supplier)
        } else {
            supplierDao.updateSupplier(supplier)
            supplier.id
        }
    }
    suspend fun deleteSupplier(supplier: SupplierEntity) = supplierDao.deleteSupplier(supplier)
    suspend fun deleteSupplierById(id: Long) = supplierDao.deleteSupplierById(id)

    // Products
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    fun getProductsBySupplier(supplierId: Long): Flow<List<ProductEntity>> = productDao.getProductsBySupplier(supplierId)
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> = productDao.getProductsByCategory(category)
    suspend fun getProductById(id: Long) = productDao.getProductById(id)
    suspend fun getProductByCode(code: String) = productDao.getProductByCode(code)

    fun validateProduct(product: ProductEntity): ValidationResult = RecordValidator.validateProduct(product)

    suspend fun saveProduct(product: ProductEntity): Long {
        val validation = validateProduct(product)
        if (validation is ValidationResult.Invalid) {
            throw RecordValidationException(validation.errors)
        }
        return if (product.id == 0L) {
            productDao.insertProduct(product)
        } else {
            productDao.updateProduct(product)
            product.id
        }
    }
    suspend fun deleteProduct(product: ProductEntity) = productDao.deleteProduct(product)

    // Garment Items
    val allGarmentItems: Flow<List<GarmentItemEntity>> = garmentItemDao.getAllGarmentItems()
    fun getGarmentItemsBySupplier(supplierId: Long): Flow<List<GarmentItemEntity>> = garmentItemDao.getGarmentItemsBySupplier(supplierId)
    fun getGarmentItemsByCategory(category: String): Flow<List<GarmentItemEntity>> = garmentItemDao.getGarmentItemsByCategory(category)
    fun searchGarmentItems(query: String): Flow<List<GarmentItemEntity>> = garmentItemDao.searchGarmentItems(query)
    suspend fun getGarmentItemById(id: Long) = garmentItemDao.getGarmentItemById(id)
    suspend fun getGarmentItemByCode(code: String) = garmentItemDao.getGarmentItemByCode(code)

    fun validateGarmentItem(item: GarmentItemEntity): ValidationResult = RecordValidator.validateGarmentItem(item)

    suspend fun saveGarmentItem(item: GarmentItemEntity): Long {
        val validation = validateGarmentItem(item)
        if (validation is ValidationResult.Invalid) {
            throw RecordValidationException(validation.errors)
        }
        return if (item.id == 0L) {
            garmentItemDao.insertGarmentItem(item)
        } else {
            garmentItemDao.updateGarmentItem(item)
            item.id
        }
    }
    suspend fun deleteGarmentItem(item: GarmentItemEntity) = garmentItemDao.deleteGarmentItem(item)
    suspend fun deleteGarmentItemById(id: Long) = garmentItemDao.deleteGarmentItemById(id)

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    fun getTransactionsByVisit(visitId: Long): Flow<List<TransactionEntity>> = transactionDao.getTransactionsByVisit(visitId)
    fun getTransactionsByCustomer(customerId: Long): Flow<List<TransactionEntity>> = transactionDao.getTransactionsByCustomer(customerId)
    fun getTransactionsBySupplier(supplierId: Long): Flow<List<TransactionEntity>> = transactionDao.getTransactionsBySupplier(supplierId)
    fun getTransactionsByDeliveryStatus(status: String): Flow<List<TransactionEntity>> = transactionDao.getTransactionsByDeliveryStatus(status)
    suspend fun getTransactionById(id: Long) = transactionDao.getTransactionById(id)
    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        return if (transaction.id == 0L) {
            transactionDao.insertTransaction(transaction)
        } else {
            transactionDao.updateTransaction(transaction)
            transaction.id
        }
    }
    suspend fun updateTransactionDeliveryStatus(id: Long, status: String, transporter: String) {
        transactionDao.updateDeliveryStatus(id, status, transporter)
        val txn = transactionDao.getTransactionById(id)
        if (txn != null && txn.orderNo.isNotBlank()) {
            purchaseEntryDao.updateDeliveryStatusByOrderNo(txn.orderNo, status, transporter)
        }
    }
    suspend fun updateTransactionPaymentStatus(id: Long, status: String) =
        transactionDao.updatePaymentStatus(id, status)
    suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.deleteTransaction(transaction)

    // Transaction Logs
    val allTransactionLogs: Flow<List<TransactionLogEntity>> = transactionLogDao.getAllLogs()
    fun getTransactionLogsBySupplier(supplierId: Long): Flow<List<TransactionLogEntity>> = transactionLogDao.getLogsBySupplier(supplierId)
    fun getTransactionLogsByCustomer(customerId: Long): Flow<List<TransactionLogEntity>> = transactionLogDao.getLogsByCustomer(customerId)
    fun getTransactionLogsByVisit(visitId: Long): Flow<List<TransactionLogEntity>> = transactionLogDao.getLogsByVisit(visitId)
    fun getTransactionLogsByOrderNo(orderNo: String): Flow<List<TransactionLogEntity>> = transactionLogDao.getLogsByOrderNo(orderNo)
    fun getTransactionLogsByType(type: String): Flow<List<TransactionLogEntity>> = transactionLogDao.getLogsByType(type)
    fun getTransactionLogsByDeliveryStatus(status: String): Flow<List<TransactionLogEntity>> = transactionLogDao.getLogsByDeliveryStatus(status)
    fun getTransactionLogsByPaymentStatus(status: String): Flow<List<TransactionLogEntity>> = transactionLogDao.getLogsByPaymentStatus(status)
    fun searchTransactionLogs(query: String): Flow<List<TransactionLogEntity>> = transactionLogDao.searchLogs(query)
    suspend fun getTransactionLogById(id: Long) = transactionLogDao.getLogById(id)
    suspend fun getTransactionLogByNumber(logNumber: String) = transactionLogDao.getLogByNumber(logNumber)
    suspend fun saveTransactionLog(log: TransactionLogEntity): Long {
        return if (log.id == 0L) {
            transactionLogDao.insertLog(log)
        } else {
            transactionLogDao.updateLog(log)
            log.id
        }
    }
    suspend fun updateTransactionLogDeliveryStatus(id: Long, status: String, transporter: String) =
        transactionLogDao.updateDeliveryStatus(id, status, transporter)
    suspend fun updateTransactionLogPaymentStatus(id: Long, status: String) =
        transactionLogDao.updatePaymentStatus(id, status)
    suspend fun deleteTransactionLog(log: TransactionLogEntity) = transactionLogDao.deleteLog(log)
    suspend fun deleteTransactionLogById(id: Long) = transactionLogDao.deleteLogById(id)

    // Employees
    val allEmployees: Flow<List<EmployeeEntity>> = employeeDao.getAllEmployees()
    suspend fun getEmployeeById(id: Long) = employeeDao.getEmployeeById(id)
    suspend fun saveEmployee(employee: EmployeeEntity): Long {
        return if (employee.id == 0L) {
            employeeDao.insertEmployee(employee)
        } else {
            employeeDao.updateEmployee(employee)
            employee.id
        }
    }
    suspend fun deleteEmployee(employee: EmployeeEntity) = employeeDao.deleteEmployee(employee)

    // Visits
    val allVisits: Flow<List<VisitEntity>> = visitDao.getAllVisits()
    fun getVisitsByEmployee(employeeId: Long): Flow<List<VisitEntity>> = visitDao.getVisitsByEmployee(employeeId)
    suspend fun getVisitById(id: Long) = visitDao.getVisitById(id)
    suspend fun saveVisit(visit: VisitEntity): Long {
        return if (visit.id == 0L) {
            visitDao.insertVisit(visit)
        } else {
            visitDao.updateVisit(visit)
            visit.id
        }
    }
    suspend fun deleteVisit(visit: VisitEntity) = visitDao.deleteVisit(visit)

    // Purchase Entries
    val allEntries: Flow<List<PurchaseEntryEntity>> = purchaseEntryDao.getAllEntries()
    fun getEntriesByVisit(visitId: Long): Flow<List<PurchaseEntryEntity>> = purchaseEntryDao.getEntriesByVisit(visitId)
    fun getEntriesByVisitAndSupplier(visitId: Long, supplierId: Long): Flow<List<PurchaseEntryEntity>> =
        purchaseEntryDao.getEntriesByVisitAndSupplier(visitId, supplierId)
    fun getIncompleteEntriesByVisit(visitId: Long): Flow<List<PurchaseEntryEntity>> =
        purchaseEntryDao.getIncompleteEntriesByVisit(visitId)
    fun getEntriesByDeliveryStatus(status: String): Flow<List<PurchaseEntryEntity>> =
        purchaseEntryDao.getEntriesByDeliveryStatus(status)
    val distinctItemCodes: Flow<List<String>> = purchaseEntryDao.getDistinctItemCodes()

    suspend fun getNextOrderNumber(): String {
        val count = purchaseEntryDao.getEntriesCount()
        return "HT-${2600 + count + 1}"
    }

    suspend fun savePurchaseEntry(entry: PurchaseEntryEntity): Long {
        val totalAmount = entry.pieces * entry.rate
        val gstAmount = (totalAmount * entry.gstRate) / 100.0
        val grandTotal = totalAmount + gstAmount

        // Auto Case / Loose calculation
        val caseSize = if (entry.caseSize > 0) entry.caseSize else 24
        val caseCount = entry.pieces / caseSize
        val loosePieces = entry.pieces % caseSize

        val processedEntry = entry.copy(
            totalAmount = totalAmount,
            gstAmount = gstAmount,
            grandTotalWithGst = grandTotal,
            caseSize = caseSize,
            caseCount = caseCount,
            loosePieces = loosePieces
        )

        return if (processedEntry.id == 0L) {
            purchaseEntryDao.insertEntry(processedEntry)
        } else {
            purchaseEntryDao.updateEntry(processedEntry)
            processedEntry.id
        }
    }

    suspend fun updateDeliveryStatus(id: Long, status: String, transporter: String) {
        purchaseEntryDao.updateDeliveryStatus(id, status, transporter)
        val entry = purchaseEntryDao.getEntryById(id)
        if (entry != null && entry.orderNo.isNotBlank()) {
            transactionDao.updateDeliveryStatusByOrderNo(entry.orderNo, status, transporter)
        }
    }

    suspend fun deletePurchaseEntry(entry: PurchaseEntryEntity) = purchaseEntryDao.deleteEntry(entry)

    // Mixed Case Packing
    fun getPackGroupsByVisit(visitId: Long): Flow<List<PackGroupEntity>> = packGroupDao.getPackGroupsByVisit(visitId)

    suspend fun createMixedPackGroup(
        visitId: Long,
        selectedEntries: List<PurchaseEntryEntity>,
        targetCaseSize: Int,
        customNote: String? = null
    ): Long {
        if (selectedEntries.isEmpty()) return 0L

        val totalCombinedLoose = selectedEntries.sumOf { it.loosePieces }
        val resultingCases = if (targetCaseSize > 0) totalCombinedLoose / targetCaseSize else 1
        val remainingLoose = if (targetCaseSize > 0) totalCombinedLoose % targetCaseSize else 0

        val linkedIdsString = selectedEntries.joinToString(",") { it.id.toString() }
        val summaryNote = customNote ?: buildString {
            append("Mixed Case: ")
            selectedEntries.forEachIndexed { index, entry ->
                if (index > 0) append(" + ")
                append("${entry.loosePieces} pcs ${entry.itemCode} (${entry.supplierName})")
            }
            if (remainingLoose > 0) {
                append(" [${remainingLoose} loose pcs remaining]")
            }
        }

        val packGroup = PackGroupEntity(
            visitId = visitId,
            packGroupCode = "MIX-${System.currentTimeMillis() % 10000}",
            linkedEntryIds = linkedIdsString,
            combinedPieces = totalCombinedLoose,
            resultingCases = maxOf(1, resultingCases),
            remainingLoose = remainingLoose,
            note = summaryNote
        )

        val packGroupId = packGroupDao.insertPackGroup(packGroup)

        // Update each entry so the note appears on customer report AND each supplier's copy!
        selectedEntries.forEach { entry ->
            val otherEntries = selectedEntries.filter { it.id != entry.id }
            val itemSpecificNote = if (otherEntries.isEmpty()) {
                "Packed in ${packGroup.packGroupCode}: ${entry.loosePieces} pcs"
            } else {
                val othersDesc = otherEntries.joinToString(", ") { "${it.loosePieces} pcs ${it.itemCode} (${it.supplierName})" }
                "Mixed Packing: ${entry.loosePieces} pcs packed with $othersDesc"
            }
            purchaseEntryDao.updateMixedPackInfo(entry.id, packGroupId, itemSpecificNote)
        }

        return packGroupId
    }

    suspend fun deletePackGroup(packGroup: PackGroupEntity) {
        val entryIds = packGroup.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() }
        entryIds.forEach { id ->
            purchaseEntryDao.updateMixedPackInfo(id, null, null)
        }
        packGroupDao.deletePackGroup(packGroup)
    }

    suspend fun ensureInitialDataLoaded() {
        val count = purchaseEntryDao.getEntriesCount()
        val prodCount = productDao.getProductsCount()
        val txnCount = transactionDao.getTransactionsCount()
        val garmentCount = garmentItemDao.getGarmentItemsCount()
        val logCount = transactionLogDao.getLogsCount()
        if (count == 0 || prodCount == 0 || txnCount == 0 || garmentCount == 0 || logCount == 0) {
            AppDatabase.populateDatabase(database)
        }
    }
}
