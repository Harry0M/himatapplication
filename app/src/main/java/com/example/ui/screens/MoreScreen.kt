package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.InfoCard
import com.example.ui.components.MenuRow
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.UiDimens
import com.example.ui.dialogs.CustomerRequestsDialog
import com.example.ui.dialogs.RegistrationShareBottomSheet
import com.example.ui.dialogs.SupplierRequestsDialog
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.Roles

/**
 * Everything that is not a daily tab lives here, each feature exactly once:
 * reports, money, follow-ups, registration requests and account.
 */
@Composable
fun MoreScreen(viewModel: HimatViewModel) {
    val context = LocalContext.current
    val role by viewModel.currentRole.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminUser.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val pendingCustomerRequests by viewModel.pendingRegistrationRequestsCount.collectAsStateWithLifecycle()
    val pendingSupplierRequests by viewModel.pendingSupplierRegistrationRequestsCount.collectAsStateWithLifecycle()
    val dueCheques by viewModel.dueTodayChequesCount.collectAsStateWithLifecycle()
    val pendingDeletions by viewModel.pendingDeletionCount.collectAsStateWithLifecycle()
    val isAgent = Roles.isAgent(role)

    var showCustomerRequests by remember { mutableStateOf(false) }
    var showSupplierRequests by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    val who = currentEmployee?.name ?: currentUser?.displayName ?: currentUser?.email ?: ""

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ScreenHeader(
                title = "More",
                subtitle = listOf(who, if (isAdmin) "Admin" else Roles.label(role)).filter { it.isNotBlank() }.joinToString(" • ")
            )
        }

        // Sub Agents: their customers are the Customers tab and the share link is on Home,
        // so More only holds the account.
        if (!isAgent) {
            item { MoreGroup("Reports") {
                MenuRow(Icons.Default.Assessment, "Reports & analytics", "Sales, suppliers, trends by period") {
                    viewModel.navigateTo(AppScreen.REPORTS)
                }
                MenuDivider()
                MenuRow(Icons.Default.Description, "Customer statement", "Orders of one customer as PDF") {
                    viewModel.openCustomerOrdersReport(null)
                }
            } }

            item { MoreGroup("Money") {
                MenuRow(Icons.Default.Payments, "Payments & bills", "Optional payment tracking per order") {
                    viewModel.navigateTo(AppScreen.PAYMENTS)
                }
                MenuDivider()
                MenuRow(
                    Icons.Default.AccountBalance,
                    "Cheques / PDC",
                    "Security and post-dated cheques",
                    badge = if (dueCheques > 0) "$dueCheques due" else null
                ) { viewModel.navigateTo(AppScreen.CHEQUE_PDC) }
            } }

            item { MoreGroup("Follow-ups") {
                MenuRow(Icons.Default.PendingActions, "Pending work", "Loose pieces, undelivered orders, open trips") {
                    viewModel.navigateTo(AppScreen.PENDINGS)
                }
                MenuDivider()
                MenuRow(Icons.Default.People, "Leads", "Prospects met in the market") {
                    viewModel.navigateTo(AppScreen.LEADS)
                }
            } }

            item { MoreGroup("Registrations") {
                if (isAdmin) {
                    MenuRow(
                        Icons.Default.PersonAdd,
                        "Customer requests",
                        "Approve customers who registered online",
                        badge = if (pendingCustomerRequests > 0) "$pendingCustomerRequests new" else null
                    ) { showCustomerRequests = true }
                    MenuDivider()
                    MenuRow(
                        Icons.Default.Store,
                        "Supplier requests",
                        "Approve suppliers who registered online",
                        badge = if (pendingSupplierRequests > 0) "$pendingSupplierRequests new" else null
                    ) { showSupplierRequests = true }
                    MenuDivider()
                }
                MenuRow(Icons.Default.Share, "Share registration link", "Send the customer or supplier form on WhatsApp") {
                    showShareSheet = true
                }
            } }
        }

        item { MoreGroup("Account") {
            if (isAdmin) {
                MenuRow(
                    Icons.Default.DeleteSweep,
                    "Delete requests",
                    "Confirm or restore what staff deleted",
                    badge = if (pendingDeletions > 0) "$pendingDeletions waiting" else null
                ) { viewModel.navigateTo(AppScreen.DELETION_REQUESTS) }
                MenuDivider()
            }
            MenuRow(Icons.Default.AccountCircle, "Profile & settings", "Your login, sync status") {
                viewModel.navigateTo(AppScreen.PROFILE)
            }
            MenuDivider()
            MenuRow(Icons.AutoMirrored.Filled.Logout, "Sign out", null, tint = Color(0xFFDC2626)) {
                confirmSignOut = true
            }
        } }
    }

    if (showCustomerRequests) {
        CustomerRequestsDialog(viewModel = viewModel, onDismiss = { showCustomerRequests = false })
    }
    if (showSupplierRequests) {
        SupplierRequestsDialog(viewModel = viewModel, onDismiss = { showSupplierRequests = false })
    }
    if (showShareSheet) {
        RegistrationShareBottomSheet(onDismiss = { showShareSheet = false })
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You can sign in again with the same Google account.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    viewModel.signOut(context)
                }) { Text("Sign out", color = Color(0xFFDC2626)) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MoreGroup(title: String, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
        SectionHeader(title = title)
        InfoCard(contentPadding = PaddingValues(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
