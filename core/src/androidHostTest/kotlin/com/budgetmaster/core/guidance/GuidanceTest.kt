package com.budgetmaster.core.guidance

import com.budgetmaster.core.prefs.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class InMemoryKeyValueStore : KeyValueStore {
    private val entries = MutableStateFlow<Map<String, String>>(emptyMap())
    override fun observeString(key: String): Flow<String?> = entries.map { it[key] }
    override suspend fun putString(key: String, value: String) {
        entries.value = entries.value + (key to value)
    }
    override suspend fun remove(key: String) {
        entries.value = entries.value - key
    }
}

class GuidanceRegistryTest {

    @Test
    fun everyGuidedScreenHasAGuide() {
        // A new GuidanceKey without a guide would ship a "?" that opens nothing. Fail here
        // instead, on the one screen nobody thinks to open.
        GuidanceKey.entries.forEach { key ->
            val guide = GuidanceRegistry.guides[key]
            assertTrue(guide != null, "No guide registered for $key")
        }
        assertEquals(GuidanceKey.entries.size, GuidanceRegistry.guides.size)
    }

    @Test
    fun everyGuideExplainsAtLeastOneFeature() {
        // A guide with no notes is a sheet that says nothing.
        GuidanceRegistry.guides.forEach { (key, guide) ->
            assertTrue(guide.notes.isNotEmpty(), "$key has no feature notes")
            assertEquals(key, guide.key, "$key is registered under the wrong key")
        }
    }
}

class GuidancePreferencesTest {

    @Test
    fun tipsAreOnUntilTurnedOff() = runTest {
        val preferences = GuidancePreferences(InMemoryKeyValueStore())
        // Default on: the guides only help if they appear without being sought out.
        assertTrue(preferences.tipsEnabled.first())

        preferences.setTipsEnabled(false)
        assertFalse(preferences.tipsEnabled.first())

        preferences.setTipsEnabled(true)
        assertTrue(preferences.tipsEnabled.first())
    }

    @Test
    fun aGuideIsUnseenUntilShownThenStaysSeen() = runTest {
        val preferences = GuidancePreferences(InMemoryKeyValueStore())
        assertFalse(preferences.hasSeen(GuidanceKey.BUDGETS).first())

        preferences.markSeen(GuidanceKey.BUDGETS)
        assertTrue(preferences.hasSeen(GuidanceKey.BUDGETS).first())

        // Marking one screen must not silence the others.
        assertFalse(preferences.hasSeen(GuidanceKey.GOALS).first())
    }

    @Test
    fun resetMakesEveryScreenExplainItselfAgain() = runTest {
        val preferences = GuidancePreferences(InMemoryKeyValueStore())
        GuidanceKey.entries.forEach { preferences.markSeen(it) }
        GuidanceKey.entries.forEach { assertTrue(preferences.hasSeen(it).first()) }

        preferences.resetAll()

        GuidanceKey.entries.forEach { assertFalse(preferences.hasSeen(it).first(), "$it still seen") }
    }

    @Test
    fun resetLeavesTheTipsToggleAlone() = runTest {
        // Someone who turned tips off and then reset should not be nagged again.
        val preferences = GuidancePreferences(InMemoryKeyValueStore())
        preferences.setTipsEnabled(false)

        preferences.resetAll()

        assertFalse(preferences.tipsEnabled.first())
    }
}
