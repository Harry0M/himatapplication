export interface BaseEntity {
  isDeleted?: boolean
  deletedAt?: number
  deletedBy?: string
  deletedByEmail?: string
  deletedByRole?: string
  deletionStatus?: string // "PENDING_CONFIRMATION", "CONFIRMED"
  deletionReason?: string
}

export interface Visit extends BaseEntity {
  id: number
  visitCode: string
  customerId: number
  customerName: string
  date: string
  employeeId: number
  employeeName: string
  status: string // "Active", "Completed", "Cancelled"
  notes?: string
  totalEstimatedAmount?: number
  createdAt?: number
}

export interface PurchaseEntry extends BaseEntity {
  id: number
  visitId: number
  orderNo: string
  supplierId: number
  supplierName: string
  supplierType?: string
  itemCode: string
  pieces: number
  rate?: number
  caseCount: number
  caseSize: number
  loosePieces: number
  pricePerPiece: number
  totalAmount: number
  gstPercent: number
  gstAmount: number
  grandTotalWithGst: number
  deliveryStatus: string // "Pending", "Packed", "Dispatched", "Delivered"
  transporter?: string
  lrNo?: string
  lrDate?: string
  paymentStatus: string // "Unpaid", "Partial", "Paid"
  paymentMode?: string
  paidAmount: number
  paymentRemarks?: string
  packGroupId?: number | null
  mixedPackNote?: string | null
  notes?: string
  billPdfPath?: string
  createdAt?: number
}

export interface Customer extends BaseEntity {
  id: number
  customerId?: string
  name: string
  firmName?: string
  marketArea?: string
  markets?: string
  city?: string
  state?: string
  phone: string
  phone2?: string
  phone3?: string
  phone4?: string
  phone5?: string
  phones?: string[]
  email?: string
  email2?: string
  emails?: string[]
  address?: string
  shopAddress?: string
  homeAddress?: string
  shopLocation?: string
  personalLocation?: string
  shopCount?: number
  shopLocations?: string
  referredBy?: string
  preferredCategories?: string
  gstNumber?: string
  gstin?: string
  creditDays?: number
  creditLimit?: number
  outstandingBalance?: number
  balanceType?: string
  balanceDueDate?: string
  isBlocked?: boolean
  notes?: string
}

export interface Supplier extends BaseEntity {
  id: number
  supplierId?: string
  name: string
  firmName?: string
  brand?: string
  marketArea?: string
  markets?: string
  city?: string
  state?: string
  type: string // "Manufacturer", "Wholesaler", "Trader", etc.
  contactPerson?: string
  phone: string
  phone2?: string
  phone3?: string
  phone4?: string
  phone5?: string
  phones?: string[]
  email?: string
  email2?: string
  emails?: string[]
  address?: string
  officeAddress?: string
  homeAddress?: string
  officeLocation?: string
  personalLocation?: string
  shopCount?: number
  shopLocations?: string
  referredBy?: string
  categories?: string
  garmentTypes?: string
  gstin?: string
  gstNumber?: string
  defaultCaseSize?: number
  defaultGstRate?: number
  rating?: number
  notes?: string
}

export interface PackGroup {
  id: number
  visitId: number
  packCode?: string
  packGroupCode?: string
  totalPieces?: number
  combinedPieces?: number
  totalCases?: number
  resultingCases?: number
  remainingLoose?: number
  linkedEntryIds: string
  note?: string
  createdAt: number
}

export interface Transaction {
  id: number
  visitId: number
  customerId: number
  customerName: string
  type: string
  amount: number
  paymentMode: string
  referenceNo?: string
  date: string
  notes?: string
}

export interface SuperAdmin {
  email: string
  name: string
  role: string
  createdAt: number
}

export interface Employee extends BaseEntity {
  id: number
  employeeId?: string
  name: string
  role: string // "Admin" or "Salesman"
  phone?: string
  phone2?: string
  phone3?: string
  phone4?: string
  phone5?: string
  phones?: string[]
  email?: string
  alternateEmail?: string
  emails?: string[]
  address?: string
  currentAddress?: string
  permanentAddress?: string
  personalLocation?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  referredBy?: string
  assignedMarkets?: string
  markets?: string
  status?: string // "Active", "Suspended", "Deactivated"
  isBlocked?: boolean
  blockedAt?: number | null
  blockedReason?: string
  reactivatedAt?: number | null
}

export interface Product extends BaseEntity {
  id: number
  productCode: string
  name: string
  supplierId: number
  supplierName: string
  category: string
  defaultRate: number
  defaultCaseSize: number
  hsnCode?: string
  description?: string
}

export interface SoftDeletedItem {
  id: number | string
  collection: "visits" | "purchase_entries" | "customers" | "suppliers" | "products" | "employees"
  entityType: "Visit" | "Purchase Entry" | "Customer" | "Supplier" | "Product" | "Employee"
  title: string
  subtitle?: string
  deletedAt: number
  deletedBy: string
  deletedByEmail?: string
  deletedByRole?: string
  deletionStatus?: string
  deletionReason?: string
  originalData: any
}
