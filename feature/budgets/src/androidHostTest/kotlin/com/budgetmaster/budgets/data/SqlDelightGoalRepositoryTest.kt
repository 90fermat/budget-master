@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.budgets.data

import com.budgetmaster.budgets.TestDatabaseHelper
import com.budgetmaster.budgets.data.repository.SqlDelightGoalRepository
import com.budgetmaster.budgets.domain.model.GoalDraft
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.session.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlDelightGoalRepositoryTest {

    private fun repository(): SqlDelightGoalRepository {
        val provider = TestDatabaseHelper.createProvider()
        return SqlDelightGoalRepository(provider, AppDataSeeder(provider), SessionStore())
    }

    @Test
    fun createContributeAndComplete() = runTest {
        val repo = repository()
        val target = Clock.System.now().toEpochMilliseconds() + 1_000_000
        repo.upsertGoal(GoalDraft(id = "g1", name = "Emergency Fund", targetAmount = 1000.0, targetDate = target))

        var goal = repo.observeGoals().first().first()
        assertEquals("Emergency Fund", goal.name)
        assertEquals(0.0, goal.currentAmount)
        assertTrue(!goal.isCompleted)

        repo.contribute("g1", 300.0)
        goal = repo.observeGoals().first().first()
        assertEquals(300.0, goal.currentAmount)
        assertEquals(0.3f, goal.progress)

        repo.contribute("g1", 800.0)
        goal = repo.observeGoals().first().first()
        assertEquals(1100.0, goal.currentAmount)
        assertTrue(goal.isCompleted)
    }

    @Test
    fun editPreservesSavedAmount() = runTest {
        val repo = repository()
        val target = Clock.System.now().toEpochMilliseconds() + 1_000_000
        repo.upsertGoal(GoalDraft(id = "g1", name = "Trip", targetAmount = 500.0, targetDate = target))
        repo.contribute("g1", 200.0)

        // Edit name + target; saved amount must survive.
        repo.upsertGoal(GoalDraft(id = "g1", name = "Big Trip", targetAmount = 800.0, targetDate = target))
        val goal = repo.observeGoals().first().first()
        assertEquals("Big Trip", goal.name)
        assertEquals(800.0, goal.targetAmount)
        assertEquals(200.0, goal.currentAmount)
    }

    @Test
    fun deleteRemovesGoal() = runTest {
        val repo = repository()
        val target = Clock.System.now().toEpochMilliseconds() + 1_000_000
        repo.upsertGoal(GoalDraft(id = "g1", name = "Car", targetAmount = 5000.0, targetDate = target))
        assertEquals(1, repo.observeGoals().first().size)

        repo.deleteGoal("g1")
        assertEquals(0, repo.observeGoals().first().size)
    }
}
