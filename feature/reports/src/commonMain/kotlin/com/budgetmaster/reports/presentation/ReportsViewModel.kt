@file:OptIn(ExperimentalCoroutinesApi::class)

package com.budgetmaster.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.text.intl.Locale
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.reports.domain.model.ReportRange
import com.budgetmaster.reports.domain.usecase.AnswerFinanceQuestionUseCase
import com.budgetmaster.reports.domain.usecase.ExportReportCsvUseCase
import com.budgetmaster.reports.domain.usecase.GenerateNarrativeUseCase
import com.budgetmaster.reports.domain.usecase.ObserveReportUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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
    private val settingsRepository: AppSettingsRepository,
    private val generateNarrative: GenerateNarrativeUseCase,
    private val answerQuestion: AnswerFinanceQuestionUseCase,
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
            .onEach { report ->
                // A fresh report invalidates last period's AI text, so clear it rather than leave
                // a stale narrative under new numbers.
                _state.update { it.copy(isLoading = false, report = report, narrative = AiText.Idle, answer = AiText.Idle) }
            }
            .launchIn(viewModelScope)

        settingsRepository.settings
            .onEach { settings ->
                _state.update { it.copy(aiEnabled = generateNarrative.isAvailable && settings.aiEnabled) }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: ReportsIntent) {
        when (intent) {
            is ReportsIntent.RangeChanged -> {
                _state.update { it.copy(range = intent.range, isLoading = true) }
                range.value = intent.range
            }
            ReportsIntent.ExportCsvClicked -> export()
            ReportsIntent.GenerateNarrative -> runNarrative()
            is ReportsIntent.AskQuestion -> runAnswer(intent.question)
        }
    }

    /** BCP-47 tag for the app's language, falling back to the platform locale for "System". */
    private suspend fun languageTag(): String =
        settingsRepository.settings.first().language.tag ?: Locale.current.language

    private fun runNarrative() {
        val report = _state.value.report ?: return
        if (_state.value.narrative is AiText.Loading || !_state.value.aiEnabled) return
        _state.update { it.copy(narrative = AiText.Loading) }
        viewModelScope.launch {
            val result = generateNarrative(report, languageTag())
            _state.update { it.copy(narrative = result.toAiText()) }
        }
    }

    private fun runAnswer(question: String) {
        val report = _state.value.report ?: return
        if (question.isBlank() || _state.value.answer is AiText.Loading || !_state.value.aiEnabled) return
        _state.update { it.copy(answer = AiText.Loading) }
        viewModelScope.launch {
            val result = answerQuestion(question, report, languageTag())
            _state.update { it.copy(answer = result.toAiText()) }
        }
    }

    /** Maps a use-case [Result] to the UI's [AiText]; a rate limit reads differently from a hard fail. */
    private fun Result<String>.toAiText(): AiText = fold(
        onSuccess = { if (it.isBlank()) AiText.Idle else AiText.Ready(it) },
        onFailure = {
            AiText.Failed(
                when (it) {
                    is GenAiException.RateLimited -> "rate_limited"
                    // Distinct because retrying cannot fix it, and the old generic copy invited
                    // exactly the retry that kept failing.
                    is GenAiException.NotAuthorized -> "not_authorized"
                    else -> "failed"
                },
            )
        },
    )

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
