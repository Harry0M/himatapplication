package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Sell
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
import com.example.ui.dialogs.FullScreenImageViewerDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel
import com.example.util.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandDetailScreen(
    viewModel: HimatViewModel,
    brand: BrandEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onOpenProduct: (ProductEntity) -> Unit = { viewModel.openProductDetail(it) },
    onOpenOrder: (PurchaseEntryEntity) -> Unit = { viewModel.openOrderDetail(it, returnScreen = AppScreen.BRAND_DETAIL) }
) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val allVisits by viewModel.allVisits.collectAsStateWithLifecycle()

    val visitMap = remember(allVisits) { allVisits.associateBy { it.id } }
    var showLogoViewer by remember { mutableStateOf(false) }

    val brandProducts = remember(allProducts, brand.id, brand.brandName, brand.manufacturerId) {
        allProducts.filter { product ->
            (brand.manufacturerId != null && brand.manufacturerId != 0L && product.supplierId == brand.manufacturerId) ||
            product.name.contains(brand.brandName, ignoreCase = true) ||
            product.productCode.contains(brand.brandName, ignoreCase = true) ||
            product.description.contains(brand.brandName, ignoreCase = true)
        }.sortedBy { it.name }
    }

    val brandOrders = remember(allEntries, brandProducts, brand.brandName) {
        val productCodes = brandProducts.map { it.productCode.lowercase() }.toSet()
        allEntries.filter { entry ->
            productCodes.contains(entry.itemCode.lowercase()) ||
            entry.itemCode.contains(brand.brandName, ignoreCase = true)
        }.sortedByDescending { it.id }
    }

    val totalPiecesVolume = brandOrders.sumOf { it.pieces }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = brand.brandName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Brand Master • ${brand.category.ifBlank { "Garments" }}",
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
                        Icon(Icons.Default.Edit, contentDescription = "Edit Brand", tint = MaterialTheme.colorScheme.primary)
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
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(2.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .size(66.dp)
                            .then(
                                if (brand.logoPhotoUri.isNotBlank()) Modifier.clickable { showLogoViewer = true }
                                else Modifier
                            )
                    ) {
                        if (brand.logoPhotoUri.isNotBlank()) {
                            AsyncImage(
                                model = brand.logoPhotoUri,
                                contentDescription = brand.brandName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sell,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    if (brand.logoPhotoUri.isNotBlank()) {
                        Text(
                            text = "Tap logo for full view & download",
                            fontSize = 10.5.sp,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(top = 4.dp).clickable { showLogoViewer = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = brand.brandName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    val mfrText = if (brand.manufacturerName.isNotBlank()) "Mfr: ${brand.manufacturerName}" else "Direct Garment Label"
                    Text(
                        text = mfrText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (brand.category.isNotBlank()) {
                            Surface(
                                color = Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFC7D2FE))
                            ) {
                                Text(
                                    text = brand.category,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4338CA),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Surface(
                            color = if (brand.isActive) Color(0xFFECFDF5) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (brand.isActive) "Active Label" else "Inactive",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (brand.isActive) Color(0xFF065F46) else Color(0xFFB91C1C),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Action Pills
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onEdit() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
                                Text("Edit Brand", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E))
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val text = "Brand: ${brand.brandName}\nManufacturer: ${brand.manufacturerName}\nCategory: ${brand.category}\nActive Products: ${brandProducts.size}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Brand Specs", text))
                                    Toast.makeText(context, "Brand details copied", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(13.dp))
                                Text("Copy Specs", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
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
                            text = "${brandProducts.size}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text("Products", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${brandOrders.size}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text("Orders", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFE2E8F0)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%,d", totalPiecesVolume),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Text("Pieces Sold", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }

            // Brand Details Card
            if (brand.description.isNotBlank() || brand.manufacturerName.isNotBlank()) {
                item {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ABOUT THE BRAND",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                letterSpacing = 0.5.sp
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)

                            if (brand.manufacturerName.isNotBlank()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Manufacturer / Mill:", fontSize = 12.sp, color = Color(0xFF64748B))
                                    Text(brand.manufacturerName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                }
                            }
                            if (brand.category.isNotBlank()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Segment / Category:", fontSize = 12.sp, color = Color(0xFF64748B))
                                    Text(brand.category, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                }
                            }
                            if (brand.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(brand.description, fontSize = 11.5.sp, color = Color(0xFF334155))
                            }
                        }
                    }
                }
            }

            // Section Header: Associated Products
            item {
                Text(
                    text = "Products under this Brand (${brandProducts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (brandProducts.isEmpty()) {
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
                                Icons.Default.Inventory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No catalog products linked to this brand yet", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                items(brandProducts, key = { it.id }) { prod ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenProduct(prod) }
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
                                    color = Color(0xFFEEF2FF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Inventory, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text("Code: ${prod.productCode} • ${prod.category}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("₹${prod.defaultRate.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF059669))
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Section Header: Associated Orders
            if (brandOrders.isNotEmpty()) {
                item {
                    Text(
                        text = "Orders for this Brand (${brandOrders.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(brandOrders, key = { it.id }) { entry ->
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
                                    Text(entry.orderNo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2563EB))
                                    DeliveryStatusBadge(status = entry.deliveryStatus)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Item: ${entry.itemCode} • Buyer: ${visit?.customerName ?: "Direct"}", fontSize = 11.5.sp, color = Color(0xFF475569))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${entry.pieces} pcs", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text(PdfGenerator.formatInr(entry.grandTotalWithGst), fontSize = 11.sp, color = Color(0xFF059669))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoViewer && brand.logoPhotoUri.isNotBlank()) {
        FullScreenImageViewerDialog(
            imageUrl = brand.logoPhotoUri,
            title = "${brand.brandName} Logo",
            onDismiss = { showLogoViewer = false }
        )
    }
}
