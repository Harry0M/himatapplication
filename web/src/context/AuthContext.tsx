import React, { createContext, useContext, useEffect, useMemo, useState } from "react"
import { User, signInWithPopup, signOut as fbSignOut, onAuthStateChanged } from "firebase/auth"
import { ref, get, set, onValue } from "firebase/database"
import { auth, googleProvider, rtdb } from "../lib/firebase"
import type { Employee } from "../types"
import { AppRole, isAdminRole, isAgentRole } from "../lib/domain"

interface AuthContextType {
  user: User | null
  /** True until we know who is signed in and what they may see */
  loading: boolean
  /** Owner / admin: full access to every screen */
  isAdmin: boolean
  /** admin | staff | agent. null while resolving or when the user has no access */
  role: AppRole | null
  /** employees record of the signed-in person (Staff, Sub Agent, or an Admin who also has a staff record) */
  employee: Employee | null
  /** Why a signed-in user can't use the admin app (unknown email, suspended, deactivated) */
  accessDenied: string | null
  loginWithGoogle: () => Promise<void>
  logout: () => Promise<void>
  error: string | null
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

const emailKey = (email: string) => email.trim().toLowerCase().replace(/\./g, "_").replace(/@/g, "_at_")

function emailsOf(emp: any): string[] {
  const list: string[] = []
  const push = (v: unknown) => {
    if (typeof v === "string" && v.trim()) list.push(v.trim().toLowerCase())
  }
  push(emp?.email)
  push(emp?.alternateEmail)
  if (Array.isArray(emp?.emails)) emp.emails.forEach(push)
  return list
}

function parseEmployees(val: any): Employee[] {
  if (!val || typeof val !== "object") return []
  const pairs: Array<[string, any]> = Array.isArray(val) ? val.map((v, i) => [String(i), v]) : Object.entries(val)
  return pairs
    .filter(([, v]) => v && typeof v === "object")
    .map(([key, v]) => {
      const rawId = v.id ?? key
      const id = typeof rawId === "string" && /^\d+$/.test(rawId.trim()) ? Number(rawId) : rawId
      return { ...v, id } as Employee
    })
}

/**
 * Super admin check. Same data as the Android app: super_admins/<email_key> or super_admins/<uid>.
 * The first person to sign in to an empty database becomes the owner (Android does the same).
 */
async function checkSuperAdmin(u: User): Promise<boolean> {
  const email = (u.email || "").trim().toLowerCase()
  if (!email) return false
  const byEmailRef = ref(rtdb, `super_admins/${emailKey(email)}`)
  const byUidRef = ref(rtdb, `super_admins/${u.uid}`)
  const [emailSnap, uidSnap] = await Promise.all([get(byEmailRef), get(byUidRef)])
  if (emailSnap.exists() || uidSnap.exists()) {
    if (!uidSnap.exists() && emailSnap.exists()) {
      await set(byUidRef, emailSnap.val()).catch(() => undefined)
    }
    return true
  }

  const allSnap = await get(ref(rtdb, "super_admins"))
  const data = allSnap.val() || {}
  const adminData = {
    uid: u.uid,
    email,
    name: u.displayName || "Admin",
    role: "SUPER_ADMIN",
    createdAt: Date.now(),
  }

  // There used to be a "super_admins is empty, so I must be the owner" branch here, mirroring a
  // clause in the database rules. Both are gone. Owner keys can be removed one at a time, so
  // emptying the node re-armed the clause and handed the agency to whoever signed in next. A fresh
  // deployment gets its first owner from the Firebase console, once.

  const matches = Object.values(data).some(
    (a: any) =>
      (typeof a === "string" && a.trim().toLowerCase() === email) ||
      (typeof a?.email === "string" && a.email.trim().toLowerCase() === email)
  )
  if (matches) {
    await set(byUidRef, adminData).catch(() => undefined)
  }
  return matches
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null)
  const [authResolved, setAuthResolved] = useState<boolean>(false)
  // null = still checking
  const [superAdmin, setSuperAdmin] = useState<boolean | null>(null)
  const [adminCheckFailed, setAdminCheckFailed] = useState<boolean>(false)
  // null = still loading
  const [employeesList, setEmployeesList] = useState<Employee[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser)
      setAuthResolved(true)
    })
    return () => unsubscribe()
  }, [])

  // Owner check (one time per sign-in)
  useEffect(() => {
    setSuperAdmin(null)
    setAdminCheckFailed(false)
    if (!user || !user.email) {
      setSuperAdmin(false)
      return
    }
    let cancelled = false
    checkSuperAdmin(user)
      .then((isOwner) => {
        if (!cancelled) setSuperAdmin(isOwner)
      })
      .catch((e) => {
        console.error("Error checking admin status:", e)
        if (!cancelled) {
          // Never grant access because a check failed
          setAdminCheckFailed(true)
          setSuperAdmin(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [user])

  // Live employees list, so suspending / changing someone's role applies immediately
  useEffect(() => {
    setEmployeesList(null)
    if (!user || !user.email) return
    const unsub = onValue(
      ref(rtdb, "employees"),
      (snap) => setEmployeesList(parseEmployees(snap.val())),
      (err) => {
        console.error("RTDB error reading employees for access check:", err)
        setEmployeesList([])
      }
    )
    return () => unsub()
  }, [user])

  const access = useMemo(() => {
    const none = { role: null as AppRole | null, employee: null as Employee | null, accessDenied: null as string | null }
    if (!user) return none
    if (!user.email) {
      // Phone-OTP sessions (registration forms) never open the admin app
      return { ...none, accessDenied: "Please sign in with your Google account to use the admin app." }
    }
    const email = user.email.trim().toLowerCase()
    const matched = (employeesList || []).find((e) => emailsOf(e).includes(email)) || null

    if (superAdmin) {
      // An owner who also has a staff record can start / join trips under their own name
      return { role: "admin" as AppRole, employee: matched && !isAgentRole(matched.role) ? matched : null, accessDenied: null }
    }
    if (superAdmin === null || employeesList === null) return none // still resolving

    if (matched) {
      if (isAdminRole(matched.role)) return { role: "admin" as AppRole, employee: matched, accessDenied: null }
      const status = (matched.status || "").toLowerCase()
      const deactivated = Boolean(matched.isDeleted) || status === "deactivated"
      const suspended = Boolean(matched.isBlocked) || status === "suspended"
      if (deactivated || suspended) {
        return {
          role: null,
          employee: matched,
          accessDenied:
            matched.blockedReason?.trim() ||
            (deactivated
              ? "Your account has been deactivated by the Admin. All your old records are safe."
              : "Your access has been temporarily suspended by the Admin. Please contact the office."),
        }
      }
      return { role: (isAgentRole(matched.role) ? "agent" : "staff") as AppRole, employee: matched, accessDenied: null }
    }

    return {
      role: null,
      employee: null,
      accessDenied: adminCheckFailed
        ? "We could not verify your access right now. Please check your internet and try again."
        : `${user.email} is not registered. Ask the Admin to add this email to your Staff or Sub Agent record.`,
    }
  }, [user, superAdmin, employeesList, adminCheckFailed])

  const loading =
    !authResolved ||
    (Boolean(user?.email) && superAdmin === null) ||
    (Boolean(user?.email) && !superAdmin && employeesList === null)

  const loginWithGoogle = async () => {
    setError(null)
    try {
      await signInWithPopup(auth, googleProvider)
    } catch (err: any) {
      console.error("Google sign in failed:", err)
      setError(err?.message || "Failed to sign in with Google")
    }
  }

  const logout = async () => {
    try {
      await fbSignOut(auth)
      setUser(null)
    } catch (err: any) {
      console.error("Logout failed:", err)
    }
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAdmin: access.role === "admin",
        role: access.role,
        employee: access.employee,
        accessDenied: access.accessDenied,
        loginWithGoogle,
        logout,
        error,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider")
  }
  return context
}
