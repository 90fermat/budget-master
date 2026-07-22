package com.budgetmaster.core.sync

import com.budgetmaster.core.prefs.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private class FakeStore(initial: Map<String, String> = emptyMap()) : KeyValueStore {
    val values = MutableStateFlow(initial)
    var writes = 0

    override fun observeString(key: String): Flow<String?> = values.map { it[key] }
    override suspend fun putString(key: String, value: String) {
        writes++
        values.value = values.value + (key to value)
    }
    override suspend fun remove(key: String) {
        values.value = values.value - key
    }
}

class DeviceIdProviderTest {

    @Test
    fun `mints an id on first use and keeps it`() = runTest {
        val store = FakeStore()
        val provider = DeviceIdProvider(store)

        val first = provider.deviceId()
        val second = provider.deviceId()

        assertTrue(first.isNotBlank())
        assertEquals(first, second)
        assertEquals(1, store.writes, "the id must be minted once, not on every call")
    }

    @Test
    fun `survives a restart`() = runTest {
        val store = FakeStore()
        val id = DeviceIdProvider(store).deviceId()

        // A fresh provider over the same storage is what the next app launch looks like. If the id
        // changed here, every launch would look like a new device and the tiebreak that keeps two
        // devices agreeing would be comparing against a stranger each time.
        assertEquals(id, DeviceIdProvider(store).deviceId())
    }

    @Test
    fun `two installs do not share an id`() = runTest {
        assertNotEquals(DeviceIdProvider(FakeStore()).deviceId(), DeviceIdProvider(FakeStore()).deviceId())
    }

    @Test
    fun `an id already stored is used as is`() = runTest {
        val store = FakeStore(mapOf("sync_device_id" to "existing-device"))

        assertEquals("existing-device", DeviceIdProvider(store).deviceId())
        assertEquals(0, store.writes, "an existing id must not be overwritten")
    }
}
