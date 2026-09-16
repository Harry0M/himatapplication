import { Customer, Supplier } from "../types"

function escapeXml(str: string | undefined | null): string {
  if (!str) return ""
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;")
}

/**
 * Generate Tally XML for Customer Master (Sundry Debtors)
 */
export function generateCustomersTallyXml(customers: Customer[], companyName = "Himat Textile"): string {
  const activeCustomers = customers.filter((c) => !c.isDeleted)

  const ledgersXml = activeCustomers
    .map((c) => {
      const ledgerName = c.firmName ? `${c.firmName} (${c.customerId || c.name})` : c.name
      const addressLines = [
        c.shopAddress || c.address,
        c.outlets && c.outlets[0]?.address,
        c.city,
        c.district
      ].filter(Boolean)

      const stateName = c.state || "Gujarat"
      const pinCode = c.pincode || (c.outlets && c.outlets[0]?.pincode) || ""
      const gstin = (c.gstin || c.gstNumber || "").trim().toUpperCase()
      const pan = (c.panNumber || (gstin.length === 15 ? gstin.slice(2, 12) : "")).trim().toUpperCase()
      const phone = c.phone || ""
      const email = c.email || ""
      const creditDays = c.creditDays ? `${c.creditDays} Days` : "30 Days"

      return `      <TALLYMESSAGE xmlns:UDF="TallyUDF">
        <LEDGER NAME="${escapeXml(ledgerName)}" ACTION="Create">
          <NAME.LIST>
            <NAME>${escapeXml(ledgerName)}</NAME>
            ${c.firmName ? `<NAME>${escapeXml(c.firmName)}</NAME>` : ""}
            ${c.customerId ? `<NAME>${escapeXml(c.customerId)}</NAME>` : ""}
          </NAME.LIST>
          <PARENT>Sundry Debtors</PARENT>
          <MAILINGNAME.LIST>
            <MAILINGNAME>${escapeXml(c.firmName || c.name)}</MAILINGNAME>
          </MAILINGNAME.LIST>
          <ADDRESS.LIST>
${addressLines.map((line) => `            <ADDRESS>${escapeXml(line)}</ADDRESS>`).join("\n")}
          </ADDRESS.LIST>
          <COUNTRYNAME>India</COUNTRYNAME>
          <STATENAME>${escapeXml(stateName)}</STATENAME>
          <PINCODE>${escapeXml(pinCode)}</PINCODE>
          <LEDGERPHONE>${escapeXml(phone)}</LEDGERPHONE>
          <LEDGERMOBILE>${escapeXml(phone)}</LEDGERMOBILE>
          <EMAIL>${escapeXml(email)}</EMAIL>
          <INCOMETAXNUMBER>${escapeXml(pan)}</INCOMETAXNUMBER>
          <PARTYGSTIN>${escapeXml(gstin)}</PARTYGSTIN>
          <GSTREGISTRATIONTYPE>${gstin ? "Regular" : "Unregistered"}</GSTREGISTRATIONTYPE>
          <ISBILLWISEON>Yes</ISBILLWISEON>
          <BILLCREDITPERIOD>${escapeXml(creditDays)}</BILLCREDITPERIOD>
          <ISCREDITDAYSPICKLIST>Yes</ISCREDITDAYSPICKLIST>
          <DESCRIPTION>Imported from Himat SMS - Sales &amp; CRM</DESCRIPTION>
        </LEDGER>
      </TALLYMESSAGE>`
    })
    .join("\n")

  return `<?xml version="1.0" encoding="utf-8"?>
<ENVELOPE>
  <HEADER>
    <TALLYREQUEST>Import Data</TALLYREQUEST>
  </HEADER>
  <BODY>
    <IMPORTDATA>
      <REQUESTDESC>
        <REPORTNAME>All Masters</REPORTNAME>
        <STATICVARIABLES>
          <SVCURRENTCOMPANY>${escapeXml(companyName)}</SVCURRENTCOMPANY>
        </STATICVARIABLES>
      </REQUESTDESC>
      <REQUESTDATA>
${ledgersXml}
      </REQUESTDATA>
    </IMPORTDATA>
  </BODY>
</ENVELOPE>`
}

/**
 * Generate Tally XML for Supplier Master (Sundry Creditors)
 */
export function generateSuppliersTallyXml(suppliers: Supplier[], companyName = "Himat Textile"): string {
  const activeSuppliers = suppliers.filter((s) => !s.isDeleted)

  const ledgersXml = activeSuppliers
    .map((s) => {
      const ledgerName = s.firmName ? `${s.firmName} (${s.supplierId || s.name})` : s.name
      const addressLines = [
        s.officeAddress || s.address,
        s.marketName || s.marketArea,
        s.city,
        s.district
      ].filter(Boolean)

      const stateName = s.state || "Gujarat"
      const pinCode = s.pincode || ""
      const gstin = (s.gstin || s.gstNumber || "").trim().toUpperCase()
      const pan = (s.panNumber || (gstin.length === 15 ? gstin.slice(2, 12) : "")).trim().toUpperCase()
      const phone = s.phone || ""
      const email = s.email || ""

      return `      <TALLYMESSAGE xmlns:UDF="TallyUDF">
        <LEDGER NAME="${escapeXml(ledgerName)}" ACTION="Create">
          <NAME.LIST>
            <NAME>${escapeXml(ledgerName)}</NAME>
            ${s.firmName ? `<NAME>${escapeXml(s.firmName)}</NAME>` : ""}
            ${s.supplierId ? `<NAME>${escapeXml(s.supplierId)}</NAME>` : ""}
          </NAME.LIST>
          <PARENT>Sundry Creditors</PARENT>
          <MAILINGNAME.LIST>
            <MAILINGNAME>${escapeXml(s.firmName || s.name)}</MAILINGNAME>
          </MAILINGNAME.LIST>
          <ADDRESS.LIST>
${addressLines.map((line) => `            <ADDRESS>${escapeXml(line)}</ADDRESS>`).join("\n")}
          </ADDRESS.LIST>
          <COUNTRYNAME>India</COUNTRYNAME>
          <STATENAME>${escapeXml(stateName)}</STATENAME>
          <PINCODE>${escapeXml(pinCode)}</PINCODE>
          <LEDGERPHONE>${escapeXml(phone)}</LEDGERPHONE>
          <LEDGERMOBILE>${escapeXml(phone)}</LEDGERMOBILE>
          <EMAIL>${escapeXml(email)}</EMAIL>
          <INCOMETAXNUMBER>${escapeXml(pan)}</INCOMETAXNUMBER>
          <PARTYGSTIN>${escapeXml(gstin)}</PARTYGSTIN>
          <GSTREGISTRATIONTYPE>${gstin ? "Regular" : "Unregistered"}</GSTREGISTRATIONTYPE>
          <ISBILLWISEON>Yes</ISBILLWISEON>
          <DESCRIPTION>Imported from Himat SMS - Mills &amp; Suppliers</DESCRIPTION>
        </LEDGER>
      </TALLYMESSAGE>`
    })
    .join("\n")

  return `<?xml version="1.0" encoding="utf-8"?>
<ENVELOPE>
  <HEADER>
    <TALLYREQUEST>Import Data</TALLYREQUEST>
  </HEADER>
  <BODY>
    <IMPORTDATA>
      <REQUESTDESC>
        <REPORTNAME>All Masters</REPORTNAME>
        <STATICVARIABLES>
          <SVCURRENTCOMPANY>${escapeXml(companyName)}</SVCURRENTCOMPANY>
        </STATICVARIABLES>
      </REQUESTDESC>
      <REQUESTDATA>
${ledgersXml}
      </REQUESTDATA>
    </IMPORTDATA>
  </BODY>
</ENVELOPE>`
}

/**
 * Trigger file download in browser
 */
export function downloadXmlFile(xmlContent: string, filename: string): void {
  const blob = new Blob([xmlContent], { type: "application/xml;charset=utf-8" })
  const url = URL.createObjectURL(blob)
  const link = document.createElement("a")
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * Export single customer ledger to Tally XML
 */
export function exportCustomerToTallyXml(customer: Customer, companyName = "Himat Textile"): void {
  const xml = generateCustomersTallyXml([customer], companyName)
  const cleanName = (customer.firmName || customer.name || "Customer").replace(/[^a-zA-Z0-9_-]/g, "_")
  downloadXmlFile(xml, `Tally_Ledger_${cleanName}.xml`)
}

/**
 * Export single supplier ledger to Tally XML
 */
export function exportSupplierToTallyXml(supplier: Supplier, companyName = "Himat Textile"): void {
  const xml = generateSuppliersTallyXml([supplier], companyName)
  const cleanName = (supplier.firmName || supplier.name || "Supplier").replace(/[^a-zA-Z0-9_-]/g, "_")
  downloadXmlFile(xml, `Tally_Ledger_${cleanName}.xml`)
}

