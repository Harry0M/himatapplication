import { useAuth } from "../context/AuthContext"
import { Card } from "../components/ui/Card"
import { SubAgentDetail } from "./SubAgentsView"

/** What a Sub Agent sees after signing in: their own customers, those customers' trips and orders (read-only). */
export function AgentPortalView() {
  const { employee } = useAuth()

  if (!employee) {
    return (
      <Card className="p-8 text-center text-sm text-muted-foreground">
        Your Sub Agent record could not be found. Please contact the office.
      </Card>
    )
  }

  return <SubAgentDetail agent={employee} portal />
}
