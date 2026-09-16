import React, { useState, useEffect } from "react"
import { X, Download, ZoomIn, ZoomOut, RotateCcw, ExternalLink, Loader2, Image as ImageIcon } from "lucide-react"
import { Button } from "./Button"

interface ImageLightboxModalProps {
  open: boolean
  onClose: () => Unit | void
  imageUrl: string
  title: string
  subtitle?: string
}

type Unit = void

export const ImageLightboxModal: React.FC<ImageLightboxModalProps> = ({
  open,
  onClose,
  imageUrl,
  title,
  subtitle = "Stored in Firebase Cloud Storage"
}) => {
  const [zoom, setZoom] = useState<number>(1)
  const [isDownloading, setIsDownloading] = useState<boolean>(false)
  const [downloadError, setDownloadError] = useState<string | null>(null)

  // Reset zoom when modal opens or image changes
  useEffect(() => {
    if (open) {
      setZoom(1)
      setDownloadError(null)
    }
  }, [open, imageUrl])

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && open) {
        onClose()
      }
    }
    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [open, onClose])

  if (!open || !imageUrl) return null

  const handleZoomIn = () => setZoom((prev) => Math.min(prev + 0.3, 3.5))
  const handleZoomOut = () => setZoom((prev) => Math.max(prev - 0.3, 0.5))
  const handleResetZoom = () => setZoom(1)

  const handleDownload = async () => {
    if (!imageUrl) return
    setIsDownloading(true)
    setDownloadError(null)

    try {
      const response = await fetch(imageUrl, { mode: "cors" })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const blob = await response.blob()
      const blobUrl = window.URL.createObjectURL(blob)

      const safeTitle = (title || "download").replace(/[^a-zA-Z0-9_-]/g, "_")
      const ext = imageUrl.includes(".png")
        ? "png"
        : imageUrl.includes(".webp")
        ? "webp"
        : imageUrl.includes(".pdf")
        ? "pdf"
        : "jpg"
      const filename = `${safeTitle}_${Date.now()}.${ext}`

      const link = document.createElement("a")
      link.href = blobUrl
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(blobUrl)
    } catch (err: any) {
      console.warn("Direct blob download failed, attempting window fallback", err)
      // Fallback: Trigger direct anchor download
      try {
        const link = document.createElement("a")
        link.href = imageUrl
        link.target = "_blank"
        link.rel = "noreferrer"
        const safeTitle = (title || "download").replace(/[^a-zA-Z0-9_-]/g, "_")
        link.download = `${safeTitle}.jpg`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
      } catch (fallbackErr) {
        setDownloadError("Download blocked by browser security. Use 'Open in New Tab'.")
      }
    } finally {
      setIsDownloading(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-[100] flex flex-col bg-black/90 backdrop-blur-md select-none transition-opacity duration-200"
      onClick={onClose}
    >
      {/* Top Bar */}
      <div
        className="flex items-center justify-between px-4 sm:px-6 py-3 bg-black/60 border-b border-white/10 z-10"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center gap-3 overflow-hidden">
          <div className="h-9 w-9 rounded-xl bg-white/10 flex items-center justify-center text-white shrink-0">
            <ImageIcon className="h-5 w-5" />
          </div>
          <div className="truncate">
            <h3 className="text-sm sm:text-base font-bold text-white truncate">{title}</h3>
            <p className="text-[11px] text-zinc-400 truncate">{subtitle}</p>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {/* Zoom controls */}
          <div className="hidden sm:flex items-center bg-white/10 rounded-xl p-0.5 border border-white/10 mr-2">
            <button
              onClick={handleZoomOut}
              disabled={zoom <= 0.5}
              className="p-1.5 text-zinc-300 hover:text-white disabled:opacity-40 rounded-lg hover:bg-white/10 transition-colors"
              title="Zoom Out"
            >
              <ZoomOut className="h-4 w-4" />
            </button>
            <button
              onClick={handleResetZoom}
              className="px-2 py-1 text-xs font-mono font-semibold text-zinc-300 hover:text-white rounded-lg hover:bg-white/10 transition-colors"
              title="Reset Zoom"
            >
              {Math.round(zoom * 100)}%
            </button>
            <button
              onClick={handleZoomIn}
              disabled={zoom >= 3.5}
              className="p-1.5 text-zinc-300 hover:text-white disabled:opacity-40 rounded-lg hover:bg-white/10 transition-colors"
              title="Zoom In"
            >
              <ZoomIn className="h-4 w-4" />
            </button>
          </div>

          {/* Download Button (Primary) */}
          <Button
            onClick={handleDownload}
            disabled={isDownloading}
            className="h-9 gap-1.5 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl shadow-lg shadow-emerald-950/40"
          >
            {isDownloading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                <span className="hidden sm:inline">Downloading...</span>
              </>
            ) : (
              <>
                <Download className="h-4 w-4" />
                <span>Download Image</span>
              </>
            )}
          </Button>

          {/* Open in external tab */}
          <a
            href={imageUrl}
            target="_blank"
            rel="noreferrer"
            className="p-2 text-zinc-300 hover:text-white hover:bg-white/10 rounded-xl transition-colors"
            title="Open in new browser tab"
          >
            <ExternalLink className="h-4 w-4" />
          </a>

          {/* Close button */}
          <button
            onClick={onClose}
            className="p-2 text-zinc-300 hover:text-white hover:bg-red-500/20 hover:text-red-400 rounded-xl transition-colors ml-1"
            title="Close (Esc)"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
      </div>

      {/* Image Canvas */}
      <div
        className="flex-1 flex items-center justify-center p-4 overflow-auto cursor-grab active:cursor-grabbing relative"
        onClick={(e) => {
          if (e.target === e.currentTarget) onClose()
        }}
      >
        <div
          className="transition-transform duration-150 ease-out flex items-center justify-center"
          style={{
            transform: `scale(${zoom})`,
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <img
            src={imageUrl}
            alt={title}
            className="max-h-[82vh] max-w-[92vw] w-auto h-auto object-contain rounded-lg shadow-2xl ring-1 ring-white/10 pointer-events-auto"
            draggable={false}
          />
        </div>

        {downloadError && (
          <div className="absolute bottom-6 left-1/2 -translate-x-1/2 bg-red-900/90 text-red-200 text-xs px-4 py-2 rounded-xl border border-red-700 shadow-xl backdrop-blur-sm">
            {downloadError}
          </div>
        )}
      </div>

      {/* Bottom status hint */}
      <div className="px-4 py-2 bg-black/60 border-t border-white/10 flex items-center justify-between text-[11px] text-zinc-400 z-10">
        <span>Click outside or press <kbd className="px-1.5 py-0.5 rounded bg-white/10 text-white font-mono text-[10px]">Esc</kbd> to close</span>
        <span className="hidden sm:inline">Use zoom controls or scroll to view details</span>
      </div>
    </div>
  )
}
