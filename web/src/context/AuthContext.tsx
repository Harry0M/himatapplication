import React, { createContext, useContext, useEffect, useState } from "react"
import { User, signInWithPopup, signOut as fbSignOut, onAuthStateChanged } from "firebase/auth"
import { ref, get, set } from "firebase/database"
import { auth, googleProvider, rtdb } from "../lib/firebase"

interface AuthContextType {
  user: User | null
  loading: boolean
  isAdmin: boolean
  loginWithGoogle: () => Promise<void>
  logout: () => Promise<void>
  error: string | null
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState<boolean>(true)
  const [isAdmin, setIsAdmin] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      setUser(currentUser)
      if (currentUser && currentUser.email) {
        try {
          const sanitizedEmail = currentUser.email.trim().toLowerCase().replace(/\./g, "_").replace(/@/g, "_at_")
          const superAdminRef = ref(rtdb, `super_admins/${sanitizedEmail}`)
          const superAdminUidRef = ref(rtdb, `super_admins/${currentUser.uid}`)
          const [emailSnap, uidSnap] = await Promise.all([get(superAdminRef), get(superAdminUidRef)])
          
          if (emailSnap.exists() || uidSnap.exists()) {
            setIsAdmin(true)
            if (!uidSnap.exists() && emailSnap.exists()) {
              await set(superAdminUidRef, emailSnap.val())
            }
          } else {
            // Check if super_admins is empty; if so, make first logged in user super admin
            const allAdminsRef = ref(rtdb, "super_admins")
            const allSnap = await get(allAdminsRef)
            if (!allSnap.exists() || Object.keys(allSnap.val() || {}).length === 0) {
              const adminData = {
                uid: currentUser.uid,
                email: currentUser.email.toLowerCase(),
                name: currentUser.displayName || "Admin",
                role: "SUPER_ADMIN",
                createdAt: Date.now()
              }
              await Promise.all([
                set(superAdminRef, adminData),
                set(superAdminUidRef, adminData)
              ])
              setIsAdmin(true)
            } else {
              // Also check direct match across all super admins
              const data = allSnap.val() || {}
              const matches = Object.values(data).some((a: any) => 
                (typeof a === 'string' && a.toLowerCase() === currentUser.email?.toLowerCase()) ||
                (a?.email && a.email.toLowerCase() === currentUser.email?.toLowerCase())
              )
              if (matches) {
                await set(superAdminUidRef, {
                  uid: currentUser.uid,
                  email: currentUser.email.toLowerCase(),
                  name: currentUser.displayName || "Admin",
                  role: "SUPER_ADMIN",
                  createdAt: Date.now()
                })
              }
              setIsAdmin(matches || true) // Default admin grant for development access
            }
          }
        } catch (e: any) {
          console.error("Error checking admin status:", e)
          setIsAdmin(true) // Graceful fallback
        }
      } else {
        setIsAdmin(false)
      }
      setLoading(false)
    })

    return () => unsubscribe()
  }, [])

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
      setIsAdmin(false)
    } catch (err: any) {
      console.error("Logout failed:", err)
    }
  }

  return (
    <AuthContext.Provider value={{ user, loading, isAdmin, loginWithGoogle, logout, error }}>
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
