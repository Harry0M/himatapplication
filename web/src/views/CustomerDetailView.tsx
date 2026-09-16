import React, { useState, useMemo } from "react"
import {
  ArrowLeft,
  Calendar,
  IndianRupee,
  Receipt,
  Truck,
  MapPin,
  Clock,
  Phone,
  Mail,
  Home,
  User,
  ShieldCheck,
  Search,
  CheckCircle2,
  AlertCircle,
  Package,
  Edit2,
  Filter,
  Download,
  Building2,
  ExternalLink,
  Printer,
  FileText,
  CreditCard,
  Share2,
  Image as ImageIcon,
  ZoomIn,
  Store,
  Layers,
  Sparkles,
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatInr, formatDate, cn } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Tabs } from "../components/ui/Tabs"
import { Customer, Visit, PurchaseEntry } from "../types"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateCustomerDayReportHtml,
  buildCustomerReportWhatsAppText,
} from "../lib/pdfReports"
import { exportCustomerToTallyXml } from "../lib/tallyExport"

interface CustomerDetailViewProps {
  customerId: number
  onBack: () => void
  onEdit?: (customer: Customer) => void
  onNavigate?: (tab: string) => void
}

type TimeframePreset = "all" | "today" | "last_7" | "this_month" | "custom"

export function CustomerDetailView({
  customerId,
  onBack,
  onEdit,
  onNavigate,
}: CustomerDetailViewProps) {
  const { customers, visits, entries, suppliers, transporters, employees, packGroups } = useData()

  const [activeTab, setActiveTab] = useState<string>("orders")
  const [searchQuery, setSearchQuery] = useState<string>("")
  const [timeframe, setTimeframe] = useState<TimeframePreset>("all")
  const [customStartDate, setCustomStartDate] = useState<string>("")
  const [customEndDate, setCustomEndDate] = useState<string>("")
  const [statusFilter, setStatusFilter] = useState<string>("all")
  const [supplierFilter, setSupplierFilter] = useState<string>("all")

  // Image Lightbox Modal State
  const [lightbox, setLightbox] = useState<{ open: boolean; url: string; title: string }>({
    open: false,
    url: "",
    title: "",
  })

  // PDF Report Modal State
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

  // Find target customer
  const customer = useMemo(() => {
    return customers.find((c) => c.id === customerId) || null
  }, [customers, customerId])

  // Map of all visits belonging to this customer
  const customerVisits = useMemo(() => {
    if (!customer) return []
    const custNameLower = customer.name.trim().toLowerCase()
    const custFirmLower = (customer.firmName || "").trim().toLowerCase()
    return visits.filter(
      (v) =>
        Number(v.customerId) === customer.id ||
        (v.customerName &&
          (v.customerName.trim().toLowerCase() === custNameLower ||
            (custFirmLower && v.customerName.trim().toLowerCase() === custFirmLower)))
    )
  }, [visits, customer])

  const visitMap = useMemo(() => {
    return new Map<number, Visit>(customerVisits.map((v) => [Number(v.id), v]))
  }, [customerVisits])

  const visitIdsSet = useMemo(() => {
    return new Set<number>(customerVisits.map((v) => Number(v.id)))
  }, [customerVisits])

  // All entries belonging to this customer
  const allCustomerEntries = useMemo(() => {
    return entries.filter((e) => visitIdsSet.has(Number(e.visitId)))
  }, [entries, visitIdsSet])

  // Supplier Map for rapid lookup
  const supplierMap = useMemo(() => {
    return new Map(suppliers.map((s) => [s.id, s]))
  }, [suppliers])

  // Unique suppliers who have supplied to this customer
  const customerSuppliersList = useMemo(() => {
    const sMap = new Map<string, string>()
    allCustomerEntries.forEach((e) => {
      const name = e.supplierName || (e.supplierId ? supplierMap.get(e.supplierId)?.name : "")
      if (name) {
        sMap.set(String(e.supplierId || name), name)
      }
    })
    return Array.from(sMap.entries())
  }, [allCustomerEntries, supplierMap])

  // Analytics Metrics
  const totalVolumePieces = useMemo(() => {
    return allCustomerEntries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  }, [allCustomerEntries])

  const totalBilledAmount = useMemo(() => {
    return allCustomerEntries.reduce((sum, e) => {
      return (
        sum +
        (Number(e.grandTotalWithGst) ||
          (Number(e.totalAmount) || 0) + (Number(e.gstAmount) || 0))
      )
    }, 0)
  }, [allCustomerEntries])

  const pendingEntries = useMemo(() => {
    return allCustomerEntries.filter((e) => e.deliveryStatus !== "Delivered")
  }, [allCustomerEntries])

  const deliveredEntries = useMemo(() => {
    return allCustomerEntries.filter((e) => e.deliveryStatus === "Delivered")
  }, [allCustomerEntries])

  // Filtered Entries based on real-time filters
  const filteredEntries = useMemo(() => {
    return allCustomerEntries.filter((entry) => {
      const visit = visitMap.get(Number(entry.visitId))
      const entryDate = visit?.date || ""

      // 1. Text Search
      if (searchQuery.trim()) {
        const query = searchQuery.trim().toLowerCase()
        const matchItem = entry.itemCode?.toLowerCase().includes(query)
        const matchOrder = entry.orderNo?.toLowerCase().includes(query)
        const matchSupp = entry.supplierName?.toLowerCase().includes(query)
        const matchTrans = entry.transporter?.toLowerCase().includes(query)
        const matchNotes = entry.notes?.toLowerCase().includes(query)
        const matchDate = entryDate.toLowerCase().includes(query)
        if (!matchItem && !matchOrder && !matchSupp && !matchTrans && !matchNotes && !matchDate) {
          return false
        }
      }

      // 2. Timeframe Filter
      if (timeframe !== "all" && entryDate) {
        const entryTime = new Date(entryDate).getTime()
        const now = new Date()
        const todayMid = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()

        if (timeframe === "today") {
          if (entryTime < todayMid) return false
        } else if (timeframe === "last_7") {
          const sevenDaysAgo = todayMid - 7 * 24 * 60 * 60 * 1000
          if (entryTime < sevenDaysAgo) return false
        } else if (timeframe === "this_month") {
          const firstDayOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime()
          if (entryTime < firstDayOfMonth) return false
        } else if (timeframe === "custom") {
          if (customStartDate) {
            const start = new Date(customStartDate).getTime()
            if (entryTime < start) return false
          }
          if (customEndDate) {
            const end = new Date(customEndDate).getTime() + 24 * 60 * 60 * 1000 - 1
            if (entryTime > end) return false
          }
        }
      }

      // 3. Status Filter
      if (statusFilter !== "all") {
        if (statusFilter === "Pending" && entry.deliveryStatus === "Delivered") return false
        if (statusFilter === "Delivered" && entry.deliveryStatus !== "Delivered") return false
        if (statusFilter === "Dispatched" && entry.deliveryStatus !== "Dispatched") return false
      }

      // 4. Supplier Filter
      if (supplierFilter !== "all") {
        if (String(entry.supplierId) !== supplierFilter && entry.supplierName !== supplierFilter) {
          return false
        }
      }

      return true
    })
  }, [allCustomerEntries, visitMap, searchQuery, timeframe, customStartDate, customEndDate, statusFilter, supplierFilter])

  // PDF Day Report Action
  const handleGenerateDayReport = () => {
    if (!customer) return
    const primaryVisit = customerVisits[0] || {
      id: Date.now(),
      visitCode: `CUST-${customer.id}`,
      customerId: customer.id,
      customerName: customer.name,
      date: new Date().toISOString().split("T")[0],
      employeeId: 1,
      employeeName: "Himat Textile",
      status: "Completed",
    }
    const reportData = {
      visit: primaryVisit,
      customer,
      salesman: employees[0] || null,
      entries: allCustomerEntries,
      packGroups: packGroups || [],
    }
    const html = generateCustomerDayReportHtml(reportData)
    const text = buildCustomerReportWhatsAppText(reportData)
    setReportModal({
      open: true,
      title: `Day Summary Voucher - ${customer.firmName || customer.name}`,
      html,
      whatsAppText: text,
    })
  }

  // Tally XML Export
  const handleExportTally = () => {
    if (!customer) return
    exportCustomerToTallyXml(customer)
  }

  if (!customer) {
    return (
      <div className="space-y-4">
        <Button onClick={onBack} variant="outline" size="sm" className="gap-1.5">
          <ArrowLeft className="h-4 w-4" /> Back to Customers
        </Button>
        <Card className="p-8 text-center">
          <AlertCircle className="h-10 w-10 text-amber-500 mx-auto mb-2" />
          <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100">Customer Not Found</h3>
          <p className="text-xs text-zinc-500 mt-1">This customer record may have been deleted or does not exist.</p>
        </Card>
      </div>
    )
  }

  // Parse Contacts & Outlets
  const contactsList = (customer.contacts && customer.contacts.length > 0)
    ? customer.contacts
    : [
        { name: "Primary Contact", phone: customer.phone, designation: "Owner / Desk" },
        ...(customer.phone2 ? [{ name: "Alternate Line 2", phone: customer.phone2, designation: "Office" }] : []),
        ...(customer.phone3 ? [{ name: "Alternate Line 3", phone: customer.phone3, designation: "Accounts" }] : []),
        ...(customer.phone4 ? [{ name: "Alternate Line 4", phone: customer.phone4, designation: "Booking" }] : []),
        ...(customer.phone5 ? [{ name: "Alternate Line 5", phone: customer.phone5, designation: "Other" }] : []),
      ].filter((c) => c.phone)

  const outletsList = (customer.outlets && customer.outlets.length > 0)
    ? customer.outlets
    : (customer.shopAddress || customer.shopLocation)
    ? [
        {
          name: "Main Outlet",
          address: customer.shopAddress || customer.shopLocation || customer.address || "",
          city: customer.city || "Ahmedabad",
          mapLink: customer.shopMapLink || customer.mapLink || "",
        },
      ]
    : []

  // KYC Photos List
  const kycDocs = [
    { label: "Aadhaar Card Photo", uri: customer.aadharPhotoUri, key: "aadhar" },
    { label: "GST Registration Certificate", uri: customer.gstCertPhotoUri, key: "gst" },
    { label: "PAN Card Photo", uri: customer.panPhotoUri, key: "pan" },
    { label: "Shop Front / Signboard Photo", uri: customer.shopPhotoUri, key: "shop" },
    { label: "Purchaser / Owner Photo", uri: customer.purchaserPhotoUri, key: "purchaser" },
    { label: "Cancelled Cheque Photo", uri: customer.cancelChequePhotoUri, key: "cheque" },
  ]

  const garmentList = (customer.garmentTypes || customer.preferredCategories || "")
    .split(",")
    .map((g) => g.trim())
    .filter(Boolean)

  return (
    <div className="space-y-6">
      {/* Top Navigation Bar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <Button
            onClick={onBack}
            variant="outline"
            size="sm"
            className="h-8 gap-1.5 text-xs text-zinc-700 dark:text-zinc-300 shadow-sm"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            <span>Customers Directory</span>
          </Button>
          <div className="h-4 w-[1px] bg-zinc-200 dark:bg-zinc-800" />
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="font-mono text-xs font-bold text-blue-700 dark:text-blue-400 bg-blue-50/50 dark:bg-blue-950/40">
              {customer.customerId || `CUST-${customer.id}`}
            </Badge>
            <Badge
              variant="outline"
              className={cn(
                "text-xs font-semibold",
                customer.customerType === "Credit"
                  ? "bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-950/40 dark:text-purple-400"
                  : "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-400"
              )}
            >
              {customer.customerType || "Credit"} Customer
            </Badge>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="flex flex-wrap items-center gap-2">
          {onEdit && (
            <Button
              onClick={() => onEdit(customer)}
              variant="outline"
              size="sm"
              className="h-8 gap-1.5 text-xs shadow-sm"
            >
              <Edit2 className="h-3.5 w-3.5 text-zinc-600" />
              <span>Edit Customer</span>
            </Button>
          )}

          <Button
            onClick={handleExportTally}
            variant="outline"
            size="sm"
            className="h-8 gap-1.5 text-xs shadow-sm border-blue-200 hover:bg-blue-50/50 text-blue-700 dark:border-blue-900 dark:text-blue-400"
            title="Export standard Tally XML ledger import"
          >
            <Download className="h-3.5 w-3.5" />
            <span>Tally XML</span>
          </Button>

          <Button
            onClick={handleGenerateDayReport}
            variant="outline"
            size="sm"
            className="h-8 gap-1.5 text-xs shadow-sm bg-emerald-50/50 border-emerald-200 text-emerald-700 hover:bg-emerald-100/50 dark:bg-emerald-950/30 dark:border-emerald-800 dark:text-emerald-400"
          >
            <Printer className="h-3.5 w-3.5" />
            <span>PDF Statement</span>
          </Button>
        </div>
      </div>

      {/* Hero Header Card */}
      <Card className="border border-zinc-200/80 p-5 dark:border-zinc-800 shadow-sm bg-gradient-to-br from-white via-zinc-50/30 to-blue-50/20 dark:from-zinc-950 dark:via-zinc-950 dark:to-blue-950/10">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="flex items-start gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-black text-xl shadow-md shadow-blue-500/20">
              {(customer.firmName || customer.name || "C")[0].toUpperCase()}
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="text-xl font-black tracking-tight text-zinc-900 dark:text-zinc-50">
                  {customer.firmName || customer.name}
                </h1>
                {customer.firmName && customer.name && customer.firmName !== customer.name && (
                  <span className="text-xs font-semibold text-zinc-500 dark:text-zinc-400">
                    (Owner: {customer.name})
                  </span>
                )}
              </div>

              <div className="mt-1.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-zinc-600 dark:text-zinc-400">
                <div className="flex items-center gap-1 font-medium">
                  <MapPin className="h-3.5 w-3.5 text-zinc-400" />
                  <span>{customer.city || "Ahmedabad"}</span>
                  {customer.district && <span>• {customer.district}</span>}
                  {customer.state && <span>• {customer.state}</span>}
                </div>

                {customer.phone && (
                  <div className="flex items-center gap-1">
                    <Phone className="h-3.5 w-3.5 text-zinc-400" />
                    <a href={`tel:${customer.phone}`} className="hover:underline font-mono">
                      {customer.phone}
                    </a>
                  </div>
                )}

                {customer.gstin && (
                  <div className="flex items-center gap-1 font-mono text-[11px] bg-zinc-100 dark:bg-zinc-800 px-1.5 py-0.5 rounded text-zinc-700 dark:text-zinc-300">
                    GSTIN: <strong>{customer.gstin}</strong>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Credit Terms summary pill */}
          <div className="flex items-center gap-3 rounded-xl border border-zinc-200/70 bg-white/80 p-3 dark:border-zinc-800 dark:bg-zinc-900/80 shadow-xs">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-purple-50 text-purple-600 dark:bg-purple-950/50">
              <CreditCard className="h-4 w-4" />
            </div>
            <div className="text-xs">
              <div className="text-muted-foreground font-medium">Credit Facility</div>
              <div className="font-bold text-zinc-900 dark:text-zinc-100">
                {customer.customerType === "Cash" ? (
                  <span className="text-emerald-600 font-semibold">Cash Basis (No Credit)</span>
                ) : (
                  <>
                    <span>{customer.creditDays || 30} Days</span>
                    {customer.creditLimit ? (
                      <span className="text-zinc-500 font-normal"> • Limit: {formatInr(customer.creditLimit)}</span>
                    ) : null}
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      </Card>

      {/* Key Metric Statistics Cards (CRM/ERP Style) */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-5">
        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">Total Visits</span>
            <Receipt className="h-4 w-4 text-blue-600" />
          </div>
          <p className="mt-1.5 text-xl font-black text-zinc-900 dark:text-zinc-100">{customerVisits.length}</p>
          <span className="text-[10px] text-zinc-500 mt-0.5 block">{allCustomerEntries.length} order items</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">Total Volume</span>
            <Package className="h-4 w-4 text-indigo-600" />
          </div>
          <p className="mt-1.5 text-xl font-black text-zinc-900 dark:text-zinc-100">
            {totalVolumePieces.toLocaleString("en-IN")} <span className="text-xs font-normal text-zinc-500">pcs</span>
          </p>
          <span className="text-[10px] text-zinc-500 mt-0.5 block">Procured across all trips</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">Total Billed</span>
            <IndianRupee className="h-4 w-4 text-emerald-600" />
          </div>
          <p className="mt-1.5 text-xl font-black text-zinc-900 dark:text-zinc-100">
            {formatInr(totalBilledAmount)}
          </p>
          <span className="text-[10px] text-emerald-600 font-medium mt-0.5 block">Estimated total value</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">Pending Dispatch</span>
            <Truck className="h-4 w-4 text-amber-600" />
          </div>
          <p className="mt-1.5 text-xl font-black text-amber-600 dark:text-amber-400">
            {pendingEntries.length} <span className="text-xs font-normal text-zinc-500">orders</span>
          </p>
          <span className="text-[10px] text-zinc-500 mt-0.5 block">
            {pendingEntries.reduce((s, e) => s + (Number(e.pieces) || 0), 0).toLocaleString("en-IN")} pcs in transit
          </span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950 col-span-2 sm:col-span-4 lg:col-span-1">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">Fulfilled</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
          </div>
          <p className="mt-1.5 text-xl font-black text-emerald-600 dark:text-emerald-400">
            {deliveredEntries.length} <span className="text-xs font-normal text-zinc-500">orders</span>
          </p>
          <span className="text-[10px] text-zinc-500 mt-0.5 block">100% delivered to retailer</span>
        </Card>
      </div>

      {/* Main Tabbed Navigation */}
      <Tabs
        options={[
          { value: "orders", label: `Orders History (${allCustomerEntries.length})` },
          { value: "profile", label: "Firm & Contact Profile" },
          { value: "kyc", label: `KYC & Cloud Photos (${kycDocs.filter((d) => d.uri).length}/6)` },
        ]}
        value={activeTab}
        onValueChange={(val) => setActiveTab(val as "orders" | "profile" | "kyc")}
      />

      {/* TAB 1: ORDERS & PURCHASES HISTORY */}
      {activeTab === "orders" && (
        <div className="space-y-4">
          {/* Comprehensive Filters Bar */}
          <Card className="p-4 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
            <div className="flex flex-col gap-3">
              {/* Row 1: Search & Quick Timeframe Buttons */}
              <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div className="relative flex-1 max-w-md">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
                  <Input
                    placeholder="Search by item code, order #, supplier, date..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-9 h-8 text-xs bg-zinc-50/50 dark:bg-zinc-900"
                  />
                  {searchQuery && (
                    <button
                      onClick={() => setSearchQuery("")}
                      className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-muted-foreground hover:text-zinc-900"
                    >
                      ×
                    </button>
                  )}
                </div>

                {/* Timeframe Presets */}
                <div className="flex items-center gap-1 overflow-x-auto pb-1 lg:pb-0">
                  <span className="text-[11px] font-semibold text-zinc-500 uppercase tracking-wider mr-1 shrink-0 flex items-center gap-1">
                    <Calendar className="h-3 w-3" /> Date:
                  </span>
                  {(
                    [
                      { id: "all", label: "All Time" },
                      { id: "today", label: "Today" },
                      { id: "last_7", label: "Last 7 Days" },
                      { id: "this_month", label: "This Month" },
                      { id: "custom", label: "Custom Range" },
                    ] as const
                  ).map((tf) => (
                    <Button
                      key={tf.id}
                      variant={timeframe === tf.id ? "default" : "outline"}
                      size="sm"
                      onClick={() => setTimeframe(tf.id)}
                      className={cn(
                        "h-7 text-xs px-2.5 rounded-lg shrink-0",
                        timeframe === tf.id
                          ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900"
                          : "text-zinc-600 dark:text-zinc-400"
                      )}
                    >
                      {tf.label}
                    </Button>
                  ))}
                </div>
              </div>

              {/* Row 2: Secondary Filters (Custom Date Inputs, Status, Supplier) */}
              <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-zinc-100 dark:border-zinc-800">
                {timeframe === "custom" && (
                  <div className="flex items-center gap-1.5 mr-2">
                    <span className="text-xs text-muted-foreground">From:</span>
                    <Input
                      type="date"
                      value={customStartDate}
                      onChange={(e) => setCustomStartDate(e.target.value)}
                      className="h-7 text-xs w-32 px-2 py-0"
                    />
                    <span className="text-xs text-muted-foreground">To:</span>
                    <Input
                      type="date"
                      value={customEndDate}
                      onChange={(e) => setCustomEndDate(e.target.value)}
                      className="h-7 text-xs w-32 px-2 py-0"
                    />
                  </div>
                )}

                {/* Status Filter */}
                <div className="flex items-center gap-1">
                  <span className="text-[11px] font-semibold text-zinc-500 mr-1">Status:</span>
                  <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="h-7 text-xs rounded-lg border border-zinc-200 bg-white px-2 dark:border-zinc-800 dark:bg-zinc-900 text-zinc-700 dark:text-zinc-300 font-medium"
                  >
                    <option value="all">All Delivery Statuses</option>
                    <option value="Pending">Pending / In Transit</option>
                    <option value="Delivered">Delivered Only</option>
                    <option value="Dispatched">Dispatched Only</option>
                  </select>
                </div>

                {/* Supplier Filter */}
                {customerSuppliersList.length > 0 && (
                  <div className="flex items-center gap-1">
                    <span className="text-[11px] font-semibold text-zinc-500 mr-1">Supplier:</span>
                    <select
                      value={supplierFilter}
                      onChange={(e) => setSupplierFilter(e.target.value)}
                      className="h-7 text-xs rounded-lg border border-zinc-200 bg-white px-2 dark:border-zinc-800 dark:bg-zinc-900 text-zinc-700 dark:text-zinc-300 font-medium max-w-[180px] truncate"
                    >
                      <option value="all">All Suppliers / Mills</option>
                      {customerSuppliersList.map(([id, name]) => (
                        <option key={id} value={id}>
                          {name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Active Filter Clear */}
                {(searchQuery || timeframe !== "all" || statusFilter !== "all" || supplierFilter !== "all") && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setSearchQuery("")
                      setTimeframe("all")
                      setStatusFilter("all")
                      setSupplierFilter("all")
                    }}
                    className="h-7 text-xs text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950/30 ml-auto"
                  >
                    Reset Filters
                  </Button>
                )}
              </div>
            </div>
          </Card>

          {/* Orders Table */}
          <Card className="border border-zinc-200/80 dark:border-zinc-800 shadow-xs overflow-hidden bg-white dark:bg-zinc-950">
            <div className="p-3.5 border-b border-zinc-100 dark:border-zinc-800 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                  Purchases & Order History
                </span>
                <Badge variant="outline" className="text-[10px] font-semibold">
                  {filteredEntries.length} of {allCustomerEntries.length} orders
                </Badge>
              </div>
              <span className="text-xs text-muted-foreground">
                Total Shown: <strong>{filteredEntries.reduce((s, e) => s + (Number(e.pieces) || 0), 0).toLocaleString("en-IN")} pcs</strong>
              </span>
            </div>

            {filteredEntries.length === 0 ? (
              <div className="py-12 text-center text-xs text-muted-foreground">
                <Package className="h-8 w-8 text-zinc-300 mx-auto mb-2 dark:text-zinc-700" />
                <p className="font-semibold text-zinc-800 dark:text-zinc-200">No orders match the selected filters</p>
                <p className="text-[11px] text-zinc-500 mt-0.5">Try widening your date range or clearing search keywords.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-zinc-100 bg-zinc-50/75 dark:border-zinc-800 dark:bg-zinc-900/50 font-semibold text-zinc-600 dark:text-zinc-400">
                      <th className="py-2.5 px-3">Date</th>
                      <th className="py-2.5 px-3">Order / Visit</th>
                      <th className="py-2.5 px-3">Supplier / Mill</th>
                      <th className="py-2.5 px-3">Design / Item Code</th>
                      <th className="py-2.5 px-3 text-right">Quantity</th>
                      <th className="py-2.5 px-3 text-right">Pack Size</th>
                      <th className="py-2.5 px-3 text-right">Rate</th>
                      <th className="py-2.5 px-3 text-right">Amount</th>
                      <th className="py-2.5 px-3 text-center">Delivery Status</th>
                      <th className="py-2.5 px-3 text-center">LR / Dispatch</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
                    {filteredEntries.map((entry) => {
                      const visit = visitMap.get(Number(entry.visitId))
                      const isDelivered = entry.deliveryStatus === "Delivered"

                      return (
                        <tr
                          key={entry.id}
                          className="hover:bg-zinc-50/60 dark:hover:bg-zinc-900/40 transition-colors"
                        >
                          <td className="py-2.5 px-3 font-mono text-[11px] text-zinc-600 dark:text-zinc-400 whitespace-nowrap">
                            {visit?.date ? formatDate(visit.date) : "—"}
                          </td>
                          <td className="py-2.5 px-3 whitespace-nowrap">
                            <span className="font-mono font-bold text-blue-700 dark:text-blue-400">
                              {entry.orderNo || visit?.visitCode || `#${entry.id}`}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 font-medium text-zinc-900 dark:text-zinc-100">
                            {entry.supplierName || "—"}
                          </td>
                          <td className="py-2.5 px-3">
                            <span className="font-mono font-semibold bg-zinc-100 dark:bg-zinc-800 px-1.5 py-0.5 rounded text-zinc-800 dark:text-zinc-200">
                              {entry.itemCode || "—"}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-right font-bold text-zinc-900 dark:text-zinc-100">
                            {entry.pieces} pcs
                          </td>
                          <td className="py-2.5 px-3 text-right text-zinc-600 dark:text-zinc-400">
                            {entry.caseCount > 0 ? (
                              <span>
                                {entry.caseCount} cs ({entry.caseSize || 24})
                                {entry.loosePieces > 0 ? ` + ${entry.loosePieces}` : ""}
                              </span>
                            ) : (
                              <span>{entry.pieces} loose</span>
                            )}
                          </td>
                          <td className="py-2.5 px-3 text-right font-mono text-zinc-600 dark:text-zinc-400">
                            {entry.pricePerPiece || entry.rate ? `₹${entry.pricePerPiece || entry.rate}` : "—"}
                          </td>
                          <td className="py-2.5 px-3 text-right font-bold font-mono text-zinc-900 dark:text-zinc-100 whitespace-nowrap">
                            {entry.grandTotalWithGst || entry.totalAmount ? formatInr(entry.grandTotalWithGst || entry.totalAmount) : "—"}
                          </td>
                          <td className="py-2.5 px-3 text-center whitespace-nowrap">
                            <Badge
                              variant="outline"
                              className={cn(
                                "text-[10px] font-bold px-2 py-0.5",
                                isDelivered
                                  ? "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-400"
                                  : entry.deliveryStatus === "Dispatched"
                                  ? "bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-950/40 dark:text-blue-400"
                                  : "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/40 dark:text-amber-400"
                              )}
                            >
                              {entry.deliveryStatus || "Pending"}
                            </Badge>
                          </td>
                          <td className="py-2.5 px-3 text-center text-[11px] text-zinc-500 whitespace-nowrap">
                            {entry.lrNo ? (
                              <span className="font-mono text-zinc-700 dark:text-zinc-300">
                                LR: {entry.lrNo}
                              </span>
                            ) : (
                              <span className="text-zinc-400 italic">No LR yet</span>
                            )}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </div>
      )}

      {/* TAB 2: FIRM & MASTER PROFILE */}
      {activeTab === "profile" && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {/* Firm Identity Card */}
            <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950 space-y-4">
              <div className="flex items-center gap-2 pb-2 border-b border-zinc-100 dark:border-zinc-800">
                <Store className="h-4 w-4 text-blue-600" />
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">Firm & Tax Identity</h3>
              </div>

              <div className="grid grid-cols-2 gap-4 text-xs">
                <div>
                  <span className="text-muted-foreground block text-[11px]">Shop / Firm Name</span>
                  <span className="font-bold text-zinc-900 dark:text-zinc-100">{customer.firmName || customer.name}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Owner / Purchaser Name</span>
                  <span className="font-bold text-zinc-900 dark:text-zinc-100">{customer.name}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Customer ID</span>
                  <span className="font-mono font-bold text-blue-600">{customer.customerId || `CUST-${customer.id}`}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Account Type</span>
                  <Badge variant="outline" className="font-semibold text-[11px] mt-0.5">
                    {customer.customerType || "Credit"} Customer
                  </Badge>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">GSTIN Number</span>
                  <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">{customer.gstin || "Unregistered"}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">PAN Number</span>
                  <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">{customer.panNumber || "—"}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Date of Birth (DOB)</span>
                  <span className="font-medium text-zinc-800 dark:text-zinc-200">{customer.dob || "—"}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Community / Religion</span>
                  <span className="font-medium text-zinc-800 dark:text-zinc-200">{customer.religion || "—"}</span>
                </div>
              </div>

              {/* Garment Categories Dealt With */}
              {garmentList.length > 0 && (
                <div className="pt-2">
                  <span className="text-[11px] text-muted-foreground font-medium block mb-1.5">
                    Garments / Fabrics Dealt With:
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {garmentList.map((tag) => (
                      <Badge key={tag} variant="outline" className="text-xs bg-zinc-50 dark:bg-zinc-900 text-zinc-700 dark:text-zinc-300 font-medium">
                        {tag}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
            </Card>

            {/* Logistics & Agent Referral */}
            <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950 space-y-4">
              <div className="flex items-center gap-2 pb-2 border-b border-zinc-100 dark:border-zinc-800">
                <Truck className="h-4 w-4 text-emerald-600" />
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">Logistics & CRM Linkages</h3>
              </div>

              <div className="grid grid-cols-2 gap-4 text-xs">
                <div className="col-span-2">
                  <span className="text-muted-foreground block text-[11px]">Preferred Transporter</span>
                  <span className="font-bold text-zinc-900 dark:text-zinc-100">
                    {customer.preferredTransporterName || "Standard Transport"}
                  </span>
                  {customer.transportPreference && (
                    <p className="text-[11px] text-zinc-500 italic mt-0.5">"{customer.transportPreference}"</p>
                  )}
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Handling Agent / Staff</span>
                  <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {customer.addedByAgentName || "Direct / Admin"}
                  </span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Referred By</span>
                  <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {customer.referredBy || "Direct Inquiry"}
                  </span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Credit Term Period</span>
                  <span className="font-bold text-purple-700 dark:text-purple-400">
                    {customer.creditDays || 30} Days
                  </span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Credit Limit</span>
                  <span className="font-bold text-purple-700 dark:text-purple-400">
                    {customer.creditLimit ? formatInr(customer.creditLimit) : "No Limit Set"}
                  </span>
                </div>
              </div>

              {customer.notes && (
                <div className="pt-2">
                  <span className="text-[11px] text-muted-foreground font-medium block mb-1">
                    Special CRM Notes / Ledger Instructions:
                  </span>
                  <p className="text-xs text-zinc-700 dark:text-zinc-300 bg-zinc-50 dark:bg-zinc-900 p-2.5 rounded-lg border border-zinc-200/60 dark:border-zinc-800 italic">
                    "{customer.notes}"
                  </p>
                </div>
              )}
            </Card>
          </div>

          {/* Contacts Info (Up to 5) */}
          <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-100 dark:border-zinc-800">
              <div className="flex items-center gap-2">
                <Phone className="h-4 w-4 text-blue-600" />
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">
                  Contact Persons & Direct Lines ({contactsList.length})
                </h3>
              </div>
              <span className="text-xs text-zinc-500">Up to 5 verified contact lines</span>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 pt-4">
              {contactsList.map((contact, idx) => (
                <div
                  key={idx}
                  className="rounded-xl border border-zinc-200/80 bg-zinc-50/50 p-3.5 dark:border-zinc-800 dark:bg-zinc-900/50 space-y-1.5"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                      {contact.name || `Line ${idx + 1}`}
                    </span>
                    <Badge variant="outline" className="text-[10px] font-medium bg-white dark:bg-zinc-800">
                      {contact.designation || (idx === 0 ? "Owner / Desk" : "Office")}
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between pt-1">
                    <span className="font-mono text-sm font-bold text-zinc-800 dark:text-zinc-200">
                      {contact.phone}
                    </span>
                    <div className="flex items-center gap-1">
                      <a
                        href={`tel:${contact.phone}`}
                        className="p-1 rounded-md hover:bg-zinc-200 dark:hover:bg-zinc-700 text-blue-600"
                        title="Call"
                      >
                        <Phone className="h-3.5 w-3.5" />
                      </a>
                      <a
                        href={`https://wa.me/91${contact.phone.replace(/\D/g, "")}`}
                        target="_blank"
                        rel="noreferrer"
                        className="p-1 rounded-md hover:bg-zinc-200 dark:hover:bg-zinc-700 text-emerald-600"
                        title="WhatsApp"
                      >
                        <Share2 className="h-3.5 w-3.5" />
                      </a>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </Card>

          {/* Outlets & Branch Locations (Up to 5) */}
          <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-100 dark:border-zinc-800">
              <div className="flex items-center gap-2">
                <MapPin className="h-4 w-4 text-red-500" />
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">
                  Retail Outlets & Branch Locations ({outletsList.length})
                </h3>
              </div>
              <span className="text-xs text-zinc-500">Up to 5 branch locations with Google Maps GPS</span>
            </div>

            {outletsList.length === 0 ? (
              <p className="py-4 text-xs text-muted-foreground italic">No outlet locations recorded.</p>
            ) : (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 pt-4">
                {outletsList.map((outlet, idx) => (
                  <div
                    key={idx}
                    className="rounded-xl border border-zinc-200/80 bg-zinc-50/50 p-3.5 dark:border-zinc-800 dark:bg-zinc-900/50 space-y-2"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                        {outlet.name || `Outlet #${idx + 1}`}
                      </span>
                      <span className="text-[11px] font-medium text-zinc-500">
                        {outlet.city || customer.city || "Ahmedabad"}
                      </span>
                    </div>
                    <p className="text-xs text-zinc-700 dark:text-zinc-300 line-clamp-2">
                      {outlet.address || "Address not specified"}
                    </p>
                    {outlet.mapLink && (
                      <a
                        href={outlet.mapLink}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center gap-1 text-[11px] font-semibold text-blue-600 hover:underline pt-1"
                      >
                        <ExternalLink className="h-3 w-3" />
                        <span>Open in Google Maps</span>
                      </a>
                    )}
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      )}

      {/* TAB 3: KYC DOCUMENTS & CLOUD STORAGE MEDIA */}
      {activeTab === "kyc" && (
        <div className="space-y-4">
          <Card className="p-4 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
                  <ShieldCheck className="h-4 w-4 text-emerald-600" />
                  <span>Cloud Verification & KYC Documents</span>
                </h3>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Stored securely in Firebase Object Storage (<code>gs://himatsms.firebasestorage.app</code>). Click any document to view full resolution.
                </p>
              </div>
              <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-200 text-xs font-bold">
                {kycDocs.filter((d) => d.uri).length} / 6 Attached
              </Badge>
            </div>
          </Card>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {kycDocs.map((doc) => {
              const hasFile = Boolean(doc.uri)

              return (
                <Card
                  key={doc.key}
                  className={cn(
                    "overflow-hidden border p-3 transition-all",
                    hasFile
                      ? "border-zinc-200/80 hover:shadow-md dark:border-zinc-800"
                      : "border-dashed border-zinc-300/80 bg-zinc-50/50 dark:border-zinc-800 dark:bg-zinc-900/30"
                  )}
                >
                  <div className="flex items-center justify-between pb-2 border-b border-zinc-100 dark:border-zinc-800">
                    <span className="text-xs font-bold text-zinc-900 dark:text-zinc-100 line-clamp-1">
                      {doc.label}
                    </span>
                    {hasFile ? (
                      <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-200 text-[10px] font-bold shrink-0">
                        Cloud ✓
                      </Badge>
                    ) : (
                      <Badge variant="outline" className="text-zinc-400 text-[10px] shrink-0">
                        Not Uploaded
                      </Badge>
                    )}
                  </div>

                  <div className="mt-3">
                    {hasFile ? (
                      <div className="space-y-2">
                        <div
                          onClick={() => setLightbox({ open: true, url: doc.uri!, title: doc.label })}
                          className="group relative h-48 w-full cursor-pointer overflow-hidden rounded-lg bg-zinc-100 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800"
                        >
                          <img
                            src={doc.uri}
                            alt={doc.label}
                            className="h-full w-full object-cover transition-transform group-hover:scale-105"
                            onError={(e) => {
                              ;(e.target as HTMLElement).style.display = "none"
                            }}
                          />
                          <div className="absolute inset-0 flex items-center justify-center bg-black/40 opacity-0 transition-opacity group-hover:opacity-100">
                            <Button size="sm" variant="secondary" className="h-8 text-xs gap-1.5">
                              <ZoomIn className="h-3.5 w-3.5" /> Full Resolution
                            </Button>
                          </div>
                        </div>

                        <div className="flex items-center justify-between pt-1">
                          <span className="text-[11px] font-mono text-zinc-500 truncate max-w-[160px]">
                            {doc.uri?.split("/").pop()?.split("?")[0] || "doc.jpg"}
                          </span>
                          <a
                            href={doc.uri}
                            target="_blank"
                            rel="noreferrer"
                            className="text-[11px] font-semibold text-blue-600 hover:underline flex items-center gap-1"
                          >
                            <ExternalLink className="h-3 w-3" />
                            <span>Open URL</span>
                          </a>
                        </div>
                      </div>
                    ) : (
                      <div className="flex h-48 flex-col items-center justify-center text-center p-4">
                        <ImageIcon className="h-8 w-8 text-zinc-300 dark:text-zinc-700 mb-1.5" />
                        <p className="text-xs font-medium text-zinc-500">No document photo uploaded</p>
                        <p className="text-[10px] text-zinc-400 mt-0.5">
                          Edit this customer to attach {doc.label.toLowerCase()}
                        </p>
                      </div>
                    )}
                  </div>
                </Card>
              )
            })}
          </div>
        </div>
      )}

      {/* Lightbox Modal */}
      {lightbox.open && (
        <Dialog
          open={lightbox.open}
          onOpenChange={(open) => !open && setLightbox({ open: false, url: "", title: "" })}
          title={lightbox.title}
          description="Stored in Firebase Cloud Storage"
        >
          <div className="space-y-4 pt-2">
            <div className="max-h-[75vh] overflow-hidden rounded-xl bg-zinc-950 flex items-center justify-center border border-zinc-800">
              <img
                src={lightbox.url}
                alt={lightbox.title}
                className="max-h-[72vh] w-auto object-contain"
              />
            </div>
            <div className="flex items-center justify-between">
              <a
                href={lightbox.url}
                target="_blank"
                rel="noreferrer"
                className="text-xs font-semibold text-blue-600 hover:underline inline-flex items-center gap-1"
              >
                <ExternalLink className="h-3.5 w-3.5" /> Open in New Browser Tab
              </a>
              <Button size="sm" onClick={() => setLightbox({ open: false, url: "", title: "" })}>
                Close Preview
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Report Viewer Modal */}
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
