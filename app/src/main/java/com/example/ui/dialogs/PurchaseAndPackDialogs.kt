package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.ui.components.DeliveryDaysSelector
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.PackGroupEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPurchaseEntryDialog(
    visitId: Long,
    suppliers: List<SupplierEntity>,
    historyItemCodes: List<String>,
    onDismiss: () -> Unit,
    onOpenMixedPack: () -> Unit,
    onSave: (
        supplier: SupplierEntity,
        itemCode: String,
        pieces: Int,
        rate: Double,
        caseSize: Int,
        caseCount: Int,
        loosePieces: Int,
        gstRate: Double,
        expectedDeliveryDate: String,
        transporter: String
    ) -> Unit
) {
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var itemCode by remember { mutableStateOf("") }
    var piecesText by remember { mutableStateOf("") }
    var casesText by remember { mutableStateOf("") }
    var looseText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var caseSizeText by remember { mutableStateOf((selectedSupplier?.defaultCaseSize ?: 24).toString()) }
    var gstRateText by remember { mutableStateOf("5.0") }

    val defaultDate = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 7)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    var expectedDeliveryDate by remember { mutableStateOf(defaultDate) }
    var transporter by remember { mutableStateOf("") }
    var hasOrderFormPhoto by remember { mutableStateOf(false) }
    var hasSupplierInvoicePhoto by remember { mutableStateOf(false) }

    var showSupplierSheet by remember { mutableStateOf(false) }

    // Live Calculations
    val pieces = piecesText.toIntOrNull() ?: 0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val caseSize = caseSizeText.toIntOrNull() ?: 24
    val enteredCases = casesText.toIntOrNull()
    val enteredLoose = looseText.toIntOrNull()
    val caseCount = enteredCases ?: (if (caseSize > 0) pieces / caseSize else 0)
    val loosePieces = enteredLoose ?: (if (caseSize > 0) pieces % caseSize else 0)
    val totalAmount = pieces * rate
    val isIncomplete = loosePieces > 0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    Column {
                        Text(
                            text = "Purchase Entry (Supplier Stop)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Logged on the spot while escorting client",
                            fontSize = 10.5.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Supplier Selection Picker (Tap opens Search Bottom Sheet)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showSupplierSheet = true }
                        .testTag("entry_supplier_picker")
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.let { "${it.name} (${it.type} • ${it.marketArea})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Supplier / Wholesaler Stop *", fontSize = 11.sp) },
                        placeholder = { Text("Tap to search & choose supplier", fontSize = 11.5.sp) },
                        textStyle = TextStyle(fontSize = 12.5.sp),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Supplier",
                                tint = NavyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            disabledTrailingIconColor = NavyPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Item Code with Autocomplete chips
                OutlinedTextField(
                    value = itemCode,
                    onValueChange = { itemCode = it.uppercase() },
                    label = { Text("Item / Style Code *", fontSize = 11.sp) },
                    placeholder = { Text("e.g. ABC, XYZ, KURTI-102, JEANS-88", fontSize = 11.5.sp) },
                    textStyle = TextStyle(fontSize = 12.5.sp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Quick suggestions from history
                if (historyItemCodes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Recent Items:", fontSize = 10.sp, color = TextSecondary)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        historyItemCodes.take(5).forEach { code ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { itemCode = code }
                            ) {
                                Text(
                                    text = code,
                                    fontSize = 10.sp,
                                    color = NavyPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 1: Pieces & Rate
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = piecesText,
                        onValueChange = { piecesText = it },
                        label = { Text("Total Pieces (Pc) *", fontSize = 11.sp) },
                        placeholder = { Text("75", fontSize = 11.5.sp) },
                        textStyle = TextStyle(fontSize = 12.5.sp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Rate (₹/Pc) *", fontSize = 11.sp) },
                        placeholder = { Text("420", fontSize = 11.5.sp) },
                        textStyle = TextStyle(fontSize = 12.5.sp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Cases & Loose Pieces (Direct entry)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = casesText,
                        onValueChange = { casesText = it },
                        label = { Text("Cases (Cs)", fontSize = 11.sp) },
                        placeholder = { Text("2", fontSize = 11.5.sp) },
                        textStyle = TextStyle(fontSize = 12.5.sp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = looseText,
                        onValueChange = { looseText = it },
                        label = { Text("Loose Pieces (Pcs)", fontSize = 11.sp) },
                        placeholder = { Text("5", fontSize = 11.5.sp) },
                        textStyle = TextStyle(fontSize = 12.5.sp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 3: Case Size & GST Rate
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = caseSizeText,
                        onValueChange = { caseSizeText = it },
                        label = { Text("Case Size (Ref)", fontSize = 11.sp) },
                        placeholder = { Text("24", fontSize = 11.5.sp) },
                        textStyle = TextStyle(fontSize = 12.5.sp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = gstRateText,
                        onValueChange = { gstRateText = it },
                        label = { Text("Garment GST (%)", fontSize = 11.sp) },
                        textStyle = TextStyle(fontSize = 12.5.sp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live Case / Loose Calculation Box
                Surface(
                    color = if (isIncomplete) Color(0xFFFFFBEB) else Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isIncomplete) Color(0xFFF59E0B) else Color(0xFF86EFAC)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(9.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total: ₹${String.format("%,.2f", totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Packing: $caseCount Cases" + if (loosePieces > 0) " + $loosePieces Loose" else " (Full)",
                                fontWeight = FontWeight.Bold,
                                color = if (isIncomplete) Color(0xFFB45309) else Color(0xFF15803D),
                                fontSize = 12.sp
                            )
                        }

                        if (isIncomplete) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Incomplete Case: $loosePieces loose pieces remaining.",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Pack with another supplier's item in Mixed Packing?",
                                fontSize = 10.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Delivery info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeliveryDaysSelector(
                        expectedDeliveryDate = expectedDeliveryDate,
                        onDeliveryDateChange = { expectedDeliveryDate = it }
                    )

                    OutlinedTextField(
                        value = transporter,
                        onValueChange = { transporter = it },
                        label = { Text("Transporter / LR No", fontSize = 11.sp) },
                        placeholder = { Text("e.g. VRL / Jaipur Golden", fontSize = 11.5.sp) },
                        textStyle = TextStyle(fontSize = 12.5.sp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Photos section (On-spot Order form + Supplier's printed invoice)
                Text(
                    text = "Attachments & Spot Proofs:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = if (hasOrderFormPhoto) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { hasOrderFormPhoto = !hasOrderFormPhoto }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (hasOrderFormPhoto) Icons.Default.Check else Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = if (hasOrderFormPhoto) Color(0xFF16A34A) else NavyPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasOrderFormPhoto) "Order Form Added" else "+ Order Form Pic",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (hasOrderFormPhoto) Color(0xFF15803D) else TextPrimary
                            )
                        }
                    }

                    Surface(
                        color = if (hasSupplierInvoicePhoto) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { hasSupplierInvoicePhoto = !hasSupplierInvoicePhoto }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (hasSupplierInvoicePhoto) Icons.Default.Check else Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = if (hasSupplierInvoicePhoto) Color(0xFF16A34A) else NavyPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasSupplierInvoicePhoto) "Wholesaler Bill Added" else "+ Supplier Invoice",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (hasSupplierInvoicePhoto) Color(0xFF15803D) else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                    ) {
                        Text("Cancel", fontSize = 12.5.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val sup = selectedSupplier
                            if (sup != null && itemCode.isNotBlank() && pieces > 0 && rate > 0) {
                                onSave(
                                    sup,
                                    itemCode.trim(),
                                    pieces,
                                    rate,
                                    caseSize,
                                    caseCount,
                                    loosePieces,
                                    gstRateText.toDoubleOrNull() ?: 5.0,
                                    expectedDeliveryDate.trim(),
                                    transporter.trim()
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        enabled = selectedSupplier != null && itemCode.isNotBlank() && pieces > 0 && rate > 0
                    ) {
                        Text("Log Purchase", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSupplierSheet) {
        SupplierSearchBottomSheet(
            suppliers = suppliers,
            selectedSupplier = selectedSupplier,
            onSelectSupplier = { sup ->
                selectedSupplier = sup
                caseSizeText = sup.defaultCaseSize.toString()
                showSupplierSheet = false
            },
            onDismiss = { showSupplierSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierSearchBottomSheet(
    suppliers: List<SupplierEntity>,
    selectedSupplier: SupplierEntity?,
    onSelectSupplier: (SupplierEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val cleanQuery = searchQuery.trim()

    val filteredSuppliers = remember(suppliers, cleanQuery) {
        if (cleanQuery.isBlank()) suppliers
        else {
            suppliers.filter {
                it.name.contains(cleanQuery, ignoreCase = true) ||
                    it.marketArea.contains(cleanQuery, ignoreCase = true) ||
                    it.brand.contains(cleanQuery, ignoreCase = true) ||
                    it.type.contains(cleanQuery, ignoreCase = true) ||
                    it.city.contains(cleanQuery, ignoreCase = true) ||
                    it.categories.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("supplier_search_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Select Supplier / Wholesaler",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose shop/manufacturer for this stop",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${filteredSuppliers.size} available",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search supplier, market area, brand, fabrics...",
                        fontSize = 13.5.sp
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
                                contentDescription = "Clear",
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
                    .testTag("supplier_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Supplier List
            if (filteredSuppliers.isEmpty()) {
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
                            text = "No suppliers found matching \"$searchQuery\"",
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
                    items(filteredSuppliers, key = { it.id }) { supplier ->
                        val isSelected = selectedSupplier?.id == supplier.id
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
                                .clickable { onSelectSupplier(supplier) }
                                .testTag("supplier_item_${supplier.id}")
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
                                            text = supplier.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isSelected) Color.White else NavyPrimary
                                        )
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = supplier.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            SupplierTypeBadge(type = supplier.type)
                                        }

                                        val subDetails = listOfNotNull(
                                            supplier.marketArea.takeIf { it.isNotBlank() },
                                            supplier.brand.takeIf { it.isNotBlank() }?.let { "Brand: $it" },
                                            "Default Case: ${supplier.defaultCaseSize} pcs"
                                        ).joinToString(" • ")

                                        Text(
                                            text = subDetails,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (supplier.categories.isNotBlank()) {
                                            Text(
                                                text = "Specialties: ${supplier.categories}",
                                                fontSize = 11.sp,
                                                color = TextSecondary,
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
}

@Composable
fun MixedPackDialog(
    allEntries: List<PurchaseEntryEntity>,
    packGroups: List<PackGroupEntity> = emptyList(),
    onDismiss: () -> Unit,
    onPack: (selectedEntries: List<PurchaseEntryEntity>, caseCount: Int, customNote: String?) -> Unit,
    onUnpackGroup: ((PackGroupEntity) -> Unit)? = null
) {
    val packedEntryIds = remember(packGroups) {
        packGroups.flatMap { group -> group.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() } }.toSet()
    }
    val looseEntries = remember(allEntries) {
        allEntries.filter { it.loosePieces > 0 }
    }
    val selectedEntries = remember { mutableStateListOf<PurchaseEntryEntity>() }
    var caseCountText by remember { mutableStateOf("1") }
    var customNoteText by remember { mutableStateOf("") }

    val totalSelectedLoose = selectedEntries.sumOf { it.loosePieces }
    val caseCount = caseCountText.toIntOrNull() ?: 1

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Pack Loose Pieces",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Combine orders into full cases",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select Orders with Loose Pcs to Pack:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = NavyPrimary
                )
                Text(
                    text = "Pick 2, 3 or multiple orders to combine into a case. You can repeat this multiple times for different cases.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (looseEntries.isEmpty()) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No loose items found in this visit. All orders have full cases!",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    looseEntries.forEach { entry ->
                        val isSelected = selectedEntries.any { it.id == entry.id }
                        val isAlreadyPacked = entry.id in packedEntryIds || (entry.packGroupId != null && entry.packGroupId != 0L)
                        Surface(
                            color = if (isSelected) Color(0xFFFEF3C7) else if (isAlreadyPacked) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFF59E0B) else if (isAlreadyPacked) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isSelected) {
                                        selectedEntries.removeAll { it.id == entry.id }
                                    } else {
                                        selectedEntries.add(entry)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedEntries.add(entry) else selectedEntries.removeAll { it.id == entry.id }
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = NavyPrimary.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = entry.orderNo,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp,
                                                color = NavyPrimary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = entry.supplierName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Item: ${entry.itemCode} • Total: ${entry.pieces} pcs",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "${entry.loosePieces} Loose Pcs",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFD97706),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!entry.mixedPackNote.isNullOrBlank()) {
                                        Text(
                                            text = "↳ ${entry.mixedPackNote}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF15803D),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedEntries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Case Count Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = caseCountText,
                            onValueChange = { caseCountText = it },
                            label = { Text("Resulting Cases (Cs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                        OutlinedTextField(
                            value = customNoteText,
                            onValueChange = { customNoteText = it },
                            label = { Text("Custom Note (Optional)") },
                            placeholder = { Text("e.g. Case #1") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Reciprocal Note Preview
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Reciprocal Notes Preview:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = "$totalSelectedLoose pcs → $caseCount Case(s)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF15803D)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            selectedEntries.forEach { entry ->
                                val others = selectedEntries.filter { it.id != entry.id }
                                val noteDesc = if (others.isEmpty()) {
                                    "Packed in Mixed Case (${entry.loosePieces} pcs)"
                                } else if (others.size == 1) {
                                    val other = others.first()
                                    "Packed with ${other.supplierName} (${other.itemCode} - ${other.loosePieces} pcs)"
                                } else {
                                    "Packed with " + others.joinToString(" & ") { "${it.supplierName} (${it.itemCode})" }
                                }
                                Text(
                                    text = "• ${entry.supplierName} (${entry.orderNo}): $noteDesc",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (selectedEntries.isNotEmpty()) {
                                onPack(selectedEntries.toList(), maxOf(1, caseCount), customNoteText.takeIf { it.isNotBlank() })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pack ${selectedEntries.size} Orders into Case",
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }

                // Section: Existing Packed Cases
                if (packGroups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Already Packed Cases in this Trip (${packGroups.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    packGroups.forEach { group ->
                        val linkedIds = group.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                        val groupEntries = allEntries.filter { it.id in linkedIds || it.packGroupId == group.id }

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = NavyPrimary.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = group.packGroupCode,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp,
                                                color = NavyPrimary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${group.resultingCases} Case (${group.combinedPieces} pcs)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }

                                    if (onUnpackGroup != null) {
                                        OutlinedButton(
                                            onClick = { onUnpackGroup(group) },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Undo,
                                                contentDescription = null,
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Unpack", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (groupEntries.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    groupEntries.forEach { item ->
                                        Text(
                                            text = "• ${item.supplierName} (${item.itemCode}) - ${item.loosePieces} loose pcs",
                                            fontSize = 10.5.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = TextSecondary)
                    }
                }
            }
        }
    }
}
