package com.example.util

import java.util.Calendar

/**
 * Reads a customer's date of birth well enough to spot a birthday.
 *
 * The field is genuinely messy, and that is not something to fix by guessing: the public web form
 * uses an HTML date input so it always writes `yyyy-MM-dd`, while the Android master screen is a
 * plain text box with a `DD/MM/YYYY` placeholder and no validation. Years of records exist in both
 * shapes, plus the usual dots and single-digit days.
 *
 * So this parser is deliberately forgiving about separators and about which way round the day and
 * year are, and deliberately strict about anything ambiguous: when a value cannot be read with
 * confidence it returns null and the customer simply gets no reminder. A wrong birthday message to a
 * customer is worse than no message.
 */
object Birthdays {

    /** Day and month of birth, or null when the value cannot be read. Month is 1..12. */
    data class MonthDay(val month: Int, val day: Int)

    private val separators = charArrayOf('-', '/', '.', ' ')

    /**
     * Splits on any common separator and works out which part is the year by its length.
     *
     * Two orders are accepted: year first (`2026-09-29`, what the web writes) and day first
     * (`29/09/2026`, what people type). A value with no four-digit year is rejected rather than
     * guessed at.
     */
    fun parse(raw: String?): MonthDay? {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return null

        val parts = text.split(*separators).filter { it.isNotBlank() }
        if (parts.size < 3) return null

        val numbers = parts.take(3).map { it.trim().toIntOrNull() ?: return null }

        val (day, month) = when {
            // yyyy-MM-dd
            parts[0].trim().length == 4 -> numbers[2] to numbers[1]
            // dd-MM-yyyy
            parts[2].trim().length == 4 -> numbers[0] to numbers[1]
            else -> return null
        }

        if (month !in 1..12) return null
        if (day !in 1..31) return null
        return MonthDay(month = month, day = day)
    }

    /** The year of birth, when the value can be read. Used only to say "turning 41". */
    fun birthYear(raw: String?): Int? {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return null
        val parts = text.split(*separators).filter { it.isNotBlank() }
        if (parts.size < 3) return null
        val year = when {
            parts[0].trim().length == 4 -> parts[0].trim().toIntOrNull()
            parts[2].trim().length == 4 -> parts[2].trim().toIntOrNull()
            else -> null
        } ?: return null
        return year.takeIf { it in 1900..2200 }
    }

    /** True when [raw] is a readable date whose day and month are today's. */
    fun isBirthdayToday(raw: String?, today: Calendar = Calendar.getInstance()): Boolean {
        val parsed = parse(raw) ?: return false
        return parsed.month == today.get(Calendar.MONTH) + 1 &&
            parsed.day == today.get(Calendar.DAY_OF_MONTH)
    }

    /** How old they turn today, or null when the year is missing or nonsense. */
    fun ageTurningToday(raw: String?, today: Calendar = Calendar.getInstance()): Int? {
        val year = birthYear(raw) ?: return null
        val age = today.get(Calendar.YEAR) - year
        return age.takeIf { it in 1..120 }
    }

    /** Today as `yyyyMMdd`. Used in a notification key so one birthday is announced once a day. */
    fun todayKey(today: Calendar = Calendar.getInstance()): String {
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH) + 1
        val day = today.get(Calendar.DAY_OF_MONTH)
        return "%04d%02d%02d".format(year, month, day)
    }
}
