@file:OptIn(ExperimentalCoroutinesApi::class)

package com.budgetmaster.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.reports.domain.model.ReportRange
import com.budgetmaster.reports.domain.usecase.ExportReportCsvUseCase
import com.budgetmaster.reports.domain.usecase.ObserveReportUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ViewModel for Reports. The selected range drives a `flatMapLatest` re-subscription, so
 * switching period re-queries; the report itself also re-emits when the active wallet or
 * currency changes.
 */
class ReportsViewModel(
    observeReport: ObserveReportUseCase,
    private val exportCsv: ExportReportCsvUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ReportsEffect>()
    val effects: SharedFlow<ReportsEffect> = _effects.asSharedFlow()

    private val range = MutableStateFlow(ReportRange.MONTH)

    init {
        range
            .flatMapLatest { observeReport(it) }
            .catch { e -> emitEffect(ReportsEffect.ShowError(e.message ?: "Failed to load report.")) }
            .onEach { report -> _state.update { it.copy(isLoading = false, report = report) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: ReportsIntent) {
        when (intent) {
            is ReportsIntent.RangeChanged -> {
                _state.update { it.copy(range = intent.range, isLoading = true) }
                range.value = intent.range
            }
            ReportsIntent.ExportCsvClicked -> export()
        }
    }

    private fun export() {
        if (_state.value.isExporting) return
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                val started = exportCsv(range.value)
                emitEffect(if (started) ReportsEffect.ExportStarted else ReportsEffect.ExportUnavailable)
            } catch (e: Exception) {
                emitEffect(ReportsEffect.ShowError(e.message ?: "Export failed."))
            } finally {
                _state.update { it.copy(isExporting = false) }
            }
        }
    }

    private fun emitEffect(effect: ReportsEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
