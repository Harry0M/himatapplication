package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HimatTopBar(
    role: String,
    salesmanName: String?,
    onSwitchRole: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HT",
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "HIMAT TEXTILE",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Garment Sourcing Agency",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Material 3 Role Switcher Pill with accessible touch target
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSwitchRole() }
                    .defaultMinSize(minHeight = 36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Role",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (role == "Admin") "Admin (Owner)" else (salesmanName ?: "Salesman"),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Switch",
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Standardized Material 3 Expressive Search Field.
 * Ergonomically calibrated so it doesn't look too tiny, oversized, or awkwardly tall on any device.
 */
@Composable
fun ExpressiveSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier
                            .size(40.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon: ImageVector) = when (status.lowercase()) {
        "packed" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.Inventory
        )
        "dispatched" -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Default.LocalShipping
        )
        "delivered", "completed" -> Triple(
            Color(0xFFDCFCE7),
            Color(0xFF15803D),
            Icons.Default.CheckCircle
        )
        "active" -> Triple(
            Color(0xFFEFF6FF),
            Color(0xFF1D4ED8),
            Icons.Default.Schedule
        )
        else -> Triple(
            Color(0xFFF1F5F9),
            Color(0xFF475569),
            Icons.Default.Schedule
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.heightIn(min = 22.dp, max = 26.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DeliveryStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    StatusBadge(status = status, modifier = modifier)
}

@Composable
fun SupplierTypeBadge(
    type: String,
    modifier: Modifier = Modifier
) {
    val isWholesaler = type.equals("Wholesaler", ignoreCase = true)
    val color = if (isWholesaler) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED)
    val bgColor = if (isWholesaler) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF3E8FF)
    val textColor = if (isWholesaler) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF6B21A8)
    val label = if (isWholesaler) "Wholesale" else "Mfr"

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f)),
        modifier = modifier.heightIn(min = 20.dp, max = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isWholesaler) "🏬 $label" else "🏭 $label",
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun IncompleteCaseBanner(
    looseCount: Int,
    ordersCount: Int,
    onMixedPackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mixed Pack Needed ($looseCount Loose Pcs)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = Color(0xFF92400E)
                )
                Text(
                    text = "$ordersCount order(s) have loose pieces. Combine across suppliers.",
                    fontSize = 11.sp,
                    color = Color(0xFFB45309),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color(0xFFD97706),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onMixedPackClick() }
            ) {
                Text(
                    text = "Combine →",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}
