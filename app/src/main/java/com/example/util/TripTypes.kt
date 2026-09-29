package com.example.util

import com.example.data.local.entity.VisitEntity

/**
 * How the customer placed the order.
 *
 * "Market" (the default, and what every older record means): the customer came along and the
 * salesman took them from supplier to supplier.
 * "Phone": the customer ordered by phone, so the salesman books each supplier's order from the office.
 *
 * A phone order is still a normal trip with normal orders, so every report, filter and total keeps
 * working; only the label and the way the orders are entered differ.
 */
object TripTypes {
    const val MARKET = "Market"
    const val PHONE = "Phone"

    /** Blank, missing or anything else counts as a market visit. */
    fun isPhone(raw: String?): Boolean = raw?.trim().equals(PHONE, ignoreCase = true)

    fun label(raw: String?): String = if (isPhone(raw)) "Phone order" else "Market visit"
}

fun VisitEntity.isPhoneTrip(): Boolean = TripTypes.isPhone(tripType)
