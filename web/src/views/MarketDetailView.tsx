import React, { useState, useMemo } from "react"
import {
  ArrowLeft,
  Search,
  Compass,
  Building2,
  Users,
  Receipt,
  UserCheck,
  Edit2,
  Phone,
  MapPin,
  Sparkles,
  ExternalLink,
  Layers,
  CheckCircle2,
  Calendar,
  Store
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatInr, formatDate, cn } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Input } from "../components/ui/Input"
import { Market } from "../types"

interface MarketDetailViewProps {
  marketId: number
  onBack: () => void
  onEdit?: (market: Market) => void
  onNavigate?: (tab: string) => void
}

export function MarketDetailView({
  marketId,
  onBack,
  onEdit,
  onNavigate,
}: MarketDetailViewProps) {
  const { markets, suppliers, customers, employees, entries, visits } = useData()

  const [activeTab, setActiveTab] = useState<"suppliers" | "customers" | "orders" | "agents">("suppliers")
  const [searchQuery, setSearchQuery] = useState<string>("")

  // Target Market
  const market = useMemo(() => {
    return markets.find((m) => m.id === marketId) || null
  }, [markets, marketId])

  // Suppliers in this market
  const marketSuppliers = useMemo(() => {
    if (!market) return []
    const mName = (market.marketName || "").toLowerCase()
    const mArea = (market.area || "").toLowerCase()

    return suppliers.filter((s) => {
      const sMkt = (s.marketName || s.marketArea || "").toLowerCase()
      const sMktId = s.marketId ? Number(s.marketId) : null
      const sAddress = (s.address || s.officeAddress || "").toLowerCase()

      return (
        sMktId === market.id ||
        (mName && sMkt.includes(mName)) ||
        (mArea && sAddress.includes(mArea))
      )
    })
  }, [suppliers, market])

  // Customers in this market
  const marketCustomers = useMemo(() => {
    if (!market) return []
    const mName = (market.marketName || "").toLowerCase()
    const mArea = (market.area || "").toLowerCase()

    return customers.filter((c) => {
      const cArea = (c.marketArea || "").toLowerCase()
      const cMkts = (c.markets || "").toLowerCase()
      const cAddress = (c.address || c.shopAddress || "").toLowerCase()

      return (
        (mName && (cArea.includes(mName) || cMkts.includes(mName))) ||
        (mArea && cAddress.includes(mArea))
      )
    })
  }, [customers, market])

  // Agents assigned to this market
  const assignedAgents = useMemo(() => {
    if (!market) return []
    const mName = market.marketName.toLowerCase()
    return employees.filter((e) => {
      const assigned = (e.assignedMarkets || "").toLowerCase()
      const mkts = (e.markets || "").toLowerCase()
      return assigned.includes(mName) || mkts.includes(mName)
    })
  }, [employees, market])

  // Visits map
  const visitMap = useMemo(() => {
    const map = new Map<number, (typeof visits)[0]>()
    visits.forEach((v) => map.set(v.id, v))
    return map
  }, [visits])

  // Customer IDs and Supplier IDs in this market
  const customerIdSet = useMemo(() => {
    return new Set(marketCustomers.map((c) => c.id))
  }, [marketCustomers])

  const supplierIdSet = useMemo(() => {
    return new Set(marketSuppliers.map((s) => s.id))
  }, [marketSuppliers])

  // Orders generated in this market
  const marketOrders = useMemo(() => {
    if (!market) return []
    return entries
      .filter((e) => {
        const visit = visitMap.get(e.visitId)
        const custMatches = visit && customerIdSet.has(visit.customerId)
        const suppMatches = e.supplierId && supplierIdSet.has(Number(e.supplierId))
        return custMatches || suppMatches
      })
      .sort((a, b) => (b.id || 0) - (a.id || 0))
  }, [entries, market, customerIdSet, supplierIdSet, visitMap])

  // Customer map for order lookup
  const customerMap = useMemo(() => {
    const map = new Map<number, string>()
    customers.forEach((c) => {
      map.set(c.id, c.firmName || c.name)
    })
    return map
  }, [customers])

  // Market Financial Metrics
  const totalMarketVolumePieces = useMemo(() => {
    return marketOrders.reduce((sum, o) => sum + (Number(o.pieces) || 0), 0)
  }, [marketOrders])

  const totalMarketRevenue = useMemo(() => {
    return marketOrders.reduce(
      (sum, o) => sum + (Number(o.totalAmount) || 0) + (Number(o.gstAmount) || 0),
      0
    )
  }, [marketOrders])

  // Filtered Suppliers
  const filteredSuppliers = useMemo(() => {
    const q = searchQuery.toLowerCase().trim()
    if (!q) return marketSuppliers
    return marketSuppliers.filter(
      (s) =>
        (s.firmName || "").toLowerCase().includes(q) ||
        (s.name || "").toLowerCase().includes(q) ||
        (s.phone || "").includes(q) ||
        (s.type || "").toLowerCase().includes(q) ||
        (s.productsMade || "").toLowerCase().includes(q)
    )
  }, [marketSuppliers, searchQuery])

  // Filtered Customers
  const filteredCustomers = useMemo(() => {
    const q = searchQuery.toLowerCase().trim()
    if (!q) return marketCustomers
    return marketCustomers.filter(
      (c) =>
        (c.firmName || "").toLowerCase().includes(q) ||
        (c.name || "").toLowerCase().includes(q) ||
        (c.phone || "").includes(q) ||
        (c.city || "").toLowerCase().includes(q) ||
        (c.gstin || "").toLowerCase().includes(q)
    )
  }, [marketCustomers, searchQuery])

  // Filtered Orders
  const filteredOrders = useMemo(() => {
    const q = searchQuery.toLowerCase().trim()
    if (!q) return marketOrders
    return marketOrders.filter((o) => {
      const visit = visitMap.get(o.visitId)
      const custName = (visit?.customerName || customerMap.get(visit?.customerId || 0) || "").toLowerCase()
      return (
        (o.orderNo || String(o.id) || "").toLowerCase().includes(q) ||
        (o.itemCode || "").toLowerCase().includes(q) ||
        custName.includes(q) ||
        (o.supplierName || "").toLowerCase().includes(q) ||
        (o.deliveryStatus || "").toLowerCase().includes(q)
      )
    })
  }, [marketOrders, searchQuery, customerMap, visitMap])

  // Filtered Agents
  const filteredAgents = useMemo(() => {
    const q = searchQuery.toLowerCase().trim()
    if (!q) return assignedAgents
    return assignedAgents.filter(
      (e) =>
        (e.name || "").toLowerCase().includes(q) ||
        (e.phone || "").includes(q) ||
        (e.role || "").toLowerCase().includes(q)
    )
  }, [assignedAgents, searchQuery])

  if (!market) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" size="sm" onClick={onBack} className="gap-2 text-xs">
          <ArrowLeft className="h-4 w-4" /> Back to Markets Master
        </Button>
        <Card className="p-8 text-center text-muted-foreground">
          Market record not found or has been removed.
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
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-700 font-bold dark:bg-emerald-500/20 dark:text-emerald-400">
              <Compass className="h-5 w-5" />
            </div>

            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
                  {market.marketName}
                </h2>
                <Badge
                  variant="outline"
                  className="text-[10px] bg-emerald-500/10 text-emerald-700 border-emerald-500/20 font-semibold"
                >
                  {market.city || "Ahmedabad"}
                </Badge>
                {market.marketType && (
                  <Badge variant="outline" className="text-[10px] bg-zinc-100 text-zinc-700 border-zinc-200">
                    {market.marketType}
                  </Badge>
                )}
              </div>
              <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
                {market.area && `Area: ${market.area} • `}
                {market.landmark && `Landmark: ${market.landmark} • `}
                Pincode: {market.pincode || "380002"}
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {onEdit && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => onEdit(market)}
              className="h-8 gap-1.5 text-xs"
            >
              <Edit2 className="h-3.5 w-3.5" />
              Edit Market
            </Button>
          )}
        </div>
      </div>

      {/* Market KPI Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Suppliers in Market</span>
            <Building2 className="h-4 w-4 text-amber-500" />
          </div>
          <div className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
            {marketSuppliers.length}
          </div>
          <span className="text-[10px] text-zinc-400">Wholesale mills & shops</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Local Customers</span>
            <Users className="h-4 w-4 text-blue-500" />
          </div>
          <div className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
            {marketCustomers.length}
          </div>
          <span className="text-[10px] text-zinc-400">Registered buyers in area</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Orders Generated</span>
            <Receipt className="h-4 w-4 text-emerald-500" />
          </div>
          <div className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
            {marketOrders.length}
          </div>
          <span className="text-[10px] text-zinc-400">Market purchase volume</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Pieces Transacted</span>
            <Layers className="h-4 w-4 text-purple-500" />
          </div>
          <div className="text-xl font-bold text-purple-600 mt-1">
            {totalMarketVolumePieces.toLocaleString("en-IN")} pcs
          </div>
          <span className="text-[10px] text-purple-400">Garments sourced</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 col-span-2 sm:col-span-1">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Total Market Value</span>
            <Sparkles className="h-4 w-4 text-emerald-600" />
          </div>
          <div className="text-xl font-bold text-emerald-600 mt-1">
            {formatInr(totalMarketRevenue)}
          </div>
          <span className="text-[10px] text-emerald-400">Gross invoiced volume</span>
        </Card>
      </div>

      {/* Tabs & Search Navigation Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-zinc-200 pb-3 dark:border-zinc-800">
        <div className="flex items-center gap-1.5 overflow-x-auto">
          <button
            onClick={() => setActiveTab("suppliers")}
            className={cn(
              "px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5",
              activeTab === "suppliers"
                ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
                : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
            )}
          >
            <Building2 className="h-3.5 w-3.5" />
            Suppliers & Mills ({marketSuppliers.length})
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
            <Users className="h-3.5 w-3.5" />
            Customers in Area ({marketCustomers.length})
          </button>

          <button
            onClick={() => setActiveTab("orders")}
            className={cn(
              "px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5",
              activeTab === "orders"
                ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
                : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
            )}
          >
            <Receipt className="h-3.5 w-3.5" />
            Market Orders ({marketOrders.length})
          </button>

          <button
            onClick={() => setActiveTab("agents")}
            className={cn(
              "px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5",
              activeTab === "agents"
                ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
                : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
            )}
          >
            <UserCheck className="h-3.5 w-3.5" />
            Assigned Salesmen ({assignedAgents.length})
          </button>
        </div>

        {/* Live Search */}
        <div className="relative w-full sm:w-72">
          <Search className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-zinc-400" />
          <Input
            type="text"
            placeholder={
              activeTab === "suppliers"
                ? "Search suppliers, firm, phone..."
                : activeTab === "customers"
                ? "Search customer firm, phone..."
                : activeTab === "orders"
                ? "Search order #, item, status..."
                : "Search salesmen..."
            }
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8 h-8 text-xs"
          />
        </div>
      </div>

      {/* TAB 1: Suppliers */}
      {activeTab === "suppliers" && (
        <div>
          {filteredSuppliers.length === 0 ? (
            <Card className="p-8 text-center text-zinc-500 border-dashed text-xs">
              <Building2 className="h-8 w-8 mx-auto text-zinc-300 mb-2" />
              {searchQuery ? "No suppliers match your search query." : "No suppliers registered in this textile market cluster yet."}
            </Card>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {filteredSuppliers.map((s) => (
                <Card key={s.id} className="p-4 border border-zinc-200/80 dark:border-zinc-800 space-y-2">
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="font-bold text-sm text-zinc-900 dark:text-zinc-50">
                        {s.firmName || s.name}
                      </h4>
                      <span className="text-[11px] text-zinc-500">
                        {s.type || "Supplier / Mill"} • {s.contactPerson || "In-charge"}
                      </span>
                    </div>
                    <Badge variant="outline" className="text-[10px] bg-amber-500/10 text-amber-700 border-amber-500/20">
                      {s.city || "Ahmedabad"}
                    </Badge>
                  </div>

                  <div className="text-xs space-y-1 text-zinc-600 dark:text-zinc-400 border-t border-zinc-100 pt-2 dark:border-zinc-800">
                    <div className="flex items-center gap-1.5">
                      <Phone className="h-3 w-3 text-zinc-400" />
                      <span>{s.phone}</span>
                    </div>
                    {s.productsMade && (
                      <div className="line-clamp-1 text-zinc-500 text-[11px]">
                        Products: {s.productsMade}
                      </div>
                    )}
                    {s.address && (
                      <div className="flex items-start gap-1.5">
                        <MapPin className="h-3 w-3 text-zinc-400 shrink-0 mt-0.5" />
                        <span className="line-clamp-1">{s.address}</span>
                      </div>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: Customers */}
      {activeTab === "customers" && (
        <div>
          {filteredCustomers.length === 0 ? (
            <Card className="p-8 text-center text-zinc-500 border-dashed text-xs">
              <Users className="h-8 w-8 mx-auto text-zinc-300 mb-2" />
              {searchQuery ? "No customers match your search query." : "No retail or cloth merchants registered in this market area."}
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

      {/* TAB 3: Market Orders */}
      {activeTab === "orders" && (
        <div>
          {filteredOrders.length === 0 ? (
            <Card className="p-8 text-center text-zinc-500 border-dashed text-xs">
              <Receipt className="h-8 w-8 mx-auto text-zinc-300 mb-2" />
              {searchQuery ? "No orders match your search query." : "No orders generated from this market cluster yet."}
            </Card>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950">
              <table className="w-full text-left text-xs">
                <thead className="bg-zinc-50 border-b border-zinc-200 text-zinc-600 dark:bg-zinc-900 dark:border-zinc-800 dark:text-zinc-400 font-semibold">
                  <tr>
                    <th className="py-2.5 px-3">Order #</th>
                    <th className="py-2.5 px-3">Customer</th>
                    <th className="py-2.5 px-3">Supplier</th>
                    <th className="py-2.5 px-3">Item Code</th>
                    <th className="py-2.5 px-3">Pieces</th>
                    <th className="py-2.5 px-3 text-right">Order Value</th>
                    <th className="py-2.5 px-3">Status</th>
                    <th className="py-2.5 px-3">Transporter</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
                  {filteredOrders.map((o) => {
                    const visit = visitMap.get(o.visitId)
                    const custName = visit?.customerName || customerMap.get(visit?.customerId || 0) || "Customer"
                    return (
                      <tr key={o.id} className="hover:bg-zinc-50/60 dark:hover:bg-zinc-900/60 transition-colors">
                        <td className="py-2.5 px-3 font-mono font-semibold text-zinc-900 dark:text-zinc-100">
                          #{o.orderNo || o.id}
                        </td>
                        <td className="py-2.5 px-3 font-medium text-zinc-800 dark:text-zinc-200">
                          {custName}
                        </td>
                        <td className="py-2.5 px-3 text-zinc-600 dark:text-zinc-400">
                          {o.supplierName || "—"}
                        </td>
                        <td className="py-2.5 px-3 font-mono text-[11px] text-zinc-500">
                          {o.itemCode}
                        </td>
                        <td className="py-2.5 px-3 font-bold text-zinc-900 dark:text-zinc-100">
                          {o.pieces} pcs
                        </td>
                        <td className="py-2.5 px-3 text-right font-bold text-emerald-700 dark:text-emerald-400">
                          {formatInr((Number(o.totalAmount) || 0) + (Number(o.gstAmount) || 0))}
                        </td>
                        <td className="py-2.5 px-3">
                          <Badge
                            variant="outline"
                            className={cn(
                              "text-[10px] font-semibold",
                              o.deliveryStatus?.toLowerCase() === "delivered"
                                ? "bg-emerald-500/10 text-emerald-700 border-emerald-500/20"
                                : o.deliveryStatus?.toLowerCase() === "dispatched"
                                ? "bg-blue-500/10 text-blue-700 border-blue-500/20"
                                : "bg-amber-500/10 text-amber-700 border-amber-500/20"
                            )}
                          >
                            {o.deliveryStatus || "Pending"}
                          </Badge>
                        </td>
                        <td className="py-2.5 px-3 text-zinc-500 text-[11px]">
                          {o.transporter || "Self / Standard"}
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

      {/* TAB 4: Assigned Salesmen */}
      {activeTab === "agents" && (
        <div>
          {filteredAgents.length === 0 ? (
            <Card className="p-8 text-center text-zinc-500 border-dashed text-xs">
              <UserCheck className="h-8 w-8 mx-auto text-zinc-300 mb-2" />
              {searchQuery ? "No salesmen match your search query." : "No salesmen or sales agents currently have this market assigned in their territory."}
            </Card>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {filteredAgents.map((emp) => (
                <Card key={emp.id} className="p-4 border border-zinc-200/80 dark:border-zinc-800 space-y-2">
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="font-bold text-sm text-zinc-900 dark:text-zinc-50">
                        {emp.name}
                      </h4>
                      <span className="text-[11px] text-zinc-500">
                        ID: {emp.employeeId || emp.id} • Role: {emp.role || "Salesman"}
                      </span>
                    </div>
                    <Badge variant="outline" className="text-[10px] bg-emerald-500/10 text-emerald-700 border-emerald-500/20">
                      Territory Active
                    </Badge>
                  </div>

                  <div className="text-xs space-y-1 text-zinc-600 dark:text-zinc-400 border-t border-zinc-100 pt-2 dark:border-zinc-800">
                    <div className="flex items-center gap-1.5">
                      <Phone className="h-3 w-3 text-zinc-400" />
                      <span>{emp.phone || "—"}</span>
                    </div>
                    {emp.email && (
                      <div className="text-[11px] text-zinc-500">
                        {emp.email}
                      </div>
                    )}
                    {(emp.assignedMarkets || emp.markets) && (
                      <div className="text-[10px] text-zinc-400 pt-1">
                        Assigned: {emp.assignedMarkets || emp.markets}
                      </div>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
