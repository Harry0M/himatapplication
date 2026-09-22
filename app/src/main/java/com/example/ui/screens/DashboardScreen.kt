package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyRupee
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.VisitEntity
import com.example.ui.components.IncompleteCaseBanner
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
    onOpenVisit: (VisitEntity) -> Unit,
    onBack: () -> Unit = { onNavigate(AppScreen.DASHBOARD) }
) {
    val role by viewModel.currentRole.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val visits by viewModel.visibleVisits.collectAsStateWithLifecycle()
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    val customers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.visibleSuppliers.collectAsStateWithLifecycle()

    val totalPieces = remember(entries) { entries.sumOf { it.pieces } }
    val totalCases = remember(entries) { entries.sumOf { it.caseCount } }
    val looseEntries = remember(entries) { entries.filter { it.loosePieces > 0 } }
    val totalLoosePcs = remember(looseEntries) { looseEntries.sumOf { it.loosePieces } }
    val pendingDeliveries = remember(entries) { entries.count { it.deliveryStatus != "Delivered" } }
    val deliveredCount = remember(entries) { entries.count { it.deliveryStatus.equals("Delivered", ignoreCase = true) } }
    val packedCount = remember(entries) { entries.count { it.deliveryStatus.equals("Packed", ignoreCase = true) } }
    val dispatchedCount = remember(entries) { entries.count { it.deliveryStatus.equals("Dispatched", ignoreCase = true) } }
    val pendingCount = remember(entries) { entries.count { it.deliveryStatus.equals("Pending", ignoreCase = true) } }
    val grandTotalAmount = remember(entries) { entries.sumOf { it.grandTotalWithGst } }
    val activeVisits = remember(visits) { visits.filter { it.status == "Active" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern Surface Top Bar with back button (clean, borderless)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Operations Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Market Analytics & Dispatch Pulse",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onOpenNewVisit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New Trip",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Primary Metrics Grid (Rich deep colors, NO borders)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardMetricCard(
                            title = "Total Procured",
                            value = "${String.format("%,d", totalPieces)} Pcs",
                            subtitle = "$totalCases Cases Packed",
                            accentColor = Color(0xFF059669), // Emerald
                            icon = Icons.Default.Inventory,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppScreen.REPORTS) }
                        )

                        DashboardMetricCard(
                            title = "Procurement Value",
                            value = "₹${String.format("%,d", grandTotalAmount.toLong())}",
                            subtitle = "${entries.size} Orders Logged",
                            accentColor = Color(0xFF1E3A8A), // Royal Navy
                            icon = Icons.Default.CurrencyRupee,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppScreen.REPORTS) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardMetricCard(
                            title = "Market Trips",
                            value = "${activeVisits.size} Active",
                            subtitle = "${visits.size} Total Completed",
                            accentColor = Color(0xFF4F46E5), // Indigo
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppScreen.VISITS) }
                        )

                        DashboardMetricCard(
                            title = "Deliveries In-Transit",
                            value = "$pendingDeliveries Orders",
                            subtitle = "$deliveredCount Cleared",
                            accentColor = Color(0xFFD97706), // Amber
                            icon = Icons.Default.LocalShipping,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppScreen.DELIVERIES) }
                        )
                    }
                }
            }

            // 2. Loose Pieces Mixed Packing Alert (if applicable)
            if (looseEntries.isNotEmpty()) {
                item {
                    IncompleteCaseBanner(
                        looseCount = totalLoosePcs,
                        ordersCount = looseEntries.size,
                        onMixedPackClick = {
                            val activeVisit = visits.firstOrNull { it.status == "Active" } ?: visits.firstOrNull()
                            if (activeVisit != null) {
                                onOpenVisit(activeVisit)
                            } else {
                                onNavigate(AppScreen.VISITS)
                            }
                        }
                    )
                }
            }

            // 3. Dispatch & Delivery Pipeline Breakdown (Borderless modern card)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                    text = "Dispatch & Transport Status",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Consignment tracking & parcel clearance pipeline",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${entries.size} Orders",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status counts row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PipelineStatusBadge(label = "Pending", count = pendingCount, color = Color(0xFFD97706))
                            PipelineStatusBadge(label = "Packed", count = packedCount, color = Color(0xFF2563EB))
                            PipelineStatusBadge(label = "Dispatched", count = dispatchedCount, color = Color(0xFF0D9488))
                            PipelineStatusBadge(label = "Delivered", count = deliveredCount, color = Color(0xFF059669))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val total = entries.size.coerceAtLeast(1).toFloat()
                        LinearProgressIndicator(
                            progress = { (deliveredCount / total).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = Color(0xFF059669),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // 4. Quick Tools Navigation (Borderless cards)
            item {
                Text(
                    text = "Directory & Operations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardQuickLink(
                        title = "Retailers",
                        count = "${customers.size}",
                        icon = Icons.Default.People,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.CUSTOMER_MASTER) }
                    )
                    DashboardQuickLink(
                        title = "Suppliers",
                        count = "${suppliers.size}",
                        icon = Icons.Default.Store,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.SUPPLIER_MASTER) }
                    )
                    DashboardQuickLink(
                        title = "Deliveries",
                        count = "$pendingDeliveries",
                        icon = Icons.Default.LocalShipping,
                        color = Color(0xFF0D9488),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.DELIVERIES) }
                    )
                    DashboardQuickLink(
                        title = "Reports",
                        count = "PDF",
                        icon = Icons.Default.Assessment,
                        color = Color(0xFFE11D48),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.REPORTS) }
                    )
                }
            }

            // 5. Recent Market Visits Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Market Trips",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigate(AppScreen.VISITS) }
                    ) {
                        Text(
                            text = "View All",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (visits.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No market visits logged yet. Tap '+ New Trip' to begin.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(visits.take(5)) { visit ->
                    val visitEntries = entries.filter { it.visitId == visit.id }
                    val tripPcs = visitEntries.sumOf { it.pieces }
                    val cust = customers.find { it.id == visit.customerId || it.firmName.equals(visit.customerName, true) || it.name.equals(visit.customerName, true) }
                    val photoUrl = cust?.let { it.purchaserPhotoUri.ifBlank { it.shopPhotoUri } }?.takeIf { it.isNotBlank() }
                        ?: visitEntries.firstOrNull { !it.orderFormPhotoUri.isNullOrBlank() }?.orderFormPhotoUri

                    VisitCardItem(
                        visit = visit,
                        entriesCount = visitEntries.size,
                        totalPcs = tripPcs,
                        photoUrl = photoUrl,
                        onClick = { onOpenVisit(visit) }
                    )
                }
            }
        }
    }
}

@Composable
fun VisitCardItem(
    visit: VisitEntity,
    entriesCount: Int,
    totalPcs: Int,
    photoUrl: String? = null,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Associated photo thumbnail or initial badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = visit.customerName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        val initials = visit.customerName.take(2).uppercase()
                        Text(
                            text = if (initials.isNotBlank()) initials else "MV",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = visit.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    StatusBadge(status = visit.status)
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${visit.visitCode} • ${visit.date} • Agent: ${visit.employeeName}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "$entriesCount stops • $totalPcs Pcs",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF059669)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PipelineStatusBadge(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = color,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DashboardQuickLink(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
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
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = count,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
