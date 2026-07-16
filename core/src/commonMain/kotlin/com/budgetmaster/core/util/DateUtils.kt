@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Where a date falls relative to today, for localized day-group headers. */
enum class RelativeDay { TODAY, YESTERDAY, OLDER }

/** Epoch-millisecond utilities shared across features. */
object DateUtils {
    /** Converts an epoch-millisecond [timestamp] to a [LocalDate] in the given [timeZone]. */
    fun toLocalDate(
        timestamp: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): LocalDate = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone).date

    /** Classifies [date] as today, yesterday, or older relative to the current day. */
    fun relativeDay(
        date: LocalDate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): RelativeDay {
        val today = Clock.System.todayIn(timeZone)
        return when (date) {
            today -> RelativeDay.TODAY
            today.minus(1, DateTimeUnit.DAY) -> RelativeDay.YESTERDAY
            else -> RelativeDay.OLDER
        }
    }

    /**
     * The epoch-millisecond `[start, end]` bounds of the current calendar month,
     * where `start` is the first instant of the 1st and `end` is the last millisecond
     * before the next month begins.
     */
    fun currentMonthRange(
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): LongRange {
        val today = Clock.System.todayIn(timeZone)
        val monthStart = LocalDate(today.year, today.month, 1)
        val nextMonthStart = monthStart.plus(1, DateTimeUnit.MONTH)
        val startMs = monthStart.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endMs = nextMonthStart.atStartOfDayIn(timeZone).toEpochMilliseconds() - 1
        return startMs..endMs
    }
}
