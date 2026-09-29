import { useId } from "react"
import { CalendarRange, X } from "lucide-react"
import { cn } from "../../lib/utils"
import { DATE_PRESETS, DatePreset, DateRange, dateRangeLabel } from "../../lib/domain"

interface DateRangeFilterProps {
  value: DateRange
  onChange: (next: DateRange) => void
  className?: string
  /** Hide the preset chips and show a compact select instead (for tight toolbars) */
  compact?: boolean
}

/**
 * One date filter for every list and master detail screen:
 * All Time, Today, Yesterday, Last 7 Days, This Month, Last Month, or a custom From / To range.
 */
export function DateRangeFilter({ value, onChange, className, compact = false }: DateRangeFilterProps) {
  const uid = useId()
  const fromId = `${uid}-from`
  const toId = `${uid}-to`
  const setPreset = (preset: DatePreset) => {
    if (preset === "CUSTOM") onChange({ preset, start: value.start, end: value.end })
    else onChange({ preset })
  }

  const inputClass =
    "h-8 rounded-full border border-zinc-200 bg-white px-2.5 text-xs text-zinc-800 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-200 focus:outline-none focus:ring-1 focus:ring-zinc-900"

  return (
    <div className={cn("flex flex-wrap items-center gap-1.5", className)}>
      <CalendarRange className="h-3.5 w-3.5 text-muted-foreground" aria-hidden="true" />
      {compact ? (
        <select
          value={value.preset}
          onChange={(e) => setPreset(e.target.value as DatePreset)}
          aria-label="Date range"
          className={inputClass}
        >
          {DATE_PRESETS.map((p) => (
            <option key={p.value} value={p.value}>
              {p.label}
            </option>
          ))}
        </select>
      ) : (
        <div className="inline-flex flex-wrap items-center gap-1" role="group" aria-label="Date range">
          {DATE_PRESETS.map((p) => {
            const active = value.preset === p.value
            return (
              <button
                key={p.value}
                type="button"
                onClick={() => setPreset(p.value)}
                aria-pressed={active}
                className={cn(
                  "rounded-full border px-2.5 py-1 text-[11px] font-medium transition-colors",
                  active
                    ? "border-zinc-900 bg-zinc-900 text-white dark:border-zinc-100 dark:bg-zinc-100 dark:text-zinc-900"
                    : "border-zinc-200 bg-white text-zinc-600 hover:bg-zinc-100 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300"
                )}
              >
                {p.label}
              </button>
            )
          })}
        </div>
      )}

      {value.preset === "CUSTOM" && (
        <>
          <label className="sr-only" htmlFor={fromId}>
            From date
          </label>
          <input
            id={fromId}
            type="date"
            value={value.start || ""}
            onChange={(e) => onChange({ ...value, start: e.target.value || undefined })}
            className={inputClass}
          />
          <span className="text-xs text-muted-foreground" aria-hidden="true">
            to
          </span>
          <label className="sr-only" htmlFor={toId}>
            To date
          </label>
          <input
            id={toId}
            type="date"
            value={value.end || ""}
            onChange={(e) => onChange({ ...value, end: e.target.value || undefined })}
            className={inputClass}
          />
        </>
      )}

      {value.preset !== "ALL" && (
        <button
          type="button"
          onClick={() => onChange({ preset: "ALL" })}
          className="inline-flex items-center gap-1 rounded-full px-2 py-1 text-[11px] font-semibold text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/40"
          title={`Clear date filter (${dateRangeLabel(value)})`}
        >
          <X className="h-3 w-3" aria-hidden="true" />
          Clear
        </button>
      )}
    </div>
  )
}
