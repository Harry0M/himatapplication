package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.ActivitySummary
import com.example.ui.components.AppSearchField
import com.example.ui.components.ChoiceChips
import com.example.ui.components.CustomersList
import com.example.ui.components.DateRangeFilterBar
import com.example.ui.components.EmptyState
import com.example.ui.components.HeaderIconButton
import com.example.ui.components.InfoCard
import com.example.ui.components.OrdersList
import com.example.ui.components.PersonRow
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ReferredList
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.TripsList
import com.example.ui.components.UiDimens
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.DateRangeFilter
import com.example.util.ReferrerTypes
import com.example.util.RelatedLogic
import com.example.util.Roles
import com.example.util.ShareUtil

private val AgentColor = Color(0xFFB45309)

// =============================================================================
// Sub Agents list (Masters > Sub Agents)
// =============================================================================

@Composable
fun SubAgentsScreen(
    viewModel: HimatViewModel,
    onBack: () -> Unit,
    onOpenAgent: (EmployeeEntity) -> Unit,
    onAddAgent: () -> Unit
) {
    val agents by viewModel.subAgents.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val isAgentUser by viewModel.isAgentUser.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Active") }

    val customerCounts = remember(customers, agents) {
        agents.associate { a -> a.id to RelatedLogic.customersOfSubAgent(customers, a).size }
    }
    val filtered = remember(agents, query, statusFilter) {
        val q = query.trim().lowercase()
        agents.filter { a ->
            val deactivated = a.status.equals("Deactivated", true)
            val statusOk = when (statusFilter) {
                "Active" -> !deactivated
                "Deactivated" -> deactivated
                else -> true
            }
            statusOk && (q.isEmpty() || listOf(a.name, a.firmName, a.phone, a.phone2, a.city, a.email).any { it.lowercase().contains(q) })
        }.sortedBy { it.name.lowercase() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!isAgentUser) {
                ExtendedFloatingActionButton(
                    onClick = onAddAgent,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Sub Agent") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = "Sub Agents",
                subtitle = "${agents.size} agents who bring customers",
                onBack = onBack
            )
            Column(
                modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSearchField(query = query, onQueryChange = { query = it }, placeholder = "Search name, firm, phone or city")
                ChoiceChips(
                    options = listOf("Active" to "Active", "Deactivated" to "Deactivated", "All" to "All"),
                    selected = statusFilter,
                    onSelect = { statusFilter = it }
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(start = UiDimens.ScreenPadding, end = UiDimens.ScreenPadding, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(UiDimens.ItemGap)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        EmptyState(
                            title = if (agents.isEmpty()) "No sub agents yet" else "No matching sub agents",
                            message = "Sub agents are outside people who bring customers. Link them on the customer form.",
                            icon = Icons.Default.Groups,
                            actionLabel = if (agents.isEmpty() && !isAgentUser) "Add Sub Agent" else null,
                            onAction = if (agents.isEmpty() && !isAgentUser) onAddAgent else null
                        )
                    }
                }
                items(filtered, key = { it.id }) { agent ->
                    val count = customerCounts[agent.id] ?: 0
                    PersonRow(
                        title = agent.name,
                        subtitle = listOf(agent.firmName, agent.city, "$count customers").filter { it.isNotBlank() }.joinToString(" • "),
                        badge = if (agent.status.equals("Deactivated", true)) "Deactivated" else "Sub Agent",
                        badgeColor = if (agent.status.equals("Deactivated", true)) Color(0xFF64748B) else AgentColor,
                        onClick = { onOpenAgent(agent) }
                    )
                }
            }
        }
    }
}

// =============================================================================
// Sub Agent profile with every linked record
// =============================================================================

@Composable
fun SubAgentDetailScreen(
    viewModel: HimatViewModel,
    agent: EmployeeEntity,
    onBack: () -> Unit,
    onEdit: (EmployeeEntity) -> Unit,
    onOpenCustomer: (CustomerEntity) -> Unit,
    onOpenVisit: (VisitEntity) -> Unit,
    onOpenOrder: (PurchaseEntryEntity) -> Unit,
    onOpenSupplier: (SupplierEntity) -> Unit,
    onOpenEmployee: (EmployeeEntity) -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val visits by viewModel.allVisits.collectAsStateWithLifecycle()
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    val people by viewModel.allPeople.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminUser.collectAsStateWithLifecycle()
    val isAgentUser by viewModel.isAgentUser.collectAsStateWithLifecycle()

    var dateFilter by remember { mutableStateOf(DateRangeFilter()) }
    var tab by remember { mutableStateOf("customers") }
    var confirmDeactivate by remember { mutableStateOf(false) }

    val agentCustomers = remember(customers, agent) { RelatedLogic.customersOfSubAgent(customers, agent) }
    val customerIds = remember(agentCustomers) { agentCustomers.map { it.id }.toSet() }
    val visitsById = remember(visits) { visits.associateBy { it.id } }
    val allTrips = remember(visits, customerIds) { RelatedLogic.tripsOfCustomers(visits, customerIds).sortedByDescending { it.date } }
    val trips = remember(allTrips, dateFilter) { RelatedLogic.filterTripsByDate(allTrips, dateFilter) }
    val allOrders = remember(entries, allTrips) { RelatedLogic.ordersOfTrips(entries, allTrips.map { it.id }.toSet()) }
    val orders = remember(allOrders, visitsById, dateFilter) { RelatedLogic.filterOrdersByDate(allOrders, visitsById, dateFilter) }

    val names = remember(agent) { listOf(agent.name, agent.firmName) }
    val referredCustomers = remember(customers, agent) { RelatedLogic.referredCustomers(customers, ReferrerTypes.AGENT, agent.id, names) }
    val referredSuppliers = remember(suppliers, agent) { RelatedLogic.referredSuppliers(suppliers, ReferrerTypes.AGENT, agent.id, names) }
    val referredPeople = remember(people, agent) { RelatedLogic.referredPeople(people, ReferrerTypes.AGENT, agent.id, names) }
    val referredCount = referredCustomers.size + referredSuppliers.size + referredPeople.size

    Column(modifier = Modifier.fillMaxSize().padding(bottom = 0.dp)) {
        ScreenHeader(
            title = agent.name,
            subtitle = listOf("Sub Agent", agent.firmName, agent.city).filter { it.isNotBlank() }.joinToString(" • "),
            onBack = onBack
        ) {
            if (agent.phone.isNotBlank()) {
                HeaderIconButton(Icons.Default.Call, "Call ${agent.name}", onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${agent.phone}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                })
            }
            if (!isAgentUser) {
                HeaderIconButton(Icons.Default.Edit, "Edit", onClick = { onEdit(agent) })
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = UiDimens.ScreenPadding, end = UiDimens.ScreenPadding, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoCard {
                    DetailLine("Phone", listOf(agent.phone, agent.phone2).filter { it.isNotBlank() }.joinToString(", "))
                    DetailLine("Firm", agent.firmName)
                    DetailLine("City", agent.city)
                    DetailLine("Login email", agent.email.ifBlank { "No login (records only)" })
                    DetailLine("Status", agent.status.ifBlank { "Active" })
                    DetailLine("Referred by", agent.referredBy)
                    DetailLine("Notes", agent.notes)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(
                        text = "Share registration link",
                        icon = Icons.Default.Share,
                        modifier = Modifier.weight(1f),
                        onClick = { ShareUtil.shareCustomerRegistrationLink(context, agent.phone.ifBlank { null }, agent.id) }
                    )
                }
                Text(
                    text = "Customers who register with this link are linked to ${agent.name} automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item { DateRangeFilterBar(filter = dateFilter, onChange = { dateFilter = it }) }

            item { ActivitySummary(trips = trips.size, orders = orders) }

            item {
                ChoiceChips(
                    options = listOf(
                        "customers" to "Customers (${agentCustomers.size})",
                        "trips" to "Trips (${trips.size})",
                        "orders" to "Orders (${orders.size})",
                        "referred" to "Referred ($referredCount)"
                    ),
                    selected = tab,
                    onSelect = { tab = it }
                )
            }

            item {
                when (tab) {
                    "customers" -> CustomersList(agentCustomers, onOpenCustomer, emptyText = "No customers linked to ${agent.name} yet. Pick this agent on the customer form.")
                    "trips" -> TripsList(trips, entries, employees, onOpenVisit)
                    "orders" -> OrdersList(orders, visitsById, onOpenOrder)
                    else -> ReferredList(referredCustomers, referredSuppliers, referredPeople, onOpenCustomer, onOpenSupplier, onOpenEmployee)
                }
            }

            if (isAdmin) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (agent.status.equals("Deactivated", true)) {
                        SecondaryButton(text = "Reactivate sub agent", onClick = { viewModel.reactivateSubAgent(agent) }, modifier = Modifier.fillMaxWidth())
                    } else {
                        SecondaryButton(
                            text = "Deactivate sub agent",
                            contentColor = Color(0xFFDC2626),
                            onClick = { confirmDeactivate = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (confirmDeactivate) {
        AlertDialog(
            onDismissRequest = { confirmDeactivate = false },
            title = { Text("Deactivate ${agent.name}?") },
            text = { Text("Their login stops working. Linked customers, trips and reports stay as they are.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeactivate = false
                    viewModel.deactivateSubAgent(agent) { onBack() }
                }) { Text("Deactivate", color = Color(0xFFDC2626)) }
            },
            dismissButton = { TextButton(onClick = { confirmDeactivate = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(0.38f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.62f))
    }
}

// =============================================================================
// Add / edit Sub Agent
// =============================================================================

@Composable
fun SubAgentFormScreen(
    viewModel: HimatViewModel,
    onBack: () -> Unit
) {
    val editing by viewModel.editingEmployee.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminUser.collectAsStateWithLifecycle()
    val existing = editing?.takeIf { Roles.isAgent(it.role) }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var phone by remember(existing) { mutableStateOf(existing?.phone ?: "") }
    var phone2 by remember(existing) { mutableStateOf(existing?.phone2 ?: "") }
    var firm by remember(existing) { mutableStateOf(existing?.firmName ?: "") }
    var city by remember(existing) { mutableStateOf(existing?.city ?: "") }
    var email by remember(existing) { mutableStateOf(existing?.email ?: "") }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (existing == null) "New Sub Agent" else "Edit Sub Agent",
            subtitle = "Person who brings customers to us",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UiDimens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormField("Name *", name, { name = it })
            FormField("Mobile number *", phone, { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } }, KeyboardType.Phone)
            FormField("Second mobile", phone2, { phone2 = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } }, KeyboardType.Phone)
            FormField("Firm / business name", firm, { firm = it })
            FormField("City", city, { city = it })
            if (isAdmin) {
                FormField("Login email (optional)", email, { email = it.trim() }, KeyboardType.Email)
                Text(
                    "With a Google login email the sub agent can open the app and see only their own customers and orders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / commission terms") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            error?.let { Text(it, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall) }
            PrimaryButton(
                text = if (saving) "Saving..." else "Save Sub Agent",
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val digits = phone.filter { it.isDigit() }
                    when {
                        name.isBlank() -> error = "Name is required"
                        digits.length < 10 -> error = "Enter a valid mobile number"
                        isAdmin && email.isNotBlank() && !email.contains("@") -> error = "Enter a valid email or leave it empty"
                        else -> {
                            error = null
                            saving = true
                            val candidate = (existing ?: EmployeeEntity()).copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                phone2 = phone2.trim(),
                                firmName = firm.trim(),
                                city = city.trim(),
                                email = email.trim(),
                                notes = notes.trim(),
                                role = Roles.AGENT,
                                employeeId = existing?.employeeId?.ifBlank { null } ?: "AGT-${digits.takeLast(4)}"
                            )
                            viewModel.saveSubAgent(candidate) {
                                saving = false
                                onBack()
                            }
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
