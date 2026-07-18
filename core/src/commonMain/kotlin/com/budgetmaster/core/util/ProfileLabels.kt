@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.util

import androidx.compose.runtime.Composable
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.date_day_at_time
import budgetmaster.core.generated.resources.date_month_day
import budgetmaster.core.generated.resources.month_1
import budgetmaster.core.generated.resources.month_10
import budgetmaster.core.generated.resources.month_11
import budgetmaster.core.generated.resources.month_12
import budgetmaster.core.generated.resources.month_2
import budgetmaster.core.generated.resources.month_3
import budgetmaster.core.generated.resources.month_4
import budgetmaster.core.generated.resources.month_5
import budgetmaster.core.generated.resources.month_6
import budgetmaster.core.generated.resources.month_7
import budgetmaster.core.generated.resources.month_8
import budgetmaster.core.generated.resources.month_9
import budgetmaster.core.generated.resources.month_year
import budgetmaster.core.generated.resources.time_am
import budgetmaster.core.generated.resources.time_hour_minute
import budgetmaster.core.generated.resources.time_pm
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * "June 2026" / "juin 2026" for [date].
 *
 * Month names come from string resources rather than a platform date formatter on purpose: a
 * platform formatter follows the *system* locale, while this app has its own language setting
 * (`LocalAppLocale`). Resources follow the language the user actually chose.
 */
@Composable
fun monthYearLabel(date: LocalDate = DateUtils.toLocalDate(Clock.System.now().toEpochMilliseconds())): String =
    stringResource(Res.string.month_year, monthName(date.month.number), date.year.toString())

/** The month's name in the app's language, for a 1-based [monthNumber]. */
@Composable
fun monthName(monthNumber: Int): String = stringResource(
    when (monthNumber) {
        1 -> Res.string.month_1
        2 -> Res.string.month_2
        3 -> Res.string.month_3
        4 -> Res.string.month_4
        5 -> Res.string.month_5
        6 -> Res.string.month_6
        7 -> Res.string.month_7
        8 -> Res.string.month_8
        9 -> Res.string.month_9
        10 -> Res.string.month_10
        11 -> Res.string.month_11
        else -> Res.string.month_12
    },
)

/**
 * "January 20, 10:45 PM" / "20 janvier à 22:45" for an epoch-millis [timestamp].
 *
 * Both the day/month order and the 12- vs 24-hour clock are locale conventions, so they live in
 * the `date_month_day` and `time_hour_minute` format strings rather than here: this builds every
 * component and each translation uses the placeholders its convention calls for.
 */
@Composable
fun dateTimeLabel(timestamp: Long): String {
    val dateTime = DateUtils.toLocalDateTime(timestamp)
    val day = stringResource(Res.string.date_month_day, monthName(dateTime.date.month.number), dateTime.date.day)

    val hour24 = dateTime.hour
    val hour12 = when (hour24 % 12) {
        0 -> 12
        else -> hour24 % 12
    }
    val time = stringResource(
        Res.string.time_hour_minute,
        hour24.toString().padStart(2, '0'),
        hour12.toString(),
        dateTime.minute.toString().padStart(2, '0'),
        stringResource(if (hour24 >= 12) Res.string.time_pm else Res.string.time_am),
    )
    return stringResource(Res.string.date_day_at_time, day, time)
}

/**
 * Up to two initials for an avatar: "John Doe" → "JD", "cyrille" → "C".
 *
 * Returns an empty string for a blank name, so callers can fall back to an icon rather than
 * render an empty circle.
 */
fun initialsOf(name: String): String =
    name.trim()
        .split(' ', '.', '-', '_')
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
