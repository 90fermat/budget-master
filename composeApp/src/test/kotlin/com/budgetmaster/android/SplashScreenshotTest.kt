package com.budgetmaster.android

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetmaster.auth.presentation.splash.SplashContent
import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.AppTheme
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
 * The splash screen, pinned at its fully revealed frame.
 *
 * It is the first thing anyone sees and the one screen with no state to inspect afterwards, so a
 * regression here is both the most visible and the easiest to miss. Rendering [SplashContent] with
 * the reveal at its end values avoids depending on a clock, which is what made the real screen
 * untestable.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    application = android.app.Application::class,
)
class SplashScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun capture(name: String, darkTheme: Boolean) {
        composeRule.setContent {
            AppTheme(palette = AppPalette.EMERALD, darkTheme = darkTheme) {
                SplashContent()
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/$name.png",
            roborazziOptions = roborazziOptions,
        )
    }

    @Test
    fun splash_light() = capture("splash_light", darkTheme = false)

    @Test
    fun splash_dark() = capture("splash_dark", darkTheme = true)

    /** Mid-reveal, so the staggered entrance is pinned too and not just where it lands. */
    @Test
    fun splash_midReveal() {
        composeRule.setContent {
            AppTheme(palette = AppPalette.EMERALD, darkTheme = false) {
                SplashContent(
                    markScale = 0.85f,
                    contentAlpha = 0.6f,
                    wordmarkAlpha = 0.3f,
                    accentScale = 0f,
                    creditAlpha = 0f,
                )
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/splash_midReveal.png",
            roborazziOptions = roborazziOptions,
        )
    }
}
