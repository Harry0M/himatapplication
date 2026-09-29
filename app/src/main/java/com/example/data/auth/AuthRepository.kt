package com.example.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val WEB_CLIENT_ID = "787473572186-6f99r3emcgmta13l2qi04408jspapl6e.apps.googleusercontent.com"
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
        return try {
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).awaitTask()
                val user = authResult.user

                if (user != null) {
                    _currentUser.value = user
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to obtain user session from Firebase"))
                }
            } else {
                Result.failure(Exception("Unrecognized credential type received from Google"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Sign-in cancelled"))
        } catch (e: GetCredentialException) {
            val sha1List = getAppSha1Fingerprints(activity)
            android.util.Log.e("AuthRepository", "Google Sign-in failed. App SHA-1: $sha1List", e)
            val detailedMsg = parseCredentialError(e, sha1List)
            Result.failure(Exception(detailedMsg))
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Sign-in error", e)
            Result.failure(e)
        }
    }

    private fun parseCredentialError(e: GetCredentialException, sha1List: List<String>): String {
        val msg = e.localizedMessage ?: e.message ?: ""
        val activeSha1 = sha1List.firstOrNull() ?: "Unknown"
        return when {
            e is androidx.credentials.exceptions.NoCredentialException || msg.contains("No credentials available", ignoreCase = true) -> {
                "No credentials available. Please ensure your signing SHA-1 ($activeSha1) is added to Firebase Console under com.aistudio.himattextile.sourcemgmt and a Google account is active on the phone."
            }
            msg.contains("GetCredentialResponse error returned from framework", ignoreCase = true) ||
            msg.contains("10:", ignoreCase = true) || msg.contains("DEVELOPER_ERROR", ignoreCase = true) -> {
                "Google Sign-in configuration error (Developer Error 10). Verify that your device build's SHA-1 fingerprint ($activeSha1) is registered in Firebase/Google Cloud Console for package com.aistudio.himattextile.sourcemgmt."
            }
            else -> {
                "Google Sign-in failed: $msg"
            }
        }
    }

    private fun getAppSha1Fingerprints(context: Context): List<String> {
        val fingerprints = mutableListOf<String>()
        try {
            val pm = context.packageManager
            val packageName = context.packageName
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val packageInfo = pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = packageInfo.signingInfo
                val certs = if (signingInfo != null && signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo?.signingCertificateHistory ?: emptyArray()
                }
                for (cert in certs) {
                    val md = java.security.MessageDigest.getInstance("SHA-1")
                    val digest = md.digest(cert.toByteArray())
                    fingerprints.add(digest.joinToString(":") { "%02X".format(it) })
                }
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val certs = packageInfo.signatures ?: emptyArray()
                for (cert in certs) {
                    val md = java.security.MessageDigest.getInstance("SHA-1")
                    val digest = md.digest(cert.toByteArray())
                    fingerprints.add(digest.joinToString(":") { "%02X".format(it) })
                }
            }
        } catch (ex: Exception) {
            android.util.Log.e("AuthRepository", "Failed to retrieve package signing SHA-1", ex)
        }
        return fingerprints
    }

    suspend fun signOut(context: Context) {
        try {
            auth.signOut()
            _currentUser.value = null
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Ignore sign-out cleanup exceptions
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
