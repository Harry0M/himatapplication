package com.example

import com.example.data.local.entity.EmployeeEntity
import com.example.util.StaffCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Staff codes used to be a random digit 1..9, which is why two people ended up sharing one. These
 * tests hold the replacement to the only rule that matters: never hand out a code somebody has.
 */
class StaffCodesTest {

    private fun staff(id: Long, code: String, role: String = "Salesman", removed: Boolean = false) =
        EmployeeEntity(id = id, employeeId = code, name = "Person $id", role = role, isDeleted = removed)

    @Test
    fun firstCodeOnAnEmptyTeam() {
        assertEquals("EMP-01", StaffCodes.next(emptyList()))
    }

    @Test
    fun nextCodeContinuesFromTheHighestInUse() {
        val team = listOf(staff(1, "EMP-01"), staff(2, "EMP-04"), staff(3, "EMP-02"))

        assertEquals("EMP-05", StaffCodes.next(team))
    }

    @Test
    fun aDepartedStaffMembersCodeIsNotHandedOutAgain() {
        val team = listOf(staff(1, "EMP-01"), staff(2, "EMP-02", removed = true))

        assertEquals("EMP-03", StaffCodes.next(team))
    }

    @Test
    fun pastNine_theCodeStopsPaddingWithZero() {
        val team = (1..9).map { staff(it.toLong(), "EMP-0$it") }

        assertEquals("EMP-10", StaffCodes.next(team))
    }

    @Test
    fun agentCodesAreCountedSeparatelyFromStaffCodes() {
        val team = listOf(staff(1, "EMP-07"), staff(2, "AGT-02", role = "Agent"))

        assertEquals("EMP-08", StaffCodes.next(team, StaffCodes.STAFF_PREFIX))
        assertEquals("AGT-03", StaffCodes.nextAgent(team))
    }

    @Test
    fun anIdThatLeakedIntoTheCodeFieldIsIgnored() {
        // The web used to label salesmen found only on trips as "EMP-0<numeric id>"
        val team = listOf(staff(1, "EMP-01"), staff(2, "EMP-01759240000123456"))

        assertEquals("EMP-02", StaffCodes.next(team))
    }

    @Test
    fun blankAndUnparseableCodesAreIgnored() {
        val team = listOf(staff(1, ""), staff(2, "EMP-"), staff(3, "EMP-abc"), staff(4, "EMP-03"))

        assertEquals("EMP-04", StaffCodes.next(team))
    }

    @Test
    fun aCodeSomebodyElseHolds_isReportedAsTaken() {
        val team = listOf(staff(1, "EMP-01"), staff(2, "EMP-02"))

        assertTrue(StaffCodes.isTaken("EMP-02", team, selfId = 1))
        assertTrue("Comparison must ignore case", StaffCodes.isTaken("emp-02", team, selfId = 1))
    }

    @Test
    fun myOwnCode_isNotReportedAsTaken() {
        val team = listOf(staff(1, "EMP-01"), staff(2, "EMP-02"))

        assertFalse(StaffCodes.isTaken("EMP-02", team, selfId = 2))
    }

    @Test
    fun blankCode_isNotReportedAsTaken() {
        assertFalse(StaffCodes.isTaken("", listOf(staff(1, "EMP-01"))))
    }
}
