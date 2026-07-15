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
    // Initialize Koin DI for Web
    initKoin()

    // Render the Compose Multiplatform UI. Since CMP 1.10, ComposeViewport manages
    // its own canvas inside the given container element (here: <body>).
    ComposeViewport(document.body!!) {
        App()
    }
}
