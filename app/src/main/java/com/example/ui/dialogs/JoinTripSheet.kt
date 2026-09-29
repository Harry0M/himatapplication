package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.StatusPill
import com.example.ui.components.UiDimens
import com.example.ui.theme.TextSecondary
import com.example.util.isClosed
import com.example.util.isPhoneTrip

/**
 * Shown when a salesman who is not on this trip taps "New order": they join first, so the order is
 * saved under their name and the trip shows up in their own list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTripSheet(
    visit: VisitEntity,
    memberNames: List<String>,
    showAdminSkip: Boolean,
    isWorking: Boolean,
    onJoinAndAddOrder: () -> Unit,
    onAddWithoutJoining: () -> Unit,
    onDismiss: () -> Unit
) {
    val closed = visit.isClosed()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("join_trip_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiDimens.ScreenPadding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Join this trip",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )

            Text(
                text = visit.customerName.ifBlank { "Customer" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = listOf("Trip ${visit.visitCode}", visit.date).filter { it.isNotBlank() }.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (visit.isPhoneTrip()) {
                StatusPill(
                    text = "Phone order",
                    background = MaterialTheme.colorScheme.secondaryContainer,
                    foreground = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = "On this trip: " + memberNames.filter { it.isNotBlank() }.joinToString(", ").ifBlank { "No salesman yet" },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Orders you add will be saved under your name.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (closed) {
                Text(
                    text = "This trip is already closed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (isWorking) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            }

            PrimaryButton(
                text = "Join & add order",
                icon = Icons.Default.GroupAdd,
                onClick = onJoinAndAddOrder,
                enabled = !closed && !isWorking,
                modifier = Modifier.fillMaxWidth()
            )

            if (showAdminSkip) {
                SecondaryButton(
                    text = "Add order without joining",
                    onClick = onAddWithoutJoining,
                    enabled = !closed && !isWorking,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
