import { Customer, Supplier } from "../types"
import { HIMAT_LOGO_DATA_URI } from "./logoBase64"

export interface EnvelopeRecipient {
  id?: string | number
  type: "Customer" | "Supplier" | "Custom"
  firmName: string
  contactPerson?: string
  addressLine1?: string
  addressLine2?: string
  city?: string
  district?: string
  state?: string
  pincode?: string
  phone?: string
  phone2?: string
  gstin?: string
  deliveryType?: string
}

export interface EnvelopeSender {
  companyName: string
  tagline: string
  address: string
  cityStateZip: string
  phone: string
  gstin: string
  includeLogo?: boolean
}

export const DEFAULT_HIMAT_SENDER: EnvelopeSender = {
  companyName: "HIMAT TEXTILE",
  tagline: "YOUR BUSINESS GUIDE ACROSS INDIA",
  address: "First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Sherkotda",
  cityStateZip: "Ahmedabad, Gujarat 380022",
  phone: "+91 98739 38095",
  gstin: "24EASPS6621D1ZG",
  includeLogo: true,
}

export type EnvelopeFontSize = "sm" | "md" | "lg" | "xl"

export interface EnvelopeOptions {
  includeSender?: boolean // Default: true (turn off for pre-printed envelopes)
  includeStampBox?: boolean // Default: true
  includePhone?: boolean // Default: true
  includeGstin?: boolean // Default: false / true if present
  fontSize?: EnvelopeFontSize // Default: "md"
  flip180?: boolean // Default: false (flip 180° for reverse vertical feed)
  sender?: EnvelopeSender
}

/**
 * Creates an EnvelopeRecipient data object from a Customer model
 */
export function createCustomerEnvelopeData(customer: Customer): EnvelopeRecipient {
  const firmName = customer.firmName?.trim() || customer.name?.trim() || "Valued Customer"
  const ownerName = customer.name?.trim() || ""
  const contactPerson =
    customer.firmName &&
    ownerName &&
    customer.firmName.toLowerCase() !== ownerName.toLowerCase()
      ? ownerName
      : ""

  const addressLine1 = customer.shopAddress?.trim() || customer.address?.trim() || ""
  const addressLine2 = customer.marketArea?.trim() || customer.markets?.trim() || ""

  const phone2 =
    customer.phone2?.trim() ||
    (customer.phones && customer.phones.length > 1 ? customer.phones[1] : "")

  return {
    id: customer.id,
    type: "Customer",
    firmName,
    contactPerson,
    addressLine1,
    addressLine2,
    city: customer.city?.trim() || "Ahmedabad",
    district: customer.district?.trim() || "",
    state: customer.state?.trim() || "Gujarat",
    pincode: customer.pincode?.trim() || "",
    phone: customer.phone?.trim() || "",
    phone2,
    gstin: customer.gstin?.trim() || customer.gstNumber?.trim() || "",
    deliveryType: "BOOK POST",
  }
}

/**
 * Creates an EnvelopeRecipient data object from a Supplier model
 */
export function createSupplierEnvelopeData(supplier: Supplier): EnvelopeRecipient {
  const firmName = supplier.firmName?.trim() || supplier.name?.trim() || "Valued Supplier"
  const contactName = supplier.contactPerson?.trim() || supplier.name?.trim() || ""
  const contactPerson =
    supplier.firmName &&
    contactName &&
    supplier.firmName.toLowerCase() !== contactName.toLowerCase()
      ? contactName
      : supplier.contactPerson?.trim() || ""

  const addressLine1 = supplier.officeAddress?.trim() || supplier.address?.trim() || ""
  const addressLine2 =
    supplier.marketArea?.trim() ||
    supplier.marketName?.trim() ||
    supplier.markets?.trim() ||
    ""

  const phone2 =
    supplier.phone2?.trim() ||
    (supplier.phones && supplier.phones.length > 1 ? supplier.phones[1] : "")

  return {
    id: supplier.id,
    type: "Supplier",
    firmName,
    contactPerson,
    addressLine1,
    addressLine2,
    city: supplier.city?.trim() || "Ahmedabad",
    district: supplier.district?.trim() || "",
    state: supplier.state?.trim() || "Gujarat",
    pincode: supplier.pincode?.trim() || "",
    phone: supplier.phone?.trim() || "",
    phone2,
    gstin: supplier.gstin?.trim() || supplier.gstNumber?.trim() || "",
    deliveryType: "BOOK POST",
  }
}

function escapeHtml(text?: string | null): string {
  if (!text) return ""
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;")
}

/**
 * Generates full print-ready HTML for 24cm x 10.5cm envelopes.
 * Uses dynamic @media print (orientation: landscape) and (orientation: portrait)
 * so that when the user toggles Portrait / Landscape in the browser print dialog,
 * the content dynamically adapts and rotates with the page orientation!
 */
export function generateEnvelopeHtml(
  recipients: EnvelopeRecipient[],
  options: EnvelopeOptions = {}
): string {
  const {
    includeSender = true,
    includeStampBox = true,
    includePhone = true,
    includeGstin = true,
    fontSize = "md",
    flip180 = false,
    sender = DEFAULT_HIMAT_SENDER,
  } = options

  // Font size configuration
  const fontSizes = {
    sm: {
      toTag: "9.5pt",
      firmName: "13pt",
      contact: "9.5pt",
      address: "9.5pt",
      location: "10.5pt",
      meta: "9pt",
      lineHeight: "1.3",
    },
    md: {
      toTag: "11pt",
      firmName: "15pt",
      contact: "10.5pt",
      address: "10.5pt",
      location: "11.5pt",
      meta: "10pt",
      lineHeight: "1.38",
    },
    lg: {
      toTag: "12pt",
      firmName: "17.5pt",
      contact: "11.5pt",
      address: "11.5pt",
      location: "12.5pt",
      meta: "10.5pt",
      lineHeight: "1.42",
    },
    xl: {
      toTag: "13pt",
      firmName: "19.5pt",
      contact: "12.5pt",
      address: "12.5pt",
      location: "13.5pt",
      meta: "11pt",
      lineHeight: "1.45",
    },
  }[fontSize || "md"]

  const pagesHtml = recipients
    .map((r, index) => {
      // Build location line: City, District, State - PIN
      const cityDistrictParts = [r.city, r.district].filter(Boolean)
      const cityDistrict = cityDistrictParts.join(", ")

      let statePin = ""
      if (r.state && r.pincode) {
        statePin = `${r.state} - <span class="pincode-highlight">${escapeHtml(r.pincode)}</span>`
      } else if (r.pincode) {
        statePin = `<span class="pincode-highlight">${escapeHtml(r.pincode)}</span>`
      } else if (r.state) {
        statePin = escapeHtml(r.state)
      }

      const locationLine = [cityDistrict, statePin].filter(Boolean).join(", ")

      // Phones
      const phoneParts = [r.phone, r.phone2].filter(Boolean)
      const phoneString = phoneParts.join(" / ")

      return `
      <div class="envelope-sheet" data-page="${index + 1}">
        <div class="envelope-box">
          <!-- Top Left Delivery Tag -->
          ${
            includeStampBox && r.deliveryType && r.deliveryType.trim()
              ? `<div class="delivery-badge">${escapeHtml(r.deliveryType.toUpperCase())}</div>`
              : ""
          }

          <!-- Top Right Postal Stamp Box -->
          ${
            includeStampBox
              ? `<div class="post-box">
                  <span>BOOK POST</span>
                  <span class="post-box-sub">STAMP</span>
                </div>`
              : ""
          }

          <!-- Left Portion: SENDER (FROM) -->
          ${
            includeSender
              ? `
            <div class="sender-section">
              <div class="from-tag">FROM:</div>
              <div class="sender-body">
                ${
                  sender.includeLogo && HIMAT_LOGO_DATA_URI
                    ? `<img src="${HIMAT_LOGO_DATA_URI}" alt="Logo" class="sender-logo" />`
                    : ""
                }
                <div>
                  <div class="sender-title">${escapeHtml(sender.companyName)}</div>
                  <div class="sender-tagline">${escapeHtml(sender.tagline)}</div>
                  <div class="sender-desc">${escapeHtml(sender.address)}</div>
                  <div class="sender-desc">${escapeHtml(sender.cityStateZip)}</div>
                  <div class="sender-contact">
                    <strong>Ph:</strong> ${escapeHtml(sender.phone)}
                    ${sender.gstin ? `&bull; <strong>GSTIN:</strong> ${escapeHtml(sender.gstin)}` : ""}
                  </div>
                </div>
              </div>
            </div>
          `
              : ""
          }

          <!-- Right Portion: RECIPIENT (TO) -->
          <div class="recipient-section">
            <div class="to-tag">TO,</div>

            <div class="recipient-name">
              ${escapeHtml(r.firmName)}
            </div>

            ${
              r.contactPerson
                ? `<div class="recipient-contact">
                    <span class="attn-tag">Attn / Prop:</span> ${escapeHtml(r.contactPerson)}
                  </div>`
                : ""
            }

            ${
              r.addressLine1
                ? `<div class="recipient-address">${escapeHtml(r.addressLine1)}</div>`
                : ""
            }

            ${
              r.addressLine2
                ? `<div class="recipient-address">${escapeHtml(r.addressLine2)}</div>`
                : ""
            }

            ${
              locationLine
                ? `<div class="recipient-location">${locationLine}</div>`
                : ""
            }

            ${
              includePhone && phoneString
                ? `<div class="recipient-meta">
                    <strong>Mobile:</strong> ${escapeHtml(phoneString)}
                  </div>`
                : ""
            }

            ${
              includeGstin && r.gstin
                ? `<div class="recipient-meta">
                    <strong>GSTIN:</strong> <span class="mono">${escapeHtml(r.gstin)}</span>
                  </div>`
                : ""
            }
          </div>
        </div>
      </div>
    `
    })
    .join("\n")

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Envelope Print (24cm x 10.5cm)</title>
  <style>
    /* Let the browser print dialog control portrait/landscape */
    @page {
      size: auto;
      margin: 0;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      background-color: #f1f5f9;
      color: #0f172a;
      -webkit-font-smoothing: antialiased;
    }

    /* Screen display wrapper */
    .envelope-sheet {
      width: 240mm;
      height: 105mm;
      margin: 16px auto;
      background-color: #ffffff;
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
      border-radius: 4px;
      overflow: hidden;
      border: 1px solid #cbd5e1;
    }

    .envelope-box {
      width: 240mm;
      height: 105mm;
      position: relative;
      background-color: #ffffff;
      overflow: hidden;
    }

    /* ========================================================================= */
    /* PRINT MEDIA QUERIES: ADAPTS AUTOMATICALLY TO BROWSER ORIENTATION CHOICES */
    /* ========================================================================= */
    @media print {
      body {
        background-color: #ffffff !important;
        margin: 0 !important;
        padding: 0 !important;
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
      }

      /* 1. When user selects LANDSCAPE in browser print dialog */
      @media (orientation: landscape) {
        .envelope-sheet {
          width: 240mm !important;
          height: 105mm !important;
          margin: 0 !important;
          box-shadow: none !important;
          border: none !important;
          page-break-after: always !important;
          break-after: page !important;
        }
        .envelope-box {
          width: 240mm !important;
          height: 105mm !important;
          transform: none !important;
        }
      }

      /* 2. When user selects PORTRAIT in browser print dialog (Vertical Envelope Feed: 10.5cm edge first) */
      @media (orientation: portrait) {
        .envelope-sheet {
          width: 105mm !important;
          height: 240mm !important;
          margin: 0 !important;
          box-shadow: none !important;
          border: none !important;
          page-break-after: always !important;
          break-after: page !important;
          position: relative !important;
          overflow: hidden !important;
        }
        .envelope-box {
          width: 240mm !important;
          height: 105mm !important;
          position: absolute !important;
          top: 0 !important;
          left: 0 !important;
          transform-origin: 0 0 !important;
          ${
            flip180
              ? "transform: translate(0, 240mm) rotate(-90deg) !important;"
              : "transform: translate(105mm, 0) rotate(90deg) !important;"
          }
        }
      }
    }

    /* Top Left Delivery Tag */
    .delivery-badge {
      position: absolute;
      top: 6mm;
      left: 12mm;
      font-size: 8pt;
      font-weight: 800;
      letter-spacing: 1px;
      padding: 1.5mm 3.5mm;
      background: #0f172a;
      color: #ffffff;
      border-radius: 2px;
      text-transform: uppercase;
    }

    /* Top Right Postal Box */
    .post-box {
      position: absolute;
      top: 6mm;
      right: 10mm;
      width: 28mm;
      height: 16mm;
      border: 1.2px dashed #94a3b8;
      border-radius: 3px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      background: #fafaf9;
      font-size: 7.5pt;
      font-weight: 800;
      color: #334155;
      letter-spacing: 0.8px;
    }
    .post-box-sub {
      font-size: 5.5pt;
      color: #64748b;
      margin-top: 0.5mm;
      letter-spacing: 0.5px;
    }

    /* Left Portion: SENDER (FROM) */
    .sender-section {
      position: absolute;
      top: 16mm;
      left: 12mm;
      width: 95mm;
      max-width: 102mm;
    }
    .from-tag {
      font-size: 7.5pt;
      font-weight: 800;
      color: #475569;
      letter-spacing: 0.8px;
      border-bottom: 1.5px solid #0f172a;
      padding-bottom: 0.8mm;
      display: inline-block;
      margin-bottom: 2mm;
    }
    .sender-body {
      display: flex;
      align-items: flex-start;
      gap: 3mm;
    }
    .sender-logo {
      height: 11mm;
      width: auto;
      object-fit: contain;
      flex-shrink: 0;
      margin-top: 0.5mm;
    }
    .sender-title {
      font-size: 10pt;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: 0.5px;
      line-height: 1.2;
    }
    .sender-tagline {
      font-size: 6.5pt;
      font-weight: 700;
      color: #475569;
      letter-spacing: 0.6px;
      margin-bottom: 1.5mm;
    }
    .sender-desc {
      font-size: 7.5pt;
      color: #334155;
      line-height: 1.35;
    }
    .sender-contact {
      font-size: 7.5pt;
      color: #1e293b;
      margin-top: 1.5mm;
      line-height: 1.35;
    }

    /* Right Portion: RECIPIENT (TO) */
    .recipient-section {
      position: absolute;
      top: 20mm;
      left: 52%;
      right: 12mm;
      line-height: ${fontSizes.lineHeight};
    }
    .to-tag {
      font-size: ${fontSizes.toTag};
      font-weight: 800;
      color: #0f172a;
      letter-spacing: 1px;
      margin-bottom: 1.5mm;
    }
    .recipient-name {
      font-size: ${fontSizes.firmName};
      font-weight: 800;
      color: #09090b;
      text-transform: uppercase;
      letter-spacing: 0.4px;
      margin-bottom: 1mm;
      line-height: 1.25;
      word-break: break-word;
    }
    .recipient-contact {
      font-size: ${fontSizes.contact};
      font-weight: 600;
      color: #1e293b;
      margin-bottom: 1mm;
    }
    .attn-tag {
      font-size: 8.5pt;
      font-weight: 700;
      color: #64748b;
      text-transform: uppercase;
    }
    .recipient-address {
      font-size: ${fontSizes.address};
      color: #1e293b;
      line-height: 1.35;
      margin-bottom: 0.8mm;
      word-break: break-word;
    }
    .recipient-location {
      font-size: ${fontSizes.location};
      font-weight: 800;
      color: #09090b;
      text-transform: uppercase;
      margin-top: 1mm;
      margin-bottom: 1.5mm;
    }
    .pincode-highlight {
      font-weight: 900;
      letter-spacing: 0.8px;
    }
    .recipient-meta {
      font-size: ${fontSizes.meta};
      color: #334155;
      margin-top: 1mm;
      line-height: 1.35;
    }
    .mono {
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-weight: 700;
    }
  </style>
</head>
<body>
  ${pagesHtml}
</body>
</html>`
}

/**
 * Opens a print popup/window ready for printing
 */
export function printEnvelopeHtml(title: string, htmlContent: string) {
  const printWindow = window.open("", "_blank", "width=960,height=750")
  if (!printWindow) {
    alert("Please allow popups to preview and print envelopes.")
    return
  }
  printWindow.document.open()
  printWindow.document.write(htmlContent)
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
  }, 400)
}
