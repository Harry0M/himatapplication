package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.IncompleteCaseBanner
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel

@Composable
fun DashboardScreen(
    viewModel: HimatViewModel,
    onNavigate: (AppScreen) -> Unit,
    onOpenNewVisit: () -> Unit,
    onOpenVisit: (VisitEntity) -> Unit
) {
    val role = viewModel.currentRole.value
    val currentEmployee = viewModel.currentEmployee.value
    val visits = viewModel.allVisits.value
    val entries = viewModel.allEntries.value
    val customers = viewModel.allCustomers.value
    val suppliers = viewModel.allSuppliers.value

    val totalPieces = entries.sumOf { it.pieces }
    val totalCases = entries.sumOf { it.caseCount }
    val looseEntries = entries.filter { it.loosePieces > 0 }
    val totalLoosePcs = looseEntries.sumOf { it.loosePieces }
    val pendingDeliveries = entries.count { it.deliveryStatus != "Delivered" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Role Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (role == "Admin") "Agency Operations Dashboard" else "Field Salesman Portal",
                                color = GoldAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (role == "Admin") "Himat Bhai (Owner)" else (currentEmployee?.name ?: "Salesman"),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onOpenNewVisit,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New Visit",
                                color = NavyPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Escort retailers through wholesale markets, log spot purchases, and instantly generate two-sided documents (Customer Consolidated Day Report + Wholesaler Purchase Copy).",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Incomplete Case Alert Banner
        if (looseEntries.isNotEmpty()) {
            item {
                IncompleteCaseBanner(
                    looseCount = totalLoosePcs,
                    ordersCount = looseEntries.size,
                    onMixedPackClick = {
                        val activeVisit = visits.firstOrNull { it.status == "Active" } ?: visits.firstOrNull()
                        if (activeVisit != null) {
                            onOpenVisit(activeVisit)
                        }
                    }
                )
            }
        }

        // 4 Key Stats Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Today's Visits",
                        value = "${visits.count { it.status == "Active" }} Active",
                        subtitle = "${visits.size} Total Market Trips",
                        icon = Icons.Default.Assignment,
                        iconTint = NavyPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.VISITS) }
                    )
                    StatCard(
                        title = "Procured Volume",
                        value = "${String.format("%,d", totalPieces)} Pcs",
                        subtitle = "$totalCases Full Cases Packed",
                        icon = Icons.Default.Inventory,
                        iconTint = Color(0xFF059669),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.REPORTS) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Mixed Pack Loose",
                        value = "$totalLoosePcs Loose",
                        subtitle = "${looseEntries.size} orders waiting",
                        icon = Icons.Default.FactCheck,
                        iconTint = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.REPORTS) }
                    )
                    StatCard(
                        title = "Pending Deliveries",
                        value = "$pendingDeliveries Orders",
                        subtitle = "Bilty & Dispatches",
                        icon = Icons.Default.LocalShipping,
                        iconTint = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.DELIVERIES) }
                    )
                }
            }
        }

        // Quick Navigation Shortcuts
        item {
            Text(
                text = "Quick Masters & Tools",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickNavButton(
                    title = "Customers",
                    count = "${customers.size}",
                    icon = Icons.Default.People,
                    color = NavyPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.CUSTOMER_MASTER) }
                )
                QuickNavButton(
                    title = "Suppliers",
                    count = "${suppliers.size}",
                    icon = Icons.Default.Store,
                    color = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.SUPPLIER_MASTER) }
                )
                QuickNavButton(
                    title = "Deliveries",
                    count = "$pendingDeliveries",
                    icon = Icons.Default.LocalShipping,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.DELIVERIES) }
                )
                QuickNavButton(
                    title = "Analytics",
                    count = "Sales",
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFF059669),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.REPORTS) }
                )
            }
        }

        // Active Market Trips Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Market Visits",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "View All →",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = NavyPrimary,
                    modifier = Modifier.clickable { onNavigate(AppScreen.VISITS) }
                )
            }
        }

        // Visits List
        if (visits.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No visits logged yet. Click '+ New Visit' to begin escorting a customer.",
                        fontSize = 12.5.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(visits.take(4)) { visit ->
                VisitCardItem(
                    visit = visit,
                    entriesCount = entries.count { it.visitId == visit.id },
                    totalPcs = entries.filter { it.visitId == visit.id }.sumOf { it.pieces },
                    onClick = { onOpenVisit(visit) }
                )
            }
        }
    }
}

@Composable
fun QuickNavButton(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = count,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun VisitCardItem(
    visit: VisitEntity,
    entriesCount: Int,
    totalPcs: Int,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = visit.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    StatusBadge(status = visit.status)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Visit Code: ${visit.visitCode} • Date: ${visit.date}",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Agent: ${visit.employeeName}",
                        fontSize = 11.5.sp,
                        color = NavyPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(" • ", color = TextSecondary)
                    Text(
                        text = "$entriesCount Stops ($totalPcs Pcs)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
