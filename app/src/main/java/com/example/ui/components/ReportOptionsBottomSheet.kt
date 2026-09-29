package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NavyPrimary

// ─── Data ────────────────────────────────────────────────────────────────────

/**
 * Controls which optional fields appear in a Customer Day Report / Trip (Supplier Copy) PDF.
 *
 * All flags default to true so existing behaviour is unchanged when the sheet is not shown.
 */
data class CustomerReportOptions(
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
)

// ─── Bottom Sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportOptionsBottomSheet(
    title: String = "Customize Report / PDF",
    options: CustomerReportOptions,
    onOptionsChange: (CustomerReportOptions) -> Unit,
    onGeneratePdf: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // Handle + Title
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = NavyPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "PDF mein kaunsi fields dikhani hain choose karein",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Transport Section ──────────────────────────────────────
                SectionHeader("🚚 Transport & Booking Details")

                ToggleRow(
                    icon = Icons.Default.LocalShipping,
                    label = "Transport Section (sabhi transport fields)",
                    checked = options.showTransportSection,
                    onToggle = { onOptionsChange(options.copy(showTransportSection = it)) }
                )
                if (options.showTransportSection) {
                    IndentedToggleRow(
                        label = "Transporter Name",
                        checked = options.showTransporter,
                        onToggle = { onOptionsChange(options.copy(showTransporter = it)) }
                    )
                    IndentedToggleRow(
                        label = "Booking / Delivery Station 🚉",
                        checked = options.showBookingStation,
                        onToggle = { onOptionsChange(options.copy(showBookingStation = it)) }
                    )
                    IndentedToggleRow(
                        label = "LR / Booking No.",
                        checked = options.showLrNo,
                        onToggle = { onOptionsChange(options.copy(showLrNo = it)) }
                    )
                    IndentedToggleRow(
                        label = "Dispatch Date",
                        checked = options.showDispatchDate,
                        onToggle = { onOptionsChange(options.copy(showDispatchDate = it)) }
                    )
                    IndentedToggleRow(
                        label = "Delivery To",
                        checked = options.showDeliveryTo,
                        onToggle = { onOptionsChange(options.copy(showDeliveryTo = it)) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

                // ── Customer Details ───────────────────────────────────────
                SectionHeader("👤 Customer / Shop Details")

                ToggleRow(
                    icon = Icons.Default.Phone,
                    label = "Phone Number",
                    checked = options.showPhone,
                    onToggle = { onOptionsChange(options.copy(showPhone = it)) }
                )
                ToggleRow(
                    icon = Icons.Default.Receipt,
                    label = "GSTIN",
                    checked = options.showGstin,
                    onToggle = { onOptionsChange(options.copy(showGstin = it)) }
                )
                ToggleRow(
                    icon = Icons.Default.Place,
                    label = "Address",
                    checked = options.showAddress,
                    onToggle = { onOptionsChange(options.copy(showAddress = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

                // ── Table Columns ─────────────────────────────────────────
                SectionHeader("📋 Order Table Columns")

                ToggleRow(
                    icon = Icons.Default.Label,
                    label = "Brand Column",
                    checked = options.showBrand,
                    onToggle = { onOptionsChange(options.copy(showBrand = it)) }
                )
                ToggleRow(
                    icon = Icons.Default.Person,
                    label = "Salesman Column",
                    checked = options.showSalesman,
                    onToggle = { onOptionsChange(options.copy(showSalesman = it)) }
                )
                ToggleRow(
                    icon = Icons.Default.CheckCircle,
                    label = "Status Column",
                    checked = options.showStatus,
                    onToggle = { onOptionsChange(options.copy(showStatus = it)) }
                )
                ToggleRow(
                    icon = Icons.Default.MonetizationOn,
                    label = "GST Amount Column",
                    checked = options.showGstColumn,
                    onToggle = { onOptionsChange(options.copy(showGstColumn = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

                // ── Footer ────────────────────────────────────────────────
                SectionHeader("📄 Footer & Summary")

                ToggleRow(
                    icon = Icons.Default.Calculate,
                    label = "Amount Summary (Total, GST, Grand Total)",
                    checked = options.showAmountSummary,
                    onToggle = { onOptionsChange(options.copy(showAmountSummary = it)) }
                )
                ToggleRow(
                    icon = Icons.Default.Edit,
                    label = "Signature Box",
                    checked = options.showSignatureBox,
                    onToggle = { onOptionsChange(options.copy(showSignatureBox = it)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Generate Button
            Button(
                onClick = {
                    onGeneratePdf()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF475569),
        modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = Color(0xFF1E293B), modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.height(24.dp),
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NavyPrimary)
        )
    }
}

@Composable
private fun IndentedToggleRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("•", fontSize = 10.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(12.dp))
        Text(label, fontSize = 11.5.sp, color = Color(0xFF334155), modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.height(22.dp),
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0F766E))
        )
    }
}
