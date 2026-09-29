import {
  LayoutDashboard,
  MapPin,
  Receipt,
  CreditCard,
  Truck,
  Users,
  Building2,
  UserCheck,
  LogOut,
  Sparkles,
  ShieldAlert,
  Tag,
  Compass,
  Package,
  Landmark,
  Handshake,
  Inbox,
  Store,
} from "lucide-react"
import type { LucideIcon } from "lucide-react"
import { useAuth } from "../../context/AuthContext"
import { useData } from "../../context/DataContext"
import { cn } from "../../lib/utils"
import { Badge } from "../ui/Badge"
import { Button } from "../ui/Button"
import { HIMAT_LOGO_DATA_URI } from "../../lib/logoBase64"
import type { AppRole } from "../../lib/domain"

export type ActiveTab =
  | "dashboard"
  | "trips"
  | "orders"
  | "pending"
  | "payments"
  | "cheques"
  | "deliveries"
  | "employees"
  | "subagents"
  | "leads"
  | "customers"
  | "suppliers"
  | "products"
  | "brands"
  | "transporters"
  | "markets"
  | "requests"
  | "deletions"
  | "agent"

export const ALL_TABS: ActiveTab[] = [
  "dashboard",
  "trips",
  "orders",
  "pending",
  "payments",
  "cheques",
  "deliveries",
  "employees",
  "subagents",
  "leads",
  "customers",
  "suppliers",
  "products",
  "brands",
  "transporters",
  "markets",
  "requests",
  "deletions",
  "agent",
]

/**
 * Screens each user type may open. Sub Agents get a small read-only portal (their customers with
 * trips, orders and referrals; "orders" stays open for old links).
 * "pending" and "deliveries" are old links: Pending Work is now part of Home, deliveries are the
 * Orders screen filtered to "not delivered".
 */
export function tabsForRole(role: AppRole | null): ActiveTab[] {
  if (role === "agent") return ["agent", "orders"]
  if (role === "staff") return ALL_TABS.filter((t) => t !== "requests" && t !== "deletions" && t !== "agent")
  if (role === "admin") return ALL_TABS.filter((t) => t !== "agent")
  return []
}

export function defaultTabForRole(role: AppRole | null): ActiveTab {
  return role === "agent" ? "agent" : "dashboard"
}

/** Page titles: always the same words as the menu item, so the menu and the page never disagree. */
export const TAB_TITLES: Record<ActiveTab, string> = {
  dashboard: "Home",
  trips: "Trips",
  orders: "Orders & Delivery",
  pending: "Home",
  payments: "Payments",
  cheques: "Cheques & PDC",
  deliveries: "Orders & Delivery",
  employees: "Staff",
  subagents: "Sub Agents",
  leads: "Leads",
  customers: "Customers",
  suppliers: "Suppliers & Mills",
  products: "Products",
  brands: "Brands",
  transporters: "Transporters",
  markets: "Markets",
  requests: "Registration Requests",
  deletions: "Deletions",
  agent: "My Customers",
}

interface NavItem {
  id: ActiveTab
  label: string
  icon: LucideIcon
  badge?: string
  badgeVariant?: "default" | "destructive" | "warning" | "info" | "success" | "outline"
}

interface NavGroup {
  title: string
  items: NavItem[]
}

interface SidebarProps {
  activeTab: ActiveTab
  setActiveTab: (tab: ActiveTab) => void
  isOpen: boolean
  onClose: () => void
}

export function Sidebar({ activeTab, setActiveTab, isOpen, onClose }: SidebarProps) {
  const { user, logout, role, employee } = useAuth()
  const {
    pendingDeliveriesCount,
    activeTripsCount,
    pendingDeletionsCount,
    pendingRegistrationRequestsCount,
    pendingSupplierRegistrationRequestsCount,
    dueTodayChequesCount,
    joinableVisits,
    customers,
  } = useData()

  const allowed = new Set(tabsForRole(role))
  const pendingRequests = pendingRegistrationRequestsCount + pendingSupplierRegistrationRequestsCount
  const count = (n: number) => (n > 0 ? `${n}` : undefined)

  const groups: NavGroup[] =
    role === "agent"
      ? [
          {
            // One place: customers with their trips, orders and referrals (tabs inside)
            title: "My work",
            items: [
              { id: "agent", label: "My Customers", icon: Store, badge: count(customers.length), badgeVariant: "outline" },
            ],
          },
        ]
      : [
          {
            title: "Daily work",
            items: [
              { id: "dashboard", label: "Home", icon: LayoutDashboard },
              {
                id: "trips",
                label: "Trips",
                icon: MapPin,
                badge: count(activeTripsCount + joinableVisits.length),
                badgeVariant: "default",
              },
              {
                id: "orders",
                label: "Orders & Delivery",
                icon: Receipt,
                badge: count(pendingDeliveriesCount),
                badgeVariant: "info",
              },
            ],
          },
          {
            title: "Masters",
            items: [
              { id: "customers", label: "Customers", icon: Users },
              { id: "suppliers", label: "Suppliers & Mills", icon: Building2 },
              { id: "subagents", label: "Sub Agents", icon: Handshake },
              { id: "employees", label: "Staff", icon: UserCheck },
              { id: "markets", label: "Markets", icon: Compass },
              { id: "transporters", label: "Transporters", icon: Truck },
              { id: "brands", label: "Brands", icon: Tag },
              { id: "products", label: "Products", icon: Package },
            ],
          },
          {
            title: "Money",
            items: [
              { id: "payments", label: "Payments", icon: CreditCard },
              {
                id: "cheques",
                label: "Cheques & PDC",
                icon: Landmark,
                badge: dueTodayChequesCount > 0 ? `${dueTodayChequesCount} due` : undefined,
                badgeVariant: "destructive",
              },
            ],
          },
          {
            // Everything waiting for a decision: registrations and deletions (admin), leads to follow up
            title: "Inbox",
            items: [
              {
                id: "requests",
                label: "Registration Requests",
                icon: Inbox,
                badge: count(pendingRequests),
                badgeVariant: "warning",
              },
              { id: "leads", label: "Leads", icon: Sparkles },
              {
                id: "deletions",
                label: "Deletions",
                icon: ShieldAlert,
                badge: count(pendingDeletionsCount),
                badgeVariant: "destructive",
              },
            ],
          },
        ]

  const visibleGroups = groups
    .map((g) => ({ ...g, items: g.items.filter((i) => allowed.has(i.id)) }))
    .filter((g) => g.items.length > 0)

  const roleText = role === "admin" ? "Admin" : role === "agent" ? "Sub Agent" : "Staff"
  const displayName = employee?.name || user?.displayName || "User"

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm lg:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-zinc-200/80 bg-white dark:border-zinc-800/80 dark:bg-zinc-950 transition-transform duration-200 ease-in-out lg:static lg:translate-x-0",
          isOpen ? "translate-x-0" : "-translate-x-full"
        )}
        aria-label="Main navigation"
      >
        {/* Brand Header */}
        <div className="flex h-16 items-center gap-3 border-b border-zinc-200/80 px-5 dark:border-zinc-800/80">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 p-1 shadow-2xs overflow-hidden shrink-0">
            <img src={HIMAT_LOGO_DATA_URI} alt="Himat Textile logo" className="h-full w-full object-contain" />
          </div>
          <div>
            <h1 className="text-sm font-bold tracking-tight text-zinc-900 dark:text-zinc-50 leading-tight">
              Himat Textile
            </h1>
            <p className="text-[10px] font-medium text-muted-foreground">{roleText} workspace</p>
          </div>
        </div>

        {/* Navigation Links, grouped so every feature lives in exactly one place */}
        <nav className="flex-1 overflow-y-auto px-3 py-3">
          {visibleGroups.map((group) => (
            <div key={group.title} className="mb-3">
              <p className="px-3 pb-1 text-[10px] font-bold uppercase tracking-wider text-zinc-400 dark:text-zinc-500">
                {group.title}
              </p>
              <div className="space-y-0.5">
                {group.items.map((item) => {
                  const Icon = item.icon
                  const active =
                    activeTab === item.id ||
                    (item.id === "orders" && activeTab === "deliveries") ||
                    (item.id === "dashboard" && activeTab === "pending")
                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => {
                        setActiveTab(item.id)
                        onClose()
                      }}
                      aria-current={active ? "page" : undefined}
                      className={cn(
                        "flex w-full items-center justify-between rounded-full px-3.5 py-2 text-xs font-medium transition-all select-none",
                        active
                          ? "bg-zinc-900 text-zinc-50 shadow-sm dark:bg-zinc-50 dark:text-zinc-900"
                          : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-900 dark:hover:text-zinc-100"
                      )}
                    >
                      <span className="flex items-center gap-3">
                        <Icon className={cn("h-4 w-4", active ? "text-inherit" : "text-zinc-500")} aria-hidden="true" />
                        <span>{item.label}</span>
                      </span>
                      {item.badge && (
                        <Badge variant={item.badgeVariant} className="px-2 py-0 text-[10px] font-bold">
                          {item.badge}
                        </Badge>
                      )}
                    </button>
                  )
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* Signed-in user & Logout */}
        <div className="border-t border-zinc-200/80 p-4 dark:border-zinc-800/80">
          <div className="flex items-center gap-3 rounded-2xl bg-zinc-50 p-2.5 dark:bg-zinc-900/60">
            {user?.photoURL ? (
              <img
                src={user.photoURL}
                alt=""
                className="h-8 w-8 rounded-full border border-zinc-200 object-cover dark:border-zinc-700"
              />
            ) : (
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-zinc-900 text-xs font-bold text-white dark:bg-zinc-100 dark:text-zinc-900">
                {(displayName || "U").charAt(0).toUpperCase()}
              </div>
            )}
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-semibold text-zinc-900 dark:text-zinc-100">{displayName}</p>
              <p className="truncate text-[10px] text-muted-foreground">
                {roleText} • {user?.email || ""}
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
            <LogOut className="mr-2 h-3.5 w-3.5" aria-hidden="true" />
            Sign Out
          </Button>
        </div>
      </aside>
    </>
  )
}
