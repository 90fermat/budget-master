package com.budgetmaster.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.AppTheme
import com.budgetmaster.core.security.BiometricOutcome
import com.budgetmaster.core.security.PinResult
import com.budgetmaster.shared.lock.presentation.LockScreen
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the unlock screen's appearance in both themes, and with and without the biometric key.
 *
 * Renders [LockScreen] directly with stub callbacks, so no controller, Koin or biometric hardware
 * is involved — the screen is a pure function of the two inputs that change it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    application = android.app.Application::class,
)
class LockScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun snapshot(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            AppTheme(palette = AppPalette.INDIGO, darkTheme = darkTheme) { content() }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/$name.png",
            roborazziOptions = roborazziOptions,
        )
    }

    @Composable
    private fun Subject(biometricOffered: Boolean) {
        LockScreen(
            biometricOffered = biometricOffered,
            onSubmitPin = { PinResult.Wrong },
            // Never resolves to Success, so the screenshot captures the PIN pad rather than an
            // instantly-unlocked screen.
            onBiometric = { _, _, _ -> BiometricOutcome.Cancelled },
            onBiometricSuccess = {},
        )
    }

    @Test
    fun lock_pinOnly_light() = snapshot("lock_pin_only_light", darkTheme = false) {
        Subject(biometricOffered = false)
    }

    @Test
    fun lock_withBiometric_light() = snapshot("lock_with_biometric_light", darkTheme = false) {
        Subject(biometricOffered = true)
    }

    @Test
    fun lock_withBiometric_dark() = snapshot("lock_with_biometric_dark", darkTheme = true) {
        Subject(biometricOffered = true)
    }
}
