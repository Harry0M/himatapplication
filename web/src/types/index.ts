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
  secondaryEmployeeId?: number
  secondaryEmployeeName?: string
  /** Everybody who joined the trip besides the starter: "3,7" (same order as memberNames) */
  memberIds?: string
  /** "Ravi, Suresh" */
  memberNames?: string
  /** How the order was placed: "Market" (customer came along) or "Phone". Blank = Market. */
  tripType?: string
  status: string // "Active", "Completed", "Cancelled"
  closedAt?: number
  closedBy?: string
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
  salesmanId?: number
  salesmanName?: string
  /** Who typed the order in (the salesman can be somebody else) */
  createdById?: number
  createdByName?: string
  orderDate?: string
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
  /** Android field names (same record): GST %, expected delivery, order form / supplier bill photos */
  gstRate?: number
  expectedDeliveryDate?: string
  orderFormPhotoUri?: string | null
  supplierInvoiceUri?: string | null
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
  /** Display string, e.g. "Customer: Balaji Sarees" */
  referredBy?: string
  /** Staff / Agent / Customer / Supplier / Broker */
  referredByType?: string
  referredById?: number
  /** Staff member who added / owns this customer */
  addedByAgentId?: number | string
  addedByAgentName?: string
  /** Sub Agent (employees node, role "Agent") who brought this customer */
  subAgentId?: number
  subAgentName?: string
  gstNumber?: string
  gstin?: string
  panNumber?: string
  dob?: string
  workingMarkets?: string
  religion?: string
  brandName?: string
  contactPerson?: string
  transportName?: string
  preferredTransporter?: string
  preferredTransporterId?: number | string
  preferredTransporterName?: string
  transportPreference?: string
  bookingStation?: string
  creditTerms?: string
  aadharPhotoUri?: string
  aadharBackPhotoUri?: string
  gstCertPhotoUri?: string
  panPhotoUri?: string
  shopPhotoUri?: string
  purchaserPhotoUri?: string
  cancelChequePhotoUri?: string
  bankName?: string
  accountNumber?: string
  ifscCode?: string
  notes?: string
  securityCheques?: CustomerSecurityCheque[]
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
  godownPhotoUri?: string
  visitingCardPhotoUri?: string
  referredBy?: string
  referredByType?: string
  referredById?: number
  categories?: string
  subCategories?: string
  garmentTypes?: string
  system?: {
    mrp?: { value?: string; percentage?: string | number }
    less?: { value?: string; percentage?: string | number }
  }
  systemMrpValue?: string
  systemMrpPercent?: string | number
  systemLessValue?: string
  systemLessPercent?: string | number
  gstin?: string
  gstNumber?: string
  panNumber?: string
  gstCertPhotoUri?: string
  panPhotoUri?: string
  idProofPhotoUri?: string
  idProofBackPhotoUri?: string
  aadharPhotoUri?: string
  aadharBackPhotoUri?: string
  cancelChequePhotoUri?: string
  purchaserPhotoUri?: string
  bankName?: string
  accountNumber?: string
  ifscCode?: string
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
  role: string // "Admin", "Salesman" (shown as Staff) or "Agent" (Sub Agent)
  /** Sub Agents: their own shop / firm */
  firmName?: string
  city?: string
  notes?: string
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
  referredByType?: string
  referredById?: number
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
  isActive?: boolean
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
  isActive?: boolean
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

export interface CustomerRegistrationRequest {
  id: string
  primaryKey?: string // GSTIN if available, else clean 10-digit mobile
  keyType?: "GSTIN" | "PHONE"
  firmName: string
  name: string // Owner / Contact Person
  phone: string // Mobile Number (Verified via SMS)
  phone2?: string // WhatsApp Number
  email?: string
  address: string
  shopAddress?: string
  marketArea?: string
  city: string
  district?: string
  state?: string
  pincode?: string
  shopMapLink?: string
  garmentTypes?: string // Preferred Garment Categories
  workingMarkets?: string
  dob?: string
  gstin?: string
  panNumber?: string
  preferredTransporterName?: string
  transportPreference?: string
  bankName?: string
  accountNumber?: string
  ifscCode?: string
  shopPhotoUri?: string
  gstCertPhotoUri?: string
  panPhotoUri?: string
  aadharPhotoUri?: string
  aadharBackPhotoUri?: string
  cancelChequePhotoUri?: string
  purchaserPhotoUri?: string
  notes?: string
  status: "PENDING" | "APPROVED" | "REJECTED"
  phoneVerified: boolean
  verificationUid?: string
  createdAt: number
  approvedAt?: number
  approvedBy?: string
  assignedAgentId?: number | string
  assignedAgentName?: string
  /** Set when the customer opened a Sub Agent's personal registration link (?agent=<id>) */
  subAgentId?: number
  subAgentName?: string
  creditType?: "Cash" | "Credit"
  creditDays?: number
  creditLimit?: number
  religion?: string
  createdCustomerId?: number
  rejectionReason?: string
}

export interface SupplierRegistrationRequest {
  id: string
  primaryKey?: string // GSTIN if available, else clean 10-digit mobile
  keyType?: "GSTIN" | "PHONE"
  firmName: string
  name: string // Mill / Firm Name or Owner Name
  contactPerson: string
  type: "Manufacturer" | "Trading" | "Distributor" | "Fabric" | string
  brand?: string
  phone: string // Primary Phone / Mobile
  phone2?: string // Secondary Phone / WhatsApp
  email?: string
  address?: string // Factory / Office Address (optional)
  officeAddress?: string // Market Office / Shop Address
  homeAddress?: string // Factory / Unit Address
  marketArea?: string
  /** Market master record picked on the form (markets node) */
  marketId?: number
  marketName?: string
  city: string
  district?: string
  state: string
  pincode?: string
  mapLink?: string
  productsMade?: string // e.g. Fabrics, Garments manufactured
  categories?: string // Garment categories: Ladies, Gents, Kids, Handloom
  subCategories?: string // Selected child options
  priceRange?: string
  gstin?: string
  panNumber?: string
  bankName?: string
  accountNumber?: string
  ifscCode?: string
  visitingCardPhotoUri?: string
  shopPhotoUri?: string // Shop Front Photo
  godownPhotoUri?: string // Godown Photo
  gstCertPhotoUri?: string
  panPhotoUri?: string
  idProofPhotoUri?: string
  idProofBackPhotoUri?: string
  aadharPhotoUri?: string
  aadharBackPhotoUri?: string
  cancelChequePhotoUri?: string
  purchaserPhotoUri?: string
  notes?: string
  system?: {
    mrp?: { value?: string; percentage?: string | number }
    less?: { value?: string; percentage?: string | number }
  }
  systemMrpValue?: string
  systemMrpPercent?: string | number
  systemLessValue?: string
  systemLessPercent?: string | number
  status: "PENDING" | "APPROVED" | "REJECTED"
  phoneVerified: boolean
  verificationUid?: string
  createdAt: number
  approvedAt?: number
  approvedBy?: string
  createdSupplierId?: number
  rejectionReason?: string
}

export type LeadType = "customer" | "supplier"
export type LeadStatus = "New" | "Thinking" | "Follow-up" | "Converted" | "Dropped"

export interface Lead {
  id: string | number
  leadId?: string
  type: LeadType // "customer" or "supplier"
  name: string // Contact Person Name
  firmName: string // Shop / Mill Name
  supplierType?: "Manufacturer" | "Wholesaler" // relevant if type === 'supplier'
  phone: string
  phone2?: string
  meetingPlace?: string // Place where we met him (Market, Shop, Hotel, etc.)
  city?: string
  state?: string
  notes?: string
  photos?: string[]
  status?: LeadStatus
  nextFollowUpDate?: string
  createdByUid?: string
  createdByName?: string
  createdAt?: number
  convertedAt?: number
  convertedTargetId?: number | string
  isDeleted?: boolean
}

export type ChequePartyType = "Customer" | "Supplier"
export type ChequeStatus = "Pending" | "Due Today" | "Deposited" | "Cleared" | "Bounced"

export interface ChequePdc extends BaseEntity {
  id: number
  chequeNo: string // CH N
  bankName: string // Bank Name
  amount: number // Cheque Amount
  chequeDate: string // YYYY-MM-DD
  partyType: ChequePartyType
  partyId: number
  partyName: string
  status: ChequeStatus
  depositDate?: string
  clearedDate?: string
  notes?: string
  photoUri?: string
  isSecurityCheque?: boolean
  accountNumber?: string
  branchName?: string
  createdAt?: number
}

export interface CustomerSecurityCheque {
  id: number | string
  chequeNo: string
  bankName: string
  amount: number
  chequeDate: string // YYYY-MM-DD (due/maturity date)
  accountNumber?: string
  branchName?: string
  notes?: string
  status?: ChequeStatus | string
  pdcChequeId?: number
  createdAt?: number
}



