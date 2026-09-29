/**
 * Public registration links. Same URLs as the Android app (ShareUtil.kt):
 *   https://himatsms.web.app/#/register-customer?agent=<subAgentId>
 *   https://himatsms.web.app/#/register-supplier
 */
const PUBLIC_ORIGIN = "https://himatsms.web.app"

function origin(): string {
  if (typeof window === "undefined") return PUBLIC_ORIGIN
  const host = window.location.hostname
  // Links shared from a dev machine must still open the live site
  if (host === "localhost" || host === "127.0.0.1") return PUBLIC_ORIGIN
  return window.location.origin
}

/** Customer form. With a Sub Agent id, customers who register through it are linked to that agent. */
export function customerRegistrationUrl(subAgentId?: number | null): string {
  const base = `${origin()}/#/register-customer`
  return subAgentId && subAgentId > 0 ? `${base}?agent=${subAgentId}` : base
}

export function supplierRegistrationUrl(): string {
  return `${origin()}/#/register-supplier`
}

/** Reads ?agent=<id> from "#/register-customer?agent=12" (or the normal query string). */
export function subAgentIdFromUrl(): number | null {
  if (typeof window === "undefined") return null
  const hash = window.location.hash || ""
  const qIndex = hash.indexOf("?")
  const hashParams = new URLSearchParams(qIndex >= 0 ? hash.slice(qIndex + 1) : "")
  const raw = hashParams.get("agent") || new URLSearchParams(window.location.search || "").get("agent") || ""
  return /^\d+$/.test(raw.trim()) ? Number(raw.trim()) : null
}

/** wa.me link; phone is optional (Indian numbers get the 91 prefix). */
export function whatsAppUrl(text: string, phone?: string): string {
  const digits = (phone || "").replace(/\D/g, "")
  const target = digits.length === 10 ? `91${digits}` : digits
  return `https://wa.me/${target}?text=${encodeURIComponent(text)}`
}

export function subAgentInviteMessage(agentName: string, url: string): string {
  return (
    `HIMAT TEXTILE AHMEDABAD\nYour Garment Guide Across India\n\n` +
    `Hello,\n\nPlease open your trade account with Himat Textile using this link:\n${url}\n\n` +
    `After you submit the form you will get an SMS OTP for verification.\n\n` +
    `Referred by: ${agentName}\nThank you!`
  )
}
