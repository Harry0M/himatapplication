import React, { createContext, useContext, useEffect, useState } from "react"
import { ref, onValue, set, remove, update } from "firebase/database"
import { rtdb } from "../lib/firebase"
import {
  Visit,
  PurchaseEntry,
  Customer,
  Supplier,
  PackGroup,
  Transaction,
  Employee,
  Product,
  Brand,
  Transporter,
  Market,
  SoftDeletedItem,
  CustomerRegistrationRequest
} from "../types"

// Helper to robustly extract arrays from Firebase snapshots (handles sparse arrays and keyed objects)
function parseRtdbList<T extends { id?: any }>(val: any): T[] {
  if (!val) return []
  if (Array.isArray(val)) {
    return val
      .map((item, idx) => {
        if (!item) return null
        if (typeof item === "object") {
          const rawItem = item as any
          const id = rawItem.id !== undefined && rawItem.id !== null ? Number(rawItem.id) : idx
          const isDeleted = Boolean(rawItem.isDeleted ?? rawItem.deleted ?? false)
          const isActive = Boolean(rawItem.isActive ?? rawItem.active ?? true)
          const isBlocked = Boolean(rawItem.isBlocked ?? rawItem.blocked ?? false)
          return {
            ...item,
            id,
            isDeleted,
            isActive,
            isBlocked,
          }
        }
        return item
      })
      .filter(Boolean) as T[]
  }
  if (typeof val === "object") {
    return Object.entries(val)
      .map(([key, item]) => {
        if (!item) return null
        if (typeof item === "object") {
          const numKey = Number(key)
          const rawItem = item as any
          const id =
            rawItem.id !== undefined && rawItem.id !== null
              ? Number(rawItem.id)
              : !isNaN(numKey)
              ? numKey
              : key
          const isDeleted = Boolean(rawItem.isDeleted ?? rawItem.deleted ?? false)
          const isActive = Boolean(rawItem.isActive ?? rawItem.active ?? true)
          const isBlocked = Boolean(rawItem.isBlocked ?? rawItem.blocked ?? false)
          return {
            ...item,
            id,
            isDeleted,
            isActive,
            isBlocked,
          }
        }
        return item
      })
      .filter(Boolean) as T[]
  }
  return []
}

// Helper to recursively strip undefined properties from an object so Firebase RTDB set/update never fails
function sanitizePayload<T>(obj: T): T {
  if (obj === null || obj === undefined) {
    return obj
  }
  if (Array.isArray(obj)) {
    return obj
      .filter((item) => item !== undefined)
      .map((item) => sanitizePayload(item)) as unknown as T
  }
  if (typeof obj === "object") {
    const clean: Record<string, any> = {}
    for (const [key, value] of Object.entries(obj)) {
      if (value !== undefined) {
        clean[key] = sanitizePayload(value)
      }
    }
    return clean as T
  }
  return obj
}

export interface EmployeeStats {
  employee: Employee
  visits: Visit[]
  entries: PurchaseEntry[]
  totalVisitsCount: number
  activeVisitsCount: number
  completedVisitsCount: number
  totalOrdersCount: number
  totalPieces: number
  totalInvoiced: number
  totalPaid: number
  totalDues: number
  pendingDeliveriesCount: number
  deliveredCount: number
}

interface DataContextType {
  visits: Visit[]
  entries: PurchaseEntry[]
  customers: Customer[]
  suppliers: Supplier[]
  products: Product[]
  brands: Brand[]
  transporters: Transporter[]
  markets: Market[]
  packGroups: PackGroup[]
  transactions: Transaction[]
  employees: Employee[]
  loading: boolean

  // Soft Deletions for Admin Approval
  deletedItems: SoftDeletedItem[]
  pendingDeletionsCount: number
  confirmPermanentDelete: (collection: string, id: number | string) => Promise<void>
  restoreDeletedItem: (collection: string, id: number | string) => Promise<void>

  // Computed metrics
  pendingPaymentsCount: number
  totalPendingDues: number
  pendingDeliveriesCount: number
  activeTripsCount: number
  looseEntriesCount: number
  totalLoosePieces: number

  // Employee Performance & Filter
  allEmployeeStats: EmployeeStats[]
  getEmployeeStats: (employeeId: number) => EmployeeStats | null
  selectedEmployeeId: number | "all"
  setSelectedEmployeeId: (id: number | "all") => void

  // Actions
  updatePayment: (
    entryId: number,
    paymentStatus: string,
    paymentMode: string,
    paidAmount: number,
    paymentRemarks?: string
  ) => Promise<void>
  updateDelivery: (
    entryId: number,
    deliveryStatus: string,
    transporter?: string,
    lrNo?: string,
    lrDate?: string
  ) => Promise<void>
  saveVisit: (visit: Visit) => Promise<void>
  deleteVisit: (id: number) => Promise<void>
  saveCustomer: (customer: Customer) => Promise<void>
  deleteCustomer: (id: number) => Promise<void>
  saveSupplier: (supplier: Supplier) => Promise<void>
  deleteSupplier: (id: number) => Promise<void>
  saveBrand: (brand: Brand) => Promise<void>
  deleteBrand: (id: number) => Promise<void>
  saveTransporter: (transporter: Transporter) => Promise<void>
  deleteTransporter: (id: number) => Promise<void>
  saveMarket: (market: Market) => Promise<void>
  deleteMarket: (id: number) => Promise<void>
  saveEmployee: (employee: Employee) => Promise<void>
  deleteEmployee: (id: number) => Promise<void>
  suspendEmployee: (id: number, reason?: string) => Promise<void>
  resumeEmployee: (id: number) => Promise<void>
  deactivateEmployee: (id: number, reason?: string) => Promise<void>
  saveProduct: (product: Product) => Promise<void>
  deleteProduct: (id: number) => Promise<void>
  savePurchaseEntry: (entry: PurchaseEntry) => Promise<void>
  deletePurchaseEntry: (id: number) => Promise<void>
  createPackGroup: (
    visitId: number,
    selectedEntries: PurchaseEntry[],
    targetCaseSize: number,
    customNote?: string
  ) => Promise<void>
  deletePackGroup: (groupId: number) => Promise<void>

  // Customer Self-Registration Requests (User Requests)
  registrationRequests: CustomerRegistrationRequest[]
  pendingRegistrationRequestsCount: number
  submitRegistrationRequest: (
    request: Omit<CustomerRegistrationRequest, "id" | "createdAt" | "status" | "phoneVerified"> & { verificationUid?: string }
  ) => Promise<string>
  approveRegistrationRequest: (
    requestId: string,
    options: {
      assignedAgentId: number | string
      assignedAgentName: string
      creditType?: "Cash" | "Credit"
      creditDays?: number
      creditLimit?: number
    }
  ) => Promise<number>
  rejectRegistrationRequest: (requestId: string, reason?: string) => Promise<void>
  deleteRegistrationRequest: (requestId: string) => Promise<void>
}

const DataContext = createContext<DataContextType | undefined>(undefined)

export const DataProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [rawVisits, setRawVisits] = useState<Visit[]>([])
  const [rawEntries, setRawEntries] = useState<PurchaseEntry[]>([])
  const [rawCustomers, setRawCustomers] = useState<Customer[]>([])
  const [rawSuppliers, setRawSuppliers] = useState<Supplier[]>([])
  const [rawEmployees, setRawEmployees] = useState<Employee[]>([])
  const [rawProducts, setRawProducts] = useState<Product[]>([])
  const [rawBrands, setRawBrands] = useState<Brand[]>([])
  const [rawTransporters, setRawTransporters] = useState<Transporter[]>([])
  const [rawMarkets, setRawMarkets] = useState<Market[]>([])
  const [packGroups, setPackGroups] = useState<PackGroup[]>([])
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [rawRegistrationRequests, setRawRegistrationRequests] = useState<CustomerRegistrationRequest[]>([])
  const [loading, setLoading] = useState<boolean>(true)

  // Global employee filter state
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<number | "all">("all")

  // Real-time synchronization
  useEffect(() => {
    let activeListeners = 0
    const totalListeners = 12

    const checkLoading = () => {
      activeListeners++
      if (activeListeners >= totalListeners) {
        setLoading(false)
      }
    }

    // 1. Visits
    const visitsRef = ref(rtdb, "visits")
    const unsubVisits = onValue(visitsRef, (snapshot) => {
      if (snapshot.exists()) {
        setRawVisits(parseRtdbList<Visit>(snapshot.val()))
      } else {
        setRawVisits([])
      }
      checkLoading()
    })

    // 2. Purchase Entries
    const entriesRef = ref(rtdb, "purchase_entries")
    const unsubEntries = onValue(entriesRef, (snapshot) => {
      if (snapshot.exists()) {
        setRawEntries(parseRtdbList<PurchaseEntry>(snapshot.val()))
      } else {
        setRawEntries([])
      }
      checkLoading()
    })

    // 3. Customers
    const customersRef = ref(rtdb, "customers")
    const unsubCustomers = onValue(customersRef, (snapshot) => {
      if (snapshot.exists()) {
        setRawCustomers(parseRtdbList<Customer>(snapshot.val()))
      } else {
        setRawCustomers([])
      }
      checkLoading()
    })

    // 4. Suppliers
    const suppliersRef = ref(rtdb, "suppliers")
    const unsubSuppliers = onValue(suppliersRef, (snapshot) => {
      if (snapshot.exists()) {
        setRawSuppliers(parseRtdbList<Supplier>(snapshot.val()))
      } else {
        setRawSuppliers([])
      }
      checkLoading()
    })

    // 5. Pack Groups
    const packGroupsRef = ref(rtdb, "pack_groups")
    const unsubPackGroups = onValue(packGroupsRef, (snapshot) => {
      if (snapshot.exists()) {
        setPackGroups(parseRtdbList<PackGroup>(snapshot.val()))
      } else {
        setPackGroups([])
      }
      checkLoading()
    })

    // 6. Transactions
    const txRef = ref(rtdb, "transactions")
    const unsubTx = onValue(txRef, (snapshot) => {
      if (snapshot.exists()) {
        setTransactions(parseRtdbList<Transaction>(snapshot.val()))
      } else {
        setTransactions([])
      }
      checkLoading()
    })

    // 7. Employees
    const empRef = ref(rtdb, "employees")
    const unsubEmp = onValue(empRef, (snapshot) => {
      if (snapshot.exists()) {
        setRawEmployees(parseRtdbList<Employee>(snapshot.val()))
      } else {
        setRawEmployees([])
      }
      checkLoading()
    })

    // 8. Products
    const productsRef = ref(rtdb, "products")
    const unsubProducts = onValue(productsRef, (snapshot) => {
      if (snapshot.exists()) {
        setRawProducts(parseRtdbList<Product>(snapshot.val()))
      } else {
        setRawProducts([])
      }
      checkLoading()
    })

    // 9. Brands
    const brandsRef = ref(rtdb, "brands")
    const unsubBrands = onValue(brandsRef, (snapshot) => {
      if (snapshot.exists()) {
        setRawBrands(parseRtdbList<Brand>(snapshot.val()))
      } else {
        setRawBrands([])
      }
      checkLoading()
    })

    // 10. Transporters
    const transportersRef = ref(rtdb, "transporters")
    const unsubTransporters = onValue(transportersRef, (snapshot) => {
      if (snapshot.exists()) {
        setRawTransporters(parseRtdbList<Transporter>(snapshot.val()))
      } else {
        setRawTransporters([])
      }
      checkLoading()
    })

    // 11. Markets
    const marketsRef = ref(rtdb, "markets")
    const unsubMarkets = onValue(marketsRef, (snapshot) => {
      if (snapshot.exists()) {
        setRawMarkets(parseRtdbList<Market>(snapshot.val()))
      } else {
        setRawMarkets([])
      }
      checkLoading()
    })

    // 12. Customer Registration Requests (User Requests)
    const regRequestsRef = ref(rtdb, "customer_registration_requests")
    const unsubRegRequests = onValue(regRequestsRef, (snapshot) => {
      if (snapshot.exists()) {
        setRawRegistrationRequests(parseRtdbList<CustomerRegistrationRequest>(snapshot.val()))
      } else {
        setRawRegistrationRequests([])
      }
      checkLoading()
    })

    return () => {
      unsubVisits()
      unsubEntries()
      unsubCustomers()
      unsubSuppliers()
      unsubPackGroups()
      unsubTx()
      unsubEmp()
      unsubProducts()
      unsubBrands()
      unsubTransporters()
      unsubMarkets()
      unsubRegRequests()
    }
  }, [])

  // Active filtered datasets (excluding soft-deleted)
  const visits = React.useMemo(() => {
    return rawVisits.filter((v) => !v.isDeleted)
  }, [rawVisits])

  const entries = React.useMemo(() => {
    return rawEntries.filter((e) => !e.isDeleted)
  }, [rawEntries])

  const products = React.useMemo(() => {
    return rawProducts.filter((p) => !p.isDeleted)
  }, [rawProducts])

  const brands = React.useMemo(() => {
    return rawBrands
      .filter((b) => !b.isDeleted && !(b as any).deleted)
      .map((b) => ({
        ...b,
        brandName: b.brandName || (b as any).name || `Brand #${b.id}`,
        isActive: b.isActive ?? (b as any).active ?? true,
      }))
  }, [rawBrands])

  const transporters = React.useMemo(() => {
    return rawTransporters
      .filter((t) => !t.isDeleted && !(t as any).deleted)
      .map((t) => ({
        ...t,
        transporterName: t.transporterName || (t as any).name || `Transporter #${t.id}`,
        phone: t.phone || (t as any).phone1 || "",
        isActive: t.isActive ?? (t as any).active ?? true,
      }))
  }, [rawTransporters])

  const markets = React.useMemo(() => {
    return rawMarkets
      .filter((m) => !m.isDeleted && !(m as any).deleted)
      .map((m) => ({
        ...m,
        marketName: m.marketName || (m as any).name || `Market #${m.id}`,
        isActive: m.isActive ?? (m as any).active ?? true,
      }))
  }, [rawMarkets])

  // Synthesize employees: combine explicit employees from RTDB with any agent found in visits
  const employees = React.useMemo(() => {
    const map = new Map<number, Employee>()

    // Add explicit employees from RTDB (including Active, Suspended, Deactivated)
    rawEmployees.forEach((emp) => {
      if (emp && emp.id) {
        const empStatus = emp.status || (emp.isDeleted ? "Deactivated" : emp.isBlocked ? "Suspended" : "Active")
        const empBlocked = emp.isBlocked ?? (empStatus === "Suspended" || emp.isDeleted)
        map.set(Number(emp.id), {
          ...emp,
          status: empStatus,
          isBlocked: empBlocked,
        })
      }
    })

    // Synthesize any employee referenced in visits who might not be in /employees node
    visits.forEach((v) => {
      const empId = Number(v.employeeId)
      if (empId > 0 && !map.has(empId)) {
        map.set(empId, {
          id: empId,
          employeeId: `EMP-0${empId}`,
          name: v.employeeName || `Agent #${empId}`,
          role: "Salesman",
          phone: "+91 98000 00000",
          status: "Active",
          isBlocked: false,
        })
      }
    })

    return Array.from(map.values())
  }, [rawEmployees, visits])

  // Synthesize customers: ensure any customer referenced in visits is never missing
  const customers = React.useMemo(() => {
    const map = new Map<number, Customer>()
    rawCustomers.forEach((c) => {
      if (c && c.id && !c.isDeleted) {
        map.set(Number(c.id), {
          ...c,
          firmName: c.firmName || c.name,
          city: c.city || "Ahmedabad",
          state: c.state || "Gujarat",
          gstNumber: c.gstNumber || c.gstin || "",
          gstin: c.gstin || c.gstNumber || "",
        })
      }
    })

    visits.forEach((v) => {
      if (v.customerId && !map.has(Number(v.customerId))) {
        map.set(Number(v.customerId), {
          id: Number(v.customerId),
          customerId: `CUST-${v.customerId}`,
          name: v.customerName || `Customer #${v.customerId}`,
          firmName: v.customerName || `Customer #${v.customerId}`,
          city: "Ahmedabad",
          state: "Gujarat",
          phone: "+91 98765 00000",
        })
      }
    })

    return Array.from(map.values())
  }, [rawCustomers, visits])

  // Customer registration requests (User Requests)
  const registrationRequests = React.useMemo(() => {
    return [...rawRegistrationRequests].sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0))
  }, [rawRegistrationRequests])

  const pendingRegistrationRequestsCount = React.useMemo(() => {
    return registrationRequests.filter((r) => r.status === "PENDING").length
  }, [registrationRequests])

  // Synthesize suppliers: ensure any supplier referenced in entries is never missing
  const suppliers = React.useMemo(() => {
    const map = new Map<number, Supplier>()
    rawSuppliers.forEach((s) => {
      if (s && s.id && !s.isDeleted) {
        map.set(Number(s.id), {
          ...s,
          marketArea: s.marketArea || s.city || "Textile Market",
          city: s.city || s.marketArea || "Surat",
          gstNumber: s.gstNumber || s.gstin || "",
        })
      }
    })

    entries.forEach((e) => {
      if (e.supplierId && !map.has(Number(e.supplierId))) {
        map.set(Number(e.supplierId), {
          id: Number(e.supplierId),
          supplierId: `SUP-${e.supplierId}`,
          name: e.supplierName || `Supplier #${e.supplierId}`,
          type: e.supplierType || "Manufacturer",
          marketArea: "Textile Market",
          city: "Surat",
          phone: "+91 98765 11111",
        })
      }
    })

    return Array.from(map.values())
  }, [rawSuppliers, entries])

  // Collect all soft-deleted items across entities for Admin Deletions view
  const deletedItems: SoftDeletedItem[] = React.useMemo(() => {
    const list: SoftDeletedItem[] = []

    rawVisits.forEach((v) => {
      if (v && v.isDeleted) {
        list.push({
          id: v.id,
          collection: "visits",
          entityType: "Visit",
          title: `Visit #${v.visitCode || v.id} - ${v.customerName || "Customer"}`,
          subtitle: `Date: ${v.date || "N/A"} • Salesman: ${v.employeeName || "N/A"}`,
          deletedAt: v.deletedAt || Date.now(),
          deletedBy: v.deletedBy || "Salesman",
          deletedByEmail: v.deletedByEmail || "",
          deletedByRole: v.deletedByRole || "Salesman",
          deletionStatus: v.deletionStatus || "PENDING_CONFIRMATION",
          deletionReason: v.deletionReason || "",
          originalData: v,
        })
      }
    })

    rawEntries.forEach((e) => {
      if (e && e.isDeleted) {
        list.push({
          id: e.id,
          collection: "purchase_entries",
          entityType: "Purchase Entry",
          title: `Order #${e.orderNo || e.id} - ${e.itemCode}`,
          subtitle: `Supplier: ${e.supplierName || "N/A"} • ${e.pieces} pcs • Total: ₹${e.grandTotalWithGst || e.totalAmount}`,
          deletedAt: e.deletedAt || Date.now(),
          deletedBy: e.deletedBy || "Salesman",
          deletedByEmail: e.deletedByEmail || "",
          deletedByRole: e.deletedByRole || "Salesman",
          deletionStatus: e.deletionStatus || "PENDING_CONFIRMATION",
          deletionReason: e.deletionReason || "",
          originalData: e,
        })
      }
    })

    rawCustomers.forEach((c) => {
      if (c && c.isDeleted) {
        list.push({
          id: c.id,
          collection: "customers",
          entityType: "Customer",
          title: `${c.firmName || c.name} (${c.customerId || c.id})`,
          subtitle: `City: ${c.city || "Ahmedabad"} • Phone: ${c.phone}`,
          deletedAt: c.deletedAt || Date.now(),
          deletedBy: c.deletedBy || "Salesman",
          deletedByEmail: c.deletedByEmail || "",
          deletedByRole: c.deletedByRole || "Salesman",
          deletionStatus: c.deletionStatus || "PENDING_CONFIRMATION",
          deletionReason: c.deletionReason || "",
          originalData: c,
        })
      }
    })

    rawSuppliers.forEach((s) => {
      if (s && s.isDeleted) {
        list.push({
          id: s.id,
          collection: "suppliers",
          entityType: "Supplier",
          title: `${s.firmName || s.name} (${s.type || "Supplier"})`,
          subtitle: `Market: ${s.marketArea || "N/A"} • Phone: ${s.phone}`,
          deletedAt: s.deletedAt || Date.now(),
          deletedBy: s.deletedBy || "Salesman",
          deletedByEmail: s.deletedByEmail || "",
          deletedByRole: s.deletedByRole || "Salesman",
          deletionStatus: s.deletionStatus || "PENDING_CONFIRMATION",
          deletionReason: s.deletionReason || "",
          originalData: s,
        })
      }
    })

    rawProducts.forEach((p) => {
      if (p && p.isDeleted) {
        list.push({
          id: p.id,
          collection: "products",
          entityType: "Product",
          title: `${p.name} (${p.productCode})`,
          subtitle: `Supplier: ${p.supplierName} • Rate: ₹${p.defaultRate}`,
          deletedAt: p.deletedAt || Date.now(),
          deletedBy: p.deletedBy || "Salesman",
          deletedByEmail: p.deletedByEmail || "",
          deletedByRole: p.deletedByRole || "Salesman",
          deletionStatus: p.deletionStatus || "PENDING_CONFIRMATION",
          deletionReason: p.deletionReason || "",
          originalData: p,
        })
      }
    })

    rawEmployees.forEach((emp) => {
      if (emp && emp.isDeleted) {
        list.push({
          id: emp.id,
          collection: "employees",
          entityType: "Employee",
          title: `${emp.name} (${emp.employeeId || `EMP-${emp.id}`})`,
          subtitle: `Role: ${emp.role} • Phone: ${emp.phone || "N/A"}`,
          deletedAt: emp.deletedAt || Date.now(),
          deletedBy: emp.deletedBy || "Admin",
          deletedByEmail: emp.deletedByEmail || "",
          deletedByRole: emp.deletedByRole || "Owner",
          deletionStatus: emp.deletionStatus || "CONFIRMED",
          deletionReason: emp.deletionReason || "Staff Deactivated",
          originalData: emp,
        })
      }
    })

    return list.sort((a, b) => (b.deletedAt || 0) - (a.deletedAt || 0))
  }, [rawVisits, rawEntries, rawCustomers, rawSuppliers, rawProducts, rawEmployees])

  const pendingDeletionsCount = deletedItems.length

  // Calculate packed entry IDs from all pack groups
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

  // Computed Metrics
  const pendingPayments = React.useMemo(() => {
    return entries.filter(
      (e) =>
        e.paymentStatus?.toLowerCase() !== "paid" &&
        e.paymentStatus?.toLowerCase() !== "received"
    )
  }, [entries])

  const pendingPaymentsCount = pendingPayments.length

  const totalPendingDues = React.useMemo(() => {
    return pendingPayments.reduce((sum, e) => {
      const bill = (Number(e.totalAmount) || 0) + (Number(e.gstAmount) || 0)
      const paid = Number(e.paidAmount) || 0
      return sum + Math.max(0, bill - paid)
    }, 0)
  }, [pendingPayments])

  const pendingDeliveriesCount = React.useMemo(() => {
    return entries.filter((e) => e.deliveryStatus?.toLowerCase() !== "delivered").length
  }, [entries])

  const activeTripsCount = React.useMemo(() => {
    return visits.filter((v) => v.status?.toLowerCase() === "active").length
  }, [visits])

  // Only consider entries that have loose pieces AND are NOT packed in any pack group or note
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

  const looseEntriesCount = looseEntries.length
  const totalLoosePieces = React.useMemo(() => {
    return looseEntries.reduce((sum, e) => sum + (Number(e.loosePieces) || 0), 0)
  }, [looseEntries])

  // Compute stats per employee
  const allEmployeeStats = React.useMemo(() => {
    return employees.map((emp) => {
      // Find all visits by this employee (matching by id or name)
      const empVisits = visits.filter(
        (v) =>
          Number(v.employeeId) === emp.id ||
          (v.employeeName && v.employeeName.trim().toLowerCase() === emp.name.trim().toLowerCase())
      )
      const visitIdsSet = new Set(empVisits.map((v) => v.id))

      // Find all purchase entries for these visits
      const empEntries = entries.filter((e) => visitIdsSet.has(Number(e.visitId)))

      const totalInvoiced = empEntries.reduce((sum, e) => {
        return sum + (Number(e.totalAmount) || 0) + (Number(e.gstAmount) || 0)
      }, 0)

      const totalPaid = empEntries.reduce((sum, e) => sum + (Number(e.paidAmount) || 0), 0)
      const totalDues = Math.max(0, totalInvoiced - totalPaid)
      const totalPieces = empEntries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)

      const activeVisitsCount = empVisits.filter((v) => v.status?.toLowerCase() === "active").length
      const completedVisitsCount = empVisits.filter(
        (v) => v.status?.toLowerCase() === "completed"
      ).length

      const deliveredCount = empEntries.filter(
        (e) => e.deliveryStatus?.toLowerCase() === "delivered"
      ).length
      const pendingDeliveriesCount = empEntries.filter(
        (e) => e.deliveryStatus?.toLowerCase() !== "delivered"
      ).length

      return {
        employee: emp,
        visits: empVisits,
        entries: empEntries,
        totalVisitsCount: empVisits.length,
        activeVisitsCount,
        completedVisitsCount,
        totalOrdersCount: empEntries.length,
        totalPieces,
        totalInvoiced,
        totalPaid,
        totalDues,
        pendingDeliveriesCount,
        deliveredCount,
      }
    })
  }, [employees, visits, entries])

  const getEmployeeStats = (employeeId: number): EmployeeStats | null => {
    return allEmployeeStats.find((s) => s.employee.id === employeeId) || null
  }

  // Mutation Actions
  const updatePayment = async (
    entryId: number,
    paymentStatus: string,
    paymentMode: string,
    paidAmount: number,
    paymentRemarks?: string
  ) => {
    const entryRef = ref(rtdb, `purchase_entries/${entryId}`)
    await update(entryRef, sanitizePayload({
      paymentStatus,
      paymentMode,
      paidAmount,
      paymentRemarks: paymentRemarks || "",
    }))
  }

  const updateDelivery = async (
    entryId: number,
    deliveryStatus: string,
    transporter?: string,
    lrNo?: string,
    lrDate?: string
  ) => {
    const entryRef = ref(rtdb, `purchase_entries/${entryId}`)
    await update(entryRef, sanitizePayload({
      deliveryStatus,
      transporter: transporter || "",
      lrNo: lrNo || "",
      lrDate: lrDate || "",
    }))
  }

  const saveVisit = async (visit: Visit) => {
    const visitRef = ref(rtdb, `visits/${visit.id}`)
    await set(visitRef, sanitizePayload(visit))
  }

  const deleteVisit = async (id: number) => {
    const visitRef = ref(rtdb, `visits/${id}`)
    await remove(visitRef)
  }

  const saveCustomer = async (customer: Customer) => {
    const customerRef = ref(rtdb, `customers/${customer.id}`)
    const payload = {
      ...customer,
      firmName: customer.firmName || customer.name,
      city: customer.city || "Ahmedabad",
      state: customer.state || "Gujarat",
      gstin: customer.gstin || customer.gstNumber || "",
      gstNumber: customer.gstNumber || customer.gstin || "",
    }
    await set(customerRef, sanitizePayload(payload))
  }

  const deleteCustomer = async (id: number) => {
    const customerRef = ref(rtdb, `customers/${id}`)
    await remove(customerRef)
  }

  const saveSupplier = async (supplier: Supplier) => {
    const supplierRef = ref(rtdb, `suppliers/${supplier.id}`)
    const payload = {
      ...supplier,
      firmName: supplier.firmName || supplier.name,
      gstin: supplier.gstin || supplier.gstNumber || "",
      gstNumber: supplier.gstNumber || supplier.gstin || "",
    }
    await set(supplierRef, sanitizePayload(payload))
    if (supplier.type?.toLowerCase() === "manufacturer") {
      const mfgRef = ref(rtdb, `manufacturers/${supplier.id}`)
      await set(mfgRef, sanitizePayload(payload))
    }
  }

  const deleteSupplier = async (id: number) => {
    const supplierRef = ref(rtdb, `suppliers/${id}`)
    await remove(supplierRef)
    const mfgRef = ref(rtdb, `manufacturers/${id}`)
    await remove(mfgRef)
  }

  const saveBrand = async (brand: Brand) => {
    const brandRef = ref(rtdb, `brands/${brand.id}`)
    await set(brandRef, sanitizePayload(brand))
  }

  const deleteBrand = async (id: number) => {
    const brandRef = ref(rtdb, `brands/${id}`)
    await remove(brandRef)
  }

  const saveTransporter = async (transporter: Transporter) => {
    const transporterRef = ref(rtdb, `transporters/${transporter.id}`)
    await set(transporterRef, sanitizePayload(transporter))
  }

  const deleteTransporter = async (id: number) => {
    const transporterRef = ref(rtdb, `transporters/${id}`)
    await remove(transporterRef)
  }

  const saveMarket = async (market: Market) => {
    const marketRef = ref(rtdb, `markets/${market.id}`)
    await set(marketRef, sanitizePayload(market))
  }

  const deleteMarket = async (id: number) => {
    const marketRef = ref(rtdb, `markets/${id}`)
    await remove(marketRef)
  }

  const saveEmployee = async (employee: Employee) => {
    const empRef = ref(rtdb, `employees/${employee.id}`)
    await set(empRef, sanitizePayload(employee))
  }

  const suspendEmployee = async (id: number, reason?: string) => {
    const empRef = ref(rtdb, `employees/${id}`)
    await update(empRef, {
      isBlocked: true,
      status: "Suspended",
      blockedAt: Date.now(),
      blockedReason: reason || "Access suspended by Administrator",
    })
  }

  const resumeEmployee = async (id: number) => {
    const empRef = ref(rtdb, `employees/${id}`)
    await update(empRef, {
      isBlocked: false,
      isDeleted: false,
      status: "Active",
      blockedAt: null,
      blockedReason: null,
      reactivatedAt: Date.now(),
      deletionStatus: null,
      deletionReason: null,
      deletedAt: null,
      deletedBy: null,
      deletedByEmail: null,
      deletedByRole: null,
    })
  }

  const deactivateEmployee = async (id: number, reason?: string) => {
    const empRef = ref(rtdb, `employees/${id}`)
    await update(empRef, {
      isBlocked: true,
      isDeleted: true,
      status: "Deactivated",
      deletedAt: Date.now(),
      deletedBy: "Admin",
      deletedByRole: "Owner",
      deletionStatus: "CONFIRMED",
      deletionReason: reason || "Staff account deactivated by Administrator",
    })
  }

  const deleteEmployee = async (id: number) => {
    // Soft deactivation preserves 100% of historical trips, customer visits, and sales records
    await deactivateEmployee(id, "Account deactivated by Administrator")
  }

  const savePurchaseEntry = async (entry: PurchaseEntry) => {
    const entryRef = ref(rtdb, `purchase_entries/${entry.id}`)
    await set(entryRef, sanitizePayload(entry))
  }

  const deletePurchaseEntry = async (id: number) => {
    const entryRef = ref(rtdb, `purchase_entries/${id}`)
    await remove(entryRef)
  }

  // Create Mixed Case Pack Group
  const createPackGroup = async (
    visitId: number,
    selectedEntries: PurchaseEntry[],
    targetCaseSize: number,
    customNote?: string
  ) => {
    if (selectedEntries.length === 0) return
    const totalLoose = selectedEntries.reduce(
      (sum, e) => sum + (Number(e.loosePieces) || 0),
      0
    )
    const resultingCases = targetCaseSize > 0 ? Math.floor(totalLoose / targetCaseSize) : 1
    const remainingLoose = targetCaseSize > 0 ? totalLoose % targetCaseSize : 0
    const groupId = Date.now()
    const packGroupCode = `MIX-${groupId % 10000}`
    const linkedEntryIds = selectedEntries.map((e) => e.id).join(",")

    const summaryNote =
      customNote ||
      `Mixed Case: ` +
        selectedEntries
          .map((e) => `${e.loosePieces} pcs ${e.itemCode} (${e.supplierName})`)
          .join(" + ") +
        (remainingLoose > 0 ? ` [${remainingLoose} loose pcs remaining]` : "")

    const newGroup: PackGroup = {
      id: groupId,
      visitId: visitId || 1,
      packCode: packGroupCode,
      packGroupCode: packGroupCode,
      combinedPieces: totalLoose,
      totalPieces: totalLoose,
      resultingCases: Math.max(1, resultingCases),
      totalCases: Math.max(1, resultingCases),
      remainingLoose,
      linkedEntryIds,
      note: summaryNote,
      createdAt: Date.now(),
    }

    // 1. Save pack group to RTDB
    const groupRef = ref(rtdb, `pack_groups/${groupId}`)
    await set(groupRef, sanitizePayload(newGroup))

    // 2. Update each purchase entry with packGroupId and mixedPackNote
    for (const entry of selectedEntries) {
      const others = selectedEntries.filter((e) => e.id !== entry.id)
      const note =
        others.length === 0
          ? `Packed in ${packGroupCode}: ${entry.loosePieces} pcs`
          : `Mixed Packing: ${entry.loosePieces} pcs packed with ${others
              .map((o) => `${o.loosePieces} pcs ${o.itemCode} (${o.supplierName})`)
              .join(", ")}`

      const entryRef = ref(rtdb, `purchase_entries/${entry.id}`)
      await update(entryRef, sanitizePayload({
        packGroupId: groupId,
        mixedPackNote: note,
      }))
    }
  }

  // Delete Pack Group & Unpack Entries
  const deletePackGroup = async (groupId: number) => {
    const group = packGroups.find((g) => g.id === groupId)
    if (group && group.linkedEntryIds) {
      const entryIds = group.linkedEntryIds
        .split(",")
        .map((id) => Number(id.trim()))
        .filter(Boolean)
      for (const id of entryIds) {
        const entryRef = ref(rtdb, `purchase_entries/${id}`)
        await update(entryRef, {
          packGroupId: null,
          mixedPackNote: null,
        })
      }
    }
    const groupRef = ref(rtdb, `pack_groups/${groupId}`)
    await remove(groupRef)
  }

  const saveProduct = async (product: Product) => {
    const productRef = ref(rtdb, `products/${product.id}`)
    await set(productRef, sanitizePayload(product))
  }

  const deleteProduct = async (id: number) => {
    const productRef = ref(rtdb, `products/${id}`)
    await remove(productRef)
  }

  // Admin Permanent Delete confirmation
  const confirmPermanentDelete = async (collection: string, id: number | string) => {
    // 1. Hard remove from primary collection in RTDB
    const itemRef = ref(rtdb, `${collection}/${id}`)
    await remove(itemRef)

    // 2. Also remove from manufacturer if supplier
    if (collection === "suppliers") {
      const mfgRef = ref(rtdb, `manufacturers/${id}`)
      await remove(mfgRef)
    }

    // 3. Remove from deletion_requests queue
    const requestRef = ref(rtdb, `deletion_requests/${collection}_${id}`)
    await remove(requestRef)
  }

  // Admin Restore / Undo deletion
  const restoreDeletedItem = async (collection: string, id: number | string) => {
    if (collection === "employees") {
      await resumeEmployee(Number(id))
      const requestRef = ref(rtdb, `deletion_requests/${collection}_${id}`)
      await remove(requestRef)
      return
    }

    // 1. Reset isDeleted and audit metadata on the entity
    const itemRef = ref(rtdb, `${collection}/${id}`)
    await update(itemRef, {
      isDeleted: false,
      deletedAt: null,
      deletedBy: null,
      deletedByEmail: null,
      deletedByRole: null,
      deletionStatus: null,
      deletionReason: null,
    })

    // 2. If supplier, also update manufacturer
    if (collection === "suppliers") {
      const mfgRef = ref(rtdb, `manufacturers/${id}`)
      await update(mfgRef, {
        isDeleted: false,
        deletedAt: null,
        deletedBy: null,
        deletedByEmail: null,
        deletedByRole: null,
        deletionStatus: null,
        deletionReason: null,
      })
    }

    // 3. Remove from deletion_requests queue
    const requestRef = ref(rtdb, `deletion_requests/${collection}_${id}`)
    await remove(requestRef)
  }

  // Customer Self-Registration Request Methods
  const submitRegistrationRequest = async (
    request: Omit<CustomerRegistrationRequest, "id" | "createdAt" | "status" | "phoneVerified"> & { verificationUid?: string }
  ): Promise<string> => {
    const id = `req_${Date.now()}_${Math.floor(Math.random() * 1000)}`
    const reqRef = ref(rtdb, `customer_registration_requests/${id}`)
    const payload: CustomerRegistrationRequest = {
      ...request,
      id,
      status: "PENDING",
      phoneVerified: true,
      createdAt: Date.now(),
    }
    await set(reqRef, sanitizePayload(payload))
    return id
  }

  const approveRegistrationRequest = async (
    requestId: string,
    options: {
      assignedAgentId: number | string
      assignedAgentName: string
      creditType?: "Cash" | "Credit"
      creditDays?: number
      creditLimit?: number
    }
  ) => {
    const req = registrationRequests.find((r) => r.id === requestId)
    if (!req) throw new Error("Registration request not found")

    // Find next numeric customer ID
    const maxId = customers.reduce((max, c) => Math.max(max, Number(c.id) || 0), 0)
    const newCustId = maxId + 1

    const newCustomer: Customer = {
      id: newCustId,
      customerId: `CUST-${newCustId}`,
      name: req.name.trim(),
      firmName: req.firmName?.trim() || req.name.trim(),
      phone: req.phone.trim(),
      phone2: req.phone2?.trim() || "",
      email: req.email?.trim() || "",
      address: req.address.trim(),
      shopAddress: req.shopAddress?.trim() || req.address.trim(),
      marketArea: req.marketArea?.trim() || "",
      city: req.city?.trim() || "Ahmedabad",
      district: req.district?.trim() || "",
      state: req.state?.trim() || "Gujarat",
      pincode: req.pincode?.trim() || "",
      shopMapLink: req.shopMapLink?.trim() || "",
      garmentTypes: req.garmentTypes?.trim() || "",
      preferredCategories: req.garmentTypes?.trim() || "",
      gstin: req.gstin?.trim() || "",
      gstNumber: req.gstin?.trim() || "",
      panNumber: req.panNumber?.trim() || "",
      preferredTransporterName: req.preferredTransporterName?.trim() || "",
      transportPreference: req.transportPreference?.trim() || "",
      shopPhotoUri: req.shopPhotoUri || "",
      gstCertPhotoUri: req.gstCertPhotoUri || "",
      panPhotoUri: req.panPhotoUri || "",
      aadharPhotoUri: req.aadharPhotoUri || "",
      notes: [
        req.bankName ? `Bank: ${req.bankName} | A/C: ${req.accountNumber || ""} | IFSC: ${req.ifscCode || ""}` : "",
        req.notes ? `Customer Note: ${req.notes}` : "",
        `Registered via Web Form on ${new Date(req.createdAt).toLocaleDateString()}`
      ].filter(Boolean).join("\n"),
      customerType: options.creditType || "Cash",
      creditDays: Number(options.creditDays) || 0,
      creditLimit: Number(options.creditLimit) || 0,
      addedByAgentId: options.assignedAgentId,
      addedByAgentName: options.assignedAgentName,
      createdAt: Date.now(),
    }

    // Save newly approved customer
    await saveCustomer(newCustomer)

    // Mark registration request as APPROVED
    const reqRef = ref(rtdb, `customer_registration_requests/${requestId}`)
    await update(reqRef, sanitizePayload({
      status: "APPROVED",
      approvedAt: Date.now(),
      approvedBy: "Admin",
      assignedAgentId: options.assignedAgentId,
      assignedAgentName: options.assignedAgentName,
      creditType: options.creditType || "Cash",
      creditDays: Number(options.creditDays) || 0,
      creditLimit: Number(options.creditLimit) || 0,
      createdCustomerId: newCustId,
    }))

    return newCustId
  }

  const rejectRegistrationRequest = async (requestId: string, reason?: string) => {
    const reqRef = ref(rtdb, `customer_registration_requests/${requestId}`)
    await update(reqRef, sanitizePayload({
      status: "REJECTED",
      rejectedAt: Date.now(),
      rejectedBy: "Admin",
      rejectionReason: reason || "Declined by Admin",
    }))
  }

  const deleteRegistrationRequest = async (requestId: string) => {
    const reqRef = ref(rtdb, `customer_registration_requests/${requestId}`)
    await remove(reqRef)
  }

  return (
    <DataContext.Provider
      value={{
        visits,
        entries,
        customers,
        suppliers,
        products,
        brands,
        transporters,
        markets,
        packGroups,
        transactions,
        employees,
        loading,
        deletedItems,
        pendingDeletionsCount,
        confirmPermanentDelete,
        restoreDeletedItem,
        pendingPaymentsCount,
        totalPendingDues,
        pendingDeliveriesCount,
        activeTripsCount,
        looseEntriesCount,
        totalLoosePieces,
        allEmployeeStats,
        getEmployeeStats,
        selectedEmployeeId,
        setSelectedEmployeeId,
        updatePayment,
        updateDelivery,
        saveVisit,
        deleteVisit,
        saveCustomer,
        deleteCustomer,
        saveSupplier,
        deleteSupplier,
        saveBrand,
        deleteBrand,
        saveTransporter,
        deleteTransporter,
        saveMarket,
        deleteMarket,
        saveEmployee,
        deleteEmployee,
        suspendEmployee,
        resumeEmployee,
        deactivateEmployee,
        saveProduct,
        deleteProduct,
        savePurchaseEntry,
        deletePurchaseEntry,
        createPackGroup,
        deletePackGroup,
        registrationRequests,
        pendingRegistrationRequestsCount,
        submitRegistrationRequest,
        approveRegistrationRequest,
        rejectRegistrationRequest,
        deleteRegistrationRequest,
      }}
    >
      {children}
    </DataContext.Provider>
  )
}

export const useData = () => {
  const context = useContext(DataContext)
  if (!context) {
    throw new Error("useData must be used within a DataProvider")
  }
  return context
}
