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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.category_entertainment
import budgetmaster.core.generated.resources.category_fees
import budgetmaster.core.generated.resources.category_food
import budgetmaster.core.generated.resources.category_groceries
import budgetmaster.core.generated.resources.category_health
import budgetmaster.core.generated.resources.category_housing
import budgetmaster.core.generated.resources.category_other
import budgetmaster.core.generated.resources.category_salary
import budgetmaster.core.generated.resources.category_shopping
import budgetmaster.core.generated.resources.category_transport
import budgetmaster.core.generated.resources.category_travel
import com.budgetmaster.core.db.DefaultData
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

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
    "cat_fees" -> Icons.Filled.Payments
    "cat_other" -> Icons.Filled.Category
    else -> Icons.Filled.Payments
}

/**
 * The accent color for a category, resolved from the seeded palette in `DefaultData`.
 *
 * Reads the same hex the database was seeded with rather than restating a palette in UI code,
 * so the icon tint always agrees with the stored category color. User-created categories (and
 * unknown ids) fall back to the theme primary.
 *
 * @param categoryId A seeded id from `DefaultData.categories`, or any user-defined id.
 */
@Composable
fun categoryAccentFor(categoryId: String?): Color {
    val fallback = MaterialTheme.colorScheme.primary
    val hex = DefaultData.categories.firstOrNull { it.id == categoryId }?.colorHex ?: return fallback
    return parseHexColor(hex, fallback)
}

/**
 * The display name for a category, localized for the built-in ones.
 *
 * `DefaultData` seeds English names into the database, so a stored name is only translated for
 * categories the app itself created. Resolving seeded ids through string resources keeps them in
 * the app's language; anything the user named is theirs, so [storedName] is returned untouched.
 *
 * @param categoryId A seeded id from `DefaultData.categories`, or any user-defined id.
 * @param storedName The name held in the database, used for user-created categories.
 */
@Composable
fun categoryNameFor(categoryId: String?, storedName: String): String =
    categoryNameRes(categoryId)?.let { stringResource(it) } ?: storedName

/**
 * The string resource for a seeded category id, or null for a user-created one.
 *
 * The non-composable half of [categoryNameFor], so code outside a Compose scope — a notification
 * written in the domain layer, a report built in the data layer — can localise a category name
 * too, by resolving this with the suspend `getString`. Default categories are stored in the
 * database as English literals, so without this they would surface in English whatever the app
 * language is.
 */
fun categoryNameRes(categoryId: String?): StringResource? = when (categoryId) {
    "cat_food" -> Res.string.category_food
    "cat_groceries" -> Res.string.category_groceries
    "cat_housing" -> Res.string.category_housing
    "cat_transport" -> Res.string.category_transport
    "cat_shopping" -> Res.string.category_shopping
    "cat_travel" -> Res.string.category_travel
    "cat_entertainment" -> Res.string.category_entertainment
    "cat_health" -> Res.string.category_health
    "cat_salary" -> Res.string.category_salary
    "cat_fees" -> Res.string.category_fees
    "cat_other" -> Res.string.category_other
    else -> null
}
