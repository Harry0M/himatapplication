import React, { useState, useEffect } from "react"
import { Menu, Search, Moon, Sun, RefreshCw, Bell } from "lucide-react"
import { Button } from "../ui/Button"
import { Input } from "../ui/Input"
import { Badge } from "../ui/Badge"
import { useData } from "../../context/DataContext"

interface NavbarProps {
  onToggleSidebar: () => void
  activeTabTitle: string
  searchQuery: string
  onSearchChange: (q: string) => void
}

export function Navbar({
  onToggleSidebar,
  activeTabTitle,
  searchQuery,
  onSearchChange,
}: NavbarProps) {
  const [isDark, setIsDark] = useState<boolean>(() => {
    return document.documentElement.classList.contains("dark")
  })
  const { pendingPaymentsCount, pendingDeliveriesCount, activeTripsCount, looseEntriesCount } = useData()

  const totalPending =
    pendingPaymentsCount + pendingDeliveriesCount + activeTripsCount + looseEntriesCount

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add("dark")
    } else {
      document.documentElement.classList.remove("dark")
    }
  }, [isDark])

  const toggleTheme = () => {
    setIsDark(!isDark)
  }

  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-zinc-200/80 bg-white/95 px-4 backdrop-blur dark:border-zinc-800/80 dark:bg-zinc-950/95 sm:px-6">
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          shape="pill"
          onClick={onToggleSidebar}
          className="lg:hidden"
        >
          <Menu className="h-5 w-5" />
        </Button>
        <div>
          <h2 className="text-base font-bold tracking-tight text-zinc-900 dark:text-zinc-50 capitalize">
            {activeTabTitle}
          </h2>
        </div>
      </div>

      <div className="flex items-center gap-2 sm:gap-3">
        {/* Global Search Input */}
        <div className="relative hidden w-64 md:block">
          <Search className="absolute left-3.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Search orders, clients..."
            className="pl-9 h-8 text-xs bg-zinc-50 dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800"
          />
        </div>

        {/* Pending Alerts Pill Button */}
        {totalPending > 0 && (
          <div className="flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50/80 px-3 py-1 text-xs font-semibold text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/50 dark:text-amber-300 select-none">
            <Bell className="h-3 w-3 animate-pulse text-amber-600 dark:text-amber-400" />
            <span>{totalPending} Actions Due</span>
          </div>
        )}

        {/* Theme Toggle Button */}
        <Button
          variant="outline"
          size="icon"
          shape="pill"
          onClick={toggleTheme}
          className="h-8 w-8 text-zinc-600 dark:text-zinc-400"
          title={isDark ? "Switch to Light Mode" : "Switch to Dark Mode"}
        >
          {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
        </Button>
      </div>
    </header>
  )
}
