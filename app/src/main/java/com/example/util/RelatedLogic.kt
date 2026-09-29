package com.example.util

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.PurchaseEntryEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.VisitEntity

/**
 * "Show me everything linked to this master" lookups, shared by all master detail screens.
 * Pure functions (no Android types) so they are easy to unit test.
 */
object RelatedLogic {

    fun referredCustomers(all: List<CustomerEntity>, type: String, id: Long, names: List<String>): List<CustomerEntity> =
        all.filter { !it.isDeleted && isReferredBy(it.referredBy, it.referredByType, it.referredById, type, id, names) }

    fun referredSuppliers(all: List<SupplierEntity>, type: String, id: Long, names: List<String>): List<SupplierEntity> =
        all.filter { !it.isDeleted && isReferredBy(it.referredBy, it.referredByType, it.referredById, type, id, names) }

    fun referredPeople(all: List<EmployeeEntity>, type: String, id: Long, names: List<String>): List<EmployeeEntity> =
        all.filter { !it.isDeleted && it.id != id && isReferredBy(it.referredBy, it.referredByType, it.referredById, type, id, names) }

    /** Customers a Sub Agent brought in (by id; name match only for records saved before ids existed). */
    fun customersOfSubAgent(all: List<CustomerEntity>, agent: EmployeeEntity): List<CustomerEntity> =
        all.filter { c ->
            !c.isDeleted && (
                c.subAgentId == agent.id ||
                    (c.subAgentId == null && agent.name.isNotBlank() && c.subAgentName.trim().equals(agent.name.trim(), ignoreCase = true))
                )
        }

    /** Customers a staff member added / handles. */
    fun customersAddedBy(all: List<CustomerEntity>, emp: EmployeeEntity): List<CustomerEntity> =
        all.filter { c ->
            !c.isDeleted && (
                c.addedByAgentId == emp.id ||
                    (c.addedByAgentId == null && emp.name.isNotBlank() && c.addedByAgentName.trim().equals(emp.name.trim(), ignoreCase = true))
                )
        }

    fun tripsOfCustomers(visits: List<VisitEntity>, customerIds: Set<Long>): List<VisitEntity> =
        visits.filter { !it.isDeleted && it.customerId in customerIds }

    /**
     * Every trip of one customer. Linked by id; a trip whose customerId is missing (0) or points at a
     * customer record that no longer exists (e.g. a duplicate removed during sync) is matched by the
     * exact shop / owner name, so its orders never disappear from the customer master.
     * Does not filter soft-deleted trips (callers decide).
     */
    fun tripsOfCustomer(
        visits: List<VisitEntity>,
        customer: CustomerEntity,
        knownCustomerIds: Set<Long>
    ): List<VisitEntity> {
        val names = listOf(customer.firmName, customer.name).map { it.trim() }.filter { it.isNotBlank() }
        return visits.filter { v ->
            v.customerId == customer.id || (
                (v.customerId <= 0L || v.customerId !in knownCustomerIds) &&
                    v.customerName.isNotBlank() &&
                    names.any { it.equals(v.customerName.trim(), ignoreCase = true) }
                )
        }
    }

    /**
     * The customer of one trip: by id, else (orphaned trip, see [tripsOfCustomer]) the only customer
     * whose shop / owner name matches the trip's customer name exactly. Null when unknown or ambiguous.
     */
    fun customerOfTrip(visit: VisitEntity, customers: List<CustomerEntity>): CustomerEntity? {
        if (visit.customerId > 0L) customers.firstOrNull { it.id == visit.customerId }?.let { return it }
        val name = visit.customerName.trim()
        if (name.isBlank()) return null
        return customers
            .filter { c -> listOf(c.firmName, c.name).any { it.trim().equals(name, ignoreCase = true) } }
            .distinctBy { it.id }
            .singleOrNull()
    }

    /**
     * visitId -> customerId for counting / grouping, with the same orphan fallback as [tripsOfCustomer]
     * (an orphaned trip is credited to the customer whose name matches exactly, when there is exactly one).
     */
    fun customerIdByTrip(visits: List<VisitEntity>, customers: List<CustomerEntity>): Map<Long, Long> {
        val known = customers.map { it.id }.toSet()
        val byName = HashMap<String, MutableSet<Long>>()
        customers.forEach { c ->
            listOf(c.firmName, c.name).map { it.trim().lowercase() }.filter { it.isNotBlank() }.forEach { n ->
                byName.getOrPut(n) { mutableSetOf() }.add(c.id)
            }
        }
        val result = HashMap<Long, Long>()
        visits.forEach { v ->
            val id = when {
                v.customerId > 0L && v.customerId in known -> v.customerId
                else -> byName[v.customerName.trim().lowercase()]?.singleOrNull() ?: v.customerId
            }
            result[v.id] = id
        }
        return result
    }

    fun ordersOfTrips(entries: List<PurchaseEntryEntity>, tripIds: Set<Long>): List<PurchaseEntryEntity> =
        entries.filter { !it.isDeleted && it.visitId in tripIds }

    /** Trips a staff member started, co-owned or joined. */
    fun tripsOfStaff(visits: List<VisitEntity>, emp: EmployeeEntity): List<VisitEntity> =
        visits.filter { !it.isDeleted && it.hasMember(emp) }

    /**
     * Orders credited to a staff member: they are the order's salesman. Orders saved before
     * per-order salesmen existed count for the trip starter.
     */
    fun ordersOfStaff(entries: List<PurchaseEntryEntity>, visitsById: Map<Long, VisitEntity>, emp: EmployeeEntity): List<PurchaseEntryEntity> =
        entries.filter { e ->
            if (e.isDeleted) return@filter false
            when {
                e.salesmanId > 0L -> e.salesmanId == emp.id
                e.salesmanName.isNotBlank() -> e.salesmanName.trim().equals(emp.name.trim(), ignoreCase = true)
                else -> visitsById[e.visitId]?.let { it.employeeId == emp.id } ?: false
            }
        }

    fun filterTripsByDate(visits: List<VisitEntity>, filter: DateRangeFilter): List<VisitEntity> =
        if (!filter.isActive) visits else visits.filter { filter.matches(it.date) }

    fun filterOrdersByDate(
        entries: List<PurchaseEntryEntity>,
        visitsById: Map<Long, VisitEntity>,
        filter: DateRangeFilter
    ): List<PurchaseEntryEntity> =
        if (!filter.isActive) entries else entries.filter { filter.matches(it.effectiveDate(visitsById[it.visitId])) }
}
