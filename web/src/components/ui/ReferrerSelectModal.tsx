import React, { useState, useMemo } from "react"
import { Search, User, Store, Building2, Edit3, Check, X, Link, ArrowRight } from "lucide-react"
import { Dialog } from "./Dialog"
import { Button } from "./Button"
import { Input } from "./Input"
import { Customer, Supplier, Employee } from "../../types"

interface ReferrerSelectModalProps {
  value: string
  onChange: (value: string) => void
  employees?: Employee[]
  customers?: Customer[]
  suppliers?: Supplier[]
  label?: string
  placeholder?: string
}

interface ReferrerRecord {
  id: string
  title: string
  subtitle: string
  type: "staff" | "customer" | "supplier"
  typeLabel: string
  formattedValue: string
}

export function ReferrerSelectModal({
  value,
  onChange,
  employees = [],
  customers = [],
  suppliers = [],
  label = "Referred By (Entity Link)",
  placeholder = "Select Staff, Customer, Supplier or Broker..."
}: ReferrerSelectModalProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")
  const [activeTab, setActiveTab] = useState<"all" | "staff" | "customer" | "supplier" | "custom">("all")
  const [customInput, setCustomInput] = useState("")

  // Normalize all master lists into searchable records
  const allRecords = useMemo<ReferrerRecord[]>(() => {
    const list: ReferrerRecord[] = []

    employees.forEach((emp) => {
      list.push({
        id: `emp-${emp.id}`,
        title: emp.name,
        subtitle: `${emp.role || "Staff Agent"} • ${emp.phone || "No phone"}`,
        type: "staff",
        typeLabel: "Staff Agent",
        formattedValue: `Staff: ${emp.name}`
      })
    })

    customers.forEach((cust) => {
      const firm = cust.firmName || cust.name || "Unknown Customer"
      list.push({
        id: `cust-${cust.id}`,
        title: firm,
        subtitle: `${cust.city || "Ahmedabad"} • ${cust.phone || "No phone"}`,
        type: "customer",
        typeLabel: "Customer",
        formattedValue: `Customer: ${firm}`
      })
    })

    suppliers.forEach((sup) => {
      const firm = sup.firmName || sup.name || "Unknown Supplier"
      list.push({
        id: `sup-${sup.id}`,
        title: firm,
        subtitle: `${sup.type || "Mill/Supplier"} • ${sup.city || "Ahmedabad"}`,
        type: "supplier",
        typeLabel: "Supplier/Mill",
        formattedValue: `Supplier: ${firm}`
      })
    })

    return list
  }, [employees, customers, suppliers])

  // Filter based on activeTab and searchQuery
  const filteredRecords = useMemo(() => {
    const q = searchQuery.trim().toLowerCase()
    return allRecords.filter((rec) => {
      if (activeTab === "staff" && rec.type !== "staff") return false
      if (activeTab === "customer" && rec.type !== "customer") return false
      if (activeTab === "supplier" && rec.type !== "supplier") return false

      if (!q) return true
      return (
        rec.title.toLowerCase().includes(q) ||
        rec.subtitle.toLowerCase().includes(q) ||
        rec.typeLabel.toLowerCase().includes(q)
      )
    })
  }, [allRecords, activeTab, searchQuery])

  // Detect current value style and badge
  const entityMeta = useMemo(() => {
    if (!value) return null
    const lower = value.toLowerCase()
    if (lower.startsWith("staff:") || lower.includes("(agent)")) {
      return {
        label: "Staff Agent",
        color: "bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-950/40 dark:text-blue-300 dark:border-blue-800",
        icon: User
      }
    }
    if (lower.startsWith("customer:") || lower.includes("(customer)")) {
      return {
        label: "Customer Link",
        color: "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800",
        icon: Store
      }
    }
    if (lower.startsWith("supplier:") || lower.includes("(supplier)")) {
      return {
        label: "Supplier / Mill",
        color: "bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-950/40 dark:text-purple-300 dark:border-purple-800",
        icon: Building2
      }
    }
    return {
      label: "Broker / Introducer",
      color: "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800",
      icon: Edit3
    }
  }, [value])

  const handleSelect = (val: string) => {
    onChange(val)
    setIsOpen(false)
  }

  const handleApplyCustom = () => {
    if (!customInput.trim()) return
    const formatted = customInput.includes(":") ? customInput.trim() : `Broker: ${customInput.trim()}`
    onChange(formatted)
    setCustomInput("")
    setIsOpen(false)
  }

  const CurrentIcon = entityMeta ? entityMeta.icon : Link

  return (
    <div className="space-y-1.5">
      {label && (
        <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
          {label}
        </label>
      )}

      {/* Interactive Display Field */}
      <div
        onClick={() => setIsOpen(true)}
        className={`flex items-center justify-between p-2 rounded-lg border cursor-pointer transition-all hover:border-zinc-400 dark:hover:border-zinc-600 ${
          value
            ? "border-zinc-300 bg-zinc-50/70 dark:border-zinc-700 dark:bg-zinc-900/60"
            : "border-dashed border-zinc-300 bg-white dark:border-zinc-700 dark:bg-zinc-950"
        }`}
      >
        <div className="flex items-center gap-2.5 overflow-hidden">
          <div
            className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full border ${
              entityMeta ? entityMeta.color : "bg-zinc-100 text-zinc-500 dark:bg-zinc-800 dark:text-zinc-400"
            }`}
          >
            <CurrentIcon className="h-3.5 w-3.5" />
          </div>

          <div className="truncate">
            {value ? (
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-semibold text-zinc-900 dark:text-zinc-100 truncate">
                  {value}
                </span>
                {entityMeta && (
                  <span
                    className={`inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium border ${entityMeta.color}`}
                  >
                    {entityMeta.label}
                  </span>
                )}
              </div>
            ) : (
              <span className="text-xs text-zinc-400 dark:text-zinc-500">
                {placeholder}
              </span>
            )}
          </div>
        </div>

        <div className="flex items-center gap-1 shrink-0 ml-2" onClick={(e) => e.stopPropagation()}>
          {value && (
            <button
              type="button"
              onClick={() => onChange("")}
              className="p-1 rounded text-zinc-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors"
              title="Clear selection"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
          <button
            type="button"
            onClick={() => setIsOpen(true)}
            className="px-2 py-0.5 text-[11px] font-semibold text-zinc-700 bg-zinc-200/80 hover:bg-zinc-300 rounded dark:text-zinc-200 dark:bg-zinc-800 dark:hover:bg-zinc-700 transition-colors"
          >
            {value ? "Change" : "Select"}
          </button>
        </div>
      </div>

      {/* Modal Dialog */}
      <Dialog
        open={isOpen}
        onOpenChange={setIsOpen}
        title="Select Master Referrer"
        description="Link to Staff, Customer, Supplier, or Custom Introducer"
        className="max-w-md"
      >
        <div className="space-y-3">
          {/* Quick tabs */}
          <div className="flex items-center gap-1 overflow-x-auto pb-1 text-xs border-b border-zinc-200 dark:border-zinc-800">
            <button
              type="button"
              onClick={() => setActiveTab("all")}
              className={`px-2.5 py-1 rounded-md font-medium whitespace-nowrap transition-colors ${
                activeTab === "all"
                  ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900"
                  : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
              }`}
            >
              All ({allRecords.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("staff")}
              className={`px-2.5 py-1 rounded-md font-medium whitespace-nowrap transition-colors ${
                activeTab === "staff"
                  ? "bg-blue-600 text-white"
                  : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
              }`}
            >
              👔 Staff ({employees.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("customer")}
              className={`px-2.5 py-1 rounded-md font-medium whitespace-nowrap transition-colors ${
                activeTab === "customer"
                  ? "bg-emerald-600 text-white"
                  : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
              }`}
            >
              🏪 Customers ({customers.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("supplier")}
              className={`px-2.5 py-1 rounded-md font-medium whitespace-nowrap transition-colors ${
                activeTab === "supplier"
                  ? "bg-purple-600 text-white"
                  : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
              }`}
            >
              🏭 Suppliers ({suppliers.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("custom")}
              className={`px-2.5 py-1 rounded-md font-medium whitespace-nowrap transition-colors ${
                activeTab === "custom"
                  ? "bg-amber-600 text-white"
                  : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
              }`}
            >
              ✍️ Custom Broker
            </button>
          </div>

          {activeTab === "custom" ? (
            <div className="space-y-3 py-2">
              <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
                Type External Broker / Introducer Name:
              </label>
              <Input
                placeholder="e.g. Suresh Bhai (Ring Road Agent)"
                value={customInput}
                onChange={(e) => setCustomInput(e.target.value)}
                autoFocus
              />
              <div className="flex items-center justify-between pt-2">
                {value ? (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="text-red-600 border-red-200 hover:bg-red-50"
                    onClick={() => {
                      onChange("")
                      setIsOpen(false)
                    }}
                  >
                    Clear Current
                  </Button>
                ) : <span />}

                <Button
                  type="button"
                  size="sm"
                  onClick={handleApplyCustom}
                  disabled={!customInput.trim()}
                >
                  Apply Referrer
                </Button>
              </div>
            </div>
          ) : (
            <>
              {/* Search bar */}
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-zinc-400" />
                <Input
                  placeholder="Search name, firm, phone, city..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-9 text-xs h-9"
                  autoFocus
                />
                {searchQuery && (
                  <button
                    type="button"
                    onClick={() => setSearchQuery("")}
                    className="absolute right-2.5 top-2.5 text-zinc-400 hover:text-zinc-600"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>

              {/* Records List */}
              <div className="max-h-64 overflow-y-auto space-y-1 pr-1">
                {filteredRecords.length === 0 ? (
                  <div className="py-6 text-center space-y-2">
                    <p className="text-xs text-zinc-500">No matching records found.</p>
                    {searchQuery && (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => handleSelect(`Broker: ${searchQuery.trim()}`)}
                        className="text-xs"
                      >
                        Use &quot;{searchQuery.trim()}&quot; as Broker
                      </Button>
                    )}
                  </div>
                ) : (
                  filteredRecords.map((rec) => {
                    const isSelected =
                      value.toLowerCase() === rec.formattedValue.toLowerCase() ||
                      value.toLowerCase() === rec.title.toLowerCase()

                    const badgeClasses =
                      rec.type === "staff"
                        ? "bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-950/40 dark:text-blue-300 dark:border-blue-800"
                        : rec.type === "customer"
                        ? "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800"
                        : "bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-950/40 dark:text-purple-300 dark:border-purple-800"

                    const IconComp =
                      rec.type === "staff"
                        ? User
                        : rec.type === "customer"
                        ? Store
                        : Building2

                    return (
                      <div
                        key={rec.id}
                        onClick={() => handleSelect(rec.formattedValue)}
                        className={`flex items-center justify-between p-2 rounded-lg cursor-pointer transition-colors border ${
                          isSelected
                            ? "bg-zinc-100 border-zinc-400 dark:bg-zinc-800 dark:border-zinc-600"
                            : "bg-white border-transparent hover:bg-zinc-50 hover:border-zinc-200 dark:bg-zinc-950 dark:hover:bg-zinc-900"
                        }`}
                      >
                        <div className="flex items-center gap-2.5 min-w-0">
                          <div
                            className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full border ${badgeClasses}`}
                          >
                            <IconComp className="h-3.5 w-3.5" />
                          </div>
                          <div className="min-w-0">
                            <div className="flex items-center gap-1.5">
                              <span className="text-xs font-semibold text-zinc-900 dark:text-zinc-100 truncate">
                                {rec.title}
                              </span>
                              <span
                                className={`text-[10px] px-1 py-0.2 rounded border font-medium ${badgeClasses}`}
                              >
                                {rec.typeLabel}
                              </span>
                            </div>
                            <p className="text-[11px] text-zinc-500 dark:text-zinc-400 truncate">
                              {rec.subtitle}
                            </p>
                          </div>
                        </div>

                        {isSelected && (
                          <Check className="h-4 w-4 text-zinc-900 dark:text-zinc-100 shrink-0 ml-2" />
                        )}
                      </div>
                    )
                  })
                )}
              </div>

              {/* Bottom footer */}
              <div className="flex items-center justify-between pt-2 border-t border-zinc-200 dark:border-zinc-800 text-xs">
                {value ? (
                  <button
                    type="button"
                    onClick={() => {
                      onChange("")
                      setIsOpen(false)
                    }}
                    className="text-red-600 hover:text-red-700 font-medium"
                  >
                    Clear Selection
                  </button>
                ) : (
                  <span />
                )}

                <button
                  type="button"
                  onClick={() => setActiveTab("custom")}
                  className="flex items-center gap-1 text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100 font-medium"
                >
                  <Edit3 className="h-3.5 w-3.5" />
                  Type Custom Broker
                  <ArrowRight className="h-3 w-3" />
                </button>
              </div>
            </>
          )}
        </div>
      </Dialog>
    </div>
  )
}
