package com.budgetmaster.android

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.AppTheme
import com.budgetmaster.shared.AdoptionDialog
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
 * The one dialog in the app whose wording decides what happens to somebody's records.
 *
 * Pinned because the risk here is not a layout regression but a wording one: if the three options
 * ever stop making clear which side is discarded, the user cannot recover from choosing wrong.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    application = android.app.Application::class,
)
class AdoptionDialogScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun capture(name: String, darkTheme: Boolean) {
        composeRule.setContent {
            AppTheme(palette = AppPalette.EMERALD, darkTheme = darkTheme) {
                AdoptionDialog(onChoice = {})
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/$name.png",
            roborazziOptions = roborazziOptions,
        )
    }

    @Test
    fun adoptionDialog_light() = capture("adoptionDialog_light", darkTheme = false)

    @Test
    fun adoptionDialog_dark() = capture("adoptionDialog_dark", darkTheme = true)
}
