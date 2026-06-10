package com.budgetmaster.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.budgetmaster.shared.di.initKoin
import platform.UIKit.UIViewController

/**
 * Creates the iOS UIViewController hosting the Compose Multiplatform App UI.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}

/**
 * Bootstraps Koin dependency injection from the Swift side.
 */
fun initKoinIos() {
    initKoin()
}
