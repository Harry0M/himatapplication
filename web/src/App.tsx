import React, { useState } from "react"
import { useAuth } from "./context/AuthContext"
import { useData } from "./context/DataContext"
import { Sidebar, ActiveTab } from "./components/layout/Sidebar"
import { Navbar } from "./components/layout/Navbar"
import { LoginView } from "./views/LoginView"
import { DashboardView } from "./views/DashboardView"
import { VisitsView } from "./views/VisitsView"
import { OrdersView } from "./views/OrdersView"
import { PendingHubView } from "./views/PendingHubView"
import { PaymentsView } from "./views/PaymentsView"
import { DeliveriesView } from "./views/DeliveriesView"
import { CustomersView } from "./views/CustomersView"
import { SuppliersView } from "./views/SuppliersView"
import { ProductsView } from "./views/ProductsView"
import { EmployeesView } from "./views/EmployeesView"
import { BrandsView } from "./views/BrandsView"
import { TransportersView } from "./views/TransportersView"
import { MarketsView } from "./views/MarketsView"
import { DeletionsView } from "./views/DeletionsView"
import { Loader2 } from "lucide-react"

function MainLayout() {
  const { user, loading: authLoading } = useAuth()
  const [activeTab, setActiveTab] = useState<ActiveTab>("dashboard")
  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(false)
  const [searchQuery, setSearchQuery] = useState<string>("")

  if (authLoading) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-zinc-50 dark:bg-black">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="h-8 w-8 animate-spin text-zinc-900 dark:text-zinc-100" />
          <p className="text-xs font-medium text-muted-foreground">Authenticating session...</p>
        </div>
      </div>
    )
  }

  if (!user) {
    return <LoginView />
  }

  const renderActiveView = () => {
    switch (activeTab) {
      case "dashboard":
        return <DashboardView onNavigate={setActiveTab} />
      case "trips":
        return <VisitsView />
      case "orders":
        return <OrdersView />
      case "pending":
        return <PendingHubView />
      case "payments":
        return <PaymentsView />
      case "deliveries":
        return <DeliveriesView />
      case "employees":
        return <EmployeesView onNavigate={(tab) => setActiveTab(tab as ActiveTab)} />
      case "customers":
        return <CustomersView />
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
      case "deletions":
        return <DeletionsView />
      default:
        return <DashboardView onNavigate={setActiveTab} />
    }
  }

  const tabTitles: Record<ActiveTab, string> = {
    dashboard: "Dashboard Overview",
    trips: "Market Trips",
    orders: "Orders & Purchases",
    pending: "Pending Hub",
    payments: "Payments & Billing",
    deliveries: "Deliveries & Dispatch",
    employees: "Staff & Sales Agents",
    customers: "Customers",
    suppliers: "Suppliers & Mills",
    products: "Product Master",
    brands: "Brand Master",
    transporters: "Transporter & Logistics Master",
    markets: "Textile Markets Master",
    deletions: "Deletion Approvals & Recovery",
  }

  return (
    <div className="flex h-screen overflow-hidden bg-zinc-50/50 dark:bg-black">
      {/* Responsive Sidebar */}
      <Sidebar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
      />

      {/* Main Content Area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        <Navbar
          onToggleSidebar={() => setIsSidebarOpen(!isSidebarOpen)}
          activeTabTitle={tabTitles[activeTab]}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
        />
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 md:p-8">
          <div className="mx-auto max-w-7xl">
            {renderActiveView()}
          </div>
        </main>
      </div>
    </div>
  )
}

export function App() {
  return <MainLayout />
}

export default App
