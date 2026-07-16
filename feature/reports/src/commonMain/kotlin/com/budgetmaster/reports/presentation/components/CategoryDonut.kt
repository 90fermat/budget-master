package com.budgetmaster.reports.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.budgetmaster.core.designsystem.parseHexColor
import com.budgetmaster.core.util.isReducedMotionEnabled
import com.budgetmaster.reports.domain.model.CategorySlice

/**
 * A donut of each category's share of spending.
 *
 * Drawn with Compose `Canvas` rather than a charting library so one implementation covers
 * Android, iOS, and Wasm identically. [description] is applied to the whole chart because a
 * canvas is opaque to screen readers — the surrounding legend carries the detail.
 */
@Composable
fun CategoryDonut(
    slices: List<CategorySlice>,
    description: String,
    modifier: Modifier = Modifier,
    diameter: Int = 180,
) {
    val reducedMotion = isReducedMotionEnabled()
    val sweep by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(if (reducedMotion) 0 else 700),
        label = "donut",
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val colors = slices.map { parseHexColor(it.colorHex, MaterialTheme.colorScheme.primary) }

    Box(modifier = modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier.size(diameter.dp).semantics { contentDescription = description },
        ) {
            val stroke = Stroke(width = size.minDimension * 0.18f)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)

            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )

            var start = -90f
            slices.forEachIndexed { index, slice ->
                val angle = slice.share * 360f * sweep
                drawArc(
                    color = colors[index],
                    startAngle = start,
                    sweepAngle = angle,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
                start += angle
            }
        }
    }
}
