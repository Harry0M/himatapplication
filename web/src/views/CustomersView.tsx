import React, { useState } from "react"
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
  Sparkles
} from "lucide-react"
import { useData } from "../context/DataContext"
import { useAuth } from "../context/AuthContext"
import { formatInr } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Tabs } from "../components/ui/Tabs"
import { Customer, CustomerContact, CustomerOutlet, Visit } from "../types"
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
  } = useData()

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

  // Open Add Dialog
  const handleOpenAdd = () => {
    setEditingId(null)
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
    setAadharPhotoUri("")
    setGstCertPhotoUri("")
    setPanPhotoUri("")
    setShopPhotoUri("")
    setPurchaserPhotoUri("")
    setCancelChequePhotoUri("")
    setActiveFormTab("basic")
    setIsDialogOpen(true)
  }

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
                size="sm"
                onClick={handleOpenAdd}
                className="h-8 px-3 text-xs font-semibold shadow-sm bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 gap-1"
              >
                <Plus className="h-3.5 w-3.5" />
                <span>New Customer</span>
              </Button>
            </div>
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

          {/* Customer Cards Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredCustomers.length === 0 ? (
              <div className="col-span-full p-12 text-center border border-dashed rounded-2xl">
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
              filteredCustomers.map((cust) => {
                const hasGst = Boolean(cust.gstin || cust.gstNumber)
                const outletCount = (cust.outlets && cust.outlets.length) || cust.shopCount || 1
                const primaryPhone = cust.phone || (cust.contacts && cust.contacts[0]?.phone) || ""

                return (
                  <Card
                    key={cust.id}
                    className="group relative flex flex-col justify-between p-4 border border-zinc-200/80 dark:border-zinc-800 hover:shadow-md transition-all"
                  >
                    <div
                      className="cursor-pointer"
                      onClick={() => setSelectedCustomerId(cust.id)}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex items-center gap-2.5 min-w-0">
                          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-indigo-500/10 text-indigo-700 font-bold text-sm dark:bg-indigo-500/20 dark:text-indigo-400 group-hover:bg-indigo-600 group-hover:text-white transition-colors">
                            {(cust.firmName || cust.name || "C")[0].toUpperCase()}
                          </div>
                          <div className="min-w-0">
                            <h4 className="font-bold text-sm text-zinc-900 dark:text-zinc-50 truncate group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                              {cust.firmName || cust.name}
                            </h4>
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

                        <Badge
                          variant={cust.customerType === "Credit" ? "warning" : "default"}
                          className="text-[10px] uppercase font-bold shrink-0"
                        >
                          {cust.customerType || "Cash"}
                        </Badge>
                      </div>

                      <div className="mt-3.5 space-y-1.5 text-xs text-zinc-600 dark:text-zinc-300">
                        <div className="flex items-center gap-1.5">
                          <MapPin className="h-3.5 w-3.5 text-zinc-400 shrink-0" />
                          <span className="truncate">
                            {cust.city || "Ahmedabad"}{cust.state ? `, ${cust.state}` : ""}
                            {cust.pincode ? ` (${cust.pincode})` : ""}
                          </span>
                        </div>

                        {primaryPhone && (
                          <div className="flex items-center justify-between text-xs">
                            <div className="flex items-center gap-1.5 font-medium text-zinc-800 dark:text-zinc-200">
                              <Phone className="h-3.5 w-3.5 text-zinc-400 shrink-0" />
                              <span>{primaryPhone}</span>
                            </div>
                            {cust.contacts && cust.contacts.length > 1 && (
                              <span className="text-[10px] font-semibold bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400 px-1.5 py-0.5 rounded-full">
                                +{cust.contacts.length - 1} lines
                              </span>
                            )}
                          </div>
                        )}

                        <div className="flex items-center justify-between text-[11px] pt-1 text-muted-foreground">
                          <span>{outletCount} {outletCount === 1 ? "Outlet" : "Outlets"}</span>
                          {hasGst ? (
                            <span className="font-mono text-emerald-600 dark:text-emerald-400 font-semibold">
                              GST: {cust.gstin || cust.gstNumber}
                            </span>
                          ) : (
                            <span className="italic text-zinc-400">Unregistered</span>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Bottom Actions */}
                    <div className="mt-4 pt-3 border-t border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between gap-1.5">
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
                        className="flex-1 h-7 text-xs font-medium"
                      >
                        <Info className="h-3 w-3 mr-1" />
                        Full Profile
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
                  </Card>
                )
              })
            )}
          </div>
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
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Referred By (Select Master or Type)
                  </label>
                  <div className="flex gap-1.5 mt-1">
                    <select
                      value={referredBy}
                      onChange={(e) => setReferredBy(e.target.value)}
                      className="w-full h-8 rounded-md border border-zinc-300 bg-white px-2.5 text-xs text-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                    >
                      <option value="">-- Select Referrer --</option>
                      <optgroup label="Staff & Agents">
                        {employees.map((e) => (
                          <option key={`emp-${e.id}`} value={`Staff: ${e.name}`}>
                            Staff: {e.name} ({e.role})
                          </option>
                        ))}
                      </optgroup>
                      <optgroup label="Existing Customers">
                        {customers.slice(0, 10).map((c) => (
                          <option key={`cust-${c.id}`} value={`Customer: ${c.firmName || c.name}`}>
                            Customer: {c.firmName || c.name}
                          </option>
                        ))}
                      </optgroup>
                      <optgroup label="Suppliers & Mills">
                        {suppliers.slice(0, 10).map((s) => (
                          <option key={`sup-${s.id}`} value={`Supplier: ${s.firmName || s.name}`}>
                            Supplier: {s.firmName || s.name}
                          </option>
                        ))}
                      </optgroup>
                    </select>
                  </div>
                  <Input
                    placeholder="Or type custom referrer..."
                    value={referredBy}
                    onChange={(e) => setReferredBy(e.target.value)}
                    className="mt-1 h-7 text-[11px]"
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

      {/* Report Modal */}
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
