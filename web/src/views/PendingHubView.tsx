import React, { useState } from "react"
import {
  IndianRupee,
  Truck,
  MapPin,
  Package,
  CheckCircle2,
  Search,
  Plus,
  Trash2,
  X,
  Layers
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatInr, formatDate } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Tabs } from "../components/ui/Tabs"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { PurchaseEntry, PackGroup } from "../types"

export function PendingHubView() {
  const {
    visits,
    entries,
    packGroups,
    pendingPaymentsCount,
    totalPendingDues,
    pendingDeliveriesCount,
    activeTripsCount,
    looseEntriesCount,
    totalLoosePieces,
    updatePayment,
    updateDelivery,
    createPackGroup,
    deletePackGroup,
  } = useData()

  const [filterTab, setFilterTab] = useState<string>("all")
  const [search, setSearch] = useState<string>("")
  const [showSearch, setShowSearch] = useState<boolean>(false)

  // Payment Dialog states
  const [paymentEntry, setPaymentEntry] = useState<PurchaseEntry | null>(null)
  const [paymentAmount, setPaymentAmount] = useState<string>("")
  const [paymentMode, setPaymentMode] = useState<string>("Cash")
  const [paymentStatus, setPaymentStatus] = useState<string>("Paid")
  const [paymentRemarks, setPaymentRemarks] = useState<string>("")

  // Delivery Dialog states
  const [deliveryEntry, setDeliveryEntry] = useState<PurchaseEntry | null>(null)
  const [deliveryStatus, setDeliveryStatus] = useState<string>("Delivered")
  const [transporter, setTransporter] = useState<string>("")
  const [lrNo, setLrNo] = useState<string>("")

  // Mixed Packing Dialog states
  const [isPackDialogOpen, setIsPackDialogOpen] = useState<boolean>(false)
  const [selectedEntryIds, setSelectedEntryIds] = useState<number[]>([])
  const [targetCaseSize, setTargetCaseSize] = useState<number>(24)
  const [packCustomNote, setPackCustomNote] = useState<string>("")

  // Packed Entry IDs
  const packedEntryIds = React.useMemo(() => {
    const set = new Set<number>()
    packGroups.forEach((g) => {
      if (g.linkedEntryIds) {
        g.linkedEntryIds.split(",").forEach((idStr) => {
          const num = Number(idStr.trim())
          if (!isNaN(num) && num > 0) {
            set.add(num)
          }
        })
      }
    })
    return set
  }, [packGroups])

  // 1. Pending Payments
  const pendingPayments = React.useMemo(() => {
    return entries.filter(
      (e) =>
        e.paymentStatus?.toLowerCase() !== "paid" &&
        e.paymentStatus?.toLowerCase() !== "received"
    )
  }, [entries])

  // 2. Pending Deliveries
  const pendingDeliveries = React.useMemo(() => {
    return entries.filter((e) => e.deliveryStatus?.toLowerCase() !== "delivered")
  }, [entries])

  // 3. Active Trips
  const activeTrips = React.useMemo(() => {
    return visits.filter((v) => v.status?.toLowerCase() === "active")
  }, [visits])

  // 4. Loose Entries (unpacked only)
  const looseEntries = React.useMemo(() => {
    return entries.filter((e) => {
      const loose = Number(e.loosePieces) || 0
      if (loose <= 0) return false
      const isPackGroupAssigned =
        e.packGroupId !== undefined && e.packGroupId !== null && Number(e.packGroupId) > 0
      const hasMixedNote =
        typeof e.mixedPackNote === "string" && e.mixedPackNote.trim().length > 0
      const isInPackGroup = packedEntryIds.has(Number(e.id))
      return !isPackGroupAssigned && !hasMixedNote && !isInPackGroup
    })
  }, [entries, packedEntryIds])

  // Filter with Search
  const q = search.trim().toLowerCase()
  const filteredPayments = pendingPayments.filter(
    (e) =>
      !q ||
      e.orderNo?.toLowerCase().includes(q) ||
      e.itemCode?.toLowerCase().includes(q) ||
      e.supplierName?.toLowerCase().includes(q)
  )
  const filteredDeliveries = pendingDeliveries.filter(
    (e) =>
      !q ||
      e.orderNo?.toLowerCase().includes(q) ||
      e.itemCode?.toLowerCase().includes(q) ||
      e.supplierName?.toLowerCase().includes(q) ||
      e.transporter?.toLowerCase().includes(q)
  )
  const filteredTrips = activeTrips.filter(
    (v) =>
      !q ||
      v.customerName?.toLowerCase().includes(q) ||
      v.visitCode?.toLowerCase().includes(q) ||
      v.employeeName?.toLowerCase().includes(q)
  )
  const filteredLoose = looseEntries.filter(
    (e) =>
      !q ||
      e.orderNo?.toLowerCase().includes(q) ||
      e.itemCode?.toLowerCase().includes(q) ||
      e.supplierName?.toLowerCase().includes(q)
  )

  const handleOpenPaymentDialog = (entry: PurchaseEntry) => {
    const totalBill = (Number(entry.totalAmount) || 0) + (Number(entry.gstAmount) || 0)
    const due = Math.max(0, totalBill - (Number(entry.paidAmount) || 0))
    setPaymentEntry(entry)
    setPaymentAmount(due.toString())
    setPaymentMode(entry.paymentMode || "Cash")
    setPaymentStatus("Paid")
    setPaymentRemarks("")
  }

  const handleSavePayment = async () => {
    if (!paymentEntry) return
    const paid = Number(paymentAmount) || 0
    await updatePayment(paymentEntry.id, paymentStatus, paymentMode, paid, paymentRemarks)
    setPaymentEntry(null)
  }

  const handleOpenDeliveryDialog = (entry: PurchaseEntry) => {
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

  const handleOpenPackModal = (entry?: PurchaseEntry) => {
    if (entry) {
      setSelectedEntryIds([entry.id])
    } else {
      setSelectedEntryIds(looseEntries.map((e) => e.id))
    }
    setTargetCaseSize(24)
    setPackCustomNote("")
    setIsPackDialogOpen(true)
  }

  const handleToggleEntrySelection = (id: number) => {
    setSelectedEntryIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    )
  }

  const handleCreateMixedPack = async () => {
    const selected = looseEntries.filter((e) => selectedEntryIds.includes(e.id))
    if (selected.length === 0) return
    const visitId = selected[0].visitId || 1
    await createPackGroup(visitId, selected, targetCaseSize, packCustomNote.trim() || undefined)
    setIsPackDialogOpen(false)
    setSelectedEntryIds([])
  }

  const totalActionsCount =
    pendingPayments.length + pendingDeliveries.length + activeTrips.length + looseEntries.length

  // Pack group calculation preview
  const selectedEntriesForPacking = looseEntries.filter((e) => selectedEntryIds.includes(e.id))
  const previewTotalLoose = selectedEntriesForPacking.reduce(
    (sum, e) => sum + (Number(e.loosePieces) || 0),
    0
  )
  const previewCases = targetCaseSize > 0 ? Math.floor(previewTotalLoose / targetCaseSize) : 1
  const previewRemainingLoose = targetCaseSize > 0 ? previewTotalLoose % targetCaseSize : 0

  return (
    <div className="space-y-6">
      {/* Header with Search & Tabs */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
            Pending Operations Hub
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            {totalActionsCount > 0
              ? `${totalActionsCount} operations require immediate attention`
              : "All tasks are cleared ✓"}
          </p>
        </div>
        <div className="flex items-center gap-2">
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

          <Tabs
            value={filterTab}
            onValueChange={setFilterTab}
            options={[
              { value: "all", label: "All", count: totalActionsCount },
              { value: "payments", label: "Payments", count: pendingPayments.length },
              { value: "deliveries", label: "Deliveries", count: pendingDeliveries.length },
              { value: "trips", label: "Trips", count: activeTrips.length },
              { value: "loose", label: "Loose Packs", count: looseEntries.length },
              { value: "packed", label: "Mixed Cases", count: packGroups.length },
            ]}
          />
        </div>
      </div>

      {/* Expandable Search Input */}
      {showSearch && (
        <div className="relative">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by order #, item code, supplier name..."
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

      {/* 4 Overview Mini Cards */}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div
          onClick={() => setFilterTab(filterTab === "payments" ? "all" : "payments")}
          className={`cursor-pointer rounded-2xl border p-4 transition-all ${
            filterTab === "payments"
              ? "border-zinc-900 bg-zinc-900 text-white dark:border-white dark:bg-white dark:text-black"
              : "border-zinc-200/80 bg-white hover:border-zinc-300 dark:border-zinc-800 dark:bg-zinc-950"
          }`}
        >
          <div className="flex items-center justify-between text-xs opacity-70">
            <span>Pending Dues</span>
            <IndianRupee className="h-4 w-4" />
          </div>
          <div className="mt-2 text-xl font-bold">₹{formatInr(totalPendingDues)}</div>
          <p className="text-[10px] mt-1 opacity-80">{pendingPayments.length} unpaid invoices</p>
        </div>

        <div
          onClick={() => setFilterTab(filterTab === "deliveries" ? "all" : "deliveries")}
          className={`cursor-pointer rounded-2xl border p-4 transition-all ${
            filterTab === "deliveries"
              ? "border-zinc-900 bg-zinc-900 text-white dark:border-white dark:bg-white dark:text-black"
              : "border-zinc-200/80 bg-white hover:border-zinc-300 dark:border-zinc-800 dark:bg-zinc-950"
          }`}
        >
          <div className="flex items-center justify-between text-xs opacity-70">
            <span>Deliveries</span>
            <Truck className="h-4 w-4" />
          </div>
          <div className="mt-2 text-xl font-bold">{pendingDeliveries.length}</div>
          <p className="text-[10px] mt-1 opacity-80">Orders in transit / packed</p>
        </div>

        <div
          onClick={() => setFilterTab(filterTab === "trips" ? "all" : "trips")}
          className={`cursor-pointer rounded-2xl border p-4 transition-all ${
            filterTab === "trips"
              ? "border-zinc-900 bg-zinc-900 text-white dark:border-white dark:bg-white dark:text-black"
              : "border-zinc-200/80 bg-white hover:border-zinc-300 dark:border-zinc-800 dark:bg-zinc-950"
          }`}
        >
          <div className="flex items-center justify-between text-xs opacity-70">
            <span>Active Trips</span>
            <MapPin className="h-4 w-4" />
          </div>
          <div className="mt-2 text-xl font-bold">{activeTrips.length}</div>
          <p className="text-[10px] mt-1 opacity-80">Market trips ongoing</p>
        </div>

        <div
          onClick={() => setFilterTab(filterTab === "loose" ? "all" : "loose")}
          className={`cursor-pointer rounded-2xl border p-4 transition-all ${
            filterTab === "loose"
              ? "border-zinc-900 bg-zinc-900 text-white dark:border-white dark:bg-white dark:text-black"
              : "border-zinc-200/80 bg-white hover:border-zinc-300 dark:border-zinc-800 dark:bg-zinc-950"
          }`}
        >
          <div className="flex items-center justify-between text-xs opacity-70">
            <span>Loose Packs</span>
            <Package className="h-4 w-4" />
          </div>
          <div className="mt-2 text-xl font-bold">{totalLoosePieces} pcs</div>
          <p className="text-[10px] mt-1 opacity-80">{looseEntries.length} orders need cases</p>
        </div>
      </div>

      {/* Main List Section */}
      <div className="space-y-6">
        {/* 1. Payments Section */}
        {(filterTab === "all" || filterTab === "payments") && filteredPayments.length > 0 && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                <IndianRupee className="h-3.5 w-3.5 text-amber-500" />
                <span>Pending Payments ({filteredPayments.length})</span>
              </h3>
            </div>
            <div className="grid gap-3 md:grid-cols-2">
              {filteredPayments.map((entry) => {
                const totalBill =
                  (Number(entry.totalAmount) || 0) + (Number(entry.gstAmount) || 0)
                const due = Math.max(0, totalBill - (Number(entry.paidAmount) || 0))
                return (
                  <Card key={entry.id} className="p-4 rounded-2xl">
                    <div className="flex items-start justify-between">
                      <div>
                        <p className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                          Order #{entry.orderNo} • {entry.itemCode}
                        </p>
                        <p className="text-[11px] text-muted-foreground mt-0.5">
                          🏭 {entry.supplierName} • {entry.pieces} pcs
                        </p>
                      </div>
                      <Badge
                        variant={
                          entry.paymentStatus?.toLowerCase() === "partial"
                            ? "warning"
                            : "destructive"
                        }
                      >
                        {entry.paymentStatus || "Unpaid"}
                      </Badge>
                    </div>

                    <div className="mt-3 flex items-center justify-between">
                      <div>
                        <span className="text-[10px] text-muted-foreground">Due Balance</span>
                        <div className="text-base font-bold text-red-600 dark:text-red-400">
                          ₹{formatInr(due)}
                        </div>
                        <span className="text-[10px] text-muted-foreground">
                          Total: ₹{formatInr(totalBill)} • Paid: ₹
                          {formatInr(Number(entry.paidAmount) || 0)}
                        </span>
                      </div>
                      <Button
                        size="sm"
                        shape="pill"
                        onClick={() => handleOpenPaymentDialog(entry)}
                        className="text-xs font-semibold shadow-sm"
                      >
                        Record Payment
                      </Button>
                    </div>
                  </Card>
                )
              })}
            </div>
          </div>
        )}

        {/* 2. Deliveries Section */}
        {(filterTab === "all" || filterTab === "deliveries") && filteredDeliveries.length > 0 && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                <Truck className="h-3.5 w-3.5 text-sky-500" />
                <span>Pending Deliveries ({filteredDeliveries.length})</span>
              </h3>
            </div>
            <div className="grid gap-3 md:grid-cols-2">
              {filteredDeliveries.map((entry) => (
                <Card key={entry.id} className="p-4 rounded-2xl">
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                        Order #{entry.orderNo} • {entry.itemCode}
                      </p>
                      <p className="text-[11px] text-muted-foreground mt-0.5">
                        🏭 {entry.supplierName} • {entry.pieces} pcs ({entry.caseCount} cases)
                      </p>
                    </div>
                    <Badge variant="info">{entry.deliveryStatus || "Pending"}</Badge>
                  </div>

                  <div className="mt-3 flex items-center justify-between">
                    <div>
                      <span className="text-[10px] text-muted-foreground">Transporter LR</span>
                      <p className="text-xs font-medium text-zinc-800 dark:text-zinc-200">
                        {entry.transporter ? `🚚 ${entry.transporter}` : "⚠️ No LR assigned"}
                      </p>
                      {entry.lrNo && (
                        <span className="text-[10px] text-muted-foreground">LR #{entry.lrNo}</span>
                      )}
                    </div>
                    <Button
                      size="sm"
                      shape="pill"
                      variant="outline"
                      onClick={() => handleOpenDeliveryDialog(entry)}
                      className="text-xs font-semibold"
                    >
                      Update Status
                    </Button>
                  </div>
                </Card>
              ))}
            </div>
          </div>
        )}

        {/* 3. Active Trips Section */}
        {(filterTab === "all" || filterTab === "trips") && filteredTrips.length > 0 && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                <MapPin className="h-3.5 w-3.5 text-blue-500" />
                <span>Active Market Trips ({filteredTrips.length})</span>
              </h3>
            </div>
            <div className="grid gap-3 md:grid-cols-2">
              {filteredTrips.map((trip) => (
                <Card key={trip.id} className="p-4 rounded-2xl">
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                        {trip.customerName}
                      </p>
                      <p className="text-[11px] text-muted-foreground mt-0.5">
                        Code: {trip.visitCode} • Date: {formatDate(trip.date)}
                      </p>
                    </div>
                    <Badge variant="success">Active Trip</Badge>
                  </div>
                  <div className="mt-3 flex items-center justify-between">
                    <p className="text-xs text-muted-foreground">
                      Assigned Agent:{" "}
                      <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                        {trip.employeeName}
                      </span>
                    </p>
                    <span className="text-[11px] font-medium text-emerald-600 dark:text-emerald-400">
                      In Progress
                    </span>
                  </div>
                </Card>
              ))}
            </div>
          </div>
        )}

        {/* 4. Loose Packs Section */}
        {(filterTab === "all" || filterTab === "loose") && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                <Package className="h-3.5 w-3.5 text-orange-500" />
                <span>Loose Pieces Needing Mixed Case ({filteredLoose.length})</span>
              </h3>
              {filteredLoose.length > 0 && (
                <Button
                  size="sm"
                  shape="pill"
                  onClick={() => handleOpenPackModal()}
                  className="h-7 text-xs font-semibold shadow-sm"
                >
                  <Layers className="h-3.5 w-3.5 mr-1" />
                  Pack Into Mixed Case
                </Button>
              )}
            </div>

            {filteredLoose.length === 0 ? (
              filterTab === "loose" && (
                <Card className="p-8 text-center rounded-2xl">
                  <CheckCircle2 className="h-6 w-6 text-emerald-500 mx-auto mb-2" />
                  <p className="text-xs font-bold text-zinc-800 dark:text-zinc-200">
                    All loose pieces are packed!
                  </p>
                  <p className="text-[11px] text-muted-foreground mt-0.5">
                    There are no unbundled loose garments pending in any order.
                  </p>
                </Card>
              )
            ) : (
              <div className="grid gap-3 md:grid-cols-2">
                {filteredLoose.map((entry) => (
                  <Card key={entry.id} className="p-4 rounded-2xl">
                    <div className="flex items-start justify-between">
                      <div>
                        <p className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                          Order #{entry.orderNo} • {entry.itemCode}
                        </p>
                        <p className="text-[11px] text-muted-foreground mt-0.5">
                          🏭 {entry.supplierName} • {entry.caseCount} full cases ({entry.caseSize}{" "}
                          pcs/case)
                        </p>
                      </div>
                      <Badge variant="warning">{entry.loosePieces} Loose Pcs</Badge>
                    </div>
                    <div className="mt-3 flex items-center justify-between">
                      <span className="text-xs text-muted-foreground">
                        Requires mixed packing to finalize shipment
                      </span>
                      <Button
                        size="sm"
                        variant="outline"
                        shape="pill"
                        onClick={() => handleOpenPackModal(entry)}
                        className="h-7 text-[11px] font-semibold"
                      >
                        Pack Loose
                      </Button>
                    </div>
                  </Card>
                ))}
              </div>
            )}
          </div>
        )}

        {/* 5. Mixed Cases (Packed Groups) Section */}
        {(filterTab === "all" || filterTab === "packed") && packGroups.length > 0 && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
                <Layers className="h-3.5 w-3.5 text-emerald-500" />
                <span>Packed Mixed Cases ({packGroups.length})</span>
              </h3>
            </div>
            <div className="grid gap-3 md:grid-cols-2">
              {packGroups.map((group) => (
                <Card key={group.id} className="p-4 rounded-2xl border-emerald-500/20 bg-emerald-500/5">
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold text-zinc-900 dark:text-zinc-100">
                          {group.packGroupCode || group.packCode}
                        </span>
                        <Badge variant="success">
                          {group.combinedPieces || group.totalPieces} pcs combined
                        </Badge>
                      </div>
                      <p className="text-xs text-zinc-700 dark:text-zinc-300 mt-1.5 font-medium">
                        {group.note}
                      </p>
                    </div>
                    <Button
                      size="sm"
                      variant="ghost"
                      shape="pill"
                      onClick={() => deletePackGroup(group.id)}
                      className="text-red-500 hover:bg-red-50 dark:hover:bg-red-950/30 h-7 w-7 p-0"
                      title="Unpack Mixed Case"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                  <div className="mt-3 flex items-center justify-between text-[11px] text-muted-foreground border-t border-zinc-200/60 dark:border-zinc-800 pt-2">
                    <span>
                      Resulting Cases:{" "}
                      <strong className="text-zinc-800 dark:text-zinc-200">
                        {group.resultingCases || group.totalCases || 1}
                      </strong>
                    </span>
                    <span>
                      Remaining Loose:{" "}
                      <strong className="text-zinc-800 dark:text-zinc-200">
                        {group.remainingLoose || 0} pcs
                      </strong>
                    </span>
                  </div>
                </Card>
              ))}
            </div>
          </div>
        )}

        {/* Empty State */}
        {totalActionsCount === 0 && (
          <Card className="py-12 text-center rounded-3xl">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-400 mb-3">
              <CheckCircle2 className="h-6 w-6" />
            </div>
            <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-100">
              All Operations Cleared!
            </h3>
            <p className="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
              There are no pending payments, deliveries, ongoing trips, or loose packs requiring
              attention.
            </p>
          </Card>
        )}
      </div>

      {/* Record Payment Dialog */}
      <Dialog
        open={!!paymentEntry}
        onOpenChange={(open) => !open && setPaymentEntry(null)}
        title="Record Payment"
        description={`Order #${paymentEntry?.orderNo} • ${paymentEntry?.supplierName}`}
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
            <label className="text-xs font-medium text-muted-foreground">Amount Paid (₹)</label>
            <Input
              type="number"
              value={paymentAmount}
              onChange={(e) => setPaymentAmount(e.target.value)}
              placeholder="Enter amount"
              className="mt-1.5"
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
            <label className="text-xs font-medium text-muted-foreground">Remarks / Reference</label>
            <Input
              value={paymentRemarks}
              onChange={(e) => setPaymentRemarks(e.target.value)}
              placeholder="Optional notes..."
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
        title="Update Delivery Status"
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

      {/* Pack Loose Pieces Dialog */}
      <Dialog
        open={isPackDialogOpen}
        onOpenChange={setIsPackDialogOpen}
        title="Create Mixed Case Pack"
        description="Bundle multiple loose garments into complete shipment cartons"
      >
        <div className="space-y-4 pt-2">
          <div>
            <label className="text-xs font-medium text-muted-foreground">
              Select Loose Orders to Combine
            </label>
            <div className="mt-2 space-y-2 max-h-48 overflow-y-auto pr-1">
              {looseEntries.map((entry) => {
                const isSelected = selectedEntryIds.includes(entry.id)
                return (
                  <div
                    key={entry.id}
                    onClick={() => handleToggleEntrySelection(entry.id)}
                    className={`flex items-center justify-between p-2.5 rounded-xl border cursor-pointer transition-all ${
                      isSelected
                        ? "border-zinc-900 bg-zinc-900/5 dark:border-white dark:bg-white/10"
                        : "border-zinc-200 dark:border-zinc-800 hover:border-zinc-300"
                    }`}
                  >
                    <div>
                      <p className="text-xs font-bold text-zinc-900 dark:text-zinc-100">
                        Order #{entry.orderNo} • {entry.itemCode}
                      </p>
                      <p className="text-[11px] text-muted-foreground">
                        🏭 {entry.supplierName}
                      </p>
                    </div>
                    <Badge variant={isSelected ? "default" : "outline"}>
                      {entry.loosePieces} pcs
                    </Badge>
                  </div>
                )
              })}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-medium text-muted-foreground">
                Target Carton Size (pcs)
              </label>
              <Input
                type="number"
                value={targetCaseSize}
                onChange={(e) => setTargetCaseSize(Number(e.target.value) || 24)}
                min={1}
                className="mt-1.5"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground">Packing Summary</label>
              <div className="mt-1.5 h-10 px-3 rounded-lg border border-zinc-200 dark:border-zinc-800 flex items-center justify-between text-xs">
                <span>Total: {previewTotalLoose} pcs</span>
                <span className="font-bold">
                  {previewCases} case{previewCases > 1 ? "s" : ""}
                  {previewRemainingLoose > 0 ? ` + ${previewRemainingLoose} loose` : ""}
                </span>
              </div>
            </div>
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground">
              Custom Packing Note (Optional)
            </label>
            <Input
              value={packCustomNote}
              onChange={(e) => setPackCustomNote(e.target.value)}
              placeholder="e.g. Mixed pack with Kurti & Shirting"
              className="mt-1.5"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button
              variant="outline"
              shape="pill"
              size="sm"
              onClick={() => setIsPackDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button
              shape="pill"
              size="sm"
              onClick={handleCreateMixedPack}
              disabled={selectedEntryIds.length === 0}
            >
              Create Mixed Pack ({previewTotalLoose} pcs)
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  )
}
