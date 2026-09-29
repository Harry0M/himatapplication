package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.google.firebase.auth.FirebaseUser
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.GarmentItemEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SupplierEntity
import com.example.util.IdGenerator
import com.example.util.RecordValidator
import com.example.util.ValidationResult
import com.example.util.brandName
import androidx.compose.material3.InputChip
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
    var transportPreference by remember { mutableStateOf(customer?.transportPreference ?: "") }
    var preferredTransporterName by remember { mutableStateOf(customer?.preferredTransporterName ?: "") }

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
                        style = MaterialTheme.typography.titleLarge,
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

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Transport / Delivery Details",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = preferredTransporterName,
                    onValueChange = { preferredTransporterName = it },
                    label = { Text("Preferred Transporter (Optional)") },
                    placeholder = { Text("e.g. V-Trans, ARC, GATI") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                com.example.ui.components.StationPickerField(
                    value = transportPreference,
                    onValueChange = { transportPreference = it },
                    label = "Booking Station / Delivery Station",
                    modifier = Modifier.fillMaxWidth()
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
                                        creditDays = creditDays.toIntOrNull() ?: 30,
                                        transportPreference = transportPreference.trim(),
                                        preferredTransporterName = preferredTransporterName.trim()
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
                            text = "Manufacturer",
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
                            text = "Wholesaler",
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

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand (Optional)") },
                    placeholder = { Text("e.g. V-Denim") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    singleLine = true
                )

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
    var email by remember { mutableStateOf(employee?.email ?: "") }
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
                        style = MaterialTheme.typography.titleLarge,
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

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Google Account Email (for Login)") },
                    placeholder = { Text("e.g. rahul.salesman@gmail.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Role:", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontWeight = FontWeight.SemiBold)
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
                            text = "Admin (Owner)",
                            color = if (isAdmin) Color.White else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
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
                            text = "Salesman",
                            color = if (isSalesman) NavyPrimary else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
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
                                        role = role,
                                        email = email.trim().lowercase()
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

@Composable
fun QuickAddCustomerDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var firmName by remember { mutableStateOf(initialName) }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Ahmedabad") }
    var customerType by remember { mutableStateOf("Credit") }
    var creditDays by remember { mutableStateOf("30") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NavyPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Add Customer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "Add a new retailer on the fly. You can complete full KYC later in Customer Masters.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = firmName,
                    onValueChange = { firmName = it; errorMessage = null },
                    label = { Text("Shop / Firm Name *", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("e.g. Radhe Krishna Fashion", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner / Contact Person", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("e.g. Ramesh Patel", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMessage = null },
                    label = { Text("Mobile Number *", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("10-digit mobile number", style = MaterialTheme.typography.bodySmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City *", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Customer Type Toggle (Credit / Cash)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Credit", "Cash").forEach { type ->
                        val isSelected = customerType.equals(type, ignoreCase = true)
                        Surface(
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { customerType = type }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (type == "Credit") "💳 Credit" else "💵 Cash",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }

                if (customerType.equals("Credit", ignoreCase = true)) {
                    OutlinedTextField(
                        value = creditDays,
                        onValueChange = { creditDays = it },
                        label = { Text("Credit Days", style = MaterialTheme.typography.bodySmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanFirm = firmName.trim()
                            val cleanOwner = ownerName.trim()
                            val cleanPhone = phone.trim()
                            val cleanCity = city.trim().ifBlank { "Ahmedabad" }
                            if (cleanFirm.isBlank() && cleanOwner.isBlank()) {
                                errorMessage = "Please enter Shop/Firm or Owner Name"
                                return@Button
                            }
                            if (cleanPhone.isBlank()) {
                                errorMessage = "Please enter Mobile Number"
                                return@Button
                            }
                            val effectiveName = cleanOwner.ifBlank { cleanFirm }
                            val effectiveFirm = cleanFirm.ifBlank { cleanOwner }
                            val newCustomer = CustomerEntity(
                                // Real id right away: a trip started with this customer must link to it
                                id = IdGenerator.newId(),
                                customerId = "CUST-${(100..999).random()}",
                                name = effectiveName,
                                firmName = effectiveFirm,
                                phone = cleanPhone,
                                city = cleanCity,
                                customerType = customerType,
                                creditDays = creditDays.toIntOrNull() ?: 30,
                                notes = "Quick created during Market Visit creation"
                            )
                            onSave(newCustomer)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save & Select", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CreateVisitDialog(
    customers: List<CustomerEntity>,
    employees: List<EmployeeEntity>,
    defaultEmployee: EmployeeEntity?,
    onDismiss: () -> Unit,
    /** customer, trip starter, other salesmen going along, notes */
    onSave: (CustomerEntity, EmployeeEntity, List<EmployeeEntity>, String) -> Unit,
    onQuickCreateCustomer: ((CustomerEntity) -> Unit)? = null,
    initialCustomer: CustomerEntity? = null
) {
    val activeStaff = remember(employees) {
        employees.filter { !it.isBlocked && !it.isDeleted && !it.status.equals("Deactivated", true) && !it.status.equals("Suspended", true) }
    }
    // No silent default customer: the user must pick who the trip is for
    var selectedCustomer by remember { mutableStateOf(initialCustomer) }
    var selectedEmployee by remember { mutableStateOf(defaultEmployee?.takeIf { d -> activeStaff.any { it.id == d.id } } ?: defaultEmployee) }
    var otherSalesmen by remember { mutableStateOf<List<EmployeeEntity>>(emptyList()) }
    var notes by remember { mutableStateOf("") }

    var showCustomerSheet by remember { mutableStateOf(false) }
    var showEmployeeSheet by remember { mutableStateOf(false) }
    var showOtherSalesmanSheet by remember { mutableStateOf(false) }
    var showQuickAddCustomer by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Trip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Pick the customer and who takes them to the market. Other salesmen can also join later from their own phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("1. Customer *", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                    TextButton(onClick = { showQuickAddCustomer = true }) {
                        Text("+ New customer", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCustomerSheet = true }
                        .testTag("create_visit_customer_field")
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.let { c -> "${c.firmName.ifBlank { c.name }} (${c.city})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        placeholder = { Text("Tap to search customer", style = MaterialTheme.typography.bodyMedium) },
                        trailingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search customer", tint = NavyPrimary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            disabledTrailingIconColor = NavyPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("2. Salesman *", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showEmployeeSheet = true }
                        .testTag("create_visit_employee_field")
                ) {
                    OutlinedTextField(
                        value = selectedEmployee?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        placeholder = { Text("Tap to pick the salesman", style = MaterialTheme.typography.bodyMedium) },
                        trailingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search salesman", tint = NavyPrimary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            disabledTrailingIconColor = NavyPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3. Other salesmen (optional)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                    TextButton(onClick = { showOtherSalesmanSheet = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
                if (otherSalesmen.isEmpty()) {
                    Text("Solo trip. Add salesmen who go along, or they can join later.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        otherSalesmen.forEach { emp ->
                            InputChip(
                                selected = true,
                                onClick = { otherSalesmen = otherSalesmen.filter { it.id != emp.id } },
                                label = { Text(emp.name, style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove ${emp.name}", modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("e.g. Festive denim stock", style = MaterialTheme.typography.bodyMedium) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cust = selectedCustomer
                            val emp = selectedEmployee
                            if (cust != null && emp != null) {
                                onSave(cust, emp, otherSalesmen.filter { it.id != emp.id }, notes.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 44.dp),
                        enabled = selectedCustomer != null && selectedEmployee != null
                    ) {
                        Text("Start Trip", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCustomerSheet) {
        CustomerSearchBottomSheet(
            customers = customers,
            selectedCustomer = selectedCustomer,
            onSelectCustomer = { customer ->
                selectedCustomer = customer
                showCustomerSheet = false
            },
            onDismiss = { showCustomerSheet = false },
            onQuickCreateCustomer = { newCust ->
                // Give the new customer its real id now, so the trip links to it even before sync finishes
                val withId = if (newCust.id == 0L) newCust.copy(id = IdGenerator.newId()) else newCust
                onQuickCreateCustomer?.invoke(withId)
                selectedCustomer = withId
                showCustomerSheet = false
            }
        )
    }

    if (showQuickAddCustomer) {
        QuickAddCustomerDialog(
            initialName = "",
            onDismiss = { showQuickAddCustomer = false },
            onSave = { newCust ->
                val withId = if (newCust.id == 0L) newCust.copy(id = IdGenerator.newId()) else newCust
                onQuickCreateCustomer?.invoke(withId)
                selectedCustomer = withId
                showQuickAddCustomer = false
            }
        )
    }

    if (showEmployeeSheet) {
        EmployeeSearchBottomSheet(
            employees = activeStaff,
            selectedEmployee = selectedEmployee,
            onSelectEmployee = { employee ->
                selectedEmployee = employee
                otherSalesmen = otherSalesmen.filter { it.id != employee.id }
                showEmployeeSheet = false
            },
            onDismiss = { showEmployeeSheet = false }
        )
    }

    if (showOtherSalesmanSheet) {
        EmployeeSearchBottomSheet(
            employees = activeStaff.filter { e -> e.id != selectedEmployee?.id && otherSalesmen.none { it.id == e.id } },
            selectedEmployee = null,
            onSelectEmployee = { employee ->
                otherSalesmen = otherSalesmen + employee
                showOtherSalesmanSheet = false
            },
            onDismiss = { showOtherSalesmanSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSearchBottomSheet(
    customers: List<CustomerEntity>,
    selectedCustomer: CustomerEntity?,
    onSelectCustomer: (CustomerEntity) -> Unit,
    onDismiss: () -> Unit,
    onQuickCreateCustomer: ((CustomerEntity) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    val cleanQuery = searchQuery.trim()
    var showQuickAddCustomer by remember { mutableStateOf(false) }

    val filteredCustomers = remember(customers, cleanQuery) {
        if (cleanQuery.isBlank()) customers
        else {
            customers.filter {
                it.name.contains(cleanQuery, ignoreCase = true) ||
                    it.firmName.contains(cleanQuery, ignoreCase = true) ||
                    it.city.contains(cleanQuery, ignoreCase = true) ||
                    it.phone.contains(cleanQuery, ignoreCase = true) ||
                    it.address.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("customer_search_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header with Title & Count Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Select Customer / Retailer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose retailer for this market visit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${filteredCustomers.size} available",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar with Realtime Filtering
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search by name, city, phone...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NavyPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("customer_search_input")
            )

            if (onQuickCreateCustomer != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (cleanQuery.isBlank()) "Don't see retailer listed?" else "Can't find \"$cleanQuery\"?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showQuickAddCustomer = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, NavyPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (cleanQuery.isNotBlank()) "Add \"$cleanQuery\"" else "+ Create New",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No customers found matching \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onQuickCreateCustomer != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showQuickAddCustomer = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (cleanQuery.isNotBlank()) "Add \"$cleanQuery\" Now" else "Create New Customer",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        val isSelected = selectedCustomer?.id == customer.id
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NavyPrimary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NavyPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectCustomer(customer) }
                                .testTag("customer_item_${customer.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) NavyPrimary else NavyPrimary.copy(alpha = 0.1f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = customer.brandName().take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSelected) Color.White else NavyPrimary
                                        )
                                    }
                                    Column {
                                        // Brand (shop / firm) name first; owner name goes in the details line
                                        Text(
                                            text = customer.brandName(),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val details = listOfNotNull(
                                            customer.name.trim().takeIf { it.isNotBlank() && !it.equals(customer.brandName(), ignoreCase = true) },
                                            customer.city.takeIf { it.isNotBlank() },
                                            customer.phone.takeIf { it.isNotBlank() },
                                            customer.address.takeIf { it.isNotBlank() }
                                        ).joinToString(" • ")

                                        if (details.isNotBlank()) {
                                            Text(
                                                text = details,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuickAddCustomer) {
        QuickAddCustomerDialog(
            initialName = cleanQuery,
            onDismiss = { showQuickAddCustomer = false },
            onSave = { newCust ->
                // Same id for "save" and "select"; before, the trip got the unsaved copy with id 0
                // and its orders never showed in the customer master
                val withId = if (newCust.id == 0L) newCust.copy(id = IdGenerator.newId()) else newCust
                onQuickCreateCustomer?.invoke(withId)
                onSelectCustomer(withId)
                showQuickAddCustomer = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeSearchBottomSheet(
    employees: List<EmployeeEntity>,
    selectedEmployee: EmployeeEntity?,
    onSelectEmployee: (EmployeeEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val cleanQuery = searchQuery.trim()

    val filteredEmployees = remember(employees, cleanQuery) {
        if (cleanQuery.isBlank()) employees
        else {
            employees.filter {
                it.name.contains(cleanQuery, ignoreCase = true) ||
                    it.role.contains(cleanQuery, ignoreCase = true) ||
                    it.phone.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("employee_search_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header with Title & Count Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Select Salesman / Agent",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Assign representative leading this trip",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${filteredEmployees.size} available",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar with Realtime Filtering
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search by name, role, phone...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NavyPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("employee_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Employee List
            if (filteredEmployees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No salesmen found matching \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredEmployees, key = { it.id }) { employee ->
                        val isSelected = selectedEmployee?.id == employee.id
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NavyPrimary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NavyPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectEmployee(employee) }
                                .testTag("employee_item_${employee.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) NavyPrimary else NavyPrimary.copy(alpha = 0.1f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = employee.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSelected) Color.White else NavyPrimary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = employee.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = employee.role,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (employee.phone.isNotBlank()) {
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = employee.phone,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDialog(
    currentRole: String,
    currentEmployee: EmployeeEntity?,
    employees: List<EmployeeEntity>,
    currentUser: FirebaseUser? = null,
    isSuperAdmin: Boolean = true,
    onSignOut: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSelectRole: (role: String, employee: EmployeeEntity?) -> Unit
) {
    val isAdmin = currentRole == "Admin"
    val activeTitle = if (isSuperAdmin) {
        if (isAdmin) "Himat Textile Owner (Super Admin)" else (currentEmployee?.name ?: "Salesman (Simulated)")
    } else {
        currentEmployee?.name ?: "Salesman"
    }
    val activeRoleLabel = if (isSuperAdmin) {
        if (isAdmin) "Agency Owner (Super Admin)" else "Field Salesman (Simulated View)"
    } else {
        "Field Salesman • Account Locked"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "User Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Himat Textile Agency • Ahmedabad",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Profile Hero Banner (Borderless, Rich Styling)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isAdmin) NavyPrimary else Color(0xFF059669)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeTitle,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeRoleLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isAdmin) NavyPrimary else Color(0xFF059669)
                            )
                        }
                    }
                }

                if (isSuperAdmin) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Switch Account / Role",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select an operational mode to manage visits and entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Admin / Owner Option
                    Surface(
                        color = if (isAdmin) NavyPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectRole("Admin", null)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NavyPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Agency Owner (Admin)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Full operations: Masters, Ledger, All Visits & Reports",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isAdmin) {
                                Surface(
                                    shape = CircleShape,
                                    color = NavyPrimary
                                ) {
                                    Text(
                                        text = "Active",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Field Salesmen Profiles List
                    Text(
                        text = "Or operate as Field Salesman:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val salesmen = employees.filter { it.role == "Salesman" || it.name.contains("Salesman", ignoreCase = true) }
                    if (salesmen.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No salesman accounts created yet in Employee Master.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        salesmen.forEach { emp ->
                            val isCurrent = !isAdmin && currentEmployee?.id == emp.id
                            Surface(
                                color = if (isCurrent) Color(0xFF059669).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onSelectRole("Salesman", emp)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF059669).copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = emp.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val empSub = if (emp.phone.isNotBlank()) "ID: ${emp.employeeId} • ${emp.phone}" else "ID: ${emp.employeeId}"
                                        Text(
                                            text = empSub,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isCurrent) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF059669)
                                        ) {
                                            Text(
                                                text = "Active",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Regular Employee Information Notice
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Role Managed by Super Admin",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Your account permissions are locked to your assigned field profile.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Firebase / Google Account & Sign Out Section
                if (currentUser != null || onSignOut != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.6.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connected Google Account",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentUser?.email ?: "Signed in",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }

                        if (onSignOut != null) {
                            TextButton(
                                onClick = onSignOut,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sign Out",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Backward-compatible alias for existing usages
@Composable
fun RoleSwitcherDialog(
    currentRole: String,
    currentEmployee: EmployeeEntity?,
    employees: List<EmployeeEntity>,
    currentUser: FirebaseUser? = null,
    isSuperAdmin: Boolean = true,
    onSignOut: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSelectRole: (role: String, employee: EmployeeEntity?) -> Unit
) {
    ProfileDialog(
        currentRole = currentRole,
        currentEmployee = currentEmployee,
        employees = employees,
        currentUser = currentUser,
        isSuperAdmin = isSuperAdmin,
        onSignOut = onSignOut,
        onDismiss = onDismiss,
        onSelectRole = onSelectRole
    )
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
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
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
                        style = MaterialTheme.typography.titleSmall,
                        color = NavyPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = productCode,
                    onValueChange = { productCode = it.uppercase() },
                    label = { Text("Product / Item Code *", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("e.g. DENIM-701, COT-SHIRT", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("e.g. Slim Fit Denim 701", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        label = { Text("Supplier *", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(10.dp),
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
                                text = { Text("${sup.name} (${sup.type})", style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    selectedSupplier = sup
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hsnCode,
                        onValueChange = { hsnCode = it },
                        label = { Text("HSN Code", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaultRate,
                        onValueChange = { defaultRate = it },
                        label = { Text("Default Rate (₹)", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = defaultCaseSize,
                        onValueChange = { defaultCaseSize = it },
                        label = { Text("Case Size (pcs)", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes", style = MaterialTheme.typography.labelSmall) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMsg, color = Color(0xFFDC2626), style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                    ) {
                        Text("Cancel", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text("Save Product", color = GoldAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
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
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item == null) "New Garment Item" else "Edit Garment Item",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                if (validationErrors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Validation Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Incomplete Garment Record:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            validationErrors.forEach { err ->
                                Text(
                                    text = "• $err",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = itemCode,
                        onValueChange = { itemCode = it },
                        label = { Text("Item Code *", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("e.g. DENIM-701", style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        isError = hasAttemptedSubmit && itemCode.trim().length < 2,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category *", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("e.g. Denim", style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        isError = hasAttemptedSubmit && category.trim().isBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
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
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Garment Name / Description *", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("e.g. Slim Fit Stretch Jeans 701", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    isError = hasAttemptedSubmit && name.trim().length < 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Supplier Selector
                ExposedDropdownMenuBox(
                    expanded = supplierExpanded,
                    onExpandedChange = { supplierExpanded = !supplierExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.let { "${it.name} (${it.type})" } ?: "Select Supplier *",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supplier Master *", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                        shape = RoundedCornerShape(10.dp),
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
                                text = { Text("${supp.name} - ${supp.marketArea.ifBlank { supp.city }}", style = MaterialTheme.typography.bodySmall) },
                                onClick = {
                                    selectedSupplier = supp
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaultRate,
                        onValueChange = { defaultRate = it },
                        label = { Text("Rate (₹/pc) *", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("450", style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = hasAttemptedSubmit && ((defaultRate.toDoubleOrNull() ?: 0.0) <= 0.0),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = defaultCaseSize,
                        onValueChange = { defaultCaseSize = it },
                        label = { Text("Case Size (Pcs) *", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = hasAttemptedSubmit && ((defaultCaseSize.toIntOrNull() ?: 0) <= 0),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fabricType,
                        onValueChange = { fabricType = it },
                        label = { Text("Fabric Type", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("e.g. Cotton Spandex", style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sizeRange,
                        onValueChange = { sizeRange = it },
                        label = { Text("Size Range", style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("e.g. 28 to 36", style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hsnCode,
                        onValueChange = { hsnCode = it },
                        label = { Text("HSN Code", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inStockPieces,
                        onValueChange = { inStockPieces = it },
                        label = { Text("Current Stock (Pcs)", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Specs", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("e.g. Enzyme washed, 5 pocket styling", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.bodySmall)
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                    ) {
                        Text("Save Garment Item", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


