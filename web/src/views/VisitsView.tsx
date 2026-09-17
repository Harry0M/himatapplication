import React, { useState } from "react"
import {
  MapPin,
  Plus,
  Calendar,
  User,
  Search,
  Eye,
  CheckCircle2,
  X,
  Filter,
  UserCheck,
  FileText,
  Printer
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatDate, formatInr } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Tabs } from "../components/ui/Tabs"
import { Visit, PurchaseEntry, Supplier } from "../types"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateCustomerDayReportHtml,
  buildCustomerReportWhatsAppText,
  generateSupplierInvoiceHtml,
  buildSupplierInvoiceWhatsAppText,
} from "../lib/pdfReports"

export function VisitsView() {
  const {
    visits,
    entries,
    customers,
    suppliers,
    employees,
    packGroups,
    selectedEmployeeId,
    setSelectedEmployeeId,
    saveVisit,
    savePurchaseEntry,
  } = useData()

  const [search, setSearch] = useState<string>("")
  const [showSearch, setShowSearch] = useState<boolean>(false)
  const [statusFilter, setStatusFilter] = useState<string>("all")
  const [selectedVisit, setSelectedVisit] = useState<Visit | null>(null)
  const [isAddOpen, setIsAddOpen] = useState<boolean>(false)

  // Add Stop / Order into Visit state
  const [isAddOrderOpen, setIsAddOrderOpen] = useState<boolean>(false)
  const [orderSupplierId, setOrderSupplierId] = useState<number>(0)
  const [orderItemCode, setOrderItemCode] = useState<string>("")
  const [orderPieces, setOrderPieces] = useState<string>("")
  const [orderCases, setOrderCases] = useState<string>("")
  const [orderLoose, setOrderLoose] = useState<string>("")
  const [orderRate, setOrderRate] = useState<string>("")
  const [orderCaseSize, setOrderCaseSize] = useState<string>("24")
  const [orderTransporter, setOrderTransporter] = useState<string>("")

  // New Visit Form state
  const [customerName, setCustomerName] = useState<string>("")
  const [customerId, setCustomerId] = useState<number>(0)
  const [agentId, setAgentId] = useState<number>(employees[0]?.id || 1)
  const [agentName, setAgentName] = useState<string>(employees[0]?.name || "harry")
  const [visitDate, setVisitDate] = useState<string>(new Date().toISOString().split("T")[0])

  // PDF / Report Viewer modal state
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

  // Active employee for filtering
  const activeEmployee = React.useMemo(() => {
    if (selectedEmployeeId === "all") return null
    return employees.find((e) => e.id === selectedEmployeeId) || null
  }, [selectedEmployeeId, employees])

  const q = search.trim().toLowerCase()
  const filteredVisits = visits.filter((v) => {
    // 1. Employee filter
    if (activeEmployee) {
      const matchId = Number(v.employeeId) === activeEmployee.id
      const matchName =
        v.employeeName &&
        v.employeeName.trim().toLowerCase() === activeEmployee.name.trim().toLowerCase()
      if (!matchId && !matchName) return false
    }

    // 2. Status filter
    if (statusFilter !== "all" && v.status?.toLowerCase() !== statusFilter.toLowerCase()) {
      return false
    }

    // 3. Search query
    return (
      !q ||
      v.customerName?.toLowerCase().includes(q) ||
      v.visitCode?.toLowerCase().includes(q) ||
      v.employeeName?.toLowerCase().includes(q)
    )
  })

  const handleCreateVisit = async () => {
    if (!customerName.trim()) return
    const newId = Date.now()
    const chosenEmp = employees.find((e) => e.id === agentId) || {
      id: agentId,
      name: agentName,
      role: "Salesman",
    }

    const newVisit: Visit = {
      id: newId,
      visitCode: `VIS-${Math.floor(1000 + Math.random() * 9000)}`,
      customerId: customerId || 0,
      customerName: customerName.trim(),
      date: visitDate,
      employeeId: chosenEmp.id,
      employeeName: chosenEmp.name,
      status: "Active",
      notes: "Created via Admin Web Panel",
    }
    await saveVisit(newVisit)
    setCustomerName("")
    setCustomerId(0)
    setIsAddOpen(false)
  }

  const handleCompleteVisit = async (visit: Visit) => {
    await saveVisit({ ...visit, status: "Completed" })
    if (selectedVisit?.id === visit.id) {
      setSelectedVisit({ ...visit, status: "Completed" })
    }
  }

  const handleAddStopToTrip = async () => {
    if (!selectedVisit || !orderItemCode.trim() || !orderPieces) return
    const chosenSup = suppliers.find((s) => s.id === orderSupplierId) || suppliers[0]
    if (!chosenSup) return

    const pcs = parseInt(orderPieces, 10) || 0
    const rt = parseFloat(orderRate) || 0
    const cs = parseInt(orderCaseSize, 10) || 24
    const csCount = orderCases !== "" ? parseInt(orderCases, 10) || 0 : (cs > 0 ? Math.floor(pcs / cs) : 0)
    const lsPcs = orderLoose !== "" ? parseInt(orderLoose, 10) || 0 : (cs > 0 ? pcs % cs : 0)
    const baseTotal = pcs * rt
    const gstAmt = (baseTotal * 5) / 100
    const grandTotal = baseTotal + gstAmt

    const newOrder: PurchaseEntry = {
      id: Date.now(),
      visitId: selectedVisit.id,
      orderNo: `HT-${Math.floor(1000 + Math.random() * 9000)}`,
      supplierId: chosenSup.id,
      supplierName: chosenSup.name,
      supplierType: chosenSup.type,
      itemCode: orderItemCode.trim().toUpperCase(),
      pieces: pcs,
      rate: rt,
      caseSize: cs,
      caseCount: csCount,
      loosePieces: lsPcs,
      pricePerPiece: rt,
      totalAmount: baseTotal,
      gstPercent: 5,
      gstAmount: gstAmt,
      grandTotalWithGst: grandTotal,
      deliveryStatus: "Pending",
      transporter: orderTransporter.trim() || undefined,
      paymentStatus: "Pending",
      paidAmount: 0,
      createdAt: Date.now(),
    }

    await savePurchaseEntry(newOrder)
    setIsAddOrderOpen(false)
    setOrderItemCode("")
    setOrderPieces("")
    setOrderCases("")
    setOrderLoose("")
    setOrderRate("")
    setOrderTransporter("")
  }

  // Visit entries breakdown
  const visitEntries = React.useMemo(() => {
    if (!selectedVisit) return []
    return entries.filter((e) => e.visitId === selectedVisit.id)
  }, [entries, selectedVisit])

  // Open Customer Day Report for a visit
  const handleOpenCustomerReport = (visit: Visit) => {
    const visitItems = entries.filter((e) => e.visitId === visit.id)
    const customer = customers.find((c) => c.id === visit.customerId || c.name === visit.customerName)
    const salesman = employees.find((e) => e.id === visit.employeeId || e.name === visit.employeeName)
    const linkedPackGroups = packGroups.filter((pg) => pg.visitId === visit.id)

    const reportData = {
      visit,
      customer,
      salesman,
      entries: visitItems,
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

  // Open Wholesaler Invoice for a specific purchase entry
  const handleOpenSupplierInvoice = (entry: PurchaseEntry, visit: Visit) => {
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
      {/* Top Action Bar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
            <span>Market Trips & Field Sourcing</span>
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Track buyer sourcing trips, assigned sales reps, and download consolidated Customer Day Reports.
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
              { value: "all", label: "All", count: visits.length },
              {
                value: "active",
                label: "Active",
                count: visits.filter((v) => v.status?.toLowerCase() === "active").length,
              },
              {
                value: "completed",
                label: "Completed",
                count: visits.filter((v) => v.status?.toLowerCase() === "completed").length,
              },
            ]}
          />

          <Button
            shape="pill"
            size="sm"
            onClick={() => setIsAddOpen(true)}
            className="h-8 shadow-sm font-semibold text-xs"
          >
            <Plus className="mr-1.5 h-3.5 w-3.5" />
            New Trip
          </Button>
        </div>
      </div>

      {/* Expandable Search Input */}
      {showSearch && (
        <div className="relative">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by buyer name, trip code, or agent name..."
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
              Showing trips handled by:{" "}
              <strong className="text-zinc-900 dark:text-zinc-100">{activeEmployee.name}</strong>{" "}
              ({activeEmployee.role})
            </span>
          </div>
          <button
            onClick={() => setSelectedEmployeeId("all")}
            className="text-[11px] font-semibold text-red-600 dark:text-red-400 hover:underline"
          >
            Show All Trips
          </button>
        </div>
      )}

      {/* Trips Table / Cards */}
      <Card className="rounded-2xl overflow-hidden border border-zinc-200/80 dark:border-zinc-800 shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-zinc-200/80 bg-zinc-50/70 font-semibold text-muted-foreground dark:border-zinc-800 dark:bg-zinc-900/50">
              <tr>
                <th className="py-3.5 pl-6 pr-3">Trip Code</th>
                <th className="px-3 py-3.5">Customer / Buyer</th>
                <th className="px-3 py-3.5">Date</th>
                <th className="px-3 py-3.5">Assigned Agent</th>
                <th className="px-3 py-3.5">Orders Booked</th>
                <th className="px-3 py-3.5">Status</th>
                <th className="py-3.5 pl-3 pr-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
              {filteredVisits.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-xs text-muted-foreground">
                    No market trips found {activeEmployee ? `for ${activeEmployee.name}` : ""}.
                  </td>
                </tr>
              ) : (
                filteredVisits.map((visit) => {
                  const visitItems = entries.filter((e) => e.visitId === visit.id)
                  const stopCount = visitItems.length
                  const isOngoing = visit.status?.toLowerCase() === "active"

                  return (
                    <tr
                      key={visit.id}
                      className="hover:bg-zinc-50/50 dark:hover:bg-zinc-900/50 transition-colors"
                    >
                      <td className="py-4 pl-6 pr-3 font-semibold text-zinc-900 dark:text-zinc-100 font-mono">
                        {visit.visitCode}
                      </td>
                      <td className="px-3 py-4 font-medium text-zinc-900 dark:text-zinc-100">
                        {visit.customerName}
                      </td>
                      <td className="px-3 py-4 text-muted-foreground">
                        {formatDate(visit.date)}
                      </td>
                      <td className="px-3 py-4">
                        <div className="flex items-center gap-1.5">
                          <span className="h-2 w-2 rounded-full bg-zinc-400" />
                          <strong className="text-zinc-800 dark:text-zinc-200">
                            {visit.employeeName}
                          </strong>
                        </div>
                      </td>
                      <td className="px-3 py-4">
                        <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                          {stopCount}
                        </span>{" "}
                        orders
                      </td>
                      <td className="px-3 py-4">
                        <Badge variant={isOngoing ? "success" : "secondary"}>
                          {visit.status}
                        </Badge>
                      </td>
                      <td className="py-4 pl-3 pr-6 text-right space-x-1.5">
                        {/* Customer Day Report (PDF) */}
                        <Button
                          variant="outline"
                          size="sm"
                          shape="pill"
                          onClick={() => handleOpenCustomerReport(visit)}
                          className="h-7 text-[11px] px-2.5 font-medium text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-900/50 hover:bg-blue-50 dark:hover:bg-blue-950/40"
                          title="Generate Customer Day Report PDF"
                        >
                          <FileText className="mr-1 h-3 w-3" />
                          Report PDF
                        </Button>

                        {/* View Details */}
                        <Button
                          variant="outline"
                          size="sm"
                          shape="pill"
                          onClick={() => setSelectedVisit(visit)}
                          className="h-7 text-[11px] px-2.5 font-medium"
                        >
                          <Eye className="mr-1 h-3 w-3" />
                          Details
                        </Button>

                        {isOngoing && (
                          <Button
                            variant="secondary"
                            size="sm"
                            shape="pill"
                            onClick={() => handleCompleteVisit(visit)}
                            className="h-7 text-[11px] px-2.5 font-medium"
                          >
                            Done
                          </Button>
                        )}
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Trip Detail Dialog */}
      {selectedVisit && (
        <Dialog
          open={!!selectedVisit}
          onOpenChange={(open) => !open && setSelectedVisit(null)}
          title={`Trip ${selectedVisit.visitCode} — ${selectedVisit.customerName}`}
          description={`Date: ${formatDate(selectedVisit.date)} • Assigned Agent: ${selectedVisit.employeeName}`}
          className="max-w-2xl"
        >
          <div className="space-y-4 pt-2">
            <div className="flex flex-wrap items-center justify-between border-b border-zinc-100 dark:border-zinc-800 pb-3 text-xs gap-3">
              <div>
                <span className="text-muted-foreground">Buyer / Firm</span>
                <p className="font-bold text-zinc-900 dark:text-zinc-100">
                  {selectedVisit.customerName}
                </p>
              </div>
              <div>
                <span className="text-muted-foreground">Field Agent</span>
                <p className="font-bold text-zinc-900 dark:text-zinc-100">
                  {selectedVisit.employeeName}
                </p>
              </div>
              <div>
                <span className="text-muted-foreground">Status</span>
                <Badge
                  variant={
                    selectedVisit.status?.toLowerCase() === "active" ? "success" : "secondary"
                  }
                  className="mt-0.5 block"
                >
                  {selectedVisit.status}
                </Badge>
              </div>
              <div>
                <Button
                  size="sm"
                  shape="pill"
                  onClick={() => handleOpenCustomerReport(selectedVisit)}
                  className="text-xs font-semibold gap-1.5 shadow-sm"
                >
                  <Printer className="h-3.5 w-3.5" />
                  <span>Customer Day Report</span>
                </Button>
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                  Orders Booked During this Trip ({visitEntries.length})
                </h4>
                <div className="flex items-center gap-2">
                  <span className="text-[11px] text-muted-foreground">
                    Total Billed: ₹{formatInr(
                      visitEntries.reduce(
                        (sum, e) => sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
                        0
                      )
                    )}
                  </span>
                  {selectedVisit.status?.toLowerCase() === "active" && (
                    <Button
                      size="sm"
                      shape="pill"
                      onClick={() => {
                        setOrderSupplierId(suppliers[0]?.id || 0)
                        setOrderCaseSize(suppliers[0]?.defaultCaseSize ? String(suppliers[0].defaultCaseSize) : "24")
                        setOrderItemCode("")
                        setOrderPieces("")
                        setOrderCases("")
                        setOrderLoose("")
                        setOrderRate("")
                        setOrderTransporter("")
                        setIsAddOrderOpen(true)
                      }}
                      className="h-6 text-[10px] px-2 font-semibold bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm gap-1"
                    >
                      <Plus className="h-3 w-3" />
                      Add Stop
                    </Button>
                  )}
                </div>
              </div>

              {visitEntries.length === 0 ? (
                <p className="text-xs text-muted-foreground py-6 text-center">
                  No purchase entries booked during this trip yet.
                </p>
              ) : (
                <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                  {visitEntries.map((e) => {
                    const bill =
                      Number(e.grandTotalWithGst) ||
                      (Number(e.totalAmount) + Number(e.gstAmount)) ||
                      0
                    const paid = Number(e.paidAmount) || 0
                    const due = Math.max(0, bill - paid)

                    return (
                      <div
                        key={e.id}
                        className="flex items-center justify-between p-3 rounded-xl border border-zinc-200 dark:border-zinc-800 text-xs bg-zinc-50/50 dark:bg-zinc-900/40"
                      >
                        <div>
                          <p className="font-bold text-zinc-900 dark:text-zinc-100">
                            #{e.orderNo} • {e.itemCode}
                          </p>
                          <p className="text-[11px] text-muted-foreground">
                            🏭 {e.supplierName} • {e.pieces} pcs ({e.caseCount} cases, {e.loosePieces} loose)
                          </p>
                          <div className="flex items-center gap-2 mt-1">
                            <span className="text-emerald-600 font-semibold text-[10px]">
                              Done: ₹{formatInr(paid)}
                            </span>
                            <span className="text-zinc-400 text-[10px]">•</span>
                            <span className={due > 0 ? "text-red-600 font-semibold text-[10px]" : "text-zinc-400 text-[10px]"}>
                              Due: ₹{formatInr(due)}
                            </span>
                          </div>
                        </div>
                        <div className="text-right flex flex-col items-end gap-1.5">
                          <p className="font-bold text-zinc-900 dark:text-zinc-100">
                            ₹{formatInr(bill)}
                          </p>
                          <div className="flex items-center gap-1.5">
                            <Badge variant={e.paymentStatus?.toLowerCase() === "paid" ? "success" : "outline"} className="text-[10px]">
                              {e.paymentStatus || "Unpaid"}
                            </Badge>
                            <Button
                              variant="ghost"
                              size="sm"
                              shape="pill"
                              onClick={() => handleOpenSupplierInvoice(e, selectedVisit)}
                              className="h-6 text-[10px] px-2 text-blue-600 hover:bg-blue-50 dark:text-blue-400 dark:hover:bg-blue-950/40"
                              title="Wholesaler Invoice PDF"
                            >
                              <FileText className="h-3 w-3 mr-0.5" />
                              Invoice
                            </Button>
                          </div>
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-zinc-100 dark:border-zinc-800">
              <Button
                variant="outline"
                shape="pill"
                size="sm"
                onClick={() => setSelectedVisit(null)}
              >
                Close
              </Button>
              {selectedVisit.status?.toLowerCase() === "active" && (
                <Button
                  shape="pill"
                  size="sm"
                  onClick={() => handleCompleteVisit(selectedVisit)}
                >
                  <CheckCircle2 className="mr-1.5 h-3.5 w-3.5" />
                  Complete Trip
                </Button>
              )}
            </div>
          </div>
        </Dialog>
      )}

      {/* New Trip Dialog */}
      <Dialog
        open={isAddOpen}
        onOpenChange={setIsAddOpen}
        title="Create New Market Trip"
        description="Assign a sales rep to visit a buyer and record purchases"
      >
        <div className="space-y-4 pt-2">
          <div>
            <label className="text-xs font-medium text-muted-foreground">
              Select or Enter Customer / Buyer
            </label>
            <select
              value={customerId}
              onChange={(e) => {
                const id = Number(e.target.value)
                setCustomerId(id)
                const found = customers.find((c) => c.id === id)
                if (found) setCustomerName(found.name)
              }}
              aria-label="Select Customer"
              className="mt-1.5 w-full rounded-lg border border-zinc-200 bg-white p-2 text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200"
            >
              <option value={0}>Choose registered customer...</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.marketArea || c.city || "Local"})
                </option>
              ))}
            </select>
            <Input
              value={customerName}
              onChange={(e) => setCustomerName(e.target.value)}
              placeholder="Or enter buyer / firm name directly"
              className="mt-2"
            />
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground">
              Assign Sales Agent / Rep
            </label>
            <select
              value={agentId}
              onChange={(e) => {
                const id = Number(e.target.value)
                setAgentId(id)
                const emp = employees.find((x) => x.id === id)
                if (emp) setAgentName(emp.name)
              }}
              aria-label="Select Sales Agent"
              className="mt-1.5 w-full rounded-lg border border-zinc-200 bg-white p-2 text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200"
            >
              {employees.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  👤 {emp.name} ({emp.role})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground">Trip Date</label>
            <Input
              type="date"
              value={visitDate}
              onChange={(e) => setVisitDate(e.target.value)}
              className="mt-1.5"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button
              variant="outline"
              shape="pill"
              size="sm"
              onClick={() => setIsAddOpen(false)}
            >
              Cancel
            </Button>
            <Button shape="pill" size="sm" onClick={handleCreateVisit}>
              Create Trip
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Add Stop / Order Modal */}
      {selectedVisit && (
        <Dialog
          open={isAddOrderOpen}
          onOpenChange={setIsAddOrderOpen}
          title={`Add Purchase Stop: ${selectedVisit.visitCode}`}
          description={`Customer: ${selectedVisit.customerName} • Agent: ${selectedVisit.employeeName}`}
        >
          <div className="space-y-3.5 pt-2 text-xs">
            <div>
              <label className="text-xs font-medium text-muted-foreground">
                Select Supplier / Wholesaler *
              </label>
              <select
                value={orderSupplierId}
                onChange={(e) => {
                  const id = Number(e.target.value)
                  setOrderSupplierId(id)
                  const sup = suppliers.find((s) => s.id === id)
                  if (sup && sup.defaultCaseSize) {
                    setOrderCaseSize(String(sup.defaultCaseSize))
                  }
                }}
                aria-label="Select Supplier"
                className="mt-1.5 w-full rounded-lg border border-zinc-200 bg-white p-2 text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200"
              >
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    🏭 {s.name} ({s.marketArea || s.city || "Local"})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-xs font-medium text-muted-foreground">Item / Style Code *</label>
              <Input
                value={orderItemCode}
                onChange={(e) => setOrderItemCode(e.target.value.toUpperCase())}
                placeholder="e.g. KURTI-102, JEANS-88, ABC"
                className="mt-1"
              />
            </div>

            <div className="grid grid-cols-2 gap-2.5">
              <div>
                <label className="text-xs font-medium text-muted-foreground">Total Pieces (Pc) *</label>
                <Input
                  type="number"
                  value={orderPieces}
                  onChange={(e) => setOrderPieces(e.target.value)}
                  placeholder="e.g. 75"
                  className="mt-1 font-semibold"
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground">Rate (₹/Pc) *</label>
                <Input
                  type="number"
                  value={orderRate}
                  onChange={(e) => setOrderRate(e.target.value)}
                  placeholder="e.g. 450"
                  className="mt-1"
                />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-2">
              <div>
                <label className="text-xs font-medium text-muted-foreground">Cases (Cs)</label>
                <Input
                  type="number"
                  value={orderCases}
                  onChange={(e) => setOrderCases(e.target.value)}
                  placeholder="e.g. 2"
                  className="mt-1"
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground">Loose (Pcs)</label>
                <Input
                  type="number"
                  value={orderLoose}
                  onChange={(e) => setOrderLoose(e.target.value)}
                  placeholder="e.g. 5"
                  className="mt-1"
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground">Case Size (Ref)</label>
                <Input
                  type="number"
                  value={orderCaseSize}
                  onChange={(e) => setOrderCaseSize(e.target.value)}
                  placeholder="24"
                  className="mt-1"
                />
              </div>
            </div>

            <div>
              <label className="text-xs font-medium text-muted-foreground">Transporter (Optional)</label>
              <Input
                value={orderTransporter}
                onChange={(e) => setOrderTransporter(e.target.value)}
                placeholder="e.g. V-Trans, Navata, SafeX"
                className="mt-1"
              />
            </div>

            {/* Live Calculation Preview */}
            <div className="rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/60 p-3">
              <div className="flex items-center justify-between text-xs font-bold text-zinc-900 dark:text-zinc-100">
                <span>Total: ₹{formatInr((Number(orderPieces) || 0) * (Number(orderRate) || 0))}</span>
                <span>
                  {orderCases ? `${orderCases} Cases` : ""}{" "}
                  {orderLoose ? `+ ${orderLoose} Loose` : ""}
                  {!orderCases && !orderLoose && `${Number(orderPieces) || 0} Pcs`}
                </span>
              </div>
              {Number(orderLoose) > 0 && (
                <p className="text-[11px] text-amber-600 dark:text-amber-400 mt-1">
                  ⚠️ {orderLoose} loose pieces remaining. Can be packed with other bills in Mixed Pack.
                </p>
              )}
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="outline"
                shape="pill"
                size="sm"
                onClick={() => setIsAddOrderOpen(false)}
              >
                Cancel
              </Button>
              <Button
                shape="pill"
                size="sm"
                disabled={!orderItemCode.trim() || !orderPieces}
                onClick={handleAddStopToTrip}
              >
                Save Stop
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
