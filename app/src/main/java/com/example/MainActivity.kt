package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.HimatTopBar
import com.example.ui.dialogs.CreateVisitDialog
import com.example.ui.dialogs.MixedPackDialog
import com.example.ui.dialogs.RoleSwitcherDialog
import com.example.ui.screens.AddEditMasterScreen
import com.example.ui.screens.AddStopScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.BrandDetailScreen
import com.example.ui.screens.CustomerReportScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeliveriesScreen
import com.example.ui.screens.EmployeeDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeadsScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MarketDetailScreen
import com.example.ui.screens.MastersScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.PendingScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SupplierDetailScreen
import com.example.ui.screens.SupplierReportScreen
import com.example.ui.screens.TransporterDetailScreen
import com.example.ui.screens.VisitDetailScreen
import com.example.ui.screens.VisitsScreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MasterTab
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import com.google.firebase.auth.FirebaseUser
import com.example.ui.viewmodel.HimatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            MyApplicationTheme {
                val viewModel: HimatViewModel = viewModel()
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val isAuthorized by viewModel.isAuthorized.collectAsStateWithLifecycle()
                val authorizationMessage by viewModel.authorizationMessage.collectAsStateWithLifecycle()

                if (currentUser == null) {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = { /* Automatically navigates on auth state update */ }
                    )
                } else if (isAuthorized == false) {
                    AccessRestrictedScreen(
                        currentUser = currentUser!!,
                        authorizationMessage = authorizationMessage,
                        onSignOut = { viewModel.signOut(this@MainActivity) },
                        onRetry = { viewModel.retryAuthorization() }
                    )
                } else if (isAuthorized == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NavyPrimary)
                    }
                } else {
                    HimatApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AccessRestrictedScreen(
    currentUser: FirebaseUser,
    authorizationMessage: String? = null,
    onSignOut: () -> Unit,
    onRetry: () -> Unit
) {
    val isDeactivated = authorizationMessage?.contains("deactivated", ignoreCase = true) == true
    val isSuspended = !isDeactivated && !authorizationMessage.isNullOrBlank()
    val screenTitle = if (isDeactivated) "Account Deactivated" else if (isSuspended) "Access Suspended" else "Access Restricted"
    val accentColor = if (isDeactivated) MaterialTheme.colorScheme.error else if (isSuspended) Color(0xFFD97706) else MaterialTheme.colorScheme.error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Restricted",
                            tint = accentColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = screenTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = authorizationMessage ?: "Your Google account is not authorized to access Himat Textile Agency data.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Signed In As:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentUser.displayName ?: "Google User",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentUser.email ?: "",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSuspended) Color(0xFFFEF3C7) else if (isDeactivated) Color(0xFFFFE4E6) else Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isSuspended) Color(0xFFD97706) else if (isDeactivated) Color(0xFFE11D48) else Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isSuspended) {
                                "Your historical trips, orders, and customer data are 100% safely preserved. Please contact your agency administrator to resume your mobile app access."
                            } else if (isDeactivated) {
                                "All your past trips, orders, and client relationships remain safely preserved in the agency records."
                            } else {
                                "Please ask the Agency Super Admin to register this email in Employee Master to activate your access."
                            },
                            fontSize = 12.sp,
                            color = if (isSuspended) Color(0xFF92400E) else if (isDeactivated) Color(0xFF9F1239) else Color(0xFF92400E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Sign Out", fontSize = 13.sp)
                    }
                }
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
    val visitPackGroups by viewModel.visitPackGroups.collectAsStateWithLifecycle()
    val historyItemCodes by viewModel.distinctItemCodes.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val selectedSupplier by viewModel.selectedSupplier.collectAsStateWithLifecycle()
    val selectedEmployeeDetail by viewModel.selectedEmployeeDetail.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val selectedBrand by viewModel.selectedBrand.collectAsStateWithLifecycle()
    val selectedTransporter by viewModel.selectedTransporter.collectAsStateWithLifecycle()
    val selectedMarket by viewModel.selectedMarket.collectAsStateWithLifecycle()
    val selectedPurchaseEntry by viewModel.selectedPurchaseEntry.collectAsStateWithLifecycle()
    val visitDetailReturnScreen by viewModel.visitDetailReturnScreen.collectAsStateWithLifecycle()
    val orderDetailReturnScreen by viewModel.orderDetailReturnScreen.collectAsStateWithLifecycle()

    var showCreateVisitDialog by remember { mutableStateOf(false) }
    var showRoleSwitcherDialog by remember { mutableStateOf(false) }
    var showMixedPackDialog by remember { mutableStateOf(false) }

    val isDetailOrDocumentScreen = currentScreen in listOf(
        AppScreen.VISIT_DETAIL,
        AppScreen.ADD_STOP,
        AppScreen.CUSTOMER_REPORT_VIEW,
        AppScreen.SUPPLIER_COPY_VIEW,
        AppScreen.CUSTOMER_DETAIL,
        AppScreen.SUPPLIER_DETAIL,
        AppScreen.EMPLOYEE_DETAIL,
        AppScreen.PRODUCT_DETAIL,
        AppScreen.BRAND_DETAIL,
        AppScreen.TRANSPORTER_DETAIL,
        AppScreen.MARKET_DETAIL,
        AppScreen.ORDER_DETAIL,
        AppScreen.ANALYTICS_DASHBOARD,
        AppScreen.ADD_EDIT_MASTER,
        AppScreen.PAYMENTS,
        AppScreen.PENDINGS,
        AppScreen.PROFILE,
        AppScreen.LEADS
    )

    // Screens that display their own integrated flat header
    val screensWithOwnHeader = listOf(
        AppScreen.VISITS,
        AppScreen.CUSTOMER_MASTER,
        AppScreen.SUPPLIER_MASTER,
        AppScreen.EMPLOYEE_MASTER,
        AppScreen.PRODUCT_MASTER,
        AppScreen.BRAND_MASTER,
        AppScreen.TRANSPORTER_MASTER,
        AppScreen.MARKET_MASTER,
        AppScreen.VISIT_DETAIL,
        AppScreen.ADD_STOP,
        AppScreen.CUSTOMER_REPORT_VIEW,
        AppScreen.SUPPLIER_COPY_VIEW,
        AppScreen.CUSTOMER_DETAIL,
        AppScreen.SUPPLIER_DETAIL,
        AppScreen.EMPLOYEE_DETAIL,
        AppScreen.PRODUCT_DETAIL,
        AppScreen.BRAND_DETAIL,
        AppScreen.TRANSPORTER_DETAIL,
        AppScreen.MARKET_DETAIL,
        AppScreen.ORDER_DETAIL,
        AppScreen.ANALYTICS_DASHBOARD,
        AppScreen.ADD_EDIT_MASTER,
        AppScreen.PAYMENTS,
        AppScreen.PENDINGS,
        AppScreen.PROFILE,
        AppScreen.LEADS
    )

    // Handle Android system back button smoothly
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        when (currentScreen) {
            AppScreen.CUSTOMER_REPORT_VIEW,
            AppScreen.SUPPLIER_COPY_VIEW,
            AppScreen.ADD_STOP -> {
                viewModel.navigateTo(AppScreen.VISIT_DETAIL)
            }
            AppScreen.VISIT_DETAIL -> {
                viewModel.navigateTo(visitDetailReturnScreen)
            }
            AppScreen.ORDER_DETAIL -> {
                viewModel.navigateTo(orderDetailReturnScreen)
            }
            AppScreen.PRODUCT_DETAIL -> {
                viewModel.navigateTo(AppScreen.PRODUCT_MASTER)
            }
            AppScreen.BRAND_DETAIL -> {
                viewModel.navigateTo(AppScreen.BRAND_MASTER)
            }
            AppScreen.TRANSPORTER_DETAIL -> {
                viewModel.navigateTo(AppScreen.TRANSPORTER_MASTER)
            }
            AppScreen.MARKET_DETAIL -> {
                viewModel.navigateTo(AppScreen.MARKET_MASTER)
            }
            AppScreen.CUSTOMER_DETAIL -> {
                viewModel.navigateTo(AppScreen.CUSTOMER_MASTER)
            }
            AppScreen.SUPPLIER_DETAIL -> {
                viewModel.navigateTo(AppScreen.SUPPLIER_MASTER)
            }
            AppScreen.EMPLOYEE_DETAIL -> {
                viewModel.navigateTo(AppScreen.EMPLOYEE_MASTER)
            }
            AppScreen.ADD_EDIT_MASTER -> {
                viewModel.navigateTo(AppScreen.CUSTOMER_MASTER)
            }
            AppScreen.ANALYTICS_DASHBOARD,
            AppScreen.PROFILE -> {
                viewModel.navigateTo(AppScreen.DASHBOARD)
            }
            else -> {
                viewModel.navigateTo(AppScreen.DASHBOARD)
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF6F8FB),
        topBar = {
            if (currentScreen !in screensWithOwnHeader) {
                HimatTopBar(
                    role = currentRole,
                    salesmanName = currentEmployee?.name,
                    onOpenProfile = { viewModel.navigateTo(AppScreen.PROFILE) }
                )
            }
        },
        bottomBar = {
            // Show bottom bar only on primary top-level tabs
            if (!isDetailOrDocumentScreen) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    androidx.compose.foundation.layout.Column {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.6.dp
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            val navItemColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NavyPrimary,
                                selectedTextColor = NavyPrimary,
                                indicatorColor = NavyPrimary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home", style = MaterialTheme.typography.labelMedium) },
                                colors = navItemColors
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.VISITS,
                                onClick = { viewModel.navigateTo(AppScreen.VISITS) },
                                icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Visits") },
                                label = { Text("Visits", style = MaterialTheme.typography.labelMedium) },
                                colors = navItemColors
                            )

                            NavigationBarItem(
                                selected = currentScreen in listOf(
                                    AppScreen.CUSTOMER_MASTER,
                                    AppScreen.SUPPLIER_MASTER,
                                    AppScreen.EMPLOYEE_MASTER,
                                    AppScreen.PRODUCT_MASTER,
                                    AppScreen.BRAND_MASTER,
                                    AppScreen.TRANSPORTER_MASTER,
                                    AppScreen.MARKET_MASTER
                                ),
                                onClick = { viewModel.navigateTo(AppScreen.CUSTOMER_MASTER) },
                                icon = { Icon(Icons.Default.Storefront, contentDescription = "Masters") },
                                label = { Text("Masters", style = MaterialTheme.typography.labelMedium) },
                                colors = navItemColors
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.DELIVERIES,
                                onClick = { viewModel.navigateTo(AppScreen.DELIVERIES) },
                                icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Deliveries") },
                                label = { Text("Deliveries", style = MaterialTheme.typography.labelMedium) },
                                colors = navItemColors
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.REPORTS,
                                onClick = { viewModel.navigateTo(AppScreen.REPORTS) },
                                icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                                label = { Text("Reports", style = MaterialTheme.typography.labelMedium) },
                                colors = navItemColors
                            )
                        }
                    }
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
                        onSwitchRole = { viewModel.navigateTo(AppScreen.PROFILE) }
                    )
                }

                AppScreen.ANALYTICS_DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) },
                        onOpenNewVisit = { showCreateVisitDialog = true },
                        onOpenVisit = { viewModel.openVisitDetail(it) },
                        onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
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
                            onBack = { viewModel.navigateTo(visitDetailReturnScreen) },
                            onOpenAddEntry = { viewModel.openAddStop(visit) },
                            onOpenMixedPack = { showMixedPackDialog = true },
                            onOpenCustomerReport = { viewModel.openCustomerReport(it) },
                            onOpenSupplierCopy = { v, sup -> viewModel.openSupplierCopy(v, sup) }
                        )
                    } ?: run {
                        viewModel.navigateTo(visitDetailReturnScreen)
                    }
                }

                AppScreen.ADD_STOP -> {
                    selectedVisit?.let { visit ->
                        AddStopScreen(
                            viewModel = viewModel,
                            visit = visit,
                            onBack = { viewModel.navigateTo(AppScreen.VISIT_DETAIL) },
                            onSaveSuccess = {
                                viewModel.navigateTo(AppScreen.VISIT_DETAIL)
                            },
                            onOpenMixedPack = {
                                viewModel.navigateTo(AppScreen.VISIT_DETAIL)
                                showMixedPackDialog = true
                            }
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
                AppScreen.EMPLOYEE_MASTER,
                AppScreen.PRODUCT_MASTER,
                AppScreen.BRAND_MASTER,
                AppScreen.TRANSPORTER_MASTER,
                AppScreen.MARKET_MASTER -> {
                    val initialTab = when (currentScreen) {
                        AppScreen.SUPPLIER_MASTER -> MasterTab.SUPPLIERS
                        AppScreen.EMPLOYEE_MASTER -> MasterTab.EMPLOYEES
                        AppScreen.PRODUCT_MASTER -> MasterTab.PRODUCTS
                        AppScreen.BRAND_MASTER -> MasterTab.BRANDS
                        AppScreen.TRANSPORTER_MASTER -> MasterTab.TRANSPORTERS
                        AppScreen.MARKET_MASTER -> MasterTab.MARKETS
                        else -> null
                    }
                    MastersScreen(
                        viewModel = viewModel,
                        initialTab = initialTab,
                        onOpenCustomer = { viewModel.openCustomerDetail(it) },
                        onOpenSupplier = { viewModel.openSupplierDetail(it) },
                        onOpenEmployee = { viewModel.openEmployeeDetail(it) },
                        onOpenProduct = { viewModel.openProductDetail(it) },
                        onOpenBrand = { viewModel.openBrandDetail(it) },
                        onOpenTransporter = { viewModel.openTransporterDetail(it) },
                        onOpenMarket = { viewModel.openMarketDetail(it) }
                    )
                }

                AppScreen.ADD_EDIT_MASTER -> {
                    AddEditMasterScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.CUSTOMER_MASTER) }
                    )
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
                            },
                            onOpenVisit = { visit ->
                                viewModel.openVisitDetail(visit, returnScreen = AppScreen.CUSTOMER_DETAIL)
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
                            onBack = { viewModel.navigateTo(AppScreen.SUPPLIER_MASTER) },
                            onOpenVisit = { visit ->
                                viewModel.openVisitDetail(visit, returnScreen = AppScreen.SUPPLIER_DETAIL)
                            },
                            onOpenOrder = { entry ->
                                viewModel.openOrderDetail(entry, returnScreen = AppScreen.SUPPLIER_DETAIL)
                            }
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
                            onOpenVisit = { visit ->
                                viewModel.openVisitDetail(visit, returnScreen = AppScreen.EMPLOYEE_DETAIL)
                            }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.EMPLOYEE_MASTER)
                    }
                }

                AppScreen.PRODUCT_DETAIL -> {
                    val product = selectedProduct
                    if (product != null) {
                        ProductDetailScreen(
                            viewModel = viewModel,
                            product = product,
                            onBack = { viewModel.navigateTo(AppScreen.PRODUCT_MASTER) },
                            onEdit = { viewModel.openEditProduct(product) },
                            onOpenOrder = { entry ->
                                viewModel.openOrderDetail(entry, returnScreen = AppScreen.PRODUCT_DETAIL)
                            }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.PRODUCT_MASTER)
                    }
                }

                AppScreen.BRAND_DETAIL -> {
                    val brand = selectedBrand
                    if (brand != null) {
                        BrandDetailScreen(
                            viewModel = viewModel,
                            brand = brand,
                            onBack = { viewModel.navigateTo(AppScreen.BRAND_MASTER) },
                            onEdit = { viewModel.openEditBrand(brand) },
                            onOpenProduct = { viewModel.openProductDetail(it) },
                            onOpenOrder = { entry ->
                                viewModel.openOrderDetail(entry, returnScreen = AppScreen.BRAND_DETAIL)
                            }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.BRAND_MASTER)
                    }
                }

                AppScreen.TRANSPORTER_DETAIL -> {
                    val transporter = selectedTransporter
                    if (transporter != null) {
                        TransporterDetailScreen(
                            viewModel = viewModel,
                            transporter = transporter,
                            onBack = { viewModel.navigateTo(AppScreen.TRANSPORTER_MASTER) },
                            onEdit = { viewModel.openEditTransporter(transporter) },
                            onOpenOrder = { entry ->
                                viewModel.openOrderDetail(entry, returnScreen = AppScreen.TRANSPORTER_DETAIL)
                            },
                            onOpenCustomer = { viewModel.openCustomerDetail(it) }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.TRANSPORTER_MASTER)
                    }
                }

                AppScreen.MARKET_DETAIL -> {
                    val market = selectedMarket
                    if (market != null) {
                        MarketDetailScreen(
                            viewModel = viewModel,
                            market = market,
                            onBack = { viewModel.navigateTo(AppScreen.MARKET_MASTER) },
                            onEdit = { viewModel.openEditMarket(market) },
                            onOpenCustomer = { viewModel.openCustomerDetail(it) },
                            onOpenSupplier = { viewModel.openSupplierDetail(it) },
                            onOpenOrder = { entry ->
                                viewModel.openOrderDetail(entry, returnScreen = AppScreen.MARKET_DETAIL)
                            },
                            onOpenEmployee = { viewModel.openEmployeeDetail(it) }
                        )
                    } else {
                        viewModel.navigateTo(AppScreen.MARKET_MASTER)
                    }
                }

                AppScreen.ORDER_DETAIL -> {
                    val entry = selectedPurchaseEntry
                    if (entry != null) {
                        OrderDetailScreen(
                            viewModel = viewModel,
                            entry = entry,
                            onBack = { viewModel.navigateTo(orderDetailReturnScreen) },
                            onOpenVisit = { visit ->
                                viewModel.openVisitDetail(visit, returnScreen = orderDetailReturnScreen)
                            }
                        )
                    } else {
                        viewModel.navigateTo(orderDetailReturnScreen)
                    }
                }

                AppScreen.DELIVERIES -> {
                    DeliveriesScreen(viewModel = viewModel)
                }

                AppScreen.REPORTS -> {
                    ReportsScreen(viewModel = viewModel)
                }

                AppScreen.PAYMENTS -> {
                    PaymentsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        onOpenCustomer = { viewModel.openCustomerDetail(it) },
                        onOpenSupplier = { viewModel.openSupplierDetail(it) },
                        onOpenVisit = { viewModel.openVisitDetail(it) }
                    )
                }

                AppScreen.PENDINGS -> {
                    PendingScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        onOpenVisit = { viewModel.openVisitDetail(it) },
                        onOpenCustomer = { viewModel.openCustomerDetail(it) },
                        onOpenSupplier = { viewModel.openSupplierDetail(it) },
                        onOpenMixedPack = { showMixedPackDialog = true }
                    )
                }

                AppScreen.PROFILE -> {
                    ProfileScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
                    )
                }

                AppScreen.LEADS -> {
                    LeadsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
                    )
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
        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
        val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()
        val context = LocalContext.current
        RoleSwitcherDialog(
            currentRole = currentRole,
            currentEmployee = currentEmployee,
            employees = employees,
            currentUser = currentUser,
            isSuperAdmin = isSuperAdmin,
            onDismiss = { showRoleSwitcherDialog = false },
            onSelectRole = { role, employee ->
                viewModel.setRole(role, employee)
                showRoleSwitcherDialog = false
            },
            onSignOut = {
                viewModel.signOut(context)
                showRoleSwitcherDialog = false
            }
        )
    }

    if (showMixedPackDialog) {
        selectedVisit?.let { visit ->
            val packedEntryIds = visitPackGroups.flatMap { group ->
                group.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            }.toSet()
            val incompleteEntries = visitEntries.filter {
                it.loosePieces > 0 && it.packGroupId == null && it.id !in packedEntryIds
            }
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
