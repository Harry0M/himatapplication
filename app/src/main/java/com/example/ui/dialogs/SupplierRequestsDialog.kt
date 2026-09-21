package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.SupplierRegistrationRequestEntity
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.rememberDialogBottomPadding
import com.example.ui.viewmodel.HimatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SUPPLIER_INVITE_URL = "https://himatsms.web.app/#supplier-register"

private fun buildSupplierInviteMessage(): String {
    return "नमस्कार!\nहिम्मत टेक्सटाइल (Himat Textile) के साथ फैब्रिक मिल / सप्लायर के रूप में जुड़ने के लिए कृपया नीचे दिए गए लिंक पर अपनी मिल व व्यावसायिक जानकारी भरें:\n\n$SUPPLIER_INVITE_URL\n\nधन्यवाद!\nहिम्मत टेक्सटाइल, अहमदाबाद"
}

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

private fun shareInviteGeneral(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Himat Textile - Supplier Registration Invite")
            putExtra(Intent.EXTRA_TEXT, buildSupplierInviteMessage())
        }
        val chooser = Intent.createChooser(intent, "Share Supplier Invite via")
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(context, "Could not share invite", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierRequestsDialog(
    viewModel: HimatViewModel,
    onDismiss: () -> Unit,
    onOpenDirectForm: (() -> Unit)? = null
) {
    val requests by viewModel.supplierRegistrationRequests.collectAsStateWithLifecycle()
    val markets by viewModel.visibleMarkets.collectAsStateWithLifecycle()
    val brands by viewModel.visibleBrands.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var filterTab by remember { mutableStateOf("PENDING") } // "PENDING", "APPROVED", "REJECTED", "ALL"
    var searchQuery by remember { mutableStateOf("") }
    var selectedRequest by remember { mutableStateOf<SupplierRegistrationRequestEntity?>(null) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var fullscreenImageTitle by remember { mutableStateOf("") }
    var showInviteDialog by remember { mutableStateOf(false) }

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
                    req.contactPerson.lowercase().contains(q) ||
                    req.brand.lowercase().contains(q) ||
                    req.phone.contains(q) ||
                    req.city.lowercase().contains(q) ||
                    req.marketArea.lowercase().contains(q) ||
                    req.productsMade.lowercase().contains(q) ||
                    req.categories.lowercase().contains(q)
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
                                text = "Supplier Registration Requests",
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

                        // Invite Link Button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showInviteDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Invite",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Invite",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                        placeholder = { Text("Search by mill, brand, person, phone, city...", fontSize = 11.5.sp) },
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
                                imageVector = Icons.Default.Factory,
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
                                text = if (searchQuery.isNotBlank()) "Try changing your search terms" else "No $filterTab supplier registration requests yet",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 10.5.sp
                            )
                            if (onOpenDirectForm != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(onClick = onOpenDirectForm) {
                                    Text("Open In-App Supplier Form", fontSize = 11.sp)
                                }
                            }
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
                            SupplierRequestListItemCard(
                                request = req,
                                onClick = { selectedRequest = req },
                                onCall = { dialPhone(context, req.phone) },
                                onWhatsApp = {
                                    openWhatsApp(
                                        context,
                                        req.phone,
                                        "Hello ${req.firmName.ifBlank { req.name }}, greetings from Himat Textile Agency!"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full Request Inspection Detail Dialog
    selectedRequest?.let { req ->
        SupplierRequestInspectorDialog(
            request = req,
            markets = markets,
            brands = brands,
            onDismiss = { selectedRequest = null },
            onApprove = { brand, marketName, type ->
                viewModel.approveSupplierRegistrationRequest(
                    request = req,
                    brand = brand,
                    marketName = marketName,
                    type = type
                ) { newSuppId ->
                    Toast.makeText(context, "Approved! Supplier #$newSuppId onboarded successfully.", Toast.LENGTH_LONG).show()
                    selectedRequest = null
                }
            },
            onReject = { reason ->
                viewModel.rejectSupplierRegistrationRequest(req, reason) {
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

    // Invite Modal Dialog
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = {
                Text(
                    text = "Invite Supplier / Fabric Mill",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Share the public registration link with prospective textile mills & suppliers:",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = SUPPLIER_INVITE_URL,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NavyPrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInviteDialog = false
                        shareInviteGeneral(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Link", fontSize = 11.sp)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Supplier Registration Link", SUPPLIER_INVITE_URL))
                            Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showInviteDialog = false
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Copy Link", fontSize = 11.sp)
                    }
                    TextButton(onClick = { showInviteDialog = false }) {
                        Text("Close", fontSize = 11.sp)
                    }
                }
            }
        )
    }
}

@Composable
private fun SupplierRequestListItemCard(
    request: SupplierRegistrationRequestEntity,
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
                    if (request.contactPerson.isNotBlank() && request.contactPerson != request.firmName) {
                        Text(
                            text = "Contact: ${request.contactPerson}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = request.type,
                            color = Color(0xFF1D4ED8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(6.dp)
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
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Brand & Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (request.brand.isNotBlank()) {
                    Text(
                        text = "Brand: ${request.brand} • ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyPrimary
                    )
                }
                Text(
                    text = "${request.city} ${if (request.marketArea.isNotBlank()) "• ${request.marketArea}" else ""}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            if (request.productsMade.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Products: ${request.productsMade}",
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
private fun SupplierRequestInspectorDialog(
    request: SupplierRegistrationRequestEntity,
    markets: List<com.example.data.local.entity.MarketEntity>,
    brands: List<com.example.data.local.entity.BrandEntity>,
    onDismiss: () -> Unit,
    onApprove: (brand: String, marketName: String, type: String) -> Unit,
    onReject: (reason: String) -> Unit,
    onViewImage: (url: String, title: String) -> Unit
) {
    val context = LocalContext.current

    // Onboarding form state
    var selectedBrand by remember { mutableStateOf(request.brand.ifBlank { request.firmName }) }
    var selectedMarket by remember { mutableStateOf(request.marketArea) }
    var selectedType by remember { mutableStateOf(request.type.ifBlank { "Manufacturer" }) }

    var marketExpanded by remember { mutableStateOf(false) }

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
                                text = "Supplier Request ID: ${request.id}",
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
                    // Contact & Location Card
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Contact & Mill Details", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NavyPrimary)
                            Spacer(modifier = Modifier.height(8.dp))

                            SupplierInspectorRow("Firm Name", request.firmName)
                            SupplierInspectorRow("Contact Person", request.contactPerson.ifBlank { request.name })
                            SupplierInspectorRow("Supplier Type", request.type)
                            SupplierInspectorRow("Primary Phone", request.phone)
                            if (request.phone2.isNotBlank()) SupplierInspectorRow("Alt Phone", request.phone2)
                            if (request.email.isNotBlank()) SupplierInspectorRow("Email", request.email)
                            SupplierInspectorRow("City / State", "${request.city}, ${request.state.ifBlank { "Gujarat" }}")
                            if (request.marketArea.isNotBlank()) SupplierInspectorRow("Market Area", request.marketArea)
                            SupplierInspectorRow("Mill / Office Address", request.officeAddress.ifBlank { request.address })
                            if (request.pincode.isNotBlank()) SupplierInspectorRow("Pincode", request.pincode)

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
                                    onClick = {
                                        openWhatsApp(
                                            context,
                                            request.phone,
                                            "Hello ${request.firmName}, regarding your supplier registration with Himat Textile Agency:"
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF128C7E)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                if (request.mapLink.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.mapLink)))
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

                    // Manufacturing & Commercial Info
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Manufacturing & Commercial Info", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = NavyPrimary)
                            Spacer(modifier = Modifier.height(8.dp))

                            SupplierInspectorRow("Brand Name", request.brand.ifBlank { request.firmName })
                            if (request.productsMade.isNotBlank()) SupplierInspectorRow("Products Made", request.productsMade)
                            if (request.categories.isNotBlank()) SupplierInspectorRow("Categories", request.categories)
                            if (request.priceRange.isNotBlank()) SupplierInspectorRow("Price Range", request.priceRange)
                            SupplierInspectorRow("GSTIN", request.gstin.ifBlank { "Not Provided" })
                            SupplierInspectorRow("PAN", request.panNumber.ifBlank { "Not Provided" })
                            if (request.bankName.isNotBlank()) {
                                SupplierInspectorRow("Bank Details", "${request.bankName} • A/C: ${request.accountNumber} • IFSC: ${request.ifscCode}")
                            }
                            if (request.notes.isNotBlank()) {
                                SupplierInspectorRow("Applicant Notes", request.notes)
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
                                if (request.visitingCardPhotoUri.isNotBlank()) Pair("Visiting Card", request.visitingCardPhotoUri) else null,
                                if (request.shopPhotoUri.isNotBlank()) Pair("Mill / Shop Photo", request.shopPhotoUri) else null,
                                if (request.gstCertPhotoUri.isNotBlank()) Pair("GST Certificate", request.gstCertPhotoUri) else null,
                                if (request.panPhotoUri.isNotBlank()) Pair("PAN Card", request.panPhotoUri) else null
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

                                // Supplier Type
                                Text(
                                    text = "Supplier Type:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedType == "Manufacturer",
                                        onClick = { selectedType = "Manufacturer" },
                                        label = { Text("Manufacturer / Mill", fontSize = 10.5.sp) },
                                        leadingIcon = if (selectedType == "Manufacturer") {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )

                                    FilterChip(
                                        selected = selectedType == "Wholesaler",
                                        onClick = { selectedType = "Wholesaler" },
                                        label = { Text("Wholesaler / Trader", fontSize = 10.5.sp) },
                                        leadingIcon = if (selectedType == "Wholesaler") {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(9.dp))

                                // Brand Name Field
                                Text(
                                    text = "Brand Name:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                OutlinedTextField(
                                    value = selectedBrand,
                                    onValueChange = { selectedBrand = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("e.g. Laxmi Cottons", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(9.dp))

                                // Market Area Field / Dropdown
                                Text(
                                    text = "Market Area / Hub:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                ExposedDropdownMenuBox(
                                    expanded = marketExpanded,
                                    onExpandedChange = { marketExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedMarket,
                                        onValueChange = { selectedMarket = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        placeholder = { Text("Select or type market area", fontSize = 11.sp) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = marketExpanded) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        )
                                    )
                                    if (markets.isNotEmpty()) {
                                        ExposedDropdownMenu(
                                            expanded = marketExpanded,
                                            onDismissRequest = { marketExpanded = false }
                                        ) {
                                            markets.forEach { m ->
                                                DropdownMenuItem(
                                                    text = { Text("${m.marketName} (${m.city})", fontSize = 11.sp) },
                                                    onClick = {
                                                        selectedMarket = m.marketName
                                                        marketExpanded = false
                                                    }
                                                )
                                            }
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
                                    Text("Approved Supplier Record", fontWeight = FontWeight.Bold, color = Color(0xFF166534), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                if (request.createdSupplierId != null) {
                                    Text("Master Supplier ID: #${request.createdSupplierId}", fontSize = 10.5.sp, color = Color(0xFF166534))
                                }
                                if (request.brand.isNotBlank()) {
                                    Text("Brand: ${request.brand}", fontSize = 10.5.sp, color = Color(0xFF166534))
                                }
                                if (request.approvedBy.isNotBlank()) {
                                    Text("Approved By: ${request.approvedBy}", fontSize = 10.5.sp, color = Color(0xFF166534))
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
                                    onApprove(
                                        selectedBrand.trim(),
                                        selectedMarket.trim(),
                                        selectedType
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
            title = { Text("Reject Supplier Registration Request", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column {
                    Text("Enter a reason for rejecting this supplier request:", fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Incomplete verification, invalid GST/PAN", fontSize = 11.sp) },
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
private fun SupplierInspectorRow(label: String, value: String) {
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
