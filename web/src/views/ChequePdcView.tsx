import React, { useState, useMemo } from "react"
import {
  Landmark,
  Calendar,
  AlertCircle,
  CheckCircle2,
  Clock,
  Search,
  Plus,
  Edit2,
  Trash2,
  Filter,
  Check,
  X,
  FileText,
  AlertTriangle,
  ArrowUpRight,
  TrendingUp,
  User,
  Building2
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatInr, formatDate, cn } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { ChequePdc, ChequePartyType, ChequeStatus } from "../types"

export function ChequePdcView() {
  const {
    chequesPdc,
    dueTodayChequesCount,
    customers,
    suppliers,
    saveChequePdc,
    updateChequePdcStatus,
    deleteChequePdc
  } = useData()

  const today = useMemo(() => new Date().toISOString().split("T")[0], [])

  // Filters
  const [search, setSearch] = useState<string>("")
  const [partyTypeFilter, setPartyTypeFilter] = useState<string>("ALL") // "ALL", "CUSTOMER", "SUPPLIER"
  const [statusFilter, setStatusFilter] = useState<string>("ALL") // "ALL", "DUE_TODAY", "UPCOMING_PDC", "PENDING", "DEPOSITED", "CLEARED", "BOUNCED"
  const [selectedPartyId, setSelectedPartyId] = useState<number | "all">("all")

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingCheque, setEditingCheque] = useState<ChequePdc | null>(null)
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null)

  // Form State
  const [formChequeNo, setFormChequeNo] = useState("")
  const [formBankName, setFormBankName] = useState("")
  const [formAmount, setFormAmount] = useState("")
  const [formDate, setFormDate] = useState(today)
  const [formPartyType, setFormPartyType] = useState<ChequePartyType>("Customer")
  const [formPartyId, setFormPartyId] = useState<number>(0)
  const [formPartyName, setFormPartyName] = useState("")
  const [formStatus, setFormStatus] = useState<ChequeStatus>("Pending")
  const [formNotes, setFormNotes] = useState("")
  const [formIsSecurityCheque, setFormIsSecurityCheque] = useState(false)
  const [formAccountNumber, setFormAccountNumber] = useState("")

  // Quick stats
  const dueTodayCheques = useMemo(() => {
    return chequesPdc.filter(
      (c) => c.chequeDate === today && (c.status === "Pending" || c.status === "Due Today")
    )
  }, [chequesPdc, today])

  const upcomingPdcCheques = useMemo(() => {
    return chequesPdc.filter((c) => c.chequeDate > today && c.status === "Pending")
  }, [chequesPdc, today])

  const depositedCheques = useMemo(() => {
    return chequesPdc.filter((c) => c.status === "Deposited")
  }, [chequesPdc])

  const clearedCheques = useMemo(() => {
    return chequesPdc.filter((c) => c.status === "Cleared")
  }, [chequesPdc])

  const bouncedCheques = useMemo(() => {
    return chequesPdc.filter((c) => c.status === "Bounced")
  }, [chequesPdc])

  // Filtered Cheques
  const filteredCheques = useMemo(() => {
    return chequesPdc.filter((cheque) => {
      // Search filter
      const q = search.trim().toLowerCase()
      const matchesSearch =
        !q ||
        cheque.chequeNo.toLowerCase().includes(q) ||
        cheque.bankName.toLowerCase().includes(q) ||
        cheque.partyName.toLowerCase().includes(q) ||
        (cheque.notes && cheque.notes.toLowerCase().includes(q)) ||
        (cheque.accountNumber && cheque.accountNumber.toLowerCase().includes(q)) ||
        cheque.amount.toString().includes(q)

      // Party Type filter
      const matchesPartyType =
        partyTypeFilter === "ALL" ||
        cheque.partyType.toUpperCase() === partyTypeFilter

      // Specific party ID filter (Customer-wise / Supplier-wise)
      const matchesPartyId =
        selectedPartyId === "all" || cheque.partyId === selectedPartyId

      // Status filter
      let matchesStatus = true
      if (statusFilter === "DUE_TODAY") {
        matchesStatus =
          cheque.chequeDate === today &&
          (cheque.status === "Pending" || cheque.status === "Due Today")
      } else if (statusFilter === "UPCOMING_PDC") {
        matchesStatus = cheque.chequeDate > today && cheque.status === "Pending"
      } else if (statusFilter === "SECURITY") {
        matchesStatus = Boolean(cheque.isSecurityCheque || (cheque.notes && cheque.notes.toLowerCase().includes("security")))
      } else if (statusFilter !== "ALL") {
        matchesStatus =
          cheque.status.toUpperCase() === statusFilter.toUpperCase()
      }

      return matchesSearch && matchesPartyType && matchesPartyId && matchesStatus
    })
  }, [chequesPdc, search, partyTypeFilter, statusFilter, selectedPartyId, today])

  // Open modal for Create
  const handleOpenCreate = () => {
    setEditingCheque(null)
    setFormChequeNo("")
    setFormBankName("")
    setFormAmount("")
    setFormDate(today)
    setFormPartyType("Customer")
    setFormPartyId(0)
    setFormPartyName("")
    setFormStatus("Pending")
    setFormNotes("")
    setFormIsSecurityCheque(false)
    setFormAccountNumber("")
    setIsModalOpen(true)
  }

  // Open modal for Edit
  const handleOpenEdit = (cheque: ChequePdc) => {
    setEditingCheque(cheque)
    setFormChequeNo(cheque.chequeNo)
    setFormBankName(cheque.bankName)
    setFormAmount(cheque.amount.toString())
    setFormDate(cheque.chequeDate || today)
    setFormPartyType(cheque.partyType)
    setFormPartyId(cheque.partyId)
    setFormPartyName(cheque.partyName)
    setFormStatus(cheque.status)
    setFormNotes(cheque.notes || "")
    setFormIsSecurityCheque(Boolean(cheque.isSecurityCheque || (cheque.notes && cheque.notes.toLowerCase().includes("security"))))
    setFormAccountNumber(cheque.accountNumber || "")
    setIsModalOpen(true)
  }

  // Save Cheque
  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formChequeNo || !formBankName || !formAmount || !formPartyName || !formDate) {
      alert("Please fill in all required fields.")
      return
    }

    try {
      await saveChequePdc({
        id: editingCheque?.id,
        chequeNo: formChequeNo,
        bankName: formBankName,
        amount: Number(formAmount),
        chequeDate: formDate,
        partyType: formPartyType,
        partyId: formPartyId,
        partyName: formPartyName,
        status: formStatus,
        notes: formNotes,
        isSecurityCheque: formIsSecurityCheque,
        accountNumber: formAccountNumber,
      })
      setIsModalOpen(false)
    } catch (err) {
      console.error("Error saving cheque:", err)
      alert("Failed to save cheque.")
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Landmark className="h-6 w-6 text-primary" /> Cheques & PDC
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Post-Dated and Regular Cheques Register for Customers & Suppliers
          </p>
        </div>
        <Button onClick={handleOpenCreate} className="gap-2 shadow-sm">
          <Plus className="h-4 w-4" /> Record New Cheque
        </Button>
      </div>

      {/* DUE TODAY ALERT BANNER */}
      {dueTodayCheques.length > 0 && (
        <div className="bg-red-50 border-2 border-red-400 rounded-xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-sm animate-pulse-once">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-red-600 text-white rounded-full">
              <AlertCircle className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-red-900 flex items-center gap-2">
                ATTENTION: {dueTodayCheques.length} Cheque{dueTodayCheques.length > 1 ? "s" : ""} Due for Deposit Today!
              </h3>
              <p className="text-sm text-red-700">
                Total Amount: <span className="font-bold">₹{formatInr(dueTodayCheques.reduce((sum, c) => sum + c.amount, 0))}</span> must be deposited in bank today ({today}).
              </p>
            </div>
          </div>
          <Button
            size="sm"
            onClick={() => setStatusFilter("DUE_TODAY")}
            className="bg-red-600 hover:bg-red-700 text-white whitespace-nowrap shadow-sm"
          >
            View Due Today
          </Button>
        </div>
      )}

      {/* KPI Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-3">
        {/* Due Today */}
        <Card
          onClick={() => setStatusFilter(statusFilter === "DUE_TODAY" ? "ALL" : "DUE_TODAY")}
          className={cn(
            "p-4 cursor-pointer transition-all duration-200 border",
            statusFilter === "DUE_TODAY"
              ? "ring-2 ring-red-500 bg-red-50/50 border-red-300"
              : "hover:border-red-300 hover:bg-slate-50"
          )}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Due Today</span>
            <span className="p-1.5 rounded-lg bg-red-100 text-red-700">
              <AlertCircle className="h-4 w-4" />
            </span>
          </div>
          <div className="mt-2 text-2xl font-black text-red-600">
            {dueTodayCheques.length}
          </div>
          <div className="text-xs text-slate-600 mt-1 font-medium">
            ₹{formatInr(dueTodayCheques.reduce((s, c) => s + c.amount, 0))}
          </div>
        </Card>

        {/* Upcoming PDC */}
        <Card
          onClick={() => setStatusFilter(statusFilter === "UPCOMING_PDC" ? "ALL" : "UPCOMING_PDC")}
          className={cn(
            "p-4 cursor-pointer transition-all duration-200 border",
            statusFilter === "UPCOMING_PDC"
              ? "ring-2 ring-blue-500 bg-blue-50/50 border-blue-300"
              : "hover:border-blue-300 hover:bg-slate-50"
          )}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Upcoming PDC</span>
            <span className="p-1.5 rounded-lg bg-blue-100 text-blue-700">
              <Clock className="h-4 w-4" />
            </span>
          </div>
          <div className="mt-2 text-2xl font-black text-blue-600">
            {upcomingPdcCheques.length}
          </div>
          <div className="text-xs text-slate-600 mt-1 font-medium">
            ₹{formatInr(upcomingPdcCheques.reduce((s, c) => s + c.amount, 0))}
          </div>
        </Card>

        {/* Deposited */}
        <Card
          onClick={() => setStatusFilter(statusFilter === "DEPOSITED" ? "ALL" : "DEPOSITED")}
          className={cn(
            "p-4 cursor-pointer transition-all duration-200 border",
            statusFilter === "DEPOSITED"
              ? "ring-2 ring-amber-500 bg-amber-50/50 border-amber-300"
              : "hover:border-amber-300 hover:bg-slate-50"
          )}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Deposited</span>
            <span className="p-1.5 rounded-lg bg-amber-100 text-amber-700">
              <Landmark className="h-4 w-4" />
            </span>
          </div>
          <div className="mt-2 text-2xl font-black text-amber-600">
            {depositedCheques.length}
          </div>
          <div className="text-xs text-slate-600 mt-1 font-medium">
            ₹{formatInr(depositedCheques.reduce((s, c) => s + c.amount, 0))}
          </div>
        </Card>

        {/* Cleared */}
        <Card
          onClick={() => setStatusFilter(statusFilter === "CLEARED" ? "ALL" : "CLEARED")}
          className={cn(
            "p-4 cursor-pointer transition-all duration-200 border",
            statusFilter === "CLEARED"
              ? "ring-2 ring-emerald-500 bg-emerald-50/50 border-emerald-300"
              : "hover:border-emerald-300 hover:bg-slate-50"
          )}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Cleared</span>
            <span className="p-1.5 rounded-lg bg-emerald-100 text-emerald-700">
              <CheckCircle2 className="h-4 w-4" />
            </span>
          </div>
          <div className="mt-2 text-2xl font-black text-emerald-600">
            {clearedCheques.length}
          </div>
          <div className="text-xs text-slate-600 mt-1 font-medium">
            ₹{formatInr(clearedCheques.reduce((s, c) => s + c.amount, 0))}
          </div>
        </Card>

        {/* Bounced */}
        {bouncedCheques.length > 0 && (
          <Card
            onClick={() => setStatusFilter(statusFilter === "BOUNCED" ? "ALL" : "BOUNCED")}
            className={cn(
              "p-4 cursor-pointer transition-all duration-200 border",
              statusFilter === "BOUNCED"
                ? "ring-2 ring-rose-500 bg-rose-50/50 border-rose-300"
                : "hover:border-rose-300 hover:bg-slate-50"
            )}
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Bounced</span>
              <span className="p-1.5 rounded-lg bg-rose-100 text-rose-700">
                <AlertTriangle className="h-4 w-4" />
              </span>
            </div>
            <div className="mt-2 text-2xl font-black text-rose-700">
              {bouncedCheques.length}
            </div>
            <div className="text-xs text-slate-600 mt-1 font-medium">
              ₹{formatInr(bouncedCheques.reduce((s, c) => s + c.amount, 0))}
            </div>
          </Card>
        )}
      </div>

      {/* Filters Bar */}
      <Card className="p-4 space-y-4">
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
          {/* Search Box */}
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              placeholder="Search by cheque number, bank, party, notes..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9 pr-8"
            />
            {search && (
              <button
                onClick={() => setSearch("")}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </div>

          {/* Party Type Filter */}
          <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-lg">
            {(["ALL", "CUSTOMER", "SUPPLIER"] as const).map((t) => (
              <button
                key={t}
                onClick={() => {
                  setPartyTypeFilter(t)
                  setSelectedPartyId("all")
                }}
                className={cn(
                  "px-3 py-1.5 text-xs font-semibold rounded-md transition-all",
                  partyTypeFilter === t
                    ? "bg-white text-slate-900 shadow-sm"
                    : "text-slate-600 hover:text-slate-900"
                )}
              >
                {t === "ALL" ? "All Parties" : t === "CUSTOMER" ? "Customers" : "Suppliers"}
              </button>
            ))}
          </div>

          {/* Customer / Supplier Wise Filter Dropdown */}
          <div className="min-w-[200px]">
            <select
              value={selectedPartyId}
              onChange={(e) =>
                setSelectedPartyId(e.target.value === "all" ? "all" : Number(e.target.value))
              }
              className="w-full text-sm border border-slate-200 rounded-lg px-3 py-2 bg-white text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option value="all">All Specific Parties (Show All)</option>
              {partyTypeFilter !== "SUPPLIER" && (
                <optgroup label="Customers">
                  {customers.map((c) => (
                    <option key={`c-${c.id}`} value={c.id}>
                      {c.firmName || c.name} {c.city ? `(${c.city})` : ""}
                    </option>
                  ))}
                </optgroup>
              )}
              {partyTypeFilter !== "CUSTOMER" && (
                <optgroup label="Suppliers">
                  {suppliers.map((s) => (
                    <option key={`s-${s.id}`} value={s.id}>
                      {s.brand || s.name} {s.city ? `(${s.city})` : ""}
                    </option>
                  ))}
                </optgroup>
              )}
            </select>
          </div>
        </div>

        {/* Status Pills */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1 pt-1">
          <span className="text-xs font-semibold text-slate-400 mr-1 flex items-center gap-1">
            <Filter className="h-3.5 w-3.5" /> Status:
          </span>
          {[
            { id: "ALL", label: "All Status" },
            { id: "DUE_TODAY", label: "Due Today" },
            { id: "UPCOMING_PDC", label: "Upcoming PDC" },
            { id: "SECURITY", label: "🛡️ Security Cheques" },
            { id: "PENDING", label: "Pending" },
            { id: "DEPOSITED", label: "Deposited" },
            { id: "CLEARED", label: "Cleared" },
            { id: "BOUNCED", label: "Bounced" },
          ].map((pill) => (
            <button
              key={pill.id}
              onClick={() => setStatusFilter(pill.id)}
              className={cn(
                "px-3 py-1 text-xs font-medium rounded-full transition-all whitespace-nowrap",
                statusFilter === pill.id
                  ? pill.id === "DUE_TODAY"
                    ? "bg-red-600 text-white font-bold shadow-sm"
                    : "bg-primary text-white font-semibold shadow-sm"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              )}
            >
              {pill.label}
            </button>
          ))}
        </div>
      </Card>

      {/* Cheques Table & List */}
      <Card className="overflow-hidden border shadow-sm">
        <div className="p-4 border-b bg-slate-50 flex items-center justify-between text-xs font-semibold text-slate-600">
          <span>{filteredCheques.length} Cheque{filteredCheques.length !== 1 ? "s" : ""} Displayed</span>
          <span>
            Total Value:{" "}
            <span className="text-slate-900 font-bold text-sm">
              ₹{formatInr(filteredCheques.reduce((sum, c) => sum + c.amount, 0))}
            </span>
          </span>
        </div>

        {filteredCheques.length === 0 ? (
          <div className="p-12 text-center">
            <Landmark className="h-12 w-12 text-slate-300 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-slate-700">No Cheques Found</h3>
            <p className="text-sm text-slate-500 mt-1 max-w-sm mx-auto">
              {search || statusFilter !== "ALL" || selectedPartyId !== "all"
                ? "Try clearing filters or search terms to see matching cheques."
                : "Record your first cheque by clicking '+ Record New Cheque'."}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50/75 border-b text-xs font-semibold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Cheque No</th>
                  <th className="py-3 px-4">Bank</th>
                  <th className="py-3 px-4">Party</th>
                  <th className="py-3 px-4">Deposit Date</th>
                  <th className="py-3 px-4 text-right">Amount</th>
                  <th className="py-3 px-4 text-center">Status</th>
                  <th className="py-3 px-4 text-right">Quick Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredCheques.map((cheque) => {
                  const isDueToday =
                    cheque.chequeDate === today &&
                    (cheque.status === "Pending" || cheque.status === "Due Today")
                  const isOverdue =
                    cheque.chequeDate < today && cheque.status === "Pending"
                  const isPdc =
                    cheque.chequeDate > today && cheque.status === "Pending"

                  return (
                    <tr
                      key={cheque.id}
                      className={cn(
                        "hover:bg-slate-50/80 transition-colors",
                        isDueToday ? "bg-red-50/40 font-medium" : ""
                      )}
                    >
                      {/* Cheque No */}
                      <td className="py-3.5 px-4 font-mono font-bold text-slate-900">
                        <div className="flex flex-col items-start gap-0.5">
                          <span>{cheque.chequeNo}</span>
                          {(cheque.isSecurityCheque || (cheque.notes && cheque.notes.toLowerCase().includes("security"))) && (
                            <span className="inline-flex items-center gap-1 text-[9px] font-bold px-1.5 py-0.5 rounded bg-amber-50 text-amber-800 border border-amber-300">
                              🛡️ Security
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Bank Name */}
                      <td className="py-3.5 px-4 text-slate-700 font-medium">
                        {cheque.bankName}
                      </td>

                      {/* Party */}
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-1.5">
                          <Badge
                            variant={cheque.partyType === "Customer" ? "default" : "warning"}
                            className="text-[10px] uppercase font-bold py-0.5 px-1.5"
                          >
                            {cheque.partyType}
                          </Badge>
                          <span className="font-semibold text-slate-900">
                            {cheque.partyName}
                          </span>
                        </div>
                        {cheque.notes && (
                          <div className="text-xs text-slate-500 mt-0.5 truncate max-w-[200px]">
                            {cheque.notes}
                          </div>
                        )}
                      </td>

                      {/* Date + Tag */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <div className="flex items-center gap-1.5">
                          <span className={cn(isDueToday ? "text-red-700 font-bold" : "text-slate-800")}>
                            {cheque.chequeDate}
                          </span>
                          {isDueToday && (
                            <span className="px-2 py-0.5 text-[10px] font-black uppercase rounded bg-red-600 text-white animate-pulse">
                              DUE TODAY
                            </span>
                          )}
                          {isPdc && (
                            <span className="px-1.5 py-0.5 text-[10px] font-semibold rounded bg-blue-100 text-blue-700">
                              PDC
                            </span>
                          )}
                          {isOverdue && (
                            <span className="px-1.5 py-0.5 text-[10px] font-bold rounded bg-amber-100 text-amber-800">
                              OVERDUE
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Amount */}
                      <td className="py-3.5 px-4 text-right font-black text-base text-slate-900">
                        ₹{formatInr(cheque.amount)}
                      </td>

                      {/* Status */}
                      <td className="py-3.5 px-4 text-center">
                        <span
                          className={cn(
                            "px-2.5 py-1 text-xs font-bold rounded-full inline-block",
                            cheque.status === "Cleared"
                              ? "bg-emerald-100 text-emerald-800"
                              : cheque.status === "Deposited"
                              ? "bg-blue-100 text-blue-800"
                              : cheque.status === "Bounced"
                              ? "bg-rose-100 text-rose-800"
                              : isDueToday
                              ? "bg-red-600 text-white font-extrabold"
                              : "bg-amber-100 text-amber-800"
                          )}
                        >
                          {cheque.status === "Cleared" && "Cleared ✓"}
                          {cheque.status === "Deposited" && "Deposited"}
                          {cheque.status === "Bounced" && "Bounced ✕"}
                          {cheque.status === "Pending" && (isDueToday ? "Due Today" : "Pending")}
                          {cheque.status === "Due Today" && "Due Today"}
                        </span>
                      </td>

                      {/* Quick Actions */}
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end gap-1.5">
                          {/* Status Actions */}
                          {(cheque.status === "Pending" || cheque.status === "Due Today") && (
                            <>
                              <Button
                                size="sm"
                                variant="outline"
                                className="h-7 text-xs border-blue-300 text-blue-700 hover:bg-blue-50"
                                onClick={() => updateChequePdcStatus(cheque.id, "Deposited")}
                              >
                                Deposited
                              </Button>
                              <Button
                                size="sm"
                                className="h-7 text-xs bg-emerald-600 hover:bg-emerald-700 text-white"
                                onClick={() => updateChequePdcStatus(cheque.id, "Cleared")}
                              >
                                Cleared
                              </Button>
                            </>
                          )}

                          {cheque.status === "Deposited" && (
                            <>
                              <Button
                                size="sm"
                                className="h-7 text-xs bg-emerald-600 hover:bg-emerald-700 text-white"
                                onClick={() => updateChequePdcStatus(cheque.id, "Cleared")}
                              >
                                Mark Cleared
                              </Button>
                              <Button
                                size="sm"
                                variant="outline"
                                className="h-7 text-xs border-rose-300 text-rose-700 hover:bg-rose-50"
                                onClick={() => updateChequePdcStatus(cheque.id, "Bounced")}
                              >
                                Bounced
                              </Button>
                            </>
                          )}

                          {(cheque.status === "Cleared" || cheque.status === "Bounced") && (
                            <Button
                              size="sm"
                              variant="outline"
                              className="h-7 text-xs text-slate-600 hover:bg-slate-100"
                              onClick={() => updateChequePdcStatus(cheque.id, "Pending")}
                            >
                              Reset
                            </Button>
                          )}

                          {/* Edit / Delete buttons */}
                          <button
                            onClick={() => handleOpenEdit(cheque)}
                            className="p-1.5 text-slate-400 hover:text-primary rounded-lg transition-colors"
                            title="Edit Cheque"
                          >
                            <Edit2 className="h-4 w-4" />
                          </button>
                          <button
                            onClick={() => setDeleteConfirmId(cheque.id)}
                            className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg transition-colors"
                            title="Delete Cheque"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Add / Edit Cheque Dialog */}
      <Dialog
        open={isModalOpen}
        onOpenChange={(open) => {
          if (!open) setIsModalOpen(false)
        }}
      >
        <div className="p-6 max-w-lg w-full bg-white rounded-xl shadow-xl space-y-4">
          <div className="flex items-center justify-between border-b pb-3">
            <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
              <Landmark className="h-5 w-5 text-primary" />
              {editingCheque ? "Edit Cheque" : "Record New Cheque (PDC)"}
            </h2>
            <button
              onClick={() => setIsModalOpen(false)}
              className="text-slate-400 hover:text-slate-600"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <form onSubmit={handleSave} className="space-y-4">
            {/* Party Type Toggle: Customer vs Supplier */}
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1.5">
                Party Type *
              </label>
              <div className="grid grid-cols-2 gap-2 bg-slate-100 p-1 rounded-lg">
                <button
                  type="button"
                  onClick={() => {
                    setFormPartyType("Customer")
                    setFormPartyId(0)
                    setFormPartyName("")
                  }}
                  className={cn(
                    "py-1.5 text-xs font-bold rounded-md transition-all flex items-center justify-center gap-1.5",
                    formPartyType === "Customer"
                      ? "bg-white text-indigo-700 shadow-sm"
                      : "text-slate-600 hover:text-slate-900"
                  )}
                >
                  <User className="h-3.5 w-3.5" /> Customer Cheque
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setFormPartyType("Supplier")
                    setFormPartyId(0)
                    setFormPartyName("")
                  }}
                  className={cn(
                    "py-1.5 text-xs font-bold rounded-md transition-all flex items-center justify-center gap-1.5",
                    formPartyType === "Supplier"
                      ? "bg-white text-amber-700 shadow-sm"
                      : "text-slate-600 hover:text-slate-900"
                  )}
                >
                  <Building2 className="h-3.5 w-3.5" /> Supplier Cheque
                </button>
              </div>
            </div>

            {/* Select Party */}
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Select {formPartyType} *
              </label>
              <select
                value={formPartyId || ""}
                onChange={(e) => {
                  const id = Number(e.target.value)
                  setFormPartyId(id)
                  if (formPartyType === "Customer") {
                    const c = customers.find((cust) => cust.id === id)
                    if (c) setFormPartyName(c.firmName || c.name)
                  } else {
                    const s = suppliers.find((sup) => sup.id === id)
                    if (s) setFormPartyName(s.brand || s.name)
                  }
                }}
                className="w-full text-sm border border-slate-200 rounded-lg px-3 py-2 bg-white text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary/20"
                required
              >
                <option value="">-- Choose {formPartyType} --</option>
                {formPartyType === "Customer"
                  ? customers.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.firmName || c.name} {c.city ? `(${c.city})` : ""}
                      </option>
                    ))
                  : suppliers.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.brand || s.name} {s.city ? `(${s.city})` : ""}
                      </option>
                    ))}
              </select>
            </div>

            {/* Cheque No & Bank */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Cheque Number (CH N) *
                </label>
                <Input
                  value={formChequeNo}
                  onChange={(e) => setFormChequeNo(e.target.value)}
                  placeholder="e.g. 000123"
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Bank Name *
                </label>
                <Input
                  value={formBankName}
                  onChange={(e) => setFormBankName(e.target.value)}
                  placeholder="e.g. HDFC Bank, SBI"
                  required
                />
              </div>
            </div>

            {/* Amount & Date */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Amount (₹) *
                </label>
                <Input
                  type="number"
                  value={formAmount}
                  onChange={(e) => setFormAmount(e.target.value)}
                  placeholder="e.g. 50000"
                  required
                />
              </div>
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-semibold text-slate-600">
                    Deposit Date *
                  </label>
                  <button
                    type="button"
                    onClick={() => setFormDate(today)}
                    className="text-[11px] font-bold text-primary hover:underline"
                  >
                    Today
                  </button>
                </div>
                <Input
                  type="date"
                  value={formDate}
                  onChange={(e) => setFormDate(e.target.value)}
                  required
                />
              </div>
            </div>

            {/* Status */}
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Current Status
              </label>
              <select
                value={formStatus}
                onChange={(e) => setFormStatus(e.target.value as ChequeStatus)}
                className="w-full text-sm border border-slate-200 rounded-lg px-3 py-2 bg-white text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary/20"
              >
                <option value="Pending">Pending (Not yet deposited)</option>
                <option value="Deposited">Deposited (Awaiting clearance)</option>
                <option value="Cleared">Cleared (Money credited/debited)</option>
                <option value="Bounced">Bounced (Dishonored)</option>
              </select>
            </div>

            {/* Account Number & Security Cheque Checkbox */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 items-end">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Bank Account Number (Optional)
                </label>
                <Input
                  value={formAccountNumber}
                  onChange={(e) => setFormAccountNumber(e.target.value)}
                  placeholder="Optional A/C Number"
                />
              </div>

              <div className="flex items-center gap-2 p-2 bg-amber-50/70 border border-amber-200 rounded-lg">
                <input
                  type="checkbox"
                  id="security_cheque_chk"
                  checked={formIsSecurityCheque}
                  onChange={(e) => setFormIsSecurityCheque(e.target.checked)}
                  className="h-4 w-4 rounded border-amber-300 text-amber-600 focus:ring-amber-500 cursor-pointer"
                />
                <label htmlFor="security_cheque_chk" className="text-xs font-semibold text-amber-900 cursor-pointer select-none">
                  🛡️ Security Cheque (सुरक्षा चेक)
                </label>
              </div>
            </div>

            {/* Notes */}
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Remarks / Notes
              </label>
              <Input
                value={formNotes}
                onChange={(e) => setFormNotes(e.target.value)}
                placeholder="Optional notes or bill references"
              />
            </div>

            {/* Submit / Cancel buttons */}
            <div className="flex justify-end gap-2 pt-2 border-t">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsModalOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit">
                {editingCheque ? "Update Cheque" : "Save Cheque"}
              </Button>
            </div>
          </form>
        </div>
      </Dialog>

      {/* Delete Confirmation Modal */}
      {deleteConfirmId && (
        <Dialog open={true} onOpenChange={() => setDeleteConfirmId(null)}>
          <div className="p-6 max-w-sm w-full bg-white rounded-xl shadow-xl space-y-4">
            <h3 className="text-lg font-bold text-slate-900">Confirm Cheque Deletion</h3>
            <p className="text-sm text-slate-600">
              Are you sure you want to delete this cheque record? This action will remove it from both Android and Web.
            </p>
            <div className="flex justify-end gap-2 pt-2">
              <Button variant="outline" onClick={() => setDeleteConfirmId(null)}>
                Cancel
              </Button>
              <Button
                className="bg-red-600 hover:bg-red-700 text-white"
                onClick={async () => {
                  await deleteChequePdc(deleteConfirmId)
                  setDeleteConfirmId(null)
                }}
              >
                Delete
              </Button>
            </div>
          </div>
        </Dialog>
      )}
    </div>
  )
}
