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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.util.RecordDetail
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
                    RequestCard(
                        request = request,
                        busy = busyKey == request.key,
                        isAdmin = isAdmin,
                        loadDetail = { viewModel.loadDeletionDetail(request) },
                        onApprove = { toApprove = request },
                        onReject = { toReject = request }
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

/**
 * One request in the queue, with the record itself rather than just a one-line summary.
 *
 * The full record is loaded when the card first appears, because deciding on a delete without seeing
 * the amount, the payment state and whether the goods have already gone is guesswork. Warnings come
 * first: "₹42,000 has already been received for this order" is the sentence that should stop a tap.
 */
@Composable
private fun RequestCard(
    request: DeletionRequest,
    busy: Boolean,
    isAdmin: Boolean,
    loadDetail: suspend () -> RecordDetail,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var detail by remember(request.key) { mutableStateOf<RecordDetail?>(null) }
    var expanded by remember(request.key) { mutableStateOf(false) }

    LaunchedEffect(request.key) {
        detail = loadDetail()
    }

    InfoCard(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detail?.title ?: request.itemSummary.ifBlank { "${request.entityLabel} #${request.itemId}" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                detail?.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusPill(
                text = request.entityLabel,
                background = Color(0xFFFEF3C7),
                foreground = Color(0xFF92400E)
            )
        }

        detail?.amount?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                it,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F766E)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = listOfNotNull(
                "Asked by ${request.deletedBy.ifBlank { "staff" }}",
                request.deletedByRole.takeIf { it.isNotBlank() },
                request.deletedAt.takeIf { it > 0 }?.let { requestedAtFmt.format(Date(it)) }
            ).joinToString(" • "),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        request.deletionReason.takeIf { it.isNotBlank() }?.let {
            Text(
                "Reason: $it",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // What else this takes with it, and anything that should give an admin pause
        detail?.warnings?.forEach { warning ->
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB45309)
                )
            }
        }

        val fields = detail?.fields.orEmpty()
        if (fields.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (expanded) "Hide the full record" else "See the full record (${fields.size} fields)",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (expanded) {
                fields.forEach { (label, value) ->
                    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.width(116.dp)
                        )
                        Text(
                            value,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (busy) {
            Spacer(modifier = Modifier.height(10.dp))
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else if (isAdmin) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReject) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onApprove) {
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
}
