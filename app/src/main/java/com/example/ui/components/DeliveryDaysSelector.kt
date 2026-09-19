package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

/**
 * Calculates days from today until target date string (YYYY-MM-DD).
 */
fun calculateDaysFromToday(dateStr: String): Int {
    return try {
        if (dateStr.isBlank()) return 7
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDate = sdf.parse(dateStr) ?: return 7
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val target = Calendar.getInstance().apply {
            time = targetDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = target.timeInMillis - today.timeInMillis
        val days = (diff / (24 * 60 * 60 * 1000)).toInt()
        if (days < 0) 0 else days
    } catch (_: Exception) {
        7
    }
}

/**
 * Generates YYYY-MM-DD from today + [days].
 */
fun calculateDateFromDays(days: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, days.coerceAtLeast(0))
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
}

/**
 * Formats YYYY-MM-DD into a human-friendly format (e.g. "Sat, 03 Oct 2026").
 */
fun formatFriendlyDeliveryDate(dateStr: String): String {
    return try {
        if (dateStr.isBlank()) return "Standard Delivery"
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return dateStr
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        dateStr
    }
}

/**
 * Reusable Day-based delivery date selector with quick chips, days input, and date preview.
 */
@Composable
fun DeliveryDaysSelector(
    expectedDeliveryDate: String,
    onDeliveryDateChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDays = remember(expectedDeliveryDate) {
        calculateDaysFromToday(expectedDeliveryDate)
    }

    var daysInputText by remember(currentDays) {
        mutableStateOf(currentDays.toString())
    }

    val quickDayPresets = remember {
        listOf(
            0 to "Today",
            3 to "3 Days",
            5 to "5 Days",
            7 to "7 Days",
            10 to "10 Days",
            15 to "15 Days",
            20 to "20 Days",
            25 to "25 Days",
            30 to "30 Days"
        )
    }

    // DatePicker Dialog helper
    val showDatePicker = {
        val cal = Calendar.getInstance()
        try {
            if (expectedDeliveryDate.isNotBlank()) {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expectedDeliveryDate)
                if (parsed != null) cal.time = parsed
            }
        } catch (_: Exception) {}

        val picker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val newDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.time)
                onDeliveryDateChange(newDate)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        picker.datePicker.minDate = System.currentTimeMillis() - 1000
        picker.show()
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        // Quick Days Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickDayPresets.forEach { (days, label) ->
                val isSelected = currentDays == days
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            daysInputText = days.toString()
                            onDeliveryDateChange(calculateDateFromDays(days))
                        }
                ) {
                    Text(
                        text = label,
                        fontSize = 9.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) GoldAccent else TextPrimary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Days Input & Date Preview in Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Days TextField
            OutlinedTextField(
                value = daysInputText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(3)
                    daysInputText = filtered
                    val days = filtered.toIntOrNull() ?: 0
                    onDeliveryDateChange(calculateDateFromDays(days))
                },
                label = { Text("Delivery", fontSize = 9.sp) },
                placeholder = { Text("15", fontSize = 9.5.sp) },
                textStyle = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = NavyPrimary),
                suffix = { Text("Days", fontSize = 9.5.sp, color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.width(108.dp)
            )

            // Date Badge / Picker Trigger
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDatePicker() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = formatFriendlyDeliveryDate(expectedDeliveryDate),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = if (currentDays == 0) "Expected: Today" else "In $currentDays days from today",
                            fontSize = 8.5.sp,
                            color = TextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Pick calendar date",
                        tint = NavyPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
