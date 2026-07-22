package com.budgetmaster.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The modifier order that keeps sign-in a readable column on a wide window.
 *
 * The auth screens capped their width with `fillMaxWidth().widthIn(max = 420.dp)`, which reads as
 * "fill, but no wider than 420" and does nothing at all. Constraints flow left to right:
 * `fillMaxWidth` fixes the width to the parent's, and `widthIn` cannot shrink a fixed constraint.
 * On a phone the cap was never reached so nothing looked wrong; on a tablet or in a browser every
 * field and button ran edge to edge.
 *
 * Asserting the widths directly is worth more than a screenshot here — it names the rule rather
 * than recording what it happened to look like, and it fails for the right reason.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = "w1280dp-h800dp-xhdpi",
    application = android.app.Application::class,
)
class AuthWidthTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theCapAppliesOnlyWhenItComesFirst() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .testTag("capped"),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .testTag("uncapped"),
                )
            }
        }

        composeRule.onNodeWithTag("capped").assertWidthIsEqualTo(420.dp)
        // The order the auth screens used. Kept in the test so the difference is visible rather
        // than folklore: it fills the whole 1280dp window despite naming a 420dp maximum.
        composeRule.onNodeWithTag("uncapped").assertWidthIsEqualTo(1280.dp)
    }
}
