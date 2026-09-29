import { useState, useEffect, useCallback, lazy, Suspense } from "react"
import { Loader2, ShieldX, LogOut } from "lucide-react"
import { useAuth } from "./context/AuthContext"
import { Sidebar, ActiveTab, ALL_TABS, TAB_TITLES, tabsForRole, defaultTabForRole } from "./components/layout/Sidebar"
import { Navbar } from "./components/layout/Navbar"
import { Button } from "./components/ui/Button"
import { LoginView } from "./views/LoginView"

// Every screen is loaded when it is first opened: the public registration links and each admin
// screen download only their own code instead of the whole admin app.
const DashboardView = lazy(() => import("./views/DashboardView").then((m) => ({ default: m.DashboardView })))
const VisitsView = lazy(() => import("./views/VisitsView").then((m) => ({ default: m.VisitsView })))
const OrdersView = lazy(() => import("./views/OrdersView").then((m) => ({ default: m.OrdersView })))
const PaymentsView = lazy(() => import("./views/PaymentsView").then((m) => ({ default: m.PaymentsView })))
const CustomersView = lazy(() => import("./views/CustomersView").then((m) => ({ default: m.CustomersView })))
const SuppliersView = lazy(() => import("./views/SuppliersView").then((m) => ({ default: m.SuppliersView })))
const ProductsView = lazy(() => import("./views/ProductsView").then((m) => ({ default: m.ProductsView })))
const EmployeesView = lazy(() => import("./views/EmployeesView").then((m) => ({ default: m.EmployeesView })))
const BrandsView = lazy(() => import("./views/BrandsView").then((m) => ({ default: m.BrandsView })))
const TransportersView = lazy(() => import("./views/TransportersView").then((m) => ({ default: m.TransportersView })))
const MarketsView = lazy(() => import("./views/MarketsView").then((m) => ({ default: m.MarketsView })))
const DeletionsView = lazy(() => import("./views/DeletionsView").then((m) => ({ default: m.DeletionsView })))
const CustomerRegistrationView = lazy(() =>
  import("./views/CustomerRegistrationView").then((m) => ({ default: m.CustomerRegistrationView }))
)
const SupplierRegistrationView = lazy(() =>
  import("./views/SupplierRegistrationView").then((m) => ({ default: m.SupplierRegistrationView }))
)
const LeadsView = lazy(() => import("./views/LeadsView").then((m) => ({ default: m.LeadsView })))
const ChequePdcView = lazy(() => import("./views/ChequePdcView").then((m) => ({ default: m.ChequePdcView })))
const SubAgentsView = lazy(() => import("./views/SubAgentsView").then((m) => ({ default: m.SubAgentsView })))
const RequestsView = lazy(() => import("./views/RequestsView").then((m) => ({ default: m.RequestsView })))
const AgentPortalView = lazy(() => import("./views/AgentPortalView").then((m) => ({ default: m.AgentPortalView })))

/** Shown for the moment a screen's code is loading */
function ScreenLoading() {
  return (
    <div className="flex items-center justify-center py-16" role="status">
      <Loader2 className="h-6 w-6 animate-spin text-zinc-500" aria-hidden="true" />
      <span className="sr-only">Loading</span>
    </div>
  )
}

// -----------------------------------------------------------------------------
// Admin tab <-> URL hash (#/app/<tab>) so refresh, back button and shared links keep the screen
// -----------------------------------------------------------------------------

function readTabFromHash(): ActiveTab | null {
  const m = /^#\/?app\/([a-z]+)/i.exec(window.location.hash || "")
  const tab = m?.[1]?.toLowerCase()
  return tab && (ALL_TABS as string[]).includes(tab) ? (tab as ActiveTab) : null
}

function writeTabToHash(tab: ActiveTab) {
  const next = `#/app/${tab}`
  if (window.location.hash !== next) {
    window.history.pushState(null, "", next)
  }
}

function FullScreenMessage({ text }: { text: string }) {
  return (
    <div className="flex h-screen w-screen items-center justify-center bg-zinc-50 dark:bg-black">
      <div className="flex flex-col items-center gap-3" role="status">
        <Loader2 className="h-8 w-8 animate-spin text-zinc-900 dark:text-zinc-100" aria-hidden="true" />
        <p className="text-xs font-medium text-muted-foreground">{text}</p>
      </div>
    </div>
  )
}

/** Signed in, but not allowed in (unknown email, suspended or deactivated). */
function AccessDeniedView({ message }: { message: string }) {
  const { user, logout } = useAuth()
  return (
    <div className="flex min-h-screen items-center justify-center bg-zinc-50 p-4 dark:bg-black">
      <div className="w-full max-w-md rounded-2xl border border-zinc-200 bg-white p-8 text-center shadow-xl dark:border-zinc-800 dark:bg-zinc-950">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-red-50 text-red-600 dark:bg-red-950 dark:text-red-300">
          <ShieldX className="h-6 w-6" aria-hidden="true" />
        </div>
        <h1 className="text-base font-bold text-zinc-900 dark:text-zinc-50">No access yet</h1>
        <p className="mt-2 text-xs text-muted-foreground">{message}</p>
        {user?.email && <p className="mt-3 text-[11px] font-mono text-zinc-500">{user.email}</p>}
        <Button shape="pill" variant="outline" onClick={logout} className="mt-6 gap-2">
          <LogOut className="h-4 w-4" aria-hidden="true" />
          Sign out and use another account
        </Button>
      </div>
    </div>
  )
}

function MainLayout() {
  const { user, loading: authLoading, role, accessDenied } = useAuth()
  const [activeTab, setActiveTabState] = useState<ActiveTab>(() => readTabFromHash() || "dashboard")
  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(false)

  const allowedTabs = tabsForRole(role)

  const setActiveTab = useCallback((tab: ActiveTab) => {
    setActiveTabState(tab)
    writeTabToHash(tab)
  }, [])

  // Back / forward buttons
  useEffect(() => {
    const onPop = () => {
      const tab = readTabFromHash()
      if (tab) setActiveTabState(tab)
    }
    window.addEventListener("popstate", onPop)
    window.addEventListener("hashchange", onPop)
    return () => {
      window.removeEventListener("popstate", onPop)
      window.removeEventListener("hashchange", onPop)
    }
  }, [])

  // Keep the user on a screen their role may open
  useEffect(() => {
    if (!role) return
    if (!allowedTabs.includes(activeTab)) {
      const fallback = defaultTabForRole(role)
      setActiveTabState(fallback)
      window.history.replaceState(null, "", `#/app/${fallback}`)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [role, activeTab])

  if (authLoading) {
    return <FullScreenMessage text="Checking your account..." />
  }

  if (!user) {
    return <LoginView />
  }

  if (accessDenied || !role) {
    return <AccessDeniedView message={accessDenied || "Your account has no role yet. Please contact the Admin."} />
  }

  const navigate = (tab: string) => {
    if ((ALL_TABS as string[]).includes(tab)) setActiveTab(tab as ActiveTab)
  }

  const renderActiveView = () => {
    switch (activeTab) {
      case "dashboard":
        return <DashboardView onNavigate={setActiveTab} />
      case "trips":
        return <VisitsView />
      case "orders":
        return <OrdersView />
      case "deliveries":
        return <OrdersView initialStatus="pending_delivery" />
      case "pending":
        // Old link: Pending Work is part of Home now (numbers link to Trips / Orders / Payments)
        return <DashboardView onNavigate={setActiveTab} />
      case "payments":
        return <PaymentsView />
      case "cheques":
        return <ChequePdcView />
      case "employees":
        return <EmployeesView onNavigate={navigate} />
      case "subagents":
        return <SubAgentsView />
      case "leads":
        return <LeadsView />
      case "customers":
        return <CustomersView onNavigate={navigate} />
      case "suppliers":
        return <SuppliersView />
      case "products":
        return <ProductsView />
      case "brands":
        return <BrandsView />
      case "transporters":
        return <TransportersView />
      case "markets":
        return <MarketsView />
      case "requests":
        return <RequestsView onNavigate={navigate} />
      case "deletions":
        return <DeletionsView />
      case "agent":
        return <AgentPortalView />
      default:
        return <DashboardView onNavigate={setActiveTab} />
    }
  }

  return (
    <div className="flex h-screen overflow-hidden bg-zinc-50/50 dark:bg-black">
      <Sidebar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
      />

      <div className="flex flex-1 flex-col overflow-hidden">
        <Navbar
          onToggleSidebar={() => setIsSidebarOpen(!isSidebarOpen)}
          activeTabTitle={TAB_TITLES[activeTab]}
          onOpenPending={allowedTabs.includes("deliveries") ? () => setActiveTab("deliveries") : undefined}
        />
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 md:p-8">
          {/* key: switching tabs starts the screen fresh (no stale detail pages) */}
          <div className="mx-auto max-w-7xl" key={activeTab}>
            <Suspense fallback={<ScreenLoading />}>{renderActiveView()}</Suspense>
          </div>
        </main>
      </div>
    </div>
  )
}

// -----------------------------------------------------------------------------
// Public registration forms. Exact routes only: before, any URL containing "supplier" or
// "register" (e.g. an admin screen) was hijacked by a registration form.
// -----------------------------------------------------------------------------

const SUPPLIER_ROUTES = new Set(["register-supplier", "supplier-register", "supplier-registration"])
const CUSTOMER_ROUTES = new Set(["register-customer", "customer-register", "customer-registration", "register"])

function getRegistrationRoutes() {
  if (typeof window === "undefined") {
    return { isSupplier: false, isCustomer: false }
  }
  // "#/register-customer?agent=12" -> "register-customer"
  const hashRoute = (window.location.hash || "")
    .replace(/^#\/?/, "")
    .split("?")[0]
    .replace(/\/+$/, "")
    .toLowerCase()
  const pathRoute = (window.location.pathname || "").replace(/\/+$/, "").split("/").pop()?.toLowerCase() || ""
  const params = new URLSearchParams(window.location.search || "")
  const mode = (params.get("mode") || params.get("action") || "").toLowerCase()

  const isSupplier =
    SUPPLIER_ROUTES.has(hashRoute) || SUPPLIER_ROUTES.has(pathRoute) || SUPPLIER_ROUTES.has(mode) || mode === "supplier"
  const isCustomer =
    !isSupplier &&
    (CUSTOMER_ROUTES.has(hashRoute) || CUSTOMER_ROUTES.has(pathRoute) || CUSTOMER_ROUTES.has(mode) || mode === "customer")

  return { isSupplier, isCustomer }
}

export function App() {
  const [routes, setRoutes] = useState(getRegistrationRoutes)

  useEffect(() => {
    const handleRouteChange = () => {
      setRoutes(getRegistrationRoutes())
    }

    window.addEventListener("hashchange", handleRouteChange)
    window.addEventListener("popstate", handleRouteChange)
    return () => {
      window.removeEventListener("hashchange", handleRouteChange)
      window.removeEventListener("popstate", handleRouteChange)
    }
  }, [])

  if (routes.isSupplier) {
    return (
      <Suspense fallback={<FullScreenMessage text="Opening the form..." />}>
        <SupplierRegistrationView />
      </Suspense>
    )
  }

  if (routes.isCustomer) {
    return (
      <Suspense fallback={<FullScreenMessage text="Opening the form..." />}>
        <CustomerRegistrationView />
      </Suspense>
    )
  }

  return <MainLayout />
}

export default App
