package com.budgetmaster.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Budget Master brand mark: a coin ring with a rising "growth" spark.
 *
 * The ring is **palette-aware** — by default it uses the active theme's primary→tertiary
 * gradient, so the mark restyles with the selected [AppPalette]. The growth spark stays
 * the semantic income green ([FinancialColors.income]) regardless of palette.
 *
 * Rendered as vector geometry (Canvas) so it scales crisply at any size.
 */
@Composable
fun AppLogoMark(
    modifier: Modifier = Modifier,
    ringColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
    ),
    sparkColor: Color = MaterialTheme.financialColors.income,
) {
    Canvas(modifier) {
        val s = size.minDimension
        fun p(nx: Float, ny: Float) = Offset(size.width * nx, size.height * ny)

        // Coin ring
        drawCircle(
            brush = Brush.linearGradient(
                ringColors,
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
            ),
            radius = s * 0.325f,
            center = p(0.5f, 0.517f),
            style = Stroke(width = s * 0.092f),
        )

        val sparkStroke = Stroke(
            width = s * 0.066f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        // Rising growth line
        drawPath(
            path = Path().apply {
                moveTo(p(0.333f, 0.617f).x, p(0.333f, 0.617f).y)
                lineTo(p(0.45f, 0.483f).x, p(0.45f, 0.483f).y)
                lineTo(p(0.533f, 0.55f).x, p(0.533f, 0.55f).y)
                lineTo(p(0.683f, 0.367f).x, p(0.683f, 0.367f).y)
            },
            color = sparkColor,
            style = sparkStroke,
        )
        // Arrow head
        drawPath(
            path = Path().apply {
                moveTo(p(0.575f, 0.367f).x, p(0.575f, 0.367f).y)
                lineTo(p(0.683f, 0.367f).x, p(0.683f, 0.367f).y)
                lineTo(p(0.683f, 0.475f).x, p(0.683f, 0.475f).y)
            },
            color = sparkColor,
            style = sparkStroke,
        )
    }
}

/**
 * The "Budget Master" wordmark: "Budget" bold, "Master" light and letter-spaced.
 * The brand name is a proper noun and intentionally not localized.
 */
@Composable
fun AppWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 26.sp,
    color: Color = MaterialTheme.colorScheme.onBackground,
    mutedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        modifier = modifier,
        fontSize = fontSize,
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp, color = color)) {
                append("Budget")
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Normal, letterSpacing = 1.sp, color = mutedColor)) {
                append(" Master")
            }
        },
    )
}

/**
 * The full brand lockup: [AppLogoMark] + [AppWordmark], laid out horizontally or,
 * when [stacked] is true, vertically (for splash/hero use).
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
    markSize: Dp = 48.dp,
    wordmarkSize: TextUnit = 26.sp,
) {
    if (stacked) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppLogoMark(Modifier.size(markSize))
            Spacer(Modifier.height(Spacing.medium))
            AppWordmark(fontSize = wordmarkSize)
        }
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            AppLogoMark(Modifier.size(markSize))
            Spacer(Modifier.width(Spacing.compact))
            AppWordmark(fontSize = wordmarkSize)
        }
    }
}
