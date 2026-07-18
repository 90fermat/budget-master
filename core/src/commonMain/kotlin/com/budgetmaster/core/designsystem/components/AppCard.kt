package com.budgetmaster.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    level: SurfaceLevel = SurfaceLevel.Raised,
    contentPadding: androidx.compose.ui.unit.Dp = when (level) {
        SurfaceLevel.Hero -> Spacing.large
        SurfaceLevel.Raised -> Spacing.medium
        SurfaceLevel.Flat -> Spacing.compact
    },
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = level.shape,
        colors = CardDefaults.cardColors(containerColor = level.containerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = level.elevation),
        border = level.border(),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}
