import React, { useState } from "react"
import {
  Receipt,
  Search,
  Filter,
  IndianRupee,
  Truck,
  X,
  UserCheck,
  FileText,
  CheckCircle2,
  AlertCircle,
  Clock,
  Printer
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatInr, formatDate, cn } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Tabs } from "../components/ui/Tabs"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { PurchaseEntry, Supplier, Customer } from "../types"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateSupplierInvoiceHtml,
  buildSupplierInvoiceWhatsAppText,
} from "../lib/pdfReports"

export function OrdersView() {
  const {
    entries,
    visits,
    customers,
    suppliers,
    employees,
    selectedEmployeeId,
    setSelectedEmployeeId,
    updatePayment,
    updateDelivery,
  } = useData()

  const [search, setSearch] = useState<string>("" )
  const [showSearch, setShowSearch] = useState<boolean>(false)
  const [statusFilter, setStatusFilter] = useState<string>("all")

  // Inline Payment dialog state
  const [paymentEntry, setPaymentEntry] = useState<PurchaseEntry | null>(null)
  const [paymentAmount, setPaymentAmount] = useState<string>("")
  const [paymentMode, setPaymentMode] = useState<string>("Cash")
  const [paymentStatus, setPaymentStatus] = useState<string>("Paid")
  const [paymentRemarks, setPaymentRemarks] = useState<string>("")

  // Inline Delivery dialog state
  const [deliveryEntry, setDeliveryEntry] = useState<PurchaseEntry | null>(null)
  const [deliveryStatus, setDeliveryStatus] = useState<string>("Dispatched")
  const [transporter, setTransporter] = useState<string>("")
  const [lrNo, setLrNo] = useState<string>("")

  // Report modal state
  const [reportModal, setReportModal] = useState<{
    open: boolean
    title: string
    html: string
    whatsAppText: string
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

  const q = search.trim().toLowerCase()
  const filteredEntries = entries.filter((e) => {
    const visit = visitMap.get(e.visitId)
    const custName = visit?.customerName || ""
    const agentName = visit?.employeeName || ""

    // 1. Employee filter
    if (activeEmployee) {
      const matchEmpId = visit && Number(visit.employeeId) === activeEmployee.id
      const matchEmpName =
        agentName.trim().toLowerCase() === activeEmployee.name.trim().toLowerCase()
      if (!matchEmpId && !matchEmpName) return false
    }

    // 2. Search filter
    const matchesSearch =
      !q ||
      e.orderNo?.toLowerCase().includes(q) ||
      e.itemCode?.toLowerCase().includes(q) ||
      e.supplierName?.toLowerCase().includes(q) ||
      custName.toLowerCase().includes(q) ||
      agentName.toLowerCase().includes(q)

    if (!matchesSearch) return false

    // 3. Status filter
    const pStatus = (e.paymentStatus || "").toLowerCase()
    if (statusFilter === "paid") return pStatus === "paid" || pStatus === "received"
    if (statusFilter === "partial") return pStatus === "partial"
    if (statusFilter === "unpaid") return pStatus !== "paid" && pStatus !== "received" && pStatus !== "partial"
    if (statusFilter === "pending_delivery") return e.deliveryStatus?.toLowerCase() !== "delivered"

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

  const handleOpenPayment = (entry: PurchaseEntry) => {
    const total =
      Number(entry.grandTotalWithGst) ||
      (Number(entry.totalAmount) + Number(entry.gstAmount)) ||
      0
    const currentPaid = Number(entry.paidAmount) || 0
    const due = Math.max(0, total - currentPaid)

    setPaymentEntry(entry)
    setPaymentAmount(due > 0 ? due.toString() : total.toString())
    setPaymentMode(entry.paymentMode || "Cash")
    setPaymentStatus(due === 0 ? "Paid" : "Partial")
    setPaymentRemarks(entry.paymentRemarks || "")
  }

  const handleSavePayment = async () => {
    if (!paymentEntry) return
    const amountVal = Number(paymentAmount) || 0
    const total =
      Number(paymentEntry.grandTotalWithGst) ||
      (Number(paymentEntry.totalAmount) + Number(paymentEntry.gstAmount)) ||
      0
    
    // Auto-adjust status if amount matches total
    let finalStatus = paymentStatus
    if (amountVal >= total) {
      finalStatus = "Paid"
    } else if (amountVal > 0 && amountVal < total) {
      finalStatus = "Partial"
    } else if (amountVal === 0) {
      finalStatus = "Unpaid"
    }

    await updatePayment(
      paymentEntry.id,
      finalStatus,
      paymentMode,
      amountVal,
      paymentRemarks
    )
    setPaymentEntry(null)
  }

  const handleOpenDelivery = (entry: PurchaseEntry) => {
    setDeliveryEntry(entry)
    setDeliveryStatus(entry.deliveryStatus || "Dispatched")
    setTransporter(entry.transporter || "")
    setLrNo(entry.lrNo || "")
  }

  const handleSaveDelivery = async () => {
    if (!deliveryEntry) return
    await updateDelivery(deliveryEntry.id, deliveryStatus, transporter, lrNo)
    setDeliveryEntry(null)
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
      title: `Supplier Voucher: #${entry.orderNo} • ${supplier.name}`,
      html,
      whatsAppText,
    })
  }

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
            <span>Orders & Purchase Invoices</span>
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Full purchase ledger with item codes, packing breakdown, live payment tracking, and PDF invoices.
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
              aria-label="Filter by Sales Agent"
              className="h-8 rounded-full border border-zinc-200 bg-white px-3 text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200 focus:outline-none focus:ring-1 focus:ring-zinc-900 shadow-sm"
            >
              <option value="all">👥 All Sales Agents</option>
              {employees.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  👤 {emp.name} ({emp.role})
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

          {/* Status Tabs */}
          <Tabs
            value={statusFilter}
            onValueChange={setStatusFilter}
            options={[
              { value: "all", label: "All Orders", count: entries.length },
              {
                value: "unpaid",
                label: "Unpaid",
                count: entries.filter((e) => {
                  const s = (e.paymentStatus || "").toLowerCase()
                  return s !== "paid" && s !== "received" && s !== "partial"
                }).length,
              },
              {
                value: "partial",
                label: "Partial",
                count: entries.filter((e) => (e.paymentStatus || "").toLowerCase() === "partial").length,
              },
              {
                value: "paid",
                label: "Paid",
                count: entries.filter((e) => {
                  const s = (e.paymentStatus || "").toLowerCase()
                  return s === "paid" || s === "received"
                }).length,
              },
              {
                value: "pending_delivery",
                label: "In Transit",
                count: entries.filter((e) => e.deliveryStatus?.toLowerCase() !== "delivered").length,
              },
            ]}
          />
        </div>
      </div>

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
                <th className="px-3 py-3.5">Assigned Agent</th>
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
                  const agentName = visit?.employeeName || "Agent"
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

                      {/* Supplier */}
                      <td className="px-3 py-4 font-medium text-zinc-800 dark:text-zinc-200">
                        {entry.supplierName}
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
                      </td>

                      {/* Actions */}
                      <td className="py-4 pl-3 pr-6 text-right">
                        <div className="flex items-center justify-end gap-1.5">
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

                          {/* Record Payment */}
                          <Button
                            variant="outline"
                            size="sm"
                            shape="pill"
                            onClick={() => handleOpenPayment(entry)}
                            className="h-7 text-[11px] px-2 font-medium"
                          >
                            Payment
                          </Button>

                          {/* Update Delivery */}
                          <Button
                            variant="secondary"
                            size="sm"
                            shape="pill"
                            onClick={() => handleOpenDelivery(entry)}
                            className="h-7 text-[11px] px-2 font-medium"
                          >
                            Delivery
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

      {/* Record Payment Dialog */}
      <Dialog
        open={!!paymentEntry}
        onOpenChange={(open) => !open && setPaymentEntry(null)}
        title="Record Bill Payment"
        description={`Order #${paymentEntry?.orderNo} • ${paymentEntry?.supplierName} • Total Bill: ₹${formatInr(
          (Number(paymentEntry?.totalAmount) || 0) + (Number(paymentEntry?.gstAmount) || 0)
        )}`}
      >
        <div className="space-y-4 pt-2">
          <div>
            <label className="text-xs font-medium text-muted-foreground">Payment Status</label>
            <div className="mt-1.5 flex gap-2">
              {["Paid", "Partial", "Unpaid"].map((status) => (
                <Button
                  key={status}
                  size="sm"
                  shape="pill"
                  variant={paymentStatus === status ? "default" : "outline"}
                  onClick={() => setPaymentStatus(status)}
                  className="flex-1 text-xs"
                >
                  {status}
                </Button>
              ))}
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between">
              <label className="text-xs font-medium text-muted-foreground">Amount Paid (₹)</label>
              {paymentEntry && (
                <span className="text-[11px] text-muted-foreground">
                  Total Bill: ₹{formatInr(
                    (Number(paymentEntry.totalAmount) || 0) + (Number(paymentEntry.gstAmount) || 0)
                  )}
                </span>
              )}
            </div>
            <Input
              type="number"
              value={paymentAmount}
              onChange={(e) => setPaymentAmount(e.target.value)}
              className="mt-1.5 font-bold"
              placeholder="Enter amount paid"
            />
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground">Payment Mode</label>
            <div className="mt-1.5 flex gap-2">
              {["Cash", "UPI", "Bank / Cheque"].map((mode) => (
                <Button
                  key={mode}
                  size="sm"
                  shape="pill"
                  variant={paymentMode === mode ? "default" : "outline"}
                  onClick={() => setPaymentMode(mode)}
                  className="flex-1 text-xs"
                >
                  {mode}
                </Button>
              ))}
            </div>
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground">Reference / Remarks</label>
            <Input
              value={paymentRemarks}
              onChange={(e) => setPaymentRemarks(e.target.value)}
              placeholder="e.g. Cheque #, UTR #, or partial note"
              className="mt-1.5"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button
              variant="outline"
              shape="pill"
              size="sm"
              onClick={() => setPaymentEntry(null)}
            >
              Cancel
            </Button>
            <Button shape="pill" size="sm" onClick={handleSavePayment}>
              Save Payment
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Update Delivery Dialog */}
      <Dialog
        open={!!deliveryEntry}
        onOpenChange={(open) => !open && setDeliveryEntry(null)}
        title="Update Dispatch & Delivery"
        description={`Order #${deliveryEntry?.orderNo} • ${deliveryEntry?.itemCode}`}
      >
        <div className="space-y-4 pt-2">
          <div>
            <label className="text-xs font-medium text-muted-foreground">Delivery Status</label>
            <div className="mt-1.5 flex gap-2">
              {["Pending", "Packed", "Dispatched", "Delivered"].map((status) => (
                <Button
                  key={status}
                  size="sm"
                  shape="pill"
                  variant={deliveryStatus === status ? "default" : "outline"}
                  onClick={() => setDeliveryStatus(status)}
                  className="flex-1 text-xs"
                >
                  {status}
                </Button>
              ))}
            </div>
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground">Transporter Name</label>
            <Input
              value={transporter}
              onChange={(e) => setTransporter(e.target.value)}
              placeholder="e.g. V-Trans, ARC, Safexpress"
              className="mt-1.5"
            />
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground">LR / Bilty Number</label>
            <Input
              value={lrNo}
              onChange={(e) => setLrNo(e.target.value)}
              placeholder="e.g. LR-99882"
              className="mt-1.5"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button
              variant="outline"
              shape="pill"
              size="sm"
              onClick={() => setDeliveryEntry(null)}
            >
              Cancel
            </Button>
            <Button shape="pill" size="sm" onClick={handleSaveDelivery}>
              Update Delivery
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Wholesaler / Supplier Invoice Report Viewer Modal */}
      <ReportViewerModal
        open={reportModal.open}
        onOpenChange={(open) => setReportModal((prev) => ({ ...prev, open }))}
        title={reportModal.title}
        htmlContent={reportModal.html}
        whatsAppText={reportModal.whatsAppText}
      />
    </div>
  )
}
