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
import com.example.data.remote.FcmTokenRegistrar
import com.example.data.remote.FirebaseStorageService

import com.google.firebase.auth.FirebaseUser
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.ChequePdcEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.CustomerRegistrationRequestEntity
import com.example.data.local.entity.SupplierRegistrationRequestEntity
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
import com.example.data.repository.HimatRepository
import com.example.util.AppNotifications
import com.example.util.DeleteImpact
import com.example.util.DeletionRequest
import com.example.util.IdGenerator
import com.example.util.OrphanScan
import com.example.util.RecordKind
import com.example.util.SyncStatus
import com.example.util.cleared
import com.example.util.markedDeleted
import com.example.util.PdfGenerator
import com.example.util.WorkNotification
import com.example.util.isPhoneTrip
import com.example.util.RelatedLogic
import com.example.util.Roles
import com.example.util.StaffCodes
import com.example.util.SupplierQueue
import com.example.util.TripTypes
import com.example.util.brandName
import com.example.util.hasMember
import com.example.util.isClosed
import com.example.util.tripMembers
import com.example.util.withBrandName
import com.example.util.withBrandNames
import com.example.util.withMember
import com.example.util.RecordValidator
import com.example.util.ShareUtil
import com.example.util.SupplierOrderFormOptions
import com.example.util.TallyExportUtil
import com.example.util.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import android.util.Log
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
    PRODUCT_DETAIL,
    BRAND_DETAIL,
    TRANSPORTER_DETAIL,
    MARKET_DETAIL,
    ORDER_DETAIL,
    PRODUCT_MASTER,
    BRAND_MASTER,
    TRANSPORTER_MASTER,
    MARKET_MASTER,
    SUPPLIER_HUB,
    ANALYTICS_DASHBOARD,
    ADD_EDIT_MASTER,
    PAYMENTS,
    PENDINGS,
    PROFILE,
    LEADS,
    PURCHASE_ORDERS,
    CUSTOMER_ORDERS_REPORT,
    CHEQUE_PDC,
    // v18 navigation
    MASTERS,
    MORE,
    /** Admin console for staff delete requests: approve (hard delete) or reject (restore). */
    DELETION_REQUESTS,
    SUB_AGENT_MASTER,
    SUB_AGENT_DETAIL,
    SUB_AGENT_FORM;

    companion object {
        /** Bottom navigation tabs. Opening one of these clears the back stack. */
        val ROOT_SCREENS = setOf(DASHBOARD, VISITS, PURCHASE_ORDERS, MASTERS, MORE)

        /** Screens that belong to the Masters tab (hub + every master list). */
        val MASTER_SCREENS = setOf(
            MASTERS, CUSTOMER_MASTER, SUPPLIER_MASTER, EMPLOYEE_MASTER, PRODUCT_MASTER,
            BRAND_MASTER, TRANSPORTER_MASTER, MARKET_MASTER, SUB_AGENT_MASTER
        )

        fun masterTabFor(screen: AppScreen): MasterTab? = when (screen) {
            CUSTOMER_MASTER -> MasterTab.CUSTOMERS
            SUPPLIER_MASTER -> MasterTab.SUPPLIERS
            EMPLOYEE_MASTER -> MasterTab.EMPLOYEES
            PRODUCT_MASTER -> MasterTab.PRODUCTS
            BRAND_MASTER -> MasterTab.BRANDS
            TRANSPORTER_MASTER -> MasterTab.TRANSPORTERS
            MARKET_MASTER -> MasterTab.MARKETS
            else -> null
        }

        fun masterScreenFor(tab: MasterTab): AppScreen = when (tab) {
            MasterTab.CUSTOMERS -> CUSTOMER_MASTER
            MasterTab.SUPPLIERS -> SUPPLIER_MASTER
            MasterTab.EMPLOYEES -> EMPLOYEE_MASTER
            MasterTab.PRODUCTS -> PRODUCT_MASTER
            MasterTab.BRANDS -> BRAND_MASTER
            MasterTab.TRANSPORTERS -> TRANSPORTER_MASTER
            MasterTab.MARKETS -> MARKET_MASTER
        }
    }
}

private const val MAX_BACK_STACK = 30

/** "Close this trip?" after the last supplier of a phone order. [savedCount] = orders saved just now. */
data class CloseTripPromptState(val tripId: Long, val savedCount: Int)

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
    private val authPrefs = application.getSharedPreferences("himat_auth_prefs", Context.MODE_PRIVATE)

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser

    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> =
        authRepository.signInWithGoogle(activity)

    fun signOut(context: Context) {
        viewModelScope.launch {
            // Stop team pushes reaching this phone once nobody is signed in
            FcmTokenRegistrar.unregister(context)
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

    private val initialCachedAdmins = authPrefs.getStringSet("cached_super_admins", emptySet()) ?: emptySet()
    private val _superAdminEmails = MutableStateFlow<Set<String>>(initialCachedAdmins)
    val superAdminEmails: StateFlow<Set<String>> = _superAdminEmails.asStateFlow()

    private val _superAdminEmailsLoaded = MutableStateFlow(initialCachedAdmins.isNotEmpty())
    val superAdminEmailsLoaded: StateFlow<Boolean> = _superAdminEmailsLoaded.asStateFlow()

    private val _cloudEmployees = MutableStateFlow<List<EmployeeEntity>>(emptyList())
    val cloudEmployees: StateFlow<List<EmployeeEntity>> = _cloudEmployees.asStateFlow()

    private val _cloudEmployeesLoaded = MutableStateFlow(false)
    val cloudEmployeesLoaded: StateFlow<Boolean> = _cloudEmployeesLoaded.asStateFlow()

    private var realtimeSyncJob: Job? = null

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

    /**
     * Back stack (see navigate helpers below). Must be declared BEFORE the init block:
     * init collects currentUser on Main.immediate, so a signed-out start calls
     * stopRealtimeSync() -> resetNavigation() while the constructor is still running.
     */
    private val navStack = ArrayDeque<NavEntry>()

    private val _selectedVisit = MutableStateFlow<VisitEntity?>(null)
    val selectedVisit: StateFlow<VisitEntity?> = _selectedVisit.asStateFlow()

    /** Which master list is open (null = Masters hub). Kept here so it survives opening a detail screen. */
    private val _mastersCategory = MutableStateFlow<MasterTab?>(null)
    val mastersCategory: StateFlow<MasterTab?> = _mastersCategory.asStateFlow()

    /** Status chip to preselect when the Orders tab opens (e.g. "Pending" from a Home shortcut). */
    private val _ordersStatusFilter = MutableStateFlow("All")
    val ordersStatusFilter: StateFlow<String> = _ordersStatusFilter.asStateFlow()

    /** Super admin "view as" simulation (role, employee); null = normal admin view. */
    private var simulatedRole: Pair<String, EmployeeEntity?>? = null

    private val _selectedSupplierForCopy = MutableStateFlow<SupplierEntity?>(null)
    val selectedSupplierForCopy: StateFlow<SupplierEntity?> = _selectedSupplierForCopy.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _selectedCustomerForReport = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomerForReport: StateFlow<CustomerEntity?> = _selectedCustomerForReport.asStateFlow()

    private val _selectedSupplier = MutableStateFlow<SupplierEntity?>(null)
    val selectedSupplier: StateFlow<SupplierEntity?> = _selectedSupplier.asStateFlow()

    private val _selectedEmployeeDetail = MutableStateFlow<EmployeeEntity?>(null)
    val selectedEmployeeDetail: StateFlow<EmployeeEntity?> = _selectedEmployeeDetail.asStateFlow()

    private val _selectedProduct = MutableStateFlow<ProductEntity?>(null)
    val selectedProduct: StateFlow<ProductEntity?> = _selectedProduct.asStateFlow()

    private val _selectedBrand = MutableStateFlow<BrandEntity?>(null)
    val selectedBrand: StateFlow<BrandEntity?> = _selectedBrand.asStateFlow()

    private val _selectedTransporter = MutableStateFlow<TransporterEntity?>(null)
    val selectedTransporter: StateFlow<TransporterEntity?> = _selectedTransporter.asStateFlow()

    private val _selectedMarket = MutableStateFlow<MarketEntity?>(null)
    val selectedMarket: StateFlow<MarketEntity?> = _selectedMarket.asStateFlow()

    private val _selectedPurchaseEntry = MutableStateFlow<PurchaseEntryEntity?>(null)
    val selectedPurchaseEntry: StateFlow<PurchaseEntryEntity?> = _selectedPurchaseEntry.asStateFlow()

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

    /** Everybody in the employees node: admins, staff (salesmen) and sub agents. */
    val allPeople: StateFlow<List<EmployeeEntity>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Staff + admins only. Use for salesman pickers, staff lists and stats (sub agents excluded). */
    val allEmployees: StateFlow<List<EmployeeEntity>> = allPeople
        .map { list -> list.filter { !Roles.isAgent(it.role) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Sub Agents (employees with role = "Agent"): people who bring customers. */
    val subAgents: StateFlow<List<EmployeeEntity>> = allPeople
        .map { list -> list.filter { Roles.isAgent(it.role) && !it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Every staff record ever, deactivated ones included. Do not show this in a list — it exists so a
     * new staff code is never one that somebody who left is still holding in old reports.
     */
    // Eagerly on purpose: the staff form asks for the next code the moment it opens, and if this were
    // still empty it would offer a code a departed staff member is holding.
    val everyStaffRecord: StateFlow<List<EmployeeEntity>> = repository.everyStaffRecord
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The next free staff code, e.g. "EMP-07". Pass [StaffCodes.AGENT_PREFIX] for a Sub Agent. */
    fun nextStaffCode(prefix: String = StaffCodes.STAFF_PREFIX): String =
        StaffCodes.next(everyStaffRecord.value.ifEmpty { allPeople.value }, prefix)

    /**
     * Every trip, with customerName showing the customer's BRAND (shop / firm) name instead of the owner,
     * so all trip and order lists, logs and reports read the same way. Trips whose customer record is
     * missing keep the stored name.
     */
    val allVisits: StateFlow<List<VisitEntity>> = combine(repository.allVisits, repository.allCustomers) { visits, customers ->
        visits.withBrandNames(customers)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEntries: StateFlow<List<PurchaseEntryEntity>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctItemCodes: StateFlow<List<String>> = repository.distinctItemCodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Leads & Customer Registration Requests
    val allLeads: StateFlow<List<LeadEntity>> = repository.allLeads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerLeads: StateFlow<List<LeadEntity>> = allLeads.map { list ->
        list.filter { it.type == "customer" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supplierLeads: StateFlow<List<LeadEntity>> = allLeads.map { list ->
        list.filter { it.type == "supplier" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _registrationRequests = MutableStateFlow<List<CustomerRegistrationRequestEntity>>(emptyList())
    val registrationRequests: StateFlow<List<CustomerRegistrationRequestEntity>> = _registrationRequests.asStateFlow()

    val pendingRegistrationRequestsCount: StateFlow<Int> = _registrationRequests.map { list ->
        list.count { it.status.uppercase() == "PENDING" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _supplierRegistrationRequests = MutableStateFlow<List<SupplierRegistrationRequestEntity>>(emptyList())
    val supplierRegistrationRequests: StateFlow<List<SupplierRegistrationRequestEntity>> = _supplierRegistrationRequests.asStateFlow()

    val pendingSupplierRegistrationRequestsCount: StateFlow<Int> = _supplierRegistrationRequests.map { list ->
        list.count { it.status.uppercase() == "PENDING" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Cheques PDC
    val allChequesPdc: StateFlow<List<ChequePdcEntity>> = repository.allChequesPdc
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueTodayChequesCount: StateFlow<Int> = allChequesPdc.map { list ->
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        list.count { !it.isDeleted && it.chequeDate == today && (it.status.equals("Pending", ignoreCase = true) || it.status.equals("Due Today", ignoreCase = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Role helpers for the three user types (Admin / Staff / Sub Agent)
    val isAgentUser: StateFlow<Boolean> = currentRole
        .map { Roles.isAgent(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isAdminUser: StateFlow<Boolean> = combine(isSuperAdmin, currentRole) { superAdmin, role ->
        superAdmin || Roles.isAdmin(role)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Visible Streams (Filtered for Soft Deletion & Scoped by Employee Role)
    // Sub Agents only see the customers linked to them.
    val visibleCustomers: StateFlow<List<CustomerEntity>> = combine(allCustomers, currentRole, currentEmployee) { list, role, emp ->
        val active = list.filter { !it.isDeleted }
        if (Roles.isAgent(role) && emp != null) {
            active.filter { c ->
                c.subAgentId == emp.id ||
                    (c.subAgentId == null && emp.name.isNotBlank() && c.subAgentName.trim().equals(emp.name.trim(), ignoreCase = true))
            }
        } else active
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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


    // Admin sees every trip. Staff see trips they started or joined. Sub Agents see trips of their customers.
    /**
     * Trips everyone in the office can see.
     *
     * Admins and staff both see every trip: the agency works as one book, and a salesman who joins
     * a trip or covers for someone has to be able to find it. Only a Sub Agent is scoped, to the
     * trips of the customers linked to them.
     *
     * "My trips" is a filter on the Trips screen, not a wall — trips used to be hidden from staff
     * whose membership had been overwritten by another phone, which read as data loss.
     */
    val visibleVisits: StateFlow<List<VisitEntity>> = combine(allVisits, currentRole, currentEmployee, visibleCustomers) { visits, role, _, custs ->
        val active = visits.filter { !it.isDeleted }
        if (Roles.isAgent(role)) {
            val customerIds = custs.map { it.id }.toSet()
            active.filter { it.customerId in customerIds }
        } else active
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Open trips started by other salesmen that the logged-in staff member can join from this phone. */
    val joinableVisits: StateFlow<List<VisitEntity>> = combine(allVisits, currentRole, currentEmployee) { visits, role, emp ->
        if (emp == null || Roles.isAgent(role)) emptyList()
        else visits.filter { !it.isDeleted && !it.isClosed() && !it.hasMember(emp) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Orders everyone in the office can see.
     *
     * Admins and staff see every order, including ones whose trip has not reached this phone yet.
     * Tying visibility to the trips table is what made an admin say "I cannot see the entries my
     * staff added": one missing trip row hid all of its orders.
     *
     * A Sub Agent still only sees the orders of trips for their own customers.
     */
    val visibleEntries: StateFlow<List<PurchaseEntryEntity>> = combine(allEntries, visibleVisits, currentRole) { entries, visVisits, role ->
        val active = entries.filter { !it.isDeleted }
        if (Roles.isAgent(role)) {
            val allowedVisitIds = visVisits.map { it.id }.toSet()
            active.filter { it.visitId in allowedVisitIds }
        } else active
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pending (non-Delivered) order count per customer — for the master list badge
    // Uses allVisits/allEntries (same as CustomerDetailScreen) so we never under-count
    val pendingCountByCustomer: StateFlow<Map<Long, Int>> = combine(allEntries, allVisits, allCustomers) { entries, visits, customers ->
        // Same orphan fallback as the customer screen (trips saved without / with a stale customer id)
        val visitCustomerMap = RelatedLogic.customerIdByTrip(visits.filter { !it.isDeleted }, customers)
        val countMap = mutableMapOf<Long, Int>()
        entries.filter { !it.isDeleted }.forEach { entry ->
            if (entry.deliveryStatus.lowercase() != "delivered") {
                val custId = visitCustomerMap[entry.visitId] ?: return@forEach
                countMap[custId] = (countMap[custId] ?: 0) + 1
            }
        }
        countMap
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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
        // Observe authentication state to manage RTDB listeners & cloud sync lifecycle
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    startRealtimeSync(user)
                } else {
                    stopRealtimeSync()
                }
            }
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

    private fun startRealtimeSync(user: FirebaseUser) {
        realtimeSyncJob?.cancel()
        rtdbService.removeAllListeners()

        val userEmail = user.email?.trim()?.lowercase() ?: ""

        // 1. Start listening to Super Admins & update cache
        rtdbService.listenToSuperAdmins { emails ->
            _superAdminEmails.value = emails
            _superAdminEmailsLoaded.value = true
            if (emails.isNotEmpty()) {
                authPrefs.edit().putStringSet("cached_super_admins", emails).apply()
            }
        }

        // 2. Start listening to Employees
        rtdbService.listenToEmployees { employees ->
            _cloudEmployees.value = employees
            _cloudEmployeesLoaded.value = true
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncEmployeesFromCloud(employees)
            }
        }

        // 3. Start listening to Customers
        rtdbService.listenToCustomers { customers ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncCustomersFromCloud(customers)
            }
        }

        // 4. Start listening to Suppliers (includes manufacturers)
        rtdbService.listenToSuppliers { suppliers ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncSuppliersFromCloud(suppliers)
            }
        }

        // 5. Start listening to Products
        rtdbService.listenToProducts { products ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncProductsFromCloud(products)
            }
        }

        // 6. Start listening to Visits
        rtdbService.listenToVisits { visits ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncVisitsFromCloud(visits)
            }
        }

        // 6b. Work announcements from the rest of the team -> phone notifications
        AppNotifications.ensureChannel(getApplication())
        notificationsSince = System.currentTimeMillis()
        rtdbService.listenToNotifications { notes -> handleNotifications(notes) }

        // 6c. Pending deletion requests, for the admin console
        rtdbService.listenToDeletionRequestList { requests -> _deletionRequests.value = requests }

        // 6d. Live connection state, for the sync indicator in the header
        rtdbService.listenToConnection { online -> _isOnline.value = online }

        // 7. Start listening to Purchase Entries
        rtdbService.listenToPurchaseEntries { entries ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncEntriesFromCloud(entries)
                notifyNewCloudOrders(entries)
            }
        }

        // 8. Start listening to Transactions
        rtdbService.listenToTransactions { txns ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncTransactionsFromCloud(txns)
            }
        }

        // 9. Start listening to Pack Groups
        rtdbService.listenToPackGroups { packGroups ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncPackGroupsFromCloud(packGroups)
            }
        }

        // 10. Start listening to Brands
        rtdbService.listenToBrands { brands ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncBrandsFromCloud(brands)
            }
        }

        // 11. Start listening to Transporters
        rtdbService.listenToTransporters { transporters ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncTransportersFromCloud(transporters)
            }
        }

        // 12. Start listening to Markets
        rtdbService.listenToMarkets { markets ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncMarketsFromCloud(markets)
            }
        }

        // 13. Start listening to Leads
        rtdbService.listenToLeads { leads ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncLeadsFromCloud(leads)
            }
        }

        // 14. Start listening to Registration Requests
        rtdbService.listenToRegistrationRequests { reqs ->
            _registrationRequests.value = reqs
        }

        // 15. Start listening to Supplier Registration Requests
        rtdbService.listenToSupplierRegistrationRequests { reqs ->
            _supplierRegistrationRequests.value = reqs
        }

        // 16. Start listening to Cheques PDC
        rtdbService.listenToChequesPdc { cheques ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.syncChequesFromCloud(cheques)
            }
        }

        // 17. Start listening to Deletion Requests
        rtdbService.listenToDeletionRequests {
            viewModelScope.launch(Dispatchers.IO) {
                val markets = rtdbService.fetchMarkets()
                repository.syncMarketsFromCloud(markets)
                val suppliers = rtdbService.fetchSuppliers()
                repository.syncSuppliersFromCloud(suppliers)
                val customers = rtdbService.fetchCustomers()
                repository.syncCustomersFromCloud(customers)
                val brands = rtdbService.fetchBrands()
                repository.syncBrandsFromCloud(brands)
                val transporters = rtdbService.fetchTransporters()
                repository.syncTransportersFromCloud(transporters)
                val cheques = rtdbService.fetchChequesPdc()
                repository.syncChequesFromCloud(cheques)
            }
        }

        // 18. Proactive fast fetch of Super Admins and Employees with a 4s timeout guarantee
        realtimeSyncJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeoutOrNull(4000L) {
                    val emails = rtdbService.getSuperAdminEmails()
                    if (emails.isNotEmpty()) {
                        _superAdminEmails.value = emails
                        authPrefs.edit().putStringSet("cached_super_admins", emails).apply()
                    }
                    val emps = rtdbService.fetchEmployees()
                    if (emps.isNotEmpty()) {
                        _cloudEmployees.value = emps
                        repository.syncEmployeesFromCloud(emps)
                    }
                }
            } catch (e: Exception) {
                Log.w("HimatViewModel", "Proactive auth fetch error: ${e.message}")
            } finally {
                _superAdminEmailsLoaded.value = true
                _cloudEmployeesLoaded.value = true
            }

            // Sync all cloud collections into local Room database
            syncCloudToLocal()

            // Anything this phone saved but never managed to upload goes up now. This is what
            // recovers work that was only ever cached on a salesman's device.
            uploadPendingToCloud()
        }
    }

    private fun stopRealtimeSync() {
        realtimeSyncJob?.cancel()
        rtdbService.removeAllListeners()
        _isSuperAdmin.value = false
        _isAuthorized.value = null
        _authorizationMessage.value = null
        _currentEmployee.value = null
        simulatedRole = null
        // Next login (maybe a different person) starts fresh on Home
        resetNavigation()
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

        // Google email -> employees node record (staff, admin or sub agent).
        //
        // The cloud copy is listed first and wins on a matching id, because it is the only copy that
        // knows about a suspension or a removal. The local copy is a fallback for working offline.
        fun List<EmployeeEntity>.matching() = find {
            (it.email.isNotBlank() && it.email.trim().equals(userEmail, ignoreCase = true)) ||
                (it.alternateEmail.isNotBlank() && it.alternateEmail.trim().equals(userEmail, ignoreCase = true))
        }

        val cloudMatch = cloudEmps.matching()
        val localMatch = localEmps.matching()
        val matchedEmployee = cloudMatch ?: localMatch

        // Check if user is explicit Super Admin in RTDB or Local Cache
        if (adminEmails.contains(userEmail)) {
            _isSuperAdmin.value = true
            _isAuthorized.value = true
            _authorizationMessage.value = null
            // Keep a "view as salesman" simulation alive across sync updates
            val simulated = simulatedRole
            if (simulated != null) {
                _currentRole.value = simulated.first
                _currentEmployee.value = simulated.second
            } else {
                _currentRole.value = "Admin"
                // An owner who also has a staff record can start / join trips under their own name
                _currentEmployee.value = matchedEmployee?.takeIf { !Roles.isAgent(it.role) }
            }
            authPrefs.edit().putStringSet("cached_super_admins", adminEmails).apply()
            publishPushToken()
            return
        }

        // Membership is decided by the office, not by this phone's memory.
        //
        // Removing a staff record used to delete the cloud node outright, while every *other* phone
        // kept its own row (the ingestion never hard-deletes, and the local query already hides
        // isDeleted rows). The removed person's phone then matched its own stale copy and let them
        // straight back in. So a local-only match is not proof of anything: wait for the cloud, and
        // once the cloud has spoken and does not know this email, the account is out.
        if (cloudMatch == null && localMatch != null) {
            if (!empsLoaded) {
                _isAuthorized.value = null
                return
            }
            _isSuperAdmin.value = false
            _isAuthorized.value = false
            _currentEmployee.value = null
            _authorizationMessage.value =
                "This account is no longer on the staff list. Ask the Admin to add it again. " +
                    "Everything you entered stays safe in the office records."
            return
        }

        if (matchedEmployee != null) {
            val isEmpAdmin = matchedEmployee.role.equals("Admin", ignoreCase = true)

            // Suspended, blocked or deactivated — and this applies to an Admin record too. It used to
            // skip admins, which meant deactivating an admin did nothing at all. A real owner is
            // unaffected: they are matched by the super_admins branch above and never reach here.
            val isSuspendedOrBlocked =
                matchedEmployee.isBlocked ||
                    matchedEmployee.isDeleted ||
                    matchedEmployee.status.equals("Suspended", ignoreCase = true) ||
                    matchedEmployee.status.equals("Deactivated", ignoreCase = true)

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
            publishPushToken()
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
            _superAdminEmails.value = setOf(userEmail)
            _superAdminEmailsLoaded.value = true
            authPrefs.edit().putStringSet("cached_super_admins", setOf(userEmail)).apply()
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

    /**
     * Re-checks this account against the office and reports back when it has settled.
     *
     * Used by the access sheet: after an admin changes somebody's role or unblocks them, the user
     * needs a way to pick that up without reinstalling or waiting.
     */
    fun refreshAccess(onFinished: (() -> Unit)? = null) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user == null) {
                onFinished?.invoke()
                return@launch
            }
            _superAdminEmailsLoaded.value = false
            _cloudEmployeesLoaded.value = false
            try {
                withTimeoutOrNull(8000L) {
                    val emails = rtdbService.getSuperAdminEmails()
                    if (emails.isNotEmpty()) {
                        _superAdminEmails.value = emails
                        authPrefs.edit().putStringSet("cached_super_admins", emails).apply()
                    }
                    val emps = rtdbService.fetchEmployees()
                    if (emps.isNotEmpty()) {
                        _cloudEmployees.value = emps
                        repository.syncEmployeesFromCloud(emps)
                    }
                }
                _pendingPushCount.value = repository.pendingPushCount()
            } catch (e: Exception) {
                Log.w("HimatViewModel", "refreshAccess failed: ${e.message}")
            } finally {
                _superAdminEmailsLoaded.value = true
                _cloudEmployeesLoaded.value = true
                onFinished?.invoke()
            }
        }
    }

    fun retryAuthorization() {
        val user = currentUser.value
        if (user != null) {
            _isAuthorized.value = null
            _superAdminEmailsLoaded.value = false
            _cloudEmployeesLoaded.value = false
            startRealtimeSync(user)
        } else {
            _isAuthorized.value = null
        }
    }

    fun setRole(role: String, employee: EmployeeEntity? = null) {
        // Only allow switching roles if user is Super Admin
        if (!_isSuperAdmin.value) {
            return
        }
        simulatedRole = if (Roles.isAdmin(role)) null else role to employee
        _currentRole.value = role
        _currentEmployee.value = employee
        resetNavigation()
    }

    // ---------------------------------------------------------------------
    // Navigation: one real back stack. Every entry remembers which record was
    // open, so "Back" always returns to exactly where the user came from.
    // ---------------------------------------------------------------------

    private data class NavEntry(
        val screen: AppScreen,
        val visit: VisitEntity?,
        val customer: CustomerEntity?,
        val supplier: SupplierEntity?,
        val employee: EmployeeEntity?,
        val product: ProductEntity?,
        val brand: BrandEntity?,
        val transporter: TransporterEntity?,
        val market: MarketEntity?,
        val entry: PurchaseEntryEntity?,
        val supplierForCopy: SupplierEntity?,
        val customerForReport: CustomerEntity?,
        val mastersCategory: MasterTab?
    )

    // navStack itself is declared near the top of the class (initialization order, see comment there)

    private fun currentNavEntry() = NavEntry(
        screen = _currentScreen.value,
        visit = _selectedVisit.value,
        customer = _selectedCustomer.value,
        supplier = _selectedSupplier.value,
        employee = _selectedEmployeeDetail.value,
        product = _selectedProduct.value,
        brand = _selectedBrand.value,
        transporter = _selectedTransporter.value,
        market = _selectedMarket.value,
        entry = _selectedPurchaseEntry.value,
        supplierForCopy = _selectedSupplierForCopy.value,
        customerForReport = _selectedCustomerForReport.value,
        mastersCategory = _mastersCategory.value
    )

    /** Open [screen]. [setup] selects the record to show; the previous screen goes on the back stack. */
    private fun go(screen: AppScreen, setup: () -> Unit = {}) {
        val before = currentNavEntry()
        setup()
        if (screen in AppScreen.ROOT_SCREENS) {
            navStack.clear()
        } else {
            // Same screen with the same record (double tap) - nothing to do
            if (before.screen == screen && currentNavEntry() == before) return
            navStack.addLast(before)
            while (navStack.size > MAX_BACK_STACK) navStack.removeFirst()
        }
        _currentScreen.value = screen
    }

    fun navigateTo(screen: AppScreen) {
        if (screen in AppScreen.ROOT_SCREENS) clearSupplierQueue()
        val tab = AppScreen.masterTabFor(screen)
        when {
            screen == AppScreen.MASTERS -> go(screen) { _mastersCategory.value = null }
            tab != null -> go(screen) { _mastersCategory.value = tab }
            else -> go(screen)
        }
    }

    /**
     * Go back one step. Returns false when already on Home with nothing behind it
     * (the system back button then closes the app).
     */
    fun navigateBack(): Boolean {
        // Leaving the order form ends a phone order queue; orders already saved stay saved
        if (_currentScreen.value == AppScreen.ADD_STOP) clearSupplierQueue()
        val previous = navStack.removeLastOrNull()
        if (previous == null) {
            if (_currentScreen.value == AppScreen.DASHBOARD) return false
            _currentScreen.value = AppScreen.DASHBOARD
            return true
        }
        _selectedVisit.value = previous.visit
        previous.visit?.let { observeVisitData(it.id) }
        _selectedCustomer.value = previous.customer
        _selectedSupplier.value = previous.supplier
        _selectedEmployeeDetail.value = previous.employee
        _selectedProduct.value = previous.product
        _selectedBrand.value = previous.brand
        _selectedTransporter.value = previous.transporter
        _selectedMarket.value = previous.market
        _selectedPurchaseEntry.value = previous.entry
        _selectedSupplierForCopy.value = previous.supplierForCopy
        _selectedCustomerForReport.value = previous.customerForReport
        _mastersCategory.value = previous.mastersCategory
        _currentScreen.value = previous.screen
        return true
    }

    fun resetNavigation() {
        navStack.clear()
        clearSupplierQueue()
        _mastersCategory.value = null
        _currentScreen.value = AppScreen.DASHBOARD
    }

    fun openVisitDetail(visit: VisitEntity, @Suppress("UNUSED_PARAMETER") returnScreen: AppScreen? = null) =
        go(AppScreen.VISIT_DETAIL) {
            // Trips opened right after a join / status change come straight from Room: show the brand name
            _selectedVisit.value = visit.withBrandName(allCustomers.value.associateBy { it.id })
            observeVisitData(visit.id)
        }

    private var visitDataJob: Job? = null
    private var observedVisitId: Long? = null

    /** Live entries, pack groups and the trip itself (members can join from other phones). */
    private fun observeVisitData(visitId: Long) {
        if (observedVisitId == visitId && visitDataJob?.isActive == true) return
        visitDataJob?.cancel()
        if (observedVisitId != visitId) {
            _visitEntries.value = emptyList()
            _visitPackGroups.value = emptyList()
        }
        observedVisitId = visitId
        visitDataJob = viewModelScope.launch {
            launch {
                repository.getEntriesByVisit(visitId).collect { entries ->
                    _visitEntries.value = entries.filter { !it.isDeleted }
                }
            }
            launch {
                repository.getPackGroupsByVisit(visitId).collect { groups ->
                    _visitPackGroups.value = groups
                }
            }
            launch {
                combine(repository.getVisitFlowById(visitId), repository.allCustomers) { fresh, customers ->
                    fresh?.withBrandName(customers.associateBy { it.id })
                }.collect { fresh ->
                    if (fresh != null && !fresh.isDeleted && _selectedVisit.value?.id == visitId) {
                        _selectedVisit.value = fresh
                    }
                }
            }
        }
    }

    fun openAddStop(visit: VisitEntity) {
        if (blockIfAgent("Adding an order")) return
        val latest = _selectedVisit.value?.takeIf { it.id == visit.id } ?: visit
        if (latest.isClosed()) {
            toast("This trip is closed. Reopen it to add orders.")
            return
        }
        go(AppScreen.ADD_STOP) {
            _selectedVisit.value = visit
            observeVisitData(visit.id)
        }
    }

    fun openCustomerReport(visit: VisitEntity) = go(AppScreen.CUSTOMER_REPORT_VIEW) {
        _selectedVisit.value = visit
        observeVisitData(visit.id)
    }

    fun openSupplierCopy(visit: VisitEntity, supplier: SupplierEntity) = go(AppScreen.SUPPLIER_COPY_VIEW) {
        _selectedVisit.value = visit
        _selectedSupplierForCopy.value = supplier
        observeVisitData(visit.id)
    }

    fun openCustomerDetail(customer: CustomerEntity) = go(AppScreen.CUSTOMER_DETAIL) {
        _selectedCustomer.value = customer
    }

    fun openCustomerOrdersReport(customer: CustomerEntity? = null) = go(AppScreen.CUSTOMER_ORDERS_REPORT) {
        _selectedCustomerForReport.value = customer
    }

    /** Orders tab, optionally preselecting a delivery status chip ("Pending", "Delivered", ...). */
    fun openOrders(statusFilter: String = "All") {
        _ordersStatusFilter.value = statusFilter
        navigateTo(AppScreen.PURCHASE_ORDERS)
    }

    fun openPurchaseOrders() = openOrders()

    fun openSupplierDetail(supplier: SupplierEntity) = go(AppScreen.SUPPLIER_DETAIL) {
        _selectedSupplier.value = supplier
    }

    /** Staff open the staff profile, Sub Agents open the sub agent profile. */
    fun openEmployeeDetail(employee: EmployeeEntity) =
        go(if (Roles.isAgent(employee.role)) AppScreen.SUB_AGENT_DETAIL else AppScreen.EMPLOYEE_DETAIL) {
            _selectedEmployeeDetail.value = employee
        }

    fun openSubAgentDetail(agent: EmployeeEntity) = go(AppScreen.SUB_AGENT_DETAIL) {
        _selectedEmployeeDetail.value = agent
    }

    fun openProductDetail(product: ProductEntity) = go(AppScreen.PRODUCT_DETAIL) {
        _selectedProduct.value = product
    }

    fun openBrandDetail(brand: BrandEntity) = go(AppScreen.BRAND_DETAIL) {
        _selectedBrand.value = brand
    }

    fun openTransporterDetail(transporter: TransporterEntity) = go(AppScreen.TRANSPORTER_DETAIL) {
        _selectedTransporter.value = transporter
    }

    fun openMarketDetail(market: MarketEntity) = go(AppScreen.MARKET_DETAIL) {
        _selectedMarket.value = market
    }

    fun openOrderDetail(entry: PurchaseEntryEntity, @Suppress("UNUSED_PARAMETER") returnScreen: AppScreen? = null) =
        go(AppScreen.ORDER_DETAIL) {
            _selectedPurchaseEntry.value = entry
            val visit = allVisits.value.find { it.id == entry.visitId }
            _selectedVisit.value = visit
            if (visit != null) {
                observeVisitData(visit.id)
            }
            val supplier = allSuppliers.value.find {
                it.id == entry.supplierId ||
                    (it.name.isNotBlank() && it.name.trim().equals(entry.supplierName.trim(), ignoreCase = true)) ||
                    (it.firmName.isNotBlank() && it.firmName.trim().equals(entry.supplierName.trim(), ignoreCase = true))
            }
            _selectedSupplierForCopy.value = supplier
        }

    /** Open one master list (Customers, Suppliers, ...) from the Masters hub or a shortcut. */
    fun openMasterList(tab: MasterTab) = go(AppScreen.masterScreenFor(tab)) {
        _mastersCategory.value = tab
    }

    fun openSubAgents() = go(AppScreen.SUB_AGENT_MASTER)

    private fun isAdminNow(): Boolean = _isSuperAdmin.value || Roles.isAdmin(_currentRole.value)

    private fun openMasterForm(tab: MasterTab, screen: AppScreen = AppScreen.ADD_EDIT_MASTER, setup: () -> Unit = {}) =
        go(screen) {
            _activeMasterTab.value = tab
            _editingCustomer.value = null
            _editingSupplier.value = null
            _editingProduct.value = null
            _editingEmployee.value = null
            _editingBrand.value = null
            _editingTransporter.value = null
            _editingMarket.value = null
            setup()
        }

    fun openAddMaster(tab: MasterTab) {
        if (blockIfAgent("Adding records")) return
        if (tab == MasterTab.EMPLOYEES && !isAdminNow()) {
            toast("Only Admins can add new staff")
            return
        }
        openMasterForm(tab)
    }

    fun openEditCustomer(customer: CustomerEntity) {
        if (blockIfAgent("Editing a customer")) return
        openMasterForm(MasterTab.CUSTOMERS) { _editingCustomer.value = customer }
    }

    fun openEditSupplier(supplier: SupplierEntity) {
        if (blockIfAgent("Editing a supplier")) return
        openMasterForm(MasterTab.SUPPLIERS) { _editingSupplier.value = supplier }
    }

    fun openEditBrand(brand: BrandEntity) {
        if (blockIfAgent("Editing a brand")) return
        openMasterForm(MasterTab.BRANDS) { _editingBrand.value = brand }
    }

    fun openEditTransporter(transporter: TransporterEntity) {
        if (blockIfAgent("Editing a transporter")) return
        openMasterForm(MasterTab.TRANSPORTERS) { _editingTransporter.value = transporter }
    }

    fun openEditMarket(market: MarketEntity) {
        if (blockIfAgent("Editing a market")) return
        openMasterForm(MasterTab.MARKETS) { _editingMarket.value = market }
    }

    fun openEditProduct(product: ProductEntity) {
        if (blockIfAgent("Editing a product")) return
        openMasterForm(MasterTab.PRODUCTS) { _editingProduct.value = product }
    }

    fun openEditEmployee(employee: EmployeeEntity) {
        if (Roles.isAgent(employee.role)) {
            openEditSubAgent(employee)
            return
        }
        if (!isAdminNow()) {
            toast("Only Admins can edit staff profiles")
            return
        }
        openMasterForm(MasterTab.EMPLOYEES) { _editingEmployee.value = employee }
    }

    // Sub Agent master (stored in the employees node with role = "Agent")
    fun openAddSubAgent() {
        if (blockIfAgent("Adding a sub agent")) return
        openMasterForm(MasterTab.EMPLOYEES, AppScreen.SUB_AGENT_FORM)
    }

    fun openEditSubAgent(agent: EmployeeEntity) {
        if (blockIfAgent("Editing a sub agent")) return
        openMasterForm(MasterTab.EMPLOYEES, AppScreen.SUB_AGENT_FORM) { _editingEmployee.value = agent }
    }

    /** Staff and admins can register sub agents; only admins may give them a login email. */
    fun saveSubAgent(agent: EmployeeEntity, onSaved: ((EmployeeEntity) -> Unit)? = null) {
        if (blockIfAgent("Saving a sub agent")) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = if (agent.id != 0L) repository.getEmployeeById(agent.id) else null
            val loginEmail = if (isAdminNow()) agent.email.trim().lowercase() else existing?.email.orEmpty()
            val toSave = agent.copy(
                role = Roles.AGENT,
                name = agent.name.trim(),
                email = loginEmail,
                status = agent.status.ifBlank { "Active" }
            )
            val id = repository.saveEmployee(toSave)
            val saved = if (toSave.id == 0L) toSave.copy(id = id) else toSave
            rtdbService.syncEmployee(saved)
            launch(Dispatchers.Main) {
                if (_selectedEmployeeDetail.value?.id == saved.id) _selectedEmployeeDetail.value = saved
                onSaved?.invoke(saved)
            }
        }
    }

    /** Soft deactivation keeps every customer link and report intact. */
    fun deactivateSubAgent(agent: EmployeeEntity, onDone: (() -> Unit)? = null) {
        if (blockIfAgent("Removing a sub agent")) return
        if (!isAdminNow()) {
            toast("Only Admins can remove sub agents")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val updated = agent.copy(status = "Deactivated", isBlocked = true, blockedAt = System.currentTimeMillis())
            repository.saveEmployee(updated)
            rtdbService.syncEmployee(updated)
            launch(Dispatchers.Main) { onDone?.invoke() }
        }
    }

    fun reactivateSubAgent(agent: EmployeeEntity) {
        if (!isAdminNow()) return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = agent.copy(status = "Active", isBlocked = false, reactivatedAt = System.currentTimeMillis())
            repository.saveEmployee(updated)
            rtdbService.syncEmployee(updated)
            launch(Dispatchers.Main) {
                if (_selectedEmployeeDetail.value?.id == updated.id) _selectedEmployeeDetail.value = updated
            }
        }
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
            rtdbService.fetchDeletionRequestsKeys()
            // "The cloud is empty" may only be concluded while we are actually connected. Offline,
            // the SDK answers every read from its own cache, so a cold cache reads as an empty
            // database — and acting on that used to seed sample records into the real book and flag
            // every trip and order on the phone as unsent.
            val connected = rtdbService.isConnected()
            _isOnline.value = connected
            val isCloudEmpty = connected && rtdbService.isCloudEmpty()
            if (!connected) {
                // Nothing to conclude and nothing to fetch: the listeners already serve the cache,
                // and whatever this phone is holding goes up on the next successful upload.
                return
            }
            if (isCloudEmpty) {
                // Cloud is genuinely empty: seed SampleData only if local database is also empty
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

                val leads = rtdbService.fetchLeads()
                repository.syncLeadsFromCloud(leads)

                val cheques = rtdbService.fetchChequesPdc()
                repository.syncChequesFromCloud(cheques)

                val reqs = rtdbService.fetchRegistrationRequests()
                _registrationRequests.value = reqs

                val supplierReqs = rtdbService.fetchSupplierRegistrationRequests()
                _supplierRegistrationRequests.value = supplierReqs
            }

            // Collapse records saved twice. Only an admin's phone may write that decision to the
            // shared book; a staff phone just tidies its own lists.
            repository.deduplicateDatabase(rtdbService, allowCloudWrite = isAdminNow())
        } catch (_: Exception) {
            // Offline fallback - local Room DB serves existing records
        } finally {
            _isCloudSyncing.value = false
        }
    }

    // Explicit Pull-to-Refresh & Sync
    fun refreshAllData(onFinished: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            syncCloudToLocal()
            launch(Dispatchers.Main) {
                onFinished?.invoke()
            }
        }
    }

    // Full Master Cloud Synchronization
    fun syncAllLocalMastersToCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Sync all customers
                val customers = repository.allCustomers.first()
                customers.filter { it.id > 0L && !it.isDeleted }.forEach { rtdbService.syncCustomer(it) }

                // Sync all suppliers & manufacturers
                val suppliers = repository.allSuppliers.first()
                suppliers.filter { it.id > 0L && !it.isDeleted }.forEach { rtdbService.syncSupplier(it) }

                // Sync all brands
                val brands = repository.allBrands.first()
                brands.filter { it.id > 0L && !it.isDeleted }.forEach { rtdbService.syncBrand(it) }

                // Sync all transporters
                val transporters = repository.allTransporters.first()
                transporters.filter { it.id > 0L && !it.isDeleted }.forEach { rtdbService.syncTransporter(it) }

                // Sync all markets
                val markets = repository.allMarkets.first()
                markets.filter { it.id > 0L && !it.isDeleted }.forEach { rtdbService.syncMarket(it) }

                // Sync all products
                val products = repository.allProducts.first()
                products.filter { it.id > 0L && !it.isDeleted }.forEach { rtdbService.syncProduct(it) }

                // Sync all employees
                val employees = repository.allEmployees.first()
                employees.filter { it.id > 0L && !it.isDeleted }.forEach { rtdbService.syncEmployee(it) }

                // Sync all pack groups
                val packGroups = repository.allPackGroups.first()
                packGroups.filter { it.id > 0L }.forEach { rtdbService.syncPackGroup(it) }

                // Sync all visits
                val visits = repository.allVisits.first()
                visits.filter { it.id > 0L && !it.isDeleted }.forEach { pushVisit(it) }

                // Sync all purchase entries
                val entries = repository.allEntries.first()
                entries.filter { it.id > 0L && !it.isDeleted }.forEach { pushEntry(it) }

                // Sync all transactions
                val transactions = repository.allTransactions.first()
                transactions.filter { it.id > 0L }.forEach { rtdbService.syncTransaction(it) }

                // Sync all leads
                val leads = repository.allLeads.first()
                leads.filter { it.leadId.isNotBlank() && !it.isDeleted }.forEach { rtdbService.syncLead(it) }
            } catch (_: Exception) {
            }
        }
    }

    // Lead Operations
    fun saveLead(lead: LeadEntity, onComplete: () -> Unit = {}) {
        if (blockIfAgent("Saving a lead")) return
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val emp = currentEmployee.value
            val finalLeadId = lead.leadId.ifBlank { "lead_${System.currentTimeMillis()}" }
            val leadWithMeta = lead.copy(
                leadId = finalLeadId,
                createdByUid = lead.createdByUid.ifBlank { user?.uid ?: "" },
                createdByName = lead.createdByName.ifBlank { emp?.name ?: user?.displayName ?: "Staff" }
            )
            val generatedId = repository.saveLead(leadWithMeta)
            val toSync = if (leadWithMeta.id == 0L) leadWithMeta.copy(id = generatedId) else leadWithMeta
            rtdbService.syncLead(toSync)
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun deleteLead(lead: LeadEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLead(lead)
            if (lead.leadId.isNotBlank()) {
                rtdbService.deleteLead(lead.leadId)
            }
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun convertLeadToCustomer(lead: LeadEntity, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            // Globally unique id (max + 1 collided when two people saved at the same time)
            val nextId = IdGenerator.newId()
            val displayCode = "CUST-${SimpleDateFormat("yyMMdd", Locale.US).format(Date())}-${(100..999).random()}"

            val newCustomer = CustomerEntity(
                id = nextId,
                customerId = displayCode,
                firmName = lead.firmName.ifBlank { lead.name },
                name = lead.name.ifBlank { lead.firmName },
                phone = lead.phone,
                phone2 = lead.phone2,
                address = lead.meetingPlace,
                shopAddress = lead.meetingPlace,
                city = lead.city.ifBlank { "Ahmedabad" },
                state = lead.state.ifBlank { "Gujarat" },
                notes = (if (lead.notes.isNotBlank()) "Notes: ${lead.notes}\n" else "") +
                        "Converted from Lead (Met at: ${lead.meetingPlace})",
                customerType = "Cash",
                creditDays = 30
            )
            repository.saveCustomer(newCustomer)
            rtdbService.syncCustomer(newCustomer)

            val updatedLead = lead.copy(
                status = "Converted",
                convertedAt = System.currentTimeMillis(),
                convertedTargetId = nextId
            )
            repository.saveLead(updatedLead)
            rtdbService.syncLead(updatedLead)

            launch(Dispatchers.Main) {
                onComplete(nextId)
            }
        }
    }

    fun convertLeadToSupplier(lead: LeadEntity, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextId = IdGenerator.newId()
            val displayCode = "SUP-${SimpleDateFormat("yyMMdd", Locale.US).format(Date())}-${(100..999).random()}"

            val newSupplier = SupplierEntity(
                id = nextId,
                supplierId = displayCode,
                firmName = lead.firmName.ifBlank { lead.name },
                name = lead.name.ifBlank { lead.firmName },
                type = if (lead.supplierType.equals("Wholesaler", ignoreCase = true)) "Wholesaler" else "Manufacturer",
                brand = lead.firmName.ifBlank { lead.name },
                phone = lead.phone,
                phone2 = lead.phone2,
                address = lead.meetingPlace,
                officeAddress = lead.meetingPlace,
                city = lead.city.ifBlank { "Ahmedabad" },
                notes = (if (lead.notes.isNotBlank()) "Notes: ${lead.notes}\n" else "") +
                        "Converted from Supplier Lead (Met at: ${lead.meetingPlace}, State: ${lead.state.ifBlank { "Gujarat" }})",
                createdAt = System.currentTimeMillis()
            )
            repository.saveSupplier(newSupplier)
            rtdbService.syncSupplier(newSupplier)

            val updatedLead = lead.copy(
                status = "Converted",
                convertedAt = System.currentTimeMillis(),
                convertedTargetId = nextId
            )
            repository.saveLead(updatedLead)
            rtdbService.syncLead(updatedLead)

            launch(Dispatchers.Main) {
                onComplete(nextId)
            }
        }
    }

    // Cheque PDC Operations
    fun saveChequePdc(
        id: Long = 0L,
        chequeNo: String,
        bankName: String,
        amount: Double,
        chequeDate: String,
        partyType: String,
        partyId: Long,
        partyName: String,
        status: String = "Pending",
        notes: String = "",
        photoUri: String = "",
        onComplete: () -> Unit = {}
    ) {
        if (blockIfAgent("Saving a cheque")) return
        viewModelScope.launch(Dispatchers.IO) {
            val effectiveId = if (id == 0L) System.currentTimeMillis() else id
            val cheque = ChequePdcEntity(
                id = effectiveId,
                chequeNo = chequeNo.trim(),
                bankName = bankName.trim(),
                amount = amount,
                chequeDate = chequeDate.trim(),
                partyType = partyType,
                partyId = partyId,
                partyName = partyName.trim(),
                status = status,
                notes = notes.trim(),
                photoUri = photoUri,
                createdAt = if (id == 0L) System.currentTimeMillis() else 0L
            )
            repository.saveChequePdc(cheque)
            rtdbService.syncChequePdc(cheque)
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun updateChequePdcStatus(id: Long, newStatus: String, clearedDate: String = "", onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val finalClearedDate = if (newStatus.equals("Cleared", ignoreCase = true) && clearedDate.isBlank()) today else clearedDate
            val depositDate = if (newStatus.equals("Deposited", ignoreCase = true)) today else ""
            repository.updateChequePdcStatus(id, newStatus, finalClearedDate)
            val existing = repository.getChequeById(id)
            if (existing != null) {
                val updated = existing.copy(
                    status = newStatus,
                    clearedDate = finalClearedDate,
                    depositDate = if (depositDate.isNotBlank()) depositDate else existing.depositDate
                )
                rtdbService.syncChequePdc(updated)
            }
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun deleteChequePdc(id: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val emp = currentEmployee.value
            val deletedBy = emp?.name ?: user?.displayName ?: "User"
            val email = user?.email ?: ""
            val role = currentRole.value
            val existing = repository.getChequeById(id)
            if (existing != null) {
                repository.deleteChequePdcById(id)
                rtdbService.deleteChequePdc(id)
            }
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // Customer Registration Requests Operations
    fun approveRegistrationRequest(
        request: CustomerRegistrationRequestEntity,
        adminReligion: String,
        creditType: String = "Cash",
        creditDays: Int = 30,
        creditLimit: Double = 0.0,
        assignedAgentId: Long? = null,
        assignedAgentName: String = "",
        onComplete: (Long) -> Unit = {}
    ) {
        if (blockIfAgent("Approving requests")) return
        viewModelScope.launch(Dispatchers.IO) {
            // Globally unique id (max + 1 collided when two people saved at the same time)
            val nextId = IdGenerator.newId()
            val displayCode = "CUST-${SimpleDateFormat("yyMMdd", Locale.US).format(Date())}-${(100..999).random()}"

            val notesList = mutableListOf<String>()
            if (request.bankName.isNotBlank()) {
                notesList.add("Bank: ${request.bankName} | A/C: ${request.accountNumber} | IFSC: ${request.ifscCode}")
            }
            if (request.notes.isNotBlank()) {
                notesList.add("Request Note: ${request.notes}")
            }
            notesList.add("Approved via Android App on ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}")

            // Registered through a Sub Agent's personal link -> keep that link on the customer
            val linkedAgent = request.subAgentId?.let { id -> allPeople.value.firstOrNull { it.id == id && Roles.isAgent(it.role) } }
            val newCustomer = CustomerEntity(
                id = nextId,
                customerId = displayCode,
                subAgentId = linkedAgent?.id ?: request.subAgentId,
                subAgentName = linkedAgent?.name ?: request.subAgentName,
                firmName = request.firmName.ifBlank { request.name },
                name = request.name.ifBlank { request.firmName },
                phone = request.phone,
                phone2 = request.phone2,
                email = request.email,
                address = request.address.ifBlank { request.shopAddress },
                shopAddress = request.shopAddress.ifBlank { request.address },
                marketArea = request.marketArea,
                city = request.city.ifBlank { "Ahmedabad" },
                district = request.district,
                state = request.state.ifBlank { "Gujarat" },
                pincode = request.pincode,
                shopMapLink = request.shopMapLink,
                garmentTypes = request.garmentTypes,
                workingMarkets = request.workingMarkets,
                dob = request.dob,
                gstin = request.gstin,
                panNumber = request.panNumber,
                preferredTransporterName = request.preferredTransporterName,
                transportPreference = request.transportPreference,
                shopPhotoUri = request.shopPhotoUri,
                gstCertPhotoUri = request.gstCertPhotoUri,
                panPhotoUri = request.panPhotoUri,
                aadharPhotoUri = request.aadharPhotoUri,
                aadharBackPhotoUri = request.aadharBackPhotoUri,
                cancelChequePhotoUri = request.cancelChequePhotoUri,
                purchaserPhotoUri = request.purchaserPhotoUri,
                bankName = request.bankName,
                accountNumber = request.accountNumber,
                ifscCode = request.ifscCode,
                religion = adminReligion.trim(),
                customerType = creditType,
                creditDays = creditDays,
                creditLimit = creditLimit,
                addedByAgentId = assignedAgentId,
                addedByAgentName = assignedAgentName,
                notes = notesList.joinToString("\n")
            )

            repository.saveCustomer(newCustomer)
            rtdbService.syncCustomer(newCustomer)

            val adminName = currentEmployee.value?.name ?: "Admin"
            rtdbService.approveRegistrationRequest(
                requestId = request.id,
                newCustomerId = nextId,
                religion = adminReligion.trim(),
                creditType = creditType,
                creditDays = creditDays,
                creditLimit = creditLimit,
                assignedAgentId = assignedAgentId,
                assignedAgentName = assignedAgentName,
                approvedBy = adminName
            )

            val updatedReqs = rtdbService.fetchRegistrationRequests()
            _registrationRequests.value = updatedReqs

            launch(Dispatchers.Main) {
                onComplete(nextId)
            }
        }
    }

    fun rejectRegistrationRequest(
        request: CustomerRegistrationRequestEntity,
        reason: String,
        onComplete: () -> Unit = {}
    ) {
        if (blockIfAgent("Rejecting requests")) return
        viewModelScope.launch(Dispatchers.IO) {
            rtdbService.rejectRegistrationRequest(request.id, reason)
            val updatedReqs = rtdbService.fetchRegistrationRequests()
            _registrationRequests.value = updatedReqs
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // Supplier Registration Requests Operations
    fun approveSupplierRegistrationRequest(
        request: SupplierRegistrationRequestEntity,
        brand: String = "",
        marketName: String = "",
        type: String = "",
        systemMrpValue: String = "",
        systemMrpPercent: String = "",
        systemLessValue: String = "",
        systemLessPercent: String = "",
        createMarketMaster: Boolean = false,
        newMarketCity: String = "",
        onComplete: (Long) -> Unit = {}
    ) {
        if (blockIfAgent("Approving requests")) return
        viewModelScope.launch(Dispatchers.IO) {
            val nextId = IdGenerator.newId()
            val displayCode = "SUP-${SimpleDateFormat("yyMMdd", Locale.US).format(Date())}-${(100..999).random()}"

            val notesList = mutableListOf<String>()
            if (request.bankName.isNotBlank()) {
                notesList.add("Bank: ${request.bankName} | A/C: ${request.accountNumber} | IFSC: ${request.ifscCode}")
            }
            if (request.notes.isNotBlank()) {
                notesList.add("Request Note: ${request.notes}")
            }
            if (request.district.isNotBlank() || request.state.isNotBlank() || request.pincode.isNotBlank()) {
                notesList.add("Location: ${listOf(request.district, request.state, request.pincode).filter { it.isNotBlank() }.joinToString(", ")}")
            }
            notesList.add("Approved via Android App on ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}")

            val finalBrand = brand.ifBlank { request.brand.ifBlank { request.firmName } }
            val finalMarket = marketName.ifBlank { request.marketName.ifBlank { request.marketArea } }
            val finalType = type.ifBlank { request.type.ifBlank { "Manufacturer" } }

            // Link the supplier to the Market master by id (create the market when asked).
            // The public form sends the picked market's id; the admin may still change the name here.
            val allMarketsNow = repository.allMarkets.first().filter { !it.isDeleted }
            var linkedMarket = allMarketsNow.firstOrNull { it.marketName.trim().equals(finalMarket.trim(), ignoreCase = true) }
                ?: request.marketId?.takeIf { marketName.isBlank() }?.let { id -> allMarketsNow.firstOrNull { it.id == id } }
            if (linkedMarket == null && createMarketMaster && finalMarket.isNotBlank()) {
                val newMarket = com.example.data.local.entity.MarketEntity(
                    id = IdGenerator.newId(),
                    marketName = finalMarket.trim(),
                    city = newMarketCity.ifBlank { request.city.ifBlank { "Ahmedabad" } }
                )
                repository.saveMarket(newMarket)
                rtdbService.syncMarket(newMarket)
                linkedMarket = newMarket
            }

            val finalSystemMrpValue = systemMrpValue.ifBlank { request.systemMrpValue }
            val finalSystemMrpPercent = systemMrpPercent.ifBlank { request.systemMrpPercent }
            val finalSystemLessValue = systemLessValue.ifBlank { request.systemLessValue }
            val finalSystemLessPercent = systemLessPercent.ifBlank { request.systemLessPercent }

            val newSupplier = SupplierEntity(
                id = nextId,
                supplierId = displayCode,
                firmName = request.firmName.ifBlank { request.name },
                name = request.name.ifBlank { request.firmName },
                contactPerson = request.contactPerson.ifBlank { request.name },
                type = finalType,
                brand = finalBrand,
                phone = request.phone,
                phone2 = request.phone2,
                email = request.email,
                address = request.address.ifBlank { request.officeAddress },
                officeAddress = request.officeAddress.ifBlank { request.address },
                homeAddress = request.homeAddress.ifBlank { request.address.ifBlank { request.officeAddress } },
                bankName = request.bankName,
                accountNumber = request.accountNumber,
                ifscCode = request.ifscCode,
                marketArea = linkedMarket?.marketName ?: finalMarket,
                marketName = linkedMarket?.marketName ?: finalMarket,
                marketId = linkedMarket?.id,
                markets = linkedMarket?.marketName ?: finalMarket,
                district = request.district,
                state = request.state,
                pincode = request.pincode,
                city = request.city.ifBlank { "Ahmedabad" },
                officeLocation = request.mapLink,
                productsMade = request.productsMade,
                categories = request.categories,
                subCategories = request.subCategories,
                priceRange = request.priceRange,
                gstin = request.gstin,
                panNumber = request.panNumber,
                visitingCardPhotoUri = request.visitingCardPhotoUri,
                shopPhotoUri = request.shopPhotoUri,
                godownPhotoUri = request.godownPhotoUri,
                systemMrpValue = finalSystemMrpValue,
                systemMrpPercent = finalSystemMrpPercent,
                systemLessValue = finalSystemLessValue,
                systemLessPercent = finalSystemLessPercent,
                notes = notesList.joinToString("\n"),
                createdAt = System.currentTimeMillis()
            )

            repository.saveSupplier(newSupplier)
            rtdbService.syncSupplier(newSupplier)

            val adminName = currentEmployee.value?.name ?: "Admin"
            rtdbService.approveSupplierRegistrationRequest(
                requestId = request.id,
                newSupplierId = nextId,
                brand = finalBrand,
                marketName = finalMarket,
                approvedBy = adminName,
                systemMrpValue = finalSystemMrpValue,
                systemMrpPercent = finalSystemMrpPercent,
                systemLessValue = finalSystemLessValue,
                systemLessPercent = finalSystemLessPercent
            )

            val updatedReqs = rtdbService.fetchSupplierRegistrationRequests()
            _supplierRegistrationRequests.value = updatedReqs

            launch(Dispatchers.Main) {
                onComplete(nextId)
            }
        }
    }

    fun rejectSupplierRegistrationRequest(
        request: SupplierRegistrationRequestEntity,
        reason: String,
        onComplete: () -> Unit = {}
    ) {
        if (blockIfAgent("Rejecting requests")) return
        viewModelScope.launch(Dispatchers.IO) {
            rtdbService.rejectSupplierRegistrationRequest(request.id, reason)
            val updatedReqs = rtdbService.fetchSupplierRegistrationRequests()
            _supplierRegistrationRequests.value = updatedReqs
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // Customer Operations
    fun saveCustomer(customer: CustomerEntity, onSaved: ((CustomerEntity) -> Unit)? = null) {
        if (blockIfAgent("Saving a customer")) return
        viewModelScope.launch(Dispatchers.IO) {
            val generatedId = repository.saveCustomer(customer)
            val toSync = if (customer.id == 0L) customer.copy(id = generatedId) else customer
            rtdbService.syncCustomer(toSync)
            onSaved?.let { cb ->
                launch(Dispatchers.Main) {
                    cb(toSync)
                }
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        if (blockIfAgent("Deleting a customer")) return
        startDelete(OrphanScan.forCustomer(customer, orphanData()))
    }

    // Supplier Operations
    fun saveSupplier(
        supplier: SupplierEntity,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        onSaved: ((SupplierEntity) -> Unit)? = null
    ) {
        if (blockIfAgent("Saving a supplier")) return
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
                    onSaved?.invoke(toSync)
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
        if (blockIfAgent("Deleting a supplier")) return
        startDelete(OrphanScan.forSupplier(supplier, orphanData()))
    }

    // Brand Operations
    fun saveBrand(brand: BrandEntity, onSuccess: (() -> Unit)? = null) {
        if (blockIfAgent("Saving a brand")) return
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
        if (blockIfAgent("Deleting a brand")) return
        startDelete(OrphanScan.forBrand(brand, orphanData()))
    }

    // Transporter Operations
    fun saveTransporter(transporter: TransporterEntity, onSuccess: (() -> Unit)? = null) {
        if (blockIfAgent("Saving a transporter")) return
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
        if (blockIfAgent("Deleting a transporter")) return
        startDelete(OrphanScan.forTransporter(transporter, orphanData()))
    }

    // Market Operations
    /** [onSuccess] gets the saved market (with its id), e.g. to link a supplier to a market created inline. */
    fun saveMarket(market: MarketEntity, onSuccess: ((MarketEntity) -> Unit)? = null) {
        if (blockIfAgent("Saving a market")) return
        viewModelScope.launch(Dispatchers.IO) {
            val generatedId = repository.saveMarket(market)
            val toSync = if (market.id == 0L) market.copy(id = generatedId) else market
            rtdbService.syncMarket(toSync)
            launch(Dispatchers.Main) {
                onSuccess?.invoke(toSync)
            }
        }
    }

    fun deleteMarket(market: MarketEntity) {
        if (blockIfAgent("Deleting a market")) return
        startDelete(OrphanScan.forMarket(market, orphanData()))
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
        if (blockIfAgent("Saving a product")) return
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
        if (blockIfAgent("Deleting a product")) return
        startDelete(OrphanScan.forProduct(product, orphanData()))
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
            // isAdminNow() also counts an owner who has no staff record of their own
            if (!isAdminNow()) {
                launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Only Admins can create or edit employee records", Toast.LENGTH_LONG).show()
                }
                return@launch
            }
            // Last line of defence on the staff code. Two admins adding people at the same time, or an
            // old app version still generating a random digit, would otherwise hand out the same code
            // and make two different people look like one in every report.
            val known = everyStaffRecord.value.ifEmpty { allPeople.value }
            val prefix = if (Roles.isAgent(employee.role)) StaffCodes.AGENT_PREFIX else StaffCodes.STAFF_PREFIX
            val wanted = employee.employeeId.trim()
            val code = when {
                wanted.isBlank() -> StaffCodes.next(known, prefix)
                StaffCodes.isTaken(wanted, known, employee.id) -> StaffCodes.next(known, prefix)
                else -> wanted
            }
            val record = if (code == employee.employeeId) employee else employee.copy(employeeId = code)
            val generatedId = repository.saveEmployee(record)
            val toSync = if (record.id == 0L) record.copy(id = generatedId) else record
            rtdbService.syncEmployee(toSync)
            if (code != wanted && wanted.isNotBlank()) {
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        "$wanted was already taken, saved as $code",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun deleteEmployee(employee: EmployeeEntity) {
        // isAdminNow() also counts an owner who has no staff record of their own
        if (!isAdminNow()) {
            toast("Only Admins can delete employee records")
            return
        }
        startDelete(OrphanScan.forStaff(employee, orphanData()))
    }

    // ---------------------------------------------------------------------
    // Role guards & current actor
    // ---------------------------------------------------------------------

    private fun toast(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
        }
    }

    /** Sub Agents have a read-only login. Returns true (and explains) when the action must be blocked. */
    private fun blockIfAgent(action: String): Boolean {
        if (Roles.isAgent(_currentRole.value)) {
            toast("$action is not available for Sub Agent logins")
            return true
        }
        return false
    }

    /** The person using this phone (employee id, name). Owners without a staff record fall back to their Google name. */
    private fun currentActor(): Pair<Long, String> {
        val emp = _currentEmployee.value
        if (emp != null) return emp.id to emp.name
        val user = currentUser.value
        return 0L to (user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email ?: "Admin")
    }

    // Visit Operations
    fun createVisit(
        customer: CustomerEntity,
        employee: EmployeeEntity,
        notes: String,
        secondaryEmployee: EmployeeEntity? = null,
        extraMembers: List<EmployeeEntity> = emptyList(),
        tripType: String = TripTypes.MARKET,
        onCreated: (VisitEntity) -> Unit
    ) {
        if (blockIfAgent("Starting a trip")) return
        viewModelScope.launch(Dispatchers.IO) {
            val now = Date()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            val visitCode = "VIS-${SimpleDateFormat("yyMMdd", Locale.getDefault()).format(now)}-${(100..999).random()}"
            val members = (listOf(employee) + listOfNotNull(secondaryEmployee) + extraMembers)
                .filter { it.id > 0L }
                .distinctBy { it.id }
            // First extra salesman is also written to the legacy co-agent fields so older app versions still see the trip
            val coAgent = members.drop(1).firstOrNull()
            // Never start a trip for an unsaved customer (id 0): its orders would not show in the customer master
            val linkedCustomer = if (customer.id > 0L) customer else {
                val phone = customer.phone.filter { it.isDigit() }.takeLast(10)
                allCustomers.value.firstOrNull { c ->
                    phone.isNotBlank() && c.phone.filter { it.isDigit() }.takeLast(10) == phone &&
                        c.brandName().equals(customer.brandName(), ignoreCase = true)
                } ?: run {
                    val newCustomerId = repository.saveCustomer(customer)
                    val saved = customer.copy(id = newCustomerId)
                    rtdbService.syncCustomer(saved)
                    saved
                }
            }
            var newVisit = VisitEntity(
                visitCode = visitCode,
                customerId = linkedCustomer.id,
                // Trips show the shop / firm (brand) name everywhere
                customerName = linkedCustomer.brandName(),
                employeeId = employee.id,
                employeeName = employee.name,
                secondaryEmployeeId = coAgent?.id ?: 0L,
                secondaryEmployeeName = coAgent?.name ?: "",
                date = dateStr,
                notes = notes,
                tripType = tripType,
                status = "Active"
            )
            members.forEach { newVisit = newVisit.withMember(it.id, it.name) }
            val id = repository.saveVisit(newVisit)
            val created = newVisit.copy(id = id)
            // Stays marked pending until the office confirms it, so no sync pass can drop it
            pushVisit(created)
            announceNewTrip(created)
            launch(Dispatchers.Main) {
                openVisitDetail(created)
                onCreated(created)
            }
        }
    }

    /**
     * A second salesman joins a running trip from their own phone. Their orders are then
     * logged under their name (and printed as salesman on the customer report).
     */
    fun joinVisit(visit: VisitEntity, onJoined: ((VisitEntity) -> Unit)? = null) {
        if (blockIfAgent("Joining a trip")) return
        val emp = _currentEmployee.value
        if (emp == null) {
            toast("Your login is not linked to a staff record, so you cannot join trips")
            return
        }
        if (visit.isClosed()) {
            toast("This trip is already closed")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val updated = ensureTripMember(visit, emp.id, emp.name)
            announceJoin(updated, emp.name)
            launch(Dispatchers.Main) {
                Toast.makeText(getApplication(), "You joined the trip for ${visit.customerName}", Toast.LENGTH_SHORT).show()
                if (_currentScreen.value == AppScreen.VISIT_DETAIL && _selectedVisit.value?.id == visit.id) {
                    // Already on this trip: refresh it in place instead of stacking a second copy
                    _selectedVisit.value = updated.withBrandName(allCustomers.value.associateBy { it.id })
                } else {
                    openVisitDetail(updated)
                }
                onJoined?.invoke(updated)
            }
        }
    }

    /**
     * "New order" from a trip for a staff member who is not on it yet: join first, then open the
     * order form, so the order is saved under their name and the trip shows in their list.
     */
    fun joinAndAddOrder(visit: VisitEntity) {
        if (visit.isClosed()) {
            toast("This trip is closed. Reopen it to add orders.")
            return
        }
        joinVisit(visit) { updated -> openAddStop(updated) }
    }

    // ---------------------------------------------------------------------
    // Phone order: one trip, one order form per supplier the customer named
    // ---------------------------------------------------------------------

    private val _supplierQueue = MutableStateFlow<SupplierQueue?>(null)
    val supplierQueue: StateFlow<SupplierQueue?> = _supplierQueue.asStateFlow()

    private val _closeTripPrompt = MutableStateFlow<CloseTripPromptState?>(null)
    val closeTripPrompt: StateFlow<CloseTripPromptState?> = _closeTripPrompt.asStateFlow()

    /**
     * The customer ordered by phone: save the trip, then walk the order form through the suppliers
     * they named. Each saved order is a normal order, so nothing downstream changes.
     */
    fun startPhoneOrder(
        customer: CustomerEntity,
        salesman: EmployeeEntity,
        others: List<EmployeeEntity>,
        suppliers: List<SupplierEntity>,
        notes: String
    ) {
        if (blockIfAgent("Starting a phone order")) return
        createVisit(
            customer = customer,
            employee = salesman,
            notes = notes,
            extraMembers = others,
            tripType = TripTypes.PHONE
        ) { trip ->
            val queue = SupplierQueue.start(trip.id, suppliers.map { it.id }, salesman.id, salesman.name)
            if (queue == null) {
                toast("Pick at least one supplier for a phone order")
                return@createVisit
            }
            _supplierQueue.value = queue
            // createVisit already opened the trip, so Back from the order form lands on the trip
            openAddStop(trip)
        }
    }

    fun queueSaveAndNext() = stepQueue { it.saveAndNext() }

    fun queueSkip() = stepQueue { it.skip() }

    fun queueSaveAndFinish() = stepQueue { it.saveAndFinish() }

    private fun stepQueue(step: (SupplierQueue) -> SupplierQueue) {
        val queue = _supplierQueue.value ?: return
        val next = step(queue)
        if (next.finished) finishSupplierQueue(next) else _supplierQueue.value = next
    }

    private fun finishSupplierQueue(queue: SupplierQueue) {
        _supplierQueue.value = null
        _closeTripPrompt.value = CloseTripPromptState(queue.tripId, queue.savedCount)
        if (_currentScreen.value == AppScreen.ADD_STOP) navigateBack()
        if (_currentScreen.value != AppScreen.VISIT_DETAIL || _selectedVisit.value?.id != queue.tripId) {
            viewModelScope.launch(Dispatchers.IO) {
                val trip = repository.getVisitById(queue.tripId) ?: return@launch
                launch(Dispatchers.Main) { openVisitDetail(trip) }
            }
        }
    }

    /** Back or leaving the order form ends the queue; orders already saved stay saved. */
    private fun clearSupplierQueue() {
        if (_supplierQueue.value != null) _supplierQueue.value = null
    }

    fun closeTripFromPrompt() {
        val prompt = _closeTripPrompt.value ?: return
        _closeTripPrompt.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val trip = repository.getVisitById(prompt.tripId) ?: return@launch
            launch(Dispatchers.Main) { closeVisit(trip) }
        }
    }

    fun dismissCloseTripPrompt() {
        _closeTripPrompt.value = null
    }

    /** Add another salesman to a trip (e.g. admin or trip starter adds a helper). */
    fun addVisitMember(visit: VisitEntity, member: EmployeeEntity) {
        if (blockIfAgent("Changing a trip")) return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = ensureTripMember(visit, member.id, member.name)
            launch(Dispatchers.Main) {
                if (_selectedVisit.value?.id == visit.id) _selectedVisit.value = updated
            }
        }
    }

    /**
     * Puts a salesman on a trip: atomic on the server (several phones can join at once), saved
     * locally, and queued as a field update when offline. Returns the updated trip.
     */
    private suspend fun ensureTripMember(visit: VisitEntity, memberId: Long, memberName: String): VisitEntity {
        val joinedOnServer = rtdbService.joinVisit(visit.id, memberId, memberName)
        val latest = repository.getVisitById(visit.id) ?: visit
        val updated = latest.withMember(memberId, memberName)
        repository.saveVisit(updated)
        if (!joinedOnServer) {
            // Offline: queue a plain field update, RTDB replays it when back online
            rtdbService.updateVisitFields(
                visit.id,
                mapOf("memberIds" to updated.memberIds, "memberNames" to updated.memberNames)
            )
        }
        return updated
    }

    fun closeVisit(visit: VisitEntity) = updateVisitStatus(visit, "Completed")

    fun reopenVisit(visit: VisitEntity) = updateVisitStatus(visit, "Active")

    fun updateVisitStatus(visit: VisitEntity, newStatus: String) {
        if (blockIfAgent("Changing a trip")) return
        viewModelScope.launch(Dispatchers.IO) {
            val closing = newStatus.equals("Completed", ignoreCase = true)
            val latest = repository.getVisitById(visit.id) ?: visit
            val updated = latest.copy(
                status = newStatus,
                closedAt = if (closing) System.currentTimeMillis() else null,
                closedBy = if (closing) currentActor().second else ""
            )
            repository.saveVisit(updated)
            // Field-level update: never overwrite salesmen who joined from another phone
            rtdbService.updateVisitFields(
                visit.id,
                mapOf("status" to newStatus, "closedAt" to updated.closedAt, "closedBy" to updated.closedBy)
            )
            launch(Dispatchers.Main) {
                if (_selectedVisit.value?.id == visit.id) {
                    _selectedVisit.value = updated
                }
            }
        }
    }

    fun deleteVisit(visit: VisitEntity, onComplete: (() -> Unit)? = null) {
        if (blockIfAgent("Deleting a trip")) return
        // A trip owns its orders, so the warning lists them and offers to remove them together
        startDelete(OrphanScan.forVisit(visit, orphanData()), onDone = onComplete)
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
        caseCount: Int? = null,
        loosePieces: Int? = null,
        gstRate: Double,
        expectedDeliveryDate: String,
        transporter: String,
        paymentStatus: String = "Pending",
        paymentMode: String = "Cash",
        paidAmount: Double = 0.0,
        paymentRemarks: String = "",
        mixedPackNote: String? = null,
        orderFormPhotoUri: String? = null,
        supplierInvoiceUri: String? = null,
        salesmanId: Long = 0,
        salesmanName: String = "",
        orderDate: String = ""
    ) {
        if (blockIfAgent("Adding an order")) return
        viewModelScope.launch(Dispatchers.IO) {
            val seqOrderNo = orderNo ?: nextOrderNumber()
            val totalAmount = pieces * rate
            val gstAmount = (totalAmount * gstRate) / 100.0
            val grandTotal = totalAmount + gstAmount

            val resolvedCaseCount = caseCount ?: if (caseSize > 0) pieces / caseSize else 0
            val resolvedLoosePieces = loosePieces ?: if (caseSize > 0) pieces % caseSize else 0

            val visit = repository.getVisitById(visitId)
            val (actorId, actorName) = currentActor()
            // Salesman credited on reports: explicit choice, else whoever enters it, else the trip starter
            val resolvedSalesmanId = when {
                salesmanId != 0L -> salesmanId
                actorId != 0L -> actorId
                else -> visit?.employeeId ?: 0L
            }
            val resolvedSalesmanName = salesmanName.ifBlank {
                if (actorId != 0L) actorName else visit?.employeeName ?: ""
            }
            val resolvedOrderDate = orderDate.ifBlank { visit?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

            val entry = PurchaseEntryEntity(
                orderNo = seqOrderNo,
                visitId = visitId,
                supplierId = supplier.id,
                supplierName = supplier.name,
                supplierType = supplier.type,
                salesmanId = resolvedSalesmanId,
                salesmanName = resolvedSalesmanName,
                createdById = actorId,
                createdByName = actorName,
                orderDate = resolvedOrderDate,
                itemCode = itemCode.trim().uppercase(Locale.getDefault()),
                pieces = pieces,
                rate = rate,
                totalAmount = totalAmount,
                gstRate = gstRate,
                gstAmount = gstAmount,
                grandTotalWithGst = grandTotal,
                caseSize = caseSize,
                caseCount = resolvedCaseCount,
                loosePieces = resolvedLoosePieces,
                mixedPackNote = mixedPackNote?.trim()?.ifEmpty { null },
                expectedDeliveryDate = expectedDeliveryDate,
                transporter = transporter,
                paymentStatus = paymentStatus,
                paymentMode = paymentMode,
                paidAmount = if (paymentStatus.equals("Received", ignoreCase = true) && paidAmount == 0.0) grandTotal else paidAmount,
                paymentRemarks = paymentRemarks,
                orderFormPhotoUri = orderFormPhotoUri,
                supplierInvoiceUri = supplierInvoiceUri
            )
            val entryId = repository.savePurchaseEntry(entry)
            val savedEntry = entry.copy(id = entryId)
            // Stays marked pending until the office confirms it, so no sync pass can drop it
            pushEntry(savedEntry)
            announceNewOrder(savedEntry, visit)

            // The credited salesman is always on the trip, so it shows in their trips and on the customer report
            if (visit != null && resolvedSalesmanId > 0L && visit.tripMembers().none { it.id == resolvedSalesmanId }) {
                val updatedTrip = ensureTripMember(visit, resolvedSalesmanId, resolvedSalesmanName)
                launch(Dispatchers.Main) {
                    if (_selectedVisit.value?.id == visit.id) {
                        _selectedVisit.value = updatedTrip.withBrandName(allCustomers.value.associateBy { it.id })
                    }
                }
            }

            val txn = TransactionEntity(
                transactionNumber = "TXN-${seqOrderNo.replace("HT-", "")}",
                orderNo = seqOrderNo,
                visitId = visitId,
                customerId = visit?.customerId ?: 0L,
                customerName = allCustomers.value.firstOrNull { it.id == visit?.customerId }?.brandName()
                    ?: visit?.customerName ?: "",
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
                caseCount = resolvedCaseCount,
                loosePieces = resolvedLoosePieces,
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

    /**
     * Next HT-<n> order number. Uses the shared RTDB counter so two phones on the same trip never
     * collide; falls back to this device's highest known number when offline.
     */
    private suspend fun nextOrderNumber(): String {
        val localMax = repository.localMaxOrderSequence()
        val seq = rtdbService.allocateOrderSequence(localMax) ?: (localMax + 1)
        return "HT-$seq"
    }

    fun updatePurchaseEntry(
        entry: PurchaseEntryEntity,
        newPieces: Int,
        newRate: Double,
        newCaseSize: Int,
        newCaseCount: Int? = null,
        newLoosePieces: Int? = null,
        deliveryStatus: String,
        transporter: String,
        expectedDeliveryDate: String,
        paymentStatus: String,
        paymentMode: String,
        paidAmount: Double,
        paymentRemarks: String,
        newMixedPackNote: String? = null,
        orderFormPhotoUri: String? = entry.orderFormPhotoUri,
        supplierInvoiceUri: String? = entry.supplierInvoiceUri,
        newSalesmanId: Long? = null,
        newSalesmanName: String? = null,
        lrNo: String? = null,
        lrDate: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        if (blockIfAgent("Editing an order")) return
        viewModelScope.launch(Dispatchers.IO) {
            val totalAmount = newPieces * newRate
            val gstAmount = (totalAmount * entry.gstRate) / 100.0
            val grandTotal = totalAmount + gstAmount

            val resolvedPaidAmount = when {
                paymentStatus.equals("Received", ignoreCase = true) -> grandTotal
                paymentStatus.equals("Pending", ignoreCase = true) -> 0.0
                else -> paidAmount
            }

            val resolvedCaseCount = newCaseCount ?: entry.caseCount
            val resolvedLoosePieces = newLoosePieces ?: entry.loosePieces
            val resolvedNote = newMixedPackNote?.trim()?.ifEmpty { null } ?: if (newMixedPackNote != null) null else entry.mixedPackNote
            val resolvedPackGroupId = if (resolvedLoosePieces == 0) null else entry.packGroupId

            val toUpdate = entry.copy(
                pieces = newPieces,
                rate = newRate,
                caseSize = newCaseSize,
                caseCount = resolvedCaseCount,
                loosePieces = resolvedLoosePieces,
                packGroupId = resolvedPackGroupId,
                mixedPackNote = resolvedNote,
                deliveryStatus = deliveryStatus,
                transporter = transporter,
                expectedDeliveryDate = expectedDeliveryDate,
                paymentStatus = paymentStatus,
                paymentMode = paymentMode,
                paidAmount = resolvedPaidAmount,
                paymentRemarks = paymentRemarks,
                orderFormPhotoUri = orderFormPhotoUri,
                supplierInvoiceUri = supplierInvoiceUri,
                salesmanId = newSalesmanId ?: entry.salesmanId,
                salesmanName = newSalesmanName?.trim()?.takeIf { it.isNotBlank() } ?: entry.salesmanName,
                lrNo = lrNo?.trim() ?: entry.lrNo,
                lrDate = lrDate?.trim() ?: entry.lrDate
            )

            val updated = repository.updatePurchaseEntryDetails(toUpdate)
            pushEntry(updated)

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
        if (blockIfAgent("Updating payment")) return
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
                pushEntry(updated)
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
        if (blockIfAgent("Deleting an order")) return
        startDelete(OrphanScan.forOrder(entry, orphanData()))
    }

    // ---------------------------------------------------------------------
    // Deleting anything: always show what else is attached first
    //
    // No delete happens straight away. The app works out what would be left orphaned, and if
    // anything would be, it asks. An admin then removes the record for good (optionally with the
    // attached records); anyone else files a request the admin has to confirm.
    // ---------------------------------------------------------------------

    /** A delete waiting on the user's answer, with the full picture of what it would affect. */
    data class PendingDelete(
        val impact: DeleteImpact,
        /** True for an admin: the record really goes. False: it is hidden and sent for confirmation. */
        val hardDelete: Boolean
    )

    private val _pendingDelete = MutableStateFlow<PendingDelete?>(null)
    val pendingDelete: StateFlow<PendingDelete?> = _pendingDelete.asStateFlow()

    private var pendingDeleteOnDone: (() -> Unit)? = null

    fun dismissPendingDelete() {
        _pendingDelete.value = null
        pendingDeleteOnDone = null
    }

    /** Snapshot of everything the orphan scan needs. */
    private fun orphanData() = OrphanScan.Data(
        visits = allVisits.value,
        entries = allEntries.value,
        packGroups = allPackGroups.value,
        customers = allCustomers.value,
        suppliers = allSuppliers.value,
        products = allProducts.value,
        brands = allBrands.value,
        transporters = allTransporters.value,
        markets = allMarkets.value,
        employees = allPeople.value,
        cheques = allChequesPdc.value
    )

    /**
     * Common entry point for every delete. Asks first when something is attached, otherwise goes
     * ahead, because a lone record needs no explanation.
     */
    private fun startDelete(impact: DeleteImpact, onDone: (() -> Unit)? = null) {
        val hard = isAdminNow()
        if (impact.hasImpact) {
            pendingDeleteOnDone = onDone
            _pendingDelete.value = PendingDelete(impact, hard)
        } else {
            performDelete(impact, hard, removeLinked = false, onDone = onDone)
        }
    }

    /** The user answered the warning. [alsoRemoveLinked] deletes the attached records as well. */
    fun confirmPendingDelete(alsoRemoveLinked: Boolean) {
        val pending = _pendingDelete.value ?: return
        val onDone = pendingDeleteOnDone
        _pendingDelete.value = null
        pendingDeleteOnDone = null
        performDelete(pending.impact, pending.hardDelete, alsoRemoveLinked, onDone)
    }

    private fun performDelete(
        impact: DeleteImpact,
        hard: Boolean,
        removeLinked: Boolean,
        onDone: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val actorName = _currentEmployee.value?.name ?: currentActor().second
            val actorEmail = currentUser.value?.email.orEmpty()
            val actorRole = _currentRole.value

            // Children first, so a parent is never removed while something still points at it
            if (removeLinked) {
                impact.removableIdsByKind().forEach { (kind, ids) ->
                    ids.forEach { id ->
                        if (hard) hardDeleteRecord(kind, id) else softDeleteRecord(kind, id, actorName, actorEmail, actorRole)
                    }
                }
            }
            if (hard) hardDeleteRecord(impact.kind, impact.id)
            else softDeleteRecord(impact.kind, impact.id, actorName, actorEmail, actorRole)

            val extra = if (removeLinked && impact.removableCount > 0) {
                " with ${impact.removableCount} linked record${if (impact.removableCount == 1) "" else "s"}"
            } else ""
            val message = if (hard) {
                "${impact.kind.label} deleted$extra"
            } else {
                "${impact.kind.label} sent to the Admin for confirmation$extra"
            }
            launch(Dispatchers.Main) {
                Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
                onDone?.invoke()
            }
        }
    }

    /** Removes one record for good, locally and in the cloud. Admin only, enforced by the caller. */
    private suspend fun hardDeleteRecord(kind: RecordKind, id: Long) {
        when (kind) {
            RecordKind.VISIT -> {
                repository.getVisitById(id)?.let { repository.deleteVisit(it) }
                rtdbService.deleteVisit(id)
            }
            RecordKind.ORDER -> {
                repository.getEntryById(id)?.let { repository.deletePurchaseEntry(it) }
                rtdbService.deletePurchaseEntry(id)
            }
            RecordKind.CUSTOMER -> {
                allCustomers.value.firstOrNull { it.id == id }?.let { repository.deleteCustomer(it) }
                rtdbService.deleteCustomer(id)
            }
            RecordKind.SUPPLIER -> {
                allSuppliers.value.firstOrNull { it.id == id }?.let { repository.deleteSupplier(it) }
                rtdbService.deleteSupplier(id)
            }
            RecordKind.PRODUCT -> {
                allProducts.value.firstOrNull { it.id == id }?.let { repository.deleteProduct(it) }
                rtdbService.deleteProduct(id)
            }
            RecordKind.BRAND -> {
                allBrands.value.firstOrNull { it.id == id }?.let { repository.deleteBrand(it) }
                rtdbService.deleteBrand(id)
            }
            RecordKind.TRANSPORTER -> {
                allTransporters.value.firstOrNull { it.id == id }?.let { repository.deleteTransporter(it) }
                rtdbService.deleteTransporter(id)
            }
            RecordKind.MARKET -> {
                allMarkets.value.firstOrNull { it.id == id }?.let { repository.deleteMarket(it) }
                rtdbService.deleteMarket(id)
            }
            // Staff are deactivated, never erased: the record is what stops a removed login from
            // working, and it keeps their name on the trips and orders they did.
            RecordKind.STAFF -> {
                allPeople.value.firstOrNull { it.id == id }?.let { employee ->
                    repository.saveEmployee(
                        employee.copy(
                            isBlocked = true,
                            isDeleted = true,
                            status = "Deactivated",
                            deletedAt = System.currentTimeMillis(),
                            deletedBy = _currentEmployee.value?.name ?: currentActor().second,
                            deletedByEmail = currentUser.value?.email.orEmpty(),
                            deletedByRole = "Admin",
                            deletionStatus = "CONFIRMED",
                            deletionReason = "Staff account deactivated by Administrator"
                        )
                    )
                }
                rtdbService.deactivateEmployee(
                    employeeId = id,
                    by = _currentEmployee.value?.name ?: currentActor().second,
                    email = currentUser.value?.email.orEmpty()
                )
            }
        }
    }

    /** Hides one record and files a request for the admin. Never loses data. */
    private suspend fun softDeleteRecord(
        kind: RecordKind,
        id: Long,
        actorName: String,
        actorEmail: String,
        actorRole: String
    ) {
        when (kind) {
            RecordKind.VISIT -> repository.getVisitById(id)?.let { visit ->
                repository.saveVisit(visit.markedDeleted(actorName, actorEmail, actorRole))
                rtdbService.softDeleteVisit(visit, actorName, actorEmail, actorRole)
            }
            RecordKind.ORDER -> repository.getEntryById(id)?.let { entry ->
                repository.savePurchaseEntry(entry.markedDeleted(actorName, actorEmail, actorRole))
                rtdbService.softDeletePurchaseEntry(entry, actorName, actorEmail, actorRole)
            }
            RecordKind.CUSTOMER -> allCustomers.value.firstOrNull { it.id == id }?.let { customer ->
                repository.saveCustomer(customer.markedDeleted(actorName, actorEmail, actorRole))
                rtdbService.softDeleteCustomer(customer, actorName, actorEmail, actorRole)
            }
            RecordKind.SUPPLIER -> allSuppliers.value.firstOrNull { it.id == id }?.let { supplier ->
                repository.saveSupplier(supplier.markedDeleted(actorName, actorEmail, actorRole))
                rtdbService.softDeleteSupplier(supplier, actorName, actorEmail, actorRole)
            }
            RecordKind.PRODUCT -> allProducts.value.firstOrNull { it.id == id }?.let { product ->
                repository.saveProduct(product.markedDeleted(actorName, actorEmail, actorRole))
                rtdbService.softDeleteProduct(product, actorName, actorEmail, actorRole)
            }
            RecordKind.BRAND -> allBrands.value.firstOrNull { it.id == id }?.let { brand ->
                repository.saveBrand(brand.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
                rtdbService.softDeleteBrand(brand, actorName, actorEmail, actorRole)
            }
            RecordKind.TRANSPORTER -> allTransporters.value.firstOrNull { it.id == id }?.let { transporter ->
                repository.saveTransporter(transporter.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
                rtdbService.softDeleteTransporter(transporter, actorName, actorEmail, actorRole)
            }
            RecordKind.MARKET -> allMarkets.value.firstOrNull { it.id == id }?.let { market ->
                repository.saveMarket(market.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
                rtdbService.softDeleteMarket(market, actorName, actorEmail, actorRole)
            }
            // Staff records are admin-only anyway; there is no soft path for them
            RecordKind.STAFF -> toast("Only Admins can remove a staff record")
        }
    }

    // ---------------------------------------------------------------------
    // Deletion requests: staff ask, admin approves or rejects
    // ---------------------------------------------------------------------

    private val _deletionRequests = MutableStateFlow<List<DeletionRequest>>(emptyList())
    val deletionRequests: StateFlow<List<DeletionRequest>> = _deletionRequests.asStateFlow()

    val pendingDeletionCount: StateFlow<Int> = _deletionRequests
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _deletionActionBusy = MutableStateFlow<String?>(null)
    val deletionActionBusy: StateFlow<String?> = _deletionActionBusy.asStateFlow()

    /** Admin approved: the record really goes, along with a trip's orders and packs. */
    fun approveDeletionRequest(request: DeletionRequest) {
        if (!isAdminNow()) {
            toast("Only Admins can confirm a deletion")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _deletionActionBusy.value = request.key
            try {
                val linkedEntryIds = if (request.collection == "visits") {
                    allEntries.value.filter { it.visitId == request.itemId }.map { it.id }
                } else emptyList()
                val linkedPackIds = if (request.collection == "visits") {
                    allPackGroups.value.filter { it.visitId == request.itemId }.map { it.id }
                } else emptyList()

                val ok = rtdbService.approveDeletionRequest(request, linkedEntryIds, linkedPackIds)
                if (ok) {
                    // Clear the phone's own copy too, so it does not linger in any list
                    deleteLocalRecord(request.collection, request.itemId, linkedEntryIds)
                    toast("${request.entityLabel} deleted")
                } else {
                    toast("Could not delete right now. Check your connection.")
                }
            } finally {
                _deletionActionBusy.value = null
            }
        }
    }

    /** Admin rejected: the record comes back and the request disappears. */
    fun rejectDeletionRequest(request: DeletionRequest) {
        if (!isAdminNow()) {
            toast("Only Admins can reject a deletion")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _deletionActionBusy.value = request.key
            try {
                val linkedEntryIds = if (request.collection == "visits") {
                    allEntries.value.filter { it.visitId == request.itemId }.map { it.id }
                } else emptyList()
                val ok = rtdbService.rejectDeletionRequest(request, linkedEntryIds)
                if (ok) {
                    restoreLocalRecord(request.collection, request.itemId, linkedEntryIds)
                    toast("${request.entityLabel} restored")
                } else {
                    toast("Could not restore right now. Check your connection.")
                }
            } finally {
                _deletionActionBusy.value = null
            }
        }
    }

    /** Mirrors an approved deletion into this phone's database. */
    private suspend fun deleteLocalRecord(collection: String, id: Long, linkedEntryIds: List<Long>) {
        when (collection) {
            "visits" -> {
                repository.getVisitById(id)?.let { repository.deleteVisit(it) }
                linkedEntryIds.forEach { entryId ->
                    repository.getEntryById(entryId)?.let { repository.deletePurchaseEntry(it) }
                }
            }
            "purchase_entries" -> repository.getEntryById(id)?.let { repository.deletePurchaseEntry(it) }
            "customers" -> allCustomers.value.firstOrNull { it.id == id }?.let { repository.deleteCustomer(it) }
            "suppliers" -> allSuppliers.value.firstOrNull { it.id == id }?.let { repository.deleteSupplier(it) }
            "products" -> allProducts.value.firstOrNull { it.id == id }?.let { repository.deleteProduct(it) }
            "brands" -> allBrands.value.firstOrNull { it.id == id }?.let { repository.deleteBrand(it) }
            "transporters" -> allTransporters.value.firstOrNull { it.id == id }?.let { repository.deleteTransporter(it) }
            "markets" -> allMarkets.value.firstOrNull { it.id == id }?.let { repository.deleteMarket(it) }
            "cheques_pdc" -> repository.deleteChequePdcById(id)
        }
    }

    /** Mirrors a rejected deletion into this phone's database, clearing the flags locally too. */
    private suspend fun restoreLocalRecord(collection: String, id: Long, linkedEntryIds: List<Long>) {
        when (collection) {
            "visits" -> {
                repository.getVisitById(id)?.let { repository.saveVisit(it.cleared()) }
                linkedEntryIds.forEach { entryId ->
                    repository.getEntryById(entryId)?.let { repository.savePurchaseEntry(it.cleared()) }
                }
            }
            "purchase_entries" -> repository.getEntryById(id)?.let { repository.savePurchaseEntry(it.cleared()) }
            "customers" -> allCustomers.value.firstOrNull { it.id == id }?.let { repository.saveCustomer(it.cleared()) }
            "suppliers" -> allSuppliers.value.firstOrNull { it.id == id }?.let { repository.saveSupplier(it.cleared()) }
            "products" -> allProducts.value.firstOrNull { it.id == id }?.let { repository.saveProduct(it.cleared()) }
            "brands" -> allBrands.value.firstOrNull { it.id == id }?.let { repository.saveBrand(it.copy(isDeleted = false, deletedAt = null)) }
            "transporters" -> allTransporters.value.firstOrNull { it.id == id }?.let { repository.saveTransporter(it.copy(isDeleted = false, deletedAt = null)) }
            "markets" -> allMarkets.value.firstOrNull { it.id == id }?.let { repository.saveMarket(it.copy(isDeleted = false, deletedAt = null)) }
        }
    }

    /**
     * Orders whose trip has not arrived on this phone. Reported, never deleted — the old version
     * removed them from the cloud too, which is how whole days of orders vanished.
     */
    private val _orphanEntryCount = MutableStateFlow(0)
    val orphanEntryCount: StateFlow<Int> = _orphanEntryCount.asStateFlow()

    fun refreshOrphanEntryCount() {
        viewModelScope.launch(Dispatchers.IO) {
            _orphanEntryCount.value = repository.findOrphanEntries().size
        }
    }

    // ---------------------------------------------------------------------
    // Telling the team: new trip, someone joined, new order
    // ---------------------------------------------------------------------

    /** Announcements already turned into a phone notification, so a re-delivered snapshot is quiet. */
    private val shownNotificationIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /** When this phone started listening. Older announcements are history, not news. */
    private var notificationsSince = Long.MAX_VALUE

    /** Order ids seen in a cloud snapshot, so an order added on another phone only notifies once. */
    private val knownCloudOrderIds = java.util.Collections.synchronizedSet(mutableSetOf<Long>())
    private var knownCloudOrdersPrimed = false

    /**
     * Puts this device in the push list under the person now signed in. Called once the role is
     * known, because the Cloud Function needs the role to skip Sub Agents and the employee id to
     * skip the person who caused an event.
     */
    private fun publishPushToken() {
        val emp = _currentEmployee.value
        val role = _currentRole.value
        if (Roles.isAgent(role)) {
            viewModelScope.launch(Dispatchers.IO) { FcmTokenRegistrar.unregister(getApplication()) }
            return
        }
        val email = currentUser.value?.email.orEmpty()
        val name = emp?.name ?: currentUser.value?.displayName.orEmpty()
        viewModelScope.launch(Dispatchers.IO) {
            FcmTokenRegistrar.register(
                context = getApplication(),
                employeeId = emp?.id ?: 0L,
                employeeName = name,
                email = email,
                role = role
            )
        }
    }

    private fun postNote(type: String, title: String, body: String, refId: Long) {
        // Sub Agents have a read-only login and only see their own customers; they do not broadcast
        if (Roles.isAgent(_currentRole.value)) return
        val (actorId, actorName) = currentActor()
        viewModelScope.launch(Dispatchers.IO) {
            rtdbService.postNotification(
                WorkNotification(
                    id = "${type}_${refId}_${System.currentTimeMillis()}",
                    type = type,
                    title = title,
                    body = body,
                    actorId = actorId,
                    actorName = actorName,
                    // Lets the Cloud Function skip the sender even when they have no staff record
                    actorEmail = currentUser.value?.email.orEmpty().lowercase(),
                    refId = refId,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun announceNewTrip(visit: VisitEntity) {
        val (_, actorName) = currentActor()
        val kind = if (visit.isPhoneTrip()) "Phone order" else "Trip"
        postNote(
            type = AppNotifications.TYPE_TRIP,
            title = "$kind started: ${visit.customerName.ifBlank { "Customer" }}",
            body = "$actorName started ${visit.visitCode} on ${visit.date}",
            refId = visit.id
        )
    }

    private fun announceJoin(visit: VisitEntity, who: String) {
        postNote(
            type = AppNotifications.TYPE_JOIN,
            title = "$who joined a trip",
            body = "${visit.customerName.ifBlank { "Customer" }} • ${visit.visitCode}",
            refId = visit.id
        )
    }

    private fun announceNewOrder(entry: PurchaseEntryEntity, visit: VisitEntity?) {
        val (_, actorName) = currentActor()
        val customer = visit?.customerName?.takeIf { it.isNotBlank() } ?: "Customer"
        postNote(
            type = AppNotifications.TYPE_ORDER,
            title = "New order ${entry.orderNo}".trim(),
            body = "$customer • ${entry.supplierName} • ${entry.pieces} pcs by $actorName",
            refId = entry.id
        )
    }

    /**
     * Turns announcements from other people into phone notifications. Own work is skipped (you just
     * did it), and anything from before this phone started listening is treated as history.
     */
    private fun handleNotifications(notes: List<WorkNotification>) {
        if (Roles.isAgent(_currentRole.value)) return
        val myId = _currentEmployee.value?.id ?: 0L
        val myName = currentActor().second
        notes.asReversed().forEach { note ->
            if (note.createdAt < notificationsSince) return@forEach
            if (note.id.isBlank() || !shownNotificationIds.add(note.id)) return@forEach
            val mine = (myId > 0L && note.actorId == myId) ||
                (note.actorName.isNotBlank() && note.actorName.equals(myName, ignoreCase = true))
            if (mine) return@forEach
            AppNotifications.show(getApplication(), note.id, note.title, note.body)
        }
    }

    /**
     * An order that appeared in the cloud without a matching announcement (added from the web, or by
     * an older app version) still deserves a notification the first time we see it.
     */
    private fun notifyNewCloudOrders(entries: List<PurchaseEntryEntity>) {
        val live = entries.filter { !it.isDeleted && it.id > 0L }
        if (!knownCloudOrdersPrimed) {
            knownCloudOrderIds.addAll(live.map { it.id })
            knownCloudOrdersPrimed = true
            return
        }
        if (Roles.isAgent(_currentRole.value)) return
        val myId = _currentEmployee.value?.id ?: 0L
        live.forEach { entry ->
            if (!knownCloudOrderIds.add(entry.id)) return@forEach
            val mine = myId > 0L && (entry.createdById == myId || entry.salesmanId == myId)
            if (mine) return@forEach
            val by = entry.createdByName.ifBlank { entry.salesmanName }.ifBlank { "the team" }
            AppNotifications.show(
                getApplication(),
                "cloud_order_${entry.id}",
                "New order ${entry.orderNo}".trim(),
                "${entry.supplierName} • ${entry.pieces} pcs by $by"
            )
        }
    }

    // ---------------------------------------------------------------------
    // Getting local work to the office, and never losing it on the way
    // ---------------------------------------------------------------------

    /** Trips + orders still sitting only on this phone. */
    private val _pendingPushCount = MutableStateFlow(0)
    val pendingPushCount: StateFlow<Int> = _pendingPushCount.asStateFlow()

    /** Live connection to the office, straight from the database's own `.info/connected`. */
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isUploadingPending = MutableStateFlow(false)
    val isUploadingPending: StateFlow<Boolean> = _isUploadingPending.asStateFlow()

    /**
     * One value the header can show. Order matters: being offline or holding unsent work is more
     * important to know about than a sync that is simply in progress.
     *
     * Declared after the flows it combines — a property initialiser can only read fields already
     * constructed above it.
     */
    val syncStatus: StateFlow<SyncStatus> =
        combine(_isOnline, isCloudSyncing, _pendingPushCount, _isUploadingPending) { online, syncing, pending, uploading ->
            when {
                !online -> SyncStatus.Offline(pending)
                uploading -> SyncStatus.Uploading(pending)
                pending > 0 -> SyncStatus.Pending(pending)
                syncing -> SyncStatus.Syncing
                else -> SyncStatus.Synced
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SyncStatus.Syncing)

    fun refreshPendingPushCount() {
        viewModelScope.launch(Dispatchers.IO) {
            _pendingPushCount.value = repository.pendingPushCount()
        }
    }

    /** Save a trip to the office; clears the pending mark only once the server confirms. */
    private suspend fun pushVisit(visit: VisitEntity): Boolean {
        repository.markVisitPending(visit.id)
        val ok = rtdbService.syncVisit(visit)
        if (ok) repository.markVisitPushed(visit.id)
        _pendingPushCount.value = repository.pendingPushCount()
        return ok
    }

    /** Save an order to the office; clears the pending mark only once the server confirms. */
    private suspend fun pushEntry(entry: PurchaseEntryEntity): Boolean {
        repository.markEntryPending(entry.id)
        val ok = rtdbService.syncPurchaseEntry(entry)
        if (ok) repository.markEntryPushed(entry.id)
        _pendingPushCount.value = repository.pendingPushCount()
        return ok
    }

    /**
     * Uploads everything this phone still holds that the office has not confirmed: trips first, so
     * their orders always land against an existing trip. Runs on every sign-in and can be triggered
     * by hand from Profile, which is how data cached on a phone gets recovered.
     */
    fun uploadPendingToCloud(onFinished: ((Int) -> Unit)? = null) {
        if (_isUploadingPending.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isUploadingPending.value = true
            var sent = 0
            try {
                repository.pendingPushVisits().forEach { visit ->
                    if (rtdbService.syncVisit(visit)) {
                        repository.markVisitPushed(visit.id)
                        sent++
                    }
                }
                repository.pendingPushEntries().forEach { entry ->
                    if (rtdbService.syncPurchaseEntry(entry)) {
                        repository.markEntryPushed(entry.id)
                        sent++
                    }
                }
            } catch (e: Exception) {
                Log.w("HimatViewModel", "uploadPendingToCloud failed: ${e.message}")
            } finally {
                _pendingPushCount.value = repository.pendingPushCount()
                _isUploadingPending.value = false
                launch(Dispatchers.Main) { onFinished?.invoke(sent) }
            }
        }
    }

    fun updateDeliveryStatus(
        entry: PurchaseEntryEntity,
        newStatus: String,
        transporter: String,
        lrNo: String? = null,
        lrDate: String? = null
    ) {
        if (blockIfAgent("Updating delivery")) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDeliveryStatus(entry.id, newStatus, transporter)
            if (lrNo != null || lrDate != null) {
                repository.updateLrDetails(entry.id, (lrNo ?: entry.lrNo).trim(), (lrDate ?: entry.lrDate).trim())
            }
            val updated = repository.getEntryById(entry.id)
            if (updated != null) {
                pushEntry(updated)
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
        if (blockIfAgent("Packing")) return
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
                    pushEntry(updatedEntry)
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
        if (blockIfAgent("Unpacking")) return
        viewModelScope.launch(Dispatchers.IO) {
            val entryIds = group.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            repository.deletePackGroup(group)
            rtdbService.deletePackGroup(group.id)
            // RTDB cleanup
            entryIds.forEach { id ->
                val updatedEntry = repository.getEntryById(id)
                if (updatedEntry != null) {
                    pushEntry(updatedEntry)
                }
            }
            launch(Dispatchers.Main) {
                onSuccess?.invoke()
            }
        }
    }

    // Sharing / PDF
    /** The trip's customer; orphaned trips (customerId 0 / removed duplicate) resolve by exact name. */
    private fun customerOfTrip(visit: VisitEntity): CustomerEntity? =
        RelatedLogic.customerOfTrip(visit, allCustomers.value)

    /** Brand (shop / firm) name for titles, falling back to the name stored on the trip. */
    private fun customerBrandOf(visit: VisitEntity, customer: CustomerEntity?): String =
        customer?.brandName()?.takeIf { it.isNotBlank() } ?: visit.customerName

    fun shareCustomerDayReportPdf(
        visit: VisitEntity,
        options: com.example.ui.components.CustomerReportOptions = com.example.ui.components.CustomerReportOptions()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = customerOfTrip(visit)
            val salesman = allEmployees.value.find { it.id == visit.employeeId }
            val entries = visitEntries.value
            val pdfFile = PdfGenerator.generateCustomerDayReport(
                getApplication(),
                visit,
                customer,
                salesman,
                entries,
                options
            )
            launch(Dispatchers.Main) {
                ShareUtil.sharePdfFile(
                    getApplication(),
                    pdfFile,
                    "${customerBrandOf(visit, customer)} - Customer Report ${visit.visitCode} | Himat Textile"
                )
            }
        }
    }

    fun shareCustomerReportWhatsApp(visit: VisitEntity) {
        val customer = customerOfTrip(visit)
        val entries = visitEntries.value
        val text = ShareUtil.buildCustomerReportText(visit, customer, entries)
        ShareUtil.shareWhatsAppText(getApplication(), text)
    }

    fun shareCustomerGstInvoicePdf(visit: VisitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = customerOfTrip(visit)
            val salesman = allEmployees.value.find { it.id == visit.employeeId }
            val entries = visitEntries.value
            val pdfFile = PdfGenerator.generateCustomerGstInvoice(
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
                    "${customerBrandOf(visit, customer)} - GST Invoice ${visit.visitCode} | Himat Textile"
                )
            }
        }
    }

    fun shareCustomerDateRangeReportPdf(
        customer: CustomerEntity,
        startDate: String,
        endDate: String,
        statusFilter: String = "All"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val knownIds = allCustomers.value.map { it.id }.toSet()
            val customerVisits = RelatedLogic.tripsOfCustomer(allVisits.value, customer, knownIds)
            val visitMap = customerVisits.associateBy { it.id }

            val filteredEntries = allEntries.value.filter { entry ->
                val visit = visitMap[entry.visitId]
                val entryDate = if (visit != null && visit.date.isNotBlank()) visit.date else entry.expectedDeliveryDate
                val inDateRange = when {
                    startDate.isNotBlank() && endDate.isNotBlank() -> entryDate in startDate..endDate
                    startDate.isNotBlank() -> entryDate >= startDate
                    endDate.isNotBlank() -> entryDate <= endDate
                    else -> true
                }
                if (!inDateRange) return@filter false

                val matchesStatus = when (statusFilter) {
                    "Dispatched" -> entry.deliveryStatus.equals("Dispatched", ignoreCase = true) || entry.deliveryStatus.equals("Delivered", ignoreCase = true)
                    "Pending" -> !entry.deliveryStatus.equals("Dispatched", ignoreCase = true) && !entry.deliveryStatus.equals("Delivered", ignoreCase = true)
                    else -> true
                }
                matchesStatus
            }.sortedByDescending { it.orderNo }

            val pdfFile = PdfGenerator.generateCustomerDateRangeReport(
                getApplication(),
                customer,
                filteredEntries,
                startDate.ifBlank { "All Time" },
                endDate.ifBlank { "Present" },
                statusFilter
            )
            launch(Dispatchers.Main) {
                ShareUtil.sharePdfFile(
                    getApplication(),
                    pdfFile,
                    "${customer.brandName()} - Statement ($startDate to $endDate) | Himat Textile"
                )
            }
        }
    }

    fun shareSupplierGstInvoicePdf(visit: VisitEntity, supplier: SupplierEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = customerOfTrip(visit)
            val salesman = allEmployees.value.find { it.id == visit.employeeId }
            val supplierEntries = visitEntries.value.filter {
                it.supplierId == supplier.id || (it.supplierName.isNotBlank() && it.supplierName.trim().equals(supplier.name.trim(), ignoreCase = true))
            }
            val pdfFile = PdfGenerator.generateSupplierGstInvoice(
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
                    "${supplier.brandName()} - GST PO ${visit.visitCode} | Himat Textile"
                )
            }
        }
    }

    /** Order form for one supplier of a trip. [options] come from the Order Form PDF sheet. */
    fun shareSupplierCopyPdf(
        visit: VisitEntity,
        supplier: SupplierEntity,
        options: SupplierOrderFormOptions = SupplierOrderFormOptions()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = customerOfTrip(visit)
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
                supplierEntries,
                options
            )
            launch(Dispatchers.Main) {
                ShareUtil.sharePdfFile(
                    getApplication(),
                    pdfFile,
                    "${supplier.brandName()} - Order Form ${visit.visitCode} | Himat Textile"
                )
            }
        }
    }

    fun shareSupplierCopyWhatsApp(visit: VisitEntity, supplier: SupplierEntity) {
        val customer = customerOfTrip(visit)
        val supplierEntries = visitEntries.value.filter {
            it.supplierId == supplier.id || (it.supplierName.isNotBlank() && it.supplierName.trim().equals(supplier.name.trim(), ignoreCase = true))
        }
        val text = ShareUtil.buildSupplierCopyText(visit, supplier, customer, supplierEntries)
        ShareUtil.shareWhatsAppText(getApplication(), text)
    }

    /** Order form for a single order. [options] come from the Order Form PDF sheet. */
    fun shareOrderPdf(entry: PurchaseEntryEntity, options: SupplierOrderFormOptions = SupplierOrderFormOptions()) {
        viewModelScope.launch(Dispatchers.IO) {
            val visit = allVisits.value.find { it.id == entry.visitId } ?: VisitEntity(
                id = entry.visitId,
                date = entry.expectedDeliveryDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
                visitCode = entry.orderNo
            )
            val customer = customerOfTrip(visit)
            val supplier = supplierOfOrder(entry)
            val salesman = allEmployees.value.find { it.id == visit.employeeId }
            val pdfFile = PdfGenerator.generateSupplierCopy(
                getApplication(),
                visit,
                supplier,
                customer,
                salesman,
                listOf(entry),
                options
            )
            launch(Dispatchers.Main) {
                ShareUtil.sharePdfFile(
                    getApplication(),
                    pdfFile,
                    "${supplier.brandName()} - Order Form #${entry.orderNo} | Himat Textile"
                )
            }
        }
    }

    /** The order's supplier record (by id, then name); a stub from the order when it is not in the master. */
    fun supplierOfOrder(entry: PurchaseEntryEntity): SupplierEntity =
        allSuppliers.value.find {
            it.id == entry.supplierId ||
                (it.name.isNotBlank() && it.name.trim().equals(entry.supplierName.trim(), ignoreCase = true)) ||
                (it.firmName.isNotBlank() && it.firmName.trim().equals(entry.supplierName.trim(), ignoreCase = true))
        } ?: SupplierEntity(id = entry.supplierId, name = entry.supplierName, firmName = entry.supplierName, type = entry.supplierType)

    fun shareOrderWhatsApp(entry: PurchaseEntryEntity) {
        val visit = allVisits.value.find { it.id == entry.visitId } ?: VisitEntity(
            id = entry.visitId,
            date = entry.expectedDeliveryDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
            visitCode = entry.orderNo
        )
        val customer = customerOfTrip(visit)
        val supplier = supplierOfOrder(entry)
        val text = ShareUtil.buildSupplierCopyText(visit, supplier, customer, listOf(entry))
        ShareUtil.shareWhatsAppText(getApplication(), text)
    }
}

