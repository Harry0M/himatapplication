package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SupplierEntity
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.ui.viewmodel.MasterTab
import com.example.util.MasterConstants
import com.example.util.RecordValidator
import com.example.util.ValidationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMasterScreen(
    viewModel: HimatViewModel,
    onBack: () -> Unit
) {
    val activeTab by viewModel.activeMasterTab.collectAsStateWithLifecycle()
    val editingCustomer by viewModel.editingCustomer.collectAsStateWithLifecycle()
    val editingSupplier by viewModel.editingSupplier.collectAsStateWithLifecycle()
    val editingProduct by viewModel.editingProduct.collectAsStateWithLifecycle()
    val editingEmployee by viewModel.editingEmployee.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()

    val screenTitle = when (activeTab) {
        MasterTab.CUSTOMERS -> if (editingCustomer == null) "New Customer" else "Edit Customer"
        MasterTab.SUPPLIERS -> if (editingSupplier == null) "New Supplier" else "Edit Supplier"
        MasterTab.PRODUCTS -> if (editingProduct == null) "New Product" else "Edit Product"
        MasterTab.EMPLOYEES -> if (editingEmployee == null) "New Staff Member" else "Edit Staff Member"
    }

    val screenSubtitle = when (activeTab) {
        MasterTab.CUSTOMERS -> "Enter retailer profile, shops, 5 phones & Ahmedabad markets"
        MasterTab.SUPPLIERS -> "Register mill / supplier profile, brands, shops & markets"
        MasterTab.PRODUCTS -> "Configure garment product code, supplier mapping & rates"
        MasterTab.EMPLOYEES -> "Manage employee role, contacts, emergency lines & assigned markets"
    }

    // --- State for Customer ---
    var custName by remember(editingCustomer) { mutableStateOf(editingCustomer?.name ?: "") }
    var custFirmName by remember(editingCustomer) { mutableStateOf(editingCustomer?.firmName ?: "") }
    var custId by remember(editingCustomer) { mutableStateOf(editingCustomer?.customerId ?: "CUST-${(100..999).random()}") }
    var custPhone by remember(editingCustomer) { mutableStateOf(editingCustomer?.phone ?: "") }
    var custPhone2 by remember(editingCustomer) { mutableStateOf(editingCustomer?.phone2 ?: "") }
    var custPhone3 by remember(editingCustomer) { mutableStateOf(editingCustomer?.phone3 ?: "") }
    var custPhone4 by remember(editingCustomer) { mutableStateOf(editingCustomer?.phone4 ?: "") }
    var custPhone5 by remember(editingCustomer) { mutableStateOf(editingCustomer?.phone5 ?: "") }
    var custPhoneCount by remember(editingCustomer) {
        val count = listOfNotNull(
            editingCustomer?.phone.takeIf { !it.isNullOrBlank() },
            editingCustomer?.phone2.takeIf { !it.isNullOrBlank() },
            editingCustomer?.phone3.takeIf { !it.isNullOrBlank() },
            editingCustomer?.phone4.takeIf { !it.isNullOrBlank() },
            editingCustomer?.phone5.takeIf { !it.isNullOrBlank() }
        ).size
        mutableStateOf(maxOf(1, count))
    }
    var custEmail by remember(editingCustomer) { mutableStateOf(editingCustomer?.email ?: "") }
    var custEmail2 by remember(editingCustomer) { mutableStateOf(editingCustomer?.email2 ?: "") }
    var custCity by remember(editingCustomer) { mutableStateOf(editingCustomer?.city.takeIf { !it.isNullOrBlank() } ?: "Ahmedabad") }
    var custShopAddress by remember(editingCustomer) { mutableStateOf(editingCustomer?.shopAddress ?: editingCustomer?.address ?: "") }
    var custShopLocation by remember(editingCustomer) { mutableStateOf(editingCustomer?.shopLocation ?: "") }
    var custHomeAddress by remember(editingCustomer) { mutableStateOf(editingCustomer?.homeAddress ?: "") }
    var custPersonalLocation by remember(editingCustomer) { mutableStateOf(editingCustomer?.personalLocation ?: "") }
    var custShopCount by remember(editingCustomer) { mutableStateOf((editingCustomer?.shopCount ?: 1).toString()) }
    var custShopLocations by remember(editingCustomer) { mutableStateOf(editingCustomer?.shopLocations ?: "") }
    var custGstin by remember(editingCustomer) { mutableStateOf(editingCustomer?.gstin ?: "") }
    var custCreditDays by remember(editingCustomer) { mutableStateOf((editingCustomer?.creditDays ?: 30).toString()) }
    var custCreditLimit by remember(editingCustomer) { mutableStateOf(if ((editingCustomer?.creditLimit ?: 0.0) > 0.0) editingCustomer?.creditLimit?.toInt()?.toString() ?: "" else "") }
    var custReferredBy by remember(editingCustomer) { mutableStateOf(editingCustomer?.referredBy ?: "") }
    var custNotes by remember(editingCustomer) { mutableStateOf(editingCustomer?.notes ?: "") }
    var custSelectedMarkets by remember(editingCustomer) {
        val m = editingCustomer?.markets?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: (if (!editingCustomer?.marketArea.isNullOrBlank()) listOf(editingCustomer!!.marketArea) else listOf("Maskati Cloth Market (Sakarkalupur)"))
        mutableStateOf(m.toSet())
    }
    var custPreferredCategories by remember(editingCustomer) {
        val c = editingCustomer?.preferredCategories?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        mutableStateOf(c.toSet())
    }
    var custCustomCategory by remember { mutableStateOf("") }

    // --- State for Supplier ---
    var supName by remember(editingSupplier) { mutableStateOf(editingSupplier?.name ?: "") }
    var supFirmName by remember(editingSupplier) { mutableStateOf(editingSupplier?.firmName ?: "") }
    var supId by remember(editingSupplier) { mutableStateOf(editingSupplier?.supplierId ?: "SUP-${(100..999).random()}") }
    var supType by remember(editingSupplier) { mutableStateOf(editingSupplier?.type ?: "Manufacturer") }
    var supBrand by remember(editingSupplier) { mutableStateOf(editingSupplier?.brand ?: "") }
    var supGstin by remember(editingSupplier) { mutableStateOf(editingSupplier?.gstin ?: "") }
    var supContactPerson by remember(editingSupplier) { mutableStateOf(editingSupplier?.contactPerson ?: "") }
    var supPhone by remember(editingSupplier) { mutableStateOf(editingSupplier?.phone ?: "") }
    var supPhone2 by remember(editingSupplier) { mutableStateOf(editingSupplier?.phone2 ?: "") }
    var supPhone3 by remember(editingSupplier) { mutableStateOf(editingSupplier?.phone3 ?: "") }
    var supPhone4 by remember(editingSupplier) { mutableStateOf(editingSupplier?.phone4 ?: "") }
    var supPhone5 by remember(editingSupplier) { mutableStateOf(editingSupplier?.phone5 ?: "") }
    var supPhoneCount by remember(editingSupplier) {
        val count = listOfNotNull(
            editingSupplier?.phone.takeIf { !it.isNullOrBlank() },
            editingSupplier?.phone2.takeIf { !it.isNullOrBlank() },
            editingSupplier?.phone3.takeIf { !it.isNullOrBlank() },
            editingSupplier?.phone4.takeIf { !it.isNullOrBlank() },
            editingSupplier?.phone5.takeIf { !it.isNullOrBlank() }
        ).size
        mutableStateOf(maxOf(1, count))
    }
    var supEmail by remember(editingSupplier) { mutableStateOf(editingSupplier?.email ?: "") }
    var supEmail2 by remember(editingSupplier) { mutableStateOf(editingSupplier?.email2 ?: "") }
    var supCity by remember(editingSupplier) { mutableStateOf(editingSupplier?.city.takeIf { !it.isNullOrBlank() } ?: "Ahmedabad") }
    var supOfficeAddress by remember(editingSupplier) { mutableStateOf(editingSupplier?.officeAddress ?: editingSupplier?.address ?: "") }
    var supOfficeLocation by remember(editingSupplier) { mutableStateOf(editingSupplier?.officeLocation ?: "") }
    var supHomeAddress by remember(editingSupplier) { mutableStateOf(editingSupplier?.homeAddress ?: "") }
    var supPersonalLocation by remember(editingSupplier) { mutableStateOf(editingSupplier?.personalLocation ?: "") }
    var supShopCount by remember(editingSupplier) { mutableStateOf((editingSupplier?.shopCount ?: 1).toString()) }
    var supShopLocations by remember(editingSupplier) { mutableStateOf(editingSupplier?.shopLocations ?: "") }
    var supDefaultCaseSize by remember(editingSupplier) { mutableStateOf((editingSupplier?.defaultCaseSize ?: 24).toString()) }
    var supReferredBy by remember(editingSupplier) { mutableStateOf(editingSupplier?.referredBy ?: "") }
    var supNotes by remember(editingSupplier) { mutableStateOf(editingSupplier?.notes ?: "") }
    var supSelectedMarkets by remember(editingSupplier) {
        val m = editingSupplier?.markets?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: (if (!editingSupplier?.marketArea.isNullOrBlank()) listOf(editingSupplier!!.marketArea) else listOf("New Cloth Market (Raipur)"))
        mutableStateOf(m.toSet())
    }
    var supCategories by remember(editingSupplier) {
        val c = (editingSupplier?.categories?.takeIf { it.isNotBlank() } ?: editingSupplier?.garmentTypes)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        mutableStateOf(c.toSet())
    }
    var supCustomCategory by remember { mutableStateOf("") }

    // --- State for Product ---
    var prodCode by remember(editingProduct) { mutableStateOf(editingProduct?.productCode ?: "") }
    var prodName by remember(editingProduct) { mutableStateOf(editingProduct?.name ?: "") }
    var prodCategory by remember(editingProduct) { mutableStateOf(editingProduct?.category ?: "Apparel") }
    var prodSupplier by remember(editingProduct, suppliers) {
        mutableStateOf(suppliers.find { it.id == editingProduct?.supplierId } ?: suppliers.firstOrNull())
    }
    var prodSupplierExpanded by remember { mutableStateOf(false) }
    var prodRate by remember(editingProduct) { mutableStateOf(editingProduct?.defaultRate?.toInt()?.toString() ?: "300") }
    var prodCaseSize by remember(editingProduct) { mutableStateOf(editingProduct?.defaultCaseSize?.toString() ?: "24") }
    var prodHsn by remember(editingProduct) { mutableStateOf(editingProduct?.hsnCode ?: "6203") }
    var prodDescription by remember(editingProduct) { mutableStateOf(editingProduct?.description ?: "") }

    // --- State for Employee ---
    var empName by remember(editingEmployee) { mutableStateOf(editingEmployee?.name ?: "") }
    var empId by remember(editingEmployee) { mutableStateOf(editingEmployee?.employeeId ?: "EMP-0${(1..9).random()}") }
    var empRole by remember(editingEmployee) { mutableStateOf(editingEmployee?.role ?: "Salesman") }
    var empPhone by remember(editingEmployee) { mutableStateOf(editingEmployee?.phone ?: "") }
    var empPhone2 by remember(editingEmployee) { mutableStateOf(editingEmployee?.phone2 ?: "") }
    var empPhone3 by remember(editingEmployee) { mutableStateOf(editingEmployee?.phone3 ?: "") }
    var empPhone4 by remember(editingEmployee) { mutableStateOf(editingEmployee?.phone4 ?: "") }
    var empPhone5 by remember(editingEmployee) { mutableStateOf(editingEmployee?.phone5 ?: "") }
    var empPhoneCount by remember(editingEmployee) {
        val count = listOfNotNull(
            editingEmployee?.phone.takeIf { !it.isNullOrBlank() },
            editingEmployee?.phone2.takeIf { !it.isNullOrBlank() },
            editingEmployee?.phone3.takeIf { !it.isNullOrBlank() },
            editingEmployee?.phone4.takeIf { !it.isNullOrBlank() },
            editingEmployee?.phone5.takeIf { !it.isNullOrBlank() }
        ).size
        mutableStateOf(maxOf(1, count))
    }
    var empEmail by remember(editingEmployee) { mutableStateOf(editingEmployee?.email ?: "") }
    var empAlternateEmail by remember(editingEmployee) { mutableStateOf(editingEmployee?.alternateEmail ?: "") }
    var empCurrentAddress by remember(editingEmployee) { mutableStateOf(editingEmployee?.currentAddress ?: editingEmployee?.address ?: "") }
    var empPermanentAddress by remember(editingEmployee) { mutableStateOf(editingEmployee?.permanentAddress ?: "") }
    var empPersonalLocation by remember(editingEmployee) { mutableStateOf(editingEmployee?.personalLocation ?: "") }
    var empEmergencyContactName by remember(editingEmployee) { mutableStateOf(editingEmployee?.emergencyContactName ?: "") }
    var empEmergencyContactPhone by remember(editingEmployee) { mutableStateOf(editingEmployee?.emergencyContactPhone ?: "") }
    var empReferredBy by remember(editingEmployee) { mutableStateOf(editingEmployee?.referredBy ?: "") }
    var empSelectedMarkets by remember(editingEmployee) {
        val m = editingEmployee?.assignedMarkets?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        mutableStateOf(m.toSet())
    }

    // Validation & Error state
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }

    fun handleSave() {
        when (activeTab) {
            MasterTab.CUSTOMERS -> {
                val effectiveName = custName.trim().ifBlank { custFirmName.trim() }
                if (effectiveName.isBlank()) {
                    validationErrors = listOf("Customer or Firm name is required.")
                    return
                }
                val candidate = CustomerEntity(
                    id = editingCustomer?.id ?: 0L,
                    customerId = custId.trim(),
                    name = effectiveName,
                    firmName = custFirmName.trim().ifBlank { effectiveName },
                    phone = custPhone.trim(),
                    phone2 = custPhone2.trim(),
                    phone3 = custPhone3.trim(),
                    phone4 = custPhone4.trim(),
                    phone5 = custPhone5.trim(),
                    email = custEmail.trim(),
                    email2 = custEmail2.trim(),
                    address = custShopAddress.trim(),
                    shopAddress = custShopAddress.trim(),
                    homeAddress = custHomeAddress.trim(),
                    shopLocation = custShopLocation.trim(),
                    personalLocation = custPersonalLocation.trim(),
                    shopCount = custShopCount.toIntOrNull() ?: 1,
                    shopLocations = custShopLocations.trim(),
                    marketArea = custSelectedMarkets.firstOrNull() ?: custCity.trim(),
                    markets = custSelectedMarkets.joinToString(", "),
                    city = custCity.trim(),
                    gstin = custGstin.trim().uppercase(),
                    preferredCategories = (custPreferredCategories + listOfNotNull(custCustomCategory.trim().takeIf { it.isNotBlank() })).joinToString(", "),
                    referredBy = custReferredBy.trim(),
                    creditDays = custCreditDays.toIntOrNull() ?: 30,
                    creditLimit = custCreditLimit.toDoubleOrNull() ?: 0.0,
                    notes = custNotes.trim()
                )
                viewModel.saveCustomer(candidate)
                onBack()
            }
            MasterTab.SUPPLIERS -> {
                val effectiveName = supFirmName.trim().ifBlank { supName.trim() }
                if (effectiveName.isBlank()) {
                    validationErrors = listOf("Supplier / Mill name is required.")
                    return
                }
                val candidate = SupplierEntity(
                    id = editingSupplier?.id ?: 0L,
                    supplierId = supId.trim(),
                    name = effectiveName,
                    firmName = effectiveName,
                    type = supType,
                    brand = supBrand.trim(),
                    gstin = supGstin.trim().uppercase(),
                    address = supOfficeAddress.trim(),
                    officeAddress = supOfficeAddress.trim(),
                    homeAddress = supHomeAddress.trim(),
                    officeLocation = supOfficeLocation.trim(),
                    personalLocation = supPersonalLocation.trim(),
                    shopCount = supShopCount.toIntOrNull() ?: 1,
                    shopLocations = supShopLocations.trim(),
                    city = supCity.trim(),
                    marketArea = supSelectedMarkets.firstOrNull() ?: supCity.trim(),
                    markets = supSelectedMarkets.joinToString(", "),
                    contactPerson = supContactPerson.trim(),
                    phone = supPhone.trim(),
                    phone2 = supPhone2.trim(),
                    phone3 = supPhone3.trim(),
                    phone4 = supPhone4.trim(),
                    phone5 = supPhone5.trim(),
                    email = supEmail.trim(),
                    email2 = supEmail2.trim(),
                    categories = (supCategories + listOfNotNull(supCustomCategory.trim().takeIf { it.isNotBlank() })).joinToString(", "),
                    garmentTypes = (supCategories + listOfNotNull(supCustomCategory.trim().takeIf { it.isNotBlank() })).joinToString(", "),
                    referredBy = supReferredBy.trim(),
                    defaultCaseSize = supDefaultCaseSize.toIntOrNull() ?: 24,
                    notes = supNotes.trim()
                )
                val validation = RecordValidator.validateSupplier(candidate)
                if (validation is ValidationResult.Invalid) {
                    validationErrors = validation.errors
                } else {
                    validationErrors = emptyList()
                    viewModel.saveSupplier(candidate, onSuccess = onBack, onError = { err ->
                        validationErrors = listOf(err)
                    })
                }
            }
            MasterTab.PRODUCTS -> {
                val rateVal = prodRate.toDoubleOrNull() ?: 0.0
                val caseVal = prodCaseSize.toIntOrNull() ?: 0
                val supplierId = prodSupplier?.id ?: 0L
                val supplierName = prodSupplier?.name ?: ""

                val candidate = (editingProduct ?: ProductEntity(
                    productCode = prodCode.trim(),
                    name = prodName.trim(),
                    supplierId = supplierId,
                    supplierName = supplierName,
                    defaultRate = rateVal,
                    defaultCaseSize = caseVal
                )).copy(
                    productCode = prodCode.trim(),
                    name = prodName.trim(),
                    category = prodCategory.trim(),
                    supplierId = supplierId,
                    supplierName = supplierName,
                    defaultRate = rateVal,
                    defaultCaseSize = caseVal,
                    hsnCode = prodHsn.trim(),
                    description = prodDescription.trim()
                )

                val validation = RecordValidator.validateProduct(candidate)
                if (validation is ValidationResult.Invalid) {
                    validationErrors = validation.errors
                } else {
                    validationErrors = emptyList()
                    viewModel.saveProduct(candidate, onSuccess = onBack, onError = { err ->
                        validationErrors = listOf(err)
                    })
                }
            }
            MasterTab.EMPLOYEES -> {
                if (!isSuperAdmin) {
                    validationErrors = listOf("Only Administrators can create or update employee records.")
                    return
                }
                if (empName.isBlank()) {
                    validationErrors = listOf("Employee name is required.")
                    return
                }
                val candidate = EmployeeEntity(
                    id = editingEmployee?.id ?: 0L,
                    employeeId = empId.trim(),
                    name = empName.trim(),
                    phone = empPhone.trim(),
                    phone2 = empPhone2.trim(),
                    phone3 = empPhone3.trim(),
                    phone4 = empPhone4.trim(),
                    phone5 = empPhone5.trim(),
                    role = empRole,
                    email = empEmail.trim().lowercase(),
                    alternateEmail = empAlternateEmail.trim().lowercase(),
                    address = empCurrentAddress.trim(),
                    currentAddress = empCurrentAddress.trim(),
                    permanentAddress = empPermanentAddress.trim(),
                    personalLocation = empPersonalLocation.trim(),
                    emergencyContactName = empEmergencyContactName.trim(),
                    emergencyContactPhone = empEmergencyContactPhone.trim(),
                    referredBy = empReferredBy.trim(),
                    assignedMarkets = empSelectedMarkets.joinToString(", "),
                    markets = empSelectedMarkets.joinToString(", ")
                )
                viewModel.saveEmployee(candidate)
                onBack()
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF6F8FB),
        bottomBar = {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 38.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = { handleSave() },
                        enabled = activeTab != MasterTab.EMPLOYEES || isSuperAdmin,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyPrimary,
                            disabledContainerColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .defaultMinSize(minHeight = 38.dp)
                    ) {
                        Text(
                            text = if (editingCustomer != null || editingSupplier != null || editingProduct != null || editingEmployee != null) "Update Record" else "Save Record",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = GoldAccent
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
                .padding(paddingValues)
        ) {
            val isCompact = maxWidth < 380.dp
            val horizontalPadding = if (isCompact) 10.dp else 12.dp
            val cardSpacing = if (isCompact) 6.dp else 8.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(cardSpacing)
            ) {
                // Top Header (Flat, borderless with circular back button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NavyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = screenTitle,
                            fontSize = if (isCompact) 15.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = screenSubtitle,
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Validation Errors Banner
                if (validationErrors.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Please fix the following issues:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            validationErrors.forEach { err ->
                                Text(
                                    text = "• $err",
                                    fontSize = 12.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }

                // Dynamic Form Content depending on active tab
                when (activeTab) {
                    MasterTab.CUSTOMERS -> {
                        CustomerFormContent(
                            firmName = custFirmName,
                            onFirmNameChange = { custFirmName = it },
                            name = custName,
                            onNameChange = { custName = it },
                            customerId = custId,
                            onCustomerIdChange = { custId = it },
                            phone = custPhone,
                            onPhoneChange = { custPhone = it },
                            phone2 = custPhone2,
                            onPhone2Change = { custPhone2 = it },
                            phone3 = custPhone3,
                            onPhone3Change = { custPhone3 = it },
                            phone4 = custPhone4,
                            onPhone4Change = { custPhone4 = it },
                            phone5 = custPhone5,
                            onPhone5Change = { custPhone5 = it },
                            phoneCount = custPhoneCount,
                            onAddPhone = { custPhoneCount = minOf(5, custPhoneCount + 1) },
                            email = custEmail,
                            onEmailChange = { custEmail = it },
                            email2 = custEmail2,
                            onEmail2Change = { custEmail2 = it },
                            city = custCity,
                            onCityChange = { custCity = it },
                            shopAddress = custShopAddress,
                            onShopAddressChange = { custShopAddress = it },
                            shopLocation = custShopLocation,
                            onShopLocationChange = { custShopLocation = it },
                            homeAddress = custHomeAddress,
                            onHomeAddressChange = { custHomeAddress = it },
                            personalLocation = custPersonalLocation,
                            onPersonalLocationChange = { custPersonalLocation = it },
                            shopCount = custShopCount,
                            onShopCountChange = { custShopCount = it },
                            shopLocations = custShopLocations,
                            onShopLocationsChange = { custShopLocations = it },
                            selectedMarkets = custSelectedMarkets,
                            onToggleMarket = { mkt ->
                                custSelectedMarkets = if (custSelectedMarkets.contains(mkt)) {
                                    custSelectedMarkets - mkt
                                } else {
                                    custSelectedMarkets + mkt
                                }
                            },
                            preferredCategories = custPreferredCategories,
                            onToggleCategory = { cat ->
                                custPreferredCategories = if (custPreferredCategories.contains(cat)) {
                                    custPreferredCategories - cat
                                } else {
                                    custPreferredCategories + cat
                                }
                            },
                            customCategory = custCustomCategory,
                            onCustomCategoryChange = { custCustomCategory = it },
                            gstin = custGstin,
                            onGstinChange = { custGstin = it },
                            creditDays = custCreditDays,
                            onCreditDaysChange = { custCreditDays = it },
                            creditLimit = custCreditLimit,
                            onCreditLimitChange = { custCreditLimit = it },
                            referredBy = custReferredBy,
                            onReferredByChange = { custReferredBy = it },
                            notes = custNotes,
                            onNotesChange = { custNotes = it }
                        )
                    }
                    MasterTab.SUPPLIERS -> {
                        SupplierFormContent(
                            firmName = supFirmName,
                            onFirmNameChange = { supFirmName = it },
                            name = supName,
                            onNameChange = { supName = it },
                            supplierId = supId,
                            onSupplierIdChange = { supId = it },
                            type = supType,
                            onTypeChange = { supType = it },
                            brand = supBrand,
                            onBrandChange = { supBrand = it },
                            contactPerson = supContactPerson,
                            onContactPersonChange = { supContactPerson = it },
                            phone = supPhone,
                            onPhoneChange = { supPhone = it },
                            phone2 = supPhone2,
                            onPhone2Change = { supPhone2 = it },
                            phone3 = supPhone3,
                            onPhone3Change = { supPhone3 = it },
                            phone4 = supPhone4,
                            onPhone4Change = { supPhone4 = it },
                            phone5 = supPhone5,
                            onPhone5Change = { supPhone5 = it },
                            phoneCount = supPhoneCount,
                            onAddPhone = { supPhoneCount = minOf(5, supPhoneCount + 1) },
                            email = supEmail,
                            onEmailChange = { supEmail = it },
                            email2 = supEmail2,
                            onEmail2Change = { supEmail2 = it },
                            officeAddress = supOfficeAddress,
                            onOfficeAddressChange = { supOfficeAddress = it },
                            officeLocation = supOfficeLocation,
                            onOfficeLocationChange = { supOfficeLocation = it },
                            homeAddress = supHomeAddress,
                            onHomeAddressChange = { supHomeAddress = it },
                            personalLocation = supPersonalLocation,
                            onPersonalLocationChange = { supPersonalLocation = it },
                            shopCount = supShopCount,
                            onShopCountChange = { supShopCount = it },
                            shopLocations = supShopLocations,
                            onShopLocationsChange = { supShopLocations = it },
                            selectedMarkets = supSelectedMarkets,
                            onToggleMarket = { mkt ->
                                supSelectedMarkets = if (supSelectedMarkets.contains(mkt)) {
                                    supSelectedMarkets - mkt
                                } else {
                                    supSelectedMarkets + mkt
                                }
                            },
                            city = supCity,
                            onCityChange = { supCity = it },
                            selectedCategories = supCategories,
                            onToggleCategory = { cat ->
                                supCategories = if (supCategories.contains(cat)) {
                                    supCategories - cat
                                } else {
                                    supCategories + cat
                                }
                            },
                            customCategory = supCustomCategory,
                            onCustomCategoryChange = { supCustomCategory = it },
                            gstin = supGstin,
                            onGstinChange = { supGstin = it },
                            defaultCaseSize = supDefaultCaseSize,
                            onDefaultCaseSizeChange = { supDefaultCaseSize = it },
                            referredBy = supReferredBy,
                            onReferredByChange = { supReferredBy = it },
                            notes = supNotes,
                            onNotesChange = { supNotes = it }
                        )
                    }
                    MasterTab.PRODUCTS -> {
                        ProductFormContent(
                            productCode = prodCode,
                            onProductCodeChange = { prodCode = it },
                            name = prodName,
                            onNameChange = { prodName = it },
                            category = prodCategory,
                            onCategoryChange = { prodCategory = it },
                            suppliers = suppliers,
                            selectedSupplier = prodSupplier,
                            onSupplierSelect = { prodSupplier = it },
                            supplierExpanded = prodSupplierExpanded,
                            onExpandedChange = { prodSupplierExpanded = it },
                            defaultRate = prodRate,
                            onDefaultRateChange = { prodRate = it },
                            defaultCaseSize = prodCaseSize,
                            onDefaultCaseSizeChange = { prodCaseSize = it },
                            hsnCode = prodHsn,
                            onHsnCodeChange = { prodHsn = it },
                            description = prodDescription,
                            onDescriptionChange = { prodDescription = it }
                        )
                    }
                    MasterTab.EMPLOYEES -> {
                        if (!isSuperAdmin) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Admin Access Only",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Only Administrators can add or edit staff members. Salesmen can view all staff from the Masters tab.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                        } else {
                            EmployeeFormContent(
                                name = empName,
                                onNameChange = { empName = it },
                                employeeId = empId,
                                onEmployeeIdChange = { empId = it },
                                role = empRole,
                                onRoleChange = { empRole = it },
                                phone = empPhone,
                                onPhoneChange = { empPhone = it },
                                phone2 = empPhone2,
                                onPhone2Change = { empPhone2 = it },
                                phone3 = empPhone3,
                                onPhone3Change = { empPhone3 = it },
                                phone4 = empPhone4,
                                onPhone4Change = { empPhone4 = it },
                                phone5 = empPhone5,
                                onPhone5Change = { empPhone5 = it },
                                phoneCount = empPhoneCount,
                                onAddPhone = { empPhoneCount = minOf(5, empPhoneCount + 1) },
                                email = empEmail,
                                onEmailChange = { empEmail = it },
                                alternateEmail = empAlternateEmail,
                                onAlternateEmailChange = { empAlternateEmail = it },
                                currentAddress = empCurrentAddress,
                                onCurrentAddressChange = { empCurrentAddress = it },
                                permanentAddress = empPermanentAddress,
                                onPermanentAddressChange = { empPermanentAddress = it },
                                personalLocation = empPersonalLocation,
                                onPersonalLocationChange = { empPersonalLocation = it },
                                emergencyContactName = empEmergencyContactName,
                                onEmergencyContactNameChange = { empEmergencyContactName = it },
                                emergencyContactPhone = empEmergencyContactPhone,
                                onEmergencyContactPhoneChange = { empEmergencyContactPhone = it },
                                referredBy = empReferredBy,
                                onReferredByChange = { empReferredBy = it },
                                selectedMarkets = empSelectedMarkets,
                                onToggleMarket = { mkt ->
                                    empSelectedMarkets = if (empSelectedMarkets.contains(mkt)) {
                                        empSelectedMarkets - mkt
                                    } else {
                                        empSelectedMarkets + mkt
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// --- Customer Form Content ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerFormContent(
    firmName: String,
    onFirmNameChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    customerId: String,
    onCustomerIdChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    phone2: String,
    onPhone2Change: (String) -> Unit,
    phone3: String,
    onPhone3Change: (String) -> Unit,
    phone4: String,
    onPhone4Change: (String) -> Unit,
    phone5: String,
    onPhone5Change: (String) -> Unit,
    phoneCount: Int,
    onAddPhone: () -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    email2: String,
    onEmail2Change: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    shopAddress: String,
    onShopAddressChange: (String) -> Unit,
    shopLocation: String,
    onShopLocationChange: (String) -> Unit,
    homeAddress: String,
    onHomeAddressChange: (String) -> Unit,
    personalLocation: String,
    onPersonalLocationChange: (String) -> Unit,
    shopCount: String,
    onShopCountChange: (String) -> Unit,
    shopLocations: String,
    onShopLocationsChange: (String) -> Unit,
    selectedMarkets: Set<String>,
    onToggleMarket: (String) -> Unit,
    preferredCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    customCategory: String,
    onCustomCategoryChange: (String) -> Unit,
    gstin: String,
    onGstinChange: (String) -> Unit,
    creditDays: String,
    onCreditDaysChange: (String) -> Unit,
    creditLimit: String,
    onCreditLimitChange: (String) -> Unit,
    referredBy: String,
    onReferredByChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Card 1: Identity & Firm
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Firm & Retailer Identity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                OutlinedTextField(
                    value = firmName,
                    onValueChange = onFirmNameChange,
                    label = { Text("Shop / Firm Name *") },
                    placeholder = { Text("e.g. Balaji Sarees & Garments") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Proprietor / Owner Name") },
                        placeholder = { Text("e.g. Ramesh Bhai Patel") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = customerId,
                        onValueChange = onCustomerIdChange,
                        label = { Text("Customer ID") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { onGstinChange(it.uppercase()) },
                        label = { Text("GSTIN") },
                        placeholder = { Text("24AAAAA0000A1Z5") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = onCityChange,
                        label = { Text("City / Hub") },
                        placeholder = { Text("Ahmedabad") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }
            }
        }

        // Card 2: Contact Numbers (Up to 5) & Emails
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Contact Numbers (Up to 5)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NavyPrimary
                    )
                    if (phoneCount < 5) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onAddPhone() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Phone", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone 1 (Primary / WhatsApp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                if (phoneCount >= 2) {
                    OutlinedTextField(
                        value = phone2,
                        onValueChange = onPhone2Change,
                        label = { Text("Phone 2 (Shop / Counter Desk)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                if (phoneCount >= 3) {
                    OutlinedTextField(
                        value = phone3,
                        onValueChange = onPhone3Change,
                        label = { Text("Phone 3 (Accountant / Billing)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                if (phoneCount >= 4) {
                    OutlinedTextField(
                        value = phone4,
                        onValueChange = onPhone4Change,
                        label = { Text("Phone 4 (Partner / Manager)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                if (phoneCount >= 5) {
                    OutlinedTextField(
                        value = phone5,
                        onValueChange = onPhone5Change,
                        label = { Text("Phone 5 (Residence / Personal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Primary Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                    OutlinedTextField(
                        value = email2,
                        onValueChange = onEmail2Change,
                        label = { Text("Billing Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }
            }
        }

        // Card 3: Addresses & Shop Outlets
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "3. Addresses & Shop Outlets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                OutlinedTextField(
                    value = shopAddress,
                    onValueChange = onShopAddressChange,
                    label = { Text("Shop / Business Address") },
                    placeholder = { Text("e.g. Shop 24, Ground Floor, Maskati Cloth Market") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = shopLocation,
                    onValueChange = onShopLocationChange,
                    label = { Text("Shop Location (Landmark / Maps link)") },
                    placeholder = { Text("e.g. Near Sakarkalupur Chowki Gate") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = homeAddress,
                    onValueChange = onHomeAddressChange,
                    label = { Text("Home / Residence Address") },
                    placeholder = { Text("e.g. Bungalow 12, Shanti Nagar, Paldi") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = personalLocation,
                    onValueChange = onPersonalLocationChange,
                    label = { Text("Personal Location / Native Town") },
                    placeholder = { Text("e.g. Paldi / Nadiad / Mehsana") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shopCount,
                        onValueChange = onShopCountChange,
                        label = { Text("Shop Outlets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = shopLocations,
                        onValueChange = onShopLocationsChange,
                        label = { Text("Branch / Outlet Locations") },
                        placeholder = { Text("e.g. Maskati & 1 in Bapunagar") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }
            }
        }

        // Card 4: Ahmedabad Textile Markets (Pre-provided)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "4. Ahmedabad Textile Markets (Selectable)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                Text(
                    text = "Tap to associate buyer with Ahmedabad markets:",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MasterConstants.AHMEDABAD_TEXTILE_MARKETS.forEach { mkt ->
                        val isSelected = selectedMarkets.contains(mkt)
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onToggleMarket(mkt) }
                        ) {
                            Text(
                                text = if (isSelected) "✓ $mkt" else "+ $mkt",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Card 5: Preferred Garment Categories & Credit Terms
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "5. Garment Preferences & Credit Terms",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MasterConstants.GARMENT_CATEGORIES.forEach { cat ->
                        val isSelected = preferredCategories.contains(cat)
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onToggleCategory(cat) }
                        ) {
                            Text(
                                text = if (isSelected) "✓ $cat" else "+ $cat",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customCategory,
                    onValueChange = onCustomCategoryChange,
                    label = { Text("+ Custom Fabric / Category") },
                    placeholder = { Text("e.g. Rayon Kurti 14kg, Silk Dupattas") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = creditDays,
                        onValueChange = onCreditDaysChange,
                        label = { Text("Credit Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = creditLimit,
                        onValueChange = onCreditLimitChange,
                        label = { Text("Credit Limit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = referredBy,
                    onValueChange = onReferredByChange,
                    label = { Text("Referred By / Introducer") },
                    placeholder = { Text("e.g. Arvind Bhai / Sunil Verma") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Special Notes / Remarks") },
                    placeholder = { Text("Payment terms, delivery preferences...") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }
        }
    }
}

// --- Supplier Form Content ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupplierFormContent(
    firmName: String,
    onFirmNameChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    supplierId: String,
    onSupplierIdChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    brand: String,
    onBrandChange: (String) -> Unit,
    contactPerson: String,
    onContactPersonChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    phone2: String,
    onPhone2Change: (String) -> Unit,
    phone3: String,
    onPhone3Change: (String) -> Unit,
    phone4: String,
    onPhone4Change: (String) -> Unit,
    phone5: String,
    onPhone5Change: (String) -> Unit,
    phoneCount: Int,
    onAddPhone: () -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    email2: String,
    onEmail2Change: (String) -> Unit,
    officeAddress: String,
    onOfficeAddressChange: (String) -> Unit,
    officeLocation: String,
    onOfficeLocationChange: (String) -> Unit,
    homeAddress: String,
    onHomeAddressChange: (String) -> Unit,
    personalLocation: String,
    onPersonalLocationChange: (String) -> Unit,
    shopCount: String,
    onShopCountChange: (String) -> Unit,
    shopLocations: String,
    onShopLocationsChange: (String) -> Unit,
    selectedMarkets: Set<String>,
    onToggleMarket: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    selectedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    customCategory: String,
    onCustomCategoryChange: (String) -> Unit,
    gstin: String,
    onGstinChange: (String) -> Unit,
    defaultCaseSize: String,
    onDefaultCaseSizeChange: (String) -> Unit,
    referredBy: String,
    onReferredByChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Card 1: Supplier Nature & Firm
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Mill & Supplier Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Manufacturer", "Wholesaler", "Trader").forEach { t ->
                        val isSelected = type.equals(t, ignoreCase = true)
                        Surface(
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTypeChange(t) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when (t) {
                                        "Manufacturer" -> "🏭 Mill"
                                        "Wholesaler" -> "🏪 Whole"
                                        else -> "🤝 Trader"
                                    },
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = firmName,
                    onValueChange = {
                        onFirmNameChange(it)
                        onNameChange(it)
                    },
                    label = { Text("Firm / Mill Name *") },
                    placeholder = { Text("e.g. Radheshyam Textile Mills Pvt Ltd") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = onBrandChange,
                        label = { Text("Brand Name") },
                        placeholder = { Text("e.g. RADHE") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = onContactPersonChange,
                        label = { Text("Key Contact Person") },
                        placeholder = { Text("e.g. Arvind Bhai") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { onGstinChange(it.uppercase()) },
                        label = { Text("GSTIN") },
                        placeholder = { Text("24AAAAA0000A1Z5") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = defaultCaseSize,
                        onValueChange = onDefaultCaseSizeChange,
                        label = { Text("Case Size (Pcs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }
            }
        }

        // Card 2: Contact Numbers (Up to 5) & Emails
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Contact Numbers (Up to 5)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NavyPrimary
                    )
                    if (phoneCount < 5) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onAddPhone() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Phone", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone 1 (Primary / WhatsApp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                if (phoneCount >= 2) {
                    OutlinedTextField(
                        value = phone2,
                        onValueChange = onPhone2Change,
                        label = { Text("Phone 2 (Office / Order Desk)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                if (phoneCount >= 3) {
                    OutlinedTextField(
                        value = phone3,
                        onValueChange = onPhone3Change,
                        label = { Text("Phone 3 (Accountant / Billing)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                if (phoneCount >= 4) {
                    OutlinedTextField(
                        value = phone4,
                        onValueChange = onPhone4Change,
                        label = { Text("Phone 4 (Godown / Dispatch)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                if (phoneCount >= 5) {
                    OutlinedTextField(
                        value = phone5,
                        onValueChange = onPhone5Change,
                        label = { Text("Phone 5 (Residence / Personal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Primary Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                    OutlinedTextField(
                        value = email2,
                        onValueChange = onEmail2Change,
                        label = { Text("Accounts Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }
            }
        }

        // Card 3: Addresses & Shops
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "3. Addresses & Shop Locations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                OutlinedTextField(
                    value = officeAddress,
                    onValueChange = onOfficeAddressChange,
                    label = { Text("Office / Mill Address") },
                    placeholder = { Text("e.g. 3rd Floor, Radha Krishna Complex, Ring Road") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = officeLocation,
                    onValueChange = onOfficeLocationChange,
                    label = { Text("Office Location (Landmark / Maps link)") },
                    placeholder = { Text("e.g. Near Kalupur Overbridge Gate") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = homeAddress,
                    onValueChange = onHomeAddressChange,
                    label = { Text("Home / Factory Address") },
                    placeholder = { Text("e.g. Plot 44, Narol GIDC Phase 2") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = personalLocation,
                    onValueChange = onPersonalLocationChange,
                    label = { Text("Personal Location / Residence Area") },
                    placeholder = { Text("e.g. Bodakdev / Paldi / Maninagar") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shopCount,
                        onValueChange = onShopCountChange,
                        label = { Text("Shops Owned") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = shopLocations,
                        onValueChange = onShopLocationsChange,
                        label = { Text("Branch / Shop Locations") },
                        placeholder = { Text("e.g. Shop 12 New Cloth Mkt, Shop 45 Maskati") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }
            }
        }

        // Card 4: Ahmedabad Textile Markets
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "4. Ahmedabad Textile Markets (Selectable)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MasterConstants.AHMEDABAD_TEXTILE_MARKETS.forEach { mkt ->
                        val isSelected = selectedMarkets.contains(mkt)
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onToggleMarket(mkt) }
                        ) {
                            Text(
                                text = if (isSelected) "✓ $mkt" else "+ $mkt",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Card 5: Products & Garment Categories
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "5. Garment Categories Sold",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MasterConstants.GARMENT_CATEGORIES.forEach { cat ->
                        val isSelected = selectedCategories.contains(cat)
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onToggleCategory(cat) }
                        ) {
                            Text(
                                text = if (isSelected) "✓ $cat" else "+ $cat",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customCategory,
                    onValueChange = onCustomCategoryChange,
                    label = { Text("+ Custom Fabric / Garment Category") },
                    placeholder = { Text("e.g. Pure Cotton Shirting, Denim Lycra") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = referredBy,
                        onValueChange = onReferredByChange,
                        label = { Text("Referred By") },
                        placeholder = { Text("e.g. Ramesh Bhai") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = onCityChange,
                        label = { Text("City") },
                        placeholder = { Text("Ahmedabad") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Special Terms / Notes") },
                    placeholder = { Text("e.g. 5% cash discount on 7 days") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }
        }
    }
}

// --- Product Form Content ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormContent(
    productCode: String,
    onProductCodeChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    suppliers: List<SupplierEntity>,
    selectedSupplier: SupplierEntity?,
    onSupplierSelect: (SupplierEntity) -> Unit,
    supplierExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    defaultRate: String,
    onDefaultRateChange: (String) -> Unit,
    defaultCaseSize: String,
    onDefaultCaseSizeChange: (String) -> Unit,
    hsnCode: String,
    onHsnCodeChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Product Specifications",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = NavyPrimary
            )

            OutlinedTextField(
                value = productCode,
                onValueChange = { onProductCodeChange(it.uppercase()) },
                label = { Text("Product / Item Code *") },
                placeholder = { Text("e.g. DENIM-701, COT-SHIRT") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Product Name *") },
                placeholder = { Text("e.g. Slim Fit Denim 701") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            // Supplier Dropdown
            ExposedDropdownMenuBox(
                expanded = supplierExpanded,
                onExpandedChange = onExpandedChange,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedSupplier?.let { "${it.name} (${it.type})" } ?: "Select Supplier",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Supplier *") },
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                    colors = defaultTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = supplierExpanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    suppliers.forEach { sup ->
                        DropdownMenuItem(
                            text = { Text("${sup.name} (${sup.type})") },
                            onClick = {
                                onSupplierSelect(sup)
                                onExpandedChange(false)
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = onCategoryChange,
                    label = { Text("Category") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = defaultTextFieldColors()
                )
                OutlinedTextField(
                    value = hsnCode,
                    onValueChange = onHsnCodeChange,
                    label = { Text("HSN Code") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = defaultTextFieldColors()
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = defaultRate,
                    onValueChange = onDefaultRateChange,
                    label = { Text("Default Rate (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = defaultTextFieldColors()
                )
                OutlinedTextField(
                    value = defaultCaseSize,
                    onValueChange = onDefaultCaseSizeChange,
                    label = { Text("Case Size (pcs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = defaultTextFieldColors()
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description / Notes") },
                placeholder = { Text("Fabric quality, wash details, packaging notes...") },
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )
        }
    }
}

// --- Employee Form Content ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployeeFormContent(
    name: String,
    onNameChange: (String) -> Unit,
    employeeId: String,
    onEmployeeIdChange: (String) -> Unit,
    role: String,
    onRoleChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    phone2: String,
    onPhone2Change: (String) -> Unit,
    phone3: String,
    onPhone3Change: (String) -> Unit,
    phone4: String,
    onPhone4Change: (String) -> Unit,
    phone5: String,
    onPhone5Change: (String) -> Unit,
    phoneCount: Int,
    onAddPhone: () -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    alternateEmail: String,
    onAlternateEmailChange: (String) -> Unit,
    currentAddress: String,
    onCurrentAddressChange: (String) -> Unit,
    permanentAddress: String,
    onPermanentAddressChange: (String) -> Unit,
    personalLocation: String,
    onPersonalLocationChange: (String) -> Unit,
    emergencyContactName: String,
    onEmergencyContactNameChange: (String) -> Unit,
    emergencyContactPhone: String,
    onEmergencyContactPhoneChange: (String) -> Unit,
    referredBy: String,
    onReferredByChange: (String) -> Unit,
    selectedMarkets: Set<String>,
    onToggleMarket: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Card 1: Identity & Role
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Staff Identity & Role",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Sunil Verma") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = employeeId,
                        onValueChange = onEmployeeIdChange,
                        label = { Text("Staff ID") },
                        placeholder = { Text("EMP-02") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    val isSalesman = role == "Salesman"
                    Surface(
                        color = if (isSalesman) NavyPrimary else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isSalesman) NavyPrimary else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onRoleChange(if (isSalesman) "Admin" else "Salesman") }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isSalesman) "🚶 Salesman" else "👑 Admin",
                                color = if (isSalesman) Color.White else NavyPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = "Tap to switch role",
                                color = if (isSalesman) GoldAccent else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Google Account Email (for App Login)") },
                    placeholder = { Text("e.g. sunil.salesman@gmail.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = alternateEmail,
                    onValueChange = onAlternateEmailChange,
                    label = { Text("Alternate / Personal Email") },
                    placeholder = { Text("e.g. sunil.personal@outlook.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }
        }

        // Card 2: Contact Numbers (Up to 5)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Contact Numbers (Up to 5)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NavyPrimary
                    )
                    if (phoneCount < 5) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onAddPhone() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Phone", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone 1 (Primary / Calling) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                if (phoneCount >= 2) {
                    OutlinedTextField(
                        value = phone2,
                        onValueChange = onPhone2Change,
                        label = { Text("Phone 2 (Alternate Calling)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                if (phoneCount >= 3) {
                    OutlinedTextField(
                        value = phone3,
                        onValueChange = onPhone3Change,
                        label = { Text("Phone 3 (Family / Home)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                if (phoneCount >= 4) {
                    OutlinedTextField(
                        value = phone4,
                        onValueChange = onPhone4Change,
                        label = { Text("Phone 4 (Emergency Line)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                if (phoneCount >= 5) {
                    OutlinedTextField(
                        value = phone5,
                        onValueChange = onPhone5Change,
                        label = { Text("Phone 5 (Other)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }
            }
        }

        // Card 3: Addresses & Native Town
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "3. Addresses & Native Place",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                OutlinedTextField(
                    value = currentAddress,
                    onValueChange = onCurrentAddressChange,
                    label = { Text("Current Residence Address (Ahmedabad)") },
                    placeholder = { Text("e.g. Flat 302, Royal Residency, Vastral") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = permanentAddress,
                    onValueChange = onPermanentAddressChange,
                    label = { Text("Permanent / Home Town Address") },
                    placeholder = { Text("e.g. Village Mandvi, Taluka Bhuj, Kutch") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = personalLocation,
                    onValueChange = onPersonalLocationChange,
                    label = { Text("Personal Location / Native Town") },
                    placeholder = { Text("e.g. Vastral / Maninagar / Kutch") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }
        }

        // Card 4: Emergency Contacts & Assigned Ahmedabad Markets
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "4. Emergency & Assigned Markets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NavyPrimary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emergencyContactName,
                        onValueChange = onEmergencyContactNameChange,
                        label = { Text("Emergency Contact") },
                        placeholder = { Text("e.g. Rajesh (Brother)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = emergencyContactPhone,
                        onValueChange = onEmergencyContactPhoneChange,
                        label = { Text("Emergency Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = referredBy,
                    onValueChange = onReferredByChange,
                    label = { Text("Referred By / Reference") },
                    placeholder = { Text("e.g. Paresh Bhai (Radhe Mills)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assigned Ahmedabad Textile Markets:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyPrimary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MasterConstants.AHMEDABAD_TEXTILE_MARKETS.forEach { mkt ->
                        val isSelected = selectedMarkets.contains(mkt)
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onToggleMarket(mkt) }
                        ) {
                            Text(
                                text = if (isSelected) "✓ $mkt" else "+ $mkt",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun defaultTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = NavyPrimary,
    unfocusedBorderColor = Color(0xFFE2E8F0)
)
