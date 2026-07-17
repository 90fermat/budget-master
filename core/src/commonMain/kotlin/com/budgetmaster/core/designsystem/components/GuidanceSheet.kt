@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgetmaster.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.guide_got_it
import budgetmaster.core.generated.resources.guide_help
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.pressScale
import com.budgetmaster.core.guidance.GuidanceKey
import com.budgetmaster.core.guidance.GuidancePreferences
import com.budgetmaster.core.guidance.GuidanceRegistry
import com.budgetmaster.core.guidance.ScreenGuide
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Whether a screen's guide is currently open. */
@Stable
class GuidanceController internal constructor(val key: GuidanceKey) {
    var visible by mutableStateOf(false)
        private set

    fun show() {
        visible = true
    }

    fun dismiss() {
        visible = false
    }
}

/**
 * Wires a screen to its guide: auto-opens once on first visit, then leaves it on demand.
 *
 * Marks the guide seen when it opens rather than when it's dismissed — it *was* shown, and a
 * user who swipes it away has made their choice.
 *
 * Pair with [GuidanceHost] to render it:
 * ```
 * val guidance = rememberGuidance(GuidanceKey.BUDGETS)
 * HelpIconButton(onClick = guidance::show)   // in the header
 * GuidanceHost(guidance)                     // anywhere in the screen
 * ```
 */
@Composable
fun rememberGuidance(key: GuidanceKey): GuidanceController {
    val preferences = koinInject<GuidancePreferences>()
    val controller = remember(key) { GuidanceController(key) }

    LaunchedEffect(key) {
        if (preferences.tipsEnabled.first() && !preferences.hasSeen(key).first()) {
            controller.show()
            preferences.markSeen(key)
        }
    }
    return controller
}

/** Renders [controller]'s guide when it's open. */
@Composable
fun GuidanceHost(controller: GuidanceController) {
    if (controller.visible) {
        GuidanceSheet(
            guide = GuidanceRegistry.guideFor(controller.key),
            onDismiss = controller::dismiss,
        )
    }
}

/** The `?` that opens a screen's guide. */
@Composable
fun HelpIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = stringResource(Res.string.guide_help),
        )
    }
}

/**
 * A screen's guide: what it is, then each feature as icon + name + one line.
 *
 * Bottom sheet on phones, dialog on wide layouts — the same adaptive rule as every editor.
 * Notes are real text rather than an annotated diagram, so a screen reader can read them.
 */
@Composable
fun GuidanceSheet(guide: ScreenGuide, onDismiss: () -> Unit) {
    val body: @Composable () -> Unit = { GuidanceBody(guide, onDismiss) }

    BoxWithConstraints {
        if (maxWidth < 600.dp) {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) { body() }
        } else {
            Dialog(onDismissRequest = onDismiss) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.width(520.dp)) { body() }
                }
            }
        }
    }
}

@Composable
private fun GuidanceBody(guide: ScreenGuide, onDismiss: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = stringResource(guide.title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(guide.intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        guide.notes.forEach { note ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.compact)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = note.icon,
                        // Decorative: the note's title and body carry the meaning.
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = stringResource(note.title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(note.body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(
            onClick = onDismiss,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .pressScale(interaction),
        ) {
            Text(stringResource(Res.string.guide_got_it), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(Spacing.small))
    }
}
