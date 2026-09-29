package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.ChequePdcEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.LeadEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.GarmentItemEntity
import com.example.data.local.entity.MarketEntity
import com.example.data.local.entity.PackGroupEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionLogEntity
import com.example.data.local.entity.TransporterEntity
import com.example.data.local.entity.VisitEntity
import com.example.data.remote.FirebaseRtdbService
import com.example.util.DuplicateScan
import com.example.util.IdGenerator
import com.example.util.RecordValidationException
import com.example.util.RecordValidator
import com.example.util.ValidationResult
import com.example.util.markedDuplicate
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
    private val brandDao = database.brandDao()
    private val transporterDao = database.transporterDao()
    private val marketDao = database.marketDao()
    private val leadDao = database.leadDao()
    private val chequePdcDao = database.chequePdcDao()

    // Cheques PDC
    val allChequesPdc: Flow<List<ChequePdcEntity>> = chequePdcDao.getAllCheques()
    fun getChequesByPartyType(partyType: String): Flow<List<ChequePdcEntity>> = chequePdcDao.getChequesByPartyType(partyType)
    fun getChequesByCustomer(customerId: Long): Flow<List<ChequePdcEntity>> = chequePdcDao.getChequesByCustomer(customerId)
    fun getChequesBySupplier(supplierId: Long): Flow<List<ChequePdcEntity>> = chequePdcDao.getChequesBySupplier(supplierId)
    fun getChequesByDate(date: String): Flow<List<ChequePdcEntity>> = chequePdcDao.getChequesByDate(date)
    fun getChequesByStatus(status: String): Flow<List<ChequePdcEntity>> = chequePdcDao.getChequesByStatus(status)
    suspend fun getChequeById(id: Long): ChequePdcEntity? = chequePdcDao.getChequeById(id)
    suspend fun saveChequePdc(cheque: ChequePdcEntity): Long {
        return if (cheque.id == 0L) {
            chequePdcDao.insertCheque(cheque.copy(id = IdGenerator.newId()))
        } else {
            chequePdcDao.insertCheque(cheque)
            cheque.id
        }
    }
    suspend fun updateChequePdcStatus(id: Long, status: String, clearedDate: String = "") {
        chequePdcDao.updateStatus(id, status, clearedDate)
    }
    suspend fun deleteChequePdc(cheque: ChequePdcEntity) = chequePdcDao.deleteCheque(cheque)
    suspend fun deleteChequePdcById(id: Long) = chequePdcDao.deleteChequeById(id)

    // Leads
    val allLeads: Flow<List<LeadEntity>> = leadDao.getAllLeads()
    fun getLeadsByType(type: String): Flow<List<LeadEntity>> = leadDao.getLeadsByType(type)
    suspend fun getLeadById(id: Long): LeadEntity? = leadDao.getLeadById(id)
    suspend fun getLeadByLeadId(leadId: String): LeadEntity? = leadDao.getLeadByLeadId(leadId)
    suspend fun saveLead(lead: LeadEntity): Long {
        return if (lead.id == 0L) {
            leadDao.insertLead(lead)
        } else {
            leadDao.updateLead(lead)
            lead.id
        }
    }
    suspend fun deleteLead(lead: LeadEntity) = leadDao.deleteLead(lead)
    suspend fun deleteLeadById(id: Long) = leadDao.deleteLeadById(id)

    // Customers
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    suspend fun getCustomerById(id: Long) = customerDao.getCustomerById(id)
    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            customerDao.insertCustomer(customer.copy(id = IdGenerator.newId()))
        } else {
            customerDao.insertCustomer(customer)
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
            supplierDao.insertSupplier(supplier.copy(id = IdGenerator.newId()))
        } else {
            supplierDao.insertSupplier(supplier)
            supplier.id
        }
    }
    suspend fun deleteSupplier(supplier: SupplierEntity) = supplierDao.deleteSupplier(supplier)
    suspend fun deleteSupplierById(id: Long) = supplierDao.deleteSupplierById(id)

    // Brands
    val allBrands: Flow<List<BrandEntity>> = brandDao.getAllBrands()
    suspend fun getBrandById(id: Long) = brandDao.getBrandById(id)
    suspend fun saveBrand(brand: BrandEntity): Long {
        return if (brand.id == 0L) {
            brandDao.insertBrand(brand.copy(id = IdGenerator.newId()))
        } else {
            brandDao.insertBrand(brand)
            brand.id
        }
    }
    suspend fun deleteBrand(brand: BrandEntity) = brandDao.deleteBrand(brand)

    // Transporters
    val allTransporters: Flow<List<TransporterEntity>> = transporterDao.getAllTransporters()
    suspend fun getTransporterById(id: Long) = transporterDao.getTransporterById(id)
    suspend fun saveTransporter(transporter: TransporterEntity): Long {
        return if (transporter.id == 0L) {
            transporterDao.insertTransporter(transporter.copy(id = IdGenerator.newId()))
        } else {
            transporterDao.insertTransporter(transporter)
            transporter.id
        }
    }
    suspend fun deleteTransporter(transporter: TransporterEntity) = transporterDao.deleteTransporter(transporter)

    // Markets
    val allMarkets: Flow<List<MarketEntity>> = marketDao.getAllMarkets()
    suspend fun getMarketById(id: Long) = marketDao.getMarketById(id)
    suspend fun saveMarket(market: MarketEntity): Long {
        return if (market.id == 0L) {
            marketDao.insertMarket(market.copy(id = IdGenerator.newId()))
        } else {
            marketDao.insertMarket(market)
            market.id
        }
    }

    suspend fun deleteMarket(market: MarketEntity) {
        marketDao.deleteMarketById(market.id)
        marketDao.deleteMarket(market)
    }


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
            productDao.insertProduct(product.copy(id = IdGenerator.newId()))
        } else {
            productDao.insertProduct(product)
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
            transactionDao.insertTransaction(transaction.copy(id = IdGenerator.newId()))
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

    /** Deactivated staff included. Only for handing out a staff code that is not already taken. */
    val everyStaffRecord: Flow<List<EmployeeEntity>> = employeeDao.getAllEmployeesIncludingRemoved()

    suspend fun getEmployeeById(id: Long) = employeeDao.getEmployeeById(id)
    suspend fun saveEmployee(employee: EmployeeEntity): Long {
        return if (employee.id == 0L) {
            employeeDao.insertEmployee(employee.copy(id = IdGenerator.newId()))
        } else {
            employeeDao.insertEmployee(employee)
            employee.id
        }
    }
    suspend fun deleteEmployee(employee: EmployeeEntity) = employeeDao.deleteEmployee(employee)

    // Visits
    val allVisits: Flow<List<VisitEntity>> = visitDao.getAllVisits()
    fun getVisitsByEmployee(employeeId: Long): Flow<List<VisitEntity>> = visitDao.getVisitsByEmployee(employeeId)
    suspend fun getVisitById(id: Long) = visitDao.getVisitById(id)
    fun getVisitFlowById(id: Long): Flow<VisitEntity?> = visitDao.getVisitFlowById(id)
    suspend fun saveVisit(visit: VisitEntity): Long {
        return if (visit.id == 0L) {
            visitDao.insertVisit(visit.copy(id = IdGenerator.newId()))
        } else {
            visitDao.insertVisit(visit)
            visit.id
        }
    }
    suspend fun deleteVisit(visit: VisitEntity) {
        visitDao.deleteVisit(visit)
        purchaseEntryDao.deleteEntriesByVisit(visit.id)
        packGroupDao.deletePackGroupsByVisit(visit.id)
    }

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
    suspend fun getEntryById(id: Long) = purchaseEntryDao.getEntryById(id)
    suspend fun getTransactionByOrderNo(orderNo: String) = transactionDao.getTransactionByOrderNo(orderNo)

    /** Highest HT-<n> order sequence known on this device (never below 2600). */
    suspend fun localMaxOrderSequence(): Int {
        val allEntries = purchaseEntryDao.getAllEntries().first()
        val allTxns = transactionDao.getAllTransactions().first()
        val maxEntryNum = allEntries.mapNotNull { it.orderNo.trim().removePrefix("HT-").toIntOrNull() }.maxOrNull() ?: 2600
        val maxTxnNum = allTxns.mapNotNull { it.orderNo.trim().removePrefix("HT-").toIntOrNull() }.maxOrNull() ?: 2600
        return maxOf(2600, maxOf(maxEntryNum, maxTxnNum))
    }

    /** Local-only fallback; prefer FirebaseRtdbService.allocateOrderSequence for multi-phone safety. */
    suspend fun getNextOrderNumber(): String = "HT-${localMaxOrderSequence() + 1}"

    suspend fun savePurchaseEntry(entry: PurchaseEntryEntity): Long {
        val totalAmount = entry.pieces * entry.rate
        val gstAmount = (totalAmount * entry.gstRate) / 100.0
        val grandTotal = totalAmount + gstAmount

        // Respect user-specified Case / Loose values directly as-is
        val caseSize = if (entry.caseSize > 0) entry.caseSize else 24
        val caseCount = entry.caseCount
        val loosePieces = entry.loosePieces

        val processedEntry = entry.copy(
            totalAmount = totalAmount,
            gstAmount = gstAmount,
            grandTotalWithGst = grandTotal,
            caseSize = caseSize,
            caseCount = caseCount,
            loosePieces = loosePieces
        )

        return if (processedEntry.id == 0L) {
            purchaseEntryDao.insertEntry(processedEntry.copy(id = IdGenerator.newId()))
        } else {
            // Upsert (REPLACE): works for edits and for new rows that already carry an id
            purchaseEntryDao.insertEntry(processedEntry)
            processedEntry.id
        }
    }

    suspend fun updateLrDetails(id: Long, lrNo: String, lrDate: String) =
        purchaseEntryDao.updateLrDetails(id, lrNo, lrDate)

    suspend fun updateDeliveryStatus(id: Long, status: String, transporter: String) {
        purchaseEntryDao.updateDeliveryStatus(id, status, transporter)
        val entry = purchaseEntryDao.getEntryById(id)
        if (entry != null && entry.orderNo.isNotBlank()) {
            transactionDao.updateDeliveryStatusByOrderNo(entry.orderNo, status, transporter)
        }
    }

    suspend fun updatePurchaseEntryDetails(
        entry: PurchaseEntryEntity
    ): PurchaseEntryEntity {
        val totalAmount = entry.pieces * entry.rate
        val gstAmount = (totalAmount * entry.gstRate) / 100.0
        val grandTotal = totalAmount + gstAmount

        val caseSize = if (entry.caseSize > 0) entry.caseSize else 24
        val caseCount = entry.caseCount
        val loosePieces = entry.loosePieces

        val processedEntry = entry.copy(
            totalAmount = totalAmount,
            gstAmount = gstAmount,
            grandTotalWithGst = grandTotal,
            caseSize = caseSize,
            caseCount = caseCount,
            loosePieces = loosePieces
        )

        purchaseEntryDao.updateEntry(processedEntry)

        if (processedEntry.orderNo.isNotBlank()) {
            val existingTxn = transactionDao.getTransactionByOrderNo(processedEntry.orderNo)
            if (existingTxn != null) {
                val updatedTxn = existingTxn.copy(
                    pieces = processedEntry.pieces,
                    rate = processedEntry.rate,
                    totalAmount = totalAmount,
                    gstAmount = gstAmount,
                    grandTotalWithGst = grandTotal,
                    caseSize = caseSize,
                    caseCount = caseCount,
                    loosePieces = loosePieces,
                    deliveryStatus = processedEntry.deliveryStatus,
                    transporter = processedEntry.transporter,
                    paymentStatus = processedEntry.paymentStatus,
                    paymentMode = processedEntry.paymentMode,
                    paidAmount = processedEntry.paidAmount,
                    paymentRemarks = processedEntry.paymentRemarks
                )
                transactionDao.updateTransaction(updatedTxn)
            }
        }
        return processedEntry
    }

    suspend fun updatePaymentInfo(
        id: Long,
        paymentStatus: String,
        paymentMode: String,
        paidAmount: Double,
        paymentRemarks: String
    ) {
        purchaseEntryDao.updatePaymentInfo(id, paymentStatus, paymentMode, paidAmount, paymentRemarks)
        val entry = purchaseEntryDao.getEntryById(id)
        if (entry != null && entry.orderNo.isNotBlank()) {
            transactionDao.updatePaymentStatusByOrderNo(entry.orderNo, paymentStatus, paymentMode, paidAmount, paymentRemarks)
        }
    }

    suspend fun deletePurchaseEntry(entry: PurchaseEntryEntity) = purchaseEntryDao.deleteEntry(entry)

    /**
     * Orders whose trip is not in this phone's database. Read-only on purpose.
     *
     * This used to delete them locally and then remove them from the cloud as well. Because the
     * trips table can legitimately be empty or half-filled for a moment (first sign-in, a fresh
     * install, or the trips snapshot arriving after the orders one), that classified perfectly good
     * orders as orphans and destroyed them for the whole company. Nothing is deleted here any more:
     * a missing trip is a sync gap, not a reason to throw away an order.
     */
    suspend fun findOrphanEntries(): List<PurchaseEntryEntity> {
        val validVisitIds = visitDao.getAllVisits().first().map { it.id }.toSet()
        if (validVisitIds.isEmpty()) return emptyList()
        return purchaseEntryDao.getAllEntries().first().filter { it.visitId !in validVisitIds }
    }

    // Mixed Case Packing
    val allPackGroups: Flow<List<PackGroupEntity>> = packGroupDao.getAllPackGroups()
    fun getPackGroupsByVisit(visitId: Long): Flow<List<PackGroupEntity>> = packGroupDao.getPackGroupsByVisit(visitId)
    suspend fun getPackGroupById(id: Long) = packGroupDao.getPackGroupById(id)

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
            id = IdGenerator.newId(),
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
            // By id only: two different orders can legitimately share an order number from older data
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

    // -------------------------------------------------------------------------
    // Cloud -> local ingestion
    //
    // These used to treat the cloud snapshot as absolute truth and hard-delete every local row the
    // snapshot did not contain. That is how trips and orders "disappeared by themselves": a write
    // that never reached the server (offline, or rejected) left the row only on the phone, and the
    // very next snapshot deleted it for good.
    //
    // Now nothing is ever hard-deleted here. The cloud copy is merged in, real deletions travel as
    // the isDeleted flag (which the lists already hide), and a row still waiting to be uploaded is
    // left completely alone. Cleaning up a genuinely removed record is an explicit admin action.
    // -------------------------------------------------------------------------

    /**
     * Staff from the cloud, including the ones that were deactivated.
     *
     * This is the one collection where a "deleted" flag from the office is applied locally, because
     * the login check reads these rows. Dropping deactivated records used to leave every phone with
     * its own row saying the person was still active, which is exactly how a removed staff member
     * kept getting in. The row is only hidden — the office can switch it back on.
     */
    suspend fun syncEmployeesFromCloud(employees: List<EmployeeEntity>) {
        val valid = employees.filter { it.id > 0L && it.name.isNotBlank() }.distinctBy { it.id }
        if (valid.isNotEmpty()) {
            employeeDao.insertAll(valid)
        }
    }

    suspend fun syncCustomersFromCloud(customers: List<CustomerEntity>) {
        val valid = customers.filter { it.id > 0L && it.name.isNotBlank() && !it.isDeleted }
            .distinctBy { it.id }
            .distinctBy { "${it.name.trim().lowercase()}_${it.phone.trim()}" }
        if (valid.isNotEmpty()) {
            customerDao.insertAll(valid)
        }
    }

    suspend fun syncSuppliersFromCloud(suppliers: List<SupplierEntity>) {
        val valid = suppliers.filter { it.id > 0L && it.name.isNotBlank() && !it.isDeleted }
            .distinctBy { it.id }
            .distinctBy { "${it.name.trim().lowercase()}_${it.brand.trim().lowercase()}_${it.phone.trim()}" }
        if (valid.isNotEmpty()) {
            supplierDao.insertAll(valid)
        }
    }

    suspend fun syncProductsFromCloud(products: List<ProductEntity>) {
        val valid = products.filter { it.id > 0L && it.name.isNotBlank() && !it.isDeleted }
            .distinctBy { it.id }
            .distinctBy {
                if (it.productCode.isNotBlank()) it.productCode.trim().lowercase()
                else "${it.name.trim().lowercase()}_${it.supplierName.trim().lowercase()}"
            }
        if (valid.isNotEmpty()) {
            productDao.insertAll(valid)
        }
    }

    /**
     * Trips from the cloud. A local trip that is still [VisitEntity.pendingPush] keeps its local
     * copy even if the cloud already has that id, so an unsent edit is not silently reverted.
     */
    suspend fun syncVisitsFromCloud(visits: List<VisitEntity>) {
        val valid = visits.filter { it.id > 0L && !it.isDeleted }.distinctBy { it.id }
        if (valid.isEmpty()) return
        val stillPending = visitDao.getPendingPushVisitIds().toSet()
        val toInsert = valid.filter { it.id !in stillPending }
        if (toInsert.isNotEmpty()) {
            visitDao.insertAll(toInsert)
        }
    }

    /**
     * Orders from the cloud. Same rule as trips: an order this phone has not managed to upload yet
     * is never overwritten and never deleted.
     */
    suspend fun syncEntriesFromCloud(entries: List<PurchaseEntryEntity>) {
        val valid = entries.filter { it.id > 0L && !it.isDeleted }.distinctBy { it.id }
        if (valid.isEmpty()) return
        val stillPending = purchaseEntryDao.getPendingPushEntryIds().toSet()
        val toInsert = valid.filter { it.id !in stillPending }
        if (toInsert.isNotEmpty()) {
            purchaseEntryDao.insertAll(toInsert)
        }
    }

    suspend fun syncTransactionsFromCloud(transactions: List<TransactionEntity>) {
        val valid = transactions.filter { it.id > 0L }.distinctBy { it.id }
        if (valid.isNotEmpty()) {
            transactionDao.insertAll(valid)
        }
    }

    suspend fun syncPackGroupsFromCloud(packGroups: List<PackGroupEntity>) {
        packGroups.filter { it.id > 0L }.distinctBy { it.id }.forEach {
            packGroupDao.insertPackGroup(it)
        }
    }

    suspend fun syncBrandsFromCloud(brands: List<BrandEntity>) {
        val valid = brands.filter { it.id > 0L && it.brandName.isNotBlank() && !it.isDeleted }.distinctBy { it.id }
        if (valid.isNotEmpty()) {
            brandDao.insertAll(valid)
        }
    }

    suspend fun syncTransportersFromCloud(transporters: List<TransporterEntity>) {
        val valid = transporters.filter { it.id > 0L && it.transporterName.isNotBlank() && !it.isDeleted }.distinctBy { it.id }
        if (valid.isNotEmpty()) {
            transporterDao.insertAll(valid)
        }
    }

    suspend fun syncMarketsFromCloud(markets: List<MarketEntity>) {
        val valid = markets.filter { it.id > 0L && it.marketName.isNotBlank() && !it.isDeleted }.distinctBy { it.id }
        if (valid.isNotEmpty()) {
            marketDao.insertAll(valid)
        }
    }

    suspend fun syncLeadsFromCloud(leads: List<LeadEntity>) {
        val valid = leads.filter { it.leadId.isNotBlank() && !it.isDeleted }.distinctBy { it.leadId }
        valid.forEach { cloudLead ->
            val existing = leadDao.getLeadByLeadId(cloudLead.leadId)
            if (existing != null) {
                leadDao.updateLead(cloudLead.copy(id = existing.id))
            } else {
                leadDao.insertLead(cloudLead)
            }
        }
    }

    suspend fun syncChequesFromCloud(cheques: List<ChequePdcEntity>) {
        val valid = cheques.filter { it.id > 0L && !it.isDeleted }.distinctBy { it.id }
        if (valid.isNotEmpty()) {
            chequePdcDao.insertAll(valid)
        }
    }

    // -------------------------------------------------------------------------
    // Local -> cloud recovery: anything still sitting on this phone
    // -------------------------------------------------------------------------

    /** Trips this phone has not managed to upload yet, oldest first. */
    suspend fun pendingPushVisits(): List<VisitEntity> = visitDao.getPendingPushVisits()

    /** Orders this phone has not managed to upload yet, oldest first. */
    suspend fun pendingPushEntries(): List<PurchaseEntryEntity> = purchaseEntryDao.getPendingPushEntries()

    suspend fun markVisitPushed(id: Long) = visitDao.setPendingPush(id, false)

    suspend fun markEntryPushed(id: Long) = purchaseEntryDao.setPendingPush(id, false)

    suspend fun markVisitPending(id: Long) = visitDao.setPendingPush(id, true)

    suspend fun markEntryPending(id: Long) = purchaseEntryDao.setPendingPush(id, true)

    /** How many trips + orders are still waiting to reach the office. */
    suspend fun pendingPushCount(): Int =
        visitDao.getPendingPushCount() + purchaseEntryDao.getPendingPushCount()


    /**
     * Collapses records that were saved twice. Runs after every sync.
     *
     * Two rules make this safe, because this routine used to be a silent data-loss path of its own:
     * it ran on every sync, including offline against a half-filled database, and hard-deleted the
     * loser from the cloud with no warning and no way back.
     *
     *  1. It never removes anything. The duplicate is *hidden* ([markedDuplicate]), so a wrong guess
     *     costs a tap to undo instead of a lost order. Deciding what really goes is an admin action.
     *  2. [allowCloudWrite] is only true for an admin. A staff phone tidies up its own screen; it
     *     does not get to rewrite the shared book based on whatever it happens to have synced.
     *
     * Which records count as the same thing lives in [DuplicateScan], where the rules are unit tested.
     */
    suspend fun deduplicateDatabase(rtdbService: FirebaseRtdbService, allowCloudWrite: Boolean = false) {
        try {
            val actor = "Automatic duplicate check"

            // 1. Customers: same owner name and same phone number
            DuplicateScan.customers(customerDao.getAllCustomers().first()).forEach { group ->
                group.duplicates.forEach { dup ->
                    // Trips must follow the copy that is kept, or they lose their customer
                    visitDao.repointCustomerId(dup.id, group.canonical.id)
                    val hidden = dup.markedDuplicate(group.canonical.id)
                    customerDao.insertCustomer(hidden)
                    if (allowCloudWrite) rtdbService.softDeleteCustomer(dup, actor, "", "Admin")
                }
            }

            // 2. Suppliers: same name, brand and phone
            DuplicateScan.suppliers(supplierDao.getAllSuppliers().first()).forEach { group ->
                group.duplicates.forEach { dup ->
                    purchaseEntryDao.repointSupplierId(dup.id, group.canonical.id)
                    transactionDao.repointSupplierId(dup.id, group.canonical.id)
                    val hidden = dup.markedDuplicate(group.canonical.id)
                    supplierDao.insertSupplier(hidden)
                    if (allowCloudWrite) rtdbService.softDeleteSupplier(dup, actor, "", "Admin")
                }
            }

            // 3. Orders: a true double-save, every business field identical
            DuplicateScan.orders(purchaseEntryDao.getAllEntries().first()).forEach { group ->
                group.duplicates.forEach { dup ->
                    val hidden = dup.markedDuplicate(group.canonical.id)
                    purchaseEntryDao.insertEntry(hidden)
                    if (allowCloudWrite) rtdbService.softDeletePurchaseEntry(dup, actor, "", "Admin")
                }
            }

            // 4. Products: same item code under the same supplier
            DuplicateScan.products(productDao.getAllProducts().first()).forEach { group ->
                group.duplicates.forEach { dup ->
                    val hidden = dup.markedDuplicate(group.canonical.id)
                    productDao.insertProduct(hidden)
                    if (allowCloudWrite) rtdbService.softDeleteProduct(dup, actor, "", "Admin")
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun isLocalDatabaseEmpty(): Boolean {
        val count = purchaseEntryDao.getEntriesCount()
        val prodCount = productDao.getProductsCount()
        val txnCount = transactionDao.getTransactionsCount()
        val garmentCount = garmentItemDao.getGarmentItemsCount()
        val logCount = transactionLogDao.getLogsCount()
        return (count == 0 && prodCount == 0 && txnCount == 0 && garmentCount == 0 && logCount == 0)
    }

    suspend fun ensureInitialDataLoaded(isCloudEmpty: Boolean = false) {
        if (!isCloudEmpty) {
            // Cloud has data! Do not load mock SampleData, let cloud sync populate real data.
            return
        }
        if (isLocalDatabaseEmpty()) {
            AppDatabase.populateDatabase(database)
        }
    }
}
