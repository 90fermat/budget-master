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
    
    // Render the Compose Multiplatform UI inside the HTML canvas
    val canvas = document.getElementById("compose-canvas") ?: throw IllegalStateException("Canvas element not found!")
    ComposeViewport(canvas) {
        App()
    }
}
