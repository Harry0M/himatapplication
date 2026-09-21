package com.example.ui.dialogs

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.entity.SupplierEntity
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun SupplierRegistrationDialog(
    viewModel: HimatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var type by remember { mutableStateOf("Manufacturer") }
    var firmName by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phone2 by remember { mutableStateOf("") }
    var marketName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Ahmedabad") }
    var officeAddress by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var panNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankAccountNo by remember { mutableStateOf("") }
    var bankIfsc by remember { mutableStateOf("") }
    var paymentTermsDays by remember { mutableIntStateOf(30) }
    var notes by remember { mutableStateOf("") }

    var visitingCardPhotoUri by remember { mutableStateOf("") }
    var shopPhotoUri by remember { mutableStateOf("") }

    var isUploadingCard by remember { mutableStateOf(false) }
    var isUploadingShop by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val visitingCardLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploadingCard = true
                try {
                    val ref = FirebaseStorage.getInstance().reference.child("suppliers/visiting_cards/${UUID.randomUUID()}.jpg")
                    ref.putFile(it).await()
                    visitingCardPhotoUri = ref.downloadUrl.await().toString()
                } catch (e: Exception) {
                    visitingCardPhotoUri = it.toString()
                } finally {
                    isUploadingCard = false
                }
            }
        }
    }

    val shopPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploadingShop = true
                try {
                    val ref = FirebaseStorage.getInstance().reference.child("suppliers/shop_photos/${UUID.randomUUID()}.jpg")
                    ref.putFile(it).await()
                    shopPhotoUri = ref.downloadUrl.await().toString()
                } catch (e: Exception) {
                    shopPhotoUri = it.toString()
                } finally {
                    isUploadingShop = false
                }
            }
        }
    }

    fun submit() {
        if (firmName.isBlank()) {
            errorMessage = "Firm / Mill Name is required."
            return
        }
        if (contactPerson.isBlank()) {
            errorMessage = "Contact Person / Proprietor Name is required."
            return
        }
        if (phone.isBlank()) {
            errorMessage = "Primary phone number is required."
            return
        }

        isSubmitting = true
        val generatedId = "SUPP-${(100..999).random()}"
        val candidate = SupplierEntity(
            supplierId = generatedId,
            name = contactPerson.trim(),
            firmName = firmName.trim(),
            type = type,
            phone = phone.trim(),
            phone2 = phone2.trim(),
            marketArea = marketName.trim(),
            marketName = marketName.trim(),
            city = city.trim(),
            address = officeAddress.trim(),
            officeAddress = officeAddress.trim(),
            gstin = gstin.trim().uppercase(),
            panNumber = panNumber.trim().uppercase(),
            visitingCardPhotoUri = visitingCardPhotoUri,
            shopPhotoUri = shopPhotoUri,
            notes = (if (paymentTermsDays > 0) "Payment Terms: $paymentTermsDays days. " else "") + notes.trim()
        )

        viewModel.saveSupplier(candidate)
        Toast.makeText(context, "Supplier registered successfully!", Toast.LENGTH_SHORT).show()
        isSubmitting = false
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFECFDF5),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Store,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Register New Supplier",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Add fabric mill or garment manufacturer",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    errorMessage?.let { msg ->
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Supplier Classification
                    Text("SUPPLIER TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Manufacturer", "Wholesaler").forEach { opt ->
                            val isSelected = type == opt
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF059669) else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF059669) else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { type = opt }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = opt,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Basic Information
                    Text("BASIC DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                    // GSTIN First (Optional)
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { input ->
                            val clean = input.uppercase().filter { it.isLetterOrDigit() }.take(15)
                            gstin = clean
                            if (clean.length >= 12 && panNumber.isBlank()) {
                                val extractedPan = clean.substring(2, 12)
                                if (extractedPan.matches(Regex("^[A-Z]{5}[0-9]{4}[A-Z]$"))) {
                                    panNumber = extractedPan
                                }
                            }
                        },
                        label = { Text("GSTIN Number (Optional)", fontSize = 12.sp) },
                        placeholder = { Text("24AAAAA0000A1Z5", fontSize = 12.sp, color = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = firmName,
                        onValueChange = { firmName = it; errorMessage = null },
                        label = { Text("Firm / Mill Name *", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it; errorMessage = null },
                        label = { Text("Contact Person / Proprietor *", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it; errorMessage = null },
                            label = { Text("Mobile Phone *", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = phone2,
                            onValueChange = { phone2 = it },
                            label = { Text("WhatsApp / Alt Phone", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Location
                    Text("LOCATION & ADDRESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = marketName,
                            onValueChange = { marketName = it },
                            label = { Text("Market / Complex", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = officeAddress,
                        onValueChange = { officeAddress = it },
                        label = { Text("Office / Mill Address", fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tax & Registration
                    Text("TAX & REGISTRATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it },
                            label = { Text("GSTIN", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = panNumber,
                            onValueChange = { panNumber = it },
                            label = { Text("PAN Number", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Verification Photos
                    Text("VERIFICATION PHOTOS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Visiting Card Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { visitingCardLauncher.launch("image/*") }
                        ) {
                            if (visitingCardPhotoUri.isNotBlank()) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = visitingCardPhotoUri,
                                        contentDescription = "Visiting Card",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { visitingCardPhotoUri = "" },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (isUploadingCard) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Visiting Card", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }

                        // Shop / Mill Front Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { shopPhotoLauncher.launch("image/*") }
                        ) {
                            if (shopPhotoUri.isNotBlank()) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = shopPhotoUri,
                                        contentDescription = "Shop Front",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { shopPhotoUri = "" },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (isUploadingShop) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Shop / Mill Front", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // Payment Terms & Notes
                    Text("TERMS & REMARKS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = paymentTermsDays.toString(),
                            onValueChange = { paymentTermsDays = it.toIntOrNull() ?: 30 },
                            label = { Text("Credit Days", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Specialty / Fabrics", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { submit() },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Register Supplier", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
