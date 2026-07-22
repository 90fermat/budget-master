package com.budgetmaster.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Wraps an editor's contents so they can always be reached.
 *
 * Every editor in the app is hosted twice — a bottom sheet on a phone, a dialog on a wide window —
 * and neither host scrolls on its own. A form taller than its host is therefore not merely awkward:
 * everything past the fold is *unreachable*, with no indication that anything is missing. The
 * add-transaction form hit this on an ordinary phone, where the date field and the save button both
 * sat below the cut.
 *
 * It exists as a shared container rather than a modifier repeated at each host because it was
 * repeated at each host, and three of the four sites had quietly not repeated it.
 *
 * [imePadding] belongs here for the same reason: an on-screen keyboard takes roughly half the
 * height, which turns a form that just fits into one that does not.
 */
@Composable
fun EditorScrollContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding(),
        content = content,
    )
}
