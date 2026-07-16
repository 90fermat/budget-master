package com.budgetmaster.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps a category to a Material vector icon.
 *
 * Categories store an emoji in the database, but emoji render as tofu boxes on Wasm (the web
 * build has no color-emoji font). Drawing a vector instead is consistent on every platform
 * and costs nothing in bundle size. The stored emoji is kept as data — it stays useful for
 * AI prompts and as a label — but the UI draws these icons.
 *
 * Falls back to a generic icon for user-created categories and unknown ids.
 *
 * @param categoryId A seeded id from `DefaultData.categories`, or any user-defined id.
 */
fun categoryIconFor(categoryId: String?): ImageVector = when (categoryId) {
    "cat_food" -> Icons.Filled.Fastfood
    "cat_groceries" -> Icons.Filled.ShoppingCart
    "cat_housing" -> Icons.Filled.Home
    "cat_transport" -> Icons.Filled.DirectionsCar
    "cat_shopping" -> Icons.Filled.ShoppingBag
    "cat_travel" -> Icons.Filled.Flight
    "cat_entertainment" -> Icons.Filled.LocalActivity
    "cat_health" -> Icons.Filled.MedicalServices
    "cat_salary" -> Icons.Filled.Savings
    "cat_other" -> Icons.Filled.Category
    else -> Icons.Filled.Payments
}
