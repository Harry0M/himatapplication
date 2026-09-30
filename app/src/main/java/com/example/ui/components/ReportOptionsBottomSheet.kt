package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NavyPrimary
import com.example.util.ReportFields
import com.example.util.ReportOverrides

// ─── Data ────────────────────────────────────────────────────────────────────

/** One row the user typed in themselves, printed on the PDF under the customer block. */
data class ReportCustomField(
    val label: String = "",
    val value: String = ""
) {
    val isUsable: Boolean get() = label.isNotBlank() && value.isNotBlank()
}

/**
 * How the quotation is drawn on paper.
 *
 * Two shapes of the same document, because the people who receive it read differently. A shop owner
 * is used to the card layout; an accountant, a transport office or a check-post wants the ruled form
 * they already know from a GST bill or an e-way bill, where every field sits in its own box.
 */
enum class ReportPdfLayout(val label: String, val hint: String) {
    /** Cards, soft borders, coloured badges. The original look. */
    MODERN(
        label = "Card style",
        hint = "Rounded cards and colour — easy on the eye"
    ),

    /** Every field inside a ruled box, like a GST invoice or an e-way bill. */
    RULED_FORM(
        label = "GST form style",
        hint = "Boxes, rows and columns — like a GST bill or e-way bill"
    )
}

/**
 * Controls which optional fields appear in a Customer Quotation / Trip (Supplier Copy) PDF.
 *
 * All flags default to true so existing behaviour is unchanged when the sheet is not shown.
 */
data class CustomerReportOptions(
    /** Which of the two paper layouts to print. Defaults to the look people already receive. */
    val layout: ReportPdfLayout = ReportPdfLayout.MODERN,
    // Transport section
    val showTransportSection: Boolean = true,
    val showTransporter: Boolean = true,
    val showBookingStation: Boolean = true,
    val showLrNo: Boolean = true,
    val showDispatchDate: Boolean = true,
    val showDeliveryTo: Boolean = true,
    // Customer section
    val showGstin: Boolean = true,
    val showAddress: Boolean = true,
    val showPhone: Boolean = true,
    // Order table columns
    val showBrand: Boolean = true,
    val showSalesman: Boolean = true,
    val showStatus: Boolean = true,
    val showGstColumn: Boolean = false,
    // Footer / Summary
    val showAmountSummary: Boolean = true,
    val showSignatureBox: Boolean = true,
    // ── The user's own additions ──
    /**
     * Values the sender typed over for this one PDF. Blank means "use what the record says".
     *
     * A trip is booked before the transporter is decided and long before the LR number exists, so the
     * person sending the quotation regularly knows something the database does not. This edits the
     * document, not the customer master — the master belongs to everybody.
     */
    val overrides: ReportOverrides = ReportOverrides(),
    /**
     * Extra label / value rows, printed under the customer block.
     *
     * Exists because no fixed set of fields covers every deal: a broker's name, an agreed discount, a
     * delivery promise. Rather than guessing which ones to add, the person sending the PDF types them.
     */
    val customFields: List<ReportCustomField> = emptyList(),
    /** A free note printed above the terms, in the sender's own words. */
    val customNote: String = ""
) {
    /** Only the rows with both a label and a value; a half-typed row is not printed. */
    fun usableCustomFields(): List<ReportCustomField> = customFields.filter { it.isUsable }
}

// ─── Bottom Sheet ─────────────────────────────────────────────────────────────

/**
 * Chooses what goes on the PDF, and lets the sender add their own lines.
 *
 * The rows used to be 2dp apart with the switches squeezed to a fixed 24dp, which made them hard to
 * hit and hard to read. Each row is now a proper 52dp target and the whole row toggles, not just the
 * switch — on a phone in a market, aiming at a small switch is the thing that goes wrong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportOptionsBottomSheet(
    title: String = "Customise the PDF",
    options: CustomerReportOptions,
    onOptionsChange: (CustomerReportOptions) -> Unit,
    onGeneratePdf: () -> Unit,
    onDismiss: () -> Unit,
    /**
     * What the record currently holds for each field, shown as the placeholder in every box.
     *
     * Passed in rather than read here because the sheet has no access to the trip: the screen resolves
     * it once, with the same function the PDF writer uses, so the grey text in the box is exactly what
     * will be printed if the sender types nothing.
     */
    defaults: ReportFields? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newLabel by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    val overrides = options.overrides

    fun setOverrides(updated: ReportOverrides) = onOptionsChange(options.copy(overrides = updated))

    fun addCustomField() {
        val field = ReportCustomField(newLabel.trim(), newValue.trim())
        if (!field.isUsable) return
        onOptionsChange(options.copy(customFields = options.customFields + field))
        newLabel = ""
        newValue = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = NavyPrimary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "Choose what to show, and add anything of your own at the bottom.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Paper layout ───────────────────────────────────────────
                SheetSection("Paper layout")

                ReportPdfLayout.entries.forEach { layout ->
                    LayoutChoiceRow(
                        layout = layout,
                        selected = options.layout == layout,
                        onSelect = { onOptionsChange(options.copy(layout = layout)) }
                    )
                }

                SheetDivider()

                // ── Transport Section ──────────────────────────────────────
                SheetSection("Transport and booking")

                ToggleRow(
                    icon = Icons.Default.LocalShipping,
                    label = "Transport section",
                    hint = "All the transport fields together",
                    checked = options.showTransportSection,
                    onToggle = { onOptionsChange(options.copy(showTransportSection = it)) }
                )
                if (options.showTransportSection) {
                    Text(
                        text = "Tap a box to correct it for this PDF only. Left empty, it prints what " +
                            "the record already says (shown in grey). The customer master is not changed.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(start = 30.dp, bottom = 4.dp)
                    )
                    IndentedToggleRow(
                        label = "Transporter name",
                        checked = options.showTransporter,
                        onToggle = { onOptionsChange(options.copy(showTransporter = it)) },
                        value = overrides.transporter,
                        onValueChange = { setOverrides(overrides.copy(transporter = it)) },
                        placeholder = defaults?.transporter
                    )
                    IndentedToggleRow(
                        label = "Booking / delivery station",
                        checked = options.showBookingStation,
                        onToggle = { onOptionsChange(options.copy(showBookingStation = it)) },
                        value = overrides.bookingStation,
                        onValueChange = { setOverrides(overrides.copy(bookingStation = it)) },
                        placeholder = defaults?.bookingStation
                    )
                    IndentedToggleRow(
                        label = "LR / booking number",
                        checked = options.showLrNo,
                        onToggle = { onOptionsChange(options.copy(showLrNo = it)) },
                        value = overrides.lrNo,
                        onValueChange = { setOverrides(overrides.copy(lrNo = it)) },
                        placeholder = defaults?.lrNo
                    )
                    IndentedToggleRow(
                        label = "Dispatch date",
                        checked = options.showDispatchDate,
                        onToggle = { onOptionsChange(options.copy(showDispatchDate = it)) },
                        value = overrides.dispatchDate,
                        onValueChange = { setOverrides(overrides.copy(dispatchDate = it)) },
                        placeholder = defaults?.dispatchDate
                    )
                    IndentedToggleRow(
                        label = "Delivery to",
                        checked = options.showDeliveryTo,
                        onToggle = { onOptionsChange(options.copy(showDeliveryTo = it)) },
                        value = overrides.deliveryTo,
                        onValueChange = { setOverrides(overrides.copy(deliveryTo = it)) },
                        placeholder = defaults?.deliveryTo
                    )
                }

                SheetDivider()

                // ── Customer Details ───────────────────────────────────────
                SheetSection("Customer and shop")

                // Firm and proprietor are always printed, so they get a box and no switch
                OverrideField(
                    label = "Firm / shop name",
                    value = overrides.firmName,
                    onValueChange = { setOverrides(overrides.copy(firmName = it)) },
                    placeholder = defaults?.firmName
                )
                OverrideField(
                    label = "Proprietor",
                    value = overrides.proprietor,
                    onValueChange = { setOverrides(overrides.copy(proprietor = it)) },
                    placeholder = defaults?.proprietor
                )

                ToggleRow(
                    icon = Icons.Default.Phone,
                    label = "Phone number",
                    checked = options.showPhone,
                    onToggle = { onOptionsChange(options.copy(showPhone = it)) },
                    value = overrides.phone,
                    onValueChange = { setOverrides(overrides.copy(phone = it)) },
                    placeholder = defaults?.phone
                )
                ToggleRow(
                    icon = Icons.Default.Receipt,
                    label = "GSTIN",
                    checked = options.showGstin,
                    onToggle = { onOptionsChange(options.copy(showGstin = it)) },
                    value = overrides.gstin,
                    onValueChange = { setOverrides(overrides.copy(gstin = it)) },
                    placeholder = defaults?.gstin
                )
                ToggleRow(
                    icon = Icons.Default.Place,
                    label = "Address",
                    checked = options.showAddress,
                    onToggle = { onOptionsChange(options.copy(showAddress = it)) },
                    value = overrides.address,
                    onValueChange = { setOverrides(overrides.copy(address = it)) },
                    placeholder = defaults?.address
                )

                if (!overrides.isEmpty) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${overrides.editedCount} field(s) edited for this PDF. The " +
                                    "customer record is unchanged.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF9A3412),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Undo all",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                modifier = Modifier
                                    .clickable { setOverrides(ReportOverrides()) }
                                    .padding(6.dp)
                            )
                        }
                    }
                }

                SheetDivider()

                // ── Table Columns ─────────────────────────────────────────
                SheetSection("Order table columns")

                ToggleRow(
                    icon = Icons.Default.Label,
                    label = "Brand",
                    checked = options.showBrand,
                    onToggle = { onOptionsChange(options.copy(showBrand = it)) }
                )
                ToggleRow(
                    icon = Icons.Default.Person,
                    label = "Salesman",
                    checked = options.showSalesman,
                    onToggle = { onOptionsChange(options.copy(showSalesman = it)) }
                )
                ToggleRow(
                    icon = Icons.Default.CheckCircle,
                    label = "Delivery status",
                    checked = options.showStatus,
                    onToggle = { onOptionsChange(options.copy(showStatus = it)) }
                )
                ToggleRow(
                    icon = Icons.Default.MonetizationOn,
                    label = "GST amount",
                    checked = options.showGstColumn,
                    onToggle = { onOptionsChange(options.copy(showGstColumn = it)) }
                )

                SheetDivider()

                // ── Footer ────────────────────────────────────────────────
                SheetSection("Footer and totals")

                ToggleRow(
                    icon = Icons.Default.Calculate,
                    label = "Amount summary",
                    hint = "Total, GST and grand total",
                    checked = options.showAmountSummary,
                    onToggle = { onOptionsChange(options.copy(showAmountSummary = it)) }
                )
                ToggleRow(
                    icon = Icons.Default.Edit,
                    label = "Signature box",
                    checked = options.showSignatureBox,
                    onToggle = { onOptionsChange(options.copy(showSignatureBox = it)) }
                )

                SheetDivider()

                // ── The sender's own lines ────────────────────────────────
                SheetSection("Add your own details")
                Text(
                    text = "For anything the fixed fields do not cover — a broker, an agreed discount, " +
                        "a delivery promise. These print under the customer details.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                options.customFields.forEachIndexed { index, field ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .padding(start = 12.dp, end = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    field.label,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    field.value,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = {
                                    onOptionsChange(
                                        options.copy(
                                            customFields = options.customFields.filterIndexed { i, _ -> i != index }
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove ${field.label}",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("Label", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Broker", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Detail", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Suresh Bhai", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = "Add this line",
                    icon = Icons.Default.Add,
                    enabled = newLabel.isNotBlank() && newValue.isNotBlank(),
                    onClick = { addCustomField() },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = options.customNote,
                    onValueChange = { onOptionsChange(options.copy(customNote = it)) },
                    label = { Text("Note for the customer", fontSize = 12.sp) },
                    placeholder = { Text("Anything you want printed on this PDF", fontSize = 12.sp) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    // A line half typed into the boxes is clearly meant to be included
                    val pending = ReportCustomField(newLabel.trim(), newValue.trim())
                    val finalOptions = if (pending.isUsable) {
                        options.copy(customFields = options.customFields + pending)
                    } else {
                        options
                    }
                    if (finalOptions !== options) onOptionsChange(finalOptions)
                    onGeneratePdf()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Generate ${options.layout.label} PDF",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SheetSection(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF475569),
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
}

/**
 * One of the two paper layouts, as a radio row.
 *
 * A radio rather than a switch: these are two versions of the same document, not two features to turn
 * on. The whole row is the target and the selected one is tinted, so the choice is readable at a
 * glance without opening the PDF.
 */
@Composable
private fun LayoutChoiceRow(
    layout: ReportPdfLayout,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) NavyPrimary.copy(alpha = 0.06f) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) NavyPrimary else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 56.dp)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = NavyPrimary)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    layout.label,
                    fontSize = 13.5.sp,
                    color = Color(0xFF1E293B),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                Text(layout.hint, fontSize = 11.sp, color = Color(0xFF64748B))
            }
            Icon(
                imageVector = if (layout == ReportPdfLayout.RULED_FORM) {
                    Icons.Default.GridOn
                } else {
                    Icons.Default.Dashboard
                },
                contentDescription = null,
                tint = if (selected) NavyPrimary else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * One switch row. The whole row is the target, not just the switch, and it is 52dp tall — the old
 * rows were 2dp apart with the switch forced to 24dp, which is well under what a thumb can hit.
 */
@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    hint: String? = null,
    /** When these are supplied the row also offers a box to correct the value for this one PDF. */
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    placeholder: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(role = Role.Switch, onClickLabel = label) { onToggle(!checked) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = NavyPrimary.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 13.5.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                if (hint != null) {
                    Text(hint, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NavyPrimary)
            )
        }
        // No point offering to edit a field that is switched off and will not be printed
        if (checked && value != null && onValueChange != null) {
            OverrideField(
                label = label,
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                startPadding = 30.dp
            )
        }
    }
}

@Composable
private fun IndentedToggleRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    placeholder: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Switch, onClickLabel = label) { onToggle(!checked) }
                .padding(start = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 12.5.sp, color = Color(0xFF334155), modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0F766E))
            )
        }
        if (checked && value != null && onValueChange != null) {
            OverrideField(
                label = label,
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                startPadding = 30.dp
            )
        }
    }
}

/**
 * One box for correcting a field on this PDF.
 *
 * The record's current value is the placeholder, so the sender can see what they are replacing without
 * generating the document first. An edited box is tinted and offers a cross to put it back — the point
 * of the tint is that a value typed here does not touch the customer master, and that should be
 * obvious at a glance rather than remembered.
 */
@Composable
private fun OverrideField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String?,
    startPadding: Dp = 0.dp,
) {
    val edited = value.isNotBlank()
    val onRecord = placeholder?.takeIf { it.isNotBlank() } ?: "Not recorded"
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Edit $label", fontSize = 11.sp) },
        placeholder = { Text(onRecord, fontSize = 12.sp, color = Color(0xFF94A3B8)) },
        // Shown whatever the state of the field. A placeholder would have been the obvious place for
        // this, but Material hides the placeholder until the box is focused, so the one thing the
        // sender needs before deciding to edit — what it says now — would have been invisible.
        supportingText = {
            Text(
                text = if (edited) "Replacing: $onRecord" else "On record: $onRecord",
                fontSize = 10.5.sp,
                color = if (edited) Color(0xFF9A3412) else Color(0xFF94A3B8)
            )
        },
        trailingIcon = if (edited) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Use the recorded $label instead",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else null,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = if (edited) Color(0xFFFFF7ED) else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, bottom = 8.dp)
    )
}
