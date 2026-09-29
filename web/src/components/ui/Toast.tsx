import React, { useEffect, useState } from "react"
import { AlertCircle, AlertTriangle, CheckCircle2, Info, X } from "lucide-react"
import { cn } from "../../lib/utils"

export interface ToastProps {
  open: boolean
  message: string
  title?: string
  type?: "error" | "warning" | "success" | "info"
  duration?: number
  onClose: () => void
}

export function Toast({
  open,
  message,
  title,
  type = "error",
  duration = 5000,
  onClose,
}: ToastProps) {
  const [progress, setProgress] = useState(100)

  useEffect(() => {
    if (!open) {
      setProgress(100)
      return
    }

    // Attempt haptic vibration on mobile devices to alert the user
    if (typeof window !== "undefined" && "vibrate" in navigator && (type === "error" || type === "warning")) {
      try {
        navigator.vibrate([100, 50, 100])
      } catch (e) {
        // Ignore devices not permitting vibration
      }
    }

    const startTime = Date.now()
    const interval = setInterval(() => {
      const elapsed = Date.now() - startTime
      const remainingPct = Math.max(0, 100 - (elapsed / duration) * 100)
      setProgress(remainingPct)
      if (elapsed >= duration) {
        clearInterval(interval)
        onClose()
      }
    }, 50)

    return () => clearInterval(interval)
  }, [open, duration, onClose, type])

  if (!open || !message) return null

  const isError = type === "error"
  const isWarning = type === "warning"
  const isSuccess = type === "success"
  const isInfo = type === "info"

  return (
    <div
      role="alert"
      aria-live="assertive"
      className="fixed top-5 left-1/2 -translate-x-1/2 z-[9999] w-[92%] sm:w-auto sm:min-w-[360px] sm:max-w-lg transition-all duration-300 animate-in fade-in slide-in-from-top-4"
    >
      <div
        className={cn(
          "relative overflow-hidden rounded-2xl p-4 shadow-2xl backdrop-blur-md border-2",
          isError && "bg-white/95 dark:bg-zinc-900/95 border-red-500 text-zinc-900 dark:text-zinc-100",
          isWarning && "bg-white/95 dark:bg-zinc-900/95 border-amber-500 text-zinc-900 dark:text-zinc-100",
          isSuccess && "bg-white/95 dark:bg-zinc-900/95 border-emerald-500 text-zinc-900 dark:text-zinc-100",
          isInfo && "bg-white/95 dark:bg-zinc-900/95 border-blue-500 text-zinc-900 dark:text-zinc-100"
        )}
      >
        <div className="flex items-start gap-3">
          <div
            className={cn(
              "p-2 rounded-xl shrink-0 mt-0.5",
              isError && "bg-red-100 dark:bg-red-950/60 text-red-600 dark:text-red-400",
              isWarning && "bg-amber-100 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400",
              isSuccess && "bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400",
              isInfo && "bg-blue-100 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400"
            )}
          >
            {isError && <AlertCircle className="w-5 h-5 animate-pulse" />}
            {isWarning && <AlertTriangle className="w-5 h-5" />}
            {isSuccess && <CheckCircle2 className="w-5 h-5" />}
            {isInfo && <Info className="w-5 h-5" />}
          </div>

          <div className="flex-1 pr-2">
            <h4 className="font-bold text-sm leading-tight text-zinc-900 dark:text-zinc-100">
              {title || (isError ? "Mandatory Field Required" : isWarning ? "Attention" : isSuccess ? "Success" : "Notice")}
            </h4>
            <p className="mt-1 text-xs text-zinc-700 dark:text-zinc-300 font-medium leading-relaxed">
              {message}
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close notification"
            className="p-1 rounded-lg text-zinc-400 hover:text-zinc-700 dark:hover:text-zinc-200 hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Dynamic progress bar showing time until auto-dismiss */}
        <div className="absolute bottom-0 left-0 right-0 h-1 bg-zinc-100 dark:bg-zinc-800">
          <div
            className={cn(
              "h-full transition-all duration-75",
              isError && "bg-red-500",
              isWarning && "bg-amber-500",
              isSuccess && "bg-emerald-500",
              isInfo && "bg-blue-500"
            )}
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>
    </div>
  )
}
