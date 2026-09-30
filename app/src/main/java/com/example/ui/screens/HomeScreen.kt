package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import com.example.util.CreditBreach
import com.example.ui.components.InfoCard
import com.example.ui.components.OrderRow
import com.example.ui.components.PrimaryButton
import com.example.ui.components.QuickAction
import com.example.ui.components.QuickActionsRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.components.TripRow
import com.example.ui.components.UiDimens
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel
import com.example.ui.viewmodel.MasterTab
import com.example.util.Roles
import com.example.util.ShareUtil
import com.example.util.hasMember
import com.example.util.isClosed
import com.example.util.isDelivered
import com.example.util.membersDisplay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home = today's work only: start a trip, what needs attention, trips to continue or join and the
 * latest orders. Every other feature has exactly one home: the bottom tabs (Trips, Orders, Masters)
 * or More (reports, money, follow-ups, registrations, account).
 */
@Composable
fun HomeScreen(
    viewModel: HimatViewModel,
    onOpenNewVisit: () -> Unit,
    onOpenVisit: (VisitEntity) -> Unit,
    onOpenOrder: (PurchaseEntryEntity) -> Unit,
    /** Opens the shared registration inbox (customers + suppliers) from the top bar. */
    onOpenRequests: () -> Unit = {}
) {
    val context = LocalContext.current
    val role by viewModel.currentRole.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminUser.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val visits by viewModel.visibleVisits.collectAsStateWithLifecycle()
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val joinable by viewModel.joinableVisits.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val pendingCustomerRequests by viewModel.pendingRegistrationRequestsCount.collectAsStateWithLifecycle()
    val pendingSupplierRequests by viewModel.pendingSupplierRegistrationRequestsCount.collectAsStateWithLifecycle()

    val isAgent = Roles.isAgent(role)
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val todayLabel = remember { SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date()) }
    val visitsById = remember(visits) { visits.associateBy { it.id } }
    val byTrip = remember(entries) { entries.groupBy { it.visitId } }

    val openTrips = remember(visits, currentEmployee, isAdmin, isAgent) {
        visits.filter { !it.isClosed() }
            .filter { isAdmin || isAgent || it.hasMember(currentEmployee) }
            .sortedByDescending { it.createdAt }
    }
    val ordersToday = remember(entries, visitsById, today) {
        entries.count { (it.orderDate.ifBlank { visitsById[it.visitId]?.date.orEmpty() }).take(10) == today }
    }
    val notDelivered = remember(entries) { entries.count { !it.isDelivered() } }
    val recentOrders = remember(entries) { entries.sortedByDescending { it.createdAt }.take(5) }
    val requests = pendingCustomerRequests + pendingSupplierRequests
    // The name the business knows them by, not whatever their Google profile says
    val name by viewModel.signedInName.collectAsStateWithLifecycle()
    val creditBreaches by viewModel.creditBreaches.collectAsStateWithLifecycle()

    // List rows carry their own side padding so they can run edge to edge with a divider;
    // everything else on Home is inset by this.
    val inset = Modifier.padding(horizontal = UiDimens.ScreenPadding)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = inset) {
                Text(
                    "Hello, $name",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    "$todayLabel • ${if (isAdmin) "Admin" else Roles.label(role)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Credit customers who have bought past their limit. Sits above the day's work because it is
        // the one thing on this screen that costs money if it is scrolled past. Stays until an admin
        // clears it, and comes back if the balance climbs further.
        if (!isAgent && creditBreaches.isNotEmpty()) {
            item {
                CreditLimitWarningCard(
                    breaches = creditBreaches,
                    canClear = isAdmin,
                    onClear = { viewModel.acknowledgeCreditBreach(it) },
                    onOpenCustomer = { id ->
                        customers.firstOrNull { it.id == id }?.let { viewModel.openCustomerDetail(it) }
                    },
                    modifier = inset
                )
            }
        }

        // The one main action for this user type
        if (!isAgent) {
            item {
                PrimaryButton(
                    text = "Start a new trip",
                    icon = Icons.Default.AddCircle,
                    onClick = onOpenNewVisit,
                    modifier = inset.fillMaxWidth()
                )
            }
        } else {
            item {
                InfoCard(modifier = inset) {
                    Text("Your customers (${customers.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "You see only the customers linked to you, their trips and orders. " +
                            "Customers who register with your link are linked to you automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            item {
                PrimaryButton(
                    text = "Share my registration link",
                    icon = Icons.Default.Share,
                    onClick = { ShareUtil.shareCustomerRegistrationLink(context, null, currentEmployee?.id) },
                    modifier = inset.fillMaxWidth()
                )
            }
        }

        // Small shortcuts to the screens people open all day; every tab still has its own home
        item {
            // Four rows of four, grouped the way the day runs: work, who we buy from, money,
            // then the rest of the book. Requests and More are not repeated here; they already
            // live in the top bar and the bottom tabs.
            val quickActions = remember(isAdmin, isAgent, notDelivered) {
                buildList {
                    // Today's work
                    add(QuickAction("Trips", Icons.Default.Route) { viewModel.navigateTo(AppScreen.VISITS) })
                    add(QuickAction("Orders", Icons.Default.ReceiptLong) { viewModel.openOrders() })
                    if (isAgent) {
                        add(
                            QuickAction("Not delivered", Icons.Default.LocalShipping, badge = notDelivered) {
                                viewModel.openOrders("Not delivered")
                            }
                        )
                        add(QuickAction("Reports", Icons.Default.Assessment) { viewModel.navigateTo(AppScreen.REPORTS) })
                        add(QuickAction("Customers", Icons.Default.Groups) { viewModel.openMasterList(MasterTab.CUSTOMERS) })
                        add(QuickAction("Profile", Icons.Default.AccountCircle) { viewModel.navigateTo(AppScreen.PROFILE) })
                        return@buildList
                    }
                    add(
                        QuickAction("Deliveries", Icons.Default.LocalShipping, badge = notDelivered) {
                            viewModel.navigateTo(AppScreen.DELIVERIES)
                        }
                    )
                    add(QuickAction("Pendings", Icons.Default.PendingActions) { viewModel.navigateTo(AppScreen.PENDINGS) })

                    // Who we buy from and where
                    add(QuickAction("Customers", Icons.Default.Groups) { viewModel.openMasterList(MasterTab.CUSTOMERS) })
                    add(QuickAction("Suppliers", Icons.Default.Storefront) { viewModel.openMasterList(MasterTab.SUPPLIERS) })
                    add(QuickAction("Brands", Icons.Default.Sell) { viewModel.openMasterList(MasterTab.BRANDS) })
                    add(QuickAction("Markets", Icons.Default.LocationCity) { viewModel.openMasterList(MasterTab.MARKETS) })

                    // Money
                    add(QuickAction("Payments", Icons.Default.Payments) { viewModel.navigateTo(AppScreen.PAYMENTS) })
                    add(QuickAction("Cheque / PDC", Icons.Default.AccountBalanceWallet) { viewModel.navigateTo(AppScreen.CHEQUE_PDC) })
                    add(QuickAction("Reports", Icons.Default.Assessment) { viewModel.navigateTo(AppScreen.REPORTS) })
                    add(QuickAction("Leads", Icons.Default.PersonSearch) { viewModel.navigateTo(AppScreen.LEADS) })

                    // The rest of the book
                    add(QuickAction("Transporters", Icons.Default.LocalShipping) { viewModel.openMasterList(MasterTab.TRANSPORTERS) })
                    add(QuickAction("Products", Icons.Default.Inventory2) { viewModel.openMasterList(MasterTab.PRODUCTS) })
                    add(QuickAction("Staff", Icons.Default.Badge) { viewModel.openMasterList(MasterTab.EMPLOYEES) })
                    if (isAdmin) {
                        add(QuickAction("Sub Agents", Icons.Default.SupportAgent) { viewModel.navigateTo(AppScreen.SUB_AGENT_MASTER) })
                    } else {
                        add(QuickAction("Profile", Icons.Default.AccountCircle) { viewModel.navigateTo(AppScreen.PROFILE) })
                    }
                }
            }
            Column(modifier = inset) {
                SectionHeader(title = "Quick actions")
                QuickActionsRow(actions = quickActions)
            }
        }

        // What needs attention; each number opens the one place where it is handled
        item {
            Column(modifier = inset, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        label = "Open trips",
                        value = "${openTrips.size}",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.VISITS) }
                    )
                    StatTile(
                        label = "Orders today",
                        value = "$ordersToday",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openOrders() }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        label = "Not delivered",
                        value = "$notDelivered",
                        modifier = Modifier.weight(1f),
                        accent = if (notDelivered > 0) Color(0xFFB45309) else Color(0xFF059669),
                        onClick = { viewModel.openOrders("Not delivered") }
                    )
                    if (isAdmin) {
                        StatTile(
                            label = "New registrations",
                            value = "$requests",
                            modifier = Modifier.weight(1f),
                            accent = if (requests > 0) Color(0xFFB91C1C) else Color(0xFF059669),
                            // One inbox for both customer and supplier registrations
                            onClick = onOpenRequests
                        )
                    } else {
                        StatTile(
                            label = "Customers",
                            value = "${customers.size}",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.openMasterList(MasterTab.CUSTOMERS) }
                        )
                    }
                }
            }
        }

        if (joinable.isNotEmpty()) {
            item {
                Column(modifier = inset) {
                    SectionHeader(title = "Running trips you can join", count = joinable.size)
                    Text(
                        "Join to add orders from your phone. Your orders are saved under your name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            items(joinable.take(5), key = { "join_${it.id}" }) { visit ->
                val tripOrders = byTrip[visit.id].orEmpty()
                TripRow(
                    visit = visit,
                    salesmen = visit.membersDisplay(employees),
                    ordersCount = tripOrders.size,
                    pieces = tripOrders.sumOf { it.pieces },
                    onClick = { onOpenVisit(visit) },
                    trailing = {
                        TextButton(onClick = { viewModel.joinVisit(visit) }) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null)
                            Text("  Join")
                        }
                    }
                )
            }
        }

        item {
            SectionHeader(
                title = if (isAdmin || isAgent) "Open trips" else "My open trips",
                count = openTrips.size,
                modifier = inset,
                action = {
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.VISITS) }) { Text("All trips") }
                }
            )
        }
        if (openTrips.isEmpty()) {
            item {
                Text(
                    if (isAgent) "No open trips for your customers." else "No open trips. Tap \"Start a new trip\" when you go to the market.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = inset
                )
            }
        }
        items(openTrips.take(10), key = { "open_${it.id}" }) { visit ->
            val tripOrders = byTrip[visit.id].orEmpty()
            TripRow(
                visit = visit,
                salesmen = visit.membersDisplay(employees),
                ordersCount = tripOrders.size,
                pieces = tripOrders.sumOf { it.pieces },
                onClick = { onOpenVisit(visit) }
            )
        }

        if (recentOrders.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Latest orders",
                    modifier = inset,
                    action = { TextButton(onClick = { viewModel.openOrders() }) { Text("All orders") } }
                )
            }
            items(recentOrders, key = { "order_${it.id}" }) { entry ->
                OrderRow(
                    entry = entry,
                    customerName = visitsById[entry.visitId]?.customerName.orEmpty(),
                    onClick = { onOpenOrder(entry) }
                )
            }
        }
    }

}

/**
 * "These credit customers are over their limit."
 *
 * Deliberately not dismissible by anyone: a salesman seeing it is the point, and only an admin can
 * clear it. Clearing records the balance it was cleared at, so the warning returns if the customer
 * keeps buying rather than being silenced for good.
 */
@Composable
private fun CreditLimitWarningCard(
    breaches: List<CreditBreach>,
    canClear: Boolean,
    onClear: (CreditBreach) -> Unit,
    onOpenCustomer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val amber = Color(0xFFB45309)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (breaches.size == 1) "A credit customer is over their limit"
                    else "${breaches.size} credit customers are over their limit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amber
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (canClear) "Tap a name to open the customer. Clear a warning once you have decided."
                else "Please check with the Admin before booking more for them.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Only the worst few; the rest are on the customer screens
            breaches.take(4).forEach { breach ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenCustomer(breach.customerId) }
                    ) {
                        Text(
                            breach.customerName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Owes ₹${breach.outstanding.toLong()} against ₹${breach.creditLimit.toLong()}" +
                                " • over by ₹${breach.overBy.toLong()}" +
                                if (breach.overPercent > 0) " (${breach.overPercent}%)" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = amber
                        )
                    }
                    if (canClear) {
                        TextButton(onClick = { onClear(breach) }) {
                            Text("Clear", color = amber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (breaches.size > 4) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "and ${breaches.size - 4} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
