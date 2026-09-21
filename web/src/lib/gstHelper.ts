/**
 * GSTIN (Goods and Services Tax Identification Number) Helper
 * Provides offline state decoding, PAN extraction, format validation,
 * and best-effort live detail fetching.
 */

export const GST_STATE_CODES: Record<string, string> = {
  "01": "Jammu and Kashmir",
  "02": "Himachal Pradesh",
  "03": "Punjab",
  "04": "Chandigarh",
  "05": "Uttarakhand",
  "06": "Haryana",
  "07": "Delhi",
  "08": "Rajasthan",
  "09": "Uttar Pradesh",
  "10": "Bihar",
  "11": "Sikkim",
  "12": "Arunachal Pradesh",
  "13": "Nagaland",
  "14": "Manipur",
  "15": "Mizoram",
  "16": "Tripura",
  "17": "Meghalaya",
  "18": "Assam",
  "19": "West Bengal",
  "20": "Jharkhand",
  "21": "Odisha",
  "22": "Chhattisgarh",
  "23": "Madhya Pradesh",
  "24": "Gujarat",
  "25": "Daman and Diu",
  "26": "Dadra and Nagar Haveli",
  "27": "Maharashtra",
  "28": "Andhra Pradesh (Old)",
  "29": "Karnataka",
  "30": "Goa",
  "31": "Lakshadweep",
  "32": "Kerala",
  "33": "Tamil Nadu",
  "34": "Puducherry",
  "35": "Andaman and Nicobar",
  "36": "Telangana",
  "37": "Andhra Pradesh",
  "38": "Ladakh"
}

// 15-character GSTIN structure: 2 digits (state) + 10 chars (PAN) + 1 entity code + 1 'Z' + 1 check digit
export const GSTIN_REGEX = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/i

export function isValidGstin(gstin: string): boolean {
  if (!gstin) return false
  return GSTIN_REGEX.test(gstin.trim())
}

export function extractPanFromGstin(gstin: string): string {
  const clean = gstin.trim().toUpperCase()
  if (clean.length >= 12) {
    const pan = clean.substring(2, 12)
    // Validate PAN format: 5 letters + 4 digits + 1 letter
    if (/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(pan)) {
      return pan
    }
  }
  return ""
}

export function getStateFromGstin(gstin: string): string {
  const clean = gstin.trim()
  if (clean.length >= 2) {
    const code = clean.substring(0, 2)
    return GST_STATE_CODES[code] || ""
  }
  return ""
}

export interface GstFetchedDetails {
  firmName?: string
  legalName?: string
  tradeName?: string
  address?: string
  city?: string
  state?: string
  pincode?: string
  pan?: string
}

/**
 * Attempts to fetch business name and address for a given GSTIN.
 * Tries public endpoints with timeout, and falls back gracefully to
 * offline state & PAN extraction if network request is unavailable.
 */
export async function fetchGstDetails(gstin: string): Promise<GstFetchedDetails | null> {
  const clean = gstin.trim().toUpperCase()
  if (!isValidGstin(clean)) {
    return null
  }

  const baseResult: GstFetchedDetails = {
    state: getStateFromGstin(clean) || undefined,
    pan: extractPanFromGstin(clean) || undefined
  }

  try {
    // Attempt public CORS lookup proxy with short timeout (4 seconds)
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 4000)

    const proxyUrls = [
      `https://api.allorigins.win/raw?url=${encodeURIComponent(`https://commonapi.mastersindia.co/commonapis/searchgstin?gstin=${clean}`)}`,
      `https://api.allorigins.win/raw?url=${encodeURIComponent(`https://sheet.gstincheck.co.in/check/${clean}`)}`
    ]

    for (const url of proxyUrls) {
      try {
        const res = await fetch(url, { signal: controller.signal })
        if (res.ok) {
          const data = await res.json()
          clearTimeout(timeoutId)

          const tradeName = data.tradeNam || data.trade_name || data.data?.tradeNam || data.data?.trade_name || ""
          const legalName = data.lgnm || data.legal_name || data.data?.lgnm || data.data?.legal_name || ""
          const firmName = tradeName || legalName || ""

          // Address parsing
          let address = ""
          let city = ""
          let state = baseResult.state || ""
          let pincode = ""

          const pradr = data.pradr?.addr || data.data?.pradr?.addr || data.address
          if (pradr) {
            if (typeof pradr === "string") {
              address = pradr
            } else {
              const parts = [
                pradr.bno && `Shop/Bldg No. ${pradr.bno}`,
                pradr.bnm,
                pradr.st,
                pradr.loc,
                pradr.city || pradr.dst
              ].filter(Boolean)
              address = parts.join(", ")
              city = pradr.loc || pradr.city || pradr.dst || ""
              pincode = pradr.pncd || ""
              state = pradr.stcd || state
            }
          }

          if (firmName || address) {
            return {
              firmName: firmName || undefined,
              legalName: legalName || undefined,
              tradeName: tradeName || undefined,
              address: address || undefined,
              city: city || undefined,
              state: state || baseResult.state,
              pincode: pincode || undefined,
              pan: baseResult.pan
            }
          }
        }
      } catch (_subErr) {
        // Try next proxy or fallback
      }
    }
    clearTimeout(timeoutId)
  } catch (_err) {
    // Network / CORS / Timeout - graceful fallback
  }

  // If live network fetch could not retrieve name/address, return offline extracted fields
  return baseResult
}
