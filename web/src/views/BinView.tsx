import React, { useCallback, useEffect, useMemo, useState } from "react"
import { onValue, ref } from "firebase/database"
import { getFunctions, httpsCallable } from "firebase/functions"
import { AlertTriangle, ChevronDown, ChevronRight, KeyRound, Loader2, RefreshCw, ShieldX, Trash2 } from "lucide-react"
import { rtdb } from "../lib/firebase"
import { useAuth } from "../context/AuthContext"
import { Button } from "../components/ui/Button"
import { Card } from "../components/ui/Card"
import { Input } from "../components/ui/Input"

/**
 * The bin: what is left of a record after it was really deleted.
 *
 * Three things about this screen are deliberate and will otherwise look like missing features:
 *
 *  - There is no Restore button. Nothing comes back out of the bin. It is the final place, which is
 *    the only reason it can safely be separate from the working data.
 *  - Nothing expires. There is no 30-day cleanup, here or on the server.
 *  - Emptying needs a password that is checked on the server, by a Cloud Function, against a hash in
 *    a node no browser can read. Checking it here would be theatre: anyone could skip the page.
 *
 * The bin lives on the web only. The Android app carries a link to it and nothing else.
 */

/** Folder labels, in the order they are worth looking at. */
const FOLDER_LABELS: Record<string, string> = {
  purchase_entries: "Orders",
  visits: "Trips",
  customers: "Customers",
  suppliers: "Suppliers",
  cheques_pdc: "Cheques / PDC",
  products: "Products",
  brands: "Brands",
  transporters: "Transporters",
  markets: "Markets",
  leads: "Leads",
  employees: "Staff",
}

interface BinEntry {
  id: string
  collection: string
  itemId: number | string
  summary?: string
  deletedAt?: number
  deletedBy?: string
  deletedByEmail?: string
  deletedByRole?: string
  deletionReason?: string
  record?: Record<string, unknown>
}

function formatWhen(ms?: number): string {
  if (!ms) return "Unknown date"
  return new Date(ms).toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

export function BinView() {
  const { isAdmin } = useAuth()

  const [entries, setEntries] = useState<BinEntry[] | null>(null)
  const [readError, setReadError] = useState<string | null>(null)
  const [openFolders, setOpenFolders] = useState<Set<string>>(new Set())
  const [openRecord, setOpenRecord] = useState<string | null>(null)

  const [passwordSet, setPasswordSet] = useState<boolean | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [message, setMessage] = useState<{ kind: "ok" | "err"; text: string } | null>(null)

  // Empty dialog
  const [emptyTarget, setEmptyTarget] = useState<{ collection?: string; itemId?: string; label: string } | null>(null)
  const [password, setPassword] = useState("")

  // Set / change password
  const [showPasswordForm, setShowPasswordForm] = useState(false)
  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")

  const functions = useMemo(() => getFunctions(undefined, "us-central1"), [])

  const loadStatus = useCallback(async () => {
    if (!isAdmin) return
    try {
      const res: any = await httpsCallable(functions, "binStatus")({})
      setPasswordSet(Boolean(res.data?.passwordSet))
    } catch (e: any) {
      setMessage({ kind: "err", text: e?.message || "Could not read the bin status." })
    }
  }, [functions, isAdmin])

  useEffect(() => {
    loadStatus()
  }, [loadStatus])

  // The bin node itself is readable by owners, so the list is live
  useEffect(() => {
    if (!isAdmin) return
    const unsub = onValue(
      ref(rtdb, "bin"),
      (snap) => {
        const list: BinEntry[] = []
        snap.forEach((folder) => {
          folder.forEach((child) => {
            const val = (child.val() || {}) as Omit<BinEntry, "id">
            list.push({ ...val, id: `${folder.key}/${child.key}`, collection: folder.key || "", itemId: child.key || "" })
            return undefined
          })
          return undefined
        })
        list.sort((a, b) => (b.deletedAt || 0) - (a.deletedAt || 0))
        setEntries(list)
        setReadError(null)
      },
      (err) => {
        console.error("RTDB error reading bin:", err)
        setReadError(err.message)
        setEntries([])
      }
    )
    return () => unsub()
  }, [isAdmin])

  const byFolder = useMemo(() => {
    const map = new Map<string, BinEntry[]>()
    ;(entries || []).forEach((e) => {
      const list = map.get(e.collection) || []
      list.push(e)
      map.set(e.collection, list)
    })
    return Array.from(map.entries()).sort((a, b) => {
      const order = Object.keys(FOLDER_LABELS)
      return order.indexOf(a[0]) - order.indexOf(b[0])
    })
  }, [entries])

  const toggleFolder = (name: string) => {
    setOpenFolders((prev) => {
      const next = new Set(prev)
      if (next.has(name)) next.delete(name)
      else next.add(name)
      return next
    })
  }

  const handleSavePassword = async () => {
    setMessage(null)
    if (newPassword.length < 8) {
      setMessage({ kind: "err", text: "Use at least 8 characters." })
      return
    }
    if (newPassword !== confirmPassword) {
      setMessage({ kind: "err", text: "The two new passwords do not match." })
      return
    }
    setBusy("password")
    try {
      await httpsCallable(functions, "setBinPassword")({ newPassword, currentPassword })
      setMessage({ kind: "ok", text: passwordSet ? "Bin password changed." : "Bin password set." })
      setShowPasswordForm(false)
      setCurrentPassword("")
      setNewPassword("")
      setConfirmPassword("")
      await loadStatus()
    } catch (e: any) {
      setMessage({ kind: "err", text: e?.message || "Could not save the password." })
    } finally {
      setBusy(null)
    }
  }

  const handleEmpty = async () => {
    if (!emptyTarget) return
    setMessage(null)
    setBusy("empty")
    try {
      const res: any = await httpsCallable(functions, "emptyBin")({
        password,
        collection: emptyTarget.collection,
        itemId: emptyTarget.itemId,
      })
      setMessage({ kind: "ok", text: `Removed ${res.data?.removed ?? 0} record(s) for good.` })
      setEmptyTarget(null)
      setPassword("")
    } catch (e: any) {
      setMessage({ kind: "err", text: e?.message || "Could not empty the bin." })
    } finally {
      setBusy(null)
    }
  }

  if (!isAdmin) {
    return (
      <Card className="p-8 text-center">
        <ShieldX className="mx-auto mb-3 h-8 w-8 text-red-500" aria-hidden="true" />
        <h2 className="text-base font-bold text-zinc-900 dark:text-zinc-50">Admins only</h2>
        <p className="mt-1 text-xs text-muted-foreground">
          The bin holds records that have been deleted for good. Only an Admin can open it.
        </p>
      </Card>
    )
  }

  const total = entries?.length ?? 0

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="flex items-center gap-2 text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
            <Trash2 className="h-5 w-5" aria-hidden="true" />
            Bin
          </h2>
          <p className="mt-0.5 max-w-2xl text-xs text-muted-foreground">
            A copy of every record that was deleted for good, kept apart from the working data.{" "}
            <strong>Nothing here can be restored, and nothing expires on its own.</strong> Emptying it needs the bin
            password.
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <Button shape="pill" variant="outline" size="sm" onClick={loadStatus} className="h-8 gap-1 px-3 text-xs">
            <RefreshCw className="h-3.5 w-3.5" aria-hidden="true" />
            Refresh
          </Button>
          <Button
            shape="pill"
            variant="outline"
            size="sm"
            onClick={() => setShowPasswordForm((v) => !v)}
            className="h-8 gap-1 px-3 text-xs"
          >
            <KeyRound className="h-3.5 w-3.5" aria-hidden="true" />
            {passwordSet ? "Change password" : "Set password"}
          </Button>
          {total > 0 && (
            <Button
              shape="pill"
              variant="destructive"
              size="sm"
              onClick={() => setEmptyTarget({ label: `everything in the bin (${total} record(s))` })}
              className="h-8 gap-1 px-3 text-xs"
            >
              <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
              Empty bin
            </Button>
          )}
        </div>
      </div>

      {message && (
        <div
          className={
            message.kind === "ok"
              ? "rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs font-medium text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-200"
              : "rounded-xl border border-red-200 bg-red-50 p-3 text-xs font-medium text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200"
          }
          role="status"
        >
          {message.text}
        </div>
      )}

      {passwordSet === false && (
        <div className="flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3 dark:border-amber-900 dark:bg-amber-950/40">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" aria-hidden="true" />
          <p className="text-xs font-medium text-amber-900 dark:text-amber-200">
            No bin password has been set yet, so the bin cannot be emptied. There is deliberately no default password —
            set one now and keep it somewhere safe.
          </p>
        </div>
      )}

      {showPasswordForm && (
        <Card className="space-y-3 p-4">
          <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">
            {passwordSet ? "Change the bin password" : "Set the bin password"}
          </h3>
          <p className="text-xs text-muted-foreground">
            This is the only thing standing between a mistaken click and records disappearing for good. It is stored as a
            one-way hash on the server — if it is lost, it can only be reset by an Admin who knows the current one.
          </p>
          <div className="grid gap-3 sm:grid-cols-3">
            {passwordSet && (
              <Input
                type="password"
                placeholder="Current password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                autoComplete="current-password"
              />
            )}
            <Input
              type="password"
              placeholder="New password (8+ characters)"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              autoComplete="new-password"
            />
            <Input
              type="password"
              placeholder="Repeat new password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              autoComplete="new-password"
            />
          </div>
          <div className="flex gap-2">
            <Button shape="pill" size="sm" onClick={handleSavePassword} disabled={busy === "password"} className="gap-1">
              {busy === "password" && <Loader2 className="h-3.5 w-3.5 animate-spin" aria-hidden="true" />}
              Save
            </Button>
            <Button shape="pill" variant="outline" size="sm" onClick={() => setShowPasswordForm(false)}>
              Cancel
            </Button>
          </div>
        </Card>
      )}

      {readError && (
        <Card className="p-4 text-xs text-red-700 dark:text-red-300">
          Could not read the bin: {readError}
        </Card>
      )}

      {entries === null && (
        <div className="flex items-center justify-center py-12" role="status">
          <Loader2 className="h-5 w-5 animate-spin text-zinc-500" aria-hidden="true" />
        </div>
      )}

      {entries !== null && total === 0 && !readError && (
        <Card className="p-8 text-center">
          <Trash2 className="mx-auto mb-3 h-8 w-8 text-zinc-400" aria-hidden="true" />
          <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">The bin is empty</h3>
          <p className="mt-1 text-xs text-muted-foreground">
            Records appear here after an Admin deletes them outright, or approves somebody else's deletion request.
          </p>
        </Card>
      )}

      {byFolder.map(([folder, list]) => {
        const isOpen = openFolders.has(folder)
        return (
          <Card key={folder} className="overflow-hidden">
            <button
              type="button"
              onClick={() => toggleFolder(folder)}
              className="flex w-full items-center justify-between gap-3 p-4 text-left hover:bg-zinc-50 dark:hover:bg-zinc-900"
              aria-expanded={isOpen}
            >
              <span className="flex items-center gap-2">
                {isOpen ? (
                  <ChevronDown className="h-4 w-4 text-zinc-500" aria-hidden="true" />
                ) : (
                  <ChevronRight className="h-4 w-4 text-zinc-500" aria-hidden="true" />
                )}
                <span className="text-sm font-bold text-zinc-900 dark:text-zinc-50">
                  {FOLDER_LABELS[folder] || folder}
                </span>
                <span className="rounded-full bg-zinc-100 px-2 py-0.5 text-[11px] font-semibold text-zinc-600 dark:bg-zinc-800 dark:text-zinc-300">
                  {list.length}
                </span>
              </span>
              <Button
                shape="pill"
                variant="outline"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation()
                  setEmptyTarget({
                    collection: folder,
                    label: `all ${list.length} record(s) under ${FOLDER_LABELS[folder] || folder}`,
                  })
                }}
                className="h-7 px-2.5 text-[11px]"
              >
                Empty folder
              </Button>
            </button>

            {isOpen && (
              <div className="divide-y divide-zinc-100 border-t border-zinc-100 dark:divide-zinc-800 dark:border-zinc-800">
                {list.map((entry) => (
                  <div key={entry.id} className="p-4">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-zinc-900 dark:text-zinc-50">
                          {entry.summary || `${folder} ${entry.itemId}`}
                        </p>
                        <p className="mt-0.5 text-[11px] text-muted-foreground">
                          Deleted {formatWhen(entry.deletedAt)} by {entry.deletedBy || "Unknown"}
                          {entry.deletedByRole ? ` (${entry.deletedByRole})` : ""}
                          {entry.deletedByEmail ? ` • ${entry.deletedByEmail}` : ""}
                        </p>
                        {entry.deletionReason && (
                          <p className="mt-0.5 text-[11px] italic text-muted-foreground">"{entry.deletionReason}"</p>
                        )}
                      </div>
                      <div className="flex shrink-0 items-center gap-2">
                        <Button
                          shape="pill"
                          variant="outline"
                          size="sm"
                          onClick={() => setOpenRecord(openRecord === entry.id ? null : entry.id)}
                          className="h-7 px-2.5 text-[11px]"
                        >
                          {openRecord === entry.id ? "Hide details" : "View details"}
                        </Button>
                        <Button
                          shape="pill"
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            setEmptyTarget({
                              collection: folder,
                              itemId: String(entry.itemId),
                              label: entry.summary || `${folder} ${entry.itemId}`,
                            })
                          }
                          className="h-7 px-2.5 text-[11px] text-red-600"
                        >
                          Remove
                        </Button>
                      </div>
                    </div>

                    {openRecord === entry.id && (
                      <pre className="mt-3 max-h-80 overflow-auto rounded-xl bg-zinc-50 p-3 text-[11px] leading-relaxed text-zinc-700 dark:bg-zinc-900 dark:text-zinc-300">
                        {JSON.stringify(entry.record ?? {}, null, 2)}
                      </pre>
                    )}
                  </div>
                ))}
              </div>
            )}
          </Card>
        )
      })}

      {/* Empty confirmation — password required, checked on the server */}
      {emptyTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <Card className="w-full max-w-md space-y-4 p-6">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-red-50 text-red-600 dark:bg-red-950">
                <AlertTriangle className="h-5 w-5" aria-hidden="true" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-50">This cannot be undone</h3>
                <p className="mt-1 text-xs text-muted-foreground">
                  You are about to remove <strong>{emptyTarget.label}</strong>. There is no restore from here — once this
                  goes, the only copy is gone.
                </p>
              </div>
            </div>

            <Input
              type="password"
              placeholder="Bin password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="off"
              autoFocus
            />

            <div className="flex justify-end gap-2">
              <Button
                shape="pill"
                variant="outline"
                size="sm"
                onClick={() => {
                  setEmptyTarget(null)
                  setPassword("")
                }}
              >
                Keep them
              </Button>
              <Button
                shape="pill"
                variant="destructive"
                size="sm"
                onClick={handleEmpty}
                disabled={busy === "empty" || password.length === 0}
                className="gap-1"
              >
                {busy === "empty" && <Loader2 className="h-3.5 w-3.5 animate-spin" aria-hidden="true" />}
                Remove for good
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  )
}
