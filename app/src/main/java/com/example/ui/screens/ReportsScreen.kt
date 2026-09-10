package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ManufacturerBadge
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WholesalerBadge
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator

@Composable
fun ReportsScreen(
    viewModel: HimatViewModel
) {
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()

    val totalPieces = entries.sumOf { it.pieces }
    val totalAmount = entries.sumOf { it.totalAmount }
    val totalCases = entries.sumOf { it.caseCount }
    val totalLoose = entries.sumOf { it.loosePieces }

    val manufacturerEntries = entries.filter { it.supplierType.equals("Manufacturer", ignoreCase = true) }
    val wholesalerEntries = entries.filter { it.supplierType.equals("Wholesaler", ignoreCase = true) }

    val mfrAmount = manufacturerEntries.sumOf { it.totalAmount }
    val wholesaleAmount = wholesalerEntries.sumOf { it.totalAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Sourcing Analytics & Reports",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Text(
                    text = "Procurement volume across manufacturers and wholesale hubs",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
            }
        }

        // Manufacturer vs Wholesaler Split Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Source Distribution (Manufacturer vs Wholesaler)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val mfrRatio = if (totalAmount > 0) (mfrAmount / totalAmount).toFloat() else 0.5f

                    LinearProgressIndicator(
                        progress = { mfrRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = ManufacturerBadge,
                        trackColor = WholesalerBadge,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(ManufacturerBadge, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manufacturers (Direct)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(PdfGenerator.formatInr(mfrAmount), fontSize = 11.5.sp, color = TextSecondary)
                            Text("${manufacturerEntries.sumOf { it.pieces }} Pcs", fontSize = 11.sp, color = ManufacturerBadge, fontWeight = FontWeight.SemiBold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(WholesalerBadge, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Wholesalers / Stockists", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(PdfGenerator.formatInr(wholesaleAmount), fontSize = 11.5.sp, color = TextSecondary)
                            Text("${wholesalerEntries.sumOf { it.pieces }} Pcs", fontSize = 11.sp, color = WholesalerBadge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Supplier-wise Volume Ranking
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Top Sourcing Partners (Suppliers)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val groupedSuppliers = entries.groupBy { it.supplierName }
                    if (groupedSuppliers.isEmpty()) {
                        Text("No supplier entries yet.", fontSize = 12.sp, color = TextSecondary)
                    } else {
                        groupedSuppliers.forEach { (name, list) ->
                            val pcs = list.sumOf { it.pieces }
                            val amt = list.sumOf { it.totalAmount }
                            val supType = list.firstOrNull()?.supplierType ?: ""

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SupplierTypeBadge(type = supType)
                                    }
                                    Text("$pcs Pieces procured across ${list.size} orders", fontSize = 11.sp, color = TextSecondary)
                                }

                                Text(PdfGenerator.formatInr(amt), fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = NavyPrimary)
                            }
                            Divider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }

        // Retailer Sourcing Volume
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Retailer Procurement Aggregates",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val visits = viewModel.allVisits.value
                    if (visits.isEmpty()) {
                        Text("No customer visits recorded yet.", fontSize = 12.sp, color = TextSecondary)
                    } else {
                        visits.forEach { visit ->
                            val visitItems = entries.filter { it.visitId == visit.id }
                            val pcs = visitItems.sumOf { it.pieces }
                            val amt = visitItems.sumOf { it.totalAmount }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(visit.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text("Visit: ${visit.visitCode} • $pcs Pcs sourced", fontSize = 11.sp, color = TextSecondary)
                                }
                                Text(PdfGenerator.formatInr(amt), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF059669))
                            }
                            Divider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}
