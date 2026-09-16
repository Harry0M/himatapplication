import React, { useState, useRef } from "react"
import { UploadCloud, CheckCircle2, X, Eye, Loader2, Image as ImageIcon } from "lucide-react"
import { uploadFileToFirebaseStorage } from "../../lib/storage"
import { ImageLightboxModal } from "./ImageLightboxModal"

interface FileUploadProps {
  label: string
  folder: string
  prefix?: string
  value?: string
  onChange: (url: string) => void
  disabled?: boolean
  description?: string
  accept?: string
}

export const FileUpload: React.FC<FileUploadProps> = ({
  label,
  folder,
  prefix = "doc",
  value = "",
  onChange,
  disabled = false,
  description,
  accept = "image/*"
}) => {
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [isDragOver, setIsDragOver] = useState(false)
  const [showPreviewModal, setShowPreviewModal] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleFile = async (file: File) => {
    if (!file) return
    setErrorMessage(null)
    setIsUploading(true)
    setUploadProgress(0)

    try {
      const downloadUrl = await uploadFileToFirebaseStorage(
        file,
        folder,
        prefix,
        (progress) => setUploadProgress(progress)
      )
      onChange(downloadUrl)
    } catch (err: any) {
      console.error("Upload failed:", err)
      setErrorMessage(err?.message || "Upload failed. Check Firebase Storage rules.")
    } finally {
      setIsUploading(false)
    }
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) handleFile(file)
  }

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    setIsDragOver(false)
    if (disabled || isUploading) return
    const file = e.dataTransfer.files?.[0]
    if (file) handleFile(file)
  }

  return (
    <div className="space-y-1.5 text-xs">
      <div className="flex items-center justify-between">
        <label className="font-semibold text-zinc-800 dark:text-zinc-200 flex items-center gap-1.5">
          <span>{label}</span>
          {value && (
            <span className="inline-flex items-center gap-0.5 text-[10px] text-emerald-600 dark:text-emerald-400 font-medium">
              <CheckCircle2 className="h-3 w-3" /> Cloud Saved
            </span>
          )}
        </label>
        {description && (
          <span className="text-[10px] text-muted-foreground">{description}</span>
        )}
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept={accept}
        onChange={handleInputChange}
        disabled={disabled || isUploading}
        className="hidden"
      />

      {value ? (
        // Preview State with Uploaded Image Thumbnail
        <div className="p-2.5 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 flex items-center justify-between gap-3 shadow-sm">
          <div className="flex items-center gap-2.5 overflow-hidden">
            <div className="h-11 w-11 rounded-lg border border-zinc-200 dark:border-zinc-800 overflow-hidden bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center flex-shrink-0 relative group">
              <img
                src={value}
                alt={label}
                className="h-full w-full object-cover cursor-pointer"
                onClick={() => setShowPreviewModal(true)}
                onError={(e) => {
                  // Fallback icon if image cannot render directly
                  (e.target as HTMLElement).style.display = "none"
                }}
              />
              <button
                type="button"
                onClick={() => setShowPreviewModal(true)}
                className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity text-white"
                title="View full image"
              >
                <Eye className="h-4 w-4" />
              </button>
            </div>

            <div className="truncate">
              <p className="font-medium text-zinc-900 dark:text-zinc-100 text-xs truncate">
                {label}
              </p>
              <a
                href={value}
                target="_blank"
                rel="noreferrer"
                className="text-[10.5px] text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1 truncate"
              >
                Open Storage Link ↗
              </a>
            </div>
          </div>

          <div className="flex items-center gap-1.5 flex-shrink-0">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={disabled || isUploading}
              className="px-2.5 py-1 text-[11px] font-medium rounded-lg border border-zinc-200 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-800 text-zinc-700 dark:text-zinc-300 transition-colors"
            >
              Replace
            </button>
            <button
              type="button"
              onClick={() => onChange("")}
              disabled={disabled || isUploading}
              className="p-1 rounded-lg text-zinc-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/40 transition-colors"
              title="Remove file"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>
      ) : isUploading ? (
        // Uploading Progress State
        <div className="p-4 rounded-xl border border-indigo-200 dark:border-indigo-900/50 bg-indigo-50/50 dark:bg-indigo-950/20 text-center space-y-2">
          <div className="flex items-center justify-center gap-2 text-indigo-600 dark:text-indigo-400">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span className="font-semibold text-xs">Uploading to Cloud Storage... {uploadProgress}%</span>
          </div>
          <div className="w-full bg-indigo-100 dark:bg-indigo-950 rounded-full h-1.5 overflow-hidden">
            <div
              className="bg-indigo-600 dark:bg-indigo-500 h-1.5 rounded-full transition-all duration-300"
              style={{ width: `${uploadProgress}%` }}
            />
          </div>
        </div>
      ) : (
        // Empty State: Dropzone / Click to Upload
        <div
          onDragOver={(e) => {
            e.preventDefault()
            setIsDragOver(true)
          }}
          onDragLeave={() => setIsDragOver(false)}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className={`p-3.5 rounded-xl border-2 border-dashed transition-all cursor-pointer flex items-center justify-center gap-2.5 ${
            isDragOver
              ? "border-indigo-500 bg-indigo-50/50 dark:bg-indigo-950/30"
              : "border-zinc-200 dark:border-zinc-800 hover:border-zinc-400 dark:hover:border-zinc-700 bg-zinc-50/50 dark:bg-zinc-900/40"
          }`}
        >
          <div className="h-8 w-8 rounded-full bg-indigo-50 dark:bg-indigo-950/60 flex items-center justify-center text-indigo-600 dark:text-indigo-400 flex-shrink-0">
            <UploadCloud className="h-4 w-4" />
          </div>
          <div className="text-left">
            <p className="font-medium text-zinc-700 dark:text-zinc-300 text-xs">
              Click to select or drag photo here
            </p>
            <p className="text-[10px] text-muted-foreground">
              Images up to 10MB (JPEG, PNG, WEBP)
            </p>
          </div>
        </div>
      )}

      {errorMessage && (
        <p className="text-[11px] text-red-500 font-medium">⚠️ {errorMessage}</p>
      )}

      {/* Fullscreen Lightbox Modal with Download & Zoom */}
      <ImageLightboxModal
        open={showPreviewModal}
        onClose={() => setShowPreviewModal(false)}
        imageUrl={value}
        title={label}
      />
    </div>
  )
}
