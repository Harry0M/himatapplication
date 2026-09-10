package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.GarmentItemEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SupplierEntity
import com.example.util.RecordValidator
import com.example.util.ValidationResult
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ManufacturerBadge
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WholesalerBadge

@Composable
fun AddEditCustomerDialog(
    customer: CustomerEntity? = null,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var customerId by remember { mutableStateOf(customer?.customerId ?: "CUST-${(100..999).random()}") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var city by remember { mutableStateOf(customer?.city ?: "") }
    var gstin by remember { mutableStateOf(customer?.gstin ?: "") }
    var creditDays by remember { mutableStateOf((customer?.creditDays ?: 30).toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (customer == null) "New Customer Master" else "Edit Customer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer / Retailer Name *") },
                    placeholder = { Text("e.g. Rajesh Garments") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customerId,
                        onValueChange = { customerId = it },
                        label = { Text("Customer ID") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City / Wholesale Market Area *") },
                    placeholder = { Text("e.g. Karol Bagh, Delhi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Full Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it.uppercase() },
                        label = { Text("GSTIN (Optional)") },
                        placeholder = { Text("07AAAAA0000A1Z5") },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = creditDays,
                        onValueChange = { creditDays = it },
                        label = { Text("Credit Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.7f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    CustomerEntity(
                                        id = customer?.id ?: 0L,
                                        customerId = customerId.trim(),
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        address = address.trim(),
                                        city = city.trim(),
                                        gstin = gstin.trim(),
                                        creditDays = creditDays.toIntOrNull() ?: 30
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save Customer")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditSupplierDialog(
    supplier: SupplierEntity? = null,
    onDismiss: () -> Unit,
    onSave: (SupplierEntity) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var supplierId by remember { mutableStateOf(supplier?.supplierId ?: "SUP-${(100..999).random()}") }
    var type by remember { mutableStateOf(supplier?.type ?: "Manufacturer") } // Explicitly single select Manufacturer/Wholesaler
    var brand by remember { mutableStateOf(supplier?.brand ?: "") }
    var gstin by remember { mutableStateOf(supplier?.gstin ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var marketArea by remember { mutableStateOf(supplier?.marketArea ?: "") }
    var contactPerson by remember { mutableStateOf(supplier?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var city by remember { mutableStateOf(supplier?.city ?: "") }
    var categories by remember { mutableStateOf(supplier?.categories ?: "") }
    var defaultCaseSize by remember { mutableStateOf((supplier?.defaultCaseSize ?: 24).toString()) }

    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val commonCategories = listOf("Denim", "Cotton Shirting", "Rayon", "Hosiery Knits", "Kurtis", "Shirts", "Linen", "Silk", "T-Shirts", "Sarees")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (supplier == null) "New Supplier Master" else "Edit Supplier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (validationErrors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Validation Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Incomplete Supplier Record:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            validationErrors.forEach { err ->
                                Text(
                                    text = "• $err",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Explicit Single-Select Type: Manufacturer or Wholesaler
                Text(
                    text = "Supplier Type (Explicitly Required):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val isMfr = type == "Manufacturer"
                    Surface(
                        color = if (isMfr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { type = "Manufacturer" }
                            .defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text(
                            text = "🏭 Manufacturer",
                            color = if (isMfr) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                        )
                    }

                    val isWholesale = type == "Wholesaler"
                    Surface(
                        color = if (isWholesale) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { type = "Wholesaler" }
                            .defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text(
                            text = "🏪 Wholesaler",
                            color = if (isWholesale) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supplier Name *") },
                    placeholder = { Text("e.g. Vardhman Textiles") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand (Optional)") },
                        placeholder = { Text("e.g. V-Denim") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = defaultCaseSize,
                        onValueChange = { defaultCaseSize = it },
                        label = { Text("Case Size (Pcs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = marketArea,
                    onValueChange = { marketArea = it },
                    label = { Text("Market / Area (Where they sit) *") },
                    placeholder = { Text("e.g. Surat Ring Road / Tank Road") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fabric / Garment Categories Provided
                OutlinedTextField(
                    value = categories,
                    onValueChange = { categories = it },
                    label = { Text("Fabric & Garment Categories Provided") },
                    placeholder = { Text("e.g. Denim, Cotton Shirting, Rayon, Kurtis") },
                    supportingText = { Text("Comma-separated categories provided by this supplier") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Category suggestion chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonCategories.forEach { cat ->
                        val isAdded = categories.contains(cat, ignoreCase = true)
                        Surface(
                            shape = CircleShape,
                            color = if (isAdded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    if (!isAdded) {
                                        categories = if (categories.isBlank()) cat else "$categories, $cat"
                                    }
                                }
                        ) {
                            Text(
                                text = if (isAdded) "✓ $cat" else "+ $cat",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isAdded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it.uppercase() },
                    label = { Text("Supplier GSTIN") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Shop / Factory Address") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        placeholder = { Text("e.g. Surat") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.8f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            hasAttemptedSubmit = true
                            val candidate = SupplierEntity(
                                id = supplier?.id ?: 0L,
                                supplierId = supplierId.trim(),
                                name = name.trim(),
                                type = type,
                                brand = brand.trim(),
                                gstin = gstin.trim(),
                                address = address.trim(),
                                city = city.trim(),
                                marketArea = marketArea.trim(),
                                contactPerson = contactPerson.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                categories = categories.trim(),
                                defaultCaseSize = defaultCaseSize.toIntOrNull() ?: 0
                            )
                            val validation = RecordValidator.validateSupplier(candidate)
                            if (validation is ValidationResult.Invalid) {
                                validationErrors = validation.errors
                            } else {
                                validationErrors = emptyList()
                                onSave(candidate)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Save Supplier", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditEmployeeDialog(
    employee: EmployeeEntity? = null,
    onDismiss: () -> Unit,
    onSave: (EmployeeEntity) -> Unit
) {
    var name by remember { mutableStateOf(employee?.name ?: "") }
    var employeeId by remember { mutableStateOf(employee?.employeeId ?: "EMP-0${(1..9).random()}") }
    var phone by remember { mutableStateOf(employee?.phone ?: "") }
    var role by remember { mutableStateOf(employee?.role ?: "Salesman") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (employee == null) "New Employee Master" else "Edit Employee",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Employee / Salesman Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = employeeId,
                        onValueChange = { employeeId = it },
                        label = { Text("ID") },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Role:", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val isAdmin = role == "Admin"
                    Surface(
                        color = if (isAdmin) NavyPrimary else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { role = "Admin" }
                    ) {
                        Text(
                            text = "👑 Admin (Owner)",
                            color = if (isAdmin) Color.White else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                        )
                    }

                    val isSalesman = role == "Salesman"
                    Surface(
                        color = if (isSalesman) GoldAccent else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { role = "Salesman" }
                    ) {
                        Text(
                            text = "💼 Salesman",
                            color = if (isSalesman) NavyPrimary else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    EmployeeEntity(
                                        id = employee?.id ?: 0L,
                                        employeeId = employeeId.trim(),
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        role = role
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save Employee")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVisitDialog(
    customers: List<CustomerEntity>,
    employees: List<EmployeeEntity>,
    defaultEmployee: EmployeeEntity?,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity, EmployeeEntity, String) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var selectedEmployee by remember { mutableStateOf(defaultEmployee ?: employees.firstOrNull()) }
    var notes by remember { mutableStateOf("") }

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var employeeDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Market Visit",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "A visit links a retailer, today's date, and your salesman for tracking multiple wholesaler stops.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Customer Dropdown
                ExposedDropdownMenuBox(
                    expanded = customerDropdownExpanded,
                    onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.let { "${it.name} (${it.city})" } ?: "Select Customer",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Customer / Retailer *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false }
                    ) {
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.SemiBold)
                                        Text(cust.city, fontSize = 11.sp, color = TextSecondary)
                                    }
                                },
                                onClick = {
                                    selectedCustomer = cust
                                    customerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Salesman Dropdown
                ExposedDropdownMenuBox(
                    expanded = employeeDropdownExpanded,
                    onExpandedChange = { employeeDropdownExpanded = !employeeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedEmployee?.name ?: "Select Salesman",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assigned Salesman *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = employeeDropdownExpanded,
                        onDismissRequest = { employeeDropdownExpanded = false }
                    ) {
                        employees.forEach { emp ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(emp.name, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("(${emp.role})", fontSize = 11.sp, color = TextSecondary)
                                    }
                                },
                                onClick = {
                                    selectedEmployee = emp
                                    employeeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Trip Purpose / Notes") },
                    placeholder = { Text("e.g. Wholesale market tour for festive denim & kurti stock") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cust = selectedCustomer
                            val emp = selectedEmployee
                            if (cust != null && emp != null) {
                                onSave(cust, emp, notes.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        enabled = selectedCustomer != null && selectedEmployee != null
                    ) {
                        Text("Start Market Visit")
                    }
                }
            }
        }
    }
}

@Composable
fun RoleSwitcherDialog(
    currentRole: String,
    currentEmployee: EmployeeEntity?,
    employees: List<EmployeeEntity>,
    onDismiss: () -> Unit,
    onSelectRole: (role: String, employee: EmployeeEntity?) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Switch User Role",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Switch between Admin mode (Owner full access) and Field Salesman view.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Admin option
                val isAdmin = currentRole == "Admin"
                Surface(
                    color = if (isAdmin) NavyPrimary.copy(alpha = 0.1f) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isAdmin) 1.5.dp else 1.dp,
                        if (isAdmin) NavyPrimary else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectRole("Admin", null)
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👑", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Admin (Owner)", fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Text("Full access: Masters, all visits, all reports, settings", fontSize = 11.5.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Or select field salesman profile:", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))

                employees.filter { it.role == "Salesman" || it.name.contains("Salesman", ignoreCase = true) }.forEach { emp ->
                    val isCurrent = currentRole == "Salesman" && currentEmployee?.id == emp.id
                    Surface(
                        color = if (isCurrent) GoldAccent.copy(alpha = 0.15f) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isCurrent) 1.5.dp else 1.dp,
                            if (isCurrent) GoldAccent else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectRole("Salesman", emp)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💼", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(emp.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Salesman (${emp.employeeId}) • ${emp.phone}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    product: ProductEntity? = null,
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var productCode by remember { mutableStateOf(product?.productCode ?: "") }
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Apparel") }
    var selectedSupplier by remember {
        mutableStateOf(suppliers.find { it.id == product?.supplierId } ?: suppliers.firstOrNull())
    }
    var supplierExpanded by remember { mutableStateOf(false) }
    var defaultRate by remember { mutableStateOf(product?.defaultRate?.toString() ?: "300") }
    var defaultCaseSize by remember { mutableStateOf(product?.defaultCaseSize?.toString() ?: "24") }
    var hsnCode by remember { mutableStateOf(product?.hsnCode ?: "6203") }
    var description by remember { mutableStateOf(product?.description ?: "") }

    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (product == null) "Add Product" else "Edit Product",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = NavyPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = productCode,
                    onValueChange = { productCode = it.uppercase() },
                    label = { Text("Product / Item Code *") },
                    placeholder = { Text("e.g. DENIM-701, COT-SHIRT") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    placeholder = { Text("e.g. Slim Fit Denim 701") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Supplier Dropdown
                ExposedDropdownMenuBox(
                    expanded = supplierExpanded,
                    onExpandedChange = { supplierExpanded = !supplierExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.name ?: "Select Supplier",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supplier *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = supplierExpanded,
                        onDismissRequest = { supplierExpanded = false }
                    ) {
                        suppliers.forEach { sup ->
                            DropdownMenuItem(
                                text = { Text("${sup.name} (${sup.type})") },
                                onClick = {
                                    selectedSupplier = sup
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hsnCode,
                        onValueChange = { hsnCode = it },
                        label = { Text("HSN Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = defaultRate,
                        onValueChange = { defaultRate = it },
                        label = { Text("Default Rate (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = defaultCaseSize,
                        onValueChange = { defaultCaseSize = it },
                        label = { Text("Case Size (pcs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg, color = Color(0xFFDC2626), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val rateVal = defaultRate.toDoubleOrNull() ?: 0.0
                            val caseVal = defaultCaseSize.toIntOrNull() ?: 0
                            val supplierId = selectedSupplier?.id ?: 0L
                            val supplierName = selectedSupplier?.name ?: ""

                            val saved = (product ?: ProductEntity(
                                productCode = productCode.trim(),
                                name = name.trim(),
                                supplierId = supplierId,
                                supplierName = supplierName,
                                defaultRate = rateVal,
                                defaultCaseSize = caseVal
                            )).copy(
                                productCode = productCode.trim(),
                                name = name.trim(),
                                category = category.trim(),
                                supplierId = supplierId,
                                supplierName = supplierName,
                                defaultRate = rateVal,
                                defaultCaseSize = caseVal,
                                hsnCode = hsnCode.trim(),
                                description = description.trim()
                            )

                            val validation = RecordValidator.validateProduct(saved)
                            if (validation is ValidationResult.Invalid) {
                                errorMsg = validation.errorMessage
                                return@Button
                            }

                            onSave(saved)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text("Save Product", color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGarmentItemDialog(
    item: GarmentItemEntity? = null,
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onSave: (GarmentItemEntity) -> Unit
) {
    var itemCode by remember { mutableStateOf(item?.itemCode ?: "") }
    var name by remember { mutableStateOf(item?.name ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Denim") }
    var selectedSupplier by remember {
        mutableStateOf(suppliers.find { it.id == item?.supplierId } ?: suppliers.firstOrNull())
    }
    var supplierExpanded by remember { mutableStateOf(false) }
    var defaultRate by remember { mutableStateOf(item?.defaultRate?.let { if (it > 0) it.toString() else "" } ?: "") }
    var defaultCaseSize by remember { mutableStateOf((item?.defaultCaseSize ?: 24).toString()) }
    var fabricType by remember { mutableStateOf(item?.fabricType ?: "") }
    var sizeRange by remember { mutableStateOf(item?.sizeRange ?: "28-36") }
    var colorOptions by remember { mutableStateOf(item?.colorOptions ?: "") }
    var hsnCode by remember { mutableStateOf(item?.hsnCode ?: "6203") }
    var gstRate by remember { mutableStateOf((item?.gstRate ?: 5.0).toString()) }
    var inStockPieces by remember { mutableStateOf((item?.inStockPieces ?: 0).toString()) }
    var description by remember { mutableStateOf(item?.description ?: "") }

    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val categories = listOf("Denim", "Kurtis", "Shirts", "Cotton Shirting", "Hosiery Knits", "Ethnic Wear", "Fabrics", "Trousers", "T-Shirts")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item == null) "New Garment Item" else "Edit Garment Item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (validationErrors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Validation Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Incomplete Garment Record:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            validationErrors.forEach { err ->
                                Text(
                                    text = "• $err",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = itemCode,
                        onValueChange = { itemCode = it },
                        label = { Text("Item Code *") },
                        placeholder = { Text("e.g. DENIM-701") },
                        isError = hasAttemptedSubmit && itemCode.trim().length < 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category *") },
                        placeholder = { Text("e.g. Denim") },
                        isError = hasAttemptedSubmit && category.trim().isBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                }

                // Fast category chips
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category.equals(cat, ignoreCase = true)
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Garment Name / Description *") },
                    placeholder = { Text("e.g. Slim Fit Stretch Jeans 701") },
                    isError = hasAttemptedSubmit && name.trim().length < 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Supplier Selector
                ExposedDropdownMenuBox(
                    expanded = supplierExpanded,
                    onExpandedChange = { supplierExpanded = !supplierExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.let { "${it.name} (${it.type})" } ?: "Select Supplier *",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supplier Master *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = supplierExpanded,
                        onDismissRequest = { supplierExpanded = false }
                    ) {
                        suppliers.forEach { supp ->
                            DropdownMenuItem(
                                text = { Text("${supp.name} - ${supp.marketArea.ifBlank { supp.city }}") },
                                onClick = {
                                    selectedSupplier = supp
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = defaultRate,
                        onValueChange = { defaultRate = it },
                        label = { Text("Rate (₹/pc) *") },
                        placeholder = { Text("450") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = hasAttemptedSubmit && ((defaultRate.toDoubleOrNull() ?: 0.0) <= 0.0),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = defaultCaseSize,
                        onValueChange = { defaultCaseSize = it },
                        label = { Text("Case Size (Pcs) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = hasAttemptedSubmit && ((defaultCaseSize.toIntOrNull() ?: 0) <= 0),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fabricType,
                        onValueChange = { fabricType = it },
                        label = { Text("Fabric Type") },
                        placeholder = { Text("e.g. Cotton Spandex") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sizeRange,
                        onValueChange = { sizeRange = it },
                        label = { Text("Size Range") },
                        placeholder = { Text("e.g. 28 to 36") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hsnCode,
                        onValueChange = { hsnCode = it },
                        label = { Text("HSN Code") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inStockPieces,
                        onValueChange = { inStockPieces = it },
                        label = { Text("Current Stock (Pcs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Specs") },
                    placeholder = { Text("e.g. Enzyme washed, 5 pocket styling") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            hasAttemptedSubmit = true
                            val rateVal = defaultRate.toDoubleOrNull() ?: 0.0
                            val caseVal = defaultCaseSize.toIntOrNull() ?: 0
                            val stockVal = inStockPieces.toIntOrNull() ?: 0
                            val gstVal = gstRate.toDoubleOrNull() ?: 5.0
                            val suppId = selectedSupplier?.id ?: 0L
                            val suppName = selectedSupplier?.name ?: ""

                            val candidate = (item ?: GarmentItemEntity(
                                itemCode = itemCode.trim(),
                                name = name.trim(),
                                supplierId = suppId,
                                supplierName = suppName,
                                defaultRate = rateVal,
                                defaultCaseSize = caseVal
                            )).copy(
                                itemCode = itemCode.trim(),
                                name = name.trim(),
                                category = category.trim(),
                                supplierId = suppId,
                                supplierName = suppName,
                                defaultRate = rateVal,
                                defaultCaseSize = caseVal,
                                fabricType = fabricType.trim(),
                                sizeRange = sizeRange.trim(),
                                colorOptions = colorOptions.trim(),
                                hsnCode = hsnCode.trim(),
                                gstRate = gstVal,
                                inStockPieces = stockVal,
                                description = description.trim()
                            )

                            val validation = RecordValidator.validateGarmentItem(candidate)
                            if (validation is ValidationResult.Invalid) {
                                validationErrors = validation.errors
                            } else {
                                validationErrors = emptyList()
                                onSave(candidate)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Save Garment Item", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


