import { ref, uploadBytesResumable, getDownloadURL, deleteObject } from "firebase/storage"
import { storage } from "./firebase"

export interface UploadProgressCallback {
  (progress: number): void
}

/**
 * Uploads a file (image, PDF, document) to Firebase Cloud Storage.
 * @param file The browser File object to upload
 * @param folder Cloud storage directory path (e.g. "customers/CUST-101/kyc")
 * @param prefix File prefix (e.g. "aadhar", "gst", "shop", "visiting_card")
 * @param onProgress Optional callback receiving 0-100 percentage
 * @returns Permanent HTTPS download URL
 */
export async function uploadFileToFirebaseStorage(
  file: File,
  folder: string,
  prefix: string = "doc",
  onProgress?: UploadProgressCallback
): Promise<string> {
  const timestamp = Date.now()
  const randomSuffix = Math.random().toString(36).substring(2, 7)
  const extension = file.name.split('.').pop() || 'jpg'
  const safeFilename = `${prefix}_${timestamp}_${randomSuffix}.${extension}`
  const fileRef = ref(storage, `${folder}/${safeFilename}`)

  const uploadTask = uploadBytesResumable(fileRef, file, {
    contentType: file.type || 'image/jpeg',
  })

  return new Promise((resolve, reject) => {
    uploadTask.on(
      'state_changed',
      (snapshot) => {
        if (snapshot.totalBytes > 0) {
          const progress = (snapshot.bytesTransferred / snapshot.totalBytes) * 100
          if (onProgress) onProgress(Math.round(progress))
        }
      },
      (error) => {
        console.error("Firebase Storage Upload Error:", error)
        reject(error)
      },
      async () => {
        try {
          const downloadUrl = await getDownloadURL(uploadTask.snapshot.ref)
          resolve(downloadUrl)
        } catch (err) {
          reject(err)
        }
      }
    )
  })
}

/**
 * Attempts to delete a file from Firebase Cloud Storage by its full download URL.
 */
export async function deleteFileFromFirebaseStorage(fileUrl: string): Promise<void> {
  if (!fileUrl || !fileUrl.includes("firebasestorage.googleapis.com")) return
  try {
    const fileRef = ref(storage, fileUrl)
    await deleteObject(fileRef)
  } catch (err) {
    console.warn("Could not delete file from Firebase Storage (may have been deleted or expired):", err)
  }
}
