package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.ui.components.DeliveryDaysSelector
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.dialogs.SupplierSearchBottomSheet
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
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
    onOpenMixedPack: (() -> Unit)? = null
) {
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val historyItemCodes by viewModel.distinctItemCodes.collectAsStateWithLifecycle()

    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var itemCode by remember { mutableStateOf("") }
    var piecesText by remember { mutableStateOf("") }
    var casesText by remember { mutableStateOf("") }
    var looseText by remember { mutableStateOf("") }
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
    var hasOrderFormPhoto by remember { mutableStateOf(false) }
    var hasSupplierInvoicePhoto by remember { mutableStateOf(false) }

    var paymentStatus by remember { mutableStateOf("Pending") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var paymentRemarks by remember { mutableStateOf("") }
    var mixedPackNote by remember { mutableStateOf("") }

    var showSupplierSheet by remember { mutableStateOf(false) }

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
        containerColor = Color(0xFFF6F8FB),
        bottomBar = {
            Surface(
                color = Color.White,
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
                                color = NavyPrimary
                            )
                        }
                        Button(
                            onClick = {
                                val sup = selectedSupplier ?: return@Button
                                if (isFormValid) {
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
                                        mixedPackNote = mixedPackNote.trim().takeIf { it.isNotBlank() }
                                    )
                                    onSaveSuccess()
                                }
                            },
                            enabled = isFormValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NavyPrimary,
                                disabledContainerColor = Color(0xFFCBD5E1)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 120.dp, minHeight = 38.dp)
                        ) {
                            Text(
                                text = "Save Stop",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = if (isFormValid) GoldAccent else Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
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
                            text = "Add Purchase Stop",
                            fontSize = if (isCompact) 14.5.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = "${visit.customerName} • ${visit.visitCode}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NavyPrimary.copy(alpha = 0.08f)
                                ) {
                                    Text(
                                        text = "${currentSupplier.defaultCaseSize} pcs/case",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NavyPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

                        // Row 3: Case Size & Garment GST (%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
                        ) {
                            OutlinedTextField(
                                value = caseSizeText,
                                onValueChange = { caseSizeText = it },
                                label = { Text("Case Size (Ref)", fontSize = 11.sp) },
                                placeholder = { Text("24", fontSize = 11.5.sp) },
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
                                value = gstRateText,
                                onValueChange = { gstRateText = it },
                                label = { Text("Garment GST (%)", fontSize = 11.sp) },
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

                        // Live calculation strip inside pricing card
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = if (isIncomplete) Color(0xFFFFFBEB) else Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isIncomplete) Color(0xFFFDE68A) else Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(9.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
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
                                        color = if (isIncomplete) Color(0xFFB45309) else Color(0xFF15803D)
                                    )
                                }

                                if (isIncomplete) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$loosePieces loose pieces remaining. Can be packed with another stop's loose items in Mixed Pack.",
                                            fontSize = 10.sp,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = mixedPackNote,
                            onValueChange = { mixedPackNote = it },
                            label = { Text("Packing Remarks / Packed With (Optional)", fontSize = 11.sp) },
                            placeholder = { Text("e.g. Packed with Shree Ambica 5 pcs", fontSize = 11.5.sp) },
                            textStyle = TextStyle(fontSize = 12.sp),
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
                            text = "Payment Information",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NavyPrimary
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
                                                fontWeight = if (isSelectedMode) FontWeight.Bold else FontWeight.Normal,
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = if (hasOrderFormPhoto) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (hasOrderFormPhoto) Color(0xFF86EFAC) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { hasOrderFormPhoto = !hasOrderFormPhoto }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (hasOrderFormPhoto) Icons.Default.Check else Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = if (hasOrderFormPhoto) Color(0xFF16A34A) else NavyPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (hasOrderFormPhoto) "Order Form Added" else "+ Order Form Pic",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (hasOrderFormPhoto) Color(0xFF15803D) else TextPrimary
                                    )
                                }
                            }

                            Surface(
                                color = if (hasSupplierInvoicePhoto) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (hasSupplierInvoicePhoto) Color(0xFF86EFAC) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { hasSupplierInvoicePhoto = !hasSupplierInvoicePhoto }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (hasSupplierInvoicePhoto) Icons.Default.Check else Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = if (hasSupplierInvoicePhoto) Color(0xFF16A34A) else NavyPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (hasSupplierInvoicePhoto) "Invoice Added" else "+ Wholesaler Bill",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (hasSupplierInvoicePhoto) Color(0xFF15803D) else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
            onDismiss = { showSupplierSheet = false }
        )
    }
}
