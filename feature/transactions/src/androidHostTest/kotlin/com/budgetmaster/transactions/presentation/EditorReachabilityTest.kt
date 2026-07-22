package com.budgetmaster.transactions.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.AppTheme
import com.budgetmaster.core.designsystem.EditorScrollContainer
import com.budgetmaster.transactions.presentation.components.AddEditTransactionForm
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The add-transaction form must be reachable to its last field on a phone.
 *
 * This is a regression test for a bug that shipped: the form is a plain Column, and the bottom
 * sheet that hosts it on a phone scrolled not at all — so the date field and the Save button sat
 * below the fold, unreachable, with nothing to indicate anything was missing. Three of the app's
 * four editor hosts had the same gap.
 *
 * A screenshot could not have caught it. An image of a truncated form looks exactly like an image
 * of a form that continues below the frame; only asking whether the control can be *reached*
 * distinguishes them, which is what `performScrollTo` does.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    // A short phone on purpose: the bug is invisible on a tall window.
    qualifiers = "w360dp-h640dp-normal-notlong-notround-any-mdpi-keyshidden-nonav",
    application = android.app.Application::class,
)
class EditorReachabilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theDateAndSaveButtonCanBeReached() {
        composeRule.setContent {
            AppTheme(palette = AppPalette.EMERALD, darkTheme = false) {
                EditorScrollContainer(modifier = Modifier.fillMaxSize()) {
                    AddEditTransactionForm(
                        editing = null,
                        categories = emptyList(),
                        accounts = emptyList(),
                        activeAccountId = null,
                        onSave = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Date").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
    }
}
