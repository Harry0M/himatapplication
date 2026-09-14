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
  ChevronLeft,
  ChevronRight,
  Filter,
  DollarSign,
  Download,
  Building2,
  Sparkles,
  ExternalLink,
  Pause,
  Play,
  AlertTriangle,
  UserX,
  RotateCcw,
  ShieldAlert,
  Printer,
  FileText,
} from "lucide-react"
import { useData } from "../context/DataContext"
import { useAuth } from "../context/AuthContext"
import { formatInr, formatDate, cn } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Tabs } from "../components/ui/Tabs"
import { Employee, Visit, PurchaseEntry, Supplier } from "../types"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateCustomerDayReportHtml,
  buildCustomerReportWhatsAppText,
  generateSupplierInvoiceHtml,
  buildSupplierInvoiceWhatsAppText,
} from "../lib/pdfReports"

interface StaffDetailViewProps {
  employeeId: number
  onBack: () => void
  onEdit?: (employee: Employee) => void
  onNavigate?: (tab: string) => void
}

type TimeframeMode = "all" | "daily" | "monthly" | "yearly" | "custom"

const MONTH_NAMES = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December"
]

// Parse date string or timestamp safely into midnight timestamp (local)
function parseDateToMidnight(val?: string | number): number {
  if (!val) return 0
  if (typeof val === "number") {
    const d = new Date(val)
    return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  }
  const clean = val.trim()
  if (!clean) return 0

  // Check YYYY-MM-DD
  const parts = clean.split(/[-/]/)
  if (parts.length === 3) {
    if (parts[0].length === 4) {
      return new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2])).getTime()
    }
    if (parts[2].length === 4) {
      return new Date(Number(parts[2]), Number(parts[1]) - 1, Number(parts[0])).getTime()
    }
  }

  const parsed = Date.parse(clean)
  if (!isNaN(parsed)) {
    const d = new Date(parsed)
    return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  }
  return 0
}

function getTodayString(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, "0")
  const day = String(d.getDate()).padStart(2, "0")
  return `${y}-${m}-${day}`
}

export function StaffDetailView({
  employeeId,
  onBack,
  onEdit,
  onNavigate
}: StaffDetailViewProps) {
  const {
    employees,
    visits,
    entries,
    customers,
    suppliers,
    packGroups,
    updatePayment,
    updateDelivery,
    suspendEmployee,
    resumeEmployee,
    deactivateEmployee,
    selectedEmployeeId,
    setSelectedEmployeeId
  } = useData()
  const { isAdmin } = useAuth()

  // Find targeted employee
  const employee = useMemo(() => {
    return employees.find((e) => e.id === employeeId) || null
  }, [employees, employeeId])

  // Customer map for rapid lookup
  const customerMap = useMemo(() => {
    return new Map(customers.map((c) => [c.id, c]))
  }, [customers])

  // Visit map for fast parent resolution
  const visitMap = useMemo(() => {
    return new Map(visits.map((v) => [v.id, v]))
  }, [visits])

  // All visits belonging to this staff member
  const allStaffVisits = useMemo(() => {
    if (!employee) return []
    const empNameLower = employee.name.trim().toLowerCase()
    return visits.filter(
      (v) =>
        Number(v.employeeId) === employee.id ||
        (v.employeeName && v.employeeName.trim().toLowerCase() === empNameLower)
    )
  }, [visits, employee])

  // Set of visit IDs
  const staffVisitIdsSet = useMemo(() => {
    return new Set(allStaffVisits.map((v) => Number(v.id)))
  }, [allStaffVisits])

  // All purchase entries belonging to this staff member's visits
  const allStaffEntries = useMemo(() => {
    return entries.filter((e) => staffVisitIdsSet.has(Number(e.visitId)))
  }, [entries, staffVisitIdsSet])

  // All-time sales volume for banners and analytics
  const allTimeSalesVolume = useMemo(() => {
    return allStaffEntries.reduce((sum, e) => {
      return sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) || 0) + (Number(e.gstAmount) || 0))
    }, 0)
  }, [allStaffEntries])

  // Time-frame filtering state
  const [timeframeMode, setTimeframeMode] = useState<TimeframeMode>("all")
  const [selectedDailyDate, setSelectedDailyDate] = useState<string>(getTodayString())
  const [selectedMonth, setSelectedMonth] = useState<number>(new Date().getMonth())
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear())
  const [customStartDate, setCustomStartDate] = useState<string>(getTodayString())
  const [customEndDate, setCustomEndDate] = useState<string>(getTodayString())

  // Tab state
  const [activeTab, setActiveTab] = useState<string>("orders")

  // Search & secondary filters inside tabs
  const [ordersSearch, setOrdersSearch] = useState<string>("")
  const [ordersPaymentFilter, setOrdersPaymentFilter] = useState<string>("all")
  const [ordersDeliveryFilter, setOrdersDeliveryFilter] = useState<string>("all")
  const [visitsStatusFilter, setVisitsStatusFilter] = useState<string>("all")

  // Payment update modal
  const [paymentEntry, setPaymentEntry] = useState<PurchaseEntry | null>(null)
  const [paymentAmount, setPaymentAmount] = useState<string>("")
  const [paymentMode, setPaymentMode] = useState<string>("Cash")
  const [paymentStatus, setPaymentStatus] = useState<string>("Paid")
  const [paymentRemarks, setPaymentRemarks] = useState<string>("")

  // Delivery update modal
  const [deliveryEntry, setDeliveryEntry] = useState<PurchaseEntry | null>(null)
  const [deliveryStatus, setDeliveryStatus] = useState<string>("Dispatched")
  const [transporter, setTransporter] = useState<string>("")
  const [lrNo, setLrNo] = useState<string>("")
  const [lrDate, setLrDate] = useState<string>("")

  // Access Control States & Handlers
  const [showSuspendDialog, setShowSuspendDialog] = useState(false)
  const [suspendReason, setSuspendReason] = useState("Temporary suspension by Administrator")
  const [showDeactivateDialog, setShowDeactivateDialog] = useState(false)
  const [deactivateReason, setDeactivateReason] = useState("Staff deactivated by Administrator")
  const [isProcessingAction, setIsProcessingAction] = useState(false)

  const isDeactivated = Boolean(employee?.isDeleted || employee?.status === "Deactivated")
  const isSuspended = Boolean(!isDeactivated && (employee?.isBlocked || employee?.status === "Suspended"))
  const isActive = Boolean(employee && !isDeactivated && !isSuspended)

  const handleSuspend = async () => {
    if (!employee) return
    setIsProcessingAction(true)
    try {
      await suspendEmployee(employee.id, suspendReason.trim() || "Suspended by Administrator")
      setShowSuspendDialog(false)
    } finally {
      setIsProcessingAction(false)
    }
  }

  const handleResume = async () => {
    if (!employee) return
    setIsProcessingAction(true)
    try {
      await resumeEmployee(employee.id)
    } finally {
      setIsProcessingAction(false)
    }
  }

  const handleDeactivate = async () => {
    if (!employee) return
    setIsProcessingAction(true)
    try {
      await deactivateEmployee(employee.id, deactivateReason.trim() || "Deactivated by Administrator")
      setShowDeactivateDialog(false)
    } finally {
      setIsProcessingAction(false)
    }
  }

  // Resolve entry timestamp (from entry or parent visit)
  const getEntryDateTimestamp = (entry: PurchaseEntry): number => {
    if (entry.createdAt) {
      return parseDateToMidnight(entry.createdAt)
    }
    const parentVisit = visitMap.get(Number(entry.visitId))
    if (parentVisit && parentVisit.date) {
      return parseDateToMidnight(parentVisit.date)
    }
    return 0
  }

  // Filter test function
  const isDateInSelectedTimeframe = (timestamp: number): boolean => {
    if (timeframeMode === "all") return true
    if (!timestamp) return false

    const itemDate = new Date(timestamp)
    const itemYear = itemDate.getFullYear()
    const itemMonth = itemDate.getMonth()
    const itemDayMidnight = new Date(itemYear, itemMonth, itemDate.getDate()).getTime()

    if (timeframeMode === "daily") {
      const targetMidnight = parseDateToMidnight(selectedDailyDate)
      return itemDayMidnight === targetMidnight
    }

    if (timeframeMode === "monthly") {
      return itemYear === selectedYear && itemMonth === selectedMonth
    }

    if (timeframeMode === "yearly") {
      return itemYear === selectedYear
    }

    if (timeframeMode === "custom") {
      const startMidnight = parseDateToMidnight(customStartDate)
      const endMidnight = parseDateToMidnight(customEndDate)
      if (startMidnight && endMidnight) {
        return itemDayMidnight >= startMidnight && itemDayMidnight <= endMidnight
      }
      if (startMidnight) return itemDayMidnight >= startMidnight
      if (endMidnight) return itemDayMidnight <= endMidnight
      return true
    }

    return true
  }

  // Filtered Visits & Entries
  const filteredVisits = useMemo(() => {
    return allStaffVisits.filter((v) => {
      const ts = parseDateToMidnight(v.date || v.createdAt)
      return isDateInSelectedTimeframe(ts)
    })
  }, [allStaffVisits, timeframeMode, selectedDailyDate, selectedMonth, selectedYear, customStartDate, customEndDate])

  const filteredEntries = useMemo(() => {
    return allStaffEntries.filter((e) => {
      const ts = getEntryDateTimestamp(e)
      return isDateInSelectedTimeframe(ts)
    })
  }, [allStaffEntries, timeframeMode, selectedDailyDate, selectedMonth, selectedYear, customStartDate, customEndDate, visitMap])

  // Real-time Metrics for Filtered Period
  const periodMetrics = useMemo(() => {
    const totalSales = filteredEntries.reduce((sum, e) => {
      return sum + (Number(e.totalAmount) || 0) + (Number(e.gstAmount) || 0)
    }, 0)

    const totalPaid = filteredEntries.reduce((sum, e) => sum + (Number(e.paidAmount) || 0), 0)
    const totalDues = Math.max(0, totalSales - totalPaid)
    const totalOrders = filteredEntries.length
    const totalPieces = filteredEntries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
    const totalCases = filteredEntries.reduce((sum, e) => sum + (Number(e.caseCount) || 0), 0)

    const pendingDeliveries = filteredEntries.filter(
      (e) => e.deliveryStatus?.toLowerCase() !== "delivered"
    ).length
    const dispatched = filteredEntries.filter(
      (e) => e.deliveryStatus?.toLowerCase() === "dispatched"
    ).length
    const delivered = filteredEntries.filter(
      (e) => e.deliveryStatus?.toLowerCase() === "delivered"
    ).length

    const activeVisits = filteredVisits.filter((v) => v.status?.toLowerCase() === "active").length
    const completedVisits = filteredVisits.filter((v) => v.status?.toLowerCase() === "completed").length

    return {
      totalSales,
      totalPaid,
      totalDues,
      totalOrders,
      totalPieces,
      totalCases,
      pendingDeliveries,
      dispatched,
      delivered,
      totalVisits: filteredVisits.length,
      activeVisits,
      completedVisits
    }
  }, [filteredEntries, filteredVisits])

  // Payment Handlers
  const handleOpenPayment = (entry: PurchaseEntry) => {
    const totalBill = (Number(entry.totalAmount) || 0) + (Number(entry.gstAmount) || 0)
    const due = Math.max(0, totalBill - (Number(entry.paidAmount) || 0))
    setPaymentEntry(entry)
    setPaymentAmount(due > 0 ? due.toString() : "0")
    setPaymentMode(entry.paymentMode || "Cash")
    setPaymentStatus(due > 0 ? "Paid" : "Paid")
    setPaymentRemarks("")
  }

  const handleSavePayment = async () => {
    if (!paymentEntry) return
    const paid = Number(paymentAmount) || 0
    await updatePayment(paymentEntry.id, paymentStatus, paymentMode, paid, paymentRemarks)
    setPaymentEntry(null)
  }

  // Delivery Handlers
  const handleOpenDelivery = (entry: PurchaseEntry) => {
    setDeliveryEntry(entry)
    setDeliveryStatus(entry.deliveryStatus || "Dispatched")
    setTransporter(entry.transporter || "")
    setLrNo(entry.lrNo || "")
    setLrDate(entry.lrDate || getTodayString())
  }

  const handleSaveDelivery = async () => {
    if (!deliveryEntry) return
    await updateDelivery(deliveryEntry.id, deliveryStatus, transporter, lrNo, lrDate)
    setDeliveryEntry(null)
  }

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

  const handleOpenInvoice = (entry: PurchaseEntry) => {
    const parentVisit = visitMap.get(Number(entry.visitId)) || {
      id: entry.visitId,
      visitCode: `VIS-${entry.visitId}`,
      customerId: 0,
      customerName: "Buyer",
      date: entry.createdAt
        ? new Date(entry.createdAt).toISOString().split("T")[0]
        : new Date().toISOString().split("T")[0],
      employeeId: employee?.id || 1,
      employeeName: employee?.name || "Agent",
      status: "Completed",
    }
    const customer = parentVisit ? customerMap.get(Number(parentVisit.customerId)) : null
    const supplier: Supplier = (suppliers &&
      suppliers.find(
        (s) =>
          s.id === entry.supplierId ||
          s.name.toLowerCase() === entry.supplierName?.toLowerCase()
      )) || {
      id: entry.supplierId || 1,
      name: entry.supplierName,
      type: entry.supplierType || "Wholesaler",
      phone: "—",
      marketArea: "Wholesale Market",
    }
    const invoiceData = {
      supplier,
      visit: parentVisit,
      customer,
      salesman: employee,
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

  const handleOpenCustomerReport = (visit: Visit) => {
    const bookedEntries = entries.filter((e) => Number(e.visitId) === visit.id)
    const customer = customerMap.get(Number(visit.customerId)) || null
    const linkedPackGroups = packGroups
      ? packGroups.filter((pg) => pg.visitId === visit.id)
      : []
    const reportData = {
      visit,
      customer,
      salesman: employee,
      entries: bookedEntries,
      packGroups: linkedPackGroups,
    }
    const html = generateCustomerDayReportHtml(reportData)
    const whatsAppText = buildCustomerReportWhatsAppText(reportData)
    setReportModal({
      open: true,
      title: `Customer Day Report: ${visit.customerName} (${visit.visitCode})`,
      html,
      whatsAppText,
    })
  }

  // Quick Date Navigation for Daily
  const shiftDailyDate = (days: number) => {
    const current = new Date(selectedDailyDate || getTodayString())
    current.setDate(current.getDate() + days)
    const y = current.getFullYear()
    const m = String(current.getMonth() + 1).padStart(2, "0")
    const d = String(current.getDate()).padStart(2, "0")
    setSelectedDailyDate(`${y}-${m}-${d}`)
  }

  // Quick Month Navigation
  const shiftMonth = (months: number) => {
    let newM = selectedMonth + months
    let newY = selectedYear
    if (newM > 11) {
      newM = 0
      newY += 1
    } else if (newM < 0) {
      newM = 11
      newY -= 1
    }
    setSelectedMonth(newM)
    setSelectedYear(newY)
  }

  // Filtered Orders for the Tab
  const q = ordersSearch.trim().toLowerCase()
  const displayOrders = useMemo(() => {
    return filteredEntries.filter((e) => {
      const parentVisit = visitMap.get(Number(e.visitId))
      const custName = parentVisit?.customerName || ""
      const matchesSearch =
        !q ||
        e.orderNo?.toLowerCase().includes(q) ||
        e.itemCode?.toLowerCase().includes(q) ||
        e.supplierName?.toLowerCase().includes(q) ||
        custName.toLowerCase().includes(q)

      const matchesPayment =
        ordersPaymentFilter === "all" ||
        (ordersPaymentFilter === "paid" && e.paymentStatus?.toLowerCase() === "paid") ||
        (ordersPaymentFilter === "partial" && e.paymentStatus?.toLowerCase() === "partial") ||
        (ordersPaymentFilter === "unpaid" &&
          (e.paymentStatus?.toLowerCase() === "unpaid" || !e.paymentStatus))

      const matchesDelivery =
        ordersDeliveryFilter === "all" ||
        (ordersDeliveryFilter === "delivered" && e.deliveryStatus?.toLowerCase() === "delivered") ||
        (ordersDeliveryFilter === "dispatched" && e.deliveryStatus?.toLowerCase() === "dispatched") ||
        (ordersDeliveryFilter === "pending" &&
          (e.deliveryStatus?.toLowerCase() === "pending" || !e.deliveryStatus))

      return matchesSearch && matchesPayment && matchesDelivery
    })
  }, [filteredEntries, q, ordersPaymentFilter, ordersDeliveryFilter, visitMap])

  // Filtered Visits for the Tab
  const displayVisits = useMemo(() => {
    return filteredVisits.filter((v) => {
      const matchesSearch =
        !q ||
        v.customerName?.toLowerCase().includes(q) ||
        v.visitCode?.toLowerCase().includes(q) ||
        v.notes?.toLowerCase().includes(q)

      const matchesStatus =
        visitsStatusFilter === "all" ||
        v.status?.toLowerCase() === visitsStatusFilter.toLowerCase()

      return matchesSearch && matchesStatus
    })
  }, [filteredVisits, q, visitsStatusFilter])

  // Pending Payments list
  const pendingPaymentsList = useMemo(() => {
    return filteredEntries.filter(
      (e) =>
        e.paymentStatus?.toLowerCase() !== "paid" &&
        e.paymentStatus?.toLowerCase() !== "received"
    )
  }, [filteredEntries])

  // If employee not found
  if (!employee) {
    return (
      <div className="p-8 text-center space-y-4">
        <AlertCircle className="h-12 w-12 text-red-500 mx-auto" />
        <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">
          Staff Profile Not Found
        </h2>
        <p className="text-xs text-muted-foreground">
          The requested staff record could not be loaded or was removed.
        </p>
        <Button onClick={onBack} variant="outline" size="sm">
          <ArrowLeft className="h-4 w-4 mr-1.5" /> Back to Staff Directory
        </Button>
      </div>
    )
  }

  const isGloballyFiltered = selectedEmployeeId === employee.id
  const extraPhones = [employee.phone2, employee.phone3, employee.phone4, employee.phone5].filter(Boolean)
  const assignedMarketList = employee.assignedMarkets
    ? employee.assignedMarkets.split(",").map((m) => m.trim()).filter(Boolean)
    : []

  return (
    <div className="space-y-6 pb-16">
      {/* 1. TOP STICKY HEADER & PROFILE SUMMARY */}
      <div className="rounded-2xl border border-zinc-200 bg-white p-5 dark:border-zinc-800 dark:bg-zinc-950 shadow-sm">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          {/* Back button & Staff ID Info */}
          <div className="flex items-start gap-4">
            <Button
              variant="outline"
              size="sm"
              shape="pill"
              onClick={onBack}
              className="h-9 gap-1.5 font-semibold text-xs shrink-0"
              title="Return to Staff Directory"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>Back</span>
            </Button>

            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-tr from-zinc-900 to-zinc-700 text-lg font-bold text-white dark:from-zinc-100 dark:to-zinc-300 dark:text-zinc-900 shadow-md">
                {employee.name.charAt(0).toUpperCase()}
              </div>
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <h1 className="text-xl font-extrabold text-zinc-900 dark:text-zinc-50 tracking-tight">
                    {employee.name}
                  </h1>
                  <Badge
                    variant={
                      employee.role === "Admin"
                        ? "default"
                        : "secondary"
                    }
                  >
                    {employee.role}
                  </Badge>

                  {/* Access Status Badge */}
                  {isDeactivated ? (
                    <Badge variant="outline" className="bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-950/40 dark:text-rose-400 dark:border-rose-800 font-semibold flex items-center gap-1">
                      ✕ Deactivated
                    </Badge>
                  ) : isSuspended ? (
                    <Badge variant="outline" className="bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/40 dark:text-amber-400 dark:border-amber-800 font-semibold flex items-center gap-1">
                      ⏸ Access Suspended
                    </Badge>
                  ) : (
                    <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-400 dark:border-emerald-800 font-semibold flex items-center gap-1">
                      ● Active Access
                    </Badge>
                  )}

                  {periodMetrics.activeVisits > 0 && (
                    <Badge variant="success" className="animate-pulse">
                      ● In Field ({periodMetrics.activeVisits} Active Trip)
                    </Badge>
                  )}
                </div>

                <div className="flex items-center gap-2 mt-1 text-xs text-muted-foreground flex-wrap">
                  <span className="font-mono font-medium text-zinc-700 dark:text-zinc-300">
                    {employee.employeeId || `EMP-${employee.id}`}
                  </span>
                  {employee.phone && (
                    <>
                      <span>•</span>
                      <a
                        href={`tel:${employee.phone}`}
                        className="flex items-center gap-1 text-blue-600 dark:text-blue-400 hover:underline"
                      >
                        <Phone className="h-3 w-3" />
                        {employee.phone}
                      </a>
                    </>
                  )}
                  {employee.email && (
                    <>
                      <span>•</span>
                      <a
                        href={`mailto:${employee.email}`}
                        className="flex items-center gap-1 text-zinc-600 dark:text-zinc-400 hover:underline"
                      >
                        <Mail className="h-3 w-3" />
                        {employee.email}
                      </a>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-2 shrink-0 flex-wrap">
            {isAdmin && (
              <>
                {isActive && (
                  <Button
                    variant="outline"
                    size="sm"
                    shape="pill"
                    disabled={isProcessingAction}
                    onClick={() => {
                      setSuspendReason("Temporary suspension by Administrator")
                      setShowSuspendDialog(true)
                    }}
                    className="text-xs h-8 font-semibold gap-1 text-amber-700 border-amber-300 hover:bg-amber-50 dark:text-amber-400 dark:border-amber-800 dark:hover:bg-amber-950/50"
                    title="Stop this salesman's Android app access immediately"
                  >
                    <Pause className="h-3.5 w-3.5" />
                    Stop Access
                  </Button>
                )}

                {isSuspended && (
                  <Button
                    size="sm"
                    shape="pill"
                    disabled={isProcessingAction}
                    onClick={handleResume}
                    className="text-xs h-8 font-semibold gap-1 bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm"
                    title="Instantly restore salesman's Android app access"
                  >
                    <Play className="h-3.5 w-3.5" />
                    Resume Access
                  </Button>
                )}

                {isDeactivated ? (
                  <Button
                    size="sm"
                    shape="pill"
                    disabled={isProcessingAction}
                    onClick={handleResume}
                    className="text-xs h-8 font-semibold gap-1 bg-blue-600 hover:bg-blue-700 text-white shadow-sm"
                    title="Reactivate this staff member and restore access"
                  >
                    <RotateCcw className="h-3.5 w-3.5" />
                    Reactivate Staff
                  </Button>
                ) : (
                  <Button
                    variant="ghost"
                    size="sm"
                    shape="pill"
                    disabled={isProcessingAction}
                    onClick={() => {
                      setDeactivateReason("Staff deactivated by Administrator")
                      setShowDeactivateDialog(true)
                    }}
                    className="text-xs h-8 font-medium gap-1 text-zinc-500 hover:text-rose-600 dark:text-zinc-400 dark:hover:text-rose-400"
                    title="Soft-deactivate staff while keeping 100% of historical visits and orders intact"
                  >
                    <UserX className="h-3.5 w-3.5" />
                    Deactivate
                  </Button>
                )}
              </>
            )}

            {isAdmin && onEdit && (
              <Button
                variant="outline"
                size="sm"
                shape="pill"
                onClick={() => onEdit(employee)}
                className="text-xs h-8 font-medium gap-1"
              >
                <Edit2 className="h-3.5 w-3.5" />
                Edit Profile
              </Button>
            )}

            <Button
              variant={isGloballyFiltered ? "default" : "secondary"}
              size="sm"
              shape="pill"
              onClick={() => {
                if (isGloballyFiltered) {
                  setSelectedEmployeeId("all")
                } else {
                  setSelectedEmployeeId(employee.id)
                }
              }}
              className="text-xs h-8 font-semibold gap-1"
              title="Filter entire application work by this staff member"
            >
              <Filter className="h-3.5 w-3.5" />
              {isGloballyFiltered ? "App Filtered ✓" : "Filter Global Work"}
            </Button>
          </div>
        </div>

        {/* Assigned Textile Markets & Quick Bio Chips */}
        {assignedMarketList.length > 0 && (
          <div className="mt-4 pt-3 border-t border-zinc-100 dark:border-zinc-800/80 flex items-center gap-2 flex-wrap text-xs">
            <span className="text-muted-foreground flex items-center gap-1 font-medium">
              <MapPin className="h-3.5 w-3.5 text-zinc-400" />
              Assigned Territory ({assignedMarketList.length}):
            </span>
            {assignedMarketList.map((m) => (
              <span
                key={m}
                className="rounded-full bg-zinc-100 dark:bg-zinc-800/70 text-zinc-800 dark:text-zinc-200 px-2.5 py-0.5 text-[11px] font-medium"
              >
                {m}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Access Status Warning Banners */}
      {isSuspended && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50/90 p-4 dark:border-amber-900/50 dark:bg-amber-950/30 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-amber-950 dark:text-amber-100 shadow-sm animate-in fade-in">
          <div className="flex items-start gap-3">
            <div className="p-2 rounded-xl bg-amber-100 dark:bg-amber-900/60 text-amber-800 dark:text-amber-300 shrink-0">
              <Pause className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <p className="text-sm font-bold">Android App Access Stopped (Suspended)</p>
                <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-amber-200 text-amber-900 dark:bg-amber-800 dark:text-amber-100">
                  Real-time Locked
                </span>
              </div>
              <p className="text-xs text-amber-800/90 dark:text-amber-300/90 mt-0.5">
                This salesman is currently locked out of logging new trips or orders in the mobile app.
                <strong> 100% of historical data ({allStaffVisits.length} trips, {allStaffEntries.length} purchase entries, ₹{formatInr(allTimeSalesVolume)} sales) is safely preserved.</strong>
                {employee.blockedReason && (
                  <span className="block mt-1 font-medium text-amber-900 dark:text-amber-200">
                    Reason: "{employee.blockedReason}"
                  </span>
                )}
              </p>
            </div>
          </div>
          {isAdmin && (
            <Button
              size="sm"
              disabled={isProcessingAction}
              className="bg-emerald-600 hover:bg-emerald-700 text-white font-semibold h-8 text-xs shrink-0 gap-1.5 shadow-sm"
              onClick={handleResume}
            >
              <Play className="h-3.5 w-3.5" />
              Resume Access Now
            </Button>
          )}
        </div>
      )}

      {isDeactivated && (
        <div className="rounded-2xl border border-rose-200 bg-rose-50/90 p-4 dark:border-rose-900/50 dark:bg-rose-950/30 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-rose-950 dark:text-rose-100 shadow-sm animate-in fade-in">
          <div className="flex items-start gap-3">
            <div className="p-2 rounded-xl bg-rose-100 dark:bg-rose-900/60 text-rose-800 dark:text-rose-300 shrink-0">
              <UserX className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <p className="text-sm font-bold">Staff Account Deactivated</p>
                <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-rose-200 text-rose-900 dark:bg-rose-800 dark:text-rose-100">
                  Data Preserved
                </span>
              </div>
              <p className="text-xs text-rose-800/90 dark:text-rose-300/90 mt-0.5">
                This staff member has been soft-deactivated. All past customer relationships, visit records ({allStaffVisits.length}), orders ({allStaffEntries.length}), and total invoices (₹{formatInr(allTimeSalesVolume)}) remain permanently available for business audits.
                {employee.deletionReason && (
                  <span className="block mt-1 font-medium text-rose-900 dark:text-rose-200">
                    Reason: "{employee.deletionReason}"
                  </span>
                )}
              </p>
            </div>
          </div>
          {isAdmin && (
            <Button
              size="sm"
              disabled={isProcessingAction}
              className="bg-blue-600 hover:bg-blue-700 text-white font-semibold h-8 text-xs shrink-0 gap-1.5 shadow-sm"
              onClick={handleResume}
            >
              <RotateCcw className="h-3.5 w-3.5" />
              Reactivate Staff
            </Button>
          )}
        </div>
      )}

      {/* 2. TIMEFRAME & PERIOD FILTER CONTROLS */}
      <Card className="p-4 rounded-2xl border-zinc-200 dark:border-zinc-800 space-y-3 bg-zinc-50/50 dark:bg-zinc-900/30">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
          {/* Preset Buttons */}
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-xs font-semibold text-zinc-600 dark:text-zinc-400 mr-1 flex items-center gap-1">
              <Calendar className="h-3.5 w-3.5" /> Timeframe:
            </span>
            <button
              onClick={() => setTimeframeMode("all")}
              className={cn(
                "px-3 py-1 text-xs font-medium rounded-full transition-all",
                timeframeMode === "all"
                  ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-950 shadow-sm"
                  : "bg-white text-zinc-700 hover:bg-zinc-100 dark:bg-zinc-800 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700"
              )}
            >
              All Time
            </button>
            <button
              onClick={() => {
                setTimeframeMode("daily")
                setSelectedDailyDate(getTodayString())
              }}
              className={cn(
                "px-3 py-1 text-xs font-medium rounded-full transition-all",
                timeframeMode === "daily"
                  ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-950 shadow-sm"
                  : "bg-white text-zinc-700 hover:bg-zinc-100 dark:bg-zinc-800 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700"
              )}
            >
              Daily
            </button>
            <button
              onClick={() => setTimeframeMode("monthly")}
              className={cn(
                "px-3 py-1 text-xs font-medium rounded-full transition-all",
                timeframeMode === "monthly"
                  ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-950 shadow-sm"
                  : "bg-white text-zinc-700 hover:bg-zinc-100 dark:bg-zinc-800 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700"
              )}
            >
              Monthly
            </button>
            <button
              onClick={() => setTimeframeMode("yearly")}
              className={cn(
                "px-3 py-1 text-xs font-medium rounded-full transition-all",
                timeframeMode === "yearly"
                  ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-950 shadow-sm"
                  : "bg-white text-zinc-700 hover:bg-zinc-100 dark:bg-zinc-800 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700"
              )}
            >
              Yearly
            </button>
            <button
              onClick={() => setTimeframeMode("custom")}
              className={cn(
                "px-3 py-1 text-xs font-medium rounded-full transition-all",
                timeframeMode === "custom"
                  ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-950 shadow-sm"
                  : "bg-white text-zinc-700 hover:bg-zinc-100 dark:bg-zinc-800 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700"
              )}
            >
              Custom Range
            </button>
          </div>

          {/* Active Range Summary Pill */}
          <div className="text-xs text-muted-foreground flex items-center gap-1.5">
            <span className="font-medium">Filter Active:</span>
            <Badge variant="outline" className="font-mono text-[11px] bg-white dark:bg-zinc-900">
              {timeframeMode === "all" && "All Time Records"}
              {timeframeMode === "daily" && `Day: ${formatDate(selectedDailyDate)}`}
              {timeframeMode === "monthly" && `${MONTH_NAMES[selectedMonth]} ${selectedYear}`}
              {timeframeMode === "yearly" && `Year: ${selectedYear}`}
              {timeframeMode === "custom" && `${formatDate(customStartDate)} to ${formatDate(customEndDate)}`}
            </Badge>
          </div>
        </div>

        {/* Dynamic Controls depending on mode */}
        {timeframeMode === "daily" && (
          <div className="pt-2 border-t border-zinc-200/70 dark:border-zinc-800 flex items-center gap-3 flex-wrap">
            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="sm"
                onClick={() => shiftDailyDate(-1)}
                className="h-8 w-8 p-0"
                title="Previous Day"
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Input
                type="date"
                value={selectedDailyDate}
                onChange={(e) => setSelectedDailyDate(e.target.value)}
                className="w-40 h-8 text-xs font-medium"
              />
              <Button
                variant="outline"
                size="sm"
                onClick={() => shiftDailyDate(1)}
                className="h-8 w-8 p-0"
                title="Next Day"
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setSelectedDailyDate(getTodayString())}
              className="text-xs h-8 text-blue-600 dark:text-blue-400"
            >
              Jump to Today
            </Button>
          </div>
        )}

        {timeframeMode === "monthly" && (
          <div className="pt-2 border-t border-zinc-200/70 dark:border-zinc-800 flex items-center gap-3 flex-wrap">
            <Button
              variant="outline"
              size="sm"
              onClick={() => shiftMonth(-1)}
              className="h-8 w-8 p-0"
              title="Previous Month"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <select
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(Number(e.target.value))}
              className="h-8 px-3 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs font-medium"
            >
              {MONTH_NAMES.map((name, idx) => (
                <option key={name} value={idx}>
                  {name}
                </option>
              ))}
            </select>
            <select
              value={selectedYear}
              onChange={(e) => setSelectedYear(Number(e.target.value))}
              className="h-8 px-3 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs font-medium"
            >
              {[2024, 2025, 2026, 2027].map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
            <Button
              variant="outline"
              size="sm"
              onClick={() => shiftMonth(1)}
              className="h-8 w-8 p-0"
              title="Next Month"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setSelectedMonth(new Date().getMonth())
                setSelectedYear(new Date().getFullYear())
              }}
              className="text-xs h-8 text-blue-600 dark:text-blue-400"
            >
              This Month
            </Button>
          </div>
        )}

        {timeframeMode === "yearly" && (
          <div className="pt-2 border-t border-zinc-200/70 dark:border-zinc-800 flex items-center gap-3 flex-wrap">
            <span className="text-xs text-muted-foreground font-medium">Select Year:</span>
            <select
              value={selectedYear}
              onChange={(e) => setSelectedYear(Number(e.target.value))}
              className="h-8 px-3 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs font-medium"
            >
              {[2024, 2025, 2026, 2027].map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setSelectedYear(new Date().getFullYear())}
              className="text-xs h-8 text-blue-600 dark:text-blue-400"
            >
              Current Year
            </Button>
          </div>
        )}

        {timeframeMode === "custom" && (
          <div className="pt-2 border-t border-zinc-200/70 dark:border-zinc-800 flex items-center gap-3 flex-wrap text-xs">
            <div className="flex items-center gap-1.5">
              <span className="text-muted-foreground">From:</span>
              <Input
                type="date"
                value={customStartDate}
                onChange={(e) => setCustomStartDate(e.target.value)}
                className="w-36 h-8 text-xs font-medium"
              />
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-muted-foreground">To:</span>
              <Input
                type="date"
                value={customEndDate}
                onChange={(e) => setCustomEndDate(e.target.value)}
                className="w-36 h-8 text-xs font-medium"
              />
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setCustomStartDate(getTodayString())
                setCustomEndDate(getTodayString())
              }}
              className="text-xs h-8 text-blue-600 dark:text-blue-400"
            >
              Reset to Today
            </Button>
          </div>
        )}
      </Card>

      {/* 3. PERIOD METRICS & KPI HIGHLIGHTS */}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {/* Total Sales Volume */}
        <Card className="p-4 rounded-2xl border-zinc-200 dark:border-zinc-800 bg-gradient-to-br from-amber-500/10 via-amber-500/5 to-transparent">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span className="font-semibold text-amber-700 dark:text-amber-400">Total Sales Booked</span>
            <IndianRupee className="h-4 w-4 text-amber-600" />
          </div>
          <div className="mt-2 text-2xl font-black text-zinc-900 dark:text-zinc-50">
            ₹{formatInr(periodMetrics.totalSales)}
          </div>
          <p className="text-[11px] text-muted-foreground mt-1">
            {periodMetrics.totalOrders} orders ({periodMetrics.totalPieces} pcs / {periodMetrics.totalCases} cases)
          </p>
        </Card>

        {/* Collections & Recovery */}
        <Card className="p-4 rounded-2xl border-zinc-200 dark:border-zinc-800 bg-gradient-to-br from-emerald-500/10 via-emerald-500/5 to-transparent">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span className="font-semibold text-emerald-700 dark:text-emerald-400">Total Paid Amount</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
          </div>
          <div className="mt-2 text-2xl font-black text-emerald-600 dark:text-emerald-400">
            ₹{formatInr(periodMetrics.totalPaid)}
          </div>
          <p className="text-[11px] text-muted-foreground mt-1">
            Paid & confirmed collections in period
          </p>
        </Card>

        {/* Outstanding Pending Dues */}
        <Card className="p-4 rounded-2xl border-zinc-200 dark:border-zinc-800 bg-gradient-to-br from-red-500/10 via-red-500/5 to-transparent">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span className="font-semibold text-red-700 dark:text-red-400">Pending Dues</span>
            <AlertCircle className="h-4 w-4 text-red-600" />
          </div>
          <div className="mt-2 text-2xl font-black text-red-600 dark:text-red-400">
            ₹{formatInr(periodMetrics.totalDues)}
          </div>
          <p className="text-[11px] text-muted-foreground mt-1">
            {pendingPaymentsList.length} orders awaiting balance settlement
          </p>
        </Card>

        {/* Trips & Market Coverage */}
        <Card className="p-4 rounded-2xl border-zinc-200 dark:border-zinc-800 bg-gradient-to-br from-blue-500/10 via-blue-500/5 to-transparent">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span className="font-semibold text-blue-700 dark:text-blue-400">Trips & Deliveries</span>
            <MapPin className="h-4 w-4 text-blue-600" />
          </div>
          <div className="mt-2 text-2xl font-black text-zinc-900 dark:text-zinc-50">
            {periodMetrics.totalVisits} Trips
          </div>
          <div className="flex items-center gap-2 text-[11px] text-muted-foreground mt-1">
            <span className="text-emerald-600 font-medium">✓ {periodMetrics.delivered} Delivered</span>
            <span>•</span>
            <span className="text-amber-600 font-medium">⏳ {periodMetrics.pendingDeliveries} Pending</span>
          </div>
        </Card>
      </div>

      {/* 4. TABS NAVIGATION */}
      <div className="flex items-center justify-between border-b border-zinc-200 dark:border-zinc-800 pb-3">
        <Tabs
          value={activeTab}
          onValueChange={setActiveTab}
          options={[
            { value: "orders", label: "Orders Booked", count: filteredEntries.length },
            { value: "visits", label: "Market Trips", count: filteredVisits.length },
            { value: "payments", label: "Pending Payments", count: pendingPaymentsList.length },
            { value: "deliveries", label: "Dispatch & Logistics", count: periodMetrics.pendingDeliveries },
            { value: "bio", label: "Staff Profile & Territory" }
          ]}
        />
      </div>

      {/* 5. TAB CONTENT PANELS */}

      {/* TAB 1: ALL ORDERS BOOKED */}
      {activeTab === "orders" && (
        <div className="space-y-4">
          {/* Controls Bar */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
            <div className="relative w-full sm:w-80">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-zinc-400" />
              <Input
                placeholder="Search Item, Supplier, Customer, Order #..."
                value={ordersSearch}
                onChange={(e) => setOrdersSearch(e.target.value)}
                className="pl-8 text-xs h-8"
              />
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto flex-wrap">
              {/* Payment Filter */}
              <select
                value={ordersPaymentFilter}
                onChange={(e) => setOrdersPaymentFilter(e.target.value)}
                className="h-8 px-2.5 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs"
              >
                <option value="all">Payment: All</option>
                <option value="paid">Payment: Paid</option>
                <option value="partial">Payment: Partial</option>
                <option value="unpaid">Payment: Unpaid</option>
              </select>

              {/* Delivery Filter */}
              <select
                value={ordersDeliveryFilter}
                onChange={(e) => setOrdersDeliveryFilter(e.target.value)}
                className="h-8 px-2.5 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs"
              >
                <option value="all">Delivery: All</option>
                <option value="pending">Delivery: Pending</option>
                <option value="dispatched">Delivery: Dispatched</option>
                <option value="delivered">Delivery: Delivered</option>
              </select>
            </div>
          </div>

          {/* Table of Orders */}
          {displayOrders.length === 0 ? (
            <Card className="p-12 text-center text-xs text-muted-foreground">
              No orders found for the selected period or filters.
            </Card>
          ) : (
            <div className="overflow-x-auto rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 shadow-sm">
              <table className="w-full text-left text-xs">
                <thead className="bg-zinc-50 dark:bg-zinc-900/60 text-zinc-500 font-semibold border-b border-zinc-200 dark:border-zinc-800">
                  <tr>
                    <th className="p-3">Order / Date</th>
                    <th className="p-3">Customer</th>
                    <th className="p-3">Supplier & Item</th>
                    <th className="p-3 text-right">Quantity</th>
                    <th className="p-3 text-right">Rate</th>
                    <th className="p-3 text-right">Total Bill</th>
                    <th className="p-3">Payment</th>
                    <th className="p-3">Delivery / LR</th>
                    <th className="p-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                  {displayOrders.map((entry) => {
                    const parentVisit = visitMap.get(Number(entry.visitId))
                    const customer = parentVisit ? customerMap.get(Number(parentVisit.customerId)) : null
                    const bill = (Number(entry.totalAmount) || 0) + (Number(entry.gstAmount) || 0)
                    const paid = Number(entry.paidAmount) || 0
                    const due = Math.max(0, bill - paid)

                    return (
                      <tr key={entry.id} className="hover:bg-zinc-50/70 dark:hover:bg-zinc-900/40 transition-colors">
                        <td className="p-3">
                          <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">
                            {entry.orderNo || `ORD-${entry.id}`}
                          </span>
                          <p className="text-[11px] text-muted-foreground mt-0.5">
                            {formatDate(parentVisit?.date || entry.createdAt)}
                          </p>
                        </td>

                        <td className="p-3">
                          <p className="font-bold text-zinc-900 dark:text-zinc-100 truncate max-w-[150px]">
                            {parentVisit?.customerName || customer?.name || "Client Account"}
                          </p>
                          {customer?.marketArea && (
                            <p className="text-[10px] text-muted-foreground truncate max-w-[150px]">
                              {customer.marketArea}
                            </p>
                          )}
                        </td>

                        <td className="p-3">
                          <p className="font-semibold text-zinc-800 dark:text-zinc-200 truncate max-w-[150px]">
                            {entry.supplierName}
                          </p>
                          <span className="font-mono text-[11px] text-muted-foreground">
                            {entry.itemCode}
                          </span>
                        </td>

                        <td className="p-3 text-right">
                          <span className="font-bold text-zinc-900 dark:text-zinc-100">
                            {entry.pieces} pcs
                          </span>
                          <p className="text-[10px] text-muted-foreground">
                            {entry.caseCount ? `${entry.caseCount} cs` : ""}
                            {entry.loosePieces ? ` + ${entry.loosePieces} loose` : ""}
                          </p>
                        </td>

                        <td className="p-3 text-right font-medium">
                          ₹{formatInr(Number(entry.pricePerPiece) || Number(entry.rate) || 0)}
                        </td>

                        <td className="p-3 text-right">
                          <span className="font-bold text-zinc-900 dark:text-zinc-100">
                            ₹{formatInr(bill)}
                          </span>
                          {due > 0 ? (
                            <p className="text-[10px] text-red-600 dark:text-red-400 font-medium">
                              Due: ₹{formatInr(due)}
                            </p>
                          ) : (
                            <p className="text-[10px] text-emerald-600 dark:text-emerald-400 font-medium">
                              Cleared ✓
                            </p>
                          )}
                        </td>

                        <td className="p-3">
                          <Badge
                            variant={
                              entry.paymentStatus?.toLowerCase() === "paid"
                                ? "success"
                                : entry.paymentStatus?.toLowerCase() === "partial"
                                ? "warning"
                                : "destructive"
                            }
                          >
                            {entry.paymentStatus || "Unpaid"}
                          </Badge>
                          {entry.paymentMode && (
                            <p className="text-[10px] text-muted-foreground mt-0.5">
                              via {entry.paymentMode}
                            </p>
                          )}
                        </td>

                        <td className="p-3">
                          <Badge
                            variant={
                              entry.deliveryStatus?.toLowerCase() === "delivered"
                                ? "success"
                                : entry.deliveryStatus?.toLowerCase() === "dispatched"
                                ? "default"
                                : "secondary"
                            }
                          >
                            {entry.deliveryStatus || "Pending"}
                          </Badge>
                          {entry.lrNo && (
                            <p className="text-[10px] font-mono text-muted-foreground mt-0.5" title={entry.transporter}>
                              LR: {entry.lrNo}
                            </p>
                          )}
                        </td>

                        <td className="p-3 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => handleOpenInvoice(entry)}
                              className="h-6 px-2 text-[11px] text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40"
                              title="View & Download Supplier Invoice (PDF)"
                            >
                              <FileText className="h-3 w-3 mr-0.5" />
                              PDF
                            </Button>
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => handleOpenPayment(entry)}
                              className="h-6 px-2 text-[11px]"
                              title="Update Payment Record"
                            >
                              Pay
                            </Button>
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => handleOpenDelivery(entry)}
                              className="h-6 px-2 text-[11px]"
                              title="Update Delivery & LR info"
                            >
                              LR
                            </Button>
                          </div>
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

      {/* TAB 2: MARKET TRIPS / VISITS */}
      {activeTab === "visits" && (
        <div className="space-y-4">
          <div className="flex items-center justify-between gap-3">
            <p className="text-xs text-muted-foreground">
              Showing <span className="font-bold text-zinc-900 dark:text-zinc-100">{displayVisits.length}</span> market trips conducted by {employee.name} in this timeframe.
            </p>
            <div className="flex items-center gap-2">
              <select
                value={visitsStatusFilter}
                onChange={(e) => setVisitsStatusFilter(e.target.value)}
                className="h-8 px-2.5 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs"
              >
                <option value="all">Status: All</option>
                <option value="active">Active Only</option>
                <option value="completed">Completed Only</option>
                <option value="cancelled">Cancelled Only</option>
              </select>
            </div>
          </div>

          {displayVisits.length === 0 ? (
            <Card className="p-12 text-center text-xs text-muted-foreground">
              No trips recorded for this timeframe.
            </Card>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {displayVisits.map((visit) => {
                const customer = customerMap.get(Number(visit.customerId))
                const bookedEntries = entries.filter((e) => Number(e.visitId) === visit.id)
                const visitSales = bookedEntries.reduce(
                  (sum, e) => sum + (Number(e.totalAmount) || 0) + (Number(e.gstAmount) || 0),
                  0
                )
                const visitPieces = bookedEntries.reduce(
                  (sum, e) => sum + (Number(e.pieces) || 0),
                  0
                )

                return (
                  <Card key={visit.id} className="p-4 rounded-2xl flex flex-col justify-between hover:border-zinc-300 dark:hover:border-zinc-700 transition-all">
                    <div>
                      <div className="flex items-start justify-between">
                        <div>
                          <Badge
                            variant={
                              visit.status?.toLowerCase() === "active"
                                ? "success"
                                : visit.status?.toLowerCase() === "completed"
                                ? "secondary"
                                : "destructive"
                            }
                          >
                            {visit.status || "Active"}
                          </Badge>
                          <h3 className="font-bold text-zinc-900 dark:text-zinc-100 mt-2 text-sm">
                            {visit.customerName || customer?.name || "Client Visit"}
                          </h3>
                        </div>
                        <span className="font-mono text-[10px] text-muted-foreground bg-zinc-100 dark:bg-zinc-800 px-2 py-0.5 rounded-full">
                          {visit.visitCode || `VST-${visit.id}`}
                        </span>
                      </div>

                      <div className="mt-2 text-xs text-muted-foreground space-y-1">
                        <p className="flex items-center gap-1">
                          <Calendar className="h-3 w-3 text-zinc-400" />
                          <span>Date: {formatDate(visit.date)}</span>
                        </p>
                        {customer?.marketArea && (
                          <p className="flex items-center gap-1">
                            <MapPin className="h-3 w-3 text-zinc-400" />
                            <span>{customer.marketArea}</span>
                          </p>
                        )}
                        {visit.notes && (
                          <p className="text-[11px] italic line-clamp-2 mt-1.5 text-zinc-600 dark:text-zinc-400 bg-zinc-50 dark:bg-zinc-900/50 p-2 rounded-lg">
                            "{visit.notes}"
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="mt-3.5 pt-3 border-t border-zinc-100 dark:border-zinc-800 flex items-center justify-between text-xs">
                      <div>
                        <span className="text-[10px] text-muted-foreground">Bookings</span>
                        <p className="font-bold text-zinc-900 dark:text-zinc-100">
                          {bookedEntries.length} Items ({visitPieces} pcs)
                        </p>
                      </div>
                      <div className="flex items-center gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          shape="pill"
                          onClick={() => handleOpenCustomerReport(visit)}
                          className="h-7 text-[11px] px-2.5 text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-900/50 hover:bg-blue-50 dark:hover:bg-blue-950/40"
                          title="Generate Customer Day Report PDF"
                        >
                          <Printer className="h-3 w-3 mr-1" />
                          Report
                        </Button>
                        <div className="text-right">
                          <span className="text-[10px] text-muted-foreground">Total Invoiced</span>
                          <p className="font-bold text-amber-600 dark:text-amber-400">
                            ₹{formatInr(visitSales)}
                          </p>
                        </div>
                      </div>
                    </div>
                  </Card>
                )
              })}
            </div>
          )}
        </div>
      )}

      {/* TAB 3: PENDING PAYMENTS & COLLECTIONS */}
      {activeTab === "payments" && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-xs text-muted-foreground">
              Total <span className="font-bold text-red-600">{pendingPaymentsList.length}</span> orders booked by {employee.name} with pending payment balances (Total Dues: ₹{formatInr(periodMetrics.totalDues)}).
            </p>
          </div>

          {pendingPaymentsList.length === 0 ? (
            <Card className="p-12 text-center text-xs text-emerald-600 dark:text-emerald-400 font-medium">
              ✓ All orders booked in this timeframe have been fully paid and cleared!
            </Card>
          ) : (
            <div className="overflow-x-auto rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 shadow-sm">
              <table className="w-full text-left text-xs">
                <thead className="bg-zinc-50 dark:bg-zinc-900/60 text-zinc-500 font-semibold border-b border-zinc-200 dark:border-zinc-800">
                  <tr>
                    <th className="p-3">Order #</th>
                    <th className="p-3">Customer Account</th>
                    <th className="p-3">Supplier & Item</th>
                    <th className="p-3 text-right">Total Bill</th>
                    <th className="p-3 text-right">Paid Amount</th>
                    <th className="p-3 text-right">Remaining Due</th>
                    <th className="p-3">Status</th>
                    <th className="p-3 text-right">Collect Payment</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                  {pendingPaymentsList.map((entry) => {
                    const parentVisit = visitMap.get(Number(entry.visitId))
                    const customer = parentVisit ? customerMap.get(Number(parentVisit.customerId)) : null
                    const bill = (Number(entry.totalAmount) || 0) + (Number(entry.gstAmount) || 0)
                    const paid = Number(entry.paidAmount) || 0
                    const due = Math.max(0, bill - paid)

                    return (
                      <tr key={entry.id} className="hover:bg-zinc-50/70 dark:hover:bg-zinc-900/40">
                        <td className="p-3 font-mono font-bold text-zinc-900 dark:text-zinc-100">
                          {entry.orderNo || `ORD-${entry.id}`}
                        </td>
                        <td className="p-3">
                          <p className="font-bold text-zinc-900 dark:text-zinc-100">
                            {parentVisit?.customerName || customer?.name || "Client"}
                          </p>
                          {customer?.phone && (
                            <p className="text-[10px] text-muted-foreground font-mono">
                              {customer.phone}
                            </p>
                          )}
                        </td>
                        <td className="p-3">
                          <p className="font-medium text-zinc-800 dark:text-zinc-200">
                            {entry.supplierName}
                          </p>
                          <span className="font-mono text-[10px] text-muted-foreground">
                            {entry.itemCode} ({entry.pieces} pcs)
                          </span>
                        </td>
                        <td className="p-3 text-right font-medium">
                          ₹{formatInr(bill)}
                        </td>
                        <td className="p-3 text-right text-emerald-600 dark:text-emerald-400 font-semibold">
                          ₹{formatInr(paid)}
                        </td>
                        <td className="p-3 text-right text-red-600 dark:text-red-400 font-bold">
                          ₹{formatInr(due)}
                        </td>
                        <td className="p-3">
                          <Badge variant={entry.paymentStatus?.toLowerCase() === "partial" ? "warning" : "destructive"}>
                            {entry.paymentStatus || "Unpaid"}
                          </Badge>
                        </td>
                        <td className="p-3 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            <Button
                              size="sm"
                              variant="outline"
                              shape="pill"
                              onClick={() => handleOpenInvoice(entry)}
                              className="h-7 text-xs font-semibold gap-1 text-blue-600 dark:text-blue-400 border-blue-200"
                              title="View Invoice PDF"
                            >
                              <FileText className="h-3 w-3" />
                              Invoice
                            </Button>
                            <Button
                              size="sm"
                              shape="pill"
                              onClick={() => handleOpenPayment(entry)}
                              className="h-7 text-xs font-semibold gap-1"
                            >
                              <IndianRupee className="h-3 w-3" />
                              Record Payment
                            </Button>
                          </div>
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

      {/* TAB 4: DISPATCH & DELIVERIES */}
      {activeTab === "deliveries" && (
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-3">
            <Card className="p-3 text-center rounded-xl bg-amber-500/10 border-amber-500/20">
              <span className="text-[10px] text-muted-foreground font-semibold">PENDING DISPATCH</span>
              <p className="text-xl font-bold text-amber-700 dark:text-amber-400 mt-0.5">
                {periodMetrics.pendingDeliveries - periodMetrics.dispatched} Orders
              </p>
            </Card>
            <Card className="p-3 text-center rounded-xl bg-blue-500/10 border-blue-500/20">
              <span className="text-[10px] text-muted-foreground font-semibold">IN TRANSIT / DISPATCHED</span>
              <p className="text-xl font-bold text-blue-700 dark:text-blue-400 mt-0.5">
                {periodMetrics.dispatched} Orders
              </p>
            </Card>
            <Card className="p-3 text-center rounded-xl bg-emerald-500/10 border-emerald-500/20">
              <span className="text-[10px] text-muted-foreground font-semibold">DELIVERED</span>
              <p className="text-xl font-bold text-emerald-700 dark:text-emerald-400 mt-0.5">
                {periodMetrics.delivered} Orders
              </p>
            </Card>
          </div>

          <div className="overflow-x-auto rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 shadow-sm">
            <table className="w-full text-left text-xs">
              <thead className="bg-zinc-50 dark:bg-zinc-900/60 text-zinc-500 font-semibold border-b border-zinc-200 dark:border-zinc-800">
                <tr>
                  <th className="p-3">Order #</th>
                  <th className="p-3">Customer</th>
                  <th className="p-3">Supplier / Goods</th>
                  <th className="p-3">Status</th>
                  <th className="p-3">Transporter</th>
                  <th className="p-3">LR Tracking</th>
                  <th className="p-3 text-right">Update</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                {filteredEntries.map((entry) => {
                  const parentVisit = visitMap.get(Number(entry.visitId))
                  return (
                    <tr key={entry.id} className="hover:bg-zinc-50/70 dark:hover:bg-zinc-900/40">
                      <td className="p-3 font-mono font-bold text-zinc-900 dark:text-zinc-100">
                        {entry.orderNo || `ORD-${entry.id}`}
                      </td>
                      <td className="p-3 font-medium text-zinc-900 dark:text-zinc-100">
                        {parentVisit?.customerName || "Customer"}
                      </td>
                      <td className="p-3">
                        <span className="text-zinc-800 dark:text-zinc-200 font-medium">
                          {entry.supplierName}
                        </span>
                        <p className="text-[10px] text-muted-foreground">
                          {entry.pieces} pcs ({entry.itemCode})
                        </p>
                      </td>
                      <td className="p-3">
                        <Badge
                          variant={
                            entry.deliveryStatus?.toLowerCase() === "delivered"
                              ? "success"
                              : entry.deliveryStatus?.toLowerCase() === "dispatched"
                              ? "default"
                              : "secondary"
                          }
                        >
                          {entry.deliveryStatus || "Pending"}
                        </Badge>
                      </td>
                      <td className="p-3 font-medium">
                        {entry.transporter || "—"}
                      </td>
                      <td className="p-3 font-mono">
                        {entry.lrNo ? (
                          <div>
                            <span className="font-semibold text-zinc-900 dark:text-zinc-100">
                              {entry.lrNo}
                            </span>
                            {entry.lrDate && (
                              <p className="text-[10px] text-muted-foreground">
                                Date: {formatDate(entry.lrDate)}
                              </p>
                            )}
                          </div>
                        ) : (
                          <span className="text-muted-foreground">—</span>
                        )}
                      </td>
                      <td className="p-3 text-right">
                        <Button
                          size="sm"
                          variant="outline"
                          shape="pill"
                          onClick={() => handleOpenDelivery(entry)}
                          className="h-7 text-xs"
                        >
                          Update LR
                        </Button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 5: STAFF BIO & TERRITORY PROFILE */}
      {activeTab === "bio" && (
        <div className="grid gap-4 md:grid-cols-2">
          {/* Card A: Contact Directory (Up to 5 Phones & Emails) */}
          <Card className="p-5 rounded-2xl border-zinc-200 dark:border-zinc-800 space-y-4">
            <h3 className="font-bold text-sm text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
              <Phone className="h-4 w-4 text-zinc-500" />
              Direct Contacts & Numbers
            </h3>

            <div className="space-y-2.5 text-xs">
              {/* Primary Phone */}
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-zinc-50 dark:bg-zinc-900">
                <span className="text-muted-foreground font-medium">Primary Contact:</span>
                {employee.phone ? (
                  <a
                    href={`tel:${employee.phone}`}
                    className="font-bold font-mono text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1"
                  >
                    <Phone className="h-3.5 w-3.5" />
                    {employee.phone}
                  </a>
                ) : (
                  <span className="text-muted-foreground italic">Not provided</span>
                )}
              </div>

              {/* Extra Phones 2..5 */}
              {extraPhones.map((phone, idx) => (
                <div key={idx} className="flex items-center justify-between p-2.5 rounded-xl bg-zinc-50 dark:bg-zinc-900">
                  <span className="text-muted-foreground font-medium">Alternate Line {idx + 2}:</span>
                  <a
                    href={`tel:${phone}`}
                    className="font-mono text-zinc-800 dark:text-zinc-200 hover:underline flex items-center gap-1"
                  >
                    <Phone className="h-3.5 w-3.5 text-zinc-400" />
                    {phone}
                  </a>
                </div>
              ))}

              {/* Primary & Alternate Email */}
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-zinc-50 dark:bg-zinc-900">
                <span className="text-muted-foreground font-medium">Primary Google Email:</span>
                {employee.email ? (
                  <a
                    href={`mailto:${employee.email}`}
                    className="font-medium text-zinc-800 dark:text-zinc-200 hover:underline flex items-center gap-1"
                  >
                    <Mail className="h-3.5 w-3.5 text-zinc-400" />
                    {employee.email}
                  </a>
                ) : (
                  <span className="text-muted-foreground italic">Not linked</span>
                )}
              </div>

              {employee.alternateEmail && (
                <div className="flex items-center justify-between p-2.5 rounded-xl bg-zinc-50 dark:bg-zinc-900">
                  <span className="text-muted-foreground font-medium">Alternate Email:</span>
                  <a
                    href={`mailto:${employee.alternateEmail}`}
                    className="font-medium text-zinc-800 dark:text-zinc-200 hover:underline flex items-center gap-1"
                  >
                    <Mail className="h-3.5 w-3.5 text-zinc-400" />
                    {employee.alternateEmail}
                  </a>
                </div>
              )}
            </div>

            {/* Emergency Contact */}
            {employee.emergencyContactPhone && (
              <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-xs">
                <span className="text-[10px] font-bold text-red-700 dark:text-red-400 uppercase tracking-wide">
                  Emergency Contact (SOS)
                </span>
                <div className="mt-1 flex items-center justify-between">
                  <span className="font-semibold text-zinc-900 dark:text-zinc-100">
                    {employee.emergencyContactName || "Guardian / Kin"}
                  </span>
                  <a
                    href={`tel:${employee.emergencyContactPhone}`}
                    className="font-mono font-bold text-red-600 dark:text-red-400 hover:underline"
                  >
                    {employee.emergencyContactPhone}
                  </a>
                </div>
              </div>
            )}
          </Card>

          {/* Card B: Territory & Addresses */}
          <Card className="p-5 rounded-2xl border-zinc-200 dark:border-zinc-800 space-y-4">
            <h3 className="font-bold text-sm text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
              <MapPin className="h-4 w-4 text-zinc-500" />
              Territory, Addresses & Origin
            </h3>

            <div className="space-y-3 text-xs">
              <div>
                <span className="text-muted-foreground font-medium">Assigned Textile Markets:</span>
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {assignedMarketList.length > 0 ? (
                    assignedMarketList.map((m) => (
                      <span
                        key={m}
                        className="rounded-full bg-zinc-100 dark:bg-zinc-800 text-zinc-800 dark:text-zinc-200 px-3 py-1 text-[11px] font-medium"
                      >
                        {m}
                      </span>
                    ))
                  ) : (
                    <span className="text-muted-foreground italic">No specific markets assigned (All territories)</span>
                  )}
                </div>
              </div>

              <div className="pt-2 border-t border-zinc-100 dark:border-zinc-800/80">
                <span className="text-muted-foreground font-medium">Current Residential Address:</span>
                <p className="mt-1 text-zinc-800 dark:text-zinc-200">
                  {employee.currentAddress || employee.address || "Not specified"}
                </p>
              </div>

              {employee.permanentAddress && (
                <div className="pt-2 border-t border-zinc-100 dark:border-zinc-800/80">
                  <span className="text-muted-foreground font-medium">Permanent / Native Address:</span>
                  <p className="mt-1 text-zinc-800 dark:text-zinc-200">
                    {employee.permanentAddress}
                  </p>
                </div>
              )}

              {employee.personalLocation && (
                <div className="pt-2 border-t border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between">
                  <span className="text-muted-foreground font-medium">Native / Personal Location:</span>
                  <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {employee.personalLocation}
                  </span>
                </div>
              )}

              {employee.referredBy && (
                <div className="pt-2 border-t border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between">
                  <span className="text-muted-foreground font-medium">Referred By:</span>
                  <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {employee.referredBy}
                  </span>
                </div>
              )}
            </div>
          </Card>
        </div>
      )}

      {/* PAYMENT RECORDING DIALOG */}
      {paymentEntry && (
        <Dialog
          open={!!paymentEntry}
          onOpenChange={(open) => !open && setPaymentEntry(null)}
          title={`Record Payment for Order ${paymentEntry.orderNo || paymentEntry.id}`}
          description={`Customer: ${visitMap.get(Number(paymentEntry.visitId))?.customerName || "Client"} • Supplier: ${paymentEntry.supplierName}`}
        >
          <div className="space-y-4 pt-2">
            <div>
              <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
                Amount Paid (₹)
              </label>
              <Input
                type="number"
                value={paymentAmount}
                onChange={(e) => setPaymentAmount(e.target.value)}
                placeholder="Enter paid amount"
                className="mt-1"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
                  Payment Status
                </label>
                <select
                  value={paymentStatus}
                  onChange={(e) => setPaymentStatus(e.target.value)}
                  className="mt-1 w-full h-9 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs px-2.5"
                >
                  <option value="Paid">Paid (Full)</option>
                  <option value="Partial">Partial</option>
                  <option value="Unpaid">Unpaid</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
                  Payment Mode
                </label>
                <select
                  value={paymentMode}
                  onChange={(e) => setPaymentMode(e.target.value)}
                  className="mt-1 w-full h-9 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs px-2.5"
                >
                  <option value="Cash">Cash</option>
                  <option value="UPI">UPI / GPay / PhonePe</option>
                  <option value="Bank Transfer">Bank Transfer / NEFT</option>
                  <option value="Cheque">Cheque</option>
                </select>
              </div>
            </div>

            <div>
              <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
                Remarks / Reference Note
              </label>
              <Input
                value={paymentRemarks}
                onChange={(e) => setPaymentRemarks(e.target.value)}
                placeholder="Cheque #, transaction ref, note..."
                className="mt-1"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button variant="outline" size="sm" onClick={() => setPaymentEntry(null)}>
                Cancel
              </Button>
              <Button size="sm" onClick={handleSavePayment}>
                Save Payment
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* DELIVERY UPDATE DIALOG */}
      {deliveryEntry && (
        <Dialog
          open={!!deliveryEntry}
          onOpenChange={(open) => !open && setDeliveryEntry(null)}
          title={`Update Dispatch for ${deliveryEntry.orderNo || deliveryEntry.id}`}
          description={`Supplier: ${deliveryEntry.supplierName} • Item: ${deliveryEntry.itemCode}`}
        >
          <div className="space-y-4 pt-2">
            <div>
              <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
                Delivery / Logistics Status
              </label>
              <select
                value={deliveryStatus}
                onChange={(e) => setDeliveryStatus(e.target.value)}
                className="mt-1 w-full h-9 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs px-2.5"
              >
                <option value="Pending">Pending Dispatch</option>
                <option value="Packed">Packed in Case</option>
                <option value="Dispatched">Dispatched (LR Issued)</option>
                <option value="Delivered">Delivered to Customer</option>
              </select>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
                  Transporter / Angadia Name
                </label>
                <Input
                  value={transporter}
                  onChange={(e) => setTransporter(e.target.value)}
                  placeholder="e.g. Navrang, Mahavir..."
                  className="mt-1"
                />
              </div>

              <div>
                <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
                  LR Number
                </label>
                <Input
                  value={lrNo}
                  onChange={(e) => setLrNo(e.target.value)}
                  placeholder="e.g. LR-98214"
                  className="mt-1"
                />
              </div>
            </div>

            <div>
              <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
                LR Booking Date
              </label>
              <Input
                type="date"
                value={lrDate}
                onChange={(e) => setLrDate(e.target.value)}
                className="mt-1"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button variant="outline" size="sm" onClick={() => setDeliveryEntry(null)}>
                Cancel
              </Button>
              <Button size="sm" onClick={handleSaveDelivery}>
                Save Delivery Info
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* SUSPEND ACCESS DIALOG */}
      {showSuspendDialog && (
        <Dialog
          open={showSuspendDialog}
          onOpenChange={(open) => !open && setShowSuspendDialog(false)}
          title={`Stop Access - ${employee.name}`}
          description="Suspend salesman login and Android mobile app permissions"
        >
          <div className="space-y-4 pt-1">
            <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-3.5 text-xs text-amber-900 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-200">
              <p className="font-semibold mb-1">🔒 Immediate Real-time Lockout</p>
              <p>
                When suspended, {employee.name} will be immediately locked out of the Android mobile app and cannot start visits or enter orders.
              </p>
              <p className="mt-2 font-medium text-emerald-800 dark:text-emerald-300">
                ✓ 100% of their historical visits, client mappings, orders, and sales volume remain completely intact and visible in the Admin Panel. You can resume their access at any moment.
              </p>
            </div>

            <div>
              <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                Reason for Suspension (Optional)
              </label>
              <Input
                value={suspendReason}
                onChange={(e) => setSuspendReason(e.target.value)}
                placeholder="e.g. On temporary leave, administrative review..."
                className="mt-1"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="outline"
                size="sm"
                disabled={isProcessingAction}
                onClick={() => setShowSuspendDialog(false)}
              >
                Cancel
              </Button>
              <Button
                size="sm"
                disabled={isProcessingAction}
                onClick={handleSuspend}
                className="bg-amber-600 hover:bg-amber-700 text-white font-semibold gap-1.5"
              >
                <Pause className="h-3.5 w-3.5" />
                {isProcessingAction ? "Stopping..." : "Confirm Stop Access"}
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* DEACTIVATE STAFF DIALOG */}
      {showDeactivateDialog && (
        <Dialog
          open={showDeactivateDialog}
          onOpenChange={(open) => !open && setShowDeactivateDialog(false)}
          title={`Deactivate Staff Profile - ${employee.name}`}
          description="Soft-deactivate employee while safeguarding all historical data"
        >
          <div className="space-y-4 pt-1">
            <div className="rounded-xl border border-rose-200 bg-rose-50/80 p-3.5 text-xs text-rose-900 dark:border-rose-900/50 dark:bg-rose-950/40 dark:text-rose-200">
              <p className="font-semibold mb-1">⚠️ Safe Soft Deactivation</p>
              <p>
                This will deactivate {employee.name}'s account and prevent mobile app login.
              </p>
              <p className="mt-2 font-medium text-zinc-800 dark:text-zinc-200">
                ✓ <strong>Zero Data Loss:</strong> All past trips, purchase orders, customer history, and ledger balances stay 100% preserved for business audits and analytics.
              </p>
            </div>

            <div>
              <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                Reason for Deactivation (Optional)
              </label>
              <Input
                value={deactivateReason}
                onChange={(e) => setDeactivateReason(e.target.value)}
                placeholder="e.g. Left organization, role transitioned..."
                className="mt-1"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="outline"
                size="sm"
                disabled={isProcessingAction}
                onClick={() => setShowDeactivateDialog(false)}
              >
                Cancel
              </Button>
              <Button
                size="sm"
                disabled={isProcessingAction}
                onClick={handleDeactivate}
                className="bg-rose-600 hover:bg-rose-700 text-white font-semibold gap-1.5"
              >
                <UserX className="h-3.5 w-3.5" />
                {isProcessingAction ? "Deactivating..." : "Confirm Deactivate"}
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
