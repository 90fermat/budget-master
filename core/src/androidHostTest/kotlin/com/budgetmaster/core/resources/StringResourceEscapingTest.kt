package com.budgetmaster.core.resources

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compose resources are **not** Android XML.
 *
 * Android's `aapt` unescapes `\'` and `\"`; compose-resources does not — it renders the
 * backslash. The app shipped "Don't have an account?" as `Don\'t` from Phase 0 onward, in
 * 55 strings, and nothing caught it until the app was run and read. XML text needs no such
 * escape, so this fails the build instead of the reader noticing.
 */
class StringResourceEscapingTest {

    private val resourceDirs = listOf(
        "src/commonMain/composeResources/values",
        "src/commonMain/composeResources/values-fr",
    )

    private fun stringFiles(): List<File> =
        resourceDirs.map { File(it, "strings.xml") }.filter { it.exists() }

    @Test
    fun resourceFilesAreFound() {
        // Guards the guard: a wrong path would make every assertion below vacuously pass.
        assertTrue(stringFiles().size == resourceDirs.size, "Expected a strings.xml per locale")
    }

    @Test
    fun stringsDoNotUseAndroidStyleEscapes() {
        val backslashApostrophe = "\\" + "'"
        val backslashQuote = "\\" + "\""

        stringFiles().forEach { file ->
            val offenders = file.readLines()
                .withIndex()
                .filter { (_, line) ->
                    line.contains(backslashApostrophe) || line.contains(backslashQuote)
                }
                .map { (i, line) -> "${file.path}:${i + 1}: ${line.trim()}" }

            assertTrue(
                offenders.isEmpty(),
                "Compose resources render these escapes literally; use a plain ' or \" instead:\n" +
                    offenders.joinToString("\n"),
            )
        }
    }
}
