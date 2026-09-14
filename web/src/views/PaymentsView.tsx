import React, { useState, useMemo } from "react"
import {
  IndianRupee,
  Clock,
  CheckCircle,
  AlertTriangle,
  Search,
  FileText,
  Percent,
  Receipt,
  Printer,
  Calendar,
  User,
  Share2,
  X,
  Filter,
  ArrowRight
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatInr, formatDate, cn } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Tabs } from "../components/ui/Tabs"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Customer, PurchaseEntry, Supplier, Visit } from "../types"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateSupplierInvoiceHtml,
  buildSupplierInvoiceWhatsAppText,
  generateCustomerStatementHtml,
  buildCustomerStatementWhatsAppText,
  CustomerStatementData,
} from "../lib/pdfReports"

type DatePreset = "all" | "today" | "this_week" | "this_month" | "this_year" | "custom"

export function PaymentsView() {
  const { entries, visits, customers, suppliers, employees, updatePayment } = useData()

  // Filters
  const [filter, setFilter] = useState<string>("all")
  const [search, setSearch] = useState<string>("")
  const [selectedCustomerId, setSelectedCustomerId] = useState<number | "all">("all")
  const [datePreset, setDatePreset] = useState<DatePreset>("all")
  const [startDate, setStartDate] = useState<string>("")
  const [endDate, setEndDate] = useState<string>("")
  const [showCustomDateInputs, setShowCustomDateInputs] = useState<boolean>(false)

  // Inline Payment dialog state
  const [paymentEntry, setPaymentEntry] = useState<PurchaseEntry | null>(null)
  const [paymentAmount, setPaymentAmount] = useState<string>("")
  const [paymentMode, setPaymentMode] = useState<string>("Cash")
  const [paymentStatus, setPaymentStatus] = useState<string>("Paid")
  const [paymentRemarks, setPaymentRemarks] = useState<string>("")

  // Report viewer modal
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

  const visitMap = useMemo(() => {
    return new Map(visits.map((v) => [v.id, v]))
  }, [visits])

  // Selected customer object
  const selectedCustomer = useMemo(() => {
    if (selectedCustomerId === "all") return null
    return customers.find((c) => c.id === selectedCustomerId) || null
  }, [selectedCustomerId, customers])

  // Helper to extract timestamp for an entry
  const getEntryTimestamp = (entry: PurchaseEntry): number => {
    const v = visitMap.get(entry.visitId)
    if (v && v.date) {
      const parts = v.date.split("-")
      if (parts.length === 3) {
        return new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2])).getTime()
      }
      return new Date(v.date).getTime()
    }
    if (entry.createdAt) return new Date(entry.createdAt).getTime()
    return 0
  }

  // Date filtering logic
  const isEntryInDateRange = (entry: PurchaseEntry): boolean => {
    if (datePreset === "all" && !startDate && !endDate) return true

    const entryTs = getEntryTimestamp(entry)
    if (!entryTs) return true

    const d = new Date(entryTs)
    const entryMidnight = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()

    const now = new Date()
    const todayMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()

    if (datePreset === "today") {
      return entryMidnight === todayMidnight
    }

    if (datePreset === "this_week") {
      const day = now.getDay()
      const diff = now.getDate() - day + (day === 0 ? -6 : 1)
      const monday = new Date(now.setDate(diff))
      monday.setHours(0, 0, 0, 0)
      return entryMidnight >= monday.getTime()
    }

    if (datePreset === "this_month") {
      return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth()
    }

    if (datePreset === "this_year") {
      return d.getFullYear() === now.getFullYear()
    }

    if (datePreset === "custom" || startDate || endDate) {
      let matches = true
      if (startDate) {
        const sParts = startDate.split("-")
        const sMidnight = new Date(
          Number(sParts[0]),
          Number(sParts[1]) - 1,
          Number(sParts[2])
        ).getTime()
        matches = matches && entryMidnight >= sMidnight
      }
      if (endDate) {
        const eParts = endDate.split("-")
        const eMidnight = new Date(
          Number(eParts[0]),
          Number(eParts[1]) - 1,
          Number(eParts[2])
        ).getTime()
        matches = matches && entryMidnight <= eMidnight
      }
      return matches
    }

    return true
  }

  // Customer matching logic
  const isEntryForSelectedCustomer = (entry: PurchaseEntry): boolean => {
    if (!selectedCustomer) return true
    const v = visitMap.get(entry.visitId)
    if (!v) return false
    if (Number(v.customerId) === selectedCustomer.id) return true
    if (
      v.customerName &&
      selectedCustomer.name &&
      v.customerName.trim().toLowerCase() === selectedCustomer.name.trim().toLowerCase()
    )
      return true
    if (
      v.customerName &&
      selectedCustomer.firmName &&
      v.customerName.trim().toLowerCase() === selectedCustomer.firmName.trim().toLowerCase()
    )
      return true
    return false
  }

  const q = search.trim().toLowerCase()

  // Base filtered entries (matching Customer + Date Range)
  const scopedEntries = useMemo(() => {
    return entries.filter((e) => {
      if (!isEntryForSelectedCustomer(e)) return false
      if (!isEntryInDateRange(e)) return false
      return true
    })
  }, [entries, selectedCustomer, datePreset, startDate, endDate, visitMap])

  // Aggregate totals for the active scope (selected customer + date range)
  const totalBilled = scopedEntries.reduce(
    (sum, e) =>
      sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )
  const totalPaid = scopedEntries.reduce((sum, e) => sum + (Number(e.paidAmount) || 0), 0)
  const totalDue = Math.max(0, totalBilled - totalPaid)
  const collectionRate = totalBilled > 0 ? Math.round((totalPaid / totalBilled) * 100) : 0

  // Final filtered entries for table (including status tab + search)
  const filteredEntries = useMemo(() => {
    return scopedEntries.filter((e) => {
      const v = visitMap.get(e.visitId)
      const cust = v?.customerName || ""
      const matches =
        !q ||
        e.orderNo?.toLowerCase().includes(q) ||
        e.supplierName?.toLowerCase().includes(q) ||
        e.itemCode?.toLowerCase().includes(q) ||
        cust.toLowerCase().includes(q)

      if (!matches) return false

      const pStatus = (e.paymentStatus || "").toLowerCase()
      if (filter === "unpaid")
        return pStatus !== "paid" && pStatus !== "received" && pStatus !== "partial"
      if (filter === "partial") return pStatus === "partial"
      if (filter === "paid") return pStatus === "paid" || pStatus === "received"
      return true
    })
  }, [scopedEntries, q, filter, visitMap])

  // Payment Recording
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

  // Wholesaler purchase invoice
  const handleOpenInvoice = (entry: PurchaseEntry) => {
    const visit = visitMap.get(entry.visitId) || {
      id: entry.visitId,
      visitCode: `VIS-${entry.visitId}`,
      customerId: 0,
      customerName: "Buyer",
      date: entry.createdAt
        ? new Date(entry.createdAt).toISOString().split("T")[0]
        : new Date().toISOString().split("T")[0],
      employeeId: 1,
      employeeName: "Agent",
      status: "Completed",
    }

    const customer =
      customers.find((c) => c.id === visit.customerId || c.name === visit.customerName) || null
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
      title: `Invoice Voucher: #${entry.orderNo} • ${supplier.name}`,
      html,
      whatsAppText,
    })
  }

  // Download / preview Customer Consolidated Statement (PDF)
  const handleDownloadCustomerStatement = () => {
    if (!selectedCustomer) return

    let label = "All Records"
    if (datePreset === "today") label = "Today"
    else if (datePreset === "this_week") label = "This Week"
    else if (datePreset === "this_month") label = "This Month"
    else if (datePreset === "this_year") label = "This Year"
    else if (startDate && endDate) label = `${startDate} to ${endDate}`
    else if (startDate) label = `From ${startDate}`
    else if (endDate) label = `Up to ${endDate}`

    const statementData: CustomerStatementData = {
      customer: selectedCustomer,
      entries: scopedEntries,
      visits,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      dateRangeLabel: label,
    }

    const html = generateCustomerStatementHtml(statementData)
    const whatsAppText = buildCustomerStatementWhatsAppText(statementData)

    setReportModal({
      open: true,
      title: `Account Statement: ${selectedCustomer.firmName || selectedCustomer.name} (${label})`,
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
            <span>Payments & Billing Ledger</span>
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Filter by customer and date range, track paid vs pending dues, and download consolidated PDF statements.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Status Tabs */}
          <Tabs
            value={filter}
            onValueChange={setFilter}
            options={[
              { value: "all", label: "All Bills", count: scopedEntries.length },
              {
                value: "unpaid",
                label: "Pending Dues",
                count: scopedEntries.filter((e) => {
                  const s = (e.paymentStatus || "").toLowerCase()
                  return s !== "paid" && s !== "received" && s !== "partial"
                }).length,
              },
              {
                value: "partial",
                label: "Partial",
                count: scopedEntries.filter((e) => (e.paymentStatus || "").toLowerCase() === "partial").length,
              },
              {
                value: "paid",
                label: "Cleared",
                count: scopedEntries.filter((e) => {
                  const s = (e.paymentStatus || "").toLowerCase()
                  return s === "paid" || s === "received"
                }).length,
              },
            ]}
          />
        </div>
      </div>

      {/* Filter Control Bar: Customer Selector + Date Range Selector */}
      <Card className="p-4 rounded-2xl border border-zinc-200/80 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-sm space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          {/* Left: Customer Selector */}
          <div className="flex flex-1 items-center gap-2 min-w-[260px]">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-zinc-700 dark:text-zinc-300">
              <User className="h-4 w-4 text-zinc-500" />
              <span>Customer:</span>
            </div>
            <select
              value={selectedCustomerId === "all" ? "all" : String(selectedCustomerId)}
              onChange={(e) => {
                const val = e.target.value
                setSelectedCustomerId(val === "all" ? "all" : Number(val))
              }}
              aria-label="Filter by Customer"
              className="flex-1 h-9 rounded-xl border border-zinc-200 bg-zinc-50 px-3 text-xs font-medium text-zinc-900 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-100 focus:outline-none focus:ring-1 focus:ring-zinc-900 shadow-xs"
            >
              <option value="all">👥 All Customers (Consolidated Ledger)</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  👤 {c.firmName ? `${c.firmName} (${c.name})` : c.name} • {c.city || c.marketArea || "Ahmedabad"}
                </option>
              ))}
            </select>
            {selectedCustomerId !== "all" && (
              <Button
                variant="ghost"
                size="sm"
                shape="pill"
                onClick={() => setSelectedCustomerId("all")}
                className="h-8 px-2 text-xs text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/40"
                title="Clear customer filter"
              >
                <X className="h-3.5 w-3.5" />
              </Button>
            )}
          </div>

          {/* Right: Date Range Preset Selector */}
          <div className="flex flex-wrap items-center gap-2">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-zinc-700 dark:text-zinc-300">
              <Calendar className="h-4 w-4 text-zinc-500" />
              <span>Date:</span>
            </div>
            <div className="flex items-center gap-1 rounded-xl bg-zinc-100 p-1 dark:bg-zinc-800 text-xs">
              {(
                [
                  { id: "all", label: "All Time" },
                  { id: "today", label: "Today" },
                  { id: "this_month", label: "This Month" },
                  { id: "this_year", label: "This Year" },
                  { id: "custom", label: "Custom" },
                ] as const
              ).map((preset) => (
                <button
                  key={preset.id}
                  onClick={() => {
                    setDatePreset(preset.id)
                    if (preset.id === "custom") {
                      setShowCustomDateInputs(true)
                    }
                  }}
                  className={cn(
                    "rounded-lg px-2.5 py-1 text-xs font-medium transition-all",
                    datePreset === preset.id
                      ? "bg-white text-zinc-900 shadow-xs dark:bg-zinc-900 dark:text-zinc-50"
                      : "text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
                  )}
                >
                  {preset.label}
                </button>
              ))}
            </div>

            {(datePreset === "custom" || showCustomDateInputs) && (
              <Button
                variant="ghost"
                size="sm"
                shape="pill"
                onClick={() => {
                  setShowCustomDateInputs(!showCustomDateInputs)
                }}
                className="h-8 text-xs font-semibold px-2 text-blue-600"
              >
                {showCustomDateInputs ? "Hide Range" : "Set Dates"}
              </Button>
            )}
          </div>
        </div>

        {/* Expandable Custom Date Range Inputs */}
        {(datePreset === "custom" || showCustomDateInputs) && (
          <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-zinc-100 dark:border-zinc-800 text-xs">
            <div className="flex items-center gap-1.5">
              <span className="text-muted-foreground font-medium">From:</span>
              <Input
                type="date"
                value={startDate}
                onChange={(e) => {
                  setStartDate(e.target.value)
                  setDatePreset("custom")
                }}
                className="h-8 w-36 text-xs"
              />
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-muted-foreground font-medium">To:</span>
              <Input
                type="date"
                value={endDate}
                onChange={(e) => {
                  setEndDate(e.target.value)
                  setDatePreset("custom")
                }}
                className="h-8 w-36 text-xs"
              />
            </div>
            {(startDate || endDate) && (
              <Button
                variant="ghost"
                size="sm"
                shape="pill"
                onClick={() => {
                  setStartDate("")
                  setEndDate("")
                  setDatePreset("all")
                  setShowCustomDateInputs(false)
                }}
                className="h-8 text-xs text-red-600 hover:bg-red-50"
              >
                Reset Dates
              </Button>
            )}
          </div>
        )}
      </Card>

      {/* Customer Account Statement Banner (Appears when a customer is selected) */}
      {selectedCustomer && (
        <Card className="p-4 rounded-2xl border-2 border-blue-200 dark:border-blue-900/60 bg-gradient-to-r from-blue-50/70 via-indigo-50/40 to-blue-50/30 dark:from-blue-950/30 dark:via-zinc-900 dark:to-zinc-900 shadow-sm">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <Badge variant="info" className="text-[11px] font-bold">
                  Client Statement
                </Badge>
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-50">
                  {selectedCustomer.firmName ? `${selectedCustomer.firmName} (${selectedCustomer.name})` : selectedCustomer.name}
                </h3>
              </div>
              <p className="text-xs text-zinc-600 dark:text-zinc-400">
                Phone: <strong>{selectedCustomer.phone}</strong> • GSTIN:{" "}
                <strong>{selectedCustomer.gstin || selectedCustomer.gstNumber || "Unregistered"}</strong> •{" "}
                {selectedCustomer.city || selectedCustomer.marketArea || "Ahmedabad"} • Credit:{" "}
                <strong>{selectedCustomer.creditDays || 30} Days</strong>
              </p>
              <div className="flex items-center gap-3 text-xs pt-1">
                <span>
                  Orders in Scope: <strong>{scopedEntries.length}</strong>
                </span>
                <span>•</span>
                <span>
                  Billed: <strong>₹{formatInr(totalBilled)}</strong>
                </span>
                <span>•</span>
                <span className="text-emerald-600 font-bold">
                  Paid: ₹{formatInr(totalPaid)}
                </span>
                <span>•</span>
                <span className={totalDue > 0 ? "text-red-600 font-bold" : "text-emerald-600 font-bold"}>
                  Due: ₹{formatInr(totalDue)}
                </span>
              </div>
            </div>

            {/* Statement Action Buttons */}
            <div className="flex flex-wrap items-center gap-2">
              <Button
                size="sm"
                shape="pill"
                onClick={handleDownloadCustomerStatement}
                className="bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs shadow-sm gap-1.5"
              >
                <Printer className="h-3.5 w-3.5" />
                <span>Download Statement (PDF)</span>
              </Button>
            </div>
          </div>
        </Card>
      )}

      {/* Financial Overview Cards (Dynamically reflects the customer & date range) */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Card className="rounded-2xl border-zinc-200/80 p-4 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-sm">
          <div className="flex items-center justify-between text-muted-foreground">
            <span className="text-xs font-semibold">Total Invoiced</span>
            <Receipt className="h-4 w-4 text-zinc-500" />
          </div>
          <p className="text-xl sm:text-2xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
            ₹{formatInr(totalBilled)}
          </p>
          <span className="text-[11px] text-muted-foreground mt-0.5 block">
            Across {scopedEntries.length} orders
          </span>
        </Card>

        <Card className="rounded-2xl border-zinc-200/80 p-4 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-sm">
          <div className="flex items-center justify-between text-emerald-600 dark:text-emerald-400">
            <span className="text-xs font-semibold">Total Collected</span>
            <CheckCircle className="h-4 w-4 text-emerald-500" />
          </div>
          <p className="text-xl sm:text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
            ₹{formatInr(totalPaid)}
          </p>
          <span className="text-[11px] text-muted-foreground mt-0.5 block">
            Verified cash & bank receipts
          </span>
        </Card>

        <Card className="rounded-2xl border-zinc-200/80 p-4 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-sm">
          <div className="flex items-center justify-between text-red-600 dark:text-red-400">
            <span className="text-xs font-semibold">Outstanding Due</span>
            <AlertTriangle className="h-4 w-4 text-red-500" />
          </div>
          <p className="text-xl sm:text-2xl font-bold text-red-600 dark:text-red-400 mt-1">
            ₹{formatInr(totalDue)}
          </p>
          <span className="text-[11px] text-muted-foreground mt-0.5 block">
            Pending receivable dues
          </span>
        </Card>

        <Card className="rounded-2xl border-zinc-200/80 p-4 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-sm">
          <div className="flex items-center justify-between text-blue-600 dark:text-blue-400">
            <span className="text-xs font-semibold">Collection Ratio</span>
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

      {/* Search Bar */}
      <div className="relative">
        <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Filter ledger by order #, item code, supplier name, or buyer..."
          className="pl-9"
        />
      </div>

      {/* Payment Ledger Table */}
      <Card className="rounded-2xl overflow-hidden border border-zinc-200/80 dark:border-zinc-800 shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-zinc-200/80 bg-zinc-50/70 font-semibold text-muted-foreground dark:border-zinc-800 dark:bg-zinc-900/50">
              <tr>
                <th className="py-3.5 pl-6 pr-3">Date</th>
                <th className="px-3 py-3.5">Order / Item</th>
                <th className="px-3 py-3.5">Buyer</th>
                <th className="px-3 py-3.5">Supplier / Mill</th>
                <th className="px-3 py-3.5">Total Bill</th>
                <th className="px-3 py-3.5">Payment Done</th>
                <th className="px-3 py-3.5">Balance Due</th>
                <th className="px-3 py-3.5">Status</th>
                <th className="py-3.5 pl-3 pr-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
              {filteredEntries.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-10 text-center text-xs text-muted-foreground">
                    No payment records found for the selected criteria.
                  </td>
                </tr>
              ) : (
                filteredEntries.map((entry) => {
                  const v = visitMap.get(entry.visitId)
                  const orderDate =
                    v?.date ||
                    (entry.createdAt
                      ? new Date(entry.createdAt).toISOString().split("T")[0]
                      : "—")
                  const bill =
                    Number(entry.grandTotalWithGst) ||
                    (Number(entry.totalAmount) + Number(entry.gstAmount)) ||
                    0
                  const paid = Number(entry.paidAmount) || 0
                  const due = Math.max(0, bill - paid)
                  const isPaid =
                    entry.paymentStatus?.toLowerCase() === "paid" ||
                    entry.paymentStatus?.toLowerCase() === "received"
                  const isPartial =
                    entry.paymentStatus?.toLowerCase() === "partial" || (paid > 0 && due > 0)

                  return (
                    <tr
                      key={entry.id}
                      className="hover:bg-zinc-50/50 dark:hover:bg-zinc-900/50 transition-colors"
                    >
                      <td className="py-4 pl-6 pr-3 font-mono font-medium text-zinc-600 dark:text-zinc-400 whitespace-nowrap">
                        {orderDate !== "—" ? formatDate(orderDate) : "—"}
                      </td>
                      <td className="px-3 py-4">
                        <span className="font-bold text-zinc-900 dark:text-zinc-100 font-mono">
                          #{entry.orderNo}
                        </span>
                        <p className="text-[11px] text-muted-foreground">
                          {entry.itemCode} ({entry.pieces} pcs)
                        </p>
                      </td>
                      <td className="px-3 py-4 font-medium text-zinc-900 dark:text-zinc-100">
                        {v?.customerName || "Customer"}
                      </td>
                      <td className="px-3 py-4 text-muted-foreground">{entry.supplierName}</td>
                      <td className="px-3 py-4 font-semibold text-zinc-900 dark:text-zinc-100">
                        ₹{formatInr(bill)}
                      </td>
                      <td className="px-3 py-4 text-emerald-600 dark:text-emerald-400 font-semibold">
                        ₹{formatInr(paid)}
                        {entry.paymentMode && (
                          <span className="block text-[10px] text-muted-foreground font-normal">
                            via {entry.paymentMode}
                          </span>
                        )}
                      </td>
                      <td className="px-3 py-4 font-bold">
                        <span
                          className={
                            due > 0
                              ? "text-red-600 dark:text-red-400"
                              : "text-emerald-600 dark:text-emerald-400"
                          }
                        >
                          ₹{formatInr(due)}
                        </span>
                      </td>
                      <td className="px-3 py-4">
                        <Badge
                          variant={
                            isPaid
                              ? "success"
                              : isPartial
                              ? "warning"
                              : "destructive"
                          }
                        >
                          {entry.paymentStatus || (paid > 0 ? "Partial" : "Unpaid")}
                        </Badge>
                      </td>
                      <td className="py-4 pl-3 pr-6 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            variant="outline"
                            size="sm"
                            shape="pill"
                            onClick={() => handleOpenInvoice(entry)}
                            className="h-7 text-[11px] px-2.5 font-medium text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-900/50 hover:bg-blue-50 dark:hover:bg-blue-950/40"
                            title="View Invoice PDF"
                          >
                            <FileText className="h-3 w-3 mr-1" />
                            Invoice
                          </Button>
                          <Button
                            size="sm"
                            shape="pill"
                            onClick={() => handleOpenPayment(entry)}
                            className="h-7 text-[11px] px-3 font-semibold"
                          >
                            Update
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
        title="Record Payment in Ledger"
        description={`Order #${paymentEntry?.orderNo} • Total Bill: ₹${formatInr(
          (Number(paymentEntry?.totalAmount) || 0) + (Number(paymentEntry?.gstAmount) || 0)
        )}`}
      >
        <div className="space-y-4 pt-2">
          <div>
            <label className="text-xs font-medium text-muted-foreground">Status</label>
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

      {/* Invoice & Statement Report Viewer Modal */}
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
