package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.HimatTopBar
import com.example.ui.dialogs.AddPurchaseEntryDialog
import com.example.ui.dialogs.CreateVisitDialog
import com.example.ui.dialogs.MixedPackDialog
import com.example.ui.dialogs.RoleSwitcherDialog
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomerReportScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeliveriesScreen
import com.example.ui.screens.EmployeeDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.MastersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SupplierDetailScreen
import com.example.ui.screens.SupplierReportScreen
import com.example.ui.screens.VisitDetailScreen
import com.example.ui.screens.VisitsScreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HimatApp()
            }
        }
    }
}

@Composable
fun HimatApp(viewModel: HimatViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val selectedVisit by viewModel.selectedVisit.collectAsStateWithLifecycle()
    val selectedSupplierForCopy by viewModel.selectedSupplierForCopy.collectAsStateWithLifecycle()

    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val visitEntries by viewModel.visitEntries.collectAsStateWithLifecycle()
    val historyItemCodes by viewModel.distinctItemCodes.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val selectedSupplier by viewModel.selectedSupplier.collectAsStateWithLifecycle()
    val selectedEmployeeDetail by viewModel.selectedEmployeeDetail.collectAsStateWithLifecycle()

    var showCreateVisitDialog by remember { mutableStateOf(false) }
    var showRoleSwitcherDialog by remember { mutableStateOf(false) }
    var showAddPurchaseEntryDialog by remember { mutableStateOf(false) }
    var showMixedPackDialog by remember { mutableStateOf(false) }

    val isDetailOrDocumentScreen = currentScreen in listOf(
        AppScreen.VISIT_DETAIL,
        AppScreen.CUSTOMER_REPORT_VIEW,
        AppScreen.SUPPLIER_COPY_VIEW,
        AppScreen.CUSTOMER_DETAIL,
        AppScreen.SUPPLIER_DETAIL,
        AppScreen.EMPLOYEE_DETAIL
    )

    // Handle Android system back button smoothly
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        when (currentScreen) {
            AppScreen.CUSTOMER_REPORT_VIEW,
            AppScreen.SUPPLIER_COPY_VIEW -> {
                viewModel.navigateTo(AppScreen.VISIT_DETAIL)
            }
            AppScreen.VISIT_DETAIL -> {
                viewModel.navigateTo(AppScreen.VISITS)
            }
            AppScreen.CUSTOMER_DETAIL,
            AppScreen.SUPPLIER_DETAIL,
            AppScreen.EMPLOYEE_DETAIL -> {
                viewModel.navigateTo(AppScreen.CUSTOMER_MASTER)
            }
            else -> {
                viewModel.navigateTo(AppScreen.DASHBOARD)
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isDetailOrDocumentScreen) {
                HimatTopBar(
                    role = currentRole,
                    salesmanName = currentEmployee?.name,
                    onSwitchRole = { showRoleSwitcherDialog = true }
                )
            }
        },
        bottomBar = {
            // Show bottom bar only on primary top-level tabs
            if (!isDetailOrDocumentScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.DASHBOARD,
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", style = MaterialTheme.typography.labelMedium) }
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.VISITS,
                        onClick = { viewModel.navigateTo(AppScreen.VISITS) },
                        icon = { Icon(Icons.Default.Assignment, contentDescription = "Visits") },
                        label = { Text("Visits", style = MaterialTheme.typography.labelMedium) }
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.CUSTOMER_MASTER ||
                                currentScreen == AppScreen.SUPPLIER_MASTER ||
                                currentScreen == AppScreen.EMPLOYEE_MASTER,
                        onClick = { viewModel.navigateTo(AppScreen.CUSTOMER_MASTER) },
                        icon = { Icon(Icons.Default.Storefront, contentDescription = "Masters") },
                        label = { Text("Masters", style = MaterialTheme.typography.labelMedium) }
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.DELIVERIES,
                        onClick = { viewModel.navigateTo(AppScreen.DELIVERIES) },
                        icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Deliveries") },
                        label = { Text("Deliveries", style = MaterialTheme.typography.labelMedium) }
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.REPORTS,
                        onClick = { viewModel.navigateTo(AppScreen.REPORTS) },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                        label = { Text("Reports", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) },
                        onOpenNewVisit = { showCreateVisitDialog = true },
                        onOpenVisit = { viewModel.openVisitDetail(it) },
                        onOpenSupplier = { viewModel.openSupplierDetail(it) },
                        onOpenMixedPack = { showMixedPackDialog = true },
                        onSwitchRole = { showRoleSwitcherDialog = true }
                    )
                }

                AppScreen.SUPPLIER_HUB -> {
                    MainScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) },
                        onOpenNewVisit = { showCreateVisitDialog = true }
                    )
                }

                AppScreen.VISITS -> {
                    VisitsScreen(
                        viewModel = viewModel,
                        onOpenVisit = { viewModel.openVisitDetail(it) },
                        onOpenNewVisit = { showCreateVisitDialog = true }
                    )
                }

                AppScreen.VISIT_DETAIL -> {
                    selectedVisit?.let { visit ->
                        VisitDetailScreen(
                            viewModel = viewModel,
                            visit = visit,
                            onBack = { viewModel.navigateTo(AppScreen.VISITS) },
                            onOpenAddEntry = { showAddPurchaseEntryDialog = true },
                            onOpenMixedPack = { showMixedPackDialog = true },
                            onOpenCustomerReport = { viewModel.openCustomerReport(it) },
                            onOpenSupplierCopy = { v, sup -> viewModel.openSupplierCopy(v, sup) }
                        )
                    } ?: run {
                        viewModel.navigateTo(AppScreen.VISITS)
                    }
                }

                AppScreen.CUSTOMER_REPORT_VIEW -> {
                    selectedVisit?.let { visit ->
                        CustomerReportScreen(
                            viewModel = viewModel,
                            visit = visit,
                            onBack = { viewModel.navigateTo(AppScreen.VISIT_DETAIL) }
                        )
                    } ?: run {
                        viewModel.navigateTo(AppScreen.VISITS)
                    }
                }

                AppScreen.SUPPLIER_COPY_VIEW -> {
                    val visit = selectedVisit
                    val supplier = selectedSupplierForCopy
                    if (visit != null && supplier != null) {
                        SupplierReportScreen(
                            viewModel = viewModel,
                            visit = visit,
                            initialSupplier = supplier,
                            onBack = { viewModel.navigateTo(AppScreen.VISIT_DETAIL) }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.VISITS)
                    }
                }

                AppScreen.CUSTOMER_MASTER,
                AppScreen.SUPPLIER_MASTER,
                AppScreen.EMPLOYEE_MASTER -> {
                    MastersScreen(viewModel = viewModel)
                }

                AppScreen.CUSTOMER_DETAIL -> {
                    val customer = selectedCustomer
                    if (customer != null) {
                        CustomerDetailScreen(
                            viewModel = viewModel,
                            customer = customer,
                            onBack = { viewModel.navigateTo(AppScreen.CUSTOMER_MASTER) },
                            onCreateVisit = {
                                showCreateVisitDialog = true
                            }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.CUSTOMER_MASTER)
                    }
                }

                AppScreen.SUPPLIER_DETAIL -> {
                    val supplier = selectedSupplier
                    if (supplier != null) {
                        SupplierDetailScreen(
                            viewModel = viewModel,
                            supplier = supplier,
                            onBack = { viewModel.navigateTo(AppScreen.SUPPLIER_MASTER) }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.SUPPLIER_MASTER)
                    }
                }

                AppScreen.EMPLOYEE_DETAIL -> {
                    val employee = selectedEmployeeDetail
                    if (employee != null) {
                        EmployeeDetailScreen(
                            viewModel = viewModel,
                            employee = employee,
                            onBack = { viewModel.navigateTo(AppScreen.EMPLOYEE_MASTER) },
                            onOpenVisit = { viewModel.openVisitDetail(it) }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.EMPLOYEE_MASTER)
                    }
                }

                AppScreen.DELIVERIES -> {
                    DeliveriesScreen(viewModel = viewModel)
                }

                AppScreen.REPORTS -> {
                    ReportsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Modal Dialogs
    if (showCreateVisitDialog) {
        CreateVisitDialog(
            customers = customers,
            employees = employees,
            defaultEmployee = currentEmployee,
            onDismiss = { showCreateVisitDialog = false },
            onSave = { customer, employee, notes ->
                viewModel.createVisit(customer, employee, notes) {
                    showCreateVisitDialog = false
                }
            }
        )
    }

    if (showRoleSwitcherDialog) {
        RoleSwitcherDialog(
            currentRole = currentRole,
            currentEmployee = currentEmployee,
            employees = employees,
            onDismiss = { showRoleSwitcherDialog = false },
            onSelectRole = { role, employee ->
                viewModel.setRole(role, employee)
                showRoleSwitcherDialog = false
            }
        )
    }

    if (showAddPurchaseEntryDialog) {
        selectedVisit?.let { visit ->
            AddPurchaseEntryDialog(
                visitId = visit.id,
                suppliers = suppliers,
                historyItemCodes = historyItemCodes,
                onDismiss = { showAddPurchaseEntryDialog = false },
                onOpenMixedPack = {
                    showAddPurchaseEntryDialog = false
                    showMixedPackDialog = true
                },
                onSave = { sup, itemCode, pieces, rate, caseSize, gstRate, expDate, transporter ->
                    viewModel.savePurchaseEntry(
                        orderNo = null,
                        visitId = visit.id,
                        supplier = sup,
                        itemCode = itemCode,
                        pieces = pieces,
                        rate = rate,
                        caseSize = caseSize,
                        gstRate = gstRate,
                        expectedDeliveryDate = expDate,
                        transporter = transporter
                    )
                    showAddPurchaseEntryDialog = false
                }
            )
        }
    }

    if (showMixedPackDialog) {
        selectedVisit?.let { visit ->
            val incompleteEntries = visitEntries.filter { it.loosePieces > 0 }
            MixedPackDialog(
                incompleteEntries = incompleteEntries,
                onDismiss = { showMixedPackDialog = false },
                onPack = { selected, targetCaseSize ->
                    viewModel.createMixedPack(
                        visitId = visit.id,
                        selectedEntries = selected,
                        targetCaseSize = targetCaseSize,
                        onSuccess = {
                            showMixedPackDialog = false
                        }
                    )
                }
            )
        }
    }
}
