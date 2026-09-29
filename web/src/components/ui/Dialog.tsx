import React, { useEffect, useRef } from "react"
import { X } from "lucide-react"
import { cn } from "../../lib/utils"
import { Button } from "./Button"

interface DialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  children: React.ReactNode
  title?: string
  description?: string
  className?: string
}

// Open dialogs, oldest first. Escape and the scroll lock only act on the top-most one, so a
// dialog opened from inside another (e.g. "+ New Sub Agent" in the customer form) closes alone.
const openDialogs: object[] = []

export function Dialog({
  open,
  onOpenChange,
  children,
  title,
  description,
  className
}: DialogProps) {
  // Kept in a ref so an inline onOpenChange does not re-run the effect and reorder the stack
  const onOpenChangeRef = useRef(onOpenChange)
  useEffect(() => {
    onOpenChangeRef.current = onOpenChange
  }, [onOpenChange])

  useEffect(() => {
    if (!open) return
    const token = {}
    openDialogs.push(token)
    document.body.style.overflow = "hidden"
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && openDialogs[openDialogs.length - 1] === token) onOpenChangeRef.current(false)
    }
    window.addEventListener("keydown", handleKeyDown)
    return () => {
      window.removeEventListener("keydown", handleKeyDown)
      const index = openDialogs.lastIndexOf(token)
      if (index >= 0) openDialogs.splice(index, 1)
      if (openDialogs.length === 0) document.body.style.overflow = "unset"
    }
  }, [open])

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/50 backdrop-blur-sm transition-opacity animate-in fade-in"
        onClick={() => onOpenChange(false)}
      />

      {/* Dialog Content */}
      <div
        className={cn(
          "relative z-50 w-full max-w-lg rounded-2xl border border-zinc-200 bg-white p-6 shadow-2xl transition-all animate-in zoom-in-95 dark:border-zinc-800 dark:bg-zinc-950",
          className
        )}
      >
        <div className="flex items-center justify-between pb-4">
          <div>
            {title && (
              <h2 className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-50">
                {title}
              </h2>
            )}
            {description && (
              <p className="text-sm text-muted-foreground mt-0.5">{description}</p>
            )}
          </div>
          <Button
            variant="ghost"
            size="icon"
            shape="pill"
            onClick={() => onOpenChange(false)}
            className="h-8 w-8 text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-50"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
        <div>{children}</div>
      </div>
    </div>
  )
}
