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
  Tag,
  Store,
  User,
  ExternalLink,
  Edit2,
  Trash2,
  Check,
  ShieldCheck,
  Home,
  Info,
  Printer,
  FileText,
  FileDown,
  Compass,
  CreditCard,
  Eye,
  Share2,
  MessageSquare,
  CheckCircle2,
  Clock,
  XCircle,
  Copy,
  Factory,
  RefreshCw,
  Sparkles
} from "lucide-react"
import { useData } from "../context/DataContext"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { ReferrerSelectModal } from "../components/ui/ReferrerSelectModal"
import { Tabs } from "../components/ui/Tabs"
import { Supplier, SupplierAddress, Visit, SupplierRegistrationRequest } from "../types"
import { GARMENT_CATEGORIES } from "../lib/constants"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateSupplierInvoiceHtml,
  buildSupplierInvoiceWhatsAppText,
} from "../lib/pdfReports"
import { generateSuppliersTallyXml, downloadXmlFile } from "../lib/tallyExport"
import { FileUpload } from "../components/ui/FileUpload"
import { ImageLightboxModal } from "../components/ui/ImageLightboxModal"
import { SupplierDetailView } from "./SupplierDetailView"
import {
  fetchGstDetails,
  isValidGstin,
  extractPanFromGstin,
  getStateFromGstin,
} from "../lib/gstHelper"

export function SuppliersView() {
  const {
    suppliers,
    visits,
    entries,
    customers,
    employees,
    markets,
    brands,
    saveSupplier,
    deleteSupplier,
    saveMarket,
    saveBrand,
    supplierRegistrationRequests,
    pendingSupplierRegistrationRequestsCount,
    approveSupplierRegistrationRequest,
    rejectSupplierRegistrationRequest,
    deleteSupplierRegistrationRequest,
  } = useData()

  // Navigation mode: 'suppliers' vs 'requests'
  const [viewMode, setViewMode] = useState<"suppliers" | "requests">("suppliers")
  const [isShareLinkModalOpen, setIsShareLinkModalOpen] = useState<boolean>(false)
  const [copiedLink, setCopiedLink] = useState<boolean>(false)
  const [directSharePhone, setDirectSharePhone] = useState<string>("")

  // Registration Requests Management State
  const [requestFilterStatus, setRequestFilterStatus] = useState<"ALL" | "PENDING" | "APPROVED" | "REJECTED">("PENDING")
  const [selectedRequestForDetails, setSelectedRequestForDetails] = useState<SupplierRegistrationRequest | null>(null)
  const [selectedRequestForApproval, setSelectedRequestForApproval] = useState<SupplierRegistrationRequest | null>(null)
  const [approvalBrand, setApprovalBrand] = useState<string>("")
  const [approvalMarket, setApprovalMarket] = useState<string>("")
  const [isApproving, setIsApproving] = useState<boolean>(false)
  const [rejectModal, setRejectModal] = useState<{
    open: boolean
    request: SupplierRegistrationRequest | null
    reason: string
  }>({
    open: false,
    request: null,
    reason: "",
  })
  const [isRejecting, setIsRejecting] = useState<boolean>(false)
  const [lightbox, setLightbox] = useState<{ open: boolean; url: string; title: string }>({
    open: false,
    url: "",
    title: "",
  })

  // Synced detail request
  const activeDetailRequest = selectedRequestForDetails
    ? supplierRegistrationRequests.find((r) => String(r.id) === String(selectedRequestForDetails.id)) || selectedRequestForDetails
    : null

  const getPublicSupplierRegistrationUrl = () => {
    if (typeof window !== "undefined" && (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1")) {
      return "https://himatsms.web.app/#supplier-register"
    }
    return `${window.location.origin}/#supplier-register`
  }

  const buildSupplierInviteMessage = () => {
    const regUrl = getPublicSupplierRegistrationUrl()
    return `नमस्कार!\nहिम्मत टेक्सटाइल (Himat Textile) के साथ फैब्रिक मिल / सप्लायर के रूप में जुड़ने के लिए कृपया नीचे दिए गए लिंक पर अपनी मिल व व्यावसायिक जानकारी भरें:\n\n${regUrl}\n\nधन्यवाद!\nहिम्मत टेक्सटाइल, अहमदाबाद`
  }

  const [search, setSearch] = useState<string>("")
  const [showSearch, setShowSearch] = useState<boolean>(false)
  const [typeFilter, setTypeFilter] = useState<string>("all")

  // Master Detail Full Page State
  const [selectedSupplierId, setSelectedSupplierId] = useState<number | null>(null)

  // Modal States
  const [isDialogOpen, setIsDialogOpen] = useState<boolean>(false)
  const [activeFormTab, setActiveFormTab] = useState<string>("basic")

  // Quick inline creation dialogs
  const [isQuickMarketOpen, setIsQuickMarketOpen] = useState<boolean>(false)
  const [quickMarketName, setQuickMarketName] = useState<string>("")
  const [quickMarketCity, setQuickMarketCity] = useState<string>("Ahmedabad")

  const [isQuickBrandOpen, setIsQuickBrandOpen] = useState<boolean>(false)
  const [quickBrandName, setQuickBrandName] = useState<string>("")

  // Invoice / Report Modal State
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

  const handleOpenSupplierInvoice = (sup: Supplier) => {
    const supEntries = entries.filter(
      (e) =>
        Number(e.supplierId) === sup.id ||
        (e.supplierName && e.supplierName.toLowerCase() === sup.name.toLowerCase())
    )

    const visit: Visit = visits[0] || {
      id: Date.now(),
      visitCode: "HT-PO",
      customerId: 1,
      customerName: "All Buyers",
      date: new Date().toISOString().split("T")[0],
      employeeId: 1,
      employeeName: "Himat Textile",
      status: "Completed",
    }

    const reportData = {
      supplier: sup,
      visit,
      entries: supEntries,
      customer: customers.find((c) => c.id === visit.customerId) || customers[0],
    }

    const html = generateSupplierInvoiceHtml(reportData)
    const whatsAppText = buildSupplierInvoiceWhatsAppText(reportData)

    setReportModal({
      open: true,
      title: `Supplier Order Copy: ${sup.firmName || sup.name}`,
      html,
      whatsAppText,
    })
  }

  // Form State
  const [editingId, setEditingId] = useState<number | null>(null)
  const [supplierId, setSupplierId] = useState<string>("")
  const [name, setName] = useState<string>("")
  const [firmName, setFirmName] = useState<string>("")
  const [type, setType] = useState<string>("Manufacturer")
  const [selectedMarketName, setSelectedMarketName] = useState<string>("")
  const [selectedBrandName, setSelectedBrandName] = useState<string>("")
  const [contactPerson, setContactPerson] = useState<string>("")
  const [gstin, setGstin] = useState<string>("")
  const [panNumber, setPanNumber] = useState<string>("")
  const [city, setCity] = useState<string>("Ahmedabad")
  const [state, setState] = useState<string>("Gujarat")

  // Contacts (Up to 5)
  const [phone1, setPhone1] = useState<string>("")
  const [phone2, setPhone2] = useState<string>("")
  const [phone3, setPhone3] = useState<string>("")
  const [phone4, setPhone4] = useState<string>("")
  const [phone5, setPhone5] = useState<string>("")
  const [phoneCount, setPhoneCount] = useState<number>(1)
  const [email, setEmail] = useState<string>("")

  // Factories & Outlets (Up to 5)
  const [factories, setFactories] = useState<SupplierAddress[]>([
    { name: "Primary Mill / Factory 1", address: "", city: "Ahmedabad", pincode: "" }
  ])
  const [outlets, setOutlets] = useState<SupplierAddress[]>([
    { name: "Market Shop / Outlet 1", address: "", city: "Ahmedabad", pincode: "" }
  ])

  // Products & Pricing
  const [productsMade, setProductsMade] = useState<string>("")
  const [priceRange, setPriceRange] = useState<string>("")
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])
  const [customCategory, setCustomCategory] = useState<string>("")

  // KYC & Photos
  const [shopPhotoUri, setShopPhotoUri] = useState<string>("")
  const [visitingCardPhotoUri, setVisitingCardPhotoUri] = useState<string>("")
  const [referredBy, setReferredBy] = useState<string>("")
  const [notes, setNotes] = useState<string>("")

  // GST Lookup & auto-population state
  const [isFetchingGst, setIsFetchingGst] = useState<boolean>(false)
  const [gstFeedback, setGstFeedback] = useState<{
    type: "success" | "offline" | "error" | null
    message: string
  }>({ type: null, message: "" })

  const handleGstLookup = async (inputGst?: string) => {
    const rawGst = (inputGst !== undefined ? inputGst : gstin).trim().toUpperCase()
    if (!rawGst) {
      setGstFeedback({ type: null, message: "" })
      return
    }

    if (rawGst.length !== 15 || !isValidGstin(rawGst)) {
      setGstFeedback({
        type: "error",
        message: "Please enter a valid 15-character GSTIN (e.g. 24AAAAA0000A1Z5)",
      })
      return
    }

    setIsFetchingGst(true)
    setGstFeedback({ type: null, message: "" })

    try {
      const offlinePan = extractPanFromGstin(rawGst)
      const offlineState = getStateFromGstin(rawGst)

      setGstin(rawGst)
      if (offlinePan) setPanNumber(offlinePan)
      if (offlineState) setState(offlineState)

      const details = await fetchGstDetails(rawGst)
      if (details && (details.firmName || details.address || details.city || details.pincode)) {
        if (details.firmName) {
          setFirmName(details.firmName)
          setName(details.firmName)
        }
        if (details.city) setCity(details.city)
        if (details.state) setState(details.state)
        if (details.pan) setPanNumber(details.pan)
        if (details.address) {
          setFactories((prev) => {
            const updated = [...prev]
            if (updated.length > 0 && (!updated[0].address || updated[0].address.trim() === "")) {
              updated[0] = {
                ...updated[0],
                address: details.address || updated[0].address,
                city: details.city || updated[0].city,
                pincode: details.pincode || updated[0].pincode,
              }
            }
            return updated
          })
        }
        setGstFeedback({
          type: "success",
          message: "✓ Mill details & address retrieved from GSTIN",
        })
      } else {
        setGstFeedback({
          type: "offline",
          message: "✓ State & PAN auto-detected from GSTIN. Please enter Mill Name below.",
        })
      }
    } catch (err) {
      console.warn("GST lookup error in supplier dialog:", err)
      const offlinePan = extractPanFromGstin(rawGst)
      const offlineState = getStateFromGstin(rawGst)
      if (offlinePan) setPanNumber(offlinePan)
      if (offlineState) setState(offlineState)
      setGstFeedback({
        type: "offline",
        message: "✓ State & PAN auto-detected from GSTIN.",
      })
    } finally {
      setIsFetchingGst(false)
    }
  }

  const handleClearGst = () => {
    setGstin("")
    setGstFeedback({ type: null, message: "" })
  }

  // Draft state (strictly local browser storage)
  const [hasDraft, setHasDraft] = useState<boolean>(false)

  // Open Add Dialog
  const handleOpenAdd = () => {
    setEditingId(null)
    const draft = getMasterDraft<any>("supplier")
    if (draft) {
      setSupplierId(draft.supplierId || `SUP-${Math.floor(100 + Math.random() * 900)}`)
      setName(draft.name || draft.firmName || "")
      setFirmName(draft.firmName || "")
      setType(draft.type || "Manufacturer")
      setSelectedMarketName(draft.selectedMarketName || (markets[0]?.marketName || "Maskati Cloth Market"))
      setSelectedBrandName(draft.selectedBrandName || "")
      setContactPerson(draft.contactPerson || "")
      setGstin(draft.gstin || "")
      setPanNumber(draft.panNumber || "")
      setCity(draft.city || "Ahmedabad")
      setState(draft.state || "Gujarat")
      setPhone1(draft.phone1 || "")
      setPhone2(draft.phone2 || "")
      setPhone3(draft.phone3 || "")
      setPhone4(draft.phone4 || "")
      setPhone5(draft.phone5 || "")
      setPhoneCount(draft.phoneCount || 1)
      setEmail(draft.email || "")
      setFactories(draft.factories && draft.factories.length > 0 ? draft.factories : [{ name: "Main Factory", address: "", city: "Ahmedabad", pincode: "" }])
      setOutlets(draft.outlets && draft.outlets.length > 0 ? draft.outlets : [{ name: "Market Outlet", address: "", city: "Ahmedabad", pincode: "" }])
      setProductsMade(draft.productsMade || "")
      setPriceRange(draft.priceRange || "₹250 - ₹1200")
      setSelectedCategories(draft.selectedCategories || [])
      setCustomCategory("")
      setShopPhotoUri("")
      setVisitingCardPhotoUri("")
      setReferredBy(draft.referredBy || "")
      setNotes(draft.notes || "")
      setHasDraft(true)
    } else {
      setSupplierId(`SUP-${Math.floor(100 + Math.random() * 900)}`)
      setName("")
      setFirmName("")
      setType("Manufacturer")
      setSelectedMarketName(markets[0]?.marketName || "Maskati Cloth Market")
      setSelectedBrandName(brands[0]?.brandName || "")
      setContactPerson("")
      setGstin("")
      setPanNumber("")
      setCity("Ahmedabad")
      setState("Gujarat")
      setPhone1("")
      setPhone2("")
      setPhone3("")
      setPhone4("")
      setPhone5("")
      setPhoneCount(1)
      setEmail("")
      setFactories([{ name: "Main Factory", address: "", city: "Ahmedabad", pincode: "" }])
      setOutlets([{ name: "Market Outlet", address: "", city: "Ahmedabad", pincode: "" }])
      setProductsMade("")
      setPriceRange("₹250 - ₹1200")
      setSelectedCategories([])
      setCustomCategory("")
      setShopPhotoUri("")
      setVisitingCardPhotoUri("")
      setReferredBy("")
      setNotes("")
      setHasDraft(false)
    }
    setActiveFormTab("basic")
    setIsDialogOpen(true)
  }

  const handleDiscardDraft = () => {
    clearMasterDraft("supplier")
    setHasDraft(false)
    setSupplierId(`SUP-${Math.floor(100 + Math.random() * 900)}`)
    setName("")
    setFirmName("")
    setType("Manufacturer")
    setSelectedMarketName(markets[0]?.marketName || "Maskati Cloth Market")
    setSelectedBrandName(brands[0]?.brandName || "")
    setContactPerson("")
    setGstin("")
    setPanNumber("")
    setCity("Ahmedabad")
    setState("Gujarat")
    setPhone1("")
    setPhone2("")
    setPhone3("")
    setPhone4("")
    setPhone5("")
    setPhoneCount(1)
    setEmail("")
    setFactories([{ name: "Main Factory", address: "", city: "Ahmedabad", pincode: "" }])
    setOutlets([{ name: "Market Outlet", address: "", city: "Ahmedabad", pincode: "" }])
    setProductsMade("")
    setPriceRange("₹250 - ₹1200")
    setSelectedCategories([])
    setCustomCategory("")
    setReferredBy("")
    setNotes("")
  }

  // Auto-save local draft
  useEffect(() => {
    if (!isDialogOpen || editingId !== null) return
    if (firmName.trim() || contactPerson.trim() || gstin.trim() || phone1.trim()) {
      saveMasterDraft("supplier", {
        supplierId,
        name,
        firmName,
        type,
        selectedMarketName,
        selectedBrandName,
        contactPerson,
        gstin,
        panNumber,
        city,
        state,
        phone1,
        phone2,
        phone3,
        phone4,
        phone5,
        phoneCount,
        email,
        factories,
        outlets,
        productsMade,
        priceRange,
        selectedCategories,
        referredBy,
        notes,
      })
    }
  }, [
    isDialogOpen,
    editingId,
    supplierId,
    name,
    firmName,
    type,
    selectedMarketName,
    selectedBrandName,
    contactPerson,
    gstin,
    panNumber,
    city,
    state,
    phone1,
    phone2,
    phone3,
    phone4,
    phone5,
    phoneCount,
    email,
    factories,
    outlets,
    productsMade,
    priceRange,
    selectedCategories,
    referredBy,
    notes,
  ])

  // Open Edit Dialog
  const handleOpenEdit = (s: Supplier) => {
    setEditingId(s.id)
    setSupplierId(s.supplierId || `SUP-${s.id}`)
    setName(s.name || s.firmName || "")
    setFirmName(s.firmName || s.name || "")
    setType(s.type || "Manufacturer")
    setSelectedMarketName(s.marketName || s.marketArea || (markets[0]?.marketName || ""))
    setSelectedBrandName(s.brand || "")
    setContactPerson(s.contactPerson || "")
    setGstin(s.gstin || s.gstNumber || "")
    setPanNumber(s.panNumber || "")
    setCity(s.city || "Ahmedabad")
    setState(s.state || "Gujarat")

    setPhone1(s.phone || "")
    setPhone2(s.phone2 || "")
    setPhone3(s.phone3 || "")
    setPhone4(s.phone4 || "")
    setPhone5(s.phone5 || "")
    const count = [s.phone, s.phone2, s.phone3, s.phone4, s.phone5].filter(Boolean).length
    setPhoneCount(Math.max(1, count))
    setEmail(s.email || "")

    // Factories
    if (s.factories && s.factories.length > 0) {
      setFactories(s.factories.slice(0, 5))
    } else {
      setFactories([
        {
          name: "Main Factory",
          address: s.address || "",
          city: s.city || "Ahmedabad",
          pincode: s.pincode || "",
        },
      ])
    }

    // Outlets
    if (s.outlets && s.outlets.length > 0) {
      setOutlets(s.outlets.slice(0, 5))
    } else {
      setOutlets([
        {
          name: "Market Outlet",
          address: s.officeAddress || "",
          city: s.city || "Ahmedabad",
          pincode: s.pincode || "",
        },
      ])
    }

    setProductsMade(s.productsMade || s.garmentTypes || "")
    setPriceRange(s.priceRange || "")

    const cats = (s.categories || s.garmentTypes || "")
      .split(",")
      .map((c) => c.trim())
      .filter(Boolean)
    setSelectedCategories(cats)
    setCustomCategory("")

    setShopPhotoUri(s.shopPhotoUri || "")
    setVisitingCardPhotoUri(s.visitingCardPhotoUri || "")
    setReferredBy(s.referredBy || "")
    setNotes(s.notes || "")

    setActiveFormTab("basic")
    setIsDialogOpen(true)
  }

  const handleToggleCategory = (cat: string) => {
    if (selectedCategories.includes(cat)) {
      setSelectedCategories(selectedCategories.filter((c) => c !== cat))
    } else {
      setSelectedCategories([...selectedCategories, cat])
    }
  }

  const handleSave = async () => {
    const finalFirmName = firmName.trim() || name.trim()
    const finalContactPerson = contactPerson.trim() || name.trim()
    if (!finalFirmName) return

    const id = editingId || Date.now()
    const allPhones = [phone1.trim(), phone2.trim(), phone3.trim(), phone4.trim(), phone5.trim()].filter(Boolean)
    const primaryPhone = allPhones[0] || ""

    const allCats = [...selectedCategories]
    if (customCategory.trim() && !allCats.includes(customCategory.trim())) {
      allCats.push(customCategory.trim())
    }

    const payload: Supplier = {
      id,
      supplierId: supplierId.trim() || `SUP-${id % 10000}`,
      name: finalFirmName,
      firmName: finalFirmName,
      type,
      marketArea: selectedMarketName,
      marketName: selectedMarketName,
      brand: selectedBrandName,
      contactPerson: finalContactPerson,
      phone: primaryPhone,
      phone2: phone2.trim(),
      phone3: phone3.trim(),
      phone4: phone4.trim(),
      phone5: phone5.trim(),
      phones: allPhones,
      email: email.trim(),
      city: city.trim() || "Ahmedabad",
      state: state.trim() || "Gujarat",
      factories: factories.filter((f) => f.address.trim() || f.name.trim()),
      outlets: outlets.filter((o) => o.address.trim() || o.name.trim()),
      address: factories[0]?.address || outlets[0]?.address || "",
      officeAddress: outlets[0]?.address || "",
      productsMade: productsMade.trim(),
      priceRange: priceRange.trim(),
      categories: allCats.join(", "),
      garmentTypes: allCats.join(", "),
      gstin: gstin.trim().toUpperCase(),
      gstNumber: gstin.trim().toUpperCase(),
      panNumber: panNumber.trim().toUpperCase() || (gstin.length === 15 ? gstin.slice(2, 12) : ""),
      shopPhotoUri: shopPhotoUri.trim(),
      visitingCardPhotoUri: visitingCardPhotoUri.trim(),
      referredBy: referredBy.trim(),
      notes: notes.trim(),
      defaultCaseSize: 24, // Maintained internally for seamless order case math
      createdAt: editingId ? (suppliers.find((s) => s.id === editingId)?.createdAt || Date.now()) : Date.now(),
    }

    await saveSupplier(payload)
    clearMasterDraft("supplier")
    setHasDraft(false)
    setIsDialogOpen(false)
  }

  const handleDelete = async (id: number) => {
    if (window.confirm("Are you sure you want to remove this supplier/mill master record?")) {
      await deleteSupplier(id)
      if (selectedSupplierId === id) {
        setSelectedSupplierId(null)
      }
    }
  }

  const handleExportTally = () => {
    const xml = generateSuppliersTallyXml(suppliers)
    const today = new Date().toISOString().slice(0, 10)
    downloadXmlFile(xml, `Himat_Suppliers_Tally_Import_${today}.xml`)
  }

  const handleQuickCreateMarket = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!quickMarketName.trim()) return
    const newMarket = {
      id: Date.now(),
      marketName: quickMarketName.trim(),
      city: quickMarketCity.trim(),
      createdAt: Date.now(),
    }
    await saveMarket(newMarket)
    setSelectedMarketName(newMarket.marketName)
    setIsQuickMarketOpen(false)
    setQuickMarketName("")
  }

  const handleQuickCreateBrand = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!quickBrandName.trim()) return
    const newBrand = {
      id: Date.now(),
      brandName: quickBrandName.trim(),
      manufacturerName: firmName || name,
      createdAt: Date.now(),
    }
    await saveBrand(newBrand)
    setSelectedBrandName(newBrand.brandName)
    setIsQuickBrandOpen(false)
    setQuickBrandName("")
  }

  // Filter
  const q = search.trim().toLowerCase()
  const filteredSuppliers = suppliers.filter((s) => {
    const matchesQuery =
      !q ||
      s.name?.toLowerCase().includes(q) ||
      s.firmName?.toLowerCase().includes(q) ||
      s.brand?.toLowerCase().includes(q) ||
      s.marketArea?.toLowerCase().includes(q) ||
      s.marketName?.toLowerCase().includes(q) ||
      s.contactPerson?.toLowerCase().includes(q) ||
      s.phone?.includes(q) ||
      s.gstin?.toLowerCase().includes(q) ||
      s.productsMade?.toLowerCase().includes(q)

    const matchesType =
      typeFilter === "all" || s.type?.toLowerCase() === typeFilter.toLowerCase()

    return matchesQuery && matchesType
  })

  // Filtered Supplier Registration Requests
  const filteredSupplierRequests = supplierRegistrationRequests.filter((r) => {
    if (!r) return false
    const matchesStatus =
      requestFilterStatus === "ALL" ||
      r.status?.toUpperCase() === requestFilterStatus

    const matchesSearch =
      !q ||
      r.firmName?.toLowerCase().includes(q) ||
      r.name?.toLowerCase().includes(q) ||
      r.contactPerson?.toLowerCase().includes(q) ||
      r.phone?.includes(q) ||
      r.marketArea?.toLowerCase().includes(q) ||
      r.city?.toLowerCase().includes(q) ||
      r.productsMade?.toLowerCase().includes(q) ||
      r.gstin?.toLowerCase().includes(q)

    return matchesStatus && matchesSearch
  })

  // Action Handlers for Supplier Registration Requests
  const handleOpenApprovalDialog = (req: SupplierRegistrationRequest) => {
    setSelectedRequestForApproval(req)
    setApprovalBrand(req.brand || "")
    setApprovalMarket(req.marketArea || "")
  }

  const handleConfirmApproval = async () => {
    if (!selectedRequestForApproval) return
    setIsApproving(true)
    try {
      await approveSupplierRegistrationRequest(selectedRequestForApproval.id, {
        brand: approvalBrand.trim(),
        marketName: approvalMarket.trim(),
        fallbackRequest: selectedRequestForApproval,
      })
      setSelectedRequestForApproval(null)
      setSelectedRequestForDetails(null)
    } catch (err: any) {
      console.error("Supplier approval error:", err)
      alert(err.message || "Failed to approve supplier registration request")
    } finally {
      setIsApproving(false)
    }
  }

  const handleOpenRejectDialog = (req: SupplierRegistrationRequest) => {
    setRejectModal({ open: true, request: req, reason: "" })
  }

  const handleConfirmReject = async () => {
    if (!rejectModal.request) return
    setIsRejecting(true)
    try {
      await rejectSupplierRegistrationRequest(rejectModal.request.id, rejectModal.reason, rejectModal.request.phone)
      setRejectModal({ open: false, request: null, reason: "" })
      setSelectedRequestForDetails(null)
    } catch (err: any) {
      console.error("Supplier reject error:", err)
      alert(err.message || "Failed to reject supplier registration request")
    } finally {
      setIsRejecting(false)
    }
  }

  const handleDeleteRequest = async (requestId: string, fallbackPhone?: string) => {
    if (!confirm("Are you sure you want to permanently delete this supplier registration request?")) return
    try {
      await deleteSupplierRegistrationRequest(requestId, fallbackPhone)
      if (selectedRequestForDetails?.id === requestId) {
        setSelectedRequestForDetails(null)
      }
    } catch (err: any) {
      console.error("Delete request error:", err)
      alert(err.message || "Failed to delete supplier registration request")
    }
  }

  return (
    <div className="space-y-6">
      {selectedSupplierId !== null ? (
        <SupplierDetailView
          supplierId={selectedSupplierId}
          onBack={() => setSelectedSupplierId(null)}
          onEdit={(sup) => handleOpenEdit(sup)}
        />
      ) : (
        <>
          {/* Top Header */}
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
                  Suppliers & Textile Mills Master
                </h2>
                <Badge variant="outline" className="text-xs bg-amber-500/10 text-amber-700 border-amber-500/20 font-semibold">
                  {suppliers.length} Suppliers
                </Badge>
              </div>
              <p className="text-xs text-muted-foreground mt-0.5">
                Fabric mills, wholesale suppliers, manufacturing factories & visiting cards.
              </p>
            </div>

            <div className="flex items-center gap-2 flex-wrap">
              <Button
                variant="outline"
                size="sm"
                onClick={handleExportTally}
                className="h-8 px-3 text-xs gap-1.5 border-emerald-600/30 text-emerald-700 hover:bg-emerald-50 dark:text-emerald-400 font-medium"
                title="Export all supplier ledgers to Tally Prime / ERP 9 XML"
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
                className="h-8 px-3 text-xs gap-1.5 border-amber-500/40 text-amber-700 dark:text-amber-300 hover:bg-amber-50 dark:hover:bg-amber-950/40 font-semibold"
                title="Share supplier registration link with mills & manufacturers"
              >
                <Share2 className="h-3.5 w-3.5 text-amber-600 dark:text-amber-400" />
                <span>Invite Supplier</span>
              </Button>

              <Button
                size="sm"
                onClick={handleOpenAdd}
                className="h-8 px-3 text-xs font-semibold shadow-sm bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 gap-1"
              >
                <Plus className="h-3.5 w-3.5" />
                <span>New Supplier</span>
              </Button>
            </div>
          </div>

          {/* Navigation Tabs: Active Suppliers vs Registration Requests */}
          <div className="flex items-center gap-2 border-b border-zinc-200 dark:border-zinc-800">
            <button
              type="button"
              onClick={() => setViewMode("suppliers")}
              className={`flex items-center gap-2 py-2.5 px-3 border-b-2 font-medium text-xs transition-all ${
                viewMode === "suppliers"
                  ? "border-zinc-900 text-zinc-900 dark:border-zinc-100 dark:text-zinc-100 font-bold"
                  : "border-transparent text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300"
              }`}
            >
              <Factory className="h-4 w-4" />
              <span>Active Suppliers ({suppliers.length})</span>
            </button>

            <button
              type="button"
              onClick={() => setViewMode("requests")}
              className={`flex items-center gap-2 py-2.5 px-3 border-b-2 font-medium text-xs transition-all ${
                viewMode === "requests"
                  ? "border-zinc-900 text-zinc-900 dark:border-zinc-100 dark:text-zinc-100 font-bold"
                  : "border-transparent text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300"
              }`}
            >
              <Building2 className="h-4 w-4" />
              <span>Registration Requests ({supplierRegistrationRequests.length})</span>
              {pendingSupplierRegistrationRequestsCount > 0 && (
                <span className="flex h-4 min-w-[16px] px-1 items-center justify-center rounded-full bg-amber-500 text-[10px] font-bold text-white shadow-xs animate-pulse">
                  {pendingSupplierRegistrationRequestsCount}
                </span>
              )}
            </button>
          </div>

          {viewMode === "suppliers" ? (
            <>
              {/* Search Input Bar */}
              {showSearch && (
                <Card className="p-3 bg-zinc-50/70 dark:bg-zinc-900/70 border-zinc-200/80">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Search by firm name, contact, brand, market area, GSTIN, products..."
                  className="pl-9 pr-8 text-xs h-9 bg-white dark:bg-zinc-950"
                  autoFocus
                />
                {search && (
                  <button
                    onClick={() => setSearch("")}
                    className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-zinc-800"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>
            </Card>
          )}

          {/* Supplier Type Filter Tabs */}
          <div className="flex gap-2 border-b border-zinc-200 dark:border-zinc-800 pb-2 overflow-x-auto text-xs">
            {[
              { id: "all", label: "All Suppliers" },
              { id: "Manufacturer", label: "Fabric Mills / Manufacturers" },
              { id: "Wholesaler", label: "Wholesalers / Traders" },
              { id: "Processor", label: "Dyeing & Processors" },
              { id: "Jobworker", label: "Job Workers" },
            ].map((t) => (
              <button
                key={t.id}
                onClick={() => setTypeFilter(t.id)}
                className={`px-3 py-1.5 rounded-lg font-medium whitespace-nowrap transition-colors ${
                  typeFilter === t.id
                    ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
                    : "text-zinc-600 dark:text-zinc-400 hover:bg-zinc-100 dark:hover:bg-zinc-800"
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>

          {/* Suppliers Master List Table */}
          {filteredSuppliers.length === 0 ? (
            <div className="p-12 text-center border border-dashed rounded-2xl bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800">
              <Building2 className="h-10 w-10 text-muted-foreground mx-auto mb-3 opacity-60" />
              <h3 className="font-semibold text-sm">No suppliers found</h3>
              <p className="text-xs text-muted-foreground max-w-sm mx-auto mt-1">
                {search ? "No mills match your filter criteria." : "Start registering fabric suppliers & mills."}
              </p>
              <Button onClick={handleOpenAdd} variant="outline" size="sm" className="mt-4 text-xs gap-1.5">
                <Plus className="h-3.5 w-3.5" />
                Add First Supplier
              </Button>
            </div>
          ) : (
            <div className="overflow-hidden rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-xs">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="border-b border-zinc-200 dark:border-zinc-800 bg-zinc-50/80 dark:bg-zinc-900/80 text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
                    <tr>
                      <th className="py-3 px-4">Supplier / Mill</th>
                      <th className="py-3 px-4">Type</th>
                      <th className="py-3 px-4">Contact Person</th>
                      <th className="py-3 px-4">Market & Location</th>
                      <th className="py-3 px-4">Brand / Range</th>
                      <th className="py-3 px-4">Phone & GST</th>
                      <th className="py-3 px-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                    {filteredSuppliers.map((sup) => (
                      <tr
                        key={sup.id}
                        onClick={() => setSelectedSupplierId(sup.id)}
                        className="hover:bg-amber-50/40 dark:hover:bg-amber-950/20 cursor-pointer transition-colors group"
                      >
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-2.5">
                            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-amber-500/10 text-amber-700 font-bold text-sm dark:bg-amber-500/20 dark:text-amber-400 group-hover:bg-amber-600 group-hover:text-white transition-colors">
                              {(sup.firmName || sup.name || "S")[0].toUpperCase()}
                            </div>
                            <div className="min-w-0">
                              <div className="font-bold text-zinc-900 dark:text-zinc-100 group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
                                {sup.firmName || sup.name}
                              </div>
                              {sup.supplierId && (
                                <div className="font-mono text-[10px] text-zinc-400 font-semibold">
                                  {sup.supplierId}
                                </div>
                              )}
                            </div>
                          </div>
                        </td>
                        <td className="py-3 px-4 whitespace-nowrap">
                          <Badge variant="outline" className="text-[10px] uppercase font-bold text-amber-700 border-amber-200 dark:border-amber-800 dark:text-amber-300">
                            {sup.type || "Supplier"}
                          </Badge>
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-1.5 text-zinc-700 dark:text-zinc-300 font-medium">
                            <User className="h-3 w-3 text-zinc-400 shrink-0" />
                            <span>{sup.contactPerson || "—"}</span>
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <div className="space-y-0.5">
                            <div className="flex items-center gap-1.5 font-medium text-amber-800 dark:text-amber-400">
                              <Compass className="h-3 w-3 text-amber-600 shrink-0" />
                              <span className="truncate">{sup.marketName || sup.marketArea || "Ahmedabad Market"}</span>
                            </div>
                            {sup.city && (
                              <div className="text-[11px] text-zinc-400 flex items-center gap-1">
                                <MapPin className="h-2.5 w-2.5 text-zinc-400" />
                                <span>{sup.city}{sup.state ? `, ${sup.state}` : ""}</span>
                              </div>
                            )}
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <div className="space-y-0.5">
                            {sup.brand ? (
                              <div className="flex items-center gap-1 text-zinc-700 dark:text-zinc-300 font-semibold text-[11px]">
                                <Tag className="h-3 w-3 text-zinc-400 shrink-0" />
                                <span>{sup.brand}</span>
                              </div>
                            ) : (
                              <span className="text-zinc-400">—</span>
                            )}
                            {sup.priceRange && (
                              <div className="text-[10px] text-emerald-600 dark:text-emerald-400 font-medium">
                                Range: {sup.priceRange}
                              </div>
                            )}
                          </div>
                        </td>
                        <td className="py-3 px-4 whitespace-nowrap">
                          <div className="space-y-0.5">
                            {sup.phone ? (
                              <div className="flex items-center gap-1.5 font-medium text-zinc-800 dark:text-zinc-200">
                                <Phone className="h-3 w-3 text-zinc-400 shrink-0" />
                                <span>{sup.phone}</span>
                              </div>
                            ) : null}
                            {sup.gstin ? (
                              <div className="font-mono text-[10px] text-zinc-400">
                                {sup.gstin}
                              </div>
                            ) : null}
                          </div>
                        </td>
                        <td className="py-3 px-4 text-right whitespace-nowrap">
                          <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => handleOpenSupplierInvoice(sup)}
                              className="h-7 text-xs px-2 text-amber-700 dark:text-amber-400 font-medium"
                              title="Generate Purchase Order Copy"
                            >
                              <Printer className="h-3 w-3 mr-1" />
                              PO Copy
                            </Button>
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => setSelectedSupplierId(sup.id)}
                              className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-900"
                              title="View Full Profile"
                            >
                              <Eye className="h-3.5 w-3.5" />
                            </Button>
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => handleOpenEdit(sup)}
                              className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-900"
                              title="Edit Supplier"
                            >
                              <Edit2 className="h-3.5 w-3.5" />
                            </Button>
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => handleDelete(sup.id)}
                              className="h-7 w-7 p-0 text-red-500 hover:text-red-700"
                              title="Delete Supplier"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      ) : (
            /* ============================================================== */
            /* VIEW MODE: REGISTRATION REQUESTS                               */
            /* ============================================================== */
            <div className="space-y-4">
              {/* Filter Tabs for Requests */}
              <div className="flex items-center justify-between gap-4 flex-wrap">
                <div className="flex gap-2 border-b border-zinc-200 dark:border-zinc-800 pb-2 overflow-x-auto text-xs">
                  {[
                    { id: "ALL", label: `All Requests (${supplierRegistrationRequests.length})` },
                    { id: "PENDING", label: `Pending Review (${pendingSupplierRegistrationRequestsCount})` },
                    { id: "APPROVED", label: `Approved (${supplierRegistrationRequests.filter((r) => r.status === "APPROVED").length})` },
                    { id: "REJECTED", label: `Rejected (${supplierRegistrationRequests.filter((r) => r.status === "REJECTED").length})` },
                  ].map((tab) => (
                    <button
                      key={tab.id}
                      type="button"
                      onClick={() => setRequestFilterStatus(tab.id as any)}
                      className={`px-3 py-1.5 rounded-lg font-medium whitespace-nowrap transition-colors ${
                        requestFilterStatus === tab.id
                          ? "bg-amber-600 text-white shadow-sm font-bold"
                          : "text-zinc-600 dark:text-zinc-400 hover:bg-zinc-100 dark:hover:bg-zinc-800"
                      }`}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setIsShareLinkModalOpen(true)}
                  className="h-8 px-3 text-xs gap-1.5 border-amber-500/40 text-amber-700 dark:text-amber-300 hover:bg-amber-50"
                >
                  <Share2 className="h-3.5 w-3.5 text-amber-600" />
                  <span>Copy / Share Invite Link</span>
                </Button>
              </div>

              {/* Table of Requests */}
              {filteredSupplierRequests.length === 0 ? (
                <div className="p-12 text-center border border-dashed rounded-2xl bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800">
                  <Building2 className="h-10 w-10 text-muted-foreground mx-auto mb-3 opacity-60" />
                  <h3 className="font-semibold text-sm">No supplier requests found</h3>
                  <p className="text-xs text-muted-foreground max-w-sm mx-auto mt-1">
                    Share your public invite link with prospective textile mills & suppliers.
                  </p>
                  <Button
                    onClick={() => setIsShareLinkModalOpen(true)}
                    variant="outline"
                    size="sm"
                    className="mt-4 text-xs gap-1.5 border-amber-500/40 text-amber-700"
                  >
                    <Share2 className="h-3.5 w-3.5 text-amber-600" />
                    Share Registration Link
                  </Button>
                </div>
              ) : (
                <div className="overflow-hidden rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-xs">
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs">
                      <thead className="border-b border-zinc-200 dark:border-zinc-800 bg-zinc-50/80 dark:bg-zinc-900/80 text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
                        <tr>
                          <th className="py-3 px-4">Mill / Firm & Contact</th>
                          <th className="py-3 px-4">Classification</th>
                          <th className="py-3 px-4">Phone & WhatsApp</th>
                          <th className="py-3 px-4">Market & Location</th>
                          <th className="py-3 px-4">Fabrics / Products</th>
                          <th className="py-3 px-4">Status</th>
                          <th className="py-3 px-4 text-right">Actions</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                        {filteredSupplierRequests.map((req) => {
                          const isPending = req.status === "PENDING"
                          const isApproved = req.status === "APPROVED"
                          const isRejected = req.status === "REJECTED"

                          return (
                            <tr
                              key={req.id}
                              onClick={() => setSelectedRequestForDetails(req)}
                              className="hover:bg-amber-50/40 dark:hover:bg-amber-950/20 cursor-pointer transition-colors group"
                            >
                              <td className="py-3 px-4">
                                <div className="flex items-center gap-2.5">
                                  <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-amber-500/10 text-amber-700 font-bold text-sm dark:bg-amber-500/20 dark:text-amber-400 group-hover:bg-amber-600 group-hover:text-white transition-colors">
                                    {(req.firmName || req.name || "S")[0].toUpperCase()}
                                  </div>
                                  <div className="min-w-0">
                                    <p className="font-bold text-sm text-zinc-900 dark:text-zinc-50 group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors truncate">
                                      {req.firmName || req.name}
                                    </p>
                                    <p className="text-[11px] text-muted-foreground flex items-center gap-1.5 truncate">
                                      <User className="h-3 w-3 shrink-0" />
                                      <span>{req.contactPerson || req.name || "Proprietor"}</span>
                                      <span className="font-mono text-[10px] text-zinc-400 font-semibold">
                                        • {req.id}
                                      </span>
                                    </p>
                                  </div>
                                </div>
                              </td>

                              <td className="py-3 px-4 whitespace-nowrap">
                                <Badge variant="outline" className="text-[10px] uppercase font-bold text-amber-700 border-amber-200 dark:border-amber-800">
                                  {req.type || "Manufacturer"}
                                </Badge>
                              </td>

                              <td className="py-3 px-4 whitespace-nowrap">
                                <div className="flex items-center gap-2">
                                  <span className="font-mono font-semibold text-zinc-800 dark:text-zinc-200">
                                    {req.phone}
                                  </span>
                                  {req.phone && (
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
                                  )}
                                </div>
                              </td>

                              <td className="py-3 px-4">
                                <p className="font-medium text-zinc-800 dark:text-zinc-200">
                                  {req.marketArea || req.city || "Ahmedabad"}
                                </p>
                                <p className="text-[11px] text-muted-foreground truncate">
                                  {req.city ? `${req.city}, ${req.state || "Gujarat"}` : req.address}
                                </p>
                              </td>

                              <td className="py-3 px-4">
                                <p className="text-xs text-zinc-700 dark:text-zinc-300 truncate max-w-[200px]">
                                  {req.productsMade || req.categories || "—"}
                                </p>
                                {req.priceRange && (
                                  <span className="text-[10px] text-emerald-600 font-medium">
                                    Range: {req.priceRange}
                                  </span>
                                )}
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
                                    {req.createdSupplierId && (
                                      <span className="font-mono ml-0.5">#{req.createdSupplierId}</span>
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
                                    className="h-7 px-2.5 text-xs font-semibold text-amber-700 dark:text-amber-400 hover:bg-amber-50 gap-1 border-amber-200"
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
                                        className="h-7 px-2 text-xs text-rose-600 border-rose-200 hover:bg-rose-50"
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

          {/* SLIDE-OVER DRAWER FOR REQUEST DETAILS */}
          {activeDetailRequest && (
            <div className="fixed inset-0 z-50 overflow-hidden animate-in fade-in duration-200">
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
                          <Factory className="h-5 w-5 text-amber-600 shrink-0" />
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
                      <p className="text-xs text-muted-foreground flex items-center gap-2">
                        <span>Submitted on {new Date(activeDetailRequest.createdAt).toLocaleDateString()}</span>
                        <span>•</span>
                        <span>{activeDetailRequest.type || "Manufacturer"}</span>
                      </p>
                    </div>

                    <button
                      onClick={() => setSelectedRequestForDetails(null)}
                      className="p-2 rounded-xl text-zinc-400 hover:text-zinc-600 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
                    >
                      <X className="h-5 w-5" />
                    </button>
                  </div>

                  {/* Drawer Content */}
                  <div className="flex-1 overflow-y-auto p-6 space-y-6 text-xs">
                    {/* Quick Contact & Map Actions */}
                    <div className="flex flex-wrap gap-2">
                      {activeDetailRequest.phone && (
                        <a
                          href={`tel:${activeDetailRequest.phone}`}
                          className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300 font-bold hover:bg-indigo-100 transition-colors"
                        >
                          <Phone className="h-3.5 w-3.5" />
                          <span>Call: {activeDetailRequest.phone}</span>
                        </a>
                      )}
                      {activeDetailRequest.phone && (
                        <a
                          href={`https://wa.me/91${activeDetailRequest.phone.replace(/\D/g, "").slice(-10)}`}
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 font-bold hover:bg-emerald-100 transition-colors"
                        >
                          <MessageSquare className="h-3.5 w-3.5" />
                          <span>WhatsApp</span>
                        </a>
                      )}
                      {activeDetailRequest.mapLink && (
                        <a
                          href={activeDetailRequest.mapLink}
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 font-bold hover:bg-zinc-200 transition-colors"
                        >
                          <MapPin className="h-3.5 w-3.5" />
                          <span>Google Maps</span>
                        </a>
                      )}
                    </div>

                    {/* Section 1: Business Profile */}
                    <div className="space-y-3 p-4 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/40 dark:bg-zinc-900/40">
                      <h4 className="font-bold text-xs uppercase tracking-wider text-zinc-500 flex items-center gap-1.5">
                        <Building2 className="h-3.5 w-3.5" />
                        <span>Business & Proprietor Profile</span>
                      </h4>
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <span className="text-zinc-400 block text-[11px]">Mill / Firm Name</span>
                          <span className="font-bold text-zinc-900 dark:text-zinc-100 text-xs">
                            {activeDetailRequest.firmName || activeDetailRequest.name}
                          </span>
                        </div>
                        <div>
                          <span className="text-zinc-400 block text-[11px]">Contact Person / Owner</span>
                          <span className="font-semibold text-zinc-800 dark:text-zinc-200 text-xs">
                            {activeDetailRequest.contactPerson || activeDetailRequest.name}
                          </span>
                        </div>
                        <div>
                          <span className="text-zinc-400 block text-[11px]">Classification</span>
                          <span className="font-semibold text-amber-600 dark:text-amber-400 text-xs">
                            {activeDetailRequest.type}
                          </span>
                        </div>
                        <div>
                          <span className="text-zinc-400 block text-[11px]">Brand Name</span>
                          <span className="font-medium text-zinc-800 dark:text-zinc-200 text-xs">
                            {activeDetailRequest.brand || "—"}
                          </span>
                        </div>
                        {activeDetailRequest.email && (
                          <div className="col-span-2">
                            <span className="text-zinc-400 block text-[11px]">Email</span>
                            <span className="font-medium text-zinc-800 dark:text-zinc-200 text-xs">
                              {activeDetailRequest.email}
                            </span>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Section 2: Location & Addresses */}
                    <div className="space-y-3 p-4 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/40 dark:bg-zinc-900/40">
                      <h4 className="font-bold text-xs uppercase tracking-wider text-zinc-500 flex items-center gap-1.5">
                        <MapPin className="h-3.5 w-3.5" />
                        <span>Location & Market Hub</span>
                      </h4>
                      <div className="grid grid-cols-2 gap-3">
                        <div className="col-span-2">
                          <span className="text-zinc-400 block text-[11px]">Market Area / Cluster</span>
                          <span className="font-bold text-amber-800 dark:text-amber-300 text-xs">
                            {activeDetailRequest.marketArea || "Ahmedabad Market"}
                          </span>
                        </div>
                        <div className="col-span-2">
                          <span className="text-zinc-400 block text-[11px]">Mill / Factory Address</span>
                          <span className="font-medium text-zinc-800 dark:text-zinc-200 text-xs">
                            {activeDetailRequest.address || "—"}
                          </span>
                        </div>
                        {activeDetailRequest.officeAddress && (
                          <div className="col-span-2">
                            <span className="text-zinc-400 block text-[11px]">Market Office Address</span>
                            <span className="font-medium text-zinc-800 dark:text-zinc-200 text-xs">
                              {activeDetailRequest.officeAddress}
                            </span>
                          </div>
                        )}
                        <div>
                          <span className="text-zinc-400 block text-[11px]">City, State</span>
                          <span className="font-medium text-zinc-800 dark:text-zinc-200 text-xs">
                            {activeDetailRequest.city}, {activeDetailRequest.state || "Gujarat"}
                          </span>
                        </div>
                        <div>
                          <span className="text-zinc-400 block text-[11px]">Pincode</span>
                          <span className="font-medium text-zinc-800 dark:text-zinc-200 text-xs font-mono">
                            {activeDetailRequest.pincode || "—"}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Section 3: Fabrics & Commercials */}
                    <div className="space-y-3 p-4 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/40 dark:bg-zinc-900/40">
                      <h4 className="font-bold text-xs uppercase tracking-wider text-zinc-500 flex items-center gap-1.5">
                        <Tag className="h-3.5 w-3.5" />
                        <span>Fabrics, Products & Commercials</span>
                      </h4>
                      <div className="grid grid-cols-2 gap-3">
                        <div className="col-span-2">
                          <span className="text-zinc-400 block text-[11px]">Fabrics / Products Made</span>
                          <span className="font-semibold text-zinc-900 dark:text-zinc-100 text-xs">
                            {activeDetailRequest.productsMade || activeDetailRequest.categories || "—"}
                          </span>
                        </div>
                        <div>
                          <span className="text-zinc-400 block text-[11px]">Price Range</span>
                          <span className="font-semibold text-emerald-600 text-xs">
                            {activeDetailRequest.priceRange || "—"}
                          </span>
                        </div>
                        <div>
                          <span className="text-zinc-400 block text-[11px]">GSTIN</span>
                          <span className="font-mono text-zinc-800 dark:text-zinc-200 text-xs font-semibold">
                            {activeDetailRequest.gstin || "—"}
                          </span>
                        </div>
                        <div>
                          <span className="text-zinc-400 block text-[11px]">PAN Number</span>
                          <span className="font-mono text-zinc-800 dark:text-zinc-200 text-xs font-semibold">
                            {activeDetailRequest.panNumber || "—"}
                          </span>
                        </div>
                        <div>
                          <span className="text-zinc-400 block text-[11px]">Bank Name</span>
                          <span className="font-medium text-zinc-800 dark:text-zinc-200 text-xs">
                            {activeDetailRequest.bankName || "—"}
                          </span>
                        </div>
                        {activeDetailRequest.notes && (
                          <div className="col-span-2">
                            <span className="text-zinc-400 block text-[11px]">Notes / Capabilities</span>
                            <span className="text-zinc-700 dark:text-zinc-300 text-xs italic">
                              "{activeDetailRequest.notes}"
                            </span>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Section 4: Verification Photos & Lightbox */}
                    <div className="space-y-3 p-4 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/40 dark:bg-zinc-900/40">
                      <h4 className="font-bold text-xs uppercase tracking-wider text-zinc-500 flex items-center gap-1.5">
                        <ShieldCheck className="h-3.5 w-3.5" />
                        <span>KYC & Verification Photos</span>
                      </h4>
                      <div className="grid grid-cols-2 gap-3">
                        {activeDetailRequest.visitingCardPhotoUri && (
                          <div
                            onClick={() => setLightbox({ open: true, url: activeDetailRequest.visitingCardPhotoUri!, title: "Visiting Card" })}
                            className="cursor-pointer group relative rounded-xl border border-zinc-200 dark:border-zinc-700 overflow-hidden bg-white dark:bg-zinc-800 p-2 text-center"
                          >
                            <img
                              src={activeDetailRequest.visitingCardPhotoUri}
                              alt="Visiting Card"
                              className="h-24 w-full object-cover rounded-lg group-hover:scale-105 transition-transform"
                            />
                            <span className="text-[10px] font-semibold text-zinc-600 dark:text-zinc-400 mt-1 block">
                              Visiting Card
                            </span>
                          </div>
                        )}

                        {activeDetailRequest.shopPhotoUri && (
                          <div
                            onClick={() => setLightbox({ open: true, url: activeDetailRequest.shopPhotoUri!, title: "Mill / Factory Front" })}
                            className="cursor-pointer group relative rounded-xl border border-zinc-200 dark:border-zinc-700 overflow-hidden bg-white dark:bg-zinc-800 p-2 text-center"
                          >
                            <img
                              src={activeDetailRequest.shopPhotoUri}
                              alt="Mill Front"
                              className="h-24 w-full object-cover rounded-lg group-hover:scale-105 transition-transform"
                            />
                            <span className="text-[10px] font-semibold text-zinc-600 dark:text-zinc-400 mt-1 block">
                              Mill Front Photo
                            </span>
                          </div>
                        )}

                        {activeDetailRequest.gstCertPhotoUri && (
                          <div
                            onClick={() => setLightbox({ open: true, url: activeDetailRequest.gstCertPhotoUri!, title: "GST Certificate" })}
                            className="cursor-pointer group relative rounded-xl border border-zinc-200 dark:border-zinc-700 overflow-hidden bg-white dark:bg-zinc-800 p-2 text-center"
                          >
                            <img
                              src={activeDetailRequest.gstCertPhotoUri}
                              alt="GST Cert"
                              className="h-24 w-full object-cover rounded-lg group-hover:scale-105 transition-transform"
                            />
                            <span className="text-[10px] font-semibold text-zinc-600 dark:text-zinc-400 mt-1 block">
                              GST Certificate
                            </span>
                          </div>
                        )}

                        {activeDetailRequest.panPhotoUri && (
                          <div
                            onClick={() => setLightbox({ open: true, url: activeDetailRequest.panPhotoUri!, title: "PAN Card" })}
                            className="cursor-pointer group relative rounded-xl border border-zinc-200 dark:border-zinc-700 overflow-hidden bg-white dark:bg-zinc-800 p-2 text-center"
                          >
                            <img
                              src={activeDetailRequest.panPhotoUri}
                              alt="PAN Card"
                              className="h-24 w-full object-cover rounded-lg group-hover:scale-105 transition-transform"
                            />
                            <span className="text-[10px] font-semibold text-zinc-600 dark:text-zinc-400 mt-1 block">
                              PAN Card
                            </span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Drawer Footer Actions */}
                  <div className="p-4 border-t border-zinc-100 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/80 flex items-center justify-between gap-3">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleOpenRejectDialog(activeDetailRequest)}
                      className="text-xs text-rose-600 border-rose-200 hover:bg-rose-50"
                    >
                      <XCircle className="h-3.5 w-3.5 mr-1" />
                      <span>Decline</span>
                    </Button>

                    <Button
                      size="sm"
                      onClick={() => handleOpenApprovalDialog(activeDetailRequest)}
                      className="text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white gap-1.5 shadow-sm px-4"
                    >
                      <CheckCircle2 className="h-4 w-4" />
                      <span>Approve & Onboard Supplier</span>
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {/* Add / Edit Supplier Multi-Tab Dialog */}
      <Dialog
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
        title={editingId ? "Edit Supplier / Mill Master" : "Register Supplier / Mill Master"}
        description="Textile mill profile with market selection, brand mapping, multi-factories, price range, and visiting cards."
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

          <Tabs
            value={activeFormTab}
            onValueChange={setActiveFormTab}
            options={[
              { value: "basic", label: "1. Firm, Market & Brand" },
              { value: "contact", label: "2. Contact & 5 Phones" },
              { value: "factories", label: "3. Factories & Outlets" },
              { value: "products", label: "4. Products & Price Range" },
              { value: "photos", label: "5. Visiting Card & Photos" },
            ]}
          />

          {/* TAB 1: Firm, Market & Brand */}
          {activeFormTab === "basic" && (
            <div className="space-y-3.5 text-xs">
              {/* GSTIN First with Auto-Fetch Option */}
              <div className="p-3.5 rounded-xl bg-gradient-to-r from-indigo-50/80 via-blue-50/50 to-indigo-50/80 dark:from-indigo-950/40 dark:via-zinc-800/50 dark:to-indigo-950/40 border border-indigo-200/80 dark:border-indigo-800/80 space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-indigo-950 dark:text-indigo-200 flex items-center gap-1.5">
                    <ShieldCheck className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
                    <span>GSTIN Number (Auto-Fetch Mill Details)</span>
                  </label>
                  <div className="flex items-center gap-1.5">
                    <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-indigo-100 dark:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800">
                      Optional
                    </span>
                    {gstin && (
                      <button
                        type="button"
                        onClick={handleClearGst}
                        className="text-[11px] text-zinc-500 hover:text-red-600 dark:hover:text-red-400 underline ml-1"
                      >
                        Clear
                      </button>
                    )}
                  </div>
                </div>

                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <Input
                      maxLength={15}
                      value={gstin}
                      onChange={(e) => {
                        const val = e.target.value.toUpperCase().replace(/[^0-9A-Z]/g, "")
                        setGstin(val)
                        if (val.length === 15 && isValidGstin(val)) {
                          handleGstLookup(val)
                        } else if (val.length === 0) {
                          setGstFeedback({ type: null, message: "" })
                        }
                      }}
                      placeholder="e.g. 24AAAAA0000A1Z5 (15 Characters)"
                      className="h-9 text-xs font-mono uppercase bg-white dark:bg-zinc-900 border-indigo-300 dark:border-indigo-700"
                    />
                    {gstin.length === 15 && isValidGstin(gstin) && (
                      <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                      </div>
                    )}
                  </div>

                  <Button
                    type="button"
                    disabled={isFetchingGst || !gstin.trim()}
                    onClick={() => handleGstLookup()}
                    className="h-9 px-3.5 text-xs bg-indigo-600 hover:bg-indigo-700 text-white gap-1.5 shrink-0"
                  >
                    {isFetchingGst ? (
                      <>
                        <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                        <span>Fetching...</span>
                      </>
                    ) : (
                      <>
                        <Sparkles className="h-3.5 w-3.5" />
                        <span>Fetch Details</span>
                      </>
                    )}
                  </Button>
                </div>

                {gstFeedback.message ? (
                  <div
                    className={`text-[11px] p-2 rounded-lg flex items-start gap-1.5 font-medium ${
                      gstFeedback.type === "success"
                        ? "bg-emerald-50 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800"
                        : gstFeedback.type === "offline"
                        ? "bg-blue-50 dark:bg-blue-950/50 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800"
                        : "bg-amber-50 dark:bg-amber-950/50 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800"
                    }`}
                  >
                    <span className="shrink-0">
                      {gstFeedback.type === "success" ? "✓" : gstFeedback.type === "offline" ? "ℹ" : "⚠"}
                    </span>
                    <span className="leading-tight">{gstFeedback.message}</span>
                  </div>
                ) : (
                  <p className="text-[11px] text-indigo-900/70 dark:text-indigo-300/70">
                    Entering GSTIN automatically decodes State and PAN, and auto-fills Mill Name, City, and Address.
                  </p>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Firm / Mill Name <span className="text-red-500">*</span>
                  </label>
                  <Input
                    required
                    value={firmName}
                    onChange={(e) => {
                      setFirmName(e.target.value)
                      setName(e.target.value)
                    }}
                    placeholder="e.g. Radheshyam Textile Mills Pvt Ltd"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Contact Person Name
                  </label>
                  <Input
                    value={contactPerson}
                    onChange={(e) => setContactPerson(e.target.value)}
                    placeholder="e.g. Rajesh Bhai Shah"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                      Market (From Master) *
                    </label>
                    <button
                      type="button"
                      onClick={() => setIsQuickMarketOpen(true)}
                      className="text-[11px] font-bold text-amber-700 hover:underline flex items-center gap-0.5"
                    >
                      <Plus className="h-3 w-3" /> New Market
                    </button>
                  </div>
                  <select
                    value={selectedMarketName}
                    onChange={(e) => setSelectedMarketName(e.target.value)}
                    className="mt-1 w-full h-8 rounded-md border border-zinc-300 bg-white px-2.5 text-xs text-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                  >
                    <option value="">-- Choose Textile Market --</option>
                    {markets.map((m) => (
                      <option key={m.id} value={m.marketName}>
                        {m.marketName} ({m.city})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                      Brand (From Master)
                    </label>
                    <button
                      type="button"
                      onClick={() => setIsQuickBrandOpen(true)}
                      className="text-[11px] font-bold text-amber-700 hover:underline flex items-center gap-0.5"
                    >
                      <Plus className="h-3 w-3" /> New Brand
                    </button>
                  </div>
                  <select
                    value={selectedBrandName}
                    onChange={(e) => setSelectedBrandName(e.target.value)}
                    className="mt-1 w-full h-8 rounded-md border border-zinc-300 bg-white px-2.5 text-xs text-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                  >
                    <option value="">-- Choose Brand --</option>
                    {brands.map((b) => (
                      <option key={b.id} value={b.brandName}>
                        {b.brandName} ({b.category || "Apparel"})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Supplier Type
                  </label>
                  <select
                    value={type}
                    onChange={(e) => setType(e.target.value)}
                    className="mt-1 w-full h-8 rounded-md border border-zinc-300 bg-white px-2.5 text-xs text-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                  >
                    <option value="Wholesaler">Wholesaler</option>
                    <option value="Manufacturer">Manufacturer</option>
                  </select>
                </div>

                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    GSTIN
                  </label>
                  <Input
                    value={gstin}
                    onChange={(e) => setGstin(e.target.value.toUpperCase())}
                    placeholder="24AAAAA0000A1Z5"
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
                    placeholder="AAAAA0000A"
                    className="mt-1 h-8 text-xs font-mono uppercase"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    City
                  </label>
                  <Input
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="Ahmedabad / Surat"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    State
                  </label>
                  <Input
                    value={state}
                    onChange={(e) => setState(e.target.value)}
                    placeholder="Gujarat"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: Contacts & 5 Phones */}
          {activeFormTab === "contact" && (
            <div className="space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Contact Phone Lines (Up to 5)
                </label>
                {phoneCount < 5 && (
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => setPhoneCount((prev) => Math.min(5, prev + 1))}
                    className="h-7 text-xs gap-1"
                  >
                    <Plus className="h-3 w-3" />
                    Add Another Line ({phoneCount}/5)
                  </Button>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-[11px] text-muted-foreground">Phone 1 (Primary / Desk) *</label>
                  <Input
                    required
                    value={phone1}
                    onChange={(e) => setPhone1(e.target.value)}
                    placeholder="Primary contact"
                    className="h-8 text-xs mt-0.5"
                  />
                </div>
                {phoneCount >= 2 && (
                  <div>
                    <label className="text-[11px] text-muted-foreground">Phone 2 (Owner Mobile)</label>
                    <Input
                      value={phone2}
                      onChange={(e) => setPhone2(e.target.value)}
                      placeholder="Line 2"
                      className="h-8 text-xs mt-0.5"
                    />
                  </div>
                )}
                {phoneCount >= 3 && (
                  <div>
                    <label className="text-[11px] text-muted-foreground">Phone 3 (Dispatch Desk)</label>
                    <Input
                      value={phone3}
                      onChange={(e) => setPhone3(e.target.value)}
                      placeholder="Line 3"
                      className="h-8 text-xs mt-0.5"
                    />
                  </div>
                )}
                {phoneCount >= 4 && (
                  <div>
                    <label className="text-[11px] text-muted-foreground">Phone 4 (Accounts)</label>
                    <Input
                      value={phone4}
                      onChange={(e) => setPhone4(e.target.value)}
                      placeholder="Line 4"
                      className="h-8 text-xs mt-0.5"
                    />
                  </div>
                )}
                {phoneCount >= 5 && (
                  <div>
                    <label className="text-[11px] text-muted-foreground">Phone 5 (Factory Office)</label>
                    <Input
                      value={phone5}
                      onChange={(e) => setPhone5(e.target.value)}
                      placeholder="Line 5"
                      className="h-8 text-xs mt-0.5"
                    />
                  </div>
                )}
              </div>

              <div>
                <label className="text-[11px] text-muted-foreground">Official Email Address</label>
                <Input
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="sales@mill.com"
                  className="h-8 text-xs mt-0.5"
                />
              </div>
            </div>
          )}

          {/* TAB 3: Factories & Outlets (Up to 5) */}
          {activeFormTab === "factories" && (
            <div className="space-y-4 text-xs">
              {/* Factories */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <h4 className="font-semibold text-zinc-800 dark:text-zinc-200">Factory & Mill Addresses (Up to 5)</h4>
                  {factories.length < 5 && (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        setFactories([...factories, { name: `Factory ${factories.length + 1}`, address: "", city: "Ahmedabad", pincode: "" }])
                      }
                      className="h-7 text-xs gap-1"
                    >
                      <Plus className="h-3 w-3" /> Add Factory
                    </Button>
                  )}
                </div>

                {factories.map((f, idx) => (
                  <div key={idx} className="p-2.5 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-900/50 space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-amber-700">Factory #{idx + 1}</span>
                      {factories.length > 1 && (
                        <button
                          type="button"
                          onClick={() => setFactories(factories.filter((_, i) => i !== idx))}
                          className="text-red-500 hover:text-red-700 text-xs"
                        >
                          Remove
                        </button>
                      )}
                    </div>
                    <div className="grid grid-cols-2 gap-2">
                      <Input
                        placeholder="Factory Unit / GIDC Plot"
                        value={f.name}
                        onChange={(e) => {
                          const updated = [...factories]
                          updated[idx].name = e.target.value
                          setFactories(updated)
                        }}
                        className="h-7 text-xs"
                      />
                      <Input
                        placeholder="City"
                        value={f.city || ""}
                        onChange={(e) => {
                          const updated = [...factories]
                          updated[idx].city = e.target.value
                          setFactories(updated)
                        }}
                        className="h-7 text-xs"
                      />
                    </div>
                    <Input
                      placeholder="Full factory address, GIDC Phase, Estate"
                      value={f.address}
                      onChange={(e) => {
                        const updated = [...factories]
                        updated[idx].address = e.target.value
                        setFactories(updated)
                      }}
                      className="h-7 text-xs"
                    />
                  </div>
                ))}
              </div>

              {/* Outlets */}
              <div className="space-y-2 pt-2 border-t border-zinc-100 dark:border-zinc-800">
                <div className="flex items-center justify-between">
                  <h4 className="font-semibold text-zinc-800 dark:text-zinc-200">Market Shops & Outlets (Up to 5)</h4>
                  {outlets.length < 5 && (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        setOutlets([...outlets, { name: `Outlet ${outlets.length + 1}`, address: "", city: "Ahmedabad", pincode: "" }])
                      }
                      className="h-7 text-xs gap-1"
                    >
                      <Plus className="h-3 w-3" /> Add Outlet
                    </Button>
                  )}
                </div>

                {outlets.map((o, idx) => (
                  <div key={idx} className="p-2.5 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50/50 dark:bg-zinc-900/50 space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-indigo-700">Outlet #{idx + 1}</span>
                      {outlets.length > 1 && (
                        <button
                          type="button"
                          onClick={() => setOutlets(outlets.filter((_, i) => i !== idx))}
                          className="text-red-500 hover:text-red-700 text-xs"
                        >
                          Remove
                        </button>
                      )}
                    </div>
                    <div className="grid grid-cols-2 gap-2">
                      <Input
                        placeholder="Shop / Office Name"
                        value={o.name}
                        onChange={(e) => {
                          const updated = [...outlets]
                          updated[idx].name = e.target.value
                          setOutlets(updated)
                        }}
                        className="h-7 text-xs"
                      />
                      <Input
                        placeholder="City"
                        value={o.city || ""}
                        onChange={(e) => {
                          const updated = [...outlets]
                          updated[idx].city = e.target.value
                          setOutlets(updated)
                        }}
                        className="h-7 text-xs"
                      />
                    </div>
                    <Input
                      placeholder="Shop No, Cloth Market, Floor, Gate"
                      value={o.address}
                      onChange={(e) => {
                        const updated = [...outlets]
                        updated[idx].address = e.target.value
                        setOutlets(updated)
                      }}
                      className="h-7 text-xs"
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 4: Products & Price Range */}
          {activeFormTab === "products" && (
            <div className="space-y-3.5 text-xs">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    What They Make / Manufacturing Items
                  </label>
                  <Input
                    value={productsMade}
                    onChange={(e) => setProductsMade(e.target.value)}
                    placeholder="e.g. Men's Stretch Jeans, Lycra Trousers, Cotton Shirts"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Price Range in Rupees (₹)
                  </label>
                  <Input
                    value={priceRange}
                    onChange={(e) => setPriceRange(e.target.value)}
                    placeholder="e.g. ₹250 - ₹1200 / pc"
                    className="mt-1 h-8 text-xs"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Fabric & Garment Categories
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
                            ? "bg-zinc-900 text-white border-zinc-900 dark:bg-zinc-100 dark:text-zinc-900 font-semibold"
                            : "bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 border-zinc-200 dark:border-zinc-700 hover:border-zinc-400"
                        }`}
                      >
                        {isSelected && <Check className="h-2.5 w-2.5" />}
                        <span>{cat}</span>
                      </button>
                    )
                  })}
                </div>
              </div>
            </div>
          )}

          {/* TAB 5: Visiting Card & Photos */}
          {activeFormTab === "photos" && (
            <div className="space-y-3.5 text-xs">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <FileUpload
                  label="Visiting Card Photo"
                  folder={`suppliers/${supplierId || "temp"}/photos`}
                  prefix="visiting_card"
                  value={visitingCardPhotoUri}
                  onChange={setVisitingCardPhotoUri}
                />

                <FileUpload
                  label="Shop / Mill Front Photo"
                  folder={`suppliers/${supplierId || "temp"}/photos`}
                  prefix="shop"
                  value={shopPhotoUri}
                  onChange={setShopPhotoUri}
                />
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

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Notes & Special Terms
                </label>
                <textarea
                  rows={2}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Dispatch turnaround time, preferred courier, payment instructions..."
                  className="mt-1 w-full rounded-md border border-zinc-300 bg-white p-2 text-xs text-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                />
              </div>
            </div>
          )}

          {/* Action Buttons */}
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
              {editingId ? "Update Supplier" : "Save Supplier"}
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Quick Add Market Modal */}
      <Dialog
        open={isQuickMarketOpen}
        onOpenChange={setIsQuickMarketOpen}
        title="Quick Create Textile Market"
      >
        <form onSubmit={handleQuickCreateMarket} className="space-y-3 text-xs">
          <div>
            <label className="font-semibold text-xs">Market Name *</label>
            <Input
              required
              placeholder="e.g. New Cloth Market"
              value={quickMarketName}
              onChange={(e) => setQuickMarketName(e.target.value)}
              className="mt-1 h-8 text-xs"
            />
          </div>
          <div>
            <label className="font-semibold text-xs">City</label>
            <Input
              value={quickMarketCity}
              onChange={(e) => setQuickMarketCity(e.target.value)}
              className="mt-1 h-8 text-xs"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" size="sm" onClick={() => setIsQuickMarketOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" size="sm" className="bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900">
              Save Market
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Quick Add Brand Modal */}
      <Dialog
        open={isQuickBrandOpen}
        onOpenChange={setIsQuickBrandOpen}
        title="Quick Create Brand Master"
      >
        <form onSubmit={handleQuickCreateBrand} className="space-y-3 text-xs">
          <div>
            <label className="font-semibold text-xs">Brand Name *</label>
            <Input
              required
              placeholder="e.g. V-Denim, Royal Cotton"
              value={quickBrandName}
              onChange={(e) => setQuickBrandName(e.target.value)}
              className="mt-1 h-8 text-xs"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" size="sm" onClick={() => setIsQuickBrandOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" size="sm" className="bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900">
              Save Brand
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Invoice Modal */}
      <ReportViewerModal
        open={reportModal.open}
        onOpenChange={(open) => !open && setReportModal({ open: false, title: "", html: "", whatsAppText: "" })}
        title={reportModal.title}
        htmlContent={reportModal.html}
        whatsAppText={reportModal.whatsAppText}
      />

      {/* Supplier Request Approval Modal */}
      <Dialog
        open={Boolean(selectedRequestForApproval)}
        onOpenChange={(open) => !open && setSelectedRequestForApproval(null)}
        title="Approve & Onboard Supplier"
        description="Confirm brand mapping and textile market hub for this new supplier."
      >
        {selectedRequestForApproval && (
          <div className="space-y-4 pt-1 text-xs">
            <div className="p-3 rounded-xl bg-amber-50/70 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800">
              <p className="font-bold text-sm text-zinc-900 dark:text-zinc-50">
                {selectedRequestForApproval.firmName || selectedRequestForApproval.name}
              </p>
              <p className="text-zinc-600 dark:text-zinc-400 mt-0.5">
                Contact: {selectedRequestForApproval.contactPerson} ({selectedRequestForApproval.phone}) • {selectedRequestForApproval.type}
              </p>
            </div>

            <div>
              <label className="font-semibold block mb-1">Textile Brand Name</label>
              <Input
                value={approvalBrand}
                onChange={(e) => setApprovalBrand(e.target.value)}
                placeholder="e.g. Radhey Silk, RT Cotton"
                className="h-8 text-xs"
              />
            </div>

            <div>
              <label className="font-semibold block mb-1">Market Hub / Area</label>
              <Input
                value={approvalMarket}
                onChange={(e) => setApprovalMarket(e.target.value)}
                placeholder="e.g. Maskati Market, New Cloth Market"
                className="h-8 text-xs"
              />
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-zinc-100 dark:border-zinc-800">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setSelectedRequestForApproval(null)}
              >
                Cancel
              </Button>
              <Button
                size="sm"
                onClick={handleConfirmApproval}
                disabled={isApproving}
                className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold"
              >
                {isApproving ? "Approving..." : "Confirm & Onboard"}
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      {/* Reject Modal */}
      <Dialog
        open={rejectModal.open}
        onOpenChange={(open) => !open && setRejectModal({ open: false, request: null, reason: "" })}
        title="Decline Supplier Request"
        description="Provide an optional reason for declining this supplier application."
      >
        <div className="space-y-3 pt-1 text-xs">
          <div>
            <label className="font-semibold block mb-1">Reason for Rejection</label>
            <textarea
              rows={3}
              value={rejectModal.reason}
              onChange={(e) => setRejectModal((prev) => ({ ...prev, reason: e.target.value }))}
              placeholder="e.g. Incomplete KYC documents, invalid phone number, outside supply scope..."
              className="w-full p-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs focus:outline-none focus:ring-2 focus:ring-rose-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setRejectModal({ open: false, request: null, reason: "" })}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleConfirmReject}
              disabled={isRejecting}
              className="bg-rose-600 hover:bg-rose-700 text-white font-bold"
            >
              {isRejecting ? "Declining..." : "Decline Request"}
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Share Link Modal */}
      <Dialog
        open={isShareLinkModalOpen}
        onOpenChange={setIsShareLinkModalOpen}
        title="Invite Supplier / Fabric Mill"
        description="Share the public registration link with prospective textile mills, fabric manufacturers, and wholesalers."
      >
        <div className="space-y-4 pt-1 text-xs">
          <div className="p-3 rounded-xl bg-zinc-50 dark:bg-zinc-800/60 border border-zinc-200 dark:border-zinc-700 space-y-2">
            <label className="font-bold text-zinc-700 dark:text-zinc-300 block">
              Public Registration Link:
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={getPublicSupplierRegistrationUrl()}
                className="flex-1 h-8 px-2.5 rounded-lg border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-900 font-mono text-[11px] text-zinc-800 dark:text-zinc-200 select-all"
              />
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  navigator.clipboard.writeText(getPublicSupplierRegistrationUrl())
                  setCopiedLink(true)
                  setTimeout(() => setCopiedLink(false), 2000)
                }}
                className="h-8 px-3 text-xs gap-1 font-semibold"
              >
                {copiedLink ? <Check className="h-3.5 w-3.5 text-emerald-600" /> : <Copy className="h-3.5 w-3.5" />}
                <span>{copiedLink ? "Copied!" : "Copy"}</span>
              </Button>
            </div>
          </div>

          <div className="space-y-2">
            <label className="font-bold text-zinc-700 dark:text-zinc-300 block">
              Direct WhatsApp Invitation:
            </label>
            <div className="flex gap-2">
              <div className="relative flex-1">
                <span className="absolute left-2.5 top-2 text-[11px] text-zinc-400 font-bold">+91</span>
                <input
                  type="tel"
                  maxLength={10}
                  value={directSharePhone}
                  onChange={(e) => setDirectSharePhone(e.target.value.replace(/\D/g, ""))}
                  placeholder="Enter 10-digit mobile number"
                  className="w-full h-8 pl-9 pr-2 rounded-lg border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs"
                />
              </div>
              <a
                href={`https://wa.me/${directSharePhone ? `91${directSharePhone}` : ""}?text=${encodeURIComponent(buildSupplierInviteMessage())}`}
                target="_blank"
                rel="noreferrer"
                className={`inline-flex items-center gap-1.5 px-3 h-8 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-sm transition-all ${
                  !directSharePhone ? "opacity-50 pointer-events-none" : ""
                }`}
              >
                <MessageSquare className="h-3.5 w-3.5" />
                <span>Send WhatsApp</span>
              </a>
            </div>
          </div>
        </div>
      </Dialog>

      {/* Image Lightbox Modal */}
      <ImageLightboxModal
        open={lightbox.open}
        onClose={() => setLightbox({ open: false, url: "", title: "" })}
        imageUrl={lightbox.url}
        title={lightbox.title}
      />
    </div>
  )
}
