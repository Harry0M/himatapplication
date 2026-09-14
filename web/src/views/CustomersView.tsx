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
  FileText
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatInr } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Tabs } from "../components/ui/Tabs"
import { Customer, Visit } from "../types"
import { AHMEDABAD_TEXTILE_MARKETS, GARMENT_CATEGORIES } from "../lib/constants"
import { ReportViewerModal } from "../components/ui/ReportViewerModal"
import {
  generateCustomerDayReportHtml,
  buildCustomerReportWhatsAppText,
} from "../lib/pdfReports"

export function CustomersView() {
  const {
    customers,
    visits,
    entries,
    employees,
    packGroups,
    saveCustomer,
    deleteCustomer,
  } = useData()
  const [search, setSearch] = useState<string>("")
  const [showSearch, setShowSearch] = useState<boolean>(false)

  // Modal States
  const [isDialogOpen, setIsDialogOpen] = useState<boolean>(false)
  const [activeFormTab, setActiveFormTab] = useState<string>("basic")
  const [viewProfileCustomer, setViewProfileCustomer] = useState<Customer | null>(null)

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
      title: `Customer Statement & Report: ${c.name}`,
      html,
      whatsAppText,
    })
  }

  // Form State
  const [editingId, setEditingId] = useState<number | null>(null)
  const [name, setName] = useState<string>("")
  const [firmName, setFirmName] = useState<string>("")
  const [gstin, setGstin] = useState<string>("")

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
  const [shopAddress, setShopAddress] = useState<string>("")
  const [shopLocation, setShopLocation] = useState<string>("")
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
  const [creditDays, setCreditDays] = useState<string>("30")
  const [creditLimit, setCreditLimit] = useState<string>("")
  const [notes, setNotes] = useState<string>("")

  // Open Add Dialog
  const handleOpenAdd = () => {
    setEditingId(null)
    setName("")
    setFirmName("")
    setGstin("")
    setPhone1("")
    setPhone2("")
    setPhone3("")
    setPhone4("")
    setPhone5("")
    setPhoneCount(1)
    setEmail1("")
    setEmail2("")
    setShopAddress("")
    setShopLocation("")
    setHomeAddress("")
    setPersonalLocation("")
    setShopCount("1")
    setShopLocations("")
    setSelectedMarkets(["Maskati Cloth Market (Sakarkalupur)"])
    setCity("Ahmedabad")
    setSelectedCategories([])
    setCustomCategory("")
    setReferredBy("")
    setCreditDays("30")
    setCreditLimit("")
    setNotes("")
    setActiveFormTab("basic")
    setIsDialogOpen(true)
  }

  // Open Edit Dialog
  const handleOpenEdit = (c: Customer) => {
    setEditingId(c.id)
    setName(c.name || "")
    setFirmName(c.firmName || "")
    setGstin(c.gstin || c.gstNumber || "")

    setPhone1(c.phone || "")
    setPhone2(c.phone2 || "")
    setPhone3(c.phone3 || "")
    setPhone4(c.phone4 || "")
    setPhone5(c.phone5 || "")
    const count = [c.phone, c.phone2, c.phone3, c.phone4, c.phone5].filter(Boolean).length
    setPhoneCount(Math.max(1, count))

    setEmail1(c.email || "")
    setEmail2(c.email2 || "")

    setShopAddress(c.shopAddress || c.address || "")
    setShopLocation(c.shopLocation || "")
    setHomeAddress(c.homeAddress || "")
    setPersonalLocation(c.personalLocation || "")
    setShopCount(String(c.shopCount || 1))
    setShopLocations(c.shopLocations || "")

    const mkts = c.markets
      ? c.markets.split(",").map((m) => m.trim()).filter(Boolean)
      : c.marketArea
      ? [c.marketArea]
      : []
    setSelectedMarkets(mkts.length > 0 ? mkts : ["Maskati Cloth Market (Sakarkalupur)"])
    setCity(c.city || "Ahmedabad")

    const cats = (c.preferredCategories || "")
      .split(",")
      .map((cat) => cat.trim())
      .filter(Boolean)
    setSelectedCategories(cats)
    setCustomCategory("")
    setReferredBy(c.referredBy || "")
    setCreditDays(String(c.creditDays || 30))
    setCreditLimit(c.creditLimit ? String(c.creditLimit) : "")
    setNotes(c.notes || "")
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
    const finalName = name.trim() || firmName.trim()
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

    const newCustomer: Customer = {
      id,
      customerId: editingId
        ? (customers.find((c) => c.id === editingId)?.customerId || `CUST-${id % 10000}`)
        : `CUST-${id % 10000}`,
      name: finalName,
      firmName: firmName.trim() || finalName,
      phone: primaryPhone,
      phone2: phone2.trim() || "",
      phone3: phone3.trim() || "",
      phone4: phone4.trim() || "",
      phone5: phone5.trim() || "",
      phones: allPhones,
      email: primaryEmail || "",
      email2: email2.trim() || "",
      emails: allEmails,
      address: shopAddress.trim() || "",
      shopAddress: shopAddress.trim() || "",
      homeAddress: homeAddress.trim() || "",
      shopLocation: shopLocation.trim() || "",
      personalLocation: personalLocation.trim() || "",
      shopCount: parseInt(shopCount, 10) || 1,
      shopLocations: shopLocations.trim() || "",
      marketArea: selectedMarkets[0] || "Maskati Cloth Market (Sakarkalupur)",
      markets: selectedMarkets.join(", "),
      city: city.trim() || "Ahmedabad",
      gstin: gstin.trim().toUpperCase(),
      gstNumber: gstin.trim().toUpperCase(),
      preferredCategories: allCats.join(", "),
      referredBy: referredBy.trim() || "",
      creditDays: parseInt(creditDays, 10) || 30,
      creditLimit: creditLimit ? parseFloat(creditLimit) : 0,
      notes: notes.trim() || "",
    }

    await saveCustomer(newCustomer)
    setIsDialogOpen(false)
  }

  const handleDelete = async (id: number) => {
    if (window.confirm("Are you sure you want to remove this customer record?")) {
      await deleteCustomer(id)
      if (viewProfileCustomer?.id === id) {
        setViewProfileCustomer(null)
      }
    }
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
      c.phone3?.includes(q) ||
      c.phone4?.includes(q) ||
      c.phone5?.includes(q) ||
      c.gstNumber?.toLowerCase().includes(q) ||
      c.gstin?.toLowerCase().includes(q) ||
      c.customerId?.toLowerCase().includes(q) ||
      c.marketArea?.toLowerCase().includes(q) ||
      c.markets?.toLowerCase().includes(q) ||
      c.city?.toLowerCase().includes(q) ||
      c.preferredCategories?.toLowerCase().includes(q) ||
      c.referredBy?.toLowerCase().includes(q)
  )

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
            <span>Customers & Retail Clients Directory</span>
          </h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            {customers.length} registered textile buyers, boutique owners, and retail shops.
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

          <Button
            shape="pill"
            size="sm"
            onClick={handleOpenAdd}
            className="h-8 shadow-sm font-semibold text-xs"
          >
            <Plus className="mr-1.5 h-3.5 w-3.5" />
            Add Customer
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
            placeholder="Search by customer name, firm name, any of 5 phones, GSTIN, Ahmedabad market, garment category..."
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

      {/* Grid of Customer Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {filteredCustomers.length === 0 ? (
          <div className="col-span-full py-12 text-center text-xs text-muted-foreground">
            No customers found matching the search criteria.
          </div>
        ) : (
          filteredCustomers.map((cust) => {
            const extraPhonesCount = [cust.phone2, cust.phone3, cust.phone4, cust.phone5].filter(Boolean).length
            const marketDisplay = cust.marketArea || (cust.markets ? cust.markets.split(",")[0] : cust.city || "Ahmedabad")

            return (
              <Card
                key={cust.id}
                className="p-5 rounded-2xl hover:border-zinc-300 dark:hover:border-zinc-700 transition-all flex flex-col justify-between"
              >
                <div>
                  {/* Card Header */}
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-1.5">
                        <span>{cust.firmName || cust.name}</span>
                      </h3>
                      {cust.firmName && cust.name && cust.firmName !== cust.name && (
                        <p className="text-xs text-zinc-600 dark:text-zinc-400 font-medium mt-0.5">
                          Owner: {cust.name}
                        </p>
                      )}
                      <p className="text-xs text-muted-foreground flex items-center gap-1 mt-1">
                        <MapPin className="h-3 w-3 text-zinc-400 shrink-0" />
                        <span className="truncate max-w-[200px]" title={cust.markets || marketDisplay}>
                          {marketDisplay}
                        </span>
                      </p>
                    </div>

                    {cust.customerId && (
                      <span className="text-[10px] font-mono font-semibold bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-300 px-2 py-0.5 rounded-full">
                        {cust.customerId}
                      </span>
                    )}
                  </div>

                  {/* Garment Categories & Shops Tag */}
                  <div className="mt-3 flex flex-wrap items-center gap-1.5">
                    {cust.shopCount && cust.shopCount > 1 && (
                      <span className="inline-flex items-center gap-1 text-[10px] font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 px-2 py-0.5 rounded-full">
                        <Store className="h-2.5 w-2.5" />
                        {cust.shopCount} Outlets
                      </span>
                    )}

                    {cust.preferredCategories && (
                      <span className="inline-flex items-center gap-1 text-[10px] font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 px-2 py-0.5 rounded-full truncate max-w-[220px]">
                        <Tag className="h-2.5 w-2.5 text-zinc-400" />
                        {cust.preferredCategories}
                      </span>
                    )}
                  </div>

                  {/* Contact Info & Up to 5 Phones */}
                  <div className="mt-3.5 space-y-1 border-t border-zinc-100 pt-3 dark:border-zinc-800 text-xs">
                    {cust.phone ? (
                      <div className="flex items-center justify-between">
                        <p className="text-muted-foreground flex items-center gap-1.5">
                          <Phone className="h-3 w-3 text-zinc-400" />
                          <span className="text-zinc-800 dark:text-zinc-200 font-medium">
                            {cust.phone}
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

                    {(cust.gstin || cust.gstNumber) && (
                      <p className="text-[11px] text-muted-foreground font-mono truncate">
                        GST: {cust.gstin || cust.gstNumber}
                      </p>
                    )}

                    {cust.creditDays && (
                      <p className="text-[11px] text-muted-foreground">
                        Credit Terms: <strong>{cust.creditDays} Days</strong>
                        {cust.creditLimit ? ` • Limit: ₹${formatInr(cust.creditLimit)}` : ""}
                      </p>
                    )}

                    {cust.referredBy && (
                      <p className="text-[11px] text-muted-foreground italic">
                        Ref: {cust.referredBy}
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
                    onClick={() => handleGenerateCustomerReport(cust)}
                    className="text-xs h-7 px-2.5 font-medium text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-900/50 hover:bg-blue-50 dark:hover:bg-blue-950/40"
                    title="Generate Customer Day Report & Statement"
                  >
                    <Printer className="h-3 w-3 mr-1" />
                    Report
                  </Button>

                  <Button
                    size="sm"
                    shape="pill"
                    variant="outline"
                    onClick={() => setViewProfileCustomer(cust)}
                    className="flex-1 text-xs h-7 font-medium"
                  >
                    <Info className="h-3 w-3 mr-1" />
                    Profile
                  </Button>

                  <Button
                    size="sm"
                    shape="pill"
                    variant="ghost"
                    onClick={() => handleOpenEdit(cust)}
                    className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100"
                    title="Edit Customer"
                  >
                    <Edit2 className="h-3.5 w-3.5" />
                  </Button>

                  <Button
                    size="sm"
                    shape="pill"
                    variant="ghost"
                    onClick={() => handleDelete(cust.id)}
                    className="h-7 w-7 p-0 text-red-400 hover:text-red-600"
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

      {/* Add / Edit Customer Multi-Tab Dialog */}
      <Dialog
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
        title={editingId ? "Edit Customer / Buyer Profile" : "Register New Customer"}
        description="Comprehensive client directory with multiple contacts, shops, and Ahmedabad markets"
      >
        <div className="space-y-4 pt-1">
          {/* Form Tabs */}
          <Tabs
            value={activeFormTab}
            onValueChange={setActiveFormTab}
            options={[
              { value: "basic", label: "1. Buyer & Firm" },
              { value: "contact", label: "2. Contact & 5 Phones" },
              { value: "address", label: "3. Address & Shops" },
              { value: "markets", label: "4. Markets & Credit" },
            ]}
          />

          {/* TAB 1: Buyer & Firm */}
          {activeFormTab === "basic" && (
            <div className="space-y-3.5">
              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Shop / Firm Name <span className="text-red-500">*</span>
                </label>
                <Input
                  value={firmName}
                  onChange={(e) => setFirmName(e.target.value)}
                  placeholder="e.g. Balaji Sarees & Garments"
                  className="mt-1"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Proprietor / Contact Person Name
                  </label>
                  <Input
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="e.g. Ramesh Bhai Patel"
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
                    placeholder="e.g. 24ABCDE1234F1Z5"
                    className="mt-1 font-mono uppercase"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    City / Native Hub
                  </label>
                  <Input
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="e.g. Ahmedabad"
                    className="mt-1"
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Referred By / Introducer
                  </label>
                  <Input
                    value={referredBy}
                    onChange={(e) => setReferredBy(e.target.value)}
                    placeholder="e.g. Arvind Bhai / Sunil Verma (Salesman)"
                    className="mt-1"
                  />
                </div>
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
                  placeholder="e.g. 9876543210"
                  className="mt-1"
                />
              </div>

              {/* Phone 2 */}
              {phoneCount >= 2 && (
                <div>
                  <label className="text-[11px] text-muted-foreground">
                    Phone 2 (Shop / Counter Desk)
                  </label>
                  <Input
                    value={phone2}
                    onChange={(e) => setPhone2(e.target.value)}
                    placeholder="e.g. 9876543211"
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
                    placeholder="e.g. 9876543212"
                    className="mt-1"
                  />
                </div>
              )}

              {/* Phone 4 */}
              {phoneCount >= 4 && (
                <div>
                  <label className="text-[11px] text-muted-foreground">
                    Phone 4 (Partner / Manager)
                  </label>
                  <Input
                    value={phone4}
                    onChange={(e) => setPhone4(e.target.value)}
                    placeholder="e.g. 9876543213"
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
                    placeholder="e.g. 9876543214"
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
                      placeholder="e.g. purchase@balajisarees.com"
                      className="mt-1 text-xs"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] text-muted-foreground">Accounts / Billing Email</label>
                    <Input
                      value={email2}
                      onChange={(e) => setEmail2(e.target.value)}
                      placeholder="e.g. accounts@balajisarees.com"
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
                  Shop / Business Address
                </label>
                <Input
                  value={shopAddress}
                  onChange={(e) => setShopAddress(e.target.value)}
                  placeholder="e.g. Shop 24, Ground Floor, Maskati Cloth Market"
                  className="mt-1"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Shop Location (Landmark / Google Maps Link)
                </label>
                <Input
                  value={shopLocation}
                  onChange={(e) => setShopLocation(e.target.value)}
                  placeholder="e.g. Near Sakarkalupur Police Chowki, Gate 2"
                  className="mt-1"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Home / Residence Address
                </label>
                <Input
                  value={homeAddress}
                  onChange={(e) => setHomeAddress(e.target.value)}
                  placeholder="e.g. Bungalow 12, Shanti Nagar, Paldi, Ahmedabad"
                  className="mt-1"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Personal Location / Native Town
                </label>
                <Input
                  value={personalLocation}
                  onChange={(e) => setPersonalLocation(e.target.value)}
                  placeholder="e.g. Paldi / Nadiad / Mehsana"
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
                    placeholder="e.g. 1"
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
                    placeholder="e.g. Maskati Mkt & 1 Branch in Bapunagar"
                    className="mt-1"
                  />
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: Ahmedabad Markets, Garments & Credit */}
          {activeFormTab === "markets" && (
            <div className="space-y-3.5 max-h-[350px] overflow-y-auto pr-1">
              <div>
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Ahmedabad Textile Markets (Pre-provided)
                  </label>
                  <span className="text-[10px] text-muted-foreground">Select buyer's markets</span>
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
                    Garment Categories Preferred / Bought
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
                    placeholder="+ Custom category (e.g. Rayon Kurti, Silk Dupattas)"
                    className="text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 pt-2 border-t border-zinc-100 dark:border-zinc-800">
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Credit Period (Days)
                  </label>
                  <Input
                    type="number"
                    value={creditDays}
                    onChange={(e) => setCreditDays(e.target.value)}
                    placeholder="e.g. 30"
                    className="mt-1"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    Credit Limit (₹ Optional)
                  </label>
                  <Input
                    type="number"
                    value={creditLimit}
                    onChange={(e) => setCreditLimit(e.target.value)}
                    placeholder="e.g. 200000"
                    className="mt-1"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Special Notes / Payment Terms
                </label>
                <Input
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="e.g. Good party, pays within 21 days with cheque"
                  className="mt-1 text-xs"
                />
              </div>
            </div>
          )}

          {/* Dialog Bottom Action Buttons */}
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
                {editingId ? "Update Customer" : "Save Customer"}
              </Button>
            </div>
          </div>
        </div>
      </Dialog>

      {/* View Full Profile Modal */}
      {viewProfileCustomer && (
        <Dialog
          open={!!viewProfileCustomer}
          onOpenChange={(open) => !open && setViewProfileCustomer(null)}
          title={`${viewProfileCustomer.firmName || viewProfileCustomer.name}`}
          description={`Customer ID: ${viewProfileCustomer.customerId || "CUST"} • Market: ${viewProfileCustomer.marketArea || viewProfileCustomer.city || "Ahmedabad"}`}
        >
          <div className="space-y-4 pt-1 max-h-[420px] overflow-y-auto pr-1 text-xs">
            {/* Identity Banner */}
            <div className="rounded-xl p-3 bg-zinc-100/70 dark:bg-zinc-900 border border-zinc-200/70 dark:border-zinc-800 flex items-center justify-between">
              <div>
                <p className="font-bold text-sm text-zinc-900 dark:text-zinc-100">
                  {viewProfileCustomer.firmName || viewProfileCustomer.name}
                </p>
                {viewProfileCustomer.firmName && viewProfileCustomer.name && (
                  <p className="text-zinc-600 dark:text-zinc-400 font-medium mt-0.5">
                    Owner / Contact Person: <strong>{viewProfileCustomer.name}</strong>
                  </p>
                )}
              </div>
              <span className="text-[10px] font-mono font-semibold bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 px-2 py-0.5 rounded-full">
                {viewProfileCustomer.customerId}
              </span>
            </div>

            {/* GSTIN, Credit Terms & Referral */}
            <div className="grid grid-cols-3 gap-2">
              <div className="p-2.5 rounded-xl border border-zinc-200 dark:border-zinc-800">
                <span className="text-[10px] text-muted-foreground">GSTIN</span>
                <p className="font-mono font-bold text-zinc-900 dark:text-zinc-100 truncate">
                  {viewProfileCustomer.gstin || viewProfileCustomer.gstNumber || "Not Provided"}
                </p>
              </div>
              <div className="p-2.5 rounded-xl border border-zinc-200 dark:border-zinc-800">
                <span className="text-[10px] text-muted-foreground">Credit Period</span>
                <p className="font-bold text-zinc-900 dark:text-zinc-100">
                  {viewProfileCustomer.creditDays || 30} Days
                </p>
              </div>
              <div className="p-2.5 rounded-xl border border-zinc-200 dark:border-zinc-800">
                <span className="text-[10px] text-muted-foreground">Referred By</span>
                <p className="font-bold text-zinc-900 dark:text-zinc-100 truncate">
                  {viewProfileCustomer.referredBy || "Direct Walk-in"}
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
                {viewProfileCustomer.phone && (
                  <div className="bg-zinc-50 dark:bg-zinc-900 p-2 rounded-lg">
                    <span className="text-[10px] text-muted-foreground">Phone 1 (Primary):</span>
                    <p className="font-bold text-zinc-900 dark:text-zinc-100">{viewProfileCustomer.phone}</p>
                  </div>
                )}
                {viewProfileCustomer.phone2 && (
                  <div className="bg-zinc-50 dark:bg-zinc-900 p-2 rounded-lg">
                    <span className="text-[10px] text-muted-foreground">Phone 2 (Counter):</span>
                    <p className="font-bold text-zinc-900 dark:text-zinc-100">{viewProfileCustomer.phone2}</p>
                  </div>
                )}
                {viewProfileCustomer.phone3 && (
                  <div className="bg-zinc-50 dark:bg-zinc-900 p-2 rounded-lg">
                    <span className="text-[10px] text-muted-foreground">Phone 3 (Accounts):</span>
                    <p className="font-bold text-zinc-900 dark:text-zinc-100">{viewProfileCustomer.phone3}</p>
                  </div>
                )}
                {viewProfileCustomer.phone4 && (
                  <div className="bg-zinc-50 dark:bg-zinc-900 p-2 rounded-lg">
                    <span className="text-[10px] text-muted-foreground">Phone 4 (Partner):</span>
                    <p className="font-bold text-zinc-900 dark:text-zinc-100">{viewProfileCustomer.phone4}</p>
                  </div>
                )}
                {viewProfileCustomer.phone5 && (
                  <div className="bg-zinc-50 dark:bg-zinc-900 p-2 rounded-lg">
                    <span className="text-[10px] text-muted-foreground">Phone 5 (Residence):</span>
                    <p className="font-bold text-zinc-900 dark:text-zinc-100">{viewProfileCustomer.phone5}</p>
                  </div>
                )}
              </div>
              {(viewProfileCustomer.email || viewProfileCustomer.email2) && (
                <div className="pt-2 border-t border-zinc-100 dark:border-zinc-800/60 text-muted-foreground space-y-0.5">
                  {viewProfileCustomer.email && <p>Email: {viewProfileCustomer.email}</p>}
                  {viewProfileCustomer.email2 && <p>Alt Email: {viewProfileCustomer.email2}</p>}
                </div>
              )}
            </div>

            {/* Addresses & Shop Outlets */}
            <div className="p-3 rounded-xl border border-zinc-200 dark:border-zinc-800 space-y-2">
              <span className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1.5">
                <MapPin className="h-3.5 w-3.5 text-zinc-500" />
                <span>Addresses & Outlets</span>
              </span>
              <div className="space-y-1.5 text-xs text-muted-foreground">
                {(viewProfileCustomer.shopAddress || viewProfileCustomer.address) && (
                  <p>
                    <strong className="text-zinc-700 dark:text-zinc-300">Shop / Office Address:</strong>{" "}
                    {viewProfileCustomer.shopAddress || viewProfileCustomer.address}
                  </p>
                )}
                {viewProfileCustomer.shopLocation && (
                  <p>
                    <strong className="text-zinc-700 dark:text-zinc-300">Shop Location / Landmark:</strong>{" "}
                    {viewProfileCustomer.shopLocation}
                  </p>
                )}
                {viewProfileCustomer.homeAddress && (
                  <p>
                    <strong className="text-zinc-700 dark:text-zinc-300">Home Residence Address:</strong>{" "}
                    {viewProfileCustomer.homeAddress}
                  </p>
                )}
                {viewProfileCustomer.personalLocation && (
                  <p>
                    <strong className="text-zinc-700 dark:text-zinc-300">Personal Location / Native:</strong>{" "}
                    {viewProfileCustomer.personalLocation}
                  </p>
                )}
                <div className="pt-1.5 border-t border-zinc-100 dark:border-zinc-800/60 flex items-center justify-between">
                  <span>Number of Outlets: <strong>{viewProfileCustomer.shopCount || 1}</strong></span>
                  {viewProfileCustomer.shopLocations && (
                    <span className="text-[11px] truncate max-w-[200px]">
                      Branches: {viewProfileCustomer.shopLocations}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Ahmedabad Markets & Preferred Products */}
            <div className="p-3 rounded-xl border border-zinc-200 dark:border-zinc-800 space-y-2">
              <span className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1.5">
                <Building2 className="h-3.5 w-3.5 text-zinc-500" />
                <span>Ahmedabad Markets & Preferred Fabrics</span>
              </span>
              <div>
                <span className="text-[10px] text-muted-foreground">Markets:</span>
                <p className="font-medium text-zinc-800 dark:text-zinc-200 mt-0.5">
                  {viewProfileCustomer.markets || viewProfileCustomer.marketArea || "Ahmedabad Central"}
                </p>
              </div>
              {viewProfileCustomer.preferredCategories && (
                <div className="pt-1.5 border-t border-zinc-100 dark:border-zinc-800/60">
                  <span className="text-[10px] text-muted-foreground">Garment Preferences:</span>
                  <p className="font-medium text-zinc-800 dark:text-zinc-200 mt-0.5">
                    {viewProfileCustomer.preferredCategories}
                  </p>
                </div>
              )}
              {viewProfileCustomer.notes && (
                <div className="pt-1.5 border-t border-zinc-100 dark:border-zinc-800/60">
                  <span className="text-[10px] text-muted-foreground">Terms / Notes:</span>
                  <p className="italic text-muted-foreground mt-0.5">{viewProfileCustomer.notes}</p>
                </div>
              )}
            </div>

            {/* Modal Bottom Actions */}
            <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-zinc-100 dark:border-zinc-800">
              <Button
                variant="outline"
                shape="pill"
                size="sm"
                onClick={() => handleGenerateCustomerReport(viewProfileCustomer)}
                className="text-xs font-semibold gap-1.5 text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-900/50 hover:bg-blue-50 dark:hover:bg-blue-950/40"
              >
                <Printer className="h-3.5 w-3.5" />
                <span>Customer Report (PDF)</span>
              </Button>

              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  shape="pill"
                  size="sm"
                  onClick={() => setViewProfileCustomer(null)}
                >
                  Close
                </Button>
                <Button
                  shape="pill"
                  size="sm"
                  onClick={() => {
                    const c = viewProfileCustomer
                    setViewProfileCustomer(null)
                    handleOpenEdit(c)
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
