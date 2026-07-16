@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.budgets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.budgets.domain.model.GoalDraft
import com.budgetmaster.budgets.domain.usecase.ContributeToGoalUseCase
import com.budgetmaster.budgets.domain.usecase.DeleteGoalUseCase
import com.budgetmaster.budgets.domain.usecase.ObserveGoalsUseCase
import com.budgetmaster.budgets.domain.usecase.SaveGoalUseCase
import com.budgetmaster.budgets.domain.usecase.WithdrawFromGoalUseCase
import com.budgetmaster.core.prefs.AppSettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** One year in milliseconds — the default target horizon for a new goal. */
private const val DEFAULT_GOAL_HORIZON_MS = 365L * 24 * 60 * 60 * 1000

/**
 * MVI ViewModel for the Goals screen. Combines the goals stream with the user's
 * currency; tracks editor and contribute-dialog state.
 */
class GoalsViewModel(
    observeGoals: ObserveGoalsUseCase,
    private val saveGoal: SaveGoalUseCase,
    private val contributeToGoal: ContributeToGoalUseCase,
    private val withdrawFromGoal: WithdrawFromGoalUseCase,
    private val deleteGoal: DeleteGoalUseCase,
    settingsRepository: AppSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GoalsState())
    val state: StateFlow<GoalsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GoalsEffect>()
    val effects: SharedFlow<GoalsEffect> = _effects.asSharedFlow()

    init {
        combine(
            observeGoals(),
            settingsRepository.settings.map { it.currency },
        ) { goals, currency -> goals to currency }
            .catch { e -> emitEffect(GoalsEffect.ShowError(e.message ?: "Failed to load goals.")) }
            .onEach { (goals, currency) ->
                _state.update { it.copy(isLoading = false, goals = goals, currencyCode = currency) }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: GoalsIntent) {
        when (intent) {
            is GoalsIntent.AddClicked ->
                _state.update { it.copy(editor = GoalsEditorState(visible = true, editing = null)) }
            is GoalsIntent.EditClicked ->
                _state.update { it.copy(editor = GoalsEditorState(visible = true, editing = intent.item)) }
            is GoalsIntent.EditorDismissed ->
                _state.update { it.copy(editor = GoalsEditorState(visible = false)) }
            is GoalsIntent.SaveGoal -> save(intent)
            is GoalsIntent.DeleteRequested -> delete(intent.id)
            is GoalsIntent.ContributeClicked ->
                _state.update { it.copy(contribute = ContributeState(visible = true, goal = intent.item)) }
            is GoalsIntent.ContributeDismissed ->
                _state.update { it.copy(contribute = ContributeState(visible = false)) }
            is GoalsIntent.SubmitContribution -> contribute(intent)
            is GoalsIntent.WithdrawClicked ->
                _state.update { it.copy(withdraw = WithdrawState(visible = true, goal = intent.item)) }
            is GoalsIntent.WithdrawDismissed ->
                _state.update { it.copy(withdraw = WithdrawState(visible = false)) }
            is GoalsIntent.SubmitWithdrawal -> withdraw(intent)
        }
    }

    private fun save(intent: GoalsIntent.SaveGoal) {
        viewModelScope.launch {
            try {
                saveGoal(
                    GoalDraft(
                        id = intent.editingId,
                        name = intent.name,
                        targetAmount = intent.targetAmount,
                        targetDate = intent.targetDate,
                    )
                )
                _state.update { it.copy(editor = GoalsEditorState(visible = false)) }
            } catch (e: IllegalArgumentException) {
                emitEffect(GoalsEffect.ShowError(e.message ?: "Invalid goal."))
            } catch (e: Exception) {
                emitEffect(GoalsEffect.ShowError(e.message ?: "Failed to save goal."))
            }
        }
    }

    private fun contribute(intent: GoalsIntent.SubmitContribution) {
        viewModelScope.launch {
            try {
                contributeToGoal(intent.id, intent.amount)
                _state.update { it.copy(contribute = ContributeState(visible = false)) }
            } catch (e: IllegalArgumentException) {
                emitEffect(GoalsEffect.ShowError(e.message ?: "Invalid amount."))
            } catch (e: Exception) {
                emitEffect(GoalsEffect.ShowError(e.message ?: "Failed to add funds."))
            }
        }
    }

    private fun withdraw(intent: GoalsIntent.SubmitWithdrawal) {
        viewModelScope.launch {
            try {
                withdrawFromGoal(intent.id, intent.amount)
                _state.update { it.copy(withdraw = WithdrawState(visible = false)) }
            } catch (e: IllegalArgumentException) {
                emitEffect(GoalsEffect.ShowError(e.message ?: "Invalid amount."))
            } catch (e: Exception) {
                emitEffect(GoalsEffect.ShowError(e.message ?: "Failed to withdraw funds."))
            }
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            try {
                deleteGoal(id)
            } catch (e: Exception) {
                emitEffect(GoalsEffect.ShowError(e.message ?: "Failed to delete goal."))
            }
        }
    }

    private fun emitEffect(effect: GoalsEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
