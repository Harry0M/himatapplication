package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SupplierOrderFormOptions

/**
 * Shown every time a supplier order form PDF is created.
 * Extra supplier details (GSTIN, market area) are optional and start switched off;
 * the order nature is Self or WhatsApp, and the printed text can be edited.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierOrderFormOptionsSheet(
    supplierName: String,
    onDismiss: () -> Unit,
    onCreate: (SupplierOrderFormOptions) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var showGstin by rememberSaveable { mutableStateOf(false) }
    var showMarketArea by rememberSaveable { mutableStateOf(false) }
    var isWhatsApp by rememberSaveable { mutableStateOf(false) }
    var natureText by rememberSaveable { mutableStateOf(SupplierOrderFormOptions.ORDER_NATURE_SELF) }

    fun presetText(whatsApp: Boolean) =
        if (whatsApp) SupplierOrderFormOptions.ORDER_NATURE_WHATSAPP else SupplierOrderFormOptions.ORDER_NATURE_SELF

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(shape = CircleShape, color = Color(0xFFE0E7FF), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Order Form PDF", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = NavyPrimary)
                    Text(
                        text = if (supplierName.isNotBlank()) "For $supplierName" else "Choose what to print",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            SheetSectionLabel("Extra supplier details (optional)")
            Spacer(modifier = Modifier.height(6.dp))
            OptionSwitchRow(
                title = "Supplier GSTIN",
                subtitle = "Print the supplier's GST number",
                checked = showGstin,
                onCheckedChange = { showGstin = it }
            )
            OptionSwitchRow(
                title = "Market area",
                subtitle = "Print the supplier's market / area",
                checked = showMarketArea,
                onCheckedChange = { showMarketArea = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SheetSectionLabel("Order nature")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OrderNatureCard(
                    title = "Self",
                    subtitle = "Ordered in person",
                    icon = Icons.Default.Storefront,
                    selected = !isWhatsApp,
                    accent = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        isWhatsApp = false
                        natureText = presetText(false)
                    }
                )
                OrderNatureCard(
                    title = "WhatsApp",
                    subtitle = "Order came on WhatsApp",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    selected = isWhatsApp,
                    accent = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        isWhatsApp = true
                        natureText = presetText(true)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = natureText,
                onValueChange = { natureText = it.take(40) },
                label = { Text("Printed on the PDF as") },
                supportingText = { Text("Edit if you want different wording") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onCreate(
                        SupplierOrderFormOptions(
                            showGstin = showGstin,
                            showMarketArea = showMarketArea,
                            orderNature = natureText.trim().ifBlank { presetText(isWhatsApp) }
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create & Share PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(text = text, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextPrimary)
}

/** Whole row toggles, so the target is large and TalkBack reads it as one switch. */
@Composable
private fun OptionSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Text(subtitle, fontSize = 11.5.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun OrderNatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accent else Color(0xFFE2E8F0)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) accent else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
