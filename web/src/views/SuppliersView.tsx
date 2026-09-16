import React, { useState } from "react"
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
  CreditCard
} from "lucide-react"
import { useData } from "../context/DataContext"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Tabs } from "../components/ui/Tabs"
import { Supplier, SupplierAddress, Visit } from "../types"
import { GARMENT_CATEGORIES } from "../lib/constants"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateSupplierInvoiceHtml,
  buildSupplierInvoiceWhatsAppText,
} from "../lib/pdfReports"
import { generateSuppliersTallyXml, downloadXmlFile } from "../lib/tallyExport"
import { FileUpload } from "../components/ui/FileUpload"
import { SupplierDetailView } from "./SupplierDetailView"

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
  } = useData()

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

  // Open Add Dialog
  const handleOpenAdd = () => {
    setEditingId(null)
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
    setActiveFormTab("basic")
    setIsDialogOpen(true)
  }

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
                size="sm"
                onClick={handleOpenAdd}
                className="h-8 px-3 text-xs font-semibold shadow-sm bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 gap-1"
              >
                <Plus className="h-3.5 w-3.5" />
                <span>New Supplier</span>
              </Button>
            </div>
          </div>

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

          {/* Suppliers Card Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredSuppliers.length === 0 ? (
              <div className="col-span-full p-12 text-center border border-dashed rounded-2xl">
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
              filteredSuppliers.map((sup) => (
                <Card
                  key={sup.id}
                  className="group relative flex flex-col justify-between p-4 border border-zinc-200/80 dark:border-zinc-800 hover:shadow-md transition-all"
                >
                  <div
                    className="cursor-pointer"
                    onClick={() => setSelectedSupplierId(sup.id)}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-amber-500/10 text-amber-700 font-bold text-sm dark:bg-amber-500/20 dark:text-amber-400 group-hover:bg-amber-600 group-hover:text-white transition-colors">
                          {(sup.firmName || sup.name || "S")[0].toUpperCase()}
                        </div>
                        <div className="min-w-0">
                          <h4 className="font-bold text-sm text-zinc-900 dark:text-zinc-50 truncate group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
                            {sup.firmName || sup.name}
                          </h4>
                          <p className="text-[11px] text-muted-foreground flex items-center gap-1.5 truncate">
                            <User className="h-3 w-3 shrink-0" />
                            <span>{sup.contactPerson || "In-charge"}</span>
                            {sup.supplierId && (
                              <span className="font-mono text-[10px] text-zinc-400 font-semibold">
                                • {sup.supplierId}
                              </span>
                            )}
                          </p>
                        </div>
                      </div>

                      <Badge variant="outline" className="text-[10px] uppercase font-bold shrink-0">
                        {sup.type || "Supplier"}
                      </Badge>
                    </div>

                    <div className="mt-3.5 space-y-1.5 text-xs text-zinc-600 dark:text-zinc-300">
                      <div className="flex items-center gap-1.5">
                        <Compass className="h-3.5 w-3.5 text-amber-600 shrink-0" />
                        <span className="truncate font-medium text-amber-800 dark:text-amber-400">
                          {sup.marketName || sup.marketArea || "Ahmedabad Market"}
                        </span>
                      </div>

                      {sup.brand && (
                        <div className="flex items-center gap-1.5 text-zinc-700 dark:text-zinc-300 font-semibold">
                          <Tag className="h-3.5 w-3.5 text-zinc-400 shrink-0" />
                          <span>Brand: {sup.brand}</span>
                        </div>
                      )}

                      {sup.phone && (
                        <div className="flex items-center gap-1.5 font-medium text-zinc-800 dark:text-zinc-200">
                          <Phone className="h-3.5 w-3.5 text-zinc-400 shrink-0" />
                          <span>{sup.phone}</span>
                        </div>
                      )}

                      {sup.priceRange && (
                        <div className="text-[11px] font-medium text-emerald-700 dark:text-emerald-400 pt-0.5">
                          Price Range: {sup.priceRange}
                        </div>
                      )}

                      {sup.gstin && (
                        <div className="text-[11px] font-mono text-zinc-400">
                          GST: {sup.gstin}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="mt-4 pt-3 border-t border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between gap-1.5">
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
                      variant="outline"
                      onClick={() => setSelectedSupplierId(sup.id)}
                      className="flex-1 h-7 text-xs font-medium"
                    >
                      <Info className="h-3 w-3 mr-1" />
                      Full Profile
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
                </Card>
              ))
            )}
          </div>
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
                    <option value="Manufacturer">Manufacturer (Mill)</option>
                    <option value="Wholesaler">Wholesaler</option>
                    <option value="Trader">Trader / Stockist</option>
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
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Referred By
                </label>
                <Input
                  placeholder="Introducer, Broker, or Agent"
                  value={referredBy}
                  onChange={(e) => setReferredBy(e.target.value)}
                  className="mt-1 h-8 text-xs"
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
    </div>
  )
}
