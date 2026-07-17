@file:OptIn(ExperimentalCoroutinesApi::class)

package com.budgetmaster.transactions.presentation

import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.session.ActiveAccountStore
import com.budgetmaster.transactions.InMemoryKeyValueStore
import com.budgetmaster.transactions.domain.model.TransactionAccount
import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.model.TransactionDraft
import com.budgetmaster.transactions.domain.model.TransactionItem
import com.budgetmaster.transactions.domain.repository.TransactionRepository
import com.budgetmaster.transactions.domain.usecase.DeleteTransactionUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveCategoriesUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveTransactionAccountsUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveTransactionsUseCase
import com.budgetmaster.transactions.domain.usecase.RestoreTransactionUseCase
import com.budgetmaster.transactions.domain.usecase.DetectRecurringChargesUseCase
import com.budgetmaster.transactions.domain.usecase.ParseQuickEntryUseCase
import com.budgetmaster.transactions.domain.usecase.SuggestCategoryUseCase
import com.budgetmaster.transactions.domain.usecase.SaveTransactionUseCase
import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val food = TransactionCategory("cat_food", "Food & Dining", "🍔", "#F59E0B")

    private val transactions = MutableStateFlow(
        listOf(
            TransactionItem("1", -12.5, "Coffee", 1_735_000_000_000, food, null),
            TransactionItem("2", -8.0, "Tea", 1_735_000_100_000, food, null),
        )
    )

    private val repository = object : TransactionRepository {
        override fun observeTransactions(limit: Long): Flow<List<TransactionItem>> = transactions
        override fun observeCategories() = flowOf(listOf(food))
        override fun observeAccounts() = flowOf(listOf(TransactionAccount("acc1", "Cash", "USD")))
        var deleted: String? = null
        var restored: TransactionItem? = null
        override suspend fun upsertTransaction(draft: TransactionDraft) =
            TransactionItem("new", -1.0, draft.description, draft.timestamp, food, null)
        override suspend fun deleteTransaction(id: String) { deleted = id }
        override suspend fun restoreTransaction(item: TransactionItem) { restored = item }
    }

    private fun viewModel() = TransactionsViewModel(
        observeTransactions = ObserveTransactionsUseCase(repository),
        observeCategories = ObserveCategoriesUseCase(repository),
        observeAccounts = ObserveTransactionAccountsUseCase(repository),
        settingsRepository = AppSettingsRepository(InMemoryKeyValueStore()),
        activeAccountStore = ActiveAccountStore(InMemoryKeyValueStore()),
        saveTransaction = SaveTransactionUseCase(repository),
        deleteTransaction = DeleteTransactionUseCase(repository),
        restoreTransaction = RestoreTransactionUseCase(repository),
        // No AI provider in unit tests; quick-add stays disabled, which these tests don't exercise.
        parseQuickEntry = ParseQuickEntryUseCase(
            object : GenAiClient {
                override val isAvailable = false
                override suspend fun generateJson(prompt: String, schema: GenAiSchema) =
                    throw GenAiException.Unavailable()
            },
        ),
        detectRecurringCharges = DetectRecurringChargesUseCase(),
        suggestCategory = SuggestCategoryUseCase(
            object : GenAiClient {
                override val isAvailable = false
                override suspend fun generateJson(prompt: String, schema: GenAiSchema) =
                    throw GenAiException.Unavailable()
            },
            InMemoryKeyValueStore(),
        ),
    )

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun groupsTransactionsByDayAndClearsLoading() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(!state.isLoading)
        assertEquals(1, state.groups.size)
        assertEquals(2, state.groups.first().items.size)
        assertEquals(1, state.categories.size)
    }

    @Test
    fun deleteThenUndoDelegatesToRepository() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(TransactionsIntent.DeleteRequested("1"))
        advanceUntilIdle()
        assertEquals("1", repository.deleted)

        vm.onIntent(TransactionsIntent.UndoDelete)
        advanceUntilIdle()
        assertEquals("1", repository.restored?.id)
    }
}
