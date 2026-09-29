package com.example.ui.dialogs

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.ui.theme.TextSecondary
import com.example.util.DeleteImpact
import com.example.util.OrphanGroup

/**
 * Shown before anything is deleted, whenever other records are attached to it.
 *
 * Two kinds of attachment are listed separately, because they need different decisions:
 *  - records the app can remove together with the parent (a trip's orders, a customer's trips), with
 *    a tick box so the user chooses;
 *  - records that only mention the parent by name and are left alone, listed so nobody is surprised.
 *
 * [hardDelete] changes the wording: an admin is told it cannot be undone, everyone else is told it
 * goes to the admin for confirmation.
 */
@Composable
fun DeleteImpactDialog(
    impact: DeleteImpact,
    hardDelete: Boolean,
    onCancel: () -> Unit,
    onConfirm: (alsoRemoveLinked: Boolean) -> Unit
) {
    val removable = impact.removableGroups
    val advisory = impact.advisoryGroups
    // Taking the linked records along is the safe default: leaving orphans behind is what causes
    // "my order vanished" reports later.
    var alsoRemove by remember { mutableStateOf(removable.isNotEmpty()) }

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Surface(shape = CircleShape, color = Color(0xFFFEF3C7), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = if (hardDelete) {
                    "Delete this ${impact.kind.label.lowercase()}?"
                } else {
                    "Ask the Admin to delete this ${impact.kind.label.lowercase()}?"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    impact.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Other records are attached to it:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                removable.forEach { group -> ImpactRow(group, willBeRemoved = alsoRemove) }
                advisory.forEach { group -> ImpactRow(group, willBeRemoved = false, advisory = true) }

                if (removable.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = alsoRemove, onCheckedChange = { alsoRemove = it })
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Also ${if (hardDelete) "delete" else "send"} the ${impact.removableCount} linked record${if (impact.removableCount == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (alsoRemove) {
                                        "Nothing is left pointing at a record that is gone."
                                    } else {
                                        "They will stay behind with nothing to belong to."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (hardDelete) {
                        "This cannot be undone, on any phone."
                    } else {
                        "Nothing is lost: the records are hidden and the Admin can restore them."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hardDelete) Color(0xFFB91C1C) else Color(0xFF15803D),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(alsoRemove) }) {
                Text(
                    text = if (hardDelete) "Delete" else "Send request",
                    color = if (hardDelete) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Keep it") }
        }
    )
}

/** One line of the impact list: how many records, and what happens to them. */
@Composable
private fun ImpactRow(
    group: OrphanGroup,
    willBeRemoved: Boolean,
    advisory: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            if (advisory) Icons.Default.Info else Icons.Default.Warning,
            contentDescription = null,
            tint = if (advisory) TextSecondary else Color(0xFFB45309),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${group.count} × ${group.label}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    advisory -> "Left as they are. ${group.consequence}."
                    willBeRemoved -> "Will be removed too."
                    else -> "${group.consequence}."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
