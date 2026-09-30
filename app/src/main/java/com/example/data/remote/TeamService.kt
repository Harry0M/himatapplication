package com.example.data.remote

import com.example.data.local.entity.EmployeeEntity
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Adding people, decided by the office.
 *
 * Staff records cannot be written from a phone: the database rules make the `employees` node
 * owner-only, because the `role` field on it is what both apps read to decide who is an admin. When
 * clients could write that node, any account could hand itself the Admin role with one request.
 *
 * So "who may create whom" is answered server-side, by a Cloud Function:
 *   Admin     -> Admin, Staff, Sub Agent
 *   Staff     -> Staff, Sub Agent
 *   Sub Agent -> nobody
 *
 * The function also issues the staff code, which removes the last way two people could end up sharing
 * one: two phones adding somebody at the same moment now get their numbers from the same place.
 */
class TeamService(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) {

    /** What the office allows this account to do, so a form only offers what will be accepted. */
    data class Permissions(
        val role: String = "",
        val isOwner: Boolean = false,
        val canCreate: List<String> = emptyList(),
        val canSetLoginEmail: Boolean = false,
        val canEditExisting: Boolean = false
    )

    data class SavedMember(
        val id: Long,
        val employeeId: String,
        val role: String,
        /** True when the code asked for was taken and the office issued a different one. */
        val codeChanged: Boolean
    )

    suspend fun myPermissions(timeoutMs: Long = 12000L): Result<Permissions> = withContext(Dispatchers.IO) {
        call("myCreatableRoles", emptyMap(), timeoutMs).map { data ->
            @Suppress("UNCHECKED_CAST")
            Permissions(
                role = data["role"] as? String ?: "",
                isOwner = data["isOwner"] as? Boolean ?: false,
                canCreate = (data["canCreate"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                canSetLoginEmail = data["canSetLoginEmail"] as? Boolean ?: false,
                canEditExisting = data["canEditExisting"] as? Boolean ?: false
            )
        }
    }

    /**
     * Creates or updates a person. The office decides whether the role is allowed and what code they get.
     *
     * A failure comes back with the office's own wording ("Only an Admin can make somebody an Admin"),
     * because that is more use to the person in front of the screen than a generic refusal.
     */
    suspend fun saveMember(employee: EmployeeEntity, timeoutMs: Long = 20000L): Result<SavedMember> =
        withContext(Dispatchers.IO) {
            val payload = mutableMapOf<String, Any>(
                "role" to employee.role,
                "name" to employee.name,
                "employeeId" to employee.employeeId,
                "phone" to employee.phone,
                "phone2" to employee.phone2,
                "phone3" to employee.phone3,
                "phone4" to employee.phone4,
                "phone5" to employee.phone5,
                "email" to employee.email,
                "alternateEmail" to employee.alternateEmail,
                "firmName" to employee.firmName,
                "city" to employee.city,
                "notes" to employee.notes,
                "address" to employee.address,
                "currentAddress" to employee.currentAddress,
                "permanentAddress" to employee.permanentAddress,
                "personalLocation" to employee.personalLocation,
                "emergencyContactName" to employee.emergencyContactName,
                "emergencyContactPhone" to employee.emergencyContactPhone,
                "referredBy" to employee.referredBy,
                "referredByType" to employee.referredByType,
                "assignedMarkets" to employee.assignedMarkets,
                "markets" to employee.markets,
                "photoUri" to employee.photoUri
            )
            if (employee.id > 0L) payload["id"] = employee.id

            call("saveTeamMember", payload, timeoutMs).map { data ->
                SavedMember(
                    id = (data["id"] as? Number)?.toLong() ?: employee.id,
                    employeeId = data["employeeId"] as? String ?: employee.employeeId,
                    role = data["role"] as? String ?: employee.role,
                    codeChanged = data["codeChanged"] as? Boolean ?: false
                )
            }
        }

    private suspend fun call(
        name: String,
        payload: Map<String, Any>,
        timeoutMs: Long
    ): Result<Map<String, Any?>> {
        val result = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Result<Map<String, Any?>>> { cont ->
                functions.getHttpsCallable(name).call(payload)
                    .addOnSuccessListener { response ->
                        @Suppress("UNCHECKED_CAST")
                        val data = response.data as? Map<String, Any?>
                        if (cont.isActive) {
                            cont.resumeWith(
                                Result.success(
                                    if (data == null) Result.failure(IllegalStateException("The office sent nothing back"))
                                    else Result.success(data)
                                )
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        val message = (e as? FirebaseFunctionsException)?.message
                            ?: e.message
                            ?: "Could not reach the office"
                        android.util.Log.w("TeamService", "$name failed: $message")
                        if (cont.isActive) cont.resumeWith(Result.success(Result.failure(Exception(message))))
                    }
            }
        }
        return result ?: Result.failure(IllegalStateException("The office did not answer. Check your connection."))
    }
}
