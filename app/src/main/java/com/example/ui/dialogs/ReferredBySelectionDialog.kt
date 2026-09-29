package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.SupplierEntity
import com.example.ui.components.AppSearchField
import com.example.ui.components.PillChip
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ReferrerTypes
import com.example.util.Roles

/**
 * One selectable referrer. [type] + [id] are stored as referredByType / referredById so links survive
 * renames; [formattedValue] ("Customer: Balaji Sarees") is the display text stored in referredBy.
 */
data class ReferrerItem(
    val type: String,
    val id: Long,
    val title: String,
    val subtitle: String,
    val searchText: String
) {
    val formattedValue: String get() = ReferrerTypes.format(type, title)
    val key: String get() = "$type:$id"
}

private fun EmployeeEntity.toReferrer(type: String) = ReferrerItem(
    type = type,
    id = id,
    title = name.ifBlank { "Unnamed" },
    subtitle = listOf(
        if (type == ReferrerTypes.AGENT) firmName.ifBlank { "Sub Agent" } else Roles.label(role),
        phone, city
    ).filter { it.isNotBlank() }.joinToString(" • "),
    searchText = listOf(name, firmName, phone, phone2, city, email, assignedMarkets).joinToString(" ").lowercase()
)

private fun CustomerEntity.toReferrer() = ReferrerItem(
    type = ReferrerTypes.CUSTOMER,
    id = id,
    title = firmName.ifBlank { name }.ifBlank { "Customer #$id" },
    subtitle = listOf(name.takeIf { it != firmName }.orEmpty(), city, phone).filter { it.isNotBlank() }.joinToString(" • "),
    searchText = listOf(firmName, name, phone, phone2, city, gstin, marketArea).joinToString(" ").lowercase()
)

private fun SupplierEntity.toReferrer() = ReferrerItem(
    type = ReferrerTypes.SUPPLIER,
    id = id,
    // Some suppliers only have a name, no firm name
    title = firmName.ifBlank { name }.ifBlank { "Supplier #$id" },
    subtitle = listOf(type, marketName.ifBlank { marketArea }, phone).filter { it.isNotBlank() }.joinToString(" • "),
    searchText = listOf(firmName, name, contactPerson, phone, phone2, city, marketName, marketArea, brand, gstin).joinToString(" ").lowercase()
)

private data class ReferrerStyle(val color: Color, val label: String, val icon: ImageVector)

private fun styleFor(type: String): ReferrerStyle = when (type) {
    ReferrerTypes.STAFF -> ReferrerStyle(Color(0xFF2563EB), "Staff", Icons.Default.Person)
    ReferrerTypes.AGENT -> ReferrerStyle(Color(0xFFB45309), "Sub Agent", Icons.Default.Handshake)
    ReferrerTypes.CUSTOMER -> ReferrerStyle(Color(0xFF059669), "Customer", Icons.Default.Store)
    ReferrerTypes.SUPPLIER -> ReferrerStyle(Color(0xFF7C3AED), "Supplier", Icons.Default.Business)
    else -> ReferrerStyle(Color(0xFF64748B), "Broker", Icons.Default.Edit)
}

/**
 * Pick who referred a customer / supplier / staff member: Staff, Sub Agent, Customer, Supplier or free-text broker.
 * [employeesList] may contain everybody from the employees node; sub agents are split out by role.
 * [excludeKey] ("Customer:12") hides the record being edited so it cannot refer itself.
 */
@Composable
fun ReferredBySelectionDialog(
    currentValue: String,
    customersList: List<CustomerEntity>,
    suppliersList: List<SupplierEntity>,
    employeesList: List<EmployeeEntity>,
    onSelect: (value: String, type: String, id: Long?) -> Unit,
    onDismiss: () -> Unit,
    agentsList: List<EmployeeEntity> = emptyList(),
    currentType: String = "",
    currentId: Long? = null,
    excludeKey: String? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("ALL") }
    var customText by remember { mutableStateOf("") }

    val allItems = remember(employeesList, agentsList, customersList, suppliersList, excludeKey) {
        val people = (employeesList + agentsList).filter { !it.isDeleted && it.id > 0L }.distinctBy { it.id }
        val staff = people.filter { !Roles.isAgent(it.role) }.map { it.toReferrer(ReferrerTypes.STAFF) }
        val agents = people.filter { Roles.isAgent(it.role) }.map { it.toReferrer(ReferrerTypes.AGENT) }
        val custs = customersList.filter { !it.isDeleted && it.id > 0L }.map { it.toReferrer() }
        val supps = suppliersList.filter { !it.isDeleted && it.id > 0L }.map { it.toReferrer() }
        (agents + staff + custs + supps).filter { it.key != excludeKey }
    }

    val filteredItems = remember(allItems, searchQuery, category) {
        val q = searchQuery.trim().lowercase()
        allItems.filter { item ->
            val matchesCategory = category == "ALL" || item.type == category
            matchesCategory && (q.isEmpty() || item.searchText.contains(q) || item.title.lowercase().contains(q))
        }.take(200)
    }

    val categories = listOf(
        "ALL" to "All",
        ReferrerTypes.AGENT to "Sub Agents",
        ReferrerTypes.STAFF to "Staff",
        ReferrerTypes.CUSTOMER to "Customers",
        ReferrerTypes.SUPPLIER to "Suppliers",
        ReferrerTypes.BROKER to "Other / Broker"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Referred By", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = NavyPrimary)
                        Text("Who brought this party to us?", fontSize = 12.5.sp, color = TextSecondary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { (key, label) ->
                        val count = if (key == "ALL") allItems.size else allItems.count { it.type == key }
                        PillChip(
                            label = if (key == ReferrerTypes.BROKER) label else "$label ($count)",
                            selected = category == key,
                            onClick = { category = key }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (category == ReferrerTypes.BROKER) {
                    Text(
                        text = "Someone outside our records (broker, relative, friend)",
                        fontSize = 12.5.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = { Text("Name") },
                        placeholder = { Text("e.g. Suresh Bhai") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val name = customText.trim().substringAfter(":").trim()
                                if (name.isNotBlank()) {
                                    onSelect(ReferrerTypes.format(ReferrerTypes.BROKER, name), ReferrerTypes.BROKER, null)
                                    onDismiss()
                                }
                            },
                            enabled = customText.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Text("Use this name", color = Color.White)
                        }
                    }
                } else {
                    AppSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search name, firm, phone or city"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredItems.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No matching records", fontSize = 13.sp, color = TextSecondary)
                                    if (searchQuery.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                onSelect(ReferrerTypes.format(ReferrerTypes.BROKER, searchQuery.trim()), ReferrerTypes.BROKER, null)
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Use \"${searchQuery.trim()}\" as broker name", fontSize = 12.5.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredItems, key = { it.key }) { item ->
                                val isSelected = if (currentId != null && currentId > 0L && currentType.isNotBlank()) {
                                    currentType.equals(item.type, ignoreCase = true) && currentId == item.id
                                } else {
                                    currentValue.equals(item.formattedValue, ignoreCase = true)
                                }
                                val style = styleFor(item.type)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) NavyPrimary.copy(alpha = 0.08f) else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onSelect(item.formattedValue, item.type, item.id)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = style.color.copy(alpha = 0.12f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${style.label}${if (item.subtitle.isNotBlank()) " • ${item.subtitle}" else ""}",
                                                fontSize = 12.sp,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = NavyPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (currentValue.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    TextButton(
                        onClick = {
                            onSelect("", "", null)
                            onDismiss()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Remove referrer", color = Color(0xFFDC2626), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Clickable field showing the current referrer; opens [ReferredBySelectionDialog].
 * [onLinkChange] receives (display value, type, id) so screens can store the structured link.
 */
@Composable
fun ReferredBySelectorField(
    value: String,
    onValueChange: (String) -> Unit,
    customersList: List<CustomerEntity>,
    suppliersList: List<SupplierEntity>,
    employeesList: List<EmployeeEntity>,
    modifier: Modifier = Modifier,
    label: String = "Referred By",
    agentsList: List<EmployeeEntity> = emptyList(),
    referredByType: String = "",
    referredById: Long? = null,
    excludeKey: String? = null,
    onLinkChange: ((value: String, type: String, id: Long?) -> Unit)? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    val effectiveType = remember(value, referredByType) {
        referredByType.ifBlank { ReferrerTypes.parse(value).first }
    }
    val style = styleFor(effectiveType)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (value.isNotBlank()) style.color.copy(alpha = 0.06f) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (value.isNotBlank()) style.color.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (value.isNotBlank()) style.icon else Icons.Default.Link,
                    contentDescription = null,
                    tint = if (value.isNotBlank()) style.color else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (value.isNotBlank()) {
                        Text(text = style.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = style.color)
                        Text(
                            text = ReferrerTypes.parse(value).second.ifBlank { value },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Select sub agent, staff, customer, supplier or broker",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                if (value.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onValueChange("")
                            onLinkChange?.invoke("", "", null)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear referrer", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showDialog) {
        ReferredBySelectionDialog(
            currentValue = value,
            customersList = customersList,
            suppliersList = suppliersList,
            employeesList = employeesList,
            agentsList = agentsList,
            currentType = referredByType,
            currentId = referredById,
            excludeKey = excludeKey,
            onSelect = { v, type, id ->
                onValueChange(v)
                onLinkChange?.invoke(v, type, id)
            },
            onDismiss = { showDialog = false }
        )
    }
}
