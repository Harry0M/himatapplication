package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.CustomerRegistrationRequestEntity
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.rememberDialogBottomPadding
import com.example.ui.viewmodel.HimatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun dialPhone(context: Context, phone: String) {
    if (phone.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
    }
}

private fun openWhatsApp(context: Context, rawPhone: String, message: String = "") {
    if (rawPhone.isBlank()) return
    val clean = rawPhone.filter { it.isDigit() }.let {
        if (it.length == 10) "91$it" else it
    }
    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$clean&text=${Uri.encode(message)}")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRequestsDialog(
    viewModel: HimatViewModel,
    onDismiss: () -> Unit
) {
    val requests by viewModel.registrationRequests.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var filterTab by remember { mutableStateOf("PENDING") } // "PENDING", "APPROVED", "REJECTED", "ALL"
    var searchQuery by remember { mutableStateOf("") }
    var selectedRequest by remember { mutableStateOf<CustomerRegistrationRequestEntity?>(null) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var fullscreenImageTitle by remember { mutableStateOf("") }

    val pendingCount = remember(requests) { requests.count { it.status.equals("PENDING", ignoreCase = true) } }
    val approvedCount = remember(requests) { requests.count { it.status.equals("APPROVED", ignoreCase = true) } }
    val rejectedCount = remember(requests) { requests.count { it.status.equals("REJECTED", ignoreCase = true) } }

    val filteredList = remember(requests, filterTab, searchQuery) {
        val q = searchQuery.trim().lowercase()
        requests.filter { req ->
            val matchesTab = when (filterTab) {
                "PENDING" -> req.status.equals("PENDING", ignoreCase = true)
                "APPROVED" -> req.status.equals("APPROVED", ignoreCase = true)
                "REJECTED" -> req.status.equals("REJECTED", ignoreCase = true)
                else -> true
            }
            val matchesQuery = q.isEmpty() ||
                    req.firmName.lowercase().contains(q) ||
                    req.name.lowercase().contains(q) ||
                    req.phone.contains(q) ||
                    req.city.lowercase().contains(q) ||
                    req.marketArea.lowercase().contains(q) ||
                    req.garmentTypes.lowercase().contains(q)
            matchesTab && matchesQuery
        }.sortedByDescending { it.createdAt }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = NavyPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Registration Requests",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$pendingCount pending verification",
                                color = GoldAccent,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Search & Filter Tabs
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by firm, name, phone, city...", fontSize = 11.5.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterTab == "PENDING",
                            onClick = { filterTab = "PENDING" },
                            label = { Text("Pending ($pendingCount)", fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEF3C7),
                                selectedLabelColor = Color(0xFF92400E)
                            )
                        )
                        FilterChip(
                            selected = filterTab == "APPROVED",
                            onClick = { filterTab = "APPROVED" },
                            label = { Text("Approved ($approvedCount)", fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDCFCE7),
                                selectedLabelColor = Color(0xFF166534)
                            )
                        )
                        FilterChip(
                            selected = filterTab == "REJECTED",
                            onClick = { filterTab = "REJECTED" },
                            label = { Text("Rejected ($rejectedCount)", fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEE2E2),
                                selectedLabelColor = Color(0xFF991B1B)
                            )
                        )
                        FilterChip(
                            selected = filterTab == "ALL",
                            onClick = { filterTab = "ALL" },
                            label = { Text("All (${requests.size})", fontSize = 10.5.sp) }
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Requests List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No requests found",
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "Try changing your search terms" else "No $filterTab customer registration requests yet",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { req ->
                            RequestListItemCard(
                                request = req,
                                onClick = { selectedRequest = req },
                                onCall = { dialPhone(context, req.phone) },
                                onWhatsApp = { openWhatsApp(context, req.phone, "Hello ${req.firmName.ifBlank { req.name }}, greetings from Himat Agency!") }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full Request Inspection Detail Dialog
    selectedRequest?.let { req ->
        CustomerRequestInspectorDialog(
            request = req,
            employees = employees,
            onDismiss = { selectedRequest = null },
            onApprove = { adminReligion, creditType, creditDays, creditLimit, agentId, agentName ->
                viewModel.approveRegistrationRequest(
                    request = req,
                    adminReligion = adminReligion,
                    creditType = creditType,
                    creditDays = creditDays,
                    creditLimit = creditLimit,
                    assignedAgentId = agentId,
                    assignedAgentName = agentName
                ) { newCustId ->
                    Toast.makeText(context, "Approved! Customer #$newCustId created successfully.", Toast.LENGTH_LONG).show()
                    selectedRequest = null
                }
            },
            onReject = { reason ->
                viewModel.rejectRegistrationRequest(req, reason) {
                    Toast.makeText(context, "Request rejected.", Toast.LENGTH_SHORT).show()
                    selectedRequest = null
                }
            },
            onViewImage = { url, title ->
                fullscreenImageUrl = url
                fullscreenImageTitle = title
            }
        )
    }

    // Lightbox image viewer
    fullscreenImageUrl?.let { url ->
        FullScreenImageViewerDialog(
            imageUrl = url,
            title = fullscreenImageTitle,
            subtitle = "Pinch to zoom",
            onDismiss = { fullscreenImageUrl = null }
        )
    }
}

@Composable
private fun RequestListItemCard(
    request: CustomerRegistrationRequestEntity,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val statusColor = when (request.status.uppercase()) {
        "APPROVED" -> Color(0xFF16A34A)
        "REJECTED" -> Color(0xFFDC2626)
        else -> Color(0xFFD97706)
    }
    val statusBg = when (request.status.uppercase()) {
        "APPROVED" -> Color(0xFFDCFCE7)
        "REJECTED" -> Color(0xFFFEE2E2)
        else -> Color(0xFFFEF3C7)
    }

    val dateStr = remember(request.createdAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(request.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.firmName.ifBlank { request.name },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextPrimary
                    )
                    if (request.firmName.isNotBlank() && request.name.isNotBlank() && request.firmName != request.name) {
                        Text(
                            text = "Prop: ${request.name}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text(
                        text = request.status.uppercase(),
                        color = statusColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Location & Garments
            Text(
                text = "${request.city} ${if (request.marketArea.isNotBlank()) "• ${request.marketArea}" else ""}",
                fontSize = 11.sp,
                color = TextSecondary
            )

            if (request.garmentTypes.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Deals in: ${request.garmentTypes}",
                    fontSize = 10.sp,
                    color = NavyPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    fontSize = 9.5.sp,
                    color = TextSecondary.copy(alpha = 0.8f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = Color(0xFF25D366).copy(alpha = 0.12f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onWhatsApp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("WA", fontWeight = FontWeight.Black, fontSize = 9.sp, color = Color(0xFF128C7E))
                        }
                    }

                    Surface(
                        color = NavyPrimary.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onCall)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = NavyPrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Inspect", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerRequestInspectorDialog(
    request: CustomerRegistrationRequestEntity,
    employees: List<com.example.data.local.entity.EmployeeEntity>,
    onDismiss: () -> Unit,
    onApprove: (religion: String, creditType: String, creditDays: Int, creditLimit: Double, agentId: Long?, agentName: String) -> Unit,
    onReject: (reason: String) -> Unit,
    onViewImage: (url: String, title: String) -> Unit
) {
    val context = LocalContext.current

    // Onboarding form state
    var selectedReligion by remember { mutableStateOf(request.religion.ifBlank { "Hindu" }) }
    var religionExpanded by remember { mutableStateOf(false) }
    val religionOptions = listOf("Hindu", "Muslim", "Jain", "Sikh", "Christian", "Other")

    var creditType by remember { mutableStateOf("Cash") }
    var creditDays by remember { mutableStateOf("30") }
    var creditLimit by remember { mutableStateOf("0") }

    var selectedAgentId by remember { mutableStateOf<Long?>(null) }
    var selectedAgentName by remember { mutableStateOf("") }
    var agentExpanded by remember { mutableStateOf(false) }

    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Surface(
                    color = NavyPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = request.firmName.ifBlank { request.name },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Request ID: ${request.id}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 9.5.sp
                            )
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                        .then(
                            if (!request.status.equals("PENDING", ignoreCase = true)) {
                                Modifier.navigationBarsPadding()
                            } else Modifier
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Contact Card
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Contact & Location", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NavyPrimary)
                            Spacer(modifier = Modifier.height(8.dp))

                            InspectorRow("Firm Name", request.firmName)
                            InspectorRow("Contact Person", request.name)
                            InspectorRow("Primary Phone", request.phone)
                            if (request.phone2.isNotBlank()) InspectorRow("Alt Phone", request.phone2)
                            if (request.email.isNotBlank()) InspectorRow("Email", request.email)
                            InspectorRow("City / District", "${request.city} / ${request.district.ifBlank { request.state }}")
                            InspectorRow("Market Area", request.marketArea)
                            InspectorRow("Full Address", request.address.ifBlank { request.shopAddress })
                            if (request.pincode.isNotBlank()) InspectorRow("Pincode", request.pincode)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { dialPhone(context, request.phone) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { openWhatsApp(context, request.phone, "Hello ${request.firmName}, regarding your customer registration with Himat Agency:") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF128C7E)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                if (request.shopMapLink.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.shopMapLink)))
                                            } catch (_: Exception) {}
                                        },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Map, contentDescription = "Map", modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Business & Tax Info
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Business & Commercial Info", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NavyPrimary)
                            Spacer(modifier = Modifier.height(8.dp))

                            InspectorRow("Deals In", request.garmentTypes)
                            InspectorRow("GSTIN", request.gstin.ifBlank { "Not Provided" })
                            InspectorRow("PAN", request.panNumber.ifBlank { "Not Provided" })
                            if (request.preferredTransporterName.isNotBlank()) {
                                InspectorRow("Transport Preference", "${request.preferredTransporterName} (${request.transportPreference})")
                            }
                            if (request.bankName.isNotBlank()) {
                                InspectorRow("Bank Details", "${request.bankName} • A/C: ${request.accountNumber} • IFSC: ${request.ifscCode}")
                            }
                            if (request.notes.isNotBlank()) {
                                InspectorRow("Applicant Notes", request.notes)
                            }
                        }
                    }

                    // KYC Documents & Photos (Click to Zoom)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("KYC Photos & Documents", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NavyPrimary)
                                Text("Tap to zoom", fontSize = 9.5.sp, color = TextSecondary)
                            }

                            Spacer(modifier = Modifier.height(9.dp))

                            val photos = listOfNotNull(
                                if (request.shopPhotoUri.isNotBlank()) Pair("Shop Front Photo", request.shopPhotoUri) else null,
                                if (request.gstCertPhotoUri.isNotBlank()) Pair("GST Certificate", request.gstCertPhotoUri) else null,
                                if (request.panPhotoUri.isNotBlank()) Pair("PAN Card", request.panPhotoUri) else null,
                                if (request.aadharPhotoUri.isNotBlank()) Pair("Aadhaar Card", request.aadharPhotoUri) else null
                            )

                            if (photos.isEmpty()) {
                                Text(
                                    text = "No document photos attached.",
                                    color = TextSecondary,
                                    fontSize = 10.5.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    photos.forEach { (title, url) ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(104.dp)
                                                .clickable { onViewImage(url, title) }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(104.dp, 80.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFF1F5F9))
                                            ) {
                                                AsyncImage(
                                                    model = url,
                                                    contentDescription = title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.5f),
                                                    shape = CircleShape,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(4.dp)
                                                        .size(20.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Visibility,
                                                        contentDescription = "Zoom",
                                                        tint = Color.White,
                                                        modifier = Modifier.padding(3.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = title,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Admin Approval / Setup Form (Only if PENDING)
                    if (request.status.equals("PENDING", ignoreCase = true)) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.7f)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Admin Onboarding Configuration",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.height(9.dp))

                                // RELIGION SELECTION (User Explicit Requirement)
                                Text(
                                    text = "Religion (धर्म):",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))

                                ExposedDropdownMenuBox(
                                    expanded = religionExpanded,
                                    onExpandedChange = { religionExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedReligion,
                                        onValueChange = { selectedReligion = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = religionExpanded) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = religionExpanded,
                                        onDismissRequest = { religionExpanded = false }
                                    ) {
                                        religionOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt, fontSize = 11.sp) },
                                                onClick = {
                                                    selectedReligion = opt
                                                    religionExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(9.dp))

                                // Customer Type & Credit Terms
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = creditType == "Cash",
                                        onClick = { creditType = "Cash" },
                                        label = { Text("Cash Party", fontSize = 10.5.sp) },
                                        leadingIcon = if (creditType == "Cash") {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )

                                    FilterChip(
                                        selected = creditType == "Credit",
                                        onClick = { creditType = "Credit" },
                                        label = { Text("Credit Party", fontSize = 10.5.sp) },
                                        leadingIcon = if (creditType == "Credit") {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (creditType == "Credit") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = creditDays,
                                            onValueChange = { creditDays = it.filter { ch -> ch.isDigit() } },
                                            label = { Text("Credit Days", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White
                                            )
                                        )
                                        OutlinedTextField(
                                            value = creditLimit,
                                            onValueChange = { creditLimit = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                            label = { Text("Credit Limit (₹)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(9.dp))

                                // Assign Agent Dropdown
                                Text(
                                    text = "Assign Sales Agent (Optional):",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))

                                ExposedDropdownMenuBox(
                                    expanded = agentExpanded,
                                    onExpandedChange = { agentExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedAgentName.ifBlank { "Unassigned" },
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = agentExpanded) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = agentExpanded,
                                        onDismissRequest = { agentExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("None (Unassigned)", fontSize = 11.sp) },
                                            onClick = {
                                                selectedAgentId = null
                                                selectedAgentName = ""
                                                agentExpanded = false
                                            }
                                        )
                                        employees.forEach { emp ->
                                            DropdownMenuItem(
                                                text = { Text("${emp.name} (${emp.role.ifBlank { "Agent" }})", fontSize = 11.sp) },
                                                onClick = {
                                                    selectedAgentId = emp.id
                                                    selectedAgentName = emp.name
                                                    agentExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (request.status.equals("APPROVED", ignoreCase = true)) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Approved Customer Record", fontWeight = FontWeight.Bold, color = Color(0xFF166534), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                if (request.createdCustomerId != null) {
                                    Text("Master Customer ID: #${request.createdCustomerId}", fontSize = 10.5.sp, color = Color(0xFF166534))
                                }
                                if (request.religion.isNotBlank()) {
                                    Text("Religion: ${request.religion}", fontSize = 10.5.sp, color = Color(0xFF166534))
                                }
                                if (request.assignedAgentName.isNotBlank()) {
                                    Text("Assigned Agent: ${request.assignedAgentName}", fontSize = 10.5.sp, color = Color(0xFF166534))
                                }
                            }
                        }
                    } else if (request.status.equals("REJECTED", ignoreCase = true)) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Registration Rejected", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), fontSize = 12.sp)
                                if (request.rejectionReason.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text("Reason: ${request.rejectionReason}", fontSize = 10.5.sp, color = Color(0xFF991B1B))
                                }
                            }
                        }
                    }
                }

                // Action Bar at Bottom (If PENDING)
                if (request.status.equals("PENDING", ignoreCase = true)) {
                    val safeBottomPadding = rememberDialogBottomPadding(extraPadding = 14.dp, fallbackNavHeight = 48.dp)

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = safeBottomPadding),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Reject", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                            }

                            Button(
                                onClick = {
                                    val days = creditDays.toIntOrNull() ?: 30
                                    val limit = creditLimit.toDoubleOrNull() ?: 0.0
                                    onApprove(
                                        selectedReligion,
                                        creditType,
                                        days,
                                        limit,
                                        selectedAgentId,
                                        selectedAgentName
                                    )
                                },
                                modifier = Modifier.weight(2f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve & Onboard", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Rejection Reason Prompt
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Registration Request", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column {
                    Text("Enter a reason for rejecting this customer request:", fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Incomplete KYC, invalid shop address", fontSize = 11.sp) },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog = false
                        onReject(rejectReason.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm Reject", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel", fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun InspectorRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = TextSecondary,
            modifier = Modifier.weight(0.85f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1.65f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
