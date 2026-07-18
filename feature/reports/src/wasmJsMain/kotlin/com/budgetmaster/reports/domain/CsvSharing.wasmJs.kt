// Wasm JS interop is still experimental upstream; this file is entirely interop by nature.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.budgetmaster.reports.domain

import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement

/**
 * Triggers a browser download via a data URL on a synthetic anchor — no Blob/URL lifetime to
 * manage, and it works without a server.
 */
actual suspend fun shareCsv(fileName: String, content: String): Boolean = runCatching {
    val anchor = document.createElement("a") as HTMLAnchorElement
    anchor.href = "data:text/csv;charset=utf-8," + encodeURIComponent(content)
    anchor.download = fileName
    document.body?.appendChild(anchor)
    anchor.click()
    document.body?.removeChild(anchor)
    true
}.getOrDefault(false)

private fun encodeURIComponent(value: String): String =
    js("encodeURIComponent(value)")
