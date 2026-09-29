import React, { useMemo, useState } from "react"
import {
  MapPin,
  Plus,
  Search,
  X,
  UserCheck,
  UserPlus,
  FileText,
  Printer,
  Pencil,
  Lock,
  Unlock,
  LogIn,
  Users,
  Filter,
} from "lucide-react"
import { useData } from "../context/DataContext"
import { useAuth } from "../context/AuthContext"
import { formatDate, formatInr } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Tabs } from "../components/ui/Tabs"
import { FileUpload } from "../components/ui/FileUpload"
import { DateRangeFilter } from "../components/ui/DateRangeFilter"
import { Visit, PurchaseEntry, Supplier, Customer, Employee } from "../types"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateCustomerDayReportHtml,
  buildCustomerReportWhatsAppText,
  generateSupplierInvoiceHtml,
  buildSupplierInvoiceWhatsAppText,
  type SupplierOrderFormOptions,
} from "../lib/pdfReports"
import {
  ALL_TIME,
  DateRange,
  displayCode,
  encodeMembers,
  hasMember,
  isDelivered,
  isTripClosed,
  matchesDateRange,
  membersDisplay,
  newId,
  salesmenForReport,
  todayYmd,
  toNumericId,
  tripMembers,
  WEB_PAYMENT_STATUSES,
  webPaymentStatus,
} from "../lib/domain"
import { TRIP_TYPE_MARKET, TRIP_TYPE_PHONE, isPhoneTrip } from "../lib/tripTypes"

const selectClass =
  "mt-1.5 w-full rounded-lg border border-zinc-200 bg-white p-2 text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200"

const isCancelled = (v: Visit) => (v.status || "").toLowerCase() === "cancelled"
const isOpenTrip = (v: Visit) => !isTripClosed(v) && !isCancelled(v)
const billOf = (e: PurchaseEntry) =>
  Number(e.grandTotalWithGst) || Number(e.totalAmount || 0) + Number(e.gstAmount || 0) || 0
const isActivePerson = (e: Employee) => !e.isDeleted && !e.isBlocked && (e.status || "Active").toLowerCase() === "active"

export function VisitsView() {
  const {
    visits,
    entries,
    customers,
    suppliers,
    employees,
    markets,
    transporters,
    packGroups,
    joinableVisits,
    selectedEmployeeId,
    setSelectedEmployeeId,
    saveVisit,
    savePurchaseEntry,
    saveCustomer,
    saveSupplier,
    joinVisit,
    addVisitMember,
    closeVisit,
    reopenVisit,
    nextOrderNo,
  } = useData()
  const { user, employee: currentEmployee, isAdmin, role } = useAuth()
  const readOnly = role === "agent"
  const actorName = currentEmployee?.name || user?.displayName || "Admin"

  // ---------------------------------------------------------------------------
  // List state
  // ---------------------------------------------------------------------------
  const [search, setSearch] = useState<string>("")
  const [statusFilter, setStatusFilter] = useState<string>("all")
  const [dateRange, setDateRange] = useState<DateRange>(ALL_TIME)
  const [selectedVisitId, setSelectedVisitId] = useState<number | null>(null)
  /** Trip we just wrote, so the order form works before the live listener echoes it back */
  const [justCreatedVisit, setJustCreatedVisit] = useState<Visit | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  // New trip
  const [isAddOpen, setIsAddOpen] = useState<boolean>(false)
  // "market" = the customer comes along; "phone" = they ordered by phone and named the suppliers
  const [tripMode, setTripMode] = useState<"market" | "phone">("market")
  const [tripSupplierIds, setTripSupplierIds] = useState<number[]>([])
  const [supplierFilterNewTrip, setSupplierFilterNewTrip] = useState<string>("")
  /** Phone order in progress: the order dialog opens once per supplier */
  const [queue, setQueue] = useState<{ tripId: number; supplierIds: number[]; index: number; saved: number } | null>(null)
  const [closePrompt, setClosePrompt] = useState<{ tripId: number; saved: number } | null>(null)
  const [customerFilter, setCustomerFilter] = useState<string>("")
  const [customerId, setCustomerId] = useState<number>(0)
  const [agentId, setAgentId] = useState<number>(0)
  const [extraMemberIds, setExtraMemberIds] = useState<number[]>([])
  const [visitDate, setVisitDate] = useState<string>(todayYmd())
  const [visitNotes, setVisitNotes] = useState<string>("")
  const [tripError, setTripError] = useState<string>("")

  // Trip detail dialogs
  const [isAddMemberOpen, setIsAddMemberOpen] = useState<boolean>(false)
  const [isCloseOpen, setIsCloseOpen] = useState<boolean>(false)
  // "New Order" by a staff member who is not on the trip: join first
  const [isJoinPromptOpen, setIsJoinPromptOpen] = useState<boolean>(false)
  const [tripSupplierFilter, setTripSupplierFilter] = useState<number | "all">("all")

  // Add order
  const [isAddOrderOpen, setIsAddOrderOpen] = useState<boolean>(false)
  const [supplierFilter, setSupplierFilter] = useState<string>("")
  const [orderSupplierId, setOrderSupplierId] = useState<number>(0)
  const [orderSalesmanId, setOrderSalesmanId] = useState<number>(0)
  const [orderItemCode, setOrderItemCode] = useState<string>("")
  const [orderPieces, setOrderPieces] = useState<string>("")
  const [orderCases, setOrderCases] = useState<string>("")
  const [orderLoose, setOrderLoose] = useState<string>("")
  const [orderRate, setOrderRate] = useState<string>("")
  const [orderCaseSize, setOrderCaseSize] = useState<string>("24")
  const [orderTransporter, setOrderTransporter] = useState<string>("")
  const [orderRemarks, setOrderRemarks] = useState<string>("")
  const [orderDeliveryStatus, setOrderDeliveryStatus] = useState<string>("Pending")
  const [orderPaymentStatus, setOrderPaymentStatus] = useState<string>("Unpaid")
  const [orderDate, setOrderDate] = useState<string>("")
  const [orderBillUri, setOrderBillUri] = useState<string>("")
  const [orderError, setOrderError] = useState<string>("")
  const [savingOrder, setSavingOrder] = useState<boolean>(false)

  // Edit order
  const [editingOrder, setEditingOrder] = useState<PurchaseEntry | null>(null)
  const [editItemCode, setEditItemCode] = useState<string>("")
  const [editPieces, setEditPieces] = useState<string>("")
  const [editRate, setEditRate] = useState<string>("")
  const [editCases, setEditCases] = useState<string>("")
  const [editLoose, setEditLoose] = useState<string>("")
  const [editCaseSize, setEditCaseSize] = useState<string>("24")
  const [editRemarks, setEditRemarks] = useState<string>("")
  const [editTransporter, setEditTransporter] = useState<string>("")
  const [editLrNo, setEditLrNo] = useState<string>("")
  const [editDeliveryStatus, setEditDeliveryStatus] = useState<string>("Pending")
  const [editPaymentStatus, setEditPaymentStatus] = useState<string>("Unpaid")
  const [editPaymentMode, setEditPaymentMode] = useState<string>("Cash")
  const [editPaidAmount, setEditPaidAmount] = useState<string>("0")
  const [editSalesmanId, setEditSalesmanId] = useState<number>(0)
  const [editOrderDate, setEditOrderDate] = useState<string>("")
  const [editBillUri, setEditBillUri] = useState<string>("")

  // Quick add customer
  const [isQuickAddCustomerOpen, setIsQuickAddCustomerOpen] = useState<boolean>(false)
  const [quickCustFirm, setQuickCustFirm] = useState<string>("")
  const [quickCustOwner, setQuickCustOwner] = useState<string>("")
  const [quickCustPhone, setQuickCustPhone] = useState<string>("")
  const [quickCustCity, setQuickCustCity] = useState<string>("Ahmedabad")
  const [quickCustType, setQuickCustType] = useState<string>("Credit")
  const [quickCustCreditDays, setQuickCustCreditDays] = useState<string>("30")
  const [quickCustError, setQuickCustError] = useState<string>("")

  // Quick add supplier
  const [isQuickAddSupplierOpen, setIsQuickAddSupplierOpen] = useState<boolean>(false)
  const [quickSupName, setQuickSupName] = useState<string>("")
  const [quickSupContact, setQuickSupContact] = useState<string>("")
  const [quickSupPhone, setQuickSupPhone] = useState<string>("")
  const [quickSupCity, setQuickSupCity] = useState<string>("Ahmedabad")
  const [quickSupMarket, setQuickSupMarket] = useState<string>("")
  const [quickSupType, setQuickSupType] = useState<string>("Manufacturer")
  const [quickSupError, setQuickSupError] = useState<string>("")

  // Report viewer
  const [reportModal, setReportModal] = useState<{
    open: boolean
    title: string
    html: string
    whatsAppText: string
    /** Order forms: rebuilds the document for the chosen options */
    buildHtml?: (options: SupplierOrderFormOptions) => string
  }>({
    open: false,
    title: "",
    html: "",
    whatsAppText: "",
  })

  // ---------------------------------------------------------------------------
  // Derived data
  // ---------------------------------------------------------------------------
  const activeStaff = useMemo(
    () => employees.filter(isActivePerson).sort((a, b) => (a.name || "").localeCompare(b.name || "")),
    [employees]
  )

  const sortedCustomers = useMemo(
    () => [...customers].sort((a, b) => (a.firmName || a.name || "").localeCompare(b.firmName || b.name || "")),
    [customers]
  )
  const sortedSuppliers = useMemo(
    () => [...suppliers].sort((a, b) => (a.firmName || a.name || "").localeCompare(b.firmName || b.name || "")),
    [suppliers]
  )

  // Live trip (members / status change while the page is open, e.g. somebody joins from a phone)
  const selectedVisit = useMemo(() => {
    if (!selectedVisitId) return null
    return (
      visits.find((v) => Number(v.id) === selectedVisitId) ||
      joinableVisits.find((v) => Number(v.id) === selectedVisitId) ||
      // A trip created a moment ago, before the live listener has echoed it back
      (justCreatedVisit && Number(justCreatedVisit.id) === selectedVisitId ? justCreatedVisit : null)
    )
  }, [selectedVisitId, visits, joinableVisits, justCreatedVisit])

  const entriesByVisit = useMemo(() => {
    const map = new Map<number, PurchaseEntry[]>()
    entries.forEach((e) => {
      const key = Number(e.visitId)
      if (!map.has(key)) map.set(key, [])
      map.get(key)!.push(e)
    })
    return map
  }, [entries])

  const activeEmployee = useMemo(() => {
    if (selectedEmployeeId === "all") return null
    return employees.find((e) => e.id === selectedEmployeeId) || null
  }, [selectedEmployeeId, employees])

  const q = search.trim().toLowerCase()
  const baseVisits = visits.filter((v) => {
    if (activeEmployee && !hasMember(v, activeEmployee)) return false
    if (!matchesDateRange(v.date || v.createdAt, dateRange)) return false
    return (
      !q ||
      (v.customerName || "").toLowerCase().includes(q) ||
      (v.visitCode || "").toLowerCase().includes(q) ||
      membersDisplay(v, employees).toLowerCase().includes(q)
    )
  })
  const filteredVisits = baseVisits
    .filter((v) => {
      if (statusFilter === "active") return isOpenTrip(v)
      if (statusFilter === "closed") return !isOpenTrip(v)
      return true
    })
    .sort((a, b) => (b.date || "").localeCompare(a.date || "") || Number(b.id) - Number(a.id))

  const flash = (text: string) => {
    setNotice(text)
    setTimeout(() => setNotice(null), 3500)
  }

  // ---------------------------------------------------------------------------
  // New trip
  // ---------------------------------------------------------------------------
  const openNewTrip = () => {
    setTripError("")
    setCustomerFilter("")
    setCustomerId(0)
    const me = currentEmployee && activeStaff.find((e) => Number(e.id) === Number(currentEmployee.id))
    setAgentId(me ? Number(me.id) : Number(activeStaff[0]?.id || 0))
    setExtraMemberIds([])
    setVisitDate(todayYmd())
    setVisitNotes("")
    setTripMode("market")
    setTripSupplierIds([])
    setSupplierFilterNewTrip("")
    setIsAddOpen(true)
  }

  const handleCreateVisit = async () => {
    const customer = customers.find((c) => Number(c.id) === customerId)
    const starter = activeStaff.find((e) => Number(e.id) === agentId)
    if (!customer) return setTripError("Pick the customer (or create a new one).")
    if (!starter) return setTripError("Pick the salesman who starts the trip.")
    const isPhoneOrder = tripMode === "phone"
    if (isPhoneOrder && tripSupplierIds.length === 0) return setTripError("Add at least one supplier for a phone order.")
    const extras = extraMemberIds
      .filter((id) => id !== agentId)
      .map((id) => activeStaff.find((e) => Number(e.id) === id))
      .filter(Boolean) as Employee[]
    const members = [starter, ...extras].map((e) => ({ id: Number(e.id), name: e.name }))
    const encoded = encodeMembers(members)

    const newVisit: Visit = {
      id: newId(),
      visitCode: displayCode("VIS"),
      customerId: Number(customer.id),
      // Trips carry the brand (shop / firm) name, same as the Android app
      customerName: (customer.firmName || "").trim() || customer.name || "",
      date: visitDate || todayYmd(),
      employeeId: Number(starter.id),
      employeeName: starter.name,
      // First extra salesman also goes into the old co-agent fields so older app versions still show the trip
      secondaryEmployeeId: extras[0] ? Number(extras[0].id) : undefined,
      secondaryEmployeeName: extras[0]?.name,
      memberIds: encoded.memberIds,
      memberNames: encoded.memberNames,
      tripType: isPhoneOrder ? TRIP_TYPE_PHONE : TRIP_TYPE_MARKET,
      status: "Active",
      notes: visitNotes.trim() || `Started from web by ${actorName}`,
      createdAt: Date.now(),
    }
    setBusy("create")
    try {
      await saveVisit(newVisit)
      setIsAddOpen(false)
      setJustCreatedVisit(newVisit)
      setSelectedVisitId(Number(newVisit.id))
      if (isPhoneOrder) {
        // Work through the suppliers the customer named, one order form at a time
        setQueue({ tripId: Number(newVisit.id), supplierIds: tripSupplierIds, index: 0, saved: 0 })
        openAddOrderForQueue(newVisit, tripSupplierIds[0], Number(starter.id))
      }
    } catch (e: any) {
      setTripError(e?.message || "Could not create the trip. Please try again.")
    } finally {
      setBusy(null)
    }
  }

  // ---------------------------------------------------------------------------
  // Phone order queue
  // ---------------------------------------------------------------------------
  /** Opens a blank order form for one supplier of a phone order. */
  const openAddOrderForQueue = (visit: Visit, supplierId: number, salesmanId: number) => {
    const sup = suppliers.find((s) => Number(s.id) === Number(supplierId))
    setOrderSalesmanId(salesmanId)
    setSupplierFilter("")
    setOrderSupplierId(sup ? Number(sup.id) : 0)
    setOrderCaseSize(String(sup?.defaultCaseSize || 24))
    setOrderItemCode("")
    setOrderPieces("")
    setOrderCases("")
    setOrderLoose("")
    setOrderRate("")
    setOrderTransporter("")
    setOrderRemarks("")
    setOrderDeliveryStatus("Pending")
    setOrderPaymentStatus("Unpaid")
    setOrderDate(visit.date || todayYmd())
    setOrderBillUri("")
    setOrderError("")
    setIsAddOrderOpen(true)
  }

  /** After Save & next / Skip: next supplier, or finish and offer to close the trip. */
  const advanceQueue = (saved: boolean) => {
    const q = queue
    if (!q) return
    const nextIndex = q.index + 1
    const savedCount = saved ? q.saved + 1 : q.saved
    if (nextIndex >= q.supplierIds.length) {
      setQueue(null)
      setIsAddOrderOpen(false)
      setClosePrompt({ tripId: q.tripId, saved: savedCount })
      return
    }
    setQueue({ ...q, index: nextIndex, saved: savedCount })
    const visit =
      visits.find((v) => Number(v.id) === q.tripId) ||
      (justCreatedVisit && Number(justCreatedVisit.id) === q.tripId ? justCreatedVisit : null)
    if (visit) openAddOrderForQueue(visit, q.supplierIds[nextIndex], orderSalesmanId)
  }

  // ---------------------------------------------------------------------------
  // Trip actions
  // ---------------------------------------------------------------------------
  const handleJoin = async (visit: Visit): Promise<boolean> => {
    setBusy(`join-${visit.id}`)
    try {
      const ok = await joinVisit(Number(visit.id))
      if (ok) {
        flash(`You joined trip ${visit.visitCode}. Your orders will carry your name.`)
        setSelectedVisitId(Number(visit.id))
      } else {
        flash("Could not join the trip. Check your internet and try again.")
      }
      return ok
    } finally {
      setBusy(null)
    }
  }

  const handleJoinAndAddOrder = async () => {
    if (!selectedVisit) return
    setIsJoinPromptOpen(false)
    if (!isOpenTrip(selectedVisit)) {
      flash("This trip is closed. Reopen it to add orders.")
      return
    }
    const ok = await handleJoin(selectedVisit)
    if (ok) openAddOrder({ asMe: true })
  }

  const handleAddMember = async (emp: Employee) => {
    if (!selectedVisit) return
    setBusy(`member-${emp.id}`)
    try {
      const ok = await addVisitMember(Number(selectedVisit.id), { id: Number(emp.id), name: emp.name })
      flash(ok ? `${emp.name} added to the trip.` : "Could not add the salesman. Please try again.")
    } finally {
      setBusy(null)
    }
  }

  const handleCloseTrip = async () => {
    if (!selectedVisit) return
    setBusy("close")
    try {
      await closeVisit(Number(selectedVisit.id))
      setIsCloseOpen(false)
      flash(`Trip ${selectedVisit.visitCode} closed.`)
    } finally {
      setBusy(null)
    }
  }

  const handleReopenTrip = async () => {
    if (!selectedVisit) return
    setBusy("reopen")
    try {
      await reopenVisit(Number(selectedVisit.id))
      flash(`Trip ${selectedVisit.visitCode} reopened.`)
    } finally {
      setBusy(null)
    }
  }

  // ---------------------------------------------------------------------------
  // Orders
  // ---------------------------------------------------------------------------
  const resolvePerson = (id: number, visit?: Visit | null): { id: number; name: string } => {
    const emp = employees.find((e) => Number(e.id) === id)
    if (emp) return { id: Number(emp.id), name: emp.name }
    const member = visit ? tripMembers(visit, employees).find((m) => m.id === id) : undefined
    if (member) return member
    return { id: Number(visit?.employeeId || 0), name: visit?.employeeName || "" }
  }

  const openAddOrder = (opts: { asMe?: boolean } = {}) => {
    if (!selectedVisit) return
    if (!isOpenTrip(selectedVisit)) {
      flash("This trip is closed. Reopen it to add orders.")
      return
    }
    // The salesman on the trip who types the order is its salesman by default; they can pick somebody else
    const me =
      currentEmployee && (opts.asMe || hasMember(selectedVisit, currentEmployee)) ? Number(currentEmployee.id) : 0
    setOrderSalesmanId(me || Number(selectedVisit.employeeId) || 0)
    setSupplierFilter("")
    setOrderSupplierId(0)
    setOrderCaseSize("24")
    setOrderItemCode("")
    setOrderPieces("")
    setOrderCases("")
    setOrderLoose("")
    setOrderRate("")
    setOrderTransporter("")
    setOrderRemarks("")
    setOrderDeliveryStatus("Pending")
    setOrderPaymentStatus("Unpaid")
    setOrderDate(selectedVisit.date || todayYmd())
    setOrderBillUri("")
    setOrderError("")
    setIsAddOrderOpen(true)
  }

  const splitPacking = (pcs: number, caseSize: number, casesRaw: string, looseRaw: string) => {
    const caseCount = casesRaw !== "" ? parseInt(casesRaw, 10) || 0 : caseSize > 0 ? Math.floor(pcs / caseSize) : 0
    const loosePieces = looseRaw !== "" ? parseInt(looseRaw, 10) || 0 : caseSize > 0 ? pcs % caseSize : 0
    return { caseCount, loosePieces }
  }

  const handleSaveOrder = async () => {
    if (!selectedVisit) return
    const sup = suppliers.find((s) => Number(s.id) === orderSupplierId)
    const pcs = parseInt(orderPieces, 10) || 0
    if (!sup) return setOrderError("Pick the supplier.")
    if (!orderItemCode.trim()) return setOrderError("Enter the item / style code.")
    if (pcs <= 0) return setOrderError("Enter the quantity (pieces).")
    const rt = parseFloat(orderRate) || 0
    const cs = parseInt(orderCaseSize, 10) || 0
    const { caseCount, loosePieces } = splitPacking(pcs, cs, orderCases, orderLoose)
    const gstRate = Number(sup.defaultGstRate ?? 5) || 0
    const baseTotal = pcs * rt
    const gstAmt = (baseTotal * gstRate) / 100
    const salesman = resolvePerson(orderSalesmanId, selectedVisit)

    setSavingOrder(true)
    setOrderError("")
    try {
      // Shared HT-number counter with the Android app
      const orderNo = await nextOrderNo()
      const newOrder: PurchaseEntry = {
        id: newId(),
        visitId: Number(selectedVisit.id),
        orderNo,
        supplierId: Number(sup.id),
        supplierName: sup.name,
        supplierType: sup.type,
        salesmanId: salesman.id || undefined,
        salesmanName: salesman.name || undefined,
        createdById: currentEmployee ? Number(currentEmployee.id) : undefined,
        createdByName: actorName,
        orderDate: orderDate || selectedVisit.date || todayYmd(),
        itemCode: orderItemCode.trim().toUpperCase(),
        pieces: pcs,
        rate: rt,
        caseSize: cs,
        caseCount,
        loosePieces,
        pricePerPiece: rt,
        totalAmount: baseTotal,
        gstPercent: gstRate,
        gstRate,
        gstAmount: gstAmt,
        grandTotalWithGst: baseTotal + gstAmt,
        deliveryStatus: orderDeliveryStatus,
        transporter: orderTransporter.trim() || undefined,
        mixedPackNote: orderRemarks.trim() || undefined,
        // Payment is optional; "Pending" until somebody records it
        paymentStatus: orderPaymentStatus || "Unpaid",
        paymentMode: "Cash",
        paidAmount: 0,
        supplierInvoiceUri: orderBillUri || undefined,
        createdAt: Date.now(),
      }
      await savePurchaseEntry(newOrder)
      // The credited salesman is always on the trip (their trips list, customer report)
      if (salesman.id > 0 && !tripMembers(selectedVisit, employees).some((m) => m.id === salesman.id)) {
        await addVisitMember(Number(selectedVisit.id), { id: salesman.id, name: salesman.name })
      }
      if (queue) {
        // Phone order: move on to the next supplier instead of closing the form
        advanceQueue(true)
      } else {
        setIsAddOrderOpen(false)
      }
      flash(`Order ${orderNo} saved for ${sup.name}.`)
    } catch (e: any) {
      setOrderError(e?.message || "Could not save the order. Please try again.")
    } finally {
      setSavingOrder(false)
    }
  }

  const openEditOrder = (entry: PurchaseEntry) => {
    setEditingOrder(entry)
    setEditItemCode(entry.itemCode || "")
    setEditPieces(String(entry.pieces || ""))
    setEditRate(String(entry.rate || entry.pricePerPiece || ""))
    setEditCases(String(entry.caseCount ?? ""))
    setEditLoose(String(entry.loosePieces ?? ""))
    setEditCaseSize(String(entry.caseSize || "24"))
    setEditRemarks(entry.mixedPackNote || "")
    setEditTransporter(entry.transporter || "")
    setEditLrNo(entry.lrNo || "")
    setEditDeliveryStatus(entry.deliveryStatus || "Pending")
    setEditPaymentStatus(webPaymentStatus(entry.paymentStatus))
    setEditPaymentMode(entry.paymentMode || "Cash")
    setEditPaidAmount(String(entry.paidAmount || "0"))
    setEditSalesmanId(toNumericId(entry.salesmanId) || Number(selectedVisit?.employeeId || 0))
    setEditOrderDate(entry.orderDate || selectedVisit?.date || "")
    setEditBillUri(entry.supplierInvoiceUri || "")
  }

  const handleSaveEditOrder = async () => {
    if (!editingOrder) return
    const pcs = parseInt(editPieces, 10) || 0
    const rt = parseFloat(editRate) || 0
    const cs = parseInt(editCaseSize, 10) || 0
    const { caseCount, loosePieces } = splitPacking(pcs, cs, editCases, editLoose)
    const gstRate = Number(editingOrder.gstPercent ?? editingOrder.gstRate ?? 5) || 0
    const baseTotal = pcs * rt
    const gstAmt = (baseTotal * gstRate) / 100
    const salesman = editSalesmanId ? resolvePerson(editSalesmanId, selectedVisit) : null

    const updated: PurchaseEntry = {
      ...editingOrder,
      salesmanId: salesman?.id || editingOrder.salesmanId,
      salesmanName: salesman?.name || editingOrder.salesmanName,
      orderDate: editOrderDate || editingOrder.orderDate,
      itemCode: editItemCode.trim().toUpperCase(),
      pieces: pcs,
      rate: rt,
      pricePerPiece: rt,
      caseSize: cs,
      caseCount,
      loosePieces,
      totalAmount: baseTotal,
      gstAmount: gstAmt,
      grandTotalWithGst: baseTotal + gstAmt,
      transporter: editTransporter.trim(),
      lrNo: editLrNo.trim(),
      deliveryStatus: editDeliveryStatus,
      paymentStatus: editPaymentStatus,
      paymentMode: editPaymentMode,
      paidAmount: parseFloat(editPaidAmount) || 0,
      mixedPackNote: editRemarks.trim() || null,
      packGroupId: loosePieces === 0 ? null : editingOrder.packGroupId ?? null,
      supplierInvoiceUri: editBillUri || null,
    }
    await savePurchaseEntry(updated)
    setEditingOrder(null)
  }

  // ---------------------------------------------------------------------------
  // Reports
  // ---------------------------------------------------------------------------
  const findCustomer = (visit: Visit) =>
    customers.find((c) => Number(c.id) === Number(visit.customerId)) ||
    customers.find((c) => c.name === visit.customerName || c.firmName === visit.customerName)

  const openCustomerReport = (visit: Visit) => {
    const visitItems = entriesByVisit.get(Number(visit.id)) || []
    const names = salesmenForReport(visit, visitItems)
    const salesman =
      employees.find((e) => (e.name || "").trim().toLowerCase() === (names[0] || "").toLowerCase()) ||
      employees.find((e) => Number(e.id) === Number(visit.employeeId))
    const reportData = {
      visit,
      customer: findCustomer(visit),
      salesman,
      entries: visitItems,
      packGroups: packGroups.filter((pg) => Number(pg.visitId) === Number(visit.id)),
    }
    setReportModal({
      open: true,
      title: `Customer Report: ${visit.customerName} (${visit.visitCode})`,
      html: generateCustomerDayReportHtml(reportData),
      whatsAppText: buildCustomerReportWhatsAppText(reportData),
    })
  }

  const supplierFor = (entry: PurchaseEntry): Supplier =>
    suppliers.find((s) => Number(s.id) === Number(entry.supplierId)) || {
      id: entry.supplierId || 0,
      name: entry.supplierName,
      type: entry.supplierType || "Wholesaler",
      phone: "—",
      marketArea: "—",
    }

  const openSupplierInvoice = (entry: PurchaseEntry, visit: Visit) => {
    const salesman =
      employees.find((e) => Number(e.id) === toNumericId(entry.salesmanId)) ||
      employees.find((e) => Number(e.id) === Number(visit.employeeId))
    const invoiceData = { supplier: supplierFor(entry), visit, customer: findCustomer(visit), salesman, entries: [entry] }
    setReportModal({
      open: true,
      title: `Order Form: #${entry.orderNo} • ${invoiceData.supplier.brand || invoiceData.supplier.name}`,
      html: generateSupplierInvoiceHtml(invoiceData),
      whatsAppText: buildSupplierInvoiceWhatsAppText(invoiceData),
      buildHtml: (options) => generateSupplierInvoiceHtml({ ...invoiceData, options }),
    })
  }

  const printAllPOs = (visit: Visit, visitEntries: PurchaseEntry[]) => {
    const grouped = new Map<number, PurchaseEntry[]>()
    visitEntries.forEach((e) => {
      const sid = Number(e.supplierId || 0)
      if (!grouped.has(sid)) grouped.set(sid, [])
      grouped.get(sid)!.push(e)
    })
    let idx = 0
    grouped.forEach((grpEntries) => {
      const salesman = employees.find((e) => Number(e.id) === toNumericId(grpEntries[0]?.salesmanId))
      const html = generateSupplierInvoiceHtml({
        supplier: supplierFor(grpEntries[0]),
        visit,
        customer: findCustomer(visit),
        salesman,
        entries: grpEntries,
      })
      const delay = idx++ * 800
      setTimeout(() => {
        const win = window.open("", `po_${delay}`, "width=800,height=600")
        if (win) {
          win.document.write(html)
          win.document.close()
          setTimeout(() => win.print(), 500)
        }
      }, delay)
    })
  }

  // ---------------------------------------------------------------------------
  // Quick add customer / supplier
  // ---------------------------------------------------------------------------
  const handleSaveQuickCustomer = async () => {
    const cleanFirm = quickCustFirm.trim()
    const cleanOwner = quickCustOwner.trim()
    const cleanPhone = quickCustPhone.trim()
    if (!cleanFirm && !cleanOwner) return setQuickCustError("Please enter Shop/Firm or Owner Name")
    if (cleanPhone.replace(/\D/g, "").length < 10) return setQuickCustError("Please enter a 10 digit mobile number")

    const id = newId()
    const newCust: Customer = {
      id,
      customerId: displayCode("CUST"),
      name: cleanOwner || cleanFirm,
      firmName: cleanFirm || cleanOwner,
      phone: cleanPhone,
      city: quickCustCity.trim() || "Ahmedabad",
      customerType: quickCustType,
      creditDays: Number(quickCustCreditDays) || 30,
      creditLimit: 0,
      addedByAgentId: currentEmployee ? Number(currentEmployee.id) : undefined,
      addedByAgentName: actorName,
      notes: "Quick created while starting a trip",
      createdAt: Date.now(),
    }
    await saveCustomer(newCust)
    setCustomerId(id)
    setIsQuickAddCustomerOpen(false)
    setQuickCustFirm("")
    setQuickCustOwner("")
    setQuickCustPhone("")
    setQuickCustError("")
  }

  const handleSaveQuickSupplier = async () => {
    const cleanName = quickSupName.trim()
    if (cleanName.length < 2) return setQuickSupError("Supplier name must be at least 2 characters")
    const marketName = quickSupMarket.trim()
    const market = marketName
      ? markets.find((m) => (m.marketName || "").trim().toLowerCase() === marketName.toLowerCase())
      : undefined
    const id = newId()
    const newSup: Supplier = {
      id,
      supplierId: displayCode("SUP"),
      name: cleanName,
      firmName: cleanName,
      type: quickSupType,
      contactPerson: quickSupContact.trim(),
      phone: quickSupPhone.trim(),
      city: quickSupCity.trim() || "Ahmedabad",
      marketId: market ? Number(market.id) : undefined,
      marketArea: market?.marketName || marketName,
      marketName: market?.marketName || marketName,
      defaultCaseSize: 24,
      notes: "Quick created while adding an order",
      createdAt: Date.now(),
    }
    await saveSupplier(newSup)
    setOrderSupplierId(id)
    setOrderCaseSize("24")
    setIsQuickAddSupplierOpen(false)
    setQuickSupName("")
    setQuickSupContact("")
    setQuickSupPhone("")
    setQuickSupMarket("")
    setQuickSupError("")
  }

  // ---------------------------------------------------------------------------
  // Shared dialogs (rendered on both the list and the trip page)
  // ---------------------------------------------------------------------------
  const customerOptions = sortedCustomers.filter((c) => {
    const f = customerFilter.trim().toLowerCase()
    if (!f || Number(c.id) === customerId) return true
    return [c.firmName, c.name, c.city, c.phone, c.customerId].some((v) => (v || "").toLowerCase().includes(f))
  })
  const supplierOptions = sortedSuppliers.filter((s) => {
    const f = supplierFilter.trim().toLowerCase()
    if (!f || Number(s.id) === orderSupplierId) return true
    return [s.firmName, s.name, s.brand, s.marketArea, s.city, s.phone].some((v) => (v || "").toLowerCase().includes(f))
  })
  // Suppliers to pick from for a phone order (picked ones always stay visible)
  const phoneSupplierOptions = sortedSuppliers.filter((s) => {
    const f = supplierFilterNewTrip.trim().toLowerCase()
    if (!f || tripSupplierIds.includes(Number(s.id))) return true
    return [s.firmName, s.name, s.brand, s.marketArea, s.city, s.phone].some((v) => (v || "").toLowerCase().includes(f))
  })
  const queueSupplierName = (id: number) => {
    const s = suppliers.find((x) => Number(x.id) === Number(id))
    return s?.firmName || s?.name || `Supplier ${id}`
  }

  const orderMembers = selectedVisit ? tripMembers(selectedVisit, employees) : []
  const otherStaff = activeStaff.filter((e) => !orderMembers.some((m) => m.id === Number(e.id)))

  const salesmanSelect = (value: number, onChange: (id: number) => void, id: string) => (
    <select id={id} value={value || ""} onChange={(e) => onChange(Number(e.target.value) || 0)} className={selectClass}>
      <optgroup label="On this trip">
        {orderMembers.map((m) => (
          <option key={`m-${m.id}`} value={m.id}>
            👤 {m.name}
          </option>
        ))}
      </optgroup>
      {otherStaff.length > 0 && (
        <optgroup label="Other staff">
          {otherStaff.map((e) => (
            <option key={`s-${e.id}`} value={e.id}>
              {e.name}
            </option>
          ))}
        </optgroup>
      )}
    </select>
  )

  const addOrderPreviewPcs = parseInt(orderPieces, 10) || 0
  const addOrderPreview = splitPacking(addOrderPreviewPcs, parseInt(orderCaseSize, 10) || 0, orderCases, orderLoose)

  const dialogs = (
    <>
      {/* New Trip */}
      <Dialog
        open={isAddOpen}
        onOpenChange={setIsAddOpen}
        title="Start a New Trip"
        description="Market visit, or a phone order where the customer named the suppliers."
        className="max-h-[92vh] overflow-y-auto"
      >
        <div className="space-y-4 pt-1">
          <div role="radiogroup" aria-label="Trip type" className="grid grid-cols-2 gap-1 rounded-xl bg-zinc-100 p-1 dark:bg-zinc-900">
            {([
              { key: "market" as const, label: "Market visit", hint: "Customer is in the market" },
              { key: "phone" as const, label: "Phone order", hint: "Ordered on call" },
            ]).map((opt) => {
              const on = tripMode === opt.key
              return (
                <button
                  key={opt.key}
                  type="button"
                  role="radio"
                  aria-checked={on}
                  onClick={() => setTripMode(opt.key)}
                  className={`rounded-lg px-3 py-2 text-left ${
                    on ? "bg-white shadow-sm dark:bg-zinc-800" : "hover:bg-white/60 dark:hover:bg-zinc-800/60"
                  }`}
                >
                  <span className="block text-xs font-semibold text-zinc-900 dark:text-zinc-100">{opt.label}</span>
                  <span className="block text-[10px] text-muted-foreground">{opt.hint}</span>
                </button>
              )
            })}
          </div>

          <div>
            <div className="flex items-center justify-between">
              <label htmlFor="trip-customer" className="text-xs font-medium text-muted-foreground">
                Customer *
              </label>
              <button
                type="button"
                onClick={() => setIsQuickAddCustomerOpen(true)}
                className="text-[11px] font-bold text-blue-600 hover:text-blue-700 dark:text-blue-400"
              >
                + New customer
              </button>
            </div>
            <Input
              value={customerFilter}
              onChange={(e) => setCustomerFilter(e.target.value)}
              placeholder="Type to find: shop, owner, city, phone..."
              className="mt-1.5 h-8 text-xs"
              aria-label="Find customer"
            />
            <select
              id="trip-customer"
              value={customerId || ""}
              onChange={(e) => setCustomerId(Number(e.target.value) || 0)}
              className={selectClass}
            >
              <option value="">Choose customer ({customerOptions.length})</option>
              {customerOptions.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.firmName || c.name}
                  {c.firmName && c.name && c.firmName !== c.name ? ` - ${c.name}` : ""} ({c.city || c.marketArea || "Local"})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="trip-starter" className="text-xs font-medium text-muted-foreground">
              Salesman starting the trip *
            </label>
            <select id="trip-starter" value={agentId || ""} onChange={(e) => setAgentId(Number(e.target.value) || 0)} className={selectClass}>
              <option value="">Choose salesman</option>
              {activeStaff.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  👤 {emp.name}
                </option>
              ))}
            </select>
          </div>

          <fieldset>
            <legend className="text-xs font-medium text-muted-foreground">More salesmen on this trip (optional)</legend>
            <p className="text-[10px] text-muted-foreground">They can also join later from their own phone.</p>
            <div className="mt-1.5 flex flex-wrap gap-1.5">
              {activeStaff
                .filter((e) => Number(e.id) !== agentId)
                .map((e) => {
                  const on = extraMemberIds.includes(Number(e.id))
                  return (
                    <button
                      key={e.id}
                      type="button"
                      aria-pressed={on}
                      onClick={() =>
                        setExtraMemberIds((prev) => (on ? prev.filter((x) => x !== Number(e.id)) : [...prev, Number(e.id)]))
                      }
                      className={`rounded-full border px-2.5 py-1 text-[11px] font-medium ${
                        on
                          ? "border-zinc-900 bg-zinc-900 text-white dark:border-zinc-100 dark:bg-zinc-100 dark:text-zinc-900"
                          : "border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-100 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300"
                      }`}
                    >
                      {on ? "✓ " : "+ "}
                      {e.name}
                    </button>
                  )
                })}
              {activeStaff.length <= 1 && <span className="text-[11px] text-muted-foreground">No other active staff.</span>}
            </div>
          </fieldset>

          {tripMode === "phone" && (
            <fieldset className="rounded-xl border border-zinc-200 p-3 dark:border-zinc-800">
              <legend className="px-1 text-xs font-semibold text-zinc-900 dark:text-zinc-100">
                Suppliers the customer ordered from *
              </legend>
              <p className="text-[10px] text-muted-foreground">
                One order form opens for each supplier, in the order you pick them.
              </p>
              <Input
                value={supplierFilterNewTrip}
                onChange={(e) => setSupplierFilterNewTrip(e.target.value)}
                placeholder="Find supplier: name, brand, market..."
                className="mt-2 h-8 text-xs"
                aria-label="Find supplier"
              />
              <div className="mt-2 max-h-48 space-y-1 overflow-y-auto pr-1">
                {phoneSupplierOptions.slice(0, 80).map((s) => {
                  const sid = Number(s.id)
                  const pickedAt = tripSupplierIds.indexOf(sid)
                  const on = pickedAt >= 0
                  return (
                    <button
                      key={s.id}
                      type="button"
                      aria-pressed={on}
                      onClick={() =>
                        setTripSupplierIds((prev) => (on ? prev.filter((x) => x !== sid) : [...prev, sid]))
                      }
                      className={`flex w-full items-center gap-2 rounded-lg border px-2.5 py-2 text-left ${
                        on
                          ? "border-blue-500 bg-blue-50 dark:border-blue-500 dark:bg-blue-950/40"
                          : "border-zinc-200 bg-white hover:bg-zinc-50 dark:border-zinc-800 dark:bg-zinc-900 dark:hover:bg-zinc-800/60"
                      }`}
                    >
                      <span
                        aria-hidden="true"
                        className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px] font-bold ${
                          on ? "bg-blue-600 text-white" : "bg-zinc-200 text-zinc-500 dark:bg-zinc-800 dark:text-zinc-400"
                        }`}
                      >
                        {on ? pickedAt + 1 : "+"}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-xs font-semibold text-zinc-900 dark:text-zinc-100">
                          {s.firmName || s.name}
                        </span>
                        <span className="block truncate text-[10px] text-muted-foreground">
                          {[s.brand, s.marketArea || s.city].filter(Boolean).join(" • ") || "Supplier"}
                        </span>
                      </span>
                    </button>
                  )
                })}
                {phoneSupplierOptions.length === 0 && (
                  <p className="py-2 text-[11px] text-muted-foreground">No supplier matches that search.</p>
                )}
              </div>
              <p className="mt-2 text-[11px] font-semibold text-zinc-900 dark:text-zinc-100">
                {tripSupplierIds.length === 0
                  ? "No supplier picked yet"
                  : `${tripSupplierIds.length} supplier${tripSupplierIds.length > 1 ? "s" : ""}: ${tripSupplierIds
                      .map(queueSupplierName)
                      .join(", ")}`}
              </p>
            </fieldset>
          )}

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label htmlFor="trip-date" className="text-xs font-medium text-muted-foreground">
                {tripMode === "phone" ? "Order date" : "Trip date"}
              </label>
              <Input id="trip-date" type="date" value={visitDate} onChange={(e) => setVisitDate(e.target.value)} className="mt-1.5" />
            </div>
            <div>
              <label htmlFor="trip-notes" className="text-xs font-medium text-muted-foreground">
                Notes
              </label>
              <Input id="trip-notes" value={visitNotes} onChange={(e) => setVisitNotes(e.target.value)} placeholder="Optional" className="mt-1.5" />
            </div>
          </div>

          {tripError && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700 dark:bg-red-950/40 dark:text-red-300" role="alert">
              {tripError}
            </p>
          )}
          <div className="flex justify-end gap-2">
            <Button variant="outline" size="sm" onClick={() => setIsAddOpen(false)}>
              Cancel
            </Button>
            <Button size="sm" onClick={handleCreateVisit} disabled={busy === "create"}>
              {busy === "create"
                ? "Starting..."
                : tripMode === "phone"
                  ? `Start & add ${tripSupplierIds.length || ""} order${tripSupplierIds.length === 1 ? "" : "s"}`.replace("  ", " ")
                  : "Start Trip"}
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Add Order */}
      <Dialog
        open={isAddOrderOpen}
        onOpenChange={setIsAddOrderOpen}
        title="New Order"
        description={selectedVisit ? `${selectedVisit.customerName} • Trip ${selectedVisit.visitCode}` : undefined}
        className="max-w-2xl max-h-[92vh] overflow-y-auto"
      >
        <div className="space-y-3.5 pt-1 text-xs">
          {queue && (
            <div className="rounded-xl border border-blue-200 bg-blue-50 p-3 dark:border-blue-900 dark:bg-blue-950/40">
              <div className="flex items-center justify-between gap-2">
                <p className="text-xs font-bold text-blue-900 dark:text-blue-200">
                  Phone order · supplier {queue.index + 1} of {queue.supplierIds.length}
                </p>
                <p className="text-[10px] font-semibold text-blue-700 dark:text-blue-300">{queue.saved} saved</p>
              </div>
              <p className="mt-0.5 truncate text-[11px] text-blue-800 dark:text-blue-300">
                {queueSupplierName(queue.supplierIds[queue.index])}
              </p>
              <div className="mt-2 flex gap-1" aria-hidden="true">
                {queue.supplierIds.map((sid, i) => (
                  <span
                    key={sid}
                    className={`h-1.5 flex-1 rounded-full ${
                      i < queue.index ? "bg-blue-600" : i === queue.index ? "bg-blue-400" : "bg-blue-200 dark:bg-blue-900"
                    }`}
                  />
                ))}
              </div>
            </div>
          )}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <div className="flex items-center justify-between">
                <label htmlFor="order-supplier" className="text-xs font-medium text-muted-foreground">
                  Supplier *
                </label>
                <button
                  type="button"
                  onClick={() => setIsQuickAddSupplierOpen(true)}
                  className="text-[11px] font-bold text-blue-600 hover:text-blue-700 dark:text-blue-400"
                >
                  + New supplier
                </button>
              </div>
              <Input
                value={supplierFilter}
                onChange={(e) => setSupplierFilter(e.target.value)}
                placeholder="Type to find supplier / brand / market"
                className="mt-1.5 h-8 text-xs"
                aria-label="Find supplier"
              />
              <select
                id="order-supplier"
                value={orderSupplierId || ""}
                onChange={(e) => {
                  const id = Number(e.target.value) || 0
                  setOrderSupplierId(id)
                  const sup = suppliers.find((s) => Number(s.id) === id)
                  setOrderCaseSize(String(sup?.defaultCaseSize || 24))
                }}
                className={selectClass}
              >
                <option value="">Choose supplier ({supplierOptions.length})</option>
                {supplierOptions.map((s) => (
                  <option key={s.id} value={s.id}>
                    🏭 {s.firmName || s.name}
                    {s.brand ? ` [${s.brand}]` : ""} ({s.marketArea || s.city || "Local"})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="order-salesman" className="text-xs font-medium text-muted-foreground">
                Salesman for this order
              </label>
              {salesmanSelect(orderSalesmanId, setOrderSalesmanId, "order-salesman")}
              <p className="mt-1 text-[10px] text-muted-foreground">Their name goes on the customer report.</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div className="col-span-2">
              <label htmlFor="order-item" className="text-xs font-medium text-muted-foreground">
                Item / style code *
              </label>
              <Input
                id="order-item"
                value={orderItemCode}
                onChange={(e) => setOrderItemCode(e.target.value.toUpperCase())}
                placeholder="e.g. KURTI-102"
                className="mt-1"
              />
            </div>
            <div>
              <label htmlFor="order-pcs" className="text-xs font-medium text-muted-foreground">
                Quantity (pcs) *
              </label>
              <Input id="order-pcs" type="number" min={0} value={orderPieces} onChange={(e) => setOrderPieces(e.target.value)} className="mt-1 font-semibold" />
            </div>
            <div>
              <label htmlFor="order-rate" className="text-xs font-medium text-muted-foreground">
                Rate (₹/pc)
              </label>
              <Input id="order-rate" type="number" min={0} value={orderRate} onChange={(e) => setOrderRate(e.target.value)} className="mt-1" />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label htmlFor="order-case-size" className="text-xs font-medium text-muted-foreground">
                Pcs per case
              </label>
              <Input id="order-case-size" type="number" min={0} value={orderCaseSize} onChange={(e) => setOrderCaseSize(e.target.value)} className="mt-1" />
            </div>
            <div>
              <label htmlFor="order-cases" className="text-xs font-medium text-muted-foreground">
                Cases / packs
              </label>
              <Input
                id="order-cases"
                type="number"
                min={0}
                value={orderCases}
                onChange={(e) => setOrderCases(e.target.value)}
                placeholder={String(addOrderPreview.caseCount)}
                className="mt-1"
              />
            </div>
            <div>
              <label htmlFor="order-loose" className="text-xs font-medium text-muted-foreground">
                Loose pcs
              </label>
              <Input
                id="order-loose"
                type="number"
                min={0}
                value={orderLoose}
                onChange={(e) => setOrderLoose(e.target.value)}
                placeholder={String(addOrderPreview.loosePieces)}
                className="mt-1"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label htmlFor="order-transporter" className="text-xs font-medium text-muted-foreground">
                Transport
              </label>
              <Input
                id="order-transporter"
                list="trip-transporters"
                value={orderTransporter}
                onChange={(e) => setOrderTransporter(e.target.value)}
                placeholder="Pick or type"
                className="mt-1"
              />
            </div>
            <div>
              <label htmlFor="order-date" className="text-xs font-medium text-muted-foreground">
                Order date
              </label>
              <Input id="order-date" type="date" value={orderDate} onChange={(e) => setOrderDate(e.target.value)} className="mt-1" />
            </div>
          </div>

          <div>
            <label htmlFor="order-remark" className="text-xs font-medium text-muted-foreground">
              Delivery / packing remark
            </label>
            <Input
              id="order-remark"
              value={orderRemarks}
              onChange={(e) => setOrderRemarks(e.target.value)}
              placeholder="e.g. Pack with HT-2710, deliver after Diwali"
              className="mt-1"
            />
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <p className="text-xs font-medium text-muted-foreground">Delivery status</p>
              <div className="mt-1.5 flex flex-wrap gap-1.5" role="group" aria-label="Delivery status">
                {["Pending", "Packed", "Dispatched", "Delivered"].map((s) => (
                  <Button
                    key={s}
                    type="button"
                    size="sm"
                    variant={orderDeliveryStatus === s ? "default" : "outline"}
                    aria-pressed={orderDeliveryStatus === s}
                    onClick={() => setOrderDeliveryStatus(s)}
                    className="h-7 px-2.5 text-[11px]"
                  >
                    {s}
                  </Button>
                ))}
              </div>
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Payment (optional)</p>
              <div className="mt-1.5 flex flex-wrap gap-1.5" role="group" aria-label="Payment status">
                {WEB_PAYMENT_STATUSES.map((s) => (
                  <Button
                    key={s}
                    type="button"
                    size="sm"
                    variant={orderPaymentStatus === s ? "default" : "outline"}
                    aria-pressed={orderPaymentStatus === s}
                    onClick={() => setOrderPaymentStatus(s)}
                    className="h-7 px-2.5 text-[11px]"
                  >
                    {s}
                  </Button>
                ))}
              </div>
            </div>
          </div>

          {selectedVisit && (
            <FileUpload
              label="Bill / order photo (optional)"
              folder={`orders/${selectedVisit.id}`}
              prefix="bill"
              value={orderBillUri}
              onChange={setOrderBillUri}
            />
          )}

          <div className="rounded-xl border border-zinc-200 bg-zinc-50 p-3 dark:border-zinc-800 dark:bg-zinc-900/60">
            <div className="flex items-center justify-between text-xs font-bold text-zinc-900 dark:text-zinc-100">
              <span>Total: ₹{formatInr(addOrderPreviewPcs * (Number(orderRate) || 0))} + GST</span>
              <span>
                {addOrderPreview.caseCount} cases
                {addOrderPreview.loosePieces > 0 ? ` + ${addOrderPreview.loosePieces} loose` : ""}
              </span>
            </div>
          </div>

          {orderError && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700 dark:bg-red-950/40 dark:text-red-300" role="alert">
              {orderError}
            </p>
          )}
          {queue ? (
            <div className="flex flex-wrap justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => advanceQueue(false)} disabled={savingOrder}>
                Skip this supplier
              </Button>
              {queue.index + 1 < queue.supplierIds.length ? (
                <Button size="sm" onClick={handleSaveOrder} disabled={savingOrder}>
                  {savingOrder ? "Saving..." : "Save & next supplier"}
                </Button>
              ) : (
                <Button size="sm" onClick={handleSaveOrder} disabled={savingOrder}>
                  {savingOrder ? "Saving..." : "Save & finish"}
                </Button>
              )}
            </div>
          ) : (
            <div className="flex justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => setIsAddOrderOpen(false)}>
                Cancel
              </Button>
              <Button size="sm" onClick={handleSaveOrder} disabled={savingOrder}>
                {savingOrder ? "Saving..." : "Save Order"}
              </Button>
            </div>
          )}
        </div>
      </Dialog>

      {/* After a phone order: offer to close the trip right away */}
      {closePrompt && (
        <Dialog
          open={!!closePrompt}
          onOpenChange={(open) => !open && setClosePrompt(null)}
          title="Phone order done"
          description={`${closePrompt.saved} order${closePrompt.saved === 1 ? "" : "s"} saved on this trip.`}
        >
          <div className="space-y-3 pt-1">
            <p className="text-xs text-muted-foreground">
              Close the trip now, or keep it open to add more orders. Closing does not need payment to be recorded.
            </p>
            <div className="flex justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => setClosePrompt(null)}>
                Keep it open
              </Button>
              <Button
                size="sm"
                disabled={busy === "close-prompt"}
                onClick={async () => {
                  const tripId = closePrompt.tripId
                  setBusy("close-prompt")
                  try {
                    await closeVisit(tripId)
                    setClosePrompt(null)
                    flash("Trip closed.")
                  } catch (e: any) {
                    setNotice(e?.message || "Could not close the trip.")
                  } finally {
                    setBusy(null)
                  }
                }}
              >
                {busy === "close-prompt" ? "Closing..." : "Close trip"}
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      <datalist id="trip-transporters">
        {transporters.map((t) => (
          <option key={t.id} value={t.transporterName} />
        ))}
      </datalist>
      <datalist id="trip-markets">
        {markets.map((m) => (
          <option key={m.id} value={m.marketName} />
        ))}
      </datalist>

      {/* Edit Order */}
      {editingOrder && (
        <Dialog
          open={!!editingOrder}
          onOpenChange={(open) => !open && setEditingOrder(null)}
          title={`Edit Order #${editingOrder.orderNo}`}
          description={editingOrder.supplierName}
          className="max-w-2xl max-h-[92vh] overflow-y-auto"
        >
          <div className="space-y-3 pt-1 text-xs">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label htmlFor="edit-salesman" className="text-xs font-medium text-muted-foreground">
                  Salesman
                </label>
                {salesmanSelect(editSalesmanId, setEditSalesmanId, "edit-salesman")}
              </div>
              <div>
                <label htmlFor="edit-date" className="text-xs font-medium text-muted-foreground">
                  Order date
                </label>
                <Input id="edit-date" type="date" value={editOrderDate} onChange={(e) => setEditOrderDate(e.target.value)} className="mt-1.5" />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <div className="col-span-2">
                <label htmlFor="edit-item" className="text-xs font-medium text-muted-foreground">
                  Item code
                </label>
                <Input id="edit-item" value={editItemCode} onChange={(e) => setEditItemCode(e.target.value.toUpperCase())} className="mt-1" />
              </div>
              <div>
                <label htmlFor="edit-pcs" className="text-xs font-medium text-muted-foreground">
                  Pieces
                </label>
                <Input id="edit-pcs" type="number" value={editPieces} onChange={(e) => setEditPieces(e.target.value)} className="mt-1" />
              </div>
              <div>
                <label htmlFor="edit-rate" className="text-xs font-medium text-muted-foreground">
                  Rate (₹/pc)
                </label>
                <Input id="edit-rate" type="number" value={editRate} onChange={(e) => setEditRate(e.target.value)} className="mt-1" />
              </div>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div>
                <label htmlFor="edit-case-size" className="text-xs font-medium text-muted-foreground">
                  Pcs per case
                </label>
                <Input id="edit-case-size" type="number" value={editCaseSize} onChange={(e) => setEditCaseSize(e.target.value)} className="mt-1" />
              </div>
              <div>
                <label htmlFor="edit-cases" className="text-xs font-medium text-muted-foreground">
                  Cases
                </label>
                <Input id="edit-cases" type="number" value={editCases} onChange={(e) => setEditCases(e.target.value)} className="mt-1" />
              </div>
              <div>
                <label htmlFor="edit-loose" className="text-xs font-medium text-muted-foreground">
                  Loose pcs
                </label>
                <Input id="edit-loose" type="number" value={editLoose} onChange={(e) => setEditLoose(e.target.value)} className="mt-1" />
              </div>
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label htmlFor="edit-transporter" className="text-xs font-medium text-muted-foreground">
                  Transport
                </label>
                <Input id="edit-transporter" list="trip-transporters" value={editTransporter} onChange={(e) => setEditTransporter(e.target.value)} className="mt-1" />
              </div>
              <div>
                <label htmlFor="edit-lr" className="text-xs font-medium text-muted-foreground">
                  LR / Bilty no.
                </label>
                <Input id="edit-lr" value={editLrNo} onChange={(e) => setEditLrNo(e.target.value)} className="mt-1" />
              </div>
            </div>
            <div>
              <label htmlFor="edit-remark" className="text-xs font-medium text-muted-foreground">
                Delivery / packing remark
              </label>
              <Input id="edit-remark" value={editRemarks} onChange={(e) => setEditRemarks(e.target.value)} className="mt-1" />
            </div>
            <div>
              <p className="text-xs font-medium text-muted-foreground">Delivery status</p>
              <div className="mt-1.5 flex flex-wrap gap-1.5" role="group" aria-label="Delivery status">
                {["Pending", "Packed", "Dispatched", "Delivered"].map((s) => (
                  <Button
                    key={s}
                    type="button"
                    size="sm"
                    variant={editDeliveryStatus === s ? "default" : "outline"}
                    aria-pressed={editDeliveryStatus === s}
                    onClick={() => setEditDeliveryStatus(s)}
                    className="h-7 px-2.5 text-[11px]"
                  >
                    {s}
                  </Button>
                ))}
              </div>
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div>
                <label htmlFor="edit-payment" className="text-xs font-medium text-muted-foreground">
                  Payment (optional)
                </label>
                <select id="edit-payment" value={editPaymentStatus} onChange={(e) => setEditPaymentStatus(e.target.value)} className={selectClass}>
                  {WEB_PAYMENT_STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </div>
              {editPaymentStatus !== "Unpaid" && (
                <>
                  <div>
                    <label htmlFor="edit-mode" className="text-xs font-medium text-muted-foreground">
                      Mode
                    </label>
                    <select id="edit-mode" value={editPaymentMode} onChange={(e) => setEditPaymentMode(e.target.value)} className={selectClass}>
                      {["Cash", "UPI", "Bank", "Cheque"].map((m) => (
                        <option key={m} value={m}>
                          {m}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="edit-paid" className="text-xs font-medium text-muted-foreground">
                      Amount paid (₹)
                    </label>
                    <Input id="edit-paid" type="number" value={editPaidAmount} onChange={(e) => setEditPaidAmount(e.target.value)} className="mt-1.5" />
                  </div>
                </>
              )}
            </div>
            <FileUpload
              label="Bill / order photo"
              folder={`orders/${editingOrder.visitId}`}
              prefix="bill"
              value={editBillUri}
              onChange={setEditBillUri}
            />
            <div className="flex justify-end gap-2 pt-1">
              <Button variant="outline" size="sm" onClick={() => setEditingOrder(null)}>
                Cancel
              </Button>
              <Button size="sm" onClick={handleSaveEditOrder} disabled={!editItemCode.trim() || !editPieces}>
                Save Changes
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add salesman to trip */}
      <Dialog
        open={isAddMemberOpen}
        onOpenChange={setIsAddMemberOpen}
        title="Add salesman to this trip"
        description="Everybody on the trip can add orders under their own name."
      >
        <div className="space-y-2">
          {otherStaff.length === 0 ? (
            <p className="text-xs text-muted-foreground">All active staff are already on this trip.</p>
          ) : (
            otherStaff.map((e) => (
              <div key={e.id} className="flex items-center justify-between rounded-lg border border-zinc-200 px-3 py-2 dark:border-zinc-800">
                <div>
                  <p className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">{e.name}</p>
                  <p className="text-[10px] text-muted-foreground">{e.phone || e.role}</p>
                </div>
                <Button size="sm" variant="outline" className="h-7 px-2.5 text-[11px]" disabled={busy === `member-${e.id}`} onClick={() => handleAddMember(e)}>
                  <UserPlus className="h-3 w-3" aria-hidden="true" />
                  Add
                </Button>
              </div>
            ))
          )}
        </div>
      </Dialog>

      {/* Close trip with summary */}
      {selectedVisit && (
        <Dialog open={isCloseOpen} onOpenChange={setIsCloseOpen} title={`Close trip ${selectedVisit.visitCode}?`} description={selectedVisit.customerName}>
          {(() => {
            const items = entriesByVisit.get(Number(selectedVisit.id)) || []
            const pcs = items.reduce((s, e) => s + (Number(e.pieces) || 0), 0)
            const value = items.reduce((s, e) => s + billOf(e), 0)
            const notDelivered = items.filter((e) => !isDelivered(e)).length
            const loose = items.reduce((s, e) => s + (Number(e.loosePieces) || 0), 0)
            return (
              <div className="space-y-3 text-xs">
                <div className="grid grid-cols-2 gap-2">
                  <Card className="p-3">
                    <p className="text-muted-foreground">Orders</p>
                    <p className="text-lg font-bold">{items.length}</p>
                  </Card>
                  <Card className="p-3">
                    <p className="text-muted-foreground">Pieces</p>
                    <p className="text-lg font-bold">{pcs}</p>
                  </Card>
                  <Card className="p-3">
                    <p className="text-muted-foreground">Value</p>
                    <p className="text-lg font-bold">₹{formatInr(value)}</p>
                  </Card>
                  <Card className="p-3">
                    <p className="text-muted-foreground">Salesmen</p>
                    <p className="text-sm font-semibold">{membersDisplay(selectedVisit, employees)}</p>
                  </Card>
                </div>
                {(notDelivered > 0 || loose > 0) && (
                  <p className="rounded-lg bg-amber-50 px-3 py-2 text-amber-800 dark:bg-amber-950/40 dark:text-amber-300">
                    {notDelivered > 0 ? `${notDelivered} order(s) not delivered yet. ` : ""}
                    {loose > 0 ? `${loose} loose pieces. ` : ""}
                    You can still update delivery after closing.
                  </p>
                )}
                <p className="text-muted-foreground">No new orders can be added after closing. An admin can reopen it.</p>
                <div className="flex justify-end gap-2">
                  <Button variant="outline" size="sm" onClick={() => setIsCloseOpen(false)}>
                    Cancel
                  </Button>
                  <Button size="sm" onClick={handleCloseTrip} disabled={busy === "close"}>
                    <Lock className="h-3.5 w-3.5" aria-hidden="true" />
                    {busy === "close" ? "Closing..." : "Close Trip"}
                  </Button>
                </div>
              </div>
            )
          })()}
        </Dialog>
      )}

      {/* Join first: a staff member who is not on the trip taps "New Order" */}
      {selectedVisit && (
        <Dialog
          open={isJoinPromptOpen}
          onOpenChange={setIsJoinPromptOpen}
          title="Join this trip?"
          description={`${selectedVisit.customerName} • Trip ${selectedVisit.visitCode}`}
        >
          <div className="space-y-3 pt-1 text-xs">
            <p className="text-muted-foreground">
              You will be added to this trip. Orders you add are saved under your name and show on the customer report.
            </p>
            <p>
              <span className="font-semibold">On this trip: </span>
              {membersDisplay(selectedVisit, employees) || selectedVisit.employeeName}
            </p>
            <div className="flex flex-wrap justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => setIsJoinPromptOpen(false)}>
                Cancel
              </Button>
              {isAdmin && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setIsJoinPromptOpen(false)
                    openAddOrder()
                  }}
                >
                  Add without joining
                </Button>
              )}
              <Button size="sm" onClick={handleJoinAndAddOrder} disabled={busy === `join-${selectedVisit.id}`}>
                <LogIn className="h-3.5 w-3.5" aria-hidden="true" />
                Join & add order
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Quick add customer */}
      <Dialog open={isQuickAddCustomerOpen} onOpenChange={setIsQuickAddCustomerOpen} title="New Customer" description="Created in the Customer master and picked for this trip">
        <div className="space-y-3 pt-1 text-xs">
          <div>
            <label htmlFor="qc-firm" className="text-xs font-medium text-muted-foreground">
              Shop / firm name *
            </label>
            <Input
              id="qc-firm"
              value={quickCustFirm}
              onChange={(e) => {
                setQuickCustFirm(e.target.value)
                setQuickCustError("")
              }}
              className="mt-1"
            />
          </div>
          <div className="grid grid-cols-2 gap-2.5">
            <div>
              <label htmlFor="qc-owner" className="text-xs font-medium text-muted-foreground">
                Owner name
              </label>
              <Input id="qc-owner" value={quickCustOwner} onChange={(e) => setQuickCustOwner(e.target.value)} className="mt-1" />
            </div>
            <div>
              <label htmlFor="qc-phone" className="text-xs font-medium text-muted-foreground">
                Mobile *
              </label>
              <Input
                id="qc-phone"
                inputMode="tel"
                value={quickCustPhone}
                onChange={(e) => {
                  setQuickCustPhone(e.target.value)
                  setQuickCustError("")
                }}
                className="mt-1"
              />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-2">
            <div>
              <label htmlFor="qc-city" className="text-xs font-medium text-muted-foreground">
                City
              </label>
              <Input id="qc-city" value={quickCustCity} onChange={(e) => setQuickCustCity(e.target.value)} className="mt-1" />
            </div>
            <div>
              <label htmlFor="qc-type" className="text-xs font-medium text-muted-foreground">
                Type
              </label>
              <select id="qc-type" value={quickCustType} onChange={(e) => setQuickCustType(e.target.value)} className={selectClass}>
                <option value="Credit">Credit</option>
                <option value="Cash">Cash</option>
              </select>
            </div>
            <div>
              <label htmlFor="qc-days" className="text-xs font-medium text-muted-foreground">
                Credit days
              </label>
              <Input id="qc-days" type="number" value={quickCustCreditDays} onChange={(e) => setQuickCustCreditDays(e.target.value)} className="mt-1" />
            </div>
          </div>
          {quickCustError && (
            <p className="text-[11px] font-semibold text-rose-500" role="alert">
              {quickCustError}
            </p>
          )}
          <div className="flex justify-end gap-2">
            <Button variant="outline" size="sm" onClick={() => setIsQuickAddCustomerOpen(false)}>
              Cancel
            </Button>
            <Button size="sm" onClick={handleSaveQuickCustomer}>
              Save & Select
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Quick add supplier */}
      <Dialog open={isQuickAddSupplierOpen} onOpenChange={setIsQuickAddSupplierOpen} title="New Supplier" description="Created in the Supplier master and picked for this order">
        <div className="space-y-3 pt-1 text-xs">
          <div>
            <label htmlFor="qs-name" className="text-xs font-medium text-muted-foreground">
              Firm / mill name *
            </label>
            <Input
              id="qs-name"
              value={quickSupName}
              onChange={(e) => {
                setQuickSupName(e.target.value)
                setQuickSupError("")
              }}
              className="mt-1"
            />
          </div>
          <div className="grid grid-cols-2 gap-2.5">
            <div>
              <label htmlFor="qs-contact" className="text-xs font-medium text-muted-foreground">
                Contact person
              </label>
              <Input id="qs-contact" value={quickSupContact} onChange={(e) => setQuickSupContact(e.target.value)} className="mt-1" />
            </div>
            <div>
              <label htmlFor="qs-phone" className="text-xs font-medium text-muted-foreground">
                Phone
              </label>
              <Input id="qs-phone" inputMode="tel" value={quickSupPhone} onChange={(e) => setQuickSupPhone(e.target.value)} className="mt-1" />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-2">
            <div>
              <label htmlFor="qs-market" className="text-xs font-medium text-muted-foreground">
                Market
              </label>
              <Input id="qs-market" list="trip-markets" value={quickSupMarket} onChange={(e) => setQuickSupMarket(e.target.value)} className="mt-1" />
            </div>
            <div>
              <label htmlFor="qs-city" className="text-xs font-medium text-muted-foreground">
                City
              </label>
              <Input id="qs-city" value={quickSupCity} onChange={(e) => setQuickSupCity(e.target.value)} className="mt-1" />
            </div>
            <div>
              <label htmlFor="qs-type" className="text-xs font-medium text-muted-foreground">
                Type
              </label>
              <select id="qs-type" value={quickSupType} onChange={(e) => setQuickSupType(e.target.value)} className={selectClass}>
                <option value="Manufacturer">Manufacturer</option>
                <option value="Wholesaler">Wholesaler</option>
                <option value="Trading">Trading</option>
              </select>
            </div>
          </div>
          {quickSupError && (
            <p className="text-[11px] font-semibold text-rose-500" role="alert">
              {quickSupError}
            </p>
          )}
          <div className="flex justify-end gap-2">
            <Button variant="outline" size="sm" onClick={() => setIsQuickAddSupplierOpen(false)}>
              Cancel
            </Button>
            <Button size="sm" onClick={handleSaveQuickSupplier}>
              Save & Select
            </Button>
          </div>
        </div>
      </Dialog>

      <ReportViewerModal
        open={reportModal.open}
        onOpenChange={(open) => setReportModal((prev) => ({ ...prev, open }))}
        title={reportModal.title}
        htmlContent={reportModal.html}
        whatsAppText={reportModal.whatsAppText}
        buildHtml={reportModal.buildHtml}
      />
    </>
  )

  const noticeBanner = notice && (
    <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-2 text-xs font-medium text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-300" role="status">
      {notice}
    </div>
  )

  // ---------------------------------------------------------------------------
  // Trip page
  // ---------------------------------------------------------------------------
  if (selectedVisit) {
    const visitEntries = entriesByVisit.get(Number(selectedVisit.id)) || []
    const shownEntries =
      tripSupplierFilter === "all" ? visitEntries : visitEntries.filter((e) => Number(e.supplierId) === tripSupplierFilter)
    const open = isOpenTrip(selectedVisit)
    const members = tripMembers(selectedVisit, employees)
    const iAmMember = hasMember(selectedVisit, currentEmployee)
    const canWork = !readOnly && (isAdmin || iAmMember)
    const canJoin = !readOnly && open && Boolean(currentEmployee) && !iAmMember
    // Any staff member or admin can add an order to an open trip; a staff member who is not on it joins first
    const canAddOrder = !readOnly && open && (isAdmin || Boolean(currentEmployee))
    const mustJoinFirst = canAddOrder && Boolean(currentEmployee) && !iAmMember
    const totalBilled = visitEntries.reduce((s, e) => s + billOf(e), 0)
    const totalPieces = visitEntries.reduce((s, e) => s + (Number(e.pieces) || 0), 0)
    const deliveredCount = visitEntries.filter(isDelivered).length
    const visitSuppliers = Array.from(new Map(visitEntries.map((e) => [Number(e.supplierId), e.supplierName])).entries())

    return (
      <div className="space-y-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="flex items-start gap-3">
            <Button variant="outline" size="sm" onClick={() => setSelectedVisitId(null)} className="h-8 px-3 text-xs shrink-0">
              ← Trips
            </Button>
            <div>
              <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50 flex flex-wrap items-center gap-2">
                <MapPin className="h-5 w-5 text-blue-500" aria-hidden="true" />
                <span>{selectedVisit.customerName}</span>
                <Badge variant={open ? "success" : "secondary"}>{open ? "Open" : isCancelled(selectedVisit) ? "Cancelled" : "Closed"}</Badge>
                {isPhoneTrip(selectedVisit) && <Badge variant="outline">Phone order</Badge>}
              </h2>
              <p className="text-xs text-muted-foreground mt-0.5">
                Trip {selectedVisit.visitCode} • {formatDate(selectedVisit.date)}
                {selectedVisit.closedBy ? ` • Closed by ${selectedVisit.closedBy}` : ""}
              </p>
              <div className="mt-2 flex flex-wrap items-center gap-1.5">
                <Users className="h-3.5 w-3.5 text-muted-foreground" aria-hidden="true" />
                {members.map((m, i) => (
                  <span
                    key={m.id}
                    className="inline-flex items-center rounded-full bg-zinc-100 px-2.5 py-0.5 text-[11px] font-medium text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200"
                  >
                    {m.name}
                    {i === 0 ? " (started)" : ""}
                  </span>
                ))}
              </div>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {canJoin && (
              <Button size="sm" onClick={() => handleJoin(selectedVisit)} disabled={busy === `join-${selectedVisit.id}`} className="h-8 bg-emerald-600 hover:bg-emerald-700 text-white">
                <LogIn className="h-3.5 w-3.5" aria-hidden="true" />
                Join this trip
              </Button>
            )}
            {canWork && open && (
              <Button variant="outline" size="sm" onClick={() => setIsAddMemberOpen(true)} className="h-8 text-xs">
                <UserPlus className="h-3.5 w-3.5" aria-hidden="true" />
                Add salesman
              </Button>
            )}
            <Button variant="outline" size="sm" onClick={() => openCustomerReport(selectedVisit)} className="h-8 text-xs text-blue-600 border-blue-200 hover:bg-blue-50">
              <FileText className="h-3.5 w-3.5" aria-hidden="true" />
              Customer Report
            </Button>
            {visitEntries.length > 0 && (
              <Button variant="outline" size="sm" onClick={() => printAllPOs(selectedVisit, visitEntries)} className="h-8 text-xs">
                <Printer className="h-3.5 w-3.5" aria-hidden="true" />
                Print POs
              </Button>
            )}
            {canWork && open && (
              <Button variant="secondary" size="sm" onClick={() => setIsCloseOpen(true)} className="h-8 text-xs">
                <Lock className="h-3.5 w-3.5" aria-hidden="true" />
                Close Trip
              </Button>
            )}
            {isAdmin && !open && (
              <Button variant="outline" size="sm" onClick={handleReopenTrip} disabled={busy === "reopen"} className="h-8 text-xs">
                <Unlock className="h-3.5 w-3.5" aria-hidden="true" />
                Reopen
              </Button>
            )}
          </div>
        </div>

        {noticeBanner}
        {canJoin && (
          <p className="rounded-xl bg-sky-50 px-3.5 py-2 text-xs text-sky-800 dark:bg-sky-950/40 dark:text-sky-300">
            Join this trip to add orders under your name.
          </p>
        )}

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Card className="p-4">
            <p className="text-xs text-muted-foreground">Orders</p>
            <p className="text-2xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">{visitEntries.length}</p>
          </Card>
          <Card className="p-4">
            <p className="text-xs text-muted-foreground">Pieces</p>
            <p className="text-2xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">{totalPieces}</p>
          </Card>
          <Card className="p-4">
            <p className="text-xs text-muted-foreground">Value</p>
            <p className="text-2xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">₹{formatInr(totalBilled)}</p>
          </Card>
          <Card className="p-4">
            <p className="text-xs text-muted-foreground">Delivered</p>
            <p className="text-2xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
              {deliveredCount}/{visitEntries.length}
            </p>
          </Card>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Filter className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
          <select
            value={tripSupplierFilter === "all" ? "all" : String(tripSupplierFilter)}
            onChange={(e) => setTripSupplierFilter(e.target.value === "all" ? "all" : Number(e.target.value))}
            aria-label="Filter orders by supplier"
            className="h-8 rounded-full border border-zinc-200 bg-white px-3 text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200"
          >
            <option value="all">🏭 All suppliers</option>
            {visitSuppliers.map(([id, name]) => (
              <option key={id} value={id}>
                {name}
              </option>
            ))}
          </select>
          <div className="ml-auto">
            {canAddOrder && (
              <Button
                size="sm"
                onClick={() => (mustJoinFirst ? setIsJoinPromptOpen(true) : openAddOrder())}
                className="h-8 font-semibold text-xs bg-emerald-600 hover:bg-emerald-700 text-white"
              >
                <Plus className="h-3.5 w-3.5" aria-hidden="true" />
                New Order
              </Button>
            )}
          </div>
        </div>

        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-zinc-200/80 bg-zinc-50/70 font-semibold text-muted-foreground dark:border-zinc-800 dark:bg-zinc-900/50">
                <tr>
                  <th className="py-3 pl-5 pr-2">Order</th>
                  <th className="px-2 py-3">Supplier</th>
                  <th className="px-2 py-3">Item</th>
                  <th className="px-2 py-3">Qty / packing</th>
                  <th className="px-2 py-3">Rate</th>
                  <th className="px-2 py-3">Amount</th>
                  <th className="px-2 py-3">Salesman</th>
                  <th className="px-2 py-3">Delivery</th>
                  <th className="px-2 py-3">Payment</th>
                  <th className="py-3 pl-2 pr-5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
                {shownEntries.length === 0 ? (
                  <tr>
                    <td colSpan={10} className="py-10 text-center text-xs text-muted-foreground">
                      {open ? "No orders yet. Visit a supplier and add the first order." : "No orders on this trip."}
                    </td>
                  </tr>
                ) : (
                  shownEntries.map((entry) => (
                    <tr key={entry.id} className="hover:bg-zinc-50/50 dark:hover:bg-zinc-900/50">
                      <td className="py-3 pl-5 pr-2 font-mono font-bold text-zinc-900 dark:text-zinc-100">#{entry.orderNo}</td>
                      <td className="px-2 py-3 font-medium">
                        {entry.supplierName}
                        {entry.supplierType && <span className="block text-[10px] text-muted-foreground">{entry.supplierType}</span>}
                      </td>
                      <td className="px-2 py-3 font-mono font-semibold">{entry.itemCode}</td>
                      <td className="px-2 py-3">
                        <span className="font-bold">{entry.pieces} pcs</span>
                        <span className="block text-[10px] text-muted-foreground">
                          {entry.caseCount} cs{Number(entry.loosePieces) > 0 ? ` + ${entry.loosePieces} loose` : ""}
                        </span>
                        {entry.mixedPackNote && <span className="block text-[10px] italic text-zinc-400">{entry.mixedPackNote}</span>}
                      </td>
                      <td className="px-2 py-3">₹{entry.rate || entry.pricePerPiece || 0}</td>
                      <td className="px-2 py-3 font-bold whitespace-nowrap">₹{formatInr(billOf(entry))}</td>
                      <td className="px-2 py-3">{entry.salesmanName || selectedVisit.employeeName}</td>
                      <td className="px-2 py-3">
                        <Badge variant={isDelivered(entry) ? "success" : "info"}>{entry.deliveryStatus || "Pending"}</Badge>
                        {entry.transporter && <span className="block text-[10px] text-muted-foreground mt-0.5">🚛 {entry.transporter}</span>}
                      </td>
                      <td className="px-2 py-3">
                        <Badge variant="outline" className="text-[10px]">
                          {entry.paymentStatus || "Unpaid"}
                        </Badge>
                      </td>
                      <td className="py-3 pl-2 pr-5 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {!readOnly && (isAdmin || iAmMember) && (
                            <Button variant="outline" size="sm" onClick={() => openEditOrder(entry)} className="h-6 text-[10px] px-2">
                              <Pencil className="h-3 w-3" aria-hidden="true" />
                              Edit
                            </Button>
                          )}
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => openSupplierInvoice(entry, selectedVisit)}
                            className="h-6 text-[10px] px-2 text-blue-600 border-blue-200"
                          >
                            <FileText className="h-3 w-3" aria-hidden="true" />
                            PO
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>

        {dialogs}
      </div>
    )
  }

  // ---------------------------------------------------------------------------
  // Trip list
  // ---------------------------------------------------------------------------
  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">Market Trips</h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            New trip → customer → salesmen → visit suppliers → add orders → close trip.
          </p>
        </div>
        {!readOnly && (
          <Button size="sm" onClick={openNewTrip} className="h-8 font-semibold text-xs self-start sm:self-auto">
            <Plus className="h-3.5 w-3.5" aria-hidden="true" />
            New Trip
          </Button>
        )}
      </div>

      {noticeBanner}

      {/* Trips started by others that this staff member can join from here */}
      {!readOnly && joinableVisits.length > 0 && (
        <Card className="p-4">
          <p className="text-xs font-bold text-zinc-900 dark:text-zinc-100">Open trips you can join</p>
          <p className="text-[11px] text-muted-foreground">Join to add orders under your name.</p>
          <div className="mt-2 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {joinableVisits.slice(0, 9).map((v) => (
              <div key={v.id} className="flex items-center justify-between gap-2 rounded-xl border border-zinc-200 px-3 py-2 dark:border-zinc-800">
                <button type="button" className="min-w-0 text-left" onClick={() => setSelectedVisitId(Number(v.id))}>
                  <p className="truncate text-xs font-semibold text-zinc-900 dark:text-zinc-100">{v.customerName}</p>
                  <p className="truncate text-[10px] text-muted-foreground">
                    {v.date} • {membersDisplay(v, employees)}
                  </p>
                </button>
                <Button size="sm" className="h-7 shrink-0 px-2.5 text-[11px]" disabled={busy === `join-${v.id}`} onClick={() => handleJoin(v)}>
                  <LogIn className="h-3 w-3" aria-hidden="true" />
                  Join
                </Button>
              </div>
            ))}
          </div>
        </Card>
      )}

      <div className="flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-wrap items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-2 h-4 w-4 text-muted-foreground" aria-hidden="true" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Customer, trip code or salesman"
              className="h-8 w-64 pl-9 text-xs"
              aria-label="Search trips"
            />
            {search && (
              <button type="button" onClick={() => setSearch("")} className="absolute right-3 top-2 text-muted-foreground" aria-label="Clear search">
                <X className="h-4 w-4" />
              </button>
            )}
          </div>
          {!readOnly && (
            <select
              value={selectedEmployeeId === "all" ? "all" : String(selectedEmployeeId)}
              onChange={(e) => setSelectedEmployeeId(e.target.value === "all" ? "all" : Number(e.target.value))}
              aria-label="Filter by salesman"
              className="h-8 rounded-full border border-zinc-200 bg-white px-3 text-xs font-medium text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200"
            >
              <option value="all">👥 All salesmen</option>
              {employees.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  👤 {emp.name}
                </option>
              ))}
            </select>
          )}
        </div>
        <Tabs
          value={statusFilter}
          onValueChange={setStatusFilter}
          options={[
            { value: "all", label: "All", count: baseVisits.length },
            { value: "active", label: "Open", count: baseVisits.filter(isOpenTrip).length },
            { value: "closed", label: "Closed", count: baseVisits.filter((v) => !isOpenTrip(v)).length },
          ]}
        />
      </div>

      <DateRangeFilter value={dateRange} onChange={setDateRange} />

      {activeEmployee && (
        <div className="flex items-center justify-between rounded-xl bg-zinc-100 px-3.5 py-2 text-xs dark:bg-zinc-900">
          <span className="flex items-center gap-2">
            <UserCheck className="h-4 w-4" aria-hidden="true" />
            Trips with <strong>{activeEmployee.name}</strong> (started or joined)
          </span>
          <button type="button" onClick={() => setSelectedEmployeeId("all")} className="text-[11px] font-semibold text-red-600 hover:underline">
            Show all
          </button>
        </div>
      )}

      <Card className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-zinc-200/80 bg-zinc-50/70 font-semibold text-muted-foreground dark:border-zinc-800 dark:bg-zinc-900/50">
              <tr>
                <th className="py-3 pl-5 pr-2">Date</th>
                <th className="px-2 py-3">Customer</th>
                <th className="px-2 py-3">Salesmen</th>
                <th className="px-2 py-3">Orders</th>
                <th className="px-2 py-3">Value</th>
                <th className="px-2 py-3">Status</th>
                <th className="py-3 pl-2 pr-5 text-right">Report</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
              {filteredVisits.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-xs text-muted-foreground">
                    No trips found{activeEmployee ? ` for ${activeEmployee.name}` : ""}.
                  </td>
                </tr>
              ) : (
                filteredVisits.map((visit) => {
                  const items = entriesByVisit.get(Number(visit.id)) || []
                  const open = isOpenTrip(visit)
                  return (
                    <tr
                      key={visit.id}
                      className="cursor-pointer hover:bg-zinc-50/50 dark:hover:bg-zinc-900/50"
                      onClick={() => setSelectedVisitId(Number(visit.id))}
                    >
                      <td className="py-3 pl-5 pr-2 whitespace-nowrap">
                        <span className="font-medium">{formatDate(visit.date)}</span>
                        <span className="block font-mono text-[10px] text-muted-foreground">{visit.visitCode}</span>
                      </td>
                      <td className="px-2 py-3 font-semibold text-zinc-900 dark:text-zinc-100">
                        {visit.customerName}
                        {isPhoneTrip(visit) && (
                          <span className="ml-1.5 inline-flex items-center rounded-full bg-violet-100 px-1.5 py-0.5 text-[10px] font-semibold text-violet-700 dark:bg-violet-950/50 dark:text-violet-300">
                            Phone
                          </span>
                        )}
                      </td>
                      <td className="px-2 py-3">{membersDisplay(visit, employees)}</td>
                      <td className="px-2 py-3">
                        <span className="font-semibold">{items.length}</span> • {items.reduce((s, e) => s + (Number(e.pieces) || 0), 0)} pcs
                      </td>
                      <td className="px-2 py-3 whitespace-nowrap">₹{formatInr(items.reduce((s, e) => s + billOf(e), 0))}</td>
                      <td className="px-2 py-3">
                        <Badge variant={open ? "success" : "secondary"}>{open ? "Open" : isCancelled(visit) ? "Cancelled" : "Closed"}</Badge>
                      </td>
                      <td className="py-3 pl-2 pr-5 text-right" onClick={(e) => e.stopPropagation()}>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openCustomerReport(visit)}
                          className="h-7 text-[11px] px-2.5 text-blue-600 border-blue-200 hover:bg-blue-50"
                          title="Customer report (PDF / WhatsApp)"
                        >
                          <FileText className="h-3 w-3" aria-hidden="true" />
                          Report
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

      {dialogs}
    </div>
  )
}
