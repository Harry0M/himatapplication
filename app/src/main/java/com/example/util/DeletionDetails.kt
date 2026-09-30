package com.example.util

import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.ChequePdcEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.MarketEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.TransporterEntity
import com.example.data.local.entity.VisitEntity
import java.util.Locale

/**
 * The full picture of a record somebody has asked to delete.
 *
 * The delete queue used to show only the one-line summary written when the request was filed, which is
 * not enough to decide on: an admin looking at "Order #HT-2711" cannot tell whether it is ₹800 or
 * ₹80,000, whether it has been dispatched, or whether it has been paid. This carries the fields that
 * actually inform that decision, read from the real record rather than from a string.
 */
data class RecordDetail(
    val title: String,
    val subtitle: String = "",
    /** Label to value, in the order worth reading. Blank values are dropped by the builders. */
    val fields: List<Pair<String, String>> = emptyList(),
    /** Money on the line, called out separately because it is the thing that decides most cases. */
    val amount: String = "",
    /** Warnings: what else goes, and whether it has already been paid or dispatched. */
    val warnings: List<String> = emptyList()
)

/**
 * Turns a record into something readable. Pure, so it can be unit tested and so the queue, the
 * confirmation dialog and any future screen all describe a record the same way.
 */
object DeletionDetails {

    private fun money(value: Double): String =
        "₹" + String.format(Locale.US, "%,.0f", value)

    /** Drops any row with nothing in it, so a half-filled record does not print empty labels. */
    private fun rows(vararg pairs: Pair<String, String?>): List<Pair<String, String>> =
        pairs.mapNotNull { (label, value) ->
            val v = value?.trim().orEmpty()
            if (v.isBlank() || v == "0" || v == "—") null else label to v
        }

    fun forVisit(
        visit: VisitEntity,
        customer: CustomerEntity?,
        orders: List<PurchaseEntryEntity>
    ): RecordDetail {
        val live = orders.filter { it.visitId == visit.id }
        val value = live.sumOf { if (it.grandTotalWithGst > 0.0) it.grandTotalWithGst else it.totalAmount }
        val paid = live.sumOf { it.paidAmount }
        val dispatched = live.count { it.deliveryStatus.equals("Dispatched", true) || it.deliveryStatus.equals("Delivered", true) }

        return RecordDetail(
            title = "Trip ${visit.visitCode.ifBlank { visit.id.toString() }}",
            subtitle = customer?.brandName()?.ifBlank { visit.customerName } ?: visit.customerName,
            amount = if (value > 0) money(value) else "",
            fields = rows(
                "Date" to visit.date,
                "Type" to if (visit.isPhoneTrip()) "Phone order" else "Market visit",
                "Status" to visit.status,
                "Salesmen" to visit.memberNames.ifBlank { visit.employeeName },
                "Orders on it" to live.size.toString(),
                "Customer phone" to customer?.phone,
                "Notes" to visit.notes
            ),
            warnings = buildList {
                if (live.isNotEmpty()) {
                    add("Deleting this trip also removes ${live.size} order${if (live.size == 1) "" else "s"} worth ${money(value)}")
                }
                if (paid > 0) add("${money(paid)} has already been received against these orders")
                if (dispatched > 0) add("$dispatched of them are already dispatched or delivered")
            }
        )
    }

    fun forOrder(entry: PurchaseEntryEntity, visit: VisitEntity?, customer: CustomerEntity?): RecordDetail {
        val billed = if (entry.grandTotalWithGst > 0.0) entry.grandTotalWithGst else entry.totalAmount
        return RecordDetail(
            title = "Order ${entry.orderNo.ifBlank { entry.id.toString() }}",
            subtitle = listOfNotNull(
                entry.supplierName.takeIf { it.isNotBlank() },
                customer?.brandName()?.takeIf { it.isNotBlank() } ?: visit?.customerName?.takeIf { it.isNotBlank() }
            ).joinToString(" → "),
            amount = if (billed > 0) money(billed) else "",
            fields = rows(
                "Item" to entry.itemCode,
                "Pieces" to entry.pieces.takeIf { it > 0 }?.toString(),
                "Rate" to entry.rate.takeIf { it > 0 }?.let { money(it) },
                "Cases / loose" to listOfNotNull(
                    entry.caseCount.takeIf { it > 0 }?.let { "$it case" },
                    entry.loosePieces.takeIf { it > 0 }?.let { "$it loose" }
                ).joinToString(" + "),
                "Order date" to entry.orderDate,
                "Trip" to visit?.visitCode,
                "Salesman" to entry.salesmanName.ifBlank { entry.createdByName },
                "Delivery" to entry.deliveryStatus,
                "Transporter" to entry.transporter,
                "LR number" to entry.lrNo,
                "Payment" to entry.paymentStatus,
                "Paid" to entry.paidAmount.takeIf { it > 0 }?.let { money(it) },
                "Pack note" to entry.mixedPackNote
            ),
            warnings = buildList {
                if (entry.paidAmount > 0) add("${money(entry.paidAmount)} has already been received for this order")
                if (entry.deliveryStatus.equals("Dispatched", true) || entry.deliveryStatus.equals("Delivered", true)) {
                    add("This order is already ${entry.deliveryStatus.lowercase()} — the goods have left")
                }
                if (entry.lrNo.isNotBlank()) add("It has an LR number (${entry.lrNo}), so it is on a transporter's books")
                if (entry.packGroupId != null) add("It is part of a mixed pack; the pack note will stop making sense")
            }
        )
    }

    fun forCustomer(customer: CustomerEntity, trips: Int, orders: Int, outstanding: Double): RecordDetail =
        RecordDetail(
            title = customer.brandName().ifBlank { customer.name },
            subtitle = listOfNotNull(
                customer.name.takeIf { it.isNotBlank() && it != customer.brandName() },
                customer.marketArea.takeIf { it.isNotBlank() } ?: customer.city.takeIf { it.isNotBlank() }
            ).joinToString(" • "),
            amount = if (outstanding > 0) "${money(outstanding)} outstanding" else "",
            fields = rows(
                "Phone" to customer.phone,
                "Customer code" to customer.customerId,
                "Type" to customer.customerType,
                "Credit limit" to customer.creditLimit.takeIf { it > 0 }?.let { money(it) },
                "GSTIN" to customer.gstin,
                "Address" to customer.shopAddress.ifBlank { customer.address },
                "Sub agent" to customer.subAgentName,
                "Trips" to trips.takeIf { it > 0 }?.toString(),
                "Orders" to orders.takeIf { it > 0 }?.toString()
            ),
            warnings = buildList {
                if (trips > 0) add("$trips trip${if (trips == 1) "" else "s"} and $orders order${if (orders == 1) "" else "s"} belong to this customer")
                if (outstanding > 0) add("${money(outstanding)} is still unpaid on their account")
            }
        )

    fun forSupplier(supplier: SupplierEntity, orders: Int, products: Int): RecordDetail =
        RecordDetail(
            title = supplier.brandName().ifBlank { supplier.name },
            subtitle = listOfNotNull(
                supplier.type.takeIf { it.isNotBlank() },
                supplier.marketArea.takeIf { it.isNotBlank() } ?: supplier.city.takeIf { it.isNotBlank() }
            ).joinToString(" • "),
            fields = rows(
                "Phone" to supplier.phone,
                "Supplier code" to supplier.supplierId,
                "Brand" to supplier.brand,
                "GSTIN" to supplier.gstin,
                "Address" to supplier.officeAddress.ifBlank { supplier.address },
                "Categories" to supplier.categories,
                "Orders" to orders.takeIf { it > 0 }?.toString(),
                "Products" to products.takeIf { it > 0 }?.toString()
            ),
            warnings = buildList {
                if (orders > 0) add("$orders order${if (orders == 1) "" else "s"} were bought from them")
                if (products > 0) add("$products product${if (products == 1) "" else "s"} are listed under them")
            }
        )

    fun forProduct(product: ProductEntity, ordersUsingCode: Int): RecordDetail =
        RecordDetail(
            title = product.name.ifBlank { product.productCode },
            subtitle = listOfNotNull(
                product.productCode.takeIf { it.isNotBlank() },
                product.supplierName.takeIf { it.isNotBlank() }
            ).joinToString(" • "),
            fields = rows(
                "Item code" to product.productCode,
                "Category" to product.category,
                "Supplier" to product.supplierName,
                "Rate" to product.defaultRate.takeIf { it > 0 }?.let { money(it) },
                "Case size" to product.defaultCaseSize.takeIf { it > 0 }?.toString(),
                "HSN" to product.hsnCode,
                "Orders using this code" to ordersUsingCode.takeIf { it > 0 }?.toString()
            ),
            warnings = buildList {
                if (ordersUsingCode > 0) {
                    add("$ordersUsingCode order${if (ordersUsingCode == 1) "" else "s"} use this item code; they stay but stop matching the master")
                }
            }
        )

    fun forCheque(cheque: ChequePdcEntity): RecordDetail =
        RecordDetail(
            title = "Cheque ${cheque.chequeNo.ifBlank { cheque.id.toString() }}",
            subtitle = cheque.partyName,
            amount = if (cheque.amount > 0) money(cheque.amount) else "",
            fields = rows(
                "Bank" to cheque.bankName,
                "Cheque date" to cheque.chequeDate,
                "Party" to "${cheque.partyName} (${cheque.partyType})",
                "Status" to cheque.status,
                "Deposited" to cheque.depositDate,
                "Cleared" to cheque.clearedDate,
                "Notes" to cheque.notes
            ),
            warnings = buildList {
                if (cheque.amount > 0) add("This is a money record for ${money(cheque.amount)}")
                if (cheque.status.equals("Cleared", true)) add("It is already marked cleared")
            }
        )

    fun forBrand(brand: BrandEntity, suppliers: Int): RecordDetail =
        RecordDetail(
            title = brand.brandName,
            subtitle = brand.manufacturerName,
            fields = rows(
                "Category" to brand.category,
                "Manufacturer" to brand.manufacturerName,
                "Description" to brand.description,
                "Suppliers linked" to suppliers.takeIf { it > 0 }?.toString()
            ),
            warnings = buildList {
                if (suppliers > 0) add("$suppliers supplier${if (suppliers == 1) "" else "s"} point at this brand and will lose the link")
            }
        )

    fun forTransporter(transporter: TransporterEntity, orders: Int): RecordDetail =
        RecordDetail(
            title = transporter.transporterName,
            subtitle = transporter.city,
            fields = rows(
                "Contact" to transporter.contactPerson,
                "Phone" to transporter.phone1.ifBlank { transporter.phone2 },
                "Office" to transporter.officeAddress,
                "Godown" to transporter.godownAddress,
                "Destinations" to transporter.destinationsCovered,
                "GSTIN" to transporter.gstin,
                "Orders booked" to orders.takeIf { it > 0 }?.toString()
            ),
            warnings = buildList {
                if (orders > 0) add("$orders order${if (orders == 1) "" else "s"} were booked with them")
            }
        )

    fun forMarket(market: MarketEntity, customers: Int, suppliers: Int): RecordDetail =
        RecordDetail(
            title = market.marketName,
            subtitle = listOfNotNull(market.area.takeIf { it.isNotBlank() }, market.city.takeIf { it.isNotBlank() })
                .joinToString(", "),
            fields = rows(
                "City" to market.city,
                "Area" to market.area,
                "Landmark" to market.landmark,
                "Type" to market.marketType,
                "Customers here" to customers.takeIf { it > 0 }?.toString(),
                "Suppliers here" to suppliers.takeIf { it > 0 }?.toString()
            ),
            warnings = buildList {
                if (customers + suppliers > 0) {
                    add("$customers customer${if (customers == 1) "" else "s"} and $suppliers supplier${if (suppliers == 1) "" else "s"} name this market")
                }
            }
        )

    fun forStaff(employee: EmployeeEntity, trips: Int, orders: Int): RecordDetail =
        RecordDetail(
            title = employee.name,
            subtitle = listOfNotNull(
                employee.employeeId.takeIf { it.isNotBlank() },
                Roles.label(employee.role)
            ).joinToString(" • "),
            fields = rows(
                "Phone" to employee.phone,
                "Login email" to employee.email,
                "Role" to Roles.label(employee.role),
                "Status" to employee.status,
                "City" to employee.city,
                "Trips" to trips.takeIf { it > 0 }?.toString(),
                "Orders credited" to orders.takeIf { it > 0 }?.toString()
            ),
            warnings = buildList {
                if (employee.email.isNotBlank()) add("Removing them also removes their login")
                if (trips + orders > 0) add("Their name stays on $trips trip${if (trips == 1) "" else "s"} and $orders order${if (orders == 1) "" else "s"}")
            }
        )

    /** Nothing could be loaded — the record is not on this phone. */
    fun unavailable(label: String, itemId: Long, summary: String): RecordDetail = RecordDetail(
        title = summary.ifBlank { "$label #$itemId" },
        subtitle = "",
        warnings = listOf(
            "The full record is not on this phone, so only the summary from the request is shown. " +
                "It is still safe to decide — approving or rejecting works on the office copy."
        )
    )
}
