import React, { useState } from "react"
import {
  Receipt,
  Search,
  X,
  UserCheck,
  FileText,
  CheckCircle2,
  AlertCircle,
  Clock,
  Calendar,
  Download,
} from "lucide-react"
import { useData } from "../context/DataContext"
import { useAuth } from "../context/AuthContext"
import { formatInr, formatDate, cn } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Tabs } from "../components/ui/Tabs"
import { Input } from "../components/ui/Input"
import { Dialog } from "../components/ui/Dialog"
import { DateRangeFilter } from "../components/ui/DateRangeFilter"
import { PurchaseEntry, Supplier } from "../types"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateSupplierInvoiceHtml,
  buildSupplierInvoiceWhatsAppText,
  type SupplierOrderFormOptions,
} from "../lib/pdfReports"
import { ALL_TIME, DateRange, effectiveOrderDate, isDelivered as isOrderDelivered, matchesDateRange, toNumericId } from "../lib/domain"

interface OrdersViewProps {
  /** "all" | "pending_delivery" | "delivered" */
  initialStatus?: string
}

export function OrdersView({ initialStatus = "all" }: OrdersViewProps = {}) {
  const {
    entries,
    visits,
    customers,
    suppliers,
    employees,
    brands,
    transporters,
    selectedEmployeeId,
    setSelectedEmployeeId,
    updateDelivery,
  } = useData()
  const { role } = useAuth()
  const readOnly = role === "agent"

  const [search, setSearch] = useState<string>("")
  const [showSearch, setShowSearch] = useState<boolean>(false)
  // Delivery decides whether an order is done; payment is an optional second filter
  const [statusFilter, setStatusFilter] = useState<string>(initialStatus)
  const [paymentFilter, setPaymentFilter] = useState<string>("any")

  // Customer filter
  const [selectedCustomerId, setSelectedCustomerId] = useState<string>("all")

  // Date range filter (presets + custom)
  const [dateRange, setDateRange] = useState<DateRange>(ALL_TIME)

  // Delivery update dialog
  const [deliveryEntry, setDeliveryEntry] = useState<PurchaseEntry | null>(null)
  const [deliveryStatus, setDeliveryStatus] = useState<string>("Dispatched")
  const [deliveryTransporter, setDeliveryTransporter] = useState<string>("")
  const [deliveryLrNo, setDeliveryLrNo] = useState<string>("")
  const [deliveryLrDate, setDeliveryLrDate] = useState<string>("")
  const [savingDelivery, setSavingDelivery] = useState<boolean>(false)

  const openDeliveryDialog = (entry: PurchaseEntry) => {
    setDeliveryEntry(entry)
    setDeliveryStatus(entry.deliveryStatus || "Dispatched")
    setDeliveryTransporter(entry.transporter || "")
    setDeliveryLrNo(entry.lrNo || "")
    setDeliveryLrDate(entry.lrDate || "")
  }

  const saveDelivery = async () => {
    if (!deliveryEntry) return
    setSavingDelivery(true)
    try {
      await updateDelivery(deliveryEntry.id, deliveryStatus, deliveryTransporter.trim(), deliveryLrNo.trim(), deliveryLrDate)
      setDeliveryEntry(null)
    } finally {
      setSavingDelivery(false)
    }
  }

  // Report modal state
  const [reportModal, setReportModal] = useState<{
    open: boolean
    title: string
    html: string
    whatsAppText: string
    /** Order forms: rebuilds the document for the chosen options */
    buildHtml?: (options: SupplierOrderFormOptions) => string
  }>({
    open: false,
    title: "",
    html: "",
    whatsAppText: "",
  })


  const visitMap = React.useMemo(() => {
    return new Map(visits.map((v) => [v.id, v]))
  }, [visits])

  // Active employee for filtering
  const activeEmployee = React.useMemo(() => {
    if (selectedEmployeeId === "all") return null
    return employees.find((e) => e.id === selectedEmployeeId) || null
  }, [selectedEmployeeId, employees])

  // Brand lookup map: supplierId → brand name
  const brandMap = React.useMemo(() => {
    const map = new Map<number, string>()
    if (brands) {
      brands.forEach((b: any) => {
        if (b.id && b.name) map.set(b.id, b.name)
      })
    }
    return map
  }, [brands])

  // Supplier → brand name helper
  const getDisplayName = (entry: PurchaseEntry) => {
    const supplier = suppliers.find((s) => s.id === entry.supplierId)
    const brandId = supplier?.brandId || (supplier as any)?.brand_id
    if (brandId && brandMap.has(brandId)) return brandMap.get(brandId)!
    const brandName = (supplier as any)?.brandName || (supplier as any)?.brand_name
    if (brandName) return brandName
    return entry.supplierName || "—"
  }

  /** The salesman of an order: its own salesman, else the trip starter (orders saved before per-order salesmen). */
  const salesmanOf = (e: PurchaseEntry) => {
    const visit = visitMap.get(e.visitId)
    return {
      id: toNumericId(e.salesmanId) || toNumericId(visit?.employeeId),
      name: (e.salesmanName || "").trim() || visit?.employeeName || "",
    }
  }

  const paymentMatches = (e: PurchaseEntry) => {
    const pStatus = (e.paymentStatus || "").toLowerCase()
    if (paymentFilter === "paid") return pStatus === "paid" || pStatus === "received"
    if (paymentFilter === "partial") return pStatus === "partial"
    if (paymentFilter === "unpaid") return pStatus !== "paid" && pStatus !== "received" && pStatus !== "partial"
    return true
  }

  const q = search.trim().toLowerCase()
  // Everything except the delivery status tab, so the tab counts follow the other filters
  const baseFiltered = entries.filter((e) => {
    const visit = visitMap.get(e.visitId)
    const custName = visit?.customerName || ""
    const salesman = salesmanOf(e)

    // 1. Salesman filter (the order's own salesman)
    if (activeEmployee) {
      const matchId = salesman.id > 0 && salesman.id === Number(activeEmployee.id)
      const matchName = salesman.name.toLowerCase() === activeEmployee.name.trim().toLowerCase()
      if (!matchId && !matchName) return false
    }

    // 2. Customer filter
    if (selectedCustomerId !== "all") {
      const custId = visit?.customerId
      if (String(custId) !== selectedCustomerId) return false
    }

    // 3. Date range filter: the order date, else the trip date
    if (!matchesDateRange(effectiveOrderDate(e, visit), dateRange)) return false

    // 4. Payment filter (optional)
    if (!paymentMatches(e)) return false

    // 5. Search filter
    return (
      !q ||
      e.orderNo?.toLowerCase().includes(q) ||
      e.itemCode?.toLowerCase().includes(q) ||
      e.supplierName?.toLowerCase().includes(q) ||
      custName.toLowerCase().includes(q) ||
      salesman.name.toLowerCase().includes(q) ||
      (e.lrNo || "").toLowerCase().includes(q) ||
      (e.transporter || "").toLowerCase().includes(q)
    )
  })

  const filteredEntries = baseFiltered.filter((e) => {
    if (statusFilter === "pending_delivery") return !isOrderDelivered(e)
    if (statusFilter === "delivered") return isOrderDelivered(e)
    return true
  })

  // Global financial totals
  const totalBilledAll = entries.reduce(
    (sum, e) =>
      sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )
  const totalPaidAll = entries.reduce((sum, e) => sum + (Number(e.paidAmount) || 0), 0)
  const totalDueAll = Math.max(0, totalBilledAll - totalPaidAll)
  const collectionRate = totalBilledAll > 0 ? Math.round((totalPaidAll / totalBilledAll) * 100) : 0

  // Filtered view totals
  const totalBilledFiltered = filteredEntries.reduce(
    (sum, e) =>
      sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )
  const totalPaidFiltered = filteredEntries.reduce((sum, e) => sum + (Number(e.paidAmount) || 0), 0)
  const totalDueFiltered = Math.max(0, totalBilledFiltered - totalPaidFiltered)
  const totalPiecesFiltered = filteredEntries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)

  // Bulk download all filtered invoices — opens each in a new print window
  const handleBulkDownload = () => {
    if (filteredEntries.length === 0) return
    filteredEntries.forEach((entry, idx) => {
      const visit = visitMap.get(entry.visitId) || {
        id: entry.visitId,
        visitCode: `VIS-${entry.visitId}`,
        customerId: 0,
        customerName: "Buyer",
        date: entry.createdAt ? new Date(entry.createdAt).toISOString().split("T")[0] : new Date().toISOString().split("T")[0],
        employeeId: 1,
        employeeName: "Agent",
        status: "Completed",
      }
      const customer = customers.find((c) => c.id === visit.customerId || c.name === visit.customerName)
      const supplier: Supplier = suppliers.find(
        (s) => s.id === entry.supplierId || s.name.toLowerCase() === entry.supplierName?.toLowerCase()
      ) || { id: entry.supplierId || 1, name: entry.supplierName, type: entry.supplierType || "Wholesaler", phone: "—", marketArea: "Wholesale Market" }
      const salesman = employees.find((e) => e.id === visit.employeeId || e.name === visit.employeeName)
      const html = generateSupplierInvoiceHtml({ supplier, visit, customer, salesman, entries: [entry] })
      setTimeout(() => {
        const win = window.open("", `invoice_${idx}`, "width=800,height=600")
        if (win) {
          win.document.write(html)
          win.document.close()
          setTimeout(() => win.print(), 500)
        }
      }, idx * 800)
    })
  }

  // Generate & preview wholesaler invoice for this order
  const handleViewInvoice = (entry: PurchaseEntry) => {
    const visit = visitMap.get(entry.visitId) || {
      id: entry.visitId,
      visitCode: `VIS-${entry.visitId}`,
      customerId: 0,
      customerName: "Buyer",
      date: entry.createdAt ? new Date(entry.createdAt).toISOString().split("T")[0] : new Date().toISOString().split("T")[0],
      employeeId: 1,
      employeeName: "Agent",
      status: "Completed",
    }

    const customer = customers.find((c) => c.id === visit.customerId || c.name === visit.customerName)
    const supplier: Supplier = suppliers.find(
      (s) => s.id === entry.supplierId || s.name.toLowerCase() === entry.supplierName?.toLowerCase()
    ) || {
      id: entry.supplierId || 1,
      name: entry.supplierName,
      type: entry.supplierType || "Wholesaler",
      phone: "—",
      marketArea: "Wholesale Market",
    }

    const salesman = employees.find((e) => e.id === visit.employeeId || e.name === visit.employeeName)

    const invoiceData = {
      supplier,
      visit,
      customer,
      salesman,
      entries: [entry],
    }

    const html = generateSupplierInvoiceHtml(invoiceData)
    const whatsAppText = buildSupplierInvoiceWhatsAppText(invoiceData)

    setReportModal({
      open: true,
      title: `Order Form: #${entry.orderNo} • ${supplier.brand || supplier.name}`,
      html,
      whatsAppText,
      buildHtml: (options) => generateSupplierInvoiceHtml({ ...invoiceData, options }),
    })
  }

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
            <span>Orders & Delivery</span>
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Every order from every trip. Update delivery here; payment tracking is optional.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Employee Filter Dropdown */}
          <div className="flex items-center gap-1.5">
            <select
              value={selectedEmployeeId === "all" ? "all" : String(selectedEmployeeId)}
              onChange={(e) => {
                const val = e.target.value
                setSelectedEmployeeId(val === "all" ? "all" : Number(val))
              }}
              aria-label="Filter by salesman"
              className="h-8 rounded-full border border-zinc-200 bg-white px-3 text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200 focus:outline-none focus:ring-1 focus:ring-zinc-900 shadow-sm"
            >
              <option value="all">👥 All Salesmen</option>
              {employees.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  👤 {emp.name}
                </option>
              ))}
            </select>
            {selectedEmployeeId !== "all" && (
              <Button
                variant="ghost"
                size="sm"
                shape="pill"
                onClick={() => setSelectedEmployeeId("all")}
                className="h-8 px-2 text-xs text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/40"
                title="Clear employee filter"
              >
                <X className="h-3.5 w-3.5" />
              </Button>
            )}
          </div>

          {/* Customer Filter Dropdown */}
          <div className="flex items-center gap-1.5">
            <select
              value={selectedCustomerId}
              onChange={(e) => setSelectedCustomerId(e.target.value)}
              aria-label="Filter by Customer"
              className="h-8 rounded-full border border-zinc-200 bg-white px-3 text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200 focus:outline-none focus:ring-1 focus:ring-zinc-900 shadow-sm"
            >
              <option value="all">🏪 All Customers</option>
              {customers.map((c) => (
                <option key={c.id} value={String(c.id)}>
                  {c.firmName || c.name}
                </option>
              ))}
            </select>
            {selectedCustomerId !== "all" && (
              <Button
                variant="ghost"
                size="sm"
                shape="pill"
                onClick={() => setSelectedCustomerId("all")}
                className="h-8 px-2 text-xs text-red-600 hover:bg-red-50"
                title="Clear customer filter"
              >
                <X className="h-3.5 w-3.5" />
              </Button>
            )}
          </div>

          {/* Payment filter (optional) */}
          <select
            value={paymentFilter}
            onChange={(e) => setPaymentFilter(e.target.value)}
            aria-label="Filter by payment"
            className="h-8 rounded-full border border-zinc-200 bg-white px-3 text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200 focus:outline-none focus:ring-1 focus:ring-zinc-900 shadow-sm"
          >
            <option value="any">💳 Any payment</option>
            <option value="unpaid">Unpaid</option>
            <option value="partial">Partial</option>
            <option value="paid">Paid</option>
          </select>

          {/* Bulk Download Button */}
          <Button
            shape="pill"
            variant="outline"
            size="sm"
            onClick={handleBulkDownload}
            disabled={filteredEntries.length === 0}
            className="h-8 px-3 text-xs"
            title={`Download invoices for ${filteredEntries.length} filtered orders`}
          >
            <Download className="h-3.5 w-3.5 mr-1" />
            Download ({filteredEntries.length})
          </Button>


          {/* Search Toggle */}
          <Button
            shape="pill"
            variant="outline"
            size="sm"
            onClick={() => {
              setShowSearch(!showSearch)
              if (showSearch) setSearch("")
            }}
            className="h-8 px-3 text-xs"
          >
            <Search className="h-3.5 w-3.5 mr-1" />
            <span>Search</span>
          </Button>

          {/* Delivery status: the only status that decides whether an order is done */}
          <Tabs
            value={statusFilter}
            onValueChange={setStatusFilter}
            options={[
              { value: "all", label: "All Orders", count: baseFiltered.length },
              {
                value: "pending_delivery",
                label: "Not Delivered",
                count: baseFiltered.filter((e) => !isOrderDelivered(e)).length,
              },
              {
                value: "delivered",
                label: "Delivered",
                count: baseFiltered.filter((e) => isOrderDelivered(e)).length,
              },
            ]}
          />
        </div>
      </div>

      {/* Date filter: presets + custom range */}
      <DateRangeFilter value={dateRange} onChange={setDateRange} />

      {/* Financial Overview Cards */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Card className="rounded-2xl border-zinc-200/80 p-4 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-sm">
          <div className="flex items-center justify-between text-muted-foreground">
            <span className="text-xs font-semibold">Total Invoiced</span>
            <Receipt className="h-4 w-4 text-zinc-500" />
          </div>
          <p className="text-xl sm:text-2xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
            ₹{formatInr(totalBilledAll)}
          </p>
          <span className="text-[11px] text-muted-foreground mt-0.5 block">
            Across {entries.length} orders
          </span>
        </Card>

        <Card className="rounded-2xl border-zinc-200/80 p-4 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-sm">
          <div className="flex items-center justify-between text-emerald-600 dark:text-emerald-400">
            <span className="text-xs font-semibold">Payment Done</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-500" />
          </div>
          <p className="text-xl sm:text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
            ₹{formatInr(totalPaidAll)}
          </p>
          <span className="text-[11px] text-muted-foreground mt-0.5 block">
            Collected & verified
          </span>
        </Card>

        <Card className="rounded-2xl border-zinc-200/80 p-4 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-sm">
          <div className="flex items-center justify-between text-red-600 dark:text-red-400">
            <span className="text-xs font-semibold">Pending Dues</span>
            <AlertCircle className="h-4 w-4 text-red-500" />
          </div>
          <p className="text-xl sm:text-2xl font-bold text-red-600 dark:text-red-400 mt-1">
            ₹{formatInr(totalDueAll)}
          </p>
          <span className="text-[11px] text-muted-foreground mt-0.5 block">
            Outstanding receivables
          </span>
        </Card>

        <Card className="rounded-2xl border-zinc-200/80 p-4 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-sm">
          <div className="flex items-center justify-between text-blue-600 dark:text-blue-400">
            <span className="text-xs font-semibold">Collection Rate</span>
            <Clock className="h-4 w-4 text-blue-500" />
          </div>
          <p className="text-xl sm:text-2xl font-bold text-blue-600 dark:text-blue-400 mt-1">
            {collectionRate}%
          </p>
          <div className="w-full bg-zinc-100 dark:bg-zinc-800 rounded-full h-1.5 mt-2 overflow-hidden">
            <div
              className="bg-blue-600 dark:bg-blue-500 h-full rounded-full transition-all"
              style={{ width: `${collectionRate}%` }}
            />
          </div>
        </Card>
      </div>

      {/* Expandable Search Input */}
      {showSearch && (
        <div className="relative">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by order #, item code, supplier/mill, customer, or agent name..."
            className="pl-9 pr-8"
            autoFocus
          />
          {search && (
            <button
              onClick={() => setSearch("")}
              className="absolute right-3 top-2.5 text-muted-foreground hover:text-foreground"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>
      )}

      {/* Active Filter Banner */}
      {activeEmployee && (
        <div className="flex items-center justify-between rounded-xl bg-zinc-100 dark:bg-zinc-900 px-3.5 py-2 text-xs">
          <div className="flex items-center gap-2">
            <UserCheck className="h-4 w-4 text-zinc-700 dark:text-zinc-300" />
            <span>
              Showing orders booked by:{" "}
              <strong className="text-zinc-900 dark:text-zinc-100">{activeEmployee.name}</strong>{" "}
              ({activeEmployee.role}) • {filteredEntries.length} orders • {totalPiecesFiltered} pcs •{" "}
              <strong>Total Billed: ₹{formatInr(totalBilledFiltered)}</strong> •{" "}
              <span className="text-emerald-600 font-semibold">Done: ₹{formatInr(totalPaidFiltered)}</span> •{" "}
              <span className="text-red-600 font-semibold">Due: ₹{formatInr(totalDueFiltered)}</span>
            </span>
          </div>
          <button
            onClick={() => setSelectedEmployeeId("all")}
            className="text-[11px] font-semibold text-red-600 dark:text-red-400 hover:underline"
          >
            Show All Orders
          </button>
        </div>
      )}

      {/* Orders Table */}
      <Card className="rounded-2xl overflow-hidden border border-zinc-200/80 dark:border-zinc-800 shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-zinc-200/80 bg-zinc-50/70 font-semibold text-muted-foreground dark:border-zinc-800 dark:bg-zinc-900/50">
              <tr>
                <th className="py-3.5 pl-6 pr-3">Order / Code</th>
                <th className="px-3 py-3.5">Supplier / Mill</th>
                <th className="px-3 py-3.5">Buyer</th>
                <th className="px-3 py-3.5">Salesman</th>
                <th className="px-3 py-3.5">Packing</th>
                <th className="px-3 py-3.5">Bill Amount</th>
                <th className="px-3 py-3.5 min-w-[150px]">Payment Status & Dues</th>
                <th className="px-3 py-3.5">Delivery</th>
                <th className="py-3.5 pl-3 pr-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
              {filteredEntries.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-xs text-muted-foreground">
                    No orders found {activeEmployee ? `for ${activeEmployee.name}` : ""}.
                  </td>
                </tr>
              ) : (
                filteredEntries.map((entry) => {
                  const billTotal =
                    Number(entry.grandTotalWithGst) ||
                    (Number(entry.totalAmount) + Number(entry.gstAmount)) ||
                    0
                  const paidAmount = Number(entry.paidAmount) || 0
                  const dueAmount = Math.max(0, billTotal - paidAmount)
                  const isPaid =
                    entry.paymentStatus?.toLowerCase() === "paid" ||
                    entry.paymentStatus?.toLowerCase() === "received"
                  const isPartial = entry.paymentStatus?.toLowerCase() === "partial" || (paidAmount > 0 && dueAmount > 0)
                  const isDelivered = entry.deliveryStatus?.toLowerCase() === "delivered"
                  const visit = visitMap.get(entry.visitId)
                  const customerName = visit?.customerName || "Customer"
                  const agentName = salesmanOf(entry).name || "—"
                  const pct = billTotal > 0 ? Math.min(100, Math.round((paidAmount / billTotal) * 100)) : (isPaid ? 100 : 0)

                  return (
                    <tr
                      key={entry.id}
                      className="hover:bg-zinc-50/50 dark:hover:bg-zinc-900/50 transition-colors"
                    >
                      {/* Order Code */}
                      <td className="py-4 pl-6 pr-3">
                        <span className="font-bold text-zinc-900 dark:text-zinc-100 font-mono">
                          #{entry.orderNo}
                        </span>
                        <p className="text-[11px] text-muted-foreground font-medium">
                          {entry.itemCode}
                        </p>
                      </td>

                      {/* Supplier / Brand */}
                      <td className="px-3 py-4 font-medium text-zinc-800 dark:text-zinc-200">
                        {getDisplayName(entry)}
                        {entry.supplierName && getDisplayName(entry) !== entry.supplierName && (
                          <span className="block text-[10px] text-muted-foreground">
                            {entry.supplierName}
                          </span>
                        )}
                        {entry.supplierType && (
                          <span className="block text-[10px] text-muted-foreground">
                            {entry.supplierType}
                          </span>
                        )}
                      </td>

                      {/* Buyer */}
                      <td className="px-3 py-4 text-zinc-800 dark:text-zinc-200 font-medium">
                        {customerName}
                      </td>

                      {/* Agent */}
                      <td className="px-3 py-4">
                        <span className="inline-flex items-center gap-1 rounded-full bg-zinc-100 dark:bg-zinc-800 px-2.5 py-0.5 text-[11px] font-medium text-zinc-800 dark:text-zinc-200">
                          👤 {agentName}
                        </span>
                      </td>

                      {/* Packing */}
                      <td className="px-3 py-4">
                        <span className="font-bold text-zinc-900 dark:text-zinc-100">
                          {entry.pieces} pcs
                        </span>
                        <p className="text-[10px] text-muted-foreground">
                          {entry.caseCount} cases
                          {entry.loosePieces > 0 ? ` • ${entry.loosePieces} loose` : ""}
                        </p>
                      </td>

                      {/* Bill Amount */}
                      <td className="px-3 py-4">
                        <span className="font-bold text-zinc-900 dark:text-zinc-100">
                          ₹{formatInr(billTotal)}
                        </span>
                        <p className="text-[10px] text-muted-foreground">
                          Rate: ₹{entry.rate || entry.pricePerPiece || 0}
                          {entry.gstAmount ? ` + ₹${formatInr(entry.gstAmount)} GST` : ""}
                        </p>
                      </td>

                      {/* Payment Status & Detailed Dues */}
                      <td className="px-3 py-4">
                        <div className="space-y-1">
                          <div className="flex items-center justify-between gap-1.5">
                            <Badge
                              variant={
                                isPaid
                                  ? "success"
                                  : isPartial
                                  ? "warning"
                                  : "destructive"
                              }
                              className="text-[10px] px-1.5 py-0 font-medium"
                            >
                              {entry.paymentStatus || (paidAmount > 0 ? "Partial" : "Unpaid")}
                            </Badge>
                            <span className="text-[10px] font-semibold text-zinc-500">
                              {pct}%
                            </span>
                          </div>

                          {/* Progress bar */}
                          <div className="w-full bg-zinc-100 dark:bg-zinc-800 rounded-full h-1.5 overflow-hidden">
                            <div
                              className={cn(
                                "h-full rounded-full transition-all",
                                isPaid ? "bg-emerald-500" : pct > 0 ? "bg-amber-500" : "bg-red-500"
                              )}
                              style={{ width: `${pct}%` }}
                            />
                          </div>

                          <div className="flex items-center justify-between text-[10px] leading-tight pt-0.5">
                            <span className="text-emerald-600 dark:text-emerald-400 font-semibold" title="Amount Paid">
                              Done: ₹{formatInr(paidAmount)}
                            </span>
                            <span
                              className={cn(
                                "font-bold",
                                dueAmount > 0 ? "text-red-600 dark:text-red-400" : "text-emerald-600 dark:text-emerald-400"
                              )}
                              title="Pending Due Balance"
                            >
                              Due: ₹{formatInr(dueAmount)}
                            </span>
                          </div>

                          {entry.paymentMode && (
                            <p className="text-[9.5px] text-muted-foreground truncate">
                              💳 {entry.paymentMode} {entry.paymentRemarks ? `• ${entry.paymentRemarks}` : ""}
                            </p>
                          )}
                        </div>
                      </td>

                      {/* Delivery */}
                      <td className="px-3 py-4">
                        <Badge variant={isDelivered ? "default" : "info"}>
                          {entry.deliveryStatus || "Pending"}
                        </Badge>
                        {entry.transporter && (
                          <p className="text-[10px] text-muted-foreground mt-1 truncate max-w-[110px]" title={entry.transporter}>
                            🚛 {entry.transporter}
                          </p>
                        )}
                        {entry.lrNo && (
                          <p className="text-[10px] text-muted-foreground truncate max-w-[110px]">LR: {entry.lrNo}</p>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="py-4 pl-3 pr-6 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {!readOnly && (
                            <Button
                              variant={isDelivered ? "outline" : "default"}
                              size="sm"
                              shape="pill"
                              onClick={() => openDeliveryDialog(entry)}
                              className="h-7 text-[11px] px-2.5 font-medium"
                              title="Update delivery status, transporter and LR"
                            >
                              Delivery
                            </Button>
                          )}
                          {/* Invoice PDF button */}
                          <Button
                            variant="outline"
                            size="sm"
                            shape="pill"
                            onClick={() => handleViewInvoice(entry)}
                            className="h-7 text-[11px] px-2 font-medium text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-900/50 hover:bg-blue-50 dark:hover:bg-blue-950/40"
                            title="View & Download Wholesaler Invoice (PDF)"
                          >
                            <FileText className="h-3 w-3 mr-1" />
                            Invoice
                          </Button>
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Wholesaler / Supplier Invoice Report Viewer Modal */}
      <ReportViewerModal
        open={reportModal.open}
        onOpenChange={(open) => setReportModal((prev) => ({ ...prev, open }))}
        title={reportModal.title}
        htmlContent={reportModal.html}
        whatsAppText={reportModal.whatsAppText}
        buildHtml={reportModal.buildHtml}
      />

      {/* Delivery update (same fields as the Android app) */}
      <Dialog
        open={!!deliveryEntry}
        onOpenChange={(open) => !open && setDeliveryEntry(null)}
        title="Update Delivery"
        description={deliveryEntry ? `Order #${deliveryEntry.orderNo} • ${deliveryEntry.itemCode} • ${deliveryEntry.supplierName}` : undefined}
      >
        <div className="space-y-4 pt-1">
          <div>
            <p className="text-xs font-medium text-muted-foreground">Status</p>
            <div className="mt-1.5 grid grid-cols-2 gap-2 sm:grid-cols-4" role="group" aria-label="Delivery status">
              {["Pending", "Packed", "Dispatched", "Delivered"].map((status) => (
                <Button
                  key={status}
                  type="button"
                  size="sm"
                  shape="pill"
                  variant={deliveryStatus === status ? "default" : "outline"}
                  onClick={() => setDeliveryStatus(status)}
                  aria-pressed={deliveryStatus === status}
                  className="text-xs"
                >
                  {status}
                </Button>
              ))}
            </div>
          </div>
          <div>
            <label htmlFor="orders-delivery-transporter" className="text-xs font-medium text-muted-foreground">
              Transporter
            </label>
            <Input
              id="orders-delivery-transporter"
              list="orders-transporter-options"
              value={deliveryTransporter}
              onChange={(e) => setDeliveryTransporter(e.target.value)}
              placeholder="Pick from the Transporter master or type"
              className="mt-1.5"
            />
            <datalist id="orders-transporter-options">
              {transporters.map((t) => (
                <option key={t.id} value={t.transporterName} />
              ))}
            </datalist>
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label htmlFor="orders-delivery-lr" className="text-xs font-medium text-muted-foreground">
                LR / Bilty No.
              </label>
              <Input
                id="orders-delivery-lr"
                value={deliveryLrNo}
                onChange={(e) => setDeliveryLrNo(e.target.value)}
                placeholder="e.g. LR-55441"
                className="mt-1.5"
              />
            </div>
            <div>
              <label htmlFor="orders-delivery-lr-date" className="text-xs font-medium text-muted-foreground">
                LR Date
              </label>
              <Input
                id="orders-delivery-lr-date"
                type="date"
                value={deliveryLrDate}
                onChange={(e) => setDeliveryLrDate(e.target.value)}
                className="mt-1.5"
              />
            </div>
          </div>
          <div className="flex justify-end gap-2 pt-1">
            <Button variant="outline" shape="pill" size="sm" onClick={() => setDeliveryEntry(null)}>
              Cancel
            </Button>
            <Button shape="pill" size="sm" onClick={saveDelivery} disabled={savingDelivery}>
              {savingDelivery ? "Saving..." : "Save Delivery"}
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  )
}
