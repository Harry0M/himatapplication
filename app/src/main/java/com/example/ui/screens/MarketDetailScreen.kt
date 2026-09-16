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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.MarketEntity
import com.example.data.local.entity.SupplierEntity
import com.example.ui.components.SupplierTypeBadge
import com.example.ui.viewmodel.HimatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDetailScreen(
    viewModel: HimatViewModel,
    market: MarketEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onOpenCustomer: (CustomerEntity) -> Unit = { viewModel.openCustomerDetail(it) },
    onOpenSupplier: (SupplierEntity) -> Unit = { viewModel.openSupplierDetail(it) }
) {
    val context = LocalContext.current
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val allSuppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("CUSTOMERS") } // "CUSTOMERS" or "SUPPLIERS"

    val marketCustomers = remember(allCustomers, market.id, market.marketName) {
        allCustomers.filter {
            it.marketArea.contains(market.marketName, ignoreCase = true) ||
            it.markets.contains(market.marketName, ignoreCase = true) ||
            (market.area.isNotBlank() && it.address.contains(market.area, ignoreCase = true))
        }.sortedBy { it.firmName.ifBlank { it.name } }
    }

    val marketSuppliers = remember(allSuppliers, market.id, market.marketName) {
        allSuppliers.filter {
            it.marketId == market.id ||
            it.marketArea.contains(market.marketName, ignoreCase = true) ||
            it.markets.contains(market.marketName, ignoreCase = true) ||
            (market.area.isNotBlank() && it.address.contains(market.area, ignoreCase = true))
        }.sortedBy { it.firmName.ifBlank { it.name } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = market.marketName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Market Hub • ${market.city}",
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
                        Icon(Icons.Default.Edit, contentDescription = "Edit Market", tint = MaterialTheme.colorScheme.primary)
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
                        color = Color(0xFFFAF5FF),
                        border = BorderStroke(2.dp, Color(0xFFE9D5FF)),
                        modifier = Modifier.size(66.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = null,
                                tint = Color(0xFF9333EA),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = market.marketName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    val areaText = listOfNotNull(market.area.takeIf { it.isNotBlank() }, market.city.takeIf { it.isNotBlank() }).joinToString(", ")
                    Text(
                        text = areaText,
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
                            color = Color(0xFFFAF5FF),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color(0xFFE9D5FF))
                        ) {
                            Text(
                                text = "🏷️ ${market.marketType}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7E22CE),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (market.pincode.isNotBlank()) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "PIN: ${market.pincode}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Action Pills: Map Direction, Edit
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val fullAddress = listOfNotNull(market.marketName, market.landmark.takeIf { it.isNotBlank() }, market.area.takeIf { it.isNotBlank() }, market.city).joinToString(", ")
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(fullAddress))
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
                                Text("Market Map", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D4ED8))
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFAF5FF),
                            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onEdit() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(13.dp))
                                Text("Edit Hub", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7E22CE))
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
                            text = "${marketCustomers.size}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                        Text("Buyers", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${marketSuppliers.size}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Text("Suppliers", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = market.marketType,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7E22CE)
                        )
                        Text("Market Type", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = market.city,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text("City", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }

            // Market Details Card
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MARKET LOCATION & DETAILS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            letterSpacing = 0.5.sp
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)

                        if (market.area.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Area / Locality:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(market.area, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            }
                        }

                        if (market.landmark.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Landmark:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(market.landmark, fontSize = 12.sp, color = Color(0xFF0F172A))
                            }
                        }

                        if (market.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("About Market:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Text(market.description, fontSize = 11.5.sp, color = Color(0xFF334155))
                        }
                    }
                }
            }

            // Tabs for Customers vs Suppliers in this Market
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isCust = selectedTab == "CUSTOMERS"
                    Surface(
                        shape = CircleShape,
                        color = if (isCust) Color(0xFF2563EB) else Color.White,
                        border = BorderStroke(1.dp, if (isCust) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { selectedTab = "CUSTOMERS" }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = if (isCust) Color.White else Color(0xFF2563EB),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Customers (${marketCustomers.size})",
                                fontSize = 12.sp,
                                fontWeight = if (isCust) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCust) Color.White else Color(0xFF334155)
                            )
                        }
                    }

                    val isSupp = selectedTab == "SUPPLIERS"
                    Surface(
                        shape = CircleShape,
                        color = if (isSupp) Color(0xFF059669) else Color.White,
                        border = BorderStroke(1.dp, if (isSupp) Color(0xFF059669) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { selectedTab = "SUPPLIERS" }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Store,
                                contentDescription = null,
                                tint = if (isSupp) Color.White else Color(0xFF059669),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Suppliers (${marketSuppliers.size})",
                                fontSize = 12.sp,
                                fontWeight = if (isSupp) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSupp) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // List of entities in the selected tab
            if (selectedTab == "CUSTOMERS") {
                if (marketCustomers.isEmpty()) {
                    item {
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No customers mapped to this market yet", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    items(marketCustomers, key = { it.id }) { customer ->
                        val name = customer.firmName.ifBlank { customer.name }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenCustomer(customer) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFEFF6FF),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = name.take(1).uppercase(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2563EB)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(customer.phone.ifBlank { customer.city }, fontSize = 11.5.sp, color = Color(0xFF64748B))
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (customer.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                if (marketSuppliers.isEmpty()) {
                    item {
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No suppliers/mills mapped to this market yet", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    items(marketSuppliers, key = { it.id }) { supplier ->
                        val name = supplier.firmName.ifBlank { supplier.name }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenSupplier(supplier) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFECFDF5),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            SupplierTypeBadge(type = supplier.type)
                                        }
                                        Text(supplier.phone.ifBlank { supplier.city }, fontSize = 11.5.sp, color = Color(0xFF64748B))
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (supplier.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier.phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
