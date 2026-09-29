package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.dialogs.CustomDateRangePickerDialog
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.StatusDelivered
import com.example.ui.theme.StatusDeliveredBg
import com.example.ui.theme.StatusDispatched
import com.example.ui.theme.StatusDispatchedBg
import com.example.ui.theme.StatusPacked
import com.example.ui.theme.StatusPackedBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.TextSecondary
import com.example.util.DatePreset
import com.example.util.DateRangeFilter
import com.example.util.PdfGenerator
import com.example.util.isClosed

/**
 * One size system for every screen, so buttons, chips and fields stop varying from screen to screen.
 * New and restructured screens use these; older screens keep working unchanged.
 */
object UiDimens {
    val ScreenPadding = 16.dp
    val ItemGap = 10.dp
    val CardRadius = 14.dp
    val ControlHeight = 44.dp
    val ChipHeight = 34.dp
    val IconButtonSize = 40.dp
}

private val CardBorder = Color(0xFFE2E8F0)

// -----------------------------------------------------------------------------
// Headers & buttons
// -----------------------------------------------------------------------------

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            HeaderIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    Surface(
        shape = CircleShape,
        color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(UiDimens.IconButtonSize)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(UiDimens.IconButtonSize)) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = modifier.heightIn(min = UiDimens.ControlHeight)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier.heightIn(min = UiDimens.ControlHeight)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = contentColor, maxLines = 1)
    }
}

// -----------------------------------------------------------------------------
// Search, chips, date filter
// -----------------------------------------------------------------------------

@Composable
fun AppSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(UiDimens.ControlHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** A pill chip with one fixed height. */
@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    Surface(
        shape = CircleShape,
        color = if (selected) NavyPrimary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) NavyPrimary else CardBorder),
        modifier = modifier
            .height(UiDimens.ChipHeight)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = if (selected) Color.White else NavyPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/** Horizontally scrollable single-choice chips. [options] = key to label. */
@Composable
fun ChoiceChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (key, label) ->
            PillChip(label = label, selected = key == selected, onClick = { onSelect(key) })
        }
    }
}

/**
 * Date filter used on every list and master detail screen:
 * All Time, Today, Last 7 Days, This Month, Last Month and a Custom range picker.
 */
@Composable
fun DateRangeFilterBar(
    filter: DateRangeFilter,
    onChange: (DateRangeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val presets = listOf(DatePreset.ALL, DatePreset.TODAY, DatePreset.LAST_7, DatePreset.THIS_MONTH, DatePreset.LAST_MONTH)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { preset ->
            PillChip(
                label = preset.label,
                selected = filter.preset == preset,
                onClick = { onChange(DateRangeFilter(preset = preset)) }
            )
        }
        PillChip(
            label = if (filter.preset == DatePreset.CUSTOM && filter.customLabel.isNotBlank()) filter.customLabel else "Custom",
            selected = filter.preset == DatePreset.CUSTOM,
            leadingIcon = Icons.Default.CalendarMonth,
            onClick = { showPicker = true }
        )
    }

    if (showPicker) {
        CustomDateRangePickerDialog(
            initialStartMillis = filter.customStartMillis,
            initialEndMillis = filter.customEndMillis,
            onDismissRequest = { showPicker = false },
            onDateRangeSelected = { start, end, label ->
                onChange(
                    DateRangeFilter(
                        preset = DatePreset.CUSTOM,
                        customStartMillis = start,
                        customEndMillis = end,
                        customLabel = label
                    )
                )
                showPicker = false
            }
        )
    }
}

// -----------------------------------------------------------------------------
// Cards, sections, rows
// -----------------------------------------------------------------------------

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(UiDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(UiDimens.CardRadius)).clickable(onClick = onClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (count != null) "$title ($count)" else title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    InfoCard(modifier = modifier, contentPadding = PaddingValues(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = NavyPrimary,
    onClick: (() -> Unit)? = null
) {
    InfoCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent, maxLines = 1)
    }
}

@Composable
fun StatusPill(text: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = background, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DeliveryStatusPill(status: String, modifier: Modifier = Modifier) {
    val s = status.ifBlank { "Pending" }
    val (bg, fg) = when (s.lowercase()) {
        "delivered" -> StatusDeliveredBg to StatusDelivered
        "dispatched" -> StatusDispatchedBg to StatusDispatched
        "packed" -> StatusPackedBg to StatusPacked
        else -> StatusPendingBg to StatusPending
    }
    StatusPill(text = s, background = bg, foreground = fg, modifier = modifier)
}

@Composable
fun TripStatusPill(visit: VisitEntity, modifier: Modifier = Modifier) {
    if (visit.isClosed()) {
        StatusPill("Closed", StatusDeliveredBg, StatusDelivered, modifier)
    } else {
        StatusPill("Open", Color(0xFFE0F2FE), Color(0xFF0369A1), modifier)
    }
}

/**
 * The one list row for the whole app: plain row, divider below, no card and no coloured circle.
 *
 * Line 1: [title] on the left, [value] (amount or status) on the right.
 * Line 2: [detail]. Line 3: [note], only when it is not blank.
 * Long text ends with "..."; at a large font scale the row grows instead of clipping the text.
 */
@Composable
fun ListRow(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    note: String? = null,
    status: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    /** Small fixed-size avatar or icon in front of the text; keep it at 36dp. */
    leading: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    /** Pass 0.dp when the list already sits inside a padded container. */
    horizontalPadding: Dp = UiDimens.ScreenPadding,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 48.dp)
                .padding(horizontal = horizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (value != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = valueColor,
                            maxLines = 1
                        )
                    }
                }
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!note.isNullOrBlank()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (status != null) {
                Spacer(modifier = Modifier.width(10.dp))
                status()
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(4.dp))
                trailing()
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

/** Customer, supplier, staff or agent row used by every list. */
@Composable
fun PersonRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeColor: Color = NavyPrimary,
    onClick: (() -> Unit)? = null
) {
    ListRow(
        title = title,
        detail = subtitle,
        modifier = modifier,
        status = badge?.let { { StatusPill(it, badgeColor.copy(alpha = 0.10f), badgeColor) } },
        onClick = onClick
    )
}

@Composable
fun TripRow(
    visit: VisitEntity,
    salesmen: String,
    ordersCount: Int,
    pieces: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    ListRow(
        title = visit.customerName.ifBlank { "Customer" },
        detail = "${visit.date} • $ordersCount orders • $pieces pcs",
        note = "Salesmen: ${salesmen.ifBlank { "—" }}",
        modifier = modifier,
        status = { TripStatusPill(visit) },
        trailing = trailing,
        onClick = onClick
    )
}

@Composable
fun OrderRow(
    entry: PurchaseEntryEntity,
    customerName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val packing = buildString {
        append("${entry.pieces} pcs")
        if (entry.caseCount > 0) append(" • ${entry.caseCount} cs")
        if (entry.loosePieces > 0) append(" • ${entry.loosePieces} loose")
    }
    // The bill amount (with GST) is what everyone reads off a list; fall back for older rows
    val amount = if (entry.grandTotalWithGst > 0.0) entry.grandTotalWithGst else entry.totalAmount
    ListRow(
        title = "${entry.itemCode.ifBlank { entry.orderNo }} • ${entry.supplierName}",
        value = "₹${PdfGenerator.formatInr(amount)}",
        detail = listOf(customerName.takeIf { it.isNotBlank() }, packing).filterNotNull().joinToString(" • "),
        note = listOf(
            entry.orderNo.takeIf { it.isNotBlank() },
            entry.orderDate.takeIf { it.isNotBlank() },
            entry.salesmanName.takeIf { it.isNotBlank() }
        ).filterNotNull().joinToString(" • "),
        modifier = modifier,
        status = { DeliveryStatusPill(entry.deliveryStatus) },
        trailing = trailing,
        onClick = onClick
    )
}

// -----------------------------------------------------------------------------
// Home quick actions
// -----------------------------------------------------------------------------

/** One small icon shortcut on Home. [badge] shows a count, e.g. new registrations. */
data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0,
    val onClick: () -> Unit
)

/**
 * Small icon shortcuts laid out as a fixed grid, [columns] per line, so 3-4 rows of shortcuts are
 * all visible at once instead of hiding behind a sideways scroll.
 *
 * Every tile is the same size, so nothing shifts when a badge appears, and each tile is one
 * 48dp+ tap target that speaks its own label.
 */
@Composable
fun QuickActionsRow(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier,
    columns: Int = 4
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        actions.chunked(columns).forEach { rowActions ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowActions.forEach { action ->
                    QuickActionTile(action = action, modifier = Modifier.weight(1f))
                }
                // Keep the last row's tiles the same width as every other row
                repeat(columns - rowActions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(action: QuickAction, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = action.label,
                onClick = action.onClick
            )
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(21.dp)
                )
            }
            if (action.badge > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-5).dp)
                        .clearAndSetSemantics { }
                ) {
                    Text(
                        text = if (action.badge > 99) "99+" else "${action.badge}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A read-only field that opens a picker. One height everywhere, so a sheet never shifts when a value
 * is chosen. [contentDescription] is what TalkBack reads, e.g. "Customer".
 */
@Composable
fun PickerField(
    value: String?,
    placeholder: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filled = !value.isNullOrBlank()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (filled) MaterialTheme.colorScheme.outline else CardBorder),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = if (filled) "$contentDescription: $value" else contentDescription }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (filled) value!! else placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (filled) MaterialTheme.colorScheme.onSurface else TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

/** Row in a settings-style menu (More screen). */
@Composable
fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    tint: Color = NavyPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (badge != null) {
            StatusPill(badge, Color(0xFFFEE2E2), Color(0xFFB91C1C))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
    }
}
