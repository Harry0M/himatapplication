package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.data.remote.FirebaseStorageService
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel
import com.example.ui.viewmodel.MasterTab
import com.example.util.MasterConstants
import com.example.util.RecordValidator
import com.example.util.ValidationResult
import org.json.JSONArray
import org.json.JSONObject

// -------------------------------------------------------------
// Helper Data Classes for Dynamic Lists
// -------------------------------------------------------------

data class MasterContact(
    val name: String = "",
    val phone: String = "",
    val designation: String = ""
)

data class MasterLocation(
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val mapLink: String = ""
)

private fun parseContactsJson(json: String, fallbackPhones: List<String>): List<MasterContact> {
    if (json.isNotBlank() && json != "[]") {
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<MasterContact>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MasterContact(
                        name = obj.optString("name", ""),
                        phone = obj.optString("phone", ""),
                        designation = obj.optString("designation", "")
                    )
                )
            }
            if (list.isNotEmpty()) return list
        } catch (_: Exception) {}
    }
    val filtered = fallbackPhones.filter { it.isNotBlank() }
    if (filtered.isNotEmpty()) {
        return filtered.mapIndexed { idx, p ->
            MasterContact(
                name = if (idx == 0) "Primary" else "Contact ${idx + 1}",
                phone = p,
                designation = if (idx == 0) "Owner / Desk" else "Office"
            )
        }
    }
    return listOf(MasterContact(name = "Primary", phone = "", designation = "Owner / Desk"))
}

private fun contactsToJson(list: List<MasterContact>): String {
    val arr = JSONArray()
    for (c in list) {
        if (c.phone.isNotBlank() || c.name.isNotBlank()) {
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            obj.put("designation", c.designation)
            arr.put(obj)
        }
    }
    return arr.toString()
}

private fun parseLocationsJson(json: String, fallbackName: String, fallbackAddress: String, fallbackMapLink: String): List<MasterLocation> {
    if (json.isNotBlank() && json != "[]") {
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<MasterLocation>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MasterLocation(
                        name = obj.optString("name", ""),
                        address = obj.optString("address", ""),
                        city = obj.optString("city", ""),
                        mapLink = obj.optString("mapLink", "")
                    )
                )
            }
            if (list.isNotEmpty()) return list
        } catch (_: Exception) {}
    }
    if (fallbackAddress.isNotBlank()) {
        return listOf(MasterLocation(name = fallbackName, address = fallbackAddress, mapLink = fallbackMapLink))
    }
    return listOf(MasterLocation(name = fallbackName))
}

private fun locationsToJson(list: List<MasterLocation>): String {
    val arr = JSONArray()
    for (loc in list) {
        if (loc.address.isNotBlank() || loc.name.isNotBlank() || loc.mapLink.isNotBlank()) {
            val obj = JSONObject()
            obj.put("name", loc.name)
            obj.put("address", loc.address)
            obj.put("city", loc.city)
            obj.put("mapLink", loc.mapLink)
            arr.put(obj)
        }
    }
    return arr.toString()
}

// -------------------------------------------------------------
// Main AddEditMasterScreen
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMasterScreen(
    viewModel: HimatViewModel,
    onBack: () -> Unit
) {
    val activeTab by viewModel.activeMasterTab.collectAsStateWithLifecycle()
    val editingCustomer by viewModel.editingCustomer.collectAsStateWithLifecycle()
    val editingSupplier by viewModel.editingSupplier.collectAsStateWithLifecycle()
    val editingBrand by viewModel.editingBrand.collectAsStateWithLifecycle()
    val editingTransporter by viewModel.editingTransporter.collectAsStateWithLifecycle()
    val editingMarket by viewModel.editingMarket.collectAsStateWithLifecycle()
    val editingProduct by viewModel.editingProduct.collectAsStateWithLifecycle()
    val editingEmployee by viewModel.editingEmployee.collectAsStateWithLifecycle()

    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val brands by viewModel.allBrands.collectAsStateWithLifecycle()
    val transporters by viewModel.allTransporters.collectAsStateWithLifecycle()
    val markets by viewModel.allMarkets.collectAsStateWithLifecycle()
    val employees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val currentEmployee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()

    val screenTitle = when (activeTab) {
        MasterTab.CUSTOMERS -> if (editingCustomer == null) "New Customer" else "Edit Customer"
        MasterTab.SUPPLIERS -> if (editingSupplier == null) "New Supplier" else "Edit Supplier"
        MasterTab.BRANDS -> if (editingBrand == null) "New Brand" else "Edit Brand"
        MasterTab.TRANSPORTERS -> if (editingTransporter == null) "New Transporter" else "Edit Transporter"
        MasterTab.MARKETS -> if (editingMarket == null) "New Market" else "Edit Market"
        MasterTab.PRODUCTS -> if (editingProduct == null) "New Product" else "Edit Product"
        MasterTab.EMPLOYEES -> if (editingEmployee == null) "New Staff Member" else "Edit Staff Member"
    }

    val screenSubtitle = when (activeTab) {
        MasterTab.CUSTOMERS -> "Retailer CRM: contacts, 5 outlets, KYC docs & credit terms"
        MasterTab.SUPPLIERS -> "Mill / manufacturer profile, market, brand & factory units"
        MasterTab.BRANDS -> "Brand identity, manufacturer link, logo & garment category"
        MasterTab.TRANSPORTERS -> "Logistics partner, godown hub, contact lines & route coverage"
        MasterTab.MARKETS -> "Textile market cluster, trade area & landmark in city hub"
        MasterTab.PRODUCTS -> "Configure garment product code, supplier mapping & rates"
        MasterTab.EMPLOYEES -> "Manage staff role, contacts, emergency lines & assigned markets"
    }

    // =========================================================================
    // CUSTOMER STATE
    // =========================================================================
    var custFirmName by remember(editingCustomer) { mutableStateOf(editingCustomer?.firmName ?: "") }
    var custName by remember(editingCustomer) { mutableStateOf(editingCustomer?.name ?: "") }
    var custId by remember(editingCustomer) { mutableStateOf(editingCustomer?.customerId ?: "CUST-${(100..999).random()}") }
    var custGstin by remember(editingCustomer) { mutableStateOf(editingCustomer?.gstin ?: "") }
    var custPanNumber by remember(editingCustomer) { mutableStateOf(editingCustomer?.panNumber ?: "") }
    var custCity by remember(editingCustomer) { mutableStateOf(editingCustomer?.city.takeIf { !it.isNullOrBlank() } ?: "Ahmedabad") }
    var custDistrict by remember(editingCustomer) { mutableStateOf(editingCustomer?.district ?: "Ahmedabad") }
    var custState by remember(editingCustomer) { mutableStateOf(editingCustomer?.state ?: "Gujarat") }
    var custPincode by remember(editingCustomer) { mutableStateOf(editingCustomer?.pincode ?: "") }
    var custCustomerType by remember(editingCustomer) { mutableStateOf(editingCustomer?.customerType ?: "Credit") }

    // Contacts up to 5
    var custContacts by remember(editingCustomer) {
        val phones = listOfNotNull(
            editingCustomer?.phone,
            editingCustomer?.phone2,
            editingCustomer?.phone3,
            editingCustomer?.phone4,
            editingCustomer?.phone5
        )
        mutableStateOf(parseContactsJson(editingCustomer?.contactsJson ?: "", phones))
    }

    // Outlets up to 5 (with shop map links)
    var custOutlets by remember(editingCustomer) {
        mutableStateOf(
            parseLocationsJson(
                json = editingCustomer?.outletsJson ?: "",
                fallbackName = "Main Shop / Showroom",
                fallbackAddress = editingCustomer?.shopAddress ?: editingCustomer?.address ?: "",
                fallbackMapLink = editingCustomer?.shopMapLink ?: editingCustomer?.shopLocation ?: ""
            )
        )
    }

    // Garment types dealt with mostly
    var custGarmentTypes by remember(editingCustomer) {
        val types = (editingCustomer?.garmentTypes.takeIf { !it.isNullOrBlank() }
            ?: editingCustomer?.preferredCategories.takeIf { !it.isNullOrBlank() }
            ?: "").split(",").map { it.trim() }.filter { it.isNotBlank() }
        mutableStateOf(types.toSet())
    }
    var custCustomGarmentType by remember { mutableStateOf("") }

    // Referred By & Creator Agent
    var custReferredBy by remember(editingCustomer) { mutableStateOf(editingCustomer?.referredBy ?: "") }
    var custAddedByAgentId by remember(editingCustomer, currentEmployee) {
        mutableStateOf(editingCustomer?.addedByAgentId ?: currentEmployee?.id)
    }
    var custAddedByAgentName by remember(editingCustomer, currentEmployee) {
        mutableStateOf(editingCustomer?.addedByAgentName ?: currentEmployee?.name ?: "Sales Agent")
    }

    // Preferred Transporter
    var custPreferredTransporterId by remember(editingCustomer) { mutableStateOf(editingCustomer?.preferredTransporterId) }
    var custPreferredTransporterName by remember(editingCustomer) { mutableStateOf(editingCustomer?.preferredTransporterName ?: "") }
    var custTransportPreference by remember(editingCustomer) { mutableStateOf(editingCustomer?.transportPreference ?: "") }

    // Personal & KYC
    var custDob by remember(editingCustomer) { mutableStateOf(editingCustomer?.dob ?: "") }
    var custReligion by remember(editingCustomer) { mutableStateOf(editingCustomer?.religion ?: "") }
    var custHomeAddress by remember(editingCustomer) { mutableStateOf(editingCustomer?.homeAddress ?: "") }
    var custPersonalLocation by remember(editingCustomer) { mutableStateOf(editingCustomer?.personalLocation ?: "") }
    var custAadharUri by remember(editingCustomer) { mutableStateOf(editingCustomer?.aadharPhotoUri ?: "") }
    var custGstCertUri by remember(editingCustomer) { mutableStateOf(editingCustomer?.gstCertPhotoUri ?: "") }
    var custPanUri by remember(editingCustomer) { mutableStateOf(editingCustomer?.panPhotoUri ?: "") }
    var custShopPicUri by remember(editingCustomer) { mutableStateOf(editingCustomer?.shopPhotoUri ?: "") }
    var custPurchaserPicUri by remember(editingCustomer) { mutableStateOf(editingCustomer?.purchaserPhotoUri ?: "") }
    var custCancelChequeUri by remember(editingCustomer) { mutableStateOf(editingCustomer?.cancelChequePhotoUri ?: "") }

    var custCreditDays by remember(editingCustomer) { mutableStateOf((editingCustomer?.creditDays ?: 30).toString()) }
    var custCreditLimit by remember(editingCustomer) { mutableStateOf(if ((editingCustomer?.creditLimit ?: 0.0) > 0.0) editingCustomer?.creditLimit?.toInt()?.toString() ?: "" else "") }
    var custEmail by remember(editingCustomer) { mutableStateOf(editingCustomer?.email ?: "") }
    var custEmail2 by remember(editingCustomer) { mutableStateOf(editingCustomer?.email2 ?: "") }
    var custNotes by remember(editingCustomer) { mutableStateOf(editingCustomer?.notes ?: "") }

    // =========================================================================
    // SUPPLIER STATE
    // =========================================================================
    var supFirmName by remember(editingSupplier) { mutableStateOf(editingSupplier?.firmName ?: "") }
    var supContactPerson by remember(editingSupplier) { mutableStateOf(editingSupplier?.contactPerson ?: editingSupplier?.name ?: "") }
    var supId by remember(editingSupplier) { mutableStateOf(editingSupplier?.supplierId ?: "SUP-${(100..999).random()}") }
    var supType by remember(editingSupplier) { mutableStateOf(editingSupplier?.type ?: "Manufacturer") }
    var supGstin by remember(editingSupplier) { mutableStateOf(editingSupplier?.gstin ?: "") }
    var supPanNumber by remember(editingSupplier) { mutableStateOf(editingSupplier?.panNumber ?: "") }
    var supCity by remember(editingSupplier) { mutableStateOf(editingSupplier?.city.takeIf { !it.isNullOrBlank() } ?: "Ahmedabad") }

    // Market selection with inline create
    var supMarketId by remember(editingSupplier) { mutableStateOf(editingSupplier?.marketId) }
    var supMarketName by remember(editingSupplier) { mutableStateOf(editingSupplier?.marketName.takeIf { !it.isNullOrBlank() } ?: editingSupplier?.marketArea ?: "") }

    // Brand selection with inline create
    var supBrandId by remember(editingSupplier) { mutableStateOf(editingSupplier?.brandId) }
    var supBrandName by remember(editingSupplier) { mutableStateOf(editingSupplier?.brand ?: "") }

    // Contacts up to 5
    var supContacts by remember(editingSupplier) {
        val phones = listOfNotNull(
            editingSupplier?.phone,
            editingSupplier?.phone2,
            editingSupplier?.phone3,
            editingSupplier?.phone4,
            editingSupplier?.phone5
        )
        mutableStateOf(parseContactsJson("", phones))
    }

    // Manufacturing / Products made & Price range
    var supProductsMade by remember(editingSupplier) { mutableStateOf(editingSupplier?.productsMade ?: "") }
    var supPriceRange by remember(editingSupplier) { mutableStateOf(editingSupplier?.priceRange ?: "") }
    var supCategories by remember(editingSupplier) {
        val c = (editingSupplier?.categories.takeIf { !it.isNullOrBlank() } ?: editingSupplier?.garmentTypes ?: "")
            .split(",").map { it.trim() }.filter { it.isNotBlank() }
        mutableStateOf(c.toSet())
    }
    var supCustomCategory by remember { mutableStateOf("") }

    // Factories up to 5
    var supFactories by remember(editingSupplier) {
        mutableStateOf(
            parseLocationsJson(
                json = editingSupplier?.factoriesJson ?: "",
                fallbackName = "Factory Unit 1",
                fallbackAddress = editingSupplier?.homeAddress ?: "",
                fallbackMapLink = ""
            )
        )
    }

    // Outlets up to 5
    var supOutlets by remember(editingSupplier) {
        mutableStateOf(
            parseLocationsJson(
                json = editingSupplier?.outletsJson ?: "",
                fallbackName = "Mill Showroom / Office",
                fallbackAddress = editingSupplier?.officeAddress ?: editingSupplier?.address ?: "",
                fallbackMapLink = editingSupplier?.officeLocation ?: ""
            )
        )
    }

    // Photos
    var supShopPhotoUri by remember(editingSupplier) { mutableStateOf(editingSupplier?.shopPhotoUri ?: "") }
    var supVisitingCardPhotoUri by remember(editingSupplier) { mutableStateOf(editingSupplier?.visitingCardPhotoUri ?: "") }

    var supOfficeAddress by remember(editingSupplier) { mutableStateOf(editingSupplier?.officeAddress ?: editingSupplier?.address ?: "") }
    var supOfficeLocation by remember(editingSupplier) { mutableStateOf(editingSupplier?.officeLocation ?: "") }
    var supHomeAddress by remember(editingSupplier) { mutableStateOf(editingSupplier?.homeAddress ?: "") }
    var supPersonalLocation by remember(editingSupplier) { mutableStateOf(editingSupplier?.personalLocation ?: "") }
    var supEmail by remember(editingSupplier) { mutableStateOf(editingSupplier?.email ?: "") }
    var supEmail2 by remember(editingSupplier) { mutableStateOf(editingSupplier?.email2 ?: "") }
    var supReferredBy by remember(editingSupplier) { mutableStateOf(editingSupplier?.referredBy ?: "") }
    var supNotes by remember(editingSupplier) { mutableStateOf(editingSupplier?.notes ?: "") }

    // =========================================================================
    // BRAND STATE
    // =========================================================================
    var brandName by remember(editingBrand) { mutableStateOf(editingBrand?.brandName ?: "") }
    var brandManufacturerId by remember(editingBrand) { mutableStateOf(editingBrand?.manufacturerId) }
    var brandManufacturerName by remember(editingBrand) { mutableStateOf(editingBrand?.manufacturerName ?: "") }
    var brandCategory by remember(editingBrand) { mutableStateOf(editingBrand?.category ?: "Apparel") }
    var brandLogoUri by remember(editingBrand) { mutableStateOf(editingBrand?.logoPhotoUri ?: "") }
    var brandDescription by remember(editingBrand) { mutableStateOf(editingBrand?.description ?: "") }
    var brandIsActive by remember(editingBrand) { mutableStateOf(editingBrand?.isActive ?: true) }

    // =========================================================================
    // TRANSPORTER STATE
    // =========================================================================
    var transName by remember(editingTransporter) { mutableStateOf(editingTransporter?.transporterName ?: "") }
    var transContactPerson by remember(editingTransporter) { mutableStateOf(editingTransporter?.contactPerson ?: "") }
    var transPhone1 by remember(editingTransporter) { mutableStateOf(editingTransporter?.phone1 ?: "") }
    var transPhone2 by remember(editingTransporter) { mutableStateOf(editingTransporter?.phone2 ?: "") }
    var transPhone3 by remember(editingTransporter) { mutableStateOf(editingTransporter?.phone3 ?: "") }
    var transOfficeAddress by remember(editingTransporter) { mutableStateOf(editingTransporter?.officeAddress ?: "") }
    var transGodownAddress by remember(editingTransporter) { mutableStateOf(editingTransporter?.godownAddress ?: "") }
    var transCity by remember(editingTransporter) { mutableStateOf(editingTransporter?.city ?: "Ahmedabad") }
    var transDestinations by remember(editingTransporter) { mutableStateOf(editingTransporter?.destinationsCovered ?: "") }
    var transGstin by remember(editingTransporter) { mutableStateOf(editingTransporter?.gstin ?: "") }
    var transTrackingUrl by remember(editingTransporter) { mutableStateOf(editingTransporter?.trackingUrl ?: "") }
    var transNotes by remember(editingTransporter) { mutableStateOf(editingTransporter?.notes ?: "") }

    // =========================================================================
    // MARKET STATE
    // =========================================================================
    var mktName by remember(editingMarket) { mutableStateOf(editingMarket?.marketName ?: "") }
    var mktCity by remember(editingMarket) { mutableStateOf(editingMarket?.city ?: "Ahmedabad") }
    var mktArea by remember(editingMarket) { mutableStateOf(editingMarket?.area ?: "") }
    var mktLandmark by remember(editingMarket) { mutableStateOf(editingMarket?.landmark ?: "") }
    var mktPincode by remember(editingMarket) { mutableStateOf(editingMarket?.pincode ?: "") }
    var mktType by remember(editingMarket) { mutableStateOf(editingMarket?.marketType ?: "Wholesale") }
    var mktDescription by remember(editingMarket) { mutableStateOf(editingMarket?.description ?: "") }

    // =========================================================================
    // PRODUCT STATE
    // =========================================================================
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

    // =========================================================================
    // EMPLOYEE STATE
    // =========================================================================
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

    // Inline dialog states for Supplier form
    var showInlineMarketDialog by remember { mutableStateOf(false) }
    var showInlineBrandDialog by remember { mutableStateOf(false) }

    // Validation
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }

    fun handleSave() {
        when (activeTab) {
            MasterTab.CUSTOMERS -> {
                val effectiveFirmName = custFirmName.trim()
                val effectiveOwnerName = custName.trim()
                if (effectiveFirmName.isBlank() && effectiveOwnerName.isBlank()) {
                    validationErrors = listOf("Shop / Firm Name or Owner Name is required.")
                    return
                }

                val allGarments = (custGarmentTypes + listOfNotNull(custCustomGarmentType.trim().takeIf { it.isNotBlank() })).joinToString(", ")
                val primaryContactPhone = custContacts.firstOrNull { it.phone.isNotBlank() }?.phone?.trim() ?: ""
                val primaryOutlet = custOutlets.firstOrNull()

                val candidate = CustomerEntity(
                    id = editingCustomer?.id ?: 0L,
                    customerId = custId.trim(),
                    name = effectiveOwnerName.ifBlank { effectiveFirmName },
                    firmName = effectiveFirmName.ifBlank { effectiveOwnerName },
                    phone = primaryContactPhone,
                    phone2 = custContacts.getOrNull(1)?.phone?.trim() ?: "",
                    phone3 = custContacts.getOrNull(2)?.phone?.trim() ?: "",
                    phone4 = custContacts.getOrNull(3)?.phone?.trim() ?: "",
                    phone5 = custContacts.getOrNull(4)?.phone?.trim() ?: "",
                    email = custEmail.trim(),
                    email2 = custEmail2.trim(),
                    address = primaryOutlet?.address?.trim() ?: "",
                    shopAddress = primaryOutlet?.address?.trim() ?: "",
                    homeAddress = custHomeAddress.trim(),
                    shopLocation = primaryOutlet?.mapLink?.trim() ?: "",
                    personalLocation = custPersonalLocation.trim(),
                    shopCount = maxOf(1, custOutlets.size),
                    shopLocations = custOutlets.joinToString("; ") { "${it.name}: ${it.address}" },
                    marketArea = custCity.trim(), // NO customer market selection!
                    markets = "",                  // NO customer market selection!
                    city = custCity.trim(),
                    district = custDistrict.trim(),
                    state = custState.trim(),
                    pincode = custPincode.trim(),
                    shopMapLink = primaryOutlet?.mapLink?.trim() ?: "",
                    gstin = custGstin.trim().uppercase(),
                    panNumber = custPanNumber.trim().uppercase(),
                    customerType = custCustomerType,
                    contactsJson = contactsToJson(custContacts),
                    outletsJson = locationsToJson(custOutlets),
                    garmentTypes = allGarments,
                    preferredCategories = allGarments,
                    addedByAgentId = custAddedByAgentId,
                    addedByAgentName = custAddedByAgentName.trim(),
                    preferredTransporterId = custPreferredTransporterId,
                    preferredTransporterName = custPreferredTransporterName.trim(),
                    transportPreference = custTransportPreference.trim(),
                    dob = custDob.trim(),
                    religion = custReligion.trim(),
                    aadharPhotoUri = custAadharUri,
                    gstCertPhotoUri = custGstCertUri,
                    panPhotoUri = custPanUri,
                    shopPhotoUri = custShopPicUri,
                    purchaserPhotoUri = custPurchaserPicUri,
                    cancelChequePhotoUri = custCancelChequeUri,
                    referredBy = custReferredBy.trim(),
                    creditDays = custCreditDays.toIntOrNull() ?: 30,
                    creditLimit = custCreditLimit.toDoubleOrNull() ?: 0.0,
                    notes = custNotes.trim()
                )
                viewModel.saveCustomer(candidate)
                onBack()
            }

            MasterTab.SUPPLIERS -> {
                val effectiveFirmName = supFirmName.trim()
                if (effectiveFirmName.isBlank()) {
                    validationErrors = listOf("Firm / Mill Name is required.")
                    return
                }

                val primaryContactPhone = supContacts.firstOrNull { it.phone.isNotBlank() }?.phone?.trim() ?: ""
                val primaryOutlet = supOutlets.firstOrNull()
                val allCategories = (supCategories + listOfNotNull(supCustomCategory.trim().takeIf { it.isNotBlank() })).joinToString(", ")

                val candidate = SupplierEntity(
                    id = editingSupplier?.id ?: 0L,
                    supplierId = supId.trim(),
                    name = supContactPerson.trim().ifBlank { effectiveFirmName },
                    firmName = effectiveFirmName,
                    type = supType,
                    brand = supBrandName.trim(),
                    brandId = supBrandId,
                    gstin = supGstin.trim().uppercase(),
                    panNumber = supPanNumber.trim().uppercase(),
                    address = supOfficeAddress.trim().ifBlank { primaryOutlet?.address?.trim() ?: "" },
                    officeAddress = supOfficeAddress.trim(),
                    homeAddress = supHomeAddress.trim(),
                    officeLocation = supOfficeLocation.trim(),
                    personalLocation = supPersonalLocation.trim(),
                    shopCount = maxOf(1, supOutlets.size),
                    shopLocations = supOutlets.joinToString("; ") { "${it.name}: ${it.address}" },
                    city = supCity.trim(),
                    marketArea = supMarketName.trim().ifBlank { supCity.trim() },
                    markets = supMarketName.trim(),
                    marketId = supMarketId,
                    marketName = supMarketName.trim(),
                    contactPerson = supContactPerson.trim(),
                    phone = primaryContactPhone,
                    phone2 = supContacts.getOrNull(1)?.phone?.trim() ?: "",
                    phone3 = supContacts.getOrNull(2)?.phone?.trim() ?: "",
                    phone4 = supContacts.getOrNull(3)?.phone?.trim() ?: "",
                    phone5 = supContacts.getOrNull(4)?.phone?.trim() ?: "",
                    email = supEmail.trim(),
                    email2 = supEmail2.trim(),
                    categories = allCategories,
                    garmentTypes = allCategories,
                    productsMade = supProductsMade.trim(),
                    priceRange = supPriceRange.trim(),
                    factoriesJson = locationsToJson(supFactories),
                    outletsJson = locationsToJson(supOutlets),
                    shopPhotoUri = supShopPhotoUri,
                    visitingCardPhotoUri = supVisitingCardPhotoUri,
                    referredBy = supReferredBy.trim(),
                    defaultCaseSize = editingSupplier?.defaultCaseSize ?: 24, // NO case size input in form!
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

            MasterTab.BRANDS -> {
                if (brandName.trim().isBlank()) {
                    validationErrors = listOf("Brand Name is required.")
                    return
                }
                val candidate = BrandEntity(
                    id = editingBrand?.id ?: 0L,
                    brandName = brandName.trim(),
                    manufacturerId = brandManufacturerId,
                    manufacturerName = brandManufacturerName.trim(),
                    category = brandCategory.trim(),
                    logoPhotoUri = brandLogoUri,
                    description = brandDescription.trim(),
                    isActive = brandIsActive
                )
                viewModel.saveBrand(candidate, onSuccess = onBack)
            }

            MasterTab.TRANSPORTERS -> {
                if (transName.trim().isBlank()) {
                    validationErrors = listOf("Transporter Name is required.")
                    return
                }
                val candidate = TransporterEntity(
                    id = editingTransporter?.id ?: 0L,
                    transporterName = transName.trim(),
                    contactPerson = transContactPerson.trim(),
                    phone1 = transPhone1.trim(),
                    phone2 = transPhone2.trim(),
                    phone3 = transPhone3.trim(),
                    officeAddress = transOfficeAddress.trim(),
                    godownAddress = transGodownAddress.trim(),
                    city = transCity.trim(),
                    destinationsCovered = transDestinations.trim(),
                    gstin = transGstin.trim().uppercase(),
                    trackingUrl = transTrackingUrl.trim(),
                    notes = transNotes.trim()
                )
                viewModel.saveTransporter(candidate, onSuccess = onBack)
            }

            MasterTab.MARKETS -> {
                if (mktName.trim().isBlank()) {
                    validationErrors = listOf("Market Name is required.")
                    return
                }
                val candidate = MarketEntity(
                    id = editingMarket?.id ?: 0L,
                    marketName = mktName.trim(),
                    city = mktCity.trim(),
                    area = mktArea.trim(),
                    landmark = mktLandmark.trim(),
                    pincode = mktPincode.trim(),
                    marketType = mktType,
                    description = mktDescription.trim()
                )
                viewModel.saveMarket(candidate, onSuccess = onBack)
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

    // Inline dialogs
    if (showInlineMarketDialog) {
        InlineMarketDialog(
            onDismiss = { showInlineMarketDialog = false },
            onSave = { newMkt ->
                viewModel.saveMarket(newMkt) {
                    supMarketName = newMkt.marketName
                }
                showInlineMarketDialog = false
            }
        )
    }

    if (showInlineBrandDialog) {
        InlineBrandDialog(
            suppliers = suppliers,
            onDismiss = { showInlineBrandDialog = false },
            onSave = { newBrand ->
                viewModel.saveBrand(newBrand) {
                    supBrandName = newBrand.brandName
                }
                showInlineBrandDialog = false
            }
        )
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
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 42.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
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
                            .defaultMinSize(minHeight = 42.dp)
                    ) {
                        val isEdit = editingCustomer != null || editingSupplier != null ||
                                editingBrand != null || editingTransporter != null ||
                                editingMarket != null || editingProduct != null || editingEmployee != null
                        Text(
                            text = if (isEdit) "Update Master" else "Save Master",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NavyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = screenTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = screenSubtitle,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Validation Errors
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
                                    text = "Please resolve issues below:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
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

                // Dynamic Form Content
                when (activeTab) {
                    MasterTab.CUSTOMERS -> {
                        CustomerMasterForm(
                            firmName = custFirmName,
                            onFirmNameChange = { custFirmName = it },
                            ownerName = custName,
                            onOwnerNameChange = { custName = it },
                            customerId = custId,
                            onCustomerIdChange = { custId = it },
                            gstin = custGstin,
                            onGstinChange = { custGstin = it },
                            panNumber = custPanNumber,
                            onPanNumberChange = { custPanNumber = it },
                            city = custCity,
                            onCityChange = { custCity = it },
                            district = custDistrict,
                            onDistrictChange = { custDistrict = it },
                            state = custState,
                            onStateChange = { custState = it },
                            pincode = custPincode,
                            onPincodeChange = { custPincode = it },
                            customerType = custCustomerType,
                            onCustomerTypeChange = { custCustomerType = it },
                            contacts = custContacts,
                            onContactsChange = { custContacts = it },
                            outlets = custOutlets,
                            onOutletsChange = { custOutlets = it },
                            selectedGarmentTypes = custGarmentTypes,
                            onToggleGarmentType = { type ->
                                custGarmentTypes = if (custGarmentTypes.contains(type)) {
                                    custGarmentTypes - type
                                } else {
                                    custGarmentTypes + type
                                }
                            },
                            customGarmentType = custCustomGarmentType,
                            onCustomGarmentTypeChange = { custCustomGarmentType = it },
                            referredBy = custReferredBy,
                            onReferredByChange = { custReferredBy = it },
                            addedByAgentName = custAddedByAgentName,
                            onAddedByAgentChange = { name, id ->
                                custAddedByAgentName = name
                                custAddedByAgentId = id
                            },
                            preferredTransporterName = custPreferredTransporterName,
                            onPreferredTransporterChange = { name, id ->
                                custPreferredTransporterName = name
                                custPreferredTransporterId = id
                            },
                            transportPreference = custTransportPreference,
                            onTransportPreferenceChange = { custTransportPreference = it },
                            dob = custDob,
                            onDobChange = { custDob = it },
                            religion = custReligion,
                            onReligionChange = { custReligion = it },
                            homeAddress = custHomeAddress,
                            onHomeAddressChange = { custHomeAddress = it },
                            personalLocation = custPersonalLocation,
                            onPersonalLocationChange = { custPersonalLocation = it },
                            aadharPhotoUri = custAadharUri,
                            onAadharPhotoChange = { custAadharUri = it },
                            gstCertPhotoUri = custGstCertUri,
                            onGstCertPhotoChange = { custGstCertUri = it },
                            panPhotoUri = custPanUri,
                            onPanPhotoChange = { custPanUri = it },
                            shopPhotoUri = custShopPicUri,
                            onShopPhotoChange = { custShopPicUri = it },
                            purchaserPhotoUri = custPurchaserPicUri,
                            onPurchaserPhotoChange = { custPurchaserPicUri = it },
                            cancelChequePhotoUri = custCancelChequeUri,
                            onCancelChequePhotoChange = { custCancelChequeUri = it },
                            creditDays = custCreditDays,
                            onCreditDaysChange = { custCreditDays = it },
                            creditLimit = custCreditLimit,
                            onCreditLimitChange = { custCreditLimit = it },
                            email = custEmail,
                            onEmailChange = { custEmail = it },
                            notes = custNotes,
                            onNotesChange = { custNotes = it },
                            customersList = customers,
                            suppliersList = suppliers,
                            employeesList = employees,
                            transportersList = transporters
                        )
                    }

                    MasterTab.SUPPLIERS -> {
                        SupplierMasterForm(
                            firmName = supFirmName,
                            onFirmNameChange = { supFirmName = it },
                            contactPerson = supContactPerson,
                            onContactPersonChange = { supContactPerson = it },
                            supplierId = supId,
                            onSupplierIdChange = { supId = it },
                            type = supType,
                            onTypeChange = { supType = it },
                            marketName = supMarketName,
                            onMarketSelect = { mkt ->
                                supMarketName = mkt.marketName
                                supMarketId = mkt.id
                            },
                            onNewMarketClick = { showInlineMarketDialog = true },
                            brandName = supBrandName,
                            onBrandSelect = { b ->
                                supBrandName = b.brandName
                                supBrandId = b.id
                            },
                            onNewBrandClick = { showInlineBrandDialog = true },
                            gstin = supGstin,
                            onGstinChange = { supGstin = it },
                            panNumber = supPanNumber,
                            onPanNumberChange = { supPanNumber = it },
                            city = supCity,
                            onCityChange = { supCity = it },
                            contacts = supContacts,
                            onContactsChange = { supContacts = it },
                            productsMade = supProductsMade,
                            onProductsMadeChange = { supProductsMade = it },
                            priceRange = supPriceRange,
                            onPriceRangeChange = { supPriceRange = it },
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
                            factories = supFactories,
                            onFactoriesChange = { supFactories = it },
                            outlets = supOutlets,
                            onOutletsChange = { supOutlets = it },
                            officeAddress = supOfficeAddress,
                            onOfficeAddressChange = { supOfficeAddress = it },
                            shopPhotoUri = supShopPhotoUri,
                            onShopPhotoChange = { supShopPhotoUri = it },
                            visitingCardPhotoUri = supVisitingCardPhotoUri,
                            onVisitingCardPhotoChange = { supVisitingCardPhotoUri = it },
                            referredBy = supReferredBy,
                            onReferredByChange = { supReferredBy = it },
                            email = supEmail,
                            onEmailChange = { supEmail = it },
                            notes = supNotes,
                            onNotesChange = { supNotes = it },
                            marketsList = markets,
                            brandsList = brands,
                            customersList = customers,
                            suppliersList = suppliers,
                            employeesList = employees
                        )
                    }

                    MasterTab.BRANDS -> {
                        BrandMasterForm(
                            brandName = brandName,
                            onBrandNameChange = { brandName = it },
                            manufacturerName = brandManufacturerName,
                            onManufacturerSelect = { sup ->
                                brandManufacturerName = sup?.firmName ?: ""
                                brandManufacturerId = sup?.id
                            },
                            category = brandCategory,
                            onCategoryChange = { brandCategory = it },
                            logoPhotoUri = brandLogoUri,
                            onLogoPhotoChange = { brandLogoUri = it },
                            description = brandDescription,
                            onDescriptionChange = { brandDescription = it },
                            isActive = brandIsActive,
                            onIsActiveChange = { brandIsActive = it },
                            suppliersList = suppliers
                        )
                    }

                    MasterTab.TRANSPORTERS -> {
                        TransporterMasterForm(
                            transporterName = transName,
                            onTransporterNameChange = { transName = it },
                            contactPerson = transContactPerson,
                            onContactPersonChange = { transContactPerson = it },
                            phone1 = transPhone1,
                            onPhone1Change = { transPhone1 = it },
                            phone2 = transPhone2,
                            onPhone2Change = { transPhone2 = it },
                            phone3 = transPhone3,
                            onPhone3Change = { transPhone3 = it },
                            officeAddress = transOfficeAddress,
                            onOfficeAddressChange = { transOfficeAddress = it },
                            godownAddress = transGodownAddress,
                            onGodownAddressChange = { transGodownAddress = it },
                            city = transCity,
                            onCityChange = { transCity = it },
                            destinationsCovered = transDestinations,
                            onDestinationsCoveredChange = { transDestinations = it },
                            gstin = transGstin,
                            onGstinChange = { transGstin = it },
                            trackingUrl = transTrackingUrl,
                            onTrackingUrlChange = { transTrackingUrl = it },
                            notes = transNotes,
                            onNotesChange = { transNotes = it }
                        )
                    }

                    MasterTab.MARKETS -> {
                        MarketMasterForm(
                            marketName = mktName,
                            onMarketNameChange = { mktName = it },
                            city = mktCity,
                            onCityChange = { mktCity = it },
                            area = mktArea,
                            onAreaChange = { mktArea = it },
                            landmark = mktLandmark,
                            onLandmarkChange = { mktLandmark = it },
                            pincode = mktPincode,
                            onPincodeChange = { mktPincode = it },
                            marketType = mktType,
                            onMarketTypeChange = { mktType = it },
                            description = mktDescription,
                            onDescriptionChange = { mktDescription = it }
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
                                        text = "Only Administrators can add or edit staff members.",
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

// =============================================================================
// CUSTOMER MASTER FORM COMPOSABLE
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerMasterForm(
    firmName: String,
    onFirmNameChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    customerId: String,
    onCustomerIdChange: (String) -> Unit,
    gstin: String,
    onGstinChange: (String) -> Unit,
    panNumber: String,
    onPanNumberChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    district: String,
    onDistrictChange: (String) -> Unit,
    state: String,
    onStateChange: (String) -> Unit,
    pincode: String,
    onPincodeChange: (String) -> Unit,
    customerType: String,
    onCustomerTypeChange: (String) -> Unit,
    contacts: List<MasterContact>,
    onContactsChange: (List<MasterContact>) -> Unit,
    outlets: List<MasterLocation>,
    onOutletsChange: (List<MasterLocation>) -> Unit,
    selectedGarmentTypes: Set<String>,
    onToggleGarmentType: (String) -> Unit,
    customGarmentType: String,
    onCustomGarmentTypeChange: (String) -> Unit,
    referredBy: String,
    onReferredByChange: (String) -> Unit,
    addedByAgentName: String,
    onAddedByAgentChange: (String, Long?) -> Unit,
    preferredTransporterName: String,
    onPreferredTransporterChange: (String, Long?) -> Unit,
    transportPreference: String,
    onTransportPreferenceChange: (String) -> Unit,
    dob: String,
    onDobChange: (String) -> Unit,
    religion: String,
    onReligionChange: (String) -> Unit,
    homeAddress: String,
    onHomeAddressChange: (String) -> Unit,
    personalLocation: String,
    onPersonalLocationChange: (String) -> Unit,
    aadharPhotoUri: String,
    onAadharPhotoChange: (String) -> Unit,
    gstCertPhotoUri: String,
    onGstCertPhotoChange: (String) -> Unit,
    panPhotoUri: String,
    onPanPhotoChange: (String) -> Unit,
    shopPhotoUri: String,
    onShopPhotoChange: (String) -> Unit,
    purchaserPhotoUri: String,
    onPurchaserPhotoChange: (String) -> Unit,
    cancelChequePhotoUri: String,
    onCancelChequePhotoChange: (String) -> Unit,
    creditDays: String,
    onCreditDaysChange: (String) -> Unit,
    creditLimit: String,
    onCreditLimitChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    customersList: List<CustomerEntity>,
    suppliersList: List<SupplierEntity>,
    employeesList: List<EmployeeEntity>,
    transportersList: List<TransporterEntity>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Card 1: Identity & Business Type
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1. Firm Identity & Account Type",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                // Cash vs Credit customer toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Credit", "Cash").forEach { type ->
                        val isSelected = customerType.equals(type, ignoreCase = true)
                        Surface(
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onCustomerTypeChange(type) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (type == "Credit") Icons.Default.CreditScore else Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = if (isSelected) GoldAccent else NavyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$type Customer",
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = firmName,
                    onValueChange = onFirmNameChange,
                    label = { Text("Shop / Firm Name *") },
                    placeholder = { Text("e.g. Radhe Krishna Fashion Hub") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = onOwnerNameChange,
                        label = { Text("Proprietor / Owner Name") },
                        placeholder = { Text("e.g. Ramesh Patel") },
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
                        modifier = Modifier.weight(1.1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = panNumber,
                        onValueChange = { onPanNumberChange(it.uppercase()) },
                        label = { Text("PAN Number") },
                        placeholder = { Text("ABCDE1234F") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.9f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }
            }
        }

        // Card 2: City, Location & Pincode (NO MARKET SELECTION)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2. City & Geographical Region",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = onCityChange,
                        label = { Text("City *") },
                        placeholder = { Text("e.g. Surat / Ahmedabad") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = district,
                        onValueChange = onDistrictChange,
                        label = { Text("District") },
                        placeholder = { Text("e.g. Surat") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state,
                        onValueChange = onStateChange,
                        label = { Text("State") },
                        placeholder = { Text("Gujarat") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = pincode,
                        onValueChange = onPincodeChange,
                        label = { Text("Pincode") },
                        placeholder = { Text("380002") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }
            }
        }

        // Card 3: Contacts Info (Up to 5)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Contacts Info (Up to 5 Lines)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary
                    )
                    if (contacts.size < 5) {
                        TextButton(
                            onClick = {
                                onContactsChange(contacts + MasterContact(name = "Contact ${contacts.size + 1}"))
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Contact", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }
                }

                contacts.forEachIndexed { index, contact ->
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (index == 0) "Primary Contact / WhatsApp *" else "Contact Line #${index + 1}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = NavyPrimary
                                )
                                if (contacts.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val updated = contacts.toMutableList()
                                            updated.removeAt(index)
                                            onContactsChange(updated)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = contact.name,
                                    onValueChange = { newName ->
                                        val updated = contacts.toMutableList()
                                        updated[index] = contact.copy(name = newName)
                                        onContactsChange(updated)
                                    },
                                    label = { Text("Person / Role") },
                                    placeholder = { Text("e.g. Ramesh Bhai (Owner)") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = defaultTextFieldColors()
                                )

                                OutlinedTextField(
                                    value = contact.phone,
                                    onValueChange = { newPhone ->
                                        val updated = contacts.toMutableList()
                                        updated[index] = contact.copy(phone = newPhone)
                                        onContactsChange(updated)
                                    },
                                    label = { Text("Phone Number") },
                                    placeholder = { Text("98250XXXXX") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = defaultTextFieldColors()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 4: Address of Outlets (Up to 5) with Google Map links
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "4. Shop Outlets & Map Links (Up to 5)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary
                    )
                    if (outlets.size < 5) {
                        TextButton(
                            onClick = {
                                onOutletsChange(outlets + MasterLocation(name = "Outlet #${outlets.size + 1}"))
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Outlet", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }
                }

                outlets.forEachIndexed { index, outlet ->
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (index == 0) "Main Outlet / Flagship Shop" else "Branch / Outlet #${index + 1}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = NavyPrimary
                                )
                                if (outlets.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val updated = outlets.toMutableList()
                                            updated.removeAt(index)
                                            onOutletsChange(updated)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = outlet.name,
                                onValueChange = { newName ->
                                    val updated = outlets.toMutableList()
                                    updated[index] = outlet.copy(name = newName)
                                    onOutletsChange(updated)
                                },
                                label = { Text("Shop / Branch Title") },
                                placeholder = { Text("e.g. Ring Road Branch") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = defaultTextFieldColors()
                            )

                            OutlinedTextField(
                                value = outlet.address,
                                onValueChange = { newAddr ->
                                    val updated = outlets.toMutableList()
                                    updated[index] = outlet.copy(address = newAddr)
                                    onOutletsChange(updated)
                                },
                                label = { Text("Full Shop Address") },
                                placeholder = { Text("e.g. Shop 104, City Center Mall, Surat") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                colors = defaultTextFieldColors()
                            )

                            OutlinedTextField(
                                value = outlet.mapLink,
                                onValueChange = { newMap ->
                                    val updated = outlets.toMutableList()
                                    updated[index] = outlet.copy(mapLink = newMap)
                                    onOutletsChange(updated)
                                },
                                label = { Text("Google Maps Link / Landmark") },
                                placeholder = { Text("https://maps.app.goo.gl/...") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = defaultTextFieldColors()
                            )
                        }
                    }
                }
            }
        }

        // Card 5: Garments Dealt With & Preferences
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "5. Garments Dealt With Mostly",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val garmentOptions = listOf(
                        "Kurti", "Saree", "Dress Material", "Shirting", "Suiting",
                        "Denim Jeans", "T-Shirt", "Leggings", "Fancy Fabrics",
                        "Cotton Voile", "Lycra Trousers", "Ethnic Wear", "Kids Wear"
                    )
                    garmentOptions.forEach { g ->
                        val isSelected = selectedGarmentTypes.contains(g)
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onToggleGarmentType(g) }
                        ) {
                            Text(
                                text = if (isSelected) "✓ $g" else "+ $g",
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customGarmentType,
                    onValueChange = onCustomGarmentTypeChange,
                    label = { Text("+ Custom Garment Category") },
                    placeholder = { Text("e.g. Nightwear / Rayon Print 14kg") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }
        }

        // Card 6: Dynamic Master References (Referred By & Agent & Transporter)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "6. References, Agent & Transport Preference",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                // Referred By (Master Suggestions)
                OutlinedTextField(
                    value = referredBy,
                    onValueChange = onReferredByChange,
                    label = { Text("Referred By") },
                    placeholder = { Text("Select master or type reference name") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                // Quick chips from Masters for Referred By
                Text("Quick suggestions from Masters:", fontSize = 11.sp, color = TextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    customersList.take(3).forEach { c ->
                        SuggestionChip(
                            onClick = { onReferredByChange("${c.firmName} (Customer)") },
                            label = { Text("${c.firmName.take(15)}.. (Cust)", fontSize = 10.5.sp) }
                        )
                    }
                    suppliersList.take(3).forEach { s ->
                        SuggestionChip(
                            onClick = { onReferredByChange("${s.firmName} (Supplier)") },
                            label = { Text("${s.firmName.take(15)}.. (Supp)", fontSize = 10.5.sp) }
                        )
                    }
                    employeesList.take(3).forEach { e ->
                        SuggestionChip(
                            onClick = { onReferredByChange("${e.name} (Agent)") },
                            label = { Text("${e.name} (Staff)", fontSize = 10.5.sp) }
                        )
                    }
                }

                // Added / Handled By Agent
                MasterDropdownField(
                    label = "Added By / Handling Agent *",
                    selectedValue = addedByAgentName,
                    items = employeesList.map { it.name to it.id },
                    onSelect = { name, id -> onAddedByAgentChange(name, id) },
                    placeholder = "Select sales agent / staff"
                )

                // Preferred Transporter Dropdown
                MasterDropdownField(
                    label = "Transporter Preference (From Master)",
                    selectedValue = preferredTransporterName,
                    items = transportersList.map { "${it.transporterName} (${it.city})" to it.id },
                    onSelect = { name, id ->
                        onPreferredTransporterChange(name, id)
                    },
                    placeholder = "Select preferred courier / transport"
                )

                OutlinedTextField(
                    value = transportPreference,
                    onValueChange = onTransportPreferenceChange,
                    label = { Text("Special Transport Instructions") },
                    placeholder = { Text("e.g. Booking via Kalupur Godown / Paid LR") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }
        }

        // Card 7: KYC Documents & Photos
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "7. KYC & Verification Photos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                val safeCustFolder = "customers/${customerId.ifBlank { "cust_${System.currentTimeMillis()}" }.replace("/", "_")}"

                PhotoUploadCard(
                    title = "Aadhaar Card Photo",
                    uriString = aadharPhotoUri,
                    onUriSelected = onAadharPhotoChange,
                    onClear = { onAadharPhotoChange("") },
                    folder = "$safeCustFolder/kyc",
                    prefix = "aadhar"
                )

                PhotoUploadCard(
                    title = "GST Registration Certificate",
                    uriString = gstCertPhotoUri,
                    onUriSelected = onGstCertPhotoChange,
                    onClear = { onGstCertPhotoChange("") },
                    folder = "$safeCustFolder/kyc",
                    prefix = "gst"
                )

                PhotoUploadCard(
                    title = "PAN Card Photo",
                    uriString = panPhotoUri,
                    onUriSelected = onPanPhotoChange,
                    onClear = { onPanPhotoChange("") },
                    folder = "$safeCustFolder/kyc",
                    prefix = "pan"
                )

                PhotoUploadCard(
                    title = "Shop Front / Signboard Photo",
                    uriString = shopPhotoUri,
                    onUriSelected = onShopPhotoChange,
                    onClear = { onShopPhotoChange("") },
                    folder = "$safeCustFolder/photos",
                    prefix = "shop_front"
                )

                PhotoUploadCard(
                    title = "Purchaser / Owner Photo",
                    uriString = purchaserPhotoUri,
                    onUriSelected = onPurchaserPhotoChange,
                    onClear = { onPurchaserPhotoChange("") },
                    folder = "$safeCustFolder/photos",
                    prefix = "purchaser"
                )

                PhotoUploadCard(
                    title = "Cancelled Cheque Photo",
                    uriString = cancelChequePhotoUri,
                    onUriSelected = onCancelChequePhotoChange,
                    onClear = { onCancelChequePhotoChange("") },
                    folder = "$safeCustFolder/kyc",
                    prefix = "cheque"
                )
            }
        }

        // Card 8: Personal Info & Credit Terms
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "8. Personal & Financial Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dob,
                        onValueChange = onDobChange,
                        label = { Text("Date of Birth") },
                        placeholder = { Text("DD/MM/YYYY") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = religion,
                        onValueChange = onReligionChange,
                        label = { Text("Religion / Community") },
                        placeholder = { Text("e.g. Hindu / Jain") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = homeAddress,
                    onValueChange = onHomeAddressChange,
                    label = { Text("Residence / Home Address") },
                    placeholder = { Text("e.g. 12, Shanti Nagar, Paldi") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = personalLocation,
                    onValueChange = onPersonalLocationChange,
                    label = { Text("Native Town / Residence Landmark") },
                    placeholder = { Text("e.g. Paldi / Mehsana") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                if (customerType.equals("Credit", ignoreCase = true)) {
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
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Additional Remarks") },
                    placeholder = { Text("Payment history, preferences...") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = defaultTextFieldColors()
                )
            }
        }
    }
}

// =============================================================================
// SUPPLIER MASTER FORM COMPOSABLE
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupplierMasterForm(
    firmName: String,
    onFirmNameChange: (String) -> Unit,
    contactPerson: String,
    onContactPersonChange: (String) -> Unit,
    supplierId: String,
    onSupplierIdChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    marketName: String,
    onMarketSelect: (MarketEntity) -> Unit,
    onNewMarketClick: () -> Unit,
    brandName: String,
    onBrandSelect: (BrandEntity) -> Unit,
    onNewBrandClick: () -> Unit,
    gstin: String,
    onGstinChange: (String) -> Unit,
    panNumber: String,
    onPanNumberChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    contacts: List<MasterContact>,
    onContactsChange: (List<MasterContact>) -> Unit,
    productsMade: String,
    onProductsMadeChange: (String) -> Unit,
    priceRange: String,
    onPriceRangeChange: (String) -> Unit,
    selectedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    customCategory: String,
    onCustomCategoryChange: (String) -> Unit,
    factories: List<MasterLocation>,
    onFactoriesChange: (List<MasterLocation>) -> Unit,
    outlets: List<MasterLocation>,
    onOutletsChange: (List<MasterLocation>) -> Unit,
    officeAddress: String,
    onOfficeAddressChange: (String) -> Unit,
    shopPhotoUri: String,
    onShopPhotoChange: (String) -> Unit,
    visitingCardPhotoUri: String,
    onVisitingCardPhotoChange: (String) -> Unit,
    referredBy: String,
    onReferredByChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    marketsList: List<MarketEntity>,
    brandsList: List<BrandEntity>,
    customersList: List<CustomerEntity>,
    suppliersList: List<SupplierEntity>,
    employeesList: List<EmployeeEntity>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Card 1: Mill / Supplier Profile
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1. Mill & Supplier Nature",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Manufacturer", "Wholesaler", "Trader").forEach { t ->
                        val isSelected = type.equals(t, ignoreCase = true)
                        Surface(
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onTypeChange(t) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
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
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = firmName,
                    onValueChange = onFirmNameChange,
                    label = { Text("Firm / Mill Name *") },
                    placeholder = { Text("e.g. Radheshyam Textile Mills Pvt Ltd") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = onContactPersonChange,
                        label = { Text("Key Contact Person") },
                        placeholder = { Text("e.g. Arvind Bhai") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = supplierId,
                        onValueChange = onSupplierIdChange,
                        label = { Text("Supplier ID") },
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
                        modifier = Modifier.weight(1.1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = panNumber,
                        onValueChange = { onPanNumberChange(it.uppercase()) },
                        label = { Text("PAN Number") },
                        placeholder = { Text("ABCDE1234F") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.9f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }
            }
        }

        // Card 2: Market & Brand Association (with Inline Create)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2. Market & Brand Selection",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                // Market Selection
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        MasterDropdownField(
                            label = "Select Textile Market *",
                            selectedValue = marketName,
                            items = marketsList.map { "${it.marketName} (${it.city})" to it.id },
                            onSelect = { name, id ->
                                val mkt = marketsList.find { it.id == id }
                                if (mkt != null) onMarketSelect(mkt)
                            },
                            placeholder = "Choose market from master"
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onNewMarketClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NavyPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 50.dp)
                    ) {
                        Text("+ New", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                }

                // Brand Selection
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        MasterDropdownField(
                            label = "Select Brand (Optional)",
                            selectedValue = brandName,
                            items = brandsList.map { it.brandName to it.id },
                            onSelect = { name, id ->
                                val b = brandsList.find { it.id == id }
                                if (b != null) onBrandSelect(b)
                            },
                            placeholder = "Choose brand from master"
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onNewBrandClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NavyPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 50.dp)
                    ) {
                        Text("+ New", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                }

                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("City / Textile Hub") },
                    placeholder = { Text("Ahmedabad / Surat / Mumbai") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }
        }

        // Card 3: Contact Info (Up to 5)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Contacts Info (Up to 5 Lines)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary
                    )
                    if (contacts.size < 5) {
                        TextButton(
                            onClick = {
                                onContactsChange(contacts + MasterContact(name = "Desk ${contacts.size + 1}"))
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Line", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }
                }

                contacts.forEachIndexed { index, contact ->
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (index == 0) "Primary Order Desk / WhatsApp *" else "Contact Line #${index + 1}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = NavyPrimary
                                )
                                if (contacts.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val updated = contacts.toMutableList()
                                            updated.removeAt(index)
                                            onContactsChange(updated)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = contact.name,
                                    onValueChange = { newName ->
                                        val updated = contacts.toMutableList()
                                        updated[index] = contact.copy(name = newName)
                                        onContactsChange(updated)
                                    },
                                    label = { Text("Person / Department") },
                                    placeholder = { Text("e.g. Sales Desk") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = defaultTextFieldColors()
                                )

                                OutlinedTextField(
                                    value = contact.phone,
                                    onValueChange = { newPhone ->
                                        val updated = contacts.toMutableList()
                                        updated[index] = contact.copy(phone = newPhone)
                                        onContactsChange(updated)
                                    },
                                    label = { Text("Phone Number") },
                                    placeholder = { Text("98250XXXXX") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = defaultTextFieldColors()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 4: What They Make & Price Range
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "4. Manufacturing Items & Price Range",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                OutlinedTextField(
                    value = productsMade,
                    onValueChange = onProductsMadeChange,
                    label = { Text("What They Make / Manufacturing Items") },
                    placeholder = { Text("e.g. 100% Cotton Printed Kurtis, Heavy Rayon Palazzos, Shirting") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = priceRange,
                    onValueChange = onPriceRangeChange,
                    label = { Text("Product Price Range (₹)") },
                    placeholder = { Text("e.g. ₹250 - ₹750 / piece") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Text("Garment categories sold:", fontSize = 11.5.sp, color = TextSecondary)
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
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onToggleCategory(cat) }
                        ) {
                            Text(
                                text = if (isSelected) "✓ $cat" else "+ $cat",
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customCategory,
                    onValueChange = onCustomCategoryChange,
                    label = { Text("+ Custom Fabric / Category") },
                    placeholder = { Text("e.g. Denim Lycra 10oz") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }
        }

        // Card 5: Factory Addresses (Up to 5)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "5. Factory / Manufacturing Units (Up to 5)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary
                    )
                    if (factories.size < 5) {
                        TextButton(
                            onClick = {
                                onFactoriesChange(factories + MasterLocation(name = "Factory Unit #${factories.size + 1}"))
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Unit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }
                }

                factories.forEachIndexed { index, fac ->
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (index == 0) "Main Mill / Manufacturing Unit" else "Factory Unit #${index + 1}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = NavyPrimary
                                )
                                if (factories.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val updated = factories.toMutableList()
                                            updated.removeAt(index)
                                            onFactoriesChange(updated)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = fac.name,
                                onValueChange = { newName ->
                                    val updated = factories.toMutableList()
                                    updated[index] = fac.copy(name = newName)
                                    onFactoriesChange(updated)
                                },
                                label = { Text("Unit Title") },
                                placeholder = { Text("e.g. Narol GIDC Dyeing Unit") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = defaultTextFieldColors()
                            )

                            OutlinedTextField(
                                value = fac.address,
                                onValueChange = { newAddr ->
                                    val updated = factories.toMutableList()
                                    updated[index] = fac.copy(address = newAddr)
                                    onFactoriesChange(updated)
                                },
                                label = { Text("Factory Physical Address") },
                                placeholder = { Text("Plot 24, Narol GIDC Phase 2, Ahmedabad") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                colors = defaultTextFieldColors()
                            )
                        }
                    }
                }
            }
        }

        // Card 6: Outlet / Showroom Addresses (Up to 5)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "6. Outlets / Showrooms (Up to 5)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary
                    )
                    if (outlets.size < 5) {
                        TextButton(
                            onClick = {
                                onOutletsChange(outlets + MasterLocation(name = "Outlet #${outlets.size + 1}"))
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Outlet", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }
                }

                outlets.forEachIndexed { index, out ->
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (index == 0) "Main Sales Gaddi / Showroom" else "Branch / Outlet #${index + 1}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = NavyPrimary
                                )
                                if (outlets.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val updated = outlets.toMutableList()
                                            updated.removeAt(index)
                                            onOutletsChange(updated)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = out.name,
                                onValueChange = { newName ->
                                    val updated = outlets.toMutableList()
                                    updated[index] = out.copy(name = newName)
                                    onOutletsChange(updated)
                                },
                                label = { Text("Showroom / Shop Name") },
                                placeholder = { Text("e.g. Maskati Cloth Market Shop") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = defaultTextFieldColors()
                            )

                            OutlinedTextField(
                                value = out.address,
                                onValueChange = { newAddr ->
                                    val updated = outlets.toMutableList()
                                    updated[index] = out.copy(address = newAddr)
                                    onOutletsChange(updated)
                                },
                                label = { Text("Shop Address") },
                                placeholder = { Text("Shop 45, Ground Floor, Maskati Market") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                colors = defaultTextFieldColors()
                            )
                        }
                    }
                }
            }
        }

        // Card 7: Photos (Shop Photo & Visiting Card Photo)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "7. Verification Photos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                val safeSuppFolder = "suppliers/${supplierId.ifBlank { "supp_${System.currentTimeMillis()}" }.replace("/", "_")}/photos"

                PhotoUploadCard(
                    title = "Shop / Mill Front Photo",
                    uriString = shopPhotoUri,
                    onUriSelected = onShopPhotoChange,
                    onClear = { onShopPhotoChange("") },
                    folder = safeSuppFolder,
                    prefix = "mill_front"
                )

                PhotoUploadCard(
                    title = "Visiting Card Photo",
                    uriString = visitingCardPhotoUri,
                    onUriSelected = onVisitingCardPhotoChange,
                    onClear = { onVisitingCardPhotoChange("") },
                    folder = safeSuppFolder,
                    prefix = "visiting_card"
                )
            }
        }

        // Card 8: References & Communication
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "8. References & Office Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                OutlinedTextField(
                    value = officeAddress,
                    onValueChange = onOfficeAddressChange,
                    label = { Text("Registered Office Address") },
                    placeholder = { Text("e.g. 401, Textile Tower, Ring Road") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = referredBy,
                    onValueChange = onReferredByChange,
                    label = { Text("Referred By") },
                    placeholder = { Text("e.g. Ramesh Bhai / Paresh (Agent)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Official Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Commercial Terms / Notes") },
                    placeholder = { Text("e.g. 5% cash discount in 7 days...") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = defaultTextFieldColors()
                )
            }
        }
    }
}

// =============================================================================
// BRAND MASTER FORM COMPOSABLE
// =============================================================================

@Composable
private fun BrandMasterForm(
    brandName: String,
    onBrandNameChange: (String) -> Unit,
    manufacturerName: String,
    onManufacturerSelect: (SupplierEntity?) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    logoPhotoUri: String,
    onLogoPhotoChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    isActive: Boolean,
    onIsActiveChange: (Boolean) -> Unit,
    suppliersList: List<SupplierEntity>
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Brand Master Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = NavyPrimary
            )

            OutlinedTextField(
                value = brandName,
                onValueChange = onBrandNameChange,
                label = { Text("Brand Name *") },
                placeholder = { Text("e.g. RADHE TEXTILES / VIMAL") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = defaultTextFieldColors()
            )

            MasterDropdownField(
                label = "Linked Manufacturer / Mill",
                selectedValue = manufacturerName,
                items = suppliersList.map { it.firmName to it.id },
                onSelect = { name, id ->
                    val sup = suppliersList.find { it.id == id }
                    onManufacturerSelect(sup)
                },
                placeholder = "Select supplier / mill"
            )

            OutlinedTextField(
                value = category,
                onValueChange = onCategoryChange,
                label = { Text("Garment Category") },
                placeholder = { Text("e.g. Cotton Shirting, Kurtis, Denim") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = defaultTextFieldColors()
            )

            val safeBrandFolder = "brands/${brandName.ifBlank { "brand_${System.currentTimeMillis()}" }.replace(" ", "_").replace("/", "_")}/logo"

            PhotoUploadCard(
                title = "Brand Logo / Label Photo",
                uriString = logoPhotoUri,
                onUriSelected = onLogoPhotoChange,
                onClear = { onLogoPhotoChange("") },
                folder = safeBrandFolder,
                prefix = "logo"
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Brand Description") },
                placeholder = { Text("e.g. Premium pure cotton formal fabrics") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = defaultTextFieldColors()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Brand Status", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NavyPrimary)
                    Text(if (isActive) "Active brand for sales" else "Archived / Inactive", fontSize = 11.sp, color = TextSecondary)
                }
                Switch(checked = isActive, onCheckedChange = onIsActiveChange)
            }
        }
    }
}

// =============================================================================
// TRANSPORTER MASTER FORM COMPOSABLE
// =============================================================================

@Composable
private fun TransporterMasterForm(
    transporterName: String,
    onTransporterNameChange: (String) -> Unit,
    contactPerson: String,
    onContactPersonChange: (String) -> Unit,
    phone1: String,
    onPhone1Change: (String) -> Unit,
    phone2: String,
    onPhone2Change: (String) -> Unit,
    phone3: String,
    onPhone3Change: (String) -> Unit,
    officeAddress: String,
    onOfficeAddressChange: (String) -> Unit,
    godownAddress: String,
    onGodownAddressChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    destinationsCovered: String,
    onDestinationsCoveredChange: (String) -> Unit,
    gstin: String,
    onGstinChange: (String) -> Unit,
    trackingUrl: String,
    onTrackingUrlChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Transporter / Courier Master",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = NavyPrimary
            )

            OutlinedTextField(
                value = transporterName,
                onValueChange = onTransporterNameChange,
                label = { Text("Transporter / Courier Name *") },
                placeholder = { Text("e.g. Mahavir Transport / VRL Logistics") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = defaultTextFieldColors()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = onContactPersonChange,
                    label = { Text("Contact Person") },
                    placeholder = { Text("e.g. Naresh Bhai") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("City / Main Hub") },
                    placeholder = { Text("Ahmedabad") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }

            Text("Booking & Godown Contact Numbers:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone1,
                    onValueChange = onPhone1Change,
                    label = { Text("Phone 1 (Booking) *") },
                    placeholder = { Text("98250XXXXX") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = phone2,
                    onValueChange = onPhone2Change,
                    label = { Text("Phone 2 (Godown)") },
                    placeholder = { Text("98251XXXXX") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }

            OutlinedTextField(
                value = phone3,
                onValueChange = onPhone3Change,
                label = { Text("Phone 3 (Accounts / Dispatch)") },
                placeholder = { Text("98252XXXXX") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = defaultTextFieldColors()
            )

            OutlinedTextField(
                value = destinationsCovered,
                onValueChange = onDestinationsCoveredChange,
                label = { Text("Routes / Destinations Covered") },
                placeholder = { Text("e.g. Gujarat, Rajasthan, MP, Maharashtra, Delhi NCR") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = defaultTextFieldColors()
            )

            OutlinedTextField(
                value = officeAddress,
                onValueChange = onOfficeAddressChange,
                label = { Text("Office Address") },
                placeholder = { Text("e.g. Shop 5, Near Kalupur Overbridge") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = defaultTextFieldColors()
            )

            OutlinedTextField(
                value = godownAddress,
                onValueChange = onGodownAddressChange,
                label = { Text("Godown / Delivery Hub Address") },
                placeholder = { Text("e.g. Plot 18, Aslali Ring Road Godown, Ahmedabad") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = defaultTextFieldColors()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gstin,
                    onValueChange = { onGstinChange(it.uppercase()) },
                    label = { Text("GSTIN") },
                    placeholder = { Text("24AAAAA0000A1Z5") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = trackingUrl,
                    onValueChange = onTrackingUrlChange,
                    label = { Text("Consignment Tracking URL") },
                    placeholder = { Text("https://...") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Special Notes / Transit Terms") },
                placeholder = { Text("e.g. 2 days transit to Jaipur, door delivery available") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = defaultTextFieldColors()
            )
        }
    }
}

// =============================================================================
// MARKET MASTER FORM COMPOSABLE
// =============================================================================

@Composable
private fun MarketMasterForm(
    marketName: String,
    onMarketNameChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    area: String,
    onAreaChange: (String) -> Unit,
    landmark: String,
    onLandmarkChange: (String) -> Unit,
    pincode: String,
    onPincodeChange: (String) -> Unit,
    marketType: String,
    onMarketTypeChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Textile Market Master",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = NavyPrimary
            )

            OutlinedTextField(
                value = marketName,
                onValueChange = onMarketNameChange,
                label = { Text("Market / Complex Name *") },
                placeholder = { Text("e.g. Maskati Cloth Market / New Cloth Market") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = defaultTextFieldColors()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("City *") },
                    placeholder = { Text("Ahmedabad") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = area,
                    onValueChange = onAreaChange,
                    label = { Text("Area / Sub-locality") },
                    placeholder = { Text("Kalupur / Sakar Bazar") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = landmark,
                    onValueChange = onLandmarkChange,
                    label = { Text("Landmark") },
                    placeholder = { Text("Near Railway Station Gate") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = pincode,
                    onValueChange = onPincodeChange,
                    label = { Text("Pincode") },
                    placeholder = { Text("380002") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(0.8f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }

            Text("Market Type / Cluster:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Wholesale", "Retail", "Mill Agents", "Mixed").forEach { mType ->
                    val isSelected = marketType.equals(mType, ignoreCase = true)
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { onMarketTypeChange(mType) }
                    ) {
                        Text(
                            text = mType,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimary,
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Market Details / Description") },
                placeholder = { Text("e.g. Major trading market for cotton grey, prints and shirting") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = defaultTextFieldColors()
            )
        }
    }
}

// =============================================================================
// PRODUCT FORM CONTENT
// =============================================================================

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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Product Specifications",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = NavyPrimary
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = productCode,
                    onValueChange = onProductCodeChange,
                    label = { Text("Product Code *") },
                    placeholder = { Text("e.g. KRT-101") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Product Name *") },
                    placeholder = { Text("e.g. Cotton Anarkali Kurti") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.3f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }

            OutlinedTextField(
                value = category,
                onValueChange = onCategoryChange,
                label = { Text("Category") },
                placeholder = { Text("e.g. Kurti / Saree / Shirting") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = defaultTextFieldColors()
            )

            // Supplier Mapping
            ExposedDropdownMenuBox(
                expanded = supplierExpanded,
                onExpandedChange = onExpandedChange,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedSupplier?.firmName ?: selectedSupplier?.name ?: "Select Supplier / Mill",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Supplier / Mill *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = defaultTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = supplierExpanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    suppliers.forEach { sup ->
                        DropdownMenuItem(
                            text = { Text(sup.firmName.ifBlank { sup.name }) },
                            onClick = {
                                onSupplierSelect(sup)
                                onExpandedChange(false)
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = defaultRate,
                    onValueChange = onDefaultRateChange,
                    label = { Text("Rate (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = defaultCaseSize,
                    onValueChange = onDefaultCaseSizeChange,
                    label = { Text("Case Size (Pcs) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = hsnCode,
                    onValueChange = onHsnCodeChange,
                    label = { Text("HSN Code") },
                    placeholder = { Text("6203") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Product Description / Fabric Specs") },
                placeholder = { Text("60x60 Cambric cotton, 44 inch width") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = defaultTextFieldColors()
            )
        }
    }
}

// =============================================================================
// EMPLOYEE FORM CONTENT
// =============================================================================

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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1. Staff Identity & Role",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyPrimary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Salesman", "Admin").forEach { r ->
                        val isSelected = role.equals(r, ignoreCase = true)
                        Surface(
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onRoleChange(r) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (r == "Admin") "👑 Administrator" else "💼 Sales Agent",
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Staff Full Name *") },
                        placeholder = { Text("e.g. Sunil Verma") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )

                    OutlinedTextField(
                        value = employeeId,
                        onValueChange = onEmployeeIdChange,
                        label = { Text("Employee ID") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Google Account Email (For App Login) *") },
                    placeholder = { Text("user@gmail.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Contact Lines (Up to 5)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary
                    )
                    if (phoneCount < 5) {
                        TextButton(
                            onClick = onAddPhone,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Line", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Primary Phone / WhatsApp *") },
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
                        label = { Text("Phone 2 (Alternate)") },
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
                        label = { Text("Phone 3 (Family / Desk)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "3. Emergency & Assigned Markets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
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

                Text("Assigned Ahmedabad Markets:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
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
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1)),
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

// =============================================================================
// REUSABLE HELPER COMPONENTS
// =============================================================================

@Composable
private fun PhotoUploadCard(
    title: String,
    uriString: String,
    onUriSelected: (String) -> Unit,
    onClear: () -> Unit,
    folder: String = "uploads",
    prefix: String = "doc",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageService = remember { FirebaseStorageService() }
    var isUploading by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            coroutineScope.launch {
                val result = storageService.uploadFile(context, uri, folder, prefix)
                isUploading = false
                result.onSuccess { downloadUrl ->
                    onUriSelected(downloadUrl)
                    Toast.makeText(context, "$title uploaded to Cloud!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Upload failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (uriString.isNotBlank()) Color(0xFF10B981) else Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (uriString.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                            .clickable { showPreviewDialog = true }
                    ) {
                        AsyncImage(
                            model = uriString,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = NavyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isUploading -> "Uploading to Firebase Cloud..."
                            uriString.startsWith("http") -> "Cloud Storage ✓"
                            uriString.isNotBlank() -> "Photo Attached ✓"
                            else -> "Tap to upload to cloud"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = if (uriString.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                        color = when {
                            isUploading -> Color(0xFFD97706)
                            uriString.isNotBlank() -> Color(0xFF059669)
                            else -> TextSecondary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = NavyPrimary
                    )
                } else {
                    if (uriString.isNotBlank()) {
                        IconButton(
                            onClick = { showPreviewDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "View Photo",
                                tint = NavyPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Button(
                        onClick = { launcher.launch("image/*") },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uriString.isNotBlank()) Color(0xFFE2E8F0) else NavyPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 28.dp)
                    ) {
                        Text(
                            text = if (uriString.isNotBlank()) "Change" else "Upload",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uriString.isNotBlank()) NavyPrimary else GoldAccent
                        )
                    }
                }
            }
        }
    }

    if (showPreviewDialog && uriString.isNotBlank()) {
        Dialog(onDismissRequest = { showPreviewDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NavyPrimary
                        )
                        IconButton(
                            onClick = { showPreviewDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = uriString,
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (uriString.startsWith("http")) {
                            OutlinedButton(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uriString))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Open in Browser", fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { showPreviewDialog = false },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Text("Done", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MasterDropdownField(
    label: String,
    selectedValue: String,
    items: List<Pair<String, Long>>,
    onSelect: (String, Long) -> Unit,
    placeholder: String = "Select from master"
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue.ifBlank { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = defaultTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (items.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No records available", color = TextSecondary) },
                    onClick = { expanded = false }
                )
            } else {
                items.forEach { (name, id) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelect(name, id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Inline Creation Dialogs
// -------------------------------------------------------------

@Composable
private fun InlineMarketDialog(
    onDismiss: () -> Unit,
    onSave: (MarketEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Ahmedabad") }
    var area by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var marketType by remember { mutableStateOf("Wholesale") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick Add New Market", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyPrimary)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Market Name *") },
                    placeholder = { Text("e.g. Ratanpole Wholesale") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = { Text("Area") },
                        placeholder = { Text("Kalupur") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = defaultTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text("Landmark") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    MarketEntity(
                                        marketName = name.trim(),
                                        city = city.trim(),
                                        area = area.trim(),
                                        landmark = landmark.trim(),
                                        marketType = marketType
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add & Select", color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineBrandDialog(
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onSave: (BrandEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Apparel") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick Add New Brand", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyPrimary)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Brand Name *") },
                    placeholder = { Text("e.g. RADHE TEXTILES") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("e.g. Sarees, Shirting") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = defaultTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    BrandEntity(
                                        brandName = name.trim(),
                                        category = category.trim()
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add & Select", color = GoldAccent, fontWeight = FontWeight.Bold)
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
