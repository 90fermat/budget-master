@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.util

import androidx.compose.runtime.Composable
import budgetmaster.core.generated.resources.Res
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
import kotlinx.datetime.LocalDate
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
fun monthYearLabel(date: LocalDate = DateUtils.toLocalDate(Clock.System.now().toEpochMilliseconds())): String {
    val month = stringResource(
        when (date.monthNumber) {
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
    return stringResource(Res.string.month_year, month, date.year.toString())
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
