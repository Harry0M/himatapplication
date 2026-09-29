package com.example.ui.dialogs

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.SecondaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.Permission
import com.example.util.Permissions
import com.example.util.Roles
import com.example.util.SyncStatus

/**
 * "What am I allowed to do, and is my work saved?" — opened from the i button in the header.
 *
 * Everything on it is live: the role comes from the session, the permission list is computed from
 * that role rather than written out by hand, and the sync line reflects the database connection as
 * it changes. Refresh re-checks the account against the office, which is what a user needs when an
 * admin has just changed their role or unblocked them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSheet(
    viewModel: HimatViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val isOwner by viewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val isAuthorized by viewModel.isAuthorized.collectAsStateWithLifecycle()
    val authorizationMessage by viewModel.authorizationMessage.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val uploading by viewModel.isUploadingPending.collectAsStateWithLifecycle()

    var refreshing by remember { mutableStateOf(false) }

    val profile = remember(currentRole, isOwner, currentEmployee) {
        Permissions.profileFor(
            role = currentRole,
            isOwner = isOwner,
            hasStaffRecord = currentEmployee != null
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = currentEmployee?.name
                    ?: currentUser?.displayName
                    ?: currentUser?.email
                    ?: "My access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentUser?.email.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = profile.roleLabel,
                    background = NavyPrimary.copy(alpha = 0.10f),
                    foreground = NavyPrimary
                )
                if (Roles.isAgent(currentRole)) {
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusPill("Read only", Color(0xFFFEF3C7), Color(0xFF92400E))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(profile.roleNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            if (isAuthorized == false) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFEF2F2)) {
                    Text(
                        authorizationMessage ?: "This account is not allowed into the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB91C1C),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sync line
            SyncRow(status = syncStatus, busy = uploading, onUpload = { viewModel.uploadPendingToCloud() })

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "What you can do",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${profile.allowedCount} allowed • ${profile.blockedCount} not allowed",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            profile.permissions.forEach { permission -> PermissionRow(permission) }

            Spacer(modifier = Modifier.height(18.dp))
            SecondaryButton(
                text = if (refreshing) "Checking with the office..." else "Refresh my access",
                icon = Icons.Default.Refresh,
                enabled = !refreshing,
                onClick = {
                    refreshing = true
                    viewModel.refreshAccess { refreshing = false }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Use this after the Admin changes your role or unblocks you.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SyncRow(status: SyncStatus, busy: Boolean, onUpload: () -> Unit) {
    val (icon, tint) = when (status) {
        is SyncStatus.Offline -> Icons.Default.CloudOff to Color(0xFFB45309)
        is SyncStatus.Pending -> Icons.Default.CloudUpload to Color(0xFFB45309)
        is SyncStatus.Uploading -> Icons.Default.CloudUpload to NavyPrimary
        SyncStatus.Syncing -> Icons.Default.Sync to NavyPrimary
        SyncStatus.Synced -> Icons.Default.CloudDone to Color(0xFF15803D)
    }
    val tappable = status is SyncStatus.Pending && !busy

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
        onClick = onUpload,
        enabled = tappable
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (busy) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    status.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tint
                )
                Text(status.detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun PermissionRow(permission: Permission) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (permission.allowed) Icons.Default.CheckCircle else Icons.Default.Block,
            contentDescription = if (permission.allowed) "Allowed" else "Not allowed",
            tint = if (permission.allowed) Color(0xFF15803D) else Color(0xFF9CA3AF),
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                permission.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (permission.allowed) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (permission.note.isNotBlank()) {
                Text(permission.note, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
