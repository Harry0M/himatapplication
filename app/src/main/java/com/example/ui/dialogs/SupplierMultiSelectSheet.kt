package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.SupplierEntity
import com.example.ui.components.AppSearchField
import com.example.ui.components.PrimaryButton
import com.example.ui.components.UiDimens
import com.example.ui.theme.TextSecondary
import com.example.util.brandName
import com.example.util.primaryPhone

/**
 * Pick the suppliers of a phone order. The tick order is kept, because the order forms then open in
 * the same order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierMultiSelectSheet(
    suppliers: List<SupplierEntity>,
    selectedIds: List<Long>,
    onDone: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(selectedIds) }

    val q = query.trim()
    val shown = remember(suppliers, q) {
        if (q.isBlank()) suppliers
        else suppliers.filter {
            it.brandName().contains(q, ignoreCase = true) ||
                it.firmName.contains(q, ignoreCase = true) ||
                it.name.contains(q, ignoreCase = true) ||
                it.marketName.contains(q, ignoreCase = true) ||
                it.marketArea.contains(q, ignoreCase = true) ||
                it.primaryPhone().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("supplier_multi_select_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiDimens.ScreenPadding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Select suppliers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            AppSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search brand, firm, market or phone"
            )

            if (shown.isEmpty()) {
                Text(
                    text = if (q.isBlank()) "No suppliers yet. Add one in the Suppliers master." else "No supplier matches \"$q\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(shown, key = { it.id }) { sup ->
                    val checked = picked.contains(sup.id)
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .toggleable(
                                    value = checked,
                                    role = Role.Checkbox,
                                    onValueChange = { on ->
                                        picked = if (on) picked + sup.id else picked.filter { it != sup.id }
                                    }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sup.brandName(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val detail = listOf(sup.firmName.takeIf { it != sup.brandName() }.orEmpty(), sup.marketName.ifBlank { sup.marketArea })
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • ")
                                if (detail.isNotBlank()) {
                                    Text(
                                        text = detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Checkbox(checked = checked, onCheckedChange = null)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }

            PrimaryButton(
                text = if (picked.isEmpty()) "Done" else "Done (${picked.size})",
                onClick = { onDone(picked) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
