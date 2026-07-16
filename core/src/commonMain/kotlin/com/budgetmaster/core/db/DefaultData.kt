package com.budgetmaster.core.db

/**
 * The single source of first-launch seed data shared by every feature (transactions,
 * budgets, goals, dashboard). IDs are stable so re-seeding is idempotent via
 * `INSERT OR REPLACE`.
 */
object DefaultData {
    const val DEFAULT_USER_ID = "default_user"
    const val DEFAULT_ACCOUNT_ID = "default_account"
    const val DEFAULT_CURRENCY = "USD"

    /** Default categories (id, display name, emoji, hex color). */
    val categories: List<SeedCategory> = listOf(
        SeedCategory("cat_food", "Food & Dining", "🍔", "#F59E0B"),
        SeedCategory("cat_groceries", "Groceries", "🛒", "#10B981"),
        SeedCategory("cat_housing", "Housing & Bills", "🏠", "#3B82F6"),
        SeedCategory("cat_transport", "Transport", "🚗", "#6366F1"),
        SeedCategory("cat_shopping", "Shopping", "🛍️", "#EC4899"),
        SeedCategory("cat_travel", "Travel", "✈️", "#14B8A6"),
        SeedCategory("cat_entertainment", "Entertainment", "🎬", "#8B5CF6"),
        SeedCategory("cat_health", "Health", "💊", "#EF4444"),
        SeedCategory("cat_salary", "Salary", "💰", "#059669"),
        SeedCategory("cat_other", "Other", "📦", "#94A3B8"),
    )
}

/** A default category to seed. */
data class SeedCategory(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
)
