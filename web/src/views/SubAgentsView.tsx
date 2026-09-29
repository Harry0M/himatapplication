import { useMemo, useState } from "react"
import type { ChangeEvent, InputHTMLAttributes } from "react"
import {
  ArrowLeft,
  Copy,
  Check,
  Edit2,
  Handshake,
  MessageCircle,
  Phone,
  Plus,
  Search,
  UserX,
  RotateCcw,
  X,
} from "lucide-react"
import { useData } from "../context/DataContext"
import { useAuth } from "../context/AuthContext"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Tabs } from "../components/ui/Tabs"
import { DateRangeFilter } from "../components/ui/DateRangeFilter"
import { ActivitySummary, CustomersTable, OrdersTable, ReferredList, Section, TripsTable } from "../components/related/RelatedRecords"
import type { Employee } from "../types"
import {
  ALL_TIME,
  DateRange,
  ReferrerTypes,
  Roles,
  customersOfSubAgent,
  displayCode,
  filterOrdersByDate,
  filterTripsByDate,
  indexById,
  newId,
  ordersOfTrips,
  referredRecords,
  tripsOfCustomers,
} from "../lib/domain"
import { customerRegistrationUrl, subAgentInviteMessage, whatsAppUrl } from "../lib/links"

const isInactive = (a: Employee) => Boolean(a.isDeleted) || (a.status || "").toLowerCase() === "deactivated"

// -----------------------------------------------------------------------------
// Master list
// -----------------------------------------------------------------------------

export function SubAgentsView() {
  const { subAgents, customers, visits, entries, deactivateEmployee, resumeEmployee } = useData()
  const { isAdmin } = useAuth()
  const [search, setSearch] = useState("")
  const [statusFilter, setStatusFilter] = useState<"active" | "inactive" | "all">("active")
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Employee | null>(null)

  const counts = useMemo(
    () => ({
      active: subAgents.filter((a) => !isInactive(a)).length,
      inactive: subAgents.filter(isInactive).length,
      all: subAgents.length,
    }),
    [subAgents]
  )

  const q = search.trim().toLowerCase()
  const list = subAgents.filter((a) => {
    if (statusFilter === "active" && isInactive(a)) return false
    if (statusFilter === "inactive" && !isInactive(a)) return false
    if (!q) return true
    return [a.name, a.firmName, a.phone, a.city, a.email, a.employeeId].some((v) => (v || "").toLowerCase().includes(q))
  })

  const selected = selectedId ? subAgents.find((a) => Number(a.id) === selectedId) : undefined

  const openAdd = () => {
    setEditing(null)
    setFormOpen(true)
  }
  const openEdit = (a: Employee) => {
    setEditing(a)
    setFormOpen(true)
  }

  if (selected) {
    return (
      <>
        <SubAgentDetail
          agent={selected}
          onBack={() => setSelectedId(null)}
          onEdit={() => openEdit(selected)}
          onDeactivate={isAdmin ? () => deactivateEmployee(Number(selected.id), "Sub Agent deactivated by Admin") : undefined}
          onReactivate={isAdmin ? () => resumeEmployee(Number(selected.id)) : undefined}
        />
        <SubAgentFormDialog open={formOpen} onOpenChange={setFormOpen} editing={editing} />
      </>
    )
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
            <Handshake className="h-5 w-5" aria-hidden="true" />
            Sub Agents
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Outside agents who bring customers to us. Each has a personal registration link; customers who use it are linked automatically.
          </p>
        </div>
        <Button size="sm" onClick={openAdd} className="gap-1 self-start sm:self-auto">
          <Plus className="h-3.5 w-3.5" aria-hidden="true" />
          New Sub Agent
        </Button>
      </div>

      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative w-full sm:max-w-xs">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" aria-hidden="true" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search name, firm, phone, city..."
            className="pl-9 h-9 text-xs"
            aria-label="Search sub agents"
          />
          {search && (
            <button
              type="button"
              onClick={() => setSearch("")}
              className="absolute right-3 top-2.5 text-muted-foreground"
              aria-label="Clear search"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>
        <Tabs
          value={statusFilter}
          onValueChange={(v) => setStatusFilter(v as "active" | "inactive" | "all")}
          options={[
            { value: "active", label: "Active", count: counts.active },
            { value: "inactive", label: "Deactivated", count: counts.inactive },
            { value: "all", label: "All", count: counts.all },
          ]}
        />
      </div>

      {list.length === 0 ? (
        <Card className="p-10 text-center">
          <p className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">No sub agents here yet</p>
          <p className="mt-1 text-xs text-muted-foreground">Add the agents who bring you customers, then share their personal link.</p>
          <Button size="sm" onClick={openAdd} className="mt-4 gap-1">
            <Plus className="h-3.5 w-3.5" aria-hidden="true" />
            New Sub Agent
          </Button>
        </Card>
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-zinc-200/80 bg-zinc-50/70 font-semibold text-muted-foreground dark:border-zinc-800 dark:bg-zinc-900/50">
                <tr>
                  <th className="py-3 pl-4 pr-2">Sub Agent</th>
                  <th className="px-2 py-3">Phone</th>
                  <th className="px-2 py-3">City</th>
                  <th className="px-2 py-3">Customers</th>
                  <th className="px-2 py-3">Orders</th>
                  <th className="px-2 py-3">Status</th>
                  <th className="py-3 pl-2 pr-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
                {list.map((a) => {
                  const mine = customersOfSubAgent(customers, a)
                  const trips = tripsOfCustomers(visits, new Set(mine.map((c) => Number(c.id))))
                  const orders = ordersOfTrips(entries, new Set(trips.map((v) => Number(v.id))))
                  return (
                    <tr
                      key={a.id}
                      className="cursor-pointer hover:bg-zinc-50/60 dark:hover:bg-zinc-900/50"
                      onClick={() => setSelectedId(Number(a.id))}
                    >
                      <td className="py-3 pl-4 pr-2">
                        <span className="font-semibold text-zinc-900 dark:text-zinc-100">{a.name}</span>
                        <span className="block text-[10px] text-muted-foreground">
                          {[a.firmName, a.employeeId].filter(Boolean).join(" • ")}
                        </span>
                      </td>
                      <td className="px-2 py-3 whitespace-nowrap">{a.phone || "—"}</td>
                      <td className="px-2 py-3">{a.city || "—"}</td>
                      <td className="px-2 py-3 font-semibold">{mine.length}</td>
                      <td className="px-2 py-3">{orders.length}</td>
                      <td className="px-2 py-3">
                        <Badge variant={isInactive(a) ? "secondary" : "success"}>{isInactive(a) ? "Deactivated" : "Active"}</Badge>
                      </td>
                      <td className="py-3 pl-2 pr-4 text-right" onClick={(e) => e.stopPropagation()}>
                        <Button variant="outline" size="sm" className="h-7 px-2.5 text-[11px]" onClick={() => openEdit(a)}>
                          <Edit2 className="h-3 w-3" aria-hidden="true" />
                          Edit
                        </Button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <SubAgentFormDialog open={formOpen} onOpenChange={setFormOpen} editing={editing} />
    </div>
  )
}

// -----------------------------------------------------------------------------
// Add / edit form
// -----------------------------------------------------------------------------

export function SubAgentFormDialog({
  open,
  onOpenChange,
  editing,
  onSaved,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  editing: Employee | null
  /** Called with the saved record (used by the customer form to pick a freshly created Sub Agent) */
  onSaved?: (agent: Employee) => void
}) {
  const { saveEmployee, allPeople } = useData()
  // Same rule as the Android app: only Admins can give a Sub Agent a login email
  const { isAdmin } = useAuth()
  const [form, setForm] = useState({ name: "", firmName: "", phone: "", phone2: "", email: "", city: "", notes: "" })
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [lastOpenKey, setLastOpenKey] = useState<string>("")

  // Reset the form each time the dialog opens
  const openKey = open ? `open-${editing?.id ?? "new"}` : ""
  if (openKey !== lastOpenKey) {
    setLastOpenKey(openKey)
    if (open) {
      setForm({
        name: editing?.name || "",
        firmName: editing?.firmName || "",
        phone: editing?.phone || "",
        phone2: editing?.phone2 || "",
        email: editing?.email || "",
        city: editing?.city || "",
        notes: editing?.notes || "",
      })
      setError(null)
    }
  }

  const set = (key: keyof typeof form) => (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }))

  const save = async () => {
    const name = form.name.trim()
    const phoneDigits = form.phone.replace(/\D/g, "")
    const email = isAdmin ? form.email.trim().toLowerCase() : (editing?.email || "").trim().toLowerCase()
    if (!name) return setError("Name is required.")
    if (phoneDigits.length < 10) return setError("Enter a 10 digit mobile number.")
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return setError("Email looks wrong.")
    const clash = allPeople.find(
      (p) =>
        Number(p.id) !== Number(editing?.id) &&
        !p.isDeleted &&
        ((p.phone || "").replace(/\D/g, "").slice(-10) === phoneDigits.slice(-10) ||
          (email && (p.email || "").trim().toLowerCase() === email))
    )
    if (clash) return setError(`${clash.name} already uses this phone or email.`)

    setSaving(true)
    setError(null)
    try {
      const base: Partial<Employee> = editing ? { ...editing } : {}
      const payload: Employee = {
        ...base,
        id: editing ? Number(editing.id) : newId(),
        employeeId: editing?.employeeId || displayCode("AGT"),
        name,
        firmName: form.firmName.trim(),
        phone: form.phone.trim(),
        phone2: form.phone2.trim(),
        email,
        city: form.city.trim(),
        notes: form.notes.trim(),
        role: Roles.AGENT,
        status: editing?.status || "Active",
        isBlocked: editing?.isBlocked ?? false,
        isDeleted: editing?.isDeleted ?? false,
      } as Employee
      await saveEmployee(payload)
      onSaved?.(payload)
      onOpenChange(false)
    } catch (e: any) {
      setError(e?.message || "Could not save. Please try again.")
    } finally {
      setSaving(false)
    }
  }

  const field = (id: keyof typeof form, label: string, props: Partial<InputHTMLAttributes<HTMLInputElement>> = {}) => (
    <div>
      <label htmlFor={`subagent-${id}`} className="text-xs font-medium text-muted-foreground">
        {label}
      </label>
      <Input id={`subagent-${id}`} value={form[id]} onChange={set(id)} className="mt-1 h-9 text-xs" {...props} />
    </div>
  )

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title={editing ? "Edit Sub Agent" : "New Sub Agent"}
      description={
        isAdmin
          ? "The email lets them sign in to see their own customers (read-only)."
          : "An Admin can add their login email later."
      }
    >
      <div className="space-y-3">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {field("name", "Name *", { autoFocus: true, placeholder: "Agent name" })}
          {field("firmName", "Firm / shop", { placeholder: "Optional" })}
          {field("phone", "Mobile *", { inputMode: "tel", placeholder: "10 digit mobile" })}
          {field("phone2", "WhatsApp / other phone", { inputMode: "tel" })}
          {isAdmin && field("email", "Google email (for login)", { type: "email", placeholder: "Optional" })}
          {field("city", "City", { placeholder: "e.g. Ahmedabad" })}
        </div>
        <div>
          <label htmlFor="subagent-notes" className="text-xs font-medium text-muted-foreground">
            Notes
          </label>
          <textarea
            id="subagent-notes"
            value={form.notes}
            onChange={set("notes")}
            rows={2}
            className="mt-1 w-full rounded-xl border border-zinc-200 bg-transparent px-3 py-2 text-xs dark:border-zinc-800 focus:outline-none focus:ring-1 focus:ring-zinc-900"
          />
        </div>
        {error && (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700 dark:bg-red-950/40 dark:text-red-300" role="alert">
            {error}
          </p>
        )}
        <div className="flex justify-end gap-2 pt-1">
          <Button variant="outline" size="sm" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button size="sm" onClick={save} disabled={saving}>
            {saving ? "Saving..." : editing ? "Save changes" : "Add Sub Agent"}
          </Button>
        </div>
      </div>
    </Dialog>
  )
}

// -----------------------------------------------------------------------------
// Detail: everything linked to one Sub Agent (also used as the Sub Agent's own portal)
// -----------------------------------------------------------------------------

export function SubAgentDetail({
  agent,
  onBack,
  onEdit,
  onDeactivate,
  onReactivate,
  portal = false,
}: {
  agent: Employee
  onBack?: () => void
  onEdit?: () => void
  onDeactivate?: () => void
  onReactivate?: () => void
  /** The agent looking at their own data: read-only, no referred section */
  portal?: boolean
}) {
  const { customers, visits, entries, suppliers, allPeople, employees } = useData()
  const [range, setRange] = useState<DateRange>(ALL_TIME)
  const [tab, setTab] = useState<"customers" | "trips" | "orders" | "referred">("customers")
  const [copied, setCopied] = useState(false)

  const myCustomers = useMemo(() => customersOfSubAgent(customers, agent), [customers, agent])
  const customerIds = useMemo(() => new Set(myCustomers.map((c) => Number(c.id))), [myCustomers])
  const allTrips = useMemo(() => tripsOfCustomers(visits, customerIds), [visits, customerIds])
  const visitsById = useMemo(() => indexById(visits), [visits])
  const allOrders = useMemo(() => ordersOfTrips(entries, new Set(allTrips.map((v) => Number(v.id)))), [entries, allTrips])
  const trips = useMemo(() => filterTripsByDate(allTrips, range), [allTrips, range])
  const orders = useMemo(() => filterOrdersByDate(allOrders, visitsById, range), [allOrders, visitsById, range])

  const names = [agent.name, agent.firmName]
  const referredCustomers = portal ? [] : referredRecords(customers, ReferrerTypes.AGENT, Number(agent.id), names)
  const referredSuppliers = portal ? [] : referredRecords(suppliers, ReferrerTypes.AGENT, Number(agent.id), names)
  const referredPeople = portal ? [] : referredRecords(allPeople, ReferrerTypes.AGENT, Number(agent.id), names, true)
  const referredCount = referredCustomers.length + referredSuppliers.length + referredPeople.length

  const link = customerRegistrationUrl(Number(agent.id))
  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(link)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      window.prompt("Copy this link", link)
    }
  }
  const inactive = isInactive(agent)

  const tabs = [
    { value: "customers", label: "Customers", count: myCustomers.length },
    { value: "trips", label: "Trips", count: trips.length },
    { value: "orders", label: "Orders", count: orders.length },
    ...(portal ? [] : [{ value: "referred", label: "Referred", count: referredCount }]),
  ]

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-start gap-3">
          {onBack && (
            <Button variant="outline" size="icon" onClick={onBack} className="h-8 w-8 shrink-0" aria-label="Back to Sub Agents">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          )}
          <div>
            <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
              {portal ? `Welcome, ${agent.name}` : agent.name}
            </h2>
            <p className="text-xs text-muted-foreground mt-0.5">
              {[agent.firmName, agent.city, agent.employeeId].filter(Boolean).join(" • ") || "Sub Agent"}
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <Badge variant={inactive ? "secondary" : "success"}>{inactive ? "Deactivated" : "Active"} Sub Agent</Badge>
              {agent.phone && (
                <a href={`tel:${agent.phone}`} className="inline-flex items-center gap-1 text-xs text-zinc-700 hover:underline dark:text-zinc-300">
                  <Phone className="h-3 w-3" aria-hidden="true" />
                  {agent.phone}
                </a>
              )}
            </div>
          </div>
        </div>
        {!portal && (
          <div className="flex flex-wrap gap-2">
            {onEdit && (
              <Button variant="outline" size="sm" onClick={onEdit} className="gap-1">
                <Edit2 className="h-3.5 w-3.5" aria-hidden="true" />
                Edit
              </Button>
            )}
            {inactive
              ? onReactivate && (
                  <Button variant="outline" size="sm" onClick={onReactivate} className="gap-1">
                    <RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />
                    Reactivate
                  </Button>
                )
              : onDeactivate && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      if (confirm(`Deactivate ${agent.name}? Their customers and history stay linked.`)) onDeactivate()
                    }}
                    className="gap-1 text-red-600 border-red-200 hover:bg-red-50"
                  >
                    <UserX className="h-3.5 w-3.5" aria-hidden="true" />
                    Deactivate
                  </Button>
                )}
          </div>
        )}
      </div>

      {/* Personal registration link */}
      <Card className="p-4">
        <p className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">
          {portal ? "Your registration link" : "Personal registration link"}
        </p>
        <p className="mt-0.5 text-[11px] text-muted-foreground">
          Customers who register through this link are linked to {portal ? "you" : agent.name} automatically.
        </p>
        <div className="mt-2 flex flex-col gap-2 sm:flex-row sm:items-center">
          <code className="flex-1 truncate rounded-lg bg-zinc-100 px-3 py-2 text-[11px] dark:bg-zinc-900">{link}</code>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={copyLink} className="gap-1">
              {copied ? <Check className="h-3.5 w-3.5" aria-hidden="true" /> : <Copy className="h-3.5 w-3.5" aria-hidden="true" />}
              {copied ? "Copied" : "Copy"}
            </Button>
            <a
              href={whatsAppUrl(subAgentInviteMessage(agent.name, link))}
              target="_blank"
              rel="noreferrer"
              className="inline-flex h-8 items-center gap-1 rounded-full bg-emerald-600 px-3 text-xs font-medium text-white hover:bg-emerald-700"
            >
              <MessageCircle className="h-3.5 w-3.5" aria-hidden="true" />
              WhatsApp
            </a>
          </div>
        </div>
      </Card>

      <DateRangeFilter value={range} onChange={setRange} />
      <ActivitySummary trips={trips} orders={orders} />

      <Tabs value={tab} onValueChange={(v) => setTab(v as typeof tab)} options={tabs} />

      {tab === "customers" && (
        <Section title="Customers brought in" count={myCustomers.length}>
          <CustomersTable
            customers={myCustomers}
            visits={visits}
            entries={entries}
            emptyText={portal ? "No customers linked to you yet. Share your link to get started." : "No customers linked yet."}
          />
        </Section>
      )}
      {tab === "trips" && (
        <Section title="Trips of these customers" count={trips.length}>
          <TripsTable trips={trips} entries={entries} employees={employees} />
        </Section>
      )}
      {tab === "orders" && (
        <Section title="Orders of these customers" count={orders.length}>
          <OrdersTable orders={orders} visitsById={visitsById} />
        </Section>
      )}
      {tab === "referred" && !portal && (
        <Section title={`Referred by ${agent.name}`} count={referredCount}>
          <ReferredList customers={referredCustomers} suppliers={referredSuppliers} people={referredPeople} />
        </Section>
      )}
    </div>
  )
}
