package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
        gstRate: Double,
        expectedDeliveryDate: String,
        transporter: String
    ) -> Unit
) {
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var itemCode by remember { mutableStateOf("") }
    var piecesText by remember { mutableStateOf("") }
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

    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    // Live Calculations
    val pieces = piecesText.toIntOrNull() ?: 0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val caseSize = caseSizeText.toIntOrNull() ?: 24
    val totalAmount = pieces * rate
    val caseCount = if (caseSize > 0) pieces / caseSize else 0
    val loosePieces = if (caseSize > 0) pieces % caseSize else 0
    val isIncomplete = loosePieces > 0

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
                    Column {
                        Text(
                            text = "Purchase Entry (Supplier Stop)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Logged on the spot while escorting client",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Supplier Picker
                ExposedDropdownMenuBox(
                    expanded = supplierDropdownExpanded,
                    onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.let { "${it.name} (${it.type})" } ?: "Select Supplier",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supplier / Wholesaler Stop *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = supplierDropdownExpanded,
                        onDismissRequest = { supplierDropdownExpanded = false }
                    ) {
                        suppliers.forEach { sup ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(sup.name, fontWeight = FontWeight.SemiBold)
                                            Text(sup.marketArea, fontSize = 11.sp, color = TextSecondary)
                                        }
                                        SupplierTypeBadge(type = sup.type)
                                    }
                                },
                                onClick = {
                                    selectedSupplier = sup
                                    caseSizeText = sup.defaultCaseSize.toString()
                                    supplierDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Item Code with Autocomplete chips
                OutlinedTextField(
                    value = itemCode,
                    onValueChange = { itemCode = it.uppercase() },
                    label = { Text("Item / Style Code *") },
                    placeholder = { Text("e.g. ABC, XYZ, KURTI-102, JEANS-88") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Quick suggestions from history
                if (historyItemCodes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Recent Items:", fontSize = 10.5.sp, color = TextSecondary)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                    fontSize = 11.sp,
                                    color = NavyPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pieces & Rate
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = piecesText,
                        onValueChange = { piecesText = it },
                        label = { Text("Pieces (Pc) *") },
                        placeholder = { Text("50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Rate (₹/Pc) *") },
                        placeholder = { Text("420") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Case Size & GST Rate
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = caseSizeText,
                        onValueChange = { caseSizeText = it },
                        label = { Text("Case Size (Pcs/Case)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = gstRateText,
                        onValueChange = { gstRateText = it },
                        label = { Text("Garment GST (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total: ₹${String.format("%,.2f", totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Packing: $caseCount Cases" + if (loosePieces > 0) " + $loosePieces Loose" else " (Full)",
                                fontWeight = FontWeight.Bold,
                                color = if (isIncomplete) Color(0xFFB45309) else Color(0xFF15803D),
                                fontSize = 13.sp
                            )
                        }

                        if (isIncomplete) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Incomplete Case: $loosePieces loose pieces remaining.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Pack with another supplier's item in Mixed Packing?",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Delivery info
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expectedDeliveryDate,
                        onValueChange = { expectedDeliveryDate = it },
                        label = { Text("Exp Delivery") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = transporter,
                        onValueChange = { transporter = it },
                        label = { Text("Transporter / Bilty") },
                        placeholder = { Text("e.g. VRL / Jaipur Golden") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Photos section (On-spot Order form + Supplier's printed invoice)
                Text(
                    text = "Attachments & Spot Proofs:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (hasOrderFormPhoto) Icons.Default.Check else Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = if (hasOrderFormPhoto) Color(0xFF16A34A) else NavyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasOrderFormPhoto) "Order Form Attached" else "+ Order Form Pic",
                                fontSize = 11.sp,
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (hasSupplierInvoicePhoto) Icons.Default.Check else Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = if (hasSupplierInvoicePhoto) Color(0xFF16A34A) else NavyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasSupplierInvoicePhoto) "Wholesaler Bill Attached" else "+ Supplier Invoice",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (hasSupplierInvoicePhoto) Color(0xFF15803D) else TextPrimary
                            )
                        }
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
                            val sup = selectedSupplier
                            if (sup != null && itemCode.isNotBlank() && pieces > 0 && rate > 0) {
                                onSave(
                                    sup,
                                    itemCode.trim(),
                                    pieces,
                                    rate,
                                    caseSize,
                                    gstRateText.toDoubleOrNull() ?: 5.0,
                                    expectedDeliveryDate.trim(),
                                    transporter.trim()
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        enabled = selectedSupplier != null && itemCode.isNotBlank() && pieces > 0 && rate > 0
                    ) {
                        Text("Log Purchase")
                    }
                }
            }
        }
    }
}

@Composable
fun MixedPackDialog(
    incompleteEntries: List<PurchaseEntryEntity>,
    onDismiss: () -> Unit,
    onPack: (selectedEntries: List<PurchaseEntryEntity>, targetCaseSize: Int) -> Unit
) {
    val selectedEntries = remember { mutableStateListOf<PurchaseEntryEntity>() }
    var targetCaseSizeText by remember { mutableStateOf("24") }

    val totalSelectedLoose = selectedEntries.sumOf { it.loosePieces }
    val targetCaseSize = targetCaseSizeText.toIntOrNull() ?: 24
    val resultingCases = if (targetCaseSize > 0) totalSelectedLoose / targetCaseSize else 1
    val remainingLoose = if (targetCaseSize > 0) totalSelectedLoose % targetCaseSize else 0

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mixed Case Packing",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Link loose pieces across multiple supplier stops into physically packed full cases. The packing note will appear on both the Customer Report and each Supplier's copy.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Incomplete Purchase Stops to Combine:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (incompleteEntries.isEmpty()) {
                    Text(
                        text = "No loose items found in this visit. All orders have full cases!",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    incompleteEntries.forEach { entry ->
                        val isSelected = selectedEntries.contains(entry)
                        Surface(
                            color = if (isSelected) Color(0xFFFEF3C7) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isSelected) selectedEntries.remove(entry) else selectedEntries.add(entry)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedEntries.add(entry) else selectedEntries.remove(entry)
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${entry.supplierName} • ${entry.itemCode}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Order: ${entry.orderNo} | Total: ${entry.pieces} pcs | Loose: ${entry.loosePieces} pcs",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD97706),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = targetCaseSizeText,
                    onValueChange = { targetCaseSizeText = it },
                    label = { Text("Standard Target Case Size (Pcs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Result Summary Card
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Packing Preview:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFF166534)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Combined Loose: $totalSelectedLoose pcs from ${selectedEntries.size} orders",
                            fontSize = 12.sp,
                            color = Color(0xFF15803D)
                        )
                        Text(
                            text = "• Resulting Mixed Cases: $resultingCases Case(s)" +
                                    if (remainingLoose > 0) " + $remainingLoose loose left" else " (Fully Packed)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
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
                            if (selectedEntries.size >= 2) {
                                onPack(selectedEntries, targetCaseSize)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        enabled = selectedEntries.size >= 2
                    ) {
                        Text("Pack Mixed Case(s)")
                    }
                }
            }
        }
    }
}
