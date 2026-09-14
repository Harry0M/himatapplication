import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatInr(value: number): string {
  if (isNaN(value) || value === 0) return "0.00"
  return new Intl.NumberFormat("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

export function formatDate(timestampOrStr?: string | number): string {
  if (!timestampOrStr) return ""
  if (typeof timestampOrStr === "number") {
    return new Date(timestampOrStr).toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric"
    })
  }
  return timestampOrStr
}
