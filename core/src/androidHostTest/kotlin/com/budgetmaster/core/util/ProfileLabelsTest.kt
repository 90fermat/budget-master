package com.budgetmaster.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileLabelsTest {

    @Test
    fun initialsTakeAtMostTwoLetters() {
        assertEquals("JD", initialsOf("John Doe"))
        assertEquals("CF", initialsOf("Cyrille Foyang"))
        // A third name would overflow a 44dp avatar.
        assertEquals("AB", initialsOf("Ada B Lovelace"))
    }

    @Test
    fun initialsHandleASingleName() {
        assertEquals("C", initialsOf("cyrille"))
        assertEquals("C", initialsOf("  cyrille  "))
    }

    @Test
    fun initialsSplitOnTheSeparatorsEmailLocalPartsUse() {
        // The greeting falls back to an email's local part, which is rarely "First Last".
        assertEquals("JD", initialsOf("john.doe"))
        assertEquals("JD", initialsOf("john-doe"))
        assertEquals("JD", initialsOf("john_doe"))
    }

    @Test
    fun blankNameYieldsNoInitialsSoCallersCanFallBackToAnIcon() {
        assertEquals("", initialsOf(""))
        assertEquals("", initialsOf("   "))
    }
}
