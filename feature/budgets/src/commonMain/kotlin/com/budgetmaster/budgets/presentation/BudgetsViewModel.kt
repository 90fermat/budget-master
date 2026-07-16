@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.budgets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.budgets.domain.model.BudgetDraft
import com.budgetmaster.budgets.domain.usecase.DeleteBudgetUseCase
import com.budgetmaster.budgets.domain.usecase.NotifyBudgetThresholdsUseCase
import com.budgetmaster.budgets.domain.usecase.ObserveBudgetCategoriesUseCase
import com.budgetmaster.budgets.domain.usecase.ObserveBudgetsUseCase
import com.budgetmaster.budgets.domain.usecase.SaveBudgetUseCase
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.util.DateUtils
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
import kotlin.time.ExperimentalTime

/**
 * MVI ViewModel for the Budgets screen. Combines the live budgets with the user's
 * currency preference; category list and editor state are tracked separately.
 */
class BudgetsViewModel(
    observeBudgets: ObserveBudgetsUseCase,
    observeCategories: ObserveBudgetCategoriesUseCase,
    private val saveBudget: SaveBudgetUseCase,
    private val deleteBudget: DeleteBudgetUseCase,
    private val notifyBudgetThresholds: NotifyBudgetThresholdsUseCase,
    settingsRepository: AppSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetsState())
    val state: StateFlow<BudgetsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<BudgetsEffect>()
    val effects: SharedFlow<BudgetsEffect> = _effects.asSharedFlow()

    init {
        combine(
            observeBudgets(),
            settingsRepository.settings.map { it.currency },
        ) { budgets, currency -> budgets to currency }
            .catch { e -> emitEffect(BudgetsEffect.ShowError(e.message ?: "Failed to load budgets.")) }
            .onEach { (budgets, currency) ->
                _state.update { it.copy(isLoading = false, budgets = budgets, currencyCode = currency) }
                // Idempotent: each (budget, period, threshold) alert is written at most once.
                notifyBudgetThresholds(budgets)
            }
            .launchIn(viewModelScope)

        observeCategories()
            .onEach { categories -> _state.update { it.copy(categories = categories) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: BudgetsIntent) {
        when (intent) {
            is BudgetsIntent.AddClicked ->
                _state.update { it.copy(editor = BudgetsEditorState(visible = true, editing = null)) }
            is BudgetsIntent.EditClicked ->
                _state.update { it.copy(editor = BudgetsEditorState(visible = true, editing = intent.item)) }
            is BudgetsIntent.EditorDismissed ->
                _state.update { it.copy(editor = BudgetsEditorState(visible = false)) }
            is BudgetsIntent.SaveBudget -> save(intent)
            is BudgetsIntent.DeleteRequested -> delete(intent.id)
        }
    }

    private fun save(intent: BudgetsIntent.SaveBudget) {
        val period = DateUtils.currentMonthRange()
        viewModelScope.launch {
            try {
                saveBudget(
                    BudgetDraft(
                        id = intent.editingId,
                        categoryId = intent.categoryId,
                        limit = intent.limit,
                        periodStart = period.first,
                        periodEnd = period.last,
                    )
                )
                _state.update { it.copy(editor = BudgetsEditorState(visible = false)) }
            } catch (e: IllegalArgumentException) {
                emitEffect(BudgetsEffect.ShowError(e.message ?: "Invalid budget."))
            } catch (e: Exception) {
                emitEffect(BudgetsEffect.ShowError(e.message ?: "Failed to save budget."))
            }
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            try {
                deleteBudget(id)
            } catch (e: Exception) {
                emitEffect(BudgetsEffect.ShowError(e.message ?: "Failed to delete budget."))
            }
        }
    }

    private fun emitEffect(effect: BudgetsEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
