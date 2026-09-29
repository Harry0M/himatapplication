import { useState, useMemo } from "react"
import { Search, User, Store, Building2, Edit3, Check, X, Link, ArrowRight, Handshake } from "lucide-react"
import type { LucideIcon } from "lucide-react"
import { Dialog } from "./Dialog"
import { Button } from "./Button"
import { Input } from "./Input"
import { Customer, Supplier, Employee } from "../../types"
import { formatReferrer, isAgentRole, parseReferrer, ReferrerSelection, ReferrerTypes } from "../../lib/domain"

interface ReferrerSelectModalProps {
  /** Display string, e.g. "Customer: Balaji Sarees" */
  value: string
  /** Called with the display string (kept for older callers) */
  onChange: (value: string) => void
  /** Called with the structured pick (type + id). null when cleared. Broker picks have id 0. */
  onSelect?: (selection: ReferrerSelection | null) => void
  /** Currently linked id, so the right row is ticked even if the name changed */
  selectedType?: string
  selectedId?: number
  /** Staff list. Sub Agents found here are moved to the Agents tab automatically. */
  employees?: Employee[]
  subAgents?: Employee[]
  customers?: Customer[]
  suppliers?: Supplier[]
  /** Hide the record being edited so nobody refers themselves */
  excludeType?: string
  excludeId?: number
  label?: string
  placeholder?: string
}

type RecordType = "staff" | "agent" | "customer" | "supplier"

interface ReferrerRecord {
  key: string
  id: number
  title: string
  subtitle: string
  type: RecordType
  referrerType: string
  typeLabel: string
  formattedValue: string
  /** Everything the search box matches: names, phones, city, market, brand, GST */
  searchText: string
}

const joinSearch = (...parts: Array<string | number | undefined | null>) =>
  parts.filter((p) => p !== undefined && p !== null && String(p).trim() !== "").join(" ").toLowerCase()

const TYPE_STYLE: Record<RecordType | "broker", { badge: string; icon: LucideIcon; label: string }> = {
  staff: {
    badge: "bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-950/40 dark:text-blue-300 dark:border-blue-800",
    icon: User,
    label: "Staff",
  },
  agent: {
    badge: "bg-teal-50 text-teal-700 border-teal-200 dark:bg-teal-950/40 dark:text-teal-300 dark:border-teal-800",
    icon: Handshake,
    label: "Sub Agent",
  },
  customer: {
    badge: "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800",
    icon: Store,
    label: "Customer",
  },
  supplier: {
    badge: "bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-950/40 dark:text-purple-300 dark:border-purple-800",
    icon: Building2,
    label: "Supplier / Mill",
  },
  broker: {
    badge: "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800",
    icon: Edit3,
    label: "Broker",
  },
}

const REFERRER_TYPE_FOR: Record<RecordType, string> = {
  staff: ReferrerTypes.STAFF,
  agent: ReferrerTypes.AGENT,
  customer: ReferrerTypes.CUSTOMER,
  supplier: ReferrerTypes.SUPPLIER,
}

export function ReferrerSelectModal({
  value,
  onChange,
  onSelect,
  selectedType,
  selectedId,
  employees = [],
  subAgents = [],
  customers = [],
  suppliers = [],
  excludeType,
  excludeId,
  label = "Referred By",
  placeholder = "Pick Staff, Sub Agent, Customer, Supplier or type a Broker...",
}: ReferrerSelectModalProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")
  const [activeTab, setActiveTab] = useState<"all" | RecordType | "custom">("all")
  const [customInput, setCustomInput] = useState("")

  // Normalize all master lists into searchable records
  const allRecords = useMemo<ReferrerRecord[]>(() => {
    const list: ReferrerRecord[] = []
    const seenPeople = new Set<number>()
    const skip = (type: string, id: number) =>
      Boolean(excludeType && excludeId && excludeType.toLowerCase() === type.toLowerCase() && Number(excludeId) === id)

    const addPerson = (emp: Employee, forceAgent: boolean) => {
      const id = Number(emp.id)
      if (!id || seenPeople.has(id) || emp.isDeleted) return
      seenPeople.add(id)
      const type: RecordType = forceAgent || isAgentRole(emp.role) ? "agent" : "staff"
      const referrerType = REFERRER_TYPE_FOR[type]
      if (skip(referrerType, id)) return
      list.push({
        key: `${type}-${id}`,
        id,
        title: emp.name,
        subtitle: [type === "agent" ? emp.firmName || "Sub Agent" : emp.role || "Staff", emp.phone || "No phone"]
          .filter(Boolean)
          .join(" • "),
        type,
        referrerType,
        typeLabel: TYPE_STYLE[type].label,
        formattedValue: formatReferrer(referrerType, emp.name),
        searchText: joinSearch(emp.name, emp.firmName, emp.phone, emp.phone2, emp.city, emp.email, emp.assignedMarkets),
      })
    }
    subAgents.forEach((a) => addPerson(a, true))
    employees.forEach((e) => addPerson(e, false))

    customers.forEach((cust) => {
      const id = Number(cust.id)
      if (!id || cust.isDeleted || skip(ReferrerTypes.CUSTOMER, id)) return
      const firm = cust.firmName || cust.name || "Unknown Customer"
      list.push({
        key: `customer-${id}`,
        id,
        title: firm,
        subtitle: `${cust.city || "Ahmedabad"} • ${cust.phone || "No phone"}`,
        type: "customer",
        referrerType: ReferrerTypes.CUSTOMER,
        typeLabel: TYPE_STYLE.customer.label,
        formattedValue: formatReferrer(ReferrerTypes.CUSTOMER, firm),
        searchText: joinSearch(cust.firmName, cust.name, cust.phone, cust.phone2, cust.city, cust.gstin, cust.marketArea),
      })
    })

    suppliers.forEach((sup) => {
      const id = Number(sup.id)
      if (!id || sup.isDeleted || skip(ReferrerTypes.SUPPLIER, id)) return
      const firm = sup.firmName || sup.name || "Unknown Supplier"
      list.push({
        key: `supplier-${id}`,
        id,
        title: firm,
        subtitle: `${sup.type || "Mill/Supplier"} • ${sup.city || "Ahmedabad"}`,
        type: "supplier",
        referrerType: ReferrerTypes.SUPPLIER,
        typeLabel: TYPE_STYLE.supplier.label,
        formattedValue: formatReferrer(ReferrerTypes.SUPPLIER, firm),
        searchText: joinSearch(sup.firmName, sup.name, sup.contactPerson, sup.phone, sup.phone2, sup.city, sup.marketName, sup.marketArea, sup.brand, sup.gstin),
      })
    })

    return list
  }, [employees, subAgents, customers, suppliers, excludeType, excludeId])

  const counts = useMemo(() => {
    const c: Record<RecordType, number> = { staff: 0, agent: 0, customer: 0, supplier: 0 }
    allRecords.forEach((r) => c[r.type]++)
    return c
  }, [allRecords])

  // Filter based on activeTab and searchQuery
  const filteredRecords = useMemo(() => {
    const q = searchQuery.trim().toLowerCase()
    return allRecords.filter((rec) => {
      if (activeTab !== "all" && activeTab !== "custom" && rec.type !== activeTab) return false
      if (!q) return true
      return (
        rec.title.toLowerCase().includes(q) ||
        rec.searchText.includes(q) ||
        rec.subtitle.toLowerCase().includes(q) ||
        rec.typeLabel.toLowerCase().includes(q)
      )
    })
  }, [allRecords, activeTab, searchQuery])

  // Badge for the current value (works for old "(agent)" style strings too)
  const currentMeta = useMemo(() => {
    if (!value) return null
    const { type } = parseReferrer(value)
    switch (type) {
      case ReferrerTypes.STAFF:
        return TYPE_STYLE.staff
      case ReferrerTypes.AGENT:
        return TYPE_STYLE.agent
      case ReferrerTypes.CUSTOMER:
        return TYPE_STYLE.customer
      case ReferrerTypes.SUPPLIER:
        return TYPE_STYLE.supplier
      default:
        return TYPE_STYLE.broker
    }
  }, [value])

  const isSelectedRecord = (rec: ReferrerRecord) => {
    if (selectedId && selectedType) {
      return Number(selectedId) === rec.id && selectedType.toLowerCase() === rec.referrerType.toLowerCase()
    }
    return value.trim().toLowerCase() === rec.formattedValue.toLowerCase()
  }

  const emit = (selection: ReferrerSelection | null) => {
    onChange(selection ? selection.referredBy : "")
    onSelect?.(selection)
  }

  const handleSelect = (rec: ReferrerRecord) => {
    emit({ referredBy: rec.formattedValue, referredByType: rec.referrerType, referredById: rec.id })
    setIsOpen(false)
  }

  const applyBroker = (raw: string) => {
    const name = raw.trim()
    if (!name) return
    const parsed = parseReferrer(name)
    const text = parsed.type ? name : formatReferrer(ReferrerTypes.BROKER, name)
    emit({ referredBy: text, referredByType: parsed.type || ReferrerTypes.BROKER, referredById: 0 })
    setCustomInput("")
    setIsOpen(false)
  }

  const CurrentIcon = currentMeta ? currentMeta.icon : Link

  const tabButton = (id: "all" | RecordType | "custom", text: string, activeClass: string) => (
    <button
      type="button"
      onClick={() => setActiveTab(id)}
      aria-pressed={activeTab === id}
      className={`px-2.5 py-1 rounded-md font-medium whitespace-nowrap transition-colors ${
        activeTab === id ? activeClass : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
      }`}
    >
      {text}
    </button>
  )

  return (
    <div className="space-y-1.5">
      {label && <p className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">{label}</p>}

      {/* Display field */}
      <div
        role="button"
        tabIndex={0}
        onClick={() => setIsOpen(true)}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault()
            setIsOpen(true)
          }
        }}
        aria-label={value ? `Referred by ${value}. Change` : "Choose who referred"}
        className={`flex items-center justify-between p-2 rounded-lg border cursor-pointer transition-all hover:border-zinc-400 dark:hover:border-zinc-600 ${
          value
            ? "border-zinc-300 bg-zinc-50/70 dark:border-zinc-700 dark:bg-zinc-900/60"
            : "border-dashed border-zinc-300 bg-white dark:border-zinc-700 dark:bg-zinc-950"
        }`}
      >
        <div className="flex items-center gap-2.5 overflow-hidden">
          <div
            className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full border ${
              currentMeta ? currentMeta.badge : "bg-zinc-100 text-zinc-500 dark:bg-zinc-800 dark:text-zinc-400"
            }`}
          >
            <CurrentIcon className="h-3.5 w-3.5" aria-hidden="true" />
          </div>

          <div className="truncate">
            {value ? (
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-semibold text-zinc-900 dark:text-zinc-100 truncate">{value}</span>
                {currentMeta && (
                  <span className={`inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium border ${currentMeta.badge}`}>
                    {currentMeta.label}
                  </span>
                )}
              </div>
            ) : (
              <span className="text-xs text-zinc-400 dark:text-zinc-500">{placeholder}</span>
            )}
          </div>
        </div>

        <div className="flex items-center gap-1 shrink-0 ml-2" onClick={(e) => e.stopPropagation()}>
          {value && (
            <button
              type="button"
              onClick={() => emit(null)}
              className="p-1 rounded text-zinc-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors"
              title="Clear selection"
              aria-label="Clear referred by"
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

      <Dialog
        open={isOpen}
        onOpenChange={setIsOpen}
        title="Who referred?"
        description="Pick a Staff member, Sub Agent, Customer or Supplier, or type an outside broker"
        className="max-w-md"
      >
        <div className="space-y-3">
          <div className="flex items-center gap-1 overflow-x-auto pb-1 text-xs border-b border-zinc-200 dark:border-zinc-800">
            {tabButton("all", `All (${allRecords.length})`, "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900")}
            {tabButton("staff", `Staff (${counts.staff})`, "bg-blue-600 text-white")}
            {tabButton("agent", `Sub Agents (${counts.agent})`, "bg-teal-600 text-white")}
            {tabButton("customer", `Customers (${counts.customer})`, "bg-emerald-600 text-white")}
            {tabButton("supplier", `Suppliers (${counts.supplier})`, "bg-purple-600 text-white")}
            {tabButton("custom", "Broker", "bg-amber-600 text-white")}
          </div>

          {activeTab === "custom" ? (
            <div className="space-y-3 py-2">
              <label htmlFor="referrer-broker-name" className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
                Outside broker / introducer name
              </label>
              <Input
                id="referrer-broker-name"
                placeholder="e.g. Suresh Bhai (Ring Road)"
                value={customInput}
                onChange={(e) => setCustomInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault()
                    applyBroker(customInput)
                  }
                }}
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
                      emit(null)
                      setIsOpen(false)
                    }}
                  >
                    Clear Current
                  </Button>
                ) : (
                  <span />
                )}
                <Button type="button" size="sm" onClick={() => applyBroker(customInput)} disabled={!customInput.trim()}>
                  Use this name
                </Button>
              </div>
            </div>
          ) : (
            <>
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-zinc-400" aria-hidden="true" />
                <Input
                  placeholder="Search name, firm, phone, city..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-9 text-xs h-9"
                  aria-label="Search referrers"
                  autoFocus
                />
                {searchQuery && (
                  <button
                    type="button"
                    onClick={() => setSearchQuery("")}
                    className="absolute right-2.5 top-2.5 text-zinc-400 hover:text-zinc-600"
                    aria-label="Clear search"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>

              <div className="max-h-64 overflow-y-auto space-y-1 pr-1" role="listbox" aria-label="Referrers">
                {filteredRecords.length === 0 ? (
                  <div className="py-6 text-center space-y-2">
                    <p className="text-xs text-zinc-500">No matching records found.</p>
                    {searchQuery && (
                      <Button type="button" variant="outline" size="sm" onClick={() => applyBroker(searchQuery)} className="text-xs">
                        Use &quot;{searchQuery.trim()}&quot; as Broker
                      </Button>
                    )}
                  </div>
                ) : (
                  filteredRecords.map((rec) => {
                    const selected = isSelectedRecord(rec)
                    const style = TYPE_STYLE[rec.type]
                    const IconComp = style.icon
                    return (
                      <div
                        key={rec.key}
                        role="option"
                        aria-selected={selected}
                        tabIndex={0}
                        onClick={() => handleSelect(rec)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter" || e.key === " ") {
                            e.preventDefault()
                            handleSelect(rec)
                          }
                        }}
                        className={`flex items-center justify-between p-2 rounded-lg cursor-pointer transition-colors border ${
                          selected
                            ? "bg-zinc-100 border-zinc-400 dark:bg-zinc-800 dark:border-zinc-600"
                            : "bg-white border-transparent hover:bg-zinc-50 hover:border-zinc-200 dark:bg-zinc-950 dark:hover:bg-zinc-900"
                        }`}
                      >
                        <div className="flex items-center gap-2.5 min-w-0">
                          <div className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full border ${style.badge}`}>
                            <IconComp className="h-3.5 w-3.5" aria-hidden="true" />
                          </div>
                          <div className="min-w-0">
                            <div className="flex items-center gap-1.5">
                              <span className="text-xs font-semibold text-zinc-900 dark:text-zinc-100 truncate">{rec.title}</span>
                              <span className={`text-[10px] px-1 py-0.2 rounded border font-medium ${style.badge}`}>{rec.typeLabel}</span>
                            </div>
                            <p className="text-[11px] text-zinc-500 dark:text-zinc-400 truncate">{rec.subtitle}</p>
                          </div>
                        </div>
                        {selected && <Check className="h-4 w-4 text-zinc-900 dark:text-zinc-100 shrink-0 ml-2" aria-hidden="true" />}
                      </div>
                    )
                  })
                )}
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-zinc-200 dark:border-zinc-800 text-xs">
                {value ? (
                  <button
                    type="button"
                    onClick={() => {
                      emit(null)
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
                  <Edit3 className="h-3.5 w-3.5" aria-hidden="true" />
                  Type outside broker
                  <ArrowRight className="h-3 w-3" aria-hidden="true" />
                </button>
              </div>
            </>
          )}
        </div>
      </Dialog>
    </div>
  )
}
