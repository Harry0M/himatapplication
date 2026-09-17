import React, { useState, useMemo } from "react"
import {
  ArrowLeft,
  Search,
  Truck,
  Package,
  Receipt,
  Building,
  Edit2,
  Calendar,
  ExternalLink,
  Phone,
  MapPin,
  CheckCircle2,
  Clock,
  Navigation,
  FileText,
  User,
  ShieldCheck,
  Sparkles
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatInr, formatDate, cn } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Input } from "../components/ui/Input"
import { Transporter } from "../types"

interface TransporterDetailViewProps {
  transporterId: number
  onBack: () => void
  onEdit?: (transporter: Transporter) => void
  onNavigate?: (tab: string) => void
}

export function TransporterDetailView({
  transporterId,
  onBack,
  onEdit,
  onNavigate,
}: TransporterDetailViewProps) {
  const { transporters, entries, customers, visits } = useData()

  const [activeTab, setActiveTab] = useState<"shipments" | "customers" | "depot">("shipments")
  const [searchQuery, setSearchQuery] = useState<string>("")

  // Target Transporter
  const transporter = useMemo(() => {
    return transporters.find((t) => t.id === transporterId) || null
  }, [transporters, transporterId])

  // Shipments handled by this transporter
  const shipments = useMemo(() => {
    if (!transporter) return []
    const tName = (transporter.transporterName || "").toLowerCase()
    return entries
      .filter((e) => {
        const trans = (e.transporter || "").toLowerCase()
        return trans.includes(tName)
      })
      .sort((a, b) => (b.id || 0) - (a.id || 0))
  }, [entries, transporter])

  // Customers preferring this transporter
  const preferredCustomers = useMemo(() => {
    if (!transporter) return []
    const tName = (transporter.transporterName || "").toLowerCase()
    return customers.filter((c) => {
      const pref = (c.preferredTransporterName || "").toLowerCase()
      const prefId = c.preferredTransporterId ? Number(c.preferredTransporterId) : null
      return pref.includes(tName) || prefId === transporter.id
    })
  }, [customers, transporter])

  // Visits map for orders
  const visitMap = useMemo(() => {
    const map = new Map<number, (typeof visits)[0]>()
    visits.forEach((v) => map.set(v.id, v))
    return map
  }, [visits])

  // Customer map for order lookup
  const customerMap = useMemo(() => {
    const map = new Map<number, any>()
    customers.forEach((c) => {
      map.set(c.id, c)
    })
    return map
  }, [customers])

  // Logistics Stats
  const totalShipments = shipments.length
  const inTransitCount = shipments.filter(
    (s) => s.deliveryStatus?.toLowerCase() === "dispatched"
  ).length
  const deliveredCount = shipments.filter(
    (s) => s.deliveryStatus?.toLowerCase() === "delivered"
  ).length
  const pendingCount = shipments.filter(
    (s) =>
      s.deliveryStatus?.toLowerCase() !== "delivered" &&
      s.deliveryStatus?.toLowerCase() !== "dispatched"
  ).length
  const totalShipmentValue = useMemo(() => {
    return shipments.reduce(
      (sum, s) => sum + (Number(s.totalAmount) || 0) + (Number(s.gstAmount) || 0),
      0
    )
  }, [shipments])

  // Filtered Shipments
  const filteredShipments = useMemo(() => {
    const q = searchQuery.toLowerCase().trim()
    if (!q) return shipments
    return shipments.filter((s) => {
      const visit = visitMap.get(s.visitId)
      const cust = visit ? customerMap.get(visit.customerId) : undefined
      const custName = (cust?.firmName || cust?.name || visit?.customerName || "").toLowerCase()
      const city = (cust?.city || "").toLowerCase()
      return (
        (s.orderNo || String(s.id) || "").toLowerCase().includes(q) ||
        (s.itemCode || "").toLowerCase().includes(q) ||
        custName.includes(q) ||
        city.includes(q) ||
        (s.deliveryStatus || "").toLowerCase().includes(q) ||
        (s.lrNo || "").toLowerCase().includes(q)
      )
    })
  }, [shipments, searchQuery, customerMap, visitMap])

  // Filtered Customers
  const filteredCustomers = useMemo(() => {
    const q = searchQuery.toLowerCase().trim()
    if (!q) return preferredCustomers
    return preferredCustomers.filter(
      (c) =>
        (c.firmName || "").toLowerCase().includes(q) ||
        (c.name || "").toLowerCase().includes(q) ||
        (c.city || "").toLowerCase().includes(q) ||
        (c.phone || "").includes(q) ||
        (c.gstin || "").toLowerCase().includes(q)
    )
  }, [preferredCustomers, searchQuery])

  if (!transporter) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" size="sm" onClick={onBack} className="gap-2 text-xs">
          <ArrowLeft className="h-4 w-4" /> Back to Transporters Master
        </Button>
        <Card className="p-8 text-center text-muted-foreground">
          Transporter record not found or has been removed.
        </Card>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Top Header & Actions */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={onBack}
            className="h-9 w-9 p-0 rounded-lg shrink-0"
          >
            <ArrowLeft className="h-4 w-4 text-zinc-600 dark:text-zinc-400" />
          </Button>

          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-blue-500/10 text-blue-700 font-bold dark:bg-blue-500/20 dark:text-blue-400">
              <Truck className="h-5 w-5" />
            </div>

            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
                  {transporter.transporterName}
                </h2>
                <Badge
                  variant="outline"
                  className="text-[10px] bg-blue-500/10 text-blue-700 border-blue-500/20 font-semibold"
                >
                  {transporter.city || "Ahmedabad Hub"}
                </Badge>
                {transporter.gstin && (
                  <span className="font-mono text-[11px] text-zinc-500">
                    GSTIN: {transporter.gstin}
                  </span>
                )}
              </div>
              <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
                {transporter.contactPerson && `Desk: ${transporter.contactPerson} • `}
                Phone: {transporter.phone}
                {transporter.destinationsCovered && ` • Routes: ${transporter.destinationsCovered}`}
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {transporter.trackingUrl && (
            <a
              href={transporter.trackingUrl.startsWith("http") ? transporter.trackingUrl : `https://${transporter.trackingUrl}`}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 h-8 px-3 rounded-md text-xs font-medium border border-zinc-200 bg-white hover:bg-zinc-50 text-blue-600 dark:border-zinc-800 dark:bg-zinc-900 dark:text-blue-400"
            >
              <ExternalLink className="h-3.5 w-3.5" />
              Online Portal
            </a>
          )}
          {onEdit && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => onEdit(transporter)}
              className="h-8 gap-1.5 text-xs"
            >
              <Edit2 className="h-3.5 w-3.5" />
              Edit Transporter
            </Button>
          )}
        </div>
      </div>

      {/* Logistics KPI Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Total Shipments</span>
            <Truck className="h-4 w-4 text-blue-500" />
          </div>
          <div className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
            {totalShipments}
          </div>
          <span className="text-[10px] text-zinc-400">Recorded orders</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">In Transit</span>
            <Clock className="h-4 w-4 text-blue-600" />
          </div>
          <div className="text-xl font-bold text-blue-600 mt-1">
            {inTransitCount}
          </div>
          <span className="text-[10px] text-blue-400">Dispatched parcles</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Delivered</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-500" />
          </div>
          <div className="text-xl font-bold text-emerald-600 mt-1">
            {deliveredCount}
          </div>
          <span className="text-[10px] text-emerald-400">Successful drops</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Pending Dispatch</span>
            <Package className="h-4 w-4 text-amber-500" />
          </div>
          <div className="text-xl font-bold text-amber-600 mt-1">
            {pendingCount}
          </div>
          <span className="text-[10px] text-amber-400">Ready to book</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 col-span-2 sm:col-span-1">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Merchandise Value</span>
            <Sparkles className="h-4 w-4 text-purple-500" />
          </div>
          <div className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
            {formatInr(totalShipmentValue)}
          </div>
          <span className="text-[10px] text-zinc-400">Handled cargo worth</span>
        </Card>
      </div>

      {/* Tabs & Search Navigation Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-zinc-200 pb-3 dark:border-zinc-800">
        <div className="flex items-center gap-1.5 overflow-x-auto">
          <button
            onClick={() => setActiveTab("shipments")}
            className={cn(
              "px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5",
              activeTab === "shipments"
                ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
                : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
            )}
          >
            <Truck className="h-3.5 w-3.5" />
            Shipments & Orders ({shipments.length})
          </button>

          <button
            onClick={() => setActiveTab("customers")}
            className={cn(
              "px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5",
              activeTab === "customers"
                ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
                : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
            )}
          >
            <User className="h-3.5 w-3.5" />
            Preferred By Customers ({preferredCustomers.length})
          </button>

          <button
            onClick={() => setActiveTab("depot")}
            className={cn(
              "px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5",
              activeTab === "depot"
                ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
                : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
            )}
          >
            <Building className="h-3.5 w-3.5" />
            Depot & Contacts Info
          </button>
        </div>

        {/* Live Search */}
        <div className="relative w-full sm:w-72">
          <Search className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-zinc-400" />
          <Input
            type="text"
            placeholder={
              activeTab === "shipments"
                ? "Search order #, customer, city, status..."
                : activeTab === "customers"
                ? "Search customer firm, city, phone..."
                : "Search details..."
            }
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8 h-8 text-xs"
          />
        </div>
      </div>

      {/* TAB 1: Shipments & Orders */}
      {activeTab === "shipments" && (
        <div>
          {filteredShipments.length === 0 ? (
            <Card className="p-8 text-center text-zinc-500 border-dashed text-xs">
              <Truck className="h-8 w-8 mx-auto text-zinc-300 mb-2" />
              {searchQuery ? "No shipments match your search query." : "No shipment bookings under this transporter yet."}
            </Card>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950">
              <table className="w-full text-left text-xs">
                <thead className="bg-zinc-50 border-b border-zinc-200 text-zinc-600 dark:bg-zinc-900 dark:border-zinc-800 dark:text-zinc-400 font-semibold">
                  <tr>
                    <th className="py-2.5 px-3">Order #</th>
                    <th className="py-2.5 px-3">Consignee / Customer</th>
                    <th className="py-2.5 px-3">Destination City</th>
                    <th className="py-2.5 px-3">Item Code</th>
                    <th className="py-2.5 px-3">Pieces</th>
                    <th className="py-2.5 px-3 text-right">Order Value</th>
                    <th className="py-2.5 px-3">Delivery Status</th>
                    <th className="py-2.5 px-3">Tracking / Bilty</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
                  {filteredShipments.map((s) => {
                    const visit = visitMap.get(s.visitId)
                    const cust = visit ? customerMap.get(visit.customerId) : undefined
                    const custName = cust?.firmName || cust?.name || visit?.customerName || "Customer"
                    const destCity = cust?.city || "—"
                    return (
                      <tr key={s.id} className="hover:bg-zinc-50/60 dark:hover:bg-zinc-900/60 transition-colors">
                        <td className="py-2.5 px-3 font-mono font-semibold text-zinc-900 dark:text-zinc-100">
                          #{s.orderNo || s.id}
                        </td>
                        <td className="py-2.5 px-3 font-medium text-zinc-800 dark:text-zinc-200">
                          {custName}
                        </td>
                        <td className="py-2.5 px-3 text-zinc-600 dark:text-zinc-400">
                          📍 {destCity}
                        </td>
                        <td className="py-2.5 px-3 font-mono text-[11px] text-zinc-500">
                          {s.itemCode}
                        </td>
                        <td className="py-2.5 px-3 font-bold text-zinc-900 dark:text-zinc-100">
                          {s.pieces} pcs
                        </td>
                        <td className="py-2.5 px-3 text-right font-bold text-emerald-700 dark:text-emerald-400">
                          {formatInr((Number(s.totalAmount) || 0) + (Number(s.gstAmount) || 0))}
                        </td>
                        <td className="py-2.5 px-3">
                          <Badge
                            variant="outline"
                            className={cn(
                              "text-[10px] font-semibold",
                              s.deliveryStatus?.toLowerCase() === "delivered"
                                ? "bg-emerald-500/10 text-emerald-700 border-emerald-500/20"
                                : s.deliveryStatus?.toLowerCase() === "dispatched"
                                ? "bg-blue-500/10 text-blue-700 border-blue-500/20"
                                : "bg-amber-500/10 text-amber-700 border-amber-500/20"
                            )}
                          >
                            {s.deliveryStatus || "Pending"}
                          </Badge>
                        </td>
                        <td className="py-2.5 px-3 font-mono text-zinc-500 text-[11px]">
                          {s.lrNo || "—"}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: Preferred Customers */}
      {activeTab === "customers" && (
        <div>
          {filteredCustomers.length === 0 ? (
            <Card className="p-8 text-center text-zinc-500 border-dashed text-xs">
              <User className="h-8 w-8 mx-auto text-zinc-300 mb-2" />
              {searchQuery ? "No customer accounts match your search query." : "No customers currently select this transporter as their preferred delivery agent."}
            </Card>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {filteredCustomers.map((c) => (
                <Card key={c.id} className="p-4 border border-zinc-200/80 dark:border-zinc-800 space-y-2">
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="font-bold text-sm text-zinc-900 dark:text-zinc-50">
                        {c.firmName || c.name}
                      </h4>
                      <span className="text-[11px] text-zinc-500">
                        Owner: {c.name || "N/A"}
                      </span>
                    </div>
                    <Badge variant="outline" className="text-[10px] bg-blue-500/10 text-blue-700 border-blue-500/20">
                      {c.city || "Ahmedabad"}
                    </Badge>
                  </div>

                  <div className="text-xs space-y-1 text-zinc-600 dark:text-zinc-400 border-t border-zinc-100 pt-2 dark:border-zinc-800">
                    <div className="flex items-center gap-1.5">
                      <Phone className="h-3 w-3 text-zinc-400" />
                      <span>{c.phone}</span>
                    </div>
                    {c.address && (
                      <div className="flex items-start gap-1.5">
                        <MapPin className="h-3 w-3 text-zinc-400 shrink-0 mt-0.5" />
                        <span className="line-clamp-1">{c.address}</span>
                      </div>
                    )}
                    {c.gstin && (
                      <div className="font-mono text-[10px] text-zinc-500">
                        GST: {c.gstin}
                      </div>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 3: Depot & Contact Info */}
      {activeTab === "depot" && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 space-y-3">
            <h3 className="font-bold text-sm text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
              <Building className="h-4 w-4 text-blue-600" />
              Office & Booking Godowns
            </h3>
            <div className="space-y-3 text-xs divide-y divide-zinc-100 dark:divide-zinc-800">
              <div className="pt-2">
                <span className="text-[11px] text-zinc-400 block font-medium">Head Booking Office</span>
                <p className="mt-0.5 text-zinc-800 dark:text-zinc-200 font-medium">
                  {transporter.officeAddress || "Office address not registered."}
                </p>
              </div>

              <div className="pt-2">
                <span className="text-[11px] text-zinc-400 block font-medium">Godown / Delivery Hub</span>
                <p className="mt-0.5 text-zinc-800 dark:text-zinc-200 font-medium">
                  {transporter.godownAddress || "Godown location not registered."}
                </p>
              </div>

              <div className="pt-2">
                <span className="text-[11px] text-zinc-400 block font-medium">City Hub</span>
                <p className="mt-0.5 text-zinc-800 dark:text-zinc-200 font-medium">
                  {transporter.city || "Ahmedabad Hub"}
                </p>
              </div>
            </div>
          </Card>

          <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 space-y-3">
            <h3 className="font-bold text-sm text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
              <Navigation className="h-4 w-4 text-emerald-600" />
              Routes, Portals & Contacts
            </h3>
            <div className="space-y-3 text-xs divide-y divide-zinc-100 dark:divide-zinc-800">
              <div className="pt-2">
                <span className="text-[11px] text-zinc-400 block font-medium">Destinations & States Covered</span>
                <p className="mt-0.5 text-blue-700 dark:text-blue-400 font-semibold">
                  {transporter.destinationsCovered || "All India coverage."}
                </p>
              </div>

              <div className="pt-2">
                <span className="text-[11px] text-zinc-400 block font-medium">Phone Lines</span>
                <div className="mt-1 flex flex-wrap gap-2">
                  <span className="px-2 py-0.5 rounded bg-zinc-100 text-zinc-800 font-medium">
                    Primary: {transporter.phone}
                  </span>
                  {transporter.phone2 && (
                    <span className="px-2 py-0.5 rounded bg-zinc-100 text-zinc-800 font-medium">
                      Line 2: {transporter.phone2}
                    </span>
                  )}
                  {transporter.phone3 && (
                    <span className="px-2 py-0.5 rounded bg-zinc-100 text-zinc-800 font-medium">
                      Line 3: {transporter.phone3}
                    </span>
                  )}
                </div>
              </div>

              {transporter.trackingUrl && (
                <div className="pt-2">
                  <span className="text-[11px] text-zinc-400 block font-medium">Online Tracking URL</span>
                  <a
                    href={transporter.trackingUrl.startsWith("http") ? transporter.trackingUrl : `https://${transporter.trackingUrl}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="mt-0.5 text-blue-600 hover:underline flex items-center gap-1 font-medium"
                  >
                    <ExternalLink className="h-3 w-3" />
                    {transporter.trackingUrl}
                  </a>
                </div>
              )}

              {transporter.notes && (
                <div className="pt-2">
                  <span className="text-[11px] text-zinc-400 block font-medium">Special Notes</span>
                  <p className="mt-0.5 text-zinc-500 italic">"{transporter.notes}"</p>
                </div>
              )}
            </div>
          </Card>
        </div>
      )}
    </div>
  )
}
