package com.budgetmaster.budgets.domain.repository

import com.budgetmaster.budgets.domain.model.GoalDraft
import com.budgetmaster.budgets.domain.model.GoalItem
import kotlinx.coroutines.flow.Flow

/** Data source for savings goals. */
interface GoalRepository {
    /** Observes all savings goals (soonest target first). */
    fun observeGoals(): Flow<List<GoalItem>>

    /** Inserts or updates a goal from [draft], preserving the saved amount on edit. */
    suspend fun upsertGoal(draft: GoalDraft)

    /** Adds [amount] to the goal's saved balance. */
    suspend fun contribute(id: String, amount: Double)

    /** Deletes the goal with [id]. */
    suspend fun deleteGoal(id: String)
}
