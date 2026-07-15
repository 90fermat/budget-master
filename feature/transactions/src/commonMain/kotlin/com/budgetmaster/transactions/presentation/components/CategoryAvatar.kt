package com.budgetmaster.transactions.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.budgetmaster.transactions.domain.model.TransactionCategory

/**
 * A circular category badge: the category's emoji on a tint of its accent color.
 * Falls back to a neutral coin for uncategorized transactions.
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
        Text(text = category?.icon ?: "💸")
    }
}
