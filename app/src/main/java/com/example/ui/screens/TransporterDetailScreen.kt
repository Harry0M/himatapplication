package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.TransporterEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransporterDetailScreen(
    viewModel: HimatViewModel,
    transporter: TransporterEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onOpenOrder: (PurchaseEntryEntity) -> Unit = { viewModel.openOrderDetail(it, returnScreen = AppScreen.TRANSPORTER_DETAIL) }
) {
    val context = LocalContext.current
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()

    val visitMap = remember(allVisits) { allVisits.associateBy { it.id } }

    val shipments = remember(allEntries, transporter.transporterName) {
        allEntries.filter {
            it.transporter.contains(transporter.transporterName, ignoreCase = true)
        }.sortedByDescending { it.id }
    }

    val totalShipments = shipments.size
    val inTransitCount = shipments.count { it.deliveryStatus.equals("Dispatched", ignoreCase = true) }
    val deliveredCount = shipments.count { it.deliveryStatus.equals("Delivered", ignoreCase = true) }
    val pendingCount = shipments.count { it.deliveryStatus != "Delivered" && it.deliveryStatus != "Dispatched" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = transporter.transporterName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Logistics Master • ${transporter.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Transporter", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF6F8FB))
            )
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
            // Cardless Hero Profile Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFECFEFF),
                        border = BorderStroke(2.dp, Color(0xFFA5F3FC)),
                        modifier = Modifier.size(66.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = Color(0xFF0891B2),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = transporter.transporterName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    val contactSubtitle = if (transporter.contactPerson.isNotBlank()) "Desk: ${transporter.contactPerson}" else "Hub: ${transporter.city}"
                    Text(
                        text = contactSubtitle,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "📍 ${transporter.city}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (transporter.gstin.isNotBlank()) {
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFBFDBFE))
                            ) {
                                Text(
                                    text = "GST: ${transporter.gstin}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Action Pills: Phone 1, Phone 2, Map Direction, Tracking URL
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (transporter.phone1.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${transporter.phone1}"))
                                        context.startActivity(intent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(13.dp))
                                    Text(text = transporter.phone1, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF065F46))
                                }
                            }
                        }

                        val mapAddr = transporter.godownAddress.ifBlank { transporter.officeAddress }.ifBlank { transporter.city }
                        if (mapAddr.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(mapAddr))
                                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                        context.startActivity(mapIntent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                                    Text("Depot", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D4ED8))
                                }
                            }
                        }

                        if (transporter.trackingUrl.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFAF5FF),
                                border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val url = if (!transporter.trackingUrl.startsWith("http")) "https://${transporter.trackingUrl}" else transporter.trackingUrl
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(13.dp))
                                    Text("Tracking", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7E22CE))
                                }
                            }
                        }
                    }
                }
            }

            // Cardless Stats Row
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
                            text = "$totalShipments",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text("Shipments", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$inTransitCount",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                        Text("Dispatched", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$deliveredCount",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Text("Delivered", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$pendingCount",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingCount > 0) Color(0xFFDC2626) else Color(0xFF059669)
                        )
                        Text("Pending", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }

            // Logistics Hub Details
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "LOGISTICS & DEPOT DETAILS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            letterSpacing = 0.5.sp
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)

                        if (transporter.officeAddress.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Office Address:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(transporter.officeAddress, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            }
                        }

                        if (transporter.godownAddress.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Godown / Depot:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(transporter.godownAddress, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            }
                        }

                        if (transporter.destinationsCovered.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Routes Covered:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(transporter.destinationsCovered, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0891B2))
                            }
                        }

                        if (transporter.phone2.isNotBlank() || transporter.phone3.isNotBlank()) {
                            val extraPhones = listOfNotNull(transporter.phone2.takeIf { it.isNotBlank() }, transporter.phone3.takeIf { it.isNotBlank() }).joinToString(", ")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Alternate Desks:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(extraPhones, fontSize = 12.sp, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }
            }

            // Shipments Section
            item {
                Text(
                    text = "Shipments Handled (${shipments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (shipments.isEmpty()) {
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No shipments recorded under this transporter yet", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                items(shipments, key = { it.id }) { entry ->
                    val visit = visitMap[entry.visitId]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenOrder(entry) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(entry.orderNo, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF2563EB))
                                    DeliveryStatusBadge(status = entry.deliveryStatus)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Buyer: ${visit?.customerName ?: "Direct Customer"} • ${entry.itemCode}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val packText = if (entry.caseCount > 0) "${entry.caseCount}c + ${entry.loosePieces}L (${entry.pieces} pcs)" else "${entry.pieces} loose pcs"
                                Text(
                                    text = "Pack: $packText • Date: ${visit?.date ?: entry.expectedDeliveryDate}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (entry.deliveryStatus != "Delivered") {
                                    OutlinedButton(
                                        onClick = { viewModel.advanceEntryDeliveryStatus(entry) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = 30.dp)
                                    ) {
                                        val nextLabel = when (entry.deliveryStatus.lowercase()) {
                                            "pending" -> "Packed"
                                            "packed" -> "Dispatch"
                                            "dispatched" -> "Deliver"
                                            else -> "Advance"
                                        }
                                        Text(nextLabel, style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View Order", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
