package com.example.util

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

/**
 * Shared domain helpers. Field names and formats here mirror the web admin
 * (web/src/lib/domain.ts) so both apps read and write the same Realtime Database data.
 *
 * NOTE: keep these as top-level / extension functions. Member properties on the entity
 * classes would be serialized into Firebase by the reflection mapper.
 */

/** Globally unique numeric ids that do not collide across phones and the web admin. */
object IdGenerator {
    private var lastIssued = 0L

    /**
     * epochMillis * 1000 + random(0..999). Stays below JS Number.MAX_SAFE_INTEGER (~9.0e15).
     * Never repeats on this device, even when called several times in the same millisecond.
     */
    @Synchronized
    fun newId(): Long {
        var id = System.currentTimeMillis() * 1000L + Random.nextInt(0, 1000)
        if (id <= lastIssued) id = lastIssued + 1
        lastIssued = id
        return id
    }
}

/** The three user types. Stored in EmployeeEntity.role (employees node). */
object Roles {
    const val ADMIN = "Admin"
    /** Stored value kept as "Salesman" for backward compatibility; shown as "Staff". */
    const val STAFF = "Salesman"
    /** Sub Agent: external person who brings customers. */
    const val AGENT = "Agent"

    fun isAdmin(role: String?): Boolean = role.equals(ADMIN, ignoreCase = true)
    fun isAgent(role: String?): Boolean =
        role.equals(AGENT, ignoreCase = true) || role.equals("Sub Agent", ignoreCase = true)
    fun isStaff(role: String?): Boolean = !isAdmin(role) && !isAgent(role)

    fun label(role: String?): String = when {
        isAdmin(role) -> "Admin"
        isAgent(role) -> "Sub Agent"
        else -> "Staff"
    }
}

fun EmployeeEntity.isSubAgent(): Boolean = Roles.isAgent(role)

// -----------------------------------------------------------------------------
// Undoing a soft delete: wipe every "this is deleted" field so the record is live again
// -----------------------------------------------------------------------------

// -----------------------------------------------------------------------------
// Soft delete: hide the record and record who asked, so an admin can confirm or undo it
// -----------------------------------------------------------------------------

private const val DELETION_PENDING = "PENDING_CONFIRMATION"

fun VisitEntity.markedDeleted(by: String, email: String, role: String): VisitEntity = copy(
    isDeleted = true, deletedAt = System.currentTimeMillis(), deletedBy = by,
    deletedByEmail = email, deletedByRole = role, deletionStatus = DELETION_PENDING
)

fun PurchaseEntryEntity.markedDeleted(by: String, email: String, role: String): PurchaseEntryEntity = copy(
    isDeleted = true, deletedAt = System.currentTimeMillis(), deletedBy = by,
    deletedByEmail = email, deletedByRole = role, deletionStatus = DELETION_PENDING
)

fun CustomerEntity.markedDeleted(by: String, email: String, role: String): CustomerEntity = copy(
    isDeleted = true, deletedAt = System.currentTimeMillis(), deletedBy = by,
    deletedByEmail = email, deletedByRole = role, deletionStatus = DELETION_PENDING
)

fun SupplierEntity.markedDeleted(by: String, email: String, role: String): SupplierEntity = copy(
    isDeleted = true, deletedAt = System.currentTimeMillis(), deletedBy = by,
    deletedByEmail = email, deletedByRole = role, deletionStatus = DELETION_PENDING
)

fun ProductEntity.markedDeleted(by: String, email: String, role: String): ProductEntity = copy(
    isDeleted = true, deletedAt = System.currentTimeMillis(), deletedBy = by,
    deletedByEmail = email, deletedByRole = role, deletionStatus = DELETION_PENDING
)

fun VisitEntity.cleared(): VisitEntity = copy(
    isDeleted = false, deletedAt = null, deletedBy = "", deletedByEmail = "",
    deletedByRole = "", deletionStatus = "", deletionReason = ""
)

fun PurchaseEntryEntity.cleared(): PurchaseEntryEntity = copy(
    isDeleted = false, deletedAt = null, deletedBy = "", deletedByEmail = "",
    deletedByRole = "", deletionStatus = "", deletionReason = ""
)

fun CustomerEntity.cleared(): CustomerEntity = copy(
    isDeleted = false, deletedAt = null, deletedBy = "", deletedByEmail = "",
    deletedByRole = "", deletionStatus = "", deletionReason = ""
)

fun SupplierEntity.cleared(): SupplierEntity = copy(
    isDeleted = false, deletedAt = null, deletedBy = "", deletedByEmail = "",
    deletedByRole = "", deletionStatus = "", deletionReason = ""
)

fun ProductEntity.cleared(): ProductEntity = copy(
    isDeleted = false, deletedAt = null, deletedBy = "", deletedByEmail = "",
    deletedByRole = "", deletionStatus = "", deletionReason = ""
)

// -----------------------------------------------------------------------------
// Customer brand name: trips, orders and reports show the shop / firm name, not the owner
// -----------------------------------------------------------------------------

/** The customer's brand (shop / firm) name, falling back to the owner name. */
fun CustomerEntity.brandName(): String = firmName.trim().ifBlank { name.trim() }

/** The same trip with customerName replaced by the linked customer's brand name (display only). */
fun VisitEntity.withBrandName(customersById: Map<Long, CustomerEntity>): VisitEntity {
    val brand = customersById[customerId]?.brandName().orEmpty()
    return if (brand.isNotBlank() && brand != customerName) copy(customerName = brand) else this
}

fun List<VisitEntity>.withBrandNames(customers: List<CustomerEntity>): List<VisitEntity> {
    if (isEmpty() || customers.isEmpty()) return this
    val byId = customers.associateBy { it.id }
    return map { it.withBrandName(byId) }
}

/** The supplier's brand name, falling back to the firm / supplier name. */
fun SupplierEntity.brandName(): String = brand.trim().ifBlank { name.trim() }.ifBlank { firmName.trim() }

/** The supplier's own primary number: the first filled phone slot (never our office number). */
fun SupplierEntity.primaryPhone(): String =
    listOf(phone, phone2, phone3, phone4, phone5).map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()

// -----------------------------------------------------------------------------
// PDF file names: the party's brand name leads, so WhatsApp shows whose document it is
// -----------------------------------------------------------------------------

object PdfFileNames {
    private val illegalChars = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    /** e.g. "Shree Fashion - Order Form - HT-0012.pdf" */
    fun build(brand: String, document: String, code: String = ""): String {
        val cleanBrand = clean(brand).take(60).trim().ifBlank { "Himat Textile" }
        val cleanCode = clean(code).take(40).trim()
        return listOf(cleanBrand, document, cleanCode).filter { it.isNotBlank() }.joinToString(" - ") + ".pdf"
    }

    private fun clean(value: String): String =
        value.replace(illegalChars, " ").replace(Regex("\\s+"), " ").trim().trim('.').trim()
}

// -----------------------------------------------------------------------------
// Trips with multiple salesmen
// -----------------------------------------------------------------------------

data class TripMember(val id: Long, val name: String)

object TripMembers {
    fun parseIds(raw: String): List<Long> =
        raw.split(",").mapNotNull { it.trim().toLongOrNull() }.filter { it > 0L }

    fun parseNames(raw: String): List<String> =
        raw.split(",").map { it.trim() }

    fun encode(members: List<TripMember>): Pair<String, String> =
        members.joinToString(",") { it.id.toString() } to members.joinToString(", ") { it.name.replace(",", " ") }
}

/** Starter first, then legacy co-agent, then everybody who joined. Distinct by id. */
fun VisitEntity.tripMembers(employees: List<EmployeeEntity> = emptyList()): List<TripMember> {
    val result = LinkedHashMap<Long, String>()
    fun add(id: Long, fallbackName: String) {
        if (id <= 0L || result.containsKey(id)) return
        val resolved = employees.firstOrNull { it.id == id }?.name?.takeIf { it.isNotBlank() }
        result[id] = resolved ?: fallbackName.trim()
    }
    add(employeeId, employeeName)
    add(secondaryEmployeeId, secondaryEmployeeName)
    val ids = TripMembers.parseIds(memberIds)
    val names = TripMembers.parseNames(memberNames)
    ids.forEachIndexed { index, id -> add(id, names.getOrNull(index).orEmpty()) }
    return result.map { (id, name) -> TripMember(id, name.ifBlank { "Salesman #$id" }) }
}

fun VisitEntity.membersDisplay(employees: List<EmployeeEntity> = emptyList()): String =
    tripMembers(employees).joinToString(", ") { it.name }

/** True when the employee started, co-owns or joined the trip (id match, legacy name match). */
fun VisitEntity.hasMember(emp: EmployeeEntity?): Boolean {
    if (emp == null) return false
    if (tripMembers().any { it.id == emp.id }) return true
    val name = emp.name.trim()
    if (name.isBlank()) return false
    return employeeName.trim().equals(name, ignoreCase = true) ||
        secondaryEmployeeName.trim().equals(name, ignoreCase = true)
}

fun VisitEntity.isClosed(): Boolean =
    status.equals("Completed", ignoreCase = true) || status.equals("Closed", ignoreCase = true)

fun VisitEntity.withMember(id: Long, name: String): VisitEntity {
    val members = tripMembers().toMutableList()
    if (members.none { it.id == id }) members += TripMember(id, name.trim())
    val (ids, names) = TripMembers.encode(members)
    return copy(memberIds = ids, memberNames = names)
}

/** Salesman names to print on the customer report: per-order salesmen first, then trip members. */
fun salesmenForReport(visit: VisitEntity, entries: List<PurchaseEntryEntity>): List<String> {
    val names = LinkedHashSet<String>()
    entries.forEach { e -> e.salesmanName.trim().takeIf { it.isNotBlank() }?.let { names += it } }
    visit.tripMembers().forEach { names += it.name }
    return names.toList().distinctBy { it.lowercase(Locale.getDefault()) }
}

// -----------------------------------------------------------------------------
// Referred By
// -----------------------------------------------------------------------------

object ReferrerTypes {
    const val STAFF = "Staff"
    const val AGENT = "Agent"
    const val CUSTOMER = "Customer"
    const val SUPPLIER = "Supplier"
    const val BROKER = "Broker"

    private fun normalizeType(raw: String): String = when (raw.trim().lowercase(Locale.ROOT)) {
        "staff", "salesman", "employee", "staff agent" -> STAFF
        "agent", "sub agent", "subagent", "sub-agent" -> AGENT
        "customer", "retailer" -> CUSTOMER
        "supplier", "mill", "supplier/mill", "manufacturer" -> SUPPLIER
        "broker", "introducer" -> BROKER
        else -> ""
    }

    /** "Customer: Balaji Sarees" -> ("Customer", "Balaji Sarees"). Legacy "(agent)" suffixes are understood. */
    fun parse(value: String): Pair<String, String> {
        val v = value.trim()
        if (v.isEmpty()) return "" to ""
        val idx = v.indexOf(':')
        if (idx > 0) {
            val type = normalizeType(v.substring(0, idx))
            if (type.isNotEmpty()) return type to v.substring(idx + 1).trim()
        }
        val suffix = Regex("\\((agent|staff|customer|supplier)\\)\\s*$", RegexOption.IGNORE_CASE).find(v)
        if (suffix != null) {
            // Before v18 "(agent)" meant a salesman
            val type = if (suffix.groupValues[1].equals("agent", true)) STAFF else normalizeType(suffix.groupValues[1])
            return type to v.substring(0, suffix.range.first).trim()
        }
        // Free text without a type prefix (older records)
        return "" to v
    }

    fun format(type: String, name: String): String = "$type: ${name.trim()}"
}

/**
 * Does a record's referrer point at the target entity?
 * Structured id match wins; records saved before v18 only have the display string, so fall back to a name match.
 */
fun isReferredBy(
    referredBy: String,
    referredByType: String,
    referredById: Long?,
    targetType: String,
    targetId: Long,
    targetNames: List<String>
): Boolean {
    if (referredById != null && referredById > 0L && referredByType.isNotBlank()) {
        return referredByType.equals(targetType, ignoreCase = true) && referredById == targetId
    }
    val (type, name) = ReferrerTypes.parse(referredBy)
    if (name.isBlank() || type == ReferrerTypes.BROKER) return false
    if (type.isNotEmpty() && !type.equals(targetType, ignoreCase = true)) return false
    return targetNames.any { it.isNotBlank() && it.trim().equals(name, ignoreCase = true) }
}

// -----------------------------------------------------------------------------
// Date range filter shared by all list / detail screens
// -----------------------------------------------------------------------------

enum class DatePreset(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7("Last 7 Days"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom")
}

data class DateRangeFilter(
    val preset: DatePreset = DatePreset.ALL,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val customLabel: String = ""
) {
    val label: String
        get() = if (preset == DatePreset.CUSTOM && customLabel.isNotBlank()) customLabel else preset.label

    val isActive: Boolean get() = preset != DatePreset.ALL

    /** Inclusive yyyy-MM-dd bounds, null = open ended. */
    fun bounds(now: Calendar = Calendar.getInstance()): Pair<String?, String?> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fun day(offset: Int): String {
            val c = now.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, offset)
            return fmt.format(c.time)
        }
        return when (preset) {
            DatePreset.ALL -> null to null
            DatePreset.TODAY -> day(0) to day(0)
            DatePreset.YESTERDAY -> day(-1) to day(-1)
            DatePreset.LAST_7 -> day(-6) to day(0)
            DatePreset.THIS_MONTH -> {
                val c = now.clone() as Calendar
                c.set(Calendar.DAY_OF_MONTH, 1)
                fmt.format(c.time) to day(0)
            }
            DatePreset.LAST_MONTH -> {
                val start = now.clone() as Calendar
                start.add(Calendar.MONTH, -1)
                start.set(Calendar.DAY_OF_MONTH, 1)
                val end = start.clone() as Calendar
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                fmt.format(start.time) to fmt.format(end.time)
            }
            DatePreset.CUSTOM -> {
                val s = customStartMillis?.let { fmt.format(java.util.Date(it)) }
                val e = customEndMillis?.let { fmt.format(java.util.Date(it)) }
                s to e
            }
        }
    }

    /** [date] is yyyy-MM-dd (longer strings are cut to the first 10 chars). */
    fun matches(date: String?): Boolean {
        if (preset == DatePreset.ALL) return true
        val d = date?.trim()?.take(10).orEmpty()
        if (d.length < 10) return false
        val (start, end) = bounds()
        if (start != null && d < start) return false
        if (end != null && d > end) return false
        return true
    }
}

/** The date an order belongs to: its own order date, else the trip date. */
fun PurchaseEntryEntity.effectiveDate(visit: VisitEntity?): String =
    orderDate.ifBlank { visit?.date.orEmpty() }

/** Delivery is the only status that decides whether an order is done. Payment is optional. */
fun PurchaseEntryEntity.isDelivered(): Boolean = deliveryStatus.equals("Delivered", ignoreCase = true)

/**
 * Payment status (optional). Stored words are Pending / Partial / Received; older web builds wrote
 * Unpaid / Paid, which are read as the same thing.
 */
object PaymentStatus {
    const val PENDING = "Pending"
    const val PARTIAL = "Partial"
    const val RECEIVED = "Received"

    fun normalize(raw: String?): String = when (raw?.trim()?.lowercase()) {
        "received", "paid", "cleared" -> RECEIVED
        "partial" -> PARTIAL
        else -> PENDING
    }

    fun isReceived(raw: String?): Boolean = normalize(raw) == RECEIVED
}
