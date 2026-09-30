package com.example.util

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.VisitEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * What the sender typed over, before the quotation is printed.
 *
 * Every field is blank by default, and blank means "use whatever the record says". A trip is booked
 * hours before the transporter is decided and days before the LR number exists, so the person sending
 * the PDF often knows something the database does not yet. Rather than make them go and edit the
 * customer master — which would change it for everybody, forever, to suit one document — they correct
 * it here and it applies to this one PDF.
 */
data class ReportOverrides(
    val firmName: String = "",
    val proprietor: String = "",
    val phone: String = "",
    val gstin: String = "",
    val address: String = "",
    val transporter: String = "",
    val bookingStation: String = "",
    val lrNo: String = "",
    val dispatchDate: String = "",
    val deliveryTo: String = ""
) {
    /** True when the sender changed nothing, so the document is purely what the office holds. */
    val isEmpty: Boolean
        get() = firmName.isBlank() && proprietor.isBlank() && phone.isBlank() && gstin.isBlank() &&
            address.isBlank() && transporter.isBlank() && bookingStation.isBlank() &&
            lrNo.isBlank() && dispatchDate.isBlank() && deliveryTo.isBlank()

    /** How many fields were typed over. Printed on the PDF so nobody has to guess it was edited. */
    val editedCount: Int
        get() = listOf(
            firmName, proprietor, phone, gstin, address,
            transporter, bookingStation, lrNo, dispatchDate, deliveryTo
        ).count { it.isNotBlank() }
}

/**
 * The values that actually go on the quotation.
 *
 * One resolver for both paper layouts and for the options sheet. The sheet shows these as the
 * placeholder in each box, so what the sender is about to replace is on screen next to the box they
 * type into — previously the sheet could only hide a field, and the only way to see what it held was
 * to generate the PDF and look.
 */
data class ReportFields(
    val firmName: String,
    val proprietor: String,
    val phone: String,
    val gstin: String,
    val address: String,
    val cityState: String,
    val transporter: String,
    val bookingStation: String,
    val lrNo: String,
    val dispatchDate: String,
    val deliveryTo: String
) {
    companion object {
        /** Printed where a field is genuinely empty. */
        const val BLANK = "—"

        fun resolve(
            visit: VisitEntity,
            customer: CustomerEntity?,
            entries: List<PurchaseEntryEntity>,
            overrides: ReportOverrides = ReportOverrides()
        ): ReportFields {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val cityState = listOfNotNull(
                customer?.city?.trim()?.takeIf { it.isNotBlank() },
                customer?.state?.trim()?.takeIf { it.isNotBlank() }
            ).joinToString(", ")

            // The booked LR numbers, off the orders. "One Bill One LR" only means something when the
            // number it refers to is on the paper.
            val bookedLrs = entries
                .mapNotNull { it.lrNo.trim().takeIf { lr -> lr.isNotBlank() } }
                .distinct()
            val lrFromOrders = when {
                bookedLrs.isEmpty() -> "Not booked yet"
                bookedLrs.size == 1 -> bookedLrs.first()
                else -> "${bookedLrs.first()} +${bookedLrs.size - 1} more"
            }

            return ReportFields(
                firmName = pick(
                    overrides.firmName,
                    customer?.firmName,
                    customer?.name,
                    visit.customerName
                ),
                proprietor = pick(overrides.proprietor, customer?.name),
                phone = pick(overrides.phone, customer?.phone),
                gstin = pick(overrides.gstin, customer?.gstin, fallback = "Unregistered"),
                address = pick(
                    overrides.address,
                    customer?.shopAddress,
                    customer?.address,
                    cityState
                ),
                cityState = cityState.ifBlank { BLANK },
                transporter = pick(
                    overrides.transporter,
                    entries.firstOrNull { it.transporter.isNotBlank() }?.transporter,
                    customer?.preferredTransporterName,
                    fallback = "To be advised"
                ),
                bookingStation = pick(
                    overrides.bookingStation,
                    customer?.transportPreference,
                    customer?.city
                ),
                lrNo = overrides.lrNo.trim().ifBlank { lrFromOrders },
                dispatchDate = pick(overrides.dispatchDate, visit.date, fallback = today),
                deliveryTo = pick(overrides.deliveryTo, cityState)
            )
        }

        /**
         * The first of [candidates] that has something in it, or [fallback].
         *
         * There is deliberately no invented data here. The card layout used to fill an empty phone
         * number with "+91 98765 43210", an empty proprietor with "Mr. Jitendra Bhai" and an unknown
         * transporter with "Shree Maruti Transport" — sample values from a design mock, printed on
         * documents that go to real customers over WhatsApp. A dash tells the reader the truth; a
         * plausible wrong phone number gets somebody called.
         */
        private fun pick(vararg candidates: String?, fallback: String = BLANK): String =
            candidates.firstNotNullOfOrNull { value ->
                value?.trim()?.takeIf { it.isNotBlank() }
            } ?: fallback
    }
}
