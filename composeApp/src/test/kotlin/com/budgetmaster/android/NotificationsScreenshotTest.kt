package com.budgetmaster.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.AppTheme
import com.budgetmaster.core.notifications.AppNotification
import com.budgetmaster.shared.notifications.presentation.NotificationsContent
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
 * Screenshot coverage for the notifications inbox, so the unread/read visual distinction and the
 * empty state are pinned. Renders the stateless [NotificationsContent] directly, no Koin needed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    application = android.app.Application::class,
)
class NotificationsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    // A day in July 2026, so "Older" rows render a stable date rather than Today/Yesterday.
    private val olderDay = 1_752_000_000_000L

    private val sample = listOf(
        AppNotification(
            id = "1",
            title = "Orange Money",
            message = "-20 288,48 FCFA — FOYANG CYRILLE → Everyday",
            timestamp = olderDay,
            isRead = false,
        ),
        AppNotification(
            id = "2",
            title = "Food & Dining",
            message = "You've gone over your Food & Dining budget (112%).",
            timestamp = olderDay,
            isRead = false,
        ),
        AppNotification(
            id = "3",
            title = "Transaction needs your decision",
            message = "Orange Money: -5 000 FCFA — MIKAM looks like one you already entered.",
            timestamp = olderDay,
            isRead = true,
        ),
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

    @Test
    fun notifications_populated_light() = snapshot("notifications_populated_light", darkTheme = false) {
        NotificationsContent(sample, onBack = {}, onMarkAllRead = {}, onItemClick = {}, onDelete = {})
    }

    @Test
    fun notifications_populated_dark() = snapshot("notifications_populated_dark", darkTheme = true) {
        NotificationsContent(sample, onBack = {}, onMarkAllRead = {}, onItemClick = {}, onDelete = {})
    }

    @Test
    fun notifications_empty_light() = snapshot("notifications_empty_light", darkTheme = false) {
        NotificationsContent(emptyList(), onBack = {}, onMarkAllRead = {}, onItemClick = {}, onDelete = {})
    }
}
