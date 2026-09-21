import React, { useState, useEffect } from "react"
import { getMasterDraft, saveMasterDraft, clearMasterDraft } from "../lib/masterDrafts"
import {
  Building2,
  Plus,
  Phone,
  Mail,
  MapPin,
  Search,
  X,
  Store,
  User,
  ExternalLink,
  Edit2,
  Trash2,
  Check,
  CreditCard,
  ShieldCheck,
  Tag,
  Info,
  Printer,
  FileText,
  FileDown,
  Camera,
  Navigation,
  Truck,
  Sparkles,
  Share2,
  Copy,
  CheckCircle2,
  Clock,
  XCircle,
  AlertTriangle,
  MessageSquare,
  Landmark,
  Eye
} from "lucide-react"
import { useData } from "../context/DataContext"
import { useAuth } from "../context/AuthContext"
import { formatInr, formatDate } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { ReferrerSelectModal } from "../components/ui/ReferrerSelectModal"
import { ImageLightboxModal } from "../components/ui/ImageLightboxModal"
import { Tabs } from "../components/ui/Tabs"
import { Customer, CustomerContact, CustomerOutlet, Visit, CustomerRegistrationRequest } from "../types"
import { GARMENT_CATEGORIES } from "../lib/constants"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateCustomerDayReportHtml,
  buildCustomerReportWhatsAppText,
} from "../lib/pdfReports"
import { generateCustomersTallyXml, downloadXmlFile } from "../lib/tallyExport"
import { FileUpload } from "../components/ui/FileUpload"
import { CustomerDetailView } from "./CustomerDetailView"

export function CustomersView() {
  const { user } = useAuth()
  const {
    customers,
    visits,
    entries,
    employees,
    packGroups,
    transporters,
    suppliers,
    saveCustomer,
    deleteCustomer,
    registrationRequests,
    pendingRegistrationRequestsCount,
    approveRegistrationRequest,
    rejectRegistrationRequest,
    deleteRegistrationRequest,
  } = useData()

  // Top-level View Mode: Active Customers Master vs Registration Requests Hub
  const [viewMode, setViewMode] = useState<"customers" | "requests">("customers")
  const [isShareLinkModalOpen, setIsShareLinkModalOpen] = useState<boolean>(false)
  const [copiedLink, setCopiedLink] = useState<boolean>(false)
  const [directSharePhone, setDirectSharePhone] = useState<string>("")
  const [approvedSuccessData, setApprovedSuccessData] = useState<{
    open: boolean
    customerId: number
    req: CustomerRegistrationRequest
    agentName: string
  } | null>(null)

  // Registration Requests Management State
  const [requestFilterStatus, setRequestFilterStatus] = useState<"ALL" | "PENDING" | "APPROVED" | "REJECTED">("PENDING")
  const [selectedRequestForApproval, setSelectedRequestForApproval] = useState<CustomerRegistrationRequest | null>(null)
  const [approvalAssignedAgentId, setApprovalAssignedAgentId] = useState<number | string>("")
  const [approvalAssignedAgentName, setApprovalAssignedAgentName] = useState<string>("")
  const [approvalCreditType, setApprovalCreditType] = useState<"Cash" | "Credit">("Cash")
  const [approvalCreditDays, setApprovalCreditDays] = useState<number>(30)
  const [approvalCreditLimit, setApprovalCreditLimit] = useState<number>(0)
  const [approvalReligion, setApprovalReligion] = useState<string>("")
  const [isApproving, setIsApproving] = useState<boolean>(false)
  const [rejectModal, setRejectModal] = useState<{
    open: boolean
    request: CustomerRegistrationRequest | null
    reason: string
  }>({
    open: false,
    request: null,
    reason: "",
  })
  const [isRejecting, setIsRejecting] = useState<boolean>(false)

  const [selectedRequestForDetails, setSelectedRequestForDetails] = useState<CustomerRegistrationRequest | null>(null)
  const [lightbox, setLightbox] = useState<{ open: boolean; url: string; title: string }>({
    open: false,
    url: "",
    title: "",
  })

  // Synced detail request
  const activeDetailRequest = selectedRequestForDetails
    ? registrationRequests.find((r) => String(r.id) === String(selectedRequestForDetails.id)) || selectedRequestForDetails
    : null

  const [search, setSearch] = useState<string>("")
  const [showSearch, setShowSearch] = useState<boolean>(false)

  // Master Detail Full Page State
  const [selectedCustomerId, setSelectedCustomerId] = useState<number | null>(null)

  // Modal States
  const [isDialogOpen, setIsDialogOpen] = useState<boolean>(false)
  const [activeFormTab, setActiveFormTab] = useState<string>("basic")

  // Report Modal State
  const [reportModal, setReportModal] = useState<{
    open: boolean
    title: string
    html: string
    whatsAppText: string
  }>({
    open: false,
    title: "",
    html: "",
    whatsAppText: "",
  })

  const handleGenerateCustomerReport = (c: Customer) => {
    const custVisits = visits.filter(
      (v) =>
        Number(v.customerId) === c.id ||
        (v.customerName && v.customerName.toLowerCase() === c.name.toLowerCase())
    )
    const custVisitIds = new Set(custVisits.map((v) => v.id))
    const custEntries = entries.filter((e) => custVisitIds.has(e.visitId))

    const visit: Visit = custVisits[0] || {
      id: Date.now(),
      visitCode: `CUST-${c.id}`,
      customerId: c.id,
      customerName: c.name,
      date: new Date().toISOString().split("T")[0],
      employeeId: 1,
      employeeName: "Himat Textile",
      status: "Completed",
    }

    const salesman =
      employees.find((e) => e.id === visit.employeeId || e.name === visit.employeeName) ||
      employees[0]
    const linkedPackGroups = packGroups.filter((pg) => custVisitIds.has(pg.visitId))

    const reportData = {
      visit,
      customer: c,
      salesman,
      entries: custEntries,
      packGroups: linkedPackGroups,
    }

    const html = generateCustomerDayReportHtml(reportData)
    const whatsAppText = buildCustomerReportWhatsAppText(reportData)

    setReportModal({
      open: true,
      title: `Customer Statement & Report: ${c.firmName || c.name}`,
      html,
      whatsAppText,
    })
  }

  // Form State
  const [editingId, setEditingId] = useState<number | null>(null)
  const [customerId, setCustomerId] = useState<string>("")
  const [name, setName] = useState<string>("") // Owner Name
  const [firmName, setFirmName] = useState<string>("") // Shop / Firm Name
  const [gstin, setGstin] = useState<string>("")
  const [panNumber, setPanNumber] = useState<string>("")
  const [city, setCity] = useState<string>("Ahmedabad")
  const [district, setDistrict] = useState<string>("")
  const [state, setState] = useState<string>("Gujarat")
  const [pincode, setPincode] = useState<string>("")
  const [customerType, setCustomerType] = useState<string>("Cash")
  const [creditDays, setCreditDays] = useState<string>("30")
  const [creditLimit, setCreditLimit] = useState<string>("")

  // Dynamic Contacts (up to 5)
  const [contacts, setContacts] = useState<CustomerContact[]>([
    { name: "", phone: "", designation: "Proprietor / Owner", email: "" }
  ])

  // Dynamic Outlets (up to 5)
  const [outlets, setOutlets] = useState<CustomerOutlet[]>([
    { name: "Main Shop / Outlet 1", address: "", pincode: "", mapLink: "" }
  ])

  // Garments & CRM
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])
  const [customCategory, setCustomCategory] = useState<string>("")
  const [referredBy, setReferredBy] = useState<string>("")
  const [addedByAgentName, setAddedByAgentName] = useState<string>("")
  const [preferredTransporterName, setPreferredTransporterName] = useState<string>("")
  const [dob, setDob] = useState<string>("")
  const [religion, setReligion] = useState<string>("")
  const [notes, setNotes] = useState<string>("")

  // KYC & Photos (URLs / Paths)
  const [aadharPhotoUri, setAadharPhotoUri] = useState<string>("")
  const [gstCertPhotoUri, setGstCertPhotoUri] = useState<string>("")
  const [panPhotoUri, setPanPhotoUri] = useState<string>("")
  const [shopPhotoUri, setShopPhotoUri] = useState<string>("")
  const [purchaserPhotoUri, setPurchaserPhotoUri] = useState<string>("")
  const [cancelChequePhotoUri, setCancelChequePhotoUri] = useState<string>("")

  // Draft state (strictly local browser storage)
  const [hasDraft, setHasDraft] = useState<boolean>(false)

  // Open Add Dialog
  const handleOpenAdd = () => {
    setEditingId(null)
    const draft = getMasterDraft<any>("customer")
    if (draft) {
      setCustomerId(draft.customerId || `CUST-${Math.floor(100 + Math.random() * 900)}`)
      setName(draft.name || "")
      setFirmName(draft.firmName || "")
      setGstin(draft.gstin || "")
      setPanNumber(draft.panNumber || "")
      setCity(draft.city || "Ahmedabad")
      setDistrict(draft.district || "")
      setState(draft.state || "Gujarat")
      setPincode(draft.pincode || "")
      setCustomerType(draft.customerType || "Cash")
      setCreditDays(String(draft.creditDays || "30"))
      setCreditLimit(draft.creditLimit ? String(draft.creditLimit) : "")
      setContacts(draft.contacts && draft.contacts.length > 0 ? draft.contacts : [{ name: "", phone: "", designation: "Owner / Purchaser", email: "" }])
      setOutlets(draft.outlets && draft.outlets.length > 0 ? draft.outlets : [{ name: "Main Outlet", address: "", pincode: "", mapLink: "" }])
      setSelectedCategories(draft.selectedCategories || [])
      setCustomCategory("")
      setReferredBy(draft.referredBy || "")
      setAddedByAgentName(user?.displayName || employees[0]?.name || "Himat Staff")
      setPreferredTransporterName(draft.preferredTransporterName || "")
      setDob(draft.dob || "")
      setReligion(draft.religion || "")
      setNotes(draft.notes || "")
      setHasDraft(true)
    } else {
      setCustomerId(`CUST-${Math.floor(100 + Math.random() * 900)}`)
      setName("")
      setFirmName("")
      setGstin("")
      setPanNumber("")
      setCity("Ahmedabad")
      setDistrict("")
      setState("Gujarat")
      setPincode("")
      setCustomerType("Cash")
      setCreditDays("30")
      setCreditLimit("")
      setContacts([{ name: "", phone: "", designation: "Owner / Purchaser", email: "" }])
      setOutlets([{ name: "Main Outlet", address: "", pincode: "", mapLink: "" }])
      setSelectedCategories([])
      setCustomCategory("")
      setReferredBy("")
      setAddedByAgentName(user?.displayName || employees[0]?.name || "Himat Staff")
      setPreferredTransporterName("")
      setDob("")
      setReligion("")
      setNotes("")
      setHasDraft(false)
    }
    setAadharPhotoUri("")
    setGstCertPhotoUri("")
    setPanPhotoUri("")
    setShopPhotoUri("")
    setPurchaserPhotoUri("")
    setCancelChequePhotoUri("")
    setActiveFormTab("basic")
    setIsDialogOpen(true)
  }

  const handleDiscardDraft = () => {
    clearMasterDraft("customer")
    setHasDraft(false)
    setCustomerId(`CUST-${Math.floor(100 + Math.random() * 900)}`)
    setName("")
    setFirmName("")
    setGstin("")
    setPanNumber("")
    setCity("Ahmedabad")
    setDistrict("")
    setState("Gujarat")
    setPincode("")
    setCustomerType("Cash")
    setCreditDays("30")
    setCreditLimit("")
    setContacts([{ name: "", phone: "", designation: "Owner / Purchaser", email: "" }])
    setOutlets([{ name: "Main Outlet", address: "", pincode: "", mapLink: "" }])
    setSelectedCategories([])
    setCustomCategory("")
    setReferredBy("")
    setPreferredTransporterName("")
    setDob("")
    setReligion("")
    setNotes("")
  }

  // Auto-save local draft
  useEffect(() => {
    if (!isDialogOpen || editingId !== null) return
    if (firmName.trim() || name.trim() || gstin.trim() || (contacts[0] && contacts[0].phone?.trim())) {
      saveMasterDraft("customer", {
        customerId,
        name,
        firmName,
        gstin,
        panNumber,
        city,
        district,
        state,
        pincode,
        customerType,
        creditDays,
        creditLimit,
        contacts,
        outlets,
        selectedCategories,
        referredBy,
        preferredTransporterName,
        dob,
        religion,
        notes,
      })
    }
  }, [
    isDialogOpen,
    editingId,
    customerId,
    name,
    firmName,
    gstin,
    panNumber,
    city,
    district,
    state,
    pincode,
    customerType,
    creditDays,
    creditLimit,
    contacts,
    outlets,
    selectedCategories,
    referredBy,
    preferredTransporterName,
    dob,
    religion,
    notes,
  ])

  // Open Edit Dialog
  const handleOpenEdit = (c: Customer) => {
    setEditingId(c.id)
    setCustomerId(c.customerId || `CUST-${c.id}`)
    setName(c.name || "")
    setFirmName(c.firmName || c.name || "")
    setGstin(c.gstin || c.gstNumber || "")
    setPanNumber(c.panNumber || "")
    setCity(c.city || "Ahmedabad")
    setDistrict(c.district || "")
    setState(c.state || "Gujarat")
    setPincode(c.pincode || "")
    setCustomerType(c.customerType || "Cash")
    setCreditDays(String(c.creditDays || 30))
    setCreditLimit(c.creditLimit ? String(c.creditLimit) : "")

    // Initialize contacts from model or existing phones
    if (c.contacts && c.contacts.length > 0) {
      setContacts(c.contacts.slice(0, 5))
    } else {
      const phones = [c.phone, c.phone2, c.phone3, c.phone4, c.phone5].filter(Boolean)
      if (phones.length > 0) {
        setContacts(
          phones.map((p, idx) => ({
            name: idx === 0 ? c.name : "",
            phone: p || "",
            designation: idx === 0 ? "Owner" : `Contact ${idx + 1}`,
            email: idx === 0 ? (c.email || "") : "",
          }))
        )
      } else {
        setContacts([{ name: c.name || "", phone: "", designation: "Owner", email: c.email || "" }])
      }
    }

    // Initialize outlets from model or existing shopAddress
    if (c.outlets && c.outlets.length > 0) {
      setOutlets(c.outlets.slice(0, 5))
    } else {
      setOutlets([
        {
          name: "Main Outlet",
          address: c.shopAddress || c.address || "",
          pincode: c.pincode || "",
          mapLink: c.shopMapLink || c.mapLink || c.shopLocation || "",
        },
      ])
    }

    const cats = (c.garmentTypes || c.preferredCategories || "")
      .split(",")
      .map((cat) => cat.trim())
      .filter(Boolean)
    setSelectedCategories(cats)
    setCustomCategory("")
    setReferredBy(c.referredBy || "")
    setAddedByAgentName(c.addedByAgentName || user?.displayName || employees[0]?.name || "Staff")
    setPreferredTransporterName(c.preferredTransporterName || c.transportPreference || "")
    setDob(c.dob || "")
    setReligion(c.religion || "")
    setNotes(c.notes || "")

    setAadharPhotoUri(c.aadharPhotoUri || "")
    setGstCertPhotoUri(c.gstCertPhotoUri || "")
    setPanPhotoUri(c.panPhotoUri || "")
    setShopPhotoUri(c.shopPhotoUri || "")
    setPurchaserPhotoUri(c.purchaserPhotoUri || "")
    setCancelChequePhotoUri(c.cancelChequePhotoUri || "")

    setActiveFormTab("basic")
    setIsDialogOpen(true)
  }

  // Toggle Category selection
  const handleToggleCategory = (cat: string) => {
    if (selectedCategories.includes(cat)) {
      setSelectedCategories(selectedCategories.filter((c) => c !== cat))
    } else {
      setSelectedCategories([...selectedCategories, cat])
    }
  }

  const handleSave = async () => {
    const finalFirmName = firmName.trim() || name.trim()
    const finalOwnerName = name.trim() || firmName.trim()
    if (!finalFirmName) return

    const id = editingId || Date.now()
    const validContacts = contacts.filter((ct) => ct.phone.trim() || ct.name?.trim())
    const primaryPhone = validContacts[0]?.phone.trim() || ""
    const primaryEmail = validContacts[0]?.email?.trim() || ""

    const allCats = [...selectedCategories]
    if (customCategory.trim() && !allCats.includes(customCategory.trim())) {
      allCats.push(customCategory.trim())
    }

    const newCustomer: Customer = {
      id,
      customerId: customerId.trim() || `CUST-${id % 10000}`,
      name: finalOwnerName,
      firmName: finalFirmName,
      phone: primaryPhone,
      phone2: validContacts[1]?.phone.trim() || "",
      phone3: validContacts[2]?.phone.trim() || "",
      phone4: validContacts[3]?.phone.trim() || "",
      phone5: validContacts[4]?.phone.trim() || "",
      phones: validContacts.map((ct) => ct.phone.trim()).filter(Boolean),
      contacts: validContacts,
      email: primaryEmail,
      email2: validContacts[1]?.email?.trim() || "",
      address: outlets[0]?.address?.trim() || "",
      shopAddress: outlets[0]?.address?.trim() || "",
      shopLocation: outlets[0]?.mapLink?.trim() || "",
      mapLink: outlets[0]?.mapLink?.trim() || "",
      shopMapLink: outlets[0]?.mapLink?.trim() || "",
      outlets: outlets.filter((o) => o?.address?.trim() || o?.name?.trim()),
      shopCount: outlets.filter((o) => o?.address?.trim()).length || 1,
      city: city.trim() || "Ahmedabad",
      district: district.trim(),
      state: state.trim() || "Gujarat",
      pincode: pincode.trim() || (outlets[0]?.pincode?.trim() || ""),
      gstin: gstin.trim().toUpperCase(),
      gstNumber: gstin.trim().toUpperCase(),
      panNumber: panNumber.trim().toUpperCase() || (gstin.length === 15 ? gstin.slice(2, 12) : ""),
      customerType,
      creditDays: parseInt(creditDays, 10) || 30,
      creditLimit: creditLimit ? parseFloat(creditLimit) : 0,
      garmentTypes: allCats.join(", "),
      preferredCategories: allCats.join(", "),
      referredBy: referredBy.trim(),
      addedByAgentName: addedByAgentName.trim(),
      preferredTransporterName: preferredTransporterName.trim(),
      transportPreference: preferredTransporterName.trim(),
      dob: dob.trim(),
      religion: religion.trim(),
      notes: notes.trim(),
      aadharPhotoUri: aadharPhotoUri.trim(),
      gstCertPhotoUri: gstCertPhotoUri.trim(),
      panPhotoUri: panPhotoUri.trim(),
      shopPhotoUri: shopPhotoUri.trim(),
      purchaserPhotoUri: purchaserPhotoUri.trim(),
      cancelChequePhotoUri: cancelChequePhotoUri.trim(),
      createdAt: editingId ? (customers.find((c) => c.id === editingId)?.createdAt || Date.now()) : Date.now(),
    }

    await saveCustomer(newCustomer)
    clearMasterDraft("customer")
    setHasDraft(false)
    setIsDialogOpen(false)
  }

  const handleDelete = async (id: number) => {
    if (window.confirm("Are you sure you want to remove this customer master record?")) {
      await deleteCustomer(id)
      if (selectedCustomerId === id) {
        setSelectedCustomerId(null)
      }
    }
  }

  const handleExportTally = () => {
    const xml = generateCustomersTallyXml(customers)
    const today = new Date().toISOString().slice(0, 10)
    downloadXmlFile(xml, `Himat_Customers_Tally_Import_${today}.xml`)
  }

  // Search filter
  const q = search.trim().toLowerCase()
  const filteredCustomers = customers.filter(
    (c) =>
      !q ||
      c.name?.toLowerCase().includes(q) ||
      c.firmName?.toLowerCase().includes(q) ||
      c.phone?.includes(q) ||
      c.phone2?.includes(q) ||
      c.gstNumber?.toLowerCase().includes(q) ||
      c.gstin?.toLowerCase().includes(q) ||
      c.customerId?.toLowerCase().includes(q) ||
      c.city?.toLowerCase().includes(q) ||
      c.state?.toLowerCase().includes(q) ||
      c.preferredCategories?.toLowerCase().includes(q) ||
      c.garmentTypes?.toLowerCase().includes(q) ||
      c.referredBy?.toLowerCase().includes(q)
  )

  // Filter Registration Requests (User Requests)
  const filteredRequests = registrationRequests.filter((req) => {
    if (requestFilterStatus !== "ALL" && req.status !== requestFilterStatus) {
      return false
    }
    if (!q) return true
    return (
      req.firmName?.toLowerCase().includes(q) ||
      req.name?.toLowerCase().includes(q) ||
      req.phone?.includes(q) ||
      req.city?.toLowerCase().includes(q) ||
      req.gstin?.toLowerCase().includes(q) ||
      String(req.id || "").toLowerCase().includes(q)
    )
  })

  // Action Handlers for Registration Requests
  const handleOpenApprovalDialog = (req: CustomerRegistrationRequest) => {
    setSelectedRequestForApproval(req)
    const activeEmps = employees.filter((e) => !e.isBlocked && !e.isDeleted)
    const firstEmp = activeEmps[0] || employees[0]
    setApprovalAssignedAgentId(firstEmp ? firstEmp.id : "")
    setApprovalAssignedAgentName(firstEmp ? firstEmp.name : "")
    setApprovalCreditType((req.creditType as "Cash" | "Credit") || "Cash")
    setApprovalCreditDays(req.creditDays || 30)
    setApprovalCreditLimit(req.creditLimit || 0)
    setApprovalReligion(req.religion || "")
  }

  const getPublicRegistrationUrl = () => {
    if (typeof window !== "undefined" && (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1")) {
      return "https://himatsms.web.app/#/register-customer"
    }
    return `${window.location.origin}/#/register-customer`
  }

  const buildRegistrationInviteMessage = () => {
    const regUrl = getPublicRegistrationUrl()
    return `नमस्कार!\nहिम्मत टेक्सटाइल (Himat Textile) के साथ नया व्यापारिक खाता खोलने के लिए कृपया नीचे दिए गए लिंक पर अपनी व्यावसायिक जानकारी एवं आवश्यक विवरण भरें:\n\n${regUrl}\n\nधन्यवाद!\nहिम्मत टेक्सटाइल, अहमदाबाद`
  }

  const buildApprovalWelcomeMessage = (
    req: CustomerRegistrationRequest,
    createdId?: number,
    agentName?: string
  ) => {
    const custCode = createdId || req.createdCustomerId || ""
    const salesman = agentName || req.assignedAgentName || "हिम्मत टेक्सटाइल टीम"
    return `प्रिय ${req.name} जी (${req.firmName}),\nबधाई हो! हिम्मत टेक्सटाइल (Himat Textile) में आपका व्यापारिक खाता सफलतापूर्वक स्वीकृत (Approve) कर दिया गया है।\n\n🆔 ग्राहक क्रमांक (Customer ID): #CUST-${custCode}\n🤵 आपके प्रतिनिधि (Sales Agent): ${salesman}\n📦 खाता प्रकार: ${req.creditType || "Cash"} ${req.creditDays ? `(${req.creditDays} दिन)` : ""}\n\nकिसी भी आर्डर या जानकारी के लिए आप अपने प्रतिनिधि या हमारे कार्यालय से संपर्क कर सकते हैं।\n\nहार्दिक शुभकामनाएं!\nहिम्मत टेक्सटाइल, अहमदाबाद`
  }

  const buildRejectionMessage = (req: CustomerRegistrationRequest) => {
    return `प्रिय ${req.name} जी (${req.firmName}),\nहिम्मत टेक्सटाइल (Himat Textile) में आपके पंजीकरण आवेदन के संदर्भ में:\n\nवर्तमान में आपका आवेदन निम्नलिखित कारण से स्वीकृत नहीं हो सका है:\n"${req.rejectionReason || "अपूर्ण विवरण / सत्यापन समस्या"}"\n\nकृपया सही दस्तावेजों एवं विवरण के साथ पुनः आवेदन करें या अधिक जानकारी के लिए हमसे संपर्क करें।\n\nधन्यवाद!\nहिम्मत टेक्सटाइल`
  }

  const handleConfirmApproval = async () => {
    if (!selectedRequestForApproval) return
    setIsApproving(true)
    try {
      const currentReq = selectedRequestForApproval
      const currentAgentName = approvalAssignedAgentName
      const createdId = await approveRegistrationRequest(selectedRequestForApproval.id, {
        assignedAgentId: approvalAssignedAgentId,
        assignedAgentName: approvalAssignedAgentName,
        creditType: approvalCreditType,
        creditDays: approvalCreditDays,
        creditLimit: approvalCreditLimit,
        religion: approvalReligion.trim(),
        fallbackRequest: currentReq,
      })
      setSelectedRequestForApproval(null)
      setApprovedSuccessData({
        open: true,
        customerId: createdId,
        req: currentReq,
        agentName: currentAgentName,
      })
    } catch (err: any) {
      console.error("Approval error:", err)
      alert(err.message || "Failed to approve customer registration request")
    } finally {
      setIsApproving(false)
    }
  }

  const handleOpenRejectDialog = (req: CustomerRegistrationRequest) => {
    setRejectModal({ open: true, request: req, reason: "" })
  }

  const handleConfirmReject = async () => {
    if (!rejectModal.request) return
    setIsRejecting(true)
    try {
      await rejectRegistrationRequest(rejectModal.request.id, rejectModal.reason, rejectModal.request.phone)
      setRejectModal({ open: false, request: null, reason: "" })
    } catch (err: any) {
      console.error("Reject error:", err)
      alert(err.message || "Failed to reject customer registration request")
    } finally {
      setIsRejecting(false)
    }
  }

  const handleDeleteRequest = async (id: string, phone?: string) => {
    if (window.confirm("Are you sure you want to delete this customer registration request?")) {
      await deleteRegistrationRequest(id, phone)
    }
  }

  const handleCopyRegistrationLink = () => {
    const url = getPublicRegistrationUrl()
    navigator.clipboard.writeText(url)
    setCopiedLink(true)
    setTimeout(() => setCopiedLink(false), 3000)
  }

  return (
    <div className="space-y-6">
      {selectedCustomerId !== null ? (
        <CustomerDetailView
          customerId={selectedCustomerId}
          onBack={() => setSelectedCustomerId(null)}
          onEdit={(cust) => handleOpenEdit(cust)}
        />
      ) : (
        <>
          {/* Top Header */}
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
                  Customer Master & CRM Directory
                </h2>
                <Badge variant="outline" className="text-xs bg-indigo-500/10 text-indigo-700 border-indigo-500/20 font-semibold">
                  {customers.length} Retailers
                </Badge>
              </div>
              <p className="text-xs text-muted-foreground mt-0.5">
                Manage buyer shops across India, multi-outlets, KYC documents, credit rules, and Tally ledgers.
              </p>
            </div>

            <div className="flex items-center gap-2 flex-wrap">
              <Button
                variant="outline"
                size="sm"
                onClick={handleExportTally}
                className="h-8 px-3 text-xs gap-1.5 border-emerald-600/30 text-emerald-700 hover:bg-emerald-50 dark:text-emerald-400 font-medium"
                title="Export all customer ledgers to Tally Prime / ERP 9 XML"
              >
                <FileDown className="h-3.5 w-3.5 text-emerald-600" />
                <span>Export to Tally XML</span>
              </Button>

              <Button
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

              <Button
                variant="outline"
                size="sm"
                onClick={() => setIsShareLinkModalOpen(true)}
                className="h-8 px-3 text-xs gap-1.5 border-indigo-500/40 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 font-semibold"
                title="Share customer registration link with prospective retailers"
              >
                <Share2 className="h-3.5 w-3.5 text-indigo-600 dark:text-indigo-400" />
                <span>Share Registration Link</span>
              </Button>

              <Button
                size="sm"
                onClick={handleOpenAdd}
                className="h-8 px-3 text-xs font-semibold shadow-sm bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 gap-1"
              >
                <Plus className="h-3.5 w-3.5" />
                <span>New Customer</span>
              </Button>
            </div>
          </div>

          {/* Navigation Tabs: Customers vs User Requests */}
          <div className="flex items-center gap-2 border-b border-zinc-200 dark:border-zinc-800">
            <button
              type="button"
              onClick={() => setViewMode("customers")}
              className={`flex items-center gap-2 py-2.5 px-3 border-b-2 font-medium text-xs transition-all ${
                viewMode === "customers"
                  ? "border-zinc-900 text-zinc-900 dark:border-zinc-100 dark:text-zinc-100 font-bold"
                  : "border-transparent text-muted-foreground hover:text-zinc-900 dark:hover:text-zinc-100"
              }`}
            >
              <Store className="h-3.5 w-3.5" />
              <span>Active Retailers</span>
              <Badge variant="outline" className="text-[10px] px-1.5 py-0 font-normal">
                {customers.length}
              </Badge>
            </button>

            <button
              type="button"
              onClick={() => setViewMode("requests")}
              className={`flex items-center gap-2 py-2.5 px-3 border-b-2 font-medium text-xs transition-all ${
                viewMode === "requests"
                  ? "border-indigo-600 text-indigo-700 dark:border-indigo-400 dark:text-indigo-300 font-bold"
                  : "border-transparent text-muted-foreground hover:text-indigo-600 dark:hover:text-indigo-400"
              }`}
            >
              <User className="h-3.5 w-3.5" />
              <span>User Requests (पंजीकरण)</span>
              {pendingRegistrationRequestsCount > 0 ? (
                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500 text-white animate-pulse">
                  {pendingRegistrationRequestsCount} Pending
                </span>
              ) : (
                <Badge variant="outline" className="text-[10px] px-1.5 py-0 font-normal">
                  {registrationRequests.length}
                </Badge>
              )}
            </button>
          </div>

          {/* Search Input Bar */}
          {showSearch && (
            <div className="flex items-center gap-2 p-3 bg-zinc-50 dark:bg-zinc-900/50 rounded-xl border border-zinc-200 dark:border-zinc-800">
              <Search className="h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search by shop name, owner, city, GST, garment categories, phone..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="border-none bg-transparent shadow-none focus-visible:ring-0 text-xs h-7"
                autoFocus
              />
              {search && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setSearch("")}
                  className="h-6 w-6 p-0 text-muted-foreground"
                >
                  <X className="h-3.5 w-3.5" />
                </Button>
              )}
            </div>
          )}

          {viewMode === "customers" ? (
            /* Active Customers Master List View */
            filteredCustomers.length === 0 ? (
              <div className="p-12 text-center border border-dashed rounded-2xl bg-white dark:bg-zinc-900/40">
                <Store className="h-10 w-10 text-muted-foreground mx-auto mb-3 opacity-60" />
                <h3 className="font-semibold text-sm">No customer records found</h3>
                <p className="text-xs text-muted-foreground max-w-sm mx-auto mt-1">
                  {search ? "No retailers match your search filters." : "Start registering buyer retail shops."}
                </p>
                <Button onClick={handleOpenAdd} variant="outline" size="sm" className="mt-4 text-xs gap-1.5">
                  <Plus className="h-3.5 w-3.5" />
                  Add First Customer
                </Button>
              </div>
            ) : (
              <div className="overflow-hidden rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-xs">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="border-b border-zinc-200 dark:border-zinc-800 bg-zinc-50/80 dark:bg-zinc-900/80 text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
                        <th className="py-3 px-4">Firm & Proprietor</th>
                        <th className="py-3 px-4">City & Market</th>
                        <th className="py-3 px-4">Contact Phone</th>
                        <th className="py-3 px-4">Terms</th>
                        <th className="py-3 px-4">GST / Outlets</th>
                        <th className="py-3 px-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                      {filteredCustomers.map((cust) => {
                        const hasGst = Boolean(cust.gstin || cust.gstNumber)
                        const outletCount = (cust.outlets && cust.outlets.length) || cust.shopCount || 1
                        const primaryPhone = cust.phone || (cust.contacts && cust.contacts[0]?.phone) || ""

                        return (
                          <tr
                            key={cust.id}
                            onClick={() => setSelectedCustomerId(cust.id)}
                            className="hover:bg-indigo-50/30 dark:hover:bg-indigo-950/20 cursor-pointer transition-colors group"
                          >
                            <td className="py-3 px-4">
                              <div className="flex items-center gap-2.5">
                                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-indigo-500/10 text-indigo-700 font-bold text-xs dark:bg-indigo-500/20 dark:text-indigo-400 group-hover:bg-indigo-600 group-hover:text-white transition-colors">
                                  {(cust.firmName || cust.name || "C")[0].toUpperCase()}
                                </div>
                                <div className="min-w-0">
                                  <p className="font-bold text-sm text-zinc-900 dark:text-zinc-50 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors truncate">
                                    {cust.firmName || cust.name}
                                  </p>
                                  <p className="text-[11px] text-muted-foreground flex items-center gap-1.5 truncate">
                                    <User className="h-3 w-3 shrink-0" />
                                    <span>{cust.name || "Proprietor"}</span>
                                    {cust.customerId && (
                                      <span className="font-mono text-[10px] text-zinc-400 font-semibold">
                                        • {cust.customerId}
                                      </span>
                                    )}
                                  </p>
                                </div>
                              </div>
                            </td>

                            <td className="py-3 px-4">
                              <p className="font-medium text-zinc-800 dark:text-zinc-200">
                                {cust.city || "Ahmedabad"}
                              </p>
                              <p className="text-[11px] text-muted-foreground truncate">
                                {cust.marketArea || cust.address || "Main Bazaar"}
                              </p>
                            </td>

                            <td className="py-3 px-4 whitespace-nowrap">
                              <div className="flex items-center gap-2">
                                <span className="font-mono font-medium text-zinc-800 dark:text-zinc-200">
                                  {primaryPhone || "N/A"}
                                </span>
                                {cust.contacts && cust.contacts.length > 1 && (
                                  <span className="text-[10px] font-semibold bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400 px-1.5 py-0.5 rounded-full">
                                    +{cust.contacts.length - 1}
                                  </span>
                                )}
                                {primaryPhone && (
                                  <a
                                    href={`https://wa.me/91${primaryPhone.replace(/\D/g, "").slice(-10)}`}
                                    target="_blank"
                                    rel="noreferrer"
                                    onClick={(e) => e.stopPropagation()}
                                    className="p-1 rounded-lg text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/50"
                                    title="Chat on WhatsApp"
                                  >
                                    <MessageSquare className="h-3.5 w-3.5" />
                                  </a>
                                )}
                              </div>
                            </td>

                            <td className="py-3 px-4 whitespace-nowrap">
                              <Badge
                                variant={cust.customerType === "Credit" ? "warning" : "default"}
                                className="text-[10px] uppercase font-bold"
                              >
                                {cust.customerType || "Cash"}
                                {cust.creditDays ? ` (${cust.creditDays}d)` : ""}
                              </Badge>
                            </td>

                            <td className="py-3 px-4 whitespace-nowrap">
                              <p className="text-[11px] text-zinc-700 dark:text-zinc-300">
                                {outletCount} {outletCount === 1 ? "Outlet" : "Outlets"}
                              </p>
                              {hasGst ? (
                                <span className="font-mono text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold block">
                                  {cust.gstin || cust.gstNumber}
                                </span>
                              ) : (
                                <span className="italic text-[10px] text-zinc-400 block">Unregistered</span>
                              )}
                            </td>

                            <td className="py-3 px-4 text-right whitespace-nowrap" onClick={(e) => e.stopPropagation()}>
                              <div className="flex items-center justify-end gap-1">
                                <Button
                                  size="sm"
                                  variant="outline"
                                  onClick={() => handleGenerateCustomerReport(cust)}
                                  className="h-7 text-xs px-2 text-indigo-600 dark:text-indigo-400 font-medium"
                                  title="View Day Report & WhatsApp Copy"
                                >
                                  <Printer className="h-3 w-3 mr-1" />
                                  Report
                                </Button>

                                <Button
                                  size="sm"
                                  variant="outline"
                                  onClick={() => setSelectedCustomerId(cust.id)}
                                  className="h-7 text-xs font-medium"
                                >
                                  <Info className="h-3 w-3 mr-1" />
                                  Profile
                                </Button>

                                <Button
                                  size="sm"
                                  variant="ghost"
                                  onClick={() => handleOpenEdit(cust)}
                                  className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-900"
                                  title="Edit Customer"
                                >
                                  <Edit2 className="h-3.5 w-3.5" />
                                </Button>

                                <Button
                                  size="sm"
                                  variant="ghost"
                                  onClick={() => handleDelete(cust.id)}
                                  className="h-7 w-7 p-0 text-red-500 hover:text-red-700"
                                  title="Delete Customer"
                                >
                                  <Trash2 className="h-3.5 w-3.5" />
                                </Button>
                              </div>
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )
          ) : (
            /* Registration Requests (User Requests) Master List View */
            <div className="space-y-4">
              {/* Sub-status filters */}
              <div className="flex items-center gap-2 overflow-x-auto pb-1">
                {(["ALL", "PENDING", "APPROVED", "REJECTED"] as const).map((st) => {
                  const count =
                    st === "ALL"
                      ? registrationRequests.length
                      : registrationRequests.filter((r) => r.status === st).length

                  const isSelected = requestFilterStatus === st
                  return (
                    <button
                      key={st}
                      type="button"
                      onClick={() => setRequestFilterStatus(st)}
                      className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all border ${
                        isSelected
                          ? st === "PENDING"
                            ? "bg-amber-500 text-white border-amber-500 shadow-sm"
                            : st === "APPROVED"
                            ? "bg-emerald-600 text-white border-emerald-600 shadow-sm"
                            : st === "REJECTED"
                            ? "bg-rose-600 text-white border-rose-600 shadow-sm"
                            : "bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 border-transparent shadow-sm"
                          : "bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 text-zinc-600 dark:text-zinc-400 hover:border-zinc-300 dark:hover:border-zinc-700"
                      }`}
                    >
                      <span>
                        {st === "ALL"
                          ? "All Requests"
                          : st === "PENDING"
                          ? "Pending Review"
                          : st === "APPROVED"
                          ? "Approved"
                          : "Rejected"}
                      </span>
                      <span
                        className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                          isSelected
                            ? "bg-white/20 text-white"
                            : "bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400"
                        }`}
                      >
                        {count}
                      </span>
                    </button>
                  )
                })}
              </div>

              {filteredRequests.length === 0 ? (
                <div className="p-12 text-center border border-dashed border-zinc-200 dark:border-zinc-800 rounded-2xl bg-white dark:bg-zinc-900/40">
                  <User className="h-10 w-10 text-muted-foreground mx-auto mb-3 opacity-60" />
                  <h3 className="font-semibold text-sm">No registration requests found</h3>
                  <p className="text-xs text-muted-foreground max-w-sm mx-auto mt-1">
                    {search
                      ? "No customer applications match your search query."
                      : "Share your onboarding link with retail buyers to receive self-registration applications with SMS OTP verification."}
                  </p>
                  <Button
                    onClick={() => setIsShareLinkModalOpen(true)}
                    variant="outline"
                    size="sm"
                    className="mt-4 text-xs gap-1.5 border-indigo-500/30 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-50"
                  >
                    <Share2 className="h-3.5 w-3.5" />
                    Share Registration Link
                  </Button>
                </div>
              ) : (
                <div className="overflow-hidden rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-xs">
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs border-collapse">
                      <thead>
                        <tr className="border-b border-zinc-200 dark:border-zinc-800 bg-zinc-50/80 dark:bg-zinc-900/80 text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
                          <th className="py-3 px-4">Firm & Proprietor</th>
                          <th className="py-3 px-4">Mobile & Contacts</th>
                          <th className="py-3 px-4">City / Market</th>
                          <th className="py-3 px-4">Submitted</th>
                          <th className="py-3 px-4">Status</th>
                          <th className="py-3 px-4 text-right">Actions</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                        {filteredRequests.map((req) => {
                          const isPending = req.status === "PENDING"
                          const isApproved = req.status === "APPROVED"
                          const isRejected = req.status === "REJECTED"

                          return (
                            <tr
                              key={req.id}
                              onClick={() => setSelectedRequestForDetails(req)}
                              className="hover:bg-indigo-50/30 dark:hover:bg-indigo-950/20 cursor-pointer transition-colors group"
                            >
                              <td className="py-3 px-4">
                                <div className="flex items-center gap-2.5">
                                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-indigo-500/10 text-indigo-700 font-bold text-xs dark:bg-indigo-500/20 dark:text-indigo-400 group-hover:bg-indigo-600 group-hover:text-white transition-colors">
                                    {(req.firmName || req.name || "R")[0].toUpperCase()}
                                  </div>
                                  <div className="min-w-0">
                                    <p className="font-bold text-sm text-zinc-900 dark:text-zinc-50 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors truncate">
                                      {req.firmName || req.name}
                                    </p>
                                    <p className="text-[11px] text-muted-foreground flex items-center gap-1 truncate">
                                      <User className="h-3 w-3 shrink-0" />
                                      <span>{req.name}</span>
                                      <span className="font-mono text-[10px] text-zinc-400 font-normal">
                                        • {req.id}
                                      </span>
                                    </p>
                                  </div>
                                </div>
                              </td>

                              <td className="py-3 px-4 whitespace-nowrap">
                                <div className="flex items-center gap-2">
                                  <span className="font-mono font-semibold text-zinc-800 dark:text-zinc-200">
                                    +91 {req.phone}
                                  </span>
                                  {req.phoneVerified && (
                                    <span className="text-[10px] font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 px-1.5 py-0.2 rounded">
                                      ✓ Verified
                                    </span>
                                  )}
                                  <a
                                    href={`https://wa.me/91${req.phone.replace(/\D/g, "").slice(-10)}`}
                                    target="_blank"
                                    rel="noreferrer"
                                    onClick={(e) => e.stopPropagation()}
                                    className="p-1 rounded-lg text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/50"
                                    title="Direct WhatsApp Chat"
                                  >
                                    <MessageSquare className="h-3.5 w-3.5" />
                                  </a>
                                </div>
                              </td>

                              <td className="py-3 px-4">
                                <p className="font-medium text-zinc-800 dark:text-zinc-200">
                                  {req.city || "Ahmedabad"}
                                </p>
                                <p className="text-[11px] text-muted-foreground truncate">
                                  {req.marketArea ? req.marketArea : req.address}
                                </p>
                              </td>

                              <td className="py-3 px-4 whitespace-nowrap text-[11px] text-muted-foreground">
                                {formatDate(req.createdAt)}
                              </td>

                              <td className="py-3 px-4 whitespace-nowrap">
                                {isPending && (
                                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
                                    <Clock className="h-3 w-3" />
                                    Pending Review
                                  </span>
                                )}
                                {isApproved && (
                                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
                                    <CheckCircle2 className="h-3 w-3" />
                                    Approved
                                    {req.createdCustomerId && (
                                      <span className="font-mono ml-0.5">#{req.createdCustomerId}</span>
                                    )}
                                  </span>
                                )}
                                {isRejected && (
                                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300">
                                    <XCircle className="h-3 w-3" />
                                    Rejected
                                  </span>
                                )}
                              </td>

                              <td className="py-3 px-4 text-right whitespace-nowrap" onClick={(e) => e.stopPropagation()}>
                                <div className="flex items-center justify-end gap-1.5">
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    onClick={() => setSelectedRequestForDetails(req)}
                                    className="h-7 px-2.5 text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 gap-1 border-indigo-200 dark:border-indigo-800"
                                    title="View Full Application in Side Panel"
                                  >
                                    <Eye className="h-3.5 w-3.5" />
                                    <span>View Details</span>
                                  </Button>

                                  {isPending && (
                                    <>
                                      <Button
                                        size="sm"
                                        onClick={() => handleOpenApprovalDialog(req)}
                                        className="h-7 px-2.5 text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white gap-1 shadow-xs"
                                      >
                                        <CheckCircle2 className="h-3 w-3" />
                                        <span>Approve</span>
                                      </Button>
                                      <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => handleOpenRejectDialog(req)}
                                        className="h-7 px-2 text-xs text-rose-600 border-rose-200 hover:bg-rose-50 dark:hover:bg-rose-950/40"
                                      >
                                        <XCircle className="h-3 w-3" />
                                      </Button>
                                    </>
                                  )}

                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => handleDeleteRequest(req.id, req.phone)}
                                    className="h-7 w-7 p-0 text-zinc-400 hover:text-red-600"
                                    title="Delete Request"
                                  >
                                    <Trash2 className="h-3.5 w-3.5" />
                                  </Button>
                                </div>
                              </td>
                            </tr>
                          )
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ============================================================== */}
          {/* SLIDE-OVER SIDE PANEL (RIGHT DRAWER) FOR FULL REQUEST DETAILS   */}
          {/* ============================================================== */}
          {activeDetailRequest && (
            <div className="fixed inset-0 z-50 overflow-hidden animate-in fade-in duration-200">
              {/* Backdrop */}
              <div
                className="absolute inset-0 bg-zinc-950/50 backdrop-blur-xs transition-opacity"
                onClick={() => setSelectedRequestForDetails(null)}
              />

              <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
                <div className="w-screen max-w-2xl bg-white dark:bg-zinc-900 border-l border-zinc-200 dark:border-zinc-800 shadow-2xl flex flex-col justify-between animate-in slide-in-from-right duration-300">
                  {/* Drawer Header */}
                  <div className="p-5 border-b border-zinc-100 dark:border-zinc-800 flex items-start justify-between gap-3 bg-zinc-50/50 dark:bg-zinc-900/50">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h3 className="text-lg font-bold text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
                          <Store className="h-5 w-5 text-indigo-600 shrink-0" />
                          <span>{activeDetailRequest.firmName || activeDetailRequest.name}</span>
                        </h3>
                        <Badge variant="outline" className="font-mono text-[10px] text-zinc-500">
                          {activeDetailRequest.id}
                        </Badge>
                        {activeDetailRequest.status === "PENDING" && (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
                            <Clock className="h-3 w-3" />
                            Pending Review
                          </span>
                        )}
                        {activeDetailRequest.status === "APPROVED" && (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
                            <CheckCircle2 className="h-3 w-3" />
                            Approved
                          </span>
                        )}
                        {activeDetailRequest.status === "REJECTED" && (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300">
                            <XCircle className="h-3 w-3" />
                            Rejected
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-muted-foreground">
                        Submitted on {formatDate(activeDetailRequest.createdAt)} via Himat Textile Web Registration
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={() => setSelectedRequestForDetails(null)}
                      className="p-1.5 rounded-xl text-zinc-400 hover:text-zinc-700 dark:hover:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
                    >
                      <X className="h-5 w-5" />
                    </button>
                  </div>

                  {/* Drawer Scrollable Body */}
                  <div className="flex-1 overflow-y-auto p-5 sm:p-6 space-y-6 text-xs">
                    {/* Section 1: Proprietor & Direct Contacts */}
                    <div className="space-y-3">
                      <h4 className="text-xs font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 flex items-center gap-1.5 border-b border-zinc-100 dark:border-zinc-800 pb-1.5">
                        <User className="h-3.5 w-3.5 text-indigo-600" />
                        <span>Proprietor & Direct Contacts</span>
                      </h4>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <div className="p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/60 dark:border-zinc-800 space-y-1">
                          <span className="text-[11px] text-muted-foreground block">Proprietor / Owner Name</span>
                          <span className="font-semibold text-sm text-zinc-900 dark:text-zinc-100">
                            {activeDetailRequest.name}
                          </span>
                        </div>

                        <div className="p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/60 dark:border-zinc-800 space-y-1">
                          <span className="text-[11px] text-muted-foreground block">Primary Mobile (SMS Verified)</span>
                          <div className="flex items-center justify-between">
                            <span className="font-mono font-bold text-sm text-zinc-900 dark:text-zinc-100">
                              +91 {activeDetailRequest.phone}
                            </span>
                            <span className="text-[10px] font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 px-1.5 py-0.5 rounded">
                              ✓ Verified
                            </span>
                          </div>
                          <div className="flex items-center gap-2 pt-1.5">
                            <a
                              href={`tel:+91${activeDetailRequest.phone.replace(/\D/g, "").slice(-10)}`}
                              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-zinc-200 dark:bg-zinc-700 text-zinc-800 dark:text-zinc-200 text-[11px] font-medium hover:bg-zinc-300"
                            >
                              <Phone className="h-3 w-3" /> Call
                            </a>
                            <a
                              href={`https://wa.me/91${activeDetailRequest.phone.replace(/\D/g, "").slice(-10)}`}
                              target="_blank"
                              rel="noreferrer"
                              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-600 text-white text-[11px] font-medium hover:bg-emerald-700"
                            >
                              <MessageSquare className="h-3 w-3" /> WhatsApp
                            </a>
                          </div>
                        </div>

                        {activeDetailRequest.phone2 && (
                          <div className="p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/60 dark:border-zinc-800 space-y-1">
                            <span className="text-[11px] text-muted-foreground block">Alternate WhatsApp Number</span>
                            <span className="font-mono font-medium text-sm text-zinc-900 dark:text-zinc-100">
                              +91 {activeDetailRequest.phone2}
                            </span>
                          </div>
                        )}

                        {activeDetailRequest.email && (
                          <div className="p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/60 dark:border-zinc-800 space-y-1">
                            <span className="text-[11px] text-muted-foreground block">Email Address</span>
                            <a
                              href={`mailto:${activeDetailRequest.email}`}
                              className="font-medium text-indigo-600 hover:underline break-all"
                            >
                              {activeDetailRequest.email}
                            </a>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Section 2: Shop Location & Address */}
                    <div className="space-y-3">
                      <h4 className="text-xs font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 flex items-center gap-1.5 border-b border-zinc-100 dark:border-zinc-800 pb-1.5">
                        <MapPin className="h-3.5 w-3.5 text-indigo-600" />
                        <span>Shop Address & Location Details</span>
                      </h4>
                      <div className="p-3.5 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/60 dark:border-zinc-800 space-y-2">
                        <div>
                          <span className="text-[11px] text-muted-foreground block">Complete Shop Address</span>
                          <p className="font-medium text-zinc-900 dark:text-zinc-100 mt-0.5">
                            {activeDetailRequest.address || activeDetailRequest.shopAddress}
                          </p>
                        </div>

                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-zinc-200/40 dark:border-zinc-700/40 text-[11px]">
                          <div>
                            <span className="text-muted-foreground block">City</span>
                            <span className="font-semibold text-zinc-800 dark:text-zinc-200">{activeDetailRequest.city || "Ahmedabad"}</span>
                          </div>
                          <div>
                            <span className="text-muted-foreground block">Market Area</span>
                            <span className="font-semibold text-zinc-800 dark:text-zinc-200">{activeDetailRequest.marketArea || "N/A"}</span>
                          </div>
                          <div>
                            <span className="text-muted-foreground block">District / State</span>
                            <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                              {activeDetailRequest.district ? `${activeDetailRequest.district}, ` : ""}{activeDetailRequest.state || "Gujarat"}
                            </span>
                          </div>
                          <div>
                            <span className="text-muted-foreground block">Pincode</span>
                            <span className="font-semibold font-mono text-zinc-800 dark:text-zinc-200">{activeDetailRequest.pincode || "N/A"}</span>
                          </div>
                        </div>

                        {activeDetailRequest.shopMapLink && (
                          <div className="pt-2">
                            <a
                              href={activeDetailRequest.shopMapLink}
                              target="_blank"
                              rel="noreferrer"
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800 hover:bg-indigo-100 transition-colors font-semibold text-xs"
                            >
                              <Navigation className="h-3.5 w-3.5 text-indigo-600" />
                              <span>Open Shop in Google Maps</span>
                              <ExternalLink className="h-3 w-3" />
                            </a>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Section 3: Garment Categories Preferences */}
                    {activeDetailRequest.garmentTypes && (
                      <div className="space-y-2">
                        <h4 className="text-xs font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 flex items-center gap-1.5 border-b border-zinc-100 dark:border-zinc-800 pb-1.5">
                          <Tag className="h-3.5 w-3.5 text-indigo-600" />
                          <span>Dealing Garment Categories</span>
                        </h4>
                        <div className="flex flex-wrap gap-1.5">
                          {activeDetailRequest.garmentTypes.split(",").map((cat, idx) => (
                            <span
                              key={idx}
                              className="px-2.5 py-1 rounded-lg bg-indigo-50 dark:bg-indigo-950/50 text-indigo-800 dark:text-indigo-300 border border-indigo-200/70 dark:border-indigo-800 font-medium text-xs"
                            >
                              {cat.trim()}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Section 4: Tax & Business Registration */}
                    <div className="space-y-3">
                      <h4 className="text-xs font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 flex items-center gap-1.5 border-b border-zinc-100 dark:border-zinc-800 pb-1.5">
                        <ShieldCheck className="h-3.5 w-3.5 text-indigo-600" />
                        <span>Tax & Identification</span>
                      </h4>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <div className="p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/60 dark:border-zinc-800 space-y-1">
                          <div className="flex items-center justify-between">
                            <span className="text-[11px] text-muted-foreground block">GSTIN Number</span>
                            {activeDetailRequest.keyType === "GSTIN" && (
                              <span className="text-[10px] font-bold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/60 px-1.5 py-0.5 rounded">
                                Primary Key
                              </span>
                            )}
                          </div>
                          {activeDetailRequest.gstin ? (
                            <span className="font-mono font-bold text-sm text-zinc-900 dark:text-zinc-100">
                              {activeDetailRequest.gstin}
                            </span>
                          ) : (
                            <span className="italic text-zinc-400">Unregistered / Composition</span>
                          )}
                        </div>

                        <div className="p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/60 dark:border-zinc-800 space-y-1">
                          <span className="text-[11px] text-muted-foreground block">PAN Number</span>
                          {activeDetailRequest.panNumber ? (
                            <span className="font-mono font-bold text-sm text-zinc-900 dark:text-zinc-100">
                              {activeDetailRequest.panNumber}
                            </span>
                          ) : (
                            <span className="italic text-zinc-400">Not provided</span>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Section 5: Logistics & Bank Details */}
                    <div className="space-y-3">
                      <h4 className="text-xs font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 flex items-center gap-1.5 border-b border-zinc-100 dark:border-zinc-800 pb-1.5">
                        <Truck className="h-3.5 w-3.5 text-indigo-600" />
                        <span>Logistics & Bank Settlement</span>
                      </h4>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <div className="p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/60 dark:border-zinc-800 space-y-1">
                          <span className="text-[11px] text-muted-foreground block">Preferred Transporter</span>
                          <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                            {activeDetailRequest.preferredTransporterName || "Not specified"}
                          </span>
                          {activeDetailRequest.transportPreference && (
                            <p className="text-[11px] text-zinc-500">Station: {activeDetailRequest.transportPreference}</p>
                          )}
                        </div>

                        <div className="p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/60 dark:border-zinc-800 space-y-1">
                          <span className="text-[11px] text-muted-foreground block">Bank Settlement</span>
                          <p className="font-semibold text-zinc-800 dark:text-zinc-200">
                            {activeDetailRequest.bankName || "Not specified"}
                          </p>
                          {activeDetailRequest.accountNumber && (
                            <p className="font-mono text-[11px] text-zinc-500">A/C: {activeDetailRequest.accountNumber}</p>
                          )}
                          {activeDetailRequest.ifscCode && (
                            <p className="font-mono text-[11px] text-zinc-500">IFSC: {activeDetailRequest.ifscCode}</p>
                          )}
                        </div>

                        {activeDetailRequest.notes && (
                          <div className="sm:col-span-2 p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200/60 dark:border-zinc-800 space-y-1">
                            <span className="text-[11px] text-muted-foreground block">Customer Delivery / Billing Notes</span>
                            <p className="text-zinc-700 dark:text-zinc-300 whitespace-pre-wrap">{activeDetailRequest.notes}</p>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Section 6: Uploaded KYC Documents Gallery */}
                    <div className="space-y-3">
                      <h4 className="text-xs font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400 flex items-center gap-1.5 border-b border-zinc-100 dark:border-zinc-800 pb-1.5">
                        <Camera className="h-3.5 w-3.5 text-indigo-600" />
                        <span>KYC & Verification Document Gallery</span>
                      </h4>
                      {!(activeDetailRequest.shopPhotoUri || activeDetailRequest.gstCertPhotoUri || activeDetailRequest.panPhotoUri || activeDetailRequest.aadharPhotoUri) ? (
                        <p className="text-muted-foreground text-xs italic p-3 bg-zinc-50 dark:bg-zinc-800/50 rounded-xl">
                          No KYC documents or photos were attached during online self-registration.
                        </p>
                      ) : (
                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                          {activeDetailRequest.shopPhotoUri && (
                            <div className="space-y-1 text-center">
                              <div
                                onClick={() => setLightbox({ open: true, url: activeDetailRequest.shopPhotoUri!, title: `${activeDetailRequest.firmName} - Shop Front Photo` })}
                                className="group relative aspect-square rounded-xl border border-zinc-200 dark:border-zinc-700 overflow-hidden bg-zinc-100 dark:bg-zinc-800 cursor-pointer shadow-xs hover:ring-2 hover:ring-indigo-500 transition-all"
                              >
                                <img src={activeDetailRequest.shopPhotoUri} alt="Shop Front" className="h-full w-full object-cover group-hover:scale-105 transition-transform" />
                                <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity text-white text-xs font-semibold">
                                  <Eye className="h-4 w-4 mr-1" /> View
                                </div>
                              </div>
                              <span className="text-[10px] font-medium text-zinc-600 dark:text-zinc-400 block truncate">Shop Front</span>
                            </div>
                          )}

                          {activeDetailRequest.gstCertPhotoUri && (
                            <div className="space-y-1 text-center">
                              <div
                                onClick={() => setLightbox({ open: true, url: activeDetailRequest.gstCertPhotoUri!, title: `${activeDetailRequest.firmName} - Visiting Card / GST Certificate` })}
                                className="group relative aspect-square rounded-xl border border-zinc-200 dark:border-zinc-700 overflow-hidden bg-zinc-100 dark:bg-zinc-800 cursor-pointer shadow-xs hover:ring-2 hover:ring-indigo-500 transition-all"
                              >
                                <img src={activeDetailRequest.gstCertPhotoUri} alt="GST / Visiting Card" className="h-full w-full object-cover group-hover:scale-105 transition-transform" />
                                <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity text-white text-xs font-semibold">
                                  <Eye className="h-4 w-4 mr-1" /> View
                                </div>
                              </div>
                              <span className="text-[10px] font-medium text-zinc-600 dark:text-zinc-400 block truncate">GST / Card</span>
                            </div>
                          )}

                          {activeDetailRequest.panPhotoUri && (
                            <div className="space-y-1 text-center">
                              <div
                                onClick={() => setLightbox({ open: true, url: activeDetailRequest.panPhotoUri!, title: `${activeDetailRequest.firmName} - PAN Card Photo` })}
                                className="group relative aspect-square rounded-xl border border-zinc-200 dark:border-zinc-700 overflow-hidden bg-zinc-100 dark:bg-zinc-800 cursor-pointer shadow-xs hover:ring-2 hover:ring-indigo-500 transition-all"
                              >
                                <img src={activeDetailRequest.panPhotoUri} alt="PAN Card" className="h-full w-full object-cover group-hover:scale-105 transition-transform" />
                                <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity text-white text-xs font-semibold">
                                  <Eye className="h-4 w-4 mr-1" /> View
                                </div>
                              </div>
                              <span className="text-[10px] font-medium text-zinc-600 dark:text-zinc-400 block truncate">PAN Card</span>
                            </div>
                          )}

                          {activeDetailRequest.aadharPhotoUri && (
                            <div className="space-y-1 text-center">
                              <div
                                onClick={() => setLightbox({ open: true, url: activeDetailRequest.aadharPhotoUri!, title: `${activeDetailRequest.firmName} - Aadhaar Card Photo` })}
                                className="group relative aspect-square rounded-xl border border-zinc-200 dark:border-zinc-700 overflow-hidden bg-zinc-100 dark:bg-zinc-800 cursor-pointer shadow-xs hover:ring-2 hover:ring-indigo-500 transition-all"
                              >
                                <img src={activeDetailRequest.aadharPhotoUri} alt="Aadhaar Card" className="h-full w-full object-cover group-hover:scale-105 transition-transform" />
                                <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity text-white text-xs font-semibold">
                                  <Eye className="h-4 w-4 mr-1" /> View
                                </div>
                              </div>
                              <span className="text-[10px] font-medium text-zinc-600 dark:text-zinc-400 block truncate">Aadhaar Card</span>
                            </div>
                          )}
                        </div>
                      )}
                    </div>

                    {/* Section 7: Audit / Approval Information */}
                    {activeDetailRequest.status === "APPROVED" && (
                      <div className="p-4 rounded-xl bg-emerald-50/70 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800/60 space-y-2">
                        <h4 className="text-xs font-bold text-emerald-800 dark:text-emerald-300 flex items-center gap-1.5">
                          <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                          <span>Commercial Account Approved</span>
                        </h4>
                        <div className="grid grid-cols-2 gap-2 text-[11px] text-emerald-900 dark:text-emerald-200">
                          <div>
                            <span className="text-emerald-700/70 dark:text-emerald-400/70 block">Customer ID</span>
                            <span className="font-mono font-bold">#CUST-{activeDetailRequest.createdCustomerId}</span>
                          </div>
                          <div>
                            <span className="text-emerald-700/70 dark:text-emerald-400/70 block">Assigned Agent</span>
                            <span className="font-semibold">{activeDetailRequest.assignedAgentName || "Unassigned"}</span>
                          </div>
                          <div>
                            <span className="text-emerald-700/70 dark:text-emerald-400/70 block">Payment Terms</span>
                            <span className="font-semibold">
                              {activeDetailRequest.creditType || "Cash"} {activeDetailRequest.creditDays ? `(${activeDetailRequest.creditDays} days)` : ""}
                            </span>
                          </div>
                          <div>
                            <span className="text-emerald-700/70 dark:text-emerald-400/70 block">Religion</span>
                            <span className="font-semibold">{activeDetailRequest.religion || "None"}</span>
                          </div>
                        </div>
                      </div>
                    )}

                    {activeDetailRequest.status === "REJECTED" && (
                      <div className="p-4 rounded-xl bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-800 space-y-1.5">
                        <h4 className="text-xs font-bold text-rose-800 dark:text-rose-300 flex items-center gap-1.5">
                          <XCircle className="h-4 w-4 text-rose-600" />
                          <span>Application Declined</span>
                        </h4>
                        <p className="text-xs text-rose-700 dark:text-rose-300">
                          Reason: {activeDetailRequest.rejectionReason || "Declined by Admin"}
                        </p>
                      </div>
                    )}
                  </div>

                  {/* Drawer Sticky Footer Actions */}
                  <div className="p-4 sm:p-5 border-t border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/90 flex flex-wrap items-center justify-between gap-2.5">
                    <div className="flex items-center gap-2">
                      <a
                        href={`https://wa.me/91${activeDetailRequest.phone.replace(/\D/g, "").slice(-10)}`}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center gap-1.5 h-8 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs shadow-xs transition-colors"
                      >
                        <MessageSquare className="h-3.5 w-3.5" />
                        <span>WhatsApp</span>
                      </a>

                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          handleDeleteRequest(activeDetailRequest.id, activeDetailRequest.phone)
                          setSelectedRequestForDetails(null)
                        }}
                        className="h-8 px-2.5 text-xs text-zinc-400 hover:text-red-600"
                        title="Delete this request permanently"
                      >
                        <Trash2 className="h-3.5 w-3.5 mr-1" />
                        Delete
                      </Button>
                    </div>

                    <div className="flex items-center gap-2">
                      {activeDetailRequest.status === "PENDING" && (
                        <>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleOpenRejectDialog(activeDetailRequest)}
                            className="h-8 px-3 text-xs text-rose-600 border-rose-200 hover:bg-rose-50 dark:hover:bg-rose-950/40 font-semibold"
                          >
                            <XCircle className="h-3.5 w-3.5 mr-1" />
                            Reject
                          </Button>

                          <Button
                            size="sm"
                            onClick={() => handleOpenApprovalDialog(activeDetailRequest)}
                            className="h-8 px-4 text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white gap-1.5 shadow-sm"
                          >
                            <CheckCircle2 className="h-3.5 w-3.5" />
                            <span>Approve & Assign</span>
                          </Button>
                        </>
                      )}

                      {activeDetailRequest.status === "APPROVED" && activeDetailRequest.createdCustomerId && (
                        <Button
                          size="sm"
                          onClick={() => {
                            setSelectedRequestForDetails(null)
                            setViewMode("customers")
                            setSelectedCustomerId(activeDetailRequest.createdCustomerId!)
                          }}
                          className="h-8 px-3 text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white gap-1"
                        >
                          <Store className="h-3.5 w-3.5 mr-1" />
                          View Customer Profile
                        </Button>
                      )}

                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setSelectedRequestForDetails(null)}
                        className="h-8 px-3 text-xs"
                      >
                        Close
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
          </>
        )}

      {/* Add / Edit Customer Multi-Tab Dialog */}
      <Dialog
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
        title={editingId ? "Edit Customer / Retailer Master" : "Register Customer Master (Sales & CRM)"}
        description="Standard retailer profile with multi-contacts, multiple shop outlets, KYC documents, and Tally export compliance."
      >
        <div className="space-y-4 pt-1 max-h-[80vh] overflow-y-auto pr-1">
          {/* Draft Notification Banner */}
          {hasDraft && !editingId && (
            <div className="flex items-center justify-between rounded-lg border border-emerald-200 bg-emerald-50 px-3.5 py-2 text-xs text-emerald-800">
              <div className="flex items-center gap-2">
                <Info className="h-4 w-4 text-emerald-600 shrink-0" />
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
              { value: "basic", label: "1. Firm & Owner" },
              { value: "contacts", label: "2. Contacts (Up to 5)" },
              { value: "outlets", label: "3. Outlets & Addresses" },
              { value: "crm", label: "4. Garments & Preferences" },
              { value: "kyc", label: "5. KYC & Photos" },
            ]}
          />

          {/* TAB 1: Firm & Owner */}
          {activeFormTab === "basic" && (
            <div className="space-y-3.5 text-xs">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Shop / Firm Name <span className="text-red-500">*</span>
                  </label>
                  <Input
                    required
                    value={firmName}
                    onChange={(e) => setFirmName(e.target.value)}
                    placeholder="e.g. Balaji Sarees & Garments"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Proprietor / Owner Name <span className="text-red-500">*</span>
                  </label>
                  <Input
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="e.g. Ramesh Bhai Patel"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Customer ID
                  </label>
                  <Input
                    value={customerId}
                    onChange={(e) => setCustomerId(e.target.value)}
                    placeholder="e.g. CUST-101"
                    className="mt-1 h-8 text-xs font-mono"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    GSTIN
                  </label>
                  <Input
                    value={gstin}
                    onChange={(e) => setGstin(e.target.value.toUpperCase())}
                    placeholder="e.g. 24ABCDE1234F1Z5"
                    className="mt-1 h-8 text-xs font-mono uppercase"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    PAN Card Number
                  </label>
                  <Input
                    value={panNumber}
                    onChange={(e) => setPanNumber(e.target.value.toUpperCase())}
                    placeholder="e.g. ABCDE1234F"
                    className="mt-1 h-8 text-xs font-mono uppercase"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    City *
                  </label>
                  <Input
                    required
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="e.g. Ahmedabad / Delhi"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    District
                  </label>
                  <Input
                    value={district}
                    onChange={(e) => setDistrict(e.target.value)}
                    placeholder="District name"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    State *
                  </label>
                  <Input
                    required
                    value={state}
                    onChange={(e) => setState(e.target.value)}
                    placeholder="e.g. Gujarat"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Pincode
                  </label>
                  <Input
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value)}
                    placeholder="e.g. 380001"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 border-t border-zinc-100 dark:border-zinc-800">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Billing Terms (Cash / Credit)
                  </label>
                  <select
                    value={customerType}
                    onChange={(e) => setCustomerType(e.target.value)}
                    className="mt-1 w-full h-8 rounded-md border border-zinc-300 bg-white px-2.5 text-xs text-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                  >
                    <option value="Cash">Cash Customer</option>
                    <option value="Credit">Credit Customer</option>
                  </select>
                </div>

                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Credit Period (Days)
                  </label>
                  <Input
                    type="number"
                    value={creditDays}
                    onChange={(e) => setCreditDays(e.target.value)}
                    placeholder="30"
                    className="mt-1 h-8 text-xs"
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Credit Limit (₹)
                  </label>
                  <Input
                    type="number"
                    value={creditLimit}
                    onChange={(e) => setCreditLimit(e.target.value)}
                    placeholder="e.g. 500000"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: Contacts (Up to 5) */}
          {activeFormTab === "contacts" && (
            <div className="space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="font-semibold text-zinc-800 dark:text-zinc-200">Registered Contacts</h4>
                  <p className="text-[11px] text-muted-foreground">Add up to 5 contact persons, numbers & roles</p>
                </div>
                {contacts.length < 5 && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      setContacts([...contacts, { name: "", phone: "", designation: "Sales Incharge", email: "" }])
                    }
                    className="h-7 text-xs gap-1"
                  >
                    <Plus className="h-3 w-3" />
                    Add Contact
                  </Button>
                )}
              </div>

              {contacts.map((contact, idx) => (
                <div key={idx} className="p-3 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-900/50 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-xs text-indigo-600 dark:text-indigo-400">
                      Contact #{idx + 1} {idx === 0 ? "(Primary)" : ""}
                    </span>
                    {contacts.length > 1 && (
                      <button
                        type="button"
                        onClick={() => setContacts(contacts.filter((_, i) => i !== idx))}
                        className="text-red-500 hover:text-red-700 text-xs flex items-center gap-0.5"
                      >
                        <Trash2 className="h-3 w-3" /> Remove
                      </button>
                    )}
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    <div>
                      <label className="text-[11px] text-muted-foreground">Phone Number *</label>
                      <Input
                        required={idx === 0}
                        placeholder="+91 98..."
                        value={contact.phone}
                        onChange={(e) => {
                          const updated = [...contacts]
                          updated[idx].phone = e.target.value
                          setContacts(updated)
                        }}
                        className="h-8 text-xs mt-0.5"
                      />
                    </div>
                    <div>
                      <label className="text-[11px] text-muted-foreground">Contact Person Name</label>
                      <Input
                        placeholder="e.g. Ramesh Patel"
                        value={contact.name || ""}
                        onChange={(e) => {
                          const updated = [...contacts]
                          updated[idx].name = e.target.value
                          setContacts(updated)
                        }}
                        className="h-8 text-xs mt-0.5"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    <div>
                      <label className="text-[11px] text-muted-foreground">Designation / Note</label>
                      <Input
                        placeholder="Owner, Partner, Purchaser, Accounts"
                        value={contact.designation || ""}
                        onChange={(e) => {
                          const updated = [...contacts]
                          updated[idx].designation = e.target.value
                          setContacts(updated)
                        }}
                        className="h-8 text-xs mt-0.5"
                      />
                    </div>
                    <div>
                      <label className="text-[11px] text-muted-foreground">Email Address</label>
                      <Input
                        placeholder="email@domain.com"
                        value={contact.email || ""}
                        onChange={(e) => {
                          const updated = [...contacts]
                          updated[idx].email = e.target.value
                          setContacts(updated)
                        }}
                        className="h-8 text-xs mt-0.5"
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* TAB 3: Outlets & Addresses (Up to 5) */}
          {activeFormTab === "outlets" && (
            <div className="space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="font-semibold text-zinc-800 dark:text-zinc-200">Shop Outlets & Locations</h4>
                  <p className="text-[11px] text-muted-foreground">Add up to 5 shop branches with addresses & Google Map links</p>
                </div>
                {outlets.length < 5 && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      setOutlets([...outlets, { name: `Outlet ${outlets.length + 1}`, address: "", pincode: "", mapLink: "" }])
                    }
                    className="h-7 text-xs gap-1"
                  >
                    <Plus className="h-3 w-3" />
                    Add Outlet
                  </Button>
                )}
              </div>

              {outlets.map((outlet, idx) => (
                <div key={idx} className="p-3 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-900/50 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-xs text-emerald-600 dark:text-emerald-400">
                      Outlet #{idx + 1} {idx === 0 ? "(Main Shop)" : ""}
                    </span>
                    {outlets.length > 1 && (
                      <button
                        type="button"
                        onClick={() => setOutlets(outlets.filter((_, i) => i !== idx))}
                        className="text-red-500 hover:text-red-700 text-xs flex items-center gap-0.5"
                      >
                        <Trash2 className="h-3 w-3" /> Remove
                      </button>
                    )}
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                    <div className="sm:col-span-2">
                      <label className="text-[11px] text-muted-foreground">Outlet / Branch Name</label>
                      <Input
                        placeholder="e.g. Main Showroom, Branch 2"
                        value={outlet.name}
                        onChange={(e) => {
                          const updated = [...outlets]
                          updated[idx].name = e.target.value
                          setOutlets(updated)
                        }}
                        className="h-8 text-xs mt-0.5"
                      />
                    </div>
                    <div>
                      <label className="text-[11px] text-muted-foreground">Pincode</label>
                      <Input
                        placeholder="e.g. 110005"
                        value={outlet.pincode || ""}
                        onChange={(e) => {
                          const updated = [...outlets]
                          updated[idx].pincode = e.target.value
                          setOutlets(updated)
                        }}
                        className="h-8 text-xs mt-0.5"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="text-[11px] text-muted-foreground">Full Address</label>
                    <Input
                      placeholder="Shop number, building, market, area, city"
                      value={outlet.address}
                      onChange={(e) => {
                        const updated = [...outlets]
                        updated[idx].address = e.target.value
                        setOutlets(updated)
                      }}
                      className="h-8 text-xs mt-0.5"
                    />
                  </div>

                  <div>
                    <label className="text-[11px] text-muted-foreground">Google Maps Link</label>
                    <Input
                      placeholder="https://maps.app.goo.gl/... or landmark coordinates"
                      value={outlet.mapLink || ""}
                      onChange={(e) => {
                        const updated = [...outlets]
                        updated[idx].mapLink = e.target.value
                        setOutlets(updated)
                      }}
                      className="h-8 text-xs mt-0.5 font-mono text-[11px]"
                    />
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* TAB 4: Garments & Preferences */}
          {activeFormTab === "crm" && (
            <div className="space-y-3.5 text-xs">
              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Which type of garments they deal with mostly?
                </label>
                <div className="mt-2 flex flex-wrap gap-1.5 max-h-32 overflow-y-auto p-2 rounded-xl bg-zinc-50 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800">
                  {GARMENT_CATEGORIES.map((cat) => {
                    const isSelected = selectedCategories.includes(cat)
                    return (
                      <button
                        key={cat}
                        type="button"
                        onClick={() => handleToggleCategory(cat)}
                        className={`text-[11px] px-2.5 py-1 rounded-full border transition-all flex items-center gap-1 ${
                          isSelected
                            ? "bg-zinc-900 text-white border-zinc-900 dark:bg-zinc-100 dark:text-zinc-900 font-semibold shadow-sm"
                            : "bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 border-zinc-200 dark:border-zinc-700 hover:border-zinc-400"
                        }`}
                      >
                        {isSelected && <Check className="h-2.5 w-2.5" />}
                        <span>{cat}</span>
                      </button>
                    )
                  })}
                </div>
                <div className="mt-2">
                  <Input
                    value={customCategory}
                    onChange={(e) => setCustomCategory(e.target.value)}
                    placeholder="+ Add custom garment type (e.g. Rayon Kurtis, Silk Sarees)"
                    className="h-8 text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-zinc-100 dark:border-zinc-800">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Transport / Logistics Preference
                  </label>
                  <select
                    value={preferredTransporterName}
                    onChange={(e) => setPreferredTransporterName(e.target.value)}
                    className="mt-1 w-full h-8 rounded-md border border-zinc-300 bg-white px-2.5 text-xs text-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                  >
                    <option value="">-- Select from Transporters Master --</option>
                    {transporters.map((t) => (
                      <option key={t.id} value={t.transporterName}>
                        {t.transporterName} ({t.city || "Hub"})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <ReferrerSelectModal
                    value={referredBy}
                    onChange={setReferredBy}
                    employees={employees}
                    customers={customers}
                    suppliers={suppliers}
                    label="Referred By (Entity Link)"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Added / Creator Agent
                  </label>
                  <select
                    value={addedByAgentName}
                    onChange={(e) => setAddedByAgentName(e.target.value)}
                    className="mt-1 w-full h-8 rounded-md border border-zinc-300 bg-white px-2.5 text-xs text-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                  >
                    <option value={user?.displayName || "Admin"}>{user?.displayName || "Current Logged-in User"}</option>
                    {employees.map((e) => (
                      <option key={e.id} value={e.name}>
                        {e.name} ({e.role})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Date of Birth (DOB)
                  </label>
                  <Input
                    type="date"
                    value={dob}
                    onChange={(e) => setDob(e.target.value)}
                    className="mt-1 h-8 text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Religion (CRM Demographic)
                  </label>
                  <Input
                    placeholder="e.g. Hindu, Jain, Muslim, Sikh"
                    value={religion}
                    onChange={(e) => setReligion(e.target.value)}
                    className="mt-1 h-8 text-xs"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Special Notes & Sourcing Habits
                </label>
                <textarea
                  rows={2}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Payment punctuality, preferred payment days, fabric preferences..."
                  className="mt-1 w-full rounded-md border border-zinc-300 bg-white p-2 text-xs text-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                />
              </div>
            </div>
          )}

          {/* TAB 5: KYC & Documents */}
          {activeFormTab === "kyc" && (
            <div className="space-y-3.5 text-xs">
              <p className="text-[11px] text-muted-foreground">
                Upload photos directly to Firebase Cloud Storage for KYC verification, fraud prevention, and instant cross-platform viewing.
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <FileUpload
                  label="Aadhaar Card Photo"
                  folder={`customers/${customerId || "temp"}/kyc`}
                  prefix="aadhar"
                  value={aadharPhotoUri}
                  onChange={setAadharPhotoUri}
                />

                <FileUpload
                  label="GST Certificate Photo"
                  folder={`customers/${customerId || "temp"}/kyc`}
                  prefix="gst_cert"
                  value={gstCertPhotoUri}
                  onChange={setGstCertPhotoUri}
                />

                <FileUpload
                  label="PAN Card Photo"
                  folder={`customers/${customerId || "temp"}/kyc`}
                  prefix="pan"
                  value={panPhotoUri}
                  onChange={setPanPhotoUri}
                />

                <FileUpload
                  label="Shop Front / Signboard Photo"
                  folder={`customers/${customerId || "temp"}/photos`}
                  prefix="shop"
                  value={shopPhotoUri}
                  onChange={setShopPhotoUri}
                />

                <FileUpload
                  label="Purchaser / Owner Photo"
                  folder={`customers/${customerId || "temp"}/photos`}
                  prefix="purchaser"
                  value={purchaserPhotoUri}
                  onChange={setPurchaserPhotoUri}
                />

                <FileUpload
                  label="Cancelled Cheque Photo"
                  folder={`customers/${customerId || "temp"}/kyc`}
                  prefix="cheque"
                  value={cancelChequePhotoUri}
                  onChange={setCancelChequePhotoUri}
                />
              </div>
            </div>
          )}


          {/* Dialog Action Buttons */}
          <div className="flex items-center justify-between pt-3 border-t border-zinc-200 dark:border-zinc-800">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsDialogOpen(false)}
              className="h-8 text-xs"
            >
              Cancel
            </Button>
            <Button
              type="button"
              size="sm"
              onClick={handleSave}
              className="h-8 text-xs font-semibold bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900"
            >
              {editingId ? "Update Customer" : "Save Customer"}
            </Button>
          </div>
        </div>
      </Dialog>

      {/* 1. Share Customer Registration Link Modal */}
      <Dialog
        open={isShareLinkModalOpen}
        onOpenChange={setIsShareLinkModalOpen}
        title="🔗 Share Customer Self-Registration Link"
        description="Share this link with retail buyers. They will fill basic business & KYC details and verify via SMS OTP."
      >
        <div className="space-y-4 pt-2 text-xs">
          <div className="p-3.5 rounded-xl bg-indigo-50/60 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900/50 space-y-2">
            <p className="font-semibold text-indigo-900 dark:text-indigo-200">
              Customer Public Onboarding Link
            </p>
            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={getPublicRegistrationUrl()}
                className="flex-1 px-3 py-2 rounded-lg border border-indigo-200 dark:border-indigo-800 bg-white dark:bg-zinc-900 font-mono text-xs text-zinc-800 dark:text-zinc-200"
              />
              <Button
                size="sm"
                onClick={handleCopyRegistrationLink}
                className={`h-8 px-3 text-xs gap-1.5 transition-all ${
                  copiedLink
                    ? "bg-emerald-600 hover:bg-emerald-700 text-white"
                    : "bg-indigo-600 hover:bg-indigo-700 text-white"
                }`}
              >
                {copiedLink ? (
                  <>
                    <Check className="h-3.5 w-3.5" />
                    <span>Copied!</span>
                  </>
                ) : (
                  <>
                    <Copy className="h-3.5 w-3.5" />
                    <span>Copy Link</span>
                  </>
                )}
              </Button>
            </div>
          </div>

          {/* 1-Click WhatsApp Share */}
          <div className="p-3.5 rounded-xl bg-emerald-50/50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/50 space-y-2">
            <p className="font-semibold text-emerald-900 dark:text-emerald-200 flex items-center gap-1.5">
              <MessageSquare className="h-4 w-4 text-emerald-600" />
              <span>Share Directly on WhatsApp</span>
            </p>
            <p className="text-muted-foreground text-[11px]">
              Sends a pre-composed message invitation with the link directly to buyer on WhatsApp.
            </p>
            <Button
              type="button"
              onClick={() => {
                const msg = encodeURIComponent(buildRegistrationInviteMessage())
                window.open(`https://wa.me/?text=${msg}`, "_blank")
              }}
              className="w-full h-9 text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white gap-2 shadow-sm"
            >
              <MessageSquare className="h-4 w-4" />
              <span>Share Invite to Any WhatsApp Contact</span>
            </Button>
          </div>

          {/* Direct Send to Specific WhatsApp Number */}
          <div className="p-3.5 rounded-xl bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-700 space-y-2">
            <p className="font-semibold text-zinc-900 dark:text-zinc-100 flex items-center gap-1.5">
              <Phone className="h-3.5 w-3.5 text-emerald-600" />
              <span>Send Directly to Buyer's Mobile Number</span>
            </p>
            <p className="text-muted-foreground text-[11px]">
              Type the retailer's 10-digit number to open WhatsApp directly with their personal chat:
            </p>
            <div className="flex gap-2">
              <div className="relative flex-1">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-zinc-400">
                  +91
                </span>
                <input
                  type="tel"
                  maxLength={10}
                  placeholder="9876543210"
                  value={directSharePhone}
                  onChange={(e) => setDirectSharePhone(e.target.value.replace(/\D/g, "").slice(0, 10))}
                  className="w-full pl-10 pr-3 py-2 text-xs rounded-lg border border-zinc-300 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800 font-mono focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>
              <Button
                type="button"
                disabled={directSharePhone.length < 10}
                onClick={() => {
                  const msg = encodeURIComponent(buildRegistrationInviteMessage())
                  window.open(`https://wa.me/91${directSharePhone}?text=${msg}`, "_blank")
                }}
                className="h-auto px-3.5 text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white disabled:opacity-40 gap-1.5 shadow-sm"
              >
                <MessageSquare className="h-3.5 w-3.5" />
                <span>Send WhatsApp</span>
              </Button>
            </div>
          </div>

          <div className="p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200 dark:border-zinc-700 space-y-1 text-[11px] text-muted-foreground">
            <p className="font-semibold text-zinc-700 dark:text-zinc-300">
              Form Configuration Note:
            </p>
            <ul className="list-disc list-inside space-y-0.5">
              <li>Sales Agent and Religion fields are intentionally excluded from the customer portal.</li>
              <li>Customers verify their phone number through SMS OTP via Firebase Phone Auth.</li>
              <li>Once submitted, the request will appear in your <strong>User Requests</strong> tab for approval.</li>
            </ul>
          </div>

          <div className="pt-2 flex justify-end">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsShareLinkModalOpen(false)}
              className="h-8 text-xs"
            >
              Close
            </Button>
          </div>
        </div>
      </Dialog>

      {/* 2. Approve Request & Assign Agent Dialog */}
      <Dialog
        open={selectedRequestForApproval !== null}
        onOpenChange={(open) => !open && setSelectedRequestForApproval(null)}
        title="Approve Customer Registration & Assign Agent"
        description={`Approving retail buyer application for ${selectedRequestForApproval?.firmName || selectedRequestForApproval?.name}`}
      >
        {selectedRequestForApproval && (
          <div className="space-y-4 pt-2 text-xs">
            {/* Quick summary of submitted application */}
            <div className="p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/60 border border-zinc-200 dark:border-zinc-700 space-y-1.5">
              <div className="flex justify-between items-center text-[11px]">
                <span className="text-muted-foreground">Firm Name:</span>
                <span className="font-bold text-zinc-900 dark:text-zinc-100">{selectedRequestForApproval.firmName}</span>
              </div>
              <div className="flex justify-between items-center text-[11px]">
                <span className="text-muted-foreground">Owner Name:</span>
                <span className="font-semibold text-zinc-900 dark:text-zinc-100">{selectedRequestForApproval.name}</span>
              </div>
              <div className="flex justify-between items-center text-[11px]">
                <span className="text-muted-foreground">Verified Phone:</span>
                <span className="font-mono font-semibold text-emerald-600 dark:text-emerald-400">
                  {selectedRequestForApproval.phone} (SMS Verified)
                </span>
              </div>
              <div className="flex justify-between items-center text-[11px]">
                <span className="text-muted-foreground">Location:</span>
                <span className="text-zinc-800 dark:text-zinc-200">
                  {selectedRequestForApproval.city}{selectedRequestForApproval.marketArea ? `, ${selectedRequestForApproval.marketArea}` : ""}
                </span>
              </div>
            </div>

            {/* Admin assignment controls */}
            <div className="space-y-3 pt-1">
              {/* Assign Sales Agent */}
              <div className="space-y-1">
                <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                  <span>Assign Sales Agent (कर्मचारी / सेल्समैन)</span>
                  <span className="text-red-500 font-bold">*</span>
                </label>
                <select
                  required
                  value={approvalAssignedAgentId}
                  onChange={(e) => {
                    const id = e.target.value
                    setApprovalAssignedAgentId(id)
                    const found = employees.find((emp) => String(emp.id) === String(id))
                    setApprovalAssignedAgentName(found ? found.name : "")
                  }}
                  className="w-full h-9 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium"
                >
                  <option value="">-- Select Sales Agent --</option>
                  {employees
                    .filter((e) => !e.isBlocked && !e.isDeleted)
                    .map((emp) => (
                      <option key={emp.id} value={emp.id}>
                        {emp.name} ({emp.role})
                      </option>
                    ))}
                </select>
                <p className="text-[10px] text-muted-foreground">
                  The customer will be managed under this salesman's portfolio.
                </p>
              </div>

              {/* Billing / Credit Terms */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 pt-1">
                <div className="space-y-1">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    Customer Type
                  </label>
                  <select
                    value={approvalCreditType}
                    onChange={(e) => {
                      const type = e.target.value as "Cash" | "Credit"
                      setApprovalCreditType(type)
                      if (type === "Cash") setApprovalCreditDays(0)
                      else if (approvalCreditDays === 0) setApprovalCreditDays(30)
                    }}
                    className="w-full h-8 rounded-lg border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-2.5 text-xs"
                  >
                    <option value="Cash">Cash (नकद)</option>
                    <option value="Credit">Credit (उधार)</option>
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    Credit Days
                  </label>
                  <input
                    type="number"
                    min={0}
                    value={approvalCreditDays}
                    onChange={(e) => setApprovalCreditDays(Number(e.target.value) || 0)}
                    disabled={approvalCreditType === "Cash"}
                    className="w-full h-8 px-2.5 rounded-lg border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs font-mono disabled:opacity-50"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    Credit Limit (₹)
                  </label>
                  <input
                    type="number"
                    min={0}
                    step={5000}
                    placeholder="50000"
                    value={approvalCreditLimit || ""}
                    onChange={(e) => setApprovalCreditLimit(Number(e.target.value) || 0)}
                    disabled={approvalCreditType === "Cash"}
                    className="w-full h-8 px-2.5 rounded-lg border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs font-mono disabled:opacity-50"
                  />
                </div>
              </div>

              {/* Religion (CRM Demographic) */}
              <div className="space-y-1.5 pt-1">
                <div className="flex items-center justify-between">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    Religion (CRM Demographic)
                  </label>
                  <span className="text-[10px] text-muted-foreground font-normal">Optional</span>
                </div>
                <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
                  <input
                    type="text"
                    placeholder="e.g. Hindu, Jain, Muslim, Sikh..."
                    value={approvalReligion}
                    onChange={(e) => setApprovalReligion(e.target.value)}
                    className="flex-1 h-8 px-2.5 rounded-lg border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium"
                  />
                  <div className="flex items-center gap-1 shrink-0">
                    {["Hindu", "Jain", "Muslim", "Sikh"].map((rel) => (
                      <button
                        key={rel}
                        type="button"
                        onClick={() => setApprovalReligion(rel)}
                        className={`px-2 py-1 rounded-md text-[11px] font-semibold transition-colors border ${
                          approvalReligion === rel
                            ? "bg-indigo-600 text-white border-indigo-600 shadow-2xs"
                            : "bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 border-zinc-200 dark:border-zinc-700 hover:bg-zinc-200 dark:hover:bg-zinc-700"
                        }`}
                      >
                        {rel}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="pt-3 border-t border-zinc-200 dark:border-zinc-800 flex items-center justify-between">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setSelectedRequestForApproval(null)}
                className="h-8 text-xs"
              >
                Cancel
              </Button>

              <Button
                type="button"
                size="sm"
                disabled={isApproving || !approvalAssignedAgentId}
                onClick={handleConfirmApproval}
                className="h-8 px-4 text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white gap-1.5 shadow-sm"
              >
                <CheckCircle2 className="h-3.5 w-3.5" />
                <span>{isApproving ? "Approving & Creating..." : "Confirm & Create Customer"}</span>
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      {/* 3. Reject Request Dialog */}
      <Dialog
        open={rejectModal.open}
        onOpenChange={(open) => !open && setRejectModal({ open: false, request: null, reason: "" })}
        title="Reject Customer Registration Request"
        description={`Declining application for ${rejectModal.request?.firmName || "this customer"}`}
      >
        <div className="space-y-3 pt-2 text-xs">
          <div className="space-y-1.5">
            <label className="font-semibold text-zinc-800 dark:text-zinc-200">
              Reason for Rejection (अस्वीकृति का कारण)
            </label>
            <textarea
              rows={3}
              placeholder="e.g. Incomplete address, unable to verify business documents, outside delivery area..."
              value={rejectModal.reason}
              onChange={(e) => setRejectModal((prev) => ({ ...prev, reason: e.target.value }))}
              className="w-full p-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-900 text-xs focus:outline-none focus:ring-2 focus:ring-rose-500"
            />
          </div>

          <div className="pt-2 border-t border-zinc-200 dark:border-zinc-800 flex items-center justify-between">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setRejectModal({ open: false, request: null, reason: "" })}
              className="h-8 text-xs"
            >
              Cancel
            </Button>

            <Button
              type="button"
              size="sm"
              disabled={isRejecting}
              onClick={handleConfirmReject}
              className="h-8 px-4 text-xs font-semibold bg-rose-600 hover:bg-rose-700 text-white gap-1.5"
            >
              <XCircle className="h-3.5 w-3.5" />
              <span>{isRejecting ? "Rejecting..." : "Confirm Rejection"}</span>
            </Button>
          </div>
        </div>
      </Dialog>

      {/* 4. Post-Approval Instant WhatsApp Welcome Dialog */}
      <Dialog
        open={approvedSuccessData?.open || false}
        onOpenChange={(open) => !open && setApprovedSuccessData(null)}
        title="Customer Approved & Created!"
        description="The customer account has been created and added to your active database."
      >
        {approvedSuccessData && (
          <div className="space-y-4 pt-2 text-xs">
            <div className="p-4 rounded-2xl bg-emerald-50/80 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800 text-center space-y-2">
              <div className="w-12 h-12 bg-emerald-100 dark:bg-emerald-900/60 rounded-full flex items-center justify-center text-emerald-600 dark:text-emerald-400 mx-auto">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <h4 className="text-sm font-bold text-emerald-950 dark:text-emerald-200">
                {approvedSuccessData.req.firmName || approvedSuccessData.req.name}
              </h4>
              <p className="font-mono font-bold text-xs text-zinc-800 dark:text-zinc-200">
                Customer ID: #CUST-{approvedSuccessData.customerId}
              </p>
              <p className="text-[11px] text-muted-foreground">
                Assigned Sales Agent: <strong className="text-zinc-800 dark:text-zinc-200">{approvedSuccessData.agentName}</strong>
              </p>
            </div>

            <div className="p-3.5 rounded-xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200 dark:border-zinc-700 space-y-2">
              <p className="font-semibold text-zinc-900 dark:text-zinc-100 flex items-center gap-1.5">
                <MessageSquare className="h-4 w-4 text-emerald-600" />
                <span>Send Welcome & Customer ID on WhatsApp</span>
              </p>
              <p className="text-muted-foreground text-[11px]">
                Notify the retailer on WhatsApp that their account has been approved, provide their new Customer ID, and introduce their assigned sales representative.
              </p>
              <Button
                type="button"
                onClick={() => {
                  const msg = encodeURIComponent(
                    buildApprovalWelcomeMessage(
                      approvedSuccessData.req,
                      approvedSuccessData.customerId,
                      approvedSuccessData.agentName
                    )
                  )
                  window.open(
                    `https://wa.me/91${approvedSuccessData.req.phone.replace(/\D/g, "").slice(-10)}?text=${msg}`,
                    "_blank"
                  )
                  setApprovedSuccessData(null)
                }}
                className="w-full h-9 text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white gap-2 shadow-sm"
              >
                <MessageSquare className="h-4 w-4" />
                <span>Send Welcome to {approvedSuccessData.req.phone} on WhatsApp</span>
              </Button>
            </div>

            <div className="pt-2 flex justify-end">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setApprovedSuccessData(null)}
                className="h-8 text-xs"
              >
                Close & View Requests
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      {/* Report Modal */}
      <ReportViewerModal
        open={reportModal.open}
        onOpenChange={(open) => !open && setReportModal({ open: false, title: "", html: "", whatsAppText: "" })}
        title={reportModal.title}
        htmlContent={reportModal.html}
        whatsAppText={reportModal.whatsAppText}
      />

      {/* Lightbox for KYC Photos */}
      <ImageLightboxModal
        open={lightbox.open}
        onClose={() => setLightbox((prev) => ({ ...prev, open: false }))}
        imageUrl={lightbox.url}
        title={lightbox.title}
      />
    </div>
  )
}
