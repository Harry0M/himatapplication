package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ShareUtil

enum class RegistrationShareTarget {
    CUSTOMER,
    SUPPLIER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationShareBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTarget by remember { mutableStateOf(RegistrationShareTarget.CUSTOMER) }
    var phoneNumber by remember { mutableStateOf("") }
    val isCompletePhone = phoneNumber.length == 10

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Share Registration Link",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = NavyPrimary
                    )
                    Text(
                        text = "Share registration link directly via WhatsApp",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step 1: Target Selector (Customer vs Supplier)
            Text(
                text = "1. Select Registration Type:",
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Customer Option
                val isCustomerSelected = selectedTarget == RegistrationShareTarget.CUSTOMER
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCustomerSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        width = if (isCustomerSelected) 2.dp else 1.dp,
                        color = if (isCustomerSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedTarget = RegistrationShareTarget.CUSTOMER }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = if (isCustomerSelected) Color(0xFF2563EB) else Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                            if (isCustomerSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Customer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isCustomerSelected) Color(0xFF1E40AF) else TextPrimary
                        )
                    }
                }

                // Supplier Option
                val isSupplierSelected = selectedTarget == RegistrationShareTarget.SUPPLIER
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSupplierSelected) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        width = if (isSupplierSelected) 2.dp else 1.dp,
                        color = if (isSupplierSelected) Color(0xFF059669) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedTarget = RegistrationShareTarget.SUPPLIER }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = if (isSupplierSelected) Color(0xFF059669) else Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                            if (isSupplierSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Supplier",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSupplierSelected) Color(0xFF065F46) else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step 2: Phone Number Input
            Text(
                text = "2. Mobile Number (Optional for Direct Chat):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter mobile number to open chat directly in WhatsApp",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    if (digits.length <= 10) {
                        phoneNumber = digits
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("9876543210", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = {
                    Text(
                        text = "+91",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                    )
                },
                trailingIcon = {
                    if (phoneNumber.isNotEmpty()) {
                        IconButton(onClick = { phoneNumber = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF16A34A),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter ${10 - phoneNumber.length} more digits for direct chat",
                    fontSize = 10.5.sp,
                    color = Color(0xFFD97706)
                )
            } else if (isCompletePhone) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ Direct WhatsApp chat ready",
                    fontSize = 10.5.sp,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step 3: Action Buttons
            val handleSend = { targetPhone: String? ->
                if (selectedTarget == RegistrationShareTarget.CUSTOMER) {
                    ShareUtil.shareCustomerRegistrationLink(context, targetPhone)
                } else {
                    ShareUtil.shareSupplierRegistrationLink(context, targetPhone)
                }
                onDismiss()
            }

            if (isCompletePhone) {
                Button(
                    onClick = { handleSend(phoneNumber) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open WhatsApp (+91 $phoneNumber) ➜",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { handleSend(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF166534)),
                    border = BorderStroke(1.dp, Color(0xFF16A34A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Share to Any Contact Instead",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = { handleSend(if (phoneNumber.length == 10) phoneNumber else null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (phoneNumber.isEmpty()) "Open WhatsApp & Select Contact" else "Continue to WhatsApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
