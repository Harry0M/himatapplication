package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID
import kotlin.coroutines.resume

class FirebaseStorageService(
    private val bucketUrl: String = "gs://himatsms.firebasestorage.app"
) {
    companion object {
        private const val TAG = "FirebaseStorageService"
    }

    private val storage: FirebaseStorage by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Default FirebaseStorage instance failed, falling back to $bucketUrl", e)
            FirebaseStorage.getInstance(bucketUrl)
        }
    }

    /**
     * Uploads an image or document from an Android content URI directly to Firebase Cloud Storage.
     * @param context Android context to access ContentResolver
     * @param fileUri Content URI (e.g. content://... or file://...) from Photo Picker or Camera
     * @param folder Remote storage directory (e.g. "customers/CUST-101/kyc")
     * @param prefix File prefix (e.g. "aadhar", "gst", "shop", "visiting_card", "brand_logo")
     * @return Result containing the permanent public HTTPS download URL
     */
    suspend fun uploadFile(
        context: Context,
        fileUri: Uri,
        folder: String,
        prefix: String = "doc"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(fileUri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                mimeType.contains("pdf") -> "pdf"
                else -> "jpg"
            }

            val timestamp = System.currentTimeMillis()
            val randomSuffix = UUID.randomUUID().toString().take(6)
            val cleanFolder = folder.trim().trim('/')
            val filename = "${prefix}_${timestamp}_${randomSuffix}.$extension"
            val fileRef = storage.reference.child("$cleanFolder/$filename")

            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .build()

            // Try upload via putFile (recommended for Android Uris)
            // with fallback to putStream if putFile throws an exception
            val uploadSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
                val uploadTask = try {
                    fileRef.putFile(fileUri, metadata)
                } catch (e: Exception) {
                    Log.w(TAG, "putFile threw exception, attempting putStream fallback", e)
                    val stream: InputStream = contentResolver.openInputStream(fileUri)
                        ?: throw Exception("Unable to open input stream for URI: $fileUri")
                    fileRef.putStream(stream, metadata)
                }

                uploadTask.addOnSuccessListener {
                    Log.d(TAG, "Upload succeeded for $cleanFolder/$filename")
                    if (continuation.isActive) continuation.resume(true)
                }.addOnFailureListener { exc ->
                    Log.e(TAG, "Upload failed for $cleanFolder/$filename", exc)
                    if (continuation.isActive) continuation.resumeWith(kotlin.Result.failure(exc))
                }
            }

            if (!uploadSuccess) {
                return@withContext Result.failure(Exception("Upload task returned false"))
            }

            // Retrieve permanent download URL
            val downloadUrl = suspendCancellableCoroutine<String> { continuation ->
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    Log.d(TAG, "Obtained download URL: $uri")
                    if (continuation.isActive) continuation.resume(uri.toString())
                }.addOnFailureListener { exc ->
                    Log.e(TAG, "Failed to get download URL for $filename", exc)
                    if (continuation.isActive) continuation.resumeWith(kotlin.Result.failure(exc))
                }
            }

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "uploadFile error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a file from Firebase Cloud Storage by its download URL.
     */
    suspend fun deleteFile(fileUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (fileUrl.isBlank() || !fileUrl.contains("firebasestorage.googleapis.com")) {
            return@withContext Result.success(Unit)
        }
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                val fileRef = storage.getReferenceFromUrl(fileUrl)
                fileRef.delete().addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }.addOnFailureListener {
                    // File may already be deleted, proceed gracefully
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
