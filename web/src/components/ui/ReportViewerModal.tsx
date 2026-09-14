import React, { useState } from "react"
import { Printer, Download, Share2, Check, X, Eye } from "lucide-react"
import { Dialog } from "./Dialog"
import { Button } from "./Button"
import { printReportHtml } from "../../lib/pdfReports"

interface ReportViewerModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  htmlContent: string
  whatsAppText?: string
}

export function ReportViewerModal({
  open,
  onOpenChange,
  title,
  htmlContent,
  whatsAppText,
}: ReportViewerModalProps) {
  const [copied, setCopied] = useState(false)

  const handlePrint = () => {
    printReportHtml(title, htmlContent)
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

      {/* Embedded Document Preview */}
      <div className="bg-zinc-200/50 p-4 dark:bg-zinc-950/60 max-h-[72vh] overflow-y-auto flex justify-center">
        <div className="w-full max-w-[800px] shadow-lg rounded-md overflow-hidden bg-white">
          <iframe
            srcDoc={htmlContent}
            title={title}
            className="w-full h-[650px] border-0"
            sandbox="allow-same-origin allow-scripts allow-popups"
          />
        </div>
      </div>
    </Dialog>
  )
}
