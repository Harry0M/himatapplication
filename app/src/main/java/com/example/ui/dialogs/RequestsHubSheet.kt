package com.example.ui.dialogs

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ListRow
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One pending registration, whichever database it came from. */
private data class PendingRequestRow(
    val id: String,
    val isSupplier: Boolean,
    val title: String,
    val detail: String,
    val createdAt: Long
)

private val requestDateFmt = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

/**
 * One inbox for everything waiting on the office: customers and suppliers who filled the public
 * registration form, newest first, plus the link to send more people. Tapping a row opens the full
 * verification screen for that kind of request, which is where approve and reject already live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsHubSheet(
    viewModel: HimatViewModel,
    onDismiss: () -> Unit,
    onOpenCustomerRequests: () -> Unit,
    onOpenSupplierRequests: () -> Unit,
    onShareLink: () -> Unit,
    /** Admin only: open the console for deletions staff have asked for. */
    onOpenDeleteRequests: (() -> Unit)? = null
) {
    val customerRequests by viewModel.registrationRequests.collectAsStateWithLifecycle()
    val supplierRequests by viewModel.supplierRegistrationRequests.collectAsStateWithLifecycle()
    val pendingDeletions by viewModel.pendingDeletionCount.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminUser.collectAsStateWithLifecycle()

    val customerPending = remember(customerRequests) {
        customerRequests.filter { it.status.equals("PENDING", ignoreCase = true) }
    }
    val supplierPending = remember(supplierRequests) {
        supplierRequests.filter { it.status.equals("PENDING", ignoreCase = true) }
    }

    // Both lists merged so the newest request is on top, whoever sent it
    val merged = remember(customerPending, supplierPending) {
        val rows = mutableListOf<PendingRequestRow>()
        customerPending.forEach { r ->
            rows += PendingRequestRow(
                id = "c_${r.id}",
                isSupplier = false,
                title = r.firmName.ifBlank { r.name }.ifBlank { "Customer" },
                detail = listOf(
                    r.name.takeIf { it.isNotBlank() && it != r.firmName },
                    r.city.takeIf { it.isNotBlank() },
                    r.marketArea.takeIf { it.isNotBlank() },
                    r.phone.takeIf { it.isNotBlank() }
                ).filterNotNull().joinToString(" • "),
                createdAt = r.createdAt
            )
        }
        supplierPending.forEach { r ->
            rows += PendingRequestRow(
                id = "s_${r.id}",
                isSupplier = true,
                title = r.firmName.ifBlank { r.name }.ifBlank { "Supplier" },
                detail = listOf(
                    r.brand.takeIf { it.isNotBlank() },
                    r.marketArea.ifBlank { r.marketName }.takeIf { it.isNotBlank() },
                    r.city.takeIf { it.isNotBlank() },
                    r.phone.takeIf { it.isNotBlank() }
                ).filterNotNull().joinToString(" • "),
                createdAt = r.createdAt
            )
        }
        rows.sortedByDescending { it.createdAt }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = NavyPrimary.copy(alpha = 0.10f), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Inbox, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(21.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Registration requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Customers and suppliers waiting for approval",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Counts side by side, so both databases are visible in one look
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RequestCountTile(
                    icon = Icons.Default.PersonAdd,
                    label = "Customers",
                    count = customerPending.size,
                    accent = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCustomerRequests
                )
                RequestCountTile(
                    icon = Icons.Default.Business,
                    label = "Suppliers",
                    count = supplierPending.size,
                    accent = Color(0xFF059669),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSupplierRequests
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (merged.isEmpty()) {
                Text(
                    "Nothing waiting. New registrations from the public form land here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                Text(
                    "Newest first (${merged.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Long lists stay scrollable inside the sheet without a nested LazyColumn
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    merged.take(30).forEach { row ->
                        ListRow(
                            title = row.title,
                            detail = row.detail,
                            note = requestDateFmt.format(Date(row.createdAt)),
                            horizontalPadding = 20.dp,
                            status = {
                                if (row.isSupplier) {
                                    StatusPill("Supplier", Color(0xFFECFDF5), Color(0xFF059669))
                                } else {
                                    StatusPill("Customer", Color(0xFFEFF6FF), Color(0xFF2563EB))
                                }
                            },
                            onClick = if (row.isSupplier) onOpenSupplierRequests else onOpenCustomerRequests
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Deletions an admin still has to confirm or reject
                if (isAdmin && onOpenDeleteRequests != null) {
                    RequestCountTile(
                        icon = Icons.Default.DeleteSweep,
                        label = "Delete requests",
                        count = pendingDeletions,
                        accent = Color(0xFFDC2626),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenDeleteRequests
                    )
                }
                PrimaryButton(
                    text = "Share registration link",
                    icon = Icons.Default.Share,
                    onClick = onShareLink,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        text = "Customer requests",
                        onClick = onOpenCustomerRequests,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = "Supplier requests",
                        onClick = onOpenSupplierRequests,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestCountTile(
    icon: ImageVector,
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.07f),
        modifier = modifier.heightIn(min = 64.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    "$count",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
        }
    }
}
