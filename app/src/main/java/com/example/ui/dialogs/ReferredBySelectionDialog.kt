package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.SupplierEntity
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

sealed class ReferrerItem {
    abstract val formattedValue: String
    abstract val title: String
    abstract val subtitle: String
    abstract val entityType: String

    data class Staff(val employee: EmployeeEntity) : ReferrerItem() {
        override val formattedValue: String = "Staff: ${employee.name}"
        override val title: String = employee.name
        override val subtitle: String = "${employee.role.ifBlank { "Staff" }} • ${employee.phone}"
        override val entityType: String = "Staff"
    }

    data class Customer(val customer: CustomerEntity) : ReferrerItem() {
        override val formattedValue: String = "Customer: ${customer.firmName.ifBlank { customer.name }}"
        override val title: String = customer.firmName.ifBlank { customer.name }
        override val subtitle: String = "${customer.city.ifBlank { "Customer" }} • ${customer.phone}"
        override val entityType: String = "Customer"
    }

    data class Supplier(val supplier: SupplierEntity) : ReferrerItem() {
        override val formattedValue: String = "Supplier: ${supplier.firmName}"
        override val title: String = supplier.firmName
        override val subtitle: String = "${supplier.type.ifBlank { "Supplier/Mill" }} • ${supplier.city}"
        override val entityType: String = "Supplier"
    }
}

/**
 * Full master selection modal dialog for linking a referrer (Staff, Customer, Supplier, or Custom text).
 */
@Composable
fun ReferredBySelectionDialog(
    currentValue: String,
    customersList: List<CustomerEntity>,
    suppliersList: List<SupplierEntity>,
    employeesList: List<EmployeeEntity>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var customText by remember { mutableStateOf("") }

    val categories = listOf(
        "All (${employeesList.size + customersList.size + suppliersList.size})",
        "👔 Staff (${employeesList.size})",
        "🏪 Customers (${customersList.size})",
        "🏭 Suppliers (${suppliersList.size})",
        "✍️ Custom Broker"
    )

    val allItems = remember(employeesList, customersList, suppliersList) {
        val staff = employeesList.map { ReferrerItem.Staff(it) }
        val custs = customersList.map { ReferrerItem.Customer(it) }
        val supps = suppliersList.map { ReferrerItem.Supplier(it) }
        staff + custs + supps
    }

    val filteredItems = remember(allItems, searchQuery, selectedCategoryIndex) {
        val q = searchQuery.trim().lowercase()
        allItems.filter { item ->
            val matchesCategory = when (selectedCategoryIndex) {
                1 -> item is ReferrerItem.Staff
                2 -> item is ReferrerItem.Customer
                3 -> item is ReferrerItem.Supplier
                else -> true
            }
            val matchesQuery = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.subtitle.lowercase().contains(q) ||
                item.entityType.lowercase().contains(q)

            matchesCategory && matchesQuery
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NavyPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Select Referred By",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Link to Staff, Customer, Supplier or Broker",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Tabs Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEachIndexed { index, label ->
                        val isSelected = selectedCategoryIndex == index
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedCategoryIndex = index }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedCategoryIndex == 4) {
                    // Custom Broker / Introducer tab
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Add Custom External Broker or Introducer",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = NavyPrimary
                        )
                        OutlinedTextField(
                            value = customText,
                            onValueChange = { customText = it },
                            label = { Text("Broker / Introducer Name") },
                            placeholder = { Text("e.g. Broker: Suresh Bhai (Agent)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentValue.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        onSelect("")
                                        onDismiss()
                                    }
                                ) {
                                    Text("Clear Referrer", color = Color(0xFFDC2626), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Button(
                                onClick = {
                                    if (customText.isNotBlank()) {
                                        val finalVal = if (!customText.contains(":")) "Broker: ${customText.trim()}" else customText.trim()
                                        onSelect(finalVal)
                                        onDismiss()
                                    }
                                },
                                enabled = customText.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                            ) {
                                Text("Apply Referrer", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search name, firm, phone...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyPrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // List of filtered entities
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filteredItems.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No matching records found", fontSize = 12.sp, color = TextSecondary)
                                    if (searchQuery.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                onSelect("Broker: ${searchQuery.trim()}")
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Use \"${searchQuery.trim()}\" as Broker", fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredItems) { item ->
                                val isSelected = currentValue.equals(item.formattedValue, ignoreCase = true) ||
                                    currentValue.equals(item.title, ignoreCase = true)

                                val (badgeColor, badgeText, badgeIcon) = when (item) {
                                    is ReferrerItem.Staff -> Triple(Color(0xFF2563EB), "Staff", Icons.Default.Person)
                                    is ReferrerItem.Customer -> Triple(Color(0xFF059669), "Customer", Icons.Default.Store)
                                    is ReferrerItem.Supplier -> Triple(Color(0xFF7C3AED), "Supplier", Icons.Default.Business)
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) NavyPrimary.copy(alpha = 0.08f) else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFF1F5F9)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelect(item.formattedValue)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = badgeColor.copy(alpha = 0.12f),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(15.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = TextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = badgeColor.copy(alpha = 0.1f)
                                                    ) {
                                                        Text(
                                                            text = badgeText,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = badgeColor,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = item.subtitle,
                                                    fontSize = 10.5.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = NavyPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentValue.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    onSelect("")
                                    onDismiss()
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Clear Selection", color = Color(0xFFDC2626), fontSize = 11.5.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        TextButton(
                            onClick = { selectedCategoryIndex = 4 },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Type Custom Broker", fontSize = 11.5.sp, color = NavyPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated clickable field that displays the current referrer and opens the selection dialog.
 */
@Composable
fun ReferredBySelectorField(
    value: String,
    onValueChange: (String) -> Unit,
    customersList: List<CustomerEntity>,
    suppliersList: List<SupplierEntity>,
    employeesList: List<EmployeeEntity>,
    modifier: Modifier = Modifier,
    label: String = "Referred By (Entity Link)"
) {
    var showDialog by remember { mutableStateOf(false) }

    val (badgeBg, badgeFg, entityIcon, displayTitle) = remember(value) {
        val lower = value.lowercase()
        when {
            lower.startsWith("staff:") || lower.contains("(agent)") -> {
                Quadruple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), Icons.Default.Person, "👔 Staff Reference")
            }
            lower.startsWith("customer:") || lower.contains("(customer)") -> {
                Quadruple(Color(0xFFECFDF5), Color(0xFF047857), Icons.Default.Store, "🏪 Customer Reference")
            }
            lower.startsWith("supplier:") || lower.contains("(supplier)") -> {
                Quadruple(Color(0xFFF5F3FF), Color(0xFF6D28D9), Icons.Default.Business, "🏭 Supplier / Mill Reference")
            }
            value.isNotBlank() -> {
                Quadruple(Color(0xFFFFFBEB), Color(0xFFB45309), Icons.Default.Edit, "✍️ Broker / Introducer")
            }
            else -> {
                Quadruple(Color(0xFFF8FAFC), Color(0xFF64748B), Icons.Default.Link, "Link Master Referrer")
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = NavyPrimary
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (value.isNotBlank()) badgeBg else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (value.isNotBlank()) badgeFg.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { showDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        entityIcon,
                        contentDescription = null,
                        tint = if (value.isNotBlank()) badgeFg else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        if (value.isNotBlank()) {
                            Text(
                                text = displayTitle,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeFg
                            )
                            Text(
                                text = value,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        } else {
                            Text(
                                text = "Tap to select Staff, Customer, Supplier or Broker...",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (value.isNotBlank()) {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NavyPrimary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = if (value.isNotBlank()) "Change" else "Select",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
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
            onSelect = { onValueChange(it) },
            onDismiss = { showDialog = false }
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
