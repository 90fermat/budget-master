package com.budgetmaster.transactions.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.budgetmaster.core.designsystem.categoryIconFor
import com.budgetmaster.transactions.domain.model.TransactionCategory

/**
 * A circular category badge: the category's vector icon on a tint of its accent color.
 *
 * Uses a Material vector rather than the stored emoji so it renders identically on Android,
 * iOS, and Web (Wasm has no color-emoji font and would draw tofu boxes).
 */
@Composable
internal fun CategoryAvatar(
    category: TransactionCategory?,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    val accent = category?.let { parseHexColor(it.colorHex, MaterialTheme.colorScheme.primary) }
        ?: MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIconFor(category?.id),
            contentDescription = category?.name,
            tint = accent,
            modifier = Modifier.size((size * 0.5).dp),
        )
    }
}
