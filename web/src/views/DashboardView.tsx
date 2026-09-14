import React from "react"
import {
  IndianRupee,
  MapPin,
  Package,
  Truck,
  AlertCircle,
  ArrowRight,
  Clock,
  UserCheck,
  X,
  Filter
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatInr, formatDate } from "../lib/utils"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { ActiveTab } from "../components/layout/Sidebar"

interface DashboardViewProps {
  onNavigate: (tab: ActiveTab) => void
}

export function DashboardView({ onNavigate }: DashboardViewProps) {
  const {
    visits,
    entries,
    employees,
    selectedEmployeeId,
    setSelectedEmployeeId,
    pendingPaymentsCount,
    totalPendingDues,
    pendingDeliveriesCount,
    activeTripsCount,
    looseEntriesCount,
    totalLoosePieces
  } = useData()

  // Selected Employee object if filtered
  const activeEmployee = React.useMemo(() => {
    if (selectedEmployeeId === "all") return null
    return employees.find((e) => e.id === selectedEmployeeId) || null
  }, [selectedEmployeeId, employees])

  // Filtered visits
  const displayVisits = React.useMemo(() => {
    if (!activeEmployee) return visits
    return visits.filter(
      (v) =>
        Number(v.employeeId) === activeEmployee.id ||
        (v.employeeName &&
          v.employeeName.trim().toLowerCase() === activeEmployee.name.trim().toLowerCase())
    )
  }, [visits, activeEmployee])

  const visitIdsSet = React.useMemo(() => {
    return new Set(displayVisits.map((v) => v.id))
  }, [displayVisits])

  // Filtered entries linked to employee's visits
  const displayEntries = React.useMemo(() => {
    if (!activeEmployee) return entries
    return entries.filter((e) => visitIdsSet.has(Number(e.visitId)))
  }, [entries, activeEmployee, visitIdsSet])

  // Metrics for active display
  const displayDues = React.useMemo(() => {
    if (!activeEmployee) return totalPendingDues
    return displayEntries
      .filter(
        (e) =>
          e.paymentStatus?.toLowerCase() !== "paid" &&
          e.paymentStatus?.toLowerCase() !== "received"
      )
      .reduce((sum, e) => {
        const bill = (Number(e.totalAmount) || 0) + (Number(e.gstAmount) || 0)
        return sum + Math.max(0, bill - (Number(e.paidAmount) || 0))
      }, 0)
  }, [displayEntries, activeEmployee, totalPendingDues])

  const displayPendingPaymentsCount = React.useMemo(() => {
    if (!activeEmployee) return pendingPaymentsCount
    return displayEntries.filter(
      (e) =>
        e.paymentStatus?.toLowerCase() !== "paid" &&
        e.paymentStatus?.toLowerCase() !== "received"
    ).length
  }, [displayEntries, activeEmployee, pendingPaymentsCount])

  const displayActiveTrips = React.useMemo(() => {
    if (!activeEmployee) return activeTripsCount
    return displayVisits.filter((v) => v.status?.toLowerCase() === "active").length
  }, [displayVisits, activeEmployee, activeTripsCount])

  const displayDeliveriesCount = React.useMemo(() => {
    if (!activeEmployee) return pendingDeliveriesCount
    return displayEntries.filter((e) => e.deliveryStatus?.toLowerCase() !== "delivered").length
  }, [displayEntries, activeEmployee, pendingDeliveriesCount])

  const displayLoosePieces = React.useMemo(() => {
    if (!activeEmployee) return totalLoosePieces
    return displayEntries
      .filter((e) => {
        const loose = Number(e.loosePieces) || 0
        const isGrouped = e.packGroupId && Number(e.packGroupId) > 0
        const hasNote = e.mixedPackNote && e.mixedPackNote.trim() !== ""
        return loose > 0 && !isGrouped && !hasNote
      })
      .reduce((sum, e) => sum + (Number(e.loosePieces) || 0), 0)
  }, [displayEntries, activeEmployee, totalLoosePieces])

  const recentVisits = React.useMemo(() => {
    return [...displayVisits].sort((a, b) => (b.id || 0) - (a.id || 0)).slice(0, 5)
  }, [displayVisits])

  const recentEntries = React.useMemo(() => {
    return [...displayEntries].sort((a, b) => (b.id || 0) - (a.id || 0)).slice(0, 5)
  }, [displayEntries])

  return (
    <div className="space-y-6">
      {/* Top Welcome Banner */}
      <div className="flex flex-col justify-between gap-4 rounded-3xl border border-zinc-200/80 bg-gradient-to-r from-zinc-900 to-zinc-800 p-6 text-white dark:border-zinc-800 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-xl font-bold tracking-tight">Welcome to Himat SMS Admin</h2>
          <p className="mt-1 text-xs text-zinc-300">
            Real-time live operations tracker across market trips, supplier mills, and sales staff.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            shape="pill"
            size="sm"
            onClick={() => onNavigate("employees")}
            className="border border-white/20 bg-white/10 text-white backdrop-blur hover:bg-white/20 text-xs"
          >
            <UserCheck className="mr-1.5 h-3.5 w-3.5 text-blue-400" />
            <span>Staff Directory</span>
          </Button>
          <Button
            variant="default"
            shape="pill"
            size="sm"
            onClick={() => onNavigate("pending")}
            className="bg-white text-zinc-900 hover:bg-zinc-100 text-xs font-semibold shadow-sm"
          >
            <AlertCircle className="mr-1.5 h-3.5 w-3.5 text-amber-500" />
            <span>Pending Hub</span>
          </Button>
        </div>
      </div>

      {/* Employee Specific Filter Bar */}
      <div className="flex flex-col gap-3 rounded-2xl border border-zinc-200/80 bg-white p-3.5 dark:border-zinc-800 dark:bg-zinc-950 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2 text-xs">
          <div className="flex h-7 w-7 items-center justify-center rounded-full bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300">
            <Filter className="h-3.5 w-3.5" />
          </div>
          <div>
            <span className="font-semibold text-zinc-900 dark:text-zinc-100">
              Filter Operations by Staff / Agent:
            </span>
            {activeEmployee && (
              <span className="ml-2 text-muted-foreground text-[11px]">
                Showing work specifically for{" "}
                <strong className="text-zinc-900 dark:text-zinc-100">
                  {activeEmployee.name}
                </strong>{" "}
                ({activeEmployee.role})
              </span>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2">
          <select
            value={selectedEmployeeId === "all" ? "all" : String(selectedEmployeeId)}
            onChange={(e) => {
              const val = e.target.value
              setSelectedEmployeeId(val === "all" ? "all" : Number(val))
            }}
            aria-label="Filter by Staff Member"
            className="h-8 rounded-full border border-zinc-200 bg-zinc-50 px-3 text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200 focus:outline-none focus:ring-1 focus:ring-zinc-900"
          >
            <option value="all">👥 All Staff ({employees.length} Agents)</option>
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
              className="h-8 px-2.5 text-xs text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/40"
            >
              <X className="h-3.5 w-3.5 mr-1" />
              Clear Filter
            </Button>
          )}
        </div>
      </div>

      {/* Top 4 Metrics Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Metric 1: Pending Dues */}
        <Card
          className="cursor-pointer border-zinc-200/80 hover:border-zinc-400 dark:border-zinc-800 dark:hover:border-zinc-700"
          onClick={() => onNavigate("payments")}
        >
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">
              {activeEmployee ? `${activeEmployee.name}'s Dues` : "Pending Dues"}
            </CardTitle>
            <div className="flex h-7 w-7 items-center justify-center rounded-full bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300">
              <IndianRupee className="h-4 w-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">
              ₹{formatInr(displayDues)}
            </div>
            <p className="mt-1 text-[11px] text-muted-foreground flex items-center gap-1">
              <Clock className="h-3 w-3 text-amber-600" />
              <span>{displayPendingPaymentsCount} unpaid / partial invoices</span>
            </p>
          </CardContent>
        </Card>

        {/* Metric 2: Active Market Trips */}
        <Card
          className="cursor-pointer border-zinc-200/80 hover:border-zinc-400 dark:border-zinc-800 dark:hover:border-zinc-700"
          onClick={() => onNavigate("trips")}
        >
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">
              {activeEmployee ? `${activeEmployee.name}'s Trips` : "Active Trips"}
            </CardTitle>
            <div className="flex h-7 w-7 items-center justify-center rounded-full bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300">
              <MapPin className="h-4 w-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">
              {displayActiveTrips}
            </div>
            <p className="mt-1 text-[11px] text-muted-foreground">
              {displayVisits.length} total recorded trips
            </p>
          </CardContent>
        </Card>

        {/* Metric 3: Pending Deliveries */}
        <Card
          className="cursor-pointer border-zinc-200/80 hover:border-zinc-400 dark:border-zinc-800 dark:hover:border-zinc-700"
          onClick={() => onNavigate("deliveries")}
        >
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">
              Pending Deliveries
            </CardTitle>
            <div className="flex h-7 w-7 items-center justify-center rounded-full bg-sky-100 text-sky-800 dark:bg-sky-950 dark:text-sky-300">
              <Truck className="h-4 w-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">
              {displayDeliveriesCount}
            </div>
            <p className="mt-1 text-[11px] text-muted-foreground">
              In transit / awaiting dispatch
            </p>
          </CardContent>
        </Card>

        {/* Metric 4: Loose Packs Left */}
        <Card
          className="cursor-pointer border-zinc-200/80 hover:border-zinc-400 dark:border-zinc-800 dark:hover:border-zinc-700"
          onClick={() => onNavigate("pending")}
        >
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">
              Loose Pieces
            </CardTitle>
            <div className="flex h-7 w-7 items-center justify-center rounded-full bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300">
              <Package className="h-4 w-4" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">
              {displayLoosePieces} pcs
            </div>
            <p className="mt-1 text-[11px] text-muted-foreground">
              Unbundled loose garments
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Two-Column Section: Recent Trips & Recent Orders */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Recent Market Trips */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle className="text-sm font-semibold">
                {activeEmployee ? `${activeEmployee.name}'s Recent Trips` : "Recent Market Trips"}
              </CardTitle>
              <CardDescription className="text-xs">Live buyer sourcing visits</CardDescription>
            </div>
            <Button
              variant="outline"
              size="sm"
              shape="pill"
              onClick={() => onNavigate("trips")}
              className="text-xs"
            >
              View All
            </Button>
          </CardHeader>
          <CardContent>
            {recentVisits.length === 0 ? (
              <div className="py-8 text-center text-xs text-muted-foreground">
                No trips recorded {activeEmployee ? `for ${activeEmployee.name}` : "yet"}.
              </div>
            ) : (
              <div className="divide-y divide-zinc-100 dark:divide-zinc-800">
                {recentVisits.map((visit) => (
                  <div
                    key={visit.id}
                    className="flex items-center justify-between py-3 hover:bg-zinc-50/50 dark:hover:bg-zinc-900/50 rounded-xl px-2 transition-colors"
                  >
                    <div>
                      <p className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">
                        {visit.customerName}
                      </p>
                      <p className="text-[11px] text-muted-foreground">
                        {visit.visitCode} • {formatDate(visit.date)} • Agent:{" "}
                        <strong className="text-zinc-800 dark:text-zinc-200">
                          {visit.employeeName}
                        </strong>
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge
                        variant={
                          visit.status?.toLowerCase() === "active"
                            ? "success"
                            : visit.status?.toLowerCase() === "completed"
                            ? "default"
                            : "secondary"
                        }
                      >
                        {visit.status}
                      </Badge>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Recent Orders & Invoices */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle className="text-sm font-semibold">
                {activeEmployee ? `${activeEmployee.name}'s Orders` : "Recent Orders & Bills"}
              </CardTitle>
              <CardDescription className="text-xs">Latest supplier purchases</CardDescription>
            </div>
            <Button
              variant="outline"
              size="sm"
              shape="pill"
              onClick={() => onNavigate("orders")}
              className="text-xs"
            >
              View All
            </Button>
          </CardHeader>
          <CardContent>
            {recentEntries.length === 0 ? (
              <div className="py-8 text-center text-xs text-muted-foreground">
                No orders recorded {activeEmployee ? `for ${activeEmployee.name}` : "yet"}.
              </div>
            ) : (
              <div className="divide-y divide-zinc-100 dark:divide-zinc-800">
                {recentEntries.map((entry) => {
                  const bill =
                    (Number(entry.totalAmount) || 0) + (Number(entry.gstAmount) || 0)
                  return (
                    <div
                      key={entry.id}
                      className="flex items-center justify-between py-3 hover:bg-zinc-50/50 dark:hover:bg-zinc-900/50 rounded-xl px-2 transition-colors"
                    >
                      <div>
                        <p className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">
                          Order #{entry.orderNo} • {entry.itemCode}
                        </p>
                        <p className="text-[11px] text-muted-foreground">
                          🏭 {entry.supplierName} • {entry.pieces} pcs
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                          ₹{formatInr(bill)}
                        </p>
                        <span className="text-[10px] text-muted-foreground">
                          {entry.deliveryStatus || "Pending"}
                        </span>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
