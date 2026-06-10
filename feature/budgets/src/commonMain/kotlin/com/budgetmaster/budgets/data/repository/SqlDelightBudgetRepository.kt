package com.budgetmaster.budgets.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.budgets.domain.repository.BudgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of [BudgetRepository] using SQLDelight for persistence.
 */
class SqlDelightBudgetRepository(
    private val databaseProvider: DatabaseProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : BudgetRepository {

    override fun getTransactions(): Flow<List<Transaction>> {
        return flow {
            val db = databaseProvider.getDatabase()
            val queries = db.budgetMasterDatabaseQueries
            emitAll(
                queries.selectAllTransactions()
                    .asFlow()
                    .mapToList(dispatcher)
                    .map { entities ->
                        entities.map { entity ->
                            Transaction(
                                id = entity.id,
                                amount = entity.amount,
                                category = entity.categoryId ?: "Uncategorized",
                                description = entity.description,
                                timestamp = entity.timestamp
                            )
                        }
                    }
            )
        }
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        val db = databaseProvider.getDatabase()
        val queries = db.budgetMasterDatabaseQueries
        
        val defaultUserId = "default_user"
        val defaultAccountId = "default_account"
        
        // Ensure default user exists to satisfy foreign key constraints
        queries.insertUser(
            id = defaultUserId,
            name = "Default User",
            email = "user@example.com",
            currency = "USD",
            createdAt = 0L
        )
        
        // Ensure default account exists to satisfy foreign key constraints
        queries.insertAccount(
            id = defaultAccountId,
            userId = defaultUserId,
            name = "Default Account",
            type = "CHECKING",
            balance = 0.0,
            currency = "USD",
            createdAt = 0L
        )
        
        // Ensure default category exists to satisfy foreign key constraints
        val categoryId = transaction.category
        queries.insertCategory(
            id = categoryId,
            userId = defaultUserId,
            name = categoryId,
            icon = "category_icon",
            color = "#6366F1",
            isDefault = 1L
        )

        queries.insertTransaction(
            id = transaction.id,
            accountId = defaultAccountId,
            categoryId = categoryId,
            amount = transaction.amount,
            description = transaction.description,
            timestamp = transaction.timestamp,
            notes = null,
            tags = null,
            isRecurring = 0L
        )
    }

    override suspend fun deleteTransaction(id: String) {
        val db = databaseProvider.getDatabase()
        db.budgetMasterDatabaseQueries.deleteTransaction(id)
    }
}
