import React, { useState, useEffect, useMemo } from "react"
import {
  Printer,
  Mail,
  Copy,
  Check,
  Search,
  RotateCw,
} from "lucide-react"
import { Dialog } from "./Dialog"
import { Button } from "./Button"
import { Input } from "./Input"
import { Badge } from "./Badge"
import { useData } from "../../context/DataContext"
import {
  EnvelopeRecipient,
  EnvelopeOptions,
  EnvelopeFontSize,
  DEFAULT_HIMAT_SENDER,
  createCustomerEnvelopeData,
  createSupplierEnvelopeData,
  generateEnvelopeHtml,
  printEnvelopeHtml,
} from "../../lib/envelopePrint"
import { HIMAT_LOGO_DATA_URI } from "../../lib/logoBase64"

interface EnvelopePrintModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialRecipient?: EnvelopeRecipient | null
  initialBatch?: EnvelopeRecipient[]
  defaultType?: "Customer" | "Supplier"
}

export function EnvelopePrintModal({
  open,
  onOpenChange,
  initialRecipient,
  defaultType = "Customer",
}: EnvelopePrintModalProps) {
  const { customers, suppliers } = useData()

  // Recipient state
  const [recipient, setRecipient] = useState<EnvelopeRecipient>({
    type: "Customer",
    firmName: "",
    contactPerson: "",
    addressLine1: "",
    addressLine2: "",
    city: "Ahmedabad",
    state: "Gujarat",
    pincode: "",
    phone: "",
    deliveryType: "BOOK POST",
  })

  // Simple source switch (Customer vs Supplier vs Custom)
  const [sourceType, setSourceType] = useState<"Customer" | "Supplier" | "Custom">(defaultType)
  const [searchQuery, setSearchQuery] = useState<string>("")
  const [isSearchOpen, setIsSearchOpen] = useState<boolean>(false)

  // Clean Options - Book stamp turned off by default as requested, easily toggleable
  const [includeStampBox, setIncludeStampBox] = useState<boolean>(false)
  const [includeSender, setIncludeSender] = useState<boolean>(true)
  const [includePhone, setIncludePhone] = useState<boolean>(true)
  const [fontSize, setFontSize] = useState<EnvelopeFontSize>("md")
  const [flip180, setFlip180] = useState<boolean>(false)
  const [copied, setCopied] = useState<boolean>(false)

  // Initialize when modal opens
  useEffect(() => {
    if (open) {
      if (initialRecipient) {
        setRecipient(initialRecipient)
        setSourceType(initialRecipient.type)
      } else if (customers.length > 0 && defaultType === "Customer") {
        setRecipient(createCustomerEnvelopeData(customers[0]))
        setSourceType("Customer")
      } else if (suppliers.length > 0 && defaultType === "Supplier") {
        setRecipient(createSupplierEnvelopeData(suppliers[0]))
        setSourceType("Supplier")
      }
    }
  }, [open, initialRecipient, defaultType, customers, suppliers])

  // Filtered list for search selector
  const searchResults = useMemo(() => {
    const q = searchQuery.toLowerCase().trim()
    if (sourceType === "Customer") {
      return customers
        .filter((c) => {
          if (!q) return true
          const firm = (c.firmName || "").toLowerCase()
          const name = (c.name || "").toLowerCase()
          const city = (c.city || "").toLowerCase()
          const phone = (c.phone || "").toLowerCase()
          return firm.includes(q) || name.includes(q) || city.includes(q) || phone.includes(q)
        })
        .slice(0, 15)
    } else if (sourceType === "Supplier") {
      return suppliers
        .filter((s) => {
          if (!q) return true
          const firm = (s.firmName || "").toLowerCase()
          const name = (s.name || "").toLowerCase()
          const city = (s.city || "").toLowerCase()
          const phone = (s.phone || "").toLowerCase()
          return firm.includes(q) || name.includes(q) || city.includes(q) || phone.includes(q)
        })
        .slice(0, 15)
    }
    return []
  }, [sourceType, searchQuery, customers, suppliers])

  const handleSelectCustomer = (c: (typeof customers)[0]) => {
    setRecipient(createCustomerEnvelopeData(c))
    setIsSearchOpen(false)
    setSearchQuery("")
  }

  const handleSelectSupplier = (s: (typeof suppliers)[0]) => {
    setRecipient(createSupplierEnvelopeData(s))
    setIsSearchOpen(false)
    setSearchQuery("")
  }

  const updateField = (field: keyof EnvelopeRecipient, val: string) => {
    setRecipient((prev) => ({ ...prev, [field]: val }))
  }

  // Copy address to clipboard
  const handleCopyAddress = async () => {
    const r = recipient
    const lines = [
      `TO:`,
      r.firmName,
      r.contactPerson ? `Attn: ${r.contactPerson}` : null,
      r.addressLine1 || null,
      r.addressLine2 || null,
      `${r.city || ""}${r.district ? `, ${r.district}` : ""}, ${r.state || ""} ${r.pincode ? `- ${r.pincode}` : ""}`.trim(),
      includePhone && r.phone ? `Ph: ${r.phone}` : null,
      r.gstin ? `GSTIN: ${r.gstin}` : null,
    ].filter(Boolean)

    try {
      await navigator.clipboard.writeText(lines.join("\n"))
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch (err) {
      console.error("Clipboard copy failed", err)
    }
  }

  // Trigger Print
  const handlePrint = () => {
    const printOptions: EnvelopeOptions = {
      includeSender,
      includeStampBox,
      includePhone,
      includeGstin: true,
      fontSize,
      flip180,
      sender: DEFAULT_HIMAT_SENDER,
    }
    const html = generateEnvelopeHtml([recipient], printOptions)
    printEnvelopeHtml(`Envelope - ${recipient.firmName || "Print"}`, html)
  }

  // Font size classes for visual preview
  const previewSizeClasses = {
    sm: {
      firm: "text-sm",
      contact: "text-[10px]",
      text: "text-[10px]",
      location: "text-[10.5px]",
      leading: "leading-snug",
    },
    md: {
      firm: "text-base",
      contact: "text-xs",
      text: "text-xs",
      location: "text-xs font-bold",
      leading: "leading-normal",
    },
    lg: {
      firm: "text-lg",
      contact: "text-[13px]",
      text: "text-[12.5px]",
      location: "text-[13px] font-bold",
      leading: "leading-relaxed",
    },
    xl: {
      firm: "text-xl",
      contact: "text-sm",
      text: "text-sm",
      location: "text-sm font-black",
      leading: "leading-relaxed",
    },
  }[fontSize]

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      className="max-w-5xl p-0 overflow-hidden"
    >
      {/* Top Header Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-200 bg-zinc-50/90 px-6 py-3.5 dark:border-zinc-800 dark:bg-zinc-900/90">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600 text-white shadow-md shadow-blue-500/20">
            <Mail className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-50">
                Print Envelope
              </h3>
              <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200 text-xs font-semibold dark:bg-blue-950/40 dark:text-blue-300">
                24 cm × 10.5 cm
              </Badge>
            </div>
            <p className="text-xs text-muted-foreground">
              Address placed on right portion • Choose Portrait in print dialog for vertical feed
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={handleCopyAddress}
            className="h-8 text-xs font-semibold gap-1.5"
          >
            {copied ? (
              <>
                <Check className="h-3.5 w-3.5 text-emerald-600" />
                <span className="text-emerald-600">Copied!</span>
              </>
            ) : (
              <>
                <Copy className="h-3.5 w-3.5" />
                <span>Copy Address</span>
              </>
            )}
          </Button>

          <Button
            size="sm"
            onClick={handlePrint}
            className="h-8 text-xs font-bold gap-1.5 shadow-sm bg-blue-600 hover:bg-blue-700 text-white px-4"
          >
            <Printer className="h-3.5 w-3.5" />
            <span>Print Envelope</span>
          </Button>
        </div>
      </div>

      {/* Main Body: Two-Column Clean Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 p-5 bg-zinc-100/70 dark:bg-zinc-950/70 max-h-[78vh] overflow-y-auto">
        {/* LEFT COLUMN: Recipient Form & Clean Controls (5 cols) */}
        <div className="lg:col-span-5 space-y-3 bg-white dark:bg-zinc-900 p-4 rounded-xl border border-zinc-200 dark:border-zinc-800 shadow-sm text-xs">
          {/* Source Selector */}
          <div className="flex items-center justify-between border-b border-zinc-200 pb-2.5 dark:border-zinc-800">
            <span className="font-bold text-zinc-900 dark:text-zinc-100">Select Recipient:</span>
            <div className="flex items-center rounded-lg border border-zinc-200 p-0.5 dark:border-zinc-800 bg-zinc-100 dark:bg-zinc-800">
              <button
                type="button"
                onClick={() => {
                  setSourceType("Customer")
                  setIsSearchOpen(true)
                }}
                className={`px-2 py-0.5 rounded font-semibold text-[11px] transition-all ${
                  sourceType === "Customer"
                    ? "bg-white text-blue-700 shadow-sm dark:bg-zinc-700 dark:text-blue-300"
                    : "text-zinc-600 dark:text-zinc-400"
                }`}
              >
                Customer
              </button>
              <button
                type="button"
                onClick={() => {
                  setSourceType("Supplier")
                  setIsSearchOpen(true)
                }}
                className={`px-2 py-0.5 rounded font-semibold text-[11px] transition-all ${
                  sourceType === "Supplier"
                    ? "bg-white text-amber-700 shadow-sm dark:bg-zinc-700 dark:text-amber-300"
                    : "text-zinc-600 dark:text-zinc-400"
                }`}
              >
                Supplier
              </button>
              <button
                type="button"
                onClick={() => setSourceType("Custom")}
                className={`px-2 py-0.5 rounded font-semibold text-[11px] transition-all ${
                  sourceType === "Custom"
                    ? "bg-white text-zinc-900 shadow-sm dark:bg-zinc-700 dark:text-zinc-100"
                    : "text-zinc-600 dark:text-zinc-400"
                }`}
              >
                Custom
              </button>
            </div>
          </div>

          {/* Search Dropdown (if Customer or Supplier) */}
          {sourceType !== "Custom" && (
            <div className="relative">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setIsSearchOpen(!isSearchOpen)}
                className="w-full justify-between h-8 text-xs font-medium text-zinc-700 dark:text-zinc-300 border-zinc-300 dark:border-zinc-700"
              >
                <span className="truncate flex items-center gap-1.5">
                  <Search className="h-3 w-3 text-zinc-400" />
                  <span>Choose from {sourceType}s...</span>
                </span>
                <span className="text-[10px] text-zinc-400">Click to change</span>
              </Button>

              {isSearchOpen && (
                <div className="absolute left-0 top-9 z-50 w-full rounded-xl border border-zinc-200 bg-white p-2 shadow-2xl dark:border-zinc-800 dark:bg-zinc-900">
                  <Input
                    autoFocus
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder={`Search name, firm, city...`}
                    className="h-7 text-xs mb-1.5"
                  />
                  <div className="max-h-48 overflow-y-auto space-y-1">
                    {searchResults.map((item: any) => (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => {
                          if (sourceType === "Customer") handleSelectCustomer(item)
                          else handleSelectSupplier(item)
                        }}
                        className="w-full text-left p-1.5 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors flex flex-col"
                      >
                        <span className="font-bold text-zinc-900 dark:text-zinc-100">
                          {item.firmName || item.name}
                        </span>
                        <span className="text-[10px] text-zinc-500">
                          {item.city || "Ahmedabad"} {item.phone ? `• ${item.phone}` : ""}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Form Fields */}
          <div className="space-y-2">
            <div>
              <label className="font-bold text-zinc-700 dark:text-zinc-300 block mb-0.5">
                Firm / Business Name *
              </label>
              <Input
                value={recipient.firmName || ""}
                onChange={(e) => updateField("firmName", e.target.value)}
                placeholder="e.g. M/S. SHREE BALAJI TEXTILE"
                className="font-bold uppercase h-8 text-xs"
              />
            </div>

            <div>
              <label className="font-medium text-zinc-600 dark:text-zinc-400 block mb-0.5">
                Attention / Contact Person (Optional)
              </label>
              <Input
                value={recipient.contactPerson || ""}
                onChange={(e) => updateField("contactPerson", e.target.value)}
                placeholder="e.g. Shri Rajesh Patel"
                className="h-8 text-xs"
              />
            </div>

            <div>
              <label className="font-medium text-zinc-600 dark:text-zinc-400 block mb-0.5">
                Address Line 1 (Shop / Office)
              </label>
              <Input
                value={recipient.addressLine1 || ""}
                onChange={(e) => updateField("addressLine1", e.target.value)}
                placeholder="e.g. Shop No. 12, Krishna Cloth Market"
                className="h-8 text-xs"
              />
            </div>

            <div>
              <label className="font-medium text-zinc-600 dark:text-zinc-400 block mb-0.5">
                Address Line 2 (Market / Area)
              </label>
              <Input
                value={recipient.addressLine2 || ""}
                onChange={(e) => updateField("addressLine2", e.target.value)}
                placeholder="e.g. Sarangpur, Sherkotda"
                className="h-8 text-xs"
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="font-medium text-zinc-600 dark:text-zinc-400 block mb-0.5">
                  City
                </label>
                <Input
                  value={recipient.city || ""}
                  onChange={(e) => updateField("city", e.target.value)}
                  placeholder="e.g. Ahmedabad"
                  className="h-8 text-xs"
                />
              </div>

              <div>
                <label className="font-bold text-zinc-700 dark:text-zinc-300 block mb-0.5">
                  PIN Code *
                </label>
                <Input
                  value={recipient.pincode || ""}
                  onChange={(e) => updateField("pincode", e.target.value)}
                  placeholder="e.g. 380002"
                  className="font-bold h-8 text-xs"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="font-medium text-zinc-600 dark:text-zinc-400 block mb-0.5">
                  Mobile / Phone
                </label>
                <Input
                  value={recipient.phone || ""}
                  onChange={(e) => updateField("phone", e.target.value)}
                  placeholder="e.g. +91 98250 12345"
                  className="h-8 text-xs"
                />
              </div>

              <div>
                <label className="font-medium text-zinc-600 dark:text-zinc-400 block mb-0.5">
                  State
                </label>
                <Input
                  value={recipient.state || "Gujarat"}
                  onChange={(e) => updateField("state", e.target.value)}
                  placeholder="e.g. Gujarat"
                  className="h-8 text-xs"
                />
              </div>
            </div>
          </div>

          {/* TEXT SIZE ADJUSTMENT SELECTOR */}
          <div className="border-t border-zinc-200 pt-2.5 dark:border-zinc-800">
            <div className="flex items-center justify-between mb-1.5">
              <span className="font-bold text-zinc-800 dark:text-zinc-200">
                Text Size:
              </span>
              <span className="text-[11px] font-semibold text-blue-600 capitalize">
                {fontSize === "sm" ? "Small (Compact)" : fontSize === "md" ? "Medium (Standard)" : fontSize === "lg" ? "Large" : "Extra Large"}
              </span>
            </div>

            <div className="grid grid-cols-4 gap-1.5">
              {[
                { id: "sm", label: "Small" },
                { id: "md", label: "Medium" },
                { id: "lg", label: "Large" },
                { id: "xl", label: "X-Large" },
              ].map((sz) => (
                <button
                  key={sz.id}
                  type="button"
                  onClick={() => setFontSize(sz.id as EnvelopeFontSize)}
                  className={`py-1 px-1.5 rounded-lg text-center text-xs font-bold transition-all border ${
                    fontSize === sz.id
                      ? "bg-blue-600 text-white border-blue-600 shadow-sm"
                      : "bg-zinc-50 text-zinc-700 border-zinc-200 hover:bg-zinc-100 dark:bg-zinc-800 dark:text-zinc-300 dark:border-zinc-700"
                  }`}
                >
                  {sz.label}
                </button>
              ))}
            </div>
          </div>

          {/* Clean Simple Toggles */}
          <div className="border-t border-zinc-200 pt-2.5 dark:border-zinc-800 space-y-2">
            <label className="flex items-center gap-2 cursor-pointer font-medium text-zinc-800 dark:text-zinc-200">
              <input
                type="checkbox"
                checked={includeStampBox}
                onChange={(e) => setIncludeStampBox(e.target.checked)}
                className="rounded text-blue-600 focus:ring-blue-500"
              />
              <span>Include Postal / Book Post Stamp Box</span>
            </label>

            <label className="flex items-center gap-2 cursor-pointer font-medium text-zinc-800 dark:text-zinc-200">
              <input
                type="checkbox"
                checked={includeSender}
                onChange={(e) => setIncludeSender(e.target.checked)}
                className="rounded text-blue-600 focus:ring-blue-500"
              />
              <span>Include Sender Address (Himat Textile on Left)</span>
            </label>

            <label className="flex items-center gap-2 cursor-pointer font-medium text-zinc-800 dark:text-zinc-200">
              <input
                type="checkbox"
                checked={includePhone}
                onChange={(e) => setIncludePhone(e.target.checked)}
                className="rounded text-blue-600 focus:ring-blue-500"
              />
              <span>Include Mobile / Phone Number</span>
            </label>

            <label className="flex items-center gap-2 cursor-pointer font-medium text-zinc-800 dark:text-zinc-200">
              <input
                type="checkbox"
                checked={flip180}
                onChange={(e) => setFlip180(e.target.checked)}
                className="rounded text-blue-600 focus:ring-blue-500"
              />
              <span className="flex items-center gap-1">
                <RotateCw className="h-3 w-3 text-zinc-500" />
                <span>Flip 180° (if printer feeds bottom edge first)</span>
              </span>
            </label>
          </div>
        </div>

        {/* RIGHT COLUMN: Live Envelope Preview (7 cols) */}
        <div className="lg:col-span-7 flex flex-col items-center justify-center space-y-3">
          {/* Envelope Card */}
          <div className="w-full shadow-lg rounded-lg overflow-hidden bg-white border border-zinc-300 relative select-none">
            {/* Dimension Bar */}
            <div className="bg-zinc-800 text-zinc-300 text-[10px] px-3 py-1 flex items-center justify-between font-mono">
              <span>◀ 24 cm (240 mm) Width ▶</span>
              <span className="text-blue-300 font-semibold">Live Preview ({fontSize.toUpperCase()})</span>
              <span>▲ 10.5 cm Height ▼</span>
            </div>

            {/* 240 / 105 Aspect Ratio Envelope Area */}
            <div
              className="relative w-full bg-white text-zinc-900 p-5 overflow-hidden"
              style={{
                aspectRatio: "240 / 105",
                minHeight: "280px",
              }}
            >
              {/* Top Delivery Badge (Only if includeStampBox is checked) */}
              {includeStampBox && (
                <div className="absolute top-3 left-5 px-2 py-0.5 bg-zinc-900 text-white text-[8px] font-black tracking-widest uppercase rounded">
                  BOOK POST
                </div>
              )}

              {/* Stamp Box (Only if includeStampBox is checked) */}
              {includeStampBox && (
                <div className="absolute top-3 right-5 w-20 h-12 border border-dashed border-zinc-400 bg-zinc-50/80 rounded flex flex-col items-center justify-center text-center p-1">
                  <span className="text-[9px] font-black text-zinc-700 tracking-wider">
                    BOOK POST
                  </span>
                  <span className="text-[7px] text-zinc-400">STAMP</span>
                </div>
              )}

              {/* Left Portion: SENDER */}
              {includeSender ? (
                <div className="absolute top-10 left-5 w-[42%] max-w-[280px] text-left">
                  <div className="border-b border-zinc-900 pb-0.5 mb-1 inline-block">
                    <span className="text-[8px] font-black tracking-wider text-zinc-800 uppercase">
                      FROM:
                    </span>
                  </div>

                  <div className="flex items-start gap-2">
                    <img
                      src={HIMAT_LOGO_DATA_URI}
                      alt="Logo"
                      className="h-8 w-auto object-contain shrink-0 mt-0.5"
                    />
                    <div>
                      <div className="font-black text-[11px] text-zinc-900 tracking-wide">
                        HIMAT TEXTILE
                      </div>
                      <div className="text-[7.5px] font-bold text-zinc-600 mb-0.5">
                        YOUR BUSINESS GUIDE ACROSS INDIA
                      </div>
                      <div className="text-[8px] text-zinc-600 leading-tight">
                        First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur
                      </div>
                      <div className="text-[8px] text-zinc-600 leading-tight">
                        Ahmedabad, Gujarat 380022
                      </div>
                      <div className="text-[8px] text-zinc-700 mt-0.5 font-medium">
                        Ph: +91 98739 38095
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="absolute top-10 left-5 w-[42%] border border-dashed border-zinc-300 rounded p-4 text-center bg-zinc-50/50">
                  <span className="text-[9px] text-zinc-400 italic">
                    Left portion left blank for pre-printed envelopes
                  </span>
                </div>
              )}

              {/* Center Divider Guideline (Non-printing) */}
              <div className="absolute top-3 bottom-3 left-[52%] border-l border-dashed border-blue-200 pointer-events-none" />

              {/* Right Portion: RECIPIENT (TO) with dynamic font size */}
              <div className="absolute top-5 left-[54%] right-5">
                <div className="text-[11px] font-black text-zinc-900 tracking-wider mb-1">
                  TO,
                </div>

                <div className={`font-black uppercase tracking-tight text-zinc-950 leading-tight mb-1 ${previewSizeClasses.firm}`}>
                  {recipient.firmName || "RECIPIENT FIRM NAME"}
                </div>

                {recipient.contactPerson && (
                  <div className={`font-bold text-zinc-800 mb-0.5 ${previewSizeClasses.contact}`}>
                    <span className="text-[8px] font-semibold text-zinc-500 uppercase mr-1">
                      Attn / Prop:
                    </span>
                    {recipient.contactPerson}
                  </div>
                )}

                {recipient.addressLine1 && (
                  <div className={`text-zinc-700 ${previewSizeClasses.text} ${previewSizeClasses.leading}`}>
                    {recipient.addressLine1}
                  </div>
                )}

                {recipient.addressLine2 && (
                  <div className={`text-zinc-700 ${previewSizeClasses.text} ${previewSizeClasses.leading}`}>
                    {recipient.addressLine2}
                  </div>
                )}

                <div className={`uppercase text-zinc-950 mt-1 ${previewSizeClasses.location}`}>
                  {[recipient.city, recipient.district].filter(Boolean).join(", ")}
                  {recipient.state ? `, ${recipient.state}` : ""}
                  {recipient.pincode && (
                    <span className="ml-1 font-black text-blue-700 bg-blue-50 px-1 py-0.2 rounded border border-blue-200">
                      - {recipient.pincode}
                    </span>
                  )}
                </div>

                {includePhone && recipient.phone && (
                  <div className={`text-zinc-700 mt-1 font-medium ${previewSizeClasses.text}`}>
                    <strong>Mob:</strong> {recipient.phone}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Simple Helpful Printing Guide */}
          <div className="w-full bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900 rounded-lg p-3 text-xs text-blue-900 dark:text-blue-200 space-y-1">
            <div className="font-bold flex items-center gap-1.5">
              <span>🖨️ How to Print:</span>
            </div>
            <p className="text-[11px] leading-relaxed">
              When the browser print dialog opens, set <strong>Orientation</strong>:
            </p>
            <ul className="list-disc pl-4 text-[11px] space-y-0.5">
              <li>
                <strong>Portrait:</strong> Use when putting the envelope in vertically (10.5 cm width enters the tray).
              </li>
              <li>
                <strong>Landscape:</strong> Use if feeding the envelope horizontally (24 cm wide).
              </li>
            </ul>
          </div>
        </div>
      </div>

      {/* Modal Bottom Bar */}
      <div className="flex items-center justify-between border-t border-zinc-200 bg-zinc-50 px-6 py-3 dark:border-zinc-800 dark:bg-zinc-900/90 text-xs">
        <span className="text-muted-foreground">
          Envelope Dimensions: <strong>24 cm × 10.5 cm</strong>
        </span>

        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onOpenChange(false)}
          >
            Cancel
          </Button>

          <Button
            size="sm"
            onClick={handlePrint}
            className="gap-1.5 font-bold bg-blue-600 hover:bg-blue-700 text-white shadow-sm px-4"
          >
            <Printer className="h-4 w-4" />
            <span>Print Envelope</span>
          </Button>
        </div>
      </div>
    </Dialog>
  )
}
