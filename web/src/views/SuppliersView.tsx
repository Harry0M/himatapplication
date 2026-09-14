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
  FileText
} from "lucide-react"
import { useData } from "../context/DataContext"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Tabs } from "../components/ui/Tabs"
import { Supplier, Visit } from "../types"
import { AHMEDABAD_TEXTILE_MARKETS, GARMENT_CATEGORIES } from "../lib/constants"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateSupplierInvoiceHtml,
  buildSupplierInvoiceWhatsAppText,
} from "../lib/pdfReports"

export function SuppliersView() {
  const {
    suppliers,
    visits,
    entries,
    customers,
    employees,
    saveSupplier,
    deleteSupplier,
  } = useData()
  const [search, setSearch] = useState<string>("")
  const [showSearch, setShowSearch] = useState<boolean>(false)
  const [typeFilter, setTypeFilter] = useState<string>("all")

  // Modal States
  const [isDialogOpen, setIsDialogOpen] = useState<boolean>(false)
  const [selectedSupplier, setSelectedSupplier] = useState<Supplier | null>(null)
  const [activeFormTab, setActiveFormTab] = useState<string>("basic")
  const [viewProfileSupplier, setViewProfileSupplier] = useState<Supplier | null>(null)

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

  const handleOpenSupplierInvoice = (sup: Supplier) => {
    const supEntries = entries.filter(
      (e) =>
        e.supplierId === sup.id ||
        (e.supplierName && e.supplierName.toLowerCase() === sup.name.toLowerCase())
    )

    const firstEntry = supEntries[0]
    const visit: Visit =
      (firstEntry && visits.find((v) => v.id === firstEntry.visitId)) || {
        id: Date.now(),
        visitCode: `SUP-${sup.id}`,
        customerId: 0,
        customerName: "Consolidated Retailers",
        date: new Date().toISOString().split("T")[0],
        employeeId: 1,
        employeeName: "Himat Textile",
        status: "Completed",
      }

    const customer =
      customers.find((c) => c.id === visit.customerId || c.name === visit.customerName) || null
    const salesman =
      employees.find((e) => e.id === visit.employeeId || e.name === visit.employeeName) || null

    const invoiceData = {
      supplier: sup,
      visit,
      customer,
      salesman,
      entries: supEntries,
    }

    const html = generateSupplierInvoiceHtml(invoiceData)
    const whatsAppText = buildSupplierInvoiceWhatsAppText(invoiceData)

    setReportModal({
      open: true,
      title: `Supplier Voucher: ${sup.name}`,
      html,
      whatsAppText,
    })
  }

  // Form State
  const [editingId, setEditingId] = useState<number | null>(null)
  const [name, setName] = useState<string>("")
  const [firmName, setFirmName] = useState<string>("")
  const [brand, setBrand] = useState<string>("")
  const [type, setType] = useState<string>("Manufacturer")
  const [gstin, setGstin] = useState<string>("")
  const [contactPerson, setContactPerson] = useState<string>("")

  // Phones (Up to 5)
  const [phone1, setPhone1] = useState<string>("")
  const [phone2, setPhone2] = useState<string>("")
  const [phone3, setPhone3] = useState<string>("")
  const [phone4, setPhone4] = useState<string>("")
  const [phone5, setPhone5] = useState<string>("")
  const [phoneCount, setPhoneCount] = useState<number>(1)

  // Emails
  const [email1, setEmail1] = useState<string>("")
  const [email2, setEmail2] = useState<string>("")

  // Addresses & Locations
  const [officeAddress, setOfficeAddress] = useState<string>("")
  const [officeLocation, setOfficeLocation] = useState<string>("")
  const [homeAddress, setHomeAddress] = useState<string>("")
  const [personalLocation, setPersonalLocation] = useState<string>("")
  const [shopCount, setShopCount] = useState<string>("1")
  const [shopLocations, setShopLocations] = useState<string>("")

  // Markets & Garments
  const [selectedMarkets, setSelectedMarkets] = useState<string[]>([])
  const [city, setCity] = useState<string>("Ahmedabad")
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])
  const [customCategory, setCustomCategory] = useState<string>("")
  const [referredBy, setReferredBy] = useState<string>("")
  const [notes, setNotes] = useState<string>("")

  // Open Add Dialog
  const handleOpenAdd = () => {
    setEditingId(null)
    setName("")
    setFirmName("")
    setBrand("")
    setType("Manufacturer")
    setGstin("")
    setContactPerson("")
    setPhone1("")
    setPhone2("")
    setPhone3("")
    setPhone4("")
    setPhone5("")
    setPhoneCount(1)
    setEmail1("")
    setEmail2("")
    setOfficeAddress("")
    setOfficeLocation("")
    setHomeAddress("")
    setPersonalLocation("")
    setShopCount("1")
    setShopLocations("")
    setSelectedMarkets(["New Cloth Market (Raipur)"])
    setCity("Ahmedabad")
    setSelectedCategories([])
    setCustomCategory("")
    setReferredBy("")
    setNotes("")
    setActiveFormTab("basic")
    setIsDialogOpen(true)
  }

  // Open Edit Dialog
  const handleOpenEdit = (s: Supplier) => {
    setEditingId(s.id)
    setName(s.name || "")
    setFirmName(s.firmName || s.name || "")
    setBrand(s.brand || "")
    setType(s.type || "Manufacturer")
    setGstin(s.gstin || s.gstNumber || "")
    setContactPerson(s.contactPerson || "")

    setPhone1(s.phone || "")
    setPhone2(s.phone2 || "")
    setPhone3(s.phone3 || "")
    setPhone4(s.phone4 || "")
    setPhone5(s.phone5 || "")
    const count = [s.phone, s.phone2, s.phone3, s.phone4, s.phone5].filter(Boolean).length
    setPhoneCount(Math.max(1, count))

    setEmail1(s.email || "")
    setEmail2(s.email2 || "")

    setOfficeAddress(s.officeAddress || s.address || "")
    setOfficeLocation(s.officeLocation || "")
    setHomeAddress(s.homeAddress || "")
    setPersonalLocation(s.personalLocation || "")
    setShopCount(String(s.shopCount || 1))
    setShopLocations(s.shopLocations || "")

    const mkts = s.markets
      ? s.markets.split(",").map((m) => m.trim()).filter(Boolean)
      : s.marketArea
      ? [s.marketArea]
      : []
    setSelectedMarkets(mkts.length > 0 ? mkts : ["New Cloth Market (Raipur)"])
    setCity(s.city || "Ahmedabad")

    const cats = (s.categories || s.garmentTypes || "")
      .split(",")
      .map((c) => c.trim())
      .filter(Boolean)
    setSelectedCategories(cats)
    setCustomCategory("")
    setReferredBy(s.referredBy || "")
    setNotes(s.notes || "")
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

  // Toggle Market selection
  const handleToggleMarket = (mkt: string) => {
    if (selectedMarkets.includes(mkt)) {
      setSelectedMarkets(selectedMarkets.filter((m) => m !== mkt))
    } else {
      setSelectedMarkets([...selectedMarkets, mkt])
    }
  }

  const handleSave = async () => {
    const finalName = (firmName.trim() || name.trim())
    if (!finalName) return

    const id = editingId || Date.now()
    const allPhones = [phone1.trim(), phone2.trim(), phone3.trim(), phone4.trim(), phone5.trim()].filter(Boolean)
    const primaryPhone = allPhones[0] || ""

    const allEmails = [email1.trim(), email2.trim()].filter(Boolean)
    const primaryEmail = allEmails[0] || ""

    const allCats = [...selectedCategories]
    if (customCategory.trim() && !allCats.includes(customCategory.trim())) {
      allCats.push(customCategory.trim())
    }

    const newSupplier: Supplier = {
      id,
      supplierId: editingId
        ? (suppliers.find((s) => s.id === editingId)?.supplierId || `SUP-${id % 10000}`)
        : `SUP-${id % 10000}`,
      name: finalName,
      firmName: finalName,
      brand: brand.trim(),
      type: type.trim(),
      gstin: gstin.trim().toUpperCase(),
      gstNumber: gstin.trim().toUpperCase(),
      contactPerson: contactPerson.trim(),
      phone: primaryPhone,
      phone2: phone2.trim() || "",
      phone3: phone3.trim() || "",
      phone4: phone4.trim() || "",
      phone5: phone5.trim() || "",
      phones: allPhones,
      email: primaryEmail || "",
      email2: email2.trim() || "",
      emails: allEmails,
      address: officeAddress.trim() || "",
      officeAddress: officeAddress.trim() || "",
      homeAddress: homeAddress.trim() || "",
      officeLocation: officeLocation.trim() || "",
      personalLocation: personalLocation.trim() || "",
      shopCount: parseInt(shopCount, 10) || 1,
      shopLocations: shopLocations.trim() || "",
      marketArea: selectedMarkets[0] || "New Cloth Market (Raipur)",
      markets: selectedMarkets.join(", "),
      city: city.trim() || "Ahmedabad",
      categories: allCats.join(", "),
      garmentTypes: allCats.join(", "),
      referredBy: referredBy.trim() || "",
      notes: notes.trim() || "",
      defaultCaseSize: 24,
      rating: 4.5,
    }

    await saveSupplier(newSupplier)
    setIsDialogOpen(false)
  }

  const handleDelete = async (id: number) => {
    if (window.confirm("Are you sure you want to remove this supplier master record?")) {
      await deleteSupplier(id)
      if (viewProfileSupplier?.id === id) {
        setViewProfileSupplier(null)
      }
    }
  }

  // Filter logic
  const q = search.trim().toLowerCase()
  const filteredSuppliers = suppliers.filter((s) => {
    const matchesQuery =
      !q ||
      s.name?.toLowerCase().includes(q) ||
      s.firmName?.toLowerCase().includes(q) ||
      s.brand?.toLowerCase().includes(q) ||
      s.contactPerson?.toLowerCase().includes(q) ||
      s.phone?.includes(q) ||
      s.phone2?.includes(q) ||
      s.phone3?.includes(q) ||
      s.phone4?.includes(q) ||
      s.phone5?.includes(q) ||
      s.gstin?.toLowerCase().includes(q) ||
      s.gstNumber?.toLowerCase().includes(q) ||
      s.marketArea?.toLowerCase().includes(q) ||
      s.markets?.toLowerCase().includes(q) ||
      s.city?.toLowerCase().includes(q) ||
      s.categories?.toLowerCase().includes(q) ||
      s.referredBy?.toLowerCase().includes(q)

    const matchesType =
      typeFilter === "all" || s.type?.toLowerCase() === typeFilter.toLowerCase()

    return matchesQuery && matchesType
  })

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
            <span>Suppliers & Textile Mills</span>
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            {suppliers.length} registered manufacturers, weavers, and Ahmedabad market dealers.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            shape="pill"
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

          <Tabs
            value={typeFilter}
            onValueChange={setTypeFilter}
            options={[
              { value: "all", label: "All", count: suppliers.length },
              {
                value: "manufacturer",
                label: "Mills / Mfg",
                count: suppliers.filter((s) => s.type?.toLowerCase() === "manufacturer").length,
              },
              {
                value: "wholesaler",
                label: "Wholesalers",
                count: suppliers.filter((s) => s.type?.toLowerCase() === "wholesaler").length,
              },
            ]}
          />

          <Button
            shape="pill"
            size="sm"
            onClick={handleOpenAdd}
            className="h-8 shadow-sm font-semibold text-xs"
          >
            <Plus className="mr-1.5 h-3.5 w-3.5" />
            Add Supplier
          </Button>
        </div>
      </div>

      {/* Expandable Search Input */}
      {showSearch && (
        <div className="relative">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by Firm, Brand, Ahmedabad Market, any of 5 Phones, GSTIN, Garment type..."
            className="pl-9 pr-8"
            autoFocus
          />
          {search && (
            <button
              onClick={() => setSearch("")}
              className="absolute right-3 top-2.5 text-muted-foreground hover:text-foreground"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>
      )}

      {/* Grid of Supplier Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {filteredSuppliers.length === 0 ? (
          <div className="col-span-full py-12 text-center text-xs text-muted-foreground">
            No suppliers found matching the criteria.
          </div>
        ) : (
          filteredSuppliers.map((sup) => {
            const extraPhonesCount = [sup.phone2, sup.phone3, sup.phone4, sup.phone5].filter(Boolean).length
            const marketDisplay = sup.marketArea || (sup.markets ? sup.markets.split(",")[0] : sup.city || "Ahmedabad")

            return (
              <Card
                key={sup.id}
                className="p-5 rounded-2xl hover:border-zinc-300 dark:hover:border-zinc-700 transition-all flex flex-col justify-between"
              >
                <div>
                  {/* Card Header */}
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-1.5">
                        <span>{sup.firmName || sup.name}</span>
                      </h3>
                      {sup.brand && (
                        <p className="text-xs font-semibold text-zinc-600 dark:text-zinc-400 mt-0.5 flex items-center gap-1">
                          <Tag className="h-3 w-3 text-zinc-400" />
                          <span>Brand: {sup.brand}</span>
                        </p>
                      )}
                      <p className="text-xs text-muted-foreground flex items-center gap-1 mt-1">
                        <MapPin className="h-3 w-3 text-zinc-400 shrink-0" />
                        <span className="truncate max-w-[200px]" title={sup.markets || marketDisplay}>
                          {marketDisplay}
                        </span>
                      </p>
                    </div>

                    <Badge
                      variant={
                        sup.type?.toLowerCase() === "manufacturer" ? "default" : "secondary"
                      }
                    >
                      {sup.type || "Supplier"}
                    </Badge>
                  </div>

                  {/* Garment Categories & Shops Tag */}
                  <div className="mt-3 flex flex-wrap items-center gap-1.5">
                    {sup.shopCount && sup.shopCount > 1 && (
                      <span className="inline-flex items-center gap-1 text-[10px] font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 px-2 py-0.5 rounded-full">
                        <Store className="h-2.5 w-2.5" />
                        {sup.shopCount} Shops
                      </span>
                    )}

                    {sup.categories && (
                      <span className="inline-flex items-center gap-1 text-[10px] font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 px-2 py-0.5 rounded-full truncate max-w-[220px]">
                        <Tag className="h-2.5 w-2.5 text-zinc-400" />
                        {sup.categories}
                      </span>
                    )}
                  </div>

                  {/* Contact Info & Up to 5 Phones */}
                  <div className="mt-3.5 space-y-1 border-t border-zinc-100 pt-3 dark:border-zinc-800 text-xs">
                    {sup.contactPerson && (
                      <p className="text-muted-foreground flex items-center gap-1">
                        <User className="h-3 w-3 text-zinc-400" />
                        <span>
                          Contact:{" "}
                          <strong className="text-zinc-800 dark:text-zinc-200">
                            {sup.contactPerson}
                          </strong>
                        </span>
                      </p>
                    )}

                    {sup.phone ? (
                      <div className="flex items-center justify-between">
                        <p className="text-muted-foreground flex items-center gap-1.5">
                          <Phone className="h-3 w-3 text-zinc-400" />
                          <span className="text-zinc-800 dark:text-zinc-200 font-medium">
                            {sup.phone}
                          </span>
                        </p>
                        {extraPhonesCount > 0 && (
                          <span className="text-[10px] font-semibold bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-300 px-1.5 py-0.5 rounded-full">
                            +{extraPhonesCount} more
                          </span>
                        )}
                      </div>
                    ) : (
                      <p className="text-muted-foreground text-[11px] italic">
                        No phone number registered
                      </p>
                    )}

                    {(sup.gstin || sup.gstNumber) && (
                      <p className="text-[11px] text-muted-foreground font-mono truncate">
                        GST: {sup.gstin || sup.gstNumber}
                      </p>
                    )}

                    {sup.referredBy && (
                      <p className="text-[11px] text-muted-foreground italic">
                        Ref: {sup.referredBy}
                      </p>
                    )}
                  </div>
                </div>

                {/* Bottom Actions */}
                <div className="mt-4 pt-2.5 border-t border-zinc-100 dark:border-zinc-800/80 flex items-center justify-between gap-2">
                  <Button
                    size="sm"
                    shape="pill"
                    variant="outline"
                    onClick={() => handleOpenSupplierInvoice(sup)}
                    className="text-xs h-7 px-2.5 font-medium text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-900/50 hover:bg-blue-50 dark:hover:bg-blue-950/40"
                    title="Generate Supplier Purchase Voucher / Invoice"
                  >
                    <Printer className="h-3 w-3 mr-1" />
                    Invoice
                  </Button>

                  <Button
                    size="sm"
                    shape="pill"
                    variant="outline"
                    onClick={() => setViewProfileSupplier(sup)}
                    className="flex-1 text-xs h-7 font-medium"
                  >
                    <Info className="h-3 w-3 mr-1" />
                    Profile
                  </Button>

                  <Button
                    size="sm"
                    shape="pill"
                    variant="ghost"
                    onClick={() => handleOpenEdit(sup)}
                    className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100"
                    title="Edit Supplier"
                  >
                    <Edit2 className="h-3.5 w-3.5" />
                  </Button>

                  <Button
                    size="sm"
                    shape="pill"
                    variant="ghost"
                    onClick={() => handleDelete(sup.id)}
                    className="h-7 w-7 p-0 text-red-400 hover:text-red-600"
                    title="Delete Supplier"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </Card>
            )
          })
        )}
      </div>

      {/* Add / Edit Supplier Multi-Tab Dialog */}
      <Dialog
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
        title={editingId ? "Edit Supplier / Mill Profile" : "Register New Supplier / Mill"}
        description="Comprehensive supplier directory for mills, brands, and Ahmedabad market shops"
      >
        <div className="space-y-4 pt-1">
          {/* Form Tabs */}
          <Tabs
            value={activeFormTab}
            onValueChange={setActiveFormTab}
            options={[
              { value: "basic", label: "1. Firm & Brand" },
              { value: "contact", label: "2. Contact & 5 Phones" },
              { value: "address", label: "3. Address & Shops" },
              { value: "markets", label: "4. Markets & Garments" },
            ]}
          />

          {/* TAB 1: Firm & Brand */}
          {activeFormTab === "basic" && (
            <div className="space-y-3.5">
              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Firm / Mill / Trade Name <span className="text-red-500">*</span>
                </label>
                <Input
                  value={firmName}
                  onChange={(e) => {
                    setFirmName(e.target.value)
                    setName(e.target.value)
                  }}
                  placeholder="e.g. Radheshyam Textile Mills Pvt Ltd"
                  className="mt-1"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Brand Name
                  </label>
                  <Input
                    value={brand}
                    onChange={(e) => setBrand(e.target.value)}
                    placeholder="e.g. RADHE / RT-FAB"
                    className="mt-1"
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    GSTIN
                  </label>
                  <Input
                    value={gstin}
                    onChange={(e) => setGstin(e.target.value.toUpperCase())}
                    placeholder="e.g. 24AAAAA0000A1Z5"
                    className="mt-1 font-mono uppercase"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Supplier Category / Nature
                </label>
                <div className="mt-1 flex gap-2">
                  {["Manufacturer", "Wholesaler", "Trader", "Semi-Wholesaler"].map((t) => (
                    <Button
                      key={t}
                      size="sm"
                      shape="pill"
                      variant={type === t ? "default" : "outline"}
                      onClick={() => setType(t)}
                      className="flex-1 text-xs"
                    >
                      {t}
                    </Button>
                  ))}
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Key Contact Person / Proprietor
                </label>
                <Input
                  value={contactPerson}
                  onChange={(e) => setContactPerson(e.target.value)}
                  placeholder="e.g. Arvind Bhai Patel / Mukesh Bhai"
                  className="mt-1"
                />
              </div>
            </div>
          )}

          {/* TAB 2: Contact & Up to 5 Phones */}
          {activeFormTab === "contact" && (
            <div className="space-y-3.5 max-h-[350px] overflow-y-auto pr-1">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Contact Phone Numbers (Up to 5)
                </label>
                {phoneCount < 5 && (
                  <Button
                    size="sm"
                    variant="outline"
                    shape="pill"
                    onClick={() => setPhoneCount((prev) => Math.min(5, prev + 1))}
                    className="h-6 text-[11px] px-2"
                  >
                    <Plus className="h-3 w-3 mr-1" /> Add Another Phone
                  </Button>
                )}
              </div>

              {/* Phone 1 */}
              <div>
                <label className="text-[11px] text-muted-foreground">
                  Phone 1 (Primary / WhatsApp) <span className="text-red-500">*</span>
                </label>
                <Input
                  value={phone1}
                  onChange={(e) => setPhone1(e.target.value)}
                  placeholder="e.g. 9825100000"
                  className="mt-1"
                />
              </div>

              {/* Phone 2 */}
              {phoneCount >= 2 && (
                <div>
                  <label className="text-[11px] text-muted-foreground">
                    Phone 2 (Office / Order Desk)
                  </label>
                  <Input
                    value={phone2}
                    onChange={(e) => setPhone2(e.target.value)}
                    placeholder="e.g. 9825100001"
                    className="mt-1"
                  />
                </div>
              )}

              {/* Phone 3 */}
              {phoneCount >= 3 && (
                <div>
                  <label className="text-[11px] text-muted-foreground">
                    Phone 3 (Accountant / Billing)
                  </label>
                  <Input
                    value={phone3}
                    onChange={(e) => setPhone3(e.target.value)}
                    placeholder="e.g. 9825100002"
                    className="mt-1"
                  />
                </div>
              )}

              {/* Phone 4 */}
              {phoneCount >= 4 && (
                <div>
                  <label className="text-[11px] text-muted-foreground">
                    Phone 4 (Godown / Dispatch)
                  </label>
                  <Input
                    value={phone4}
                    onChange={(e) => setPhone4(e.target.value)}
                    placeholder="e.g. 9825100003"
                    className="mt-1"
                  />
                </div>
              )}

              {/* Phone 5 */}
              {phoneCount >= 5 && (
                <div>
                  <label className="text-[11px] text-muted-foreground">
                    Phone 5 (Residence / Personal)
                  </label>
                  <Input
                    value={phone5}
                    onChange={(e) => setPhone5(e.target.value)}
                    placeholder="e.g. 9825100004"
                    className="mt-1"
                  />
                </div>
              )}

              {/* Emails */}
              <div className="pt-2 border-t border-zinc-100 dark:border-zinc-800 space-y-3">
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Email Addresses
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-[11px] text-muted-foreground">Primary Email</label>
                    <Input
                      value={email1}
                      onChange={(e) => setEmail1(e.target.value)}
                      placeholder="e.g. sales@radhemills.com"
                      className="mt-1 text-xs"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] text-muted-foreground">Secondary / Accounts Email</label>
                    <Input
                      value={email2}
                      onChange={(e) => setEmail2(e.target.value)}
                      placeholder="e.g. accounts@radhemills.com"
                      className="mt-1 text-xs"
                    />
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: Addresses & Shops */}
          {activeFormTab === "address" && (
            <div className="space-y-3.5 max-h-[350px] overflow-y-auto pr-1">
              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Office / Registered Business Address
                </label>
                <Input
                  value={officeAddress}
                  onChange={(e) => setOfficeAddress(e.target.value)}
                  placeholder="e.g. 3rd Floor, Radha Krishna Complex, Ring Road"
                  className="mt-1"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Office / Shop Location (Landmark / Maps Link)
                </label>
                <Input
                  value={officeLocation}
                  onChange={(e) => setOfficeLocation(e.target.value)}
                  placeholder="e.g. Near Kalupur Overbridge, Opposite Maskati Gate"
                  className="mt-1"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Home / Factory Address
                </label>
                <Input
                  value={homeAddress}
                  onChange={(e) => setHomeAddress(e.target.value)}
                  placeholder="e.g. Plot 44, Narol GIDC Phase 2, Ahmedabad"
                  className="mt-1"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Personal Location / Residence Area
                </label>
                <Input
                  value={personalLocation}
                  onChange={(e) => setPersonalLocation(e.target.value)}
                  placeholder="e.g. Bodakdev / Paldi / Maninagar, Ahmedabad"
                  className="mt-1"
                />
              </div>

              <div className="grid grid-cols-2 gap-3 pt-2 border-t border-zinc-100 dark:border-zinc-800">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    How Many Shops / Outlets?
                  </label>
                  <Input
                    type="number"
                    min="1"
                    max="50"
                    value={shopCount}
                    onChange={(e) => setShopCount(e.target.value)}
                    placeholder="e.g. 2"
                    className="mt-1"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Shop / Branch Locations
                  </label>
                  <Input
                    value={shopLocations}
                    onChange={(e) => setShopLocations(e.target.value)}
                    placeholder="e.g. Shop 12 New Cloth Mkt, Shop 45 Maskati"
                    className="mt-1"
                  />
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: Ahmedabad Markets & Garment Types */}
          {activeFormTab === "markets" && (
            <div className="space-y-3.5 max-h-[350px] overflow-y-auto pr-1">
              <div>
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Ahmedabad Textile Markets (Pre-provided)
                  </label>
                  <span className="text-[10px] text-muted-foreground">Click to select markets</span>
                </div>
                <div className="mt-2 flex flex-wrap gap-1.5 max-h-32 overflow-y-auto p-2 rounded-xl bg-zinc-50 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800">
                  {AHMEDABAD_TEXTILE_MARKETS.map((mkt) => {
                    const isSelected = selectedMarkets.includes(mkt)
                    return (
                      <button
                        key={mkt}
                        type="button"
                        onClick={() => handleToggleMarket(mkt)}
                        className={`text-[11px] px-2.5 py-1 rounded-full border transition-all flex items-center gap-1 ${
                          isSelected
                            ? "bg-zinc-900 text-white border-zinc-900 dark:bg-zinc-100 dark:text-zinc-900 dark:border-zinc-100 font-semibold shadow-sm"
                            : "bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 border-zinc-200 dark:border-zinc-700 hover:border-zinc-400"
                        }`}
                      >
                        {isSelected && <Check className="h-2.5 w-2.5" />}
                        <span>{mkt}</span>
                      </button>
                    )
                  })}
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Garment Types / Categories Sold
                  </label>
                  <span className="text-[10px] text-muted-foreground">Tap categories</span>
                </div>
                <div className="mt-2 flex flex-wrap gap-1.5 max-h-28 overflow-y-auto p-2 rounded-xl bg-zinc-50 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800">
                  {GARMENT_CATEGORIES.map((cat) => {
                    const isSelected = selectedCategories.includes(cat)
                    return (
                      <button
                        key={cat}
                        type="button"
                        onClick={() => handleToggleCategory(cat)}
                        className={`text-[11px] px-2.5 py-1 rounded-full border transition-all flex items-center gap-1 ${
                          isSelected
                            ? "bg-zinc-900 text-white border-zinc-900 dark:bg-zinc-100 dark:text-zinc-900 dark:border-zinc-100 font-semibold"
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
                    placeholder="+ Custom category (e.g. Rayon Kurti 14kg)"
                    className="text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 pt-2 border-t border-zinc-100 dark:border-zinc-800">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Referred By
                  </label>
                  <Input
                    value={referredBy}
                    onChange={(e) => setReferredBy(e.target.value)}
                    placeholder="e.g. Ramesh Bhai / Balaji Sarees"
                    className="mt-1"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    City / Hub
                  </label>
                  <Input
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="e.g. Ahmedabad"
                    className="mt-1"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Special Notes / Terms
                </label>
                <Input
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="e.g. 5% cash discount on 7 days payment"
                  className="mt-1 text-xs"
                />
              </div>
            </div>
          )}

          {/* Dialog Action Buttons */}
          <div className="flex items-center justify-between pt-3 border-t border-zinc-100 dark:border-zinc-800">
            <div className="flex gap-1.5">
              {activeFormTab !== "basic" && (
                <Button
                  variant="outline"
                  shape="pill"
                  size="sm"
                  onClick={() => {
                    if (activeFormTab === "markets") setActiveFormTab("address")
                    else if (activeFormTab === "address") setActiveFormTab("contact")
                    else if (activeFormTab === "contact") setActiveFormTab("basic")
                  }}
                  className="h-8 text-xs"
                >
                  Previous
                </Button>
              )}
              {activeFormTab !== "markets" && (
                <Button
                  variant="secondary"
                  shape="pill"
                  size="sm"
                  onClick={() => {
                    if (activeFormTab === "basic") setActiveFormTab("contact")
                    else if (activeFormTab === "contact") setActiveFormTab("address")
                    else if (activeFormTab === "address") setActiveFormTab("markets")
                  }}
                  className="h-8 text-xs"
                >
                  Next Step
                </Button>
              )}
            </div>

            <div className="flex gap-2">
              <Button
                variant="outline"
                shape="pill"
                size="sm"
                onClick={() => setIsDialogOpen(false)}
                className="h-8 text-xs"
              >
                Cancel
              </Button>
              <Button
                shape="pill"
                size="sm"
                onClick={handleSave}
                className="h-8 text-xs font-semibold shadow-sm"
              >
                {editingId ? "Update Supplier" : "Save Supplier"}
              </Button>
            </div>
          </div>
        </div>
      </Dialog>

      {/* View Full Profile Modal */}
      {viewProfileSupplier && (
        <Dialog
          open={!!viewProfileSupplier}
          onOpenChange={(open) => !open && setViewProfileSupplier(null)}
          title={`${viewProfileSupplier.firmName || viewProfileSupplier.name}`}
          description={`Supplier ID: ${viewProfileSupplier.supplierId || "SUP"} • Nature: ${viewProfileSupplier.type || "Manufacturer"}`}
        >
          <div className="space-y-4 pt-1 max-h-[420px] overflow-y-auto pr-1 text-xs">
            {/* Identity Banner */}
            <div className="rounded-xl p-3 bg-zinc-100/70 dark:bg-zinc-900 border border-zinc-200/70 dark:border-zinc-800 flex items-center justify-between">
              <div>
                <p className="font-bold text-sm text-zinc-900 dark:text-zinc-100">
                  {viewProfileSupplier.firmName || viewProfileSupplier.name}
                </p>
                {viewProfileSupplier.brand && (
                  <p className="text-zinc-600 dark:text-zinc-400 font-semibold mt-0.5">
                    Brand: {viewProfileSupplier.brand}
                  </p>
                )}
                {viewProfileSupplier.contactPerson && (
                  <p className="text-muted-foreground mt-0.5">
                    Proprietor / Key Contact: <strong>{viewProfileSupplier.contactPerson}</strong>
                  </p>
                )}
              </div>
              <Badge variant="default">{viewProfileSupplier.type || "Supplier"}</Badge>
            </div>

            {/* GSTIN & Referral */}
            <div className="grid grid-cols-2 gap-2">
              <div className="p-2.5 rounded-xl border border-zinc-200 dark:border-zinc-800">
                <span className="text-[10px] text-muted-foreground">GSTIN</span>
                <p className="font-mono font-bold text-zinc-900 dark:text-zinc-100">
                  {viewProfileSupplier.gstin || viewProfileSupplier.gstNumber || "Not Provided"}
                </p>
              </div>
              <div className="p-2.5 rounded-xl border border-zinc-200 dark:border-zinc-800">
                <span className="text-[10px] text-muted-foreground">Referred By</span>
                <p className="font-bold text-zinc-900 dark:text-zinc-100">
                  {viewProfileSupplier.referredBy || "Direct Walk-in"}
                </p>
              </div>
            </div>

            {/* All Contact Phone Numbers */}
            <div className="p-3 rounded-xl border border-zinc-200 dark:border-zinc-800 space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1.5">
                  <Phone className="h-3.5 w-3.5 text-zinc-500" />
                  <span>Registered Contact Numbers</span>
                </span>
                <span className="text-[10px] text-muted-foreground">Up to 5 lines</span>
              </div>
              <div className="grid grid-cols-2 gap-2 text-xs">
                {viewProfileSupplier.phone && (
                  <div className="bg-zinc-50 dark:bg-zinc-900 p-2 rounded-lg">
                    <span className="text-[10px] text-muted-foreground">Phone 1 (Primary):</span>
                    <p className="font-bold text-zinc-900 dark:text-zinc-100">{viewProfileSupplier.phone}</p>
                  </div>
                )}
                {viewProfileSupplier.phone2 && (
                  <div className="bg-zinc-50 dark:bg-zinc-900 p-2 rounded-lg">
                    <span className="text-[10px] text-muted-foreground">Phone 2 (Office):</span>
                    <p className="font-bold text-zinc-900 dark:text-zinc-100">{viewProfileSupplier.phone2}</p>
                  </div>
                )}
                {viewProfileSupplier.phone3 && (
                  <div className="bg-zinc-50 dark:bg-zinc-900 p-2 rounded-lg">
                    <span className="text-[10px] text-muted-foreground">Phone 3 (Accounts):</span>
                    <p className="font-bold text-zinc-900 dark:text-zinc-100">{viewProfileSupplier.phone3}</p>
                  </div>
                )}
                {viewProfileSupplier.phone4 && (
                  <div className="bg-zinc-50 dark:bg-zinc-900 p-2 rounded-lg">
                    <span className="text-[10px] text-muted-foreground">Phone 4 (Godown):</span>
                    <p className="font-bold text-zinc-900 dark:text-zinc-100">{viewProfileSupplier.phone4}</p>
                  </div>
                )}
                {viewProfileSupplier.phone5 && (
                  <div className="bg-zinc-50 dark:bg-zinc-900 p-2 rounded-lg">
                    <span className="text-[10px] text-muted-foreground">Phone 5 (Residence):</span>
                    <p className="font-bold text-zinc-900 dark:text-zinc-100">{viewProfileSupplier.phone5}</p>
                  </div>
                )}
              </div>
              {(viewProfileSupplier.email || viewProfileSupplier.email2) && (
                <div className="pt-2 border-t border-zinc-100 dark:border-zinc-800/60 text-muted-foreground space-y-0.5">
                  {viewProfileSupplier.email && <p>Email: {viewProfileSupplier.email}</p>}
                  {viewProfileSupplier.email2 && <p>Alt Email: {viewProfileSupplier.email2}</p>}
                </div>
              )}
            </div>

            {/* Addresses & Locations */}
            <div className="p-3 rounded-xl border border-zinc-200 dark:border-zinc-800 space-y-2">
              <span className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1.5">
                <MapPin className="h-3.5 w-3.5 text-zinc-500" />
                <span>Addresses & Shop Locations</span>
              </span>
              <div className="space-y-1.5 text-xs text-muted-foreground">
                {(viewProfileSupplier.officeAddress || viewProfileSupplier.address) && (
                  <p>
                    <strong className="text-zinc-700 dark:text-zinc-300">Office / Mill Address:</strong>{" "}
                    {viewProfileSupplier.officeAddress || viewProfileSupplier.address}
                  </p>
                )}
                {viewProfileSupplier.officeLocation && (
                  <p>
                    <strong className="text-zinc-700 dark:text-zinc-300">Office Location / Landmark:</strong>{" "}
                    {viewProfileSupplier.officeLocation}
                  </p>
                )}
                {viewProfileSupplier.homeAddress && (
                  <p>
                    <strong className="text-zinc-700 dark:text-zinc-300">Home / Factory Address:</strong>{" "}
                    {viewProfileSupplier.homeAddress}
                  </p>
                )}
                {viewProfileSupplier.personalLocation && (
                  <p>
                    <strong className="text-zinc-700 dark:text-zinc-300">Personal Location / Area:</strong>{" "}
                    {viewProfileSupplier.personalLocation}
                  </p>
                )}
                <div className="pt-1.5 border-t border-zinc-100 dark:border-zinc-800/60 flex items-center justify-between">
                  <span>Number of Shops Owned: <strong>{viewProfileSupplier.shopCount || 1}</strong></span>
                  {viewProfileSupplier.shopLocations && (
                    <span className="text-[11px] truncate max-w-[200px]">
                      Branches: {viewProfileSupplier.shopLocations}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Ahmedabad Markets & Garments */}
            <div className="p-3 rounded-xl border border-zinc-200 dark:border-zinc-800 space-y-2">
              <span className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1.5">
                <Building2 className="h-3.5 w-3.5 text-zinc-500" />
                <span>Ahmedabad Markets & Products</span>
              </span>
              <div>
                <span className="text-[10px] text-muted-foreground">Markets:</span>
                <p className="font-medium text-zinc-800 dark:text-zinc-200 mt-0.5">
                  {viewProfileSupplier.markets || viewProfileSupplier.marketArea || "Ahmedabad Central"}
                </p>
              </div>
              {(viewProfileSupplier.categories || viewProfileSupplier.garmentTypes) && (
                <div className="pt-1.5 border-t border-zinc-100 dark:border-zinc-800/60">
                  <span className="text-[10px] text-muted-foreground">Garment Categories:</span>
                  <p className="font-medium text-zinc-800 dark:text-zinc-200 mt-0.5">
                    {viewProfileSupplier.categories || viewProfileSupplier.garmentTypes}
                  </p>
                </div>
              )}
              {viewProfileSupplier.notes && (
                <div className="pt-1.5 border-t border-zinc-100 dark:border-zinc-800/60">
                  <span className="text-[10px] text-muted-foreground">Terms / Notes:</span>
                  <p className="italic text-muted-foreground mt-0.5">{viewProfileSupplier.notes}</p>
                </div>
              )}
            </div>

            {/* Modal Bottom Actions */}
            <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-zinc-100 dark:border-zinc-800">
              <Button
                variant="outline"
                shape="pill"
                size="sm"
                onClick={() => handleOpenSupplierInvoice(viewProfileSupplier)}
                className="text-xs font-semibold gap-1.5 text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-900/50 hover:bg-blue-50 dark:hover:bg-blue-950/40"
              >
                <Printer className="h-3.5 w-3.5" />
                <span>Supplier Invoice (PDF)</span>
              </Button>

              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  shape="pill"
                  size="sm"
                  onClick={() => setViewProfileSupplier(null)}
                >
                  Close
                </Button>
                <Button
                  shape="pill"
                  size="sm"
                  onClick={() => {
                    const s = viewProfileSupplier
                    setViewProfileSupplier(null)
                    handleOpenEdit(s)
                  }}
                >
                  Edit Details
                </Button>
              </div>
            </div>
          </div>
        </Dialog>
      )}

      {/* Report Viewer Modal */}
      <ReportViewerModal
        open={reportModal.open}
        onOpenChange={(open) => setReportModal((prev) => ({ ...prev, open }))}
        title={reportModal.title}
        htmlContent={reportModal.html}
        whatsAppText={reportModal.whatsAppText}
      />
    </div>
  )
}
