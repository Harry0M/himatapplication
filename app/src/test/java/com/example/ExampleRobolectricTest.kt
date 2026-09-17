package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import com.example.util.PdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Himat Textile", appName)
  }

  @Test
  fun `verify mixed case packing math`() {
    // 50 pcs with case size 24
    val pieces1 = 50
    val caseSize1 = 24
    val cases1 = pieces1 / caseSize1
    val loose1 = pieces1 % caseSize1
    assertEquals(2, cases1)
    assertEquals(2, loose1)

    // 52 pcs with case size 30
    val pieces2 = 52
    val caseSize2 = 30
    val cases2 = pieces2 / caseSize2
    val loose2 = pieces2 % caseSize2
    assertEquals(1, cases2)
    assertEquals(22, loose2)

    // Combined mixed pack
    val totalCombinedLoose = loose1 + loose2
    val targetCaseSize = 24
    val mixedCases = totalCombinedLoose / targetCaseSize
    val remainingLoose = totalCombinedLoose % targetCaseSize

    assertEquals(24, totalCombinedLoose)
    assertEquals(1, mixedCases)
    assertEquals(0, remainingLoose)
  }

  @Test
  fun `verify document text generation for customer and supplier`() {
    val visit = VisitEntity(
      id = 1,
      visitCode = "VIS-TEST-01",
      customerId = 1,
      customerName = "Rajesh Garments",
      employeeId = 2,
      employeeName = "Sunil Verma",
      date = "2026-09-07",
      notes = "Test trip",
      status = "Active"
    )
    val customer = CustomerEntity(
      id = 1,
      customerId = "CUST-101",
      name = "Rajesh Garments",
      phone = "9810123456",
      address = "Karol Bagh",
      city = "Delhi",
      gstin = "07AAACR1234F1Z8",
      defaultSalesmanId = 2,
      creditDays = 30
    )
    val supplier = SupplierEntity(
      id = 1,
      supplierId = "SUP-101",
      name = "Vardhman Denim Mills",
      type = "Manufacturer",
      brand = "V-Denim",
      gstin = "24AABCV9876B1Z2",
      address = "Surat",
      marketArea = "Surat Textile Market",
      contactPerson = "Sanjay Bhai",
      phone = "9825123456",
      defaultCaseSize = 24
    )
    val entries = listOf(
      PurchaseEntryEntity(
        id = 1,
        orderNo = "HT-2601",
        visitId = 1,
        supplierId = 1,
        supplierName = "Vardhman Denim Mills",
        supplierType = "Manufacturer",
        itemCode = "DENIM-701",
        pieces = 50,
        rate = 420.0,
        totalAmount = 21000.0,
        caseSize = 24,
        caseCount = 2,
        loosePieces = 2,
        gstRate = 5.0,
        gstAmount = 1050.0,
        grandTotalWithGst = 22050.0,
        expectedDeliveryDate = "2026-09-15",
        deliveryStatus = "Packed",
        transporter = "Jaipur Golden",
        mixedPackNote = "Packed 2 pcs into Mixed Case #1 with 22 pcs COT-SHIRT"
      )
    )

    val customerReportText = com.example.util.ShareUtil.buildCustomerReportText(visit, customer, entries)
    assertTrue(customerReportText.contains("Customer Day Report"))
    assertTrue(customerReportText.contains("Rajesh Garments"))
    assertTrue(customerReportText.contains("DENIM-701"))
    assertTrue(customerReportText.contains("Vardhman Denim Mills"))
    assertTrue(customerReportText.contains("Packed 2 pcs into Mixed Case #1"))

    val supplierCopyText = com.example.util.ShareUtil.buildSupplierCopyText(visit, supplier, customer, entries)
    assertTrue(supplierCopyText.contains("Supplier Purchase Order Copy"))
    assertTrue(supplierCopyText.contains("Vardhman Denim Mills"))
    assertTrue(supplierCopyText.contains("DENIM-701"))
    assertTrue(supplierCopyText.contains("Packing Note"))
  }

  @Test
  fun `verify transaction status progression order`() {
    fun nextStatus(current: String): String = when (current.lowercase()) {
      "pending" -> "Packed"
      "packed" -> "Dispatched"
      "dispatched" -> "Delivered"
      else -> "Delivered"
    }

    assertEquals("Packed", nextStatus("Pending"))
    assertEquals("Dispatched", nextStatus("Packed"))
    assertEquals("Delivered", nextStatus("Dispatched"))
    assertEquals("Delivered", nextStatus("Delivered"))
  }

  @Test
  fun `verify supplier entity with contact details and categories`() {
    val supplier = SupplierEntity(
      id = 10,
      supplierId = "SUP-201",
      name = "Arvind Mills Fabric Hub",
      type = "Manufacturer",
      brand = "Arvind Denim",
      gstin = "24AAACA0000A1Z5",
      address = "Naroda Road",
      city = "Ahmedabad",
      marketArea = "Maskati Cloth Market",
      contactPerson = "Naveen Patel",
      phone = "+91 98980 12345",
      email = "naveen@arvindfabrics.com",
      categories = "Denim Fabric, Cotton Shirting, Linen Blend, Jeans Garments",
      defaultCaseSize = 24
    )

    assertEquals("Arvind Mills Fabric Hub", supplier.name)
    assertEquals("+91 98980 12345", supplier.phone)
    assertEquals("naveen@arvindfabrics.com", supplier.email)
    assertEquals(4, supplier.categoryList.size)
    assertTrue(supplier.categoryList.contains("Denim Fabric"))
    assertTrue(supplier.categoryList.contains("Cotton Shirting"))
    assertTrue(supplier.categories.contains("Jeans Garments"))
  }

  @Test
  fun `verify custom packaging with independent cases and loose pieces`() {
    // User scenario: 75 pcs total with 2 cases and 5 loose pieces (e.g. 50 in case 1, 20 in case 2, 5 loose)
    val pieces = 75
    val enteredCases = 2
    val enteredLoose = 5
    val rate = 450.0

    val totalAmount = pieces * rate
    assertEquals(33750.0, totalAmount, 0.001)

    val entry = PurchaseEntryEntity(
      id = 101,
      orderNo = "HT-7501",
      visitId = 1,
      supplierId = 1,
      supplierName = "Vardhman Denim Mills",
      supplierType = "Manufacturer",
      itemCode = "KURTI-102",
      pieces = pieces,
      rate = rate,
      totalAmount = totalAmount,
      caseSize = 24,
      caseCount = enteredCases,
      loosePieces = enteredLoose,
      gstRate = 5.0,
      gstAmount = (totalAmount * 5.0) / 100.0,
      grandTotalWithGst = totalAmount + ((totalAmount * 5.0) / 100.0)
    )

    assertEquals(75, entry.pieces)
    assertEquals(2, entry.caseCount)
    assertEquals(5, entry.loosePieces)
  }
}

