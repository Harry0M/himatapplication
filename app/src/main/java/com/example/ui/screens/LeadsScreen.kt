package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.local.entity.LeadEntity
import com.example.ui.dialogs.FullScreenImageViewerDialog
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import kotlinx.coroutines.launch
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
fun LeadsScreen(
    viewModel: HimatViewModel,
    onBack: () -> Unit
) {
    val allLeads by viewModel.allLeads.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Retailers, 2: Suppliers
    var statusFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingLead by remember { mutableStateOf<LeadEntity?>(null) }
    var convertingLead by remember { mutableStateOf<LeadEntity?>(null) }
    var deletingLead by remember { mutableStateOf<LeadEntity?>(null) }

    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var fullscreenImageTitle by remember { mutableStateOf("") }

    val customerLeadsCount = remember(allLeads) { allLeads.count { it.type == "customer" } }
    val supplierLeadsCount = remember(allLeads) { allLeads.count { it.type == "supplier" } }

    val filteredLeads = remember(allLeads, selectedTab, statusFilter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        allLeads.filter { lead ->
            val matchesType = when (selectedTab) {
                1 -> lead.type == "customer"
                2 -> lead.type == "supplier"
                else -> true
            }
            val matchesStatus = if (statusFilter == "All") true else lead.status.equals(statusFilter, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                    lead.firmName.lowercase().contains(q) ||
                    lead.name.lowercase().contains(q) ||
                    lead.phone.contains(q) ||
                    lead.meetingPlace.lowercase().contains(q) ||
                    lead.city.lowercase().contains(q) ||
                    lead.notes.lowercase().contains(q)

            matchesType && matchesStatus && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = NavyPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Leads & Prospects",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allLeads.size} contacts met in market",
                            color = GoldAccent,
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = {
                            editingLead = null
                            showAddEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("New Lead", color = NavyPrimary, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingLead = null
                    showAddEditDialog = true
                },
                containerColor = GoldAccent,
                contentColor = NavyPrimary,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Lead", modifier = Modifier.size(20.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by shop, name, phone, meeting place...", fontSize = 11.5.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
            }

            // Entity Type Tabs (All, Retailers, Suppliers)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NavyPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NavyPrimary,
                        height = 2.5.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All (${allLeads.size})", fontSize = 11.5.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Retailers ($customerLeadsCount)", fontSize = 11.5.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Suppliers ($supplierLeadsCount)", fontSize = 11.5.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val statuses = listOf("All", "Thinking", "Follow-up", "New", "Converted", "Dropped")
                statuses.forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = st },
                        label = { Text(st, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary.copy(alpha = 0.12f),
                            selectedLabelColor = NavyPrimary
                        )
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Leads List
            if (filteredLeads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(9.dp))
                        Text(
                            text = "No leads found",
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No contacts match '$searchQuery'" else "Tap '+ New Lead' to record someone you met",
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
                    items(filteredLeads, key = { it.leadId.ifBlank { it.id.toString() } }) { lead ->
                        LeadCard(
                            lead = lead,
                            onEdit = {
                                editingLead = lead
                                showAddEditDialog = true
                            },
                            onConvert = { convertingLead = lead },
                            onDelete = { deletingLead = lead },
                            onCall = { dialPhone(context, lead.phone) },
                            onWhatsApp = { openWhatsApp(context, lead.phone, "Hello ${lead.firmName.ifBlank { lead.name }}, greetings from Himat Agency!") },
                            onViewPhoto = { url ->
                                fullscreenImageUrl = url
                                fullscreenImageTitle = "${lead.firmName} Photo"
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Lead Dialog
    if (showAddEditDialog) {
        AddEditLeadDialog(
            initialLead = editingLead,
            viewModel = viewModel,
            onDismiss = { showAddEditDialog = false },
            onSaved = {
                showAddEditDialog = false
                Toast.makeText(context, "Lead saved successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Convert Lead Confirmation Dialog
    convertingLead?.let { lead ->
        AlertDialog(
            onDismissRequest = { convertingLead = null },
            title = {
                Text(
                    text = if (lead.type == "supplier") "Convert to Wholesale Supplier?" else "Convert to Retail Customer?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            text = {
                Text(
                    "This will create an official master record for '${lead.firmName.ifBlank { lead.name }}' in ${if (lead.type == "supplier") "Suppliers" else "Customers"} master and mark this lead as Converted.",
                    fontSize = 11.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = convertingLead ?: return@Button
                        convertingLead = null
                        if (target.type == "supplier") {
                            viewModel.convertLeadToSupplier(target) { newId ->
                                Toast.makeText(context, "Converted! Supplier #$newId created.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            viewModel.convertLeadToCustomer(target) { newId ->
                                Toast.makeText(context, "Converted! Customer #$newId created.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Confirm Convert", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { convertingLead = null }) {
                    Text("Cancel", fontSize = 11.sp)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deletingLead?.let { lead ->
        AlertDialog(
            onDismissRequest = { deletingLead = null },
            title = { Text("Delete Lead?", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = { Text("Are you sure you want to remove '${lead.firmName.ifBlank { lead.name }}'?", fontSize = 11.5.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLead(lead)
                        deletingLead = null
                        Toast.makeText(context, "Lead deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Delete", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingLead = null }) {
                    Text("Cancel", fontSize = 11.sp)
                }
            }
        )
    }

    // Fullscreen image viewer
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
private fun LeadCard(
    lead: LeadEntity,
    onEdit: () -> Unit,
    onConvert: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onViewPhoto: (String) -> Unit
) {
    val isCustomer = lead.type == "customer"
    val isConverted = lead.status.equals("Converted", ignoreCase = true)

    val statusColor = when (lead.status.lowercase()) {
        "thinking" -> Color(0xFFD97706)
        "follow-up" -> Color(0xFF2563EB)
        "new" -> Color(0xFF0284C7)
        "converted" -> Color(0xFF16A34A)
        "dropped" -> Color(0xFF64748B)
        else -> Color(0xFF4B5563)
    }
    val statusBg = statusColor.copy(alpha = 0.12f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            // Badges Row (Type, Subtype, Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isCustomer) NavyPrimary.copy(alpha = 0.1f) else Color(0xFF7C3AED).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = if (isCustomer) "Retailer Lead" else "Supplier Lead",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCustomer) NavyPrimary else Color(0xFF7C3AED),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (!isCustomer && lead.supplierType.isNotBlank()) {
                        Surface(
                            color = Color(0xFFEA580C).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = lead.supplierType,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEA580C),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text(
                        text = lead.status,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Firm Name & Contact Person
            Text(
                text = lead.firmName.ifBlank { lead.name },
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = TextPrimary
            )
            if (lead.firmName.isNotBlank() && lead.name.isNotBlank() && lead.firmName != lead.name) {
                Text(
                    text = "Contact: ${lead.name}",
                    fontSize = 10.5.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Where We Met Him (Crucial Field requested by user!)
            if (lead.meetingPlace.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "Meeting Place",
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Met at: ${lead.meetingPlace}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9A3412)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
            }

            // City / State
            Text(
                text = "${lead.city}, ${lead.state}",
                fontSize = 10.sp,
                color = TextSecondary
            )

            // Meeting Notes
            if (lead.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lead.notes,
                        fontSize = 10.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(6.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Photos Preview Strip (visiting cards, shop front, etc.)
            val photos = lead.photoList
            if (photos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    photos.forEachIndexed { idx, url ->
                        Box(
                            modifier = Modifier
                                .size(56.dp, 44.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(5.dp))
                                .clickable { onViewPhoto(url) }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Photo $idx",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            // Action Row: Calls, WhatsApp, Convert, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call & WhatsApp icons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
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
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = NavyPrimary, modifier = Modifier.size(13.dp))
                        }
                    }
                }

                // Convert & Edit buttons
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isConverted) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Converted", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            }
                        }
                    } else {
                        Button(
                            onClick = onConvert,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(5.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (isCustomer) "Convert" else "Convert", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary, modifier = Modifier.size(15.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditLeadDialog(
    initialLead: LeadEntity?,
    viewModel: HimatViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var leadType by remember { mutableStateOf(initialLead?.type ?: "customer") } // "customer" or "supplier"
    var supplierType by remember { mutableStateOf(initialLead?.supplierType?.ifBlank { "Manufacturer" } ?: "Manufacturer") }
    var firmName by remember { mutableStateOf(initialLead?.firmName ?: "") }
    var name by remember { mutableStateOf(initialLead?.name ?: "") }
    var phone by remember { mutableStateOf(initialLead?.phone ?: "") }
    var phone2 by remember { mutableStateOf(initialLead?.phone2 ?: "") }
    var meetingPlace by remember { mutableStateOf(initialLead?.meetingPlace ?: "") }
    var city by remember { mutableStateOf(initialLead?.city?.ifBlank { "Ahmedabad" } ?: "Ahmedabad") }
    var state by remember { mutableStateOf(initialLead?.state?.ifBlank { "Gujarat" } ?: "Gujarat") }
    var status by remember { mutableStateOf(initialLead?.status?.ifBlank { "Thinking" } ?: "Thinking") }
    var notes by remember { mutableStateOf(initialLead?.notes ?: "") }

    val photosList = remember { mutableStateListOf<String>().apply { addAll(initialLead?.photoList ?: emptyList()) } }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isUploadingPhoto = true
            coroutineScope.launch {
                for (uri in uris) {
                    viewModel.uploadFileToStorage(
                        context = context,
                        fileUri = uri,
                        folder = "leads",
                        prefix = "lead_doc",
                        onSuccess = { downloadUrl ->
                            photosList.add(downloadUrl)
                        },
                        onError = { err ->
                            Toast.makeText(context, "Upload failed: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                isUploadingPhoto = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
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
                        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (initialLead == null) "New Lead / Prospect" else "Edit Lead",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }
                }

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    // Lead Type Segmented (Customer vs Supplier)
                    Text("Lead Type:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = leadType == "customer",
                            onClick = { leadType = "customer" },
                            label = { Text("Retailer (Customer)", fontSize = 10.5.sp) },
                            leadingIcon = if (leadType == "customer") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = leadType == "supplier",
                            onClick = { leadType = "supplier" },
                            label = { Text("Supplier Lead", fontSize = 10.5.sp) },
                            leadingIcon = if (leadType == "supplier") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // If Supplier: Manufacturer vs Wholesaler
                    if (leadType == "supplier") {
                        Text("Supplier Category:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = supplierType == "Manufacturer",
                                onClick = { supplierType = "Manufacturer" },
                                label = { Text("Manufacturer", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = supplierType == "Wholesaler",
                                onClick = { supplierType = "Wholesaler" },
                                label = { Text("Wholesaler", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Firm Name / Shop Name
                    OutlinedTextField(
                        value = firmName,
                        onValueChange = { firmName = it },
                        label = { Text("Firm Name / Shop Name *", fontSize = 10.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Contact Person Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Contact Person Name", fontSize = 10.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Primary Phone & Phone 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' } },
                            label = { Text("Phone Number *", fontSize = 10.5.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = phone2,
                            onValueChange = { phone2 = it.filter { ch -> ch.isDigit() || ch == '+' } },
                            label = { Text("Alt Phone", fontSize = 10.5.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Meeting Place / Where Met (Requested by user!)
                    OutlinedTextField(
                        value = meetingPlace,
                        onValueChange = { meetingPlace = it },
                        label = { Text("Place Where We Met Him *", fontSize = 10.5.sp) },
                        placeholder = { Text("e.g. Surat Textile Market, Kalupur Shop...", fontSize = 10.5.sp) },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(15.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // City & State
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City", fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State", fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Status selection
                    Text("Lead Status:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statuses = listOf("Thinking", "Follow-up", "New", "Converted", "Dropped")
                        statuses.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st, fontSize = 10.5.sp) }
                            )
                        }
                    }

                    // Notes / Remarks
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Discussion Notes & Requirements", fontSize = 10.5.sp) },
                        placeholder = { Text("What did you discuss? Price expectations, item samples needed, etc.", fontSize = 10.5.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Photos (Visiting Card, Shop Front, Sample Photos)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Photos (Visiting Card / Shop / Samples):", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Add Photo", fontSize = 10.sp)
                            }
                        }

                        if (isUploadingPhoto) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Uploading photo to cloud...", fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        if (photosList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                photosList.forEachIndexed { idx, url ->
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp, 52.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Photo $idx",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.6f),
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                                .size(17.dp)
                                                .clickable { photosList.removeAt(idx) }
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.padding(2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Save Action Bar at bottom
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Cancel", fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = {
                                if (firmName.isBlank() && name.isBlank()) {
                                    Toast.makeText(context, "Please enter firm name or contact name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (phone.isBlank()) {
                                    Toast.makeText(context, "Please enter phone number", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val photosJsonStr = if (photosList.isEmpty()) "[]"
                                else "[" + photosList.joinToString(",") { "\"$it\"" } + "]"

                                val leadToSave = (initialLead ?: LeadEntity()).copy(
                                    type = leadType,
                                    supplierType = if (leadType == "supplier") supplierType else "",
                                    firmName = firmName.trim(),
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    phone2 = phone2.trim(),
                                    meetingPlace = meetingPlace.trim(),
                                    city = city.trim(),
                                    state = state.trim(),
                                    status = status,
                                    notes = notes.trim(),
                                    photosJson = photosJsonStr
                                )

                                viewModel.saveLead(leadToSave) {
                                    onSaved()
                                }
                            },
                            modifier = Modifier.weight(2f),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Save Lead", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
}
