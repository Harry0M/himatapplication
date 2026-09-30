package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.HimatTopBar
import com.example.ui.dialogs.NewTripSheet
import com.example.ui.dialogs.MixedPackDialog
import com.example.ui.dialogs.CustomerRequestsDialog
import com.example.ui.dialogs.DeleteImpactDialog
import com.example.ui.dialogs.PermissionsSheet
import com.example.ui.dialogs.RegistrationShareBottomSheet
import com.example.ui.dialogs.RequestsHubSheet
import com.example.ui.dialogs.RoleSwitcherDialog
import com.example.ui.dialogs.SupplierRequestsDialog
import com.example.ui.screens.AddEditMasterScreen
import com.example.ui.screens.AddStopScreen
import com.example.ui.screens.ChequePdcScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.BrandDetailScreen
import com.example.ui.screens.CustomerReportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.CustomerOrderReportScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeletionRequestsScreen
import com.example.ui.screens.DeliveriesScreen
import com.example.ui.screens.EmployeeDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeadsScreen
import com.example.ui.screens.PurchaseOrdersScreen
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import com.example.data.local.entity.CustomerEntity
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.SubAgentDetailScreen
import com.example.ui.screens.SubAgentFormScreen
import com.example.ui.screens.SubAgentsScreen
import com.example.util.AppNotifications
import com.example.util.Roles

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
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

                // Android 13+ asks before we may notify about the team's new trips and orders
                val askNotifications = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* Declined is fine: the app works, it just stays quiet */ }
                LaunchedEffect(currentUser) {
                    if (currentUser != null &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !AppNotifications.canPost(this@MainActivity)
                    ) {
                        askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

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
                    AuthorizingScreen(
                        onRetry = { viewModel.retryAuthorization() },
                        onSignOut = { viewModel.signOut(this@MainActivity) }
                    )
                } else {
                    HimatApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AuthorizingScreen(
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    var showRetry by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(4500L)
        showRetry = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFC),
                        Color(0xFFEEF2F6)
                    )
                )
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.himat_logo),
                        contentDescription = "Himat Textile Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            CircularProgressIndicator(
                color = NavyPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(38.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Verifying Access...",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Connecting to cloud database",
                fontSize = 12.5.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            if (showRetry) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Retry Connection", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onSignOut) {
                    Text("Sign Out", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
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
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.himat_logo),
                            contentDescription = "Himat Textile Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Restricted",
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
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

/** Runs once when a screen has lost the record it needs (e.g. it was deleted): go back. */
@Composable
private fun MissingSelection(viewModel: HimatViewModel) {
    LaunchedEffect(Unit) { viewModel.navigateBack() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HimatApp(viewModel: HimatViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val selectedVisit by viewModel.selectedVisit.collectAsStateWithLifecycle()
    val selectedSupplierForCopy by viewModel.selectedSupplierForCopy.collectAsStateWithLifecycle()

    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val people by viewModel.allPeople.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val brands by viewModel.allBrands.collectAsStateWithLifecycle()
    val transporters by viewModel.allTransporters.collectAsStateWithLifecycle()
    val markets by viewModel.allMarkets.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val visitEntries by viewModel.visitEntries.collectAsStateWithLifecycle()
    val visitPackGroups by viewModel.visitPackGroups.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val selectedSupplier by viewModel.selectedSupplier.collectAsStateWithLifecycle()
    val selectedEmployeeDetail by viewModel.selectedEmployeeDetail.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val selectedBrand by viewModel.selectedBrand.collectAsStateWithLifecycle()
    val selectedTransporter by viewModel.selectedTransporter.collectAsStateWithLifecycle()
    val selectedMarket by viewModel.selectedMarket.collectAsStateWithLifecycle()
    val selectedPurchaseEntry by viewModel.selectedPurchaseEntry.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val supplierQueue by viewModel.supplierQueue.collectAsStateWithLifecycle()

    var showCreateVisitDialog by remember { mutableStateOf(false) }
    var createVisitCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showMixedPackDialog by remember { mutableStateOf(false) }

    // Registration inbox opened from the top bar: both databases in one sheet
    val pendingCustomerRequests by viewModel.pendingRegistrationRequestsCount.collectAsStateWithLifecycle()
    val pendingSupplierRequests by viewModel.pendingSupplierRegistrationRequestsCount.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    var showAccessSheet by remember { mutableStateOf(false) }
    var showRequestsHub by remember { mutableStateOf(false) }
    var showCustomerRequestsFull by remember { mutableStateOf(false) }
    var showSupplierRequestsFull by remember { mutableStateOf(false) }
    var showShareLinkSheet by remember { mutableStateOf(false) }

    val isAgent = Roles.isAgent(currentRole)
    val isTabScreen = currentScreen in AppScreen.ROOT_SCREENS || currentScreen in AppScreen.MASTER_SCREENS

    fun startTrip(customer: CustomerEntity? = null) {
        createVisitCustomer = customer
        showCreateVisitDialog = true
    }

    // One back behaviour everywhere: return to exactly where the user came from
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        viewModel.navigateBack()
    }

    val goBack: () -> Unit = { viewModel.navigateBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (currentScreen == AppScreen.DASHBOARD) {
                HimatTopBar(
                    role = currentRole,
                    salesmanName = currentEmployee?.name,
                    onOpenProfile = { viewModel.navigateTo(AppScreen.PROFILE) },
                    // Sub Agents do not verify registrations, so they do not get the inbox
                    onOpenRequests = if (isAgent) null else ({ showRequestsHub = true }),
                    requestsCount = pendingCustomerRequests + pendingSupplierRequests,
                    syncStatus = syncStatus,
                    onOpenSyncInfo = { showAccessSheet = true },
                    showShare = false
                )
            }
        },
        bottomBar = {
            if (isTabScreen) {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    Column {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.6.dp
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            val navItemColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text("Home") },
                                colors = navItemColors
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.VISITS,
                                onClick = { viewModel.navigateTo(AppScreen.VISITS) },
                                icon = { Icon(Icons.Default.Map, contentDescription = null) },
                                label = { Text("Trips") },
                                colors = navItemColors
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.PURCHASE_ORDERS,
                                onClick = { viewModel.openOrders() },
                                icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
                                label = { Text("Orders") },
                                colors = navItemColors
                            )
                            NavigationBarItem(
                                selected = currentScreen in AppScreen.MASTER_SCREENS,
                                onClick = {
                                    if (isAgent) viewModel.openMasterList(com.example.ui.viewmodel.MasterTab.CUSTOMERS)
                                    else viewModel.navigateTo(AppScreen.MASTERS)
                                },
                                icon = { Icon(if (isAgent) Icons.Default.People else Icons.Default.Storefront, contentDescription = null) },
                                label = { Text(if (isAgent) "Customers" else "Masters") },
                                colors = navItemColors
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.MORE,
                                onClick = { viewModel.navigateTo(AppScreen.MORE) },
                                icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
                                label = { Text("More") },
                                colors = navItemColors
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // consumeWindowInsets: screens with their own Scaffold / top bar no longer add the
        // status and navigation bar space a second time (this caused gaps and cut-off content).
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .consumeWindowInsets(paddingValues)

        val router: @Composable () -> Unit = {
            when (currentScreen) {
                AppScreen.DASHBOARD -> HomeScreen(
                    viewModel = viewModel,
                    onOpenNewVisit = { startTrip() },
                    onOpenVisit = { viewModel.openVisitDetail(it) },
                    onOpenOrder = { viewModel.openOrderDetail(it) },
                    onOpenRequests = { showRequestsHub = true }
                )

                AppScreen.VISITS -> VisitsScreen(
                    viewModel = viewModel,
                    onOpenVisit = { viewModel.openVisitDetail(it) },
                    onOpenNewVisit = { startTrip() }
                )

                AppScreen.PURCHASE_ORDERS -> PurchaseOrdersScreen(
                    viewModel = viewModel,
                    onBack = null,
                    onOpenOrder = { viewModel.openOrderDetail(it) },
                    onOpenVisit = { viewModel.openVisitDetail(it) }
                )

                AppScreen.MASTERS,
                AppScreen.CUSTOMER_MASTER,
                AppScreen.SUPPLIER_MASTER,
                AppScreen.EMPLOYEE_MASTER,
                AppScreen.PRODUCT_MASTER,
                AppScreen.BRAND_MASTER,
                AppScreen.TRANSPORTER_MASTER,
                AppScreen.MARKET_MASTER -> MastersScreen(
                    viewModel = viewModel,
                    onOpenCustomer = { viewModel.openCustomerDetail(it) },
                    onOpenSupplier = { viewModel.openSupplierDetail(it) },
                    onOpenEmployee = { viewModel.openEmployeeDetail(it) },
                    onOpenProduct = { viewModel.openProductDetail(it) },
                    onOpenBrand = { viewModel.openBrandDetail(it) },
                    onOpenTransporter = { viewModel.openTransporterDetail(it) },
                    onOpenMarket = { viewModel.openMarketDetail(it) }
                )

                AppScreen.SUB_AGENT_MASTER -> SubAgentsScreen(
                    viewModel = viewModel,
                    onBack = goBack,
                    onOpenAgent = { viewModel.openSubAgentDetail(it) },
                    onAddAgent = { viewModel.openAddSubAgent() }
                )

                AppScreen.SUB_AGENT_DETAIL -> {
                    val agent = selectedEmployeeDetail?.let { sel -> people.find { it.id == sel.id } ?: sel }
                    if (agent != null) {
                        SubAgentDetailScreen(
                            viewModel = viewModel,
                            agent = agent,
                            onBack = goBack,
                            onEdit = { viewModel.openEditSubAgent(it) },
                            onOpenCustomer = { viewModel.openCustomerDetail(it) },
                            onOpenVisit = { viewModel.openVisitDetail(it) },
                            onOpenOrder = { viewModel.openOrderDetail(it) },
                            onOpenSupplier = { viewModel.openSupplierDetail(it) },
                            onOpenEmployee = { viewModel.openEmployeeDetail(it) }
                        )
                    } else MissingSelection(viewModel)
                }

                AppScreen.SUB_AGENT_FORM -> SubAgentFormScreen(viewModel = viewModel, onBack = goBack)

                AppScreen.MORE -> MoreScreen(viewModel = viewModel)

                AppScreen.VISIT_DETAIL -> {
                    val visit = selectedVisit
                    if (visit != null) {
                        VisitDetailScreen(
                            viewModel = viewModel,
                            visit = visit,
                            onBack = goBack,
                            onOpenAddEntry = { viewModel.openAddStop(visit) },
                            onOpenMixedPack = { showMixedPackDialog = true },
                            onOpenCustomerReport = { viewModel.openCustomerReport(it) },
                            onOpenSupplierCopy = { v, sup -> viewModel.openSupplierCopy(v, sup) }
                        )
                    } else MissingSelection(viewModel)
                }

                AppScreen.ADD_STOP -> {
                    val visit = selectedVisit
                    if (visit != null) {
                        val activeQueue = supplierQueue?.takeIf { it.tripId == visit.id && !it.finished }
                        // A phone order starts a fresh, empty form for every supplier
                        key(visit.id, activeQueue?.index) {
                            AddStopScreen(
                                viewModel = viewModel,
                                visit = visit,
                                onBack = goBack,
                                onSaveSuccess = goBack,
                                onOpenMixedPack = {
                                    viewModel.navigateBack()
                                    showMixedPackDialog = true
                                },
                                queue = activeQueue
                            )
                        }
                    } else MissingSelection(viewModel)
                }

                AppScreen.CUSTOMER_REPORT_VIEW -> {
                    val visit = selectedVisit
                    if (visit != null) {
                        CustomerReportScreen(viewModel = viewModel, visit = visit, onBack = goBack)
                    } else MissingSelection(viewModel)
                }

                AppScreen.SUPPLIER_COPY_VIEW -> {
                    val visit = selectedVisit
                    val supplier = selectedSupplierForCopy
                    if (visit != null && supplier != null) {
                        SupplierReportScreen(viewModel = viewModel, visit = visit, initialSupplier = supplier, onBack = goBack)
                    } else MissingSelection(viewModel)
                }

                AppScreen.ADD_EDIT_MASTER -> AddEditMasterScreen(viewModel = viewModel, onBack = goBack)

                AppScreen.CUSTOMER_DETAIL -> {
                    // Always show the latest saved version (e.g. right after editing)
                    val customer = selectedCustomer?.let { sel -> allCustomers.find { it.id == sel.id } ?: sel }
                    if (customer != null) {
                        CustomerDetailScreen(
                            viewModel = viewModel,
                            customer = customer,
                            onBack = goBack,
                            onCreateVisit = { startTrip(customer) },
                            onOpenVisit = { viewModel.openVisitDetail(it) },
                            onEditCustomer = { viewModel.openEditCustomer(it) },
                            onDeleteCustomer = {
                                viewModel.deleteCustomer(it)
                                viewModel.navigateBack()
                            }
                        )
                    } else MissingSelection(viewModel)
                }

                AppScreen.SUPPLIER_DETAIL -> {
                    val supplier = selectedSupplier?.let { sel -> suppliers.find { it.id == sel.id } ?: sel }
                    if (supplier != null) {
                        SupplierDetailScreen(
                            viewModel = viewModel,
                            supplier = supplier,
                            onBack = goBack,
                            onOpenVisit = { viewModel.openVisitDetail(it) },
                            onOpenOrder = { viewModel.openOrderDetail(it) },
                            onEditSupplier = { viewModel.openEditSupplier(it) },
                            onDeleteSupplier = {
                                viewModel.deleteSupplier(it)
                                viewModel.navigateBack()
                            }
                        )
                    } else MissingSelection(viewModel)
                }

                AppScreen.EMPLOYEE_DETAIL -> {
                    val employee = selectedEmployeeDetail?.let { sel -> people.find { it.id == sel.id } ?: sel }
                    if (employee != null) {
                        EmployeeDetailScreen(
                            viewModel = viewModel,
                            employee = employee,
                            onBack = goBack,
                            onOpenVisit = { viewModel.openVisitDetail(it) }
                        )
                    } else MissingSelection(viewModel)
                }

                AppScreen.PRODUCT_DETAIL -> {
                    val product = selectedProduct?.let { sel -> products.find { it.id == sel.id } ?: sel }
                    if (product != null) {
                        ProductDetailScreen(
                            viewModel = viewModel,
                            product = product,
                            onBack = goBack,
                            onEdit = { viewModel.openEditProduct(product) },
                            onOpenOrder = { viewModel.openOrderDetail(it) }
                        )
                    } else MissingSelection(viewModel)
                }

                AppScreen.BRAND_DETAIL -> {
                    val brand = selectedBrand?.let { sel -> brands.find { it.id == sel.id } ?: sel }
                    if (brand != null) {
                        BrandDetailScreen(
                            viewModel = viewModel,
                            brand = brand,
                            onBack = goBack,
                            onEdit = { viewModel.openEditBrand(brand) },
                            onOpenProduct = { viewModel.openProductDetail(it) },
                            onOpenOrder = { viewModel.openOrderDetail(it) }
                        )
                    } else MissingSelection(viewModel)
                }

                AppScreen.TRANSPORTER_DETAIL -> {
                    val transporter = selectedTransporter?.let { sel -> transporters.find { it.id == sel.id } ?: sel }
                    if (transporter != null) {
                        TransporterDetailScreen(
                            viewModel = viewModel,
                            transporter = transporter,
                            onBack = goBack,
                            onEdit = { viewModel.openEditTransporter(transporter) },
                            onOpenOrder = { viewModel.openOrderDetail(it) },
                            onOpenCustomer = { viewModel.openCustomerDetail(it) }
                        )
                    } else MissingSelection(viewModel)
                }

                AppScreen.MARKET_DETAIL -> {
                    val market = selectedMarket?.let { sel -> markets.find { it.id == sel.id } ?: sel }
                    if (market != null) {
                        MarketDetailScreen(
                            viewModel = viewModel,
                            market = market,
                            onBack = goBack,
                            onEdit = { viewModel.openEditMarket(market) },
                            onOpenCustomer = { viewModel.openCustomerDetail(it) },
                            onOpenSupplier = { viewModel.openSupplierDetail(it) },
                            onOpenOrder = { viewModel.openOrderDetail(it) },
                            onOpenEmployee = { viewModel.openEmployeeDetail(it) }
                        )
                    } else MissingSelection(viewModel)
                }

                AppScreen.ORDER_DETAIL -> {
                    val entry = selectedPurchaseEntry?.let { sel -> allEntries.find { it.id == sel.id } ?: sel }
                    if (entry != null) {
                        OrderDetailScreen(
                            viewModel = viewModel,
                            entry = entry,
                            onBack = goBack,
                            onOpenVisit = { viewModel.openVisitDetail(it) }
                        )
                    } else MissingSelection(viewModel)
                }

                AppScreen.REPORTS -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigate = { target ->
                        when (target) {
                            AppScreen.DELIVERIES -> viewModel.openOrders("Not delivered")
                            AppScreen.PURCHASE_ORDERS -> viewModel.openOrders()
                            else -> viewModel.navigateTo(target)
                        }
                    },
                    onOpenNewVisit = { startTrip() },
                    onOpenVisit = { viewModel.openVisitDetail(it) },
                    onBack = goBack
                )

                AppScreen.PAYMENTS -> PaymentsScreen(
                    viewModel = viewModel,
                    onBack = goBack,
                    onOpenCustomer = { viewModel.openCustomerDetail(it) },
                    onOpenSupplier = { viewModel.openSupplierDetail(it) },
                    onOpenVisit = { viewModel.openVisitDetail(it) }
                )

                AppScreen.PENDINGS -> PendingScreen(
                    viewModel = viewModel,
                    onBack = goBack,
                    onOpenVisit = { viewModel.openVisitDetail(it) },
                    onOpenCustomer = { viewModel.openCustomerDetail(it) },
                    onOpenSupplier = { viewModel.openSupplierDetail(it) },
                    onOpenMixedPack = { showMixedPackDialog = true }
                )

                AppScreen.PROFILE -> ProfileScreen(viewModel = viewModel, onBack = goBack)

                AppScreen.DELETION_REQUESTS -> DeletionRequestsScreen(viewModel = viewModel, onBack = goBack)

                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel, onBack = goBack)

                AppScreen.LEADS -> LeadsScreen(viewModel = viewModel, onBack = goBack)

                AppScreen.CUSTOMER_ORDERS_REPORT -> {
                    val customerForReport by viewModel.selectedCustomerForReport.collectAsStateWithLifecycle()
                    CustomerOrderReportScreen(
                        viewModel = viewModel,
                        initialCustomer = customerForReport,
                        onBack = goBack,
                        onOpenOrder = { viewModel.openOrderDetail(it) }
                    )
                }

                AppScreen.CHEQUE_PDC -> ChequePdcScreen(viewModel = viewModel, onBack = goBack)

                // Delivery updates now live on the Orders tab; kept for old deep links
                AppScreen.DELIVERIES -> DeliveriesScreen(viewModel = viewModel)

                else -> {
                    // Retired screens (old analytics / supplier hub) fall back to Home
                    LaunchedEffect(currentScreen) { viewModel.resetNavigation() }
                }
            }
        }

        if (isTabScreen) {
            // Pull to refresh only on the main tabs, so a scroll on a form never triggers a full re-sync
            PullToRefreshBox(
                isRefreshing = isCloudSyncing,
                onRefresh = { viewModel.refreshAllData() },
                modifier = contentModifier
            ) { router() }
        } else {
            Box(modifier = contentModifier) { router() }
        }
    }

    if (showCreateVisitDialog) {
        NewTripSheet(
            customers = customers,
            employees = employees,
            suppliers = suppliers,
            defaultEmployee = currentEmployee?.takeIf { !Roles.isAgent(it.role) },
            initialCustomer = createVisitCustomer,
            onDismiss = { showCreateVisitDialog = false },
            onStartMarketTrip = { customer, employee, others, notes ->
                viewModel.createVisit(customer, employee, notes, extraMembers = others) {
                    showCreateVisitDialog = false
                }
            },
            onStartPhoneOrder = { customer, salesman, others, pickedSuppliers, notes ->
                showCreateVisitDialog = false
                viewModel.startPhoneOrder(customer, salesman, others, pickedSuppliers, notes)
            },
            onQuickCreateCustomer = { newCust -> viewModel.saveCustomer(newCust) }
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
                        onSuccess = { showMixedPackDialog = false }
                    )
                }
            ) 
        } ?: run { showMixedPackDialog = false }
    }

    // Registration inbox: the sheet is the summary, the full screens do the approving
    if (showRequestsHub) {
        RequestsHubSheet(
            viewModel = viewModel,
            onDismiss = { showRequestsHub = false },
            onOpenCustomerRequests = {
                showRequestsHub = false
                showCustomerRequestsFull = true
            },
            onOpenSupplierRequests = {
                showRequestsHub = false
                showSupplierRequestsFull = true
            },
            onShareLink = {
                showRequestsHub = false
                showShareLinkSheet = true
            },
            onOpenDeleteRequests = {
                showRequestsHub = false
                viewModel.navigateTo(AppScreen.DELETION_REQUESTS)
            }
        )
    }
    if (showCustomerRequestsFull) {
        CustomerRequestsDialog(viewModel = viewModel, onDismiss = { showCustomerRequestsFull = false })
    }
    if (showSupplierRequestsFull) {
        SupplierRequestsDialog(viewModel = viewModel, onDismiss = { showSupplierRequestsFull = false })
    }
    if (showShareLinkSheet) {
        RegistrationShareBottomSheet(onDismiss = { showShareLinkSheet = false })
    }

    if (showAccessSheet) {
        PermissionsSheet(viewModel = viewModel, onDismiss = { showAccessSheet = false })
    }

    // Every delete in the app passes through here first, so nothing is ever removed without the
    // user seeing what else is attached to it.
    pendingDelete?.let { pending ->
        DeleteImpactDialog(
            impact = pending.impact,
            hardDelete = pending.hardDelete,
            onCancel = { viewModel.dismissPendingDelete() },
            onConfirm = { alsoRemoveLinked -> viewModel.confirmPendingDelete(alsoRemoveLinked) }
        )
    }
}
