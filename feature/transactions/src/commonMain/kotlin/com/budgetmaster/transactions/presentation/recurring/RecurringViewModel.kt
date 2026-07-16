package com.budgetmaster.transactions.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.transactions.domain.usecase.DeleteRecurringUseCase
import com.budgetmaster.transactions.domain.usecase.MaterializeDueRecurringUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveCategoriesUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveRecurringUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveTransactionAccountsUseCase
import com.budgetmaster.transactions.domain.usecase.SaveRecurringUseCase
import com.budgetmaster.transactions.domain.usecase.SetRecurringActiveUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ViewModel for managing recurring schedules.
 */
class RecurringViewModel(
    observeRecurring: ObserveRecurringUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observeAccounts: ObserveTransactionAccountsUseCase,
    settingsRepository: AppSettingsRepository,
    private val saveRecurring: SaveRecurringUseCase,
    private val setRecurringActive: SetRecurringActiveUseCase,
    private val deleteRecurring: DeleteRecurringUseCase,
    private val materializeDue: MaterializeDueRecurringUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RecurringState())
    val state: StateFlow<RecurringState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<RecurringEffect>()
    val effects: SharedFlow<RecurringEffect> = _effects.asSharedFlow()

    init {
        observeRecurring()
            .onEach { schedules -> _state.update { it.copy(isLoading = false, schedules = schedules) } }
            .launchIn(viewModelScope)
        observeCategories()
            .onEach { categories -> _state.update { it.copy(categories = categories) } }
            .launchIn(viewModelScope)
        observeAccounts()
            .onEach { accounts -> _state.update { it.copy(accounts = accounts) } }
            .launchIn(viewModelScope)
        settingsRepository.settings
            .map { it.currency }
            .onEach { currency -> _state.update { it.copy(currencyCode = currency) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: RecurringIntent) {
        when (intent) {
            RecurringIntent.AddClicked ->
                _state.update { it.copy(editor = RecurringEditorState(visible = true, editing = null)) }
            is RecurringIntent.EditClicked ->
                _state.update { it.copy(editor = RecurringEditorState(visible = true, editing = intent.item)) }
            RecurringIntent.EditorDismissed ->
                _state.update { it.copy(editor = RecurringEditorState(visible = false)) }
            is RecurringIntent.Save -> viewModelScope.launch {
                try {
                    saveRecurring(intent.draft)
                    _state.update { it.copy(editor = RecurringEditorState(visible = false)) }
                    // A schedule starting today (or in the past) should produce its entries
                    // immediately rather than waiting for the next app launch.
                    materializeDue()
                } catch (e: IllegalArgumentException) {
                    emitEffect(RecurringEffect.ShowError(e.message ?: "Invalid schedule."))
                } catch (e: Exception) {
                    emitEffect(RecurringEffect.ShowError(e.message ?: "Failed to save."))
                }
            }
            is RecurringIntent.SetActive -> viewModelScope.launch {
                setRecurringActive(intent.id, intent.active)
                if (intent.active) materializeDue()
            }
            is RecurringIntent.Delete -> viewModelScope.launch { deleteRecurring(intent.id) }
        }
    }

    private fun emitEffect(effect: RecurringEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
