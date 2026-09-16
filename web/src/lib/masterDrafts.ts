/**
 * Purely local draft persistence using browser localStorage.
 * Master creation drafts are stored locally on the user's device only
 * and never sent to Firebase or the backend until explicitly submitted.
 */

const DRAFT_PREFIX = "himat_master_draft_"

export function saveMasterDraft<T = Record<string, any>>(masterType: string, data: T): void {
  try {
    localStorage.setItem(`${DRAFT_PREFIX}${masterType}`, JSON.stringify(data))
  } catch (err) {
    console.warn(`Failed to save local draft for ${masterType}:`, err)
  }
}

export function getMasterDraft<T = Record<string, any>>(masterType: string): T | null {
  try {
    const raw = localStorage.getItem(`${DRAFT_PREFIX}${masterType}`)
    if (!raw) return null
    return JSON.parse(raw) as T
  } catch (err) {
    console.warn(`Failed to parse local draft for ${masterType}:`, err)
    return null
  }
}

export function hasMasterDraft(masterType: string): boolean {
  try {
    return localStorage.getItem(`${DRAFT_PREFIX}${masterType}`) !== null
  } catch {
    return false
  }
}

export function clearMasterDraft(masterType: string): void {
  try {
    localStorage.removeItem(`${DRAFT_PREFIX}${masterType}`)
  } catch (err) {
    console.warn(`Failed to clear local draft for ${masterType}:`, err)
  }
}
