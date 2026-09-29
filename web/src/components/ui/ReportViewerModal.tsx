import React, { useEffect, useMemo, useState } from "react"
import { Printer, Share2, Check, Eye } from "lucide-react"
import { Dialog } from "./Dialog"
import { Button } from "./Button"
import {
  DEFAULT_ORDER_FORM_OPTIONS,
  ORDER_NATURE_SELF,
  ORDER_NATURE_WHATSAPP,
  SupplierOrderFormOptions,
  printReportHtml,
} from "../../lib/pdfReports"

interface ReportViewerModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  htmlContent: string
  whatsAppText?: string
  /**
   * Supplier order forms only: rebuilds the document for the chosen options. When given, an
   * options bar is shown (GSTIN and market area off by default, order nature Self / WhatsApp).
   */
  buildHtml?: (options: SupplierOrderFormOptions) => string
}

export function ReportViewerModal({
  open,
  onOpenChange,
  title,
  htmlContent,
  whatsAppText,
  buildHtml,
}: ReportViewerModalProps) {
  const [copied, setCopied] = useState(false)
  const [options, setOptions] = useState<Required<SupplierOrderFormOptions>>(DEFAULT_ORDER_FORM_OPTIONS)

  // Every newly opened order form starts from the defaults
  useEffect(() => {
    if (open) setOptions(DEFAULT_ORDER_FORM_OPTIONS)
  }, [open])

  const documentHtml = useMemo(
    () => (buildHtml ? buildHtml(options) : htmlContent),
    [buildHtml, options, htmlContent]
  )

  const handlePrint = () => {
    printReportHtml(title, documentHtml)
  }

  const handleCopyWhatsApp = async () => {
    if (!whatsAppText) return
    try {
      await navigator.clipboard.writeText(whatsAppText)
      setCopied(true)
      setTimeout(() => setCopied(false), 2500)
    } catch (err) {
      console.error("Failed to copy", err)
    }
  }

  const natureButton = (label: string, value: string, selected: boolean) => (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      onClick={() => setOptions((o) => ({ ...o, orderNature: value }))}
      className={`h-7 rounded-full border px-3 text-xs font-semibold transition-colors ${
        selected
          ? "border-zinc-900 bg-zinc-900 text-white dark:border-zinc-100 dark:bg-zinc-100 dark:text-zinc-900"
          : "border-zinc-300 bg-white text-zinc-700 hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200"
      }`}
    >
      {label}
    </button>
  )

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      className="max-w-4xl p-0 overflow-hidden"
    >
      {/* Modal Top Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-200 bg-zinc-50/90 px-6 py-4 dark:border-zinc-800 dark:bg-zinc-900/90">
        <div>
          <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-50 flex items-center gap-2">
            <Eye className="h-4 w-4 text-zinc-500" />
            <span>{title}</span>
          </h3>
          <p className="text-xs text-muted-foreground">
            Print layout preview • Ready for PDF export or instant WhatsApp sharing
          </p>
        </div>

        <div className="flex items-center gap-2">
          {whatsAppText && (
            <Button
              variant="outline"
              size="sm"
              shape="pill"
              onClick={handleCopyWhatsApp}
              className="text-xs font-semibold gap-1.5"
            >
              {copied ? (
                <>
                  <Check className="h-3.5 w-3.5 text-emerald-600" />
                  <span className="text-emerald-600">Copied!</span>
                </>
              ) : (
                <>
                  <Share2 className="h-3.5 w-3.5 text-emerald-600" />
                  <span>Copy WhatsApp</span>
                </>
              )}
            </Button>
          )}

          <Button
            size="sm"
            shape="pill"
            onClick={handlePrint}
            className="text-xs font-semibold gap-1.5 shadow-sm"
          >
            <Printer className="h-3.5 w-3.5" />
            <span>Print / Save as PDF</span>
          </Button>
        </div>
      </div>

      {/* Order form options (same choices as the Android "Order Form PDF" sheet) */}
      {buildHtml && (
        <div className="flex flex-wrap items-center gap-x-5 gap-y-2 border-b border-zinc-200 bg-white px-6 py-3 text-xs dark:border-zinc-800 dark:bg-zinc-950">
          <span className="font-semibold text-zinc-700 dark:text-zinc-300">Optional details:</span>
          <label className="flex cursor-pointer items-center gap-1.5 text-zinc-700 dark:text-zinc-300">
            <input
              type="checkbox"
              checked={options.showGstin}
              onChange={(e) => setOptions((o) => ({ ...o, showGstin: e.target.checked }))}
              className="h-3.5 w-3.5 accent-zinc-900"
            />
            Supplier GSTIN
          </label>
          <label className="flex cursor-pointer items-center gap-1.5 text-zinc-700 dark:text-zinc-300">
            <input
              type="checkbox"
              checked={options.showMarketArea}
              onChange={(e) => setOptions((o) => ({ ...o, showMarketArea: e.target.checked }))}
              className="h-3.5 w-3.5 accent-zinc-900"
            />
            Market area
          </label>

          <div className="flex items-center gap-2" role="radiogroup" aria-label="Order nature">
            <span className="font-semibold text-zinc-700 dark:text-zinc-300">Order nature:</span>
            {natureButton("Self", ORDER_NATURE_SELF, options.orderNature === ORDER_NATURE_SELF)}
            {natureButton("WhatsApp", ORDER_NATURE_WHATSAPP, options.orderNature === ORDER_NATURE_WHATSAPP)}
          </div>
          <label className="flex items-center gap-1.5 text-zinc-700 dark:text-zinc-300">
            <span className="whitespace-nowrap">Printed as</span>
            <input
              type="text"
              value={options.orderNature}
              maxLength={40}
              onChange={(e) => setOptions((o) => ({ ...o, orderNature: e.target.value }))}
              className="h-7 w-40 rounded-md border border-zinc-300 bg-white px-2 text-xs text-zinc-900 focus:outline-none focus:ring-1 focus:ring-zinc-900 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
            />
          </label>
        </div>
      )}

      {/* Embedded Document Preview */}
      <div className="bg-zinc-200/50 p-4 dark:bg-zinc-950/60 max-h-[72vh] overflow-y-auto flex justify-center">
        <div className="w-full max-w-[800px] shadow-lg rounded-md overflow-hidden bg-white">
          <iframe
            srcDoc={documentHtml}
            title={title}
            className="w-full h-[650px] border-0"
            sandbox="allow-same-origin allow-scripts allow-popups"
          />
        </div>
      </div>
    </Dialog>
  )
}
