package com.example.data.remote

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
            rootRef.child("brands").child(brand.id.toString()).setValue(brand)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun deleteBrand(brandId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("brands").child(brandId.toString()).removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncTransporter(transporter: TransporterEntity) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("transporters").child(transporter.id.toString()).setValue(transporter)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun deleteTransporter(transporterId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("transporters").child(transporterId.toString()).removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun syncMarket(market: MarketEntity) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("markets").child(market.id.toString()).setValue(market)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun deleteMarket(marketId: Long) = withContext(Dispatchers.IO) {
        try {
            rootRef.child("markets").child(marketId.toString()).removeValue()
        } catch (e: Exception) {
            // Ignore
        }
    }


    // Downstream Deserialization Helper
    private inline fun <reified T> DataSnapshot.extractList(): List<T> {
        val list = mutableListOf<T>()
        for (child in children) {
            try {
                if (child.key == "0" || child.key == "null") {
                    child.ref.removeValue()
                    continue
                }
                val item = child.getValue(T::class.java)
                if (item != null) {
                    val keyLong = child.key?.toLongOrNull() ?: 0L
                    val fixedItem = when (item) {
                        is CustomerEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
                        is SupplierEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
                        is ProductEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
                        is EmployeeEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
                        is VisitEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
                        is PurchaseEntryEntity -> {
                            val packGroupId = child.child("packGroupId").getValue(Long::class.java)
                                ?: item.packGroupId
                            val mixedPackNote = child.child("mixedPackNote").getValue(String::class.java)
                                ?: item.mixedPackNote
                            item.copy(
                                id = if (item.id <= 0L && keyLong > 0L) keyLong else item.id,
                                packGroupId = packGroupId,
                                mixedPackNote = mixedPackNote
                            )
                        }
                        is TransactionEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
                        is PackGroupEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
                        is BrandEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
                        is TransporterEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
                        is MarketEntity -> if (item.id <= 0L && keyLong > 0L) item.copy(id = keyLong) else item
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
}

