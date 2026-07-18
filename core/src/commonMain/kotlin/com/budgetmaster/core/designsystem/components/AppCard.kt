package com.budgetmaster.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.SurfaceLevel
import com.budgetmaster.core.designsystem.border
import com.budgetmaster.core.designsystem.containerColor
import com.budgetmaster.core.designsystem.elevation
import com.budgetmaster.core.designsystem.shape

/**
 * A card at a given [SurfaceLevel].
 *
 * Screens previously each spelled out their own container colour, radius, border and padding,
 * which is why everything ended up looking equally important. Routing them through one component
 * makes the hierarchy a decision ("is this the hero?") rather than an accident of which values
 * got copied.
 *
 * @param level exactly one [SurfaceLevel.Hero] per screen.
 * @param contentPadding defaults to the level's natural inset; a hero gets more room to breathe.
 *   Pass `0.dp` when the content has to reach the card edge, such as a full-bleed indicator bar.
 * @param onClick when the whole card is tappable. Prefer this over a `.clickable` on [modifier]:
 *   Material's clickable overload keeps the ripple inside the card's rounded corners, whereas a
 *   clickable applied outside the shape spills a rectangular ripple past them.
 * @param verticalArrangement spacing between children, so callers do not have to nest a second
 *   Column inside the one this already provides.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    level: SurfaceLevel = SurfaceLevel.Raised,
    contentPadding: Dp = when (level) {
        SurfaceLevel.Hero -> Spacing.large
        SurfaceLevel.Raised -> Spacing.medium
        SurfaceLevel.Flat -> Spacing.compact
    },
    onClick: (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = level.containerColor())
    val elevation = CardDefaults.cardElevation(defaultElevation = level.elevation)
    val border = level.border()

    // Two branches rather than one, because Material's clickable Card is a separate overload —
    // there is no "maybe clickable" Card, and faking it with Modifier.clickable is exactly the
    // ripple-clipping bug this component exists to stop callers from writing.
    if (onClick == null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = level.shape,
            colors = colors,
            elevation = elevation,
            border = border,
        ) {
            Column(
                Modifier.padding(contentPadding),
                verticalArrangement = verticalArrangement,
                content = content,
            )
        }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = level.shape,
            colors = colors,
            elevation = elevation,
            border = border,
        ) {
            Column(
                Modifier.padding(contentPadding),
                verticalArrangement = verticalArrangement,
                content = content,
            )
        }
    }
}
