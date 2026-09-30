package com.example.data.remote

import com.example.data.local.entity.CustomerRegistrationRequestEntity
import com.example.data.local.entity.SupplierRegistrationRequestEntity
import com.example.data.local.entity.LeadEntity
import com.example.data.local.entity.BrandEntity
import com.example.data.local.entity.ChequePdcEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.MarketEntity
import com.example.data.local.entity.PackGroupEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransporterEntity
import com.example.data.local.entity.VisitEntity
import com.example.util.AgencyProfile
import com.example.util.DeletionRequest
import com.example.util.WorkNotification
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import kotlin.coroutines.resume

class FirebaseRtdbService(
    private val databaseUrl: String = "https://himatsms-default-rtdb.firebaseio.com"
) {
    private val db: FirebaseDatabase by lazy {
        val instance = FirebaseDatabase.getInstance(databaseUrl)
        try {
            instance.setPersistenceEnabled(true)
        } catch (_: Exception) {
            // Persistence can only be set once
        }
        instance
    }

    private val rootRef: DatabaseReference
        get() = db.reference

    val pendingDeletionKeys: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private val activeListeners = Collections.synchronizedList(mutableListOf<Pair<Query, ValueEventListener>>())

    fun removeAllListeners() {
        synchronized(activeListeners) {
            for ((query, listener) in activeListeners) {
                try {
                    query.removeEventListener(listener)
                } catch (e: Exception) {
                    // Ignore
                }
            }
            activeListeners.clear()
        }
    }

    private fun registerListener(query: Query, listener: ValueEventListener): ValueEventListener {
        query.addValueEventListener(listener)
        synchronized(activeListeners) {
            activeListeners.add(Pair(query, listener))
        }
        return listener
    }

    /**
     * Write one record as a merge: every Android field plus [extra] (the web copies of the same data).
     * setValue replaced the whole record and wiped fields only the web admin keeps (security cheques,
     * contact designations, ...). Uses Firebase's own mapper, so key names are exactly what setValue wrote.
     */
    private fun DatabaseReference.mergeRecord(value: Any, extra: Map<String, Any?> = emptyMap()) {
        val map = WebFieldBridge.toMap(value)
        if (map.isEmpty()) {
            setValue(value)
            return
        }
        map.putAll(extra)
        updateChildren(map)
    }

    /**
     * Same merge, but waits for the server to accept it and says whether it did.
     *
     * Writes used to be fired and forgotten: a rejected or failed write left the record only on the
     * phone while the app happily showed it as saved. Callers that must not lose data use this and
     * keep the row marked as pending until it returns true.
     *
     * Note the timeout is generous — Firebase queues writes while offline, and the task only
     * completes once the server has actually acknowledged it.
     */
    private suspend fun DatabaseReference.mergeRecordConfirmed(
        value: Any,
        extra: Map<String, Any?> = emptyMap(),
        timeoutMs: Long = 20000L
    ): Boolean {
        val map = WebFieldBridge.toMap(value)
        map.putAll(extra)
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Boolean> { cont ->
                val task = if (map.isEmpty()) setValue(value) else updateChildren(map)
                task.addOnSuccessListener { if (cont.isActive) cont.resumeWith(Result.success(true)) }
                    .addOnFailureListener { e ->
                        android.util.Log.w("FirebaseRtdbService", "write to $this rejected: ${e.message}")
                        if (cont.isActive) cont.resumeWith(Result.success(false))
                    }
            }
        } ?: false
    }

    fun listenToDeletionRequests(onUpdate: ((Set<String>) -> Unit)? = null): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val set = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val status = child.child("status").getValue(String::class.java)
                    if (status == null || status == "PENDING_CONFIRMATION" || status == "PENDING") {
                        val key = child.key ?: continue
                        set.add(key)
                        val collection = child.child("collection").getValue(String::class.java) ?: ""
                        val itemId = child.child("itemId").getValue(Long::class.java) ?: 0L
                        if (collection.isNotBlank() && itemId > 0L) {
                            set.add("${collection}_$itemId")
                        }
                    }
                }
                pendingDeletionKeys.clear()
                pendingDeletionKeys.addAll(set)
                onUpdate?.invoke(set)
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "deletion_requests onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("deletion_requests"), listener)
    }

    suspend fun fetchDeletionRequestsKeys(): Set<String> = suspendCancellableCoroutine { cont ->
        rootRef.child("deletion_requests").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val set = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val status = child.child("status").getValue(String::class.java)
                    if (status == null || status == "PENDING_CONFIRMATION" || status == "PENDING") {
                        val key = child.key ?: continue
                        set.add(key)
                        val collection = child.child("collection").getValue(String::class.java) ?: ""
                        val itemId = child.child("itemId").getValue(Long::class.java) ?: 0L
                        if (collection.isNotBlank() && itemId > 0L) {
                            set.add("${collection}_$itemId")
                        }
                    }
                }
                pendingDeletionKeys.clear()
                pendingDeletionKeys.addAll(set)
                if (cont.isActive) cont.resumeWith(Result.success(set))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptySet()))
            }
        })
    }

    // -------------------------------------------------------------------------
    // Work notifications: "a new trip started", "a new order came in"
    // -------------------------------------------------------------------------

    /**
     * Announce something the rest of the team should know about. Fire-and-forget on purpose: a
     * missed announcement must never hold up or fail the actual save.
     */
    suspend fun postNotification(note: WorkNotification) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("notifications").child(note.id).setValue(note)
        } catch (_: Exception) {
        }
    }

    /**
     * Announces something only if nobody has announced it yet.
     *
     * For events several devices can notice independently — a customer's birthday being the obvious
     * one — the note key is the thing that makes it happen once. Checking first also leaves the
     * original `createdAt` alone, so the record still says when the team was actually told.
     */
    suspend fun postNotificationIfAbsent(note: WorkNotification) = withContext(Dispatchers.IO) {
        try {
            val ref = rootRef.child("notifications").child(note.id)
            val existing = suspendCancellableCoroutine<Boolean> { cont ->
                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (cont.isActive) cont.resumeWith(Result.success(snapshot.exists()))
                    }

                    // Could not tell: treat as present, because a missed reminder beats a duplicate
                    override fun onCancelled(error: DatabaseError) {
                        if (cont.isActive) cont.resumeWith(Result.success(true))
                    }
                })
            }
            if (!existing) ref.setValue(note)
        } catch (_: Exception) {
        }
    }

    /** Live announcements, newest 50 only, so an old database does not replay months of history. */
    fun listenToNotifications(onUpdate: (List<WorkNotification>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<WorkNotification>()
                for (child in snapshot.children) {
                    try {
                        val note = child.getValue(WorkNotification::class.java) ?: continue
                        list.add(if (note.id.isBlank()) note.copy(id = child.key.orEmpty()) else note)
                    } catch (_: Exception) {
                    }
                }
                onUpdate(list.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "notifications onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("notifications").orderByChild("createdAt").limitToLast(50), listener)
    }

    // -------------------------------------------------------------------------
    // Deletion requests: the admin console
    //
    // Staff cannot delete anything outright. Their delete marks the record `isDeleted` and files a
    // request here. An admin then either approves it (the record is really removed) or rejects it
    // (the record comes back exactly as it was).
    // -------------------------------------------------------------------------

    /** Every pending request, newest first, with enough detail to decide on it. */
    fun listenToDeletionRequestList(onUpdate: (List<DeletionRequest>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<DeletionRequest>()
                for (child in snapshot.children) {
                    val status = child.child("status").getValue(String::class.java).orEmpty()
                    if (status.equals("CONFIRMED", true) || status.equals("REJECTED", true)) continue
                    val collection = child.child("collection").getValue(String::class.java).orEmpty()
                    val itemId = child.child("itemId").getValue(Long::class.java) ?: 0L
                    if (collection.isBlank() || itemId <= 0L) continue
                    list.add(
                        DeletionRequest(
                            key = child.key.orEmpty().ifBlank { "${collection}_$itemId" },
                            collection = collection,
                            itemId = itemId,
                            itemSummary = child.child("itemSummary").getValue(String::class.java).orEmpty(),
                            deletedBy = child.child("deletedBy").getValue(String::class.java).orEmpty(),
                            deletedByEmail = child.child("deletedByEmail").getValue(String::class.java).orEmpty(),
                            deletedByRole = child.child("deletedByRole").getValue(String::class.java).orEmpty(),
                            deletedAt = child.child("deletedAt").getValue(Long::class.java) ?: 0L,
                            deletionReason = child.child("deletionReason").getValue(String::class.java).orEmpty()
                        )
                    )
                }
                onUpdate(list.sortedByDescending { it.deletedAt })
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "deletion_requests list onCancelled: ${error.message}")
            }
        }
        return registerListener(rootRef.child("deletion_requests"), listener)
    }

    /**
     * Admin approved: really remove the record, and for a trip its orders and pack groups too.
     * Returns false when the write was refused, so the caller can say so instead of pretending.
     */
    suspend fun approveDeletionRequest(
        request: DeletionRequest,
        linkedEntryIds: List<Long> = emptyList(),
        linkedPackGroupIds: List<Long> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val node = request.collection
            rootRef.child(node).child(request.itemId.toString()).removeValue()
            if (node == "suppliers") {
                rootRef.child("manufacturers").child(request.itemId.toString()).removeValue()
            }
            if (node == "visits") {
                linkedEntryIds.forEach { id ->
                    rootRef.child("purchase_entries").child(id.toString()).removeValue()
                    rootRef.child("deletion_requests").child("purchase_entries_$id").removeValue()
                }
                linkedPackGroupIds.forEach { id ->
                    rootRef.child("pack_groups").child(id.toString()).removeValue()
                    rootRef.child("deletion_requests").child("pack_groups_$id").removeValue()
                }
            }
            rootRef.child("deletion_requests").child(request.key).removeValue()
            rootRef.child("deletion_requests").child("${node}_${request.itemId}").removeValue()
            true
        } catch (e: Exception) {
            android.util.Log.w("FirebaseRtdbService", "approveDeletionRequest ${request.key} failed: ${e.message}")
            false
        }
    }

    /**
     * Admin rejected: put the record back and drop the request.
     *
     * Brands, transporters and markets also carry the web's own `deleted` / `isActive` / `active`
     * copies of the same flag, and the web lists filter on those too — clearing only `isDeleted`
     * used to leave a "restored" record still invisible, so all of them are reset here.
     */
    suspend fun rejectDeletionRequest(
        request: DeletionRequest,
        linkedEntryIds: List<Long> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val node = request.collection
            rootRef.child(node).child(request.itemId.toString()).updateChildren(restoreFields(node))
            if (node == "suppliers") {
                rootRef.child("manufacturers").child(request.itemId.toString())
                    .updateChildren(restoreFields(node))
            }
            if (node == "visits") {
                linkedEntryIds.forEach { id ->
                    rootRef.child("purchase_entries").child(id.toString())
                        .updateChildren(restoreFields("purchase_entries"))
                    rootRef.child("deletion_requests").child("purchase_entries_$id").removeValue()
                }
            }
            rootRef.child("deletion_requests").child(request.key).removeValue()
            rootRef.child("deletion_requests").child("${node}_${request.itemId}").removeValue()
            true
        } catch (e: Exception) {
            android.util.Log.w("FirebaseRtdbService", "rejectDeletionRequest ${request.key} failed: ${e.message}")
            false
        }
    }

    /** The exact set of fields that has to be cleared to make a record live again. */
    private fun restoreFields(collection: String): Map<String, Any?> {
        val base = mutableMapOf<String, Any?>(
            "isDeleted" to false,
            "deletedAt" to null,
            "deletedBy" to null,
            "deletedByEmail" to null,
            "deletedByRole" to null,
            "deletionStatus" to null,
            "deletionReason" to null
        )
        if (collection == "brands" || collection == "transporters" || collection == "markets") {
            base["deleted"] = false
            base["isActive"] = true
            base["active"] = true
        }
        return base
    }

    /**
     * Whether we are actually talking to the office right now, from the database's own
     * `.info/connected` flag. This is the real state, not a guess based on the last save.
     */
    /**
     * Are we actually talking to the office right now?
     *
     * `.info/connected` is the SDK's own socket state, answered locally, so this returns almost
     * immediately. Callers need it before deciding anything from the *absence* of data: with disk
     * persistence on, an offline read succeeds against a cold cache and looks exactly like an empty
     * database, which is not something to act on.
     */
    suspend fun isConnected(timeoutMs: Long = 5000L): Boolean = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine<Boolean> { cont ->
            val ref = db.getReference(".info/connected")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.getValue(Boolean::class.java) == true && cont.isActive) {
                        ref.removeEventListener(this)
                        cont.resumeWith(Result.success(true))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (cont.isActive) {
                        ref.removeEventListener(this)
                        cont.resumeWith(Result.success(false))
                    }
                }
            }
            ref.addValueEventListener(listener)
            cont.invokeOnCancellation { ref.removeEventListener(listener) }
        }
    } ?: false

    fun listenToConnection(onChange: (Boolean) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChange(snapshot.getValue(Boolean::class.java) == true)
            }

            override fun onCancelled(error: DatabaseError) {
                onChange(false)
            }
        }
        return registerListener(db.getReference(".info/connected"), listener)
    }

    fun sanitizeEmail(email: String): String {
        return email.trim().lowercase().replace(".", "_").replace("@", "_at_")
    }

    // -------------------------------------------------------------------------
    // The agency's own details, printed on every document
    // -------------------------------------------------------------------------

    /** Live agency profile. Every device keeps a local copy so PDFs print the same footer offline. */
    fun listenToAgencyProfile(onUpdate: (AgencyProfile) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                fun str(key: String) = snapshot.child(key).getValue(String::class.java).orEmpty()
                onUpdate(
                    AgencyProfile(
                        businessName = str("businessName").ifBlank { AgencyProfile.DEFAULT_NAME },
                        tagline = str("tagline").ifBlank { AgencyProfile.DEFAULT_TAGLINE },
                        website = str("website"),
                        phone = str("phone"),
                        whatsapp = str("whatsapp"),
                        email = str("email"),
                        address = str("address"),
                        gstin = str("gstin"),
                        instagram = str("instagram"),
                        facebook = str("facebook"),
                        linkedin = str("linkedin"),
                        youtube = str("youtube"),
                        upiId = str("upiId")
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "settings/agency onCancelled: ${error.message}")
            }
        }
        return registerListener(
            rootRef.child(AgencyProfile.NODE).child(AgencyProfile.CHILD),
            listener
        )
    }

    /** Saves the agency profile. Owner-only by the rules: it appears on every customer document. */
    suspend fun saveAgencyProfile(profile: AgencyProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            rootRef.child(AgencyProfile.NODE).child(AgencyProfile.CHILD)
                .mergeRecordConfirmed(profile)
        } catch (e: Exception) {
            android.util.Log.w("FirebaseRtdbService", "saveAgencyProfile failed: ${e.message}")
            false
        }
    }

    // -------------------------------------------------------------------------
    // Membership: is this account one of ours, and what is it allowed to be?
    // -------------------------------------------------------------------------

    /**
     * What the office says about one login. Read from `members/<emailKey>`, which is mirrored from
     * the staff list by a Cloud Function and cannot be written by any app.
     */
    data class Membership(
        val employeeId: Long = 0,
        val name: String = "",
        val role: String = "",
        val active: Boolean = false
    )

    /**
     * Asks the office about the signed-in account, and says plainly when the answer is "nobody".
     *
     * [Membership] null means this email is not on the staff list. That is different from "could not
     * ask", which is why this returns a [Result]: refusing somebody because the network was down
     * would lock out a salesman with no signal, and letting somebody in because the check failed is
     * the hole this whole node exists to close.
     *
     * The rules let an account read only its own entry, so this leaks nothing and works even for a
     * complete stranger — they simply get null instead of a permission error.
     */
    suspend fun fetchMembership(email: String, timeoutMs: Long = 8000L): Result<Membership?> {
        val key = sanitizeEmail(email)
        if (key.isBlank()) return Result.success(null)
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Result<Membership?>> { cont ->
                rootRef.child("members").child(key)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!cont.isActive) return
                            if (!snapshot.exists()) {
                                cont.resumeWith(Result.success(Result.success(null)))
                                return
                            }
                            val membership = Membership(
                                employeeId = snapshot.child("employeeId").getValue(Long::class.java) ?: 0L,
                                name = snapshot.child("name").getValue(String::class.java).orEmpty(),
                                role = snapshot.child("role").getValue(String::class.java).orEmpty(),
                                active = snapshot.child("active").getValue(Boolean::class.java) ?: false
                            )
                            cont.resumeWith(Result.success(Result.success(membership)))
                        }

                        override fun onCancelled(error: DatabaseError) {
                            android.util.Log.w("FirebaseRtdbService", "members lookup refused: ${error.message}")
                            if (cont.isActive) {
                                cont.resumeWith(Result.success(Result.failure(error.toException())))
                            }
                        }
                    })
            }
        } ?: Result.failure(IllegalStateException("membership lookup timed out"))
    }

    /** Owner check without reading the whole node: just this account's two possible keys. */
    suspend fun isOwner(email: String, uid: String, timeoutMs: Long = 8000L): Result<Boolean> {
        val key = sanitizeEmail(email)
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Result<Boolean>> { cont ->
                rootRef.child("super_admins").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!cont.isActive) return
                        val byKey = key.isNotBlank() && snapshot.child(key).exists()
                        val byUid = uid.isNotBlank() && snapshot.child(uid).exists()
                        cont.resumeWith(Result.success(Result.success(byKey || byUid)))
                    }

                    override fun onCancelled(error: DatabaseError) {
                        if (cont.isActive) cont.resumeWith(Result.success(Result.failure(error.toException())))
                    }
                })
            }
        } ?: Result.failure(IllegalStateException("owner lookup timed out"))
    }

    suspend fun getSuperAdminEmails(): Set<String> = suspendCancellableCoroutine { cont ->
        rootRef.child("super_admins").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val emails = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val email = child.child("email").getValue(String::class.java)
                        ?: child.getValue(String::class.java)
                    if (!email.isNullOrBlank()) {
                        emails.add(email.trim().lowercase())
                    }
                }
                if (cont.isActive) cont.resumeWith(Result.success(emails))
            }

            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptySet()))
            }
        })
    }

    suspend fun registerSuperAdmin(email: String, name: String = "Agency Owner") = withContext(Dispatchers.IO) {
        try {
            val key = sanitizeEmail(email)
            val data = mapOf(
                "email" to email.trim().lowercase(),
                "name" to name.trim(),
                "role" to "SUPER_ADMIN",
                "createdAt" to System.currentTimeMillis()
            )
            rootRef.child("super_admins").child(key).setValue(data)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun listenToSuperAdmins(onUpdate: (Set<String>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val emails = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val email = child.child("email").getValue(String::class.java)
                        ?: child.getValue(String::class.java)
                    if (!email.isNullOrBlank()) {
                        emails.add(email.trim().lowercase())
                    }
                }
                onUpdate(emails)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "super_admins onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("super_admins"), listener)
    }

    /** Returns true only when the office copy of this trip is confirmed saved. */
    suspend fun syncVisit(visit: VisitEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            rootRef.child("visits").child(visit.id.toString()).mergeRecordConfirmed(visit)
        } catch (e: Exception) {
            android.util.Log.w("FirebaseRtdbService", "syncVisit ${visit.id} failed: ${e.message}")
            false
        }
    }

    /**
     * Update only the given fields of a trip. Used for status / close so that a stale local
     * copy never overwrites salesmen who joined from another phone.
     */
    suspend fun updateVisitFields(visitId: Long, fields: Map<String, Any?>) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("visits").child(visitId.toString()).updateChildren(fields)
        } catch (_: Exception) {
        }
    }

    /**
     * Atomically add a salesman to a trip (memberIds / memberNames). Safe when several
     * salesmen join the same trip at the same moment. Returns false when offline / denied.
     */
    suspend fun joinVisit(visitId: Long, employeeId: Long, employeeName: String): Boolean = withContext(Dispatchers.IO) {
        val ref = rootRef.child("visits").child(visitId.toString())
        withTimeoutOrNull(6000L) {
            suspendCancellableCoroutine<Boolean> { cont ->
                ref.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        if (currentData.value == null) {
                            // Local cache miss: the server re-runs this handler with the real value
                            return Transaction.success(currentData)
                        }
                        val starterId = currentData.child("employeeId").getValue(Long::class.java) ?: 0L
                        val starterName = currentData.child("employeeName").getValue(String::class.java).orEmpty()
                        val coId = currentData.child("secondaryEmployeeId").getValue(Long::class.java) ?: 0L
                        val coName = currentData.child("secondaryEmployeeName").getValue(String::class.java).orEmpty()
                        val rawIds = currentData.child("memberIds").getValue(String::class.java).orEmpty()
                        val rawNames = currentData.child("memberNames").getValue(String::class.java).orEmpty()

                        val members = LinkedHashMap<Long, String>()
                        if (starterId > 0L) members[starterId] = starterName
                        if (coId > 0L && !members.containsKey(coId)) members[coId] = coName
                        val ids = com.example.util.TripMembers.parseIds(rawIds)
                        val names = com.example.util.TripMembers.parseNames(rawNames)
                        ids.forEachIndexed { i, id -> if (!members.containsKey(id)) members[id] = names.getOrNull(i).orEmpty() }
                        if (!members.containsKey(employeeId)) members[employeeId] = employeeName.trim()

                        currentData.child("memberIds").value = members.keys.joinToString(",")
                        currentData.child("memberNames").value = members.values.joinToString(", ") { it.replace(",", " ") }
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (error != null) {
                            android.util.Log.w("FirebaseRtdbService", "joinVisit failed: ${error.message}")
                        }
                        if (cont.isActive) cont.resume(error == null && committed)
                    }
                })
            }
        } ?: false
    }

    /**
     * Allocate the next purchase order sequence (HT-<n>) from a shared counter so two phones
     * adding orders at the same moment never get the same number. Returns null when the
     * counter is unreachable (offline or rules not deployed) - caller falls back to local max + 1.
     */
    suspend fun allocateOrderSequence(localMax: Int): Int? = withContext(Dispatchers.IO) {
        val ref = rootRef.child("counters").child("orderNo")
        withTimeoutOrNull(3500L) {
            suspendCancellableCoroutine<Int?> { cont ->
                ref.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val current = (currentData.value as? Number)?.toLong() ?: 0L
                        currentData.value = maxOf(current, localMax.toLong()) + 1L
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        val value = (currentData?.value as? Number)?.toInt()
                        if (cont.isActive) cont.resume(if (error == null && committed) value else null)
                    }
                })
            }
        }
    }

    suspend fun deleteVisit(visitId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("visits").child(visitId.toString()).removeValue()
            rootRef.child("deletion_requests").child("visits_$visitId").removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    /** Returns true only when the office copy of this order is confirmed saved. */
    suspend fun syncPurchaseEntry(entry: PurchaseEntryEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            rootRef.child("purchase_entries").child(entry.id.toString())
                .mergeRecordConfirmed(entry, WebFieldBridge.orderMirrors(entry))
        } catch (e: Exception) {
            android.util.Log.w("FirebaseRtdbService", "syncPurchaseEntry ${entry.id} failed: ${e.message}")
            false
        }
    }

    suspend fun deletePurchaseEntry(entryId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("purchase_entries").child(entryId.toString()).removeValue()
            rootRef.child("deletion_requests").child("purchase_entries_$entryId").removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("customers").child(customer.id.toString()).mergeRecord(customer, WebFieldBridge.customerMirrors(customer))
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun deleteCustomer(customerId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("customers").child(customerId.toString()).removeValue()
            rootRef.child("deletion_requests").child("customers_$customerId").removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) {
        try {
            // Sync to general suppliers master node
            rootRef.child("suppliers").child(supplier.id.toString()).mergeRecord(supplier, WebFieldBridge.supplierMirrors(supplier))

            // If manufacturer, also sync directly to dedicated manufacturers node for instant visibility
            if (supplier.type.equals("Manufacturer", ignoreCase = true)) {
                rootRef.child("manufacturers").child(supplier.id.toString()).mergeRecord(supplier, WebFieldBridge.supplierMirrors(supplier))
            } else {
                rootRef.child("manufacturers").child(supplier.id.toString()).removeValue()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun deleteSupplier(supplierId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("suppliers").child(supplierId.toString()).removeValue()
            rootRef.child("manufacturers").child(supplierId.toString()).removeValue()
            rootRef.child("deletion_requests").child("suppliers_$supplierId").removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("products").child(product.id.toString()).mergeRecord(product)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun deleteProduct(productId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("products").child(productId.toString()).removeValue()
            rootRef.child("deletion_requests").child("products_$productId").removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncEmployee(employee: EmployeeEntity) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("employees").child(employee.id.toString()).mergeRecord(employee, WebFieldBridge.employeeMirrors(employee))
        } catch (e: Exception) {
            // Ignore
        }
    }

    // Note: there is deliberately no "erase this employee" call. Removing the node is what let a
    // removed staff member keep signing in (see deactivateEmployee below).

    /**
     * Takes a staff member off the team without erasing the record — this is how removal works now.
     *
     * Wiping the node was the reason a removed person could still sign in: their own phone kept a
     * local copy of the record, and with nothing left in the cloud to contradict it, the app matched
     * that stale copy and let them in. A record that stays and says "deactivated" is the only thing
     * every phone can agree on. It also keeps their name on past trips and orders.
     *
     * Field names match what the web admin writes, so either side can deactivate or restore.
     */
    suspend fun deactivateEmployee(
        employeeId: Long,
        by: String,
        email: String,
        reason: String = "Staff account deactivated by Administrator"
    ) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("employees").child(employeeId.toString()).updateChildren(
                mapOf(
                    "isBlocked" to true,
                    "isDeleted" to true,
                    // The web reads these shorter names
                    "blocked" to true,
                    "deleted" to true,
                    "status" to "Deactivated",
                    "deletedAt" to System.currentTimeMillis(),
                    "deletedBy" to by,
                    "deletedByEmail" to email,
                    "deletedByRole" to "Admin",
                    "deletionStatus" to "CONFIRMED",
                    "deletionReason" to reason
                )
            )
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Writes who is about to remove a record, immediately before it is removed.
     *
     * Nothing in the app reads these fields on a record that is on its way out. They exist for the
     * copy the office keeps: the bin is filled by a server-side trigger that sees the record as it
     * was at the moment of deletion, so whatever is stamped here is what the bin can say about who
     * threw it away. Without it, an admin deleting something directly leaves an anonymous entry.
     *
     * Awaited so the stamp lands first, and silent on failure: an anonymous bin entry is a small loss
     * next to a delete that refuses to happen.
     */
    suspend fun stampDeleter(
        node: String,
        id: Long,
        deletedBy: String,
        email: String,
        role: String
    ) = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(6000L) {
                suspendCancellableCoroutine<Unit> { cont ->
                    rootRef.child(node).child(id.toString()).updateChildren(
                        mapOf(
                            "deletedBy" to deletedBy,
                            "deletedByEmail" to email,
                            "deletedByRole" to role,
                            "deletedAt" to System.currentTimeMillis()
                        )
                    ).addOnCompleteListener { if (cont.isActive) cont.resumeWith(Result.success(Unit)) }
                }
            }
            Unit
        } catch (_: Exception) {
            Unit
        }
    }

    suspend fun recordDeletionRequest(
        collection: String,
        itemId: Long,
        itemSummary: String,
        deletedBy: String,
        email: String,
        role: String,
        reason: String = ""
    ) = withContext(Dispatchers.IO) {
        try {
            val key = "${collection}_$itemId"
            val data = mapOf(
                "key" to key,
                "collection" to collection,
                "itemId" to itemId,
                "itemSummary" to itemSummary,
                "deletedBy" to deletedBy,
                "deletedByEmail" to email,
                "deletedByRole" to role,
                "deletedAt" to System.currentTimeMillis(),
                "deletionReason" to reason,
                "status" to "PENDING_CONFIRMATION"
            )
            rootRef.child("deletion_requests").child(key).setValue(data)
        } catch (_: Exception) {}
    }

    suspend fun softDeleteVisit(visit: VisitEntity, deletedBy: String, email: String, role: String) = withContext(Dispatchers.IO) {
        try {
            val updated = visit.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis(),
                deletedBy = deletedBy,
                deletedByEmail = email,
                deletedByRole = role,
                deletionStatus = "PENDING_CONFIRMATION"
            )
            rootRef.child("visits").child(visit.id.toString()).mergeRecord(updated)
            recordDeletionRequest(
                collection = "visits",
                itemId = visit.id,
                itemSummary = "Visit #${visit.visitCode} - ${visit.customerName} (${visit.date})",
                deletedBy = deletedBy,
                email = email,
                role = role
            )
        } catch (_: Exception) {}
    }

    suspend fun softDeletePurchaseEntry(entry: PurchaseEntryEntity, deletedBy: String, email: String, role: String) = withContext(Dispatchers.IO) {
        try {
            val updated = entry.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis(),
                deletedBy = deletedBy,
                deletedByEmail = email,
                deletedByRole = role,
                deletionStatus = "PENDING_CONFIRMATION"
            )
            rootRef.child("purchase_entries").child(entry.id.toString()).mergeRecord(updated, WebFieldBridge.orderMirrors(updated))
            recordDeletionRequest(
                collection = "purchase_entries",
                itemId = entry.id,
                itemSummary = "Order #${entry.orderNo} - ${entry.itemCode} (${entry.pieces} pcs from ${entry.supplierName})",
                deletedBy = deletedBy,
                email = email,
                role = role
            )
        } catch (_: Exception) {}
    }

    suspend fun softDeleteCustomer(customer: CustomerEntity, deletedBy: String, email: String, role: String) = withContext(Dispatchers.IO) {
        try {
            val updated = customer.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis(),
                deletedBy = deletedBy,
                deletedByEmail = email,
                deletedByRole = role,
                deletionStatus = "PENDING_CONFIRMATION"
            )
            rootRef.child("customers").child(customer.id.toString()).mergeRecord(updated, WebFieldBridge.customerMirrors(updated))
            recordDeletionRequest(
                collection = "customers",
                itemId = customer.id,
                itemSummary = "Customer: ${customer.name} (${customer.city})",
                deletedBy = deletedBy,
                email = email,
                role = role
            )
        } catch (_: Exception) {}
    }

    suspend fun softDeleteSupplier(supplier: SupplierEntity, deletedBy: String, email: String, role: String) = withContext(Dispatchers.IO) {
        try {
            val updated = supplier.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis(),
                deletedBy = deletedBy,
                deletedByEmail = email,
                deletedByRole = role,
                deletionStatus = "PENDING_CONFIRMATION"
            )
            rootRef.child("suppliers").child(supplier.id.toString()).mergeRecord(updated, WebFieldBridge.supplierMirrors(updated))
            if (supplier.type.equals("Manufacturer", ignoreCase = true)) {
                rootRef.child("manufacturers").child(supplier.id.toString()).mergeRecord(updated, WebFieldBridge.supplierMirrors(updated))
            }
            recordDeletionRequest(
                collection = "suppliers",
                itemId = supplier.id,
                itemSummary = "Supplier: ${supplier.name} (${supplier.brand})",
                deletedBy = deletedBy,
                email = email,
                role = role
            )
        } catch (_: Exception) {}
    }

    suspend fun softDeleteProduct(product: ProductEntity, deletedBy: String, email: String, role: String) = withContext(Dispatchers.IO) {
        try {
            val updated = product.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis(),
                deletedBy = deletedBy,
                deletedByEmail = email,
                deletedByRole = role,
                deletionStatus = "PENDING_CONFIRMATION"
            )
            rootRef.child("products").child(product.id.toString()).mergeRecord(updated)
            recordDeletionRequest(
                collection = "products",
                itemId = product.id,
                itemSummary = "Product: ${product.name} (${product.productCode})",
                deletedBy = deletedBy,
                email = email,
                role = role
            )
        } catch (_: Exception) {}
    }

    suspend fun syncTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("transactions").child(transaction.id.toString()).setValue(transaction)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncPackGroup(group: PackGroupEntity) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("pack_groups").child(group.id.toString()).setValue(group)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun deletePackGroup(groupId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("pack_groups").child(groupId.toString()).removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncBrand(brand: BrandEntity) = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "id" to brand.id,
                "brandName" to brand.brandName,
                "manufacturerId" to brand.manufacturerId,
                "manufacturerName" to brand.manufacturerName,
                "category" to brand.category,
                "logoPhotoUri" to brand.logoPhotoUri,
                "description" to brand.description,
                "isActive" to brand.isActive,
                "active" to brand.isActive,
                "isDeleted" to brand.isDeleted,
                "deleted" to brand.isDeleted,
                "deletedAt" to brand.deletedAt,
                "createdAt" to brand.createdAt
            )
            rootRef.child("brands").child(brand.id.toString()).updateChildren(data)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun removeDeletionRequest(collection: String, itemId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("deletion_requests").child("${collection}_$itemId").removeValue()
        } catch (_: Exception) {}
    }

    suspend fun deleteBrand(brandId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("brands").child(brandId.toString()).removeValue()
            rootRef.child("deletion_requests").child("brands_$brandId").removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun softDeleteBrand(brand: BrandEntity, deletedBy: String, email: String, role: String) = withContext(Dispatchers.IO) {
        try {
            val updated = brand.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis()
            )
            syncBrand(updated)
            recordDeletionRequest(
                collection = "brands",
                itemId = brand.id,
                itemSummary = "Brand: ${brand.brandName}",
                deletedBy = deletedBy,
                email = email,
                role = role
            )
        } catch (_: Exception) {}
    }

    suspend fun syncTransporter(transporter: TransporterEntity) = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "id" to transporter.id,
                "transporterName" to transporter.transporterName,
                "contactPerson" to transporter.contactPerson,
                "phone" to transporter.phone1,
                "phone1" to transporter.phone1,
                "phone2" to transporter.phone2,
                "phone3" to transporter.phone3,
                "officeAddress" to transporter.officeAddress,
                "godownAddress" to transporter.godownAddress,
                "city" to transporter.city,
                "destinationsCovered" to transporter.destinationsCovered,
                "gstin" to transporter.gstin,
                "trackingUrl" to transporter.trackingUrl,
                "notes" to transporter.notes,
                "isActive" to !transporter.isDeleted,
                "active" to !transporter.isDeleted,
                "isDeleted" to transporter.isDeleted,
                "deleted" to transporter.isDeleted,
                "createdAt" to transporter.createdAt
            )
            rootRef.child("transporters").child(transporter.id.toString()).updateChildren(data)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun deleteTransporter(transporterId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("transporters").child(transporterId.toString()).removeValue()
            rootRef.child("deletion_requests").child("transporters_$transporterId").removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun softDeleteTransporter(transporter: TransporterEntity, deletedBy: String, email: String, role: String) = withContext(Dispatchers.IO) {
        try {
            val updated = transporter.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis()
            )
            syncTransporter(updated)
            recordDeletionRequest(
                collection = "transporters",
                itemId = transporter.id,
                itemSummary = "Transporter: ${transporter.transporterName}",
                deletedBy = deletedBy,
                email = email,
                role = role
            )
        } catch (_: Exception) {}
    }

    suspend fun syncMarket(market: MarketEntity) = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "id" to market.id,
                "marketName" to market.marketName,
                "city" to market.city,
                "area" to market.area,
                "landmark" to market.landmark,
                "pincode" to market.pincode,
                "marketType" to market.marketType,
                "description" to market.description,
                "isActive" to !market.isDeleted,
                "active" to !market.isDeleted,
                "isDeleted" to market.isDeleted,
                "deleted" to market.isDeleted,
                "createdAt" to market.createdAt
            )
            rootRef.child("markets").child(market.id.toString()).updateChildren(data)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun deleteMarket(marketId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("markets").child(marketId.toString()).removeValue()
            rootRef.child("deletion_requests").child("markets_$marketId").removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun softDeleteMarket(market: MarketEntity, deletedBy: String, email: String, role: String) = withContext(Dispatchers.IO) {
        try {
            val updated = market.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis()
            )
            syncMarket(updated)
            recordDeletionRequest(
                collection = "markets",
                itemId = market.id,
                itemSummary = "Market: ${market.marketName}",
                deletedBy = deletedBy,
                email = email,
                role = role
            )
        } catch (_: Exception) {}
    }

    suspend fun syncLead(lead: LeadEntity) = withContext(Dispatchers.IO) {
        try {
            val key = lead.leadId.ifBlank { "lead_${System.currentTimeMillis()}" }
            val data = mapOf(
                "id" to key,
                "leadId" to key,
                "type" to lead.type,
                "name" to lead.name,
                "firmName" to lead.firmName,
                "supplierType" to lead.supplierType,
                "phone" to lead.phone,
                "phone2" to lead.phone2,
                "meetingPlace" to lead.meetingPlace,
                "city" to lead.city,
                "state" to lead.state,
                "notes" to lead.notes,
                "photos" to lead.photoList,
                "status" to lead.status,
                "nextFollowUpDate" to lead.nextFollowUpDate,
                "createdByUid" to lead.createdByUid,
                "createdByName" to lead.createdByName,
                "createdAt" to lead.createdAt,
                "convertedAt" to lead.convertedAt,
                "convertedTargetId" to lead.convertedTargetId,
                "isDeleted" to lead.isDeleted
            )
            rootRef.child("leads").child(key).setValue(data)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun deleteLead(leadId: String) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("leads").child(leadId).removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncChequePdc(cheque: ChequePdcEntity) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("cheques_pdc").child(cheque.id.toString()).mergeRecord(cheque)
        } catch (_: Exception) {}
    }

    suspend fun deleteChequePdc(chequeId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("cheques_pdc").child(chequeId.toString()).removeValue()
            rootRef.child("deletion_requests").child("cheques_pdc_$chequeId").removeValue()
        } catch (_: Exception) {}
    }

    suspend fun softDeleteChequePdc(cheque: ChequePdcEntity, deletedBy: String, email: String, role: String) = withContext(Dispatchers.IO) {
        try {
            val updated = cheque.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis(),
                deletedBy = deletedBy
            )
            rootRef.child("cheques_pdc").child(cheque.id.toString()).mergeRecord(updated)
            recordDeletionRequest(
                collection = "cheques_pdc",
                itemId = cheque.id,
                itemSummary = "Cheque #${cheque.chequeNo}: ${cheque.bankName} (₹${cheque.amount})",
                deletedBy = deletedBy,
                email = email,
                role = role
            )
        } catch (_: Exception) {}
    }

    suspend fun approveRegistrationRequest(
        requestId: String,
        newCustomerId: Long,
        religion: String,
        creditType: String = "Cash",
        creditDays: Int = 30,
        creditLimit: Double = 0.0,
        assignedAgentId: Long? = null,
        assignedAgentName: String = "",
        approvedBy: String = "Admin"
    ) = withContext(Dispatchers.IO) {
        try {
            val updates = mutableMapOf<String, Any?>(
                "status" to "APPROVED",
                "approvedAt" to System.currentTimeMillis(),
                "approvedBy" to approvedBy,
                "createdCustomerId" to newCustomerId,
                "religion" to religion,
                "creditType" to creditType,
                "creditDays" to creditDays,
                "creditLimit" to creditLimit
            )
            if (assignedAgentId != null) {
                updates["assignedAgentId"] = assignedAgentId
                updates["assignedAgentName"] = assignedAgentName
            }
            rootRef.child("customer_registration_requests").child(requestId).updateChildren(updates)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun rejectRegistrationRequest(
        requestId: String,
        reason: String = ""
    ) = withContext(Dispatchers.IO) {
        try {
            val updates = mapOf<String, Any?>(
                "status" to "REJECTED",
                "rejectedAt" to System.currentTimeMillis(),
                "rejectionReason" to reason
            )
            rootRef.child("customer_registration_requests").child(requestId).updateChildren(updates)
        } catch (e: Exception) {
            // Ignore
        }
    }


    // Downstream Deserialization Helper
    private inline fun <reified T> DataSnapshot.extractList(): List<T> {
        val list = mutableListOf<T>()
        val collectionName = when (T::class) {
            CustomerEntity::class -> "customers"
            SupplierEntity::class -> "suppliers"
            ProductEntity::class -> "products"
            EmployeeEntity::class -> "employees"
            VisitEntity::class -> "visits"
            PurchaseEntryEntity::class -> "purchase_entries"
            TransactionEntity::class -> "transactions"
            PackGroupEntity::class -> "pack_groups"
            BrandEntity::class -> "brands"
            TransporterEntity::class -> "transporters"
            MarketEntity::class -> "markets"
            ChequePdcEntity::class -> "cheques_pdc"
            else -> ""
        }
        for (child in children) {
            try {
                if (child.key == "0" || child.key == "null") {
                    child.ref.removeValue()
                    continue
                }
                val item = child.getValue(T::class.java)
                if (item != null) {
                    val keyLong = child.key?.toLongOrNull() ?: 0L
                    val rawIsDeleted = child.child("isDeleted").getValue(Boolean::class.java)
                        ?: child.child("deleted").getValue(Boolean::class.java)
                        ?: (child.child("deletionStatus").getValue(String::class.java)?.let { it == "PENDING_CONFIRMATION" || it == "CONFIRMED" } ?: false)
                    val isPendingInQueue = if (collectionName.isNotBlank() && keyLong > 0L) {
                        pendingDeletionKeys.contains("${collectionName}_$keyLong")
                    } else false
                    val effectivelyDeleted = rawIsDeleted || isPendingInQueue

                    val fixedItem = when (item) {
                        // Records created / edited on the web: fill Android fields from the web copies
                        is CustomerEntity -> WebFieldBridge.readCustomer(item, child).copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is SupplierEntity -> WebFieldBridge.readSupplier(item, child).copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is ProductEntity -> item.copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is EmployeeEntity -> WebFieldBridge.readEmployee(item, child).copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is VisitEntity -> item.copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is PurchaseEntryEntity -> {
                            val packGroupId = child.child("packGroupId").getValue(Long::class.java)
                                ?: item.packGroupId
                            val mixedPackNote = child.child("mixedPackNote").getValue(String::class.java)
                                ?: item.mixedPackNote
                            WebFieldBridge.readOrder(item, child).copy(
                                id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                                packGroupId = packGroupId,
                                mixedPackNote = mixedPackNote,
                                isDeleted = effectivelyDeleted || item.isDeleted
                            )
                        }
                        is TransactionEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
                        is PackGroupEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
                        is BrandEntity -> item.copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is TransporterEntity -> WebFieldBridge.readTransporter(item, child).copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is MarketEntity -> item.copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is ChequePdcEntity -> item.copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        else -> item
                    }
                    val isValidId = when (fixedItem) {
                        is CustomerEntity -> fixedItem.id > 0L
                        is SupplierEntity -> fixedItem.id > 0L
                        is ProductEntity -> fixedItem.id > 0L
                        is EmployeeEntity -> fixedItem.id > 0L
                        is VisitEntity -> fixedItem.id > 0L
                        is PurchaseEntryEntity -> fixedItem.id > 0L
                        is TransactionEntity -> fixedItem.id > 0L
                        is PackGroupEntity -> fixedItem.id > 0L
                        is BrandEntity -> fixedItem.id > 0L
                        is TransporterEntity -> fixedItem.id > 0L
                        is MarketEntity -> fixedItem.id > 0L
                        is ChequePdcEntity -> fixedItem.id > 0L
                        else -> true
                    }
                    if (isValidId) {
                        @Suppress("UNCHECKED_CAST")
                        list.add(fixedItem as T)
                    }
                }
            } catch (e: Exception) {
                // A record the mapper cannot read (e.g. an id saved as text) would silently vanish from the
                // app otherwise - log it so it can be fixed at the source.
                android.util.Log.w("FirebaseRtdbService", "Skipped $collectionName/${child.key}: ${e.message}")
            }
        }
        return list
    }

    suspend fun purgeLegacyZeroKeys() = withContext(Dispatchers.IO) {
        // Safe no-op to prevent deleting actual entities
    }

    // Check if cloud database is completely uninitialized
    suspend fun isCloudEmpty(): Boolean = suspendCancellableCoroutine { cont ->
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hasAdmins = snapshot.hasChild("super_admins") && snapshot.child("super_admins").childrenCount > 0
                val hasEmployees = snapshot.hasChild("employees") && snapshot.child("employees").childrenCount > 0
                val hasCustomers = snapshot.hasChild("customers") && snapshot.child("customers").childrenCount > 0
                val hasSuppliers = snapshot.hasChild("suppliers") && snapshot.child("suppliers").childrenCount > 0
                val hasProducts = snapshot.hasChild("products") && snapshot.child("products").childrenCount > 0
                val empty = !(hasAdmins || hasEmployees || hasCustomers || hasSuppliers || hasProducts)
                if (cont.isActive) cont.resumeWith(Result.success(empty))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(false))
            }
        })
    }

    // Downstream Fetch Operations
    suspend fun fetchEmployees(): List<EmployeeEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("employees").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resumeWith(Result.success(snapshot.extractList<EmployeeEntity>()))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun fetchCustomers(): List<CustomerEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("customers").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resumeWith(Result.success(snapshot.extractList<CustomerEntity>()))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun fetchSuppliers(): List<SupplierEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("suppliers").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<Long, SupplierEntity>()
                for (item in snapshot.extractList<SupplierEntity>()) {
                    if (item.id > 0L) {
                        map[item.id] = item
                    }
                }
                // Also merge /manufacturers to ensure manufacturers entered under dedicated node are included.
                // suppliers/ is the master copy: a (possibly stale) manufacturers/ copy never replaces it.
                rootRef.child("manufacturers").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(manSnapshot: DataSnapshot) {
                        for (item in manSnapshot.extractList<SupplierEntity>()) {
                            if (item.id > 0L && !map.containsKey(item.id)) {
                                map[item.id] = item
                            }
                        }
                        if (cont.isActive) cont.resumeWith(Result.success(map.values.toList()))
                    }
                    override fun onCancelled(error: DatabaseError) {
                        if (cont.isActive) cont.resumeWith(Result.success(map.values.toList()))
                    }
                })
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun fetchProducts(): List<ProductEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("products").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resumeWith(Result.success(snapshot.extractList<ProductEntity>()))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun fetchVisits(): List<VisitEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("visits").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resumeWith(Result.success(snapshot.extractList<VisitEntity>()))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun fetchPurchaseEntries(): List<PurchaseEntryEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("purchase_entries").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resumeWith(Result.success(snapshot.extractList<PurchaseEntryEntity>()))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun fetchTransactions(): List<TransactionEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("transactions").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resumeWith(Result.success(snapshot.extractList<TransactionEntity>()))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun fetchPackGroups(): List<PackGroupEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("pack_groups").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resumeWith(Result.success(snapshot.extractList<PackGroupEntity>()))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun fetchBrands(): List<BrandEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("brands").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resumeWith(Result.success(snapshot.extractList<BrandEntity>()))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun fetchTransporters(): List<TransporterEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("transporters").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resumeWith(Result.success(snapshot.extractList<TransporterEntity>()))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun fetchMarkets(): List<MarketEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("markets").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resumeWith(Result.success(snapshot.extractList<MarketEntity>()))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun fetchChequesPdc(): List<ChequePdcEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("cheques_pdc").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resumeWith(Result.success(snapshot.extractList<ChequePdcEntity>()))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }


    // Realtime Downstream Listeners
    fun listenToEmployees(onUpdate: (List<EmployeeEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<EmployeeEntity>())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "employees onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("employees"), listener)
    }

    fun listenToCustomers(onUpdate: (List<CustomerEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<CustomerEntity>())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "customers onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("customers"), listener)
    }

    fun listenToSuppliers(onUpdate: (List<SupplierEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<Long, SupplierEntity>()
                for (item in snapshot.extractList<SupplierEntity>()) {
                    if (item.id > 0L) {
                        map[item.id] = item
                    }
                }
                onUpdate(map.values.toList())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "suppliers onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("suppliers"), listener)
    }

    fun listenToProducts(onUpdate: (List<ProductEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<ProductEntity>())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "products onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("products"), listener)
    }

    fun listenToVisits(onUpdate: (List<VisitEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<VisitEntity>())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "visits onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("visits"), listener)
    }

    fun listenToPurchaseEntries(onUpdate: (List<PurchaseEntryEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<PurchaseEntryEntity>())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "purchase_entries onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("purchase_entries"), listener)
    }

    fun listenToTransactions(onUpdate: (List<TransactionEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<TransactionEntity>())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "transactions onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("transactions"), listener)
    }

    fun listenToPackGroups(onUpdate: (List<PackGroupEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<PackGroupEntity>())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "pack_groups onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("pack_groups"), listener)
    }

    fun listenToBrands(onUpdate: (List<BrandEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<BrandEntity>())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "brands onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("brands"), listener)
    }

    fun listenToTransporters(onUpdate: (List<TransporterEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<TransporterEntity>())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "transporters onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("transporters"), listener)
    }

    fun listenToMarkets(onUpdate: (List<MarketEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<MarketEntity>())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "markets onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("markets"), listener)
    }

    fun listenToChequesPdc(onUpdate: (List<ChequePdcEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<ChequePdcEntity>())
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "cheques_pdc onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("cheques_pdc"), listener)
    }

    private fun DataSnapshot.toLead(): LeadEntity? {
        val key = key ?: return null
        val leadId = child("id").getValue(String::class.java)?.takeIf { it.isNotBlank() }
            ?: child("leadId").getValue(String::class.java)?.takeIf { it.isNotBlank() }
            ?: key

        val photosList = mutableListOf<String>()
        val photosChild = child("photos")
        if (photosChild.exists()) {
            for (p in photosChild.children) {
                val url = p.getValue(String::class.java) ?: p.value?.toString()
                if (!url.isNullOrBlank()) photosList.add(url)
            }
        }
        val photosJsonStr = if (photosList.isEmpty()) {
            child("photosJson").getValue(String::class.java) ?: "[]"
        } else {
            "[" + photosList.joinToString(",") { "\"$it\"" } + "]"
        }

        return LeadEntity(
            leadId = leadId,
            type = child("type").getValue(String::class.java) ?: "customer",
            name = child("name").getValue(String::class.java) ?: "",
            firmName = child("firmName").getValue(String::class.java) ?: "",
            supplierType = child("supplierType").getValue(String::class.java) ?: "",
            phone = child("phone").getValue(String::class.java) ?: "",
            phone2 = child("phone2").getValue(String::class.java) ?: "",
            meetingPlace = child("meetingPlace").getValue(String::class.java) ?: "",
            city = child("city").getValue(String::class.java) ?: "Ahmedabad",
            state = child("state").getValue(String::class.java) ?: "Gujarat",
            notes = child("notes").getValue(String::class.java) ?: "",
            photosJson = photosJsonStr,
            status = child("status").getValue(String::class.java) ?: "Thinking",
            nextFollowUpDate = child("nextFollowUpDate").getValue(String::class.java) ?: "",
            createdByUid = child("createdByUid").getValue(String::class.java) ?: "",
            createdByName = child("createdByName").getValue(String::class.java) ?: "",
            createdAt = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
            convertedAt = child("convertedAt").getValue(Long::class.java),
            convertedTargetId = child("convertedTargetId").getValue(Long::class.java),
            isDeleted = child("isDeleted").getValue(Boolean::class.java) ?: false
        )
    }

    private fun DataSnapshot.toRegistrationRequest(): CustomerRegistrationRequestEntity? {
        val key = key ?: return null
        val reqId = child("id").getValue(String::class.java)?.takeIf { it.isNotBlank() } ?: key
        return CustomerRegistrationRequestEntity(
            id = reqId,
            firmName = child("firmName").getValue(String::class.java) ?: "",
            name = child("name").getValue(String::class.java) ?: "",
            phone = child("phone").getValue(String::class.java) ?: "",
            phone2 = child("phone2").getValue(String::class.java) ?: "",
            email = child("email").getValue(String::class.java) ?: "",
            address = child("address").getValue(String::class.java) ?: "",
            shopAddress = child("shopAddress").getValue(String::class.java) ?: "",
            marketArea = child("marketArea").getValue(String::class.java) ?: "",
            city = child("city").getValue(String::class.java) ?: "Ahmedabad",
            district = child("district").getValue(String::class.java) ?: "",
            state = child("state").getValue(String::class.java) ?: "Gujarat",
            pincode = child("pincode").getValue(String::class.java) ?: "",
            shopMapLink = child("shopMapLink").getValue(String::class.java) ?: "",
            garmentTypes = lenientText("garmentTypes"),
            // Filled on the public form; approval copies them onto the customer
            workingMarkets = lenientText("workingMarkets"),
            dob = lenientText("dob"),
            cancelChequePhotoUri = lenientText("cancelChequePhotoUri"),
            purchaserPhotoUri = lenientText("purchaserPhotoUri"),
            gstin = child("gstin").getValue(String::class.java) ?: "",
            panNumber = child("panNumber").getValue(String::class.java) ?: "",
            preferredTransporterName = child("preferredTransporterName").getValue(String::class.java) ?: "",
            transportPreference = child("transportPreference").getValue(String::class.java) ?: "",
            bankName = child("bankName").getValue(String::class.java) ?: "",
            accountNumber = child("accountNumber").getValue(String::class.java) ?: "",
            ifscCode = child("ifscCode").getValue(String::class.java) ?: "",
            shopPhotoUri = child("shopPhotoUri").getValue(String::class.java) ?: "",
            gstCertPhotoUri = child("gstCertPhotoUri").getValue(String::class.java) ?: "",
            panPhotoUri = child("panPhotoUri").getValue(String::class.java) ?: "",
            aadharPhotoUri = child("aadharPhotoUri").getValue(String::class.java) ?: "",
            aadharBackPhotoUri = child("aadharBackPhotoUri").getValue(String::class.java) ?: "",
            notes = child("notes").getValue(String::class.java) ?: "",
            status = child("status").getValue(String::class.java) ?: "PENDING",
            phoneVerified = child("phoneVerified").getValue(Boolean::class.java) ?: true,
            createdAt = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
            approvedAt = child("approvedAt").getValue(Long::class.java),
            approvedBy = child("approvedBy").getValue(String::class.java) ?: "",
            // Lenient: the web admin used to save these ids as text
            assignedAgentId = lenientLong("assignedAgentId"),
            assignedAgentName = child("assignedAgentName").getValue(String::class.java) ?: "",
            creditType = child("creditType").getValue(String::class.java) ?: "Cash",
            creditDays = lenientLong("creditDays")?.toInt() ?: 30,
            creditLimit = (child("creditLimit").value as? Number)?.toDouble()
                ?: child("creditLimit").value?.toString()?.toDoubleOrNull() ?: 0.0,
            religion = child("religion").getValue(String::class.java) ?: "",
            createdCustomerId = lenientLong("createdCustomerId"),
            rejectionReason = child("rejectionReason").getValue(String::class.java) ?: "",
            subAgentId = lenientLong("subAgentId"),
            subAgentName = child("subAgentName").getValue(String::class.java) ?: ""
        )
    }

    /** Reads a numeric child that may have been stored as a number or as numeric text. */
    private fun DataSnapshot.lenientLong(key: String): Long? = when (val v = child(key).value) {
        is Number -> v.toLong()
        is String -> v.trim().toLongOrNull() ?: v.trim().toDoubleOrNull()?.toLong()
        else -> null
    }

    /** Reads a text child that may also have been stored as a list (joined with ", ") or a number. */
    private fun DataSnapshot.lenientText(key: String): String = when (val v = child(key).value) {
        null -> ""
        is List<*> -> v.filterNotNull().joinToString(", ") { it.toString().trim() }
        is Map<*, *> -> v.values.filterNotNull().joinToString(", ") { it.toString().trim() }
        else -> v.toString().trim()
    }

    fun listenToLeads(onUpdate: (List<LeadEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<LeadEntity>()
                for (child in snapshot.children) {
                    val lead = child.toLead()
                    if (lead != null && !lead.isDeleted) {
                        list.add(lead)
                    }
                }
                onUpdate(list)
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "leads onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("leads"), listener)
    }

    suspend fun fetchLeads(): List<LeadEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("leads").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<LeadEntity>()
                for (child in snapshot.children) {
                    val lead = child.toLead()
                    if (lead != null && !lead.isDeleted) {
                        list.add(lead)
                    }
                }
                if (cont.isActive) cont.resumeWith(Result.success(list))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    fun listenToRegistrationRequests(onUpdate: (List<CustomerRegistrationRequestEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CustomerRegistrationRequestEntity>()
                for (child in snapshot.children) {
                    val req = child.toRegistrationRequest()
                    if (req != null) {
                        list.add(req)
                    }
                }
                onUpdate(list)
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "customer_registration_requests onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("customer_registration_requests"), listener)
    }

    suspend fun fetchRegistrationRequests(): List<CustomerRegistrationRequestEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("customer_registration_requests").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CustomerRegistrationRequestEntity>()
                for (child in snapshot.children) {
                    val req = child.toRegistrationRequest()
                    if (req != null) {
                        list.add(req)
                    }
                }
                if (cont.isActive) cont.resumeWith(Result.success(list))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    private fun DataSnapshot.toSupplierRegistrationRequest(): SupplierRegistrationRequestEntity? {
        val key = key ?: return null
        val reqId = child("id").getValue(String::class.java)?.takeIf { it.isNotBlank() } ?: key
        return SupplierRegistrationRequestEntity(
            id = reqId,
            firmName = child("firmName").getValue(String::class.java) ?: "",
            name = child("name").getValue(String::class.java) ?: "",
            contactPerson = child("contactPerson").getValue(String::class.java) ?: "",
            type = child("type").getValue(String::class.java) ?: "Manufacturer",
            brand = child("brand").getValue(String::class.java) ?: "",
            phone = child("phone").getValue(String::class.java) ?: "",
            phone2 = child("phone2").getValue(String::class.java) ?: "",
            email = child("email").getValue(String::class.java) ?: "",
            address = child("address").getValue(String::class.java) ?: "",
            officeAddress = child("officeAddress").getValue(String::class.java) ?: "",
            homeAddress = lenientText("homeAddress"),
            marketArea = child("marketArea").getValue(String::class.java) ?: "",
            marketId = lenientLong("marketId")?.takeIf { it > 0L },
            marketName = lenientText("marketName"),
            city = child("city").getValue(String::class.java) ?: "Ahmedabad",
            district = child("district").getValue(String::class.java) ?: "",
            state = child("state").getValue(String::class.java) ?: "Gujarat",
            pincode = child("pincode").getValue(String::class.java) ?: "",
            mapLink = child("mapLink").getValue(String::class.java) ?: "",
            productsMade = lenientText("productsMade"),
            categories = lenientText("categories"),
            subCategories = lenientText("subCategories"),
            godownPhotoUri = lenientText("godownPhotoUri"),
            systemMrpValue = lenientText("systemMrpValue").ifBlank { child("system").child("mrp").child("value").value?.toString().orEmpty() },
            systemMrpPercent = lenientText("systemMrpPercent").ifBlank { child("system").child("mrp").child("percentage").value?.toString().orEmpty() },
            systemLessValue = lenientText("systemLessValue").ifBlank { child("system").child("less").child("value").value?.toString().orEmpty() },
            systemLessPercent = lenientText("systemLessPercent").ifBlank { child("system").child("less").child("percentage").value?.toString().orEmpty() },
            priceRange = child("priceRange").getValue(String::class.java) ?: "",
            gstin = child("gstin").getValue(String::class.java) ?: "",
            panNumber = child("panNumber").getValue(String::class.java) ?: "",
            bankName = child("bankName").getValue(String::class.java) ?: "",
            accountNumber = child("accountNumber").getValue(String::class.java) ?: "",
            ifscCode = child("ifscCode").getValue(String::class.java) ?: "",
            visitingCardPhotoUri = child("visitingCardPhotoUri").getValue(String::class.java) ?: "",
            shopPhotoUri = child("shopPhotoUri").getValue(String::class.java) ?: "",
            gstCertPhotoUri = child("gstCertPhotoUri").getValue(String::class.java) ?: "",
            panPhotoUri = child("panPhotoUri").getValue(String::class.java) ?: "",
            idProofPhotoUri = child("idProofPhotoUri").getValue(String::class.java)
                ?: child("aadharPhotoUri").getValue(String::class.java) ?: "",
            idProofBackPhotoUri = child("idProofBackPhotoUri").getValue(String::class.java)
                ?: child("aadharBackPhotoUri").getValue(String::class.java) ?: "",
            aadharPhotoUri = child("aadharPhotoUri").getValue(String::class.java)
                ?: child("idProofPhotoUri").getValue(String::class.java) ?: "",
            aadharBackPhotoUri = child("aadharBackPhotoUri").getValue(String::class.java)
                ?: child("idProofBackPhotoUri").getValue(String::class.java) ?: "",
            notes = child("notes").getValue(String::class.java) ?: "",
            status = child("status").getValue(String::class.java) ?: "PENDING",
            phoneVerified = child("phoneVerified").getValue(Boolean::class.java) ?: true,
            createdAt = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
            approvedAt = child("approvedAt").getValue(Long::class.java),
            approvedBy = child("approvedBy").getValue(String::class.java) ?: "",
            createdSupplierId = child("createdSupplierId").getValue(Long::class.java),
            rejectionReason = child("rejectionReason").getValue(String::class.java) ?: ""
        )
    }

    fun listenToSupplierRegistrationRequests(onUpdate: (List<SupplierRegistrationRequestEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<SupplierRegistrationRequestEntity>()
                for (child in snapshot.children) {
                    val req = child.toSupplierRegistrationRequest()
                    if (req != null) {
                        list.add(req)
                    }
                }
                onUpdate(list)
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("FirebaseRtdbService", "supplier_registration_requests onCancelled: ${error.message} (code ${error.code})")
            }
        }
        return registerListener(rootRef.child("supplier_registration_requests"), listener)
    }

    suspend fun fetchSupplierRegistrationRequests(): List<SupplierRegistrationRequestEntity> = suspendCancellableCoroutine { cont ->
        rootRef.child("supplier_registration_requests").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<SupplierRegistrationRequestEntity>()
                for (child in snapshot.children) {
                    val req = child.toSupplierRegistrationRequest()
                    if (req != null) {
                        list.add(req)
                    }
                }
                if (cont.isActive) cont.resumeWith(Result.success(list))
            }
            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWith(Result.success(emptyList()))
            }
        })
    }

    suspend fun approveSupplierRegistrationRequest(
        requestId: String,
        newSupplierId: Long,
        brand: String,
        marketName: String,
        approvedBy: String,
        systemMrpValue: String = "",
        systemMrpPercent: String = "",
        systemLessValue: String = "",
        systemLessPercent: String = ""
    ) = suspendCancellableCoroutine<Unit> { cont ->
        val updates = mutableMapOf<String, Any>(
            "status" to "APPROVED",
            "approvedAt" to System.currentTimeMillis(),
            "approvedBy" to approvedBy,
            "brand" to brand,
            "marketArea" to marketName,
            "createdSupplierId" to newSupplierId
        )
        if (systemMrpValue.isNotBlank()) updates["systemMrpValue"] = systemMrpValue
        if (systemMrpPercent.isNotBlank()) updates["systemMrpPercent"] = systemMrpPercent
        if (systemLessValue.isNotBlank()) updates["systemLessValue"] = systemLessValue
        if (systemLessPercent.isNotBlank()) updates["systemLessPercent"] = systemLessPercent
        rootRef.child("supplier_registration_requests").child(requestId).updateChildren(updates)
            .addOnSuccessListener {
                if (cont.isActive) cont.resumeWith(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                if (cont.isActive) cont.resumeWith(Result.failure(e))
            }
    }

    suspend fun rejectSupplierRegistrationRequest(
        requestId: String,
        reason: String
    ) = suspendCancellableCoroutine<Unit> { cont ->
        val updates = mapOf(
            "status" to "REJECTED",
            "rejectionReason" to reason
        )
        rootRef.child("supplier_registration_requests").child(requestId).updateChildren(updates)
            .addOnSuccessListener {
                if (cont.isActive) cont.resumeWith(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                if (cont.isActive) cont.resumeWith(Result.failure(e))
            }
    }
}

