package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.material3.FilterChip
import com.example.data.local.entity.EmployeeEntity
import com.example.util.hasMember
import com.example.util.isSubAgent
import com.example.util.tripMembers
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.data.remote.FirebaseStorageService
import com.example.ui.components.DeliveryDaysSelector
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.dialogs.EmployeeSearchBottomSheet
import com.example.ui.dialogs.FullScreenImageViewerDialog
import com.example.ui.dialogs.QuickAddSupplierDialog
import com.example.ui.dialogs.SupplierSearchBottomSheet
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.QueueButtons
import com.example.util.SupplierQueue
import com.example.util.brandName
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddStopScreen(
    viewModel: HimatViewModel,
    visit: VisitEntity,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    onOpenMixedPack: (() -> Unit)? = null,
    /** Set for a phone order: this form runs once per supplier the customer named. */
    queue: SupplierQueue? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageService = remember { FirebaseStorageService() }

    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val historyItemCodes by viewModel.distinctItemCodes.collectAsStateWithLifecycle()

    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }

    // Phone order: the supplier for this step is already chosen, the salesman can still change it
    LaunchedEffect(queue?.currentSupplierId, suppliers) {
        val wanted = queue?.currentSupplierId ?: return@LaunchedEffect
        val match = suppliers.firstOrNull { it.id == wanted }
        if (match != null) {
            selectedSupplier = match
        } else if (suppliers.isNotEmpty()) {
            Toast.makeText(context, "Supplier not found, skipped", Toast.LENGTH_SHORT).show()
            viewModel.queueSkip()
        }
    }

    var itemCode by remember { mutableStateOf("") }
    var piecesText by remember { mutableStateOf("") }
    var casesText by remember { mutableStateOf("") }
    var looseText by remember { mutableStateOf("") }
    var packingRemarks by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var caseSizeText by remember { mutableStateOf("24") }
    var gstRateText by remember { mutableStateOf("5.0") }

    val defaultDate = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 7)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    var expectedDeliveryDate by remember { mutableStateOf(defaultDate) }
    var transporter by remember { mutableStateOf("") }

    var orderFormPhotoUri by remember { mutableStateOf<String?>(null) }
    var isUploadingOrderForm by remember { mutableStateOf(false) }

    var supplierInvoiceUri by remember { mutableStateOf<String?>(null) }
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
                    Toast.makeText(context, "Saved locally (offline): ${err.localizedMessage ?: "Upload issue"}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "Saved locally (offline): ${err.localizedMessage ?: "Upload issue"}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var paymentStatus by remember { mutableStateOf("Pending") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var paymentRemarks by remember { mutableStateOf("") }

    var showSupplierSheet by remember { mutableStateOf(false) }
    var showQuickAddSupplier by remember { mutableStateOf(false) }

    val allEmployees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val tripMembers = remember(visit, allEmployees) { visit.tripMembers(allEmployees) }
    // The salesman on this trip who is entering the order from this phone is its salesman by default;
    // anybody else (e.g. an admin adding without joining) starts with the trip's salesman. Both can pick
    // someone else below. Keyed on the trip id only, so a live trip update never resets the choice mid-entry.
    var selectedSalesman by remember(visit.id) {
        val me = currentEmployee?.takeIf { !it.isSubAgent() && it.id > 0L && visit.hasMember(it) }
        val initialEmp = me
            ?: allEmployees.find { it.id == visit.employeeId }
            ?: EmployeeEntity(id = visit.employeeId, name = visit.employeeName)
        mutableStateOf(initialEmp)
    }
    var showSalesmanSheet by remember { mutableStateOf(false) }

    // Live Calculations
    val pieces = piecesText.toIntOrNull() ?: 0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val caseSize = caseSizeText.toIntOrNull() ?: 24
    val enteredCases = casesText.toIntOrNull()
    val enteredLoose = looseText.toIntOrNull()

    // Respect user's explicit Cases and Loose entries directly without forced math logic
    val caseCount = enteredCases ?: (if (caseSize > 0) pieces / caseSize else 0)
    val loosePieces = enteredLoose ?: (if (caseSize > 0) pieces % caseSize else 0)
    val baseAmount = pieces * rate
    val isIncomplete = loosePieces > 0

    val isFormValid = selectedSupplier != null && itemCode.isNotBlank() && pieces > 0 && rate > 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Estimated Total",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "₹${String.format("%,.2f", baseAmount)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val saveOrder: () -> Unit = {
                            val sup = selectedSupplier
                            if (sup != null && isFormValid) {
                                viewModel.savePurchaseEntry(
                                    orderNo = null,
                                    visitId = visit.id,
                                    supplier = sup,
                                    itemCode = itemCode.trim().uppercase(Locale.getDefault()),
                                    pieces = pieces,
                                    rate = rate,
                                    caseSize = caseSize,
                                    caseCount = caseCount,
                                    loosePieces = loosePieces,
                                    gstRate = gstRateText.toDoubleOrNull() ?: 5.0,
                                    expectedDeliveryDate = expectedDeliveryDate,
                                    transporter = transporter,
                                    paymentStatus = paymentStatus,
                                    paymentMode = paymentMode,
                                    paidAmount = if (paymentStatus == "Received") (pieces * rate) + ((pieces * rate * (gstRateText.toDoubleOrNull() ?: 5.0)) / 100.0) else 0.0,
                                    paymentRemarks = paymentRemarks,
                                    mixedPackNote = packingRemarks.trim().ifEmpty { null },
                                    orderFormPhotoUri = orderFormPhotoUri,
                                    supplierInvoiceUri = supplierInvoiceUri,
                                    salesmanId = selectedSalesman.id,
                                    salesmanName = selectedSalesman.name,
                                    orderDate = visit.date
                                )
                            }
                        }

                        if (queue != null) {
                            // Phone order: one supplier at a time, so the buttons move the queue on
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.queueSkip() },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text("Skip", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                }
                                Button(
                                    onClick = {
                                        saveOrder()
                                        if (queue.buttons == QueueButtons.FINISH_AND_SKIP) {
                                            viewModel.queueSaveAndFinish()
                                        } else {
                                            viewModel.queueSaveAndNext()
                                        }
                                    },
                                    enabled = isFormValid,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text(
                                        text = if (queue.buttons == QueueButtons.FINISH_AND_SKIP) "Save & finish" else "Save & next",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    saveOrder()
                                    if (selectedSupplier != null && isFormValid) onSaveSuccess()
                                },
                                enabled = isFormValid,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 120.dp, minHeight = 44.dp)
                            ) {
                                Text(
                                    text = "Save order",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isFormValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            val isCompact = maxWidth < 380.dp
            val horizontalPadding = if (isCompact) 10.dp else 12.dp
            val cardSpacing = if (isCompact) 6.dp else 8.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(cardSpacing)
            ) {
                // Top Header (Flat, borderless, matching VisitDetailScreen)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NavyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New order",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "${visit.customerName} • ${visit.visitCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Phone order: which supplier of the list this is
                if (queue != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                text = "Phone order • supplier ${queue.progressLabel}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = selectedSupplier?.brandName() ?: "Loading supplier...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Accompanying Salesman Card (Since multiple agents can join a trip)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Salesman for this order *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Change / Search",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { showSalesmanSheet = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // One chip per salesman on this trip (started, joined from their phone, or added)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tripMembers.forEach { member ->
                                val isSel = selectedSalesman.id == member.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        selectedSalesman = allEmployees.find { it.id == member.id }
                                            ?: EmployeeEntity(id = member.id, name = member.name)
                                    },
                                    label = { Text(member.name, fontSize = 12.sp) }
                                )
                            }
                            // Someone picked through "Change / Search" who is not on the trip yet
                            if (tripMembers.none { it.id == selectedSalesman.id } && selectedSalesman.name.isNotBlank()) {
                                FilterChip(
                                    selected = true,
                                    onClick = { showSalesmanSheet = true },
                                    label = { Text(selectedSalesman.name, fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                )
                            }
                        }

                        Text(
                            text = "Order will be logged under: ${selectedSalesman.name}",
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Card 1: Supplier / Wholesaler Stop
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Supplier / Wholesaler *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NavyPrimary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+ Create New",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.clickable { showQuickAddSupplier = true }
                                )
                                if (selectedSupplier != null) {
                                    Text(
                                        text = "Change",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary,
                                        modifier = Modifier.clickable { showSupplierSheet = true }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val currentSupplier = selectedSupplier
                        if (currentSupplier == null) {
                            // Empty State Picker Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                    .clickable { showSupplierSheet = true }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tap to choose shop / manufacturer...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = NavyPrimary
                                    )
                                }
                            }
                        } else {
                            // Selected Supplier Details
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                    .clickable { showSupplierSheet = true }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = NavyPrimary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = currentSupplier.name.take(2).uppercase(Locale.getDefault()),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = NavyPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = currentSupplier.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        SupplierTypeBadge(type = currentSupplier.type)
                                    }
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "${currentSupplier.marketArea}${if (currentSupplier.brand.isNotBlank()) " • ${currentSupplier.brand}" else ""}",
                                        fontSize = 10.5.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: Article & Item Code Details
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp)) {
                        Text(
                            text = "Item & Style Code *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NavyPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = itemCode,
                            onValueChange = { itemCode = it.uppercase(Locale.getDefault()) },
                            placeholder = { Text("e.g. KURTI-102, JEANS-88, ABC", fontSize = 11.5.sp) },
                            textStyle = TextStyle(fontSize = 12.5.sp),
                            trailingIcon = {
                                if (itemCode.isNotEmpty()) {
                                    IconButton(onClick = { itemCode = "" }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_stop_item_code_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        // History suggestions
                        if (historyItemCodes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Recent Items:", fontSize = 10.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(3.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                historyItemCodes.take(6).forEach { code ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { itemCode = code }
                                    ) {
                                        Text(
                                            text = code,
                                            fontSize = 10.sp,
                                            color = NavyPrimary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Card 3: Quantity, Pricing & Packing
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp)) {
                        Text(
                            text = "Quantity & Pricing *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NavyPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
                        ) {
                            OutlinedTextField(
                                value = piecesText,
                                onValueChange = { piecesText = it },
                                label = { Text("Total Pieces (Pc) *", fontSize = 11.sp) },
                                placeholder = { Text("75", fontSize = 11.5.sp) },
                                textStyle = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
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
                                label = { Text("Rate (₹/Pc) *", fontSize = 11.sp) },
                                placeholder = { Text("450", fontSize = 11.5.sp) },
                                textStyle = TextStyle(fontSize = 12.5.sp),
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

                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 2: Cases & Loose Pieces (freely enterable per user specification)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
                        ) {
                            OutlinedTextField(
                                value = casesText,
                                onValueChange = { casesText = it },
                                label = { Text("Cases (Cs)", fontSize = 11.sp) },
                                placeholder = { Text("2", fontSize = 11.5.sp) },
                                textStyle = TextStyle(fontSize = 12.5.sp),
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
                                label = { Text("Loose Pieces (Pcs)", fontSize = 11.sp) },
                                placeholder = { Text("5", fontSize = 11.5.sp) },
                                textStyle = TextStyle(fontSize = 12.5.sp),
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

                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 3: Garment GST (%)
                        OutlinedTextField(
                            value = gstRateText,
                            onValueChange = { gstRateText = it },
                            label = { Text("Garment GST (%)", fontSize = 11.sp) },
                            textStyle = TextStyle(fontSize = 12.5.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 4: Packing Remarks / Note (e.g. Packed with another bill/wholesaler)
                        OutlinedTextField(
                            value = packingRemarks,
                            onValueChange = { packingRemarks = it },
                            label = { Text("Packing Remarks / Note (Optional)", fontSize = 11.sp) },
                            placeholder = { Text("e.g. Packed with Order HT-1002 / Wholesaler X", fontSize = 11.5.sp) },
                            textStyle = TextStyle(fontSize = 12.5.sp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        // Live calculation strip inside pricing card
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(9.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Base Amount: ₹${String.format("%,.2f", baseAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (caseSize > 0) "$caseCount Cases" + if (loosePieces > 0) " + $loosePieces Loose" else " (Full)" else "$pieces pcs",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = if (loosePieces > 0) Color(0xFFD97706) else Color(0xFF15803D)
                                    )
                                }
                                if (packingRemarks.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "↳ Note: $packingRemarks",
                                        fontSize = 10.5.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 4: Logistics & Delivery
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Delivery & Logistics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NavyPrimary
                        )

                        // Day-based Delivery Date Selector
                        DeliveryDaysSelector(
                            expectedDeliveryDate = expectedDeliveryDate,
                            onDeliveryDateChange = { expectedDeliveryDate = it }
                        )

                        OutlinedTextField(
                            value = transporter,
                            onValueChange = { transporter = it },
                            label = { Text("Transporter / LR", fontSize = 11.sp) },
                            placeholder = { Text("e.g. VRL, Jaipur Golden", fontSize = 11.5.sp) },
                            textStyle = TextStyle(fontSize = 12.5.sp),
                            leadingIcon = {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }

                // Card 5: Payment Details
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp)) {
                        Text(
                            text = "Payment (optional)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Not required. An order is complete once its delivery status is Delivered.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Payment Status", fontSize = 10.5.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Pending", "Received").forEach { status ->
                                val selected = paymentStatus.equals(status, ignoreCase = true)
                                Surface(
                                    color = if (selected) (if (status == "Received") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (selected) (if (status == "Received") Color(0xFF86EFAC) else Color(0xFFFDE68A)) else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { paymentStatus = status }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (status == "Received") "✓ Payment Received" else "⏳ Payment Pending",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (selected) (if (status == "Received") Color(0xFF15803D) else Color(0xFFB45309)) else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        if (paymentStatus == "Received") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Payment Mode", fontSize = 10.5.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Cash", "Online", "Cheque").forEach { mode ->
                                    val isSelectedMode = paymentMode.equals(mode, ignoreCase = true)
                                    Surface(
                                        color = if (isSelectedMode) NavyPrimary else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (isSelectedMode) NavyPrimary else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { paymentMode = mode }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (mode) {
                                                    "Cash" -> "💵 Cash"
                                                    "Online" -> "📱 Online"
                                                    else -> "🏦 Cheque"
                                                },
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 10.5.sp,
                                                color = if (isSelectedMode) Color.White else TextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = paymentRemarks,
                                onValueChange = { paymentRemarks = it },
                                label = { Text("Payment Note / Ref (Optional)", fontSize = 11.sp) },
                                placeholder = { Text("e.g. UPI Ref # or Cheque #", fontSize = 11.5.sp) },
                                textStyle = TextStyle(fontSize = 12.5.sp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NavyPrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )
                        }
                    }
                }

                // Card 6: Spot Proofs & Attachments
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (isCompact) 10.dp else 12.dp)) {
                        Text(
                            text = "Spot Proofs & Attachments",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NavyPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

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
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSalesmanSheet) {
        EmployeeSearchBottomSheet(
            employees = allEmployees,
            selectedEmployee = selectedSalesman,
            onSelectEmployee = { emp ->
                selectedSalesman = emp
                showSalesmanSheet = false
            },
            onDismiss = { showSalesmanSheet = false }
        )
    }

    if (showSupplierSheet) {
        SupplierSearchBottomSheet(
            suppliers = suppliers,
            selectedSupplier = selectedSupplier,
            onSelectSupplier = { sup ->
                selectedSupplier = sup
                caseSizeText = sup.defaultCaseSize.toString()
                showSupplierSheet = false
            },
            onDismiss = { showSupplierSheet = false },
            onQuickCreateSupplier = { newSup ->
                viewModel.saveSupplier(
                    supplier = newSup,
                    onSaved = { savedSup ->
                        selectedSupplier = savedSup
                        caseSizeText = savedSup.defaultCaseSize.toString()
                    }
                )
                selectedSupplier = newSup
                caseSizeText = newSup.defaultCaseSize.toString()
                showSupplierSheet = false
            }
        )
    }

    if (showQuickAddSupplier) {
        QuickAddSupplierDialog(
            initialName = "",
            onDismiss = { showQuickAddSupplier = false },
            onSave = { newSup ->
                viewModel.saveSupplier(
                    supplier = newSup,
                    onSaved = { savedSup ->
                        selectedSupplier = savedSup
                        caseSizeText = savedSup.defaultCaseSize.toString()
                    }
                )
                selectedSupplier = newSup
                caseSizeText = newSup.defaultCaseSize.toString()
                showQuickAddSupplier = false
            }
        )
    }

    previewImageUrl?.let { url ->
        FullScreenImageViewerDialog(
            imageUrl = url,
            title = previewImageTitle,
            onDismiss = { previewImageUrl = null }
        )
    }
}

@Composable
fun StopDocumentAttachmentCard(
    title: String,
    subtitle: String,
    uriString: String?,
    isUploading: Boolean,
    onPickImage: () -> Unit,
    onViewImage: () -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPhoto = !uriString.isNullOrBlank()
    Surface(
        color = if (hasPhoto) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (hasPhoto) Color(0xFF86EFAC) else Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !isUploading && !hasPhoto) { onPickImage() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isUploading) {
                    Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = NavyPrimary
                        )
                    }
                } else if (hasPhoto) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF16A34A), RoundedCornerShape(6.dp))
                            .clickable { onViewImage() }
                    ) {
                        AsyncImage(
                            model = uriString,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasPhoto) Color(0xFF15803D) else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isUploading -> "Uploading to Firebase..."
                            hasPhoto -> "Attached ✓ (Tap thumbnail to view)"
                            else -> subtitle
                        },
                        fontSize = 10.sp,
                        fontWeight = if (hasPhoto) FontWeight.Medium else FontWeight.Normal,
                        color = when {
                            isUploading -> Color(0xFFD97706)
                            hasPhoto -> Color(0xFF16A34A)
                            else -> TextSecondary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (!isUploading) {
                if (hasPhoto) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onViewImage,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "View",
                                tint = NavyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onRemoveImage,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onPickImage,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 28.dp)
                    ) {
                        Text(
                            text = "+ Add Pic",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }
        }
    }
}
