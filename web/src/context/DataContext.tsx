import React, { createContext, useContext, useEffect, useState } from "react"
import { ref, onValue, set, remove, update, get } from "firebase/database"
import { rtdb } from "../lib/firebase"
import { useAuth } from "./AuthContext"
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
  CustomerRegistrationRequest,
  SupplierRegistrationRequest,
  Lead
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
          let id = rawItem.id
          if (id === undefined || id === null || (typeof id === "number" && isNaN(id)) || String(id) === "NaN") {
            id = idx
          } else if (typeof id === "string" && !isNaN(Number(id)) && /^\d+$/.test(id.trim())) {
            id = Number(id)
          }
          const isDeleted = Boolean(rawItem.isDeleted ?? rawItem.deleted ?? false)
          const isActive = Boolean(rawItem.isActive ?? rawItem.active ?? true)
          const isBlocked = Boolean(rawItem.isBlocked ?? rawItem.blocked ?? false)
          return {
            ...item,
            id,
            _rtdbKey: String(idx),
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
          let id = rawItem.id
          if (id === undefined || id === null || (typeof id === "number" && isNaN(id)) || String(id) === "NaN") {
            id = !isNaN(numKey) && /^\d+$/.test(key) ? numKey : key
          } else if (typeof id === "string" && !isNaN(Number(id)) && /^\d+$/.test(id.trim())) {
            id = Number(id)
          }
          const isDeleted = Boolean(rawItem.isDeleted ?? rawItem.deleted ?? false)
          const isActive = Boolean(rawItem.isActive ?? rawItem.active ?? true)
          const isBlocked = Boolean(rawItem.isBlocked ?? rawItem.blocked ?? false)
          return {
            ...item,
            id,
            _rtdbKey: key,
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
      religion?: string
      fallbackRequest?: CustomerRegistrationRequest
    }
  ) => Promise<number>
  rejectRegistrationRequest: (requestId: string, reason?: string, fallbackPhone?: string) => Promise<void>
  deleteRegistrationRequest: (requestId: string, fallbackPhone?: string) => Promise<void>

  // Supplier Self-Registration Requests (User Requests)
  supplierRegistrationRequests: SupplierRegistrationRequest[]
  pendingSupplierRegistrationRequestsCount: number
  submitSupplierRegistrationRequest: (
    request: Omit<SupplierRegistrationRequest, "id" | "createdAt" | "status" | "phoneVerified"> & { verificationUid?: string }
  ) => Promise<string>
  approveSupplierRegistrationRequest: (
    requestId: string,
    options?: {
      brand?: string
      marketName?: string
      fallbackRequest?: SupplierRegistrationRequest
    }
  ) => Promise<number>
  rejectSupplierRegistrationRequest: (requestId: string, reason?: string, fallbackPhone?: string) => Promise<void>
  deleteSupplierRegistrationRequest: (requestId: string, fallbackPhone?: string) => Promise<void>

  // Leads (Prospects for Customers and Suppliers)
  leads: Lead[]
  customerLeads: Lead[]
  supplierLeads: Lead[]
  saveLead: (lead: Partial<Lead> & { firmName: string; phone: string; type: "customer" | "supplier" }) => Promise<string | number>
  deleteLead: (leadId: string | number) => Promise<void>
  convertLeadToMaster: (lead: Lead, targetType: "customer" | "supplier") => Promise<void>
}

const DataContext = createContext<DataContextType | undefined>(undefined)

export const DataProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user } = useAuth()
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
  const [rawSupplierRegistrationRequests, setRawSupplierRegistrationRequests] = useState<SupplierRegistrationRequest[]>([])
  const [rawLeads, setRawLeads] = useState<Lead[]>([])
  const [rawDeletionRequests, setRawDeletionRequests] = useState<any[]>([])
  const [loading, setLoading] = useState<boolean>(true)

  // Global employee filter state
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<number | "all">("all")

  // Real-time synchronization
  useEffect(() => {
    if (!user) {
      setLoading(false)
      return
    }

    setLoading(true)
    let activeListeners = 0
    const totalListeners = 15

    const checkLoading = () => {
      activeListeners++
      if (activeListeners >= totalListeners) {
        setLoading(false)
      }
    }

    // 1. Visits
    const visitsRef = ref(rtdb, "visits")
    const unsubVisits = onValue(
      visitsRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawVisits(parseRtdbList<Visit>(snapshot.val()))
        } else {
          setRawVisits([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading visits:", error)
        checkLoading()
      }
    )

    // 2. Purchase Entries
    const entriesRef = ref(rtdb, "purchase_entries")
    const unsubEntries = onValue(
      entriesRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawEntries(parseRtdbList<PurchaseEntry>(snapshot.val()))
        } else {
          setRawEntries([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading purchase_entries:", error)
        checkLoading()
      }
    )

    // 3. Customers
    const customersRef = ref(rtdb, "customers")
    const unsubCustomers = onValue(
      customersRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawCustomers(parseRtdbList<Customer>(snapshot.val()))
        } else {
          setRawCustomers([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading customers:", error)
        checkLoading()
      }
    )

    // 4. Suppliers
    const suppliersRef = ref(rtdb, "suppliers")
    const unsubSuppliers = onValue(
      suppliersRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawSuppliers(parseRtdbList<Supplier>(snapshot.val()))
        } else {
          setRawSuppliers([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading suppliers:", error)
        checkLoading()
      }
    )

    // 5. Pack Groups
    const packGroupsRef = ref(rtdb, "pack_groups")
    const unsubPackGroups = onValue(
      packGroupsRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setPackGroups(parseRtdbList<PackGroup>(snapshot.val()))
        } else {
          setPackGroups([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading pack_groups:", error)
        checkLoading()
      }
    )

    // 6. Transactions
    const txRef = ref(rtdb, "transactions")
    const unsubTx = onValue(
      txRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setTransactions(parseRtdbList<Transaction>(snapshot.val()))
        } else {
          setTransactions([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading transactions:", error)
        checkLoading()
      }
    )

    // 7. Employees
    const empRef = ref(rtdb, "employees")
    const unsubEmp = onValue(
      empRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawEmployees(parseRtdbList<Employee>(snapshot.val()))
        } else {
          setRawEmployees([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading employees:", error)
        checkLoading()
      }
    )

    // 8. Products
    const productsRef = ref(rtdb, "products")
    const unsubProducts = onValue(
      productsRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawProducts(parseRtdbList<Product>(snapshot.val()))
        } else {
          setRawProducts([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading products:", error)
        checkLoading()
      }
    )

    // 9. Brands
    const brandsRef = ref(rtdb, "brands")
    const unsubBrands = onValue(
      brandsRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawBrands(parseRtdbList<Brand>(snapshot.val()))
        } else {
          setRawBrands([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading brands:", error)
        checkLoading()
      }
    )

    // 10. Transporters
    const transportersRef = ref(rtdb, "transporters")
    const unsubTransporters = onValue(
      transportersRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawTransporters(parseRtdbList<Transporter>(snapshot.val()))
        } else {
          setRawTransporters([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading transporters:", error)
        checkLoading()
      }
    )

    // 11. Markets
    const marketsRef = ref(rtdb, "markets")
    const unsubMarkets = onValue(
      marketsRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawMarkets(parseRtdbList<Market>(snapshot.val()))
        } else {
          setRawMarkets([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading markets:", error)
        checkLoading()
      }
    )

    // 12. Customer Registration Requests (User Requests)
    const regRequestsRef = ref(rtdb, "customer_registration_requests")
    const unsubRegRequests = onValue(
      regRequestsRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawRegistrationRequests(parseRtdbList<CustomerRegistrationRequest>(snapshot.val()))
        } else {
          setRawRegistrationRequests([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading customer_registration_requests:", error)
        checkLoading()
      }
    )

    // 13. Leads (Customer & Supplier Prospects)
    const leadsRef = ref(rtdb, "leads")
    const unsubLeads = onValue(
      leadsRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawLeads(parseRtdbList<Lead>(snapshot.val()))
        } else {
          setRawLeads([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading leads:", error)
        checkLoading()
      }
    )

    // 14. Supplier Registration Requests
    const supRegRequestsRef = ref(rtdb, "supplier_registration_requests")
    const unsubSupRegRequests = onValue(
      supRegRequestsRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawSupplierRegistrationRequests(parseRtdbList<SupplierRegistrationRequest>(snapshot.val()))
        } else {
          setRawSupplierRegistrationRequests([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading supplier_registration_requests:", error)
        checkLoading()
      }
    )

    // 15. Deletion Requests (Staff Deletions Queue from Android & Web)
    const delRequestsRef = ref(rtdb, "deletion_requests")
    const unsubDelRequests = onValue(
      delRequestsRef,
      (snapshot) => {
        if (snapshot.exists()) {
          setRawDeletionRequests(parseRtdbList<any>(snapshot.val()))
        } else {
          setRawDeletionRequests([])
        }
        checkLoading()
      },
      (error) => {
        console.error("RTDB error reading deletion_requests:", error)
        checkLoading()
      }
    )

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
      unsubSupRegRequests()
      unsubLeads()
      unsubDelRequests()
    }
  }, [user])

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

  // Supplier registration requests (User Requests)
  const supplierRegistrationRequests = React.useMemo(() => {
    return [...rawSupplierRegistrationRequests].sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0))
  }, [rawSupplierRegistrationRequests])

  const pendingSupplierRegistrationRequestsCount = React.useMemo(() => {
    return supplierRegistrationRequests.filter((r) => r.status === "PENDING").length
  }, [supplierRegistrationRequests])

  // Leads (Prospects for Customers & Suppliers)
  const leads = React.useMemo(() => {
    return rawLeads
      .filter((l) => !l.isDeleted)
      .sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0))
  }, [rawLeads])

  const customerLeads = React.useMemo(() => {
    return leads.filter((l) => l.type === "customer")
  }, [leads])

  const supplierLeads = React.useMemo(() => {
    return leads.filter((l) => l.type === "supplier")
  }, [leads])

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

  // Collect all soft-deleted items across entities and deletion_requests for Admin Deletions view
  const deletedItems: SoftDeletedItem[] = React.useMemo(() => {
    const map = new Map<string, SoftDeletedItem>()

    // 1. Process explicit deletion requests queue from RTDB (from Android mobile and Web)
    rawDeletionRequests.forEach((req) => {
      if (!req || req.status === "CONFIRMED" || req.status === "REJECTED") return
      const rawCollection = (req.collection || "").trim().toLowerCase()
      const itemId = req.itemId ?? req.id
      if (!rawCollection || itemId === undefined || itemId === null) return

      let collection: SoftDeletedItem["collection"] = "visits"
      let entityType: SoftDeletedItem["entityType"] = "Visit"

      if (rawCollection.includes("visit")) {
        collection = "visits"
        entityType = "Visit"
      } else if (rawCollection.includes("entry") || rawCollection.includes("purchase")) {
        collection = "purchase_entries"
        entityType = "Purchase Entry"
      } else if (rawCollection.includes("customer")) {
        collection = "customers"
        entityType = "Customer"
      } else if (rawCollection.includes("supplier")) {
        collection = "suppliers"
        entityType = "Supplier"
      } else if (rawCollection.includes("product")) {
        collection = "products"
        entityType = "Product"
      } else if (rawCollection.includes("employee")) {
        collection = "employees"
        entityType = "Employee"
      } else if (rawCollection.includes("brand")) {
        collection = "brands"
        entityType = "Brand"
      } else if (rawCollection.includes("transporter")) {
        collection = "transporters"
        entityType = "Transporter"
      } else if (rawCollection.includes("market")) {
        collection = "markets"
        entityType = "Market"
      }

      const key = `${collection}_${itemId}`
      let originalData = req.originalData || null
      let title = req.itemSummary || `${entityType} #${itemId}`
      let subtitle = req.deletionReason ? `Reason: ${req.deletionReason}` : undefined

      // Attempt to link to rich entity details if available
      if (collection === "visits") {
        const found = rawVisits.find((v) => String(v.id) === String(itemId))
        if (found) {
          originalData = found
          title = `Visit #${found.visitCode || found.id} - ${found.customerName || "Customer"}`
          subtitle = `Date: ${found.date || "N/A"} • Salesman: ${found.employeeName || req.deletedBy || "N/A"}`
        }
      } else if (collection === "purchase_entries") {
        const found = rawEntries.find((e) => String(e.id) === String(itemId))
        if (found) {
          originalData = found
          title = `Order #${found.orderNo || found.id} - ${found.itemCode}`
          subtitle = `Supplier: ${found.supplierName || "N/A"} • ${found.pieces} pcs • Total: ₹${found.grandTotalWithGst || found.totalAmount}`
        }
      } else if (collection === "customers") {
        const found = rawCustomers.find((c) => String(c.id) === String(itemId))
        if (found) {
          originalData = found
          title = `${found.firmName || found.name} (${found.customerId || found.id})`
          subtitle = `City: ${found.city || "Ahmedabad"} • Phone: ${found.phone}`
        }
      } else if (collection === "suppliers") {
        const found = rawSuppliers.find((s) => String(s.id) === String(itemId))
        if (found) {
          originalData = found
          title = `${found.firmName || found.name} (${found.type || "Supplier"})`
          subtitle = `Market: ${found.marketArea || "N/A"} • Phone: ${found.phone}`
        }
      } else if (collection === "products") {
        const found = rawProducts.find((p) => String(p.id) === String(itemId))
        if (found) {
          originalData = found
          title = `${found.name} (${found.productCode})`
          subtitle = `Supplier: ${found.supplierName} • Rate: ₹${found.defaultRate}`
        }
      } else if (collection === "employees") {
        const found = rawEmployees.find((emp) => String(emp.id) === String(itemId))
        if (found) {
          originalData = found
          title = `${found.name} (${found.employeeId || `EMP-${found.id}`})`
          subtitle = `Role: ${found.role} • Phone: ${found.phone || "N/A"}`
        }
      } else if (collection === "brands") {
        const found = rawBrands.find((b) => String(b.id) === String(itemId))
        if (found) {
          originalData = found
          title = `${found.brandName}`
          subtitle = `Category: ${found.category || "N/A"} • Manufacturer: ${found.manufacturerName || "N/A"}`
        }
      } else if (collection === "transporters") {
        const found = rawTransporters.find((t) => String(t.id) === String(itemId))
        if (found) {
          originalData = found
          title = `${found.transporterName}`
          subtitle = `Phone: ${found.phone} • City: ${found.city || "N/A"}`
        }
      } else if (collection === "markets") {
        const found = rawMarkets.find((m) => String(m.id) === String(itemId))
        if (found) {
          originalData = found
          title = `${found.marketName}`
          subtitle = `City: ${found.city} • Area: ${found.area || "N/A"}`
        }
      }

      map.set(key, {
        id: itemId,
        collection,
        entityType,
        title,
        subtitle,
        deletedAt: req.deletedAt || Date.now(),
        deletedBy: req.deletedBy || "Salesman",
        deletedByEmail: req.deletedByEmail || "",
        deletedByRole: req.deletedByRole || "Salesman",
        deletionStatus: req.status || req.deletionStatus || "PENDING_CONFIRMATION",
        deletionReason: req.deletionReason || "",
        originalData: originalData || req,
      })
    })

    // 2. Scan primary collections for entities marked isDeleted or PENDING_CONFIRMATION
    rawVisits.forEach((v) => {
      if (v && (v.isDeleted || v.deletionStatus === "PENDING_CONFIRMATION")) {
        const key = `visits_${v.id}`
        if (!map.has(key)) {
          map.set(key, {
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
        } else {
          const existing = map.get(key)!
          if (!existing.originalData) existing.originalData = v
        }
      }
    })

    rawEntries.forEach((e) => {
      if (e && (e.isDeleted || e.deletionStatus === "PENDING_CONFIRMATION")) {
        const key = `purchase_entries_${e.id}`
        if (!map.has(key)) {
          map.set(key, {
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
        } else {
          const existing = map.get(key)!
          if (!existing.originalData) existing.originalData = e
        }
      }
    })

    rawCustomers.forEach((c) => {
      if (c && (c.isDeleted || c.deletionStatus === "PENDING_CONFIRMATION")) {
        const key = `customers_${c.id}`
        if (!map.has(key)) {
          map.set(key, {
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
        } else {
          const existing = map.get(key)!
          if (!existing.originalData) existing.originalData = c
        }
      }
    })

    rawSuppliers.forEach((s) => {
      if (s && (s.isDeleted || s.deletionStatus === "PENDING_CONFIRMATION")) {
        const key = `suppliers_${s.id}`
        if (!map.has(key)) {
          map.set(key, {
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
        } else {
          const existing = map.get(key)!
          if (!existing.originalData) existing.originalData = s
        }
      }
    })

    rawProducts.forEach((p) => {
      if (p && (p.isDeleted || p.deletionStatus === "PENDING_CONFIRMATION")) {
        const key = `products_${p.id}`
        if (!map.has(key)) {
          map.set(key, {
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
        } else {
          const existing = map.get(key)!
          if (!existing.originalData) existing.originalData = p
        }
      }
    })

    rawEmployees.forEach((emp) => {
      if (emp && (emp.isDeleted || emp.status === "Deactivated")) {
        const key = `employees_${emp.id}`
        if (!map.has(key)) {
          map.set(key, {
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
        } else {
          const existing = map.get(key)!
          if (!existing.originalData) existing.originalData = emp
        }
      }
    })

    rawBrands.forEach((b) => {
      if (b && (b as any).isDeleted) {
        const key = `brands_${b.id}`
        if (!map.has(key)) {
          map.set(key, {
            id: b.id,
            collection: "brands",
            entityType: "Brand",
            title: `${b.brandName}`,
            subtitle: `Category: ${b.category || "N/A"} • Manufacturer: ${b.manufacturerName || "N/A"}`,
            deletedAt: (b as any).deletedAt || Date.now(),
            deletedBy: (b as any).deletedBy || "Admin",
            deletedByEmail: (b as any).deletedByEmail || "",
            deletedByRole: (b as any).deletedByRole || "Admin",
            deletionStatus: (b as any).deletionStatus || "PENDING_CONFIRMATION",
            deletionReason: (b as any).deletionReason || "",
            originalData: b,
          })
        }
      }
    })

    rawTransporters.forEach((t) => {
      if (t && (t as any).isDeleted) {
        const key = `transporters_${t.id}`
        if (!map.has(key)) {
          map.set(key, {
            id: t.id,
            collection: "transporters",
            entityType: "Transporter",
            title: `${t.transporterName}`,
            subtitle: `Phone: ${t.phone} • City: ${t.city || "N/A"}`,
            deletedAt: (t as any).deletedAt || Date.now(),
            deletedBy: (t as any).deletedBy || "Admin",
            deletedByEmail: (t as any).deletedByEmail || "",
            deletedByRole: (t as any).deletedByRole || "Admin",
            deletionStatus: (t as any).deletionStatus || "PENDING_CONFIRMATION",
            deletionReason: (t as any).deletionReason || "",
            originalData: t,
          })
        }
      }
    })

    rawMarkets.forEach((m) => {
      if (m && (m as any).isDeleted) {
        const key = `markets_${m.id}`
        if (!map.has(key)) {
          map.set(key, {
            id: m.id,
            collection: "markets",
            entityType: "Market",
            title: `${m.marketName}`,
            subtitle: `City: ${m.city} • Area: ${m.area || "N/A"}`,
            deletedAt: (m as any).deletedAt || Date.now(),
            deletedBy: (m as any).deletedBy || "Admin",
            deletedByEmail: (m as any).deletedByEmail || "",
            deletedByRole: (m as any).deletedByRole || "Admin",
            deletionStatus: (m as any).deletionStatus || "PENDING_CONFIRMATION",
            deletionReason: (m as any).deletionReason || "",
            originalData: m,
          })
        }
      }
    })

    return Array.from(map.values()).sort((a, b) => (b.deletedAt || 0) - (a.deletedAt || 0))
  }, [
    rawDeletionRequests,
    rawVisits,
    rawEntries,
    rawCustomers,
    rawSuppliers,
    rawProducts,
    rawEmployees,
    rawBrands,
    rawTransporters,
    rawMarkets,
  ])

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
    await remove(ref(rtdb, `deletion_requests/visits_${id}`))

    // Cascade delete purchase entries for this visit
    const entriesToDelete = rawEntries.filter(e => e.visitId === id)
    for (const entry of entriesToDelete) {
      await remove(ref(rtdb, `purchase_entries/${entry.id}`))
      await remove(ref(rtdb, `deletion_requests/purchase_entries_${entry.id}`))
    }

    // Cascade delete pack groups for this visit
    const packGroupsToDelete = packGroups.filter(p => p.visitId === id)
    for (const pg of packGroupsToDelete) {
      await remove(ref(rtdb, `pack_groups/${pg.id}`))
      await remove(ref(rtdb, `deletion_requests/pack_groups_${pg.id}`))
    }
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

  const deleteCustomer = async (id: number | string) => {
    const strId = String(id)
    const found = rawCustomers.find((c) => String(c.id) === strId || (c as any)._rtdbKey === strId)
    const keys = new Set<string>([strId])
    if (found) {
      keys.add(String(found.id))
      if ((found as any)._rtdbKey) keys.add(String((found as any)._rtdbKey))
    }
    for (const k of keys) {
      await remove(ref(rtdb, `customers/${k}`))
      await remove(ref(rtdb, `deletion_requests/customers_${k}`))
    }
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

  const deleteSupplier = async (id: number | string) => {
    const strId = String(id)
    const found = rawSuppliers.find((s) => String(s.id) === strId || (s as any)._rtdbKey === strId)
    const keys = new Set<string>([strId])
    if (found) {
      keys.add(String(found.id))
      if ((found as any)._rtdbKey) keys.add(String((found as any)._rtdbKey))
    }
    for (const k of keys) {
      await remove(ref(rtdb, `suppliers/${k}`))
      await remove(ref(rtdb, `manufacturers/${k}`))
      await remove(ref(rtdb, `deletion_requests/suppliers_${k}`))
    }
  }

  const saveBrand = async (brand: Brand) => {
    const brandRef = ref(rtdb, `brands/${brand.id}`)
    await set(brandRef, sanitizePayload(brand))
  }

  const deleteBrand = async (id: number | string) => {
    const strId = String(id)
    const found = rawBrands.find((b) => String(b.id) === strId || (b as any)._rtdbKey === strId)
    const keys = new Set<string>([strId])
    if (found) {
      keys.add(String(found.id))
      if ((found as any)._rtdbKey) keys.add(String((found as any)._rtdbKey))
    }
    for (const k of keys) {
      await remove(ref(rtdb, `brands/${k}`))
      await remove(ref(rtdb, `deletion_requests/brands_${k}`))
    }
  }

  const saveTransporter = async (transporter: Transporter) => {
    const transporterRef = ref(rtdb, `transporters/${transporter.id}`)
    await set(transporterRef, sanitizePayload(transporter))
  }

  const deleteTransporter = async (id: number | string) => {
    const strId = String(id)
    const found = rawTransporters.find((t) => String(t.id) === strId || (t as any)._rtdbKey === strId)
    const keys = new Set<string>([strId])
    if (found) {
      keys.add(String(found.id))
      if ((found as any)._rtdbKey) keys.add(String((found as any)._rtdbKey))
    }
    for (const k of keys) {
      await remove(ref(rtdb, `transporters/${k}`))
      await remove(ref(rtdb, `deletion_requests/transporters_${k}`))
    }
  }

  const saveMarket = async (market: Market) => {
    const marketRef = ref(rtdb, `markets/${market.id}`)
    await set(marketRef, sanitizePayload(market))
  }

  const deleteMarket = async (id: number | string) => {
    const strId = String(id)
    const found = rawMarkets.find((m) => String(m.id) === strId || (m as any)._rtdbKey === strId)
    const keys = new Set<string>([strId])
    if (found) {
      keys.add(String(found.id))
      if ((found as any)._rtdbKey) keys.add(String((found as any)._rtdbKey))
    }
    for (const k of keys) {
      await remove(ref(rtdb, `markets/${k}`))
      await remove(ref(rtdb, `deletion_requests/markets_${k}`))
    }
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
    await remove(ref(rtdb, `deletion_requests/purchase_entries_${id}`))
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
    await remove(ref(rtdb, `deletion_requests/products_${id}`))
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

    // 3. If visit, cascade hard remove to linked purchase entries
    if (collection === "visits") {
      const linkedEntries = rawEntries.filter((e) => String(e.visitId) === String(id))
      for (const entry of linkedEntries) {
        await remove(ref(rtdb, `purchase_entries/${entry.id}`))
        await remove(ref(rtdb, `deletion_requests/purchase_entries_${entry.id}`))
      }
    }

    // 4. Remove from deletion_requests queue
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

    // 3. If visit, also restore any linked purchase entries that were soft deleted
    if (collection === "visits") {
      const linkedEntries = rawEntries.filter((e) => String(e.visitId) === String(id) && e.isDeleted)
      for (const entry of linkedEntries) {
        await update(ref(rtdb, `purchase_entries/${entry.id}`), {
          isDeleted: false,
          deletedAt: null,
          deletedBy: null,
          deletedByEmail: null,
          deletedByRole: null,
          deletionStatus: null,
          deletionReason: null,
        })
        await remove(ref(rtdb, `deletion_requests/purchase_entries_${entry.id}`))
      }
    }

    // 4. Remove from deletion_requests queue
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
      religion?: string
      fallbackRequest?: CustomerRegistrationRequest
    }
  ) => {
    let targetId = requestId
    let req = registrationRequests.find(
      (r) =>
        r &&
        String(r.id) !== "NaN" &&
        (String(r.id) === String(requestId) || r.id === requestId)
    )

    // Fallback 1: options.fallbackRequest
    if (!req && options.fallbackRequest) {
      req = options.fallbackRequest
      if (req.id && String(req.id) !== "NaN") {
        targetId = String(req.id)
      }
    }

    // Fallback 2: Direct lookup by requestId in RTDB
    if ((!req || !targetId || String(targetId) === "NaN") && requestId && String(requestId) !== "NaN") {
      try {
        const snap = await get(ref(rtdb, `customer_registration_requests/${requestId}`))
        if (snap.exists()) {
          const val = snap.val()
          req = { ...val, id: val.id || requestId }
          targetId = requestId
        }
      } catch (e) {
        console.warn("Could not fetch request directly by ID:", e)
      }
    }

    // Fallback 3: Search across all RTDB customer_registration_requests node
    if (!targetId || String(targetId) === "NaN" || !req) {
      try {
        const allSnap = await get(ref(rtdb, "customer_registration_requests"))
        if (allSnap.exists()) {
          const allVal = allSnap.val() || {}
          for (const [k, v] of Object.entries<any>(allVal)) {
            if (!v) continue
            const idMatch = String(k) === String(requestId) || String(v.id) === String(requestId)
            const phoneMatch = req?.phone && (v.phone === req.phone || v.phone?.slice(-10) === req.phone?.slice(-10))
            if (idMatch || phoneMatch) {
              req = { ...v, id: k }
              targetId = k
              break
            }
          }
        }
      } catch (e) {
        console.warn("Could not scan registration requests in RTDB:", e)
      }
    }

    if (!req) throw new Error("Registration request not found")

    // Find next numeric customer ID
    const maxId = customers.reduce((max, c) => Math.max(max, Number(c.id) || 0), 0)
    const newCustId = maxId + 1

    const assignedReligion = (options.religion !== undefined ? options.religion.trim() : (req.religion?.trim() || ""))

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
      religion: assignedReligion,
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
    const finalReqKey = targetId && String(targetId) !== "NaN" ? targetId : req.id
    if (finalReqKey && String(finalReqKey) !== "NaN") {
      const reqRef = ref(rtdb, `customer_registration_requests/${finalReqKey}`)
      await update(reqRef, sanitizePayload({
        status: "APPROVED",
        approvedAt: Date.now(),
        approvedBy: "Admin",
        assignedAgentId: options.assignedAgentId,
        assignedAgentName: options.assignedAgentName,
        creditType: options.creditType || "Cash",
        creditDays: Number(options.creditDays) || 0,
        creditLimit: Number(options.creditLimit) || 0,
        religion: assignedReligion,
        createdCustomerId: newCustId,
      }))
    }

    return newCustId
  }

  const rejectRegistrationRequest = async (requestId: string, reason?: string, fallbackPhone?: string) => {
    let targetId = requestId
    if (!targetId || String(targetId) === "NaN") {
      try {
        const allSnap = await get(ref(rtdb, "customer_registration_requests"))
        if (allSnap.exists()) {
          const allVal = allSnap.val() || {}
          for (const [k, v] of Object.entries<any>(allVal)) {
            if (String(k) === String(requestId) || (fallbackPhone && (v?.phone === fallbackPhone || v?.phone?.slice(-10) === fallbackPhone.slice(-10)))) {
              targetId = k
              break
            }
          }
        }
      } catch (e) {
        console.warn("Could not resolve reject request ID:", e)
      }
    }
    const reqRef = ref(rtdb, `customer_registration_requests/${targetId}`)
    await update(reqRef, sanitizePayload({
      status: "REJECTED",
      rejectedAt: Date.now(),
      rejectedBy: "Admin",
      rejectionReason: reason || "Declined by Admin",
    }))
  }

  const deleteRegistrationRequest = async (requestId: string, fallbackPhone?: string) => {
    let targetId = requestId
    if (!targetId || String(targetId) === "NaN") {
      try {
        const allSnap = await get(ref(rtdb, "customer_registration_requests"))
        if (allSnap.exists()) {
          const allVal = allSnap.val() || {}
          for (const [k, v] of Object.entries<any>(allVal)) {
            if (String(k) === String(requestId) || (fallbackPhone && (v?.phone === fallbackPhone || v?.phone?.slice(-10) === fallbackPhone.slice(-10)))) {
              targetId = k
              break
            }
          }
        }
      } catch (e) {
        console.warn("Could not resolve delete request ID:", e)
      }
    }
    const reqRef = ref(rtdb, `customer_registration_requests/${targetId}`)
    await remove(reqRef)
  }

  // Supplier Self-Registration Request Methods
  const submitSupplierRegistrationRequest = async (
    request: Omit<SupplierRegistrationRequest, "id" | "createdAt" | "status" | "phoneVerified"> & { verificationUid?: string }
  ): Promise<string> => {
    const id = `sup_req_${Date.now()}_${Math.floor(Math.random() * 1000)}`
    const reqRef = ref(rtdb, `supplier_registration_requests/${id}`)
    const payload: SupplierRegistrationRequest = {
      ...request,
      id,
      status: "PENDING",
      phoneVerified: true,
      createdAt: Date.now(),
    }
    await set(reqRef, sanitizePayload(payload))
    return id
  }

  const approveSupplierRegistrationRequest = async (
    requestId: string,
    options?: {
      brand?: string
      marketName?: string
      fallbackRequest?: SupplierRegistrationRequest
    }
  ) => {
    let targetId = requestId
    let req = supplierRegistrationRequests.find(
      (r) =>
        r &&
        String(r.id) !== "NaN" &&
        (String(r.id) === String(requestId) || r.id === requestId)
    )

    if (!req && options?.fallbackRequest) {
      req = options.fallbackRequest
      if (req.id && String(req.id) !== "NaN") {
        targetId = String(req.id)
      }
    }

    if ((!req || !targetId || String(targetId) === "NaN") && requestId && String(requestId) !== "NaN") {
      try {
        const snap = await get(ref(rtdb, `supplier_registration_requests/${requestId}`))
        if (snap.exists()) {
          const val = snap.val()
          req = { ...val, id: val.id || requestId }
          targetId = requestId
        }
      } catch (e) {
        console.warn("Could not fetch supplier request directly by ID:", e)
      }
    }

    if (!targetId || String(targetId) === "NaN" || !req) {
      try {
        const allSnap = await get(ref(rtdb, "supplier_registration_requests"))
        if (allSnap.exists()) {
          const allVal = allSnap.val() || {}
          for (const [k, v] of Object.entries<any>(allVal)) {
            if (!v) continue
            const idMatch = String(k) === String(requestId) || String(v.id) === String(requestId)
            const phoneMatch = req?.phone && (v.phone === req.phone || v.phone?.slice(-10) === req.phone?.slice(-10))
            if (idMatch || phoneMatch) {
              req = { ...v, id: k }
              targetId = k
              break
            }
          }
        }
      } catch (e) {
        console.warn("Could not scan supplier registration requests in RTDB:", e)
      }
    }

    if (!req) throw new Error("Supplier registration request not found")

    // Next numeric supplier ID
    const maxId = suppliers.reduce((max, s) => Math.max(max, Number(s.id) || 0), 0)
    const newSupId = maxId + 1

    const finalFirmName = req.firmName?.trim() || req.name.trim()
    const finalContactPerson = req.contactPerson?.trim() || req.name.trim()

    const newSupplier: Supplier = {
      id: newSupId,
      supplierId: `SUP-${newSupId}`,
      name: finalFirmName,
      firmName: finalFirmName,
      type: req.type || "Manufacturer",
      brand: options?.brand?.trim() || req.brand?.trim() || "",
      contactPerson: finalContactPerson,
      phone: req.phone.trim(),
      phone2: req.phone2?.trim() || "",
      phones: [req.phone.trim(), req.phone2?.trim()].filter(Boolean) as string[],
      email: req.email?.trim() || "",
      address: req.address?.trim() || "",
      officeAddress: req.officeAddress?.trim() || "",
      marketArea: options?.marketName?.trim() || req.marketArea?.trim() || "",
      marketName: options?.marketName?.trim() || req.marketArea?.trim() || "",
      city: req.city?.trim() || "Ahmedabad",
      state: req.state?.trim() || "Gujarat",
      productsMade: req.productsMade?.trim() || "",
      categories: req.categories?.trim() || req.productsMade?.trim() || "",
      garmentTypes: req.categories?.trim() || req.productsMade?.trim() || "",
      priceRange: req.priceRange?.trim() || "",
      gstin: req.gstin?.trim() || "",
      gstNumber: req.gstin?.trim() || "",
      panNumber: req.panNumber?.trim() || "",
      visitingCardPhotoUri: req.visitingCardPhotoUri || "",
      shopPhotoUri: req.shopPhotoUri || "",
      notes: [
        req.bankName ? `Bank: ${req.bankName} | A/C: ${req.accountNumber || ""} | IFSC: ${req.ifscCode || ""}` : "",
        req.notes ? `Supplier Note: ${req.notes}` : "",
        `Registered via Web Form on ${new Date(req.createdAt).toLocaleDateString()}`
      ].filter(Boolean).join("\n"),
      createdAt: Date.now(),
    }

    await saveSupplier(newSupplier)

    const finalReqKey = targetId && String(targetId) !== "NaN" ? targetId : req.id
    if (finalReqKey && String(finalReqKey) !== "NaN") {
      const reqRef = ref(rtdb, `supplier_registration_requests/${finalReqKey}`)
      await update(reqRef, sanitizePayload({
        status: "APPROVED",
        approvedAt: Date.now(),
        approvedBy: "Admin",
        createdSupplierId: newSupId,
      }))
    }

    return newSupId
  }

  const rejectSupplierRegistrationRequest = async (requestId: string, reason?: string, fallbackPhone?: string) => {
    let targetId = requestId
    if (!targetId || String(targetId) === "NaN") {
      try {
        const allSnap = await get(ref(rtdb, "supplier_registration_requests"))
        if (allSnap.exists()) {
          const allVal = allSnap.val() || {}
          for (const [k, v] of Object.entries<any>(allVal)) {
            if (String(k) === String(requestId) || (fallbackPhone && (v?.phone === fallbackPhone || v?.phone?.slice(-10) === fallbackPhone.slice(-10)))) {
              targetId = k
              break
            }
          }
        }
      } catch (e) {
        console.warn("Could not resolve reject supplier request ID:", e)
      }
    }
    const reqRef = ref(rtdb, `supplier_registration_requests/${targetId}`)
    await update(reqRef, sanitizePayload({
      status: "REJECTED",
      rejectedAt: Date.now(),
      rejectedBy: "Admin",
      rejectionReason: reason || "Declined by Admin",
    }))
  }

  const deleteSupplierRegistrationRequest = async (requestId: string, fallbackPhone?: string) => {
    let targetId = requestId
    if (!targetId || String(targetId) === "NaN") {
      try {
        const allSnap = await get(ref(rtdb, "supplier_registration_requests"))
        if (allSnap.exists()) {
          const allVal = allSnap.val() || {}
          for (const [k, v] of Object.entries<any>(allVal)) {
            if (String(k) === String(requestId) || (fallbackPhone && (v?.phone === fallbackPhone || v?.phone?.slice(-10) === fallbackPhone.slice(-10)))) {
              targetId = k
              break
            }
          }
        }
      } catch (e) {
        console.warn("Could not resolve delete supplier request ID:", e)
      }
    }
    const reqRef = ref(rtdb, `supplier_registration_requests/${targetId}`)
    await remove(reqRef)
  }

  // Leads (Customer & Supplier Prospects)
  const saveLead = async (lead: Partial<Lead> & { firmName: string; phone: string; type: "customer" | "supplier" }): Promise<string | number> => {
    const leadId: string | number = lead.id !== undefined && lead.id !== null && String(lead.id) !== "NaN" ? lead.id : `lead_${Date.now()}`
    const payload: Lead = {
      id: leadId,
      leadId: lead.leadId || `LEAD-${Date.now().toString().slice(-4)}`,
      type: lead.type || "customer",
      name: lead.name?.trim() || "",
      firmName: lead.firmName.trim(),
      supplierType: lead.supplierType,
      phone: lead.phone.trim(),
      phone2: lead.phone2?.trim() || "",
      meetingPlace: lead.meetingPlace?.trim() || "",
      city: lead.city?.trim() || "Ahmedabad",
      state: lead.state?.trim() || "Gujarat",
      notes: lead.notes?.trim() || "",
      photos: lead.photos || [],
      status: lead.status || "Thinking",
      nextFollowUpDate: lead.nextFollowUpDate || "",
      createdAt: lead.createdAt || Date.now(),
      createdByUid: lead.createdByUid || "",
      createdByName: lead.createdByName || "Admin",
      isDeleted: false,
    }
    await set(ref(rtdb, `leads/${leadId}`), sanitizePayload(payload))
    return leadId
  }

  const deleteLead = async (leadId: string | number) => {
    const targetRef = ref(rtdb, `leads/${leadId}`)
    await update(targetRef, { isDeleted: true, deletedAt: Date.now() })
  }

  const convertLeadToMaster = async (lead: Lead, targetType: "customer" | "supplier") => {
    if (targetType === "customer") {
      const maxId = customers.reduce((max, c) => Math.max(max, Number(c.id) || 0), 0)
      const newCustId = maxId + 1
      const newCust: Customer = {
        id: newCustId,
        customerId: `CUST-${newCustId}`,
        name: lead.name?.trim() || lead.firmName.trim(),
        firmName: lead.firmName.trim(),
        phone: lead.phone.trim(),
        phone2: lead.phone2?.trim() || "",
        address: lead.meetingPlace?.trim() || lead.city || "Ahmedabad",
        city: lead.city || "Ahmedabad",
        state: lead.state || "Gujarat",
        notes: `Converted from Lead on ${new Date().toLocaleDateString()}.\nMet at: ${lead.meetingPlace || ""}\n${lead.notes || ""}`,
        customerType: "Credit",
        creditDays: 30,
        creditLimit: 0,
        shopPhotoUri: lead.photos && lead.photos.length > 0 ? lead.photos[0] : "",
        createdAt: Date.now(),
      }
      await saveCustomer(newCust)
      await update(ref(rtdb, `leads/${lead.id}`), {
        status: "Converted",
        convertedAt: Date.now(),
        convertedTargetId: newCustId,
      })
    } else {
      const maxId = suppliers.reduce((max, s) => Math.max(max, Number(s.id) || 0), 0)
      const newSupId = maxId + 1
      const newSup: Supplier = {
        id: newSupId,
        supplierId: `SUP-${newSupId}`,
        name: lead.name?.trim() || lead.firmName.trim(),
        firmName: lead.firmName.trim(),
        type: lead.supplierType || "Manufacturer",
        phone: lead.phone.trim(),
        contactPerson: lead.name?.trim() || "In-charge",
        address: lead.meetingPlace?.trim() || lead.city || "Ahmedabad",
        city: lead.city || "Ahmedabad",
        marketArea: lead.meetingPlace || "Ahmedabad Market",
        notes: `Converted from Lead on ${new Date().toLocaleDateString()}.\nMet at: ${lead.meetingPlace || ""}\n${lead.notes || ""}`,
        shopPhotoUri: lead.photos && lead.photos.length > 0 ? lead.photos[0] : "",
        createdAt: Date.now(),
      }
      await saveSupplier(newSup)
      await update(ref(rtdb, `leads/${lead.id}`), {
        status: "Converted",
        convertedAt: Date.now(),
        convertedTargetId: newSupId,
      })
    }
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
        supplierRegistrationRequests,
        pendingSupplierRegistrationRequestsCount,
        submitSupplierRegistrationRequest,
        approveSupplierRegistrationRequest,
        rejectSupplierRegistrationRequest,
        deleteSupplierRegistrationRequest,
        leads,
        customerLeads,
        supplierLeads,
        saveLead,
        deleteLead,
        convertLeadToMaster,
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
