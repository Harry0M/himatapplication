package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.dialogs.FullScreenImageViewerDialog
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator
import com.example.util.ShareUtil
import com.example.util.rememberDialogBottomPadding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
fun PurchaseOrdersScreen(
    viewModel: HimatViewModel,
    onBack: () -> Unit,
    onOpenOrder: (PurchaseEntryEntity) -> Unit = {},
    onOpenVisit: (VisitEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    val visits by viewModel.allVisits.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()

    val visitsMap = remember(visits) { visits.associateBy { it.id } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedDateRange by remember { mutableStateOf("All Time") } // "All Time", "Today", "Yesterday", "This Week", "This Month"
    var selectedSupplierName by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf("All") } // "All", "Pending", "Delivered"
    var isSupplierDropdownExpanded by remember { mutableStateOf(false) }

    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 20

    var isGeneratingPdf by remember { mutableStateOf(false) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var fullscreenImageTitle by remember { mutableStateOf("") }

    // Filter Logic
    val filteredEntries = remember(
        entries, searchQuery, selectedDateRange, selectedSupplierName, selectedStatus, visitsMap
    ) {
        val now = Calendar.getInstance()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayStart = todayStart - 86400000L
        val weekStart = todayStart - (6 * 86400000L)
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        entries.filter { entry ->
            if (entry.isDeleted) return@filter false

            // Date Range Filter
            val entryTime = entry.createdAt
            val dateMatches = when (selectedDateRange) {
                "Today" -> entryTime >= todayStart
                "Yesterday" -> entryTime in yesterdayStart until todayStart
                "This Week" -> entryTime >= weekStart
                "This Month" -> entryTime >= monthStart
                else -> true
            }
            if (!dateMatches) return@filter false

            // Supplier Filter
            if (selectedSupplierName != null && entry.supplierName != selectedSupplierName) {
                return@filter false
            }

            // Status Filter
            if (selectedStatus != "All" && !entry.deliveryStatus.equals(selectedStatus, ignoreCase = true)) {
                return@filter false
            }

            // Search Query Filter
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val orderMatches = entry.orderNo.lowercase().contains(q) || "po-${entry.id}".contains(q)
                val supMatches = entry.supplierName.lowercase().contains(q)
                val itemMatches = entry.itemCode.lowercase().contains(q)
                val visit = visitsMap[entry.visitId]
                val custMatches = visit?.customerName?.lowercase()?.contains(q) ?: false
                if (!orderMatches && !supMatches && !itemMatches && !custMatches) {
                    return@filter false
                }
            }

            true
        }.sortedByDescending { it.createdAt }
    }

    // Pagination calculations
    val totalRecords = filteredEntries.size
    val totalPages = maxOf(1, ceil(totalRecords / pageSize.toDouble()).toInt())

    // Adjust page if out of bounds
    val safePage = currentPage.coerceIn(1, totalPages)
    val startIndex = (safePage - 1) * pageSize
    val pagedEntries = remember(filteredEntries, safePage) {
        filteredEntries.drop(startIndex).take(pageSize)
    }

    // Metrics for filtered set
    val totalPieces = remember(filteredEntries) { filteredEntries.sumOf { it.pieces } }
    val totalCases = remember(filteredEntries) { filteredEntries.sumOf { it.caseCount } }
    val totalAmount = remember(filteredEntries) { filteredEntries.sumOf { it.grandTotalWithGst } }
    val totalGst = remember(filteredEntries) { filteredEntries.sumOf { it.gstAmount } }

    val safeBottomPadding = rememberDialogBottomPadding(extraPadding = 8.dp, fallbackNavHeight = 48.dp)

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
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Purchase Orders & Invoices",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalRecords total orders • $totalPieces pcs of all time",
                            color = GoldAccent,
                            fontSize = 11.sp
                        )
                    }

                    // Bulk PDF Download / Share Action
                    Button(
                        onClick = {
                            if (filteredEntries.isEmpty()) {
                                Toast.makeText(context, "No purchase orders to export", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isGeneratingPdf = true
                            coroutineScope.launch {
                                try {
                                    val pdfFile = PdfGenerator.generatePurchaseOrdersBulkPdf(
                                        context = context,
                                        entries = filteredEntries,
                                        dateFilterLabel = selectedDateRange,
                                        supplierFilterLabel = selectedSupplierName ?: "All Suppliers",
                                        statusFilterLabel = if (selectedStatus == "All") "All Status" else selectedStatus
                                    )
                                    ShareUtil.sharePdfFile(context, pdfFile, "Purchase Orders & Supplier Invoices Statement")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "PDF Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isGeneratingPdf = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2410C)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        enabled = !isGeneratingPdf
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bulk PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Pagination Bar at Bottom
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = safeBottomPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (currentPage > 1) currentPage-- },
                        enabled = safePage > 1,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Previous", fontSize = 12.sp)
                    }

                    Text(
                        text = "Page $safePage of $totalPages ($totalRecords total)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedButton(
                        onClick = { if (currentPage < totalPages) currentPage++ },
                        enabled = safePage < totalPages,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Next", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // KPI Summary Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Billed", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = PdfGenerator.formatInr(totalAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFC2410C)
                    )
                }
                Column {
                    Text("Total Pieces", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = "$totalPieces pcs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NavyPrimary
                    )
                }
                Column {
                    Text("Total Cases", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = "$totalCases cases",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF047857)
                    )
                }
                Column {
                    Text("GST Included", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = PdfGenerator.formatInr(totalGst),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Search Bar & Filter Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        currentPage = 1
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by Order #, Supplier, Item code, or Retailer...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = ""; currentPage = 1 }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date Filter Chips
                    listOf("All Time", "Today", "Yesterday", "This Week", "This Month").forEach { range ->
                        FilterChip(
                            selected = selectedDateRange == range,
                            onClick = {
                                selectedDateRange = range
                                currentPage = 1
                            },
                            label = { Text(range, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    // Supplier Filter Dropdown Chip
                    Box {
                        FilterChip(
                            selected = selectedSupplierName != null,
                            onClick = { isSupplierDropdownExpanded = true },
                            label = {
                                Text(
                                    text = selectedSupplierName ?: "All Suppliers",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                if (selectedSupplierName != null) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                selectedSupplierName = null
                                                currentPage = 1
                                            }
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0D9488),
                                selectedLabelColor = Color.White
                            )
                        )

                        DropdownMenu(
                            expanded = isSupplierDropdownExpanded,
                            onDismissRequest = { isSupplierDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Suppliers (Show All)", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedSupplierName = null
                                    currentPage = 1
                                    isSupplierDropdownExpanded = false
                                }
                            )
                            suppliers.map { it.firmName.ifBlank { it.name } }.distinct().sorted().forEach { supName ->
                                DropdownMenuItem(
                                    text = { Text(supName) },
                                    onClick = {
                                        selectedSupplierName = supName
                                        currentPage = 1
                                        isSupplierDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Status Filter Chips
                    listOf("All", "Pending", "Delivered").forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = {
                                selectedStatus = st
                                currentPage = 1
                            },
                            label = { Text("Status: $st", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFC2410C),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Order List
            if (pagedEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No purchase orders found",
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedDateRange != "All Time" || selectedSupplierName != null) {
                                "Try resetting filters or changing your search terms."
                            } else {
                                "Purchase orders recorded during market visits will appear here."
                            },
                            fontSize = 11.sp,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pagedEntries, key = { it.id }) { entry ->
                        val visit = visitsMap[entry.visitId]
                        val formattedDate = remember(entry.createdAt) {
                            if (entry.createdAt > 0L) {
                                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.createdAt))
                            } else {
                                entry.expectedDeliveryDate.ifBlank { "Date not recorded" }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenOrder(entry) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Top Row: Order # & Badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFFC2410C).copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(5.dp)
                                        ) {
                                            Text(
                                                text = entry.orderNo.ifBlank { "PO-${entry.id}" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFFC2410C),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }

                                        if (entry.supplierType.isNotBlank()) {
                                            SupplierTypeBadge(entry.supplierType)
                                        }
                                    }

                                    StatusBadge(status = entry.deliveryStatus)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Supplier Mill & Retailer Link
                                Text(
                                    text = entry.supplierName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )

                                if (visit != null) {
                                    Text(
                                        text = "Trip: ${visit.customerName} (${visit.visitCode})",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Commercial / Goods Breakdown
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Item: ${entry.itemCode.ifBlank { "Standard Apparel" }}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${entry.pieces} pcs • ${entry.caseCount} cases (${entry.loosePieces} loose)",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = PdfGenerator.formatInr(entry.grandTotalWithGst),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFF047857)
                                        )
                                        Text(
                                            text = "@ ₹${entry.rate}/pc + GST",
                                            fontSize = 10.5.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Footer: Date & Invoice Photo Attachment
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formattedDate,
                                        fontSize = 10.sp,
                                        color = TextSecondary.copy(alpha = 0.8f)
                                    )

                                    // Invoice / Order Photo Button
                                    val invoiceUri = entry.supplierInvoiceUri ?: entry.orderFormPhotoUri
                                    if (!invoiceUri.isNullOrBlank()) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF2563EB).copy(alpha = 0.1f))
                                                .clickable {
                                                    fullscreenImageUrl = invoiceUri
                                                    fullscreenImageTitle = "Invoice for ${entry.orderNo} (${entry.supplierName})"
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Image,
                                                contentDescription = "View Invoice",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "View Invoice",
                                                fontSize = 10.sp,
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
        }
    }

    // Fullscreen Image Dialog Viewer
    fullscreenImageUrl?.let { url ->
        FullScreenImageViewerDialog(
            imageUrl = url,
            title = fullscreenImageTitle,
            onDismiss = { fullscreenImageUrl = null }
        )
    }
}
