package com.example.data.remote

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.TransporterEntity
import com.example.util.PaymentStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.core.utilities.encoding.CustomClassMapper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Keeps the Android app and the web admin on the same records.
 *
 * The web keeps some data as arrays / objects (contacts[], outlets[], factories[], phones[],
 * emails[], system{}, gstNumber, mapLink, pricePerPiece, gstPercent). Android keeps the same data
 * as JSON strings / flat fields. Both are written by both sides:
 *
 * - Write: Android merges its fields into the record (the web-only fields such as security cheques
 *   survive) and refreshes the web copies from the Android values.
 * - Read: when an Android field is empty but the web copy has data (record created on the web),
 *   the Android field is filled from it.
 */
internal object WebFieldBridge {

    /** The same key names setValue would write (Firebase's own mapper), as a mutable map. */
    fun toMap(value: Any): MutableMap<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        val plain = CustomClassMapper.convertToPlainJavaTypes(value) as? Map<String, Any?>
        return plain?.toMutableMap() ?: mutableMapOf()
    }

    // ---------------------------------------------------------------------
    // Write side: web copies computed from the Android values
    // ---------------------------------------------------------------------

    fun customerMirrors(c: CustomerEntity): Map<String, Any?> {
        val out = mutableMapOf<String, Any?>(
            "gstNumber" to c.gstin,
            "mapLink" to c.shopMapLink,
            "phones" to nonBlank(c.phone, c.phone2, c.phone3, c.phone4, c.phone5),
            "emails" to nonBlank(c.email, c.email2)
        )
        jsonToList(c.contactsJson, CONTACT_KEYS)?.let { out["contacts"] = it }
        jsonToList(c.outletsJson, LOCATION_KEYS)?.let { out["outlets"] = it }
        return out
    }

    fun supplierMirrors(s: SupplierEntity): Map<String, Any?> {
        val out = mutableMapOf<String, Any?>(
            "gstNumber" to s.gstin,
            "phones" to nonBlank(s.phone, s.phone2, s.phone3, s.phone4, s.phone5),
            "emails" to nonBlank(s.email, s.email2)
        )
        jsonToList(s.factoriesJson, LOCATION_KEYS)?.let { out["factories"] = it }
        jsonToList(s.outletsJson, LOCATION_KEYS)?.let { out["outlets"] = it }
        if (listOf(s.systemMrpValue, s.systemMrpPercent, s.systemLessValue, s.systemLessPercent).any { it.isNotBlank() }) {
            out["system"] = mapOf(
                "mrp" to mapOf("value" to s.systemMrpValue, "percentage" to s.systemMrpPercent),
                "less" to mapOf("value" to s.systemLessValue, "percentage" to s.systemLessPercent)
            )
        }
        return out
    }

    fun employeeMirrors(e: EmployeeEntity): Map<String, Any?> = mapOf(
        "phones" to nonBlank(e.phone, e.phone2, e.phone3, e.phone4, e.phone5),
        "emails" to nonBlank(e.email, e.alternateEmail).map { it.lowercase() }
    )

    fun orderMirrors(e: PurchaseEntryEntity): Map<String, Any?> = mapOf(
        "pricePerPiece" to e.rate,
        "gstPercent" to e.gstRate
    )

    // ---------------------------------------------------------------------
    // Read side: fill empty Android fields from the web copies
    // ---------------------------------------------------------------------

    fun readCustomer(c: CustomerEntity, s: DataSnapshot): CustomerEntity {
        val phones = s.strings("phones")
        val emails = s.strings("emails")
        return c.copy(
            phone = c.phone.ifBlank { phones.getOrNull(0).orEmpty() },
            phone2 = c.phone2.ifBlank { phones.getOrNull(1).orEmpty() },
            phone3 = c.phone3.ifBlank { phones.getOrNull(2).orEmpty() },
            phone4 = c.phone4.ifBlank { phones.getOrNull(3).orEmpty() },
            phone5 = c.phone5.ifBlank { phones.getOrNull(4).orEmpty() },
            email = c.email.ifBlank { emails.getOrNull(0).orEmpty() },
            email2 = c.email2.ifBlank { emails.getOrNull(1).orEmpty() },
            gstin = c.gstin.ifBlank { s.text("gstNumber") },
            shopMapLink = c.shopMapLink.ifBlank { s.text("mapLink") },
            contactsJson = if (isEmptyJson(c.contactsJson)) s.listAsJson("contacts", CONTACT_KEYS) ?: c.contactsJson else c.contactsJson,
            outletsJson = if (isEmptyJson(c.outletsJson)) s.listAsJson("outlets", LOCATION_KEYS) ?: c.outletsJson else c.outletsJson
        )
    }

    fun readSupplier(sup: SupplierEntity, s: DataSnapshot): SupplierEntity {
        val phones = s.strings("phones")
        val emails = s.strings("emails")
        val system = s.child("system")
        return sup.copy(
            phone = sup.phone.ifBlank { phones.getOrNull(0).orEmpty() },
            phone2 = sup.phone2.ifBlank { phones.getOrNull(1).orEmpty() },
            phone3 = sup.phone3.ifBlank { phones.getOrNull(2).orEmpty() },
            phone4 = sup.phone4.ifBlank { phones.getOrNull(3).orEmpty() },
            phone5 = sup.phone5.ifBlank { phones.getOrNull(4).orEmpty() },
            email = sup.email.ifBlank { emails.getOrNull(0).orEmpty() },
            email2 = sup.email2.ifBlank { emails.getOrNull(1).orEmpty() },
            gstin = sup.gstin.ifBlank { s.text("gstNumber") },
            factoriesJson = if (isEmptyJson(sup.factoriesJson)) s.listAsJson("factories", LOCATION_KEYS) ?: sup.factoriesJson else sup.factoriesJson,
            outletsJson = if (isEmptyJson(sup.outletsJson)) s.listAsJson("outlets", LOCATION_KEYS) ?: sup.outletsJson else sup.outletsJson,
            systemMrpValue = sup.systemMrpValue.ifBlank { system.child("mrp").text("value") },
            systemMrpPercent = sup.systemMrpPercent.ifBlank { system.child("mrp").text("percentage") },
            systemLessValue = sup.systemLessValue.ifBlank { system.child("less").text("value") },
            systemLessPercent = sup.systemLessPercent.ifBlank { system.child("less").text("percentage") }
        )
    }

    fun readEmployee(e: EmployeeEntity, s: DataSnapshot): EmployeeEntity {
        val phones = s.strings("phones")
        val emails = s.strings("emails")
        return e.copy(
            phone = e.phone.ifBlank { phones.getOrNull(0).orEmpty() },
            phone2 = e.phone2.ifBlank { phones.getOrNull(1).orEmpty() },
            phone3 = e.phone3.ifBlank { phones.getOrNull(2).orEmpty() },
            phone4 = e.phone4.ifBlank { phones.getOrNull(3).orEmpty() },
            phone5 = e.phone5.ifBlank { phones.getOrNull(4).orEmpty() },
            email = e.email.ifBlank { emails.getOrNull(0).orEmpty() },
            alternateEmail = e.alternateEmail.ifBlank { emails.getOrNull(1).orEmpty() },
            // The web writes isBlocked; Firebase's mapper only reads "blocked"
            isBlocked = e.isBlocked || s.child("isBlocked").getValue(Boolean::class.java) == true
        )
    }

    fun readOrder(e: PurchaseEntryEntity, s: DataSnapshot): PurchaseEntryEntity {
        val rate = if (e.rate > 0.0) e.rate else s.number("pricePerPiece") ?: e.rate
        // Orders created on the web may only have gstPercent (the Android default would say 5%)
        val gst = if (!s.hasChild("gstRate")) s.number("gstPercent") ?: e.gstRate else e.gstRate
        // Older web builds wrote Unpaid / Paid
        val payment = PaymentStatus.normalize(e.paymentStatus)
        return if (rate == e.rate && gst == e.gstRate && payment == e.paymentStatus) e
        else e.copy(rate = rate, gstRate = gst, paymentStatus = payment)
    }

    fun readTransporter(t: TransporterEntity, s: DataSnapshot): TransporterEntity =
        if (t.phone1.isNotBlank()) t else t.copy(phone1 = s.text("phone"))

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private val CONTACT_KEYS = listOf("name", "phone", "designation", "email")
    private val LOCATION_KEYS = listOf("name", "address", "city", "pincode", "mapLink", "phone")

    private fun nonBlank(vararg values: String): List<String> = values.map { it.trim() }.filter { it.isNotEmpty() }

    private fun isEmptyJson(json: String): Boolean = json.isBlank() || json.trim() == "[]"

    /** Android JSON string -> list of maps for the web; null when there is nothing to write (keep the web copy). */
    private fun jsonToList(json: String, keys: List<String>): List<Map<String, String>>? {
        if (isEmptyJson(json)) return null
        return try {
            val arr = JSONArray(json)
            val list = (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                keys.mapNotNull { k -> obj.optString(k, "").trim().takeIf { it.isNotEmpty() }?.let { k to it } }
                    .toMap()
                    .takeIf { it.isNotEmpty() }
            }
            list.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun DataSnapshot.text(key: String): String = child(key).value?.toString()?.trim().orEmpty()

    private fun DataSnapshot.number(key: String): Double? = when (val v = child(key).value) {
        is Number -> v.toDouble()
        is String -> v.trim().toDoubleOrNull()
        else -> null
    }

    private fun DataSnapshot.strings(key: String): List<String> =
        child(key).children.mapNotNull { it.value?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }

    /** Web array of objects -> Android JSON string; null when the web has nothing either. */
    private fun DataSnapshot.listAsJson(key: String, keys: List<String>): String? {
        val node = child(key)
        if (!node.hasChildren()) return null
        val arr = JSONArray()
        node.children.forEach { item ->
            val obj = JSONObject()
            keys.forEach { k -> item.child(k).value?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { obj.put(k, it) } }
            if (obj.length() > 0) arr.put(obj)
        }
        return if (arr.length() > 0) arr.toString() else null
    }
}
