package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.ui.theme.NavyPrimary
import com.example.util.IndianRailwayStations

/**
 * A searchable dropdown field for selecting an Indian Railway Station.
 *
 * @param value          The currently selected station string (or free-form text).
 * @param onValueChange  Callback when user selects a station or types a custom value.
 * @param label          Field label. Defaults to "Booking / Delivery Station".
 * @param modifier       Modifier for the outer column.
 * @param isRequired     If true, shows a red asterisk.
 * @param shape          Corner shape of the OutlinedTextField.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationPickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Booking / Delivery Station",
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
) {
    var query by remember { mutableStateOf(value) }
    var showDropdown by remember { mutableStateOf(false) }

    // Keep query in sync with external value changes (e.g. form reset)
    LaunchedEffect(value) {
        if (value != query) query = value
    }

    val suggestions = remember(query) {
        IndianRailwayStations.search(query)
    }

    Column(modifier = modifier) {
        Box {
            OutlinedTextField(
                value = query,
                onValueChange = { typed ->
                    query = typed
                    onValueChange(typed)
                    showDropdown = typed.isNotEmpty() || suggestions.isNotEmpty()
                },
                label = {
                    Text(
                        text = if (isRequired) "$label *" else label,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                placeholder = {
                    Text(
                        "Railway station khoje...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Train,
                        contentDescription = "Station",
                        modifier = Modifier.size(18.dp),
                        tint = NavyPrimary.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            onValueChange("")
                            showDropdown = false
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = shape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable { showDropdown = true },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    focusedLabelColor = NavyPrimary,
                    focusedLeadingIconColor = NavyPrimary,
                )
            )

            // Dropdown
            DropdownMenu(
                expanded = showDropdown && suggestions.isNotEmpty(),
                onDismissRequest = { showDropdown = false },
                properties = PopupProperties(focusable = false),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 260.dp)
            ) {
                suggestions.forEachIndexed { index, station ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Train,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(end = 4.dp),
                                    tint = NavyPrimary.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = station,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (station == value) FontWeight.Bold else FontWeight.Normal,
                                    color = if (station == value) NavyPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            query = station
                            onValueChange(station)
                            showDropdown = false
                        }
                    )
                    if (index < suggestions.lastIndex) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        // Helper text
        Text(
            text = "India ke railway station list mein se chunein",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}
