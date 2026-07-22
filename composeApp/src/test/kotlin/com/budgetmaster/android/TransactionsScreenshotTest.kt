package com.budgetmaster.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.AppTheme
import com.budgetmaster.core.util.RelativeDay
import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.model.TransactionItem
import com.budgetmaster.transactions.presentation.TransactionDayGroup
import com.budgetmaster.transactions.presentation.TransactionsContent
import com.budgetmaster.transactions.presentation.TransactionsState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The transactions list, in the three states a user actually meets: loading, populated, and empty.
 *
 * Fixed dates and amounts, so the images change only when the design does.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    application = android.app.Application::class,
)
class TransactionsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private val food = TransactionCategory("cat_food", "Food & Dining", "🍔", "#F59E0B")
    private val salary = TransactionCategory("cat_salary", "Salary", "💰", "#059669")
    private val transport = TransactionCategory("cat_transport", "Transport", "🚗", "#6366F1")

    // A fixed day, so "Older" rows render a stable date instead of Today/Yesterday.
    private val day = LocalDate(2026, 7, 8)
    private val stamp = 1_752_000_000_000L

    private val populated = TransactionsState(
        isLoading = false,
        currencyCode = "XAF",
        categories = listOf(food, salary, transport),
        groups = listOf(
            TransactionDayGroup(
                date = day,
                relative = RelativeDay.OLDER,
                net = -21_288.0,
                items = listOf(
                    TransactionItem("t1", -20_288.0, "FOYANG CYRILLE", stamp, food, null),
                    TransactionItem("t2", -1_000.0, "Taxi to Akwa", stamp, transport, null),
                ),
            ),
            TransactionDayGroup(
                date = LocalDate(2026, 7, 1),
                relative = RelativeDay.OLDER,
                net = 450_000.0,
                items = listOf(
                    TransactionItem("t3", 450_000.0, "July salary", stamp, salary, null),
                ),
            ),
        ),
    )

    private fun capture(name: String, state: TransactionsState, darkTheme: Boolean = false) {
        composeRule.setContent {
            AppTheme(palette = AppPalette.EMERALD, darkTheme = darkTheme) {
                TransactionsContent(state = state, onIntent = {}, modifier = Modifier.fillMaxSize())
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/$name.png",
            roborazziOptions = roborazziOptions,
        )
    }

    @Test
    fun transactions_populated_light() = capture("transactions_populated_light", populated)

    @Test
    fun transactions_populated_dark() = capture("transactions_populated_dark", populated, darkTheme = true)

    @Test
    fun transactions_empty() = capture(
        "transactions_empty",
        TransactionsState(isLoading = false, currencyCode = "XAF"),
    )

    @Test
    fun transactions_loading() = capture(
        "transactions_loading",
        TransactionsState(isLoading = true, currencyCode = "XAF"),
    )
}
