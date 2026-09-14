import { Customer, Employee, PackGroup, PurchaseEntry, Supplier, Visit } from "../types"
import { formatInr } from "./utils"

export interface CustomerReportData {
  visit: Visit
  customer?: Customer | null
  salesman?: Employee | null
  entries: PurchaseEntry[]
  packGroups?: PackGroup[]
}

export interface SupplierInvoiceData {
  supplier: Supplier
  visit: Visit
  customer?: Customer | null
  salesman?: Employee | null
  entries: PurchaseEntry[]
}

/**
 * Builds HTML for Customer Consolidated Day Report
 * Matches Android PdfGenerator.generateCustomerDayReport
 */
export function generateCustomerDayReportHtml(data: CustomerReportData): string {
  const { visit, customer, salesman, entries, packGroups = [] } = data

  const totalPieces = entries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  const totalAmount = entries.reduce((sum, e) => sum + (Number(e.totalAmount) || 0), 0)
  const totalGst = entries.reduce((sum, e) => sum + (Number(e.gstAmount) || 0), 0)
  const grandTotal = entries.reduce(
    (sum, e) => sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )
  const totalPaid = entries.reduce((sum, e) => sum + (Number(e.paidAmount) || 0), 0)
  const totalDue = Math.max(0, grandTotal - totalPaid)
  const totalCases = entries.reduce((sum, e) => sum + (Number(e.caseCount) || 0), 0)
  const totalLoose = entries.reduce((sum, e) => sum + (Number(e.loosePieces) || 0), 0)

  // Group entries by supplier
  const supplierMap = new Map<string, PurchaseEntry[]>()
  entries.forEach((e) => {
    const sName = e.supplierName || "Unknown Supplier"
    const list = supplierMap.get(sName) || []
    list.push(e)
    supplierMap.set(sName, list)
  })

  let supplierRowsHtml = ""
  supplierMap.forEach((supEntries, supName) => {
    const supType = supEntries[0]?.supplierType || "Wholesaler"
    supplierRowsHtml += `
      <tr class="supplier-header-row">
        <td colspan="7">
          <span class="supplier-tag">▶ ${supName}</span>
          <span class="supplier-type-badge">(${supType})</span>
        </td>
      </tr>
    `
    supEntries.forEach((item) => {
      const packDesc =
        item.loosePieces > 0
          ? `${item.caseCount}c + ${item.loosePieces}L`
          : `${item.caseCount} cases`
      const rate = item.rate || item.pricePerPiece || 0
      const itemAmount = Number(item.totalAmount) || 0

      supplierRowsHtml += `
        <tr class="item-row">
          <td class="mono font-bold">#${item.orderNo}</td>
          <td>${item.supplierName}</td>
          <td class="font-bold">${item.itemCode}</td>
          <td class="text-center font-bold">${item.pieces}</td>
          <td class="text-right">₹${formatInr(rate)}</td>
          <td class="text-center">${packDesc}</td>
          <td class="text-right font-bold">₹${formatInr(itemAmount)}</td>
        </tr>
      `
      if (item.mixedPackNote && item.mixedPackNote.trim()) {
        supplierRowsHtml += `
          <tr class="note-row">
            <td></td>
            <td colspan="6" class="note-text">↳ NOTE: ${item.mixedPackNote}</td>
          </tr>
        `
      }
    })
  })

  let packGroupsHtml = ""
  if (packGroups.length > 0) {
    packGroupsHtml = `
      <div class="mixed-pack-box">
        <h4>Mixed Packing Group Summary</h4>
        ${packGroups
          .map(
            (pg) => `
          <div class="pg-item">
            <strong>${pg.packGroupCode || "PACK"}:</strong> ${pg.note || "Combined loose cartons"}
          </div>
        `
          )
          .join("")}
      </div>
    `
  }

  const customerGstin = customer?.gstin || customer?.gstNumber || "Unregistered / Consumer"
  const customerPhone = customer?.phone || "—"
  const customerCity = customer?.city || customer?.marketArea || "—"

  return `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Customer Day Report - ${visit.visitCode}</title>
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
      color: #141923;
      background: #ffffff;
      font-size: 11px;
      line-height: 1.4;
    }
    .header-banner {
      background: #132338;
      color: #ffffff;
      padding: 16px 20px;
      border-radius: 8px 8px 0 0;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }
    .company-title {
      font-size: 20px;
      font-weight: 800;
      letter-spacing: 0.5px;
      color: #ffffff;
      margin: 0;
    }
    .company-sub {
      color: #c89d3c;
      font-size: 10px;
      font-weight: 700;
      letter-spacing: 0.5px;
      margin-top: 3px;
    }
    .company-desc {
      color: #c8d2e1;
      font-size: 9px;
      margin-top: 3px;
    }
    .header-right {
      text-align: right;
    }
    .doc-type {
      font-size: 14px;
      font-weight: 800;
      color: #ffffff;
      letter-spacing: 0.5px;
    }
    .doc-meta {
      color: #dce6f5;
      font-size: 10px;
      margin-top: 4px;
    }
    .info-container {
      background: #f8fafc;
      border: 1px solid #dce2ea;
      border-top: none;
      padding: 12px 18px;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }
    .info-col h4 {
      margin: 0 0 4px 0;
      font-size: 10px;
      color: #64707d;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 700;
    }
    .info-title {
      font-size: 13px;
      font-weight: 700;
      color: #141923;
    }
    .info-sub {
      font-size: 10px;
      color: #4b5563;
      margin-top: 2px;
    }
    table.data-table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 14px;
      font-size: 10.5px;
    }
    table.data-table thead tr {
      background: #132338;
      color: #ffffff;
    }
    table.data-table th {
      padding: 7px 10px;
      font-weight: 700;
      font-size: 9.5px;
      letter-spacing: 0.3px;
    }
    table.data-table td {
      padding: 7px 10px;
      border-bottom: 1px solid #e2e8f0;
    }
    .supplier-header-row td {
      background: #eef2f8;
      color: #132338;
      font-weight: 700;
      padding: 6px 10px;
      font-size: 10.5px;
      border-top: 1px solid #cbd5e1;
      border-bottom: 1px solid #cbd5e1;
    }
    .supplier-type-badge {
      font-size: 9.5px;
      font-weight: normal;
      color: #475569;
      margin-left: 6px;
    }
    .note-row td {
      padding: 2px 10px 6px 10px;
      border-bottom: 1px solid #e2e8f0;
    }
    .note-text {
      color: #b45309;
      font-style: italic;
      font-size: 9.5px;
    }
    .text-center { text-align: center; }
    .text-right { text-align: right; }
    .font-bold { font-weight: 700; }
    .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }

    .bottom-section {
      margin-top: 14px;
      display: grid;
      grid-template-columns: 1.2fr 1fr;
      gap: 16px;
    }
    .summary-box {
      background: #f8fafc;
      border: 1px solid #dce2ea;
      border-radius: 6px;
      overflow: hidden;
    }
    .summary-content {
      padding: 10px 14px;
    }
    .summary-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 5px;
      font-size: 10.5px;
    }
    .summary-row.label-sub {
      color: #64707d;
    }
    .summary-banner {
      background: #132338;
      color: #ffffff;
      padding: 8px 14px;
      display: flex;
      justify-content: space-between;
      font-weight: 700;
      font-size: 12px;
    }
    .payment-status-box {
      margin-top: 8px;
      background: #ffffff;
      border: 1px dashed #cbd5e1;
      border-radius: 4px;
      padding: 8px 12px;
      font-size: 10px;
    }
    .payment-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 3px;
    }
    .mixed-pack-box {
      background: #fef3c7;
      border: 1px solid #fde68a;
      border-radius: 6px;
      padding: 8px 12px;
      margin-bottom: 10px;
    }
    .mixed-pack-box h4 {
      margin: 0 0 4px 0;
      font-size: 10.5px;
      color: #92400e;
    }
    .pg-item {
      font-size: 10px;
      color: #92400e;
      margin-bottom: 2px;
    }
    .terms-box {
      font-size: 9px;
      color: #64707d;
      line-height: 1.5;
    }
    .terms-box ul {
      margin: 4px 0;
      padding-left: 16px;
    }
    .signatures-box {
      margin-top: 40px;
      display: flex;
      justify-content: space-between;
      padding: 0 30px;
    }
    .sig-line {
      width: 180px;
      text-align: center;
      border-top: 1.5px solid #141923;
      padding-top: 6px;
      font-weight: 700;
      font-size: 10.5px;
    }
  </style>
</head>
<body>
  <div class="header-banner">
    <div>
      <h1 class="company-title">HIMAT TEXTILE</h1>
      <div class="company-sub">GARMENT SOURCING AGENCY • WHOLESALE TO RETAIL FACILITATOR</div>
      <div class="company-desc">Market Escort & Spot Procurement Logs • Multi-Supplier Consolidated Billing</div>
    </div>
    <div class="header-right">
      <div class="doc-type">CUSTOMER DAY REPORT</div>
      <div class="doc-meta"><strong>Trip Code:</strong> ${visit.visitCode}</div>
      <div class="doc-meta"><strong>Date:</strong> ${visit.date}</div>
      <div class="doc-meta"><strong>Status:</strong> ${visit.status || "Active"}</div>
    </div>
  </div>

  <div class="info-container">
    <div class="info-col">
      <h4>Customer / Retailer Details:</h4>
      <div class="info-title">${customer?.name || visit.customerName}</div>
      <div class="info-sub">${customer?.firmName ? `${customer.firmName} • ` : ""}${customerCity}</div>
      <div class="info-sub">Phone: ${customerPhone} • GSTIN: ${customerGstin}</div>
    </div>
    <div class="info-col">
      <h4>Field Agent / Salesman:</h4>
      <div class="info-title">${salesman?.name || visit.employeeName}</div>
      <div class="info-sub">Role: ${salesman?.role || "Field Sales Representative"}</div>
      <div class="info-sub">Phone: ${salesman?.phone || "—"}</div>
    </div>
  </div>

  <table class="data-table">
    <thead>
      <tr>
        <th style="width: 12%;">ORDER #</th>
        <th style="width: 25%;">SUPPLIER / MILL</th>
        <th style="width: 20%;">ITEM / STYLE</th>
        <th style="width: 8%; text-align: center;">PCS</th>
        <th style="width: 10%; text-align: right;">RATE</th>
        <th style="width: 12%; text-align: center;">PACKING</th>
        <th style="width: 13%; text-align: right;">AMOUNT</th>
      </tr>
    </thead>
    <tbody>
      ${supplierRowsHtml}
    </tbody>
  </table>

  <div class="bottom-section">
    <div>
      ${packGroupsHtml}
      <div class="terms-box">
        <strong>Terms & Conditions:</strong>
        <ul>
          <li>This consolidated report is generated by Himat Textile for internal coordination and retailer verification.</li>
          <li>Delivery & goods receipt subject to individual supplier dispatch terms & transporter consignment note (LR).</li>
          <li>For any packing discrepancies or invoice queries, contact Himat Textile customer desk immediately.</li>
        </ul>
      </div>
    </div>

    <div>
      <div class="summary-box">
        <div class="summary-content">
          <div class="summary-row">
            <span class="label-sub">Subtotal (${totalPieces} Pcs):</span>
            <span class="font-bold">₹${formatInr(totalAmount)}</span>
          </div>
          <div class="summary-row">
            <span class="label-sub">Garment GST (5%):</span>
            <span class="font-bold">₹${formatInr(totalGst)}</span>
          </div>
          <div class="summary-row">
            <span class="label-sub">Packing Breakdown:</span>
            <span class="font-bold" style="color: #132338;">${totalCases} Full Cases + ${totalLoose} Loose</span>
          </div>
        </div>
        <div class="summary-banner">
          <span>CONSOLIDATED TOTAL:</span>
          <span>₹${formatInr(grandTotal)}</span>
        </div>
        <div class="payment-status-box">
          <div class="payment-row">
            <span>Payment Cleared:</span>
            <strong style="color: #16a34a;">₹${formatInr(totalPaid)}</strong>
          </div>
          <div class="payment-row">
            <span>Balance Due:</span>
            <strong style="color: ${totalDue > 0 ? "#dc2626" : "#16a34a"};">₹${formatInr(totalDue)}</strong>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="signatures-box">
    <div class="sig-line">
      Customer Acceptance / Signature
    </div>
    <div class="sig-line">
      For HIMAT TEXTILE (Authorized Signatory)
    </div>
  </div>
</body>
</html>
`
}

/**
 * Builds HTML for Supplier Purchase Copy / Wholesale Invoice
 * Matches Android PdfGenerator.generateSupplierCopy
 */
export function generateSupplierInvoiceHtml(data: SupplierInvoiceData): string {
  const { supplier, visit, customer, salesman, entries } = data

  const totalPieces = entries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  const totalAmount = entries.reduce((sum, e) => sum + (Number(e.totalAmount) || 0), 0)
  const totalGst = entries.reduce((sum, e) => sum + (Number(e.gstAmount) || 0), 0)
  const netTotal = entries.reduce(
    (sum, e) => sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )
  const totalCases = entries.reduce((sum, e) => sum + (Number(e.caseCount) || 0), 0)
  const totalLoose = entries.reduce((sum, e) => sum + (Number(e.loosePieces) || 0), 0)

  const firstEntry = entries[0]
  const transporter = firstEntry?.transporter || "To be advised"
  const lrNo = firstEntry?.lrNo || "Pending Dispatch"
  const lrDate = firstEntry?.lrDate || "—"
  const orderDate = visit.date || (firstEntry?.createdAt ? new Date(firstEntry.createdAt).toISOString().split("T")[0] : new Date().toISOString().split("T")[0])

  let itemsHtml = ""
  entries.forEach((item) => {
    const rate = item.rate || item.pricePerPiece || 0
    const packSplit =
      item.loosePieces > 0
        ? `${item.caseCount} Cases + ${item.loosePieces} Loose`
        : `${item.caseCount} Full Cases`

    itemsHtml += `
      <tr>
        <td class="mono font-bold">#${item.orderNo}</td>
        <td class="font-bold">${item.itemCode}</td>
        <td class="text-center font-bold">${item.pieces}</td>
        <td class="text-right">₹${formatInr(rate)}</td>
        <td class="text-center">${item.caseSize || "—"} pcs/cs</td>
        <td class="text-center">${packSplit}</td>
        <td class="text-right font-bold">₹${formatInr(Number(item.totalAmount) || 0)}</td>
      </tr>
    `
    if (item.mixedPackNote && item.mixedPackNote.trim()) {
      itemsHtml += `
        <tr class="pack-note-row">
          <td></td>
          <td colspan="6" class="pack-note-text">PACKING INSTRUCTION: ${item.mixedPackNote}</td>
        </tr>
      `
    }
  })

  const suppGstin = supplier.gstin || supplier.gstNumber || "Unregistered"
  const suppPhone = supplier.phone || "—"
  const suppContact = supplier.contactPerson || supplier.name
  const buyerName = customer?.name || visit.customerName || "Registered Retailer"
  const buyerCity = customer?.city || customer?.marketArea || "—"

  return `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Supplier Invoice / Voucher - ${supplier.name}</title>
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
      color: #141923;
      background: #ffffff;
      font-size: 11px;
      line-height: 1.4;
    }
    .header-banner {
      background: #132338;
      color: #ffffff;
      padding: 16px 20px;
      border-radius: 8px 8px 0 0;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }
    .company-title {
      font-size: 20px;
      font-weight: 800;
      letter-spacing: 0.5px;
      color: #ffffff;
      margin: 0;
    }
    .company-sub {
      color: #c89d3c;
      font-size: 10px;
      font-weight: 700;
      letter-spacing: 0.5px;
      margin-top: 3px;
    }
    .company-desc {
      color: #c8d2e1;
      font-size: 9px;
      margin-top: 3px;
    }
    .header-right {
      text-align: right;
    }
    .doc-type {
      font-size: 14px;
      font-weight: 800;
      color: #ffffff;
      letter-spacing: 0.5px;
    }
    .doc-meta {
      color: #dce6f5;
      font-size: 10px;
      margin-top: 4px;
    }
    .info-container {
      background: #f8fafc;
      border: 1px solid #dce2ea;
      border-top: none;
      padding: 12px 18px;
      display: grid;
      grid-template-columns: 1.2fr 1fr;
      gap: 16px;
    }
    .info-col h4 {
      margin: 0 0 4px 0;
      font-size: 10px;
      color: #64707d;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 700;
    }
    .info-title {
      font-size: 13px;
      font-weight: 700;
      color: #141923;
    }
    .info-sub {
      font-size: 10px;
      color: #4b5563;
      margin-top: 2px;
    }
    table.data-table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 14px;
      font-size: 10.5px;
    }
    table.data-table thead tr {
      background: #132338;
      color: #ffffff;
    }
    table.data-table th {
      padding: 7px 10px;
      font-weight: 700;
      font-size: 9.5px;
      letter-spacing: 0.3px;
    }
    table.data-table td {
      padding: 7px 10px;
      border-bottom: 1px solid #e2e8f0;
    }
    .pack-note-row td {
      padding: 2px 10px 6px 10px;
      border-bottom: 1px solid #e2e8f0;
    }
    .pack-note-text {
      color: #b45309;
      font-style: italic;
      font-weight: 600;
      font-size: 9.5px;
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
    .dispatch-box {
      background: #f8fafc;
      border: 1px solid #dce2ea;
      border-radius: 6px;
      padding: 10px 14px;
    }
    .dispatch-box h4 {
      margin: 0 0 8px 0;
      font-size: 10px;
      color: #132338;
      text-transform: uppercase;
      font-weight: 700;
      letter-spacing: 0.5px;
    }
    .dispatch-item {
      font-size: 10px;
      margin-bottom: 4px;
      color: #334155;
    }
    .summary-box {
      background: #f8fafc;
      border: 1px solid #dce2ea;
      border-radius: 6px;
      overflow: hidden;
    }
    .summary-content {
      padding: 10px 14px;
    }
    .summary-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 5px;
      font-size: 10.5px;
    }
    .summary-row.label-sub {
      color: #64707d;
    }
    .summary-banner {
      background: #132338;
      color: #ffffff;
      padding: 8px 14px;
      display: flex;
      justify-content: space-between;
      font-weight: 700;
      font-size: 12px;
    }
    .signatures-box {
      margin-top: 45px;
      display: flex;
      justify-content: space-between;
      padding: 0 30px;
    }
    .sig-line {
      width: 190px;
      text-align: center;
      border-top: 1.5px solid #141923;
      padding-top: 6px;
      font-weight: 700;
      font-size: 10.5px;
    }
  </style>
</head>
<body>
  <div class="header-banner">
    <div>
      <h1 class="company-title">HIMAT TEXTILE</h1>
      <div class="company-sub">GARMENT SOURCING AGENCY • SUPPLIER PURCHASE COPY</div>
      <div class="company-desc">Official Purchase Order & Spot Booking Voucher</div>
    </div>
    <div class="header-right">
      <div class="doc-type">SUPPLIER VOUCHER</div>
      <div class="doc-meta"><strong>Date:</strong> ${orderDate}</div>
      <div class="doc-meta"><strong>Type:</strong> ${supplier.type || "Wholesaler"}</div>
      <div class="doc-meta"><strong>Trip Code:</strong> ${visit.visitCode}</div>
    </div>
  </div>

  <div class="info-container">
    <div class="info-col">
      <h4>SUPPLIER (${(supplier.type || "Wholesaler").toUpperCase()}):</h4>
      <div class="info-title">${supplier.name}</div>
      <div class="info-sub">${supplier.marketArea || supplier.city || "Local Market"} • Contact: ${suppContact} (${suppPhone})</div>
      <div class="info-sub">GSTIN: ${suppGstin}</div>
    </div>
    <div class="info-col">
      <h4>BUYER / RETAILER:</h4>
      <div class="info-title">${buyerName}</div>
      <div class="info-sub">Destination Market: ${buyerCity}</div>
      <div class="info-sub">Agency Rep: ${salesman?.name || visit.employeeName || "Himat Textile"}</div>
    </div>
  </div>

  <table class="data-table">
    <thead>
      <tr>
        <th style="width: 12%;">ORDER #</th>
        <th style="width: 22%;">ITEM / STYLE CODE</th>
        <th style="width: 8%; text-align: center;">PCS</th>
        <th style="width: 12%; text-align: right;">RATE</th>
        <th style="width: 14%; text-align: center;">CASE SIZE</th>
        <th style="width: 18%; text-align: center;">PACKING</th>
        <th style="width: 14%; text-align: right;">AMOUNT</th>
      </tr>
    </thead>
    <tbody>
      ${itemsHtml}
    </tbody>
  </table>

  <div class="bottom-section">
    <div class="dispatch-box">
      <h4>Delivery & Dispatch Instructions:</h4>
      <div class="dispatch-item"><strong>Transporter:</strong> ${transporter}</div>
      <div class="dispatch-item"><strong>LR / Bilty No:</strong> ${lrNo} ${lrDate !== "—" ? `(${lrDate})` : ""}</div>
      <div class="dispatch-item"><strong>Billing:</strong> Supplier's GST Tax Invoice to follow goods</div>
      <div class="dispatch-item" style="color: #64748b; margin-top: 6px; font-size: 9px;">
        * Note: Please ensure LR number and transporter details are confirmed upon dispatch.
      </div>
    </div>

    <div>
      <div class="summary-box">
        <div class="summary-content">
          <div class="summary-row">
            <span class="label-sub">Total Quantity:</span>
            <span class="font-bold">${totalPieces} Pcs (${totalCases} Cases, ${totalLoose} Loose)</span>
          </div>
          <div class="summary-row">
            <span class="label-sub">Taxable Subtotal:</span>
            <span class="font-bold">₹${formatInr(totalAmount)}</span>
          </div>
          <div class="summary-row">
            <span class="label-sub">Garment GST (5%):</span>
            <span class="font-bold">₹${formatInr(totalGst)}</span>
          </div>
        </div>
        <div class="summary-banner">
          <span>ORDER NET TOTAL:</span>
          <span>₹${formatInr(netTotal)}</span>
        </div>
      </div>
    </div>
  </div>

  <div class="signatures-box">
    <div class="sig-line">
      Supplier's Confirmation / Stamp
    </div>
    <div class="sig-line">
      Himat Textile Representative
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
 * WhatsApp message generator for Customer Day Report
 */
export function buildCustomerReportWhatsAppText(data: CustomerReportData): string {
  const { visit, customer, salesman, entries } = data
  const totalPieces = entries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  const grandTotal = entries.reduce(
    (sum, e) => sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )
  const totalPaid = entries.reduce((sum, e) => sum + (Number(e.paidAmount) || 0), 0)
  const totalDue = Math.max(0, grandTotal - totalPaid)
  const custName = customer?.name || visit.customerName

  let text = `*HIMAT TEXTILE - CUSTOMER DAY REPORT*\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `📋 *Trip:* ${visit.visitCode} | 📅 ${visit.date}\n`
  text += `👤 *Customer:* ${custName}\n`
  text += `👔 *Agent:* ${salesman?.name || visit.employeeName}\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `*ITEMS PURCHASED:*\n`

  entries.forEach((item, idx) => {
    const rate = item.rate || item.pricePerPiece || 0
    const amt = Number(item.totalAmount) || 0
    text += `${idx + 1}. *${item.itemCode}* (${item.supplierName})\n`
    text += `   Qty: ${item.pieces} pcs @ ₹${rate} = ₹${formatInr(amt)}\n`
    if (item.mixedPackNote) {
      text += `   ↳ Note: ${item.mixedPackNote}\n`
    }
  })

  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `📦 *Total Qty:* ${totalPieces} Pcs\n`
  text += `💰 *Grand Total:* ₹${formatInr(grandTotal)}\n`
  text += `✅ *Payment Paid:* ₹${formatInr(totalPaid)}\n`
  text += `⚠️ *Balance Due:* ₹${formatInr(totalDue)}\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `_Generated via Himat Textile Platform_`

  return text
}

/**
 * WhatsApp message generator for Supplier Purchase Voucher
 */
export function buildSupplierInvoiceWhatsAppText(data: SupplierInvoiceData): string {
  const { supplier, visit, customer, entries } = data
  const totalPieces = entries.reduce((sum, e) => sum + (Number(e.pieces) || 0), 0)
  const netTotal = entries.reduce(
    (sum, e) => sum + (Number(e.grandTotalWithGst) || (Number(e.totalAmount) + Number(e.gstAmount)) || 0),
    0
  )
  const buyerName = customer?.name || visit.customerName

  let text = `*HIMAT TEXTILE - SUPPLIER PURCHASE ORDER*\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `🏭 *Supplier:* ${supplier.name}\n`
  text += `🛍️ *Buyer:* ${buyerName}\n`
  text += `📅 *Date:* ${visit.date} | *Trip:* ${visit.visitCode}\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `*ORDER DETAILS:*\n`

  entries.forEach((item, idx) => {
    const rate = item.rate || item.pricePerPiece || 0
    const pack =
      item.loosePieces > 0
        ? `${item.caseCount}c + ${item.loosePieces}L`
        : `${item.caseCount} cases`
    text += `${idx + 1}. *${item.itemCode}* - ${item.pieces} pcs (${pack})\n`
    text += `   Rate: ₹${rate} | Amt: ₹${formatInr(Number(item.totalAmount) || 0)}\n`
  })

  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `📦 *Total Quantity:* ${totalPieces} Pcs\n`
  text += `💰 *Order Net Total:* ₹${formatInr(netTotal)}\n`
  text += `━━━━━━━━━━━━━━━━━━━━\n`
  text += `_Please pack as per instructions & dispatch._`

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

  let tableRowsHtml = ""
  if (sortedEntries.length === 0) {
    tableRowsHtml = `
      <tr>
        <td colspan="8" class="text-center" style="padding: 24px; color: #64748b;">
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
  <title>Account Statement - ${customer.firmName || customer.name}</title>
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
      color: #141923;
      background: #ffffff;
      font-size: 10.5px;
      line-height: 1.35;
    }
    .header-banner {
      background: #132338;
      color: #ffffff;
      padding: 16px 20px;
      border-radius: 8px 8px 0 0;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }
    .company-title {
      font-size: 20px;
      font-weight: 800;
      letter-spacing: 0.5px;
      color: #ffffff;
      margin: 0;
    }
    .company-sub {
      color: #c89d3c;
      font-size: 10px;
      font-weight: 700;
      letter-spacing: 0.5px;
      margin-top: 3px;
    }
    .company-desc {
      color: #c8d2e1;
      font-size: 8.5px;
      margin-top: 3px;
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
      color: #dce6f5;
      font-size: 9.5px;
      margin-top: 3px;
    }
    .info-container {
      background: #f8fafc;
      border: 1px solid #dce2ea;
      border-top: none;
      padding: 12px 18px;
      display: grid;
      grid-template-columns: 1.4fr 1fr;
      gap: 16px;
    }
    .info-col h4 {
      margin: 0 0 4px 0;
      font-size: 9.5px;
      color: #64707d;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 700;
    }
    .info-title {
      font-size: 13px;
      font-weight: 700;
      color: #141923;
    }
    .info-sub {
      font-size: 9.5px;
      color: #4b5563;
      margin-top: 2px;
    }
    table.data-table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 12px;
      font-size: 10px;
    }
    table.data-table thead tr {
      background: #132338;
      color: #ffffff;
    }
    table.data-table th {
      padding: 7px 8px;
      font-weight: 700;
      font-size: 9px;
      letter-spacing: 0.3px;
    }
    table.data-table td {
      padding: 6px 8px;
      border-bottom: 1px solid #e2e8f0;
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
      border: 1px solid #dce2ea;
      border-radius: 6px;
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
      color: #64707d;
    }
    .summary-banner {
      background: #132338;
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
      color: #64707d;
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
      border-top: 1.5px solid #141923;
      padding-top: 5px;
      font-weight: 700;
      font-size: 10px;
    }
  </style>
</head>
<body>
  <div class="header-banner">
    <div>
      <h1 class="company-title">HIMAT TEXTILE</h1>
      <div class="company-sub">GARMENT SOURCING AGENCY • WHOLESALE TO RETAIL FACILITATOR</div>
      <div class="company-desc">Consolidated Account Ledger • Purchase Billing & Payment Realization Statement</div>
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
      <h4>Customer Account Details:</h4>
      <div class="info-title">${customer.firmName ? `${customer.firmName} (${customer.name})` : customer.name}</div>
      <div class="info-sub">${customer.shopAddress || customer.address || customerCity}</div>
      <div class="info-sub">Phone(s): ${customerPhones || "—"}</div>
      <div class="info-sub">GSTIN: ${customerGstin} • Markets: ${customer.markets || customer.marketArea || "Ahmedabad Wholesale"}</div>
    </div>
    <div class="info-col">
      <h4>Account Standing:</h4>
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
        <th style="width: 11%;">DATE</th>
        <th style="width: 10%;">ORDER #</th>
        <th style="width: 23%;">SUPPLIER / MILL</th>
        <th style="width: 22%;">ITEM & PACKING</th>
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
          <li>This statement reflects all purchases booked via Himat Textile for ${customer.firmName || customer.name} during the stated period.</li>
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

  let text = `*HIMAT TEXTILE - ACCOUNT STATEMENT*\n`
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
  text += `_Generated via Himat Textile Agency_`

  return text
}

