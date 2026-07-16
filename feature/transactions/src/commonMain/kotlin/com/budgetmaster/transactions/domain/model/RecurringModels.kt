@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.transactions.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** How often a recurring entry repeats. */
enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    /**
     * The next occurrence after [from].
     *
     * Steps by calendar unit rather than a fixed number of milliseconds, so months and years
     * keep their real length and DST shifts don't drift the time of day. Month-end is handled
     * by kotlinx-datetime (e.g. Jan 31 + 1 month → Feb 28/29).
     *
     * @param from epoch-ms of the current occurrence.
     */
    fun next(from: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
        val current = Instant.fromEpochMilliseconds(from).toLocalDateTime(timeZone)
        val nextDate = when (this) {
            DAILY -> current.date.plus(1, DateTimeUnit.DAY)
            WEEKLY -> current.date.plus(1, DateTimeUnit.WEEK)
            MONTHLY -> current.date.plus(1, DateTimeUnit.MONTH)
            YEARLY -> current.date.plus(1, DateTimeUnit.YEAR)
        }
        return LocalDateTime(nextDate, current.time).toInstant(timeZone).toEpochMilliseconds()
    }
}

/**
 * A scheduled entry that materializes into real transactions as its due dates pass.
 *
 * @property amount signed like a transaction: negative is an expense, positive income.
 * @property nextRunDate epoch-ms of the next occurrence still to be created.
 * @property isActive paused schedules keep their history but stop producing entries.
 */
data class RecurringTransaction(
    val id: String,
    val accountId: String,
    val categoryId: String?,
    val amount: Double,
    val description: String,
    val frequency: Frequency,
    val startDate: Long,
    val nextRunDate: Long,
    val isActive: Boolean,
) {
    val isExpense: Boolean get() = amount < 0
}

/** Editable payload for creating (id == null) or updating a schedule. */
data class RecurringDraft(
    val id: String? = null,
    val accountId: String? = null,
    val categoryId: String?,
    val amountAbs: Double,
    val isExpense: Boolean,
    val description: String,
    val frequency: Frequency,
    val startDate: Long,
)
