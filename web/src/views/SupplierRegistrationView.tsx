import React, { useState, useEffect, useRef } from "react"
import {
  Building2,
  User,
  Phone,
  Mail,
  MapPin,
  Factory,
  Landmark,
  ShieldCheck,
  CheckCircle2,
  ArrowRight,
  ArrowLeft,
  Send,
  RefreshCw,
  AlertCircle,
  Sparkles,
  Check,
  MessageSquare,
  Globe,
  Tag,
  Store
} from "lucide-react"
import { RecaptchaVerifier, signInWithPhoneNumber, ConfirmationResult } from "firebase/auth"
import { ref, set } from "firebase/database"
import { auth, rtdb } from "../lib/firebase"
import { FileUpload } from "../components/ui/FileUpload"
import { SupplierRegistrationRequest } from "../types"
import { HIMAT_LOGO_DATA_URI } from "../lib/logoBase64"
import {
  fetchGstDetails,
  isValidGstin,
  extractPanFromGstin,
  getStateFromGstin,
} from "../lib/gstHelper"

const SUPPLIER_FABRIC_CATEGORIES = [
  "Shirting (Cotton & PC)",
  "Suiting & Trouser",
  "Denim & Jeans",
  "Rayon & Viscose",
  "Lycra & Stretch",
  "Digital Print Fabrics",
  "Linen & Flax",
  "Knits & Hosiery",
  "T-Shirts & Polos",
  "Formal Shirts",
  "Casual Shirts",
  "Kurtis & Ethnic Wear",
  "Nightwear & Loungewear",
  "Yarn & Grey Fabric",
]

const AHMEDABAD_MARKETS = [
  "Maskati Market",
  "New Cloth Market",
  "Revdi Bazar",
  "Rituraj Market",
  "Sumel Business Park 1, 2 & 3",
  "Narol GIDC Industrial Area",
  "Changodar Industrial Area",
  "Piramana Industrial Hub",
  "Kalupur Commercial Center",
  "Kuber Nagar Textile Market",
  "Other / Outside Ahmedabad"
]

export function SupplierRegistrationView() {
  const [currentStep, setCurrentStep] = useState<number>(1)

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
    type: "Manufacturer" as "Manufacturer" | "Wholesaler",
    brand: "",
    phone: "",
    phone2: "",
    sameAsMobile: true,
    email: "",

    // Step 2: Location & Mill
    marketArea: "Maskati Market",
    customMarket: "",
    address: "",
    officeAddress: "",
    city: "Ahmedabad",
    district: "",
    state: "Gujarat",
    pincode: "",
    mapLink: "",

    // Step 3: Fabrics, Garments & Commercials
    priceRange: "",
    gstin: "",
    panNumber: "",
    bankName: "",
    accountNumber: "",
    ifscCode: "",
    notes: "",

    // Step 4: KYC & Photos
    visitingCardPhotoUri: "",
    shopPhotoUri: "",
    gstCertPhotoUri: "",
    panPhotoUri: "",
  })

  // Selected Fabrics/Garment Categories
  const [selectedCategories, setSelectedCategories] = useState<string[]>([
    "Shirting (Cotton & PC)",
    "Rayon & Viscose"
  ])
  const [customCategory, setCustomCategory] = useState<string>("")

  // Phone Auth State
  const [confirmationResult, setConfirmationResult] = useState<ConfirmationResult | null>(null)
  const [otpCode, setOtpCode] = useState<string>("")
  const [isSendingOtp, setIsSendingOtp] = useState<boolean>(false)
  const [isVerifyingOtp, setIsVerifyingOtp] = useState<boolean>(false)
  const [otpSent, setOtpSent] = useState<boolean>(false)
  const [resendTimer, setResendTimer] = useState<number>(0)
  const [submittedRequestId, setSubmittedRequestId] = useState<string | null>(null)

  // Feedback State
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const recaptchaVerifierRef = useRef<RecaptchaVerifier | null>(null)
  const recaptchaWrapperRef = useRef<HTMLDivElement | null>(null)

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

      // Attempt live fetch for mill name, address, city, pincode
      const details = await fetchGstDetails(cleanGst)

      if (details && (details.firmName || details.address || details.city || details.pincode)) {
        setFormData((prev) => ({
          ...prev,
          gstin: cleanGst,
          panNumber: details.pan || offlinePan || prev.panNumber,
          state: details.state || offlineState || prev.state,
          firmName: details.firmName || prev.firmName,
          address: details.address || prev.address,
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
              ? "✓ राज्य और पैन नंबर स्वतः पहचान लिए गए हैं। कृपया नीचे मिल का नाम दर्ज करें।"
              : "✓ State & PAN auto-detected from GSTIN. Please enter Mill Name below.",
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
    setFormData((prev) => ({
      ...prev,
      gstin: "",
    }))
    setGstFeedback({ type: null, message: "" })
  }

  // Sync phone2 if sameAsMobile is checked
  useEffect(() => {
    if (formData.sameAsMobile) {
      setFormData((prev) => ({ ...prev, phone2: prev.phone }))
    }
  }, [formData.phone, formData.sameAsMobile])

  // Timer countdown for OTP resend
  useEffect(() => {
    if (resendTimer <= 0) return
    const interval = setInterval(() => {
      setResendTimer((prev) => (prev > 0 ? prev - 1 : 0))
    }, 1000)
    return () => clearInterval(interval)
  }, [resendTimer])

  // Handle Input Changes
  const handleInputChange = (field: string, value: any) => {
    setErrorMessage(null)
    setFormData((prev) => ({ ...prev, [field]: value }))
  }

  // Toggle Category
  const toggleCategory = (cat: string) => {
    setSelectedCategories((prev) =>
      prev.includes(cat) ? prev.filter((c) => c !== cat) : [...prev, cat]
    )
  }

  const addCustomCategory = () => {
    if (customCategory.trim() && !selectedCategories.includes(customCategory.trim())) {
      setSelectedCategories((prev) => [...prev, customCategory.trim()])
      setCustomCategory("")
    }
  }

  // Step 1 completeness check
  const isStep1Complete = Boolean(
    formData.firmName.trim() &&
    formData.contactPerson.trim() &&
    formData.phone.replace(/\D/g, "").length === 10
  )

  // Step Validation
  const validateCurrentStep = (targetStep?: number, isSkip = false): boolean => {
    setErrorMessage(null)
    // Mandatory fields check for Step 1
    if (currentStep === 1 || (targetStep && targetStep > 1)) {
      if (!formData.firmName.trim()) {
        setErrorMessage("Please enter Mill / Firm Name")
        return false
      }
      if (!formData.contactPerson.trim()) {
        setErrorMessage("Please enter Contact Person / Proprietor Name")
        return false
      }
      const cleanPhone = formData.phone.replace(/\D/g, "")
      if (!cleanPhone || cleanPhone.length < 10) {
        setErrorMessage("Please enter a valid 10-digit mobile number")
        return false
      }
    }

    if (!isSkip && (currentStep === 2 || (targetStep && targetStep > 2 && currentStep > 1))) {
      if (!formData.address.trim() && !formData.officeAddress.trim()) {
        setErrorMessage("Please enter Factory / Mill or Office Address")
        return false
      }
      if (!formData.city.trim()) {
        setErrorMessage("Please enter City")
        return false
      }
    }

    return true
  }

  const handleSkipToSubmit = () => {
    if (validateCurrentStep(5, true)) {
      setCurrentStep(5)
      window.scrollTo({ top: 0, behavior: "smooth" })
    }
  }

  const goToNextStep = () => {
    if (validateCurrentStep(currentStep + 1)) {
      setCurrentStep((prev) => Math.min(prev + 1, 5))
      window.scrollTo({ top: 0, behavior: "smooth" })
    }
  }

  const goToPrevStep = () => {
    setErrorMessage(null)
    setCurrentStep((prev) => Math.max(prev - 1, 1))
    window.scrollTo({ top: 0, behavior: "smooth" })
  }

  // Firebase Phone Auth - Setup invisible reCAPTCHA
  const setupRecaptcha = () => {
    if (!recaptchaVerifierRef.current) {
      try {
        recaptchaVerifierRef.current = new RecaptchaVerifier(
          auth,
          "supplier-recaptcha-container",
          {
            size: "invisible",
            callback: () => {},
            "expired-callback": () => {
              setErrorMessage("reCAPTCHA expired. Please try sending OTP again.")
            },
          }
        )
      } catch (err: any) {
        console.error("Recaptcha setup error:", err)
      }
    }
  }

  // Send OTP
  const handleSendOtp = async () => {
    setErrorMessage(null)
    const cleanPhone = formData.phone.replace(/\D/g, "").slice(-10)
    if (cleanPhone.length !== 10) {
      setErrorMessage("Please enter a valid 10-digit mobile number")
      return
    }

    setIsSendingOtp(true)
    try {
      setupRecaptcha()
      const formattedPhone = `+91${cleanPhone}`
      const appVerifier = recaptchaVerifierRef.current
      if (!appVerifier) throw new Error("Recaptcha not initialized")

      const confirmation = await signInWithPhoneNumber(auth, formattedPhone, appVerifier)
      setConfirmationResult(confirmation)
      setOtpSent(true)
      setResendTimer(30)
    } catch (err: any) {
      console.error("SMS Send Error:", err)
      if (err.code === "auth/invalid-phone-number") {
        setErrorMessage("Invalid mobile number format.")
      } else if (err.code === "auth/too-many-requests") {
        setErrorMessage("Too many SMS attempts. Please try again later.")
      } else {
        setErrorMessage(err.message || "Failed to send OTP. Please check your connection.")
      }
    } finally {
      setIsSendingOtp(false)
    }
  }

  // Verify OTP and Submit Form to Firebase RTDB
  const handleVerifyAndSubmit = async () => {
    setErrorMessage(null)
    if (!confirmationResult) {
      setErrorMessage("Please request an OTP first.")
      return
    }
    if (!otpCode.trim() || otpCode.trim().length < 6) {
      setErrorMessage("Please enter the 6-digit verification code.")
      return
    }

    setIsVerifyingOtp(true)
    try {
      const userCredential = await confirmationResult.confirm(otpCode.trim())
      const verifiedUser = userCredential.user

      const cleanGstin = formData.gstin.trim().toUpperCase()
      const cleanPhone = formData.phone.replace(/\D/g, "").slice(-10)
      const primaryKey = cleanGstin || cleanPhone
      const keyType: "GSTIN" | "PHONE" = cleanGstin ? "GSTIN" : "PHONE"
      const requestId = cleanGstin ? `req_sup_gst_${cleanGstin}` : `req_sup_phone_${cleanPhone}`
      const reqRef = ref(rtdb, `supplier_registration_requests/${requestId}`)

      const finalMarket = formData.marketArea === "Other / Outside Ahmedabad" && formData.customMarket.trim()
        ? formData.customMarket.trim()
        : formData.marketArea

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
        address: formData.address.trim(),
        officeAddress: formData.officeAddress.trim() || formData.address.trim(),
        marketArea: finalMarket,
        city: formData.city.trim() || "Ahmedabad",
        district: formData.district.trim(),
        state: formData.state.trim() || "Gujarat",
        pincode: formData.pincode.trim(),
        mapLink: formData.mapLink.trim(),
        productsMade: selectedCategories.join(", "),
        categories: selectedCategories.join(", "),
        priceRange: formData.priceRange.trim(),
        gstin: cleanGstin,
        panNumber: formData.panNumber.trim().toUpperCase(),
        bankName: formData.bankName.trim(),
        accountNumber: formData.accountNumber.trim(),
        ifscCode: formData.ifscCode.trim().toUpperCase(),
        visitingCardPhotoUri: formData.visitingCardPhotoUri || "",
        shopPhotoUri: formData.shopPhotoUri || "",
        gstCertPhotoUri: formData.gstCertPhotoUri || "",
        panPhotoUri: formData.panPhotoUri || "",
        notes: formData.notes.trim(),
        status: "PENDING",
        phoneVerified: true,
        verificationUid: verifiedUser.uid,
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
      console.error("Submission error:", err)
      if (err.code === "auth/invalid-verification-code") {
        setErrorMessage("Invalid OTP code. Please re-check the SMS.")
      } else {
        setErrorMessage(err.message || "Verification failed. Please try again.")
      }
    } finally {
      setIsVerifyingOtp(false)
    }
  }

  // SUCCESS VIEW
  if (submittedRequestId) {
    const cleanGst = formData.gstin.trim().toUpperCase()
    const cleanPhone = formData.phone.replace(/\D/g, "").slice(-10)
    const primaryKeyDisplay = cleanGst ? `${cleanGst} (GSTIN)` : `+91 ${cleanPhone} (Mobile)`
    const shareMessage = `Namaste! We have submitted our supplier registration with Himat Textile.%0A%0A*Mill / Firm:* ${encodeURIComponent(formData.firmName)}%0A*Contact:* ${encodeURIComponent(formData.contactPerson)} (+91 ${cleanPhone})%0A${cleanGst ? `*GSTIN:* ${encodeURIComponent(cleanGst)}%0A` : `*Mobile:* +91 ${cleanPhone}%0A`}*Type:* ${formData.type}%0A*Market:* ${encodeURIComponent(formData.marketArea)}%0A*Ref ID:* ${submittedRequestId}`

    return (
      <div className="min-h-screen bg-gradient-to-b from-zinc-50 to-zinc-100 dark:from-zinc-950 dark:to-zinc-900 py-10 px-4 sm:px-6">
        <div className="max-w-xl mx-auto bg-white dark:bg-zinc-900 rounded-2xl shadow-xl border border-zinc-200 dark:border-zinc-800 overflow-hidden text-center p-8 sm:p-10">
          <div className="w-16 h-16 bg-emerald-100 dark:bg-emerald-950/60 rounded-full flex items-center justify-center mx-auto mb-5 text-emerald-600 dark:text-emerald-400 shadow-inner">
            <CheckCircle2 className="w-9 h-9" />
          </div>

          <h2 className="text-2xl font-black text-zinc-900 dark:text-zinc-50 tracking-tight">
            Registration Submitted Successfully!
          </h2>
          <p className="text-sm text-zinc-600 dark:text-zinc-400 mt-2">
            Thank you for registering your textile mill / wholesale supply firm with <strong>Himat Textile</strong>. Our admin team will review your business profile and KYC documents.
          </p>

          <div className="mt-6 p-4 bg-zinc-50 dark:bg-zinc-800/60 rounded-xl border border-zinc-200 dark:border-zinc-700 text-left space-y-2">
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">{formData.gstin ? "GSTIN:" : "Mobile:"}</span>
              <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">
                {formData.gstin || `+91 ${formData.phone.replace(/\D/g, "").slice(-10)}`}
              </span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Reference ID:</span>
              <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">{submittedRequestId}</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Mill / Firm:</span>
              <span className="font-semibold text-zinc-900 dark:text-zinc-100">{formData.firmName}</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Contact Person:</span>
              <span className="font-medium text-zinc-800 dark:text-zinc-200">{formData.contactPerson} ({formData.phone})</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Classification:</span>
              <span className="font-semibold text-emerald-600 dark:text-emerald-400">{formData.type}</span>
            </div>
          </div>

          <div className="mt-8 flex flex-col sm:flex-row gap-3 justify-center">
            <a
              href={`https://api.whatsapp.com/send?phone=919825000000&text=${shareMessage}`}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl font-bold text-xs bg-emerald-600 hover:bg-emerald-700 text-white shadow-md transition-all"
            >
              <MessageSquare className="w-4 h-4" />
              Notify Admin on WhatsApp
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

  // WIZARD VIEW
  return (
    <div className="min-h-screen bg-gradient-to-b from-zinc-50 via-zinc-100 to-zinc-200 dark:from-zinc-950 dark:via-zinc-900 dark:to-zinc-950 py-8 px-4 sm:px-6">
      {/* Invisible reCAPTCHA container */}
      <div id="supplier-recaptcha-container" ref={recaptchaWrapperRef}></div>

      <div className="max-w-2xl mx-auto">
        {/* Header with Himat Textile Logo */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-3 mb-2">
            <img
              src={HIMAT_LOGO_DATA_URI}
              alt="Himat Textile"
              className="h-10 w-auto rounded-lg shadow-sm"
            />
            <h1 className="text-2xl font-black tracking-tight text-zinc-900 dark:text-zinc-50">
              Himat Textile
            </h1>
          </div>

          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 text-xs font-semibold mb-2">
            <Factory className="w-3.5 h-3.5" />
            <span>Fabric Mill & Supplier Onboarding</span>
          </div>

          <p className="text-xs text-zinc-600 dark:text-zinc-400 max-w-md mx-auto">
            Register your manufacturing unit, mill, or wholesale agency to supply fabrics and garments to our retail network across India.
          </p>

          {/* Language Toggle */}
          <div className="mt-4 inline-flex items-center p-1 rounded-xl bg-zinc-200/80 dark:bg-zinc-800 border border-zinc-300 dark:border-zinc-700">
            <button
              type="button"
              onClick={() => handleLanguageSwitch("en")}
              className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                lang === "en"
                  ? "bg-white dark:bg-zinc-700 text-zinc-900 dark:text-zinc-100 shadow-sm"
                  : "text-zinc-600 dark:text-zinc-400 hover:text-zinc-900"
              }`}
            >
              English
            </button>
            <button
              type="button"
              onClick={() => handleLanguageSwitch("hi")}
              className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                lang === "hi"
                  ? "bg-white dark:bg-zinc-700 text-zinc-900 dark:text-zinc-100 shadow-sm"
                  : "text-zinc-600 dark:text-zinc-400 hover:text-zinc-900"
              }`}
            >
              हिंदी
            </button>
            <button
              type="button"
              onClick={() => handleLanguageSwitch("gu")}
              className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                lang === "gu"
                  ? "bg-white dark:bg-zinc-700 text-zinc-900 dark:text-zinc-100 shadow-sm"
                  : "text-zinc-600 dark:text-zinc-400 hover:text-zinc-900"
              }`}
            >
              ગુજરાતી
            </button>
          </div>
        </div>

        {/* Wizard Steps Indicator */}
        <div className="mb-6 bg-white dark:bg-zinc-900 rounded-xl p-3 border border-zinc-200 dark:border-zinc-800 shadow-sm">
          <div className="flex items-center justify-between">
            {[
              { num: 1, label: "Profile" },
              { num: 2, label: "Location" },
              { num: 3, label: "Fabrics" },
              { num: 4, label: "Photos" },
              { num: 5, label: "Verify" },
            ].map((step, idx) => (
              <React.Fragment key={step.num}>
                <div className="flex flex-col items-center">
                  <div
                    className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                      currentStep === step.num
                        ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 ring-2 ring-emerald-500"
                        : currentStep > step.num
                        ? "bg-emerald-600 text-white"
                        : "bg-zinc-100 dark:bg-zinc-800 text-zinc-400"
                    }`}
                  >
                    {currentStep > step.num ? <Check className="w-3.5 h-3.5" /> : step.num}
                  </div>
                  <span className="text-[10px] font-medium text-zinc-600 dark:text-zinc-400 mt-1 hidden sm:block">
                    {step.label}
                  </span>
                </div>
                {idx < 4 && (
                  <div
                    className={`flex-1 h-0.5 mx-1.5 transition-all ${
                      currentStep > idx + 1 ? "bg-emerald-600" : "bg-zinc-200 dark:bg-zinc-800"
                    }`}
                  />
                )}
              </React.Fragment>
            ))}
          </div>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="mb-6 p-3 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800/60 flex items-start gap-2.5 text-xs text-red-700 dark:text-red-300">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Step Card */}
        <div className="bg-white dark:bg-zinc-900 rounded-2xl shadow-lg border border-zinc-200 dark:border-zinc-800 p-6 sm:p-8">
          {/* STEP 1: BUSINESS & CONTACT PROFILE */}
          {currentStep === 1 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <Building2 className="w-4 h-4 text-emerald-600" />
                  <span>Step 1: Mill & Contact Profile</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  Enter your firm name and primary proprietor/manager details.
                </p>
              </div>

              {/* GSTIN (Auto-Fetch) */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    {lang === "hi"
                      ? "जीएसटी नंबर (वैकल्पिक)"
                      : lang === "gu"
                      ? "GST નંબર (વૈકલ્પિક)"
                      : "GSTIN Number (Optional)"}
                  </label>
                  {formData.gstin && (
                    <button
                      type="button"
                      onClick={handleClearGst}
                      className="text-[11px] text-zinc-400 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                    >
                      {lang === "hi" ? "साफ़ करें" : lang === "gu" ? "સાફ કરો" : "Clear"}
                    </button>
                  )}
                </div>
                <div className="relative">
                  <input
                    type="text"
                    maxLength={15}
                    placeholder="24AAAAA0000A1Z5"
                    value={formData.gstin}
                    onChange={(e) => {
                      const val = e.target.value.toUpperCase().replace(/[^0-9A-Z]/g, "")
                      handleInputChange("gstin", val)
                      if (val.length === 15 && isValidGstin(val)) {
                        handleGstLookup(val)
                      } else if (val.length === 0) {
                        setGstFeedback({ type: null, message: "" })
                      } else if (val.length < 15 && gstFeedback.type) {
                        setGstFeedback({ type: null, message: "" })
                      }
                    }}
                    onBlur={() => {
                      if (formData.gstin.length === 15 && isValidGstin(formData.gstin) && !isFetchingGst && !gstFeedback.type) {
                        handleGstLookup(formData.gstin)
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
                  <p
                    className={`text-[11px] mt-1 flex items-center gap-1 ${
                      gstFeedback.type === "success" || gstFeedback.type === "offline"
                        ? "text-emerald-600 dark:text-emerald-400 font-medium"
                        : "text-amber-600 dark:text-amber-400"
                    }`}
                  >
                    <span>{gstFeedback.type === "success" ? "✓" : "ℹ"}</span>
                    <span>{gstFeedback.message}</span>
                  </p>
                )}
              </div>

              {/* Classification Toggle */}
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1.5">
                  Supplier Classification <span className="text-red-500">*</span>
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => handleInputChange("type", "Manufacturer")}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all flex items-center justify-center gap-2 ${
                      formData.type === "Manufacturer"
                        ? "border-emerald-600 bg-emerald-50 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300 ring-1 ring-emerald-600"
                        : "border-zinc-300 dark:border-zinc-700 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50"
                    }`}
                  >
                    <Factory className="w-4 h-4" />
                    <span>Manufacturer / Mill</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => handleInputChange("type", "Wholesaler")}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold transition-all flex items-center justify-center gap-2 ${
                      formData.type === "Wholesaler"
                        ? "border-emerald-600 bg-emerald-50 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300 ring-1 ring-emerald-600"
                        : "border-zinc-300 dark:border-zinc-700 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50"
                    }`}
                  >
                    <Store className="w-4 h-4" />
                    <span>Wholesaler / Stockist</span>
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Mill / Firm Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.firmName}
                  onChange={(e) => handleInputChange("firmName", e.target.value)}
                  placeholder="e.g. Radheshyam Textile Mills Ltd"
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Contact Person / Proprietor <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.contactPerson}
                  onChange={(e) => handleInputChange("contactPerson", e.target.value)}
                  placeholder="e.g. Rajeshbhai Patel"
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Mobile Number (for SMS OTP) <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <span className="absolute left-3 top-2.5 text-xs text-zinc-500 font-semibold">+91</span>
                    <input
                      type="tel"
                      maxLength={10}
                      value={formData.phone}
                      onChange={(e) => handleInputChange("phone", e.target.value)}
                      placeholder="9825012345"
                      className="w-full h-10 pl-11 pr-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Brand Name (Optional)
                  </label>
                  <input
                    type="text"
                    value={formData.brand}
                    onChange={(e) => handleInputChange("brand", e.target.value)}
                    placeholder="e.g. Radhey Silk, RT Cotton"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              {/* WhatsApp Number */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                    WhatsApp Number
                  </label>
                  <label className="inline-flex items-center gap-1 text-[11px] text-zinc-500 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={formData.sameAsMobile}
                      onChange={(e) => handleInputChange("sameAsMobile", e.target.checked)}
                      className="rounded border-zinc-300 text-emerald-600 focus:ring-emerald-500"
                    />
                    <span>Same as Mobile</span>
                  </label>
                </div>
                {!formData.sameAsMobile && (
                  <div className="relative">
                    <span className="absolute left-3 top-2.5 text-xs text-zinc-500 font-semibold">+91</span>
                    <input
                      type="tel"
                      maxLength={10}
                      value={formData.phone2}
                      onChange={(e) => handleInputChange("phone2", e.target.value)}
                      placeholder="WhatsApp phone number"
                      className="w-full h-10 pl-11 pr-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Email Address (Optional)
                </label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => handleInputChange("email", e.target.value)}
                  placeholder="e.g. sales@radheytextile.com"
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>
            </div>
          )}

          {/* STEP 2: LOCATION & MILL DETAILS */}
          {currentStep === 2 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-emerald-600" />
                  <span>Step 2: Location & Market Cluster</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  Specify your textile market area and manufacturing/office addresses.
                </p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Ahmedabad Textile Market / Cluster
                </label>
                <select
                  value={formData.marketArea}
                  onChange={(e) => handleInputChange("marketArea", e.target.value)}
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                >
                  {AHMEDABAD_MARKETS.map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>
              </div>

              {formData.marketArea === "Other / Outside Ahmedabad" && (
                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Custom Market / City Name
                  </label>
                  <input
                    type="text"
                    value={formData.customMarket}
                    onChange={(e) => handleInputChange("customMarket", e.target.value)}
                    placeholder="e.g. Surat Ring Road Market"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Mill / Factory Address <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={2}
                  value={formData.address}
                  onChange={(e) => handleInputChange("address", e.target.value)}
                  placeholder="Factory plot, GIDC phase, shed number, road..."
                  className="w-full p-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Market Office / Shop Address (Optional)
                </label>
                <textarea
                  rows={2}
                  value={formData.officeAddress}
                  onChange={(e) => handleInputChange("officeAddress", e.target.value)}
                  placeholder="Market shop number, floor, tower..."
                  className="w-full p-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    City <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={formData.city}
                    onChange={(e) => handleInputChange("city", e.target.value)}
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    State
                  </label>
                  <input
                    type="text"
                    value={formData.state}
                    onChange={(e) => handleInputChange("state", e.target.value)}
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Pincode
                  </label>
                  <input
                    type="text"
                    maxLength={6}
                    value={formData.pincode}
                    onChange={(e) => handleInputChange("pincode", e.target.value)}
                    placeholder="380002"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

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

          {/* STEP 3: FABRICS, GARMENTS & COMMERCIALS */}
          {currentStep === 3 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <Tag className="w-4 h-4 text-emerald-600" />
                  <span>Step 3: Fabrics & Commercials</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  Select the types of fabrics/garments your unit produces and your commercial terms.
                </p>
              </div>

              {/* Categories Pills */}
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-2">
                  Fabrics & Products Manufactured (Select all that apply)
                </label>
                <div className="flex flex-wrap gap-1.5 mb-2">
                  {SUPPLIER_FABRIC_CATEGORIES.map((cat) => {
                    const isSelected = selectedCategories.includes(cat)
                    return (
                      <button
                        key={cat}
                        type="button"
                        onClick={() => toggleCategory(cat)}
                        className={`px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
                          isSelected
                            ? "bg-emerald-600 text-white shadow-sm"
                            : "bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-200"
                        }`}
                      >
                        {isSelected && <Check className="w-3 h-3 inline mr-1" />}
                        {cat}
                      </button>
                    )
                  })}
                </div>

                <div className="flex gap-2 mt-2">
                  <input
                    type="text"
                    value={customCategory}
                    onChange={(e) => setCustomCategory(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault()
                        addCustomCategory()
                      }
                    }}
                    placeholder="Add other fabric or product..."
                    className="flex-1 h-9 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                  <button
                    type="button"
                    onClick={addCustomCategory}
                    className="px-3 h-9 bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 text-xs font-bold rounded-xl"
                  >
                    Add
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Price Range / Meter or Piece (Optional)
                  </label>
                  <input
                    type="text"
                    value={formData.priceRange}
                    onChange={(e) => handleInputChange("priceRange", e.target.value)}
                    placeholder="e.g. ₹95 - ₹220 / meter"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    GSTIN Number (Optional)
                  </label>
                  <input
                    type="text"
                    maxLength={15}
                    value={formData.gstin}
                    onChange={(e) => {
                      const val = e.target.value.toUpperCase().replace(/[^0-9A-Z]/g, "")
                      handleInputChange("gstin", val)
                      if (val.length >= 12 && !formData.panNumber) {
                        const pan = extractPanFromGstin(val)
                        if (pan) handleInputChange("panNumber", pan)
                      }
                    }}
                    placeholder="24ABCDE1234F1Z5"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 uppercase font-mono"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    PAN Number (Optional)
                  </label>
                  <input
                    type="text"
                    maxLength={10}
                    value={formData.panNumber}
                    onChange={(e) => handleInputChange("panNumber", e.target.value.toUpperCase().replace(/[^0-9A-Z]/g, ""))}
                    placeholder="ABCDE1234F"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 uppercase font-mono"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Bank Name (Optional)
                  </label>
                  <input
                    type="text"
                    value={formData.bankName}
                    onChange={(e) => handleInputChange("bankName", e.target.value)}
                    placeholder="e.g. HDFC Bank, SBI"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Additional Notes / Supply Capabilities
                </label>
                <textarea
                  rows={2}
                  value={formData.notes}
                  onChange={(e) => handleInputChange("notes", e.target.value)}
                  placeholder="e.g. Minimum order quantity, ready stock availability, dispatch timeline..."
                  className="w-full p-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>
            </div>
          )}

          {/* STEP 4: KYC & PHOTOS */}
          {currentStep === 4 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  <span>Step 4: Verification Photos</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  Upload visiting cards and photos to fast-track your supplier approval.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <FileUpload
                    label="Visiting Card Photo"
                    folder="supplier_requests/visiting_cards"
                    value={formData.visitingCardPhotoUri}
                    onChange={(url: string) => handleInputChange("visitingCardPhotoUri", url)}
                  />
                </div>

                <div>
                  <FileUpload
                    label="Mill / Factory Front Photo"
                    folder="supplier_requests/mill_front"
                    value={formData.shopPhotoUri}
                    onChange={(url: string) => handleInputChange("shopPhotoUri", url)}
                  />
                </div>

                <div>
                  <FileUpload
                    label="GST Certificate (Optional)"
                    folder="supplier_requests/gst_certs"
                    value={formData.gstCertPhotoUri}
                    onChange={(url: string) => handleInputChange("gstCertPhotoUri", url)}
                  />
                </div>

                <div>
                  <FileUpload
                    label="PAN Card Photo (Optional)"
                    folder="supplier_requests/pan_cards"
                    value={formData.panPhotoUri}
                    onChange={(url: string) => handleInputChange("panPhotoUri", url)}
                  />
                </div>
              </div>
            </div>
          )}

          {/* STEP 5: PHONE VERIFICATION & SUBMISSION */}
          {currentStep === 5 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <Phone className="w-4 h-4 text-emerald-600" />
                  <span>Step 5: Verify Mobile & Submit</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  Verify your mobile number via 6-digit SMS code to finalize your registration.
                </p>
              </div>

              <div className="bg-zinc-50 dark:bg-zinc-800/60 p-4 rounded-xl border border-zinc-200 dark:border-zinc-700 space-y-2">
                <div className="flex justify-between text-xs">
                  <span className="text-zinc-500">Firm Name:</span>
                  <span className="font-bold text-zinc-900 dark:text-zinc-100">{formData.firmName}</span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-zinc-500">{formData.gstin ? "GSTIN:" : "Mobile:"}</span>
                  <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">
                    {formData.gstin || `+91 ${formData.phone.replace(/\D/g, "").slice(-10)}`}
                  </span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-zinc-500">Classification:</span>
                  <span className="font-semibold text-emerald-600">{formData.type}</span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-zinc-500">Contact:</span>
                  <span className="font-medium text-zinc-800 dark:text-zinc-200">{formData.contactPerson} ({formData.phone})</span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-zinc-500">Market / City:</span>
                  <span className="font-medium text-zinc-800 dark:text-zinc-200">{formData.marketArea}, {formData.city}</span>
                </div>
              </div>

              {!otpSent ? (
                <div className="text-center py-4 space-y-3">
                  <p className="text-xs text-zinc-600 dark:text-zinc-400">
                    We will send a one-time password (OTP) via SMS to <strong>+91 {formData.phone}</strong>.
                  </p>
                  <button
                    type="button"
                    onClick={handleSendOtp}
                    disabled={isSendingOtp}
                    className="inline-flex items-center justify-center gap-2 px-6 py-3 rounded-xl font-bold text-xs bg-emerald-600 hover:bg-emerald-700 text-white shadow-md transition-all disabled:opacity-50"
                  >
                    {isSendingOtp ? (
                      <>
                        <RefreshCw className="w-4 h-4 animate-spin" />
                        <span>Sending OTP...</span>
                      </>
                    ) : (
                      <>
                        <Send className="w-4 h-4" />
                        <span>Send Verification OTP</span>
                      </>
                    )}
                  </button>
                </div>
              ) : (
                <div className="space-y-4 pt-2">
                  <div>
                    <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                      Enter 6-Digit OTP Code
                    </label>
                    <input
                      type="text"
                      maxLength={6}
                      value={otpCode}
                      onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ""))}
                      placeholder="123456"
                      className="w-full h-12 text-center tracking-widest text-lg font-bold rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>

                  <div className="flex items-center justify-between text-xs">
                    <span className="text-zinc-500">Didn't receive code?</span>
                    {resendTimer > 0 ? (
                      <span className="text-zinc-400">Resend in {resendTimer}s</span>
                    ) : (
                      <button
                        type="button"
                        onClick={handleSendOtp}
                        disabled={isSendingOtp}
                        className="text-emerald-600 hover:underline font-semibold"
                      >
                        Resend OTP
                      </button>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={handleVerifyAndSubmit}
                    disabled={isVerifyingOtp || otpCode.length < 6}
                    className="w-full h-11 rounded-xl font-bold text-xs bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 text-white shadow-md transition-all disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    {isVerifyingOtp ? (
                      <>
                        <RefreshCw className="w-4 h-4 animate-spin" />
                        <span>Verifying & Submitting...</span>
                      </>
                    ) : (
                      <>
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Verify & Submit Registration</span>
                      </>
                    )}
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Navigation Buttons */}
          <div className="mt-8 pt-4 border-t border-zinc-100 dark:border-zinc-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              {currentStep > 1 ? (
                <button
                  type="button"
                  onClick={goToPrevStep}
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-zinc-300 dark:border-zinc-700 text-xs font-semibold text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-all"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Back</span>
                </button>
              ) : null}

              {currentStep < 5 && isStep1Complete && (
                <button
                  type="button"
                  onClick={handleSkipToSubmit}
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-emerald-600 dark:border-emerald-500 bg-emerald-50/80 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 text-xs font-bold transition-all shadow-xs"
                >
                  <Sparkles className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
                  <span>
                    {lang === "hi"
                      ? "सीधे सबमिट करें"
                      : lang === "gu"
                      ? "સીધા સબમિટ કરો"
                      : "Skip to Submit"}
                  </span>
                  <ArrowRight className="w-3 h-3 text-emerald-600 dark:text-emerald-400" />
                </button>
              )}
            </div>

            {currentStep < 5 && (
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
  )
}
