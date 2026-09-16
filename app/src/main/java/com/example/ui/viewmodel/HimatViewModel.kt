package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.remote.FirebaseRtdbService
import com.example.data.remote.FirebaseStorageService

import com.google.firebase.auth.FirebaseUser
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.CustomerEntity
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
import com.example.data.repository.HimatRepository
import com.example.util.PdfGenerator
import com.example.util.RecordValidator
import com.example.util.ShareUtil
import com.example.util.TallyExportUtil
import com.example.util.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    ADD_STOP,
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
    SUPPLIER_HUB,
    ANALYTICS_DASHBOARD,
    ADD_EDIT_MASTER,
    PAYMENTS,
    PENDINGS,
    PROFILE
}

enum class MasterTab {
    CUSTOMERS,
    SUPPLIERS,
    BRANDS,
    TRANSPORTERS,
    EMPLOYEES,
    MARKETS,
    PRODUCTS
}

class HimatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = HimatRepository(database)
    val authRepository = AuthRepository()
    val rtdbService = FirebaseRtdbService()
    val storageService = FirebaseStorageService()


    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser

    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> =
        authRepository.signInWithGoogle(activity)

    fun signOut(context: Context) {
        viewModelScope.launch {
            authRepository.signOut(context)
        }
    }

    // Super Admin & Authorization State
    private val _isSuperAdmin = MutableStateFlow(false)
    val isSuperAdmin: StateFlow<Boolean> = _isSuperAdmin.asStateFlow()

    private val _isAuthorized = MutableStateFlow<Boolean?>(null) // null = checking, true = authorized, false = restricted
    val isAuthorized: StateFlow<Boolean?> = _isAuthorized.asStateFlow()

    private val _authorizationMessage = MutableStateFlow<String?>(null)
    val authorizationMessage: StateFlow<String?> = _authorizationMessage.asStateFlow()

    private val _superAdminEmails = MutableStateFlow<Set<String>>(emptySet())
    val superAdminEmails: StateFlow<Set<String>> = _superAdminEmails.asStateFlow()

    private val _superAdminEmailsLoaded = MutableStateFlow(false)
    val superAdminEmailsLoaded: StateFlow<Boolean> = _superAdminEmailsLoaded.asStateFlow()

    private val _cloudEmployees = MutableStateFlow<List<EmployeeEntity>>(emptyList())
    val cloudEmployees: StateFlow<List<EmployeeEntity>> = _cloudEmployees.asStateFlow()

    private val _cloudEmployeesLoaded = MutableStateFlow(false)
    val cloudEmployeesLoaded: StateFlow<Boolean> = _cloudEmployeesLoaded.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

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

    // Master Add/Edit state
    private val _activeMasterTab = MutableStateFlow(MasterTab.CUSTOMERS)
    val activeMasterTab: StateFlow<MasterTab> = _activeMasterTab.asStateFlow()

    private val _editingCustomer = MutableStateFlow<CustomerEntity?>(null)
    val editingCustomer: StateFlow<CustomerEntity?> = _editingCustomer.asStateFlow()

    private val _editingSupplier = MutableStateFlow<SupplierEntity?>(null)
    val editingSupplier: StateFlow<SupplierEntity?> = _editingSupplier.asStateFlow()

    private val _editingProduct = MutableStateFlow<ProductEntity?>(null)
    val editingProduct: StateFlow<ProductEntity?> = _editingProduct.asStateFlow()

    private val _editingEmployee = MutableStateFlow<EmployeeEntity?>(null)
    val editingEmployee: StateFlow<EmployeeEntity?> = _editingEmployee.asStateFlow()

    private val _editingBrand = MutableStateFlow<BrandEntity?>(null)
    val editingBrand: StateFlow<BrandEntity?> = _editingBrand.asStateFlow()

    private val _editingTransporter = MutableStateFlow<TransporterEntity?>(null)
    val editingTransporter: StateFlow<TransporterEntity?> = _editingTransporter.asStateFlow()

    private val _editingMarket = MutableStateFlow<MarketEntity?>(null)
    val editingMarket: StateFlow<MarketEntity?> = _editingMarket.asStateFlow()

    // Data streams from Repository
    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBrands: StateFlow<List<BrandEntity>> = repository.allBrands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransporters: StateFlow<List<TransporterEntity>> = repository.allTransporters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMarkets: StateFlow<List<MarketEntity>> = repository.allMarkets
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

    // Visible Streams (Filtered for Soft Deletion & Scoped by Employee Role)
    val visibleCustomers: StateFlow<List<CustomerEntity>> = allCustomers
        .map { list -> list.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleSuppliers: StateFlow<List<SupplierEntity>> = allSuppliers
        .map { list -> list.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleBrands: StateFlow<List<BrandEntity>> = allBrands
        .map { list -> list.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleTransporters: StateFlow<List<TransporterEntity>> = allTransporters
        .map { list -> list.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleMarkets: StateFlow<List<MarketEntity>> = allMarkets
        .map { list -> list.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleProducts: StateFlow<List<ProductEntity>> = allProducts
        .map { list -> list.filter { !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // Salesman sees only their own visits; Admin sees all visits
    val visibleVisits: StateFlow<List<VisitEntity>> = combine(allVisits, currentRole, currentEmployee) { visits, role, emp ->
        visits.filter { !it.isDeleted }.filter { visit ->
            if (role.equals("Admin", ignoreCase = true) || emp == null) {
                true
            } else {
                visit.employeeId == emp.id || visit.employeeName.equals(emp.name, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Salesman sees only entries from their own visits; Admin sees all entries
    val visibleEntries: StateFlow<List<PurchaseEntryEntity>> = combine(allEntries, visibleVisits, currentRole, currentEmployee) { entries, visVisits, role, emp ->
        val allowedVisitIds = visVisits.map { it.id }.toSet()
        entries.filter { !it.isDeleted }.filter { entry ->
            if (role.equals("Admin", ignoreCase = true) || emp == null) {
                true
            } else {
                entry.visitId in allowedVisitIds
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered entries for selected visit
    private val _visitEntries = MutableStateFlow<List<PurchaseEntryEntity>>(emptyList())
    val visitEntries: StateFlow<List<PurchaseEntryEntity>> = _visitEntries.asStateFlow()

    // Pack groups for selected visit
    private val _visitPackGroups = MutableStateFlow<List<PackGroupEntity>>(emptyList())
    val visitPackGroups: StateFlow<List<PackGroupEntity>> = _visitPackGroups.asStateFlow()

    val allPackGroups: StateFlow<List<PackGroupEntity>> = repository.allPackGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        // Start listening to Super Admins
        rtdbService.listenToSuperAdmins { emails ->
            _superAdminEmails.value = emails
            _superAdminEmailsLoaded.value = true
        }

        // Start listening to Employees
        rtdbService.listenToEmployees { employees ->
            _cloudEmployees.value = employees
            _cloudEmployeesLoaded.value = true
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncEmployeesFromCloud(employees)
            }
        }

        // Start listening to Customers
        rtdbService.listenToCustomers { customers ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncCustomersFromCloud(customers)
            }
        }

        // Start listening to Suppliers (includes manufacturers)
        rtdbService.listenToSuppliers { suppliers ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncSuppliersFromCloud(suppliers)
            }
        }

        // Start listening to Products
        rtdbService.listenToProducts { products ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncProductsFromCloud(products)
            }
        }

        // Start listening to Visits
        rtdbService.listenToVisits { visits ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncVisitsFromCloud(visits)
            }
        }

        // Start listening to Purchase Entries
        rtdbService.listenToPurchaseEntries { entries ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncEntriesFromCloud(entries)
            }
        }

        // Start listening to Transactions
        rtdbService.listenToTransactions { txns ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncTransactionsFromCloud(txns)
            }
        }

        // Start listening to Pack Groups
        rtdbService.listenToPackGroups { packGroups ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncPackGroupsFromCloud(packGroups)
            }
        }

        // Start listening to Brands
        rtdbService.listenToBrands { brands ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncBrandsFromCloud(brands)
            }
        }

        // Start listening to Transporters
        rtdbService.listenToTransporters { transporters ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncTransportersFromCloud(transporters)
            }
        }

        // Start listening to Markets
        rtdbService.listenToMarkets { markets ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncMarketsFromCloud(markets)
            }
        }

        // Full Startup Cloud -> Local Sync
        viewModelScope.launch(Dispatchers.IO) {
            syncCloudToLocal()
        }

        // Combine authorization states
        val authFlow = combine(currentUser, _superAdminEmails, _superAdminEmailsLoaded) { user, adminEmails, adminsLoaded ->
            Triple(user, adminEmails, adminsLoaded)
        }
        val empFlow = combine(_cloudEmployees, _cloudEmployeesLoaded, repository.allEmployees) { cloudEmps, empsLoaded, localEmps ->
            Triple(cloudEmps, empsLoaded, localEmps)
        }

        viewModelScope.launch {
            combine(authFlow, empFlow) { auth, emp ->
                reconcileUserAuthorization(auth.first, auth.second, auth.third, emp.first, emp.second, emp.third)
            }.collect { }
        }
    }

    private fun reconcileUserAuthorization(
        user: FirebaseUser?,
        adminEmails: Set<String>,
        adminsLoaded: Boolean,
        cloudEmps: List<EmployeeEntity>,
        empsLoaded: Boolean,
        localEmps: List<EmployeeEntity>
    ) {
        if (user == null) {
            _isAuthorized.value = null
            _isSuperAdmin.value = false
            return
        }

        val userEmail = user.email?.trim()?.lowercase() ?: ""
        if (userEmail.isBlank()) {
            _isAuthorized.value = false
            _isSuperAdmin.value = false
            return
        }

        // Check if user is explicit Super Admin in RTDB
        if (adminEmails.contains(userEmail)) {
            _isSuperAdmin.value = true
            _isAuthorized.value = true
            _authorizationMessage.value = null
            _currentRole.value = "Admin"
            return
        }

        // Check if user's Google email matches an employee in Employee Master (cloud or local)
        val allEmployeesCombined = (cloudEmps + localEmps).distinctBy { it.id }
        val matchedEmployee = allEmployeesCombined.find {
            it.email.isNotBlank() && it.email.trim().equals(userEmail, ignoreCase = true)
        }

        if (matchedEmployee != null) {
            val isEmpAdmin = matchedEmployee.role.equals("Admin", ignoreCase = true)

            // Check if salesman is suspended, blocked, or deactivated by admin
            val isSuspendedOrBlocked = !isEmpAdmin && (
                matchedEmployee.isBlocked ||
                matchedEmployee.isDeleted ||
                matchedEmployee.status.equals("Suspended", ignoreCase = true) ||
                matchedEmployee.status.equals("Deactivated", ignoreCase = true)
            )

            if (isSuspendedOrBlocked) {
                _isSuperAdmin.value = false
                _isAuthorized.value = false
                _currentEmployee.value = matchedEmployee
                _currentRole.value = matchedEmployee.role.ifBlank { "Salesman" }
                val isDeactivated = matchedEmployee.isDeleted || matchedEmployee.status.equals("Deactivated", ignoreCase = true)
                _authorizationMessage.value = if (matchedEmployee.blockedReason.isNotBlank()) {
                    matchedEmployee.blockedReason
                } else if (isDeactivated) {
                    "Your staff account has been deactivated by the Admin. All your historical records remain safe."
                } else {
                    "Your salesman access has been temporarily suspended by the Admin. Please contact management."
                }
                return
            }

            _isSuperAdmin.value = isEmpAdmin
            _isAuthorized.value = true
            _authorizationMessage.value = null
            _currentEmployee.value = matchedEmployee
            _currentRole.value = matchedEmployee.role.ifBlank { "Salesman" }
            return
        }

        // While cloud super admins or employees are still loading, wait before restricting
        if (!adminsLoaded || !empsLoaded) {
            _isAuthorized.value = null
            return
        }

        // If cloud data is fully loaded and no super admins exist in RTDB at all, auto-claim as first Super Admin
        if (adminEmails.isEmpty()) {
            viewModelScope.launch {
                rtdbService.registerSuperAdmin(userEmail, user.displayName ?: "Agency Owner")
            }
            _isSuperAdmin.value = true
            _isAuthorized.value = true
            _authorizationMessage.value = null
            _currentRole.value = "Admin"
            return
        }

        // Unregistered user
        _isSuperAdmin.value = false
        _isAuthorized.value = false
        _authorizationMessage.value = null
    }

    fun retryAuthorization() {
        viewModelScope.launch {
            _isAuthorized.value = null
            val emails = rtdbService.getSuperAdminEmails()
            _superAdminEmails.value = emails
            _superAdminEmailsLoaded.value = true

            val emps = rtdbService.fetchEmployees()
            _cloudEmployees.value = emps
            _cloudEmployeesLoaded.value = true
            repository.syncEmployeesFromCloud(emps)

            syncCloudToLocal()
        }
    }

    fun setRole(role: String, employee: EmployeeEntity? = null) {
        // Only allow switching roles if user is Super Admin
        if (!_isSuperAdmin.value) {
            return
        }
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
                _visitEntries.value = entries.filter { !it.isDeleted }
            }
        }
        viewModelScope.launch {
            repository.getPackGroupsByVisit(visitId).collect { groups ->
                _visitPackGroups.value = groups
            }
        }
    }

    fun openAddStop(visit: VisitEntity) {
        _selectedVisit.value = visit
        observeVisitData(visit.id)
        _currentScreen.value = AppScreen.ADD_STOP
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

    fun openAddMaster(tab: MasterTab) {
        if (tab == MasterTab.EMPLOYEES && !_currentRole.value.equals("Admin", ignoreCase = true)) {
            Toast.makeText(getApplication(), "Only Admins can add new employees", Toast.LENGTH_LONG).show()
            return
        }
        _activeMasterTab.value = tab
        _editingCustomer.value = null
        _editingSupplier.value = null
        _editingProduct.value = null
        _editingEmployee.value = null
        _editingBrand.value = null
        _editingTransporter.value = null
        _editingMarket.value = null
        _currentScreen.value = AppScreen.ADD_EDIT_MASTER
    }

    fun openEditCustomer(customer: CustomerEntity) {
        _activeMasterTab.value = MasterTab.CUSTOMERS
        _editingCustomer.value = customer
        _editingSupplier.value = null
        _editingProduct.value = null
        _editingEmployee.value = null
        _editingBrand.value = null
        _editingTransporter.value = null
        _editingMarket.value = null
        _currentScreen.value = AppScreen.ADD_EDIT_MASTER
    }

    fun openEditSupplier(supplier: SupplierEntity) {
        _activeMasterTab.value = MasterTab.SUPPLIERS
        _editingCustomer.value = null
        _editingSupplier.value = supplier
        _editingProduct.value = null
        _editingEmployee.value = null
        _editingBrand.value = null
        _editingTransporter.value = null
        _editingMarket.value = null
        _currentScreen.value = AppScreen.ADD_EDIT_MASTER
    }

    fun openEditBrand(brand: BrandEntity) {
        _activeMasterTab.value = MasterTab.BRANDS
        _editingCustomer.value = null
        _editingSupplier.value = null
        _editingProduct.value = null
        _editingEmployee.value = null
        _editingBrand.value = brand
        _editingTransporter.value = null
        _editingMarket.value = null
        _currentScreen.value = AppScreen.ADD_EDIT_MASTER
    }

    fun openEditTransporter(transporter: TransporterEntity) {
        _activeMasterTab.value = MasterTab.TRANSPORTERS
        _editingCustomer.value = null
        _editingSupplier.value = null
        _editingProduct.value = null
        _editingEmployee.value = null
        _editingBrand.value = null
        _editingTransporter.value = transporter
        _editingMarket.value = null
        _currentScreen.value = AppScreen.ADD_EDIT_MASTER
    }

    fun openEditMarket(market: MarketEntity) {
        _activeMasterTab.value = MasterTab.MARKETS
        _editingCustomer.value = null
        _editingSupplier.value = null
        _editingProduct.value = null
        _editingEmployee.value = null
        _editingBrand.value = null
        _editingTransporter.value = null
        _editingMarket.value = market
        _currentScreen.value = AppScreen.ADD_EDIT_MASTER
    }

    fun openEditProduct(product: ProductEntity) {
        _activeMasterTab.value = MasterTab.PRODUCTS
        _editingCustomer.value = null
        _editingSupplier.value = null
        _editingProduct.value = product
        _editingEmployee.value = null
        _editingBrand.value = null
        _editingTransporter.value = null
        _editingMarket.value = null
        _currentScreen.value = AppScreen.ADD_EDIT_MASTER
    }

    fun openEditEmployee(employee: EmployeeEntity) {
        if (!_currentRole.value.equals("Admin", ignoreCase = true)) {
            Toast.makeText(getApplication(), "Only Admins can edit employee profiles", Toast.LENGTH_LONG).show()
            return
        }
        _activeMasterTab.value = MasterTab.EMPLOYEES
        _editingCustomer.value = null
        _editingSupplier.value = null
        _editingProduct.value = null
        _editingEmployee.value = employee
        _editingBrand.value = null
        _editingTransporter.value = null
        _editingMarket.value = null
        _currentScreen.value = AppScreen.ADD_EDIT_MASTER
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

    // Full Downstream Cloud -> Local Synchronization
    suspend fun syncCloudToLocal() {
        try {
            _isCloudSyncing.value = true
            rtdbService.purgeLegacyZeroKeys()
            val isCloudEmpty = rtdbService.isCloudEmpty()
            if (isCloudEmpty) {
                // Cloud is completely empty: seed SampleData only if local database is also empty
                repository.ensureInitialDataLoaded(isCloudEmpty = true)
                // And sync local masters to cloud so they persist in RTDB
                syncAllLocalMastersToCloud()
            } else {
                // Cloud has data! Fetch all collections and populate local Room SQLite
                val employees = rtdbService.fetchEmployees()
                if (employees.isNotEmpty()) {
                    _cloudEmployees.value = employees
                    _cloudEmployeesLoaded.value = true
                    repository.syncEmployeesFromCloud(employees)
                }

                val customers = rtdbService.fetchCustomers()
                repository.syncCustomersFromCloud(customers)

                val suppliers = rtdbService.fetchSuppliers()
                repository.syncSuppliersFromCloud(suppliers)

                val products = rtdbService.fetchProducts()
                repository.syncProductsFromCloud(products)

                val visits = rtdbService.fetchVisits()
                repository.syncVisitsFromCloud(visits)

                val entries = rtdbService.fetchPurchaseEntries()
                repository.syncEntriesFromCloud(entries)

                val transactions = rtdbService.fetchTransactions()
                repository.syncTransactionsFromCloud(transactions)

                val packGroups = rtdbService.fetchPackGroups()
                repository.syncPackGroupsFromCloud(packGroups)

                val brands = rtdbService.fetchBrands()
                repository.syncBrandsFromCloud(brands)

                val transporters = rtdbService.fetchTransporters()
                repository.syncTransportersFromCloud(transporters)

                val markets = rtdbService.fetchMarkets()
                repository.syncMarketsFromCloud(markets)

                // Sync any local records that aren't yet in RTDB up to the cloud!
                syncAllLocalMastersToCloud()
            }

            // Always run deduplication to ensure any duplicate records are cleaned up
            repository.deduplicateDatabase(rtdbService)
        } catch (_: Exception) {
            // Offline fallback - local Room DB serves existing records
        } finally {
            _isCloudSyncing.value = false
        }
    }

    // Full Master Cloud Synchronization
    fun syncAllLocalMastersToCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Sync all customers
                allCustomers.value.filter { it.id > 0L }.forEach { rtdbService.syncCustomer(it) }

                // Sync all suppliers & manufacturers
                allSuppliers.value.filter { it.id > 0L }.forEach { rtdbService.syncSupplier(it) }

                // Sync all brands
                allBrands.value.filter { it.id > 0L }.forEach { rtdbService.syncBrand(it) }

                // Sync all transporters
                allTransporters.value.filter { it.id > 0L }.forEach { rtdbService.syncTransporter(it) }

                // Sync all markets
                allMarkets.value.filter { it.id > 0L }.forEach { rtdbService.syncMarket(it) }

                // Sync all products
                allProducts.value.filter { it.id > 0L }.forEach { rtdbService.syncProduct(it) }

                // Sync all employees
                allEmployees.value.filter { it.id > 0L }.forEach { rtdbService.syncEmployee(it) }

                // Sync all pack groups
                allPackGroups.value.filter { it.id > 0L }.forEach { rtdbService.syncPackGroup(it) }

                // Sync all visits
                allVisits.value.filter { it.id > 0L }.forEach { rtdbService.syncVisit(it) }

                // Sync all purchase entries
                allEntries.value.filter { it.id > 0L }.forEach { rtdbService.syncPurchaseEntry(it) }


                // Sync all transactions
                allTransactions.value.filter { it.id > 0L }.forEach { rtdbService.syncTransaction(it) }
            } catch (_: Exception) {
            }
        }
    }

    // Customer Operations
    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val generatedId = repository.saveCustomer(customer)
            val toSync = if (customer.id == 0L) customer.copy(id = generatedId) else customer
            rtdbService.syncCustomer(toSync)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val userRole = _currentRole.value
            val empName = _currentEmployee.value?.name ?: "Salesman"
            val empEmail = currentUser.value?.email ?: ""
            if (userRole.equals("Admin", ignoreCase = true)) {
                repository.deleteCustomer(customer)
                rtdbService.deleteCustomer(customer.id)
            } else {
                val softDeleted = customer.copy(
                    isDeleted = true,
                    deletedAt = System.currentTimeMillis(),
                    deletedBy = empName,
                    deletedByEmail = empEmail,
                    deletedByRole = userRole,
                    deletionStatus = "PENDING_CONFIRMATION"
                )
                repository.saveCustomer(softDeleted)
                rtdbService.softDeleteCustomer(customer, empName, empEmail, userRole)
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Customer sent to Admin for deletion confirmation", Toast.LENGTH_SHORT).show()
                }
            }
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
                val generatedId = repository.saveSupplier(supplier)
                val toSync = if (supplier.id == 0L) supplier.copy(id = generatedId) else supplier
                rtdbService.syncSupplier(toSync)
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
            val userRole = _currentRole.value
            val empName = _currentEmployee.value?.name ?: "Salesman"
            val empEmail = currentUser.value?.email ?: ""
            if (userRole.equals("Admin", ignoreCase = true)) {
                repository.deleteSupplier(supplier)
                rtdbService.deleteSupplier(supplier.id)
            } else {
                val softDeleted = supplier.copy(
                    isDeleted = true,
                    deletedAt = System.currentTimeMillis(),
                    deletedBy = empName,
                    deletedByEmail = empEmail,
                    deletedByRole = userRole,
                    deletionStatus = "PENDING_CONFIRMATION"
                )
                repository.saveSupplier(softDeleted)
                rtdbService.softDeleteSupplier(supplier, empName, empEmail, userRole)
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Supplier sent to Admin for deletion confirmation", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Brand Operations
    fun saveBrand(brand: BrandEntity, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val generatedId = repository.saveBrand(brand)
            val toSync = if (brand.id == 0L) brand.copy(id = generatedId) else brand
            rtdbService.syncBrand(toSync)
            launch(Dispatchers.Main) {
                onSuccess?.invoke()
            }
        }
    }

    fun deleteBrand(brand: BrandEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBrand(brand)
            rtdbService.deleteBrand(brand.id)
        }
    }

    // Transporter Operations
    fun saveTransporter(transporter: TransporterEntity, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val generatedId = repository.saveTransporter(transporter)
            val toSync = if (transporter.id == 0L) transporter.copy(id = generatedId) else transporter
            rtdbService.syncTransporter(toSync)
            launch(Dispatchers.Main) {
                onSuccess?.invoke()
            }
        }
    }

    fun deleteTransporter(transporter: TransporterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransporter(transporter)
            rtdbService.deleteTransporter(transporter.id)
        }
    }

    // Market Operations
    fun saveMarket(market: MarketEntity, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val generatedId = repository.saveMarket(market)
            val toSync = if (market.id == 0L) market.copy(id = generatedId) else market
            rtdbService.syncMarket(toSync)
            launch(Dispatchers.Main) {
                onSuccess?.invoke()
            }
        }
    }

    fun deleteMarket(market: MarketEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMarket(market)
            rtdbService.deleteMarket(market.id)
        }
    }

    // Tally Export Operations
    fun exportCustomersToTallyXml(context: Context) {
        val list = visibleCustomers.value
        val xml = TallyExportUtil.generateCustomersTallyXml(list)
        TallyExportUtil.exportAndShareTallyXml(context, xml, "Himat_Customers_Tally")
    }

    fun exportSuppliersToTallyXml(context: Context) {
        val list = visibleSuppliers.value
        val xml = TallyExportUtil.generateSuppliersTallyXml(list)
        TallyExportUtil.exportAndShareTallyXml(context, xml, "Himat_Suppliers_Tally")
    }

    // Cloud Object Storage Operations
    fun uploadFileToStorage(
        context: Context,
        fileUri: Uri,
        folder: String,
        prefix: String = "doc",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = storageService.uploadFile(context, fileUri, folder, prefix)
            result.onSuccess { downloadUrl ->
                onSuccess(downloadUrl)
            }.onFailure { exc ->
                onError(exc.message ?: "Upload failed")
            }
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
                val generatedId = repository.saveProduct(product)
                val toSync = if (product.id == 0L) product.copy(id = generatedId) else product
                rtdbService.syncProduct(toSync)
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
            val userRole = _currentRole.value
            val empName = _currentEmployee.value?.name ?: "Salesman"
            val empEmail = currentUser.value?.email ?: ""
            if (userRole.equals("Admin", ignoreCase = true)) {
                repository.deleteProduct(product)
                rtdbService.deleteProduct(product.id)
            } else {
                val softDeleted = product.copy(
                    isDeleted = true,
                    deletedAt = System.currentTimeMillis(),
                    deletedBy = empName,
                    deletedByEmail = empEmail,
                    deletedByRole = userRole,
                    deletionStatus = "PENDING_CONFIRMATION"
                )
                repository.saveProduct(softDeleted)
                rtdbService.softDeleteProduct(product, empName, empEmail, userRole)
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Product sent to Admin for deletion confirmation", Toast.LENGTH_SHORT).show()
                }
            }
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
            if (!_currentRole.value.equals("Admin", ignoreCase = true)) {
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Only Admins can create or edit employee records", Toast.LENGTH_LONG).show()
                }
                return@launch
            }
            val generatedId = repository.saveEmployee(employee)
            val toSync = if (employee.id == 0L) employee.copy(id = generatedId) else employee
            rtdbService.syncEmployee(toSync)
        }
    }

    fun deleteEmployee(employee: EmployeeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!_currentRole.value.equals("Admin", ignoreCase = true)) {
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Only Admins can delete employee records", Toast.LENGTH_LONG).show()
                }
                return@launch
            }
            repository.deleteEmployee(employee)
            rtdbService.deleteEmployee(employee.id)
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
            rtdbService.syncVisit(created)
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
            rtdbService.syncVisit(updated)
            if (_selectedVisit.value?.id == visit.id) {
                _selectedVisit.value = updated
            }
        }
    }

    fun deleteVisit(visit: VisitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val userRole = _currentRole.value
            val empName = _currentEmployee.value?.name ?: "Salesman"
            val empEmail = currentUser.value?.email ?: ""
            if (userRole.equals("Admin", ignoreCase = true)) {
                repository.deleteVisit(visit)
                rtdbService.deleteVisit(visit.id)
            } else {
                val softDeleted = visit.copy(
                    isDeleted = true,
                    deletedAt = System.currentTimeMillis(),
                    deletedBy = empName,
                    deletedByEmail = empEmail,
                    deletedByRole = userRole,
                    deletionStatus = "PENDING_CONFIRMATION"
                )
                repository.saveVisit(softDeleted)
                rtdbService.softDeleteVisit(visit, empName, empEmail, userRole)
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Visit sent to Admin for deletion confirmation", Toast.LENGTH_SHORT).show()
                }
            }
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
        transporter: String,
        paymentStatus: String = "Pending",
        paymentMode: String = "Cash",
        paidAmount: Double = 0.0,
        paymentRemarks: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val seqOrderNo = orderNo ?: repository.getNextOrderNumber()
            val totalAmount = pieces * rate
            val gstAmount = (totalAmount * gstRate) / 100.0
            val grandTotal = totalAmount + gstAmount

            val entry = PurchaseEntryEntity(
                orderNo = seqOrderNo,
                visitId = visitId,
                supplierId = supplier.id,
                supplierName = supplier.name,
                supplierType = supplier.type,
                itemCode = itemCode.trim().uppercase(Locale.getDefault()),
                pieces = pieces,
                rate = rate,
                totalAmount = totalAmount,
                gstRate = gstRate,
                gstAmount = gstAmount,
                grandTotalWithGst = grandTotal,
                caseSize = caseSize,
                caseCount = if (caseSize > 0) pieces / caseSize else 0,
                loosePieces = if (caseSize > 0) pieces % caseSize else 0,
                expectedDeliveryDate = expectedDeliveryDate,
                transporter = transporter,
                paymentStatus = paymentStatus,
                paymentMode = paymentMode,
                paidAmount = if (paymentStatus.equals("Received", ignoreCase = true) && paidAmount == 0.0) grandTotal else paidAmount,
                paymentRemarks = paymentRemarks
            )
            val entryId = repository.savePurchaseEntry(entry)
            val savedEntry = entry.copy(id = entryId)
            rtdbService.syncPurchaseEntry(savedEntry)

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
                totalAmount = totalAmount,
                gstRate = gstRate,
                gstAmount = gstAmount,
                grandTotalWithGst = grandTotal,
                caseSize = caseSize,
                caseCount = if (caseSize > 0) pieces / caseSize else 0,
                loosePieces = if (caseSize > 0) pieces % caseSize else 0,
                deliveryStatus = "Pending",
                transporter = transporter,
                paymentStatus = paymentStatus,
                paymentMode = paymentMode,
                paidAmount = savedEntry.paidAmount,
                paymentRemarks = paymentRemarks,
                transactionDate = visit?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
            val txnId = repository.saveTransaction(txn)
            rtdbService.syncTransaction(txn.copy(id = txnId))
        }
    }

    fun updatePurchaseEntry(
        entry: PurchaseEntryEntity,
        newPieces: Int,
        newRate: Double,
        newCaseSize: Int,
        deliveryStatus: String,
        transporter: String,
        expectedDeliveryDate: String,
        paymentStatus: String,
        paymentMode: String,
        paidAmount: Double,
        paymentRemarks: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val totalAmount = newPieces * newRate
            val gstAmount = (totalAmount * entry.gstRate) / 100.0
            val grandTotal = totalAmount + gstAmount

            val resolvedPaidAmount = when {
                paymentStatus.equals("Received", ignoreCase = true) -> grandTotal
                paymentStatus.equals("Pending", ignoreCase = true) -> 0.0
                else -> paidAmount
            }

            val toUpdate = entry.copy(
                pieces = newPieces,
                rate = newRate,
                caseSize = newCaseSize,
                deliveryStatus = deliveryStatus,
                transporter = transporter,
                expectedDeliveryDate = expectedDeliveryDate,
                paymentStatus = paymentStatus,
                paymentMode = paymentMode,
                paidAmount = resolvedPaidAmount,
                paymentRemarks = paymentRemarks
            )

            val updated = repository.updatePurchaseEntryDetails(toUpdate)
            rtdbService.syncPurchaseEntry(updated)

            val txn = repository.getTransactionByOrderNo(updated.orderNo)
            if (txn != null) {
                rtdbService.syncTransaction(txn)
            }

            launch(Dispatchers.Main) {
                onSuccess?.invoke()
            }
        }
    }

    fun updatePaymentInfo(
        entry: PurchaseEntryEntity,
        paymentStatus: String,
        paymentMode: String,
        paidAmount: Double,
        paymentRemarks: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePaymentInfo(
                id = entry.id,
                paymentStatus = paymentStatus,
                paymentMode = paymentMode,
                paidAmount = paidAmount,
                paymentRemarks = paymentRemarks
            )
            val updated = repository.getEntryById(entry.id)
            if (updated != null) {
                rtdbService.syncPurchaseEntry(updated)
                val txn = repository.getTransactionByOrderNo(updated.orderNo)
                if (txn != null) {
                    rtdbService.syncTransaction(txn)
                }
            }
            launch(Dispatchers.Main) {
                onSuccess?.invoke()
            }
        }
    }

    fun deletePurchaseEntry(entry: PurchaseEntryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val userRole = _currentRole.value
            val empName = _currentEmployee.value?.name ?: "Salesman"
            val empEmail = currentUser.value?.email ?: ""
            if (userRole.equals("Admin", ignoreCase = true)) {
                repository.deletePurchaseEntry(entry)
                rtdbService.deletePurchaseEntry(entry.id)
            } else {
                val softDeleted = entry.copy(
                    isDeleted = true,
                    deletedAt = System.currentTimeMillis(),
                    deletedBy = empName,
                    deletedByEmail = empEmail,
                    deletedByRole = userRole,
                    deletionStatus = "PENDING_CONFIRMATION"
                )
                repository.savePurchaseEntry(softDeleted)
                rtdbService.softDeletePurchaseEntry(entry, empName, empEmail, userRole)
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Order entry sent to Admin for deletion confirmation", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateDeliveryStatus(entry: PurchaseEntryEntity, newStatus: String, transporter: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDeliveryStatus(entry.id, newStatus, transporter)
            val updated = repository.getEntryById(entry.id)
            if (updated != null) {
                rtdbService.syncPurchaseEntry(updated)
                val txn = repository.getTransactionByOrderNo(updated.orderNo)
                if (txn != null) {
                    rtdbService.syncTransaction(txn)
                }
            }
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
            val packGroupId = repository.createMixedPackGroup(
                visitId = visitId,
                selectedEntries = selectedEntries,
                targetCaseSize = targetCaseSize
            )
            // Sync all updated entries to RTDB
            selectedEntries.forEach { entry ->
                val updatedEntry = repository.getEntryById(entry.id)
                if (updatedEntry != null) {
                    rtdbService.syncPurchaseEntry(updatedEntry)
                }
            }
            // Fetch and sync created pack group
            val createdGroup = repository.getPackGroupById(packGroupId)
            if (createdGroup != null) {
                rtdbService.syncPackGroup(createdGroup)
            }
            launch(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun deletePackGroup(group: PackGroupEntity, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val entryIds = group.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            repository.deletePackGroup(group)
            rtdbService.deletePackGroup(group.id)
            // RTDB cleanup
            entryIds.forEach { id ->
                val updatedEntry = repository.getEntryById(id)
                if (updatedEntry != null) {
                    rtdbService.syncPurchaseEntry(updatedEntry)
                }
            }
            launch(Dispatchers.Main) {
                onSuccess?.invoke()
            }
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
            val supplierEntries = visitEntries.value.filter {
                it.supplierId == supplier.id || (it.supplierName.isNotBlank() && it.supplierName.trim().equals(supplier.name.trim(), ignoreCase = true))
            }
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
        val supplierEntries = visitEntries.value.filter {
            it.supplierId == supplier.id || (it.supplierName.isNotBlank() && it.supplierName.trim().equals(supplier.name.trim(), ignoreCase = true))
        }
        val text = ShareUtil.buildSupplierCopyText(visit, supplier, customer, supplierEntries)
        ShareUtil.shareWhatsAppText(getApplication(), text)
    }
}

