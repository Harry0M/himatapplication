package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator
import com.example.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: HimatViewModel,
    entry: PurchaseEntryEntity,
    onBack: () -> Unit,
    onOpenVisit: ((VisitEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val allSuppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val allEmployees by viewModel.allEmployees.collectAsStateWithLifecycle()

    val visit = allVisits.find { it.id == entry.visitId }
    val customer = allCustomers.find { it.id == visit?.customerId }
    val supplier = allSuppliers.find {
        it.id == entry.supplierId ||
        (it.name.isNotBlank() && it.name.trim().equals(entry.supplierName.trim(), ignoreCase = true)) ||
        (it.firmName.isNotBlank() && it.firmName.trim().equals(entry.supplierName.trim(), ignoreCase = true))
    } ?: SupplierEntity(
        id = entry.supplierId,
        name = entry.supplierName,
        firmName = entry.supplierName,
        type = entry.supplierType
    )
    val salesman = allEmployees.find { it.id == visit?.employeeId }

    val customerDisplayName = customer?.firmName?.ifBlank { customer.name }
        ?: visit?.customerName?.takeIf { it.isNotBlank() }
        ?: "Direct Customer"

    val customerPhone = customer?.phone?.takeIf { it.isNotBlank() } ?: ""
    val supplierPhone = supplier.phone.takeIf { it.isNotBlank() } ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Order #${entry.orderNo}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            DeliveryStatusBadge(status = entry.deliveryStatus)
                        }
                        Text(
                            text = "Customer: $customerDisplayName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (customerPhone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$customerPhone"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call Customer",
                                tint = Color(0xFF2563EB)
                            )
                        }
                    }
                    if (supplierPhone.isNotBlank() && supplierPhone != customerPhone) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supplierPhone"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call Supplier",
                                tint = Color(0xFF059669)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF6F8FB)
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // WhatsApp Share
                    Button(
                        onClick = { viewModel.shareOrderWhatsApp(entry) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }

                    // PDF Bill
                    Button(
                        onClick = { viewModel.shareOrderPdf(entry) },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF Bill", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }

                    // Copy text details
                    OutlinedButton(
                        onClick = {
                            val dummyVisit = visit ?: VisitEntity(
                                id = entry.visitId,
                                date = entry.expectedDeliveryDate,
                                visitCode = entry.orderNo
                            )
                            val text = ShareUtil.buildSupplierCopyText(dummyVisit, supplier, customer, listOf(entry))
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Order Details", text))
                            Toast.makeText(context, "Order details copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(0.6f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NavyPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cardless Hero Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(2.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Order #${entry.orderNo}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    val dateSubtitle = visit?.date?.takeIf { it.isNotBlank() } ?: entry.expectedDeliveryDate
                    Text(
                        text = "Trip Date: $dateSubtitle • Item: ${entry.itemCode}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DeliveryStatusBadge(status = entry.deliveryStatus)

                        if (entry.transporter.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "🚚 ${entry.transporter}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (entry.expectedDeliveryDate.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "Exp: ${entry.expectedDeliveryDate}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Cardless Inline Stats Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${entry.pieces} pcs",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Quantity",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val packText = if (entry.caseCount > 0) "${entry.caseCount}c + ${entry.loosePieces}L" else "${entry.loosePieces} Loose"
                        Text(
                            text = packText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Packaging",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "₹${entry.rate.toInt()}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "Rate / pc",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = PdfGenerator.formatInr(entry.grandTotalWithGst),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Net Amount",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Quick Status Advance & Trip Navigation Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (entry.deliveryStatus != "Delivered") {
                        Button(
                            onClick = { viewModel.advanceEntryDeliveryStatus(entry) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val nextLabel = when (entry.deliveryStatus.lowercase()) {
                                "pending" -> "Mark Packed"
                                "packed" -> "Mark Dispatched"
                                "dispatched" -> "Mark Delivered"
                                else -> "Advance Status"
                            }
                            Text(nextLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (visit != null && onOpenVisit != null) {
                        OutlinedButton(
                            onClick = { onOpenVisit(visit) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Trip (${visit.date})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Customer Details Card
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CUSTOMER / BUYER DETAILS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                letterSpacing = 0.5.sp
                            )
                            if (customer != null && customer.customerType.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = customer.customerType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = customerDisplayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        if (customer?.firmName?.isNotBlank() == true && customer.name.isNotBlank() && customer.firmName != customer.name) {
                            Text(
                                text = "Contact Person: ${customer.name}",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        val customerCity = listOfNotNull(
                            customer?.city?.takeIf { it.isNotBlank() },
                            customer?.state?.takeIf { it.isNotBlank() }
                        ).joinToString(", ")

                        if (customerCity.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "City: $customerCity",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        if (customer?.gstin?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "GSTIN: ${customer.gstin}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }

                        if (salesman != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Escort / Salesman: ${salesman.name} (${salesman.phone})",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Action Pills for Customer
                        if (customerPhone.isNotBlank() || customerCity.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (customerPhone.isNotBlank()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFEFF6FF),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$customerPhone"))
                                                context.startActivity(intent)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                                            Text(text = customerPhone, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D4ED8))
                                        }
                                    }
                                }

                                val fullAddr = customer?.shopAddress?.ifBlank { customer.address } ?: customerCity
                                if (fullAddr.isNotBlank()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(fullAddr))
                                                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                                context.startActivity(mapIntent)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(13.dp))
                                            Text(text = "Directions", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Supplier Details Card
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SUPPLIER / MILL DETAILS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                letterSpacing = 0.5.sp
                            )
                            SupplierTypeBadge(type = supplier.type.ifBlank { entry.supplierType })
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val supplierDisplayName = supplier.firmName.ifBlank { supplier.name }
                        Text(
                            text = supplierDisplayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        if (supplier.firmName.isNotBlank() && supplier.name.isNotBlank() && supplier.firmName != supplier.name) {
                            Text(
                                text = "Contact Person: ${supplier.name}",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        if (supplier.city.isNotBlank() || supplier.marketArea.isNotBlank()) {
                            val marketStr = listOfNotNull(supplier.marketArea.takeIf { it.isNotBlank() }, supplier.city.takeIf { it.isNotBlank() }).joinToString(" • ")
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Market: $marketStr",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        if (supplier.gstin.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "GSTIN: ${supplier.gstin}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }

                        if (supplierPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supplierPhone"))
                                        context.startActivity(intent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(13.dp))
                                    Text(text = supplierPhone, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF065F46))
                                }
                            }
                        }
                    }
                }
            }

            // Order & Packaging Specs Card
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "ORDER & PACKAGING SPECIFICATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            letterSpacing = 0.5.sp
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Design / Item Code:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(entry.itemCode, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Pieces:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("${entry.pieces} Pcs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rate per Piece:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("₹${entry.rate.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Case Packaging:", fontSize = 12.sp, color = Color(0xFF64748B))
                            val packStr = if (entry.caseCount > 0 && entry.loosePieces > 0) {
                                "${entry.caseCount} Cases (${entry.caseCount * entry.caseSize} pcs) + ${entry.loosePieces} Loose"
                            } else if (entry.caseCount > 0) {
                                "${entry.caseCount} Cases (${entry.pieces} pcs)"
                            } else {
                                "${entry.loosePieces} Loose pcs"
                            }
                            Text(packStr, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                        }

                        if (!entry.mixedPackNote.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Pack Group Note: ${entry.mixedPackNote}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (entry.transporter.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Transporter:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(entry.transporter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment Status:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(
                                text = "${entry.paymentStatus} (${entry.paymentMode})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (entry.paymentStatus == "Received") Color(0xFF059669) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            // PDF Voucher Visual Document Preview
            item {
                Text(
                    text = "PDF VOUCHER PREVIEW",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Letterhead
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.himat_logo),
                                    contentDescription = "Himat Textile Logo",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .padding(end = 10.dp)
                                )
                                Column {
                                    Text(
                                        text = "HIMAT TEXTILE",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "YOUR BUSINESS GUIDE ACROSS INDIA",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                    Text(
                                        text = "Spot Procurement Voucher",
                                        fontSize = 8.5.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = Color(0xFF7C3AED),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "VOUCHER",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Date: ${visit?.date ?: entry.expectedDeliveryDate}",
                                    fontSize = 9.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                        // Table Header
                        Surface(
                            color = NavyPrimary,
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ITEM / ORDER", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color.White, modifier = Modifier.weight(1.8f))
                                Text("PCS", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
                                Text("RATE", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                                Text("PACK", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1.0f))
                                Text("AMOUNT", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color.White, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                            }
                        }

                        // Single Item Row
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.8f)) {
                                Text(
                                    text = entry.itemCode,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = entry.orderNo,
                                    fontSize = 9.5.sp,
                                    color = TextSecondary
                                )
                            }

                            Text(
                                text = "${entry.pieces}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.7f)
                            )

                            Text(
                                text = "₹${entry.rate.toInt()}",
                                fontSize = 10.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.8f)
                            )

                            val pack = if (entry.loosePieces > 0) "${entry.caseCount}c+${entry.loosePieces}L" else "${entry.caseCount}c"
                            Text(
                                text = pack,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = if (entry.loosePieces > 0) Color(0xFFD97706) else Color(0xFF15803D),
                                modifier = Modifier.weight(1.0f)
                            )

                            Text(
                                text = PdfGenerator.formatInr(entry.totalAmount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.End,
                                color = TextPrimary,
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)

                        // Totals Summary Box
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Taxable Subtotal:", fontSize = 11.5.sp, color = TextSecondary)
                                    Text(PdfGenerator.formatInr(entry.totalAmount), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Garment GST (${entry.gstRate.toInt()}%):", fontSize = 11.5.sp, color = TextSecondary)
                                    Text(PdfGenerator.formatInr(entry.gstAmount), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFE2E8F0))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("NET TOTAL PAYABLE:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = NavyPrimary)
                                    Text(PdfGenerator.formatInr(entry.grandTotalWithGst), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Supplier to issue original GST Tax Invoice against this purchase order.",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
