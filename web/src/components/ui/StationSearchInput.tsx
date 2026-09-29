import React, { useState, useRef, useEffect } from "react"
import { Train, X, ChevronDown } from "lucide-react"
import { INDIAN_RAILWAY_STATIONS } from "../../lib/indianRailwayStations"

interface StationSearchInputProps {
  id?: string
  value: string
  onChange: (value: string) => void
  placeholder?: string
  required?: boolean
  className?: string
  label?: string
}

/**
 * Searchable dropdown for Indian Railway Stations.
 * Type to filter – shows up to 10 matching results.
 * Value is stored as the full "Station Name (CODE)" string.
 */
export function StationSearchInput({
  id,
  value,
  onChange,
  placeholder = "Search railway station...",
  required,
  className = "",
  label,
}: StationSearchInputProps) {
  const [query, setQuery] = useState(value)
  const [isOpen, setIsOpen] = useState(false)
  const [focused, setFocused] = useState(false)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // Sync external value changes (e.g. reset)
  useEffect(() => {
    setQuery(value)
  }, [value])

  // Close on outside click
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setIsOpen(false)
        // If user typed something that doesn't match, keep their text as free-form value
        if (query !== value) {
          onChange(query)
        }
      }
    }
    document.addEventListener("mousedown", handleClickOutside)
    return () => document.removeEventListener("mousedown", handleClickOutside)
  }, [query, value, onChange])

  const filtered = query.length >= 1
    ? INDIAN_RAILWAY_STATIONS.filter((s) =>
        s.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 12)
    : INDIAN_RAILWAY_STATIONS.slice(0, 12)

  function handleSelect(station: string) {
    setQuery(station)
    onChange(station)
    setIsOpen(false)
  }

  function handleClear() {
    setQuery("")
    onChange("")
    setIsOpen(false)
    inputRef.current?.focus()
  }

  return (
    <div ref={wrapperRef} className={`relative ${className}`}>
      {label && (
        <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
          {label}
          {required && <span className="text-red-500 ml-0.5">*</span>}
        </label>
      )}
      <div className="relative">
        {/* Train icon */}
        <Train className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-zinc-400 dark:text-zinc-500 pointer-events-none" />

        <input
          ref={inputRef}
          id={id}
          type="text"
          value={query}
          autoComplete="off"
          placeholder={placeholder}
          required={required}
          onChange={(e) => {
            setQuery(e.target.value)
            setIsOpen(true)
          }}
          onFocus={() => {
            setFocused(true)
            setIsOpen(true)
          }}
          onBlur={() => setFocused(false)}
          className={`w-full h-10 pl-8 pr-8 rounded-xl border text-xs font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500 transition-colors ${
            focused
              ? "border-emerald-400 dark:border-emerald-600"
              : "border-zinc-300 dark:border-zinc-700"
          } bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100`}
        />

        {/* Clear / Chevron icon */}
        {query ? (
          <button
            type="button"
            onClick={handleClear}
            className="absolute right-2.5 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-700 dark:hover:text-zinc-200 transition-colors"
            tabIndex={-1}
          >
            <X className="w-3.5 h-3.5" />
          </button>
        ) : (
          <ChevronDown
            className="absolute right-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-zinc-400 pointer-events-none"
          />
        )}
      </div>

      {/* Dropdown */}
      {isOpen && filtered.length > 0 && (
        <ul className="absolute z-50 w-full mt-1 max-h-56 overflow-y-auto rounded-xl border border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 shadow-xl py-1 text-xs">
          {filtered.map((station) => (
            <li
              key={station}
              onMouseDown={(e) => {
                e.preventDefault() // keep input focused long enough to fire click
                handleSelect(station)
              }}
              className={`flex items-center gap-2 px-3 py-2 cursor-pointer transition-colors ${
                station === value
                  ? "bg-emerald-50 dark:bg-emerald-950/40 text-emerald-800 dark:text-emerald-300 font-semibold"
                  : "text-zinc-800 dark:text-zinc-200 hover:bg-zinc-50 dark:hover:bg-zinc-800"
              }`}
            >
              <Train className="w-3 h-3 shrink-0 text-zinc-400" />
              {station}
            </li>
          ))}
          {/* Allow typing anything else (free-form entry) */}
          {query && !INDIAN_RAILWAY_STATIONS.some(
            (s) => s.toLowerCase() === query.toLowerCase()
          ) && (
            <li
              onMouseDown={(e) => {
                e.preventDefault()
                onChange(query)
                setIsOpen(false)
              }}
              className="flex items-center gap-2 px-3 py-2 cursor-pointer text-zinc-500 dark:text-zinc-400 hover:bg-zinc-50 dark:hover:bg-zinc-800 border-t border-zinc-100 dark:border-zinc-800 italic"
            >
              Use "{query}" (custom entry)
            </li>
          )}
        </ul>
      )}
    </div>
  )
}
