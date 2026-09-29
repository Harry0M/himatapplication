package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyState
import com.example.ui.components.InfoCard
import com.example.ui.components.ListRow
import com.example.ui.components.ScreenHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.UiDimens
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.DeletionRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val requestedAtFmt = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())

/**
 * Admin console for the delete queue.
 *
 * Staff cannot remove anything themselves; their delete hides the record and files a request. Here
 * an admin either deletes it for good or puts it back. Approving a trip also removes its orders,
 * which is spelled out before it happens.
 */
@Composable
fun DeletionRequestsScreen(
    viewModel: HimatViewModel,
    onBack: () -> Unit
) {
    val requests by viewModel.deletionRequests.collectAsStateWithLifecycle()
    val busyKey by viewModel.deletionActionBusy.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdminUser.collectAsStateWithLifecycle()
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()

    var toApprove by remember { mutableStateOf<DeletionRequest?>(null) }
    var toReject by remember { mutableStateOf<DeletionRequest?>(null) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Delete requests",
                    subtitle = if (requests.isEmpty()) "Nothing waiting" else "${requests.size} waiting for you",
                    onBack = onBack
                )
            }

            if (!isAdmin) {
                item {
                    InfoCard(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                        Text(
                            "Only Admins can confirm or reject a deletion",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "When you delete something it is hidden and sent here for the Admin to decide.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (requests.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nothing to review",
                        message = "When a staff member deletes a trip, order or master record, it lands here for your confirmation.",
                        modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)
                    )
                }
            } else {
                item {
                    Text(
                        "Each record is already hidden from the lists. Delete removes it for good; " +
                            "Restore puts it back exactly as it was.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)
                    )
                }
                items(requests, key = { it.key }) { request ->
                    val linkedOrders = if (request.cascades) {
                        entries.count { it.visitId == request.itemId }
                    } else 0
                    val busy = busyKey == request.key
                    ListRow(
                        title = request.itemSummary.ifBlank { "${request.entityLabel} #${request.itemId}" },
                        detail = listOf(
                            "Asked by ${request.deletedBy.ifBlank { "staff" }}",
                            request.deletedAt.takeIf { it > 0 }?.let { requestedAtFmt.format(Date(it)) }
                        ).filterNotNull().joinToString(" • "),
                        note = listOfNotNull(
                            request.deletionReason.takeIf { it.isNotBlank() }?.let { "Reason: $it" },
                            if (linkedOrders > 0) "Also removes $linkedOrders order${if (linkedOrders == 1) "" else "s"}" else null
                        ).joinToString(" • "),
                        status = {
                            StatusPill(
                                text = request.entityLabel,
                                background = Color(0xFFFEF3C7),
                                foreground = Color(0xFF92400E)
                            )
                        },
                        trailing = {
                            if (busy) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (isAdmin) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { toReject = request }) {
                                        Icon(
                                            Icons.Default.Restore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restore", style = MaterialTheme.typography.labelLarge)
                                    }
                                    TextButton(onClick = { toApprove = request }) {
                                        Icon(
                                            Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Delete",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    toApprove?.let { request ->
        val linkedOrders = if (request.cascades) entries.count { it.visitId == request.itemId } else 0
        AlertDialog(
            onDismissRequest = { toApprove = null },
            title = {
                Text(
                    "Delete this ${request.entityLabel.lowercase()} for good?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        request.itemSummary.ifBlank { "${request.entityLabel} #${request.itemId}" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (linkedOrders > 0) {
                            "This also removes $linkedOrders order${if (linkedOrders == 1) "" else "s"} on this trip. " +
                                "It cannot be undone, on any phone."
                        } else {
                            "This cannot be undone, on any phone."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.approveDeletionRequest(request)
                    toApprove = null
                }) {
                    Text("Delete for good", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { toApprove = null }) { Text("Keep it") }
            }
        )
    }

    toReject?.let { request ->
        AlertDialog(
            onDismissRequest = { toReject = null },
            title = {
                Text(
                    "Put it back?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "${request.itemSummary.ifBlank { request.entityLabel }} will appear in the lists again, " +
                        "exactly as it was. ${request.deletedBy.ifBlank { "The staff member" }} is not told.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rejectDeletionRequest(request)
                    toReject = null
                }) {
                    Text("Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { toReject = null }) { Text("Cancel") }
            }
        )
    }
}
