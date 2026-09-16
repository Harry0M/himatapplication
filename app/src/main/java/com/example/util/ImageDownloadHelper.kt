package com.example.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

object ImageDownloadHelper {
    private const val TAG = "ImageDownloadHelper"

    /**
     * Downloads or saves an image to the device's public Downloads / Pictures directory.
     * Supports both remote HTTPS URLs (Firebase Storage) and local content:// or file:// URIs.
     */
    suspend fun downloadOrSaveImage(
        context: Context,
        imageUriOrUrl: String,
        title: String = "Image"
    ): Boolean = withContext(Dispatchers.IO) {
        if (imageUriOrUrl.isBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No image to download", Toast.LENGTH_SHORT).show()
            }
            return@withContext false
        }

        val cleanTitle = title.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val timestamp = System.currentTimeMillis()
        val extension = when {
            imageUriOrUrl.contains(".png", ignoreCase = true) -> "png"
            imageUriOrUrl.contains(".webp", ignoreCase = true) -> "webp"
            imageUriOrUrl.contains(".pdf", ignoreCase = true) -> "pdf"
            else -> "jpg"
        }
        val filename = "${cleanTitle}_$timestamp.$extension"

        try {
            if (imageUriOrUrl.startsWith("http://", ignoreCase = true) ||
                imageUriOrUrl.startsWith("https://", ignoreCase = true)
            ) {
                // Remote URL -> Use Android DownloadManager
                try {
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    if (downloadManager != null) {
                        val request = DownloadManager.Request(Uri.parse(imageUriOrUrl))
                            .setTitle(cleanTitle)
                            .setDescription("Downloading $filename")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)

                        downloadManager.enqueue(request)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Downloading to Downloads folder: $filename", Toast.LENGTH_LONG).show()
                        }
                        return@withContext true
                    }
                } catch (dmEx: Exception) {
                    Log.w(TAG, "DownloadManager enqueue failed, falling back to direct stream save", dmEx)
                }

                // Fallback: download via URL stream and save to MediaStore / Downloads
                val connection = URL(imageUriOrUrl).openConnection()
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                val inputStream = connection.getInputStream()
                val saved = saveStreamToStorage(context, inputStream, filename, extension)
                withContext(Dispatchers.Main) {
                    if (saved) {
                        Toast.makeText(context, "Saved to Downloads: $filename", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
                return@withContext saved
            } else {
                // Local content:// or file:// URI
                val localUri = Uri.parse(imageUriOrUrl)
                val inputStream = context.contentResolver.openInputStream(localUri)
                    ?: return@withContext false
                val saved = saveStreamToStorage(context, inputStream, filename, extension)
                withContext(Dispatchers.Main) {
                    if (saved) {
                        Toast.makeText(context, "Saved to Downloads: $filename", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
                return@withContext saved
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download/save image", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            return@withContext false
        }
    }

    private fun saveStreamToStorage(
        context: Context,
        inputStream: InputStream,
        filename: String,
        extension: String
    ): Boolean {
        return try {
            val mimeType = when (extension) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "pdf" -> "application/pdf"
                else -> "image/jpeg"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return false
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    inputStream.copyTo(out)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, filename)
                FileOutputStream(targetFile).use { out ->
                    inputStream.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "saveStreamToStorage error", e)
            false
        } finally {
            try { inputStream.close() } catch (_: Exception) {}
        }
    }
}
