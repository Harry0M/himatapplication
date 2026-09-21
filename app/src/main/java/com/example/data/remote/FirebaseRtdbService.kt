package com.example.data.remote

import com.example.data.local.entity.CustomerRegistrationRequestEntity
import com.example.data.local.entity.SupplierRegistrationRequestEntity
import com.example.data.local.entity.LeadEntity
import com.example.data.local.entity.BrandEntity
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

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
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("deletion_requests").addValueEventListener(listener)
        return listener
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

    fun sanitizeEmail(email: String): String {
        return email.trim().lowercase().replace(".", "_").replace("@", "_at_")
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

            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("super_admins").addValueEventListener(listener)
        return listener
    }

    suspend fun syncVisit(visit: VisitEntity) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("visits").child(visit.id.toString()).setValue(visit)
        } catch (e: Exception) {
            // Cloud sync fails gracefully when offline
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

    suspend fun syncPurchaseEntry(entry: PurchaseEntryEntity) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("purchase_entries").child(entry.id.toString()).setValue(entry)
        } catch (e: Exception) {
            // Ignore
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
            rootRef.child("customers").child(customer.id.toString()).setValue(customer)
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
            rootRef.child("suppliers").child(supplier.id.toString()).setValue(supplier)

            // If manufacturer, also sync directly to dedicated manufacturers node for instant visibility
            if (supplier.type.equals("Manufacturer", ignoreCase = true)) {
                rootRef.child("manufacturers").child(supplier.id.toString()).setValue(supplier)
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
            rootRef.child("products").child(product.id.toString()).setValue(product)
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
            rootRef.child("employees").child(employee.id.toString()).setValue(employee)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun deleteEmployee(employeeId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("employees").child(employeeId.toString()).removeValue()
        } catch (e: Exception) {
            // Ignore
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
            rootRef.child("visits").child(visit.id.toString()).setValue(updated)
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
            rootRef.child("purchase_entries").child(entry.id.toString()).setValue(updated)
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
            rootRef.child("customers").child(customer.id.toString()).setValue(updated)
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
            rootRef.child("suppliers").child(supplier.id.toString()).setValue(updated)
            if (supplier.type.equals("Manufacturer", ignoreCase = true)) {
                rootRef.child("manufacturers").child(supplier.id.toString()).setValue(updated)
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
            rootRef.child("products").child(product.id.toString()).setValue(updated)
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
            rootRef.child("brands").child(brand.id.toString()).setValue(data)
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
            rootRef.child("transporters").child(transporter.id.toString()).setValue(data)
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
            rootRef.child("markets").child(market.id.toString()).setValue(data)
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
                        is CustomerEntity -> item.copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is SupplierEntity -> item.copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is ProductEntity -> item.copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is EmployeeEntity -> item.copy(
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
                            item.copy(
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
                        is TransporterEntity -> item.copy(
                            id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                            isDeleted = effectivelyDeleted || item.isDeleted
                        )
                        is MarketEntity -> item.copy(
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
                        else -> true
                    }
                    if (isValidId) {
                        @Suppress("UNCHECKED_CAST")
                        list.add(fixedItem as T)
                    }
                }
            } catch (_: Exception) {}
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
                // Also merge /manufacturers to ensure manufacturers entered under dedicated node are included
                rootRef.child("manufacturers").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(manSnapshot: DataSnapshot) {
                        for (item in manSnapshot.extractList<SupplierEntity>()) {
                            if (item.id > 0L) {
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


    // Realtime Downstream Listeners
    fun listenToEmployees(onUpdate: (List<EmployeeEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<EmployeeEntity>())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("employees").addValueEventListener(listener)
        return listener
    }

    fun listenToCustomers(onUpdate: (List<CustomerEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<CustomerEntity>())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("customers").addValueEventListener(listener)
        return listener
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
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("suppliers").addValueEventListener(listener)
        return listener
    }

    fun listenToProducts(onUpdate: (List<ProductEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<ProductEntity>())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("products").addValueEventListener(listener)
        return listener
    }

    fun listenToVisits(onUpdate: (List<VisitEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<VisitEntity>())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("visits").addValueEventListener(listener)
        return listener
    }

    fun listenToPurchaseEntries(onUpdate: (List<PurchaseEntryEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<PurchaseEntryEntity>())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("purchase_entries").addValueEventListener(listener)
        return listener
    }

    fun listenToTransactions(onUpdate: (List<TransactionEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<TransactionEntity>())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("transactions").addValueEventListener(listener)
        return listener
    }

    fun listenToPackGroups(onUpdate: (List<PackGroupEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<PackGroupEntity>())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("pack_groups").addValueEventListener(listener)
        return listener
    }

    fun listenToBrands(onUpdate: (List<BrandEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<BrandEntity>())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("brands").addValueEventListener(listener)
        return listener
    }

    fun listenToTransporters(onUpdate: (List<TransporterEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<TransporterEntity>())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("transporters").addValueEventListener(listener)
        return listener
    }

    fun listenToMarkets(onUpdate: (List<MarketEntity>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.extractList<MarketEntity>())
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("markets").addValueEventListener(listener)
        return listener
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
            garmentTypes = child("garmentTypes").getValue(String::class.java) ?: "",
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
            notes = child("notes").getValue(String::class.java) ?: "",
            status = child("status").getValue(String::class.java) ?: "PENDING",
            phoneVerified = child("phoneVerified").getValue(Boolean::class.java) ?: true,
            createdAt = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
            approvedAt = child("approvedAt").getValue(Long::class.java),
            approvedBy = child("approvedBy").getValue(String::class.java) ?: "",
            assignedAgentId = child("assignedAgentId").getValue(Long::class.java),
            assignedAgentName = child("assignedAgentName").getValue(String::class.java) ?: "",
            creditType = child("creditType").getValue(String::class.java) ?: "Cash",
            creditDays = child("creditDays").getValue(Int::class.java) ?: 30,
            creditLimit = child("creditLimit").getValue(Double::class.java) ?: 0.0,
            religion = child("religion").getValue(String::class.java) ?: "",
            createdCustomerId = child("createdCustomerId").getValue(Long::class.java),
            rejectionReason = child("rejectionReason").getValue(String::class.java) ?: ""
        )
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
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("leads").addValueEventListener(listener)
        return listener
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
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("customer_registration_requests").addValueEventListener(listener)
        return listener
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
            marketArea = child("marketArea").getValue(String::class.java) ?: "",
            city = child("city").getValue(String::class.java) ?: "Ahmedabad",
            district = child("district").getValue(String::class.java) ?: "",
            state = child("state").getValue(String::class.java) ?: "Gujarat",
            pincode = child("pincode").getValue(String::class.java) ?: "",
            mapLink = child("mapLink").getValue(String::class.java) ?: "",
            productsMade = child("productsMade").getValue(String::class.java) ?: "",
            categories = child("categories").getValue(String::class.java) ?: "",
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
            override fun onCancelled(error: DatabaseError) {}
        }
        rootRef.child("supplier_registration_requests").addValueEventListener(listener)
        return listener
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
        approvedBy: String
    ) = suspendCancellableCoroutine<Unit> { cont ->
        val updates = mapOf(
            "status" to "APPROVED",
            "approvedAt" to System.currentTimeMillis(),
            "approvedBy" to approvedBy,
            "brand" to brand,
            "marketArea" to marketName,
            "createdSupplierId" to newSupplierId
        )
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

