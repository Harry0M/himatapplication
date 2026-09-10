package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator

@Composable
fun DeliveriesScreen(
    viewModel: HimatViewModel
) {
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }
    var entryToUpdate by remember { mutableStateOf<PurchaseEntryEntity?>(null) }

    val filtered = entries.filter {
        if (selectedFilter == "All") true
        else it.deliveryStatus.equals(selectedFilter, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Delivery & Dispatch Tracking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage transporter bilty & order fulfilment status",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status filter chips with standard touch sizes and horizontal scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Pending", "Packed", "Dispatched", "Delivered").forEach { status ->
                val isSelected = selectedFilter == status
                val count = if (status == "All") entries.size else entries.count { it.deliveryStatus.equals(status, ignoreCase = true) }

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = status },
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                    shape = CircleShape,
                    label = {
                        Text(
                            text = "$status ($count)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No orders with status '$selectedFilter'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { entry ->
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        entry.orderNo,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        entry.supplierName,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                StatusBadge(status = entry.deliveryStatus)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Item: ${entry.itemCode} • ${entry.pieces} Pcs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    PdfGenerator.formatInr(entry.totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    val exp = if (entry.expectedDeliveryDate.isNotBlank()) entry.expectedDeliveryDate else "Standard"
                                    val trans = if (entry.transporter.isNotBlank()) entry.transporter else "Not assigned"
                                    Text("Exp Date: $exp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "Transporter: $trans",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                FilledTonalButton(
                                    onClick = { entryToUpdate = entry },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Update Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Update Status Dialog
    entryToUpdate?.let { entry ->
        UpdateDeliveryStatusDialog(
            entry = entry,
            onDismiss = { entryToUpdate = null },
            onSave = { newStatus, transporter ->
                viewModel.updateDeliveryStatus(entry, newStatus, transporter)
                entryToUpdate = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDeliveryStatusDialog(
    entry: PurchaseEntryEntity,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var status by remember { mutableStateOf(entry.deliveryStatus) }
    var transporter by remember { mutableStateOf(entry.transporter) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text("Update Delivery Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Order ${entry.orderNo} (${entry.itemCode} - ${entry.supplierName})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fulfillment Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        listOf("Pending", "Packed", "Dispatched", "Delivered").forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    status = st
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = transporter,
                    onValueChange = { transporter = it },
                    label = { Text("Transporter / Bilty No") },
                    placeholder = { Text("e.g. VRL Logistics / Bilty #4920") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    singleLine = true
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
                        onClick = { onSave(status, transporter) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Save Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
