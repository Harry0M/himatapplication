import { ReactNode } from "react"
import { Card } from "../ui/Card"
import { Badge } from "../ui/Badge"
import { formatInr } from "../../lib/utils"
import type { Customer, Employee, PurchaseEntry, Supplier, Visit } from "../../types"
import { effectiveOrderDate, isDelivered, isTripClosed, membersDisplay } from "../../lib/domain"

/**
 * Building blocks for "everything linked to this master" sections.
 * Same idea as the Android RelatedRecords.kt: summary, trips, orders, customers, referred.
 */

const amountOf = (e: PurchaseEntry) =>
  Number(e.grandTotalWithGst) || Number(e.totalAmount || 0) + Number(e.gstAmount || 0) || 0

export function Section({ title, count, children, action }: { title: string; count?: number; children: ReactNode; action?: ReactNode }) {
  return (
    <section className="space-y-2" aria-label={title}>
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">
          {title}
          {count !== undefined && <span className="ml-1.5 text-xs font-semibold text-muted-foreground">({count})</span>}
        </h3>
        {action}
      </div>
      {children}
    </section>
  )
}

function Empty({ text }: { text: string }) {
  return (
    <Card className="p-6 text-center text-xs text-muted-foreground">
      {text}
    </Card>
  )
}

/** Trips / orders / pieces / amount / delivery for the filtered period */
export function ActivitySummary({ trips, orders }: { trips: Visit[]; orders: PurchaseEntry[] }) {
  const pieces = orders.reduce((s, e) => s + (Number(e.pieces) || 0), 0)
  const amount = orders.reduce((s, e) => s + amountOf(e), 0)
  const delivered = orders.filter(isDelivered).length
  const items = [
    { label: "Trips", value: String(trips.length) },
    { label: "Orders", value: String(orders.length) },
    { label: "Pieces", value: String(pieces) },
    { label: "Order value", value: `₹${formatInr(amount)}` },
    { label: "Delivered", value: `${delivered}/${orders.length}` },
  ]
  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-5">
      {items.map((i) => (
        <Card key={i.label} className="p-3">
          <p className="text-[11px] font-semibold text-muted-foreground">{i.label}</p>
          <p className="mt-0.5 text-lg font-bold text-zinc-900 dark:text-zinc-50">{i.value}</p>
        </Card>
      ))}
    </div>
  )
}

export function TripsTable({
  trips,
  entries,
  employees = [],
  emptyText = "No trips in this period.",
}: {
  trips: Visit[]
  entries: PurchaseEntry[]
  employees?: Employee[]
  emptyText?: string
}) {
  if (trips.length === 0) return <Empty text={emptyText} />
  const sorted = [...trips].sort((a, b) => (b.date || "").localeCompare(a.date || "") || Number(b.id) - Number(a.id))
  return (
    <Card className="overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="border-b border-zinc-200/80 bg-zinc-50/70 font-semibold text-muted-foreground dark:border-zinc-800 dark:bg-zinc-900/50">
            <tr>
              <th className="py-2.5 pl-4 pr-2">Date</th>
              <th className="px-2 py-2.5">Customer</th>
              <th className="px-2 py-2.5">Salesmen</th>
              <th className="px-2 py-2.5">Orders</th>
              <th className="px-2 py-2.5 pr-4">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
            {sorted.map((v) => {
              const tripOrders = entries.filter((e) => Number(e.visitId) === Number(v.id))
              const closed = isTripClosed(v)
              return (
                <tr key={v.id}>
                  <td className="py-2.5 pl-4 pr-2 whitespace-nowrap font-medium">{v.date || "—"}</td>
                  <td className="px-2 py-2.5">
                    <span className="font-semibold text-zinc-900 dark:text-zinc-100">{v.customerName}</span>
                    {v.visitCode && <span className="block text-[10px] text-muted-foreground">{v.visitCode}</span>}
                  </td>
                  <td className="px-2 py-2.5">{membersDisplay(v, employees) || "—"}</td>
                  <td className="px-2 py-2.5">
                    {tripOrders.length} • {tripOrders.reduce((s, e) => s + (Number(e.pieces) || 0), 0)} pcs
                  </td>
                  <td className="px-2 py-2.5 pr-4">
                    <Badge variant={closed ? "secondary" : "info"}>{closed ? "Closed" : v.status || "Active"}</Badge>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </Card>
  )
}

export function OrdersTable({
  orders,
  visitsById,
  emptyText = "No orders in this period.",
  showCustomer = true,
  showSupplier = true,
}: {
  orders: PurchaseEntry[]
  visitsById: Map<number, Visit>
  emptyText?: string
  showCustomer?: boolean
  showSupplier?: boolean
}) {
  if (orders.length === 0) return <Empty text={emptyText} />
  const rows = orders
    .map((e) => ({ e, visit: visitsById.get(Number(e.visitId)) }))
    .map((r) => ({ ...r, date: effectiveOrderDate(r.e, r.visit) }))
    .sort((a, b) => b.date.localeCompare(a.date) || Number(b.e.id) - Number(a.e.id))
  return (
    <Card className="overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="border-b border-zinc-200/80 bg-zinc-50/70 font-semibold text-muted-foreground dark:border-zinc-800 dark:bg-zinc-900/50">
            <tr>
              <th className="py-2.5 pl-4 pr-2">Order</th>
              <th className="px-2 py-2.5">Date</th>
              {showCustomer && <th className="px-2 py-2.5">Customer</th>}
              {showSupplier && <th className="px-2 py-2.5">Supplier</th>}
              <th className="px-2 py-2.5">Salesman</th>
              <th className="px-2 py-2.5">Pieces</th>
              <th className="px-2 py-2.5">Amount</th>
              <th className="px-2 py-2.5 pr-4">Delivery</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
            {rows.map(({ e, visit, date }) => (
              <tr key={e.id}>
                <td className="py-2.5 pl-4 pr-2">
                  <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">#{e.orderNo}</span>
                  <span className="block text-[10px] text-muted-foreground">{e.itemCode}</span>
                </td>
                <td className="px-2 py-2.5 whitespace-nowrap">{date || "—"}</td>
                {showCustomer && <td className="px-2 py-2.5 font-medium">{visit?.customerName || "—"}</td>}
                {showSupplier && <td className="px-2 py-2.5">{e.supplierName || "—"}</td>}
                <td className="px-2 py-2.5">{e.salesmanName || visit?.employeeName || "—"}</td>
                <td className="px-2 py-2.5">
                  {e.pieces}
                  {Number(e.loosePieces) > 0 && <span className="block text-[10px] text-muted-foreground">{e.loosePieces} loose</span>}
                </td>
                <td className="px-2 py-2.5 whitespace-nowrap">₹{formatInr(amountOf(e))}</td>
                <td className="px-2 py-2.5 pr-4">
                  <Badge variant={isDelivered(e) ? "success" : "warning"}>{e.deliveryStatus || "Pending"}</Badge>
                  {e.lrNo && <span className="block text-[10px] text-muted-foreground mt-0.5">LR {e.lrNo}</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}

export function CustomersTable({
  customers,
  visits,
  entries,
  onOpen,
  emptyText = "No customers yet.",
}: {
  customers: Customer[]
  visits: Visit[]
  entries: PurchaseEntry[]
  onOpen?: (c: Customer) => void
  emptyText?: string
}) {
  if (customers.length === 0) return <Empty text={emptyText} />
  return (
    <Card className="overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="border-b border-zinc-200/80 bg-zinc-50/70 font-semibold text-muted-foreground dark:border-zinc-800 dark:bg-zinc-900/50">
            <tr>
              <th className="py-2.5 pl-4 pr-2">Customer</th>
              <th className="px-2 py-2.5">City</th>
              <th className="px-2 py-2.5">Phone</th>
              <th className="px-2 py-2.5">Trips</th>
              <th className="px-2 py-2.5">Orders</th>
              <th className="px-2 py-2.5 pr-4">Last trip</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
            {customers.map((c) => {
              const cTrips = visits.filter((v) => Number(v.customerId) === Number(c.id))
              const tripIds = new Set(cTrips.map((v) => Number(v.id)))
              const cOrders = entries.filter((e) => tripIds.has(Number(e.visitId)))
              const last = cTrips.map((v) => v.date || "").sort().pop()
              return (
                <tr
                  key={c.id}
                  className={onOpen ? "cursor-pointer hover:bg-zinc-50 dark:hover:bg-zinc-900/50" : undefined}
                  onClick={onOpen ? () => onOpen(c) : undefined}
                >
                  <td className="py-2.5 pl-4 pr-2">
                    <span className="font-semibold text-zinc-900 dark:text-zinc-100">{c.firmName || c.name}</span>
                    <span className="block text-[10px] text-muted-foreground">
                      {[c.customerId, c.firmName && c.name !== c.firmName ? c.name : ""].filter(Boolean).join(" • ")}
                    </span>
                  </td>
                  <td className="px-2 py-2.5">{c.city || "—"}</td>
                  <td className="px-2 py-2.5 whitespace-nowrap">{c.phone || "—"}</td>
                  <td className="px-2 py-2.5">{cTrips.length}</td>
                  <td className="px-2 py-2.5">{cOrders.length}</td>
                  <td className="px-2 py-2.5 pr-4 whitespace-nowrap">{last || "—"}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </Card>
  )
}

/** Customers, suppliers and people whose "Referred By" points at this master */
export function ReferredList({
  customers = [],
  suppliers = [],
  people = [],
}: {
  customers?: Customer[]
  suppliers?: Supplier[]
  people?: Employee[]
}) {
  const rows: Array<{ key: string; name: string; kind: string; detail: string }> = [
    ...customers.map((c) => ({ key: `c-${c.id}`, name: c.firmName || c.name, kind: "Customer", detail: [c.city, c.phone].filter(Boolean).join(" • ") })),
    ...suppliers.map((s) => ({ key: `s-${s.id}`, name: s.firmName || s.name, kind: "Supplier", detail: [s.marketArea || s.city, s.phone].filter(Boolean).join(" • ") })),
    ...people.map((p) => ({ key: `p-${p.id}`, name: p.name, kind: p.role === "Agent" ? "Sub Agent" : "Staff", detail: p.phone || "" })),
  ]
  if (rows.length === 0) return <Empty text="Nobody has been referred by them yet." />
  return (
    <Card className="divide-y divide-zinc-100 dark:divide-zinc-800">
      {rows.map((r) => (
        <div key={r.key} className="flex items-center justify-between gap-2 px-4 py-2.5 text-xs">
          <div className="min-w-0">
            <p className="truncate font-semibold text-zinc-900 dark:text-zinc-100">{r.name}</p>
            {r.detail && <p className="truncate text-[11px] text-muted-foreground">{r.detail}</p>}
          </div>
          <Badge variant="outline" className="shrink-0 text-[10px]">
            {r.kind}
          </Badge>
        </div>
      ))}
    </Card>
  )
}
