package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.SupplierEntity
import com.example.ui.components.PickerField
import com.example.ui.components.PrimaryButton
import com.example.ui.components.UiDimens
import com.example.ui.theme.TextSecondary
import com.example.util.brandName

enum class NewTripMode { MARKET, PHONE }

/**
 * Start a trip. Two ways, same trip record:
 *
 * - Market visit: the customer comes along, the salesman adds each supplier's order on the way.
 * - Phone order: the customer ordered by phone and named the suppliers, so all suppliers are picked
 *   here and the order form then opens once per supplier.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewTripSheet(
    customers: List<CustomerEntity>,
    employees: List<EmployeeEntity>,
    suppliers: List<SupplierEntity>,
    defaultEmployee: EmployeeEntity?,
    onDismiss: () -> Unit,
    /** customer, salesman, other salesmen, notes */
    onStartMarketTrip: (CustomerEntity, EmployeeEntity, List<EmployeeEntity>, String) -> Unit,
    /** customer, salesman, other salesmen, suppliers in the picked order, notes */
    onStartPhoneOrder: (CustomerEntity, EmployeeEntity, List<EmployeeEntity>, List<SupplierEntity>, String) -> Unit,
    onQuickCreateCustomer: ((CustomerEntity) -> Unit)? = null,
    initialCustomer: CustomerEntity? = null
) {
    val activeStaff = remember(employees) {
        employees.filter {
            !it.isBlocked && !it.isDeleted &&
                !it.status.equals("Deactivated", true) && !it.status.equals("Suspended", true)
        }
    }
    val liveSuppliers = remember(suppliers) { suppliers.filter { !it.isDeleted } }

    var mode by remember { mutableStateOf(NewTripMode.MARKET) }
    var customer by remember { mutableStateOf(initialCustomer) }
    var salesman by remember { mutableStateOf(defaultEmployee) }
    var otherSalesmen by remember { mutableStateOf<List<EmployeeEntity>>(emptyList()) }
    var pickedSuppliers by remember { mutableStateOf<List<SupplierEntity>>(emptyList()) }
    var notes by remember { mutableStateOf("") }

    var showCustomerSheet by remember { mutableStateOf(false) }
    var showSalesmanSheet by remember { mutableStateOf(false) }
    var showOtherSalesmanSheet by remember { mutableStateOf(false) }
    var showSupplierSheet by remember { mutableStateOf(false) }

    val isPhone = mode == NewTripMode.PHONE
    val missing = when {
        customer == null -> "Select a customer"
        salesman == null -> "Select a salesman"
        isPhone && pickedSuppliers.isEmpty() -> "Add at least one supplier"
        else -> null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("new_trip_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = UiDimens.ScreenPadding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "New trip",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )

            // Both options are the same width, so the sheet does not shift when you switch
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == NewTripMode.MARKET,
                    onClick = { mode = NewTripMode.MARKET },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {},
                    label = { Text("Market visit", style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                )
                SegmentedButton(
                    selected = isPhone,
                    onClick = { mode = NewTripMode.PHONE },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {},
                    label = { Text("Phone order", style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                )
            }

            Text(
                text = if (isPhone) {
                    "The customer ordered on the phone. Pick every supplier they named; the order form opens one by one."
                } else {
                    "The customer comes along. Add each supplier's order during the trip."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Customer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showCustomerSheet = true }) { Text("+ New customer") }
            }
            PickerField(
                value = customer?.let { c -> "${c.brandName()}${if (c.city.isNotBlank()) " • ${c.city}" else ""}" },
                placeholder = "Tap to search customer",
                contentDescription = "Customer",
                onClick = { showCustomerSheet = true },
                modifier = Modifier.testTag("new_trip_customer_field")
            )

            Text("Salesman", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            PickerField(
                value = salesman?.name,
                placeholder = "Tap to pick the salesman",
                contentDescription = "Salesman",
                onClick = { showSalesmanSheet = true },
                modifier = Modifier.testTag("new_trip_salesman_field")
            )

            if (isPhone) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Suppliers",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showSupplierSheet = true }) {
                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add suppliers")
                    }
                }
                if (pickedSuppliers.isEmpty()) {
                    Text(
                        text = "One order will be created for each supplier you add.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        pickedSuppliers.forEach { sup ->
                            InputChip(
                                selected = true,
                                onClick = { pickedSuppliers = pickedSuppliers.filter { it.id != sup.id } },
                                label = { Text(sup.brandName(), style = MaterialTheme.typography.labelLarge) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove ${sup.brandName()}",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Other salesmen (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showOtherSalesmanSheet = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
            if (otherSalesmen.isEmpty()) {
                Text(
                    text = "They can also join later from their own phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    otherSalesmen.forEach { emp ->
                        InputChip(
                            selected = true,
                            onClick = { otherSalesmen = otherSalesmen.filter { it.id != emp.id } },
                            label = { Text(emp.name, style = MaterialTheme.typography.labelLarge) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove ${emp.name}",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (missing != null) {
                Text(
                    text = missing,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            PrimaryButton(
                text = if (isPhone) "Start phone order" else "Start trip",
                onClick = {
                    val c = customer ?: return@PrimaryButton
                    val s = salesman ?: return@PrimaryButton
                    if (isPhone) {
                        if (pickedSuppliers.isEmpty()) return@PrimaryButton
                        onStartPhoneOrder(c, s, otherSalesmen, pickedSuppliers, notes)
                    } else {
                        onStartMarketTrip(c, s, otherSalesmen, notes)
                    }
                },
                enabled = missing == null,
                modifier = Modifier.fillMaxWidth().testTag("new_trip_start_button")
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    if (showCustomerSheet) {
        CustomerSearchBottomSheet(
            customers = customers,
            selectedCustomer = customer,
            onSelectCustomer = {
                customer = it
                showCustomerSheet = false
            },
            onDismiss = { showCustomerSheet = false },
            onQuickCreateCustomer = onQuickCreateCustomer
        )
    }

    if (showSalesmanSheet) {
        EmployeeSearchBottomSheet(
            employees = activeStaff,
            selectedEmployee = salesman,
            onSelectEmployee = {
                salesman = it
                otherSalesmen = otherSalesmen.filter { o -> o.id != it.id }
                showSalesmanSheet = false
            },
            onDismiss = { showSalesmanSheet = false }
        )
    }

    if (showOtherSalesmanSheet) {
        EmployeeSearchBottomSheet(
            employees = activeStaff.filter { e -> e.id != salesman?.id && otherSalesmen.none { it.id == e.id } },
            selectedEmployee = null,
            onSelectEmployee = { emp ->
                otherSalesmen = otherSalesmen + emp
                showOtherSalesmanSheet = false
            },
            onDismiss = { showOtherSalesmanSheet = false }
        )
    }

    if (showSupplierSheet) {
        SupplierMultiSelectSheet(
            suppliers = liveSuppliers,
            selectedIds = pickedSuppliers.map { it.id },
            onDone = { ids ->
                // Keep the order the user ticked them in
                pickedSuppliers = ids.mapNotNull { id -> liveSuppliers.firstOrNull { it.id == id } }
                showSupplierSheet = false
            },
            onDismiss = { showSupplierSheet = false }
        )
    }
}
