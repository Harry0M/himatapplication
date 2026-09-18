import React, { useState, useEffect, useRef } from "react"
import {
  Building2,
  User,
  Phone,
  Mail,
  MapPin,
  Truck,
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
  Globe
} from "lucide-react"
import { RecaptchaVerifier, signInWithPhoneNumber, ConfirmationResult } from "firebase/auth"
import { ref, set } from "firebase/database"
import { auth, rtdb } from "../lib/firebase"
import { FileUpload } from "../components/ui/FileUpload"
import { GARMENT_CATEGORIES } from "../lib/constants"
import { CustomerRegistrationRequest } from "../types"
import { REGISTRATION_TRANSLATIONS, RegistrationLang } from "../lib/registrationI18n"

export function CustomerRegistrationView() {
  const [currentStep, setCurrentStep] = useState<number>(1)

  // Language State - Defaults to English ('en') with user toggle
  const [lang, setLang] = useState<RegistrationLang>(() => {
    return (localStorage.getItem("himat_reg_lang") as RegistrationLang) || "en"
  })

  const t = REGISTRATION_TRANSLATIONS[lang]

  const handleLanguageSwitch = (newLang: RegistrationLang) => {
    setLang(newLang)
    localStorage.setItem("himat_reg_lang", newLang)
  }

  // Form State
  const [formData, setFormData] = useState({
    // Step 1: Business Profile
    firmName: "",
    marketArea: "",
    address: "",
    shopAddress: "",
    city: "Ahmedabad",
    district: "",
    state: "Gujarat",
    pincode: "",
    shopMapLink: "",

    // Step 2: Owner & Contact
    name: "",
    phone: "",
    phone2: "",
    sameAsMobile: true,
    email: "",

    // Step 3: KYC & Docs
    gstin: "",
    panNumber: "",
    shopPhotoUri: "",
    gstCertPhotoUri: "",
    panPhotoUri: "",
    aadharPhotoUri: "",

    // Step 4: Transport & Bank
    preferredTransporterName: "",
    transportPreference: "",
    bankName: "",
    accountNumber: "",
    ifscCode: "",
    notes: "",
  })

  // Selected Garment Categories (Pills)
  const [selectedGarments, setSelectedGarments] = useState<string[]>([])
  const [customGarment, setCustomGarment] = useState<string>("")

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

  // Toggle Garment Category
  const toggleGarment = (cat: string) => {
    setSelectedGarments((prev) =>
      prev.includes(cat) ? prev.filter((c) => c !== cat) : [...prev, cat]
    )
  }

  const addCustomGarment = () => {
    if (customGarment.trim() && !selectedGarments.includes(customGarment.trim())) {
      setSelectedGarments((prev) => [...prev, customGarment.trim()])
      setCustomGarment("")
    }
  }

  // Step Validation with localized error messages
  const validateCurrentStep = (): boolean => {
    setErrorMessage(null)
    if (currentStep === 1) {
      if (!formData.firmName.trim()) {
        setErrorMessage(t.errFirmName)
        return false
      }
      if (!formData.address.trim()) {
        setErrorMessage(t.errAddress)
        return false
      }
      if (!formData.city.trim()) {
        setErrorMessage(t.errCity)
        return false
      }
    } else if (currentStep === 2) {
      if (!formData.name.trim()) {
        setErrorMessage(t.errOwnerName)
        return false
      }
      const cleanPhone = formData.phone.replace(/\D/g, "")
      if (cleanPhone.length !== 10) {
        setErrorMessage(t.errPhone)
        return false
      }
    }
    return true
  }

  const handleNextStep = () => {
    if (validateCurrentStep()) {
      setCurrentStep((prev) => Math.min(prev + 1, t.stepIndicators.length))
      window.scrollTo({ top: 0, behavior: "smooth" })
    }
  }

  const handlePrevStep = () => {
    setErrorMessage(null)
    setCurrentStep((prev) => Math.max(prev - 1, 1))
    window.scrollTo({ top: 0, behavior: "smooth" })
  }

  // Initialize Recaptcha Verifier
  const getRecaptchaVerifier = () => {
    if (recaptchaVerifierRef.current) {
      try {
        recaptchaVerifierRef.current.clear()
      } catch (e) {
        // ignore
      }
      recaptchaVerifierRef.current = null
    }

    const verifier = new RecaptchaVerifier(auth, "recaptcha-container", {
      size: "invisible",
      callback: () => {
        // Recaptcha resolved
      },
      "expired-callback": () => {
        setErrorMessage(t.errSecurityExpired)
      },
    })
    recaptchaVerifierRef.current = verifier
    return verifier
  }

  // Send SMS OTP via Firebase Phone Auth
  const handleSendOtp = async () => {
    setErrorMessage(null)
    const cleanPhone = formData.phone.replace(/\D/g, "").slice(-10)
    if (cleanPhone.length !== 10) {
      setErrorMessage(t.errPhone)
      return
    }

    setIsSendingOtp(true)
    try {
      const fullPhoneNumber = `+91${cleanPhone}`
      const appVerifier = getRecaptchaVerifier()
      const confirmation = await signInWithPhoneNumber(auth, fullPhoneNumber, appVerifier)
      setConfirmationResult(confirmation)
      setOtpSent(true)
      setResendTimer(60)
    } catch (err: any) {
      console.error("Firebase Phone Auth error:", err)
      if (err.code === "auth/invalid-phone-number") {
        setErrorMessage(t.errPhone)
      } else if (err.code === "auth/too-many-requests") {
        setErrorMessage(t.errTooManyAttempts)
      } else if (err.code === "auth/quota-exceeded") {
        setErrorMessage(t.errQuotaExceeded)
      } else {
        setErrorMessage(err.message || t.errGenericPhoneAuth)
      }
    } finally {
      setIsSendingOtp(false)
    }
  }

  // Verify OTP and Submit Form to Firebase RTDB
  const handleVerifyAndSubmit = async () => {
    setErrorMessage(null)
    if (!confirmationResult) {
      setErrorMessage(t.errSendOtpFirst)
      return
    }
    if (!otpCode.trim() || otpCode.trim().length < 6) {
      setErrorMessage(t.errOtpLength)
      return
    }

    setIsVerifyingOtp(true)
    try {
      // 1. Confirm OTP with Firebase Auth
      const userCredential = await confirmationResult.confirm(otpCode.trim())
      const verifiedUser = userCredential.user

      // 2. Prepare Registration Request Payload
      const requestId = `req_${Date.now()}_${Math.floor(Math.random() * 1000)}`
      const reqRef = ref(rtdb, `customer_registration_requests/${requestId}`)

      const payload: CustomerRegistrationRequest = {
        id: requestId,
        firmName: formData.firmName.trim(),
        name: formData.name.trim(),
        phone: `+91${formData.phone.replace(/\D/g, "").slice(-10)}`,
        phone2: formData.phone2.trim() ? `+91${formData.phone2.replace(/\D/g, "").slice(-10)}` : "",
        email: formData.email.trim(),
        address: formData.address.trim(),
        shopAddress: formData.shopAddress.trim() || formData.address.trim(),
        marketArea: formData.marketArea.trim(),
        city: formData.city.trim() || "Ahmedabad",
        district: formData.district.trim(),
        state: formData.state.trim() || "Gujarat",
        pincode: formData.pincode.trim(),
        shopMapLink: formData.shopMapLink.trim(),
        garmentTypes: selectedGarments.join(", "),
        gstin: formData.gstin.trim().toUpperCase(),
        panNumber: formData.panNumber.trim().toUpperCase(),
        preferredTransporterName: formData.preferredTransporterName.trim(),
        transportPreference: formData.transportPreference.trim(),
        bankName: formData.bankName.trim(),
        accountNumber: formData.accountNumber.trim(),
        ifscCode: formData.ifscCode.trim().toUpperCase(),
        shopPhotoUri: formData.shopPhotoUri || "",
        gstCertPhotoUri: formData.gstCertPhotoUri || "",
        panPhotoUri: formData.panPhotoUri || "",
        aadharPhotoUri: formData.aadharPhotoUri || "",
        notes: formData.notes.trim(),
        status: "PENDING",
        phoneVerified: true,
        verificationUid: verifiedUser.uid,
        createdAt: Date.now(),
      }

      // Filter out undefined keys for Firebase RTDB
      const cleanPayload: Record<string, any> = {}
      for (const [k, v] of Object.entries(payload)) {
        if (v !== undefined) cleanPayload[k] = v
      }

      await set(reqRef, cleanPayload)
      setSubmittedRequestId(requestId)
      window.scrollTo({ top: 0, behavior: "smooth" })
    } catch (err: any) {
      console.error("OTP verification or submission error:", err)
      if (err.code === "auth/invalid-verification-code") {
        setErrorMessage(t.errOtpInvalid)
      } else if (err.code === "auth/code-expired") {
        setErrorMessage(t.errOtpExpired)
      } else {
        setErrorMessage(err.message || t.errGenericPhoneAuth)
      }
    } finally {
      setIsVerifyingOtp(false)
    }
  }

  // Render Submitted Success Screen
  if (submittedRequestId) {
    const whatsappMsg = encodeURIComponent(
      lang === "en"
        ? `Hello Shree Himat Trading Company!\nI have submitted my new commercial account registration form online.\n\n📌 Firm Name: ${formData.firmName}\n👤 Proprietor: ${formData.name}\n📞 Mobile: +91 ${formData.phone.slice(-10)}\n📍 City: ${formData.city}\n🆔 Ref ID: ${submittedRequestId}\n\nPlease review our application and grant commercial account approval. Thank you!`
        : `नमस्कार श्री हिम्मत ट्रेडिंग कंपनी!\nमैंने नया व्यापारिक खाता खोलने के लिए ऑनलाइन पंजीकरण फॉर्म सबमिट किया है।\n\n📌 फर्म का नाम: ${formData.firmName}\n👤 संचालक: ${formData.name}\n📞 मोबाइल: +91 ${formData.phone.slice(-10)}\n📍 शहर: ${formData.city}\n🆔 संदर्भ क्रमांक (Ref ID): ${submittedRequestId}\n\nकृपया हमारे खाते की समीक्षा कर अनुमोदन (Approval) प्रदान करें। धन्यवाद!`
    )

    return (
      <div className="min-h-screen bg-gradient-to-b from-zinc-50 to-zinc-100 dark:from-zinc-950 dark:to-zinc-900 py-10 px-4 sm:px-6 flex items-center justify-center">
        <div className="max-w-xl w-full bg-white dark:bg-zinc-900 rounded-3xl shadow-xl border border-zinc-200 dark:border-zinc-800 p-6 sm:p-10 text-center space-y-6">
          {/* Header Language Switcher on Success Screen */}
          <div className="flex justify-end">
            <div className="inline-flex items-center bg-zinc-100 dark:bg-zinc-800 p-1 rounded-xl border border-zinc-200 dark:border-zinc-700 text-xs">
              <button
                type="button"
                onClick={() => handleLanguageSwitch("en")}
                className={`px-2.5 py-0.5 rounded-lg font-semibold transition-all ${
                  lang === "en"
                    ? "bg-white dark:bg-zinc-900 text-indigo-600 dark:text-indigo-400 shadow-2xs"
                    : "text-zinc-500 hover:text-zinc-800 dark:hover:text-zinc-200"
                }`}
              >
                English
              </button>
              <button
                type="button"
                onClick={() => handleLanguageSwitch("hi")}
                className={`px-2.5 py-0.5 rounded-lg font-semibold transition-all ${
                  lang === "hi"
                    ? "bg-white dark:bg-zinc-900 text-indigo-600 dark:text-indigo-400 shadow-2xs"
                    : "text-zinc-500 hover:text-zinc-800 dark:hover:text-zinc-200"
                }`}
              >
                हिंदी
              </button>
            </div>
          </div>

          <div className="mx-auto w-20 h-20 bg-emerald-100 dark:bg-emerald-950/60 rounded-full flex items-center justify-center text-emerald-600 dark:text-emerald-400 ring-8 ring-emerald-50 dark:ring-emerald-950/30">
            <CheckCircle2 className="w-10 h-10 animate-bounce" />
          </div>

          <div className="space-y-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
              <Sparkles className="w-3.5 h-3.5" /> {t.successBadge}
            </span>
            <h1 className="text-2xl sm:text-3xl font-bold text-zinc-900 dark:text-zinc-100">
              {t.successHeading} {formData.name}!
            </h1>
            <p className="text-sm text-zinc-600 dark:text-zinc-400 max-w-md mx-auto">
              <strong className="text-zinc-900 dark:text-zinc-100">{formData.firmName}</strong>'s {t.successSubtitle}
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-zinc-50 dark:bg-zinc-800/60 border border-zinc-200 dark:border-zinc-700 text-left space-y-2.5 text-xs">
            <div className="flex justify-between items-center py-1 border-b border-zinc-200/60 dark:border-zinc-700/60">
              <span className="text-muted-foreground">{t.refIdLabel}</span>
              <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">{submittedRequestId}</span>
            </div>
            <div className="flex justify-between items-center py-1 border-b border-zinc-200/60 dark:border-zinc-700/60">
              <span className="text-muted-foreground">{t.verifiedMobileLabel}</span>
              <span className="font-semibold text-zinc-900 dark:text-zinc-100 flex items-center gap-1">
                <Check className="w-3.5 h-3.5 text-emerald-600" /> +91 {formData.phone.slice(-10)}
              </span>
            </div>
            <div className="flex justify-between items-center py-1 border-b border-zinc-200/60 dark:border-zinc-700/60">
              <span className="text-muted-foreground">{t.cityMarketLabel}</span>
              <span className="font-semibold text-zinc-900 dark:text-zinc-100">
                {formData.city} {formData.marketArea ? `• ${formData.marketArea}` : ""}
              </span>
            </div>
            <div className="flex justify-between items-center py-1">
              <span className="text-muted-foreground">{t.statusLabel}</span>
              <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300">
                {t.statusPendingValue}
              </span>
            </div>
          </div>

          <div className="text-xs text-muted-foreground leading-relaxed px-2">
            {t.successNotice}
          </div>

          <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center">
            <a
              href={`https://wa.me/919427028169?text=${whatsappMsg}`}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-sm transition-all shadow-md hover:shadow-lg"
            >
              <MessageSquare className="w-4 h-4" />
              {t.notifyAdminWhatsAppBtn}
            </a>

            <button
              type="button"
              onClick={() => {
                setSubmittedRequestId(null)
                setCurrentStep(1)
                setOtpSent(false)
                setOtpCode("")
                setFormData({
                  firmName: "",
                  marketArea: "",
                  address: "",
                  shopAddress: "",
                  city: "Ahmedabad",
                  district: "",
                  state: "Gujarat",
                  pincode: "",
                  shopMapLink: "",
                  name: "",
                  phone: "",
                  phone2: "",
                  sameAsMobile: true,
                  email: "",
                  gstin: "",
                  panNumber: "",
                  shopPhotoUri: "",
                  gstCertPhotoUri: "",
                  panPhotoUri: "",
                  aadharPhotoUri: "",
                  preferredTransporterName: "",
                  transportPreference: "",
                  bankName: "",
                  accountNumber: "",
                  ifscCode: "",
                  notes: "",
                })
                setSelectedGarments([])
              }}
              className="inline-flex items-center justify-center gap-2 px-4 py-3 rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-700 dark:text-zinc-200 font-medium text-sm hover:bg-zinc-50 dark:hover:bg-zinc-700/50 transition-colors"
            >
              {t.registerAnotherBtn}
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-zinc-50/70 dark:bg-black text-zinc-900 dark:text-zinc-100 flex flex-col">
      {/* Hidden Recaptcha Container */}
      <div id="recaptcha-container"></div>

      {/* Top Header */}
      <header className="sticky top-0 z-30 bg-white/95 dark:bg-zinc-900/95 backdrop-blur border-b border-zinc-200 dark:border-zinc-800 px-4 py-3 sm:px-6">
        <div className="max-w-4xl mx-auto flex items-center justify-between gap-2">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-indigo-600 flex items-center justify-center text-white font-black text-lg shadow-sm">
              हि
            </div>
            <div>
              <h1 className="text-base font-bold tracking-tight text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                <span>{t.brandName}</span>
                <span className="hidden sm:inline-block text-[11px] px-2 py-0.5 rounded-full bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 font-medium border border-indigo-100 dark:border-indigo-900">
                  {t.brandTag}
                </span>
              </h1>
              <p className="text-xs text-muted-foreground truncate max-w-xs sm:max-w-md">
                {t.pageTitle}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 text-xs">
            {/* Language Switcher Pill */}
            <div className="flex items-center bg-zinc-100 dark:bg-zinc-800 p-1 rounded-xl border border-zinc-200 dark:border-zinc-700 shadow-2xs">
              <button
                type="button"
                onClick={() => handleLanguageSwitch("en")}
                className={`px-2.5 py-1 text-xs font-semibold rounded-lg transition-all ${
                  lang === "en"
                    ? "bg-white dark:bg-zinc-900 text-indigo-600 dark:text-indigo-400 shadow-xs"
                    : "text-zinc-500 hover:text-zinc-800 dark:hover:text-zinc-200"
                }`}
                title="Switch to English"
              >
                English
              </button>
              <button
                type="button"
                onClick={() => handleLanguageSwitch("hi")}
                className={`px-2.5 py-1 text-xs font-semibold rounded-lg transition-all ${
                  lang === "hi"
                    ? "bg-white dark:bg-zinc-900 text-indigo-600 dark:text-indigo-400 shadow-xs"
                    : "text-zinc-500 hover:text-zinc-800 dark:hover:text-zinc-200"
                }`}
                title="हिंदी में बदलें"
              >
                हिंदी
              </button>
            </div>

            {/* Help Support */}
            <a
              href="https://wa.me/919427028169"
              target="_blank"
              rel="noreferrer"
              className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-emerald-300 dark:border-emerald-800 bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 font-medium hover:bg-emerald-100 transition-colors"
            >
              <MessageSquare className="w-3.5 h-3.5" />
              <span>{t.whatsappHelp}</span>
            </a>
          </div>
        </div>
      </header>

      {/* Main Form Content */}
      <main className="flex-1 max-w-4xl w-full mx-auto p-4 sm:p-6 md:p-8 space-y-6">
        {/* Wizard Steps Navigation Bar */}
        <div className="bg-white dark:bg-zinc-900 p-3 sm:p-4 rounded-2xl border border-zinc-200 dark:border-zinc-800 shadow-sm">
          <div className="grid grid-cols-5 gap-1 sm:gap-2">
            {t.stepIndicators.map((step) => {
              const Icon = step.icon
              const isCompleted = currentStep > step.id
              const isCurrent = currentStep === step.id

              return (
                <button
                  key={step.id}
                  type="button"
                  onClick={() => {
                    if (step.id < currentStep || validateCurrentStep()) {
                      setCurrentStep(step.id)
                    }
                  }}
                  className={`flex flex-col sm:flex-row items-center gap-1.5 sm:gap-2 p-2 sm:p-2.5 rounded-xl text-center sm:text-left transition-all ${
                    isCurrent
                      ? "bg-indigo-50 dark:bg-indigo-950/50 text-indigo-700 dark:text-indigo-300 ring-1 ring-indigo-500/30"
                      : isCompleted
                      ? "text-emerald-600 dark:text-emerald-400 hover:bg-zinc-50 dark:hover:bg-zinc-800/50"
                      : "text-zinc-400 dark:text-zinc-600 hover:bg-zinc-50 dark:hover:bg-zinc-800/30"
                  }`}
                >
                  <div
                    className={`w-7 h-7 sm:w-8 sm:h-8 rounded-lg flex items-center justify-center text-xs font-bold transition-all ${
                      isCurrent
                        ? "bg-indigo-600 text-white shadow-sm"
                        : isCompleted
                        ? "bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300"
                        : "bg-zinc-100 dark:bg-zinc-800 text-zinc-500"
                    }`}
                  >
                    {isCompleted ? <Check className="w-4 h-4" /> : <Icon className="w-3.5 h-3.5" />}
                  </div>
                  <div className="min-w-0">
                    <p className="text-[11px] sm:text-xs font-bold truncate leading-tight">
                      {step.title}
                    </p>
                    <p className="hidden sm:block text-[10px] text-muted-foreground truncate">
                      {step.subtitle}
                    </p>
                  </div>
                </button>
              )
            })}
          </div>
        </div>

        {/* Global Error Banner */}
        {errorMessage && (
          <div className="p-3.5 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/50 flex items-start gap-2.5 text-xs text-red-700 dark:text-red-300 animate-in fade-in">
            <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
            <div className="flex-1 font-medium">{errorMessage}</div>
          </div>
        )}

        {/* Step Body */}
        <div className="bg-white dark:bg-zinc-900 rounded-3xl border border-zinc-200 dark:border-zinc-800 shadow-sm p-5 sm:p-8 space-y-6">
          {/* ============================================================== */}
          {/* STEP 1: BUSINESS PROFILE                                       */}
          {/* ============================================================== */}
          {currentStep === 1 && (
            <div className="space-y-6 animate-in fade-in">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3">
                <h2 className="text-lg sm:text-xl font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <Building2 className="w-5 h-5 text-indigo-600" />
                  <span>{t.step1Heading}</span>
                </h2>
                <p className="text-xs text-muted-foreground">
                  {t.step1Subheading}
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                {/* Firm Name */}
                <div className="sm:col-span-2 space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <span>{t.firmNameLabel}</span>
                    <span className="text-red-500 font-bold">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder={t.firmNamePlaceholder}
                    value={formData.firmName}
                    onChange={(e) => handleInputChange("firmName", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium text-sm"
                  />
                </div>

                {/* Market Area */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {t.marketAreaLabel}
                  </label>
                  <input
                    type="text"
                    placeholder={t.marketAreaPlaceholder}
                    value={formData.marketArea}
                    onChange={(e) => handleInputChange("marketArea", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>

                {/* City */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <span>{t.cityLabel}</span>
                    <span className="text-red-500 font-bold">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder={t.cityPlaceholder}
                    value={formData.city}
                    onChange={(e) => handleInputChange("city", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>

                {/* District */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {t.districtLabel}
                  </label>
                  <input
                    type="text"
                    placeholder={t.districtPlaceholder}
                    value={formData.district}
                    onChange={(e) => handleInputChange("district", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>

                {/* State & Pincode */}
                <div className="grid grid-cols-2 gap-2">
                  <div className="space-y-1.5">
                    <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                      {t.stateLabel}
                    </label>
                    <input
                      type="text"
                      value={formData.state}
                      onChange={(e) => handleInputChange("state", e.target.value)}
                      className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                      {t.pincodeLabel}
                    </label>
                    <input
                      type="text"
                      maxLength={6}
                      placeholder="380002"
                      value={formData.pincode}
                      onChange={(e) => handleInputChange("pincode", e.target.value.replace(/\D/g, ""))}
                      className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs font-mono"
                    />
                  </div>
                </div>

                {/* Full Address */}
                <div className="sm:col-span-2 space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <span>{t.addressLabel}</span>
                    <span className="text-red-500 font-bold">*</span>
                  </label>
                  <textarea
                    rows={2}
                    required
                    placeholder={t.addressPlaceholder}
                    value={formData.address}
                    onChange={(e) => handleInputChange("address", e.target.value)}
                    className="w-full px-3.5 py-2 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>

                {/* Google Maps Location Link */}
                <div className="sm:col-span-2 space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-indigo-500" />
                    <span>{t.mapLinkLabel}</span>
                  </label>
                  <input
                    type="url"
                    placeholder={t.mapLinkPlaceholder}
                    value={formData.shopMapLink}
                    onChange={(e) => handleInputChange("shopMapLink", e.target.value)}
                    className="w-full px-3.5 py-2 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* STEP 2: OWNER & CONTACT DETAILS                                */}
          {/* ============================================================== */}
          {currentStep === 2 && (
            <div className="space-y-6 animate-in fade-in">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3">
                <h2 className="text-lg sm:text-xl font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <User className="w-5 h-5 text-indigo-600" />
                  <span>{t.step2Heading}</span>
                </h2>
                <p className="text-xs text-muted-foreground">
                  {t.step2Subheading}
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                {/* Proprietor Name */}
                <div className="sm:col-span-2 space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <span>{t.ownerNameLabel}</span>
                    <span className="text-red-500 font-bold">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder={t.ownerNamePlaceholder}
                    value={formData.name}
                    onChange={(e) => handleInputChange("name", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium text-sm"
                  />
                </div>

                {/* Mobile Number for SMS OTP */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <Phone className="w-3.5 h-3.5 text-emerald-600" />
                    <span>{t.mobileLabel}</span>
                    <span className="text-red-500 font-bold">*</span>
                  </label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 font-semibold text-zinc-500 text-xs">
                      +91
                    </span>
                    <input
                      type="tel"
                      required
                      maxLength={10}
                      placeholder="9876543210"
                      value={formData.phone}
                      onChange={(e) => handleInputChange("phone", e.target.value.replace(/\D/g, ""))}
                      className="w-full pl-12 pr-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono font-medium text-sm"
                    />
                  </div>
                  <p className="text-[10px] text-muted-foreground">
                    {t.mobileHelp}
                  </p>
                </div>

                {/* WhatsApp Number */}
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                      <MessageSquare className="w-3.5 h-3.5 text-emerald-500" />
                      <span>{t.whatsappLabel}</span>
                    </label>
                    <label className="flex items-center gap-1 text-[11px] text-indigo-600 dark:text-indigo-400 cursor-pointer font-medium">
                      <input
                        type="checkbox"
                        checked={formData.sameAsMobile}
                        onChange={(e) => handleInputChange("sameAsMobile", e.target.checked)}
                        className="rounded text-indigo-600 focus:ring-0"
                      />
                      <span>{t.sameAsMobileLabel}</span>
                    </label>
                  </div>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 font-semibold text-zinc-500 text-xs">
                      +91
                    </span>
                    <input
                      type="tel"
                      maxLength={10}
                      disabled={formData.sameAsMobile}
                      placeholder="9876543210"
                      value={formData.phone2}
                      onChange={(e) => handleInputChange("phone2", e.target.value.replace(/\D/g, ""))}
                      className="w-full pl-12 pr-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-xs disabled:opacity-60"
                    />
                  </div>
                </div>

                {/* Email Address */}
                <div className="sm:col-span-2 space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <Mail className="w-3.5 h-3.5 text-zinc-400" />
                    <span>{t.emailLabel}</span>
                  </label>
                  <input
                    type="email"
                    placeholder={t.emailPlaceholder}
                    value={formData.email}
                    onChange={(e) => handleInputChange("email", e.target.value)}
                    className="w-full px-3.5 py-2 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>

                {/* Garment Categories Preference */}
                <div className="sm:col-span-2 space-y-2 pt-2 border-t border-zinc-100 dark:border-zinc-800">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {t.garmentsLabel}
                  </label>
                  <p className="text-[11px] text-muted-foreground">
                    {t.garmentsSubheading}
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {GARMENT_CATEGORIES.map((cat) => {
                      const isSelected = selectedGarments.includes(cat)
                      return (
                        <button
                          key={cat}
                          type="button"
                          onClick={() => toggleGarment(cat)}
                          className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-all ${
                            isSelected
                              ? "bg-indigo-600 text-white border-indigo-600 shadow-sm"
                              : "bg-zinc-50 dark:bg-zinc-800/50 border-zinc-200 dark:border-zinc-700 text-zinc-700 dark:text-zinc-300 hover:border-zinc-400"
                          }`}
                        >
                          {isSelected ? `✓ ${cat}` : cat}
                        </button>
                      )
                    })}
                  </div>

                  {/* Custom Garment add */}
                  <div className="flex gap-2 max-w-sm pt-1">
                    <input
                      type="text"
                      placeholder={t.customGarmentPlaceholder}
                      value={customGarment}
                      onChange={(e) => setCustomGarment(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          e.preventDefault()
                          addCustomGarment()
                        }
                      }}
                      className="flex-1 px-3 py-1.5 rounded-lg border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800 text-xs"
                    />
                    <button
                      type="button"
                      onClick={addCustomGarment}
                      className="px-3 py-1.5 rounded-lg bg-zinc-200 dark:bg-zinc-700 text-zinc-800 dark:text-zinc-200 text-xs font-semibold hover:bg-zinc-300"
                    >
                      {t.addBtn}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* STEP 3: TAX & KYC DOCUMENTS                                    */}
          {/* ============================================================== */}
          {currentStep === 3 && (
            <div className="space-y-6 animate-in fade-in">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3">
                <h2 className="text-lg sm:text-xl font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-indigo-600" />
                  <span>{t.step3Heading}</span>
                </h2>
                <p className="text-xs text-muted-foreground">
                  {t.step3Subheading}
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                {/* GSTIN */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {t.gstinLabel}
                  </label>
                  <input
                    type="text"
                    maxLength={15}
                    placeholder={t.gstinPlaceholder}
                    value={formData.gstin}
                    onChange={(e) => handleInputChange("gstin", e.target.value.toUpperCase())}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-xs uppercase"
                  />
                  <p className="text-[10px] text-muted-foreground">
                    {t.gstinHelp}
                  </p>
                </div>

                {/* PAN Number */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {t.panLabel}
                  </label>
                  <input
                    type="text"
                    maxLength={10}
                    placeholder={t.panPlaceholder}
                    value={formData.panNumber}
                    onChange={(e) => handleInputChange("panNumber", e.target.value.toUpperCase())}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-xs uppercase"
                  />
                </div>

                {/* Upload: Shop Photo */}
                <div className="sm:col-span-2 pt-2 border-t border-zinc-100 dark:border-zinc-800">
                  <FileUpload
                    label={t.shopPhotoLabel}
                    folder="kyc/shop"
                    prefix="shop_front"
                    value={formData.shopPhotoUri}
                    onChange={(url) => handleInputChange("shopPhotoUri", url)}
                    description={t.shopPhotoDesc}
                  />
                </div>

                {/* Upload: GST or Visiting Card */}
                <div>
                  <FileUpload
                    label={t.gstCertLabel}
                    folder="kyc/gst"
                    prefix="gst_card"
                    value={formData.gstCertPhotoUri}
                    onChange={(url) => handleInputChange("gstCertPhotoUri", url)}
                  />
                </div>

                {/* Upload: Aadhaar / Owner ID */}
                <div>
                  <FileUpload
                    label={t.aadharLabel}
                    folder="kyc/id"
                    prefix="aadhar"
                    value={formData.aadharPhotoUri}
                    onChange={(url) => handleInputChange("aadharPhotoUri", url)}
                  />
                </div>
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* STEP 4: LOGISTICS & BANK DETAILS                               */}
          {/* ============================================================== */}
          {currentStep === 4 && (
            <div className="space-y-6 animate-in fade-in">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3">
                <h2 className="text-lg sm:text-xl font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <Truck className="w-5 h-5 text-indigo-600" />
                  <span>{t.step4Heading}</span>
                </h2>
                <p className="text-xs text-muted-foreground">
                  {t.step4Subheading}
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                {/* Preferred Transporter */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {t.transporterLabel}
                  </label>
                  <input
                    type="text"
                    placeholder={t.transporterPlaceholder}
                    value={formData.preferredTransporterName}
                    onChange={(e) => handleInputChange("preferredTransporterName", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>

                {/* Booking Station / Delivery Destination */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {t.stationLabel}
                  </label>
                  <input
                    type="text"
                    placeholder={t.stationPlaceholder}
                    value={formData.transportPreference}
                    onChange={(e) => handleInputChange("transportPreference", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>

                {/* Bank Name */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <Landmark className="w-3.5 h-3.5 text-zinc-400" />
                    <span>{t.bankNameLabel}</span>
                  </label>
                  <input
                    type="text"
                    placeholder={t.bankNamePlaceholder}
                    value={formData.bankName}
                    onChange={(e) => handleInputChange("bankName", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>

                {/* Account Number */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {t.accountNumberLabel}
                  </label>
                  <input
                    type="text"
                    placeholder={t.accountNumberPlaceholder}
                    value={formData.accountNumber}
                    onChange={(e) => handleInputChange("accountNumber", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-xs"
                  />
                </div>

                {/* IFSC Code */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {t.ifscLabel}
                  </label>
                  <input
                    type="text"
                    maxLength={11}
                    placeholder={t.ifscPlaceholder}
                    value={formData.ifscCode}
                    onChange={(e) => handleInputChange("ifscCode", e.target.value.toUpperCase())}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-xs uppercase"
                  />
                </div>

                {/* Remarks / Notes */}
                <div className="sm:col-span-2 space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                    {t.notesLabel}
                  </label>
                  <textarea
                    rows={2}
                    placeholder={t.notesPlaceholder}
                    value={formData.notes}
                    onChange={(e) => handleInputChange("notes", e.target.value)}
                    className="w-full px-3.5 py-2 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* STEP 5: SMS OTP VERIFICATION                                   */}
          {/* ============================================================== */}
          {currentStep === 5 && (
            <div className="space-y-6 animate-in fade-in">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3">
                <h2 className="text-lg sm:text-xl font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <Phone className="w-5 h-5 text-indigo-600" />
                  <span>{t.step5Heading}</span>
                </h2>
                <p className="text-xs text-muted-foreground">
                  {t.step5Subheading}
                </p>
              </div>

              {/* Summary Card Before OTP */}
              <div className="p-4 rounded-2xl bg-zinc-50 dark:bg-zinc-800/60 border border-zinc-200 dark:border-zinc-700/60 space-y-2 text-xs">
                <p className="font-bold text-zinc-800 dark:text-zinc-200">
                  {t.summaryTitle}
                </p>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 text-[11px]">
                  <div>
                    <span className="text-muted-foreground block">{t.summaryFirm}</span>
                    <span className="font-semibold text-zinc-900 dark:text-zinc-100">{formData.firmName}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground block">{t.summaryOwner}</span>
                    <span className="font-semibold text-zinc-900 dark:text-zinc-100">{formData.name}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground block">{t.summaryCityState}</span>
                    <span className="font-semibold text-zinc-900 dark:text-zinc-100">{formData.city}, {formData.state}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground block">{t.summaryMobile}</span>
                    <span className="font-semibold font-mono text-indigo-600 dark:text-indigo-400">+91 {formData.phone.slice(-10)}</span>
                  </div>
                  {formData.gstin && (
                    <div>
                      <span className="text-muted-foreground block">{t.summaryGst}</span>
                      <span className="font-semibold font-mono text-zinc-900 dark:text-zinc-100">{formData.gstin}</span>
                    </div>
                  )}
                  {formData.preferredTransporterName && (
                    <div>
                      <span className="text-muted-foreground block">{t.summaryTransport}</span>
                      <span className="font-semibold text-zinc-900 dark:text-zinc-100">{formData.preferredTransporterName}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* OTP Verification Box */}
              <div className="max-w-md mx-auto p-6 rounded-2xl bg-indigo-50/40 dark:bg-indigo-950/20 border border-indigo-100 dark:border-indigo-900/50 text-center space-y-4">
                <div className="w-12 h-12 rounded-full bg-indigo-100 dark:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400 mx-auto flex items-center justify-center">
                  <Phone className="w-6 h-6" />
                </div>

                <div className="space-y-1">
                  <h3 className="font-bold text-sm text-zinc-900 dark:text-zinc-100">
                    {t.otpBoxHeading}
                  </h3>
                  <p className="text-xs text-muted-foreground">
                    {t.otpBoxSub} <strong className="text-zinc-900 dark:text-zinc-100 font-mono">+91 {formData.phone.slice(-10)}</strong>
                  </p>
                </div>

                {!otpSent ? (
                  <button
                    type="button"
                    disabled={isSendingOtp}
                    onClick={handleSendOtp}
                    className="w-full inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm transition-all shadow disabled:opacity-60"
                  >
                    {isSendingOtp ? (
                      <>
                        <RefreshCw className="w-4 h-4 animate-spin" />
                        <span>{t.sendingOtpBtn}</span>
                      </>
                    ) : (
                      <>
                        <Send className="w-4 h-4" />
                        <span>{t.sendOtpBtn}</span>
                      </>
                    )}
                  </button>
                ) : (
                  <div className="space-y-4">
                    <div className="space-y-1.5">
                      <label className="text-xs font-semibold text-zinc-800 dark:text-zinc-200 block">
                        {t.enterOtpLabel}
                      </label>
                      <input
                        type="text"
                        maxLength={6}
                        autoFocus
                        placeholder="• • • • • •"
                        value={otpCode}
                        onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ""))}
                        className="w-48 mx-auto text-center px-4 py-2.5 rounded-xl border-2 border-indigo-400 dark:border-indigo-600 bg-white dark:bg-zinc-900 text-xl font-mono tracking-widest font-bold focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      />
                    </div>

                    <div className="flex items-center justify-between text-xs">
                      <button
                        type="button"
                        disabled={resendTimer > 0 || isSendingOtp}
                        onClick={handleSendOtp}
                        className="text-indigo-600 dark:text-indigo-400 font-medium hover:underline disabled:opacity-50 disabled:no-underline flex items-center gap-1"
                      >
                        <RefreshCw className="w-3 h-3" />
                        <span>{t.resendOtpBtn} {resendTimer > 0 ? `(${resendTimer}s)` : ""}</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => setCurrentStep(2)}
                        className="text-zinc-500 hover:underline"
                      >
                        {t.changeNumberBtn}
                      </button>
                    </div>

                    <button
                      type="button"
                      disabled={isVerifyingOtp || otpCode.length < 6}
                      onClick={handleVerifyAndSubmit}
                      className="w-full inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm transition-all shadow-md hover:shadow-lg disabled:opacity-60"
                    >
                      {isVerifyingOtp ? (
                        <>
                          <RefreshCw className="w-4 h-4 animate-spin" />
                          <span>{t.verifyingBtn}</span>
                        </>
                      ) : (
                        <>
                          <CheckCircle2 className="w-4 h-4" />
                          <span>{t.verifySubmitBtn}</span>
                        </>
                      )}
                    </button>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Navigation Buttons (Prev / Next) */}
          <div className="pt-4 border-t border-zinc-100 dark:border-zinc-800 flex items-center justify-between">
            {currentStep > 1 ? (
              <button
                type="button"
                onClick={handlePrevStep}
                className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl border border-zinc-200 dark:border-zinc-700 text-zinc-700 dark:text-zinc-300 font-medium text-xs hover:bg-zinc-50 dark:hover:bg-zinc-800 transition-colors"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>{t.backBtn}</span>
              </button>
            ) : (
              <div></div>
            )}

            {currentStep < 5 && (
              <button
                type="button"
                onClick={handleNextStep}
                className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs transition-colors shadow-sm"
              >
                <span>{t.nextBtn}</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>
      </main>

      {/* Public Footer */}
      <footer className="mt-auto py-6 border-t border-zinc-200 dark:border-zinc-800 text-center text-xs text-muted-foreground">
        <p>© {new Date().getFullYear()} {t.footerText}</p>
      </footer>
    </div>
  )
}
