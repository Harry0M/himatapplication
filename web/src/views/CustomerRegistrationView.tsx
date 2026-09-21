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
  Globe,
  Store,
  Tag
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
      <div className="min-h-screen bg-gradient-to-b from-zinc-50 to-zinc-200 dark:from-zinc-950 dark:to-zinc-900 flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white dark:bg-zinc-900 rounded-3xl shadow-xl border border-zinc-200 dark:border-zinc-800 p-6 sm:p-8 text-center">
          <div className="w-16 h-16 bg-emerald-100 dark:bg-emerald-950/60 rounded-full flex items-center justify-center mx-auto mb-4 text-emerald-600 dark:text-emerald-400">
            <CheckCircle2 className="w-10 h-10" />
          </div>

          <h2 className="text-xl font-bold text-zinc-900 dark:text-zinc-100">
            {lang === "hi" ? "पंजीकरण अनुरोध सबमिट हुआ!" : "Registration Submitted!"}
          </h2>
          <p className="text-xs text-zinc-500 mt-1">
            {lang === "hi"
              ? "आपका ग्राहक खाता अनुरोध प्राप्त हो गया है। व्यवस्थापक अनुमोदन के बाद सक्रिय किया जाएगा।"
              : "Your customer account request has been received. Our team will verify and activate your commercial account."}
          </p>

          <div className="mt-6 p-4 rounded-2xl bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-200 dark:border-zinc-700 text-left space-y-2">
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Request ID:</span>
              <span className="font-mono font-bold text-zinc-800 dark:text-zinc-200">{submittedRequestId}</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Shop / Firm:</span>
              <span className="font-semibold text-zinc-900 dark:text-zinc-100">{formData.firmName}</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">Contact Person:</span>
              <span className="font-medium text-zinc-800 dark:text-zinc-200">{formData.name} ({formData.phone})</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-zinc-500">City / State:</span>
              <span className="font-semibold text-emerald-600 dark:text-emerald-400">{formData.city}, {formData.state}</span>
            </div>
          </div>

          <div className="mt-8 flex flex-col sm:flex-row gap-3 justify-center">
            <a
              href={`https://api.whatsapp.com/send?phone=919427028169&text=${whatsappMsg}`}
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

  // WIZARD VIEW - Identical clean, simple style as Supplier Onboarding
  return (
    <div className="min-h-screen bg-gradient-to-b from-zinc-50 via-zinc-100 to-zinc-200 dark:from-zinc-950 dark:via-zinc-900 dark:to-zinc-950 py-8 px-4 sm:px-6">
      {/* Hidden Recaptcha Wrapper & Target Container */}
      <div ref={recaptchaWrapperRef} id="recaptcha-wrapper">
        <div id="recaptcha-container"></div>
      </div>

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
            <Store className="w-3.5 h-3.5" />
            <span>Wholesale Customer & Buyer Onboarding</span>
          </div>

          <p className="text-xs text-zinc-600 dark:text-zinc-400 max-w-md mx-auto">
            {lang === "hi"
              ? "सीधे थोक में कपड़े और गारमेंट्स खरीदने के लिए अपनी दुकान या फर्म को पंजीकृत करें।"
              : "Register your garment retail shop, wholesale agency, or textile trading business to order directly."}
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
          </div>
        </div>

        {/* Wizard Steps Indicator */}
        <div className="mb-6 bg-white dark:bg-zinc-900 rounded-xl p-3 border border-zinc-200 dark:border-zinc-800 shadow-sm">
          <div className="flex items-center justify-between">
            {[
              { num: 1, label: "Profile" },
              { num: 2, label: "Owner" },
              { num: 3, label: "KYC" },
              { num: 4, label: "Transport" },
              { num: 5, label: "Verify" },
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

        {/* Global Error Banner */}
        {errorMessage && (
          <div className="mb-6 p-3 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800/60 flex items-start gap-2.5 text-xs text-red-700 dark:text-red-300">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Step Card Container */}
        <div className="bg-white dark:bg-zinc-900 rounded-2xl shadow-lg border border-zinc-200 dark:border-zinc-800 p-6 sm:p-8">
          {/* STEP 1: BUSINESS PROFILE & MANDATORY DETAILS */}
          {currentStep === 1 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <Building2 className="w-4 h-4 text-emerald-600" />
                  <span>Step 1: Business Profile</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  {lang === "hi"
                    ? "अपनी फर्म का नाम, संचालक और प्राथमिक संपर्क विवरण दर्ज करें।"
                    : "Enter your firm name, proprietor, and primary contact details."}
                </p>
              </div>

              {/* GSTIN First with Auto-Fetch Option */}
              <div className="p-4 rounded-2xl bg-gradient-to-r from-emerald-50/70 via-teal-50/40 to-emerald-50/70 dark:from-emerald-950/40 dark:via-zinc-800/50 dark:to-emerald-950/40 border border-emerald-200/80 dark:border-emerald-800/80 space-y-2.5">
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <label className="font-bold text-xs text-emerald-950 dark:text-emerald-200 flex items-center gap-1.5">
                    <ShieldCheck className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
                    <span>
                      {lang === "hi"
                        ? "जीएसटी नंबर (GSTIN - वैकल्पिक / स्वतः विवरण भरें)"
                        : "GSTIN Number (Optional - Auto-Fetch Details)"}
                    </span>
                  </label>
                  <div className="flex items-center gap-1.5">
                    <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-900/60 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
                      {lang === "hi" ? "वैकल्पिक • मुख्य पहचान" : "Optional • Primary Key"}
                    </span>
                    {formData.gstin && (
                      <button
                        type="button"
                        onClick={handleClearGst}
                        className="text-[11px] text-zinc-500 hover:text-red-600 dark:hover:text-red-400 underline ml-1"
                      >
                        {lang === "hi" ? "हटाएं" : "Clear"}
                      </button>
                    )}
                  </div>
                </div>

                <div className="flex flex-col sm:flex-row gap-2">
                  <div className="relative flex-1">
                    <input
                      type="text"
                      maxLength={15}
                      placeholder="24AAAAA0000A1Z5 (15 Characters)"
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
                      className="w-full h-10 px-3.5 rounded-xl border border-emerald-300 dark:border-emerald-700/80 bg-white dark:bg-zinc-800 focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono text-sm tracking-wider uppercase text-zinc-900 dark:text-zinc-100"
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
                    className="h-10 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs transition-all shadow-xs disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-1.5 shrink-0"
                  >
                    {isFetchingGst ? (
                      <>
                        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                        <span>{lang === "hi" ? "डेटा लोड हो रहा है..." : "Fetching..."}</span>
                      </>
                    ) : (
                      <>
                        <Sparkles className="w-3.5 h-3.5" />
                        <span>{lang === "hi" ? "विवरण प्राप्त करें" : "Fetch Details"}</span>
                      </>
                    )}
                  </button>
                </div>

                {gstFeedback.message ? (
                  <div
                    className={`text-[11px] p-2 rounded-xl flex items-start gap-1.5 font-medium ${
                      gstFeedback.type === "success"
                        ? "bg-emerald-100/70 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-200 border border-emerald-300 dark:border-emerald-800"
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
                  <p className="text-[11px] text-emerald-900/70 dark:text-emerald-300/70">
                    {lang === "hi"
                      ? "जीएसटी नंबर दर्ज करने से राज्य, पैन नंबर, फर्म का नाम और पता स्वतः भर जाएगा। यदि जीएसटी नहीं है तो खाली छोड़ें।"
                      : "Enter 15-digit GSTIN to auto-fetch firm name, address, state & PAN. If unregistered, leave blank."}
                  </p>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Firm / Shop Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.firmName}
                  onChange={(e) => handleInputChange("firmName", e.target.value)}
                  placeholder="e.g. Shree Ram Garments"
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Proprietor / Owner Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => handleInputChange("name", e.target.value)}
                  placeholder="e.g. Mukeshbhai Shah"
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

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
                    onChange={(e) => handleInputChange("phone", e.target.value.replace(/\D/g, ""))}
                    placeholder="9825012345"
                    className="w-full h-10 pl-11 pr-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  City <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.city}
                  onChange={(e) => handleInputChange("city", e.target.value)}
                  placeholder="e.g. Ahmedabad, Surat, Rajkot, Indore"
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Complete Shop / Office Address <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={2}
                  value={formData.address}
                  onChange={(e) => handleInputChange("address", e.target.value)}
                  placeholder="Shop number, market name, road, landmark..."
                  className="w-full p-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>
            </div>
          )}

          {/* STEP 2: ADDITIONAL PROFILE & CONTACT */}
          {currentStep === 2 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <User className="w-4 h-4 text-emerald-600" />
                  <span>Step 2: Additional Profile & Preferences</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  Provide secondary contact, market area, and garment sourcing preferences.
                </p>
              </div>

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
                      onChange={(e) => handleInputChange("phone2", e.target.value.replace(/\D/g, ""))}
                      placeholder="WhatsApp phone number"
                      className="w-full h-10 pl-11 pr-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Email Address (Optional)
                  </label>
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) => handleInputChange("email", e.target.value)}
                    placeholder="e.g. contact@shreeram.com"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Market Area (Optional)
                  </label>
                  <input
                    type="text"
                    value={formData.marketArea}
                    onChange={(e) => handleInputChange("marketArea", e.target.value)}
                    placeholder="e.g. Relief Road, Rituraj Market"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    District
                  </label>
                  <input
                    type="text"
                    value={formData.district}
                    onChange={(e) => handleInputChange("district", e.target.value)}
                    placeholder="e.g. Ahmedabad"
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
                    onChange={(e) => handleInputChange("pincode", e.target.value.replace(/\D/g, ""))}
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
                  value={formData.shopMapLink}
                  onChange={(e) => handleInputChange("shopMapLink", e.target.value)}
                  placeholder="https://maps.app.goo.gl/..."
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              {/* Garment Categories Preference */}
              <div className="pt-2 border-t border-zinc-100 dark:border-zinc-800">
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-2">
                  Garments / Products Dealt In (Select all that apply)
                </label>
                <div className="flex flex-wrap gap-1.5 mb-2">
                  {GARMENT_CATEGORIES.map((cat) => {
                    const isSelected = selectedGarments.includes(cat)
                    return (
                      <button
                        key={cat}
                        type="button"
                        onClick={() => toggleGarment(cat)}
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
                    value={customGarment}
                    onChange={(e) => setCustomGarment(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault()
                        addCustomGarment()
                      }
                    }}
                    placeholder="Add custom garment or category..."
                    className="flex-1 h-9 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                  <button
                    type="button"
                    onClick={addCustomGarment}
                    className="px-3 h-9 bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 text-xs font-bold rounded-xl"
                  >
                    Add
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* STEP 3: KYC & DOCUMENTS */}
          {currentStep === 3 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  <span>Step 3: Verification & KYC Documents</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  Provide GSTIN, PAN, and shop photos for verification.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
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
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
                <div className="sm:col-span-2">
                  <FileUpload
                    label="Shop Front / Showroom Photo"
                    folder="kyc/shop"
                    prefix="shop_front"
                    value={formData.shopPhotoUri}
                    onChange={(url) => handleInputChange("shopPhotoUri", url)}
                    description="Clear photo of your shop with board name visible"
                  />
                </div>

                <div>
                  <FileUpload
                    label="GST Certificate or Visiting Card"
                    folder="kyc/gst"
                    prefix="gst_card"
                    value={formData.gstCertPhotoUri}
                    onChange={(url) => handleInputChange("gstCertPhotoUri", url)}
                  />
                </div>

                <div>
                  <FileUpload
                    label="Aadhaar / Owner ID Photo (Optional)"
                    folder="kyc/id"
                    prefix="aadhar"
                    value={formData.aadharPhotoUri}
                    onChange={(url) => handleInputChange("aadharPhotoUri", url)}
                  />
                </div>
              </div>
            </div>
          )}

          {/* STEP 4: TRANSPORT & BANK */}
          {currentStep === 4 && (
            <div className="space-y-4">
              <div className="border-b border-zinc-100 dark:border-zinc-800 pb-3 mb-4">
                <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100 flex items-center gap-2">
                  <Truck className="w-4 h-4 text-emerald-600" />
                  <span>Step 4: Logistics & Bank Details</span>
                </h3>
                <p className="text-xs text-zinc-500 mt-0.5">
                  Provide preferred transporter, delivery station, and bank account for smooth dispatch.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Preferred Transporter Name (Optional)
                  </label>
                  <input
                    type="text"
                    value={formData.preferredTransporterName}
                    onChange={(e) => handleInputChange("preferredTransporterName", e.target.value)}
                    placeholder="e.g. V-Trans, ARC, TCI Freight"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Booking Station / Destination (Optional)
                  </label>
                  <input
                    type="text"
                    value={formData.transportPreference}
                    onChange={(e) => handleInputChange("transportPreference", e.target.value)}
                    placeholder="e.g. Ring Road Station, Godown Delivery"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Bank Name (Optional)
                  </label>
                  <input
                    type="text"
                    value={formData.bankName}
                    onChange={(e) => handleInputChange("bankName", e.target.value)}
                    placeholder="e.g. State Bank of India, HDFC"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                    Account Number (Optional)
                  </label>
                  <input
                    type="text"
                    value={formData.accountNumber}
                    onChange={(e) => handleInputChange("accountNumber", e.target.value)}
                    placeholder="Bank Account Number"
                    className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  IFSC Code (Optional)
                </label>
                <input
                  type="text"
                  maxLength={11}
                  value={formData.ifscCode}
                  onChange={(e) => handleInputChange("ifscCode", e.target.value.toUpperCase())}
                  placeholder="SBIN0001234"
                  className="w-full h-10 px-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono uppercase"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Additional Notes / Instructions
                </label>
                <textarea
                  rows={2}
                  value={formData.notes}
                  onChange={(e) => handleInputChange("notes", e.target.value)}
                  placeholder="e.g. Special packing instructions, timing preference..."
                  className="w-full p-3 rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>
            </div>
          )}

          {/* STEP 5: VERIFY & SUBMIT */}
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
                  <span className="text-zinc-500">Primary Key:</span>
                  <span className="font-mono font-bold text-indigo-600 dark:text-indigo-400">
                    {formData.gstin ? `${formData.gstin} (GSTIN)` : `+91 ${formData.phone.replace(/\D/g, "").slice(-10)} (Mobile)`}
                  </span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-zinc-500">Proprietor:</span>
                  <span className="font-medium text-zinc-800 dark:text-zinc-200">{formData.name} ({formData.phone})</span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-zinc-500">City / State:</span>
                  <span className="font-medium text-zinc-800 dark:text-zinc-200">{formData.city}, {formData.state}</span>
                </div>
                {formData.preferredTransporterName && (
                  <div className="flex justify-between text-xs">
                    <span className="text-zinc-500">Transport:</span>
                    <span className="font-medium text-zinc-800 dark:text-zinc-200">{formData.preferredTransporterName}</span>
                  </div>
                )}
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
            {currentStep > 1 ? (
              <button
                type="button"
                onClick={handlePrevStep}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-zinc-300 dark:border-zinc-700 text-xs font-semibold text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-all"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Back</span>
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
                    className="inline-flex items-center gap-1 px-3.5 py-2 rounded-xl border border-zinc-300 dark:border-zinc-700 text-xs font-semibold text-zinc-600 dark:text-zinc-400 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-all"
                  >
                    <span>Skip to Verification</span>
                  </button>
                )}
                <button
                  type="button"
                  onClick={handleNextStep}
                  className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900 text-white text-xs font-bold shadow-md transition-all"
                >
                  <span>Continue</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
