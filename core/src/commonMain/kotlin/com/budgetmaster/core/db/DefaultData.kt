package com.budgetmaster.core.db

/**
 * The single source of first-launch seed data shared by every feature (transactions,
 * budgets, goals, dashboard). IDs are stable so re-seeding is idempotent via
 * `INSERT OR REPLACE`.
 */
object DefaultData {
    /**
     * System user that owns the shared default categories (visible to every account via
     * `isDefault = 1`). Also the fallback owner when nobody is signed in (tests, local mode
     * before the session is bound).
     */
    const val DEFAULT_USER_ID = "default_user"
    const val DEFAULT_ACCOUNT_ID = "default_account"
    const val DEFAULT_CURRENCY = "USD"

    /** Deterministic id of the first "Cash" wallet seeded for a given user. */
    fun firstAccountId(userId: String): String = "${userId}_cash"

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
        // Mobile-money transfer fees are a real, recurring cost in XAF/NGN markets — large
        // enough that folding them into the transfer would hide a meaningful spend line.
        SeedCategory("cat_fees", "Fees & Charges", "💸", "#F97316"),
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
