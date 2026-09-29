import React, { useEffect, useState } from "react"
import {
  Building2,
  User,
  Phone,
  MapPin,
  Store,
  Layers,
  Sparkles,
  Check,
  CheckCircle2,
  ArrowRight,
  ArrowLeft,
  Send,
  RefreshCw,
  AlertCircle,
  MessageSquare,
  Globe,
  Tag
} from "lucide-react"
import { ref, set, onValue } from "firebase/database"
import { rtdb } from "../lib/firebase"
import { FileUpload } from "../components/ui/FileUpload"
import { Toast } from "../components/ui/Toast"
import AutoGrowTextarea from "../components/ui/AutoGrowTextarea"
import { SupplierRegistrationRequest } from "../types"
import { HIMAT_LOGO_DATA_URI } from "../lib/logoBase64"
import {
  fetchGstDetails,
  isValidGstin,
  extractPanFromGstin,
  getStateFromGstin,
} from "../lib/gstHelper"
import {
  AHMEDABAD_TEXTILE_MARKETS,
  SUPPLIER_TYPES,
  SUPPLIER_CATEGORIES,
  SUPPLIER_GENTS_CHILD_OPTIONS,
  SUPPLIER_LADIES_CHILD_OPTIONS,
} from "../lib/constants"

interface MarketOption {
  /** Market master id (0 for the built-in fallback list) */
  id: number
  name: string
}

const OTHER_MARKET_VALUES = new Set(["Other / Outside Ahmedabad", "Other / Specify"])

// Used only until the Market master can be read (offline, or database rules not deployed yet)
const FALLBACK_MARKETS: MarketOption[] = AHMEDABAD_TEXTILE_MARKETS.filter((m) => !OTHER_MARKET_VALUES.has(m)).map(
  (name) => ({ id: 0, name })
)

/** Active, non-deleted markets from the markets node, de-duplicated by name */
function parseMarkets(val: any): MarketOption[] {
  if (!val || typeof val !== "object") return []
  const pairs: Array<[string, any]> = Array.isArray(val) ? val.map((v, i) => [String(i), v]) : Object.entries(val)
  const seen = new Set<string>()
  const list: MarketOption[] = []
  pairs.forEach(([key, m]) => {
    if (!m || typeof m !== "object") return
    if (m.isDeleted || m.deleted) return
    if (m.isActive === false || m.active === false) return
    const name = String(m.marketName || m.name || "").trim()
    const lower = name.toLowerCase()
    if (!name || seen.has(lower)) return
    seen.add(lower)
    const rawId = m.id ?? key
    list.push({ id: /^\d+$/.test(String(rawId)) ? Number(rawId) : 0, name })
  })
  return list.sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true }))
}

export function SupplierRegistrationView() {
  const [currentStep, setCurrentStep] = useState<number>(1)

  // Market list comes from the Market master (same list as the app), live: a market added in the
  // app or the admin shows up here without a reload. The built-in list is only a fallback.
  const [marketOptions, setMarketOptions] = useState<MarketOption[]>(FALLBACK_MARKETS)
  useEffect(() => {
    const unsubscribe = onValue(
      ref(rtdb, "markets"),
      (snap) => {
        const list = parseMarkets(snap.val())
        setMarketOptions(list.length > 0 ? list : FALLBACK_MARKETS)
      },
      (err) => {
        console.warn("Could not load markets, using the built-in list:", err?.message || err)
      }
    )
    return () => unsubscribe()
  }, [])

  // Language State - English ('en'), Hindi ('hi'), Gujarati ('gu')
  const [lang, setLang] = useState<"en" | "hi" | "gu">(() => {
    return (localStorage.getItem("himat_supplier_reg_lang") as "en" | "hi" | "gu") || "en"
  })

  const handleLanguageSwitch = (newLang: "en" | "hi" | "gu") => {
    setLang(newLang)
    localStorage.setItem("himat_supplier_reg_lang", newLang)
  }

  // Form State
  const [formData, setFormData] = useState({
    // Step 1: Business Profile
    firmName: "",
    contactPerson: "",
    type: "Manufacturer", // Manufacturer, Trading, Distributor, Fabric
    brand: "",
    phone: "",
    phone2: "",
    email: "",
    gstin: "",
    panNumber: "",

    // Step 2: Location (supplier must pick their market; no silent default)
    marketArea: "",
    customMarket: "",
    address: "", // Factory / Work Address (optional)
    officeAddress: "", // Shop / Office Address (mandatory)
    city: "Ahmedabad",
    district: "",
    state: "Gujarat",
    pincode: "",
    mapLink: "",

    // Step 3: Photos & Bank Details (optional)
    shopPhotoUri: "",
    godownPhotoUri: "",
    visitingCardPhotoUri: "",
    bankName: "",
    accountNumber: "",
    ifscCode: "",
    notes: "",
  })

  // Selected Categories (Ladies, Gents, Kids, Handloom) - multi-select
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])

  // Selected Child Options
  const [selectedGentsItems, setSelectedGentsItems] = useState<string[]>([])
  const [customGentsItem, setCustomGentsItem] = useState<string>("")

  const [selectedLadiesItems, setSelectedLadiesItems] = useState<string[]>([])
  const [customLadiesItem, setCustomLadiesItem] = useState<string>("")

  // Submission State
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [submittedRequestId, setSubmittedRequestId] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Toast Notification
  const [toast, setToast] = useState<{
    open: boolean
    message: string
    title?: string
    type?: "error" | "warning" | "success" | "info"
  }>({
    open: false,
    message: "",
    title: "",
    type: "error",
  })

  const triggerValidationError = (
    msg: string,
    fieldId?: string,
    customTitle?: string,
    type: "error" | "warning" | "success" | "info" = "error"
  ) => {
    setErrorMessage(msg)
    setToast({
      open: true,
      message: msg,
      title:
        customTitle ||
        (lang === "hi"
          ? "आवश्यक जानकारी अधूरी है (Mandatory)"
          : lang === "gu"
          ? "જરૂરી વિગત ખૂટે છે (Mandatory)"
          : "Mandatory Field Required"),
      type,
    })
    if (fieldId) {
      setTimeout(() => {
        const el = document.getElementById(fieldId)
        if (el) {
          el.scrollIntoView({ behavior: "smooth", block: "center" })
          if ("focus" in el && typeof (el as HTMLElement).focus === "function") {
            ;(el as HTMLElement).focus()
          }
        }
      }, 80)
    }
  }

  // GST Lookup and auto-population state
  const [isFetchingGst, setIsFetchingGst] = useState<boolean>(false)
  const [gstFeedback, setGstFeedback] = useState<{
    type: "success" | "offline" | "error" | null
    message: string
  }>({ type: null, message: "" })

  const handleGstLookup = async (inputGst?: string) => {
    const cleanGst = (inputGst !== undefined ? inputGst : formData.gstin).toUpperCase().trim()
    if (!cleanGst) {
      setGstFeedback({ type: null, message: "" })
      return
    }

    if (cleanGst.length !== 15 || !isValidGstin(cleanGst)) {
      setGstFeedback({
        type: "error",
        message:
          lang === "hi"
            ? "कृपया 15 अक्षरों का मान्य जीएसटी नंबर दर्ज करें (उदा. 24AAAAA0000A1Z5)"
            : "Please enter a valid 15-character GSTIN (e.g. 24AAAAA0000A1Z5)",
      })
      return
    }

    setIsFetchingGst(true)
    setGstFeedback({ type: null, message: "" })

    try {
      const offlinePan = extractPanFromGstin(cleanGst)
      const offlineState = getStateFromGstin(cleanGst)

      // Pre-fill offline decoded values immediately
      setFormData((prev) => ({
        ...prev,
        gstin: cleanGst,
        panNumber: offlinePan || prev.panNumber,
        state: offlineState || prev.state,
      }))

      // Attempt live fetch for firm name, address, city, pincode
      const details = await fetchGstDetails(cleanGst)

      if (details && (details.firmName || details.address || details.city || details.pincode)) {
        setFormData((prev) => ({
          ...prev,
          gstin: cleanGst,
          panNumber: details.pan || offlinePan || prev.panNumber,
          state: details.state || offlineState || prev.state,
          firmName: details.firmName || prev.firmName,
          officeAddress: details.address || prev.officeAddress || prev.address,
          city: details.city || prev.city,
          pincode: details.pincode || prev.pincode,
        }))
        setGstFeedback({
          type: "success",
          message:
            lang === "hi"
              ? "✓ जीएसटी से फर्म विवरण व पता स्वतः प्राप्त हो गया"
              : "✓ Business name & address retrieved from GSTIN",
        })
      } else {
        setGstFeedback({
          type: "offline",
          message:
            lang === "hi"
              ? "✓ राज्य और पैन नंबर स्वतः पहचान लिए गए हैं।"
              : "✓ State & PAN auto-detected from GSTIN.",
        })
      }
    } catch (err) {
      console.warn("GST lookup error:", err)
      const offlinePan = extractPanFromGstin(cleanGst)
      const offlineState = getStateFromGstin(cleanGst)
      setFormData((prev) => ({
        ...prev,
        gstin: cleanGst,
        panNumber: offlinePan || prev.panNumber,
        state: offlineState || prev.state,
      }))
      setGstFeedback({
        type: "offline",
        message:
          lang === "hi"
            ? "✓ राज्य और पैन नंबर स्वतः पहचान लिए गए हैं।"
            : "✓ State & PAN auto-detected from GSTIN.",
      })
    } finally {
      setIsFetchingGst(false)
    }
  }

  const handleClearGst = () => {
    setFormData((prev) => ({ ...prev, gstin: "" }))
    setGstFeedback({ type: null, message: "" })
  }

  // Handle Input Changes
  const handleInputChange = (field: string, value: any) => {
    setErrorMessage(null)
    setFormData((prev) => ({ ...prev, [field]: value }))
  }

  // Toggle Primary Category
  const toggleCategory = (cat: string) => {
    setSelectedCategories((prev) =>
      prev.includes(cat) ? prev.filter((c) => c !== cat) : [...prev, cat]
    )
  }

  // Toggle Gents/Kids Child Item
  const toggleGentsItem = (item: string) => {
    setSelectedGentsItems((prev) =>
      prev.includes(item) ? prev.filter((i) => i !== item) : [...prev, item]
    )
  }

  // Toggle Ladies Child Item
  const toggleLadiesItem = (item: string) => {
    setSelectedLadiesItems((prev) =>
      prev.includes(item) ? prev.filter((i) => i !== item) : [...prev, item]
    )
  }

  // Has Gents or Kids selected
  const hasGentsOrKids = selectedCategories.includes("Gents") || selectedCategories.includes("Kids")
  const hasLadies = selectedCategories.includes("Ladies")

  // Step Validation
  const validateCurrentStep = (targetStep?: number): boolean => {
    setErrorMessage(null)

    // Step 1 validation
    if (currentStep === 1 || (targetStep && targetStep > 1)) {
      if (!formData.firmName.trim()) {
        triggerValidationError(
          lang === "hi" ? "कृपया फर्म / दुकान का नाम दर्ज करें (अनिवार्य)" : "Please enter Firm / Shop Name (Mandatory)",
          "field-supplier-firmName"
        )
        return false
      }
      if (!formData.contactPerson.trim()) {
        triggerValidationError(
          lang === "hi" ? "कृपया संपर्क व्यक्ति / संचालक का नाम दर्ज करें (अनिवार्य)" : "Please enter Contact Person Name (Mandatory)",
          "field-supplier-contactPerson"
        )
        return false
      }
      const cleanPhone = formData.phone.replace(/\D/g, "")
      if (!cleanPhone || cleanPhone.length < 10) {
        triggerValidationError(
          lang === "hi" ? "कृपया 10 अंकों का मान्य मोबाइल नंबर दर्ज करें (अनिवार्य)" : "Please enter a valid 10-digit mobile number (Mandatory)",
          "field-supplier-phone"
        )
        return false
      }
      if (selectedCategories.length === 0) {
        triggerValidationError(
          lang === "hi" ? "कृपया कम से कम एक कैटेगरी चुनें (जैसे Gents, Ladies, Kids, Handloom)" : "Please select at least one Category (e.g. Gents, Ladies, Kids, Handloom)",
          "category-section"
        )
        return false
      }
    }

    // Step 2 validation
    if (currentStep === 2 || (targetStep && targetStep > 2)) {
      const currentMarket = formData.marketArea === "Other / Outside Ahmedabad" || formData.marketArea === "Other / Specify"
        ? formData.customMarket.trim()
        : formData.marketArea.trim()

      if (!currentMarket) {
        triggerValidationError(
          lang === "hi" ? "कृपया मार्केट का नाम दर्ज करें (अनिवार्य)" : "Please select or specify Market Area (Mandatory)",
          "field-supplier-marketArea"
        )
        return false
      }
      if (!formData.officeAddress.trim() && !formData.address.trim()) {
        triggerValidationError(
          lang === "hi" ? "कृपया दुकान / ऑफिस का पता दर्ज करें (अनिवार्य)" : "Please enter Shop / Office Address (Mandatory)",
          "field-supplier-officeAddress"
        )
        return false
      }
      if (!formData.city.trim()) {
        triggerValidationError(
          lang === "hi" ? "कृपया शहर दर्ज करें (अनिवार्य)" : "Please enter City (Mandatory)",
          "field-supplier-city"
        )
        return false
      }
    }

    return true
  }

  const goToNextStep = () => {
    if (validateCurrentStep(currentStep + 1)) {
      setCurrentStep((prev) => Math.min(prev + 1, 3))
      window.scrollTo({ top: 0, behavior: "smooth" })
    }
  }

  const goToPrevStep = () => {
    setErrorMessage(null)
    setCurrentStep((prev) => Math.max(prev - 1, 1))
    window.scrollTo({ top: 0, behavior: "smooth" })
  }

  // Final Direct Submission (No SMS OTP verification!)
  const handleSubmitSupplier = async () => {
    if (!validateCurrentStep(3)) return

    setIsSubmitting(true)
    setErrorMessage(null)

    try {
      const cleanGstin = formData.gstin.trim().toUpperCase()
      const cleanPhone = formData.phone.replace(/\D/g, "").slice(-10)
      const primaryKey = cleanGstin || cleanPhone
      const keyType: "GSTIN" | "PHONE" = cleanGstin ? "GSTIN" : "PHONE"
      const requestId = cleanGstin ? `req_sup_gst_${cleanGstin}` : `req_sup_phone_${cleanPhone}`
      const reqRef = ref(rtdb, `supplier_registration_requests/${requestId}`)

      const finalMarket =
        (formData.marketArea === "Other / Outside Ahmedabad" || formData.marketArea === "Other / Specify") && formData.customMarket.trim()
          ? formData.customMarket.trim()
          : formData.marketArea
      const pickedMarket = marketOptions.find((m) => m.name.toLowerCase() === finalMarket.trim().toLowerCase())

      // Combine child items
      const combinedSubCategories: string[] = []
      if (hasGentsOrKids) {
        selectedGentsItems.forEach((it) => {
          if (it === "Others" && customGentsItem.trim()) {
            combinedSubCategories.push(customGentsItem.trim())
          } else if (it !== "Others") {
            combinedSubCategories.push(it)
          }
        })
      }
      if (hasLadies) {
        selectedLadiesItems.forEach((it) => {
          if (it === "Others" && customLadiesItem.trim()) {
            combinedSubCategories.push(customLadiesItem.trim())
          } else if (it !== "Others") {
            combinedSubCategories.push(it)
          }
        })
      }

      const productsMadeString = combinedSubCategories.length > 0
        ? combinedSubCategories.join(", ")
        : selectedCategories.join(", ")

      const payload: SupplierRegistrationRequest = {
        id: requestId,
        primaryKey,
        keyType,
        firmName: formData.firmName.trim(),
        name: formData.firmName.trim(),
        contactPerson: formData.contactPerson.trim(),
        type: formData.type,
        brand: formData.brand.trim(),
        phone: `+91${cleanPhone}`,
        phone2: formData.phone2.trim() ? `+91${formData.phone2.replace(/\D/g, "").slice(-10)}` : "",
        email: formData.email.trim(),
        address: formData.address.trim(), // Factory address (optional)
        officeAddress: formData.officeAddress.trim() || formData.address.trim(), // Shop / Office Address
        marketArea: finalMarket,
        // Market master link (the approval screen uses it to connect the supplier to the market)
        marketId: pickedMarket && pickedMarket.id > 0 ? pickedMarket.id : undefined,
        marketName: finalMarket,
        city: formData.city.trim(),
        district: formData.district.trim(),
        state: formData.state.trim(),
        pincode: formData.pincode.trim(),
        mapLink: formData.mapLink.trim(),
        productsMade: productsMadeString,
        categories: selectedCategories.join(", "),
        subCategories: combinedSubCategories.join(", "),
        gstin: cleanGstin,
        panNumber: formData.panNumber.trim().toUpperCase(),
        bankName: formData.bankName.trim(),
        accountNumber: formData.accountNumber.trim(),
        ifscCode: formData.ifscCode.trim().toUpperCase(),
        shopPhotoUri: formData.shopPhotoUri || "",
        godownPhotoUri: formData.godownPhotoUri || "",
        visitingCardPhotoUri: formData.visitingCardPhotoUri || "",
        notes: formData.notes.trim(),
        status: "PENDING",
        phoneVerified: false,
        createdAt: Date.now(),
      }

      const cleanPayload: Record<string, any> = {}
      for (const [k, v] of Object.entries(payload)) {
        if (v !== undefined) cleanPayload[k] = v
      }

      await set(reqRef, cleanPayload)
      setSubmittedRequestId(requestId)
      window.scrollTo({ top: 0, behavior: "smooth" })
    } catch (err: any) {
      console.error("Supplier submission error:", err)
      triggerValidationError(err.message || "Failed to submit registration. Please check connection.")
    } finally {
      setIsSubmitting(false)
    }
  }

  // SUCCESS VIEW
  if (submittedRequestId) {
    const cleanGst = formData.gstin.trim().toUpperCase()
    const cleanPhone = formData.phone.replace(/\D/g, "").slice(-10)
    const finalMarket =
      (formData.marketArea === "Other / Outside Ahmedabad" || formData.marketArea === "Other / Specify") && formData.customMarket.trim()
        ? formData.customMarket.trim()
        : formData.marketArea

    const shareMessage = encodeURIComponent(
      `Namaste Himat Textile!\nWe have submitted our supplier registration online.\n\n📌 Firm Name: ${formData.firmName}\n👤 Contact Person: ${formData.contactPerson}\n📞 Mobile: +91 ${cleanPhone}\n🏢 Type: ${formData.type}\n🏷️ Category: ${selectedCategories.join(", ")}\n📍 Market: ${finalMarket}\n🆔 Ref ID: ${submittedRequestId}\n\nPlease review our application for onboarding. Thank you!`
    )

    return (
      <div className="min-h-screen bg-gradient-to-b from-zinc-50 to-zinc-100 dark:from-zinc-950 dark:to-zinc-900 py-10 px-4 sm:px-6">
        <div className="max-w-xl mx-auto bg-white dark:bg-zinc-900 rounded-3xl shadow-xl border border-zinc-200 dark:border-zinc-800 text-center p-8 sm:p-10">
          <div className="w-16 h-16 bg-emerald-100 dark:bg-emerald-950/60 rounded-full flex items-center justify-center mx-auto mb-5 text-emerald-600 dark:text-emerald-400 shadow-inner">
            <CheckCircle2 className="w-9 h-9" />
          </div>

          <h2 className="text-2xl font-black text-zinc-900 dark:text-zinc-50 tracking-tight">
            Registration Submitted Successfully!
          </h2>
          <p className="text-xs sm:text-sm text-zinc-600 dark:text-zinc-400 mt-2">
            Thank you for registering your firm with <strong>Himat Textile</strong>. Our team will review your business profile and proceed with onboarding.
          </p>

          <div className="mt-6 p-4 bg-zinc-50 dark:bg-zinc-800/60 rounded-2xl border border-zinc-200 dark:border-zinc-700 text-left space-y-2">
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Request ID:</span>
              <span className="font-mono font-bold text-zinc-800 dark:text-zinc-200">{submittedRequestId}</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Firm / Shop:</span>
              <span className="font-bold text-zinc-900 dark:text-zinc-100">{formData.firmName}</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Contact:</span>
              <span className="font-medium text-zinc-800 dark:text-zinc-200">{formData.contactPerson} (+91 {cleanPhone})</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Supplier Type:</span>
              <span className="font-semibold text-emerald-600 dark:text-emerald-400">{formData.type}</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Market Hub:</span>
              <span className="font-medium text-zinc-800 dark:text-zinc-200">{finalMarket}</span>
            </div>
          </div>

          <div className="mt-8 flex flex-col sm:flex-row gap-3 justify-center">
            <a
              href={`https://api.whatsapp.com/send?phone=919873938095&text=${shareMessage}`}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center justify-center gap-2 px-6 py-3 rounded-xl font-bold text-xs bg-emerald-600 hover:bg-emerald-700 text-white shadow-md transition-all"
            >
              <MessageSquare className="w-4 h-4" />
              Inform via WhatsApp
            </a>

            <button
              type="button"
              onClick={() => window.location.reload()}
              className="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl font-semibold text-xs border border-zinc-300 dark:border-zinc-700 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-all"
            >
              <RefreshCw className="w-4 h-4" />
              Submit Another Registration
            </button>
          </div>
        </div>
      </div>
    )
  }

  // WIZARD FORM VIEW
  return (
    <div className="min-h-screen bg-gradient-to-b from-zinc-50 via-zinc-100 to-zinc-200 dark:from-zinc-950 dark:via-zinc-900 dark:to-zinc-950 py-8 px-4 sm:px-6">
      <div className="max-w-2xl mx-auto">
        {/* Header with Himat Textile Logo */}
        <div className="text-center mb-6">
          <div className="flex items-center justify-center gap-3 mb-2">
            <img
              src={HIMAT_LOGO_DATA_URI}
              alt="Himat Textile"
              className="h-10 w-auto rounded-lg shadow-sm"
            />
            <h1 className="text-2xl font-black tracking-tight text-zinc-900 dark:text-zinc-50">
              Himat Textile Ahmedabad
            </h1>
          </div>

          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 text-xs font-semibold mb-2">
            <Store className="w-3.5 h-3.5" />
            <span>Supplier & Vendor Onboarding</span>
          </div>

          <p className="text-xs text-zinc-600 dark:text-zinc-400 max-w-md mx-auto">
            {lang === "hi"
              ? "हिम्मत टेक्सटाइल के साथ जुड़ने के लिए अपनी फर्म और गारमेंट निर्माण/ट्रेडिंग विवरण दर्ज करें।"
              : lang === "gu"
              ? "હિંમત ટેક્સટાઇલ સાથે જોડાવા માટે તમારી પેઢી અને વ્યવસાયની વિગતો નોંધાવો."
              : "Register your garment manufacturing, trading, or fabric supply firm to connect with wholesale buyers."}
          </p>

          {/* Language Toggle */}
          <div className="mt-4 inline-flex items-center p-1 rounded-xl bg-zinc-200/80 dark:bg-zinc-800 border border-zinc-300 dark:border-zinc-700">
            {(["en", "hi", "gu"] as const).map((l) => (
              <button
                key={l}
                type="button"
                onClick={() => handleLanguageSwitch(l)}
                className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                  lang === l
                    ? "bg-white dark:bg-zinc-700 text-zinc-900 dark:text-zinc-100 shadow-sm"
                    : "text-zinc-600 dark:text-zinc-400 hover:text-zinc-900"
                }`}
              >
                {l === "en" ? "English" : l === "hi" ? "हिंदी" : "ગુજરાતી"}
              </button>
            ))}
          </div>
        </div>

        {/* Wizard Steps Indicator (3 Steps) */}
        <div className="mb-6 bg-white dark:bg-zinc-900 rounded-xl p-3 border border-zinc-200 dark:border-zinc-800 shadow-sm">
          <div className="flex items-center justify-between">
            {[
              { num: 1, label: "Firm & Category" },
              { num: 2, label: "Market & Address" },
              { num: 3, label: "Photos & Submit" },
            ].map((step, idx) => (
              <React.Fragment key={step.num}>
                <div className="flex flex-col items-center">
                  <button
                    type="button"
                    onClick={() => {
                      if (step.num < currentStep || validateCurrentStep(step.num)) {
                        setCurrentStep(step.num)
                      }
                    }}
                    className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                      currentStep === step.num
                        ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 ring-2 ring-emerald-500"
                        : currentStep > step.num
                        ? "bg-emerald-600 text-white"
                        : "bg-zinc-100 dark:bg-zinc-800 text-zinc-400"
                    }`}
                  >
                    {currentStep > step.num ? <Check className="w-3.5 h-3.5" /> : step.num}
                  </button>
                  <span className="text-[10px] font-medium text-zinc-600 dark:text-zinc-400 mt-1 hidden sm:block">
                    {step.label}
                  </span>
                </div>
                {idx < 2 && (
                  <div
                    className={`flex-1 h-0.5 mx-2 transition-all ${
                      currentStep > idx + 1 ? "bg-emerald-600" : "bg-zinc-200 dark:bg-zinc-800"
                    }`}
                  />
                )}
              </React.Fragment>
            ))}
          </div>
        </div>

        {/* Global Error Banner */}
        {errorMessage && (
          <div className="mb-6 p-3 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800/60 flex items-start gap-2.5 text-xs text-red-700 dark:text-red-300">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Step Card Container */}
        <div className="bg-white dark:bg-zinc-900 rounded-2xl shadow-lg border border-zinc-200 dark:border-zinc-800 p-6 sm:p-8">
          {/* STEP 1: BUSINESS PROFILE & CATEGORIES */}
          {currentStep === 1 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <Building2 className="w-4 h-4 text-emerald-600" />
                  <span>Step 1: Firm Profile & Categories</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  Select your supplier type and categories to get started.
                </p>
              </div>

              {/* Supplier Type Selection (Manufacturer, Trading, Distributor, Fabric) */}
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1.5">
                  Supplier Type <span className="text-red-500">*</span>
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  {SUPPLIER_TYPES.map((st) => {
                    const isSelected = formData.type === st
                    return (
                      <button
                        key={st}
                        type="button"
                        onClick={() => handleInputChange("type", st)}
                        className={`h-11 px-3 rounded-xl text-xs font-bold transition-all border flex items-center justify-center gap-1.5 ${
                          isSelected
                            ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 border-zinc-900 dark:border-zinc-100 shadow-sm"
                            : "bg-zinc-50 dark:bg-zinc-800/60 text-zinc-700 dark:text-zinc-300 border-zinc-200 dark:border-zinc-700 hover:bg-zinc-100"
                        }`}
                      >
                        {isSelected && <Check className="w-3.5 h-3.5" />}
                        <span>{st}</span>
                      </button>
                    )
                  })}
                </div>
              </div>

              {/* Firm / Shop Name */}
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Firm / Shop Name <span className="text-red-500">*</span>
                </label>
                <input
                  id="field-supplier-firmName"
                  type="text"
                  value={formData.firmName}
                  onChange={(e) => handleInputChange("firmName", e.target.value)}
                  placeholder="e.g. Radhey Textiles, Mahadev Creation"
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              {/* Contact Person Name */}
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Contact Person / Proprietor Name <span className="text-red-500">*</span>
                </label>
                <input
                  id="field-supplier-contactPerson"
                  type="text"
                  value={formData.contactPerson}
                  onChange={(e) => handleInputChange("contactPerson", e.target.value)}
                  placeholder="e.g. Rameshbhai Patel"
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              {/* Mobile Number & WhatsApp */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Primary Mobile Number <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <span className="absolute left-3 top-2.5 text-xs text-zinc-500 font-semibold">+91</span>
                    <input
                      id="field-supplier-phone"
                      type="tel"
                      maxLength={10}
                      value={formData.phone}
                      onChange={(e) => handleInputChange("phone", e.target.value.replace(/\D/g, ""))}
                      placeholder="9825012345"
                      className="w-full h-10 pl-11 pr-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    WhatsApp / Alternate Number (Optional)
                  </label>
                  <div className="relative">
                    <span className="absolute left-3 top-2.5 text-xs text-zinc-500 font-semibold">+91</span>
                    <input
                      id="field-supplier-phone2"
                      type="tel"
                      maxLength={10}
                      value={formData.phone2}
                      onChange={(e) => handleInputChange("phone2", e.target.value.replace(/\D/g, ""))}
                      placeholder="WhatsApp phone number"
                      className="w-full h-10 pl-11 pr-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>
                </div>
              </div>

              {/* GSTIN (with auto-lookup) */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    GSTIN Number (Optional)
                  </label>
                  {formData.gstin && (
                    <button
                      type="button"
                      onClick={handleClearGst}
                      className="text-[11px] text-zinc-400 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                    >
                      Clear
                    </button>
                  )}
                </div>
                <div className="relative">
                  <input
                    id="field-supplier-gstin"
                    type="text"
                    maxLength={15}
                    placeholder="24AAAAA0000A1Z5"
                    value={formData.gstin}
                    onChange={(e) => {
                      const val = e.target.value.toUpperCase().replace(/[^0-9A-Z]/g, "")
                      handleInputChange("gstin", val)
                      if (val.length === 15 && isValidGstin(val)) {
                        handleGstLookup(val)
                      }
                    }}
                    className="w-full h-10 px-3.5 pr-10 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 uppercase font-mono"
                  />
                  <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center pointer-events-none">
                    {isFetchingGst ? (
                      <RefreshCw className="w-4 h-4 text-emerald-600 animate-spin" />
                    ) : formData.gstin.length === 15 && isValidGstin(formData.gstin) ? (
                      <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                    ) : null}
                  </div>
                </div>
                {gstFeedback.message && (
                  <p className="text-[11px] mt-1 text-emerald-600 dark:text-emerald-400 font-medium">
                    {gstFeedback.message}
                  </p>
                )}
              </div>

              {/* Category Selection (Ladies, Gents, Kids, Handloom) */}
              <div id="category-section" className="pt-3 border-t border-zinc-100 dark:border-zinc-800">
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1.5">
                  Select Categories <span className="text-red-500">*</span>
                  <span className="block text-[11px] font-normal text-zinc-500">
                    You can select multiple categories
                  </span>
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 mb-3">
                  {SUPPLIER_CATEGORIES.map((cat) => {
                    const isSelected = selectedCategories.includes(cat)
                    return (
                      <button
                        key={cat}
                        type="button"
                        onClick={() => toggleCategory(cat)}
                        className={`h-10 px-3 rounded-xl text-xs font-semibold transition-all border flex items-center justify-center gap-1.5 ${
                          isSelected
                            ? "bg-emerald-600 text-white border-emerald-600 shadow-sm"
                            : "bg-zinc-50 dark:bg-zinc-800/60 text-zinc-700 dark:text-zinc-300 border-zinc-200 dark:border-zinc-700 hover:bg-zinc-100"
                        }`}
                      >
                        {isSelected && <Check className="w-3.5 h-3.5" />}
                        <span>{cat}</span>
                      </button>
                    )
                  })}
                </div>

                {/* Child Options for Gents / Kids (shared list, rendered once if either or both selected) */}
                {hasGentsOrKids && (
                  <div className="p-3 mb-3 bg-zinc-50 dark:bg-zinc-800/40 rounded-xl border border-zinc-200 dark:border-zinc-700">
                    <label className="block text-xs font-semibold text-zinc-800 dark:text-zinc-200 mb-1.5">
                      {selectedCategories.includes("Gents") && selectedCategories.includes("Kids")
                        ? "Gents & Kids Items"
                        : selectedCategories.includes("Gents")
                        ? "Gents Items"
                        : "Kids Items"}
                      <span className="text-[10px] font-normal text-zinc-500 ml-1.5">(Select items)</span>
                    </label>
                    <div className="flex flex-wrap gap-1.5">
                      {SUPPLIER_GENTS_CHILD_OPTIONS.map((item) => {
                        const isSelected = selectedGentsItems.includes(item)
                        return (
                          <button
                            key={item}
                            type="button"
                            onClick={() => toggleGentsItem(item)}
                            className={`px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
                              isSelected
                                ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-xs"
                                : "bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700 hover:bg-zinc-100"
                            }`}
                          >
                            {isSelected && <Check className="w-3 h-3 inline mr-1" />}
                            {item}
                          </button>
                        )
                      })}
                    </div>
                    {selectedGentsItems.includes("Others") && (
                      <input
                        type="text"
                        value={customGentsItem}
                        onChange={(e) => setCustomGentsItem(e.target.value)}
                        placeholder="Specify other gents/kids items..."
                        className="mt-2 w-full h-8 px-3 rounded-lg border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs text-zinc-900 dark:text-zinc-100"
                      />
                    )}
                  </div>
                )}

                {/* Child Options for Ladies */}
                {hasLadies && (
                  <div className="p-3 bg-zinc-50 dark:bg-zinc-800/40 rounded-xl border border-zinc-200 dark:border-zinc-700">
                    <label className="block text-xs font-semibold text-zinc-800 dark:text-zinc-200 mb-1.5">
                      Ladies Items
                      <span className="text-[10px] font-normal text-zinc-500 ml-1.5">(Select items)</span>
                    </label>
                    <div className="flex flex-wrap gap-1.5">
                      {SUPPLIER_LADIES_CHILD_OPTIONS.map((item) => {
                        const isSelected = selectedLadiesItems.includes(item)
                        return (
                          <button
                            key={item}
                            type="button"
                            onClick={() => toggleLadiesItem(item)}
                            className={`px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
                              isSelected
                                ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-xs"
                                : "bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700 hover:bg-zinc-100"
                            }`}
                          >
                            {isSelected && <Check className="w-3 h-3 inline mr-1" />}
                            {item}
                          </button>
                        )
                      })}
                    </div>
                    {selectedLadiesItems.includes("Others") && (
                      <input
                        type="text"
                        value={customLadiesItem}
                        onChange={(e) => setCustomLadiesItem(e.target.value)}
                        placeholder="Specify other ladies items..."
                        className="mt-2 w-full h-8 px-3 rounded-lg border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs text-zinc-900 dark:text-zinc-100"
                      />
                    )}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* STEP 2: MARKET & ADDRESS */}
          {currentStep === 2 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-emerald-600" />
                  <span>Step 2: Market Hub & Location</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  Select your market area and provide shop address.
                </p>
              </div>

              {/* Market Selection Dropdown */}
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Textile Market Hub <span className="text-red-500">*</span>
                </label>
                <select
                  id="field-supplier-marketArea"
                  value={formData.marketArea}
                  onChange={(e) => handleInputChange("marketArea", e.target.value)}
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                >
                  <option value="">-- Select your market --</option>
                  {marketOptions.map((mkt) => (
                    <option key={`${mkt.id}-${mkt.name}`} value={mkt.name}>
                      {mkt.name}
                    </option>
                  ))}
                  <option value="Other / Specify">Other / Specify...</option>
                </select>

                {(formData.marketArea === "Other / Outside Ahmedabad" || formData.marketArea === "Other / Specify") && (
                  <div className="mt-2">
                    <input
                      type="text"
                      value={formData.customMarket}
                      onChange={(e) => handleInputChange("customMarket", e.target.value)}
                      placeholder="Please enter your market name / area..."
                      className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>
                )}
              </div>

              {/* City & District */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    City <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="field-supplier-city"
                    type="text"
                    value={formData.city}
                    onChange={(e) => handleInputChange("city", e.target.value)}
                    placeholder="e.g. Ahmedabad, Surat, Mumbai"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Pincode (Optional)
                  </label>
                  <input
                    type="text"
                    maxLength={6}
                    value={formData.pincode}
                    onChange={(e) => handleInputChange("pincode", e.target.value.replace(/\D/g, ""))}
                    placeholder="e.g. 380002"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              {/* Shop / Office Address (Mandatory) */}
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Shop / Market Office Address <span className="text-red-500">*</span>
                </label>
                <AutoGrowTextarea
                  minRows={2}
                  id="field-supplier-officeAddress"
                  value={formData.officeAddress}
                  onChange={(val) => handleInputChange("officeAddress", val)}
                  placeholder="Shop number, building/complex, road, market hub..."
                  className="w-full p-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              {/* Factory / Work Address (Optional) */}
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Factory / Manufacturing Address (Optional)
                </label>
                <AutoGrowTextarea
                  minRows={2}
                  value={formData.address}
                  onChange={(val) => handleInputChange("address", val)}
                  placeholder="Factory / GIDC / Unit address (optional)..."
                  className="w-full p-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              {/* Google Maps Link (Optional) */}
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Google Maps Location Link (Optional)
                </label>
                <input
                  type="url"
                  value={formData.mapLink}
                  onChange={(e) => handleInputChange("mapLink", e.target.value)}
                  placeholder="https://maps.app.goo.gl/..."
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>
            </div>
          )}

          {/* STEP 3: PHOTOS & BANK DETAILS (OPTIONAL) & SUBMISSION */}
          {currentStep === 3 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <Store className="w-4 h-4 text-emerald-600" />
                  <span>Step 3: Shop / Godown Photos & Submit</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  Upload shop and godown photos for fast verification.
                </p>
              </div>

              {/* Photos: Shop Photo & Godown Photo */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <FileUpload
                    label="Shop Front / Showroom Photo"
                    folder="suppliers/shop"
                    prefix="shop_photo"
                    value={formData.shopPhotoUri}
                    onChange={(url) => handleInputChange("shopPhotoUri", url)}
                    description="Photo of shop front showing board or banner"
                  />
                </div>

                <div>
                  <FileUpload
                    label="Godown / Warehouse Photo"
                    folder="suppliers/godown"
                    prefix="godown_photo"
                    value={formData.godownPhotoUri}
                    onChange={(url) => handleInputChange("godownPhotoUri", url)}
                    description="Photo of godown or stock facility"
                  />
                </div>

                <div className="sm:col-span-2">
                  <FileUpload
                    label="Visiting Card Photo (Optional)"
                    folder="suppliers/cards"
                    prefix="visiting_card"
                    value={formData.visitingCardPhotoUri}
                    onChange={(url) => handleInputChange("visitingCardPhotoUri", url)}
                    description="Optional visiting card for contact records"
                  />
                </div>
              </div>

              {/* Bank Details (Optional) */}
              <div className="pt-3 border-t border-zinc-100 dark:border-zinc-800">
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Bank Details (Optional)
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <input
                      type="text"
                      value={formData.bankName}
                      onChange={(e) => handleInputChange("bankName", e.target.value)}
                      placeholder="Bank Name (Optional)"
                      className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>
                  <div>
                    <input
                      type="text"
                      value={formData.accountNumber}
                      onChange={(e) => handleInputChange("accountNumber", e.target.value)}
                      placeholder="Account Number (Optional)"
                      className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono"
                    />
                  </div>
                  <div>
                    <input
                      type="text"
                      value={formData.ifscCode}
                      onChange={(e) => handleInputChange("ifscCode", e.target.value.toUpperCase())}
                      placeholder="IFSC Code (Optional)"
                      className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono uppercase"
                    />
                  </div>
                </div>
              </div>

              {/* Notes */}
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Additional Notes (Optional)
                </label>
                <AutoGrowTextarea
                  minRows={2}
                  value={formData.notes}
                  onChange={(val) => handleInputChange("notes", val)}
                  placeholder="Any special remarks or product details..."
                  className="w-full p-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              {/* Summary Card */}
              <div className="bg-zinc-50 dark:bg-zinc-800/60 p-4 rounded-xl border border-zinc-200 dark:border-zinc-700 space-y-1.5 text-xs">
                <div className="flex justify-between">
                  <span className="text-zinc-500">Firm / Shop:</span>
                  <span className="font-bold text-zinc-900 dark:text-zinc-100">{formData.firmName || "—"}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Type:</span>
                  <span className="font-semibold text-emerald-600 dark:text-emerald-400">{formData.type}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Categories:</span>
                  <span className="font-medium text-zinc-800 dark:text-zinc-200">{selectedCategories.join(", ") || "None"}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Market Hub:</span>
                  <span className="font-medium text-zinc-800 dark:text-zinc-200">{formData.marketArea}</span>
                </div>
              </div>

              {/* Direct Submit Button (NO OTP required!) */}
              <button
                type="button"
                onClick={handleSubmitSupplier}
                disabled={isSubmitting}
                className="w-full h-12 rounded-xl font-bold text-xs bg-emerald-600 hover:bg-emerald-700 text-white shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-50 mt-4"
              >
                {isSubmitting ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Submitting Registration...</span>
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    <span>Submit Supplier Registration</span>
                  </>
                )}
              </button>
            </div>
          )}

          {/* Navigation Footer */}
          <div className="mt-8 pt-4 border-t border-zinc-100 dark:border-zinc-800 flex items-center justify-between">
            <div>
              {currentStep > 1 && (
                <button
                  type="button"
                  onClick={goToPrevStep}
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-zinc-300 dark:border-zinc-700 text-xs font-semibold text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-all"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Back</span>
                </button>
              )}
            </div>

            <div>
              {currentStep < 3 && (
                <button
                  type="button"
                  onClick={goToNextStep}
                  className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 text-white text-xs font-bold shadow-md transition-all"
                >
                  <span>Continue</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Floating Toast Notification */}
      <Toast
        open={toast.open}
        message={toast.message}
        title={toast.title}
        type={toast.type}
        onClose={() => setToast((prev) => ({ ...prev, open: false }))}
      />
    </div>
  )
}
