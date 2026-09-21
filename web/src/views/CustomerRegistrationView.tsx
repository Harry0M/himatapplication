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
import { HIMAT_LOGO_DATA_URI } from "../lib/logoBase64"
import {
  fetchGstDetails,
  isValidGstin,
  extractPanFromGstin,
  getStateFromGstin,
} from "../lib/gstHelper"

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

      // Attempt live fetch for firm name, address, city, pincode
      const details = await fetchGstDetails(cleanGst)

      if (details && (details.firmName || details.address || details.city || details.pincode)) {
        setFormData((prev) => ({
          ...prev,
          gstin: cleanGst,
          panNumber: details.pan || offlinePan || prev.panNumber,
          state: details.state || offlineState || prev.state,
          firmName: details.firmName || prev.firmName,
          address: details.address || prev.address,
          shopAddress: details.address || prev.shopAddress || prev.address,
          city: details.city || prev.city,
          pincode: details.pincode || prev.pincode,
        }))
        setGstFeedback({
          type: "success",
          message: t.gstAutoSuccess,
        })
      } else {
        // Offline state & PAN detected, manual entry for rest
        setGstFeedback({
          type: "offline",
          message: t.gstAutoOffline,
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
        message: t.gstAutoOffline,
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
  const validateCurrentStep = (targetStep?: number): boolean => {
    setErrorMessage(null)
    // Always enforce all 5 mandatory fields if currently on Step 1 or trying to navigate past Step 1
    if (currentStep === 1 || (targetStep && targetStep > 1)) {
      if (!formData.firmName.trim()) {
        setErrorMessage(t.errFirmName)
        return false
      }
      if (!formData.name.trim()) {
        setErrorMessage(t.errOwnerName)
        return false
      }
      const cleanPhone = formData.phone.replace(/\D/g, "")
      if (cleanPhone.length !== 10) {
        setErrorMessage(t.errPhone)
        return false
      }
      if (!formData.city.trim()) {
        setErrorMessage(t.errCity)
        return false
      }
      if (!formData.address.trim()) {
        setErrorMessage(t.errAddress)
        return false
      }
    }
    return true
  }

  const handleNextStep = () => {
    if (validateCurrentStep(currentStep + 1)) {
      setCurrentStep((prev) => Math.min(prev + 1, t.stepIndicators.length))
      window.scrollTo({ top: 0, behavior: "smooth" })
    }
  }

  const handlePrevStep = () => {
    setErrorMessage(null)
    setCurrentStep((prev) => Math.max(prev - 1, 1))
    window.scrollTo({ top: 0, behavior: "smooth" })
  }

  // Cleanup recaptcha on unmount
  useEffect(() => {
    return () => {
      if (recaptchaVerifierRef.current) {
        try {
          recaptchaVerifierRef.current.clear()
        } catch (e) {
          // ignore
        }
        recaptchaVerifierRef.current = null
      }
    }
  }, [])

  // Initialize or Reset Recaptcha Verifier
  const getRecaptchaVerifier = () => {
    if (recaptchaVerifierRef.current) {
      try {
        recaptchaVerifierRef.current.clear()
      } catch (e) {
        // ignore
      }
      recaptchaVerifierRef.current = null
    }

    // Completely replace the container DOM element so that no leftover grecaptcha widget ID or data attributes remain
    const wrapper = recaptchaWrapperRef.current || document.getElementById("recaptcha-wrapper")
    if (wrapper) {
      wrapper.innerHTML = '<div id="recaptcha-container"></div>'
    } else {
      const oldContainer = document.getElementById("recaptcha-container")
      if (oldContainer && oldContainer.parentNode) {
        const newContainer = document.createElement("div")
        newContainer.id = "recaptcha-container"
        oldContainer.parentNode.replaceChild(newContainer, oldContainer)
      }
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
      // On failure, clean up recaptcha verifier and DOM so the user can immediately retry
      if (recaptchaVerifierRef.current) {
        try {
          recaptchaVerifierRef.current.clear()
        } catch (e) {
          // ignore
        }
        recaptchaVerifierRef.current = null
      }
      const wrapper = recaptchaWrapperRef.current || document.getElementById("recaptcha-wrapper")
      if (wrapper) {
        wrapper.innerHTML = '<div id="recaptcha-container"></div>'
      } else {
        const oldContainer = document.getElementById("recaptcha-container")
        if (oldContainer && oldContainer.parentNode) {
          const newContainer = document.createElement("div")
          newContainer.id = "recaptcha-container"
          oldContainer.parentNode.replaceChild(newContainer, oldContainer)
        }
      }

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

      // 2. Determine Primary Key and Request ID
      // GSTIN is the primary key if provided; if not, 10-digit clean phone number is used.
      const cleanGstin = formData.gstin.trim().toUpperCase()
      const cleanPhone = formData.phone.replace(/\D/g, "").slice(-10)
      const primaryKey = cleanGstin || cleanPhone
      const keyType: "GSTIN" | "PHONE" = cleanGstin ? "GSTIN" : "PHONE"
      const requestId = cleanGstin ? `req_gst_${cleanGstin}` : `req_phone_${cleanPhone}`
      const reqRef = ref(rtdb, `customer_registration_requests/${requestId}`)

      const payload: CustomerRegistrationRequest = {
        id: requestId,
        primaryKey,
        keyType,
        firmName: formData.firmName.trim(),
        name: formData.name.trim(),
        phone: `+91${cleanPhone}`,
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
        gstin: cleanGstin,
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
    const cleanGst = formData.gstin.trim().toUpperCase()
    const cleanPhone = formData.phone.replace(/\D/g, "").slice(-10)
    const primaryKeyDisplay = cleanGst ? `${cleanGst} (GSTIN)` : `+91 ${cleanPhone} (Mobile)`
    const whatsappMsg = encodeURIComponent(
      lang === "en"
        ? `Hello Himat Textile!\nI have submitted my new commercial account registration form online.\n\n📌 Firm Name: ${formData.firmName}\n👤 Proprietor: ${formData.name}\n🔑 Primary Key: ${primaryKeyDisplay}\n📞 Mobile: +91 ${cleanPhone}\n📍 City: ${formData.city}\n🆔 Ref ID: ${submittedRequestId}\n\nPlease review our application and grant commercial account approval. Thank you!`
        : `नमस्ते हिम्मत टेक्सटाइल (Himat Textile)!\nमैंने नया व्यापारिक खाता खोलने के लिए ऑनलाइन पंजीकरण फॉर्म सबमिट किया है।\n\n📌 फर्म का नाम: ${formData.firmName}\n👤 संचालक: ${formData.name}\n🔑 मुख्य पहचान (Key): ${primaryKeyDisplay}\n📞 मोबाइल: +91 ${cleanPhone}\n📍 शहर: ${formData.city}\n🆔 संदर्भ क्रमांक (Ref ID): ${submittedRequestId}\n\nकृपया हमारे खाते की समीक्षा कर अनुमोदन (Approval) प्रदान करें। धन्यवाद!`
    )

    return (
      <div className="min-h-screen bg-gradient-to-b from-zinc-50 to-zinc-100 dark:from-zinc-950 dark:to-zinc-900 py-10 px-4 sm:px-6 flex items-center justify-center">
        <div className="max-w-xl w-full bg-white dark:bg-zinc-900 rounded-3xl shadow-xl border border-zinc-200 dark:border-zinc-800 p-6 sm:p-10 text-center space-y-6">
          {/* Header Brand & Language Switcher on Success Screen */}
          <div className="flex items-center justify-between border-b border-zinc-100 dark:border-zinc-800 pb-3">
            <div className="flex items-center gap-2.5">
              <div className="h-9 w-9 rounded-xl bg-white dark:bg-zinc-800 p-1 border border-zinc-200 dark:border-zinc-700 shadow-2xs flex items-center justify-center overflow-hidden shrink-0">
                <img src={HIMAT_LOGO_DATA_URI} alt="Himat Textile" className="h-full w-full object-contain" />
              </div>
              <div className="text-left">
                <p className="font-bold text-xs text-zinc-900 dark:text-zinc-100 leading-none">{t.brandName}</p>
                <p className="text-[10px] text-muted-foreground mt-0.5">{t.brandTag}</p>
              </div>
            </div>
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
              <span className="text-muted-foreground">{t.primaryKeyLabel}</span>
              <span className="font-mono font-bold text-indigo-600 dark:text-indigo-400">
                {formData.gstin ? `${formData.gstin} (GSTIN)` : `+91 ${formData.phone.slice(-10)} (Mobile)`}
              </span>
            </div>
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
      {/* Hidden Recaptcha Wrapper & Target Container */}
      <div ref={recaptchaWrapperRef} id="recaptcha-wrapper">
        <div id="recaptcha-container"></div>
      </div>

      {/* Top Header */}
      <header className="sticky top-0 z-30 bg-white/95 dark:bg-zinc-900/95 backdrop-blur border-b border-zinc-200 dark:border-zinc-800 px-4 py-3 sm:px-6">
        <div className="max-w-4xl mx-auto flex items-center justify-between gap-2">
          <div className="flex items-center gap-3">
            <div className="h-11 w-11 sm:h-12 sm:w-12 rounded-xl bg-white dark:bg-zinc-800 p-1 border border-zinc-200 dark:border-zinc-700 shadow-2xs flex items-center justify-center overflow-hidden shrink-0">
              <img
                src={HIMAT_LOGO_DATA_URI}
                alt="Himat Textile"
                className="h-full w-full object-contain"
              />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-base font-bold tracking-tight text-zinc-900 dark:text-zinc-100">
                  {t.brandName}
                </h1>
                <span className="hidden sm:inline-block text-[11px] px-2.5 py-0.5 rounded-full bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 font-medium border border-indigo-100 dark:border-indigo-900">
                  {t.brandTag}
                </span>
              </div>
              <p className="text-[11px] sm:text-xs text-muted-foreground truncate max-w-[210px] sm:max-w-md">
                <span className="sm:hidden font-medium text-indigo-600 dark:text-indigo-400">{t.brandTag} • </span>
                <span>{t.pageTitle}</span>
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
          {/* Mobile Stepper: Connected Circles + Step Banner (No overlapping icons or wrapped text) */}
          <div className="block sm:hidden space-y-3">
            {/* Step Circles & Connecting Lines */}
            <div className="flex items-center justify-between px-1">
              {t.stepIndicators.map((step, idx) => {
                const isCompleted = currentStep > step.id
                const isCurrent = currentStep === step.id
                const isLast = idx === t.stepIndicators.length - 1

                return (
                  <div key={step.id} className={`flex items-center ${isLast ? "" : "flex-1"}`}>
                    <button
                      type="button"
                      onClick={() => {
                        if (step.id < currentStep || validateCurrentStep(step.id)) {
                          setCurrentStep(step.id)
                        }
                      }}
                      className="relative z-10 flex flex-col items-center focus:outline-none"
                    >
                      <div
                        className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                          isCompleted
                            ? "bg-emerald-500 text-white shadow-xs"
                            : isCurrent
                            ? "bg-indigo-600 text-white shadow-sm ring-4 ring-indigo-100 dark:ring-indigo-950"
                            : "bg-zinc-100 dark:bg-zinc-800 text-zinc-400 dark:text-zinc-500"
                        }`}
                      >
                        {isCompleted ? <Check className="w-3.5 h-3.5 stroke-[2.5]" /> : step.id}
                      </div>
                    </button>

                    {/* Connecting Line between steps */}
                    {!isLast && (
                      <div className="flex-1 mx-1.5 h-0.5 bg-zinc-200 dark:bg-zinc-800 relative">
                        <div
                          className={`h-full transition-all duration-300 ${
                            isCompleted ? "bg-emerald-500" : "bg-transparent"
                          }`}
                        />
                      </div>
                    )}
                  </div>
                )
              })}
            </div>

            {/* Current Step Active Label Display */}
            <div className="pt-2 border-t border-zinc-100 dark:border-zinc-800 flex items-center justify-between gap-2">
              <div className="flex items-center gap-2 min-w-0">
                <span className="shrink-0 text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-100 dark:border-indigo-900">
                  {lang === "hi" ? `चरण ${currentStep} / 5` : `Step ${currentStep} of 5`}
                </span>
                <span className="text-xs font-bold text-zinc-900 dark:text-zinc-100 truncate">
                  {t.stepIndicators[currentStep - 1]?.title}
                </span>
              </div>
              <span className="shrink-0 text-[10px] text-muted-foreground truncate">
                {t.stepIndicators[currentStep - 1]?.subtitle}
              </span>
            </div>
          </div>

          {/* Desktop Stepper: 5-Column Grid with Icon, Title, and Subtitle */}
          <div className="hidden sm:grid sm:grid-cols-5 gap-2">
            {t.stepIndicators.map((step) => {
              const Icon = step.icon
              const isCompleted = currentStep > step.id
              const isCurrent = currentStep === step.id

              return (
                <button
                  key={step.id}
                  type="button"
                  onClick={() => {
                    if (step.id < currentStep || validateCurrentStep(step.id)) {
                      setCurrentStep(step.id)
                    }
                  }}
                  className={`flex flex-row items-center gap-2 p-2.5 rounded-xl text-left transition-all ${
                    isCurrent
                      ? "bg-indigo-50 dark:bg-indigo-950/50 text-indigo-700 dark:text-indigo-300 ring-1 ring-indigo-500/30"
                      : isCompleted
                      ? "text-emerald-600 dark:text-emerald-400 hover:bg-zinc-50 dark:hover:bg-zinc-800/50"
                      : "text-zinc-400 dark:text-zinc-600 hover:bg-zinc-50 dark:hover:bg-zinc-800/30"
                  }`}
                >
                  <div
                    className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold shrink-0 transition-all ${
                      isCurrent
                        ? "bg-indigo-600 text-white shadow-sm"
                        : isCompleted
                        ? "bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300"
                        : "bg-zinc-100 dark:bg-zinc-800 text-zinc-500"
                    }`}
                  >
                    {isCompleted ? <Check className="w-4 h-4" /> : <Icon className="w-4 h-4" />}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-bold truncate leading-tight">
                      {step.title}
                    </p>
                    <p className="text-[10px] text-muted-foreground truncate">
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
          {/* STEP 1: MANDATORY DETAILS (*)                                  */}
          {/* ============================================================== */}
          {currentStep === 1 && (
            <div className="space-y-6 animate-in fade-in">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3">
                <h2 className="text-lg sm:text-xl font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <Building2 className="w-5 h-5 text-indigo-600" />
                  <span>{t.step1Heading}</span>
                </h2>
                <p className="text-xs text-muted-foreground mt-0.5">
                  {t.step1Subheading}
                </p>
              </div>

              {/* Informational Banner */}
              <div className="p-3.5 rounded-2xl bg-indigo-50/80 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-900/50 flex items-start gap-2.5 text-xs text-indigo-900 dark:text-indigo-200">
                <Sparkles className="w-4 h-4 flex-shrink-0 mt-0.5 text-indigo-600 dark:text-indigo-400" />
                <span className="font-medium leading-relaxed">{t.step1MandatoryBadge}</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                {/* 0. GSTIN Number - Primary Key (Optional) at the very beginning of the form */}
                <div className="sm:col-span-2 p-4 rounded-2xl bg-gradient-to-r from-indigo-50/70 via-blue-50/40 to-indigo-50/70 dark:from-indigo-950/40 dark:via-zinc-800/50 dark:to-indigo-950/40 border border-indigo-200/80 dark:border-indigo-800/80 space-y-2.5">
                  <div className="flex items-center justify-between flex-wrap gap-2">
                    <label className="font-bold text-xs text-indigo-950 dark:text-indigo-200 flex items-center gap-1.5">
                      <ShieldCheck className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
                      <span>{t.gstinStep1Label}</span>
                    </label>
                    <div className="flex items-center gap-1.5">
                      <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-indigo-100 dark:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800">
                        {t.gstOptionalBadge}
                      </span>
                      {formData.gstin && (
                        <button
                          type="button"
                          onClick={handleClearGst}
                          className="text-[11px] text-zinc-500 hover:text-red-600 dark:hover:text-red-400 underline ml-1"
                        >
                          {t.gstClearBtn}
                        </button>
                      )}
                    </div>
                  </div>

                  <div className="flex flex-col sm:flex-row gap-2">
                    <div className="relative flex-1">
                      <input
                        type="text"
                        maxLength={15}
                        placeholder={t.gstinStep1Placeholder}
                        value={formData.gstin}
                        onChange={(e) => {
                          const val = e.target.value.toUpperCase().replace(/[^0-9A-Z]/g, "")
                          handleInputChange("gstin", val)
                          if (val.length === 15 && isValidGstin(val)) {
                            handleGstLookup(val)
                          } else if (val.length === 0) {
                            setGstFeedback({ type: null, message: "" })
                          }
                        }}
                        className="w-full px-3.5 py-2.5 rounded-xl border border-indigo-300 dark:border-indigo-700/80 bg-white dark:bg-zinc-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-sm tracking-wider uppercase"
                      />
                      {formData.gstin.length === 15 && isValidGstin(formData.gstin) && (
                        <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none">
                          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                        </div>
                      )}
                    </div>

                    <button
                      type="button"
                      disabled={isFetchingGst || !formData.gstin.trim()}
                      onClick={() => handleGstLookup()}
                      className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs transition-all shadow-xs disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-1.5 shrink-0"
                    >
                      {isFetchingGst ? (
                        <>
                          <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                          <span>{t.fetchingGst}</span>
                        </>
                      ) : (
                        <>
                          <Sparkles className="w-3.5 h-3.5" />
                          <span>{t.fetchGstBtn}</span>
                        </>
                      )}
                    </button>
                  </div>

                  {/* Feedback or Helper Message */}
                  {gstFeedback.message ? (
                    <div
                      className={`text-[11px] p-2 rounded-xl flex items-start gap-1.5 font-medium ${
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
                      {t.gstinStep1Help}
                    </p>
                  )}
                </div>

                {/* 1. Firm Name (Required *) */}
                <div className="sm:col-span-2 space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <Building2 className="w-3.5 h-3.5 text-zinc-500" />
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

                {/* 2. Proprietor / Owner Name (Required *) */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <User className="w-3.5 h-3.5 text-zinc-500" />
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

                {/* 3. Primary Mobile Number for SMS OTP (Required *) */}
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

                {/* 4. City (Required *) */}
                <div className="sm:col-span-2 space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <Building2 className="w-3.5 h-3.5 text-zinc-500" />
                    <span>{t.cityLabel}</span>
                    <span className="text-red-500 font-bold">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder={t.cityPlaceholder}
                    value={formData.city}
                    onChange={(e) => handleInputChange("city", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium text-sm"
                  />
                </div>

                {/* 5. Complete Shop Address (Required *) */}
                <div className="sm:col-span-2 space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-zinc-500" />
                    <span>{t.addressLabel}</span>
                    <span className="text-red-500 font-bold">*</span>
                  </label>
                  <textarea
                    rows={3}
                    required
                    placeholder={t.addressPlaceholder}
                    value={formData.address}
                    onChange={(e) => handleInputChange("address", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>
              </div>
            </div>
          )}

          {/* ============================================================== */}
          {/* STEP 2: ADDITIONAL PROFILE & CONTACT (OPTIONAL)                 */}
          {/* ============================================================== */}
          {currentStep === 2 && (
            <div className="space-y-6 animate-in fade-in">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3">
                <h2 className="text-lg sm:text-xl font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <User className="w-5 h-5 text-indigo-600" />
                  <span>{t.step2Heading}</span>
                </h2>
                <p className="text-xs text-muted-foreground mt-0.5">
                  {t.step2Subheading}
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                {/* WhatsApp Number (Optional) */}
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

                {/* Email Address (Optional) */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <Mail className="w-3.5 h-3.5 text-zinc-400" />
                    <span>{t.emailLabel}</span>
                  </label>
                  <input
                    type="email"
                    placeholder={t.emailPlaceholder}
                    value={formData.email}
                    onChange={(e) => handleInputChange("email", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>

                {/* Market Area (Optional) */}
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

                {/* District (Optional) */}
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

                {/* State & Pincode (Optional) */}
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

                {/* Google Maps Location Link (Optional) */}
                <div className="space-y-1.5">
                  <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-indigo-500" />
                    <span>{t.mapLinkLabel}</span>
                  </label>
                  <input
                    type="url"
                    placeholder={t.mapLinkPlaceholder}
                    value={formData.shopMapLink}
                    onChange={(e) => handleInputChange("shopMapLink", e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-800/60 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-xs"
                  />
                </div>

                {/* Garment Categories Preference (Optional) */}
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
                  <div className="flex items-center justify-between">
                    <label className="font-semibold text-zinc-800 dark:text-zinc-200">
                      {t.gstinLabel}
                    </label>
                    {formData.gstin && (
                      <span className="text-[10px] text-indigo-600 dark:text-indigo-400 font-medium">
                        ✓ Primary Key
                      </span>
                    )}
                  </div>
                  <input
                    type="text"
                    maxLength={15}
                    placeholder={t.gstinPlaceholder}
                    value={formData.gstin}
                    onChange={(e) => {
                      const val = e.target.value.toUpperCase().replace(/[^0-9A-Z]/g, "")
                      handleInputChange("gstin", val)
                      if (val.length >= 12 && !formData.panNumber) {
                        const pan = extractPanFromGstin(val)
                        if (pan) handleInputChange("panNumber", pan)
                      }
                    }}
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
                    onChange={(e) => handleInputChange("panNumber", e.target.value.toUpperCase().replace(/[^0-9A-Z]/g, ""))}
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
                    <span className="text-muted-foreground block">{t.summaryKey}</span>
                    <span className="font-semibold font-mono text-indigo-600 dark:text-indigo-400">
                      {formData.gstin ? `${formData.gstin} (GSTIN)` : `+91 ${formData.phone.slice(-10)} (Mobile)`}
                    </span>
                  </div>
                  <div>
                    <span className="text-muted-foreground block">{t.summaryCityState}</span>
                    <span className="font-semibold text-zinc-900 dark:text-zinc-100">{formData.city}, {formData.state}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground block">{t.summaryMobile}</span>
                    <span className="font-semibold font-mono text-zinc-900 dark:text-zinc-100">+91 {formData.phone.slice(-10)}</span>
                  </div>
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
              <div className="flex items-center gap-2">
                {currentStep >= 2 && currentStep <= 4 && (
                  <button
                    type="button"
                    onClick={() => {
                      if (validateCurrentStep(5)) {
                        setCurrentStep(5)
                      }
                    }}
                    className="hidden sm:inline-flex items-center gap-1 px-3.5 py-2.5 rounded-xl border border-indigo-200 dark:border-indigo-800/60 bg-indigo-50/70 dark:bg-indigo-950/30 text-indigo-700 dark:text-indigo-300 font-semibold text-xs hover:bg-indigo-100 dark:hover:bg-indigo-900/50 transition-colors"
                  >
                    <span>{t.skipToVerificationBtn}</span>
                  </button>
                )}
                <button
                  type="button"
                  onClick={handleNextStep}
                  className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs transition-colors shadow-sm"
                >
                  <span>{t.nextBtn}</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
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
