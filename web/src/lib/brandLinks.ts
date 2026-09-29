import { Brand, Customer, Product, PurchaseEntry, Supplier, Visit } from "../types"
import { customerOfTrip, toNumericId } from "./domain"

export interface BrandLinkResult {
  suppliers: Supplier[]
  products: Product[]
  orders: PurchaseEntry[]
  trips: Visit[]
  customers: Customer[]
}

const text = (v: unknown) => (v === undefined || v === null ? "" : String(v).trim())
const lower = (v: unknown) => text(v).toLowerCase()

/**
 * "Show me everything for this brand" — the same rules as the Android app (util/BrandLinks.kt).
 *
 * A supplier belongs to a brand through its brandId, its brand name, or because the brand names it
 * as its manufacturer. Products, orders, trips and customers then follow those suppliers, so the
 * brand screen shows the same records as the supplier screens.
 */
export function computeBrandLinks(
  brand: Brand,
  suppliers: Supplier[],
  products: Product[],
  entries: PurchaseEntry[],
  visits: Visit[],
  customers: Customer[]
): BrandLinkResult {
  const name = text(brand.brandName)
  // A brand with no name can only be matched by id, never by text
  const useText = name.length > 0
  const nameLower = name.toLowerCase()
  const brandId = toNumericId(brand.id)
  const manufacturerId = toNumericId(brand.manufacturerId)
  const manufacturerName = lower(brand.manufacturerName)

  const linkedSuppliers = suppliers.filter((s) => {
    if (s.isDeleted) return false
    const sId = toNumericId(s.id)
    if (brandId > 0 && toNumericId(s.brandId) === brandId) return true
    if (useText && lower(s.brand) === nameLower) return true
    if (manufacturerId > 0 && sId === manufacturerId) return true
    if (useText && manufacturerName && (lower(s.firmName) === manufacturerName || lower(s.name) === manufacturerName)) return true
    return false
  })
  linkedSuppliers.sort((a, b) => lower(a.brand || a.firmName || a.name).localeCompare(lower(b.brand || b.firmName || b.name)))

  const supplierIds = new Set(linkedSuppliers.map((s) => toNumericId(s.id)).filter((id) => id > 0))

  const linkedProducts = products.filter((p) => {
    if (p.isDeleted) return false
    const pSupplierId = toNumericId(p.supplierId)
    if (pSupplierId > 0 && supplierIds.has(pSupplierId)) return true
    if (!useText) return false
    return lower(p.name).includes(nameLower) || lower(p.productCode).includes(nameLower) || lower(p.description).includes(nameLower)
  })
  linkedProducts.sort((a, b) => lower(a.name).localeCompare(lower(b.name)))

  const productCodes = new Set(linkedProducts.map((p) => lower(p.productCode)).filter(Boolean))

  const linkedOrders = entries.filter((e) => {
    if (e.isDeleted) return false
    const eSupplierId = toNumericId(e.supplierId)
    if (eSupplierId > 0 && supplierIds.has(eSupplierId)) return true
    if (!useText) return false
    const code = lower(e.itemCode)
    return productCodes.has(code) || code.includes(nameLower)
  })
  linkedOrders.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0) || toNumericId(b.id) - toNumericId(a.id))

  const tripIds = new Set(linkedOrders.map((e) => toNumericId(e.visitId)))
  const linkedTrips = visits.filter((v) => !v.isDeleted && tripIds.has(toNumericId(v.id)))
  linkedTrips.sort((a, b) => text(b.date).localeCompare(text(a.date)))

  const byCustomerId = new Map<number, Customer>()
  linkedTrips.forEach((v) => {
    const c = customerOfTrip(v, customers)
    if (c && !c.isDeleted) byCustomerId.set(toNumericId(c.id), c)
  })
  const linkedCustomers = Array.from(byCustomerId.values()).sort((a, b) =>
    lower(a.firmName || a.name).localeCompare(lower(b.firmName || b.name))
  )

  return {
    suppliers: linkedSuppliers,
    products: linkedProducts,
    orders: linkedOrders,
    trips: linkedTrips,
    customers: linkedCustomers,
  }
}
