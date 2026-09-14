import React from "react"
import {
  LayoutDashboard,
  MapPin,
  Receipt,
  AlertCircle,
  CreditCard,
  Truck,
  Users,
  Building2,
  UserCheck,
  LogOut,
  Sparkles,
  ShieldAlert
} from "lucide-react"
import { useAuth } from "../../context/AuthContext"
import { useData } from "../../context/DataContext"
import { cn } from "../../lib/utils"
import { Badge } from "../ui/Badge"
import { Button } from "../ui/Button"

export type ActiveTab =
  | "dashboard"
  | "trips"
  | "orders"
  | "pending"
  | "payments"
  | "deliveries"
  | "employees"
  | "customers"
  | "suppliers"
  | "deletions"

interface SidebarProps {
  activeTab: ActiveTab
  setActiveTab: (tab: ActiveTab) => void
  isOpen: boolean
  onClose: () => void
}

export function Sidebar({ activeTab, setActiveTab, isOpen, onClose }: SidebarProps) {
  const { user, logout } = useAuth()
  const {
    pendingPaymentsCount,
    pendingDeliveriesCount,
    activeTripsCount,
    looseEntriesCount,
    pendingDeletionsCount
  } = useData()

  const totalPending =
    pendingPaymentsCount + pendingDeliveriesCount + activeTripsCount + looseEntriesCount

  const navItems = [
    {
      id: "dashboard",
      label: "Dashboard",
      icon: LayoutDashboard,
    },
    {
      id: "trips",
      label: "Market Trips",
      icon: MapPin,
      badge: activeTripsCount > 0 ? `${activeTripsCount}` : undefined,
      badgeVariant: "default" as const,
    },
    {
      id: "orders",
      label: "Orders & Purchases",
      icon: Receipt,
    },
    {
      id: "pending",
      label: "Pending Hub",
      icon: AlertCircle,
      badge: totalPending > 0 ? `${totalPending}` : undefined,
      badgeVariant: "destructive" as const,
    },
    {
      id: "payments",
      label: "Payments & Billing",
      icon: CreditCard,
      badge: pendingPaymentsCount > 0 ? `${pendingPaymentsCount}` : undefined,
      badgeVariant: "warning" as const,
    },
    {
      id: "deliveries",
      label: "Deliveries & Dispatch",
      icon: Truck,
      badge: pendingDeliveriesCount > 0 ? `${pendingDeliveriesCount}` : undefined,
      badgeVariant: "info" as const,
    },
    {
      id: "employees",
      label: "Staff & Agents",
      icon: UserCheck,
    },
    {
      id: "customers",
      label: "Customers",
      icon: Users,
    },
    {
      id: "suppliers",
      label: "Suppliers & Mills",
      icon: Building2,
    },
    {
      id: "deletions",
      label: "Deletions",
      icon: ShieldAlert,
      badge: pendingDeletionsCount > 0 ? `${pendingDeletionsCount}` : undefined,
      badgeVariant: "destructive" as const,
    },
  ]

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-zinc-200/80 bg-white dark:border-zinc-800/80 dark:bg-zinc-950 transition-transform duration-200 ease-in-out lg:static lg:translate-x-0",
          isOpen ? "translate-x-0" : "-translate-x-full"
        )}
      >
        {/* Brand Header */}
        <div className="flex h-16 items-center gap-3 border-b border-zinc-200/80 px-6 dark:border-zinc-800/80">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-zinc-900 text-white dark:bg-zinc-50 dark:text-zinc-950 shadow-sm">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-sm font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
              HIMAT SMS
            </h1>
            <p className="text-[11px] font-medium text-muted-foreground">Admin Operations</p>
          </div>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 space-y-1.5 overflow-y-auto p-4">
          {navItems.map((item) => {
            const Icon = item.icon
            const active = activeTab === item.id
            return (
              <button
                key={item.id}
                onClick={() => {
                  setActiveTab(item.id as ActiveTab)
                  onClose()
                }}
                className={cn(
                  "flex w-full items-center justify-between rounded-full px-4 py-2.5 text-xs font-medium transition-all select-none",
                  active
                    ? "bg-zinc-900 text-zinc-50 shadow-sm dark:bg-zinc-50 dark:text-zinc-900"
                    : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-900 dark:hover:text-zinc-100"
                )}
              >
                <div className="flex items-center gap-3">
                  <Icon className={cn("h-4 w-4", active ? "text-inherit" : "text-zinc-500")} />
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <Badge variant={item.badgeVariant} className="px-2 py-0 text-[10px] font-bold">
                    {item.badge}
                  </Badge>
                )}
              </button>
            )
          })}
        </nav>

        {/* Google Admin Profile & Logout */}
        <div className="border-t border-zinc-200/80 p-4 dark:border-zinc-800/80">
          <div className="flex items-center gap-3 rounded-2xl bg-zinc-50 p-2.5 dark:bg-zinc-900/60">
            {user?.photoURL ? (
              <img
                src={user.photoURL}
                alt={user.displayName || "Admin"}
                className="h-8 w-8 rounded-full border border-zinc-200 object-cover dark:border-zinc-700"
              />
            ) : (
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-zinc-900 text-xs font-bold text-white dark:bg-zinc-100 dark:text-zinc-900">
                {user?.email?.charAt(0).toUpperCase() || "A"}
              </div>
            )}
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-semibold text-zinc-900 dark:text-zinc-100">
                {user?.displayName || "Admin"}
              </p>
              <p className="truncate text-[10px] text-muted-foreground">
                {user?.email || "admin@himat.com"}
              </p>
            </div>
          </div>
          <Button
            variant="ghost"
            size="sm"
            shape="pill"
            onClick={logout}
            className="mt-2 w-full justify-start text-xs text-red-600 hover:bg-red-50 hover:text-red-700 dark:text-red-400 dark:hover:bg-red-950/50"
          >
            <LogOut className="mr-2 h-3.5 w-3.5" />
            Sign Out
          </Button>
        </div>
      </aside>
    </>
  )
}
