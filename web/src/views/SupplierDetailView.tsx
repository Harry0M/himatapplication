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
  Share2,
  Image as ImageIcon,
  ZoomIn,
  Store,
  Layers,
  Sparkles,
  Tag,
  Factory,
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatInr, formatDate, cn } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Tabs } from "../components/ui/Tabs"
import { Supplier, Visit, PurchaseEntry, Customer } from "../types"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateSupplierInvoiceHtml,
  buildSupplierInvoiceWhatsAppText,
} from "../lib/pdfReports"
import { exportSupplierToTallyXml } from "../lib/tallyExport"

interface SupplierDetailViewProps {
  supplierId: number
  onBack: () => void
  onEdit?: (supplier: Supplier) => void
  onNavigate?: (tab: string) => void
}

type TimeframePreset = "all" | "today" | "last_7" | "this_month" | "custom"

export function SupplierDetailView({
  supplierId,
  onBack,
  onEdit,
  onNavigate,
}: SupplierDetailViewProps) {
  const { suppliers, visits, entries, customers, employees, markets, brands } = useData()

  const [activeTab, setActiveTab] = useState<string>("orders")
  const [searchQuery, setSearchQuery] = useState<string>("")
  const [timeframe, setTimeframe] = useState<TimeframePreset>("all")
  const [customStartDate, setCustomStartDate] = useState<string>("")
  const [customEndDate, setCustomEndDate] = useState<string>("")
  const [statusFilter, setStatusFilter] = useState<string>("all")
  const [customerFilter, setCustomerFilter] = useState<string>("all")

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

  // Target Supplier
  const supplier = useMemo(() => {
    return suppliers.find((s) => s.id === supplierId) || null
  }, [suppliers, supplierId])

  // Customer Map
  const customerMap = useMemo(() => {
    return new Map<number, Customer>(customers.map((c) => [c.id, c]))
  }, [customers])

  // Visit Map
  const visitMap = useMemo(() => {
    return new Map<number, Visit>(visits.map((v) => [v.id, v]))
  }, [visits])

  // All entries supplied by this mill/supplier
  const allSupplierEntries = useMemo(() => {
    if (!supplier) return []
    const suppNameLower = supplier.name.trim().toLowerCase()
    const suppFirmLower = (supplier.firmName || "").trim().toLowerCase()
    return entries.filter(
      (e) =>
        Number(e.supplierId) === supplier.id ||
        (e.supplierName &&
          (e.supplierName.trim().toLowerCase() === suppNameLower ||
            (suppFirmLower && e.supplierName.trim().toLowerCase() === suppFirmLower)))
    )
  }, [entries, supplier])

  // Unique buyer customers list
  const uniqueBuyersList = useMemo(() => {
    const cMap = new Map<string, string>()
    allSupplierEntries.forEach((e) => {
      const visit = visitMap.get(Number(e.visitId))
      const cId = visit?.customerId || 0
      const cName = visit?.customerName || (cId ? customerMap.get(cId)?.name : "")
      if (cName) {
        cMap.set(String(cId || cName), cName)
      }
    })
    return Array.from(cMap.entries())
  }, [allSupplierEntries, visitMap, customerMap])

  // Analytics Metrics
  const totalVolumePieces = useMemo(() => {
    return allSupplierEntries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  }, [allSupplierEntries])

  const totalCasesCount = useMemo(() => {
    return allSupplierEntries.reduce((sum, e) => sum + (Number(e.caseCount) || 0), 0)
  }, [allSupplierEntries])

  const totalLoosePieces = useMemo(() => {
    return allSupplierEntries.reduce((sum, e) => sum + (Number(e.loosePieces) || 0), 0)
  }, [allSupplierEntries])

  const totalBilledAmount = useMemo(() => {
    return allSupplierEntries.reduce((sum, e) => {
      return (
        sum +
        (Number(e.grandTotalWithGst) ||
          (Number(e.totalAmount) || 0) + (Number(e.gstAmount) || 0))
      )
    }, 0)
  }, [allSupplierEntries])

  const pendingEntries = useMemo(() => {
    return allSupplierEntries.filter((e) => e.deliveryStatus !== "Delivered")
  }, [allSupplierEntries])

  const deliveredEntries = useMemo(() => {
    return allSupplierEntries.filter((e) => e.deliveryStatus === "Delivered")
  }, [allSupplierEntries])

  // Filtered entries
  const filteredEntries = useMemo(() => {
    return allSupplierEntries.filter((entry) => {
      const visit = visitMap.get(Number(entry.visitId))
      const entryDate = visit?.date || ""
      const custName = visit?.customerName || ""

      // 1. Text Search
      if (searchQuery.trim()) {
        const query = searchQuery.trim().toLowerCase()
        const matchItem = entry.itemCode?.toLowerCase().includes(query)
        const matchOrder = entry.orderNo?.toLowerCase().includes(query)
        const matchCust = custName.toLowerCase().includes(query)
        const matchNotes = entry.notes?.toLowerCase().includes(query)
        const matchDate = entryDate.toLowerCase().includes(query)
        if (!matchItem && !matchOrder && !matchCust && !matchNotes && !matchDate) {
          return false
        }
      }

      // 2. Timeframe
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

      // 4. Customer Filter
      if (customerFilter !== "all") {
        const cId = String(visit?.customerId || "")
        if (cId !== customerFilter && custName !== customerFilter) {
          return false
        }
      }

      return true
    })
  }, [allSupplierEntries, visitMap, searchQuery, timeframe, customStartDate, customEndDate, statusFilter, customerFilter])

  // PDF Supplier Voucher Action
  const handleGenerateSupplierVoucher = () => {
    if (!supplier) return
    const primaryVisit = visits[0] || {
      id: Date.now(),
      visitCode: "SUP-STMT",
      customerId: 1,
      customerName: "All Buyers",
      date: new Date().toISOString().split("T")[0],
      employeeId: 1,
      employeeName: "Himat Textile",
      status: "Completed",
    }
    const reportData = {
      supplier,
      visit: primaryVisit,
      entries: allSupplierEntries,
      customer: customers[0] || null,
      salesman: employees[0] || null,
    }
    const html = generateSupplierInvoiceHtml(reportData)
    const text = buildSupplierInvoiceWhatsAppText(reportData)
    setReportModal({
      open: true,
      title: `Supplier Procurement Voucher - ${supplier.firmName || supplier.name}`,
      html,
      whatsAppText: text,
    })
  }

  // Tally Export
  const handleExportTally = () => {
    if (!supplier) return
    exportSupplierToTallyXml(supplier)
  }

  if (!supplier) {
    return (
      <div className="space-y-4">
        <Button onClick={onBack} variant="outline" size="sm" className="gap-1.5">
          <ArrowLeft className="h-4 w-4" /> Back to Suppliers
        </Button>
        <Card className="p-8 text-center">
          <AlertCircle className="h-10 w-10 text-amber-500 mx-auto mb-2" />
          <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100">Supplier Not Found</h3>
          <p className="text-xs text-zinc-500 mt-1">This supplier record may have been deleted or does not exist.</p>
        </Card>
      </div>
    )
  }

  // Parse Contacts & Factories
  const contactsList = (supplier.phones && supplier.phones.length > 0)
    ? supplier.phones.map((p, idx) => ({
        name: idx === 0 ? "Key Contact / Desk" : `Line #${idx + 1}`,
        phone: p,
        designation: idx === 0 ? supplier.contactPerson || "Primary" : "Factory / Office",
      }))
    : [
        { name: supplier.contactPerson || "Key Contact", phone: supplier.phone, designation: "Primary Desk" },
        ...(supplier.phone2 ? [{ name: "Line 2", phone: supplier.phone2, designation: "Billing / Accounts" }] : []),
        ...(supplier.phone3 ? [{ name: "Line 3", phone: supplier.phone3, designation: "Godown Hub" }] : []),
        ...(supplier.phone4 ? [{ name: "Line 4", phone: supplier.phone4, designation: "Mill Unit" }] : []),
        ...(supplier.phone5 ? [{ name: "Line 5", phone: supplier.phone5, designation: "Other Line" }] : []),
      ].filter((c) => c.phone)

  const factoriesList = (supplier.factories && supplier.factories.length > 0)
    ? supplier.factories
    : supplier.officeAddress || supplier.officeLocation
    ? [
        {
          name: "Main Production Unit / Mill",
          address: supplier.officeAddress || supplier.officeLocation || supplier.address || "",
          city: supplier.city || "Ahmedabad",
        },
      ]
    : []

  const showroomsList = (supplier.outlets && supplier.outlets.length > 0)
    ? supplier.outlets
    : []

  const verificationDocs = [
    { label: "Visiting Card Photo", uri: supplier.visitingCardPhotoUri, key: "card" },
    { label: "Shop / Mill Front Photo", uri: supplier.shopPhotoUri, key: "shop" },
  ]

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
            <span>Suppliers Directory</span>
          </Button>
          <div className="h-4 w-[1px] bg-zinc-200 dark:bg-zinc-800" />
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="font-mono text-xs font-bold text-indigo-700 dark:text-indigo-400 bg-indigo-50/50 dark:bg-indigo-950/40">
              {supplier.supplierId || `SUP-${supplier.id}`}
            </Badge>
            <Badge
              variant="outline"
              className={cn(
                "text-xs font-semibold",
                supplier.type === "Manufacturer"
                  ? "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/40 dark:text-amber-400"
                  : "bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-950/40 dark:text-blue-400"
              )}
            >
              {supplier.type || "Manufacturer"}
            </Badge>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="flex flex-wrap items-center gap-2">
          {onEdit && (
            <Button
              onClick={() => onEdit(supplier)}
              variant="outline"
              size="sm"
              className="h-8 gap-1.5 text-xs shadow-sm"
            >
              <Edit2 className="h-3.5 w-3.5 text-zinc-600" />
              <span>Edit Supplier</span>
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
            onClick={handleGenerateSupplierVoucher}
            variant="outline"
            size="sm"
            className="h-8 gap-1.5 text-xs shadow-sm bg-indigo-50/50 border-indigo-200 text-indigo-700 hover:bg-indigo-100/50 dark:bg-indigo-950/30 dark:border-indigo-800 dark:text-indigo-400"
          >
            <Printer className="h-3.5 w-3.5" />
            <span>Supplier Voucher</span>
          </Button>
        </div>
      </div>

      {/* Hero Header Card */}
      <Card className="border border-zinc-200/80 p-5 dark:border-zinc-800 shadow-sm bg-gradient-to-br from-white via-zinc-50/30 to-amber-50/20 dark:from-zinc-950 dark:via-zinc-950 dark:to-amber-950/10">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="flex items-start gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-amber-600 to-orange-600 text-white font-black text-xl shadow-md shadow-amber-500/20">
              {(supplier.firmName || supplier.name || "S")[0].toUpperCase()}
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="text-xl font-black tracking-tight text-zinc-900 dark:text-zinc-50">
                  {supplier.firmName || supplier.name}
                </h1>
                {supplier.brand && (
                  <Badge variant="outline" className="bg-purple-50 text-purple-700 border-purple-200 text-xs font-semibold dark:bg-purple-950/40 dark:text-purple-400">
                    🏷️ Brand: {supplier.brand}
                  </Badge>
                )}
              </div>

              <div className="mt-1.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-zinc-600 dark:text-zinc-400">
                <div className="flex items-center gap-1 font-medium">
                  <User className="h-3.5 w-3.5 text-zinc-400" />
                  <span>Contact: <strong>{supplier.contactPerson || "Owner / Desk"}</strong></span>
                </div>

                <div className="flex items-center gap-1 font-medium">
                  <MapPin className="h-3.5 w-3.5 text-zinc-400" />
                  <span>Market: <strong>{supplier.marketName || supplier.marketArea || "Ahmedabad Cluster"}</strong></span>
                </div>

                {supplier.phone && (
                  <div className="flex items-center gap-1">
                    <Phone className="h-3.5 w-3.5 text-zinc-400" />
                    <a href={`tel:${supplier.phone}`} className="hover:underline font-mono">
                      {supplier.phone}
                    </a>
                  </div>
                )}

                {supplier.gstin && (
                  <div className="flex items-center gap-1 font-mono text-[11px] bg-zinc-100 dark:bg-zinc-800 px-1.5 py-0.5 rounded text-zinc-700 dark:text-zinc-300">
                    GSTIN: <strong>{supplier.gstin}</strong>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Manufacturing Speciality pill */}
          <div className="flex items-center gap-3 rounded-xl border border-zinc-200/70 bg-white/80 p-3 dark:border-zinc-800 dark:bg-zinc-900/80 shadow-xs">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-50 text-amber-600 dark:bg-amber-950/50">
              <Factory className="h-4 w-4" />
            </div>
            <div className="text-xs">
              <div className="text-muted-foreground font-medium">Manufacturing Scope</div>
              <div className="font-bold text-zinc-900 dark:text-zinc-100 max-w-[200px] truncate">
                {supplier.productsMade || supplier.categories || "All Textiles"}
              </div>
              <div className="text-[11px] text-zinc-500 font-medium mt-0.5">
                Price: {supplier.priceRange || "On Inquiry"}
              </div>
            </div>
          </div>
        </div>
      </Card>

      {/* Key Metric Statistics Cards (CRM/ERP Style) */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-6">
        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">Total Orders</span>
            <Receipt className="h-4 w-4 text-blue-600" />
          </div>
          <p className="mt-1.5 text-xl font-black text-zinc-900 dark:text-zinc-100">{allSupplierEntries.length}</p>
          <span className="text-[10px] text-zinc-500 mt-0.5 block">{uniqueBuyersList.length} unique buyers</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">Total Volume</span>
            <Package className="h-4 w-4 text-indigo-600" />
          </div>
          <p className="mt-1.5 text-xl font-black text-zinc-900 dark:text-zinc-100">
            {totalVolumePieces.toLocaleString("en-IN")} <span className="text-xs font-normal text-zinc-500">pcs</span>
          </p>
          <span className="text-[10px] text-zinc-500 mt-0.5 block">Total supplied goods</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">Packaging</span>
            <Layers className="h-4 w-4 text-amber-600" />
          </div>
          <p className="mt-1.5 text-xl font-black text-zinc-900 dark:text-zinc-100">
            {totalCasesCount} <span className="text-xs font-normal text-zinc-500">cases</span>
          </p>
          <span className="text-[10px] text-zinc-500 mt-0.5 block">{totalLoosePieces} loose pcs</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">Procurement ₹</span>
            <IndianRupee className="h-4 w-4 text-emerald-600" />
          </div>
          <p className="mt-1.5 text-xl font-black text-zinc-900 dark:text-zinc-100">
            {formatInr(totalBilledAmount)}
          </p>
          <span className="text-[10px] text-emerald-600 font-medium mt-0.5 block">Total turnover</span>
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
            {pendingEntries.reduce((s, e) => s + (Number(e.pieces) || 0), 0).toLocaleString("en-IN")} pcs in pipeline
          </span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-zinc-500 uppercase tracking-wider">Delivered</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
          </div>
          <p className="mt-1.5 text-xl font-black text-emerald-600 dark:text-emerald-400">
            {deliveredEntries.length} <span className="text-xs font-normal text-zinc-500">orders</span>
          </p>
          <span className="text-[10px] text-zinc-500 mt-0.5 block">100% completed</span>
        </Card>
      </div>

      {/* Main Tabbed Navigation */}
      <Tabs
        options={[
          { value: "orders", label: `Supply Orders (${allSupplierEntries.length})` },
          { value: "profile", label: "Mill Profile & Factories" },
          { value: "media", label: `Verification Media (${verificationDocs.filter((d) => d.uri).length}/2)` },
        ]}
        value={activeTab}
        onValueChange={(val) => setActiveTab(val as "orders" | "profile" | "media")}
      />

      {/* TAB 1: SUPPLY ORDERS & PROCUREMENT HISTORY */}
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
                    placeholder="Search by order #, item code, buyer customer..."
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

              {/* Row 2: Secondary Filters */}
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

                {/* Buyer Customer Filter */}
                {uniqueBuyersList.length > 0 && (
                  <div className="flex items-center gap-1">
                    <span className="text-[11px] font-semibold text-zinc-500 mr-1">Buyer:</span>
                    <select
                      value={customerFilter}
                      onChange={(e) => setCustomerFilter(e.target.value)}
                      className="h-7 text-xs rounded-lg border border-zinc-200 bg-white px-2 dark:border-zinc-800 dark:bg-zinc-900 text-zinc-700 dark:text-zinc-300 font-medium max-w-[180px] truncate"
                    >
                      <option value="all">All Buyer Retailers</option>
                      {uniqueBuyersList.map(([id, name]) => (
                        <option key={id} value={id}>
                          {name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Reset Filters */}
                {(searchQuery || timeframe !== "all" || statusFilter !== "all" || customerFilter !== "all") && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setSearchQuery("")
                      setTimeframe("all")
                      setStatusFilter("all")
                      setCustomerFilter("all")
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
                  Procurement & Supply Lines
                </span>
                <Badge variant="outline" className="text-[10px] font-semibold">
                  {filteredEntries.length} of {allSupplierEntries.length} orders
                </Badge>
              </div>
              <span className="text-xs text-muted-foreground">
                Total Shown: <strong>{filteredEntries.reduce((s, e) => s + (Number(e.pieces) || 0), 0).toLocaleString("en-IN")} pcs</strong>
              </span>
            </div>

            {filteredEntries.length === 0 ? (
              <div className="py-12 text-center text-xs text-muted-foreground">
                <Package className="h-8 w-8 text-zinc-300 mx-auto mb-2 dark:text-zinc-700" />
                <p className="font-semibold text-zinc-800 dark:text-zinc-200">No supply orders match the selected filters</p>
                <p className="text-[11px] text-zinc-500 mt-0.5">Try changing the date filter or clearing search terms.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-zinc-100 bg-zinc-50/75 dark:border-zinc-800 dark:bg-zinc-900/50 font-semibold text-zinc-600 dark:text-zinc-400">
                      <th className="py-2.5 px-3">Date</th>
                      <th className="py-2.5 px-3">Order / Visit</th>
                      <th className="py-2.5 px-3">Buyer Customer (Retailer)</th>
                      <th className="py-2.5 px-3">Design / Item Code</th>
                      <th className="py-2.5 px-3 text-right">Quantity</th>
                      <th className="py-2.5 px-3 text-right">Packaging</th>
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
                            <span className="font-mono font-bold text-indigo-700 dark:text-indigo-400">
                              {entry.orderNo || visit?.visitCode || `#${entry.id}`}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 font-medium text-zinc-900 dark:text-zinc-100">
                            {visit?.customerName || "Customer"}
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

      {/* TAB 2: MILL PROFILE & FACTORIES */}
      {activeTab === "profile" && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {/* Mill Identity Card */}
            <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950 space-y-4">
              <div className="flex items-center gap-2 pb-2 border-b border-zinc-100 dark:border-zinc-800">
                <Store className="h-4 w-4 text-amber-600" />
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">Mill & Identity Profile</h3>
              </div>

              <div className="grid grid-cols-2 gap-4 text-xs">
                <div>
                  <span className="text-muted-foreground block text-[11px]">Firm / Mill Name</span>
                  <span className="font-bold text-zinc-900 dark:text-zinc-100">{supplier.firmName || supplier.name}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Key Contact Person</span>
                  <span className="font-bold text-zinc-900 dark:text-zinc-100">{supplier.contactPerson || supplier.name}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Supplier ID</span>
                  <span className="font-mono font-bold text-indigo-600">{supplier.supplierId || `SUP-${supplier.id}`}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Supplier Nature</span>
                  <Badge variant="outline" className="font-semibold text-[11px] mt-0.5">
                    {supplier.type || "Manufacturer"}
                  </Badge>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Associated Brand</span>
                  <span className="font-semibold text-purple-700 dark:text-purple-400">{supplier.brand || "Unbranded / Independent"}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Textile Market Cluster</span>
                  <span className="font-semibold text-zinc-800 dark:text-zinc-200">{supplier.marketName || supplier.marketArea || "Ahmedabad"}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">GSTIN Number</span>
                  <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">{supplier.gstin || "Unregistered"}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">PAN Number</span>
                  <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">{supplier.panNumber || "—"}</span>
                </div>
              </div>

              {/* What They Make / Price Range */}
              {(supplier.productsMade || supplier.priceRange) && (
                <div className="pt-2 space-y-2 border-t border-zinc-100 dark:border-zinc-800">
                  {supplier.productsMade && (
                    <div>
                      <span className="text-[11px] text-muted-foreground font-medium block">
                        Manufacturing Profile & Fabrics Made:
                      </span>
                      <p className="text-xs text-zinc-800 dark:text-zinc-200 font-medium mt-0.5">
                        {supplier.productsMade}
                      </p>
                    </div>
                  )}
                  {supplier.priceRange && (
                    <div>
                      <span className="text-[11px] text-muted-foreground font-medium block">
                        Product Price Range:
                      </span>
                      <span className="text-xs font-bold text-emerald-600">
                        {supplier.priceRange}
                      </span>
                    </div>
                  )}
                </div>
              )}
            </Card>

            {/* Packaging & Logistics Parameters */}
            <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950 space-y-4">
              <div className="flex items-center gap-2 pb-2 border-b border-zinc-100 dark:border-zinc-800">
                <Truck className="h-4 w-4 text-indigo-600" />
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">Logistics & Trade Settings</h3>
              </div>

              <div className="grid grid-cols-2 gap-4 text-xs">
                <div>
                  <span className="text-muted-foreground block text-[11px]">Default Case Pack</span>
                  <span className="font-bold text-zinc-900 dark:text-zinc-100">{supplier.defaultCaseSize || 24} pcs / case</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Referred By</span>
                  <span className="font-semibold text-zinc-800 dark:text-zinc-200">{supplier.referredBy || "Direct Mill Connection"}</span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">Primary Office Address</span>
                  <span className="text-zinc-700 dark:text-zinc-300">
                    {supplier.officeAddress || supplier.address || "Address not specified"}
                  </span>
                </div>
                <div>
                  <span className="text-muted-foreground block text-[11px]">City / Location</span>
                  <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {supplier.city || "Ahmedabad"}
                  </span>
                </div>
              </div>

              {supplier.notes && (
                <div className="pt-2">
                  <span className="text-[11px] text-muted-foreground font-medium block mb-1">
                    Special CRM & Production Notes:
                  </span>
                  <p className="text-xs text-zinc-700 dark:text-zinc-300 bg-zinc-50 dark:bg-zinc-900 p-2.5 rounded-lg border border-zinc-200/60 dark:border-zinc-800 italic">
                    "{supplier.notes}"
                  </p>
                </div>
              )}
            </Card>
          </div>

          {/* Contact Lines (Up to 5) */}
          <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-100 dark:border-zinc-800">
              <div className="flex items-center gap-2">
                <Phone className="h-4 w-4 text-blue-600" />
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">
                  Direct Contact Lines & Desks ({contactsList.length})
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
                      {contact.designation || "Factory Desk"}
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

          {/* Factories & Production Units (Up to 5) */}
          <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-100 dark:border-zinc-800">
              <div className="flex items-center gap-2">
                <Factory className="h-4 w-4 text-amber-600" />
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">
                  Factories & Production Units ({factoriesList.length})
                </h3>
              </div>
              <span className="text-xs text-zinc-500">Up to 5 manufacturing facilities</span>
            </div>

            {factoriesList.length === 0 ? (
              <p className="py-4 text-xs text-muted-foreground italic">No separate factory units recorded.</p>
            ) : (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 pt-4">
                {factoriesList.map((unit, idx) => (
                  <div
                    key={idx}
                    className="rounded-xl border border-zinc-200/80 bg-zinc-50/50 p-3.5 dark:border-zinc-800 dark:bg-zinc-900/50 space-y-2"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                        {unit.name || `Factory Unit #${idx + 1}`}
                      </span>
                      <span className="text-[11px] font-medium text-zinc-500">
                        {unit.city || supplier.city || "Ahmedabad"}
                      </span>
                    </div>
                    <p className="text-xs text-zinc-700 dark:text-zinc-300 line-clamp-2">
                      {unit.address || "Address not specified"}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </Card>

          {/* Showrooms & Sales Gaddi (Up to 5) */}
          {showroomsList.length > 0 && (
            <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
              <div className="flex items-center justify-between pb-3 border-b border-zinc-100 dark:border-zinc-800">
                <div className="flex items-center gap-2">
                  <Store className="h-4 w-4 text-indigo-600" />
                  <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">
                    Showrooms & Sales Gaddi Outlets ({showroomsList.length})
                  </h3>
                </div>
                <span className="text-xs text-zinc-500">Trade counters and showrooms</span>
              </div>

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 pt-4">
                {showroomsList.map((sr, idx) => (
                  <div
                    key={idx}
                    className="rounded-xl border border-zinc-200/80 bg-zinc-50/50 p-3.5 dark:border-zinc-800 dark:bg-zinc-900/50 space-y-2"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                        {sr.name || `Showroom #${idx + 1}`}
                      </span>
                      <span className="text-[11px] font-medium text-zinc-500">
                        {sr.city || supplier.city || "Ahmedabad"}
                      </span>
                    </div>
                    <p className="text-xs text-zinc-700 dark:text-zinc-300 line-clamp-2">
                      {sr.address || "Address not specified"}
                    </p>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </div>
      )}

      {/* TAB 3: VERIFICATION MEDIA & CLOUD STORAGE */}
      {activeTab === "media" && (
        <div className="space-y-4">
          <Card className="p-4 border border-zinc-200/80 dark:border-zinc-800 shadow-xs bg-white dark:bg-zinc-950">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
                  <ShieldCheck className="h-4 w-4 text-emerald-600" />
                  <span>Cloud Verification Photos & Media</span>
                </h3>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Stored securely in Firebase Object Storage (<code>gs://himatsms.firebasestorage.app</code>). Click any photo to view full resolution.
                </p>
              </div>
              <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-200 text-xs font-bold">
                {verificationDocs.filter((d) => d.uri).length} / 2 Attached
              </Badge>
            </div>
          </Card>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 max-w-2xl">
            {verificationDocs.map((doc) => {
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
                            {doc.uri?.split("/").pop()?.split("?")[0] || "photo.jpg"}
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
                        <p className="text-xs font-medium text-zinc-500">No verification photo uploaded</p>
                        <p className="text-[10px] text-zinc-400 mt-0.5">
                          Edit this supplier to attach {doc.label.toLowerCase()}
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
