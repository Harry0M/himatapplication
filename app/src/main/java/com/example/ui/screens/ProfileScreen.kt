package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.local.entity.EmployeeEntity
import com.example.ui.components.AppSearchField
import com.example.ui.components.InfoCard
import com.example.ui.components.ListRow
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.StatTile
import com.example.ui.components.StatusPill
import com.example.ui.components.UiDimens
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator
import com.example.util.ProfileStats
import com.example.util.Roles
import com.example.util.initialsOf
import java.util.Locale

/**
 * Profile = who am I, what did I do this month, and the two account actions (switch view, sign out).
 *
 * Read-only on purpose: names, phones and roles are edited in the Staff master by an admin, so the
 * same record cannot be changed from two places. Everything reads from the live session, so a role
 * change or a lost connection shows up here without reopening the screen.
 */
@Composable
fun ProfileScreen(
    viewModel: HimatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val isAdminUser by viewModel.isAdminUser.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val visits by viewModel.visibleVisits.collectAsStateWithLifecycle()
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()

    val pendingPush by viewModel.pendingPushCount.collectAsStateWithLifecycle()
    val uploading by viewModel.isUploadingPending.collectAsStateWithLifecycle()

    val isAgent = Roles.isAgent(currentRole)
    val isAdminView = currentRole.equals("Admin", ignoreCase = true)
    var showSignOut by remember { mutableStateOf(false) }
    var showSwitch by remember { mutableStateOf(false) }
    var staffQuery by remember { mutableStateOf("") }
    var uploadResult by remember { mutableStateOf<String?>(null) }

    // Opening Profile is a natural moment to check whether anything is still stuck on this phone
    LaunchedEffect(Unit) { viewModel.refreshPendingPushCount() }

    val displayName = currentEmployee?.name
        ?: currentUser?.displayName
        ?: if (isSuperAdmin) "Himat Textile Owner" else "My account"

    val roleLabel = when {
        isSuperAdmin && isAdminView -> "Admin (owner account)"
        isSuperAdmin -> "Viewing as ${currentEmployee?.name ?: "salesman"}"
        isAgent -> "Sub Agent"
        Roles.isAdmin(currentRole) -> "Admin"
        else -> "Staff (Salesman)"
    }

    val stats = remember(visits, entries, currentEmployee, isAdminUser) {
        ProfileStats.compute(
            visits = visits,
            entries = entries,
            employee = currentEmployee,
            isAdmin = isAdminUser && currentEmployee == null
        )
    }

    // Same `.info/connected` flag the header chip uses, so the two can never disagree
    val online by viewModel.isOnline.collectAsStateWithLifecycle()

    val salesmen = remember(employees, staffQuery) {
        val base = employees.filter { !it.isDeleted && !Roles.isAgent(it.role) }
        if (staffQuery.isBlank()) base else {
            val q = staffQuery.trim().lowercase(Locale.getDefault())
            base.filter {
                it.name.lowercase(Locale.getDefault()).contains(q) ||
                    it.employeeId.lowercase(Locale.getDefault()).contains(q) ||
                    it.phone.contains(q)
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader(title = "Profile", subtitle = "Himat Textile", onBack = onBack)
            }

            // Who is signed in
            item {
                InfoCard(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            photoUri = currentEmployee?.photoUri.orEmpty(),
                            fallbackUrl = currentUser?.photoUrl?.toString().orEmpty(),
                            name = displayName,
                            isAdmin = isAdminView
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { heading() }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusPill(
                                text = roleLabel,
                                background = NavyPrimary.copy(alpha = 0.10f),
                                foreground = NavyPrimary
                            )
                        }
                    }
                }
            }

            // Account details, one fact per row
            item {
                Column(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                    SectionHeader(title = "Account")
                }
            }
            item {
                ProfileFactRow(
                    icon = Icons.Default.Email,
                    label = "Signed in with",
                    value = currentUser?.email.orEmpty().ifBlank { "Google account" }
                )
            }
            currentEmployee?.let { emp ->
                if (emp.phone.isNotBlank()) {
                    item {
                        ProfileFactRow(icon = Icons.Default.Phone, label = "Phone", value = emp.phone)
                    }
                }
                if (emp.employeeId.isNotBlank()) {
                    item {
                        ProfileFactRow(
                            icon = Icons.Default.Badge,
                            label = if (isAgent) "Sub agent ID" else "Staff ID",
                            value = emp.employeeId
                        )
                    }
                }
                if (emp.city.isNotBlank()) {
                    item {
                        ProfileFactRow(icon = Icons.Default.Person, label = "City", value = emp.city)
                    }
                }
            }
            item {
                ProfileFactRow(
                    icon = if (online) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    label = if (online) "Synced with the office" else "Offline — saving on this phone",
                    value = if (online) "Live" else "Waiting",
                    valueColor = if (online) Color(0xFF15803D) else Color(0xFFB45309)
                )
            }
            // Anything saved on this phone that the office has not confirmed yet
            item {
                ProfileFactRow(
                    icon = Icons.Default.CloudUpload,
                    label = when {
                        uploading -> "Uploading to the office..."
                        pendingPush > 0 -> "$pendingPush trips / orders not uploaded yet"
                        else -> "Everything on this phone is uploaded"
                    },
                    value = when {
                        uploading -> "Working"
                        pendingPush > 0 -> "Upload now"
                        else -> "Done"
                    },
                    valueColor = if (pendingPush > 0) Color(0xFFB45309) else Color(0xFF15803D),
                    onClick = if (uploading) null else ({
                        viewModel.uploadPendingToCloud { sent ->
                            uploadResult = if (sent > 0) {
                                "$sent record${if (sent == 1) "" else "s"} sent to the office"
                            } else {
                                "Nothing left to upload"
                            }
                        }
                    })
                )
            }
            uploadResult?.let { message ->
                item {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)
                    )
                }
            }

            // This month
            item {
                Column(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                    SectionHeader(
                        title = if (isAdminUser && currentEmployee == null) "Agency this month" else "My work this month"
                    )
                    Text(
                        stats.monthLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = "Trips", value = "${stats.trips}", modifier = Modifier.weight(1f))
                        StatTile(label = "Orders", value = "${stats.orders}", modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(label = "Pieces", value = "${stats.pieces}", modifier = Modifier.weight(1f))
                        StatTile(
                            label = "Order value",
                            value = "₹${PdfGenerator.formatInr(stats.amount)}",
                            modifier = Modifier.weight(1f),
                            accent = Color(0xFF047857)
                        )
                    }
                }
            }

            // A Sub Agent's own book of customers
            if (isAgent) {
                item {
                    Column(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                        SectionHeader(title = "My customers", count = customers.size)
                        Text(
                            "Customers who registered through your link, or were linked to you by the office.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                items(customers.take(20), key = { "cust_${it.id}" }) { c ->
                    ListRow(
                        title = c.firmName.ifBlank { c.name },
                        detail = listOf(c.name.takeIf { it.isNotBlank() && it != c.firmName }, c.city, c.phone)
                            .filter { !it.isNullOrBlank() }
                            .joinToString(" • "),
                        onClick = { viewModel.openCustomerDetail(c) }
                    )
                }
            }

            // Switch view (owner only) — the one place this exists
            if (isSuperAdmin) {
                item {
                    Column(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                        SectionHeader(title = "Switch view")
                        Text(
                            "Check what a salesman sees on their phone. Nothing is changed for them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                item {
                    ProfileFactRow(
                        icon = Icons.Default.AdminPanelSettings,
                        label = "Admin (everything)",
                        value = if (isAdminView) "Active" else "Switch",
                        valueColor = if (isAdminView) Color(0xFF15803D) else NavyPrimary,
                        onClick = { viewModel.setRole("Admin", null) }
                    )
                }
                item {
                    ProfileFactRow(
                        icon = Icons.Default.Groups,
                        label = "A salesman's view",
                        value = if (showSwitch) "Hide list" else "Pick staff",
                        valueColor = NavyPrimary,
                        onClick = { showSwitch = !showSwitch }
                    )
                }
                if (showSwitch) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                            AppSearchField(
                                query = staffQuery,
                                onQueryChange = { staffQuery = it },
                                placeholder = "Find staff by name, ID or phone"
                            )
                        }
                    }
                    if (salesmen.isEmpty()) {
                        item {
                            Text(
                                "No staff found. Add them in Masters → Staff.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)
                            )
                        }
                    }
                    items(salesmen, key = { "staff_${it.id}" }) { emp ->
                        val isActiveView = !isAdminView && currentEmployee?.id == emp.id
                        ListRow(
                            title = emp.name,
                            detail = listOf(emp.role, emp.employeeId, emp.phone)
                                .filter { it.isNotBlank() }
                                .joinToString(" • "),
                            status = if (isActiveView) {
                                { StatusPill("Active", Color(0xFFDCFCE7), Color(0xFF15803D)) }
                            } else null,
                            onClick = { viewModel.setRole("Salesman", emp) }
                        )
                    }
                }
            } else {
                item {
                    InfoCard(modifier = Modifier.padding(horizontal = UiDimens.ScreenPadding)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Your role is set by the office",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (isAgent) {
                                        "As a Sub Agent you see only your own customers and their orders."
                                    } else {
                                        "Ask the admin if you need access to something you cannot see."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Sub agent shortcut for admins, because this is where people look for it
            if (isAdminUser && !isAgent) {
                item {
                    ProfileFactRow(
                        icon = Icons.Default.SupportAgent,
                        label = "Sub agents",
                        value = "Open",
                        valueColor = NavyPrimary,
                        onClick = { viewModel.navigateTo(AppScreen.SUB_AGENT_MASTER) }
                    )
                }
            }

            item {
                SecondaryButton(
                    text = "Sign out",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = { showSignOut = true },
                    contentColor = Color(0xFFDC2626),
                    modifier = Modifier
                        .padding(horizontal = UiDimens.ScreenPadding)
                        .fillMaxWidth()
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Himat Textile ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        "Works offline; saves to the office as soon as there is signal.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    if (showSignOut) {
        AlertDialog(
            onDismissRequest = { showSignOut = false },
            title = { Text("Sign out?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Anything already saved stays safe. Sign back in with the same Google account to continue.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSignOut = false
                    viewModel.signOut(context)
                }) {
                    Text("Sign out", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOut = false }) { Text("Stay signed in") }
            }
        )
    }
}

/** Photo if we have one, otherwise the person's initials. Same 56dp box either way. */
@Composable
private fun ProfileAvatar(
    photoUri: String,
    fallbackUrl: String,
    name: String,
    isAdmin: Boolean
) {
    val url = photoUri.ifBlank { fallbackUrl }
    Surface(
        shape = CircleShape,
        color = if (isAdmin) NavyPrimary else Color(0xFF059669),
        modifier = Modifier.size(56.dp)
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "Photo of $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initialsOf(name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/** One fact: icon, what it is, and the value on the right. Tappable rows get a 48dp target. */
@Composable
private fun ProfileFactRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
                } else Modifier
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = UiDimens.ScreenPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}



