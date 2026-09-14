import React, { useState } from "react"
import {
  Trash2,
  RotateCcw,
  AlertTriangle,
  Search,
  Filter,
  CheckCircle2,
  Calendar,
  User,
  ShieldAlert,
  ArrowRight,
  Info
} from "lucide-react"
import { useData } from "../context/DataContext"
import { useAuth } from "../context/AuthContext"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { SoftDeletedItem } from "../types"
import { formatDate } from "../lib/utils"

export function DeletionsView() {
  const { deletedItems, confirmPermanentDelete, restoreDeletedItem } = useData()
  const { isAdmin } = useAuth()

  const [searchQuery, setSearchQuery] = useState("")
  const [selectedType, setSelectedType] = useState<string>("all")
  const [selectedItemForAction, setSelectedItemForAction] = useState<{
    item: SoftDeletedItem
    action: "confirm" | "restore"
  } | null>(null)
  const [isProcessing, setIsProcessing] = useState(false)
  const [feedbackMessage, setFeedbackMessage] = useState<{ type: "success" | "error"; text: string } | null>(null)

  // Filtered list
  const filteredItems = deletedItems.filter((item) => {
    const matchesType = selectedType === "all" || item.collection === selectedType
    const matchesSearch =
      searchQuery.trim() === "" ||
      item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (item.subtitle && item.subtitle.toLowerCase().includes(searchQuery.toLowerCase())) ||
      item.deletedBy.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (item.deletedByEmail && item.deletedByEmail.toLowerCase().includes(searchQuery.toLowerCase()))
    return matchesType && matchesSearch
  })

  const handleExecuteAction = async () => {
    if (!selectedItemForAction) return
    setIsProcessing(true)
    setFeedbackMessage(null)
    try {
      if (selectedItemForAction.action === "confirm") {
        await confirmPermanentDelete(selectedItemForAction.item.collection, selectedItemForAction.item.id)
        setFeedbackMessage({
          type: "success",
          text: `"${selectedItemForAction.item.title}" was permanently removed from the cloud database.`
        })
      } else {
        await restoreDeletedItem(selectedItemForAction.item.collection, selectedItemForAction.item.id)
        setFeedbackMessage({
          type: "success",
          text: `"${selectedItemForAction.item.title}" was successfully restored to active records!`
        })
      }
      setSelectedItemForAction(null)
    } catch (err: any) {
      console.error(err)
      setFeedbackMessage({
        type: "error",
        text: err?.message || "Action failed. Please check permissions."
      })
    } finally {
      setIsProcessing(false)
    }
  }

  const getEntityBadge = (type: string) => {
    switch (type) {
      case "Visit":
        return <Badge variant="outline" className="bg-zinc-100 text-zinc-900 border-zinc-200">Visit</Badge>
      case "Purchase Entry":
        return <Badge variant="outline" className="bg-zinc-900 text-white border-zinc-900">Order Entry</Badge>
      case "Customer":
        return <Badge variant="outline" className="bg-zinc-200 text-zinc-800 border-zinc-300">Customer</Badge>
      case "Supplier":
        return <Badge variant="outline" className="bg-zinc-800 text-zinc-100 border-zinc-700">Supplier</Badge>
      case "Product":
        return <Badge variant="outline" className="bg-zinc-100 text-zinc-800 border-zinc-300">Product</Badge>
      default:
        return <Badge variant="outline">{type}</Badge>
    }
  }

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-zinc-900 text-white p-6 rounded-2xl shadow-sm border border-zinc-800">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-2 rounded-xl bg-zinc-800 border border-zinc-700">
              <ShieldAlert className="w-5 h-5 text-zinc-200" />
            </span>
            <h1 className="text-xl font-bold tracking-tight">Deletion Approvals & Recovery</h1>
            <span className="ml-2 px-2.5 py-0.5 text-xs font-semibold rounded-full bg-white text-zinc-900">
              {deletedItems.length} Pending
            </span>
          </div>
          <p className="text-xs sm:text-sm text-zinc-400 mt-2 max-w-2xl">
            Entries and masters deleted on Android mobile devices by non-admin staff are automatically quarantined here.
            As an Administrator, you can either confirm permanent deletion or instantly undo and restore them to active views.
          </p>
        </div>
      </div>

      {feedbackMessage && (
        <div
          className={`p-4 rounded-xl flex items-center justify-between gap-3 text-sm font-medium border ${
            feedbackMessage.type === "success"
              ? "bg-zinc-50 border-zinc-200 text-zinc-900"
              : "bg-red-50 border-red-200 text-red-900"
          }`}
        >
          <div className="flex items-center gap-2">
            <Info className="w-4 h-4" />
            <span>{feedbackMessage.text}</span>
          </div>
          <button onClick={() => setFeedbackMessage(null)} className="text-xs underline hover:opacity-80">
            Dismiss
          </button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-3">
        {/* Type Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-1">
          {[
            { id: "all", label: "All Records", count: deletedItems.length },
            { id: "visits", label: "Visits", count: deletedItems.filter((i) => i.collection === "visits").length },
            {
              id: "purchase_entries",
              label: "Orders",
              count: deletedItems.filter((i) => i.collection === "purchase_entries").length,
            },
            {
              id: "customers",
              label: "Customers",
              count: deletedItems.filter((i) => i.collection === "customers").length,
            },
            {
              id: "suppliers",
              label: "Suppliers",
              count: deletedItems.filter((i) => i.collection === "suppliers").length,
            },
            { id: "products", label: "Products", count: deletedItems.filter((i) => i.collection === "products").length },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setSelectedType(tab.id)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-medium transition-all shrink-0 flex items-center gap-1.5 border ${
                selectedType === tab.id
                  ? "bg-zinc-900 text-white border-zinc-900 shadow-sm"
                  : "bg-white text-zinc-600 border-zinc-200 hover:border-zinc-300 hover:text-zinc-900"
              }`}
            >
              <span>{tab.label}</span>
              <span
                className={`px-1.5 py-0.2 rounded-full text-[10px] font-bold ${
                  selectedType === tab.id ? "bg-zinc-800 text-zinc-200" : "bg-zinc-100 text-zinc-600"
                }`}
              >
                {tab.count}
              </span>
            </button>
          ))}
        </div>

        {/* Search */}
        <div className="relative w-full md:w-72">
          <Search className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by title, staff or email..."
            className="pl-9 pr-4 py-2 text-xs rounded-full border-zinc-200 bg-white"
          />
        </div>
      </div>

      {/* Items List */}
      {filteredItems.length === 0 ? (
        <Card className="p-12 text-center border-dashed border-zinc-200 rounded-2xl bg-zinc-50/50">
          <div className="w-12 h-12 rounded-full bg-zinc-100 text-zinc-400 mx-auto flex items-center justify-center mb-3">
            <CheckCircle2 className="w-6 h-6 text-zinc-900" />
          </div>
          <h3 className="text-sm font-semibold text-zinc-900">No Pending Deletions</h3>
          <p className="text-xs text-zinc-500 mt-1 max-w-sm mx-auto">
            {searchQuery || selectedType !== "all"
              ? "No quarantined records match your filter criteria."
              : "All data is healthy and synchronized. No soft-deleted records require administrative review."}
          </p>
        </Card>
      ) : (
        <div className="space-y-3">
          {filteredItems.map((item) => (
            <Card
              key={`${item.collection}_${item.id}`}
              className="p-4 sm:p-5 rounded-2xl border-zinc-200 hover:border-zinc-300 transition-all bg-white shadow-sm"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                {/* Information Column */}
                <div className="space-y-1.5 flex-1 min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    {getEntityBadge(item.entityType)}
                    <span className="text-sm font-semibold text-zinc-900 truncate">{item.title}</span>
                    <span className="text-[11px] px-2 py-0.5 rounded-full bg-zinc-100 text-zinc-600 font-medium border border-zinc-200">
                      ID #{item.id}
                    </span>
                  </div>

                  {item.subtitle && <p className="text-xs text-zinc-500 line-clamp-1">{item.subtitle}</p>}

                  <div className="flex flex-wrap items-center gap-3 text-[11px] text-zinc-500 pt-1">
                    <span className="flex items-center gap-1">
                      <User className="w-3.5 h-3.5 text-zinc-400" />
                      <span>
                        Deleted by: <strong className="text-zinc-700">{item.deletedBy}</strong>{" "}
                        {item.deletedByRole ? `(${item.deletedByRole})` : ""}
                      </span>
                    </span>

                    {item.deletedByEmail && (
                      <span className="text-zinc-400 truncate max-w-[200px]">({item.deletedByEmail})</span>
                    )}

                    <span className="flex items-center gap-1">
                      <Calendar className="w-3.5 h-3.5 text-zinc-400" />
                      <span>{formatDate(item.deletedAt)}</span>
                    </span>
                  </div>
                </div>

                {/* Actions Column */}
                <div className="flex items-center gap-2 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-zinc-100">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setSelectedItemForAction({ item, action: "restore" })}
                    className="rounded-full text-xs font-medium border-zinc-300 text-zinc-800 hover:bg-zinc-100 hover:text-zinc-950 flex items-center gap-1.5"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Undo & Restore</span>
                  </Button>

                  <Button
                    variant="destructive"
                    size="sm"
                    onClick={() => setSelectedItemForAction({ item, action: "confirm" })}
                    className="rounded-full text-xs font-medium bg-zinc-900 text-white hover:bg-zinc-800 border border-zinc-900 flex items-center gap-1.5"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Confirm Delete</span>
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Confirmation Modal */}
      {selectedItemForAction && (
        <Dialog
          open={true}
          onOpenChange={(open) => !open && !isProcessing && setSelectedItemForAction(null)}
          title={selectedItemForAction.action === "confirm" ? "Confirm Permanent Hard Delete" : "Restore Soft-Deleted Record"}
          className="max-w-md"
        >
          <div className="space-y-4 pt-2">
            <div className="flex items-start gap-3">
              <div
                className={`p-2.5 rounded-full shrink-0 ${
                  selectedItemForAction.action === "confirm" ? "bg-red-100 text-red-700" : "bg-zinc-100 text-zinc-900"
                }`}
              >
                {selectedItemForAction.action === "confirm" ? (
                  <AlertTriangle className="w-5 h-5" />
                ) : (
                  <RotateCcw className="w-5 h-5" />
                )}
              </div>
              <div className="space-y-1">
                <h4 className="text-sm font-semibold text-zinc-900">
                  {selectedItemForAction.action === "confirm"
                    ? "Are you sure you want to permanently delete this?"
                    : "Restore this record back to active use?"}
                </h4>
                <p className="text-xs text-zinc-500 leading-relaxed">
                  {selectedItemForAction.action === "confirm"
                    ? "This will remove the record completely from Firebase Realtime Database. Once permanently deleted, it cannot be recovered."
                    : "This will remove the deleted flag and immediately restore this record across all Android mobile devices and Admin web reports."}
                </p>
              </div>
            </div>

            <div className="p-3 bg-zinc-50 rounded-xl border border-zinc-200 text-xs space-y-1">
              <div>
                <span className="text-zinc-500">Record:</span>{" "}
                <strong className="text-zinc-800">{selectedItemForAction.item.title}</strong>
              </div>
              <div>
                <span className="text-zinc-500">Deleted by:</span>{" "}
                <span className="text-zinc-700">
                  {selectedItemForAction.item.deletedBy} ({selectedItemForAction.item.deletedByRole || "Staff"})
                </span>
              </div>
              <div>
                <span className="text-zinc-500">Timestamp:</span>{" "}
                <span className="text-zinc-700">{formatDate(selectedItemForAction.item.deletedAt)}</span>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <Button
                variant="outline"
                size="sm"
                disabled={isProcessing}
                onClick={() => setSelectedItemForAction(null)}
                className="rounded-full text-xs"
              >
                Cancel
              </Button>

              <Button
                size="sm"
                disabled={isProcessing}
                onClick={handleExecuteAction}
                className={`rounded-full text-xs font-semibold text-white ${
                  selectedItemForAction.action === "confirm" ? "bg-red-600 hover:bg-red-700" : "bg-zinc-900 hover:bg-zinc-800"
                }`}
              >
                {isProcessing
                  ? "Processing..."
                  : selectedItemForAction.action === "confirm"
                  ? "Yes, Permanently Delete"
                  : "Yes, Restore Record"}
              </Button>
            </div>
          </div>
        </Dialog>
      )}
    </div>
  )
}
