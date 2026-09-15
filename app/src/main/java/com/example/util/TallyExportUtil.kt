package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.SupplierEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TallyExportUtil {

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun generateCustomersTallyXml(customers: List<CustomerEntity>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("<ENVELOPE>\n")
        sb.append("  <HEADER>\n")
        sb.append("    <TALLYREQUEST>Import Data</TALLYREQUEST>\n")
        sb.append("  </HEADER>\n")
        sb.append("  <BODY>\n")
        sb.append("    <IMPORTDATA>\n")
        sb.append("      <REQUESTDESC>\n")
        sb.append("        <REPORTNAME>All Masters</REPORTNAME>\n")
        sb.append("      </REQUESTDESC>\n")
        sb.append("      <REQUESTDATA>\n")

        for (c in customers) {
            val displayName = (if (c.firmName.isNotBlank()) c.firmName else c.name).trim()
            val fullName = if (c.customerId.isNotBlank()) "$displayName (${c.customerId})" else displayName
            val escapedName = escapeXml(fullName)
            val escapedDisplayName = escapeXml(displayName)
            val primaryAddress = c.shopAddress.ifBlank { c.address }.trim()
            val stateName = c.state.ifBlank { "Gujarat" }.trim()
            val gstType = if (c.gstin.isNotBlank()) "Regular" else "Unregistered"

            sb.append("        <TALLYMESSAGE xmlns:UDF=\"TallyUDF\">\n")
            sb.append("          <LEDGER NAME=\"$escapedName\" ACTION=\"Create\">\n")
            sb.append("            <NAME.LIST>\n")
            sb.append("              <NAME>$escapedName</NAME>\n")
            if (c.customerId.isNotBlank()) {
                sb.append("              <NAME>${escapeXml(c.customerId)}</NAME>\n")
            }
            sb.append("            </NAME.LIST>\n")
            sb.append("            <PARENT>Sundry Debtors</PARENT>\n")
            sb.append("            <MAILINGNAME.LIST>\n")
            sb.append("              <MAILINGNAME>$escapedDisplayName</MAILINGNAME>\n")
            sb.append("            </MAILINGNAME.LIST>\n")
            if (primaryAddress.isNotBlank()) {
                sb.append("            <ADDRESS.LIST>\n")
                sb.append("              <ADDRESS>${escapeXml(primaryAddress)}</ADDRESS>\n")
                if (c.city.isNotBlank()) {
                    sb.append("              <ADDRESS>${escapeXml(c.city)}</ADDRESS>\n")
                }
                sb.append("            </ADDRESS.LIST>\n")
            }
            sb.append("            <STATENAME>${escapeXml(stateName)}</STATENAME>\n")
            if (c.pincode.isNotBlank()) {
                sb.append("            <PINCODE>${escapeXml(c.pincode)}</PINCODE>\n")
            }
            sb.append("            <COUNTRYNAME>India</COUNTRYNAME>\n")
            if (c.phone.isNotBlank()) {
                sb.append("            <LEDGERPHONE>${escapeXml(c.phone)}</LEDGERPHONE>\n")
            }
            if (c.email.isNotBlank()) {
                sb.append("            <EMAIL>${escapeXml(c.email)}</EMAIL>\n")
            }
            if (c.gstin.isNotBlank()) {
                sb.append("            <PARTYGSTIN>${escapeXml(c.gstin)}</PARTYGSTIN>\n")
            }
            if (c.panNumber.isNotBlank()) {
                sb.append("            <PANNUMBER>${escapeXml(c.panNumber)}</PANNUMBER>\n")
            }
            sb.append("            <GSTREGISTRATIONTYPE>$gstType</GSTREGISTRATIONTYPE>\n")
            sb.append("            <ISBILLWISEON>Yes</ISBILLWISEON>\n")
            sb.append("            <BILLCREDITPERIOD>${c.creditDays} Days</BILLCREDITPERIOD>\n")
            if (c.creditLimit > 0) {
                sb.append("            <CREDITLIMIT>${c.creditLimit.toLong()}</CREDITLIMIT>\n")
            }
            sb.append("          </LEDGER>\n")
            sb.append("        </TALLYMESSAGE>\n")
        }

        sb.append("      </REQUESTDATA>\n")
        sb.append("    </IMPORTDATA>\n")
        sb.append("  </BODY>\n")
        sb.append("</ENVELOPE>")
        return sb.toString()
    }

    fun generateSuppliersTallyXml(suppliers: List<SupplierEntity>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("<ENVELOPE>\n")
        sb.append("  <HEADER>\n")
        sb.append("    <TALLYREQUEST>Import Data</TALLYREQUEST>\n")
        sb.append("  </HEADER>\n")
        sb.append("  <BODY>\n")
        sb.append("    <IMPORTDATA>\n")
        sb.append("      <REQUESTDESC>\n")
        sb.append("        <REPORTNAME>All Masters</REPORTNAME>\n")
        sb.append("      </REQUESTDESC>\n")
        sb.append("      <REQUESTDATA>\n")

        for (s in suppliers) {
            val displayName = (if (s.firmName.isNotBlank()) s.firmName else s.name).trim()
            val fullName = if (s.supplierId.isNotBlank()) "$displayName (${s.supplierId})" else displayName
            val escapedName = escapeXml(fullName)
            val escapedDisplayName = escapeXml(displayName)
            val primaryAddress = s.officeAddress.ifBlank { s.address }.trim()
            val gstType = if (s.gstin.isNotBlank()) "Regular" else "Unregistered"

            sb.append("        <TALLYMESSAGE xmlns:UDF=\"TallyUDF\">\n")
            sb.append("          <LEDGER NAME=\"$escapedName\" ACTION=\"Create\">\n")
            sb.append("            <NAME.LIST>\n")
            sb.append("              <NAME>$escapedName</NAME>\n")
            if (s.supplierId.isNotBlank()) {
                sb.append("              <NAME>${escapeXml(s.supplierId)}</NAME>\n")
            }
            sb.append("            </NAME.LIST>\n")
            sb.append("            <PARENT>Sundry Creditors</PARENT>\n")
            sb.append("            <MAILINGNAME.LIST>\n")
            sb.append("              <MAILINGNAME>$escapedDisplayName</MAILINGNAME>\n")
            sb.append("            </MAILINGNAME.LIST>\n")
            if (primaryAddress.isNotBlank()) {
                sb.append("            <ADDRESS.LIST>\n")
                sb.append("              <ADDRESS>${escapeXml(primaryAddress)}</ADDRESS>\n")
                if (s.city.isNotBlank()) {
                    sb.append("              <ADDRESS>${escapeXml(s.city)}</ADDRESS>\n")
                }
                sb.append("            </ADDRESS.LIST>\n")
            }
            sb.append("            <STATENAME>Gujarat</STATENAME>\n")
            sb.append("            <COUNTRYNAME>India</COUNTRYNAME>\n")
            if (s.phone.isNotBlank()) {
                sb.append("            <LEDGERPHONE>${escapeXml(s.phone)}</LEDGERPHONE>\n")
            }
            if (s.email.isNotBlank()) {
                sb.append("            <EMAIL>${escapeXml(s.email)}</EMAIL>\n")
            }
            if (s.gstin.isNotBlank()) {
                sb.append("            <PARTYGSTIN>${escapeXml(s.gstin)}</PARTYGSTIN>\n")
            }
            if (s.panNumber.isNotBlank()) {
                sb.append("            <PANNUMBER>${escapeXml(s.panNumber)}</PANNUMBER>\n")
            }
            sb.append("            <GSTREGISTRATIONTYPE>$gstType</GSTREGISTRATIONTYPE>\n")
            sb.append("            <ISBILLWISEON>Yes</ISBILLWISEON>\n")
            sb.append("          </LEDGER>\n")
            sb.append("        </TALLYMESSAGE>\n")
        }

        sb.append("      </REQUESTDATA>\n")
        sb.append("    </IMPORTDATA>\n")
        sb.append("  </BODY>\n")
        sb.append("</ENVELOPE>")
        return sb.toString()
    }

    fun exportAndShareTallyXml(
        context: Context,
        xmlContent: String,
        fileNamePrefix: String = "Himat_Tally_Import"
    ) {
        try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            val xmlFile = File(reportsDir, "${fileNamePrefix}_$dateStr.xml")
            xmlFile.writeText(xmlContent, Charsets.UTF_8)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                xmlFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/xml"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Tally XML Import - Himat Textile")
                putExtra(Intent.EXTRA_TEXT, "Import this XML file into Tally Prime / ERP 9 (Masters > Import Data).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export to Tally XML via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export Tally XML: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
