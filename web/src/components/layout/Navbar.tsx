import { useState, useEffect } from "react"
import { Menu, Moon, Sun, Bell } from "lucide-react"
import { Button } from "../ui/Button"
import { useData } from "../../context/DataContext"
import { useAuth } from "../../context/AuthContext"

interface NavbarProps {
  onToggleSidebar: () => void
  activeTabTitle: string
  /** Opens the Pending Work screen */
  onOpenPending?: () => void
}

export function Navbar({ onToggleSidebar, activeTabTitle, onOpenPending }: NavbarProps) {
  const [isDark, setIsDark] = useState<boolean>(() => {
    return document.documentElement.classList.contains("dark")
  })
  const { pendingDeliveriesCount } = useData()
  const { role } = useAuth()

  // Delivery is the only status that decides whether an order is done (payment is optional)
  const totalPending = pendingDeliveriesCount

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add("dark")
    } else {
      document.documentElement.classList.remove("dark")
    }
  }, [isDark])

  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-zinc-200/80 bg-white/95 px-4 backdrop-blur dark:border-zinc-800/80 dark:bg-zinc-950/95 sm:px-6">
      <div className="flex items-center gap-3 min-w-0">
        <Button
          variant="ghost"
          size="icon"
          shape="pill"
          onClick={onToggleSidebar}
          className="lg:hidden"
          aria-label="Open menu"
        >
          <Menu className="h-5 w-5" />
        </Button>
        <h2 className="truncate text-base font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
          {activeTabTitle}
        </h2>
      </div>

      <div className="flex items-center gap-2 sm:gap-3">
        {totalPending > 0 && role !== "agent" && onOpenPending && (
          <button
            type="button"
            onClick={onOpenPending}
            className="flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50/80 px-3 py-1 text-xs font-semibold text-amber-800 hover:bg-amber-100 dark:border-amber-900/50 dark:bg-amber-950/50 dark:text-amber-300 select-none"
            title="Orders not delivered yet"
          >
            <Bell className="h-3 w-3 text-amber-600 dark:text-amber-400" aria-hidden="true" />
            <span>{totalPending} not delivered</span>
          </button>
        )}

        <Button
          variant="outline"
          size="icon"
          shape="pill"
          onClick={() => setIsDark(!isDark)}
          className="h-8 w-8 text-zinc-600 dark:text-zinc-400"
          title={isDark ? "Switch to Light Mode" : "Switch to Dark Mode"}
          aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
        >
          {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
        </Button>
      </div>
    </header>
  )
}
