package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.AppSearchField
import com.example.ui.components.ChoiceChips
import com.example.ui.components.DateRangeFilterBar
import com.example.ui.components.EmptyState
import com.example.ui.components.HeaderIconButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.TripRow
import com.example.ui.components.UiDimens
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.DateRangeFilter
import com.example.util.Roles
import com.example.util.hasMember
import com.example.util.isClosed
import com.example.util.membersDisplay
import com.example.util.tripMembers

/** Trips tab: every trip the user may see, with open/closed, date, salesman and search filters. */
@Composable
fun VisitsScreen(
    viewModel: HimatViewModel,
    onOpenVisit: (VisitEntity) -> Unit,
    onOpenNewVisit: () -> Unit
) {
    val visits by viewModel.visibleVisits.collectAsStateWithLifecycle()
    val joinable by viewModel.joinableVisits.collectAsStateWithLifecycle()
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val role by viewModel.currentRole.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminUser.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val isAgent = Roles.isAgent(role)

    var showSearch by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("Open") }
    var salesmanFilter by rememberSaveable { mutableStateOf("all") }
    var dateFilter by remember { mutableStateOf(DateRangeFilter()) }

    val byTrip = remember(entries) { entries.groupBy { it.visitId } }
    val joinableIds = remember(joinable) { joinable.map { it.id }.toSet() }

    // "Join" shows running trips of other salesmen next to the user's own trips
    val source = remember(visits, joinable, statusFilter) {
        if (statusFilter == "Join") joinable else visits
    }
    val salesmenOptions = remember(visits, employees) {
        visits.flatMap { it.tripMembers(employees) }.distinctBy { it.id }.sortedBy { it.name.lowercase() }
    }
    val filtered = remember(source, query, statusFilter, salesmanFilter, dateFilter, employees) {
        val q = query.trim().lowercase()
        source.filter { v ->
            val statusOk = when (statusFilter) {
                "Open" -> !v.isClosed()
                "Closed" -> v.isClosed()
                else -> true
            }
            val salesmanOk = salesmanFilter == "all" || v.tripMembers().any { it.id.toString() == salesmanFilter }
            val searchOk = q.isEmpty() || listOf(v.customerName, v.visitCode, v.membersDisplay(employees), v.date)
                .any { it.lowercase().contains(q) }
            statusOk && salesmanOk && searchOk && dateFilter.matches(v.date)
        }.sortedWith(compareByDescending<VisitEntity> { it.date }.thenByDescending { it.createdAt })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!isAgent) {
                ExtendedFloatingActionButton(
                    onClick = onOpenNewVisit,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Trip") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = "Trips",
                subtitle = "${visits.size} trips • ${visits.count { !it.isClosed() }} open"
            ) {
                HeaderIconButton(
                    icon = if (showSearch) Icons.Default.Clear else Icons.Default.Search,
                    contentDescription = if (showSearch) "Close search" else "Search trips",
                    highlighted = showSearch || query.isNotBlank(),
                    onClick = {
                        showSearch = !showSearch
                        if (!showSearch) query = ""
                    }
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showSearch) {
                    AppSearchField(query = query, onQueryChange = { query = it }, placeholder = "Customer, trip code, salesman or date")
                }
                val statusOptions = buildList {
                    add("Open" to "Open (${visits.count { !it.isClosed() }})")
                    add("Closed" to "Closed (${visits.count { it.isClosed() }})")
                    add("All" to "All (${visits.size})")
                    if (!isAgent && joinable.isNotEmpty()) add("Join" to "Can join (${joinable.size})")
                }
                ChoiceChips(options = statusOptions, selected = statusFilter, onSelect = { statusFilter = it })
                DateRangeFilterBar(filter = dateFilter, onChange = { dateFilter = it })
                if (isAdmin && salesmenOptions.size > 1) {
                    ChoiceChips(
                        options = listOf("all" to "All salesmen") + salesmenOptions.map { it.id.toString() to it.name },
                        selected = salesmanFilter,
                        onSelect = { salesmanFilter = it }
                    )
                }
            }

            // Rows run edge to edge with their own padding and a divider, so the list is not inset here
            LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)) {
                if (filtered.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No trips found",
                            message = when {
                                query.isNotBlank() || dateFilter.isActive -> "Try another search or date range."
                                isAgent -> "Trips of your customers will show here."
                                else -> "Tap New Trip when you take a customer to the market."
                            },
                            modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)
                        )
                    }
                }
                items(filtered, key = { it.id }) { visit ->
                    val tripOrders = byTrip[visit.id].orEmpty()
                    val canJoin = visit.id in joinableIds && !visit.hasMember(currentEmployee)
                    TripRow(
                        visit = visit,
                        salesmen = visit.membersDisplay(employees),
                        ordersCount = tripOrders.size,
                        pieces = tripOrders.sumOf { it.pieces },
                        onClick = { onOpenVisit(visit) },
                        trailing = if (canJoin) {
                            {
                                TextButton(onClick = { viewModel.joinVisit(visit) }) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                                    Text("  Join")
                                }
                            }
                        } else null
                    )
                }
            }
        }
    }
}
