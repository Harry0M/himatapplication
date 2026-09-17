import React, { useState, useEffect } from "react"
import { getMasterDraft, saveMasterDraft, clearMasterDraft } from "../lib/masterDrafts"
import {
  UserCheck,
  Plus,
  Phone,
  Mail,
  MapPin,
  Search,
  X,
  Receipt,
  IndianRupee,
  Calendar,
  ExternalLink,
  Briefcase,
  CheckCircle2,
  Clock,
  Truck,
  Edit2,
  Trash2,
  Check,
  ShieldCheck,
  AlertCircle,
  Home,
  User,
  Pause,
  Play,
  RotateCcw,
  UserX
} from "lucide-react"
import { useData, EmployeeStats } from "../context/DataContext"
import { useAuth } from "../context/AuthContext"
import { formatInr, formatDate } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { ReferrerSelectModal } from "../components/ui/ReferrerSelectModal"
import { Tabs } from "../components/ui/Tabs"
import { Employee } from "../types"
import { AHMEDABAD_TEXTILE_MARKETS } from "../lib/constants"
import { StaffDetailView } from "./StaffDetailView"

interface EmployeesViewProps {
  onNavigate?: (tab: string) => void
}

export function EmployeesView({ onNavigate }: EmployeesViewProps) {
  const {
    employees,
    customers,
    suppliers,
    markets,
    allEmployeeStats,
    saveEmployee,
    deleteEmployee,
    suspendEmployee,
    resumeEmployee,
    deactivateEmployee,
    setSelectedEmployeeId,
    selectedEmployeeId,
  } = useData()
  const { isAdmin } = useAuth()

  const availableMarkets = React.useMemo(() => {
    const list = [...AHMEDABAD_TEXTILE_MARKETS]
    markets.forEach((m) => {
      if (m.marketName && !list.includes(m.marketName)) {
        list.push(m.marketName)
      }
    })
    return list
  }, [markets])

  const [search, setSearch] = useState<string>("")
  const [showSearch, setShowSearch] = useState<boolean>(false)
  const [roleFilter, setRoleFilter] = useState<string>("all")
  const [statusFilter, setStatusFilter] = useState<string>("all")

  // Modal States
  const [isDialogOpen, setIsDialogOpen] = useState<boolean>(false)
  const [activeFormTab, setActiveFormTab] = useState<string>("basic")
  const [editingId, setEditingId] = useState<number | null>(null)

  // Form State
  const [name, setName] = useState<string>("")
  const [employeeId, setEmployeeId] = useState<string>("")
  const [role, setRole] = useState<string>("Salesman")

  // Phones (Up to 5)
  const [phone1, setPhone1] = useState<string>("")
  const [phone2, setPhone2] = useState<string>("")
  const [phone3, setPhone3] = useState<string>("")
  const [phone4, setPhone4] = useState<string>("")
  const [phone5, setPhone5] = useState<string>("")
  const [phoneCount, setPhoneCount] = useState<number>(1)

  // Emails
  const [email, setEmail] = useState<string>("")
  const [alternateEmail, setAlternateEmail] = useState<string>("")

  // Addresses & Native Place
  const [currentAddress, setCurrentAddress] = useState<string>("")
  const [permanentAddress, setPermanentAddress] = useState<string>("")
  const [personalLocation, setPersonalLocation] = useState<string>("")

  // Emergency Contact & Reference
  const [emergencyContactName, setEmergencyContactName] = useState<string>("")
  const [emergencyContactPhone, setEmergencyContactPhone] = useState<string>("")
  const [referredBy, setReferredBy] = useState<string>("")

  // Assigned Markets
  const [selectedMarkets, setSelectedMarkets] = useState<string[]>([])

  // Drill-down Profile & Full-Page View
  const [selectedStaffId, setSelectedStaffId] = useState<number | null>(null)

  // Draft state (strictly local browser storage)
  const [hasDraft, setHasDraft] = useState<boolean>(false)

  // Open Add Modal
  const handleOpenAdd = () => {
    setEditingId(null)
    const draft = getMasterDraft<any>("employee")
    if (draft) {
      setName(draft.name || "")
      setEmployeeId(draft.employeeId || `EMP-0${employees.length + 1}`)
      setRole(draft.role || "Salesman")
      setPhone1(draft.phone1 || "")
      setPhone2(draft.phone2 || "")
      setPhone3(draft.phone3 || "")
      setPhone4(draft.phone4 || "")
      setPhone5(draft.phone5 || "")
      setPhoneCount(draft.phoneCount || 1)
      setEmail(draft.email || "")
      setAlternateEmail(draft.alternateEmail || "")
      setCurrentAddress(draft.currentAddress || "")
      setPermanentAddress(draft.permanentAddress || "")
      setPersonalLocation(draft.personalLocation || "")
      setEmergencyContactName(draft.emergencyContactName || "")
      setEmergencyContactPhone(draft.emergencyContactPhone || "")
      setReferredBy(draft.referredBy || "")
      setSelectedMarkets(draft.selectedMarkets || ["New Cloth Market (Raipur)"])
      setHasDraft(true)
    } else {
      setName("")
      setEmployeeId(`EMP-0${employees.length + 1}`)
      setRole("Salesman")
      setPhone1("")
      setPhone2("")
      setPhone3("")
      setPhone4("")
      setPhone5("")
      setPhoneCount(1)
      setEmail("")
      setAlternateEmail("")
      setCurrentAddress("")
      setPermanentAddress("")
      setPersonalLocation("")
      setEmergencyContactName("")
      setEmergencyContactPhone("")
      setReferredBy("")
      setSelectedMarkets(["New Cloth Market (Raipur)"])
      setHasDraft(false)
    }
    setActiveFormTab("basic")
    setIsDialogOpen(true)
  }

  const handleDiscardDraft = () => {
    clearMasterDraft("employee")
    setHasDraft(false)
    setName("")
    setEmployeeId(`EMP-0${employees.length + 1}`)
    setRole("Salesman")
    setPhone1("")
    setPhone2("")
    setPhone3("")
    setPhone4("")
    setPhone5("")
    setPhoneCount(1)
    setEmail("")
    setAlternateEmail("")
    setCurrentAddress("")
    setPermanentAddress("")
    setPersonalLocation("")
    setEmergencyContactName("")
    setEmergencyContactPhone("")
    setReferredBy("")
    setSelectedMarkets(["New Cloth Market (Raipur)"])
  }

  // Auto-save local draft
  useEffect(() => {
    if (!isDialogOpen || editingId !== null) return
    if (name.trim() || phone1.trim() || email.trim()) {
      saveMasterDraft("employee", {
        name,
        employeeId,
        role,
        phone1,
        phone2,
        phone3,
        phone4,
        phone5,
        phoneCount,
        email,
        alternateEmail,
        currentAddress,
        permanentAddress,
        personalLocation,
        emergencyContactName,
        emergencyContactPhone,
        referredBy,
        selectedMarkets,
      })
    }
  }, [
    isDialogOpen,
    editingId,
    name,
    employeeId,
    role,
    phone1,
    phone2,
    phone3,
    phone4,
    phone5,
    phoneCount,
    email,
    alternateEmail,
    currentAddress,
    permanentAddress,
    personalLocation,
    emergencyContactName,
    emergencyContactPhone,
    referredBy,
    selectedMarkets,
  ])

  // Open Edit Modal
  const handleOpenEdit = (emp: Employee) => {
    setEditingId(emp.id)
    setName(emp.name || "")
    setEmployeeId(emp.employeeId || `EMP-${emp.id}`)
    setRole(emp.role || "Salesman")

    setPhone1(emp.phone || "")
    setPhone2(emp.phone2 || "")
    setPhone3(emp.phone3 || "")
    setPhone4(emp.phone4 || "")
    setPhone5(emp.phone5 || "")
    const count = [emp.phone, emp.phone2, emp.phone3, emp.phone4, emp.phone5].filter(Boolean).length
    setPhoneCount(Math.max(1, count))

    setEmail(emp.email || "")
    setAlternateEmail(emp.alternateEmail || "")
    setCurrentAddress(emp.currentAddress || emp.address || "")
    setPermanentAddress(emp.permanentAddress || "")
    setPersonalLocation(emp.personalLocation || "")
    setEmergencyContactName(emp.emergencyContactName || "")
    setEmergencyContactPhone(emp.emergencyContactPhone || "")
    setReferredBy(emp.referredBy || "")

    const mkts = emp.assignedMarkets
      ? emp.assignedMarkets.split(",").map((m) => m.trim()).filter(Boolean)
      : []
    setSelectedMarkets(mkts)
    setActiveFormTab("basic")
    setIsDialogOpen(true)
  }

  const handleToggleMarket = (mkt: string) => {
    if (selectedMarkets.includes(mkt)) {
      setSelectedMarkets(selectedMarkets.filter((m) => m !== mkt))
    } else {
      setSelectedMarkets([...selectedMarkets, mkt])
    }
  }

  const handleSaveEmployee = async () => {
    if (!isAdmin) {
      alert("Permission Denied: Only Super Admins / Owners can create or edit employee accounts.")
      return
    }
    if (!name.trim()) return
    const id = editingId || Date.now()

    const allPhones = [phone1.trim(), phone2.trim(), phone3.trim(), phone4.trim(), phone5.trim()].filter(Boolean)
    const primaryPhone = allPhones[0] || ""

    const allEmails = [email.trim(), alternateEmail.trim()].filter(Boolean)

    const newEmp: Employee = {
      id,
      employeeId: employeeId.trim() || `EMP-0${employees.length + 1}`,
      name: name.trim(),
      role: role.trim() || "Salesman",
      phone: primaryPhone,
      phone2: phone2.trim() || "",
      phone3: phone3.trim() || "",
      phone4: phone4.trim() || "",
      phone5: phone5.trim() || "",
      phones: allPhones,
      email: email.trim().toLowerCase() || "",
      alternateEmail: alternateEmail.trim().toLowerCase() || "",
      emails: allEmails,
      address: currentAddress.trim() || "",
      currentAddress: currentAddress.trim() || "",
      permanentAddress: permanentAddress.trim() || "",
      personalLocation: personalLocation.trim() || "",
      emergencyContactName: emergencyContactName.trim() || "",
      emergencyContactPhone: emergencyContactPhone.trim() || "",
      referredBy: referredBy.trim() || "",
      assignedMarkets: selectedMarkets.join(", "),
      markets: selectedMarkets.join(", "),
    }

    await saveEmployee(newEmp)
    clearMasterDraft("employee")
    setHasDraft(false)
    setIsDialogOpen(false)
  }

  const handleToggleAccess = async (emp: Employee) => {
    if (!isAdmin) return
    const isSuspended = emp.isBlocked || emp.status === "Suspended"
    if (isSuspended) {
      await resumeEmployee(emp.id)
    } else {
      if (window.confirm(`Stop Android mobile access for ${emp.name}? All historical visits, orders, and sales will remain 100% safe.`)) {
        await suspendEmployee(emp.id, "Access suspended by Administrator")
      }
    }
  }

  const handleReactivateStaff = async (empId: number) => {
    if (!isAdmin) return
    await resumeEmployee(empId)
  }

  const handleDelete = async (id: number) => {
    if (!isAdmin) {
      alert("Permission Denied: Only Super Admins / Owners can deactivate employee accounts.")
      return
    }
    if (window.confirm("Are you sure you want to deactivate this staff profile? All their historical trips, orders, and sales data will remain 100% intact.")) {
      await deleteEmployee(id)
      if (selectedStaffId === id) {
        setSelectedStaffId(null)
      }
    }
  }

  // Filter Search & Status
  const q = search.trim().toLowerCase()
  const filteredStats = allEmployeeStats.filter((stat) => {
    const emp = stat.employee
    const isEmpDeactivated = Boolean(emp.isDeleted || emp.status === "Deactivated")
    const isEmpSuspended = Boolean(!isEmpDeactivated && (emp.isBlocked || emp.status === "Suspended"))
    const isEmpActive = !isEmpDeactivated && !isEmpSuspended

    const matchesSearch =
      !q ||
      emp.name.toLowerCase().includes(q) ||
      emp.employeeId?.toLowerCase().includes(q) ||
      emp.phone?.includes(q) ||
      emp.phone2?.includes(q) ||
      emp.phone3?.includes(q) ||
      emp.phone4?.includes(q) ||
      emp.phone5?.includes(q) ||
      emp.email?.toLowerCase().includes(q) ||
      emp.assignedMarkets?.toLowerCase().includes(q) ||
      emp.personalLocation?.toLowerCase().includes(q) ||
      emp.referredBy?.toLowerCase().includes(q)

    const matchesRole =
      roleFilter === "all" || emp.role.toLowerCase() === roleFilter.toLowerCase()

    const matchesStatus =
      statusFilter === "all" ||
      (statusFilter === "active" && isEmpActive) ||
      (statusFilter === "suspended" && isEmpSuspended) ||
      (statusFilter === "deactivated" && isEmpDeactivated)

    return matchesSearch && matchesRole && matchesStatus
  })

  // Global aggregate stats
  const totalVisitsAcrossAll = allEmployeeStats.reduce((sum, s) => sum + s.totalVisitsCount, 0)
  const activeAgentsCount = allEmployeeStats.filter((s) => s.activeVisitsCount > 0).length
  const totalSalesVolume = allEmployeeStats.reduce((sum, s) => sum + s.totalInvoiced, 0)

  const activeStaffCount = employees.filter((e) => !e.isDeleted && !e.isBlocked && e.status !== "Suspended" && e.status !== "Deactivated").length
  const suspendedStaffCount = employees.filter((e) => !e.isDeleted && (e.isBlocked || e.status === "Suspended")).length
  const deactivatedStaffCount = employees.filter((e) => e.isDeleted || e.status === "Deactivated").length

  const handleFilterWork = (empId: number, targetTab: string = "trips") => {
    setSelectedEmployeeId(empId)
    if (onNavigate) {
      onNavigate(targetTab)
    }
  }

  return (
    <div className="space-y-6">
      {selectedStaffId !== null ? (
        <StaffDetailView
          employeeId={selectedStaffId}
          onBack={() => setSelectedStaffId(null)}
          onEdit={(emp) => handleOpenEdit(emp)}
          onNavigate={onNavigate}
        />
      ) : (
        <>
          {/* Header with Search, Role Tabs & Add Button */}
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
            <span>Staff & Field Agents Directory</span>
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            {employees.length} team members managing buyer market visits and procurement.
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

          {/* Role Filter Tabs */}
          <Tabs
            value={roleFilter}
            onValueChange={setRoleFilter}
            options={[
              { value: "all", label: "All Roles", count: employees.length },
              {
                value: "salesman",
                label: "Salesmen",
                count: employees.filter((e) => e.role?.toLowerCase() === "salesman").length,
              },
              {
                value: "admin",
                label: "Admins",
                count: employees.filter((e) => e.role?.toLowerCase() === "admin").length,
              },
            ]}
          />

          {/* Status Filter Tabs */}
          <Tabs
            value={statusFilter}
            onValueChange={setStatusFilter}
            options={[
              { value: "all", label: "All Status", count: employees.length },
              { value: "active", label: "Active", count: activeStaffCount },
              { value: "suspended", label: "Suspended", count: suspendedStaffCount },
              { value: "deactivated", label: "Deactivated", count: deactivatedStaffCount },
            ]}
          />

          {isAdmin && (
            <Button
              shape="pill"
              size="sm"
              onClick={handleOpenAdd}
              className="h-8 shadow-sm font-semibold text-xs"
            >
              <Plus className="mr-1.5 h-3.5 w-3.5" />
              Add Staff
            </Button>
          )}
        </div>
      </div>

      {/* Expandable Search Input */}
      {showSearch && (
        <div className="relative">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by staff name, EMP-ID, any of 5 phones, email, assigned market..."
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

      {/* Top 4 Performance Overview Mini-Cards */}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="p-4 rounded-2xl">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>Total Team</span>
            <UserCheck className="h-4 w-4 text-zinc-500" />
          </div>
          <div className="mt-2 text-xl font-bold text-zinc-900 dark:text-zinc-50">
            {employees.length} Members
          </div>
          <p className="text-[10px] text-muted-foreground mt-0.5">
            {employees.filter((e) => e.role === "Salesman").length} Field Sales Agents
          </p>
        </Card>

        <Card className="p-4 rounded-2xl">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>Active in Field</span>
            <Clock className="h-4 w-4 text-emerald-500" />
          </div>
          <div className="mt-2 text-xl font-bold text-emerald-600 dark:text-emerald-400">
            {activeAgentsCount} Agents
          </div>
          <p className="text-[10px] text-muted-foreground mt-0.5">Currently on market trips</p>
        </Card>

        <Card className="p-4 rounded-2xl">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>Total Trips Completed</span>
            <MapPin className="h-4 w-4 text-blue-500" />
          </div>
          <div className="mt-2 text-xl font-bold text-zinc-900 dark:text-zinc-50">
            {totalVisitsAcrossAll} Trips
          </div>
          <p className="text-[10px] text-muted-foreground mt-0.5">Across all client accounts</p>
        </Card>

        <Card className="p-4 rounded-2xl">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>Team Sales Value</span>
            <IndianRupee className="h-4 w-4 text-amber-500" />
          </div>
          <div className="mt-2 text-xl font-bold text-zinc-900 dark:text-zinc-50">
            ₹{formatInr(totalSalesVolume)}
          </div>
          <p className="text-[10px] text-muted-foreground mt-0.5">Cumulative bookings</p>
        </Card>
      </div>

      {/* Grid of Employee Performance Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {filteredStats.length === 0 ? (
          <div className="col-span-full py-12 text-center text-xs text-muted-foreground">
            No employees found matching filter.
          </div>
        ) : (
          filteredStats.map((stat) => {
            const { employee: emp } = stat
            const isSelected = selectedEmployeeId === emp.id
            const extraPhonesCount = [emp.phone2, emp.phone3, emp.phone4, emp.phone5].filter(Boolean).length
            const isEmpDeactivated = Boolean(emp.isDeleted || emp.status === "Deactivated")
            const isEmpSuspended = Boolean(!isEmpDeactivated && (emp.isBlocked || emp.status === "Suspended"))
            const isEmpActive = !isEmpDeactivated && !isEmpSuspended

            return (
              <Card
                key={emp.id}
                className={`p-5 rounded-2xl transition-all flex flex-col justify-between ${
                  isSelected
                    ? "border-zinc-900 dark:border-zinc-100 ring-2 ring-zinc-900/10 dark:ring-zinc-100/10"
                    : "hover:border-zinc-300 dark:hover:border-zinc-700"
                }`}
              >
                <div>
                  {/* Employee Header */}
                  <div className="flex items-start justify-between">
                    <div
                      className="flex items-center gap-3 cursor-pointer group"
                      onClick={() => setSelectedStaffId(emp.id)}
                    >
                      <div className="flex h-10 w-10 items-center justify-center rounded-full bg-zinc-900 text-xs font-bold text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm group-hover:scale-105 transition-transform">
                        {emp.name.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-100 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                          {emp.name}
                        </h3>
                        <div className="flex items-center gap-1.5 mt-0.5">
                          <span className="font-mono text-[10px] text-muted-foreground">
                            {emp.employeeId || `EMP-${emp.id}`}
                          </span>
                          <span>•</span>
                          <span className="text-[11px] text-muted-foreground">{emp.role}</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 flex-wrap justify-end">
                      {isEmpDeactivated ? (
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-400 border border-rose-200 dark:border-rose-900">
                          Deactivated
                        </span>
                      ) : isEmpSuspended ? (
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-400 border border-amber-200 dark:border-amber-900">
                          Suspended
                        </span>
                      ) : (
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-900">
                          Active
                        </span>
                      )}

                      <Badge
                        variant={
                          stat.activeVisitsCount > 0
                            ? "success"
                            : emp.role === "Admin"
                            ? "default"
                            : "secondary"
                        }
                      >
                        {stat.activeVisitsCount > 0 ? "In Field" : emp.role}
                      </Badge>

                      <Button
                        size="sm"
                        shape="pill"
                        variant="ghost"
                        onClick={() => handleOpenEdit(emp)}
                        className="h-6 w-6 p-0 text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100"
                        title="Edit Employee Profile"
                      >
                        <Edit2 className="h-3 w-3" />
                      </Button>
                    </div>
                  </div>

                  {/* Assigned Ahmedabad Markets Badge */}
                  {emp.assignedMarkets && (
                    <div className="mt-2.5 flex items-center gap-1 text-[11px] text-zinc-600 dark:text-zinc-400">
                      <MapPin className="h-3 w-3 text-zinc-400 shrink-0" />
                      <span className="truncate" title={emp.assignedMarkets}>
                        {emp.assignedMarkets.split(",")[0]}
                        {emp.assignedMarkets.split(",").length > 1
                          ? ` (+${emp.assignedMarkets.split(",").length - 1} markets)`
                          : ""}
                      </span>
                    </div>
                  )}

                  {/* Contact Info & 5 Phones */}
                  <div className="mt-3 space-y-1 text-xs text-muted-foreground border-t border-zinc-100 dark:border-zinc-800/80 pt-2.5">
                    {emp.phone ? (
                      <div className="flex items-center justify-between">
                        <p className="flex items-center gap-1.5">
                          <Phone className="h-3 w-3 text-zinc-400" />
                          <span className="text-zinc-800 dark:text-zinc-200 font-medium">{emp.phone}</span>
                        </p>
                        {extraPhonesCount > 0 && (
                          <span className="text-[10px] font-semibold bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-300 px-1.5 py-0.5 rounded-full">
                            +{extraPhonesCount} more
                          </span>
                        )}
                      </div>
                    ) : (
                      <p className="text-[11px] italic">No phone registered</p>
                    )}

                    {emp.email && (
                      <p className="flex items-center gap-1.5 truncate">
                        <Mail className="h-3 w-3 text-zinc-400" />
                        <span>{emp.email}</span>
                      </p>
                    )}

                    {emp.emergencyContactPhone && (
                      <p className="text-[10px] text-amber-600 dark:text-amber-400 font-medium">
                        Emergency: {emp.emergencyContactName || "Contact"} ({emp.emergencyContactPhone})
                      </p>
                    )}
                  </div>

                  {/* 4-Tile Performance Snapshot */}
                  <div
                    onClick={() => setSelectedStaffId(emp.id)}
                    className="mt-3 grid grid-cols-2 gap-2 rounded-xl bg-zinc-50 dark:bg-zinc-900/50 p-2.5 text-xs cursor-pointer hover:bg-zinc-100 dark:hover:bg-zinc-800/60 transition-colors"
                    title="Click to open comprehensive staff analytics & work history"
                  >
                    <div>
                      <span className="text-[10px] text-muted-foreground">Trips</span>
                      <p className="font-bold text-zinc-900 dark:text-zinc-100">
                        {stat.totalVisitsCount}{" "}
                        {stat.activeVisitsCount > 0 && (
                          <span className="text-emerald-600 dark:text-emerald-400 font-medium text-[10px]">
                            ({stat.activeVisitsCount} active)
                          </span>
                        )}
                      </p>
                    </div>
                    <div>
                      <span className="text-[10px] text-muted-foreground">Orders Booked</span>
                      <p className="font-bold text-zinc-900 dark:text-zinc-100">
                        {stat.totalOrdersCount}{" "}
                        <span className="text-[10px] text-muted-foreground font-normal">
                          ({stat.totalPieces} pcs)
                        </span>
                      </p>
                    </div>
                    <div>
                      <span className="text-[10px] text-muted-foreground">Total Invoiced</span>
                      <p className="font-bold text-zinc-900 dark:text-zinc-100">
                        ₹{formatInr(stat.totalInvoiced)}
                      </p>
                    </div>
                    <div>
                      <span className="text-[10px] text-muted-foreground">Pending Dues</span>
                      <p
                        className={`font-bold ${
                          stat.totalDues > 0
                            ? "text-red-600 dark:text-red-400"
                            : "text-zinc-900 dark:text-zinc-100"
                        }`}
                      >
                        ₹{formatInr(stat.totalDues)}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Actions */}
                <div className="mt-3.5 flex items-center gap-1.5 flex-wrap">
                  <Button
                    size="sm"
                    shape="pill"
                    variant="outline"
                    onClick={() => {
                      setSelectedStaffId(emp.id)
                    }}
                    className="flex-1 text-xs font-semibold h-7"
                  >
                    View Analytics & Work
                  </Button>

                  {isAdmin && (
                    <>
                      {isEmpActive && (
                        <Button
                          size="sm"
                          shape="pill"
                          variant="ghost"
                          onClick={(e) => {
                            e.stopPropagation()
                            handleToggleAccess(emp)
                          }}
                          className="h-7 px-2 text-xs font-semibold text-amber-700 hover:text-amber-800 hover:bg-amber-50 dark:text-amber-400 dark:hover:bg-amber-950/50"
                          title="Stop Android Access (Suspend)"
                        >
                          <Pause className="h-3 w-3 mr-1" />
                          Stop
                        </Button>
                      )}
                      {isEmpSuspended && (
                        <Button
                          size="sm"
                          shape="pill"
                          variant="ghost"
                          onClick={(e) => {
                            e.stopPropagation()
                            handleToggleAccess(emp)
                          }}
                          className="h-7 px-2 text-xs font-semibold text-emerald-700 hover:text-emerald-800 hover:bg-emerald-50 dark:text-emerald-400 dark:hover:bg-emerald-950/50"
                          title="Resume Android Access"
                        >
                          <Play className="h-3 w-3 mr-1" />
                          Resume
                        </Button>
                      )}
                      {isEmpDeactivated && (
                        <Button
                          size="sm"
                          shape="pill"
                          variant="ghost"
                          onClick={(e) => {
                            e.stopPropagation()
                            handleReactivateStaff(emp.id)
                          }}
                          className="h-7 px-2 text-xs font-semibold text-blue-700 hover:text-blue-800 hover:bg-blue-50 dark:text-blue-400 dark:hover:bg-blue-950/50"
                          title="Reactivate Staff Account"
                        >
                          <RotateCcw className="h-3 w-3 mr-1" />
                          Reactivate
                        </Button>
                      )}
                    </>
                  )}

                  <Button
                    size="sm"
                    shape="pill"
                    variant={isSelected ? "default" : "secondary"}
                    onClick={() => {
                      if (isSelected) {
                        setSelectedEmployeeId("all")
                      } else {
                        handleFilterWork(emp.id, "trips")
                      }
                    }}
                    className="text-xs font-semibold h-7 px-3"
                    title={isSelected ? "Clear Employee Filter" : "Filter Trips & Orders by this Agent"}
                  >
                    {isSelected ? "Filtered ✓" : "Filter Work"}
                  </Button>
                </div>
              </Card>
            )
          })
        )}
      </div>

        </>
      )}

      {/* Add / Edit Staff Multi-Tab Dialog */}
      <Dialog
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
        title={editingId ? "Edit Staff / Agent Profile" : "Register Sales Agent / Staff"}
        description="Comprehensive team member profile with contacts, emergency numbers, and market territories"
      >
        <div className="space-y-4 pt-1">
          {/* Draft Notification Banner */}
          {hasDraft && !editingId && (
            <div className="flex items-center justify-between rounded-lg border border-emerald-200 bg-emerald-50 px-3.5 py-2 text-xs text-emerald-800">
              <div className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-emerald-600 shrink-0" />
                <span className="font-medium">Resumed from your local draft</span>
              </div>
              <button
                type="button"
                onClick={handleDiscardDraft}
                className="font-semibold text-rose-600 hover:text-rose-700 hover:underline cursor-pointer"
              >
                Discard Draft
              </button>
            </div>
          )}

          {/* Form Tabs */}
          <Tabs
            value={activeFormTab}
            onValueChange={setActiveFormTab}
            options={[
              { value: "basic", label: "1. Staff & Role" },
              { value: "contact", label: "2. Contact & 5 Phones" },
              { value: "address", label: "3. Address & Native" },
              { value: "markets", label: "4. Markets & Emergency" },
            ]}
          />

          {/* TAB 1: Staff & Role */}
          {activeFormTab === "basic" && (
            <div className="space-y-3.5">
              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Full Name <span className="text-red-500">*</span>
                </label>
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Sunil Verma"
                  className="mt-1"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Staff ID
                  </label>
                  <Input
                    value={employeeId}
                    onChange={(e) => setEmployeeId(e.target.value)}
                    placeholder="e.g. EMP-02"
                    className="mt-1"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    System Role
                  </label>
                  <div className="mt-1 flex gap-2">
                    {["Salesman", "Admin"].map((r) => (
                      <Button
                        key={r}
                        size="sm"
                        shape="pill"
                        variant={role === r ? "default" : "outline"}
                        onClick={() => setRole(r)}
                        className="flex-1 text-xs"
                      >
                        {r}
                      </Button>
                    ))}
                  </div>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Google Account Email (Used for login sync)
                </label>
                <Input
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="e.g. sunil.salesman@gmail.com"
                  className="mt-1"
                />
              </div>
            </div>
          )}

          {/* TAB 2: Contact & Up to 5 Phones */}
          {activeFormTab === "contact" && (
            <div className="space-y-3.5 max-h-[350px] overflow-y-auto pr-1">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Contact Phone Numbers (Up to 5)
                </label>
                {phoneCount < 5 && (
                  <Button
                    size="sm"
                    variant="outline"
                    shape="pill"
                    onClick={() => setPhoneCount((prev) => Math.min(5, prev + 1))}
                    className="h-6 text-[11px] px-2"
                  >
                    <Plus className="h-3 w-3 mr-1" /> Add Another Phone
                  </Button>
                )}
              </div>

              {/* Phone 1 */}
              <div>
                <label className="text-[11px] text-muted-foreground">
                  Phone 1 (Primary / WhatsApp) <span className="text-red-500">*</span>
                </label>
                <Input
                  value={phone1}
                  onChange={(e) => setPhone1(e.target.value)}
                  placeholder="e.g. 9819922334"
                  className="mt-1"
                />
              </div>

              {/* Phone 2 */}
              {phoneCount >= 2 && (
                <div>
                  <label className="text-[11px] text-muted-foreground">
                    Phone 2 (Alternate Calling)
                  </label>
                  <Input
                    value={phone2}
                    onChange={(e) => setPhone2(e.target.value)}
                    placeholder="e.g. 9819922335"
                    className="mt-1"
                  />
                </div>
              )}

              {/* Phone 3 */}
              {phoneCount >= 3 && (
                <div>
                  <label className="text-[11px] text-muted-foreground">
                    Phone 3 (Family / Home)
                  </label>
                  <Input
                    value={phone3}
                    onChange={(e) => setPhone3(e.target.value)}
                    placeholder="e.g. 9819922336"
                    className="mt-1"
                  />
                </div>
              )}

              {/* Phone 4 */}
              {phoneCount >= 4 && (
                <div>
                  <label className="text-[11px] text-muted-foreground">
                    Phone 4 (Emergency Line)
                  </label>
                  <Input
                    value={phone4}
                    onChange={(e) => setPhone4(e.target.value)}
                    placeholder="e.g. 9819922337"
                    className="mt-1"
                  />
                </div>
              )}

              {/* Phone 5 */}
              {phoneCount >= 5 && (
                <div>
                  <label className="text-[11px] text-muted-foreground">
                    Phone 5 (Other)
                  </label>
                  <Input
                    value={phone5}
                    onChange={(e) => setPhone5(e.target.value)}
                    placeholder="e.g. 9819922338"
                    className="mt-1"
                  />
                </div>
              )}

              {/* Alternate Email */}
              <div className="pt-2 border-t border-zinc-100 dark:border-zinc-800">
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Alternate / Personal Email
                </label>
                <Input
                  value={alternateEmail}
                  onChange={(e) => setAlternateEmail(e.target.value)}
                  placeholder="e.g. sunil.personal@outlook.com"
                  className="mt-1 text-xs"
                />
              </div>
            </div>
          )}

          {/* TAB 3: Address & Native Place */}
          {activeFormTab === "address" && (
            <div className="space-y-3.5 max-h-[350px] overflow-y-auto pr-1">
              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Current Residential Address (Ahmedabad)
                </label>
                <Input
                  value={currentAddress}
                  onChange={(e) => setCurrentAddress(e.target.value)}
                  placeholder="e.g. Flat 302, Royal Residency, Vastral, Ahmedabad"
                  className="mt-1"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Permanent / Home Town Address
                </label>
                <Input
                  value={permanentAddress}
                  onChange={(e) => setPermanentAddress(e.target.value)}
                  placeholder="e.g. Village Mandvi, Taluka Bhuj, Kutch"
                  className="mt-1"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Personal Location / Native Town
                </label>
                <Input
                  value={personalLocation}
                  onChange={(e) => setPersonalLocation(e.target.value)}
                  placeholder="e.g. Vastral / Maninagar / Kutch"
                  className="mt-1"
                />
              </div>
            </div>
          )}

          {/* TAB 4: Assigned Ahmedabad Markets & Emergency Contact */}
          {activeFormTab === "markets" && (
            <div className="space-y-3.5 max-h-[350px] overflow-y-auto pr-1">
              <div>
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Assigned Ahmedabad Markets
                  </label>
                  <span className="text-[10px] text-muted-foreground">Select markets assigned to this agent</span>
                </div>
                <div className="mt-2 flex flex-wrap gap-1.5 max-h-32 overflow-y-auto p-2 rounded-xl bg-zinc-50 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800">
                  {availableMarkets.map((mkt) => {
                    const isSelected = selectedMarkets.includes(mkt)
                    return (
                      <button
                        key={mkt}
                        type="button"
                        onClick={() => handleToggleMarket(mkt)}
                        className={`text-[11px] px-2.5 py-1 rounded-full border transition-all flex items-center gap-1 ${
                          isSelected
                            ? "bg-zinc-900 text-white border-zinc-900 dark:bg-zinc-100 dark:text-zinc-900 dark:border-zinc-100 font-semibold shadow-sm"
                            : "bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 border-zinc-200 dark:border-zinc-700 hover:border-zinc-400"
                        }`}
                      >
                        {isSelected && <Check className="h-2.5 w-2.5" />}
                        <span>{mkt}</span>
                      </button>
                    )
                  })}
                </div>
              </div>

              <div className="pt-2 border-t border-zinc-100 dark:border-zinc-800 space-y-3">
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Emergency Contact Details
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-[11px] text-muted-foreground">Emergency Contact Name</label>
                    <Input
                      value={emergencyContactName}
                      onChange={(e) => setEmergencyContactName(e.target.value)}
                      placeholder="e.g. Rajesh Verma (Brother)"
                      className="mt-1 text-xs"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] text-muted-foreground">Emergency Contact Phone</label>
                    <Input
                      value={emergencyContactPhone}
                      onChange={(e) => setEmergencyContactPhone(e.target.value)}
                      placeholder="e.g. 9819900000"
                      className="mt-1 text-xs"
                    />
                  </div>
                </div>
              </div>

              <div>
                <ReferrerSelectModal
                  value={referredBy}
                  onChange={setReferredBy}
                  employees={employees}
                  customers={customers}
                  suppliers={suppliers}
                  label="Referred By / Reference Person"
                />
              </div>
            </div>
          )}

          {/* Dialog Bottom Action Buttons */}
          <div className="flex items-center justify-between pt-3 border-t border-zinc-100 dark:border-zinc-800">
            <div className="flex gap-1.5">
              {activeFormTab !== "basic" && (
                <Button
                  variant="outline"
                  shape="pill"
                  size="sm"
                  onClick={() => {
                    if (activeFormTab === "markets") setActiveFormTab("address")
                    else if (activeFormTab === "address") setActiveFormTab("contact")
                    else if (activeFormTab === "contact") setActiveFormTab("basic")
                  }}
                  className="h-8 text-xs"
                >
                  Previous
                </Button>
              )}
              {activeFormTab !== "markets" && (
                <Button
                  variant="secondary"
                  shape="pill"
                  size="sm"
                  onClick={() => {
                    if (activeFormTab === "basic") setActiveFormTab("contact")
                    else if (activeFormTab === "contact") setActiveFormTab("address")
                    else if (activeFormTab === "address") setActiveFormTab("markets")
                  }}
                  className="h-8 text-xs"
                >
                  Next Step
                </Button>
              )}
            </div>

            <div className="flex gap-2">
              <Button
                variant="outline"
                shape="pill"
                size="sm"
                onClick={() => setIsDialogOpen(false)}
                className="h-8 text-xs"
              >
                Cancel
              </Button>
              <Button
                shape="pill"
                size="sm"
                onClick={handleSaveEmployee}
                className="h-8 text-xs font-semibold shadow-sm"
              >
                {editingId ? "Update Staff" : "Save Staff Member"}
              </Button>
            </div>
          </div>
        </div>
      </Dialog>
    </div>
  )
}
