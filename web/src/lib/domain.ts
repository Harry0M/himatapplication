/**
 * Shared domain helpers for the web admin.
 *
 * Field names and formats mirror the Android app (app/src/main/java/com/example/util/DomainUtils.kt)
 * so both apps read and write the same Realtime Database records.
 */
import type { Customer, Employee, PurchaseEntry, Visit } from "../types"

// -----------------------------------------------------------------------------
// Ids
// -----------------------------------------------------------------------------

let lastIssuedId = 0

/**
 * Globally unique numeric id: epochMillis * 1000 + random(0..999).
 * Same scheme as Android IdGenerator.newId(). Stays below Number.MAX_SAFE_INTEGER.
 * (max + 1 and Date.now() collided when two people saved at the same moment.)
 * Never repeats inside this browser tab, even when called many times in the same millisecond.
 */
export function newId(): number {
  let id = Date.now() * 1000 + Math.floor(Math.random() * 1000)
  if (id <= lastIssuedId) id = lastIssuedId + 1
  lastIssuedId = id
  return id
}

/** Human readable code, e.g. CUST-260928-417 / SUP-260928-102 (same format as Android). */
export function displayCode(prefix: string, date: Date = new Date()): string {
  const yy = String(date.getFullYear()).slice(-2)
  const mm = String(date.getMonth() + 1).padStart(2, "0")
  const dd = String(date.getDate()).padStart(2, "0")
  const rand = 100 + Math.floor(Math.random() * 900)
  return `${prefix}-${yy}${mm}${dd}-${rand}`
}

/** Highest number a real staff code can carry. Anything larger is an id that leaked into the field. */
export const MAX_STAFF_CODE = 9999

/**
 * The next free staff code, e.g. "EMP-07". Mirrors Android's StaffCodes.next().
 *
 * `list.length + 1` was the old version and it handed out the same code twice as soon as anything
 * was missing from the list or two admins were adding people at once. This reads the highest number
 * actually in use — counting deactivated staff, so a number is never reused after somebody leaves.
 */
export function nextStaffCode(
  existing: Array<{ employeeId?: string }>,
  prefix: string = "EMP"
): string {
  const used = new Set<number>()
  existing.forEach((e) => {
    const raw = (e.employeeId || "").trim()
    if (!raw.toUpperCase().startsWith(prefix.toUpperCase())) return
    const n = Number(raw.slice(prefix.length).replace(/^[-_\s]+/, ""))
    // MAX_STAFF_CODE: an agency has staff, not thousands. A bigger number means the "code" was
    // really an id that leaked into this field, and counting it would send the next code to infinity.
    if (Number.isInteger(n) && n > 0 && n <= MAX_STAFF_CODE) used.add(n)
  })
  let candidate = (used.size ? Math.max(...used) : 0) + 1
  while (used.has(candidate)) candidate++
  return `${prefix}-${candidate < 10 ? `0${candidate}` : candidate}`
}

/** True when this staff code already belongs to somebody else. */
export function isStaffCodeTaken(
  code: string,
  existing: Array<{ id?: number; employeeId?: string }>,
  selfId = 0
): boolean {
  const wanted = code.trim().toLowerCase()
  if (!wanted) return false
  return existing.some(
    (e) => Number(e.id) !== Number(selfId) && (e.employeeId || "").trim().toLowerCase() === wanted
  )
}

/** Parses ids that may have been written as strings by older builds. Returns 0 when not numeric. */
export function toNumericId(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value
  if (typeof value === "string" && /^\d+$/.test(value.trim())) return Number(value.trim())
  return 0
}

// -----------------------------------------------------------------------------
// Roles (stored in employees/<id>/role)
// -----------------------------------------------------------------------------

export const Roles = {
  ADMIN: "Admin",
  /** Stored as "Salesman" for backward compatibility, shown as "Staff". */
  STAFF: "Salesman",
  /** Sub Agent: an outside person who brings customers to us. */
  AGENT: "Agent",
} as const

export type AppRole = "admin" | "staff" | "agent"

export function isAdminRole(role?: string | null): boolean {
  const r = (role || "").trim().toLowerCase()
  return r === "admin" || r === "super_admin" || r === "owner"
}

export function isAgentRole(role?: string | null): boolean {
  const r = (role || "").trim().toLowerCase()
  return r === "agent" || r === "sub agent" || r === "subagent" || r === "sub-agent"
}

export function isStaffRole(role?: string | null): boolean {
  return !isAdminRole(role) && !isAgentRole(role)
}

export function roleLabel(role?: string | null): string {
  if (isAdminRole(role)) return "Admin"
  if (isAgentRole(role)) return "Sub Agent"
  return "Staff"
}

export function isSubAgent(emp?: Pick<Employee, "role"> | null): boolean {
  return Boolean(emp && isAgentRole(emp.role))
}

// -----------------------------------------------------------------------------
// Trips with multiple salesmen
// -----------------------------------------------------------------------------

export interface TripMember {
  id: number
  name: string
}

export function parseMemberIds(raw?: string | null): number[] {
  return String(raw || "")
    .split(",")
    .map((s) => Number(s.trim()))
    .filter((n) => Number.isFinite(n) && n > 0)
}

export function parseMemberNames(raw?: string | null): string[] {
  return String(raw || "")
    .split(",")
    .map((s) => s.trim())
}

export function encodeMembers(members: TripMember[]): { memberIds: string; memberNames: string } {
  return {
    memberIds: members.map((m) => String(m.id)).join(","),
    memberNames: members.map((m) => m.name.replace(/,/g, " ").trim()).join(", "),
  }
}

/** Starter first, then the legacy co-agent, then everybody who joined. Distinct by id. */
export function tripMembers(visit: Visit, employees: Employee[] = []): TripMember[] {
  const result = new Map<number, string>()
  const add = (rawId: unknown, fallbackName?: string) => {
    const id = toNumericId(rawId)
    if (id <= 0 || result.has(id)) return
    const resolved = employees.find((e) => Number(e.id) === id)?.name?.trim()
    result.set(id, resolved || (fallbackName || "").trim())
  }
  add(visit.employeeId, visit.employeeName)
  add(visit.secondaryEmployeeId, visit.secondaryEmployeeName)
  const ids = parseMemberIds(visit.memberIds)
  const names = parseMemberNames(visit.memberNames)
  ids.forEach((id, index) => add(id, names[index]))
  return Array.from(result.entries()).map(([id, name]) => ({ id, name: name || `Salesman #${id}` }))
}

export function membersDisplay(visit: Visit, employees: Employee[] = []): string {
  return tripMembers(visit, employees)
    .map((m) => m.name)
    .join(", ")
}

/** True when the employee started, co-owns or joined the trip (id match, legacy name match). */
export function hasMember(visit: Visit, emp?: { id: number; name?: string } | null): boolean {
  if (!emp) return false
  if (tripMembers(visit).some((m) => m.id === Number(emp.id))) return true
  const name = (emp.name || "").trim().toLowerCase()
  if (!name) return false
  return (
    (visit.employeeName || "").trim().toLowerCase() === name ||
    (visit.secondaryEmployeeName || "").trim().toLowerCase() === name
  )
}

export function isTripClosed(visit: Pick<Visit, "status">): boolean {
  const s = (visit.status || "").trim().toLowerCase()
  return s === "completed" || s === "closed"
}

/** Member fields after adding someone to the trip. */
export function withMember(visit: Visit, id: number, name: string): { memberIds: string; memberNames: string } {
  const members = tripMembers(visit)
  if (!members.some((m) => m.id === id)) members.push({ id, name: name.trim() })
  return encodeMembers(members)
}

/** Salesman names for the customer report: per-order salesmen first, then trip members. */
export function salesmenForReport(visit: Visit | undefined | null, entries: PurchaseEntry[]): string[] {
  const seen = new Set<string>()
  const names: string[] = []
  const push = (raw?: string) => {
    const n = (raw || "").trim()
    if (!n) return
    const key = n.toLowerCase()
    if (seen.has(key)) return
    seen.add(key)
    names.push(n)
  }
  entries.forEach((e) => push(e.salesmanName))
  if (visit) tripMembers(visit).forEach((m) => push(m.name))
  return names
}

// -----------------------------------------------------------------------------
// Referred By
// -----------------------------------------------------------------------------

export const ReferrerTypes = {
  STAFF: "Staff",
  AGENT: "Agent",
  CUSTOMER: "Customer",
  SUPPLIER: "Supplier",
  BROKER: "Broker",
} as const

export type ReferrerType = (typeof ReferrerTypes)[keyof typeof ReferrerTypes]

export function normalizeReferrerType(raw?: string | null): ReferrerType | "" {
  switch ((raw || "").trim().toLowerCase()) {
    case "staff":
    case "salesman":
    case "employee":
    case "staff agent":
      return ReferrerTypes.STAFF
    case "agent":
    case "sub agent":
    case "subagent":
    case "sub-agent":
      return ReferrerTypes.AGENT
    case "customer":
    case "retailer":
      return ReferrerTypes.CUSTOMER
    case "supplier":
    case "mill":
    case "supplier/mill":
    case "manufacturer":
      return ReferrerTypes.SUPPLIER
    case "broker":
    case "introducer":
      return ReferrerTypes.BROKER
    default:
      return ""
  }
}

/** "Customer: Balaji Sarees" -> { type: "Customer", name: "Balaji Sarees" }. Legacy "(agent)" suffixes are understood. */
export function parseReferrer(value?: string | null): { type: ReferrerType | ""; name: string } {
  const v = (value || "").trim()
  if (!v) return { type: "", name: "" }
  const idx = v.indexOf(":")
  if (idx > 0) {
    const type = normalizeReferrerType(v.substring(0, idx))
    if (type) return { type, name: v.substring(idx + 1).trim() }
  }
  const suffix = /\((agent|staff|customer|supplier)\)\s*$/i.exec(v)
  if (suffix) {
    // Before v18 "(agent)" meant a salesman
    const type = suffix[1].toLowerCase() === "agent" ? ReferrerTypes.STAFF : normalizeReferrerType(suffix[1])
    return { type, name: v.substring(0, suffix.index).trim() }
  }
  return { type: "", name: v }
}

export function formatReferrer(type: string, name: string): string {
  return `${type}: ${name.trim()}`
}

export interface ReferrerFields {
  referredBy?: string
  referredByType?: string
  referredById?: number | string | null
}

/** A picked referrer: the display string plus the structured type and id. */
export interface ReferrerSelection {
  referredBy: string
  referredByType: string
  referredById: number
}

/**
 * Does a record's referrer point at the target entity?
 * A structured id match wins; records saved before the restructure only have the display string,
 * so fall back to an exact name match.
 */
export function isReferredBy(
  record: ReferrerFields,
  targetType: ReferrerType,
  targetId: number,
  targetNames: Array<string | undefined>
): boolean {
  const refId = toNumericId(record.referredById)
  if (refId > 0 && (record.referredByType || "").trim()) {
    return (record.referredByType || "").trim().toLowerCase() === targetType.toLowerCase() && refId === Number(targetId)
  }
  const { type, name } = parseReferrer(record.referredBy)
  if (!name || type === ReferrerTypes.BROKER) return false
  if (type && type.toLowerCase() !== targetType.toLowerCase()) return false
  const lower = name.toLowerCase()
  return targetNames.some((n) => Boolean(n && n.trim()) && n!.trim().toLowerCase() === lower)
}

// -----------------------------------------------------------------------------
// Date range filter shared by list and detail views
// -----------------------------------------------------------------------------

export type DatePreset = "ALL" | "TODAY" | "YESTERDAY" | "LAST_7" | "THIS_MONTH" | "LAST_MONTH" | "CUSTOM"

export const DATE_PRESETS: Array<{ value: DatePreset; label: string }> = [
  { value: "ALL", label: "All Time" },
  { value: "TODAY", label: "Today" },
  { value: "YESTERDAY", label: "Yesterday" },
  { value: "LAST_7", label: "Last 7 Days" },
  { value: "THIS_MONTH", label: "This Month" },
  { value: "LAST_MONTH", label: "Last Month" },
  { value: "CUSTOM", label: "Custom" },
]

export interface DateRange {
  preset: DatePreset
  /** yyyy-MM-dd, only used with CUSTOM */
  start?: string
  /** yyyy-MM-dd, only used with CUSTOM */
  end?: string
}

export const ALL_TIME: DateRange = { preset: "ALL" }

/** Local yyyy-MM-dd (toISOString would shift the day for IST users). */
export function toYmd(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, "0")
  const day = String(d.getDate()).padStart(2, "0")
  return `${y}-${m}-${day}`
}

export function todayYmd(): string {
  return toYmd(new Date())
}

/** Inclusive yyyy-MM-dd bounds; undefined = open ended. */
export function dateRangeBounds(range: DateRange, now: Date = new Date()): { start?: string; end?: string } {
  const day = (offset: number) => {
    const d = new Date(now)
    d.setDate(d.getDate() + offset)
    return toYmd(d)
  }
  switch (range.preset) {
    case "ALL":
      return {}
    case "TODAY":
      return { start: day(0), end: day(0) }
    case "YESTERDAY":
      return { start: day(-1), end: day(-1) }
    case "LAST_7":
      return { start: day(-6), end: day(0) }
    case "THIS_MONTH":
      return { start: toYmd(new Date(now.getFullYear(), now.getMonth(), 1)), end: day(0) }
    case "LAST_MONTH":
      return {
        start: toYmd(new Date(now.getFullYear(), now.getMonth() - 1, 1)),
        end: toYmd(new Date(now.getFullYear(), now.getMonth(), 0)),
      }
    case "CUSTOM": {
      let start = range.start || undefined
      let end = range.end || undefined
      if (start && end && start > end) [start, end] = [end, start]
      return { start, end }
    }
  }
}

/** Normalizes yyyy-MM-dd strings, ISO strings and epoch millis to yyyy-MM-dd. */
export function normalizeDate(value?: string | number | null): string {
  if (value === undefined || value === null || value === "") return ""
  if (typeof value === "number") return value > 0 ? toYmd(new Date(value)) : ""
  const s = String(value).trim()
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.slice(0, 10)
  // dd-MM-yyyy or dd/MM/yyyy
  const dmy = /^(\d{2})[-/](\d{2})[-/](\d{4})/.exec(s)
  if (dmy) return `${dmy[3]}-${dmy[2]}-${dmy[1]}`
  const parsed = new Date(s)
  return isNaN(parsed.getTime()) ? "" : toYmd(parsed)
}

export function matchesDateRange(value: string | number | undefined | null, range: DateRange): boolean {
  if (range.preset === "ALL") return true
  const d = normalizeDate(value)
  if (d.length < 10) return false
  const { start, end } = dateRangeBounds(range)
  if (start && d < start) return false
  if (end && d > end) return false
  return true
}

export function dateRangeLabel(range: DateRange): string {
  if (range.preset === "CUSTOM") {
    const { start, end } = dateRangeBounds(range)
    if (start && end) return start === end ? start : `${start} → ${end}`
    if (start) return `From ${start}`
    if (end) return `Till ${end}`
  }
  return DATE_PRESETS.find((p) => p.value === range.preset)?.label || "All Time"
}

/** The date an order belongs to: its own order date, else the trip date, else when it was created. */
export function effectiveOrderDate(entry: PurchaseEntry, visit?: Visit | null): string {
  return normalizeDate(entry.orderDate) || normalizeDate(visit?.date) || normalizeDate(entry.createdAt)
}

/** Delivery is the only status that decides whether an order is done. Payment is optional. */
export function isDelivered(entry: Pick<PurchaseEntry, "deliveryStatus">): boolean {
  return (entry.deliveryStatus || "").trim().toLowerCase() === "delivered"
}

// -----------------------------------------------------------------------------
// Payment status (optional). The database keeps the Android words (Pending / Partial / Received)
// so every app version reads it; the web shows Unpaid / Partial / Paid.
// -----------------------------------------------------------------------------
export type WebPaymentStatus = "Unpaid" | "Partial" | "Paid"
export const WEB_PAYMENT_STATUSES: WebPaymentStatus[] = ["Unpaid", "Partial", "Paid"]

function paymentKind(raw?: string | null): "paid" | "partial" | "unpaid" {
  const s = (raw || "").trim().toLowerCase()
  if (s === "paid" || s === "received" || s === "cleared") return "paid"
  if (s === "partial") return "partial"
  return "unpaid"
}

/** Any stored value -> the web word. */
export function webPaymentStatus(raw?: string | null): WebPaymentStatus {
  const k = paymentKind(raw)
  return k === "paid" ? "Paid" : k === "partial" ? "Partial" : "Unpaid"
}

/** Any value -> the word saved in the database (same as the Android app). */
export function dbPaymentStatus(raw?: string | null): "Received" | "Partial" | "Pending" {
  const k = paymentKind(raw)
  return k === "paid" ? "Received" : k === "partial" ? "Partial" : "Pending"
}

// -----------------------------------------------------------------------------
// "Show everything linked to this master" lookups (mirror Android RelatedLogic)
// -----------------------------------------------------------------------------

const sameName = (a?: string, b?: string) =>
  Boolean(a && b && a.trim() && a.trim().toLowerCase() === b.trim().toLowerCase())

/**
 * Records (customers, suppliers or people) whose "Referred By" points at the target.
 * Pass the same list the target lives in as `all` with `excludeSelf` so nobody refers themselves.
 */
export function referredRecords<T extends ReferrerFields & { id: number | string; isDeleted?: boolean }>(
  all: T[],
  type: ReferrerType,
  id: number,
  names: Array<string | undefined>,
  excludeSelf = false
): T[] {
  return all.filter(
    (r) => !r.isDeleted && !(excludeSelf && Number(r.id) === Number(id)) && isReferredBy(r, type, id, names)
  )
}

/** Customers a Sub Agent brought in (by id; name match only for records saved before ids existed). */
export function customersOfSubAgent<T extends { subAgentId?: number | string; subAgentName?: string; isDeleted?: boolean }>(
  all: T[],
  agent: { id: number; name?: string }
): T[] {
  return all.filter((c) => {
    if (c.isDeleted) return false
    const linkedId = toNumericId(c.subAgentId)
    if (linkedId > 0) return linkedId === Number(agent.id)
    return sameName(c.subAgentName, agent.name)
  })
}

/** Customers a staff member added / handles. */
export function customersAddedBy<T extends { addedByAgentId?: number | string; addedByAgentName?: string; isDeleted?: boolean }>(
  all: T[],
  emp: { id: number; name?: string }
): T[] {
  return all.filter((c) => {
    if (c.isDeleted) return false
    const linkedId = toNumericId(c.addedByAgentId)
    if (linkedId > 0) return linkedId === Number(emp.id)
    return sameName(c.addedByAgentName, emp.name)
  })
}

export function tripsOfCustomers(visits: Visit[], customerIds: Set<number>): Visit[] {
  return visits.filter((v) => !v.isDeleted && customerIds.has(Number(v.customerId)))
}

/**
 * The customer of one trip: by id, else (a trip saved without / with a stale customer id) the only
 * customer whose shop or owner name matches exactly. Null when unknown or when two customers match.
 * Same rule as Android RelatedLogic.customerOfTrip.
 */
export function customerOfTrip(visit: Visit, customers: Customer[]): Customer | null {
  const id = toNumericId(visit.customerId)
  if (id > 0) {
    const byId = customers.find((c) => toNumericId(c.id) === id)
    if (byId) return byId
  }
  const name = (visit.customerName || "").trim().toLowerCase()
  if (!name) return null
  const matches = customers.filter((c) =>
    [c.firmName, c.name].some((s) => (s || "").trim().toLowerCase() === name)
  )
  const unique = new Map(matches.map((c) => [toNumericId(c.id), c]))
  return unique.size === 1 ? Array.from(unique.values())[0] : null
}

export function ordersOfTrips(entries: PurchaseEntry[], tripIds: Set<number>): PurchaseEntry[] {
  return entries.filter((e) => !e.isDeleted && tripIds.has(Number(e.visitId)))
}

/** Trips a staff member started, co-owned or joined. */
export function tripsOfStaff(visits: Visit[], emp: { id: number; name?: string }): Visit[] {
  return visits.filter((v) => !v.isDeleted && hasMember(v, emp))
}

/**
 * Orders credited to a staff member: they are the order's salesman.
 * Orders saved before per-order salesmen existed count for the trip starter.
 */
export function ordersOfStaff(
  entries: PurchaseEntry[],
  visitsById: Map<number, Visit>,
  emp: { id: number; name?: string }
): PurchaseEntry[] {
  return entries.filter((e) => {
    if (e.isDeleted) return false
    const sid = toNumericId(e.salesmanId)
    if (sid > 0) return sid === Number(emp.id)
    if ((e.salesmanName || "").trim()) return sameName(e.salesmanName, emp.name)
    const v = visitsById.get(Number(e.visitId))
    return Boolean(v && Number(v.employeeId) === Number(emp.id))
  })
}

export function filterTripsByDate(visits: Visit[], range: DateRange): Visit[] {
  return range.preset === "ALL" ? visits : visits.filter((v) => matchesDateRange(v.date || v.createdAt, range))
}

export function filterOrdersByDate(entries: PurchaseEntry[], visitsById: Map<number, Visit>, range: DateRange): PurchaseEntry[] {
  return range.preset === "ALL"
    ? entries
    : entries.filter((e) => matchesDateRange(effectiveOrderDate(e, visitsById.get(Number(e.visitId))), range))
}

export function indexById<T extends { id: number | string }>(items: T[]): Map<number, T> {
  const map = new Map<number, T>()
  items.forEach((i) => map.set(Number(i.id), i))
  return map
}
