package com.budgetmaster.budgets.domain.usecase

import com.budgetmaster.budgets.domain.model.GoalDraft
import com.budgetmaster.budgets.domain.model.GoalItem
import com.budgetmaster.budgets.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow

/** Observes all savings goals. */
class ObserveGoalsUseCase(private val repository: GoalRepository) {
    operator fun invoke(): Flow<List<GoalItem>> = repository.observeGoals()
}

/** Validates and saves a goal. */
class SaveGoalUseCase(private val repository: GoalRepository) {
    /** @throws IllegalArgumentException if the name is blank or the target is not positive. */
    suspend operator fun invoke(draft: GoalDraft) {
        require(draft.name.isNotBlank()) { "Goal name must not be blank." }
        require(draft.targetAmount > 0.0) { "Target amount must be greater than zero." }
        repository.upsertGoal(draft)
    }
}

/** Adds funds to a goal. */
class ContributeToGoalUseCase(private val repository: GoalRepository) {
    /** @throws IllegalArgumentException if [amount] is not positive. */
    suspend operator fun invoke(id: String, amount: Double) {
        require(amount > 0.0) { "Contribution must be greater than zero." }
        repository.contribute(id, amount)
    }
}

/** Deletes a goal by id. */
class DeleteGoalUseCase(private val repository: GoalRepository) {
    suspend operator fun invoke(id: String) = repository.deleteGoal(id)
}
