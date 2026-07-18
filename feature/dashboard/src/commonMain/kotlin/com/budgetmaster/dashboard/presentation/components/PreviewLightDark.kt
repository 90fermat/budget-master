package com.budgetmaster.dashboard.presentation.components

import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Multiplatform wrapper for PreviewLightDark to allow preview compilation
 * on non-Android targets (e.g., wasmJs and iOS).
 *
 * The `org.jetbrains.compose` Preview is deprecated in favour of
 * `androidx.compose.ui.tooling.preview.Preview`, and that swap was tried and reverted: the
 * androidx annotation does not resolve on wasmJs, so taking the replacement would trade one
 * warning for a broken web build. Left deprecated deliberately until the replacement is
 * available on every target this module compiles for.
 */
@Preview
annotation class PreviewLightDark
