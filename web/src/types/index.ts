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

export interface CustomerOutlet {
  name: string
  address: string
  city?: string
  pincode?: string
  mapLink?: string
}

export interface CustomerContact {
  name?: string
  phone: string
  designation?: string
  email?: string
}

export interface Customer extends BaseEntity {
  id: number
  customerId?: string
  name: string // Owner Name
  firmName?: string // Shop / Firm Name
  city?: string
  district?: string
  state?: string
  pincode?: string
  marketArea?: string
  markets?: string
  phone: string
  phone2?: string
  phone3?: string
  phone4?: string
  phone5?: string
  phones?: string[]
  contacts?: CustomerContact[]
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
  outlets?: CustomerOutlet[]
  mapLink?: string
  shopMapLink?: string
  garmentTypes?: string // Which type of garments they deal with mostly
  preferredCategories?: string
  customerType?: "Cash" | "Credit" | string
  creditDays?: number
  creditLimit?: number
  outstandingBalance?: number
  balanceType?: string
  balanceDueDate?: string
  isBlocked?: boolean
  referredBy?: string
  addedByAgentId?: number | string
  addedByAgentName?: string
  gstNumber?: string
  gstin?: string
  panNumber?: string
  dob?: string
  religion?: string
  preferredTransporterId?: number | string
  preferredTransporterName?: string
  transportPreference?: string
  aadharPhotoUri?: string
  gstCertPhotoUri?: string
  panPhotoUri?: string
  shopPhotoUri?: string
  purchaserPhotoUri?: string
  cancelChequePhotoUri?: string
  notes?: string
  createdAt?: number
}

export interface SupplierAddress {
  name: string
  address: string
  city?: string
  pincode?: string
  phone?: string
}

export interface Supplier extends BaseEntity {
  id: number
  supplierId?: string
  name: string
  firmName?: string
  marketId?: number | string
  marketArea?: string
  markets?: string
  marketName?: string
  brandId?: number | string
  brand?: string
  city?: string
  district?: string
  state?: string
  pincode?: string
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
  factories?: SupplierAddress[]
  outlets?: SupplierAddress[]
  productsMade?: string // What they make
  priceRange?: string // Range of products in Rupees e.g. "₹250 - ₹1200"
  shopPhotoUri?: string
  visitingCardPhotoUri?: string
  referredBy?: string
  categories?: string
  garmentTypes?: string
  gstin?: string
  gstNumber?: string
  panNumber?: string
  defaultCaseSize?: number
  defaultGstRate?: number
  rating?: number
  notes?: string
  createdAt?: number
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

export interface Brand extends BaseEntity {
  id: number
  brandName: string
  manufacturerId?: number | null
  manufacturerName?: string
  category?: string
  logoPhotoUri?: string
  description?: string
  isActive?: boolean
  createdAt?: number
}

export interface Transporter extends BaseEntity {
  id: number
  transporterName: string
  contactPerson?: string
  phone: string
  phone2?: string
  phone3?: string
  officeAddress?: string
  godownAddress?: string
  city?: string
  destinationsCovered?: string
  gstin?: string
  trackingUrl?: string
  rating?: number
  notes?: string
  createdAt?: number
}

export interface Market extends BaseEntity {
  id: number
  marketName: string
  city: string
  area?: string
  pincode?: string
  landmark?: string
  marketType?: string
  description?: string
  createdAt?: number
}

export interface SoftDeletedItem {
  id: number | string
  collection: "visits" | "purchase_entries" | "customers" | "suppliers" | "products" | "employees" | "brands" | "transporters" | "markets"
  entityType: "Visit" | "Purchase Entry" | "Customer" | "Supplier" | "Product" | "Employee" | "Brand" | "Transporter" | "Market"
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

