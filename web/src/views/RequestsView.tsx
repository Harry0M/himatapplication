import { useState } from "react"
import { Users, Building2 } from "lucide-react"
import { useData } from "../context/DataContext"
import { Tabs } from "../components/ui/Tabs"
import { CustomersView } from "./CustomersView"
import { SuppliersView } from "./SuppliersView"

interface RequestsViewProps {
  onNavigate?: (tab: string) => void
}

/**
 * One inbox for everybody who registered online (customer form + supplier form).
 * Approval screens are the same ones used inside the Customer / Supplier masters.
 */
export function RequestsView({ onNavigate }: RequestsViewProps) {
  const { pendingRegistrationRequestsCount, pendingSupplierRegistrationRequestsCount } = useData()
  const [kind, setKind] = useState<"customers" | "suppliers">(
    pendingRegistrationRequestsCount === 0 && pendingSupplierRegistrationRequestsCount > 0 ? "suppliers" : "customers"
  )

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">Registration Requests</h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            People who filled the online form. Approve to create their master record, or reject.
          </p>
        </div>
        <Tabs
          value={kind}
          onValueChange={(v) => setKind(v as "customers" | "suppliers")}
          options={[
            { value: "customers", label: "Customers", count: pendingRegistrationRequestsCount },
            { value: "suppliers", label: "Suppliers", count: pendingSupplierRegistrationRequestsCount },
          ]}
        />
      </div>

      <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
        {kind === "customers" ? (
          <Users className="h-3.5 w-3.5" aria-hidden="true" />
        ) : (
          <Building2 className="h-3.5 w-3.5" aria-hidden="true" />
        )}
        <span>Numbers on the tabs are pending requests.</span>
      </div>

      {kind === "customers" ? (
        <CustomersView key="customer-requests" initialViewMode="requests" onNavigate={onNavigate} />
      ) : (
        <SuppliersView key="supplier-requests" initialViewMode="requests" />
      )}
    </div>
  )
}
