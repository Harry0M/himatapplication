package com.example.util

/**
 * What the signed-in person is allowed to do, derived from the same role the rest of the app uses.
 *
 * This exists so the "what can I do?" sheet cannot drift from reality: it is computed from the role
 * rather than written out by hand in the UI. If a rule changes, it changes here and the sheet follows.
 */
data class Permission(
    val title: String,
    val allowed: Boolean,
    /** Why, in plain words. Shown under the title when it explains something useful. */
    val note: String = ""
)

data class PermissionProfile(
    val roleLabel: String,
    val roleNote: String,
    val permissions: List<Permission>
) {
    val allowedCount: Int get() = permissions.count { it.allowed }
    val blockedCount: Int get() = permissions.count { !it.allowed }
}

object Permissions {

    /**
     * [isOwner] is the super-admin flag (listed under `super_admins`), [role] is the employee record's
     * role. [hasStaffRecord] matters because a few actions credit work to a staff member.
     */
    fun profileFor(role: String?, isOwner: Boolean, hasStaffRecord: Boolean): PermissionProfile {
        val agent = Roles.isAgent(role)
        val admin = isOwner || Roles.isAdmin(role)
        val staff = !agent && !admin

        val roleLabel = when {
            isOwner && Roles.isAdmin(role) -> "Admin (owner)"
            isOwner -> "Owner, viewing as ${Roles.label(role)}"
            admin -> "Admin"
            agent -> "Sub Agent"
            else -> "Staff (Salesman)"
        }

        val roleNote = when {
            admin -> "You can see and change everything, and you decide on deletions."
            agent -> "Read-only access to the customers linked to you."
            else -> "You can do all the day-to-day work. Deletions go to the Admin."
        }

        val permissions = listOf(
            Permission(
                title = "See every trip and order in the agency",
                allowed = !agent,
                note = if (agent) "You only see trips and orders of your own customers" else "Nothing is hidden from staff"
            ),
            Permission(
                title = "Start a trip and add orders",
                allowed = !agent,
                note = if (agent) "Sub Agent logins are read-only" else ""
            ),
            Permission(
                title = "Join a trip somebody else started",
                allowed = !agent && hasStaffRecord,
                note = when {
                    agent -> "Sub Agent logins are read-only"
                    !hasStaffRecord -> "Your login is not linked to a staff record yet, so orders cannot be credited to you"
                    else -> "Your orders are then credited to your name"
                }
            ),
            Permission(
                title = "Edit an order and update delivery",
                allowed = !agent,
                note = if (agent) "Sub Agent logins are read-only" else ""
            ),
            Permission(
                title = "Add and edit customers, suppliers and other masters",
                allowed = !agent,
                note = if (agent) "Sub Agent logins are read-only" else ""
            ),
            Permission(
                title = "Delete a record for good",
                allowed = admin,
                note = if (admin) {
                    "You are asked first what else is attached to it"
                } else {
                    "Your delete hides the record and asks the Admin to confirm. Nothing is lost."
                }
            ),
            Permission(
                title = "Approve or reject what others deleted",
                allowed = admin,
                note = if (admin) "In More → Delete requests" else "Only an Admin decides on deletions"
            ),
            Permission(
                title = "Add, edit or remove staff",
                allowed = admin,
                note = if (admin) "" else "Only an Admin manages the staff master"
            ),
            Permission(
                title = "Approve customer and supplier registrations",
                allowed = admin,
                note = if (admin) "From the Requests button in the header" else "Only an Admin approves registrations"
            ),
            Permission(
                title = "Register a Sub Agent",
                allowed = !agent,
                note = if (admin) "You can also give them a login email" else if (staff) "An Admin gives them their login email" else "Sub Agent logins are read-only"
            ),
            Permission(
                title = "Get notified about the team's new trips and orders",
                allowed = !agent,
                note = if (agent) "Sub Agents are left out of team notifications" else "Your own work does not notify you"
            ),
            Permission(
                title = "View as another salesman",
                allowed = isOwner,
                note = if (isOwner) "In Profile → Switch view" else "Owner only"
            )
        )

        return PermissionProfile(roleLabel = roleLabel, roleNote = roleNote, permissions = permissions)
    }
}
