package com.budgetmaster.budgets.domain.model

/**
 * A savings goal with its current progress.
 *
 * @property id Goal row id.
 * @property name Display name.
 * @property targetAmount The amount to reach.
 * @property currentAmount Amount saved so far.
 * @property targetDate Epoch-ms target date.
 * @property createdAt Epoch-ms creation time.
 */
data class GoalItem(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Long,
    val createdAt: Long,
) {
    val progress: Float get() = if (targetAmount > 0) (currentAmount / targetAmount).coerceIn(0.0, 1.0).toFloat() else 0f
    val remaining: Double get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
    val isCompleted: Boolean get() = currentAmount >= targetAmount && targetAmount > 0
}

/**
 * Input for creating or editing a goal (contributions are applied separately).
 *
 * @property id Existing id when editing; null to create.
 * @property name Goal name.
 * @property targetAmount Target (> 0).
 * @property targetDate Epoch-ms target date.
 */
data class GoalDraft(
    val id: String? = null,
    val name: String,
    val targetAmount: Double,
    val targetDate: Long,
)
