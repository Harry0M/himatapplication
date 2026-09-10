package com.example.ui.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.repository.HimatRepository
import com.example.util.PdfGenerator
import com.example.util.RecordValidator
import com.example.util.ShareUtil
import com.example.util.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    DASHBOARD,
    VISITS,
    VISIT_DETAIL,
    CUSTOMER_MASTER,
    SUPPLIER_MASTER,
    EMPLOYEE_MASTER,
    DELIVERIES,
    REPORTS,
    CUSTOMER_REPORT_VIEW,
    SUPPLIER_COPY_VIEW,
    CUSTOMER_DETAIL,
    SUPPLIER_DETAIL,
    EMPLOYEE_DETAIL,
    SUPPLIER_HUB
}

class HimatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = HimatRepository(database)

    // Current User Session
    private val _currentRole = MutableStateFlow("Admin") // "Admin" or "Salesman"
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    private val _currentEmployee = MutableStateFlow<EmployeeEntity?>(null)
    val currentEmployee: StateFlow<EmployeeEntity?> = _currentEmployee.asStateFlow()

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedVisit = MutableStateFlow<VisitEntity?>(null)
    val selectedVisit: StateFlow<VisitEntity?> = _selectedVisit.asStateFlow()

    private val _selectedSupplierForCopy = MutableStateFlow<SupplierEntity?>(null)
    val selectedSupplierForCopy: StateFlow<SupplierEntity?> = _selectedSupplierForCopy.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _selectedSupplier = MutableStateFlow<SupplierEntity?>(null)
    val selectedSupplier: StateFlow<SupplierEntity?> = _selectedSupplier.asStateFlow()

    private val _selectedEmployeeDetail = MutableStateFlow<EmployeeEntity?>(null)
    val selectedEmployeeDetail: StateFlow<EmployeeEntity?> = _selectedEmployeeDetail.asStateFlow()

    // Data streams from Repository
    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGarmentItems: StateFlow<List<GarmentItemEntity>> = repository.allGarmentItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactionLogs: StateFlow<List<TransactionLogEntity>> = repository.allTransactionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEmployees: StateFlow<List<EmployeeEntity>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVisits: StateFlow<List<VisitEntity>> = repository.allVisits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEntries: StateFlow<List<PurchaseEntryEntity>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctItemCodes: StateFlow<List<String>> = repository.distinctItemCodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered entries for selected visit
    private val _visitEntries = MutableStateFlow<List<PurchaseEntryEntity>>(emptyList())
    val visitEntries: StateFlow<List<PurchaseEntryEntity>> = _visitEntries.asStateFlow()

    // Pack groups for selected visit
    private val _visitPackGroups = MutableStateFlow<List<PackGroupEntity>>(emptyList())
    val visitPackGroups: StateFlow<List<PackGroupEntity>> = _visitPackGroups.asStateFlow()

    // Validation State
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    fun clearValidationError() {
        _validationError.value = null
    }

    fun validateSupplier(supplier: SupplierEntity): ValidationResult = RecordValidator.validateSupplier(supplier)
    fun validateGarmentItem(item: GarmentItemEntity): ValidationResult = RecordValidator.validateGarmentItem(item)
    fun validateProduct(product: ProductEntity): ValidationResult = RecordValidator.validateProduct(product)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureInitialDataLoaded()
        }
    }

    fun setRole(role: String, employee: EmployeeEntity? = null) {
        _currentRole.value = role
        _currentEmployee.value = employee
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openVisitDetail(visit: VisitEntity) {
        _selectedVisit.value = visit
        _currentScreen.value = AppScreen.VISIT_DETAIL
        observeVisitData(visit.id)
    }

    private fun observeVisitData(visitId: Long) {
        viewModelScope.launch {
            repository.getEntriesByVisit(visitId).collect { entries ->
                _visitEntries.value = entries
            }
        }
        viewModelScope.launch {
            repository.getPackGroupsByVisit(visitId).collect { groups ->
                _visitPackGroups.value = groups
            }
        }
    }

    fun openCustomerReport(visit: VisitEntity) {
        _selectedVisit.value = visit
        observeVisitData(visit.id)
        _currentScreen.value = AppScreen.CUSTOMER_REPORT_VIEW
    }

    fun openSupplierCopy(visit: VisitEntity, supplier: SupplierEntity) {
        _selectedVisit.value = visit
        _selectedSupplierForCopy.value = supplier
        observeVisitData(visit.id)
        _currentScreen.value = AppScreen.SUPPLIER_COPY_VIEW
    }

    fun openCustomerDetail(customer: CustomerEntity) {
        _selectedCustomer.value = customer
        _currentScreen.value = AppScreen.CUSTOMER_DETAIL
    }

    fun openSupplierDetail(supplier: SupplierEntity) {
        _selectedSupplier.value = supplier
        _currentScreen.value = AppScreen.SUPPLIER_DETAIL
    }

    fun openEmployeeDetail(employee: EmployeeEntity) {
        _selectedEmployeeDetail.value = employee
        _currentScreen.value = AppScreen.EMPLOYEE_DETAIL
    }

    fun advanceEntryDeliveryStatus(entry: PurchaseEntryEntity) {
        val nextStatus = when (entry.deliveryStatus.lowercase(Locale.getDefault())) {
            "pending" -> "Packed"
            "packed" -> "Dispatched"
            "dispatched" -> "Delivered"
            else -> "Delivered"
        }
        updateDeliveryStatus(entry, nextStatus, entry.transporter)
    }

    // Customer Operations
    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCustomer(customer)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomer(customer)
        }
    }

    // Supplier Operations
    fun saveSupplier(
        supplier: SupplierEntity,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val validation = repository.validateSupplier(supplier)
        if (validation is ValidationResult.Invalid) {
            val errorMsg = validation.errorMessage
            _validationError.value = errorMsg
            onError?.invoke(errorMsg)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveSupplier(supplier)
                launch(Dispatchers.Main) {
                    _validationError.value = null
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    val msg = e.message ?: "Failed to save supplier"
                    _validationError.value = msg
                    onError?.invoke(msg)
                }
            }
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSupplier(supplier)
        }
    }

    // Product Operations
    fun saveProduct(
        product: ProductEntity,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val validation = repository.validateProduct(product)
        if (validation is ValidationResult.Invalid) {
            val errorMsg = validation.errorMessage
            _validationError.value = errorMsg
            onError?.invoke(errorMsg)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveProduct(product)
                launch(Dispatchers.Main) {
                    _validationError.value = null
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    val msg = e.message ?: "Failed to save product"
                    _validationError.value = msg
                    onError?.invoke(msg)
                }
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProduct(product)
        }
    }

    // Garment Item Operations
    fun saveGarmentItem(
        item: GarmentItemEntity,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val validation = repository.validateGarmentItem(item)
        if (validation is ValidationResult.Invalid) {
            val errorMsg = validation.errorMessage
            _validationError.value = errorMsg
            onError?.invoke(errorMsg)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveGarmentItem(item)
                launch(Dispatchers.Main) {
                    _validationError.value = null
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    val msg = e.message ?: "Failed to save garment item"
                    _validationError.value = msg
                    onError?.invoke(msg)
                }
            }
        }
    }

    fun deleteGarmentItem(item: GarmentItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGarmentItem(item)
        }
    }

    // Transaction Operations
    fun saveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransactionDeliveryStatus(transactionId: Long, status: String, transporter: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTransactionDeliveryStatus(transactionId, status, transporter)
        }
    }

    fun updateTransactionPaymentStatus(transactionId: Long, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTransactionPaymentStatus(transactionId, status)
        }
    }

    // Employee Operations
    fun saveEmployee(employee: EmployeeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveEmployee(employee)
        }
    }

    fun deleteEmployee(employee: EmployeeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEmployee(employee)
        }
    }

    // Visit Operations
    fun createVisit(customer: CustomerEntity, employee: EmployeeEntity, notes: String, onCreated: (VisitEntity) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val visitCode = "VIS-${SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())}-${(10..99).random()}"
            val newVisit = VisitEntity(
                visitCode = visitCode,
                customerId = customer.id,
                customerName = customer.name,
                employeeId = employee.id,
                employeeName = employee.name,
                date = dateStr,
                notes = notes,
                status = "Active"
            )
            val id = repository.saveVisit(newVisit)
            val created = newVisit.copy(id = id)
            launch(Dispatchers.Main) {
                openVisitDetail(created)
                onCreated(created)
            }
        }
    }

    fun updateVisitStatus(visit: VisitEntity, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = visit.copy(status = newStatus)
            repository.saveVisit(updated)
            if (_selectedVisit.value?.id == visit.id) {
                _selectedVisit.value = updated
            }
        }
    }

    fun deleteVisit(visit: VisitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVisit(visit)
            launch(Dispatchers.Main) {
                if (_selectedVisit.value?.id == visit.id) {
                    _selectedVisit.value = null
                    _currentScreen.value = AppScreen.VISITS
                }
            }
        }
    }

    // Purchase Entry Operations
    fun savePurchaseEntry(
        orderNo: String?,
        visitId: Long,
        supplier: SupplierEntity,
        itemCode: String,
        pieces: Int,
        rate: Double,
        caseSize: Int,
        gstRate: Double,
        expectedDeliveryDate: String,
        transporter: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val seqOrderNo = orderNo ?: repository.getNextOrderNumber()
            val entry = PurchaseEntryEntity(
                orderNo = seqOrderNo,
                visitId = visitId,
                supplierId = supplier.id,
                supplierName = supplier.name,
                supplierType = supplier.type,
                itemCode = itemCode.trim().uppercase(Locale.getDefault()),
                pieces = pieces,
                rate = rate,
                totalAmount = pieces * rate,
                caseSize = caseSize,
                caseCount = pieces / caseSize,
                loosePieces = pieces % caseSize,
                gstRate = gstRate,
                expectedDeliveryDate = expectedDeliveryDate,
                transporter = transporter
            )
            repository.savePurchaseEntry(entry)

            val visit = repository.getVisitById(visitId)
            val txn = TransactionEntity(
                transactionNumber = "TXN-${seqOrderNo.replace("HT-", "")}",
                orderNo = seqOrderNo,
                visitId = visitId,
                customerId = visit?.customerId ?: 0L,
                customerName = visit?.customerName ?: "",
                supplierId = supplier.id,
                supplierName = supplier.name,
                itemCode = itemCode.trim().uppercase(Locale.getDefault()),
                pieces = pieces,
                rate = rate,
                totalAmount = pieces * rate,
                gstRate = gstRate,
                gstAmount = (pieces * rate * gstRate) / 100.0,
                grandTotalWithGst = (pieces * rate) + (pieces * rate * gstRate) / 100.0,
                caseSize = caseSize,
                caseCount = pieces / caseSize,
                loosePieces = pieces % caseSize,
                deliveryStatus = "Pending",
                transporter = transporter,
                transactionDate = visit?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
            repository.saveTransaction(txn)
        }
    }

    fun deletePurchaseEntry(entry: PurchaseEntryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePurchaseEntry(entry)
        }
    }

    fun updateDeliveryStatus(entry: PurchaseEntryEntity, newStatus: String, transporter: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDeliveryStatus(entry.id, newStatus, transporter)
        }
    }

    // Mixed Packing
    fun createMixedPack(
        visitId: Long,
        selectedEntries: List<PurchaseEntryEntity>,
        targetCaseSize: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createMixedPackGroup(
                visitId = visitId,
                selectedEntries = selectedEntries,
                targetCaseSize = targetCaseSize
            )
            launch(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun deletePackGroup(group: PackGroupEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePackGroup(group)
        }
    }

    // Sharing / PDF
    fun shareCustomerDayReportPdf(visit: VisitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = allCustomers.value.find { it.id == visit.customerId }
            val salesman = allEmployees.value.find { it.id == visit.employeeId }
            val entries = visitEntries.value
            val pdfFile = PdfGenerator.generateCustomerDayReport(
                getApplication(),
                visit,
                customer,
                salesman,
                entries
            )
            launch(Dispatchers.Main) {
                ShareUtil.sharePdfFile(
                    getApplication(),
                    pdfFile,
                    "Himat Textile Customer Day Report - ${visit.visitCode}"
                )
            }
        }
    }

    fun shareCustomerReportWhatsApp(visit: VisitEntity) {
        val customer = allCustomers.value.find { it.id == visit.customerId }
        val entries = visitEntries.value
        val text = ShareUtil.buildCustomerReportText(visit, customer, entries)
        ShareUtil.shareWhatsAppText(getApplication(), text)
    }

    fun shareSupplierCopyPdf(visit: VisitEntity, supplier: SupplierEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = allCustomers.value.find { it.id == visit.customerId }
            val salesman = allEmployees.value.find { it.id == visit.employeeId }
            val supplierEntries = visitEntries.value.filter { it.supplierId == supplier.id }
            val pdfFile = PdfGenerator.generateSupplierCopy(
                getApplication(),
                visit,
                supplier,
                customer,
                salesman,
                supplierEntries
            )
            launch(Dispatchers.Main) {
                ShareUtil.sharePdfFile(
                    getApplication(),
                    pdfFile,
                    "Himat Textile Supplier Voucher - ${supplier.name}"
                )
            }
        }
    }

    fun shareSupplierCopyWhatsApp(visit: VisitEntity, supplier: SupplierEntity) {
        val customer = allCustomers.value.find { it.id == visit.customerId }
        val supplierEntries = visitEntries.value.filter { it.supplierId == supplier.id }
        val text = ShareUtil.buildSupplierCopyText(visit, supplier, customer, supplierEntries)
        ShareUtil.shareWhatsAppText(getApplication(), text)
    }
}
