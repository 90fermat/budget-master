package com.budgetmaster.core.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PinHasherTest {

    @Test
    fun `the correct pin verifies`() {
        val stored = PinHasher.hash("2468")
        assertTrue(PinHasher.verify("2468", stored))
    }

    @Test
    fun `a wrong pin does not verify`() {
        val stored = PinHasher.hash("2468")
        assertFalse(PinHasher.verify("1357", stored))
        assertFalse(PinHasher.verify("24680", stored))
        assertFalse(PinHasher.verify("", stored))
    }

    @Test
    fun `the same pin hashes differently each time`() {
        // Distinct salts, so two users with the same PIN do not share a hash and the stored value
        // reveals nothing about the PIN's popularity.
        assertNotEquals(PinHasher.hash("2468"), PinHasher.hash("2468"))
    }

    @Test
    fun `a malformed record fails to verify rather than throwing`() {
        assertFalse(PinHasher.verify("2468", ""))
        assertFalse(PinHasher.verify("2468", "garbage"))
        assertFalse(PinHasher.verify("2468", "v1:120000:zzzz:zzzz"))
        assertFalse(PinHasher.verify("2468", "v2:120000:00:00"))
    }
}
