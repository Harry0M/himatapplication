import React, { useState } from "react"
import { Truck, Search, CheckCircle, Clock, PackageCheck } from "lucide-react"
import { useData } from "../context/DataContext"
import { formatDate } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Tabs } from "../components/ui/Tabs"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { PurchaseEntry } from "../types"

export function DeliveriesView() {
  const { entries, visits, updateDelivery } = useData()
  const [filter, setFilter] = useState<string>("all")
  const [search, setSearch] = useState<string>("")

  // Dialog state
  const [selectedEntry, setSelectedEntry] = useState<PurchaseEntry | null>(null)
  const [deliveryStatus, setDeliveryStatus] = useState<string>("Dispatched")
  const [transporter, setTransporter] = useState<string>("")
  const [lrNo, setLrNo] = useState<string>("")

  const visitMap = React.useMemo(() => {
    return new Map(visits.map((v) => [v.id, v.customerName]))
  }, [visits])

  const q = search.trim().toLowerCase()
  const filteredEntries = entries.filter((e) => {
    const cust = visitMap.get(e.visitId) || ""
    const matches =
      !q ||
      e.orderNo?.toLowerCase().includes(q) ||
      e.itemCode?.toLowerCase().includes(q) ||
      e.supplierName?.toLowerCase().includes(q) ||
      e.transporter?.toLowerCase().includes(q) ||
      cust.toLowerCase().includes(q)

    if (!matches) return false
    if (filter === "pending")
      return e.deliveryStatus?.toLowerCase() === "pending" || !e.deliveryStatus
    if (filter === "packed") return e.deliveryStatus?.toLowerCase() === "packed"
    if (filter === "dispatched") return e.deliveryStatus?.toLowerCase() === "dispatched"
    if (filter === "delivered") return e.deliveryStatus?.toLowerCase() === "delivered"
    return true
  })

  const handleOpenDialog = (entry: PurchaseEntry) => {
    setSelectedEntry(entry)
    setDeliveryStatus(entry.deliveryStatus || "Dispatched")
    setTransporter(entry.transporter || "")
    setLrNo(entry.lrNo || "")
  }

  const handleSaveDelivery = async () => {
    if (!selectedEntry) return
    await updateDelivery(selectedEntry.id, deliveryStatus, transporter, lrNo)
    setSelectedEntry(null)
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
            Deliveries & Dispatch Logistics
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Assign transport carriers, record LR/Bilty numbers, and mark consignments delivered.
          </p>
        </div>
        <Tabs
          value={filter}
          onValueChange={setFilter}
          options={[
            { value: "all", label: "All Consignments", count: entries.length },
            {
              value: "pending",
              label: "Pending",
              count: entries.filter(
                (e) => e.deliveryStatus?.toLowerCase() === "pending" || !e.deliveryStatus
              ).length,
            },
            {
              value: "dispatched",
              label: "In Transit",
              count: entries.filter((e) => e.deliveryStatus?.toLowerCase() === "dispatched").length,
            },
            {
              value: "delivered",
              label: "Delivered",
              count: entries.filter((e) => e.deliveryStatus?.toLowerCase() === "delivered").length,
            },
          ]}
        />
      </div>

      {/* Deliveries Table */}
      <Card className="rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-zinc-200/80 bg-zinc-50/70 font-semibold text-muted-foreground dark:border-zinc-800 dark:bg-zinc-900/50">
              <tr>
                <th className="py-3.5 pl-6 pr-3">Order / Item</th>
                <th className="px-3 py-3.5">Customer / Destination</th>
                <th className="px-3 py-3.5">Supplier Origin</th>
                <th className="px-3 py-3.5">Volume</th>
                <th className="px-3 py-3.5">Transporter & LR</th>
                <th className="px-3 py-3.5">Status</th>
                <th className="py-3.5 pl-3 pr-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
              {filteredEntries.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-xs text-muted-foreground">
                    No delivery records found matching filter.
                  </td>
                </tr>
              ) : (
                filteredEntries.map((entry) => {
                  const cust = visitMap.get(entry.visitId) || "Customer"
                  const isDelivered = entry.deliveryStatus?.toLowerCase() === "delivered"

                  return (
                    <tr
                      key={entry.id}
                      className="hover:bg-zinc-50/50 dark:hover:bg-zinc-900/50 transition-colors"
                    >
                      <td className="py-4 pl-6 pr-3 font-semibold text-zinc-900 dark:text-zinc-100">
                        #{entry.orderNo}
                        <p className="text-[11px] font-normal text-muted-foreground">
                          {entry.itemCode}
                        </p>
                      </td>
                      <td className="px-3 py-4 font-medium text-zinc-900 dark:text-zinc-100">
                        {cust}
                      </td>
                      <td className="px-3 py-4 text-muted-foreground">{entry.supplierName}</td>
                      <td className="px-3 py-4">
                        <span className="font-bold text-zinc-900 dark:text-zinc-100">
                          {entry.pieces} pcs
                        </span>
                        <p className="text-[10px] text-muted-foreground">
                          {entry.caseCount} cases {entry.loosePieces > 0 ? `(${entry.loosePieces} loose)` : ""}
                        </p>
                      </td>
                      <td className="px-3 py-4">
                        {entry.transporter ? (
                          <div>
                            <span className="font-medium text-zinc-900 dark:text-zinc-100">
                              🚚 {entry.transporter}
                            </span>
                            {entry.lrNo && (
                              <p className="text-[10px] text-muted-foreground">LR: {entry.lrNo}</p>
                            )}
                          </div>
                        ) : (
                          <span className="text-[11px] text-amber-600 dark:text-amber-400 font-medium">
                            ⚠️ Not assigned
                          </span>
                        )}
                      </td>
                      <td className="px-3 py-4">
                        <Badge variant={isDelivered ? "default" : "info"}>
                          {entry.deliveryStatus || "Pending"}
                        </Badge>
                      </td>
                      <td className="py-4 pl-3 pr-6 text-right">
                        <Button
                          size="sm"
                          shape="pill"
                          variant="outline"
                          onClick={() => handleOpenDialog(entry)}
                          className="h-7 text-[11px] px-3 font-medium"
                        >
                          Update
                        </Button>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Update Delivery Dialog */}
      <Dialog
        open={!!selectedEntry}
        onOpenChange={(open) => !open && setSelectedEntry(null)}
        title="Update Dispatch & Delivery"
        description={`Order #${selectedEntry?.orderNo} • ${selectedEntry?.itemCode}`}
      >
        <div className="space-y-4 pt-2">
          <div>
            <label className="text-xs font-medium text-muted-foreground">Status</label>
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
              placeholder="e.g. V-Trans, ARC, Rivigo"
              className="mt-1.5"
            />
          </div>
          <div>
            <label className="text-xs font-medium text-muted-foreground">LR / Bilty Number</label>
            <Input
              value={lrNo}
              onChange={(e) => setLrNo(e.target.value)}
              placeholder="e.g. LR-55441"
              className="mt-1.5"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button
              variant="outline"
              shape="pill"
              size="sm"
              onClick={() => setSelectedEntry(null)}
            >
              Cancel
            </Button>
            <Button shape="pill" size="sm" onClick={handleSaveDelivery}>
              Update Delivery
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  )
}
