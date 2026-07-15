package com.budgetmaster.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.budgetmaster.shared.App
import com.budgetmaster.shared.di.initKoin
import kotlinx.browser.document

/**
 * Main entry point for the Kotlin/Wasm Web application.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    ComposeViewport(document.body!!) {
        App()
    }
}
