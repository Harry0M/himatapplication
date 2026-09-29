/**
 * How the customer placed the order — the same values as the Android app (util/TripTypes.kt).
 *
 * "Market" (the default, and what every older record means): the customer came along and the
 * salesman took them from supplier to supplier.
 * "Phone": the customer ordered by phone, so the salesman books each supplier's order from the office.
 */
export const TRIP_TYPE_MARKET = "Market"
export const TRIP_TYPE_PHONE = "Phone"

/** Blank, missing or anything else counts as a market visit. */
export function isPhoneTrip(visit?: { tripType?: string } | null): boolean {
  return (visit?.tripType || "").trim().toLowerCase() === "phone"
}

export function tripTypeLabel(visit?: { tripType?: string } | null): string {
  return isPhoneTrip(visit) ? "Phone order" : "Market visit"
}
