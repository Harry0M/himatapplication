package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import com.example.ui.components.DeliveryDaysSelector
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PackGroupEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.IncompleteCaseBanner
import com.example.ui.components.StatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.data.remote.FirebaseStorageService
import com.example.ui.dialogs.FullScreenImageViewerDialog
import kotlinx.coroutines.launch
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator

import com.example.util.hasMember
import com.example.util.isClosed
import com.example.util.isPhoneTrip
import com.example.util.tripMembers
import com.example.ui.components.InfoCard
import com.example.ui.components.SecondaryButton
import com.example.ui.components.StatusPill
import com.example.ui.components.TripStatusPill
import com.example.ui.dialogs.JoinTripSheet
import androidx.compose.material.icons.filled.GroupAdd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(
    viewModel: HimatViewModel,
    visit: VisitEntity,
    onBack: () -> Unit,
    onOpenAddEntry: () -> Unit,
    onOpenMixedPack: () -> Unit,
    onOpenCustomerReport: (VisitEntity) -> Unit,
    onOpenSupplierCopy: (VisitEntity, SupplierEntity) -> Unit
) {
    val entries by viewModel.visitEntries.collectAsStateWithLifecycle()
    val packGroups by viewModel.visitPackGroups.collectAsStateWithLifecycle()
    val suppliers by viewModel.visibleSuppliers.collectAsStateWithLifecycle()
    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val isAdminUser by viewModel.isAdminUser.collectAsStateWithLifecycle()
    val isAgentUser by viewModel.isAgentUser.collectAsStateWithLifecycle()
    val closeTripPrompt by viewModel.closeTripPrompt.collectAsStateWithLifecycle()

    // Several salesmen can work one trip; each can join from their own phone
    val tripMembers = remember(visit, employees) { visit.tripMembers(employees) }
    val isMember = visit.hasMember(currentEmployee)
    val isClosed = visit.isClosed()
    // Every staff member and admin can add an order to an open trip. A staff member who is not on
    // the trip joins it first, so the order carries their name (salesman on the customer report).
    //
    // Note there is deliberately no `currentEmployee != null` condition: a staff login whose
    // employee record has not loaded yet used to be locked out of the whole screen, which looked
    // like the app refusing their work. They can still book the order; it is credited to the trip's
    // salesman until their own record resolves.
    val canAddOrders = !isAgentUser && !isClosed
    val mustJoinFirst = canAddOrders && !isMember && currentEmployee != null
    val canJoin = !isAgentUser && !isClosed && !isMember && currentEmployee != null
    val canManageTrip = !isAgentUser
    var showCloseTripDialog by remember { mutableStateOf(false) }
    var showAddMemberSheet by remember { mutableStateOf(false) }
    var showJoinPrompt by remember { mutableStateOf(false) }
    val onAddOrder: () -> Unit = {
        if (mustJoinFirst) {
            showJoinPrompt = true
        } else {
            onOpenAddEntry()
        }
    }

    var showSupplierSheet by remember { mutableStateOf(false) }
    var showManagePackSheet by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<PurchaseEntryEntity?>(null) }
    var editingEntry by remember { mutableStateOf<PurchaseEntryEntity?>(null) }
    var showDeleteTripDialog by remember { mutableStateOf(false) }

    val totalPieces = entries.sumOf { it.pieces }
    val totalAmount = entries.sumOf { it.totalAmount }
    val totalCases = entries.sumOf { it.caseCount }

    // Set of entry IDs linked to any pack group in this visit
    val packedEntryIds = packGroups.flatMap { group ->
        group.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() }
    }.toSet()

    val isEntryPacked: (PurchaseEntryEntity) -> Boolean = { entry ->
        (entry.packGroupId != null && entry.packGroupId != 0L) || (entry.id in packedEntryIds)
    }

    val unfixedLooseEntries = entries.filter { it.loosePieces > 0 && !isEntryPacked(it) }
    val fixedLooseEntries = entries.filter { it.loosePieces > 0 && isEntryPacked(it) }
    val totalUnfixedLoose = unfixedLooseEntries.sumOf { it.loosePieces } + packGroups.sumOf { it.remainingLoose }

    // Distinct suppliers visited in this trip
    val visitedSuppliers = suppliers.filter { sup ->
        entries.any { it.supplierId == sup.id || (it.supplierName.isNotBlank() && it.supplierName.trim().equals(sup.name.trim(), ignoreCase = true)) }
    }

    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var fullscreenImageTitle by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (canAddOrders) {
                ExtendedFloatingActionButton(
                    onClick = onAddOrder,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New order", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Top Navigation & Customer Header (Borderless, flat & spacious)
            item {
                val customer = customers.find { it.id == visit.customerId || it.firmName.equals(visit.customerName, true) || it.name.equals(visit.customerName, true) }
                val visitPhotoUrl = customer?.let { it.purchaserPhotoUri.ifBlank { it.shopPhotoUri } }?.takeIf { it.isNotBlank() }
                    ?: entries.firstOrNull { !it.orderFormPhotoUri.isNullOrBlank() }?.orderFormPhotoUri

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Associated Visit / Customer Photo
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        if (!visitPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = visitPhotoUrl,
                                contentDescription = visit.customerName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = visit.customerName.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NavyPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = visit.customerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${visit.visitCode} • ${visit.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Status is shown here; closing happens with the "Close trip" button (with a summary)
                    Column(horizontalAlignment = Alignment.End) {
                        TripStatusPill(visit)
                        if (visit.isPhoneTrip()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusPill(
                                text = "Phone order",
                                background = MaterialTheme.colorScheme.secondaryContainer,
                                foreground = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    if (canManageTrip) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { showDeleteTripDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFEE2E2), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete Trip",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // 2. Salesmen on this trip (started, joined from their phones, or added)
            item {
                InfoCard(contentPadding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Salesmen on this trip (${tripMembers.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text(
                                text = tripMembers.joinToString(", ") { it.name }.ifBlank { visit.employeeName },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        if (canManageTrip && !isClosed) {
                            TextButton(onClick = { showAddMemberSheet = true }) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontWeight = FontWeight.Bold)
                            }
                        } else if (canJoin) {
                            TextButton(onClick = { viewModel.joinVisit(visit) }) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Join", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (canJoin) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Join to add orders from your phone - they will be saved under your name.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (isClosed) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val closedOn = visit.closedAt?.let {
                            java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(it))
                        }
                        Text(
                            text = "Closed" + (closedOn?.let { " on $it" } ?: "") + (visit.closedBy.takeIf { it.isNotBlank() }?.let { " by $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Close / reopen the trip
            if (canManageTrip) {
                item {
                    if (!isClosed) {
                        SecondaryButton(
                            text = "Close trip",
                            icon = Icons.Default.CheckCircle,
                            contentColor = Color(0xFF15803D),
                            onClick = { showCloseTripDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (isAdminUser) {
                        TextButton(onClick = { viewModel.reopenVisit(visit) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Reopen trip (admin)")
                        }
                    }
                }
            }

            // 3. Trip Sourcing Summary (Clean 4-metric strip, zero border, zero elevation)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Pcs", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("$totalPieces", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = NavyPrimary)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE2E8F0)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Full Cases", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("$totalCases", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFF059669))
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE2E8F0)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Loose Pcs", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            val totalLoose = entries.sumOf { it.loosePieces }
                            Text(
                                text = "$totalLoose",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (totalLoose > 0) Color(0xFFD97706) else Color(0xFF15803D)
                            )
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE2E8F0)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Trip Value", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(PdfGenerator.formatInr(totalAmount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        }
                    }
                }
            }

            // 4. Documents & Vouchers Toolbar (Clean, borderless separate row)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button 1: Customer Day Report
                    Button(
                        onClick = { onOpenCustomerReport(visit) },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Customer Report",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    // Button 2: Supplier Copy Picker
                    OutlinedButton(
                        onClick = { showSupplierSheet = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Supplier Bills (${visitedSuppliers.size})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Loose Pack Status Banner (Only if pack groups were explicitly configured)
            if (packGroups.isNotEmpty()) {
                item {
                    LoosePacksFixedBanner(
                        packGroups = packGroups,
                        remainingLoose = totalUnfixedLoose,
                        onManagePacks = { showManagePackSheet = true },
                        onCombineMore = onOpenMixedPack
                    )
                }
            }

            // Stops Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Supplier Stops (${entries.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    if (canAddOrders) {
                        TextButton(onClick = onAddOrder) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add order", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Entries List
            if (entries.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No orders in this trip yet", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (isClosed) "This trip is closed." else "Add an order when the customer buys from a supplier.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (canAddOrders) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onAddOrder,
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add first order", color = GoldAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                items(entries) { entry ->
                    PurchaseEntryItemCard(
                        entry = entry,
                        isPackedInGroup = isEntryPacked(entry),
                        onEdit = { editingEntry = entry },
                        onDelete = { entryToDelete = entry },
                        onViewSupplierCopy = {
                            val sup = suppliers.find {
                                it.id == entry.supplierId || (entry.supplierName.isNotBlank() && it.name.trim().equals(entry.supplierName.trim(), ignoreCase = true))
                            }
                            if (sup != null) {
                                onOpenSupplierCopy(visit, sup)
                            }
                        },
                        onViewPhoto = { url, title ->
                            fullscreenImageUrl = url
                            fullscreenImageTitle = title
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Purchase Entry?") },
            text = { Text("Remove order ${entry.orderNo} (${entry.itemCode} from ${entry.supplierName})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePurchaseEntry(entry)
                        entryToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSupplierSheet) {
        VisitedSupplierBottomSheet(
            visitedSuppliers = visitedSuppliers,
            entries = entries,
            onDismiss = { showSupplierSheet = false },
            onSelectSupplier = { sup ->
                showSupplierSheet = false
                onOpenSupplierCopy(visit, sup)
            }
        )
    }

    if (showManagePackSheet) {
        ManagePackGroupsBottomSheet(
            packGroups = packGroups,
            entries = entries,
            hasUnfixedLoose = totalUnfixedLoose > 0,
            onDismiss = { showManagePackSheet = false },
            onUnpackGroup = { group ->
                viewModel.deletePackGroup(group)
            },
            onCombineMore = {
                showManagePackSheet = false
                onOpenMixedPack()
            }
        )
    }

    editingEntry?.let { entry ->
        EditStopBottomSheet(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { updatedPieces, updatedRate, updatedCaseSize, updatedCases, updatedLoose, status, transp, expDate, payStatus, payMode, paidAmt, remarks, packNote, orderPhoto, billPhoto ->
                viewModel.updatePurchaseEntry(
                    entry = entry,
                    newPieces = updatedPieces,
                    newRate = updatedRate,
                    newCaseSize = updatedCaseSize,
                    newCaseCount = updatedCases,
                    newLoosePieces = updatedLoose,
                    deliveryStatus = status,
                    transporter = transp,
                    expectedDeliveryDate = expDate,
                    paymentStatus = payStatus,
                    paymentMode = payMode,
                    paidAmount = paidAmt,
                    paymentRemarks = remarks,
                    newMixedPackNote = packNote,
                    orderFormPhotoUri = orderPhoto,
                    supplierInvoiceUri = billPhoto,
                    onSuccess = {
                        editingEntry = null
                    }
                )
            }
        )
    }

    if (showDeleteTripDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTripDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Delete Trip / Visit?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Are you sure you want to delete this trip (${visit.visitCode} • ${visit.customerName})?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Text(
                            text = "⚠ WARNING: Deleting this trip will also permanently delete all ${entries.size} deliveries/orders associated with it.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB91C1C),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteTripDialog = false
                        viewModel.deleteVisit(visit) {
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Trip & Deliveries", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTripDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showJoinPrompt) {
        JoinTripSheet(
            visit = visit,
            memberNames = tripMembers.map { it.name },
            showAdminSkip = isAdminUser,
            isWorking = false,
            onJoinAndAddOrder = {
                showJoinPrompt = false
                viewModel.joinAndAddOrder(visit)
            },
            onAddWithoutJoining = {
                showJoinPrompt = false
                onOpenAddEntry()
            },
            onDismiss = { showJoinPrompt = false }
        )
    }

    // After the last supplier of a phone order
    val prompt = closeTripPrompt
    if (prompt != null && prompt.tripId == visit.id) {
        val saved = prompt.savedCount
        AlertDialog(
            onDismissRequest = { viewModel.dismissCloseTripPrompt() },
            title = { Text("Close this trip?") },
            text = {
                Text(
                    when (saved) {
                        0 -> "No orders were saved in this phone order."
                        1 -> "1 order saved in this phone order."
                        else -> "$saved orders saved in this phone order."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closeTripFromPrompt() }) {
                    Text("Close now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCloseTripPrompt() }) { Text("Later") }
            }
        )
    }

    if (showCloseTripDialog) {
        val pendingDelivery = entries.count { !it.deliveryStatus.equals("Delivered", ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { showCloseTripDialog = false },
            title = { Text("Close this trip?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${visit.customerName} • ${visit.visitCode}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${entries.size} orders • $totalPieces pcs • ₹${PdfGenerator.formatInr(totalAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Salesmen: ${tripMembers.joinToString(", ") { it.name }}", style = MaterialTheme.typography.bodySmall)
                    if (totalUnfixedLoose > 0) {
                        Text(
                            "$totalUnfixedLoose loose pieces are not packed yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309)
                        )
                    }
                    Text(
                        if (pendingDelivery > 0) "$pendingDelivery orders are not delivered yet. You can keep updating delivery status after closing."
                        else "All orders are delivered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCloseTripDialog = false
                        viewModel.closeVisit(visit)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) { Text("Close trip", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCloseTripDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    if (showAddMemberSheet) {
        com.example.ui.dialogs.EmployeeSearchBottomSheet(
            employees = employees.filter { e ->
                tripMembers.none { it.id == e.id } && !e.isBlocked && !e.status.equals("Deactivated", true)
            },
            selectedEmployee = null,
            onSelectEmployee = { emp ->
                viewModel.addVisitMember(visit, emp)
                showAddMemberSheet = false
            },
            onDismiss = { showAddMemberSheet = false }
        )
    }

    fullscreenImageUrl?.let { url ->
        FullScreenImageViewerDialog(
            imageUrl = url,
            title = fullscreenImageTitle,
            onDismiss = { fullscreenImageUrl = null }
        )
    }
}

@Composable
fun PurchaseEntryItemCard(
    entry: PurchaseEntryEntity,
    isPackedInGroup: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewSupplierCopy: () -> Unit,
    onViewPhoto: ((url: String, title: String) -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Order No, Supplier Name, Supplier Type Badge, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        color = NavyPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = entry.orderNo,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = NavyPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.supplierName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    SupplierTypeBadge(type = entry.supplierType)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Stop",
                            tint = NavyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color.Gray,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Item Code, Pieces, Grand Total, Delivery Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Item: ${entry.itemCode}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${entry.pieces} Pcs",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = PdfGenerator.formatInr(entry.grandTotalWithGst),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    StatusBadge(status = entry.deliveryStatus)
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Row 4: Payment Status Chip & View Supplier Voucher Quick Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isPaid = entry.paymentStatus.equals("Received", ignoreCase = true)
                val isPartial = entry.paymentStatus.equals("Partial", ignoreCase = true)
                val paymentBg = when {
                    isPaid -> Color(0xFFDCFCE7)
                    isPartial -> Color(0xFFE0F2FE)
                    else -> Color(0xFFFEF3C7)
                }
                val paymentTextColor = when {
                    isPaid -> Color(0xFF15803D)
                    isPartial -> Color(0xFF0369A1)
                    else -> Color(0xFFD97706)
                }

                Surface(
                    color = paymentBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = paymentTextColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Payment: ${entry.paymentStatus}" +
                                    (if (entry.paidAmount > 0) " (₹${"%,.0f".format(entry.paidAmount)})" else "") +
                                    (if (entry.paymentMode.isNotBlank()) " • ${entry.paymentMode}" else ""),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = paymentTextColor
                        )
                    }
                }

                Text(
                    text = "View Supplier Voucher →",
                    color = NavyPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onViewSupplierCopy() }
                        .padding(top = 2.dp, bottom = 2.dp)
                )
            }

            // Attached Spot Documents (Order Form Pic / Wholesaler Bill)
            val hasOrderPhoto = !entry.orderFormPhotoUri.isNullOrBlank()
            val hasBillPhoto = !entry.supplierInvoiceUri.isNullOrBlank()
            if (hasOrderPhoto || hasBillPhoto) {
                Spacer(modifier = Modifier.height(7.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasOrderPhoto) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onViewPhoto?.invoke(
                                        entry.orderFormPhotoUri!!,
                                        "Order Form - ${entry.orderNo} (${entry.supplierName})"
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Order Form Pic",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    if (hasBillPhoto) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onViewPhoto?.invoke(
                                        entry.supplierInvoiceUri!!,
                                        "Wholesaler Bill - ${entry.orderNo} (${entry.supplierName})"
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Wholesaler Bill",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitedSupplierBottomSheet(
    visitedSuppliers: List<SupplierEntity>,
    entries: List<PurchaseEntryEntity>,
    onDismiss: () -> Unit,
    onSelectSupplier: (SupplierEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredSuppliers = remember(visitedSuppliers, searchQuery) {
        if (searchQuery.isBlank()) {
            visitedSuppliers
        } else {
            val query = searchQuery.trim().lowercase()
            visitedSuppliers.filter {
                it.name.lowercase().contains(query) ||
                it.marketArea.lowercase().contains(query) ||
                it.type.lowercase().contains(query) ||
                it.brand.lowercase().contains(query)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 12.dp)
        ) {
            // Header (Compact, single-line bounded)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Supplier Bills",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "Select a supplier to view voucher",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NavyPrimary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "${visitedSuppliers.size} Stops",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar (Compact Pill Shape, concise placeholder)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search", style = MaterialTheme.typography.bodySmall, color = TextSecondary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NavyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_supplier_bills_input"),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (visitedSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No supplier stops logged yet",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            } else if (filteredSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No suppliers match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 290.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredSuppliers, key = { it.id }) { sup ->
                        val supEntries = entries.filter {
                            it.supplierId == sup.id || (it.supplierName.isNotBlank() && it.supplierName.trim().equals(sup.name.trim(), ignoreCase = true))
                        }
                        val orderCount = supEntries.size
                        val totalPieces = supEntries.sumOf { it.pieces }
                        val totalAmount = supEntries.sumOf { it.totalAmount }

                        Card(
                            onClick = { onSelectSupplier(sup) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFEDF2F7)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = NavyPrimary.copy(alpha = 0.08f),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = sup.name.take(2).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NavyPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(9.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = sup.name,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        SupplierTypeBadge(type = sup.type)
                                    }

                                    Spacer(modifier = Modifier.height(1.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (sup.marketArea.isNotBlank()) {
                                            Text(
                                                text = sup.marketArea,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.LightGray
                                            )
                                        }
                                        Text(
                                            text = "$orderCount ord • $totalPieces pcs",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = NavyPrimary,
                                            maxLines = 1
                                        )
                                        if (totalAmount > 0) {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.LightGray
                                            )
                                            Text(
                                                text = "₹${"%,.0f".format(totalAmount)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF2E7D32),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = NavyPrimary.copy(alpha = 0.08f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "View Voucher",
                                            tint = NavyPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}
}

@Composable
fun LoosePacksFixedBanner(
    packGroups: List<PackGroupEntity>,
    remainingLoose: Int = 0,
    onManagePacks: () -> Unit,
    onCombineMore: () -> Unit = {}
) {
    val totalPackedCases = packGroups.sumOf { it.resultingCases }
    val totalPackedPieces = packGroups.sumOf { it.combinedPieces }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = CircleShape,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(9.dp))
                Column {
                    Text(
                        text = "Loose Packs Fixed",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF15803D)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$totalPackedCases mixed case${if (totalPackedCases > 1) "s" else ""} ($totalPackedPieces pcs)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF166534)
                        )
                        if (remainingLoose > 0) {
                            Text(
                                text = " • $remainingLoose loose left",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (remainingLoose > 0) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onCombineMore() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Combine more loose packs",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onManagePacks() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Mixed Packs",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePackGroupsBottomSheet(
    packGroups: List<PackGroupEntity>,
    entries: List<PurchaseEntryEntity>,
    hasUnfixedLoose: Boolean,
    onDismiss: () -> Unit,
    onUnpackGroup: (PackGroupEntity) -> Unit,
    onCombineMore: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mixed Pack Groups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "Manage combined loose packs for this visit",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = "${packGroups.size} Groups",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(packGroups, key = { it.id }) { group ->
                    val linkedIds = group.linkedEntryIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                    val groupEntries = entries.filter { it.id in linkedIds || it.packGroupId == group.id }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = NavyPrimary.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = group.packGroupCode,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NavyPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${group.resultingCases} Mixed Case${if (group.resultingCases > 1) "s" else ""}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = " (${group.combinedPieces} pcs)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onUnpackGroup(group) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Undo,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Unpack",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }

                            if (group.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = group.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            if (groupEntries.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Included Stops:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                groupEntries.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${item.supplierName} (${item.itemCode})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${item.loosePieces} pcs loose",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = NavyPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (hasUnfixedLoose) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onCombineMore,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Combine Remaining Loose Pieces",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStopBottomSheet(
    entry: PurchaseEntryEntity,
    onDismiss: () -> Unit,
    onSave: (
        pieces: Int,
        rate: Double,
        caseSize: Int,
        caseCount: Int,
        loosePieces: Int,
        deliveryStatus: String,
        transporter: String,
        expectedDeliveryDate: String,
        paymentStatus: String,
        paymentMode: String,
        paidAmount: Double,
        paymentRemarks: String,
        mixedPackNote: String?,
        orderFormPhotoUri: String?,
        supplierInvoiceUri: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageService = remember { FirebaseStorageService() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var piecesText by remember(entry) { mutableStateOf(entry.pieces.toString()) }
    var casesText by remember(entry) { mutableStateOf(entry.caseCount.toString()) }
    var looseText by remember(entry) { mutableStateOf(entry.loosePieces.toString()) }
    var packingRemarks by remember(entry) { mutableStateOf(entry.mixedPackNote ?: "") }
    var rateText by remember(entry) { mutableStateOf(entry.rate.toString()) }
    var caseSizeText by remember(entry) { mutableStateOf(entry.caseSize.toString()) }
    var deliveryStatus by remember(entry) { mutableStateOf(entry.deliveryStatus) }
    var transporter by remember(entry) { mutableStateOf(entry.transporter) }
    var expectedDeliveryDate by remember(entry) { mutableStateOf(entry.expectedDeliveryDate) }
    var paymentStatus by remember(entry) { mutableStateOf(entry.paymentStatus) }
    var paymentMode by remember(entry) { mutableStateOf(entry.paymentMode) }
    var paidAmountText by remember(entry) {
        mutableStateOf(if (entry.paidAmount > 0) entry.paidAmount.toString() else "")
    }
    var paymentRemarks by remember(entry) { mutableStateOf(entry.paymentRemarks) }

    var orderFormPhotoUri by remember(entry) { mutableStateOf(entry.orderFormPhotoUri) }
    var isUploadingOrderForm by remember { mutableStateOf(false) }

    var supplierInvoiceUri by remember(entry) { mutableStateOf(entry.supplierInvoiceUri) }
    var isUploadingInvoice by remember { mutableStateOf(false) }

    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewImageTitle by remember { mutableStateOf("") }

    val orderFormLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isUploadingOrderForm = true
            coroutineScope.launch {
                val result = storageService.uploadFile(context, uri, folder = "purchase_orders/order_forms", prefix = "order_form")
                isUploadingOrderForm = false
                result.onSuccess { downloadUrl ->
                    orderFormPhotoUri = downloadUrl
                    Toast.makeText(context, "Order Form uploaded to Cloud!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    orderFormPhotoUri = uri.toString()
                    Toast.makeText(context, "Saved locally (offline)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val invoiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isUploadingInvoice = true
            coroutineScope.launch {
                val result = storageService.uploadFile(context, uri, folder = "purchase_orders/invoices", prefix = "bill")
                isUploadingInvoice = false
                result.onSuccess { downloadUrl ->
                    supplierInvoiceUri = downloadUrl
                    Toast.makeText(context, "Wholesaler Bill uploaded to Cloud!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    supplierInvoiceUri = uri.toString()
                    Toast.makeText(context, "Saved locally (offline)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Live calculations
    val pieces = piecesText.toIntOrNull() ?: 0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val caseSize = caseSizeText.toIntOrNull() ?: 24
    val enteredCases = casesText.toIntOrNull()
    val enteredLoose = looseText.toIntOrNull()
    val caseCount = enteredCases ?: (if (caseSize > 0) pieces / caseSize else 0)
    val loosePieces = enteredLoose ?: (if (caseSize > 0) pieces % caseSize else 0)
    val baseAmount = pieces * rate
    val gstAmount = (baseAmount * entry.gstRate) / 100.0
    val grandTotal = baseAmount + gstAmount

    val isValid = pieces > 0 && rate > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Stop: ${entry.orderNo}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "${entry.supplierName} • Item: ${entry.itemCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 1: Quantity & Pricing
            Text("Quantity & Price", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = piecesText,
                    onValueChange = { piecesText = it },
                    label = { Text("Total Pieces (Pc)") },
                    placeholder = { Text("75") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate (₹/pc)") },
                    placeholder = { Text("450") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = casesText,
                    onValueChange = { casesText = it },
                    label = { Text("Cases (Cs)") },
                    placeholder = { Text("2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
                OutlinedTextField(
                    value = looseText,
                    onValueChange = { looseText = it },
                    label = { Text("Loose (Pcs)") },
                    placeholder = { Text("5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = packingRemarks,
                onValueChange = { packingRemarks = it },
                label = { Text("Packing Remarks / Note (Optional)", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("e.g. Packed with Order HT-1002 / Wholesaler X", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            // Calculation Strip
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total + 5% GST",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = PdfGenerator.formatInr(grandTotal),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NavyPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Packaging",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "$caseCount Cases + $loosePieces Loose",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (loosePieces > 0) Color(0xFFD97706) else Color(0xFF15803D)
                            )
                        }
                    }
                    if (packingRemarks.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "↳ Note: $packingRemarks",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Delivery & Logistics
            Text("Delivery & Logistics", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Delivery Status", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Pending", "Packed", "Dispatched", "Delivered").forEach { status ->
                    val isSelected = deliveryStatus.equals(status, ignoreCase = true)
                    Surface(
                        color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { deliveryStatus = status }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeliveryDaysSelector(
                    expectedDeliveryDate = expectedDeliveryDate,
                    onDeliveryDateChange = { expectedDeliveryDate = it }
                )

                OutlinedTextField(
                    value = transporter,
                    onValueChange = { transporter = it },
                    label = { Text("Transporter / LR") },
                    placeholder = { Text("e.g. VRL, Jaipur Golden") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Payment Tracking
            Text("Payment Tracking", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Payment Status", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Pending", "Received", "Partial").forEach { status ->
                    val isSelected = paymentStatus.equals(status, ignoreCase = true)
                    val statusColor = when (status) {
                        "Received" -> Color(0xFF15803D)
                        "Partial" -> Color(0xFF0369A1)
                        else -> Color(0xFFD97706)
                    }
                    val statusBg = when (status) {
                        "Received" -> Color(0xFFDCFCE7)
                        "Partial" -> Color(0xFFE0F2FE)
                        else -> Color(0xFFFEF3C7)
                    }
                    Surface(
                        color = if (isSelected) statusBg else Color(0xFFF1F5F9),
                        border = if (isSelected) BorderStroke(1.5.dp, statusColor) else null,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                paymentStatus = status
                                if (status == "Received" && (paidAmountText.isBlank() || paidAmountText.toDoubleOrNull() == 0.0)) {
                                    paidAmountText = "%.2f".format(grandTotal)
                                } else if (status == "Pending") {
                                    paidAmountText = "0"
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (status) {
                                    "Received" -> "✓ Received"
                                    "Partial" -> "◑ Partial"
                                    else -> "⏳ Pending"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) statusColor else TextSecondary
                            )
                        }
                    }
                }
            }

            if (paymentStatus != "Pending") {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Payment Mode", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cash", "Online", "Cheque").forEach { mode ->
                        val isSelected = paymentMode.equals(mode, ignoreCase = true)
                        Surface(
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { paymentMode = mode }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (mode) {
                                        "Cash" -> "💵 Cash"
                                        "Online" -> "📱 Online"
                                        else -> "🏦 Cheque"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { paidAmountText = it },
                        label = { Text("Paid Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    OutlinedTextField(
                        value = paymentRemarks,
                        onValueChange = { paymentRemarks = it },
                        label = { Text("Ref / Remarks") },
                        placeholder = { Text("Txn ID / Cheque #") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Spot Proofs & Attachments
            Text("Spot Proofs & Attachments", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StopDocumentAttachmentCard(
                    title = "Order Form Pic",
                    subtitle = "Upload order form / slip",
                    uriString = orderFormPhotoUri,
                    isUploading = isUploadingOrderForm,
                    onPickImage = { orderFormLauncher.launch("image/*") },
                    onViewImage = {
                        previewImageUrl = orderFormPhotoUri
                        previewImageTitle = "Order Form Preview"
                    },
                    onRemoveImage = { orderFormPhotoUri = null }
                )

                StopDocumentAttachmentCard(
                    title = "Wholesaler Bill",
                    subtitle = "Upload wholesaler invoice / bill",
                    uriString = supplierInvoiceUri,
                    isUploading = isUploadingInvoice,
                    onPickImage = { invoiceLauncher.launch("image/*") },
                    onViewImage = {
                        previewImageUrl = supplierInvoiceUri
                        previewImageTitle = "Wholesaler Bill Preview"
                    },
                    onRemoveImage = { supplierInvoiceUri = null }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save / Cancel Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val parsedPaidAmount = paidAmountText.toDoubleOrNull() ?: 0.0
                        onSave(
                            pieces,
                            rate,
                            caseSize,
                            caseCount,
                            loosePieces,
                            deliveryStatus,
                            transporter,
                            expectedDeliveryDate,
                            paymentStatus,
                            paymentMode,
                            parsedPaidAmount,
                            paymentRemarks,
                            packingRemarks.trim().ifEmpty { null },
                            orderFormPhotoUri,
                            supplierInvoiceUri
                        )
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.5f).height(44.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes", color = GoldAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    previewImageUrl?.let { url ->
        FullScreenImageViewerDialog(
            imageUrl = url,
            title = previewImageTitle,
            onDismiss = { previewImageUrl = null }
        )
    }
}

