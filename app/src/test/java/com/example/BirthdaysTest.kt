package com.example

import com.example.util.Birthdays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.util.Calendar
import org.junit.Test

/**
 * Dates of birth arrive in two shapes: `yyyy-MM-dd` from the web form's date input, and free text
 * like `29/09/1985` from the Android master screen. A reminder on the wrong day is worse than none,
 * so anything ambiguous has to come back as null rather than a guess.
 */
class BirthdaysTest {

    private fun on(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance().apply { set(year, month - 1, day, 12, 0, 0) }

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    @Test
    fun readsTheWebFormat_yearFirst() {
        val parsed = Birthdays.parse("1985-09-29")

        assertEquals(9, parsed?.month)
        assertEquals(29, parsed?.day)
    }

    @Test
    fun readsWhatPeopleType_dayFirst() {
        val parsed = Birthdays.parse("29/09/1985")

        assertEquals(9, parsed?.month)
        assertEquals(29, parsed?.day)
    }

    @Test
    fun acceptsDotsDashesAndSingleDigits() {
        listOf("29.9.1985", "29-9-1985", "1985/9/29", " 29 / 09 / 1985 ").forEach { raw ->
            val parsed = Birthdays.parse(raw)
            assertEquals("failed on '$raw'", 9, parsed?.month)
            assertEquals("failed on '$raw'", 29, parsed?.day)
        }
    }

    @Test
    fun rejectsAnythingWithoutAFourDigitYear() {
        // Could be 09/10 or 10/09 — guessing would wish somebody on the wrong day
        assertNull(Birthdays.parse("29/09"))
        assertNull(Birthdays.parse("29/09/85"))
    }

    @Test
    fun rejectsBlankAndNonsense() {
        assertNull(Birthdays.parse(null))
        assertNull(Birthdays.parse(""))
        assertNull(Birthdays.parse("   "))
        assertNull(Birthdays.parse("not a date"))
        assertNull(Birthdays.parse("1985-13-29"))
        assertNull(Birthdays.parse("1985-09-45"))
    }

    // -------------------------------------------------------------------------
    // Is it today
    // -------------------------------------------------------------------------

    @Test
    fun birthdayToday_ignoresTheYear() {
        assertTrue(Birthdays.isBirthdayToday("1985-09-29", on(2026, 9, 29)))
        assertTrue(Birthdays.isBirthdayToday("29/09/1985", on(2026, 9, 29)))
    }

    @Test
    fun aDifferentDayIsNotToday() {
        assertFalse(Birthdays.isBirthdayToday("1985-09-28", on(2026, 9, 29)))
        assertFalse(Birthdays.isBirthdayToday("1985-10-29", on(2026, 9, 29)))
    }

    @Test
    fun anUnreadableDateIsNeverABirthday() {
        assertFalse(Birthdays.isBirthdayToday("", on(2026, 9, 29)))
        assertFalse(Birthdays.isBirthdayToday("29/09", on(2026, 9, 29)))
    }

    // -------------------------------------------------------------------------
    // Age and the daily key
    // -------------------------------------------------------------------------

    @Test
    fun ageTurningToday() {
        assertEquals(41, Birthdays.ageTurningToday("1985-09-29", on(2026, 9, 29)))
        assertEquals(41, Birthdays.ageTurningToday("29/09/1985", on(2026, 9, 29)))
    }

    @Test
    fun anImpossibleAgeIsReportedAsUnknown() {
        assertNull(Birthdays.ageTurningToday("1800-09-29", on(2026, 9, 29)))
        assertNull(Birthdays.ageTurningToday("2026-09-29", on(2026, 9, 29)))
        assertNull(Birthdays.ageTurningToday("29/09", on(2026, 9, 29)))
    }

    @Test
    fun todayKeyIsPaddedSoItSortsAndMatchesTheFunction() {
        assertEquals("20260929", Birthdays.todayKey(on(2026, 9, 29)))
        assertEquals("20260101", Birthdays.todayKey(on(2026, 1, 1)))
    }
}
