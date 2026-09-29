import QRCode from "qrcode"
import { Customer, Employee, PackGroup, PurchaseEntry, Supplier, Visit } from "../types"
import { formatInr } from "./utils"
import { HIMAT_LOGO_DATA_URI } from "./logoBase64"
import { salesmenForReport } from "./domain"

export interface CustomerReportData {
  visit: Visit
  customer?: Customer | null
  salesman?: Employee | null
  entries: PurchaseEntry[]
  packGroups?: PackGroup[]
}

export const ORDER_NATURE_SELF = "Self Order"
export const ORDER_NATURE_WHATSAPP = "WhatsApp Order"

/**
 * Choices made before printing a supplier order form (same as the Android "Order Form PDF" sheet).
 * The supplier's GSTIN and market area are optional extras, off unless switched on.
 */
export interface SupplierOrderFormOptions {
  showGstin?: boolean
  showMarketArea?: boolean
  /** Printed as-is, e.g. "Self Order" or "WhatsApp Order" (user editable) */
  orderNature?: string
}

export const DEFAULT_ORDER_FORM_OPTIONS: Required<SupplierOrderFormOptions> = {
  showGstin: false,
  showMarketArea: false,
  orderNature: ORDER_NATURE_SELF,
}

export interface SupplierInvoiceData {
  supplier: Supplier
  visit: Visit
  customer?: Customer | null
  salesman?: Employee | null
  entries: PurchaseEntry[]
  options?: SupplierOrderFormOptions
}

/** Escapes text typed by users before it goes into report HTML. */
function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")
}

/** The supplier's own primary number: the first filled phone slot (never our office number). */
function supplierPrimaryPhone(supplier: Supplier): string {
  const slots = [supplier.phone, supplier.phone2, supplier.phone3, supplier.phone4, supplier.phone5, ...(supplier.phones || [])]
  return slots.map((p) => (p || "").trim()).find((p) => p.length > 0) || ""
}

export function generateUpiQrSvg(upiUrl: string, size = 100): string {
  try {
    const qr = QRCode.create(upiUrl, { errorCorrectionLevel: "M" })
    const modCount = qr.modules.size
    const cellSize = size / modCount
    let rects = ""
    for (let r = 0; r < modCount; r++) {
      for (let c = 0; c < modCount; c++) {
        if (qr.modules.get(r, c)) {
          rects += `<rect x="${(c * cellSize).toFixed(2)}" y="${(r * cellSize).toFixed(2)}" width="${(cellSize + 0.1).toFixed(2)}" height="${(cellSize + 0.1).toFixed(2)}" fill="#0f172a" />`
        }
      }
    }
    return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}"><rect width="100%" height="100%" fill="#ffffff"/>${rects}</svg>`
  } catch {
    return `<div style="width:${size}px;height:${size}px;display:flex;align-items:center;justify-content:center;background:#f1f5f9;font-size:10px;color:#64748b;">QR Code</div>`
  }
}

/**
 * Builds HTML for Customer Consolidated Purchase Report
 * Matches Android PdfGenerator.generateCustomerDayReport
 */
export function generateCustomerDayReportHtml(data: CustomerReportData): string {
  const { visit, customer, salesman, entries } = data

  const totalPieces = entries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  const totalAmount = entries.reduce((sum, e) => sum + (Number(e.totalAmount) || 0), 0)
  const totalGst = entries.reduce((sum, e) => sum + (Number(e.gstAmount) || 0), 0)
  const grandTotal = entries.reduce(
    (sum, e) => sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )

  // Primary transporter & booking station
  const primaryTransporter =
    entries.find((e) => e.transporter && e.transporter.trim())?.transporter ||
    customer?.transportName ||
    customer?.preferredTransporterName ||
    "Shree Maruti Transport"
  const bookingStation = customer?.bookingStation || (customer?.city ? `${customer.city} (${customer.city.substring(0, 3).toUpperCase()})` : "Ahmedabad (ADI)")
  const lrNo = entries.find((e) => e.lrNo && e.lrNo.trim())?.lrNo || "—"
  const orderDate = visit.date || new Date().toISOString().split("T")[0]
  const deliveryTo = customer?.city ? `${customer.city}${customer.state ? `, ${customer.state}` : ""}` : "Ahiliyanagar, Maharashtra"

  // 11 columns: SR. NO., ORDER NO., BRAND, ITEM / STYLE, PCS, RATE (₹), AMOUNT (₹), CASE & PACKING, STATUS, PURCHASE DATE, SALESMAN
  let orderRowsHtml = ""
  entries.forEach((item, idx) => {
    const srNo = idx + 1
    const orderNo = item.orderNo || `HT-${2620 + idx}`
    const brand = item.supplierName || "—"
    const itemCode = item.itemCode || "—"
    const pc = item.pieces || 0
    const rate = item.rate || item.pricePerPiece || 0
    const amt = Number(item.totalAmount) || (pc * rate)
    const packDesc =
      item.loosePieces > 0
        ? `${item.caseCount}c+${item.loosePieces}L`
        : `${item.caseCount} cs`
    // A mixed-pack note is packing information, so it prints inside the CASE & PACKING cell rather
    // than on a separate full-width row under the order.
    const packNote = (item.mixedPackNote || "").trim()
    const packNoteHtml = packNote
      ? `<div style="font-size:6.5px;font-style:italic;color:#64748b;line-height:1.25;margin-top:1px;">${escapeHtml(packNote)}</div>`
      : ""
    const pDate = item.orderDate || visit.date || orderDate
    const salesmanName = item.salesmanName || salesman?.name || visit.employeeName || "Jalam Bhai"
    const status = item.deliveryStatus || "Pending"
    const isDel = status.toLowerCase() === "delivered"
    const isDisp = status.toLowerCase() === "dispatched"
    const statusBg = isDel ? "#dcfce7" : (isDisp ? "#dbeafe" : "#fef3c7")
    const statusColor = isDel ? "#15803d" : (isDisp ? "#1d4ed8" : "#b45309")
    const statusBadge = `<span style="background:${statusBg};color:${statusColor};padding:2px 5px;border-radius:3px;font-weight:700;font-size:7.5px;display:inline-block;white-space:nowrap;">${status}</span>`

    orderRowsHtml += `
      <tr class="item-row">
        <td class="text-center" style="width: 4%; color: #334155;">${srNo}</td>
        <td class="font-medium" style="width: 9%; color: #0f172a;">${orderNo}</td>
        <td class="font-medium" style="width: 12%; color: #0f172a;">${brand}</td>
        <td class="font-medium" style="width: 14%; color: #0f172a;">${itemCode}</td>
        <td class="text-center font-medium" style="width: 5%; color: #0f172a;">${pc}</td>
        <td class="text-right" style="width: 8%; color: #0f172a;">${formatInr(rate)}</td>
        <td class="text-right" style="width: 11%; color: #0f172a;">${formatInr(amt)}</td>
        <td class="text-center" style="width: 10%; color: #334155;">
          ${packDesc}${packNoteHtml}
        </td>
        <td class="text-center" style="width: 9%;">${statusBadge}</td>
        <td class="text-center" style="width: 9%; color: #334155;">${pDate}</td>
        <td class="font-medium" style="width: 9%; color: #0f172a;">${salesmanName}</td>
      </tr>
    `
  })

  const customerBrand = (customer?.firmName || customer?.name || visit.customerName || "RANGOLI COLLETION AHILIYANAGAR").toUpperCase()
  // Brand (shop / firm) name for the document title, which the browser uses as the PDF file name
  const customerBrandName = (customer?.firmName || "").trim() || customer?.name || visit.customerName || "Customer"
  const customerOwner = customer?.name || "Mr. Jitendra Bhai"
  const customerGstin = customer?.gstin || customer?.gstNumber || "Unregistered"
  const customerPhone = customer?.phone || "+91 98765 43210"
  const customerAddress = customer?.shopAddress || customer?.address || (customer?.city ? `${customer.city}${customer.state ? `, ${customer.state}` : ""}` : "Ahiliyanagar, Maharashtra")

  // All salesmen of the trip (per-order salesmen first, then everybody who joined), like the Android PDF
  const allSalesmen = salesmenForReport(visit, entries)
  const activeSalesmanName =
    (allSalesmen.length > 0 ? allSalesmen.join(", ") : "") || salesman?.name || visit.employeeName || "Jalam Bhai"
  const activeSalesmanPhone = salesman?.phone || "+91 98739 38095"
  const activeSalesmanEmail = salesman?.email || "jalam@himattextile.com"
  // When a Sub Agent brought this customer their name sits right under the salesman's
  const subAgentName = (customer?.subAgentName || "").trim()
  const subAgentRowHtml = subAgentName
    ? `<div class="kv-row"><span class="kv-k" style="width: 82px;">Sub Agent</span><span class="kv-sep">:</span><span class="kv-v font-bold">${escapeHtml(subAgentName)}</span></div>`
    : ""

  // Official Payment QR Code (UPI)
  const paymentQrSvg = generateUpiQrSvg("upi://pay?pa=eazypay.0000053310@icici&pn=ICICI%20Bank%20InstaBIZ%20Merchant&tr=EZYS0000053310&cu=INR&mc=5999", 76)
  // Website QR Code (Clean SVG)
  const websiteQrSvg = generateUpiQrSvg("https://himattextile.com", 76)

  return `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>${escapeHtml(customerBrandName)} - Customer Report ${visit.visitCode || ""}</title>
  <style>
    /* Our company details in bold (placed first but more specific, so it wins) */
    html body .company-address, html body .company-contact { font-weight: 700; color: #334155; }
  </style>
  <style>
    @page {
      size: A4 portrait;
      margin: 8mm 10mm;
    }
    * {
      box-sizing: border-box;
      -webkit-print-color-adjust: exact !important;
      print-color-adjust: exact !important;
    }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      margin: 0;
      padding: 0;
      color: #0f172a;
      background: #ffffff;
      font-size: 9.5px;
      line-height: 1.35;
    }

    /* HEADER */
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 10px;
    }
    .header-left {
      display: flex;
      align-items: center;
      gap: 14px;
      flex: 1;
    }
    .company-logo {
      height: 52px;
      width: auto;
      object-fit: contain;
      flex-shrink: 0;
    }
    .company-title {
      font-size: 20px;
      font-weight: 900;
      letter-spacing: 0.5px;
      color: #0f172a;
      margin: 0;
      line-height: 1.1;
    }
    .company-tagline {
      font-size: 8.5px;
      font-weight: 800;
      color: #d97706; /* Golden Yellow */
      letter-spacing: 0.8px;
      margin-top: 3px;
      text-transform: uppercase;
    }
    .company-address {
      font-size: 7.5px;
      color: #64748b;
      margin-top: 2px;
      line-height: 1.25;
    }
    .company-contact {
      font-size: 7.5px;
      color: #64748b;
      margin-top: 1.5px;
    }
    .header-v-divider {
      width: 1px;
      height: 60px;
      background: #e2e8f0;
      margin: 0 18px;
    }
    .header-right {
      text-align: left;
      min-width: 180px;
    }
    .report-title-top {
      font-size: 15px;
      font-weight: 900;
      color: #0f766e;
      letter-spacing: 0.5px;
      line-height: 1.1;
    }
    .report-title-bottom {
      font-size: 15px;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: 0.5px;
      line-height: 1.1;
    }
    .meta-line {
      font-size: 8px;
      color: #475569;
      margin-top: 3px;
      display: flex;
      align-items: center;
      gap: 5px;
    }

    /* DETAILS CARD */
    .details-card {
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 10px 14px;
      margin-top: 8px;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      background: #ffffff;
    }
    .col-header {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 8px;
      font-weight: 800;
      color: #475569;
      letter-spacing: 0.5px;
      text-transform: uppercase;
      margin-bottom: 4px;
    }
    .customer-firm-title {
      font-size: 13.5px;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: 0.3px;
      margin-bottom: 5px;
      text-transform: uppercase;
    }
    .kv-grid {
      display: table;
      width: 100%;
      font-size: 8.5px;
    }
    .kv-row {
      display: table-row;
      line-height: 1.5;
    }
    .kv-k {
      display: table-cell;
      color: #475569;
      width: 95px;
      white-space: nowrap;
    }
    .kv-sep {
      display: table-cell;
      width: 14px;
      text-align: center;
      color: #64748b;
    }
    .kv-v {
      display: table-cell;
      color: #0f172a;
      font-weight: 500;
    }

    /* TABLE */
    .table-container {
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      overflow: hidden;
      margin-top: 10px;
    }
    table.data-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 8.5px;
    }
    table.data-table thead tr {
      background: #f8fafc;
      border-bottom: 1.5px solid #cbd5e1;
    }
    table.data-table th {
      padding: 6px 5px;
      font-weight: 800;
      font-size: 7.5px;
      letter-spacing: 0.4px;
      color: #0f172a;
      border-right: 1px solid #e2e8f0;
      text-align: left;
    }
    table.data-table th:last-child {
      border-right: none;
    }
    table.data-table td {
      padding: 4.5px 5px;
      border-right: 1px solid #e2e8f0;
      border-bottom: 1px solid #e2e8f0;
    }
    table.data-table td:last-child {
      border-right: none;
    }
    table.data-table tr:last-child td {
      border-bottom: none;
    }
    .note-row td {
      padding: 2px 6px 4px 6px;
      background: #fafafa;
    }
    .note-text {
      color: #64748b;
      font-style: italic;
      font-size: 7.5px;
    }
    .text-center { text-align: center; }
    .text-right { text-align: right; }
    .font-bold { font-weight: 700; }
    .font-medium { font-weight: 600; }

    /* TOTALS BOX */
    .totals-wrapper {
      display: flex;
      justify-content: flex-end;
      margin-top: 8px;
    }
    .totals-box {
      width: 280px;
      font-size: 9.5px;
    }
    .totals-row {
      display: flex;
      justify-content: space-between;
      padding: 2.5px 6px;
      color: #334155;
    }
    .grand-total-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #eef2f6;
      border-radius: 4px;
      padding: 5px 8px;
      margin-top: 3px;
    }
    .grand-total-label {
      font-weight: 800;
      font-size: 11px;
      color: #0f172a;
    }
    .grand-total-value {
      font-weight: 900;
      font-size: 12px;
      color: #0f172a;
    }

    /* BOTTOM THREE-COLUMN CARD */
    .bottom-card {
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 10px 12px;
      margin-top: 10px;
      display: grid;
      grid-template-columns: 1.15fr 1.35fr 0.9fr;
      gap: 12px;
      background: #ffffff;
    }
    .bottom-col {
      display: flex;
      flex-direction: column;
    }
    .col-divider {
      border-right: 1px solid #e2e8f0;
      padding-right: 10px;
    }
    .bottom-col-header {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 7.5px;
      font-weight: 800;
      color: #0f172a;
      letter-spacing: 0.4px;
      margin-bottom: 5px;
      text-transform: uppercase;
    }

    /* NOTE ALERT RIBBON */
    .note-alert-banner {
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 4px;
      padding: 5px 10px;
      margin-top: 8px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .note-alert-icon {
      background: #dc2626;
      color: #ffffff;
      width: 14px;
      height: 14px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 8px;
      font-weight: 900;
      flex-shrink: 0;
    }
    .note-alert-text {
      color: #991b1b;
      font-size: 7.5px;
      line-height: 1.3;
    }

    /* FOOTER */
    .doc-footer {
      border-top: 1px solid #e2e8f0;
      padding-top: 6px;
      margin-top: 10px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 7.5px;
    }
    .footer-tagline {
      font-weight: 800;
      color: #d97706; /* Golden Yellow */
      letter-spacing: 0.5px;
    }
    .footer-divider-line {
      flex: 1;
      height: 1px;
      background: #e2e8f0;
      margin: 0 14px;
    }
    .footer-links {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .footer-web-link {
      display: flex;
      align-items: center;
      gap: 4px;
      text-decoration: none;
      color: #0f172a;
      font-weight: 700;
      font-size: 8px;
    }
    .footer-pipe {
      color: #cbd5e1;
      font-weight: 300;
    }
    .social-svg {
      display: inline-flex;
      align-items: center;
      color: #0f172a;
      text-decoration: none;
    }
  </style>
</head>
<body>
  <!-- HEADER -->
  <div class="header">
    <div class="header-left">
      <img src="${HIMAT_LOGO_DATA_URI}" alt="Himat Textile Logo" class="company-logo" />
      <div>
        <h1 class="company-title">HIMAT TEXTILE</h1>
        <div class="company-tagline">YOUR GARMENT GUIDE ACROSS INDIA</div>
        <div class="company-address">First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Ahmedabad, Gujarat 380022</div>
        <div class="company-contact"><strong>GSTIN:</strong> 24EASPS6621D1ZG &nbsp;|&nbsp; <strong>Phone:</strong> <a href="tel:+919873938095" style="color: inherit; text-decoration: none;">+91 98739 38095</a></div>
      </div>
    </div>
    <div class="header-v-divider"></div>
    <div class="header-right">
      <div class="report-title-top">CUSTOMER</div>
      <div class="report-title-bottom">PURCHASE REPORT</div>
      <div class="meta-line">
        <svg width="11" height="11" viewBox="0 0 24 24" fill="#64748b"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>
        <span><strong>Visit ID:</strong> ${visit.visitCode}</span>
      </div>
      <div class="meta-line">
        <svg width="11" height="11" viewBox="0 0 24 24" fill="#64748b"><path d="M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10z"/></svg>
        <span><strong>Date:</strong> ${orderDate}</span>
      </div>
    </div>
  </div>

  <!-- CUSTOMER & TRANSPORT DETAILS -->
  <div class="details-card">
    <!-- Left: Customer / Shop Details -->
    <div>
      <div class="col-header">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="#0f766e"><path d="M20 4H4v2h16V4zm1 10v-2l-1-5H4l-1 5v2h1v6h10v-6h4v6h2v-6h1zm-9 4H6v-4h6v4z"/></svg>
        <span>CUSTOMER / SHOP DETAILS</span>
      </div>
      <div class="customer-firm-title">${customerBrand}</div>
      <div class="kv-grid">
        <div class="kv-row">
          <span class="kv-k">Proprietor</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">${customerOwner}</span>
        </div>
        <div class="kv-row">
          <span class="kv-k">Phone</span>
          <span class="kv-sep">:</span>
          <span class="kv-v"><a href="tel:${customerPhone}" style="color: inherit; text-decoration: none;">${customerPhone}</a></span>
        </div>
        <div class="kv-row">
          <span class="kv-k">GSTIN</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">${customerGstin}</span>
        </div>
        <div class="kv-row">
          <span class="kv-k">Address</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">${customerAddress}</span>
        </div>
      </div>
    </div>

    <!-- Right: Transport & Booking Details -->
    <div>
      <div class="col-header">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="#0f766e"><path d="M20 8h-3V4H1v13h2c0 1.66 1.34 3 3 3s3-1.34 3-3h6c0 1.66 1.34 3 3 3s3-1.34 3-3h2v-5l-3-4zm-.5 1.5l1.96 2.5H17V9.5h2.5zM6 18c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm12 0c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1z"/></svg>
        <span>TRANSPORT & BOOKING DETAILS</span>
      </div>
      <div style="height: 19px;"></div>
      <div class="kv-grid">
        <div class="kv-row">
          <span class="kv-k">Transporter</span>
          <span class="kv-sep">:</span>
          <span class="kv-v font-medium">${primaryTransporter}</span>
        </div>
        <div class="kv-row">
          <span class="kv-k">Booking Station</span>
          <span class="kv-sep">:</span>
          <span class="kv-v font-medium">${bookingStation}</span>
        </div>
        <div class="kv-row">
          <span class="kv-k">LR / Booking No.</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">${lrNo}</span>
        </div>
        <div class="kv-row">
          <span class="kv-k">Dispatch Date</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">${orderDate}</span>
        </div>
        <div class="kv-row">
          <span class="kv-k">Delivery To</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">${deliveryTo}</span>
        </div>
      </div>
    </div>
  </div>

  <!-- ITEMS TABLE -->
  <div class="table-container">
    <table class="data-table">
      <thead>
        <tr>
          <th style="width: 4%; text-align: center;">SR. NO.</th>
          <th style="width: 9%;">ORDER NO.</th>
          <th style="width: 12%;">BRAND</th>
          <th style="width: 14%;">ITEM / STYLE</th>
          <th style="width: 5%; text-align: center;">PCS</th>
          <th style="width: 8%; text-align: right;">RATE (₹)</th>
          <th style="width: 11%; text-align: right;">AMOUNT (₹)</th>
          <th style="width: 10%; text-align: center;">CASE & PACKING</th>
          <th style="width: 9%; text-align: center;">STATUS</th>
          <th style="width: 9%; text-align: center;">PURCHASE DATE</th>
          <th style="width: 9%;">SALESMAN</th>
        </tr>
      </thead>
      <tbody>
        ${orderRowsHtml}
      </tbody>
    </table>
  </div>

  <!-- TOTALS ROW -->
  <div class="totals-wrapper">
    <div class="totals-box">
      <div class="totals-row">
        <span>Subtotal (${totalPieces} Pcs)</span>
        <span class="font-bold">₹${formatInr(totalAmount)}</span>
      </div>
      <div class="totals-row">
        <span>Garment GST (5%)</span>
        <span class="font-bold">₹${formatInr(totalGst)}</span>
      </div>
      <div class="grand-total-row">
        <span class="grand-total-label">Grand Total</span>
        <span class="grand-total-value">₹${formatInr(grandTotal)}</span>
      </div>
    </div>
  </div>

  <!-- BOTTOM 3-COLUMN CARD -->
  <div class="bottom-card">
    <!-- Col 1: Salesman Details -->
    <div class="bottom-col col-divider">
      <div class="bottom-col-header">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f766e"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
        <span>SALESMAN DETAILS</span>
      </div>
      <div class="kv-grid">
        <div class="kv-row"><span class="kv-k" style="width: 82px;">Salesman Name</span><span class="kv-sep">:</span><span class="kv-v font-bold">${activeSalesmanName}</span></div>
        ${subAgentRowHtml}
        <div class="kv-row"><span class="kv-k" style="width: 82px;">Contact Number</span><span class="kv-sep">:</span><span class="kv-v"><a href="tel:${activeSalesmanPhone}" style="color: inherit; text-decoration: none;">${activeSalesmanPhone}</a></span></div>
        <div class="kv-row"><span class="kv-k" style="width: 82px;">WhatsApp</span><span class="kv-sep">:</span><span class="kv-v"><a href="https://wa.me/${activeSalesmanPhone.replace(/[^0-9]/g, '')}" target="_blank" style="color: inherit; text-decoration: none;">${activeSalesmanPhone}</a></span></div>
        <div class="kv-row"><span class="kv-k" style="width: 82px;">Email</span><span class="kv-sep">:</span><span class="kv-v"><a href="mailto:${activeSalesmanEmail}" style="color: inherit; text-decoration: none;">${activeSalesmanEmail}</a></span></div>
      </div>
    </div>

    <!-- Col 2: Bank Account Details -->
    <div class="bottom-col col-divider">
      <div class="bottom-col-header">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f766e"><path d="M4 10h2v7H4zm6 0h2v7h-2zm6 0h2v7h-2zM2 22h19v-3H2v3zm9.5-20L2 7v2h19V7l-9.5-5z"/></svg>
        <span>BANK ACCOUNT DETAILS (HIMAT TEXTILE)</span>
      </div>
      <div class="kv-grid">
        <div class="kv-row"><span class="kv-k" style="width: 82px;">Bank Name</span><span class="kv-sep">:</span><span class="kv-v font-bold">ICICI Bank</span></div>
        <div class="kv-row"><span class="kv-k" style="width: 82px;">Account Name</span><span class="kv-sep">:</span><span class="kv-v font-bold">Himat Textile</span></div>
        <div class="kv-row"><span class="kv-k" style="width: 82px;">Account Number</span><span class="kv-sep">:</span><span class="kv-v font-bold">136805501447</span></div>
        <div class="kv-row"><span class="kv-k" style="width: 82px;">IFSC Code</span><span class="kv-sep">:</span><span class="kv-v font-bold">ICIC0000189</span></div>
        <div class="kv-row"><span class="kv-k" style="width: 82px;">Account Type</span><span class="kv-sep">:</span><span class="kv-v">Current</span></div>
        <div class="kv-row"><span class="kv-k" style="width: 82px;">Branch</span><span class="kv-sep">:</span><span class="kv-v">Ashram Road, Ahmedabad</span></div>
      </div>
    </div>

    <!-- Col 3: Scan to Pay via UPI QR -->
    <div class="bottom-col" style="align-items: center; text-align: center;">
      <div class="bottom-col-header" style="justify-content: center;">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f766e"><path d="M21.58 16.09l-1.09-7.66C20.21 6.46 18.52 5 16.53 5H7.47C5.48 5 3.79 6.46 3.51 8.43l-1.09 7.66C2.2 17.63 3.39 19 4.94 19h14.12c1.55 0 2.74-1.37 2.52-2.91zM7 8h10c.55 0 1 .45 1 1s-.45 1-1 1H7c-.55 0-1-.45-1-1s.45-1 1-1zm5 9c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3z"/></svg>
        <span>SCAN TO PAY (UPI QR)</span>
      </div>
      <div style="margin: 2px 0;">
        ${paymentQrSvg}
      </div>
      <div style="font-size: 8px; font-weight: 800; color: #0f172a; letter-spacing: 0.2px;">
        eazypay.0000053310@icici
      </div>
      <div style="font-size: 7.5px; color: #64748b;">
        BHIM UPI • GPay • PhonePe • Paytm
      </div>
    </div>
  </div>

  <!-- NOTE ALERT RIBBON -->
  <div class="note-alert-banner">
    <div class="note-alert-icon">!</div>
    <div class="note-alert-text">
      <strong>Note:</strong> All amounts shown in this report are approximate and may vary from final supplier invoice / dispatch quantity.
    </div>
  </div>

  <!-- FOOTER -->
  <div class="doc-footer">
    <div class="footer-tagline">YOUR GARMENT GUIDE ACROSS INDIA</div>
    <div class="footer-divider-line"></div>
    <div class="footer-links">
      <a href="https://himattextile.com" target="_blank" class="footer-web-link">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f172a"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/></svg>
        <span>himattextile.com</span>
      </a>
      <span class="footer-pipe">|</span>
      <!-- Instagram -->
      <a href="https://instagram.com/himattextile" target="_blank" class="social-svg" title="Instagram">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f172a"><path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/></svg>
      </a>
      <!-- Facebook -->
      <a href="https://facebook.com/himattextile" target="_blank" class="social-svg" title="Facebook">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f172a"><path d="M22.675 0h-21.35c-.732 0-1.325.593-1.325 1.325v21.351c0 .731.593 1.324 1.325 1.324h11.495v-9.294h-3.128v-3.622h3.128v-2.671c0-3.1 1.893-4.788 4.659-4.788 1.325 0 2.463.099 2.795.143v3.24l-1.918.001c-1.504 0-1.795.715-1.795 1.763v2.313h3.587l-.467 3.622h-3.12v9.293h6.116c.73 0 1.323-.593 1.323-1.325v-21.35c0-.732-.593-1.325-1.325-1.325z"/></svg>
      </a>
      <!-- LinkedIn -->
      <a href="https://linkedin.com/company/himattextile" target="_blank" class="social-svg" title="LinkedIn">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f172a"><path d="M19 0h-14c-2.761 0-5 2.239-5 5v14c0 2.761 2.239 5 5 5h14c2.762 0 5-2.239 5-5v-14c0-2.761-2.238-5-5-5zm-11 19h-3v-11h3v11zm-1.5-12.268c-.966 0-1.75-.79-1.75-1.764s.784-1.764 1.75-1.764 1.75.79 1.75 1.764-.783 1.764-1.75 1.764zm13.5 12.268h-3v-5.604c0-3.368-4-3.113-4 0v5.604h-3v-11h3v1.765c1.396-2.586 7-2.777 7 2.476v6.759z"/></svg>
      </a>
      <!-- YouTube -->
      <a href="https://youtube.com/@himattextile" target="_blank" class="social-svg" title="YouTube">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f172a"><path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/></svg>
      </a>
    </div>
  </div>
</body>
</html>
`
}

/**
 * Builds HTML for Supplier Purchase Copy / Wholesale Order Form (Draft Bill)
 * Matches Android PdfGenerator.generateSupplierCopy
 */
export function generateSupplierInvoiceHtml(data: SupplierInvoiceData): string {
  const { supplier, visit, customer, salesman, entries } = data
  const options = { ...DEFAULT_ORDER_FORM_OPTIONS, ...(data.options || {}) }
  const orderNature = escapeHtml((options.orderNature || "").trim() || ORDER_NATURE_SELF)

  const totalPieces = entries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  const totalAmount = entries.reduce((sum, e) => sum + (Number(e.totalAmount) || 0), 0)
  const totalGst = entries.reduce((sum, e) => sum + (Number(e.gstAmount) || 0), 0)
  const netTotal = entries.reduce(
    (sum, e) => sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )

  const firstEntry = entries[0]
  const orderDate = visit.date || (firstEntry?.createdAt ? new Date(firstEntry.createdAt).toISOString().split("T")[0] : new Date().toISOString().split("T")[0])

  let itemsHtml = ""
  entries.forEach((item, idx) => {
    const srNo = idx + 1
    const orderNo = item.orderNo ? (item.orderNo.startsWith("#") ? item.orderNo : `#${item.orderNo}`) : `#HT-${2620 + idx}`
    const itemCode = item.itemCode || "—"
    const pc = item.pieces || 0
    const rate = item.rate || item.pricePerPiece || 0
    const amt = Number(item.totalAmount) || (pc * rate)
    const packDesc =
      item.loosePieces > 0
        ? `${item.caseCount}c+${item.loosePieces}L`
        : `${item.caseCount} cs`

    itemsHtml += `
      <tr class="item-row">
        <td class="text-center" style="width: 6%; color: #334155;">${srNo}</td>
        <td class="font-medium" style="width: 14%; color: #0f172a;">${orderNo}</td>
        <td class="font-medium" style="width: 26%; color: #0f172a;">${itemCode}</td>
        <td class="text-center font-medium" style="width: 8%; color: #0f172a;">${pc}</td>
        <td class="text-right" style="width: 14%; color: #0f172a;">₹${formatInr(rate)}</td>
        <td class="text-right font-medium" style="width: 16%; color: #0f172a;">₹${formatInr(amt)}</td>
        <td class="text-center" style="width: 16%; color: #334155;">${packDesc}</td>
      </tr>
    `
    if (item.mixedPackNote && item.mixedPackNote.trim()) {
      itemsHtml += `
        <tr class="note-row">
          <td></td>
          <td colspan="6" class="note-text">↳ PACKING INSTRUCTION: ${item.mixedPackNote}</td>
        </tr>
      `
    }
  })

  const suppGstin = supplier.gstin || supplier.gstNumber || "Unregistered"
  // The supplier's own primary number (was falling back to our office number)
  const suppPhone = supplierPrimaryPhone(supplier) || "—"
  const suppPhoneDigits = suppPhone.replace(/[^0-9+]/g, "")
  const suppBrandName = (supplier.brand || "").trim() || supplier.name
  const suppBrand = suppBrandName.toUpperCase()
  const suppFirm = supplier.firmName || supplier.name
  const suppLocation = supplier.marketArea || supplier.marketName || supplier.city || "—"
  const customerId = customer?.customerId || (customer?.id ? `CUST-${customer.id}` : (visit.customerId ? `CUST-${visit.customerId}` : "CUST-TRADE"))
  const buyerCity = customer?.city ? `${customer.city}${customer.state && !customer.city.toLowerCase().includes(customer.state.toLowerCase()) ? `, ${customer.state}` : ""}` : "—"

  // The salesman who booked these orders (orders can belong to a salesman who joined the trip later)
  const orderSalesmen = Array.from(
    new Set(entries.map((e) => (e.salesmanName || "").trim()).filter(Boolean))
  )
  const activeSalesmanName =
    (orderSalesmen.length > 0 ? orderSalesmen.join(", ") : "") || salesman?.name || visit.employeeName || "Jalam Bhai"
  // No email on the supplier's copy: name, contact number and WhatsApp only
  const activeSalesmanPhone = salesman?.phone || "+91 98739 38095"

  // Website QR Code (Clean SVG)
  const websiteQrSvg = generateUpiQrSvg("https://himattextile.com", 76)

  return `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>${escapeHtml(suppBrandName)} - Order Form ${visit.visitCode || ""}</title>
  <style>
    /* Heavier type on the order form (bold / extra bold). Placed first but more specific, so it wins. */
    html body { font-weight: 500; }
    html body .company-address, html body .company-contact { font-weight: 700; color: #334155; }
    html body .meta-line { font-weight: 600; color: #334155; }
    html body .kv-k { font-weight: 600; color: #334155; }
    html body .kv-v { font-weight: 700; }
    html body .kv-v.dest-value { font-size: 11.5px; font-weight: 900; letter-spacing: 0.2px; }
    html body table.data-table td { font-weight: 600; }
    html body .font-medium { font-weight: 700; }
    html body .totals-row { font-weight: 700; }
    html body .note-text { font-weight: 600; }
    html body .note-alert-text { font-weight: 600; font-size: 8px; line-height: 1.45; }
  </style>
  <style>
    @page {
      size: A4 portrait;
      margin: 8mm 10mm;
    }
    * {
      box-sizing: border-box;
      -webkit-print-color-adjust: exact !important;
      print-color-adjust: exact !important;
    }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      margin: 0;
      padding: 0;
      color: #0f172a;
      background: #ffffff;
      font-size: 9.5px;
      line-height: 1.35;
    }

    /* HEADER */
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 10px;
    }
    .header-left {
      display: flex;
      align-items: center;
      gap: 14px;
      flex: 1;
    }
    .company-logo {
      height: 52px;
      width: auto;
      object-fit: contain;
      flex-shrink: 0;
    }
    .company-title {
      font-size: 20px;
      font-weight: 900;
      letter-spacing: 0.5px;
      color: #0f172a;
      margin: 0;
      line-height: 1.1;
    }
    .company-tagline {
      font-size: 8.5px;
      font-weight: 800;
      color: #d97706; /* Golden Yellow */
      letter-spacing: 0.8px;
      margin-top: 3px;
      text-transform: uppercase;
    }
    .company-address {
      font-size: 7.5px;
      color: #64748b;
      margin-top: 2px;
      line-height: 1.25;
    }
    .company-contact {
      font-size: 7.5px;
      color: #64748b;
      margin-top: 1.5px;
    }
    .header-v-divider {
      width: 1px;
      height: 60px;
      background: #e2e8f0;
      margin: 0 18px;
    }
    .header-right {
      text-align: left;
      min-width: 180px;
    }
    .report-title-top {
      font-size: 15px;
      font-weight: 900;
      color: #0f766e;
      letter-spacing: 0.5px;
      line-height: 1.1;
    }
    .report-title-bottom {
      font-size: 15px;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: 0.5px;
      line-height: 1.1;
    }
    .meta-line {
      font-size: 8px;
      color: #475569;
      margin-top: 3px;
      display: flex;
      align-items: center;
      gap: 5px;
    }

    /* DETAILS CARD */
    .details-card {
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 10px 14px;
      margin-top: 8px;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      background: #ffffff;
    }
    .col-header {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 8px;
      font-weight: 800;
      color: #475569;
      letter-spacing: 0.5px;
      text-transform: uppercase;
      margin-bottom: 4px;
    }
    .customer-firm-title {
      font-size: 13.5px;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: 0.3px;
      margin-bottom: 5px;
      text-transform: uppercase;
    }
    .kv-grid {
      display: table;
      width: 100%;
      font-size: 8.5px;
    }
    .kv-row {
      display: table-row;
      line-height: 1.5;
    }
    .kv-k {
      display: table-cell;
      color: #475569;
      width: 95px;
      white-space: nowrap;
    }
    .kv-sep {
      display: table-cell;
      width: 14px;
      text-align: center;
      color: #64748b;
    }
    .kv-v {
      display: table-cell;
      color: #0f172a;
      font-weight: 500;
    }

    /* TABLE */
    .table-container {
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      overflow: hidden;
      margin-top: 10px;
    }
    table.data-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 8.5px;
    }
    table.data-table thead tr {
      background: #f8fafc;
      border-bottom: 1.5px solid #cbd5e1;
    }
    table.data-table th {
      padding: 6px 6px;
      font-weight: 800;
      font-size: 7.5px;
      letter-spacing: 0.4px;
      color: #0f172a;
      border-right: 1px solid #e2e8f0;
      text-align: left;
    }
    table.data-table th:last-child {
      border-right: none;
    }
    table.data-table td {
      padding: 4.5px 6px;
      border-right: 1px solid #e2e8f0;
      border-bottom: 1px solid #e2e8f0;
    }
    table.data-table td:last-child {
      border-right: none;
    }
    table.data-table tr:last-child td {
      border-bottom: none;
    }
    .note-row td {
      padding: 2px 6px 4px 6px;
      background: #fafafa;
    }
    .note-text {
      color: #64748b;
      font-style: italic;
      font-size: 7.5px;
    }
    .text-center { text-align: center; }
    .text-right { text-align: right; }
    .font-bold { font-weight: 700; }
    .font-medium { font-weight: 600; }

    /* TOTALS BOX */
    .totals-wrapper {
      display: flex;
      justify-content: flex-end;
      margin-top: 8px;
    }
    .totals-box {
      width: 280px;
      font-size: 9.5px;
    }
    .totals-row {
      display: flex;
      justify-content: space-between;
      padding: 2.5px 6px;
      color: #334155;
    }
    .grand-total-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #eef2f6;
      border-radius: 4px;
      padding: 5px 8px;
      margin-top: 3px;
    }
    .grand-total-label {
      font-weight: 800;
      font-size: 11px;
      color: #0f172a;
    }
    .grand-total-value {
      font-weight: 900;
      font-size: 12px;
      color: #0f172a;
    }

    /* BOTTOM THREE-COLUMN CARD */
    .bottom-card {
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 10px 12px;
      margin-top: 10px;
      display: grid;
      grid-template-columns: 1.1fr 1.4fr 0.9fr;
      gap: 12px;
      background: #ffffff;
    }
    .bottom-col {
      display: flex;
      flex-direction: column;
    }
    .col-divider {
      border-right: 1px solid #e2e8f0;
      padding-right: 10px;
    }
    .bottom-col-header {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 7.5px;
      font-weight: 800;
      color: #0f172a;
      letter-spacing: 0.4px;
      margin-bottom: 5px;
      text-transform: uppercase;
    }

    /* NOTE ALERT RIBBON */
    .note-alert-banner {
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 4px;
      padding: 5px 10px;
      margin-top: 8px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .note-alert-icon {
      background: #dc2626;
      color: #ffffff;
      width: 14px;
      height: 14px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 8px;
      font-weight: 900;
      flex-shrink: 0;
    }
    .note-alert-text {
      color: #991b1b;
      font-size: 7.5px;
      line-height: 1.3;
    }

    /* FOOTER */
    .doc-footer {
      border-top: 1px solid #e2e8f0;
      padding-top: 6px;
      margin-top: 10px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 7.5px;
    }
    .footer-tagline {
      font-weight: 800;
      color: #d97706; /* Golden Yellow */
      letter-spacing: 0.5px;
    }
    .footer-divider-line {
      flex: 1;
      height: 1px;
      background: #e2e8f0;
      margin: 0 14px;
    }
    .footer-links {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .footer-web-link {
      display: flex;
      align-items: center;
      gap: 4px;
      text-decoration: none;
      color: #0f172a;
      font-weight: 700;
      font-size: 8px;
    }
    .footer-pipe {
      color: #cbd5e1;
      font-weight: 300;
    }
    .social-svg {
      display: inline-flex;
      align-items: center;
      color: #0f172a;
      text-decoration: none;
    }
  </style>
</head>
<body>
  <!-- HEADER -->
  <div class="header">
    <div class="header-left">
      <img src="${HIMAT_LOGO_DATA_URI}" alt="Himat Textile Logo" class="company-logo" />
      <div>
        <h1 class="company-title">HIMAT TEXTILE</h1>
        <div class="company-tagline">YOUR GARMENT GUIDE ACROSS INDIA</div>
        <div class="company-address">First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Ahmedabad, Gujarat 380022</div>
        <div class="company-contact"><strong>GSTIN:</strong> 24EASPS6621D1ZG &nbsp;|&nbsp; <strong>Phone:</strong> <a href="tel:+919873938095" style="color: inherit; text-decoration: none;">+91 98739 38095</a></div>
      </div>
    </div>
    <div class="header-v-divider"></div>
    <div class="header-right">
      <div class="report-title-bottom" style="font-size: 16px; margin-bottom: 4px;">ORDER FORM</div>
      <div class="meta-line">
        <svg width="11" height="11" viewBox="0 0 24 24" fill="#64748b"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg>
        <span><strong>Trip Code:</strong> ${visit.visitCode}</span>
      </div>
      <div class="meta-line">
        <svg width="11" height="11" viewBox="0 0 24 24" fill="#64748b"><path d="M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10z"/></svg>
        <span><strong>Date:</strong> ${orderDate}</span>
      </div>
    </div>
  </div>

  <!-- SUPPLIER & BUYER DETAILS -->
  <div class="details-card">
    <!-- Left: Supplier Details -->
    <div>
      <div class="col-header">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="#0f766e"><path d="M20 4H4v2h16V4zm1 10v-2l-1-5H4l-1 5v2h1v6h10v-6h4v6h2v-6h1zm-9 4H6v-4h6v4z"/></svg>
        <span>SUPPLIER DETAILS</span>
      </div>
      <div class="customer-firm-title">${suppBrand}</div>
      <div class="kv-grid">
        ${suppFirm.toUpperCase() !== suppBrand ? `
        <div class="kv-row">
          <span class="kv-k">Firm Name</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">${suppFirm}</span>
        </div>` : ""}
        <div class="kv-row">
          <span class="kv-k">Phone</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">${suppPhoneDigits.length >= 6 ? `<a href="tel:${suppPhoneDigits}" style="color: inherit; text-decoration: none;">${suppPhone}</a>` : suppPhone}</span>
        </div>
        ${options.showGstin ? `
        <div class="kv-row">
          <span class="kv-k">GSTIN</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">${suppGstin}</span>
        </div>` : ""}
        ${options.showMarketArea ? `
        <div class="kv-row">
          <span class="kv-k">Market Area</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">${suppLocation}</span>
        </div>` : ""}
      </div>
    </div>

    <!-- Right: Customer / Buyer Details (Protected Identity) -->
    <div>
      <div class="col-header">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="#0f766e"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/></svg>
        <span>CUSTOMER</span>
      </div>
      <div class="customer-firm-title">${customerId}</div>
      <div class="kv-grid">
        <div class="kv-row">
          <span class="kv-k">Destination Station</span>
          <span class="kv-sep">:</span>
          <span class="kv-v dest-value">${buyerCity}</span>
        </div>
        <div class="kv-row">
          <span class="kv-k">Dispatch Policy</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">One Bill One LR</span>
        </div>
        <div class="kv-row">
          <span class="kv-k">Order Nature</span>
          <span class="kv-sep">:</span>
          <span class="kv-v">${orderNature}</span>
        </div>
      </div>
    </div>
  </div>

  <!-- ITEMS TABLE -->
  <div class="table-container">
    <table class="data-table">
      <thead>
        <tr>
          <th style="width: 6%; text-align: center;">SR. NO.</th>
          <th style="width: 14%;">ORDER NO.</th>
          <th style="width: 26%;">ITEM / STYLE</th>
          <th style="width: 8%; text-align: center;">PCS</th>
          <th style="width: 14%; text-align: right;">RATE (₹)</th>
          <th style="width: 16%; text-align: right;">AMOUNT (₹)</th>
          <th style="width: 16%; text-align: center;">CASE & PACKING</th>
        </tr>
      </thead>
      <tbody>
        ${itemsHtml}
      </tbody>
    </table>
  </div>

  <!-- TOTALS ROW -->
  <div class="totals-wrapper">
    <div class="totals-box">
      <div class="totals-row">
        <span>Subtotal (${totalPieces} Pcs)</span>
        <span class="font-bold">₹${formatInr(totalAmount)}</span>
      </div>
      <div class="totals-row">
        <span>Approx Garment GST (5%)</span>
        <span class="font-bold">₹${formatInr(totalGst)}</span>
      </div>
      <div class="grand-total-row">
        <span class="grand-total-label">Approx Net Total</span>
        <span class="grand-total-value">₹${formatInr(netTotal)}</span>
      </div>
    </div>
  </div>

  <!-- BOTTOM 3-COLUMN CARD -->
  <div class="bottom-card">
    <!-- Col 1: Salesman Details -->
    <div class="bottom-col col-divider">
      <div class="bottom-col-header">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f766e"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
        <span>SALESMAN DETAILS</span>
      </div>
      <div class="kv-grid">
        <div class="kv-row"><span class="kv-k" style="width: 82px;">Salesman Name</span><span class="kv-sep">:</span><span class="kv-v font-bold">${activeSalesmanName}</span></div>
        <div class="kv-row"><span class="kv-k" style="width: 82px;">Contact Number</span><span class="kv-sep">:</span><span class="kv-v"><a href="tel:${activeSalesmanPhone.replace(/\s+/g, '')}" style="color: inherit; text-decoration: none;">${activeSalesmanPhone}</a></span></div>
        <div class="kv-row"><span class="kv-k" style="width: 82px;">WhatsApp</span><span class="kv-sep">:</span><span class="kv-v"><a href="https://wa.me/${activeSalesmanPhone.replace(/[^0-9]/g, '')}" target="_blank" style="color: inherit; text-decoration: none;">${activeSalesmanPhone}</a></span></div>
      </div>
    </div>

    <!-- Col 2: Order & Dispatch Instructions -->
    <div class="bottom-col col-divider">
      <div class="bottom-col-header">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f766e"><path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
        <span>ORDER & DISPATCH TERMS</span>
      </div>
      <div class="kv-grid">
        <div class="kv-row"><span class="kv-k" style="width: 88px;">One Bill One LR</span><span class="kv-sep">:</span><span class="kv-v font-medium">1 Bill per 1 LR strictly required</span></div>
        <div class="kv-row"><span class="kv-k" style="width: 88px;">Draft Estimate</span><span class="kv-sep">:</span><span class="kv-v">Values subject to final dispatch qty</span></div>
        <div class="kv-row"><span class="kv-k" style="width: 88px;">GST Invoice</span><span class="kv-sep">:</span><span class="kv-v">Original tax invoice with consignment</span></div>
        <div class="kv-row"><span class="kv-k" style="width: 88px;">Packing Note</span><span class="kv-sep">:</span><span class="kv-v">Pack strictly per case instruction</span></div>
      </div>
    </div>

    <!-- Col 3: Scan to Visit Our Website -->
    <div class="bottom-col" style="align-items: center; text-align: center;">
      <div class="bottom-col-header" style="justify-content: center;">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f766e"><path d="M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z"/></svg>
        <span>SCAN TO VISIT OUR WEBSITE</span>
      </div>
      <div style="margin: 2px 0;">
        ${websiteQrSvg}
      </div>
      <div style="font-size: 9px; font-weight: 800; color: #0f172a; letter-spacing: 0.3px;">
        himattextile.com
      </div>
    </div>
  </div>

  <!-- NOTE ALERT RIBBON -->
  <div class="note-alert-banner">
    <div class="note-alert-icon">!</div>
    <div class="note-alert-text">
      <strong>Note:</strong> All amounts shown in this draft order form are approximate and subject to final supplier dispatch and invoice.<br />
      <strong>Dispatch Policy: One Bill One LR is strictly mandatory.</strong>
    </div>
  </div>

  <!-- FOOTER -->
  <div class="doc-footer">
    <div class="footer-tagline">YOUR GARMENT GUIDE ACROSS INDIA</div>
    <div class="footer-divider-line"></div>
    <div class="footer-links">
      <a href="https://himattextile.com" target="_blank" class="footer-web-link">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f172a"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/></svg>
        <span>himattextile.com</span>
      </a>
      <span class="footer-pipe">|</span>
      <!-- Instagram -->
      <a href="https://instagram.com/himattextile" target="_blank" class="social-svg" title="Instagram">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f172a"><path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/></svg>
      </a>
      <!-- Facebook -->
      <a href="https://facebook.com/himattextile" target="_blank" class="social-svg" title="Facebook">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f172a"><path d="M22.675 0h-21.35c-.732 0-1.325.593-1.325 1.325v21.351c0 .731.593 1.324 1.325 1.324h11.495v-9.294h-3.128v-3.622h3.128v-2.671c0-3.1 1.893-4.788 4.659-4.788 1.325 0 2.463.099 2.795.143v3.24l-1.918.001c-1.504 0-1.795.715-1.795 1.763v2.313h3.587l-.467 3.622h-3.12v9.293h6.116c.73 0 1.323-.593 1.323-1.325v-21.35c0-.732-.593-1.325-1.325-1.325z"/></svg>
      </a>
      <!-- LinkedIn -->
      <a href="https://linkedin.com/company/himattextile" target="_blank" class="social-svg" title="LinkedIn">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f172a"><path d="M19 0h-14c-2.761 0-5 2.239-5 5v14c0 2.761 2.239 5 5 5h14c2.762 0 5-2.239 5-5v-14c0-2.761-2.238-5-5-5zm-11 19h-3v-11h3v11zm-1.5-12.268c-.966 0-1.75-.79-1.75-1.764s.784-1.764 1.75-1.764 1.75.79 1.75 1.764-.783 1.764-1.75 1.764zm13.5 12.268h-3v-5.604c0-3.368-4-3.113-4 0v5.604h-3v-11h3v1.765c1.396-2.586 7-2.777 7 2.476v6.759z"/></svg>
      </a>
      <!-- YouTube -->
      <a href="https://youtube.com/@himattextile" target="_blank" class="social-svg" title="YouTube">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="#0f172a"><path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/></svg>
      </a>
    </div>
  </div>
</body>
</html>
`
}

/**
 * Triggers browser print/PDF generation using an invisible or popup iframe/window
 */
export function printReportHtml(title: string, htmlContent: string) {
  const printWindow = window.open("", "_blank", "width=900,height=800")
  if (!printWindow) {
    alert("Please allow popups to preview and print reports.")
    return
  }
  printWindow.document.open()
  printWindow.document.write(htmlContent)
  printWindow.document.close()

  // Wait for resources to load, then trigger print
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
  }, 350)
}

/**
 * WhatsApp message generator for Customer Purchase Report
 */
export function buildCustomerReportWhatsAppText(data: CustomerReportData): string {
  const { visit, customer, salesman, entries } = data
  const totalPieces = entries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  const totalCases = entries.reduce((sum, e) => sum + (Number(e.caseCount) || 0), 0)
  const grandTotal = entries.reduce(
    (sum, e) => sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )
  const totalPaid = entries.reduce((sum, e) => sum + (Number(e.paidAmount) || 0), 0)
  const totalDue = Math.max(0, grandTotal - totalPaid)
  const custBrand = (customer?.firmName || customer?.name || visit.customerName).toUpperCase()
  const primaryTransporter = entries.find((e) => e.transporter?.trim())?.transporter || customer?.transportName || "Direct Dispatch"

  let text = `*HIMAT TEXTILE — YOUR GARMENT GUIDE ACROSS INDIA*\n`
  text += `📋 *CUSTOMER PURCHASE REPORT*\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `🏢 *Buyer:* ${custBrand}\n`
  if (customer?.name && customer?.firmName && customer.name.toLowerCase() !== customer.firmName.toLowerCase()) {
    text += `👤 *Contact:* ${customer.name}\n`
  }
  text += `📍 *City / Station:* ${customer?.city || "Ahmedabad"}\n`
  text += `🚛 *Transporter:* *${primaryTransporter}*\n`
  text += `📋 *Trip:* ${visit.visitCode} | 📅 ${visit.date}\n`
  // Every salesman who booked an order or joined the trip (same rule as the Android report)
  const reportSalesmen = salesmenForReport(visit, entries)
  text += `👔 *Salesmen:* ${reportSalesmen.length > 0 ? reportSalesmen.join(", ") : salesman?.name || visit.employeeName}\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `*PURCHASE ORDERS SUMMARY:*\n`

  entries.forEach((item, idx) => {
    const rate = item.rate || item.pricePerPiece || 0
    const amt = Number(item.totalAmount) || 0
    const pack = item.loosePieces > 0 ? `${item.caseCount}c+${item.loosePieces}L` : `${item.caseCount} cs`
    const emp = item.salesmanName || visit.employeeName
    const status = item.deliveryStatus || "Pending"
    text += `${idx + 1}. *${item.itemCode}* — ${item.supplierName}\n`
    text += `   Qty: ${item.pieces} pcs (${pack}) @ ₹${rate} = *₹${formatInr(amt)}*\n`
    text += `   Status: *${status}* | Salesman: ${emp} | Date: ${item.orderDate || visit.date}\n`
    if (item.mixedPackNote) {
      text += `   ↳ Note: ${item.mixedPackNote}\n`
    }
  })

  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `📦 *Total Qty:* ${totalPieces} Pcs (${totalCases} Cases)\n`
  text += `💰 *Grand Total:* ₹${formatInr(grandTotal)}\n`
  text += `✅ *Paid:* ₹${formatInr(totalPaid)}\n`
  text += `⚠️ *Balance Due:* ₹${formatInr(totalDue)}\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `🏦 *BANK PAYMENT DETAILS:*\n`
  text += `Bank: ICICI Bank (Ashram Road Ahmedabad)\n`
  text += `A/C Name: HIMAT TEXTILE\n`
  text += `A/C No: 136805501447 | Type: Current\n`
  text += `IFSC: ICIC0000189\n`
  text += `UPI ID: eazypay.0000053310@icici\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `🌐 *Website:* https://himattextile.com\n`
  text += `_Himat Textile — Your Garment Guide Across India_`

  return text
}

/**
 * WhatsApp message generator for Supplier Wholesale Order Form (Draft Bill)
 */
export function buildSupplierInvoiceWhatsAppText(data: SupplierInvoiceData): string {
  const { supplier, visit, customer, entries } = data
  const totalPieces = entries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  const totalCases = entries.reduce((sum, e) => sum + (Number(e.caseCount) || 0), 0)
  const netTotal = entries.reduce(
    (sum, e) => sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )
  const suppBrand = (supplier.brand || supplier.name).toUpperCase()
  const customerId = customer?.customerId || (customer?.id ? `CUST-${customer.id}` : (visit.customerId ? `CUST-${visit.customerId}` : "CUST-TRADE"))
  const buyerCity = customer?.city || customer?.district || customer?.state || "Ahmedabad"

  let text = `*HIMAT TEXTILE — YOUR GARMENT GUIDE ACROSS INDIA*\n`
  text += `📝 *ORDER FORM (DRAFT BILL)*\n`
  text += `⚠️ _All amounts are approximate • One Bill One LR_\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `🏭 *Supplier Brand:* *${suppBrand}*\n`
  if (supplier.firmName && supplier.brand && supplier.firmName.toLowerCase() !== supplier.brand.toLowerCase()) {
    text += `🏢 *Firm:* ${supplier.firmName}\n`
  }
  text += `🆔 *Customer ID:* *${customerId}* (${buyerCity})\n`
  text += `📅 *Date:* ${visit.date} | *Trip:* ${visit.visitCode}\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `*ORDER ITEMS:*\n`

  entries.forEach((item, idx) => {
    const rate = item.rate || item.pricePerPiece || 0
    const pack =
      item.loosePieces > 0
        ? `${item.caseCount}c + ${item.loosePieces}L`
        : `${item.caseCount} cases`
    text += `${idx + 1}. *${item.itemCode}* — ${item.pieces} pcs (${pack})\n`
    text += `   Approx Rate: ₹${rate} | Approx Amt: ₹${formatInr(Number(item.totalAmount) || 0)}\n`
    if (item.mixedPackNote) {
      text += `   ↳ Note: ${item.mixedPackNote}\n`
    }
  })

  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `📦 *Total Qty:* ${totalPieces} Pcs (${totalCases} Cases)\n`
  text += `💰 *Approx Net Total:* ₹${formatInr(netTotal)}\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `📌 *Note:* One Bill One LR. All amounts are approximate estimates.\n`
  text += `🌐 *Website:* https://himattextile.com\n`
  text += `_Himat Textile — Your Garment Guide Across India_`

  return text
}

export interface CustomerStatementData {
  customer: Customer
  entries: PurchaseEntry[]
  visits: Visit[]
  startDate?: string
  endDate?: string
  dateRangeLabel?: string
}

/**
 * Builds HTML for Customer Consolidated Account Ledger & Statement (PDF)
 * Shows all orders, total invoiced, payments realized, and outstanding balance within a date range
 */
export function generateCustomerStatementHtml(data: CustomerStatementData): string {
  const { customer, entries, visits, startDate, endDate, dateRangeLabel } = data

  const visitMap = new Map<number, Visit>(visits.map((v) => [v.id, v]))

  // Sort entries chronologically (oldest to newest or newest first)
  const sortedEntries = [...entries].sort((a, b) => {
    const vA = visitMap.get(a.visitId)
    const vB = visitMap.get(b.visitId)
    const dateA = vA?.date ? new Date(vA.date).getTime() : a.createdAt || 0
    const dateB = vB?.date ? new Date(vB.date).getTime() : b.createdAt || 0
    return dateA - dateB
  })

  const totalPieces = entries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  const totalCases = entries.reduce((sum, e) => sum + (Number(e.caseCount) || 0), 0)
  const totalLoose = entries.reduce((sum, e) => sum + (Number(e.loosePieces) || 0), 0)
  const totalTaxable = entries.reduce((sum, e) => sum + (Number(e.totalAmount) || 0), 0)
  const totalGst = entries.reduce((sum, e) => sum + (Number(e.gstAmount) || 0), 0)
  const totalBilled = entries.reduce(
    (sum, e) =>
      sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )
  const totalPaid = entries.reduce((sum, e) => sum + (Number(e.paidAmount) || 0), 0)
  const totalDue = Math.max(0, totalBilled - totalPaid)
  const recoveryPct = totalBilled > 0 ? Math.round((totalPaid / totalBilled) * 100) : 100

  let periodString = dateRangeLabel || "All Records"
  if (startDate && endDate) {
    periodString = `${startDate} to ${endDate}`
  } else if (startDate) {
    periodString = `From ${startDate}`
  } else if (endDate) {
    periodString = `Up to ${endDate}`
  }

  // Same facts the trip report carries: the brand (shop) name, the owner, the booking station and
  // the Sub Agent who looks after this customer.
  const statementBrand = (customer.firmName || "").trim() || customer.name || "Customer"
  const statementOwner = (customer.name || "").trim()
  const stationCity = (customer.city || "").trim()
  const stationText = stationCity ? `${stationCity} (${stationCity.substring(0, 3).toUpperCase()})` : "—"
  const statementSubAgent = (customer.subAgentName || "").trim()
  const statementSalesmen = Array.from(
    new Set(entries.map((e) => (e.salesmanName || "").trim()).filter(Boolean))
  )

  let tableRowsHtml = ""
  if (sortedEntries.length === 0) {
    tableRowsHtml = `
      <tr>
        <td colspan="9" class="text-center" style="padding: 24px; color: #64748b;">
          No transactions or purchases recorded for this customer in the selected date range.
        </td>
      </tr>
    `
  } else {
    sortedEntries.forEach((entry, idx) => {
      const v = visitMap.get(entry.visitId)
      const orderDate = v?.date || (entry.createdAt ? new Date(entry.createdAt).toISOString().split("T")[0] : "—")
      const bill =
        Number(entry.grandTotalWithGst) ||
        (Number(entry.totalAmount) + Number(entry.gstAmount)) ||
        0
      const paid = Number(entry.paidAmount) || 0
      const due = Math.max(0, bill - paid)
      const isPaid =
        entry.paymentStatus?.toLowerCase() === "paid" ||
        entry.paymentStatus?.toLowerCase() === "received"
      const isPartial = entry.paymentStatus?.toLowerCase() === "partial" || (paid > 0 && due > 0)
      const rate = entry.rate || entry.pricePerPiece || 0
      const packDesc =
        entry.loosePieces > 0
          ? `${entry.caseCount}c + ${entry.loosePieces}L`
          : `${entry.caseCount} cs`
      // The salesman credited with this one order, with the customer's Sub Agent under it
      const rowSalesman =
        (entry.salesmanName || "").trim() || (entry.createdByName || "").trim() || v?.employeeName || "—"
      const rowSubAgent = statementSubAgent

      tableRowsHtml += `
        <tr class="item-row">
          <td class="mono font-bold" style="white-space: nowrap;">${orderDate}</td>
          <td class="mono font-bold">#${entry.orderNo}</td>
          <td>
            <div class="font-bold text-zinc-900">${entry.supplierName}</div>
            <div style="font-size: 8.5px; color: #64748b;">${entry.supplierType || "Wholesaler"}</div>
          </td>
          <td>
            <span class="font-bold">${entry.itemCode}</span>
            <span style="font-size: 8.5px; color: #64748b;">(${entry.pieces} pcs • ${packDesc} @ ₹${rate})</span>
          </td>
          <td>
            <div class="font-bold text-zinc-900">${escapeHtml(rowSalesman)}</div>
            ${rowSubAgent ? `<div style="font-size: 8.5px; color: #64748b;">Sub Agent: ${escapeHtml(rowSubAgent)}</div>` : ""}
          </td>
          <td class="text-right font-bold">₹${formatInr(bill)}</td>
          <td class="text-right font-bold" style="color: #16a34a;">₹${formatInr(paid)}</td>
          <td class="text-right font-bold" style="color: ${due > 0 ? "#dc2626" : "#16a34a"};">
            ₹${formatInr(due)}
          </td>
          <td>
            <span class="status-badge ${isPaid ? "badge-paid" : isPartial ? "badge-partial" : "badge-unpaid"}">
              ${entry.paymentStatus || (paid > 0 ? "Partial" : "Unpaid")}
            </span>
            ${
              entry.paymentMode
                ? `<div style="font-size: 8.5px; color: #64748b; margin-top: 2px;">${entry.paymentMode}</div>`
                : ""
            }
          </td>
        </tr>
      `
    })
  }

  const customerGstin = customer.gstin || customer.gstNumber || "Unregistered / Consumer"
  const customerPhones = [customer.phone, customer.phone2, customer.phone3, customer.phone4, customer.phone5]
    .filter(Boolean)
    .join(", ")
  const customerCity = customer.city || customer.marketArea || "Ahmedabad"
  const creditDays = customer.creditDays ? `${customer.creditDays} Days` : "Standard (30 Days)"

  return `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>${escapeHtml((customer.firmName || "").trim() || customer.name || "Customer")} - Statement</title>
  <style>
    @page {
      size: A4 portrait;
      margin: 10mm 12mm;
    }
    * {
      box-sizing: border-box;
      -webkit-print-color-adjust: exact !important;
      print-color-adjust: exact !important;
    }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      margin: 0;
      padding: 0;
      color: #0f172a;
      background: #ffffff;
      font-size: 10.5px;
      line-height: 1.35;
    }
    .header-banner {
      background: #0f172a;
      color: #ffffff;
      padding: 14px 18px;
      border-radius: 6px 6px 0 0;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }
    .company-title {
      font-size: 19px;
      font-weight: 800;
      letter-spacing: 0.5px;
      color: #ffffff;
      margin: 0;
    }
    .company-sub {
      color: #cbd5e1;
      font-size: 9.5px;
      font-weight: 700;
      letter-spacing: 0.5px;
      margin-top: 2px;
    }
    .company-desc {
      color: #94a3b8;
      font-size: 8.5px;
      margin-top: 2px;
      line-height: 1.3;
    }
    .header-right {
      text-align: right;
    }
    .doc-type {
      font-size: 13.5px;
      font-weight: 800;
      color: #ffffff;
      letter-spacing: 0.5px;
    }
    .doc-meta {
      color: #cbd5e1;
      font-size: 9.5px;
      margin-top: 3px;
    }
    .info-container {
      background: #f8fafc;
      border: 1px solid #cbd5e1;
      border-top: none;
      padding: 10px 16px;
      display: grid;
      grid-template-columns: 1.4fr 1fr;
      gap: 16px;
    }
    .info-col h4 {
      margin: 0 0 4px 0;
      font-size: 9.5px;
      color: #475569;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 700;
    }
    .info-title {
      font-size: 12.5px;
      font-weight: 700;
      color: #0f172a;
    }
    .info-sub {
      font-size: 9.5px;
      color: #334155;
      margin-top: 2px;
    }
    table.data-table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 12px;
      font-size: 10px;
      border: 1px solid #94a3b8;
    }
    table.data-table thead tr {
      background: #0f172a;
      color: #ffffff;
    }
    table.data-table th {
      padding: 6px 7px;
      font-weight: 700;
      font-size: 9px;
      letter-spacing: 0.3px;
      border: 1px solid #475569;
      text-align: left;
    }
    table.data-table td {
      padding: 6px 7px;
      border: 1px solid #cbd5e1;
    }
    .status-badge {
      display: inline-block;
      padding: 2px 6px;
      border-radius: 9999px;
      font-size: 8.5px;
      font-weight: 700;
      text-transform: uppercase;
    }
    .badge-paid {
      background: #dcfce7;
      color: #15803d;
    }
    .badge-partial {
      background: #fef3c7;
      color: #b45309;
    }
    .badge-unpaid {
      background: #fee2e2;
      color: #b91c1c;
    }
    .text-center { text-align: center; }
    .text-right { text-align: right; }
    .font-bold { font-weight: 700; }
    .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }

    .bottom-section {
      margin-top: 14px;
      display: grid;
      grid-template-columns: 1.1fr 1fr;
      gap: 16px;
    }
    .summary-box {
      background: #f8fafc;
      border: 1px solid #cbd5e1;
      border-radius: 4px;
      overflow: hidden;
    }
    .summary-content {
      padding: 8px 12px;
    }
    .summary-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 4px;
      font-size: 10px;
    }
    .summary-row.label-sub {
      color: #475569;
    }
    .summary-banner {
      background: #0f172a;
      color: #ffffff;
      padding: 7px 12px;
      display: flex;
      justify-content: space-between;
      font-weight: 700;
      font-size: 11px;
    }
    .due-alert-box {
      margin-top: 6px;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 4px;
      padding: 8px 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 700;
    }
    .terms-box {
      font-size: 8.5px;
      color: #475569;
      line-height: 1.45;
    }
    .terms-box ul {
      margin: 3px 0;
      padding-left: 14px;
    }
    .signatures-box {
      margin-top: 36px;
      display: flex;
      justify-content: space-between;
      padding: 0 30px;
    }
    .sig-line {
      width: 180px;
      text-align: center;
      border-top: 1.5px solid #0f172a;
      padding-top: 5px;
      font-weight: 700;
      font-size: 10px;
    }
  </style>
</head>
<body>
  <div class="header-banner">
    <div style="display: flex; align-items: center; gap: 14px;">
      <img src="${HIMAT_LOGO_DATA_URI}" alt="Himat Textile Logo" style="height: 52px; width: auto; object-fit: contain; flex-shrink: 0;" />
      <div>
        <h1 class="company-title">HIMAT TEXTILE</h1>
        <div class="company-sub">YOUR BUSINESS GUIDE ACROSS INDIA</div>
        <div class="company-desc">First Floor, Hira Bhai 21, Dayanand Rd, Sarangpur, Sherkotda, Ahmedabad, Gujarat 380022</div>
        <div class="company-desc"><strong>GSTIN:</strong> 24EASPS6621D1ZG &bull; <strong>Phone:</strong> +91 98739 38095</div>
      </div>
    </div>
    <div class="header-right">
      <div class="doc-type">CUSTOMER STATEMENT</div>
      <div class="doc-meta"><strong>Period:</strong> ${periodString}</div>
      <div class="doc-meta"><strong>Statement Date:</strong> ${new Date().toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" })}</div>
      <div class="doc-meta"><strong>Orders Count:</strong> ${entries.length} Orders</div>
    </div>
  </div>

  <div class="info-container">
    <div class="info-col">
      <h4>Customer / Shop Details:</h4>
      <div class="info-title">${escapeHtml(statementBrand)}</div>
      ${statementOwner && statementOwner !== statementBrand ? `<div class="info-sub">Proprietor: <strong>${escapeHtml(statementOwner)}</strong></div>` : ""}
      <div class="info-sub">Booking Station: <strong>${escapeHtml(stationText)}</strong></div>
      <div class="info-sub">${escapeHtml(customer.shopAddress || customer.address || customerCity)}</div>
      <div class="info-sub">Phone(s): ${escapeHtml(customerPhones || "—")}</div>
      <div class="info-sub">GSTIN: ${escapeHtml(customerGstin)} • Markets: ${escapeHtml(customer.markets || customer.marketArea || "Ahmedabad Wholesale")}</div>
      ${statementSubAgent ? `<div class="info-sub">Sub Agent: <strong>${escapeHtml(statementSubAgent)}</strong></div>` : ""}
    </div>
    <div class="info-col">
      <h4>Account Standing:</h4>
      <div class="info-sub">${statementSalesmen.length > 1 ? "Salesmen" : "Salesman"}: <strong>${escapeHtml(statementSalesmen.join(", ") || "—")}</strong></div>
      <div class="info-sub">Payment Terms: <strong>${creditDays}</strong></div>
      <div class="info-sub">Credit Limit: <strong>${customer.creditLimit ? `₹${formatInr(customer.creditLimit)}` : "Discretionary"}</strong></div>
      <div class="info-sub">Recovery Rate: <strong>${recoveryPct}%</strong></div>
      <div class="info-sub" style="color: ${totalDue > 0 ? "#dc2626" : "#16a34a"}; font-weight: 700;">
        Current Net Outstanding: ₹${formatInr(totalDue)}
      </div>
    </div>
  </div>

  <table class="data-table">
    <thead>
      <tr>
        <th style="width: 10%;">DATE</th>
        <th style="width: 9%;">ORDER #</th>
        <th style="width: 20%;">SUPPLIER / MILL</th>
        <th style="width: 19%;">ITEM & PACKING</th>
        <th style="width: 10%;">SALESMAN</th>
        <th style="width: 11%; text-align: right;">BILL (₹)</th>
        <th style="width: 11%; text-align: right;">PAID (₹)</th>
        <th style="width: 11%; text-align: right;">DUE (₹)</th>
        <th style="width: 11%;">STATUS</th>
      </tr>
    </thead>
    <tbody>
      ${tableRowsHtml}
    </tbody>
  </table>

  <div class="bottom-section">
    <div>
      <div class="terms-box">
        <strong>Statement Terms & Notes:</strong>
        <ul>
          <li>This statement reflects all purchases booked via Himat Textile — Your Business Guide Across India for ${customer.firmName || customer.name} during the stated period.</li>
          <li>Payments received via Cash, Cheque, NEFT/RTGS, or UPI are updated upon bank/supplier realization.</li>
          <li>For any billing discrepancy or payment reconciliation, please notify our accounts desk within 7 working days.</li>
        </ul>
      </div>
    </div>

    <div>
      <div class="summary-box">
        <div class="summary-content">
          <div class="summary-row">
            <span class="label-sub">Total Items Booked:</span>
            <span class="font-bold">${entries.length} Orders (${totalPieces} Pcs)</span>
          </div>
          <div class="summary-row">
            <span class="label-sub">Packing Total:</span>
            <span class="font-bold">${totalCases} Full Cases + ${totalLoose} Loose</span>
          </div>
          <div class="summary-row">
            <span class="label-sub">Taxable Goods Value:</span>
            <span class="font-bold">₹${formatInr(totalTaxable)}</span>
          </div>
          <div class="summary-row">
            <span class="label-sub">Garment GST (5%):</span>
            <span class="font-bold">₹${formatInr(totalGst)}</span>
          </div>
          <div class="summary-row" style="border-top: 1px solid #e2e8f0; padding-top: 4px;">
            <span class="label-sub">Total Invoiced:</span>
            <span class="font-bold">₹${formatInr(totalBilled)}</span>
          </div>
          <div class="summary-row">
            <span class="label-sub">Payments Realized:</span>
            <span class="font-bold" style="color: #16a34a;">₹${formatInr(totalPaid)}</span>
          </div>
        </div>
        <div class="summary-banner">
          <span>NET DUE BALANCE:</span>
          <span style="font-size: 13px;">₹${formatInr(totalDue)}</span>
        </div>
      </div>
    </div>
  </div>

  <div class="signatures-box">
    <div class="sig-line">
      Customer Verification & Stamp
    </div>
    <div class="sig-line">
      For HIMAT TEXTILE (Accounts Desk)
    </div>
  </div>
</body>
</html>
`
}

/**
 * WhatsApp message generator for Customer Account Statement
 */
export function buildCustomerStatementWhatsAppText(data: CustomerStatementData): string {
  const { customer, entries, visits, startDate, endDate, dateRangeLabel } = data

  const visitMap = new Map<number, Visit>(visits.map((v) => [v.id, v]))
  const totalPieces = entries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  const totalBilled = entries.reduce(
    (sum, e) =>
      sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )
  const totalPaid = entries.reduce((sum, e) => sum + (Number(e.paidAmount) || 0), 0)
  const totalDue = Math.max(0, totalBilled - totalPaid)

  let periodStr = dateRangeLabel || "All Records"
  if (startDate && endDate) {
    periodStr = `${startDate} to ${endDate}`
  } else if (startDate) {
    periodStr = `From ${startDate}`
  }

  let text = `*HIMAT TEXTILE — YOUR BUSINESS GUIDE ACROSS INDIA*\n`
  text += `📑 *CUSTOMER ACCOUNT STATEMENT*\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `👤 *Client:* ${customer.firmName || customer.name}\n`
  text += `📅 *Period:* ${periodStr}\n`
  text += `📑 *Total Orders:* ${entries.length} (${totalPieces} pcs)\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `*PURCHASE & BILLING BREAKDOWN:*\n`

  entries.forEach((item, idx) => {
    const v = visitMap.get(item.visitId)
    const d = v?.date || ""
    const bill =
      Number(item.grandTotalWithGst) ||
      (Number(item.totalAmount) + Number(item.gstAmount)) ||
      0
    const paid = Number(item.paidAmount) || 0
    const due = Math.max(0, bill - paid)

    text += `${idx + 1}. *#${item.orderNo}* • ${item.supplierName} (${item.itemCode})\n`
    text += `   Bill: ₹${formatInr(bill)} | Paid: ₹${formatInr(paid)} | Due: ₹${formatInr(due)}\n`
  })

  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `💰 *Total Invoiced:* ₹${formatInr(totalBilled)}\n`
  text += `✅ *Total Paid:* ₹${formatInr(totalPaid)}\n`
  text += `⚠️ *Net Due Balance:* ₹${formatInr(totalDue)}\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `_Please clear outstanding dues at earliest convenience._\n`
  text += `_Himat Textile — Your Business Guide Across India_`

  return text
}

